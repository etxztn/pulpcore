/*
    Copyright (c) 2007, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package pulpcore.platform;

import pulpcore.animation.Fixed;
import pulpcore.math.CoreMath;
import pulpcore.sound.Sound;


public class SoundStream {
    
    // The number of milliseconds to fade from a mute/unmute
    private static final int MUTE_TIME = 50;
    
    private final AppContext context;
    private final Sound sound;
    private final int loopFrame;
    private final int numLoopFrames;
    
    private final Fixed level;
    private final Fixed pan;
    private final Fixed outputLevel = new Fixed();
    
    private int frame;
    private int animationFrame;
    private boolean lastMute;
    private boolean loop;
    
    
    public SoundStream(AppContext context, Sound sound, Fixed level, Fixed pan, 
        int loopFrame, int numLoopFrames, int animationFrameDelay)
    {
        this.context = context;
        this.sound = sound;
        this.level = level;
        this.pan = pan;
        this.loopFrame = loopFrame;
        this.numLoopFrames = numLoopFrames;
        
        this.loop = (numLoopFrames > 0);
        this.frame = 0;
        this.animationFrame = -animationFrameDelay;
        
        this.lastMute = isMute();
        this.outputLevel.set(lastMute?0:1);
    }
    
    
    public boolean isMute() {
        return context != null && context.isMute();
    }

    
    public boolean isFinished() {
        return (frame >= sound.getNumFrames());
    }
    
    
    public void skip(int numFrames) {
        int oldAnimationTime = getAnimationTime();
        int oldFrame = frame;
        int newFrame = frame + numFrames;
        animationFrame += numFrames;
        
        if (inLoop()) {
            frame = loopFrame + ((newFrame - loopFrame) % numLoopFrames);
        }
        else if (newFrame > sound.getNumFrames()) {
            frame = sound.getNumFrames();
        }
        else {
            frame = newFrame;
        }
        
        int elapsedTime = getAnimationTime() - oldAnimationTime;
        level.update(elapsedTime);
        pan.update(elapsedTime);
        outputLevel.update(elapsedTime);
    }
    
    
    private int getAnimationTime() {
        return 1000 * animationFrame / sound.getSampleRate();
    }
    
    
    private boolean inLoop() {
        return (loop && frame >= loopFrame && frame < loopFrame + numLoopFrames);
    }
    
    
    public void render(byte[] dest, int destOffset, int destChannels, int numFrames) {
        
        // Gradually mute/unmute over time to reduce popping
        boolean mute = isMute();
        if (lastMute != mute) {
            int currLevel = outputLevel.getAsFixed();
            int goalLevel = mute?0:CoreMath.ONE;
            int diff = Math.abs(goalLevel - currLevel);
            outputLevel.animateToFixed(goalLevel, (diff * MUTE_TIME) >> CoreMath.FRACTION_BITS);
            lastMute = mute;
        }
        
        int destFrameSize = destChannels * 2;
        
        while (numFrames > 0) {
            
            boolean isAnimating = level.isAnimating() || outputLevel.isAnimating() || 
                pan.isAnimating();
            int currLevel = level.getAsFixed();
            int currPan = pan.getAsFixed();
            
            if (frame >= sound.getNumFrames()) {
                currLevel = 0;
            }
            
            if (currLevel <= 0) {
                currLevel = 0;
                isAnimating = false;
                loop = false;
            }
            else {
                currLevel = CoreMath.mul(currLevel, outputLevel.getAsFixed());
            }
            if (currPan < -CoreMath.ONE) {
                currPan = -CoreMath.ONE;
            }
            else if (currPan > CoreMath.ONE) {
                currPan = CoreMath.ONE;
            }
            
            int framesToRender = numFrames;
            if (isAnimating) {
                // Only render 64 frames, then recalcuate animation parameters
                framesToRender = Math.min(64, framesToRender);
            }
            if (inLoop()) {
                // Don't render past loop boundary
                framesToRender = Math.min(framesToRender, loopFrame + numLoopFrames - frame); 
            }
            
            if (currLevel > 0) {
                sound.getSamples(dest, destOffset, destChannels, frame, framesToRender);
            }
            render(dest, destOffset, destChannels, framesToRender, currLevel, currPan);
            
            numFrames -= framesToRender;
            destOffset += framesToRender * destFrameSize;
            skip(framesToRender);
        }
    }
    
    
    private static void render(byte[] data, int offset, int channels,
        int numFrames, int level, int pan)
    {
        int frameSize = channels * 2;
        
        if (level <= 0) {
            // Mute
            int length = numFrames * frameSize;
            for (int i = 0; i < length; i++) {
                data[offset++] = 0;
            }
        }
        else if (channels == 1 || pan == 0) {
            if (level != CoreMath.ONE) {
                // Adjusted level, but no panning (both stereo and mono rendering)
                int numSamples = numFrames*channels;
                for (int i = 0; i < numSamples; i++) {
                    int input = getSample(data, offset); 
                    int output = (input * level) >> CoreMath.FRACTION_BITS;
                    setSample(data, offset, output);
                    
                    offset += 2;
                }
            }
        }
        else {
            // Stereo sound with panning
            int leftLevel4LeftInput;
            int leftLevel4RightInput;
            int rightLevel4LeftInput;
            int rightLevel4RightInput;
            if (pan < 0) {
                leftLevel4LeftInput = CoreMath.ONE + pan / 2;
                leftLevel4RightInput = -pan / 2;
                rightLevel4LeftInput = 0;
                rightLevel4RightInput = CoreMath.ONE + pan;
            }
            else {
                leftLevel4LeftInput = CoreMath.ONE - pan;
                leftLevel4RightInput = 0;
                rightLevel4LeftInput = pan / 2;
                rightLevel4RightInput = CoreMath.ONE - pan / 2;
            }
            if (level != CoreMath.ONE) {
                leftLevel4LeftInput = CoreMath.mul(level, leftLevel4LeftInput);
                leftLevel4RightInput = CoreMath.mul(level, leftLevel4RightInput);
                rightLevel4LeftInput = CoreMath.mul(level, rightLevel4LeftInput);
                rightLevel4RightInput = CoreMath.mul(level, rightLevel4RightInput);
            }
            
            for (int i = 0; i < numFrames; i++) {
                int leftInput = getSample(data, offset);
                int rightInput = getSample(data, offset + 2);
                int leftOutput = 
                    (leftInput * leftLevel4LeftInput + rightInput * leftLevel4RightInput) >>
                    CoreMath.FRACTION_BITS;
                int rightOutput = 
                    (leftInput * rightLevel4LeftInput + rightInput * rightLevel4RightInput) >>
                    CoreMath.FRACTION_BITS;                        
                setSample(data, offset, leftOutput);
                setSample(data, offset + 2, rightOutput);
                
                offset += 4;
            }
        }
    }
    
    
    private static int getSample(byte[] data, int offset) {
        // Signed big endian
        return (data[offset] << 8) | (data[offset + 1] & 0xff); 
    }
    
    
    private static void setSample(byte[] data, int offset, int sample) {
        // Signed big endian
        data[offset] = (byte)((sample >> 8) & 0xff);
        data[offset + 1] = (byte)(sample & 0xff);
    }    
}



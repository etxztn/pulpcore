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
import pulpcore.sound.SoundClip;


public class SoundStream {
    
    // The number of milliseconds to fade from a mute/unmute
    private static final int MUTE_TIME = 50;
    
    private static byte[] silentBuffer;
    
    private final AppContext context;
    private final SoundClip clip;
    private final int length;
    private final int clipStartPosition;
    private boolean loop;
    
    private final Fixed level;
    private final Fixed outputLevel = new Fixed();
    
    private int position;
    private int animationPosition;
    private boolean lastMute;
    
    public static void setSilentBuffer(byte[] silentBuffer) {
        SoundStream.silentBuffer = silentBuffer;
    }
    
    public SoundStream(AppContext context, SoundClip clip, Fixed level, boolean loop) {
        this(context, clip, level, loop, 0, 0);
    }
    
    
    public SoundStream(AppContext context, SoundClip clip, Fixed level, boolean loop, 
        int preClipSilenceLength, int postClipSilenceLength)
    {
        this.context = context;
        this.clip = clip;
        this.level = level;
        this.loop = loop;
        this.length = clip.length() + preClipSilenceLength + postClipSilenceLength;
        this.clipStartPosition = preClipSilenceLength;
        
        this.position = 0;
        this.animationPosition = -clipStartPosition;
        
        this.lastMute = isMute();
        this.outputLevel.set(lastMute?0:1);
    }
    
    
    public boolean isMute() {
        return context != null && context.isMute();
    }

    
    public boolean isFinished() {
        return (position >= length);
    }
    
    
    public int skip(int n) {
        int oldAnimationTime = getAnimationTime();
        int oldPosition = position;
        int newPosition = position + n;
        animationPosition += n;
        
        // Don't skip back to the pre-clip silence if it's already played
        if (position >= clipStartPosition && newPosition < clipStartPosition) {
            if (loop) {
                position = clipStartPosition + 
                    ((clip.length() + newPosition - clipStartPosition) % clip.length());
            }
            else {
                position = clipStartPosition;
                animationPosition = 0;
            }
        }
        else if (newPosition < 0) {
            position = 0;
            animationPosition = -clipStartPosition;
        }
        else if (loop && newPosition >= clipStartPosition + clip.length()) {
            position = clipStartPosition + 
                ((newPosition - clipStartPosition) % clip.length());
        }
        else if (newPosition > length) {
            position = length;
        }
        else {
            position = newPosition;
        }
        
        int elapsedTime = getAnimationTime() - oldAnimationTime;
        level.update(elapsedTime);
        outputLevel.update(elapsedTime);
        
        return position - oldPosition;
    }
    
    
    private int getAnimationTime() {
        return 1000 * animationPosition / clip.getByteRate();
    }
    
    
    public void render(byte[] buffer, int bufferOffset, int numBytes) {
        // Gradually mute/unmute over time to reduce popping
        boolean mute = isMute();
        if (lastMute != mute) {
            int currLevel = outputLevel.getAsFixed();
            int goalLevel = mute?0:CoreMath.ONE;
            int diff = Math.abs(goalLevel - currLevel);
            outputLevel.animateToFixed(goalLevel, (diff * MUTE_TIME) >> CoreMath.FRACTION_BITS);
            lastMute = mute;
        }
        
        while (numBytes > 0) {
            
            boolean isAnimating = level.isAnimating() || outputLevel.isAnimating();
            int currLevel = level.getAsFixed();
            if (currLevel < 0) {
                loop = false;
                currLevel = 0;
            }
            else {
                currLevel = CoreMath.mul(currLevel, outputLevel.getAsFixed());
            }
            
            int byteLength = Math.min(buffer.length - bufferOffset, numBytes);
            
            if (position < clipStartPosition) {
                // Pre-clip silence
                byteLength = Math.min(clipStartPosition - position, byteLength);
                byteLength = Math.min(silentBuffer.length, byteLength);
                System.arraycopy(silentBuffer, 0, buffer, bufferOffset, byteLength);
            }
            else if ((!isAnimating && currLevel <= 0) ||
                position >= clipStartPosition + clip.length()) 
            {
                // Mute or post-clip silence
                byteLength = Math.min(silentBuffer.length, byteLength);
                System.arraycopy(silentBuffer, 0, buffer, bufferOffset, byteLength);
            }
            else if (!isAnimating && currLevel == CoreMath.ONE) {
                // Simple case - no rendering
                int clipOffset = position - clipStartPosition;
                byteLength = Math.min(clip.length() - clipOffset, byteLength);
                System.arraycopy(clip.getData(), clipOffset, buffer, bufferOffset, byteLength);
            }
            else {
                // Complex case - rendering
                // Note: rendered sample is not clipped.
                int clipOffset = position - clipStartPosition;
                byteLength = Math.min(clip.length() - clipOffset, byteLength);
                if (isAnimating) {
                    // Only render 64 samples, then recalcuate animation parameters
                    byteLength = Math.min(128, byteLength);
                }
                
                for (int i = 0; i < byteLength; i+=2) {
                    int sample = (clip.getSample(clipOffset + i) * currLevel) >> 
                        CoreMath.FRACTION_BITS;
                    buffer[bufferOffset+i] = (byte)((sample >> 8) & 0xff);
                    buffer[bufferOffset+i+1] = (byte)(sample & 0xff);
                }
            }
            
            bufferOffset += byteLength;
            numBytes -= byteLength;
            skip(byteLength);
        }
    }
}



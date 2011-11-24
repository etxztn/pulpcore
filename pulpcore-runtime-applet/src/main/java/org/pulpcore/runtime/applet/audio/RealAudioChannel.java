/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime.applet.audio;

import java.lang.ref.WeakReference;
import org.pulpcore.math.FixedMath;
import org.pulpcore.math.MathUtil;
import org.pulpcore.media.AudioChannel.OnCompletionListener;
import org.pulpcore.runtime.applet.AppletContext;
import org.pulpcore.view.animation.TweenFloat;
import org.pulpcore.view.property.FloatProperty;

public class RealAudioChannel {
    /** The number of seconds to fade from a mute/unmute */
    public static final float MUTE_DUR = 0.005f;
    
    // Number of frames to render while animating. Should represent at least 1 ms at 44100Hz
    // (that is, greater than 44 frames)
    private static final int MAX_FRAMES_TO_RENDER_WHILE_ANIMATING = 64;
    
    public static final int STATE_IDLE = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_STOPPING = 3;
    
    private AppletContext context;
    private VirtualAudioChannel channel;
    private WeakReference<VirtualAudioChannel> channelRef;
    private AppletAudio audio;
    
    private int frame;
    private int startFrame;
    private int trackingFrame;
    private float channelVolume;
    private boolean channelLooping;
    private float lastGoalVolume;
    private final FloatProperty actualVolume = new FloatProperty();
    private TweenFloat actualVolumeTween;
    private int state;
    
    /** If true, the audio is muting, and when it is fully muted, the frame is set to trackingFrame. */
    private boolean tracking;
    
    private int preSilenceFrames;
    private int postSilenceFrames;
    private int numPreSilienceFramesPlayed;
    private int numPostSilienceFramesPlayed;
    
    private Runnable onCompletionCallback;
    
    public RealAudioChannel(AppletContext context, VirtualAudioChannel channel,
            int preSilenceFrames, int postSilenceFrames) {
        reset(context, channel, preSilenceFrames,  postSilenceFrames);
    }
    
    public void reset(AppletContext context, VirtualAudioChannel channel,
            int preSilenceFrames, int postSilenceFrames) {
        this.context = context;
        unbind();
        this.channel = channel;
        this.channelRef = new WeakReference<VirtualAudioChannel>(channel);
        this.audio = (AppletAudio)channel.getAudio();
        
        this.frame = 0;
        this.startFrame = 0;
        this.state = STATE_IDLE;
        this.tracking = false;
        this.preSilenceFrames = preSilenceFrames;
        this.postSilenceFrames = postSilenceFrames;
        this.numPreSilienceFramesPlayed = 0;
        this.numPostSilienceFramesPlayed = 0;
        this.onCompletionCallback = null;
        
        this.channelVolume = channel.isMute() ? 0 : channel.getVolume();
        this.channelLooping = channel.isLooping();
        this.lastGoalVolume = getGoalVolume();
        this.actualVolume.set(lastGoalVolume);
        this.channel.bind(this);
        setOnCompletionListener(channel.getOnCompletionListener());
    }
    
    private void setActualVolume(float actualVolume) {
        actualVolumeTween = null;
        this.actualVolume.set(actualVolume);
        this.lastGoalVolume = actualVolume;
    }
    
    public void unbind() {
        if (this.channel == null && this.channelRef == null) {
            // Do nothing
        }
        else {
            VirtualAudioChannel c = getBoundChannel();
            if (c != null && c.getBinding() == this) {
                c.unbind();
            }
            this.channel = null;
            this.channelRef = null;
        }
    }
    
    public void setOnCompletionListener(final OnCompletionListener l) {
        final VirtualAudioChannel c = getBoundChannel();
        if (l != null && c != null) {
            onCompletionCallback = new Runnable() {
                @Override
                public void run() {
                    if (c.getBinding() == null) {
                        l.onCompletion(c);
                    }
                }
            };
        }
        else {
            onCompletionCallback = null;
        }
    }
    
    void setVolume(float f) {
        channelVolume = f;
    }

    void setLooping(boolean looping) {
        channelLooping = looping;
    }

    
    public int getState() {
        return state;
    }
    
    public AppletAudio getAudio() {
        return audio;
    }
    
    private float getGoalVolume() {
        if (context == null || context.getMasterMute() || tracking) {
            return 0;
        }
        else {
            return Math.max(0, context.getMasterVolume() * channelVolume);
        }
    }
    
    private int clampFrame(int frame) {
        return MathUtil.clamp(frame, 0, audio.getNumFrames() - 1);
    }
    
    public int getCurrentFrame() {
        if (tracking) {
            return trackingFrame;
        }
        else if (state == STATE_PLAYING || state == STATE_PAUSED) {
            return frame;
        }
        else {
            return startFrame;
        }
    }

    public void setCurrentFrame(int currentFrame) {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            tracking = true;
            trackingFrame = clampFrame(currentFrame);
        }
        else {
            startFrame = clampFrame(currentFrame);
        }
    }
    
    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }
 
    public void play(int startFrameIfNotActive) {
        if (getState() != RealAudioChannel.STATE_PAUSED) {
            setCurrentFrame(startFrameIfNotActive);
        }
        if (state == STATE_IDLE) {
            numPreSilienceFramesPlayed = 0;
            numPostSilienceFramesPlayed = 0;
        }
        if (state == STATE_IDLE && startFrame == 0) {
            state = STATE_PLAYING;
            frame = 0;
        }
        else if (state != STATE_PLAYING) {
            state = STATE_PLAYING;
            frame = clampFrame(startFrame);
            setActualVolume(0);
        }
    }

    public void pause() {
        if (state == STATE_PLAYING) {
            state = STATE_PAUSED;
            startFrame = frame;
        }
    }

    public void stop() {
        if (state != STATE_IDLE) {
            state = STATE_STOPPING;
            unbind();
        }
        startFrame = 0;
    }
 
    private void checkPlayback() {
        if (channel == null) {
            VirtualAudioChannel thisChannel = getBoundChannel();
            if (thisChannel != null) {
                if (state != STATE_PAUSED && state != STATE_IDLE) {
                    channel = thisChannel;
                }
            }
            else if (state == STATE_PAUSED) {
                // VirtualAudioChannel was paused, but no references to the VirtualAudioChannel
                // object exist, so stop the sound
                stop();
            }
        }
        else if (state == STATE_PAUSED || state == STATE_IDLE) {
            channel = null;
        }
    }
    
    public VirtualAudioChannel getBoundChannel() {
        VirtualAudioChannel c = channel;
        if (c == null && channelRef != null) {
            c = channelRef.get();
        }
        return c;
    }
    
    private void setFramePosition(int newFrame) {
        int numFrames = audio.getNumFrames();
        if (channelLooping) {
            frame = (newFrame % numFrames);
        }
        else if (newFrame >= numFrames) {
            int oldFrame = frame;
            frame = numFrames;
            setActualVolume(0);
            if (oldFrame < numFrames && (state == STATE_PLAYING || state == STATE_PAUSED)) {
                state = STATE_STOPPING;
                unbind();
            }
        }
        else {
            frame = newFrame;
        }
        
        if (state == STATE_PAUSED) {
            startFrame = frame;
        }
    }
  
    private void advanceFramePosition(int numFrames) {
        if (state == STATE_IDLE) {
            // Do nothing
        }
        else if (state != STATE_PLAYING && actualVolume.get() == 0) {
            // Do nothing
        }
        else {
            // Either playing, or transitioning towards paused or stopped.
            setFramePosition(frame + numFrames);
            
            if (actualVolumeTween != null) {
                float dt = Math.max(0, (float)numFrames) / audio.getSampleRate();
                actualVolumeTween.tick(dt);
                if (actualVolumeTween.isFinished()) {
                    actualVolumeTween = null;
                }
            }
        }
    }

    public void render(byte[] dest, int destOffset, int destChannels, int numFrames) {
        
        int destFrameSize = destChannels * AppletAudio.SAMPLE_SIZE;
        
        if (numPreSilienceFramesPlayed < preSilenceFrames) {
            int silenceFrames = Math.min(numFrames, preSilenceFrames - numPreSilienceFramesPlayed);
            int length = silenceFrames * destFrameSize;
            for (int i = 0; i < length; i++) {
                dest[destOffset++] = 0;
            }
            numPreSilienceFramesPlayed += silenceFrames;
            numFrames -= silenceFrames;
            if (numFrames <= 0) {
                return;
            }
        }
        
        float goalVolume = getGoalVolume();
        if (state != STATE_PLAYING) {
            goalVolume = 0;
        }
        
        // Gradually change volume over time to reduce popping
        if (goalVolume != lastGoalVolume) {
            actualVolumeTween = new TweenFloat(MUTE_DUR, actualVolume, actualVolume.get(), goalVolume);
            lastGoalVolume = goalVolume;
        }
        
        // Last render when stopping
        if (state == STATE_STOPPING && actualVolume.get() == 0) {
            final Runnable callback = onCompletionCallback;
            onCompletionCallback = null;
            if (callback != null) {
                try {
                    callback.run();
                }
                catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
            if (numPostSilienceFramesPlayed < postSilenceFrames) {
                int silenceFrames = Math.min(numFrames, postSilenceFrames - numPostSilienceFramesPlayed);
                int length = silenceFrames * destFrameSize;
                for (int i = 0; i < length; i++) {
                    dest[destOffset++] = 0;
                }
                numPostSilienceFramesPlayed += silenceFrames;
                numFrames -= silenceFrames;
                
            }
            
            if (numPostSilienceFramesPlayed >= postSilenceFrames) {
                state = STATE_IDLE;
            }
        }
        else if (tracking && actualVolume.get() == 0) {
            tracking = false;
            setFramePosition(trackingFrame);
        }
        
        checkPlayback();
        
        while (numFrames > 0) {
            
            boolean isAnimating = actualVolumeTween != null;
            int currLevel = FixedMath.toFixed(actualVolume.get());
            int currPan = 0;
            
            int framesToRender = numFrames;
            if (isAnimating) {
                // Only render a few frames, then recalcuate animation parameters
                framesToRender = Math.min(MAX_FRAMES_TO_RENDER_WHILE_ANIMATING, framesToRender);
            }
            if (frame < audio.getNumFrames()) {
                framesToRender = Math.min(framesToRender, audio.getNumFrames() - frame); 
            }
            
            // Figure out the next level and pan (for interpolation)
            int srcFrame = frame;
            advanceFramePosition(framesToRender);
            int nextLevel = FixedMath.toFixed(actualVolume.get());
            int nextPan = 0;
            
            // Render
            if (currLevel > 0 || nextLevel > 0) {
                audio.getSamples(dest, destOffset, destChannels, srcFrame, framesToRender);
            }
            render(dest, destOffset, destChannels, framesToRender, 
                currLevel, nextLevel, currPan, nextPan);
            
            // Inc offsets
            numFrames -= framesToRender;
            destOffset += framesToRender * destFrameSize;
        }
    }
 
    private static void render(byte[] data, int offset, int channels,
        int numFrames, int startLevel, int endLevel, int startPan, int endPan)
    {
        int frameSize = channels * 2;
        
        if (startLevel <= 0 && endLevel <= 0) {
            // Mute
            int length = numFrames * frameSize;
            for (int i = 0; i < length; i++) {
                data[offset++] = 0;
            }
        }
        else if (channels == 1 || (startPan == 0 && endPan == 0)) {
            // No panning (both stereo and mono rendering)
            if (startLevel != FixedMath.ONE || endLevel != FixedMath.ONE) {
                int numSamples = numFrames*channels;
                int level = startLevel;
                int levelInc = (endLevel - startLevel) / numSamples;
                for (int i = 0; i < numSamples; i++) {
                    int input = getSample(data, offset); 
                    int output = (input * level) >> FixedMath.FRACTION_BITS;
                    setSample(data, offset, output);
                    
                    offset += 2;
                    level += levelInc;
                }
            }
        }
        else {
            // Stereo sound with panning
            int startLeftLevel4LeftInput;
            int startLeftLevel4RightInput;
            int startRightLevel4LeftInput;
            int startRightLevel4RightInput;
            int endLeftLevel4LeftInput;
            int endLeftLevel4RightInput;
            int endRightLevel4LeftInput;
            int endRightLevel4RightInput;
            if (startPan < 0) {
                startLeftLevel4LeftInput = FixedMath.ONE + startPan / 2;
                startLeftLevel4RightInput = -startPan / 2;
                startRightLevel4LeftInput = 0;
                startRightLevel4RightInput = FixedMath.ONE + startPan;
            }
            else {
                startLeftLevel4LeftInput = FixedMath.ONE - startPan;
                startLeftLevel4RightInput = 0;
                startRightLevel4LeftInput = startPan / 2;
                startRightLevel4RightInput = FixedMath.ONE - startPan / 2;
            }
            if (endPan < 0) {
                endLeftLevel4LeftInput = FixedMath.ONE + endPan / 2;
                endLeftLevel4RightInput = -endPan / 2;
                endRightLevel4LeftInput = 0;
                endRightLevel4RightInput = FixedMath.ONE + endPan;
            }
            else {
                endLeftLevel4LeftInput = FixedMath.ONE - endPan;
                endLeftLevel4RightInput = 0;
                endRightLevel4LeftInput = endPan / 2;
                endRightLevel4RightInput = FixedMath.ONE - endPan / 2;
            }
            if (startLevel != FixedMath.ONE) {
                startLeftLevel4LeftInput = FixedMath.mul(startLevel, startLeftLevel4LeftInput);
                startLeftLevel4RightInput = FixedMath.mul(startLevel, startLeftLevel4RightInput);
                startRightLevel4LeftInput = FixedMath.mul(startLevel, startRightLevel4LeftInput);
                startRightLevel4RightInput = FixedMath.mul(startLevel, startRightLevel4RightInput);
            }
            if (endLevel != FixedMath.ONE) {
                endLeftLevel4LeftInput = FixedMath.mul(endLevel, endLeftLevel4LeftInput);
                endLeftLevel4RightInput = FixedMath.mul(endLevel, endLeftLevel4RightInput);
                endRightLevel4LeftInput = FixedMath.mul(endLevel, endRightLevel4LeftInput);
                endRightLevel4RightInput = FixedMath.mul(endLevel, endRightLevel4RightInput);
            }
            
            int leftLevel4LeftInput = startLeftLevel4LeftInput;
            int leftLevel4RightInput = startLeftLevel4RightInput;
            int rightLevel4LeftInput = startRightLevel4LeftInput;
            int rightLevel4RightInput = startRightLevel4RightInput;
            int leftLevel4LeftInputInc = 
                (endLeftLevel4LeftInput - startLeftLevel4LeftInput) / numFrames;
            int leftLevel4RightInputInc = 
                (endLeftLevel4RightInput - startLeftLevel4RightInput) / numFrames;
            int rightLevel4LeftInputInc = 
                (endRightLevel4LeftInput - startRightLevel4LeftInput) / numFrames;
            int rightLevel4RightInputInc = 
                (endRightLevel4RightInput - startRightLevel4RightInput) / numFrames;
            for (int i = 0; i < numFrames; i++) {
                int leftInput = getSample(data, offset);
                int rightInput = getSample(data, offset + 2);
                int leftOutput = 
                    (leftInput * leftLevel4LeftInput + rightInput * leftLevel4RightInput) >>
                    FixedMath.FRACTION_BITS;
                int rightOutput = 
                    (leftInput * rightLevel4LeftInput + rightInput * rightLevel4RightInput) >>
                    FixedMath.FRACTION_BITS;                        
                setSample(data, offset, leftOutput);
                setSample(data, offset + 2, rightOutput);
                
                offset += 4;
                leftLevel4LeftInput += leftLevel4LeftInputInc;
                leftLevel4RightInput += leftLevel4RightInputInc;
                rightLevel4LeftInput += rightLevel4LeftInputInc;
                rightLevel4RightInput += rightLevel4RightInputInc;
            }
        }
    }
    
    public static int getSample(byte[] data, int offset) {
        // Signed little endian
        return (data[offset + 1] << 8) | (data[offset] & 0xff);
    }
    
    public static void setSample(byte[] data, int offset, int sample) {
        // Signed little endian
        data[offset] = (byte)sample;
        data[offset + 1] = (byte)(sample >> 8);
    }
}

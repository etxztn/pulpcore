/*
    Copyright (c) 2009-2011, Interactive Pulp, LLC
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
package org.pulpcore.media;

import org.pulpcore.view.property.FloatProperty;

/**
The AudioPlayer can be used for more complex interactions with Audio 
playback. The AudioPlayer class is created from the {@link Audio} class.
@see Audio#createAudioPlayer() 
@see Audio#play() 
@see Audio#loop() 
*/
public abstract class AudioChannel {
    
    public interface OnCompletionListener  {
        public void onCompletion(AudioChannel audioChannel);
    }
    
    public final FloatProperty volume = new FloatProperty(1);
    
    private boolean mute = false;
    private boolean looping = false;
    private OnCompletionListener completionListener = null;
    
    private final Audio audio;
    
    protected AudioChannel(Audio audio) {
        this.audio = audio;
    }
    
    public Audio getAudio() {
        return audio;
    }

    public float getVolume() {
        return volume.get();
    }
    
    public void setVolume(float volume) {
        this.volume.set(volume);
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }
    
    public OnCompletionListener getOnCompletionListener() {
        return completionListener;
    }

    public void setOnCompletionListener(OnCompletionListener completionListener) {
        this.completionListener = completionListener;
    }
    
    public double getCurrentTime() {
        return (double)getCurrentFrame() / audio.getSampleRate();
    }
    
    public void setCurrentTime(double time) {
        setCurrentFrame((int)(time * audio.getSampleRate()));
    }

    public abstract boolean isPlaying();

    public abstract int getCurrentFrame();

    public abstract void setCurrentFrame(int currentFrame);
    
    public abstract void play();
    
    public abstract void pause();
    
    public abstract void stop();
}

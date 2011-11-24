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

import org.pulpcore.runtime.Context;

/**
    The Audio class represents an audio buffer to play.
*/
public abstract class Audio {
    
    /**
    Returns the list of the platform's supported audio formats, like "wav" and "ogg".
    */
    public static String[] getSupportedFormats() {
        return Context.getContext().getSupportedSoundFormats();
    }
    
    public static float getMasterVolume() {
        return Context.getContext().getMasterVolume();
    }
    
    public static void setMasterVolume(float volume) {
        Context.getContext().setMasterVolume(volume);
    }
    
    public static boolean getMasterMute() {
        return Context.getContext().getMasterMute();
    }
    
    public static void setMasterMute(boolean mute) {
        Context.getContext().setMasterMute(mute);
    }
    
    /**
    Loads an audio buffer. Returns a previously cached audio buffer if possible.
    If the audio asset is not found, a "silent" buffer is returned - this method never returns null.
    */
    public static Audio load(String audioAsset) {
        return Context.getContext().loadAudio(audioAsset);
    }

    private final int sampleRate;
    
    /**
    Creates a new Audio buffer with the specified sample rate.
    @param sampleRate the sample rate (samples per second, per channel). 
    @param numChannels the number of channels (1 for mono, 2 for stereo). 
    */
    protected Audio(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    /**
        Returns the sample rate - the number of samples played per second, per channel. 
    */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
        Gets the duration of this clip in seconds.
        @return the duration of this clip in seconds.
    */
    public double getDuration() {
        return (double)getNumFrames() / sampleRate;
    }

    /**
        Gets the length of this sound, expressed in the number of frames. For mono sounds,
        a frame is one sample, for stereo sounds, a frame consists of two samples - one for
        the left channel, and one for the right channel.
    */
    public abstract int getNumFrames();
    
    /**
    Creates a new audio player. The AudioPlayer will be returned in the PREPARING or READY state.
    If an AudioPlayer could not be created (for example, if this Audio buffer 
    <p>
    This method may return a previously created AudioPlayer if it is in the READY state.
    */
    public abstract AudioChannel createAudioChannel();
    
    /**
    Gets the number of copies of this Audio buffer currently playing or paused.
    */
    public abstract int getNumCurrentlyPlaying();

    /**
    Plays a copy of this Audio file.
    @return The AudioPlayer object, or null if the sound could not be played.
    */
    public AudioChannel play() {
        AudioChannel channel = createAudioChannel();
        if (channel != null) {
            channel.play();
        }
        return channel;
    }
    
    /**
    Plays a copy of this Audio file, looping at the end.
    @return The AudioPlayer object, or null if the sound could not be played.
    */
    public AudioChannel loop() {
        AudioChannel channel = createAudioChannel();
        if (channel != null) {
            channel.setLooping(true);
            channel.play();
        }
        return channel;
    }
    
    @Override
    public abstract boolean equals(Object object);

    @Override
    public abstract int hashCode();
}

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

import org.pulpcore.media.Audio;
import org.pulpcore.media.AudioChannel;

public abstract class AppletAudio extends Audio {
    
    public static final int SAMPLE_SIZE = 2;
    
    private final int numChannels;
    
    protected AppletAudio(int sampleRate, int numChannels) {
        super(sampleRate);
        this.numChannels = numChannels;
    }
    
    public int getNumChannels() {
        return numChannels;
    }
    
    /**
        Copies a sequence of samples from this Audio to a byte array as 
        signed, little endian, 16-bit PCM format.
        @param dest the destination buffer.
        @param destOffset the offset, in bytes, in the destination buffer.
        @param destChannels The number of channels of the destination (1 or 2).
        @param srcFrame the frame position to start copying from.
        @param numFrames the number of frames to copy.
    */
    public abstract void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames);
    
    @Override
    public AudioChannel createAudioChannel() {
        return new VirtualAudioChannel(AppletAudioEngine.getAudioEngine(), this);
    }

    @Override
    public int getNumCurrentlyPlaying() {
        AppletAudioEngine engine = AppletAudioEngine.getAudioEngine();
        if (engine != null) {
            return engine.getNumCurrentlyPlaying(this);
        }
        else {
            return 0;
        }
    }
}

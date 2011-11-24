package org.pulpcore.runtime.lwjgl.audio;

import org.pulpcore.media.AudioChannel;

/**
Internal class for PulpCore's audio playback using LWJGL. 
PulpCore apps do not use this class directly.
*/
public class BufferedAudio extends LWJGLAudio {
    
    private final int bufferId;
    private final int numFrames;
    
    /**
    Creates a silent sound.
    */
    public BufferedAudio(LWJGLAudioEngine engine) {
        super(engine, 44100, 1);
        this.bufferId = 0;
        this.numFrames = 0;
    }
    
    public BufferedAudio(LWJGLAudioEngine engine, int bufferId, int sampleRate, int numChannels, int numFrames) {
        super(engine, sampleRate, numChannels);
        this.bufferId = bufferId;
        this.numFrames = numFrames;
    }
    
    public int getBufferId() {
        return bufferId;
    }

    @Override
    public int getNumFrames() {
        return numFrames;
    }

    @Override
    public AudioChannel createAudioChannel() {
        if (bufferId == 0 || getAudioEngine() == null) {
            return null;
        }
        else {
            return new VirtualAudioChannel(getAudioEngine(), this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BufferedAudio other = (BufferedAudio) obj;
        if (this.bufferId != other.bufferId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.bufferId;
    }
}

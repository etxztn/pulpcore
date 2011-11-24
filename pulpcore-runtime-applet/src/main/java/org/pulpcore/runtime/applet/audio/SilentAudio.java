package org.pulpcore.runtime.applet.audio;

/**
A simple sound generator that creates silence.
*/
public class SilentAudio extends AppletAudio {

    private final int numFrames;

    public SilentAudio(int sampleRate, int numFrames) {
        super(sampleRate, 1);
        this.numFrames = numFrames;
    }

    @Override
    public int getNumFrames() {
        return numFrames;
    }

    @Override
    public void getSamples(byte[] dest, int destOffset, int destChannels, 
    int srcFrame, int numFrames) {
        int frameSize = SAMPLE_SIZE * destChannels;
        int length = numFrames * frameSize;
        for (int i = 0; i < length; i++) {
            dest[destOffset++] = 0;
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
        final SilentAudio other = (SilentAudio) obj;
        if (this.numFrames != other.numFrames) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return numFrames;
    }
}
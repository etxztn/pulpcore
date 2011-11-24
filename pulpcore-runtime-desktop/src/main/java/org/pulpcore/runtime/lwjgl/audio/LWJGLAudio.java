package org.pulpcore.runtime.lwjgl.audio;

import org.pulpcore.media.Audio;

public abstract class LWJGLAudio extends Audio {
    
    private final LWJGLAudioEngine engine;
    private final int numChannels;
    
    public LWJGLAudio(LWJGLAudioEngine engine, int sampleRate, int numChannels) {
        super(sampleRate);
        this.engine = engine;
        this.numChannels = numChannels;
    }
    
    public LWJGLAudioEngine getAudioEngine() {
        return engine;
    }
    
    
    @Override
    public int getNumCurrentlyPlaying() {
        if (engine != null) {
            return engine.getNumCurrentlyPlaying(this);
        }
        else {
            return 0;
        }
    }

    public int getNumChannels() {
        return numChannels;
    }
    
    //
    // For streaming
    //
    
    public boolean isStreaming() {
        return false;
    }
    
    /**
    @return bytesRead, or -1 for end of stream.
    */
    public int getNextStreamingSamples(byte[] dest) {
        return -1;
    }
    
    public int getStreamingFrame() {
        return 0;
    }

    public void setStreamingFrame(int currentFrame) {
        // Do nothing
    }

}

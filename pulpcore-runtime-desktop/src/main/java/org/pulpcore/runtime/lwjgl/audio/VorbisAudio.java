package org.pulpcore.runtime.lwjgl.audio;

import java.io.InputStream;
import org.pulpcore.media.AudioChannel;
import org.pulpcore.runtime.jre.ogg.VorbisFile;
import org.pulpcore.runtime.jre.ogg.VorbisFile.State;
import org.pulpcore.util.Objects;


public class VorbisAudio extends LWJGLAudio {

    public static VorbisAudio load(LWJGLAudioEngine audioEngine, String filename, InputStream in) {
        VorbisFile file = VorbisFile.load(filename, in);
        if (file == null) {
            return null;
        }
        else {
            return new VorbisAudio(audioEngine, file);
        }
    }

    private VorbisFile file;
    
    private VorbisAudio(VorbisAudio src) {
        this(src.getAudioEngine(), src.file == null ? null : new VorbisFile(src.file));
    }

    private VorbisAudio(LWJGLAudioEngine audioEngine, VorbisFile file) {
        super(audioEngine, file == null ? 44100 : file.getSampleRate(), file == null ? 1 : file.getNumChannels());
        this.file = file;
    }
    
    @Override
    public int getNumFrames() {
        return (file == null) ? 0 : file.getNumFrames();
    }
    
    public void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames)
    {
        boolean clearDest = false;
        if (file == null) {
            clearDest = true;
        }
        else {
            
            try {
                file.getSamples(dest, destOffset, destChannels, srcFrame, numFrames);
            }
            catch (Exception ex) {
                System.out.println("JOrbis problem: " + ex.getMessage());
                // Internal JOrbis problem - happens rarely. (Notably on IBM 1.4 VMs)
                // Kill JOrbis and start over.
                clearDest = true;

                file = new VorbisFile(file);
                if (file.getState() == State.INVALID) {
                    file = null;
                }
            }
        }

        if (clearDest) {
            int frameSize = 2 * destChannels;
            int length = numFrames * frameSize;
            for (int i = 0; i < length; i++) {
                dest[destOffset++] = 0;
            }
        }
    }

    @Override
    public AudioChannel createAudioChannel() {
        return new VirtualAudioChannel(getAudioEngine(), new VorbisAudio(this));
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof VorbisAudio) && file != null && file.equals(((VorbisAudio)obj).file);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file);
    }
    
    //
    // For streaming
    //
    
    @Override 
    public boolean isStreaming() {
        return true;
    }
    
    @Override
    public int getNextStreamingSamples(byte[] dest) {
        int frameSize = 2 * getNumChannels();
        
        if (file == null || file.getFramePosition() >= file.getNumFrames()) {
            return -1;
        }
        else {
            int numFrames = Math.min(dest.length / frameSize, file.getNumFrames() - file.getFramePosition());
            getSamples(dest, 0, getNumChannels(), file.getFramePosition(), numFrames);
            return numFrames * frameSize;
        }
    }
    
    @Override
    public int getStreamingFrame() {
        return file == null ? 0 : file.getFramePosition();
    }

    @Override
    public void setStreamingFrame(int currentFrame) {
        if (file != null) {
            file.setFramePosition(currentFrame);
        }
    }
}

package org.pulpcore.runtime.lwjgl.audio;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.pulpcore.media.Audio;
import org.pulpcore.media.AudioChannel;
import org.pulpcore.runtime.jre.WAVFile;

public class LWJGLAudioEngine {
    /**
        The decompress threshold, in seconds. Sounds with a duration less than or equal to this
        value are fully decompressed when loaded. Sounds with a duration greater than this value
        are decompressed on the fly as they are played.
    */
    private static final float DECOMPRESS_THRESHOLD = 4;    
    
    private static final int MAX_SIMULTANEOUS_SOUNDS = 32;

    private static class Buffer {
        private int bufferId;
        private final WeakReference<Audio> bufferRef;

        public Buffer(int bufferId, Audio audio) {
            this.bufferId = bufferId;
            this.bufferRef = new WeakReference<Audio>(audio);
        }

        public boolean isDestroyed() {
            if (bufferId == 0) {
                return true;
            }
            else if (bufferRef.get() == null) {
                destroy();
                return true;
            }
            else {
                return false;
            }
        }

        public void destroy() {
            if (bufferId != 0) {
                AL10.alDeleteBuffers(bufferId);
                bufferId = 0;
            }
        }
    }
    
    private boolean initilized;
    private RealAudioChannel[] sources = new RealAudioChannel[0];
    private List<Buffer> buffers = new ArrayList<Buffer>();
    
    public LWJGLAudioEngine() {
       init();
    }
    
    private void init() {
        boolean createdAL = false;
        try {
            AL.create();
            if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                createdAL = true;
            }
            else {
                System.out.println("Couldn't init OpenAL");
            }   
        } 
        catch (LWJGLException ex) {
            ex.printStackTrace(System.out);
        }

        if (createdAL) {
            List<RealAudioChannel> sourceList = new ArrayList<RealAudioChannel>();
            for (int i = 0; i < MAX_SIMULTANEOUS_SOUNDS; i++) {
                boolean success = false;
                int sourceId = AL10.alGenSources();
		if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                    AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
                    if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                        sourceList.add(new RealAudioChannel(sourceId));
                        success = true;
                    }
                    else {
                        AL10.alDeleteSources(sourceId);
                    }
                }
                
                if (!success) {
                    break;
                }
            }
            sources = sourceList.toArray(new RealAudioChannel[sourceList.size()]);
        
            initilized = true;
        }
    }
    
    public BufferedAudio createBufferedAudio(int numChannels, int sampleRate, ByteBuffer data) {
        int bufferId = AL10.alGenBuffers();
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            return null;
        }
        
        int frameSize = numChannels * 2;
        int numFrames = data.limit() / frameSize;
        
        AL10.alBufferData(bufferId, 
                numChannels == 2 ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16, 
                data,
                sampleRate);
        
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            return null;
        }
        
        BufferedAudio audioBuffer = new BufferedAudio(this, bufferId, sampleRate, numChannels, numFrames);
        Buffer buffer = new Buffer(bufferId, audioBuffer);
        buffers.add(buffer);
        return audioBuffer;
    }    

    public Audio loadOGG(String filename, InputStream in) {
        if (!initilized) {
            return null;
        }
        
        purgeUnusedBuffers();
        
        VorbisAudio audio = VorbisAudio.load(this, filename, in);
        if (audio != null && audio.getDuration() <= DECOMPRESS_THRESHOLD) {
            // Decompress
            byte[] decompressedData = new byte[2 * audio.getNumChannels() * audio.getNumFrames()];
            audio.getSamples(decompressedData, 0, audio.getNumChannels(), 0, audio.getNumFrames());
            
            ByteBuffer data = BufferUtils.createByteBuffer(decompressedData.length);
            data.put(decompressedData);
            data.rewind();
            return createBufferedAudio(audio.getNumChannels(), audio.getSampleRate(), data);
        }
        else {
            return audio;
        }
    }

    public BufferedAudio loadWAV(String name, InputStream in) throws IOException {
        if (!initilized) {
            return null;
        }
        
        purgeUnusedBuffers();
        
        WAVFile wavFile = WAVFile.load(in, true, ByteOrder.nativeOrder());
        if (wavFile == null) {
            return null;
        }
        
        return createBufferedAudio(wavFile.getNumChannels(), wavFile.getSampleRate(), wavFile.getData());
    }
    
    private void purgeUnusedBuffers() {
        Iterator<Buffer> i = buffers.iterator();
        while (i.hasNext()) {
            Buffer b = i.next();
            if (b.isDestroyed()) {
                i.remove();
            }
        }
    }
    
    public void requestBind(VirtualAudioChannel channel) {
        for (RealAudioChannel source : sources) {
            if (!source.isBound()) {
                source.bind(channel);
                return;
            }
        }
    }

    public int getNumCurrentlyPlaying(LWJGLAudio audio) {
        int count = 0;
        List<AudioChannel> list = null;
        for (RealAudioChannel source : sources) {
            if (source.isPlaying(audio)) {
                count++;
            }
        }
        return count;
    }
    
    public void destroy() {
        for (Buffer buffer : buffers) {
            buffer.destroy();
        }
        buffers.clear();
        for (RealAudioChannel source : sources) {
            source.destroy();
        }
        sources = new RealAudioChannel[0];
        AL.destroy();
    }
    
    public void tick(float dt) {
        for (RealAudioChannel source : sources) {
            source.tick();
        }
    }
    
    public void setVolume(float volume) {
        AL10.alListenerf(AL10.AL_GAIN, volume);
    }
}

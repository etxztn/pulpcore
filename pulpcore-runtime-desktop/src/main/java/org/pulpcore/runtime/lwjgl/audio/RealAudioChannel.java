package org.pulpcore.runtime.lwjgl.audio;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.pulpcore.media.AudioChannel.OnCompletionListener;

/**
An OpenAL source.
*/
public class RealAudioChannel {
    
    private static final int NUM_STREAMING_BUFFERS = 3;
    private static final int STREAMING_BUFFER_SIZE = 4096;
    
    private static final byte[] TEMP_BUFFER = new byte[STREAMING_BUFFER_SIZE];
    
        
    private final int sourceId;
    private boolean playing;
    private boolean looping;
    private WeakReference<VirtualAudioChannel> channelRef;
    private LWJGLAudio audio;
    private IntBuffer streamingBufferIds;
    private ByteBuffer streamingBuffer;
    private Runnable onCompletionCallback;
        
    public RealAudioChannel(int sourceId) {
        this.sourceId = sourceId;
    }
    
    private VirtualAudioChannel getBoundChannel() {
        if (channelRef == null) {
            return null;
        }
        else {
            return channelRef.get();
        }
    }
    
    public boolean isPlaying(LWJGLAudio audio) {
        if (audio != null && audio.equals(this.audio)) {
            if (playing) {
                return true;
            }
            else {
                int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
                return state == AL10.AL_PLAYING || state == AL10.AL_PAUSED;
            }
        }
        return false;
    }
    
    public boolean isPlaying() {
        return playing;
    }
    
    public void setVolume(float volume) {
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
        if (!audio.isStreaming()) {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    public int getCurrentFrame() {
        if (audio.isStreaming()) {
            return audio.getStreamingFrame();
        }
        else {
            return AL10.alGetSourcei(sourceId, AL11.AL_SAMPLE_OFFSET); 
        }
    }

    public void setCurrentFrame(int currentFrame) {
        if (audio.isStreaming()) {
            audio.setStreamingFrame(currentFrame);
        }
        else {
            AL10.alSourcei(sourceId, AL11.AL_SAMPLE_OFFSET, currentFrame);
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

    public void play(int startFrameIfNotActive) {
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PAUSED) {
            if (audio.isStreaming()) {
                audio.setStreamingFrame(startFrameIfNotActive);
                for (int i = 0; i < NUM_STREAMING_BUFFERS; i++) {
                    int bytesRead = audio.getNextStreamingSamples(TEMP_BUFFER);
                    if (bytesRead > 0) {
                        int bufferId = streamingBufferIds.get(i);
                        bufferStreamingData(bufferId, bytesRead);
                        AL10.alSourceQueueBuffers(sourceId, bufferId);
                    }
                    else {
                        break;
                    }
                }
            }
            else {
                AL10.alSourcei(sourceId, AL11.AL_SAMPLE_OFFSET, startFrameIfNotActive);
            }
        }
        AL10.alSourcePlay(sourceId);
        playing = true;
    }

    public void pause() {
        AL10.alSourcePause(sourceId);
        playing = false;
    }

    public void stop() {
        AL10.alSourceStop(sourceId);
        AL10.alSourceRewind(sourceId);
        playing = false;
        unbind();
    }
    
    public void destroy() {
        unbind();
        if (sourceId != 0) {
            AL10.alSourceStop(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
            AL10.alDeleteSources(sourceId);
        }
    }
    
    public void bind(VirtualAudioChannel c) {
        unbind();
        
        if (c == null) {
            return;
        }
        
        LWJGLAudio channelAudio = (LWJGLAudio)c.getAudio();
        
        if (channelAudio == null) {
            System.out.println("No audio in channel: " + c.getClass());
            return;
        }
        else if (channelAudio instanceof BufferedAudio) {
            int bufferId = ((BufferedAudio)channelAudio).getBufferId();
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
        }
        else if (channelAudio.isStreaming()) {
            streamingBufferIds = BufferUtils.createIntBuffer(NUM_STREAMING_BUFFERS);
            AL10.alGenBuffers(streamingBufferIds);
            if (streamingBuffer == null) {
                streamingBuffer = BufferUtils.createByteBuffer(STREAMING_BUFFER_SIZE);
            }
        }
        else {
            System.out.println("Don't know how to play audio of type: " + channelAudio.getClass());
            return;
        }
        
        channelRef = new WeakReference<VirtualAudioChannel>(c);
        audio = channelAudio;
        
        // Channel settings
        setOnCompletionListener(c.getOnCompletionListener());
        looping = c.isLooping();
        float volume = c.isMute() ? 0 : c.getVolume();
        if (!audio.isStreaming()) {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
        AL10.alSourcef(sourceId, AL10.AL_GAIN, volume);
        
        c.bind(this);
    }
    
    private void unbind() {
        if (streamingBufferIds != null) {
            streamingBufferIds.rewind();
            AL10.alDeleteBuffers(streamingBufferIds);
            streamingBufferIds = null;
        }
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
        VirtualAudioChannel c = getBoundChannel();
        if (c != null && c.getBinding() == this) {
            c.unbind();
        }
        channelRef = null;
        audio = null;
        onCompletionCallback = null;
    }
    
    public boolean isBound() {
        return channelRef != null;
    }
    
    private void bufferStreamingData(int bufferId, int bytesRead) {
        int format = audio.getNumChannels() == 2 ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
        streamingBuffer.rewind();
        streamingBuffer.limit(STREAMING_BUFFER_SIZE);
        streamingBuffer.put(TEMP_BUFFER, 0, bytesRead);
        streamingBuffer.flip();
        AL10.alBufferData(bufferId, format, streamingBuffer, audio.getSampleRate());
    }
        
    public void tick() {
        if (isBound()) {
            int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            if (playing) {
                if (audio.isStreaming()) {
                    
                    int count = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
                    boolean bufferQueued = false;
                    boolean triedLooping = false;
                    while (count > 0) {
                        int bytesRead = audio.getNextStreamingSamples(TEMP_BUFFER);
                        if (bytesRead > 0) {
                            int bufferId = AL10.alSourceUnqueueBuffers(sourceId);
                            bufferStreamingData(bufferId, bytesRead);
                            AL10.alSourceQueueBuffers(sourceId, bufferId);
                            bufferQueued = true;
                        }
                        else if (bytesRead < 0) {
                            if (looping && !triedLooping) {
                                audio.setStreamingFrame(0);
                                triedLooping = true;
                                // Countinue without changing the count
                                continue;
                            }
                            else {
                                break;
                            }
                        }
                        count--;
                    }
                    
                    if (bufferQueued && state != AL10.AL_PLAYING) {
                        AL10.alSourcePlay(sourceId);
                        state = AL10.AL_PLAYING;
                    }
                }
                
                if (state == AL10.AL_INITIAL || state == AL10.AL_STOPPED) {
                    playing = false;
                    Runnable callback = onCompletionCallback;
                    unbind();
                    if (callback != null) {
                        try {
                            callback.run();
                        }
                        catch (Exception ex) {
                            ex.printStackTrace(System.out);
                        }
                    }
                }
            }
            else if (state == AL10.AL_PAUSED) {
                VirtualAudioChannel c = getBoundChannel();
                if (c == null) {
                    // Paused, but the channel reference is lost, so it can't be unpaused.
                    // Kill it.
                    AL10.alSourceStop(sourceId);
                    unbind();
                }
            }
        }
    }
}

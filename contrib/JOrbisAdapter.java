// Don't change the package name! This class is called via reflection from PulpCore.
package pulpcore.sound;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.sound.SoundClip;
import pulpcore.util.ByteArray;


/*
    SoundClip loader for JOrbis' OGG Vorbis decoder, based of JOrbis' DecoderExample.
    
    To use:
    1. Get JOrbis here: http://www.jcraft.com/jorbis/
    2. Drop jorbis-0.0.15.jar and jogg-0.0.7.jar in your lib/ directory (using the "project"
       template from the PulpCore templates/ directory)
    3. Drop this file in your src/ directory. 
    4. Load sounds like normal:
    
    SoundClip sound = SoundClip.load("mysound.ogg");
    
    You can manipulate the level and pan in real time, just like regular SoundClips.
    
    Note that the OGG sound is decoded entirely into memory, which means it can take up
    around 10x the memory as the size of the file. Future versions may decompress on the fly.
*/
public class JOrbisAdapter {
    
    private static final int BLOCK_SIZE = 4096;
    
    // This method is called from SoundClip.load() via reflection
    public static SoundClip decode(ByteArray input, String soundAsset) throws EOFException, 
        IllegalArgumentException 
    {
        int convsize = BLOCK_SIZE * 2;
        byte[] convbuffer = new byte[convsize];
        byte[] buffer;
        int bytes = 0;

        SyncState oy = new SyncState();
        StreamState os = new StreamState();
        Page og = new Page();
        Packet op = new Packet();
        Info vi = new Info();
        Comment vc = new Comment();
        DspState vd = new DspState();
        Block vb = new Block(vd);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
 
        oy.init();
 
        while (true) {
            // Init 
            int eos = 0;
            int index = oy.buffer(BLOCK_SIZE);
            buffer = oy.data;
            bytes = input.read(buffer, index, Math.min(BLOCK_SIZE, input.available()));
            oy.wrote(bytes);
            if (oy.pageout(og) != 1) {
                if (bytes < BLOCK_SIZE) {
                    break;
                }

                throw new IllegalArgumentException();
            }
            os.init(og.serialno());
            vi.init();
            vc.init();
            if (os.pagein(og) < 0 || os.packetout(op) != 1 || vi.synthesis_headerin(vc,op) < 0) { 
                throw new IllegalArgumentException();
            }
            
            // Check audio format
            if (!isSupportedSampleRate(soundAsset, vi.rate)) {
                return null;
            }
            
            if (vi.channels < 0 || vi.channels > 2) {
                if (Build.DEBUG) {
                    CoreSystem.print("Not a mono or stereo sound: " + soundAsset);
                }
                return null;
            }
 
            // Get headers
            int i = 0;
            while (i < 2) {
                while (i < 2) {
                    int result = oy.pageout(og);
                    if (result == 0) {
                        break;
                    }
 
                    if (result == 1) {
                        os.pagein(og);
                        while (i < 2) {
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }

                            if (result == - 1) {
                                throw new IllegalArgumentException();
                            }
                            vi.synthesis_headerin(vc,op);
                            i++;
                        }
                    }
                }
                index = oy.buffer(BLOCK_SIZE);
                buffer = oy.data; 
                bytes = input.read(buffer, index, Math.min(BLOCK_SIZE, input.available()));
                if (bytes == 0 && i < 2) {
                    throw new IllegalArgumentException();
                }
                oy.wrote(bytes);
            }
 
            // Start decoding
            convsize = BLOCK_SIZE / vi.channels;
            vd.synthesis_init(vi);
            vb.init(vd);
            float[][][] _pcm = new float[1][][];
            int[] _index = new int[vi.channels];
            
            while (eos == 0) {
                while (eos == 0) {

                    int result = oy.pageout(og);
                    if (result == 0) {
                        break;
                    }
                    if (result == - 1) {
                        throw new IllegalArgumentException();
                    } else {
                        os.pagein(og); 
                        
                        while (true) {
                            
                            result = os.packetout(op);
                            if (result == 0) {
                                break;
                            }
                            if (result != - 1) {
                                int samples;
                                if (vb.synthesis(op) == 0) {
                                    vd.synthesis_blockin(vb);
                                }

                                while ((samples = vd.synthesis_pcmout(_pcm, _index)) > 0) {

                                    float[][] pcm = _pcm[0];
                                    boolean clipflag = false;
                                    int bout = (samples < convsize ? samples : convsize);
 
                                    // Convert to signed, big endian, 16-bit PCM format.
                                    for (i = 0; i < vi.channels; i++) {
                                        int ptr = i * 2;
                                        int ptrInc = 2 * (vi.channels);
                                        int mono = _index[i];
                                        float[] pcm_row = pcm[i];
                                        for (int j = 0; j < bout; j++) {
                                            int sample = (int) (pcm_row[mono + j] * 32767);
                                            if (sample > 32767) {
                                                sample = 32767;
                                            }
                                            if (sample < -32768) {
                                                sample = -32768;
                                            }

                                            convbuffer[ptr] = (byte)((sample >> 8) & 0xff);
                                            convbuffer[ptr + 1] = (byte)(sample & 0xff);
                                            ptr += ptrInc;
                                        }
                                    }
                                    
                                    out.write(convbuffer, 0, 2 * vi.channels * bout);
                                    vd.synthesis_read(bout);
                                } 
                            }
                        }
                        if (og.eos() != 0) {
                            eos = 1;
                        }

                    }
                }
                if (eos == 0) {
                    index = oy.buffer(BLOCK_SIZE);
                    buffer = oy.data;
                    bytes = input.read(buffer, index, Math.min(BLOCK_SIZE, input.available()));
                    oy.wrote(bytes);
                    if (bytes == 0) {
                        eos = 1;
                    }
                }
            }

            // Clean up 
            os.clear();
            vb.clear();
            vd.clear();
            vi.clear();
        }

        oy.clear();
        
        return new SoundClip(out.toByteArray(), vi.rate, (vi.channels == 2));
    }
    
    
    private static boolean isSupportedSampleRate(String soundAsset, int sampleRate) {
        int[] sampleRates = CoreSystem.getPlatform().getSoundEngine().getSupportedSampleRates();
        for (int i = 0; i < sampleRates.length; i++) {
            if (sampleRate == sampleRates[i]) {
                return true;
            }
        }
        
        if (Build.DEBUG) {
            CoreSystem.print("Unsupported sample rate (" + sampleRate + "Hz): " + soundAsset);
        }
        return false;
    }
}

package org.pulpcore.runtime.applet.audio;

import java.io.InputStream;
import org.pulpcore.media.AudioChannel;
import org.pulpcore.runtime.applet.Log;
import org.pulpcore.util.Objects;
import org.pulpcore.runtime.jre.ogg.VorbisFile;
import org.pulpcore.runtime.jre.ogg.VorbisFile.State;

public class VorbisAudio extends AppletAudio {

    /**
        The decompress threshold, in seconds. Sounds with a duration less than or equal to this
        value are fully decompressed when loaded. Sounds with a duration greater than this value
        are decompressed on the fly as they are played.
    */
    private static final float DECOMPRESS_THRESHOLD = 4;

    public static AppletAudio load(String filename, InputStream in) {
        VorbisFile file = VorbisFile.load(filename, in);
        if (file == null) {
            return null;
        }
        else {
            VorbisAudio audio = new VorbisAudio(file);
            if (audio.getDuration() <= DECOMPRESS_THRESHOLD) {
                return audio.decompress();
            }
            else {
                return audio;
            }
        }
    }

    private VorbisFile file;
    
    private VorbisAudio(VorbisAudio src) {
        this(src.file == null ? null : new VorbisFile(src.file));
    }

    private VorbisAudio(VorbisFile file) {
        super(file == null ? 44100 : file.getSampleRate(), file == null ? 1 : file.getNumChannels());
        this.file = file;
    }

    private RawAudio decompress() {
        if (file == null) {
            return null;
        }
        else {
            byte[] dest = new byte[2 * file.getNumChannels() * file.getNumFrames()];
            file.getSamples(dest, 0, file.getNumChannels(), 0, file.getNumFrames());
            return new RawAudio(file.getFilename(), dest, file.getSampleRate(), (file.getNumChannels() == 2));
        }
    }

    @Override
    public int getNumFrames() {
        return (file == null) ? 0 : file.getNumFrames();
    }

    @Override
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
                Log.w("JOrbis problem", ex);
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
        return new VorbisAudio(this).createAudioChannelImpl();
    }
    
    private AudioChannel createAudioChannelImpl() {
        return super.createAudioChannel();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof VorbisAudio) && file != null && file.equals(((VorbisAudio)obj).file);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file);
    }

}

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
package org.pulpcore.runtime.jre.ogg;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VorbisFile extends OggFile {
    
    private static boolean needsWarmup = true;
    
    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int count;
        byte[] data = new byte[16384];

        while ((count = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, count);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static VorbisFile load(String filename, InputStream is) {
        VorbisFile file = null;
        try {
            file = new VorbisFile(filename, toByteArray(is));
        }
        catch (IOException ex) {
            ex.printStackTrace(System.out);
        }
        if (file == null || file.getState() != State.OPEN) {
            System.out.println("Couldn't load Ogg Vorbis file: " + filename);
            return null;
        }
        
        if (file.getNumChannels() < 1 || file.getNumChannels() > 2) {
            System.out.println("Can't play Ogg Vorbis file with " + file.getNumChannels() + 
                    " channels: " + filename);
            return null;
        }

        if (needsWarmup) {
            needsWarmup = false;
            // Decompress a small amount to warmup HotSpot
            new VorbisFile(file).warmup();
        }
        return file;
    }

    public enum State {
        INIT,
        INVALID,
        OPEN,
    }
    
    private final String filename;
    
    private State state;
    private Info info;
    private Comment comment;
    private int serialno;
    private int numFrames;
    private int startOffset;

    private int framePosition = 0;
    private boolean decodeReady = false;
    
    private StreamState os = null;
    private final DspState vd = new DspState();
    private final Block vb = new Block(vd);
    private int[] _index;
    private float[][][] _pcm = new float[1][][];

    public VorbisFile(VorbisFile file) {
        super(file.getData().newView());

        this.filename = file.filename;
        
        switch (file.getState()) {
            case INVALID:
                this.state = State.INVALID;
                break;
            case OPEN:
                this.state = State.OPEN;
                this.info = file.info;
                this.comment = file.comment;
                this.serialno = file.serialno;
                this.numFrames = file.numFrames;
                this.startOffset = file.startOffset;
                this.os = new StreamState();
                this._index = new int[this.info.channels];
                seek(startOffset);
                break;
            case INIT: default:
                this.state = State.INIT;
                open();
                break;
        }
    }

    public VorbisFile(String filename, byte[] data) {
        this(filename, new ByteArrayDataSource(data));
    }

    public VorbisFile(String filename, DataSource data) {
        super(data);
        this.filename = filename;
        this.state = State.INIT;
        open();
    }

    public String getFilename() {
        return filename;
    }
    
    // Warmup for HotSpot
    private void warmup() {
        int frames = Math.min(4096, getNumFrames());
        byte[] dest = new byte[2 * getNumChannels() * frames];
        getSamples(dest, 0, getNumChannels(), 0, frames);
    }

    public State getState() {
        return state;
    }

//    @Override
//    public void rewind() {
//        super.rewind();
//        this.framePosition = 0;
//        this.decodeReady = false;
//    }

    public boolean isRunning() {
        return decodeReady;
    }

    // Public methods (file info)
    public int getSampleRate() {
        return info.rate;
    }

    public int getNumChannels() {
        return info.channels;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public Comment getComment() {
        return comment;
    }

    public int getFramePosition() {
        return framePosition;
    }

    private void open() {
        Info tempInfo = new Info();
        Comment tempComment = new Comment();
        int ret = getHeaders(tempInfo, tempComment);
        if (ret < 0) {
            state = State.INIT;
        }
        else if (ret == 0) {
            state = State.INVALID;
        }
        else if (!getData().isFilled()) {
            // TODO: Allow streaming. Currently requiring a filled datasource to get the
            // duration.
            state = State.INVALID;
        }
        else if (seekDuration() < 0) {
            state = State.INVALID;
        }
        else {
            this.info = tempInfo;
            this.comment = tempComment;
            this._index = new int[info.channels];
            os.clear();
            state = State.OPEN;
        }
    }

    /**

     @return a negative number if not enough data available,
     0 for failure or
     a positive number for success.
     */
    private int getHeaders(Info tempInfo, Comment tempComment) {
        Page og = new Page();
        Packet op = new Packet();
        boolean done = false;
        int packets = 0;

        // Parse the headers
        // Only interested in Vorbis stream
        while (!done) {
            int ret = getDataChunk();
            if (ret <= 0) {
                return ret;
            }
            while (oy.pageout(og) > 0) {
                StreamState test = new StreamState();

                // is this a mandated initial header? If not, stop parsing
                if (og.bos() == 0) {
                    if (os != null) {
                        os.pagein(og);
                    }
                    done = true;
                    break;
                }

                int testSerialNo = og.serialno();
                test.init(testSerialNo);
                test.pagein(og);
                test.packetout(op);

                if (packets == 0 && tempInfo.synthesis_headerin(tempComment, op) >= 0) {
                    os = test;
                    serialno = testSerialNo;
                    packets = 1;
                }
                else {
                    // Ignore unknown stream
                    test.clear();
                }
            }
        }

        if (packets == 0) {
            return 0;
        }

        // we've now identified all the bitstreams. parse the secondary header packets.
        while (packets < 3) {
            int ret;

            // look for more vorbis header packets
            while (packets < 3 && ((ret = os.packetout(op)) != 0)) {
                if (ret < 0 || tempInfo.synthesis_headerin(tempComment, op) != 0) {
                    return 0;
                }
                packets++;
            }

            // The header pages/packets will arrive before anything else we
            // care about, or the stream is not obeying spec
            if (oy.pageout(og) > 0) {
                os.pagein(og);
            }
            else {
                ret = getDataChunk();
                if (ret <= 0) {
                    return ret;
                }
            }
        }

        vd.synthesis_init(tempInfo);
        return 1;
    }

    // Ogg Vorbis requires seeking to the end to get the duration of the audio.
    // TODO: HTTP streaming.
    //       1. Start downloading the file. The server should return the file length.
    //       2. While the downloading occurs, in a second connection open the file
    //       near its end and seek to find the last granulepos

    /**

     @return negative value on error
     */
    private int seekDuration() {
        this.startOffset = getNextPageOffset();

        seek(getData().size());

        Page og = new Page();

        int endOffset = getPrevPage(og, serialno);
        if (endOffset < 0) {
            return -1;
        }
        else {
            numFrames = (int)og.granulepos();
            seek(startOffset);
            return 0;
        }
    }

    /**
    @return -1 for lost packet, 0 not enough data, or 1 for success
     */
    private int processPacket(boolean readPage) {
        Page og = new Page();
        while (true) {
            if (decodeReady) {
                Packet op = new Packet();
                int result = os.packetout(op);
                if (result > 0) {
                    int granulepos = (int) op.granulepos;
                    if (vb.synthesis(op) == 0) {
                        vd.synthesis_blockin(vb);
                        if (granulepos != -1 && op.e_o_s == 0) {
                            int samples = vd.synthesis_pcmout(null, null);
                            granulepos -= samples;
                            framePosition = granulepos;
                        }
                        return 1;
                    }
                }
            }
            if (!readPage || getNextPage(og, -1) < 0) {
                return 0;
            }
            if (!decodeReady) {
                os.init(serialno);
                os.reset();
                vd.synthesis_init(info);
                vb.init(vd);
                decodeReady = true;
            }
            os.pagein(og);
        }
    }

    // Public methods

    // TODO: needs a better seek algorithm.
    // See http://svn.xiph.org/trunk/vorbis/lib/vorbisfile.c
    public void setFramePosition(int newFramePosition) {
        if (state == State.INIT) {
            open();
        }
        if (state != State.OPEN) {
            return;
        }
        if (newFramePosition < 0) {
            newFramePosition = 0;
        }
        else if (newFramePosition > numFrames) {
            newFramePosition = numFrames;
        }
        if (newFramePosition < framePosition) {
            seek(startOffset);
            framePosition = 0;
        }
        int framesToSkip = newFramePosition - framePosition;
        while (framesToSkip > 0) {
            int f = skip(framesToSkip);
            if (f < 0) {
                return;
            }
            else {
                framesToSkip -= f;
            }
        }
    }

    /**
    @return number of frames skipped, or -1 on error.
     */
    private int skip(int numFrames) {
        if (state == State.INIT) {
            open();
        }
        if (state != State.OPEN) {
            return 0;
        }
        while (true) {
            if (decodeReady) {
                int frames = vd.synthesis_pcmout(_pcm, _index);
                if (frames != 0) {
                    if (frames > numFrames) {
                        frames = numFrames;
                    }
                    vd.synthesis_read(frames);
                    framePosition += frames;
                    return frames;
                }
            }
            if (processPacket(true) <= 0) {
                return -1;
            }
        }
    }

    /**
    @param dest destination buffer
    @param destOffset offset in the destination buffer
    @param destChannels number of channels in the destination (either 1 or 2).
    @param numFrames number of frames to read.
    @return number of frames read, or -1 on error. Always fails if this Vorbis file has more
    than two channels.
     */
    public int read(byte[] dest, int destOffset, int destChannels, int numFrames) {
        if (state == State.INIT) {
            open();
        }
        if (state == State.INVALID) {
            return -1;
        }
        else if (state != State.OPEN) {
            return 0;
        }
        while (true) {
            if (decodeReady) {
                int frames = vd.synthesis_pcmout(_pcm, _index);
                if (frames != 0) {
                    int channels = info.channels;
                    if (frames > numFrames) {
                        frames = numFrames;
                    }
                    if (destChannels == channels) {
                        // Mono-to-mono or stereo-to-stereo
                        int frameSize = 2 * channels;
                        for (int i = 0; i < channels; i++) {
                            int ptr = destOffset + 2 * i;
                            int mono = _index[i];
                            float[] pcm_row = _pcm[0][i];
                            for (int j = 0; j < frames; j++) {
                                int sample = (int) (pcm_row[mono + j] * 32767);
                                if (sample > 32767) {
                                    sample = 32767;
                                }
                                else if (sample < -32768) {
                                    sample = -32768;
                                }
                                dest[ptr] = (byte) sample;
                                dest[ptr + 1] = (byte) (sample >> 8);
                                ptr += frameSize;
                            }
                        }
                    }
                    else if (channels == 1 && destChannels == 2) {
                        // Mono-to-stereo
                        int ptr = destOffset;
                        int mono = _index[0];
                        float[] pcm_row = _pcm[0][0];
                        for (int j = 0; j < frames; j++) {
                            int sample = (int) (pcm_row[mono + j] * 32767);
                            if (sample > 32767) {
                                sample = 32767;
                            }
                            else if (sample < -32768) {
                                sample = -32768;
                            }
                            byte a = (byte) sample;
                            byte b = (byte) (sample >> 8);
                            dest[ptr++] = a;
                            dest[ptr++] = b;
                            dest[ptr++] = a;
                            dest[ptr++] = b;
                        }
                    }
                    else if (destChannels == 1) {
                        // Mix all channels to 1 (not tested)
                        for (int j = 0; j < frames * 2; j++) {
                            dest[destOffset + j] = 0;
                        }
                        for (int i = 0; i < channels; i++) {
                            int ptr = destOffset;
                            int mono = _index[i];
                            float[] pcm_row = _pcm[0][i];
                            for (int j = 0; j < frames; j++) {
                                int oldSample = (dest[ptr] & 255) | (dest[ptr + 1] << 8);
                                int sample = (int) (oldSample +
                                        pcm_row[mono + j] * 32767 / channels);
                                if (sample > 32767) {
                                    sample = 32767;
                                }
                                else if (sample < -32768) {
                                    sample = -32768;
                                }
                                dest[ptr++] = (byte) sample;
                                dest[ptr++] = (byte) (sample >> 8);
                            }
                        }
                    }
                    else {
                        return -1;
                    }
                    vd.synthesis_read(frames);
                    framePosition += frames;
                    return frames;
                }
            }
            if (processPacket(true) <= 0) {
                return -1;
            }
        }
    }
    
    public void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames)
    {
        if (getFramePosition() != srcFrame) {
            setFramePosition(srcFrame);
        }
        int frameSize = destChannels * 2;
        int framesRemaining = numFrames;
        while (framesRemaining > 0) {
            int f = read(dest, destOffset, destChannels, framesRemaining);
            if (f < 0) {
                System.out.println("Couldn't fully decompress Ogg Vorbis file: " + filename);
                for (int i = 0; i < framesRemaining*frameSize; i++) {
                    dest[destOffset++] = 0;
                }
                framesRemaining = 0;
            }
            else {
                framesRemaining -= f;
                destOffset += f * frameSize;
            }
        }
    }
}

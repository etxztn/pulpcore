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
package org.pulpcore.runtime.jre;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
Loads a 16-bit PCM mono or stereo WAV file.
*/
public class WAVFile {
    
    private static final int RIFF = 0x52494646;
    private static final int WAVE = 0x57415645;
    private static final int FMT = 0x666d7420;
    private static final int DATA = 0x64617461;
    
    private final ByteBuffer data;
    private final int sampleRate;
    private final int numChannels;
    
    
    private WAVFile(ByteBuffer data, int sampleRate, int numChannels) {
        this.data = data;
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
    }

    public ByteBuffer getData() {
        return data;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }
    
    public int getNumFrames() {
        int frameSize = getNumChannels() * 2;
        return data.limit() / frameSize;
    }
    
    /**
    Loads a WAV file.
    @param in The input stream.
    @param nativeBuffer Whether to create a native buffer for the samples.
    @param byteOrder The byteOrder to load the samples as.
    @return The WAV file data.
    @throws IOException If the file could not be read, or the file is not a 16-bit PCM mono or stereo WAV file.
    */
    public static WAVFile load(InputStream in, boolean nativeBuffer, ByteOrder byteOrder) throws IOException {
        int sampleRate = -1;
        int numChannels = 0;
        
        if (in == null) {
            throw new IOException("No such file.");
        }
        
        DataInputStream dis;
        if (in instanceof DataInputStream) {
            dis = (DataInputStream)in;
        }
        else {
            dis = new DataInputStream(in);
        }
        
   
        // Magic number is "RIFF", followed by 4 bytes, and then "WAVE"
        int chunkID = readIntBigEndian(dis);
        int chunkSize = readIntLittleEndian(dis);
        int fileFormat = readIntBigEndian(dis);
        if (chunkID != RIFF || fileFormat != WAVE) {
            throw new IOException("Invalid WAV file header.");
        }
        
        // Read each chunk. Only process the FMT and DATA chunks; ignore others.
        // Return once the data chunk is found.
        while (true) {
            chunkID = readIntBigEndian(dis);
            chunkSize = readIntLittleEndian(dis);
            
            if (chunkID == FMT) {
                int format = readShortLittleEndian(dis);
                numChannels = readShortLittleEndian(dis);
                sampleRate = readIntLittleEndian(dis);
                int avgByteRate = readIntLittleEndian(dis); // ignored
                int blockAlign = readShortLittleEndian(dis); // ignored
                int bitsPerSample = readShortLittleEndian(dis);
                
                // Sometimes 8012Hz to 8016Hz is used
                if (sampleRate >= 8000 && sampleRate <= 8100) {
                    sampleRate = 8000;
                }
                
                // Check basic format
                if (format != 1 || bitsPerSample != 16 || numChannels < 1 || numChannels > 2) {
                    throw new IOException("Not an 16-bit PCM mono or stereo file.");
                }
            }
            else if (chunkID == DATA) {
                if (sampleRate == -1) {
                    throw new IOException("No format in WAV file.");
                }
                
                ByteBuffer dataBuffer;
                if (!nativeBuffer && byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    byte[] data = new byte[chunkSize];
                    dis.readFully(data);
                    dataBuffer = ByteBuffer.wrap(data);
                }
                else {
                    if (nativeBuffer) {
                        dataBuffer = ByteBuffer.allocateDirect(chunkSize).order(byteOrder);
                    }
                    else {
                        dataBuffer = ByteBuffer.allocate(chunkSize).order(byteOrder);
                    }
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                        readToBuffer(in, dataBuffer, chunkSize);
                        dataBuffer.rewind();
                    }
                    else {
                        ShortBuffer shortBuffer = dataBuffer.asShortBuffer();
                        for (int i = 0; i < chunkSize / 2; i++) {
                            shortBuffer.put(readShortLittleEndian(dis));
                        }
                        shortBuffer.rewind();
                    }
                }
                return new WAVFile(dataBuffer, sampleRate, numChannels);
            }
            else {
                skipFully(dis, chunkSize);
            }
        }
    }
    
    private static void skipFully(DataInputStream in, int numBytes) throws IOException {
        while (numBytes > 0) {
            int skipped = in.skipBytes(numBytes);
            if (skipped == 0) {
                in.readByte();
                skipped++;
            }
            numBytes -= skipped;
        }
    }
    
    private static void readToBuffer(InputStream src, ByteBuffer dst, int numBytes) throws IOException {
        byte[] buffer = new byte[4096];
        while (numBytes > 0) {
            int bytesRead = src.read(buffer, 0, Math.min(buffer.length, numBytes));
            if (bytesRead == -1) {
                throw new EOFException("Error reading WAV file.");
            }
            dst.put(buffer, 0, bytesRead);
            numBytes -= bytesRead;
        }
    }
    
    private static int readIntBigEndian(DataInputStream in) throws IOException {
        return in.readInt();
    }
    
    private static short readShortLittleEndian(DataInputStream in) throws IOException {
        return (short)(
            in.readUnsignedByte() | (in.readUnsignedByte() << 8));
    }
    
    private static int readIntLittleEndian(DataInputStream in) throws IOException {
        return 
                in.readUnsignedByte() |
                (in.readUnsignedByte() << 8) | 
                (in.readUnsignedByte() << 16) | 
                (in.readUnsignedByte() << 24); 
    }
}

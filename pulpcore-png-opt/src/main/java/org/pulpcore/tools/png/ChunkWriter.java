/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
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
package org.pulpcore.tools.png;

import java.io.IOException;

/**
Writes a PNG chunk.
 */
public class ChunkWriter {

    public static final int CHUNK_IHDR = 0x49484452;
    public static final int CHUNK_PLTE = 0x504c5445;
    public static final int CHUNK_tRNS = 0x74524e53;
    public static final int CHUNK_IDAT = 0x49444154;
    public static final int CHUNK_IEND = 0x49454e44;
    public static final int CHUNK_gAMA = 0x67414d41;

    /** Number of extra bytes needed to stare a chunk. 4 bytes each for length, chunkType, and CRC. */
    public static final int CHUNK_OVERHEAD = 12;
    
    private final CRCOutputStream out;

    public ChunkWriter(CRCOutputStream out) {
        this.out = out;
    }

    public void writeChunk(int chunkType, byte[] data) throws IOException {
        writeChunk(chunkType, data, 0, data.length);
    }

    public void writeChunk(int chunkType, byte[] data, int offset, int length)
            throws IOException {
        out.writeInt(length);
        out.resetCRC();
        out.writeInt(chunkType);
        out.write(data, offset, length);
        out.writeInt(out.getCRC());
    }

    public long getBytesWritten() {
        return out.getBytesWritten();
    }
}

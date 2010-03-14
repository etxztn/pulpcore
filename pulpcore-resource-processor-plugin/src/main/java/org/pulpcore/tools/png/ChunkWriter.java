/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class ChunkWriter {
    
    public static final byte[] CHUNK_IHDR = { 0x49, 0x48, 0x44, 0x52 };
    public static final byte[] CHUNK_PLTE = { 0x50, 0x4c, 0x54, 0x45 };
    public static final byte[] CHUNK_tRNS = { 0x74, 0x52, 0x4e, 0x53 };
    public static final byte[] CHUNK_IDAT = { 0x49, 0x44, 0x41, 0x54 };
    public static final byte[] CHUNK_IEND = { 0x49, 0x45, 0x4e, 0x44 };
    
    /** Private ancillary chunk for PulpCore font information. */ 
    public static final byte[] CHUNK_foNT = { 0x66, 0x6f, 0x4e, 0x74 };
    
    /** Private ancillary chunk for PulpCore hot spot information. */
    public static final byte[] CHUNK_hoTS = { 0x68, 0x6f, 0x54, 0x73 };
    
    /** Private ancillary chunk for PulpCore animation information. */
    public static final byte[] CHUNK_anIM = { 0x61, 0x6e, 0x49, 0x6d };
    
    private final DataOutputStream os;
    private final CRCOutputStream crc = new CRCOutputStream();

    public ChunkWriter(DataOutputStream os) {
        this.os = os;
    }


    public void writeChunk(byte[] chunkType, byte[] data) throws IOException {
        writeChunk(chunkType, data, 0, data.length);
    }
    
    
    public void writeChunk(byte[] chunkType, byte[] data, int offset, int length) 
        throws IOException 
    {
        crc.resetCRC();
        crc.write(chunkType, 0, 4);
        crc.write(data, offset, length);
        
        os.writeInt(length);
        os.write(chunkType, 0, 4);
        os.write(data, offset, length);
        os.writeInt(crc.getCRC());
    }
    
    
    /*public void writeChunk(byte[] chunkType, ByteArrayOutputStream data) throws IOException { 
        crc.resetCRC();
        crc.write(chunkType, 0, 4);
        data.writeTo(crc);
        
        os.writeInt(data.size());
        os.write(chunkType, 0, 4);
        data.writeTo(os);
        os.writeInt(crc.getCRC());
    }*/
}

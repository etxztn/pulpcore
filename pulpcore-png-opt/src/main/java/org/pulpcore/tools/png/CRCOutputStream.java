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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
    CRC algorithm for PNG chunks. From http://www.w3.org/TR/PNG-CRCAppendix.html
*/
public class CRCOutputStream extends FilterOutputStream {
    
    /** Table of CRCs of all 8-bit messages. */
    private static final int[] CRC_TABLE = new int[256];
   
    static {
        // Make the table for a fast CRC. 
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) {
                if ((c & 1) == 1) {
                    c = 0xedb88320 ^ (c >>> 1);
                }
                else {
                    c >>>= 1;
                }
            }
            CRC_TABLE[n] = c;
        }
    }

    private byte[] buffer = new byte[8];
    private int crc = 0xffffffff;
    private long bytesWritten = 0;

    public CRCOutputStream(OutputStream out) {
        super(out);
    }
    
    public void resetCRC() {
        crc = 0xffffffff;
    }
    
    public int getCRC() {
        return crc ^ 0xffffffff;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public void writeInt(int value) throws IOException {
        buffer[0] = (byte)(value >> 24);
        buffer[1] = (byte)(value >> 16);
        buffer[2] = (byte)(value >> 8);
        buffer[3] = (byte)value;
        write(buffer, 0, 4);
    }

    public void writeLong(long value) throws IOException {
        buffer[0] = (byte)(value >> 56);
        buffer[1] = (byte)(value >> 48);
        buffer[2] = (byte)(value >> 40);
        buffer[3] = (byte)(value >> 32);
        buffer[4] = (byte)(value >> 24);
        buffer[5] = (byte)(value >> 16);
        buffer[6] = (byte)(value >> 8);
        buffer[7] = (byte)value;
        write(buffer, 0, 8);
    }
    
    /**
        Writes a single byte to the underlying stream and updates the running CRC.
    */
    @Override
    public void write(int b) throws IOException {
        out.write(b);
        byte bb = (byte)b;
        crc = CRC_TABLE[(crc ^ bb) & 0xff] ^ (crc >>> 8);
        bytesWritten++;
    }

    /**
        Writes bytes to the underlying stream and updates the running CRC.
    */
    @Override
    public void write(byte[] buf, int offset, int length) throws IOException {
        out.write(buf, offset, length);
        for (int n = offset; n < offset + length; n++) {
            crc = CRC_TABLE[(crc ^ buf[n]) & 0xff] ^ (crc >>> 8);
        }
        bytesWritten += length;
    }
}

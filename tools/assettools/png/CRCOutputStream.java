/*
    Copyright (c) 2007, Interactive Pulp, LLC
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

package pulpcore.assettools.png;

import java.io.OutputStream;

/**
    CRC algorithm for PNG chunks. From http://www.w3.org/TR/PNG-CRCAppendix.html
*/
public class CRCOutputStream extends OutputStream {
    
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
    
    private int crc = 0xffffffff;
    
    
    public void resetCRC() {
        crc = 0xffffffff;
    }

    
    public int getCRC() {
        return crc ^ 0xffffffff;
    }
    
    
    /**
        Update a running CRC with a single byte. 
    */
    public void write(int b) {
        byte bb = (byte)b;
        crc = CRC_TABLE[(crc ^ bb) & 0xff] ^ (crc >>> 8);
    }
    

    /**
        Update a running CRC. 
    */
    public void write(byte[] buf, int offset, int length) {
        for (int n = offset; n < offset + length; n++) {
            crc = CRC_TABLE[(crc ^ buf[n]) & 0xff] ^ (crc >>> 8);
        }
    }
}

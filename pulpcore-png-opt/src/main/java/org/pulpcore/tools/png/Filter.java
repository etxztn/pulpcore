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

public class Filter {
    
    public static final int TYPE_NONE = 0;
    public static final int TYPE_SUB = 1;
    public static final int TYPE_UP = 2;
    public static final int TYPE_AVERAGE = 3;
    public static final int TYPE_PAETH = 4;
    public static final int NUM_TYPES = 5;

    private Filter() {
    }

    public static void encodeFilter(byte[] curr, byte[] prev, byte[] result, int bpp, 
            int filterType) {
        int length = curr.length;
        
        // For all x < 0, assume Raw(x) = 0.
        switch (filterType) {
            case TYPE_NONE:
                System.arraycopy(curr, 0, result, 0, length);
                break;
                
            case TYPE_SUB:
                // Sub(x) = Raw(x) - Raw(x-bpp)
                for (int i = 0; i < bpp; i++) {
                    result[i] = curr[i];
                }
                for (int i = bpp; i < length; i++) {
                    result[i] = (byte)(curr[i] - curr[i - bpp]);
                }
                break;
                
            case TYPE_UP:
                // Up(x) = Raw(x) - Prior(x)
                for (int i = 0; i < length; i++) {
                    result[i] = (byte)(curr[i] - prev[i]);
                }
                break;
                
            case TYPE_AVERAGE:
                // Average(x) = Raw(x) - floor((Raw(x-bpp)+Prior(x))/2)
                for (int i = 0; i < bpp; i++) {
                    result[i] = (byte)(curr[i] - ((prev[i] & 0xff) >> 1));
                }
                for (int i = bpp; i < length; i++) {
                    result[i] = (byte)(curr[i] - (((curr[i - bpp] & 0xff) + (prev[i] & 0xff)) >> 1));
                }
                break;
              
            case TYPE_PAETH:
                // Paeth(x) = Raw(x) - PaethPredictor(Raw(x-bpp), Prior(x), Prior(x-bpp))
                for (int i = 0; i < bpp; i++) {
                    result[i] = (byte)(curr[i] - prev[i]);
                }
                for (int i = bpp; i < length; i++) {
                    result[i] = (byte)(curr[i] - 
                        paethPredictor(curr[i - bpp] & 0xff, prev[i] & 0xff, prev[i - bpp] & 0xff));
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported filter: " + filterType);
        }
    }
    
    // a = left, b = above, c = upper left
    private static int paethPredictor(int a, int b, int c) {
        
        // Initial estimate
        int p = a + b - c;
        
        // Distances to a, b, c
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
    
        // Return nearest of a,b,c, breaking ties in order a,b,c.
        if (pa <= pb && pa <= pc) {
            return a;
        }
        else if (pb <= pc) {
            return b;
        }
        else {
            return c;
        }
    }
}

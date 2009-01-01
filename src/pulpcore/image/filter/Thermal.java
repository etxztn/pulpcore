/*
    Copyright (c) 2009, Interactive Pulp, LLC
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
package pulpcore.image.filter;

import pulpcore.image.CoreImage;

public final class Thermal extends Filter {

    public Filter copy() {
        Filter in = getInput();
        Filter copy = new Thermal();
        copy.setInput(in == null ? null : in.copy());
        return copy;
    }

    protected void filter(CoreImage src, CoreImage dst) {
	
        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();

        for(int i = 0; i < srcPixels.length; i++) {
            int srcRGB = srcPixels[i];

            int srcR = (srcRGB >> 16) & 0xff;
            int srcG = (srcRGB >> 8) & 0xff;
            int srcB = srcRGB & 0xff;

            // add together 30% of red value, 59% of green, 11% of blue
            int dstGray = ((srcR * 77) >> 8) + ((srcG * 150) >>8) + ((srcB  * 28) >> 8);

            // rearrange the grayscale colors to obtain different values from white to red to green to blue to black
            // real thermal filter have 10 values. See examples here : http://en.wikipedia.org/wiki/Thermal_imaging
            // but this "fake thermal filter" have only 4 ranges to facilitate calculation.
            if (dstGray >= 0 && dstGray <= 63) {
                // from white to red  0xffffffff -> 0xffff0000
                int val = 0xff - (dstGray << 2);
                dstPixels[i] = 0xffff0000 | (val << 8) | val;
            }
            else if (dstGray >= 64 && dstGray <= 127) {
                // from red to green 0xffff0000 -> 0xff00ff00
                int val1 = 0xff - ((dstGray-64) << 2);
                int val2 = (dstGray-64) << 2;
                dstPixels[i] = 0xff000000 | (val1 << 16) | (val2 << 8 );
            }
            else if (dstGray >= 128 && dstGray <= 191) {
                // from green to blue 0xff00ff00 -> 0xff0000ff
                int val1 = 0xff - ((dstGray-128) << 2);
                int val2 = (dstGray-128) << 2;
                dstPixels[i] = 0xff000000 | (val1 << 8) | val2;
            }
            else {
                // from blue to black 0xff0000ff -> 0xff000000
                int val = 0xff - ((dstGray-192) << 2);
                dstPixels[i] = 0xff000000 | val;
            }
        }
	}
}

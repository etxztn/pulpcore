/*
    Copyright (c) 2011, Interactive Pulp, LLC
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
package org.pulpcore.tools.png.heuristic;

/**
 Gets the estimates number of bits needed to store the data.
 */
public class EntropyHeuristic extends AdaptiveFilterHeuristic {

    private int[] prevCount = new int[256];

    @Override
    protected void setPrevScanline(byte[] filteredPrevScanline) {
        prevCount = new int[256];
        for (int i = 0; i < filteredPrevScanline.length; i++) {
            int v = filteredPrevScanline[i] & 255;
            prevCount[v]++;
        }
    }

    public double getCompressability(byte filter, byte[] scanline) {
        // Get the count of each appearance of each value.
        int[] count = new int[256];
        count[filter & 255]++;
        for (int i = 0; i < scanline.length; i++) {
            count[scanline[i] & 255]++;
        }
        // Num values in the set: prevScanline, getFilterType, scanline
        double sum = scanline.length * 2 + 1;
        // Calculate the entropy of both the filteredPrevScanline and the current one.
        double H = 0;
        for (int i = 0; i < 256; i++) {
            int c = count[i] + prevCount[i];
            if (c != 0) {
                double p = c / sum;
                H += p * Math.log(p);
            }
        }
        return -H / Math.log(2);
    }

    @Override
    protected boolean shouldUsePreviousFilter(double bestCompressability, double prevCompressability) {
        return prevCompressability - bestCompressability <  1.0 / 32.0;
    }

}

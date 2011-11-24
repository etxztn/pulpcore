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

import org.pulpcore.tools.png.Filter;

/**
 * Chooses the best of all the available filters for each scanline
 */
public abstract class AdaptiveFilterHeuristic extends FilterHeuristic {

    @Override
    public int getFilterType(int prevFilterType, byte[] rawScanline, byte[] rawPrevScanline,
            byte[] filteredScanline, byte[] filtredPrevScanline, int bytesPerPixel) {
        return getFilterType(prevFilterType, rawScanline, rawPrevScanline,
                filteredScanline, filtredPrevScanline, bytesPerPixel, DEFAULT_FILTER_TYPES);
    }

    protected int getFilterType(int prevFilterType, byte[] rawScanline, byte[] rawPrevScanline,
            byte[] filteredScanline, byte[] filtredPrevScanline, int bytesPerPixel, int[] filterTypes) {
        double[] compressabilityList = new double[filterTypes.length];
        int bestFilterType = Filter.TYPE_NONE;
        double bestCompressability = Double.POSITIVE_INFINITY;
        double prevCompressability = Double.POSITIVE_INFINITY;
        setPrevScanline(filtredPrevScanline);
        for (int i = 0; i < filterTypes.length; i++) {
            int filterType = filterTypes[i];
            double compressability;
            if (filterType == Filter.TYPE_NONE) {
                compressability = getCompressability((byte) filterType, rawScanline);
            }
            else {
                Filter.encodeFilter(rawScanline, rawPrevScanline, filteredScanline, bytesPerPixel, filterType);
                compressability = getCompressability((byte) filterType, filteredScanline);
            }
            if (filterType == prevFilterType) {
                prevCompressability = compressability;
            }
            compressabilityList[i] = compressability;
            if (compressability < bestCompressability) {
                bestFilterType = filterType;
                bestCompressability = compressability;
            }
        }
        // Weighted
        if (bestFilterType != prevFilterType && shouldUsePreviousFilter(bestCompressability, prevCompressability)) {
            bestFilterType = prevFilterType;
        }
        return bestFilterType;
    }

    /**
     Returns true if the previous filter type should be used instead of the best filter type.
     This may be useful when changing the filter type often creates worse long-term results.
     The prevCompressability param will always be >= the bestCompressability param.
     The default implementation returns false.
     */
    protected boolean shouldUsePreviousFilter(double bestCompressability, double prevCompressability) {
        return false;
    }

    /**
    The data will mutate - clone it if you need a copy.
     */
    protected void setPrevScanline(byte[] filtredPrevScanline) {
    }

    /**
    @return a positive number ranking the compressability of the data, where smaller
    numbers signify that the data is estimated to compress better than larger numbers.
     */
    protected abstract double getCompressability(byte filter, byte[] scanline);
}

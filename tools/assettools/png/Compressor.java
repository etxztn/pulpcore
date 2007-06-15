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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class Compressor {
    
    private static final int VERBOSE = 0;
    
    public static final int OPTIMIZATION_NONE = 0;
    public static final int OPTIMIZATION_DEFAULT = 2;
    public static final int OPTIMIZATION_MAX = 5;

    /*
        Optimization |                ZLIB        ZLIB compression                Optimized
           Level     |  Filters    Strategies          levels          Trials   BitDepth < 8 ?
        -------------+--------------------------------------------------------------------------
         0           |     0           0                 9               1            No
         1           |    0,5          0                 9               2            No
         2 (default) |   0,5,6        0-2                9               9            No
         3           |   0,5-8        0-2               8,9             30            Yes
         4           |   0,2,5-9      0-2              3,7-9            84            Yes
         5           |    0-9         0-2               1-9             243           Yes
    
         
        Results with Milpa 1.0 files:
        Level 2 - 594,696
        Level 5 - 593,990
    
        "optipng -force" - 593,829
        "optipng -o7 -force" - 593,045
    */
    


    private static final int[][] DEFLATE_STRATEGIES = {
        { Deflater.DEFAULT_STRATEGY },
        { Deflater.DEFAULT_STRATEGY },
        { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY },
        { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY },
        { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY },
        { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY },
    };
    
    private static final int[][] DEFLATE_LEVELS = {
        { 9 },
        { 9 },
        { 9 },
        { 9, 8 },
        { 9, 8, 7, 3 },
        { 9, 8, 7, 6, 5, 4, 3, 2, 1 },
    };
    
    private static final int[][] FILTER_STRATEGIES = {
        { 0 },
        { 0, 5 },
        { 0, 5, 6 },
        { 0, 5, 6, 7, 8 },
        { 0, 5, 2, 6, 7, 8, 9 },
        { 0, 5, 1, 2, 3, 4, 6, 7, 8, 9 },
    };
    
    
    private static final FilterStrategy[] ALL_FILTER_STRATEGIES = {
        // The LIBPNG filters
        /* 0 */ null, 
        /* 1 */ new FixedFilterStrategy(Filter.TYPE_SUB), 
        /* 2 */ new FixedFilterStrategy(Filter.TYPE_UP),
        /* 3 */ new FixedFilterStrategy(Filter.TYPE_AVERAGE),
        /* 4 */ new FixedFilterStrategy(Filter.TYPE_PAETH), 
        /* 5 */ new LIBPNGStrategy(),
        
        // Alternatives to the adaptive filter. 
        // It would be nice to have one filter that works best in all caes,
        // but each of these filters work differently depending on the source data. 
        /* 6 */ new RedundancyStrategy(),
        /* 7 */ new LIBPNGStrategyAlternative(), 
        /* 8 */ new EntropyStrategy(), 
        /* 9 */ new VarianceStrategy(),
    };    
    
       
    public byte[] compress(byte[][] scanlines, int bitDepth, int bytesPerPixel, int optimizationLevel) { 
        ArrayOutputStream out;
        int deflateStrategy;
        int deflateLevel;
        int filterStrategy;
        
        if (optimizationLevel == OPTIMIZATION_NONE || 
            (bitDepth < 8 && optimizationLevel <= 2))
        {
            // Don't try any filters or ZLIB options
            filterStrategy = 0;
            deflateStrategy = Deflater.DEFAULT_STRATEGY;
            deflateLevel = Deflater.BEST_COMPRESSION;
            out = new ArrayOutputStream(false);
        }
        else {
            // Try all combinations and choose the one that compresses the best.
            int[] deflateStrategies = DEFLATE_STRATEGIES[optimizationLevel];
            int[] deflateLevels = DEFLATE_LEVELS[optimizationLevel];
            int[] filterStrategies = FILTER_STRATEGIES[optimizationLevel];
            
            filterStrategy = filterStrategies[0];
            deflateStrategy = deflateStrategies[0];
            deflateLevel = deflateLevels[0];
            int bestSize = Integer.MAX_VALUE;
            
            for (int i = 0; i < deflateLevels.length; i++) {
                int currDeflateLevel = deflateLevels[i];
                
                for (int j = 0; j < deflateStrategies.length; j++) {
                    int currDeflateStrategy = deflateStrategies[j];
                    
                    Deflater deflater = new Deflater(currDeflateLevel);
                    deflater.setStrategy(currDeflateStrategy);
                    
                    for (int k = 0; k < filterStrategies.length; k++) {
                        int currFilterStrategy = filterStrategies[k];
                        int size = getCompressedSize(scanlines, bytesPerPixel, 
                            ALL_FILTER_STRATEGIES[currFilterStrategy], deflater, bestSize);
                    
                        if (VERBOSE >= 2) {
                            String s;
                            if (size < 0) {
                                s = "(Too big. Abandoned at " + (-size) + "%)";
                            }
                            else {
                                s = "IDAT size = " + size;
                            }
                            System.out.println(
                                " f = " + currFilterStrategy + 
                                " d = " + currDeflateStrategy +
                                " l = " + currDeflateLevel +
                                " " + s);
                        }
                    
                        if (size > 0 && size < bestSize) {
                            filterStrategy = currFilterStrategy;
                            deflateStrategy = currDeflateStrategy;
                            deflateLevel = currDeflateLevel;
                            bestSize = size;
                        }
                    }
                }
            }
            
            if (VERBOSE >= 2) {
                System.out.println("Best parameters:");
            }
            if (VERBOSE >= 1) {
                System.out.println(
                    " f = " + filterStrategy + 
                    " d = " + deflateStrategy +
                    " l = " + deflateLevel +
                    " IDAT size = " + bestSize);
            }
            
            out = new ArrayOutputStream(bestSize);
        }
        
        Deflater deflater = new Deflater(deflateLevel);
        deflater.setStrategy(deflateStrategy);
        
        try {
            compressScanlines(scanlines, bytesPerPixel, 
                ALL_FILTER_STRATEGIES[filterStrategy], deflater, Integer.MAX_VALUE, out);
        }
        catch (IOException ex) {
            // Won't happen with underlying ArrayOutputStream
        }
        return out.getArray();
    }

    
    private int getCompressedSize(byte[][] scanlines, int bytesPerPixel, 
        FilterStrategy filterStrategy, Deflater deflater, int maxSize) 
    {
        ArrayOutputStream out = new ArrayOutputStream(true);
        try {
            compressScanlines(scanlines, bytesPerPixel, filterStrategy, deflater, maxSize, out);
        }
        catch (IOException ex) {
            // Won't happen with underlying ArrayOutputStream
        }
        return out.size();
    }
    
    
    private void compressScanlines(byte[][] scanlines, int bytesPerPixel, 
        FilterStrategy filterStrategy, Deflater deflater, int maxSize,
        ArrayOutputStream os) 
        throws IOException
    {
        int bytesPerScanline = scanlines[0].length;
        
        // First prevScanline must be filled with zeros
        byte[] prevScanline = new byte[bytesPerScanline];
        byte[] filteredScanline = new byte[bytesPerScanline];
        
        if (filterStrategy != null) {
            filterStrategy.reset();
        }
        deflater.reset();
        DeflaterOutputStream out = new DeflaterOutputStream(os, deflater);
        
        for (int i = 0; i < scanlines.length; i++) {
            
            byte[] currScanline = scanlines[i];
            
            if (filterStrategy == null) {
                // No filtering
                out.write(0);
                out.write(currScanline);
            }
            else {
                int filterType = filterStrategy.filter(currScanline, prevScanline, filteredScanline,
                    bytesPerPixel);
                out.write(filterType);
                out.write(filteredScanline);        
            }
            
            // Exit if we've already hit the max size
            if (os.size() > maxSize) {
                out.close();
                int percent = 100 * i / scanlines.length;
                os.bytesWritten = -percent;
                return;
            }
                    
            prevScanline = currScanline;
        }
        
        out.close();
    }
    
    
    //
    // Filter strategy
    //
    
    
    public abstract static class FilterStrategy {
        public abstract int filter(byte[] rawScanline, byte[] rawPrevScanline, 
            byte[] filteredScanline, int bytesPerPixel);
        
        public void reset() { };
    }
    
    private static class FixedFilterStrategy extends FilterStrategy {
        
        private final int filterType;
        
        public FixedFilterStrategy(int filterType) {
            this.filterType = filterType;
        }
        
        public int filter(byte[] rawScanline, byte[] rawPrevScanline, byte[] filteredScanline, 
            int bytesPerPixel)
        {
            Filter.encodeFilter(rawScanline, rawPrevScanline, filteredScanline, bytesPerPixel, 
                filterType);
            return filterType;
        }
    }
    
    
    /**
        Chooses the best of all the available filters for each scanline
    */
    private abstract static class AdaptiveFilterStrategy extends FilterStrategy {
        
        
        public int filter(byte[] rawScanline, byte[] rawPrevScanline, byte[] filteredScanline, 
            int bytesPerPixel) {

            int filterType = 0;
            double bestCompressability = Double.POSITIVE_INFINITY;
            
            for (int j = 0; j < Filter.NUM_TYPES; j++) {
                Filter.encodeFilter(rawScanline, rawPrevScanline, filteredScanline, bytesPerPixel, 
                    j);
                double compressability = getCompressability((byte)filterType, filteredScanline);
                if (compressability < bestCompressability) {
                    filterType = j;
                    bestCompressability = compressability;
                }
            }
            
            Filter.encodeFilter(rawScanline, rawPrevScanline, filteredScanline, bytesPerPixel, 
                filterType);
            return filterType;
        }
        
        
        /**
            @return a positive number ranking the compressability of the data, where smaller
            numbers signify that the data is estimated to compress better than larger numbers. 
        */
        protected abstract double getCompressability(byte filter, byte[] scanline);
        
    }
    
    // libpng strategy  (best adaptive strategy in many cases)
    private static class LIBPNGStrategy extends AdaptiveFilterStrategy {
    
        public double getCompressability(byte filter, byte[] scanline) {
            int sum = Math.abs(filter);
            for (int i = 0; i < scanline.length; i++) {
                sum += Math.abs(scanline[i]);
            }
            
            return sum;
        }
    }
    
    
    private static class LIBPNGStrategyAlternative extends AdaptiveFilterStrategy {
    
        public double getCompressability(byte filter, byte[] scanline) {
            int sum = filter;
            for (int i = 0; i < scanline.length; i++) {
                sum += scanline[i];
            }
            
            return Math.abs(sum);
        }
    }
    
    
    private static class VarianceStrategy extends AdaptiveFilterStrategy {
    
        // Lower values for less variance in data
        public double getCompressability(byte filter, byte[] scanline) {
            
            int sum = Math.abs(filter);
            for (int i = 0; i < scanline.length; i++) {
                sum += Math.abs(scanline[i]);
            }
            
            double mean = (double)sum / (scanline.length + 1);
            double s = Math.abs(filter) - mean;
            double var = s * s;
            for (int i = 0; i < scanline.length; i++) {
                s = Math.abs(scanline[i]) - mean;
                var += s*s;
            }
            
            return var / (scanline.length + 1);
        }
    }
    
    //  (best adaptive strategy in many cases)
    private static class RedundancyStrategy extends AdaptiveFilterStrategy {
        
        private static int[] count = new int[256];
        
        public double getCompressability(byte filter, byte[] scanline) {
            // Get the count of each appearance of each value.
            for (int i = 0; i < 256; i++) {
                count[i] = 0;
            }
            count[filter & 0xff]++;
            for (int i = 0; i < scanline.length; i++) {
                count[scanline[i] & 0xff]++;
            }
            
            // If there's only one value, return 0
            int numValuesRepresented = 0;
            int sum = 0;
            for (int i = 0; i < 256; i++) {
                if (count[i] != 0) {
                    numValuesRepresented++;
                    sum += count[i];
                }
            }
            if (numValuesRepresented == 1) {
                return 0;
            }
            
            // Calculate the variance in the counts
            double mean = (double)sum / numValuesRepresented;
            double var = 0;
            for (int i = 0; i < 256; i++) {
                if (count[i] != 0) {
                    double s = count[i] - mean;
                    var+=s*s;
                }
            }
            
            var /= numValuesRepresented;
            
            // We want a lot variance, so return the inverse of the variance
            double inv;
            if (var == 0) {
                return Double.POSITIVE_INFINITY;
                //inv = 1 / (sum * sum);
            }
            else {
                inv = 1 / var;
            }
            // We want a lot of variance in a small sample space
            return inv * numValuesRepresented;
        }
    }
    
    
    private static class EntropyStrategy extends AdaptiveFilterStrategy {
        
        private static int[] count = new int[256];
        private static double log2 = Math.log(2);
        
        public double getCompressability(byte filter, byte[] scanline) {
            // Get the count of each appearance of each value.
            for (int i = 0; i < 256; i++) {
                count[i] = 0;
            }
            count[filter & 0xff]++;
            for (int i = 0; i < scanline.length; i++) {
                count[scanline[i] & 0xff]++;
            }
            
            // If there's only one value, return 0
            int numValuesRepresented = 0;
            for (int i = 0; i < 256; i++) {
                if (count[i] != 0) {
                    numValuesRepresented++;
                }
            }
            if (numValuesRepresented == 1) {
                return 0;
            }
            
            // Calculate the entropy
            double sum = scanline.length + 1;
            double H = 0;
            for (int i = 0; i < 256; i++) {
                if (count[i] != 0) {
                    double p = count[i] / sum;
                    H += Math.abs(p * Math.log(p) / log2);
                }
            }
            
            return H;
        }
    }
    
    
    private static class ArrayOutputStream extends ByteArrayOutputStream {
        
        private final boolean countOnly;
        private int bytesWritten = 0;
        
        
        public ArrayOutputStream(int bufferSize) {
            super(bufferSize);
            this.countOnly = false;
        }
        
        
        public ArrayOutputStream(boolean countOnly) {
            this.countOnly = countOnly;
        }
        
        
        public void write(int b) {
            bytesWritten++;
            if (!countOnly) {
                super.write(b);
            }
        }
        
        
        public void write(byte[] b, int off, int len) {
            bytesWritten += len;
            if (!countOnly) {
                super.write(b, off, len);
            }
        }
        
        
        public byte[] getArray() {
            if (super.buf.length == bytesWritten) {
                return super.buf;
            }
            else {
                return super.toByteArray();
            }
        }
        
        
        public int size() {
            return bytesWritten;
        }
    }
    
}

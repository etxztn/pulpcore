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

import org.pulpcore.tools.png.heuristic.RunCountHeuristic;
import org.pulpcore.tools.png.heuristic.EntropyHeuristic;
import org.pulpcore.tools.png.heuristic.UniqueSymbolCountHeuristic;
import org.pulpcore.tools.png.heuristic.DefaultHeuristic;
import org.pulpcore.tools.png.heuristic.FixedFilter;
import org.pulpcore.tools.png.heuristic.FilterHeuristic;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.pulpcore.tools.png.heuristic.MeanSquareHeuristic;

/**
 Compresses image scanlines using a specific set of deflate parameters.
 */
public class ScanlineDeflater {

    public static final int NUM_DEFLATE_STRATEGIES = 3;
    public static final int NUM_DEFLATE_LEVELS = 9;
    public static final int NUM_FILTER_HEURISTICS = 8;

    private static final FilterHeuristic createFilterHeuristic(int h) {
        switch (h) {
            // libpng filter heuristics
            case 0: return null; 
            case 1: return new FixedFilter(Filter.TYPE_SUB); 
            case 2: return new FixedFilter(Filter.TYPE_UP); 
            case 3: return new FixedFilter(Filter.TYPE_AVERAGE);
            case 4: return new FixedFilter(Filter.TYPE_PAETH);
            case 5: return new DefaultHeuristic();

            // Custom heuristics not found in libpng.

            case 6: return new UniqueSymbolCountHeuristic();
            case 7: return new EntropyHeuristic();
            case 8: return new RunCountHeuristic(); 
            case 9: return new MeanSquareHeuristic();
            default: throw new IllegalArgumentException("No such filter heuristic " + h);
        }
    }
    
    private final int deflateLevel;
    private final int deflateStrategy;
    private final int filterHeuristic;

    public ScanlineDeflater(int deflateLevel, int deflateStrategy, int filterHeuristic) {
        this.deflateLevel = deflateLevel;
        this.deflateStrategy = deflateStrategy;
        this.filterHeuristic = filterHeuristic;
    }

    public int getDeflateLevel() {
        return deflateLevel;
    }

    public int getDeflateStrategy() {
        return deflateStrategy;
    }

    public int getFilterHeuristic() {
        return filterHeuristic;
    }

    public int getCompressedSize(byte[][] scanlines, int bitsPerPixel) {
        return getCompressedSize(scanlines, bitsPerPixel, new AtomicInteger(Integer.MAX_VALUE));
    }

    /**
     @return the compressed size, or a negative value if the compression was aborted because
     the size was larger than maxSize.
     */
    public int getCompressedSize(byte[][] scanlines, int bitsPerPixel, AtomicInteger maxSize) {
        try {
            return compress(scanlines, bitsPerPixel, maxSize, new NullOutputStream());
        }
        catch (IOException ex) {
            // Won't happen with underlying NullOutputStream
            return 0;
        }
    }

    public byte[] compress(byte[][] scanlines, int bitsPerPixel) {
        return compress(scanlines, bitsPerPixel, -1);
    }

    public byte[] compress(byte[][] scanlines, int bitsPerPixel, AtomicInteger maxSize) {
        ArrayOutputStream out = new ArrayOutputStream(4096);
        int size = -1;
        try {
            size = compress(scanlines, bitsPerPixel, maxSize, out);
        }
        catch (IOException ex) {
            // Won't happen with underlying ArrayOutputStream
        }
        if (size < 0) {
            return null;
        }
        else {
            return out.getArray();
        }
    }

    public byte[] compress(byte[][] scanlines, int bitsPerPixel, int estimatedCompressedSize) {
        if (estimatedCompressedSize <= 0) {
            estimatedCompressedSize = 4096;
        }

        ArrayOutputStream out = new ArrayOutputStream(estimatedCompressedSize);
        int size = -1;
        try {
            size = compress(scanlines, bitsPerPixel, new AtomicInteger(Integer.MAX_VALUE), out);
        }
        catch (IOException ex) {
            // Won't happen with underlying ArrayOutputStream
        }
        if (size < 0) {
            return null;
        }
        else {
            return out.getArray();
        }
    }

    private int compress(byte[][] scanlines, int bitsPerPixel, AtomicInteger maxSize, OutputStream os) throws IOException {

        FilterHeuristic filter = createFilterHeuristic(filterHeuristic);
        int bytesPerScanline = scanlines[0].length;
        int bytesPerPixel = (bitsPerPixel + 7) / 8;

        // First prevScanline must be filled with zeros
        byte[] prevScanline = new byte[bytesPerScanline];
        byte[] prevFilteredScanline = prevScanline;
        byte[] filteredScanline = new byte[bytesPerScanline];
        byte[] filterType = new byte[1];

        // Attempts of using both Java's Deflater and JZLib. JZLib can use memory level 9,
        // while Java's deflater only allows memory level 8. However, the
        // Waterloo files came out larger using JZLib. Perhaps JZlib is based on an older
        // version of zlib that is missing some optimizations? Or some JZLib defaults
        // are different?
        //
        // The best possible optimization right now would be delflate optimizations:
        // 1. Use an updated zlib.
        // 2. Try bother memory level 8 and 9.
        // 3. Try deflate strategy Z_RLE.
        Deflater deflater = new Deflater(deflateLevel);
        deflater.setStrategy(deflateStrategy);
        DeflaterOutputStream out = new DeflaterOutputStream(os, deflater);
//        ZOutputStream out = new ZOutputStream(os, deflateLevel) {
//            {
//                z.deflateParams(deflateLevel, deflateStrategy);
//            }
//        };

        for (int i = 0; i < scanlines.length; i++) {

            byte[] currScanline = scanlines[i];
            int lastFilterType = filterType[0];
            filterType[0] = 0;
            if (filter != null) {
                filterType[0] = (byte)filter.getFilterType(lastFilterType, 
                        currScanline, prevScanline, filteredScanline,
                        prevFilteredScanline, bytesPerPixel);
            }

            out.write(filterType);
            if (filterType[0] == 0) {
                out.write(currScanline);
                prevFilteredScanline = currScanline;
            }
            else {
                Filter.encodeFilter(currScanline, prevScanline, filteredScanline, bytesPerPixel,
                  filterType[0]);

                out.write(filteredScanline);
                prevFilteredScanline = filteredScanline;
            }

            // Exit if we've already hit the max size
//            if (out.getTotalOut() > maxSize.get()) {
//                out.close();
//                return -1;
//            }
            if (deflater.getTotalOut() > maxSize.get()) {
                out.close();
                deflater.end();
                return -1;
            }

            prevScanline = currScanline;
        }

        out.close();
        int totalOut = deflater.getTotalOut();
        deflater.end();

//        int totalOut = (int)out.getTotalOut();
//        out.close();
        return totalOut;
    }

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }

    private static class ArrayOutputStream extends ByteArrayOutputStream {


        public ArrayOutputStream(int bufferSize) {
            super(bufferSize);
        }

        public ArrayOutputStream() {

        }

        public byte[] getArray() {
            if (super.buf.length == size()) {
                return super.buf;
            }
            else {
                // Even for large images, in my benchmarks this only takes about 100 microseconds
                return super.toByteArray();
            }
        }
    }
}

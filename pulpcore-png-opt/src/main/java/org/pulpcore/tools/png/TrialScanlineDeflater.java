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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Chooses the best compression strategy from several different trials. This class runs the
 different trials in parallel and waits for them to all to finish, then returns the best results.
 */
public class TrialScanlineDeflater {

    private final int[] deflateLevels;
    private final int[] deflateStrategies;
    private final int[] filterHeuristics;

    TrialScanlineDeflater(int[] deflateLevels, int[] deflateStrategies, int[] filterHeuristics) {
        this.deflateLevels = deflateLevels.clone();
        this.deflateStrategies = deflateStrategies.clone();
        this.filterHeuristics = filterHeuristics.clone();
    }

    private static class CompressTask implements Callable<Void> {
        private final byte[][] scanlines;
        private final int bitsPerPixel;
        private final AtomicInteger maxSize;
        private final ScanlineDeflater deflater;
        private int size = -1;
        private boolean setKeepCompressedCopy;
        private SoftReference<byte[]> cachedData;

        public CompressTask(byte[][] scanlines, int bitsPerPixel, AtomicInteger maxSize, 
                int deflateLevel, int deflateStrategy, int filterHeuristic) {
            this.scanlines = scanlines;
            this.bitsPerPixel = bitsPerPixel;
            this.maxSize = maxSize;
            this.deflater = new ScanlineDeflater(deflateLevel, deflateStrategy, filterHeuristic);
        }

        public int getDeflateLevel() {
            return deflater.getDeflateLevel();
        }

        public int getDeflateStrategy() {
            return deflater.getDeflateStrategy();
        }

        public int getFilterHeuristic() {
            return deflater.getFilterHeuristic();
        }

        public Void call() {
            if (setKeepCompressedCopy) {
                // Attempt to keep the data in a SoftReference
                byte[] data = deflater.compress(scanlines, bitsPerPixel, maxSize);
                if (data == null) {
                    size = -1;
                }
                else {
                    size = data.length;
                    cachedData = new SoftReference<byte[]>(data);
                }
            }
            else {
                // Calculate, but don't keep the data
                size = calculateCompressedSize();
            }
            if (size > 0) {
                synchronized (maxSize) {
                    if (size < maxSize.get()) {
                        maxSize.set(size);
                    }
                }
            }
            return null;
        }

        public int getSize() {
            return size;
        }
        
        /**
         @return the compressed size, or a negative value if the compression was aborted because
         the size was going to be larger than maxSize.
         */
        private int calculateCompressedSize() {
            return deflater.getCompressedSize(scanlines, bitsPerPixel, maxSize);
        }

        public byte[] getCompressedData() {
            byte[] data = cachedData == null ? null : cachedData.get();
            if (data != null) {
                return data;
            }
            else {
                return deflater.compress(scanlines, bitsPerPixel, size);
            }
        }

        private void setKeepCompressedCopy(boolean setKeepCompressedCopy) {
            this.setKeepCompressedCopy = setKeepCompressedCopy;
        }
    }

    public static class Results {
        private final byte[] compressedData;
        private final int deflateLevel;
        private final int deflateStrategy;
        private final int filterHeuristic;

        private Results(byte[] compressedData, int deflateLevel, int deflateStrategy, int filterHeuristic) {
            this.compressedData = compressedData;
            this.deflateLevel = deflateLevel;
            this.deflateStrategy = deflateStrategy;
            this.filterHeuristic = filterHeuristic;
        }

        public byte[] getCompressedData() {
            return compressedData;
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

        @Override
        public String toString() {
            return "Params:" +
                    " zc = " + getDeflateLevel() +
                    " zs = " + getDeflateStrategy() +
                    " f = " + getFilterHeuristic() +
                    ", IDAT size = " + getCompressedData().length;

        }
    }

    public Results compress(byte[][] scanlines, int bitsPerPixel) {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService es = Executors.newFixedThreadPool(numProcessors);
        Results results = compress(scanlines, bitsPerPixel, es);
        es.shutdown();
        return results;
    }

    public Results compress(byte[][] scanlines, int bitsPerPixel, ExecutorService es) {

        AtomicInteger bestSize = new AtomicInteger(Integer.MAX_VALUE);

        // Create the CompressTasks
        // Try all combinations and choose the one that compresses the best.
        List<CompressTask> compressTrials = new ArrayList<CompressTask>();
        
        for (int i = 0; i < deflateLevels.length; i++) {
            int deflateLevel = deflateLevels[i];
            for (int j = 0; j < deflateStrategies.length; j++) {
                int deflateStrategy = deflateStrategies[j];
                for (int k = 0; k < filterHeuristics.length; k++) {
                    int filterHeuristic = filterHeuristics[k];
                    compressTrials.add(new CompressTask(scanlines, bitsPerPixel, bestSize,
                            deflateLevel, deflateStrategy, filterHeuristic));
                }
            }
        }

        int numTrials = compressTrials.size();
        int bytes = scanlines.length * scanlines[0].length;
        // Try to keep initially compressed data if it doesn't take up too much memory.
        // Designed so a 1024x1024x4 image fits in 16 trials.
        // This means the code doesn't have to re-getCompressedData after it finds the best settings.
        boolean keepCompressedCopies = (64 << 20) >= bytes * numTrials;
        for (CompressTask task : compressTrials) {
            task.setKeepCompressedCopy(keepCompressedCopies);
        }

        CompressTask bestSettings = null;
        if (numTrials < 1) {
            return null;
        }
        else if (numTrials == 1) {
            // Special case: one task
            bestSettings = compressTrials.get(0);
        }
        else {
            try {
                es.invokeAll(compressTrials);
                for (CompressTask task : compressTrials) {
                    if (task.getSize() == bestSize.get()) {
                        bestSettings = task;
                        break;
                    }
                }
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        // Return results
        return new Results(bestSettings.getCompressedData(), bestSettings.getDeflateLevel(),
                bestSettings.getDeflateStrategy(), bestSettings.getFilterHeuristic());
    }
}

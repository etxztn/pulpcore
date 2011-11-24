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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;
import javax.imageio.ImageIO;

/**
    Creates an optimized PNG file.
    All PNG files created by this encoder are legal according to the PNG spec.
    <p>
    This encoder attempts many compression strategies. The resulting compression ratio is higher 
    than the ImageIO PNG encoder and near the ratio of the OptiPNG encoder.
    <p>
    This encoder can only encode a subset of the PNG spec.
    Only are the following color formats are supported: 
    <ul>
    <li>8-bit grayscale</li>
    <li>8-bit grayscale with full alpha</li>
    <li>8-bit RGB</li>
    <li>8-bit RGB with full alpha</li>
    <li>8-bit palette</li>
    <li>8-bit palette with transparency</li>
    <li>4-bit palette</li>
    <li>4-bit palette with transparency</li>
    </ul>
    These formats are not used:
    <ul>
        <li>1, 2, and 16-bit color formats</li>
        <li>Single-color transparency</li>
    </ul>
*/
public class PNGEncoder {

    public static final int OPTIMIZATION_OFF = 0;
    public static final int OPTIMIZATION_LEVEL_DEFAULT = 2;
    public static final int OPTIMIZATION_LEVEL_MAX = 5;

    /*
        Optimization |                ZLIB        ZLIB compression
           Level     |  Filters    Strategies          levels          Trials
        -------------+------------------------------------------------------------
         0           |      0          0                 9               1
         1           |     0,6         0                 9               2
         2 (default) |    0,5,7       0,1                9               6
         3           |    0,5-8       0,1               8,9             20
         4           |   0-2,5-8      0,1               6-9             56
         5           |     0-8        0-2               1-9            243
    */

    /*
     Java provides ints for Z_DEFAULT_STRATEGY (0), Z_FILTERED (1), Z_HUFFMAN_ONLY (2), but
     does not provide ints for Z_RLE (3) or Z_FIXED (4). OptiPNG uses stategies 0-3, but this
     compressor can only use 0-2. Also, Java doesn't provided a way to set the memory level to 9
     (the default is 8).
     */
    private static final int[][] DEFLATE_STRATEGIES = {
        /* 0 */ { Deflater.DEFAULT_STRATEGY },
        /* 1 */ { Deflater.DEFAULT_STRATEGY },
        /* 2 */ { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED },
        /* 3 */ { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED },
        /* 4 */ { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED },
        /* 5 */ { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY },
    };

    private static final int[][] DEFLATE_LEVELS = {
        /* 0 */ { 9 },
        /* 1 */ { 9 },
        /* 2 */ { 9 },
        /* 3 */ { 9, 8 },
        /* 4 */ { 9, 8, 7, 6 },
        /* 5 */ { 9, 8, 7, 6, 5, 4, 3, 2, 1 },
    };

    private static final int[][] FILTER_HEURISTICS = {
        /* 0 */ { 0 },
        /* 1 */ { 0, 6 },
        /* 2 */ { 0, 5, 7 },
        /* 3 */ { 0, 5, 6, 7, 8 },
        /* 4 */ { 0, 1, 2, 5, 6, 7, 8 },
        /* 5 */ { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
    };


    private static final long PNG_SIGNATURE = 0x89504e470d0a1a0aL;
    
    static final byte COLOR_TYPE_GRAYSCALE = 0;
    static final byte COLOR_TYPE_RGB = 2;
    static final byte COLOR_TYPE_PALETTE = 3;
    static final byte COLOR_TYPE_GRAYSCALE_WITH_ALPHA = 4;
    static final byte COLOR_TYPE_RGB_WITH_ALPHA = 6;

    private final TrialScanlineDeflater c;
    private final boolean optimizeForPremultipliedDisplay;

    /**
     Creates a PNGEncoder with the default optimization level.
     */
    public PNGEncoder() {
        this(OPTIMIZATION_LEVEL_DEFAULT, false);
    }
    
    /**
     Creates a PNGEncoder with the specified optimization level.
     */
    public PNGEncoder(int optimizationLevel) {
        this(optimizationLevel, false);
    }

    /**
     Creates a PNGEncoder with the default optimization level and with the option to enable
     optimization for premultiplied display. This options creates unmultiplied PNGS, but
     may compress better.
     */
    public PNGEncoder(boolean optimizeForPremultipliedDisplay) {
        this(OPTIMIZATION_LEVEL_DEFAULT, optimizeForPremultipliedDisplay);
    }

    /**
     Creates a PNGEncoder with the specified optimization level and with the option to enable
     optimization for premultiplied display. This options creates unmultiplied PNGS, but
     may compress better.
     */
    public PNGEncoder(int optimizationLevel, boolean optimizeForPremultipliedDisplay) {
        this(DEFLATE_LEVELS[optimizationLevel], DEFLATE_STRATEGIES[optimizationLevel],
                FILTER_HEURISTICS[optimizationLevel], optimizeForPremultipliedDisplay);
    }

    /**
     Creates a PNGEncoder with the specified deflate levels (1-9), deflate strategies (0-2),
     filter heuristics (0-8), and an option to enable optimization for premultiplied display.
     This options creates unmultiplied PNGS, but may compress better.
     */
    public PNGEncoder(int[] deflateLevels, int[] deflateStrategies, int[] filterHeuristics,
            boolean optimizeForPremultipliedDisplay) {
        this.c = new TrialScanlineDeflater(deflateLevels, deflateStrategies, filterHeuristics);
        this.optimizeForPremultipliedDisplay = optimizeForPremultipliedDisplay;
    }
    
    public String encode(File srcFile, File destFile) throws IOException {
        return encode(srcFile, destFile, null);
    }

    public String encode(File srcFile, File destFile, ExecutorService es) throws IOException {
        return encode(ImageIO.read(srcFile), destFile, es);
    }

    public String encode(BufferedImage image, File destFile) throws IOException {
        return encode(image, destFile, null);
    }

    public String encode(BufferedImage image, File destFile, ExecutorService es) throws IOException {
        FileOutputStream fos = new FileOutputStream(destFile);
        BufferedOutputStream out = new BufferedOutputStream(fos);
        String imageDescription = encode(image, out, es);
        fos.getFD().sync();
        out.close();
        return imageDescription;
    }
    
    /**
     Writes an image to an output stream, and returns a String describing the image.
     This method writes with the default optimization level and without optimizing for
     pre-multiplied display.
     */
    public String encode(BufferedImage image, OutputStream out) throws IOException {
        return encode(image, out, null);
    }

    /**
     Writes an image to an output stream, and returns a String describing the image.
     <p>
     This method is thread-safe. However, due to weirdness with ZLIB, if several PNGs are
     created in parallel, their outputted size may be slightly larger than if the same set of PNGs
     were created sequentially. The size difference will be small - maybe only a few bytes per
     image.
     */
    public String encode(BufferedImage image, OutputStream os, ExecutorService es) throws IOException {

        boolean newService = (es == null);

        try {
            if (newService) {
                int numProcessors = Runtime.getRuntime().availableProcessors();
                es = Executors.newFixedThreadPool(numProcessors);
            }

            // Get pixel data
            int width = image.getWidth();
            int height = image.getHeight();
            int[] dataARGB = ImageUtil.getData(image);
            if (optimizeForPremultipliedDisplay) {
                for (int i = 0; i < dataARGB.length; i++) {
                    dataARGB[i] = ImageUtil.flattenColor(dataARGB[i]);
                }
            }

            // Analyze pixel data
            ImageInfo imageInfo = ImageInfo.analyze(dataARGB);

            // Create the raw scanlines
            byte[][] scanlines = createScanlines(width, height, dataARGB, imageInfo);
            dataARGB = null;

            // Compress the scanlines
            TrialScanlineDeflater.Results results = c.compress(scanlines, imageInfo.getBitsPerPixel(),  es);
            byte[] compressedData = results.getCompressedData();
            scanlines = null;

            // Prepare to encode
            CRCOutputStream out = new CRCOutputStream(os);
            ChunkWriter chunkWriter = new ChunkWriter(out);

            // Write PNG signature
            out.writeLong(PNG_SIGNATURE);

            // Write header (must appear first)
            byte[] headerData = new byte[13];
            headerData[0] = (byte)(width >>> 24);
            headerData[1] = (byte)((width >> 16) & 0xff);
            headerData[2] = (byte)((width >> 8) & 0xff);
            headerData[3] = (byte)(width & 0xff);
            headerData[4] = (byte)(height >>> 24);
            headerData[5] = (byte)((height >> 16) & 0xff);
            headerData[6] = (byte)((height >> 8) & 0xff);
            headerData[7] = (byte)(height & 0xff);
            headerData[8] = (byte)imageInfo.getBitDepth();
            headerData[9] = imageInfo.getColorType();
            headerData[10] = (byte)0; // Compression method (deflate)
            headerData[11] = (byte)0; // Filter method
            headerData[12] = (byte)0; // Interlace method (none)
            chunkWriter.writeChunk(ChunkWriter.CHUNK_IHDR, headerData);

            // Write gAMA chunk (must appear before PLTE)
            // For now, just use Gamma 2.2 which is both Windows' and Snow Leopard's gamma value.
            // (A gamma of 1/2.2 would be stored as 45455)
//            int gamma = 45455;
//            byte[] gammaData = new byte[4];
//            gammaData[0] = (byte)(gamma >>> 24);
//            gammaData[1] = (byte)((gamma >> 16) & 0xff);
//            gammaData[2] = (byte)((gamma >> 8) & 0xff);
//            gammaData[3] = (byte)(gamma & 0xff);
//            chunkWriter.writeChunk(ChunkWriter.CHUNK_gAMA, gammaData);

            // Write PLTE chunk (must appear before IDAT)
            if (imageInfo.getColorType() == COLOR_TYPE_PALETTE) {
                List<Integer> palette = imageInfo.getPalette();
                byte[] paletteData = new byte[3*palette.size()];
                int index = 0;
                for (int color : palette) {
                    paletteData[index++] = (byte)((color >> 16) & 0xff);
                    paletteData[index++] = (byte)((color >> 8) & 0xff);
                    paletteData[index++] = (byte)(color & 0xff);
                }
                chunkWriter.writeChunk(ChunkWriter.CHUNK_PLTE, paletteData);
            }

            // Write tRNS chunk (must appear after PLTE and before IDAT)
            if (imageInfo.getColorType() == COLOR_TYPE_PALETTE && !imageInfo.isOpaque()) {
                List<Integer> palette = imageInfo.getPalette();

                // Count non-opaque entries (all opaque entries should appear last)
                int numEntries = 0;
                boolean opaqueFound = false;
                for (int color : palette) {
                    int alpha = color >>> 24;
                    if (alpha < 0xff) {
                        if (opaqueFound) {
                            // Not sorted correctly - just include all palette entries
                            numEntries = palette.size();
                            break;
                        }
                        else {
                            numEntries++;
                        }
                    }
                    else {
                        opaqueFound = true;
                    }
                }

                byte[] alphaData = new byte[numEntries];
                for (int i = 0; i < numEntries; i++) {
                    alphaData[i] = (byte)(palette.get(i).intValue() >>> 24);
                }
                chunkWriter.writeChunk(ChunkWriter.CHUNK_tRNS, alphaData);
            }

            // Write IDAT chunk
            chunkWriter.writeChunk(ChunkWriter.CHUNK_IDAT, compressedData);

            // Write end chunk (must appear last)
            chunkWriter.writeChunk(ChunkWriter.CHUNK_IEND, new byte[0]);

            // Clean up
            out.flush();

            return imageInfo + "\n  " + results;
        }
        finally {
            if (newService && es != null) {
                es.shutdown();
            }
        }
    }
    
    private byte[][] createScanlines(int width, int height, int[] dataARGB, 
            ImageInfo imageAnalyzer) {

        HashMap<Integer, Integer> paletteMap = new HashMap<Integer, Integer>();
        byte colorType = imageAnalyzer.getColorType();
        int bitDepth = imageAnalyzer.getBitDepth();
        int bitsPerPixel = imageAnalyzer.getBitsPerPixel();
        
        if (colorType == COLOR_TYPE_PALETTE) {
            List<Integer> palette = imageAnalyzer.getPalette();
            for (int i = 0; i < palette.size(); i++) {
                paletteMap.put(palette.get(i), new Integer(i));
            }
        }
        
        int bytesPerScanline = (width * bitsPerPixel + 7) / 8;
        byte[][] scanlines = new byte[height][bytesPerScanline];
        int index = 0;
        for (int i = 0; i < height; i++) {

            int scanlineIndex = 0;
            byte[] currScanline = scanlines[i];
            
            // Create the scanline
            switch (colorType) {
                case COLOR_TYPE_PALETTE:
                    if (bitDepth == 4) {
                        // Write 4-bit color indeces
                        for (int j = 0; j < width; j+=2) {
                            int index1 = paletteMap.get(dataARGB[index++]);
                            int index2 = 0;
                            if (j + 1 < width) {
                                index2 = paletteMap.get(dataARGB[index++]);
                            }
                            currScanline[scanlineIndex++] = 
                                (byte)((((index1 & 0x0f) << 4) | (index2 & 0x0f)) & 0xff);
                        }
                    }
                    else if (bitDepth == 8) {
                        // Write 8-bit color indeces
                        for (int j = 0; j < width; j++) {
                            int pixel = dataARGB[index++];
                            currScanline[scanlineIndex++] = (byte)(paletteMap.get(pixel) & 0xff);
                        }
                    }
                    else {
                        throw new IllegalArgumentException(
                                "Bit depth not supported with COLOR_TYPE_PALETTE: " + bitDepth);
                    }
                    break;
                    
                case COLOR_TYPE_GRAYSCALE:
                    if (bitDepth != 8) {
                        throw new IllegalArgumentException(
                                "Bit depth not supported with COLOR_TYPE_GRAYSCALE: " + bitDepth);
                    }
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)(pixel & 0xff); // v
                    }
                    break;
                
                case COLOR_TYPE_GRAYSCALE_WITH_ALPHA:
                    if (bitDepth != 8) {
                        throw new IllegalArgumentException(
                                "Bit depth not supported with COLOR_TYPE_GRAYSCALE_WITH_ALPHA: " + bitDepth);
                    }
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)(pixel & 0xff); // v
                        currScanline[scanlineIndex++] = (byte)(pixel >>> 24); // a
                    }
                    break;
                
                case COLOR_TYPE_RGB:
                    if (bitDepth != 8) {
                        throw new IllegalArgumentException(
                                "Bit depth not supported with COLOR_TYPE_RGB: " + bitDepth);
                    }
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)((pixel >> 16) & 0xff); // r
                        currScanline[scanlineIndex++] = (byte)((pixel >> 8) & 0xff);  // g
                        currScanline[scanlineIndex++] = (byte)((pixel >> 0) & 0xff);  // b
                    }
                    break;
                
                case COLOR_TYPE_RGB_WITH_ALPHA:
                    if (bitDepth != 8) {
                        throw new IllegalArgumentException(
                                "Bit depth not supported with COLOR_TYPE_RGB_WITH_ALPHA: " + bitDepth);
                    }
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)((pixel >> 16) & 0xff); // r
                        currScanline[scanlineIndex++] = (byte)((pixel >> 8) & 0xff);  // g
                        currScanline[scanlineIndex++] = (byte)((pixel >> 0) & 0xff);  // b
                        currScanline[scanlineIndex++] = (byte)(pixel >>> 24);         // a
                    }
                    break;
            }
        }
        return scanlines;
    }
}

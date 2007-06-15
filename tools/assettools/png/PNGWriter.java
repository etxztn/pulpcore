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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;


/**
    Creates a PNG file that can be read by the PulpCore PNG decoder. 
    All PNG files created by this encoder are legal according to the PNG spec.
    <p>
    This encoder attempts many compression strategies. The resulting compression ratio is higher 
    than the ImageIO PNG encoder and near the ratio of the OptiPNG encoder.
    <p>
    The PulpCore PNG decoder can only read a subset of the PNG spec.
    Only are the following color formats are supported: 
    <ul>
    <li>8-bit grayscale (ignores single-color transparency)</li>
    <li>8-bit grayscale with full alpha</li>
    <li>8-bit RGB (ignores single-color transparency)</li>
    <li>8-bit RGB with full alpha</li>
    <li>8-bit palette</li>
    <li>8-bit palette with transparency</li>
    <li>4-bit palette</li>
    <li>4-bit palette with transparency</li>
    </ul>
    Additionally, the PulpCore PNG decoder ignores color space information and suggested palettes, 
    and it cannot read interlaced PNGs or multiple IDAT chunks.
*/

/*
    To modify this PNG encoder to work with all bit depths or single-transparency,
    see ImageAnalyzer.analyze() and PNGWriter.createScanlines(). 
    
    You won't have to edit ChunkWriter, Compressor, CRCOutputStream, or Filter.
*/
public class PNGWriter {
    
    public static final int DEFAULT_OPTIMIZATION_LEVEL = Compressor.OPTIMIZATION_DEFAULT;
    public static final int MAX_OPTIMIZATION_LEVEL = Compressor.OPTIMIZATION_MAX;
    
    private static final long PNG_SIGNATURE = 0x89504e470d0a1a0aL;
    
    public static final byte COLOR_TYPE_GRAYSCALE = 0;
    public static final byte COLOR_TYPE_RGB = 2;
    public static final byte COLOR_TYPE_PALETTE = 3;
    public static final byte COLOR_TYPE_GRAYSCALE_WITH_ALPHA = 4;
    public static final byte COLOR_TYPE_RGB_WITH_ALPHA = 6;
    
    private int optimizationLevel = Compressor.OPTIMIZATION_DEFAULT;
    
    public void setOptimizationLevel(int level) {
        optimizationLevel = level;
    }
    
    
    public String write(BufferedImage image, OutputStream out) throws IOException {
        return write(image, 0, 0, null, null, out);
    }
    
    
    public String write(BufferedImage image, int hotspotX, int hotspotY,
        byte[] animData, byte[] fontData, OutputStream out) throws IOException 
    {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = new int[w * h];
        image.getRGB(0, 0, w, h, data, 0, w);
        return write(w, h, data, hotspotX, hotspotY, animData, fontData, out);
    }

    
    public String write(int width, int height, int[] dataARGB, int hotspotX, int hotspotY,
        byte[] animData, byte[] fontData, OutputStream out) throws IOException 
    {
        DataOutputStream os;
        if (out instanceof DataOutputStream) {
            os = (DataOutputStream)out;
        }
        else {
            os = new DataOutputStream(out);
        }
        
        ChunkWriter chunkWriter = new ChunkWriter(os);
        ImageAnalyzer imageAnalyzer = new ImageAnalyzer();
        imageAnalyzer.analyze(dataARGB);
    
        // Write PNG signature
        os.writeLong(PNG_SIGNATURE);
        
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
        headerData[8] = (byte)imageAnalyzer.getBitDepth();
        headerData[9] = imageAnalyzer.getColorType();
        headerData[10] = (byte)0; // Compression method (deflate)
        headerData[11] = (byte)0; // Filter method
        headerData[12] = (byte)0; // Interlace method (none)
        chunkWriter.writeChunk(ChunkWriter.CHUNK_IHDR, headerData);
        
        // Write hotspot chunk (must appear after IHDR)
        if (hotspotX != 0 || hotspotY != 0) {
            byte[] hotspotData = new byte[8];
            hotspotData[0] = (byte)(hotspotX >>> 24);
            hotspotData[1] = (byte)((hotspotX >> 16) & 0xff);
            hotspotData[2] = (byte)((hotspotX >> 8) & 0xff);
            hotspotData[3] = (byte)(hotspotX & 0xff);
            hotspotData[4] = (byte)(hotspotY >>> 24);
            hotspotData[5] = (byte)((hotspotY >> 16) & 0xff);
            hotspotData[6] = (byte)((hotspotY >> 8) & 0xff);
            hotspotData[7] = (byte)(hotspotY & 0xff);
            chunkWriter.writeChunk(ChunkWriter.CHUNK_hoTS, hotspotData);
        }
        
        // Write PLTE chunk (must appear before IDAT)
        if (imageAnalyzer.getColorType() == COLOR_TYPE_PALETTE) {
            List<Integer> palette = imageAnalyzer.getPalette();
            byte[] paletteData = new byte[3*palette.size()];
            int index = 0;
            for (int color : palette) {
                paletteData[index++] = (byte)((color >> 16) & 0xff);
                paletteData[index++] = (byte)((color >> 8) & 0xff);
                paletteData[index++] = (byte)(color & 0xff);
            }
            chunkWriter.writeChunk(ChunkWriter.CHUNK_PLTE, paletteData);
        }
        
        // Write tRNS chunk (must appear before IDAT and after PLTE)
        if (imageAnalyzer.getColorType() == COLOR_TYPE_PALETTE && !imageAnalyzer.isOpaque()) {
            List<Integer> palette = imageAnalyzer.getPalette();
            
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
        writeIDATChunk(width, height, dataARGB, imageAnalyzer, chunkWriter);
        
        // Write animation chunk (must appear after the IDAT chunk)
        if (animData != null) {
            chunkWriter.writeChunk(ChunkWriter.CHUNK_anIM, animData);
        }
        
        // Write font chunk (must appear after the animation chunk)
        if (fontData != null) {
            chunkWriter.writeChunk(ChunkWriter.CHUNK_foNT, fontData);
        }
        
        // Write end chunk (must appear last)
        chunkWriter.writeChunk(ChunkWriter.CHUNK_IEND, new byte[0]);
        
        return imageAnalyzer.toString();
    }
    
    
    private void writeIDATChunk(int width, int height, int[] dataARGB, 
        ImageAnalyzer imageAnalyzer, ChunkWriter chunkWriter) throws IOException 
    {
        // Create the raw scanlines
        int bytesPerPixel = (imageAnalyzer.getBitsPerPixel() + 7) / 8;
        byte[][] scanlines = createScanlines(width, height, dataARGB, imageAnalyzer);
        
        // Compress the scanlines
        Compressor compressor = new Compressor();
        byte[] compressedData = compressor.compress(scanlines, imageAnalyzer.getBitDepth(),
            bytesPerPixel, optimizationLevel);
            
        // Write it out
        chunkWriter.writeChunk(ChunkWriter.CHUNK_IDAT, compressedData);
    }
    
    
    /**
        Creates the raw, uncompressed image data.
    */
    private byte[][] createScanlines(int width, int height, int[] dataARGB, 
        ImageAnalyzer imageAnalyzer) 
    {
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
                            int index1 = paletteMap.get(new Integer(dataARGB[index++])).intValue();
                            int index2 = 0;
                            if (j + 1 < width) {
                                index2 = paletteMap.get(new Integer(dataARGB[index++])).intValue();
                            }
                            currScanline[scanlineIndex++] = 
                                (byte)((((index1 & 0x0f) << 4) | (index2 & 0x0f)) & 0xff);
                        }
                    }
                    else {
                        // Write 8-bit color indeces
                        for (int j = 0; j < width; j++) {
                            int pixel = dataARGB[index++];
                            Integer paletteIndex = paletteMap.get(new Integer(pixel));
                            currScanline[scanlineIndex++] = 
                                (byte)(paletteIndex.intValue() & 0xff);
                        }
                    }
                    break;
                    
                case COLOR_TYPE_GRAYSCALE:
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)(pixel & 0xff);
                    }
                    break;
                
                case COLOR_TYPE_GRAYSCALE_WITH_ALPHA:
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)(pixel & 0xff);
                        currScanline[scanlineIndex++] = (byte)(pixel >>> 24);
                    }
                    break;
                
                case COLOR_TYPE_RGB:
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)((pixel >> 16) & 0xff);
                        currScanline[scanlineIndex++] = (byte)((pixel >> 8) & 0xff);
                        currScanline[scanlineIndex++] = (byte)((pixel >> 0) & 0xff);
                    }
                    break;
                
                case COLOR_TYPE_RGB_WITH_ALPHA:
                    for (int j = 0; j < width; j++) {
                        int pixel = dataARGB[index++];
                        currScanline[scanlineIndex++] = (byte)((pixel >> 16) & 0xff);
                        currScanline[scanlineIndex++] = (byte)((pixel >> 8) & 0xff);
                        currScanline[scanlineIndex++] = (byte)((pixel >> 0) & 0xff);
                        currScanline[scanlineIndex++] = (byte)(pixel >>> 24); // alpha
                    }
                    break;
            }
        }
        return scanlines;
    }
}

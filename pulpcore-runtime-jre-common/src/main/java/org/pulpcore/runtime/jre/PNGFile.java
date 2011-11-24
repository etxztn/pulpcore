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
package org.pulpcore.runtime.jre;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.pulpcore.graphics.Color;

/**
    Loads a PNG file. This PNG decoder is faster and use less memory, because it loads the
    image data directly to an IntBuffer instead of loading an AWT image first. It  optionally
    color converts, flips vertically, and expands to power-of-two images. Using this PNG decoder
    also avoids some AWT bugs on older JRE implementations (for example, 4-bit color images
    with an odd width would not load correctly, and there was a memory leak because inflater.end() 
    was not called)
    <p>
    However, this decoder only supports a subset of the PNG spec. These features are missing:
    <ul>
        <li>1-bit, 2-bit, and 16-bit formats</li>
        <li>Single-color transparency</li>
        <li>Interlaced PNGs</li>
        <li>Multiple IDAT chunks</li>
    </ul>
    The following color formats are supported:
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
    <p>
    Use the PulpCore PNG Optimizer to ensure images are compliant with this decoder.
    <p>
*/
public class PNGFile {
    
    private static final Color.Format PNG_PIXEL_FORMAT = Color.Format.RGBA;

    private static final String PNG_ERROR_MESSAGE = "Error reading PNG file";
    private static final String ZLIB_ERROR_MESSAGE = "Invalid ZLIB data";

    private static final long PNG_SIGNATURE = 0x89504e470d0a1a0aL;

    private static final int CHUNK_IHDR = 0x49484452; // "IHDR"
    private static final int CHUNK_PLTE = 0x504c5445; // "PLTE"
    private static final int CHUNK_TRNS = 0x74524e53; // "tRNS"
    private static final int CHUNK_IDAT = 0x49444154; // "IDAT"
    private static final int CHUNK_IEND = 0x49454e44; // "IEND"

    private static final byte COLOR_TYPE_GRAYSCALE = 0;
    private static final byte COLOR_TYPE_RGB = 2;
    private static final byte COLOR_TYPE_PALETTE = 3;
    private static final byte COLOR_TYPE_GRAYSCALE_WITH_ALPHA = 4;
    private static final byte COLOR_TYPE_RGB_WITH_ALPHA = 6;
    private static final int[] SAMPLES_PER_PIXEL = { 1, 0, 3, 1, 2, 0, 4 };
    
    public static int nextHighestPowerOfTwo(int n) {
        int x = 1;
        while (x < n) {
            x <<= 1;
        }
        return x;
    }

    private int imageWidth;
    private int imageHeight;
    private int textureWidth;
    private int textureHeight;
    private boolean isOpaque;
    private IntBuffer data;
    
    private int bitDepth;
    private int colorType;
    private int[] palette;

    private PNGFile() { }

    /**
    Reads a PNG file.
    @param is the InputStream
    @param pixelFormat The destination pixel format.
    @param flipY Whether the y axis should be flipped (for OpenGL).
    @param nativeBuffer Whether the IntBuffer representing pixel data should be a native buffer.
    @param forcePowerOfTwoTextures Whether to force power-of-two textures. If true, 
        #getTextureWidth() and #getTextureHeight() may be larger than
        #getImageWidth() and #getImageHeight().
    @return a PNGFile object representing the PNG data.
    @throws IOException If there was an underlying IOError, or the PNG is not valid.
    */
    public static PNGFile load(InputStream is, Color.Format pixelFormat, boolean flipY, 
            boolean nativeBuffer, boolean forcePowerOfTwoTextures) throws IOException {
        PNGFile file = new PNGFile();
        file.doLoad(is, pixelFormat, flipY, nativeBuffer, forcePowerOfTwoTextures);
        return file;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public boolean isOpaque() {
        return isOpaque;
    }

    public IntBuffer getData() {
        return data;
    }

    private void doLoad(InputStream is, Color.Format pixelFormat, boolean flipY, 
            boolean nativeBuffer, boolean forcePowerOfTwoTextures) throws IOException {

        DataInputStream in;
        if (is instanceof DataInputStream) {
            in = (DataInputStream)is;
        }
        else {
            in = new DataInputStream(is);
        }
        
        boolean unknownChunkWarning = false;

        long sig = in.readLong();
        if (sig != PNG_SIGNATURE) {
            throw new IOException(PNG_ERROR_MESSAGE + ": Bad PNG signature: 0x" + Long.toString(sig, 16));
        }
        
        while (true) {
            int length = in.readInt();
            int chunkType = in.readInt();

            if (chunkType == CHUNK_IHDR) {
                readHeader(in, forcePowerOfTwoTextures, nativeBuffer);
            }
            else if (chunkType == CHUNK_PLTE) {
                readPalette(in, length);
            }
            else if (chunkType == CHUNK_TRNS) {
                readTransparency(in, length);
            }
            else if (chunkType == CHUNK_IDAT) {
                readData(in, length, pixelFormat, flipY);
            }
            else if (chunkType != CHUNK_IEND) {
                if (!unknownChunkWarning) {
                    System.out.println("Warning: Ignoring PNG chunk: " + chunkTypeToString(chunkType));
                    System.out.println("  Output image may be invalid.");
                    System.out.println("  Use PulpCore PNG Optimizer to avoid this warning.");
                    unknownChunkWarning = true;
                }
                for (int i = 0; i < length; i++) {
                    in.readByte();
                }
            }

            // Read CRC, assume it is correct
            in.readInt();

            if (chunkType == CHUNK_IEND) {
                break;
            }
        }
        data.rewind();
    }

    private void readHeader(DataInputStream in, 
            boolean forcePowerOfTwoTextures, boolean nativeBuffer) throws IOException {
        imageWidth = in.readInt();
        imageHeight = in.readInt();
        bitDepth = in.readByte();
        colorType = in.readByte();
        int compressionMethod = in.readByte();
        int filterMethod = in.readByte();
        int interlaceMethod = in.readByte();
        boolean supportedBitDepth =
            (bitDepth == 8 || (bitDepth == 4 && colorType == COLOR_TYPE_PALETTE));

        // More detailed error messages in DEBUG mode
        if (compressionMethod != 0) {
            throw new IOException("Invalid compression method: " + compressionMethod);
        }
        else if (filterMethod != 0) {
            throw new IOException("Invalid filter method: " + filterMethod);
        }
        else if (interlaceMethod != 0) {
            throw new IOException("Unsupported interlace method: " + interlaceMethod);
        }
        else if (!supportedBitDepth) {
            throw new IOException("Unsupported bit depth: " + bitDepth + " (color type: " +
                colorType + ")");
        }
        else if (colorType != COLOR_TYPE_GRAYSCALE &&
                colorType != COLOR_TYPE_RGB &&
                colorType != COLOR_TYPE_PALETTE &&
                colorType != COLOR_TYPE_GRAYSCALE_WITH_ALPHA &&
                colorType != COLOR_TYPE_RGB_WITH_ALPHA) {
            throw new IOException("Invalid color type: " + colorType);
        }

        isOpaque = true;
        if (colorType == COLOR_TYPE_GRAYSCALE_WITH_ALPHA || 
                colorType == COLOR_TYPE_RGB_WITH_ALPHA) {
            isOpaque = false;
        }
        
        if (forcePowerOfTwoTextures) {
            textureWidth = nextHighestPowerOfTwo(imageWidth);
            textureHeight = nextHighestPowerOfTwo(imageHeight);
        }
        else {
            textureWidth = imageWidth;
            textureHeight = imageHeight;
        }
        
        int dataSize = textureWidth * textureHeight;
        if (nativeBuffer) {
            data = ByteBuffer.allocateDirect(dataSize * 4).asIntBuffer();
        }
        else {
            data = IntBuffer.allocate(dataSize);
        }
    }

    private void readPalette(DataInputStream in, int length) throws IOException {

        palette = new int[length/3];

        if (palette.length * 3 != length) {
            throw new IOException(PNG_ERROR_MESSAGE + ": Invalid palette length: " + length);
        }

        for (int i = 0; i < palette.length; i++) {
            palette[i] = rgba(in.readByte(), in.readByte(), in.readByte(), (byte)0xff);
        }
    }

    private void readTransparency(DataInputStream in, int length) throws IOException {
        if (palette == null) {
            throw new IOException("Transparency unsupported without a palette. " +
                "Color type = " + colorType);
        }

        if (length > palette.length) {
            throw new IOException("Invalid transparency length: " + length);
        }

        for (int i = 0; i < length; i++) {
            int a = in.readByte() & 0xff;
            if (a < 0xff) {
                isOpaque = false;
            }
            
            palette[i] = (palette[i] & 0xffffff00) | a;
        }
    }
    
    private int rgba(byte r, byte g, byte b, byte a) {
        return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
    }

    private void readData(DataInputStream in, int length, 
            Color.Format pixelFormat, boolean flipY) throws IOException {

        Inflater inflater = new Inflater();
        int bitsPerPixel = bitDepth * SAMPLES_PER_PIXEL[colorType];
        int bytesPerPixel = (bitsPerPixel + 7) / 8;
        int bytesPerScanline = (imageWidth * bitsPerPixel + 7) / 8;
        byte[] prevScanline = new byte[bytesPerScanline];
        byte[] currScanline = new byte[bytesPerScanline];
        byte[] filterBuffer = new byte[1];
        byte[] inflateBuffer = new byte[4096];
        int[] dst = new int[imageWidth];
        Color.Format dstColorFormat;
        
        if (colorType == COLOR_TYPE_PALETTE) {
            Color.convert(PNG_PIXEL_FORMAT, pixelFormat, palette);
            dstColorFormat = pixelFormat;
        }
        else {
            dstColorFormat = PNG_PIXEL_FORMAT;
        }
        
        for (int i = 0; i < imageHeight; i++) {
            length = inflateFully(in, length, inflater, inflateBuffer, filterBuffer);
            length = inflateFully(in, length, inflater, inflateBuffer, currScanline);
            int filter = filterBuffer[0];

            // Apply filter
            if (filter > 0 && filter < 5) {
                decodeFilter(currScanline, prevScanline, filter, bytesPerPixel);
            }
            else if (filter != 0) {
                throw new IOException(PNG_ERROR_MESSAGE + ": Illegal filter type: " + filter);
            }

            // Read a scanline
            int srcOffset = 0;
            switch (colorType) {
                default: case COLOR_TYPE_GRAYSCALE:
                    for (int j = 0; j < imageWidth; j++) {
                        byte v = currScanline[j];
                        dst[j] = rgba(v, v, v, (byte)0xff);
                    }
                    break;

                case COLOR_TYPE_RGB:
                    for (int j = 0; j < imageWidth; j++) {
                        byte r = currScanline[srcOffset++];
                        byte g = currScanline[srcOffset++];
                        byte b = currScanline[srcOffset++];
                        dst[j] = rgba(r, g, b, (byte)0xff);
                    }
                    break;

                case COLOR_TYPE_PALETTE:
                    if (bitDepth == 8) {
                        for (int j = 0; j < imageWidth; j++) {
                            dst[j] = palette[currScanline[j] & 0xff];
                        }
                    }
                    else if (bitDepth == 4) {
                        int dstOffset = 0;
                        boolean isOdd = (imageWidth & 1) == 1;
                        int s = imageWidth & ~1;
                        for (int j = 0; j < s; j += 2) {
                            int b = currScanline[srcOffset++] & 0xff;
                            dst[dstOffset++] = palette[b >> 4];
                            dst[dstOffset++] = palette[b & 0x0f];
                        }
                        if (isOdd) {
                            int b = currScanline[srcOffset++] & 0xff;
                            dst[dstOffset++] = palette[b >> 4];
                        }
                    }
                    break;

                case COLOR_TYPE_GRAYSCALE_WITH_ALPHA:
                    for (int j = 0; j < imageWidth; j++) {
                        byte v = currScanline[srcOffset++];
                        byte a = currScanline[srcOffset++];
                        dst[j] = rgba(v, v, v, a);
                    }
                    break;

                case COLOR_TYPE_RGB_WITH_ALPHA:
                    for (int j = 0; j < imageWidth; j++) {
                        byte r = currScanline[srcOffset++];
                        byte g = currScanline[srcOffset++];
                        byte b = currScanline[srcOffset++];
                        byte a = currScanline[srcOffset++];
                        dst[j] = rgba(r, g, b, a);
                    }
                    break;
            }
            
            // Color convert, and add to dest buffer
            Color.convert(dstColorFormat, pixelFormat, dst);
            if (flipY) {
                data.position((imageHeight - i - 1) * textureWidth);
            }
            else {
                data.position(i * textureWidth);
            }
            data.put(dst);

            // Swap curr and prev scanlines
            byte[] temp = currScanline;
            currScanline = prevScanline;
            prevScanline = temp;
        }

        inflater.end();
    }

    private int inflateFully(DataInputStream in, int length, Inflater inflater, 
            byte[] inflateBuffer, byte[] result) throws IOException {
        int bytesRead = 0;

        while (bytesRead < result.length) {
            if (inflater.needsInput()) {
                if (length <= 0) {
                    throw new IOException(ZLIB_ERROR_MESSAGE);
                }
                else {
                    int bufLen = Math.min(length, inflateBuffer.length);
                    in.readFully(inflateBuffer, 0, bufLen);
                    inflater.setInput(inflateBuffer, 0, bufLen);
                    length -= bufLen;
                }
            }

            try {
                bytesRead += inflater.inflate(result, bytesRead, result.length - bytesRead);
            }
            catch (DataFormatException ex) {
                throw new IOException(ZLIB_ERROR_MESSAGE);
            }
        }
        return length;
    }

    private void decodeFilter(byte[] curr, byte[] prev, int filter, int bpp) {
        int length = curr.length;

        if (filter == 1) {
            // Input = Sub
            // Raw(x) = Sub(x) + Raw(x-bpp)
            // For all x < 0, assume Raw(x) = 0.
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] + curr[i - bpp]);
            }
        }
        else if (filter == 2) {
            // Input = Up
            // Raw(x) = Up(x) + Prior(x)
            for (int i = 0; i < length; i++) {
                curr[i] = (byte)(curr[i] + prev[i]);
            }
        }
        else if (filter == 3) {
            // Input = Average
            // Raw(x) = Average(x) + floor((Raw(x-bpp)+Prior(x))/2)
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte)(curr[i] + ((prev[i] & 0xff) >> 1));
            }
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] + (((curr[i - bpp] & 0xff) + (prev[i] & 0xff)) >> 1));
            }
        }
        else if (filter == 4) {
            // Input = Paeth
            // Raw(x) = Paeth(x) + PaethPredictor(Raw(x-bpp), Prior(x), Prior(x-bpp))
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte)(curr[i] + prev[i]);
            }
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] +
                    paethPredictor(curr[i - bpp] & 0xff, prev[i] & 0xff, prev[i - bpp] & 0xff));
            }
        }
    }

    // a = left, b = above, c = upper left
    private int paethPredictor(int a, int b, int c) {

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

    private String chunkTypeToString(int chunkType) {

        char ch1 = (char)(chunkType >>> 24);
        char ch2 = (char)((chunkType >> 16) & 0xff);
        char ch3 = (char)((chunkType >> 8) & 0xff);
        char ch4 = (char)(chunkType & 0xff);

        return "" + ch1 + ch2 + ch3 + ch4;
    }
}

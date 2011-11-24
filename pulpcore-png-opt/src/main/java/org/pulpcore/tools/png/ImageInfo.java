/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 Analyzes an image to get determine if it's grayscale or color, the color count, the bit depth,
 and whether or not it is opaque.
 */
public class ImageInfo {

    public static ImageInfo analyze(int[] dataARGB) {
        ImageInfo analyzer = new ImageInfo();
        analyzer.analyzeARGB(dataARGB);
        return analyzer;
    }
        
    private int bitDepth;
    private byte colorType;
    private boolean isOpaque;
    private Map<Integer, Color> palette;
    private List<Integer> sortedPalette;
    private String description;
    
    private ImageInfo() {
        
    }
    
    private void analyzeARGB(int[] dataARGB) {
        
        boolean isColor = false;
        BitSet alphas = new BitSet(256);
        palette = new HashMap<Integer, Color>(257);
        bitDepth = 8;
        isOpaque = true;
        
        for (int i = 0; i < dataARGB.length; i++) {
            int pixel = dataARGB[i];
            
            int a = pixel >>> 24;
            int r = (pixel >> 16) & 0xff;
            int g = (pixel >> 8) & 0xff;
            int b = pixel & 0xff;
            
            if (a != 0xff) {
                isOpaque = false;
            }
            
            if (r != g || r != b || g != b) {
                isColor = true;
            }
            
            if (palette.size() <= 256) {
                Color color = palette.get(pixel);
                if (color == null) {
                    color = new Color(pixel);
                    palette.put(pixel, color);
                }
                color.count++;
                color.positionSum += i;
            }
                
            alphas.set(a);
        }
        
        int paletteSize = palette.size();

        // Don't use the palette for small images
        // TODO: Just do brute-force tests instead?
        if (paletteSize <= 256) {
            int paletteBytes = palette.size() * 3 + ChunkWriter.CHUNK_OVERHEAD;
            if (!isOpaque) {
                // Assume every palette entry also has a trns entry
                paletteBytes += palette.size() + ChunkWriter.CHUNK_OVERHEAD;
            }

            int bpp = (paletteSize <= 16) ? 4 : 8;
            int altBPP;
            if (isColor) {
                altBPP = 8 * (isOpaque ? 3 : 4);
            }
            else {
                altBPP = 8 * (isOpaque ? 1 : 2);
            }
            int length = dataARGB.length * bpp / 8;
            int altLength = dataARGB.length * altBPP / 8;
            float compressionEst = 0.5f;

            // TODO: pick a better threshold?
            if (paletteBytes + length * compressionEst >= altLength * compressionEst) {
                // Don't use the palette
                paletteSize = Integer.MAX_VALUE;
            }
        }
        
        // Choose the smallest bits-per-pixel (bpp) needed to encode the image
        if (paletteSize <= 16) {
            // 4 bpp
            bitDepth = 4;
            colorType = PNGEncoder.COLOR_TYPE_PALETTE;
            createSortedPalette();
            description = "4-bit color, " + paletteSize + " colors, " +
                (isOpaque ? "opaque": (alphas.cardinality() + " alphas"));
        }
        else if (!isColor && isOpaque) {
            // 8 bpp
            colorType = PNGEncoder.COLOR_TYPE_GRAYSCALE;
            description = "Grayscale, opaque";
        }
        else if (paletteSize <= 256) {
            // 8 bpp
            colorType = PNGEncoder.COLOR_TYPE_PALETTE;
            createSortedPalette();
            description = "8-bit color, " + paletteSize + " colors, " +
                (isOpaque ? "opaque": (alphas.cardinality() + " alphas"));
        }
        else if (!isColor && !isOpaque) {
            // 16 bpp
            colorType = PNGEncoder.COLOR_TYPE_GRAYSCALE_WITH_ALPHA;
            description = "Grayscale, " + alphas.cardinality() + " alphas";
            // TODO: Optimize with transparent background color if:
            // Image uses bitmask alpha (alpha are 255 and 0 only) and there is a color available
            // that isn't in the image.
        }
        else if (isOpaque) {
            // 24 bpp
            colorType = PNGEncoder.COLOR_TYPE_RGB;
            description = "Full color, opaque";
        }
        else {
            // 32 bpp
            colorType = PNGEncoder.COLOR_TYPE_RGB_WITH_ALPHA;
            description = "Full color, " + alphas.cardinality() + " alphas";
            // TODO: Optimize with transparent background color if:
            // Image uses bitmask alpha (alpha are 255 and 0 only) and there is a color available
            // that isn't in the image.
        }

        description = "Color type: " + description;
    }
    
    public int getBitDepth() {
        return bitDepth;
    }
    
    public byte getColorType() {
        return colorType;
    }
    
    public boolean isOpaque() {
        return isOpaque;
    }

    private static class Color implements Comparable<Color> {
        final int color;
        int count;
        int positionSum;

        Color(int color) {
            this.color = color;
            this.count = 0;
            this.positionSum = 0;
        }

        private double getAveragePosition() {
            return (double)positionSum / count;
        }

        private int getComponentDist() {
            int a = color >>> 24;
            int r = (color >> 16) & 0xff;
            int g = (color >> 8) & 0xff;
            int b = color & 0xff;

            return a*a + r*r + g*g + b*b;
        }

        public int compareTo(Color that) {
            // Sort by unsigned integer. This put all translucent colors first, which allows the
            // tRNS chunk to be shorter.
            //
            // I tried other methods, but got poor results:
            // - Sort by popularity
            // - Sort by average position in the file (try to keep colors near each other in the
            //   image also near each other in the palette).
            // - Sort by component distance to rgb(0, 0, 0).

            long thisLong = this.color & 0xffffffffl;
            long thatLong = that.color & 0xffffffffl;

            if (thisLong < thatLong) {
                return -1;
            }
            else if (thisLong > thatLong) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }

    private void createSortedPalette() {
        List<Color> colors = new ArrayList<Color>(palette.values());
        Collections.sort(colors);
        sortedPalette = new ArrayList<Integer>();
        for (Color color : colors) {
            sortedPalette.add(color.color);
        }
    }
    
    /**
        Returns the palette which is sorted so that all opaque entries appear last.
    */
    public List<Integer> getPalette() {
        return sortedPalette;
    }
    
    public int getBitsPerPixel() {
        switch (colorType) {
            default: case PNGEncoder.COLOR_TYPE_GRAYSCALE: return bitDepth;
            case PNGEncoder.COLOR_TYPE_RGB: return bitDepth * 3;
            case PNGEncoder.COLOR_TYPE_PALETTE: return bitDepth;
            case PNGEncoder.COLOR_TYPE_GRAYSCALE_WITH_ALPHA: return bitDepth * 2;
            case PNGEncoder.COLOR_TYPE_RGB_WITH_ALPHA: return bitDepth * 4;
        }        
    }
    
    @Override
    public String toString() {
        return description;
    }
}

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
import java.util.Comparator;
import java.util.List;

public class ImageAnalyzer {
        
    private int bitDepth;
    private byte colorType;
    private boolean isOpaque;
    private List<Integer> palette;
    private String description;
    
    public void analyze(int[] dataARGB) {
        
        boolean isColor = false;
        BitSet alphas = new BitSet(256);
        palette = new ArrayList<Integer>(257);
        bitDepth = 8;
        isOpaque = true;
        
        for (int i = 0; i < dataARGB.length; i++) {
            int pixel = dataARGB[i];
            
            int a = pixel >>> 24;
            int r = (pixel >> 16) & 0xff;
            int g = (pixel >> 8) & 0xff;
            int b = pixel & 0xff;
            
            // The images tend to compress better with this statement.
            // Since PulpCore uses premultiplied alpha, the result is the same
            if (a == 0) {
                dataARGB[i] = 0;
                pixel = 0;
                r = 0;
                g = 0;
                b = 0;
            }
            
            if (a != 0xff) {
                isOpaque = false;
            }
            
            if (r != g || r != b || g != b) {
                isColor = true;
            }
            
            if (palette.size() <= 256) {
                Integer color = new Integer(pixel);
                if (!palette.contains(color)) {
                    palette.add(color);
                }
            }
                
            alphas.set(a);
        }
        
        // Choose the smallest bits-per-pixel (bpp) needed to encode the image
        if (palette.size() <= 16) {
            // 4 bpp
            bitDepth = 4;
            colorType = PNGWriter.COLOR_TYPE_PALETTE;
            Collections.sort(palette, new UnsignedIntComparator());
            description = "4-bit color, " + palette.size() + " colors, " + 
                (isOpaque ? "opaque": "w/alpha");
        }
        else if (!isColor && isOpaque) {
            // 8 bpp
            colorType = PNGWriter.COLOR_TYPE_GRAYSCALE;
            description = "Grayscale, opaque";
        }
        else if (palette.size() <= 256) {
            // 8 bpp
            colorType = PNGWriter.COLOR_TYPE_PALETTE;
            Collections.sort(palette, new UnsignedIntComparator());
            description = "8-bit color, " + palette.size() + " colors, " + 
                (isOpaque ? "opaque": "w/alpha");
        }
        else if (!isColor && !isOpaque) {
            // 16 bpp
            colorType = PNGWriter.COLOR_TYPE_GRAYSCALE_WITH_ALPHA;
            description = "Grayscale, " + alphas.cardinality() + " alphas";
        }
        else if (isOpaque) {
            // 24 bpp
            colorType = PNGWriter.COLOR_TYPE_RGB;
            description = "Full color, opaque";
        }
        else {
            // 32 bpp
            colorType = PNGWriter.COLOR_TYPE_RGB_WITH_ALPHA;
            description = "Full color, " + alphas.cardinality() + " alphas";
        }
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
    
    
    /**
        Returns the palette which is sorted so that all opaque entries appear last.
    */
    public List<Integer> getPalette() {
        return palette;
    }
    
    
    public int getBitsPerPixel() {
        switch (colorType) {
            default: case PNGWriter.COLOR_TYPE_GRAYSCALE: return bitDepth;
            case PNGWriter.COLOR_TYPE_RGB: return bitDepth * 3;
            case PNGWriter.COLOR_TYPE_PALETTE: return bitDepth;
            case PNGWriter.COLOR_TYPE_GRAYSCALE_WITH_ALPHA: return bitDepth * 2;
            case PNGWriter.COLOR_TYPE_RGB_WITH_ALPHA: return bitDepth * 4;
        }        
    }
    
    
    public String toString() {
        return description;
    }
    
    
    private static class UnsignedIntComparator implements Comparator<Integer> {
        
        public int compare(Integer o1, Integer o2) {
            long long1 = o1.longValue() & 0xffffffffl;
            long long2 = o2.longValue() & 0xffffffffl;
            
            if (long1 < long2) {
                return -1;
            }
            else if (long1 == long2) {
                return 0;
            }
            else {
                return 1;
            }
        }
    }
}

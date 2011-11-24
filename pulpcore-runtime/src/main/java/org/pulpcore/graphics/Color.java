/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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

package org.pulpcore.graphics;

import org.pulpcore.math.MathUtil;

/**
    The Color class provides convenience methods to create packed, 32-bit ARGB colors.
*/
public class Color {

    public enum Format {
        /** ARGB is the format used in the Color class. */    
        ARGB,
        /** Alpha, hue, saturation, brightness. */
        AHSB,
        /** RGBA format for HTML5 Canvas and the PNG internal format */
        RGBA, 
        /** ARGB_PREMULTIPLIED is for Direct3D9 and Java images. */
        // D3DFMT_A8R8G8B8
        ARGB_PREMULTIPLIED,
        /** RGBA_PREMULTIPLIED is for OpenGL and Direct3D10 images. */
        // DXGI_FORMAT_R8G8B8A8_SINT
        RGBA_PREMULTIPLIED, 
    }    
    
    // Prevent instantiation
    private Color() { }
    
    /** Fully transparent color. Same as {@code gray(0, 0)}. */
    public static final int TRANSPARENT = gray(0, 0);
    
    /** The color black. Same as {@code gray(0)}. */
    public static final int BLACK = gray(0);
    
    /** The color white. Same as {@code gray(255)}. */
    public static final int WHITE = gray(255);
    
    /** The color light gray. Same as {@code gray(192)}. */
    public static final int LIGHTGRAY = gray(192);
    
    /** The color gray. Same as {@code gray(128)}. */
    public static final int GRAY = gray(128);
    
    /** The color dark black. Same as {@code gray(64)}. */
    public static final int DARKGRAY = gray(64);
    
    /** The color red. Same as {@code rgb(255, 0 0)}. */
    public static final int RED = rgb(255, 0, 0); // almost hue(0)
    
    /** The color orange. Same as {@code rgb(255, 200, 0)}. */
    public static final int ORANGE = rgb(255, 200, 0); // almost hue(33)
    
    /** The color yellow. Same as {@code rgb(255, 255, 0)}. */
    public static final int YELLOW = rgb(255, 255, 0); // almost hue(43)
    
    /** The color green. Same as {@code rgb(0, 255, 0)}. */
    public static final int GREEN = rgb(0, 255, 0); // almost hue(85)
    
    /** The color cyan. Same as {@code rgb(0, 255, 255)}. */
    public static final int CYAN = rgb(0, 255, 255); // almost hue(128)
    
    /** The color blue. Same as {@code rgb(0, 0, 255)}. */
    public static final int BLUE = rgb(0, 0, 255); // almost hue(170)
    
    /** The color purple. Same as {@code rgb(162, 0, 255)}. */
    public static final int PURPLE = rgb(162, 0, 255); // almost hue(197)
    
    /** The color magenta. Same as {@code rgb(255, 0, 255)}. */
    public static final int MAGENTA = rgb(255, 0, 255); // almost hue(213)
    
    /**
        Creates an opaque gray color.
        @param gray the gray value from 0 (black) to 255 (white).
        @return a packed 32-bit ARGB color.
    */
    public static int gray(int gray) {
        return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    
    /**
        Creates a translucent gray color.
        @param gray the gray value from 0 (black) to 255 (white).
        @param alpha the opacity from 0 (transparent) to 255 (opaque).
        @return a packed 32-bit ARGB color.
    */
    public static int gray(int gray, int alpha) {
        return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
    }
    
    /**
        Creates an opaque color. This method ensures the alpha is set to 255 (opaque).
        @param rgb a packed 32-bit ARGB color.
        @return a packed 32-bit ARGB color with the alpha set to 255 (opaque).
    */
    public static int rgb(int rgb) {
        return 0xff000000 | rgb;
    }
    
    /**
        Creates a color. If {@code hasAlpha} is true, this method returns {@code rgb}
        without modification. Otherwise, this method sets the alpha to 255 (opaque).
        @param rgb a packed 32-bit ARGB color.
        @param hasAlpha flag indicating the {@code rgb} param has alpha or not.
        @return a packed 32-bit ARGB color with the alpha set.
    */
    public static int rgb(int rgb, boolean hasAlpha) {
        if (hasAlpha) {
            return rgb;
        }
        else {
            return 0xff000000 | rgb;
        }
    }
    
    /**
        Creates a color with alpha. The original color's alpha value, if any, is ignored.
        @param rgb a packed 32-bit ARGB color.
        @param alpha the alpha component, from 0 (transparent) to 255 (opaque).
        @return a packed 32-bit ARGB color.
    */
    public static int rgba(int rgb, int alpha) {
        return (alpha << 24) | (0xffffff & rgb);
    }
    
    /**
        Creates an opaque color.
        @param r the red component, from 0 to 255.
        @param g the green component, from 0 to 255.
        @param b the blue component, from 0 to 255.
        @return a packed 32-bit ARGB color with the alpha set to 255 (opaque).
    */
    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 0xff);
    }
    
    /**
        Creates a color with alpha.
        @param r the red component, from 0 to 255.
        @param g the green component, from 0 to 255.
        @param b the blue component, from 0 to 255.
        @param alpha the alpha component, from 0 (transparent) to 255 (opaque).
        @return a packed 32-bit ARGB color.
    */
    public static int rgba(int r, int g, int b, int alpha) {
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
        Creates a fully bright, fully saturated opaque color with the specified hue.
        @param h the hue, from 0 to 255.
        @return a packed 32-bit ARGB color with the alpha set to 255 (opaque).
    */
    public static int hue(int h) {
        return hsba(h, 0xff, 0xff, 0xff);
    }
    
    /**
        Creates a fully bright, fully saturated color with the specified hue.
        @param h the hue, from 0 to 255.
        @param alpha the alpha component, from 0 (transparent) to 255 (opaque).
        @return a packed 32-bit ARGB color.
    */
    public static int hue(int h, int alpha) {
        return hsba(h, 0xff, 0xff, alpha);
    }
    
    /**
        Creates an opaque color.
        @param h the hue, from 0 to 255.
        @param s the saturation, from 0 to 255.
        @param b the brightness, from 0 to 255.
        @return a packed 32-bit ARGB color with the alpha set to 255 (opaque).
    */
    public static int hsb(int h, int s, int b) {
        return hsba(h, s, b, 0xff);
    }
    
    /**
        Creates a color with alpha.
        @param h the hue, from 0 to 255.
        @param s the saturation, from 0 to 255.
        @param b the brightness, from 0 to 255.
        @param alpha the alpha component, from 0 (transparent) to 255 (opaque).
        @return a packed 32-bit ARGB color.
    */
    public static int hsba(int h, int s, int b, int alpha) {
        return HSBtoRGB((alpha << 24) | (h << 16) | (s << 8) | b);
    }
    
    /**
        Gets the alpha component of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the alpha component, from 0 to 255.
    */
    public static int getAlpha(int color) {
        return color >>> 24;
    }
    
    /**
        Gets the red component of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the red component, from 0 to 255.
    */
    public static int getRed(int color) {
        return (color >> 16) & 0xff;
    }
    
    /**
        Gets the green component of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the green component, from 0 to 255.
    */
    public static int getGreen(int color) {
        return (color >> 8) & 0xff;
    }
    
    /**
        Gets the blue component of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the blue component, from 0 to 255.
    */
    public static int getBlue(int color) {
        return color & 0xff;
    }
    
    /**
        Gets the hue of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the hue, from 0 to 255.
    */
    public static int getHue(int color) {
        int v = RGBtoHSB(color);
        return (v >> 16) & 0xff;
    }
    
    /**
        Gets the saturation of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the saturation, from 0 to 255.
    */
    public static int getSaturation(int color) {
        int v = RGBtoHSB(color);
        return (v >> 8) & 0xff;
    }
    
    /**
        Gets the brightness of a 32-bit ARGB color.
        @param color a 32-bit ARGB color.
        @return the brightness, from 0 to 255.
    */
    public static int getBrightness(int color) {
        int v = RGBtoHSB(color);
        return v & 0xff;
    }
    
    /**
        Checks if a 32-bit ARGB color is a gray color, from 0 (black) to 255 (white).
        @param color a 32-bit ARGB color.
        @return true if the color is gray.
    */
    public static boolean isGray(int color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        return (r == g && g == b);
    }

    /**
     Checks if a 32-bit ARGB color is opaque (has an alpha of 255).
     @param color a 32-bit ARGB color.
     @return true if the color is opaque
     */
    public static boolean isOpaque(int color) {
        return (color >>> 24) == 255;
    }

    /**
     Checks if a 32-bit ARGB color is fully transparent (has an alpha of 0).
     @param color a 32-bit ARGB color.
     @return true if the color is transparent
     */
    public static boolean isTransparent(int color) {
        return (color >>> 24) == 0;
    }
    
    /**
        Creates a brighter version of the specified color. The alpha is not modified.
        @param color a 32-bit ARGB color.
        @return a brighter version of the 32-bit ARGB color.
    */
    public static int brighter(int color) {
        int alpha = color & 0xff000000;
        if ((color & 0xffffff) == 0) {
            return alpha | 0x010101;
        }
        else {
            // If top bit is set for a component, the result if 0xff
            int topBit = (color >> 7) & 0x010101;
            int mask = (topBit << 8) - topBit;
            
            color = (color << 1) & 0xfefefe;
            return alpha | mask | color;
        }
    }
    
    /**
        Creates a darker version of the specified color. The alpha is not modified.
        @param color a 32-bit ARGB color.
        @return a darker version of the 32-bit ARGB color.
    */
    public static int darker(int color) {
        int alpha = color & 0xff000000;
        return alpha | ((color >> 1) & 0x7f7f7f);
    }
    
    /**
        Returns a string representation of the specified color.
        @param color a 32-bit ARGB color.
        @return a string representation of the color.
    */
    public static String toString(int color) {
        int a = color >>> 24;
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        
        if (a == 0xff) {
            return "rgb(" + r + ", " + g + ", " + b + ")";
        }
        else {
            return "rgba(" + r + ", " + g + ", " + b + ", " + a + ")";
        }
    }
    
    /**
     Blends two colors together. Both colors need to be the same format, but can be either
     RGB or HSB, with or without alpha.
     @param fromColor The color to blend from.
     @param toColor The color to blend to.
     @param p A value representing how much to blend, from 0 (100% fromColor) to 1.0 (100% toColor).
     @return The blended color.
     */
    public static int blend(int fromColor, int toColor, float p) {
        if (p == 0) {
            return fromColor;
        }
        else if (p == 1) {
            return toColor;
        }

        int a1 = fromColor >>> 24;
        int b1 = (fromColor >> 16) & 0xff;
        int c1 = (fromColor >> 8) & 0xff;
        int d1 = fromColor & 0xff;

        int a2 = toColor >>> 24;
        int b2 = (toColor >> 16) & 0xff;
        int c2 = (toColor >> 8) & 0xff;
        int d2 = toColor & 0xff;

        int a = (int)(a1 + (a2 - a1) * p);
        int b = (int)(b1 + (b2 - b1) * p);
        int c = (int)(c1 + (c2 - c1) * p);
        int d = (int)(d1 + (d2 - d1) * p);

        if (a < 0) {
            a = 0;
        }
        if (b < 0) {
            b = 0;
        }
        if (c < 0) {
            c = 0;
        }
        if (d < 0) {
            d = 0;
        }

        if (a > 255) {
            a = 255;
        }
        if (b > 255) {
            b = 255;
        }
        if (c > 255) {
            c = 255;
        }
        if (d > 255) {
            d = 255;
        }

        return (a << 24) | (b << 16) | (c << 8) | d;
    }
    
    //
    // Color conversion
    //
    
    /**
        Converts a a packed, 32-bit RGB (red, green, blue) color to HSB (hue, saturation, 
        brightness). The hue, saturation, and brightness are in the range
        0 - 255. The alpha value, if any, is not modified.
        
        <pre>
        int hsb = Colors.RGBtoHSB(rgb);
        int h = (hsb >> 16) & 0xff;
        int s = (hsb >> 8) & 0xff;
        int b = hsb & 0xff;
        </pre>
        @param argbColor a 32-bit ARGB color.
        @return a 32-bit AHSB color.
        @see #HSBtoRGB(int)
    */
    private static int RGBtoHSB(int argbColor) {
        
        int a = argbColor >>> 24;
        int r = (argbColor >> 16) & 0xff;
        int g = (argbColor >> 8) & 0xff;
        int b = argbColor & 0xff;
        
        int minRGB = Math.min(Math.min(r, g), b);
        int maxRGB = Math.max(Math.max(r, g), b);
        
        // Brightness
        int v = maxRGB;
        
        if (minRGB == maxRGB) {
            // Gray - no hue or saturation
            return (a << 24) | v;
        }
        
        int diff = maxRGB - minRGB;
        
        // Saturation
        int s;
        if (diff == maxRGB) {                   
            s = 255;
        }
        else {
            s = MathUtil.intDivRound(diff << 8, maxRGB);
        }
        
        // Hue
        int h;
        if (r == maxRGB) {
            h = (g - b);
        }
        else if (g == maxRGB) {
            h = (diff << 1) + (b - r);
        }
        else { // b == maxRGB
            h = (diff << 2) + (r - g);
        }
        h = MathUtil.intDivFloor(h << 8, diff * 6) & 0xff;
        
        return (a << 24) | (h << 16) | (s << 8) | v;
    }
    
    
    /**
        Converts a packed, 32-bit HSB (hue, saturation, brightness) color to RGB 
        (red, green, blue). The alpha value, if any, is not modified.
        <pre>
        int rgb = Colors.HSBtoRGB(hsb);
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        </pre>
        @param ahsbColor a 32-bit AHSB color.
        @return a 32-bit ARGB color.
        @see #RGBtoHSB(int)
    */
    private static int HSBtoRGB(int ahsbColor) {
        
        int a = ahsbColor >>> 24;
        int h = (ahsbColor >> 16) & 0xff;
        int s = (ahsbColor >> 8) & 0xff;
        int v = ahsbColor & 0xff;
        
        if (s == 0) {
            // Gray
            return (a << 24) | (v << 16) | (v << 8) | v;
        }
      
        int h6 = h * 6 + 3;
        int i = h6 >> 8; // 0 .. 5
        int f = h6 - (i << 8);

        int p = s * 255;
        int q = s * f;
        int t = p - q;
        
        int r = 0;
        int g = 0;
        int b = 0;
        
        if (i == 0) { 
            g = t;
            b = p;
        }
        else if (i == 1) {
            r = q;
            b = p;
        }
        else if (i == 2) {
            r = p;
            b = t; 
        }
        else if (i == 3) { 
            r = p;
            g = q;
        }
        else if (i == 4) {
            r = t; 
            g = p;
        }
        else { 
            g = p; 
            b = q;
        }
        
        r = ((v << 16) - (v * r)) >> 16;
        g = ((v << 16) - (v * g)) >> 16;
        b = ((v << 16) - (v * b)) >> 16;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }    
    
    public static int convert(Format from, Format to, int color) {
        // Identical
        if (from == to) {
            return color;
        }
        else {
            int[] colors = new int[1];
            colors[0] = color;
            convert(from, to, colors);
            return colors[0];
        }
    }
    
    public static void convert(Format from, Format to, int[] colors) {
        if (from == to) {
            return;
        }
        
        // For HSB sources, first convert to ARGB.
        if (from == Format.AHSB) {
            for (int i = 0; i < colors.length; i++) {
                colors[i] = HSBtoRGB(colors[i]);
            }
            from = Format.ARGB;
        }
        
        boolean convertRGBtoHSB = false;
        if (to == Format.AHSB) {
            convertRGBtoHSB = true;
            to = Format.ARGB;
        }
        
        // Identical
        if (from == to) {
            // Do nothing
        }
        
        // Simple Swap
        else if (from == Format.ARGB && to == Format.RGBA) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                colors[i] = (color << 8) | (color >>> 24);
            }
        }
        else if (from == Format.RGBA && to == Format.ARGB) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                colors[i] = (color >>> 8) | (color << 24);
            }
        }
        else if (from == Format.ARGB_PREMULTIPLIED && to == Format.RGBA_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                colors[i] = (color << 8) | (color >>> 24);
            }
        }
        else if (from == Format.RGBA_PREMULTIPLIED && to == Format.ARGB_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                colors[i] = (color >>> 8) | (color << 24);
            }
        }
        
        // Premultiply
        else if (from == Format.ARGB && to == Format.ARGB_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color >>> 24;
                if (a == 255) {
                    // Do nothing
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = (color >> 16) & 0xff;
                    int g = (color >> 8) & 0xff;
                    int b = color & 0xff;
                    r = (a * r + 127) / 255;
                    g = (a * g + 127) / 255;
                    b = (a * b + 127) / 255;
                    colors[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
        else if (from == Format.RGBA && to == Format.RGBA_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color & 0xff;
                if (a == 255) {
                    // Do nothing
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = color >>> 24;
                    int g = (color >> 16) & 0xff;
                    int b = (color >> 8) & 0xff;
                    r = (a * r + 127) / 255;
                    g = (a * g + 127) / 255;
                    b = (a * b + 127) / 255;
                    colors[i] = (r << 24) | (g << 16) | (b << 8) | a;
                }
            }
        }
        
        // Premultiply and swap
        else if (from == Format.ARGB && to == Format.RGBA_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color >>> 24;
                if (a == 255) {
                    colors[i] = (color << 8) | (color >>> 24);
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = (color >> 16) & 0xff;
                    int g = (color >> 8) & 0xff;
                    int b = color & 0xff;
                    r = (a * r + 127) / 255;
                    g = (a * g + 127) / 255;
                    b = (a * b + 127) / 255;
                    colors[i] = (r << 24) | (g << 16) | (b << 8) | a;
                }
            }
        }
        else if (from == Format.RGBA && to == Format.ARGB_PREMULTIPLIED) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color & 0xff;
                if (a == 255) {
                    colors[i] = (color >>> 8) | (color << 24);
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = color >>> 24;
                    int g = (color >> 16) & 0xff;
                    int b = (color >> 8) & 0xff;
                    r = (a * r + 127) / 255;
                    g = (a * g + 127) / 255;
                    b = (a * b + 127) / 255;
                    colors[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
        
        // Unpremultiply
        else if (from == Format.ARGB_PREMULTIPLIED && to == Format.ARGB) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color >>> 24;
                if (a == 255) {
                    // Do nothing
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = (color >> 16) & 0xff;
                    int g = (color >> 8) & 0xff;
                    int b = color & 0xff;
                    r = 255 * r / a;
                    g = 255 * g / a;
                    b = 255 * b / a;
                    colors[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
        else if (from == Format.RGBA_PREMULTIPLIED && to == Format.RGBA) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color & 0xff;
                if (a == 255) {
                    // Do nothing
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = color >>> 24;
                    int g = (color >> 16) & 0xff;
                    int b = (color >> 8) & 0xff;
                    r = 255 * r / a;
                    g = 255 * g / a;
                    b = 255 * b / a;
                    colors[i] = (r << 24) | (g << 16) | (b << 8) | a;
                }
            }
        }

        // Unpremultiply and swap
        else if (from == Format.ARGB_PREMULTIPLIED && to == Format.RGBA) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color >>> 24;
                if (a == 255) {
                    colors[i] = (color << 8) | (color >>> 24);
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = (color >> 16) & 0xff;
                    int g = (color >> 8) & 0xff;
                    int b = color & 0xff;
                    r = 255 * r / a;
                    g = 255 * g / a;
                    b = 255 * b / a;
                    colors[i] = (r << 24) | (g << 16) | (b << 8) | a;
                }
            }
        }
        else if (from == Format.RGBA_PREMULTIPLIED && to == Format.ARGB) {
            for (int i = 0; i < colors.length; i++) {
                int color = colors[i];
                int a = color & 0xff;
                if (a == 255) {
                    colors[i] = (color >>> 8) | (color << 24);
                }
                else if (a == 0) {
                    colors[i] = 0;
                }
                else {
                    int r = color >>> 24;
                    int g = (color >> 16) & 0xff;
                    int b = (color >> 8) & 0xff;
                    r = 255 * r / a;
                    g = 255 * g / a;
                    b = 255 * b / a;
                    colors[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
        
        else {
            // Shouldn't happen
            throw new IllegalArgumentException("Can't convert from " + from + " to " + to + ".");
        }
        
        if (convertRGBtoHSB) {
            for (int i = 0; i < colors.length; i++) {
                colors[i] = RGBtoHSB(colors[i]);
            }
        }
    }
}

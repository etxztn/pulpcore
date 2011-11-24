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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ImageUtil {

    private ImageUtil() { }

    public static int[] getData(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = null;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            if (data.length != w * h) {
                data = null;
            }
        }
        if (data == null) {
            data = image.getRGB(0, 0, w, h, null, 0, w);
        }
        return data;
    }

    public static Dimension getImageDimensions(File file) {

        try {
            ImageInputStream in = ImageIO.createImageInputStream(file);
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                }
                finally {
                    reader.dispose();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Dimension(0, 0);
    }

    /**
        Converts an ARGB color to a premultiplied ARGB color.
        @param argbColor the ARGB color to premultiply
        @return the premultiplied ARGB color
    */
    public static int premultiply(int argbColor) {
        int a = argbColor >>> 24;

        if (a == 0) {
            return 0;
        }
        else if (a == 255) {
            return argbColor;
        }
        else {
            int r = (argbColor >> 16) & 0xff;
            int g = (argbColor >> 8) & 0xff;
            int b = argbColor & 0xff;
            r = (a * r + 127) / 255;
            g = (a * g + 127) / 255;
            b = (a * b + 127) / 255;
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
        Converts a premultiplied ARGB color to an ARGB color.
        @param preARGBColor the premultiplied ARGB color to premultiply
        @return the converted ARGB color
    */
    public static int unpremultiply(int preARGBColor) {
        int a = preARGBColor >>> 24;

        if (a == 0) {
            return 0;
        }
        else if (a == 255) {
            return preARGBColor;
        }
        else {
            int r = (preARGBColor >> 16) & 0xff;
            int g = (preARGBColor >> 8) & 0xff;
            int b = preARGBColor & 0xff;

            r = 255 * r / a;
            g = 255 * g / a;
            b = 255 * b / a;
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
     Premultiplies, then un-premultiplies the color. After the initial call to flattenColor,
     calling flattenColor with the returned value will return the same value.
     (TODO: Test this theory)
     */
    public static int flattenColor(int argbColor) {
        int a = argbColor >>> 24;

        if (a == 0) {
            return 0;
        }
        else if (a == 255) {
            return argbColor;
        }
        else {
            int r = (argbColor >> 16) & 0xff;
            int g = (argbColor >> 8) & 0xff;
            int b = argbColor & 0xff;

            // Premultiply
            r = (a * r + 127) / 255;
            g = (a * g + 127) / 255;
            b = (a * b + 127) / 255;

            // Unpremultiply
            r = 255 * r / a;
            g = 255 * g / a;
            b = 255 * b / a;

            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
}

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
package org.pulpcore.tools.imagefont;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.FontFormatException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pulpcore.tools.packer.ImageUtils;

public class ImageFontBuilder {

    private ImageFontBuilder() {
    }
    
    public static ImageFont build(File inFile, double scale) throws IOException {

        long startTime = System.nanoTime();
        
        String name = inFile.getName();
        if (name.toLowerCase().endsWith(".font.json")) {
            name = name.substring(0, name.length() - 10);
        }
        else if (name.toLowerCase().endsWith(".font.js")) {
            name = name.substring(0, name.length() - 8);
        }
        else if (name.toLowerCase().endsWith(".json")) {
            name = name.substring(0, name.length() - 5);
        }
        else if (name.toLowerCase().endsWith(".js")) {
            name = name.substring(0, name.length() - 3);
        }

        FileReader reader = new FileReader(inFile);
        ImageFontDefinition fontDef = ImageFontDefinition.fromJson(reader);
        fontDef = fontDef.getScaledClone(scale);
        reader.close();

        ImageFont font = build(name, inFile.getParentFile(), fontDef);

        long durMillis = (System.nanoTime() - startTime) / 1000000;
        float dur = durMillis / 1000f;
        synchronized (ImageFontBuilder.class) {
            System.out.println("Created font from: " + inFile);
            System.out.println(dur + "s.");
        }

        return font;
    }

    private static ImageFont build(String name, File localFontDir,
            ImageFontDefinition imageFontDef) throws IOException {

        // Get chars
        String chars = imageFontDef.getSortedChars();

        // Get font style
        int fontStyle = Font.PLAIN;
        if (imageFontDef.isBold()) {
            fontStyle |= Font.BOLD;
        }
        if (imageFontDef.isItalic()) {
            fontStyle |= Font.ITALIC;
        }

        // Get Fill (solid or gradient)
        Paint fillPaint = imageFontDef.getColor();
        if (imageFontDef.getBottomColor() != null) {
            float yOffset = imageFontDef.getColorOffset();
            fillPaint = new GradientPaint(
                0, -imageFontDef.getSize() + yOffset,
                imageFontDef.getColor(),
                0, yOffset,
                imageFontDef.getBottomColor());
        }

        // Get stroke color, size
        Paint strokePaint = imageFontDef.getStrokeColor();
        BasicStroke stroke;
        if (imageFontDef.getStrokeSize() > 0) {
            stroke = new BasicStroke(imageFontDef.getStrokeSize());
        }
        else {
            stroke = null;
        }

        // Shadow
        Color shadowColor = imageFontDef.getShadowColor();
        Filter shadowFilter = null;
        if (shadowColor != null && shadowColor.getAlpha() > 0) {
            shadowFilter = new BlurFilter(imageFontDef.getShadowBlurPasses(), shadowColor.getRGB());
        }

        // Create the render context
        FontRenderContext context = new FontRenderContext(new AffineTransform(),
                imageFontDef.isAntiAlias(), imageFontDef.usesFractionalMetrics());

        // Create the AWT Font
        String[] a = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames();
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i].toLowerCase();
        }
        List<String> available = Arrays.asList(a);
        Font font = new Font("Dialog", Font.PLAIN, 1);
        String[] familyNames = imageFontDef.getFamily();
        for (String family : familyNames) {
            if (family.toLowerCase().endsWith(".ttf")) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, new File(localFontDir, family));
                }
                catch (FontFormatException ex) {
                    throw new IOException("Couldn't load font", ex);
                }
                break;
            }
            else if (available.contains(family.toLowerCase())) {
                font = new Font(family, Font.PLAIN, 1);
                break;
            }
        }
        font = font.deriveFont(fontStyle, imageFontDef.getSize());

        // Get the legal chars for this font
        StringBuffer legalCharBuffer = new StringBuffer(chars);
        for (int i = 0; i < legalCharBuffer.length(); i++) {
            char ch = legalCharBuffer.charAt(i);
            if (!font.canDisplay(ch)) {
                legalCharBuffer.deleteCharAt(i);
                i--;
            }
        }
        if (legalCharBuffer.length() == 0) {
            throw new IOException("No legal characters using font " + font.getFamily());
        }
        char[] legalChars = legalCharBuffer.toString().toCharArray();

        // Create the glyphs
        List<Glyph> glyphs = new ArrayList<Glyph>();
        for (Character ch : legalChars) {
            glyphs.add(createGlyph(context, font, ch, fillPaint, stroke, strokePaint,
                    shadowColor, imageFontDef.getShadowX(), imageFontDef.getShadowY(), shadowFilter));
        }

        // Check if any are lowercase
        boolean hasLowercase = false;
        for (char ch : legalChars) {
            if (Character.isLowerCase(ch)) {
                hasLowercase = true;
                break;
            }
        }

        // Get ascent & descent
        int ascent = Integer.MAX_VALUE;
        int descent = Integer.MIN_VALUE;
        for (Glyph glyph : glyphs) {
            BufferedImage image = glyph.getImage();
            int offsetY = glyph.getImageOffset().y;
            int y1 = offsetY;
            int y2 = offsetY + image.getHeight();
            ascent = Math.min(ascent, y1);
            descent = Math.max(descent, y2);
        }
        ascent = -ascent;

        List<Kerning> kerningPairs;
        if (imageFontDef.isMonospace()) {
            // No kerning
            kerningPairs = new ArrayList<Kerning>();

            setAsMonospace(glyphs, false);
        }
        else {
            // Get the kerning pairs
            kerningPairs = KerningFactory.getKerningPairs(context, font, legalChars);

            if (imageFontDef.isMonospaceNumerals()) {
                setAsMonospace(glyphs, true);
            }
        }

        return new ImageFont(name, ascent, descent, hasLowercase, glyphs, kerningPairs);
    }

    private static void setAsMonospace(List<Glyph> glyphs, boolean digitsOnly) {
        float maxAdvance = 0;
        for (Glyph glyph : glyphs) {
            if (!digitsOnly || Character.isDigit(glyph.getCharacter())) {
                maxAdvance = Math.max(maxAdvance, glyph.getAdvance());
            }
        }
        for (Glyph glyph : glyphs) {
            if (!digitsOnly || Character.isDigit(glyph.getCharacter())) {
                float diff = maxAdvance - glyph.getAdvance();
                if (diff > 0) {
                    // Set the advance, center the glyph image as much as possible
                    int dx = (int)diff / 2;
                    glyph.getImageOffset().x += dx;
                    glyph.setAdvance(maxAdvance);
                }
            }
        }
    }

    private static Glyph createGlyph(FontRenderContext context, Font font, char ch,
        Paint fillPaint, BasicStroke stroke, Paint strokePaint, Paint shadowPaint,
        float shadowX, float shadowY, Filter shadowBlur)
    {
        // Get the bounds (make plently of room for the stroke, anti-aliasing, etc.)
        Rectangle2D bounds = font.getMaxCharBounds(context);
        int width = (int)(bounds.getWidth() * 3);
        int height = (int)(bounds.getHeight() * 3);

        // Create the shape (baseline in the middle of the image)
        int originX = width / 3;
        int originY = height / 2;
        GlyphVector glyphVector = font.createGlyphVector(context, Character.toString(ch));
        //GlyphVector glyphVector = font.layoutGlyphVector(context, new char[] { ch }, 0, 1, Font.LAYOUT_LEFT_TO_RIGHT);
        GlyphMetrics metrics = glyphVector.getGlyphMetrics(0);
        Shape shape = glyphVector.getGlyphOutline(0);

        // Create the image buffer
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        setQuality(g, context);
        g.translate(originX, originY);

        // Draw the shadow
        // This might look like shit if anti-alias is off (not tested)
        if (shadowPaint != null && shadowBlur != null) {
            g.translate(shadowX, shadowY);
            g.setPaint(shadowPaint);
            g.fill(shape);
            if (stroke != null) {
                g.setStroke(stroke);
                g.draw(shape);
            }

            image = shadowBlur.filter(image);
            g = image.createGraphics();
            setQuality(g, context);
            g.translate(originX, originY);
        }

        // Draw the character
        if (context.isAntiAliased()) {
            g.setPaint(fillPaint);
            g.fill(shape);
            if (stroke != null) {
                g.setPaint(strokePaint);
                g.setStroke(stroke);
                g.draw(shape);
            }
        }
        else {
            g.setPaint(fillPaint);
            g.setFont(font);
            g.drawString(Character.toString(ch), 0, 0);
        }
        g.dispose();

        // Create the glyph
        Glyph glyph = new Glyph();
        glyph.setCharacter(ch);
        Glyph.Type type;
        if (metrics.isCombining()) {
            type = Glyph.Type.COMBINING;
        }
        else if (metrics.isComponent()) {
            type = Glyph.Type.COMPONENT;
        }
        else if (metrics.isLigature()) {
            type = Glyph.Type.LIGATURE;
        }
        else {
            type = Glyph.Type.STANDARD;
        }
        glyph.setType(type);
        Rectangle visibleBounds = ImageUtils.getVisibleBounds(image);
        if (visibleBounds == null) {
            glyph.setImage(ImageUtils.subImage(image, 0, 0, 1, 1));
            glyph.setImageOffset(new Point(0, 0));
        }
        else {
            glyph.setImage(ImageUtils.subImage(image, visibleBounds.x, visibleBounds.y,
                    visibleBounds.width, visibleBounds.height));
            glyph.setImageOffset(new Point(visibleBounds.x - originX, visibleBounds.y - originY));
        }
        float advanceOffset = 0;
        if (stroke != null) {
            advanceOffset += stroke.getLineWidth();
        }
        glyph.setAdvance(metrics.getAdvance() + advanceOffset);
        return glyph;
    }

    private static void setQuality(Graphics2D g, FontRenderContext context) {
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        if (context.isAntiAliased()) {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
       }
       else {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_NORMALIZE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
       }
       if (context.usesFractionalMetrics()) {
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
       }
       else {
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
       }
    }


    //
    // Filters
    //

    public interface Filter {
        /**
            The filter may edit the contents of the BufferedImage
        */
        public BufferedImage filter(BufferedImage in);
    }

    public static class NoFilter implements Filter {
        public BufferedImage filter(BufferedImage in) {
            return in;
        }
    }

    /**
        3x3 box filter that blurs the alpha and copies the RGB.
        Nearly 2X faster that using ConvolveOp.
    */
    public static class BlurFilter implements Filter {

        public static final int SIZE = 3;

        private final int passes;
        private final int rgb;

        public BlurFilter(int passes, int rgb) {
            this.passes = passes;
            this.rgb = 0xffffff & rgb;
        }

        public BufferedImage filter(BufferedImage in) {
            if (passes > 0) {
                if (in.getType() != BufferedImage.TYPE_INT_ARGB &&
                    in.getType() != BufferedImage.TYPE_INT_RGB)
                {
                    throw new IllegalArgumentException(
                        "Image type must be TYPE_INT_RGB or TYPE_INT_ARGB");
                }

                int imageWidth = in.getWidth();
                int imageHeight = in.getHeight();
                int[] srcData = ((DataBufferInt)in.getRaster().getDataBuffer()).getData();
                int[] temp1 = srcData;
                int[] temp2 = new int[imageWidth * imageHeight];

                for (int i = 0; i < passes; i++) {
                    filter(imageWidth, imageHeight, temp1, temp2);
                    int[] t = temp1;
                    temp1 = temp2;
                    temp2 = t;
                }

                if (temp1 != srcData) {
                    System.arraycopy(temp1, 0, srcData, 0, srcData.length);
                }
            }
            return in;
        }

        protected void filter(int imageWidth, int imageHeight, int[] srcData, int[] destData) {
            int xOffset = SIZE/2;
            int yOffset = SIZE/2;
            int offset;
            int startRowOffset = -yOffset * imageWidth;

            for (int y = yOffset; y < imageHeight-yOffset; y++) {
                offset = y * imageWidth + xOffset;
                for (int x = xOffset; x < imageWidth-xOffset; x++) {
                    int suma = 0;
                    //int sumr = 0;
                    //int sumg = 0;
                    //int sumb = 0;
                    int srcOffset = offset + startRowOffset - xOffset;
                    for (int j = 0; j < SIZE; j++) {
                        for (int i = 0; i < SIZE; i++) {
                            int argb = srcData[srcOffset + i];
                            suma += (argb >>> 24);
                            //sumr += ((argb >> 16) & 0xff);
                            //sumg += ((argb >> 8) & 0xff);
                            //sumb += (argb & 0xff);
                        }
                        srcOffset += imageWidth;
                    }

                    // n / 9 = (n * 3641) >> 15
                    suma = (suma * 3641) >> 15;
                    //sumr = (sumr * 3641) >> 15;
                    //sumg = (sumg * 3641) >> 15;
                    //sumb = (sumb * 3641) >> 15;

                    //destData[offset] = (suma << 24) | (sumr << 16) | (sumg << 8) | sumb;
                    destData[offset] = (suma << 24) | rgb;
                    offset++;
                }
            }
        }
    }
}
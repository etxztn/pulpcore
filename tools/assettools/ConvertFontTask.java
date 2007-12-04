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

package pulpcore.assettools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.FontFormatException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import pulpcore.assettools.png.PNGWriter;

public class ConvertFontTask extends Task {
    
    private static final long MAGIC = 0x70756c70666e740bL; // "pulpfnt" 0.11
    
    private File srcFile;
    private File destFile;
    
    
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
    
    
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }
    
    
    public void execute() throws BuildException {
        if (srcFile == null) {
            throw new BuildException("The srcFile is not specified.");
        }
        if (destFile == null) {
            throw new BuildException("The destFile is not specified.");
        }
        
        log("Converting: " + srcFile, Project.MSG_VERBOSE);
        
        try {
            create();
        }
        catch (FontFormatException ex) {
            throw new BuildException("Error creating font " + srcFile, ex);
        }
        catch (IOException ex) {
            throw new BuildException("Error creating font " + srcFile, ex);
        }
    }
    

    //
    //
    //
    
    
    private String chars;
    
    private String[] fontNames;
    private float fontSize;
    private int fontStyle;
    private int fontTracking;
    
    private int[] fontBearingLeft;
    private int[] fontBearingRight;
    private boolean hasBearing;
    
    private Paint backgroundPaint;
    private Color fillColor1;
    private Color fillColor2;
    private Paint strokePaint;
    private Stroke stroke;
    
    private float shadowX;
    private float shadowY;
    private Paint shadowPaint;
    
    private int numColors;
    
    private boolean monospace;
    private boolean monospaceNumerals;
    
    private final boolean autoKern = true;
    
    
    private void create() throws IOException, FontFormatException {
        try {
            loadProperties(srcFile);
        }
        catch (NumberFormatException ex) {
            log(ex.getMessage(), Project.MSG_WARN);
            return;
        }
        
        createFont();
    }
    
    
    private void loadProperties(File file) throws IOException, NumberFormatException {
        CoreProperties props = new CoreProperties();
        props.load(new FileInputStream(file));
        
        fontBearingLeft = new int[Character.MAX_VALUE];
        fontBearingRight = new int[Character.MAX_VALUE];
        hasBearing = false;
        
        List<Character> charList = new ArrayList<Character>();
        String[] charsets = props.getListProperty("chars");
        if (charsets == null || charsets.length == 0 || 
            charsets[0] == null || charsets[0].length() == 0)
        {
            // Default: basic Latin
            for (char ch = ' '; ch <= '~' ; ch++) {
                charList.add(ch);
            }
        }
        else {
            for (int i = 0; i < charsets.length; i++) {
                String charset = charsets[i];
                if (charset == null) {
                    continue;
                }
                if (charset.length() == 1) {
                    charList.add(charset.charAt(0));
                }
                else if (charset.length() == 3 && charset.charAt(1) == '-') {
                    char firstChar = charset.charAt(0);
                    char lastChar = charset.charAt(2);
                    for (char ch = firstChar; ch <= lastChar ; ch++) {
                        charList.add(ch);
                    }
                }
                else {
                    throw new BuildException("Unsupported charset description: " + charset);
                }
            }
        }
        
        // Sort the chars and remove duplicates
        Collections.sort(charList);
        chars = Character.toString(charList.get(0));
        for (int i = 1; i < charList.size(); i++) {
            if (charList.get(i) != chars.charAt(chars.length() - 1)) {
                chars += charList.get(i);
            }
        }
        
        fontNames = props.getListProperty("family", "dialog");
        fontSize = props.getFloatProperty("size", 16);
        fontTracking = props.getIntProperty("tracking", 0);
        fontStyle = Font.PLAIN;
        String styleString = props.getProperty("style", "plain").toLowerCase();
        if (styleString.contains("bold")) {
            fontStyle |= Font.BOLD;
        }
        
        if (styleString.contains("italic")) {
            fontStyle |= Font.ITALIC;
        }
        
        // Find any bearing
        for (int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            int bearingLeft = 0;
            int bearingRight = 0;
            if (props.hasProperty("kerning.left." + ch)) {
                log("The kerning.left.* property is deprecated. Use bearing.left.*",
                    Project.MSG_WARN);
                bearingLeft = props.getIntProperty("kerning.left." + ch, 0);
            }
            if (props.hasProperty("bearing.left." + ch)) {
                bearingLeft = props.getIntProperty("bearing.left." + ch, 0);
            }
            
            if (props.hasProperty("kerning.right." + ch)) {
                log("The kerning.right.* property is deprecated. Use bearing.right.*",
                    Project.MSG_WARN);
                bearingRight = props.getIntProperty("kerning.right." + ch, 0);
            }
            if (props.hasProperty("bearing.right." + ch)) {
                bearingRight = props.getIntProperty("bearing.right." + ch, 0);
            }
            
            if (bearingLeft != 0) {
                hasBearing = true;
                fontBearingLeft[ch] = bearingLeft;
            }
            
            if (bearingRight != 0) {
                hasBearing = true;
                fontBearingRight[ch] = bearingRight;
            }
        }
        
        backgroundPaint = props.getColorProperty("background.color", null);
        fillColor1 = props.getColorProperty("color", Color.BLACK);
        fillColor2 = props.getColorProperty("gradient.color", null);
        strokePaint = props.getColorProperty("stroke.color", null);
        
        float strokeSize = props.getFloatProperty("stroke.size", 0);
        if (strokeSize > 0) {
            stroke = new BasicStroke(strokeSize);
        }
        else {
            stroke = null;
        }
        
        shadowX = props.getFloatProperty("shadow.x", 0);
        shadowY = props.getFloatProperty("shadow.y", 0);
        shadowPaint = props.getColorProperty("shadow.color", null);
        
        numColors = props.getIntProperty("colors", 0);
        
        monospace = props.getProperty("monospace", "false").equals("true");
        monospaceNumerals = props.getProperty("monospace.numerals", "false").equals("true");
        
        Iterator<String> i = props.getUnrequestedKeys();
        while (i.hasNext()) {
            throw new BuildException(file + 
                " has unknown property \"" + i.next() + "\"");
        }
        
    }
    
    
    private void createFont() throws IOException, FontFormatException, BuildException {
        
        long startTime = System.nanoTime();
        
        // Create the AWT Font 
        String[] a =  
            GraphicsEnvironment.getLocalGraphicsEnvironment().
            getAvailableFontFamilyNames();
            
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i].toLowerCase();
        }
        List <String> available = Arrays.asList(a);
        
        Font font = new Font("Dialog", Font.PLAIN, 1);
        for (int i = 0; i < fontNames.length; i++) {
            String fontName = fontNames[i].trim();
            try {
                if (fontName.toLowerCase().endsWith(".ttf")) { 
                    font = Font.createFont(Font.TRUETYPE_FONT, new File(srcFile.getParentFile(), fontName));
                    break;
                }
                else if (available.contains(fontName.toLowerCase())) {
                    font = new Font(fontName, Font.PLAIN, 1);
                    break;
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        font = font.deriveFont(fontStyle, fontSize);
        
        // Determine the legal chars using this font
        StringBuffer legalChars = new StringBuffer(chars);
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            if (!font.canDisplay(ch)) {
                legalChars.deleteCharAt(i);
                i--;
            }
        }
        if (legalChars.length() == 0) {
            throw new IOException("No legal characters using font " + font.getFamily());
        }
        
        // Create the raster font image
        char firstChar = legalChars.charAt(0);
        char lastChar = legalChars.charAt(legalChars.length() - 1);
        int[] widths = new int[lastChar - firstChar + 1];
        BufferedImage image = renderFont(font, legalChars.toString(), widths);
        
        
        // Reduce the colors of the image
        if (numColors > 0) {
            int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            
            QuantizeARGB quantize = new QuantizeARGB(6);
            quantize.reduce(data, numColors);
        }
        
        // Write the font info
        ByteArrayOutputStream fontData = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(fontData);
        out.writeLong(MAGIC);
        out.writeChar(firstChar);
        out.writeChar(lastChar);
        out.writeShort((short)fontTracking);
        out.writeBoolean(hasBearing);
        int position = 0;
        for (int i=0; i<widths.length; i++) {
            out.writeShort((short)position);
            position+=widths[i];
        }
        out.writeShort((short)position);
        if (hasBearing) {
            for (int i = firstChar; i <= lastChar; i++) {
                out.writeShort(fontBearingLeft[i]);
                out.writeShort(fontBearingRight[i]);
            }
        }
        out.close();
        
        
        // Save the image
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
        PNGWriter writer = new PNGWriter();
        String imageDescription = writer.write(image, 0, 0, null, fontData.toByteArray(), out);
        out.close();
        
        //
        // Log diagnostic info
        //
        
        long time = (System.nanoTime() - startTime) / 1000000;
        
        //String description = srcFile + " (" + time + "ms): " + imageDescription;
        String description = destFile + " (" + font.getFamily() + " font, " + imageDescription + ")";
        log("Created: " + description, Project.MSG_INFO);
    }
    
    
    private BufferedImage renderFont(Font font, String legalChars, int[] widths) {
        
        char firstChar = legalChars.charAt(0);
        char lastChar = legalChars.charAt(legalChars.length() - 1);
        int maxWidth = 0;
        int minY = Integer.MAX_VALUE;
        int maxY = -1; 
        
        // Find the character bounds
        Rectangle[] bounds = new Rectangle[legalChars.length()];
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            bounds[i] = calcBounds(drawChar(font, ch, Color.WHITE, Color.BLACK,
                stroke, Color.BLACK, Color.BLACK, shadowX, shadowY));
            int w = bounds[i].width;
            int h = bounds[i].height;
            
            if (w > 0 && h > 0) {
                widths[ch - firstChar] = w;
                maxWidth = Math.max(maxWidth, w);
                minY = Math.min(minY, bounds[i].y);
                maxY = Math.max(maxY, bounds[i].y + bounds[i].height - 1);
            }
            else {
                // A space character - calculate later
                bounds[i] = null;
            }
        }
        
        // Calculate width of monospace numerals 0 - 9
        int maxNumeralWidth = 0;
        if (!monospace && monospaceNumerals) {
            for (int ch = '0'; ch <= '9'; ch++) {
                int w = widths[ch - firstChar];
                maxNumeralWidth = Math.max(maxNumeralWidth, w);
            }
        }
        
        // Calculate width of the whitespace characters
        for (int i = 0; i < legalChars.length(); i++) {
            if (bounds[i] == null) {
                char ch = legalChars.charAt(i);
                if (monospace) {
                    widths[ch - firstChar] = maxWidth;
                }
                else {
                    GlyphMetrics metrics = getMetrics(font, ch);
                    widths[ch - firstChar] = Math.round(metrics.getAdvance());
                }
            }
        }
        
        // Calculate final image dimensions
        int width = 0;
        int height = maxY - minY + 1;
        for (int i = 0; i < widths.length; i++) {
            char ch = (char)(i + firstChar);
            boolean isNumeral = (ch >= '0' && ch <= '9');
            
            if (monospace && widths[i] != 0) {
                width += maxWidth;
            }
            else if (monospaceNumerals && isNumeral && widths[i] != 0) {
                width += maxNumeralWidth;
            }
            else {
                width += widths[i];
            }
        }
        
        Paint fillPaint = fillColor1;
        if (fillColor2 != null) {
            fillPaint = new GradientPaint(
                new Point(0, 0),
                fillColor1,
                new Point(0, height),
                fillColor2);
        }
        
        // Draw the final image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        Graphics2D g = image.createGraphics();
        setHighQuality(g);
        if (backgroundPaint != null) {
            g.setPaint(backgroundPaint);
            g.fillRect(0, 0, width, height);
        }        
        
        for (int i = 0; i < legalChars.length(); i++) {
            char ch = legalChars.charAt(i);
            int xOffset = 0;
            boolean isNumeral = (ch >= '0' && ch <= '9');
            
            if (monospace) {
                xOffset += (maxWidth - widths[ch - firstChar]) / 2; 
                widths[ch - firstChar] = maxWidth;
            }
            else if (monospaceNumerals && isNumeral) {
                xOffset += (maxNumeralWidth - widths[ch - firstChar] + 1) / 2; 
                widths[ch - firstChar] = maxNumeralWidth;
                fontBearingLeft[ch] = 0;
                fontBearingRight[ch] = 0;
            }
            
            if (bounds[i] != null) {
                xOffset += x - bounds[i].x;
                
                BufferedImage charImage = drawChar(font, ch, null, fillPaint,
                    stroke, strokePaint, shadowPaint, shadowX, shadowY);
                
                if (autoKern && !monospace && !(monospaceNumerals && isNumeral)) {
                    autoKern(font, ch, charImage, bounds[i].x, bounds[i].x + bounds[i].width - 1);
                }
                
                //if (backgroundPaint != null) {
                //    g.setPaint(backgroundPaint);
                //    g.fillRect(xOffset, -minY, charImage.getWidth(), charImage.getHeight());
                //}
                g.drawImage(charImage, xOffset, -minY, null);
            }
            x += widths[ch - firstChar];
        }

        return image;
    }
    
    
    private void autoKern(Font font, char ch, BufferedImage image, int x1, int x2) {
        
        GlyphMetrics metrics = getMetrics(font, ch);
        float w = (float)metrics.getBounds2D().getWidth();
        if (w > 0) {
            //System.out.println(ch + ": lsb=" + metrics.getLSB() + " rsb=" + metrics.getRSB());
            int lsb = (int)metrics.getLSB();
            float lsbDiff = metrics.getLSB() - lsb;
            int rsb = Math.round(metrics.getRSB() + lsbDiff);
            
            if (lsb != 0 || rsb != 0) {
                hasBearing = true;
                fontBearingLeft[ch] += lsb;
                fontBearingRight[ch] += rsb;
            }
        }
    }
    
    
    private Rectangle calcBounds(BufferedImage image) {
        int boundX1 = image.getWidth() + 1; 
        int boundX2 = -1;
        int boundY1 = image.getHeight() + 1;
        int boundY2 = -1;
        
        int backgroundColor = Color.WHITE.getRGB();
        
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                if (image.getRGB(i, j) != backgroundColor) {
                    boundX1 = Math.min(boundX1, i);
                    boundX2 = Math.max(boundX2, i);
                    boundY1 = Math.min(boundY1, j);
                    boundY2 = Math.max(boundY2, j);
                }
            }
        }
        
        return new Rectangle(boundX1, boundY1, boundX2 - boundX1 + 1, boundY2 - boundY1 + 1);
    }
    
    
    private BufferedImage drawChar(Font font, char ch,
        Paint backgroundPaint, Paint fillPaint, Stroke stroke, Paint strokePaint,
        Paint shadowPaint, float shadowX, float shadowY)
    {
        // Get the bounds (make plently of room for the stroke, anti-aliasing, etc.)
        FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);
        Rectangle2D bounds = font.getMaxCharBounds(context);
        int width = (int)Math.round(bounds.getWidth() * 3);
        int height = (int)Math.round(bounds.getHeight() * 3);
        
        // Create the shape (baseline in the middle of the image)
        int x = width / 3;
        int y = height / 2;
        Shape shape = font.createGlyphVector(context, Character.toString(ch)).getOutline(x, y);
        
        // Create the image buffer
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        setHighQuality(g);

        // Draw the background
        if (backgroundPaint != null) {
            g.setPaint(backgroundPaint);
            g.fillRect(0, 0, width, height);
        }
        
        // Draw the shadow
        if (shadowPaint != null) {
            g.translate(shadowX, shadowY);
            g.setPaint(shadowPaint);
            g.fill(shape);
            if (stroke != null) {
                g.setStroke(stroke);
                g.draw(shape);
            }
            g.translate(-shadowX, -shadowY);
        }
        
        // Draw the character
        g.setPaint(fillPaint);
        g.fill(shape);
        if (stroke != null) {
            g.setPaint(strokePaint);
            g.setStroke(stroke);
            g.draw(shape);
        }
        
        return image;
    }
    
    
    private void setHighQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);         
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    
    private GlyphMetrics getMetrics(Font font, char ch) {
        FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);
        return font.createGlyphVector(context, Character.toString(ch)).getGlyphMetrics(0);
    }
}
package org.pulpcore.tools.packer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.pulpcore.tools.imagefont.Glyph;
import org.pulpcore.tools.imagefont.ImageFont;
import org.pulpcore.tools.imagefont.Kerning;
import org.pulpcore.tools.packer.TexturePacker.Image;

public class OutputCocos2D {

    private OutputCocos2D() {

    }

    static void output(List<Bin2D<Image>> bins, int padding, List<ImageFont> fonts,
            File destDir, String filePrefix, String fileSuffix) throws IOException {

        if (padding <= 0) {
            System.err.println("Warning: padding is zero, but most systems need a padding of " +
                    "at least 1.");
        }
        // One plist for each bin
        for (int i = 0; i < bins.size(); i++) {
            Bin2D<Image> bin = bins.get(i);
            String name = filePrefix + i + fileSuffix;
            outputBinAsPlist(bin, destDir, name);
        }
        // One BMFont file for each Font
        for (ImageFont font : fonts) {
            outputFontAsBMFont(font, bins, destDir, filePrefix, fileSuffix);
        }
    }

    private static void outputBinAsPlist(Bin2D<Image> bin, File destDir, String name) throws IOException {

        if (bin.getWidth() > 2048 || bin.getHeight() > 2048) {
            System.err.println("Warning: iPhone 4 texture size is 2048x2048, requested size is " +
                    bin.getWidth() + "x" + bin.getHeight() + ".");
        }

        File destFile = new File(destDir, name + ".plist");
        List<Image> list = bin.getObjects();

        PrintWriter out = new PrintWriter(destFile, "UTF-8");
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">");
        out.println("<plist version=\"1.0\">");
        out.println("<dict>");

        out.println("    <key>" + name + "</key>");
        out.println("    <dict>");
        out.println("        <key>width</key>");
        out.println("        <integer>" + bin.getWidth() + "</integer>");
        out.println("        <key>height</key>");
        out.println("        <integer>" + bin.getHeight() + "</integer>");
	out.println("    </dict>");
        out.println("    <key>metadata</key>");
        out.println("    <dict>");
        out.println("        <key>format</key>");
        out.println("        <integer>1</integer>");
        out.println("        <key>textureFileName</key>");
        out.println("        <string>" + name + ".png</string>");
	out.println("    </dict>");
        out.println("    <key>frames</key>");
        out.println("    <dict>");
        for (Image image : list) {
            if (image.getGlyph() == null) {
                float offsetX = image.visibleBounds.x + (image.visibleBounds.width - image.getOriginalWidth()) / 2.0f;
                float offsetY = -(image.visibleBounds.y + (image.visibleBounds.height - image.getOriginalHeight()) / 2.0f);
                out.println("        <key>" + image.getName() + "</key>");
                out.println("        <dict>");
                out.println("            <key>frame</key>");
                out.println("            <string>{{" + image.getVisibleX() + "," + image.getVisibleY() + "},{" + image.visibleBounds.width + "," + image.visibleBounds.height + "}}</string>");
                out.println("            <key>offset</key>");
                out.println("            <string>{" + offsetX + "," + offsetY + "}</string>");
                out.println("            <key>sourceSize</key>");
                out.println("            <string>{" + image.getOriginalWidth() + "," + image.getOriginalHeight() + "}</string>");
                out.println("        </dict>");
            }
        }
	out.println("    </dict>");
        out.println("</dict>");
        out.println("</plist>");
        out.close();
    }

    private static void outputFontAsBMFont(ImageFont font, List<Bin2D<Image>> bins,
            File destDir, String filePrefix, String fileSuffix) throws IOException {

        boolean gaveCharWarning = false;
        int pageWidth = 0;
        int pageHeight = 0;

        // Find pages
        List<String> pageNames = new ArrayList<String>();
        for (int i = 0; i < bins.size(); i++) {
            Bin2D<Image> bin = bins.get(i);
            String textureName = filePrefix + i + fileSuffix + ".png";
            for (Image image : bin.getObjects()) {
                Glyph glyph = image.getGlyph();
                if (glyph != null && glyph.getFont() == font) {
                    pageNames.add(textureName);
                    pageWidth = bin.getWidth();
                    pageHeight = bin.getHeight();
                    break;
                }
            }
        }

        if (pageNames.size() == 0) {
            throw new IOException("No pages for font " + font.getName());
        }
        else if (pageNames.size() > 1) {
            System.err.println("Warning: cocos2d does not support multiple-page BMFonts");
        }

        // Font info
        File destFile = new File(destDir, font.getName() + fileSuffix + ".fnt");
        PrintWriter out = new PrintWriter(destFile, "UTF-8");
        int lineHeight = font.getAscent() + font.getDescent();
        out.println("common lineHeight=" + lineHeight + " base=" + font.getAscent() +
                " scaleW=" + pageWidth + " scaleH=" + pageHeight + " pages=" + pageNames.size() + " packed=0");
        for (int i = 0; i < pageNames.size(); i++) {
            out.println("page id=" + i + " file=\"" + pageNames.get(i) + "\"");
        }
        out.println("chars count=" + font.getGlyphs().size());

        // Glyphs
        for (int i = 0; i < bins.size(); i++) {
            Bin2D<Image> bin = bins.get(i);
            List<Image> list = bin.getObjects();
            for (int j = 0; j < list.size(); j++) {
                Image image = list.get(j);
                Glyph glyph = image.getGlyph();
                if (glyph != null && glyph.getFont() == font) {
                    if (!gaveCharWarning && glyph.getCharacter() >= 2048) {
                        System.err.println("Warning: By default, cocos2d only supports chars 0 through 2047");
                        gaveCharWarning = true;
                    }
                    out.println("char id=" + Integer.toString(glyph.getCharacter()) +
                        " x=" + image.getVisibleX() + " y=" + image.getVisibleY() +
                        " width=" +  image.getVisibleWidth() + " height=" + image.getVisibleHeight() +
                        " xoffset=" + glyph.getImageOffset().x + 
                        " yoffset=" + (glyph.getImageOffset().y + font.getAscent()) +
                        " xadvance=" + Math.round(glyph.getAdvance()) +
                        " page=" + i + " chnl=0");
                }
            }
        }

        // Kerning pairs
        out.println("kernings count=" + font.getKerningPairs().size());
        for (Kerning kerning : font.getKerningPairs()) {
            int k =  Math.round(kerning.getKerning());
            if (k != 0) {
                out.println("kerning first=" + Integer.toString(kerning.getCurrChar()) +
                        " second=" + Integer.toString(kerning.getNextChar()) +
                        " amount=" + k);
            }
        }

        out.close();
    }
}

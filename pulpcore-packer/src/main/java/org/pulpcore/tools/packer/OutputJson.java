package org.pulpcore.tools.packer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.pulpcore.tools.imagefont.Glyph;
import org.pulpcore.tools.imagefont.ImageFont;
import org.pulpcore.tools.imagefont.Kerning;
import org.pulpcore.tools.packer.TexturePacker.Image;

public class OutputJson {

    private OutputJson() {

    }

    static void output(List<Bin2D<Image>> bins, int padding, List<ImageFont> fonts,
            File destDir, String filePrefix, String fileSuffix) throws IOException {

        if (padding <= 0) {
            System.err.println("Warning: padding is zero, but most systems need a padding of " +
                    "at least 1.");
        }
                
        File destFile = new File(destDir, filePrefix + fileSuffix + ".json");
        PrintWriter out = new PrintWriter(destFile, "UTF-8");
        out.println("{");
        out.println("    \"version\": 1,");
        out.println("    \"padding\": " + padding + ",");
        out.println("    \"textures\": [");
        for (int i = 0; i < bins.size(); i++) {
            String textureName = filePrefix + i + fileSuffix + ".png";
            if (i != 0) {
                out.println(",");
            }
            out.print("        \"" + textureName + "\"");
        }
        out.println();
        out.print("    ]");

        // Images
        boolean imagePrinted = false;

        for (int i = 0; i < bins.size(); i++) {
            Bin2D<Image> bin = bins.get(i);
            if (bin.getWidth() > 512 || bin.getHeight() > 512) {
                System.err.println("Warning: Android G1 texture size is 512x512, requested size is " +
                        bin.getWidth() + "x" + bin.getHeight() + ".");
            }
            
            String textureName = filePrefix + i + fileSuffix + ".png";
            List<Image> list = bin.getObjects();
            for (int j = 0; j < list.size(); j++) {
                Image image = list.get(j);
                if (image.getGlyph() == null) {
                    out.println(",");
                    if (!imagePrinted) {
                        out.println("    \"images\": {");
                    }
                    out.println("        \"" + image.getName() + "\": ");
                    out.println("        {");
                    out.println("            \"opaque\": " + image.isOpaque() + ",");
                    out.println("            \"texture\": \"" + textureName + "\",");
                    out.print("            \"textureRect\": [" + image.getVisibleX() + ", " + image.getVisibleY() + ", " + image.getVisibleWidth() + ", " + image.getVisibleHeight() + "]");
                    if (image.getVisibleWidth() != image.getOriginalWidth() || image.getVisibleHeight() != image.getOriginalHeight()) {
                        out.println(",");
                        out.print("            \"size\": [" + image.getOriginalWidth() + ", " + image.getOriginalHeight() + "]");
                    }
                    if (image.visibleBounds.x != 0 || image.visibleBounds.y != 0) {
                        out.println(",");
                        out.print("            \"offset\": [" + image.visibleBounds.x + ", " + image.visibleBounds.y + "]");
                    }
                    out.println();
                    out.print("        }");
                    imagePrinted = true;
                }
            }
        }
        if (imagePrinted) {
            out.println();
            out.print("    }");
        }

        // Fonts
        boolean fontPrinted = false;
        for (ImageFont font : fonts) {
            out.println(",");
            if (!fontPrinted) {
                out.println("    \"fonts\": {");
            }
            fontPrinted = true;
            out.println("        \"" + font.getName() + "\": {");
            out.println("            \"ascent\": " + font.getAscent() + ",");
            out.println("            \"descent\": " + font.getDescent() + ",");
            out.println("            \"hasLowercase\": " + font.hasLowercase() + ",");

            // Determine most popular texture, use it as the default.
            int[] textureCounts = new int[bins.size()];
            int mostPopularTexture = 0;
            for (int i = 0; i < bins.size(); i++) {
                Bin2D<Image> bin = bins.get(i);
                List<Image> list = bin.getObjects();
                for (int j = 0; j < list.size(); j++) {
                    Image image = list.get(j);
                    Glyph glyph = image.getGlyph();
                    if (glyph != null && glyph.getFont() == font) {
                        textureCounts[i]++;
                        if (textureCounts[i] > textureCounts[mostPopularTexture]) {
                            mostPopularTexture = i;
                        }
                    }
                }
            }

            String defaultTextureName = filePrefix + mostPopularTexture + fileSuffix + ".png";
            out.println("            \"texture\": \"" + defaultTextureName + "\",");

            // Print out glyphs
            out.println("            \"glyphs\": [");
            boolean glyphPrinted = false;
            for (int i = 0; i < bins.size(); i++) {
                Bin2D<Image> bin = bins.get(i);
                String textureName = filePrefix + i + fileSuffix + ".png";
                List<Image> list = bin.getObjects();
                for (int j = 0; j < list.size(); j++) {
                    Image image = list.get(j);
                    Glyph glyph = image.getGlyph();
                    if (glyph != null && glyph.getFont() == font) {
                        if (glyphPrinted) {
                            out.println(",");
                        }
                        glyphPrinted = true;
                        out.println("                {");
                        out.println("                    \"char\": " + Integer.toString(glyph.getCharacter()) + ",");
                        if (glyph.getType() != Glyph.Type.STANDARD) {
                            out.println("                    \"type\": \"" + glyph.getType().toString().toLowerCase() + "\",");
                        }
                        if (i != mostPopularTexture) {
                            out.println("                    \"texture\": \"" + textureName + "\",");
                        }
                        out.println("                    \"textureRect\": [" + image.getVisibleX() + ", " + image.getVisibleY() + ", " + image.getVisibleWidth() + ", " + image.getVisibleHeight() + "],");
                        out.println("                    \"offset\": [" + glyph.getImageOffset().x + ", " + glyph.getImageOffset().y + "],");
                        out.println("                    \"advance\": " + Math.round(glyph.getAdvance()) + "");
                        out.print("                }");
                    }
                }
            }
            if (glyphPrinted) {
                out.println();
            }
            out.print("            ]");

            // Kerning
            List<Kerning> kerningPairs = font.getKerningPairs();
            if (kerningPairs.size() > 0) {
                boolean kerningPairPrinted = false;
                out.println(",");
                out.println("            \"kerningPairs\": [");
                for (Kerning pair : kerningPairs) {
                    int k = Math.round(pair.getKerning());
                    if (k != 0) {
                        if (kerningPairPrinted) {
                            out.println(",");
                        }
                        out.print("                [ " + Integer.toString(pair.getCurrChar()) + ", " +
                                  Integer.toString(pair.getNextChar()) + ", " +
                                  k + " ]");
                        kerningPairPrinted = true;
                    }
                }
                out.println();
                out.println("            ]");
            }
            else {
                out.println();
            }
            out.print("        }");
        }
        if (fontPrinted) {
            out.println();
            out.println("    }");
        }

        // TODO: tiled images
        /*
        {
                "name": "BigImage.png",
                "tiles": [
                                {
                                        "source": "pack1.png",
                                        "sourceRect": [0, 0, 400, 512],
                                        "isOpaque": true
                                }.
                                {
                                        "source": "pack2.png",
                                        "sourceRect": [0, 0, 400, 88],
                                        "isOpaque": true
                                }
                ],
                "size": [400, 600],
                "numXTiles": 1,
                "numYTiles": 2,
                "offset": [0, 0]
        }
         */


        out.println("}");
        out.close();
    }
}

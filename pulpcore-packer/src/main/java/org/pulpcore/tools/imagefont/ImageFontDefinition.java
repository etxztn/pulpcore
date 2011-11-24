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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.awt.Color;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageFontDefinition implements Cloneable {

    // GSON support

    private static final String HEX_CHARS = "0123456789ABCDEF";

    private static String byteToHex(int b) {
        return "" + HEX_CHARS.charAt(b >> 4) + HEX_CHARS.charAt(b & 0xf);
    }

    private static Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Color.class, new ColorSerializer());
        return gsonBuilder.create();
    }

    public static ImageFontDefinition fromJson(Reader reader) {
        return createGson().fromJson(reader, ImageFontDefinition.class);
    }

    private static class ColorSerializer implements JsonSerializer<Color>, JsonDeserializer<Color> {
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            int r = src.getRed();
            int g = src.getGreen();
            int b = src.getBlue();
            int a = src.getAlpha();

            String s = byteToHex(r) + byteToHex(g) + byteToHex(b);

            if (a != 255) {
                s = byteToHex(a) + s;
            }

            return new JsonPrimitive("#" + s);
        }

        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

            String value = json.getAsJsonPrimitive().getAsString();
            if (value == null) {
                return null;
            }
            value = value.trim();
            if (value.equals("null")) {
                return null;
            }
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            value = value.trim();
            
            int color;
            try {
                color = (int)Long.parseLong(value, 16);
            }
            catch (NumberFormatException ex) {
                throw new JsonParseException("\"" + value + "\" is not a color.");
            }

            if (value.length() == 6) {
                return new Color(color, false);
            }
            else if (value.length() == 8) {
                return new Color(color, true);
            }
            else {
                throw new JsonParseException("\"" + value + "\" is not a color.");
            }
        }
    }

    private String[] charGroups = {" -~", "\u00A0-\u00FF", "\u2010-\u201f", "\uFFFD"};
    private String[] family = { "Helvetica Neue", "Helvetica", "SansSerif"};
    private float size = 16;
    private boolean bold = false;
    private boolean italic = false;
    private Color color = Color.BLACK;
    private Color bottomColor = null;
    private float colorOffset = 0;
    private Color strokeColor = null;
    private float strokeSize = 0;
    private float shadowX = 0;
    private float shadowY = 0;
    private Color shadowColor = null;
    private int shadowBlurPasses = 1;
    private boolean monospace = false;
    private boolean monospaceNumerals = false;
    private boolean antiAlias = true;
    private boolean fractionalMetrics = true;

    @Override
    protected Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            // Won't happen
            return null;
        }
    }

    public ImageFontDefinition getScaledClone(double scale) {
        ImageFontDefinition clone = (ImageFontDefinition) this.clone();
        clone.size *= scale;
        clone.colorOffset *= scale;
        clone.strokeSize *= scale;
        clone.shadowX *= scale;
        clone.shadowY *= scale;
        return clone;
    }

    public String getSortedChars() throws IllegalArgumentException {
        List<Character> charList = new ArrayList<Character>();
        if (charGroups == null || charGroups.length == 0 || charGroups[0] == null || charGroups[0].length() == 0) {
            // Default: basic Latin
            for (char ch = ' '; ch <= '~'; ch++) {
                charList.add(ch);
            }
        }
        else {
            for (String charGroup : charGroups) {
                if (charGroup == null || charGroup.length() == 0) {
                    charList.add(' ');
                }
                else if (charGroup.length() == 1) {
                    charList.add(charGroup.charAt(0));
                }
                else if (charGroup.length() == 3 && charGroup.charAt(1) == '-') {
                    char firstChar = charGroup.charAt(0);
                    char lastChar = charGroup.charAt(2);
                    for (char ch = firstChar; ch <= lastChar; ch++) {
                        charList.add(ch);
                    }
                }
                else {
                    throw new IllegalArgumentException("Unsupported char group description: " + charGroup);
                }
            }
        }
        // Sort the chars and remove duplicates
        Collections.sort(charList);
        String chars = Character.toString(charList.get(0));
        for (int i = 1; i < charList.size(); i++) {
            if (charList.get(i) != chars.charAt(chars.length() - 1)) {
                chars += charList.get(i);
            }
        }
        return chars;
    }

    public String toJson() {
        return createGson().toJson(this);
    }

    public void toJson(Appendable writer) {
        createGson().toJson(this, writer);
    }

    public boolean isFractionalMetrics() {
        return fractionalMetrics;
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        this.fractionalMetrics = fractionalMetrics;
    }

    public int getShadowBlurPasses() {
        return shadowBlurPasses;
    }

    public void setShadowBlurPasses(int shadowBlurPasses) {
        this.shadowBlurPasses = shadowBlurPasses;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
    }

    public float getShadowX() {
        return shadowX;
    }

    public void setShadowX(float shadowX) {
        this.shadowX = shadowX;
    }

    public float getShadowY() {
        return shadowY;
    }

    public void setShadowY(float shadowY) {
        this.shadowY = shadowY;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        this.antiAlias = antiAlias;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public Color getBottomColor() {
        return bottomColor;
    }

    public void setBottomColor(Color bottomColor) {
        this.bottomColor = bottomColor;
    }

    public String[] getCharGroups() {
        return charGroups;
    }

    public void setCharGroups(String[] charGroups) {
        this.charGroups = charGroups;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public float getColorOffset() {
        return colorOffset;
    }

    public void setColorOffset(float colorOffset) {
        this.colorOffset = colorOffset;
    }

    public String[] getFamily() {
        return family;
    }

    public void setFamily(String[] family) {
        this.family = family;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isMonospace() {
        return monospace;
    }

    public void setMonospace(boolean monospace) {
        this.monospace = monospace;
    }

    public boolean isMonospaceNumerals() {
        return monospaceNumerals;
    }

    public void setMonospaceNumerals(boolean monospaceNumerals) {
        this.monospaceNumerals = monospaceNumerals;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public float getStrokeSize() {
        return strokeSize;
    }

    public void setStrokeSize(float strokeSize) {
        this.strokeSize = strokeSize;
    }

    public boolean usesFractionalMetrics() {
        return fractionalMetrics;
    }

    public void setUseFractionalMetrics(boolean fractionalMetrics) {
        this.fractionalMetrics = fractionalMetrics;
    }

}

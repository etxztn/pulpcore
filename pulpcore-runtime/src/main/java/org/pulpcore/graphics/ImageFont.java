package org.pulpcore.graphics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pulpcore.runtime.Context;

public class ImageFont {

    /**
     Loads an ImageFont. If the ImageFont was previously loaded, the same object instance is returned.
     */
    public static ImageFont load(String fontName) {
        return Context.getContext().loadFont(fontName);
    }

    public static ImageFont getDefaultFont() {
        return Context.getContext().loadFont("default");
    }
    
    /*
    TODO: Tinted fonts?
    First make sure the texture parent is the same for every glyph, then:
    texture = texture.createMutableCopy();
    Graphics g = texture.createGraphics();
    g.setBlendMode(Graphics.BlendMode.SRC_ATOP);
    g.setColor(Color.PURPLE);
    g.fill();
    texture = texture.createImmutableCopy(); 
    */

    /**
     The ImageFont.Glyph class represents a single character as a Texture.
     */
    public static class Glyph implements Comparable<Glyph> {
        private final char ch;
        private final Texture texture;
        private final int offsetX;
        private final int offsetY;
        private final float advanceX;
        private Map<Character, Float> kerning;

        public Glyph(char ch, Texture texture, int offsetX, int offsetY, float advanceX) {
            this.ch = ch;
            this.texture = texture;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.advanceX = advanceX;
        }

        /** The character this Glyph represents. */
        public char getCharacter() {
            return ch;
        }

        /** The Texture, which is most likely a sub-region of a larger Texture .*/
        public Texture getTexture() {
            return texture;
        }

        /** The x offset to display the Texture, relative to the origin. */
        public int getOffsetX() {
            return offsetX;
        }

        /** The y offset to display the Texture, relative to the origin. */
        public int getOffsetY() {
            return offsetY;
        }

        /** The amount to advance the origin after drawing this Glyph. */
        public float getAdvanceX() {
            return advanceX;
        }
        
        private void addKerning(Character second, float amount) {
            if (amount != 0) {
                if (kerning == null) {
                    kerning = new HashMap<Character, Float>();
                }
                kerning.put(second, amount);
            }
        }

        /** The Kerning between this character and the next. */
        public float getKerning(char second) {
            if (kerning == null) {
                return 0;
            }
            Float amount = kerning.get(second);
            if (amount == null) {
                return 0;
            }
            else {
                return amount.floatValue();
            }
        }

        /**
         The advance plus the kerning to the next character.
         */
        public float getDistanceX(char nextChar) {
            return getDistanceX(nextChar, 0);
        }
        
        /**
         The advance plus the kerning to the next character.
         */
        public float getDistanceX(char nextChar, float tracking) {
            // TODO: If the character is a combo mark, don't use tracking?
            return getAdvanceX() + getKerning(nextChar) + tracking;
        }

        @Override
        public int compareTo(Glyph o) {
            return ch - o.ch;
        }
    }

    private int ascent;
    private int descent;
    private List<Glyph> glyphs;
    private boolean hasLowercaseChars;
    private Glyph replacementGlyph;

    /**
     Creates a font from a collection of Glyphs. The collection is copied to a new List.
     */
    public ImageFont(Collection<Glyph> glyphs) {
        // Sort glyphs for later searching
        this.glyphs = new ArrayList<Glyph>(glyphs);
        Collections.sort(this.glyphs);

        // Setup replacement character
        replacementGlyph = getGlyphInternal('\uFFFD');
        if (replacementGlyph == null) {
            replacementGlyph = this.glyphs.get(this.glyphs.size() - 1);
        }
        
        // Get ascent/decent and flag if there are lowercase chars
        hasLowercaseChars = false;
        ascent = Integer.MAX_VALUE;
        descent = Integer.MIN_VALUE;
        for (Glyph glyph : glyphs) {
            Texture image = glyph.getTexture();
            int offsetY = glyph.getOffsetY();
            int y1 = offsetY;
            int y2 = offsetY + image.getHeight();
            ascent = Math.min(ascent, y1);
            descent = Math.max(descent, y2);
            if (!hasLowercaseChars && Character.isLowerCase(glyph.ch)) {
                hasLowercaseChars = true;
            }
        }
        ascent = -ascent;
    }

    /**
     Gets the height of this font above the baseline (positive).
     */
    public int getAscent() {
        return ascent;
    }

    /**
     Gets the height of this font below the baseline (positive).
     */
    public int getDescent() {
        return descent;
    }

    /**
     Gets the total height of this font, or the sum of the ascent and the descent.
     */
    public int getHeight() {
        return ascent + descent;
    }

    /**
     Sets the kerning for the specified character pair. If the first or second character is
     not displayable in this font, this method does nothing.
     */
    public void setKerning(char first, char second, float kerning) {
        Glyph glyph = getGlyphInternal(first);
        if (glyph != null) {
            glyph.addKerning(second, kerning);
        }
    }
    
    /**
     Gets the Glyph for the specific character. If a Glyph is not available, the replacement
     Glyph is used.
     */
    public Glyph getGlyph(char ch) {
        Glyph glyphInfo = getGlyphInternal(ch);
        if (glyphInfo == null) {
            return replacementGlyph;
        }
        return glyphInfo;
    }

    /**
     Returns true if this font can display the specified character.
     */
    public boolean hasGlyph(char ch) {
        return getGlyphInternal(ch) != null;
    }

    private Glyph getGlyphInternal(char ch) {
        if (!hasLowercaseChars && Character.isLowerCase(ch)) {
            ch = Character.toUpperCase(ch);
        }

        // Binary search on sorted glyphs list
        int min = 0;
        int max = glyphs.size() - 1;
        while (min <= max) {
            int mid = min + (max - min) / 2;
            Glyph midGlyph = glyphs.get(mid);
            if (ch > midGlyph.ch) {
                min = mid + 1;
            }
            else if (ch < midGlyph.ch) {
                max = mid - 1;
            }
            else {
                return midGlyph;
            }
        }
        return null;
    }
}

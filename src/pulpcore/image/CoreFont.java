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

package pulpcore.image;

import java.io.IOException;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.util.ByteArray;

/**
    The CoreFont class is a image-based font used for drawing text.
*/
public class CoreFont {
    
    private static final long MAGIC = 0x70756c70666e740bL; // "pulpfnt" 0.11
    
    private static CoreFont systemFont;
    
    private CoreImage image;
    
    protected char firstChar;
    protected char lastChar;
    protected int[] charPositions;
    private boolean uppercaseOnly;
    /** horizontal pixel space between chars */
    private int tracking;
    private int[] kerningLeft;
    private int[] kerningRight;
    
    
    public static CoreFont getSystemFont() {
        if (systemFont == null) {
            systemFont = load("system.font.png");
        }
        
        return systemFont;
    }
    
    
    public static CoreFont load(String fontAsset) {
        CoreFont font = new CoreFont();
        
        CoreImage.load(fontAsset, font);
        
        if (font.image != null) {
            return font;
        }
        else if ("system.font.png".equals(fontAsset)) {
            if (Build.DEBUG) CoreSystem.print("Can't load system.font.png. Quitting");
            throw new Error();
        }
        else {
            return getSystemFont();
        }
    }
    
    
    private CoreFont() { }
     
    
    public CoreFont(CoreFont font) {
        this.image = font.image;
        this.firstChar = font.firstChar;
        this.lastChar = font.lastChar;
        this.charPositions = font.charPositions;
        this.kerningLeft = font.kerningLeft;
        this.kerningRight = font.kerningRight;
        this.uppercaseOnly = font.uppercaseOnly;
        this.tracking = font.tracking;
    }
    
    // Callback from CoreImage.load()
    void set(CoreImage image, ByteArray in) throws IOException {
        
        long magic = in.readLong();
            
        if (magic != MAGIC) {
            if (Build.DEBUG) {
                CoreSystem.print("CoreFont.set() - Bad magic number (try recompiling game assets)");
            }
            throw new IOException();
        }
        
        firstChar = (char)in.readShort();
        lastChar = (char)in.readShort();
        tracking = in.readShort();
        boolean hasKerning = in.readBoolean();
        
        int numChars = lastChar - firstChar + 1;
        charPositions = new int[numChars + 1];
        kerningLeft = new int[numChars];
        kerningRight = new int[numChars];
        
        for (int i=0; i<charPositions.length; i++) {
            charPositions[i] = in.readShort() & 0xffff;
        }
        
        if (hasKerning) {
            for (int i = 0; i < kerningLeft.length; i++) {
                kerningLeft[i] = in.readShort();
                kerningRight[i] = in.readShort();
            }
        }
            
        uppercaseOnly = (lastChar < 'a');
        
        this.image = image;
    }
    
    
    public boolean canDisplay(char ch) {
        if (uppercaseOnly && ch >= 'a' && ch <= 'z') {
            ch += 'A' - 'a';
        }
        if (ch < firstChar || ch > lastChar) {
            return false;
        }
        
        int index = ch - firstChar;
        int charWidth = charPositions[index+1] - charPositions[index];
        return (charWidth > 0);
    }
    
    
    /**
        Gets the total width of all the characters in a string. Tracking between 
        characters is included.
        If a character isn't valid for this font, the last character in the set is used.
        <p>
        Equivalent to calling getStringWidth(s, 0, s.length())
    */
    public int getStringWidth(String s) {
        return getStringWidth(s, 0, s.length());
    }
    
    
    /**
        Gets the total width of a range of characters in a string. Tracking 
        and kerning between 
        characters is included.
        If a character isn't valid for this font, the last character in the set is used.
        @param beginIndex the beginning index, inclusive.
        @param endIndex the ending index, exclusive.
    */
    public int getStringWidth(String s, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return 0;
        }
        int stringWidth = 0;
        
        int lastIndex = -1;
        for (int i = beginIndex; i < endIndex; i++) {
            int index = getCharIndex(s.charAt(i));
            int charWidth = charPositions[index+1] - charPositions[index];
            
            if (lastIndex != -1) {
                stringWidth += getKerning(lastIndex, index);
            }
            stringWidth += charWidth;
            lastIndex = index;
        }
        return stringWidth;
    }
    
    
    public int getCharPosition(String s, int beginIndex, int charIndex) {
        if (charIndex <= beginIndex) {
            return 0;
        }
        return getStringWidth(s, beginIndex, charIndex + 1) - getCharWidth(s.charAt(charIndex));
    }
    
    
    protected int getCharIndex(char ch) {
        if (uppercaseOnly && ch >= 'a' && ch <= 'z') {
            ch += 'A' - 'a';
        }
        if (ch < firstChar || ch > lastChar) {
            ch = lastChar;
        }
        return ch - firstChar;
    }
    
    
    /**
        Gets the width of the specified character. Tracking and kerning is not included.
        If a character isn't valid for this font, the last character in the set is used.
    */
    public int getCharWidth(char ch) {
        int index = getCharIndex(ch);
        return (charPositions[index+1] - charPositions[index]);
    }
    
    
    public int getKerning(char left, char right) {
        return getKerning(getCharIndex(left), getCharIndex(right));
    }
    
    
    protected int getKerning(int leftIndex, int rightIndex) {
        // Future versions of this method might handle kerning pairs, like "WA" and "Yo"
        return kerningRight[leftIndex] + tracking + kerningLeft[rightIndex];
    }
    
    
    public int getHeight() {
        return image.getHeight();
    }
    
    
    public CoreImage getImage() {
        return image;
    }
    
    
    //
    //
    //
    
    
    /**
        Returns a new CoreFont with every pixel set to the specified color,
        without changing the alpha of each pixel. 
    */
    public CoreFont tint(int rgbColor) {
        CoreFont tintedFont = new CoreFont(this);
        tintedFont.image = image.tint(rgbColor);
        return tintedFont;
    }
    
    
    public CoreFont background(int rgbColor) {
        return background(rgbColor, false);
    }
    
    
    public CoreFont background(int argbColor, boolean hasAlpha) {
        CoreFont newFont = new CoreFont(this);
        newFont.image = image.background(argbColor, hasAlpha);
        return newFont;
    }
    
    
    public CoreFont fade(int alpha) {
        CoreFont fadedFont = new CoreFont(this);
        fadedFont.image = image.fade(alpha);
        return fadedFont;
    }
    
    
    /**
        Creates a scaled instance of this font.
    */
    public CoreFont scale(float scale) {
        
        int numChars = lastChar - firstChar + 1;
        
        // Determine new char positions
        int[] scaledCharPositions = new int[charPositions.length];
        int position = 0;
        for (int i = 0; i < numChars; i++) {
            scaledCharPositions[i] = position;
            int charWidth = charPositions[i+1] - charPositions[i];
            int scaledWidth = Math.round(charWidth * scale);
            position += scaledWidth;
        }
        scaledCharPositions[numChars] = position;
        
        // Scale each character image
        CoreImage scaledImage = new CoreImage(position, Math.round(getHeight() * scale), false);
        CoreGraphics g = scaledImage.createGraphics();
        g.setComposite(CoreGraphics.COMPOSITE_SRC);
        for (int i = 0; i < numChars; i++) {
            int oldWidth = charPositions[i+1] - charPositions[i];
            int newWidth = scaledCharPositions[i+1] - scaledCharPositions[i];
            g.drawScaledImage(image, scaledCharPositions[i], 0, newWidth, scaledImage.getHeight(),
                charPositions[i], 0, oldWidth, getHeight());
        }
        
        // Scale the kerning
        int[] scaledKerningLeft = new int[kerningLeft.length];
        int[] scaledKerningRight = new int[kerningRight.length];
        for (int i = 0; i < kerningLeft.length; i++) {
            scaledKerningLeft[i] = Math.round(kerningLeft[i] * scale);
            scaledKerningRight[i] = Math.round(kerningRight[i] * scale);
        }
        
        // Create the new font
        CoreFont scaledFont = new CoreFont(this);
        scaledFont.charPositions = scaledCharPositions;
        scaledFont.kerningLeft = scaledKerningLeft;
        scaledFont.kerningRight = scaledKerningRight;
        scaledFont.image = scaledImage;
        scaledFont.tracking = Math.round(tracking * scale);
        
        return scaledFont;
    }
}

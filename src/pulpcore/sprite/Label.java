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

package pulpcore.sprite;

import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.image.AnimatedImage;
import pulpcore.math.CoreMath;
import pulpcore.util.StringUtil;


/**
    The Label is a Sprite that displays text. The text can be formatted
    using printf-style parameters 
    @see pulpcore.util.StringUtil
*/
public class Label extends Sprite {
    
    private String formatText;
    private String displayText;
    private Object[] formatArgs;
    
    private int stringWidth;
    private CoreFont font;
    private boolean autoWidth;
    private boolean autoHeight;
    
    private int lastImageFrame;
    
    public final Int numDisplayChars = new Int(this);
    
    
    public Label(String text, int x, int y) {
        this(null, text, x, y, -1, -1);
    }
    
    
    public Label(String text, double x, double y) {
        this(null, text, x, y, -1, -1);
    }
    
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(String text, int x, int y, int w, int h) {
        this(null, text, x, y, w, h);
    }
    
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(String text, double x, double y, double w, double h) {
        this(null, text, x, y, w, h);
    }
    
    
    public Label(CoreFont font, String text, int x, int y) {
        this(font, text, x, y, -1, -1);
    }
    
    
    public Label(CoreFont font, String text, double x, double y) {
        this(font, text, x, y, -1, -1);
    }
    
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(CoreFont font, String text, int x, int y, int w, int h) {
        super(x, y, w, h);
        
        init(font, text, w < 0, h < 0);
    }
    
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(CoreFont font, String text, double x, double y, double w, double h) {
        super(x, y, w, h);
        
        init(font, text, w < 0, h < 0); 
    }
    
    
    private void init(CoreFont font, String text, boolean autoWidth, boolean autoHeight) {
        lastImageFrame = -1;
        
        if (font == null) {
            this.font = CoreFont.getSystemFont();
        }
        else {
            this.font = font;
        }
        
        this.autoWidth = autoWidth;
        this.autoHeight = autoHeight;
        
        setText(text);
    }
    
    
    public void propertyChange(Property p) {
        
        super.propertyChange(p);
        
        if (p == numDisplayChars || 
            (p == super.width && !autoWidth) || 
            (p == super.height && !autoHeight))
        {
            pack();
            return;
        }
        
        if (formatArgs != null) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (p == formatArgs[i]) {
                    format();
                    return;
                }
            }
        }
    }
    
    
    protected int getNaturalWidth() {
        int w = stringWidth;
        if (numDisplayChars.get() != displayText.length()) {
            w = font.getStringWidth(displayText, 0, numDisplayChars.get());
        }
        return CoreMath.toFixed(w);
    }
    
    
    protected int getNaturalHeight() {
        return CoreMath.toFixed(font.getHeight());
    }
    
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        numDisplayChars.update(elapsedTime);
        
        CoreImage fontImage = font.getImage();
        fontImage.update(elapsedTime);
        
        if (fontImage instanceof AnimatedImage) {
            int frame = ((AnimatedImage)fontImage).getFrame();
            if (frame != lastImageFrame) {
                setDirty(true);
            }
        }
    }
    
    
    private void pack() {
        
        if (autoWidth) {
            setDirty(true);
            if (autoHeight) {
                width.setAsFixed(getNaturalWidth());
            }
            else {
                width.setAsFixed(CoreMath.mulDiv(getNaturalWidth(), height.getAsFixed(), 
                    getNaturalHeight()));
            }
        }
        
        if (autoHeight) {
            setDirty(true);
            if (autoWidth) {
                height.setAsFixed(getNaturalHeight());
            }
            else {
                height.setAsFixed(CoreMath.mulDiv(getNaturalHeight(), width.getAsFixed(), 
                    getNaturalWidth()));
            }
        }
    }
    
    
    /**
        @return the formatted display text.
    */
    public String getText() {
        return displayText;
    }
    
    
    public void setText(String text) {
        this.formatText = text;
        format();
    }
    
    
    public void setFormatArg(Object arg) {
        setFormatArgs(new Object[] { arg });
    }
    
    
    public void setFormatArgs(Object[] args) {
        formatArgs = args;
        if (formatArgs != null) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (formatArgs[i] instanceof Property) {
                    ((Property)formatArgs[i]).setListener(this);
                }
                
            }
        }
        format();
    }
    
    
    private void format() {
        displayText = StringUtil.format(formatText, formatArgs);
        if (displayText == null) {
            displayText = "null";
        }
        stringWidth = font.getStringWidth(displayText);
        numDisplayChars.set(displayText.length());
        setDirty(true);
        pack();
    }
    
    
    protected void drawSprite(CoreGraphics g) {
        
        CoreImage fontImage = font.getImage();
        if (fontImage instanceof AnimatedImage) {
            lastImageFrame = ((AnimatedImage)fontImage).getFrame();
        }

        String currDisplayText = displayText;
        
        if (numDisplayChars.get() != displayText.length()) {
            currDisplayText = displayText.substring(0, numDisplayChars.get());
        }
        
        g.setFont(font);
        g.drawString(currDisplayText);
    }
    
}
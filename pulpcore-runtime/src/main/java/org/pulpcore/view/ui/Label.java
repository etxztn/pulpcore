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
package org.pulpcore.view.ui;

import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Texture;
import org.pulpcore.util.Objects;
import org.pulpcore.graphics.ImageFont;
import org.pulpcore.view.Group;
import org.pulpcore.view.View;
import org.pulpcore.view.property.FloatProperty;
import org.pulpcore.view.property.Property;
import org.pulpcore.view.property.PropertyListener;

/**
A text label. The text can be multi-line and/or word-wrapped. Each character is created as a 
separate view, so each character can be animated separately.
<p>
The Label manages its subviews - if you add additional subviews, errors may occur.
*/
public class Label extends Group {
    
    /*
    Implementation notes:
    1) First create all glyphs. Glyphs only change if font or text changes.
    2) Layout glyphs. Layout changes when the following changes:
      Alignment, LineBreakMode, width, tracking, maxLines.
    (So, animating tracking won't create a bunch of objects )
    */
    
    public enum LineBreakMode {
        /** No word wrapping or truncating is performed. */
        NONE,
        /** Lines are word-wrapped to the width of the Label. */
        WORD_WRAP,
        // TODO: Truncate
        //TRUNCATE_START,
        //TRUNCATE_MIDDLE,
        //TRUNCATE_END,
    }
    
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }
    
    /**
    The tracking (spacing) between characters. The default is zero.
    */
    public final FloatProperty tracking = new FloatProperty(0, new PropertyListener() {
        @Override
        public void onPropertyChange(Property property) {
            doLayout();
        }
    });
    
    private ImageFont font = ImageFont.getDefaultFont();
    private String text = "";
    private int maxLines = 0;
    private LineBreakMode lineBreakMode = LineBreakMode.NONE;
    private Alignment alignment = Alignment.LEFT;
    private boolean sizeToFit = true;
    
    public Label() {
        this("");
    }
    
    public Label(String text) {
        setText(text);
    }
    
    @Override
    public void setWidth(float width) {
        if (getWidth() != width) {
            super.setWidth(width);
            doLayout();
        }
    }
    
    public float getTracking() {
        return tracking.get();
    }
    
    public void setTracking(float tracking) {
        this.tracking.set(tracking);
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        if (this.alignment != alignment) {
            this.alignment = alignment;
            doLayout();
        }
    }

    public LineBreakMode getLineBreakMode() {
        return lineBreakMode;
    }

    public void setLineBreakMode(LineBreakMode lineBreakMode) {
        if (this.lineBreakMode != lineBreakMode) {
            this.lineBreakMode = lineBreakMode;
            doLayout();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (!Objects.equal(this.text, text)) {
            this.text = text;
            // Reuse Glpyhs if possible
            createGlyphs();
            doLayout();
        }
    }

    public ImageFont getFont() {
        return font;
    }

    public void setFont(ImageFont font) {
        if (this.font != font) {
            this.font = font;
            // New font - all glyphs are invalid, so don't reuse existing GlyphViews.
            removeAllSubviews();
            createGlyphs();
            doLayout();
        }
    }

    public int getMaxLines() {
        return maxLines;
    }

    /**
    Sets the maximum number of lines to display. Set to 0 for no limit. The default is 0.
    */
    public void setMaxLines(int maxLines) {
        if (this.maxLines != maxLines) {
            this.maxLines = maxLines;
            doLayout();
        }
    }
    
    /**
    Create the GlyphViews
    */
    private void createGlyphs() {
        if (Objects.isNullOrEmpty(text) || font == null) {
            removeAllSubviews();
        }
        else {
            // Reuse glyphs if possible.
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (i < size()) {
                    GlyphView view = (GlyphView)getSubview(i);
                    if (view.getOriginalChar() != ch) {
                        ImageFont.Glyph glyph = font.getGlyph(ch);
                        view.setGlyph(glyph, ch);
                    }
                }
                else {
                    ImageFont.Glyph glyph = font.getGlyph(ch);
                    GlyphView view = new GlyphView(glyph, ch);
                    addSubview(view);
                }
            }
            while (size() > text.length()) {
                getSubview(size() - 1).removeFromSuperview();
            }
        }
    }
    
    /**
    Performs the text layout on the existing GlyphViews.
    */
    private void doLayout() {
        if (size() == 0 || font == null) {
            return;
        }
        float maxWidth = 0;
        float lineHeight = font.getHeight();
        float t = tracking.get();
        float charX = 0;
        float charY = font.getAscent();
        int lineStartIndex = 0;
        int lastSpaceIndex = -1;
        int line = 0;
        float visibleLineWidth = 0;
        float visibleLineWidthBeforeLastSpace = 0;
        for (int i = 0; i < size(); i++) {
            boolean visibleLine = (maxLines == 0 || line < maxLines);
            GlyphView view = (GlyphView)getSubview(i);
            ImageFont.Glyph glyph = view.getGlyph();
            char ch = view.getOriginalChar();
            if (ch == '\n') {
                addLine(lineStartIndex, i + 1, visibleLineWidth, visibleLine, 0, charY);
                maxWidth = Math.max(maxWidth, visibleLineWidth);
                lineStartIndex = i + 1;
                lastSpaceIndex = -1;
                line++;
                visibleLineWidth = 0;
                visibleLineWidthBeforeLastSpace = 0;
                charX = 0;
                charY += lineHeight;
            }
            else {
                boolean wrapped = false;
                if (ch == ' ' || ch == '\t') {
                    lastSpaceIndex = i;
                    visibleLineWidthBeforeLastSpace = visibleLineWidth;
                }
                else {
                    float previousVisibleLineWidth = visibleLineWidth;
                    visibleLineWidth = charX + glyph.getAdvanceX();
                    if (lineBreakMode == LineBreakMode.WORD_WRAP && getWidth() != 0 && visibleLineWidth > getWidth()) {
                        float w;
                        int end;
                        if (lastSpaceIndex != -1) {
                            // wrap this word
                            end = lastSpaceIndex + 1;
                            w = visibleLineWidthBeforeLastSpace;
                        }
                        else if (lineStartIndex < i) {
                            // this word doesn't fit on the line... split it
                            end = i;
                            w = previousVisibleLineWidth;
                        }
                        else { // if (startIndex == i)
                            // this 1 character doesn't fit on the line... add it anyway
                            end = i + 1;
                            w = visibleLineWidth;
                        }
                        addLine(lineStartIndex, end, w, visibleLine, 0, charY);
                        maxWidth = Math.max(maxWidth, w);
                        lineStartIndex = end;
                        lastSpaceIndex = -1;
                        i = lineStartIndex - 1;
                        wrapped = true;
                        line++;
                        visibleLineWidth = 0;
                        visibleLineWidthBeforeLastSpace = 0;
                        charX = 0;
                        charY += lineHeight;
                    }
                }

                if (!wrapped && i < size() - 1) {
                    char nextChar = ((GlyphView)getSubview(i + 1)).getOriginalChar();
                    charX += glyph.getDistanceX(nextChar, t);
                }
            }
        }

        // Add remaining chars
        if (lineStartIndex < size()) {
            boolean visibleLine = (maxLines == 0 || line < maxLines);

            float w;
            char lastChar = ((GlyphView)getSubview(size() - 1)).getOriginalChar();
            if (lastChar == ' ' || lastChar == '\t') {
                w = visibleLineWidthBeforeLastSpace;
            }
            else {
                w = visibleLineWidth;
            }
            addLine(lineStartIndex, text.length(), w, visibleLine, 0, charY);
            maxWidth = Math.max(maxWidth, w);
            line++;
        }
        
        if (sizeToFit) {
            int visibleLines = maxLines == 0 ? line : maxLines;
            setHeight(visibleLines * lineHeight);
            if (lineBreakMode == LineBreakMode.NONE) {
                setWidth(maxWidth);
            }
        }
    }
    
    private void addLine(int start, int end, float visibleLineWidth, boolean visible, float x, float y) {
        float t = tracking.get();
        
        // Round offsets so that the label still appears on integer locations
        if (alignment == Alignment.CENTER) {
            x += Math.round((getWidth() - visibleLineWidth) / 2);
        }
        else if (alignment == Alignment.RIGHT) {
            x += Math.round(getWidth() - visibleLineWidth);
        }
        
        for (int i = start; i < end; i++) {
            GlyphView view = (GlyphView)getSubview(i);
            char ch = view.getOriginalChar();
            view.layout(x, y);
            view.setVisible(ch != '\n' && visible);
            
            if (i < end - 1) {
                ImageFont.Glyph glyph = view.getGlyph();
                char nextChar = text.charAt(i + 1);
                x += glyph.getDistanceX(nextChar, t);
            }
        }
    }
 
    /*
    TODO: Generic TextureView, like ImageView?
    */
    private static class GlyphView extends View {

        private ImageFont.Glyph glyph;
        private char originalChar;

        public GlyphView(ImageFont.Glyph glyph, char ch) {
            setGlyph(glyph, ch);
            setPixelLevelChecks(true);
        }
        
        public void layout(float x, float y) {
            setLocation(x + glyph.getOffsetX(), y + glyph.getOffsetY());
        }
        
        public char getOriginalChar() {
            return originalChar;
        }
        
        public ImageFont.Glyph getGlyph() {
            return glyph;
        }
        
        public void setGlyph(ImageFont.Glyph glyph, char ch) {
            this.glyph = glyph;
            this.originalChar = ch;
            Texture texture = glyph.getTexture();
            if (texture != null) {
                setSize(texture.getWidth(), texture.getHeight());
            }
            else {
                setSize(0, 0);
            }
            setTag(glyph);
        }

        @Override
        public boolean isOpaque() {
            Texture texture = glyph.getTexture();
            return (texture != null && texture.isOpaque()) || super.isOpaque();
        }
   
        @Override
        protected boolean isTransparent(int localX, int localY) {
            Texture texture = glyph.getTexture();
            return (texture == null || texture.isTransparent(localX, localY)) && super.isTransparent(localX, localY);
        }

        @Override
        protected void onRender(Graphics g) {
            Texture texture = glyph.getTexture();
            g.setEdgeClamp(Graphics.EDGE_CLAMP_NONE);
            if (texture == null || !texture.isOpaque()) {
                // Draw background fill
                super.onRender(g);
            }
            if (texture != null) {
                g.drawTexture(texture);
            }
        }
    }
}



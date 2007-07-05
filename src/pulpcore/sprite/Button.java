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

import pulpcore.animation.Bool;
import pulpcore.image.AnimatedImage;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.Rect;

/**
    The Button is a Sprite that behaves like a common UI push button. A
    Button has
    three visual states: normal, hover, and pressed. Call 
    {@link #isClicked()} to check if the user clicked the button.
*/
public class Button extends ImageSprite {
    
    // For labeled button creation. See createLabeledButton(). 
    private static final int MARGIN = 8;
    private static final int MIN_WIDTH = 72;

    public static final int NORMAL = 0;
    public static final int HOVER = 1;
    public static final int PRESSED = 2;
    private static final int PRESSED_BUT_OUTSIDE = 3;
    
    private static final int NUM_VISIBLE_STATES = 3;
    
    public final Bool enabled = new Bool(this, true);
    
    private final CoreImage[] images;
    private final boolean isToggleButton;
    
    private int state;
    
    private int[] keyBinding;
    
    private boolean isSelected;
    private boolean isClicked;
    
    private int cursor = Input.CURSOR_HAND;
    private int outsideCursor = Input.CURSOR_DEFAULT;
    
    
    /**
        @param images an array of three images: normal, hover, and pressed. 
    */
    public Button(CoreImage[] images, int x, int y) {
        this(images, x, y, false);
    }
    
    
    /**
        @param images an array of three images: normal, hover, and pressed.
        Use six image for toggle buttons (unselected and selected).
    */
    public Button(CoreImage[] images, int x, int y, boolean isToggleButton) {
        super(images[0], x, y);
        
        this.isToggleButton = isToggleButton;
        this.images = new CoreImage[images.length];
        init(images, isToggleButton);
    }
    
    
    /**
        @param images an array of three images: normal, hover, and pressed. 
    */
    public Button(CoreImage[] images, double x, double y) {
        this(images, x, y, false);
    }
    
    
    /**
        @param images an array of three images: normal, hover, and pressed.
        Use six image for toggle buttons (unselected and selected).
    */
    public Button(CoreImage[] images, double x, double y, boolean isToggleButton) {
        super(images[0], x, y);
        
        this.isToggleButton = isToggleButton;
        this.images = new CoreImage[images.length];
        init(images, isToggleButton);
    }
    
    
    private void init(CoreImage[] images, boolean isToggleButton) {
        if (images.length < (isToggleButton?6:3)) {
            throw new IllegalArgumentException("Not enough button images.");
        }
        
        System.arraycopy(images, 0, this.images, 0, images.length);
        
        outsideCursor = Input.getCursor();
    }
    
    
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }
    
    
    public int getCursor() {
        return cursor;
    }
  
    
    private void getOutsideCursor() {
        int systemCursor =  Input.getCursor();
        if (systemCursor != Input.CURSOR_HAND) {
            outsideCursor = systemCursor;
        }
    }
    
    
    public boolean isToggleButton() {
        return isToggleButton;
    }
    
    
    public int[] getKeyBinding() {
        return keyBinding;
    }
    
    
    public void clearKeyBinding() {
        keyBinding = null;
    }
    
    
    public void setKeyBinding(int keyCode) {
        setKeyBinding(new int[] { keyCode });
    }
    
    
    public void setKeyBinding(int[] keyCodes) {
        keyBinding = keyCodes;
    }
    
    
    public void setSelected(boolean isSelected) {
        
        if (this.isSelected != isSelected) {
            this.isSelected = isSelected;
            
            // update the frame 
            setState(state);
            setDirty(true);
        }
    }
    
    
    public boolean isSelected() {
        return isSelected;
    }
    

    public void setState(int state) {
        this.state = state;
        
        int frame;
        if (enabled.get() == false || state == PRESSED_BUT_OUTSIDE) {
            frame = NORMAL;
        }
        else {
            frame = state;
        }
        
        if (isToggleButton && isSelected) {
            frame += NUM_VISIBLE_STATES;
        }
        
        if (images != null) {
            super.setImage(images[frame]);
        }
    }
    
    
    public int getState() {
        return state;
    }
    
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        isClicked = isClickedImpl();
    }
    
    
    public boolean isClicked() {
        return isClicked;
    }
    
        
    private boolean isClickedImpl() {
        
        if (enabled.get() == false) {
            if (state != NORMAL) {
                setState(NORMAL);
                Input.setCursor(outsideCursor);
            }
            return false;
        }
        
        // Not visible (disabled button)
        if (!visible.get() || alpha.get() <= 0) {
            if (state != NORMAL) {
                setState(NORMAL);
                Input.setCursor(outsideCursor);
            }
            else {
                getOutsideCursor();
            }
            return false;
        }
        
        
        // Set the mouse cursor
        if (isMouseOver()) {
            Input.setCursor(cursor);
        }
        else if (state != NORMAL) {
            Input.setCursor(outsideCursor);
        }
        else {
            getOutsideCursor();
        }
        
        
        // Handle key input
        if (keyBinding != null) {
                
            if (Input.isPressed(keyBinding)) {
                setState(PRESSED);
                return false;
            }
            else if (Input.isDown(keyBinding) && state == PRESSED) {
                return false;
            }
            else if (Input.isReleased(keyBinding) && state == PRESSED) {
                
                if (isToggleButton) {
                    isSelected = !isSelected;
                }
                setState(NORMAL);
                Input.setCursor(outsideCursor);
                return true;
            }
        }
        
        
        // Handle mouse input
        if (state == PRESSED) {
            if (isMouseReleased()) {
                if (isToggleButton) {
                    isSelected = !isSelected;
                }
                setState(HOVER);
                return true;
            }
            else if (!isMouseDown()) {
                setState(PRESSED_BUT_OUTSIDE);
            }
        }
        else if (state == PRESSED_BUT_OUTSIDE) {
            if (isMouseDown()) {
                setState(PRESSED);
            }
            else if (!Input.isMouseDown()) {
                setState(NORMAL);
            }
        }
        else if (state == HOVER) {
            if (isMousePressed()) {
                setState(PRESSED);
            }
            else if (!isMouseHover()) {
                setState(NORMAL);
            }
        }
        else {
            if (isMousePressed()) {
                setState(PRESSED);
            }
            else if (isMouseHover()) {
                setState(HOVER);
            }
        }
        
        return false;
    }
    
    
    //
    // Convenience methods to create buttons with text labels.
    //
    
    public static Button createLabeledButton(String text, int x, int y) {
        return createLabeledButton(null, null, text, x, y);
    }
    
    
    public static Button createLabeledButton(CoreImage[] images, CoreFont font, 
        String text, int x, int y)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
            
        int textX;
        int textY;
        if (images != null) {
            textX = images[0].getWidth() / 2;
            textY = images[0].getHeight() / 2;
        }
        else {
            int textWidth = font.getStringWidth(text);
            int textHeight = font.getHeight();
            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;
            
            textX = buttonWidth / 2;
            textY = buttonHeight / 2;
        }
        
        return createLabeledButton(images, font, text, x, y, 
            textX, textY,
            Sprite.HCENTER | Sprite.VCENTER, false, true);
    }
    
    
    public static Button createLabeledButton(CoreImage[] images, CoreFont font, 
        String text, int x, int y, int textX, int textY, int textAnchor, boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y, 
            textX, textY, textAnchor, false, offsetPressedText);
    }
    
    
    public static Button createLabeledToggleButton(CoreImage[] images, CoreFont font, 
        String text, int x, int y)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
            
        int textX;
        int textY;
        if (images != null) {
            textX = images[0].getWidth() / 2;
            textY = images[0].getHeight() / 2;
        }
        else {
            int textWidth = font.getStringWidth(text);
            int textHeight = font.getHeight();
            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;
            
            textX = buttonWidth / 2;
            textY = buttonHeight / 2;
        }
        
        return createLabeledButton(images, font, text, x, y, 
            textX, textY,
            Sprite.HCENTER | Sprite.VCENTER, true, true);
    }
    
    
    public static Button createLabeledToggleButton(CoreImage[] images, CoreFont font, 
        String text, int x, int y, int textX, int textY, int textAnchor, boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y, 
            textX, textY, textAnchor, true, offsetPressedText);
    }
    
    
    /**
        @param images the images to use. If null, simple gray images are created to fit the text
        @param font the font to use for rendering the text label. If null, the system font is used.
        @param offsetPressedText Set to true to offset the button's text when the button is pressed.
    */
    public static Button createLabeledButton(CoreImage[] images, CoreFont font, 
        String text, int x, int y, int textX, int textY, int textAnchor,
        boolean isToggleButton, boolean offsetPressedText)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        
        // Determine text label location
        int textWidth = font.getStringWidth(text);
        int textHeight = font.getHeight();
        if ((textAnchor & HCENTER) != 0) {
            textX -= textWidth / 2;
        }
        else if ((textAnchor & RIGHT) != 0) {
            textX -= textWidth;
        }
        if ((textAnchor & VCENTER) != 0) {
            textY -= textHeight / 2;
        }
        else if ((textAnchor & BOTTOM) != 0) {
            textY -= textHeight;
        }        
        
        // Create button image, if needed
        if (images == null) {
            images = new CoreImage[isToggleButton?6:3];
            
            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;
        
            // Normal
            images[0] = new CoreImage(buttonWidth, buttonHeight);
            CoreGraphics g = images[0].createGraphics();
            g.setColor(CoreGraphics.WHITE);
            g.fill();
            g.setColor(CoreGraphics.LIGHTGRAY);
            g.drawRect(0, 0, buttonWidth, buttonHeight);
            
            // Hover
            images[1] = new CoreImage(buttonWidth, buttonHeight);
            g = images[1].createGraphics();
            g.setColor(CoreGraphics.WHITE);
            g.fill();
            g.setColor(CoreGraphics.DARKGRAY);
            g.drawRect(0, 0, buttonWidth, buttonHeight);
            g.setColor(CoreGraphics.LIGHTGRAY);
            g.drawRect(1, 1, buttonWidth-2, buttonHeight-2);
            
            // Pressed
            images[2] = new CoreImage(buttonWidth, buttonHeight);
            g = images[2].createGraphics();
            g.setColor(CoreGraphics.LIGHTGRAY);
            g.fill();
            g.setColor(CoreGraphics.DARKGRAY);
            g.drawRect(0, 0, buttonWidth, buttonHeight);
            
            if (isToggleButton) {
                images[4] = images[0];
                images[5] = images[1];
                images[6] = images[2];
            }
        }
        
        // Determine bounds of the image after the text is added
        Rect bounds = new Rect(0, 0, images[0].getWidth(), images[0].getHeight());
        bounds.union(textX, textY, textWidth + 1, textHeight + 1);
        
        // Create new images
        CoreImage[] textImages = new CoreImage[images.length];
        for (int i = 0; i < textImages.length; i++) {

            int offsetX = 0;
            int offsetY = 0;
            if (offsetPressedText && (i % NUM_VISIBLE_STATES) == PRESSED) {
                offsetX = 1;
                offsetY = 1;
            }
            
            textImages[i] = new CoreImage(bounds.width, bounds.height, false);
            
            CoreGraphics g = textImages[i].createGraphics();
            g.setFont(font);
            
            // Draw the text outside the button
            g.setComposite(CoreGraphics.COMPOSITE_SRC);
            g.drawString(text, textX-bounds.x + offsetX, textY-bounds.y + offsetY);
            
            // Draw the button image
            g.setClip(-bounds.x, -bounds.y, images[i].getWidth(), images[i].getHeight());
            g.drawImage(images[i], -bounds.x, -bounds.y);
            
            // Draw the text inside the button
            g.setComposite(CoreGraphics.COMPOSITE_SRC_OVER);
            g.drawString(text, textX-bounds.x + offsetX, textY-bounds.y + offsetY);
        }
    
        return new Button(textImages, x, y, isToggleButton);
    }
    
}
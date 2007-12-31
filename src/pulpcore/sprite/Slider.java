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
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.image.AnimatedImage;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.CoreMath;

/**
    A Slider is a widget that lets the user select a value by sliding a thumb. 
*/
public class Slider extends Sprite {
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    
    private static final int DRAG_NONE = 0;
    private static final int DRAG_THUMB = 1;
    private static final int DRAG_GUTTER = 2;
    
    /**
        The flag indicating whether this Slider is enabled. An enabled Slider 
        responds to user input. Sliders are enabled by default.
    */
    public final Bool enabled = new Bool(this, true);
    
    /** 
        The value of this Slider, initially set to 50.
    */
    public final Int value = new Int(this, 50);
    
    private CoreImage backgroundImage;
    private CoreImage thumbImage;
    private int orientation;
    private int top, left, bottom, right;
    private int min, max;
    private int pageAmount;
    
    private int lastFrameBackgroundImage;
    private int lastFrameThumbImage;
    
    private int thumbX, thumbY;
    private int dragMode;
    private int dragOffset;
    
    /**
        Creates a Slider with a background image and a thumb image.
    */
    public Slider(String backgroundImage, String thumbImage, int x, int y) {
        this(CoreImage.load(backgroundImage), CoreImage.load(thumbImage), x, y);
    }
    
    /**
        Creates a Slider with a background image and a thumb image.
    */
    public Slider(CoreImage backgroundImage, CoreImage thumbImage, int x, int y) {
        super(x, y, backgroundImage.getWidth(), backgroundImage.getHeight());
        this.backgroundImage = backgroundImage;
        this.thumbImage = thumbImage;
        this.min = 0;
        this.max = 100;
    }
    
    public void propertyChange(Property property) {
        super.propertyChange(property);
        if (property == value) {
            positionThumb();
        }
    }
    
    private void checkDirtyImage() {
        if (backgroundImage instanceof AnimatedImage) {
            int frame = ((AnimatedImage)backgroundImage).getFrame();
            if (frame != lastFrameBackgroundImage) {
                setDirty(true);
                lastFrameBackgroundImage = frame;
            }
        }
        
        if (thumbImage instanceof AnimatedImage) {
            int frame = ((AnimatedImage)thumbImage).getFrame();
            if (frame != lastFrameThumbImage) {
                setDirty(true);
                lastFrameThumbImage = frame;
            }
        }
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        value.update(elapsedTime);
        enabled.update(elapsedTime);
        
        backgroundImage.update(elapsedTime);
        thumbImage.update(elapsedTime);
        checkDirtyImage();
        
        if (enabled.get()) {
            if (dragMode == DRAG_THUMB) {
                if (Input.isMouseReleased()) {
                    dragMode = DRAG_NONE;
                }
                else {
                    int offset;
                    if (isHorizontal()) {
                        offset = getLocalX(Input.getMouseX(), Input.getMouseY());
                    }
                    else {
                        offset = getLocalY(Input.getMouseX(), Input.getMouseY());
                    }
                    value.set(getValue(offset - dragOffset));
                }
            }
            else if (dragMode == DRAG_GUTTER) {
                if (Input.isMouseReleased()) {
                    dragMode = DRAG_NONE;
                }
                else {
                    int x = getLocalX(Input.getMouseX(), Input.getMouseY());
                    int y = getLocalY(Input.getMouseX(), Input.getMouseY());
                    if (isHorizontal()) {
                        if (x < thumbX) {
                            // Scroll left
                            page(-pageAmount, x - thumbImage.getWidth()/2);
                        }
                        else if (x >= thumbX + thumbImage.getWidth()) {
                            // Scroll right
                            page(pageAmount, x - thumbImage.getWidth()/2);
                        }
                    }
                    else {
                        // Vertical
                        if (y < thumbY) {
                            // Scroll left
                            page(-pageAmount, y - thumbImage.getWidth()/2);
                        }
                        else if (y >= thumbY + thumbImage.getHeight()) {
                            // Scroll right
                            page(pageAmount, y - thumbImage.getWidth()/2);
                        }
                    }
                }
            }
            else if (isMousePressed()) {
                int x = getLocalX(Input.getMousePressX(), Input.getMousePressY());
                int y = getLocalY(Input.getMousePressX(), Input.getMousePressY());
                if (isHorizontal()) {
                    if (x < thumbX) {
                        // Scroll left
                        page(-pageAmount, x - thumbImage.getWidth()/2);
                        dragMode = DRAG_GUTTER;
                    }
                    else if (x >= thumbX + thumbImage.getWidth()) {
                        // Scroll right
                        page(pageAmount, x - thumbImage.getWidth()/2);
                        dragMode = DRAG_GUTTER;
                    }
                    else if (y >= thumbY && y < thumbY + thumbImage.getHeight()) {
                        // Start dragging
                        dragOffset = x - thumbX;
                        dragMode = DRAG_THUMB;
                    }
                }
                else {
                    // Vertical
                    if (y < thumbY) {
                        // Scroll left
                        page(-pageAmount, y - thumbImage.getWidth()/2);
                        dragMode = DRAG_GUTTER;
                    }
                    else if (y >= thumbY + thumbImage.getHeight()) {
                        // Scroll right
                        page(pageAmount, y - thumbImage.getWidth()/2);
                        dragMode = DRAG_GUTTER;
                    }
                    else if (x >= thumbX && x < thumbX + thumbImage.getWidth()) {
                        // Start dragging
                        dragOffset = y - thumbY;
                        dragMode = DRAG_THUMB;
                    }
                }
            }
        }
        else {
            dragMode = DRAG_NONE;
        }
    }
    
    private int getValue(int offset) {
        int start;
        int space;
        if (isHorizontal()) {
            start = (left > 0 ? left : 0);
            space = getHorizontalSpace();
        }
        else {
            start = (top > 0 ? top : 0);
            space = getVerticalSpace();
        }
        
        int value = min + CoreMath.intDivCeil((offset - start) * (max - min), space);
        return CoreMath.clamp(value, min, max);
    }
    
    private void page(int scroll, int offset) {
        if (scroll != 0) {
            value.set(CoreMath.clamp(value.get() + scroll, min, max));
        }
        else {
            value.set(getValue(offset));
        }
    }
    
    private void positionThumb() {
        setDirty(true);
        int horizontalSpace = getHorizontalSpace();
        int verticalSpace =  getVerticalSpace();
        int thumbValue = CoreMath.clamp(value.get(), min, max);
        
        if (isHorizontal()) {
            thumbX = (left > 0 ? left : 0) + (thumbValue - min) * horizontalSpace / (max - min);
            thumbY = (top > 0 ? top : 0) + verticalSpace / 2;
        }
        else {
            thumbX = (left > 0 ? left : 0) + horizontalSpace / 2;
            thumbY = (top > 0 ? top : 0) + (thumbValue - min) * verticalSpace / (max - min);
        }
    }
    
    private int getHorizontalSpace() {
        return backgroundImage.getWidth() - left - right - thumbImage.getWidth();
    }
    
    private int getVerticalSpace() {
        return backgroundImage.getHeight() - top - bottom - thumbImage.getHeight();
    }
    
    protected int getNaturalWidth() {
        int w = backgroundImage.getWidth();
        if (left < 0) {
            w -= left;
        }
        if (right < 0) {
            w -= right;
        }
        return CoreMath.toFixed(w);
    }
    
    protected int getNaturalHeight() {
        int h = backgroundImage.getHeight();
        if (top < 0) {
            h -= top;
        }
        if (bottom < 0) {
            h -= bottom;
        }
        return CoreMath.toFixed(h);
    }
    
    /**
        Sets the thumb image.
    */
    public void setThumb(CoreImage thumbImage) {
        if (this.thumbImage != thumbImage) {
            this.thumbImage = thumbImage;
            positionThumb();
            if (thumbImage instanceof AnimatedImage) {
                lastFrameThumbImage = ((AnimatedImage)thumbImage).getFrame();
            }
        }
    }
    
    /**
        Sets the visual insets that the thumb image is bound to. 
        <p>
        If an inset is positive, it is used as inner boundry within the background image. If 
        an inset is negative, the the thumb can extend outisde the background image by that amount.
        <p>
        For horizontal sliders, the left and right insets are use as boundaries, 
        and the thumb is centered vertically between the top and bottom insets.
        <p>
        For vertical sliders, the top and bottom insets are use as boundaries, 
        and the thumb is centered horizontally between the left and right insets.
    */
    public void setInsets(int top, int left, int bottom, int right) {
        if (this.top != top || this.left != left || this.bottom != bottom || this.right != right) {
            int oldNaturalWidth = getNaturalWidth();
            int oldNaturalHeight = getNaturalHeight();
            this.top = top;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            if (width.getAsFixed() == oldNaturalWidth) {
                width.setAsFixed(getNaturalWidth());
            }
            if (height.getAsFixed() == oldNaturalHeight) {
                height.setAsFixed(getNaturalHeight());
            }
            positionThumb();
        }
    }
    
    protected void drawSprite(CoreGraphics g) {
        int bgX = (left < 0) ? -left : 0;
        int bgY = (top < 0) ? -top : 0;
        
        g.drawImage(backgroundImage, bgX, bgY);
        g.drawImage(thumbImage, thumbX, thumbY);
    }
    
    // 
    // Model
    //
    
    private boolean isHorizontal() {
        // Default is horizontal
        return (orientation != VERTICAL);
    }
    
    /**
        Sets the orientation of this Slider: either {@link #HORIZONTAL} or {@link #VERTICAL}.
        By default, the Slider is horizontal.
    */
    public void setOrientation(int orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            positionThumb();
        }
    }
    
    /**
        Sets the min and max range of the value. By default, the min is 0 and the max is 100.
    */
    public void setRange(int min, int max) {
        if (this.min != min || this.max != max) {
            this.min = min;
            this.max = max;
            value.set(CoreMath.clamp(value.get(), min, max));
            positionThumb();
        }
    }
    
    /**
        Sets the amount to page when clicking the gutter 
        (the background of the Slider outside the thumb).
        If zero, the value is scrolled to exactly the position clicked on. 
        By default, the page amount is zero.
    */
    public void setPageAmount(int pageAmount) {
        this.pageAmount = pageAmount;
    }
    
}

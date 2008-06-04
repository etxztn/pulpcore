/*
    Copyright (c) 2008, Interactive Pulp, LLC
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

import pulpcore.animation.BindFunction;
import pulpcore.animation.Bool;
import pulpcore.animation.Fixed;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.image.AnimatedImage;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.CoreMath;

/**
    A ScrollPane is a {@link ScrollArea} with scroll bars that appear as needed. 
    The scroll bars' appearance is limited (no arrow buttons, no dynamic thumb size) and may
    change in future versions.
*/
public class ScrollPane extends Group {
    
    public static final int SCROLL_BAR_WIDTH = 16;
    private static final int GUTTER_COLOR = Colors.gray(224);
    private static final int THUMB_COLOR = Colors.gray(128);
    
    /**
        The scroll x location. Identical to {@code getContentPane().x}.
    */
    public final Fixed scrollX;
    
    /**
        The scroll y location. Identical to {@code getContentPane().y}.
    */
    public final Fixed scrollY;
    
    /**
        The flag to indicate pixel snapping for scroll location. Initially set to {@code true}.
        Identical to {@code getContentPane().pixelSnapping}.
    */
    public final Bool scrollPixelSnapping;
        
    /*
        Use a slider as a scrollbar. Ideally a real scroll bar would look better (a thumb
        image that changes its dimensions based on the size of the extent, and up/down arrows) 
        but using a slider works for now.
    */
    private Slider verticalScrollBar;
    private Slider horizontalScrollBar;
    private final ScrollArea scrollArea;
    private boolean hasFocus;
    
    private int scrollUnitSize = 1;
    private int pageDuration = 0;
    private int unitDuration = 0;
    
    public ScrollPane(int x, int y, int w, int h) {
        super(x, y, w, h);
        hasFocus = true;
        scrollArea = new ScrollArea(0, 0, w, h);
        scrollX = getContentPane().x;
        scrollY = getContentPane().y;
        scrollPixelSnapping = getContentPane().pixelSnapping;
        scrollPixelSnapping.set(true);
        super.add(scrollArea);
    }
    
    /**
        Returns {@code true} if this ScrollPane automatically reacts to keyboard events for 
        scrolling.
    */
    public boolean hasFocus() {
        return hasFocus;
    }
    
    /**
        Set whether this ScrollPane automatically reacts to keyboard events for scrolling.
    */
    public void setFocus(boolean hasFocus) {
        this.hasFocus = hasFocus;
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        updateScrollBars();
        if (hasFocus) {
            checkInput();
        }
    }
        
    private void updateScrollBars() {
        
        int w = hMax();
        int h = vMax();
        int innerWidth = hExtent();
        int innerHeight = vExtent();
        
        // Create or remove vertical scroll bar
        if (h > innerHeight) {
            if (verticalScrollBar == null || 
                verticalScrollBar.height.getAsInt() != innerHeight)
            {
                if (verticalScrollBar != null) {
                    super.remove(verticalScrollBar);
                }
                verticalScrollBar = createVerticalScrollBar(innerHeight);
                verticalScrollBar.x.bindTo(width);
                verticalScrollBar.y.set(0);
                verticalScrollBar.height.set(innerHeight);
                verticalScrollBar.setAnchor(Sprite.NORTH_EAST);
                verticalScrollBar.setRange(0, h, innerHeight);
                verticalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
                
                verticalScrollBar.value.bindTo(new NegativeFunction(scrollArea.scrollY));
                scrollArea.scrollY.bindTo(new NegativeFunction(verticalScrollBar.value));
                super.add(verticalScrollBar);
            }
            verticalScrollBar.setRange(0, h, innerHeight);
        }
        else if (verticalScrollBar != null) {
            super.remove(verticalScrollBar);
            verticalScrollBar = null;
        }
        
        // Create or remove hortizontal scroll bar
        if (w > innerWidth) {
            if (horizontalScrollBar == null || 
                horizontalScrollBar.width.getAsInt() != innerWidth)
            {
                if (horizontalScrollBar != null) {
                    super.remove(horizontalScrollBar);
                }
                horizontalScrollBar = createHorizontalScrollBar(innerWidth);
                horizontalScrollBar.x.set(0);
                horizontalScrollBar.y.bindTo(height);
                horizontalScrollBar.width.set(innerWidth);
                horizontalScrollBar.setAnchor(Sprite.SOUTH_WEST);
                horizontalScrollBar.setRange(0, w, innerWidth);
                horizontalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
                
                horizontalScrollBar.value.bindTo(new NegativeFunction(scrollArea.scrollX));
                scrollArea.scrollX.bindTo(new NegativeFunction(horizontalScrollBar.value));
                super.add(horizontalScrollBar);
            }
            horizontalScrollBar.setRange(0, w, innerWidth);
        }
        else if (horizontalScrollBar != null) {
            super.remove(horizontalScrollBar);
            horizontalScrollBar = null;
        }
        
        // Set scroll area dimensions
        scrollArea.width.set(hExtent());
        scrollArea.height.set(vExtent());
    }
    
    private void checkInput() {
        if (Input.isPressed(Input.KEY_HOME)) {
            scrollHome();
        }
        if (Input.isPressed(Input.KEY_END)) {
            scrollEnd();
        }
        if (Input.isTyped(Input.KEY_UP)) {
            scrollUp();
        }
        if (Input.isTyped(Input.KEY_DOWN)) {
            scrollDown();
        }
        if (Input.isTyped(Input.KEY_LEFT)) {
            scrollLeft();
        }
        if (Input.isTyped(Input.KEY_RIGHT)) {
            scrollRight();
        }
        if (Input.isTyped(Input.KEY_PAGE_UP)) {
            scrollPageUp();
        }
        if (Input.isTyped(Input.KEY_PAGE_DOWN)) {
            scrollPageDown();
        }
        if (isMouseWheelRotated()) {
            scrollVertical(Input.getMouseWheelRotation() * 3);
        }
    }
    
    private static class NegativeFunction implements BindFunction {
        
        private final Fixed v;
        
        public NegativeFunction(Fixed v) {
            this.v = v;
        }
        
        public Number f() {
            return new Double(-v.get());
        }
    }
    
    /**
        Creates a vertical scroll bar with the default appearance. 
        <p>Subclasses may override this method to provide a custom look and feel. 
        The location, range, and value of the Slider is set automatically.
    */
    protected Slider createVerticalScrollBar(int height) {
        int w = SCROLL_BAR_WIDTH;
        int h = height;
        Slider slider = new Slider(
            createVerticalBarImage(GUTTER_COLOR, h),
            createVerticalBarImage(THUMB_COLOR, Math.max(w, h/5)), 0, 0);
        slider.setOrientation(Slider.VERTICAL);
        return slider;
    }
    
    /**
        Creates a horizontal scroll bar with the default appearance. 
        <p>Subclasses may override this method to provide a custom look and feel. 
        The location, range, and value of the Slider is set automatically.
    */
    protected Slider createHorizontalScrollBar(int width) {
        int w = width;
        int h = SCROLL_BAR_WIDTH;
        Slider slider = new Slider(
            createHorizontalBarImage(GUTTER_COLOR, width),
            createHorizontalBarImage(THUMB_COLOR, Math.max(h, w/5)), 0, 0);
        slider.setOrientation(Slider.HORIZONTAL);
        return slider;
    }
    
    private CoreImage createVerticalBarImage(int color, int height) {
        AnimatedImage glpyhs = (AnimatedImage)(CoreImage.load("glyphs.png").tint(color));
        
        CoreImage bar = new CoreImage(16, height, false);
        CoreGraphics g = bar.createGraphics();
        g.drawImage(glpyhs.getImage(4).crop(0, 0, 16, 8), 0, 0);
        g.drawImage(glpyhs.getImage(4).crop(0, 8, 16, 8), 0, height-8);
        g.setColor(color);
        g.fillRect(0, 8, 16, height-16);
        
        return bar;
    }
    
    private CoreImage createHorizontalBarImage(int color, int width) {
        AnimatedImage glpyhs = (AnimatedImage)(CoreImage.load("glyphs.png").tint(color));
        
        CoreImage bar = new CoreImage(width, 16, false);
        CoreGraphics g = bar.createGraphics();
        g.drawImage(glpyhs.getImage(4).crop(0, 0, 8, 16), 0, 0);
        g.drawImage(glpyhs.getImage(4).crop(8, 0, 8, 16), width-8, 0);
        g.setColor(color);
        g.fillRect(8, 0, width-16, 16);
        
        return bar;
    }
    
    //
    // Model
    //
    
    private int vMin() {
        return 0;
    }
    
    private int vMax() {
        return scrollArea.getContentHeight();
    }
    
    private int vExtent() {
        return height.getAsInt() - 
            (horizontalScrollBar == null ? 0 : horizontalScrollBar.height.getAsInt());
    }
    
    private double vClamp(double n) {
        return CoreMath.clamp(n, vMin(), vMax() - vExtent());
    }
    
    private int hMin() {
        return 0;
    }
    
    private int hMax() {
        return scrollArea.getContentWidth();
    }
    
    private int hExtent() {
        return width.getAsInt() - 
            (verticalScrollBar == null ? 0 : verticalScrollBar.width.getAsInt());
    }
    
    private double hClamp(double n) {
        return CoreMath.clamp(n, hMin(), hMax() - hExtent());
    }
    
    //
    // Scrolling
    //
    
    /**
        Sets the duration, in milliseconds, when scrolling. By default, both animation
        durations are set to zero.
    */
    public void setAnimationDuration(int unitDuration, int pageDuration) {
        this.unitDuration = unitDuration;
        this.pageDuration = pageDuration;
        if (verticalScrollBar != null) {
            verticalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
        }
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
        }
    }
    
    /**
        Set the amount to scroll when using the arrow keys. By default, the value is 1.
    */
    public void setScrollUnitSize(int scrollUnitSize) {
        this.scrollUnitSize = scrollUnitSize;
        if (verticalScrollBar != null) {
            verticalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
        }
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setAnimationDuration(unitDuration/scrollUnitSize, pageDuration);
        }
    }
    
    /**
        Gets the amount to scroll when using the arrow keys. By default, the value is 1.
    */
    public int getScrollUnitSize() {
        return scrollUnitSize;
    }
    
    /**
        Scrolls vertically by the specificed number of units, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollVertical(int units) {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scroll(units * scrollUnitSize);
        }
    }
    
    /**
        Scrolls horizontally by the specificed number of units, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollHorizontal(int units) {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scroll(units * scrollUnitSize);
        }
    }
    
    /**
        Scrolls to the top, if possible. No animation is performed.
    */
    public void scrollHome() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scrollHome();
        }
    }
    
    /**
        Scrolls to the bottom, if possible. No animation is performed.
    */
    public void scrollEnd() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scrollEnd();
        }
    }
    
    /**
        Scrolls to the left-most side, if possible. No animation is performed.
    */
    public void scrollLeftSide() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scrollHome();
        }
    }
    
    /**
        Scrolls to the right-most side, if possible. No animation is performed.
    */
    public void scrollRightSide() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scrollEnd();
        }
    }
    
    /**
        Scrolls up one unit, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollUp() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scroll(-scrollUnitSize);
        }
    }
    
    /**
        Scrolls down one unit, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollDown() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scroll(scrollUnitSize);
        }
    }
    
    /**
        Scrolls left one unit, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollLeft() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scroll(-scrollUnitSize);
        }
    }
    
    /**
        Scrolls right one unit, if possible.
        @see #setScrollUnitSize(int)
        @see #setAnimationDuration(int, int)
    */
    public void scrollRight() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scroll(scrollUnitSize);
        }
    }
    
    /**
        Scrolls up one page, if possible.
        @see #setAnimationDuration(int, int)
    */
    public void scrollPageUp() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scrollPageUp();
        }
    }
    
    /**
        Scrolls down one page, if possible.
        @see #setAnimationDuration(int, int)
    */
    public void scrollPageDown() {
        updateScrollBars();
        if (verticalScrollBar != null) {
            verticalScrollBar.scrollPageDown();
        }
    }
    
    /**
        Scrolls left one page, if possible.
        @see #setAnimationDuration(int, int)
    */
    public void scrollPageLeft() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scrollPageUp();
        }
    }
    
    /**
        Scrolls right one page, if possible.
        @see #setAnimationDuration(int, int)
    */
    public void scrollPageRight() {
        updateScrollBars();
        if (horizontalScrollBar != null) {
            horizontalScrollBar.scrollPageDown();
        }
    }
    
    //
    // ScrollPane
    //
    
    public Group getContentPane() {
        return scrollArea.getContentPane();
    }
    
    public int getContentWidth() {
        return scrollArea.getContentWidth();
    }
    
    public int getContentHeight() {
        return scrollArea.getContentHeight();
    }
    
    /**
        Calls {@code add(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void add(Sprite sprite) {
        scrollArea.add(sprite);
    }
    
    /**
        Calls {@code remove(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void remove(Sprite sprite) {
        scrollArea.remove(sprite);
    }
    
    /**
        Calls {@code removeAll()} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void removeAll() {
        scrollArea.removeAll();
    }
    
    /**
        Calls {@code moveToTop(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void moveToTop(Sprite sprite) {
        scrollArea.moveToTop(sprite);
    }
    
    /**
        Calls {@code moveToBottom(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void moveToBottom(Sprite sprite) {
        scrollArea.moveToBottom(sprite);
    }
    
    /**
        Calls {@code moveUp(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void moveUp(Sprite sprite) {
        scrollArea.moveUp(sprite);
    }
    
    /**
        Calls {@code moveDown(sprite)} on the internal {@link ScrollArea}.
        <p>
        {@inheritDoc}
    */
    public void moveDown(Sprite sprite) {
        scrollArea.moveDown(sprite);
    }
}
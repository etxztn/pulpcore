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
import pulpcore.animation.Easing;
import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.Build;
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;


/**
    The superclass of all sprites. Contains location, dimension, alpha, 
    angle, visibility, and anchor information. 
    The Sprite does no drawing - subclasses implement the 
    {@link #drawSprite(CoreGraphics)}
    method to draw.
*/
public abstract class Sprite implements PropertyListener {

    //
    // Text anchors
    //
    
    /** 
        Constant for positioning the anchor point at the "default" location
        of the Sprite, which is usually its upper-left corner. One exception is
        ImageSprite which uses the image's hotspot at the default anchor. 
        This is the default anchor.
    */
    public static final int DEFAULT = 0;
    
    /** Constant for positioning the anchor point on the left side of the sprite. */
    public static final int LEFT = 1;
    
    /** Constant for positioning the anchor point on the right side of the sprite. */
    public static final int RIGHT = 2;
    
    /** Constant for positioning the anchor point in the horizontal center of the sprite. */
    public static final int HCENTER = 4;
    
    /** Constant for positioning the anchor point on the upper side of the sprite. */
    public static final int TOP = 8;
    
    /** Constant for positioning the anchor point on the lower side of the sprite. */
    public static final int BOTTOM = 16;
    
    /** Constant for positioning the anchor point in the vertical center of the sprite. */
    public static final int VCENTER = 32;
    
    /** 
        Constant for positioning the anchor point in the upper center of the sprite.
        Equivalent to TOP | HCENTER.
    */
    public static final int NORTH = TOP | HCENTER;
    
    /** 
        Constant for positioning the anchor point in the lower center of the sprite.
        Equivalent to BOTTOM | HCENTER.
    */
    public static final int SOUTH = BOTTOM | HCENTER;
    
    /** 
        Constant for positioning the anchor point in the left center of the sprite.
        Equivalent to LEFT | VCENTER.
    */
    public static final int WEST = LEFT | VCENTER;
    
    /** 
        Constant for positioning the anchor point in the right center of the sprite.
        Equivalent to RIGHT | VCENTER.
    */
    public static final int EAST = RIGHT | VCENTER;
    
    /** 
        Constant for positioning the anchor point in the upper left corner of the sprite.
        Equivalent to TOP | LEFT.
    */
    public static final int NORTH_WEST = TOP | LEFT;
    
    /** 
        Constant for positioning the anchor point in the upper right corner of the sprite.
        Equivalent to TOP | RIGHT.
    */
    public static final int NORTH_EAST = TOP | RIGHT;
    
    /** 
        Constant for positioning the anchor point in the lower left corner of the sprite.
        Equivalent to BOTTOM | LEFT.
    */
    public static final int SOUTH_WEST = BOTTOM | LEFT;
    
    /** 
        Constant for positioning the anchor point in the lower right corner of the sprite.
        Equivalent to BOTTOM | RIGHT.
    */
    public static final int SOUTH_EAST = BOTTOM | RIGHT;
    
    /** 
        Constant for positioning the anchor point in the center of the sprite.
        Equivalent to VCENTER | HCENTER.
    */
    public static final int CENTER = VCENTER | HCENTER;
    
    //
    // Properties
    //
    
    /** The x location of this Sprite. */
    public final Fixed x = new Fixed(this);
    
    /** The y location of this Sprite. */
    public final Fixed y = new Fixed(this);
    
    /** The width of this Sprite. */
    public final Fixed width = new Fixed(this);
    
    /** The height of this Sprite. */
    public final Fixed height = new Fixed(this);
    
    /** 
        The angle of this Sprite, typically in range from 0 to 
        {@link pulpcore.math.CoreMath#TWO_PI}, although the angle can
        have any value. The Sprite is rotated around its anchor.
    */
    public final Fixed angle = new Fixed(this);

    /** 
        The alpha of this Sprite, in range from 0 to 255. A value of 0 is fully 
        tranaparent and a value of 255 is fully opaque. The default is 255.
    */
    public final Int alpha = new Int(this, 0xff);
    
    /** 
        The flag indicating whether or not this Sprite is visible.
    */
    public final Bool visible = new Bool(this, true);
    
    /**
        Sets whether pixel snapping enabled for rendering this Sprite. 
        If this value is true, only the integer portion of the x and y
        properties are used to draw this sprite.
        <p>
        Enabling pixel snapping may allow some type of images (e.g. pixel art) to look better.
        <p>
        This value is false by default.
    */
    public final Bool pixelSnapping = new Bool(false);
    
    //
    // Private fields
    //
    
    private int anchor = DEFAULT;
    private int composite = -1;
    private boolean dirty = true;
    protected final Transform drawTransform = new Transform();
    
    /** The draw bounding box used for directy rectangles in Scene2D */
    private Rect dirtyRect;
    
    protected int cosAngle = CoreMath.ONE;
    protected int sinAngle = 0;
    
    
    public Sprite(int x, int y, int width, int height) {
        this.x.set(x);
        this.y.set(y);
        this.width.set(width);
        this.height.set(height);
    }
    
    
    public Sprite(double x, double y, double width, double height) {
        this.x.set(x);
        this.y.set(y);
        this.width.set(width);
        this.height.set(height);
    }
    
    
    /**
        On a property change this Sprite is marked as dirty.
    */
    public void propertyChange(Property property) {
        setDirty(true);
        
        if (property == angle) {
            cosAngle = CoreMath.cos(angle.getAsFixed());
            sinAngle = CoreMath.sin(angle.getAsFixed());
        }
    }
    
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public Rect getDirtyRect() {
        if (dirtyRect == null || dirtyRect.width <= 0) {
            return null;
        }
        else {
            return dirtyRect;
        }
    }
    
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public boolean calcDirtyRect() {
        
        boolean changed = false;
        
        if (visible.get() == false || alpha.get() <= 0) {
            changed = (dirtyRect != null && dirtyRect.width > 0);
            clearDirtyRect();
        }
        else {
            if (dirtyRect == null) {
                changed = true;
                dirtyRect = new Rect();
            }
            
            changed |= drawTransform.getBounds(
                getNaturalWidth(), getNaturalHeight(), dirtyRect);
        }
        
        return changed;
       
    }
    
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public void clearDirtyRect() {
        if (dirtyRect != null) {
            dirtyRect.width = -1;
        }
    }
    
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    
    /**
        Returns true if the Sprite's properties have changed since the last call to draw()
    */
    public boolean isDirty() {
        return dirty;
    }


    protected int getNaturalWidth() {
        return width.getAsFixed();
    }
    
    
    protected int getNaturalHeight() {
        return height.getAsFixed();
    }
    
    
    /**
        @return the fixed-point x anchor.
    */
    protected int getAnchorX() {
        if ((anchor & HCENTER) != 0) {
            int w = getNaturalWidth();
            // Special case: centered sprites with an integer width on an integer x location
            if (width.getAsFixed() == w && CoreMath.fracPart(w) == 0 && 
                CoreMath.fracPart(x.getAsFixed()) == 0) 
            {
                return CoreMath.floor(w / 2);
            }
            else {
                return w / 2;
            }
        }
        else if ((anchor & RIGHT) != 0) {
            return getNaturalWidth() - CoreMath.ONE;
        }
        else {
            return 0;
        }
    }
    
    
    /**
        @return the fixed-point y anchor.
    */
    protected int getAnchorY() {
        if ((anchor & VCENTER) != 0) {
            int h = getNaturalHeight();
            // Special case: centered sprites with an integer height on an integer y location
            if (height.getAsFixed() == h && CoreMath.fracPart(h) == 0 && 
                CoreMath.fracPart(y.getAsFixed()) == 0) 
            {
                return CoreMath.floor(h / 2);
            }
            else {
                return h / 2;
            }
        }
        else if ((anchor & BOTTOM) != 0) {
            return getNaturalHeight() - CoreMath.ONE;
        }
        else {
            return 0;
        }
    }
    
    
    /**
        Sets the absolute location of this Sprite.
    */
    public void setLocation(int x, int y) {
        this.x.set(x);
        this.y.set(y);
    }
    
    
    /**
        Sets the absolute location of this Sprite.
    */
    public void setLocation(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }    
    
    
    /**
        Translates the absolute location of this Sprite.
    */
    public void translate(int x, int y) {
        this.x.set(this.x.get() + x);
        this.y.set(this.y.get() + y);
    }
    
    
    /**
        Translates the absolute location of this Sprite.
    */
    public void translate(double x, double y) {
        this.x.set(this.x.get() + x);
        this.y.set(this.y.get() + y);
    }    
    
    
    /**
        Sets the size of this Sprite.
        Changing the size is non-destructive - for example, an ImageSprite 
        doesn't internally scale it's image when this method is called. 
        Instead, an ImageSprite uses appropriate
        CoreGraphics methods to draw a scaled version of its image.
    */
    public void setSize(int width, int height) {
        this.width.set(width);
        this.height.set(height);
    }    
    
    
    /**
        Sets the size of this Sprite.
        Changing the size is non-destructive - for example, an ImageSprite 
        doesn't internally scale it's image when this method is called. 
        Instead, an ImageSprite uses appropriate
        CoreGraphics methods to draw a scaled version of its image.
    */
    public void setSize(double width, double height) {
        this.width.set(width);
        this.height.set(height);
    }
    
    
    /**
        Sets the anchor of this Sprite. The anchor point is the combination of
        one of the horizontal anchor constants (LEFT, HCENTER, RIGHT) with
        one of the vertical anchor constants (TOP, VCENTER, BOTTOM). 
        <p>
        For example, to set the anchor to the sprite's lower right corner:
        <code>sprite.setAnchor(Sprite.BOTTOM | Sprite.RIGHT);</code>.
        <p>
        The anchor works with all Sprite subclasses.
    */
    public void setAnchor(int anchor) {
        if (this.anchor != anchor) {
            this.anchor = anchor;
            setDirty(true);
        }
    }
    
    
    public int getAnchor() {
        return anchor;
    }
    
    
    /**
        Sets the compositing method used to draw this Sprite. By default, the compositing 
        method is -1, which means the compositing method of this Sprite's parent is used. 
        Valid values are -1, CoreGraphics.COMPOSITE_SRC_OVER, CoreGraphics.COMPOSITE_SRC, and
        CoreGraphics.COMPOSITE_ADD.
    */
    public void setComposite(int composite) {
        if (this.composite != composite) {
            this.composite = composite;
            setDirty(true);
        }
    }
    
    
    public int getComposite() {
        return composite;
    }    
    
    
    /**
        Updates all of this Sprite's properties. Subclasses that override this method should
        call super.update().
    */
    public void update(int elapsedTime) {
        x.update(elapsedTime);
        y.update(elapsedTime);
        width.update(elapsedTime);
        height.update(elapsedTime);
        alpha.update(elapsedTime);
        angle.update(elapsedTime);
        visible.update(elapsedTime);
        pixelSnapping.update(elapsedTime);
    }
        
        
    public void prepareToDraw(Transform parentTransform, boolean parentDirty) {
        
        if (!parentDirty && !isDirty()) {
            return;
        }
        
        drawTransform.set(parentTransform);
        
        // Translate
        if (pixelSnapping.get()) {
            drawTransform.translate(CoreMath.floor(x.getAsFixed()), 
                CoreMath.floor(y.getAsFixed()));
        }
        else {
            drawTransform.translate(x.getAsFixed(), y.getAsFixed());
        }
        
        // Rotate
        if (cosAngle != CoreMath.ONE || sinAngle != 0) {
            drawTransform.rotate(cosAngle, sinAngle);
        }
        
        // Scale
        if (getNaturalWidth() != width.getAsFixed() || getNaturalHeight() != height.getAsFixed()) {
            int sx = CoreMath.div(width.getAsFixed(), getNaturalWidth());
            int sy = CoreMath.div(height.getAsFixed(), getNaturalHeight());
            drawTransform.scale(sx, sy);
        }
        
        // Adjust for anchor
        if (pixelSnapping.get()) {
            drawTransform.translate(CoreMath.floor(-getAnchorX()), CoreMath.floor(-getAnchorY()));
        }
        else {
            drawTransform.translate(-getAnchorX(), -getAnchorY());
        }
    }
    
    
    public final void draw(CoreGraphics g) {
        
        if (isDirty()) {
            prepareToDraw(null, false);
            setDirty(false);
        }
        
        if (!visible.get()) {
            return;
        }
        
        if (dirtyRect != null && dirtyRect.width > 0 &&
            !dirtyRect.intersects(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight())) 
        {
            return;
        }
        
        // Set alpha
        int newAlpha = alpha.get();
        int oldAlpha = g.getAlpha();
        if (oldAlpha != 255) {
            newAlpha = (newAlpha * oldAlpha) >> 8;
        }
        if (newAlpha <= 0) {
            return;
        }
        g.setAlpha(newAlpha);
        
        // Set composite
        int oldComposite = g.getComposite();
        if (composite != -1) {
            g.setComposite(composite);
        }
        
        // Set transform
        g.pushTransform();
        g.setTransform(drawTransform);
        
        // Draw
        drawSprite(g);
        
        // Undo changes
        g.popTransform();
        g.setAlpha(oldAlpha);
        g.setComposite(oldComposite);
    }
    
    
    /**
        Draws the sprite. The graphic context's alpha is set to this sprite,
        and it's translation is offset by this sprite's location.
        This method is not called if the sprite is not visible or it's alpha
        is less than or equal to zero.
    */
    protected abstract void drawSprite(CoreGraphics g);
    
    
    /**
        Gets the integer x-cordinate in Local Space of the specified location in
        View Space. Returns Integer.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public int getLocalX(int viewX, int viewY) {
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localX = drawTransform.inverseTransformX(fx, fy);
        if (localX == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return CoreMath.toInt(localX);
    }
    
    
    /**
        Gets the integer y-cordinate in Local Space of the specified location in
        View Space. Returns Integer.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public int getLocalY(int viewX, int viewY) {
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localY = drawTransform.inverseTransformY(fx, fy);
        if (localY == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return CoreMath.toInt(localY);
    }
    
    
    /**
        Checks if the specified point is within the bounds of this 
        Sprite. 
            
        @return true if the specified point is within the bounds of this 
        Sprite.
    */
    public boolean contains(int viewX, int viewY) {
        
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        
        int localX = drawTransform.inverseTransformX(fx, fy);
        int localY = drawTransform.inverseTransformY(fx, fy);
        
        if (localX == Integer.MAX_VALUE || localY == Integer.MAX_VALUE) {
            return false;
        }
        
        return (localX >= 0 && localX < getNaturalWidth() &&
                localY >= 0 && localY < getNaturalHeight());
    }
    
    
    /**
        Checks if the mouse is currently within the bounds of this Sprite.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite.
    */
    public boolean isMouseOver() {
        return contains(Input.getMouseX(), Input.getMouseY());
    }
    
    
    /**
        Checks if the mouse is within the bounds of this Sprite and the primary mouse
        button is not pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is not pressed down.
    */
    public boolean isMouseHover() {
        return !Input.isMouseDown() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    
    /**
        Checks if the mouse is currently within the bounds of this Sprite and the primary mouse
        button is pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is pressed down.
    */
    public boolean isMouseDown() {
        return Input.isMouseDown() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    
    /**
        Checks if the primary mouse button was pressed since the last update and the press 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was pressed since the last update and the press 
        occurred within this Sprite's bounds.
    */
    public boolean isMousePressed() {
        return Input.isMousePressed() && 
            contains(Input.getMousePressX(), Input.getMousePressY());
    }
    
    
    /**
        Checks if the primary mouse button was released since the last update and the release 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was released since the last update and the release 
        occurred within this Sprite's bounds.
    */
    public boolean isMouseReleased() {
        return Input.isMouseReleased() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    
    /**
        Checks if the primary mouse button was double-clicked since the last update and the
        double-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was double-clicked since the last update and the 
        double-click occurred within this Sprite's bounds.
    */
    public boolean isMouseDoubleClicked() {
        return Input.isPressed(Input.KEY_DOUBLE_MOUSE_BUTTON_1) && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    
    /**
        Checks if the primary mouse button was triple-clicked since the last update and the
        triple-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was triple-clicked since the last update and the 
        triple-click occurred within this Sprite's bounds.
    */
    public boolean isMouseTripleClicked() {
        return Input.isPressed(Input.KEY_TRIPLE_MOUSE_BUTTON_1) && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    
    /**
        Checks if the mouse wheel was rotated over this Sprite.
        @return true if the mouse wheel was rotated over this sprite since the  
        last rendering frame.
    */
    public boolean isMouseWheelRotated() {
        return Input.getMouseWheelRotation() != 0 && 
            contains(Input.getMouseWheelX(), Input.getMouseWheelY());
    }
    
    
// CONVENIENCE METHODS - BELOW THIS LINE THAR BE DRAGONS  


    //
    // Move as int convenience methods
    //
    
    public void move(int startX, int startY, int endX, int endY, int duration) {
        x.animate(startX, endX, duration);
        y.animate(startY, endY, duration);
    }
    
    
    public void move(int startX, int startY, int endX, int endY, int duration, Easing easing) {
        x.animate(startX, endX, duration, easing);
        y.animate(startY, endY, duration, easing);
    }
    
    
    public void move(int startX, int startY, int endX, int endY, int duration, Easing easing,
        int startDelay)
    {
        x.animate(startX, endX, duration, easing, startDelay);
        y.animate(startY, endY, duration, easing, startDelay);
    }
    
    
    public void moveTo(int x, int y, int duration) {
        this.x.animateTo(x, duration);
        this.y.animateTo(y, duration);
    }
    
    
    public void moveTo(int x, int y, int duration, Easing easing) {
        this.x.animateTo(x, duration, easing);
        this.y.animateTo(y, duration, easing);
    }
    
    
    public void moveTo(int x, int y, int duration, Easing easing, int startDelay) {
        this.x.animateTo(x, duration, easing, startDelay);
        this.y.animateTo(y, duration, easing, startDelay);
    }

    
    //
    // Move as double convenience methods
    //
    
    
    public void move(double startX, double startY, double endX, double endY, int duration) {
        x.animate(startX, endX, duration);
        y.animate(startY, endY, duration);
    }
    
    
    public void move(double startX, double startY, double endX, double endY, int duration,
        Easing easing) 
    {
        x.animate(startX, endX, duration, easing);
        y.animate(startY, endY, duration, easing);
    }
    
    
    public void move(double startX, double startY, double endX, double endY, int duration,
        Easing easing, int startDelay)
    {
        x.animate(startX, endX, duration, easing, startDelay);
        y.animate(startY, endY, duration, easing, startDelay);
    }
    
    public void moveTo(double x, double y, int duration) {
        this.x.animateTo(x, duration);
        this.y.animateTo(y, duration);
    }
    
    
    public void moveTo(double x, double y, int duration, Easing easing) {
        this.x.animateTo(x, duration, easing);
        this.y.animateTo(y, duration, easing);
    }
    
    
    public void moveTo(double x, double y, int duration, Easing easing, int startDelay) {
        this.x.animateTo(x, duration, easing, startDelay);
        this.y.animateTo(y, duration, easing, startDelay);
    }
    
    
    //
    // Scale as int convenience methods
    //

    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration) 
    {
        width.animate(width1, width2, duration);
        height.animate(height1, height2, duration);
    }
    
    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration, Easing easing) 
    {
        width.animate(width1, width2, duration, easing);
        height.animate(height1, height2, duration, easing);
    }
    
    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration, Easing easing, int startDelay)
    {
        width.animate(width1, width2, duration, easing, startDelay);
        height.animate(height1, height2, duration, easing, startDelay);
    }
    
    
    public void scaleTo(int width, int height, int duration) {
        this.width.animateTo(width, duration);
        this.height.animateTo(height, duration);
    }
    
    
    public void scaleTo(int width, int height, int duration, Easing easing) {
        this.width.animateTo(width, duration, easing);
        this.height.animateTo(height, duration, easing);
    }    
    
    
    public void scaleTo(int width, int height, int duration, Easing easing, 
        int startDelay) 
    {
        this.width.animateTo(width, duration, easing, startDelay);
        this.height.animateTo(height, duration, easing, startDelay);
    }    
    
    
    //
    // Scale as double convenience methods
    //
    
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration) 
    {
        width.animate(width1, width2, duration);
        height.animate(height1, height2, duration);
    }
    
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration, Easing easing) 
    {
        width.animate(width1, width2, duration, easing);
        height.animate(height1, height2, duration, easing);
    }
    
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration, Easing easing, int startDelay)
    {
        width.animate(width1, width2, duration, easing, startDelay);
        height.animate(height1, height2, duration, easing, startDelay);
    }
    
    
    public void scaleTo(double width, double height, int duration) {
        this.width.animateTo(width, duration);
        this.height.animateTo(height, duration);
    }
    
    
    public void scaleTo(double width, double height, int duration, Easing easing) {
        this.width.animateTo(width, duration, easing);
        this.height.animateTo(height, duration, easing);
    }    
    
    
    public void scaleTo(double width, double height, int duration, Easing easing, 
        int startDelay) 
    {
        this.width.animateTo(width, duration, easing, startDelay);
        this.height.animateTo(height, duration, easing, startDelay);
    }            
    
}
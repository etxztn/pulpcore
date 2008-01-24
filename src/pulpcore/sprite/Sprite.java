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
import pulpcore.Stage;

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
    
    /**
        The flag indicating whether this Sprite is enabled. An enabled sprite can 
        respond to user input. Sprites are enabled by default.
    */
    public final Bool enabled = new Bool(this, true);
    
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
    
    private int cursor = -1;
    private int anchor = DEFAULT;
    private int composite = -1;
    private int cosAngle = CoreMath.ONE;
    private int sinAngle = 0;
    
    private Group parent;
    private int parentTransformModCount;
    private boolean dirty = true;
    private boolean transformDirty = true;
    
    /** Transform based on parent transform and this sprite's x, y, width, height, and angle. */
    protected final Transform transform = new Transform();
    
    /** The draw bounding box used for directy rectangles in Scene2D */
    private Rect dirtyRect;
    
    
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
        Gets this Sprite's parent Group, or null if this Sprite does not have a parent.
    */
    public final Group getParent() {
        return parent;
    }
    
    /**
        Gets this Sprite's oldest ancestor Group, or null if this Sprite does not have a parent.
    */
    public final Group getRoot() {
        Group currRoot = null;
        Group nextRoot = parent;
        while (true) {
            if (nextRoot == null) {
                return currRoot;
            }
            else {
                currRoot = nextRoot;
                nextRoot = nextRoot.getParent();
            }
        }
    }
    
    /* package-private */ final void setParent(Group parent) {
        if (this.parent != parent) {
            this.parent = parent;
            if (parent == null) {
                parentTransformModCount = -1;
            }
            else {
                parentTransformModCount = parent.getTransformModCount();
            }
            setDirty(true);
        }
    }
    
    /**
        Returns true if this Sprite's enabled property is set to true and its parent, if any, 
        is enabled.
    */
    public final boolean isEnabled() {
        return (enabled.get() == true && (parent == null || parent.isEnabled())); 
    }
    
    /**
        Returns true if this Sprite's enabled property is set to true, its visible property set
        to true, its alpha property is greater than zero, and its parent, if any, is enabled
        and visible.
    */
    public final boolean isEnabledAndVisible() {
        return (enabled.get() == true && visible.get() == true && 
            alpha.get() > 0 && (parent == null || parent.isEnabledAndVisible())); 
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
    public final Rect getDirtyRect() {
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
    public final boolean updateDirtyRect() {
        
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
            
            updateTransform();
            
            changed |= transform.getBounds(
                getNaturalWidth(), getNaturalHeight(), dirtyRect);
        }
        
        return changed;
       
    }
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public final void clearDirtyRect() {
        if (dirtyRect != null) {
            dirtyRect.width = -1;
        }
    }
    
    /**
        Marks this Sprite as dirty, which will force it to redraw on the next frame.
    */
    public final void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            transformDirty = true;
        }
    }
    
    /**
        Returns true if the Sprite's properties have changed since the last call to draw()
    */
    public final boolean isDirty() {
        return dirty;
    }
    
    /* package-private */ final Transform getTransform() {
        updateTransform();
        return transform;
    }
    
    /* package-private */ final boolean isTransformDirty() {
        if (transformDirty) {
            return true;
        }
        else if (parent == null) {
            return false;
        }
        else {
            return (parentTransformModCount != parent.getTransformModCount() ||
                parent.isTransformDirty());
        }
    }
    
    /* package-private */ final Transform getParentTransform() {
        if (parent == null) {
            return Stage.getDefaultTransform();
        }
        else {
            return parent.getTransform();
        }
    }
    
    /* package-private */ final void updateTransform(Transform parentTransform) {
        transform.set(parentTransform);
            
        // Translate
        if (pixelSnapping.get()) {
            transform.translate(CoreMath.floor(x.getAsFixed()), 
                CoreMath.floor(y.getAsFixed()));
        }
        else {
            transform.translate(x.getAsFixed(), y.getAsFixed());
        }
        
        // Rotate
        if (cosAngle != CoreMath.ONE || sinAngle != 0) {
            transform.rotate(cosAngle, sinAngle);
        }
        
        // Scale
        int naturalWidth = getNaturalWidth();
        int naturalHeight = getNaturalHeight();
        if (naturalWidth > 0 && naturalHeight > 0 &&
            (naturalWidth != width.getAsFixed() || naturalHeight != height.getAsFixed())) 
        {
            int sx = CoreMath.div(width.getAsFixed(), naturalWidth);
            int sy = CoreMath.div(height.getAsFixed(), naturalHeight);
            transform.scale(sx, sy);
        }
        
        // Adjust for anchor
        if (pixelSnapping.get()) {
            transform.translate(CoreMath.floor(-getAnchorX()), CoreMath.floor(-getAnchorY()));
        }
        else {
            transform.translate(-getAnchorX(), -getAnchorY());
        }
    }
    
    /**
        Gets the bounds relative to the parent.
    */
    /* package-private */ final Rect getRelativeBounds() {
        Transform oldTransform = new Transform(transform);
        Transform identityTransform = new Transform();
        Rect bounds = new Rect();
        
        updateTransform(identityTransform);
        transform.getBounds(getNaturalWidth(), getNaturalHeight(), bounds);
        
        transform.set(oldTransform);
        return bounds;
    }
    
    private final void updateTransform() {
        if (isTransformDirty()) {
            updateTransform(getParentTransform());
            
            // Keep track of dirty state
            transformDirty = false;
            if (parent != null) {
                // Must happen after getParentTransform()
                parentTransformModCount = parent.getTransformModCount();
            }
            if (this instanceof Group) {
                ((Group)this).updateTransformModCount();
            }
        }
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
            // Special case: make sure centered sprites are drawn on an integer boundary
            return CoreMath.floor(getNaturalWidth() / 2);
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
            // Special case: make sure centered sprites are drawn on an integer boundary
            return CoreMath.floor(getNaturalHeight() / 2);
        }
        else if ((anchor & BOTTOM) != 0) {
            return getNaturalHeight() - CoreMath.ONE;
        }
        else {
            return 0;
        }
    }
    
    /**
        Sets the anchor of this Sprite. The anchor affects where the Sprite is drawn in
        relation to its (x, y) location, and can be one of {@link #DEFAULT}, 
        {@link #NORTH}, {@link #SOUTH}, {@link #WEST}, {@link #EAST},
        {@link #NORTH_WEST}, {@link #SOUTH_WEST}, {@link #NORTH_EAST}, {@link #SOUTH_EAST}, or
        {@link #CENTER}.
        <p>
        <pre>
        NW     N     NE
          +----+----+
          |         |
        W +    *    + E
          |         |
          +----+----+
        SW     S     SE
        </pre>
        The {@link #DEFAULT} anchor is equivilant to {@link #NORTH_WEST} for most Sprites (except
        for ImageSprites, which use the CoreImage's hotspot as the anchor).
    */
    public final void setAnchor(int anchor) {
        if (this.anchor != anchor) {
            this.anchor = anchor;
            setDirty(true);
        }
    }
    
    public final int getAnchor() {
        return anchor;
    }
    
    /**
        Sets the cursor for this Sprite. By default, a Sprite does not have a defined cursor.
        Note, the Sprite itself does not set the cursor -
        it is set by {@link pulpcore.scene.Scene2D}.
        @see pulpcore.Input
        @see #getCursor()
        @see #clearCursor()
    */
    public final void setCursor(int cursor) {
        this.cursor = cursor;
    }
    
    /**
        Clears the cursor for this Sprite, so that it's parent cursor is used.
        @see pulpcore.Input
        @see #getCursor()
        @see #setCursor(int)
    */
    public final void clearCursor() {
        this.cursor = -1;
    }
    
    
    /**
        Gets the cursor for this Sprite. If a cursor is not defined for this Sprite, the parent's
        cursor is used.
        @see pulpcore.Input
        @see #setCursor(int)
        @see #clearCursor()
    */
    public final int getCursor() {
        if (cursor == -1) {
            if (parent == null) {
                return Input.CURSOR_DEFAULT;
            }
            else {
                return parent.getCursor();
            }
        }
        else {
            return cursor;
        }
    }
    
    /**
        Sets the compositing method used to draw this Sprite. By default, the compositing 
        method is -1, which means the compositing method of this Sprite's parent is used. 
        Valid values are -1, CoreGraphics.COMPOSITE_SRC_OVER, CoreGraphics.COMPOSITE_SRC, and
        CoreGraphics.COMPOSITE_ADD.
    */
    public final void setComposite(int composite) {
        if (this.composite != composite) {
            this.composite = composite;
            setDirty(true);
        }
    }
    
    public final int getComposite() {
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
        enabled.update(elapsedTime);
        pixelSnapping.update(elapsedTime);
    }
    
    public final void draw(CoreGraphics g) {
        
        if (isDirty()) {
            updateTransform();
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
        g.setTransform(transform);
        
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
        Gets the integer x-coordinate in Local Space of the specified location in
        View Space. Returns Integer.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public final int getLocalX(int viewX, int viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localX = transform.inverseTransformX(fx, fy);
        if (localX == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return CoreMath.toInt(localX);
    }
    
    /**
        Gets the integer y-coordinate in Local Space of the specified location in
        View Space. Returns Integer.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public final int getLocalY(int viewX, int viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localY = transform.inverseTransformY(fx, fy);
        if (localY == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return CoreMath.toInt(localY);
    }
    
    /**
        Gets the integer x-coordinate in View Space of the specified location in
        Local Space.
    */
    public final int getViewX(int localX, int localY) {
        updateTransform();
        int fx = CoreMath.toFixed(localX);
        int fy = CoreMath.toFixed(localY);
        return CoreMath.toInt(transform.transformX(fx, fy));
    }
    
    /**
        Gets the integer y-coordinate in View Space of the specified location in
        Local Space.
    */
    public final int getViewY(int localX, int localY) {
        updateTransform();
        int fx = CoreMath.toFixed(localX);
        int fy = CoreMath.toFixed(localY);
        return CoreMath.toInt(transform.transformY(fx, fy));
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite. 
            
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
        @return true if the specified point is within the bounds of this 
        Sprite.
    */
    public final boolean contains(int viewX, int viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localX = transform.inverseTransformX(fx, fy);
        int localY = transform.inverseTransformY(fx, fy);
        
        if (localX == Integer.MAX_VALUE || localY == Integer.MAX_VALUE) {
            return false;
        }
        
        return (localX >= 0 && localX < getNaturalWidth() &&
                localY >= 0 && localY < getNaturalHeight());
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite and this Sprite is the top-most Sprite at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public final boolean isPick(int viewX, int viewY) {
        if (contains(viewX, viewY)) {
            // Since the location is within the sprite, root.pick() won't search below this
            // sprite in the scene graph
            Group root = getRoot();
            return (root == null || root.pick(viewX, viewY) == this);
        }
        else {
            return false;
        }
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite and this Sprite is the top-most visible and enabled Sprite at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public final boolean isPickEnabledAndVisible(int viewX, int viewY) {
        if (contains(viewX, viewY)) {
            // Since the location is within the sprite, root.pick() won't search below this
            // sprite in the scene graph
            Group root = getRoot();
            return (root == null || root.pickEnabledAndVisible(viewX, viewY) == this);
        }
        else {
            return false;
        }
    }
    
    //public final boolean intersects(Sprite sprite) {
    //    sprite.updateTransform();
    //    updateTransform();
    //    
    //    // Step 1: Get this sprite's points
    //    int w1 = getNaturalWidth();
    //    int h1 = getNaturalHeight();
    //    V[] pointsA = {
    //        new V(0, 0),
    //        new V(w1, 0),
    //        new V(w1, h1),
    //        new V(0, h1),
    //    };
    //    
    //    // Step 2: Get points of specified sprite (convert this Sprite's Local Space)
    //    int w2 = sprite.getNaturalWidth();
    //    int h2 = sprite.getNaturalHeight();
    //    V[] pointsB = {
    //        new V(sprite.transform.transformX(0, 0), sprite.transform.transformY(0, 0)),
    //        new V(sprite.transform.transformX(w2, 0), sprite.transform.transformY(w2, 0)),
    //        new V(sprite.transform.transformX(w2, h2), sprite.transform.transformY(w2, h2)),
    //        new V(sprite.transform.transformX(0, h2), sprite.transform.transformY(0, h2)),
    //    };
    //    for (int i = 0; i < 4; i++) {
    //        V p = pointsB[i];
    //        int lx = transform.inverseTransformX(p.x, p.y);
    //        int ly = transform.inverseTransformY(p.x, p.y);
    //        if (lx == Integer.MAX_VALUE || ly == Integer.MAX_VALUE) {
    //            return false;
    //        }
    //        p.x = lx;
    //        p.y = ly;
    //    }
    //    
    //    // Step 3: Use separating axis theorem (four tests)
    //    // Line A: (0,0)->(w1,0)
    //    // Line B: (0,0)->(0,h1)
    //    // Line C: Perpendicular to (fx[0], fy[0])->(fx[1], fy[1])
    //    // Line D: Perpendicular to (fx[0], fy[0])->(fx[3], fy[3])
    //    V[][] vectors = { 
    //        new V(w1, 0),
    //        new V(0, h1),
    //        new V(pointsB[1].y-pointsB[0].y, pointsB[0].x-pointsB[1].x),
    //        new V(pointsB[3].y-pointsB[0].y, pointsB[0].x-pointsB[3].x)
    //    };
    //    for (int i = 0; i < vectors.length; i++) {
    //        
    //    }
    //}
        /*
           Any corner point in A lies inside box B.
           Any corner point in B lies inside box A.
           Any segment in A intersects any segment in B (after first two checks, there would have to be 2 intersections)
        */        //// Step 3: Check if any segment intersects the AABB (0,0)->(naturalWidth, naturalHeight)
        //int width = getNaturalWidth();
        //int height = getNaturalHeight();
        //for (int i = 0; i < 4; i++) {
        //    int j = (i+1)&3;
        //    if (lineSegmentBoxIntersection(fx[i], fy[i], fx[j], fy[j], 0, 0, width, height)) {
        //        return true;
        //    }
        //}
        //return false;
    //}
    //
    ///**
    //    Returns true if a line segment intersects an axis-aligned bounding box.
    //*/
    //private static boolean lineSegmentBoxIntersection(
    //    int lineX1, int lineY1, int lineX2, int lineY2,
    //    int boxXmin, int boxYmin, int boxXmax, int boxYmax)
    //{
    //    int st, et;
    //    int fst = 0;
    //    int fet = CoreMath.ONE;
    //    
    //    // Check x
    //    if (lineX1 < lineX2) {
    //        if (lineX1 > boxXmax || lineX2 < boxXmin) {
    //            return false;
    //        }
    //        int d = lineX2 - lineX1;
    //        st = (lineX1 < boxXmin) ? CoreMath.div(boxXmin - lineX1, d) : 0;
    //        et = (lineX2 > boxXmax) ? CoreMath.div(boxXmax - lineX1, d) : 1;
    //    }
    //    else {
    //        if (lineX2 > boxXmax || lineX1 < boxXmin) {
    //            return false;
    //        }
    //        int d = lineX2 - lineX1;
    //        st = (lineX1 > boxXmax) ? CoreMath.div(boxXmax - lineX1, d) : 0;
    //        et = (lineX2 < boxXmin) ? CoreMath.div(boxXmin - lineX1, d) : 1;
    //    }
    //    if (st > fst) {
    //        fst = st;
    //    }
    //    if (et < fet) {
    //        fet = et;
    //    }
    //    if (fet < fst) {
    //        return false;
    //    }
    //    
    //    // Check y
    //    if (lineY1 < lineY2) {
    //        if (lineY1 > boxYmax || lineY2 < boxYmin) {
    //            return false;
    //        }
    //        int d = lineY2 - lineY1;
    //        st = (lineY1 < boxYmin) ? CoreMath.div(boxYmin - lineY1, d) : 0;
    //        et = (lineY2 > boxYmax) ? CoreMath.div(boxYmax - lineY1, d) : 1;
    //    }
    //    else {
    //        if (lineY2 > boxYmax || lineY1 < boxYmin) {
    //            return false;
    //        }
    //        int d = lineY2 - lineY1;
    //        st = (lineY1 > boxYmax) ? CoreMath.div(boxYmax - lineY1, d) : 0;
    //        et = (lineY2 < boxYmin) ? CoreMath.div(boxYmin - lineY1, d) : 1;
    //    }
    //    if (st > fst) {
    //        fst = st;
    //    }
    //    if (et < fet) {
    //        fet = et;
    //    }
    //    if (fet < fst) {
    //        return false;
    //    }
    //    
    //    return true;
    //}
        
    /**
        Checks if this Sprite (and its parents) are enabled, and
        the mouse is currently within the bounds of this Sprite.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite.
    */
    public boolean isMouseOver() {
        return Input.isMouseInside() && isEnabled() && contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the mouse is within the bounds of this Sprite, and the primary mouse
        button is not pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is not pressed down.
    */
    public boolean isMouseHover() {
        return Input.isMouseInside() && !Input.isMouseDown() && isEnabled() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the mouse is currently within the bounds of this Sprite, and the primary mouse
        button is pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is pressed down.
    */
    public boolean isMouseDown() {
        return Input.isMouseInside() && Input.isMouseDown() && isEnabled() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was pressed since the last update, and the press 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was pressed since the last update and the press 
        occurred within this Sprite's bounds.
    */
    public boolean isMousePressed() {
        return Input.isMouseInside() && Input.isMousePressed() && isEnabled() && 
            contains(Input.getMousePressX(), Input.getMousePressY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was released since the last update, and the release 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was released since the last update and the release 
        occurred within this Sprite's bounds.
    */
    public boolean isMouseReleased() {
        return Input.isMouseInside() && Input.isMouseReleased() && isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was double-clicked since the last update, and the
        double-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was double-clicked since the last update and the 
        double-click occurred within this Sprite's bounds.
    */
    public boolean isMouseDoubleClicked() {
        return Input.isMouseInside() && Input.isPressed(Input.KEY_DOUBLE_MOUSE_BUTTON_1) && 
            isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was triple-clicked since the last update, and the
        triple-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was triple-clicked since the last update and the 
        triple-click occurred within this Sprite's bounds.
    */
    public boolean isMouseTripleClicked() {
        return Input.isMouseInside() && Input.isPressed(Input.KEY_TRIPLE_MOUSE_BUTTON_1) && 
            isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled and
        the mouse wheel was rotated over this Sprite.
        @return true if the mouse wheel was rotated over this sprite since the  
        last rendering frame.
    */
    public boolean isMouseWheelRotated() {
        return Input.isMouseInside() && Input.getMouseWheelRotation() != 0 && isEnabled() && 
            contains(Input.getMouseWheelX(), Input.getMouseWheelY());
    }
    
// CONVENIENCE METHODS

    /**
        Sets the location of this Sprite.
    */
    public void setLocation(int x, int y) {
        this.x.set(x);
        this.y.set(y);
    }
    
    /**
        Sets the location of this Sprite.
    */
    public void setLocation(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }    
    
    /**
        Translates the location of this Sprite.
    */
    public void translate(int x, int y) {
        this.x.set(this.x.get() + x);
        this.y.set(this.y.get() + y);
    }
    
    /**
        Translates the location of this Sprite.
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
    
    // Quick-and-dirty fixed-point vector class for sprite.intersects()
    private static class V {
        public int x;
        public int y;
        
        public V() {
            setTo(0, 0);
        }
        
        public V(int x, int y) {
            setTo(x, y);
        }
        
        public V(V v) {
            setTo(v.x, v.y);
        }
        
        public void setTo(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public int getLengthSq() {
            return CoreMath.mul(x, x) + CoreMath.mul(y, y);
        }
        
        public int getLength() {
            return CoreMath.sqrt(getLengthSq());
        }
        
        public void setLength(int newLength) {
            int length = getLength();
            if (length != 0) {
                mulDiv(newLength, length);
            }
        }
        
        public void normalize() {
            int length = getLength();
            if (length != 0) {
                divide(length);
            }
        }    
    
        public void add(int x, int y) {
            this.x += x;
            this.y += y;
        }
    
        public void subtract(int x, int y) {
            add(-x, -y);
        }
    
        public void add(V v) {
            add(v.x, v.y);
        }
    
        public void subtract(V v) {
            add(-v.x, -v.y);
        }    
        
        public void multiply(int s) {
           x = CoreMath.mul(x, s);
           y = CoreMath.mul(y, s);
        }
    
        public void divide(int s) {
           x = CoreMath.div(x, s);
           y = CoreMath.div(y, s);
        }
        
        public void mulDiv(int n, int d) {
           x = CoreMath.mulDiv(x, n, d);
           y = CoreMath.mulDiv(y, n, d);
        }
    
        public int getDotProduct(V v) {
            return CoreMath.mul(x, v.x) + CoreMath.mul(y, v.y);
        }
    }
}
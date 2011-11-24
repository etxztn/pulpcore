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

package org.pulpcore.view;

import java.util.ArrayList;
import java.util.Iterator;
import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Graphics.BlendMode;
import org.pulpcore.graphics.filter.Filter;
import org.pulpcore.math.AffineTransform;
import org.pulpcore.math.Rectf;
import org.pulpcore.math.Recti;
import org.pulpcore.math.Tuple2f;
import org.pulpcore.runtime.Input;
import org.pulpcore.runtime.Input.TouchEvent;
import org.pulpcore.runtime.Input.TouchScrollEvent;
import org.pulpcore.util.Objects;
import org.pulpcore.view.animation.Action;
import org.pulpcore.view.property.ColorProperty;
import org.pulpcore.view.property.FloatProperty;
import org.pulpcore.view.property.Property;
import org.pulpcore.view.property.PropertyListener;

public class View implements PropertyListener {

    /* package private*/ static final AffineTransform IDENTITY = new AffineTransform();
    
    public interface OnTouchEnterListener {
        public void onEnter(View view, TouchEvent event);
    }
    
    public interface OnTouchExitListener {
        public void onExit(View view, TouchEvent event);
    }

    public interface OnTouchPressListener {
        public void onPress(View view, TouchEvent event);
    }

    /**
    Note, this event is sent to the original view the mouse was pressed on, but after
    mouse movement, the view param may be a different View.
    @param view The View the mouse was released over.
    */
    public interface OnTouchReleaseListener {
        public void onRelease(View view, TouchEvent event);
    }

    public interface OnTouchTapListener {
        public void onTap(View view, TouchEvent event);
    }

    public interface OnTouchHoverListener {
        public void onHover(View view, TouchEvent event);
    }

    public interface OnTouchMoveListener {
        public void onMove(View view, TouchEvent event);
    }

    public interface OnTouchScrollListener {
        public void onScroll(View view, TouchScrollEvent event);
    }

    private static class TouchListeners {
        OnTouchEnterListener touchEnterListener;
        OnTouchExitListener touchExitListener;
        OnTouchPressListener touchPressListener;
        OnTouchReleaseListener touchReleaseListener;
        OnTouchTapListener touchTapListener;
        OnTouchHoverListener touchHoverListener;
        OnTouchMoveListener touchMoveListener;
        OnTouchScrollListener touchScrollListener;
    }

    //
    // Properties
    //
    
    /** The x location of this View. */
    public final FloatProperty x = new FloatProperty(0, this);
    
    /** The y location of this View. */
    public final FloatProperty y = new FloatProperty(0, this);
    
    /** The scale of this View in the x dimension. */
    public final FloatProperty scaleX = new FloatProperty(1, this);
    
    /**  The scale of this View in the y dimension. */
    public final FloatProperty scaleY = new FloatProperty(1, this);
    
    /** 
        The x anchor point of this View, in range from 0.0 to 1.0. A value of
        0.0 is far left point of the View and a value of 1.0 is far right point.
        The default is 0.0.
    */
    public final FloatProperty anchorX = new FloatProperty(0, this);

    /**
        The y anchor point of this View, in range from 0.0 to 1.0. A value of
        0.0 is far top point of the View and a value of 1.0 is far bottom point.
        The default is 0.0.
    */
    public final FloatProperty anchorY = new FloatProperty(0, this);
    
    /** 
        The angle of this View, in radians, typically in the range from 0 to 2*PI,
        although the angle can have any value. The View is rotated around its anchor.
    */
    public final FloatProperty angle = new FloatProperty(0, this);

    /** 
        The opacity of this View, in range from 0 to 1.0. A value of 0 is fully
        transparent and a value of 1.0 is fully opaque. The default is 1.0.
    */
    public final FloatProperty opacity = new FloatProperty(1, this);

    /**
     The background color of the View. The default value is Color.TRANSPARENT.
     */
    public final ColorProperty backgroundColor = new ColorProperty(Color.TRANSPARENT, this);

    // Required information
    private Group superview;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean pixelSnapping = false;
    private boolean pixelLevelChecks = false;

    private float width;
    private float height;

    // State information
    private float cosAngle = 1;
    private float sinAngle = 0;
    private int superviewTransformModCount;
    private boolean dirty = true;
    private boolean transformDirty = true;
    private boolean contentsDirty = true;

    // Optional information
    private int cursor = -1;
    private BlendMode blendMode = null;
    private Filter filter = null;
    private Object tag = null;
    private ArrayList<Action> actions = null;
    // Most views won't have any touchListeners, so instead of X number of null fields
    // (which would increase memory per view), just have one container of all possible listeners.
    private TouchListeners touchListeners = null;
    
    /** 
        The view transform, used for dirty rectangles and collision detection.
    */
    private final AffineTransform viewTransform = new AffineTransform();
    
    /**
        The draw transform, which is set only when it is different from the viewTransform.
    */
    private AffineTransform drawTransform = null;
    
    /** The draw bounding box used for dirty rectangles */
    private Recti dirtyRect;

    public View() {
        this(0, 0);
    }
    
    public View(float width, float height) {
        setSize(width, height);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setWidth(float width) {
        if (this.width != width) {
            this.width = width;
            setContentsDirty();
        }
    }

    public void setHeight(float height) {
        if (this.height != height) {
            this.height = height;
            setContentsDirty();
        }
    }

    public void setSize(float width, float height) {
        setWidth(width);
        setHeight(height);
    }
    
    public float getX() {
        return x.get();
    }
    
    public void setX(float x) {
        this.x.set(x);
    }
    
    public float getY() {
        return y.get();
    }
    
    public void setY(float y) {
        this.y.set(y);
    }

    /**
        Sets the location of this View.
    */
    public void setLocation(float x, float y) {
        this.x.set(x);
        this.y.set(y);
    }

    public void setBounds(float x, float y, float width, float height) {
        setLocation(x,y);
        setSize(width, height);
    }

    /**
        Sets the cursor for this View. By default, a View does not have a defined cursor.
        @see pulpcore.Input
        @see #getCursor()
        @see #clearCursor()
    */
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    /**
        Clears the cursor for this View, so that it's parent cursor is used.
        @see pulpcore.Input
        @see #getCursor()
        @see #setCursor(int)
    */
    public void clearCursor() {
        this.cursor = -1;
    }

    /**
        Gets the cursor for this View. If a cursor is not defined for this View, the parent's
        cursor is used.
        @see pulpcore.Input
        @see #setCursor(int)
        @see #clearCursor()
    */
    public int getCursor() {
        if (cursor == -1) {
            if (superview == null) {
                return Input.CURSOR_DEFAULT;
            }
            else {
                return superview.getCursor();
            }
        }
        else {
            return cursor;
        }
    }

    /**
        Sets the blend mode used to draw this View. By default, the blend mode
        method is null, which means the blend mode of this View's parent is used.
        @see pulpcore.image.BlendMode
    */
    public void setBlendMode(BlendMode blendMode) {
        if (this.blendMode != blendMode) {
            this.blendMode = blendMode;
            setContentsDirty();
        }
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public boolean isPixelSnapping() {
        return pixelSnapping;
    }

    /**
        Sets whether pixel snapping enabled for rendering this View.
        If this value is true, only the integer portion of the x and y
        properties are used to draw this View.
        <p>
        Enabling pixel snapping may allow some type of images (e.g. pixel art) to look better.
        <p>
        This value is false by default.
    */
    public void setPixelSnapping(boolean pixelSnapping) {
        if (this.pixelSnapping != pixelSnapping) {
            this.pixelSnapping = pixelSnapping;
            setTransformDirty();
        }
    }

    /**
        Sets whether this View should use pixel-level checking for intersections and picking.
        Note, pixel-level checks only work on subclasses of View that
        implement the {@link #isTransparent(int, int) } method ({@link Button},
        {@link ImageView}, {@link Label}, {@link StretchableImageView}).
    */
    public void setPixelLevelChecks(boolean pixelLevelChecks) {
        this.pixelLevelChecks = pixelLevelChecks;
    }

    /**
        Returns true if this View should use pixel-level checks for intersections and picking.
        By default, pixel level checks are off.
    */
    public boolean getPixelLevelChecks() {
        return pixelLevelChecks;
    }

    /**
        Returns true if the visibility flag is set.
    */
    public boolean isVisible() {
        return visible;
    }

    /**
        Sets the View's visibility flag. If a View is not visible, it is not drawn.
    */
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            setContentsDirty();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
        The flag indicating whether this View is enabled. An enabled View can
        respond to user input. Views are enabled by default.
        @see #canInteract()
    */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public float getScaleX() {
        return scaleX.get();
    }
    
    public void setScaleX(float scaleX) {
        this.scaleX.set(scaleX);
    }
    
    public float getScaleY() {
        return scaleY.get();
    }

    public void setScaleY(float scaleY) {
        this.scaleY.set(scaleY);
    }

    /**
        Sets the scale of this View.
        Changing the size is non-destructive. For example, an ImageView
        doesn't internally scale it's image, instead, the image is rendered scaled.
    */
    public void setScale(float scale) {
        this.scaleX.set(scale);
        this.scaleY.set(scale);
    }

    /**
        Sets the size of this View.
        Changing the size is non-destructive. For example, an ImageView
        doesn't internally scale it's image, instead, the image is rendered scaled.
    */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX.set(scaleX);
        this.scaleY.set(scaleY);
    }
    
    public float getAngle() {
        return angle.get();
    }
    
    public void setAngle(float angle) {
        this.angle.set(angle);
    }
    
    public float getAnchorX() {
        return anchorX.get();
    }
    
    public void setAnchorX(float anchorX) {
        this.anchorX.set(anchorX);
    }

    public float getAnchorY() {
        return anchorY.get();
    }
    
    public void setAnchorY(float anchorY) {
        this.anchorY.set(anchorY);
    }

    /**
        Sets the anchor of this View. The anchor affects where the View is drawn in
        relation to its (x, y) location. The anchor of each axis is typically from 0.0
        (top/left) and 1.0 (bottom/right):
        <pre>
        (0.0,0.0)  (0.5,0.0)  (1.0,0.0)
            +----------+----------+
            |                     |
            |                     |
            |                     |
            |      (0.5,0.5)      |
  (0.0,0.5) +          *          + (1.0,0.5)
            |                     |
            |                     |
            |                     |
            |                     |
            +----------+----------+
        (0.0,1.0)  (0.5,1.0)  (1.0,1.0)
        </pre>
        For example, to center the View at it's (x,y) location, use
        <code>View.setAnchor(0.5f, 0.5f);</code>
        @see #anchorX
        @see #anchorY
    */
    public void setAnchor(float anchorX, float anchorY) {
        this.anchorX.set(anchorX);
        this.anchorY.set(anchorY);
    }

    /**
        Gets this View's tag.
        @see #setTag(Object)
        @see Group#findWithTag(Object)
    */
    public Object getTag() {
        return tag;
    }

    /**
        Sets this View's tag. The tag can be used for marking the view or storing information
        with it. Different Views can share identical tags. By default, the tag is {@code null}.
        @see #getTag()
        @see Group#findWithTag(Object)
    */
    public void setTag(Object tag) {
        this.tag = tag;
    }

    /**
     Returns true if this View is opaque. In other words, before applying transforms and alpha,
     all the pixels within it's bounds are drawn and are themselves opaque. By default,
     this method returns true if the {@link #backgroundColor} is opaque. Subclasses should return true
     if either their content is opaque, or the {@link #backgroundColor} is opaque.
    */
    public boolean isOpaque() {
        return Color.isOpaque(backgroundColor.get());
    }
    
    public int getBackgroundColor() {
        return backgroundColor.get();
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }
    
    public float getOpacity() {
        return opacity.get();
    }
    
    public void setOpacity(float opacity) {
        this.opacity.set(opacity);
    }

    //
    // Actions/tick
    //

    public void addAction(Action action) {
        if (action != null) {
            if (actions == null) {
                actions = new ArrayList<Action>();
            }
            action.start();
            actions.add(action);
        }
    }

    public boolean stopAction(Action action) {
        if (actions != null && action != null) {
            boolean found = actions.remove(action);
            if (found && !action.isFinished()) {
                action.stop();
                return true;
            }
        }
        return false;
    }
    
    public void stopActionWithTag(Object tag) {
        stopAction(findActionWithTag(tag));
    }

    /**
        Finds the Action whose tag is equal to the specified tag (using
        {@code Objects.equal(tag, action.getTag())}. Returns null 
        if no Action with the specified tag is found.
    */
    public Action findActionWithTag(Object tag) {
        if (actions != null && tag != null) {
            for (Action action : actions) {
                if (Objects.equal(tag, action.getTag())) {
                    return action;
                }
            }
        }
        return null;
    }
    
    /*package private */ void tick(float dt) {
        if (actions != null) {
            Iterator<Action> i = actions.iterator();
            while (i.hasNext()) {
                Action action = i.next();
                action.tick(dt);
                if (action.isFinished()) {
                    i.remove();
                }
            }
            if (actions.isEmpty()) {
                actions = null;
            }
        }
        onTick(dt);
    }

    /**
    Notifies this view of a new frame.
    Subviews may override to perform an action every frame.
    The tick occurs after this view's actions are updated, before all input events are sent out,
    and before the view is rendered.    
    @param dt The time, in seconds, since the last tick.
    */
    public void onTick(float dt) {

    }

    //
    // Touch listeners
    //
    
    public OnTouchEnterListener getOnTouchEnterListener() {
        return touchListeners == null ? null : touchListeners.touchEnterListener;
    }

    public void setOnTouchEnterListener(OnTouchEnterListener touchEnterListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchEnterListener = touchEnterListener;
    }
    
    public OnTouchExitListener getOnTouchExitListener() {
        return touchListeners == null ? null : touchListeners.touchExitListener;
    }

    public void setOnTouchExitListener(OnTouchExitListener touchExitListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchExitListener = touchExitListener;
    }

    public OnTouchHoverListener getOnTouchHoverListener() {
        return touchListeners == null ? null : touchListeners.touchHoverListener;
    }

    public void setOnTouchHoverListener(OnTouchHoverListener touchHoverListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchHoverListener = touchHoverListener;
    }

    public OnTouchMoveListener getOnTouchMoveListener() {
        return touchListeners == null ? null : touchListeners.touchMoveListener;
    }

    public void setOnTouchMoveListener(OnTouchMoveListener touchMoveListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchMoveListener = touchMoveListener;
    }

    public OnTouchPressListener getOnTouchPressListener() {
        return touchListeners == null ? null : touchListeners.touchPressListener;
    }

    public void setOnTouchPressListener(OnTouchPressListener touchPressListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchPressListener = touchPressListener;
    }

    public OnTouchReleaseListener getOnTouchReleaseListener() {
        return touchListeners == null ? null : touchListeners.touchReleaseListener;
    }

    public void setOnTouchReleaseListener(OnTouchReleaseListener touchReleaseListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchReleaseListener = touchReleaseListener;
    }

    public OnTouchScrollListener getOnTouchScrollListener() {
        return touchListeners == null ? null : touchListeners.touchScrollListener;
    }

    public void setOnTouchScrollListener(OnTouchScrollListener touchScrollListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchScrollListener = touchScrollListener;
    }

    public OnTouchTapListener getOnTouchTapListener() {
        return touchListeners == null ? null : touchListeners.touchTapListener;
    }

    public void setOnTouchTapListener(OnTouchTapListener touchTapListener) {
        if (touchListeners == null) {
            touchListeners = new TouchListeners();
        }
        touchListeners.touchTapListener = touchTapListener;
    }

    //
    // Scene graph
    //
    
    /**
        Gets this View's parent Group, or null if this View does not have a parent.
    */
    public Group getSuperview() {
        return superview;
    }
    
    /**
        Removes this View from its parent Group. If this View does not have a parent,
        this method does nothing.
    */
    public void removeFromSuperview() {
        Group p = superview;
        if (p != null) {
            p.removeSubview(this);
        }
    }

    /**
        Gets this View's oldest ancestor Group, or null if this View does not have a superview.
    */
    public Group getRoot() {
        Group currRoot = null;
        Group nextRoot = getSuperview();
        while (true) {
            if (nextRoot == null) {
                return currRoot;
            }
            else {
                currRoot = nextRoot;
                nextRoot = nextRoot.getSuperview();
            }
        }
    }

    /**
     Gets the Scene this View belongs in, or null if this View is not in a Scene. If this View
     is a Scene, this method returns this View.
     */
    public Scene getScene() {
        View nextView = this;
        while (nextView != null) {
            if (nextView instanceof Scene) {
                return (Scene)nextView;
            }
            nextView = nextView.getSuperview();
        }
        return null;
    }
        
    /* package-private */ void setSuperview(Group superview) {
        if (this.superview != superview) {
            this.superview = superview;
            if (superview == null) {
                superviewTransformModCount = -1;
            }
            else {
                superviewTransformModCount = superview.getTransformModCount();
            }
            setTransformDirty();
        }
    }
    
    /**
        Returns true if this View is enabled, visible, has an opacity greater than zero, and
        if it has a parent, its parent's canInteract() method returns true.
    */
    public boolean canInteract() {
        return (isEnabled() && isVisible() && opacity.get() > 0 &&
                (superview == null || superview.canInteract()));
    }

    //
    // Dirty rects
    //
    
    /* package-private */ Recti getDirtyRect() {
        if (dirtyRect == null || dirtyRect.width <= 0) {
            return null;
        }
        else {
            return dirtyRect;
        }
    }

    /* package-private */ void clearDirtyRect() {
        if (dirtyRect != null) {
            dirtyRect.width = -1;
        }
    }
    
    /* package-private */ boolean updateDirtyRect(boolean parentVisible) {
        
        boolean changed = false;
        boolean groupWithContentChange = (this instanceof Group) && getContentsDirty();

        if (parentVisible && (groupWithContentChange || (isVisible() && opacity.get() > 0))) {
            if (dirtyRect == null) {
                changed = true;
                dirtyRect = new Recti();
            }
            
            updateTransform();
            
            AffineTransform t = viewTransform;
            
            float w = getWidth();
            float h = getHeight();

            Filter f = getWorkingFilter();
            if (f != null) {
                int fx = f.getX();
                int fy = f.getY();
                if (fx != 0 || fy != 0) {
                    t = new AffineTransform(t);
                    t.translate(fx, fy);
                }
                w = f.getWidth();
                h = f.getHeight();
            }
            
            changed |= t.getBounds(w, h, dirtyRect);
        }
        else {
            changed = (getDirtyRect() != null);
            clearDirtyRect();
        }
        
        return changed;
    }

    /* package private */ void clearDirty() {
        dirty = false;
        contentsDirty = false;
    }

    /**
     Returns true if the contents of this View are dirty, the transform has changed, or the
     filter (if any) has changed.
    */
    public boolean isDirty() {
        return dirty || (filter != null && filter.isDirty());
    }

    /**
     Flags this View has having "dirty" content, that needs to be redrawn.
     */
    public void setContentsDirty() {
        contentsDirty = true;
        if (filter != null) {
            filter.setDirty();
        }
        dirty = true;
    }

    /**
     Returns true if this View has dirty content.
     */
    public boolean getContentsDirty() {
        return contentsDirty;
    }

    /* package private */ void setTransformDirty() {
        transformDirty = true;
        dirty = true;
    }

    /* package-private */ boolean isTransformDirty() {
        if (transformDirty) {
            return true;
        }
        else if (superview == null) {
            return false;
        }
        else {
            return (superviewTransformModCount != superview.getTransformModCount() ||
                superview.isTransformDirty());
        }
    }

    /* package-private */ AffineTransform getViewTransform() {
        updateTransform();
        return viewTransform;
    }

    /* package-private */ AffineTransform getDrawTransform() {
        updateTransform();
        return drawTransform != null ? drawTransform : viewTransform;
    }
    
    private AffineTransform getSuperviewViewTransform() {
        if (superview == null) {
            return IDENTITY;
        }
        else {
            return superview.getViewTransform();
        }
    }

    private AffineTransform getSuperviewDrawTransform() {
        if (superview == null) {
            return IDENTITY;
        }
        else if (superview.hasBackBuffer()) {
            return superview.getBackBufferTransform();
        }
        else {
            return superview.getDrawTransform();
        }
    }
    
    /* package-private */ void updateTransform(AffineTransform superviewTransform,
        AffineTransform transform)
    {
        transform.set(superviewTransform);
            
        // Translate
        transform.translate(x.get(), y.get());
        
        // Rotate
        if (cosAngle != 1 || sinAngle != 0) {
            transform.rotate(cosAngle, sinAngle);
        }
        
        // Scale
        if (scaleX.get() != 1 || scaleY.get() != 1) {
            transform.scale(scaleX.get(), scaleY.get());
        }
        
        // Adjust for anchor
        float anchorLocalX = anchorX.get() * getWidth();
        float anchorLocalY = anchorY.get() * getHeight();

        transform.translate(-anchorLocalX, -anchorLocalY);

        // Snap to the nearest integer location
        if (isPixelSnapping()) {
            transform.roundTranslation();
        }
    }
    
    /**
        Gets the bounds relative to the parent.
    */
    /* package-private */ void getRelativeBounds(Recti bounds) {
        AffineTransform transform = new AffineTransform();
        updateTransform(null, transform);
        transform.getBounds(getWidth(), getHeight(), bounds);
    }
    
    /* package-private */ void updateTransform() {
        if (isTransformDirty()) {
            AffineTransform pvt = getSuperviewViewTransform();
            AffineTransform pdt = getSuperviewDrawTransform();
            updateTransform(pvt, viewTransform);
            if (pvt.equals(pdt)) {
                drawTransform = null;
            }
            else {
                if (drawTransform == null) {
                    drawTransform = new AffineTransform();
                }
                updateTransform(pdt, drawTransform);
            }

            // Keep track of dirty state
            transformDirty = false;
            if (superview != null) {
                // Must happen after getParentTransform()
                superviewTransformModCount = superview.getTransformModCount();
            }
            if (this instanceof Group) {
                ((Group)this).updateTransformModCount();
            }
        }
    }

    /**
        Sets the image filter for this View.
        If this View is a Group with no backbuffer, a backbuffer is created.
        The default filter is {@code null}.
        <p>
        If the specified filter is already attached to a View, a clone of it is created.
        @see #getFilter()
    */
//    public void setFilter(Filter filter) {
//        Filter currFilter = getFilter();
//        Filter newFilter = filter;
//        if (currFilter != newFilter) {
//            if (currFilter != null) {
//                currFilter.setInput(null);
//                markAsUnused(currFilter);
//                this.filter = null;
//            }
//
//            setDirty(true);
//
//            if (newFilter != null) {
//                newFilter = copyIfUsed(newFilter);
//                markAsUsed(newFilter);
//                this.filter = new ViewFilter(this, newFilter);
//            }
//            }
//    }
//
//    private Filter copyIfUsed(Filter filter) {
//        if (filter != null && usedFilters.containsKey(filter)) {
//            filter = filter.copy();
//        }
//        if (filter instanceof FilterChain) {
//            FilterChain chain = (FilterChain)filter;
//            for (int i = 0; i < chain.size(); i++) {
//                chain.set(i, copyIfUsed(chain.get(i)));
//            }
//        }
//        return filter;
//    }
//
//    private Filter markAsUsed(Filter filter) {
//        if (filter != null) {
//            usedFilters.put(filter, null);
//        }
//        if (filter instanceof FilterChain) {
//            FilterChain chain = (FilterChain)filter;
//            for (int i = 0; i < chain.size(); i++) {
//                markAsUsed(chain.get(i));
//            }
//        }
//        return filter;
//    }
//
//    private Filter markAsUnused(Filter filter) {
//        if (filter != null) {
//            usedFilters.remove(filter);
//        }
//        if (filter instanceof FilterChain) {
//            FilterChain chain = (FilterChain)filter;
//            for (int i = 0; i < chain.size(); i++) {
//                markAsUnused(chain.get(i));
//            }
//        }
//        return filter;
//    }
//
//    /**
//        Gets the image filter for this View, or null if there is no filter.
//        @see #setFilter(pulpcore.image.filter.Filter)
//    */
//    public Filter getFilter() {
//        if (filter != null) {
//            return filter.getFilter();
//        }
//        else {
//            return null;
//        }
//    }
//
    public void setFilter(Filter filter) {
        if (this.filter != filter) {
            this.filter = filter;
            setContentsDirty();
        }
    }

    public Filter getFilter() {
        return null;
    }
    
    /* package-private */ Filter getWorkingFilter() {
        return null;
    }

//    /* package-private */ Filter getWorkingFilter() {
//        Filter f = getFilter();
//        if (f != null) {
//            // Make sure input is up to date
//            f.setInput(filter.getCacheImage());
//        }
//        return f;
//    }
//
//    private static class ViewFilter {
//        private final View view;
//        private final Filter filter;
//        private Image cache = null;
//        private boolean cacheDirty = true;
//
//        public ViewFilter(View view, Filter filter) {
//            this.view = view;
//            this.filter = filter;
//            this.filter.setDirty();
//        }
//
//        public Filter getFilter() {
//            return filter;
//        }
//
//        private int getCacheWidth() {
//            if (view instanceof Group) {
//                // Assume Group has a backbuffer if it has a Filter.
//                // See Group#needsBackBuffer()
//                return ((Group)view).getBackBuffer().getWidth();
//            }
//            else {
//                return (int)Math.ceil(view.getContentWidth());
//            }
//        }
//
//        private int getCacheHeight() {
//            if (view instanceof Group) {
//                // Assume Group has a backbuffer if it has a Filter.
//                // See Group#needsBackBuffer()
//                return ((Group)view).getBackBuffer().getHeight();
//            }
//            else {
//                return (int)Math.ceil(view.getContentHeight());
//            }
//        }
//
//        public void setDirty() {
//            filter.setDirty();
//            cacheDirty = true;
//        }
//
//        public Image getCacheImage() {
//            if (view instanceof ImageView) {
//                return ((ImageView)view).getImage();
//            }
//            else if (view instanceof Group) {
//                // Update the back buffer
//                cache = null;
//                if (cacheDirty) {
//                    view.drawView(null);
//                    cacheDirty = false;
//                }
//                return ((Group)view).getBackBuffer();
//            }
//            else {
//                int w = getCacheWidth();
//                int h = getCacheHeight();
//                boolean isOpaque = view.isOpaque();
//                boolean needsClear = true;
//                if (cache == null ||
//                    cache.getWidth() != w ||
//                    cache.getHeight() != h ||
//                    cache.isOpaque() != isOpaque)
//                {
//                    cache = new CoreImage(w, h, isOpaque);
//                    //pulpcore.CoreSystem.print("New View cache: " + w + "x" + h);
//                    cacheDirty = true;
//                    needsClear = false;
//                }
//                if (cacheDirty) {
//                    //pulpcore.CoreSystem.print("View re-cached");
//                    Graphics g = cache.createGraphics();
//                    if (needsClear) {
//                        g.clear();
//                    }
//                    view.drawView(g);
//                    cacheDirty = false;
//                }
//                return cache;
//            }
//        }
//    }

    /**
        On a property change this View is marked as dirty.
    */
    @Override
    public void onPropertyChange(Property property) {
        if (property == backgroundColor || property == opacity) {
            setContentsDirty();
        }
        else {
            // x, y, scaleX, scaleY, anchorX, anchorY, angle
            setTransformDirty();
            if (property == angle) {
                cosAngle = (float)Math.cos(angle.get());
                sinAngle = (float)Math.sin(angle.get());
            }
        }
    }

    /**
        Draws the View.
     */
    /* package private */ void render(Graphics g) {
        if (!isVisible()) {
            return;
        }

        // Set alpha
        int newAlpha = (int)(opacity.get() * 255);
        int oldAlpha = g.getAlpha();
        if (oldAlpha != 255) {
            newAlpha = (newAlpha * oldAlpha) >> 8;
        }
        if (newAlpha <= 0) {
            return;
        }

        if (transformDirty) {
            updateTransform();
        }
        
        g.setAlpha(newAlpha);
        
        // Set blend mode
        BlendMode oldBlendMode = g.getBlendMode();
        if (blendMode != null) {
            g.setBlendMode(blendMode);
        }
        
        // Set transform
        AffineTransform t = getDrawTransform();
        Filter f = getWorkingFilter();
        if (f != null) {
            int fx = f.getX();
            int fy = f.getY();
            if (fx != 0 || fy != 0) {
                t = new AffineTransform(t);
                t.translate(fx, fy);
            }
        }

        // Set transform
        g.pushTransform();
        g.setTransform(t);

        // Draw
        int oldEdgeClamp = g.getEdgeClamp();
        if (f != null) {
            // TODO: Filters (does not take into account the fillColor)
//            if (this instanceof ImageView) {
//                // Respect the antiAlias setting
//                boolean antiAlias = ((ImageView)this).isAntiAliased();
//                g.setEdgeClamp(antiAlias ? Graphics.EDGE_CLAMP_NONE : Graphics.EDGE_CLAMP_ALL);
//            }
//            g.drawImage(f.getOutput());
        }
        else {
            onRender(g);
        }
        
        // Undo changes
        g.popTransform();
        g.setAlpha(oldAlpha);
        g.setBlendMode(oldBlendMode);
        g.setEdgeClamp(oldEdgeClamp);

        // All done!
        clearDirty();
    }
    
    /**
        Draws the view. The graphic context's alpha is set to this view's alpha,
        and it's transform is set to this View's transform.
        This method is not called if the view is not visible or it's alpha
        is less than or equal to zero.
        <p>
        This method may be called multiple times for each dirty rectangle. The clip of the
        graphics context will be set to the current dirty rectangle.
        <p>
        When the contents of this view change (in another words, the graphic output of this method
        will be different from the last time it is called), subclasses should call
        {@code setDirty(true)}.
        <p>
        Implementors should not save a reference to the graphics context as it can change
        between calls to this method.
        <p>
        By default, this method draws the background fill color.
    */
    protected void onRender(Graphics g) {
        int fillARGB = backgroundColor.get();
        if (!Color.isTransparent(fillARGB)) {
            g.setColor(fillARGB);
            g.fillRect(width, height);
        }
    }
    
    /**
        Gets the integer x-coordinate in Local Space of the specified location in
        World Space. Returns Float.NaN if the Local Space is invalid
        (that is, it's Transform determinant is zero).
    */
    public float getLocalX(float worldX, float worldY) {
        updateTransform();
        return viewTransform.inverseTransformX(worldX, worldY);
    }
    
    /**
        Gets the integer y-coordinate in Local Space of the specified location in
        World Space. Returns Float.NaN if the Local Space is invalid
        (that is, it's Transform determinant is zero).
    */
    public float getLocalY(float worldX, float worldY) {
        updateTransform();
        return viewTransform.inverseTransformY(worldX, worldY);
    }
    
    /**
        Gets the x-coordinate of this View in World Space.
    */
    public float getWorldX() {
        updateTransform();
        return viewTransform.getTranslateX();
    }
    
    /**
        Gets the y-coordinate of this View in World Space.
    */
    public float getWorldY() {
        updateTransform();
        return viewTransform.getTranslateY();
    }
    
    /**
        Gets the x-coordinate in World Space of the specified location in
        Local Space.
    */
    public float getWorldX(float localX, float localY) {
        updateTransform();
        return viewTransform.transformX(localX, localY);
    }
    
    /**
        Gets the y-coordinate in View Space of the specified location in
        Local Space.
    */
    public float getWorldY(float localX, float localY) {
        updateTransform();
        return viewTransform.transformY(localX, localY);
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        View.
            
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
        @return true if the specified point is within the bounds of this View.
    */
    public boolean contains(float worldX, float worldY) {
        updateTransform();
        float localX = viewTransform.inverseTransformX(worldX, worldY);
        float localY = viewTransform.inverseTransformY(worldX, worldY);
        
        if (localX == Float.NaN || localY == Float.NaN) {
            return false;
        }
        
        if (localX >= 0 && localX < getWidth() &&
            localY >= 0 && localY < getHeight())
        {
            if (getPixelLevelChecks()) {
                return !isTransparent((int)Math.floor(localX), (int)Math.floor(localY));
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }
    
    /**
        Checks if the pixel at the specified integer location is transparent. This method does not
        check if this View is enabled or visible, nor does it check its alpha value.
        <p>
        The default implementation always returns true if the backgroundColor is transparent.
        Subclasses of this class may need to override this method to return accurate results.
        <p>
        This method is called from {@link #contains(int,int)}.
        @param localX integer x-coordinate in local space
        @param localY integer y-coordinate in local space
    */
    protected boolean isTransparent(int localX, int localY) {
        return Color.isTransparent(backgroundColor.get());
    }

    /**
        Checks if the specified location is within the bounds of this
        View and this View is the top-most View at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public boolean isPick(float worldX, float worldY) {
        return isPick(worldX, worldY, false);
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        View and this View is the top-most View at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public boolean isPick(float worldX, float worldY, boolean allowDisabledViews) {
        if (contains(worldX, worldY)) {
            // Since the location is within the View, root.pick() won't search below this
            // View in the scene graph
            Group root = getRoot();
            return (root == null || root.pick(worldX, worldY, allowDisabledViews) == this);
        }
        else {
            return false;
        }
    }
    
    /**
        Checks if the specified View intersects this View. This method checks if this
        View's OBB (oriented bounding box) intersects with the specified View's OBB.
        The OBBs can be parallelograms in some cases.
        <p>
        The two Views do no have to be in the same Group.
        @param view the View to test against.
        @return true if the two Views' OBBs intersect.
    */
    public boolean intersects(View view) {
        View a = this;
        View b = view;
        AffineTransform at = a.getViewTransform();
        AffineTransform bt = b.getViewTransform();
        float aw = a.getWidth();
        float ah = a.getHeight();
        float bw = b.getWidth();
        float bh = b.getHeight();
        boolean pixelLevel = a.getPixelLevelChecks() || b.getPixelLevelChecks();
        
        // First, test the bounding box of the two sprites
        Rectf ab = at.getBounds(aw, ah);
        Rectf bb = bt.getBounds(bw, bh);
        if (!ab.intersects(bb)) {
            return false;
        }
        
        // If the transforms aren't rotated, no further tests are needed
        if ((at.getType() & AffineTransform.TYPE_ROTATE) == 0 &&
            (bt.getType() & AffineTransform.TYPE_ROTATE) == 0)
        {
            if (pixelLevel) {
                ab.intersection(bb);
                return isPixelLevelCollision(b, ab);
            }
            else {
                return true;
            }
        }
        
        // One or both View are rotated. Use the separating axis theorem on the two
        // View's OBB (which is actually a parallelogram)
      
        // Step 1: Get View A's points
        Tuple2f[] ap = {
            new Tuple2f(0, 0),
            new Tuple2f(aw, 0),
            new Tuple2f(aw, ah),
            new Tuple2f(0, ah)
        };
            
        // Step 2: Get View B's points and convert them to View A's local space
        Tuple2f[] bp = {
            new Tuple2f(0, 0),
            new Tuple2f(bw, 0),
            new Tuple2f(bw, bh),
            new Tuple2f(0, bh)
        };
        for (int i = 0; i < 4; i++) {
            Tuple2f p = bp[i];
            bt.transform(p);
            boolean success = at.inverseTransform(p);
            if (!success) {
                return false;
            }
        }
        
        // Step 3: Get perpendiculars of each edge
        Tuple2f[] perps = {
            new Tuple2f(ap[1].y - ap[0].y, ap[0].x - ap[1].x),
            new Tuple2f(ap[3].y - ap[0].y, ap[0].x - ap[3].x),
            new Tuple2f(bp[1].y - bp[0].y, bp[0].x - bp[1].x),
            new Tuple2f(bp[3].y - bp[0].y, bp[0].x - bp[3].x)
        };
        float[] perpLengths = {
            aw,
            ah,
            perps[2].length(),
            perps[3].length()
        };
        
        // Step 4: Project points onto each perpendicular.
        // For each perpendicular, the span of projected points from View A must intersect
        // the span of projected points from View B
        for (int i = 0; i < perps.length; i++) {
            float amin = Float.MAX_VALUE;
            float amax = Float.MIN_VALUE;
            float bmin = Float.MAX_VALUE;
            float bmax = Float.MIN_VALUE;
            float len = perpLengths[i];
            Tuple2f p = perps[i];
            
            if (len <= 0) {
                return false;
            }
            for (int j = 0; j < ap.length; j++) {
                float v = p.dot(ap[j]) / len;
                if (v < amin) {
                    amin = v;
                }
                if (v > amax) {
                    amax = v;
                }
            }
            for (int j = 0; j < bp.length; j++) {
                float v = p.dot(bp[j]) / len;
                if (v < bmin) {
                    bmin = v;
                }
                if (v > bmax) {
                    bmax = v;
                }
            }
            if (amax < bmin || amin > bmax) {
                return false;
            }
        }
        
        if (pixelLevel) {
            // TODO: better intersection bounds for rotated Views?
            ab.intersection(bb);
            return isPixelLevelCollision(b, ab);
        }
        else {
            return true;
        }
    }
    
    private boolean isPixelLevelCollision(View view, Rectf intersection) {
        View a = this;
        View b = view;
        float x1 = intersection.x;
        float y1 = intersection.y;
        float x2 = intersection.x + intersection.width;
        float y2 = intersection.y + intersection.height;
        
        for (float py = y1; py < y2; py++) {
            for (float px = x1; px < x2; px++) {
                if (a.contains(px, py) && b.contains(px, py)) {
                    return true;
                }
            }
        }
        return false;
    }
}
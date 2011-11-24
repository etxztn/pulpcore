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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Graphics.BlendMode;
import org.pulpcore.graphics.Image;
import org.pulpcore.graphics.filter.Filter;
import org.pulpcore.math.AffineTransform;
import org.pulpcore.math.Recti;
import org.pulpcore.math.Tuple2f;
import org.pulpcore.util.Objects;

/**
    A container of Views.
*/
public class Group extends View implements Iterable<View> {

    private static class BackBuffer {

        private Image image;
        private AffineTransform transform = null;
        private final Tuple2f[] transformedClip = new Tuple2f[4];
        private boolean requested = false;
        private BlendMode blendMode = BlendMode.SRC_OVER;

        public BackBuffer() {
            for (int i = 0; i < transformedClip.length; i++) {
                transformedClip[i] = new Tuple2f();
            }
        }

        public boolean set(int w, int h) {
            if (image == null || image.getWidth() != w || image.getHeight() != h) {
                image = new Image(w, h);
                return true;
            }
            else {
                return false;
            }
        }
    }

    /** Immutable list of child views. A new array is created when the list changes. */
    private View[] subviews = new View[0];
    /** The list of subviews at the last call to getRemovedViews() */
    private View[] previousSubviews = null;

    /** Used for children to check if this Group's transform has changed since the last update */
    private int transformModCount = 0;
    
    private boolean clipSubviewsToBounds = false;

    private BackBuffer backBuffer = null;
    
    public Group() {
        this(0, 0);
    }
    
    public Group(float width, float height) {
        super(width, height);
    }
    
    /* package-private */ int getTransformModCount() {
        return transformModCount;
    }
    
    /* package-private */ void updateTransformModCount() {
        transformModCount++;
    }

    @Override
    /* package private */ void tick(float dt) {
        super.tick(dt);
        View[] snapshot = subviews;
        for (View view : snapshot) {
            view.tick(dt);
        }

        if (isClippedToBounds()) {
            updateBackBuffer();
        }
    }

    /**
        Sets whether this Group clips its child Sprites to the bounds of this Group. Note that
        in order to achieve the clip on scaled or rotated Groups, a backbuffer will be created.
        The default value is false.
     */
    public void setClippedToBounds(boolean clipToBounds) {
        if (this.clipSubviewsToBounds != clipToBounds) {
            this.clipSubviewsToBounds = clipToBounds;
            updateBackBuffer();
        }
    }

    /**
        Returns true if this Group clips its child Sprites to the bounds of this Group. Note that
        even if this method returns false, the Group is also clipped if it has a backbuffer.
        The default value is false.
     */
    public boolean isClippedToBounds() {
        return clipSubviewsToBounds;
    }

    /**
        Sets whether a back buffer is requested for this Group.
     */
    public void setBackBuffered(boolean backBuffered) {
        if (isBackBuffered() != backBuffered) {
            if (backBuffer == null) {
                backBuffer = new BackBuffer();
            }
            backBuffer.requested = backBuffered;
            updateBackBuffer();
        }
    }

    /**
        Returns whether a back buffer is requested for this Group. Note that a Group may have a
        back buffer even if this method returns false. To check if a Group has a back buffer
        for any reason, call {@link #hasBackBuffer() }.
     */
    public boolean isBackBuffered() {
        return backBuffer == null ? false : backBuffer.requested;
    }

    /**
        Gets this Group's back buffer. A Group has a back buffer if
        {@link #isBackBuffered() } is true, the Group has a filter, or if
        {@link #isClippedToBounds() } is true and a back buffer is required to clip.
     */
    public Image getBackBuffer() {
        return backBuffer == null ? null : backBuffer.image;
    }

    /**
        Checks if this Group has a back buffer. A Group has a back buffer if
        {@link #isBackBuffered() } is true, the Group has a filter, or if
        {@link #isClippedToBounds() } is true and a back buffer is required to clip.
        @return true if this Group has a back buffer.
    */
    public boolean hasBackBuffer() {
        return backBuffer != null && backBuffer.image != null;
    }

    /**
        Sets this Group's blend mode for rendering onto its back buffer.
        @param backBufferBlendMode the blend mode.
    */
    public void setBackBufferBlendMode(BlendMode backBufferBlendMode) {
        if (backBufferBlendMode == null) {
            backBufferBlendMode = BlendMode.SRC_OVER;
        }
        if (getBackBufferBlendMode() != backBufferBlendMode) {
            if (backBuffer == null) {
                backBuffer = new BackBuffer();
            }
            backBuffer.blendMode = backBufferBlendMode;
            if (backBuffer.image != null) {
                backBufferChanged();
            }
        }
    }

    /**
        Gets this Group's blend mode for rendering onto its back buffer.
        @return the blend mode.
    */
    public BlendMode getBackBufferBlendMode() {
        return backBuffer == null ? BlendMode.SRC_OVER : backBuffer.blendMode;
    }

    @Override
    public void setFilter(Filter filter) {
        Filter currFilter = getFilter();
        if (currFilter != filter) {
            super.setFilter(filter);
            updateBackBuffer();
            // I'm not sure why this is needed. I found one case where it is needed,
            // but I'm not sure why.
            // To reproduce:
            // 1. Run BackBufferTest
            // 2. Click "Blue filter"
            // 3. Click "White filter"
            // 4. Notice white square is incorrectly offset.
            setSubviewsDirty();
        }
    }
    
    //
    // Subview queries
    //

    /**
        Returns an Iterator of the this Group's subviews (in proper sequence). The iterator
        provides a snapshot of the state of the list when the iterator was constructed. 
        No synchronization is needed while traversing the iterator. 
        The iterator does NOT support the {@code remove} method - use
        {@link #removeFromSuperview() instead}.
        @return The iterator.
    */
    @Override
    public Iterator<View> iterator() {
        final View[] snapshot = subviews;
        return new Iterator<View>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < snapshot.length);
            }

            @Override
            public View next() {
                if (hasNext()) {
                    return snapshot[i++];
                }
                else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    /**
        Returns the number of children in this group. This includes child groups but not
        the children of those groups.
    */
    public int size() {
        return subviews.length;
    }
    
    /**
        Returns the sprite at the specified position in this group.
        @throws ArrayIndexOutOfBoundsException if the position is invalid
    */
    public View getSubview(int index) {
        return subviews[index];
    }
    
    /**
        Returns {@code true} if this Group contains the specified View. 
    */
    public boolean contains(View sprite) {
        return indexOf(subviews, sprite) != -1;
    }
    
    /**
    Returns {@code true} if this Group is an ancestor of the specified View (or if the View
    is this Group).
    */
    public boolean isAncestorOf(View view) {
        while (view != null) {
            if (view == this) {
                return true;
            }
            else {
                view = view.getSuperview();
            }
        }
        return false;
    }

    /**
        Finds the View whose tag is equal to the specified tag (using
        {@code Objects.equal(tag, view.getTag())}. Returns null 
        if no View with the specified tag is found.
    */
    public View findViewWithTag(Object tag) {
        if (Objects.equal(tag, this.getTag())) {
            return this;
        }
        View[] snapshot = subviews;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            View child = snapshot[i];
            if (child instanceof Group) {
                child = ((Group)child).findViewWithTag(tag);
                if (child != null) {
                    return child;
                }
            }
            else if (Objects.equal(tag, child.getTag())) {
                return child;
            }
        }
        return null;
    }

    /**
        Finds the top-most sprite at the specified location, or null if none is found.
        All sprites in this Group and any child Groups are searched until a sprite is found.
        @param viewX x-coordinate in view space.
        @param viewY y-coordinate in view space.
        @return The top-most sprite at the specified location, or null if none is found.
    */
    public View pick(float worldX, float worldY) {
        return pick(worldX, worldY, false);
    }
    
    /**
        Finds the top-most View at the specified location that is visible and has an opacity
        greater than zero, or null if none is found.
        All Views in this Group and any child Groups are searched until a sprite is found.
        @param worldX x-coordinate in world space.
        @param worldY y-coordinate in world space.
        @param allowDisabledViews If true, disabled views may be picked.
        @return The top-most sprite at the specified location, or null if none is found.
    */
    public View pick(float worldX, float worldY, boolean allowDisabledViews) {
        boolean clipped = isClippedToBounds() || hasBackBuffer();
        if (clipped) {
            float lx = getLocalX(worldX, worldY);
            float ly = getLocalY(worldX, worldY);
            float lw = getWidth();
            float lh = getHeight();
            if (lx < 0 || ly < 0 || lx >= lw || ly >= lh) {
                return null;
            }
        }
        View[] snapshot = subviews;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            View view = snapshot[i];
            if (view.isVisible() && view.opacity.get() > 0 &&
                    (allowDisabledViews || view.isEnabled())) {
                if (view instanceof Group) {
                    View subview = ((Group)view).pick(worldX, worldY, allowDisabledViews);
                    if (subview != null) {
                        return subview;
                    }
                }
                // TODO: Should this be an option somewhere?
                // Currently allowing a Group to be picked, even if it's subviews aren't picked.
                if (view.contains(worldX, worldY)) {
                    return view;
                }
            }
        }
        return null;
    }
    
    /**
        Checks if the specified location is within the bounds of this
        Group and this Group is an ancestor of the top-most Sprite at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    @Override
    public boolean isPick(float worldX, float worldY, boolean allowDisabledViews) {
        if (contains(worldX, worldY)) {
            // Since the location is within the sprite, root.pick() won't search below this
            // sprite in the scene graph
            Group root = getRoot();
            return (root == null || this.isAncestorOf(root.pick(worldX, worldY, allowDisabledViews)));
        }
        else {
            return false;
        }
    }
    
    /**
        Returns the number of subviews in this group and all of it's subgroups.
    */
    public int countSubviews() {
        View[] snapshot = subviews;
        int count = 0;
        for (View view : snapshot) {
            count++;
            if (view instanceof Group) {
                count += ((Group)view).countSubviews();
            }
        }
        return count;
    }
    
    /**
        Returns the number of visible subviews in this group and all of it's subgroups.
    */
    public int countVisibleSubviews() {
        View[] snapshot = subviews;
        if (!isVisible() || opacity.get() <= 0) {
            return 0;
        }
        
        int count = 0;
        for (View view : snapshot) {
            if (view.isVisible() && view.opacity.get() > 0) {
                count++;
                if (view instanceof Group) {
                    count += ((Group)view).countVisibleSubviews();
                }
            }
        }
        return count;
    }
    
    //
    // View list modifications
    // NOTE: if adding another modification method, also add it to Viewport and ScrollPane
    //
    
    /**
        Adds a Sprite to this Group. The Sprite is added so it appears above all other sprites in
        this Group. If this Sprite already belongs to a Group, it is first removed from that 
        Group before added to this one.
        @return The sprite
    */
    public View addSubview(View view) {
        if (view != null) {
            view.removeFromSuperview();
            View[] snapshot = subviews;
            subviews = add(snapshot, view, snapshot.length);
            view.setSuperview(this);
        }
        return view;
    }

    public void addSubviews(View... views) {
        View[] snapshot = subviews;
        for (View view : views) {
            view.removeFromSuperview();
            snapshot = add(snapshot, view, snapshot.length);
            view.setSuperview(this);
        }
        subviews = snapshot;
    }

    public void addSubviews(Collection<View> views) {
        View[] snapshot = subviews;
        for (View view : views) {
            view.removeFromSuperview();
            snapshot = add(snapshot, view, snapshot.length);
            view.setSuperview(this);
        }
        subviews = snapshot;
    }
    
    /**
        Inserts a Sprite to this Group at the specified position. The Sprite at the current
        position (if any) and any subsequent Sprites are moved up in the z-order
        (adds one to their indices).
        <p>
        If the index is less than zero, the sprite is inserted at position zero (the bottom in the 
        z-order).
        If the index is greater than or equal to {@link #size()}, the sprite is inserted at 
        position {@link #size()} (the top in the z-order).
        @return The sprite
    */
    public View addSubview(int index, View view) {
        if (view != null) {
            view.removeFromSuperview();
            View[] snapshot = subviews;
            subviews = add(snapshot, view, index);
            view.setSuperview(this);
        }
        return view;
    }
    
    /**
        Removes a Sprite from this Group.
    */
    public void removeSubview(View view) {
        if (view != null) {
            View[] snapshot = subviews;
            int index = indexOf(snapshot, view);
            if (index != -1) {
                subviews = remove(snapshot, index);
                view.setSuperview(null);
            }
        }
    }
    
    /**
        Removes all Views from this Group.
    */
    public void removeAllSubviews() {
        View[] snapshot = subviews;
        for (View view : snapshot) {
            view.setSuperview(null);
        }
        subviews = new View[0];
    }
    
    private void move(View view, int position, boolean relative) {
        View[] snapshot = subviews;
        int oldPosition = indexOf(snapshot, view);
        if (oldPosition != -1) {
            if (relative) {
                position += oldPosition;
            }
            if (position < 0) {
                position = 0;
            }
            else if (position > snapshot.length - 1) {
                position = snapshot.length - 1;
            }
            if (oldPosition != position) {
                snapshot = remove(snapshot, oldPosition);
                subviews = add(snapshot, view, position);
                view.setContentsDirty();
            }
        }
    }
    
    /**
        Moves the specified Sprite to the top of the z-order, so that all the other Sprites 
        currently in this Group appear underneath it. If the specified Sprite is not in this Group,
        or the Sprite is already at the top, this method does nothing.
    */
    public void moveToTop(View view) {
        move(view, Integer.MAX_VALUE, false);
    }
    
    /**
        Moves the specified Sprite to the bottom of the z-order, so that all the other Sprites 
        currently in this Group appear above it. If the specified Sprite is not in this Group,
        or the Sprite is already at the bottom, this method does nothing.
    */
    public void moveToBottom(View view) {
        move(view, 0, false);
    }
    
    /**
        Moves the specified Sprite up in z-order, swapping places with the first Sprite that 
        appears above it. If the specified Sprite is not in this Group, or the Sprite is already
        at the top, this method does nothing.
    */
    public void moveUp(View view) {
        move(view, +1, true);
    }
    
    /**
        Moves the specified Sprite down in z-order, swapping places with the first Sprite that 
        appears below it. If the specified Sprite is not in this Group, or the Sprite is already
        at the bottom, this method does nothing.
    */
    public void moveDown(View view) {
        move(view, -1, true);
    }
    
    /**
        Gets a list of all of the Sprites in this Group that were
        removed since the last call to this method.
        <p>
        This method is used by Scene2D to implement dirty rectangles.
    */
    /* package private */ ArrayList<View> getRemovedViews() {
        ArrayList<View> removedSprites = null;
        Filter f = getWorkingFilter();
        if (previousSubviews == null) {
            // First call - no remove notifications needed
            previousSubviews = subviews;
        }
        else if (previousSubviews != subviews) {
            // Modifications occurred - get list of all removed sprites.
            // NOTE: we make the list here, rather than in remove(), because if the list was
            // creating in remove() and this method was never called (non-Scene2D implementation)
            // the removedSprites list would continue to grow, resulting in a memory leak.
            for (int i = 0; i < previousSubviews.length; i++) {
                if (previousSubviews[i].getSuperview() != this) {
                    if (removedSprites == null) {
                        removedSprites = new ArrayList<View>();
                    }
                    removedSprites.add(previousSubviews[i]);
                }
            }
            previousSubviews = subviews;
        }
        return removedSprites;
    }
    
    /**
        Packs this group so that its bounds (x, y, width, and height) match the area covered by
        its children.
        If this Group has a back buffer, the back buffer is resized if necessary.
    */
    public void pack() {
        View[] snapshot = subviews;
        float newContentWidth = 0;
        float newContentHeight = 0;
        
        if (snapshot.length > 0) {
            // Integers
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            Recti bounds = new Recti();
            
            for (View view : snapshot) {
                if (view instanceof Group) {
                    ((Group)view).pack();
                }
                view.getRelativeBounds(bounds);
                minX = Math.min(minX, bounds.x);
                minY = Math.min(minY, bounds.y);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
            newContentWidth = maxX - minX;
            newContentHeight = maxY - minY;
            if (minX != 0) {
                for (View view : snapshot) {
                    view.x.set(view.x.get() - minX);
                }
                this.x.set(this.x.get() + minX);
                // TODO: should anchorX be changed?
            }
            if (minY != 0) {
                for (View view : snapshot) {
                    view.y.set(view.y.get() - minY);
                }
                this.y.set(this.y.get() + minY);
                // TODO: should anchorY be changed?
            }
        }
        setSize(newContentWidth, newContentHeight);
        if (backBuffer != null && backBuffer.image != null) {
            createBackBufferImpl();
        }
        setContentsDirty();
    }
    
    //
    // Back buffers
    //
    
    /* package-private */ AffineTransform getBackBufferTransform() {
        return (getFilter() != null || backBuffer == null) ? View.IDENTITY : backBuffer.transform;
    }

    private boolean needsBackBuffer() {
        if (isBackBuffered() || getFilter() != null) {
            return true;
        }
        else if (isClippedToBounds()) {
            AffineTransform t = getDrawTransform();
            return !(t.getType() == AffineTransform.TYPE_IDENTITY ||
                    t.getType() == AffineTransform.TYPE_TRANSLATE);
        }
        else {
            return false;
        }
    }

    private void updateBackBuffer() {
        if (needsBackBuffer()) {
            if (backBuffer == null || backBuffer.image == null) {
                createBackBufferImpl();
            }
        }
        else {
            if (backBuffer != null && backBuffer.image != null) {
                removeBackBufferImpl();
            }
        }
    }

    private void createBackBufferImpl() {
        AffineTransform t = new AffineTransform();
        float w = getWidth();
        float h = getHeight();
        int backBufferWidth = (int)Math.ceil(w);
        int backBufferHeight = (int)Math.ceil(h);
        if (backBuffer == null) {
            backBuffer = new BackBuffer();
        }
        if (backBuffer.set(backBufferWidth, backBufferHeight)) {
            backBufferChanged();
        }
        if (backBuffer.transform == null || !backBuffer.transform.equals(t)) {
            backBuffer.transform = t;
            backBufferChanged();
        }
    }

    private void removeBackBufferImpl() {
        if (backBuffer != null && backBuffer.image != null) {
            backBuffer.image = null;
            backBuffer.transform = null;
            backBufferChanged();
        }
    }

    private void backBufferChanged() {
        setContentsDirty();
        setTransformDirty();
    }
        
    /* package-private */ void setSubviewsDirty() {
        setContentsDirty();
        View[] snapshot = subviews;
        for (View view : snapshot) {
            if (view instanceof Group) {
                ((Group)view).setSubviewsDirty();
            }
            else {
                view.setContentsDirty();
            }
        }
    }

    // g may be null if this Group has a Filter
    @Override
    protected void onRender(Graphics g) {
        View[] snapshot = subviews;

        if (backBuffer == null || backBuffer.image == null) {
            if (isBackBuffered()) {
                System.out.println("Here is yer problem");
            }
            Recti oldClip = null;
            boolean setClip = isClippedToBounds();

            if (setClip) {
                AffineTransform t = g.getTransform();
                oldClip = g.getClip();
                Recti newClip = new Recti();
                // TODO: Old version used width/height, not contentWidth/contentHeight.
                // Which is correct?
                t.getBounds(getWidth(), getHeight(), newClip);
                g.clipRect(newClip);
            }

            super.onRender(g);

            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].render(g);
            }

            if (setClip) {
                g.setClip(oldClip);
            }
        }
        else {
            int clipX;
            int clipY;
            int clipW;
            int clipH;
            AffineTransform drawTransform;
            Graphics g2 = backBuffer.image.createGraphics();
            g2.setBlendMode(backBuffer.blendMode);

            if (g == null) {
                clipX = 0;
                clipY = 0;
                clipW = backBuffer.image.getWidth();
                clipH = backBuffer.image.getHeight();
                drawTransform = View.IDENTITY;
            }
            else {
                clipX = g.getClipX();
                clipY = g.getClipY();
                clipW = g.getClipWidth();
                clipH = g.getClipHeight();
                drawTransform = getDrawTransform();
            }

            // Translate the clip rect (device space) to this Group's draw space
            if (drawTransform.getType() != AffineTransform.TYPE_IDENTITY) {
                int numPoints = ((drawTransform.getType() & AffineTransform.TYPE_ROTATE) != 0) ? 4 : 2;

                backBuffer.transformedClip[0].set(clipX, clipY);
                backBuffer.transformedClip[1].set(clipX + clipW, clipY + clipH);
                if (numPoints == 4) {
                    backBuffer.transformedClip[2].set(clipX, clipY + clipH);
                    backBuffer.transformedClip[3].set(clipX + clipW, clipY);
                }

                float x1 = Float.MAX_VALUE;
                float y1 = Float.MAX_VALUE;
                float x2 = Float.MIN_VALUE;
                float y2 = Float.MIN_VALUE;
                for (int i = 0; i < numPoints; i++) {
                    Tuple2f t = backBuffer.transformedClip[i];
                    if (!drawTransform.inverseTransform(t)) {
                        return;
                    }
                    if (t.x < x1) {
                        x1 = t.x;
                    }
                    if (t.y < y1) {
                        y1 = t.y;
                    }
                    if (t.x > x2) {
                        x2 = t.x;
                    }
                    if (t.y > y2) {
                        y2 = t.y;
                    }
                }
                clipX = (int)Math.floor(x1) - 1;
                clipY = (int)Math.floor(y1) - 1;
                clipW = (int)Math.ceil(x2) - clipX + 2;
                clipH = (int)Math.ceil(y2) - clipY + 2;
            }
            g2.setClip(clipX, clipY, clipW, clipH);
            g2.clear();
            super.onRender(g2);
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].render(g2);
            }

            // g will be null if called from a filtered sprite
            if (g != null) {
                boolean backBufferCoversScene = false;
                Scene scene = getScene();
                if (scene != null) {
                    backBufferCoversScene = backBuffer.image.getWidth() >= scene.getWidth() &&
                            backBuffer.image.getHeight() >= scene.getHeight();
                }

                g.setEdgeClamp(backBufferCoversScene ? Graphics.EDGE_CLAMP_ALL : Graphics.EDGE_CLAMP_NONE);

                g.drawImage(backBuffer.image);
            }
        }
    }
    
    //
    // Static convenience methods for working with immutable Sprite arrays
    //
    
    private static int indexOf(View[] snapshot, View s) {
        for (int i = 0; i < snapshot.length; i++) {
            if (s == snapshot[i]) {
                return i;
            }
        }
        return -1;
    }
    
    private static View[] remove(View[] snapshot, int index) {
        if (index >= 0 && index < snapshot.length) {
            View[] newViews = new View[snapshot.length - 1];
            System.arraycopy(snapshot, 0, newViews, 0, index);
            System.arraycopy(snapshot, index + 1, newViews, index,
                newViews.length - index);
            snapshot = newViews;
        }
        return snapshot;
    }
    
    private static View[] add(View[] snapshot, View view, int index) {
        if (index < 0) {
            index = 0;
        }
        else if (index > snapshot.length) {
            index = snapshot.length;
        }
        View[] newViews = new View[snapshot.length + 1];
        System.arraycopy(snapshot, 0, newViews, 0, index);
        newViews[index] = view;
        System.arraycopy(snapshot, index, newViews, index + 1, snapshot.length - index);
        return newViews;
    }

}
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import pulpcore.Build;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;

/**
    A container of Sprites.
*/
public class Group extends Sprite {
    
    /** Immuatable list of sprites. A new array is created when the list changes. */
    private Sprite[] sprites = new Sprite[0];
    /** The list of sprites at the last call to getRemovedSprites() */
    private Sprite[] previousSprites = null;
    
    /** Used for children to check if this Group's transform has changed since the last update */
    private int transformModCount = 0;
    
    private int fNaturalWidth;
    private int fNaturalHeight;
    private int fInnerX;
    private int fInnerY;
    
    public Group() {
        this(0, 0, 0, 0);
    }
    
    public Group(int x, int y) {
        this(x, y, 0, 0);
    }
    
    public Group(int x, int y, int width, int height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    public Group(double x, double y) {
        this(x, y, 0, 0);
    }
    
    public Group(double x, double y, double width, double height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    /* package-private */ int getTransformModCount() {
        return transformModCount;
    }
    
    /* package-private */ void updateTransformModCount() {
        transformModCount++;
    }

    private Object getTreeLock() {
        Object lock = getScene2D();
        if (lock == null) {
            lock = this;
        }
        return lock;
    }
    
    //
    // Sprite list queries
    //
    
    /**
        Returns an Iterator of the Sprites in this Group (in proper sequence). The iterator 
        provides a snapshot of the state of the list when the iterator was constructed. 
        No synchronization is needed while traversing the iterator. 
        The iterator does NOT support the {@code remove} method.
        @return The iterator.
    */
    public Iterator iterator() {
        return Collections.unmodifiableList(Arrays.asList(sprites)).iterator();
    }
    
    /**
        Returns the number of sprites in this group. This includes child groups but not
        the children of those groups.
    */
    public int size() {
        return sprites.length;
    }
    
    /**
        Returns the sprite at the specified position in this group. Returns {@code null } if
        the index is out of range ({@code index < 0 || index >= size()}).
    */
    public Sprite get(int index) {
        Sprite[] snapshot = sprites;
        if (index < 0 || index >= snapshot.length) {
            return null;
        }
        return snapshot[index];
    }
    
    /**
        Returns {@code true} if this Group contains the specified Sprite. 
    */
    public boolean contains(Sprite sprite) {
        return indexOf(sprites, sprite) != -1;
    }
    
    /**
        Finds the top-most sprite at the specified location, or null if none is found.
        All sprites in this Group and any child Groups are searched until a sprite is found.
        This method never returns a Group.
        @param viewX x-coordinate in view space.
        @param viewY y-coordinate in view space.
        @return The top-most sprite at the specified location, or null if none is found.
    */
    public Sprite pick(int viewX, int viewY) {
        Sprite[] snapshot = sprites;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Sprite child = snapshot[i];
            if (child instanceof Group) {
                child = ((Group)child).pick(viewX, viewY);
                if (child != null) {
                    return child;
                }
            }
            else if (child.contains(viewX, viewY)) {
                return child;
            }
        }
        return null;
    }
    
    /**
        Finds the top-most sprite that is enabled and visible at the specified location, or null 
        if none is found.
        All sprites in this Group and any child Groups are searched until a sprite is found.
        This method never returns a Group.
        <p>
        This Group or it's ancestors (if any) are not checked if they are enabled or visible.
        <p>
        This method is useful for finding a sprite to use to set the cursor or take mouse input
        from.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
        @return The top-most sprite that is enabled and visible at the specified location, or null 
        if none is found.
    */
    public Sprite pickEnabledAndVisible(int viewX, int viewY) {
        Sprite[] snapshot = sprites;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Sprite child = snapshot[i];
            if (child.enabled.get() == true && child.visible.get() == true && 
                child.alpha.get() > 0) 
            {
                if (child instanceof Group) {
                    child = ((Group)child).pickEnabledAndVisible(viewX, viewY);
                    if (child != null) {
                        return child;
                    }
                }
                else if (child.contains(viewX, viewY)) {
                    return child;
                }
            }
        }
        return null;
    }
    
    /**
        Returns the number of sprites in this group and all child groups (not counting child
        Groups themselves).
    */
    public int getNumSprites() {
        Sprite[] snapshot = sprites;
        int count = 0;
        for (int i = 0; i < snapshot.length; i++) {
            Sprite s = snapshot[i];
            if (s instanceof Group) {
                count += ((Group)s).getNumSprites();
            }
            else {
                count++;
            }
        }
        return count;
    }
    
    /**
        Returns the number of visible sprites in this group and all child groups (not counting child
        Groups themselves).
    */
    public int getNumVisibleSprites() {
        Sprite[] snapshot = sprites;
        if (visible.get() == false || alpha.get() == 0) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < snapshot.length; i++) {
            Sprite s = snapshot[i];
            if (s instanceof Group) {
                count += ((Group)s).getNumVisibleSprites();
            }
            else if (s.visible.get() == true && s.alpha.get() > 0) {
                count++;
            }
        }
        return count;
    }
    
    //
    // Sprite list modifications
    //
    
    /**
        Adds a Sprite to this Group. The Sprite is added so it appears above all other sprites in
        this Group. If this Sprite already belongs to a Group, it is first removed from that 
        Group before added to this one.
    */
    public void add(Sprite sprite) {
        if (sprite != null) {
            synchronized (getTreeLock()) {
                Sprite[] snapshot = sprites;
                Group parent = sprite.getParent();
                if (parent != null) {
                    parent.remove(sprite);
                }
                sprites = add(snapshot, sprite, snapshot.length);
                sprite.setParent(this);
            }
        }
    }
    
    /**
        Removes a Sprite from this Group.
    */
    public void remove(Sprite sprite) {
        if (sprite != null) {
            synchronized (getTreeLock()) {
                Sprite[] snapshot = sprites;
                int index = indexOf(snapshot, sprite);
                if (index != -1) {
                    sprites = remove(snapshot, index);
                    sprite.setParent(null);
                }
            }
        }
    }
    
    /**
        Removes all Sprites from this Group.
    */
    public void removeAll() {
        synchronized (getTreeLock()) {
            Sprite[] snapshot = sprites;
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].setParent(null);
            }
            sprites = new Sprite[0];
        }
    }
    
    private void move(Sprite sprite, int position, boolean relative) {
        synchronized (getTreeLock()) {
            Sprite[] snapshot = sprites;
            int oldPosition = indexOf(snapshot, sprite);
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
                    sprites = add(snapshot, sprite, position);
                    sprite.setDirty(true);
                }
            }
        }
    }
    
    /**
        Moves the specified Sprite to the top of the z-order, so that all the other Sprites 
        currently in this Group appear underneath it. If the specified Sprite is not in this Group,
        or the Sprite is already at the top, this method does nothing.
    */
    public void moveToTop(Sprite sprite) {
        move(sprite, Integer.MAX_VALUE, false);
    }
    
    /**
        Moves the specified Sprite to the bottom of the z-order, so that all the other Sprites 
        currently in this Group appear above it. If the specified Sprite is not in this Group,
        or the Sprite is already at the bottom, this method does nothing.
    */
    public void moveToBottom(Sprite sprite) {
        move(sprite, 0, false);
    }
    
    /**
        Moves the specified Sprite up in z-order, swapping places with the first Sprite that 
        appears above it. If the specified Sprite is not in this Group, or the Sprite is already
        at the top, this method does nothing.
    */
    public void moveUp(Sprite sprite) {
        move(sprite, +1, true);
    }
    
    /**
        Moves the specified Sprite down in z-order, swapping places with the first Sprite that 
        appears below it. If the specified Sprite is not in this Group, or the Sprite is already
        at the bottom, this method does nothing.
    */
    public void moveDown(Sprite sprite) {
        move(sprite, -1, true);
    }
    
    /**
        Gets a list of all of the Sprites in this Group that were
        removed since the last call to this method.
        <p>
        This method is used by Scene2D to implement dirty rectangles.
    */
    public ArrayList getRemovedSprites() {
        ArrayList removedSprites = null;
        if (previousSprites == null) {
            // First call from Scene2D - no remove notifications needed
            previousSprites = sprites;
        }
        else if (previousSprites != sprites) {
            // Modifications occured - get list of all removed sprites.
            // NOTE: we make the list here, rather than in remove(), because if the list was
            // creating in remove() and this method was never called (non-Scene2D implementation)
            // the removedSprites list would continue to grow, resulting in a memory leak.
            for (int i = 0; i < previousSprites.length; i++) {
                if (previousSprites[i].getParent() != this) {
                    if (removedSprites == null) {
                        removedSprites = new ArrayList();
                    }
                    removedSprites.add(previousSprites[i]);
                }
            }
            previousSprites = sprites;
        }
        return removedSprites;
    }
    
    /**
        Packs this group so that its dimensions match the area covered by its children. 
    */
    public void pack() {
        Sprite[] snapshot = sprites;
        
        if (snapshot.length > 0) {
            // Integers
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            
            for (int i = 0; i < snapshot.length; i++) {
                Sprite sprite = snapshot[i];
                if (sprite instanceof Group) {
                    ((Group)sprite).pack();
                }
                Rect bounds = sprite.getRelativeBounds();
                minX = Math.min(minX, bounds.x);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                minY = Math.min(minY, bounds.y);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
            fInnerX = CoreMath.toFixed(-minX);
            fInnerY = CoreMath.toFixed(-minY);
            fNaturalWidth = CoreMath.toFixed(maxX - minX);
            fNaturalHeight = CoreMath.toFixed(maxY - minY);
            width.setAsFixed(fNaturalWidth);
            height.setAsFixed(fNaturalHeight);
        }
        else {
            fInnerX = 0;
            fInnerY = 0;
            fNaturalWidth = 0;
            fNaturalHeight = 0;
            width.set(0);
            height.set(0);
        }
        setDirty(true);
    }
    
    //
    // Sprite class implementation
    // 
    
    protected int getNaturalWidth() {
        if (fNaturalWidth > 0) {
            return fNaturalWidth;
        }
        else {
            return width.getAsFixed();
        }
    }
    
    protected int getNaturalHeight() {
        if (fNaturalHeight > 0) {
            return fNaturalHeight;
        }
        else {
            return height.getAsFixed();
        }
    }
    
    protected int getAnchorX() {
        return super.getAnchorX() - fInnerX;
    }
    
    protected int getAnchorY() {
        return super.getAnchorY() - fInnerY;
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        Sprite[] snapshot = sprites;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].update(elapsedTime);
        }
    }
    
    protected void drawSprite(CoreGraphics g) {
        Sprite[] snapshot = sprites;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].draw(g);
        }
    }
    
    //
    // Static convenience methods for working with immutable Sprite arrays
    //
    
    private static int indexOf(Sprite[] snapshot, Sprite s) {
        for (int i = 0; i < snapshot.length; i++) {
            if (s == snapshot[i]) {
                return i;
            }
        }
        return -1;
    }
    
    private static Sprite[] remove(Sprite[] snapshot, int index) {
        if (index >= 0 && index < snapshot.length) {
            Sprite[] newSprites = new Sprite[snapshot.length - 1];
            System.arraycopy(snapshot, 0, newSprites, 0, index);
            System.arraycopy(snapshot, index + 1, newSprites, index, 
                newSprites.length - index);
            snapshot = newSprites;
        }
        return snapshot;
    }
    
    private static Sprite[] add(Sprite[] snapshot, Sprite sprite, int index) {
        if (index < 0) {
            index = 0;
        }
        else if (index > snapshot.length) {
            index = snapshot.length;
        }
        Sprite[] newSprites = new Sprite[snapshot.length + 1];
        System.arraycopy(snapshot, 0, newSprites, 0, index);
        newSprites[index] = sprite;
        System.arraycopy(snapshot, index, newSprites, index + 1, snapshot.length - index);
        return newSprites;
    }
}
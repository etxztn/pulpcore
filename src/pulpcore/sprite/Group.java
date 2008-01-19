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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.scene.Scene2D;
import pulpcore.Stage;

/**
    A container of Sprites.
*/
public class Group extends Sprite {
    
    private static final int MOD_NONE = 0;
    private static final int MOD_ADDED = 1;
    private static final int MOD_REMOVED = 2;
    private static final int MOD_REORDERED = 4;
    
    private ArrayList sprites = new ArrayList();
    private ArrayList previousSprites = null;
    private int modCount = 0;
    // Modification actions since the last call to getRemovedSprites()
    private int modActions = MOD_NONE;
    private int transformModCount = 0;
    
    protected int fNaturalWidth;
    protected int fNaturalHeight;
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
    
    //
    // Sprite list queries
    //
    
    /**
        Returns the number of sprites in this group. This includes child groups but not
        the children of those groups.
    */
    public int size() {
        return sprites.size();
    }
    
    /**
        Returns the sprite at the specified position in this group. Returns null if the index is
        out of range (<code>index < 0 || index >= size()</code>).
    */
    public Sprite get(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        return (Sprite)sprites.get(index);
    }
    
    /**
        Finds the top-most sprite at the specified location in this Group and any child Groups,
        or null if none. This method never returns a Group.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public Sprite pick(int viewX, int viewY) {
        for (int i = size() - 1; i >= 0 ; i--) {
            Sprite child = get(i);
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
    
    /*
        Using a custom method because list.indexOf() uses .equals(), which could cause problems
    */
    private int getIndex(Sprite s) {
        if (s != null) {
            for (int i = 0; i < sprites.size(); i++) {
                if (s == sprites.get(i)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
        Returns the number of sprites in this group and all child groups (not counting child
        Groups themselves).
    */
    public int getNumSprites() {
        int count = 0;
        for (int i = 0; i < size(); i++) {
            Sprite s = get(i);
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
        if (visible.get() == false || alpha.get() == 0) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < size(); i++) {
            Sprite s = get(i);
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
        Check to see if the current thread is the animation thread.
    */
    private boolean isModificationAllowed() {
        // Only check in DEBUG mode
        if (Build.DEBUG && !Stage.isAnimationThread()) {
            CoreSystem.print("Use Scene2D.invokeLater() or Scene2D.invokeAndWait().",
                new IllegalStateException("Could not modify group from thread " + 
                    Thread.currentThread().getName()));
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
        Adds a Sprite to this Group. The Sprite is added so it appears above all other sprites in
        this Group. If this Sprite already belongs to a Group, it is first removed from that 
        Group before added to this one.
    */
    public void add(Sprite sprite) {
        if (sprite != null && isModificationAllowed()) {
            Group parent = sprite.getParent();
            if (parent != null) {
                parent.remove(sprite);
            }
            modActions |= MOD_ADDED;
            modCount++;
            sprite.setParent(this);
            sprites.add(sprite);
        }
    }
    
    /**
        Removes a Sprite from this Group.
    */
    public void remove(Sprite sprite) {
        if (sprite != null && isModificationAllowed()) {
            boolean wasContained = sprites.remove(sprite);
            if (wasContained) {
                sprite.setParent(null);
                modActions |= MOD_REMOVED;
                modCount++;
            }
        }
    }
    
    /**
        Removes all Sprites from this Group.
    */
    public void removeAll() {
        if (isModificationAllowed() && size() > 0) {
            for (int i = 0; i < size(); i++) {
                get(i).setParent(null);
            }
            modActions |= MOD_REMOVED;
            modCount++;
            sprites.clear();
        }
    }
    
    /**
        Moves the specified Sprite to the top of the z-order, so that all the other Sprites 
        currently in this Group appear underneath it. If the specified Sprite is not in this Group,
        or the Sprite is already at the top, this method does nothing.
    */
    public void moveToTop(Sprite sprite) {
        moveTo(sprite, sprites.size() - 1);
    }
    
    /**
        Moves the specified Sprite to the bottom of the z-order, so that all the other Sprites 
        currently in this Group appear above it. If the specified Sprite is not in this Group,
        or the Sprite is already at the bottom, this method does nothing.
    */
    public void moveToBottom(Sprite sprite) {
        moveTo(sprite, 0);
    }
    
    /**
        Moves the specified Sprite up in z-order, swapping places with the first Sprite that 
        appears above it. If the specified Sprite is not in this Group, or the Sprite is already
        at the top, this method does nothing.
    */
    public void moveUp(Sprite sprite) {
        int position = getIndex(sprite);
        swap(position, Math.min(position + 1, sprites.size() - 1));
    }
    
    /**
        Moves the specified Sprite down in z-order, swapping places with the first Sprite that 
        appears below it. If the specified Sprite is not in this Group, or the Sprite is already
        at the bottom, this method does nothing.
    */
    public void moveDown(Sprite sprite) {
        int position = getIndex(sprite);
        swap(position, Math.max(position - 1, 0));
    }
    
    private void moveTo(Sprite sprite, int goalPosition) {
        int position = getIndex(sprite);
        if (position != -1 && position != goalPosition && isModificationAllowed()) {
            sprites.remove(position);
            sprites.add(goalPosition, sprite);
            sprite.setDirty(true);
            modActions |= MOD_REORDERED;
            modCount++;
        }
    }
    
    private void swap(int positionA, int positionB) {
        if (positionA != -1 && positionB != -1 && positionA != positionB && 
            isModificationAllowed()) 
        {
            Sprite a = (Sprite)sprites.get(positionA);
            Sprite b = (Sprite)sprites.get(positionB);
            sprites.set(positionA, b);
            sprites.set(positionB, a);
            // Doesn't matter which one is set to dirty
            a.setDirty(true);
            modActions |= MOD_REORDERED;
            modCount++;
        }
    }

    /**
        Gets an iterator of all of the Sprites in this Group that were
        removed since the last call to this method. 
        This method is used by Scene2D to implement dirty rectangles.
    */
    public Iterator getRemovedSprites() {
        
        if (modActions == MOD_NONE) {
            return null;
        }
        else if (previousSprites == null) {
            // First call from Scene2D - no remove notifications needed
            previousSprites = new ArrayList(sprites);
            modActions = MOD_NONE;
            return null;
        }
        else if ((modActions & MOD_REMOVED) == 0) {
            // There were modifications, but nothing was removed
            previousSprites.clear();
            previousSprites.addAll(sprites);
            modActions = MOD_NONE;
            return null;
        }
        else {
            // There were removed sprites
            // NOTE: we make the list here, rather than in remove(), because if the list was
            // creating in remove() and this method was never called (non-Scene2D implementation)
            // the removedSprites list would continue to grow, resulting in a memory leak.
            ArrayList removedSprites = new ArrayList();
            
            for (int i = 0; i < previousSprites.size(); i++) {
                Sprite sprite = (Sprite)previousSprites.get(i);
                if (sprite.getParent() != this) {
                    removedSprites.add(sprite);
                }
            }
            previousSprites.clear();
            previousSprites.addAll(sprites);
            modActions = MOD_NONE;
            return removedSprites.iterator();
        }
    }
    
    /**
        Packs this group so that its dimensions match the area covered by its children. 
    */
    public void pack() {
        if (size() > 0) {
            // Integers
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            
            for (int i = 0; i < size(); i++) {
                Sprite sprite = get(i);
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
    
    /*
    public void setNaturalSize(int fNaturalWidth, int fNaturalHeight) {
        this.fNaturalWidth = fNaturalWidth;
        this.fNaturalHeight = fNaturalHeight;
        width.set(fNaturalWidth);
        height.set(fNaturalHeight);
    }
    */
   
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
    
    // Listed here as a seperate method for HotSpot.
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956
    private void throwModificationException() {
        throw new ConcurrentModificationException("Group modified during iteration.");
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        int lastModCount = modCount;
        for (int i = 0; i < size(); i++) {
            get(i).update(elapsedTime);
            if (lastModCount != modCount) {
                throwModificationException();
            }
        }
    }
    
    protected void drawSprite(CoreGraphics g) {
        int lastModCount = modCount;
        for (int i = 0; i < size(); i++) {
            get(i).draw(g);
            if (lastModCount != modCount) {
                throwModificationException();
            }
        }
    }
}
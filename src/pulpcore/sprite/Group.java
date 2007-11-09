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
    <p>Adding the same Sprite instance to multiple groups is unsupported and
    may yield undesireable results.
*/
public class Group extends Sprite {
    
    private static final int MOD_NONE = 0;
    private static final int MOD_ADDED = 1;
    private static final int MOD_REMOVED = 2;
    
    private static boolean threadDebug = false;

    private ArrayList sprites = new ArrayList();
    private ArrayList previousSprites = null;
    private int modCount = 0;
    // Modification actions since the last call to getRemovedSprites()
    private int modActions = MOD_NONE;
    
    protected int fNaturalWidth;
    protected int fNaturalHeight;
    private int fInnerX;
    private int fInnerY;
    private Transform transformForChildren = new Transform();
    
    
    /**
        Enables or disables thread debugging on the "debug" build of PulpCore. 
        <p>
        PulpCore is a single-threaded architecture, but some apps (notably, networked apps) may 
        run in multiple threads. If your app is getting ConcurrentModificationExceptions, enabling 
        thread debugging will help track down the root of the problem in your app's thread code.
        <p>
        If thread debugging is enabled, any Group's add(), remove(), and removeAll() methods will 
        print an error to the console if they are invoked from any thread other than the animation 
        thread.
        <p>
        Thread debugging is disabled by default.
        <p>
         Thread debugging cannot be enabled in the "release" build of PulpCore.
         @see #getThreadDebug
    */
    public static void setThreadDebug(boolean threadDebug) {
        Group.threadDebug = threadDebug;
    }
    
    
    /**
        @return true if thread debugging is enabled.
        @see #setThreadDebug(boolean)
    */
    public static boolean getThreadDebug() {
        return Build.DEBUG & Group.threadDebug;
    }
    
    
    public Group() {
        this(0, 0, -1, -1);
    }
    
    
    public Group(int x, int y) {
        this(x, y, -1, -1);
    }
    
    
    public Group(int x, int y, int width, int height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    
    public Group(double x, double y) {
        this(x, y, -1, -1);
    }
    
    
    public Group(double x, double y, double width, double height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    
    //
    // Sprite list queries
    //
    
    
    public int size() {
        return sprites.size();
    }
    
    
    public boolean isEmpty() {
        return (sprites.size() == 0);
    }
    
    
    public Sprite get(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        return (Sprite)sprites.get(index);
    }
    
    
    public boolean contains(Sprite s) {
        return sprites.contains(s);
    }
    
    
    /**
        Returns the number of sprites in this group and all child groups.
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
        Returns the number of visible sprites in this group and all child groups.
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
    
    
    private void checkThread() {
        if (Build.DEBUG && threadDebug) {
            if (!Stage.isAnimationThread()) {
                CoreSystem.print("Thread issue (ignoring)", 
                    new IllegalStateException("Group modified outside the animation thread."));
            }
        }
    }
    
    
    /**
        Adds a Sprite to this Group.
    */
    public void add(Sprite sprite) {
        checkThread();
        if (sprite != null && !sprites.contains(sprite)) {
            modActions |= MOD_ADDED;
            modCount++;
            sprite.setDirty(true);
            sprite.clearDirtyRect();
            sprites.add(sprite);
        }
    }
    
    
    /**
        Removes a Sprite from this Group.
    */
    public void remove(Sprite sprite) {
        checkThread();
        if (sprite != null) {
            boolean wasContained = sprites.remove(sprite);
            if (wasContained) {
                modActions |= MOD_REMOVED;
                modCount++;
            }
        }
    }
    
    
    /**
        Removes all Sprites from this Group.
    */
    public void removeAll() {
        checkThread();
        if (sprites.size() > 0) {
            modActions |= MOD_REMOVED;
            modCount++;
            sprites.clear();
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
                if (!sprites.contains(sprite)) {
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
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            
            for (int i = 0; i < size(); i++) {
                Sprite sprite = get(i);
                if (sprite instanceof Group) {
                    ((Group)sprite).pack();
                }
                int x = sprite.x.getAsFixed() - sprite.getAnchorX();
                int y = sprite.y.getAsFixed() - sprite.getAnchorY();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x + sprite.width.getAsFixed());
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y + sprite.height.getAsFixed());
            }
            
            fInnerX = -minX;
            fInnerY = -minY;
            fNaturalWidth = maxX - minX;
            fNaturalHeight = maxY - minY;
            width.setAsFixed(fNaturalWidth);
            height.setAsFixed(fNaturalHeight);
        }
        else {
            fInnerX = 0;
            fInnerY = 0;
            fNaturalWidth = -1;
            fNaturalHeight = -1;
            width.set(1);
            height.set(1);
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
    
    
    public void prepareToDraw(Transform parentTransform, boolean parentDirty) {
        super.prepareToDraw(parentTransform, parentDirty);
        
        parentDirty |= isDirty();
        
        transformForChildren.set(drawTransform);
        if (fInnerX != 0 || fInnerY != 0) {
            if (pixelSnapping.get()) {
                transformForChildren.translate(
                    CoreMath.intPart(fInnerX), CoreMath.intPart(fInnerY));
            }
            else {
                transformForChildren.translate(fInnerX, fInnerY);
            }
        }
        
        int lastModCount = modCount;
        for (int i = 0; i < size(); i++) {
            get(i).prepareToDraw(transformForChildren, parentDirty);
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
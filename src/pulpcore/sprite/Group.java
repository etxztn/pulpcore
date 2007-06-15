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

import java.util.Vector;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.scene.Scene2D;

/**
    A container of Sprites.
    <p>Adding and removing sprites doesn't occur until {@link #commitChanges() } is invoked,
    which is invoked by Scene2D after every call to {@link pulpcore.scene.Scene2D#update(int) }.
    <p>Adding the same Sprite instance to multiple groups is unsupported and
    may yield undesireable results.
*/
public class Group extends Sprite {

    private Group parent;
    private Vector sprites;
    private Vector spritesToAdd;
    private Vector spritesToRemove;
    protected int fNaturalWidth;
    protected int fNaturalHeight;
    private int fInnerX;
    private int fInnerY;
    private Transform transformForChildren = new Transform();
    
    
    public Group() {
        this(0, 0, -1, -1);
    }
    
    
    public Group(int x, int y) {
        this(x, y, -1, -1);
    }
    
    
    public Group(int x, int y, int width, int height) {
        super(x, y, width, height);
        sprites = new Vector();
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
        return (Sprite)sprites.elementAt(index);
    }
    
    
    public boolean contains(Sprite s) {
        return sprites.contains(s);
    }
    
    
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
        Adds a Sprite to this Group.
        <p>The add doesn't occur until {@link #commitChanges() } is invoked, which is invoked by
        Scene2D after every call to {@link pulpcore.scene.Scene2D#update(int) }.
    */
    public void add(Sprite sprite) {
        if (sprite == null) {
            return;
        }
        
        if (!sprites.contains(sprite) && 
            (spritesToAdd == null || !spritesToAdd.contains(sprite))) 
        {
            sprite.setDirty(true);
            sprite.clearDirtyRect();
            if (spritesToAdd == null) {
                spritesToAdd = new Vector();
            }
            spritesToAdd.addElement(sprite);
        }
        if (spritesToRemove != null) {
            spritesToRemove.removeElement(sprite);
        }
    }
    
    
    /**
        Removes a Sprite from this Group.
        <p>The remove doesn't occur until {@link #commitChanges() } is invoked, which is invoked by
        Scene2D after every call to {@link pulpcore.scene.Scene2D#update(int) }.
    */
    public void remove(Sprite sprite) {
        if (sprite == null) {
            return;
        }
        if (sprites.contains(sprite) && 
            (spritesToRemove == null || !spritesToRemove.contains(sprite)))
        {
            sprite.setDirty(true);
            if (spritesToRemove == null) {
                spritesToRemove = new Vector();
            }
            spritesToRemove.addElement(sprite);
        }
        if (spritesToAdd != null) {
            spritesToAdd.removeElement(sprite);
        }
    }
    
    
    /**
        Removes all Sprites from this Group.
        <p>The remove doesn't occur until {@link #commitChanges() } is invoked, which is invoked by
        Scene2D after every call to {@link pulpcore.scene.Scene2D#update(int) }.
    */
    public void removeAll() {
        spritesToAdd = null;
        spritesToRemove = new Vector();
        for (int i = 0; i < size(); i++) {
            Sprite sprite = get(i);
            sprite.setDirty(true);
            spritesToRemove.addElement(sprite);
        }
    }


    /**
        Commits the changes made by the {@link #add(Sprite) } and {@link #remove(Sprite)} methods.
    */
    public void commitChanges() {
        commitChanges(null);
    }
    
    
    /**
        Commits the changes made by the {@link #add(Sprite) } and {@link #remove(Sprite)} methods, 
        and notifies the specified {@link pulpcore.scene.Scene2D } of sprites removed since 
        the last call to this method.
    */
    public void commitChanges(Scene2D scene) {
        
        if (spritesToAdd != null) {
            for (int i = 0; i < spritesToAdd.size(); i++) {
                Sprite sprite = (Sprite)spritesToAdd.elementAt(i);
                sprite.setDirty(true);
                sprite.clearDirtyRect();
                if (sprite instanceof Group) {
                    Group group = (Group)sprite;
                    group.parent = this;
                }
                sprites.addElement(sprite);
            }
            spritesToAdd = null;
        }
        
        if (spritesToRemove != null) {
            for (int i = 0; i < spritesToRemove.size(); i++) {
                Sprite sprite = (Sprite)spritesToRemove.elementAt(i);
                if (sprite instanceof Group) {
                    Group group = (Group)sprite;
                    group.parent = null;
                }
                if (scene != null) {
                    scene.notifyRemovedSprite(sprite);
                }
                sprites.removeElement(sprite);
            }
            spritesToRemove = null;
        }
        
        for (int i = 0; i < size(); i++) {
            Sprite sprite = get(i);
            if (sprite instanceof Group) {
                Group group = (Group)sprite;
                group.commitChanges(scene);
            }
        }
    }
    
    
    /**
        Packs this group so that its dimensions match the area covered by its children. The
        {@link #commitChanges() } is invoked before packing. 
    */
    public void pack() {
        
        commitChanges();
        
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
        
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        for (int i = 0; i < size(); i++) {
            get(i).update(elapsedTime);
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
        
        for (int i = 0; i < size(); i++) {
            get(i).prepareToDraw(transformForChildren, parentDirty);
        }
    }
    
        
    protected void drawSprite(CoreGraphics g) {
        for (int i = 0; i < size(); i++) {
            get(i).draw(g);
        }
    }
}
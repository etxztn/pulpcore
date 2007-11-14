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

import pulpcore.animation.Int;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.Stage;

/**
    Solid-colored rectangluar shaped sprite. Note, CoreGraphics cannot draw
    rotated solid-colored rectuanglar shapes, so changing the angle of this
    sprite will have no effect. 
*/
public class FilledSprite extends Sprite {
    
    /** Fill color in ARGB format */
    public final Int fillColor = new Int(this);
    
    /** Border color in ARGB format */
    public final Int borderColor = new Int(this);
    
    // Fixed-point
    protected int borderTop;
    protected int borderLeft;
    protected int borderBottom;
    protected int borderRight;
    
    
    public FilledSprite(int fillColor) {
        this(0, 0, Stage.getWidth(), Stage.getHeight(), fillColor, false);
    }
    
    
    public FilledSprite(int fillColor, boolean hasAlpha) {
        this(0, 0, Stage.getWidth(), Stage.getHeight(), fillColor, hasAlpha);
    }
    
    
    /**
        @param fillColor Color of the FilledSprite in the RGB format.
    */
    public FilledSprite(int x, int y, int w, int h, int fillColor) {
        this(x, y, w, h, fillColor, false);
    }
    
    
    public FilledSprite(int x, int y, int w, int h, int fillColor, boolean hasAlpha) {
        super(x, y, w, h);
        setFillColor(fillColor, hasAlpha);
    }
    
    
    public FilledSprite(double x, double y, double w, double h, int fillColor) {
        this(x, y, w, h, fillColor, false);
    }
    
    
    public FilledSprite(double x, double y, double w, double h, int fillColor, boolean hasAlpha) {
        super(x, y, w, h);
        setFillColor(fillColor, hasAlpha);
    }
    
    
    public void setFillColor(int r, int g, int b) {
        setFillColor((r << 16) | (g << 8) | b, false);
    }
    
    
    public void setFillColor(int r, int g, int b, int a) {
        setFillColor((a << 24) | (r << 16) | (g << 8) | b, true);
    }
    
    
    public void setFillColor(int rgbColor) {
        setFillColor(rgbColor, false);
    }
    
    
    public void setFillColor(int argbColor, boolean hasAlpha) {
        if (hasAlpha) {
            fillColor.set(argbColor);
        }
        else {
            fillColor.set(0xff000000 | argbColor);
        }
    }
    
    
    public void setBorderColor(int r, int g, int b) {
        setBorderColor((r << 16) | (g << 8) | b, false);
    }
    
    
    public void setBorderColor(int r, int g, int b, int a) {
        setBorderColor((a << 24) | (r << 16) | (g << 8) | b, true);
    }
    
    
    public void setBorderColor(int rgbColor) {
        setBorderColor(rgbColor, false);
    }
    
    
    public void setBorderColor(int argbColor, boolean hasAlpha) {
        if (hasAlpha) {
            borderColor.set(argbColor);
        }
        else {
            borderColor.set(0xff000000 | argbColor);
        }
    }
    
    
    public final void setBorderSize(int borderSize) {
        setBorderSize(borderSize, borderSize, borderSize, borderSize);
    }
    
    
    public void setBorderSize(int top, int left, int bottom, int right) {
        top = CoreMath.toFixed(Math.max(0, top));
        left = CoreMath.toFixed(Math.max(0, left));
        bottom = CoreMath.toFixed(Math.max(0, bottom));
        right = CoreMath.toFixed(Math.max(0, right));
        
        if (this.borderTop != top) {
            this.borderTop = top;
            setDirty(true);
        }
        if (this.borderLeft != left) {
            this.borderLeft = left;
            setDirty(true);
        }
        if (this.borderBottom != bottom) {
            this.borderBottom = bottom;
            setDirty(true);
        }
        if (this.borderRight != right) {
            this.borderRight = right;
            setDirty(true);
        }
    }
    
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        fillColor.update(elapsedTime);
        borderColor.update(elapsedTime);
    }
    
    
    protected void drawSprite(CoreGraphics g) {
        
        int w = width.getAsFixed();
        int h = height.getAsFixed();
        int innerWidth = w - (borderLeft + borderRight);
        int innerHeight = h - (borderTop + borderBottom);
        
        // Inner fill
        if ((fillColor.get() >>> 24) != 0) {
            g.setColor(fillColor.get(), true);
            g.fillRectFixedPoint(borderLeft, borderTop, innerWidth, innerHeight);
        }
        
        // Border fill
        if ((borderColor.get() >>> 24) != 0) {
            g.setColor(borderColor.get(), true);
            if (borderTop > 0) {
                g.fillRectFixedPoint(0, 0, w, borderTop);
            }
            if (borderBottom > 0) {
                g.fillRectFixedPoint(0, h - borderBottom, w, borderBottom);
            }
            if (borderLeft > 0) {
                g.fillRectFixedPoint(0, borderTop, borderLeft, innerHeight);
            }
            if (borderRight > 0) {
                g.fillRectFixedPoint(w - borderRight, borderTop, borderRight, innerHeight);
            }
        }
    }
      
}
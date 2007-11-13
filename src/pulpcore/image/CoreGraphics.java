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

package pulpcore.image;

import java.util.EmptyStackException;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;

/**
    Graphics rendering routines onto a CoreImage surface. 
    The default composite is COMPOSITE_SRC_OVER and the surface is assumed to be opaque.
    The clip is in view-space - not affected by the Transform.
*/
public class CoreGraphics {
    
    public static final int BLACK     = 0x000000;
    
    public static final int WHITE     = 0xffffff;
    
    public static final int LIGHTGRAY = 0xc0c0c0;
    
    public static final int GRAY      = 0x808080;
    
    public static final int DARKGRAY  = 0x404040;
    
    public static final int RED       = 0xff0000;
    
    public static final int PINK      = 0xffafaf;
    
    public static final int ORANGE    = 0xffc800;
    
    public static final int YELLOW    = 0xffff00;
    
    public static final int GREEN     = 0x00ff00;
    
    public static final int MAGENTA   = 0xff00ff;
    
    public static final int CYAN      = 0x00ffff;
    
    public static final int BLUE      = 0x0000ff;
    
    
    /** 
        The source is composited over the destination (Porter-Duff Source Over Destination rule).
        This is the default composite type.
        @see #setComposite(int) 
    */
    public static final int COMPOSITE_SRC_OVER = 0;
    
    /** 
        The source is copied to the destination (Porter-Duff Source rule). 
        The destination is not used as input. 
        @see #setComposite(int) 
    */
    public static final int COMPOSITE_SRC = 1;
    
    /** 
        @see #setComposite(int) 
    */
    public static final int COMPOSITE_ADD = 2;
    
    /** 
        @see #setComposite(int) 
    */
    public static final int COMPOSITE_MULT = 3;
    
    private static final Composite[] COMPOSITES = {
        new CompositeSrcOver(), new CompositeSrc(), new CompositeAdd(), new CompositeMult(), 
    };
    
    
    /**
        Nearest neighbor interpolation method (used for image scaling).
        Also known as "pixel scaling".
    */
    public static final int INTERPOLATION_NEAREST_NEIGHBOR = 0;
    
    
    /**
        Bilinear interpolation method (used for image scaling).
    */
    public static final int INTERPOLATION_BILINEAR = 1;
    
    
    // Clipping for drawLine
    private static final int CLIP_CODE_LEFT = 8;
    private static final int CLIP_CODE_RIGHT = 4;
    private static final int CLIP_CODE_ABOVE = 2;
    private static final int CLIP_CODE_BELOW = 1;
    
    // Surface data
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final int[] surfaceData;
    
    // The clip rectangle
    private int clipX;
    private int clipY;
    private int clipWidth;
    private int clipHeight;
    
    // A clipped object
    private int objectX;
    private int objectY;
    private int objectWidth;
    private int objectHeight;
    
    /** The current alpha value. 0 is fully tranaparent, 255 is fully opaque. */
    private int alpha;
    
    /** The composite method. */
    private Composite composite;
    private int compositeIndex;
    
    /** If true, bilinear filtering is used when scaling images. */
    private boolean bilinear;
    
    private boolean fractionalMetrics;
    
    /** The current color. */
    private int srcColor;
    
    /** The alpha value of the source color multiplied by the current alpha. */
    private int srcAlphaPremultiplied;
    
    /** The current color using srcAlphaPremultiplied as the alpha. */ 
    private int srcColorPremultiplied;
    
    private CoreFont font;
    
    private final Transform transform = new Transform();
    private Transform[] transformStack = new Transform[16];
    private int transformStackSize = 0;

    
    CoreGraphics(CoreImage surface) {
        surfaceWidth = surface.width;
        surfaceHeight = surface.height;
        surfaceData = surface.getData();
        compositeIndex = -1;
        
        for (int i = 0; i < transformStack.length; i++) {
            transformStack[i] = new Transform();
        }
        reset();
    }
    
    
    public int getSurfaceWidth() {
        return surfaceWidth;
    }
    
    
    public int getSurfaceHeight() {
        return surfaceHeight;
    }
    
    
    public int[] getSurfaceData() {
        return surfaceData;
    }
    
    
    /**
        Resets the rendering attributes for this CoreGraphics object to the 
        default values: 
        <ul>
            <li>No clip</li>
            <li>Identity transform (and the transform stack is cleared)</li>
            <li>color = BLACK</li>
            <li>alpha = 255</li>
            <li>composite = COMPOSITE_SRC_OVER</li>
            <li>interpolation = INTERPOLATION_BILINEAR</li>
            <li>font = null</li>
        </ul>
    */
    public void reset() {
        removeClip();
        setAlpha(0xff);
        setColor(0x000000);
        setComposite(COMPOSITE_SRC_OVER);
        bilinear = true;
        fractionalMetrics = true;
        font = null;
        
        transformStackSize = 0;
        transform.clear();
    }
    
    
    public void setComposite(int composite) {
        if (compositeIndex != composite) {
            compositeIndex = composite;
            this.composite = COMPOSITES[compositeIndex];
        }
    }
    
    
    public int getComposite() {
        return compositeIndex;
    }
    
    
    public void setInterpolation(int interpolation) {
        this.bilinear = (interpolation == INTERPOLATION_BILINEAR);
    }
    
    
    public int getInterpolation() {
        return bilinear ? INTERPOLATION_BILINEAR : INTERPOLATION_NEAREST_NEIGHBOR;
    }
    
    
    public void setFractionalMetrics(boolean useFractionalMetrics) {
        this.fractionalMetrics = useFractionalMetrics;
    }
    
    
    public boolean getFractionalMetrics() {
        return fractionalMetrics;
    }
    
    
    public void setFont(CoreFont font) {
        this.font = font;
    }
    
    
    public CoreFont getFont() {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        return font;
    }


    //
    // Transforms
    //
    
    
    /**
        Adds (pushes) a copy of the current transform to the top of the transform stack. 
    */
    public void pushTransform() {
        if (transformStackSize == transformStack.length) {
            // Double the size of the stack
            Transform[] newTransformStack = new Transform[transformStack.length * 2];
            System.arraycopy(transformStack, 0, newTransformStack, 0, transformStack.length);
            for (int i = transformStack.length; i < newTransformStack.length; i++) {
                newTransformStack[i] = new Transform();
            }
            
            transformStack = newTransformStack;
        }
        
        transformStack[transformStackSize].set(transform);
        transformStackSize++;
    }
    
    
    /**
        Removes (pops) the transform at the top of the transform stack and sets the 
        current transform to that popped transform.
        @throws EmptyStackException if the stack is empty
    */
    public void popTransform() throws EmptyStackException {
        if (transformStackSize == 0) {
            throw new EmptyStackException();
        }
        transformStackSize--;
        transform.set(transformStack[transformStackSize]);
    }
    
    
    /**
        Returns the current transform. The returned instance will always be
        the current transform of this graphics context.
    */
    public Transform getTransform() {
        return transform;
    }
    
    
    /**
        Sets the current transform to a copy of the specified transform. If
        the specified transform is null, the current transform is cleared, i.e.,
        set to the identity matrix.
    */
    public void setTransform(Transform newTransform) {
        transform.set(newTransform);
    }
    
    
    /**
        Sets the current transform to the identiy matrix.
    */
    public void clearTransform() {
        transform.clear();
    }
    

    //
    // Clipping
    //
    
    
    public void removeClip() {
        clipX = 0;
        clipY = 0;
        clipWidth = surfaceWidth;
        clipHeight = surfaceHeight;
    }
    
    
    /**
        The clip is not affected by the transform.
    */
    public void setClip(Rect r) {
        setClip(r.x, r.y, r.width, r.height);
    }
    
    
    /**
        The clip is not affected by the transform.
    */
    public void setClip(int x, int y, int w, int h) {
        removeClip();
        clipRect(x, y, w, h);
    }
    
    
    /**
        The clip is not affected by the transform.
    */
    public void clipRect(Rect r) {
        clipRect(r.x, r.y, r.width, r.height);
    }
    
    
    /**
        The clip is not affected by the transform.
    */
    public void clipRect(int x, int y, int w, int h) {
        
        clipObject(x, y, w, h);
        
        clipX = objectX;
        clipY = objectY;
        clipWidth = objectWidth;
        clipHeight = objectHeight;
    }
    
    
    private void clipObject(int x, int y, int w, int h) {
        
        if (x < clipX) {
            w -= clipX - x;
            x = clipX;
        }
        if (y < clipY) {
            h -= clipY - y;
            y = clipY;
        }
        if (x + w > clipX + clipWidth) {
            w = clipX + clipWidth - x;
        }
        if (y + h > clipY + clipHeight) {
            h = clipY + clipHeight - y;
        }
        
        objectX = x;
        objectY = y;
        objectWidth = w;
        objectHeight = h;
    }
    
    
    public int getClipX() {
        return clipX;
    }
    
    
    public int getClipY() {
        return clipY;
    }
    
    
    public int getClipWidth() {
        return clipWidth;
    }
    
    
    public int getClipHeight() {
        return clipHeight;
    }
    
    
    public void getClip(Rect rect) {
        rect.setBounds(clipX, clipY, clipWidth, clipHeight);
    }
    
    
    // 
    // ARGB color
    // 
    
    
    public void setAlpha(int alpha) {
        
        if (alpha <= 0) {
            this.alpha = 0;
            srcAlphaPremultiplied = 0;
        }
        else if (alpha >= 0xff) {
            this.alpha = 0xff;
            srcAlphaPremultiplied = srcColor >>> 24;
            srcColorPremultiplied = srcColor;
        }
        else {
            this.alpha = alpha;
            srcAlphaPremultiplied = ((srcColor >>> 24) * alpha) >> 8;
            srcColorPremultiplied = (srcAlphaPremultiplied << 24) | (srcColor & 0x00ffffff);
        }
    }
    
    
    public int getAlpha() {
        return alpha;
    }
    
    
    public void setColor(int r, int g, int b) {
        setColor((r << 16) | (g << 8) | b, false);
    }
    
    
    public void setColor(int r, int g, int b, int a) {
        setColor((a << 24) | (r << 16) | (g << 8) | b, true);
    }
    
    
    /**
        Sets the current color (color alpha is 255). 
    */
    public void setColor(int rgbColor) {
        setColor(rgbColor, false);
    }
    
    
    public void setColor(int argbColor, boolean hasAlpha) {
        
        if (hasAlpha) {
            srcColor = argbColor;
        }
        else {
            srcColor = 0xff000000 | argbColor;
        }
        
        if (alpha == 0xff) {
            srcAlphaPremultiplied = srcColor >>> 24;
            srcColorPremultiplied = srcColor;
        }
        else {
            srcAlphaPremultiplied = ((srcColor >>> 24) * alpha) >> 8;
            srcColorPremultiplied = (srcAlphaPremultiplied << 24) | (srcColor & 0x00ffffff);
        }
    }
    
    
    /**
        Returns the current color in ARGB format.
    */
    public int getColor() {
        return srcColor;
    }
    
    
    //
    // Primitive rendering
    //
   
    
    /*
        Draws a line using the current color. This method draws
        lines at integer coordinates. The transform scale and rotation is 
        ignored.
        <p>
        This method is not best suited for production use: there is
        no anti-aliasing and lines may appear distorted when clipped. 
        This method is only provided for debugging purposes. 
    */
    /*
    public void drawLineOld(int x1, int y1, int x2, int y2) {
        
        if (srcAlphaPremultiplied == 0) {
            return;
        }
        
        x1 += CoreMath.toInt(transform.getTranslateX());
        y1 += CoreMath.toInt(transform.getTranslateY());
        x2 += CoreMath.toInt(transform.getTranslateX());
        y2 += CoreMath.toInt(transform.getTranslateY());
        
        // Clip - Sutherland-Cohen algorithm
        int xmin = clipX;
        int xmax = xmin + clipWidth - 1;
        int ymin = clipY;
        int ymax = ymin + clipHeight - 1;
       
        int clipCode1 = 
            (x1 < xmin ? CLIP_CODE_LEFT : 0) | 
            (x1 > xmax ? CLIP_CODE_RIGHT : 0) | 
            (y1 < ymin ? CLIP_CODE_ABOVE : 0) | 
            (y1 > ymax ? CLIP_CODE_BELOW : 0);
        int clipCode2 = 
            (x2 < xmin ? CLIP_CODE_LEFT : 0) | 
            (x2 > xmax ? CLIP_CODE_RIGHT : 0) | 
            (y2 < ymin ? CLIP_CODE_ABOVE : 0) | 
            (y2 > ymax ? CLIP_CODE_BELOW : 0);

        while ((clipCode1 | clipCode2) != 0) {
           
            if ((clipCode1 & clipCode2) != 0) {
                // Completely outside the clip bounds - do nothing 
                return;
            }
            
            int dx = x2 - x1; 
            int dy = y2 - y1;
            if (clipCode1 != 0) {
                if ((clipCode1 & CLIP_CODE_LEFT) != 0) {
                    y1 += (xmin-x1) * dy / dx; 
                    x1 = xmin;
                }  
                else if ((clipCode1 & CLIP_CODE_RIGHT) != 0) {
                    y1 += (xmax-x1) * dy / dx; 
                    x1 = xmax;
                }  
                else if ((clipCode1 & CLIP_CODE_ABOVE) != 0) {
                    x1 += (ymin-y1) * dx / dy; 
                    y1 = ymin;
                }  
                else if ((clipCode1 & CLIP_CODE_BELOW) != 0) {
                    x1 += (ymax-y1) * dx / dy; 
                    y1 = ymax;
                }  
                clipCode1 = 
                    (x1 < xmin ? CLIP_CODE_LEFT : 0) | 
                    (x1 > xmax ? CLIP_CODE_RIGHT : 0) | 
                    (y1 < ymin ? CLIP_CODE_ABOVE : 0) | 
                    (y1 > ymax ? CLIP_CODE_BELOW : 0);
            }  
            else if (clipCode2 != 0) {
                if ((clipCode2 & CLIP_CODE_LEFT) != 0) {
                    y2 += (xmin-x2) * dy / dx; 
                    x2 = xmin;
                }  
                else if ((clipCode2 & CLIP_CODE_RIGHT) != 0) {
                    y2 += (xmax-x2) * dy / dx; 
                    x2 = xmax;
                }  
                else if ((clipCode2 & CLIP_CODE_ABOVE) != 0) {
                    x2 += (ymin-y2) * dx / dy; 
                    y2 = ymin;
                }  
                else if ((clipCode2 & CLIP_CODE_BELOW) != 0) {
                    x2 += (ymax-y2) * dx / dy; 
                    y2 = ymax;
                }  
                clipCode2 = 
                    (x2 < xmin ? CLIP_CODE_LEFT : 0) | 
                    (x2 > xmax ? CLIP_CODE_RIGHT : 0) | 
                    (y2 < ymin ? CLIP_CODE_ABOVE : 0) | 
                    (y2 > ymax ? CLIP_CODE_BELOW : 0);
            }  
        }
        
        // Draw - Bresenham's algorithm.
        int dx = x2 - x1; 
        int dy = y2 - y1;
        int dxabs = CoreMath.abs(dx);
        int dyabs = CoreMath.abs(dy);
        int sdx = (dx > 0) ? 1 : ((dx < 0) ? -1 : 0);
        int sdy = (dy > 0) ? surfaceWidth : ((dy < 0) ? -surfaceWidth : 0);
        int x = dyabs >> 1;
        int y = dxabs >> 1;
        
        int offset = x1 + y1 * surfaceWidth;
        if (compositeIndex == COMPOSITE_SRC || srcAlphaPremultiplied == 0xff) {
            surfaceData[offset] = srcColorPremultiplied;
            
            if (dxabs >= dyabs) {
                for (int i = 0; i < dxabs; i++) {
                    y += dyabs;
                    if (y >= dxabs) {
                        y -= dxabs;
                        offset += sdy;
                    }
                    offset += sdx;
                    surfaceData[offset] = srcColorPremultiplied;
                }
            }
            else {
                for (int i = 0; i < dyabs; i++) {
                    x += dxabs;
                    if (x >= dyabs) {
                        x -= dyabs;
                        offset += sdx;
                    }
                    offset += sdy;
                    surfaceData[offset] = srcColorPremultiplied;
                }
            }
        }
        else {
            composite.blend(surfaceData, offset, srcColor, srcAlphaPremultiplied);
            
            if (dxabs >= dyabs) {
                for (int i = 0; i < dxabs; i++) {
                    y += dyabs;
                    if (y >= dxabs) {
                        y -= dxabs;
                        offset += sdy;
                    }
                    offset += sdx;
                    composite.blend(surfaceData, offset, srcColor, srcAlphaPremultiplied);
                }
            }
            else {
                for (int i = 0; i < dyabs; i++) {
                    x += dxabs;
                    if (x >= dyabs) {
                        x -= dyabs;
                        offset += sdx;
                    }
                    offset += sdy;
                    composite.blend(surfaceData, offset, srcColor, srcAlphaPremultiplied);
                }
            }
        }
    }
    */
    
    
    /**
        Draws a line using the current color.
    */
    public void drawLine(int x1, int y1, int x2, int y2) {
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2), true);
    }
    
    
    /**
        Draws a line using the current color.
        @param drawLastPoint if false, the last point in the line is not drawn, which
        is useful for drawing continuous lines made of several line segments (like beziers)
    */
    public void drawLine(int x1, int y1, int x2, int y2, boolean drawLastPoint) {
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2), drawLastPoint);
    }
    
    
    /**
        Draws a line using the current color.
    */
    public void drawLine(double x1, double y1, double x2, double y2) {
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2), true);
    }
    
    
    /**
        Draws a line using the current color.
        @param drawLastPoint if false, the last point in the line is not drawn, which
        is useful for drawing continuous lines made of several line segments (like beziers).
    */
    public void drawLine(double x1, double y1, double x2, double y2, boolean drawLastPoint) {
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2), drawLastPoint);
    }
    
    
    /**
        Draws a line (at fixed-point coordinates) using the current color.
    */
    public void drawLineFixedPoint(int x1, int y1, int x2, int y2) {
        drawLineFixedPoint(x1, y1, x2, y2, true);
    }
    
    
    /**
        Draws a line (at fixed-point coordinates) using the current color.
        @param drawLastPoint if false, the last point in the line is not drawn, which
        is useful for drawing continuous lines made of several line segments (like beziers).
    */
    public void drawLineFixedPoint(int x1, int y1, int x2, int y2, boolean drawLastPoint) {
        if (srcAlphaPremultiplied == 0) {
            return;
        }
        
        pushTransform();
        transform.translate(x1, y1);
        drawLineToFixedPoint(x2 - x1, y2 - y1, drawLastPoint);
        popTransform();
    }
    
    
    ///**
    //    Draws a line from the current transform's origin to the origin plus (dx, dy)
    //    using the current color. This method is useful for drawing a series of line segments,
    //    like a bezier curve.
    //*/
    //public void drawLineTo(int dx, int dy) {
    //    drawLineToFixedPoint(CoreMath.toFixed(dx), CoreMath.toFixed(dy), false);
    //}
    //
    //
    ///**
    //    Draws a line from the current transform's origin to the origin plus (dx, dy)
    //    using the current color. This method is useful for drawing a series of line segments,
    //    like a bezier curve.
    //*/
    //public void drawLineTo(double dx, double dy) {
    //    drawLineToFixedPoint(CoreMath.toFixed(dx), CoreMath.toFixed(dy), false);
    //}
    //
    //
    ///**
    //    Draws a line from the current transform's origin to the origin plus (dx, dy)
    //    (at fixed-point coordinates) using the current color. This method is useful for drawing a 
    //    series of line segments,
    //    like a bezier curve.
    //*/
    //public void drawLineToFixedPoint(int dx, int dy) {
    //    drawLineToFixedPoint(dx, dy, false);
    //}
        
    
    /**
        Draws a line (at fixed-point coordinates) using the current color.
    */
    private void drawLineToFixedPoint(int dx, int dy, boolean drawLastPixel) {
        if (srcAlphaPremultiplied == 0) {
            return;
        }
        
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        
        pushTransform();
        transform.translate(dx, dy);
        int x2 = transform.getTranslateX();
        int y2 = transform.getTranslateY();
            
        // Get ready to draw
        transform.clear();
        
        if (!fractionalMetrics) {
            x1 = CoreMath.intPart(x1);
            y1 = CoreMath.intPart(y1);
            x2 = CoreMath.intPart(x2);
            y2 = CoreMath.intPart(y2);
        }
        
        // Clip - Sutherland-Cohen algorithm
        int xmin = CoreMath.toFixed(clipX);
        int xmax = xmin + CoreMath.toFixed(clipWidth);
        int ymin = CoreMath.toFixed(clipY);
        int ymax = ymin + CoreMath.toFixed(clipHeight);
       
        int clipCode1 = 
            (x1 < xmin ? CLIP_CODE_LEFT : 0) | 
            (x1 > xmax ? CLIP_CODE_RIGHT : 0) | 
            (y1 < ymin ? CLIP_CODE_ABOVE : 0) | 
            (y1 > ymax ? CLIP_CODE_BELOW : 0);
        int clipCode2 = 
            (x2 < xmin ? CLIP_CODE_LEFT : 0) | 
            (x2 > xmax ? CLIP_CODE_RIGHT : 0) | 
            (y2 < ymin ? CLIP_CODE_ABOVE : 0) | 
            (y2 > ymax ? CLIP_CODE_BELOW : 0);

        while ((clipCode1 | clipCode2) != 0) {
           
            if ((clipCode1 & clipCode2) != 0) {
                // Completely outside the clip bounds - do nothing 
                popTransform();
                return;
            }
            
            dx = x2 - x1; 
            dy = y2 - y1;
            if (clipCode1 != 0) {
                if ((clipCode1 & CLIP_CODE_LEFT) != 0) {
                    y1 += CoreMath.mulDiv(xmin-x1, dy, dx); 
                    x1 = xmin;
                }  
                else if ((clipCode1 & CLIP_CODE_RIGHT) != 0) {
                    y1 += CoreMath.mulDiv(xmax-x1, dy, dx); 
                    x1 = xmax;
                }  
                else if ((clipCode1 & CLIP_CODE_ABOVE) != 0) {
                    x1 += CoreMath.mulDiv(ymin-y1, dx, dy); 
                    y1 = ymin;
                }  
                else if ((clipCode1 & CLIP_CODE_BELOW) != 0) {
                    x1 += CoreMath.mulDiv(ymax-y1, dx, dy); 
                    y1 = ymax;
                }  
                clipCode1 = 
                    (x1 < xmin ? CLIP_CODE_LEFT : 0) | 
                    (x1 > xmax ? CLIP_CODE_RIGHT : 0) | 
                    (y1 < ymin ? CLIP_CODE_ABOVE : 0) | 
                    (y1 > ymax ? CLIP_CODE_BELOW : 0);
            }  
            else if (clipCode2 != 0) {
                if ((clipCode2 & CLIP_CODE_LEFT) != 0) {
                    y2 += CoreMath.mulDiv(xmin-x2, dy, dx); 
                    x2 = xmin;
                }  
                else if ((clipCode2 & CLIP_CODE_RIGHT) != 0) {
                    y2 += CoreMath.mulDiv(xmax-x2, dy, dx); 
                    x2 = xmax;
                }  
                else if ((clipCode2 & CLIP_CODE_ABOVE) != 0) {
                    x2 += CoreMath.mulDiv(ymin-y2, dx, dy); 
                    y2 = ymin;
                }  
                else if ((clipCode2 & CLIP_CODE_BELOW) != 0) {
                    x2 += CoreMath.mulDiv(ymax-y2, dx, dy); 
                    y2 = ymax;
                }  
                clipCode2 = 
                    (x2 < xmin ? CLIP_CODE_LEFT : 0) | 
                    (x2 > xmax ? CLIP_CODE_RIGHT : 0) | 
                    (y2 < ymin ? CLIP_CODE_ABOVE : 0) | 
                    (y2 > ymax ? CLIP_CODE_BELOW : 0);
            }  
        }
        
        dx = x2 - x1; 
        dy = y2 - y1;
        int dxabs = CoreMath.abs(dx);
        int dyabs = CoreMath.abs(dy);
        int sdx = (dx > 0) ? CoreMath.ONE : ((dx < 0) ? -CoreMath.ONE : 0);
        int sdy = (dy > 0) ? CoreMath.ONE : ((dy < 0) ? -CoreMath.ONE : 0);
        
        if (dx == 0 && dy == 0) {
            // Single point
            transform.translate(x1, y1);
            internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
        }
        else if (dy == 0) {
            // Horizontal line
            if (dx < 0) {
                transform.translate(x2, y2);
            }
            else {
                transform.translate(x1, y1);
            }
            if (drawLastPixel) {
                dxabs += CoreMath.ONE;
            }
            internalFillRectFixedPoint(dxabs, CoreMath.ONE);
        }
        else if (dx == 0) {
            // Vertical line
            if (dy < 0) {
                transform.translate(x2, y2);
            }
            else {
                transform.translate(x1, y1);
            }
            if (drawLastPixel) {
                dyabs += CoreMath.ONE;
            }
            internalFillRectFixedPoint(CoreMath.ONE, dyabs);
        }
        else if (dx == dy) {
            // Diagonal
            int iterations = CoreMath.intDivCeil(dxabs, CoreMath.ONE);
            transform.translate(x1, y1);
            if (drawLastPixel) {
                iterations++;
            }
            while (iterations-- > 0) {
                internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
                transform.translate(sdx, sdy);
            }
        }
        else {
            transform.translate(x1, y1);
            
            // Naive line drawing.
            // Not as pretty as it could be, and not as fast as it could be,
            // Also it looks bad with COMPOSITE_ADD because it draws some pixels multiple
            // times.
            internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
            int ldx, ldy, limit;
            
            if (dxabs > dyabs) {
                // Line is more horizontal than vertical
                ldx = sdx;
                ldy = CoreMath.div(dy, dxabs);
                limit = dxabs;
            }
            else {
                // Line is more vertical than horizontal
                ldx = CoreMath.div(dx, dyabs);
                ldy = sdy;
                limit = dyabs;
            }
            if (!drawLastPixel) {
                limit -= CoreMath.ONE;
            }
            for (int i = 0; i <= limit; i+=CoreMath.ONE) {
                transform.translate(ldx, ldy);
                internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
            }
            
            /*
            int d = (int)CoreMath.sqrt(CoreMath.mul(dx, dx) + CoreMath.mul(dy, dy));
            if (d >= CoreMath.ONE) {
                int dxp = CoreMath.div(dx, d);
                int dyp = CoreMath.div(dy, d);
                int iterations = CoreMath.toIntCeil(d);
                for (int i = 0; i < iterations; i++) {
                    internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
                    transform.translate(dxp, dyp);
                }
            }
            else {
                internalFillRectFixedPoint(CoreMath.ONE, CoreMath.ONE);
            }
            */
        }
        
        popTransform();
    }
    
    
    /**
        Draws a rectangle using the current color. This method draws
        rectangles at integer coordinates. 
        <p>
        Note, this method is different from
        java.awt.Graphics.drawRect() which draws a rectangle with 
        a width of (w+1) and a height of (h+1).
    */
    public void drawRect(int x, int y, int w, int h) {
        fillRect(x, y, w, 1);
        fillRect(x, y + h - 1, w, 1);
        fillRect(x, y, 1, h);
        fillRect(x + w - 1, y, 1, h);
    }


    /**
        Fills the entire surface with the current color. 
        Same as calling <code>fillRect(0, 0, surfaceWidth, surfaceHeight)</code>.
    */
    public void fill() {
        fillRect(0, 0, surfaceWidth, surfaceHeight);
    }
    
    
    /**
        Fills a rectangle with the current color. 
    */
    public void fillRect(int x, int y, int w, int h) {
        fillRectFixedPoint(CoreMath.toFixed(x), CoreMath.toFixed(y),
            CoreMath.toFixed(w), CoreMath.toFixed(h));
    }
    
    
    /**
        Fills a rectangle with the current color. 
    */
    public void fillRect(double x, double y, double w, double h) {
        fillRectFixedPoint(CoreMath.toFixed(x), CoreMath.toFixed(y),
            CoreMath.toFixed(w), CoreMath.toFixed(h));
    }
    
    
    /**
        Fills a rectangle (at fixed-point coordinates) with the current color. 
    */
    public void fillRectFixedPoint(int fx, int fy, int fw, int fh) {
        if (srcAlphaPremultiplied == 0 || fw == 0 || fh == 0) {
            return;
        }
        
        pushTransform();
        transform.translate(fx, fy);
        
        int type = transform.getType();
        if ((type & Transform.TYPE_ROTATE) != 0) {
            internalFillRotatedRect(fw, fh);
        }
        else {
            int x = transform.getTranslateX();
            int y = transform.getTranslateY();
            int w = CoreMath.mul(transform.getScaleX(), fw);
            int h = CoreMath.mul(transform.getScaleY(), fh);
            if (fractionalMetrics && (
                CoreMath.fracPart(x) != 0 || 
                CoreMath.fracPart(y) != 0 || 
                CoreMath.fracPart(x) != 0 || 
                CoreMath.fracPart(y) != 0))
            {
                internalFillRectFixedPoint(fw, fh);
            }
            else {
                internalFillRect(fw, fh);
            }
        }
        
        popTransform();
    }
    
    
    /**
        Draws the bounds of the current object. For debugging.
    */
    private void debugDrawObjectBounds() {
        int x = objectX;
        int y = objectY;
        int w = objectWidth;
        int h = objectHeight;
        
        // Save state
        pushTransform();
        int color = getColor();
        int alpha = getAlpha();
        
        // Draw
        clearTransform();
        setColor(0x000000);
        setAlpha(0xff);
        drawRect(x, y, w, h);
        
        // Restore state
        popTransform();
        setColor(color, true);
        setAlpha(alpha);
        objectX = x;
        objectY = y;
        objectWidth = w;
        objectHeight = h;
    }
    
    
    //
    // String rendering
    //
    
    
    public void drawString(String str) {
        
        if (str == null || str.length() == 0 || alpha == 0) {
            return;
        }
        
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        
        pushTransform();
        int nextIndex = font.getCharIndex(str.charAt(0));
        for (int i = 0; i < str.length(); i++) {
            
            int index = nextIndex;
            int pos = font.charPositions[index];
            int charWidth = font.charPositions[index+1] - pos;
            
            drawImage(font.getImage(), pos, 0, charWidth, font.getHeight());
            
            if (i < str.length() - 1) {
                nextIndex = font.getCharIndex(str.charAt(i + 1));
                int dx = charWidth + font.getKerning(index, nextIndex);
                transform.translate(CoreMath.toFixed(dx), 0);
            }
        }
        popTransform();
    }
    
    
    /**
        Internal method used to clip the TextField. 
        The problem is the clip is limited to View Space, but the 
        TextField needs clipping to occur in Local Space.
        This method can be removed if, in future versions,
        CoreGraphics objects can be clipped in Local Space.
    */
    public void drawChar(char ch, int maxWidth) {
        
        if (alpha == 0 || maxWidth <= 0) {
            return;
        }
        
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
    
        int index = font.getCharIndex(ch);
        int pos = font.charPositions[index];
        int charWidth = font.charPositions[index+1] - pos;
        int w = Math.min(charWidth, maxWidth);
        
        drawImage(font.getImage(), pos, 0, w, font.getHeight());
    }
    
    
    //
    // String rendering - convenience methods
    //
    
    
    public void drawString(String str, int x, int y) {
        if (str == null || str.length() == 0 || alpha == 0) {
            return;
        }
        
        pushTransform();
        transform.translate(CoreMath.toFixed(x), CoreMath.toFixed(y));
        drawString(str);
        popTransform();
    }
    
    
    public void drawScaledString(String str, int x, int y, int w, int h) {
        if (str == null || str.length() == 0 || w == 0 || h == 0 || alpha == 0) {
            return;
        }
        
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        
        int originalWidth = font.getStringWidth(str);
        int originalHeight = font.getHeight();
        
        if (originalWidth == 0 || originalHeight == 0) {
            return;
        }
        
        int scaleX = CoreMath.toFixed(w) / originalWidth;
        int scaleY = CoreMath.toFixed(h) / originalHeight;
        
        pushTransform();
        transform.translate(CoreMath.toFixed(x), CoreMath.toFixed(y));
        transform.scale(scaleX, scaleY);
        drawString(str);
        popTransform();
    }
    
    
    public void drawRotatedString(String str, int x, int y, int w, int h, int angle) {
        drawRotatedString(str, x, y, w, h, CoreMath.cos(angle), CoreMath.sin(angle));
    }
    
    
    public void drawRotatedString(String str, int x, int y, int w, int h, 
        int cosAngle, int sinAngle) 
    {
        if (str == null || str.length() == 0 || alpha == 0) {
            return;
        }
        
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        
        int originalWidth = font.getStringWidth(str);
        int originalHeight = font.getHeight();
        
        if (originalWidth == 0 || originalHeight == 0) {
            return;
        }
        
        int fw = CoreMath.toFixed(w);
        int fh = CoreMath.toFixed(h);
        int fSrcWidth = CoreMath.toFixed(originalWidth);
        int fSrcHeight = CoreMath.toFixed(originalHeight);
        int scaleX = fw / originalWidth;
        int scaleY = fh / originalHeight;
        
        pushTransform();
        transform.translate(CoreMath.toFixed(x), CoreMath.toFixed(y));
        transform.scale(scaleX, scaleY);
        
        transform.translate(fSrcWidth / 2, fSrcHeight / 2);
        transform.rotate(cosAngle, sinAngle);
        transform.translate(-fSrcWidth / 2, -fSrcHeight / 2);
        
        drawString(str);
        popTransform();
    }
    
    
    // 
    // Image rendering
    //
    
    
    /**
        Checks if the image arguments are valid. The image must be non-null and
        the source bounds must be within the image bounds. The width and height
        of the source bounds can be zero, in which case nothing is drawn.
        If the arguments are not valid, this method throws an IllegalArgumentException.
    */
    private void validateImage(CoreImage image, int srcX, int srcY, int srcWidth, int srcHeight) {
        if (image == null) {
            throw new IllegalArgumentException("CoreImage is null");
        }
        else if (srcX < 0 || srcY < 0 ||
            srcWidth < 0 || srcHeight < 0 || 
            srcX + srcWidth > image.width ||
            srcY + srcHeight > image.height)
        {
            throw new IllegalArgumentException("CoreImage source bounds outside of image bounds");
        }
    }
    
    
    public void drawImage(CoreImage image) {
        if (image != null) {
            drawImage(image, 0, 0, image.width, image.height);
        }
    }
    
    
    public void drawImage(CoreImage image, 
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        int type = transform.getType();
        
        if (type == Transform.TYPE_IDENTITY || type == Transform.TYPE_TRANSLATE) {
            int x = transform.getTranslateX();
            int y = transform.getTranslateY();
            if (fractionalMetrics && (CoreMath.fracPart(x) != 0 || CoreMath.fracPart(y) != 0)) { 
                internalDrawScaledImage(image, srcX, srcY, srcWidth, srcHeight);
            }
            else {
                internalDrawImage(image, srcX, srcY, srcWidth, srcHeight);
            }
        }
        else if ((type & Transform.TYPE_ROTATE) != 0) {
            internalDrawRotatedImage(image, srcX, srcY, srcWidth, srcHeight);
        }
        else {
            internalDrawScaledImage(image, srcX, srcY, srcWidth, srcHeight);
        }
    }


    //
    // Image rendering - convenience methods
    //
    
    
    /**
        Draws an image at a specific location. The image is drawn using 
        the current clip, transform, alpha value, and composite method.
        If the image is null, no action is taken and no exception is thrown.
        @param image the image to draw. 
        @param x the x coordinate.
        @param y the y coordinate.
    */
    public void drawImage(CoreImage image, int x, int y) {
        if (image != null) {
            drawImage(image, x, y, 0, 0, image.width, image.height);
        }
    }
    
    
    public void drawImage(CoreImage image, int x, int y,
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        pushTransform();
        
        int fx = CoreMath.toFixed(x);
        int fy = CoreMath.toFixed(y);
        
        transform.translate(fx, fy);
        drawImage(image, srcX, srcY, srcWidth, srcHeight);
        
        popTransform();
    }
    
    
    public void drawScaledImage(CoreImage image, int x, int y, int w, int h) {
        if (image != null) {
            drawScaledImage(image, x, y, w, h, 0, 0, image.width, image.height);
        }
    }
    
    
    public void drawScaledImage(CoreImage image, int x, int y, int w, int h,
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        pushTransform();
        
        int fx = CoreMath.toFixed(x);
        int fy = CoreMath.toFixed(y);
        transform.translate(fx, fy);
        
        int type = transform.getType();
        
        if (type == Transform.TYPE_IDENTITY || type == Transform.TYPE_TRANSLATE) {
            // Scale by exact dimensions
            internalDrawScaledImage(image, w, h, srcX, srcY, srcWidth, srcHeight);
        }
        else {
            // Scale by value
            int fScaleX = CoreMath.toFixed(w) / srcWidth;
            int fScaleY = CoreMath.toFixed(h) / srcHeight;
            transform.scale(fScaleX, fScaleY);
            drawImage(image, srcX, srcY, srcWidth, srcHeight);
        }
        
        popTransform();
    }
    
    
    /**
        Draws a rotated and scaled image. The image is rotated around it's center.
        @param angle a fixed-point angle, typically in the range from 0 to 
            2 * CoreMath.PI.
    */
    public void drawRotatedImage(CoreImage image, int x, int y, int w, int h, int angle) {
        if (image != null) {
            drawRotatedImage(image, x, y, w, h, CoreMath.cos(angle), CoreMath.sin(angle),
                0, 0, image.width, image. height);
        }
    }
    
    
    /**
        Draws a rotated and scaled image. The image is rotated around it's center.
        @param angle a fixed-point angle, typically in the range from 0 to 
            2 * CoreMath.PI.
    */
    public void drawRotatedImage(CoreImage image, int x, int y, int w, int h, int angle,
        int srcX, int srcY, int srcWidth, int srcHeight)
    {
        drawRotatedImage(image, x, y, w, h, CoreMath.cos(angle), CoreMath.sin(angle),
            srcX, srcY, srcWidth, srcHeight);
    }
    
    
    /**
        Draws a rotated and scaled image using pre-computed cosine and sine
        of the angle. The image is rotated around it's center.
        @param cosAngle The fixed-point cosine of the angle.
        @param sinAngle The fixed-point sine of the angle.
    */
    public void drawRotatedImage(CoreImage image, int x, int y, int w, int h, 
        int cosAngle, int sinAngle) 
    {
        if (image != null) {
            drawRotatedImage(image, x, y, w, h, cosAngle, sinAngle,
                0, 0, image.width, image. height);
        }
    }
     
    
    /**
        Draws a rotated and scaled image using pre-computed cosine and sine
        of the angle. The image is rotated around it's center.
        @param cosAngle The fixed-point cosine of the angle.
        @param sinAngle The fixed-point sine of the angle.
    */
    public void drawRotatedImage(CoreImage image, int x, int y, int w, int h,
        int cosAngle, int sinAngle,
        int srcX, int srcY, int srcWidth, int srcHeight)
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        pushTransform();
        
        int fx = CoreMath.toFixed(x);
        int fy = CoreMath.toFixed(y);
        int fw = CoreMath.toFixed(w);
        int fh = CoreMath.toFixed(h);
        int fSrcWidth = CoreMath.toFixed(srcWidth);
        int fSrcHeight = CoreMath.toFixed(srcHeight);
        int fScaleX = fw / srcWidth;
        int fScaleY = fh / srcHeight;
        
        transform.translate(fx, fy);
        transform.scale(fScaleX, fScaleY);
        
        transform.translate(fSrcWidth / 2, fSrcHeight / 2);
        transform.rotate(cosAngle, sinAngle);
        transform.translate(-fSrcWidth / 2, -fSrcHeight / 2);
        
        drawImage(image, srcX, srcY, srcWidth, srcHeight);
        
        popTransform();
    }
        
    
    //
    // Image rendering - internal. This is where actual rendering occurs.
    // (normal, scaled, and rotated/sheared)
    //
        

    private void internalDrawImage(CoreImage image,
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        int x = CoreMath.toInt(transform.getTranslateX());
        int y = CoreMath.toInt(transform.getTranslateY());
        
        clipObject(x, y, srcWidth, srcHeight);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        int[] srcData = image.getData();
        int srcScanSize = image.width;
        int surfaceOffset = objectX + objectY * surfaceWidth;
        int u = ((objectX - x) << 16);
        int v = ((objectY - y) << 16);          
        int srcOffset = srcX + (u >> 16) + (srcY + (v >> 16)) * srcScanSize;

        if ((alpha == 0xff) && (compositeIndex == COMPOSITE_SRC || 
            (compositeIndex == COMPOSITE_SRC_OVER && image.isOpaque)))
        {
            // Fatest case - don't use the compositor.
            // Great optimization for background rendering.
            for (int j = 0; j < objectHeight; j++) {
                System.arraycopy(srcData, srcOffset, surfaceData, surfaceOffset, objectWidth);
                srcOffset += srcScanSize;
                surfaceOffset += surfaceWidth;
            }
        }
        else {
            composite.blend(srcData, srcScanSize, image.isOpaque, 
                srcX, srcY, srcWidth, srcHeight, srcOffset,
                u, v,
                (1 << 16), 0,
                false,
                false, alpha,
                surfaceData, surfaceWidth, surfaceOffset, objectWidth, objectHeight);
          
            /*for (int j = 0; j < objectHeight; j++) {
                composite.blend(srcData, srcScanSize, image.isOpaque, 
                    srcX, srcY, srcWidth, srcHeight, srcOffset,
                    u, v,
                    (1 << 16), 0,
                    false,
                    false, alpha,
                    surfaceData, surfaceOffset, objectWidth);
                
                v += (1 << 16);
                surfaceOffset += surfaceWidth;
                srcOffset += srcScanSize;
            }
            */
        }
    }
    
    
    /**
        Draw scaled image by exact dimensions.
        
        I'm not sure this is necessary - the other internalDrawScaledImage()
        may be exact - but this method was designed to get predictable
        results from CoreImage.scale(), which is used in other parts of the
        engine.
    */
    private void internalDrawScaledImage(CoreImage image, int w, int h,
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0 || w == 0 || h == 0) {
            return;
        }
        
        int fW = CoreMath.toFixed(w);
        int fH = CoreMath.toFixed(h);
        int du = CoreMath.div(CoreMath.toFixed(srcWidth), fW);
        int dv = CoreMath.div(CoreMath.toFixed(srcHeight), fH);
    
        internalDrawScaledImage(image, fW, fH, du, dv,
            srcX, srcY, srcWidth, srcHeight);
    }

    
    /**
        Draw scaled image by scale value.
    */
    private void internalDrawScaledImage(CoreImage image, 
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        int sx = transform.getScaleX();
        int sy = transform.getScaleY();
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0 || sx == 0 || sy == 0) {
            return;
        }
        
        int fW = sx * srcWidth;
        int fH = sy * srcHeight;
        int du;
        int dv;
        
        if (!fractionalMetrics) {
            fW = CoreMath.intPart(fW);
            fH = CoreMath.intPart(fH);
            
            if (fW == 0 || fH == 0) {
                return;
            }
        
            du = CoreMath.div(CoreMath.toFixed(srcWidth), fW);
            dv = CoreMath.div(CoreMath.toFixed(srcHeight), fH);
        }
        else {
            du = CoreMath.div(CoreMath.ONE, sx);
            dv = CoreMath.div(CoreMath.ONE, sy);
        }
    
        internalDrawScaledImage(image, fW, fH, du, dv,
            srcX, srcY, srcWidth, srcHeight);
    }
        
        
    private void internalDrawScaledImage(CoreImage image,
        int fW, int fH, int du, int dv,
        int srcX, int srcY, int srcWidth, int srcHeight)
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0 || fW == 0 || fH == 0) {
            return;
        }
        
        int fX = transform.getTranslateX();
        int fY = transform.getTranslateY();
        
        if (!fractionalMetrics) {
            fX = CoreMath.intPart(fX);
            fY = CoreMath.intPart(fY);
        }
        
        int x = CoreMath.toIntFloor(fX);
        int y = CoreMath.toIntFloor(fY);
        int w = CoreMath.toIntCeil(fW + CoreMath.fracPart(fX));
        int h = CoreMath.toIntCeil(fH + CoreMath.fracPart(fY));
        
        clipObject(x, y, w, h);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        int[] srcData = image.getData();
        int srcScanSize = image.width;
        int surfaceOffset = objectX + objectY * surfaceWidth;
        int u = CoreMath.mul(CoreMath.toFixed(objectX) - fX, du);
        int v = CoreMath.mul(CoreMath.toFixed(objectY) - fY, dv);
        
        // ??? Removed - it caused an out-of-bounds error in Milpa when scaling around 2X
        //if (bilinear) {
        //    u += ((du - CoreMath.ONE) >> 1);
        //    v += ((dv - CoreMath.ONE) >> 1);
        //}
        
        for (int j = 0; j < objectHeight; j++) {
            int srcOffset = srcX + (u >> 16) + (srcY + (v >> 16)) * srcScanSize;
            
            composite.blend(srcData, srcScanSize, image.isOpaque, 
                srcX, srcY, srcWidth, srcHeight, srcOffset,
                u, v,
                du, 0,
                false,
                bilinear, alpha,
                surfaceData, surfaceWidth, surfaceOffset, objectWidth, 1);
            
            v += dv;
            surfaceOffset += surfaceWidth;            
        }
    }
    
    
    private void internalDrawRotatedImage(CoreImage image, 
        int srcX, int srcY, int srcWidth, int srcHeight) 
    {
        if (Build.DEBUG) validateImage(image, srcX, srcY, srcWidth, srcHeight);
        
        if (alpha == 0 || srcWidth == 0 || srcHeight == 0) {
            return;
        }
        
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        
        if (!fractionalMetrics) {
            x1 = CoreMath.intPart(x1);
            y1 = CoreMath.intPart(y1);
        }
        
        // Find the bounding rectangle
        
        int x2 = transform.getScaleX() * srcWidth;
        int y2 = transform.getShearY() * srcWidth;
        int x3 = transform.getShearX() * srcHeight;
        int y3 = transform.getScaleY() * srcHeight;
        
        int x4 = x1 + x2 + x3;
        int y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        int boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        int boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        int boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        int boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        int boundsX = CoreMath.toIntFloor(boundsX1) - 1;
        int boundsY = CoreMath.toIntFloor(boundsY1) - 1;
        int boundsW = CoreMath.toIntCeil(boundsX2) - boundsX + 2;
        int boundsH = CoreMath.toIntCeil(boundsY2) - boundsY + 2;
        
        // Clip
        
        clipObject(boundsX, boundsY, boundsW, boundsH);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        // Debug: draw bounds
        //debugDrawObjectBounds();
        
        // Calc deltas
    
        int duX;
        int dvX;
        int duY;
        int dvY;
        int type = transform.getType();
        int uY = CoreMath.toFixed(objectX) - x1;
        int vY = CoreMath.toFixed(objectY) - y1;
        
        if ((type & Transform.TYPE_ROTATE) != 0) {
            int det = transform.getDeterminant();
            if (det == 0) {
                // Determinant is 0
                return;
            }
            duX = CoreMath.div(transform.getScaleY(), det);
            dvX = CoreMath.div(-transform.getShearY(), det);
            duY = CoreMath.div(-transform.getShearX(), det);
            dvY = CoreMath.div(transform.getScaleX(), det);
            
            int newUY = (int)(((long)uY * transform.getScaleY() -
                (long)vY * transform.getShearX()) / det);
            int newVY = (int)(((long)vY * transform.getScaleX() - 
                (long)uY * transform.getShearY()) / det);
            
            uY = newUY;
            vY = newVY;
        }
        else if ((type & Transform.TYPE_SCALE) != 0) {
            int sx = transform.getScaleX();
            int sy = transform.getScaleY();
            if (sx == 0 || sy == 0) {
                // Determinant is 0
                return;
            }
            duX = CoreMath.div(CoreMath.ONE, sx);
            dvX = 0;
            duY = 0;
            dvY = CoreMath.div(CoreMath.ONE, sy);
            uY = CoreMath.div(uY, sx);
            vY = CoreMath.div(vY, sy);
        }
        else {
            duX = CoreMath.ONE;
            dvX = 0;
            duY = 0;
            dvY = CoreMath.ONE;
        }
        
        if (bilinear) {
            // ??? Not sure if this is right - it's based off DrawScaled. It looks correct in Milpa.
            uY += ((duX - CoreMath.ONE) >> 1);
            vY += ((dvY - CoreMath.ONE) >> 1);
        }
        else {
            uY += CoreMath.ONE >> 1;
            vY += CoreMath.ONE >> 1;
        }
        
        // Start Render
        int[] srcData = image.getData();
        int srcScanSize = image.width;
        int surfaceOffset = objectX + (objectY-1) * surfaceWidth;
        int fSrcWidth = CoreMath.toFixed(srcWidth);
        int fSrcHeight = CoreMath.toFixed(srcHeight);
        int lowerLimit = bilinear ? -0xff00 : 0;
        
        for (int j = 0; j < objectHeight; j++) {
            
            surfaceOffset += surfaceWidth;
            int u = uY;
            int v = vY;
            
            // Calc uY and vY for the next iteration
            uY += duY;
            vY += dvY;
            
            int startX = 0;
            int endX = objectWidth - 1;
            
            // Scan convert - left edge
            if (u < lowerLimit) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (u > fSrcWidth - 1) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fSrcWidth - 1) - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            
            if (v < lowerLimit) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (v > fSrcHeight - 1) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fSrcHeight - 1) - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            
            // Scan convert - right edge
            int u2 = u + (endX - startX + 1) * duX;
            if (u2 < lowerLimit) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - u2, duX);
                    endX += n;
                }
            }
            else if (u2 > fSrcWidth - 1) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fSrcWidth - 1) - u2, duX);
                    endX += n;
                }
            }
            
            int v2 = v + (endX - startX + 1) * dvX;
            if (v2 < lowerLimit) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - v2, dvX);
                    endX += n;
                }
            }
            else if (v2 > fSrcHeight - 1) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fSrcHeight - 1) - v2, dvX);
                    endX += n;
                }
            }
            
            int srcOffset = -1;
            composite.blend(srcData, srcScanSize, image.isOpaque, 
                srcX, srcY, srcWidth, srcHeight, srcOffset,
                u, v,
                duX, dvX,
                true,
                bilinear, alpha,
                surfaceData, surfaceWidth, surfaceOffset + startX, endX - startX + 1, 1);
            
        }
    }
    
    
    //
    // Rectangle filling
    //
    
    
    private void internalFillRect(int fw, int fh) {
        if (srcAlphaPremultiplied == 0 || fw == 0 || fh == 0) {
            return;
        }
        
        int x = CoreMath.toInt(transform.getTranslateX());
        int y = CoreMath.toInt(transform.getTranslateY());
        int w = CoreMath.toIntCeil(CoreMath.mul(transform.getScaleX(), fw));
        int h = CoreMath.toIntCeil(CoreMath.mul(transform.getScaleY(), fh));
        
        clipObject(x, y, w, h);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        int offset = objectX + objectY * surfaceWidth;
        for (int j = 0; j < objectHeight; j++) {
            if (compositeIndex == COMPOSITE_SRC || 
                (compositeIndex == COMPOSITE_SRC_OVER && srcAlphaPremultiplied == 0xff)) 
            {
                for (int i = 0; i < objectWidth; i++) {
                    surfaceData[offset + i] = srcColorPremultiplied;
                }
            }
            else {
                for (int i = 0; i < objectWidth; i++) {
                    composite.blend(surfaceData, offset + i, srcColor, srcAlphaPremultiplied);
                }
            }
            offset+=surfaceWidth;
        }
    }
    
    
    // Only called when fractionalMetrics is true or translations is guaranteed to be
    // an integer
    private void internalFillRectFixedPoint(int fw, int fh) {
        
        // Find the bounding rectangle
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        int x2 = x1 + CoreMath.mul(transform.getScaleX(), fw);
        int y2 = y1 + CoreMath.mul(transform.getScaleY(), fh);
        
        int boundsX1 = Math.min(x1, x2);
        int boundsY1 = Math.min(y1, y2);
        int boundsX2 = Math.max(x1, x2);
        int boundsY2 = Math.max(y1, y2);
        
        int boundsX = CoreMath.toIntFloor(boundsX1);
        int boundsY = CoreMath.toIntFloor(boundsY1);
        int boundsW = CoreMath.toIntCeil(boundsX2) - boundsX;
        int boundsH = CoreMath.toIntCeil(boundsY2) - boundsY;
        
        // Clip
        clipObject(boundsX, boundsY, boundsW, boundsH);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        // Start Render
        int rectColor = srcColorPremultiplied & 0x00ffffff;
        int rectAlpha = srcColorPremultiplied >>> 24;
        int surfaceOffset = objectX + objectY * surfaceWidth;
        int lowerLimit = bilinear ? -0xff00 : 0;
        int wLimit = fw - CoreMath.ONE;
        int hLimit = fh - CoreMath.ONE;
        int v = CoreMath.toFixed(objectY) - boundsY1;
        
        for (int j = 0; j < objectHeight; j++) {
            int u = CoreMath.toFixed(objectX) - boundsX1;
            for (int i = 0; i < objectWidth; i++) {
                int currAlpha = rectAlpha;
                if (u < 0) {
                    currAlpha = (currAlpha * ((u >> 8) & 0xff)) >> 8;
                }
                else if (u > wLimit) {
                    currAlpha = (currAlpha * (((wLimit-u) >> 8) & 0xff)) >> 8;
                }
                if (v < 0) {
                    currAlpha = (currAlpha * ((v >> 8) & 0xff)) >> 8;
                }
                else if (v > hLimit) {
                    currAlpha = (currAlpha * (((hLimit-v) >> 8) & 0xff)) >> 8;
                }
                
                composite.blend(surfaceData, surfaceOffset + i, rectColor, currAlpha);
                
                u += CoreMath.ONE;
            }
            surfaceOffset += surfaceWidth;
            v += CoreMath.ONE;
        }
    }
    
    
    private void internalFillRotatedRect(int fw, int fh) { 
        
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        
        if (!fractionalMetrics) {
            x1 = CoreMath.intPart(x1);
            y1 = CoreMath.intPart(y1);
            fw = CoreMath.intPart(fw);
            fh = CoreMath.intPart(fh);
        }
        
        // Find the bounding rectangle
        
        int x2 = CoreMath.mul(transform.getScaleX(), fw);
        int y2 = CoreMath.mul(transform.getShearY(), fw);
        int x3 = CoreMath.mul(transform.getShearX(), fh);
        int y3 = CoreMath.mul(transform.getScaleY(), fh);
        
        int x4 = x1 + x2 + x3;
        int y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        int boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        int boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        int boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        int boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        int boundsX = CoreMath.toIntFloor(boundsX1) - 1;
        int boundsY = CoreMath.toIntFloor(boundsY1) - 1;
        int boundsW = CoreMath.toIntCeil(boundsX2) - boundsX + 2;
        int boundsH = CoreMath.toIntCeil(boundsY2) - boundsY + 2;
        
        // Clip
        
        clipObject(boundsX, boundsY, boundsW, boundsH);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        // Debug: draw bounds
        //debugDrawObjectBounds();
        
        // Calc deltas
    
        int duX;
        int dvX;
        int duY;
        int dvY;
        int type = transform.getType();
        int uY = CoreMath.toFixed(objectX) - x1;
        int vY = CoreMath.toFixed(objectY) - y1;
        
        if ((type & Transform.TYPE_ROTATE) != 0) {
            int det = transform.getDeterminant();
            if (det == 0) {
                // Determinant is 0
                return;
            }
            duX = CoreMath.div(transform.getScaleY(), det);
            dvX = CoreMath.div(-transform.getShearY(), det);
            duY = CoreMath.div(-transform.getShearX(), det);
            dvY = CoreMath.div(transform.getScaleX(), det);
            
            int newUY = (int)(((long)uY * transform.getScaleY() - 
                (long)vY * transform.getShearX()) / det);
            int newVY = (int)(((long)vY * transform.getScaleX() - 
                (long)uY * transform.getShearY()) / det);
            
            uY = newUY;
            vY = newVY;
        }
        else if ((type & Transform.TYPE_SCALE) != 0) {
            int sx = transform.getScaleX();
            int sy = transform.getScaleY();
            if (sx == 0 || sy == 0) {
                // Determinant is 0
                return;
            }
            duX = CoreMath.div(CoreMath.ONE, sx);
            dvX = 0;
            duY = 0;
            dvY = CoreMath.div(CoreMath.ONE, sy);
            uY = CoreMath.div(uY, sx);
            vY = CoreMath.div(vY, sy);
        }
        else {
            duX = CoreMath.ONE;
            dvX = 0;
            duY = 0;
            dvY = CoreMath.ONE;
        }
        
        if (bilinear) {
            // ??? Not sure if this is right - it's based off DrawScaled. It looks correct in Milpa.
            uY += ((duX - CoreMath.ONE) >> 1);
            vY += ((dvY - CoreMath.ONE) >> 1);
        }
        else {
            uY += CoreMath.ONE >> 1;
            vY += CoreMath.ONE >> 1;
        }
        
        // Start Render
        int rectColor = srcColorPremultiplied & 0x00ffffff;
        int rectAlpha = srcColorPremultiplied >>> 24;
        int surfaceOffset = objectX + (objectY-1) * surfaceWidth;
        int lowerLimit = bilinear ? -0xff00 : 0;
        
        for (int j = 0; j < objectHeight; j++) {
            
            surfaceOffset += surfaceWidth;
            int u = uY;
            int v = vY;
            
            // Calc uY and vY for the next iteration
            uY += duY;
            vY += dvY;
            
            int startX = 0;
            int endX = objectWidth - 1;
            
            // Scan convert - left edge
            if (u < lowerLimit) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (u > fw - 1) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fw - 1) - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            
            if (v < lowerLimit) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (v > fh - 1) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fh - 1) - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            
            // Scan convert - right edge
            int u2 = u + (endX - startX + 1) * duX;
            if (u2 < lowerLimit) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - u2, duX);
                    endX += n;
                }
            }
            else if (u2 > fw - 1) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fw - 1) - u2, duX);
                    endX += n;
                }
            }
            
            int v2 = v + (endX - startX + 1) * dvX;
            if (v2 < lowerLimit) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil(lowerLimit - v2, dvX);
                    endX += n;
                }
            }
            else if (v2 > fh - 1) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = CoreMath.intDivCeil((fh - 1) - v2, dvX);
                    endX += n;
                }
            }
            
         
            int wLimit = fw - CoreMath.ONE;
            int hLimit = fh - CoreMath.ONE;
            for (int i = startX; i <= endX; i++) {
                int currAlpha = rectAlpha;
                if (u < 0) {
                    currAlpha = (currAlpha * ((u >> 8) & 0xff)) >> 8;
                }
                else if (u > wLimit) {
                    currAlpha = (currAlpha * (((wLimit-u) >> 8) & 0xff)) >> 8;
                }
                if (v < 0) {
                    currAlpha = (currAlpha * ((v >> 8) & 0xff)) >> 8;
                }
                else if (v > hLimit) {
                    currAlpha = (currAlpha * (((hLimit-v) >> 8) & 0xff)) >> 8;
                }
                
                composite.blend(surfaceData, surfaceOffset + i, rectColor, currAlpha);
                
                u += duX;
                v += dvX;
            }
        }
    }
    
    //
    // Utility methods
    //
    
    
    /**
        Converts an RGB (red, green, blue) color to HSB (hue, saturation, 
        brightness). The hue saturation, and brightness are in the range
        0 - 255. The alpha value, if any, is not modified.
        
        <pre>
        int hsb = convertRGBtoHSB(rgb);
        int h = (hsb >> 16) & 0xff;
        int s = (hsb >> 8) & 0xff;
        int b = hsb & 0xff;
        </pre>
    */
    public static int convertRGBtoHSB(int argbColor) {
        
        int a = argbColor >>> 24;
        int r = (argbColor >> 16) & 0xff;
        int g = (argbColor >> 8) & 0xff;
        int b = argbColor & 0xff;
        
        int minRGB = Math.min(Math.min(r, g), b);
        int maxRGB = Math.max(Math.max(r, g), b);
        
        // Brightness
        int v = maxRGB;
        
        if (minRGB == maxRGB) {
            // Gray - no hue or saturation
            return (a << 24) | v;
        }
        
        int sum = maxRGB + minRGB;
        int diff = maxRGB - minRGB;
        
        // Saturation
        int s;
        if (diff == maxRGB) {                   
            s = 255;
        }
        else {
            s = CoreMath.intDivRound(diff << 8, maxRGB);
        }
        
        // Hue
        int h;
        if (r == maxRGB) {
            h = (g - b);
        }
        else if (g == maxRGB) {
            h = (diff << 1) + (b - r);
        }
        else { // b == maxRGB
            h = (diff << 2) + (r - g);
        }
        h = CoreMath.intDivFloor(h << 8, diff * 6) & 0xff;
        
        return (a << 24) | (h << 16) | (s << 8) | v;
    }
    
    
    /**
        Converts an HSB (hue, saturation, brightness) color to RGB 
        (red, green, blue). The alpha value, if any, is not modified.
        
        <pre>
        int hsb = convertHSBtoRGB(rgbColor);
        int h = (hsb >> 16) & 0xff;
        int s = (hsb >> 8) & 0xff;
        int v = hsb & 0xff;
        
        int rgb = HSLtoRGB(hsb);
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        </pre>
    */
    public static int convertHSBtoRGB(int ahsbColor) {
        
        int a = ahsbColor >>> 24;
        int h = (ahsbColor >> 16) & 0xff;
        int s = (ahsbColor >> 8) & 0xff;
        int v = ahsbColor & 0xff;
        
        if (s == 0) {
            // Gray
            return (a << 24) | (v << 16) | (v << 8) | v;
        }
      
        int h6 = h * 6 + 3;
        int i = h6 >> 8; // 0 .. 5
        int f = h6 - (i << 8);

        int p = s * 255;
        int q = s * f;
        int t = p - q;
        
        int r = 0;
        int g = 0;
        int b = 0;
        
        if (i == 0) { 
            g = t;
            b = p;
        }
        else if (i == 1) {
            r = q;
            b = p;
        }
        else if (i == 2) {
            r = p;
            b = t; 
        }
        else if (i == 3) { 
            r = p;
            g = q;
        }
        else if (i == 4) {
            r = t; 
            g = p;
        }
        else { 
            g = p; 
            b = q;
        }
        
        r = ((v << 16) - (v * r)) >> 16;
        g = ((v << 16) - (v * g)) >> 16;
        b = ((v << 16) - (v * b)) >> 16;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
  
}
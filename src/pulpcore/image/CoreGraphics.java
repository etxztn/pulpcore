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

package pulpcore.image;

import java.util.EmptyStackException;
import pulpcore.Build;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;

/**
    Graphics rendering routines onto a CoreImage surface. 
    <p>
    The default composite is {@link #COMPOSITE_SRC_OVER} and the surface is assumed to be opaque.
    <p>
    The clip is in view-space - not affected by the Transform.
*/
public class CoreGraphics {
    
    /** If true, images have pre-multiplied alpha */
    /* package-private */ static final boolean PREMULTIPLIED_ALPHA = false;
    
    // Line drawing options
    private static final boolean CENTER_PIXEL = true;
    private static final boolean SWAP_POINTS = true;
    
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
    
    // For the software renderer
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
    
    /** The composite method. */
    private Composite composite;
    private int compositeIndex;
    
    /** If true, bilinear filtering is used when scaling images. */
    private boolean bilinear;
    
    private boolean fractionalMetrics;
    
    /** The current alpha value. 0 is fully tranaparent, 255 is fully opaque. */
    private int alpha;
    
    /** The current color (ARGB). */
    private int srcColor;
    
    /** The alpha value of the source color multiplied by the current alpha. */
    private int srcAlphaPremultiplied;
    
    private CoreFont font;
    
    private final Transform transform = new Transform();
    private Transform[] transformStack = new Transform[16];
    private int transformStackSize = 0;
    
    /* package-private */ CoreGraphics(CoreImage surface) {
        surfaceWidth = surface.getWidth();
        surfaceHeight = surface.getHeight();
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
    
    //
    // Rendering options
    //
    
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
        setColor(Colors.BLACK);
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
        }
        else {
            this.alpha = alpha;
            srcAlphaPremultiplied = ((srcColor >>> 24) * alpha) >> 8;
        }
    }
    
    public int getAlpha() {
        return alpha;
    }
    
    /**
        Sets the current color. The color is used for drawing rectangles and lines.
        @see Colors
    */
    public void setColor(int argbColor) {
        srcColor = argbColor;
        
        if (alpha == 0xff) {
            srcAlphaPremultiplied = srcColor >>> 24;
        }
        else {
            srcAlphaPremultiplied = ((srcColor >>> 24) * alpha) >> 8;
        }
    }
    
    /**
        Returns the current color in ARGB format.
        @see Colors
    */
    public int getColor() {
        return srcColor;
    }
    
    //
    // Primitive rendering
    //
    
    /**
        Draws a line using the current color.
    */
    public void drawLine(int x1, int y1, int x2, int y2) {
        // Disable fractional metrics so that the last pixel is drawn
        boolean oldFractionalMetrics = fractionalMetrics;
        fractionalMetrics = false;
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2));
        fractionalMetrics = oldFractionalMetrics;
    }
    
    /**
        Draws a line using the current color.
    */
    public void drawLine(double x1, double y1, double x2, double y2) {
        drawLineFixedPoint(
            CoreMath.toFixed(x1),
            CoreMath.toFixed(y1),
            CoreMath.toFixed(x2),
            CoreMath.toFixed(y2));
    }
    
    /**
        Draws a line (at fixed-point coordinates) using the current color.
    */
    public void drawLineFixedPoint(int x1, int y1, int x2, int y2) {
        internalDrawLine(x1, y1, x2, y2);
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
        Same as calling {@code fillRect(0, 0, surfaceWidth, surfaceHeight)} with
        the identity transform.
    */
    public void fill() {
        int type = transform.getType();
        if (type != Transform.TYPE_IDENTITY) {
            pushTransform();
            transform.clear();
        }
        
        internalFillRect(CoreMath.toFixed(surfaceWidth), CoreMath.toFixed(surfaceHeight));
        
        if (type != Transform.TYPE_IDENTITY) {
            popTransform();
        }
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
                CoreMath.fracPart(w) != 0 || 
                CoreMath.fracPart(h) != 0))
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
        setColor(Colors.BLACK);
        setAlpha(0xff);
        drawRect(x, y, w, h);
        
        // Restore state
        popTransform();
        setColor(color);
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
            srcX + srcWidth > image.getWidth() ||
            srcY + srcHeight > image.getHeight())
        {
            throw new IllegalArgumentException("CoreImage source bounds outside of image bounds");
        }
    }
    
    public void drawImage(CoreImage image) {
        if (image != null) {
            drawImage(image, 0, 0, image.getWidth(), image.getHeight());
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
            drawImage(image, x, y, 0, 0, image.getWidth(), image.getHeight());
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
            drawScaledImage(image, x, y, w, h, 0, 0, image.getWidth(), image.getHeight());
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
                0, 0, image.getWidth(), image.getHeight());
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
                0, 0, image.getWidth(), image.getHeight());
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
        int srcScanSize = image.getWidth();
        int surfaceOffset = objectX + objectY * surfaceWidth;
        int u = ((objectX - x) << 16);
        int v = ((objectY - y) << 16);          
        int srcOffset = srcX + (u >> 16) + (srcY + (v >> 16)) * srcScanSize;

        if ((alpha == 0xff) && (compositeIndex == COMPOSITE_SRC || 
            (compositeIndex == COMPOSITE_SRC_OVER && image.isOpaque())))
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
            composite.blend(srcData, srcScanSize, image.isOpaque(), 
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
            fW = CoreMath.floor(fW);
            fH = CoreMath.floor(fH);
            
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
            fX = CoreMath.floor(fX);
            fY = CoreMath.floor(fY);
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
        int srcScanSize = image.getWidth();
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
            
            composite.blend(srcData, srcScanSize, image.isOpaque(), 
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
            x1 = CoreMath.floor(x1);
            y1 = CoreMath.floor(y1);
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
        int srcScanSize = image.getWidth();
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
            composite.blend(srcData, srcScanSize, image.isOpaque(), 
                srcX, srcY, srcWidth, srcHeight, srcOffset,
                u, v,
                duX, dvX,
                true,
                bilinear, alpha,
                surfaceData, surfaceWidth, surfaceOffset + startX, endX - startX + 1, 1);
            
        }
    }
    
    //
    // Primitive filling - internal
    //
    
    /**
        Draws a line (at fixed-point coordinates) using the current color.
        OPTIMIZE: currently there is a virtual call per pixel
    */
    private void internalDrawLine(int ox1, int oy1, int ox2, int oy2) {
        if (srcAlphaPremultiplied == 0) {
            return;
        }
        
        int x1, y1, x2, y2;
        
        if (transform.getType() == Transform.TYPE_IDENTITY) {
            x1 = ox1;
            y1 = oy1;
            x2 = ox2;
            y2 = oy2;
        }
        else {
            x1 = transform.transformX(ox1, oy1);
            y1 = transform.transformY(ox1, oy1);
            x2 = transform.transformX(ox2, oy2);
            y2 = transform.transformY(ox2, oy2);
        }
        
        if (!fractionalMetrics) {
            x1 = CoreMath.floor(x1);
            y1 = CoreMath.floor(y1);
            x2 = CoreMath.floor(x2);
            y2 = CoreMath.floor(y2);
        }
        
        int dx = x2 - x1; 
        int dy = y2 - y1;
        // Horizontal and vertical lines
        if (dx == 0 || dy == 0) {
            if (dx == 0 && dy == 0) {
                // Do nothing
                return;
            }
            else if (!fractionalMetrics) {
                pushTransform();
                transform.clear();
                transform.translate(Math.min(x1, x2), Math.min(y1, y2));
                internalFillRect(CoreMath.abs(dx) + CoreMath.ONE, CoreMath.abs(dy) + CoreMath.ONE);
                popTransform();
                return;
            }
        }
        
        if (!fractionalMetrics) {
            // Make sure the first pixel is solid
            drawPixel(CoreMath.toInt(x1), CoreMath.toInt(y1));
            
            // Draw the last pixel (otherwise it won't get drawn)
            if (x1 != x2 || y1 != y2) {
                drawPixel(CoreMath.toInt(x2), CoreMath.toInt(y2));
            }
        }
        
        // Make integer locations the center of the pixel
        if (CENTER_PIXEL) {
            x1 += CoreMath.ONE_HALF;
            y1 += CoreMath.ONE_HALF;
            x2 += CoreMath.ONE_HALF;
            y2 += CoreMath.ONE_HALF;
        }
        
        // Clip - Sutherland-Cohen algorithm
        // This is used as an optimization to prevent off-screen lines from being drawn.
        // Real clipping happens in the drawPixel() functions.
        // Using an extra 1-pixel boundary because of error I saw with the Sketch demo.
        int xmin = CoreMath.toFixed(clipX - 1);
        int xmax = xmin + CoreMath.toFixed(clipWidth + 1);
        int ymin = CoreMath.toFixed(clipY - 1);
        int ymax = ymin + CoreMath.toFixed(clipHeight + 1);
       
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
            dx = x2 - x1; 
            dy = y2 - y1;
        }
        
        if (dx != 0 || dy != 0) {
            int dxabs = CoreMath.abs(dx);
            int dyabs = CoreMath.abs(dy);
            int currAlpha;
            if (SWAP_POINTS) {
                if ((dxabs >= dyabs && x1 > x2) ||
                    (dyabs > dxabs && y1 > y2))
                {
                    int t = x1;
                    x1 = x2;
                    x2 = t;
                    t = y1;
                    y1 = y2;
                    y2 = t;
                    dx = -dx;
                    dy = -dy;
                }
            }
            
            if (dxabs >= dyabs) {
                // Line is more horizontal than vertical
                // Or a diagonal
                if (dxabs < CoreMath.ONE && CoreMath.floor(x1) == CoreMath.floor(x2)) { 
                    // One pixel
                    currAlpha = dxabs >> 8;
                    currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                    drawWuPixelHorizontal(x1, y1, currAlpha);
                }
                else {
                    // First pixel and last pixel
                    if (fractionalMetrics) {
                        currAlpha = 0xff - ((x1 >> 8) & 0xff);
                        currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                        drawWuPixelHorizontal(x1, y1, currAlpha);
                        currAlpha = ((x2 >> 8) & 0xff);
                        currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                        drawWuPixelHorizontal(x2, y2, currAlpha);
                    }
                    // Line
                    int grad = CoreMath.div(dy, dxabs);
                    int d = dx > 0 ? CoreMath.ONE : -CoreMath.ONE;
                    int limit = Math.abs(CoreMath.toIntFloor(x2) - CoreMath.toIntFloor(x1)) - 1;
                    for (int i = 0; i < limit; i++) {
                        x1 += d;
                        y1 += grad;
                        drawWuPixelHorizontal(x1, y1, srcAlphaPremultiplied);
                    }
                }
            }
            else {
                // Line is more vertical than horizontal
                if (dyabs < CoreMath.ONE && CoreMath.floor(y1) == CoreMath.floor(y2)) { 
                    // One pixel
                    currAlpha = dyabs >> 8;
                    currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                    drawWuPixelVertical(x1, y1, currAlpha);
                }
                else {
                    // First first and last pixel
                    if (fractionalMetrics) {
                        currAlpha = 0xff - ((y1 >> 8) & 0xff);
                        currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                        drawWuPixelVertical(x1, y1, currAlpha);
                        currAlpha = ((y2 >> 8) & 0xff);
                        currAlpha = (srcAlphaPremultiplied * currAlpha + 0xff) >> 8;
                        drawWuPixelVertical(x2, y2, currAlpha);
                    }
                    // Line
                    int grad = CoreMath.div(dx, dyabs);
                    int d = dy > 0 ? CoreMath.ONE : -CoreMath.ONE;
                    int limit = Math.abs(CoreMath.toIntFloor(y2) - CoreMath.toIntFloor(y1)) - 1;
                    for (int i = 0; i < limit; i++) {
                        x1 += grad;
                        y1 += d;
                        drawWuPixelVertical(x1, y1, srcAlphaPremultiplied);
                    }
                }
            }
        }
    }
    
    /**
        Draws a pixel at the specified integer (x,y) location using the current color.
        Ignores the transform. Respects the clip.
    */
    private void drawPixel(int x, int y) {
        if (x >= clipX && x < clipX + clipWidth &&
            y >= clipY && y < clipY + clipHeight)
        {
            int surfaceOffset = x + y * surfaceWidth;
            composite.blend(surfaceData, surfaceOffset, srcColor, srcAlphaPremultiplied);
        }
    }
    
    /*
        For Wu's line drawing algorithm.
        Ignores the transform. Respects the clip.
        Draws two pixels, one at (fx, floor(fy)) and another at (fx, ceil(fy))
    */
    private void drawWuPixelHorizontal(int fx, int fy, int srcAlpha) {
        if (srcAlpha <= 0) {
            return;
        }
        int x = CoreMath.toIntFloor(fx);
        if (CENTER_PIXEL) {
            fy -= CoreMath.ONE_HALF;
        }
        if (x >= clipX && x < clipX + clipWidth) {
            int y1 = CoreMath.toIntFloor(fy);
            int surfaceOffset = x + y1 * surfaceWidth;
            if (y1 >= clipY && y1 < clipY + clipHeight) {
                int wuAlpha = 0xff - ((fy >> 8) & 0xff);
                int pixelAlpha = (srcAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset, srcColor, pixelAlpha);
            }
            
            int y2 = CoreMath.toIntCeil(fy);
            if (y1 != y2 && y2 >= clipY && y2 < clipY + clipHeight) {
                int wuAlpha = ((fy >> 8) & 0xff);
                int pixelAlpha = (srcAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset + surfaceWidth, srcColor, pixelAlpha);
            }
        }
    }
    
    /*
        For Wu's line drawing algorithm.
        Ignores the transform, but respects the clip.
        Draws two pixels, one at (floor(fx), fy) and another at (ceil(fx), fy)
    */
    private void drawWuPixelVertical(int fx, int fy, int srcAlpha) {
        if (srcAlpha <= 0) {
            return;
        }
        int y = CoreMath.toIntFloor(fy);
        if (CENTER_PIXEL) {
            fx -= CoreMath.ONE_HALF;
        }
        if (y >= clipY && y < clipY + clipHeight) {
            int x1 = CoreMath.toIntFloor(fx);
            int surfaceOffset = x1 + y * surfaceWidth;
            if (x1 >= clipX && x1 < clipX + clipWidth) {
                int wuAlpha = 0xff - ((fx >> 8) & 0xff);
                int pixelAlpha = (srcAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset, srcColor, pixelAlpha);
            }
            
            int x2 = CoreMath.toIntCeil(fx);
            if (x1 != x2 && x2 >= clipX && x2 < clipX + clipWidth) {
                int wuAlpha = ((fx >> 8) & 0xff);
                int pixelAlpha = (srcAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset + 1, srcColor, pixelAlpha);
            }
        }
    }
    
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
            composite.blendRow(surfaceData, offset, srcColor, srcAlphaPremultiplied, objectWidth);
            offset+=surfaceWidth;
        }
    }
    
    // Only called when fractionalMetrics is true or translations is guaranteed to be
    // an integer
    private void internalFillRectFixedPoint(int fw, int fh) {
        
        // Scale
        fw = CoreMath.mul(transform.getScaleX(), fw);
        fh = CoreMath.mul(transform.getScaleY(), fh);
        
        // Find the bounding rectangle
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        int x2 = x1 + fw;
        int y2 = y1 + fh;
        
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
        int surfaceOffset = objectX + objectY * surfaceWidth;
        int lowerLimit = bilinear ? -0xff00 : 0;
        int wLimit = fw - CoreMath.ONE;
        int hLimit = fh - CoreMath.ONE;
        int v = CoreMath.toFixed(objectY) - boundsY1;
        for (int j = 0; j < objectHeight; j++) {
            int u = CoreMath.toFixed(objectX) - boundsX1;
            // OPTIMIZE: less virtual calls?
            int lastAlpha = -1;
            int runLength = 0;
            for (int x = 0; x < objectWidth; x++) {
                int rectAlpha = srcAlphaPremultiplied;
                if (u < 0) {
                    rectAlpha = (rectAlpha * ((u >> 8) & 0xff)) >> 8;
                }
                else if (u > wLimit) {
                    rectAlpha = (rectAlpha * (((wLimit-u) >> 8) & 0xff)) >> 8;
                }
                if (v < 0) {
                    rectAlpha = (rectAlpha * ((v >> 8) & 0xff)) >> 8;
                }
                else if (v > hLimit) {
                    rectAlpha = (rectAlpha * (((hLimit-v) >> 8) & 0xff)) >> 8;
                }
                
                if (rectAlpha == lastAlpha) {
                    runLength++;
                }
                else {
                    if (runLength > 0) {
                        composite.blendRow(surfaceData, surfaceOffset + x - runLength, 
                            srcColor, lastAlpha, runLength);
                    }
                    lastAlpha = rectAlpha;
                    runLength = 1;
                }
                
                u += CoreMath.ONE;
            }
            if (runLength > 0) {
                composite.blendRow(surfaceData, surfaceOffset + objectWidth - runLength, 
                    srcColor, lastAlpha, runLength);
            }
            
            surfaceOffset += surfaceWidth;
            v += CoreMath.ONE;
        }
    }
    
    private void internalFillRotatedRect(int fw, int fh) { 
        int x1 = transform.getTranslateX();
        int y1 = transform.getTranslateY();
        
        if (!fractionalMetrics) {
            x1 = CoreMath.floor(x1);
            y1 = CoreMath.floor(y1);
            fw = CoreMath.floor(fw);
            fh = CoreMath.floor(fh);
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
            // OPTIMIZE: less virtual calls? less work per pixel?
            int lastAlpha = -1;
            int runLength = 0;
            for (int x = startX; x <= endX; x++) {
                int rectAlpha = srcAlphaPremultiplied;
                if (u < 0) {
                    rectAlpha = (rectAlpha * ((u >> 8) & 0xff)) >> 8;
                }
                else if (u > wLimit) {
                    rectAlpha = (rectAlpha * (((wLimit-u) >> 8) & 0xff)) >> 8;
                }
                if (v < 0) {
                    rectAlpha = (rectAlpha * ((v >> 8) & 0xff)) >> 8;
                }
                else if (v > hLimit) {
                    rectAlpha = (rectAlpha * (((hLimit-v) >> 8) & 0xff)) >> 8;
                }
                
                // Trying out oval rendering.
                // This is a naive technique: lots of calcs per pixel, no anti-aliasing.
                // Needs scan converting instead.
                //int a = fw / 2;
                //int b = fh / 2;
                //int m = CoreMath.div(u-a, a);
                //int n = CoreMath.div(v-b, b);
                //if (CoreMath.mul(m, m) + CoreMath.mul(n, n) > CoreMath.ONE) {
                //    rectAlpha = 0;
                //}
                
                if (rectAlpha == lastAlpha) {
                    runLength++;
                }
                else {
                    if (runLength > 0) {
                        composite.blendRow(surfaceData, surfaceOffset + x - runLength, 
                            srcColor, lastAlpha, runLength);
                    }
                    lastAlpha = rectAlpha;
                    runLength = 1;
                }
                
                u += duX;
                v += dvX;
            }
            
            if (runLength > 0) {
                composite.blendRow(surfaceData, surfaceOffset + endX + 1 - runLength, 
                    srcColor, lastAlpha, runLength);
            }
            
            // No anti-aliasing
            //composite.blendRow(surfaceData, surfaceOffset + startX, srcColor, 
            //    srcAlphaPremultiplied, endX - startX + 1);
        }
    }
}
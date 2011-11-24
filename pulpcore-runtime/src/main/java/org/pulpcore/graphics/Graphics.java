package org.pulpcore.graphics;

import java.util.EmptyStackException;
import org.pulpcore.math.AffineTransform;
import org.pulpcore.math.Recti;

public abstract class Graphics {
    
    public enum BlendMode {
        CLEAR,
        SRC,
        DST,
        SRC_OVER,
        SRC_IN,
        SRC_ATOP,
        SRC_OUT,
        DST_OVER,
        DST_IN,
        DST_ATOP,
        DST_OUT,
        MULT,
        ADD,
    }

    public enum Interpolation {
        NEAREST_NEIGHBOR,
        BILINEAR,
    }

    /**
        Specifies that the left edge of the image should be clamped (not antialiased).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_LEFT = 1;

    /**
        Specifies that the right edge of the image should be clamped (not antialiased).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_RIGHT = 2;

    /**
        Specifies that the bottom edge of the image should be clamped (not antialiased).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_BOTTOM = 4;

    /**
        Specifies that the top edge of the image should be clamped (not antialiased).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_TOP = 8;

    /**
        Specifies that all edges of the image should be clamped (not antialiased).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_ALL = EDGE_CLAMP_LEFT | EDGE_CLAMP_RIGHT |
            EDGE_CLAMP_BOTTOM | EDGE_CLAMP_TOP;

    /**
        Specifies that all edges of the image should be antialiased (not clamped).
        @see #setEdgeClamp(int)
    */
    public static final int EDGE_CLAMP_NONE = 0;
    
    // The clip rectangle
    private int clipX;
    private int clipY;
    private int clipWidth;
    private int clipHeight;
    
    private ImageFont font;
    
    /** If true, bilinear filtering is used when scaling images. */
    private boolean bilinear;

    /** Edge clamp mask */
    private int edgeClamp;

    private BlendMode blendMode;
    
    private int color;
    private int alpha;
    
    private AffineTransform[] transformStack = null;
    private int transformStackSize = 0;
    
    public abstract int getSurfaceWidth();

    public abstract int getSurfaceHeight();

    /**
        Resets the rendering attributes for this CoreGraphics object to the
        default values:
        <ul>
            <li>No clip</li>
            <li>Identity transform (and the transform stack is cleared)</li>
            <li>color = Color.BLACK</li>
            <li>alpha = 255</li>
            <li>blendMode = BlendMode.SRC_OVER)</li>
            <li>interpolation = Interpolation.BILINEAR</li>
            <li>edgeClamp = EDGE_CLAMP_NONE</li>
            <li>font = null</li>
        </ul>
    */
    public void reset() {
        removeClip();
        setAlpha(0xff);
        setColor(Color.BLACK);
        setBlendMode(BlendMode.SRC_OVER);
        setFont(null);
        setInterpolation(Interpolation.BILINEAR);
        setEdgeClamp(EDGE_CLAMP_NONE);
        clearTransformStack();
    }

    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    /**
        Sets the edge mode for bilinear interpolated scaled images. The default is
        {@link #EDGE_CLAMP_NONE}. Valid values are a bitmask combination of
        {@link #EDGE_CLAMP_LEFT}, {@link #EDGE_CLAMP_RIGHT}, {@link #EDGE_CLAMP_BOTTOM},
        {@link #EDGE_CLAMP_TOP}. A clamp edge appears "hard", and an unclamped edge appears "soft".
        @param edgeClamp A bitmask defining how the edges of an image are rendered.
    */
    public void setEdgeClamp(int edgeClamp) {
        this.edgeClamp = edgeClamp;
    }

    /**
        Gets the edge clamp bitmask.
        @see #setEdgeClamp(int)
    */
    public int getEdgeClamp() {
        return edgeClamp;
    };
    
    
    public void setInterpolation(Interpolation interpolation) {
        this.bilinear = (interpolation != Interpolation.NEAREST_NEIGHBOR);
    }

    public Interpolation getInterpolation() {
        return bilinear ? Interpolation.BILINEAR : Interpolation.NEAREST_NEIGHBOR;
    }

    public void setFont(ImageFont font) {
        this.font = font;
    }

    public ImageFont getFont() {
        if (font == null) {
            font = ImageFont.getDefaultFont();
        }
        return font;
    }

    //
    // Transforms
    //
    
    public void clearTransformStack() {
        transformStackSize = 1;
        if (transformStack == null) {
            transformStack = new AffineTransform[16];
            for (int i = 0; i < transformStack.length; i++) {
                transformStack[i] = new AffineTransform();
            }
        }
        else {
            transformStack[0].clear();
        }
    }

    protected AffineTransform getCurrentTransform() {
        return transformStack[transformStackSize - 1];
    }

    /**
        Adds (pushes) a copy of the current transform to the top of the transform stack.
    */
    public void pushTransform() {
        if (transformStackSize == transformStack.length) {
            // Double the size of the stack
            AffineTransform[] newTransformStack = new AffineTransform[transformStack.length * 2];
            System.arraycopy(transformStack, 0, newTransformStack, 0, transformStack.length);
            for (int i = transformStack.length; i < newTransformStack.length; i++) {
                newTransformStack[i] = new AffineTransform();
            }
            transformStack = newTransformStack;
        }

        transformStack[transformStackSize].set(getCurrentTransform());
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
    }

    /**
        Returns a copy of the current transform.
    */
    public AffineTransform getTransform() {
        return new AffineTransform(getCurrentTransform());
    }

    public void transform(AffineTransform transform) {
        getCurrentTransform().concatenate(transform);
    }

    public void translate(float x, float y) {
        getCurrentTransform().translate(x, y);
    }

    public void scale(float x, float y) {
        getCurrentTransform().scale(x, y);
    }

    public void shear(float x, float y) {
        getCurrentTransform().shear(x, y);
    }

    public void rotate(float angle) {
        getCurrentTransform().rotate(angle);
    }

    public void rotate(float angle, float x, float y) {
        AffineTransform t = getCurrentTransform();
        t.translate(x, y);
        t.rotate(angle);
        t.translate(-x, -y);
    }

    /**
        Sets the current transform to a copy of the specified transform. If
        the specified transform is null, the current transform is cleared, i.e.,
        set to the identity matrix.
    */
    public void setTransform(AffineTransform newTransform) {
        getCurrentTransform().set(newTransform);
    }

    /**
        Sets the current transform to the identity matrix.
    */
    public void clearTransform() {
        getCurrentTransform().clear();
    }
    

    //
    // Clipping
    //

    public void removeClip() {
        clipX = 0;
        clipY = 0;
        clipWidth = getSurfaceWidth();
        clipHeight = getSurfaceHeight();
    }

    /**
    Sets the clip, in device coordinates. The clip is not affected by the transform.
    @param r 
    */
    public void setClip(Recti r) {
        setClip(r.x, r.y, r.width, r.height);
    }

    public void setClip(int x, int y, int w, int h) {
        removeClip();
        clipRect(x, y, w, h);
    }

    public void clipRect(Recti r) {
        clipRect(r.x, r.y, r.width, r.height);
    }

    public void clipRect(int x, int y, int w, int h) {
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

        clipX = x;
        clipY = y;
        clipWidth = w;
        clipHeight = h;
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

    /**
        Copies the current clip into the specified Rect.
    */
    public void getClip(Recti rect) {
        rect.setBounds(clipX, clipY, clipWidth, clipHeight);
    }
        
    /**
        Gets the current clip as a newly allocated Rect.
    */
    public Recti getClip() {
        Recti clip = new Recti();
        getClip(clip);
        return clip;
    }

    //
    // ARGB color
    //

    /**
    Sets the current alpha, from 0 (fully transparent) to 255 (fully opaque).
    */
    public void setAlpha(int alpha) {
        if (alpha <= 0) {
            this.alpha = 0;
        }
        else if (alpha >= 0xff) {
            this.alpha = 0xff;
        }
        else {
            this.alpha = alpha;
        }
    }

    /**
    Gets the current alpha, from 0 (fully transparent) to 255 (fully opaque).
    */
    public int getAlpha() {
        return alpha;
    }

    /**
        Sets the current colorin ARGB format. The color is used for drawing rectangles and lines.
        @see Color
    */
    public void setColor(int argbColor) {
        color = argbColor;
    }

    /**
        Returns the current color in ARGB format.
        @see Color
    */
    public int getColor() {
        return color;
    }

    //
    // Rendering
    //

    /**
        Fills the entire surface with the current color.
        Same as calling {@code fillRect(0, 0, surfaceWidth, surfaceHeight)} with
        the identity transform.
    */
    public void fill() {
        int type = getCurrentTransform().getType();
        if (type != AffineTransform.TYPE_IDENTITY) {
            pushTransform();
            clearTransform();
        }

        fillRect(getSurfaceWidth(), getSurfaceHeight());

        if (type != AffineTransform.TYPE_IDENTITY) {
            popTransform();
        }
    }

    /**
        Fills the area defined by the clip with the background color of the current drawing
        surface. For opaque surfaces, the background color is black. For surfaces with alpha,
        the background color is transparent. The current blend mode is ignored - the area
        if filled using the Porter-Duff Source rule.
    */
    public void clear() {
        BlendMode oldBlendMode = getBlendMode();
        setBlendMode(BlendMode.CLEAR);
        fill();
        setBlendMode(oldBlendMode);
    }

    /**
        Draws a line using the current color.
    */
    public abstract void drawLine(float x1, float y1, float x2, float y2);

    public void drawRect(float x, float y, float w, float h) {
        int strokeWidth = 1;
        fillRect(x, y, w, strokeWidth);
        fillRect(x, y + h - strokeWidth, w, strokeWidth);
        fillRect(x, y, 1, h);
        fillRect(x + w - strokeWidth, y, strokeWidth, h);
    }

    /**
        Fills a rectangle with the current color.
    */
    public void fillRect(float x, float y, float w, float h) {
        pushTransform();
        translate(x, y);
        fillRect(w, h);
        popTransform();
    }
    
    public abstract void fillRect(float w, float h);

    public void drawString(String str, float x, float y) {
        if (str != null && str.length() >= 0) {
            if (x == 0 && y == 0) {
                drawString(str);
            }
            else {
                pushTransform();
                translate(x, y);
                drawString(str);
                popTransform();
            }
        }
    }

    /**
        Draw an image using the current transform.
     */
    public void drawString(String str) {
        if (str != null && str.length() >= 0) {

            ImageFont currFont = getFont();

            float x = 0;
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                ImageFont.Glyph glyph = currFont.getGlyph(ch);

                drawTexture(glyph.getTexture(), x + glyph.getOffsetX(), glyph.getOffsetY());

                if (i < str.length() - 1) {
                    char nextChar = str.charAt(i + 1);
                    x += glyph.getDistanceX(nextChar);
                }
            }
        }
    }

    public void drawImage(Image image, float x, float y) {
        if (image != null) {
            if (x == 0 && y == 0) {
                drawImage(image);
            }
            else {
                pushTransform();
                translate(x, y);
                drawImage(image);
                popTransform();
            }
        }
    }

    public void drawImage(Image image) {
        if (image != null) {
            if (image.hasSingleTile()) {
                Texture texture = image.getTile(0,0);
                drawTexture(texture,  image.getTileOffsetX(), image.getTileOffsetY());
            }
            else {
                int originalClamp = getEdgeClamp();
                int w = image.getNumXTiles();
                int h = image.getNumYTiles();
                int ty = image.getTileOffsetY();
                for (int y = 0; y < h; y++) {
                    int tx = image.getTileOffsetX();
                    int rowClamp = originalClamp;
                    if (y > 0) {
                        rowClamp |= Graphics.EDGE_CLAMP_TOP;
                    }
                    if (y < h - 1) {
                        rowClamp |= Graphics.EDGE_CLAMP_BOTTOM;
                    }
                    for (int x = 0; x < w; x++) {
                        int clamp = rowClamp;
                        if (x > 0) {
                            clamp |= Graphics.EDGE_CLAMP_LEFT;
                        }
                        if (x < w - 1) {
                            clamp |= Graphics.EDGE_CLAMP_RIGHT;
                        }
                        setEdgeClamp(clamp);

                        Texture texture = image.getTile(x, y);
                        drawTexture(texture, tx, ty);
                        tx += texture.getWidth();
                    }
                    ty += image.getTile(0, y).getHeight();
                }
                setEdgeClamp(originalClamp);
            }
        }
    }

    public void drawTexture(Texture texture, float x, float y) {
        if (texture != null) {
            if (x == 0 && y == 0) {
                drawTexture(texture);
            }
            else {
                pushTransform();
                translate(x, y);
                drawTexture(texture);
                popTransform();
            }
        }
    }

    public abstract void drawTexture(Texture texture);

}

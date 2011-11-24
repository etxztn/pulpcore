package org.pulpcore.runtime.softgraphics;

import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Texture;

public class SoftTexture extends Texture {
    
    public static final Color.Format PIXEL_FORMAT = Color.Format.ARGB_PREMULTIPLIED;

    private final SoftTexture sourceTexture;
    private final int width;
    private final int height;
    private final boolean opaque;
    private final boolean mutable;
    private final int scanWidth;
    private final int dataOffset;
    private final int[] data;

    /**
     Creates a subimage of the specified image, sharing the texture data.
     */
    private SoftTexture(SoftTexture sourceTexture, int x, int y, int width, int height) {
        // Keep a reference to the root sourceTexture for caching purposes. This prevents the original
        // SoftTexture object from being removed from the cache in AppletContext, which wouldn't
        // be a good idea since the texture data is shared.
        this.sourceTexture = sourceTexture.sourceTexture != null ? sourceTexture.sourceTexture : sourceTexture;
        this.scanWidth = sourceTexture.scanWidth;
        this.dataOffset = sourceTexture.getOffset(x, y);
        this.width = width;
        this.height = height;
        this.opaque = sourceTexture.opaque;
        this.mutable = sourceTexture.mutable;
        this.data = sourceTexture.data;
    }

    /**
     Create a blank, mutable image. Opaque images will be set to BLACK, and non-opaque images
     will be set to TRANSPARENT.
     */
    public SoftTexture(int width, int height, boolean isOpaque) {
        this(width, height, isOpaque, isOpaque ? Color.BLACK : Color.TRANSPARENT);
    }

    /**
     Create a mutable image initially set to the specified background color.
     */
    public SoftTexture(int width, int height, boolean isOpaque, int argbColor) {
        this.sourceTexture = null;
        this.scanWidth = width;
        this.dataOffset = 0;
        this.width = width;
        this.height = height;
        this.opaque = isOpaque;
        this.mutable = true;
        this.data = new int[width * height];
        if (isOpaque) {
            argbColor = Color.rgb(argbColor);
        }
        if (argbColor != 0) {
            for (int i = 0; i < data.length; i++) {
                data[i] = argbColor;
            }
        }
    }

    /**
     Creates a new image. For immutable images, the texture data must not be shared with any
     other image, nor should it be modified after using this constructor.
     */
    public SoftTexture(int width, int height, boolean isOpaque, int[] data, boolean isMutable) {
        if (data == null || data.length != width * height) {
            throw new IllegalArgumentException("Invalid data length");
        }
        this.sourceTexture = null;
        this.scanWidth = width;
        this.dataOffset = 0;
        this.width = width;
        this.height = height;
        this.opaque = isOpaque;
        this.mutable = isMutable;
        this.data = data;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    private int[] cloneData() {
        int[] src = data;
        int[] dst = new int[width * height];
        int srcOffset = dataOffset;
        int dstOffset = 0;
        for (int i = 0; i < height; i++) {
            System.arraycopy(src, srcOffset, dst, dstOffset, width);
            srcOffset += scanWidth;
            dstOffset += width;
        }
        return dst;
    }

    public boolean isSubRegion() {
        return dataOffset != 0 || scanWidth != width || width * height != data.length;
    }

    private int getOffset(int x, int y) {
        return dataOffset + x + y * scanWidth;
    }

    @Override
    public boolean isTransparent(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            return true;
        }
        else if (isOpaque()) {
            return false;
        }
        else {
            return (getRawPixel(x, y) >>> 24) == 0;
        }
    }

    @Override
    public int getARGB(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            return 0;
        }
        else {
            return Color.convert(PIXEL_FORMAT, Color.Format.ARGB, getRawPixel(x, y));
        }
    }  

    @Override
    public Texture createMutableCopy() {
        return new SoftTexture(width, height, opaque, cloneData(), true);
    }

    @Override
    public Texture createImmutableCopy() {
        if (isMutable() || isSubRegion()) {
            return new SoftTexture(width, height, opaque, cloneData(), false);
        }
        else {
            return this;
        }
    }

    @Override
    public Graphics createGraphics() {
        if (isMutable()) {
            return new SoftGraphics(this);
        }
        else {
            throw new RuntimeException("Texture is not mutable: " + this);
        }
    }

    @Override
    public Texture getSubTexture(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > getWidth() || y + height > getHeight()) {
            throw new IllegalArgumentException("Subregion region not valid");
        }
        return new SoftTexture(this, x, y, width, height);
    }

    public int[] getData() {
        if (isMutable()) {
            return data;
        }
        else {
            return null;
        }
    }

    /**
     Gets a premultiplied ARGB color without checking if the (x,y) values are correct.
     */
    public int getRawPixel(int x, int y) {
        return data[getOffset(x, y)];
    }

    //
    // For SoftGraphics
    //

    /* package private */ int[] getRawData() {
        return data;
    }

    /* package private */ int getDataOffset() {
        return dataOffset;
    }

    /* package private */ int getScanWidth() {
        return scanWidth;
    }
}

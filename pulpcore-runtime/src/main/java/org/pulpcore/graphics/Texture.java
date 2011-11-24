package org.pulpcore.graphics;

import org.pulpcore.runtime.Context;

/**
 A Texture represents a rectangular array of pixels. Textures
 may be immutable or mutable, and mutable Textures can be rendered onto.
 <p>
 Textures are automatically created when an {@link Image} is created. Alternatively, you can
 create a Texture by using {@link Context#loadTexture(java.lang.String) } or
 {@link Context#createTexture(int, int, boolean) }.
 */
public abstract class Texture {

    /**
    Gets the width of this Texture.
    */
    public abstract int getWidth();

    /**
    Gets the height of this Texture.
    */
    public abstract int getHeight();

    /**
    Returns true if this Texture is opaque.
    */
    public abstract boolean isOpaque();

    /**
    Returns true if this Texture is mutable, that is, it's contents can change by calling
    createGraphics() and drawing onto it.
    */
    public abstract boolean isMutable();

    /**
    Checks if the pixel at the specified location is transparent.
    @return true if the pixel is transparent or if the location is out of bounds.
    */
    public abstract boolean isTransparent(int x, int y);

    /**
    Gets the ARGB color at the specified location.
    @throws IllegalArgumentException if the specified location is out of bounds.
    @return the ARGB color in non-premultiplied format.
    */
    public abstract int getARGB(int x, int y);

    /**
    Creates a new Graphics context for drawing onto this Texture, assuming the Texture is mutable.
    If this Texture is not mutable ({@link #isMutable()} returns false) then this method
    returns null.
    */
    public abstract Graphics createGraphics();

    /**
    Creates a Texture from a specific region of this image. The texture data is shared
    between this image and the returned Texture.
    @param x The x location of the region.
    @param y The y location of the region.
    @param width The width of the region.
    @param height The height of the region.
    @throws IllegalArgumentException if the region is not valid.
    */
    public abstract Texture getSubTexture(int x, int y, int width, int height);
    
    /**
    Creates a mutable copy of this Texture. The texture data is not shared.
    @return The newly created mutable Texture.
    */
    public abstract Texture createMutableCopy();

    /**
    Creates an immutable copy of this Texture. If this Texture is mutable, or this Texture is a
    subtexture of an immutable image, new Texture data is created. Otherwise, (if this
    Texture is immutable and not a subregion of another Texture), the same instance if returned.
    Immutable textures may have certain optimizations applied to them in the scene graph.
    @return The immutable Texture.
    */
    public abstract Texture createImmutableCopy();
}

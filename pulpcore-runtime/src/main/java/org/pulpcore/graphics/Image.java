/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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
package org.pulpcore.graphics;

import org.pulpcore.runtime.Context;

/**
 Image is a container for Textures. An Image can be composed of a single Texture or an
 array of tiled Textures. Tiled Textures are useful in environment where an Image size is larger
 than the maximum Texture size for the platform.
 <p>
 The Texture can be offset at an (x,y) location and the Image can have dimensions
 larger than the Texture itself (which in effect adds transparent space around the visible
 portion of the Image).
 <p>
 If the Image is mutable, it's internal pixels can change. However, the other fields of Image
 (width, height, etc.) never change.
 */
public class Image {
    
    private static final int CHANGE_TEXTURE_NONE = 0;
    private static final int CHANGE_TEXTURE_MUTABLE = 1;
    private static final int CHANGE_TEXTURE_IMMUTABLE = 2;

    /**
    Loads an Image. If the Image was previously loaded, the same object instance is returned.
    If the image asset is not found, a "broken" image is returned - this method never returns null.
     */
    public static Image load(String imageAsset) {
        return Context.getContext().loadImage(imageAsset);
    }

    private static int getWidth(Texture[] tiles, int numXTiles, int numYTiles) {
        if (tiles.length != numXTiles * numYTiles) {
            throw new IllegalArgumentException("Incorrect number of tiles");
        }

        // Get Width
        int width = 0;
        for (int x = 0; x < numXTiles; x++) {
            width += tiles[x].getWidth();
        }
        return width;
    }

    private static int getHeight(Texture[] tiles, int numXTiles, int numYTiles) {
        if (tiles.length != numXTiles * numYTiles) {
            throw new IllegalArgumentException("Incorrect number of tiles");
        }

        // Get Height
        int height = 0;
        for (int y = 0; y < numYTiles; y++) {
            height += tiles[y * numXTiles].getHeight();
        }
        return height;
    }
    
    private static Texture[] changeTextures(Texture[] tiles, int changeTextureFlag) {
        tiles = tiles.clone();
        if (changeTextureFlag == CHANGE_TEXTURE_IMMUTABLE) {
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = tiles[i].createImmutableCopy();
            }
        }
        else if (changeTextureFlag == CHANGE_TEXTURE_MUTABLE) {
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = tiles[i].createMutableCopy();
            }
        }
        return tiles;
    }

    private final Texture[] tiles;
    private final int numXTiles;
    private final int numYTiles;
    private final int tileOffsetX;
    private final int tileOffsetY;
    private final int totalTileWidth;
    private final int totalTileHeight;
    private final int width;
    private final int height;
    private final boolean opaque;
    private final boolean mutable;

    // TODO: hotspot info needs to come from somewhere, yet still be immutable.
    private final int hotspotX;
    private final int hotspotY;

    /**
     Creates a new Image with a mutable transparent texture.
     */
    public Image(int width, int height) {
        this(width, height, false);
    }

    /**
     Creates a new Image with a mutable image that is either fully transparent or fully opaque.
     */
    public Image(int width, int height, boolean opaque) {
        this(Context.getContext().createTexture(width, height, opaque));
    }

    public Image(Texture texture) {
        this(texture, 0, 0, texture.getWidth(), texture.getHeight(), texture.isOpaque());
    }
    
    public Image(Texture texture, int tileOffsetX, int tileOffsetY, int width, int height, boolean opaque) {
        this(new Texture[] { texture }, 1, 1, tileOffsetX, tileOffsetY, width, height, opaque, CHANGE_TEXTURE_NONE);
    }

    public Image(Texture[] tiles, int numXTiles, int numYTiles, boolean opaque) {
        this(tiles, numXTiles, numYTiles, 0, 0, 
                getWidth(tiles, numXTiles, numYTiles), getHeight(tiles, numXTiles, numYTiles), opaque, CHANGE_TEXTURE_NONE);
    }
    
    public Image(Texture[] tiles, int numXTiles, int numYTiles,
            int tileOffsetX, int tileOffsetY, int width, int height, boolean opaque) {
        this(tiles, numXTiles, numYTiles, tileOffsetX, tileOffsetY, width, height, opaque, CHANGE_TEXTURE_NONE);
    }

    private Image(Texture[] tiles, int numXTiles, int numYTiles,
            int tileOffsetX, int tileOffsetY, int width, int height, 
            boolean opaque, int changeTextureFlag) {

        this.totalTileWidth = getWidth(tiles, numXTiles, numYTiles);
        this.totalTileHeight = getHeight(tiles, numXTiles, numYTiles);

        if (tileOffsetX < 0 || tileOffsetY < 0 ||
                tileOffsetX + totalTileWidth > width ||
                tileOffsetY + totalTileHeight > height) {
            throw new IllegalArgumentException("Texture not with Image bounds");
        }

        this.tiles = changeTextures(tiles, changeTextureFlag);
        this.numXTiles = numXTiles;
        this.numYTiles = numYTiles;
        this.tileOffsetX = tileOffsetX;
        this.tileOffsetY = tileOffsetY;
        this.width = width;
        this.height = height;
        this.opaque = (tileOffsetX == 0 && tileOffsetY == 0 &&
                totalTileWidth == width && totalTileHeight == height) && opaque;
        this.mutable = hasSingleTile() && this.tiles[0].isMutable();
        this.hotspotX = 0;
        this.hotspotY = 0;
    }

    //
    // Image info
    //

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isOpaque() {
        return opaque;
    }

    /**
        Gets the x component of the hotspot.
        @see #getHotspotY()
    */
    public int getHotspotX() {
        return hotspotX;
    }

    /**
        Gets the y component of the hotspot.
        @see #getHotspotX()
    */
    public int getHotspotY() {
        return hotspotY;
    }

    //
    // Tile info
    //

    public boolean hasSingleTile() {
        return tiles.length == 1;
    }

    public int getNumXTiles() {
        return numXTiles;
    }

    public int getNumYTiles() {
        return numYTiles;
    }

    public Texture getTile(int x, int y) {
        return tiles[x + y * numXTiles];
    }

    public int getTileOffsetX() {
        return tileOffsetX;
    }

    public int getTileOffsetY() {
        return tileOffsetY;
    }

    /**
        Checks if the pixel at the specified location is transparent.
        @return true if the pixel is transparent or if the location is out of bounds.
    */
    public boolean isTransparent(int x, int y) {
        if (hasSingleTile()) {
            return tiles[0].isTransparent(x - tileOffsetX, y - tileOffsetY);
        }
        else if (x < tileOffsetX || y < tileOffsetY || x >= totalTileWidth || y >= totalTileHeight) {
            return true;
        }
        else if (isOpaque()) {
            return false;
        }
        else {
            x -= tileOffsetX;
            y -= tileOffsetY;

            int tileX = 0;
            int tileY = 0;

            // Find x
            for (int i = 0; i < numXTiles; i++) {
                Texture texture = getTile(i, 0);
                if (x < texture.getWidth()) {
                    tileX = i;
                    break;
                }
                else {
                    x -= texture.getWidth();
                }
            }

            // Find y
            for (int i = 0; i < numYTiles; i++) {
                Texture texture = getTile(0, i);
                if (y < texture.getHeight()) {
                    tileY = i;
                    break;
                }
                else {
                    y -= texture.getHeight();
                }
            }

            return getTile(tileX, tileY).isTransparent(x, y);
        }
    }

    /**
        Gets the ARGB color at the specified location. Returns 0 if the specified location is
        out of bounds.
        @return the ARGB color in non-premultiplied format.
    */
    public int getARGB(int x, int y) {
        if (hasSingleTile()) {
            return tiles[0].getARGB(x - tileOffsetX, y - tileOffsetY);
        }
        else if (x < tileOffsetX || y < tileOffsetY || x >= totalTileWidth || y >= totalTileHeight) {
            return 0;
        }
        else {
            x -= tileOffsetX;
            y -= tileOffsetY;

            int tileX = 0;
            int tileY = 0;

            // Find x
            for (int i = 0; i < numXTiles; i++) {
                Texture texture = getTile(i, 0);
                if (x < texture.getWidth()) {
                    tileX = i;
                    break;
                }
                else {
                    x -= texture.getWidth();
                }
            }

            // Find y
            for (int i = 0; i < numYTiles; i++) {
                Texture texture = getTile(0, i);
                if (y < texture.getHeight()) {
                    tileY = i;
                    break;
                }
                else {
                    y -= texture.getHeight();
                }
            }

            return getTile(tileX, tileY).getARGB(x, y);
        }
    }

    //
    // Mutation
    //

    /**
     Returns true if this Image has a single tile, and that tile is mutable. Mutable Images
     can change their contents by calling createGraphics() and drawing onto it.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     Creates a new Graphics context for drawing onto this Image, assuming the Image is mutable.
     If this Image is not mutable ({@link #isMutable()} returns false) then this method
     returns null.
    */
    public Graphics createGraphics() {
        if (!mutable) {
            throw new RuntimeException("Image is not mutable: " + this);
        }
        else {
            return tiles[0].createGraphics();
        }
    }
    
    /**
    Creates a mutable copy of this Image. The internal texture data is not shared.
    @return The newly created mutable Image.
    */
    public Image createMutableCopy() {
        return new Image(tiles, numXTiles, numYTiles, tileOffsetX, tileOffsetY, width, height, opaque, CHANGE_TEXTURE_MUTABLE);
    }

    /**
    Creates an immutable copy of this Image.
    Immutable images may have certain optimizations applied to them in the scene graph.
    @return The immutable Image.
    */
    public Image createImmutableCopy() {
        return new Image(tiles, numXTiles, numYTiles, tileOffsetX, tileOffsetY, width, height, opaque, CHANGE_TEXTURE_IMMUTABLE);
    }
}

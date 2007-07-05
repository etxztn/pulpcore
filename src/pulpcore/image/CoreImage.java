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

import java.io.IOException;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;
import pulpcore.util.ByteArray;

/**
    The CoreImage class contains 32-bit, ARGB raster data and provides 
    methods for creating transformed copies of the image. 
    <p>
    Methods like 
    {@link #crop(int, int, int, int)}, {@link #scale(double)}, and {@link #tint(int)} return new
    CoreImages. The image's raster data can be manipulated using the 
    {@link #createGraphics() } method, or by directly modifying pixels retrieved
    from the {@link #getData()} method.
*/
public class CoreImage {
    
    private static CoreImage brokenImage;
    
    protected int width;
    protected int height;
    protected boolean isOpaque;
    protected int[] data;
    
    // This might be used in the future
    //private boolean sharedRaster;
    
    /** 
        Hotspot x position. The hotspot is used by default in ImageSprite.
    */
    private int hotspotX;
    
    /** 
        Hotspot y position. The hotspot is used by default in ImageSprite.
    */
    private int hotspotY;
    
    
    CoreImage() { }
    
    /**
        Creates a blank, opaque image.
    */
    public CoreImage(int width, int height) {
        this(width, height, true);
    }
    

    /**
        Creates a blank image.
    */
    public CoreImage(int width, int height, boolean isOpaque) {
        this(width, height, isOpaque, new int[width * height]);
    }
    
    
    /**
        Creates a new image using the specified pixel data. The length of the 
        data must be greater than or equal to width * height.
        <p>
        The raster data array is assumed to be unique to this CoreImage, and not used
        in any other CoreImages.
    */
    public CoreImage(int width, int height, boolean isOpaque, int[] data) {
        this.width = width;
        this.height = height;
        this.isOpaque = isOpaque;
        this.data = data;
        //this.sharedRaster = false;
    }
    

    /**
        Creates a CoreImage with the same properties as the specified 
        CoreImage. The internal raster data array is shared.
    */
    public CoreImage(CoreImage image) {
        this.width = image.width;
        this.height = image.height;
        this.isOpaque = image.isOpaque;
        this.data = image.getData();
        this.hotspotX = image.hotspotX;
        this.hotspotY = image.hotspotY;
        //this.sharedRaster = true;
    }

    
    /**
        Creates a new CoreGaphics context for drawing onto this image.
    */
    public CoreGraphics createGraphics() {
        return new CoreGraphics(this);
    }
    
    
    public boolean isOpaque() {
        return isOpaque;
    }
    

    public int getWidth() {
        return width;
    }
    

    public int getHeight() {
        return height;
    }

    
    public int[] getData() {
        return data;
    }
    
    
    public void setHotspot(int x, int y) {
        hotspotX = x;
        hotspotY = y;
    }
    
    
    public int getHotspotX() {
        return hotspotX; 
    }
    
    
    public int getHotspotY() {
        return hotspotY; 
    }
    
    
    /**
        Does nothing by default. Subclasses can use this method for
        dynamically generated images or animations.
    */
    public void update(int elapsedTime) {
        // do nothing 
    }
    
    
    public static CoreImage getBrokenImage() {
        if (brokenImage == null) {
            brokenImage = new CoreImage(16, 16, true);
            CoreGraphics g = brokenImage.createGraphics();
            g.setColor(0xffffff);
            g.fill();
            g.setColor(0x000000);
            g.drawRect(0, 0, 16, 16);
            g.setColor(0xff0000);
            g.drawLine(2, 2, 13, 13);
            g.drawLine(13, 2, 2, 13);
        }
        
        return brokenImage;
    }
    

    //
    // Image Reading
    //
    
    
    /**
        Loads either a PNG or JPEG image from the specified imageAsset. If the PNG file
        has animation info, an {@link AnimatedImage} is returned.
    */
    public static CoreImage load(String imageAsset) {
        return load(imageAsset, null);
    }
        
    
    static CoreImage load(String imageAsset, CoreFont font) {
        
        //if (Build.DEBUG) CoreSystem.print("Loading: " + imageAsset);
        
        ByteArray in = Assets.get(imageAsset);
        
        if (in == null) {
            return getBrokenImage();
        }
        
        CoreImage image = null;
        if (imageAsset.toLowerCase().endsWith(".png")) {
            try {
                PNGReader pngReader = new PNGReader();
                image = pngReader.read(in, font);
            }
            catch (IOException ex) {
                if (Build.DEBUG) CoreSystem.print("Error loading image: " + imageAsset, ex);
            }
            
            if (image != null) {
                return image;
            }
            
            // Try again with the system decoder
            in.reset();
        }
        
        image = CoreSystem.getThisAppContext().loadImage(in);

        if (image == null) {
            if (Build.DEBUG) CoreSystem.print("Could not load image: " + imageAsset);
            image = getBrokenImage();
        }
        return image;
    }
  

    //
    // Image transforms
    //
    
    
    public CoreImage[] split(int framesAcross) {
        return split(framesAcross, 1);
    }
    
    
    public CoreImage[] split(int framesAcross, int framesDown) {
        
        int numFrames = framesAcross * framesDown;
        int w = width / framesAcross;
        int h = height / framesDown;
        
        CoreImage[] frames = new CoreImage[numFrames];
        
        for (int i = 0; i < numFrames; i++) {
            int x = (i % framesAcross) * w;
            int y = (i / framesAcross) * h;
            
            frames[i] = crop(x, y, w, h);
        }
        
        return frames;
    }
    
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The borderSize parameter can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int borderSize, int rgbColor) {
        return expandCanvas(borderSize, borderSize, borderSize, borderSize, rgbColor, false);
    }
    
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The borderSize parameter can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int borderSize, int argbColor, boolean hasAlpha) {
        return expandCanvas(borderSize, borderSize, borderSize, borderSize, argbColor, hasAlpha);
    }
    
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The top, right, bottom, left parameters can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int top, int right, int bottom, int left, int rgbColor) {
        return expandCanvas(top, right, bottom, left, rgbColor, false);
    }
    
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The top, right, bottom, left parameters can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int top, int right, int bottom, int left, int argbColor, 
        boolean hasAlpha) 
    {
        if (!hasAlpha) {
            argbColor |= 0xff000000;
        }

        int alpha = argbColor >>> 24;
        boolean isOpaque = this.isOpaque && alpha == 0xff;
        int newWidth = width + left + right;
        int newHeight = height + top + bottom;

        CoreImage newImage = new CoreImage(newWidth, newHeight, isOpaque);
        newImage.setHotspot(hotspotX + left, hotspotY + top);
        if (argbColor != 0) {
            int[] destData = newImage.getData();
            for (int i = 0; i < destData.length; i++) {
                destData[i] = argbColor;
            }
        }
        
        CoreGraphics g = newImage.createGraphics();
        g.drawImage(this, left, top);
        return newImage;
    }
    
    
    /**
        Creates a cropped version of this image. The raster data is not shared.
        The hotspot is copied as-is, with no translation.
    */
    public CoreImage crop(int x, int y, int w, int h) {
        
        CoreImage croppedImage = new CoreImage(w, h, isOpaque);
        croppedImage.setHotspot(hotspotX, hotspotY);
        
        int[] srcData = data;
        int srcOffset = x + y * width;
        int[] destData = croppedImage.getData();
        int destOffset = 0;
        
        for (int i = 0; i < h; i++) {
            System.arraycopy(srcData, srcOffset, destData, destOffset, w);
            srcOffset += width;
            destOffset += w;
        }
        
        return croppedImage;
    }
    

    /**
        Creates a rotated version of this image. 
        Same as calling rotate(angle, true);
        @param angle an angle, typically in the range from 0 to Math.PI * 2
    */
    public CoreImage rotate(double angle) {
        return rotate(CoreMath.toFixed(angle), true);
    }
    
    
    /**
        Creates a rotated version of this image.
        @param sizeAsNeeded if true, the resulting image is sized to contain 
        the entire rotated image. If false, the resulting image is the same 
        size as this image. The hotspot is rotated accordingly.
        @param angle an angle, typically in the range from 0 to Math.PI * 2
    */
    public CoreImage rotate(double angle, boolean sizeAsNeeded) {
        return rotate(CoreMath.toFixed(angle), sizeAsNeeded);
    }
    

    /**
        Creates a rotated version of this image. 
        Same as calling rotate(angle, true);
        @param angle a fixed-point angle, typically in the range from 0 to 
            CoreMath.TWO_PI.
    */
    public CoreImage rotate(int angle) {
        return rotate(angle, true);
    }
    
    
    /**
        Creates a rotated version of this image.
        @param sizeAsNeeded if true, the resulting image is sized to contain 
        the entire rotated image. If false, the resulting image is the same 
        size as this image. The hotspot is rotated accordingly.
        @param angle a fixed-point angle, typically in the range from 0 to 
            CoreMath.TWO_PI.
    */
    public CoreImage rotate(int angle, boolean sizeAsNeeded) {
        
        int newWidth = width;
        int newHeight = height;
        
        int cos = CoreMath.cos(angle);
        int sin = CoreMath.sin(angle);
        
        if (sizeAsNeeded) {
            newWidth = CoreMath.toIntCeil(Math.abs(width * cos) + Math.abs(height * sin));
            newHeight = CoreMath.toIntCeil(Math.abs(width * sin) + Math.abs(height * cos));
        }
        CoreImage rotatedImage = new CoreImage(newWidth, newHeight, isOpaque);
        
        int x = hotspotX - width/2;
        int y = hotspotY - height/2;
        rotatedImage.setHotspot(
            CoreMath.toIntRound(x * cos - y * sin) + newWidth / 2,
            CoreMath.toIntRound(x * sin + y * cos) + newHeight / 2);
        
        CoreGraphics g = rotatedImage.createGraphics();
        g.setComposite(CoreGraphics.COMPOSITE_SRC);
        g.drawRotatedImage(this, (newWidth - width) / 2, (newHeight - height) / 2, 
            width, height, angle);
        
        return rotatedImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents a scaled version 
        of this image. The hotspot is scaled accordingly.
    */
    public CoreImage scale(double scale) {
        return scale((int)Math.round(scale * width), (int)Math.round(scale * height));
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents a scaled version 
        of this image. The hotspot is scaled accordingly.
    */
    public CoreImage scale(int scaledWidth, int scaledHeight) {
        
        scaledWidth = Math.max(1, scaledWidth);
        scaledHeight = Math.max(1, scaledHeight);
        
        CoreImage scaledImage = new CoreImage(scaledWidth, scaledHeight, isOpaque);
        scaledImage.setHotspot(
            hotspotX * scaledWidth / width,
            hotspotY * scaledHeight / height);
        
        CoreGraphics g = scaledImage.createGraphics();
        g.setComposite(CoreGraphics.COMPOSITE_SRC);
        g.drawScaledImage(this, 0, 0, scaledWidth, scaledHeight);
        
        return scaledImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents a 50% scaled 
        version of this image. This method uses a weighted average instead
        of bilinear interpolation. The hotspot is scaled accordingly.
    */
    public CoreImage halfSize() {
    
        CoreImage scaledImage = new CoreImage(width / 2, height / 2, isOpaque);
        scaledImage.setHotspot(hotspotX / 2, hotspotY / 2);
        
        int[] srcData = getData();
        int srcWidth = getWidth();
        int srcOffset = 0;
        
        int[] destData = scaledImage.getData();
        int destWidth = scaledImage.getWidth();
        int destHeight = scaledImage.getHeight();
        int destOffset = 0;
        
        for (int y = 0; y < destHeight; y++) {
            
            srcOffset = srcWidth * y * 2;
            
            for (int x = 0; x < destWidth; x++) {
                
                int p1 = srcData[srcOffset];
                int p2 = srcData[srcOffset + 1];
                int p3 = srcData[srcOffset + srcWidth];
                int p4 = srcData[srcOffset + srcWidth + 1];

                int p1a = p1 >>> 24;
                int p2a = p2 >>> 24;
                int p3a = p3 >>> 24;
                int p4a = p4 >>> 24;
                
                int r = 0;
                int g = 0;
                int b = 0;
                int count = 0;
                
                if (p1a != 0) {
                    r += (p1 >> 16) & 0xff;
                    g += (p1 >> 8) & 0xff;
                    b += p1 & 0xff;
                    count++;
                }
                
                if (p2a != 0) {
                    r += (p2 >> 16) & 0xff;
                    g += (p2 >> 8) & 0xff;
                    b += p2 & 0xff;
                    count++;
                }
                
                if (p3a != 0) {
                    r += (p3 >> 16) & 0xff;
                    g += (p3 >> 8) & 0xff;
                    b += p3 & 0xff;
                    count++;
                }
                
                if (p4a != 0) {
                    r += (p4 >> 16) & 0xff;
                    g += (p4 >> 8) & 0xff;
                    b += p4 & 0xff;
                    count++;
                }
                
                if (count > 0) {
                    int a = (p1a + p2a + p3a + p4a) >> 2;
                    r /= count;
                    g /= count;
                    b /= count;
                    destData[destOffset] = (a << 24) | (r << 16) | (g << 8) | b;
                }
                
                /*
                // Old method (doesn't apply alpha correctly)
                int a = ((p1 >>> 24) + (p2 >>> 24) + (p3 >>> 24) + (p4 >>> 24)) >> 2;
                int r = (((p1 >> 16) & 0xff) + ((p2 >> 16) & 0xff) + 
                        ((p3 >> 16) & 0xff) + ((p4 >> 16) & 0xff)) >> 2;
                int g = (((p1 >> 8) & 0xff) + ((p2 >> 8) & 0xff) + 
                        ((p3 >> 8) & 0xff) + ((p4 >> 8) & 0xff)) >> 2;
                int b = ((p1 & 0xff) + (p2 & 0xff) + (p3 & 0xff) + (p4 & 0xff)) >> 2;
                
                destData[destOffset] = (a << 24) | (r << 16) | (g << 8) | b;
                */
                
                srcOffset += 2;
                destOffset++;
            }
            
            
        }
        
        return scaledImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents a mirrored version 
        of this image. The hotspot is mirrored accordingly.
    */
    public CoreImage mirror() {
        CoreImage mirroredImage = new CoreImage(width, height, isOpaque);
        
        mirroredImage.setHotspot((width - 1) - hotspotX, hotspotY);
            
        int[] srcData = data;
        int[] destData = mirroredImage.getData();
        int offset = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                destData[offset + i] = srcData[offset + width - 1 - i];
            }
            offset += width;
        }
        
        return mirroredImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents a flipped version 
        of this image. The hotspot is flipped accordingly.
    */
    public CoreImage flip() {
        CoreImage flippedImage = new CoreImage(width, height, isOpaque);
        flippedImage.setHotspot(hotspotX, (height - 1) - hotspotY);
            
        int[] srcData = data;
        int[] destData = flippedImage.getData();
        int srcOffset = width * (height - 1);
        int dstOffset = 0;
        for (int i = 0; i < height; i++) {
            System.arraycopy(srcData, srcOffset, destData, dstOffset, width);
            srcOffset -= width;
            dstOffset += width;
        }
        
        return flippedImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated to the left (counter-clockwise 90 degrees).
        The hotspot is rotated accordingly.
    */
    public CoreImage rotateLeft() {
        
        int newWidth = height;
        int newHeight = width;
        
        CoreImage rotImage = new CoreImage(newWidth, newHeight, isOpaque);
        rotImage.setHotspot(hotspotY, (newHeight - 1) - hotspotX);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < newHeight; j++) {
            int srcOffset = width - 1 - j;
            for (int i = 0; i < newWidth; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset += width;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated to the right (clockwise 90 degrees).
        The hotspot is rotated accordingly.
    */
    public CoreImage rotateRight() {
        
        int newWidth = height;
        int newHeight = width;
        
        CoreImage rotImage = new CoreImage(newWidth, newHeight, isOpaque);
        rotImage.setHotspot((newWidth - 1) - hotspotY, hotspotX);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < newHeight; j++) {
            int srcOffset = width * (height - 1) + j;
            for (int i = 0; i < newWidth; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset -= width;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated 180 degrees.
        The hotspot is rotated accordingly.
    */
    public CoreImage rotate180() {
        
        CoreImage rotImage = new CoreImage(width, height, isOpaque);
        rotImage.setHotspot((width - 1) - hotspotX, (height - 1) - hotspotY);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < height; j++) {
            int srcOffset = width * (height - j) - 1;
            for (int i = 0; i < width; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset--;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    
    //
    // ARGB filters 
    //
    
    
    /**
        Returns a new CoreImage with every color set to the specified color,
        without changing the alpha of each color. This method is useful for
        creating a variety of colored fonts or for creating a solid-color
        stencil of a sprite.
    */
    public CoreImage tint(int rgbColor) {
        
        CoreImage tintedImage = new CoreImage(width, height, isOpaque);
        tintedImage.setHotspot(hotspotX, hotspotY);
        
        int[] srcData = data;
        int[] destData = tintedImage.getData();
        for (int i = 0; i < srcData.length; i++) {
            destData[i] = (srcData[i] & 0xff000000) | (rgbColor & 0x00ffffff);
        }
        
        return tintedImage;
    }
    
    
    public CoreImage background(int rgbColor) {
        return background(rgbColor, false);
    }
    
    
    public CoreImage background(int argbColor, boolean hasAlpha) {
        if (!hasAlpha) {
            argbColor |= 0xff000000;
        }
        
        CoreImage newImage = new CoreImage(width, height, 
            isOpaque || (argbColor >>> 24) == 0xff);
        newImage.setHotspot(hotspotX, hotspotY);
        
        int[] destData = newImage.getData();
        for (int i = 0; i < destData.length; i++) {
            destData[i] = argbColor;
        }
        
        CoreGraphics g = newImage.createGraphics();
        g.drawImage(this);
        return newImage;
    }
    
    
    public CoreImage fade(int alpha) {
        CoreImage fadeImage = new CoreImage(width, height, 
            isOpaque && alpha >= 0xff);
        fadeImage.setHotspot(hotspotX, hotspotY);
        
        CoreGraphics g = fadeImage.createGraphics();
        g.setComposite(CoreGraphics.COMPOSITE_SRC);
        g.setAlpha(alpha);
        g.drawImage(this);
        return fadeImage;
    }
}

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

import pulpcore.animation.Bool;
import pulpcore.image.AnimatedImage;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Transform;

/**
    An image-based sprite. The image can be an {@link pulpcore.image.AnimatedImage}.
    To ignore the CoreImage's hotspot, call {@link #setAnchor(int) } with an 
    anchor other than Sprite.DEFAULT, like Sprite.TOP | Sprite.LEFT.
*/
public class ImageSprite extends Sprite {
    
    private CoreImage image;
    private int lastImageFrame;
    
    
    /**
        Creates an ImageSprite that has the same dimensions as the image.
    */
    public ImageSprite(String imageAsset, int x, int y) {
        this(imageAsset, x, y, -1, -1);
    }
    
    
    /**
        Creates an ImageSprite that has the same dimensions as the image.
    */
    public ImageSprite(CoreImage image, int x, int y) {
        this(image, x, y, -1, -1);
    }
    
    
    /**
        Creates an ImageSprite that draws the image scaled to the specified dimensions.
    */
    public ImageSprite(String imageAsset, int x, int y, int w, int h) {
        this(CoreImage.load(imageAsset), x, y, w, h);
    }
    
    
    /**
        Creates an ImageSprite that draws the image scaled to the specified dimensions.
    */
    public ImageSprite(CoreImage image, int x, int y, int w, int h) {
        super(x, y, w, h);
        setImage(image);
        if (w < 0) {
            width.set(image.getWidth());
        }
        if (h < 0) {
            height.set(image.getHeight());
        }
    }
    
    
    /**
        Creates an ImageSprite that has the same dimensions as the image.
    */
    public ImageSprite(String imageAsset, double x, double y) {
        this(imageAsset, x, y, -1, -1);
    }
    
    
    /**
        Creates an ImageSprite that has the same dimensions as the image.
    */
    public ImageSprite(CoreImage image, double x, double y) {
        this(image, x, y, -1, -1);
    }
    
    
    /**
        Creates an ImageSprite that draws the image scaled to the specified dimensions.
    */
    public ImageSprite(String imageAsset, double x, double y, double w, double h) {
        this(CoreImage.load(imageAsset), x, y, w, h);
    }
    
    
    /**
        Creates an ImageSprite that draws the image scaled to the specified dimensions.
    */
    public ImageSprite(CoreImage image, double x, double y, double w, double h) {
        super(x, y, w, h);
        setImage(image);
        if (w < 0) {
            width.set(image.getWidth());
        }
        if (h < 0) {
            height.set(image.getHeight());
        }
    }    
    
   
    public void setImage(CoreImage image) {
        if (this.image == image) {
            return;
        }
        
        if (image instanceof AnimatedImage) {
            ((AnimatedImage)image).setFrame(0);
        }
        this.image = image;
        setDirty(true);
        lastImageFrame = 0;
    }
    
    
    public void stop() {
        if (image instanceof AnimatedImage) {
            ((AnimatedImage)image).stop();
            checkDirtyImage();
        }
    }
    
    
    public void pause() {
        if (image instanceof AnimatedImage) {
            ((AnimatedImage)image).pause();
            checkDirtyImage();
        }
    }
    
    
    public void start() {
        if (image instanceof AnimatedImage) {
            ((AnimatedImage)image).start();
            checkDirtyImage();
        }
    }
    
    
    private void checkDirtyImage() {
        if (image instanceof AnimatedImage) {
            int frame = ((AnimatedImage)image).getFrame();
            if (frame != lastImageFrame) {
                setDirty(true);
            }
        }
    }
    
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        if (image != null) {
            image.update(elapsedTime);
            checkDirtyImage();
        }
    }
    
    
    public CoreImage getImage() {
        return image;
    }
    
    
    protected int getNaturalWidth() {
        if (image != null) {
            return CoreMath.toFixed(image.getWidth());
        }
        else {
            return super.getNaturalWidth();
        }
    }
    
    
    protected int getNaturalHeight() {
        if (image != null) {
            return CoreMath.toFixed(image.getHeight());
        }
        else {
            return super.getNaturalHeight();
        }
    }
    
    
    protected int getAnchorX() {
        if (image != null && getAnchor() == DEFAULT) {
            return CoreMath.toFixed(image.getHotspotX());
        }
        else {
            return super.getAnchorX();
        }
    }
    
    
    protected int getAnchorY() {
        if (image != null && getAnchor() == DEFAULT) {
            return CoreMath.toFixed(image.getHotspotY());
            //return image.getHotspotY();
        }
        else {
            return super.getAnchorY();
        }
    }
    
    
    protected void drawSprite(CoreGraphics g) {
        if (image == null) {
            return;
        }
        
        if (image instanceof AnimatedImage) {
            lastImageFrame = ((AnimatedImage)image).getFrame();
        }
        g.drawImage(image);
    }
}
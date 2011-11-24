/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
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

package org.pulpcore.view;

import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Image;

/**
    An image-based sprite. The image can be an {@link pulpcore.image.AnimatedImage}.
    The anchor of an ImageView is automatically set to the image's hotspot.
    To ignore the  hotspot, use {@link View#setAnchor(float, float) }.
    <p>
    By default, ImageView use pixel-level checking for intersection tests. Use
    {@link View#setPixelLevelChecks(boolean) } to disable this feature.
    <p>
    If you change the pixels of this ImageView's internal Image, call
    {@code sprite.setDirty(true)}.
*/
public class ImageView extends View {

    private Image image;
    private boolean antiAliased = true;

    public ImageView(String imageAsset) {
        this(Image.load(imageAsset));
    }
   
    public ImageView(Image image) {
        super(image.getWidth(), image.getHeight());
        setImage(image);
        setAnchorToHotSpot();
        setPixelLevelChecks(true);
    }

    public boolean isAntiAliased() {
        return antiAliased;
    }

    /**
     Flag indicating whether the edges of this ImageView are anti-aliased when rotating or
     drawing at fractional locations. The default value is {@code true}. (Not all platforms
     support this.)
    */
    public void setAntiAliased(boolean antiAliased) {
        if (this.antiAliased != antiAliased) {
            this.antiAliased = antiAliased;
            setContentsDirty();
        }
    }

    @Override
    public boolean isOpaque() {
        return (image != null && image.isOpaque()) || super.isOpaque();
    }
    
    /**
     Gets this ImageView's internal image.
    */
    public Image getImage() {
        return image;
    }
    
    /**
     Sets this ImageView's internal image. The anchor of this ImageView is not changed.
    */
    public void setImage(String imageAsset) {
        setImage(Image.load(imageAsset));
    }
    
    /**
     Sets this ImageView's internal image. The anchor of this ImageView is not changed.
    */
    public void setImage(Image image) {
        if (this.image != image) {
            this.image = image;
            setContentsDirty();
            if (image != null) {
                setSize(image.getWidth(), image.getHeight());
            }
        }
    }

    /**
     Sets the anchor to the underlying image's hotspot. An image's hotspot is defined at build
     time with a <a href="http://code.google.com/p/pulpcore/wiki/PropertyFiles">property
     file</a>.
     */
    public void setAnchorToHotSpot() {
        if (image != null) {
            anchorX.set((float)image.getHotspotX() / image.getWidth());
            anchorY.set((float)image.getHotspotY() / image.getHeight());
        }
        else {
            anchorX.set(0);
            anchorY.set(0);
        }
    }
    
    @Override
    protected boolean isTransparent(int localX, int localY) {
        return (image == null || image.isTransparent(localX, localY)) && super.isTransparent(localX, localY);
    }

    @Override
    protected void onRender(Graphics g) {
        g.setEdgeClamp(isAntiAliased() ? Graphics.EDGE_CLAMP_NONE : Graphics.EDGE_CLAMP_ALL);
        if (image == null || !image.isOpaque()) {
            // Draw background fill
            super.onRender(g);
        }
        // TODO: Since an Image's internal Texture can be offset by an (x,y) value and not
        // cover the entire Image, it would be an optimization to:
        // 1. If there is no background color, the dirty rectangle is only a portion of the view.
        //    Maybe View needs a getVisibleRectangle(Recti rect) ?
        // 2. Cache the Texture transform. Or - the Visible Rect transform?
        //    onRender draws with the Visible Rect transform - and the background fill is
        //    drawn in View.render() ? Won't be an improvement when there are tiled textures.
        // OR: ImageView is actually a parent for TextureView?
        if (image != null) {
            g.drawImage(image);
        }

    }
}
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

package org.pulpcore.runtime.applet;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.Hashtable;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.runtime.Surface;
import org.pulpcore.math.Recti;
import org.pulpcore.runtime.softgraphics.SoftTexture;

/**
    The BufferedImageSurface class is a Surface implementation for Java 1.3 or 
    newer. It provides faster performance compared to ImageProducerSurface.
*/
public class BufferedImageSurface extends Surface {
    
    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
    
    private final Object paintLock;
    
    private final int defaultBackgroundColor;
    private SoftTexture image;
    private Graphics graphics;
    private Component component;
    protected BufferedImage bufferedImage;
    private boolean repaintAllowed;
    private boolean active;
    
    private Recti repaintBounds = new Recti();
    protected Recti[] dirtyRectangles;
    protected int numDirtyRectangles;
    
    public BufferedImageSurface(Component component, Object runLoopLock) {
        this.component = component;
        this.paintLock = runLoopLock;
        this.defaultBackgroundColor = component.getBackground().getRGB();
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public int getWidth() {
        if (image == null) {
            return 0;
        }
        else {
            return image.getWidth();
        }
    }

    @Override
    public int getHeight() {
        if (image == null) {
            return 0;
        }
        else {
            return image.getHeight();
        }
    }
    
    @Override
    public void start() {
        synchronized (paintLock) {
            active = true;
        }
    }

    @Override
    public void stop() {
        synchronized (paintLock) {
            active = false;
            paintLock.notify();
        }
    }

    @Override
    public boolean isPrepared() {
        int w = component.getWidth();
        int h = component.getHeight();
        return (w > 0 && h > 0 && image != null);
    }

    @Override
    public boolean hasSizeChanged() {

        int oldWidth = getWidth();
        int oldHeight = getHeight();
        int newWidth = component.getWidth();
        int newHeight = component.getHeight();
        boolean sizedChanged = false;

        if (newWidth <= 0 || newHeight <= 0) {
            return false;
        }
        else if (oldWidth != newWidth || oldHeight != newHeight) {
            setSize(newWidth, newHeight);
            sizedChanged = true;
        }

        return sizedChanged;
    }

    private void setSize(int w, int h) {
        image = new SoftTexture(w, h, true, defaultBackgroundColor);
        graphics = image.createGraphics();

        setContentsLost(true);

        if (bufferedImage != null) {
            bufferedImage.flush();
        }
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, w, h, new int[] { 0xff0000, 0x00ff00, 0x0000ff });
        DataBuffer dataBuffer = new DataBufferInt(image.getData(), w * h);
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        bufferedImage = new BufferedImage(COLOR_MODEL, raster, true, new Hashtable<Object, Object>());
    }

    @Override
    public void show(Recti[] dirtyRectangles, int numDirtyRectangles) {
        synchronized (paintLock) {
            this.dirtyRectangles = dirtyRectangles;
            this.numDirtyRectangles = numDirtyRectangles;
            
            if (contentsLost() || numDirtyRectangles < 0) {
                repaintBounds.setBounds(0, 0, getWidth(), getHeight());
                numDirtyRectangles = -1;
                setContentsLost(false);
            }
            else if (numDirtyRectangles == 0) {
                repaintBounds.width = 0;
            }
            else {
                repaintBounds.setBounds(dirtyRectangles[0]);
                for (int i = 1; i < numDirtyRectangles; i++) {
                    repaintBounds.union(dirtyRectangles[i]);
                }
            }

            if (repaintBounds.width > 0 && repaintBounds.height > 0) {
                if (active) {
                    repaintAllowed = true;
                    component.repaint(repaintBounds.x, repaintBounds.y, 
                        repaintBounds.width, repaintBounds.height);
                    try {
                        paintLock.wait(1000);
                    }
                    catch (InterruptedException ex) {
                        // ignore
                    }
                    repaintAllowed = false;
                }
            }
        }
    }
    
    // Called from the AWT thread
    public void draw(java.awt.Graphics g, boolean fullRepaint) {
        synchronized (paintLock) {
            if (repaintAllowed || fullRepaint) {
                if (bufferedImage != null) {
                    if (fullRepaint || numDirtyRectangles < 0) {
                        g.setClip(0, 0, getWidth(), getHeight());
                        g.drawImage(bufferedImage, 0, 0, null);
                    }
                    else {
                        for (int i = 0; i < numDirtyRectangles; i++) {
                            Recti r = dirtyRectangles[i];
                            g.setClip(r.x, r.y, r.width, r.height);
                            g.drawImage(bufferedImage, 0, 0, null);
                        }
                    }
                }
                if (fullRepaint) {
                    setContentsLost(true);
                }
                paintLock.notify();
            }
        }
    }
    
    @Override
    public String toString() {
        return "BufferedImage";
    }
}

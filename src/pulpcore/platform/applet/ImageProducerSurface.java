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

package pulpcore.platform.applet;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.Toolkit;

import pulpcore.platform.Surface;
import pulpcore.math.Rect;

/**
    The ImageProducerSurface class is a Surface implementation for Java 1.1.
*/
public class ImageProducerSurface extends Surface implements ImageProducer {
    
    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
    
    private final Object paintLock = new Object();
    
    private Component component;
    protected Image awtImage;
    private ImageConsumer consumer;
    private boolean needsRefresh;
    
    private Rect repaintBounds = new Rect();
    protected Rect[] dirtyRectangles;
    protected int numDirtyRectangles;
    
    
    public ImageProducerSurface(Component component) {
        this.component = component;
    }
    
    
    protected void notifyResized() {
        contentsLost = true;
        consumer = null;
        awtImage = Toolkit.getDefaultToolkit().createImage(this);
    }
    
    
    public boolean isReady() {
        
        Dimension size = component.getSize();
        if (size.width <= 0 || size.height <= 0) {
            return false;
        }
        else if (getWidth() != size.width || getHeight() != size.height) {
            setSize(size.width, size.height);
        }
        
        return true;
    }
    
    
    public void show(Rect[] dirtyRectangles, int numDirtyRectangles) {
        
        this.dirtyRectangles = dirtyRectangles;
        this.numDirtyRectangles = numDirtyRectangles;
        
        if (contentsLost || numDirtyRectangles < 0) {
            repaintBounds.setBounds(0, 0, getWidth(), getHeight());
        }
        else if (numDirtyRectangles == 0) {
            return;
        }
        else {
            repaintBounds.setBounds(dirtyRectangles[0]);
            for (int i = 1; i < numDirtyRectangles; i++) {
                repaintBounds.union(dirtyRectangles[i]);
            }
        }
        
        if (repaintBounds.width <= 0 || repaintBounds.height <= 0) {
            return;
        }
        
        synchronized (paintLock) {
            needsRefresh = true;
            component.repaint(repaintBounds.x, repaintBounds.y, 
                repaintBounds.width, repaintBounds.height);
            try {
                paintLock.wait(1000);
            }
            catch (InterruptedException ex) {
                // ignore
            }
        }
    }
    
    
    public void update(Graphics g) {
        paint(g);
    }
    
    
    public void paint(Graphics g) {
        
        if (!needsRefresh) {
            // Call from the OS?
            contentsLost = true;
            return;
        }
        if (awtImage != null) {
            show(g);
        }
        
        synchronized (paintLock) {
            needsRefresh = false;
            paintLock.notify();
        }
    }
    
    
    protected void show(Graphics g) {
        
        if (consumer != null) {
            if (contentsLost || numDirtyRectangles < 0) {
                consumer.setPixels(0, 0, getWidth(), getHeight(), 
                    COLOR_MODEL, getData(), 0, getWidth());
            }
            else {
                int scanSize = getWidth();
                
                for (int i = 0; i < numDirtyRectangles; i++) {
                    Rect r = dirtyRectangles[i];
                    int offset = r.x + r.y * scanSize;
                    consumer.setPixels(r.x, r.y, r.width, r.height, 
                        COLOR_MODEL, getData(), offset, scanSize);
                }
                
                g.setClip(repaintBounds.x, repaintBounds.y, 
                    repaintBounds.width, repaintBounds.height);
            }
            consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
        }
        g.drawImage(awtImage, 0, 0, null);
        
        contentsLost = false;
    }


    //
    // ImageProducer interface
    //

    public void addConsumer(ImageConsumer newConsumer) {
        if (newConsumer == null || consumer == newConsumer) {
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        int[] data = getData();
        
        consumer = newConsumer;

        consumer.setDimensions(width, height);
        consumer.setColorModel(COLOR_MODEL);
        consumer.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES);
        consumer.setPixels(0, 0, width, height, COLOR_MODEL, data, 0, width);
        consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
    }
    

    public boolean isConsumer(ImageConsumer consumer) {
        return (this.consumer == consumer);
    }
    
    
    public void removeConsumer(ImageConsumer ic) {
        if (this.consumer == consumer) {
            this.consumer = null;
        }
    }
    
    
    public void startProduction(ImageConsumer consumer) {
        addConsumer(consumer);
    }
    
    
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // do nothing
    }
}
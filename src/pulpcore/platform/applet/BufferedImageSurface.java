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
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Hashtable;
import pulpcore.math.Rect;
import pulpcore.platform.Surface;

/**
    The BufferedImageSurface class is a Surface implementation for Java 1.3 or 
    newer. It provides faster performace compared to ImageProducerSurface.
*/
public class BufferedImageSurface extends Surface {
    
    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
    
    private final Object paintLock = new Object();
    
    private Component component;
    protected BufferedImage awtImage;
    private boolean needsRefresh;
    
    private Rect repaintBounds = new Rect();
    protected Rect[] dirtyRectangles;
    protected int numDirtyRectangles;
    
    
    public BufferedImageSurface(Component component) {
        this.component = component;
    }
    
    
    public int getRefreshRate() {
        return -1;
    }
    
    
    protected void notifyResized() {
        contentsLost = true;
        
        int w = getWidth();
        int h = getHeight();
        
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, w, h, new int[] { 0xff0000, 0x00ff00, 0x0000ff }); 
        
        DataBuffer dataBuffer = new DataBufferInt(getData(), w * h);
        
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        
        awtImage = new BufferedImage(COLOR_MODEL, raster, true, new Hashtable());
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
    
    
    public void draw(Graphics g) {
        
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
        
        if (contentsLost || numDirtyRectangles < 0) {
            g.drawImage(awtImage, 0, 0, null);
        }
        else {
            for (int i = 0; i < numDirtyRectangles; i++) {
                Rect r = dirtyRectangles[i];
                g.setClip(r.x, r.y, r.width, r.height);
                g.drawImage(awtImage, 0, 0, null);
            }
        }
        
        contentsLost = false;
    }
    
}

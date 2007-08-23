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

package pulpcore.platform.applet.opt;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.event.AWTEventListener;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.ImageCapabilities;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Hashtable;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.Rect;
import pulpcore.platform.Surface;

/**
    The BufferStrategySurface class is a Surface implementation for Java 1.4 or 
    newer. It provides about the same performace as BufferedImageSurface, but does not
    result in dropped frames on Mac OS X.
*/
public class BufferStrategySurface extends Surface {

    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
        
    private BufferStrategy bufferStrategy;
    private BufferedImage image;
    private Container container;
    private Canvas canvas;
    private boolean osRepaint;
    private boolean useDirtyRects;
    
    public BufferStrategySurface(Container container) {
        this.container = container;
        this.canvas = new Canvas();
        container.removeAll();
        container.setLayout(null);
        canvas.setSize(1, 1);
        container.add(canvas);
        canvas.setLocation(0, 0);
    }
    

    public int getRefreshRate() {
        return -1;
    }
    
    
    public Canvas getCanvas() {
        return canvas;
    }
    
    
    public synchronized void notifyOSRepaint() {
        osRepaint = true;
    }
    
    
    private synchronized void checkOsRepaint() {
        if (osRepaint) {
            osRepaint = false;
            contentsLost = true;
        }
    }
    
    
    public boolean isReady() {
        
        Dimension size = container.getSize();
        if (size.width <= 0 || size.height <= 0) {
            return false;
        }
        else if (image == null || getWidth() != size.width || getHeight() != size.height) {
            setSize(size.width, size.height);
        }
        
        if (bufferStrategy == null) {
            try {
                useDirtyRects = false;
                createBufferStrategy();
                bufferStrategy = canvas.getBufferStrategy();
                if (bufferStrategy != null) {
                    // Commented out: It looks like Mac OS X says "COPIED" when it means "UNDEFINED".
                    // So, always copy the entire buffer.
                    // BufferCapabilities caps = bufferStrategy.getCapabilities();
                    //if (caps.getFlipContents() == BufferCapabilities.FlipContents.COPIED) {
                    //    useDirtyRects = true;
                    //}
                }
            }
            catch (Exception ex) {
                // There is a ClassCastException when running under Eclipse 3.3 for Mac OS X Java 5.
                // My guess is there is a SWT conflict. This won't affect end users, but developers 
                // will have to use appletviewer or a web browser to run PulpCore apps.
                if (Build.DEBUG) CoreSystem.print("Couldn't create surface", ex);
            }
        }
        else if (!contentsLost) {
            contentsLost = bufferStrategy.contentsLost() | bufferStrategy.contentsRestored();
        }
        
        checkOsRepaint();
        
        return (bufferStrategy != null);
    }
    
    
    protected void notifyResized() {
        int w = getWidth();
        int h = getHeight();
        contentsLost = true;
        bufferStrategy = null;
        canvas.setSize(new Dimension(w, h));

        // Create the AWT image
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, w, h, new int[] { 0xff0000, 0x00ff00, 0x0000ff }); 
        DataBuffer dataBuffer = new DataBufferInt(getData(), w * h);
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        image = new BufferedImage(COLOR_MODEL, raster, true, new Hashtable());
    }
    
    
    public void show(Rect[] dirtyRectangles, int numDirtyRectangles) {
        
        if (bufferStrategy == null || image == null || numDirtyRectangles == 0) {
            return;
        }
        
        Graphics g = null;
        try {
            g = bufferStrategy.getDrawGraphics();
            
            if (contentsLost || !useDirtyRects || numDirtyRectangles < 0) {
                g.drawImage(image, 0, 0, null);
            }
            else {
                for (int i = 0; i < numDirtyRectangles; i++) {
                    Rect r = dirtyRectangles[i];
                    g.setClip(r.x, r.y, r.width, r.height);
                    g.drawImage(image, 0, 0, null);
                }
            }
            bufferStrategy.show();
            contentsLost = false;
        }
        catch (Exception ex) {
            // Ignore
        }
        finally {
            if (g != null) {
                g.dispose();
            }
        }
    }

    
    private void createBufferStrategy() {
        // First, try Copied method (double buffering)
        //try {
        //    canvas.createBufferStrategy(2, new BufferCapabilities(
        //        new ImageCapabilities(true),
        //        new ImageCapabilities(true),
        //        BufferCapabilities.FlipContents.COPIED));
        //    return;
        //} 
        //catch (AWTException e) {
        //    // Ignore
        //}
        
        // Try whatever the system wants
        canvas.createBufferStrategy(2);
    }
}

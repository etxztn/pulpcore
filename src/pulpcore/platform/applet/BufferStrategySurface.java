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
    
    // For Mac OS X workaround
    // All times in milliseconds
    private static final int MAC_MIN_DURATION = 18; // Delay can be from 17 to 17.99999... ms
    private boolean doAppleWorkaroundSync;
    private long nextSyncTime = 0;
    
    public BufferStrategySurface(Container container) {
        this.container = container;
        this.canvas = new Canvas();
        container.removeAll();
        container.setLayout(null);
        canvas.setSize(1, 1);
        container.add(canvas);
        canvas.setLocation(0, 0);
        doAppleWorkaroundSync = CoreSystem.isMacOSXLeopardOrNewer();
        // Try to create the surface for the sake of toString(), below
        isReady();
    }
    

    public int getRefreshRate() {
        if (doAppleWorkaroundSync) {
            if (MAC_MIN_DURATION > 0) {
                return 1000 / MAC_MIN_DURATION;
            }
            else {
                return 32768;
            }
        }
        else {
            return -1;
        }
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
                    BufferCapabilities caps = bufferStrategy.getCapabilities();
                    useDirtyRects = !caps.isPageFlipping();
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
    
    
    public long show(Rect[] dirtyRectangles, int numDirtyRectangles) {
        
        if (bufferStrategy == null || image == null) {
            return 0;
        }
        
        if (numDirtyRectangles == 0) {
            if (doAppleWorkaroundSync) {
                return sync();
            }
            else {
                return 0;
            }
        }
        
        long sleepTime = 0;
        Graphics g = null;
        try {
            while (true) {
                while (true) {
                    g = bufferStrategy.getDrawGraphics();
            
                    if (g != null) {
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
                        g.dispose();
                        g = null;
                    }
                    
                    if (bufferStrategy.contentsRestored()) {
                        contentsLost = true;
                    }
                    else {
                        break;
                    }
                }
                
                if (doAppleWorkaroundSync) {
                    // Call show(), sync, and if there is enough time, call show() again
                    bufferStrategy.show();
                    boolean showAgain = (System.currentTimeMillis() < nextSyncTime);
                    sleepTime += sync();
                    if (showAgain) {
                        bufferStrategy.show();
                    }
                }
                else {
                    bufferStrategy.show();
                }
                
                if (bufferStrategy.contentsLost()) {
                    contentsLost = true;
                }
                else {
                    contentsLost = false;
                    break;
                }
            }
        }
        catch (Exception ex) {
            // Ignore
        }
        finally {
            if (g != null) {
                g.dispose();
            }
        }
        return sleepTime;
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
    
    
    private long sync() {
        long startTime = CoreSystem.getTimeMicros();
        while (true) {
            long currTime = System.currentTimeMillis();
            if (currTime >= nextSyncTime || currTime < nextSyncTime - MAC_MIN_DURATION) {
                nextSyncTime = currTime + MAC_MIN_DURATION;
                break;
            }
            else {
                int sleepTime = 1;
                if (currTime == nextSyncTime - 1) {
                    sleepTime = 0;
                }
                try {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException ex) { }
            }
        }
        return CoreSystem.getTimeMicros() - startTime;
    }
    
    
    public String toString() {
        String s = "BufferStrategy";
        if (bufferStrategy != null) {
            s += "(doAppleWorkaroundSync=" + doAppleWorkaroundSync + ", " +
            "isPageFlipping=" + bufferStrategy.getCapabilities().isPageFlipping() + ", " + 
            "useDirtyRects=" + useDirtyRects + ")";
        }
        return s;
    }
}

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

import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
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
    The BufferStrategySurface class is a Surface implementation for Java 1.4 or 
    newer. It provides about the same performance as BufferedImageSurface, but does not
    result in dropped frames on Mac OS X.
*/
public class BufferStrategySurface extends Surface {

    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);

    private final int defaultBackgroundColor;
    private SoftTexture image;
    private Graphics graphics;
    private BufferedImage bufferedImage;
    private Container container;
    private Canvas canvas;
    private boolean osRepaint;
    private boolean useDirtyRects;
    private boolean badBufferStrategyReported = false;
    
    public BufferStrategySurface(Container container) {
        this.container = container;
        this.defaultBackgroundColor = container.getBackground().getRGB();
        this.canvas = new Canvas() {

            @Override
            public void paint(java.awt.Graphics g) {
                onOSRepaint();
            }
            
            @Override
            public void update(java.awt.Graphics g) {
                onOSRepaint();
            }
        };
        container.removeAll();
        container.setLayout(null);
        canvas.setSize(1, 1);
        container.add(canvas);
        canvas.setLocation(0, 0);
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
    
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public synchronized void onOSRepaint() {
        osRepaint = true;
    }
    
    private synchronized void checkOSRepaint() {
        if (osRepaint) {
            osRepaint = false;
            setContentsLost(true);
        }
    }

    private BufferStrategy getBufferStrategy() {
        try {
            return canvas.getBufferStrategy();
        }
        catch (Exception ex) {
            if (!badBufferStrategyReported) {
                badBufferStrategyReported = true;
                // There is a ClassCastException when running under Eclipse 3.3 for Mac OS X Java 5.
                // My guess is there is a SWT conflict. This won't affect end users, but developers
                // will have to use appletviewer or a web browser to run PulpCore apps.
                Log.e("Couldn't create surface", ex);
            }
            return null;
        }
    }

    @Override
    public boolean isPrepared() {
        checkOSRepaint();
        int w = container.getWidth();
        int h = container.getHeight();
        return (w > 0 && h > 0 && image != null &&
                canvas.isDisplayable() && getBufferStrategy() != null);
    }

    @Override
    public boolean hasSizeChanged() {
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        int newWidth = container.getWidth();
        int newHeight = container.getHeight();
        boolean sizedChanged = false;

        if (newWidth <= 0 || newHeight <= 0) {
            return false;
        }
        else if (oldWidth != newWidth || oldHeight != newHeight) {
            setSize(newWidth, newHeight);
            sizedChanged = true;
        }

        if (!canvas.isDisplayable()) {
            return sizedChanged;
        }

        BufferStrategy bufferStrategy = getBufferStrategy();
        if (bufferStrategy == null) {
            useDirtyRects = false;
            createBufferStrategy();
            bufferStrategy = getBufferStrategy();
            if (bufferStrategy != null) {
                BufferCapabilities caps = bufferStrategy.getCapabilities();
                useDirtyRects = !caps.isPageFlipping();
            }
        }
        else if (!contentsLost()) {
            setContentsLost(bufferStrategy.contentsLost() | bufferStrategy.contentsRestored());
        }

        checkOSRepaint();
        
        return sizedChanged;
    }

    private void setSize(int w, int h) {
        image = new SoftTexture(w, h, true, defaultBackgroundColor);
        graphics = image.createGraphics();

        setContentsLost(true);
        canvas.setSize(w, h);

        if (bufferedImage != null) {
            bufferedImage.flush();
        }
        // Create the AWT image
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, w, h, new int[] { 0xff0000, 0x00ff00, 0x0000ff });
        DataBuffer dataBuffer = new DataBufferInt(image.getData(), w * h);
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        bufferedImage = new BufferedImage(COLOR_MODEL, raster, true, new Hashtable<Object, Object>());
    }

    @Override
    public void show(Recti[] dirtyRectangles, int numDirtyRectangles) {
        BufferStrategy bufferStrategy = getBufferStrategy();
        if (bufferStrategy == null || bufferedImage == null) {
            return;
        }
        
        if (numDirtyRectangles == 0 && !contentsLost()) {
            return;
        }
        
        java.awt.Graphics g = null;
        try {
            while (true) {
                while (true) {
                    g = bufferStrategy.getDrawGraphics();
            
                    if (g != null) {
                        if (contentsLost() || !useDirtyRects || numDirtyRectangles < 0) {
                            g.drawImage(bufferedImage, 0, 0, null);
                        }
                        else {
                            for (int i = 0; i < numDirtyRectangles; i++) {
                                Recti r = dirtyRectangles[i];
                                g.setClip(r.x, r.y, r.width, r.height);
                                g.drawImage(bufferedImage, 0, 0, null);
                            }
                        }
                        g.dispose();
                        g = null;
                    }
                    
                    if (bufferStrategy.contentsRestored()) {
                        setContentsLost(true);
                    }
                    else {
                        break;
                    }
                }

                bufferStrategy.show();
                
                if (bufferStrategy.contentsLost()) {
                    setContentsLost(true);
                }
                else {
                    setContentsLost(false);
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
    }
    
    private void createBufferStrategy() {
        canvas.createBufferStrategy(2);
        
        /* Try to enable vsync (Java 6u10)
        
            Note, "separate_jvm" property must be specified for applets that v-sync. From the bug
            description:
                Note that since the D3D pipeline uses single thread rendering - meainng
                that all D3D-related activity happens on a single thread only
                one BufferStrategy in per vm instance can be made v-synced without
                undesireable effects. 
                
                If there's more than one (say N) v-synced BSs then
                since their Present() calls will effectively be serialized (since they're
                running from a single thread) each BS will be able
                to flip only on every Nth vsync, resulting in decrease in
                perceived responsiveness.

            Unfortunately this doesn't work because sandboxed applets can't access sun.* packages.
            This is left here (commented out) in case the API gets moved to com.sun. 
        */
        /*
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();
        if (bufferStrategy != null) {
            BufferCapabilities caps = bufferStrategy.getCapabilities();
            try {
                Class ebcClass = Class.forName(
                    "sun.java2d.pipe.hw.ExtendedBufferCapabilities");
                Class vstClass = Class.forName(
                    "sun.java2d.pipe.hw.ExtendedBufferCapabilities$VSyncType");
                
                Constructor ebcConstructor = ebcClass.getConstructor(
                    new Class[] { BufferCapabilities.class, vstClass });
                Object vSyncType = vstClass.getField("VSYNC_ON").get(null);
                
                BufferCapabilities newCaps = (BufferCapabilities)ebcConstructor.newInstance(
                    new Object[] { caps, vSyncType });
                
                canvas.createBufferStrategy(2, newCaps);
                
                // TODO: if success, setCanChangeRefreshRate(false) and setRefreshRate(60). 
                // Possibly override refreshRateSync()?
            }
            catch (Throwable t) {
                // Ignore
                t.printStackTrace();
            }
        }
        */
    }
    
    @Override
    public String toString() {
        return "BufferStrategy";
    }
}

package org.pulpcore.runtime.lwjgl.graphics;

import java.awt.Frame;
import java.awt.Insets;
import org.pulpcore.runtime.lwjgl.LWJGLContext;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.math.Recti;
import org.pulpcore.runtime.Surface;

public class LWJGLSurface extends Surface {
    
    private LWJGLContext context;
    private LWJGLGraphics g;
    private int windowWidth;
    private int windowHeight;
    private boolean wasResized;
    
    public LWJGLSurface(LWJGLContext context, int defaultWidth, int defaultHeight) throws LWJGLException {
        this.context = context;
        this.windowWidth = defaultWidth;
        this.windowHeight = defaultHeight;
    }
    
    @Override
    public boolean contentsLost() {
        // Don't use dirty rectangles. Maybe add this back in later.
        return true;
    }

    @Override
    public boolean isPrepared() {
        if (!Display.isCreated()) {
            try {
                Display.setDisplayMode(new DisplayMode(windowWidth, windowHeight));
                Display.create();
                Display.setInitialBackground(0, 0, 0);
                Display.setResizable(true);
                Display.setVSyncEnabled(true);
                g = new LWJGLGraphics(windowWidth, windowHeight);
                wasResized = false;
            }
            catch (LWJGLException ex) {
                ex.printStackTrace(System.out);
                context.stop();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasSizeChanged() {
        return wasResized;
    }

    @Override
    public Graphics getGraphics() {
        if (g != null) {
            g.reset();
            // TODO: is fill() a hack? Needed when the stage is a different size than the window.
            g.fill();
        }
        return g;
    }

    @Override
    public int getWidth() {
        return (g == null ? 0 : g.getSurfaceWidth());
    }

    @Override
    public int getHeight() {
        return (g == null ? 0 : g.getSurfaceHeight());
    }

    @Override
    public void show(Recti[] dirtyRectangles, int numDirtyRectangles) {
        wasResized = false;
        Display.update();
        if (Display.wasResized()) {
            if (!"2.8.1".equals(Sys.getVersion())) {
                System.out.println("Only LWJGL 2.8.1 was tested with this resizing hack. You're running version " + Sys.getVersion());
            }
            int windowWidthFudge = 0;
            int windowHeightFudge = 0;
            if (System.getProperty("os.name").startsWith("Mac OS X")) {
                // Workaround hack: substract the Frame decoration from the display width/height.
                Frame[] frames = Frame.getFrames();
                if (frames != null && frames.length > 0) {
                    Insets i = frames[0].getInsets();
                    windowWidthFudge = -(i.left + i.right);
                    windowHeightFudge = -(i.top + i.bottom);
                }
            }
            windowWidth = Display.getWidth() + windowWidthFudge;
            windowHeight = Display.getHeight() + windowHeightFudge;
            g = new LWJGLGraphics(windowWidth, windowHeight);
            g.fill();
            Display.update();
            wasResized = true;
        }
    }    
}

package org.pulpcore.runtime.lwjgl.graphics;

import org.pulpcore.graphics.Color;
import org.pulpcore.math.AffineTransform;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;

public class LWJGLGraphics extends Graphics {
    
    private static int boundFrameBufferId = 0;
    private static boolean boundFrameBufferNeedsSetup = true;
    
    public static void setBoundFrameBufferId(int boundFrameBufferId) {
        if (LWJGLGraphics.boundFrameBufferId != boundFrameBufferId) {
            LWJGLGraphics.boundFrameBufferId = boundFrameBufferId;
            boundFrameBufferNeedsSetup = true;
        }
    }
    
    public static int getBoundFrameBufferId() {
        return boundFrameBufferId;
    }
    
    private final LWJGLTexture surface;
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final boolean surfaceOpaque;
    private final int surfaceFrameBufferId;
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private BlendMode lastBlendMode = null;
    
    /**
    Render to texture.
    @param surface 
    */
    public LWJGLGraphics(LWJGLTexture surface) {
        this.surface = surface;
        this.surfaceWidth = surface.getWidth();
        this.surfaceHeight = surface.getHeight();
        this.surfaceOpaque = surface.isOpaque();
        this.surfaceFrameBufferId = surface.getFrameBufferId();
        init();
    }
    
    /**
    Render to the back buffer.
    @param surfaceWidth
    @param surfaceHeight 
    */
    public LWJGLGraphics(int surfaceWidth, int surfaceHeight) {
        this.surface = null;
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.surfaceOpaque = true;
        this.surfaceFrameBufferId = 0;
        if (boundFrameBufferId == 0) {
            boundFrameBufferNeedsSetup = true;
        }
        init();
    }
    
    private void init() {
        matrix.put(0, 1.0f);
        matrix.put(5, 1.0f);
        matrix.put(10, 1.0f);
        matrix.put(15, 1.0f);
        
        reset();
        //setupSurface();
    }
    
    @Override
    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    @Override
    public int getSurfaceHeight() {
        return surfaceHeight;
    }
    
    public boolean isSurfaceOpaque() {
        return surfaceOpaque;
    }

    private void setupSurface() {
        
        if (boundFrameBufferId != surfaceFrameBufferId) {
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, surfaceFrameBufferId);
            boundFrameBufferId = surfaceFrameBufferId;
            boundFrameBufferNeedsSetup = true;
        }
        
        if (boundFrameBufferNeedsSetup) {

            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, getSurfaceWidth(), getSurfaceHeight(), 0, -1, 1);

            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());

            if (isSurfaceOpaque()) {
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            }
            else {
                glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            }

            //glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glEnable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);
            boundFrameBufferNeedsSetup = false;
            lastBlendMode = null;
        }
    }
    
    private void beginRender(boolean isTexture) {
        setupSurface();
        
        if (lastBlendMode != getBlendMode()) {
            lastBlendMode = getBlendMode();
            /*
            Blend functions for premultiplied textures from
            http://worldwind.arc.nasa.gov/java/jogl/docs/com/sun/opengl/util/texture/Texture.html
            */
            switch (lastBlendMode) {
                default: case SRC_OVER:
                    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case ADD:
                    glBlendFunc(GL_ONE, GL_ONE);
                    break;
                case MULT:
                    // Not 100% sure this is right
                    glBlendFunc(GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case CLEAR:
                    glBlendFunc(GL_ZERO, GL_ZERO);
                    break;

                // These are probably used less often
                case DST_OVER:
                    glBlendFunc(GL_ONE_MINUS_DST_ALPHA, GL_ONE);
                    break;
                case SRC:
                    glBlendFunc(GL_ONE, GL_ZERO);
                    break;
                case DST:
                    glBlendFunc(GL_ZERO, GL_ONE);
                    break;
                case SRC_ATOP:
                    glBlendFunc(GL_DST_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case DST_ATOP:
                    glBlendFunc(GL_ONE_MINUS_DST_ALPHA, GL_SRC_ALPHA);
                    break;
                case SRC_IN:   
                    glBlendFunc(GL_DST_ALPHA, GL_ZERO);
                    break;
                case DST_IN:   
                    glBlendFunc(GL_ZERO, GL_SRC_ALPHA);
                    break;
                case SRC_OUT:
                    glBlendFunc(GL_ONE_MINUS_DST_ALPHA, GL_ZERO);
                    break;
                case DST_OUT:
                    glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA);
                    break;
            }
        }

        // Set clip
        // TODO: I think this is wrong. (0,0) is lower-right corner in window coordinates?
        // NOTE: This may be a problem. PulpCore puts the clip in surface coordinates, 
        // but OpenGL puts the scissor in "window coordinates".
        glScissor(getClipX(), getClipY(), getClipWidth(), getClipHeight());

        // Set transform
        // TODO: I'm guessing setting the matrix for each sprite isn't the best idea. 
        // Displays Lists? VBOs? 
        // NOTE: The Scissor clip doesn't change often, but the opacity may change often.
        AffineTransform t = getCurrentTransform();
        matrix.put(0, t.getScaleX());
        matrix.put(5, t.getScaleY());
        matrix.put(1, -t.getShearX());
        matrix.put(4, -t.getShearY());
        matrix.put(12, t.getTranslateX());
        matrix.put(13, t.getTranslateY());
        int x = surface == null ? 0 : surface.getOffsetX();
        int y = surface == null ? 0 : surface.getOffsetY();

        // TODO: Offset primitives by 0.375, 0.375? See http://glprogramming.com/red/appendixg.html#name1
        if (x == 0 && y == 0) {
            glLoadMatrix(matrix);
        }
        else {
            glLoadIdentity();
            glTranslatef(x, y, 0);
            glMultMatrix(matrix);
        }
       
        // Set color/alpha
        if (isTexture) {
            glColor4ub((byte)0xff, (byte)0xff, (byte)0xff, (byte)getAlpha());
        }
        else {
            int color = getColor();
            int a = Color.getAlpha(color);
            int r = Color.getRed(color);
            int g = Color.getGreen(color);
            int b = Color.getBlue(color);

            if (getAlpha() < 0xff) {
                a = (getAlpha() * a + 127) / 255;
            }
            if (a < 0xff) {
                // Premultiply
                r = (a * r + 127) / 255;
                g = (a * g + 127) / 255;
                b = (a * b + 127) / 255;
            }
            glColor4ub((byte)r, (byte)g, (byte)b, (byte)a);
        }
    }
    
    
    private void endRender(boolean isTexture) {
        if (isTexture) {
            
        }
    }

    // TODO: Kill this function? Apparantly some OpenGL drivers drop to software rendering
    // when GL_LINE_SMOOTH is used. Also, it doesn't scale on the software renderer.
    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        beginRender(false);
        
        glEnable(GL_LINE_SMOOTH);
        glBegin(GL_LINES);
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
        glEnd();
        glDisable(GL_LINE_SMOOTH);
        
        endRender(false);
    }

    @Override
    public void fillRect(float w, float h) {
        if (w != 0 && h != 0 && getAlpha() > 0) {
            beginRender(false);
            glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(w, 0);
            glVertex2f(w, h);
            glVertex2f(0, h);
            glEnd();
            endRender(false);
        }
    }

    @Override
    public void drawTexture(Texture texture) {
        if (texture != null && getAlpha() > 0) {
            if (!(texture instanceof LWJGLTexture)) {
                throw new IllegalArgumentException("Texture not compatible with GLGraphics");
            }     
            
            beginRender(true);
            glEnable(GL_TEXTURE_2D);
            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        
            LWJGLTexture t = (LWJGLTexture)texture;
            glBindTexture(GL_TEXTURE_2D, t.getTextureId());
//            t.setInterpolation(getInterpolation());
//            t.setEdgeClamp(getEdgeClamp());

            glTexCoordPointer(2, 0, t.getTextureCoords());
            glVertexPointer(2, 0, t.getVertexCoords());
            glDrawArrays(GL_QUADS, 0, 4);
            
            glDisableClientState(GL_VERTEX_ARRAY);
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            glDisable(GL_TEXTURE_2D);
            endRender(true);
        }
    }
}

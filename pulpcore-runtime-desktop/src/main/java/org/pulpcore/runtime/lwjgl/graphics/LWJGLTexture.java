package org.pulpcore.runtime.lwjgl.graphics;

import org.pulpcore.runtime.jre.PNGFile;
import org.lwjgl.opengl.GLContext;
import org.pulpcore.runtime.lwjgl.LWJGLContext;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL12;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Graphics.Interpolation;
import org.pulpcore.graphics.Texture;
import org.pulpcore.runtime.Context;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;

public class LWJGLTexture extends Texture {
    
    public static final Color.Format PIXEL_FORMAT = Color.Format.RGBA_PREMULTIPLIED;

    public static class IDs {
        public final int textureId;
        private int frameBufferId;
        
        public IDs(int textureId) {
            this(textureId, 0);
        }
        
        public IDs(IDs src) {
            this(src.textureId, src.frameBufferId);
        }
        
        public IDs(int textureId, int frameBufferId) {
            this.textureId = textureId;
            this.frameBufferId = frameBufferId;
        }

        /**
         * @return the frameBufferId
         */
        public int getFrameBufferId() {
            return frameBufferId;
        }

        /**
         * @param frameBufferId the frameBufferId to set
         */
        public void setFrameBufferId(int frameBufferId) {
            this.frameBufferId = frameBufferId;
        }
        
    }
    
    private static IntBuffer PIXEL_BUFFER = ByteBuffer.allocateDirect(4).asIntBuffer();
    
    private final LWJGLTexture sourceTexture;
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;
    private final boolean opaque;
    private final boolean mutable;
    private final FloatBuffer textureCoords;
    private final FloatBuffer vertexCoords;
    private final IDs ids;
    
    private Interpolation interpolation;
    private int edgeClamp = -1;
    
    /**
     Creates a copy of a texture
     */
    private LWJGLTexture(LWJGLTexture sourceTexture, int textureWidth, int textureHeight, boolean mutable) {
        this.sourceTexture = null;
        this.offsetX = 0;
        this.offsetY = 0;
        this.width = textureWidth;
        this.height = textureHeight;
        this.opaque = sourceTexture.isOpaque();
        this.mutable = mutable;
        this.textureCoords = makeTextureCoords();
        this.vertexCoords = makeVertexCoords();
        this.ids = new IDs(glGenTextures());
        
        glBindTexture(GL_TEXTURE_2D, ids.textureId);
        setInterpolation(Interpolation.BILINEAR);
        setEdgeClamp(Graphics.EDGE_CLAMP_NONE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer)null);
        
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new RuntimeException("Error creating texture: " + error);
        }
        LWJGLGraphics g = new LWJGLGraphics(this);
        g.setBlendMode(Graphics.BlendMode.SRC);
        g.drawTexture(sourceTexture);
    }
    
    /**
     Creates a subimage of the specified image, sharing the texture data.
     */
    private LWJGLTexture(LWJGLTexture sourceTexture, int x, int y, int width, int height) {
        // Keep a reference to the root sourceTexture for caching purposes. This prevents the original
        // LWJGLTexture object from being removed from the cache in DesktopContext, which wouldn't
        // be a good idea since the texture data is shared.
        this.sourceTexture = sourceTexture.sourceTexture != null ? sourceTexture.sourceTexture : sourceTexture;
        this.offsetX = sourceTexture.getOffsetX() + x;
        this.offsetY = sourceTexture.getOffsetY() + y;
        this.width = width;
        this.height = height;
        this.opaque = sourceTexture.opaque;
        this.mutable = sourceTexture.mutable;
        this.ids = new IDs(sourceTexture.ids);
        this.textureCoords = makeTextureCoords();
        this.vertexCoords = makeVertexCoords();
    }
   
    /**
     Create a blank, mutable image. Opaque images will be set to BLACK, and non-opaque images
     will be set to TRANSPARENT.
     */
    public LWJGLTexture(int width, int height, boolean isOpaque) {
        this(width, height, isOpaque, isOpaque ? Color.BLACK : Color.TRANSPARENT);
    }

    /**
     Create a mutable image initially set to the specified color.
     */
    public LWJGLTexture(int width, int height, boolean isOpaque, int argbColor) {
        this.sourceTexture = null;
        this.offsetX = 0;
        this.offsetY = 0;
        this.width = width;
        this.height = height;
        this.opaque = isOpaque;
        this.mutable = true;
        this.textureCoords = makeTextureCoords();
        this.vertexCoords = makeVertexCoords();
        this.ids = new IDs(glGenTextures());
        if (isOpaque) {
            argbColor = Color.rgb(argbColor);
        }
        
        glBindTexture(GL_TEXTURE_2D, ids.textureId);
        setInterpolation(Interpolation.BILINEAR);
        setEdgeClamp(Graphics.EDGE_CLAMP_NONE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer)null);
        
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new RuntimeException("Error creating texture: " + error);
        }
        LWJGLGraphics g = (LWJGLGraphics)createGraphics();
        g.setColor(argbColor);
        g.fill();
    }

    /**
     Creates a new image. For immutable images, the texture data must not be shared with any
     other image, nor should it be modified after using this constructor.
     */
    public LWJGLTexture(int width, int height, boolean isOpaque, IntBuffer data, boolean isMutable) {
        if (data == null || data.limit() != width * height) {
            throw new IllegalArgumentException("Invalid data length");
        }
        this.sourceTexture = null;
        this.offsetX = 0;
        this.offsetY = 0;
        this.width = width;
        this.height = height;
        this.opaque = isOpaque;
        this.mutable = isMutable;
        this.textureCoords = makeTextureCoords();
        this.vertexCoords = makeVertexCoords();
        this.ids = new IDs(glGenTextures());
        glBindTexture(GL_TEXTURE_2D, ids.textureId);
        setInterpolation(Interpolation.BILINEAR);
        setEdgeClamp(Graphics.EDGE_CLAMP_NONE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new RuntimeException("Error creating texture: " + error);
        }
    }
    
    @Override
    public Texture createMutableCopy() {
        return createCopy(true);
    }

    @Override
    public Texture createImmutableCopy() {
        if (isMutable() || isSubTexture()) {
            return createCopy(false);
        }
        else {
            return this;
        }
    }
    
    private LWJGLTexture createCopy(boolean mutable) {
        boolean supportsNPOT = GLContext.getCapabilities().GL_ARB_texture_non_power_of_two;
        int textureWidth;
        int textureHeight;
        if (supportsNPOT) {
            textureWidth = width;
            textureHeight = height;
        }
        else {
            textureWidth = PNGFile.nextHighestPowerOfTwo(width);
            textureHeight = PNGFile.nextHighestPowerOfTwo(height);
        }
        LWJGLTexture texture = new LWJGLTexture(this, textureWidth, textureHeight, mutable);
        if (width != textureWidth || height != textureHeight) {
            texture = (LWJGLTexture)texture.getSubTexture(0, 0, width, height);
        }
        ((LWJGLContext)Context.getContext()).cacheTexture(null, texture);
        return texture;
    }

    public FloatBuffer getTextureCoords() {
        return textureCoords;
    }

    public FloatBuffer getVertexCoords() {
        return vertexCoords;
    }
    
    public IDs getIDs() {
        return ids;
    }
    
    private FloatBuffer makeTextureCoords() {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(8);
        if (sourceTexture == null) {
            buffer.put(0).put(0); 
            buffer.put(1).put(0); 
            buffer.put(1).put(1); 
            buffer.put(0).put(1); 
        }
        else {
            float w = sourceTexture.getWidth();
            float h = sourceTexture.getHeight();
            float x1 = getOffsetX() / w;
            float y2 = 1 - (getOffsetY() / h);
            float x2 = (getOffsetX() + getWidth()) / w;
            float y1 = 1 - ((getOffsetY() + getHeight()) / h);
            buffer.put(x1).put(y1); 
            buffer.put(x2).put(y1); 
            buffer.put(x2).put(y2); 
            buffer.put(x1).put(y2); 
        }
        buffer.rewind();
        return buffer;
    }
    
    private FloatBuffer makeVertexCoords() {
        float w = getWidth();
        float h = getHeight();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(8);
        buffer.put(0).put(h); 
        buffer.put(w).put(h);
        buffer.put(w).put(0);
        buffer.put(0).put(0);
        buffer.rewind();
        return buffer;
    }
    
    public void setInterpolation(Interpolation interpolation) {
        if (sourceTexture != null) {
            sourceTexture.setInterpolation(interpolation);
        }
        else if (this.interpolation != interpolation) {
            this.interpolation = interpolation;
            if (interpolation == Interpolation.BILINEAR) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
            else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
        }
    }
    
    public void setEdgeClamp(int edgeClamp) {
        if (sourceTexture != null) {
            sourceTexture.setEdgeClamp(edgeClamp);
        }
        else if (this.edgeClamp != edgeClamp) {
            this.edgeClamp = edgeClamp;
            // In OpenGL, we can't clamp each edge seperatly (unless I'm missing something). 
            // So that tiled images look correct, clamp both left and right edges 
            // if either is set to clamp.
            if ((edgeClamp & Graphics.EDGE_CLAMP_LEFT) != 0 ||
                    (edgeClamp & Graphics.EDGE_CLAMP_RIGHT) != 0) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            }
            else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            }
            
            if ((edgeClamp & Graphics.EDGE_CLAMP_TOP) != 0 ||
                    (edgeClamp & Graphics.EDGE_CLAMP_BOTTOM) != 0) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            }
            else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            }
        }
    }
    
    private void checkFBOError() {
        int frameBufferStatus = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT); 
        switch (frameBufferStatus) {
            case GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception" );
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception" );
            case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception" );
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception" );
            case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception" );
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new RuntimeException( "FrameBuffer: " + ids.getFrameBufferId()
                                    + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception" );
            default:
                throw new RuntimeException( "Unexpected reply from glCheckFramebufferStatusEXT: " + frameBufferStatus );
        }       

    }
        
    public int getFrameBufferId() {
        if (ids.getFrameBufferId() == 0) {
            if (sourceTexture != null) {
                ids.setFrameBufferId(sourceTexture.getFrameBufferId());
            }
            else {
                ids.setFrameBufferId(glGenFramebuffersEXT());
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, ids.getFrameBufferId());
                glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, ids.textureId, 0);
                LWJGLGraphics.setBoundFrameBufferId(ids.getFrameBufferId());
                checkFBOError();
            }
        }
        return ids.getFrameBufferId();
    }
    
    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }
    
    public int getTextureId() {
        return ids.textureId;
    }
    
    public LWJGLTexture getSource() {
        return sourceTexture;
    }
    
    public boolean isSubTexture() {
        return sourceTexture != null;
    }

    @Override
    public boolean isTransparent(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            return true;
        }
        else if (isOpaque()) {
            return false;
        }
        else {
            // Assume RGBA_PREMULTIPLIED
            return (getRawPixel(x, y) & 0xff) == 0;
        }
    }

    @Override
    public int getARGB(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            return 0;
        }
        else {
            return Color.convert(PIXEL_FORMAT, Color.Format.ARGB, getRawPixel(x, y));
        }
    }
    
    private int getRawPixel(int x, int y) {
        if (sourceTexture != null) {
            return sourceTexture.getRawPixel(x + getOffsetX(), y + getOffsetY());
        }
        else {
            int fbId = getFrameBufferId();
            if (LWJGLGraphics.getBoundFrameBufferId() != fbId) {
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, getFrameBufferId());
                LWJGLGraphics.setBoundFrameBufferId(getFrameBufferId());
            }
            PIXEL_BUFFER.rewind();
            int flipY = height - y - 1;
            glReadPixels(x, flipY, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, PIXEL_BUFFER);
            int pixel = PIXEL_BUFFER.get(0);
//            int convertedPixel = Color.convert(PIXEL_FORMAT, Color.Format.ARGB, pixel);
//            System.out.println("Read a pixel: " + Color.toString(convertedPixel));
            return pixel;
        }
    }
    
    @Override
    public Graphics createGraphics() {
        if (isMutable()) {
            return new LWJGLGraphics(this);
        }
        else {
            throw new RuntimeException("Texture is not mutable: " + this);
        }
    }

    @Override
    public Texture getSubTexture(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > getWidth() || y + height > getHeight()) {
            throw new IllegalArgumentException("Subregion region not valid");
        }
        return new LWJGLTexture(this, x, y, width, height);
    }    
}

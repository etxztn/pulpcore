package org.pulpcore.runtime.lwjgl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GLContext;
import org.pulpcore.graphics.Texture;
import org.pulpcore.media.Audio;
import org.pulpcore.runtime.Context;
import org.pulpcore.runtime.Input;
import org.pulpcore.runtime.desktop.Main;
import org.pulpcore.runtime.lwjgl.graphics.LWJGLTexture;
import org.pulpcore.runtime.jre.JreContext;
import org.pulpcore.runtime.jre.PNGFile;
import org.pulpcore.runtime.lwjgl.audio.LWJGLAudioEngine;
import org.pulpcore.runtime.lwjgl.audio.BufferedAudio;
import org.pulpcore.view.Scene;

public class LWJGLContext extends JreContext {

    // Keeps a copy of the textureID, so glDeleteTextures can be called when there are no longer
    // any references to the texture.
    private static class GLCachedTexture {
        
        final WeakReference<LWJGLTexture> textureRef;
        final LWJGLTexture.IDs ids;
        
        public GLCachedTexture(LWJGLTexture texture) {
            this.textureRef = new WeakReference<LWJGLTexture>(texture);
            this.ids = texture.isSubTexture() ? null : texture.getIDs();
        }
        
        public LWJGLTexture get() {
            LWJGLTexture texture = textureRef.get();
            if (texture == null && ids != null) {
                if (ids.getFrameBufferId() != 0) {
                    //System.out.println("--Deleted FBO");
                    glDeleteFramebuffersEXT(ids.getFrameBufferId());
                }
                //System.out.println("--Deleted Texture");
                glDeleteTextures(ids.textureId);
            }
            return texture;
        }
    }
    
    private final Main main;
    private final LWJGLInput input = new LWJGLInput();
    private final Class<? extends Scene> firstScene;
    private LWJGLAudioEngine audioEngine;
    private BufferedAudio brokenAudio;

    private final HashMap<String, GLCachedTexture> textureCache =
        new HashMap<String, GLCachedTexture>();
    private final HashMap<String, WeakReference<? extends Audio>> audioCache =
        new HashMap<String, WeakReference<? extends Audio>>();
    
    public LWJGLContext(Main main, Class<? extends Scene> firstScene) {
        this.main = main;
        this.firstScene = firstScene;
        
        Context.setContextFactory(new Context.Factory() {

            @Override
            public Context getContext() {
                return LWJGLContext.this;
            }
        });
    }
       
    @Override
    public void destroy() {
        super.destroy();
        if (audioEngine != null) {
            audioEngine.destroy();
            audioEngine = null;
        }
    }
    
    @Override
    public void tick(float dt) {
        if (audioEngine != null) {
            audioEngine.tick(dt);
        }
        if (Display.isCreated() && Display.isCloseRequested()) {
            stop();
        }
    }
    
    public void stop() {
        main.stop();
    }
    
    @Override
    public VersionInfo getBrowserInfo() {
        return null;
    }
    
    @Override
    public Input getInput() {
        return input;
    }

    @Override
    public Scene createFirstScene() {
        try {
            return firstScene.newInstance();
        }
        catch (InstantiationException ex) {
            ex.printStackTrace(System.out);
        }
        catch (IllegalAccessException ex) {
            ex.printStackTrace(System.out);
        }
        return null;
    }
    
    //
    // Resources
    //
    
    @Override
    protected InputStream getResourceAsStream(String assetName) {
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }

        Class<? extends LWJGLContext> parentLoader = getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);

        if (in == null) {
            System.out.println("Asset not found: " + assetName);
        }
        return in;
    }
    
    private void purgeTextureCache() {
        //System.out.println("===Start purge===");
        Iterator<String> i = textureCache.keySet().iterator();
        while (i.hasNext()) {
            String name = i.next();
            GLCachedTexture cachedTexture = textureCache.get(name);
            if (cachedTexture != null) {
                LWJGLTexture texture = cachedTexture.get();
                if (texture == null) {
                    //System.out.println("Deleted Texture: " + name);
                    i.remove();
                }
                else {
                    //System.out.println("Kept Texture: " + name);
                }
            }
        }
        //System.out.println("===End purge===");
    }
    
    private LWJGLTexture getCachedTexture(String name) {
        GLCachedTexture cachedTexture = textureCache.get(name);
        if (cachedTexture != null) {
            LWJGLTexture texture = cachedTexture.get();
            if (texture == null) {
                textureCache.remove(name);
            }
        }
        return null;
    }
    
    @Override
    public Texture loadTexture(String textureName) {
        LWJGLTexture texture = getCachedTexture(textureName);
        if (texture != null) {
            return texture;
        }
        
        // About to load a new texture - purge the cache 
        purgeTextureCache();
        
        // Attempt to load raw bytes from the asset collection
        InputStream in = getResourceAsStream(textureName);
        if (in == null) {
            return null;
        }

        // Use PNG Reader
        if (textureName.toLowerCase().endsWith(".png")) {
            PNGFile pngFile = null;
            boolean supportsNPOT = GLContext.getCapabilities().GL_ARB_texture_non_power_of_two;
            try {
                pngFile = PNGFile.load(in, LWJGLTexture.PIXEL_FORMAT, true, true, !supportsNPOT);
                in.close();
            }
            catch (IOException ex) {
                ex.printStackTrace(System.out);
            }
            if (pngFile != null) {
                texture = new LWJGLTexture(pngFile.getTextureWidth(), pngFile.getTextureHeight(), 
                        pngFile.isOpaque(), pngFile.getData(), false);
                if (pngFile.getImageWidth() != pngFile.getTextureWidth() ||
                        pngFile.getImageHeight() != pngFile.getTextureHeight()) {
                    texture = (LWJGLTexture)texture.getSubTexture(0, 0, pngFile.getImageWidth(), pngFile.getImageHeight());
                }
            }
        }
        else {
            try {
                in.close();
            }
            catch (IOException ex) { }
        }

        if (texture == null) {
            System.out.println("Couldn't load texture: " + textureName);
        }
        else {
            cacheTexture(textureName, texture);
        }
        
        return texture;
    }
    
    public void cacheTexture(String name, LWJGLTexture texture) {
        if (texture == null) {
            return;
        }
        if (name == null) {
            name = "$" + texture.getTextureId();
        }
        textureCache.put(name, new GLCachedTexture(texture));
        // Texture may be a subtexture for NPOT textures
        // Add it to the cache so that it can be properly deleted later, using glDeleteTextures.
        cacheTexture(name + "$parent", texture.getSource());
    }
    
    @Override
    public Texture createTexture(int width, int height, boolean opaque) {
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
        LWJGLTexture texture = new LWJGLTexture(textureWidth, textureHeight, opaque);
        if (width != textureWidth || height != textureHeight) {
            texture = (LWJGLTexture)texture.getSubTexture(0, 0, width, height);
        }
        cacheTexture(null, texture);
        return texture;
    }
    
    //
    // Audio
    //
    
    public LWJGLAudioEngine getAudioEngine() {
        if (audioEngine == null) {
            audioEngine = new LWJGLAudioEngine();
            setEngineVolume();
        }
        return audioEngine;
    }
    
    private BufferedAudio getBrokenAudio() {
        if (brokenAudio == null) {
            brokenAudio = new BufferedAudio(getAudioEngine());
        }
        return brokenAudio;
    }

    @Override
    public Audio loadAudio(String audioName) {
        // Init sound engine
        getAudioEngine();
        
        // Use OGG instead of MP3.
        if (audioName != null && audioName.toLowerCase().endsWith(".mp3")) {
            audioName = audioName.substring(0, audioName.length() - 4) + ".ogg";
        }
        
        // First try the cache
        WeakReference<? extends Audio> soundRef = audioCache.get(audioName);
        if (soundRef != null) {
            Audio sound = soundRef.get();
            if (sound != null) {
                return sound;
            }
            else {
                audioCache.remove(audioName);
            }
        }
        
        // Load the sound
        Audio audio = null;
        if (audioName != null) {
            InputStream in = null;
            try {
                in = getResourceAsStream(audioName);
                if (in == null) {
                    System.out.println("Couldn't load sound file: " + audioName + ". No such file.");
                }
                else {
                    if (audioName.toLowerCase().endsWith(".wav")) {
                        audio = audioEngine.loadWAV(audioName, in);
                    }
                    else if (audioName.toLowerCase().endsWith(".ogg")) {
                        audio = audioEngine.loadOGG(audioName, in);
                    }
                    else {
                       System.out.println("Unknown sound file type: " + audioName);
                    }
                    in.close();
                }
            }
            catch (IOException ex) {
                if (audio == null) {
                    System.out.println("Couldn't load sound file: " + audioName + ". " + ex.getMessage());
                }
                if (in != null) {
                    try {
                        in.close();
                    }
                    catch (IOException ex2) { }
                }
            }
        }

        if (audio == null) {
            audio = getBrokenAudio();
        }

        audioCache.put(audioName, new WeakReference<Audio>(audio));
        
        return audio;
    }
    
    private void setEngineVolume() {
        if (audioEngine != null) {
            audioEngine.setVolume(getMasterMute() ? 0 : getMasterVolume());
        }
    }
  
    @Override
    public void setMasterMute(boolean mute) {
        if (mute != getMasterMute()) {
            super.setMasterMute(mute);
            setEngineVolume();
        }
    }

    @Override
    public void setMasterVolume(float volume) {
        if (volume != getMasterVolume()) {
            super.setMasterVolume(volume);
            setEngineVolume();
        }
    }

    @Override
    public String[] getSupportedSoundFormats() {
        return new String[] { "wav", "ogg" };
    }
}

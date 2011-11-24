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
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteOrder;
import java.util.HashMap;
import org.pulpcore.runtime.Context;
import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Color.Format;
import org.pulpcore.graphics.Texture;
import org.pulpcore.runtime.Surface;
import org.pulpcore.runtime.Input;
import org.pulpcore.runtime.softgraphics.SoftTexture;
import org.pulpcore.runtime.applet.audio.AppletAudio;
import org.pulpcore.runtime.applet.audio.AppletAudioEngine;
import org.pulpcore.runtime.applet.audio.RawAudio;
import org.pulpcore.runtime.applet.audio.VorbisAudio;
import org.pulpcore.runtime.jre.JreContext;
import org.pulpcore.runtime.jre.PNGFile;
import org.pulpcore.runtime.jre.WAVFile;
import org.pulpcore.media.Audio;
import org.pulpcore.view.Scene;

public class AppletContext extends JreContext {

    public static final InheritableThreadLocal<AppletContext> CONTEXTS =
            new InheritableThreadLocal<AppletContext>();

    static {
        Context.setContextFactory(new Context.Factory() {
            @Override
            public Context getContext() {
                return CONTEXTS.get();
            }
        });
    }

    private final Main applet;
    private final VersionInfo browserInfo;
    private AppletAudio brokenAudio;
    private AppletAudioEngine audioEngine;
    private Surface surface;
    private AppletInput input;

    private final HashMap<String, WeakReference<? extends Texture>> textureCache =
        new HashMap<String, WeakReference<? extends Texture>>();
    private final HashMap<String, WeakReference<? extends AppletAudio>> soundCache =
        new HashMap<String, WeakReference<? extends AppletAudio>>();

    public AppletContext(Main applet) {
        this.applet = applet;
        browserInfo = new VersionInfo(getParameter("pulpcore_browser_name", ""), getParameter("pulpcore_browser_version", ""));
        createSurface();

        //setTalkBackField("pulpcore.platform.surface", surface.toString());

    }

    @Override
    public void registerContextOnThisThread() {
        CONTEXTS.set(this);
    }

    public String getParameter(String name) {
        return getParameter(name, null);
    }

    public String getParameter(String name, String nullDefault) {
        String value = applet.getParameter(name);
        if (value == null) {
            value = nullDefault;
        }
        return value;
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
    }
    
    @Override
    public Scene createFirstScene() {
        return applet.createFirstScene();
    }

    public Surface getSurface() {
        return surface;
    }

    @Override
    public Input getInput() {
        return input;
    }

    @Override
    public VersionInfo getBrowserInfo() {
        return browserInfo;
    }

    @Override
    public boolean isNativeClipboard() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard();
            return true;
        }
        catch (SecurityException ex) {
            return false;
        }
    }

    @Override
    public String getClipboardText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            int attemptsLeft = 10; // 10*100ms = 1 second
            while (attemptsLeft > 0) {
                try {
                    Transferable contents = clipboard.getContents(null);
                    Object data = contents.getTransferData(DataFlavor.stringFlavor);
                    if (data instanceof String) {
                        return (String)data;
                    }
                    else {
                        // Shouldn't happen
                        return "";
                    }
                }
                catch (UnsupportedFlavorException ex) {
                    // String text not available
                    return "";
                }
                catch (IOException ex) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ie) {
                        // Ignore
                    }
                    attemptsLeft--;
                }
                catch (IllegalStateException ex) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ie) {
                        // Ignore
                    }
                    attemptsLeft--;
                }
            }
        }
        catch (SecurityException ex) {
            // The applet doesn't have permission
        }

        // Failure: use internal clipboard
        return super.getClipboardText();
    }

    @Override
    public void setClipboardText(String text) {
        if (text == null) {
            text = "";
        }

        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection data = new StringSelection(text);
            int attemptsLeft = 10; // 10*100ms = 1 second

            while (attemptsLeft > 0) {
                try {
                    clipboard.setContents(data, data);
                    // Success
                    return;
                }
                catch (IllegalStateException ex) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ie) {
                        // Ignore
                    }
                    attemptsLeft--;
                }
            }
        }
        catch (SecurityException ex) {
            // The applet doesn't have permission
        }

        // Failure: use internal clipboard
        super.setClipboardText(text);
    }

    @Override
    protected InputStream getResourceAsStream(String assetName) {
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }

        // Check loaded zip file(s)
//        byte[] assetData = getBytes(assetName);
//        if (assetData != null) {
//            return new ByteArray(assetData);
//        }

        // Check the jar file, then the server
        Class<? extends AppletContext> parentLoader = getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);

        if (in == null) {
            Log.i("Asset not found: " + assetName);
        }
        return in;
    }

    private byte[] getResourceAsByteArray(String assetName) {
        InputStream in = getResourceAsStream(assetName);
        if (in != null) {
            try {
                byte[] data = toByteArray(in);
                in.close();
                return data;
            }
            catch (IOException ex) { }
        }
        return null;
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int count;
        byte[] data = new byte[16384];

        while ((count = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, count);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    @Override
    public Texture createTexture(int width, int height, boolean isOpaque) {
        return new SoftTexture(width, height, isOpaque);
    }


    @Override
    public Texture loadTexture(String textureName) {
        // Texture cache exists so that texture data isn't loaded multiple times.
        WeakReference<? extends Texture> textureRef = textureCache.get(textureName);
        if (textureRef != null) {
            Texture texture = textureRef.get();
            if (texture != null) {
                return texture;
            }
            else {
                textureCache.remove(textureName);
            }
        }

        // Attempt to load raw bytes from the asset collection
        InputStream in = getResourceAsStream(textureName);
        if (in == null) {
            return null;
        }

        Texture texture = null;

        // Use PNG Reader
        if (textureName.toLowerCase().endsWith(".png")) {
            PNGFile pngFile = null;
            try {
                pngFile = PNGFile.load(in, Format.ARGB_PREMULTIPLIED, false, false, false);
                in.close();
            }
            catch (IOException ex) {
                ex.printStackTrace(System.out);
            }
            if (pngFile != null) {
                texture = new SoftTexture(pngFile.getImageWidth(), pngFile.getImageHeight(), pngFile.isOpaque(), 
                        pngFile.getData().array(), false);
            }
        }
        else {
            try {
                in.close();
            }
            catch (IOException ex) { }
        }

        // Use AWT Loader
        if (texture == null) {
            texture = loadTextureViaAWT(getResourceAsByteArray(textureName));
        }

        // Give up
        if (texture == null) {
            Log.i("Could not load image: " + textureName);
            return null;
        }

        textureCache.put(textureName, new WeakReference<Texture>(texture));

        return texture;
    }

    private Texture loadTextureViaAWT(byte[] in) {

        if (in == null) {
            return null;
        }
     
        // There were problems with ImageIO - it tried to access files on the server, which cause
        // ridiculous delays in an Applet environment. PixelGrabber works fine.
        java.awt.Image image = Toolkit.getDefaultToolkit().createImage(in);

        if (image == null) {
            return null;
        }

        MediaTracker tracker = new MediaTracker(applet);
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        }
        catch (InterruptedException ex) { }

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }

        int[] data = new int[width * height];
        PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, data, 0, width);
        boolean success = false;
        try {
            success = pixelGrabber.grabPixels();
        }
        catch (InterruptedException ex) { }

        if (success) {

            // Check if has transparency
            boolean isOpaque = true;
            for (int i = 0; i < data.length; i++) {
                if (!Color.isOpaque(data[i])) {
                    isOpaque = false;
                    break;
                }
            }

            // Premultiply alpha
            if (!isOpaque) {
                Color.convert(Color.Format.ARGB, Color.Format.ARGB_PREMULTIPLIED, data);
            }
            return new SoftTexture(width, height, isOpaque, data, false);
        }
        else {
            return null;
        }
    }

    private void createSurface() {
        Component inputComponent = applet;
        surface = null;

        boolean isMac = getOSInfo().getName().startsWith("Mac OS X");
        boolean useBufferStrategy = false;
        String useBufferStrategyParam = getParameter("pulpcore_use_bufferstrategy");
        if (useBufferStrategyParam != null) {
            useBufferStrategyParam = useBufferStrategyParam.toLowerCase();
        }

        /*
            If "pulpcore_use_bufferstrategy" is neither "true" or "false", then:

            BufferStrategy has a problem on:
            * Mac OS X 10.5 (Leopard) - uses lots of CPU. Cannot reach 60fps (55fps max).
            * Java 6u10 - softare rendering is slower
            * All Windows? We found one machine where BufferStrategy was a CPU hog. See
              "Efficient movement." thread in Google Groups. Assuming the same for Linux.

            Repainting (BufferedImageSurface) has a problem on:
            * Mac OS X + Firefox (using the "apple.awt.MyCPanel" peer) - repaint events are lost
              when moving the mouse over the applet.
            * Mac OS X (all) - cannot reach 60fps (55fps max).

            My guess is BufferStrategy is only useful in PulpCore's situation when flipping is
            used, which seems to never be the case.
        */
        if ("true".equals(useBufferStrategyParam)) {
            useBufferStrategy = true;
        }
        else if ("false".equals(useBufferStrategyParam)) {
            useBufferStrategy = false;
        }
        else if (isMac && getRuntimeInfo().isAtLeastVersion("1.5")) {
            if (getOSInfo().isAtLeastVersion("10.6")) {
                useBufferStrategy = true;
            }
            else if (getOSInfo().isAtLeastVersion("10.5")) {
                // For Mac OS X 10.5:
                // Only use BufferStrategy on Firefox (the "apple.awt.MyCPanel" peer)
                try {
                    @SuppressWarnings("deprecation")
                    Object peer = applet.getPeer();
                    if (peer != null && "apple.awt.MyCPanel".equals(peer.getClass().getName())) {
                        useBufferStrategy = true;
                    }
                    else {
                        useBufferStrategy = false;
                    }
                }
                catch (Exception ex) {
                    useBufferStrategy = false;
                }
            }
            else {
                // Before Mac OS X 10.5, BufferStrategy was perfect.
                useBufferStrategy = true;
            }
        }
        else {
            useBufferStrategy = false;
        }

        if (surface == null && useBufferStrategy) {
            try {
                Class.forName("java.awt.image.BufferStrategy");
                surface = new BufferStrategySurface(applet);
                inputComponent = ((BufferStrategySurface)surface).getCanvas();
            }
            catch (Exception ex) {
                // ignore
            }
        }

        // Try to use BufferedImage. It's faster than using ImageProducer, and,
        // on some VMs, the ImageProducerSurface creates a lot of
        // garbage for the GC to cleanup.
        if (surface == null) {
            try {
                surface = new BufferedImageSurface(applet, this);
            }
            catch (Exception ex) {
                // ignore
            }
        }

        input = new AppletInput(this, inputComponent);
    }

    @Override
    public String[] getSupportedSoundFormats() {
        return new String[] { "wav", "ogg" };
    }
    
    private AppletAudio getBrokenSound() {
        if (brokenAudio == null) {
            brokenAudio = new RawAudio("null", new byte[0], 44100, false);
        }
        return brokenAudio;
    }
    
    public AppletAudioEngine getAudioEngine() {
        if (audioEngine == null) {
            audioEngine = AppletAudioEngine.create(this);
        }
        return audioEngine;
    }
    
    @Override
    public Audio loadAudio(String soundName) {
        // Init sound engine
        getAudioEngine();
        
        // Use OGG instead of MP3.
        if (soundName != null && soundName.toLowerCase().endsWith(".mp3")) {
            soundName = soundName.substring(0, soundName.length() - 4) + ".ogg";
        }
        
        // First try the cache
        WeakReference<? extends Audio> soundRef = soundCache.get(soundName);
        if (soundRef != null) {
            Audio sound = soundRef.get();
            if (sound != null) {
                return sound;
            }
            else {
                soundCache.remove(soundName);
            }
        }

        // Load the sound
        AppletAudio sound = null;
        if (soundName != null) {
            InputStream in = null;
            try {
                in = getResourceAsStream(soundName);
                if (in == null) {
                    Log.w("Couldn't load sound file: " + soundName + ". No such file.");
                }
                else {
                    if (soundName.toLowerCase().endsWith(".wav")) {
                        WAVFile wavFile = WAVFile.load(in, false, ByteOrder.LITTLE_ENDIAN);
                        sound = new RawAudio(soundName, wavFile.getData().array(), 
                            wavFile.getSampleRate(), wavFile.getNumChannels() == 2);
                    }
                    else if (soundName.toLowerCase().endsWith(".ogg")) {
                        sound = VorbisAudio.load(soundName, in);
                    }
                    else {
                        Log.w("Unknown sound file type: " + soundName);
                    }
                    in.close();
                }
            }
            catch (IOException ex) {
                if (sound == null) {
                    Log.w("Error loading load sound file: " + soundName + ". " + ex.getMessage());
                }
                if (in != null) {
                    try {
                        in.close();
                    }
                    catch (IOException ex2) { }
                }
            }
        }

        if (sound == null) {
            sound = getBrokenSound();
        }

        soundCache.put(soundName, new WeakReference<AppletAudio>(sound));
        return sound;
    }
}

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
import java.awt.Container;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelGrabber;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.platform.AppContext;
import pulpcore.platform.applet.opt.BufferedImageSurface;
import pulpcore.platform.applet.opt.BufferStrategySurface;
import pulpcore.platform.Platform;
import pulpcore.platform.Surface;
import pulpcore.scene.Scene;
import pulpcore.Stage;
import pulpcore.util.Base64;
import pulpcore.util.ByteArray;

public final class AppletAppContext extends AppContext {
    
    private CoreApplet applet;
    private Surface surface;
    private Input inputSystem;
    private Stage stage;
    private URL talkbackURL;
    private Object jsObject;
    private boolean firstFrameDrawn = false;
    
    
    public AppletAppContext(CoreApplet app, SystemTimer timer) {
        this.applet = app;
        
        // Create the JSObject for JavaScript functionality
        try {
            // JSObject jsObject = netscape.javascript.JSObject.getWindow(this);
            Class c = Class.forName("netscape.javascript.JSObject");
            Method getWindow = c.getMethod("getWindow", 
                new Class[] { Class.forName("java.applet.Applet") } );
            jsObject = getWindow.invoke(null, new Object[] { app });
        }
        catch (Throwable t) {
            // Ignore
        }
        
        boolean talkbackEnabled = "true".equals(app.getParameter("talkback"));
        
        String talkbackPath = app.getParameter("talkback-path");
        talkbackURL = null;
        
        if (talkbackEnabled && talkbackPath != null) {
            try {
                talkbackURL = new URL(app.getCodeBase(), talkbackPath);
            }
            catch (MalformedURLException ex) {
                if (Build.DEBUG) print("Bad url", ex);
                talkbackURL = null;
            }
        }
        
        setTalkBackField("pulpcore.platform", "Applet");
        setTalkBackField("pulpcore.platform.timer", timer.getName());
        setTalkBackField("pulpcore.platform.javascript", "" + (jsObject != null));  
        
        createSurface(app);
        stage = new Stage(surface, this);
    }
    
    
    CoreApplet getApplet() {
        return applet;
    }
    
    
    public Scene createFirstScene() {
        return applet.createFirstScene();
    }
    
    
    public void start() {
        if (stage != null) {
            stage.start();
        }
        
        if (Build.DEBUG) printMemory("App: start");
    }
    
    
    public void stop() {
        if (stage != null) {
            stage.stop();
        }
        if (Build.DEBUG) printMemory("App: stop");
    }
    
    
    public void destroy() {
        if (stage != null) {
            stage.destroy();
            stage = null;
        }
        surface = null;
        jsObject = null;
        inputSystem = null;
        setMute(true);
        if (Build.DEBUG) printMemory("App: destroy");
    }
    
    
    /**
        Returns true if calling JavaScript via LiveConnect is enabled.
    */
    public boolean isJavaScriptEnabled() {
        if (jsObject == null) {
            return false;
        }
        String name = "foo" + CoreMath.rand(0, 9999);
        String value = "bar" + CoreMath.rand(0, 9999);
        callJavaScript("pulpcore_setCookie", new Object[] { name, value });
        boolean enabled = value.equals(callJavaScript("pulpcore_getCookie", name));
        callJavaScript("pulpcore_deleteCookie", name);
        return enabled;
    }


    /**
        Calls a JavaScript method with no arguments.
    */
    public Object callJavaScript(String method) {
        return callJavaScript(method, (Object[])null);
    }
    
    
    /**
        Calls a JavaScript method with one argument.
    */
    public Object callJavaScript(String method, Object arg) {
        return callJavaScript(method, new Object[] { arg });
    }
    
    
    /**
        Calls a JavaScript method with a list of arguments.
    */
    public Object callJavaScript(String method, Object[] args) {
        
        if (jsObject == null) {
            return null;
        }
        
        try {
            Class c = Class.forName("netscape.javascript.JSObject");
            Method call = c.getMethod("call", 
                new Class[] { method.getClass(), new Object[0].getClass() });
            return call.invoke(jsObject, new Object[] { method, args });
        }
        catch (Throwable t) {
            if (Build.DEBUG) print("Couldn't call JavaScript method " + method, t);
        }   
        return null;
    }
    
    
    public void notifyFrameComplete() {
        if (!firstFrameDrawn) {
            firstFrameDrawn = true;
            callJavaScript("pulpcore_appletLoaded");
        }
    }
    
    
    private void createSurface(CoreApplet app) {
        
        Component inputComponent = app;
        surface = null;
        
        // Try to use BufferStrategy on Mac OS X.
        // On the Mac OS X implmentation of Java, repainting is only allowed once very 17ms 
        // (or 58.8 fps). 
        // If an app tries to flush sooner than 17 ms from the last flush, the frame
        // is ignored. So apps running at 60fps sometimes show only half the frames (30 fps).
        // A workaround is to use BufferStategy. Note, this still occationally has dropped frames.
        if (surface == null && CoreSystem.isMacOSX() && CoreSystem.isJava15orNewer()) {
            try {
                Class.forName("java.awt.image.BufferStrategy");
                surface = new BufferStrategySurface(app);
                inputComponent = ((BufferStrategySurface)surface).getCanvas();
                setTalkBackField("pulpcore.platform.surface", "BufferStrategy");
            }
            catch (Exception ex) {
                // ignore
            }
        }
        
        // Try to use BufferedImage. It's faster than using ImageProducer, and,
        // on some VMs, the ImageProducerSurface creates a lot of 
        // garbage for the GC to cleanup.
        if (surface == null && CoreSystem.isJava13orNewer()) {
            try {
                Class.forName("java.awt.image.BufferedImage");
                surface = new BufferedImageSurface(app);
                setTalkBackField("pulpcore.platform.surface", "BufferedImage");
            }
            catch (Exception ex) {
                // ignore
            }
        }
        
        // BufferedImage is not available, so use the ImageProducer surface
        if (surface == null) {
            surface = new ImageProducerSurface(app);
            setTalkBackField("pulpcore.platform.surface", "ImageProducer");
        }
        
        inputSystem = new AppletInput(inputComponent);
    }
    
    
    public Input getInputSystem() {
        return inputSystem;
    }
    
    
    public Stage getStage() {
        return stage;
    }
    
    
    public Surface getSurface() {
        return surface;
    }
    
    
    public URL getBaseURL() {
        return applet.getCodeBase();
        ///*
        //    Use the document base so we can check if applets are being hijacked in IE.
        //    
        //    For example, someone has a site: http://www.mygames.net/milpa/
        //    
        //    On this page they put the code: 
        //    <base href="http://www.pulpgames.net/milpa/" />
        //    
        //    In IE, this makes the doc base http://www.mygames.net/milpa/
        //    but the code base is http://www.pulpgames.net/milpa/
        //    
        //    In Firefox and Safari, but the doc base and the code base are
        //    http://www.pulpgames.net/milpa/
        //*/
        //if (CoreSystem.isJava15orNewer()) {
        //    return applet.getDocumentBase();
        //}
        //else {
        //    // Java 1.4 and older reports the wrong doc base for framed sites.
        //    // Just assume the site is legit and use the code base.
        //    return applet.getCodeBase();
        //}
    }
    
    
    public URL getTalkbackURL() {
        return talkbackURL;
    }
    
    
    public void showDocument(String url, String target) {
        URL parsedURL;
        
        try {
            parsedURL = new URL(url);
        }
        catch (MalformedURLException ex) {
            if (Build.DEBUG) print("Invalid URL: " + url);
            return;
        }
        
        applet.getAppletContext().showDocument(parsedURL, target);
    }
    
    
    public String getLocaleLanguage() {
        try {
            return applet.getLocale().getLanguage();
        }
        catch (Throwable t) {
            return "";
        }
        
    }
    
    
    public String getLocaleCountry() {
        try {
            return applet.getLocale().getCountry();
        }
        catch (Throwable t) {
            return "";
        }
    }
    
    
    public void putUserData(String key, byte[] data) {
        String name = "pulpcore_" + key;
        String value = Base64.encodeURLSafe(data);
        
        callJavaScript("pulpcore_setCookie", new Object[] { name, value });
    }
        
        
    public byte[] getUserData(String key) {
        String name = "pulpcore_" + key;
        
        Object result = callJavaScript("pulpcore_getCookie", name);
        if (result == null) {
            return null;
        }
        else {
            String value = result.toString();
            
            // Reset the expiration date to another 90 days
            callJavaScript("pulpcore_setCookie", new Object[] { name, value });
            
            return Base64.decodeURLSafe(value);
        }
    }
    
    
    public void removeUserData(String key) {
        String name = "pulpcore_" + key;
        
        callJavaScript("pulpcore_deleteCookie", name);
    }
    
      
    public CoreImage loadImage(ByteArray in) {
        
        if (in == null) {
            return null;
        }
        
        Image image = Toolkit.getDefaultToolkit().createImage(in.getData());
        
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
            boolean isOpaque = true;
            ColorModel model = pixelGrabber.getColorModel();
            if (model instanceof DirectColorModel) {
                isOpaque = ((DirectColorModel)model).getAlphaMask() == 0;
            }
            return new CoreImage(width, height, isOpaque, data);
        }
        else {
            return null;
        }
    }
}

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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.net.MalformedURLException;
import java.net.URL;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.Surface;
import pulpcore.scene.LoadingScene;
import pulpcore.scene.Scene;

/**
    CoreApplet is a Java 1.1 compatible Platform implementation for web
    games.
*/
// CoreApplet is final so that developers don't override CoreApplet constructor, and call Stage or 
// some other class that isn't ready until after init()
public final class CoreApplet extends Applet {
    
    private AppletAppContext context;
    private boolean oldJava = false;

    
    public final void init() {
        
        String bgColor = getParameter("boxbgcolor");
        if (bgColor != null) {
            try {
                setBackground(Color.decode(bgColor));
            }
            catch (NumberFormatException ex) { }
        }
        
        String javaVersion = "1.1";
        try {
            javaVersion = System.getProperty("java.version");
        }
        catch (SecurityException ex) { }
        
        if (javaVersion.compareTo("1.4") < 0) {
            oldJava = true;
            
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            final Applet applet = this;
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    try {
                        applet.getAppletContext().showDocument(
                            new URL("http://www.java.com/"), "_blank");
                    }
                    catch (MalformedURLException ex) { }
                }
            });
        }
        else {
            oldJava = false;
            if (context != null) {
                context.stop();
            }
            context = (AppletAppContext)AppletPlatform.getInstance().registerApp(this);
        }
    }
    
    
    public final void start() {
        if (context != null) {
            context.start();
        }
    }
    
    
    public final void stop() {
        if (context != null) {
            context.stop();
        }
    }
    
    
    public final void destroy() {
        if (context != null) {
            context.stop();
            AppletPlatform.getInstance().unregisterApp(this);
            context = null;
        }
    }
    
    
    public final void update(Graphics g) {
        paint(g);
    }
    
    
    public final void paint(Graphics g) {
        if (oldJava) {
            drawUpgradeMessage(g);
        }
        else if (context == null) {
            drawBlank(g);
        }
        else {
            drawSurface(g);
        }
    }
    
    
    private final void drawUpgradeMessage(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(Color.blue);
        g.drawString("Get Java at www.java.com", 5, 25);
    }
    
    
    private final void drawSurface(Graphics g) {
        Surface surface = context.getSurface();
        
        if (surface instanceof BufferedImageSurface) {
            ((BufferedImageSurface)surface).draw(g);
        }
        else {
            surface.notifyOSRepaint();
        }
    }
    
    
    private final void drawBlank(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getSize().width, getSize().height);
    }
    
    
    /**
        Creates a Scene object from the named "scene" applet parameter.
    */
    public Scene createFirstScene() {
        
        Scene firstScene;
        
        // Create the first scene
        String sceneName = getParameter("scene");
        if (sceneName == null || sceneName.length() == 0) {
            if (Build.DEBUG) CoreSystem.print("No defined scene.");
            return null;
        }
        try {
            Class c = Class.forName(sceneName);
            firstScene = (Scene)c.newInstance();
        }
        catch (Throwable t) {
            if (Build.DEBUG) CoreSystem.print("Could not create Scene: " + sceneName, t);
            return null;
        }
        
        // Auto-load assets
        String assetsName = getParameter("assets");
        if (assetsName == null || assetsName.length() == 0) {
            return firstScene;
        }
        else {
            return new LoadingScene(assetsName, firstScene);
        }
    }
    
    
    /**
        Gets the current active scene. This method is provided for calls from JavaScript.
        <code>pulpcore_object.getCurrentScene().callMyMethod()</code>
    */
    public Scene getCurrentScene() {
        if (context != null) {
            return context.getStage().getScene();
        }
        else {
            return null;
        }
    }
    
}

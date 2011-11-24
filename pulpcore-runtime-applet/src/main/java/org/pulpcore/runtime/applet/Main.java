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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import org.pulpcore.runtime.Surface;
import org.pulpcore.runtime.jre.JreRunLoop;
import org.pulpcore.view.Scene;
import org.pulpcore.view.Stage;

@SuppressWarnings("serial")
public class Main extends Applet {

    private AppletContext context;
    private Stage stage;

    private Color getColorParameter(String param) {
        String color = getParameter(param);
        if (color != null) {
            try {
                return Color.decode(color);
            }
            catch (NumberFormatException ex) { }
        }
        return null;
    }

    private int getIntParameter(String param, int defaultValue) {
        String value = getParameter(param);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException ex) { }
        }
        return defaultValue;
    }

    @Override
    public void init() {
        destroy();

        // Private API - should use Reflection.
        // Saw no difference on Mac OS X.
        //com.sun.java.swing.SwingUtilities3.setVsyncRequested(this, true);

        Color bgColor = getColorParameter("boxbgcolor");
        if (bgColor != null) {
            setBackground(bgColor);
        }
        Color fgColor = getColorParameter("boxfgcolor");
        if (fgColor != null) {
            setForeground(fgColor);
        }

        context = new AppletContext(this);
        stage = new Stage(context, new JreRunLoop(context), context.getSurface());
        stage.setSize(getIntParameter("pulpcore_width", getWidth()),
                getIntParameter("pulpcore_height", getHeight()));
    }

    @Override
    public void start() {
        stage.start();
    }

    @Override
    public void stop() {
        stage.stop();
    }

    @Override
    public void destroy() {
        if (stage != null) {
            stage.stop();
            stage = null;
        }
        if (context != null) {
            context.destroy();
            context = null;
        }
    }

    @Override
    public void update(Graphics g) {
        // App-triggered painting
        if (context == null) {
            // Do nothing
        }
        else {
            Surface surface = context.getSurface();
            if (surface instanceof BufferedImageSurface) {
                ((BufferedImageSurface)surface).draw(g, false);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        // System-triggered painting
        if (context == null) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        else {
            Surface surface = context.getSurface();
            surface.onOSRepaint();
            if (surface instanceof BufferedImageSurface) {
                ((BufferedImageSurface)surface).draw(g, true);
            }
        }
    }

    public Scene createFirstScene() {

        Scene firstScene;

        // Create the first scene
        String sceneName = getParameter("pulpcore_scene");
        if (sceneName == null || sceneName.length() == 0) {
            Log.e("No defined scene.");
            return null;
        }
        try {
            Class<?> c = Class.forName(sceneName);
            firstScene = (Scene)c.newInstance();
        }
        catch (Exception ex) {
            Log.e("Could not create Scene: " + sceneName, ex);
            return null;
        }

        return firstScene;

//        // Auto-load assets
//        String assetsName = getParameter("assets");
//        if (assetsName == null || assetsName.length() == 0) {
//            return firstScene;
//        }
//        else {
//            return new LoadingScene(assetsName, firstScene);
//        }
    }
}

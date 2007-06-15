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

package pulpcore.tools;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.EventQueue;
import java.awt.Image;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
    PulpCorePlayer is an applet viewer for PulpCore applets. 
    Significant features include:
    <ul>
    <li>Ability to run in the save VM as an IDE or Ant runner.
    <li>Ability to restart the current scene 
    </ul>
*/
public class PulpCorePlayer extends JFrame implements AppletStub, AppletContext {
    
    private static HashMap<String, PulpCorePlayer> players = new HashMap<String, PulpCorePlayer>();
    
    public static void main(String[] args) {
        if (args.length != 6 && args.length != 7) {
            System.out.println("PulpCorePlayer is an applet viewer for PulpCore applets.");
            System.out.println("Arguments: documentURL archive scene width height assets");
            System.out.println();
            System.out.println("Note, PulpCorePlayer does not install a SecurityManager.");
            System.out.println("Always be sure to test applets in web browsers.");
            return;
        }
        
        String documentURL = args[0];
        String archive = args[1];
        String scene = args[2];
        int width = Integer.parseInt(args[3]);
        int height = Integer.parseInt(args[4]);
        String assets = args[5];
        boolean waitUntilClosed = true;
        
        if (args.length > 6 && "false".equalsIgnoreCase(args[6])) {
            waitUntilClosed = false;
        }
        
        
        final String key = documentURL + archive;
        
        if (waitUntilClosed && !EventQueue.isDispatchThread()) {
            try {
                synchronized (key) {
                    PulpCorePlayer player = new PulpCorePlayer(key, documentURL, archive, scene, 
                        width, height, assets);
                    // Wait until closed
                    try {
                        key.wait();
                    }
                    catch (InterruptedException ex) { }
                }       
            }
            catch (MalformedURLException ex) {
                System.out.println("Path problem: " + ex);
            }
            catch (IOException ex) {
                System.out.println(ex);
            }
                 
        }
        else {
            synchronized (players) {
                PulpCorePlayer player = players.get(key);
                if (player != null) {
                    player.reloadApplet();
                }
                else {
                    try {
                        player = new PulpCorePlayer(key, documentURL, archive, scene, width, height,
                            assets);
                        players.put(key, player);
                    }
                    catch (MalformedURLException ex) {
                        System.out.println("Path problem: " + ex);
                    }
                    catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    }
    
    private final URL documentBaseURL;
    private final URL codeBaseURL;
    private final URL archiveURL;
    private final String key;
    private final String scene;
    private final String assets;
    private final int width;
    private final int height;
    
    private Class appletClass;
    private Applet applet;
    private boolean active;
    
    private List<String> savedAssets;
    private String savedScene;
    
    
    public PulpCorePlayer(String key, String documentURL, String archive, String scene, 
        int width, int height, String assets) 
        throws MalformedURLException, IOException
    { 
        // Setup the JFrame 
        super("PulpCore Player");
        setLocationByPlatform(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0,0));
        
        // Setup applet parameters
        this.key = key;
        this.scene = scene;
        this.width = width;
        this.height = height;
        this.assets = assets;
        
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                synchronized (players) {
                    players.remove(PulpCorePlayer.this.key);
                    unloadApplet();
                }
                
                synchronized (PulpCorePlayer.this.key) {
                    PulpCorePlayer.this.key.notify();
                }
            }
        });
        

        // Create the URLs
        if (documentURL.startsWith("file:/")) {
            documentURL = documentURL.substring(6);
            // Ensure only one slash at the beggining
            while (documentURL.startsWith("//")) {
                documentURL = documentURL.substring(1);
            }
        }
        
        if (documentURL.startsWith("http:")) {
            // Determine URL redirects. For example, 
            // "http://www.example.com/path" might redirect to 
            // "http://www.example.com/path/".
            URL url = new URL(documentURL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            if (conn.getResponseCode() == 301) {
                url = new URL(conn.getHeaderField("Location"));
            }
            documentBaseURL = url;
            codeBaseURL = new URL(documentBaseURL, ".");
        }
        else {
            // Assume a local path
            File file = new File(documentURL);
            if (file.isDirectory()) {
                documentBaseURL = new URL("file://" + file.toString() + "/");
                codeBaseURL = documentBaseURL;
            }
            else {
                documentBaseURL = new URL("file://" + file.toString());
                codeBaseURL = new URL("file://" + file.getParent() + "/");
            }
        }
        archiveURL = new URL(codeBaseURL, archive);

        // Show the window
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            getContentPane().setPreferredSize(new Dimension(width, height + 15));
        }
        else {
            getContentPane().setPreferredSize(new Dimension(width, height));
        }        
        pack();
        setVisible(true);
        
        // Load the applet
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    loadApplet();
                }
                catch (Throwable t) {
                    showError(t);
                }
            }
        });
        
    }
    
    
    private synchronized void loadApplet() {
        
        unloadApplet();
        
        // Create the Applet
        ClassLoader classLoader = new NoResourceCacheClassLoader(new URL[] { archiveURL, codeBaseURL },
            //getClass().getClassLoader());
            ClassLoader.getSystemClassLoader());
        try {
            applet = (Applet)classLoader.loadClass("pulpcore.platform.applet.CoreApplet").newInstance();
        }
        catch (Exception ex) {
            showError(ex);
            return;
        }
        // Hack: This anonymous class must be pre-loaded. If it is not loaded, and
        // the developer rebuilds the jar, applet.destroy() will fail.  
        try {
            classLoader.loadClass("pulpcore.Stage$1");
        }
        catch (Exception ex) {
            // Ignore
        }
        
        applet.setStub(this);
        applet.setSize(width, height);
        // Not sure why this doesn't seem to work
        applet.setLocale(Locale.getDefault());
        getContentPane().add(applet, BorderLayout.CENTER);
        // Add 15 pixels for the growbox
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            Component c = Box.createVerticalStrut(15);
            c.setBackground(Color.BLACK);
            c.setForeground(Color.BLACK);
            getContentPane().add(c, BorderLayout.SOUTH);
        }
        applet.init();
        
        // Do later
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                active = true;
                applet.start();
            }
        });
    }
    
    
    private synchronized void unloadApplet() {
        getContentPane().removeAll();
        active = false;
        if (applet != null) {
            applet.stop();
            applet.destroy();
            applet = null;
        }
    }
    
    
    private synchronized void reloadApplet() {
        saveState();
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    unloadApplet();
                    toFront();
                    loadApplet();
                }
                catch (Throwable ex) {
                    showError(ex);
                }
            }
        });
    }
    
    
    private void showError(Throwable t) {
        t.printStackTrace();
        JPanel panel = new JPanel();
        panel.add(new JLabel(t.toString()));
        getContentPane().removeAll();
        getContentPane().add(panel, BorderLayout.CENTER);
        validate();
        panel.repaint();
    }
    
    
    private void saveState() {

        clearState();
        
        if (applet != null) {
            
            ClassLoader classLoader = applet.getClass().getClassLoader();
            
            try {
                // Call Stage.getScene()
                Method getScene = classLoader.loadClass("pulpcore.Stage").getMethod("getScene");
                Object sceneObject = getScene.invoke(null, new Object[0]);
                if (sceneObject == null) {
                    return;
                }
                Class sceneClass = sceneObject.getClass();
                if (!isValidScene(sceneClass)) {
                    return;
                }
                String scene = sceneClass.getName();
                
                // Call Assets.getCatalogs()
                Method getCatalogs = classLoader.loadClass("pulpcore.Assets").getMethod("getCatalogs");
                Enumeration<String> e = (Enumeration<String>)getCatalogs.invoke(null, new Object[0]);
                List<String> assets = new ArrayList<String>();
                while (e.hasMoreElements()) {
                    assets.add(e.nextElement());
                }
                
                // Store the data
                savedAssets = assets;
                savedScene = scene;
                return;
            }
            catch (Throwable t) {
                // Ignore
                t.printStackTrace();
            }
        }
    }
    
    
    private boolean isValidScene(Class c) {
        String name = c.getName();
        if (name.startsWith("pulpcore.")) {
            return false;
        }
        
        // Check if the class is public and not abstract
        int classModifiers = c.getModifiers();
        if ((classModifiers & Modifier.PUBLIC) == 0 ||
            (classModifiers & Modifier.ABSTRACT) != 0)
        {
            return false;
        }
        
        // Check if it has a public, no-argument constructor
        try {
            Constructor constructor = c.getDeclaredConstructor(new Class[] { });
            if ((constructor.getModifiers() & Modifier.PUBLIC) == 0) {
                return false;
            }
        }
        catch (Exception ex) {
            return false;
        }
        
        // All tests passed
        return true;
    }
    
    
    private void restoreState() {
        if (applet != null) {
            
            ClassLoader classLoader = applet.getClass().getClassLoader();
            Method addCatalog;
            try {
                Class assetsClass = classLoader.loadClass("pulpcore.Assets");
                addCatalog = assetsClass.getMethod("addCatalog", 
                    new Class[] { String.class, InputStream.class });
            }
            catch (Throwable t) {
                // Ignore
                t.printStackTrace();
                return;
            }    
            
            for (String path : savedAssets) {
                try {
                    InputStream is = new URL(applet.getCodeBase(), path).openStream();
                    addCatalog.invoke(null, new Object[] { path, is } );
                }
                catch (Throwable t) {
                    // Ignore
                    t.printStackTrace();
                }
            }
        }
    }
    
    
    private void clearState() {
        savedAssets = null;
        savedScene = null;
    }
    
    
    //
    // AppletStub methods
    //
    
    
    public void appletResize(int width, int height) {
        // Do nothing
    }
    
    
    public AppletContext getAppletContext() {
        return this;
    }
    
    
    public URL getCodeBase() {
        return codeBaseURL;
    }
    

    public URL getDocumentBase() {
        return documentBaseURL;
    }
    
    
    public String getParameter(String name) {
        if ("scene".equals(name)) {
            if (savedScene != null && savedAssets != null) {
                restoreState();
                return savedScene;
            }
            else {
                return scene;
            }
        }
        else if ("assets".equals(name)) {
            if (savedScene != null && savedAssets != null) {
                return null;
            }
            else {
                return assets;
            }
        }
        else {
            return null;
        }
    }
    
    public boolean isActive() {
        return active;
    }

    
    //
    // AppletContext methods
    //
    

    public Applet getApplet(String name) {
        return null;
    }
    

    public Enumeration getApplets() {
        return null;        
    }
    
    
    public AudioClip getAudioClip(URL url) {
        return null;
    }
    
    
    public Image getImage(URL url) {
        return null;
    }
    
    
    public void showDocument(URL url) {
        // Do nothing
    }

    
    public void showDocument(URL url, String target) {
        // Do nothing
    }
    

    public void showStatus(String status) {
        // Do nothing
    }
    
    
    public void setStream(String s, InputStream in) {
        // Do nothing
    }
    
    
    public InputStream getStream(String s) {
        // Do nothing
        return null;
    }
    

    public Iterator getStreamKeys() {
        // Do nothing
        return null;
    }
    
    
    public class NoResourceCacheClassLoader extends URLClassLoader {
        
        public NoResourceCacheClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        // I'm not sure why this is necesary, but it works. I was getting ZipExceptions loading 
        // resources from the jar after it was modified. Classes always loaded fine.
        public InputStream getResourceAsStream(String name) {
            URL url = super.getResource(name);
            if (url != null) {
                try {
                    URLConnection c = url.openConnection();
                    if (url.toString().startsWith("jar:file:")) {
                        // Don't use caches - it's a local jar file that could have been modified 
                        // recently in the edit->build->test cycle.
                        c.setUseCaches(false);
                    }
                    return c.getInputStream();
                }
                catch (IOException ex) {
                    // Ignore
                }
            }
            return null;
        }
    }

}

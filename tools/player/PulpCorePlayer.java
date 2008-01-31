/*
    Copyright (c) 2008, Interactive Pulp, LLC
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

package pulpcore.player;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.EventQueue;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import pulpcore.Build;

/**
    PulpCore Player is an applet viewer for PulpCore applets. 
    Significant features not available in appletviewer include:
    <ul>
    <li>Ability to run in the save VM as an IDE or Ant runner.
    <li>Ability to reload the app, at the same scene
    <li>User Data - CoreSystem.getUserData() and CoreSystem.putUserData()
    </ul>
*/
public class PulpCorePlayer extends JFrame implements AppletStub, AppletContext {
    
    private static final int MAC_GROWBOX_SIZE = 15;
    
    private static HashMap<String, PulpCorePlayer> players = new HashMap<String, PulpCorePlayer>();
    
    /*
        Note: running from the command-line has only been tested with the html generated
        by Eclipse. NetBeans can use the Ant file's "run" task.
        
        <applet archive="archive.jar" width="640" height="480">
        <param name="name1" value="value1">
        <param name="name2" value="value2">
        <param name="name3" value="value3">
        </applet>
    */
    public static void main(String[] args) {
        if (args.length != 1) {
            showUsage();
            return;
        }
        
        String html;
        String documentURL;
        
        if (args[0].startsWith("http://") || 
            args[0].startsWith("https://") ||
            args[0].startsWith("ftp://"))
        {
            // Read text (file on the Internet)
            documentURL = args[0];
            try {
                URL url = new URL(args[0]);
                html = readTextFile(new InputStreamReader(url.openStream()));
            }
            catch (IOException ex) {
                System.out.println("Couldn't read remote file: " + args[0]);
                System.out.println(ex.getMessage());
                return;
            }
        }
        else {
            // Read text (local file)
            File file = new File(args[0]);
            documentURL = file.getAbsolutePath();
            try {
                html = readTextFile(new FileReader(file));
            }
            catch (IOException ex) {
                System.out.println("Couldn't read local file: " + args[0]);
                System.out.println(ex.getMessage());
                return;
            }
        }
        
        // Default values
        String archive = null;
        int width = 640;
        int height = 480;
        Map<String, String> params = new HashMap<String, String>();
        
        // Prepare applet tag regex
        String htmlSpace = "\\s+";
        String inTagChars = "[^>]*";
        String stringValue = "\"([^\\\"]*)\"";
        String integerValue = "\"([0-9]+)\"";
        String appletStart ="<applet" + htmlSpace;
        String paramStart ="<param" + htmlSpace;
        
        String archiveRegex = appletStart + inTagChars + "archive=" + stringValue;
        String dimRegex = appletStart + inTagChars + "width=" + integerValue +
            htmlSpace + "height=" + integerValue;
        String paramRegex = paramStart +
            "name=" + stringValue + htmlSpace + "value=" + stringValue;
        
        // Parse archive attribute
        Pattern pattern = Pattern.compile(archiveRegex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            archive = matcher.group(1);
        }
        
        // Parse width & height attributes
        pattern = Pattern.compile(dimRegex, Pattern.DOTALL);
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            width = Integer.parseInt(matcher.group(1));
            height = Integer.parseInt(matcher.group(2));
        }
        
        // Parse extra params
        pattern = Pattern.compile(paramRegex, Pattern.DOTALL);
        matcher = pattern.matcher(html);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        
        if (params.get("scene") == null) {
            System.out.println("Couldn't find 'scene' parameter in file: " + args[0]);
            return;
        }
        
        // Use system Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {
            // ignore
        }
        
        start(documentURL, archive, width, height, params, false);
    }
    
    private static void showUsage() {
        System.out.println("PulpCore Player is an applet viewer for PulpCore applets.");
        System.out.println();
        System.out.println("Usage: java -jar pulpcore-player-" + Build.VERSION + ".jar file");
        System.out.println("Where 'file' is a path or URL pointing to an HTML file containing an");
        System.out.println("applet tag. The applet tag must contain a 'scene' parameter.");
        System.out.println();
        System.out.println("Note, PulpCore Player does not install a SecurityManager.");
        System.out.println("Always be sure to test applets in web browsers.");
    }
    
    private static String readTextFile(Reader in) throws IOException {
        String text = "";
        
        BufferedReader reader = new BufferedReader(in);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return text;
            }
            text += line + '\n';
        }
    }
    
    public static void start(String documentURL, String archive, int width, int height,
        Map<String, String> params, boolean waitUntilClosed)
    {
        final String key = documentURL + archive;
        
        if (waitUntilClosed && !EventQueue.isDispatchThread()) {
            try {
                synchronized (key) {
                    PulpCorePlayer player = new PulpCorePlayer(key, documentURL, archive, 
                        width, height, params);
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
                        player = new PulpCorePlayer(key, documentURL, archive, width, height,
                            params);
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
    private final int width;
    private final int height;
    private final Map<String, String> params;
    
    private Class appletClass;
    private Applet applet;
    private boolean active;
    
    private List<String> savedAssets;
    private String savedScene;
    
    public PulpCorePlayer(String key, String documentURL, String archive, 
        int width, int height, Map<String, String> params) 
        throws MalformedURLException, IOException
    { 
        // Setup the JFrame 
        super("PulpCore Player");
        setLocationByPlatform(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0,0));
        
        // Setup menu bar
        // Use setLightWeightPopupEnabled(false) because we're mixing lightweight and 
        // heavyweight components
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(new FileAction());
        fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        fileMenu.add(new JMenuItem(new ReloadAction()));
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        // Setup applet parameters
        this.key = key;
        this.width = width;
        this.height = height;
        this.params = params;
        params.put("browsername", "PulpCore Player");
        params.put("browserversion", Build.VERSION);
        
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
                documentBaseURL = file.toURI().toURL();
                codeBaseURL = documentBaseURL;
            }
            else {
                documentBaseURL = file.toURI().toURL();
                codeBaseURL = file.getParentFile().toURI().toURL();
            }
        }
        if (archive == null) {
            archiveURL = null;
        }
        else {
            archiveURL = new URL(codeBaseURL, archive);
        }

        // Show the window
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            getContentPane().setPreferredSize(new Dimension(width, height + MAC_GROWBOX_SIZE));
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
        
        URL[] urls;
        if (archiveURL == null) {
            urls = new URL[] { codeBaseURL };
        }
        else {
            urls = new URL[] { archiveURL, codeBaseURL };
        }
        
        // Create the Applet
        // Use this class loader as the parent so that the applet has access to JSObject
        ClassLoader classLoader = NoResourceCacheClassLoader.create(urls,
            getClass().getClassLoader());
        try {
            applet = (Applet)classLoader.loadClass("pulpcore.platform.applet.CoreApplet").newInstance();
        }
        catch (Exception ex) {
            showError(ex);
            return;
        }
        // Hack: This anonymous class must be pre-loaded. If it is not loaded, and
        // the developer rebuilds the jar, applet.destroy() will fail.  
        // NOTE: that inner class is no longer used, but this is left here in case similar
        // problems pop up.
        //try {
        //    classLoader.loadClass("pulpcore.Stage$1");
        //}
        //catch (Exception ex) {
        //    // Ignore
        //}
        
        applet.setStub(this);
        applet.setSize(width, height);
        // Not sure why this doesn't seem to work
        applet.setLocale(Locale.getDefault());
        getContentPane().add(applet, BorderLayout.CENTER);
        // Add 15 pixels for the growbox
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            Component c = Box.createVerticalStrut(MAC_GROWBOX_SIZE);
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
        active = false;
        if (applet != null) {
            applet.stop();
        }
        getContentPane().removeAll();
        if (applet != null) {
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
                Iterator i = (Iterator)getCatalogs.invoke(null, new Object[0]);
                List<String> assets = new ArrayList<String>();
                while (i.hasNext()) {
                    assets.add((String)i.next());
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
        if ("scene".equals(name) && savedScene != null && savedAssets != null) {
            restoreState();
            return savedScene;
        }
        else if ("assets".equals(name) && savedScene != null && savedAssets != null) {
            return null;
        }
        else {
            return params.get(name);
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
    
    public Enumeration<Applet> getApplets() {
        return null;        
    }
    
    public AudioClip getAudioClip(URL url) {
        return null;
    }
    
    public Image getImage(URL url) {
        return null;
    }
    
    public void showDocument(URL url) {
        showDocument(url, "_self");
    }

    public void showDocument(URL url, String target) {
        try {
            BrowserLaunch.open(url);
        }
        catch (UnsupportedOperationException ex) {
            ex.getCause().printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Could not launch browser.\n" + url);
        }
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

    public Iterator<String> getStreamKeys() {
        // Do nothing
        return null;
    }
    
    //
    // Swing actions
    //
    
    public class FileAction extends EditAction { }
    
    public class ReloadAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            reloadApplet();
        }
    }

    //
    // ClassLoader hack
    //
    
    public static class NoResourceCacheClassLoader extends URLClassLoader {
        
        private NoResourceCacheClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        /*
            This is mostly a hack for Eclipse, which supplies the applet codebase in the 
            system classpath rather than using the applet tag's codebase attribute.
            
            In order for reloading to work, the applet classes need to be loaded in a 
            throw-away class loader - not the system class loader.
            
            For this to work, the PulpCore player jar must be in the bootclasspath.
        */
        public static NoResourceCacheClassLoader create(URL[] urls, ClassLoader parent) {
            // If parent is null, the PulpCorePlayer is running from the bootstrap class
            // loader.
            if (parent == null) {
                parent = ClassLoader.getSystemClassLoader();
            }
            
            // If the parent is a URLClassLoader, use all of its URLs
            List<URL> urlList = new ArrayList<URL>();
            urlList.addAll(Arrays.asList(urls));
            
            while (parent instanceof URLClassLoader) { 
                urlList.addAll(Arrays.asList(((URLClassLoader)parent).getURLs()));
                parent = parent.getParent();
            }
            
            urls = urlList.toArray(new URL[0]);
            
            return new NoResourceCacheClassLoader(urls, parent);
        }
        
        // Don't cache resources inside jars - a ZipException will occur if 
        // the jar is modified.
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

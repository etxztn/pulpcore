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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.MenuElement;
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
        
        Example:
        
        <applet code="pulpcore.platform.applet.CoreApplet" 
            archive="Widgets.jar" width="640" height="480">
            <param name="scene" value="Widgets">
            <param name="assets" value="Widgets.zip">
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
        System.setProperty("apple.laf.useScreenMenuBar", "true");
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
        PulpCorePlayer player = null;
        final String key = documentURL + archive;
        
        // Only wait if this is not the EDT
        waitUntilClosed = waitUntilClosed && !EventQueue.isDispatchThread();

        final PipedOutputStream ps = new PipedOutputStream();
        PrintStream os = new PrintStream(ps);
        
        synchronized (players) {
            if (!waitUntilClosed) {
                player = players.get(key);
                if (player != null) {
                    player.reloadApplet();
                    return;
                }
            }
            
            try {
                player = new PulpCorePlayer(key, documentURL, archive, width, height, params,
                        waitUntilClosed ? os : null);
            }
            catch (MalformedURLException ex) {
                System.out.println("Path problem: " + ex);
                return;
            }
            catch (IOException ex) {
                System.out.println(ex);
                return;
            }
            
            if (!waitUntilClosed) {
                players.put(key, player);
                player.showFrame();
            }
        }
        
        if (waitUntilClosed) {
            // Redirect output from AWT thread to this thread (for IDEs)
            final PulpCorePlayer p = player;
            Thread t = new Thread() {
                public void run() {
                    try {
                        BufferedReader r = new BufferedReader(
                                new InputStreamReader(
                                new PipedInputStream(ps)));
                        while (p.running) {
                            String input = r.readLine();
                            if (input != null) {
                                System.out.println(input);
                            }
                        }
                    }
                    catch (java.io.InterruptedIOException ex) {
                        // Ignore
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            t.start();

            synchronized (key) {
                player.showFrame();
                try {
                    key.wait();
                }
                catch (InterruptedException ex) { }
                t.interrupt();
            }
        }
    }
    
    private final URL documentBaseURL;
    private final URL codeBaseURL;
    private final URL[] archiveURL;
    private final String key;
    private final int width;
    private final int height;
    private final Map<String, String> params;
    
    private Applet applet;
    private Scripting scripting;
    private boolean active;
    
    private List<String> savedAssets;
    private String savedScene;
    
    private Color osBackground;

    private PrintStream out;
    private boolean running;
    
    public PulpCorePlayer(String key, String documentURL, String archive, 
        int width, int height, Map<String, String> params, PrintStream out)
        throws MalformedURLException, IOException
    { 
        this.key = key;
        this.width = width;
        this.height = height;
        this.params = params;
        this.out = out;
        this.running = true;
        params.put("browsername", "PulpCore Player");
        params.put("browserversion", Build.VERSION);
        
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
            archiveURL = new URL[] { codeBaseURL };
            setTitle("PulpCore");
        }
        else {
            String[] archives = archive.split("[;:]");
            archiveURL = new URL[archives.length + 1];
            for (int i = 0; i < archives.length; i++) {
                archiveURL[i] = new URL(codeBaseURL, archives[i]);
            }
            archiveURL[archives.length] = codeBaseURL;
            setTitle(getProjectTitle(archives[0]));
        }
        
        // For Mac OS X
        osBackground = getBackground();
        osBackground = new Color(osBackground.getRGB());
        
        // Setup the JFrame
        // TODO: get colors defined in params?
        setBackground(Color.BLACK);
        getContentPane().setBackground(Color.BLACK);
        setForeground(new Color(170, 170, 170));
        getContentPane().setForeground(new Color(170, 170, 170));
        
        setLocationByPlatform(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(0,0));
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            getContentPane().setPreferredSize(new Dimension(width, height + MAC_GROWBOX_SIZE));
        }
        else {
            getContentPane().setPreferredSize(new Dimension(width, height));
        }
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().setFullScreenWindow(null);

                running = false;

                synchronized (players) {
                    players.remove(PulpCorePlayer.this.key);
                    unloadApplet();
                }
                
                synchronized (PulpCorePlayer.this.key) {
                    PulpCorePlayer.this.key.notify();
                }
            }
        });
        
        setJMenuBar(createJMenuBar());
    }
    
    private JMenuBar createJMenuBar() {
        JMenu fileMenu = new JMenu(new FileAction());
        fileMenu.add(new JMenuItem(new ShowSceneSelectorAction()));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new ReloadAction()));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new ScreenshotAction()));
        
        JMenu viewMenu = new JMenu(new ViewAction());
        viewMenu.add(new JMenuItem(new SceneInfoAction()));
        viewMenu.add(new JMenuItem(new DirtyRectanglesAction()));
        viewMenu.add(new JMenuItem(new ShowConsoleAction()));
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem(new SlowSpeedAction()));
        viewMenu.add(new JMenuItem(new NormalSpeedAction()));
        viewMenu.add(new JMenuItem(new FastSpeedAction()));
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem(new FullScreenAction()));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        
        // Use setLightWeightPopupEnabled(false) because we're mixing lightweight and 
        // heavyweight components
        for (MenuElement menu : menuBar.getSubElements()) {
            if (menu instanceof JMenu) {
                ((JMenu)menu).getPopupMenu().setLightWeightPopupEnabled(false);
            }
        }
        
        return menuBar;
    }
    
    private String getProjectTitle(String archive) {
        String title = "PulpCore";
        // The project title is displayed in the HTML title.
        // Since there is no mechanism to specify the title, use the archive name.
        // There's no plans to create such mechanism since people can create their own
        // HTML template.
        if (archive != null && archive.toLowerCase().endsWith(".jar")) {
            title = archive.substring(0, archive.length() - 4);
        }
        // Replace dashes with spaces, so "HelloWorld-1.0" becomes "HelloWorld 1.0".
        title = title.replace("-", " ");
        return title;
    }
    
    private void showFrame() {
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
            urls = archiveURL;
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
        
        scripting = new Scripting(applet);
        applet.setStub(this);
        applet.setSize(width, height);
        // Not sure why this doesn't seem to work
        applet.setLocale(Locale.getDefault());
        getContentPane().add(applet, BorderLayout.CENTER);
        // Add 15 pixels for the growbox
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            Filler c = Filler.createVerticalStrut(MAC_GROWBOX_SIZE);
            c.setOpaque(true);
            c.setBackground(osBackground);
            getContentPane().add(c, BorderLayout.SOUTH);
            c.revalidate();
        }
        applet.init();

        // For IDEs that won't capture System.out printed from the EDT
        if (out != null) {
            scripting.invoke("setOut", new Class[] { PrintStream.class }, new Object[] { out });
        }
        
        // Do later
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                active = true;
                applet.start();
                applet.requestFocus();
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
            scripting = null;
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
        applet = null;
        scripting = null;
        
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
        
        if (scripting == null) {
            return;
        }
        
        // Call Stage.getScene()
        Object sceneObject = scripting.invokeStaticInAnimationThread("pulpcore.Stage", "getScene");
        if (sceneObject == null) {
            return;
        }
        Class sceneClass = sceneObject.getClass();
        if (!isValidScene(sceneClass)) {
            return;
        }
        String scene = sceneClass.getName();
        
        // Call Assets.getCatalogs()
        Iterator i = (Iterator)scripting.invokeStatic("pulpcore.Assets","getCatalogs");
        if (i == null) {
            return;
        }
        List<String> assets = new ArrayList<String>();
        while (i.hasNext()) {
            assets.add((String)i.next());
        }
        
        // Store the data
        savedAssets = assets;
        savedScene = scene;
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
        if (scripting == null) {
            return;
        }
   
        Method method = scripting.getMethod("pulpcore.Assets","addCatalog",
            String.class, InputStream.class);
        for (String path : savedAssets) {
            try {
                InputStream is = new URL(applet.getCodeBase(), path).openStream();
                method.invoke(null, path, is);
            }
            catch (Exception ex) {
                ex.printStackTrace();
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
            JOptionPane.showMessageDialog(null, "Could not launch browser.\n" + url);
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
    
    private class EscapeFullScreen extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                Window window = GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().getFullScreenWindow();
                GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().setFullScreenWindow(null);
                if (window != null) {
                    window.dispose();
                }
                if (applet != null) {
                    PulpCorePlayer.this.getContentPane().add(applet, BorderLayout.CENTER);
                    PulpCorePlayer.this.pack();
                    applet.removeKeyListener(this);
                    applet.requestFocus();
                }
                PulpCorePlayer.this.setVisible(true);
            }
        }
    };
    
    public class FileAction extends EditAction { }
    
    public class ViewAction extends EditAction { }
    
    public class FullScreenAction extends EditAction { 
        public void actionPerformed(ActionEvent e) {
            if (applet != null) {
                PulpCorePlayer.this.getContentPane().remove(applet);
                PulpCorePlayer.this.setVisible(false);
                JFrame fullScreenWindow = new JFrame();
                
                // Copy accelerators
                // (Commented out because accelerators aren't working)
                // Probably due to consumed events?
                //fullScreenWindow.setJMenuBar(createJMenuBar());
                //fullScreenWindow.getJMenuBar().setVisible(false);
                
                fullScreenWindow.setUndecorated(true);
                fullScreenWindow.setResizable(false);
                fullScreenWindow.setTitle(PulpCorePlayer.this.getTitle());
                fullScreenWindow.setBackground(Color.BLACK);
                fullScreenWindow.getContentPane().setBackground(Color.BLACK);
                fullScreenWindow.getContentPane().setLayout(new BorderLayout(0,0));
                fullScreenWindow.getContentPane().add(applet, BorderLayout.CENTER);
                applet.addKeyListener(new EscapeFullScreen());
                GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().setFullScreenWindow(fullScreenWindow);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        applet.requestFocus();
                    }
                });
            }
        }
    }
    
    public class ReloadAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            reloadApplet();
        }
    }
    
    public class ScreenshotAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                Object image = scripting.invokeInAnimationThread("getScreenshot");
                if (image != null) {
                    File dir = new File(
                        System.getProperty("user.home") + File.separator + "Desktop");
                    if (!dir.exists()) {
                        dir = new File(System.getProperty("user.home"));
                    }
                    
                    try {
                        File imageFile = File.createTempFile(getTitle(), ".png", dir);
                        ImageIO.write((BufferedImage)image, "PNG", imageFile);
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                        image = null;
                    }
                }
                
                if (image == null) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }
    
    public class SceneInfoAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "toggleInfoOverlay");
            }
        }
    }
    
    public class DirtyRectanglesAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.scene.Scene2D", 
                    "toggleShowDirtyRectangles");
            }
        }
    }
    
    public class ShowConsoleAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "showConsole");
            }
        }
    }
    
    public class ShowSceneSelectorAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "showSceneSelector");
            }
        }
    }
    
    public class SlowSpeedAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "setSpeedSlow");
            }
        }
    }
    
    public class NormalSpeedAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "setSpeedNormal");
            }
        }
    }
    
    public class FastSpeedAction extends EditAction {
        public void actionPerformed(ActionEvent e) {
            if (scripting != null) {
                scripting.invokeStaticInAnimationThread("pulpcore.Stage", "setSpeedFast");
            }
        }
    }
        
    public static class Filler extends JComponent {
        
        public static Filler createVerticalStrut(int height) {
            return new Filler(new Dimension(0, height), new Dimension(0, height), 
                new Dimension(Short.MAX_VALUE, height));
        }
        
        public static Filler createHorizontalStrut(int width) {
            return new Filler(new Dimension(width, 0), new Dimension(width, 0), 
                new Dimension(width, Short.MAX_VALUE));
        }

        public Filler(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
        }

        public void changeShape(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
            revalidate();
        }

        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
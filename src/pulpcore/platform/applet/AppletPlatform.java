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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.Vector;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.AppContext;
import pulpcore.platform.applet.opt.JavaSound;
import pulpcore.platform.applet.opt.NanoTimer;
import pulpcore.platform.Platform;
import pulpcore.platform.SoundEngine;

/*
TODO: 
Metion "accessClipboard" permission somewhere
My .java.policy file: 

grant codeBase "file:/Users/brackeen/Pulp/projects/-" {
     permission java.awt.AWTPermission "accessClipboard";
};

*/
public final class AppletPlatform implements Platform {
    
    private static final AppletPlatform INSTANCE = new AppletPlatform();
    
    private String clipboardText = "";
    private SystemTimer timer;
    
    private AppletAppContext mainContext = null;
    private Vector allContexts = null;
    
    private boolean soundEngineCreated;
    private SoundEngine soundEngine;
    
    
    public static AppletPlatform getInstance() {
        return INSTANCE;
    }
    
    
    private AppletPlatform() {
        
        // Check for Java 1.5 highRes timer
        if (CoreSystem.isJava15orNewer()) {
            try {
                 Class c = Class.forName("java.lang.System");
                 c.getMethod("nanoTime", new Class[0]);
                 timer = new NanoTimer();
            }
            catch (Throwable t) {
                // ignore
            }
        }
        
        // Use an estimating timer on Windows
        if (timer == null) {
            String osName = CoreSystem.getJavaProperty("os.name");
            if (osName != null && osName.startsWith("Windows")) {
                timer = new Win32Timer();
            }
        }
        
        // Use System.currentTimeMillis()
        if (timer == null) {
            timer = new SystemTimer();
        }

        CoreSystem.init(this);
    }
    
    
    //
    // App management
    //
    
    
    public AppContext getThisAppContext() {
        // In most cases, there will be only one registered App. In that case, this method
        // returns as quickly as possible.
        if (allContexts == null) {
            return mainContext;
        }
        
        synchronized (this) {
            // Double check inside the lock
            if (allContexts == null) {
                return mainContext;
            }
            
            // Look through all registered apps and find the context for this ThreadGroup
            ThreadGroup currentThreadGroup = Thread.currentThread().getThreadGroup();
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.elementAt(i);
                ThreadGroup contextThreadGroup = context.getThreadGroup();
                if (contextThreadGroup == currentThreadGroup ||
                    contextThreadGroup.parentOf(currentThreadGroup))
                {
                    return context;
                }
            }
            
            throw new Error("No context found for thread");
        }
    }
    
    
    private synchronized AppContext getAppContext(CoreApplet app) {
        if (mainContext != null && mainContext.getApplet() == app) {
            return mainContext;
        }
        
        if (allContexts != null) {
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.elementAt(i);
                if (context.getApplet() == app) {
                    return context;
                }
            }
        }
        
        return null;
    }
    
    
    private synchronized boolean isRegistered(CoreApplet app) {
        return (getAppContext(app) != null);
    }
    
    
    private synchronized int getNumRegisteredApps() {
        if (allContexts == null) {
            if (mainContext == null) {
                return 0;
            }
            else {
                return 1;
            }
        }
        else {
            return allContexts.size();
        }
    }
    
    
    public synchronized AppContext registerApp(CoreApplet app) {
        
        if (app == null) {
            return null;
        }
        
        AppContext context = getAppContext(app);
        if (context != null) {
            return context;
        }
        
        boolean wasEmpty = (getNumRegisteredApps() == 0);
        
        AppletAppContext newContext = new AppletAppContext(app, timer);
        if (mainContext == null) {
            mainContext = newContext;
        }
        else {
            if (allContexts == null) {
                allContexts = new Vector();
                allContexts.addElement(mainContext);
            }
            allContexts.addElement(newContext);
        }
               
        if (wasEmpty) {
            timer.start();
        }
        
        return newContext;
    }
    
    
    public synchronized void unregisterApp(CoreApplet app) {
        
        if (app == null || !isRegistered(app)) {
            return;
        }
        
        if (mainContext != null && mainContext.getApplet() == app) {
            mainContext.destroy();
            mainContext = null;
        }
        
        if (allContexts != null) { 
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.elementAt(i);
                if (context.getApplet() == app) {
                    context.destroy();
                    allContexts.removeElementAt(i);
                    break;
                }
            }
            
            if (mainContext == null) {
                mainContext = (AppletAppContext)allContexts.elementAt(0);
            }
            
            if (allContexts.size() == 1) {
                allContexts = null;
            }
        }
        
        
        if (getNumRegisteredApps() == 0) {
            timer.stop();
            if (soundEngine != null) {
                soundEngine.destroy();
                soundEngine = null;
            }
            soundEngineCreated = false;
        }
    }
    
    
    //
    // System time
    //
    
    
    public long getTimeMillis() {
        return timer.getTimeMillis();
    }
    
    
    public long getTimeMicros() {
        return timer.getTimeMicros();
    }
    
    
    public long sleepUntilTimeMillis(long time) {
        return timer.sleepUntilTimeMillis(time);
    }
    
    
    //
    // Clipboard
    //
    
    
    public boolean isNativeClipboard() {
        if (!Build.DEBUG) {
            // The applet doesn't have permission
            return false;
        }
        else {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard();
                return true;
            }
            catch (SecurityException ex) {
                return false;
            }
        }
    }
    
    
    public String getClipboardText() {
        if (!Build.DEBUG) {
            // The applet doesn't have permission
            return clipboardText;
        }
        else {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                
                int attemptsLeft = 30; // 30*100ms = 3 seconds
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
                // Failure to access clipboard
                return "";
            }
            catch (SecurityException ex) {
                // The applet doesn't have permission
                return clipboardText;
            }
        }
    }
    
    
    public void setClipboardText(String text) {
        if (text == null) {
            text = "";
        }
        
        if (!Build.DEBUG) {
            // The applet doesn't have permission
            clipboardText = text;
        }
        else {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection data = new StringSelection(text);
                int attemptsLeft = 30; // 30*100ms = 3 seconds
                
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
                // Failure to access clipboard
                return;
            }
            catch (SecurityException ex) {
                // The applet doesn't have permission
                clipboardText = text;
            }
        }
    }
    
    
    //
    // Sound
    //
    
    public boolean isSoundEngineCreated() {
        return soundEngineCreated;
    }
    
    
    public SoundEngine getSoundEngine() {
        if (soundEngineCreated) {
            return soundEngine;
        }
        
        if (CoreSystem.isJava13orNewer()) {
            try {
                Class.forName("javax.sound.sampled.AudioSystem");
                soundEngine = new JavaSound();
                CoreSystem.setTalkBackField("pulpcore.platform.sound", "javax.sound");
            }
            catch (Exception ex) {
                // ignore
            }
        }
        
        if (soundEngine == null) {
            try {
                Class.forName("sun.audio.AudioPlayer");
                soundEngine = new SunAudio();
                CoreSystem.setTalkBackField("pulpcore.platform.sound", "sun.audio");
            }
            catch (Exception ex) {
                // ignore
            }
        }
        
        if (soundEngine == null) {
            CoreSystem.setTalkBackField("pulpcore.sound", "none");
        }
        
        soundEngineCreated = true;
        
        return soundEngine;
    }
    
    
    //
    // Browser
    //
    
        
    public boolean isBrowserHosted() {
        return true;
    }
    
    
    /**
        Returns the name of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserName() {
        AppletAppContext context = (AppletAppContext)getThisAppContext();
        Object value = context.callJavaScript("pulpcore_getBrowserName");
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    
    /**
        Returns the version of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserVersion() {
        AppletAppContext context = (AppletAppContext)getThisAppContext();
        Object value = context.callJavaScript("pulpcore_getBrowserVersion");
        if (value != null) {
            return value.toString();
        }
        return null;
    }
   
}

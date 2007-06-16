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

package pulpcore;

import java.net.URL;
import java.util.Vector;
import pulpcore.net.Upload;
import pulpcore.platform.AppContext;
import pulpcore.platform.Platform;
import pulpcore.platform.SoundEngine;

/**
    The CoreSystem class contains useful platform-specific methods.
    The class cannot be instantiated.
*/
public class CoreSystem {

    static {
        System.out.println(
            "PulpCore " + Build.VERSION + " " +
            "(build " + Build.BUILD_NUMBER + ") " +
            "by Interactive Pulp, LLC.");
        
        IS_MAC_OS_X = "Mac OS X".equals(getJavaProperty("os.name"));
        
        String javaVersion = getJavaProperty("java.version");

        if (javaVersion != null) {
            IS_JAVA_1_3 = (javaVersion.compareTo("1.3") >= 0);
            IS_JAVA_1_4 = (javaVersion.compareTo("1.4") >= 0);
            IS_JAVA_1_5 = (javaVersion.compareTo("1.5") >= 0);
            IS_JAVA_1_6 = (javaVersion.compareTo("1.6") >= 0);
        }
        else {
            IS_JAVA_1_3 = false;
            IS_JAVA_1_4 = false;
            IS_JAVA_1_5 = false;
            IS_JAVA_1_6 = false;
        }
    }

    private static final boolean IS_JAVA_1_3;
    private static final boolean IS_JAVA_1_4;
    private static final boolean IS_JAVA_1_5;
    private static final boolean IS_JAVA_1_6;
    private static final boolean IS_MAC_OS_X;

    private static Platform platform;


    public static void init(Platform platform) {
        CoreSystem.platform = platform;
    }
    
    
    /**
        Gets a Java system property. Returns null if the property does not exist or there is a 
        security excepion.
    */
    public static String getJavaProperty(String name) {
        try {
            return System.getProperty(name);
        }
        catch (SecurityException ex) {
            return null;
        }
    }


    /**
        Used internally by PulpCore - most apps will not need to access
        the Platform instance.
    */
    public static Platform getPlatform() {
        return platform;
    }
    
    
    /**
        Used internally by PulpCore - most apps will not need to access
        the AppContext instance.
    */
    public static AppContext getThisAppContext() {
        return platform.getThisAppContext();
    }


    /**
        Returns true if Java 1.3 or newer is in use.
    */
    public static final boolean isJava13orNewer() {
        return IS_JAVA_1_3;
    }


    /**
        Returns true if Java 1.4 or newer is in use.
    */
    public static final boolean isJava14orNewer() {
        return IS_JAVA_1_4;
    }


    /**
        Returns true if Java 1.5 or newer is in use.
    */
    public static final boolean isJava15orNewer() {
        return IS_JAVA_1_5;
    }


    /**
        Returns true if Java 1.6 or newer is in use.
    */
    public static final boolean isJava16orNewer() {
        return IS_JAVA_1_6;
    }
    
    
    public static final boolean isMacOSX() {
        return IS_MAC_OS_X;
    }


    //
    // Shorcut methods to AppContext
    //
    

    /**
        Sets a new TalkBack field. If the named field already exists, it is replaced.
    */
    public static void setTalkBackField(String name, String value) {
        getThisAppContext().setTalkBackField(name, value);
    }


    public static void setTalkBackField(String name, Throwable t) {
        getThisAppContext().setTalkBackField(name, t);
    }


    public static void clearTalkBackFields() {
        getThisAppContext().clearTalkBackFields();
    }


    /**
        <code>
        Upload upload = Stage.sendTalkBackData();

        ...

        boolean sent = upload.isCompleted();
        </code>
        @return null if talkback is not enabled or there are no fields to send.
    */
    public static Upload sendTalkBackData() {
        return getThisAppContext().sendTalkBackData();
    }


    /**
        Determines if this app is running from one of the specified
        hosts.
    */
    public static boolean isValidHost(String[] validHosts) {
        return getThisAppContext().isValidHost(validHosts);
    }


    public static void setConsoleOutputEnabled(boolean consoleOut) {
        getThisAppContext().setConsoleOutputEnabled(consoleOut);
    }


    public static boolean isConsoleOutputEnabled() {
        return getThisAppContext().isConsoleOutputEnabled();
    }


    public static String getLogText() {
        return getThisAppContext().getLogText();
    }
    
    
    public static Vector getLogLines() {
        return getThisAppContext().getLogLines();
    }


    public static void clearLog() {
        getThisAppContext().clearLog();
    }


    /**
        Prints a line of text to the log.
    */
    public static void print(String statement) {
        getThisAppContext().print(statement);
    }


    /**
        Prints a line of text and a Throwable's stack trace to the log.
    */
    public static void print(String statement, Throwable t) {
        getThisAppContext().print(statement, t);
    }


    /**
        Prints the amount of current memory usage and the change in memory
        usage since the last call to this method. System.gc() is called
        before querying the amount of free memory.
    */
    public static void printMemory(String statement) {
        getThisAppContext().printMemory(statement);
    }


    /**
        Attempts to store persistant user data to the local machine.
        <p>
        For applets, each key is stored in a Base64-encoded cookie. 
        The user's web browser must have 
        LiveConnect, JavaScript, and cookies enabled.
        Web browsers may have the following limitations for cookies 
        (according to RFC 2109 section 6.3):
        <ul>
        <li>300 cookies total</li>
        <li>20 cookies per domain (per site, not per page)</li>
        <li>4,096 bytes per cookie (name and value combined)</li>
        </ul>
        Additionally, Internet Explorer allows only 4,096 bytes per domain. 
        <p>
        Cookies may "expire" (become unaccessable)
        after an amount of time or may be deleted at any time by the browser.
        <p>
        Base64 encoding increases the data size by 33%.
        In summary, for Applets, try to use as few keys as possible, and keep
        the data length to a minimum.
        <p>
        Example: CoreSystem.putUserData("MyGame", data);
    */
    public static void putUserData(String key, byte[] data) {
        getThisAppContext().putUserData(key, data);
    }
    
    
    public static byte[] getUserData(String key) {
        return getThisAppContext().getUserData(key);
    }


    public static void removeUserData(String key) {
        getThisAppContext().removeUserData(key);
    }


    public static URL getBaseURL() {
        return getThisAppContext().getBaseURL();
    }


    public static String getLocaleLanguage() {
        return getThisAppContext().getLocaleLanguage();
    }


    public static String getLocaleCountry() {
        return getThisAppContext().getLocaleCountry();
    }

    
    public static void showDocument(String url) {
        showDocument(url, "_top");
    }
    
    public static void showDocument(String url, String target) {
        getThisAppContext().showDocument(url, target);
    }
    
    
    //
    // Shorcut methods to Platform
    //


    /**
        Returns the current value of the system timer in milliseconds.
    */
    public static long getTimeMillis() {
        return platform.getTimeMillis();
    }


    /**
        Returns the current value of the system timer in microseconds.
    */
    public static long getTimeMicros() {
        return platform.getTimeMicros();
    }


    /**
        Checks if the platform has access to the native operating system
        clipboard. If not, an internal clipboard is used.
    */
    public static boolean isNativeClipboard() {
        return platform.isNativeClipboard();
    }


    /**
        Returns the text currently in the clipboard. Returns an empty string
        if there is no text in the clipboard.
    */
    public static String getClipboardText() {
        return platform.getClipboardText();
    }


    /**
        Sets the text in the clipboard.
    */
    public static void setClipboardText(String text) {
        platform.setClipboardText(text);
    }
    
    
    /**
        Returns true if this platform is hosted in a browser (Applets)
    */
    public static boolean isBrowserHosted() {
        return platform.isBrowserHosted();
    }
    
    
    /**
        Returns the name of the web browser, or null if the browser name could not be determined.
    */
    public static String getBrowserName() {
        return platform.getBrowserName();
    }

    
    /**
        Returns the version of the web browser, or null if the browser version
        could not be determined.
    */
    public static String getBrowserVersion() {
        return platform.getBrowserVersion();
    }
    
    
    //
    // Sound methods
    //
    
    
    public static void setMute(boolean m) {
        getThisAppContext().setMute(m);
    }

    
    public static boolean isMute() {
        return getThisAppContext().isMute();
    }
    
    
    public static boolean isSoundEngineAvailable() {
        return (getPlatform().getSoundEngine() != null);
    }
    
    
    /**
        Gets the number of sounds currently playing in the sound engine.
    */
    public static int getNumSoundsPlaying() {
        // Don't create the sound engine if it's not already created
        if (!getPlatform().isSoundEngineCreated()) {
            return 0;
        }
        
        SoundEngine soundEngine = getPlatform().getSoundEngine();
        if (soundEngine == null) {
            return 0;
        }
        else {
            return soundEngine.getNumSoundsPlaying();
        }
    }
    
    
    /**
        Gets the maximum number of sounds that can be played simultaneously.
    */
    public static int getMaxSimultaneousSounds() {
        SoundEngine soundEngine = getPlatform().getSoundEngine();
        if (soundEngine == null) {
            return 0;
        }
        else {
            return soundEngine.getMaxSimultaneousSounds();
        }
    }    
    
}
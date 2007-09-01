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

package pulpcore.platform;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.net.Upload;
import pulpcore.scene.Scene;
import pulpcore.Stage;
import pulpcore.util.ByteArray;

public abstract class AppContext {
    
    // Note for ConsoleScene: MAX_LOG_LINES * (fontHeight+2) must be less than Fixed.MAX_VALUE 
    private static final int MAX_LOG_LINES = 1000;

    private static int nextContextID = 0;
    
    private ThreadGroup threadGroup;
    private Hashtable talkbackFields = new Hashtable();

    // Logging

    private Vector logLines = new Vector();
    private boolean consoleOut = Build.DEBUG;
    
    /** Last memory value recorded in the printMemory() method. */
    private long lastMemory;
    
    // Sound
    
    private boolean mute = false;
    
    
    public AppContext() {
        getThreadGroup();
        
        setTalkBackField("pulpcore.version", Build.VERSION);
        setTalkBackField("pulpcore.version.build", Build.BUILD_NUMBER);
        setTalkBackField("pulpcore.java.vendor", CoreSystem.getJavaProperty("java.vendor"));
        setTalkBackField("pulpcore.java.version", CoreSystem.getJavaProperty("java.version"));
        setTalkBackField("pulpcore.os.arch", CoreSystem.getJavaProperty("os.arch"));
        setTalkBackField("pulpcore.os.name", CoreSystem.getJavaProperty("os.name"));
        setTalkBackField("pulpcore.os.version", CoreSystem.getJavaProperty("os.version"));
        setTalkBackField("pulpcore.locale.language", getLocaleLanguage());
        setTalkBackField("pulpcore.locale.country", getLocaleCountry());        
    }
    
    
    public ThreadGroup getThreadGroup() {
        if (threadGroup == null || threadGroup.isDestroyed()) {
            threadGroup = new ThreadGroup("PulpCore-App" + nextContextID);
            nextContextID++;
        }
        return threadGroup;
    }
    
    
    /**
        Creates a thread in this context's thread group. This method is useful for creating a new
        thread from the AWT event thread.
    */
    public Thread createThread(String threadName, Runnable runnable) {
        while (true) {
            ThreadGroup currentGroup = getThreadGroup();
            synchronized (currentGroup) {
                if (getThreadGroup() != currentGroup) {
                    continue;
                }
                // At this point we have a non-destroyed ThreadGroup that can't be destroyed
                // until the exit of this synchronized block. Therefore,
                // this constructor can't throw an IllegalThreadStateException
                return new Thread(currentGroup, runnable, threadName);
            }
        }
    }
    
    
    /**
        Runs a thread in this context's thread group. This method is useful for creating a new
        thread from the AWT event thread.
    */
    public void invokeAndWait(String threadName, Runnable runnable) {
        Thread thread = createThread(threadName, runnable);
        thread.start();
        try {
            thread.join();
        }
        catch (InterruptedException ex) {
            // Ignore
        }
    }
    
    
    /**
        Runs a thread in this context's thread group. This method is useful for creating a new
        thread from the AWT event thread.
    */
    public void invokeAndWait(String threadName, int maxWaitTime, Runnable runnable) {
        Thread thread = createThread(threadName, runnable);
        thread.start();
        try {
            thread.join(maxWaitTime);
        }
        catch (InterruptedException ex) {
            // Ignore
        }
    }
    
    public abstract String getAppProperty(String name);
    
    
    public abstract Scene createFirstScene();
    
    
    public abstract void start();
    
    
    public abstract void stop();
    
    
    public abstract void destroy();
    
    
    public abstract void putUserData(String key, byte[] data);
        
        
    public abstract byte[] getUserData(String key);
    
    
    public abstract void removeUserData(String key);
    

    /**
        Returns the local machine's language as a lowercase two-letter 
        ISO-639 code.
        @return an empty String if the language is unknown
    */
    public abstract String getLocaleLanguage();
    
    
    /**
        Returns the local machine's country as an uppercase two-letter
        ISO-3166 code.
        @return an empty String if the country is unknown
    */
    public abstract String getLocaleCountry();
    
    
    public abstract void showDocument(String url, String target);    
    
 
    public abstract CoreImage loadImage(ByteArray in);
    
    
    public abstract URL getBaseURL();
    
    
    public abstract URL getTalkbackURL();
    

    public abstract Input getInputSystem();
    
    
    public abstract Stage getStage();      
  
    
    public abstract Surface getSurface();
    
    
    public void notifyFrameComplete() { };
  
  
    /**
        Adds a new TalkBack field, or replaces an exisiting field.
    */
    public void setTalkBackField(String name, String value) {
        if (getTalkbackURL() != null) {
            talkbackFields.remove(name);
            talkbackFields.put(name, value);
        }

        if (Build.DEBUG) print(name + ": " + value);
    }


    public void setTalkBackField(String name, Throwable t) {
        setTalkBackField(name, stackTraceToString(t));
    }


    public void clearTalkBackFields() {
        talkbackFields = new Hashtable();
    }


    /**
        <code>
        Upload upload = Stage.sendTalkBackData();

        ...

        boolean sent = upload.isCompleted();
        </code>
        @return null if talkback is not enabled or there are no fields to send.
    */
    public Upload sendTalkBackData() {
        if (talkbackFields.size() == 0) {
            return null;
        }
        URL talkbackURL = getTalkbackURL();
        if (talkbackURL != null) {
            Upload upload = new Upload(talkbackURL);
            upload.addFields(talkbackFields);
            upload.start();
            return upload;
        }
        return null;
    }


    /**
        Determines if this app is running from one of the specified
        hosts.
    */
    public boolean isValidHost(String[] validHosts) {
        URL codeBase = getBaseURL();
        String host = codeBase == null ? null : codeBase.getHost();
        
        // Always allow Sloppy
        if (codeBase != null && "127.0.0.1".equals(host) && codeBase.getPort() == 7569) {
            return true;
        }
        
        for (int i = 0; i < validHosts.length; i++) {
            if (host == null && validHosts[i] == null) {
                return true;
            }
            else if (host != null && host.equals(validHosts[i])) {
                return true;
            }
        }

        return false;
    }


    public void setConsoleOutputEnabled(boolean consoleOut) {
        this.consoleOut = consoleOut;
    }


    public boolean isConsoleOutputEnabled() {
        return consoleOut;
    }


    /**
        Each element in the Vector represents one line of text.
    */
    public Vector getLogLines() {
        return logLines;
    }

    
    public String getLogText() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < logLines.size(); i++) {
            buffer.append(logLines.elementAt(i));
            buffer.append("\n");
        }
        return buffer.toString();
    }
    

    public void clearLog() {
        logLines = new Vector();
    }


    /**
        Prints a line of text to the log.
    */
    public void print(String statement) {
        if (statement == null) {
            statement = "null";
        }
        if (consoleOut) {
            System.out.println(statement);
        }

        if (!Build.DEBUG) {
            return;
        }
        
        // Split statement by newlines
        while (true) {
            int index = statement.indexOf('\n');
            if (index != -1) {
                logLines.addElement(statement.substring(0, index));
                statement = statement.substring(index + 1);
            }
            else {
                break;
            }
        }
        logLines.addElement(statement);
        
        // Trim the log if there are too many lines.
        while (logLines.size() > MAX_LOG_LINES) {
            logLines.removeElementAt(0);
        }
    }


    /**
        Prints a line of text and a Throwable's stack trace to the log.
    */
    public void print(String statement, Throwable t) {
        print(statement);
        print(stackTraceToString(t));
    }


    private String stackTraceToString(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        return writer.getBuffer().toString();
    }


    /**
        Prints the amount of current memory usage and the change in memory
        usage since the last call to this method. System.gc() is called
        before querying the amount of free memory.
    */
    public void printMemory(String statement) {

        String label = "usage";
        long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long change = currentMemory - lastMemory;

        if (Math.abs(change) < 2048) {
            // print memory change in bytes
            long valueDecimal = ((Math.abs(currentMemory) * 10) >> 10) % 10;
            print(statement + " (" +
                  (currentMemory >> 10) + "." + valueDecimal + " KB " + label + ", " +
                  (change > 0 ? "+" : "") + change + " bytes change)");
        }
        else {
            // print memory change in kilobytes
            long valueDecimal = ((Math.abs(currentMemory) * 10) >> 10) % 10;
            long changeDecimal = ((Math.abs(change) * 10) >> 10) % 10;
            print(statement + " (" +
                  (currentMemory >> 10) + "." + valueDecimal + " KB " + label + ", " +
                  (change > 0 ? "+" : "") +
                  (change >> 10) + "." + changeDecimal + " KB change)");
        }

        lastMemory = currentMemory;
    }
    
    
    public void setMute(boolean m) {
        mute = m;
    }

    
    public boolean isMute() {
        return mute;
    }

}

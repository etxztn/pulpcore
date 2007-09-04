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

package pulpcore.player;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

public class BrowserLaunch {
    
    // Prevent instantiaion
    private BrowserLaunch() {}
    
    
    /**
        Launches the default browser to display a URL. In most cases, the default 
        browser is launched.
        @throws UnsupportedOperationException if the browser could not be launched
    */
    public static void open(URL url) throws UnsupportedOperationException { 
        
        // First try java.awt.Desktop available on Java 1.6
        try {
            Class desktopClass = Class.forName("java.awt.Desktop");
            Method getDesktop = desktopClass.getMethod("getDesktop", new Class[0]);
            Method browse = desktopClass.getMethod("browse", new Class[] { URI.class });
            Object desktop = getDesktop.invoke(null, new Object[0]);
            browse.invoke(desktop, new Object[] { url.toURI() });
            return;
        }
        catch (Exception ex) {
            // Pass-through
        }
        
        // Next, try BareBones browser launch code
        // Based on code from http://www.centerkey.com/java/browser/
        String osName = System.getProperty("os.name"); 
        try { 
            if (osName.startsWith("Mac OS")) { 
                Class fileMgr = Class.forName("com.apple.eio.FileManager"); 
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class}); 
                openURL.invoke(null, new Object[] { url.toString() }); 
            } 
            else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
            else { 
                // Assume Unix or Linux 
                String[] browsers = { 
                    "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" 
                };
                String browser = null;
                for (int i = 0; i < browsers.length; i++) {
                    String[] command = { "which", browsers[i] };
                    if (Runtime.getRuntime().exec(command).waitFor() == 0) {
                        browser = browsers[i];
                        break;
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                }
                else {
                    Runtime.getRuntime().exec(new String[] { browser, url.toString() }); 
                }
            }
        } 
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
}

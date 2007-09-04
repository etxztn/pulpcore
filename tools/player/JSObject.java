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

package netscape.javascript;

import java.applet.Applet;
import java.util.prefs.Preferences;
import pulpcore.Build;

// LiveConnect emulator for the PulpCore Player
public class JSObject {
    
    private static final JSObject instance = new JSObject();
    
    private Preferences prefs;
    
    private JSObject() {
        prefs = Preferences.userNodeForPackage(pulpcore.player.PulpCorePlayer.class); 
    }
    
    
    public static JSObject getWindow(Applet applet) {
        return instance;
    }
    
    
    public Object call(String method, Object[] args) {
        try {
            if ("pulpcore_appletLoaded".equals(method)) {
                // Do nothing
                return null;
            }
            else if ("pulpcore_getBrowserName".equals(method)) {
                return "PulpCore Player";
            }
            else if ("pulpcore_getBrowserVersion".equals(method)) {
                return Build.VERSION;
            }
            else if ("pulpcore_setCookie".equals(method)) {
                // Can throw IllegalArgumentException if name or value is too long
                prefs.put((String)args[0], (String)args[1]);
                return null;
            }
            else if ("pulpcore_getCookie".equals(method)) {
                return prefs.get((String)args[0], null);
            }
            else if ("pulpcore_deleteCookie".equals(method)) {
                prefs.remove((String)args[0]);
                return null;
            }
            else {
                return null;
            }
        }
        catch (Exception ex) {
            return null;
        }
    }
}

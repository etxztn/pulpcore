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

import java.io.InputStream;
import java.io.IOException;
import java.lang.ClassLoader;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
    A class loader that doesn't cache resources (non-classes) inside of jars.
*/
public class NoResourceCacheClassLoader extends URLClassLoader {
    
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
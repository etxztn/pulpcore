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

import java.awt.Frame;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class PlayerTask extends Task {
    
    private String path;
    private String archive;
    private String scene;
    private String width;
    private String height;
    private String assets;
    private boolean waitUntilClosed = true;
    
    
    public void setPath(String path) {
        this.path = path;
    }
    
    
    public void setArchive(String archive) {
        this.archive = archive;
    }
    
    
    public void setScene(String scene) {
        this.scene = scene;
    }
    
    
    public void setAssets(String assets) {
        this.assets = assets;
    }
    
    
    public void setWidth(String width) {
        this.width = width;
    }
    
    
    public void setHeight(String height) {
        this.height = height;
    }
    
    
    public void setWaitUntilClosed(boolean waitUntilClosed) {
        this.waitUntilClosed = waitUntilClosed;
    }
    
    
    public void execute() throws BuildException {
        if (path == null) {
            throw new BuildException("The path is not specified.");
        }
        if (archive == null) {
            throw new BuildException("The archive is not specified.");
        }
        if (scene == null) {
            throw new BuildException("The scene is not specified.");
        }
        if (width == null) {
            throw new BuildException("The width is not specified.");
        }
        if (height == null) {
            throw new BuildException("The height is not specified.");
        }
        
        String[] args = { path, archive, scene, width, height, assets, 
            waitUntilClosed ? "true" : "false" };
        
        if (!waitUntilClosed) {
            
            // Find an existing player within a running IDE
            // Note, Ant creates a new ClassLoader each time a taskdef'd task is executed.
            // So we can't access the same class since it was started from a different ClassLoader.
            // A trick: loop through existing frames to find the class.
            String className = PulpCorePlayer.class.getName();
            Frame[] frames = Frame.getFrames();
            for (int i = 0; i < frames.length; i++) {
                Class c = frames[i].getClass();
                if (c.getName().equals(className)) {
                    try {
                        Method m = c.getMethod("main", new Class[] { args.getClass() });
                        m.invoke(null, new Object[] { args });
                        return;
                    }
                    catch (Exception ex) {
                        log(ex.getMessage());
                    }
                }
            }
        }
        
        // No running copy found: start a new one.
        PulpCorePlayer.main(args);
    }
  
}

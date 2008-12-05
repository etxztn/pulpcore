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

package pulpcore.assettools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;

public class AppletHTMLTask extends Task {
    
    // Matches those values in pulpcore.js
    private static final String DEFAULT_CLASS_NAME = "pulpcore.platform.applet.CoreApplet.class";
    private static final String DEFAULT_ARCHIVE = "project.jar";
    private static final String DEFAULT_BGCOLOR = "#000000";
    private static final String DEFAULT_FGCOLOR = "#aaaaaa";
    private static final String DEFAULT_SCENE = null;
    private static final String DEFAULT_ASSETS = "";
    private static final String DEFAULT_CODEBASE = null;
    private static final String DEFAULT_PARAMS = null;
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    
    private File destDir;
    private File template = null;
    private File displaySource = null;
    private String splash = null;
    private String playSplash = null;
    
    private String className = DEFAULT_CLASS_NAME;
    private String archive = DEFAULT_ARCHIVE;
    private String bgcolor = DEFAULT_BGCOLOR;
    private String fgcolor = DEFAULT_FGCOLOR;
    private String scene = DEFAULT_SCENE;
    private String assets = DEFAULT_ASSETS;
    private String params = DEFAULT_PARAMS;
    private String codebase = DEFAULT_CODEBASE;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
    
    public void setDisplaySource(File displaySource) {
        this.displaySource = displaySource;
    }
    
    public void setTemplate(File template) {
        this.template = template;
    }
    
    public void setSplash(String splash) {
        this.splash = splash;
    }

    public void setPlaySplash(String playSplash) {
        this.playSplash = playSplash;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public void setScene(String scene) {
        this.scene = scene;
    }
    
    public void setParams(String params) {
        this.params = params;
    }
    
    public void setCodebase(String codebase) {
        this.codebase = codebase;
    }
    
    public void setAssets(String assets) {
        this.assets = assets;
    }
    
    public void setArchive(String archive) {
        this.archive = archive;
    }
    
    public void setBgColor(String bgcolor) {
        this.bgcolor = bgcolor;
    }
    
    public void setFgColor(String fgcolor) {
        this.fgcolor = fgcolor;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public void execute() throws BuildException {
        if (destDir == null) {
            throw new BuildException("The destDir is not specified.");
        }
        if (!destDir.exists() || !destDir.isDirectory()) {
            throw new BuildException("Not a valid path: " + destDir);
        }
        if (scene == null) {
            throw new BuildException("The firstScene is not specified.");
        }
        
        try {
            createHTML();
        }
        catch (IOException ex) {
            throw new BuildException("Error creating applet html in " + destDir, ex);
        }
    }
    
    public static boolean equals(String arg1, String arg2) {
        if (arg1 == null || arg2 == null) {
            return (arg1 == arg2);
        }
        else {
            return arg1.equals(arg2);
        }
    }
    
    private void createHTML() throws IOException {
        String appletParams = "";
        
        // Always show width and height (for webmasters)
        appletParams += "pulpcore_width = " + width + ";\n";
        appletParams += "pulpcore_height = " + height + ";\n";
        
        if (!equals(DEFAULT_CLASS_NAME, className)) {
            appletParams += "pulpcore_class = \"" + className + "\";\n";
        }
        if (splash != null && splash.length() > 0) {
            appletParams += "pulpcore_splash = \"" + splash + "\";\n";
        }
        if (playSplash != null && playSplash.length() > 0) {
            appletParams += "pulpcore_play_splash = \"" + playSplash + "\";\n";
        }
        if (!equals(DEFAULT_ARCHIVE, archive)) {
            appletParams += "pulpcore_archive = \"" + archive + "\";\n";
        }
        if (!equals(DEFAULT_ASSETS, assets)) {
            appletParams += "pulpcore_assets = \"" + assets + "\";\n";
        }
        if (!equals(DEFAULT_SCENE, scene)) {
            appletParams += "pulpcore_scene = \"" + scene + "\";\n";
        }
        if (!equals(DEFAULT_BGCOLOR, bgcolor)) {
            appletParams += "pulpcore_bgcolor = \"" + bgcolor + "\";\n";
        }
        if (!equals(DEFAULT_FGCOLOR, fgcolor)) {
            appletParams += "pulpcore_fgcolor = \"" + fgcolor + "\";\n";
        }
        if (params != null && params.length() > 0) {
            appletParams += "pulpcore_params = { " + params + " };\n";
        }
        if (codebase != null && codebase.length() > 0) {
            appletParams += "pulpcore_codebase = \"" + codebase + "\";\n";
        }
        // Strip last newline
        appletParams = appletParams.trim();
        
        String src = "";
        if (displaySource != null && displaySource.exists() && displaySource.isFile()) {
            
            String links = "";
            
            FileFilter filter = new FileFilter() {
                public boolean accept(File pathname) {
                    return (pathname.isFile() && !AssetTask.isIgnoredAsset(pathname));
                }
            };
            
            File dir = new File(destDir, "src");
            for (File srcFile : displaySource.getParentFile().listFiles(filter)) {
                
                String filename = srcFile.getName();
                dir.mkdir();
                
                // Use the Ant copy task
                Copy copyTask = new Copy();
                copyTask.setProject(getProject());
                copyTask.setTaskName(getTaskName());
                copyTask.setFile(srcFile);
                copyTask.setTofile(new File(dir, filename));
                copyTask.execute();
                
                // Setup the hyperlink
                String link = "<a target=\"pulpcore_src\" href=\"src/" + filename + "\">" +
                    filename + "</a>&nbsp; ";
                if (AssetTask.isSourceFile(srcFile)) {
                    // First
                    links = link + links;
                }
                else {
                    // Last
                    links += link;
                }
            }
            if (links.length() > 0) {
                links = "<p>" + links + "</p>";
            }
            
            String sourceCode = readTextFile(new FileInputStream(displaySource));
            sourceCode = sourceCode.replace("&", "&amp;");
            sourceCode = sourceCode.replace("\"", "&quot;");
            sourceCode = sourceCode.replace("<", "&lt;");
            sourceCode = sourceCode.replace(">", "&gt;");
            
            src = links + "<pre class=\"prettyprint\">" + sourceCode + "</pre>";
            
            /*
            String sourceCode = readFirstComments(new FileInputStream(displaySource));
            sourceCode = sourceCode.replace("&", "&amp;");
            sourceCode = sourceCode.replace("\"", "&quot;");
            sourceCode = sourceCode.replace("<", "&lt;");
            sourceCode = sourceCode.replace(">", "&gt;");
            sourceCode = sourceCode.replace("\n", "<br />");
            
            src = links + "<p>" + sourceCode + "</p>";
            */
        }
        
        String appletHTML;
        if (template == null) {
            appletHTML = readTextFile(getClass().getResourceAsStream("/applet.html"));
        }
        else {
            appletHTML = readTextFile(new FileInputStream(template));
        }
            
        appletHTML = appletHTML.replace("@BGCOLOR@", bgcolor);
        appletHTML = appletHTML.replace("@FGCOLOR@", fgcolor);
        appletHTML = appletHTML.replace("@APPLET_PARAMS@", appletParams);
        appletHTML = appletHTML.replace("@SRC@", src);
        appletHTML = appletHTML.replace("@TITLE@", getProjectTitle());
        
        // Write to dest directory: index.html, pulpcore.js, splash.gif
        writeTextFile(new File(destDir, "index.html"), appletHTML);
        writeTextFile(new File(destDir, "pulpcore.js"),
            readTextFile(getClass().getResourceAsStream("/pulpcore.js")));
        if (splash == null || splash.length() == 0) {
            writeBinaryFile(new File(destDir, "splash.gif"),
                readBinaryFile(getClass().getResourceAsStream("/splash.gif")));
        }
        else if (!splash.startsWith("http://")) {
            File splashFile = new File(getProject().getBaseDir(), splash);
            writeBinaryFile(new File(destDir, splash),
                readBinaryFile(new FileInputStream(splashFile)));
        }
        if (playSplash == null || playSplash.length() == 0) {
            writeBinaryFile(new File(destDir, "play.gif"),
                readBinaryFile(getClass().getResourceAsStream("/play.gif")));
        }
        else if (!playSplash.startsWith("http://")) {
            File playSplashFile = new File(getProject().getBaseDir(), playSplash);
            writeBinaryFile(new File(destDir, playSplash),
                readBinaryFile(new FileInputStream(playSplashFile)));
        }
    }
    
    private String getProjectTitle() {
        String title = "Project";
        // The project title is displayed in the HTML title.
        // Since there is no mechanism to specify the title, use the archive name.
        // There's no plans to create such mechanism since people can create their own
        // HTML template.
        if (archive.toLowerCase().endsWith(".jar")) {
            title = archive.substring(0, archive.length() - 4);
        }
        // Replace dashes with spaces, so "HelloWorld-1.0" becomes "HelloWorld 1.0".
        title = title.replace("-", " ");
        return title;
    }
    
    private String readFirstComments(InputStream in) throws IOException {
        String text = "";
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(in));
        while (true) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("//")) {
                reader.close();
                return text;
            }
            text += line.substring(2).trim() + '\n';
        }
    }
    
    private String readTextFile(InputStream in) throws IOException {
        String text = "";
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(in));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return text;
            }
            text += line + '\n';
        }
    }
    
    private void writeTextFile(File file, String text) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.append(text);
        writer.close();
    }
    
    private byte[] readBinaryFile(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedInputStream bin = new BufferedInputStream(in);
        
        byte[] buffer = new byte[2048];
        
        while (true) {
            int bytesRead = bin.read(buffer);
            if (bytesRead == -1) {
                in.close();
                return out.toByteArray();
            }
            out.write(buffer, 0, bytesRead);
        }
    }
    
    private void writeBinaryFile(File file, byte[] data) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        out.write(data);
        out.close();
    }
}

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

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import pulpcore.assettools.png.PNGWriter;

public class AssetTask extends Task {
    
    public static final String[] IMAGE_TYPES = { "png", "gif", "bmp", "svg", "svgz" };
    
    private File srcDir;
    private File destDir;
    private int optimizationLevel = PNGWriter.DEFAULT_OPTIMIZATION_LEVEL;
    
    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }
    
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
    
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    } 
    
    public void execute() throws BuildException {
        if (srcDir == null) {
            throw new BuildException("The srcDir is not specified.");
        }
        if (destDir == null) {
            throw new BuildException("The destDir is not specified.");
        }
        if (optimizationLevel < 0 || optimizationLevel > PNGWriter.MAX_OPTIMIZATION_LEVEL) {
            throw new BuildException("Optimization level must be between 0 and " + 
                PNGWriter.MAX_OPTIMIZATION_LEVEL + ".");
        }
        
        if (!srcDir.exists()) {
            log("Ignoring nonexisting path: " + srcDir);
            return;
        }
        
        //String javaVersion = System.getProperty("java.version");
        //if (javaVersion.compareTo("1.6") < 0) {
        //    log("Java 6 or newer is recommended for font creation.", Project.MSG_WARN);
        //}
        
        try {
            traverseDirectory(srcDir, destDir, true);
        }
        catch (IOException ex) {
            throw new BuildException("Error converting assets", ex);
        }
    }
    
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        if (file.isFile()) {
            for (String type : IMAGE_TYPES) {
                if (name.endsWith("." + type)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isImagePropertyFile(File file) {
        boolean isImagePropertyFile = false;
        String name = file.getName().toLowerCase();
        if (file.isFile() && name.endsWith(".properties") && !name.endsWith(".font.properties")) {
            // Check if a matching image exists
            for (String type : IMAGE_TYPES) {
                 File imageFile = new File(removeExtension(file.getPath()) + "." + type);
                 if (imageFile.exists()) {
                     isImagePropertyFile = true;
                     break;
                 }
            }
        }
        return isImagePropertyFile;
    }
    
    private boolean isFontFile(File file) {
        String name = file.getName().toLowerCase();
        return (file.isFile() && name.endsWith(".font.properties"));
    }
    
    private boolean isSoundFile(File file) {
        String name = file.getName().toLowerCase();
        return 
            file.isFile() &&
            (name.endsWith(".wav") ||
            name.endsWith(".au"));
    }
    
    private boolean isOtherAsset(File file) {
        String name = file.getName();
        String lowerCaseName = name.toLowerCase();
        
        return (file.isFile() && 
            !isImageFile(file) && !isImagePropertyFile(file) && !isFontFile(file) &&
            !isIgnoredAsset(file));
    }
    
    public static boolean isIgnoredAsset(File file) {
        String name = file.getName();
        String lowerCaseName = name.toLowerCase();
        
        // Set of file to ignore. Ignore .java files because of the "quick" template
        // directory structure.
        return (
            lowerCaseName.endsWith(".ttf") || 
            lowerCaseName.endsWith(".java") || 
            name.equals("Thumbs.db") || 
            name.equals(".DS_Store") ||
            name.equals(".cvsignore") || 
            name.equals("vssver.scc"));
    }
    
    private boolean isTraverseableDirectory(File dir) {
        String name = dir.getName();
        
        return (dir.isDirectory() && !name.equals("CVS") && !name.equals("SCCS") &&
            !name.equals(".svn"));
    }
    
    private void traverseDirectory(File dir, File destDir, boolean recurse) throws IOException {
        
        destDir.mkdir();
        File[] files = dir.listFiles();
        
        for (File file : files) {
            if (isImageFile(file)) {
                File propertyFile = new File(removeExtension(file.getPath()) + ".properties");
                File destFile = new File(destDir, removeExtension(file.getName()) + ".png");
                if (needsUpdate("image", destFile, file, propertyFile)) {
                    ConvertImageTask imageTask = new ConvertImageTask();
                    initSubTask(imageTask);
                    imageTask.setSrcFile(file);
                    if (propertyFile.exists()) {
                        imageTask.setSrcPropertyFile(propertyFile);
                    }
                    imageTask.setDestFile(destFile);
                    imageTask.setOptimizationLevel(optimizationLevel);
                    imageTask.execute();
                }
            }
            else if (isFontFile(file)) {
                // From ".font.properties" to ".font.png"
                String newName = removeExtension(file.getName()) + ".png";
                File destFile = new File(destDir, newName);
                if (needsUpdate("font", destFile, file)) {
                    ConvertFontTask fontTask = new ConvertFontTask();
                    initSubTask(fontTask);
                    fontTask.setSrcFile(file);
                    fontTask.setDestFile(destFile);
                    fontTask.setOptimizationLevel(optimizationLevel);
                    fontTask.execute();
                }
            }
            else if (isSoundFile(file)) {
                File destFile = new File(destDir, file.getName());
                if (needsUpdate("sound", destFile, file)) {
                    ConvertSoundTask soundTask = new ConvertSoundTask();
                    initSubTask(soundTask);
                    soundTask.setSrcFile(file);
                    soundTask.setDestFile(destFile);
                    soundTask.execute();
                }
            }
            else if (isImagePropertyFile(file)) {
                // Do nothing!
            }
            else if (isOtherAsset(file)) {
                File destFile = new File(destDir, file.getName());
                if (needsUpdate("file", destFile, file)) {
                    Copy copyTask = new Copy();
                    initSubTask(copyTask);
                    copyTask.setFile(file);
                    copyTask.setTofile(destFile);
                    copyTask.execute();
                }
            }
            else if (isTraverseableDirectory(file) && recurse) {
                File newDestDir = new File(destDir, file.getName());
                traverseDirectory(file, newDestDir, true);
            }
            else {
                log("Ignoring: " + file, Project.MSG_VERBOSE);
            }
        }
    }
    
    private void initSubTask(Task task) {
        task.setProject(getProject());
        task.setTaskName(getTaskName());
    }
    
    private boolean needsUpdate(String type, File destFile, File srcFile) {
        return needsUpdate(type, destFile, srcFile, null);
    }
    
    private boolean needsUpdate(String type, File destFile, File srcFile, File srcMetaFile) {
        if (!destFile.exists()) {
            return true;
        }
        else {
            long destDate = destFile.lastModified();
            long srcDate = srcFile.lastModified();
            if (srcMetaFile != null) {
                srcDate = Math.max(srcDate, srcMetaFile.lastModified());
            }
            
            if (destDate == 0 || srcDate == 0) {
                // Unknown date - go ahead and update
                return true;
            }
            else if (srcDate > destDate) {
                return true;
            }
            else {
                log("Not updating " + type + ": " + destFile, Project.MSG_VERBOSE);
                return false;
            }
        }
    }
    
    public static String removeExtension(String name) {
        int index = name.lastIndexOf('.');
        
        if (index != -1) {
            name = name.substring(0, index);
        }
        
        return name;
    }
}

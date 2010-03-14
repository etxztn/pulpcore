/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
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

package org.pulpcore.tools.res;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.pulpcore.tools.png.PNGWriter;

/**
    @phase process-resources
    @goal convert-resources
 */
public class AssetTask extends AbstractMojo {
    
    public static final String[] IMAGE_TYPES = { "png", "gif", "bmp", "svg", "svgz" };

    /**
     @parameter
     @required
     */
    private File srcDir;

    /**
     @parameter
     @required
     */
    private File destDir;

    /**
     @parameter
     */
    private int optimizationLevel = PNGWriter.DEFAULT_OPTIMIZATION_LEVEL;

    /**
     @parameter
     */
    private boolean skipSourceFiles = true;
    
    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }
    
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
    
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }
    
    public void setSkipSourceFiles(boolean skipSourceFiles) {
        this.skipSourceFiles = skipSourceFiles;
    } 
    
    @Override
    public void execute() throws MojoExecutionException {
        if (srcDir == null) {
            throw new MojoExecutionException("The srcDir is not specified.");
        }
        if (destDir == null) {
            throw new MojoExecutionException("The destDir is not specified.");
        }
        if (optimizationLevel < 0 || optimizationLevel > PNGWriter.MAX_OPTIMIZATION_LEVEL) {
            throw new MojoExecutionException("Optimization level must be between 0 and " +
                PNGWriter.MAX_OPTIMIZATION_LEVEL + ".");
        }
        
        if (!srcDir.exists()) {
            getLog().info("Ignored nonexisting path: " + srcDir);
            return;
        }

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        
        //String javaVersion = System.getProperty("java.version");
        //if (javaVersion.compareTo("1.6") < 0) {
        //    log("Java 6 or newer is recommended for font creation.", Project.MSG_WARN);
        //}
        
        try {
            boolean somethingUpdated = traverseDirectory(srcDir, destDir, true);
            if (!somethingUpdated) {
                getLog().info("Nothing to convert - all resources are up to date");
            }
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Error converting assets", ex);
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
        return (file.isFile() && 
            !isImageFile(file) && !isImagePropertyFile(file) && !isFontFile(file) &&
            !isIgnoredAsset(file) && !(skipSourceFiles && isSourceFile(file)));
    }
    
    public static boolean isIgnoredAsset(File file) {
        String name = file.getName();
        
        // Set of file to ignore. Ignore source files because of the "quick" template
        // directory structure.
        return (name.toLowerCase().endsWith(".ttf") || 
            name.equals("Thumbs.db") || 
            name.equals(".DS_Store") ||
            name.equals(".cvsignore") || 
            name.equals("vssver.scc"));
    }
    
    public static boolean isSourceFile(File file) {
        String name = file.getName();
        return (
            name.endsWith(".java") || 
            name.endsWith(".scala") ||
            name.endsWith(".py") ||
            name.endsWith(".groovy") ||
            name.endsWith(".js") ||
            name.endsWith(".rb"));
    }
    
    private boolean isTraverseableDirectory(File dir) {
        String name = dir.getName();
        
        return (dir.isDirectory() && !name.equals("CVS") && !name.equals("SCCS") &&
            !name.equals(".svn"));
    }
    
    private boolean traverseDirectory(File dir, File destDir, boolean recurse) throws IOException, MojoExecutionException {

        boolean somethingUpdated = false;
        destDir.mkdir();
        File[] files = dir.listFiles();
        
        for (File file : files) {
            if (isImageFile(file)) {
                File propertyFile = new File(removeExtension(file.getPath()) + ".properties");
                File destFile = new File(destDir, removeExtension(file.getName()) + ".png");
                if (needsUpdate("image", destFile, file, propertyFile)) {
                    somethingUpdated = true;
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
                    somethingUpdated = true;
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
                    somethingUpdated = true;
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
                    somethingUpdated = true;
                    FileUtils.copyFile(file, destFile, true);
                    getLog().info("Copied: " + file);
                }
            }
            else if (isTraverseableDirectory(file) && recurse) {
                File newDestDir = new File(destDir, file.getName());
                somethingUpdated |= traverseDirectory(file, newDestDir, true);
            }
            else {
                getLog().info("Ignored: " + file);
            }
        }
        return somethingUpdated;
    }
    
    private void initSubTask(AbstractMojo task) {
        task.setLog(getLog());
        task.setPluginContext(getPluginContext());
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
                //getLog().info("Skipped (up-to-date): " + destFile);
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

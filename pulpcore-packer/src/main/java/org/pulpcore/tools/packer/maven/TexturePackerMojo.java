/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
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
package org.pulpcore.tools.packer.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.pulpcore.tools.imagefont.ImageFontBuilder;
import org.pulpcore.tools.packer.TexturePacker;
import org.pulpcore.tools.png.PNGEncoder;

/**
 @goal process-resources
 */
public class TexturePackerMojo extends AbstractMojo {

    /**
     The source directory to recursively search for PNG files.
     @parameter
     @required
     */
    private File srcDir;

    /**
     The destination directory. The destDir may be used with either the srcFile or srcDir.
     @parameter
     @required
     */
    private File destDir;

    /**
     The texture name prefix. The default is "texture".
     @parameter default-value="texture"
     */
    private String filePrefix = "texture";

    /**
     The texture name suffix. The default is a blank String.
     @parameter default-value=""
     */
    private String fileSuffix = "";

    /**
     The texture width.
     @parameter default-value="512"
     */
    private int width = 512;

    /**
     The texture height.
     @parameter default-value="512"
     */
    private int height = 512;

    /**
     The scale to apply to each image before adding it to the texture. The image is scaled
     using area averaging.
     @parameter default-value="1"
     */
    private double scale = 1;

    /**
     The padding between images in the texture. The default is 1.
     @parameter default-value="1"
     */
    private int padding = 1;

    /**
     The output file format. May either be "json" or "cocos2d". The default is "json".
     @parameter default-value="json"
     */
    private String output = "json";

    /**
     The PNG optimization level, from 0 to 5. The default is 2.
     @parameter default-value="2"
     */
    private int optimizationLevel = PNGEncoder.OPTIMIZATION_LEVEL_DEFAULT;

    public void execute() throws MojoExecutionException {
        if (srcDir == null) {
            throw new MojoExecutionException("The srcDir is not specified.");
        }
        if (destDir == null) {
            throw new MojoExecutionException("The destDir is not specified.");
        }

        TexturePacker.OutputFormat outputFormat;

        if ("json".equalsIgnoreCase(output)) {
            outputFormat = TexturePacker.OutputFormat.JSON;
        }
        else if ("cocos2d".equalsIgnoreCase(output)) {
            outputFormat = TexturePacker.OutputFormat.COCOS2D;
        }
        else {
            throw new MojoExecutionException("No such output format: " + output);
        }

        List<BuildFontTask> buildFontTasks = new ArrayList<BuildFontTask>();
        TexturePacker texturePacker = new TexturePacker(width, height, padding, false);
        texturePacker.setPngOptimizationLevel(optimizationLevel);
        try {
            traverse(buildFontTasks, texturePacker, srcDir, "", -1);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Error loading an image", ex);
        }

        if (buildFontTasks.size() == 1) {
            try {
                buildFontTasks.get(0).call();
            }
            catch (Exception ex) {
                throw new MojoExecutionException("Error building font", ex);
            }
        }
        else if (buildFontTasks.size() > 1) {
            // This is really a hack because auto-kerning is so slow.
            int numProcessors = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
            try {
                executor.invokeAll(buildFontTasks);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            executor.shutdown();
        }

        try {
            texturePacker.pack(outputFormat, destDir, filePrefix, fileSuffix);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Error packing texture", ex);
        }
    }

    private int traverse(List<BuildFontTask> buildFontTasks,
            TexturePacker texturePacker, File srcDir, String namePrefix,
            final int group) throws IOException {
        int nextGroup = group + 1;
        File[] files = srcDir.listFiles();
        for (File file : files) {
            String filename = file.getName();
            if (namePrefix.length() > 0) {
                filename = namePrefix + "/" + filename;
            }

            if (file.isDirectory()) {
                nextGroup = traverse(buildFontTasks, texturePacker, file, filename, nextGroup);
            }
            else if (file.isFile()) {
                if (file.getName().toLowerCase().endsWith(".font.json")) {
                    buildFontTasks.add(new BuildFontTask(texturePacker, file, scale, nextGroup));
                    nextGroup++;
                }
                else {
                    String ext = "";
                    int i = filename.lastIndexOf('.');
                    if (i >= 0) {
                        ext = filename.substring(i);
                    }
                    if (ext.equalsIgnoreCase(".png") ||
                            ext.equalsIgnoreCase(".gif") ||
                            ext.equalsIgnoreCase(".bmp")) {
                        texturePacker.addToGroup(filename, ImageIO.read(file), group, scale);
                    }
                }
            }
        }
        return nextGroup;
    }

    private static class BuildFontTask implements Callable<Void> {

        private final TexturePacker texturePacker;
        private final File inFile;
        private final double scale;
        private final int group;

        public BuildFontTask(TexturePacker texturePacker, File inFile, double scale, int group) {
            this.texturePacker = texturePacker;
            this.inFile = inFile;
            this.scale = scale;
            this.group = group;
        }

        public Void call() throws Exception {
            try {
                texturePacker.addImageFont(ImageFontBuilder.build(inFile, scale), group);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw ex;
            }
            return null;
        }
    }
}

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
package org.pulpcore.tools.png.maven;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.pulpcore.tools.png.PNGEncoder;
import org.pulpcore.tools.png.PNGEncoderRunner;

/**
 @goal process-resources
 */
public class PNGEncoderMojo extends AbstractMojo {

    /**
     The source image file. Either the destFile or destDir must also be set.
     @parameter
     */
    private File srcFile;

    /**
     The source directory to recursively search for PNG and GIF files. The destDir must also
     be set.
     @parameter
     */
    private File srcDir;

    /**
     The destination file. The destFile may only be used in combination with the srcFile.
     @parameter
     */
    private File destFile;

    /**
     The destination directory. The destDir may be used with either the srcFile or srcDir.
     @parameter
     */
    private File destDir;

    /**
     The optimization level, from 0 to 5. The higher levels optimize better, but take
     more time to process. The default value is 2.
     @parameter default-value="2"
     */
    private int optimizationLevel = 2;

    /**
     A flag indicating whether to optimize when the PNG will only be displayed with pre-multiplied
     alpha. If true, the data is itself is stored without pre-multiplied alpha (according to the
     PNG spec) but will compress better is some circumstances. For example, if the background is
     transparent white, it is stored as transparent black - how it would appear
     with pre-multiplied alpha - which compresses better. Don't set this value to true for
     applications that need color data separate from alpha data. The default value is false.
     @parameter default-value="false"
     */
    private boolean optimizeForPremultipliedDisplay = false;

    /**
     A flag indicating whether the the console output should be silenced. Default is false.
     @parameter default-value="false"
     */
    private boolean quiet = false;

    @Override
    public void execute() throws MojoExecutionException {
        if (srcFile != null && srcDir != null) {
            throw new MojoExecutionException("Only a srcFile or srcDir may be specified, not both.");
        }
        if (srcFile == null && srcDir == null) {
            throw new MojoExecutionException("Neither the srcFile nor srcDir is not specified.");
        }
        if (destFile != null && destDir != null) {
            throw new MojoExecutionException("Only a destFile or destDir may be specified, not both.");
        }
        if (optimizationLevel < 0 || optimizationLevel > PNGEncoder.OPTIMIZATION_LEVEL_MAX) {
            throw new MojoExecutionException("Optimization level must be between 0 and " +
                PNGEncoder.OPTIMIZATION_LEVEL_MAX + ".");
        }
        PNGEncoderRunner runner = new PNGEncoderRunner();
        runner.setOptimizationLevel(optimizationLevel);
        runner.setOptimizeForPremultipliedDisplay(optimizeForPremultipliedDisplay);
        runner.setQuiet(quiet);

        if (srcFile != null) {
            if (destFile != null) {
                runner.addFile(srcFile, destFile);
            }
            else if (destDir != null) {
                destDir.mkdirs();
                runner.addFile(srcFile, new File(destDir, srcFile.getName()));
            }
            else {
                runner.addFile(srcFile);
            }
        }
        else {
            if (destFile != null) {
                throw new MojoExecutionException("For a srcDir, a destDir must be chosen.");
            }
            else if (destDir != null) {
                destDir.mkdirs();
                runner.addDirectory(srcDir, destDir);
            }
            else {
                runner.addDirectory(srcDir);
            }
        }

        runner.run();
    }
}
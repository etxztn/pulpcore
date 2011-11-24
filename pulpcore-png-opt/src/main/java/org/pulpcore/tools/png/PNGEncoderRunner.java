/*
    Copyright (c) 2011, Interactive Pulp, LLC
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
package org.pulpcore.tools.png;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
 TODO:
 By default, overwrite files (for the sake of the limited PNGReader in PulpCore). Add an option
 to not overwrite files if bigger.
 */

/**
 Performs PNG optimization on several files, and prints the results to the console.
 */
public class PNGEncoderRunner {

    private List<Task> conversionTasks = new ArrayList<Task>();
    private int optimizationLevel = PNGEncoder.OPTIMIZATION_LEVEL_DEFAULT;
    private boolean optimizeForPremultipliedDisplay = false;
    private boolean quiet = false;

    /**
     Adds a PNG encoding task to be performed when run() is invoked.
     @param image The image to convert.
     @param destFile The destination file.
     */
    public void addImage(BufferedImage image, File destFile) {
        conversionTasks.add(new Task(image, destFile));
    }

    /**
     Adds a PNG encoding task to be performed when run() is invoked.
     @param file The file to load and optimize.
     */
    public void addFile(File file) {
        File destFile = getDestFile(file, file.getParentFile());
        if (destFile == null) {
            destFile = file;
        }
        addFile(file, destFile);
    }

    /**
     Adds a PNG encoding task to be performed when run() is invoked.
     @param srcFile The source image file to load.
     @param destFile The destination image file to create.
     */
    public void addFile(File srcFile, File destFile) {
        conversionTasks.add(new Task(srcFile, destFile));
    }

    /**
     Adds a PNG encoding task for each PNG in the specified directory (recursively).
     The tasks are performed when run() is invoked.
     */
    public void addDirectory(File dir) {
        addDirectory(dir, dir);
    }

    /**
     Adds a PNG encoding task for each PNG in the specified directory (recursively) .
     The tasks are performed when run() is invoked.
     */
    public void addDirectory(File srcDir, File destDir) {
        traverse(conversionTasks, srcDir, destDir);
    }

    /**
     Sets the optimization level, from {@link PNGEncoder#OPTIMIZATION_OFF} 0 to
     {@link PNGEncoder#OPTIMIZATION_LEVEL_MAX} (5). The default value
     is {@link PNGEncoder#OPTIMIZATION_LEVEL_DEFAULT} (2). The higher levels optimize better, but take
     more time to process.
     */
    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }

    /**
     Sets whether to optimize when the PNG will only be displayed with pre-multiplied alpha.
     If true, the data is itself is stored without pre-multiplied alpha (according to the PNG spec)
     but will compress better is some circumstances. For example, if the background is
     transparent white, it is stored as transparent black, which it how it would appear
     with pre-multiplied alpha, and compresses better. The default value is false.
     */
    public void setOptimizeForPremultipliedDisplay(boolean optimizeForPremultipliedDisplay) {
        this.optimizeForPremultipliedDisplay = optimizeForPremultipliedDisplay;
    }

    /**
     Sets whether the the console output should be silenced. Default is false.
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     Runs the encoding tasks (in parallel) and clears the queue of tasks.
     */
    public void run() {

        long startTime = System.nanoTime();

        ExecutorService parent = Executors.newFixedThreadPool(2);
        int numProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService child = Executors.newFixedThreadPool(numProcessors);

        try {
            long newSize = 0;
            long originalSize = 0;

            // Sort tasks by largest first
            List<Task> tasks = conversionTasks;
            conversionTasks = new ArrayList<Task>();
            Collections.sort(tasks, new Comparator<Task>() {
                public int compare(Task o1, Task o2) {
                    if (o1.numPixels > o2.numPixels) {
                        return -1;
                    }
                    else if (o1.numPixels < o2.numPixels) {
                        return 1;
                    }
                    else {
                        return 0;
                    }
                }
            });

            // Setup executor service, get sum of all original file sizes.
            for (Task task : tasks) {
                task.setExecutorService(child);
                if (originalSize >= 0) {
                    long size = task.getSrcFileSize();
                    if (size < 0) {
                        originalSize = -1;
                    }
                    else {
                        originalSize += size;
                    }
                }
            }

            // Execute, wait, and get total size of all new files.
            List<Future<Long>> futures = null;
            try {
                futures = parent.invokeAll(tasks);
                for (Future<Long> future :futures) {
                    try {
                        newSize += future.get();
                    }
                    catch (ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            // Output results
            if (!quiet && tasks.size() > 1) {
                long durMillis = (System.nanoTime() - startTime) / 1000000;
                float dur = durMillis / 1000f;
                if (newSize > 0) {
                    System.out.println("Total size: " + getSizeDifference(originalSize, newSize));
                }
                System.out.println(dur + "s.");
            }
        }
        finally {
            // Cleanup
            child.shutdown();
            parent.shutdown();
        }
    }

    private String getSizeDifference(long originalSize, long newSize) {
        if (originalSize > 0) {
            double percentage = 100 * (double)newSize / originalSize;
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMinimumFractionDigits(1);
            numberFormat.setMaximumFractionDigits(1);
            long diff = newSize - originalSize;
            String diffString;
            if (diff > 0) {
                diffString = "+" + diff;
            }
            else {
                diffString = "" + diff;
            }
            return newSize + " bytes (" + diffString + " bytes change, " + numberFormat.format(percentage) + "% of original size.)";
        }
        else {
            return newSize + " bytes";
        }
    }

    private class Task implements Callable<Long> {
        private ExecutorService executorService;
        private final BufferedImage srcImage;
        private final File srcFile;
        private final File destFile;
        private final int numPixels;
        private final long srcFileSize;

        public Task(BufferedImage image, File destFile) {
            this.srcImage = image;
            this.srcFile = null;
            this.destFile = destFile;
            this.numPixels = image.getWidth() * image.getHeight();
            this.srcFileSize = -1;
        }

        public Task(File srcFile, File destFile) {
            this.srcImage = null;
            this.srcFile = srcFile;
            this.destFile = destFile;
            Dimension d = ImageUtil.getImageDimensions(srcFile);
            numPixels = d.width * d.height;
            // Get srcFileSize first in case srcFile is the same as destFile
            srcFileSize = srcFile.length();
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }

        public void setExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
        }

        public long getSrcFileSize() {
            return srcFileSize;
        }

        public Long call() throws IOException {

            // Encode
            long startTime = System.nanoTime();
            PNGEncoder encoder = new PNGEncoder(optimizationLevel, optimizeForPremultipliedDisplay);
            String imageDescription;
            if (srcImage != null) {
                imageDescription = encoder.encode(srcImage, destFile, executorService);
            }
            else {
                imageDescription = encoder.encode(srcFile, destFile, executorService);
            }

            long destFileSize = destFile.length();

            // Write log info (synchronized to prevent log mixups)
            if (!quiet) {
                synchronized (PNGEncoderRunner.class) {
                    System.out.println("Created: " + destFile);
                    
                    long durMillis = (System.nanoTime() - startTime) / 1000000;
                    float dur = durMillis / 1000f;
                    System.out.println("  " + imageDescription);
                    if (destFileSize > 0) {
                        System.out.println("  Size: " + getSizeDifference(srcFileSize, destFileSize));
                    }
                    System.out.println("  " + dur + "s.");
                }
            }
            return destFileSize;
        }
    }

    /**
     Recursively traverses a directory for images, and returns Tasks to convert those images
     to PNGs.
     @param srcDir The source directory.
     @param destDir The target directory.
    */
    private void traverse(List<Task> tasks, File srcDir, File destDir) {
        destDir.mkdirs();
        File[] files = srcDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                traverse(tasks, file, new File(destDir, file.getName()));
            }
            else {
                File destPNGFile = getDestFile(file, destDir);
                if (destPNGFile != null) {
                    tasks.add(new Task(file, destPNGFile));
                }
            }
        }
    }

    private File getDestFile(File srcFile, File destDir) {
        File destFile = null;
        if (srcFile.isFile()) {
            String ext = "";
            String lname = srcFile.getName().toLowerCase();
            int i = lname.lastIndexOf('.');
            if (i >= 0) {
                ext = lname.substring(i);
            }

            if (ext.equals(".png")) {
                destFile = new File(destDir, srcFile.getName());
            }
            else if (ext.equals(".gif") || ext.equals(".bmp")) {
                String n = srcFile.getName().substring(0, srcFile.getName().length() - ext.length());
                destFile = new File(destDir, n + ".png");
            }
        }
        return destFile;
    }
}

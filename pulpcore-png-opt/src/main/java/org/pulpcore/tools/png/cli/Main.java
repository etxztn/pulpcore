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
package org.pulpcore.tools.png.cli;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.pulpcore.tools.png.PNGEncoder;
import org.pulpcore.tools.png.PNGEncoderRunner;

public class Main {

    public static void main(String... args) {

        // Create options
        Options options = new Options();

        Option levelOption = new Option("o", true, "Optimization level, from 0 to " + 
                PNGEncoder.OPTIMIZATION_LEVEL_MAX + ". Default is " +
                PNGEncoder.OPTIMIZATION_LEVEL_DEFAULT + ".");
        levelOption.setArgName("level");

        options.addOption(levelOption);
        options.addOption("p", "premultalpha", false, "Enable optimization for pre-multiplied alpha. Default is off.");
        options.addOption("q", "quiet", false, "Quiet mode.");
        options.addOption("h", "help", false, "Print this message.");

        // Parse options
        CommandLineParser parser = new GnuParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        // Print help message
        if (cmd.hasOption("h") || cmd.getArgs().length < 1 || cmd.getArgs().length > 2) {
            printHelp(options);
            return;
        }

        // Setup encoder options
        PNGEncoderRunner runner = new PNGEncoderRunner();
        if (cmd.hasOption("p")) {
            runner.setOptimizeForPremultipliedDisplay(true);
        }
        if (cmd.hasOption("q")) {
            runner.setQuiet(true);
        }
        if (cmd.hasOption("o")) {
            String o = cmd.getOptionValue("o");
            int level = -1;
            try {
                level = Integer.parseInt(o);
            }
            catch (NumberFormatException ex) {
            }
            if (level < 0 || level > PNGEncoder.OPTIMIZATION_LEVEL_MAX) {
                System.err.println("Optimization level must be an integer between 0 and " +
                        PNGEncoder.OPTIMIZATION_LEVEL_MAX + ".");
                return;
            }
            runner.setOptimizationLevel(level);
        }

        // Setup encoder input/output
        File inFile = new File(cmd.getArgs()[0]);
        File outFile = cmd.getArgs().length > 1 ? new File(cmd.getArgs()[1]) : inFile;
        if (!inFile.exists()) {
            System.err.println("Input not found: " + inFile);
            return;
        }
        else if (inFile.isDirectory()) {
            if (outFile.isFile() || outFile.getName().toLowerCase().endsWith(".png")) {
                System.err.println("Cannot convert from directory " + inFile + " to file " + outFile);
                return;
            }
            runner.addDirectory(inFile, outFile);
        }
        else {
            File destFile;
            if (outFile.isDirectory()) {
                destFile = new File(outFile, inFile.getName());
            }
            else {
                destFile = outFile;
            }
            runner.addFile(inFile, destFile);
        }

        // Run!
        runner.run();
    }

    private static void printHelp(Options options) {
        System.out.println(Main.class.getPackage().getImplementationTitle() + " " +
                Main.class.getPackage().getImplementationVersion());
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar pulpcore-png-opt-1.0.jar [options] input [output]",
                "Creates optimized PNG files. The input can be a file or a directory. " +
                "If the input is a directory, the directory is recursively searched for PNG files. " +
                "If the output is not specified, the input is overwritten.",
                options,
                "Examples:\n" +
                "java -jar pulpcore-png-opt-1.0.jar file.png\n" +
                "java -jar pulpcore-png-opt-1.0.jar -o5 in/file.png out/file.png\n" +
                "java -jar pulpcore-png-opt-1.0.jar -p -q in out");
    }

    // Prevent instantiation
    private Main() {
    }
}

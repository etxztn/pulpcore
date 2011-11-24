/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime.applet;

/* This was in the main Runtime, but I took it out because I figured people don't want
Yet Another Log Class. */
public class Log {

    public enum Level {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        VERBOSE
    }

    private static long lastMemoryUsage = 0;

    // Prevent Instantiation
    private Log() { }

    public static void println(Level level, String message) {
        println(level, message, null);
    }

    public static void println(Level level, String message, Throwable t) {
        // TODO: Print to context?
        System.out.println(level + ": " + message);
        if (t != null) {
            t.printStackTrace();
        }
    }

    public static void e(String message) {
        println(Level.ERROR, message);
    }

    public static void e(String message, Throwable t) {
        println(Level.ERROR, message, t);
    }

    public static void w(String message) {
        println(Level.WARN, message);
    }

    public static void w(String message, Throwable t) {
        println(Level.WARN, message, t);
    }

    public static void i(String message) {
        println(Level.INFO, message);
    }

    public static void i(String message, Throwable t) {
        println(Level.INFO, message, t);
    }

    public static void d(String message) {
        println(Level.DEBUG, message);
    }

    public static void d(String message, Throwable t) {
        println(Level.DEBUG, message, t);
    }

    public static void v(String message) {
        println(Level.VERBOSE, message);
    }

    public static void v(String message, Throwable t) {
        println(Level.VERBOSE, message, t);
    }

    /**
        Prints the amount of current memory usage and the change in memory
        usage since the last call to this method.
    */
    public static void printMemory(String statement) {

        String label = "usage";
        long currMemoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long change = currMemoryUsage - lastMemoryUsage;

        if (Math.abs(change) < 2048) {
            // print memory change in bytes
            long valueDecimal = ((Math.abs(currMemoryUsage) * 10) >> 10) % 10;
            Log.i(statement + " (" +
                  (currMemoryUsage >> 10) + "." + valueDecimal + " KB " + label + ", " +
                  (change > 0 ? "+" : "") + change + " bytes change)");
        }
        else {
            // print memory change in kilobytes
            long valueDecimal = ((Math.abs(currMemoryUsage) * 10) >> 10) % 10;
            long changeDecimal = ((Math.abs(change) * 10) >> 10) % 10;
            Log.i(statement + " (" +
                  (currMemoryUsage >> 10) + "." + valueDecimal + " KB " + label + ", " +
                  (change > 0 ? "+" : "") +
                  (change >> 10) + "." + changeDecimal + " KB change)");
        }

        lastMemoryUsage = currMemoryUsage;
    }
}

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
package org.pulpcore.runtime.jre;

import org.pulpcore.runtime.RunLoop;

public class JreRunLoop extends RunLoop implements Runnable {

    private static final long ONE_SEC_IN_NANOS = 1000000000L;

    // Number of frames in a row with no sleep before lowering sleep granularity
    private static final int FRAMES_BEFORE_RESOLUTION_SWITCH = 4;

    // For nanoTime()
    private static final int NUM_NANO_TIMERS = 8;
    private static final long NANO_TIMER_MAX_DIFF = ONE_SEC_IN_NANOS;
    private static final long NANO_TIMER_NEVER_USED = -1;
    private static final long NANO_TIMER_FAIL_RESET_TIME = ONE_SEC_IN_NANOS;
    
    private static int nextThreadId = 0;

    private Thread thread;
    private float delay;
    private boolean firstRunOnThisThread;

    // For high-res timer enable/disable
    private final boolean isWindows;
    private Thread highResolutionThread;
    private int framesInARowNoSleep;

    // For nanoTime()
    private long[] nanoTimerLastTimeStamps = new long[NUM_NANO_TIMERS];
    private long[] nanoTimerTimeSinceLastUsed = new long[NUM_NANO_TIMERS];
    private long virtualNanoTime;
    private int timesInARowNewNanoTimerChosen;
    private long nanoTimerLastDiff;
    private long nanoTimerFailTime;
    private long nanoTimerFailResetTime;

    public JreRunLoop(JreContext context) {
        super(context);
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").startsWith("Windows");
        }
        catch (Exception ex) { }
        this.isWindows = windows;
    }

    @Override
    public void scheduleTick(float delay) {
        this.delay = Math.max(0, delay);
        if (thread == null) {
            firstRunOnThisThread = true;
            thread = new Thread(this, "PulpCore-RunLoop-" + (nextThreadId++));
            thread.start();
        }
    }

    @Override
    public float getCurrentTime() {
        return nanoTime() / (float)ONE_SEC_IN_NANOS;
    }

    @Override
    public void stop() {
        super.stop();
        thread = null;
        delay = -1;
    }

    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        while (thread == thisThread && delay >= 0) {
            if (firstRunOnThisThread) {
                firstRunOnThisThread = false;
                ((JreContext)getContext()).registerContextOnThisThread();
            }
            sleep(delay);
            delay = -1;
            tick();
        }
    }

    //
    //
    //


    private boolean isHighTimerResolutionEnabled() {
        return (highResolutionThread != null && highResolutionThread.isAlive());
    }

    private void setHighTimerResolutionEnabled(boolean enabledRequest) {
        if (enabledRequest && isWindows && !isHighTimerResolutionEnabled()) {
            // Improves the granularity of the sleep() function on Windows XP
            // Note: on some machines, time-of-day drift may occur if another thread hogs the
            // CPU
            // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435126
            highResolutionThread = new Thread("PulpCore-Win32Granularity") {
                @Override
                public void run() {
                    while (highResolutionThread == this && isRunning()) {
                        try {
                            Thread.sleep(Integer.MAX_VALUE);
                        }
                        catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                }
            };
            highResolutionThread.setDaemon(true);
            highResolutionThread.start();
        }
        else if (!enabledRequest && highResolutionThread != null) {
            Thread t = highResolutionThread;
            highResolutionThread = null;
            if (t.isAlive()) {
                try {
                    t.interrupt();
                }
                catch (Exception ex) {
                    // This happened a couple times at pulpgames.net
                    // java.security.AccessControlException: access denied (java.lang.RuntimePermission modifyThread)
                    // The isAlive() check wasn't there, so maybe the thread already died.
                    // Just assume the thread can't be stopped.
                    highResolutionThread = t;
                }
            }
        }
    }

    /*

    */
    protected void sleep(float delay) {
        boolean slept = false;
        float yieldLimit = isWindows ? 0.002f : 0.001f;
        float startTime = getCurrentTime();
        float goalTime = startTime + delay;
        float timerTime = startTime;
        while (goalTime - timerTime > 0) {
            if (goalTime - timerTime <= yieldLimit) {
                Thread.yield();
            }
            else {
                if (isWindows) {
                    // We need to sleep, so make sure the sleep granularity is high
                    setHighTimerResolutionEnabled(true);
                }
                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException ex) { }
            }
            // Set if we didn't return immediately on the first check
            slept = true;
            timerTime = getCurrentTime();
        }
        
        if (slept) {
            framesInARowNoSleep = 0;
        }
        else {
            framesInARowNoSleep++;

            // Maxing CPU
            if (framesInARowNoSleep >= FRAMES_BEFORE_RESOLUTION_SWITCH) {
                framesInARowNoSleep = 0;
                // Force a yield
                Thread.yield();
                if (isWindows) {
                    // Disable high sleep granularity - it can cause noticable time-of-day drift
                    // with max cpu.
                    setHighTimerResolutionEnabled(false);
                }
            }
        }

        //totalSleepTime += timerTime - startTime;
        //return timerTime;
    }

    private void resetNanoTime() {
        nanoTimerFailTime = 0;
        nanoTimerLastDiff = 0;
        timesInARowNewNanoTimerChosen = 0;
        for (int i = 0; i < NUM_NANO_TIMERS; i++) {
            nanoTimerTimeSinceLastUsed[i] = NANO_TIMER_NEVER_USED;
        }
    }

    private long nanoTime() {
        long diff;

        if (timesInARowNewNanoTimerChosen >= NUM_NANO_TIMERS) {
            long nanoTime = System.currentTimeMillis() * 1000000;
            diff = nanoTime - nanoTimerLastTimeStamps[0];

            nanoTimerFailTime += diff;
            if (nanoTimerFailTime >= nanoTimerFailResetTime) {
                // Maybe thrashing or system hibernation caused the problem - try again
                resetNanoTime();
                // But, increase the reset time
                nanoTimerFailResetTime *= 2;
            }
        }
        else {
            long nanoTime = System.nanoTime();

            // Find which timer the nanoTime value came from
            int bestTimer = -1;
            long bestDiff = 0;
            for (int i = 0; i < NUM_NANO_TIMERS; i++) {
                if (nanoTimerTimeSinceLastUsed[i] != NANO_TIMER_NEVER_USED) {
                    long t = nanoTimerLastTimeStamps[i] + nanoTimerTimeSinceLastUsed[i];
                    long timerDiff = nanoTime - t;
                    if (timerDiff > 0 && timerDiff < NANO_TIMER_MAX_DIFF) {
                        if (bestTimer == -1 || timerDiff < bestDiff) {
                            bestTimer = i;
                            bestDiff = timerDiff;
                        }
                    }
                }
            }

            // No best timer found
            if (bestTimer == -1) {
                // Use last good diff
                diff = nanoTimerLastDiff;

                // Find a new timer
                bestTimer = 0;
                for (int i = 0; i < NUM_NANO_TIMERS; i++) {
                    if (nanoTimerTimeSinceLastUsed[i] == NANO_TIMER_NEVER_USED) {
                        // This timer never used - use it
                        bestTimer = i;
                        break;
                    }
                    else if (nanoTimerTimeSinceLastUsed[i] > nanoTimerTimeSinceLastUsed[bestTimer]) {
                        // Least used timer so far, but keep looking
                        bestTimer = i;
                    }
                }
                timesInARowNewNanoTimerChosen++;
            }
            else {
                // Success!
                timesInARowNewNanoTimerChosen = 0;
                nanoTimerFailResetTime = NANO_TIMER_FAIL_RESET_TIME;
                diff = nanoTime - nanoTimerLastTimeStamps[bestTimer] - nanoTimerTimeSinceLastUsed[bestTimer];

                // Set lastDiff if this same timer used twice in a row
                if (nanoTimerTimeSinceLastUsed[bestTimer] == 0) {
                    nanoTimerLastDiff = diff;
                }
            }

            nanoTimerLastTimeStamps[bestTimer] = nanoTime;
            nanoTimerTimeSinceLastUsed[bestTimer] = 0;

            // Increment usage of all other timers
            for (int i = 0; i < NUM_NANO_TIMERS; i++) {
                if (i != bestTimer && nanoTimerTimeSinceLastUsed[i] != NANO_TIMER_NEVER_USED) {
                    nanoTimerTimeSinceLastUsed[i] += diff;
                }
            }

            // Check for total failure
            if (timesInARowNewNanoTimerChosen >= NUM_NANO_TIMERS) {
                nanoTimerLastTimeStamps[0] = System.currentTimeMillis() * 1000000;
            }
        }

        virtualNanoTime += diff;

        return virtualNanoTime;
    }
}

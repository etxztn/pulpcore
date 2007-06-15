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

package pulpcore;

import java.util.Stack;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.platform.AppContext;
import pulpcore.platform.ConsoleScene;
import pulpcore.platform.SceneSelector;
import pulpcore.platform.SoundEngine;
import pulpcore.platform.Surface;
import pulpcore.scene.Scene;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;

/**
    The Stage class manages Scenes and drawing to the Surface. The Stage class is a 
    singleton that Scenes can access using its static methods.
    <p>
    Stage runs the animation loop. The main class (e.g. CoreApplet) creates, 
    starts, and stops the Stage.
*/
public class Stage implements Runnable {
    
    private static final float SLOW_MOTION_SPEED = 1/4f;
    private static final float FAST_MOTION_SPEED = 4f;
    
    /** No limit to the frame rate (not recommended) */
    public static final int MAX_FPS = -1;
    /** 60 fps  (default) */
    public static final int HIGH_FPS = 60;
    /** 30 fps */
    public static final int MEDIUM_FPS = 30;
    /** 15 fps */
    public static final int LOW_FPS = 15;
    /** 60 fps  (default) */
    public static final int DEFAULT_FPS = HIGH_FPS;
    
    /** Perform no auto scaling (default) */
    public static final int AUTO_OFF = 0;
    
    /** Automatically center the Scene in the Stage. The Scene is not scaled. */
    public static final int AUTO_CENTER = 1;
    
    /** Automatically stretch the Scene to the Stage dimensions. */
    public static final int AUTO_STETCH = 2;
    
    /** 
        Automatically scale the Scene to the Stage dimensions, preserving the Scene's 
        aspect ratio. 
    */
    public static final int AUTO_FIT = 3;
    
    private AppContext appContext;
    private Input input;
    
    // Dirty rectangles
    private Rect[] dirtyRectangles;
    private int numDirtyRectangles;
    
    // Stage info
    private int desiredFPS = DEFAULT_FPS;
    private long remainderMicros;
    private Thread animationThread;
    private final Surface surface;
    
    // Scene management
    private Scene currentScene;
    private Scene nextScene;
    private Scene interruptScene;
    private int gotoInterruptedScene;
    private Stack interruptedScenes = new Stack();
    
    // Auto scaling
    private int naturalWidth = 1;
    private int naturalHeight = 1;
    private int autoScaleType = AUTO_OFF;
    private Transform defaultTransform = new Transform();
    
    // Frame rate display (debug only)
    private boolean showInfoOverlay;
    private ImageSprite infoOverlay;
    private long overlayCreationTime;
    private int overlayFrames;
    private long overlaySleepTime;
    private Activity memActivity;
    private Activity cpuActivity;
    
    // Slow motion mode (debug only)
    private float speed = 1;
    private float elapsedTimeRemainder;
    
    public Stage(Surface surface, AppContext appContext) {
        this.surface = surface;
        this.appContext = appContext;
        this.input = appContext.getInputSystem();
    }
    
    
    //
    // Static convenience methods
    //
    
    
    private static Stage getThisStage() {
        return CoreSystem.getThisAppContext().getStage();
    }
    
    
    /**
        @return The width of the surface.
    */
    public static int getWidth() {
        Stage instance = getThisStage();
        if (instance.autoScaleType == AUTO_OFF) {
            return instance.surface.getWidth();
        }
        else {
            return instance.naturalWidth;
        }
    }
    
    
    /**
        @return The height of the surface.
    */
    public static int getHeight() {
        Stage instance = getThisStage();
        if (instance.autoScaleType == AUTO_OFF) {
            return instance.surface.getHeight();
        }
        else {
            return instance.naturalHeight;
        }
    }
    
    
    /**
        @return A copy of the default transform used to draw onto the surface.
    */
    public static Transform getDefaultTransform() {
        Stage instance = getThisStage();
        return new Transform(instance.defaultTransform);
    }
    
    
    public static void setAutoScale(int naturalWidth, int naturalHeight) {
        setAutoScale(naturalWidth, naturalHeight, AUTO_FIT);
    }
    
    
    public static void setAutoScale(int naturalWidth, int naturalHeight, int autoScaleType) {
        Stage instance = getThisStage();
        instance.naturalWidth = naturalWidth;
        instance.naturalHeight = naturalHeight;
        instance.autoScaleType = autoScaleType;
        instance.setTransform();
    }
    
    
    /**
        Sets the desired frame rate in frames per second. The Stage will attempt
        to get as close to the desired frame rate as possible, but the actual 
        frame rate may vary.
        <p>
        To run at the highest frame rate possible (no pauses between frames),
        invoke <code>setFrameRate(Stage.MAX_FPS)</code>. Note, however, 
        running at the highest frame rate possible usually means
        as many processor cycles are used as possible.
    */
    public static void setFrameRate(int desiredFPS) {
        Stage instance = getThisStage();
        if (DEFAULT_FPS == -1) {
            return;
        }
        
        if (desiredFPS == instance.desiredFPS) {
            return;
        }
        else if (desiredFPS < 1) {
            desiredFPS = MAX_FPS;
        }
        
        instance.desiredFPS = desiredFPS;
        instance.remainderMicros = 0;
    }
    
    
    public static int getFrameRate() {
        return getThisStage().desiredFPS;
    }
    
    
    public static void setDirtyRectangles(Rect[] dirtyRectangles) {
        if (dirtyRectangles == null) {
            setDirtyRectangles(null, 0);
        }
        else {
            setDirtyRectangles(dirtyRectangles, dirtyRectangles.length);
        }
    }
        
        
    public static void setDirtyRectangles(Rect[] dirtyRectangles, int numDirtyRectangles) {
        Stage instance = getThisStage();
        
        if (dirtyRectangles == null) {
            numDirtyRectangles = 0;
        }
        
        instance.dirtyRectangles = dirtyRectangles;
        instance.numDirtyRectangles = numDirtyRectangles;
    }
    
    
    public static Sprite getInfoOverlay() {
        Stage instance = getThisStage();
        if (!instance.showInfoOverlay) {
            return null;
        }
        else {
            return instance.infoOverlay;
        }
    }
    
    
    /**
        Sets the next scene to display. This scene won't be displayed until
        after the current frame is displayed and the current scene is unloaded.
        <p>Multiple calls to this method within a single frame will cause the 
        first calls to be ignored - only the last call to this method during
        a single frame is recognized. Any interrupted scenes are unloaded
        when this scene is set as the current scene.
        @see #interruptScene(Scene)
    */
    public static void setScene(Scene scene) {
        Stage instance = getThisStage();
        instance.nextScene = scene;
    }
    
    
    public static Scene getScene() {
        return getThisStage().currentScene;
    }
    
    
    public static boolean isInterrupted() {
        Stage instance = getThisStage();
        return (instance.interruptScene != null || !instance.interruptedScenes.empty());
    }
    
    
    /**
        Notifies the engine to temproarily interrupt the current scene to
        show the specified scene instead. The interrupted scene is shown again 
        when {@link #gotoInterruptedScene()} is invoked. 
        <p>Multiple calls to this method within a single frame will cause the 
        first calls to be ignored - only the last call to this method during
        a single frame is recognized.
        <p>Once the interrupt takes place, the previous Scene is added
        to a stack, and the current scene can itself be interrupted.
    */
    public static void interruptScene(Scene scene) {
        Stage instance = getThisStage();
        instance.interruptScene = scene;
    }
    
    
    /**
        Sets the current scene to the previously interrupted scene. If
        there are no interrupted scenes, this method does nothing. This method
        can be called multiple times to travrse farther up the interrupted scene 
        stack.
    */
    public static void gotoInterruptedScene() {
        Stage instance = getThisStage();
        instance.gotoInterruptedScene++;
    }
    
    
    /**
        Removes all interrupted scenes from the stack. The unload() method is
        immediately invoked on all interrupted scenes.
    */
    public static void clearInterruptedScenes() {
        Stage instance = getThisStage();
        while (!instance.interruptedScenes.empty()) {
            Scene scene = (Scene)instance.interruptedScenes.pop();
            scene.unload();
        }
    }


    /**
        Gets a screenshot of the current appearance of the stage.
        @return a new image that contains the screenshot.
    */
    public static CoreImage getScreenshot() {
        Stage instance = getThisStage();
        CoreImage image = new CoreImage(instance.surface.getWidth(), instance.surface.getHeight());
        getScreenshot(image, 0, 0);
        return image;
    }
    
    
    /**
        Gets a screenshot at the specified location on the screen and copies
        it to the specified image.
    */
    public static void getScreenshot(CoreImage image, int x, int y) {
        Stage instance = getThisStage();
        instance.surface.getScreenshot(image, x, y);
    }
    
    
    //
    // Methods called from the main platform class
    //
    
    // Called from the AWT event thread
    public synchronized void start() {
        
        if (animationThread == null) {
            // Run animation at norm priority because the AWT Event thread 
            // needs to run at a higher priority. 
            animationThread = appContext.createThread("PulpCore-Stage", this);
            animationThread.start();
        }
    }
    
    
    // Called from the AWT event thread
    public synchronized void stop() {
        
        if (animationThread != null) {
            Thread oldAnimationThread = animationThread; 
            animationThread = null;
            try {
                oldAnimationThread.join(1000);
            }
            catch (InterruptedException ex) { }
        }
    }
    
    
    // Called from the AWT event thread
    public synchronized void destroy() {
        stop();
        
        
        final Scene scene = currentScene;
        if (scene != null) {
            currentScene = null;
            // Scene destruction needs to occur in the app context for this stage
            appContext.invokeAndWait("PulpCore-Stage-Destroy", 1000, new Runnable() {
                public void run() {
                    scene.unload();
                }
            });
        }
    }
        
    
    //
    // Animation thread
    //
    
    public void run() {
        
        Thread currentThread = Thread.currentThread();
        
        // Run in a loop - if animationLoop() throws an exception or returns, 
        // the app is "rebooted".
        while (animationThread == currentThread) {
            
            try {
                animationLoop();
            }
            catch (Throwable t) {
                
                // Reboot if not ThreadDeath
                currentScene = null;
                nextScene = null;
                interruptScene = null;
                gotoInterruptedScene = 0;
                interruptedScenes = new Stack();
                
                if (t instanceof ThreadDeath) {
                    // Don't reboot 
                    animationLoopStop();
                }
                else {
                    if (Build.DEBUG) {
                        CoreSystem.print("Animation loop error");
                        CoreSystem.setTalkBackField("pulpcore.uncaught-exception", t);
                        currentScene = new ConsoleScene();
                        currentScene.load();
                    }
                    else {
                        CoreSystem.setTalkBackField("pulpcore.uncaught-exception", t);
                        // Delay before rebooting
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ex) { }
                    }
                }
            }
        }
    }
    
    
    private synchronized void animationLoopStop() {
        if (animationThread == Thread.currentThread()) {
            animationThread = null;
        }
    }
        
    
    /**
        This method is called repeatedly from the run() method.
    */
    private void animationLoop() {
        
        Thread currentThread = Thread.currentThread();
        
        long lastTime = CoreSystem.getTimeMillis();
        int elapsedTime = 0;
        
        while (animationThread == currentThread) {
            
            // Check if the surface is ready to be drawn to
            if (!surface.isReady()) {
                if (Build.DEBUG) CoreSystem.print("Surface not ready.");
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex) { }
                lastTime = CoreSystem.getTimeMillis();
                elapsedTime = 0;
                continue;
            }
            
            // Check if a new Scene should be shown
            if (Build.DEBUG) {
                if (input.isControlDown() && input.isPressed(Input.KEY_C) && 
                    !(currentScene instanceof ConsoleScene)) 
                {
                    interruptScene(new ConsoleScene());
                }
                if (input.isControlDown() && input.isPressed(Input.KEY_X) && 
                    !(currentScene instanceof SceneSelector)) 
                {
                    interruptScene(new SceneSelector());
                }
            }
            if (hasNewScene()) {
                lastTime = CoreSystem.getTimeMillis();
                elapsedTime = 0;
            }
            if (currentScene == null) {
                animationLoopStop();
                return;
            }
            
            boolean oldFocus = input.hasKeyboardFocus();
            
            // Capture input
            input.poll();
            
            // Redraw if the focus changed
            boolean focusedChanged = input.hasKeyboardFocus() != oldFocus;
            boolean needsRedraw = focusedChanged;
            
            if (Build.DEBUG) {
                if (input.isControlDown() && input.isPressed(Input.KEY_I)) {
                    showInfoOverlay = !showInfoOverlay;
                    needsRedraw = true;
                }
                
                if (input.isControlDown() && input.isPressed(Input.KEY_1)) {
                    speed = SLOW_MOTION_SPEED;
                    elapsedTimeRemainder = 0;
                }
                if (input.isControlDown() && input.isPressed(Input.KEY_2)) {
                    speed = 1;
                    elapsedTimeRemainder = 0;
                }
                if (input.isControlDown() && input.isPressed(Input.KEY_3)) {
                    speed = FAST_MOTION_SPEED;
                    elapsedTimeRemainder = 0;
                }
            }
            
            // Reset dirty rectangles. Scene can set dirty rectangles in updateScene() or 
            // drawScene()
            numDirtyRectangles = -1;
            
            if (surface.contentsLost()) {
                setTransform();
            }
            if (needsRedraw || surface.contentsLost()) {
                currentScene.redrawNotify();
            }
            
            // Update Scene
            currentScene.updateScene(elapsedTime);
            
            // Draw Scene
            CoreGraphics g = surface.getGraphics();
            g.reset();
            
            // Don't set the default transform for a Scene2D - it already handles the transform.
            if (!(currentScene instanceof Scene2D)) {
                g.getTransform().concatenate(defaultTransform);
            }
            
            try {
                currentScene.drawScene(g);
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                // I had some anecdotal evidence (not proof) that the CoreGraphics system
                // can still throw some ArrayIndexOutOfBoundsExceptions in some cases.
                // If true, it is probably rare and difficult to track down.
                // So, for release mode, just ignore it - the user may see some visual artifacts
                // or they may not. In debug mode, show the error.
                if (Build.DEBUG) {
                    throw ex;
                }
                else {
                    // Release mode. Add it to the talkback and ignore.
                    appContext.setTalkBackField("pulpcore.platform.graphics.error", ex);
                }
            }                
                
            
            // Draw custom cursor
            /*
            CoreImage cursor = input.getCustomCursor();
            if (cursor != null) {
                cursor.update(elapsedTime);
                
                int x = input.getMouseX() - cursor.getHotspotX();
                int y = input.getMouseY() - cursor.getHotspotY();
                g.reset();
                g.getTransform().concatenate(defaultTransform);
                g.drawImage(cursor, x, y);
            }
            */
            
            // Draw frame rate and memory info (DEBUG only)
            if (Build.DEBUG) {
                if (showInfoOverlay && infoOverlay != null && numDirtyRectangles < 0) {
                    g.reset();
                    g.getTransform().concatenate(defaultTransform);
                    infoOverlay.draw(g);
                }
                doInfoSample();
            }
                
            // Show surface (blocks until surface is updated)
            if (surface.contentsLost() || numDirtyRectangles < 0) {
                surface.show();
            }
            else {
                surface.show(dirtyRectangles, numDirtyRectangles);
            }
            
            appContext.notifyFrameComplete();
            
            // Send pending sound data to sound system
            SoundEngine soundEngine = CoreSystem.getPlatform().getSoundEngine();
            if (soundEngine != null) {
                soundEngine.poll();
            }
            
            // Sleep to create correct frame rate
            long currTime;
            if (desiredFPS == MAX_FPS) {
                Thread.yield();
                currTime = CoreSystem.getTimeMillis();
            }
            else {
                long goalTimeMicros = 1000 * lastTime + 1000000L / desiredFPS + remainderMicros;
                long goalTime = goalTimeMicros / 1000;
                remainderMicros = goalTimeMicros - goalTime * 1000;
                
                // sleepUntilTimeMillis() is in better sync with the system time on some platforms.
                long priorToSleepTime = CoreSystem.getTimeMillis();
                currTime = CoreSystem.getPlatform().sleepUntilTimeMillis(goalTime);
                
                if (Build.DEBUG) {
                    overlaySleepTime += currTime - priorToSleepTime;
                }
            }
            
            // Update elapsed time
            elapsedTime = (int)(currTime - lastTime);
            lastTime = currTime;
            
            if (Build.DEBUG && speed != 1) {
                float e = elapsedTime * speed + elapsedTimeRemainder;
                elapsedTime = (int)e;
                elapsedTimeRemainder = e - elapsedTime;
            }
        }
    }
    
    
    private boolean hasNewScene() {
        
        boolean returnFromInterrupt = false;
        
        if (currentScene == null) {
            currentScene = CoreSystem.getThisAppContext().createFirstScene();
            if (currentScene == null) {
                if (Build.DEBUG) {
                    CoreSystem.print("Couldn't create first scene");
                    currentScene = new ConsoleScene();
                }
                else {
                    return false;
                }
            }
        }
        else if (gotoInterruptedScene > 0) {
            
            currentScene.hideNotify();
            
            if (gotoInterruptedScene > interruptedScenes.size()) {
                currentScene.unload();
                currentScene = null;
            }
            else {
                for (int i = 0; i < gotoInterruptedScene; i++) {
                    currentScene.unload();
                    currentScene = (Scene)interruptedScenes.pop();
                }
                returnFromInterrupt = true;
            }
            gotoInterruptedScene = 0;
            if (interruptScene != null) {
                interruptedScenes.push(currentScene);
                currentScene = interruptScene;
                interruptScene = null;
                returnFromInterrupt = false;
            }
        }
        else if (nextScene != null) {
            currentScene.hideNotify();
            currentScene.unload();
            
            clearInterruptedScenes();
            
            currentScene = nextScene;
            nextScene = null;
        }
        else if (interruptScene != null) {
            interruptedScenes.push(currentScene);
            currentScene.hideNotify();
            
            currentScene = interruptScene;
            interruptScene = null;
        }
        else {
            gotoInterruptedScene = 0;
            return false;
        }
        
        // Set defaults
        setFrameRate(DEFAULT_FPS);
        input.setCursor(Input.CURSOR_DEFAULT);
        input.setTextInputMode(false);
        
        // Perform a garbage collection if no sounds are playing
        // (A GC causes sound distortion on some systems)
        if (CoreSystem.getNumSoundsPlaying() == 0) {
            System.gc();
        }
        
        if (currentScene != null) {
            if (Build.DEBUG) {
                String sceneName = currentScene.getClass().getName();
                CoreSystem.printMemory("Stage: scene set to " + sceneName); 
            }
            if (!returnFromInterrupt) {
                currentScene.load();
            }
            currentScene.showNotify();
        }
        
        // Do an extra input poll clear any keypresses during the scene switch process.
        // (Note, the input is polled again after returning from this method)
        input.poll();
        
        return true;
    }
    
    
    private void setTransform() {
        defaultTransform.clear();
        
        switch (autoScaleType) {
            default: case AUTO_OFF:
                // Do nothing
                break;
                
            case AUTO_CENTER:
                defaultTransform.translate(
                    CoreMath.toFixed((surface.getWidth() - naturalWidth) / 2),
                    CoreMath.toFixed((surface.getHeight() - naturalHeight) / 2));
                break;
                
            case AUTO_STETCH:
                defaultTransform.scale(
                    CoreMath.toFixed(surface.getWidth()) / naturalWidth,
                    CoreMath.toFixed(surface.getHeight()) / naturalHeight);
                break;
                
            case AUTO_FIT:
                int a = naturalHeight * surface.getWidth();
                int b = naturalWidth * surface.getHeight();
                int newWidth;
                int newHeight;
                
                if (a > b) {
                    newHeight = surface.getHeight();
                    newWidth = newHeight * naturalWidth / naturalHeight;
                }
                else if (a < b) {
                    newWidth = surface.getWidth();
                    newHeight = newWidth * naturalHeight / naturalWidth;
                }
                else {
                    newWidth = surface.getWidth();
                    newHeight = surface.getHeight();
                }
                
                defaultTransform.translate(
                    CoreMath.toFixed((surface.getWidth() - newWidth) / 2),
                    CoreMath.toFixed((surface.getHeight() - newHeight) / 2));
                defaultTransform.scale(
                    CoreMath.toFixed(newWidth) / naturalWidth,
                    CoreMath.toFixed(newHeight) / naturalHeight);
                break;
        }
    }
    
    //
    // Debug mode only
    //
    
    private void doInfoSample() {
        
        // Sample every 500 milliseconds
        
        if (overlayCreationTime == 0) {
            overlayFrames = 0;
            overlayCreationTime = CoreSystem.getTimeMillis();
            return;
        }
        
        overlayFrames++;
        
        long time = CoreSystem.getTimeMillis() - overlayCreationTime;
        if (time < 500) {
            return;
        }
        
        // Take a sample of CPU, memory, and Scene info
        
        int fixedFPS = (int)(1000L*CoreMath.toFixed(overlayFrames) / time);
        int fixedSleepTime = (int)(CoreMath.toFixed(overlaySleepTime) / overlayFrames);
        int fixedCPU = CoreMath.ONE - (int)(CoreMath.toFixed(overlaySleepTime) / time);
        
        overlayFrames = 0;
        overlaySleepTime = 0;
        overlayCreationTime = CoreSystem.getTimeMillis();
        
        String fps = CoreMath.toString(fixedFPS, 1) + "fps";
        if (fixedSleepTime >= CoreMath.ONE_HALF) {
            fps += " (" + CoreMath.toString(fixedSleepTime, 0) + "ms sleep)";
        }
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long currentMemory = totalMemory - runtime.freeMemory();
        
        if (memActivity == null) {
            memActivity = new Activity();
            cpuActivity = new Activity();
            cpuActivity.setMax(CoreMath.ONE);
        }
        memActivity.setMax((int)(totalMemory / 1024));
        memActivity.addSample((int)(currentMemory / 1024));
        cpuActivity.addSample(fixedCPU);
        
        String memoryUsage = 
            ((float)((currentMemory * 10) >> 20) / 10) + " of " +
            ((float)((totalMemory * 10) >> 20) / 10) + " MB";
        
        String currentSceneName;
        if (currentScene == null) {
            currentSceneName = "null";
        }
        else {
            currentSceneName = currentScene.getClass().getName();
            
            String info = "";
            
            if (currentScene instanceof Scene2D) {
                Scene2D scene2D = (Scene2D)currentScene;
                info += scene2D.getNumVisibleSprites() + " of ";
                info += scene2D.getNumSprites() + " sprites";
                info += ", " + scene2D.getNumTimelines() + " timelines";
            }
            if (numDirtyRectangles >= 0) {
                if (info.length() > 0) {
                    info += ", ";
                }
                info += numDirtyRectangles + " dirty rects";
            }
            
            if (info.length() > 0) {
                currentSceneName += " (" + info + ")";
            }
        }
        
        if (!showInfoOverlay) {
            return;
        }
        
        // Draw it
        int lineHeight = CoreFont.getSystemFont().getHeight() + 2;
        int height = 32 + 5 + lineHeight;
        int activityX = 3;
        int activityY = lineHeight + 2;
        int activityWidth = 32;
        int activityTextX = activityWidth + 3;
        int activityTextY = activityY + 9;
        
        if (infoOverlay == null || infoOverlay.width.get() != getWidth()) {
            CoreImage image = new CoreImage(getWidth(), height); 
            infoOverlay = new ImageSprite(image, 0, 0);
        }
        CoreGraphics g = infoOverlay.getImage().createGraphics();
        g.setColor(0xffffff);
        g.fillRect(0, 0, getWidth(), height);
        g.drawString(currentSceneName, activityX, 2);
        
        cpuActivity.draw(g, activityX, activityY, 32);
        g.drawString(fps, activityX + activityTextX, activityTextY);
        
        activityX = getWidth() / 2;
        memActivity.draw(g, activityX, activityY, 32); 
        g.drawString(memoryUsage, activityX + activityTextX, activityTextY);
        
        infoOverlay.setDirty(true);
    }
    
    
    static class Activity {
        
        private static final int NUM_SAMPLES = 32;
        
        private int[] samples = new int[NUM_SAMPLES];
        private int index = 0;
        private int max = -1;
        
        public void addSample(int value) {
            samples[index] = value;
            index = (index + 1) % NUM_SAMPLES;
        }
        
        public int getMax() {
            if (this.max != -1) {
                return this.max;
            }
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < NUM_SAMPLES; i++) {
                max = Math.max(max, samples[(index + i) % NUM_SAMPLES]);
            }
            return max;
        }
        
        public void setMax(int max) {
            this.max = max;
        }
        
        public void draw(CoreGraphics g, int x, int y, int height) {
            int max = getMax();
            
            g.setColor(0x000000);
            g.fillRect(x, y, NUM_SAMPLES, height);
            g.setColor(0x25f816);
            
            // Newest sample on the right
            for (int i = 0; i < NUM_SAMPLES; i++) {
                int j = NUM_SAMPLES - i - 1;
                int sample = samples[(index + j) % NUM_SAMPLES];
                int sampleHeight = height * sample / max;
                g.fillRect(x + j, y + height - sampleHeight, 1, sampleHeight); 
            }
        }
    }
}

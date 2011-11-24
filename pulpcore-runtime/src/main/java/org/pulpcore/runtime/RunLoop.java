package org.pulpcore.runtime;

import java.util.ArrayList;
import org.pulpcore.view.Stage;

public abstract class RunLoop {

    private final Context context;
    private Stage stage;

    private float desiredFPS;
    private float dt;
    private boolean fixedStep;
    private float speed;

    private float currentTime;
    private float nextTime;
    private float accumulator;

    private int measuredFrames;
    private float measuredFramesStartTime;
    private float measuredFPS;
    
    private boolean isRunning;

    private final ArrayList<Runnable> runnables = new ArrayList<Runnable>();

    public RunLoop(Context context) {
        this.context = context;
        this.isRunning = false;
        setFrameRate(60);
        //setFixedStep(true);
        setSpeed(1);
    }

    public Context getContext() {
        return context;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isFixedStep() {
        return fixedStep;
    }

    public void setFixedStep(boolean fixedStep) {
        this.fixedStep = fixedStep;
    }

    public float getFrameRate() {
        return desiredFPS;
    }

    public void setFrameRate(float frameRate) {
        desiredFPS = frameRate;
        dt = 1f / desiredFPS;
    }

    public float getMeasuredFrameRate() {
        return measuredFPS;
    }

    /**
     Invokes a Runnable on the animation loop, before {@link #onTick(float) } is invoked.
     This PulpCore's only thread-safe method - no other methods are thread-safe.
     @param runnable
     */
    public void invokeLater(Runnable runnable) {
        if (runnable != null) {
            synchronized (runnables) {
                runnables.add(runnable);
            }
        }
    }

    /**
    Gets the current time, in seconds.
    @return 
    */
    protected abstract float getCurrentTime();

    /**
    Schedules a call to #tick() after a delay.
    @param delay The delay, in seconds.
    */
    protected abstract void scheduleTick(float delay);

    /**
    Stops the RunLoop.
    */
    public void stop() {
        isRunning = false;
    }

    /**
    Returns true if this RunLoop is currently running. 
    */
    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        currentTime = getCurrentTime();
        nextTime = currentTime;
        accumulator = 0;
        measuredFrames = 0;
        measuredFramesStartTime = currentTime;
        measuredFPS = 0;
        isRunning = true;
        scheduleTick(0);
    }

    protected void tick() {

        // The BufferedImageSurface synchronizes on the context so that OS repaints
        // (on the AWT thread) don't interrupt the run-loop
        synchronized (context) {
            float newTime = getCurrentTime();
            float deltaTime = newTime - currentTime;
            nextTime = Math.max(nextTime + dt, currentTime + dt);

            measure(newTime);

            if (deltaTime > 0.2f) {
                deltaTime = 0;
                accumulator = 0;
            }

            deltaTime *= speed;

            if (fixedStep) {
                accumulator += deltaTime;
                while (accumulator >= dt) {
                    tick(dt);
                    accumulator -= dt;
                    if (!isRunning()) {
                        return;
                    }
                }
            }
            else {
                tick(deltaTime);
                if (!isRunning()) {
                    return;
                }
            }
            render();
            currentTime = newTime;
            if (isRunning()) {
                scheduleTick(nextTime - getCurrentTime());
            }
        }
        
    }

    private void tick(float deltaTime) {
        if (runnables.size() > 0) {
            ArrayList<Runnable> runnablesToExecute;
            synchronized (runnables) {
                runnablesToExecute = new ArrayList<Runnable>(runnables);
                runnables.clear();
            }
            
            for (Runnable runnable : runnablesToExecute) {
                try {
                    runnable.run();
                }
                catch (Exception ex) {
                    // Print and ignore
                    ex.printStackTrace(System.out);
                }
            }
        }
        
        try {
            if (stage != null) {
                stage.tick(deltaTime);
            }
            if (context != null) {
                context.tick(deltaTime);
            }
        }
        catch (Exception ex) {
            // Print and ignore
            ex.printStackTrace(System.out);
        }
    }

    private void render() {
        try {
            if (stage != null) {
                stage.render();
            }
        }
        catch (Exception ex) {
            // Print and ignore
            ex.printStackTrace(System.out);
        }
    }

    private void measure(float currTime) {
        measuredFrames++;
        if (currTime >= measuredFramesStartTime + 0.5f) {
            measuredFPS = measuredFrames / (currTime - measuredFramesStartTime);
            measuredFrames = 0;
            measuredFramesStartTime = currTime;
            //System.out.println("FPS: " + measuredFPS);
        }
    }
}

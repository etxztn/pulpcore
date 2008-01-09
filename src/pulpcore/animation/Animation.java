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

package pulpcore.animation;
import pulpcore.math.CoreMath;

/**
    An Animation changes a value over a specific duration.
    <pre>
    |===================|
    0     duration     end
    
    ------- time ------->
    </pre>
    
    <p>Animations can delay before they start: 
    <pre>
    |--------------|===================|
    0  startDelay        duration     end
    
    ---------------- time --------------->
    </pre>
    
    <p>Animations can loop:
    <pre>
    |--------------|===================|===================|===================|
    0  startDelay        duration            duration           duration      end
    
    ----------------------------------- time ------------------------------------>
    </pre>
    
    <p>Animations can have a delay between loops:
    <pre>
    |--------------|================|------|================|------|================|
    0  startDelay       duration      loop      duration      loop      duration   end
                                      delay                   delay
    ------------------------------------- time ------------------------------------->
    </pre>
*/
public class Animation {
    
    public static final int LOOP_FOREVER = 0;
    
    protected static final int STATE_START_DELAY = 0;
    protected static final int STATE_ACTIVE = 1;
    protected static final int STATE_LOOP_DELAY = 2;
    
    protected final int fromValue;
    protected final int toValue;
    protected final Easing easing;
    private final int startDelay;
    
    protected int duration;
    private int numLoops;
    private int loopDelay;
      
    protected int elapsedTime;
    protected int value;
    
    
    public Animation(int fromValue, int toValue, int duration) {
        this(fromValue, toValue, duration, null, 0);
    }
    
    
    public Animation(int fromValue, int toValue, int duration, Easing easing) {
        this(fromValue, toValue, duration, easing, 0);
    }
    
    
    public Animation(int fromValue, int toValue, int duration, Easing easing, int startDelay) {
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.duration = duration;
        this.easing = easing;
        this.startDelay = startDelay;
        this.numLoops = 1;
        this.loopDelay = 0;
        
        elapsedTime = 0;
        if (duration == 0 && startDelay == 0) {
            value = toValue;
        }
        else {
            value = fromValue;
        }
    }
    
    
    /**
        Causes this animation to loop indefinitely. Same as calling loop(0);
    */
    public final void loopForever() {
        loop(LOOP_FOREVER, 0);
    }
    
    
    /**
        Causes this animation to loop indefinitely. Same as calling loop(0, loopDelay);
    */
    public final void loopForever(int loopDelay) {
        loop(LOOP_FOREVER, loopDelay);
    }
    
    
    /**
        Sets the number of loops to play. A value of 0 causes this timeline
        to play indefinitely.
    */
    public final void loop(int numLoops) {
        loop(numLoops, 0);
    }
    
    
    /**
        Sets the number of loops to play. A value of 0 causes this timeline
        to play indefinitely.
    */
    public final void loop(int numLoops, int loopDelay) {
        this.numLoops = numLoops;
        this.loopDelay = loopDelay;
    }
    
    
    public final int getStartDelay() {
        return startDelay;
    }
    
    
    public final int getDuration() {
        return duration;
    }
    
    
    public final int getNumLoops() {
        return numLoops;
    }
    
    
    public final int getLoopDelay() {
        return loopDelay;
    }
    
    
    public final int getTime() {
        return elapsedTime;
    }
    
    
    public final int getValue() {
        return value;
    }
    
    
    protected final void setValue(int value) {
        this.value = value;
    }
    
    
    /**
        Returns the total duration including the start delay and loops.
    */
    public final int getTotalDuration() {
        if (numLoops <= 0 || duration < 0) {
            return -1;
        }
        else {
            return startDelay + duration * numLoops + loopDelay * (numLoops - 1);
        }
    }
    
    
    public final int getRemainingTime() {
        int totalDuration = getTotalDuration();
        if (totalDuration < 0) {
            return -1;
        }
        else {
            return Math.max(0, totalDuration - elapsedTime);
        }
    }
    
    
    /**
        Returns true if this animation has not yet finished. 
    */
    public final boolean isAnimating() {
        int totalDuration = getTotalDuration();
        return (totalDuration < 0 || elapsedTime < totalDuration);
    }
    
    
    public final boolean isFinished() {
        return (getRemainingTime() == 0);
    }
    
    
    public final void rewind() {
        setTime(0);
    }
    
    
    /**
        If this animation is not looping, the animation is fast-forwarded to its end.
    */
    public final boolean fastForward() {
        int totalDuration = getTotalDuration();
        if (totalDuration < 0) {
            return false;
        }
        else {
            return setTime(totalDuration);
        }
    }
    
    
    protected final int getAnimTime(int elapsedTime) {
        int animTime = elapsedTime - startDelay;
        if (animTime >= 0 && duration > 0) {
            if (numLoops != 1) {
                animTime %= (duration + loopDelay);
            }
        }
        return animTime;
    }
    
    
    protected final int getAnimState(int animTime) {
        if (animTime < 0) {
            return STATE_START_DELAY;
        }
        else if (duration < 0 || animTime < duration) {
            return STATE_ACTIVE;
        }
        else {
            return STATE_LOOP_DELAY;
        }
    }
    

    //
    // These 4 methods may be overridden
    //


    /**
        Returns true if the value changed.
    */
    public boolean update(int elapsedTime) {
        return setTime(this.elapsedTime + elapsedTime);
    }
    

    protected boolean setTime(int newTime) {
        
        int totalDuration = getTotalDuration();
        
        if (newTime < 0) {
            newTime = 0;
        }
        else if (totalDuration != -1 && newTime > totalDuration) {
            newTime = totalDuration;
        }
        
        if (newTime == elapsedTime) {
            return false;
        }
        
        boolean valueUpdated = false;
        int animTime = getAnimTime(newTime);
        int animState = getAnimState(animTime);
        if (animState == STATE_ACTIVE) {
            valueUpdated = true;
            updateValue(animTime);
        }
        else {
            int prevAnimState = getAnimState(getAnimTime(elapsedTime));
            if (animState == STATE_LOOP_DELAY && prevAnimState != STATE_LOOP_DELAY) {
                valueUpdated = true;
                updateValue(duration);
            }
            else if (animState == STATE_START_DELAY && prevAnimState == STATE_ACTIVE) {
                valueUpdated = true;
                updateValue(duration);
            }
        }
        
        elapsedTime = newTime;
        return valueUpdated;
    }


    protected void updateValue(int animTime) {
        if (animTime >= duration) {
            value = toValue;
        }
        else if (animTime <= 0) {
            value = fromValue;
        }
        else {
            if (easing != null) {
                animTime = easing.ease(animTime, duration);
            }            
            value = calcValue(animTime);
        }
    }
    
    
    protected int calcValue(int animTime) {
        return fromValue + CoreMath.mulDiv(toValue - fromValue, animTime, duration);
    }
}
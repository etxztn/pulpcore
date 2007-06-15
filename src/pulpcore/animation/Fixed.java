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
    A Fixed is an fixed-point value (16 bits integer, 16 bits fraction)
    that can be animated over time. See {@link pulpcore.math.CoreMath} for
    methods to convert between integers and fixed-point numbers.
*/
public final class Fixed extends Property {

    private int fValue;
    
    
    public Fixed() {
        this(null, 0);
    }

    
    public Fixed(PropertyListener listener) {
        this(listener, 0);
    }
    
    
    //
    // Constructors with setters - 2 methods
    //
    
    
    public Fixed(int value) {
        this(null, value);
    }
    
    
    public Fixed(double value) {
        this(null, value);
    }
    
    
    //
    // Constructors with setters and listeners - 2 methods
    //
    
    
    public Fixed(PropertyListener listener, int value) {
        super(listener);
        this.fValue = CoreMath.toFixed(value);
    }
    
    
    public Fixed(PropertyListener listener, double value) {
        super(listener);
        this.fValue = CoreMath.toFixed(value);
    }
    
    
    //
    // Getters
    //
    

    public int getAsFixed() {
        return fValue;
    }
    
    public int getAsInt() {
        return CoreMath.toInt(fValue);
    }
    
    
    public int getAsIntFloor() {
        return CoreMath.toIntFloor(fValue);
    }
    
    
    public int getAsIntCeil() {
        return CoreMath.toIntCeil(fValue);
    }
    
    
    public int getAsIntRound() {
        return CoreMath.toIntRound(fValue);
    }
    
    
    public double get() {
        return CoreMath.toDouble(fValue);
    }
    
    
    public String toString() {
        return CoreMath.toString(fValue, 7);
    }
    
    
    //
    // Base set method
    //
    
    
    protected void setValue(int fValue) {
        if (this.fValue != fValue) {
            this.fValue = fValue;
            notifyListener();
        }
    }
    
    
    //
    // Setters - 3 methods
    //
    

    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void setAsFixed(int fValue) {
        setValue(fValue);
        this.anim = null;
    }    
    
    
    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void set(int value) {
        setAsFixed(CoreMath.toFixed(value));
    }
    
    
    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void set(double value) {
        setAsFixed(CoreMath.toFixed(value));
    }
    
    
    //
    // Setters (with a delay) - 3 methods
    //
    
    
    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void setAsFixed(int fValue, int delay) {
        animateToFixed(fValue, 0, null, delay);
    }
    

    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void set(int value, int delay) {
        animateTo(value, 0, null, delay);
    }
    
    
    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void set(double value, int delay) {
        animateTo(value, 0, null, delay);
    }
        
    
    //
    // Base animate method
    //
    
    
    public void animate(Animation anim) {
        this.anim = anim;
        if (anim.getStartDelay() == 0) {
            setValue(anim.getValue());
        }
    }
    
    
// CONVENIENCE METHODS - BELOW THIS LINE THAR BE DRAGONS 


    //
    // Animation convenience methods - fixed-point
    //
    
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration) {
        animate(new Animation(fFromValue, fToValue, duration));
    }
    
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration, Easing easing) {
        animate(new Animation(fFromValue, fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration, Easing easing,
        int startDelay) 
    {
        animate(new Animation(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration) {
        animate(new Animation(getAsFixed(), fToValue, duration));
    }
    
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration, Easing easing) {
        animate(new Animation(getAsFixed(), fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration, Easing easing, int startDelay) {
        animate(new Animation(getAsFixed(), fToValue, duration, easing, startDelay));
    }    


    //
    // Animation convenience methods - integer
    //
    
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        animate(new Animation(fFromValue, fToValue, duration));
    }
    
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        animate(new Animation(fFromValue, fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing, int startDelay) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration));
    }
    
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing, int startDelay) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration, easing, startDelay));
    }    


    //
    // Animation convenience methods - double
    //
    
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        animate(new Animation(fFromValue, fToValue, duration));
    }
    
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration, Easing easing) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        animate(new Animation(fFromValue, fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration, Easing easing, 
        int startDelay) 
    {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration));
    }
    
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration, Easing easing) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration, easing));
    }
    
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration, Easing easing, int startDelay) {
        int fToValue = CoreMath.toFixed(toValue); 
        animate(new Animation(getAsFixed(), fToValue, duration, easing, startDelay));
    }
}
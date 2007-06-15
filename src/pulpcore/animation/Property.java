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

/**
    The Property class is the base class for animating property values. 
*/
public abstract class Property {
    
    
    protected Animation anim;
    private PropertyListener listener;
    
    
    public Property() {
        this(null);
    }

    
    public Property(PropertyListener listener) {
        this.listener = listener;
    }
    
    
    protected abstract void setValue(int value);
    
    
    public PropertyListener getListener() {
        return listener;
    }
    
    
    public void setListener(PropertyListener listener) {
        this.listener = listener;
    }
    
    
    protected void notifyListener() {
        if (listener != null) {
            listener.propertyChange(this);
        }
    }
    
     
    /**
        Updates this Property, possibly modifying its value if it has an
        Animation attached. This method should be called once per frame.
        Sprites typically handle property updating.
    */
    public void update(int elapsedTime) {
        if (anim == null) {
            return;
        }
        
        boolean isActive = anim.update(elapsedTime);
        if (isActive) {
            setValue(anim.getValue());
        }
        
        if (anim.isFinished()) {
            anim = null;
        }
    }
    
    
    public boolean isAnimating() {
        return (anim != null && anim.isAnimating());
    }
    
    
    /**
        @param gracefully if true and the animation is not 
        looping, the animation is 
        fastforwarded to its end before it is stopped.
    */
    public void stopAnimation(boolean gracefully) {
        if (anim != null && gracefully) {
            anim.fastForward();
            setValue(anim.getValue());
        }
        anim = null;
    }
    
}
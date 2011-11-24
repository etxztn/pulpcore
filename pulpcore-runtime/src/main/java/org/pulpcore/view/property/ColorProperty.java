/*
    Copyright (c) 2009-2011, Interactive Pulp, LLC
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

package org.pulpcore.view.property;

/**
    An Color is a 32-bit ARGB value that can be animated over time.
*/
public class ColorProperty extends Property {

    private int value;
    
    public ColorProperty() {
        this(0, null);
    }
    
    public ColorProperty(PropertyListener listener) {
        this(0, listener);
    }
    
    public ColorProperty(int argbColor) {
        this(argbColor, null);
    }
    
    public ColorProperty(int argbColor, PropertyListener listener) {
        super(listener);
        set(argbColor);
    }

    /**
        Gets the packed, 32-bit ARGB value of this color.
    */
    public int get() {
        return value;
    }

    /**
        Sets the value of this ColorProperty.
    */
    public void set(int argbColor) {
        if (this.value != argbColor) {
            this.value = argbColor;
            notifyPropertyListeners();
        }
    }

    //
    // Binding
    //

    @Override
    public Property getBinding() {
        return super.getBinding();
    }

    @Override
    public void unbind() {
        super.unbind();
    }

    public void bindTo(ColorProperty p) {
        super.bindTo(p);
        set(p.get());
    }

    public void bindBidirectionallyTo(ColorProperty p) {
        super.bindBidirectionallyTo(p);
    }
    
    /**
        Returns true if the specified object is a
        {@code ColorProperty} or {@link java.lang.Integer} and its value is equal to this value.
    */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        else if (obj instanceof ColorProperty) {
            return get() == ((ColorProperty)obj).get();
        }
        else if (obj instanceof Integer) {
            return get() == ((Integer)obj).intValue();
        }
        else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return get();
    }

    @Override
    public String toString() {
        String s = Integer.toHexString(get());
        while (s.length() < 8) {
            s = '0' + s;
        }
        return "0x" + s;
    }
}
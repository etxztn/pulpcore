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
    A Floating point value that can be animated over time.
*/
public class FloatProperty extends Property {

    private float value;

    public FloatProperty() {
        this(0, null);
    }

    public FloatProperty(PropertyListener listener) {
        this(0, listener);
    }
    
    public FloatProperty(float value) {
        this(value, null);
    }
    
    public FloatProperty(float value, PropertyListener listener) {
        super(listener);
        set(value);
    }

    public float get() {
        return value;
    }

    /**
        Sets the value of this IntProperty.
    */
    public void set(float value) {
        if (this.value != value) {
            this.value = value;
            notifyPropertyListeners();
        }
    }

    public void set(double value) {
        set((float)value);
    }
    
    public void add(float delta) {
        set(get() + delta);
    }

    public void add(double delta) {
        set(get() + delta);
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

    public void bindTo(FloatProperty p) {
        super.bindTo(p);
        set(p.get());
    }

    public void bindTo(IntProperty p) {
        super.bindTo(p);
        set(p.get());
    }

    public void bindBidirectionallyTo(FloatProperty p) {
        super.bindBidirectionallyTo(p);
    }

    public void bindBidirectionallyTo(IntProperty p) {
        super.bindBidirectionallyTo(p);
    }

    //
    // equals/hashcode/toString
    //

    /**
        Returns true if the specified object is an 
        {@code IntProperty},
        {@link FloatProperty},
        {@link java.lang.Byte},
        {@link java.lang.Short},
        {@link java.lang.Integer},
        {@link java.lang.Long},
        {@link java.lang.Float}, or
        {@link java.lang.Double}, and
        its value is equal to this value.
    */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        else if (obj instanceof FloatProperty) {
            return get() == ((FloatProperty)obj).get();
        }
        else if (obj instanceof IntProperty) {
            return get() == ((IntProperty)obj).get();
        }
        else if (obj instanceof Double) {
            return get() == ((Double)obj).doubleValue();
        }
        else if (obj instanceof Float) {
            return get() == ((Float)obj).floatValue();
        }
        else if (
            obj instanceof Byte || 
            obj instanceof Short || 
            obj instanceof Integer || 
            obj instanceof Long) 
        {
            return get() == ((Number)obj).longValue();
        }
        else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return Float.floatToRawIntBits(get());
    }

    @Override
    public String toString() {
        return Float.toString(get());
    }
}
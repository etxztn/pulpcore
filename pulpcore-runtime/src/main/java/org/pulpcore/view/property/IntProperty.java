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
    An IntProperty is an integer value that can be animated over time.
*/
public class IntProperty extends Property {

    private int value;

    /**
        Constructs a new IntProperty object with no listener and the value of zero.
    */
    public IntProperty() {
        this(0, null);
    }
    
    /**
        Constructs a new IntProperty object with the specified listener and the value of zero.
        The listener is notified when the value is modified.
    */
    public IntProperty(PropertyListener listener) {
        this(0, listener);
    }
    
    /**
        Constructs a new IntProperty object with the specified value and no listener.
    */
    public IntProperty(int value) {
        this(value, null);
    }
    
    /**
        Constructs a new IntProperty object with the specified listener and value.
        The listener is notified when the value is modified.
    */
    public IntProperty(int value, PropertyListener listener) {
        super(listener);
        set(value);
    }
    
    public int get() {
        return value;
    }

    /**
        Sets the value of this IntProperty.
    */
    public void set(int value) {
        if (this.value != value) {
            this.value = value;
            notifyPropertyListeners();
        }
    }
    
    public void add(int delta) {
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
        set((int)p.get());
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
        else if (obj instanceof IntProperty) {
            return get() == ((IntProperty)obj).get();
        }
        else if (obj instanceof FloatProperty) {
            return get() == ((FloatProperty)obj).get();
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
        return get();
    }

    @Override
    public String toString() {
        return Integer.toString(get());
    }
}
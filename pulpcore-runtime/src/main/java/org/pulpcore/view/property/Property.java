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

import java.util.Collections;
import java.util.List;

/**
    The Property class is the base class for animating values. Properties have a value, and
    optionally, listeners to alert when the value changes.
*/
public abstract class Property {
    
    private PropertyListener listener;

    public Property() {
        this(null);
    }
    
    /**
        Creates a property with the specified listener. The listener may be
        {@code null}.
    */
    public Property(PropertyListener listener) {
        this.listener = listener;
    }

    //
    // Listeners
    //

    public void notifyPropertyListeners() {
        if (listener != null) {
            listener.onPropertyChange(this);
        }
    }

    /**
        Returns a copy of the list of all the listeners registered on this Property.
        @return all of this Property's {@link PropertyListener}s or an empty list if no
        listeners are registered.
    */
    public List<PropertyListener> getListeners() {
        if (listener == null) {
            return Collections.emptyList();
        }
        else if (listener instanceof MultiListener) {
            return ((MultiListener)listener).getListeners();
        }
        else {
            return Collections.singletonList(listener);
        }
    }

    /**
        Adds the specified listener to receive events from this Property. If the listener is
        {@code null}, no exception is thrown and no action is performed.
        @param listener The listener to add.
    */
    public void addListener(PropertyListener listener) {
        if (listener == null || this.listener == listener) {
            // Do nothing
        }
        else if (this.listener == null) {
            this.listener = listener;
        }
        else if (listener instanceof MultiListener) {
            ((MultiListener)this.listener).addListener(listener);
        }
        else {
            this.listener = new MultiListener(this.listener, listener);
        }
    }

    /**
        Removes the specified listener so that it no longer receives events from this Property.
        This method performs no function, nor does it throw an exception, if the listener specified
        by the argument was not previously added to this Property. If the listener is {@code null},
        no exception is thrown and no action is performed.
        @param listener The listener to remove.
    */
    public void removeListener(PropertyListener listener) {
        if (this.listener == listener) {
            this.listener = null;
        }
        else if (listener instanceof MultiListener) {
            MultiListener ml = (MultiListener)this.listener;
            ml.removeListener(listener);
            unrollMultilistener();
        }
    }

    // Use a regular Listener instead of a MultiListener, if possible
    private void unrollMultilistener() {
        if (listener instanceof MultiListener) {
            MultiListener ml = (MultiListener)listener;
            if (!ml.hasMetadata()) {
                if (ml.size() == 1) {
                    listener = ml.get(0);
                }
                else if (ml.size() == 0) {
                    listener = null;
                }
            }
        }
    }
    
    //
    // Binding.
    // These methods are package-private and exposed in subclasses that support binding.
    // To work correctly, they require the package-private Binding class.
    // Subclasses may support no binding, or binding to specific types only (float to int, but not
    // float to color, for example)
    //

    Property getBinding() {
        if (listener instanceof MultiListener) {
            return ((MultiListener)listener).boundTo;
        }
        else {
            return null;
        }
    }

    void bindTo(Property src) {
        unbind();
        MultiListener ml;
        if (listener instanceof MultiListener) {
            ml = (MultiListener)listener;
        }
        else {
            ml = new MultiListener(listener);
        }
        ml.boundTo = src;
        src.addListener(new Binding(src, this));
    }

    void unbind() {
        if (listener instanceof MultiListener) {
            MultiListener ml = (MultiListener)listener;
            if (ml.boundTo != null) {
                if (ml.boundTo.listener instanceof MultiListener) {
                    ((MultiListener)ml.boundTo.listener).removeBindingTo(this);
                    ml.boundTo.unrollMultilistener();
                }
                ml.boundTo = null;
            }
            unrollMultilistener();
        }
    }

    void bindBidirectionallyTo(Property p) {
        p.bindTo(this);
        this.bindTo(p);
    }

    //
    // equals/hashcode/toString
    //

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
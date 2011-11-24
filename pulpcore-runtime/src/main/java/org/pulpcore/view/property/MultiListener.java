/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
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

import java.util.ArrayList;
import java.util.List;

/**
 Internal class used to implement Properties with multiple listeners.
 Also, this class stores binding info, if any.
 This class is separate from the Property class because most Properties won't use these features,
 and this implementation will reduce memory use for each View.
*/
class MultiListener implements PropertyListener {
    
    private final ArrayList<PropertyListener> listeners;

    Property boundTo;

    MultiListener(PropertyListener p) {
        listeners = new ArrayList<PropertyListener>(2);
        if (p != null) {
            listeners.add(p);
        }
    }
    
    MultiListener(PropertyListener a, PropertyListener b) {
        listeners = new ArrayList<PropertyListener>(2);
        listeners.add(a);
        listeners.add(b);
    }
    
    List<PropertyListener> getListeners() {
        return new ArrayList<PropertyListener>(listeners);
    }
    
    int size() {
        return listeners.size();
    }

    boolean hasMetadata() {
        return boundTo != null;
    }
    
    PropertyListener get(int index) {
        return listeners.get(index);
    }
    
    void addListener(PropertyListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    void removeListener(PropertyListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onPropertyChange(Property property) {
        // Make a copy in case any of the listeners remove themselves
        for (PropertyListener listener : getListeners()) {
            listener.onPropertyChange(property);
        }
    }

    void removeBindingTo(Property dest) {
        for (PropertyListener listener : listeners) {
            if (listener instanceof Binding && ((Binding)listener).dest == dest) {
                removeListener(listener);
                return;
            }
        }
    }
}
  

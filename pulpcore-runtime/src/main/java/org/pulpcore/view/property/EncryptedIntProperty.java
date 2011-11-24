/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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

import org.pulpcore.util.ARC4;

/**
    An EncryptedIntProperty is an {@link IntProperty } whose value is internally encrypted. This
    class is useful for protecting against runtime memory modifications from tools like 
    Cheat Engine. For games, typical encrypted values might be the score and the game level.
*/
public class EncryptedIntProperty extends IntProperty {
    
    private final ARC4 cipher = new ARC4();
    private final byte[] buffer = new byte[4];

    /**
        Constructs a new IntProperty object with no listener and the value of zero.
    */
    public EncryptedIntProperty() {
        this(0, null);
    }

    /**
        Constructs a new IntProperty object with the specified listener and the value of zero.
        The listener is notified when the value is modified.
    */
    public EncryptedIntProperty(PropertyListener listener) {
        this(0, listener);
    }

    /**
        Constructs a new IntProperty object with the specified value and no listener.
    */
    public EncryptedIntProperty(int value) {
        this(value, null);
    }

    /**
        Constructs a new IntProperty object with the specified listener and value.
        The listener is notified when the value is modified.
    */
    public EncryptedIntProperty(int value, PropertyListener listener) {
        super(listener);
        set(value);
    }
   
    @Override
    public int get() {
        return crypt(super.get());
    }

    @Override
    public void set(int value) {
        super.set(crypt(value));
    }
    
    private int crypt(int v) {
        buffer[0] = (byte)(v >>> 24);
        buffer[1] = (byte)((v >> 16) & 0xff);
        buffer[2] = (byte)((v >> 8) & 0xff);
        buffer[3] = (byte)(v & 0xff);

        cipher.crypt(buffer);

        return ((buffer[0] & 0xff) << 24) |
                ((buffer[1] & 0xff) << 16) |
                ((buffer[2] & 0xff) << 8) |
                (buffer[3] & 0xff);
    }
}

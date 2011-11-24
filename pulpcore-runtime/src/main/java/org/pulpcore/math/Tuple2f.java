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

package org.pulpcore.math;

/**
    An integer pair that can be used as a point, vector, dimension, complex, etc.
*/
public class Tuple2f {

    public float x;
    public float y;
    
    public Tuple2f() {
        set(0, 0);
    }
    
    public Tuple2f(float x, float y) {
        set(x, y);
    }
    
    public Tuple2f(Tuple2f t) {
        set(t);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple2f t = (Tuple2f)obj;
        return x == t.x && y == t.y;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Float.floatToIntBits(this.x);
        hash = 17 * hash + Float.floatToIntBits(this.y);
        return hash;
    }

    public void set(Tuple2f t) {
        this.x = t.x;
        this.y = t.y;
    }
    
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void sub(float x, float y) {
        add(-x, -y);
    }

    public void add(Tuple2f t) {
        add(t.x, t.y);
    }

    public void sub(Tuple2f t) {
        add(-t.x, -t.y);
    }

    /**
        Interpreting this Tuple2f as a vector, return the square of length.
    */
    public float lengthSquared() {
        return Math.max(0, x * x + y * y);
    }
    
    /**
        Interpreting this Tuple2f as a vector, return its length.
    */
    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }

    /**
        Interpreting this Tuple2f and the specified Tuple2f as vectors,
        return the dot product of the two vectors.
    */
    public float dot(Tuple2f t) {
        return x * t.x + y * t.y;
    }
}
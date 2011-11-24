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
    A Rectangle with integer coordinates.
*/
public class Rectf {
    
    public float x;
    public float y;
    public float width;
    public float height;
    
    public Rectf() {
        setBounds(0, 0, 0, 0);
    }
    
    public Rectf(float x, float y, float width, float height) {
        setBounds(x, y, width, height);
    }
    
    public Rectf(Rectf r) {
        setBounds(r.x, r.y, r.width, r.height);
    }
    
    public void setBounds(Rectf r) {
        setBounds(r.x, r.y, r.width, r.height);
    }
    
    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rectf r = (Rectf) obj;
        return equals(r.x, r.y, r.width, r.height);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Float.floatToIntBits(this.x);
        hash = 23 * hash + Float.floatToIntBits(this.y);
        hash = 23 * hash + Float.floatToIntBits(this.width);
        hash = 23 * hash + Float.floatToIntBits(this.height);
        return hash;
    }

    @Override
    public String toString() {
        return "Rectf: " + x + "," + y + " " + width + "x" + height;
    }

    public boolean equals(float x, float y, float width, float height) {
        return (
            this.x == x && 
            this.y == y && 
            this.width == width && 
            this.height == height);
    }
    
    public float getArea() {
        return width * height;
    }
    
    public boolean contains(float x, float y) {
        return (
            x >= this.x && 
            y >= this.y && 
            x < this.x + this.width && 
            y < this.y + this.height);
    }
    
    public boolean contains(float x, float y, float width, float height) {
        return (
            x >= this.x && 
            y >= this.y && 
            x + width <= this.x + this.width && 
            y + height <= this.y + this.height);
    }
    
    public boolean contains(Rectf r) {
        return contains(r.x, r.y, r.width, r.height);
    }
    
    public boolean intersects(Rectf r) {
        return intersects(r.x, r.y, r.width, r.height);
    }
    
    public boolean intersects(float x, float y, float width, float height) {
        return 
            x + width > this.x && 
            x < this.x + this.width &&
            y + height > this.y && 
            y < this.y + this.height;
    }
    
    /**
        Sets this rectangle to the intersection of this rectangle and the specified
        rectangle. If the rectangles don't intersect, the width and height
        will be zero.
    */
    public void intersection(Rectf r) {
        intersection(r.x, r.y, r.width, r.height);
    }
    
    /**
        Sets this rectangle to the intersection of this rectangle and the specified
        rectangle. If the rectangles don't intersect, the width and/or height
        will be zero.
    */
    public void intersection(float x, float y, float width, float height) {
        float x1 = Math.max(this.x, x);
        float y1 = Math.max(this.y, y);
        float x2 = Math.min(this.x + this.width, x + width);
        float y2 = Math.min(this.y + this.height, y + height);
        setBounds(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }
    
    /**
        Sets this rectangle to the union of this rectangle and the specified
        rectangle.
    */
    public void union(Rectf r) {
        union(r.x, r.y, r.width, r.height);
    }
    
    /**
        Sets this rectangle to the union of this rectangle and the specified
        rectangle.
    */
    public void union(float x, float y, float width, float height) {
        float x1 = Math.min(this.x, x);
        float y1 = Math.min(this.y, y);
        float x2 = Math.max(this.x + this.width, x + width);
        float y2 = Math.max(this.y + this.height, y + height);
        setBounds(x1, y1, x2 - x1, y2 - y1);
    }

}
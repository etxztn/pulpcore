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
    The Transform class represents a 2D affine transform. 
*/
public class AffineTransform {
    
    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATE = 1 << 0;
    public static final int TYPE_SCALE = 1 << 1;
    public static final int TYPE_ROTATE = 1 << 2;
    
    private int type;
    private float m00, m01, m02;
    private float m10, m11, m12;
    
    public AffineTransform() {
        clear();
    }
    
    public AffineTransform(AffineTransform transform) {
        set(transform);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AffineTransform t = (AffineTransform) obj;
            return (
                m00 == t.m00 &&
                m01 == t.m01 &&
                m02 == t.m02 &&
                m10 == t.m10 &&
                m11 == t.m11 &&
                m12 == t.m12);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Float.floatToIntBits(this.m00);
        hash = 17 * hash + Float.floatToIntBits(this.m01);
        hash = 17 * hash + Float.floatToIntBits(this.m02);
        hash = 17 * hash + Float.floatToIntBits(this.m10);
        hash = 17 * hash + Float.floatToIntBits(this.m11);
        hash = 17 * hash + Float.floatToIntBits(this.m12);
        return hash;
    }
    
    public float transformX(float x, float y) {
        // [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
        // [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
        // [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
        
        return m00 * x + m01 * y + m02;
    }
    
    public float transformY(float x, float y) {
        // [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
        // [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
        // [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]

        return m10 * x + m11 * y + m12;
    }
    
    public void transform(Tuple2f t) {
        t.set(transformX(t.x, t.y), transformY(t.x, t.y));
    }
    
    /**
        Returns NaN if this transform can't be inverted.
    */
    public float inverseTransformX(float x, float y) {
        
        x -= m02;
        y -= m12;
        
        if ((type & AffineTransform.TYPE_ROTATE) != 0) {
            float det = getDeterminant();
            if (det == 0) {
                return Float.NaN;
            }
            return (x * m11 - y * m01) / det;
        }
        else if ((type & AffineTransform.TYPE_SCALE) != 0) {
            if (m00 == 0 || m11 == 0) {
                return Float.NaN;
            }
            return x / m00;
        }
        else {
            return x;
        }
    }
    
    /**
        Returns NaN if this transform can't be inverted.
    */
    public float inverseTransformY(float x, float y) {
        
        x -= m02;
        y -= m12;
        
        if ((type & AffineTransform.TYPE_ROTATE) != 0) {
            float det = getDeterminant();
            if (det == 0) {
                return Float.NaN;
            }
            return (y * m00 - x * m10) / det;
        }
        else if ((type & AffineTransform.TYPE_SCALE) != 0) {
            if (m00 == 0 || m11 == 0) {
                return Float.NaN;
            }
            return y / m11;
        }
        else {
            return y;
        }
    }
    
    /**
        @return true on success; false if this transform can't be inverted.
    */
    public boolean inverseTransform(Tuple2f t) {
        float tx = inverseTransformX(t.x, t.y);
        float ty = inverseTransformY(t.x, t.y);
        if (tx == Float.NaN || ty == Float.NaN) {
            return false;
        }
        t.set(tx, ty);
        return true;
    }
    
    /**
        Gets the integer bounds.
        @return true if the bounds instance was changed
    */
    public boolean getBounds(float contentWidth, float contentHeight, Recti bounds) {
        float x1 = getTranslateX();
        float y1 = getTranslateY();
        float x2 = getScaleX() * contentWidth;
        float y2 = getShearY() * contentWidth;
        float x3 = getShearX() * contentHeight;
        float y3 = getScaleY() * contentHeight;
        float x4 = x1 + x2 + x3;
        float y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        float boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        float boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        float boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        float boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        int boundsX = (int)Math.floor(boundsX1);
        int boundsY = (int)Math.floor(boundsY1);
        int boundsW = (int)Math.ceil(boundsX2) - boundsX;
        int boundsH = (int)Math.ceil(boundsY2) - boundsY;
        
        if (!bounds.equals(boundsX, boundsY, boundsW, boundsH)) {
            bounds.setBounds(boundsX, boundsY, boundsW, boundsH);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
        Gets the bounds.
    */
    public Rectf getBounds(float contentWidth, float contentHeight) {
        float x1 = getTranslateX();
        float y1 = getTranslateY();
        float x2 = getScaleX() * contentWidth;
        float y2 = getShearY() * contentWidth;
        float x3 = getShearX() * contentHeight;
        float y3 = getScaleY() * contentHeight;
        float x4 = x1 + x2 + x3;
        float y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        float boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        float boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        float boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        float boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        float boundsW = boundsX2 - boundsX1;
        float boundsH = boundsY2 - boundsY1;
        
        return new Rectf(boundsX1, boundsY1, boundsW, boundsH);
    }
    
    public int getType() {
        return type;
    }
    
    public float getTranslateX() {
        return m02;
    }
    
    public float getTranslateY() {
        return m12;
    }
    
    public float getScaleX() {
        return m00;
    }
    
    public float getScaleY() {
        return m11;
    }
    
    public float getShearX() {
        return m01;
    }
    
    public float getShearY() {
        return m10;
    }
    
    public float getDeterminant() {
        return m00 * m11 - m01 * m10;
    }
    
    //
    // Matrix modifications
    //
    
    /**
        Clears this transform, i.e., sets this transform to the identity matrix.  
    */
    public void clear() {
        m00 = 1;
        m01 = 0;
        m02 = 0;
        
        m10 = 0;
        m11 = 1;
        m12 = 0;
        
        type = TYPE_IDENTITY;
    }
    
    /**
        Sets this transform to a copy of specified transform.
    */
    public void set(AffineTransform transform) {
        if (transform == this) {
            return;
        }
        else if (transform == null) {
            clear();
        }
        else {
            this.m00 = transform.m00;
            this.m01 = transform.m01;
            this.m02 = transform.m02;
            this.m10 = transform.m10;
            this.m11 = transform.m11;
            this.m12 = transform.m12;
            this.type = transform.type;
        }
    }
    
    public void concatenate(AffineTransform transform) {
        mult(this, transform, this);
    }
    
    public void preConcatenate(AffineTransform transform) {
        mult(transform, this, this);
    }
    
    private static void mult(AffineTransform a, AffineTransform b, AffineTransform result) {
        
        // Assume either a or b could be the same instance as result
        
        if (a.type == TYPE_IDENTITY) {
            result.set(b);
        }
        else if (b.type == TYPE_IDENTITY) {
            result.set(a);
        }
        else if (b.type == TYPE_TRANSLATE) {
            float x = b.m02;
            float y = b.m12;
            result.set(a);
            result.translate(x, y);
        }
        else if (b.type == TYPE_SCALE) {
            float x = b.m00;
            float y = b.m11;
            result.set(a);
            result.scale(x, y);
        }
        else if (b.type == TYPE_ROTATE) {
            float x = b.m00;
            float y = b.m01;
            result.set(a);
            result.rotate(x, y);
        }
        else {
            float c00 = a.m00*b.m00 + a.m01*b.m10;
            float c01 = a.m00*b.m01 + a.m01*b.m11;
            float c02 = a.m00*b.m02 + a.m01*b.m12 + a.m02;
            float c10 = a.m10*b.m00 + a.m11*b.m10;
            float c11 = a.m10*b.m01 + a.m11*b.m11;
            float c12 = a.m10*b.m02 + a.m11*b.m12 + a.m12;
            
            result.m00 = c00;
            result.m01 = c01;
            result.m02 = c02;
            result.m10 = c10;
            result.m11 = c11;
            result.m12 = c12;
            result.type = a.type | b.type;
        }
    }
    
    public void translate(float x, float y) {
        // [   1   0   x   ]
        // [   0   1   y   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m02 += x;
            m12 += y;
        }
        else {
            m02 += m00 * x;
            m12 += m11 * y;
            
            if ((type & TYPE_ROTATE) != 0) {
                m02 += m01 * y;
                m12 += m10 * x;
            }
        }
        
        type |= TYPE_TRANSLATE;
    }
    
    public void roundTranslation() {
        m02 = Math.round(m02);
        m12 = Math.round(m12);
    }
    
    public void scale(float x, float y) {
        // [   x   0   0   ]
        // [   0   y   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m00 = x;
            m11 = y;
        }
        else {
            m00 *= x;
            m11 *= y;
            
            if ((type & TYPE_ROTATE) != 0) {
                m01 *= y;
                m10 *= x;
            }
        }
        
        type |= TYPE_SCALE;
    }
    
    public void rotate(float angle) {
        rotate((float)Math.cos(angle), (float)Math.sin(angle));
    }
    
    public void rotate(float cosAngle, float sinAngle) {
        // [   x  -y   0   ]
        // [   y   x   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m00 = cosAngle;
            m01 = -sinAngle;
            m10 = sinAngle;
            m11 = cosAngle;
        }
        else {
            float c00 = m00 * cosAngle;
            float c01 = m00 * -sinAngle;
            float c10 = m11 * sinAngle;
            float c11 = m11 * cosAngle;
        
            if ((type & TYPE_ROTATE) != 0) {
                c00 += m01 * sinAngle;
                c01 += m01 * cosAngle;
                c10 += m10 * cosAngle;
                c11 += m10 * -sinAngle;
            }
            
            m00 = c00;
            m01 = c01;
            m10 = c10;
            m11 = c11;
        }
        
        type |= TYPE_ROTATE;
    }
    
    public void shear(float x, float y) {
        // [   1   x   0   ]
        // [   y   1   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m01 = x;
            m10 = y;
        }
        else {
            float c01 = m01 + m00 * x;
            float c10 = m10 + m11 * y;
                
            if ((type & TYPE_ROTATE) != 0) {
                m00 += m01 * y;
                m11 += m10 * x;
            }
            
            m01 = c01;
            m10 = c10;
        }
        
        type |= TYPE_ROTATE;
    }
}
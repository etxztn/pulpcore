/*
    Copyright (c) 2008, Interactive Pulp, LLC
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

import pulpcore.image.Colors;
import pulpcore.math.CoreMath;

/**
    A ColorAnimation is an Animation that allows animation from one ARGB 
    color to another. The animation can occur in either RGB space or HSB space -
    animating HSB space can often yield better-looking results.
    As an example, a ColorAnimation can be used to animate
    {@link pulpcore.sprite.FilledSprite#fillColor }.
*/
public class ColorAnimation extends Animation {
    
    public static final int RGB = 0;
    public static final int HSB = 1;
    
    private final int colorSpace;
    private final int fromHSB;
    private final int toHSB;
    
    /**
        @param colorSpace The color space to animate in, either RGB or HSB.
    */
    public ColorAnimation(int colorSpace, int fromRGB, int toRGB, int duration) {
        this(colorSpace, fromRGB, toRGB, duration, null, 0);
    }
    
    
    /**
        @param colorSpace The color space to animate in, either RGB or HSB.
    */
    public ColorAnimation(int colorSpace, int fromRGB, int toRGB, int duration, Easing easing) {
        this(colorSpace, fromRGB, toRGB, duration, easing, 0);
    }
    
    
    /**
        @param colorSpace The color space to animate in, either RGB or HSB.
    */
    public ColorAnimation(int colorSpace, int fromRGB, int toRGB, int duration, Easing easing,
        int startDelay) 
    {
        super(fromRGB, toRGB, duration, easing, startDelay);
        this.colorSpace = colorSpace;
        
        if (colorSpace == HSB) {
            fromHSB = Colors.RGBtoHSB(fromRGB);
            toHSB = Colors.RGBtoHSB(toRGB);
        }
        else {
            fromHSB = 0;
            toHSB = 0;
        }
    }
    
    
    protected int calcValue(int animTime) {
              
        int v1;
        int v2;
        
        if (colorSpace == HSB) {
            v1 = fromHSB;
            v2 = toHSB;
        }
        else {
            v1 = fromValue;
            v2 = toValue;
        }
        
        int a1 = v1 >>> 24;
        int b1 = (v1 >> 16) & 0xff;
        int c1 = (v1 >> 8) & 0xff;
        int d1 = v1 & 0xff;
        
        int a2 = v2 >>> 24;
        int b2 = (v2 >> 16) & 0xff;
        int c2 = (v2 >> 8) & 0xff;
        int d2 = v2 & 0xff;
        
        if (colorSpace == HSB && Math.abs(b1 - b2) >= 128) {
            if (b1 > b2) {
                b2 += 0x100;
            }
            else {
                b1 += 0x100;
            }
        }
        
        int a = a1 + CoreMath.mulDiv(a2 - a1, animTime, duration) & 0xff;
        int b = b1 + CoreMath.mulDiv(b2 - b1, animTime, duration) & 0xff;
        int c = c1 + CoreMath.mulDiv(c2 - c1, animTime, duration) & 0xff;
        int d = d1 + CoreMath.mulDiv(d2 - d1, animTime, duration) & 0xff;
        
        int newValue = (a << 24) | (b << 16) | (c << 8) | d;
        
        if (colorSpace == HSB) {
            return Colors.HSBtoRGB(newValue);
        }
        else {
            return newValue;
        }
    }
}
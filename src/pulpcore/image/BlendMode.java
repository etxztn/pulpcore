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

package pulpcore.image;

/**
    An enumeration of blend modes. 
    <p>
    The methods always returns the same object; that is, 
    {@code BlendMode.SrcOver() == BlendMode.SrcOver()} is always {@code true}.
    <p>
    The BlendMode class is designed for lazy-creation of blend modes so that 
    code shrinkers (ProGuard) can remove blend modes that an app does not use. 
    @see CoreGraphics#setBlendMode(BlendMode) 
    @see pulpcore.sprite.Sprite#setBlendMode(BlendMode) 
*/
public final class BlendMode {
    
    private static BlendMode SRC_OVER = null;
    private static BlendMode ADD = null;
    private static BlendMode MULT = null;
    
    /* package-private */ final Composite opaque;
    /* package-private */ final Composite alpha;
    
    /* package-private */ BlendMode(Composite opaque, Composite alpha) {
        this.opaque = opaque;
        this.alpha = alpha;
    }
    
    /** 
        Gets the SrcOver blend mode.
        The source is composited over the destination (Porter-Duff Source Over Destination rule).
        This is the default blend mode.
    */
    public static BlendMode SrcOver() {
        if (SRC_OVER == null) {
            SRC_OVER = new BlendMode(new CompositeSrcOver(true), new CompositeSrcOver(false));
        }
        return SRC_OVER;
    }
    
    /**
        Gets the Add blend mode. Color components from the source are added to those
        of the surface.
    */
    public static BlendMode Add() {
        if (ADD == null) {
            ADD = new BlendMode(new CompositeAdd(true), new CompositeAdd(false));
        }
        return ADD;
    }
    
    /**
        Gets the Multiply blend mode. Color components from the source are multiplied by those
        of the surface.
    */
    public static BlendMode Multiply() {
        if (MULT == null) {
            MULT = new BlendMode(new CompositeMult(), new CompositeMult());
        }
        return MULT;
    }
}

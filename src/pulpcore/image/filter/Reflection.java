/*
    Copyright (c) 2009, Interactive Pulp, LLC
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
package pulpcore.image.filter;

import pulpcore.animation.Fixed;
import pulpcore.image.CoreImage;

/**
	A Reflection filter.

*/
public final class Reflection extends Filter {

	public final static int NO_FADING = 0; 
	public final static int FADE_BY_2 = 1; 
	public final static int FADE_BY_4 = 2; 
	
	/**
    	The reflection height. 
    */
	public final Fixed reflectionHeight = new Fixed(130);

	/**
	The gap between the image and its reflection. 
	*/
	public final Fixed gap = new Fixed(1);
	
	/**
	The fading divider value of the reflection. 
	*/
	public final Fixed fadingDivider = new Fixed(FADE_BY_4);
	
	public Reflection() {
		this(130);
	}
	
	public Reflection(int height) {
		this(height, 1);
	}
	
	public Reflection(int height, int gap) {
		this(height, gap, FADE_BY_2);
	}
	
	public Reflection(int height, int gap, int fadingDivider) {
		this.reflectionHeight.set(height);
		this.fadingDivider.set(fadingDivider);
        this.gap.set(gap);
	}
	
    public Filter copy() {
        Filter in = getInput();
        Reflection copy = new Reflection();
        copy.setInput(in == null ? null : in.copy());
        copy.reflectionHeight.bindTo(reflectionHeight);
        copy.gap.bindTo(gap);
        copy.fadingDivider.bindTo(fadingDivider);
        return copy;
    }
    
    @Override
    public boolean isDifferentBounds() {
    	return true;
    }
    
    
    public int getWidth() {
        return super.getWidth();
    }

    public int getHeight() {
        return super.getHeight() + reflectionHeight.getAsInt() + gap.getAsInt();
    }
    
    @Override
    public boolean isOpaque() {
    	return false;
    }
    

    protected void filter(CoreImage src, CoreImage dst) {
	
        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();
        
        int height = src.getHeight();
        int width = src.getWidth();

        // copies the source into the destination
        int srcOffset = 0;
        for (int i = 0; i < height; i++) {
            System.arraycopy(srcPixels, srcOffset, dstPixels, srcOffset, width);
            srcOffset += width;
        }
        
        // copies/creates the reflection
        for (int i = 0; i < reflectionHeight.getAsInt(); i++) {
        	for(int j = 0; j < width; j++) {
        		
        		int srcARGB = srcPixels[((height - i - 1)*width)+j];
        		int srcR = (srcARGB >> 16) & 0xff;
        		int srcG = (srcARGB >> 8) & 0xff;
        		int srcB = srcARGB & 0xff;
        		int srcA = srcARGB >>> 24;

        		// calculate the alpha gradient and divide the result by the specified divider.
        		int maskA = (0xff - (i*0xff)/reflectionHeight.getAsInt()) >> fadingDivider.getAsInt();
        		// DST_IN blend mode
        		srcR = (srcR * maskA) >> 8; 
        		srcG = (srcG * maskA) >> 8;
        		srcB = (srcB * maskA) >> 8;
        		srcA = (srcA * maskA) >> 8;
        		
        		int offset = ((i+gap.getAsInt()+height-1)*width)+j; 
        		dstPixels[offset] = (srcA << 24) | (srcR << 16) | (srcG << 8) | srcB; 
        	}
        }
	}
}

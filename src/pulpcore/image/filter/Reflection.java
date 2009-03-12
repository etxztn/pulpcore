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

import pulpcore.animation.Int;
import pulpcore.image.CoreImage;

/**
 *	A Reflection filter.
 *	Fake a mirrored image on brilliant surface.
 *	
 *	This filter uses 3 parameters : 
 * - reflectionHeight : the height of the reflection area. If left blank, half of the sprite height is used;
 * - gap : the space between the sprite and its reflection. Default value is 0.
 * - fadingDivider : the amount of fading for the reflection area. take one of NO_FADING, FADE_BY_2, FADE_BY_4.
 *                   default value is FADE_BY_2.
 * 
 *	@author Florent Dupont
 *
*/
public final class Reflection extends Filter {

	public final static int NO_FADING = 0; 
	public final static int FADE_BY_2 = 1; 
	public final static int FADE_BY_4 = 2; 
	
	/**
    	The reflection height. 
    */
	public final Int reflectionHeight = new Int(130);

	/**
		The gap between the image and its reflection. 
	*/
	public final Int gap = new Int(1);
	
	/**
		The fading divider value of the reflection. 
	*/
	public final Int fadingDivider = new Int(FADE_BY_2);
	
	
	private int actualReflectionHeight;
	private int actualGap;
	private int actualFading;
		
	/**
		Creates a Reflection filter with a gap of 1.
		At the first filter() call reflectionHeight will be set to half the height of the image.
	 */
	public Reflection() {
		this(1);
	}
	
	/**
		Creates a Reflection filter with the specified reflection height. Gap has a default value of 1.
	*/
	public Reflection(int height) {
		this(height, 1);
	}
	
	/**
		Creates a Reflection filter with the specified reflection height and gap value.
	 */
	public Reflection(int height, int gap) {
		this(height, gap, FADE_BY_2);
	}
	
	/**
		Creates a Reflection filter with the specified reflection height and gap value.
		fading divider is also specified and can take 3 one of these 3 values : 
			- NO_FADING
			- FADE_BY_2
			- FADE_BY_4.
 	*/
	public Reflection(int height, int gap, int fadingDivider) {
		this.reflectionHeight.set(height);
		this.fadingDivider.set(fadingDivider);
        this.gap.set(gap);
	}
	
    public Filter copy() {
        Reflection copy = new Reflection();
        copy.reflectionHeight.bindWithInverse(reflectionHeight);
        copy.gap.bindWithInverse(gap);
        copy.fadingDivider.bindWithInverse(fadingDivider);
        return copy;
    }
    
    public int getY() {
    	return -actualGap/2;
    }
    
    public int getWidth() {
        return super.getWidth();
    }

    public int getHeight() {
        return super.getHeight() + actualReflectionHeight + actualGap;
    }

    public boolean isOpaque() {
    	return false;
    }
    
    public void update(int elapsedTime) {
        	
    	reflectionHeight.update(elapsedTime);
    	gap.update(elapsedTime);
    	fadingDivider.update(elapsedTime);
    	
    	if(reflectionHeight.get() != actualReflectionHeight) {
    		actualReflectionHeight = reflectionHeight.get();
    		setDirty();
    	}
    	
    	if(gap.get() != actualGap) {
    		actualGap = gap.get();
    		setDirty();
    	}
    	
    	if(fadingDivider.get() != actualFading) {
    		actualFading = fadingDivider.get();
    		setDirty();
    	}
    }

    protected void filter(CoreImage src, CoreImage dst) {
    	
    	if(reflectionHeight.get() > src.getHeight()) {
    		System.out.println("reflection height cannot be higher than the sprite height.");
    		// reflection is then set to be half the value of the src height
    		reflectionHeight.set(src.getHeight() / 2);
    		actualReflectionHeight = reflectionHeight.get();
    	}
    	
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
        for (int i = 0; i < actualReflectionHeight; i++) {
        	for(int j = 0; j < width; j++) {

        		int srcARGB = srcPixels[((height - i-1)*width)+j];
        		int srcR = (srcARGB >> 16) & 0xff;
        		int srcG = (srcARGB >> 8) & 0xff;
        		int srcB = srcARGB & 0xff;
        		int srcA = srcARGB >>> 24;

        		// calculate the alpha gradient and divide the result by the specified divider.
        		int maskA = (0xff - (i*0xff)/actualReflectionHeight) >> actualFading;
        		// DST_IN blend mode
        		srcR = (srcR * maskA) >> 8; 
        		srcG = (srcG * maskA) >> 8;
        		srcB = (srcB * maskA) >> 8;
        		srcA = (srcA * maskA) >> 8;

        		int offset = ((i+actualGap+height-1)*width)+j; 
        		dstPixels[offset] = (srcA << 24) | (srcR << 16) | (srcG << 8) | srcB; 
        	}
        }

	}
}

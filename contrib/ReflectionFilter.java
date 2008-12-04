/*
 * Copyright (c) 2008, Florent Dupont
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the PulpCore project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package pulpcore.image.filter;


/**
 * A fast implementation of the reflection filter.
 * Alpha fading can be set to : 
 *  0 : No alpha fading. The original image alpha is used
 *  1 : alpha faded by 2. 
 *  2 : alpha faded by 4.
 *  
 *  The original pixel array is modified and is the result of the filter.
 *  
 *  @author Florent Dupont
 */
public final class ReflectionFilter {
	
	public final static int NO_FADING = 0; 
	public final static int FADE_BY_2 = 1; 
	public final static int FADE_BY_4 = 2; 
	
	int reflectionHeight = 130;
	int fadingDivider = 2;
	
	public ReflectionFilter() {
		this(0);
	}
	
	public ReflectionFilter(int reflectionHeight) {
		this(reflectionHeight, 0);
	}
	
	public ReflectionFilter(int reflectionHeight, int fadingDivider) {
		this.reflectionHeight = reflectionHeight;
		this.fadingDivider = fadingDivider;
	}

	public void applyFilter(int[] srcPixels, int width, int height) {
		reflectionHeight = reflectionHeight == 0 ? height/2 : reflectionHeight;
		         
         for (int i = 0; i < height; i++) {
        	 if(i >= reflectionHeight) {
        		 for(int j = 0; j < width; j++) {
        			 // the rest of the image is transparent
        			 srcPixels[(i*width)+j] = 0x00; 
        		 }
        	 } else {
	        	 for(int j = 0; j < width; j++) {
	        		 int srcARGB = srcPixels[((height - i - 1)*width)+j];
	        		 int srcR = (srcARGB >> 16) & 0xff;
	        		 int srcG = (srcARGB >> 8) & 0xff;
	        		 int srcB = srcARGB & 0xff;
	        		 int srcA = srcARGB >>> 24;
	
	        		 // calculate the alpha gradient and divide the result by the specified divider.
	        		 int maskA = (0xff - (i*0xff)/reflectionHeight) >> fadingDivider;
	        		 // DST_IN blend mode
	     			 srcR = (srcR * maskA) >> 8; 
	        		 srcG = (srcG * maskA) >> 8;
	        		 srcB = (srcB * maskA) >> 8;
	        		 srcA = (srcA * maskA) >> 8;
	        		 
	        		 srcPixels[(i*width)+j] = (srcA << 24) | (srcR << 16) | (srcG << 8) | srcB; 
	        	 }
        	 }
    	 }
	}
}

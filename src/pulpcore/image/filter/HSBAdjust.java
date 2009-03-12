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
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;


/**
 *	A HSB adjust filter.
 *	
 *	This filter uses 3 parameters : hue, saturation, brightness.
 *  Each parameter indicates the offset of hue, brightness and saturation to apply to the filtered image.
 *  Each of this value can move from -255 to 255. Higher values will be clamped.
 *  The alpha channel of the original image is not affected by the filter.
 * 
 *	@author Florent Dupont
 *
*/
public final class HSBAdjust extends Filter {
	
	/**
		HUE. indicates the hue offset this filter will apply to the image.
		Values from -255 to 255
	 */
	public Int hue = new Int(0);
	
	/**
		SATURATION. indicates the saturation offset this filter will apply to the image.
		Values from -255 to 255
	 */
	public Int saturation = new Int(0);
	
	/**
		BRIGHTNESS. indicates the brightness offset this filter will apply to the image.
		Values from -255 to 255
	 */
	public Int brightness = new Int(0);

	
	
	
	int actualHue;
	int actualBrightness;
	int actualSaturation;
	
	/**
	 * Creates the HSBAdjust filter with the default values (0).
	 */
	public HSBAdjust() {
		this(0,0,0);
	}
	

	/**
	 * Creates the filters with specific values.
	 * @param hue hue offset to apply to the image.
	 * @param saturation saturation offset to apply to the image.
	 * @param brightness brightness offset to apply to the image.
	 */
	public HSBAdjust(int hue, int saturation, int brightness) {
		this.hue.set(hue);
		this.saturation.set(saturation);
		this.brightness.set(brightness);
	}
	
	 
    public Filter copy() {
        HSBAdjust copy = new HSBAdjust();
        copy.hue.bindWithInverse(hue);
        copy.brightness.bindWithInverse(brightness);
        copy.saturation.bindWithInverse(saturation);
        return copy;
    }
    
    
    public void update(int elapsedTime) {
    	hue.update(elapsedTime);
    	brightness.update(elapsedTime);
    	saturation.update(elapsedTime);
    	
    	if(hue.get() != actualHue) {
    		actualHue = hue.get();
    		setDirty();
    	}
    	
    	if(brightness.get() != actualBrightness) {
    		actualBrightness = brightness.get();
    		setDirty();
    	}
    	
    	if(saturation.get() != actualSaturation) {
    		actualSaturation = saturation.get();
    		setDirty();
    	}
    }

    protected void filter(CoreImage src, CoreImage dst) {
			
        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();

        int height = src.getHeight();
        int width  = src.getWidth();
        
        
        for (int i = 0; i < height; i++) {
        	for(int j = 0; j < width; j++) {
        		int index = i*width + j;
        		
        		int srcArgb = srcPixels[index];
        		int a = srcArgb >>> 24;
        		
        		int b = Colors.getBrightness(srcArgb);
        		int h = Colors.getHue(srcArgb);
        		int s = Colors.getSaturation(srcArgb);
        		
        		b = b + actualBrightness;
        		b = b > 255 ? 255 : ( b < 0 ? 0 : b);
        		
        		h = h + actualHue;
        		h = h > 255 ? 255 : ( h < 0 ? 0 : h);

        		s = s + actualSaturation;
        		s = s > 255 ? 255 : ( s < 0 ? 0 : s);
        		
        		dstPixels[index] = Colors.hsba(h, s, b, a);
        		
        	}
        }
	}
}

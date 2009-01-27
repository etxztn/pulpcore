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


import pulpcore.animation.Color;
import pulpcore.animation.Int;
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;


/**
    A simple drop shadow. The source image should not be opaque, and should be large enough
    to contain the drop shadow in its bounds.
    For opaque images, try CoreImage.expandCanvas() first.

    @author David Brackeen
    @author Florent Dupont
 */
public class DropShadow extends Blur {

    /**
        The shadow x offset. The default value is 3.
    */
    public final Int shadowOffsetX = new Int(3);

    /**
        The shadow y offset. The default value is 3.
    */
    public final Int shadowOffsetY = new Int(3);
    
    /**
        The shadow color. The default value is black with 50% alpha.
    */
    public final Color color = new Color(DEFAULT_COLOR);

    private int[] shadowColorTable = new int[256];
    public final static int DEFAULT_COLOR = Colors.rgba(0, 0, 0, 128);
    
    private int actualShadowOffsetX = 0; 
    private int actualShadowOffsetY = 0; 
    
    /**
		Creates a Shadow filter with the default values.
		- X offset of 3
		- Y offset of 3
		- shadow radius of 3, and quality of 3
    */
    public DropShadow() {
    	this(3, 3);
    }
    
    /**
		Creates a Shadow filter with the specified X and Y offset.
		- shadow radius of 3, and quality of 3
     */
    public DropShadow(int offsetX, int offsetY) {
    	this(offsetX, offsetY, DEFAULT_COLOR);
    }
    
    /**
		Creates a Shadow filter with the specified X and Y offset, and shadow color.
		- shadow radius of 3, and quality of 3
     */
    public DropShadow(int offsetX, int offsetY, int shadowColor) {
     	this(offsetX, offsetY, shadowColor, 3);
    }
    
    /**
		Creates a Shadow filter with the specified X and Y offset, shadow color, and shadow radius.
		Default quality of 3
     */
    public DropShadow(int offsetX, int offsetY, int shadowColor, int radius) {
     	this(offsetX, offsetY, shadowColor, radius, 3);
    }
    
    /**
		Creates a Shadow filter with the specified X and Y offset, shadow color, and shadow radius and quality.
    */
    public DropShadow(int offsetX, int offsetY, int shadowColor, float radius, int quality) {
    	super(radius, quality);
    	this.shadowOffsetX.set(offsetX);
     	this.shadowOffsetY.set(offsetY);
     	this.color.set(shadowColor);
     	
     	createShadowColorTable();
    	 	
    }
    
    
    public Filter copy() {
		Filter in = getInput();
		DropShadow copy = new DropShadow();
		copy.setInput(in == null ? null : in.copy());
		copy.radius.bindTo(radius);
        copy.quality.bindTo(quality);
        copy.shadowOffsetX.bindTo(shadowOffsetX);
        copy.shadowOffsetY.bindTo(shadowOffsetY);
        copy.color.bindTo(color);
		return copy;
	}
    
    public void update(int elapsedTime) {
		super.update(elapsedTime);
		
		if(actualShadowOffsetX != shadowOffsetX.get()) {
			actualShadowOffsetX = shadowOffsetX.get();
			setDirty(true);
		} 
		
		if(actualShadowOffsetY != shadowOffsetY.get()) {
			actualShadowOffsetY = shadowOffsetY.get();
			setDirty(true);
		} 
		 
	}
    
    private void createShadowColorTable() {
        int rgb = Colors.rgb(color.get());
        int alpha = Colors.getAlpha(color.get());
        for (int i = 0; i < 256; i++) {
            shadowColorTable[i] = Colors.premultiply(rgb, (alpha * i) >> 8);
        }
    }
    
    public boolean isDifferentBounds() {
    	return true;
    }
    
    
    public int getOffsetX() {
    	if(actualShadowOffsetX > 0) 
    		return super.getOffsetX();
    	else
    		return super.getOffsetX() + actualShadowOffsetX;
    }

    public int getOffsetY() {
    	if(actualShadowOffsetY > 0) 
    		return super.getOffsetY();
    	else
    		return super.getOffsetY() + actualShadowOffsetY;
    }

    public int getWidth() {
        return super.getWidth() + CoreMath.abs(actualShadowOffsetX);
    }

    public int getHeight() {
    	
        return super.getHeight() + CoreMath.abs(actualShadowOffsetY);
    }
    
    
    protected void filter(CoreImage src, CoreImage dst) {
    	
    	int[] dstData = dst.getData();
    	int[] srcData = src.getData();
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int dstWidth = dst.getWidth();
		int dstHeight = dst.getHeight();
		
		if(radius.get() == 0 || quality.get() == 0) {
    		
			for (int i = 0; i < srcHeight; i++) {
				for (int j = 0; j < srcWidth; j++) {
					
					int dstIndex = (i * dstWidth) + j;
					int srcIndex = (i * srcWidth) + j;
					dstData[dstIndex] = srcData[srcIndex];
					
				}
			}
    	} else {
    	 	// call the parent blur filter.
        	super.filter(src, dst);
        }
		
		// copies the dest pixels
		int []tmpDst = new int[dstData.length];
		//System.arraycopy(dst, 0, tmpDst, 0, dstData.length);
		for(int i = 0; i < dstData.length; i++) {
			tmpDst[i] = dstData[i];
		}
		for(int i = 0; i < dstData.length; i++) {
			dstData[i] = 0x00;
		}
		
		int xOffset = getOffsetXFromOriginal();
		int yOffset = getOffsetYFromOriginal();
		
		// translates the shadow
		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {
			
				int tmpIndex = (i * dstWidth) + j;
				int dstPixel = tmpDst[tmpIndex];
				
				if(((i + actualShadowOffsetY) > dstHeight-1 || i + actualShadowOffsetY < 0)) {
					continue;
				}
				if(((j + actualShadowOffsetX) > dstWidth-1) || (j + actualShadowOffsetX < 0)) {
					continue;
				}
				int dstIndex = ((i+actualShadowOffsetY)*dstWidth) + actualShadowOffsetX + j;
				dstData[dstIndex] = dstPixel;
			}
		}
		
		// copies the image over the shadow
		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {
				
				int dstIndex = (i * dstWidth) + j;
				int dstPixel = shadowColorTable[dstData[dstIndex] >>> 24];
								
				int srcPixel = 0;
				// if we're in the SRC part, copy the src over the dst.
				if(((i + yOffset) >= 0) && ((i+yOffset) < srcHeight) &&
						((j + xOffset) >= 0) && ((j + xOffset) < srcWidth)) {
					
					int srcIndex = ((i + yOffset) * srcWidth) + j + xOffset;
					srcPixel = srcData[srcIndex];
				
					int da = dstPixel >>> 24;
					int dr = (dstPixel >> 16) & 0xff;
					int dg = (dstPixel >> 8) & 0xff;
					int db = dstPixel & 0xff;
					
					int sa = srcPixel >>> 24;
					int sr = (srcPixel >> 16) & 0xff;
					int sg = (srcPixel >> 8) & 0xff;
					int sb = srcPixel & 0xff;
					
					 int oneMinusSA = 0xff - sa;
					
					// SrcOver: original image on top of shadow
			          da = sa + ((da * oneMinusSA) >> 8);
			          dr = sr + ((dr * oneMinusSA) >> 8);
			          dg = sg + ((dg * oneMinusSA) >> 8);
			          db = sb + ((db * oneMinusSA) >> 8);
			        
			          dstData[dstIndex] = (da << 24) | (dr << 16) | (dg << 8) | db;
				} else {
					// else just copy DST
			        dstData[dstIndex] = dstPixel;
					
				}
				
			}
		}
				
    }
    

}

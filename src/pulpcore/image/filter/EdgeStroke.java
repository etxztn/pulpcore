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
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
 * A edge Stroke filter.
 * An example can be found here : 
 * http://www.webdesign.org/web/photoshop/photoshop-basics/stroke-a-stroke-in-photoshop.14342.html
 * 
 * Example of use (to obtain the same effect as the above PSD example) : 
 * ImageSprite sprite = new ImageSprite("star.png", 50, 50);
 * FilterChain chain = new FilterChain(new EdgeStroke(0xff289acd, 7), new EdgeStroke(0xFF00679A, 7));
 * sprite.setFilter(chain);
 * add(sprite);
 * 
 * Restriction : 
 *  - avoid the use of fully translucents sprites.
 *  - alpha channel is not used for the stroke color. 
 *   --> some minor changes to make it work with alpha color though. 
 * 
 * @author Florent Dupont
 *
 */
public final class EdgeStroke extends Filter {
	
	private final static int DEFAULT_COLOR = 0xff000000;

	public Color color = new Color(0x00000000);
	public Int   radius = new Int(5);
	
	private int actualColor;
	private int actualRadius;
	
	private int precalculatedIntensities[];
	
	// DEBUG purpose only
	private boolean filledCircles  = true;
	// this parameter shouldn't change a lot of the visual stuffs.
	// If you use fully translucent image, use opaqueDetection = false
	private boolean opaqueDetection = true;

	
	public EdgeStroke(EdgeStroke filter) {
		radius.bindWithInverse(filter.radius);
		color.bindWithInverse(filter.color);
	}
	
	/**
	  Creates a EdgeStroke filter with the default color (black) and a radius of 5.
	 */
	public EdgeStroke() {
		this(DEFAULT_COLOR);
	}
	
	/**
	  Creates a EdgeStroke filter with the specified color and a radius of 5.
	 */
	public EdgeStroke(int color) {
		this(color, 5);
	}
	
	/**
	  Creates a EdgeStroke filter with the specified color and radius.
	 */
	public EdgeStroke(int color, int radius) {
		this.color.set(color);
		this.radius.set(radius);
		precalculatedIntensities = precalculateIntensities(radius);
	}
	
	/**
	 * this parameter shouldn't change a lot of the visual stuffs.
	 * If you use fully translucent image, use opaqueDetection = false
	 * @param opaqueDetection
	 */
	public void setOpaqueDetection(boolean opaqueDetection) {
		this.opaqueDetection = opaqueDetection;
	}

	public Filter copy() {
        return new EdgeStroke(this);
	}
	
	public void update(int elapsedTime) {
		    
		super.update(elapsedTime);

		color.update(elapsedTime);
		radius.update(elapsedTime);
		
		if(color.get() != actualColor) {
			actualColor = color.get();
			setDirty();
		}

		if(radius.get() != actualRadius) {
			actualRadius = radius.get();
			setDirty();
			precalculatedIntensities = precalculateIntensities(actualRadius);
		}
	}

	
	 public int getY() {
	   	return -actualRadius;
	 }
	
	 public int getX() {
	   	return -actualRadius;
	 }
	    
	 public int getWidth() {
	     return super.getWidth() + actualRadius * 2 ;
	 }

	 public int getHeight() {
	    return super.getHeight() + actualRadius * 2;
	 }

	
	protected void filter(CoreImage src, CoreImage dst) {
		
		int xOffset = getX();
		int yOffset = getY();
		
		int[] srcData = src.getData();
		int[] dstData = dst.getData();
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int dstWidth = dst.getWidth();
		int dstHeight = dst.getHeight();
		
		int[] pxs1 = new int[3];
		int[] pxs2 = new int[3];
		
		for (int i = 0; i < srcHeight; i++) {
			for (int j = 0; j < srcWidth; j++) {

				// first pass for edge detection 
				pxs1[0] = getPixel(srcData, srcWidth, srcHeight, j -1, i);
				pxs1[1] = getPixel(srcData, srcWidth, srcHeight, j   , i);
				pxs1[2] = getPixel(srcData, srcWidth, srcHeight, j +1, i);

				// second pass
				pxs2[0] = getPixel(srcData, srcWidth, srcHeight, j ,i -1);
				pxs2[1] = getPixel(srcData, srcWidth, srcHeight, j ,i   );
				pxs2[2] = getPixel(srcData, srcWidth, srcHeight, j ,i +1);

				boolean isBorder = false;

				if(opaqueDetection) {
					boolean b1 = processOpaqueEdges(pxs1);
					boolean b2 = processOpaqueEdges(pxs2);
					isBorder = b1 || b2; 
				} 
				else {
					boolean b1 = processEdges(pxs1);
					boolean b2 = processEdges(pxs2);
					isBorder = b1 || b2; 
				}

				if(isBorder) {
					if(filledCircles) {
						wuAntialiasedFilledCircle(dstData, dstWidth, dstHeight, j - xOffset , i- yOffset, actualRadius);
					}
					else {
						// for debug purpose
						wuAntialiasedCircle(dstData, dstWidth, dstHeight, j - xOffset , i - yOffset, actualRadius);
					}
				}

			}
		}
		
		 // Draw the input image on top of the stroke
        CoreGraphics g = dst.createGraphics();
        g.drawImage(src, -xOffset, -yOffset);
	}


	/**
	 * precalculate the D(r, j) intensity values.
	 * precalculation stays the same for a given radius.
	 * Make sure to re-precalculate if the radius changes.
	 * int this precalculation j goes from 1 to  r / sqrt(2)
	 * @param radius
	 * @return an int array containing precalcs.
	 */
	private final int[] precalculateIntensities(int radius) {

		// radius stays the same.
		// make sure to re-precalculate if the radius changes
		// j from 1 to  r / sqrt(2)
		int maxJ = (int) Math.ceil(radius * 1.0d / 1.4142135623d);  
		int[] result = new int[maxJ+1];

		for(int j = 1 ; j <= maxJ; j++) {
			result[j] = calculateIntensity(radius, j);
		}
		return result;
	}


	/**
	 * used for debug purpose.
	 * May be useful to place this in CoreGraphic ... 
	 */
	private final void wuAntialiasedCircle(int [] pixels, int width, int height, int xCenter, int yCenter, int radius) {

		int i = radius;
		int t = 0;

		drawOctants(255, pixels, width, height, 0, radius, xCenter, yCenter);

		for(int j = 1; j < i; j++) {
			int d = precalculatedIntensities[j]; //precalculated D(r, j)
			if(d < t) i--; 
			if(i < j) break;

			drawOctants(255-d, pixels, width, height, i, j, xCenter, yCenter);
			drawOctants(d, pixels, width, height, i-1, j, xCenter, yCenter);
			t = d;
		} 

	}

	/**
	 *  May be useful to place this in CoreGraphic ... 
	 */
	private final void wuAntialiasedFilledCircle(int [] pixels, int width, int height, int xCenter, int yCenter, int radius) {

		int i = radius;
		int t = 0;
		
		// fills the inside lines.
		for(int k =  0 ; k <= i; k++) {
			drawOctants(255, pixels, width, height, 0, k, xCenter, yCenter);
		}

		for(int j = 1; j < i; j++) {
			int d = precalculatedIntensities[j]; //precalculated D(r, j)
			if( d < t) i--; 
			if(i < j) break;

			drawOctants(255-d, pixels, width, height, i, j, xCenter, yCenter);
			drawOctants(d, pixels, width, height, i-1, j, xCenter, yCenter);
			t = d;

			// filling the inside lines
			for(int k = j ; k < i; k++) {
				drawOctants(255, pixels, width, height, k, j, xCenter, yCenter);
			}
		} 
	}

	/**
	 *  Wu's intensity calculation. 
	 *  Equivalent of D(r, j) in Wu's algorithm 
	 *  Math formula is given in Graphics Gem II (p. 446)
	 *  
	 *  Each values is precalculater performances issues and available with {@link #precalculateIntensities(int)}.
	 *  Fixed point values are used to increase calculation.
	 *  The equivalent in using double : 
	 *    double r2j2 = (r * r)  - (j * j);
	 *	  double pr = Math.ceil(Math.sqrt(r2j2)) - Math.sqrt(r2j2);
	 *	  double result = Math.floor(255 * pr + 0.5);
	 *  
	 */
	private int calculateIntensity(int r, int j) {
		int r2j2_fixed = CoreMath.toFixed(r * r) - CoreMath.toFixed(j * j);
		int r2j2sqrt_fixed = CoreMath.sqrt(r2j2_fixed);
		int pr_fixed = CoreMath.ceil(r2j2sqrt_fixed) - r2j2sqrt_fixed;
		int result_fixed = (255 * pr_fixed + (CoreMath.ONE/2));
		return CoreMath.toIntFloor(result_fixed);
	}


	/**
	 * Draws every circle octants with a specified intensity.
	 * Equivalent of I(i, j) in Wu's algorithm
	 */
	private final void drawOctants(int intensity, int[] pixels, int width, int height, int x,int y, int xCenter, int yCenter) {
		setPixel(intensity, pixels, width, height, xCenter + x, yCenter + y);
		setPixel(intensity, pixels, width, height, xCenter - x, yCenter + y);
		setPixel(intensity, pixels, width, height, xCenter + x, yCenter - y);
		setPixel(intensity, pixels, width, height, xCenter - x, yCenter - y);
		setPixel(intensity, pixels, width, height, xCenter + y, yCenter + x);
		setPixel(intensity, pixels, width, height, xCenter - y, yCenter + x);
		setPixel(intensity, pixels, width, height, xCenter + y, yCenter - x);
		setPixel(intensity, pixels, width, height, xCenter - y, yCenter - x);
	}

	private final void setPixel(int intensity, int[] pixels, int width, int height, int x,int y) {

		int colorsVal = (intensity << 24) | (actualColor & 0x00ffffff); 
		int ARGB = pixels[(y * width) + x];

		if((ARGB >>> 24) < intensity)
			pixels[(y * width) + x] = Colors.premultiply(colorsVal);
	}

	/**
	 * Retrieves a secific pixel value in a raster data.
	 * Pixels out of the image are considered transparent.  
	 * @return
	 */
	private final int getPixel(int[] pixels, int width, int height, int x, int y) {
		if(x < 0 || x > width-1)
			return 0;
		if(y < 0 || y > height -1)
			return 0;

		return pixels[(y * width) + x];

	}


	/**
	 * Detect borders for a vector. Edges are detected if the middle pixel is non-transparent 
	 * (i.e translucent or opaque) and at least one of its neighbour is transparent.
	 * @param values a 3x1 vector containing pixels values.
	 * @param color
	 * @return true if the center pixels (index 1) is considered as a border. 
	 */
	private final boolean processEdges(int[] values) {
		return (values[1] != 0 && (values[0] == 0 || values[2] == 0));
	}

	/**
	 * Same as {@link #processEdges(int[])} except that the middle pixel is opaque and at least
	 * one of its neighbour is non-opaque (i.e. translucent or transparent).
	 * @param values a 3x1 vector containing pixels values.
	 * @param color
	 * @return true if the center pixels (index 1) is considered as a border. 
	 */
	private final boolean processOpaqueEdges(int[] values) {
		int a0 = values[0] >>> 24;
		int a1 = values[1] >>> 24;
		int a2 = values[2] >>> 24;
		return (a1 == 255 && (a0 < 255 || a2 < 255));
	}


}

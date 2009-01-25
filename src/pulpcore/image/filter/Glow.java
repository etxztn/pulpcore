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
 * A Glow filter.
 * 
 * This Glow filter uses 3 parameters : 
 * - amount : the amount of glow to be used. float value that could go from 0 to up to 2 depending on the effect
 *   you're expecting to have. Default value is 0.5;
 * - radius : the radius of glow to be used. Default value is 3.
 * - quality : the rendering quality of the filter. A better quality needs more time to render. Default value is 3.
 *  In some cases values below 3 should be OK (quality 1 is very poor though). Quality value more than 3 won't 
 *  really affect the rendering since this 3 fast blur are somehow equivalent to a real Gaussian Blur.
 *      
 * 
 * Example of use below that mimic the PS3 menu selection :
 * <code>
 * 
 *	public void load() {
 *	
 *		add(new FilledSprite(Colors.BLACK));
 *		
 *		Glow glow = new Glow( 0.8, 5);
 *		
 *		Timeline timeline = new Timeline();
 *		timeline.animate(glow.amount, 0.2, 0.8, 500, null, 0);
 *		timeline.animate(glow.amount, 0.8, 0.2, 500, null, 500);
 *		timeline.loopForever();
 *	    addTimeline(timeline);
 *	    CoreFont font = CoreFont.getSystemFont().tint(0xff80ff80);
 *	    Label lb = new Label(font, "An example of glowing text...", 20, 20);
 *	    lb.setFilter(glow);
 *		
 *		add(lb);
 *  }
 *		
 * </code>
 * 
 * @author Florent Dupont
 */
public final class Glow extends Blur {

	public final Fixed amount = new Fixed(0.5f);
	
	private double actualAmount = 0;

	// radius and quality are also available

	public Glow() {
		this(0.5f);
	}

	public Glow(double amount) {
		this(amount, 3);
	}

	public Glow(double amount, int radius) {
		this(amount, radius, 3);
	}

	public Glow(double amount, int radius, int quality) {
		super(radius, quality);
		this.amount.set(amount);
	}

	public Filter copy() {
		Filter in = getInput();
		Glow copy = new Glow();
		copy.setInput(in == null ? null : in.copy());
		copy.amount.bindTo(amount);
        copy.radius.bindTo(radius);
        copy.quality.bindTo(quality);
		return copy;
	}
	
	public void update(int elapsedTime) {
		super.update(elapsedTime);
		amount.update(elapsedTime);
		
		if(actualAmount != amount.get()) {
			actualAmount = amount.get();
			setDirty(true);
		} 
	}

	protected void filter(CoreImage src, CoreImage dst) {

		// call the parent blur filter.
		super.filter(src, dst);

		// precalculate the amount to into an integer value.
		// mulitplication in the loop will be faster than float calculation.
		int a = (int)(actualAmount * 1024d);

		int[] dstData = dst.getData();
		int[] srcData = src.getData();
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int dstWidth = dst.getWidth();
		int dstHeight = dst.getHeight();
		
		int xOffset = getOffsetXFromOriginal();
		int yOffset = getOffsetYFromOriginal();
		
		for (int i = 0; i < dstHeight; i++) {
			for (int j = 0; j < dstWidth; j++) {
				
				int dstIndex = (i * dstWidth) + j;
				int dstPixel = dstData[dstIndex];
				
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
					
					int nr = sr + ((a * dr) >> 8);
					nr = nr > 255 ? 255 : nr;
					int ng = sg + ((a * dg) >> 8);
					ng = ng > 255 ? 255 : ng;
					int nb = sb + ((a * db) >> 8); 
					nb = nb > 255 ? 255 : nb;
					int na = sa + ((a * da) >> 8);
					na = na > 255 ? 255 : na;
					
					dstData[dstIndex] = (na << 24) | (nr << 16) | (ng << 8) | nb;
					
				} else {
					// else just copy DST
					int da = dstPixel >>> 24;
					int dr = (dstPixel >> 16) & 0xff;
					int dg = (dstPixel >> 8) & 0xff;
					int db = dstPixel & 0xff;
					
					int nr = (a * dr) >> 8;
					nr = nr > 255 ? 255 : nr;
					int ng = (a * dg) >> 8;
					ng = ng > 255 ? 255 : ng;
					int nb = (a * db) >> 8; 
					nb = nb > 255 ? 255 : nb;
					int na = (a * da) >> 8;
					na = na > 255 ? 255 : na;
					
					dstData[dstIndex] = (na << 24) | (nr << 16) | (ng << 8) | nb;
					
				}
				
			}
		}

	}
}

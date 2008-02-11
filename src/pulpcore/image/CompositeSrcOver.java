/*
    Copyright (c) 2007, Interactive Pulp, LLC
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
    Porter-Duff's SrcOver rule. In OpenGL, it is equivilent to:
    Premultiplied alpha:
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    Straight alpha:
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
*/
final class CompositeSrcOver extends Composite {
    
    private void blendInternalOpaque(int[] destData, int destOffset, int srcRGB) {
        // Same for both premultiplied and straight alpha
        destData[destOffset] = srcRGB;
    }
    
    private void blendInternalOpaque(int[] destData, int destOffset, int srcRGB, int extraAlpha) {
        int destRGB = destData[destOffset];
        int destR = (destRGB >> 16) & 0xff;
        int destG = (destRGB >> 8) & 0xff;
        int destB = destRGB & 0xff;
        int srcR = (srcRGB >> 16) & 0xff;
        int srcG = (srcRGB >> 8) & 0xff;
        int srcB = srcRGB & 0xff;
        
        // Same for both premultiplied and straight alpha
        destR += (extraAlpha * (srcR - destR)) >> 8;
        destG += (extraAlpha * (srcG - destG)) >> 8;
        destB += (extraAlpha * (srcB - destB)) >> 8;
        
        destData[destOffset] = 0xff000000 | (destR << 16) | (destG << 8) | destB;
    }

    private void blendInternal(int[] destData, int destOffset, int srcARGB) {
        int destRGB = destData[destOffset];
        int destAlpha;
        int destR = (destRGB >> 16) & 0xff;
        int destG = (destRGB >> 8) & 0xff;
        int destB = destRGB & 0xff;
        int srcAlpha = srcARGB >>> 24;
        int srcR = (srcARGB >> 16) & 0xff;
        int srcG = (srcARGB >> 8) & 0xff;
        int srcB = srcARGB & 0xff;
        
        if (CoreGraphics.PREMULTIPLIED_ALPHA) {
            // dest = src + dest*(1-srcAlpha)
            int oneMinusSrcAlpha = 0xff - srcAlpha;
            destR = srcR + ((destR * oneMinusSrcAlpha) >> 8);
            destG = srcG + ((destG * oneMinusSrcAlpha) >> 8);
            destB = srcB + ((destB * oneMinusSrcAlpha) >> 8);
            destAlpha = srcAlpha + (((destRGB >>> 24) * oneMinusSrcAlpha + 0xff) >> 8);
        }
        else {
            // dest = srcAlpha*src + dest*(1-srcAlpha)
            // dest = srcAlpha*src + dest - srcAlpha*dest;
            // dest += srcAlpha*(src - dest)
            destR += (srcAlpha * (srcR - destR)) >> 8;
            destG += (srcAlpha * (srcG - destG)) >> 8;
            destB += (srcAlpha * (srcB - destB)) >> 8;
            destAlpha = 0xff;
        }
        destData[destOffset] = (destAlpha << 24) | (destR << 16) | (destG << 8) | destB;
    }
    
    private void blendInternal(int[] destData, int destOffset, int srcARGB, int extraAlpha) {
        int destRGB = destData[destOffset];
        int destAlpha;
        int destR = (destRGB >> 16) & 0xff;
        int destG = (destRGB >> 8) & 0xff;
        int destB = destRGB & 0xff;
        int srcAlpha = ((srcARGB >>> 24) * extraAlpha) >> 8;
        int srcR = (srcARGB >> 16) & 0xff;
        int srcG = (srcARGB >> 8) & 0xff;
        int srcB = srcARGB & 0xff;
        
        if (CoreGraphics.PREMULTIPLIED_ALPHA) {
            // srcAlpha = srcAlpha*extraAlpha
            // dest = extraAlpha*src + dest*(1-srcAlpha)
            int oneMinusSrcAlpha = 0xff - srcAlpha;
            destR = (srcR * extraAlpha + destR * oneMinusSrcAlpha) >> 8;
            destG = (srcG * extraAlpha + destG * oneMinusSrcAlpha) >> 8;
            destB = (srcB * extraAlpha + destB * oneMinusSrcAlpha) >> 8;
            destAlpha = srcAlpha + (((destRGB >>> 24) * oneMinusSrcAlpha + 0xff) >> 8);
        }
        else {
            destR += (srcAlpha * (srcR - destR)) >> 8;
            destG += (srcAlpha * (srcG - destG)) >> 8;
            destB += (srcAlpha * (srcB - destB)) >> 8;
            destAlpha = 0xff;
        }
        destData[destOffset] = (destAlpha << 24) | (destR << 16) | (destG << 8) | destB;
    }
    
    /*
        DO NOT EDIT BELOW HERE
        The blend() methods need to be identical in all subclasses of Composite. Ideally, the 
        blend() methods belong in the parent class Composite. However, if that were the case, 
        calls to blendInternal() would result in an virtual method call - dramatically slowing 
        down the rendering.
        
        The blend() code is cut-and-pasted in each subclass of Composite to get HotSpot to inline 
        calls to blendInternal()
    */
    
    void blend(int[] destData, int destOffset, int srcARGB) {
        blendInternal(destData, destOffset, srcARGB);
    }
    
    void blend(int[] destData, int destOffset, int srcARGB, int extraAlpha) {
        blendInternal(destData, destOffset, srcARGB, extraAlpha);
    }
    
    void blendRow(int[] destData, int destOffset, int srcARGB, int numPixels) {
        if ((srcARGB >>> 24) == 0xff) {
            for (int i = 0; i < numPixels; i++) {
                blendInternalOpaque(destData, destOffset++, srcARGB);
            }
        }
        else {
            for (int i = 0; i < numPixels; i++) {
                blendInternal(destData, destOffset++, srcARGB);
            }
        }
    }
    
    void blend(int[] srcData, int srcScanSize, boolean srcOpaque, 
        int srcX, int srcY, int srcWidth, int srcHeight, int srcOffset, 
        int u, int v, int du, int dv, 
        boolean rotation,
        boolean renderBilinear, int renderAlpha,
        int[] destData, int destScanSize, int destOffset, int numPixels, int numRows)
    {
        if (renderAlpha <= 0) {
            return;
        }
        
        //
        // Pre-calc for bilinear scaling
        //
        
        int offsetTop = 0;
        int offsetBottom = 0;
        if (!rotation && renderBilinear) {
            int imageY = v >> 16;
            if (srcOpaque) {
                if (imageY >= srcHeight - 1) {
                    offsetTop = srcX + (srcY + srcHeight - 1) * srcScanSize;
                    offsetBottom = offsetTop;
                }
                else if (imageY < 0) {
                    offsetTop = srcX + srcY * srcScanSize;
                    offsetBottom = offsetTop;
                }
                else if ((v & 0xffff) == 0) {
                    offsetTop = srcOffset - (u >> 16);
                    offsetBottom = offsetTop;
                }
                else {
                    offsetTop = srcOffset - (u >> 16);
                    offsetBottom = offsetTop + srcScanSize;
                }
            }
            else {
                if (imageY >= 0) {
                    if (imageY < srcHeight - 1) {
                        offsetTop = srcOffset - (u >> 16);
                        offsetBottom = offsetTop + srcScanSize;
                    }
                    else if (imageY == srcHeight - 1) {
                        offsetTop = srcOffset - (u >> 16);
                        offsetBottom = -1;
                    }
                    else {
                        offsetTop = -1;
                        offsetBottom = -1;
                    }
                }
                else if (imageY == -1) {
                    offsetTop = -1;
                    offsetBottom = srcX + srcY * srcScanSize;
                }
                else {
                    offsetTop = -1;
                    offsetBottom = -1;
                }
            }
        }
 
        
        //
        // Opaque blending 
        //        
        
        if (renderAlpha == 255) {
            if (rotation) {
                // TODO: handle opaque images seperately?
                if (renderBilinear) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData, 
                                srcScanSize, srcX, srcY, srcWidth, srcHeight, u, v);
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendInternalOpaque(destData, destOffset, srcARGB);
                        }
                        else if (srcAlpha != 0) {
                            blendInternal(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
                else {
                    srcOffset = srcX + srcY * srcScanSize;
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = srcData[srcOffset + (u >> 16) + (v >> 16) * srcScanSize];
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendInternalOpaque(destData, destOffset, srcARGB);
                        }
                        else if (srcAlpha != 0) {
                            blendInternal(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
            }
            else if (renderBilinear) {
                if (srcOpaque) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearOpaque(srcData, 
                            offsetTop, offsetBottom, u, v, srcWidth);
                        blendInternalOpaque(destData, destOffset, srcARGB);
                        destOffset++;
                        u += du;
                    }
                }
                else {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendInternalOpaque(destData, destOffset, srcARGB);
                        }
                        else if (srcAlpha != 0) {
                            blendInternal(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                    }
                }
            }
            else {
                for (int y = 0; y < numRows; y++) {
                    if (du != (1 << 16)) {
                        int offset = u & 0xffff;
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                blendInternalOpaque(destData, destOffset + i, srcARGB);
                                offset += du;
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                int srcAlpha = srcARGB >>> 24;
                                if (srcAlpha == 0xff) {
                                    blendInternalOpaque(destData, destOffset + i, srcARGB);
                                }
                                else if (srcAlpha != 0) {
                                    blendInternal(destData, destOffset + i, srcARGB);
                                }
                                offset += du;
                            }
                        }
                    }
                    else {
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                blendInternalOpaque(destData, destOffset + i, srcARGB);
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                int srcAlpha = srcARGB >>> 24;
                                if (srcAlpha == 0xff) {
                                    blendInternalOpaque(destData, destOffset + i, srcARGB);
                                }
                                else if (srcAlpha != 0) {
                                    blendInternal(destData, destOffset + i, srcARGB);
                                }
                            }
                        }
                    }
                    destOffset += destScanSize;
                    srcOffset += srcScanSize;
                }
            }
        }
        
        //
        // Translucent blending 
        //
            
        else {
            if (rotation) {
                // TODO: handle opaque images seperately?
                if (renderBilinear) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData, 
                                srcScanSize, srcX, srcY, srcWidth, srcHeight, u, v);
                        if ((srcARGB >>> 24) > 0) {
                            blendInternal(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
                else {
                    srcOffset = srcX + srcY * srcScanSize;
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = srcData[srcOffset + (u >> 16) + (v >> 16) * srcScanSize];
                        if ((srcARGB >>> 24) > 0) {
                            blendInternal(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
            }
            else if (renderBilinear) {
                if (srcOpaque) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearOpaque(srcData, 
                            offsetTop, offsetBottom, u, v, srcWidth);
                        blendInternalOpaque(destData, destOffset, srcARGB, renderAlpha);
                        destOffset++;
                        u += du;
                    }
                }
                else {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        if ((srcARGB >>> 24) > 0) {
                            blendInternal(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                    }
                }
            }
            else {
                for (int y = 0; y < numRows; y++) {
                    if (du != (1 << 16)) {
                        int offset = u & 0xffff;
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                blendInternalOpaque(destData, destOffset + i, srcARGB, renderAlpha);
                                offset += du;
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                if ((srcARGB >>> 24) > 0) {
                                    blendInternal(destData, destOffset + i, srcARGB, renderAlpha);
                                }
                                offset += du;
                            }
                        }
                    }
                    else {
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                blendInternalOpaque(destData, destOffset + i, srcARGB, renderAlpha);
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                if ((srcARGB >>> 24) > 0) {
                                    blendInternal(destData, destOffset + i, srcARGB, renderAlpha);
                                }
                            }
                        }
                    }
                    destOffset += destScanSize;
                    srcOffset += srcScanSize;
                }
            }
        }
    }        
}


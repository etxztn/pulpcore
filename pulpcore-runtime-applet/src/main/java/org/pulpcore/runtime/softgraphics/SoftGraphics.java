/*
    Copyright (c) 2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime.softgraphics;

import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Texture;
import org.pulpcore.math.AffineTransform;
import org.pulpcore.math.FixedMath;
import org.pulpcore.math.MathUtil;

public class SoftGraphics extends Graphics {

    // Line drawing options
    private static final boolean CENTER_PIXEL = true;
    private static final boolean SWAP_POINTS = true;

    // Clipping for drawLine
    private static final int CLIP_CODE_LEFT = 8;
    private static final int CLIP_CODE_RIGHT = 4;
    private static final int CLIP_CODE_ABOVE = 2;
    private static final int CLIP_CODE_BELOW = 1;

    // Surface data
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final int[] surfaceData;
    private final int surfaceDataOffset;
    private final int surfaceScanWidth;
    private final boolean surfaceHasAlpha;

    // A clipped object
    private int objectX;
    private int objectY;
    private int objectWidth;
    private int objectHeight;

    // Scan line
    private int scanY;
    private int scanStartX;
    private int scanEndX;

    private Composite composite;

    /** The current color blended with the current alpha. */
    private int premultipliedSrcColor;
    private boolean premultipliedSrcColorNeedsUpdate = true;

    public SoftGraphics(SoftTexture surface) {
        surfaceWidth = surface.getWidth();
        surfaceHeight = surface.getHeight();
        surfaceData = surface.getRawData();
        surfaceDataOffset = surface.getDataOffset();
        surfaceScanWidth = surface.getScanWidth();
        surfaceHasAlpha = !surface.isOpaque();
        
        reset();
    }

    public int[] getSurfaceData() {
        return surfaceData;
    }

    public boolean getSurfaceHasAlpha() {
        return surfaceHasAlpha;
    }

    public int getSurfaceOffset() {
        return surfaceDataOffset;
    }

    public int getSurfaceScanWidth() {
        return surfaceScanWidth;
    }

    @Override
    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    @Override
    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    @Override
    public void setBlendMode(BlendMode blendMode) {
        if (getBlendMode() != blendMode) {
            super.setBlendMode(blendMode);
            boolean destOpaque = !surfaceHasAlpha;
            switch (blendMode) {
                case CLEAR:
                    this.composite = new CompositeAdd(destOpaque);
                    break;
                case SRC:
                    this.composite = new CompositeSrc(destOpaque);
                    break;
                case DST:
                    this.composite = new CompositeDst(destOpaque);
                    break;
                case SRC_OVER: default:
                    this.composite = new CompositeSrcOver(destOpaque);
                    break;
                case SRC_IN:
                    this.composite = new CompositeSrcIn(destOpaque);
                    break;
                case SRC_ATOP:
                    this.composite = new CompositeSrcAtop(destOpaque);
                    break;
                case SRC_OUT:
                    this.composite = new CompositeSrcOut(destOpaque);
                    break;
                case DST_OVER:
                    this.composite = new CompositeDstOver(destOpaque);
                    break;
                case DST_IN:
                    this.composite = new CompositeDstIn(destOpaque);
                    break;
                case DST_ATOP:
                    this.composite = new CompositeDstAtop(destOpaque);
                    break;
                case DST_OUT:
                    this.composite = new CompositeDstOut(destOpaque);
                    break;
                case ADD:
                    this.composite = new CompositeAdd(destOpaque);
                    break;
                case MULT:
                    this.composite = new CompositeMult(destOpaque);
                    break;
//                case XOR:
//                    this.composite = new CompositeXor(destOpaque);
//                    break;
            }
        }
    }

    private void clipObject(int x, int y, int w, int h) {
        int clipX = getClipX();
        int clipY = getClipY();
        int clipWidth = getClipWidth();
        int clipHeight = getClipHeight();
        
        if (x < clipX) {
            w -= clipX - x;
            x = clipX;
        }
        if (y < clipY) {
            h -= clipY - y;
            y = clipY;
        }
        if (x + w > clipX + clipWidth) {
            w = clipX + clipWidth - x;
        }
        if (y + h > clipY + clipHeight) {
            h = clipY + clipHeight - y;
        }

        objectX = x;
        objectY = y;
        objectWidth = w;
        objectHeight = h;
    }

    private boolean isClippedToIntegerBounds(int fx, int fy, int fw, int fh) {
        return (FixedMath.toFixed(objectX) >= fx &&
                FixedMath.toFixed(objectY) >= fy &&
                FixedMath.toFixed(objectX + objectWidth) <= fx + fw &&
                FixedMath.toFixed(objectY + objectHeight) <= fy + fh);
    }

    @Override
    public void setAlpha(int alpha) {
        if (getAlpha() != alpha) {
            super.setAlpha(alpha);
            premultipliedSrcColorNeedsUpdate = true;
        }
    }
    
    @Override
    public void setColor(int argbColor) {
        if (getColor() != argbColor) {
            super.setColor(argbColor);
            premultipliedSrcColorNeedsUpdate = true;
        }
    }

    private int getPremultipliedSrcColor() {
        if (premultipliedSrcColorNeedsUpdate) {
            int color = getColor();
            int alpha = getAlpha();
            int newAlpha = ((color >>> 24) * alpha + 127) / 255;
            color = Color.rgba(color, newAlpha);
            premultipliedSrcColor = Color.convert(Color.Format.ARGB, Color.Format.ARGB_PREMULTIPLIED, color);
            premultipliedSrcColorNeedsUpdate = false;
        }
        return premultipliedSrcColor;
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        internalDrawLine(x1, y1, x2, y2, false);
    }

    @Override
    public void fillRect(float w, float h) {
        if (w == 0 || h == 0) {
            return;
        }

        AffineTransform t = getCurrentTransform();
        int type = t.getType();
        if ((type & AffineTransform.TYPE_ROTATE) != 0) {
            internalFillRotatedRect(w, h);
        }
        else {
            internalFillRect(w, h);
        }
    }

    //
    // Rendering
    //

    @Override
    public void drawTexture(Texture texture) {

        if (texture != null) {
            if (!(texture instanceof SoftTexture)) {
                throw new IllegalArgumentException("Texture not compatible with SoftGraphics");
            }

            AffineTransform t = getCurrentTransform();
            int type = t.getType();

            if (type == AffineTransform.TYPE_IDENTITY || type == AffineTransform.TYPE_TRANSLATE) {
                int x = FixedMath.toFixed(t.getTranslateX());
                int y = FixedMath.toFixed(t.getTranslateY());
                if (FixedMath.fracPart(x) != 0 || FixedMath.fracPart(y) != 0) {
                    drawScaledTexture((SoftTexture)texture);
                }
                else {
                    drawIdentityTexture((SoftTexture)texture);
                }
            }
            else if ((type & AffineTransform.TYPE_ROTATE) != 0) {
                drawRotatedTexture((SoftTexture)texture);
            }
            else {
                drawScaledTexture((SoftTexture)texture);
            }
        }
    }

    private void drawIdentityTexture(SoftTexture texture) {
        AffineTransform t = getCurrentTransform();
        int x = (int)(t.getTranslateX());
        int y = (int)(t.getTranslateY());

        clipObject(x, y, texture.getWidth(), texture.getHeight());
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }

        int srcWidth = texture.getWidth();
        int srcHeight = texture.getHeight();
        int[] srcData = texture.getRawData();
        int srcScanSize = texture.getScanWidth();
        int surfaceOffset = surfaceDataOffset + objectX + objectY * surfaceScanWidth;
        int u = ((objectX - x) << 16);
        int v = ((objectY - y) << 16);
        int alpha = getAlpha();

        boolean quickBlendSrc = alpha == 0xff && getBlendMode() == BlendMode.SRC && (texture.isOpaque() || surfaceHasAlpha);
        boolean quickBlendSrcOver = alpha == 0xff && getBlendMode() == BlendMode.SRC_OVER && texture.isOpaque();

        if (quickBlendSrc || quickBlendSrcOver) {
            // Fastest case - don't use the compositor.
            // Great optimization for background rendering.
            int srcOffset = texture.getDataOffset() + (u >> 16) + (v >> 16) * srcScanSize;
            for (int j = 0; j < objectHeight; j++) {
                System.arraycopy(srcData, srcOffset, surfaceData, surfaceOffset, objectWidth);
                srcOffset += srcScanSize;
                surfaceOffset += surfaceScanWidth;
            }
        }
        else {
            composite.blend(srcData, srcScanSize, texture.isOpaque(), getEdgeClamp(),
                texture.getDataOffset(), srcWidth, srcHeight,
                u, v,
                (1 << 16), 0,
                false,
                false, alpha,
                surfaceData, surfaceScanWidth, surfaceOffset,
                objectWidth, objectHeight);
        }
    }

    private void drawScaledTexture(SoftTexture texture) {
        AffineTransform t = getCurrentTransform();
        float sx = t.getScaleX();
        float sy = t.getScaleY();

        if (sx == 0 || sy == 0) {
            return;
        }

        int fW = FixedMath.toFixed(sx * texture.getWidth());
        int fH = FixedMath.toFixed(sy * texture.getHeight());
        int du = FixedMath.toFixed(1 / sx);
        int dv = FixedMath.toFixed(1 / sy);

        drawScaledTexture(texture, fW, fH, du, dv);
    }

    private void drawScaledTexture(SoftTexture texture, int fW, int fH, int du, int dv) {
        if (fW == 0 || fH == 0) {
            return;
        }
        
        boolean bilinear = getInterpolation() == Interpolation.BILINEAR;
        int edgeClamp = getEdgeClamp();
        
        int srcWidth = texture.getWidth();
        int srcHeight = texture.getHeight();

        AffineTransform t = getCurrentTransform();
        int fX = FixedMath.toFixed(t.getTranslateX());
        int fY = FixedMath.toFixed(t.getTranslateY());

        int x;
        int y;
        int w;
        int h;
        if (bilinear) {
            if (texture.isOpaque() || (edgeClamp & EDGE_CLAMP_LEFT) != 0) {
                x = FixedMath.toIntRound(fX);
            }
            else {
                x = FixedMath.toIntFloor(fX);
            }
            if (texture.isOpaque() || (edgeClamp & EDGE_CLAMP_TOP) != 0) {
                y = FixedMath.toIntRound(fY);
            }
            else {
                y = FixedMath.toIntFloor(fY);
            }
            if (texture.isOpaque() || (edgeClamp & EDGE_CLAMP_RIGHT) != 0) {
                w = FixedMath.toIntRound(fX + fW) - x;
            }
            else {
                w = FixedMath.toIntCeil(fX + fW) - x;
            }
            if (texture.isOpaque() || (edgeClamp & EDGE_CLAMP_BOTTOM) != 0) {
                h = FixedMath.toIntRound(fY + fH) - y;
            }
            else {
                h = FixedMath.toIntCeil(fY + fH) - y;
            }
        }
        else {
            x = FixedMath.toIntRound(fX);
            y = FixedMath.toIntRound(fY);
            w = FixedMath.toIntRound(fX + fW) - x;
            h = FixedMath.toIntRound(fY + fH) - y;
            fX = FixedMath.toFixed(x);
            fY = FixedMath.toFixed(y);
            fW = FixedMath.toFixed(w);
            fH = FixedMath.toFixed(h);
            du = FixedMath.div(FixedMath.toFixed(srcWidth), fW);
            dv = FixedMath.div(FixedMath.toFixed(srcHeight), fH);
        }

        clipObject(x, y, w, h);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }

        // Adjust for internal drawing routines that put integer locations in the middle of the
        // pixel.
        if (bilinear) {
            fX -= FixedMath.ONE_HALF;
            fY -= FixedMath.ONE_HALF;
        }

        int[] srcData = texture.getRawData();
        int srcScanSize = texture.getScanWidth();
        int surfaceOffset = surfaceDataOffset + objectX + objectY * surfaceScanWidth;
        int u = FixedMath.mul(FixedMath.toFixed(objectX) - fX, du);
        int v = FixedMath.mul(FixedMath.toFixed(objectY) - fY, dv);

        // ???
        if (bilinear) {
            u -= FixedMath.ONE_HALF;
            v -= FixedMath.ONE_HALF;
        }
        else {
            u += du/2;
            v += dv/2;
        }

        for (int j = 0; j < objectHeight; j++) {
            composite.blend(srcData, srcScanSize, texture.isOpaque(), edgeClamp,
                texture.getDataOffset(), srcWidth, srcHeight,
                u, v,
                du, 0,
                false,
                bilinear, getAlpha(),
                surfaceData, surfaceScanWidth, surfaceOffset,
                objectWidth, 1);

            v += dv;
            surfaceOffset += surfaceScanWidth;
        }
    }

    private void drawRotatedTexture(SoftTexture texture) {
        boolean bilinear = getInterpolation() == Interpolation.BILINEAR;
        int edgeClamp = getEdgeClamp();
        
        AffineTransform t = getCurrentTransform();

        int srcWidth = texture.getWidth();
        int srcHeight = texture.getHeight();

        // Find the bounding rectangle
        float x1 = t.getTranslateX();
        float y1 = t.getTranslateY();
        float x2 = t.getScaleX() * srcWidth;
        float y2 = t.getShearY() * srcWidth;
        float x3 = t.getShearX() * srcHeight;
        float y3 = t.getScaleY() * srcHeight;
        float x4 = x2 + x3;
        float y4 = y2 + y3;

        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        x4 += x1;
        y4 += y1;

        if (!bilinear) {
            x1 = Math.round(x1);
            y1 = Math.round(y1);
            x2 = Math.round(x2);
            y2 = Math.round(y2);
            x3 = Math.round(x3);
            y3 = Math.round(y3);
            x4 = Math.round(x4);
            y4 = Math.round(y4);
        }

        float boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        float boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        float boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        float boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );

        int x = (int)Math.floor(boundsX1) - 1;
        int y = (int)Math.floor(boundsY1) - 1;
        int w = (int)Math.ceil(boundsX2) - x + 2;
        int h = (int)Math.ceil(boundsY2) - y + 2;

        // Clip
        clipObject(x, y, w, h);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }

        // Adjust for internal drawing routines that put integer locations in the middle of the
        // pixel.
        if (bilinear) {
            x1 -= 0.5f;
            y1 -= 0.5f;
            x2 -= 0.5f;
            y2 -= 0.5f;
            x3 -= 0.5f;
            y3 -= 0.5f;
            x4 -= 0.5f;
            y4 -= 0.5f;
        }

        // Calc deltas
        float ou = objectX - x1;
        float ov = objectY - y1;
        float det = t.getDeterminant();
        if (det == 0) {
            return;
        }
        float duX = t.getScaleY() / det;
        float dvX = -t.getShearY() / det;
        float duY = -t.getShearX() / det;
        float dvY = t.getScaleX() / det;
        float u = (ou * t.getScaleY() - ov * t.getShearX()) / det;
        float v = (ov * t.getScaleX() - ou * t.getShearY()) / det;

        // Start Render
        int[] srcData = texture.getRawData();
        int srcScanSize = texture.getScanWidth();
        int surfaceOffset = surfaceDataOffset + objectX + objectY * surfaceScanWidth;

        if (bilinear) {
            // ???
            u -= 0.5;
            v -= 0.5;

            // Anti-aliasing
            float ud = Math.max(Math.abs(duX), Math.abs(duY));
            float vd = Math.max(Math.abs(dvX), Math.abs(dvY));
            float uMin = (edgeClamp & EDGE_CLAMP_LEFT)   == 0 ? -ud : 0;
            float uMax = (edgeClamp & EDGE_CLAMP_RIGHT)  == 0 ? srcWidth + ud : srcWidth;
            float vMin = (edgeClamp & EDGE_CLAMP_TOP)    == 0 ? -vd : 0;
            float vMax = (edgeClamp & EDGE_CLAMP_BOTTOM) == 0 ? srcHeight + vd : srcHeight;
            // HACK: Fixes an off-by-one error at exactly (0.5, 0.5)
            // See RectTest at (0.5, 0.5)
            // NOT Tested since ported to SoftGraphics
            float xOffsetClamp = 1.0f / FixedMath.ONE;
            float yOffsetClamp = 1.0f / FixedMath.ONE;
            if ((edgeClamp & (EDGE_CLAMP_LEFT | EDGE_CLAMP_TOP)) == 0) {
                x1 = t.transformX(uMin, vMin);
                y1 = t.transformY(uMin, vMin);
            }
            else {
                x1 += xOffsetClamp;
                y1 += yOffsetClamp;
            }
            if ((edgeClamp & (EDGE_CLAMP_RIGHT | EDGE_CLAMP_TOP)) == 0) {
                x2 = t.transformX(uMax, vMin);
                y2 = t.transformY(uMax, vMin);
            }
            else {
                x2 += xOffsetClamp;
                y2 += yOffsetClamp;
            }
            if ((edgeClamp & (EDGE_CLAMP_LEFT | EDGE_CLAMP_BOTTOM)) == 0) {
               x3 = t.transformX(uMin, vMax);
               y3 = t.transformY(uMin, vMax);
            }
            else {
                x3 += xOffsetClamp;
                y3 += yOffsetClamp;
            }
            if ((edgeClamp & (EDGE_CLAMP_RIGHT | EDGE_CLAMP_BOTTOM)) == 0) {
                x4 = t.transformX(uMax, vMax);
                y4 = t.transformY(uMax, vMax);
            }
            else {
                x4 += xOffsetClamp;
                y4 += yOffsetClamp;
            }
        }
        else {
            u += (duX + duY) / 2;
            v += (dvX + dvY) / 2;
        }

        // Fixed Point from here on out
        int fx1 = FixedMath.toFixed(x1);
        int fy1 = FixedMath.toFixed(y1);
        int fx2 = FixedMath.toFixed(x2);
        int fy2 = FixedMath.toFixed(y2);
        int fx3 = FixedMath.toFixed(x3);
        int fy3 = FixedMath.toFixed(y3);
        int fx4 = FixedMath.toFixed(x4);
        int fy4 = FixedMath.toFixed(y4);
        int fu = FixedMath.toFixed(u);
        int fv = FixedMath.toFixed(v);
        int fduX = FixedMath.toFixed(duX);
        int fduY = FixedMath.toFixed(duY);
        int fdvX = FixedMath.toFixed(dvX);
        int fdvY = FixedMath.toFixed(dvY);

        for (int j = 0; j < objectHeight; j++) {

            // Scan convert
            startScan((objectY + j) << 16);
            scan(fx1, fy1, fx2, fy2);
            scan(fx1, fy1, fx3, fy3);
            scan(fx2, fy2, fx4, fy4);
            scan(fx3, fy3, fx4, fy4);

            // Check bounds
            if (hasScan()) {
                scanStartX = FixedMath.toIntCeil(scanStartX) - objectX;
                scanEndX = FixedMath.toIntCeil(scanEndX) - objectX;

                scanStartX = MathUtil.clamp(scanStartX, 0, objectWidth);
                scanEndX = MathUtil.clamp(scanEndX, 0, objectWidth);

                if (scanStartX < scanEndX) {
                    // Draw
                    composite.blend(srcData, srcScanSize, texture.isOpaque(), edgeClamp,
                        texture.getDataOffset(), srcWidth, srcHeight,
                        fu + scanStartX * fduX, fv + scanStartX * fdvX,
                        fduX, fdvX,
                        true,
                        bilinear, getAlpha(),
                        surfaceData, surfaceScanWidth, surfaceOffset + scanStartX,
                        scanEndX - scanStartX, 1);
                }
            }
            fu += fduY;
            fv += fdvY;
            surfaceOffset += surfaceScanWidth;
        }
    }

    private void startScan(int y) {
        scanY = y;
        scanStartX = Integer.MAX_VALUE;
        scanEndX = Integer.MIN_VALUE;
    }

    private boolean hasScan() {
        return (scanStartX != Integer.MAX_VALUE && scanEndX != Integer.MIN_VALUE);
    }

    private void scan(int x1, int y1, int x2, int y2) {
        if ((scanY < y1) == (scanY < y2)) {
            // Out of bounds, or y1 == y2
            // Do nothing
        }
        else {
            int x = x1 + FixedMath.mulDiv(scanY - y1, x2 - x1, y2 - y1);
            if (x < scanStartX) {
                scanStartX = x;
            }
            if (x > scanEndX) {
                scanEndX = x;
            }
        }
    }

    //
    // Internal Line Drawing
    //

    /**
        Draws a line using the current color.
        OPTIMIZE: currently there is a virtual call per pixel
    */
    private void internalDrawLine(float ox1, float oy1, float ox2, float oy2, boolean solidFirstPixel) {

        AffineTransform transform = getCurrentTransform();
        int x1, y1, x2, y2;

        if (transform.getType() == AffineTransform.TYPE_IDENTITY) {
            x1 = FixedMath.toFixed(ox1);
            y1 = FixedMath.toFixed(oy1);
            x2 = FixedMath.toFixed(ox2);
            y2 = FixedMath.toFixed(oy2);
        }
        else {
            x1 = FixedMath.toFixed(transform.transformX(ox1, oy1));
            y1 = FixedMath.toFixed(transform.transformY(ox1, oy1));
            x2 = FixedMath.toFixed(transform.transformX(ox2, oy2));
            y2 = FixedMath.toFixed(transform.transformY(ox2, oy2));
        }

        int dx = x2 - x1;
        int dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            // Do nothing
            return;
        }
        
        int srcColorBlended = getPremultipliedSrcColor();

        if (solidFirstPixel) {
            // Make sure the first pixel is solid
            drawPixel(srcColorBlended, FixedMath.toInt(x1), FixedMath.toInt(y1));

            // Draw the last pixel (otherwise it won't get drawn)
            if (x1 != x2 || y1 != y2) {
                drawPixel(srcColorBlended, FixedMath.toInt(x2), FixedMath.toInt(y2));
            }
        }

        // Make integer locations the center of the pixel
        if (CENTER_PIXEL) {
            x1 += FixedMath.ONE_HALF;
            y1 += FixedMath.ONE_HALF;
            x2 += FixedMath.ONE_HALF;
            y2 += FixedMath.ONE_HALF;
        }

        // Clip - Sutherland-Cohen algorithm
        // This is used as an optimization to prevent off-screen lines from being drawn.
        // Real clipping happens in the drawPixel() functions.
        // Using an extra 1-pixel boundary because of error I saw with the Sketch demo.
        int xmin = FixedMath.toFixed(getClipX() - 1);
        int xmax = xmin + FixedMath.toFixed(getClipWidth() + 1);
        int ymin = FixedMath.toFixed(getClipY() - 1);
        int ymax = ymin + FixedMath.toFixed(getClipHeight() + 1);

        int clipCode1 =
            (x1 < xmin ? CLIP_CODE_LEFT : 0) |
            (x1 > xmax ? CLIP_CODE_RIGHT : 0) |
            (y1 < ymin ? CLIP_CODE_ABOVE : 0) |
            (y1 > ymax ? CLIP_CODE_BELOW : 0);
        int clipCode2 =
            (x2 < xmin ? CLIP_CODE_LEFT : 0) |
            (x2 > xmax ? CLIP_CODE_RIGHT : 0) |
            (y2 < ymin ? CLIP_CODE_ABOVE : 0) |
            (y2 > ymax ? CLIP_CODE_BELOW : 0);

        while ((clipCode1 | clipCode2) != 0) {

            if ((clipCode1 & clipCode2) != 0) {
                // Completely outside the clip bounds - do nothing
                return;
            }

            if (clipCode1 != 0) {
                if ((clipCode1 & CLIP_CODE_LEFT) != 0) {
                    y1 += FixedMath.mulDiv(xmin-x1, dy, dx);
                    x1 = xmin;
                }
                else if ((clipCode1 & CLIP_CODE_RIGHT) != 0) {
                    y1 += FixedMath.mulDiv(xmax-x1, dy, dx);
                    x1 = xmax;
                }
                else if ((clipCode1 & CLIP_CODE_ABOVE) != 0) {
                    x1 += FixedMath.mulDiv(ymin-y1, dx, dy);
                    y1 = ymin;
                }
                else if ((clipCode1 & CLIP_CODE_BELOW) != 0) {
                    x1 += FixedMath.mulDiv(ymax-y1, dx, dy);
                    y1 = ymax;
                }
                clipCode1 =
                    (x1 < xmin ? CLIP_CODE_LEFT : 0) |
                    (x1 > xmax ? CLIP_CODE_RIGHT : 0) |
                    (y1 < ymin ? CLIP_CODE_ABOVE : 0) |
                    (y1 > ymax ? CLIP_CODE_BELOW : 0);
            }
            else if (clipCode2 != 0) {
                if ((clipCode2 & CLIP_CODE_LEFT) != 0) {
                    y2 += FixedMath.mulDiv(xmin-x2, dy, dx);
                    x2 = xmin;
                }
                else if ((clipCode2 & CLIP_CODE_RIGHT) != 0) {
                    y2 += FixedMath.mulDiv(xmax-x2, dy, dx);
                    x2 = xmax;
                }
                else if ((clipCode2 & CLIP_CODE_ABOVE) != 0) {
                    x2 += FixedMath.mulDiv(ymin-y2, dx, dy);
                    y2 = ymin;
                }
                else if ((clipCode2 & CLIP_CODE_BELOW) != 0) {
                    x2 += FixedMath.mulDiv(ymax-y2, dx, dy);
                    y2 = ymax;
                }
                clipCode2 =
                    (x2 < xmin ? CLIP_CODE_LEFT : 0) |
                    (x2 > xmax ? CLIP_CODE_RIGHT : 0) |
                    (y2 < ymin ? CLIP_CODE_ABOVE : 0) |
                    (y2 > ymax ? CLIP_CODE_BELOW : 0);
            }
            dx = x2 - x1;
            dy = y2 - y1;
        }

        if (dx != 0 || dy != 0) {
            int dxabs = Math.abs(dx);
            int dyabs = Math.abs(dy);
            int currAlpha;
            if (SWAP_POINTS) {
                if ((dxabs >= dyabs && x1 > x2) ||
                    (dyabs > dxabs && y1 > y2))
                {
                    int t = x1;
                    x1 = x2;
                    x2 = t;
                    t = y1;
                    y1 = y2;
                    y2 = t;
                    dx = -dx;
                    dy = -dy;
                }
            }

            if (dxabs >= dyabs) {
                // Line is more horizontal than vertical
                // Or a diagonal
                if (dxabs < FixedMath.ONE && FixedMath.floor(x1) == FixedMath.floor(x2)) {
                    // One pixel
                    currAlpha = dxabs >> 8;
                    drawWuPixelHorizontal(srcColorBlended, x1, y1, currAlpha);
                }
                else {
                    // First pixel and last pixel
                    if (!solidFirstPixel) {
                        currAlpha = 0xff - ((x1 >> 8) & 0xff);
                        drawWuPixelHorizontal(srcColorBlended, x1, y1, currAlpha);
                        currAlpha = ((x2 >> 8) & 0xff);
                        drawWuPixelHorizontal(srcColorBlended, x2, y2, currAlpha);
                    }
                    // Line
                    int grad = FixedMath.div(dy, dxabs);
                    int d = dx > 0 ? FixedMath.ONE : -FixedMath.ONE;
                    int limit = Math.abs(FixedMath.toIntFloor(x2) - FixedMath.toIntFloor(x1)) - 1;
                    for (int i = 0; i < limit; i++) {
                        x1 += d;
                        y1 += grad;
                        drawWuPixelHorizontal(srcColorBlended, x1, y1, 0xff);
                    }
                }
            }
            else {
                // Line is more vertical than horizontal
                if (dyabs < FixedMath.ONE && FixedMath.floor(y1) == FixedMath.floor(y2)) {
                    // One pixel
                    currAlpha = dyabs >> 8;
                    drawWuPixelVertical(srcColorBlended, x1, y1, currAlpha);
                }
                else {
                    // First first and last pixel
                    if (!solidFirstPixel) {
                        currAlpha = 0xff - ((y1 >> 8) & 0xff);
                        drawWuPixelVertical(srcColorBlended, x1, y1, currAlpha);
                        currAlpha = ((y2 >> 8) & 0xff);
                        drawWuPixelVertical(srcColorBlended, x2, y2, currAlpha);
                    }
                    // Line
                    int grad = FixedMath.div(dx, dyabs);
                    int d = dy > 0 ? FixedMath.ONE : -FixedMath.ONE;
                    int limit = Math.abs(FixedMath.toIntFloor(y2) - FixedMath.toIntFloor(y1)) - 1;
                    for (int i = 0; i < limit; i++) {
                        x1 += grad;
                        y1 += d;
                        drawWuPixelVertical(srcColorBlended, x1, y1, 0xff);
                    }
                }
            }
        }
    }

    /**
        Draws a pixel at the specified integer (x,y) location using the current color.
        Ignores the transform. Respects the clip.
    */
    private void drawPixel(int srcColorBlended, int x, int y) {
        int clipX = getClipX();
        int clipY = getClipY();
        int clipWidth = getClipWidth();
        int clipHeight = getClipHeight();
        if (x >= clipX && x < clipX + clipWidth &&
            y >= clipY && y < clipY + clipHeight)
        {
            int surfaceOffset = surfaceDataOffset + x + y * surfaceScanWidth;
            composite.blend(surfaceData, surfaceOffset, srcColorBlended);
        }
    }

    /*
        For Wu's line drawing algorithm.
        Ignores the transform. Respects the clip.
        Draws two pixels, one at (fx, floor(fy)) and another at (fx, ceil(fy))
    */
    private void drawWuPixelHorizontal(int srcColorBlended, int fx, int fy, int extraAlpha) {
        if (extraAlpha <= 0) {
            return;
        }
        int clipX = getClipX();
        int clipY = getClipY();
        int clipWidth = getClipWidth();
        int clipHeight = getClipHeight();
        int x = FixedMath.toIntFloor(fx);
        if (CENTER_PIXEL) {
            fy -= FixedMath.ONE_HALF;
        }
        if (x >= clipX && x < clipX + clipWidth) {
            int y1 = FixedMath.toIntFloor(fy);
            int surfaceOffset = surfaceDataOffset + x + y1 * surfaceScanWidth;
            if (y1 >= clipY && y1 < clipY + clipHeight) {
                int wuAlpha = 0xff - ((fy >> 8) & 0xff);
                int pixelAlpha = (extraAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset, srcColorBlended, pixelAlpha);
            }

            int y2 = FixedMath.toIntCeil(fy);
            if (y1 != y2 && y2 >= clipY && y2 < clipY + clipHeight) {
                int wuAlpha = ((fy >> 8) & 0xff);
                int pixelAlpha = (extraAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset + surfaceScanWidth, srcColorBlended,
                    pixelAlpha);
            }
        }
    }

    /*
        For Wu's line drawing algorithm.
        Ignores the transform, but respects the clip.
        Draws two pixels, one at (floor(fx), fy) and another at (ceil(fx), fy)
    */
    private void drawWuPixelVertical(int srcColorBlended, int fx, int fy, int extraAlpha) {
        if (extraAlpha <= 0) {
            return;
        }
        int clipX = getClipX();
        int clipY = getClipY();
        int clipWidth = getClipWidth();
        int clipHeight = getClipHeight();
        int y = FixedMath.toIntFloor(fy);
        if (CENTER_PIXEL) {
            fx -= FixedMath.ONE_HALF;
        }
        if (y >= clipY && y < clipY + clipHeight) {
            int x1 = FixedMath.toIntFloor(fx);
            int surfaceOffset = surfaceDataOffset + x1 + y * surfaceScanWidth;
            if (x1 >= clipX && x1 < clipX + clipWidth) {
                int wuAlpha = 0xff - ((fx >> 8) & 0xff);
                int pixelAlpha = (extraAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset, srcColorBlended, pixelAlpha);
            }

            int x2 = FixedMath.toIntCeil(fx);
            if (x1 != x2 && x2 >= clipX && x2 < clipX + clipWidth) {
                int wuAlpha = ((fx >> 8) & 0xff);
                int pixelAlpha = (extraAlpha * wuAlpha + 0xff) >> 8;
                composite.blend(surfaceData, surfaceOffset + 1, srcColorBlended, pixelAlpha);
            }
        }
    }

    //
    // Rectangle filling
    //

    private void internalFillRect(float width, float height) {

        AffineTransform transform = getCurrentTransform();

        int fw = FixedMath.toFixed(transform.getScaleX() * width);
        int fh = FixedMath.toFixed(transform.getScaleY() * height);

        int x1 = FixedMath.toFixed(transform.getTranslateX());
        int y1 = FixedMath.toFixed(transform.getTranslateY());
        int x2 = x1 + fw;
        int y2 = y1 + fh;

        int boundsX1 = Math.min(x1, x2);
        int boundsY1 = Math.min(y1, y2);
        int boundsX2 = Math.max(x1, x2);
        int boundsY2 = Math.max(y1, y2);

        int boundsX = FixedMath.toIntFloor(boundsX1);
        int boundsY = FixedMath.toIntFloor(boundsY1);
        int boundsW = FixedMath.toIntCeil(boundsX2) - boundsX;
        int boundsH = FixedMath.toIntCeil(boundsY2) - boundsY;

        // Clip
        clipObject(boundsX, boundsY, boundsW, boundsH);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }
        
        int srcColorBlended = getPremultipliedSrcColor();

        // Draw
        int surfaceOffset = surfaceDataOffset + objectX + objectY * surfaceScanWidth;
        if (isClippedToIntegerBounds(x1, y1, fw, fh)) {
            // Simple render - no anti-aliasing
            for (int j = 0; j < objectHeight; j++) {
                composite.blendRow(surfaceData, surfaceOffset, srcColorBlended, objectWidth);
                surfaceOffset += surfaceScanWidth;
            }
        }
        else {
            // Complex render - possible anti-aliasing
            int minU = 0;
            int minV = 0;
            int maxU = fw - FixedMath.ONE;
            int maxV = fh - FixedMath.ONE;

            int v = FixedMath.toFixed(objectY) - boundsY1;
            for (int j = 0; j < objectHeight; j++) {
                int u = FixedMath.toFixed(objectX) - boundsX1;
                // OPTIMIZE: less virtual calls?
                int lastAlpha = -1;
                int runLength = 0;
                for (int x = 0; x < objectWidth; x++) {
                    int rectAlpha = 0xff;
                    if (u < minU) {
                        if (u < minU - FixedMath.ONE) {
                            rectAlpha = 0;
                        }
                        else {
                            rectAlpha = (rectAlpha * (((u-minU) >> 8) & 0xff)) >> 8;
                        }
                    }
                    else if (u > maxU) {
                        if (u > maxU + FixedMath.ONE) {
                            rectAlpha = 0;
                        }
                        else {
                            rectAlpha = (rectAlpha * (((maxU-u) >> 8) & 0xff)) >> 8;
                        }
                    }
                    if (v < minV) {
                        if (v < minV - FixedMath.ONE) {
                            rectAlpha = 0;
                        }
                        else {
                            rectAlpha = (rectAlpha * (((v-minV) >> 8) & 0xff)) >> 8;
                        }
                    }
                    else if (v > maxV) {
                        if (v > maxV + FixedMath.ONE) {
                            rectAlpha = 0;
                        }
                        else {
                            rectAlpha = (rectAlpha * (((maxV-v) >> 8) & 0xff)) >> 8;
                        }
                    }

                    if (rectAlpha == lastAlpha) {
                        runLength++;
                    }
                    else {
                        if (runLength > 0) {
                            composite.blendRow(surfaceData, surfaceOffset + x - runLength,
                                srcColorBlended, lastAlpha, runLength);
                        }
                        lastAlpha = rectAlpha;
                        runLength = 1;
                    }

                    u += FixedMath.ONE;
                }
                if (runLength > 0) {
                    composite.blendRow(surfaceData, surfaceOffset + objectWidth - runLength,
                        srcColorBlended, lastAlpha, runLength);
                }

                surfaceOffset += surfaceScanWidth;
                v += FixedMath.ONE;
            }
        }
    }

    /*
        This uses the "old" scan conversion process, and it should probably be updated to
        use the scan converter from internalDrawRotatedImage().
    */
    private void internalFillRotatedRect(float w, float h) {

        AffineTransform transform = getCurrentTransform();

        // Adjust for internal drawing routines that put integer locations in the middle of the
        // pixel.
        float x1 = transform.getTranslateX() - 0.5f;
        float y1 = transform.getTranslateY() - 0.5f;

        // Find the bounding rectangle

        float x2 = transform.getScaleX() * w;
        float y2 = transform.getShearY() * w;
        float x3 = transform.getShearX() * h;
        float y3 = transform.getScaleY() * h;
        float x4 = x2 + x3;
        float y4 = y2 + y3;

        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        x4 += x1;
        y4 += y1;

        float boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        float boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        float boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        float boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );

        int boundsX = (int)Math.floor(boundsX1) - 1;
        int boundsY = (int)Math.floor(boundsY1) - 1;
        int boundsW = (int)Math.ceil(boundsX2) - boundsX + 2;
        int boundsH = (int)Math.ceil(boundsY2) - boundsY + 2;

        // Clip

        clipObject(boundsX, boundsY, boundsW, boundsH);
        if (objectWidth <= 0 || objectHeight <= 0) {
            return;
        }

        // Debug: draw bounds
        //debugDrawObjectBounds();

        // Calc deltas

        int duX;
        int dvX;
        int duY;
        int dvY;
        int type = transform.getType();
        float ou = objectX - x1;
        float ov = objectY - y1;
        int uY;
        int vY;

        if ((type & AffineTransform.TYPE_ROTATE) != 0) {
            float det = transform.getDeterminant();
            if (det == 0) {
                // Determinant is 0
                return;
            }
            duX = FixedMath.toFixed(transform.getScaleY() / det);
            dvX = FixedMath.toFixed(-transform.getShearY() / det);
            duY = FixedMath.toFixed(-transform.getShearX() / det);
            dvY = FixedMath.toFixed(transform.getScaleX() / det);

            uY = FixedMath.toFixed((ou * transform.getScaleY() - ov * transform.getShearX()) / det);
            vY = FixedMath.toFixed((ov * transform.getScaleX() - ou * transform.getShearY()) / det);
        }
        else if ((type & AffineTransform.TYPE_SCALE) != 0) {
            float sx = transform.getScaleX();
            float sy = transform.getScaleY();
            if (sx == 0 || sy == 0) {
                // Determinant is 0
                return;
            }
            duX = FixedMath.toFixed(1 / sx);
            dvX = 0;
            duY = 0;
            dvY = FixedMath.toFixed(1 / sy);
            uY = FixedMath.toFixed(ou / sx);
            vY = FixedMath.toFixed(ov / sy);
        }
        else {
            duX = FixedMath.ONE;
            dvX = 0;
            duY = 0;
            dvY = FixedMath.ONE;
            uY = FixedMath.toFixed(objectX - x1);
            vY = FixedMath.toFixed(objectY - y1);
        }

        // Start Render
        int srcColorBlended = getPremultipliedSrcColor();
        int surfaceOffset = surfaceDataOffset + objectX + (objectY-1) * surfaceScanWidth;
        int ud = Math.max(Math.abs(duX), Math.abs(duY));
        int vd = Math.max(Math.abs(dvX), Math.abs(dvY));
        int uMin = -FixedMath.ONE_HALF - (ud >> 1);
        int uMax = FixedMath.toFixed(w) - 1 - FixedMath.ONE_HALF + (ud >> 1);
        int vMin = -FixedMath.ONE_HALF - (vd >> 1);
        int vMax = FixedMath.toFixed(h) - 1 - FixedMath.ONE_HALF + (vd >> 1);

        //if (bilinear) {
            // ???
            uY -= FixedMath.ONE_HALF;
            vY -= FixedMath.ONE_HALF;
        //}

        for (int j = 0; j < objectHeight; j++) {

            surfaceOffset += surfaceScanWidth;
            int u = uY;
            int v = vY;

            // Calc uY and vY for the next iteration
            uY += duY;
            vY += dvY;

            int startX = 0;
            int endX = objectWidth - 1;

            // Scan convert - left edge
            if (u < uMin) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(uMin - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (u > uMax) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(uMax - u, duX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }

            if (v < vMin) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(vMin - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }
            else if (v > vMax) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(vMax - v, dvX);
                    startX += n;
                    u += n * duX;
                    v += n * dvX;
                }
            }

            // Scan convert - right edge
            int u2 = u + (endX - startX + 1) * duX;
            if (u2 < uMin) {
                if (duX >= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(uMin - u2, duX);
                    endX += n;
                }
            }
            else if (u2 > uMax) {
                if (duX <= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(uMax - u2, duX);
                    endX += n;
                }
            }

            int v2 = v + (endX - startX + 1) * dvX;
            if (v2 < vMin) {
                if (dvX >= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(vMin - v2, dvX);
                    endX += n;
                }
            }
            else if (v2 > vMax) {
                if (dvX <= 0) {
                    continue;
                }
                else {
                    int n = MathUtil.intDivCeil(vMax - v2, dvX);
                    endX += n;
                }
            }

            int minU = uMin + FixedMath.ONE;
            int minV = vMin + FixedMath.ONE;
            int maxU = uMax - FixedMath.ONE;
            int maxV = vMax - FixedMath.ONE;
            // OPTIMIZE: less virtual calls? less work per pixel?
            int lastAlpha = -1;
            int runLength = 0;
            for (int x = startX; x <= endX; x++) {
                int rectAlpha = 0xff;
                if (u < minU) {
                    rectAlpha = (rectAlpha * (((u-minU) >> 8) & 0xff)) >> 8;
                }
                else if (u > maxU) {
                    rectAlpha = (rectAlpha * (((maxU-u) >> 8) & 0xff)) >> 8;
                }
                if (v < minV) {
                    rectAlpha = (rectAlpha * (((v-minV) >> 8) & 0xff)) >> 8;
                }
                else if (v > maxV) {
                    rectAlpha = (rectAlpha * (((maxV-v) >> 8) & 0xff)) >> 8;
                }

                if (rectAlpha == lastAlpha) {
                    runLength++;
                }
                else {
                    if (runLength > 0) {
                        composite.blendRow(surfaceData, surfaceOffset + x - runLength,
                            srcColorBlended, lastAlpha, runLength);
                    }
                    lastAlpha = rectAlpha;
                    runLength = 1;
                }

                u += duX;
                v += dvX;
            }

            if (runLength > 0) {
                composite.blendRow(surfaceData, surfaceOffset + endX + 1 - runLength,
                    srcColorBlended, lastAlpha, runLength);
            }

            // No anti-aliasing
            //composite.blendRow(surfaceData, surfaceOffset + startX, srcColorBlended,
            //    endX - startX + 1);
        }
    }
}

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
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;

public final class MotionBlur extends Filter {

    private static final int MAX_DISTANCE = 255;

    private static int[] x1Offsets = new int[MAX_DISTANCE + 1];
    private static int[] y1Offsets = new int[MAX_DISTANCE + 1];
    private static int[][] accum;
    private static final Object bufferLock = new Object();

    /**
    The motion distance in pixels. The underlying implementation only uses integers.
    The maximum value is 255. The default value is 4.
     */
    public final Fixed distance = new Fixed(4);

    /**
    The motion angle in radians, typically from -Math.PI/2 to Math.PI/2.
     */
    public final Fixed angle = new Fixed(0);

    private boolean autoExpand = true;

    private int actualDistance;

    // Invalid value so cos/sin will be computed
    private int actualAngle = Integer.MAX_VALUE;

    private int actualAngleCos;

    private int actualAngleSin;

    private Rect bounds = new Rect();


    public MotionBlur() {
        this(0, 4);
    }

    public MotionBlur(float angle) {
        this(angle, 4);
    }

    public MotionBlur(float angle, float distance) {
        this.angle.set(angle);
        this.distance.set(distance);
    }

    private MotionBlur(MotionBlur filter) {
        autoExpand = filter.autoExpand;
        distance.bindWithInverse(filter.distance);
        angle.bindWithInverse(filter.angle);
    }

    /**
    Sets whether the edges of the output is clamped (sharp edges). The default is false
    (blurry edges).
    @see #getClampEdges()
     */
    public void setClampEdges(boolean clamp) {
        boolean newAutoExpand = !clamp;
        if (autoExpand != newAutoExpand) {
            autoExpand = newAutoExpand;
            setDirty();
        }
    }

    /**
    Gets whether the edges of the output is clamped (sharp edges). The default is false
    (blurry edges).
    @see #setClampEdges(boolean)
     */
    public boolean getClampEdges() {
        return !autoExpand;
    }

    public Filter copy() {
        return new MotionBlur(this);
    }

    public void update(int elapsedTime) {
        distance.update(elapsedTime);
        angle.update(elapsedTime);

        int d = CoreMath.clamp(distance.getAsFixed(), 0, CoreMath.toFixed(MAX_DISTANCE));
        d &= 0xffff0000;
        int a = getAngle();
        if (actualDistance != d) {
            actualDistance = d;
            setDirty();
        }
        if (a != actualAngle) {
            actualAngle = a;
            actualAngleCos = CoreMath.cos(a);
            actualAngleSin = CoreMath.sin(a);
            setDirty();
        }
        if (isDirty()) {
            setBounds();
        }
    }

    private void setBounds() {
        int w = super.getWidth();
        int h = super.getHeight();
        bounds.setBounds(0, 0, w, h);
        if (autoExpand) {
            int x = CoreMath.mul(actualAngleCos, actualDistance/2);
            int y = CoreMath.mul(actualAngleSin, actualDistance/2);
            if (x < 0) {
                x = CoreMath.toIntFloor(x);
            }
            else {
                x = CoreMath.toIntCeil(x);
            }
            if (y < 0) {
                y = CoreMath.toIntFloor(y);
            }
            else {
                y = CoreMath.toIntCeil(y);
            }
            bounds.union(x, y, w, h);
            bounds.union(-x, -y, w, h);
        }
    }

    
    private int getAngle() {
        int a = angle.getAsFixed();

        // Reduce range to -2*pi and 2*pi
        int s = a / CoreMath.TWO_PI;
        a -= s * CoreMath.TWO_PI;

        // Reduce range to -pi and pi
        if (a > CoreMath.PI) {
            a = a - CoreMath.TWO_PI;
        }
        else if (a < -CoreMath.PI) {
            a = a + CoreMath.TWO_PI;
        }

        if (a > CoreMath.ONE_HALF_PI) {
            a = a - CoreMath.PI;
        }
        else if (a < -CoreMath.ONE_HALF_PI) {
            a = a + CoreMath.PI;
        }
        return a;
    }

    private boolean isInputOpaque() {
        return getInput() == null ? false : getInput().isOpaque();
    }

    public int getX() {
        return bounds.x;
    }

    public int getY() {
        return bounds.y;
    }

    public int getWidth() {
        return bounds.width;
    }

    public int getHeight() {
        return bounds.height;
    }

    public boolean isOpaque() {
        return (isInputOpaque() && !autoExpand);
    }

    protected void filter(CoreImage input, CoreImage output) {
        synchronized (bufferLock) {
            int[] srcData = input.getData();
            int[] dstData = output.getData();
            int dstWidth = output.getWidth();
            int dstHeight = output.getHeight();
            int srcWidth = input.getWidth();
            int srcHeight = input.getHeight();
            int iterations = CoreMath.toIntCeil(actualDistance) + 1;
            int i2 = CoreMath.log2(iterations);
            int sx = CoreMath.mul(actualAngleCos, actualDistance/2);
            int sy = CoreMath.mul(actualAngleSin, actualDistance/2);
            for (int i = 0; i < iterations; i++) {
                x1Offsets[i] = (CoreMath.toIntFloor(sx)+getX());
                y1Offsets[i] = (CoreMath.toIntFloor(sy)+getY());
                sx -= actualAngleCos;
                sy -= actualAngleSin;
            }

            // Optimized version:

            if (accum == null || accum[0].length < dstWidth) {
                accum = new int[4][dstWidth];
            }

            int[] a = accum[0];
            int[] r = accum[1];
            int[] g = accum[2];
            int[] b = accum[3];

            int dstOffset = 0;
            for (int y = 0; y < dstHeight; y++) {
                // Reset this row
                int rowX1 = dstWidth - 1;
                int rowX2 = 0;
                for (int i = 0; i < iterations; i++) {
                    int sourceY = y + y1Offsets[i];
                    if (sourceY >= 0 && sourceY < srcHeight) {
                        rowX1 = Math.min(rowX1, -x1Offsets[i]);
                        rowX2 = Math.max(rowX2, -x1Offsets[i] + srcWidth-1);
                    }
                }
                rowX1 = Math.max(rowX1, 0);
                rowX2 = Math.min(rowX2, dstWidth - 1);
                if (rowX1 < rowX2) {
                    for (int x = rowX1; x <= rowX2; x++) {
                        a[x] = 0;
                        r[x] = 0;
                        g[x] = 0;
                        b[x] = 0;
                    }
                    // Write the iteration (accumulate)
                    for (int i = 0; i < iterations; i++) {
                        int sourceY = y + y1Offsets[i];

                        if (sourceY >= 0 && sourceY < srcHeight) {
                            int x1 = Math.max(-x1Offsets[i], 0);
                            int x2 = Math.min(-x1Offsets[i] + srcWidth-1, dstWidth - 1);
                            int sourceX = x1+x1Offsets[i];
                            int srcOffset = sourceX + sourceY*srcWidth;

                            for (int x = x1; x <= x2; x++) {
                                int argb = srcData[srcOffset++];
                                a[x] += Colors.getAlpha(argb);
                                r[x] += Colors.getRed(argb);
                                g[x] += Colors.getGreen(argb);
                                b[x] += Colors.getBlue(argb);
                            }
                        }
                    }

                    // Convert the row
                    for (int x = 0; x < rowX1; x++) {
                        dstData[dstOffset++] = 0;
                    }
                    if ((1 << i2) == iterations) {
                        for (int x = rowX1; x <= rowX2; x++) {
                            dstData[dstOffset++] = Colors.rgba(r[x] >> i2, g[x] >> i2, b[x] >> i2, a[x] >> i2);
                        }
                    }
                    else {
                        for (int x = rowX1; x <= rowX2; x++) {
                            dstData[dstOffset++] = Colors.rgba(r[x] / iterations, g[x] / iterations, b[x] / iterations, a[x] / iterations);
                        }
                    }
                    for (int x = rowX2+1; x < dstWidth; x++) {
                        dstData[dstOffset++] = 0;
                    }
                }
                else {
                    for (int x = 0; x < dstWidth; x++) {
                        dstData[dstOffset++] = 0;
                    }
                }
            }

            // Unoptimized version:

//            int dstOffset = 0;
//            for (int y = 0; y < dstHeight; y++) {
//                for (int x = 0; x < dstWidth; x++) {
//                    int a = 0;
//                    int r = 0;
//                    int g = 0;
//                    int b = 0;
//                    int n = 0;
//                    for (int i = 0; i < iterations; i++) {
//                        int sourceX = x + x1Offsets[i];
//                        int sourceY = y + y1Offsets[i];
//
//                        if (sourceX >= 0 && sourceY >= 0 && sourceX < srcWidth && sourceY < srcHeight) {
//                            int srcARGB = srcData[sourceX + sourceY * srcWidth];
//                            a += Colors.getAlpha(srcARGB);
//                            r += Colors.getRed(srcARGB);
//                            g += Colors.getGreen(srcARGB);
//                            b += Colors.getBlue(srcARGB);
//                            n++;
//                        }
//                    }
//                    if (n == 0) {
//                        dstData[dstOffset] = 0;
//                    }
//                    else {
//                        dstData[dstOffset] = Colors.rgba(r/n, g/n, b/n, a/n);
//                    }
//                    dstOffset++;
//                }
//            }
        }
    }
}

/*
    Copyright (c) 2009-2011, Interactive Pulp, LLC
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
package org.pulpcore.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.view.View;
import org.pulpcore.view.animation.InstantAction;
import org.pulpcore.view.animation.IntervalAction;

/*
    Internal notes:

    * Each bezier segment is converted to line segments so that
    animation along the path can occur at a constant rate.
    * For now, MOVE_TO commands that don't occur at the first draw command
    are interpreted at LINE_TO commands.
    * Previous versions used StringTokenizer, but that class isn't a part of GWT.
*/

/**
    The Path class is a series of straight lines and curves that a
    View can animate along.
    <p>
    Paths points are immutable, but the path can be translated to another location.
    <p>
    Paths are created from SVG path commands. For example, a triangle path:
    <pre>path = new Path("M 100 100 L 300 100 L 200 300 L 100 100");</pre>
    A simple curve:
    <pre>path = new Path("M100,200 C100,100 400,100 400,200");</pre>

    See <a href="http://www.w3.org/TR/SVG/paths.html#PathData">http://www.w3.org/TR/SVG/paths.html#PathData</a>.
    All SVG commands are supported, however, move-to commands in the middle of a path are
    treated as line-to commands. That is, subpaths are concatenated together to form one path.

    <p>
    Note, the Path class is not used for rendering paths or shapes.
*/
public class Path {

    private static final float PI = (float)Math.PI;
    private static final float TWO_PI = (float)(Math.PI * 2);
    private static final float ONE_HALF_PI = (float)(Math.PI / 2);

    /**
        The move command. The move-to command requires one point: the location
        to move to.
    */
    private static final int MOVE_TO = 0;

    /**
        The line segment command. The line-to command requires one point:
        the location to draw a line segment to.
    */
    private static final int LINE_TO = 1;

    /**
        The cubic bezier command. The curve-to command requires three points.
        The first point is the control point at the beginning of the curve,
        the second point is the control point at the end of the curve,
        and the third point is the final destination point. All points are
        absolute values.
    */
    private static final int CURVE_TO = 2;

    /** The number of points in the path. */
    private final int numPoints;

    /** The x-location of each point. */
    private final float[] xPoints;

    /** The y-location of each point. */
    private final float[] yPoints;

    /** The value, from 0 to 1, representing the portion of the total length at each point. */
    private final float[] pPoints;

    private float totalLength;

    private boolean isClosed;

    private transient float lastCalcP = -1;
    private transient float[] lastCalcPoint = new float[2];

    /**
        Parse an SVG path data string.
        supported. See http://www.w3.org/TR/SVG/paths.html#PathData
        @throws IllegalArgumentException If the path data string could not be parsed.
    */
    public Path(String svgPathData) throws IllegalArgumentException {
        ArrayList<float[]> commands = parseSVGPathData(svgPathData);

        float[][] points = toLineSegments(commands);

        this.xPoints = points[0];
        this.yPoints = points[1];
        this.numPoints = xPoints.length;
        this.pPoints = new float[numPoints];

        init();
    }

    /**
        Creates a new Path with the specified points.
     */
    public Path(float[] xPoints, float[] yPoints) {
        this.xPoints = xPoints.clone();
        this.yPoints = yPoints.clone();
        this.numPoints = xPoints.length;
        this.pPoints = new float[numPoints];

        init();
    }

    /**
        Returns true if the path is closed, that is, the first point and the last point
        are identical.
     */
    public boolean isClosed() {
        return isClosed;
    }

    public float getLength() {
        return totalLength;
    }

    // These two methods are commented out because they are untested

//    /**
//        Returns the area of the polygon defined by this closed path.
//    */
//    public float getArea() {
//        if (area == 0) {
//
//            // Based on "Calculating the area and centroid of a polygon" by Paul Bourke
//            // http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
//            for (int i = 0; i < numPoints; i++) {
//                int j = (i + 1) % numPoints;
//                area += xPoints[i] * yPoints[j];
//                area -= xPoints[j] * yPoints[i];
//            }
//            area /= 2;
//        }
//        return area;
//    }
//
//    /**
//        Returns true if the specified point is contained inside
//        the polygon defined by this closed path.
//    */
//    public boolean contains(float x, float y) {
//
//        // Based on the "Point Inclusion in Polygon Test" article by W. Randolph Franklin
//        // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
//        boolean isInside = false;
//        float xpj = xPoints[numPoints - 1];
//        float ypj = yPoints[numPoints - 1];
//        for (int i = 0; i < numPoints; i++) {
//            float xpi = xPoints[i];
//            float ypi = yPoints[i];
//
//            if (((ypi <= y && y < ypj) || (ypj <= y && y < ypi)) &&
//                (x < (xpj - xpi) * (y - ypi) / (ypj - ypi) + xpi))
//            {
//                // The ray crossed an edge
//                isInside = !isInside;
//            }
//            xpj = xpi;
//            ypj = ypi;
//        }
//
//        return isInside;
//    }

    private void init() {

        // Find the lengths of each segment;
        totalLength = 0;
        float[] segmentLength = new float[numPoints - 1];
        for (int i = 0; i < numPoints - 1; i++) {
            float dist = MathUtil.dist(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            segmentLength[i] = dist;
            totalLength += dist;
        }

        pPoints[0] = 0;
        pPoints[numPoints - 1] = 1;
        float length = 0;
        for (int i = 1; i < numPoints - 1; i++) {
            length += segmentLength[i - 1];
            pPoints[i] = length / totalLength;
        }

        isClosed = xPoints[0] == xPoints[numPoints - 1] && yPoints[0] == yPoints[numPoints - 1];
    }

    public void translate(float x, float y) {
        if (x == 0 && y == 0) {
            return;
        }
        for (int i = 0; i < numPoints; i++) {
            xPoints[i] += x;
            yPoints[i] += y;
        }
    }

    public float getStartX() {
        return xPoints[0];
    }

    public float getStartY() {
        return yPoints[0];
    }

    public float getEndX() {
        return xPoints[numPoints - 1];
    }

    public float getEndY() {
        return yPoints[numPoints - 1];
    }

    /**
        Gets the x location of point p on the path, where p is typically from 0 (start of the path)
        to 1 (end of the path).
        @param p The position along the path to place the sprite, from 0 to 1.
     */
    public float getX(float p) {
        float px = clampP(p);
        return get(0, px);
    }

    /**
        Gets the y location of point p on the path, where p is typically from 0 (start of the path)
        to 1 (end of the path).
        @param p The position along the path to place the sprite, from 0 to 1.
     */
    public float getY(float p) {
        float px = clampP(p);
        return get(1, px);
    }

    /**
        Gets the angle of point p on the path, where p is typically from 0 (start of the path)
        to 1 (end of the path).
        @param p The position along the path to place the sprite, from 0 to 1.
     */
    public float getAngle(float p) {
        float px = clampP(p);
        return get(2, px);
        //int i = getLowerSegment(px);
        //return Math.atan2(yPoints[i + 1] - yPoints[i], xPoints[i + 1] - xPoints[i]);
    }

    /**
     Creates an Action that places a View at a position along this path.
     @param view The View to place.
     @param p The position along this path to place the View, from 0 to 1.
     @param rotate Whether to also rotate the View to the path's angle at the specific point.
     @return the Action, which can be added to a View.
    */
    public InstantAction createPlaceAction(final View view, final float p, final boolean rotate) {
        return new InstantAction() {
            @Override
            public void run() {
                float px = clampP(p);
                view.x.set(get(0, px));
                view.y.set(get(1, px));
                if (rotate) {
                    view.angle.set(get(2, px));
                }
            }
        };
    }

    /**
     Creates an Action that moves a View along this path.
     <p>If the path is open ({@link #isClosed()} returns false), the position is clamped from
     0 to 1. Otherwise, if the path is open, the position wraps. For example, for a closed path,
     moving from 0 to 1.5 is the same as moving from 0 to 1 and then from 0 to 0.5.
     @param dur The duration
     @param view The view to animate
     @param startP The start position along the path, typically from 0 to 1.
     @param endP The end position along the path, typically from 0 to 1.
     @param rotate Whether to also rotate the View along the path's angle.
     @return the Action, which can be added to a View.
     */
    public IntervalAction createMoveAction(float dur, final View view,
            final float startP, final float endP, final boolean rotate) {
        return new IntervalAction(dur) {

            @Override
            public void update(float p) {
                float pathP = startP + (endP - startP) * p;
                float px = clampP(pathP);
                view.x.set(get(0, px));
                view.y.set(get(1, px));
                if (rotate) {
                    view.angle.set(get(2, px));
                }
            }

        };
    }

    private float clampP(float p) {
        if (isClosed()) {
            // Only wrap if the path is closed.
            // Special case: ONE is considered the end.
            if (p != 1) {
                p %= 1;
                if (p < 0) {
                    p += 1;
                }
            }
        }
        else {
            p = MathUtil.clamp(p, 0, 1);
        }
        return p;
    }

    private float get(int axis, float p) {
        float px = clampP(p);
        if (axis == 0 || axis == 1) {
            return getLocation(px)[axis];
        }
        else {
            int i = getLowerSegment(px);
            float lowerAngle = (float)Math.atan2(yPoints[i + 1] - yPoints[i],
                    xPoints[i + 1] - xPoints[i]);
            if (i == numPoints - 2) {
                return lowerAngle;
            }
            else {
                float higherAngle = (float)Math.atan2(yPoints[i + 2] - yPoints[i + 1],
                        xPoints[i + 2] - xPoints[i + 1]);
                float dAngle = higherAngle - lowerAngle;
                if (Math.abs(dAngle + TWO_PI) < Math.abs(dAngle)) {
                    dAngle += TWO_PI;
                }
                else if (Math.abs(dAngle - TWO_PI) < Math.abs(dAngle)) {
                    dAngle -= TWO_PI;
                }

                return lowerAngle + dAngle * (px - pPoints[i]) /  (pPoints[i + 1] - pPoints[i]);
            }
        }
    }

    private int getLowerSegment(float p) {

        // Binary search for P
        int high = numPoints - 1;
        int low = 0;
        while (high - low > 1) {
            int index = (high + low) >>> 1;

            if (pPoints[index] > p) {
                high = index;
            }
            else {
                low = index;
            }
        }

        return low;
    }

    private float[] getLocation(float p) {

        if (p == lastCalcP) {
            return lastCalcPoint;
        }

        lastCalcP = p;

        if (p == 0) {
            lastCalcPoint[0] = xPoints[0];
            lastCalcPoint[1] = yPoints[0];
            return lastCalcPoint;
        }
        else if (p == 1) {
            lastCalcPoint[0] = xPoints[numPoints - 1];
            lastCalcPoint[1] = yPoints[numPoints - 1];
            return lastCalcPoint;
        }

        // Binary search for P
        int high = numPoints - 1;
        int low = 0;
        while (high - low > 1) {
            int index = (high + low) >>> 1;

            if (pPoints[index] > p) {
                high = index;
            }
            else {
                low = index;
            }
        }

        if (high == low) {
            lastCalcPoint[0] = xPoints[low];
            lastCalcPoint[1] = yPoints[low];
        }
        else {
            float q = p - pPoints[low];
            float r = pPoints[high] - pPoints[low];

            lastCalcPoint[0] = xPoints[low] + (xPoints[high] - xPoints[low]) * q / r;
            lastCalcPoint[1] = yPoints[low] + (yPoints[high] - yPoints[low]) * q / r;
        }

        return lastCalcPoint;
    }

    /**
        Draws the segments of this path using the current color.
        @param drawJoints if true, draw rectangles at the joints between line segments
    */
    public void draw(Graphics g, boolean drawJoints) {
        float x1 = xPoints[0];
        float y1 = yPoints[0];
        for (int i = 1; i < numPoints; i++) {
            if (drawJoints) {
                g.fillRect(x1-1, y1-1, 3, 3);
            }
            float x2 = xPoints[i];
            float y2 = yPoints[i];
            g.drawLine(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }

        if (drawJoints) {
            g.fillRect(x1-1, y1-1, 3, 3);
        }
    }

    //
    // SVG path-data parsing
    //

    private static class SVGPathTokenizer implements Iterator<String> {

        private static final String DELIMS = "MmLlHhVvAaQqTtCcSsZz";

        private final String input;
        private int position;

        public SVGPathTokenizer(String input) {
            this.input = input;
            this.position = 0;
        }

        @Override
        public boolean hasNext() {
            return position < input.length();
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int startPosition = position;
            // Check if current char is a delim
            char ch = input.charAt(startPosition);
            if (DELIMS.indexOf(ch) >= 0) {
                position++;
                return Character.toString(ch);
            }
            // Otherwise, search until next delim is found
            int endPosition = startPosition + 1;
            while (endPosition < input.length()) {
                ch = input.charAt(endPosition);
                if (DELIMS.indexOf(ch) >= 0) {
                    break;
                }
                endPosition++;
            }
            position = endPosition;
            return input.substring(startPosition, endPosition);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
        Parses an SVG Path to simple move-to, line-to, curve-to commands.
        Returns a List of float[] arrays. The first item in the array is the command,
        (MOVE_TO, LINE_TO, or CURVE_TO) and the remaining items in the array are the command
        parameters (2, 2, and 6 respectively).
     */
    private static ArrayList<float[]> parseSVGPathData(String svgPathData) throws IllegalArgumentException {
        ArrayList<float[]> commands = new ArrayList<float[]>();
        svgPathData = svgPathData.trim();
        SVGPathTokenizer tokenizer = new SVGPathTokenizer(svgPathData);
        float currX = 0;
        float currY = 0;
        float initialX = 0;
        float initialY = 0;
        float lastControlX = 0;
        float lastControlY = 0;
        String lastCommand = "";
        while (tokenizer.hasNext()) {
            String commandType = tokenizer.next();

            if (commandType.trim().length() == 0) {
                // This might happen after a Z command
                continue;
            }
            boolean isRelative = false;

            if (commandType.length() == 1 && Character.isLowerCase(commandType.charAt(0))) {
                commandType = commandType.toUpperCase();
                isRelative = true;
            }

            if ("Z".equals(commandType)) {
                // Special case: don't parse numbers
                if (currX != initialX || currY != initialY) {
                    commands.add(new float[] { LINE_TO, initialX, initialY });
                    currX = initialX;
                    currY = initialY;
                }
            }
            else {
                ArrayList<Float> numbers = parseNumberList(tokenizer.next());
                int numbersLength = numbers.size();

                if ("M".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 2);
                    for (int i = 0; i < numbersLength; i+=2) {
                        float endX = numbers.get(i);
                        float endY = numbers.get(i + 1);
                        if (isRelative) {
                            endX += currX;
                            endY += currY;
                        }
                        if (i == 0) {
                            commands.add(new float[] { MOVE_TO, endX, endY });
                            initialX = endX;
                            initialY = endY;
                        }
                        else {
                            commands.add(new float[] { LINE_TO, endX, endY });
                        }
                        currX = endX;
                        currY = endY;
                    }
                }
                else if ("L".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 2);
                    for (int i = 0; i < numbersLength; i+=2) {
                        float endX = numbers.get(i);
                        float endY = numbers.get(i + 1);
                        if (isRelative) {
                            endX += currX;
                            endY += currY;
                        }
                        commands.add(new float[] { LINE_TO, endX, endY });
                        currX = endX;
                        currY = endY;
                    }
                }
                else if ("H".equals(commandType)) {
                    for (int i = 0; i < numbersLength; i++) {
                        float endX = numbers.get(i);
                        if (isRelative) {
                            endX += currX;
                        }
                        commands.add(new float[] { LINE_TO, endX, currY });
                        currX = endX;
                    }
                }
                else if ("V".equals(commandType)) {
                    for (int i = 0; i < numbersLength; i++) {
                        float endY = numbers.get(i);
                        if (isRelative) {
                            endY += currY;
                        }
                        commands.add(new float[] { LINE_TO, currX, endY });
                        currY = endY;
                    }
                }
                else if ("C".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 6);
                    for (int i = 0; i <numbersLength; i+=6) {
                        float x1 = numbers.get(i + 0);
                        float y1 = numbers.get(i + 1);
                        float x2 = numbers.get(i + 2);
                        float y2 = numbers.get(i + 3);
                        float endX = numbers.get(i + 4);
                        float endY = numbers.get(i + 5);
                        if (isRelative) {
                            x1 += currX;
                            y1 += currY;
                            x2 += currX;
                            y2 += currY;
                            endX += currX;
                            endY += currY;
                        }
                        commands.add(new float[] { CURVE_TO, x1, y1, x2, y2, endX, endY });
                        currX = endX;
                        currY = endY;
                        lastControlX = x2;
                        lastControlY = y2;
                    }
                }
                else if ("S".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 4);
                    for (int i = 0; i < numbersLength; i+=4) {
                        float x1;
                        float y1;
                        float x2 = numbers.get(i + 0);
                        float y2 = numbers.get(i + 1);
                        float endX = numbers.get(i + 2);
                        float endY = numbers.get(i + 3);
                        if (isRelative) {
                            x2 += currX;
                            y2 += currY;
                            endX += currX;
                            endY += currY;
                        }
                        if (i > 0 || "C".equals(lastCommand) || "S".equals(lastCommand)) {
                            // "The reflection of the second control point on the previous command
                            // relative to the current point"
                            x1 = 2*currX - lastControlX;
                            y1 = 2*currY - lastControlY;
                        }
                        else {
                            // "Coincident with the current point"
                            x1 = currX;
                            y1 = currY;
                        }
                        commands.add(new float[] { CURVE_TO, x1, y1, x2, y2, endX, endY });
                        currX = endX;
                        currY = endY;
                        lastControlX = x2;
                        lastControlY = y2;
                    }
                }
                else if ("Q".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 4);
                    for (int i = 0; i < numbersLength; i+=4) {
                        float qx = numbers.get(i + 0);
                        float qy = numbers.get(i + 1);
                        float endX = numbers.get(i + 2);
                        float endY = numbers.get(i + 3);
                        if (isRelative) {
                            qx += currX;
                            qy += currY;
                            endX += currX;
                            endY += currY;
                        }
                        // Convert quadratic bezier to cubic
                        float x1 = (currX + 2 * qx) / 3;
                        float y1 = (currY + 2 * qy) / 3;
                        float x2 = (endX + 2 * qx) / 3;
                        float y2 = (endY + 2 * qy) / 3;
                        commands.add(new float[] { CURVE_TO, x1, y1, x2, y2, endX, endY });
                        currX = endX;
                        currY = endY;
                        lastControlX = qx;
                        lastControlY = qy;
                    }
                }
                else if ("T".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 2);
                    for (int i = 0; i < numbersLength; i+=2) {
                        float qx;
                        float qy;
                        float endX = numbers.get(i + 0);
                        float endY = numbers.get(i + 1);
                        if (isRelative) {
                            endX += currX;
                            endY += currY;
                        }
                        if (i > 0 || "Q".equals(lastCommand) || "T".equals(lastCommand)) {
                            // "The reflection of the control point on the previous command relative
                            // to the current point"
                            qx = 2*currX - lastControlX;
                            qy = 2*currY - lastControlY;
                        }
                        else {
                            // "Coincident with the current point"
                            qx = currX;
                            qy = currY;
                        }
                        // Convert quadratic bezier to cubic
                        float x1 = (currX + 2 * qx) / 3;
                        float y1 = (currY + 2 * qy) / 3;
                        float x2 = (endX + 2 * qx) / 3;
                        float y2 = (endY + 2 * qy) / 3;
                        commands.add(new float[] { CURVE_TO, x1, y1, x2, y2, endX, endY });
                        currX = endX;
                        currY = endY;
                        lastControlX = qx;
                        lastControlY = qy;
                    }
                }
                else if ("A".equals(commandType)) {
                    checkIfSizeIsMultipleOf(commandType, numbersLength, 7);
                    for (int i = 0; i < numbersLength; i+=7) {
                        // See http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
                        float rx = numbers.get(i + 0);
                        float ry = numbers.get(i + 1);
                        float angle = (float)Math.toRadians(numbers.get(i + 2) % 360);
                        boolean largeArc = numbers.get(i + 3) != 0;
                        boolean sweep = numbers.get(i + 4) != 0;
                        float endX = numbers.get(i + 5);
                        float endY = numbers.get(i + 6);
                        if (isRelative) {
                            endX += currX;
                            endY += currY;
                        }
                        if (currX == endX && currY == endY) {
                            // Do nothing
                        }
                        else if (rx == 0 || ry == 0) {
                            commands.add(new float[] { LINE_TO, endX, endY });
                        }
                        else {
                            commands.addAll(parseArc(currX, currY, rx, ry, angle,
                                    largeArc, sweep, endX, endY));
                        }
                        currX = endX;
                        currY = endY;
                    }
                }
                else {
                    throw new IllegalArgumentException("Not a supported command: " + commandType);
                }
            }
            lastCommand = commandType;
        }
        return commands;
    }

    /**
        Convert arc (ellipse) to cubic bezier curve(s).
     */
    private static ArrayList<float[]> parseArc(float currX, float currY, float rx, float ry, float angle,
            boolean largeArc, boolean sweep,
            float endX, float endY)
    {

        ArrayList<float[]> commands = new ArrayList<float[]>();

        float dx2 = (currX - endX) / 2;
        float dy2 = (currY - endY) / 2;
        float cosA = (float)Math.cos(angle);
        float sinA = (float)Math.sin(angle);

        float x1p = cosA * dx2 + sinA * dy2;
        float y1p = -sinA * dx2 + cosA * dy2;
        float x1p2 = x1p * x1p;
        float y1p2 = y1p * y1p;

        // Correct out-of-range radii
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        float rx2 = rx * rx;
        float ry2 = ry * ry;
        float V = x1p2 / rx2 + y1p2 / ry2;
        if (V > 1) {
            float Vsq = (float)Math.sqrt(V);
            rx = Vsq * rx;
            ry = Vsq * ry;
            rx2 = rx * rx;
            ry2 = ry * ry;
        }

        // Find center point (cx, cy)
        float S = (float)Math.sqrt(Math.max(0,
                (rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2) /
                (rx2 * y1p2 + ry2 * x1p2)));
        if (largeArc == sweep) {
            S = -S;
        }
        float cxp = S * rx * y1p / ry;
        float cyp = -S * ry * x1p / rx;
        float cx = cosA * cxp - sinA * cyp + (currX + endX) / 2;
        float cy = sinA * cxp + cosA * cyp + (currY + endY) / 2;

        // Find startAngle and endAngle
        float ux = (x1p - cxp) / rx;
        float uy = (y1p - cyp) / ry;
        float vx = (-x1p - cxp) / rx;
        float vy = (-y1p - cyp) / ry;
        float n = (float)Math.sqrt(ux * ux + uy * uy);
        float p = ux;
        float angleStart = (float)Math.acos(p / n);
        if (uy < 0) {
            angleStart = -angleStart;
        }
        n = (float)Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        float angleExtent = (float)Math.acos(p / n);
        if (ux * vy - uy * vx < 0) {
            angleExtent = -angleExtent;
        }
        if (!sweep && angleExtent > 0) {
            angleExtent -= TWO_PI;
        }
        else if (sweep && angleExtent < 0) {
            angleExtent += TWO_PI;
        }
        float angleEnd = angleStart + angleExtent;

        // Create one bezier for each quadrant
        float cosEtaB = (float)Math.cos(angleStart);
        float sinEtaB = (float)Math.sin(angleStart);
        float aCosEtaB = rx * cosEtaB;
        float bSinEtaB = ry * sinEtaB;
        float aSinEtaB = rx * sinEtaB;
        float bCosEtaB = ry * cosEtaB;
        float xB = cx + aCosEtaB * cosA - bSinEtaB * sinA;
        float yB = cy + aCosEtaB * sinA + bSinEtaB * cosA;
        float xBDot = -aSinEtaB * cosA - bCosEtaB * sinA;
        float yBDot = -aSinEtaB * sinA + bCosEtaB * cosA;

        float s = angleStart;
        float d = sweep ? ONE_HALF_PI : -ONE_HALF_PI;
        while (true) {
            if ((sweep && s > angleEnd) || (!sweep && s < angleEnd)) {
                break;
            }

            float e = s + d;
            if ((sweep && e > angleEnd) || (!sweep && e < angleEnd)) {
                e = angleEnd;
            }

            float da = e - s;
            float alpha = 4*(float)Math.tan(da/4)/3;

            // Alternative alpha from: http://www.spaceroots.org/documents/ellipse/
            // TODO: test for very large arcs to see which one is better
            //float t = (float)Math.tan(da / 2);
            //float alpha = (float)Math.sin(da) * (float)(Math.fsqrt(4 + 3 * t * t) - 1) / 3;

            float xA = xB;
            float yA = yB;
            float xADot = xBDot;
            float yADot = yBDot;

            cosEtaB = (float)Math.cos(e);
            sinEtaB = (float)Math.sin(e);
            aCosEtaB = rx * cosEtaB;
            bSinEtaB = ry * sinEtaB;
            aSinEtaB = rx * sinEtaB;
            bCosEtaB = ry * cosEtaB;

            xBDot = -aSinEtaB * cosA - bCosEtaB * sinA;
            yBDot = -aSinEtaB * sinA + bCosEtaB * cosA;
            if (e == angleEnd) {
                xB = endX;
                yB = endY;
            }
            else {
                xB = cx + aCosEtaB * cosA - bSinEtaB * sinA;
                yB = cy + aCosEtaB * sinA + bSinEtaB * cosA;
            }

            commands.add(new float[] { CURVE_TO,
                    (xA + alpha * xADot), (yA + alpha * yADot),
                    (xB - alpha * xBDot), (yB - alpha * yBDot),
                    xB, yB });

            s += d;
        }

        return commands;
    }

    private static void checkIfSizeIsMultipleOf(String commandType, int size, int m)
            throws IllegalArgumentException
    {
        if (size == 0 || (size % m) != 0) {
            throw new IllegalArgumentException("Command '" + commandType +
                    "' parameters count (" + size + ") is not a multiple of " + m + ".");
        }
    }

    private static ArrayList<Float> parseNumberList(String svgNumberList) {
        ArrayList<Float> numbers = new ArrayList<Float>();
        svgNumberList = svgNumberList.trim();
        svgNumberList = svgNumberList.replace(',', ' ');
        // Some SVG exporters don't put a delimeter before a negative value.
        // For example, (10.4, -5.6) may be represented as "10.4-5.6".
        svgNumberList = svgNumberList.replace("-", " -");
        
        String[] tokens = svgNumberList.split("\\s");
        for (String t : tokens) {
            if (t.length() > 0) {
                try {
                    numbers.add(new Float(t));
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Not a number. Token: \"" + t +
                            "\" in string: \"" + svgNumberList + "\"");
                }
            }
        }
        return numbers;
    }

    //
    // Convert draw commands to line segments.
    //

    private static float[][] toLineSegments(ArrayList<float[]> drawCommands) {

        int numDrawCommands = drawCommands.size();
        float[][][] allPoints = new float[numDrawCommands][2][];
        int numPoints = 0;
        float x = 0;
        float y = 0;

        for (int i = 0; i < numDrawCommands; i++) {
            float[] drawCommand = drawCommands.get(i);
            switch ((int)drawCommand[0]) {

                case MOVE_TO: case LINE_TO:
                    x = drawCommand[1];
                    y = drawCommand[2];

                    allPoints[i] = new float[2][1];
                    allPoints[i][0][0] = x;
                    allPoints[i][1][0] = y;
                    numPoints++;
                    break;

                case CURVE_TO:
                    allPoints[i] = toLineSegments(x, y,
                         drawCommand[1],  drawCommand[2],
                         drawCommand[3],  drawCommand[4],
                         drawCommand[5],  drawCommand[6]);

                    x = drawCommand[5];
                    y = drawCommand[6];
                    numPoints += allPoints[i][0].length;
                    break;
            }
        }

        // Convert the allPoints array to xPoints, yPoints
        float[] xPoints = new float[numPoints];
        float[] yPoints = new float[numPoints];
        int offset = 0;
        for (int i = 0; i < numDrawCommands; i++) {
            int len = allPoints[i][0].length;
            System.arraycopy(allPoints[i][0], 0, xPoints, offset, len);
            System.arraycopy(allPoints[i][1], 0, yPoints, offset, len);

            offset += len;
            allPoints[i] = null;
        }

        float[][] returnValue = new float[2][];
        returnValue[0] = xPoints;
        returnValue[1] = yPoints;

        return returnValue;
    }

    //
    // Bezier conversion to line segments (using forward differencing).
    //

    private static float[][] toLineSegments(float x1, float y1, float x2, float y2,
        float x3, float y3, float x4, float y4)
    {
        // First division
        float x12   = (x1 + x2) / 2f;
        float y12   = (y1 + y2) / 2f;
        float x23   = (x2 + x3) / 2f;
        float y23   = (y2 + y3) / 2f;
        float x34   = (x3 + x4) / 2f;
        float y34   = (y3 + y4) / 2f;
        float x123  = (x12 + x23) / 2f;
        float y123  = (y12 + y23) / 2f;
        float x234  = (x23 + x34) / 2f;
        float y234  = (y23 + y34) / 2f;
        float x1234 = (x123 + x234) / 2f;
        float y1234 = (y123 + y234) / 2f;

        // Left division
        float lx12   = (x1 + x12) / 2f;
        float ly12   = (y1 + y12) / 2f;
        float lx23   = (x12 + x123) / 2f;
        float ly23   = (y12 + y123) / 2f;
        float lx34   = (x123 + x1234) / 2f;
        float ly34   = (y123 + y1234) / 2f;
        float lx123  = (lx12 + lx23) / 2f;
        float ly123  = (ly12 + ly23) / 2f;
        float lx234  = (lx23 + lx34) / 2f;
        float ly234  = (ly23 + ly34) / 2f;
        float lx1234 = (lx123 + lx234) / 2f;
        float ly1234 = (ly123 + ly234) / 2f;

        // Right division
        float rx12   = (x1234 + x234) / 2f;
        float ry12   = (y1234 + y234) / 2f;
        float rx23   = (x234 + x34) / 2f;
        float ry23   = (y234 + y34) / 2f;
        float rx34   = (x34 + x4) / 2f;
        float ry34   = (y34 + y4) / 2f;
        float rx123  = (rx12 + rx23) / 2f;
        float ry123  = (ry12 + ry23) / 2f;
        float rx234  = (rx23 + rx34) / 2f;
        float ry234  = (ry23 + ry34) / 2f;
        float rx1234 = (rx123 + rx234) / 2f;
        float ry1234 = (ry123 + ry234) / 2f;

        // Determine the number of segments for each division
        int numSegments1 = getNumSegments(x1, y1, lx12, ly12, lx123, ly123, lx1234, ly1234);
        int numSegments2 = getNumSegments(lx1234, ly1234, lx234, ly234, lx34, ly34, x1234, y1234);
        int numSegments3 = getNumSegments(x1234, y1234, rx12, ry12, rx123, ry123, rx1234, ry1234);
        int numSegments4 = getNumSegments(rx1234, ry1234, rx234, ry234, rx34, ry34, x4, y4);

        int totalPoints = numSegments1 + numSegments2 + numSegments3 + numSegments4;
        float[] xPoints = new float[totalPoints];
        float[] yPoints = new float[totalPoints];
        int offset = 0;

        // Convert to lines
        offset += toLineSegments(xPoints, yPoints, offset, numSegments1,
            x1, y1, lx12, ly12, lx123, ly123, lx1234, ly1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments2,
            lx1234, ly1234, lx234, ly234, lx34, ly34, x1234, y1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments3,
            x1234, y1234, rx12, ry12, rx123, ry123, rx1234, ry1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments4,
            rx1234, ry1234, rx234, ry234, rx34, ry34, x4, y4);

        float[][] returnValue = new float[2][];
        returnValue[0] = xPoints;
        returnValue[1] = yPoints;
        return returnValue;
    }

    private static int getNumSegments(float x0, float y0, float x1, float y1, float x2, float y2,
        float x3, float y3)
    {
        int numSegments;

        // attempt to measure the "curvature" of the segment - more curvature, the more points.
        /*
        float dx = (x1 + x2 - x0 - x3) / 2;
        float dy = (y1 + y2 - y0 - y3) / 2;

        int numSegments;
        if (dx == 0 && dy == 0) {
            numSegments = 1;
        }
        else {
            float distSq = dx * dx + dy * dy;

            int log2Dist = MathUtil.log2(Math.round(distSq)) >> 1;

            numSegments = Math.max(1, 1 << (log2Dist - 1));
        }
        */

        float dist = Math.max(
            getDist(x1, y1, x0, y0, x3, y3),
            getDist(x2, y2, x0, y0, x3, y3));

        if (dist <= 0) {
            numSegments = 1;
        }
        else {
            numSegments = Math.max(1, 1 << (MathUtil.log2(Math.round(dist))));
            numSegments = Math.min(numSegments, 16);
        }

        return numSegments;
    }

    private static int toLineSegments(float[] xPoints, float[] yPoints, int offset, int numSegments,
        float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3)
    {
        float t = 1f / numSegments;
        float tSquared = t * t;
        float tCubed = tSquared * t;

        float xf = x0;
        float xfd = 3 * (x1 - x0) * t;
        float xfdd2 = 3 * (x0 - 2 * x1 + x2) * tSquared;
        float xfddd6 = (3 * (x1 - x2) + x3 - x0) * tCubed;
        float xfddd2 = 3 * xfddd6;
        float xfdd = 2 * xfdd2;
        float xfddd = 2 * xfddd2;

        float yf = y0;
        float yfd = 3 * (y1 - y0) * t;
        float yfdd2 = 3 * (y0 - 2 * y1 + y2) * tSquared;
        float yfddd6 = (3 * (y1 - y2) + y3 - y0) * tCubed;
        float yfddd2 = 3 * yfddd6;
        float yfdd = 2 * yfdd2;
        float yfddd = 2 * yfddd2;

        for (int i = 1; i < numSegments; i++) {
            xf += xfd + xfdd2 + xfddd6;
            xfd += xfdd + xfddd2;
            xfdd += xfddd;
            xfdd2 += xfddd2;

            yf += yfd + yfdd2 + yfddd6;
            yfd += yfdd + yfddd2;
            yfdd += yfddd;
            yfdd2 += yfddd2;

            xPoints[offset] = xf;
            yPoints[offset] = yf;
            offset++;
        }

        xPoints[offset] = x3;
        yPoints[offset] = y3;
        offset++;

        return numSegments;
    }

    private static float getDist(float px, float py, float ax, float ay, float bx, float by) {

        // Distance from a point to a line approximation. From Graphics Gems II, page 11.

        float dx = Math.abs(bx - ax);
        float dy = Math.abs(by - ay);

        float div = dx + dy - Math.min(dx, dy)/2;

        if (div == 0) {
            return 0;
        }

        float a2 = (py - ay) * (bx - ax) - (px - ax) * (by - ay);

        return Math.abs(a2) / div;
    }
}
/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
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
package org.pulpcore.tools.imagefont;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 Gets all kerning pairs for a set of characters. This class first tries to use existing kerning
 values, and if those are not available, then auto-kerning is used.
 */
// TODO: Provide an option to kern?
// NONE, METRICS, AUTO, DEFAULT (METRICS if they exist, otherwise AUTO)
public class KerningFactory {

    private KerningFactory() {
       
    }

    /**
     Gets all kerning pairs for a set of characters.
     Note: Since there is no Java API to get kerning values, this method uses a brute-force
     search. The search takes O(n^2) time, where n is the number of characters. Only letters
     and punctuation are kerned.
     */
    public static List<Kerning> getKerningPairs(FontRenderContext context, Font font, char[] chars) {
        // Get the list of kernable chars (letters and punctuation only)
        char[] kernableChars = new char[chars.length];
        int numKernableChars = 0;
        for (char curr : chars) {
            if (isKernable(curr)) {
                kernableChars[numKernableChars] = curr;
                numKernableChars++;
            }
        }

        if (numKernableChars == 0) {
            return new ArrayList<Kerning>();
        }
        else {
            List<Kerning> pairs = getKerningPairsViaLayout(context, font, kernableChars, numKernableChars);
            if (pairs.size() == 0) {
                // No pairs reported. Currently this happens on Mac OS X Java 6.
                // Try auto-kerning
                pairs = getKerningPairsViaAutoKern(context, font, kernableChars, numKernableChars);
            }
            return pairs;
        }
    }

    private static boolean isKernable(char ch) {
        return Character.isLetter(ch) || isPunctuation(ch);
    }

    private static boolean isPunctuation(char ch) {
        int type = Character.getType(ch);
        return type == Character.CONNECTOR_PUNCTUATION ||
                type == Character.DASH_PUNCTUATION ||
                type == Character.END_PUNCTUATION ||
                type == Character.FINAL_QUOTE_PUNCTUATION ||
                type == Character.INITIAL_QUOTE_PUNCTUATION ||
                type == Character.OTHER_PUNCTUATION ||
                type == Character.START_PUNCTUATION;
    }
    
    //
    // Get kerning values from text layout.
    // This works on Windows.
    //

    private static List<Kerning> getKerningPairsViaLayout(FontRenderContext context, Font font, 
            char[] chars, int numChars) {
        List<Kerning> pairs = new ArrayList<Kerning>();

        Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);

        Font kerningFont = font.deriveFont(attributes);

        for (int i = 0; i < numChars; i++) {
            char curr = chars[i];
            for (int j = 0; j < numChars; j++) {
                char next = chars[j];
                float kerning = getKerningViaLayout(context, kerningFont, curr, next);
                if (kerning != 0) {
                    pairs.add(new Kerning(curr, next, kerning));
                }
            }
        }
        return pairs;
    }

    /**
        Gets the kerning between two chars. Assumes the font has the KERNING_ON attribute but
        not LIGATURES_ON. Note: This doesn't work on Java 6 on Mac OS X.
    */
    private static float getKerningViaLayout(FontRenderContext context, Font font, char curr, char next) {

        GlyphVector glyphVector = font.layoutGlyphVector(context, new char[] { curr, next },
            0, 2, Font.LAYOUT_LEFT_TO_RIGHT);

        float advance = glyphVector.getGlyphMetrics(0).getAdvance();
        double x1 = glyphVector.getGlyphPosition(0).getX();
        double x2 = glyphVector.getGlyphPosition(1).getX();
        float xdif = (float)(x2 - x1);
        float kerning = xdif - advance;

        return kerning;
    }

    //
    // Auto-kerning
    //

    private static List<Kerning> getKerningPairsViaAutoKern(FontRenderContext context, Font font,
            char[] chars, int numChars) {

        // Create all Glyphs (Outline Shapes and Metrics)
        GlyphInfo[] glyphs = new GlyphInfo[numChars];
        for (int i = 0; i < numChars; i++) {
            glyphs[i] = createGlyph(context, font, chars[i]);
        }

        // Get regular distance to compare to. (Distance between two H characters)
        GlyphInfo regularGlyph = createGlyph(context, font, 'H');
        float normalDistance = regularGlyph.getDistance(regularGlyph);

        // Create pairs
        List<Kerning> pairs = new ArrayList<Kerning>();
        for (GlyphInfo curr : glyphs) {
            for (GlyphInfo next : glyphs) {
                float distance = curr.getDistance(next);
                float kerning = normalDistance - distance;
                if (kerning < 0) {
                    pairs.add(new Kerning(curr.ch, next.ch, kerning));
                }
            }
        }
        return pairs;
    }

    private static GlyphInfo createGlyph(FontRenderContext context, Font font, char ch) {
        GlyphVector glyphVector = font.createGlyphVector(context, new char[] { ch });
        GlyphMetrics metrics = glyphVector.getGlyphMetrics(0);
        Shape shape = glyphVector.getGlyphOutline(0);
        return new GlyphInfo(ch, metrics, shape);
    }

    private static class GlyphInfo {
        final char ch;
        final float advance;
        final List<Line2D.Float> segments;
        final SortedSet<Float> vertexYLocations;
        final Rectangle2D.Float segmentBounds;

        GlyphInfo(char ch, GlyphMetrics metrics, Shape shape) {
            this.ch = ch;
            this.advance = metrics.getAdvance();
            this.vertexYLocations = getVertexYLocations(shape);
            this.segments = GeomUtil.shapeToSegments(shape);
            this.segmentBounds = GeomUtil.getBounds(segments);

            // Sort segments by y for quick exit in findFirstXIntersection
            Collections.sort(segments, new Comparator<Line2D.Float>() {
                public int compare(Line2D.Float o1, Line2D.Float o2) {
                    float o1y = Math.min(o1.y1, o1.y2);
                    float o2y = Math.min(o2.y1, o2.y2);
                    return Float.compare(o1y, o2y);
                }
            });
        }

        float getDistance(GlyphInfo next) {
            // First test: check if the shapes intersect horizontally
            final float intersectionMinY = Math.max(segmentBounds.y, next.segmentBounds.y);
            final float intersectionMaxY = Math.min(segmentBounds.y + segmentBounds.height,
                    next.segmentBounds.y + next.segmentBounds.height);
            if (intersectionMaxY < intersectionMinY) {
                return 0;
            }

            float minDistance = Float.POSITIVE_INFINITY;

            // Get the minimum possible distance
            final float leftX = segmentBounds.x + segmentBounds.width;
            final float rightX = next.segmentBounds.x;
            final float minPossibleDistance = (rightX + advance) - leftX;

            // Get all possible y locations within the intersection
            SortedSet<Float> yLocations = new TreeSet<Float>();
            yLocations.addAll(vertexYLocations);
            yLocations.addAll(next.vertexYLocations);
            Iterator<Float> i = yLocations.iterator();
            while (i.hasNext()) {
                float y = i.next();
                if (y > intersectionMaxY) {
                    break;
                }
                else if (y >= intersectionMinY) {
                    minDistance = Math.min(minDistance, calcDistance(segments, leftX, next.segments,
                            rightX, advance, y));
                    if (minDistance <= minPossibleDistance) {
                        // Early exit - minimum possible distance already found
                        return minDistance;
                    }
                }
            }

            // Fill in extra y locations
            final float maxDY = 1;
            i = yLocations.iterator();
            if (i.hasNext()) {
                float prevY = i.next();
                while (i.hasNext()) {
                    float nextY = i.next();
                    float dy = nextY - prevY;

                    if (dy > maxDY) {
                        int steps = (int)(dy / maxDY);
                        for (int j = 0; j < steps; j++) {
                            float y = prevY + dy * (j + 1) / (steps + 1);
                            if (y >= intersectionMinY && y <= intersectionMaxY) {
                                minDistance = Math.min(minDistance, calcDistance(segments, leftX,
                                        next.segments, rightX, advance, y));
                                if (minDistance <= minPossibleDistance) {
                                    // Early exit - minimum possible distance already found
                                    return minDistance;
                                }
                            }
                        }
                    }

                    prevY = nextY;
                }
            }

            if (Float.isInfinite(minDistance)) {
                return 0;
            }
            else {
                return minDistance;
            }
        }
    }

    private static SortedSet<Float> getVertexYLocations(Shape shape) {
        SortedSet<Float> points = new TreeSet<Float>();
        float[] coords = new float[6];
        PathIterator i = shape.getPathIterator(null);
        while (!i.isDone()) {
            int s = i.currentSegment(coords);
            if (s == PathIterator.SEG_MOVETO || s == PathIterator.SEG_LINETO) {
                points.add(coords[1]);
            }
            else if (s == PathIterator.SEG_QUADTO) {
                points.add(coords[3]);
            }
            else if (s == PathIterator.SEG_CUBICTO) {
                points.add(coords[5]);
            }
            i.next();
        }
        return points;
    }

    private static float calcDistance(List<Line2D.Float> leftShape, float leftLimitX, 
            List<Line2D.Float> rightShape, float rightLimitX,
            float advance, float y) {
        float leftX = findFirstXIntersection(leftShape, y, false, leftLimitX);
        float rightX = findFirstXIntersection(rightShape, y, true, rightLimitX);

        if (!Float.isInfinite(leftX) && !Float.isInfinite(rightX)) {
            rightX += advance;
            return rightX - leftX;
        }
        else {
            return Float.POSITIVE_INFINITY;
        }
    }

    private static float findFirstXIntersection(List<Line2D.Float> segments,
            float horizontalLineY, boolean minimum, float limitX) {
        float intersectionX = minimum ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        for (Line2D.Float segment : segments) {
            if (horizontalLineY < segment.y1 && horizontalLineY < segment.y2) {
                // Early exit because the segments list is sorted
                return intersectionX;
            }
            else if (horizontalLineY > segment.y1 && horizontalLineY > segment.y2) {
                // No intersection: horizontal line out of bounds of segment
            }
            else if (segment.y1 == segment.y2) {
                // No intersection: Segment is horizontal
            }
            else {
                // Intersection
                float sx = segment.x1 + (segment.y1 - horizontalLineY) *
                        (segment.x2 - segment.x1) / (segment.y1 - segment.y2);
                if (minimum) {
                    intersectionX = Math.min(intersectionX, sx);
                    if (intersectionX <= limitX) {
                        return intersectionX;
                    }
                }
                else {
                    intersectionX = Math.max(intersectionX, sx);
                    if (intersectionX >= limitX) {
                        return intersectionX;
                    }
                }
            }
        }
        return intersectionX;
    }
}

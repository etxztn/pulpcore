package org.pulpcore.tools.imagefont;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class GeomUtil {
    
    private GeomUtil() {
        
    }

    /**
     Converts a Shape into a list of line segments. The flatness is 0.5.
     */
    public static List<Line2D.Float> shapeToSegments(Shape shape) {
        return shapeToSegments(shape, 0.5);
    }

    /**
     Converts a Shape into a list of line segments.
     */
    public static List<Line2D.Float> shapeToSegments(Shape shape, double flatness) {
        List<Line2D.Float> segments = new ArrayList<Line2D.Float>();
        float[] coords = new float[6];
        float startX = 0;
        float startY = 0;
        float x1 = 0;
        float y1 = 0;
        PathIterator i = shape.getPathIterator(null, flatness);
        while (!i.isDone()) {
            int s = i.currentSegment(coords);
            if (s == PathIterator.SEG_MOVETO) {
                startX = coords[0];
                startY = coords[1];
                x1 = startX;
                y1 = startY;
            }
            else {
                float x2;
                float y2;
                if (s == PathIterator.SEG_LINETO) {
                    x2 = coords[0];
                    y2 = coords[1];
                }
                else { // PathIterator.SEG_CLOSE
                    x2 = startX;
                    y2 = startY;
                }

                segments.add(new Line2D.Float(x1, y1, x2, y2));

                x1 = x2;
                y1 = y2;
            }
            i.next();
        }
        return segments;
    }

    public static Rectangle2D.Float getBounds(List<Line2D.Float> segments) {
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Line2D.Float segment : segments) {
            minX = Math.min(minX, segment.x1);
            minX = Math.min(minX, segment.x2);
            maxX = Math.max(maxX, segment.x1);
            maxX = Math.max(maxX, segment.x2);
            minY = Math.min(minY, segment.y1);
            minY = Math.min(minY, segment.y2);
            maxY = Math.max(maxY, segment.y1);
            maxY = Math.max(maxY, segment.y2);
        }
        return new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
    }
}

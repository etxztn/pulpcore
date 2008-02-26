import pulpcore.animation.Color;
import pulpcore.animation.Easing;
import pulpcore.animation.Timeline;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import static pulpcore.image.Colors.*;

/**
    Tool for creating gradient Colors and images.
    Exmples:
    <pre>
    import static pulpcore.image.Colors.*;
    ...
    CoreImage dusk = GradientTool.create(GradientTool.VERTICAL, w, h, BLACK, BLUE);
    CoreImage rainbow = GradientTool.create(GradientTool.HORIZONTAL, w, h, 
        new double[] { 0, 1/6.0, 2/6.0, 3/6.0, 4/6.0, 5/6.0, 1 }, 
        new int[] { hue(0), hue(43), hue(85), hue(128), hue(170), hue(213), hue(255) });
    </pre>
*/
public class GradientTool {
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    
    public static Timeline create(Color color, int startARGB, int endARGB, int duration) {
        return create(color, new double[] { 0, 1 }, new int[] { startARGB, endARGB }, duration);
    }
    
    /**
        Creates a color gradient that animates over time.
        @param color The color property to animate.
        @param points The points of each color, sorted from 0 to 1.
        @param colors The ARGB colors that occurs at each points. Must be the same length as 
        the points list.
        @param duration The duration, in milliseconds.
        @return A Timeline that contains the color's gradient over time.
    */
    public static Timeline create(Color color, double[] points, int[] colors, int duration) {
        // Check for valid input
        String pointsErrorMessage = "Points array must be sorted, between 0 to 1, " +
            "and have no duplicates";
        if (points == null || points.length < 2 || points[0] < 0) {
            throw new IllegalArgumentException(pointsErrorMessage);
        }
        double prev = points[0];
        for (int i = 1; i < points.length; i++) {
            if (points[i] <= prev) {
                throw new IllegalArgumentException(pointsErrorMessage);
            }
            prev = points[i];
        }
        if (points[points.length - 1] > 1) {
            throw new IllegalArgumentException(pointsErrorMessage);
        }
        if (colors == null || colors.length != points.length) {
            throw new IllegalArgumentException("Number of colors must match number of points");
        }
        
        // Create the animation
        duration--;
        int prevColor;
        int startIndex;
        int delay = 0;
        if (points[0] > 0) {
            prevColor = TRANSPARENT;
            startIndex = 0;
        }
        else {
            prevColor = colors[0];
            startIndex = 1;
        }
        color.set(prevColor);
        Timeline timeline = new Timeline();
        for (int i = startIndex; i < points.length; i++) {
            int currDuration = (int)Math.round(points[i] * duration) - delay;
            timeline.animate(color, prevColor, colors[i], currDuration, Easing.NONE, delay);
            prevColor = colors[i];
            delay += currDuration;
        }
        if (points[points.length - 1] < 1) {
            timeline.animate(color, prevColor, TRANSPARENT, duration - delay, Easing.NONE, delay);
        }
        return timeline;
    }
    
    /**
        Creates an gradient image.
        @param orientation either VERTICAL or HORIZONTAL
        @param width The width of the newly created image
        @param height The height of the newly created image
        @param startARGB The start color
        @param endARGB The end color
        @return The image.
    */
    public static CoreImage create(int orientation, int width, int height, 
        int startARGB, int endARGB) 
    {
        return create(orientation, width, height, new double[] { 0, 1 }, 
            new int[] { startARGB, endARGB }); 
    }
    
    /**
        Creates an gradient image.
        @param orientation either VERTICAL or HORIZONTAL
        @param width The width of the newly created image
        @param height The height of the newly created image
        @param points The points of each color, sorted from 0 to 1.
        @param colors The ARGB colors that occurs at each points. Must be the same length as 
        the points list.
        @return The image.
    */
    public static CoreImage create(int orientation, int width, int height,
        double[] points, int[] colors)
    {
        // Create gradient
        Color color = new Color();
        int length = (orientation == HORIZONTAL) ? width : height;
        Timeline gradient = create(color, points, colors, length);
        
        // Create image
        boolean isOpaque = true;
        if (points[0] > 0 || points[points.length - 1] < 1) {
            isOpaque = false;
        }
        else {
            for (int i = 0; i < colors.length; i++) {
                if (getAlpha(colors[i]) < 255) {
                    isOpaque = false;
                    break;
                }
            }
        }
        CoreImage image = new CoreImage(width, height, isOpaque);
        
        // Draw
        CoreGraphics g = image.createGraphics();
        int lineWidth = (orientation == HORIZONTAL) ? 1 : width;
        int lineHeight = (orientation == HORIZONTAL) ? height : 1;
        int dx = (orientation == HORIZONTAL) ? 1 : 0;
        int dy = (orientation == HORIZONTAL) ? 0 : 1;
        int x = 0;
        int y = 0;
        
        for (int i = 0; i < length; i++) {
            g.setColor(color.get());
            g.fillRect(x, y, lineWidth, lineHeight);
            x += dx;
            y += dy;
            gradient.update(1);
        }
        
        return image;
    }
}

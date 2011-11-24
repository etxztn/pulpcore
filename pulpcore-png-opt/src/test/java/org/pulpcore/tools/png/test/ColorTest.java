package org.pulpcore.tools.png.test;

import org.junit.Test;
import org.pulpcore.tools.png.ImageUtil;
import static org.junit.Assert.*;

public class ColorTest {

    @Test public void testColorFlattening() {
        for (int alpha = 0; alpha < 256; alpha++) {
            for (int value = 0; value < 256; value++) {
                int color = (alpha << 24) | (value << 16) | (value << 8) | value;
                int flattenedColor = ImageUtil.flattenColor(color);
                int flattenedColor2 = ImageUtil.unpremultiply(ImageUtil.premultiply(color));
                assertEquals("Flattened color not identical=" + alpha + " value=" + value,
                        flattenedColor, ImageUtil.flattenColor(color));
                assertEquals("Flattened Color incorrect for alpha=" + alpha + " value=" + value,
                        flattenedColor, flattenedColor2);
            }
        }
    }
}

package org.pulpcore.tools.packer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;

public class ImageUtils {

    private ImageUtils() {

    }

    public static BufferedImage getScaledInstance(BufferedImage srcImage, double scale) {
        return getScaledInstance(srcImage,
                (int)Math.ceil(srcImage.getWidth() * scale),
                (int)Math.ceil(srcImage.getHeight() * scale));
    }

    public static BufferedImage getScaledInstance(BufferedImage srcImage, int width, int height) {
        ImageFilter filter = new AreaAveragingScaleFilter(width, height);
        ImageProducer producer = new FilteredImageSource(srcImage.getSource(), filter);
        Image scaledImage = Toolkit.getDefaultToolkit().createImage(producer);

        boolean srcIsOpaque = srcImage.getTransparency() == Transparency.OPAQUE;
        BufferedImage buf = new BufferedImage(width, height,
                srcIsOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.drawImage(scaledImage,0,0,null);
        g.dispose();
        return buf;

    }

    public static int[] getData(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = null;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB ||
                image.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
            data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            if (data.length != w * h) {
                data = null;
            }
        }
        if (data == null) {
            data = image.getRGB(0, 0, w, h, null, 0, w);
        }
        return data;
    }

    /**
     Returns a subimage of an image, copying the raster data.
     */
    public static BufferedImage subImage(BufferedImage image, int x, int y, int w, int h) {
        BufferedImage subImage = new BufferedImage(w, h, image.getType());
        Graphics2D g = subImage.createGraphics();
        g.drawImage(image, -x, -y, null);
        g.dispose();
        return subImage;
    }

    /**
     Gets the visual bounds of the image, or returns null if the image is transparent.
     */
    public static Rectangle getVisibleBounds(BufferedImage image) {
        if (image.getTransparency() == Transparency.OPAQUE) {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int[] data = getData(image);
        
        int boundX1 = w;
        int boundX2 = -1;
        int boundY1 = h;
        int boundY2 = -1;

        int offset = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if ((data[offset] >>> 24) > 0) {
                    boundX1 = Math.min(boundX1, i);
                    boundX2 = Math.max(boundX2, i);
                    boundY1 = Math.min(boundY1, j);
                    boundY2 = Math.max(boundY2, j);
                }
                offset++;
            }
        }

        Rectangle bounds = new Rectangle(boundX1, boundY1, boundX2 - boundX1 + 1, boundY2 - boundY1 + 1);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        return bounds;
    }

}

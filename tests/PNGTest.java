import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.Test;
import pulpcore.image.CoreImage;
import pulpcore.image.PNGWriter;
import pulpcore.util.ByteArray;
import static org.junit.Assert.*;

/**
    This class tests reading PNG and JPEG images, and writing 
    1. Write a PNG using 
*/
public class PNGTest {
    
    private CoreImage image;
    
    public PNGTest() {
        // Create a random image
        Random rand = new Random();
        int[] data = new int[100*100];
        image = new CoreImage(100, 100, false, data);
        for (int i = 0; i < data.length; i++) {
            int color = rand.nextInt();
            data[i] = premultiply(color);
        }
    }
    
    static int premultiply(int argbColor) {
        int a = argbColor >>> 24;
        int r = (argbColor >> 16) & 0xff;
        int g = (argbColor >> 8) & 0xff;
        int b = argbColor & 0xff;
    
        r = (a * r + 127) / 255;
        g = (a * g + 127) / 255;
        b = (a * b + 127) / 255;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    static int[] getComponents(int argbColor) {
        return new int[] {
            argbColor >>> 24,
            (argbColor >> 16) & 0xff,
            (argbColor >> 8) & 0xff,
            argbColor & 0xff,
        };
    }
    
    /**
        Tests PNGWriter
    */
    @Test public void writePNG() throws IOException {
        // Write the image
        ByteArray pngData = PNGWriter.write(image);
        
        // Read as a BufferedImage, convert to TYPE_INT_ARGB_PRE
        ByteArrayInputStream in = new ByteArrayInputStream(pngData.getData());
        BufferedImage image2 = ImageIO.read(in);
        BufferedImage image2Pre = new BufferedImage(image2.getWidth(), image2.getHeight(), 
            BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics g = image2Pre.createGraphics();
        g.drawImage(image2, 0, 0, null);
        g.dispose();
        
        // Compare the two images
        // Test succeeds if the difference between each component is within maxDelta
        // (The difference occurs because of conversion to/from premultiplied alpha
        int[] data = image.getData();
        int[] data2 = ((DataBufferInt)image2Pre.getData().getDataBuffer()).getData();
        int maxDelta = 1;
        for (int i = 0; i < data.length; i++) {
            int[] x = getComponents(data[i]);
            int[] y = getComponents(data2[i]);
            for (int j = 0; j < x.length; j++) {
                assertEquals("Bad result", x[j], y[j], maxDelta);
            }
        }
    }
}

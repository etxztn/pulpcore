
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.Test;
import pulpcore.image.BlendMode;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.image.PNGWriter;
import pulpcore.scene.Scene;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;
import pulpcore.util.ByteArray;
import static org.junit.Assert.*;

public class GraphicsTest {

    // Tests clipping a scaled image
    public static class ClipError extends Scene2D {
        ImageSprite sprite1;
        FilledSprite sprite2;

        @Override
        public void load() {

            add(new FilledSprite(Colors.BLACK));

            sprite1 = new ImageSprite("res/stripe.png", 320, 180, 600, 40);
            sprite1.alpha.set(128);
            sprite1.setAnchor(0.5, 0.5);

            sprite2 = new FilledSprite(200, 180, 60, 60, 0x66660000);
            sprite2.setAnchor(0.5, 0.5);
            sprite2.setBorderSize(4);
            sprite2.borderColor.set(Colors.TRANSPARENT);

            add(sprite1);
            add(sprite2);
        }

        @Override
        public void update(int elapsedTime) {
            sprite2.setDirty(true);
        }
    }

    @Test
    public void ClipError() {
        testScene(new ClipError(), 2);
    }

    // Tests using the Mult blend mode to perform masking
    public static class MaskTest extends Scene2D {

        @Override
        public void load() {
            add(new FilledSprite(Colors.BLUE));

            Group group = new Group(0, 0, 640, 480);
            group.createBackBuffer();
            group.add(new ImageSprite("res/starmask.png", 0, 0));

            Sprite sprite = new ImageSprite("res/stripe.png", 0, 0, 640, 480);
            sprite.setBlendMode(BlendMode.Multiply());
            group.add(sprite);

            add(group);
        }
    }

    @Test
    public void MaskTest() {
        testScene(new MaskTest());
    }

    //
    //
    //

    private void testScene(Scene scene) {
        testScene(scene, 1);
    }

    private void testScene(Scene scene, int loopIterations) {
        HeadlessApp app = new HeadlessApp(scene);

        scene.load();
        scene.showNotify();

        for (int i = 0; i < loopIterations; i++) {
            CoreGraphics g = app.getSurface().getGraphics();
            g.reset();
            scene.updateScene(0);
            scene.drawScene(g);
        }

        compareImage(scene.getClass().getSimpleName(), app.getOutput());
    }

    private void writeImage(CoreImage image, File file) throws IOException {
        ByteArray pngData = PNGWriter.write(image);
        FileOutputStream out = new FileOutputStream(file);
        out.write(pngData.getData());
        out.close();
    }

    private static int[] getComponents(int argbColor) {
        return new int[] {
            argbColor >>> 24,
            (argbColor >> 16) & 0xff,
            (argbColor >> 8) & 0xff,
            argbColor & 0xff,
        };
    }

    private void compareImage(String name, CoreImage image) {
        try {
            File imageFile = new File("output/" + name + ".png");
            if (!imageFile.exists()) {
                writeImage(image, imageFile);
                System.out.println("Wrote new test image: " + imageFile);
            }
            else {
                BufferedImage image2 = ImageIO.read(imageFile);
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
                        if (Math.abs(x[j] - y[j]) > maxDelta) {
                            File errorFile = new File("output/" + name + "-error.png");
                            writeImage(image, errorFile);
                            fail("Wrote failed image: " + errorFile);
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            fail(ex.getMessage());
        }
    }
}

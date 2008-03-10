// Flashlight
// Click to turn on the lights.
import pulpcore.image.BlendMode;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import static pulpcore.image.Colors.*;

public class Flashlight extends Scene2D {
    
    int[] colors = { rgb(0x365fb7), rgb(0x799ae0), rgb(0x7abaf2), rgb(0x133463) };
    ImageSprite cursor;
    Group maskLayer;
    
    public void load() {
        setCursor(Input.CURSOR_OFF);
        cursor = new ImageSprite("glow.png", 0, 0);
        cursor.setAnchor(Sprite.CENTER);
        cursor.visible.set(false);
        
        // Create the mask 
        maskLayer = new Group();
        maskLayer.add(new FilledSprite(gray(18)));
        maskLayer.add(cursor);
        maskLayer.setBlendMode(BlendMode.Multiply());
        maskLayer.createBackBuffer();
        
        // Create the background image
        Group imageLayer = new Group();
        imageLayer.add(new FilledSprite(rgb(0xba9b65)));
        int size = 80;
        int spacing = 16;
        for (int i = -spacing/2; i < Stage.getWidth(); i += spacing + size) {
            for (int j = -spacing/2; j < Stage.getHeight(); j += spacing + size) {
                int randomColor = colors[CoreMath.rand(colors.length - 1)];
                imageLayer.add(new FilledSprite(i, j, size, size, randomColor));
            }
        }
        
        // Add the layers to the scene
        addLayer(imageLayer);
        addLayer(maskLayer);
    }
    
    public void update(int elapsedTime) {
        cursor.setLocation(Input.getMouseX(), Input.getMouseY());
        cursor.visible.set(Input.isMouseInside());
        
        if (Input.isMousePressed()) {
            maskLayer.visible.toggle();
            setCursor(maskLayer.visible.get() ? Input.CURSOR_OFF : Input.CURSOR_DEFAULT);
        }
    }
}

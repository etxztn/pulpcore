import pulpcore.image.CoreFont;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.Stage;

public class ImagesAndFonts extends Scene2D {
    
    ImageSprite cursor;
    ImageSprite person;
    CoreImage personLeft;
    CoreImage personRight;
    CoreImage personUp;
    CoreImage personDown;
    
    public void load() {
        add(new ImageSprite("background.png", 0, 0));
        
        add(new Label(CoreFont.load("simple.font.png"), "Fonts & Animated Images", 205, 14));
        add(new Label(CoreFont.load("complex.font.png"), "PulpCore.", 225, 200));
                
        personRight = CoreImage.load("player-right.png");
        personLeft = personRight.mirror();
        personUp = CoreImage.load("player-up.png");
        personDown = CoreImage.load("player-down.png");
        person = new ImageSprite(personLeft, 320, 180);
        add(person);
        
        Input.setCursor(Input.CURSOR_OFF);
        cursor = new ImageSprite("cursor.png", 0, 0);
        add(cursor);
    }
    
    public void update(int elapsedTime) {
        cursor.setLocation(Input.getMouseX(), Input.getMouseY());
        
        float dx = 0;
        float dy = 0;
        CoreImage newImage = null;
        if (person.y.get() < cursor.y.get() - 1) {
            dy = 1;
            newImage = personDown;
        }
        else if (person.y.get() > cursor.y.get() + 1) {
            dy = -1;
            newImage = personUp;
        }
        if (person.x.get() < cursor.x.get() - 1) {
            dx = 2;
            newImage = personRight;
        }
        else if (person.x.get() > cursor.x.get() + 1) {
            dx = -2;
            newImage = personLeft;
        }
        if (newImage == null) {
            newImage = personDown;
        }

        person.setImage(newImage);
        person.x.set(person.x.get() + dx * elapsedTime / 20);
        person.y.set(person.y.get() + dy * elapsedTime / 20);
    }
    
}
import pulpcore.CoreSystem;
import pulpcore.image.CoreFont;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sound.SoundClip;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;

public class HelloWorld extends Scene2D {
    
    Label label;
    
    public void load() {
        add(new ImageSprite("background.png", 0, 0));
        
        String message = CoreSystem.getAppProperty("message");
        
        CoreFont font = CoreFont.load("hello.font.png");
        label = new Label(font, message, 320, 240);
        label.setAnchor(Sprite.HCENTER | Sprite.VCENTER);
        add(label);
        
        SoundClip sound = SoundClip.load("sound.wav");
        sound.play();
    }
    
    public void update(int elapsedTime) {
        double angle = 0.006 * (Input.getMouseX() - 275);
        int duration = 100;
        label.angle.animateTo(angle, duration);
    }
}

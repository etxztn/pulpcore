import pulpcore.image.CoreFont;
import pulpcore.scene.Scene2D;
import pulpcore.sound.SoundClip;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import static pulpcore.Input.*;

public class HelloWorld extends Scene2D {
    
    Label label;
    
    @Override
    public void load() {
        add(new ImageSprite("background.png", 0, 0));
        
        CoreFont font = CoreFont.load("hello.font.png");
        label = new Label(font, "Hello World", 320, 240);
        label.setAnchor(Sprite.CENTER);
        add(label);
        
        SoundClip sound = SoundClip.load("sound.wav");
        sound.play();
    }
    
    @Override
    public void update(int elapsedTime) {
        double angle = 0.006 * (getMouseX() - 320);
        int duration = 100;
        label.angle.animateTo(angle, duration);
    }
}

import pulpcore.scene.Scene2D;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;

public class HelloWorld extends Scene2D {

    @Override
    public void load() {
        add(new ImageSprite("background.png", 0, 0));
        add(new Label("Hello World!", 50, 50));
    }
    
    @Override
    public void update(int elapsedTime) {
        
    }
}


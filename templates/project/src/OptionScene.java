import pulpcore.animation.event.SceneChangeEvent;
import pulpcore.image.CoreFont;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

public class OptionScene extends Scene2D {
    
    Button toggleButton;
    Button backButton;
    
    @Override
    public void load() {
        toggleButton = Button.createLabeledToggleButton("Some Toggle", 320, 320);
        toggleButton.setAnchor(Sprite.CENTER);
        backButton = Button.createLabeledButton("<< Back", 320, 370);
        backButton.setAnchor(Sprite.CENTER);
        
        add(new ImageSprite("background.png", 0, 0));
        add(toggleButton);
        add(backButton);
    }
    
    @Override 
    public void update(int elapsedTime) {
        if (backButton.isClicked()) {
            // Go back to the previous scene
            Stage.popScene();
        }
    }
}

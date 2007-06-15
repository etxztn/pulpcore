import pulpcore.animation.event.SceneChangeEvent;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.Stage;

public class Scenes extends Scene2D {
    
    Button button;
        
    public void load() {
        button = new Button(CoreImage.load("button.png").split(3), 400, 340);
        add(new FilledSprite(CoreGraphics.GREEN));
        add(button);
    }
    
    public void update(int elapsedTime) {
        if (button.isClicked()) {
            Stage.setScene(new BlueScene());
        }
    }
    

    // Change Scenes after a delay
    class BlueScene extends Scene2D {
        
        Button button;
        
        public void load() {
            button = new Button(CoreImage.load("button.png").split(3), 400, 340);
            add(new FilledSprite(CoreGraphics.BLUE));
            add(button);
        }
        
        public void update(int elapsedTime) {
            if (button.isClicked()) {
                button.enabled.set(false);
                button.moveTo(550, 340, 200);
                addEvent(new SceneChangeEvent(new RedScene(), 200));
            }
        }
    }
    
    
    // Scene Interrupt
    class RedScene extends Scene2D {
        
        Button button;
        
        public void load() {
            button = new Button(CoreImage.load("button.png").split(3), 400, 340);
            add(new FilledSprite(CoreGraphics.RED));
            add(button);
        }
        
        public void update(int elapsedTime) {
            if (button.isClicked()) {
                Stage.interruptScene(new PurpleScene());
            }
        }
    }
    
    // Return from scene interrupt
    class PurpleScene extends Scene2D {
        
        Button button;
        
        public void load() {
            button = new Button(CoreImage.load("button.png").split(3), 400, 340);
            add(new FilledSprite(CoreGraphics.MAGENTA));
            add(button);
        }
        
        public void update(int elapsedTime) {
            if (button.isClicked()) {
                Stage.gotoInterruptedScene();
            }
        }
    }
}
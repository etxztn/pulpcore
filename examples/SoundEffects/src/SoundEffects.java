// Plays sounds with level and pan animation.
import pulpcore.animation.Fixed;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sound.SoundClip;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.Stage;

public class SoundEffects extends Scene2D {
    
    SoundClip boopSound, wooshSound;
    ImageSprite ear;
    
    public void load() {
        ear = new ImageSprite("ear.png", 300, 180);
        add(new FilledSprite(0xffffff));
        add(ear);
        Input.setCursor(Input.CURSOR_CROSSHAIR);
        
        boopSound = SoundClip.load("boop.wav");
        wooshSound = SoundClip.load("stereo.wav");
        wooshSound.play();
    }
    
    public void update(int elapsedTime) {
        if (ear.isMousePressed()) {
            // Animate from left to right speaker
            Fixed level = new Fixed(1);
            Fixed pan = new Fixed(-1);
            pan.animateTo(1, 500);
            
            wooshSound.play(level, pan);
        }
        else if (Input.isMousePressed()) {
            // Set the pan of the sound based on the mouse position
            int x = Input.getMousePressX();
            double w = Stage.getWidth() / 2;
            Fixed level = new Fixed(1);
            Fixed pan = new Fixed((x - w) / w);
            
            boopSound.play(level, pan);
        }
    }
}

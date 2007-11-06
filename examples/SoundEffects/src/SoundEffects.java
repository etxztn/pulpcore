// Plays sounds with level and pan animation.
// Try clicking near the ear and far from the ear.
import pulpcore.animation.Fixed;
import pulpcore.CoreSystem;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sound.SoundClip;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.Stage;

public class SoundEffects extends Scene2D {
    
    SoundClip boopSound, wooshSound;
    ImageSprite ear;
    Button muteButton;
    
    public void load() {
        ear = new ImageSprite("ear.png", 300, 180);
        muteButton = new Button(CoreImage.load("mute.png").split(3,2), 600, 440, true);
        muteButton.setSelected(!CoreSystem.isMute());
        add(new FilledSprite(0xffffff));
        add(ear);
        add(muteButton);
        Input.setCursor(Input.CURSOR_CROSSHAIR);
        
        boopSound = SoundClip.load("boop.wav");
        wooshSound = SoundClip.load("stereo.wav");
        wooshSound.play();
    }
    
    public void update(int elapsedTime) {
        if (muteButton.isClicked()) {
            CoreSystem.setMute(!muteButton.isSelected());
        }
        if (ear.isMousePressed()) {
            // Animate from left to right speaker
            Fixed level = new Fixed(1);
            Fixed pan = new Fixed(-1);
            pan.animateTo(1, 500);
            
            wooshSound.play(level, pan);
        }
        if (Input.isMousePressed() && !ear.isMouseOver() && !muteButton.isMouseOver()) {
            // Set the pan of the sound based on the mouse position
            int x = Input.getMousePressX();
            double w = Stage.getWidth() / 2;
            Fixed level = new Fixed(1);
            Fixed pan = new Fixed((x - w) / w);
            
            boopSound.play(level, pan);
        }
    }
}

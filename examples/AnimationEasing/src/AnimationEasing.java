// AnimationEasing 
// Click to view various easings.

import pulpcore.Input;
import pulpcore.animation.Easing;
import pulpcore.animation.Timeline;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;

public class AnimationEasing extends Scene2D {
    
    // Ease t from 0 to 1.
    Easing customEasing = new Easing() {
        protected float ease(float t) {
            return (float)Math.sin(t*2*Math.PI)/3+t;
        }   
    };
        
    Easing[] easings = { 
        Easing.NONE,
        Easing.REGULAR_IN, Easing.REGULAR_OUT, Easing.REGULAR_IN_OUT,
        Easing.STRONG_IN, Easing.STRONG_OUT, Easing.STRONG_IN_OUT,
        Easing.BACK_IN, Easing.BACK_OUT, Easing.BACK_IN_OUT,
        Easing.ELASTIC_IN, Easing.ELASTIC_OUT, Easing.ELASTIC_IN_OUT,
        customEasing
    };
    
    String[] easingNames = { 
        "Easing.NONE",
        "Easing.REGULAR_IN", "Easing.REGULAR_OUT", "Easing.REGULAR_IN_OUT",
        "Easing.STRONG_IN", "Easing.STRONG_OUT", "Easing.STRONG_IN_OUT",
        "Easing.BACK_IN", "Easing.BACK_OUT", "Easing.BACK_IN_OUT",
        "Easing.ELASTIC_IN", "Easing.ELASTIC_OUT", "Easing.ELASTIC_IN_OUT",
        "customEasing"
    };
    
    Sprite icon;
    Label label;
    Timeline timeline;
    int easingIndex;
    
    public void load() {
        CoreFont font = CoreFont.getSystemFont().tint(CoreGraphics.WHITE);
        icon = new ImageSprite("earth.png", 140, 240);
        icon.setAnchor(Sprite.HCENTER | Sprite.VCENTER);
        label = new Label(font, "", 320, 20);
        label.setAnchor(Sprite.HCENTER | Sprite.TOP);
        
        add(new ImageSprite("background.png", 0, 0));
        add(icon);
        add(label);
        
        setEasing(0);
    }
    
    private void setEasing(int index) {
        easingIndex = index;
        Easing easing = easings[index];
        label.setText(easingNames[index]);
        
        int time = 0;
        if (timeline != null) {
            time = timeline.getTime();
        }
        timeline = new Timeline();
        timeline.move(icon, 140, 240, 500, 240, 1000, easing, 500);
        timeline.move(icon, 500, 240, 140, 240, 1000, easing, 2000);
        timeline.loopForever();
        timeline.setTime(time);
    }
    
    public void update(int elapsedTime) {
        timeline.update(elapsedTime);
        
        if (Input.isMousePressed()) {
            setEasing((easingIndex + 1) % easings.length);
        }
    }
}
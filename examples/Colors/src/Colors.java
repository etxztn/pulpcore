// Colors
// Click to show different compositing modes.
import pulpcore.animation.ColorAnimation;
import pulpcore.animation.Easing;
import pulpcore.animation.Timeline;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

public class Colors extends Scene2D {
    
    String[] labels = { "Normal", "Add", "Multiply",
        "Normal, 50% alpha", "Add, 50% alpha", "Multiply, 50% alpha"};
    int[] composites = { CoreGraphics.COMPOSITE_SRC_OVER, CoreGraphics.COMPOSITE_ADD, 
        CoreGraphics.COMPOSITE_MULT, CoreGraphics.COMPOSITE_SRC_OVER, 
        CoreGraphics.COMPOSITE_ADD, CoreGraphics.COMPOSITE_MULT};
    int[] backgrounds = { Colors.GRAY, Colors.BLACK, Colors.WHITE,
        Colors.GRAY, Colors.BLACK, Colors.WHITE}; 
    int[] labelColors = { Colors.BLACK, Colors.WHITE, Colors.BLACK,
        Colors.BLACK, Colors.WHITE, Colors.BLACK};
    int[] alphas = { 255, 255, 255, 128, 128, 128 };
    
    Group[] colorGroups;
    FilledSprite background;
    int index;
    
    @Override
    public void load() {
        // Add the background
        background = new FilledSprite(Colors.GRAY);
        add(background);
        
        Timeline timeline1 = new Timeline();
        Timeline timeline2 = new Timeline();
        colorGroups = new Group[composites.length];
        for (int i = 0; i < composites.length; i++) {
            // Create the layer
            colorGroups[i] = new Group();
            colorGroups[i].alpha.set(0);
            colorGroups[i].setComposite(composites[i]);
            addLayer(colorGroups[i]);
            
            // Add three rectangles
            for (int j = 0; j < 3; j++) {
                int color = 0xff << (8 * j);
                if (colorGroups[i].getComposite() == CoreGraphics.COMPOSITE_MULT) {
                    color = ~color;
                }
                Sprite rect = new FilledSprite(Stage.getWidth() / 2, Stage.getHeight() / 2,
                    450, 150, color);
                rect.setAnchor(Sprite.CENTER);
                rect.angle.set(2 * Math.PI * j / 3);
                rect.alpha.set(alphas[i]);
                colorGroups[i].add(rect);
                
                timeline1.animate(rect.width, 450, 260, 10000, Easing.STRONG_IN_OUT);
                timeline1.animate(rect.width, 260, 450, 10000, Easing.STRONG_IN_OUT, 10000);
                timeline2.animateTo(rect.angle, rect.angle.get() + 2*Math.PI, 50000);
            }
            
            // Add the label
            CoreFont font = CoreFont.getSystemFont().tint(labelColors[i]);
            Label label = new Label(font, labels[i], Stage.getWidth() / 2, 10);
            label.setAnchor(Sprite.NORTH);
            colorGroups[i].add(label);
        }
        
        // Start
        timeline1.loopForever();
        timeline2.loopForever();
        addTimeline(timeline1);
        addTimeline(timeline2);
        setGroup(0);
        setCursor(Input.CURSOR_HAND);
    }
    
    void setGroup(int index) {
        this.index = index;
        background.fillColor.animate(new ColorAnimation(ColorAnimation.RGB,
            background.fillColor.get(), backgrounds[index], 500));
        for (Group colorGroup : colorGroups) {
            colorGroup.alpha.animateTo((colorGroup == colorGroups[index])?255:0, 500);
        }
    }
    
    @Override
    public void update(int elapsedTime) {
        if (Input.isMousePressed()) {
            setGroup((index + 1) % composites.length);
        }
    }
}

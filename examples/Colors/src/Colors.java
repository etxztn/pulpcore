// Colors
// Click to show different compositing modes.
import pulpcore.animation.ColorAnimation;
import pulpcore.animation.Timeline;
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
        "Normal, alpha=50%", "Add, alpha=50%", "Multiply, alpha=50%"};
    int[] composites = { CoreGraphics.COMPOSITE_SRC_OVER, CoreGraphics.COMPOSITE_ADD, 
        CoreGraphics.COMPOSITE_MULT, CoreGraphics.COMPOSITE_SRC_OVER, 
        CoreGraphics.COMPOSITE_ADD, CoreGraphics.COMPOSITE_MULT};
    int[] backgrounds = { CoreGraphics.GRAY, CoreGraphics.BLACK, CoreGraphics.WHITE,
        CoreGraphics.GRAY, CoreGraphics.BLACK, CoreGraphics.WHITE}; 
    int[] labelColors = { CoreGraphics.BLACK, CoreGraphics.WHITE, CoreGraphics.BLACK,
        CoreGraphics.BLACK, CoreGraphics.WHITE, CoreGraphics.BLACK};
    int[] alphas = { 255, 255, 255, 128, 128, 128 };
    
    Group[] colorGroups;
    FilledSprite background;
    int index;
    
    public void load() {
        // Add the background
        background = new FilledSprite(CoreGraphics.GRAY);
        add(background);
        
        Timeline timeline = new Timeline();
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
                    400, 150, color);
                rect.setAnchor(Sprite.CENTER);
                rect.angle.set(2 * Math.PI * j / 3);
                rect.alpha.set(alphas[i]);
                timeline.animateTo(rect.angle, rect.angle.get() + 2*Math.PI, 100000);
                colorGroups[i].add(rect);
            }
            
            // Add the label
            CoreFont font = CoreFont.getSystemFont().tint(labelColors[i]);
            Label label = new Label(font, labels[i], Stage.getWidth() / 2, 10);
            label.setAnchor(Sprite.NORTH);
            colorGroups[i].add(label);
        }
        
        // Loop the rectangle rotations
        timeline.loopForever();
        addTimeline(timeline);
        
        // Start
        setGroup(0);
    }
    
    void setGroup(int index) {
        this.index = index;
        background.fillColor.animate(new ColorAnimation(ColorAnimation.RGB,
            background.fillColor.get(), backgrounds[index], 500));
        for (int i = 0; i < colorGroups.length; i++) {
            colorGroups[i].alpha.animateTo((i == index)?255:0, 500);
        }
    }
    
    public void update(int elapsedTime) {
        if (Input.isMousePressed()) {
            setGroup((index + 1) % composites.length);
        }
    }
}

// Shows how to animate a View along a Path.
// Paths are defined with SVG path data.
package org.pulpcore.example.pathmotion;

import org.pulpcore.math.Path;
import org.pulpcore.media.Audio;
import org.pulpcore.runtime.Input.TouchEvent;
import org.pulpcore.view.ImageView;
import org.pulpcore.view.Scene;
import org.pulpcore.view.Stage;
import org.pulpcore.view.View;
import org.pulpcore.view.animation.LoopForever;
import org.pulpcore.view.ui.Label;

public class PathMotion extends Scene {
    
    // SVG path created with InkScape.
    private String svgPath = "M  181.42857,41.428571 C -13.491814,41.428571-0.00016036284,503.14876 " +
            "178.57143,439.99999 C 319.88994,390.02513 331.55729,390.01741 465.71429,438.57143 " +
            "C 637.82839,500.86285 662.71309,41.428571 448.57142,41.428571 C 102.53673,41.428571 " +
            "105.24899,329.99999 324.28571,329.99999 C 534.47946,329.99999 539.74496,41.428571 " +
            "181.42857,41.428571";

    @Override
    public void onLoad(Stage stage) {
        // Load textures and Audio
        stage.getContext().loadResources("texture.json");
        Audio.load("roll.ogg");

        setOnTouchPressListener(new OnTouchPressListener() {
            public void onPress(View view, TouchEvent event) {
                Audio.load("roll.ogg").play();
            }
        });
        
        addSubview(new Label("Hello World"));
        
        Path path = new Path(svgPath);

        int count = 7;
        float dur = 10;

        for (int i = 0; i < count; i++) {
            // Create a Man. Animate position and angle along the path.
            ImageView man = new ImageView("Man.png");
            man.setAnchor(0.5f, 0.5f);
            float position = (float)(i) / count;
            addAction(new LoopForever(path.createMoveAction(dur, man, position, position + 1, true)));
            addSubview(man);

            // Create a Spirit. Animate position (but not angle) along the path
            ImageView spirit = new ImageView("Spirit.png");
            spirit.setAnchor(0.5f, 0.5f);
            position = (i + 0.18f) / count;
            addAction(new LoopForever(path.createMoveAction(dur, spirit, position, position + 1, false)));
            addSubview(spirit);
        }
    }
}

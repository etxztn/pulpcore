#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.image.Colors;

public class ${scene} extends Scene2D {

    @Override
    public void load() {
        add(new FilledSprite(Colors.WHITE));
        add(new ImageSprite("success.png", 5, 5));
        add(new Label("Hello World!", 26, 6));
    }
    
    @Override
    public void update(int elapsedTime) {
        
    }
}


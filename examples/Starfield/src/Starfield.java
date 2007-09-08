// Starfield
// Scroll the mouse wheel or press the arrow keys to travel through the cosmos

import pulpcore.CoreSystem;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.Stage;

public class Starfield extends Scene2D {
    
    int zoomTimeRemaining;
    int zoomX;
    int zoomY;
    double zoomSpeed;

    public void load() {
        add(new FilledSprite(0x000000));
        for (int i = 0; i < 50; i++) {
            add(new Star());
        }
    }
    
    public void update(int elapsedTime) {
        if (Input.getMouseWheelRotation() != 0) {
            if (Input.getMouseWheelRotation() > 0) {
                zoom(Input.getMouseWheelX(), Input.getMouseWheelY(), -.01);
            }
            else {
                zoom(Input.getMouseWheelX(), Input.getMouseWheelY(), 1);
            }
        }
        
        if (Input.isDown(Input.KEY_UP)) {
            zoom(Stage.getWidth() / 2, Stage.getHeight() / 2, 1);
        }
        
        if (Input.isDown(Input.KEY_DOWN)) {
            zoom(Stage.getWidth() / 2, Stage.getHeight() / 2, -.01);
        }
        zoomTimeRemaining -= elapsedTime;
    }
    
    void zoom(int x, int y, double speed) {
        zoomX = x;
        zoomY = y;
        zoomSpeed = speed;
        zoomTimeRemaining = 200;
    }
    
    class Star extends FilledSprite {
        
        double z;
        
        public Star() {
            super(CoreMath.rand(0, Stage.getWidth()), CoreMath.rand(0, Stage.getHeight()),
                3, 3, 0xffffff);
            angle.set(Math.PI / 4);
            setZ(CoreMath.rand(1.0,5.0));
        }
        
        public void update(int elapsedTime) {
            super.update(elapsedTime);
            
            if (zoomTimeRemaining > 0) {
                double speed = zoomSpeed * z * z / 3000;
                double dx = (x.get() - zoomX) * elapsedTime * speed;
                double dy = (y.get() - zoomY) * elapsedTime * speed;
                z += elapsedTime * speed;
                setSize(z, z);
                setLocation(x.get() + dx, y.get() + dy);
                
                if (x.get() < 0 || x.get() >= Stage.getWidth() ||
                    y.get() < 0 || y.get() >= Stage.getHeight())
                {
                    setZ(CoreMath.rand(1.0, 3.0));
                    setLocation(
                        zoomX + CoreMath.rand(-Stage.getWidth() / 10, Stage.getWidth() / 10),
                        zoomY + CoreMath.rand(-Stage.getHeight() / 10, Stage.getHeight() / 10));
                }
            }
        }
        
        void setZ(double z) {
            this.z = z;
            setSize(z, z);
        }
    }
    
}

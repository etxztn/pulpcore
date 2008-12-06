// Drag the blocks around
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;
import pulpcore.image.Colors;

public class DragMe extends Scene2D {

    @Override
    public void load() {
        add(new FilledSprite(Colors.BLACK));
        add(new DraggableSprite("Block1.png", 100, 100));
        add(new DraggableSprite("Block2.png", 540, 100));
        add(new DraggableSprite("Block3.png", 540, 380));
        add(new DraggableSprite("Block4.png", 100, 380));
    }

    public static class DraggableSprite extends ImageSprite {

        private boolean dragging = false;

        public DraggableSprite(String image, int x, int y) {
            super(image, x, y);
            setAnchor(Sprite.CENTER);
            alpha.set(200); // So you can see which one is on top
        }

        @Override
        public void update(int elapsedTime) {
            super.update(elapsedTime);

            // Check if the mouse was pressed on this Sprite,
            // and this Sprite is the top-most Sprite under the mouse.
            if (super.isMousePressed() && super.isPick(Input.getMouseX(), Input.getMouseY())) {
                dragging = true;
                getParent().moveToTop(this);
            }
            // Check if the mouse was released anywhere in the Scene.
            if (Input.isMouseReleased()) {
                dragging = false;
            }
            if (dragging && Input.isMouseMoving()) {
                // Animate the move over 75ms
                moveTo(Input.getMouseX(), Input.getMouseY(), 75);
            }
        }
    }
}

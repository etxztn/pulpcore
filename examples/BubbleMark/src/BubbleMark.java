import pulpcore.animation.Fixed;
import pulpcore.image.CoreImage;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

/**
    PulpCore BubbleMark implementation. See http://www.bubblemark.com/
    
    There are a few more possible optimizations:
    * Always enable pixel snapping.
    * For large number of balls, use a Scene instead of a Scene2D
    * There might be some optimizations in the Ball class provided from BubbleMark
*/
public class BubbleMark extends Scene2D {
    
    int numBalls = 16;
    Ball[] balls;
    boolean running = true;
    Fixed frameRate = new Fixed();
    
    public void load() {
        // Add background
        FilledSprite background = new FilledSprite(0xffffff);
        background.setBorderSize(1);
        background.setBorderColor(0x000000);
        add(background);
        
        // Add balls. Optimization: Enable pixel-snapping if 64 balls or more
        CoreImage ballImage = CoreImage.load("ball.png");
        balls = new Ball[numBalls];
        for (int i = 0; i < balls.length; i++) {
            BallSprite ballSprite = new BallSprite(ballImage);
            ballSprite.pixelSnapping.set(balls.length >= 64);
            add(ballSprite);
            balls[i] = ballSprite.ball;
        }
        
        // Add fps display
        Label frameRateLabel = new Label("%.1f fps", 5, 5);
        frameRateLabel.setFormatArg(frameRate);
        add(frameRateLabel);
        
        // Optimization: Use dirty rectangles if there are 16 balls or fewer
        setDirtyRectanglesEnabled(balls.length <= 16);
    }
    
    public void update(int elapsedTime) {
        // Reload if ball count has changed
        if (numBalls != balls.length) {
            getMainLayer().removeAll();
            load();
        }
        
        if (!running) {
            Stage.setFrameRate(5);
        }
        else {
            Stage.setFrameRate(Stage.MAX_FPS);
        
            // Check collisions
            for (int i = 0; i < balls.length; i++) {
                for (int j = i+1; j < balls.length; j++) {
                    balls[i].doCollide(balls[j]);
                }
            }
        }
            
        // Update fps display
        double fps = Stage.getActualFrameRate();
        if (fps >= 0) {
            frameRate.set(fps);
        }
    }
    
    public void setNumBalls(int numBalls) {
        this.numBalls = numBalls;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    class BallSprite extends ImageSprite {
        
        Ball ball;
        
        public BallSprite(CoreImage image) {
            super(image, 0, 0);
            ball = new Ball();
            x.set(ball._x);
            y.set(ball._y);
        }
        
        public void update(int elpasedTime) {
            if (running) {
                ball.move();
                x.set(ball._x);
                y.set(ball._y);
            }
        }
    }
}

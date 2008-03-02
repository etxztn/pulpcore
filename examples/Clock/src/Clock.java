// Analog clock example 
import java.util.Calendar;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.Stage;
import static pulpcore.image.Colors.*;

public class Clock extends Scene2D {
    
    ImageSprite secondHand, minuteHand, hourHand;

    @Override
    public void load() {
        // Load clock hands
        int x = Stage.getWidth()/2;
        int y = Stage.getHeight()/2;
        secondHand = new ImageSprite("SecondHand.png", x, y);
        minuteHand = new ImageSprite("MinuteHand.png", x, y);
        hourHand = new ImageSprite("HourHand.png", x, y);
        
        // Add the sprites to the scene
        add(new FilledSprite(BLACK));
        add(new ImageSprite("face.png", 80, 0));
        add(hourHand);
        add(minuteHand);
        add(secondHand);
    }
    
    @Override
    public void update(int elapsedTime) {
        // Update clock hands with the current time
        Calendar time = Calendar.getInstance();
        double seconds = time.get(Calendar.SECOND) + time.get(Calendar.MILLISECOND) / 1000.0;
        double minutes = time.get(Calendar.MINUTE) + seconds / 60;
        double hours = time.get(Calendar.HOUR) + minutes / 60;
        secondHand.angle.set(seconds * 2*Math.PI / 60);
        minuteHand.angle.set(minutes * 2*Math.PI / 60);
        hourHand.angle.set(hours * 2*Math.PI / 12);
    }
}


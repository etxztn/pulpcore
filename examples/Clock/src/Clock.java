// Obligatory analog clock example 
import java.util.Calendar;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import static pulpcore.image.Colors.*;

public class Clock extends Scene2D {
    
    FilledSprite hourHand, minuteHand, secondHand;

    @Override
    public void load() {
        // Create all clock hands pointing at 12
        int x = Stage.getWidth()/2;
        int y = Stage.getHeight()/2;
        int size = Math.min(Stage.getWidth(), Stage.getHeight()); 
        secondHand = new FilledSprite(x, y, 1, size*.4, RED);
        secondHand.setAnchor(Sprite.SOUTH);
        minuteHand = new FilledSprite(x, y, 2, size*.35, BLACK);
        minuteHand.setAnchor(Sprite.SOUTH);
        hourHand = new FilledSprite(x, y, 6, size*.2, BLACK);
        hourHand.setAnchor(Sprite.SOUTH);
        
        // For fun, create clock hand shadows
        FilledSprite secondHandShadow = new FilledSprite(x+7, y+7, 1, size*.4, 
            rgba(128, 128, 128, 66));
        secondHandShadow.setAnchor(Sprite.SOUTH);
        secondHandShadow.angle.bindTo(secondHand.angle);
        FilledSprite minuteHandShadow = new FilledSprite(x+5, y+5, 2, size*.35, 
            rgba(128, 128, 128, 80));
        minuteHandShadow.setAnchor(Sprite.SOUTH);
        minuteHandShadow.angle.bindTo(minuteHand.angle);
        FilledSprite hourHandShadow = new FilledSprite(x+4, y+4, 6, size*.2, 
            rgba(128, 128, 128, 90));
        hourHandShadow.setAnchor(Sprite.SOUTH);
        hourHandShadow.angle.bindTo(hourHand.angle);
        
        // Add the background, shadows, and clock hands to the scene
        add(new FilledSprite(WHITE));
        add(hourHandShadow);
        add(minuteHandShadow);
        add(secondHandShadow);
        add(hourHand);
        add(minuteHand);
        add(secondHand);
        
        // Add clock labels
        for (int i = 0; i < 12; i++) {
            int hour = i == 0 ? 12 : i;
            double angle = i * 2*Math.PI / 12 - Math.PI/2;
            int labelX = x + (int)Math.round(size * .45 * Math.cos(angle));
            int labelY = y + (int)Math.round(size * .45 * Math.sin(angle));
            Label label = new Label(Integer.toString(hour), labelX, labelY);
            label.setAnchor(Sprite.CENTER);
            add(label);
        }
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


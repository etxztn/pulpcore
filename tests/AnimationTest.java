import org.junit.Test;
import pulpcore.animation.Easing;
import pulpcore.animation.event.TimelineEvent;
import pulpcore.animation.Int;
import pulpcore.animation.Timeline;
import static org.junit.Assert.*;

public class AnimationTest {
    
    @Test public void eventTriggersOnFastForward() {
        final boolean[] outcome = { false };
        Timeline timeline = new Timeline();
        timeline.add(new TimelineEvent(1000) {
            public void run() {
                outcome[0] = true;
            }
        });
        timeline.fastForward();
        assertTrue("Event does not trigger on fast-forward", outcome[0]);
    }
    
    @Test public void propertyUpdatesOnGracefullStop() {
        Int property = new Int(0);
        property.animate(0, 1234, 1000);
        property.stopAnimation(true);
        assertTrue("Property does not update on stopAnimation(true)", property.get() == 1234);
    }
    
    @Test public void propertyDoesNotUpdateOnStop() {
        Int property = new Int(0);
        property.animate(0, 1234, 1000);
        property.stopAnimation(false);
        assertTrue("Property incorrectly updates on stopAnimation(false)", property.get() == 0);
    }
    
    @Test public void propertyValueUpdatesOnSetBehavior() {
        Int property = new Int(0);
        property.animate(1234, 5678, 1000);
        assertTrue("Property does not update on setBehavior()", property.get() == 1234);
    }
    
    @Test public void propertyValueDoesNotUpdatesOnDelayedBehavior() {
        Int property = new Int(0);
        property.animate(1234, 5678, 1000, Easing.NONE, 500);
        assertTrue("Property incorrectly updates on delayed setBehavior()", property.get() == 0);
    }
    
}

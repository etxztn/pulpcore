import org.junit.Test;
import pulpcore.animation.Easing;
import pulpcore.animation.event.TimelineEvent;
import pulpcore.animation.Int;
import pulpcore.animation.Timeline;
import static org.junit.Assert.*;

public class AnimationTest {
    
    @Test public void eventTriggersOnFastForward() {
        final int[] executions = { 0 };
        Timeline timeline = new Timeline();
        timeline.add(new TimelineEvent(1000) {
            public void run() {
                executions[0]++;
            }
        });
        timeline.fastForward();
        assertEquals("Event does not trigger once on fast-forward", 1, executions[0]);
    }

    @Test public void eventTriggersAtEndOfLoopingTimeline() {
        final int[] executions = { 0 };
        Timeline timeline = new Timeline();
        timeline.add(new TimelineEvent(1000) {
            public void run() {
                executions[0]++;
            }
        });
        timeline.loopForever();
        timeline.update(950);
        timeline.update(100);
        assertEquals("Event does not trigger once at end of Timeline", 1, executions[0]);
        timeline.update(950);
        assertEquals("Event does not trigger once at end of Timeline", 2, executions[0]);
        timeline.update(2000);
        assertEquals("Event does not trigger once at end of Timeline", 4, executions[0]);
    }

    @Test public void eventTriggersInEveryLoopIteration() {
        final int[] executions = { 0 };
        Timeline timeline = new Timeline();
        timeline.add(new TimelineEvent(100) {
            public void run() {
                executions[0]++;
            }
        });
        timeline.loopForever();
        timeline.update(1000);
        assertEquals("Event does not trigger in every loop iteration.", 10, executions[0]);
        timeline.update(210);
        assertEquals("Event does not trigger in every loop iteration.", 12, executions[0]);
        timeline.update(125);
        assertEquals("Event does not trigger in every loop iteration.", 13, executions[0]);
        timeline.update(125);
        assertEquals("Event does not trigger in every loop iteration.", 14, executions[0]);
        timeline.update(125);
        assertEquals("Event does not trigger in every loop iteration.", 15, executions[0]);
        timeline.update(125);
        assertEquals("Event does not trigger in every loop iteration.", 17, executions[0]);
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

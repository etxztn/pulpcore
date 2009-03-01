import org.junit.Test;
import pulpcore.animation.Easing;
import pulpcore.animation.Fixed;
import pulpcore.animation.event.TimelineEvent;
import pulpcore.animation.Int;
import pulpcore.animation.Timeline;
import static org.junit.Assert.*;

public class AnimationTest {

    @Test public void bidirectionalBind() {
        Int x = new Int(5);
        Fixed y = new Fixed();
        y.bindWithInverse(x);
        assertEquals("Binding not initially set", x.get(), y.getAsInt());
        y.set(10);
        assertEquals("Bi-directional binding broken on inverse", y.getAsInt(), x.get());
        y.animateTo(20, 100);
        y.update(100);
        assertEquals("Bi-directional binding broken on animation", y.getAsInt(), x.get());
        x.animateTo(30, 100);
        x.update(100);
        assertEquals("Bi-directional binding broken on inverse animation", x.get(), y.getAsInt());
    }
    
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

    @Test public void subTimeline() {
        Int property = new Int(0);
        Timeline timeline = new Timeline();
        final int moves = 5;
        final int startTime = 50;
        final int moveDur = 100;
        final int d = 10;
        int t = startTime;
        for (int j = 0; j < moves; j++) {
            int x1 = (j+1) * d;
            int x2 = (j+2) * d;
            
            timeline.at(t).animate(property, x1, x2, moveDur);
            t += moveDur;
        }
        timeline.loopForever();
        timeline.update(25);
        assertEquals("Incorrect value.", 0, property.get());
        timeline.update(startTime-25);
        assertEquals("Incorrect value.", d, property.get());
        timeline.update(moveDur);
        assertEquals("Incorrect value.", d*2, property.get());
        timeline.update(moveDur*2);
        assertEquals("Incorrect value.", d*4, property.get());
        timeline.update(moveDur/2);
        assertEquals("Incorrect value.", d*4 + d/2, property.get());
        timeline.update(moveDur/2);
        assertEquals("Incorrect value.", d*5, property.get());
        timeline.update(moveDur-10);
        assertEquals("Incorrect value.", d*5 + d/2, property.get(), d/2);
        timeline.update(10);
        assertEquals("Incorrect value.", d*6, property.get());
        timeline.update(startTime);
        assertEquals("Incorrect value.", d, property.get());
        timeline.update(moveDur*5);
        assertEquals("Incorrect value.", d*6, property.get());
    }

    @Test public void backForthTimeline() {
        Int property = new Int(0);
        int dur = 100;
        int startValue = 0;
        int endValue = 50;
        Timeline timeline = new Timeline();
        timeline.animate(property, startValue, endValue, dur/2, null, 0);
        timeline.animate(property, endValue, startValue, dur/2, null, dur/2);
        timeline.loopForever();
        timeline.update(dur-1);
        timeline.update(1);
        assertEquals("Incorrect value.", startValue, property.get());

        // Same thing, only reversed order of adding animations to timeline
        property = new Int(0);
        timeline = new Timeline();
        timeline.animate(property, endValue, startValue, dur/2, null, dur/2);
        timeline.animate(property, startValue, endValue, dur/2, null, 0);
        timeline.loopForever();
        timeline.update(dur-1);
        timeline.update(1);
        assertEquals("Incorrect value.", startValue, property.get());
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

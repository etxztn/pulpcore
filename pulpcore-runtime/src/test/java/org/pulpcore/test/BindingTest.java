package org.pulpcore.test;

import org.junit.Test;
import org.pulpcore.view.animation.TweenFloat;
import org.pulpcore.view.animation.TweenInt;
import org.pulpcore.view.property.FloatProperty;
import org.pulpcore.view.property.IntProperty;
import static org.junit.Assert.*;

/*
 TODO:
 - Test unbind(), getBinding()
 */
public class BindingTest {

   @Test public void bidirectionalBind() {
        IntProperty x = new IntProperty(5);
        FloatProperty y = new FloatProperty();
        y.bindBidirectionallyTo(x);
        assertEquals("Binding not initially set", x.get(), (int)y.get());
        y.set(10);
        assertEquals("Bi-directional binding broken on inverse", x.get(), (int)y.get());

        TweenFloat tweenY = new TweenFloat(0.1f, y, 0, 20);
        tweenY.tick(0.1f);
        assertEquals("Bi-directional binding broken on animation", x.get(), (int)y.get());

        TweenInt tweenX = new TweenInt(0.1f, x, 0, 30);
        tweenX.tick(0.1f);
        assertEquals("Bi-directional binding broken on inverse animation", x.get(), (int)y.get());
    }

    @Test public void bidirectionalBindMultipleClones() {
        IntProperty x = new IntProperty(5);
        FloatProperty y = new FloatProperty();
        IntProperty z = new IntProperty();
        y.bindBidirectionallyTo(x);
        z.bindBidirectionallyTo(x);
        assertEquals("Binding Y not initially set", x.get(), (int)y.get());
        assertEquals("Binding Z not initially set", x.get(), z.get());
        y.set(10);
        assertEquals("Binding X not set", (int)y.get(), x.get());
        assertEquals("Binding Z not set", (int)y.get(), z.get());
        z.set(20);
        assertEquals("Binding X broken", z.get(), x.get());
        assertEquals("Binding Y broken", z.get(), (int)y.get());
        for (int i = 0; i < 10000; i++) {
            double r = Math.random();
            if (r < 0.3333333) {
                x.set((int)(Math.random()* 100));
            }
            else if (r < 0.6666667) {
                y.set((int)(Math.random()* 100));
            }
            else {
                z.set((int)(Math.random()* 100));
            }
        }
        assertEquals("Random order - Binding X broken", z.get(), x.get());
        assertEquals("Random order - Binding Y broken", z.get(), (int)y.get());
    }

    @Test public void bidirectionalBindWhatIf() {
        IntProperty x = new IntProperty(5);
        FloatProperty y = new FloatProperty();
        x.bindBidirectionallyTo(y);
        y.bindBidirectionallyTo(x);
        x.set(60);
        assertEquals("Binding not initially set", x.get(), (int)y.get());

        IntProperty x2 = new IntProperty(5);
        FloatProperty y2 = new FloatProperty();
        x2.bindBidirectionallyTo(y2);
        x2.bindBidirectionallyTo(y2); // do it twice
        x2.set(40);
        assertEquals("Binding y2 not initially set", x2.get(), (int)y2.get());
        y2.set(50);
        assertEquals("Binding x2 not initially set", x2.get(), (int)y2.get());
    }

    @Test public void bidirectionalCircular() {
        IntProperty x = new IntProperty(5);
        FloatProperty y = new FloatProperty();
        IntProperty z = new IntProperty();
        y.bindBidirectionallyTo(x);
        z.bindBidirectionallyTo(y);
        x.bindBidirectionallyTo(z);
        x.set(40);
        assertEquals("Random order - Binding X broken", z.get(), x.get());
        assertEquals("Random order - Binding Y broken", z.get(), (int)y.get());
    }
}

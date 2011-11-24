package org.pulpcore.view.animation;

public class Loop extends IntervalAction {

    private final IntervalAction action;
    private final int times;
    private int count;

    public Loop(IntervalAction action, int times) {
        super(action.getDuration() * times);
        this.action = action;
        this.times = times;
    }

    @Override
    public void start() {
        super.start();
        action.start();
        count = 0;
    }

    @Override
    public void stop() {
        super.stop();
        // Finish remaining
        while (count < times) {
            action.update(1);
            action.stop();
            count++;
            if (count < times) {
                action.start();
            }
        }
    }

    @Override
    public void update(float p) {
        if (getDuration() > 0 && count < times) {
            float currCount = p * times;
            while ((int)currCount > count) {
                action.update(1);
                action.stop();
                count++;
                if (count < times) {
                    action.start();
                }
                else {
                    return;
                }
            }
            action.update(currCount % 1);
        }
    }

}

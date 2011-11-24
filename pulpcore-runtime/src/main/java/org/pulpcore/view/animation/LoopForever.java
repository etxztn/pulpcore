package org.pulpcore.view.animation;

public class LoopForever extends Action {

    private IntervalAction action;
    private float elapsedTime;

    public LoopForever(IntervalAction action) {
        this.action = action;
    }

    @Override
    public void start() {
        super.start();
        action.start();
        elapsedTime = 0;
    }

    @Override
    public void stop() {
        super.stop();
        action.stop();
    }

    @Override
    public void tick(float dt) {
        float dur = action.getDuration();
        if (dur <= 0) {
            action.update(1);
            action.stop();
            action.start();
        }
        else {
            elapsedTime += dt;
            while (elapsedTime >= dur) {
                action.update(1);
                action.stop();
                action.start();
                elapsedTime -= dur;
            }
            action.update(elapsedTime / dur);
        }
    }

}

/*
    Copyright (c) 2011, Interactive Pulp, LLC
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package org.pulpcore.view.animation;

public class Sequence extends IntervalAction {

    private final IntervalAction[] actions;
    private int lastIndex;

    public Sequence(IntervalAction... actions) {
        super(getTotalDuration(actions));
        this.actions = actions.clone();
    }

    private static float getTotalDuration(IntervalAction... actions) {
        float dur = 0;
        for (IntervalAction action : actions) {
            dur += action.getDuration();
        }
        return dur;
    }
    
    @Override
    public void start() {
        super.start();
        lastIndex = -1;
    }

    @Override
    public void stop() {
        super.stop();
        for (IntervalAction action : actions) {
            if (!action.isFinished()) {
                action.stop();
            }
        }
    }

    @Override
    public void update(float p) {
        if (getDuration() <= 0) {
            for (IntervalAction action : actions) {
                action.start();
                action.update(1);
            }
            stop();
        }
        else if (actions.length > 0) {
            int foundIndex = actions.length;
            float actionStartP = 0;
            for (int i = 0; i < actions.length; i++) {
                IntervalAction action = actions[i];
                if (action.getDuration() > 0) {
                    float actionEndP = actionStartP + action.getDuration() / getDuration();
                    if (actionEndP > p) {
                        foundIndex = i;
                        break;
                    }
                    actionStartP = actionEndP;
                }
            }

            if (lastIndex != foundIndex) {
                if (lastIndex >= 0 && lastIndex < actions.length) {
                    actions[lastIndex].update(1);
                    actions[lastIndex].stop();
                }
                for (int i = lastIndex + 1; i < foundIndex; i++) {
                    actions[i].start();
                    actions[i].update(1);
                    actions[i].stop();
                }

                if (foundIndex < actions.length) {
                    actions[foundIndex].start();
                }
            }
            if (foundIndex < actions.length) {
                // NOTE: foundIndex will never be an action with durtion <= 0
                float actionP = (p - actionStartP) * getDuration() / actions[foundIndex].getDuration();
                actions[foundIndex].update(actionP);
            }
            lastIndex = foundIndex;
        }
    }
}

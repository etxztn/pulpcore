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

import java.util.List;

public class Simultaneous extends IntervalAction {

    private final IntervalAction[] actions;

    public Simultaneous(List<IntervalAction> actions) {
        this(false, actions.toArray(new IntervalAction[actions.size()]));
    }

    public Simultaneous(IntervalAction... actions) {
        this(true, actions);
    }

    private Simultaneous(boolean cloneArray, IntervalAction[] actions) {
        super(getMaxDuration(actions));
        this.actions = cloneArray ? actions.clone() : actions;
    }

    private static float getMaxDuration(IntervalAction... actions) {
        float dur = 0;
        for (IntervalAction action : actions) {
            dur = Math.max(dur, action.getDuration());
        }
        return dur;
    }

    @Override
    public void start() {
        super.start();
        for (IntervalAction action : actions) {
            action.start();
        }
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
        for (IntervalAction action : actions) {
            if (!action.isFinished()) {
                if (action.getDuration() <= 0) {
                    action.update(1);
                    action.stop();
                }
                else if (action.getDuration() == getDuration()) {
                    action.update(p);
                }
                else {
                    float p2 = p * getDuration() / action.getDuration();
                    if (p2 >= 1) {
                        action.update(1);
                        action.stop();
                    }
                    else {
                        action.update(p2);
                    }
                }
            }
        }
    }
}

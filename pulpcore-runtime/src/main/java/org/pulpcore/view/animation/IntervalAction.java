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

public abstract class IntervalAction extends Action {

    private final float dur;
    // This won't be used in all situations. 
    // For example, Easing calls it's child Action's update() method directly.
    private float elapsedTime;

    public IntervalAction(float dur) {
        this.dur = dur;
    }

    public float getDuration() {
        return dur;
    }

    @Override
    public void start() {
        super.start();
        elapsedTime = 0;
    }

    /**
     Updates this action.
     @param dt The amount of time, in seconds, since the last update.
     */
    @Override
    public final void tick(float dt) {
        if (!isFinished()) {
            elapsedTime += dt;
            if (dur <= 0) {
                update(1);
                stop();
            }
            else {
                float p = elapsedTime / dur;
                if (p <= 0) {
                    update(0);
                }
                else if (p >= 1) {
                    update(1);
                    stop();
                }
                else {
                    update(p);
                }
            }
        }
    }

    /**
     Updates this action with the specified position.
     @param p Usually from 0 to 1, but values can be outside this range if an effect is applied.
     */
    public abstract void update(float p);
}

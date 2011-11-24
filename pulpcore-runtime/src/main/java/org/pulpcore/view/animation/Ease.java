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

public class Ease extends IntervalAction {

    public interface Function {
        public float ease(float p);
    }

    private static final int TYPE_IN = 0;
    private static final int TYPE_OUT = 1;
    private static final int TYPE_IN_OUT = 2;

    private static final int FUNCTION_LINEAR = 0;
    private static final int FUNCTION_QUADRADIC = 1;
    private static final int FUNCTION_CUBIC = 2;
    private static final int FUNCTION_QUARTIC = 3;
    private static final int FUNCTION_QUINTIC = 4;
    private static final int FUNCTION_BACK = 5;
    private static final int FUNCTION_ELASTIC = 6;

    private static class SimpleFunction implements Function {

        private final int type;
        private final int function;

        private SimpleFunction(int type, int function) {
            this.type = type;
            this.function = function;
        }

        @Override
        public float ease(float p) {
            float easedP;

            switch (type) {

                default:
                    easedP = p;
                    break;

                case TYPE_IN:
                    easedP = functionEase(p);
                    break;

                case TYPE_OUT:
                    easedP = 1-functionEase(1-p);
                    break;

                case TYPE_IN_OUT:
                    if (p < 0.5) {
                        easedP = functionEase(2*p)/2;
                    }
                    else {
                        easedP = 1-functionEase(2 - 2*p)/2;
                    }
                    break;
            }

            return easedP;
        }

        protected float functionEase(float p) {

            float p2;
            float p3;

            switch (function) {

                default: case FUNCTION_LINEAR:
                    return p;

                case FUNCTION_QUADRADIC:
                    return p * p;

                case FUNCTION_CUBIC:
                    return p * p * p;

                case FUNCTION_QUARTIC:
                    p2 = p * p;
                    return p2 * p2;

                case FUNCTION_QUINTIC:
                    p2 = p * p;
                    return p2 * p2 * p;

                case FUNCTION_BACK:
                    p2 = p * p;
                    p3 = p2 * p;
                    return p3 + p2 - p;

                case FUNCTION_ELASTIC:
                    p2 = p * p;
                    p3 = p2 * p;

                    float scale = p2 * (2*p3 + p2 - 4*p + 2);
                    float wave = (float)-Math.sin(p * 3.5 * Math.PI);

                    return scale * wave;
            }
        }
    }

    public static class RegularIn extends Ease {

        public RegularIn(IntervalAction action) {
            this(action, 1);
        }

        public RegularIn(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN, FUNCTION_QUADRADIC), strength);
        }
    }

    public static class RegularOut extends Ease {

        public RegularOut(IntervalAction action) {
            this(action, 1);
        }

        public RegularOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_OUT, FUNCTION_QUADRADIC), strength);
        }
    }

    public static class RegularInOut extends Ease {

        public RegularInOut(IntervalAction action) {
            this(action, 1);
        }

        public RegularInOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN_OUT, FUNCTION_QUADRADIC), strength);
        }
    }

    public static class BackIn extends Ease {

        public BackIn(IntervalAction action) {
            this(action, 1);
        }

        public BackIn(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN, FUNCTION_BACK), strength);
        }
    }

    public static class BackOut extends Ease {

        public BackOut(IntervalAction action) {
            this(action, 1);
        }

        public BackOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_OUT, FUNCTION_BACK), strength);
        }
    }

    public static class BackInOut extends Ease {

        public BackInOut(IntervalAction action) {
            this(action, 1);
        }

        public BackInOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN_OUT, FUNCTION_BACK), strength);
        }
    }

     public static class ElasticIn extends Ease {

        public ElasticIn(IntervalAction action) {
            this(action, 1);
        }

        public ElasticIn(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN, FUNCTION_ELASTIC), strength);
        }
    }

    public static class ElasticOut extends Ease {

        public ElasticOut(IntervalAction action) {
            this(action, 1);
        }

        public ElasticOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_OUT, FUNCTION_ELASTIC), strength);
        }
    }

    public static class ElasticInOut extends Ease {

        public ElasticInOut(IntervalAction action) {
            this(action, 1);
        }

        public ElasticInOut(IntervalAction action, float strength) {
            super(action, new SimpleFunction(TYPE_IN_OUT, FUNCTION_ELASTIC), strength);
        }
    }

    private final IntervalAction action;
    private final Function easeFunction;
    private final float strength;

    public Ease(IntervalAction action, Ease.Function easeFunction) {
        this(action, easeFunction, 1);
    }

    public Ease(IntervalAction action, Ease.Function easeFunction, float strength) {
        super(action.getDuration());
        this.action = action;
        this.easeFunction = easeFunction;
        this.strength = strength;
    }
    
    @Override
    public void start() {
        super.start();
        action.start();
    }

    @Override
    public void stop() {
        super.stop();
        action.stop();
    }

    @Override
    public void update(float p) {
        if (p <= 0) {
            action.update(0);
        }
        else if (p >= 1) {
            action.update(1);
        }
        else {
            float easedP = easeFunction.ease(p);
            if (strength != 1) {
                easedP = strength * easedP + (1 - strength) * p;
            }
            action.update(easedP);
        }
    }

}
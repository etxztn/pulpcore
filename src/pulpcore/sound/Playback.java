/*
    Copyright (c) 2008, Interactive Pulp, LLC
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

package pulpcore.sound;

import pulpcore.animation.Fixed;

/**
    The Playback class allows a Sound be modified while it is playing.
*/
public abstract class Playback {
    
    /**
        The level, from 0 to 1. The level animation is synchronized with the Sound's playback time,
        and can't be animated with a Timeline.
    */
    public final Fixed level;
    
    /**
        The pan, from -1 to 1. The pan animation is synchronized with the Sound's playback time,
        and can't be animated with a Timeline.
    */
    public final Fixed pan;
    
    public Playback(Fixed level, Fixed pan) {
        this.level = level;
        this.pan = pan;
    }
    
    public abstract long getMicrosecondPosition();
    
    /**
        Pauses this playback or contunues playback after pausing. A paused sound may continue to
        send data (in the form of inaudible sound) to the sound engine.
    */
    public abstract void setPaused(boolean paused);
    
    /**
        Checks if this playback is currently paused and playback can continue.
    */
    public abstract boolean isPaused();
    
    /**
        Stops this playback as soon as possible. A stopped playback cannot be restarted.
    */
    public abstract void stop();
    
    /**
        Returns true if the playback is finished.
    */
    public abstract boolean isFinished();
    
    // TODO: FFT stuff
    
}

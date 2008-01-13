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
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.AppContext;
import pulpcore.platform.SoundEngine;
import pulpcore.platform.SoundStream;

/**
    The Sound class is a base class for sampled sound.
    @see SoundClip
    @see SoundSequence
*/
public abstract class Sound {
    
    private final int sampleSize = 2;
    private final int sampleRate;
    
    /**
        Creates a new Sound with the specified sample rate.
        @param sampleRate the sample rate (samples per second, per channel). 
    */
    public Sound(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    /**
        Returns the sample rate - the number of samples played per second, per channel. 
    */
    public final int getSampleRate() {
        return sampleRate;
    }
    
    /**
        Returns the sample size - the number of bytes in each sample.
        This method always returns 2.
    */
    public final int getSampleSize() {
        return sampleSize;
    }
  
    /**
        Gets the duration of this clip in milliseconds.
        @return the duration of this clip in milliseconds.
    */
    public final long getDuration() {
        return 1000L * getNumFrames() / sampleRate;
    }
    
    /**
        Gets the length of this sound, expressed in the number of frames. For mono sounds,
        a frame is one sample, for stereo sounds, a frame consists of two samples - one for
        the left channel, and one for the right channel.
    */
    public abstract int getNumFrames();
    
    /**
        Copies a sequence of samples from this Sound to a byte array as 
        signed, little endian, 16-bit PCM format.
        @param dest the destination buffer.
        @param destOffset the offset, in bytes, in the destination buffer.
        @param destChannels The number of channels of the destination (1 or 2).
        @param srcFrame the frame position to start copying from.
        @param numFrames the number of frames to copy.
    */
    public abstract void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames);

    //
    // Play methods
    //
    
    /**
        Plays this sound clip. The Sound is played at full volume with no panning.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects)
    */
    public final Playback play() {
        return play(new Fixed(1.0), new Fixed(0), false);
    }
        
    /**
        Plays this sound clip with the specified colume level (0.0 to 1.0). 
        The level may have a property animation attached.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects)
    */
    public final Playback play(Fixed level) {
        return play(level, new Fixed(0), false);
    }
    
    /**
        Plays this sound clip with the specified level (0.0 to 1.0) and pan (-1.0 to 1.0).
        The level and pan may have a property animation attached. 
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects)
    */
    public final Playback play(Fixed level, Fixed pan) {
        return play(level, pan, false);
    }
    
    /**
        Plays this sound clip with the specified level (0.0 to 1.0) and pan (-1.0 to 1.0),
        optionally looping. The level and pan may have a property animation attached.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects)
    */
    public Playback play(Fixed level, Fixed pan, boolean loop) {

        AppContext context = CoreSystem.getThisAppContext();
        SoundEngine soundEngine = CoreSystem.getPlatform().getSoundEngine();
        Playback playback = null;
        
        if (context != null && soundEngine != null && this.getNumFrames() > 0) {
            try {
                playback = soundEngine.play(context, this, level, pan, loop);
            }
            catch (Exception ex) {
                if (Build.DEBUG) CoreSystem.print("Sound play", ex);
            }
        }
        
        if (playback == null) {
            SoundStream stream = new SoundStream(null, this, level, pan, 0, 0, this.getNumFrames());
            playback = stream.getPlayback();
            playback.stop();
        }
        return playback;
    }
}

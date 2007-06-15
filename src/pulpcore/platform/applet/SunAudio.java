/*
    Copyright (c) 2007, Interactive Pulp, LLC
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

package pulpcore.platform.applet;

import java.io.InputStream;
import java.util.Vector;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.animation.Fixed;
import pulpcore.platform.AppContext;
import pulpcore.platform.SoundStream;
import pulpcore.platform.SoundEngine;
import pulpcore.sound.SoundClip;
import sun.audio.AudioPlayer;

/**
    The SunAudio class is a {@link pulpcore.platform.SoundEngine } implementation that 
    uses the sun.audio package, available on Java 1.1 platforms, to play sound.
*/
public class SunAudio extends InputStream implements SoundEngine {
    
    /*
        The MS Java implementation up-converts 8-bit u-law to 16-bit linear to mix multiple clips, 
        then down-converts the result to 8-bit u-law. We'll save the extra up-convert step and keep 
        samples in 16-bit linear format. This makes mixing more efficient, and sound filters can 
        work with the 16-bit linear samples.
    */
    
    private static final byte ULAW_SILENCE = -1;//linear2ulaw(0);
    
    
    // For u-law conversion
    private static final byte[] LOG2_TABLE = {
        0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
    };
    
    
    private Vector soundStreams = new Vector();
    private int[] linearBuffer = new int[4000];
    private byte[] workBuffer = new byte[linearBuffer.length * 2];
    private boolean closed = true;
    
    
    SunAudio() {
        byte[] silence = new byte[4000];
        for (int i = 0; i < silence.length; i++) {
            silence[i] = ULAW_SILENCE;
        }
        SoundStream.setSilentBuffer(silence);
    }
    
    
    public synchronized void play(AppContext context, SoundClip clip, Fixed level, boolean loop) {
        
        if (closed) {
            closed = false;
            try {
                AudioPlayer.player.stop(this);
                AudioPlayer.player.start(this);
            }
            catch (Throwable t) {
                closed = true;
                if (Build.DEBUG) CoreSystem.print("Couldn't start AudioPlayer", t);
                return;
            }
        }
        
        soundStreams.addElement(new SoundStream(context, clip, level, loop));
    }
    
    
    public int getNumSoundsPlaying() {
        return soundStreams.size();
    }
    
    
    public int getMaxSimultaneousSounds() {
        return 32;
    }
    
    
    public void poll() {
        // Do nothing - AudioPlayer runs in it's own thread
    }
    
    
    public synchronized void destroy() {
        try {
            AudioPlayer.player.stop(this);
        }
        catch (Throwable t) {
            // Ignore
        }
        close();
    }
        
    
    private synchronized void removeFinishedSounds() {
        int index = 0;
        while (index < soundStreams.size()) {
            SoundStream stream = (SoundStream)soundStreams.elementAt(index);
            if (stream.isFinished()) {
                soundStreams.removeElementAt(index);
            }
            else {
                index++;
            }
        }
    }
    
    
    //
    // InputStream implementation
    // 

    
    public int available() {
        return linearBuffer.length;
    }
    
    
    public synchronized void close() {
        soundStreams = new Vector();
        closed = true;
    }
    
    
    public synchronized int read() {
        // Not supported - just return silence
        if (closed) {
            return -1;
        }
        else {
            return ULAW_SILENCE;
        }
    }
    
    
    public synchronized int read(byte[] buffer, int offset, int bytesToRead) {
        
        if (closed) {
            return -1;
        }
        
        if (soundStreams.size() == 0) {
            while (offset < bytesToRead) {
                buffer[offset++] = ULAW_SILENCE;
            }
            return bytesToRead;
        }
        
        bytesToRead = Math.min(bytesToRead, linearBuffer.length);
        int numSamples = bytesToRead;
        
        if (soundStreams.size() == 1) {
            SoundStream stream = (SoundStream)soundStreams.elementAt(0);
            stream.render(workBuffer, 0, numSamples*2);
            
            // Copy single stream to the output buffer
            for (int i = 0; i < numSamples; i++) {
                int sample = (workBuffer[i<<1] << 8) | (workBuffer[(i<<1) + 1] & 0xff); 
                buffer[offset++] = linear2ulaw(sample);
            }
        }
        else if (soundStreams.size() > 1) {
            
            // First stream
            SoundStream stream = (SoundStream)soundStreams.elementAt(0);
            stream.render(workBuffer, 0, numSamples*2);
            for (int i = 0; i < numSamples; i++) {
                linearBuffer[i] = (workBuffer[i<<1] << 8) | (workBuffer[(i<<1) + 1] & 0xff);
            }
            
            // Mix additional streams
            for (int i = 1; i < soundStreams.size(); i++) {
                stream = (SoundStream)soundStreams.elementAt(i);
                stream.render(workBuffer, 0, numSamples*2);
                for (int j = 0; j < numSamples; j++) {
                    linearBuffer[j] += 
                        (workBuffer[j<<1] << 8) | (workBuffer[(j<<1) + 1] & 0xff); 
                }
            }
            
            // Copy mix to the output buffer
            for (int i = 0; i < numSamples; i++) {
                // Clip
                int sample = linearBuffer[i];
                if (sample > 32767) {
                    sample = 32767;
                }
                else if (sample < -32768) {
                    sample = -32768;
                }
                
                buffer[offset++] = linear2ulaw(sample);
            }
        }
        
        removeFinishedSounds();
        
        return bytesToRead;
    }
    
    
    public synchronized long skip(long n) {
        for (int i = 0; i < soundStreams.size(); i++) {
            SoundStream stream = (SoundStream)soundStreams.elementAt(i);
            stream.skip((int)n);
        }
        
        removeFinishedSounds();
        return n;
    }
    
        
    /**
        Converts a signed 16-bit linear sample to an 8-bit ulaw sample.
        Results will be incorrect if sample < -32768 or sample > 32767.
        From http://www.speech.cs.cmu.edu/comp.speech/Section2/Q2.7.html
    */
    protected static byte linear2ulaw(int sample) {
        
        // Convert the sample to sign-magnitude.
        int sign = (sample >> 8) & 0x80;
        if (sign != 0) {
            sample = -sample;
        }
        
        // Add the bias, then clip the sample
        sample += 132;
        if (sample > 32767) sample = 32767;
        
        // Convert from 16 bit linear to ulaw.
        int exponent = LOG2_TABLE[sample >> 7];  // 3 bit result
        int mantissa = (sample >> (exponent + 3)) & 0x0f; // 4 bit result
        return (byte)( ~(sign | (exponent << 4) | mantissa));
    }    
}


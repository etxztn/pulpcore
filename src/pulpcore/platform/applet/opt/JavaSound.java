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

package pulpcore.platform.applet.opt;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import pulpcore.animation.Fixed;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;
import pulpcore.platform.AppContext;
import pulpcore.platform.SoundStream;
import pulpcore.platform.SoundEngine;
import pulpcore.sound.SoundClip;

/**
    The JavaSound class is a {@link pulpcore.platform.SoundEngine } implementation that 
    uses the Java Sound API to play sound.
*/
public class JavaSound implements SoundEngine {
        
    private static final int MAX_SIMULTANEOUS_SOUNDS = 32;
    
    private static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000, true);
    
    private static byte[] workBuffer = new byte[8000];
    private DataLinePlayer[] players = new DataLinePlayer[0];
    private int bufferSize;
    
    
    /**
        Gets the maximum number of simultaneous sounds with the
        specified AudioFormat that the default mixer can play.
    */
    private static int calcMaxSimultaneousSounds() {
        
        int maxLines = 0;
        
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT);
            Mixer mixer = AudioSystem.getMixer(null);
            maxLines =  mixer.getMaxLines(lineInfo);
        }
        catch (Exception ex) {
            if (Build.DEBUG) CoreSystem.print("calcMaxSimultaneousSounds()", ex);
        }
            
        if (maxLines == AudioSystem.NOT_SPECIFIED || maxLines > MAX_SIMULTANEOUS_SOUNDS) {
            return MAX_SIMULTANEOUS_SOUNDS;
        }
        else {
            return maxLines;
        }
    }    
    
    
    public JavaSound() {
        int maxSounds = calcMaxSimultaneousSounds();
        
        if (maxSounds != 0) {
            players = new DataLinePlayer[maxSounds];

            for (int i = 0; i < players.length; i++) {
                players[i] = new DataLinePlayer();
                int bSize = players[i].open();
                if (bSize != -1) {
                    bufferSize = bSize;
                }
            }
        }
        else {
            players = new DataLinePlayer[0];
        }
        
        byte[] silence = new byte[bufferSize];
        SoundStream.setSilentBuffer(silence);
        
        // Play a buffer's worth of silence - Helps remove popping
        play(null, new SoundClip(silence), new Fixed(1.0), false);
    }
    
    
    public synchronized void destroy() {
        for (int i = 0; i < players.length; i++) {
            players[i].close();
        }
    }
    
    
    public synchronized void poll() {
        for (int i = 0; i < players.length; i++) {
            players[i].poll();
        }
    }
    
    
    public synchronized void play(AppContext context, SoundClip sound, Fixed level,
        boolean loop) 
    {
        for (int i = 0; i < players.length; i++) {
            if (!players[i].isPlaying() && players[i].isOpen()) {
                players[i].play(context, sound, level, loop);
                return;
            }
        }
    }
    
        
    public synchronized int getNumSoundsPlaying() {
        int count = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].isPlaying()) {
                count++;
            }
        }
        return count;
    }
    
    
    public int getMaxSimultaneousSounds() {
        return players.length;
    }
    
    
    static class DataLinePlayer {
        
        private SoundStream stream;
        private SourceDataLine line;
        private boolean lastMute;

        
        public synchronized boolean isOpen() {
            return line != null && line.isOpen();
        }
        
        
        public synchronized int open() {
            if (isOpen()) {
                return line.getBufferSize();
            }
            
            try {
                // Attempt to get a 500ms buffer
                int byteRate = Math.round(PLAYBACK_FORMAT.getFrameRate() * 
                    PLAYBACK_FORMAT.getFrameSize());
                int bufferSize = byteRate / 2;                
                DataLine.Info lineInfo =
                    new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT, bufferSize);
                line = (SourceDataLine)AudioSystem.getLine(lineInfo);
                line.open(PLAYBACK_FORMAT, bufferSize);
                return line.getBufferSize();
            }
            catch (Exception ex) {
                line = null;
            }
            
            return -1;
        }
        
        
        public synchronized void close() {
            
            stream = null;
            
            if (line == null) {
                return;
            }
            
            try {
                line.close();
            }
            catch (Exception ex) { 
                if (Build.DEBUG) CoreSystem.print("DataLinePlayer.close()", ex);
            }
            line = null;
        }


        /**
            Forces playback of the specified buffer, even if isPlaying() is true.
        */
        public synchronized void play(AppContext context, SoundClip clip, Fixed level, 
            boolean loop) 
        {
            stream = new SoundStream(context, clip, level, loop, 160, line.getBufferSize());
            lastMute = stream.isMute();
        }
        

        public synchronized boolean isPlaying() {
            return line != null && stream != null;
        }
        
       
        public synchronized void poll() {
            if (line == null || stream == null) {
                return;
            }
            
            try {
                boolean mute = stream.isMute();
                if (!line.isRunning()) {
                    line.start();
                }
                if (mute != lastMute) {
                    // This causes pops on Mac OS X - instead, SoundStream does the muting
                    /*
                    try {
                        ((BooleanControl)line.getControl(BooleanControl.Type.MUTE)).setValue(mute);
                    }
                    catch (Exception ex) {
                        // Ignore
                    }
                    */
                    lastMute = mute;
                }
                
                int available = line.available();
                if (stream.isFinished()) {
                    if (available == line.getBufferSize()) {
                        // Close the line - ready for another Clip
                        close();
                        open();
                    }
                }
                else {
                    while (available > 0) {
                        int numBytes = Math.min(available, workBuffer.length);
                        stream.render(workBuffer, 0, numBytes);
                        line.write(workBuffer, 0, numBytes);
                        available -= numBytes;
                    }
                }
            }
            catch (Exception ex) {
                if (Build.DEBUG) CoreSystem.print("Error playing sound", ex);
                close();
                open();
            }
        }
    }
}

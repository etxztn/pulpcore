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
import pulpcore.platform.SoundEngine;
import pulpcore.platform.SoundStream;
import pulpcore.sound.Sound;
import pulpcore.sound.SoundClip;
import pulpcore.sound.SoundSequence;

/**
    The JavaSound class is a {@link pulpcore.platform.SoundEngine } implementation that 
    uses the Java Sound API to play sound.
*/
public class JavaSound implements SoundEngine {
        
    private static final int MAX_SIMULTANEOUS_SOUNDS = 32;
    
    private static final int[] SAMPLE_RATES = { 8000, 11025, 22050, 44100 };
    private static final int CHANNELS = 2;
    private static final int FRAME_SIZE = 2 * CHANNELS;
    
    // The buffer size (Must be divisible by FRAME_SIZE).
    // 16K is the minimum needed to prevent popping in Mac OS X?
    private static final int BUFFER_SIZE = 24*1024;
    
    private static byte[] workBuffer = new byte[BUFFER_SIZE/4];
    
    
    private static AudioFormat getFormat(int sampleRate) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
            sampleRate, 16, CHANNELS, FRAME_SIZE, sampleRate, true);
    }
    
    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }
    
    private DataLinePlayer[] players = new DataLinePlayer[0];
    private int[] maxSoundsForSampleRate = new int[SAMPLE_RATES.length];
    private int[] sampleRates;
    
    
    public JavaSound() {
        
        // Calculate the max number of simultaneous sounds for each sample rate
        int numSampleRates = 0;
        int maxSounds = 0;
        for (int i = 0; i < SAMPLE_RATES.length; i++) {
            maxSoundsForSampleRate[i] = getMaxSimultaneousSounds(SAMPLE_RATES[i]);
            if (maxSoundsForSampleRate[i] > 0) {
                numSampleRates++;
                maxSounds += maxSoundsForSampleRate[i];
            }
        }
        
        // Create the list of supported sample rates
        sampleRates = new int[numSampleRates];
        int index = 0;
        for (int i = 0; i < SAMPLE_RATES.length && index < numSampleRates; i++) {
            if (maxSoundsForSampleRate[i] > 0) {
                sampleRates[index++] = SAMPLE_RATES[i];
            }
        }
        
        // Create the players
        maxSounds = Math.min(maxSounds, MAX_SIMULTANEOUS_SOUNDS);
        if (maxSounds > 0) {
            players = new DataLinePlayer[maxSounds];
            for (int i = 0; i < players.length; i++) {
                players[i] = new DataLinePlayer(sampleRates, i % sampleRates.length);
            }
        }
        else {
            players = new DataLinePlayer[0];
        }
        
        // Play a buffer's worth of silence to helps remove popping
        if (players.length > 0) {
            SoundClip noSound = new SoundClip(new byte[0], players[0].getSampleRate(), true);
            play(null, noSound, new Fixed(1), new Fixed(0), false);
        }
    }
    
    
    /**
        Gets the maximum number of simultaneous sounds with the
        specified AudioFormat that the default mixer can play.
    */
    private int getMaxSimultaneousSounds(int sampleRate) {
        
        AudioFormat format = getFormat(sampleRate);
        
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
            Mixer mixer = AudioSystem.getMixer(null);
            int maxLines = mixer.getMaxLines(lineInfo);
            if (maxLines == AudioSystem.NOT_SPECIFIED || maxLines > MAX_SIMULTANEOUS_SOUNDS) {
                return MAX_SIMULTANEOUS_SOUNDS;
            }
            else {
                return maxLines;
            }
        }
        catch (Exception ex) {
            if (Build.DEBUG) CoreSystem.print("getMaxSimultaneousSounds()", ex);
            return 0;
        }
    }    
    
    
    public int[] getSupportedSampleRates() {
        return sampleRates;
    }
    
    
    public synchronized void destroy() {
        for (int i = 0; i < players.length; i++) {
            players[i].close();
        }
    }
    
    
    public synchronized void poll() {
        // Poll the players
        for (int i = 0; i < players.length; i++) {
            players[i].poll();
        }
        
        // Determine if all sample rates are available and ready
        int[] availableSampleRates = new int[sampleRates.length];
        int numStoppedLines = 0;
        for (int i = 0; i < players.length; i++) {
            if (!players[i].isPlaying()) {
                numStoppedLines++;
                
                int index = indexOf(sampleRates, players[i].getSampleRate());
                if (index != -1) {
                    availableSampleRates[index]++;
                }
            }
        }
        
        // Make sure at least 1 line of each sample rate is open and ready
        if (numStoppedLines >= sampleRates.length) {
            boolean modified = false;
            for (int i = 0; i < players.length; i++) {
                if (!players[i].isPlaying()) {
                    int playerSampleRate = players[i].getSampleRate();
                    for (int j = 0; j < availableSampleRates.length; j++) {
                        if (availableSampleRates[j] == 0) {
                            boolean success = players[i].reopen(sampleRates[j]);
                            if (success) {
                                modified = true;
                                availableSampleRates[j] = 1;
                                
                                int index = indexOf(sampleRates, playerSampleRate);
                                if (index != -1) {
                                    availableSampleRates[index]--;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            
            if (modified) {
                //if (Build.DEBUG) {
                //    printStatus();
                //}
            }
        }
    }
    
    
    public synchronized void play(AppContext context, Sound sound, Fixed level, Fixed pan,
        boolean loop) 
    {
        boolean played = false;
        int numOpenLines = 0;
        
        // First, try to play an open line
        for (int i = 0; i < players.length; i++) {
            if (!players[i].isPlaying() && players[i].isOpen()) {
                if (!played && players[i].getSampleRate() == sound.getSampleRate()) {
                    players[i].play(context, sound, level, pan, loop);
                    played = true;
                }
                else {
                    numOpenLines++;
                }
            }
        }
        
        // Next, open a line at the sound's sample rate and play
        if (!played) {
            if (numOpenLines == 0) {
                if (Build.DEBUG) CoreSystem.print("Couldn't play sound: no available lines.");
            }
            else {
                for (int i = 0; i < players.length; i++) {
                    if (!players[i].isPlaying()) {
                        boolean success = players[i].reopen(sound.getSampleRate());
                        if (success) {
                            players[i].play(context, sound, level, pan, loop);
                            played = true;
                            numOpenLines--;
                            break;
                        }
                    }
                }
                
                if (!played) {
                    if (Build.DEBUG) CoreSystem.print("Couldn't play " + sound.getSampleRate() +
                        "Hz sound.");
                }
            }
        }
        
        //if (Build.DEBUG) {
        //    printStatus();
        //}
    }
    
    
    private void printStatus() {
        CoreSystem.print("Sound lines: (" + players.length + "):");
        for (int i = 0; i < players.length; i++) {
            DataLinePlayer p = players[i];
            CoreSystem.print("  " + i + ": " + p.getSampleRate() + "Hz, open=" + p.isOpen() +
                ", playing=" + p.isPlaying());
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
    
    
    static class SilentSound extends Sound {
        
        private int numFrames;
        
        public SilentSound(int sampleRate, int numFrames) {
            super(sampleRate);
            this.numFrames = numFrames;
        }
        
        
        public int getNumFrames() {
            return numFrames;
        }
        
        
        public void getSamples(byte[] dest, int destOffset, int destChannels,
            int srcFrame, int numFrames)
        {
            int frameSize = getSampleSize() * destChannels;
            int length = numFrames * frameSize;
            for (int i = 0; i < length; i++) {
                dest[destOffset++] = 0;
            }
        }
    }
    
    
    static class DataLinePlayer {
        
        private SoundStream stream;
        private SourceDataLine line;
        private boolean lastMute;
        private int sampleRate;
        
        
        public DataLinePlayer(int[] sampleRates, int firstAttempt) {
            for (int i = 0; i < sampleRates.length; i++) {
                this.sampleRate = sampleRates[(i + firstAttempt) % sampleRates.length];
                open();
                if (isOpen()) {
                    return;
                }
            }
        }
        
        
        public int getSampleRate() {
            return sampleRate;
        }

        
        public synchronized boolean isOpen() {
            return line != null && line.isOpen();
        }
        
        
        public synchronized void open() {
            if (!isOpen()) {
                AudioFormat format = getFormat(sampleRate);
                
                try {
                    DataLine.Info lineInfo =
                        new DataLine.Info(SourceDataLine.class, format, BUFFER_SIZE);
                    line = (SourceDataLine)AudioSystem.getLine(lineInfo);
                    line.open(format, BUFFER_SIZE);
                }
                catch (Exception ex) {
                    line = null;
                }
            }
        }
        
        
        public synchronized boolean reopen(int sampleRate) {
            int oldSampleRate = this.sampleRate;
            close();
            this.sampleRate = sampleRate;
            open();
            
            if (isOpen()) {
                return true;
            }
            else {
                // Try to open the old format
                this.sampleRate = oldSampleRate;
                open();
                return false;
            }
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
        public synchronized void play(AppContext context, Sound clip, Fixed level, Fixed pan,
            boolean loop) 
        {
            int loopFrame = 0;
            int numLoopFrames = loop ? clip.getNumFrames() : 0;
            int silenceFrames = line.getBufferSize() / line.getFormat().getFrameSize();
            Sound silence = new SilentSound(clip.getSampleRate(), silenceFrames);
            clip = new SoundSequence(new Sound[] { clip, silence });
            stream = new SoundStream(context, clip, level, pan, loopFrame, numLoopFrames, 0);
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
                        // Make sure numBytes is divisible by FRAME_SIZE
                        numBytes -= (numBytes % FRAME_SIZE);
                        
                        stream.render(workBuffer, 0, CHANNELS, numBytes / FRAME_SIZE);
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

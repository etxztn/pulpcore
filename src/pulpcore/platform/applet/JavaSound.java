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

package pulpcore.platform.applet;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import pulpcore.animation.Fixed;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.AppContext;
import pulpcore.platform.SoundEngine;
import pulpcore.platform.SoundStream;
import pulpcore.sound.Playback;
import pulpcore.sound.Sound;
import pulpcore.sound.SoundSequence;

/**
    The JavaSound class is a {@link pulpcore.platform.SoundEngine } implementation that 
    uses the Java Sound API to play sound.
    <p>
    The goal of the player is to have 32 lines open and ready so that latency can be 
    reduced to a minimum.
*/
public class JavaSound implements SoundEngine {
    
    // Max simultaneous sounds (if the underlying Java Sound implementation doesn't have a limit). 
    private static final int MAX_SIMULTANEOUS_SOUNDS = 32;
    
    // The playback formats supported (mono is converted to stereo in SoundStream)
    private static final int[] SAMPLE_RATES = { 8000, 11025, 22050, 44100 };
    private static final int NUM_CHANNELS = 2;
    private static final int FRAME_SIZE = 2 * NUM_CHANNELS;
    
    // The amount of time (in milliseconds) before a clip is played, on Windows.
    // The first 0-10ms sometimes plays at 100% volume, but the remainder is at 50%.
    // This results in an audible click when the volume abruptly changes. This issue cannot
    // be fixed using gain/colume controls, so delay the sound data by 10ms (1/100th of a second).
    private static final int WINDOWS_CLIP_DELAY = 10;
    
    // The amount of time (in milliseconds) to fade before and after the "glitch" 
    private static final int GLITCH_FADE_TIME = 5;
    
    // Buffer size (in milliseconds)
    // This implementation attempts to keep 250ms of sound data in the SourceDataLine's internal 
    // buffer. Up to 1 second of sound data is kept in the internal buffer for slow frame rates.
    // Based on tests, 250ms is required as a minimum on Mac OS X.
    // 4/15/2008: changed to 300ms
    private static final int MIN_BUFFER_SIZE = 300;
    private static final int MAX_BUFFER_SIZE = 1000;
    
    // Work buffer used during audio rendering.
    private static final byte[] WORK_BUFFER = new byte[44100 * FRAME_SIZE * MAX_BUFFER_SIZE / 1000];
    
    private final Mixer mixer;
    private DataLinePlayer[] players = new DataLinePlayer[0];
    private int[] maxSoundsForSampleRate = new int[SAMPLE_RATES.length];
    private int[] sampleRates;
    
    public JavaSound() {
        
        mixer = AudioSystem.getMixer(null);
        
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
                players[i] = new DataLinePlayer(mixer, sampleRates, i % sampleRates.length);
            }
        }
        else {
            players = new DataLinePlayer[0];
        }
        
        // Play a buffer's worth of silence to warm up HotSpot (helps remove popping)
        if (sampleRates.length > 0) {
            Sound noSound = new SilentSound(sampleRates[0], 0);
            play(null, noSound, new Fixed(1), new Fixed(0), false);
            
            // Bizarre: The DirectX implementation of JavaSound (Windows, Java 5 or newer)
            // works better if at least two threads write to a SourceDataLine. It doesn't matter
            // if the 2nd thread stays active or not.
            //
            // Without this 2nd thread, a faint "Geiger-counter" noise is audible on top of
            // sound playback (it is especially noticable when the sound is a pure waveform).
            if (CoreSystem.isWindows() && CoreSystem.isJava15orNewer()) {
                new Thread("PulpCore-Win32SoundInit") {
                    public void run() {
                        try {
                            AudioFormat format = getFormat(sampleRates[0]);
                            DataLine.Info lineInfo =
                                new DataLine.Info(SourceDataLine.class, format);
                            SourceDataLine line = (SourceDataLine)mixer.getLine(lineInfo);
                            line.open(format);
                            byte[] blank = new byte[line.getBufferSize()];
                            line.start();
                            line.write(blank, 0, blank.length);
                            line.drain();
                            line.close();
                        }
                        catch (Exception ex) {
                            if (Build.DEBUG) CoreSystem.print("Blank sound in separate thread", ex);
                        }
                    }
                }.start();
            }
        }
    }        
    
    private static AudioFormat getFormat(int sampleRate) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
            sampleRate, 16, NUM_CHANNELS, FRAME_SIZE, sampleRate, false);
    }
    
    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }
    
    /**
        Gets the maximum number of simultaneous sounds with the
        specified AudioFormat that the default mixer can play.
    */
    private int getMaxSimultaneousSounds(int sampleRate) {
        
        AudioFormat format = getFormat(sampleRate);
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
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
        // This method is called from the AWT event thread.
        // At this point update() won't be called again. 
        // Call update() to mute, then close the lines.
        for (int i = 0; i < players.length; i++) {
            players[i].update(SoundStream.MUTE_TIME*2);
        }
        
        for (int i = 0; i < players.length; i++) {
            players[i].close(true);
        }
    }
    
    public synchronized void update(int timeUntilNextUpdate) {
        
        // Poll the players
        for (int i = 0; i < players.length; i++) {
            players[i].update(timeUntilNextUpdate);
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
    
    public synchronized Playback play(AppContext context, Sound sound, Fixed level, Fixed pan,
        boolean loop) 
    {
        boolean played = false;
        int numOpenLines = 0;
        Playback playback = null;
        
        // First, try to play an open line
        for (int i = 0; i < players.length; i++) {
            if (!players[i].isPlaying() && players[i].isOpen()) {
                if (!played && players[i].getSampleRate() == sound.getSampleRate()) {
                    playback = players[i].play(context, sound, level, pan, loop);
                    played = true;
                }
                else {
                    numOpenLines++;
                }
            }
        }
        
        if (!played) {
            // Next, open a line at the sound's sample rate and play
            if (numOpenLines == 0) {
                if (Build.DEBUG) CoreSystem.print("Couldn't play sound: no available lines.");
            }
            else {
                for (int i = 0; i < players.length; i++) {
                    if (!players[i].isPlaying()) {
                        boolean success = players[i].reopen(sound.getSampleRate());
                        if (success) {
                            playback = players[i].play(context, sound, level, pan, loop);
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
        
        return playback;
        
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
    
    /**
        A simple sound generator that creates silence.
    */
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
        
        private final Mixer mixer;
        private SoundStream stream;
        private SourceDataLine line;
        private int sampleRate;
        private int framesWritten;
        private int numWrites;
        private int minBufferSize;
        
        // For the JavaSound "glitch"
        private int[] fadeGoal = new int[NUM_CHANNELS];
        
        public DataLinePlayer(Mixer mixer, int[] sampleRates, int firstAttempt) {
            this.mixer = mixer;
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
                
                int bufferSize = sampleRate * FRAME_SIZE * MAX_BUFFER_SIZE / 1000;
                int remainder = bufferSize % FRAME_SIZE;
                if (remainder != 0) {
                    bufferSize += FRAME_SIZE - remainder;
                }
                
                try {
                    DataLine.Info lineInfo =
                        new DataLine.Info(SourceDataLine.class, format, bufferSize);
                    line = (SourceDataLine)mixer.getLine(lineInfo);
                    line.open(format, bufferSize);
                }
                catch (Exception ex) {
                    line = null;
                }
            }
        }
        
        public synchronized boolean reopen(int sampleRate) {
            int oldSampleRate = this.sampleRate;
            close(false);
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
        
        public synchronized void close(boolean drain) {
            stream = null;
            if (line == null) {
                return;
            }
            
            if (drain) {
                try {
                    line.drain();
                }
                catch (Exception ex) { 
                    if (Build.DEBUG) CoreSystem.print("DataLinePlayer.drain()", ex);
                }
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
        public synchronized Playback play(AppContext context, Sound clip, Fixed level, Fixed pan,
            boolean loop) 
        {
            int loopFrame = 0;
            int stopFrame = clip.getNumFrames();
            int numLoopFrames = loop ? stopFrame : 0;
            
            // Create a full buffer of post-clip silence
            // This fixes a problem with circular buffers.
            int postSilenceFrames = line.getBufferSize() / line.getFormat().getFrameSize();
            Sound postSilence = new SilentSound(clip.getSampleRate(), postSilenceFrames);
            
            if (CoreSystem.isWindows()) {
                // Windows implementation needs a delay before playing the sound.
                int preSilenceFrames = clip.getSampleRate() * WINDOWS_CLIP_DELAY / 1000;
                Sound preSilence = new SilentSound(clip.getSampleRate(), preSilenceFrames);
                clip = new SoundSequence(new Sound[] { preSilence, clip, postSilence });
                loopFrame += preSilenceFrames;
                stopFrame += preSilenceFrames;
            }
            else {
                // Non-Windows OS
                clip = new SoundSequence(new Sound[] { clip, postSilence });
            }
            
            // Create sound stream
            framesWritten = 0;
            numWrites = 0;
            minBufferSize = MIN_BUFFER_SIZE;
            stream = new SoundStream(context, clip, level, pan, loopFrame, numLoopFrames,
                stopFrame);
            return stream.getPlayback();
        }

        public synchronized boolean isPlaying() {
            return line != null && stream != null;
        }
        
        public synchronized void update(int timeUntilNextUpdate) {
            if (line == null || stream == null) {
                return;
            }
            
            try {
                if (stream.isFinished()) {
                    if (line.available() == line.getBufferSize()) {
                        // Close the line - ready for another Clip
                        close(false);
                        open();
                    }
                }
                else {
                    int bufferSizeThreshold = minBufferSize / 2;
                    int bufferSize = minBufferSize;
                    if (timeUntilNextUpdate > bufferSizeThreshold) {
                        bufferSize = Math.min(MAX_BUFFER_SIZE, 
                            bufferSize + timeUntilNextUpdate - bufferSizeThreshold);
                        if (CoreSystem.isMacOSX()) {
                            // On Mac OS X, once the bufferSize is increased, don't decrease it
                            minBufferSize = Math.max(bufferSize, minBufferSize);
                        }
                    }
                    int desiredSize = sampleRate * FRAME_SIZE * bufferSize / 1000;
                    int actualSize;
                    int available = line.available();
                    
                    if (CoreSystem.isMacOSX() && !CoreSystem.isMacOSXLeopardOrNewer()) {
                        actualSize = (framesWritten - line.getFramePosition()) * FRAME_SIZE;
                    }
                    else {
                        // Windows, Linux, Mac OS X Leopard
                        actualSize = line.getBufferSize() - available;
                    }
                    available = Math.min(available, desiredSize - actualSize);
                    if (available > 0) {
                        // Make sure length is not bigger than the work buffer
                        // and is divisible by FRAME_SIZE
                        int length = available;
                        length = Math.min(length, WORK_BUFFER.length);
                        length -= (length % FRAME_SIZE);
                        
                        stream.render(WORK_BUFFER, 0, NUM_CHANNELS, length / FRAME_SIZE);
                        if (CoreSystem.isMacOSX() || !CoreSystem.isJava15orNewer()) {
                            /*
                                The JavaSound "glitch". Happens on Java 1.4 and all known
                                Mac OS X versions (tested up to Java 1.5 on Leopard).
                                
                                This is a workaround for a bug where 4 frames are repeated in the  
                                audio output. Since the 4 frames are repeated at a predictable 
                                position, fade out to minimize any audible click or pop.
                                
                                This bug was found on several different machines each with different 
                                audio hardware, on Mac OS X and Windows machines with Java 1.4.
                                This led me to believe it's a software problem with the 
                                implementation of Java Sound, and not a hardware problem.
                            */
                            if (numWrites == 0) {
                                fade(length - FRAME_SIZE, true);
                            }
                            else if (numWrites == 1) {
                                fade(0, false);
                            }
                        }
                        if (numWrites == 0) {
                            line.start();
                        }
                        int bytesWritten = 0;
                        while (bytesWritten < length) {
                            bytesWritten +=
                                line.write(WORK_BUFFER, bytesWritten, length - bytesWritten);
                        }
                        framesWritten += length / FRAME_SIZE;
                        numWrites++;
                    }
                }
            }
            catch (Exception ex) {
                if (stream != null) {
                    stream.stop();
                }
                CoreSystem.setTalkBackField("pulpcore.sound-exception", ex);
                close(false);
                open();
            }
        }
        
        private void fade(int position, boolean out) {
            
            if (out) {
                for (int j = 0; j < NUM_CHANNELS; j++) {
                    fadeGoal[j] = getSample(position + 2 * j);
                }
                
                int numRepeatedFrames = 4;
                for (int i = 0; i < numRepeatedFrames; i++) {
                    for (int j = 0; j < NUM_CHANNELS; j++) {
                        setSample(position + 2 * j, fadeGoal[j]);
                    }
                    position -= FRAME_SIZE;
                }
            }
            
            // Fade
            int fadeFrames = sampleRate * GLITCH_FADE_TIME / 1000;
            int positionInc = out ? -FRAME_SIZE : FRAME_SIZE;
            for (int i = 0; i < fadeFrames; i++) {
                for (int j = 0; j < NUM_CHANNELS; j++) {
                    int p = position + 2 * j;
                    int v1 = i + 1;
                    int v2 = fadeFrames - v1;
                    setSample(p, (getSample(p) * v1 + fadeGoal[j] * v2) / fadeFrames);
                }
                position += positionInc;
            }
        }
        
        // Shortcut methods
        
        private int getSample(int offset) {
            return SoundStream.getSample(WORK_BUFFER, offset);
        }
        
        private void setSample(int offset, int sample) {
            SoundStream.setSample(WORK_BUFFER, offset, sample);
        }
    }
}

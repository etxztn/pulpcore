package org.pulpcore.runtime.applet.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import org.pulpcore.runtime.Context;
import org.pulpcore.runtime.Context.VersionInfo;
import org.pulpcore.runtime.applet.Log;
import org.pulpcore.runtime.applet.AppletContext;

public class AppletAudioEngine {

    public enum State {
        INIT, 
        READY,
        FAILURE,
        DESTROYED
    }
    
    // Max simultaneous sounds (if the underlying Java Sound implementation doesn't have a limit). 
    private static final int MAX_SIMULTANEOUS_SOUNDS = 40;
    
    // The playback formats supported (mono is converted to stereo in SoundStream)
    private static final int[] SAMPLE_RATES = { 8000, 11025, 22050, 44100, 48000 };
    private static final int NUM_CHANNELS = 2;
    private static final int FRAME_SIZE = 2 * NUM_CHANNELS;
    private static final int MAX_RATE = SAMPLE_RATES[SAMPLE_RATES.length - 1];
    
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
    private static final byte[] WORK_BUFFER = new byte[MAX_RATE * FRAME_SIZE * MAX_BUFFER_SIZE / 1000];

    private static final Object INIT_LOCK = new Object();
    
    private final AppletContext context;
    private Mixer mixer;
    private DataLinePlayer[] players;
    private int[] maxSoundsForSampleRate;
    private int[] sampleRates;
    private boolean[] usedSampleRates;
    private State state;
    private boolean extraWaitOccurred = false;
    
    private final boolean isWindows;
    private final boolean isJava5;
    private final boolean isJava6;
    private final boolean isMacOSX;
    private final boolean isMacOSX105;
    
    public static AppletAudioEngine getAudioEngine() {
        Context context = Context.getContext();
        AppletAudioEngine audioEngine = null;

        if (context != null && context instanceof AppletContext) {
            audioEngine = ((AppletContext)context).getAudioEngine();
        }
        return audioEngine;
    }
    
    public static AppletAudioEngine create(AppletContext context) {
        final AppletAudioEngine js = new AppletAudioEngine(context);
        
        // Initialize in a new thread because creating a new Mixer takes
        // a long time (up to a minute) on some systems.
        Thread t = new Thread("PulpCore-SoundInit") {
            @Override
            public void run() {
                try {
                    js.init();
                }
                catch (Exception ex) {
                    Log.w("Java Sound init", ex);
                    js.state = AppletAudioEngine.State.FAILURE;
                    synchronized (INIT_LOCK) {
                        INIT_LOCK.notifyAll();
                    }
                }
            }
        };

        synchronized (INIT_LOCK) {
            t.start();
            // Guess: Most engines will be initialized in 250ms or less
            try {
                INIT_LOCK.wait(250);
            }
            catch (InterruptedException ex) { }
        }
        
        return js;
    }
        
    private AppletAudioEngine(AppletContext context) {
        this.context = context;
        mixer = null;
        players = new DataLinePlayer[0];
        maxSoundsForSampleRate = new int[SAMPLE_RATES.length];
        sampleRates = new int[0];
        usedSampleRates = new boolean[0];
        state = State.INIT;
        
        VersionInfo osInfo = context.getOSInfo();
        VersionInfo javaInfo = context.getRuntimeInfo();
        isWindows = osInfo != null && osInfo.getName() != null && osInfo.getName().startsWith("Windows");
        isMacOSX = osInfo != null && "Mac OS X".equals(osInfo.getName());
        isMacOSX105 = osInfo != null && isMacOSX && osInfo.isAtLeastVersion("10.5");
        isJava5 = javaInfo != null && javaInfo.isAtLeastVersion("1.5");
        isJava6 = javaInfo != null && javaInfo.isAtLeastVersion("1.6");
    }
    
    private void init() {

        try {
            mixer = AudioSystem.getMixer(null);
        }
        catch (IllegalArgumentException ex) {
            // Try alternative strategy
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            for (int i = 0; i < mixerInfo.length; i++) {
                try {
                    mixer = AudioSystem.getMixer(mixerInfo[i]);
                    if (mixer != null) {
                        break;
                    }
                }
                catch (IllegalArgumentException ex2) {
                    // Ignore
                }
            }
        }
        if (mixer == null) {
            state = State.FAILURE;
            synchronized (INIT_LOCK) {
                INIT_LOCK.notifyAll();
            }
            return;
        }
        
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
        usedSampleRates = new boolean[numSampleRates];
        int index = 0;
        for (int i = 0; i < SAMPLE_RATES.length && index < numSampleRates; i++) {
            if (maxSoundsForSampleRate[i] > 0) {
                sampleRates[index++] = SAMPLE_RATES[i];
            }
        }
        
        // Create the players
        maxSounds = Math.min(maxSounds, MAX_SIMULTANEOUS_SOUNDS);
        if (maxSounds > 0) {
            DataLinePlayer[] p = new DataLinePlayer[maxSounds];
            for (int i = 0; i < p.length; i++) {
                p[i] = new DataLinePlayer(mixer, sampleRates, i % sampleRates.length);
            }
            players = p;
        }
        else {
            players = new DataLinePlayer[0];
        }
        
        if (sampleRates.length == 0) {
            state = State.FAILURE;
            synchronized (INIT_LOCK) {
                INIT_LOCK.notifyAll();
            }
        }
        else {
            state = State.READY;

            // Play a buffer's worth of silence to warm up HotSpot (helps remove popping)
            for (int i = 0; i < sampleRates.length; i++) {
                AppletAudio noSound = new SilentAudio(sampleRates[i], 0);
                VirtualAudioChannel channel = new VirtualAudioChannel(this, noSound);
                if (requestBind(channel, false)) {
                    channel.play();
                }
            }
            
            synchronized (INIT_LOCK) {
                INIT_LOCK.notifyAll();
            }
            
            // Bizarre: The DirectX implementation of JavaSound (Windows, Java 5 or newer)
            // works better if at least two threads write to a SourceDataLine. It doesn't matter
            // if the 2nd thread stays active or not.
            //
            // Without this 2nd thread, a faint "Geiger-counter" noise is audible on top of
            // sound playback (it is especially noticeable when the sound is a pure waveform).
            //
            // Since we're in a separate thread ("PulpCore-SoundInit") play the sound now.
            if (isWindows && isJava5) {
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
                    Log.w("Blank sound in separate thread", ex);
                }
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
            Log.w("getMaxSimultaneousSounds()", ex);
            return 0;
        }
    }
    
    private boolean canPlay(AppletAudio sound) {
        
        for (int i = 0; i < sampleRates.length; i++) {
            if (sound.getSampleRate() == sampleRates[i]) {
                return true;
            }
        }
        
        String availableRates = "";
        for (int i = 0; i < sampleRates.length; i++) {
            availableRates += sampleRates[i] + "Hz";
            if (i < sampleRates.length - 1) {
                availableRates += ", ";
            }
        }
        Log.w("Can't play sound " + sound + ". Sample rate " + sound.getSampleRate() + "Hz " +
                "not one of the supported sample rates (" + availableRates + ").");
        return false;
    }
    
    public synchronized void destroy() {
        // This method is called from the AWT event thread.
        // At this point update() won't be called again. 
        // Call update() to mute, then close the lines.
        // Do it in a seprate thread so that the window can close immediately.

        state = State.DESTROYED;
        
        (new Thread("PulpCore-SoundDestroy") {
            @Override
            public void run() {
                
                synchronized (AppletAudioEngine.this) {

                    DataLinePlayer[] p = players;

                    float dt = RealAudioChannel.MUTE_DUR*2;
                    int dtms = Math.round(dt * 1000);
                    for (int i = 0; i < p.length; i++) {
                        p[i].update(dtms, true);
                    }

                    for (int i = 0; i < p.length; i++) {
                        p[i].close(true);
                    }
                }
                
            }
        }).start();
    }

    public synchronized void tick(float dt) {
        
        if (state != State.READY) {
            return;
        }
        
        DataLinePlayer[] p = players;
        
        // Poll the players
        int dtms = Math.round(dt * 1000);
        for (int i = 0; i < p.length; i++) {
            p[i].update(dtms);
        }
        
        // Determine if all sample rates are available and ready
        int[] availableSampleRates = new int[sampleRates.length];
        int numStoppedLines = 0;
        for (int i = 0; i < p.length; i++) {
            if (!p[i].isPlaying()) {
                numStoppedLines++;
                
                int index = indexOf(sampleRates, p[i].getSampleRate());
                if (index != -1) {
                    availableSampleRates[index]++;
                }
            }
        }
        
        // Make sure at least 1 line of each sample rate is open and ready
        if (numStoppedLines >= sampleRates.length) {
            boolean modified = false;
            for (int i = 0; i < p.length; i++) {
                if (!p[i].isPlaying()) {
                    int playerSampleRate = p[i].getSampleRate();
                    for (int j = 0; j < sampleRates.length; j++) {
                        if (availableSampleRates[j] == 0 && usedSampleRates[j]) {
                            boolean success = p[i].reopen(sampleRates[j]);
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
    
    public boolean requestBind(VirtualAudioChannel channel) {
        return requestBind(channel, true);
    }

    private boolean requestBind(VirtualAudioChannel channel, boolean userRequested) {
        if (channel == null || channel.getAudio() == null || channel.getAudio().getNumFrames() == 0) {
            return false;
        }
        
        AppletAudio audio = (AppletAudio)channel.getAudio();
        
        // Check if sound system is ready. If still initializing, wait at most three seconds,
        // but only do it once.
        if (state == State.INIT && !extraWaitOccurred) {
            synchronized (INIT_LOCK) {
                if (state == State.INIT) {
                    try {
                        INIT_LOCK.wait(3000);
                    }
                    catch (InterruptedException ex) { }
                    extraWaitOccurred = true;
                }
            }
        }

        if (state != State.READY || !canPlay(audio)) {
            return false;
        }
    
        // The sound engine is ready, and the sound is valid

        boolean attached = false;
        int numOpenLines = 0;
        DataLinePlayer[] p = players;

        // Try to play an open line
        for (int i = 0; i < p.length; i++) {
            if (!p[i].isPlaying() && p[i].isOpen()) {
                if (!attached && p[i].getSampleRate() == audio.getSampleRate()) {
                    p[i].attach(context, channel);
                    attached = true;
                }
                else {
                    numOpenLines++;
                }
            }
        }
        
        if (!attached) {
            // Next, open a line at the sound's sample rate and play
            if (numOpenLines > 0) {
                for (int i = 0; i < p.length; i++) {
                    if (!p[i].isPlaying()) {
                        boolean success = p[i].reopen(audio.getSampleRate());
                        if (success) {
                            p[i].attach(context, channel);
                            attached = true;
                            numOpenLines--;
                            break;
                        }
                    }
                }
                
                if (!attached) {
                    Log.w("Couldn't play " + audio.getSampleRate() + "Hz sound.");
                }
            }
        }

        // Mark this sample rate as used
        if (userRequested && attached) {
            int index = indexOf(sampleRates, audio.getSampleRate());
            if (index != -1) {
                usedSampleRates[index] = true;
            }
        }
        
        //if (Build.DEBUG) {
        //    printStatus();
        //}
        
        return attached;
    }
    
    private void printStatus() {
        DataLinePlayer[] p = players;
        
        Log.i("Sound lines: (" + p.length + "):");
        for (int i = 0; i < p.length; i++) {
            Log.i("  " + i + ": " + p[i].getSampleRate() + 
                "Hz, open=" + p[i].isOpen() +
                ", playing=" + p[i].isPlaying());
        }
    }
    
    public int getNumCurrentlyPlaying(AppletAudio audio) {
        int count = 0;
        DataLinePlayer[] p = players;
        for (int i = 0; i < p.length; i++) {
            if (p[i].isPlaying(audio)) {
                count++;
            }
        }
        return count;
    }
        
    public synchronized int getNumSoundsPlaying() {
        DataLinePlayer[] p = players;
        
        int count = 0;
        for (int i = 0; i < p.length; i++) {
            if (p[i].isPlaying()) {
                count++;
            }
        }
        return count;
    }
    
    public int getMaxSimultaneousSounds() {
        return players.length;
    }
    
    private class DataLinePlayer {
        
        private final Mixer mixer;
        private RealAudioChannel stream;
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
        
        public boolean isPlaying(AppletAudio audio) {
            return (audio != null && stream != null && audio.equals(stream.getAudio()) &&
                    (stream.getState() == RealAudioChannel.STATE_PAUSED ||
                    stream.getState() == RealAudioChannel.STATE_PLAYING));
        }
        
        public int getSampleRate() {
            return sampleRate;
        }

        public AppletAudio getPlayingSound() {
            RealAudioChannel s = stream;
            if (s != null) {
                return s.getAudio();
            }
            else {
                return null;
            }
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

        private void reopen() {
            if (stream != null) {
                stream.unbind();
                stream = null;
            }           
            boolean success = false;
            if (line != null) {
                if (isJava6) {
                    // Not tested on Java 5
                    try {
                        line.stop();
                        line.flush();
                        success = true;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                        success = false;
                    }
                }
                else if (isJava5) {
                    try {
                        // Re-opening lines reduces latency.
                        // However, it fails silently (no exception thrown) on some versions of Java 1.4
                        line.close();
                        line.open();
                        success = true;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                        success = false;
                    }
                }
            }

            if (!success) {
                close(false);
                open();
            }
        }
        
        public synchronized boolean reopen(int sampleRate) {
            if (this.sampleRate == sampleRate) {
                reopen();
                return true;
            }
            else {
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
        }
        
        public synchronized void close(boolean drain) {
            if (stream != null) {
                stream.unbind();
                stream = null;
            }
            if (line == null) {
                return;
            }
            
            if (drain) {
                try {
                    if (line.isRunning()) {
                        line.drain();
                    }
                }
                catch (Exception ex) { 
                    Log.w("DataLinePlayer.drain()", ex);
                }
            }
            
            try {
                line.close();
            }
            catch (Exception ex) { 
                Log.w("DataLinePlayer.close()", ex);
            }
            line = null;
        }

        /**
            Forces playback of the specified buffer, even if isPlaying() is true.
        */
        public synchronized void attach(AppletContext context, VirtualAudioChannel clip) {
        
            int preSilenceFrames = 0;
            
            // Create a full buffer of post-clip silence
            // This fixes a problem with circular buffers.
            int postSilenceFrames = line.getBufferSize() / line.getFormat().getFrameSize();
            
            if (isWindows) {
                // Windows implementation needs a delay before playing the sound.
                preSilenceFrames = clip.getAudio().getSampleRate() * WINDOWS_CLIP_DELAY / 1000;
            }
            
            // Create sound stream
            framesWritten = 0;
            numWrites = 0;
            minBufferSize = MIN_BUFFER_SIZE;
            if (stream != null) {
                stream.reset(context, clip, preSilenceFrames, postSilenceFrames);
            }
            else {
                stream = new RealAudioChannel(context, clip, preSilenceFrames, postSilenceFrames);
            }
        }

        public synchronized boolean isPlaying() {
            return line != null && stream != null;
        }
        
        public synchronized void update(int timeUntilNextUpdate) {
            update(timeUntilNextUpdate, false);
        }

        public synchronized void update(int timeUntilNextUpdate, boolean force) {
            if (line == null || stream == null) {
                return;
            }
            
            try {
                if (stream.getState() == RealAudioChannel.STATE_IDLE) {
                    if (line.available() == line.getBufferSize()) {
                        // Close the line - get ready for another stream
                        reopen();
                    }
                }
                else {
                    int available;
                    if (force) {
                        available = sampleRate * FRAME_SIZE * timeUntilNextUpdate / 1000;
                    }
                    else {
                        int bufferSizeThreshold = minBufferSize / 2;
                        int bufferSize = minBufferSize;
                        if (timeUntilNextUpdate > bufferSizeThreshold) {
                            bufferSize = Math.min(MAX_BUFFER_SIZE,
                                bufferSize + timeUntilNextUpdate - bufferSizeThreshold);
                            if (isMacOSX) {
                                // On Mac OS X, once the bufferSize is increased, don't decrease it
                                minBufferSize = Math.max(bufferSize, minBufferSize);
                            }
                        }
                        int desiredSize = sampleRate * FRAME_SIZE * bufferSize / 1000;
                        int actualSize;
                        available = line.available();

                        if (isMacOSX && !isMacOSX105) {
                            actualSize = (framesWritten - line.getFramePosition()) * FRAME_SIZE;
                        }
                        else {
                            // Windows, Linux, Mac OS X Leopard
                            actualSize = line.getBufferSize() - available;
                        }
                        available = Math.min(available, desiredSize - actualSize);
                    }
                    if (available > 0) {
                        // Make sure length is not bigger than the work buffer
                        // and is divisible by FRAME_SIZE
                        int length = available;
                        length = Math.min(length, WORK_BUFFER.length);
                        length -= (length % FRAME_SIZE);
                        
                        stream.render(WORK_BUFFER, 0, NUM_CHANNELS, length / FRAME_SIZE);
                        if (isMacOSX || !isJava5) {
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
                Log.w("pulpcore.sound-exception", ex);
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
            return RawAudio.getSample(WORK_BUFFER, offset);
        }
        
        private void setSample(int offset, int sample) {
            RawAudio.setSample(WORK_BUFFER, offset, sample);
        }
    }
}

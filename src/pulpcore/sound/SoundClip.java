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

package pulpcore.sound;

import java.io.EOFException;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.util.ByteArray;

/**
    A SoundClip represents a sampled sound clip. 
    Samples are in signed, big endian, 16-bit PCM format.
*/
public final class SoundClip extends Sound {
    
    private static final SoundClip NO_SOUND = new SoundClip(new byte[0]);
    
    // For u-law conversion. 132 * ((1 << i) - 1)
    private static final int[] EXP_TABLE = { 
        0, 132, 396, 924, 1980, 4092, 8316, 16764
    };
    

    private final byte[] data;
    private final int numChannels;
    private final int frameSize;
    private final int numFrames;
    
    
    /**
        Creates a mono 8000Hz clip with the specified samples 
        (signed, big endian, 16-bit PCM format). 
    */
    public SoundClip(byte[] data) {
        this(data, false);
    }
    
    
    /**
        Creates an 8000Hz clip with the specified samples 
        (signed, big endian, 16-bit PCM format). 
    */
    public SoundClip(byte[] data, boolean stereo) {
        this.data = data;
        this.numChannels = stereo ? 2 : 1;
        this.frameSize = getSampleSize() * getNumChannels();
        if ((data.length % frameSize) != 0) {
            throw new IllegalArgumentException();
        }
        this.numFrames = data.length / frameSize;
    }
    
    
    /**
        Returns the number of channels of this sound: 1 for mono or 2 for stereo.
    */
    public int getNumChannels() {
        return numChannels;
    }
    
    
    public int getNumFrames() {
        return numFrames;
    }
    
    
    public void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames)
    {
        if (srcFrame + numFrames > this.numFrames) {
            throw new IllegalArgumentException();
        }
        
        int srcOffset = srcFrame * frameSize;
        
        if (getNumChannels() == destChannels) {
            // Mono-to-mono or stereo-to-stereo
            int length = numFrames * frameSize;
            System.arraycopy(data, srcOffset, dest, destOffset, length);
        }
        else if (getNumChannels() == 1 && destChannels == 2) {
            // Mono-to-stereo
            for (int i = 0; i < numFrames; i++) {
                byte hi = data[srcOffset++];
                byte lo = data[srcOffset++];
                dest[destOffset++] = hi;
                dest[destOffset++] = lo;
                dest[destOffset++] = hi;
                dest[destOffset++] = lo;
            }
        }
        else {
            // Stereo-to-mono
            for (int i = 0; i < numFrames; i++) {
                int left = getSample(srcOffset);
                int right = getSample(srcOffset + 2);
                int sample = (left + right) >> 1;
                dest[destOffset++] = (byte)((sample >> 8) & 0xff);
                dest[destOffset++] = (byte)(sample & 0xff);
                srcOffset += frameSize;
            }
        }
    }
    
    
    private int getSample(int offset) {
        // Signed big endian
        return (data[offset] << 8) | (data[offset + 1] & 0xff); 
    }
    
    
    private void setSample(int offset, int sample) {
        // Signed big endian
        data[offset] = (byte)((sample >> 8) & 0xff);
        data[offset + 1] = (byte)(sample & 0xff);
    }    
    

    //
    // Loading
    //
    
    
    /**
        Loads a sound from the specified sound asset. 
        The sound can either be a .au file (8-bit u-law, 8000Hz) or 
        a .wav file (16-bit, signed, 8000Hz). Both mono and stereo is supported.
        <p>
        This method never returns null. If the sound asset cannot be loaded, or there is no
        sound engine available, a zero-length SoundClip is returned.
    */
    public static SoundClip load(String soundAsset) {
        
        if (!CoreSystem.isSoundEngineAvailable()) {
            return NO_SOUND;
        }
        
        ByteArray in = Assets.get(soundAsset);
        
        if (in == null) {
            return NO_SOUND;
        }
        
        try {
            if (soundAsset.toLowerCase().endsWith(".au")) {
                return loadAU(in, soundAsset);
            }
            else if (soundAsset.toLowerCase().endsWith(".wav")) {
                return loadWAV(in, soundAsset);
            }
            else {
                if (Build.DEBUG) CoreSystem.print("Unknown audio file: " + soundAsset);
                return NO_SOUND;
            }
        }
        catch (EOFException ex) {
            if (Build.DEBUG) CoreSystem.print("Error loading sound: " + soundAsset);
            return NO_SOUND;
        }
    }
        
    
    private static SoundClip loadAU(ByteArray in, String soundAsset) throws EOFException {
        // Make sure magic number is ".snd"
        int magicNumber = in.readInt();
        if (magicNumber != 0x2e736e64) {
            if (Build.DEBUG) CoreSystem.print("Not a sun audio file: " + soundAsset);
            return NO_SOUND;
        }
        int offset = in.readInt();
        int length = in.readInt();
        int encoding = in.readInt();
        int sampleRate = in.readInt();
        int numChannels = in.readInt();
        boolean stereo = (numChannels == 2);
        
        if (encoding != 1 || sampleRate < 8000 || sampleRate > 8100 || 
            numChannels < 1 || numChannels > 2) 
        {
            if (Build.DEBUG) {
                CoreSystem.print("Not an 8-bit u-law, 8000Hz file: " + soundAsset);
            }
            return NO_SOUND;
        }
        
        // Skip to the data section
        in.setPosition(offset);
        
        // Convert u-law to linear
        byte[] samples = new byte[length*2];
        int index = 0;
        for (int i = 0; i < length; i++) {
            int linear = ulaw2linear(in.readByte());
            samples[index++] = (byte)((linear >> 8) & 0xff);
            samples[index++] = (byte)(linear & 0xff);
        }
    
        return new SoundClip(samples, stereo);
    }
    
    
    private static SoundClip loadWAV(ByteArray in, String soundAsset) throws EOFException {
    
        final int RIFF = 0x52494646; // little endian
        final int RIFX = 0x52494658; // big endian (not used)
        final int WAVE = 0x57415645;
        
        final int FMT = 0x20746d66;
        final int DATA = 0x61746164;
        
        // Make sure magic number is "RIFF", followed by 4 bytes, and then "WAVE"
        int magic1 = in.readInt();
        in.readInt();
        int magic2 = in.readInt();
        boolean stereo = false;
        
        if (magic1 != RIFF || magic2 != WAVE) {
            if (Build.DEBUG) CoreSystem.print("Not a WAV file: " + soundAsset);
            return NO_SOUND;
        }
        
        in.setByteOrder(ByteArray.LITTLE_ENDIAN);
        
        // Read each chunk. Only process the FMT and DATA chunks; ignore others.
        // Return once the data chunk is found.
        while (true) {
            int chunkID = in.readInt();
            int chunkSize = in.readInt();
            
            if (chunkID == FMT) {
                int format = in.readShort();
                int numChannels = in.readShort();
                int frameRate = in.readInt();
                int avgByteRate = in.readInt();
                int blockAlign = in.readShort();
                int bitsPerSample = in.readShort();
                stereo = (numChannels == 2);
                
                if (format != 1 || bitsPerSample != 16 || 
                    frameRate < 8000 || frameRate > 8100 || 
                    numChannels < 1 || numChannels > 2) 
                {
                    if (Build.DEBUG) {
                        CoreSystem.print("Not an 16-bit PCM, 8000Hz file: " + soundAsset);
                    }
                    return NO_SOUND;
                }
            }
            else if (chunkID == DATA) {
                byte[] samples = new byte[chunkSize];
                for (int i = 0; i < samples.length; i+=2) {
                    // Convert to big endian (Java endian style)
                    samples[i + 1] = in.readByte();
                    samples[i] = in.readByte();
                }
                return new SoundClip(samples, stereo);
            }
            else {
                // Skip this unknown chunk
                in.setPosition(in.position() + chunkSize);
            }
        }
    }
    
    //
    // For u-law conversion
    //
    
    
    /** 
        Converts an 8-bit ulaw sample to a signed 16-bit linear sample.
        From http://www.speech.cs.cmu.edu/comp.speech/Section2/Q2.7.html
    */
    private static int ulaw2linear(byte ulawbyte) {
        // 1-bit sign, 3-bit exponent, 4-bit mantissa
        int ulaw = ~ulawbyte;
        int sign = ulaw & 0x80;
        int exponent = (ulaw >> 4) & 0x07;
        int mantissa = ulaw & 0x0f;
        int sample = EXP_TABLE[exponent] + (mantissa << (exponent + 3));

        if (sign != 0) 
            return -sample;
        else 
            return sample;
    }        

}

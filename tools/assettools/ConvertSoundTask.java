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

package pulpcore.assettools;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;


public class ConvertSoundTask extends Task {
    
    // PulpCore reads in little-endian WAV format and plays back big-endian samples
    private static final AudioFormat WAV_FORMAT =
        new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000, false);
    
    private File srcFile;
    private File destFile;
    
    
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
    
    
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }
    
    
    public void execute() throws BuildException {
        if (srcFile == null) {
            throw new BuildException("The srcFile is not specified.");
        }
        if (destFile == null) {
            throw new BuildException("The destFile is not specified.");
        }
        
        try {
            convert();
        }
        catch (UnsupportedAudioFileException ex) {
            //log("Invalid sound format: " + WAV_FORMAT);
            throw new BuildException("Not a valid sound file: " + srcFile);
        }
        catch (IOException ex) {
            throw new BuildException("Error creating sound " + srcFile, ex);
        }
    }
    
    private void convert() throws UnsupportedAudioFileException, IOException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(srcFile);
        AudioFormat format = fileFormat.getFormat();
        if (isValid(format)) {
            // Use the Ant copy task
            Copy copyTask = new Copy();
            copyTask.setProject(getProject());
            copyTask.setTaskName(getTaskName());
            copyTask.setFile(srcFile);
            copyTask.setTofile(destFile);
            copyTask.execute();
        }
        else {
            // Try to convert
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(srcFile);
            AudioInputStream convertedStream = null;
            try {
                convertedStream = AudioSystem.getAudioInputStream(WAV_FORMAT, sourceStream);
                AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, destFile);
                log("Converting " + srcFile);
            }
            catch (IllegalArgumentException ex) {
                throw new BuildException("Could not convert: " + srcFile + " (" + format + ")");
            }
            finally {
                sourceStream.close();
                if (convertedStream != null) {
                    convertedStream.close();
                }
            }
        }
    }
    
    
    private boolean isValid(AudioFormat format) {
        if (format.getChannels() != 1) {
            return false;
        }
        else if (format.getSampleRate() < 8000 || format.getSampleRate() > 8100) {
            return false;
        }        
        else if (format.getEncoding() == AudioFormat.Encoding.ULAW) {
            return (format.getSampleSizeInBits() == 8);
        }
        else if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            return (format.isBigEndian() == false && format.getSampleSizeInBits() == 16);
        }
        else {
            return false;
        }
    }

}

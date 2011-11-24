package org.pulpcore.runtime.applet.audio;

import org.pulpcore.media.AudioChannel;
import org.pulpcore.view.property.Property;
import org.pulpcore.view.property.PropertyListener;

/**
Implements the AudioChannel interface and passes calls to a RealAudioChannel.
NOTE: This is identical in both Applet and Desktop versions.
*/
public class VirtualAudioChannel extends AudioChannel {
    
    private final AppletAudioEngine audioEngine;
    private RealAudioChannel realAudioChannel;
    private int startFrame = 0;
    
    public VirtualAudioChannel(AppletAudioEngine audioEngine, AppletAudio audio) {
        super(audio);
        this.audioEngine = audioEngine;
        this.volume.addListener(new PropertyListener() {

            @Override
            public void onPropertyChange(Property property) {
                if (realAudioChannel != null) {
                    realAudioChannel.setVolume(isMute() ? 0 : getVolume());
                }
            }
        });
    }
    
    /* package */ void bind(RealAudioChannel realAudioChannel) {
        this.realAudioChannel = realAudioChannel;
    }
    
    /* package */ void unbind() {
        realAudioChannel = null;
    }
    
    /* package */ RealAudioChannel getBinding() {
        return realAudioChannel;
    }

    @Override
    public int getCurrentFrame() {
        if (realAudioChannel != null) {
            return realAudioChannel.getCurrentFrame();
        }
        else {
            return startFrame;
        }
    }

    @Override
    public void setCurrentFrame(int currentFrame) {
        if (realAudioChannel != null) {
            realAudioChannel.setCurrentFrame(currentFrame);
        }
        else {
            startFrame = currentFrame;
        }
    }
    
    @Override 
    public void setOnCompletionListener(OnCompletionListener completionListener) {
        super.setOnCompletionListener(completionListener);
        if (realAudioChannel != null) {
            realAudioChannel.setOnCompletionListener(completionListener);
        }
    }
    
    @Override
    public void setLooping(boolean looping) {
        if (isLooping() != looping) {
            super.setLooping(looping);
            if (realAudioChannel != null) {
                realAudioChannel.setLooping(looping);
            }
        }
    }
    
    @Override
    public void setMute(boolean mute) {
        if (isMute() != mute) {
            super.setMute(mute);
            if (realAudioChannel != null) {
                realAudioChannel.setVolume(isMute() ? 0 : getVolume());
            }
        }
    }
    
@Override
    public boolean isPlaying() {
        return (realAudioChannel != null && realAudioChannel.isPlaying());
    }    
    
    @Override
    public void play() {
        if (realAudioChannel == null) {
            audioEngine.requestBind(this);
        }
        if (realAudioChannel != null && !realAudioChannel.isPlaying()) {
            realAudioChannel.play(startFrame);
        }
    }

    @Override
    public void pause() {
        if (realAudioChannel != null) {
            realAudioChannel.pause();
        }
    }

    @Override
    public void stop() {
        if (realAudioChannel != null) {
            realAudioChannel.stop();
        }
        startFrame = 0;
    }
}

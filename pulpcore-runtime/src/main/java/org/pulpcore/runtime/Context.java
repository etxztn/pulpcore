/*
    Copyright (c) 2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime;

import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.graphics.Image;
import org.pulpcore.graphics.ImageFont;
import org.pulpcore.graphics.Texture;
import org.pulpcore.media.Audio;
import org.pulpcore.view.Scene;

/**
 
 @see #getContext()
 */
public abstract class Context {

    public static abstract class Factory {
        public abstract Context getContext();
    }

    private static Factory contextFactory;

    public static void setContextFactory(Factory cf) {
        contextFactory = cf;
    }

    /**
     Gets the context for the current app. This method must be invoked from the run loop, or
     a thread spawned from the Run Loop.
     */
    public static Context getContext() {
        if (contextFactory == null) {
            throw new RuntimeException("Context.Factory not set");
        }
        return contextFactory.getContext();
    }

    public static class VersionInfo {
        private final String name;
        private final String version;

        public VersionInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public boolean isAtLeastVersion(String minimumVersion) {
            return version != null && version.compareTo(minimumVersion) >= 0;
        }
    }
    
    private Image brokenImage = null;
    private float masterVolume = 1;
    private boolean masterMute = false;
    
    private String clipboardText;
    
    public void destroy() {
        // Do nothing
    }
    
    public abstract Input getInput();

    public abstract Scene createFirstScene();
    
    public abstract void tick(float dt);
    
    public boolean getMasterMute() {
        return masterMute;
    }

    public void setMasterMute(boolean mute) {
        masterMute = mute;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float volume) {
        masterVolume = volume;
    }

    //
    // Resources
    //
    
    
    public Image getBrokenImage() {
        if (brokenImage == null) {
            Image image = new Image(createTexture(16, 16, true));
            Graphics g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fill();
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, 16, 16);
            g.setColor(Color.RED);
            g.drawLine(2, 2, 13, 13);
            g.drawLine(13, 2, 2, 13);

            brokenImage = image;
        }

        return brokenImage;
    }

    // TODO: Need unloadResources, unloadImage, unloadTexture?
    // TODO: Need to load arbitrary files? 

    /**
    Loads defined resources (images and fonts) from a JSON file.
    @param resourceDescriptorName The JSON file.
    */
    public abstract void loadResources(String resourceDescriptorName);

    public abstract ImageFont loadFont(String fontName);
    
    public abstract Image loadImage(String imageName);
    
    public abstract Texture loadTexture(String textureName);
    
    /**
     Creates a new mutable transparent texture.
     */
    public Texture createTexture(int width, int height) {
        return createTexture(width, height, false);
    }

    /**
     Creates a new mutable image that is either fully transparent or fully opaque.
     */
    public abstract Texture createTexture(int width, int height, boolean opaque);

    public abstract String[] getSupportedSoundFormats();

    public abstract Audio loadAudio(String soundName);
    
    //
    // System info
    //

    /**
     Gets the underlying Operating System version information.
     @return the Operating System version info, or null if not available.
     */
    public abstract VersionInfo getOSInfo();

    /**
     Gets the underlying Java runtime version information.
     @return the Java runtime version info, or null if not available.
     */
    public abstract VersionInfo getRuntimeInfo();

    /**
     Gets the underlying web browser version information.
     @return the web browser version info, or null if not available.
     */
    public abstract VersionInfo getBrowserInfo();

    /**
     Returns the local machine's language as a lowercase two-letter ISO-639 code.
     @return an empty String if the language is unknown
    */
    public abstract String getLocaleLanguage();

    /**
     Returns the local machine's country as an uppercase two-letter ISO-3166 code.
     @return an empty String if the country is unknown
    */
    public abstract String getLocaleCountry();

    //
    // Clipboard
    //

    /**
    Checks if the platform has access to the native operating system
    clipboard. If not, an application-only clipboard is used. The default implementation 
    returns false.
    */
    public boolean isNativeClipboard() {
        return false;
    }

    /**
    Returns the text currently in the clipboard. Returns an empty string
    if there is no text in the clipboard.
    */
    public String getClipboardText() {
        return clipboardText;
    }

    /**
    Sets the text in the clipboard.
    */
    public void setClipboardText(String text) {
        clipboardText = text;
    }

    //
    // Misc
    //

    public abstract String formatNumber(double number, int minFractionDigits, int maxFractionDigits, boolean grouping);
    
}

/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime.jre;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.pulpcore.runtime.Context;
import org.pulpcore.graphics.Image;
import org.pulpcore.graphics.ImageFont;
import org.pulpcore.graphics.Texture;

public abstract class JreContext extends Context {
    
    /**
        Gets a Java system property. Returns null if the property does not exist or there is a
        security exception.
    */
    private static String getJavaProperty(String name, String nullDefault) {
        String value = null;
        try {
            value = System.getProperty(name);
        }
        catch (SecurityException ex) { }

        if (value == null) {
            value = nullDefault;
        }
        return value;
    }

    private final VersionInfo osInfo;
    private final VersionInfo runtimeInfo;

    private final HashMap<String, JSONObject> imageDefs = new HashMap<String, JSONObject>();
    private final HashMap<String, JSONObject> fontDefs = new HashMap<String, JSONObject>();
    
    private final HashMap<String, WeakReference<? extends Image>> imageCache =
        new HashMap<String, WeakReference<? extends Image>>();
    private final HashMap<String, WeakReference<? extends ImageFont>> fontCache =
        new HashMap<String, WeakReference<? extends ImageFont>>();

    public JreContext() {
        osInfo = new VersionInfo(getJavaProperty("os.name", ""), getJavaProperty("os.version", ""));
        runtimeInfo = new VersionInfo("Java", getJavaProperty("java.version", "1.0"));
    }
    
    public void registerContextOnThisThread() {
        // Do nothing
    }

    @Override
    public String formatNumber(double number, int minFractionDigits, int maxFractionDigits, boolean grouping) {
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(grouping);
        format.setMinimumFractionDigits(minFractionDigits);
        format.setMaximumFractionDigits(maxFractionDigits);
        return format.format(number);
    }

    @Override
    public VersionInfo getOSInfo() {
        return osInfo;
    }

    @Override
    public VersionInfo getRuntimeInfo() {
        return runtimeInfo;
    }

    @Override
    public String getLocaleLanguage() {
        Locale locale = Locale.getDefault();
        if (locale == null) {
            return "";
        }
        else {
            return locale.getLanguage();
        }
    }

    @Override
    public String getLocaleCountry() {
        Locale locale = Locale.getDefault();
        if (locale == null) {
            return "";
        }
        else {
            return locale.getCountry();
        }
    }
    
    protected abstract InputStream getResourceAsStream(String name);

    @Override
    public void loadResources(String resourceDescriptorName) {
        try {
            InputStream is = getResourceAsStream(resourceDescriptorName);
            JSONObject object = new JSONObject(new JSONTokener(new InputStreamReader(is)));
            loadResources(object);
            is.close();
        }
        catch (JSONException ex) {
            ex.printStackTrace(System.out);
            System.out.println("Couldn't load JSON file: " + resourceDescriptorName);
        }
        catch (IOException ex) {
            ex.printStackTrace(System.out);
            System.out.println("Couldn't load JSON file: " + resourceDescriptorName);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadResources(JSONObject object) throws JSONException {
        // Check version
        if (object.optInt("version") != 1) {
            throw new JSONException("Only resource format 1 is accepted");
        }

        // Load images
        JSONObject images = object.optJSONObject("images");
        if (images != null) {
            Iterator<String> i = images.keys();
            while (i.hasNext()) {
                String name = i.next();
                imageDefs.put(name, images.getJSONObject(name));
            }
        }

        // Load fonts
        JSONObject fonts = object.optJSONObject("fonts");
        if (fonts != null) {
            Iterator<String> i = fonts.keys();
            while (i.hasNext()) {
                String name = i.next();
                fontDefs.put(name, fonts.getJSONObject(name));
            }
        }
    }

    /**
    Loads an image from a larger texture, defined in the resource JSON file.
    */
    protected Image loadImageDef(String name) {
        JSONObject image = imageDefs.get(name);
        if (image == null) {
            return null;
        }
        try {
            Texture texture = loadTexture(image.getString("texture"));
            JSONArray textureRect = image.optJSONArray("textureRect");
            if (textureRect != null) {
                texture = texture.getSubTexture(
                        textureRect.getInt(0), textureRect.getInt(1),
                        textureRect.getInt(2), textureRect.getInt(3));
            }
            int offsetX = 0;
            int offsetY = 0;
            JSONArray offset = image.optJSONArray("offset");
            if (offset != null) {
                offsetX = offset.getInt(0);
                offsetY = offset.getInt(1);
            }
            int width = texture.getWidth();
            int height = texture.getHeight();
            JSONArray size = image.optJSONArray("size");
            if (size != null) {
                width = size.getInt(0);
                height = size.getInt(1);
            }
            return new Image(texture, offsetX, offsetY, width, height,
                            image.getBoolean("opaque"));
        }
        catch (JSONException ex) {
            ex.printStackTrace(System.out);
            System.out.println("Couldn't parse JSON for image " + name);
            return null;
        }
    }

    protected ImageFont loadFontDef(String name) {
        JSONObject font = fontDefs.get(name);
        if (font == null) {
            return null;
        }
        try {

            Texture defaultTexture = loadTexture(font.getString("texture"));

            // Get Glyphs
            JSONArray glyphDefs = font.getJSONArray("glyphs");
            List<ImageFont.Glyph> glyphs = new ArrayList<ImageFont.Glyph>();
            for (int i = 0; i < glyphDefs.length(); i++) {
                JSONObject glyphDef = glyphDefs.getJSONObject(i);
                char ch = (char)glyphDef.getInt("char");

                Texture texture;
                if (glyphDef.has("texture")) {
                    texture = loadTexture(glyphDef.getString("texture"));
                }
                else {
                    texture = defaultTexture;
                }
                JSONArray textureRect = glyphDef.optJSONArray("textureRect");
                if (textureRect != null) {
                    texture = texture.getSubTexture(
                            textureRect.getInt(0), textureRect.getInt(1),
                            textureRect.getInt(2), textureRect.getInt(3));
                }
                int offsetX = 0;
                int offsetY = 0;
                JSONArray offset = glyphDef.optJSONArray("offset");
                if (offset != null) {
                    offsetX = offset.getInt(0);
                    offsetY = offset.getInt(1);
                }
                glyphs.add(new ImageFont.Glyph(ch, texture, offsetX, offsetY, 
                        (float)(glyphDef.getDouble("advance"))));
            }
            
            ImageFont imageFont = new ImageFont(glyphs);

            // Get kerning
            JSONArray kerningDefs = font.getJSONArray("kerningPairs");
            for (int i = 0; i < kerningDefs.length(); i++) {
//                JSONObject pair = kerningDefs.getJSONObject(i);
//                imageFont.setKerning((char)pair.getInt("first"), (char)pair.getInt("second"),
//                        (int)Math.round(pair.getDouble("kerning")));
                JSONArray pair = kerningDefs.getJSONArray(i);
                imageFont.setKerning((char)pair.getInt(0), (char)pair.getInt(1),
                        (float)(pair.getDouble(2)));
            }
            return imageFont;
        }
        catch (JSONException ex) {
            ex.printStackTrace(System.out);
            System.out.println("Couldn't parse JSON for font " + name);
            return null;
        }
    }
    
    
    @Override
    public ImageFont loadFont(String fontName) {
        // First try Image cache.
        // Image cache exists so we can share Image instances.
        WeakReference<? extends ImageFont> fontRef = fontCache.get(fontName);
        if (fontRef != null) {
            ImageFont font = fontRef.get();
            if (font != null) {
                return font;
            }
            else {
                fontCache.remove(fontName);
            }
        }

        // Next, try FontDefs (JSON)
        ImageFont font = loadFontDef(fontName);
        if (font != null) {
            fontCache.put(fontName, new WeakReference<ImageFont>(font));
            return font;
        }
        else {
            System.out.println("Font not found: " + fontName);
            return null;
        }
    }

    @Override
    public Image loadImage(String imageName) {

        // First try Image cache.
        // Image cache exists so we can share Image instances.
        WeakReference<? extends Image> imageRef = imageCache.get(imageName);
        if (imageRef != null) {
            Image image = imageRef.get();
            if (image != null) {
                return image;
            }
            else {
                imageCache.remove(imageName);
            }
        }

        // Next, try ImageDefs (JSON)
        Image image = loadImageDef(imageName);

        // Finally try the texture.
        if (image == null) {
            Texture texture = loadTexture(imageName);
            if (texture == null) {
                System.out.println("Image not found: " + imageName);
                return getBrokenImage();
            }
            image = new Image(texture);
        }

        imageCache.put(imageName, new WeakReference<Image>(image));
        return image;
    }
}


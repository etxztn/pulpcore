/*
    Copyright (c) 2009, Interactive Pulp, LLC
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
package pulpcore.platform;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.CoreImage;

/**
    A cache for images.
    Values are soft (removed if low on heap space) and have an expiration time (removed after
    a certain time)

    Currently this is only used for filtered images.
    If this works well, it might be useful to use it in PulpCore whenever a new image is needed.
    However, the cached image data will need to be cleared in some cases, for code that needs
    a transparent image to be created.

 */
public class ImageCache {

    private static final boolean LOG = false;

    // HashMap<String, SoftReference<CoreImage>>
    private final HashMap cache = new HashMap();
    private final long timeoutMillis;
    private Timer expirationTimer = new Timer(true);

    public ImageCache() {
        this(30*1000);
    }

    public ImageCache(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    private String createKey(int width, int height, boolean isOpaque) {
        return width + "x" + height + (isOpaque ? "-opaque" : "");
    }

    private synchronized void put(String key, SoftReference value) {
        ArrayList existingList = (ArrayList)cache.get(key);
        if (existingList != null) {
            existingList.add(value);
        }
        else {
            ArrayList list = new ArrayList();
            list.add(value);
            cache.put(key, list);
        }
    }

    private synchronized CoreImage removeAny(String key) {
        ArrayList existingList = (ArrayList)cache.get(key);
        CoreImage image = null;
        if (existingList != null) {
            while (existingList.size() > 0) {
                SoftReference ref = (SoftReference)existingList.remove(existingList.size() - 1);
                image = (CoreImage)ref.get();
                if (image != null) {
                    break;
                }
            }
            if (existingList.size() == 0) {
                cache.remove(key);
            }
        }
        return image;
    }

    private synchronized boolean removeSpecific(String key, SoftReference value) {
        ArrayList existingList = (ArrayList)cache.get(key);
        boolean found = false;
        if (existingList != null) {
            for (int i = 0; i < existingList.size(); i++) {
                if (existingList.get(i) == value) {
                    existingList.remove(i);
                    found = true;
                    break;
                }
            }
            if (existingList.size() == 0) {
                cache.remove(key);
            }
        }
        return found;
    }

    /**
        Gets an image with the specified parameters from the cache, or if the cache
        doesn't contain an image of that type, a new image is created.
     */
    public synchronized CoreImage get(int width, int height, boolean isOpaque) {
        String key = createKey(width, height, isOpaque);
        CoreImage image = removeAny(key);
        if (image != null) {
            if (LOG) CoreSystem.print("ImageCache: Retrieved: " + key);
            return image;
        }
        if (LOG) CoreSystem.print("ImageCache: Created: " + key);
        return new CoreImage(width, height, isOpaque);
    }

    /**
        Puts an image in the cache. If the image is null, this method does nothing.
     */
    public synchronized void put(CoreImage image) {
        if (image != null) {
            String key = createKey(image.getWidth(), image.getHeight(), image.isOpaque());
            if (LOG) CoreSystem.print("ImageCache: Put: " + key);
            SoftReference value = new SoftReference(image);
            put(key, value);
            scheduleRemoval(key, value);
        }
    }

    private void scheduleRemoval(final String key, SoftReference value) {
        // Keep a reference to the key. If a WeakReference is used for the key,
        // the referenced is removed when additional values are added to a previous copy of the key.
        final WeakReference valueReference = new WeakReference(value);
        TimerTask removalTask = new TimerTask() {
            public void run() {
                try {
                    synchronized (ImageCache.this) {
                        SoftReference value = (SoftReference)valueReference.get();
                        if (value != null && removeSpecific(key, value)) {
                            if (LOG) CoreSystem.print("ImageCache: Removed after timeout: " + key);
                        }
                        else {
                            if (LOG) CoreSystem.print("ImageCache: Not removed after timeout: " + key);
                        }
                    }
                }
                catch (Exception ex) {
                    // Shouldn't happen, but it needs to be caught to prevent
                    // the timer from being cancelled
                    if (Build.DEBUG) {
                        CoreSystem.print("Please post this stacktrace to PulpCore mailing list",
                                ex);
                    }
                }
            }
        };

        int maxAttempts = 2;
        while (maxAttempts > 0) {
            try {
                expirationTimer.schedule(removalTask, timeoutMillis);
                break;
            }
            catch (IllegalStateException ex) {
                // TODO: In Java 1.5, name this thread
                expirationTimer = new Timer(true);
            }
            maxAttempts--;
        }
    }
}

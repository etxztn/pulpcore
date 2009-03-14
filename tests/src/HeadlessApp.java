import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import pulpcore.CoreSystem;
import pulpcore.Input;
import pulpcore.Stage;
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.math.Rect;
import pulpcore.platform.AppContext;
import pulpcore.platform.Platform;
import pulpcore.platform.PolledInput;
import pulpcore.platform.SoundEngine;
import pulpcore.platform.Surface;
import pulpcore.scene.Scene;
import pulpcore.util.ByteArray;

public class HeadlessApp extends AppContext implements Platform {

    private static class HeadlessSurface extends Surface {

        public HeadlessSurface() {
            setSize(640, 480);
        }
        
        public CoreImage getOutput() {
            return this.image;
        }

        public long show(Rect[] dirtyRectangles, int numDirtyRectangles) {
            // Do nothing
            return 0;
        }
    }

    private String clipboard = "";
    private Scene scene;
    private Stage stage;
    private HeadlessSurface surface;
    private PolledInput polledInput = new PolledInput();

    public HeadlessApp(Scene scene) {
        setConsoleOutputEnabled(false);
        CoreSystem.init(this);
        this.scene = scene;
        this.surface = new HeadlessSurface();
        this.stage = new Stage(surface, this);
    }

    public CoreImage getOutput() {
        return surface.getOutput();
    }

    public void print(String statement) {
        // Do nothing
    }

    public String getAppProperty(String name) {
        return null;
    }

    public Scene createFirstScene() {
        return scene;
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putUserData(String key, byte[] data) {
        // Do nothing
    }

    public byte[] getUserData(String key) {
        return null;
    }

    public void removeUserData(String key) {
        // Do nothing
    }

    public String getLocaleLanguage() {
        return "en";
    }

    public String getLocaleCountry() {
        return "US";
    }

    public void showDocument(String url, String target) {
        // Do nothing
    }

    public CoreImage loadImage(ByteArray in) {

        if (in == null) {
            return null;
        }

        Image image = Toolkit.getDefaultToolkit().createImage(in.getData());

        MediaTracker tracker = new MediaTracker(null);
        tracker.addImage(image, 0);
        try {
            tracker.waitForAll();
        }
        catch (InterruptedException ex) { }

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }

        int[] data = new int[width * height];
        PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, data, 0, width);
        boolean success = false;
        try {
            success = pixelGrabber.grabPixels();
        }
        catch (InterruptedException ex) { }

        if (success) {
            boolean isOpaque = true;

            // Premultiply alpha
            for (int i = 0; i < data.length; i++) {
                data[i] = Colors.premultiply(data[i]);
                if (isOpaque && (data[i] >>> 24) < 255) {
                    isOpaque = false;
                }
            }
            return new CoreImage(width, height, isOpaque, data);
        }
        else {
            return null;
        }
    }

    public URL getBaseURL() {
        try {
            return new File(".").toURL();
        }
        catch (MalformedURLException ex) {
            return null;
        }
    }

    public Stage getStage() {
        return stage;
    }

    public Surface getSurface() {
        return surface;
    }

    public void pollInput() {
        // Do nothing
    }

    public PolledInput getPolledInput() {
        return polledInput;
    }

    public void requestKeyboardFocus() {
        // Do nothing
    }

    public int getCursor() {
        return Input.CURSOR_DEFAULT;
    }

    public void setCursor(int cursor) {
        // Do nothing
    }

    public int getRefreshRate() {
        return 0;
    }

    public AppContext getThisAppContext() {
        return this;
    }

    public long getTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getTimeMicros() {
        return System.currentTimeMillis() * 1000;
    }

    public long sleepUntilTimeMicros(long timeMicros) {
        long t = (timeMicros - getTimeMicros() + 500) / 1000;
        if (t > 0) {
            try {
                Thread.sleep(t);
            }
            catch (InterruptedException ex) { }
        }
        return getTimeMicros();
    }

    public boolean isNativeClipboard() {
        return false;
    }

    public String getClipboardText() {
        return clipboard;
    }

    public void setClipboardText(String text) {
        clipboard = text;
    }

    public boolean isSoundEngineCreated() {
        return false;
    }

    public SoundEngine getSoundEngine() {
        return null;
    }

    public void updateSoundEngine(int timeUntilNextUpdate) {
        // Do nothing
    }

    public boolean isBrowserHosted() {
        return false;
    }

    public String getBrowserName() {
        return "Headless";
    }

    public String getBrowserVersion() {
        return "1.0";
    }

}

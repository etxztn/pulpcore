package org.pulpcore.tools.packer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.pulpcore.tools.imagefont.Glyph;
import org.pulpcore.tools.imagefont.ImageFont;
import org.pulpcore.tools.png.PNGEncoder;
import org.pulpcore.tools.png.PNGEncoderRunner;

public class TexturePacker {

    public enum OutputFormat {
        JSON,
        COCOS2D,
    }

    class Image extends Object2D {
        
        public final String name;
        public final BufferedImage image;
        public final Rectangle visibleBounds;
        private final Glyph glyph;

        public Image(String name, BufferedImage image) {
            this.name = name;
            this.image = image;
            this.glyph = null;
            Rectangle r = ImageUtils.getVisibleBounds(image);
            if (r == null) {
                visibleBounds = new Rectangle(0, 0, 1, 1);
            }
            else {
                visibleBounds = r;
            }
        }

        private Image(String name, Glyph glyph) {
            this.name = name + "~" + Integer.toHexString(glyph.getCharacter());
            this.image = glyph.getImage();
            this.visibleBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            this.glyph = glyph;
        }

        public Glyph getGlyph() {
            return glyph;
        }

        public BufferedImage getVisibleImage() {
            return image.getSubimage(visibleBounds.x, visibleBounds.y,
                    visibleBounds.width, visibleBounds.height);
        }

        @Override
        public int getWidth() {
            return visibleBounds.width + padding * 2;
        }

        @Override
        public int getHeight() {
            return visibleBounds.height + padding * 2;
        }

        public int getVisibleX() {
            return getX() + padding;
        }

        public int getVisibleY() {
            return getY() + padding;
        }

        public int getVisibleWidth() {
            return visibleBounds.width;
        }

        public int getVisibleHeight() {
            return visibleBounds.height;
        }

        public int getOriginalWidth() {
            return image.getWidth();
        }

        public int getOriginalHeight() {
            return image.getHeight();
        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public BufferedImage getImage() {
            return image;
        }

        public boolean isOpaque() {
            return image.getTransparency() == Transparency.OPAQUE;
        }
    }

    private final Multimap<Integer, Image> images = ArrayListMultimap.create();
    private final List<ImageFont> fonts = new ArrayList<ImageFont>();
    private final int textureWidth;
    private final int textureHeight;
    private final int padding;
    private final boolean allowImagesToSpanMultipleTextures;
    private int pngOptimizationLevel = PNGEncoder.OPTIMIZATION_LEVEL_DEFAULT;

    /**
     Creates a texture packer with a texture width and height of 512, which is a safe size that is
     compatible with most devices.
     */
    public TexturePacker() {
        // Original iPhone: 1024x1024
        // iPhone 4, iPad:  2048x2048
        // Android G1:      512x512
        this(512, 512, 0, false);
    }

    public TexturePacker(int width, int height) {
        this(width, height, 0, false);
    }

    /**
     Creates a texture packer with the specified texture size. For maximum compatibility, make
     the textures size a power of two (256, 512, 1024, etc.) and a square size (width the same
     as height).
     @param padding The padding, in pixels between images.
     */
    public TexturePacker(int textureWidth, int textureHeight, int padding,
            boolean allowImagesToSpanMultipleTextures) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.padding = padding;
        this.allowImagesToSpanMultipleTextures = allowImagesToSpanMultipleTextures;
    }

    public int getPngOptimizationLevel() {
        return pngOptimizationLevel;
    }

    public void setPngOptimizationLevel(int pngOptimizationLevel) {
        this.pngOptimizationLevel = pngOptimizationLevel;
    }

    /**
     Adds an image to later pack into a texture.
     @param name The image name.
     @param image The image to add.
     */
    public void add(String name, BufferedImage image) {
        addToGroup(name, image, -1, 1);
    }

    public void add(String name, BufferedImage image, double scale) {
        addToGroup(name, image, -1, scale);
    }

    public synchronized void addImageFont(ImageFont font, int group) {
        fonts.add(font);
        for (Glyph glyph : font.getGlyphs()) {
            addToGroup(font.getName(), glyph.getImage(), glyph, group, 1);
        }
    }

    /**
     Adds an image to later pack into a texture.
     @param name The image name.
     @param image The image to add.
     @param group The group number. Images in the same group will be added to the same texture
     if possible. If the group number is less than zero, the image is considered "ungrouped",
     and will be added to any texture.
     @throws IllegalArgumentException if the image is larger than the texture size and
     {@link #getAllowImagesToSpanMultipleTextures() } returns false.
     */
    public void addToGroup(String name, BufferedImage image, int group) {
        addToGroup(name, image, group, 1);
    }

    public void addToGroup(String name, BufferedImage image, int group, double scale) {
        addToGroup(name, image, null, group, scale);
    }

    // synchronzied so that images and fonts can be loaded/created in multiple threads
    private synchronized Image addToGroup(String name, BufferedImage image, Glyph glyph,
            int group, double scale) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image must not be null");
        }
        if (scale != 1) {
            image = ImageUtils.getScaledInstance(image, scale);
        }
        if (image.getWidth() + padding*2 > textureWidth ||
                image.getHeight() + padding*2 > textureHeight) {
            if (!allowImagesToSpanMultipleTextures) {
                throw new IllegalArgumentException("Image does not fit in the texture size");
            }
            else {
                // TODO: Split image
            }
        }
        if (group < 0) {
            group = Integer.MAX_VALUE;
        }
        Image image2D;
        if (glyph == null) {
            image2D = new Image(name, image);
        }
        else {
            image2D = new Image(name, glyph);
        }
        images.put(group, image2D);
        return image2D;
    }

    /**
     Removes all images that were added to this texture packer.
     */
    public void clear() {
        images.clear();
        fonts.clear();
    }

    /**
     Packs the images into textures. Each texture is saved as a PNG, prefixed with the string
     "texture", and numbered sequentially. For example, if three textures are created, they are named
     "texture0.png", "texture1.png", "texture2.png".
     @param destDir The directory to save the textures.
     */
    public void pack(OutputFormat format, File destDir) throws IOException {
        pack(format, destDir, "texture", "");
    }

    public void pack(OutputFormat format, File destDir, String filePrefix, String fileSuffix) throws IOException {

        destDir.mkdirs();
        
        // Place in bins
        // TODO: Sort groups so that the groups with the most pixels are positioned first.
        // TODO: Try adding in different orders in an attempt to minimize the number of bins.
        // TODO: Try to half-size any bin to see if the images still fit in it.
        List<Bin2D<Image>> bins = new ArrayList<Bin2D<Image>>();
        bins.add(new Bin2D<Image>(textureWidth, textureHeight));
        for (int group : asSortedList(images.keySet())) {
            List<Image> imageList = new ArrayList<Image>(images.get(group));
            Collections.sort(imageList);
            if (group == Integer.MAX_VALUE) {
                for (Image image : imageList) {
                    putAnywhere(bins, image);
                }
            }
            else {
                tryToPutInOneBin(bins, imageList);
            }
        }

        // Cleanup: delete old textures, plist files, and .fnt files
        deleteOldFiles(destDir, filePrefix, fileSuffix, ".png");
        if (format == OutputFormat.COCOS2D) {
            deleteOldFiles(destDir, filePrefix, fileSuffix, ".plist");
            for (File file : destDir.listFiles()) {
                if (file.getName().endsWith(fileSuffix + ".fnt")) {
                    file.delete();
                }
            }
        }

        // Write bins to textures
        PNGEncoderRunner encoder = new PNGEncoderRunner();
        encoder.setOptimizeForPremultipliedDisplay(true);
        encoder.setOptimizationLevel(pngOptimizationLevel);
        destDir.mkdirs();
        for (int i = 0; i < bins.size(); i++) {
            Bin2D<Image> bin = bins.get(i);
            List<Image> list = bin.getObjects();

            BufferedImage texture = new BufferedImage(textureWidth, textureHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = texture.createGraphics();
            for (Image image : list) {
                g.drawImage(image.getVisibleImage(), image.getVisibleX(), image.getVisibleY(), null);
            }
            g.dispose();
            
            File destFile = new File(destDir, filePrefix + i + fileSuffix + ".png");
            encoder.addImage(texture, destFile);
        }
        encoder.run();

        // Write output format
        if (format == OutputFormat.COCOS2D) {
            OutputCocos2D.output(bins, padding, fonts, destDir, filePrefix, fileSuffix);
        }
        else {
            OutputJson.output(bins, padding, fonts, destDir, filePrefix, fileSuffix);
        }
    }

    private void deleteOldFiles(File destDir, String filePrefix, String fileSuffix, String fileExt) {
        int index = 0;
        while (true) {
            File destFile = new File(destDir, filePrefix + index + fileSuffix + fileExt);
            if (destFile.exists()) {
                destFile.delete();
                index++;
            }
            else {
                break;
            }
        }
    }

    private void tryToPutInOneBin(List<Bin2D<Image>> bins, Collection<Image> imageList) {
        // Attempt to put in one existing bin
        for (Bin2D<Image> bin : bins) {
            if (bin.canFit(imageList)) {
                bin.addAll(imageList);
                return;
            }
        }

        // Attempt to put in one new bin
        List<Image> unplacedImages = new ArrayList<Image>();
        Bin2D<Image> newBin = new Bin2D<Image>(textureWidth, textureHeight);
        for (Image image : imageList) {
            if (!newBin.add(image)) {
                unplacedImages.add(image);
            }
        }
        bins.add(newBin);
        if (unplacedImages.size() > 0) {
            tryToPutInOneBin(bins, unplacedImages);
        }
    }

    private void putAnywhere(List<Bin2D<Image>> bins, Image object) {
        for (Bin2D<Image> bin : bins) {
            if (bin.add(object)) {
                return;
            }
        }
        Bin2D<Image> newBin = new Bin2D<Image>(textureWidth, textureHeight);
        if (!newBin.add(object)) {
            // Shouldn't happen
            throw new IllegalArgumentException("Image does not fit in the texture size");
        }
        bins.add(newBin);
    }

    private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        Collections.sort(list);
        return list;
    }

   
}

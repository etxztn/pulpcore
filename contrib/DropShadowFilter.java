
import pulpcore.animation.Color;
import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.sprite.ImageSprite;

/**
    A simple drop shadow. The source image should not be opaque, and should be large enough
    to contain the drop shadow in its bounds.
    For opaque images, try CoreImage.expandCanvas() first.

    Note: The API of all filters will change before they are integrated with the trunk.
 */
public class DropShadowFilter  {

    private final PropertyListener updater = new PropertyListener() {
        public void propertyChange(Property property) {
            if (property == color) {
                createShadowColorTable();
            }
            isDirty = true;
        }
    };

    /**
        The shadow x offset. The default value is 3.
    */
    public final Int offsetX = new Int(updater, 3);

    /**
        The shadow y offset. The default value is 3.
    */
    public final Int offsetY = new Int(updater, 3);
    
    /**
        The shadow color. The default value is black with 50% alpha.
    */
    public final Color color = new Color(updater, Colors.rgba(0, 0, 0, 128));

    /**
        The shadow blur radius. The radius can range from 0 (no blur) to 255.
        The default value is 4.
        Integer values are optimized to render slightly faster.
    */
    public final Fixed radius = new Fixed(updater, 4);

    private CoreImage src;
    private CoreImage dst;
    private ImageSprite sprite;
    private boolean isDirty;
    private int[] shadowColorTable = new int[256];

    public static CoreImage filter(CoreImage source, int offsetX, int offsetY, float radius,
            int argbColor)
    {
        DropShadowFilter filter = new DropShadowFilter(new ImageSprite(source, 0, 0));
        filter.radius.set(radius);
        filter.offsetX.set(offsetX);
        filter.offsetY.set(offsetY);
        filter.color.set(argbColor);
        filter.update(0);
        return filter.dst;
    }

    public DropShadowFilter(ImageSprite sprite) {
        this.sprite = sprite;
        this.src = sprite.getImage();
        this.dst = new CoreImage(src.getWidth(), src.getHeight(), false);
        isDirty = true;
        createShadowColorTable();
    }

    private void createShadowColorTable() {
        int rgb = Colors.rgb(color.get());
        int alpha = Colors.getAlpha(color.get());
        for (int i = 0; i < 256; i++) {
            shadowColorTable[i] = Colors.premultiply(rgb, (alpha * i) >> 8);
        }
    }

    public void update(int elapsedTime) {
        offsetX.update(elapsedTime);
        offsetY.update(elapsedTime);
        color.update(elapsedTime);
        radius.update(elapsedTime);

        if (isDirty) {
            BlurFilter.filter(src, dst, (float)radius.get(), offsetX.get(), offsetY.get());

            // Post processing: convert blur to the shadow, then add the src image on top.
            int[] srcData = src.getData();
            int[] dstData = dst.getData();
            for (int i = 0; i < srcData.length; i++) {

                int srcPixel = srcData[i];
                int dstPixel = shadowColorTable[dstData[i] >>> 24];

                int sa = srcPixel >>> 24;
                int sr = (srcPixel >> 16) & 0xff;
                int sg = (srcPixel >> 8) & 0xff;
                int sb = srcPixel & 0xff;
                int da = dstPixel >>> 24;
                int dr = (dstPixel >> 16) & 0xff;
                int dg = (dstPixel >> 8) & 0xff;
                int db = dstPixel & 0xff;
                int oneMinusSA = 0xff - sa;

                // SrcOver: original image on top of shadow
                da = sa + ((da * oneMinusSA) >> 8);
                dr = sr + ((dr * oneMinusSA) >> 8);
                dg = sg + ((dg * oneMinusSA) >> 8);
                db = sb + ((db * oneMinusSA) >> 8);

                dstData[i] = (da << 24) | (dr << 16) | (dg << 8) | db;
            }
            sprite.setImage(dst);
            sprite.setDirty(true);
            isDirty = false;
        }
    }
}

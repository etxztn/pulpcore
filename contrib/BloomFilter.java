
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.sprite.ImageSprite;

/**
    Performs a bloom effect. Example usage:
 <pre>

public class BloomTest extends Scene2D {

    BloomFilter filter;

	@Override
	public void load() {
		add(new FilledSprite(BLACK));

		ImageSprite sprite = new ImageSprite("image.png", 0, 0);
		add(sprite);

        filter = new BloomFilter(sprite);

        // Animate the blur radius
        Timeline t = new Timeline();
        t.animate(filter.radius, 0, 128, 5000, null, 0);
        t.animate(filter.radius, 128, 0, 5000, null, 5000);
        t.loopForever();
        addTimeline(t);
	}

	@Override
	public void update(int elapsedTime) {
        super.update(elapsedTime);
        filter.update(elapsedTime);
	}
}
 </pre>
Note: The API of all filters will change before they are integrated with the trunk.
*/
public class BloomFilter extends BlurFilter {

    public BloomFilter(ImageSprite sprite) {
        super(sprite);
    }

    public static CoreImage filter(CoreImage source, float radius) {
        BloomFilter filter = new BloomFilter(new ImageSprite(source, 0, 0));
        filter.radius.set(radius);
        filter.update(0);
        return filter.dst;
    }

    @Override
    protected int preProcessChannel(int r, int c) {
        // Brighten
        return Math.min(255, CoreMath.mul(c, (r>>8)+CoreMath.ONE));
    }

    @Override
    protected void postProcess(int r, int[] srcData, int[] dstData) {
        // Simple Add
        for (int i = 0; i < srcData.length; i++) {

            int srcPixel = srcData[i];
            int dstPixel = dstData[i];

            int sa = srcPixel >>> 24;
            int sr = (srcPixel >> 16) & 0xff;
            int sg = (srcPixel >> 8) & 0xff;
            int sb = srcPixel & 0xff;
            int da = dstPixel >>> 24;
            int dr = (dstPixel >> 16) & 0xff;
            int dg = (dstPixel >> 8) & 0xff;
            int db = dstPixel & 0xff;
            int na = Math.min(255, sa+((da*r)>>20));
            int nr = Math.min(255, sr+((dr*r)>>20));
            int ng = Math.min(255, sg+((dg*r)>>20));
            int nb = Math.min(255, sb+((db*r)>>20));

            dstData[i] = (na << 24) | (nr << 16) | (ng << 8) | nb;
        }
    }
}

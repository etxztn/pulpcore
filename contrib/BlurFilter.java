/*
 Inspired by Florent Dupont who was inspired by Romain Guy.
 */
import pulpcore.animation.Fixed;
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.sprite.ImageSprite;

/**
    Performs a blur. Example usage:
 <pre>

public class BlurTest extends Scene2D {

    BlurFilter filter;

	@Override
	public void load() {
		add(new FilledSprite(BLACK));

		ImageSprite sprite = new ImageSprite("image.png", 0, 0);
		add(sprite);

        filter = new BlurFilter(sprite);

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
public class BlurFilter {

    private static final int MAX_RADIUS = 255;

    private static final boolean INTEGER_ONLY = false;

    private static final boolean CPU_TEST = false;

    // If true, the outside of the image is the same pixels as the border.
    // If false, the outside of the image is considered BORDER_COLOR.
    private static final boolean CLAMP = true;

    private static final int BORDER_COLOR = Colors.TRANSPARENT;

    // Work buffers. Total overhead is (w * 4 * 4 + 256 * 3 * 4) bytes
    // Where w = (maxImageWidth + (MAX_RADIUS+1)*2+1)
    private static int[] columnSumA = new int[1024];
    private static int[] columnSumR = new int[1024];
    private static int[] columnSumG = new int[1024];
    private static int[] columnSumB = new int[1024];
    private static final int[] colorTable = new int[256];
    private static final int[] colorTablef = new int[256];
    private static final int[] colorTable1f = new int[256];
    private static final Object lock = new Object();

    /**
        The blur radius. The radius can range from 0 (no blur) to 255. The default value is 4.
        Integer values are optimized to render slightly faster.
    */
    public final Fixed radius = new Fixed(4);

    private ImageSprite sprite;
    private CoreImage src;
    protected CoreImage dst;
    private int offsetX;
    private int offsetY;
    private boolean isDirty;

    private int lastRadius = 0;

    private int time;
    private int timeLastUpdate;

    public BlurFilter(ImageSprite sprite) {
        this(sprite, new CoreImage(sprite.getImage().getWidth(), sprite.getImage().getHeight(),
                sprite.getImage().isOpaque()));
    }

    private BlurFilter(ImageSprite sprite, CoreImage dst) {
        this.sprite = sprite;
        this.src = sprite.getImage();
        this.dst = dst;
        int w = src.getWidth() + (MAX_RADIUS+1)*2+1;
        if (w > columnSumA.length) {
            columnSumA = new int[w];
            columnSumR = new int[w];
            columnSumG = new int[w];
            columnSumB = new int[w];
        }
        time = 0;
        timeLastUpdate = 0;
        isDirty = true;
    }

    public static CoreImage filter(CoreImage source, float radius) {
        BlurFilter filter = new BlurFilter(new ImageSprite(source, 0, 0));
        filter.radius.set(radius);
        filter.update(0);
        return filter.dst;
    }

    public static void filter(CoreImage src, CoreImage dst, float radius, int offsetX, int offsetY) {
        BlurFilter filter = new BlurFilter(new ImageSprite(src, 0, 0), dst);
        filter.radius.set(radius);
        filter.offsetX = offsetX;
        filter.offsetY = offsetY;
        filter.update(0);
    }

    public void update(int elapsedTime) {

        time += elapsedTime;
        radius.update(elapsedTime);

        int r = CoreMath.clamp(radius.getAsFixed(), 0, CoreMath.toFixed(MAX_RADIUS));
        if (INTEGER_ONLY) {
            r &= 0xffff0000;
        }
        else {
            // Round to nearest 1/8th a pixel (granularity of fmul() functions)
            r &= 0xffffe000;
        }

        if (CPU_TEST) {
            isDirty = true;
        }
        else if (r != lastRadius) {
            // Update at a limited frame rate, depending on the radius
            int timeLimit = 0;
            if (r < CoreMath.toFixed(4)) {
                timeLimit = 16; // ~60 fps
            }
            else if (r < CoreMath.toFixed(8)) {
                timeLimit = 22; // ~45 fps
            }
            else if (r < CoreMath.toFixed(16)) {
                timeLimit = 32; // ~30 fps
            }
            else if (r < CoreMath.toFixed(32)) {
                timeLimit = 40; // ~25 fps
            }
            else if (r < CoreMath.toFixed(64)) {
                timeLimit = 50; // ~20 fps
            }
            else {
                timeLimit = 64; // ~15 fps
            }

            if (time - timeLastUpdate >= timeLimit) {

                // If the change crossed an integer boundary, use the int value.
                if (r < lastRadius) {
                    if (CoreMath.toIntCeil(r) != CoreMath.toIntCeil(lastRadius)) {
                        r = CoreMath.ceil(r);
                    }
                }
                else {
                    if (CoreMath.toIntFloor(r) != CoreMath.toIntFloor(lastRadius)) {
                        r = CoreMath.floor(r);
                    }
                }
                isDirty = true;
            }

        }

        if (isDirty) {
            timeLastUpdate = time;
            if (r <= 0 && offsetX == 0 && offsetY == 0 && !CPU_TEST) {
                System.arraycopy(src.getData(), 0, dst.getData(), 0,
                        src.getWidth() * src.getHeight());
            }
            else {
                filter(r);
            }
            sprite.setImage(dst);
            sprite.setDirty(true);
            lastRadius = r;
            isDirty = false;
        }
    }

    // Synchronized because it uses shared static buffers.
    // A thread-safe implementation might store the buffers as a thread-local variable instead.
    private void filter(int r) {
        //pulpcore.CoreSystem.print(CoreMath.toString(r, 3));
        synchronized (lock) {
            doFilter(r, offsetX, offsetY);
            postProcess(r, src.getData(), dst.getData());
        }
    }

    /**

     @param c The channel value (integer), from 0 to 255
     @param r The radius (16:16 fixed-point), from 0 to 255
    */
    protected int preProcessChannel(int r, int c) {
        return c;
    }

    protected void postProcess(int r, int[] srcData, int[] dstData) {

    }

    private int clampSrcX(int x) {
        return CoreMath.clamp(x, 0, src.getWidth()-1);
    }

    private int clampSrcY(int y) {
        return CoreMath.clamp(y, 0, src.getHeight()-1);
    }

    private boolean isValidSrcX(int x) {
        return x >= 0 && x < src.getWidth();
    }

    private boolean isValidSrcY(int y) {
        return y >= 0 && y < src.getHeight();
    }

    // Ugly as hell.
    // Fractional blur size. No temporary buffers.
    private void doFilter(final int r, final int offsetX, final int offsetY) {
        final int scanSize = src.getWidth();
        final int w = src.getWidth();
        final int h = src.getHeight();
        final int[] srcData = src.getData();
        final int[] dstData = dst.getData();

        final int rInt = CoreMath.toIntFloor(r);
        final int f = CoreMath.fracPart(r);
        final long windowLength = r * 2 + CoreMath.ONE;
        final long windowArea = CoreMath.mul(windowLength, windowLength);
        final int columnOffset = rInt + 1;

        for (int i = 0; i < 256; i++) {
            int c = CoreMath.clamp(preProcessChannel(r, i), 0, 255);
            int v = ((c << 16) | (c << 8) | c);

            colorTable[i] = (int)CoreMath.div(v, windowArea);
            colorTablef[i] = (int)CoreMath.mulDiv(v, f, windowArea);
            colorTable1f[i] = colorTable[i] - colorTablef[i];
        }

        // Setup column sums for first destination row

        int limit = (f == 0 || INTEGER_ONLY) ? rInt : rInt + 1;
        for (int x = 0; x < w; x++) {
            int index = x + columnOffset;
            int srcX = x - offsetX;
            if (CLAMP) {
                srcX = clampSrcX(srcX);
            }

            columnSumA[index] = 0;
            columnSumR[index] = 0;
            columnSumG[index] = 0;
            columnSumB[index] = 0;

            for (int i = -limit; i <= limit; i++) {
                int srcY = i - offsetY;
                if (CLAMP) {
                    srcY = clampSrcY(srcY);
                }

                int pixel;
                if (!CLAMP && (!isValidSrcX(srcX) || !isValidSrcY(srcY))) {
                    pixel = BORDER_COLOR;
                }
                else {
                    pixel = srcData[srcX + srcY * scanSize];
                }
                int[] table = (!INTEGER_ONLY && f != 0 && Math.abs(i) == limit) ? colorTablef : colorTable;
                columnSumA[index] += table[(pixel >>> 24)];
                columnSumR[index] += table[((pixel >> 16) & 0xff)];
                columnSumG[index] += table[((pixel >> 8) & 0xff)];
                columnSumB[index] += table[(pixel & 0xff)];
            }
        }

        if (!CLAMP) {
            for (int i = 0; i <= rInt; i++) {
                columnSumA[i] = colorTable[BORDER_COLOR >>> 24];
                columnSumR[i] = colorTable[((BORDER_COLOR >> 16) & 0xff)];
                columnSumG[i] = colorTable[((BORDER_COLOR >> 8) & 0xff)];
                columnSumB[i] = colorTable[(BORDER_COLOR & 0xff)];
                columnSumA[w + columnOffset + i] = colorTable[BORDER_COLOR >>> 24];
                columnSumR[w + columnOffset + i] = colorTable[((BORDER_COLOR >> 16) & 0xff)];
                columnSumG[w + columnOffset + i] = colorTable[((BORDER_COLOR >> 8) & 0xff)];
                columnSumB[w + columnOffset + i] = colorTable[(BORDER_COLOR & 0xff)];
            }
        }


        int rowIndex = 0;
        for (int y = 0; y < h; y++) {

            if (CLAMP) {
                for (int i = 0; i <= rInt; i++) {
                    columnSumA[i] = columnSumA[columnOffset];
                    columnSumR[i] = columnSumR[columnOffset];
                    columnSumG[i] = columnSumG[columnOffset];
                    columnSumB[i] = columnSumB[columnOffset];
                    columnSumA[w + columnOffset + i] = columnSumA[w + columnOffset - 1];
                    columnSumR[w + columnOffset + i] = columnSumR[w + columnOffset - 1];
                    columnSumG[w + columnOffset + i] = columnSumG[w + columnOffset - 1];
                    columnSumB[w + columnOffset + i] = columnSumB[w + columnOffset - 1];
                }
            }

            if (f == 0 || INTEGER_ONLY) {
                // Setup pixel sum for (0, y)
                int sumA = 0;
                int sumR = 0;
                int sumG = 0;
                int sumB = 0;
                for (int i = -rInt; i <= rInt; i++) {
                    sumA += columnSumA[columnOffset + i];
                    sumR += columnSumR[columnOffset + i];
                    sumG += columnSumG[columnOffset + i];
                    sumB += columnSumB[columnOffset + i];
                }

                // Write this row
                int dstIndex = rowIndex;
                int prevX = columnOffset - rInt;
                int nextX = columnOffset + rInt + 1;
                for (int x = 0; x < w; x++) {
                    dstData[dstIndex++] =
                            ((sumA & 0xff0000) << 8) |
                            ((sumR & 0xff0000)) |
                            ((sumG & 0xff0000) >> 8) |
                            ((sumB & 0xff0000) >> 16);

                    // Sutract last column, add next column
                    sumA += columnSumA[nextX] - columnSumA[prevX];
                    sumR += columnSumR[nextX] - columnSumR[prevX];
                    sumG += columnSumG[nextX] - columnSumG[prevX];
                    sumB += columnSumB[nextX] - columnSumB[prevX];
                    prevX++;
                    nextX++;
                }
            }
            else {
                // Setup pixel sum for (0, y)
                int sumA = fmul(f, columnSumA[columnOffset - rInt - 1]) + fmul(f, columnSumA[columnOffset + rInt + 1]);
                int sumR = fmul(f, columnSumR[columnOffset - rInt - 1]) + fmul(f, columnSumR[columnOffset + rInt + 1]);
                int sumG = fmul(f, columnSumG[columnOffset - rInt - 1]) + fmul(f, columnSumG[columnOffset + rInt + 1]);
                int sumB = fmul(f, columnSumB[columnOffset - rInt - 1]) + fmul(f, columnSumB[columnOffset + rInt + 1]);
                for (int i = -rInt; i <= rInt; i++) {
                    sumA += columnSumA[columnOffset + i];
                    sumR += columnSumR[columnOffset + i];
                    sumG += columnSumG[columnOffset + i];
                    sumB += columnSumB[columnOffset + i];
                }

                // Write this row
                int dstIndex = rowIndex;
                int prevX = columnOffset - rInt;
                int nextX = columnOffset + rInt + 1;
                for (int x = 0; x < w; x++) {
                    dstData[dstIndex++] =
                            ((sumA & 0xff0000) << 8) |
                            ((sumR & 0xff0000)) |
                            ((sumG & 0xff0000) >> 8) |
                            ((sumB & 0xff0000) >> 16);

                    // Sutract last column, add next column
                    sumA += fmul(f, columnSumA[prevX], columnSumA[prevX-1], columnSumA[nextX], columnSumA[nextX+1]);
                    sumR += fmul(f, columnSumR[prevX], columnSumR[prevX-1], columnSumR[nextX], columnSumR[nextX+1]);
                    sumG += fmul(f, columnSumG[prevX], columnSumG[prevX-1], columnSumG[nextX], columnSumG[nextX+1]);
                    sumB += fmul(f, columnSumB[prevX], columnSumB[prevX-1], columnSumB[nextX], columnSumB[nextX+1]);
                    prevX++;
                    nextX++;
                }
            }

            // Prepare column sums for next row
            int numLoops = (f == 0 || INTEGER_ONLY) ? 1 : 2;
            for (int i = 0; i < numLoops; i++) {
                int[] table = (numLoops == 1) ? colorTable : (i == 0) ? colorTable1f : colorTablef;
                boolean prevIndexValid = true;
                boolean nextIndexValid = true;
                int prevIndex = y - offsetY - rInt - i;
                int nextIndex = y - offsetY + rInt + i + 1;
                if (CLAMP) {
                    prevIndex = clampSrcY(prevIndex);
                    nextIndex = clampSrcY(nextIndex);
                }
                else {
                    prevIndexValid = isValidSrcY(prevIndex);
                    nextIndexValid = isValidSrcY(nextIndex);
                }
                prevIndex *= scanSize;
                nextIndex *= scanSize;
                int columnIndex = columnOffset;

                if (offsetX > 0) {
                    int prevPixel = CLAMP ? srcData[prevIndex] : BORDER_COLOR;
                    int nextPixel = CLAMP ? srcData[nextIndex] : BORDER_COLOR;
                    int pa = prevPixel >>> 24;
                    int pr = (prevPixel >> 16) & 0xff;
                    int pg = (prevPixel >> 8) & 0xff;
                    int pb = prevPixel & 0xff;
                    int na = nextPixel >>> 24;
                    int nr = (nextPixel >> 16) & 0xff;
                    int ng = (nextPixel >> 8) & 0xff;
                    int nb = nextPixel & 0xff;
                    int da = table[na] - table[pa];
                    int dr = table[nr] - table[pr];
                    int dg = table[ng] - table[pg];
                    int db = table[nb] - table[pb];
                    for (int x = 0; x < offsetX; x++) {
                        columnSumA[columnIndex] += da;
                        columnSumR[columnIndex] += dr;
                        columnSumG[columnIndex] += dg;
                        columnSumB[columnIndex] += db;
                        columnIndex++;
                    }
                }
                int w2 = (offsetX > 0) ? (w - offsetX) : w;
                for (int x = 0; x < w2; x++) {
                    int prevPixel;
                    int nextPixel;
                    if (CLAMP) {
                        prevPixel = srcData[prevIndex + x];
                        nextPixel = srcData[nextIndex + x];
                    }
                    else {
                        prevPixel = prevIndexValid ? srcData[prevIndex + x] : BORDER_COLOR;
                        nextPixel = nextIndexValid ? srcData[nextIndex + x] : BORDER_COLOR;
                    }

                    int pa = prevPixel >>> 24;
                    int pr = (prevPixel >> 16) & 0xff;
                    int pg = (prevPixel >> 8) & 0xff;
                    int pb = prevPixel & 0xff;
                    int na = nextPixel >>> 24;
                    int nr = (nextPixel >> 16) & 0xff;
                    int ng = (nextPixel >> 8) & 0xff;
                    int nb = nextPixel & 0xff;

                    // Subtract last row, add next row
                    columnSumA[columnIndex] += table[na] - table[pa];
                    columnSumR[columnIndex] += table[nr] - table[pr];
                    columnSumG[columnIndex] += table[ng] - table[pg];
                    columnSumB[columnIndex] += table[nb] - table[pb];
                    columnIndex++;

                }
                if (offsetX < 0) {
                    int prevPixel = CLAMP ? srcData[prevIndex + w - 1] : BORDER_COLOR;
                    int nextPixel = CLAMP ? srcData[nextIndex + w - 1] : BORDER_COLOR;
                    int pa = prevPixel >>> 24;
                    int pr = (prevPixel >> 16) & 0xff;
                    int pg = (prevPixel >> 8) & 0xff;
                    int pb = prevPixel & 0xff;
                    int na = nextPixel >>> 24;
                    int nr = (nextPixel >> 16) & 0xff;
                    int ng = (nextPixel >> 8) & 0xff;
                    int nb = nextPixel & 0xff;
                    int da = table[na] - table[pa];
                    int dr = table[nr] - table[pr];
                    int dg = table[ng] - table[pg];
                    int db = table[nb] - table[pb];
                    for (int x = 0; x < -offsetX; x++) {
                        columnSumA[columnIndex] += da;
                        columnSumR[columnIndex] += dr;
                        columnSumG[columnIndex] += dg;
                        columnSumB[columnIndex] += db;
                        columnIndex++;
                    }
                }
            }
            rowIndex += scanSize;
        }
    }

    // f is .000 to .111
    private int fmul(int f, int x) {
        int a = 0;
        if ((f & 0x8000) != 0) {
            a += x >> 1;
        }
        if ((f & 0x4000) != 0) {
            a += x >> 2;
        }
        if ((f & 0x2000) != 0) {
            a += x >> 3;
        }
        return a;
    }

    private int fmul(int f, int w, int x, int y, int z) {
        int a = y - w;
        if ((f & 0x8000) != 0) {
            a += (w >> 1) - (x >> 1) - (y >> 1) + (z >> 1);
        }
        if ((f & 0x4000) != 0) {
            a += (w >> 2) - (x >> 2) - (y >> 2) + (z >> 2);
        }
        if ((f & 0x2000) != 0) {
            a += (w >> 3) - (x >> 3) - (y >> 3) + (z >> 3);
        }
        return a;
    }
}

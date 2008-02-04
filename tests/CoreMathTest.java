import pulpcore.math.CoreMath;
import org.junit.Test;
import static org.junit.Assert.*;

/**
    This class tests the accuracy of the Fixed-point CoreMath functions, with the exception
    of CoreMath.tan() near pi/2 and -pi/2, which can never be accurate.
*/
public class CoreMathTest {

    public static final int NUM_TESTS = 100000;
    
    @Test public void countBits() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = CoreMath.rand(Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue("Wrong bit count: " + value, 
                Integer.bitCount(value) == CoreMath.countBits(value));
        }
    }
    
    @Test public void log2() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = CoreMath.rand(1, Integer.MAX_VALUE);
            int actual = CoreMath.log2(value);
            int expected = (int)Math.floor(Math.log(value) / Math.log(2));
            assertEquals("Wrong log2: " + value, expected, actual);
        }
    }
    
    @Test public void randWithinBounds() {
        int min = -100;
        int max = 100;
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = CoreMath.rand(min, max);
            assertTrue("Random out of range: " + value, value >= min && value <= max);
        }
    }
    
    @Test public void randDistribution() {
        int min = -100;
        int max = 100;
        int numValues = max - min + 1;
        int[] count = new int[numValues];
        int samples = NUM_TESTS;
        double expected = (double)samples / count.length;
        
        // Create samples
        for (int i = 0; i < samples; i++) {
            count[-min + CoreMath.rand(min, max)]++;
        }
        
        // Get the variance
        double variance = 0;
        for (int i = 0; i < numValues; i++) {
            double s = Math.abs(count[i] - expected);
            variance += s*s;
        }
        variance /= samples;
        assertTrue("Distribution not uniform: " + variance, variance >= 0 && variance < 1.25);
    }
    
    
    @Test public void sqrtGreaterThanOne() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double v = CoreMath.rand(1.0, CoreMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.sqrt(v);
            double actualResult = CoreMath.toDouble(CoreMath.sqrt(CoreMath.toFixed(v)));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.00002);
        }
    }
    
    @Test public void sqrtLessThanOne() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int f = CoreMath.rand(0, CoreMath.ONE);
            double v = CoreMath.toDouble(f);
            double expectedResult = Math.sqrt(v);
            double actualResult = CoreMath.toDouble(CoreMath.sqrt(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.00002);
        }
    }
    
    @Test public void cosNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(-2*Math.PI, 2*Math.PI);
            double expectedResult = Math.cos(angle);
            double actualResult = CoreMath.toDouble(CoreMath.cos(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.000037);
        }
    }
    
    @Test public void cosAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(CoreMath.MIN_DOUBLE_VALUE, CoreMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.cos(angle);
            double actualResult = CoreMath.toDouble(CoreMath.cos(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void sinNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(-2*Math.PI, 2*Math.PI);
            double expectedResult = Math.sin(angle);
            double actualResult = CoreMath.toDouble(CoreMath.sin(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.000037);
        }
    }
    
    @Test public void sinAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(CoreMath.MIN_DOUBLE_VALUE, CoreMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.sin(angle);
            double actualResult = CoreMath.toDouble(CoreMath.sin(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void tanNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(-Math.PI/4, Math.PI/4);
            double expectedResult = Math.tan(angle);
            double actualResult = CoreMath.toDouble(CoreMath.tan(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0001);
        }
    }
    
    @Test public void tanWider() {
        for (int i = 0; i < NUM_TESTS; i++) {
            // It gets more inaccurate near pi/2
            double angle = CoreMath.rand(-Math.PI*.49, Math.PI*.49);
            double expectedResult = Math.tan(angle);
            double actualResult = CoreMath.toDouble(CoreMath.tan(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, .05);
        }
    }
    
    @Test public void acos() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int f = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            double v = CoreMath.toDouble(f);
            double expectedResult = Math.acos(v);
            double actualResult = CoreMath.toDouble(CoreMath.acos(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.000062);
        }
    }
    
    @Test public void asin() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int f = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            double v = CoreMath.toDouble(f);
            double expectedResult = Math.asin(v);
            double actualResult = CoreMath.toDouble(CoreMath.asin(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.000062);
        }
    }
    
    @Test public void atanNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(-1.0, 1.0);
            double expectedResult = Math.atan(angle);
            double actualResult = CoreMath.toDouble(CoreMath.atan(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.00007);
        }
    }
    
    @Test public void atanAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = CoreMath.rand(CoreMath.MIN_DOUBLE_VALUE, CoreMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.atan(angle);
            double actualResult = CoreMath.toDouble(CoreMath.atan(CoreMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void atan2NearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int fx = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            int fy = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            double x = CoreMath.toDouble(fx);
            double y = CoreMath.toDouble(fy);
            double expectedResult = Math.atan2(y, x);
            double actualResult = CoreMath.toDouble(CoreMath.atan2(fy, fx));
            assertEquals("Bad result for " + x + ", " + y, expectedResult, actualResult, 0.00006);
        }
    }
    
    @Test public void atan2Any() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int fx = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            int fy = CoreMath.rand(-CoreMath.ONE, CoreMath.ONE);
            double x = CoreMath.toDouble(fx);
            double y = CoreMath.toDouble(fy);
            double expectedResult = Math.atan2(y, x);
            double actualResult = CoreMath.toDouble(CoreMath.atan2(fy, fx));
            assertEquals("Bad result for " + x + ", " + y, expectedResult, actualResult, 0.0009);
        }
    }
}

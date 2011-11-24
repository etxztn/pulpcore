package org.pulpcore.test;

import org.pulpcore.math.FixedMath;
import org.junit.Test;
import static org.junit.Assert.*;

/**
    This class tests the accuracy of the Fixed-point FixedMath functions, with the exception
    of FixedMath.tan() near pi/2 and -pi/2, which can never be accurate.
*/
public class FixedMathTest {

    public static final int NUM_TESTS = 1000000;
    
    private static int rand(int min, int max) {
        // Prevent overflow
        long range = (long)max - (long)min;
        int value = (int)(min + (long)(Math.random()*(range+1)));

        // Bounds check is probably not needed.
        if (value < min) {
            return min;
        }
        else if (value > max) {
            return max;
        }
        else {
            return value;
        }
    }

    private static double rand(double min, double max) {
        double value = min + Math.random() * (max-min);

        // Bounds check is probably not needed.
        if (value < min) {
            return min;
        }
        else if (value > max) {
            return max;
        }
        else {
            return value;
        }
    }
    
    @Test public void randIntWithinBounds() {
        int min = -10000;
        int max = 10000;
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = rand(min, max);
            assertTrue("Random out of range: " + value, value >= min && value <= max);
        }
    }

    @Test public void randDoubleWithinBounds() {
        double min = -10000;
        double max = 10000;
        for (int i = 0; i < NUM_TESTS; i++) {
            double value = rand(min, max);
            assertTrue("Random out of range: " + value, value >= min && value <= max);
        }
    }

    @Test public void randIntDistribution() {
        int min = -10000;
        int max = 10000;
        int numValues = max - min + 1;
        int[] count = new int[numValues];
        double expected = (double)NUM_TESTS / numValues;
        
        // Create samples
        for (int i = 0; i < NUM_TESTS; i++) {
            count[-min + rand(min, max)]++;
        }
        
        // Get the variance
        double variance = 0;
        for (int i = 0; i < numValues; i++) {
            double s = Math.abs(count[i] - expected);
            variance += s*s;
        }
        variance /= NUM_TESTS;
        assertTrue("Distribution not uniform: " + variance, variance >= 0 && variance < 1.25);
    }
    
    @Test public void randFloatDistribution() {
        int min = -10000;
        int max = 10000;
        int numValues = max - min + 1;
        int[] count = new int[numValues];
        double expected = (double)NUM_TESTS / numValues;

        // Create samples
        for (int i = 0; i < NUM_TESTS; i++) {
            count[-min + (int)Math.round(rand((double)min, (double)max))]++;
        }

        // Get the variance
        double variance = 0;
        for (int i = 0; i < numValues; i++) {
            double s = Math.abs(count[i] - expected);
            variance += s*s;
        }
        variance /= NUM_TESTS;
        assertTrue("Distribution not uniform: " + variance, variance >= 0 && variance < 1.25);

//        double min = -100;
//        double max = 100;
//        double numValues = max - min + 1;
//        double[] samples = new double[NUM_TESTS];
//        double expected = (double)numValues / NUM_TESTS;
//
//        // Create samples
//        for (int i = 0; i < NUM_TESTS; i++) {
//            samples[i] = FixedMath.rand(min, max);
//        }
//
//        java.util.Arrays.sort(samples);
//
//        // Get the variance
//        double variance = 0;
//        for (int i = 1; i < NUM_TESTS; i++) {
//            double d = samples[i] - samples[i-1];
//            double s = Math.abs(d - expected);
//            variance += s*s;
//        }
//        variance /= NUM_TESTS;
//        System.out.println("Variance: " + variance);
//        System.out.println("Expected: " + expected);
//        assertTrue("Distribution not uniform: " + variance, variance >= 0 && variance < 1.25);
    }

    @Test public void sqrtGreaterThanOne() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double v = rand(1.0, FixedMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.sqrt(v);
            double actualResult = FixedMath.toDouble(FixedMath.sqrt(FixedMath.toFixed(v)));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.00002);
        }
    }
    
    @Test public void sqrtLessThanOne() {
        for (int f = 0; f <= FixedMath.ONE; f++) {
            double v = FixedMath.toDouble(f);
            double expectedResult = Math.sqrt(v);
            double actualResult = FixedMath.toDouble(FixedMath.sqrt(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.00002);
        }
    }
    
    @Test public void cosNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(-2*Math.PI, 2*Math.PI);
            double expectedResult = Math.cos(angle);
            double actualResult = FixedMath.toDouble(FixedMath.cos(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void cosAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(FixedMath.MIN_DOUBLE_VALUE, FixedMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.cos(angle);
            double actualResult = FixedMath.toDouble(FixedMath.cos(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.000951);
        }
    }
    
    @Test public void sinNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(-2*Math.PI, 2*Math.PI);
            double expectedResult = Math.sin(angle);
            double actualResult = FixedMath.toDouble(FixedMath.sin(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void sinAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(FixedMath.MIN_DOUBLE_VALUE, FixedMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.sin(angle);
            double actualResult = FixedMath.toDouble(FixedMath.sin(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.00093);
        }
    }
    
    @Test public void tanNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(-Math.PI/4, Math.PI/4);
            double expectedResult = Math.tan(angle);
            double actualResult = FixedMath.toDouble(FixedMath.tan(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.0009);
        }
    }
    
    @Test public void tanWider() {
        for (int i = 0; i < NUM_TESTS; i++) {
            // It gets more inaccurate near pi/2
            double angle = rand(-Math.PI*.49, Math.PI*.49);
            double expectedResult = Math.tan(angle);
            double actualResult = FixedMath.toDouble(FixedMath.tan(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, .03);
        }
    }
    
    @Test public void acos() {
        for (int f = -FixedMath.ONE; f <= FixedMath.ONE; f++) {
            double v = FixedMath.toDouble(f);
            double expectedResult = Math.acos(v);
            double actualResult = FixedMath.toDouble(FixedMath.acos(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.000062);
        }
    }

    @Test public void asin() {
        for (int f = -FixedMath.ONE; f <= FixedMath.ONE; f++) {
            double v = FixedMath.toDouble(f);
            double expectedResult = Math.asin(v);
            double actualResult = FixedMath.toDouble(FixedMath.asin(f));
            assertEquals("Bad result for " + v, expectedResult, actualResult, 0.000062);
        }
    }
    
    @Test public void atanNearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(-1.0, 1.0);
            double expectedResult = Math.atan(angle);
            double actualResult = FixedMath.toDouble(FixedMath.atan(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.00007);
        }
    }
    
    @Test public void atanAny() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double angle = rand(FixedMath.MIN_DOUBLE_VALUE, FixedMath.MAX_DOUBLE_VALUE);
            double expectedResult = Math.atan(angle);
            double actualResult = FixedMath.toDouble(FixedMath.atan(FixedMath.toFixed(angle)));
            assertEquals("Bad result for " + angle, expectedResult, actualResult, 0.000981);
        }
    }
    
    @Test public void atan2NearZero() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int fx = rand(-FixedMath.ONE*3/4, FixedMath.ONE*3/4);
            int fy = rand(-FixedMath.ONE*3/4, FixedMath.ONE*3/4);
            double x = FixedMath.toDouble(fx);
            double y = FixedMath.toDouble(fy);
            double expectedResult = Math.atan2(y, x);
            double actualResult = FixedMath.toDouble(FixedMath.atan2(fy, fx));
            assertEquals("Bad result for " + x + ", " + y, expectedResult, actualResult, 0.00016);
        }
    }
    
    @Test public void atan2Any() {
        for (int i = 0; i < NUM_TESTS; i++) {
            double x = rand(FixedMath.MIN_DOUBLE_VALUE, FixedMath.MAX_DOUBLE_VALUE);
            double y = rand(FixedMath.MIN_DOUBLE_VALUE, FixedMath.MAX_DOUBLE_VALUE);
            int fx = FixedMath.toFixed(x);
            int fy = FixedMath.toFixed(y);
            double expectedResult = Math.atan2(y, x);
            double actualResult = FixedMath.toDouble(FixedMath.atan2(fy, fx));
            assertEquals("Bad result for " + x + ", " + y, expectedResult, actualResult, 0.00016);
        }
    }
}

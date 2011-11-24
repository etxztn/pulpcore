package org.pulpcore.test;

import org.pulpcore.math.MathUtil;
import org.junit.Test;
import static org.junit.Assert.*;

/**
    This class tests the accuracy of the Fixed-point CoreMath functions, with the exception
    of CoreMath.tan() near pi/2 and -pi/2, which can never be accurate.
*/
public class MathUtilTest {

    public static final int NUM_TESTS = 1000000;
    
    @Test public void countBits() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = MathUtil.rand(Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue("Wrong bit count: " + value, 
                Integer.bitCount(value) == MathUtil.countBits(value));
        }
    }
    
    @Test public void log2() {
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = MathUtil.rand(1, Integer.MAX_VALUE);
            int actual = MathUtil.log2(value);
            int expected = (int)Math.floor(Math.log(value) / Math.log(2));
            assertEquals("Wrong log2: " + value, expected, actual);
        }
    }
    
    @Test public void randIntWithinBounds() {
        int min = -10000;
        int max = 10000;
        for (int i = 0; i < NUM_TESTS; i++) {
            int value = MathUtil.rand(min, max);
            assertTrue("Random out of range: " + value, value >= min && value <= max);
        }
    }

    @Test public void randDoubleWithinBounds() {
        double min = -10000;
        double max = 10000;
        for (int i = 0; i < NUM_TESTS; i++) {
            double value = MathUtil.rand(min, max);
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
            count[-min + MathUtil.rand(min, max)]++;
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
    
    @Test public void randDoubleDistribution() {
        int min = -10000;
        int max = 10000;
        int numValues = max - min + 1;
        int[] count = new int[numValues];
        double expected = (double)NUM_TESTS / numValues;

        // Create samples
        for (int i = 0; i < NUM_TESTS; i++) {
            count[-min + (int)Math.round(MathUtil.rand((double)min, (double)max))]++;
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

}

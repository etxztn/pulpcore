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

package org.pulpcore.math;

import java.util.List;

public class MathUtil {
    
    // Prevent instantiation
    private MathUtil() { }

    //
    // Bit manipulation
    //
    
    /**
        Returns true if the number (greater than 1) is a power of two.
    */
    public static boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }
  
    /**
        Counts the number of "on" bits in an integer.
    */
    public static int countBits(int n) {
        /*
        int count = 0;
        while (n > 0) {
            count += (n & 1);
            n >>= 1;
        }
        return count;
        */
        int count = n;
        count = ((count >> 1)  & 0x55555555) + (count & 0x55555555);
        count = ((count >> 2)  & 0x33333333) + (count & 0x33333333);
        count = ((count >> 4)  & 0x0F0F0F0F) + (count & 0x0F0F0F0F);
        count = ((count >> 8)  & 0x00FF00FF) + (count & 0x00FF00FF);
        count = ((count >> 16) & 0x0000FFFF) + (count & 0x0000FFFF);
        return count;
    }
    
    /**
        Returns the log base 2 of an integer greater than 0. The returned value
        is equal to {@code Math.floor(Math.log(n) / Math.log(2))}.
    */
    public static int log2(int n) {
        int count = 0;
        while (true) {
            n >>= 1;
            if (n == 0) {
                return count;
            }
            count++;
        }
        
/*
        int count = 0;
        
        if ((n & 0xFFFF0000) != 0) {
            n >>= 16;
            count = 16;
        }
        if ((n & 0xFF00) != 0) {
            n >>= 8;
            count |= 8;
        } 
        if ((n & 0xF0) != 0) {
            n >>= 4;
            count |= 4;
        } 
        if ((n & 0xC) != 0) {
            n >>= 2;
            count |= 2;
        } 
        if ((n & 0x2) != 0) {
            //n >>= 1;
            count |= 1;
        } 
        
        return count;
*/
    }
    
    //
    // Integer math
    //
  
    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number. 
    */
    public static int clamp(int n, int min, int max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }
    
    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number.
    */
    public static float clamp(float n, float min, float max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }

    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number. 
    */
    public static double clamp(double n, double min, double max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }
    
    /**
        Returns the sign of a number.
    */
    public static int sign(int n) {
        return (n > 0)?1:((n < 0)?-1:0);
    }

    /**
        Returns the sign of a number.
    */
    public static int sign(float n) {
        return (n > 0)?1:((n < 0)?-1:0);
    }
    
    /**
        Returns the sign of a number.
    */
    public static int sign(double n) {
        return (n > 0)?1:((n < 0)?-1:0);
    }
    
    /**
        Divides the number, n, by the divisor, d, rounding the result to the 
        nearest integer.
    */
    public static int intDivRound(int n, int d) {
        if ((d > 0) ^ (n > 0)) {
            return (n - (d >> 1)) / d;
        }
        else {
            return (n + (d >> 1)) / d;
        }
    }
    
    /**
        Divides the number, n, by the divisor, d, returning the nearest integer 
        less than or equal to the result.
    */
    public static int intDivFloor(int n, int d) {
        if (d > 0) {
            if (n < 0) {
                return (n - d + 1) / d;
            }
            else {
                return n / d;
            }
        }
        else if (d < 0) {
            if (n > 0) {
                return (n - d - 1) / d;
            }
            else {
                return n / d;
            }
        }
        else {
            // d == 0 throws ArithmeticException
            return n / d;
        }
    }
    
    /**
        Divides the number, n, by the divisor, d, returning the nearest integer 
        greater than or equal to the result.
    */
    public static int intDivCeil(int n, int d) {
        return -intDivFloor(-n, d);
    }
    
    //
    // Random number generation
    //
    
    /**
        Returns a random integer from 0 to max, inclusive
    */
    public static int rand(int max) {
        return rand(0, max);
    }
    
    /**
        Returns a random integer from min to max, inclusive
    */
    public static int rand(int min, int max) {
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
    
    /**
        Returns a random double from 0 to max, inclusive
    */
    public static float rand(float max) {
        return rand(0, max);
    }
    
    /**
        Returns a random double from min to max, inclusive
    */
    public static float rand(float min, float max) {
        float value = min + (float)Math.random() * (max-min);

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

    /**
        Returns a random double from 0 to max, inclusive
    */
    public static double rand(double max) {
        return rand(0, max);
    }

    /**
        Returns a random double from min to max, inclusive
    */
    public static double rand(double min, double max) {
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

    /**
        Returns a random boolean.
    */
    public static boolean rand() {
        return (rand(0, 1) == 0);
    }
    
    /**
        Returns true if a random event occurs. 
        @param percent The probability of the event occurring, from 0 (never) to 100 (always).
    */
    public static boolean randChance(int percent) {
        return (rand(1, 100) <= percent);
    }

    /**
     Gets a random element from an array.
     */
    public static boolean rand(boolean[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static byte rand(byte[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static short rand(short[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static char rand(char[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static int rand(int[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static long rand(long[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }
    
    /**
     Gets a random element from an array.
     */
    public static float rand(float[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static double rand(double[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from an array.
     */
    public static <T> T rand(T[] array) {
        int index = rand(0, array.length - 1);
        return array[index];
    }

    /**
     Gets a random element from a List.
     */
    public static <T> T rand(List<T> array) {
        int index = rand(0, array.size() - 1);
        return array.get(index);
    }

    //
    // Geometry
    //

    public static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float)Math.sqrt(dx * dx + dy *dy);
    }
  
}

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
package org.pulpcore.util;

import org.pulpcore.runtime.Context;
import org.pulpcore.view.property.FloatProperty;
import org.pulpcore.view.property.IntProperty;

/**
Convenience class for working with Objects
*/
public class Objects {
    
    private Objects() {
        
    }

    /** Gets the hash code for the object, or 0 if the object is null. */
    public static int hashCode(Object object) {
        if (object == null) {
            return 0;
        }
        else {
            return object.hashCode();
        }
    }

    /** Gets the hash code for array of object. */
    public static int hashCode(Object... objects) {
        int result = 17;
        for (Object object : objects) {
            result = 31 * result + hashCode(object);
        }
        return result;
    }

    /** Gets the String representation of the object. If the object is null, returns "null". */
    public static String toString(Object object) {
        if (object == null) {
            return "null";
        }
        else {
            return object.toString();
        }
    }

    /** 
    Returns true if the two objects are equal. If either object is null, returns true only if
    both objects are null.
    */
    public static boolean equal(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            return (obj1 == obj2);
        }
        else {
            return obj1.equals(obj2);
        }
    }

    /**
    Returns true if the CharSequence is null or empty.
    */
    public static boolean isNullOrEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }
        
    /**
    Formats a text string using the specified format and string arguments. 
    This is an alternative for GWT, which doesn't offer String.format or printf.
    <p>
    <table cellspacing="2">
    <tr>
        <td><b>Conversion</b></td>
        <td><b>Description</b></td>
    </tr>
    <tr>
        <td>%s</td>
        <td>Any object converted to a string. ("Hello World")</td>
    </tr>
    <tr>
        <td>%d</td>
        <td>An integer - (Integer or IntProperty). ("1000")</td>
    </tr>
    <tr>
        <td>%,d</td>
        <td>An integer with a grouping separator. ("1,000")</td>
    </tr>
    <tr>
        <td>%f</td>
        <td>Floating-point number (Float or FloatProperty). ("1000.12345")</td></tr>
    <tr>
        <td>%,f</td>
        <td>Floating-point number with a grouping separator. ("1,000.12345")</td>
    </tr>
    <tr>
        <td>%.2f</td>
        <td>Floating-point number with a specific number of digits after the decimal
        ("1000.12")</td>
    </tr>
    <tr>
        <td>%,.2f</td>
        <td>Floating-point number with a grouping separator and a specific number of digits
        ("1,000.12")</td>
    </tr>
    <tr>
        <td>%%</td>
        <td>The percent character.</td>
    </tr>
    </table>
    <p>

    Example:
      StringFormatter.format("Name: %s  Score: %,d", "Fred", 1000);
    returns
      "Name: Fred  Score: 1,000"
        
    */
    public static String format(String message, Object... args) {
        if (message == null) {
            return null;
        }
        else if (args == null || args.length == 0) {
            return message;
        }

        Context context = Context.getContext();
        
        StringBuilder buffer = new StringBuilder();
        int lastIndex = 0;
        int currentArg = 0;
        
        while (true) {
            int index = message.indexOf('%', lastIndex);
            
            if (index == -1) {
                break;
            }
            
            buffer.append(message.substring(lastIndex, index));
            
            boolean grouping = false;
            boolean decimal = false;
            boolean found = false;
            int numFracDigits = -1;
            int codeIndex = index + 1;
            while (!found) {
                if (message.length() <= codeIndex) {
                    buffer.append(message.substring(index, codeIndex));
                    break;
                }
                
                char ch = message.charAt(codeIndex++);
                if (ch == '%') {
                    buffer.append('%');
                    break;
                }
                else if (ch == ',') {
                    grouping = true;
                }
                else if (ch == '.') {
                    numFracDigits = 0;
                    decimal = true;
                }
                else if (ch == 's') {
                    found = true;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    buffer.append(toString(arg));
                }
                else if (ch == 'd') {
                    found = true;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    if (arg instanceof Number) {
                        int value = ((Number)arg).intValue();
                        buffer.append(context.formatNumber(value, 0, 0, grouping));
                    }
                    else if (arg instanceof IntProperty) {
                        int value = ((IntProperty)arg).get();
                        buffer.append(context.formatNumber(value, 0, 0, grouping));
                    }
                    else if (arg instanceof FloatProperty) {
                        int value = Math.round(((FloatProperty)arg).get());
                        buffer.append(context.formatNumber(value, 0, 0, grouping));
                    }
                    else {
                        buffer.append(toString(arg));
                    }
                }
                else if (ch == 'f') {
                    found = true;
                    int minDigits = (numFracDigits == -1) ? 0 : numFracDigits;
                    int maxDigits = (numFracDigits == -1) ? 7 : numFracDigits;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    if (arg instanceof Number) {
                        double value = ((Number)arg).doubleValue();
                        buffer.append(context.formatNumber(value, minDigits, maxDigits, grouping));
                    }
                    else if (arg instanceof IntProperty) {
                        int value = ((IntProperty)arg).get();
                        buffer.append(context.formatNumber(value, minDigits, maxDigits, grouping));
                    }
                    else if (arg instanceof FloatProperty) {
                        float value = ((FloatProperty)arg).get();
                        buffer.append(context.formatNumber(value, minDigits, maxDigits, grouping));
                    }
                    else {
                        buffer.append(toString(arg));
                    }
                }
                else if (decimal && ch >= '0' && ch <= '9') {
                    numFracDigits = (numFracDigits * 10) + (ch - '0');
                }
                else {
                    buffer.append(message.substring(index, codeIndex));
                    break;
                }
            }
            if (found) {
                currentArg++;
            }
            lastIndex = codeIndex;
        }
        
        if (lastIndex < message.length()) {
            buffer.append(message.substring(lastIndex, message.length()));
        }
        
        return buffer.toString();
    }
}

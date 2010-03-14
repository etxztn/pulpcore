/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
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

package org.pulpcore.tools.res;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CoreProperties extends Properties {

    private Map<String, Boolean> requestedKeys = new HashMap<String, Boolean>();
    
    
    public boolean hasProperty(String key) {
        return getProperty(key) != null;
    }
    
    
    public String getProperty(String key) {
        String value = super.getProperty(key);
        
        requestedKeys.put(key, value != null);
        return value;
    }
    
    
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    
    public String[] getListProperty(String key) {
        return getListProperty(key, (String[])null);
    }
    
    
    public String[] getListProperty(String key, String... defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        return value.split("\\s*,\\s*");
    }
    
    
    public int getIntProperty(String key, int defaultValue) throws NumberFormatException {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ex) {
            throw new NumberFormatException("Property " + key + ": \"" + value + "\"" + 
                " is not an integer.");
        }
    }
    
    
    public int[] getIntListProperty(String key) throws NumberFormatException {
        return getIntListProperty(key, (int[])null);
    }
    
    
    public int[] getIntListProperty(String key, int... defaultValue) throws NumberFormatException {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        String[] stringList = value.split("\\s*,\\s*");
        int[] list = new int[stringList.length];
        
        for (int i = 0; i < list.length; i++) {
            try {
                list[i] = Integer.parseInt(stringList[i].trim());
            }
            catch (NumberFormatException ex) {
                throw new NumberFormatException("Property " + key + ": \"" + value + "\"" +
                    " is not an integer.");
            }
        }
        
        return list;
    }
    
    
    public float getFloatProperty(String key, float defaultValue) throws NumberFormatException {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Float.parseFloat(value.trim());
        }
        catch (NumberFormatException ex) {
            throw new NumberFormatException("Property " + key + ": \"" + value + "\"" +
                " is not a floating-point value.");
        }
    }
    
    
    public Color getColorProperty(String key, Color defaultValue) {
        String value = getProperty(key);
        if (value == null || value.equals("null")) {
            return defaultValue;
        }
        
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.startsWith("0x")) {
            value = value.substring(2);
        }
        
        try {
            int color = (int)Long.parseLong(value.trim(), 16);
            boolean hasAlpha = ((color & 0xff000000) != 0);
            return new Color(color, hasAlpha);
        }
        catch (NumberFormatException ex) {
            throw new NumberFormatException("Property " + key + ": \"" + value + "\"" + 
                " is not a color.");
        } 
    }
    
    
    public Iterator<String> getRequestedKeys() {
        return Collections.unmodifiableMap(requestedKeys).keySet().iterator();
    }
    
    
    public Iterator<String> getUnrequestedKeys() {
        
        Enumeration e = propertyNames();
        List<String> unrequestedKeys = new ArrayList<String>();
        
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            if (!requestedKeys.containsKey(key)) {
                unrequestedKeys.add(key);
            }
        }
        
        return Collections.unmodifiableList(unrequestedKeys).iterator();
       
    }
}

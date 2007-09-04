/*
    Copyright (c) 2007, Interactive Pulp, LLC
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

package pulpcore.player;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

public class EditAction extends AbstractAction {
    
    public EditAction() {
        
        String actionName = getActionName();
        
        ResourceBundle bundle = ResourceBundle.getBundle("text");
        
        try {
            setName(bundle.getString(actionName + ".name"));
        }
        catch (MissingResourceException ex) {
            setName(actionName);
        }

        try {
            String keyStroke = bundle.getString(actionName + ".accelerator");
            if ("Mac OS X".equals(System.getProperty("os.name"))) {
                keyStroke = keyStroke.replace("ctrl", "meta");
            }
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyStroke));
        }
        catch (MissingResourceException ex) { }
        
        try {
            char key = bundle.getString(actionName + ".mnemonic").charAt(0);
            putValue(MNEMONIC_KEY, new Integer(KeyStroke.getKeyStroke(key).getKeyCode()));
        }
        catch (MissingResourceException ex) { }
        
        try {
            setDescription(bundle.getString(actionName + ".description"));
        }
        catch (MissingResourceException ex) { }
        
        // Set the icon
		String iconName = "/" + actionName + ".png";
        URL iconResource = getClass().getResource(iconName);
		if (iconResource != null) {
			setIcon(new ImageIcon(iconResource));
		}
        
    }
    
    private void setIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }
    
    
    private void setName(String name) {
        putValue(NAME, name);
    }
    
    
    private void setDescription(String desc) {
        putValue(SHORT_DESCRIPTION, desc);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        // Do nothing
    }
    
    
    private String getActionName() {
        // Use the class name as the action name.
        // Example: converts "pulpcore.player.PulpCorePlayer$ReloadAction" to "Reload"
		String className = getClass().getName();
		int startIndex = 0;
		int endIndex = className.length();
        startIndex = Math.max(className.lastIndexOf('.'), 0);
		startIndex = Math.max(className.lastIndexOf('$'), startIndex);
        endIndex = Math.min(className.indexOf("Action", startIndex), endIndex);
        if (endIndex <= startIndex) {
            endIndex = className.length();
        }
		
		return className.substring(startIndex + 1, endIndex);
	}
      
}

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

package pulpcore.platform.applet;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Toolkit;
import pulpcore.CoreSystem;
import pulpcore.Input;

/**
    An input manager for Applets.
*/
public class AppletInput extends Input implements KeyListener, MouseListener,
    MouseMotionListener, MouseWheelListener, FocusListener
{
    private Component comp;
    
    private boolean[] keyPressed = new boolean[NUM_KEY_CODES];
    private boolean[] keyDown = new boolean[NUM_KEY_CODES];
    
    private int cursorCode;
    private int awtCursorCode;
    
    private Cursor invisibleCursor;
    
    private int appletMouseX = -1;
    private int appletMouseY = -1;
    private int appletMousePressX = -1;
    private int appletMousePressY = -1;
    private int appletMouseReleaseX = -1;
    private int appletMouseReleaseY = -1;
    private int appletMouseWheelX = -1;
    private int appletMouseWheelY = -1;
    private int appletMouseWheel = 0;
    private boolean appletHasKeyboardFocus;
    private boolean appletIsMouseInside;
    private int focusCountdown;
    
    private StringBuffer textInputSinceLastPoll = new StringBuffer();
    
    public AppletInput(Component comp) {
        appletHasKeyboardFocus = false;
        cursorCode = CURSOR_DEFAULT;
        
        this.comp = comp;
        comp.addKeyListener(this);
        comp.addMouseListener(this);
        comp.addMouseMotionListener(this);
        comp.addMouseWheelListener(this);
        comp.addFocusListener(this);
        comp.setFocusTraversalKeysEnabled(false);
        
        Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(32, 32);
        if (cursorSize.width <= 0 || cursorSize.height <= 0) {
            // No cursor
            invisibleCursor = null;
        }
        else {
            BufferedImage cursorImage = new BufferedImage(
                cursorSize.width, cursorSize.height, BufferedImage.TYPE_INT_ARGB);
            
            invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImage, new Point(0, 0), "invisible");
        }
        

        // This is a hack and probably won't work on all machines.
        // Firefox 2 + Windows XP + Java 5 + pulpcore.js appears to need a delay
        // before calling comp.requestFocus(). Calling it repeatedly does not focus
        // the component; there must be a delay.
        // A value of 10 tested fine; I've increased it here for slower machines.
        focusCountdown = 30;
    }
    
    
    //
    // Input implementation
    //
    
    
    protected synchronized void pollImpl() {
        
        if (focusCountdown > 0) {
            if (appletHasKeyboardFocus) {
                focusCountdown = 0;
            }
            else {
                if (comp.getWidth() > 0 && comp.getHeight() > 0) {
                    focusCountdown--;
                    if (focusCountdown == 0) {
                        requestKeyboardFocusImpl();
                    }
                }
            }
        }
        
        for (int i = 0; i < NUM_KEY_CODES; i++) {
            
            int currState = keyStates[i];
            
            if (keyPressed[i]) {
                keyPressed[i] = false;
                if (currState == RELEASED || currState == UP) {
                    keyStates[i] = PRESSED;
                }
                else {
                    keyStates[i] = REPEATED;
                }
            }
            else if (keyDown[i]) {
                if (currState == PRESSED || currState == REPEATED) {
                    keyStates[i] = DOWN;
                }
            }
            else if (currState == PRESSED || currState == DOWN || currState == REPEATED) {
                keyStates[i] = RELEASED;
            }
            else {
                keyStates[i] = UP;
            }
        }
        
        super.mouseMoving = (appletMouseX != super.mouseX || appletMouseY != super.mouseY);
        super.mouseX = appletMouseX;
        super.mouseY = appletMouseY;
        super.mousePressX = appletMousePressX;
        super.mousePressY = appletMousePressY;
        super.mouseReleaseX = appletMouseReleaseX;
        super.mouseReleaseY = appletMouseReleaseY;
        super.mouseWheelX = appletMouseWheelX;
        super.mouseWheelY = appletMouseWheelY;
        super.mouseWheel = appletMouseWheel;
        super.hasKeyboardFocus = appletHasKeyboardFocus;
        super.isMouseInside = appletIsMouseInside && 
            appletMouseX >= 0 && appletMouseY >= 0 && 
            appletMouseX < comp.getWidth() && appletMouseY < comp.getHeight();
        
        appletMouseWheel = 0;
        
        if (super.textInputMode) {
            if (textInputSinceLastPoll.length() > 0) {
                super.textInput = textInputSinceLastPoll.toString();
                textInputSinceLastPoll = new StringBuffer();
            }
            else {
                super.textInput = "";
            }
        }
    }
    
    
    protected void requestKeyboardFocusImpl() {
        comp.requestFocus();
    }
    
    
    protected synchronized void clearImpl() {
        for (int i = 0; i < NUM_KEY_CODES; i++) {
            keyPressed[i] = false;
            keyDown[i] = false;
        }
        
        textInputSinceLastPoll = new StringBuffer();
    }
    
    
    protected void setCursorImpl(int cursorCode) {
        
        int awtCursorCode;
        //if (cursorCode == CURSOR_CUSTOM) {
        //    awtCursorCode = Cursor.CUSTOM_CURSOR;
        //}
        //else {
            awtCursorCode = getAWTCursorCode(cursorCode);
        //}
        
        if (awtCursorCode == Cursor.CUSTOM_CURSOR && invisibleCursor == null) {
            awtCursorCode = Cursor.DEFAULT_CURSOR;
        }
        
        if (awtCursorCode == Cursor.DEFAULT_CURSOR) {
            cursorCode = CURSOR_DEFAULT;
        }
        
        if (this.cursorCode != cursorCode || this.awtCursorCode != awtCursorCode) {
            if (awtCursorCode == Cursor.CUSTOM_CURSOR) {
                comp.setCursor(invisibleCursor);
            }
            else {
                comp.setCursor(Cursor.getPredefinedCursor(awtCursorCode));
            }
            this.awtCursorCode = awtCursorCode;
            this.cursorCode = cursorCode;
        }
    }
    
    
    protected int getCursorImpl() {
        return cursorCode;
    }
        
        
    private int getAWTCursorCode(int cursorCode) {
        
        switch (cursorCode) {
            case CURSOR_DEFAULT:   return Cursor.DEFAULT_CURSOR; 
            case CURSOR_OFF:       return Cursor.CUSTOM_CURSOR; 
            case CURSOR_CROSSHAIR: return Cursor.CROSSHAIR_CURSOR; 
            case CURSOR_HAND:      return Cursor.HAND_CURSOR; 
            case CURSOR_MOVE:      return Cursor.MOVE_CURSOR; 
            case CURSOR_TEXT:      return Cursor.TEXT_CURSOR; 
            case CURSOR_WAIT:      return Cursor.WAIT_CURSOR; 
            case CURSOR_N_RESIZE:  return Cursor.N_RESIZE_CURSOR; 
            case CURSOR_S_RESIZE:  return Cursor.S_RESIZE_CURSOR; 
            case CURSOR_W_RESIZE:  return Cursor.W_RESIZE_CURSOR; 
            case CURSOR_E_RESIZE:  return Cursor.E_RESIZE_CURSOR; 
            case CURSOR_NW_RESIZE: return Cursor.NW_RESIZE_CURSOR; 
            case CURSOR_NE_RESIZE: return Cursor.NE_RESIZE_CURSOR; 
            case CURSOR_SW_RESIZE: return Cursor.SW_RESIZE_CURSOR; 
            case CURSOR_SE_RESIZE: return Cursor.SE_RESIZE_CURSOR; 
            default:               return Cursor.DEFAULT_CURSOR; 
        }
        
    }
    
    
    /**
        Translates the java.awt.event.KeyEvent to PulpCore's virtual key code.
    */
    private int getKeyCode(KeyEvent e) {
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE:   return KEY_BACK_SPACE;
            case KeyEvent.VK_TAB:          return KEY_TAB;
            case KeyEvent.VK_ENTER:        return KEY_ENTER;
            case KeyEvent.VK_PAUSE:        return KEY_PAUSE;
            case KeyEvent.VK_CAPS_LOCK:    return KEY_CAPS_LOCK;
            case KeyEvent.VK_ESCAPE:       return KEY_ESCAPE;
            case KeyEvent.VK_SPACE:        return KEY_SPACE;
            case KeyEvent.VK_PAGE_UP:      return KEY_PAGE_UP;
            case KeyEvent.VK_PAGE_DOWN:    return KEY_PAGE_DOWN;
            case KeyEvent.VK_END:          return KEY_END;
            case KeyEvent.VK_HOME:         return KEY_HOME;
            case KeyEvent.VK_LEFT:         return KEY_LEFT;
            case KeyEvent.VK_UP:           return KEY_UP;
            case KeyEvent.VK_RIGHT:        return KEY_RIGHT;
            case KeyEvent.VK_DOWN:         return KEY_DOWN;
            case KeyEvent.VK_PRINTSCREEN:  return KEY_PRINT_SCREEN;
            case KeyEvent.VK_INSERT:       return KEY_INSERT;
            case KeyEvent.VK_DELETE:       return KEY_DELETE;
            case KeyEvent.VK_SEMICOLON:    return KEY_SEMICOLON;
            case KeyEvent.VK_EQUALS:       return KEY_EQUALS;
            case KeyEvent.VK_COMMA:        return KEY_COMMA;
            case KeyEvent.VK_MINUS:        return KEY_MINUS;
            case KeyEvent.VK_PERIOD:       return KEY_PERIOD;
            case KeyEvent.VK_SLASH:        return KEY_SLASH;
            case KeyEvent.VK_BACK_QUOTE:   return KEY_BACK_QUOTE;
            case KeyEvent.VK_OPEN_BRACKET: return KEY_OPEN_BRACKET;
            case KeyEvent.VK_BACK_SLASH:   return KEY_BACK_SLASH;
            case KeyEvent.VK_CLOSE_BRACKET:return KEY_CLOSE_BRACKET;
            case KeyEvent.VK_QUOTE:        return KEY_QUOTE;
            case KeyEvent.VK_0:            return KEY_0;
            case KeyEvent.VK_1:            return KEY_1;
            case KeyEvent.VK_2:            return KEY_2;
            case KeyEvent.VK_3:            return KEY_3;
            case KeyEvent.VK_4:            return KEY_4;
            case KeyEvent.VK_5:            return KEY_5;
            case KeyEvent.VK_6:            return KEY_6;
            case KeyEvent.VK_7:            return KEY_7;
            case KeyEvent.VK_8:            return KEY_8;
            case KeyEvent.VK_9:            return KEY_9;
            case KeyEvent.VK_A:            return KEY_A;
            case KeyEvent.VK_B:            return KEY_B;
            case KeyEvent.VK_C:            return KEY_C;
            case KeyEvent.VK_D:            return KEY_D;
            case KeyEvent.VK_E:            return KEY_E;
            case KeyEvent.VK_F:            return KEY_F;
            case KeyEvent.VK_G:            return KEY_G;
            case KeyEvent.VK_H:            return KEY_H;
            case KeyEvent.VK_I:            return KEY_I;
            case KeyEvent.VK_J:            return KEY_J;
            case KeyEvent.VK_K:            return KEY_K;
            case KeyEvent.VK_L:            return KEY_L;
            case KeyEvent.VK_M:            return KEY_M;
            case KeyEvent.VK_N:            return KEY_N;
            case KeyEvent.VK_O:            return KEY_O;
            case KeyEvent.VK_P:            return KEY_P;
            case KeyEvent.VK_Q:            return KEY_Q;
            case KeyEvent.VK_R:            return KEY_R;
            case KeyEvent.VK_S:            return KEY_S;
            case KeyEvent.VK_T:            return KEY_T;
            case KeyEvent.VK_U:            return KEY_U;
            case KeyEvent.VK_V:            return KEY_V;
            case KeyEvent.VK_W:            return KEY_W;
            case KeyEvent.VK_X:            return KEY_X;
            case KeyEvent.VK_Y:            return KEY_Y;
            case KeyEvent.VK_Z:            return KEY_Z;
            case KeyEvent.VK_NUMPAD0:      return KEY_NUMPAD0;
            case KeyEvent.VK_NUMPAD1:      return KEY_NUMPAD1;
            case KeyEvent.VK_NUMPAD2:      return KEY_NUMPAD2;
            case KeyEvent.VK_NUMPAD3:      return KEY_NUMPAD3;
            case KeyEvent.VK_NUMPAD4:      return KEY_NUMPAD4;
            case KeyEvent.VK_NUMPAD5:      return KEY_NUMPAD5;
            case KeyEvent.VK_NUMPAD6:      return KEY_NUMPAD6;
            case KeyEvent.VK_NUMPAD7:      return KEY_NUMPAD7;
            case KeyEvent.VK_NUMPAD8:      return KEY_NUMPAD8;
            case KeyEvent.VK_NUMPAD9:      return KEY_NUMPAD9;
            case KeyEvent.VK_MULTIPLY:     return KEY_MULTIPLY;
            case KeyEvent.VK_ADD:          return KEY_ADD;
            case KeyEvent.VK_SEPARATER:    return KEY_SEPARATOR;
            case KeyEvent.VK_SUBTRACT:     return KEY_SUBTRACT;
            case KeyEvent.VK_DECIMAL:      return KEY_DECIMAL;
            case KeyEvent.VK_DIVIDE:       return KEY_DIVIDE;
            case KeyEvent.VK_F1:           return KEY_F1;
            case KeyEvent.VK_F2:           return KEY_F2;
            case KeyEvent.VK_F3:           return KEY_F3;
            case KeyEvent.VK_F4:           return KEY_F4;
            case KeyEvent.VK_F5:           return KEY_F5;
            case KeyEvent.VK_F6:           return KEY_F6;
            case KeyEvent.VK_F7:           return KEY_F7;
            case KeyEvent.VK_F8:           return KEY_F8;
            case KeyEvent.VK_F9:           return KEY_F9;
            case KeyEvent.VK_F10:          return KEY_F10;
            case KeyEvent.VK_F11:          return KEY_F11;
            case KeyEvent.VK_F12:          return KEY_F12;
            case KeyEvent.VK_F13:          return KEY_F13;
            case KeyEvent.VK_F14:          return KEY_F14;
            case KeyEvent.VK_F15:          return KEY_F15;
            case KeyEvent.VK_F16:          return KEY_F16;
            case KeyEvent.VK_F17:          return KEY_F17;
            case KeyEvent.VK_F18:          return KEY_F18;
            case KeyEvent.VK_F19:          return KEY_F19;
            case KeyEvent.VK_F20:          return KEY_F20;
            case KeyEvent.VK_F21:          return KEY_F21;
            case KeyEvent.VK_F22:          return KEY_F22;
            case KeyEvent.VK_F23:          return KEY_F23;
            case KeyEvent.VK_F24:          return KEY_F24;
            case KeyEvent.VK_NUM_LOCK:     return KEY_NUM_LOCK;
            case KeyEvent.VK_SCROLL_LOCK:  return KEY_SCROLL_LOCK;
                
            case KeyEvent.VK_SHIFT:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return KEY_RIGHT_SHIFT;
                }
                else {
                    return KEY_LEFT_SHIFT;
                }
                
            case KeyEvent.VK_CONTROL:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return KEY_RIGHT_CONTROL;
                }
                else {
                    return KEY_LEFT_CONTROL;
                }
                
            case KeyEvent.VK_ALT:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return KEY_RIGHT_ALT;
                }
                else {
                    return KEY_LEFT_ALT;
                }
            
            case KeyEvent.VK_META:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return KEY_RIGHT_META;
                }
                else {
                    return KEY_LEFT_META;
                }
        }
        
     
        // Unknown key code
        return -1;
    }
    
    
    private int getKeyCode(MouseEvent e) {
        switch (e.getButton()) {
            default: case MouseEvent.BUTTON1:
                return KEY_MOUSE_BUTTON_1;
            case MouseEvent.BUTTON2:
                return KEY_MOUSE_BUTTON_2;
            case MouseEvent.BUTTON3:
                return KEY_MOUSE_BUTTON_3;
        }
    }
    
    
    private synchronized void keyEvent(int keyCode, boolean pressed) {
        if (keyCode == -1) {
            return;
        }
        
        if (pressed) {
            keyPressed[keyCode] = true;
            keyDown[keyCode] = true;
        }
        else {
            keyDown[keyCode] = false;
        }
    }
    
    
    //
    // KeyListener interface
    //
    
    
    public void keyPressed(KeyEvent e) {
        
        //pulpcore.CoreSystem.print(KeyEvent.getKeyText(e.getKeyCode()) + " = " + e.getKeyCode());
        
        int keyCode = getKeyCode(e);
        keyEvent(keyCode, true);
        e.consume();
        
        if (keyCode == KEY_LEFT_META || keyCode == KEY_RIGHT_META) {
            // On Mac OS X, press and release events are not sent if the meta key is down
            // Release all keys
            for (int i = 0; i < NUM_KEY_CODES; i++) {
                keyDown[i] = false;
            }
        }
    }


    public void keyReleased(KeyEvent e) {
        keyEvent(getKeyCode(e), false);
        e.consume();
    }


    public void keyTyped(KeyEvent e) {
        if (e.isMetaDown()) {
            // On Mac OS X, press and release events are not sent if the meta key is down
            // Press and release this key
            int keyCode = getKeyCode(e);
            keyEvent(keyCode, true);
            keyEvent(keyCode, false);
        }
        else if (super.textInputMode) {
            synchronized (this) {
                char ch = e.getKeyChar();
                if (ch == KeyEvent.VK_BACK_SPACE) {
                    if (textInputSinceLastPoll.length() > 0) {
                        textInputSinceLastPoll.setLength(textInputSinceLastPoll.length() - 1);
                    }
                }
                else if (ch != KeyEvent.VK_DELETE && ch != KeyEvent.CHAR_UNDEFINED && 
                    !e.isActionKey() && ch >= ' ')
                { 
                    textInputSinceLastPoll.append(ch);
                }
            }
        }
        
        e.consume();
    }
    
    
    //
    // MouseListener
    //
    
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (appletHasKeyboardFocus == false) {
                comp.requestFocus();
            }
            keyEvent(getKeyCode(e), true);
        
            appletMouseX = e.getX();
            appletMouseY = e.getY();
            appletMousePressX = appletMouseX;
            appletMousePressY = appletMouseY;
            appletIsMouseInside = true;
        }
    }


    public void mouseReleased(MouseEvent e) {
        synchronized (this) {
            keyEvent(getKeyCode(e), false);
            
            appletMouseReleaseX = e.getX();
            appletMouseReleaseY = e.getY();
            appletIsMouseInside = true;
        } 
        
        // Attempt to fix mouse cursor bug. In Scared, the crosshair cursor was reverting to 
        // the default mouse cursor on mouse release.
        // Commented out because the bug is hard to duplicate. Perhaps it only occured on
        // Firefox on Mac OS X? And only "sometimes"?
        /*
        if (awtCursorCode != Cursor.DEFAULT_CURSOR) { 
            comp.setCursor(null);
            comp.setCursor(Cursor.getPredefinedCursor(awtCursorCode));
        }
        */
    }


    public void mouseClicked(MouseEvent e) {
        
        int clickCount = e.getClickCount();
        
        if (clickCount <= 1) {
            return;
        }
        
        // Detect double- and triple-clicks
        synchronized (this) {
            int keyCode;
            if (clickCount == 2) {
                 keyCode = KEY_DOUBLE_MOUSE_BUTTON_1;
            }
            else {
                 keyCode = KEY_TRIPLE_MOUSE_BUTTON_1;
            }
            
            switch (e.getButton()) {
                case MouseEvent.BUTTON2: keyCode += 1; break;
                case MouseEvent.BUTTON3: keyCode += 2; break;
            }
            
            keyPressed[keyCode] = true;
            keyDown[keyCode] = false;
            
            appletMouseReleaseX = e.getX();
            appletMouseReleaseY = e.getY();
            appletIsMouseInside = true;
        }
    }


    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);
    }


    public void mouseExited(MouseEvent e) {
        mouseMoved(e);
        appletIsMouseInside = false;
    }


    //
    // MouseMouseListener
    //
    
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }


    public void mouseMoved(MouseEvent e) {
        
        synchronized (this) {
            appletMouseX = e.getX();
            appletMouseY = e.getY();
            appletIsMouseInside = true;
            /*
            boolean mouseInside = comp.contains(mouseX, mouseY);
            if (!mouseInside) {
                // Send Release events - the app doesn't receive release
                // events if the mouse is released outside the component.
                keyEvent(VK_MOUSE_BUTTON_1, false);
                keyEvent(VK_MOUSE_BUTTON_2, false);
                keyEvent(VK_MOUSE_BUTTON_3, false);
            }
            */
        }

    }
    
    
    //
    // MouseWheelListener
    //
    
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        synchronized (this) {
            appletMouseWheelX = e.getX();
            appletMouseWheelY = e.getY();
            
            int rotation = e.getWheelRotation();
            appletMouseWheel += (rotation < 0) ? -1 : (rotation > 0) ? 1 : 0;
            appletIsMouseInside = true;
        }
    }

    
    //
    // FocusListener
    //
    
    public void focusGained(FocusEvent e) {
        appletHasKeyboardFocus = true;
    }
    
    
    public void focusLost(FocusEvent e) {
        appletHasKeyboardFocus = false;
        clearImpl();
    }

}
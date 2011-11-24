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
package org.pulpcore.runtime.applet;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.pulpcore.runtime.Context;
import org.pulpcore.runtime.Input;

public class AppletInput extends Input implements KeyListener, MouseListener,
    MouseMotionListener, MouseWheelListener, FocusListener {
    
  private static int getAWTCursorCode(int cursorCode) {
        switch (cursorCode) {
            default:               return Cursor.DEFAULT_CURSOR;
            case Input.CURSOR_DEFAULT:   return Cursor.DEFAULT_CURSOR;
            case Input.CURSOR_OFF:       return Cursor.CUSTOM_CURSOR;
            case Input.CURSOR_HAND:      return Cursor.HAND_CURSOR;
            case Input.CURSOR_CROSSHAIR: return Cursor.CROSSHAIR_CURSOR;
            case Input.CURSOR_MOVE:      return Cursor.MOVE_CURSOR;
            case Input.CURSOR_TEXT:      return Cursor.TEXT_CURSOR;
            case Input.CURSOR_WAIT:      return Cursor.WAIT_CURSOR;
            case Input.CURSOR_N_RESIZE:  return Cursor.N_RESIZE_CURSOR;
            case Input.CURSOR_S_RESIZE:  return Cursor.S_RESIZE_CURSOR;
            case Input.CURSOR_W_RESIZE:  return Cursor.W_RESIZE_CURSOR;
            case Input.CURSOR_E_RESIZE:  return Cursor.E_RESIZE_CURSOR;
            case Input.CURSOR_NW_RESIZE: return Cursor.NW_RESIZE_CURSOR;
            case Input.CURSOR_NE_RESIZE: return Cursor.NE_RESIZE_CURSOR;
            case Input.CURSOR_SW_RESIZE: return Cursor.SW_RESIZE_CURSOR;
            case Input.CURSOR_SE_RESIZE: return Cursor.SE_RESIZE_CURSOR;
        }
    }

    private static int getMouseButton(java.awt.event.MouseEvent e) {
        switch (e.getButton()) {
            default: case java.awt.event.MouseEvent.NOBUTTON:
                return -1;
            case java.awt.event.MouseEvent.BUTTON1:
                return 0;
            case java.awt.event.MouseEvent.BUTTON2:
                return 1;
            case java.awt.event.MouseEvent.BUTTON3:
                return 2;
        }
    }

    /**
        Translates the java.awt.event.KeyEvent to PulpCore's virtual key code.
    */
    private static int getKeyCode(java.awt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
            case java.awt.event.KeyEvent.VK_BACK_SPACE:   return Input.KEY_BACK_SPACE;
            case java.awt.event.KeyEvent.VK_TAB:          return Input.KEY_TAB;
            case java.awt.event.KeyEvent.VK_ENTER:        return Input.KEY_ENTER;
            case java.awt.event.KeyEvent.VK_PAUSE:        return Input.KEY_PAUSE;
            case java.awt.event.KeyEvent.VK_CAPS_LOCK:    return Input.KEY_CAPS_LOCK;
            case java.awt.event.KeyEvent.VK_ESCAPE:       return Input.KEY_ESCAPE;
            case java.awt.event.KeyEvent.VK_SPACE:        return Input.KEY_SPACE;
            case java.awt.event.KeyEvent.VK_PAGE_UP:      return Input.KEY_PAGE_UP;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN:    return Input.KEY_PAGE_DOWN;
            case java.awt.event.KeyEvent.VK_END:          return Input.KEY_END;
            case java.awt.event.KeyEvent.VK_HOME:         return Input.KEY_HOME;
            case java.awt.event.KeyEvent.VK_LEFT:         return Input.KEY_LEFT;
            case java.awt.event.KeyEvent.VK_UP:           return Input.KEY_UP;
            case java.awt.event.KeyEvent.VK_RIGHT:        return Input.KEY_RIGHT;
            case java.awt.event.KeyEvent.VK_DOWN:         return Input.KEY_DOWN;
            case java.awt.event.KeyEvent.VK_PRINTSCREEN:  return Input.KEY_PRINT_SCREEN;
            case java.awt.event.KeyEvent.VK_INSERT:       return Input.KEY_INSERT;
            case java.awt.event.KeyEvent.VK_DELETE:       return Input.KEY_DELETE;
            case java.awt.event.KeyEvent.VK_SEMICOLON:    return Input.KEY_SEMICOLON;
            case java.awt.event.KeyEvent.VK_EQUALS:       return Input.KEY_EQUALS;
            case java.awt.event.KeyEvent.VK_COMMA:        return Input.KEY_COMMA;
            case java.awt.event.KeyEvent.VK_MINUS:        return Input.KEY_MINUS;
            case java.awt.event.KeyEvent.VK_PERIOD:       return Input.KEY_PERIOD;
            case java.awt.event.KeyEvent.VK_SLASH:        return Input.KEY_SLASH;
            case java.awt.event.KeyEvent.VK_BACK_QUOTE:   return Input.KEY_BACK_QUOTE;
            case java.awt.event.KeyEvent.VK_OPEN_BRACKET: return Input.KEY_OPEN_BRACKET;
            case java.awt.event.KeyEvent.VK_BACK_SLASH:   return Input.KEY_BACK_SLASH;
            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET:return Input.KEY_CLOSE_BRACKET;
            case java.awt.event.KeyEvent.VK_QUOTE:        return Input.KEY_QUOTE;
            case java.awt.event.KeyEvent.VK_0:            return Input.KEY_0;
            case java.awt.event.KeyEvent.VK_1:            return Input.KEY_1;
            case java.awt.event.KeyEvent.VK_2:            return Input.KEY_2;
            case java.awt.event.KeyEvent.VK_3:            return Input.KEY_3;
            case java.awt.event.KeyEvent.VK_4:            return Input.KEY_4;
            case java.awt.event.KeyEvent.VK_5:            return Input.KEY_5;
            case java.awt.event.KeyEvent.VK_6:            return Input.KEY_6;
            case java.awt.event.KeyEvent.VK_7:            return Input.KEY_7;
            case java.awt.event.KeyEvent.VK_8:            return Input.KEY_8;
            case java.awt.event.KeyEvent.VK_9:            return Input.KEY_9;
            case java.awt.event.KeyEvent.VK_A:            return Input.KEY_A;
            case java.awt.event.KeyEvent.VK_B:            return Input.KEY_B;
            case java.awt.event.KeyEvent.VK_C:            return Input.KEY_C;
            case java.awt.event.KeyEvent.VK_D:            return Input.KEY_D;
            case java.awt.event.KeyEvent.VK_E:            return Input.KEY_E;
            case java.awt.event.KeyEvent.VK_F:            return Input.KEY_F;
            case java.awt.event.KeyEvent.VK_G:            return Input.KEY_G;
            case java.awt.event.KeyEvent.VK_H:            return Input.KEY_H;
            case java.awt.event.KeyEvent.VK_I:            return Input.KEY_I;
            case java.awt.event.KeyEvent.VK_J:            return Input.KEY_J;
            case java.awt.event.KeyEvent.VK_K:            return Input.KEY_K;
            case java.awt.event.KeyEvent.VK_L:            return Input.KEY_L;
            case java.awt.event.KeyEvent.VK_M:            return Input.KEY_M;
            case java.awt.event.KeyEvent.VK_N:            return Input.KEY_N;
            case java.awt.event.KeyEvent.VK_O:            return Input.KEY_O;
            case java.awt.event.KeyEvent.VK_P:            return Input.KEY_P;
            case java.awt.event.KeyEvent.VK_Q:            return Input.KEY_Q;
            case java.awt.event.KeyEvent.VK_R:            return Input.KEY_R;
            case java.awt.event.KeyEvent.VK_S:            return Input.KEY_S;
            case java.awt.event.KeyEvent.VK_T:            return Input.KEY_T;
            case java.awt.event.KeyEvent.VK_U:            return Input.KEY_U;
            case java.awt.event.KeyEvent.VK_V:            return Input.KEY_V;
            case java.awt.event.KeyEvent.VK_W:            return Input.KEY_W;
            case java.awt.event.KeyEvent.VK_X:            return Input.KEY_X;
            case java.awt.event.KeyEvent.VK_Y:            return Input.KEY_Y;
            case java.awt.event.KeyEvent.VK_Z:            return Input.KEY_Z;
            case java.awt.event.KeyEvent.VK_NUMPAD0:      return Input.KEY_NUMPAD0;
            case java.awt.event.KeyEvent.VK_NUMPAD1:      return Input.KEY_NUMPAD1;
            case java.awt.event.KeyEvent.VK_NUMPAD2:      return Input.KEY_NUMPAD2;
            case java.awt.event.KeyEvent.VK_NUMPAD3:      return Input.KEY_NUMPAD3;
            case java.awt.event.KeyEvent.VK_NUMPAD4:      return Input.KEY_NUMPAD4;
            case java.awt.event.KeyEvent.VK_NUMPAD5:      return Input.KEY_NUMPAD5;
            case java.awt.event.KeyEvent.VK_NUMPAD6:      return Input.KEY_NUMPAD6;
            case java.awt.event.KeyEvent.VK_NUMPAD7:      return Input.KEY_NUMPAD7;
            case java.awt.event.KeyEvent.VK_NUMPAD8:      return Input.KEY_NUMPAD8;
            case java.awt.event.KeyEvent.VK_NUMPAD9:      return Input.KEY_NUMPAD9;
            case java.awt.event.KeyEvent.VK_MULTIPLY:     return Input.KEY_MULTIPLY;
            case java.awt.event.KeyEvent.VK_ADD:          return Input.KEY_ADD;
            case java.awt.event.KeyEvent.VK_SEPARATER:    return Input.KEY_SEPARATOR;
            case java.awt.event.KeyEvent.VK_SUBTRACT:     return Input.KEY_SUBTRACT;
            case java.awt.event.KeyEvent.VK_DECIMAL:      return Input.KEY_DECIMAL;
            case java.awt.event.KeyEvent.VK_DIVIDE:       return Input.KEY_DIVIDE;
            case java.awt.event.KeyEvent.VK_F1:           return Input.KEY_F1;
            case java.awt.event.KeyEvent.VK_F2:           return Input.KEY_F2;
            case java.awt.event.KeyEvent.VK_F3:           return Input.KEY_F3;
            case java.awt.event.KeyEvent.VK_F4:           return Input.KEY_F4;
            case java.awt.event.KeyEvent.VK_F5:           return Input.KEY_F5;
            case java.awt.event.KeyEvent.VK_F6:           return Input.KEY_F6;
            case java.awt.event.KeyEvent.VK_F7:           return Input.KEY_F7;
            case java.awt.event.KeyEvent.VK_F8:           return Input.KEY_F8;
            case java.awt.event.KeyEvent.VK_F9:           return Input.KEY_F9;
            case java.awt.event.KeyEvent.VK_F10:          return Input.KEY_F10;
            case java.awt.event.KeyEvent.VK_F11:          return Input.KEY_F11;
            case java.awt.event.KeyEvent.VK_F12:          return Input.KEY_F12;
            case java.awt.event.KeyEvent.VK_F13:          return Input.KEY_F13;
            case java.awt.event.KeyEvent.VK_F14:          return Input.KEY_F14;
            case java.awt.event.KeyEvent.VK_F15:          return Input.KEY_F15;
            case java.awt.event.KeyEvent.VK_F16:          return Input.KEY_F16;
            case java.awt.event.KeyEvent.VK_F17:          return Input.KEY_F17;
            case java.awt.event.KeyEvent.VK_F18:          return Input.KEY_F18;
            case java.awt.event.KeyEvent.VK_F19:          return Input.KEY_F19;
            case java.awt.event.KeyEvent.VK_F20:          return Input.KEY_F20;
            case java.awt.event.KeyEvent.VK_F21:          return Input.KEY_F21;
            case java.awt.event.KeyEvent.VK_F22:          return Input.KEY_F22;
            case java.awt.event.KeyEvent.VK_F23:          return Input.KEY_F23;
            case java.awt.event.KeyEvent.VK_F24:          return Input.KEY_F24;
            case java.awt.event.KeyEvent.VK_NUM_LOCK:     return Input.KEY_NUM_LOCK;
            case java.awt.event.KeyEvent.VK_SCROLL_LOCK:  return Input.KEY_SCROLL_LOCK;

            case java.awt.event.KeyEvent.VK_SHIFT:
                if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_SHIFT;
                }
                else {
                    return Input.KEY_LEFT_SHIFT;
                }

            case java.awt.event.KeyEvent.VK_CONTROL:
                if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_CONTROL;
                }
                else {
                    return Input.KEY_LEFT_CONTROL;
                }

            case java.awt.event.KeyEvent.VK_ALT:
                if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_ALT;
                }
                else {
                    return Input.KEY_LEFT_ALT;
                }

            case java.awt.event.KeyEvent.VK_META:
                if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_META;
                }
                else {
                    return Input.KEY_LEFT_META;
                }
        }

        // Unknown key code
        return -1;
    }

    private final Object lock = new Object();
    private final Context context;
    private final Component inputComponent;
    private final Cursor invisibleCursor;
    private ArrayList<Event> events = new ArrayList<Event>();
    private boolean appletHasKeyboardFocus = false;
    private int awtCursorCode;
    private int focusCountdown;

    public AppletInput(Context context, Component inputComponent) {
        this.context = context;
        this.inputComponent = inputComponent;
        
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

        inputComponent.addKeyListener(this);
        inputComponent.addMouseListener(this);
        inputComponent.addMouseMotionListener(this);
        inputComponent.addMouseWheelListener(this);
        inputComponent.addFocusListener(this);
        inputComponent.setFocusTraversalKeysEnabled(false);

        // This is a hack and probably won't work on all machines.
        // Firefox 2 + Windows XP + Java 5 + pulpcore.js appears to need a delay
        // before calling comp.requestFocus(). Calling it repeatedly does not focus
        // the component; there must be a delay.
        // A value of 10 tested fine; I've increased it here for slower machines.
        focusCountdown = 30;
    }

    @Override
    public void setCursor(int cursorCode) {
        int newAwtCursorCode = getAWTCursorCode(cursorCode);
        if (newAwtCursorCode == Cursor.CUSTOM_CURSOR && invisibleCursor == null) {
            newAwtCursorCode = Cursor.DEFAULT_CURSOR;
        }

        if (newAwtCursorCode == Cursor.DEFAULT_CURSOR) {
            cursorCode = Input.CURSOR_DEFAULT;
        }

        if (getCursor() != cursorCode || this.awtCursorCode != newAwtCursorCode) {
            if (newAwtCursorCode == Cursor.CUSTOM_CURSOR) {
                inputComponent.setCursor(invisibleCursor);
            }
            else {
                inputComponent.setCursor(Cursor.getPredefinedCursor(newAwtCursorCode));
            }
            this.awtCursorCode = newAwtCursorCode;
            super.setCursor(cursorCode);
        }
    }

    @Override
    public boolean hasMouse() {
        return true;
    }
    
    @Override
    public boolean hasKeyboardFocus() {
        return appletHasKeyboardFocus;
    }

    private void addEvent(Event event) {
        synchronized (lock) {
            events.add(event);
        }
    }

    @Override
    public List<Event> consumeEvents() {
        if (focusCountdown > 0) {
            if (appletHasKeyboardFocus) {
                focusCountdown = 0;
            }
            else {
                if (inputComponent.getWidth() > 0 && inputComponent.getHeight() > 0) {
                    focusCountdown--;
                    if (focusCountdown == 0) {
                        inputComponent.requestFocus();
                    }
                }
            }
        }
        
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            synchronized (lock) {
                List<Event> list = events;
                events = new ArrayList<Event>();
                return list;
            }
        }
    }
    
    public void keyTyped(java.awt.event.KeyEvent e) {
        addEvent(new KeyEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                KeyEvent.Type.TYPED, getKeyCode(e), e.getKeyChar()));
        e.consume();
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        addEvent(new KeyEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                KeyEvent.Type.PRESSED, getKeyCode(e), e.getKeyChar()));
        e.consume();
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        addEvent(new KeyEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                KeyEvent.Type.RELEASED, getKeyCode(e), e.getKeyChar()));
        e.consume();
    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
        addEvent(new TouchEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                TouchEvent.Type.CLICK, e.getX(), e.getY(), getMouseButton(e)));
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
        if (appletHasKeyboardFocus == false) {
            inputComponent.requestFocus();
        }
        addEvent(new TouchEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                TouchEvent.Type.PRESS, e.getX(), e.getY(), getMouseButton(e)));
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
         addEvent(new TouchEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                TouchEvent.Type.RELEASE, e.getX(), e.getY(), getMouseButton(e)));
   }

    public void mouseEntered(java.awt.event.MouseEvent e) {
        // Ignore it
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
        // Ignore it
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
        addEvent(new TouchEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                TouchEvent.Type.DRAG, e.getX(), e.getY(), getMouseButton(e)));
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {
        addEvent(new TouchEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                TouchEvent.Type.MOVE, e.getX(), e.getY(), getMouseButton(e)));
    }

    public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
        int scrollX = 0;
        int scrollY = 0;
        if (e.isShiftDown()) {
            scrollX = e.getUnitsToScroll();
        }
        else {
            scrollY = e.getUnitsToScroll();
        }

        addEvent(new TouchScrollEvent(e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(),
                e.getX(), e.getY(), scrollX, scrollY));
    }

    public void focusGained(java.awt.event.FocusEvent e) {
        synchronized (context) {
            appletHasKeyboardFocus = true;
        }
    }

    public void focusLost(java.awt.event.FocusEvent e) {
        synchronized (context) {
            appletHasKeyboardFocus = false;
        }
    }
}

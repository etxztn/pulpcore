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
package org.pulpcore.runtime;

import java.util.List;
import org.pulpcore.view.View;

public abstract class Input {

    public static final int CURSOR_DEFAULT   = 0;
    public static final int CURSOR_OFF       = 1;
    public static final int CURSOR_HAND      = 2;
    public static final int CURSOR_CROSSHAIR = 3;
    public static final int CURSOR_MOVE      = 4;
    public static final int CURSOR_TEXT      = 5;
    public static final int CURSOR_WAIT      = 6;
    public static final int CURSOR_N_RESIZE  = 7;
    public static final int CURSOR_S_RESIZE  = 8;
    public static final int CURSOR_W_RESIZE  = 9;
    public static final int CURSOR_E_RESIZE  = 10;
    public static final int CURSOR_NW_RESIZE = 11;
    public static final int CURSOR_NE_RESIZE = 12;
    public static final int CURSOR_SW_RESIZE = 13;
    public static final int CURSOR_SE_RESIZE = 14;

    // Same as Windows virtual key codes
    // Modifier keys (ctrl, alt, shift) are only represented by the left/right codes.

    //public static final int                  = 0x01;
    //public static final int                  = 0x02;
    //public static final int                  = 0x03;
    //public static final int                  = 0x04;
    //public static final int                  = 0x05;
    //public static final int                  = 0x06;
    //public static final int                  = 0x07;
    public static final int KEY_BACK_SPACE      = 0x08;
    public static final int KEY_TAB             = 0x09;
    //public static final int                  = 0x0a;
    //public static final int                  = 0x0b;
    //public static final int                  = 0x0c;
    public static final int KEY_ENTER           = 0x0d;
    //public static final int                  = 0x0e;
    //public static final int                  = 0x0f;
    //public static final int                  = 0x10;
    //public static final int                  = 0x11;
    //public static final int                  = 0x12;
    public static final int KEY_PAUSE           = 0x13;
    public static final int KEY_CAPS_LOCK       = 0x14;
    //public static final int                  = 0x15;
    //public static final int                  = 0x16;
    //public static final int                  = 0x17;
    //public static final int                  = 0x18;
    //public static final int                  = 0x19;
    //public static final int                  = 0x1a;
    public static final int KEY_ESCAPE          = 0x1b;
    //public static final int                  = 0x1c;
    //public static final int                  = 0x1d;
    //public static final int                  = 0x1e;
    //public static final int                  = 0x1f;
    public static final int KEY_SPACE           = 0x20;
    public static final int KEY_PAGE_UP         = 0x21;
    public static final int KEY_PAGE_DOWN       = 0x22;
    public static final int KEY_END             = 0x23;
    public static final int KEY_HOME            = 0x24;
    public static final int KEY_LEFT            = 0x25;
    public static final int KEY_UP              = 0x26;
    public static final int KEY_RIGHT           = 0x27;
    public static final int KEY_DOWN            = 0x28;
    //public static final int                  = 0x29;
    //public static final int                  = 0x2a;
    //public static final int                  = 0x2b;
    public static final int KEY_PRINT_SCREEN    = 0x2c;
    public static final int KEY_INSERT          = 0x2d;
    public static final int KEY_DELETE          = 0x2e;
    //public static final int                  = 0x2f;
    public static final int KEY_0               = 0x30;
    public static final int KEY_1               = 0x31;
    public static final int KEY_2               = 0x32;
    public static final int KEY_3               = 0x33;
    public static final int KEY_4               = 0x34;
    public static final int KEY_5               = 0x35;
    public static final int KEY_6               = 0x36;
    public static final int KEY_7               = 0x37;
    public static final int KEY_8               = 0x38;
    public static final int KEY_9               = 0x39;
    //public static final int                  = 0x3a;
    //public static final int                  = 0x3b;
    //public static final int                  = 0x3c;
    //public static final int                  = 0x3d;
    //public static final int                  = 0x3e;
    //public static final int                  = 0x3f;
    //public static final int                  = 0x40;
    public static final int KEY_A               = 0x41;
    public static final int KEY_B               = 0x42;
    public static final int KEY_C               = 0x43;
    public static final int KEY_D               = 0x44;
    public static final int KEY_E               = 0x45;
    public static final int KEY_F               = 0x46;
    public static final int KEY_G               = 0x47;
    public static final int KEY_H               = 0x48;
    public static final int KEY_I               = 0x49;
    public static final int KEY_J               = 0x4a;
    public static final int KEY_K               = 0x4b;
    public static final int KEY_L               = 0x4c;
    public static final int KEY_M               = 0x4d;
    public static final int KEY_N               = 0x4e;
    public static final int KEY_O               = 0x4f;
    public static final int KEY_P               = 0x50;
    public static final int KEY_Q               = 0x51;
    public static final int KEY_R               = 0x52;
    public static final int KEY_S               = 0x53;
    public static final int KEY_T               = 0x54;
    public static final int KEY_U               = 0x55;
    public static final int KEY_V               = 0x56;
    public static final int KEY_W               = 0x57;
    public static final int KEY_X               = 0x58;
    public static final int KEY_Y               = 0x59;
    public static final int KEY_Z               = 0x5a;
    public static final int KEY_LEFT_META       = 0x5b; // Left Windows key
    public static final int KEY_RIGHT_META      = 0x5c; // Right Windows key
    //public static final int                  = 0x5d; // Windows App key
    //public static final int                  = 0x5e;
    //public static final int                  = 0x5f;
    public static final int KEY_NUMPAD0         = 0x60;
    public static final int KEY_NUMPAD1         = 0x61;
    public static final int KEY_NUMPAD2         = 0x62;
    public static final int KEY_NUMPAD3         = 0x63;
    public static final int KEY_NUMPAD4         = 0x64;
    public static final int KEY_NUMPAD5         = 0x65;
    public static final int KEY_NUMPAD6         = 0x66;
    public static final int KEY_NUMPAD7         = 0x67;
    public static final int KEY_NUMPAD8         = 0x68;
    public static final int KEY_NUMPAD9         = 0x69;
    public static final int KEY_MULTIPLY        = 0x6a;
    public static final int KEY_ADD             = 0x6b;
    public static final int KEY_SEPARATOR       = 0x6c;
    public static final int KEY_SUBTRACT        = 0x6d;
    public static final int KEY_DECIMAL         = 0x6e;
    public static final int KEY_DIVIDE          = 0x6f;
    public static final int KEY_F1              = 0x70;
    public static final int KEY_F2              = 0x71;
    public static final int KEY_F3              = 0x72;
    public static final int KEY_F4              = 0x73;
    public static final int KEY_F5              = 0x74;
    public static final int KEY_F6              = 0x75;
    public static final int KEY_F7              = 0x76;
    public static final int KEY_F8              = 0x77;
    public static final int KEY_F9              = 0x78;
    public static final int KEY_F10             = 0x79;
    public static final int KEY_F11             = 0x7a;
    public static final int KEY_F12             = 0x7b;
    public static final int KEY_F13             = 0x7c;
    public static final int KEY_F14             = 0x7d;
    public static final int KEY_F15             = 0x7e;
    public static final int KEY_F16             = 0x7f;
    public static final int KEY_F17             = 0x80;
    public static final int KEY_F18             = 0x81;
    public static final int KEY_F19             = 0x82;
    public static final int KEY_F20             = 0x83;
    public static final int KEY_F21             = 0x84;
    public static final int KEY_F22             = 0x85;
    public static final int KEY_F23             = 0x86;
    public static final int KEY_F24             = 0x87;
    //public static final int                  = 0x88;
    //public static final int                  = 0x89;
    //public static final int                  = 0x8a;
    //public static final int                  = 0x8b;
    //public static final int                  = 0x8c;
    //public static final int                  = 0x8d;
    //public static final int                  = 0x8e;
    //public static final int                  = 0x8f;
    public static final int KEY_NUM_LOCK        = 0x90;
    public static final int KEY_SCROLL_LOCK     = 0x91;
    //public static final int                  = 0x92;
    //public static final int                  = 0x93;
    //public static final int                  = 0x94;
    //public static final int                  = 0x95;
    //public static final int                  = 0x96;
    //public static final int                  = 0x97;
    //public static final int                  = 0x98;
    //public static final int                  = 0x99;
    //public static final int                  = 0x9a;
    //public static final int                  = 0x9b;
    //public static final int                  = 0x9c;
    //public static final int                  = 0x9d;
    //public static final int                  = 0x9e;
    //public static final int                  = 0x9f;
    public static final int KEY_LEFT_SHIFT      = 0xa0;
    public static final int KEY_RIGHT_SHIFT     = 0xa1;
    public static final int KEY_LEFT_CONTROL    = 0xa2;
    public static final int KEY_RIGHT_CONTROL   = 0xa3;
    public static final int KEY_LEFT_ALT        = 0xa4;
    public static final int KEY_RIGHT_ALT       = 0xa5;
    //public static final int                  = 0xa6;
    //public static final int                  = 0xa7;
    //public static final int                  = 0xa8;
    //public static final int                  = 0xa9;
    //public static final int                  = 0xaa;
    //public static final int                  = 0xab;
    //public static final int                  = 0xac;
    //public static final int                  = 0xad;
    //public static final int                  = 0xae;
    //public static final int                  = 0xaf;
    //public static final int                  = 0xb0;
    //public static final int                  = 0xb1;
    //public static final int                  = 0xb2;
    //public static final int                  = 0xb3;
    //public static final int                  = 0xb4;
    //public static final int                  = 0xb5;
    //public static final int                  = 0xb6;
    //public static final int                  = 0xb7;
    //public static final int                  = 0xb8;
    //public static final int                  = 0xb9;
    public static final int KEY_SEMICOLON       = 0xba;
    public static final int KEY_EQUALS          = 0xbb;
    public static final int KEY_COMMA           = 0xbc;
    public static final int KEY_MINUS           = 0xbd;
    public static final int KEY_PERIOD          = 0xbe;
    public static final int KEY_SLASH           = 0xbf;
    public static final int KEY_BACK_QUOTE      = 0xc0;
    //public static final int                  = 0xc1;
    //public static final int                  = 0xc2;
    //public static final int                  = 0xc3;
    //public static final int                  = 0xc4;
    //public static final int                  = 0xc5;
    //public static final int                  = 0xc6;
    //public static final int                  = 0xc7;
    //public static final int                  = 0xc8;
    //public static final int                  = 0xc9;
    //public static final int                  = 0xca;
    //public static final int                  = 0xcb;
    //public static final int                  = 0xcc;
    //public static final int                  = 0xcd;
    //public static final int                  = 0xce;
    //public static final int                  = 0xcf;
    //public static final int                  = 0xd0;
    //public static final int                  = 0xd1;
    //public static final int                  = 0xd2;
    //public static final int                  = 0xd3;
    //public static final int                  = 0xd4;
    //public static final int                  = 0xd5;
    //public static final int                  = 0xd6;
    //public static final int                  = 0xd7;
    //public static final int                  = 0xd8;
    //public static final int                  = 0xd9;
    //public static final int                  = 0xda;
    public static final int KEY_OPEN_BRACKET    = 0xdb;
    public static final int KEY_BACK_SLASH      = 0xdc;
    public static final int KEY_CLOSE_BRACKET   = 0xdd;
    public static final int KEY_QUOTE           = 0xde;

    public static final int NUM_KEY_CODES      = 0x100;

    public static class Event {

        private final boolean ctrl;
        private final boolean shift;
        private final boolean alt;
        private final boolean meta;
        private boolean propagate;

        public Event(boolean ctrl, boolean shift, boolean alt, boolean meta) {
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
            this.meta = meta;
            this.propagate = true;
        }

        public boolean isControlDown() {
            return ctrl;
        }

        public boolean isShiftDown() {
            return shift;
        }

        public boolean isAltDown() {
            return alt;
        }

        public boolean isMetaDown() {
            return meta;
        }

        /**
         If true, this event will propagate to superviews.
         */
        public boolean shouldPropagate() {
            return propagate;
        }

        /**
         Tells this event to stop propagation to superviews.
         */
        public void stopPropagation() {
            this.propagate = false;
        }
    }

    public static class KeyEvent extends Event {

        public enum Type {
            PRESSED,
            RELEASED,
            TYPED
        }

        private final Type type;
        private final int keyCode;
        private final char ch;

        public KeyEvent(boolean ctrl, boolean shift, boolean alt, boolean meta, Type type, int keyCode, char ch) {
            super(ctrl, shift, alt, meta);
            this.type = type;
            this.keyCode = keyCode;
            this.ch = ch;
        }

        public Type getType() {
            return type;
        }

        public char getChar() {
            return ch;
        }

        public int getKeyCode() {
            return keyCode;
        }
    }

    public static class TouchEvent extends Event {

        public enum Type {
            MOVE,
            DRAG,
            PRESS,
            RELEASE,
            CLICK,
        }

        private final Type type;
        private final int x;
        private final int y;
        private final int button;
        
        public TouchEvent(boolean ctrl, boolean shift, boolean alt, boolean meta,
                Type type, int x, int y, int button) {
            super(ctrl, shift, alt, meta);
            this.type = type;
            this.x = x;
            this.y = y;
            this.button = button;
        }

        public int getButton() {
            return button;
        }

        public Type getType() {
            return type;
        }

        /**
         Gets this Touch's x coordinate in device coordinates.
         */
        public int getX() {
            return x;
        }

        /**
         Gets this Touch's y coordinate in device coordinates.
         */
        public int getY() {
            return y;
        }

        /**
         Gets this Touch's x location in the coordinate space of the specified view.
         */
        public float getX(View view) {
            return view.getLocalX(x, y);
        }

        /**
         Gets this Touch's y location in the coordinate space of the specified view.
         */
        public float getY(View view) {
            return view.getLocalY(x, y);
        }
    }

    public static class TouchScrollEvent extends TouchEvent {

        private final int scrollX;
        private final int scrollY;

        public TouchScrollEvent(boolean ctrl, boolean shift, boolean alt, boolean meta,
                int x, int y, int scrollX, int scrollY) {
            super(ctrl, shift, alt, meta, Type.MOVE, x, y, -1);
            this.scrollX = scrollX;
            this.scrollY = scrollY;
        }

        /**
         Gets the horizontal scroll amount
         */
        public int getScrollX() {
            return scrollX;
        }

        /**
         Gets the vertical scroll amount
         */
        public int getScrollY() {
            return scrollY;
        }
    }

    private int cursorCode = Input.CURSOR_DEFAULT;

    /**
    Returns true if the platform has a mouse-pointer attached. Otherwise, it is a touch 
    device. 
    */
    public abstract boolean hasMouse();
    
    public abstract boolean hasKeyboardFocus();

    /**
    Consumes input events. This method is invoked internally by PulpCore.
    */
    public abstract List<Event> consumeEvents();

    public int getCursor() {
        return cursorCode;
    }

    public void setCursor(int cursor) {
        this.cursorCode = cursor;
    }

}

package org.pulpcore.runtime.lwjgl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.pulpcore.runtime.Input;
import org.pulpcore.runtime.lwjgl.graphics.LWJGLSurface;

public class LWJGLInput extends Input {
    
    private LWJGLSurface surface;

    public LWJGLSurface getSurface() {
        return surface;
    }

    public void setSurface(LWJGLSurface surface) {
        this.surface = surface;
    }

    @Override
    public boolean hasMouse() {
        return true;
    }

    @Override
    public boolean hasKeyboardFocus() {
        return Display.isActive();
    }

    @Override
    public List<Event> consumeEvents() {
        // NOTE: LWJGL Bug on Mac OS X (tested on Lion):
        // If a click occurs when the window is inactive, the click (x,y) events
        // are the (x,y) location when the window became inactive.
        ArrayList<Event> events = null;
        while (Mouse.next()) {
            if (events == null) {
                events = new ArrayList<Event>();
            }
            TouchEvent.Type type = null;
            if (Mouse.getEventDX() != 0 || Mouse.getEventDY() != 0) {
                type = isAnyMouseButtonDown() ? TouchEvent.Type.DRAG : TouchEvent.Type.MOVE;
            }
            else if (Mouse.getEventButton() != -1) {
                type = Mouse.getEventButtonState() ? TouchEvent.Type.PRESS : TouchEvent.Type.RELEASE;
            }
            
            if (type != null) {
                events.add(new TouchEvent(
                        isControlDown(), isShiftDown(), isAltDown(), isMetaDown(),
                        type, Mouse.getEventX(), flipY(Mouse.getEventY()), Mouse.getEventButton()));
                
            }
            else if (Mouse.getEventDWheel() != 0) {
                // No way to detect horizontal scrolling?
                int scrollX = 0;
                int scrollY = Mouse.getEventDWheel();
                events.add(new TouchScrollEvent(isControlDown(), isShiftDown(), isAltDown(), isMetaDown(),
                    Mouse.getEventX(), flipY(Mouse.getEventY()), scrollX, scrollY));
            }
            else {
                // Unknown event
            }
            
//            System.out.println("(" + Mouse.getEventX() + "," + Mouse.getEventY() + ") " +
//                    "(" + Mouse.getEventDX() + "," + Mouse.getEventDY() + ") Button" + Mouse.getEventButton() + "=" + Mouse.getEventButtonState() + " wheel=" + Mouse.getEventDWheel());

        }
        while (Keyboard.next()) {
            // TODO
        }
        if (events == null) {
            return Collections.emptyList();
        }
        else {
            return events;
        }
    }
    
    private int flipY(int y) {
        return surface.getHeight() - y - 1;
    }
    
    private boolean isAnyMouseButtonDown() {
        for (int i = 0; i < Mouse.getButtonCount(); i++) {
            if (Mouse.isButtonDown(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private boolean isControlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private boolean isAltDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
                Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private boolean isMetaDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMETA) ||
                Keyboard.isKeyDown(Keyboard.KEY_RMETA);
    }
    
}

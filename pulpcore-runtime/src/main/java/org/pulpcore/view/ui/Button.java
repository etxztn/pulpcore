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
package org.pulpcore.view.ui;

import java.util.EnumMap;
import org.pulpcore.graphics.Color;
import org.pulpcore.math.Tuple2f;
import org.pulpcore.runtime.Context;
import org.pulpcore.runtime.Input.TouchEvent;
import org.pulpcore.util.Objects;
import org.pulpcore.view.Group;
import org.pulpcore.view.View;

/**
A push-button.
*/
public class Button extends Group {
    
    private static final int DEFAULT_WIDTH = 220;
    private static final int DEFAULT_HEIGHT = 30;
    
    /**
    A listener to detect button taps. An OnButtonTapListener is different from a 
    OnTouchTapListener in that in addition to listening for simple taps, it also listens for 
    push-button events (longer taps, for example).
    */
    public interface OnButtonTapListener {
        public void onButtonTap(Button button, TouchEvent event);
    }
    
    public enum State {
        NORMAL,
        HOVER,
        PRESSED,
        DISABLED,
    }
    
    // TODO: Move this somewhere else
    public enum Position {
        TOP,
        LEFT,
        BOTTOM,
        RIGHT
    }
    
    // Views
    private EnumMap<State, View> backgroundViews = new EnumMap<State, View>(State.class);
    private View currBackgroundView;
    private Label label;
    private View icon;
    private Tuple2f pressedOffset = new Tuple2f(0, 1);
    private int margin = 5;
    private int iconLabelMargin = 5;
    private Position iconPosition = Position.LEFT;
    
    // State
    private State state;
    private Group rootWhenArmed;
    private boolean armed = false;
    
    // Listener
    private OnButtonTapListener onButtonTapListener;
    
    public Button() {
        this(null);
    }
    
    public Button(String text) {
        // TODO: Non shitty-looking default buttons
        View normal = new View(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        normal.backgroundColor.set(Color.rgb(0xdddddd));
        setViewForState(State.NORMAL, normal);
        View hover = new View(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        hover.backgroundColor.set(Color.rgb(0xeeeeee));
        setViewForState(State.HOVER, hover);
        View pressed = new View(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        pressed.backgroundColor.set(Color.rgb(0xcccccc));
        setViewForState(State.PRESSED, pressed);
        
        if (!Objects.isNullOrEmpty(text)) {
            getLabel().setText(text);
        }
        setState(State.NORMAL);
        
        TouchHandler touchHandler = new TouchHandler();
        setOnTouchEnterListener(touchHandler);
        setOnTouchExitListener(touchHandler);
        setOnTouchPressListener(touchHandler);
        setOnTouchReleaseListener(touchHandler);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            if (enabled) {
                setState(State.NORMAL);
                opacity.set(1.0);
            }
            else {
                setState(State.DISABLED);
                opacity.set(0.5);
            }
        }
    }
    
    public void setOnButtonTapListener(OnButtonTapListener l) {
        this.onButtonTapListener = l;
    }
    
    public OnButtonTapListener getOnButtonTapListener() {
        return onButtonTapListener;
    }
    
    /**
    Gets the label for this button. To change the button text, invoke
    <code>button.getLabel().setText("New text")</code>.
    @return The button label.
    */
    public Label getLabel() {
        if (label == null) {
            label = new Label();
            label.setMaxLines(1);
            // TODO: truncate instead of word wrap
            label.setLineBreakMode(Label.LineBreakMode.WORD_WRAP);
            label.setEnabled(false);
            addSubview(label);
            doLayout();
        }
        return label;
    }
    
    public View getIcon() {
        return icon;
    }
    
    public void setIcon(View icon) {
        if (this.icon != icon) {
            if (this.icon != null) {
                this.icon.removeFromSuperview();
            }
            this.icon = icon;
            if (this.icon != null) {
                this.icon.setEnabled(false);
                addSubview(this.icon);
            }
            doLayout();
        }
    }
    
    /** Gets the icon position, relative to the label. */
    public Position getIconPosition() {
        return iconPosition;
    }
    
    /** Sets the icon position, relative to the label. */
    public void setIconPosition(Position iconPosition) {
        if (this.iconPosition != iconPosition) {
            this.iconPosition = iconPosition;
            doLayout();
        }
    }

    public int getIconLabelMargin() {
        return iconLabelMargin;
    }

    public void setIconLabelMargin(int iconLabelMargin) {
        if (this.iconLabelMargin != iconLabelMargin) {
            this.iconLabelMargin = iconLabelMargin;
            doLayout();
        }
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        if (this.margin != margin) {
            this.margin = margin;
            doLayout();
        }
    }
    
    /**
    Gets the location offset to display the label and icon when the button is pressed.
    The default is (0,1). The returned value may be modified, and will take affect the
    next time the button is pressed.
    */
    public Tuple2f getPressedOffset() {
        return pressedOffset;
    }
    
    public void setViewForState(State state, View view) {
        backgroundViews.put(state, view);
        if (this.state == state) {
            doLayout();
        }
    }
    
    public View getViewForState(State state) {
        return backgroundViews.get(state);
    }
    
    private void setState(State state) {
        this.state = state;
        View newBackgroundView = backgroundViews.get(state);
        if (newBackgroundView == null) {
            newBackgroundView = backgroundViews.get(State.NORMAL);
        }
        if (currBackgroundView != newBackgroundView) {
            if (currBackgroundView != null) {
               currBackgroundView.removeFromSuperview();
            }
            currBackgroundView = newBackgroundView;
            if (currBackgroundView != null) {
                addSubview(0, currBackgroundView);
                setSize(currBackgroundView.getWidth(), currBackgroundView.getHeight());
            }
        }
        if (state == State.DISABLED) {
            rootWhenArmed = null;
            armed = false;
        }
        doLayout();
    }
    
    private void doLayout() {
        if (icon != null && label != null) {
            float contentWidth = getWidth() - margin*2;
            float contentHeight = getHeight() - margin*2;
            float maxIconWidth;
            float maxIconHeight;
            float maxLabelWidth;
            if (iconPosition == Position.LEFT || iconPosition == Position.RIGHT) {
                maxIconWidth = (contentWidth - iconLabelMargin) / 2;
                maxIconHeight = contentHeight;
                maxLabelWidth = (contentWidth - iconLabelMargin - maxIconWidth);
            }
            else {
                maxIconWidth = contentWidth;
                maxIconHeight = Math.max(1, contentHeight - label.getHeight() - iconLabelMargin);
                maxLabelWidth = contentWidth;
            }
            
            if (icon.getWidth() > 0 && icon.getHeight() > 0) {
                float scale = 1;
                scale = Math.min(scale, maxIconWidth / icon.getWidth());
                scale = Math.min(scale, maxIconHeight / icon.getHeight());
                icon.setScale(scale);
            }
            
            label.setWidth(maxLabelWidth);
            if (iconPosition == Position.LEFT) {
                int contentY = Math.round(getHeight() / 2);
                icon.setAnchor(0, 0.5f);
                icon.setLocation(margin, contentY);
                label.setAlignment(Label.Alignment.LEFT);
                label.setAnchor(0, 0.5f);
                label.setLocation(
                        Math.round(margin + icon.getWidth() * icon.scaleX.get() + iconLabelMargin),
                        contentY);
            }
            else if (iconPosition == Position.RIGHT) {
                int contentY = Math.round(getHeight() / 2);
                icon.setAnchor(1, 0.5f);
                icon.setLocation(getWidth() - margin, contentY);
                label.setAlignment(Label.Alignment.RIGHT);
                label.setAnchor(1, 0.5f);
                label.setLocation(
                        getWidth() - Math.round(margin + icon.getWidth() * icon.scaleX.get() + iconLabelMargin),
                        contentY);
            }
            else if (iconPosition == Position.TOP) {
                int contentX = Math.round(getWidth() / 2);
                icon.setAnchor(0.5f, 0);
                icon.setLocation(contentX, margin);
                label.setAlignment(Label.Alignment.CENTER);
                label.setAnchor(0.5f, 0);
                label.setLocation(contentX,
                        Math.round(margin + icon.getWidth() * icon.scaleX.get() + iconLabelMargin));
            }
            else {
                int contentX = Math.round(getWidth() / 2);
                icon.setAnchor(0.5f, 1);
                icon.setLocation(contentX, getHeight() - margin);
                label.setAlignment(Label.Alignment.CENTER);
                label.setAnchor(0.5f, 1);
                label.setLocation(contentX,
                        getHeight() - Math.round(margin + icon.getWidth() * icon.scaleX.get() + iconLabelMargin));
            }
            
            if (state == State.PRESSED) {
                icon.setLocation(icon.x.get() + pressedOffset.x, icon.y.get() + pressedOffset.y);
                label.setLocation(label.x.get() + pressedOffset.x, label.y.get() + pressedOffset.y);
            }
        }
        else if (icon != null) {
            icon.setAnchor(0.5f, 0.5f);

            // Round to an integer so that the text isn't blurry
            float contentX = Math.round(getWidth() / 2);
            float contentY = Math.round(getHeight() / 2);
            if (state == State.PRESSED) {
                contentX += pressedOffset.x;
                contentY += pressedOffset.y;
            }
            icon.setLocation(contentX, contentY);
            
            if (icon.getWidth() > 0 && icon.getHeight() > 0) {
                float maxWidth = getWidth() - margin*2;
                float maxHeight = getHeight() - margin*2;
                float scale = 1;
                scale = Math.min(scale, maxWidth / icon.getWidth());
                scale = Math.min(scale, maxHeight / icon.getHeight());
                icon.setScale(scale);
            }
        }
        else if (label != null) {
            label.setAlignment(Label.Alignment.CENTER);
            label.setAnchor(0.5f, 0.5f);
            label.setWidth(getWidth() - margin*2);
            
            // Round to an integer so that the text isn't blurry
            float contentX = Math.round(getWidth() / 2);
            float contentY = Math.round(getHeight() / 2);
            if (state == State.PRESSED) {
                contentX += pressedOffset.x;
                contentY += pressedOffset.y;
            }
            label.setLocation(contentX, contentY);
        }
    }
    
    private class TouchHandler implements OnTouchEnterListener, OnTouchExitListener, 
            OnTouchPressListener, OnTouchReleaseListener {

        @Override
        public void onEnter(View view, TouchEvent event) {
            if (event.getType() == TouchEvent.Type.MOVE) {
                setState(State.HOVER);
            }
            else if (armed && event.getType() == TouchEvent.Type.DRAG) {
                setState(State.PRESSED);
            }
        }

        @Override
        public void onExit(View view, TouchEvent event) {
            setState(State.NORMAL);
        }

        @Override
        public void onPress(View view, TouchEvent event) {
            setState(State.PRESSED);
            rootWhenArmed = getRoot();
            armed = true;
        }

        @Override
        public void onRelease(View view, TouchEvent event) {
            Group root = getRoot();
            boolean isOver = Button.this.isAncestorOf(view);
            boolean isSameRoot = root != null && rootWhenArmed == root;
            boolean isTap = armed && state == State.PRESSED && isOver && isSameRoot;
            
            if (isOver && Context.getContext().getInput().hasMouse()) {
                setState(State.HOVER);
            }
            else {
                setState(State.NORMAL);
            }
            rootWhenArmed = null;
            armed = false;

            if (isTap && onButtonTapListener != null) {
                onButtonTapListener.onButtonTap(Button.this, event);
            }
        }
    }
}

/*
    Copyright (c) 2007-2009, Interactive Pulp, LLC
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

package pulpcore.sprite;

import pulpcore.CoreSystem;
import pulpcore.Input;
import pulpcore.animation.Color;
import pulpcore.animation.Property;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.Rect;

/**
    The Button is a Sprite that behaves like a common UI push button. A
    Button has four visual states: normal, hover, pressed, and disabled. Call
    {@link #isClicked()} to check if the user clicked the button. By default, a Button's cursor
    is {@link pulpcore.Input#CURSOR_HAND}.
    <p>
    By default, Buttons use pixel level checking for intersection tests. Use
    {@link ImageSprite#setPixelLevelChecks(boolean) } to disable this feature.
*/
public class Button extends ImageSprite {

    // For labeled button creation. See createLabeledButton().
    private static final int MARGIN = 12;
    private static final int MIN_WIDTH = 72;

    private static final int NORMAL = 0;
    private static final int HOVER = 1;
    private static final int PRESSED = 2;
    private static final int DISABLED = 3;
    private static final int PRESSED_BUT_OUTSIDE = 4;

    private static final int NUM_VISIBLE_STATES = 4;

    private final CoreImage[] images;
    private final boolean isToggleButton;

    private int[] keyBinding;

    private int state;
    private boolean isSelected;
    private boolean isClicked;
    private Group rootWhenPressed;

    /**
        @param images an array of three or four images: normal, hover, pressed, and (optionally)
        disabled.
    */
    public Button(CoreImage[] images, int x, int y) {
        this(images, x, y, false);
    }

    /**
        @param images an array of three or four images: normal, hover, pressed, and (optionally)
        disabled.
        Use six or eight images for toggle buttons (unselected and selected).
    */
    public Button(CoreImage[] images, int x, int y, boolean isToggleButton) {
        super(images[0], x, y);
        this.isToggleButton = isToggleButton;
        // To maintain backwards compatibility w/ 11.5 and earlier (3 button images instead of 4)
        images = addDisabledImageIfNotThere(images, isToggleButton);
        this.images = new CoreImage[images.length];
        init(images, isToggleButton);
    }

    /**
        @param images an array of three or four images: normal, hover, pressed, and (optionally)
        disabled.
    */
    public Button(CoreImage[] images, double x, double y) {
        this(images, x, y, false);
    }

    /**
        @param images an array of three or four images: normal, hover, pressed, and (optionally)
        disabled.
        Use six or eight images for toggle buttons (unselected and selected).
    */
    public Button(CoreImage[] images, double x, double y, boolean isToggleButton) {
        super(images[0], x, y);
        this.isToggleButton = isToggleButton;
        // To maintain backwards compatibility w/ 11.5 and earlier (3 button images instead of 4)
        images = addDisabledImageIfNotThere(images, isToggleButton);
        this.images = new CoreImage[images.length];
        init(images, isToggleButton);
    }

    private void init(CoreImage[] images, boolean isToggleButton) {
        if (images.length < (isToggleButton?2*NUM_VISIBLE_STATES:NUM_VISIBLE_STATES)) {
            throw new IllegalArgumentException("Not enough button images.");
        }
        System.arraycopy(images, 0, this.images, 0, images.length);
        setCursor(Input.CURSOR_HAND);
    }

    private CoreImage[] addDisabledImageIfNotThere(CoreImage[] images, boolean isToggleButton) {
    	CoreImage[] temp = images;
    	if (isToggleButton) {
    		if (images.length == (NUM_VISIBLE_STATES -1)*2) {
    			temp = new CoreImage[NUM_VISIBLE_STATES*2];
    			// 6 images, copy the normal image to the disabled state for both untoggled and toggled states
    			System.arraycopy(images, 0, temp, 0, NUM_VISIBLE_STATES-1);
    			temp[DISABLED] = images[NORMAL];
    			System.arraycopy(images, NUM_VISIBLE_STATES-1, temp, NUM_VISIBLE_STATES, NUM_VISIBLE_STATES-1);
    			temp[DISABLED + NUM_VISIBLE_STATES] = images[NORMAL + (NUM_VISIBLE_STATES-1)];
    		}
    	}
        else {
    		if (images.length == NUM_VISIBLE_STATES -1) {
    			temp = new CoreImage[NUM_VISIBLE_STATES];
    			// 3 images, copy the normal image to the disabled state
    			System.arraycopy(images, 0, temp, 0, images.length);
    			temp[DISABLED] = images[NORMAL];
    		}
    	}

    	return temp;
    }

    //@Override
    public void propertyChange(Property p) {
    	super.propertyChange(p);
    	if (p == enabled) {
    		if (enabled.get()) {
    			setState(NORMAL);
    		}
            else {
    			setState(DISABLED);
    		}
    	}
    }

    /**
        @return true if this button is a toggle button, false otherwise
    */
    public boolean isToggleButton() {
        return isToggleButton;
    }

    /**
        Gets a copy of the key bindings for this button. A button has no key bindings by
        default.
        @return the key bindings for this button, or null if there are no key bindings.
        @see #setKeyBinding(int)
        @see #setKeyBinding(int[])
        @see #clearKeyBinding()
    */
    public int[] getKeyBinding() {
        return CoreSystem.arraycopy(keyBinding);
    }

    /**
        Clears the key binding for this button.
        @see #getKeyBinding()
        @see #setKeyBinding(int)
        @see #setKeyBinding(int[])
    */
    public void clearKeyBinding() {
        keyBinding = null;
    }

    /**
        Sets the key binding for this button to the specified key code. The button is
        considered "clicked" if that key is pressed and released.
        @see #getKeyBinding()
        @see #setKeyBinding(int[])
        @see #clearKeyBinding()
    */
    public void setKeyBinding(int keyCode) {
        setKeyBinding(new int[] { keyCode });
    }

    /**
        Sets the key binding for this button to the specified key codes. The button is
        considered "clicked" if any of those keys are pressed and released.
        @see #getKeyBinding()
        @see #setKeyBinding(int)
        @see #clearKeyBinding()
    */
    public void setKeyBinding(int[] keyCodes) {
        keyBinding = CoreSystem.arraycopy(keyCodes);
    }

    /**
        Sets whether this button is selected. For toggle buttons only.
    */
    public void setSelected(boolean isSelected) {
        if (this.isSelected != isSelected) {
            this.isSelected = isSelected;

            // update the frame
            setState(state);
            setDirty(true);
        }
    }

    /**
        Determines if this button is selected. For toggle buttons only.
    */
    public boolean isSelected() {
        return isSelected;
    }

    private void setState(int state) {
        this.state = state;

        int frame;
        if (state == PRESSED_BUT_OUTSIDE) {
            frame = NORMAL;
        }
        else {
            frame = state;
        }

        if (isToggleButton && isSelected) {
            frame += NUM_VISIBLE_STATES;
        }

        if (images != null) {
            super.setImage(images[frame]);
        }
    }

    public void update(int elapsedTime) {
        super.update(elapsedTime);

        boolean imageChanged = false;
        for (int i = 0; i < images.length; i++) {
            imageChanged |= images[i].update(elapsedTime);
        }
        if (imageChanged) {
            setDirty(true);
        }

        isClicked = isClickedImpl();
        if (state == PRESSED) {
            rootWhenPressed = getRoot();
        }
    }

    /**
        Determines if this button was clicked since the last frame.
        @return true if this button was clicked since the last frame.
    */
    public boolean isClicked() {
        return isClicked && getRoot() == rootWhenPressed;
    }

    /**
        Determines if this button was pressed and the mouse is still held down over the button.
        This method continues to returns true as long as the mouse is pressed and still
        over the button.
        <p>To detect a click, use {@link #isClicked()}.</p>
     */
    public boolean isPressedDown() {
        return state == PRESSED && getRoot() == rootWhenPressed;
    }

    private boolean isClickedImpl() {

        // Disabled button
        if (!isEnabledAndVisible()) {
            if (state != DISABLED) {
                setState(DISABLED);
            }
            return false;
        }

        // Handle key input
        if (keyBinding != null) {
            if (Input.isPressed(keyBinding)) {
                setState(PRESSED);
                return false;
            }
            else if (Input.isDown(keyBinding) && state == PRESSED) {
                return false;
            }
            else if (Input.isReleased(keyBinding) && state == PRESSED) {
                if (isToggleButton) {
                    isSelected = !isSelected;
                }
                setState(NORMAL);
                return true;
            }
        }

        // Handle mouse input
        if (state == PRESSED) {
            if (isMouseReleased()) {
                if (isToggleButton) {
                    isSelected = !isSelected;
                }
                setState(HOVER);
                return true;
            }
            else if (!isMouseDown()) {
                setState(PRESSED_BUT_OUTSIDE);
            }
        }
        else if (state == PRESSED_BUT_OUTSIDE) {
            if (isMouseDown()) {
                setState(PRESSED);
            }
            else if (!Input.isMouseDown()) {
                setState(NORMAL);
            }
        }
        else if (state == HOVER) {
            if (isMousePressedPick()) {
                setState(PRESSED);
            }
            else if (!isMouseHoverPick()) {
                setState(NORMAL);
            }
        }
        else {
            if (isMousePressedPick()) {
                setState(PRESSED);
            }
            else if (isMouseHoverPick()) {
                setState(HOVER);
            }
        }

        return false;
    }

    private boolean isMousePressedPick() {
        return Input.isMouseInside() && Input.isMousePressed() &&
                isPickEnabledAndVisible(Input.getMousePressX(), Input.getMousePressY());
    }

    private boolean isMouseHoverPick() {
        return Input.isMouseInside() && !Input.isMouseDown() &&
                isPickEnabledAndVisible(Input.getMouseX(), Input.getMouseY());
    }

    //
    // Convenience methods to create buttons with text labels.
    //

    public static Button createLabeledButton(String text, int x, int y) {
        return createLabeledButton(null, null, text, x, y);
    }

    public static Button createLabeledToggleButton(String text, int x, int y) {
        return createLabeledToggleButton(null, null, text, x, y);
    }

    public static Button createLabeledButton(CoreImage[] images, CoreFont font,
        String text, int x, int y)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }

        int textX;
        int textY;
        if (images != null) {
            textX = images[0].getWidth() / 2;
            textY = images[0].getHeight() / 2;
        }
        else {
            font = font.tint(0xffffff);

            int textWidth = font.getStringWidth(text);
            int textHeight = font.getHeight();
            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;

            textX = buttonWidth / 2;
            textY = buttonHeight / 2;
        }

        return createLabeledButton(images, font, text, x, y,
            textX, textY, 0.5, 0.5, false, true);
    }

    /**
        @deprecated
     */
    public static Button createLabeledButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, int textAnchor, boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y,
            textX, textY, textAnchor, false, offsetPressedText);
    }

    public static Button createLabeledButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, double textAnchorX, double textAnchorY,
        boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y,
            textX, textY, textAnchorX, textAnchorY, false, offsetPressedText);
    }

    public static Button createLabeledToggleButton(CoreImage[] images, CoreFont font,
        String text, int x, int y)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }

        int textX;
        int textY;
        if (images != null) {
            textX = images[0].getWidth() / 2;
            textY = images[0].getHeight() / 2;
        }
        else {
            font = font.tint(0xffffff);

            int textWidth = font.getStringWidth(text);
            int textHeight = font.getHeight();
            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;

            textX = buttonWidth / 2;
            textY = buttonHeight / 2;
        }

        return createLabeledButton(images, font, text, x, y,
            textX, textY,
            0.5, 0.5, true, true);
    }

    /**
        @deprecated
    */
    public static Button createLabeledToggleButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, int textAnchor, boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y,
            textX, textY, textAnchor, true, offsetPressedText);
    }

    public static Button createLabeledToggleButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, double textAnchorX, double textAnchorY,
        boolean offsetPressedText)
    {
        return createLabeledButton(images, font, text, x, y,
            textX, textY, textAnchorX, textAnchorY, true, offsetPressedText);
    }

    /**
        @param images the images to use. If null, simple gray images are created to fit the text
        @param font the font to use for rendering the text label. If null, the system font is used.
        @param offsetPressedText Set to true to offset the button's text when the button is pressed.
        @deprecated
    */
    public static Button createLabeledButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, int textAnchor,
        boolean isToggleButton, boolean offsetPressedText)
    {
        double textAnchorX = 0;
        double textAnchorY = 0;
        // Hard-coded values are old
        // hcenter/right/vcenter/bottom values from Sprite.
        if ((textAnchor & 4) != 0) {
            textAnchorX = 0.5;
        }
        else if ((textAnchor & 2) != 0) {
            textAnchorX = 1.0;
        }
        if ((textAnchor & 32) != 0) {
            textAnchorY = 0.5;
        }
        else if ((textAnchor & 16) != 0) {
            textAnchorY = 1.0;
        }
        return createLabeledButton(images, font, text, x, y, textX, textY,
                textAnchorX, textAnchorY, isToggleButton, offsetPressedText);
    }

    /**
        @param images the images to use. If null, simple gray images are created to fit the text
        @param font the font to use for rendering the text label. If null, the system font is used.
        @param offsetPressedText Set to true to offset the button's text when the button is pressed.
    */
    public static Button createLabeledButton(CoreImage[] images, CoreFont font,
        String text, int x, int y, int textX, int textY, double textAnchorX, double textAnchorY,
        boolean isToggleButton, boolean offsetPressedText)
    {
        if (font == null) {
            font = CoreFont.getSystemFont();
        }

        // Determine text label location
        int textWidth = font.getStringWidth(text);
        int textHeight = font.getHeight();
        textX -= textAnchorX * textWidth;
        textY -= textAnchorY * textHeight;

        // Create button image, if needed
        if (images == null) {
            font = font.tint(0xffffff);
            images = new CoreImage[isToggleButton?NUM_VISIBLE_STATES*2:NUM_VISIBLE_STATES];

            int buttonWidth = Math.max(MIN_WIDTH, textWidth + MARGIN*2);
            int buttonHeight = textHeight + MARGIN;

            for (int i = 0; i < images.length; i++) {
                images[i] = createButtonImage(buttonWidth, buttonHeight, i);
            }
        }

        // Determine bounds of the image after the text is added
        Rect imageBounds = new Rect(0, 0, images[0].getWidth(), images[0].getHeight());
        Rect textBounds = new Rect(textX, textY, textWidth + 1, textHeight + 1);
        Rect bounds = new Rect(imageBounds);
        bounds.union(textBounds);
        boolean textInsideImage = bounds.equals(imageBounds);

        // Create new images
        CoreImage[] textImages = new CoreImage[images.length];
        for (int i = 0; i < textImages.length; i++) {

            int offsetX = 0;
            int offsetY = 0;
            if (offsetPressedText && (i % NUM_VISIBLE_STATES) == PRESSED) {
                offsetX = 1;
                offsetY = 1;
            }

            textImages[i] = new CoreImage(bounds.width, bounds.height, false);

            CoreGraphics g = textImages[i].createGraphics();
            g.setFont(font);
            g.drawImage(images[i], -bounds.x, -bounds.y);
            g.drawString(text, textX-bounds.x + offsetX, textY-bounds.y + offsetY);
        }

        Button button = new Button(textImages, x, y, isToggleButton);
        button.setPixelLevelChecks(textInsideImage);
        return button;
    }

    private static CoreImage createButtonImage(int buttonWidth, int buttonHeight, int type) {

        // Create the button image at 2x the dimensions, then scale down.
        // This way the corners look anti-aliased.
        int w = buttonWidth * 2;
        int h = buttonHeight * 2;
        int cornerSize = 3;
        CoreImage image = new CoreImage(w, h, false);
        CoreGraphics g = image.createGraphics();
        Color topGradient = new Color();
        Color bottomGradient = new Color();

        // Web 2.0-style gradients
        if (type == NORMAL || type == HOVER) {
            // Normal, Hover
            topGradient.animate(0xff626365, 0xff484848, h/2);
            bottomGradient.animate(0xff030303, 0xff2e2e2e, h/2);
        }
        else if (type == PRESSED) {
            // Pressed
            topGradient.animate(0xff2e2e2e, 0xff484848, h/2);
            bottomGradient.animate(0xff030303, 0xff2e2e2e, h/2);
        }
        else if (type == DISABLED) {
            // Disabled
            topGradient.animate(0xff626365, 0xff626365, h/2);
            bottomGradient.animate(0xff626365, 0xff626365, h/2);
        }
        else if (type == (NUM_VISIBLE_STATES + NORMAL) || type == (NUM_VISIBLE_STATES + HOVER)) {
            // Selected
            topGradient.animate(0xff3d4a8a, 0xff3d4a8a, h/2);
            bottomGradient.animate(0xff11194f, 0xff3d4a8a, h/2);
        }
        else if (type == (NUM_VISIBLE_STATES + DISABLED)) {
            // Selected & Disabled
            topGradient.animate(0xff3d4a8a, 0xff3d4a8a, h/2);
            bottomGradient.animate(0xff3d4a8a, 0xff3d4a8a, h/2);
        }
        else {
            // Selected & pressed
            topGradient.animate(0xff273271, 0xff3d4a8a, h/2);
            bottomGradient.animate(0xff11194f, 0xff3d4a8a, h/2);
        }

        for (int i = 0; i < h/2; i++) {
            int lineWidth = Math.min(w, w + (i - cornerSize) * 2);
            int x = (w - lineWidth) / 2;
            int y1 = i;
            int y2 = h - 1 - i;

            topGradient.update(1);
            g.setColor(topGradient.get());
            g.fillRect(x, y1, lineWidth, 1);

            bottomGradient.update(1);
            g.setColor(bottomGradient.get());
            g.fillRect(x, y2, lineWidth, 1);
        }

        return image.halfSize();
    }

}

/*
    Copyright (c) 2008, Interactive Pulp, LLC
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

package pulpcore.platform;

import java.util.Iterator;
import java.util.LinkedList;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Slider;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import pulpcore.util.StringUtil;

public class ConsoleScene extends Scene2D {
    
    private Button backButton;
    private Button clearButton;
    private Button copyButton;
    private TextBox textbox;
    
    public void load() {
        
        add(new FilledSprite(Colors.WHITE));
        
        backButton = Button.createLabeledButton("OK", Stage.getWidth() - 5, Stage.getHeight() - 5);
        backButton.setAnchor(Sprite.SOUTH_EAST);
        backButton.setKeyBinding(new int[] { Input.KEY_ESCAPE, Input.KEY_ENTER });
        
        clearButton = Button.createLabeledButton("Clear", 5, Stage.getHeight() - 5);
        clearButton.setAnchor(Sprite.SOUTH_WEST);
        
        copyButton = Button.createLabeledButton("Copy to Clipboard", 
            clearButton.x.getAsInt() + clearButton.width.getAsInt() + 5, Stage.getHeight() - 5);
        copyButton.setAnchor(Sprite.SOUTH_WEST);
        
        textbox = new TextBox(5, 5, Stage.getWidth() - 10, 
            Stage.getHeight() - 15 - clearButton.height.getAsInt());
        addLayer(textbox);
        
        add(clearButton);
        if (CoreSystem.isNativeClipboard()) {
            add(copyButton);
        }
        if (Stage.canPopScene()) {
            add(backButton);
        }
    }
    
    
    public void update(int elapsedTime) {
        if (clearButton.isClicked()) {
            CoreSystem.clearLog();
        }
        
        // Check button presses
        if (backButton.isClicked()) {
            if (Stage.canPopScene()) {
                Stage.popScene();
            }
        }
        if (copyButton.isClicked()) {
            CoreSystem.setClipboardText(CoreSystem.getLogText());
        }
    }
    
    static class TextBox extends Group {
        
        private static final int LINE_SPACING = CoreFont.getSystemFont().getHeight() + 2;
        
        /*
            Use a slider as a scrollbar. Ideally a real scroll bar would look better (a thumb
            image that changes its dimensions based on the size of the extent, and up/down arrows) 
            but using a slider works for now.
        */
        private Slider scrollBar;
        private Group contents;
        private String lastLine;
        
        public TextBox(int x, int y, int w, int h) {
            super(x, y, w, h);
            
            scrollBar = createScrollBar(w - 1, 0, 16, h);
            scrollBar.setAnchor(Sprite.NORTH_EAST);
            scrollBar.value.set(0);
            scrollBar.setRange(0, 10, h / LINE_SPACING);
            scrollBar.setAnimationDuration(60, 250);
            add(scrollBar);
            
            contents = new Group(0, 0, w - scrollBar.width.get() - 2, h);
            add(contents);
            
            scrollBar.value.addListener(new PropertyListener() {
                public void propertyChange(Property p) {
                    // Set as integer to prevent text blurring
                    contents.y.set((int)Math.round(-scrollBar.value.get() * LINE_SPACING));
                }
            });
            
            refresh();
            scrollBar.scrollEnd();
        }
        
        private Slider createScrollBar(int x, int y, int w, int h) {
            CoreImage background = new CoreImage(w, h);
            CoreGraphics g = background.createGraphics();
            g.setColor(Colors.gray(224));
            g.fill();
            g.setColor(Colors.gray(192));
            g.drawRect(0, 0, w, h);
            
            CoreImage thumb = new CoreImage(w, Math.max(w, h/5));
            g = thumb.createGraphics();
            g.setColor(Colors.gray(64));
            g.fill();
            
            Slider slider = new Slider(background, thumb, x, y);
            slider.setOrientation(Slider.VERTICAL);
            return slider;
        }
        
        public void update(int elapsedTime) {
            super.update(elapsedTime);
            if (needsRefresh()) {
                refresh();
            }
            
            if (Input.isPressed(Input.KEY_HOME)) {
                scrollBar.scrollHome();
            }
            if (Input.isPressed(Input.KEY_END)) {
                scrollBar.scrollEnd();
            }
            if (Input.isTyped(Input.KEY_UP)) {
                scrollBar.scrollUp();
            }
            if (Input.isTyped(Input.KEY_DOWN)) {
                scrollBar.scrollDown();
            }
            if (Input.isTyped(Input.KEY_PAGE_UP)) {
                scrollBar.scrollPageUp();
            }
            if (Input.isTyped(Input.KEY_PAGE_DOWN)) {
                scrollBar.scrollPageDown();
            }
            if (isMouseWheelRotated()) {
                scrollBar.scroll(Input.getMouseWheelRotation() * 3);
            }            
        }
        
        private boolean needsRefresh() {
            LinkedList logLines = CoreSystem.getThisAppContext().getLogLines();
            String line = null;
            if (logLines.size() > 0) {
                line = (String)logLines.getLast();
            }
            return (lastLine != line);
        }
        
        private void refresh() {
            LinkedList logLines = CoreSystem.getThisAppContext().getLogLines();
            if (logLines.size() > 0) {
                lastLine = (String)logLines.getLast();
            }
            else {
                lastLine = null;
            }
            
            contents.removeAll();
            int numLines = 0;
            int y = 0;
            int w = contents.width.getAsInt();
            Iterator i = logLines.iterator();
            while (i.hasNext()) {
                String[] text = StringUtil.wordWrap((String)i.next(), null, w);
                
                if (text.length == 0) {
                    text = new String[] { " " };
                }
                for (int j = 0; j < text.length; j++) {
                    String line = StringUtil.replace(text[j], "\t", "    ");
                    contents.add(new Label(line, 0, y));
                    y += LINE_SPACING;
                    numLines++;
                }
            }
            
            scrollBar.setRange(0, numLines, scrollBar.getExtent());
            if (scrollBar.getExtent() >= numLines) {
                scrollBar.visible.set(false);
                scrollBar.value.set(0);
            }
            else {
                scrollBar.visible.set(true);
            }
        }
        
        protected void drawSprite(CoreGraphics g) {
            Rect lastClip = new Rect();
            g.getClip(lastClip);
            
            updateDirtyRect();
            Rect newClip = getDirtyRect();
            if (newClip == null) {
                g.clipRect(x.getAsInt(), y.getAsInt(), width.getAsInt(), height.getAsInt());
            }
            else {
                g.clipRect(newClip);
            }
            super.drawSprite(g);
            g.setClip(lastClip);
        }
    }
}

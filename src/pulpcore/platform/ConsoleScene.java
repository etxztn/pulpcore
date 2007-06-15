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

package pulpcore.platform;

import java.util.Vector;
import pulpcore.CoreSystem;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import pulpcore.util.StringUtil;

public class ConsoleScene extends Scene2D {
    
    private Button backButton = Button.createLabeledButton("OK", 0, 0);
    private Button clearButton = Button.createLabeledButton("Clear", 0, 0);
    private Button copyButton = Button.createLabeledButton("Copy to Clipboard", 0, 0);
    private TextBox textbox;
    
    public void load() {
        
        add(new FilledSprite(CoreGraphics.WHITE));
        
        backButton.setLocation(Stage.getWidth() - 5, Stage.getHeight() - 5);
        backButton.setAnchor(Sprite.BOTTOM | Sprite.RIGHT);
        backButton.setKeyBinding(Input.KEY_SPACE);
        
        clearButton.setLocation(5, Stage.getHeight() - 5);
        clearButton.setAnchor(Sprite.BOTTOM | Sprite.LEFT);
        
        copyButton.setLocation(clearButton.x.getAsInt() + clearButton.width.getAsInt() + 5,
            Stage.getHeight() - 5);
        copyButton.setAnchor(Sprite.BOTTOM | Sprite.LEFT);
        
        textbox = new TextBox(5, 5, Stage.getWidth() - 10, Stage.getHeight() - 15 - 
            clearButton.height.getAsInt());
        
        addLayer(textbox);
        
        add(clearButton);
        if (CoreSystem.isNativeClipboard()) {
            add(copyButton);
        }
        add(backButton);
    }
    
    
    public void update(int elapsedTime) {
        
        if (textbox.needsRefresh()) {
            textbox.refresh();
        }
        
        // Check button presses
        if (backButton.isClicked() || Input.isPressed(Input.KEY_ESCAPE)) {
            if (Stage.isInterrupted()) {
                Stage.gotoInterruptedScene();
            }
        }
        if (clearButton.isClicked()) {
            CoreSystem.clearLog();
        }
        if (copyButton.isClicked()) {
            CoreSystem.setClipboardText(CoreSystem.getLogText());
        }
        if (Input.isPressed(Input.KEY_HOME)) {
            textbox.scrollHome();
        }
        if (Input.isPressed(Input.KEY_END)) {
            textbox.scrollEnd();
        }
        if (Input.isTyped(Input.KEY_UP)) {
            textbox.scrollLine(1);
        }
        if (Input.isTyped(Input.KEY_DOWN)) {
            textbox.scrollLine(-1);
        }
        if (Input.isTyped(Input.KEY_PAGE_UP)) {
            textbox.scrollPage(1);
        }
        if (Input.isTyped(Input.KEY_PAGE_DOWN)) {
            textbox.scrollPage(-1);
        }
    }
    
    
    static class TextBox extends Group {
        
        private static final int LINE_SPACING = CoreFont.getSystemFont().getHeight() + 2;
        
        private Group contents;
        private String lastLine;
        private final int pageSize;
        private int displayLine;
        private int numLines;
        
        
        public TextBox(int x, int y, int w, int h) {
            super(x, y, w, h);
            pageSize = CoreMath.intDivCeil(height.getAsInt(), LINE_SPACING);
            contents = new Group();
            add(contents);
            refresh();
        }
        
        
        public boolean needsRefresh() {
            Vector logLines = CoreSystem.getLogLines();
            String line = null;
            if (logLines.size() > 0) {
                line = (String)logLines.lastElement();
            }
            return (lastLine != line);
        }
        
        
        public void scrollHome() {
            scrollLine(numLines);
            contents.y.stopAnimation(true);
        }
        
        
        public void scrollEnd() {
            scrollLine(-numLines);
            contents.y.stopAnimation(true);
        }
        
        
        public void scrollPage(int dy) {
            scrollLine(dy * (pageSize-2));
        }
        
        
        public void scrollLine(int dy) {
            int newDisplayLine = displayLine + dy;
            
            if (newDisplayLine > numLines - pageSize + 1) {
                newDisplayLine = numLines - pageSize + 1;
            }
            if (newDisplayLine < 0) {
                newDisplayLine = 0;
            }
            
            if (displayLine != newDisplayLine) {
                int scrollTime = Math.max(100, Math.abs(displayLine - newDisplayLine) * 20);
                displayLine = newDisplayLine;
                contents.y.animateTo(height.getAsInt() - (numLines - displayLine) * LINE_SPACING,
                    scrollTime); 
            }
        }
        
        
        public void refresh() {
            Vector logLines = CoreSystem.getLogLines();
            if (logLines.size() > 0) {
                lastLine = (String)logLines.lastElement();
            }
            else {
                lastLine = null;
            }
            
            contents.removeAll();
            numLines = 0;
            int y = 0;
            for (int i = 0; i < logLines.size(); i++) {
                String[] text = StringUtil.wordWrapText((String)logLines.elementAt(i),
                    null, width.getAsInt());
                
                if (text.length == 0) {
                    text = new String[] { " " };
                }
                for (int j = 0; j < text.length; j++) {
                    String line = StringUtil.replace(text[j], "\t", "        ");
                    contents.add(new Label(line, 0, y));
                    y += LINE_SPACING;
                    numLines++;
                }
            }
            contents.y.set(height.getAsInt() - (numLines - displayLine) * LINE_SPACING);
        }
        
        
        protected void drawSprite(CoreGraphics g) {
            Rect lastClip = new Rect();
            g.getClip(lastClip);
            
            calcDirtyRect();
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

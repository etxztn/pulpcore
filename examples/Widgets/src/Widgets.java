import pulpcore.image.CoreFont;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.sprite.TextField;
import pulpcore.Stage;

public class Widgets extends Scene2D {
    
    Label answer;
    TextField textField;
    TextField passwordField;
    Button okButton;
    Button checkbox;
    
    public void load() {
        
        CoreFont font = CoreFont.getSystemFont();
        
        Label label = new Label("Name: ", 275, 140);
        label.setAnchor(Sprite.EAST);
        
        textField = new TextField(280, 140, 150, font.getHeight());
        textField.setAnchor(Sprite.WEST);
        textField.setFocus(true);
        
        Label label2 = new Label("Secret Password: ", 275, 180);
        label2.setAnchor(Sprite.EAST);
        
        passwordField = new TextField(280, 180, 150, font.getHeight());
        passwordField.setPasswordMode(true);
        passwordField.setAnchor(Sprite.WEST);
        
        CoreImage buttonImage = CoreImage.load("button.png");
        okButton = new Button(buttonImage.split(3), 275, 260);
        okButton.setAnchor(Sprite.NORTH);
        okButton.setKeyBinding(Input.KEY_ENTER);
        
        CoreImage checkboxImage = CoreImage.load("checkbox.png");
        checkbox = Button.createLabeledToggleButton(checkboxImage.split(3,2), font, 
            "Rotate", 280, 220, 30, 12, Sprite.WEST, false);
        checkbox.setCursor(Input.CURSOR_DEFAULT);
        checkbox.setAnchor(Sprite.WEST);
        
        answer = new Label("", 275, 350);
        answer.setAnchor(Sprite.CENTER);
        
        add(new FilledSprite(0x9999ff));
        add(label);
        add(label2);
        add(createBackground(textField));
        add(textField);
        add(createBackground(passwordField));
        add(passwordField);
        add(okButton);
        add(checkbox);
        add(answer);
    }
    
    public Sprite createBackground(TextField field) {
        FilledSprite background = new FilledSprite(
            field.x.getAsInt() - 4, field.y.getAsInt(), 
            field.width.getAsInt() + 8, field.height.getAsInt() + 8, 0xffffff);
        background.setBorderSize(1);
        background.setBorderColor(0x000000);
        background.setAnchor(Sprite.VCENTER | Sprite.LEFT);
        return background;
    }
    
    public void update(int elapsedTime) {
        if (okButton.isClicked()) {
            answer.setText("Hello \"" + textField.getText() + "\"! " + 
                (checkbox.isSelected() ? "Look, widgets work when transformed!" : ""));
            
            // Rotate all sprites except the background
            double newAngle = checkbox.isSelected() ? Math.PI / 16 : 0;
            Group layer = getMainLayer();
            for (int i = 1; i < layer.size(); i++) {
                layer.get(i).angle.animateTo(newAngle, 500);
            }
        }
        
        if (Input.isPressed(Input.KEY_TAB)) {
            if (textField.hasFocus()) {
                textField.setFocus(false);
                passwordField.setFocus(true);
            }
            else {
                textField.setFocus(true);
                passwordField.setFocus(false);
            }
        }
    }
 
}
import pulpcore.image.CoreFont;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.sprite.TextField;
import pulpcore.Stage;

public class Widgets extends Scene2D {
    
    Label answer;
    TextField textField;
    TextField passwordField;
    Button button;
    Button checkbox;
    
    public void load() {
        
        CoreFont font = CoreFont.getSystemFont();
        
        Label label = new Label("Name: ", 275, 140);
        label.setAnchor(Sprite.VCENTER | Sprite.RIGHT);
        
        textField = new TextField(280, 140, 150, TextField.AUTO);
        textField.setAnchor(Sprite.VCENTER | Sprite.LEFT);
        textField.setFocus(true);
        
        Label label2 = new Label("Secret Password: ", 275, 180);
        label2.setAnchor(Sprite.VCENTER | Sprite.RIGHT);
        
        passwordField = new TextField(280, 180, 150, TextField.AUTO);
        passwordField.setPasswordMode(true);
        passwordField.setAnchor(Sprite.VCENTER | Sprite.LEFT);
        
        CoreImage buttonImage = CoreImage.load("button.png");
        button = new Button(buttonImage.split(3), 275, 260);
        button.setAnchor(Sprite.TOP | Sprite.HCENTER);
        button.setKeyBinding(Input.KEY_ENTER);
        
        CoreImage checkboxImage = CoreImage.load("checkbox.png");
        checkbox = Button.createLabeledToggleButton(checkboxImage.split(3,2), font, 
            "Remember Me", 280, 220, 30, 12, Sprite.VCENTER | Sprite.LEFT, false);
        checkbox.setCursor(Input.CURSOR_DEFAULT);
        checkbox.setAnchor(Sprite.VCENTER | Sprite.LEFT);
        
        answer = new Label("", 275, 350);
        answer.setAnchor(Sprite.VCENTER | Sprite.HCENTER);
        
        add(new FilledSprite(0x9999ff));
        add(label);
        add(label2);
        add(createBackground(textField));
        add(textField);
        add(createBackground(passwordField));
        add(passwordField);
        add(button);
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
        if (button.isClicked()) {
            answer.setText("The name is \"" + textField.getText() + "\" " + 
                "and the secret password is \"" + passwordField.getText() + "\"");
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
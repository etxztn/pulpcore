// Widgets
// Shows various buttons and form fields.
// All widgets work when transformed!
import pulpcore.animation.Easing;
import static pulpcore.image.Colors.*;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Slider;
import pulpcore.sprite.Sprite;
import pulpcore.sprite.TextField;
import pulpcore.Stage;

public class Widgets extends Scene2D {
    
    Label answer;
    TextField textField;
    TextField passwordField;
    Button okButton;
    Button checkbox;
    Slider slider;
    Group form;
    
    @Override
    public void load() {
        CoreFont font = CoreFont.getSystemFont();
        
        // Create the form fields
        Label label = new Label("Name: ", 0, 0);
        label.setAnchor(Sprite.EAST);
        
        textField = new TextField("Suzy", 5, 0, 150, font.getHeight());
        textField.setAnchor(Sprite.WEST);
        textField.setFocus(true);
        
        Label label2 = new Label("Secret Password: ", 0, 40);
        label2.setAnchor(Sprite.EAST);
        
        passwordField = new TextField(5, 40, 150, font.getHeight());
        passwordField.setPasswordMode(true);
        passwordField.setAnchor(Sprite.WEST);
        
        Slider slider = new Slider("slider.png", "slider-thumb.png", 0, 80);
        slider.setInsets(0, 1, 0, 1);
        slider.setAnchor(Sprite.WEST);
        Label label3 = new Label("Value: %d ", 0, 80);
        label3.setFormatArg(slider.value);
        label3.setAnchor(Sprite.EAST);
        
        CoreImage checkboxImage = CoreImage.load("checkbox.png");
        checkbox = Button.createLabeledToggleButton(checkboxImage.split(3,2), font,
            "I'm feeling slanted", 0, 120, 30, 12, Sprite.WEST, false);
        checkbox.setCursor(Input.CURSOR_DEFAULT);
        checkbox.setAnchor(Sprite.WEST);
        
        CoreImage buttonImage = CoreImage.load("button.png");
        okButton = new Button(buttonImage.split(3), 0, 160);
        okButton.setAnchor(Sprite.NORTH);
        okButton.setKeyBinding(Input.KEY_ENTER);
        
        // Add the form fields to a group
        form = new Group(Stage.getWidth() / 2, Stage.getHeight() / 2);
        form.setAnchor(Sprite.CENTER);
        form.add(label);
        form.add(createTextFieldBackground(textField));
        form.add(textField);
        form.add(label2);
        form.add(createTextFieldBackground(passwordField));
        form.add(passwordField);
        form.add(label3);
        form.add(slider);
        form.add(okButton);
        form.add(checkbox);
        form.pack();
        
        // Add background, answer message, and form to the scene
        answer = new Label("", 320, 400);
        answer.setAnchor(Sprite.CENTER);
        add(new FilledSprite(WHITE));
        add(answer);
        addLayer(form);
    }
    
    public Sprite createTextFieldBackground(TextField field) {
        FilledSprite background = new FilledSprite(
            field.x.get() - 4, field.y.get(), 
            field.width.get() + 8, field.height.get() + 8, WHITE);
        background.setBorderSize(1);
        background.borderColor.set(BLACK);
        background.setAnchor(Sprite.WEST);
        return background;
    }
    
    @Override
    public void update(int elapsedTime) {
        if (checkbox.isClicked()) {
            double newAngle = checkbox.isSelected() ? Math.PI/16 : 0;
            form.angle.animateTo(newAngle, 500, Easing.ELASTIC_OUT);
        }
        if (okButton.isClicked()) {
            answer.setText("Hello, " + textField.getText() + "!");
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
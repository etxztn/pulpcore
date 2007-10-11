// Shows incoming chat messages via multi-threading.
import pulpcore.animation.Easing;
import pulpcore.math.CoreMath;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.Stage;

public class MultiThreaded extends Scene2D {
    
    Thread networkThread;
    Group messageLayer;
    int maxMessages = 5;
    
    public void load() {
        networkThread = new FauxNetworkThread();
        networkThread.start();
        
        messageLayer = new Group();
        add(new FilledSprite(0xffffff));
        addLayer(messageLayer);
    }
    
    public void unload() {
        networkThread = null;
    }
    
    public void addMessage(String message) {
        int lineHeight = 20;
        int offsetX = 5;
        int offsetY = Stage.getHeight() - lineHeight - 5;
        
        // Remove the first message
        if (messageLayer.size() == maxMessages) {
            messageLayer.remove(messageLayer.get(0));
        }
        
        // Move previous messages up
        for (int i = 0; i < messageLayer.size(); i++) {
            int j = messageLayer.size() - i;
            messageLayer.get(i).y.animateTo(offsetY - lineHeight*j, 150);
        }
        
        // Add the new message
        Label label = new Label(message, offsetX, offsetY);
        label.alpha.set(0);
        label.alpha.animateTo(255, 150, Easing.NONE, 150);
        messageLayer.add(label);
    }
    
    class FauxNetworkThread extends Thread {
        
        String[] messages = {
            "AKA - Also known as",
            "AFK - Away from keyboard",
            "BRB - Be right back",
            "BTW - By the way",
            "CU - See you",
            "FWIW - For what it's worth",
            "FYI - For your information",
            "IMO - In my opinion",
            "IMHO - In my humble opinion",
            "IOW - In other words",
            "LOL - Laughing out loud",
            "OIC - Oh, I see",
            "OMG - Oh my god",
            "OTOH - On the other hand",
            "RL - Real life",
            "ROFL - Rolling on the floor laughing",
            "TTFN - Ta-ta for now",
            "TTYL - Talk to you later",
            "WB - Welcome back",
            "YMMV - Your mileage may vary",
        };
        
        public void run() {
            while (this == networkThread) {
                
                // Make sure the code runs in the animation thread
                invokeAndWait(new Runnable() {
                    public void run() {
                        addMessage(messages[CoreMath.rand(0, messages.length - 1)]);
                    }
                });
                
                try {
                    Thread.sleep(1500);
                }
                catch (InterruptedException ex) { }
            }
        }
    }
}

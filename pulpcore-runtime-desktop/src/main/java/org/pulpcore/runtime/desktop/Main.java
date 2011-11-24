package org.pulpcore.runtime.desktop;

import org.pulpcore.runtime.lwjgl.LWJGLContext;
import org.pulpcore.runtime.lwjgl.graphics.LWJGLSurface;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.pulpcore.runtime.jre.JreRunLoop;
import org.pulpcore.runtime.lwjgl.LWJGLInput;
import org.pulpcore.view.Scene;
import org.pulpcore.view.Stage;

public class Main {
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: Main <scene-class>");
            return;
        }
        Class<?> firstScene = null;
        try {
            firstScene = Class.forName(args[0]);
        }
        catch (ClassNotFoundException ex) {
            System.out.println("Class not found: " + args[0]);
            return;
        }
        if (!Scene.class.isAssignableFrom(firstScene)) {
            System.out.println("Class is not a Scene: " + args[0]);
            return;
        }
        new Main().start((Class<? extends Scene>)firstScene);
    }
    
    private LWJGLContext context;
    private Stage stage;
    
    private void start(Class<? extends Scene> firstScene) {
        
        // TODO: Set these somewhere else
        int stageWidth = 640;
        int stageHeight = 480;
        int windowWidth = 640;//1024;
        int windowHeight = 480;//768;
        
        context = new LWJGLContext(this, firstScene);
        LWJGLSurface surface;
        try {
            surface = new LWJGLSurface(context, windowWidth, windowHeight);
        }
        catch (LWJGLException ex) {
            ex.printStackTrace(System.out);
            stop();
            return;
        }
        ((LWJGLInput)context.getInput()).setSurface(surface);
        
        stage = new Stage(context, new JreRunLoop(context), surface);
        stage.setSize(stageWidth, stageHeight);
        stage.start();
    }
    
    public void stop() {
        if (stage != null) {
            stage.stop();
            stage = null;
        }
        if (context != null) {
            context.destroy();
            context = null;
        }
        Display.destroy();
        System.exit(0);
    }
}

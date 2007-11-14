// Sketch
// Move your mouse to draw. Click to erase.
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.scene.Scene;
import pulpcore.Stage;

public class Sketch extends Scene {
    Particle[] particles;
    int particleIndex;
    int nextFade, nextFade2;
    int lastX, lastY;
    boolean erase;
    boolean wasMouseInside;
    
    public void load() {
        int numParticlesPerAttractor = 500;
        particles = new Particle[5000];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
        for (int i = 0; i < particles.length; i++) {
            int j = (i + particles.length - 1) % particles.length;
            j = j - (j % numParticlesPerAttractor);
            particles[i].attractor = particles[j];
        }
        erase = true;
    }
    
    
    public void updateScene(int elapsedTime) {
        if (Input.isMousePressed()) {
            load();
        }
        else {
            // Draw
            int x = Input.getMouseX();
            int y = Input.getMouseY();
            if (wasMouseInside && (x != lastX || y != lastY)) {
                makeParticles(lastX, lastY, x, y);
            }
            lastX = x;
            lastY = y;
            wasMouseInside = Input.isMouseInside();
        
            // Update particles
            elapsedTime = Math.min(elapsedTime, 100);
            for (int i = 0; i < particles.length; i++) {
                particles[i].update(elapsedTime);
            }
            nextFade -= elapsedTime;
            nextFade2 -= elapsedTime;
        }
    }
    
    public void drawScene(CoreGraphics g) {
        if (erase) {
            erase = false;
            g.setColor(CoreGraphics.WHITE);
            g.fill();
        }
        
        // Draw particles to surface
        g.setComposite(CoreGraphics.COMPOSITE_MULT);
        for (int i = 0; i < particles.length; i++) {
            particles[i].draw(g);
        }
        
        // Initial fade (it won't fade completely due to rounding error)
        if (nextFade <= 0) { 
            nextFade = 75;
            g.setComposite(CoreGraphics.COMPOSITE_SRC_OVER);
            g.setColor(CoreGraphics.WHITE);
            g.setAlpha(0x06);
            g.fill();
        }
        // Delayed, slow fade (it will fade completely)
        if (nextFade2 <= 0) {
            nextFade2 = 250;
            g.setComposite(CoreGraphics.COMPOSITE_ADD);
            g.setColor(0x020202);
            g.setAlpha(0xff);
            g.fill();
        }
    }
    
    void makeParticles(int x1, int y1, int x2, int y2) {
        int numParticles = 2*(int)Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)); 
        for (int i = 0; i < numParticles; i++) {
            double x = x1 + i * (x2 - x1) / (double)numParticles;
            double y = y1 + i * (y2 - y1) / (double)numParticles;
            particles[particleIndex].init(x, y);
            particleIndex = (particleIndex + 1) % particles.length;
        }
    }
    
    // Start the particle the middle, but don't draw until init()
    static class Particle {
        double x = Stage.getWidth() / 2 + CoreMath.rand(-50, 50);
        double y = Stage.getHeight() / 2 + CoreMath.rand(-50, 50);
        boolean alive = false;
        double velocityX, velocityY;
        double lastX, lastY;
        Particle attractor;

        void init(double x, double y) {
            this.x = x;
            this.y = y;
            this.lastX = x;
            this.lastY = y;
            this.velocityX = CoreMath.rand(-20, 20);
            this.velocityY = CoreMath.rand(-20, 20);
            alive = true;
        }
        
        void update(int elapsedTime) {
            double dt = elapsedTime / 1000.0;
            double dx = attractor.x-x;
            double dy = attractor.y-y;
            double dSq = dx*dx + dy*dy;
            if (dSq > 0) {
                double s = 400 / dSq;
                if (dSq < 25 * 25) {
                    s = -s / 4;
                }
                velocityX += s*dx;
                velocityY += s*dy;
            }
            velocityX *= 0.97;
            velocityY *= 0.97;
            x += velocityX * dt;
            y += velocityY * dt;
        }
        
        void draw(CoreGraphics g) {
            if (alive) {
                g.setColor(0xeeeeee);
                g.drawLine(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            }
        }
    }
}

# Introduction #

We present an additional example of incorporating physics in PulpCore applications using the physics engine JBox2D (http://www.jbox2d.org/) together with Fizzy, a simple interface for JBox2D (originally at http://old.cokeandcode.com/node/1438 and recently moved to http://github.com/nanodeath/fizzy).
This code also uses the desktop port for PulpCore created by Piotr Korzuszek (http://github.com/genail/cr-pulpcore-desktop-demo).

For those interested in building similar applications, there are two customized JAR files available (that were used when building this project):
  * PulpCore 0.11.6 combined with Piotr's desktop platform classes (and other community-created additions): http://www.adelphi.edu/~stemkoski/pulpcore/pulpcore-extra.jar
  * JBox2D (version 2.1.2) packaged together with Fizzy: http://www.adelphi.edu/~stemkoski/pulpcore/jbox2d-2.1.2-fizzy.jar

Demonstration video (http://www.youtube.com/watch?v=a7nOO0cAfzE):

<a href='http://www.youtube.com/watch?feature=player_embedded&v=a7nOO0cAfzE' target='_blank'><img src='http://img.youtube.com/vi/a7nOO0cAfzE/0.jpg' width='425' height=344 /></a>


# Details #

This program consists of two classes: the main demo program, and the PhysicsSprite class, which extends the ImageSprite class and contains a Body, which is an entity in a World, where physics calculations take place. Using PulpCore's BindFunction method as illustrated in Brackeen's Physics example (http://www.interactivepulp.com/pulpcore/physics/), we can have the coordinates and angle of the sprite automatically updated according to the properties of the corresponding Body in the physics World. Finally, by using Listeners, we can write PulpCore code to respond to collision events taking place in the physics world.

First, the main class:

```
import org.newdawn.fizzy.*;

import java.util.Iterator;
import java.text.DecimalFormat;

import pulpcore.*;
import pulpcore.animation.*;
import pulpcore.image.*;
import pulpcore.image.filter.*;
import pulpcore.platform.desktop.*;
import pulpcore.scene.*;
import pulpcore.sprite.*;

public class PhysicsDemo extends Scene2D
{
    // World: the model in which physics takes place.
    protected World physicsWorld;
    private static final float WORLD_TO_STAGE_SCALE = 10;
    private float time;
    private float dt = 1 / 60f;

    public PhysicsSprite playerSprite;   

    CoreFont font = CoreFont.getSystemFont();
    Label playerDataLabel = new Label(font, "", 0, 0);
    DecimalFormat df = new DecimalFormat("#0.00");

    public static void main(String[] args) 
    {
        CoreApplication app = new CoreApplication(PhysicsDemo.class);
        app.setWindowSize(800,600);
        app.run();
    }

    public void load()
    {
        // use to reset demo
        unload();

        // background
        add(new FilledSprite(Colors.BLACK));

        physicsWorld = new World();  // positive gravity goes down in pulpcore.
        physicsWorld.setGravity(20);

        // group of physics sprites that have potential non-physics interaction
        final Group spriteList = new Group();
        add(spriteList);

        // create and add dynamic objects
        CoreImage playerImage = new CoreImage(32,32).tint(Colors.GRAY);
        playerSprite = new PhysicsSprite(playerImage, 400, 50);
        playerSprite.setBodyRectangle(true, physicsWorld);
        spriteList.add(playerSprite);

        CoreImage circImage = CoreImage.load("ball.png");
        PhysicsSprite circSprite = new PhysicsSprite(circImage, 300, 75);
        circSprite.setBodyCircle(true, physicsWorld);
        spriteList.add(circSprite);

        CoreImage boxImage = CoreImage.load("box.png");
        PhysicsSprite boxSprite = new PhysicsSprite(boxImage, 550, 75);
        boxSprite.setBodyRectangle(true, physicsWorld);
        spriteList.add(boxSprite);

        // create and add the ground and other static objects
        CoreImage floorImage = new CoreImage(600,10).tint(Colors.PURPLE);
        PhysicsSprite floor = new PhysicsSprite(floorImage, 400, 500);
        floor.setBodyRectangle(false, physicsWorld);
        spriteList.add(floor);

        CoreImage specialImage = new CoreImage(60,60).tint(Colors.PURPLE);
        PhysicsSprite specialBox = new PhysicsSprite("special", specialImage, 200, 300);
        specialBox.setBodyRectangle(false, physicsWorld);
        spriteList.add(specialBox);

        // text labels
        CoreFont font = CoreFont.getSystemFont().tint(Colors.WHITE);
        Label directions = new Label(font, "Press ARROW keys to add velocity to gray box or R key to reset.", 100, 40);
        add(directions);

        playerDataLabel = new Label(font, "Player data: ", 100, 60);
        add(playerDataLabel);

        // actions to take place on collisions
        physicsWorld.addBodyListener( playerSprite.getBody(), new WorldListener() 
            {
                public void collided(CollisionEvent event) 
                {
                    PhysicsSprite p = getPhysicsSprite( event.getBodyB(), spriteList );

                    if (p == null) return;

                    if ( p.getName().equals("special") )
                    {
                        if (playerSprite.getYVelocity() > 0)    
                            p.setImage( p.getImage().tint(Colors.RED) );
                        else if (playerSprite.getYVelocity() < 0)
                            p.setImage( p.getImage().tint(Colors.GREEN) );
                    }
                }

                public void separated(CollisionEvent event) 
                {
                    // this method intentionally left blank
                }   
            });
    }

    // find the sprite that is both associated to a given body and in a given group
    public PhysicsSprite getPhysicsSprite(Body b, Group g)
    {
        Iterator<Sprite> spriteList = g.iterator(); // create a list to search through
        while ( spriteList.hasNext() ) 
        {
            Sprite item = spriteList.next(); // get the next Sprite in the list
            if (item instanceof PhysicsSprite && ((PhysicsSprite)item).getBody().equals(b) )
                return (PhysicsSprite)item;
        }
        return null;
    }

    public void update(int elapsedTime)
    {
        // restart application
        if (Input.isPressed(Input.KEY_R))
            load();

        // Update Physics.
        // This can be improved, see http://gafferongames.com/game-physics/fix-your-timestep/
        time += (elapsedTime / 1000f);
        while (time >= dt) 
        {
            physicsWorld.update(dt);
            time -= dt;
        }

        if ( Input.isPressed(Input.KEY_UP) ) // Pressed: only applies once (when pressed)
            playerSprite.addYVelocity( -20 );  
        if ( Input.isPressed(Input.KEY_DOWN) )
            playerSprite.addYVelocity( 10 );
        if ( Input.isPressed(Input.KEY_LEFT) )
            playerSprite.addXVelocity( -10 );
        if ( Input.isPressed(Input.KEY_RIGHT) )
            playerSprite.addXVelocity( 10 );

        String s = "Player data: ";
        s += "Position = [ " + df.format(playerSprite.x.get()) + " , " + df.format(playerSprite.y.get()) + " ]; "; 
        s += "Velocity = [ " + df.format( playerSprite.getXVelocity() ) + " , " + df.format(playerSprite.getYVelocity()) + " ] ";
        playerDataLabel.setText( s );    
    }
}
```

Second, the PhysicsSprite class:

```
import pulpcore.animation.Fixed;
import pulpcore.sprite.ImageSprite;
import pulpcore.image.*;
import pulpcore.animation.BindFunction;

import org.newdawn.fizzy.*;

public class PhysicsSprite extends ImageSprite 
{
    protected Body body;

    private static final float WORLD_TO_STAGE_SCALE = 10;

    public PhysicsSprite(String spriteName, String imageFileName, float x, float y)
    {
        super(imageFileName, x, y);
        setName(spriteName);
        setAnchor(0.5, 0.5);
    }

    public PhysicsSprite(String imageFileName, float x, float y)
    {
        super(imageFileName, x, y);
        setName("");
        setAnchor(0.5, 0.5);
    }

    public PhysicsSprite(String spriteName, CoreImage image, float x, float y)
    {
        super(image, x, y);
        setName(spriteName);
        setAnchor(0.5, 0.5);
    }

    public PhysicsSprite(CoreImage image, float x, float y)
    {
        super(image, x, y);
        setName("");
        setAnchor(0.5, 0.5);
    }

    public void setBody(Body b, World w) 
    {
        this.body = b;
        w.add( this.body );

        this.body.setPosition( (float)this.x.get() / WORLD_TO_STAGE_SCALE, 
            (float)this.y.get() / WORLD_TO_STAGE_SCALE );
        this.body.setRestitution(0);  // not bouncy   - inelastic
        this.body.setFriction(0.25f); // not slippery - "frictiony"?   

        // bind Sprite to Body
        this.x.bindTo(new BindFunction() 
            {
                public Number f() { return body.getX() * WORLD_TO_STAGE_SCALE;  }
            });

        this.y.bindTo(new BindFunction() 
            {
                public Number f() { return body.getY() * WORLD_TO_STAGE_SCALE;  }
            });

        this.angle.bindTo(new BindFunction() 
            {
                public Number f() { return body.getRotation(); }
            });
    }

    // automatically creates rectangular body based on image dimensions
    public void setBodyRectangle(boolean isDynamic, World w) 
    {
        Rectangle r = new Rectangle( (float)this.width.get() / WORLD_TO_STAGE_SCALE,
                (float)this.height.get() / WORLD_TO_STAGE_SCALE );

        Body b;        
        if ( isDynamic )
            b = new DynamicBody(r, (float)this.x.get() / WORLD_TO_STAGE_SCALE, 
                (float)this.y.get() / WORLD_TO_STAGE_SCALE);
        else
            b = new StaticBody(r, (float)this.x.get() / WORLD_TO_STAGE_SCALE, 
                (float)this.y.get() / WORLD_TO_STAGE_SCALE);

        setBody(b, w);
    }

    // automatically creates circular body based on image dimensions
    public void setBodyCircle(boolean isDynamic, World w) 
    {
        Circle c = new Circle( (float)this.width.get() / (2 * WORLD_TO_STAGE_SCALE) );

        Body b;        
        if ( isDynamic )
            b = new DynamicBody(c, (float)this.x.get() / WORLD_TO_STAGE_SCALE, 
                (float)this.y.get() / WORLD_TO_STAGE_SCALE);
        else
            b = new StaticBody(c, (float)this.x.get() / WORLD_TO_STAGE_SCALE, 
                (float)this.y.get() / WORLD_TO_STAGE_SCALE);

        setBody(b, w);
    }

    public Body getBody() 
    {   return this.body;   }

    public float getXVelocity()
    {   return this.body.getXVelocity();   }

    public float getYVelocity()
    {   return this.body.getYVelocity();   }

    public void setVelocity(float xVelocity, float yVelocity)
    {   this.body.setVelocity( xVelocity, yVelocity );   }

    public void setXVelocity(float xVelocity)
    {   this.body.setVelocity( xVelocity, this.body.getYVelocity() );   }

    public void setYVelocity(float yVelocity)
    {   this.body.setVelocity( this.body.getXVelocity(), yVelocity );   }

    public void addVelocity(float deltaX, float deltaY)
    {   this.body.setVelocity( this.body.getXVelocity() + deltaX, this.body.getYVelocity() + deltaY ); }

    public void addXVelocity(float deltaX)
    {   this.body.setVelocity( this.body.getXVelocity() + deltaX, this.body.getYVelocity() ); }

    public void addYVelocity(float deltaY)
    {   this.body.setVelocity( this.body.getXVelocity(), this.body.getYVelocity() + deltaY ); }

    public void setRestitution(float r)
    {   this.body.setRestitution(r);  }

    public void setFriction(float f)
    {   this.body.setFriction(f);  }

    public void setName(String s)
    {   super.setTag(s);  }

    public String getName()
    {   return (String)this.getTag();  }
}
```


# How do I print to the console? #
Besides `System.out`, you can also use:
```
import pulpcore.CoreSystem;
...
CoreSystem.print("Some message");
```
This method works well with the PulpCore Player in NetBeans (as of PulpCore 0.11.4). Within the app, you can see the console at any time by pressing Ctrl+C, but only when built with debug mode.

You may also want to use:
```
import pulpcore.Build;
...
if (Build.DEBUG) CoreSystem.print("Some message");
```
Which removes the statement when compiled in release mode.

# How do I get the alpha of Groups to behave like Photoshop's layers? #
Give the Group a back buffer:
```
import pulpcore.sprite.Group;
...
group.pack(); // Sets the size to the current child contents
group.createBackBuffer();
group.alpha.set(128); // Alpha is applied to the back buffer rather than each individual sprite
```

# Can I use Java 5 features? #
As of version 0.11.x PulpCore still supports Java 1.4 as the minimum, and Java 5 support comes from Retroweaver. Some users have reported problems with Retroweaver - YMMV. PulpCore 0.12 will require Java 5 as the minimum.

# Can I use Swing? #
Nope! But hey, there's always [JavaFX](http://javafx.com/) or [Project Scene Graph](https://scenegraph.dev.java.net/).

# Can I use Java2D? #
Sort of. PulpCore has it's own software renderer separate from Java2D. However, applets may use the [Java2DSprite](http://code.google.com/p/pulpcore/source/browse/contrib/Java2DSprite.java) if you need to.

Using PulpCore's tools, SVG files are converted to PNGs automatically at build time.

Why does PulpCore have it's own renderer?
**Originally, the software renderer was designed for Java 1.1, which did not have PNG support or translucent ARGB image support.** Today, the software renderer contains blend modes not found in Java2D. (Thus the cool Particles demo).
**Also, the software renderer has consistent performance across platforms and JREs. Some Java2D operations on older JREs create tons of temporary buffers, wasting CPU & memory. PulpCore's renderer does not.** The drawback is that the software renderer is raster-only, with limited vector graphics support.

# Does PulpCore run on the desktop? #
Not yet. An OpenGL port is planned for the future.

# Does PulpCore use native libraries? #
No. Using native code in the browser because it tends to 1) not work, 2) crash the browser, and/or 3) show a "trust" dialog. Not only are trust dialogs bad for the user, but developers making trusted apps tend to go nuts and do all sorts of things they shouldn't (write junk to the home folder, spend way too much time downloading stuff, force full screen without offering a way out, etc). The goal of PulpCore in the browser is to "just work." If it "just works" it's better for both the user and the developer.
# Deployment #
To deploy, copy the seven files to a path on your webserver, where "app-1.0" is the name of your app:
  1. index.html (or edit your existing HTML)
  1. pulpcore.js
  1. splash.gif
  1. app-1.0.jar
  1. app-1.0.jar.pack.gz
  1. app-1.0.zip
  1. play.gif (optional: only if you want your app to show a play button before loading)

The HTML file will contain something similar to the following:
```
<script type="text/javascript"><!--
pulpcore_width = 640;
pulpcore_height = 480;
pulpcore_archive = "app-1.0.jar";
pulpcore_assets = "app-1.0.zip";
pulpcore_scene = "MyScene";
//--> 
</script> 
<script type="text/javascript" src="pulpcore.js"></script>
<noscript><p>To play, enable JavaScript from the Options or Preferences menu.</p></noscript>
```

# Special pulpcore.js params #
These PulpCore params can be added to the HTML:
  * pulpcore\_name - The applet name, which shows in the status bar in some browsers.
  * pulpcore\_splash - The path to the loading splash. The default it "splash.gif".
  * pulpcore\_play\_splash - The path to the play button splash. The default it "play.gif". (PulpCore 0.11.4 and newer)
  * pulpcore\_start - Whether to start the applet. Either "true", (always start), "false" (don't start, instead show the play.gif button) or "auto" (only start the first applet on the page). The default is "auto". (PulpCore 0.11.4 and newer)
  * pulpcore\_bgcolor - The default background color for the  Java plugin (also used in the default loading screen).
  * pulpcore\_fgcolor - The default foreground color for the Java plugin (also used in the default loading screen).
  * pulpcore\_params - Custom applet parameters in JSON format. For example: `pulpcore_params = { id:5, username: "fred" };`

# Basic Advice #
  * Use the 'project' template. This is designed for professional releases, and is the boilerplate pulpgames.net uses.
  * Change the version number with every new release. See your build.properties file. If you don't change the version number, due to odd caching, clients could get the old code with a new asset zip file, or the new asset zip file with old code, which could result in a broken game. By changing the version number, you ensure:
    1. Users can always get the latest version simply by visiting the page again - no need to restart the browser.
    1. The new code always gets the new asset zip file.
  * Set up an UncaughtExceptionScene to send any exceptions that occur to the server, so you know if any problems happen.

# Optimization for size #
## Shrink the jar file ##
Java 6 is good with caching, so the jar download only happens the first time. However, the jar should be as small as possible so that new users can get to your app as fast as possible. Also, if you want to test the "dial-up experience", try [Sloppy](http://www.dallaway.com/sloppy/). More advice:

  * Use [ProGuard](http://proguard.sourceforge.net/). Set the `proguard.path` in your build.properties file and it is automatically used when `pulpcore.build = release` is set. ProGuard will reduce the the size of the jar by 50% or more. Milpa is 364KB before Proguard; 168KB after.
  * Use the release mode jar (by setting `pulpcore.build = release` in your project's build.properties file). Release mode cuts out debugging statements and extra stuff like the ConsoleScene and the SceneSelector. For Milpa, it reduces the jar by 12KB.
  * Don't include JOrbisAdapter.java if you're not using Ogg Vorbis files - the JOrbis library is a big chunk of a code.

## Shrink the assets file ##
PulpCore's asset tool creates PNG sizes comparable with other PNG optimizers like [OptiPNG](http://optipng.sourceforge.net/). However, there are still tricks you can do to make your assets smaller:
  * Use Ogg Vorbis instead of WAV files.
  * Reduce the number of colors in your PNGs
  * Avoid dithering in your PNGs. Large flat areas of color compress better.
  * Only include one font, and use `font.tint(myColor)` to create colored versions of that font.

# Ensuring Java 1.4 compatibility #
Java 1.4 is still used on 5-10% of machines out there, so it's important to keep Java 1.4 compatibility by not using any Java features found in Java 5 or newer. Using Generics and other Java 5 languages features is okay - Retroweaver can make those classes Java 1.4 compatible.
  * Get the 1.4 JDK, and set the `library.path` in your build.properties file to the path of the Java 1.4 rt.jar file. This makes ProGuard give you any warnings if you're using features found only in Java 5 or newer. ProGuard will complain about PulpCore using System.nanoTime(), but this is fine - PulpCore is smart enough to only use that method on Java 5+ runtimes.
  * Test on an actual Java 1.4 runtime if you can.
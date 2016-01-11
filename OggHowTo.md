# Playing Ogg Vorbis audio (PulpCore 0.11) #

To load Ogg Vorbis files:

  1. Get the JOrbis binary, jorbis-0.0.17.jar
    * Source is located here: http://www.jcraft.com/jorbis/
    * Binaries may also be found: http://www.google.com/search?q=jorbis+maven
  1. Drop jorbis-0.0.17.jar in your project's "lib" folder.
    * Use the "project" template from the PulpCore "templates" folder.
    * The "lib" folder is in the top-level of the project, just like the "src" and "res" folders.
  1. Drop [JOrbisAdapter.java](http://pulpcore.googlecode.com/hg/contrib/JOrbisAdapter.java) in your project's "src" folder.
    * Your IDE may prefer you put the file in "src/pulpcore/sound/".
  1. Load sounds like normal:
```
Sound sound = Sound.load("mysound.ogg");
sound.play();
```

Ogg Vorbis is fully integrated with PulpCore, so you can pause playback and set the level and pan in realtime, just like a regular `Sound`.











**Looking for more info? See also [Alex Jeffery's tutorial](http://blog.alexjeffery.org/pulpcore-tutorials/pulpcore-and-eclipse-hello-world-tutorial/).**

# About PulpCore Player #

PulpCore Player has two main features not available in Appletviewer:
  * Ability to reload the app when the code is modified.
  * Loading and saving user data using `CoreSystem.getUserData()` and `CoreSystem.putUserData()` works correctly.

Note, PulpCore Player does not install a SecurityManager. Always test applets in web browsers, too.

# Eclipse setup #

There are two steps; first, you need to create the project, next, you need to create a Run Configuration for this project.

Making a project for the BubbleMark example, using Eclipse Europa:

  1. Make sure you have Pulp Core downloaded. The Bubble Mark demo is in the "examples" sub-directory.
  1. File->New->Java Project
  1. Name is whatever you want.
  1. Choose the "Create Project from Existing Source" radio button, and browse to wherever you downloaded Bubble Mark.
    * (e.g.: C:\Program Files\Pulp Core\src\examples\BubbleMark\bin"
  1. Click "Next"
  1. Click the "Java Build Path" tab, and for libraries click "Add External JAR". Now, browse to the Pulp Core applet.
    * e.g.: C:\Program Files\Pulp Core\build\pulpcore-applet-debug-0.10.jar)
  1. Click "Ok"
  1. Build the project. There should be no errors.
  1. Go to the Bubble Mark's bin directory and zip up "ball.png" into an archive called "BubbleMark.zip"


Configuring the Run Dialog:

  1. Click Run->Open Run Dialog...
  1. Click "Java Applet" then click the New button ("New launch configuration")
  1. In the Main tab:
    * Set "Applet" to "pulpcore.platform.applet.CoreApplet"
    * Set "Applet viewer" to "pulpcore.player.PulpCorePlayer"
  1. In the Parameters tab:
    * Set "Width" to "500"
    * Set "Height to "300"
  1. In the Parameters tab, add extra parameters:
    * Set "scene" to "BubbleMark"
    * Set "assets" to "BubbleMark.zip" (unless you write your own LoadingScene)
  1. In the Arguments tab:
    * Set "Working directory" to the location of the "BubbleMark.zip" file --that is, the "bin" directory where you zipped up "BubbleMark.zip"
  1. In the Classpath tab:
    * Click "Bootstrap Entries", then click "Add External JARs...", then select the "pulpcore-player-0.10.jar" file.
    * In "User Entires", make sure the "pulpcore-applet-debug-0.10.jar" is included (Eclipse probably added this on import, but do double-check).
  1. Click "Run"

Now you can use Eclipse "Run" button to run the apps, and the integrated debugger works too. Also, if Project->"Build Automatically" is checked, the PulpCore Player can reload the app when the code changes, often showing the same Scene that was previously loaded.

# Reloading Scenes #
If a Scene has a public no-argument constructor, the PulpCore Player can automatically jump to that scene when the code is reloaded. To reload, in PulpCore Player click "File->Reload".

This eliminates the need to traverse through start up scenes (like the Title, Menu, and Game Scenes) just to get to later scene (like HighScoreScene) every time the code changes. The HighScoreScene can be automatically loaded - as long as it has a public no-argument constructor.


# Alternate Setup for a New Project #

If you want to build off an existing project, the previous sections will prove helpful to you. However, some users want to start from scratch every time. In that case, you should probably build from one of the Ant template files, like so:

  1. In Eclipse, choose File->New Project...
  1. Choose "Java Project From Existing Ant Buildfile..."
  1. Browse to the pulpcore "templates" directory. Choose, e.g., the Quick template's "build.xml" file
  1. Check the checkbox "Link to the buildfile in the file system"
  1. After pressing "Finish" execute the applet by running the ant build script.

Step 4 is necessary because it links in Pulp Core's custom ant tasks (e.g., ProGuard).


# Caveats #

  1. Zipping your resources will get you into trouble once you start using fonts. Assuming you are developing in Eclipse, there are basically two options:
    * Adding the jar to our build path will give you auto-completion; you can then "really" compile the example using ant from the command prompt.
    * If you want a truly integrated experience, you can configure the "Builders" to use the Ant build.xml file, and disable both the standard Java builder and auto-compilation. For small projects, just set the target to the default (build-and-run); you can now compile & test your application by pressing Ctrl+B.
  1. The steps listed should also work on Ganymede, but feel free to mail the list if something breaks.
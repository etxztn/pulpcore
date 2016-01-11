**New: Get the [PulpCore NetBeans 6 Module](http://code.google.com/p/pulpcorenb/)!** With the module, you don't need the instructions below.

# About PulpCore Player #

PulpCore Player has two main features not available in Appletviewer:
  * Ability to reload the app when the code is modified.
  * Loading and saving user data using `CoreSystem.getUserData()` and `CoreSystem.putUserData()` works correctly.

Note, PulpCore Player does not install a SecurityManager. Always test applets in web browsers, too.

# NetBeans Setup #
For the BubbleMark example:

  1. Click File->New Project...
  1. In the "Java" category, select "Java Project with Existing Ant Script", then click "Next >".
  1. In "Location:" click "Browse..." and select "<pulpcore path>\examples\BubbleMark".
  1. The other fields will automatically fill in. Click "Next >".
  1. The page "Build and Run Actions" is also automatically filled in. Click "Next >".
  1. In "Source Package Folders", click "Add Folder..." and select "<pulpcore path>\examples\BubbleMark\src".
  1. Click "Next >".
  1. Click "Add JAR/Folder..." and select "<pulpcore path>\build\pulpcore-applet-debug-0.10.jar".
  1. Click "Finish".

You can now use NetBeans' Build and Run actions.

# Reloading Scenes #
If a Scene has a public no-argument constructor, the PulpCore Player can automatically jump to that scene when the code is reloaded. To reload, in PulpCore Player click "File->Reload".

This eliminates the need to traverse through start up scenes (like the Title, Menu, and Game Scenes) just to get to later scene (like HighScoreScene) every time the code changes. The HighScoreScene can be automatically loaded - as long as it has a public no-argument constructor.

PulpCore Player can automatically reload when you click "Run" in NetBeans. (NetBeans' runs Ant in the same VM instance, unlike Eclipse). To enable this feature for all PulpCore apps:
  1. Select Tools->Options
  1. Select the "Miscellaneous" icon, then select the "Ant" tab.
  1. In the Properties box, enter (without the quotes) "pulpcore.player.wait=false".
  1. Click OK.

# NetBeans Debugger Setup #
If you'd like to use NetBeans' integrated debugger, follow these steps:
  1. Click File->"BubbleMark" Properties.
  1. Select the "Output" category.
  1. Click "Add JAR/Folder..." and select "<pulpcore path>\examples\BubbleMark\BubbleMark.jar".
  1. Click "OK".

The first time you run the debugger, NetBeans will ask you to edit an `ide-file-targets.xml` file. Copy & paste this code, which works with any PulpCore app:
```
<?xml version="1.0" encoding="UTF-8"?>
<project basedir=".." name="PulpCore-IDE">
    <target name="debug-nb">
        <path id="cp">
            <fileset dir="build"><include name="*.jar" /></fileset>
        </path>
        <nbjpdastart addressproperty="jpda.address" name="PulpCore" transport="dt_socket">
            <classpath refid="cp"/>
        </nbjpdastart>
        <java jar="${ant.home}/lib/ant-launcher.jar" fork="true">
            <arg value="run" />
            <jvmarg value="-Xdebug"/>
            <jvmarg value="-Xrunjdwp:transport=dt_socket,address=${jpda.address}"/>
        </java>
    </target>
</project>
```
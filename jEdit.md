# About PulpCore Player #

PulpCore Player has two main features not available in Appletviewer:
  * Ability to reload the app when the code is modified.
  * Loading and saving user data using `CoreSystem.getUserData()` and `CoreSystem.putUserData()` works correctly.

Note, PulpCore Player does not install a SecurityManager. Always test applets in web browsers, too.

# jEdit Setup #
[Download jEdit](http://www.jedit.org/index.php?page=download)

Install and setup the Ant Farm plugin:
  1. Click Plugins->Plugin Manager...
  1. In the Install tab, select "Ant Farm". You may also like JavaSideKick and BufferTabs.
  1. Click "Install". After installing, click "Close".
  1. Click "Plugins->Ant Farm->Ant Farm". You may want to dock Ant Farm by clicking the down arrow and selecting "Dock at Left".
  1. Click Plugins->Plugin Options...
  1. Click Ant Farm -> Build Options
  1. Select "Run targets in the same JVM"
  1. Click Ant Farm -> Properties
  1. In "Name" enter "pulpcore.player.wait", and in "Value" enter "false".
  1. Select "Do not prompt for properties when running targets"


For the any PulpCore app:
  1. Open any PulpCore build.xml file in jEdit. It automatically loads in Ant Farm.
  1. In Ant Farm, click the build file name and then click the Run icon (or, double-click the build file name).

# Reloading Scenes #
If a Scene has a public no-argument constructor, the PulpCore Player can automatically jump to that scene when the code is reloaded (when your run the Ant file).

This eliminates the need to traverse through start up scenes (like the Title, Menu, and Game Scenes) just to get to later scene (like HighScoreScene) every time the code changes. The HighScoreScene can be automatically loaded - as long as it has a public no-argument constructor.
# PulpCore vision and goal #
_Keep it simple - Make PulpCore a really great boilerplate, not a kitchen sink._

# About version numbers #
Version numbers are in the form _major.minor.revision_. The _minor_ version is incremented whenever there is a large enough change internally or to the API. For example, version 0.10 removed Java 1.1 support from version 0.9.

A new _revision_ is released, on average, once or twice a month.

# Plans for 0.11 #
  * ~~Property bindings~~ **Done!**
  * ~~Rendering onto translucent surfaces~~ **Done!**
  * ~~Make Scene2D and Group thread-safe~~ **Done!**
  * ~~Refactor to allow ProGuard to remove unused blend modes~~ **Done!**
  * ~~Color refactoring, including a Color property~~ **Done!**
  * ~~Sprites know their parent Group (fixes a few bugs, allows for some new features)~~ **Done!**
  * ~~Automatic SVG to PNG conversion in the asset pipeline~~ **Done!**
  * ~~Groups with a back-buffer~~ **Done!**
  * ~~Sprite intersection tests, including pixel-level~~ **Done!**
  * ~~Maybe: scroll pane UI component~~ **Done!**
  * ~~[Nine-Patch stretchable image](http://code.google.com/android/reference/available-resources.html#drawables)~~ **Done!**
  * ~~pulpcore.js - allow multiple applets per page~~  **Done!**
  * Realtime image filters (Coming in 0.11.5)
  * Maybe: More blend modes
  * More documentation
  * Receive ideas and feedback from the community.

# Plans for 0.12 #
  * Require Java 5 as the minimum, and use Java 5 language features.
  * Refactor to allow immutable images (currently all images are mutable).
  * Maybe: New platform: Android SDK.
  * Maybe: Provide easy, unified way to theme Buttons, checkbox Buttons, TextFields, Sliders, and ScrollPanes.
  * Maybe: font kerning (waiting for Apple's Java 6 to support kerning).
  * Maybe: multi-resolution sprites (mip-mapping).
  * Maybe: Maven2 support
  * Maybe: More image filters that didn't make it into 0.11 (ideas: blur, motion blur, radial blur, glow, drop shadow, hue adjust, gamma adjust, contrast adjust, inverse, etc.)

# Someday/Maybe #
  * New platform: LWJGL for the desktop. Create Windows executables.
  * New platform: JOGL/JOAL for the desktop and/or mobile devices.
  * APNG support (see http://wiki.mozilla.org/APNG_Specification and http://littlesvr.ca/apng/ ) Probably wait until there is a Photoshop plugin for APNG.

# Towards 1.0 #
One of the goals of PulpCore is to have an identical programming model for different platform targets. Thus no code changes would be required for an Applet version, a Desktop version, and a Mobile version. Currently only the Applet platform exists - PulpCore will considered a candidate for 1.0 when at least two platform targets exist.
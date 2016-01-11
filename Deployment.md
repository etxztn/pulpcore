# Introduction #
The pulpcore.js loader has four features:
  1. It shows a loading splash while the JRE is booting and the PulpCore application jar is downloading.
  1. It provides persistent storage for PulpCore apps (via browser cookies and LiveConnect).
  1. It provides workarounds for many browser issues, like pop-up messages on IE.
  1. It can provide Java installation dialogs for users with a JRE older than 1.4 (or no JRE at all).

# Tests #

Browsers tested (listed in order of popularity, pulpgames.net, May 2007):
  * IE6 (58.6%)
  * IE7 (20.5%)
  * Firefox 2 (10.9%)
  * Firefox 1.5 (3.1%)
  * Opera 9 (2.4%)
  * Safari 2 (1.3%)

Tests include:
  * Java 1.4.2, Java 5, Java 6, No Java
  * Externally framed site
  * Aggressively clicking refresh/reload
  * Running two games at the same time in the same browser instance.

# Design Decisions #

  * Only 1% percent of hits to pulpgames.net had Java 1.1. This meant the 

&lt;object&gt;

 tag could be used instead of the 

&lt;applet&gt;

 tag without affecting the vast majority or visitors (PulpCore was later updated to require 1.4 as the minimum).
  * IE: Using the 

&lt;object&gt;

 tag is a better user experience if the user doesn't have Java (the Java installer is automatically started)
  * IE: To avoid the "Click to interact" message, the 

&lt;object&gt;

 tag is created in JavaScript.
  * IE6: To avoid the "Click OK to continue loading the context of this page" message, the 

&lt;object&gt;

 tag is insert via innerHTML.
  * IE + Java 5: To avoid page flicker and loading two applets at once, the 

&lt;object&gt;

 tag is used instead of the 

&lt;applet&gt;

 tag.
  * IE: To correctly load if the page is externally framed, the "codebase" is automatically specified.
  * Firefox: Java detection is used to prevent clients with a JRE older than 1.4 from running PulpCore apps.

# Known Issues #
LiveConnect does not work in these situations:
  * Opera 9 on Mac OS X
  * Safari 3 on Windows
  * IE6 if the site is externally framed. For example, if SomeGamePortal.com creates a frameset that contains MyGameSite.com/MyGame, LiveConnect does not work.
# Introduction #

Most developers won't need to edit the PulpCore code itself: the distribution includes pre-built binaries, and [nightly builds](http://www.pulpcore.org/download/nightly/) are available. But if you need to edit the code for debugging, or you just want to contribute some code, this document will help you get started.

# Requirements #
  * An SVN client (to download the code).
  * The [JDK 1.4](http://java.sun.com/j2se/1.4.2/download.html) runtime jar (to make sure PulpCore runs on Java 1.4).
  * JDK 1.5 or newer (To build the PulpCore tools).
  * [Retroweaver 2.0.5](http://retroweaver.sourceforge.net/)
  * [SVG Salamander](https://svgsalamander.dev.java.net/)
  * [Apache Ant 1.7](http://ant.apache.org/) (included in most IDEs)
  * [Java Pack200 Ant Task](https://java-pack200-ant-task.dev.java.net/)

# Optional #
  * [ProGuard 3.10](http://proguard.sourceforge.net/)
  * [JUnit 4.3](http://junit.org/) or newer.
  * [Mozilla Rhino](http://www.mozilla.org/rhino/) (for running JSLint on pulpcore.js).

# Get the code from SVN #
The previous version 0.10 has it's own branch, and can be acquired with this command:
```
svn checkout http://pulpcore.googlecode.com/svn/branches/0.10/ pulpcore-read-only
```
The more stable version 0.11 is in the trunk and can be acquired with this command:
```
svn checkout http://pulpcore.googlecode.com/svn/trunk/ pulpcore-read-only
```
See http://code.google.com/p/pulpcore/source for details.

# Building #
  1. Edit the build.properties file with the local paths on your system.
  1. Run the "build" task with Ant.
If you get an OutOfMemoryError, set the ANT\_OPTS environment variable to increase the maximum heap space.

Most Unix shells:
```
export ANT_OPTS=-Xmx256m
```
Windows:
```
set ANT_OPTS=-Xmx256m
```

# Submitting Patches #
Currently there is no formal way to submit patches, but for now:

  * [Submit](http://code.google.com/p/pulpcore/issues/list) an issue (and attach your diff). It will show up on the Discussion Group, too.
  * Or, send [me](http://groups.google.com/groups/profile?enc_user=SNAhQRIAAACrqaZUIUh8-JBALTwjJ8-F8rhlH0Pnl47z4AZhN98BFg) email.
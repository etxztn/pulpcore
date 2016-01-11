# Introduction #
This file contains the documentation for PulpCore's Ant tasks used in the default build scripts, aimed towards developers who are creating custom build scripts or need to fine-tune asset conversion.

Using the default build scripts, all you need to do is drop your assets in the "res" folder (project template) or the "src" folder (quick template) and the default build scripts handle it.

## Setup ##
To define the tasks, use:
```
<taskdef resource="tasks.properties" classpath="${pulpcore.path}/pulpcore-assettools-0.11.jar"/>
```


# Assets Task #
## Description ##
Copies a directory of assets and converts images and sounds as needed.

For each file, if the destination file exists and had a modified date greater than or equal to the source file, no action is taken. Therefore, this task only copies/converts assets that have changed since the last time this task was used.

The following file types are passed to the Image task: PNG, GIF, BMP, SVG, and SVGZ. JPEG images are not sent to the image task, and instead are copied as-is. If a GIF file is an animated GIF, only the first frame is used.

For each image, the Assets tasks checks if a coresponding properties file exists with the same name of the image, but with a .properties extension. For example, for `res/player_left.png`, the corresponding file `res/player_left.properties` contains the image metadata.

Files ending in ".font.properties" are sent to the Font task.

WAV files are sent to the Sound task.

## Parameters ##
| **Attribute** | **Description** | **Required** |
|:--------------|:----------------|:-------------|
| srcDir        | The source directory to copy from. | Yes.         |
| destDir       | The destination directory to copy to. | Yes.         |
| optimizationLevel | Sets the optimization level for PNG compression from 0 (none) to 5 (max). Higher levels take considerably more time to compress.   | No; defaults to 2. |
| skipSourceFiles | Indicates whether Java source files (.java, .scala) are skipped (not copied to the destination). | No; defaults to true. |

## Examples ##
```
<pulpcore-assets srcDir="${res}" destDir="${build}/res" optimizationLevel="1" />
```

# Image Task #
## Description ##
Converts an SVG, GIF, or BMP image to an optimized PNG or optimizes an existing PNG. Optionally, metadata (hotspot, animation info) specified by a [.properties file](PropertyFiles.md) is added to the PNG.

This task is called automatically from the Asset task, but can also be used for individual files.

The Image task creates legal PNG files specifically for PulpCore's PNG reader. PulpCore was originally written for Java 1.1 - which does not have PNG support - so PulpCore needed its own PNG reader. This reader is still in use today because it is small, fast, and can easily extract metadata (animation info) from the PNG files.
## Parameters ##
| **Attribute** | **Description** | **Required** |
|:--------------|:----------------|:-------------|
| srcFile       | The source image file. | Yes.         |
| destFile      | The name of destination file. | Yes.         |
| srcPropertyFile | The property file containing image metadata. | No.          |
| optimizationLevel | Sets the optimization level for PNG compression from 0 (none) to 5 (max). Higher levels take considerably more time to compress.   | No; defaults to 2. |

## Examples ##
```
<pulpcore-image srcFile="${res}/myimage.gif" destFile="${build}/myimage.png" />
```

```
<pulpcore-image srcFile="${res}/player.png" srcPropertyFile="${res}/player.properties" destFile="${build}/player.png" optimizationLevel="5" />
```

# Font Task #
## Description ##
Creates a PNG font from a [.font.properties file](PropertyFiles.md).
## Parameters ##
| **Attribute** | **Description** | **Required** |
|:--------------|:----------------|:-------------|
| srcFile       | The source .font.properties file. | Yes.         |
| destFile      | The name of destination PNG file. | Yes.         |
| optimizationLevel | Sets the optimization level for PNG compression from 0 (none) to 5 (max). Higher levels take considerably more time to compress.   | No; defaults to 2. |

## Examples ##
```
<pulpcore-image srcFile="${res}/title.font.properties" destFile="${build}/title.font.png" />
```

# Sound Task #
## Description ##
Converts a WAV file to PCM, signed, 16-bit, little-endian, mono or stereo. Valid sample rates are 8000Hz, 11025Hz, 22050Hz, and 44100Hz. If the WAV is already in a valid format, it is copied to the destination.

If the WAV could not be converted to a valid format, a BuildException is thrown.

This task is called automatically from the Asset task, but can also be used for individual files.

## Parameters ##
| **Attribute** | **Description** | **Required** |
|:--------------|:----------------|:-------------|
| srcFile       | The source sound file. | Yes.         |
| destFile      | The name of destination file. | Yes.         |

## Examples ##
```
<pulpcore-sound srcFile="${res}/mysound.wav" destFile="${build}/mysound.wav" />
```
# Applet Task #
## Description ##
Creates the index.html file based on a template, and copies the pulpcore.js file to the destination.
## Parameters ##
| **Attribute** | **Description** | **Required** |
|:--------------|:----------------|:-------------|
| destDir       | The destination path. | Yes.         |
| archive       | The jar file name. | Yes.         |
| scene         | The fully qualified class name of the first Scene to display | Yes.         |
| assets        | The assets file name (zip file) to load automatically. If defined, the default LoadingScene is shown before the first scene is shown. If not defined, the first Scene needs to load the project's assets, usually by subclassing the default LoadingScene. | No; none by default. |
| width         | The applet width. | No; 640 by default. |
| height        | The applet height. | No; 480 by default. |
| params        | Extra applet parameters in JSON syntax | No; none by default. |
| codebase      | The URL containing the archive. | No; determined at runtime by default. |
| bgcolor       | The applet background color in hexadecimal color format. | No; "#000000" by default. |
| fgcolor       | The applet foreground color in hexadecimal color format. | No; "#aaaaaa" by default. |
| splash        | The image file to use as a splash image while the applet loads. This may either be a path to file on the local system (relative to the Ant file's basedir), or an http:// URL. | No; an animated splash.gif is provided by default. |
| template      | The filename of the HTML template (relative to the Ant file's basedir) . | No; a HTML template is provided by default. |
| displaySource | The source file to display inline on the HTML page. Links to files in the same directory of the source file are also provided in the HTML. | No; none by default. |

## HTML template ##
The HTML template needs to contain, at minimum, a @APPLET\_PARAMS@ tag. Other optional tags are @BGCOLOR@, @FGCOLOR@, @TITLE@ (from the archive name), and @SRC@ (the source code from the `displaySource` parameter).

Example:
```
<html>
<head>
    <title>@TITLE@ - Powered by PulpCore</title>
</head>
  <body style="background: @BGCOLOR@; color: @FGCOLOR@">
<div>
<script type="text/javascript"><!--
@APPLET_PARAMS@
//--> 
</script> 
<script type="text/javascript" src="pulpcore.js"></script>
<noscript><p>To play, enable JavaScript from the Options or Preferences menu.</p></noscript>
</div>

<blockquote>
@SRC@
</blockquote>
</body>
</html>
```

## Examples ##
```
<pulpcore-applet 
destDir="${build}"
archive="HelloWorld.jar"
scene="HelloWorld"
assets="HelloWorld.zip"
displaySource="HelloWorld.java" />
```

```
<pulpcore-applet 
destDir="${build}"
archive="MyProject.jar"
scene="MyLoadingScene"
width="800"
height="600"
template="template.html"
splash="myslpash.gif"
params="text: 'Hello', color: '#0099ff' " />
```
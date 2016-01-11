# Introduction #

PulpCore's AntTasks allow metadata (hotspot info, animation info) to be attached to PNG files, and allows creation of PNG fonts.

# All property files #
All property files allow color reduction to be included in the asset conversion step. Also, the Asset task will stop execution if any property file has an unknown property, so typos can be detected at compile time.

| **Property** | **Description** | **Required** |
|:-------------|:----------------|:-------------|
| colors       | Reduces the number of colors in the output PNG files to the specified number. Typically 256 or less, although values greater than 256 are allowed. | No; by default no color reduction takes place. |

# Image Hotspots #
An image hotspot allowed fine-grain control where the image is drawn relative to its (x, y) location. By default,
the hotspot is (0,0), meaning the ImageSprite is displayed down and to the right of it's (x, y) location. Rotation also occurs around to the hotspot.
| **Property** | **Description** | **Required** |
|:-------------|:----------------|:-------------|
| hotspot.x    | The hotspot x location within the image. | No; by default, 0. |
| hotspot.y    | The hotspot y location within the image. | No; by default, 0. |

## Example ##
For a `cursor.png` image, the corresponding `cursor.properties` might be:
```
# Arrow image is 32x32, the arrow's hotspot is at (12, 12)
hotspot.x = 12
hotspot.y = 12
```

# Animated Images #
Images can be animated by supplying a tiled image and a property file specifying how to interpret that image. The image contains multiple "frames" laid out on a tiled grid. For example for a 160x32 PNG image, if there are 5 frames across, each frame has a size of 32x32.

| **Property** | **Description** | **Required** |
|:-------------|:----------------|:-------------|
| frames.across | The number of frames horizontally in the image. | No; by default 1. Either frames.across or frames.down must be greater than 1 to enable animation.  |
| frames.down  | The number of frames vertically in the image. | No; by default 1. Either frames.across or frames.down must be greater than 1 to enable animation. |
| frame.sequence | A comma-separated list of the sequence to display the frames. | No; by default the sequence is the same as it appears in the source image file. |
| frame.duration | A comma-separated list of the duration, in milliseconds, of each displayed frame. Must be the same length as frame.sequence if it is specified. | No; defaults to 100ms per frame. |
| loop         | Specifies wether the animation loops continuously. | No; defaults to false. |

## Example ##
For this [player image](http://pulpcore.googlecode.com/svn/trunk/examples/Images/src/player.png):
```
# Number of physical frames
frames.across = 5

# Hotspot location (in each frame)
hotspot.x = 16
hotspot.y = 16

# Frame sequence. Physical frames are numbered from 0 to (numFrames-1)
frame.sequence =   2,  3,  4,  3,   2,  1,  0,  1
# Frame duration in milliseconds (different duration for each frame)
frame.duration = 100, 75, 75, 75, 100, 75, 75, 75
loop = true
```

To see it in action, visit http://www.interactivepulp.com/pulpcore/images/

# Fonts #
Font files are created from a .font.properties file.

All colors are in hexadecimal format and may contain alpha. For example, #000000 is opaque black, and #7f000000 is half-transparent black.

| **Property** | **Description** | **Required** |
|:-------------|:----------------|:-------------|
| chars        | Comma-separated list of the characters to include in the font. Ranges of characters can be specified in the format "<first char>-<last char>". For example, "A-Z". | No; defaults to Basic Latin. |
| family       | The font family (installed on the system), a TTF name (in the same directory as the .font.properties file), or a Java logical font (Serif, Sans-serif, Monospaced, Dialog, and DialogInput). For a comma-separated list of font family names, the first font found on the system is used. | No; defaults to "Dialog". |
| size         | The font point size. | No; defaults to 16. |
| style        | The font style, either "plain", "bold", "italic", or "bold italic". | No; defaults to "plain". |
| antialias    | Font anti-aliasing. If set to false, the stroke and shadow is ignored. _Since 0.11.4_ | No; defaults to true. |
| monospace    | Specifies wether to force all character bounds to the same width. | No; defaults to false. |
| monospace.numerals | Specifies wether to force all numerals (0 - 9) bounds to the same width. This can be useful for score displays. | No; defaults to false. |
| tracking     | The font tracking (spacing between each character, in pixels). | No; defaults to 0. |
| bearing.left.

&lt;char&gt;

 | The left-side bearing of a specific character. For example, bearing.left.W = -2 | No; defaults to 0 for each character. |
| bearing.right.

&lt;char&gt;

 | The right-side bearing of a specific character. For example, bearing.right.Y = -5 | No; defaults to 0 for each character. |
| color        | The font color in hexadecimal color format. | No; defaults to black (#000000). |
| gradient.color | The color to blend to in hexadecimal color format. The gradient is blended from 'color' at the top and 'gradient.color' at the bottom. | No; default is not defined (no gradient). |
| gradient.offset | The vertical gradient offset, in pixels. | No; defaults to 0. |
| stroke.color | The stroke color in hexadecimal color format. | No; default is not defined (no stroke). |
| stroke.size  | The stroke size, in pixels. | No; defaults to 0. |
| shadow.color | The shadow color in hexadecimal color format. | No; default is not defined (no shadow). |
| shadow.blur  | The shadow blur factor, typically from 0 (no blur) to 5 (strong blur) although there is no upper limit. | No; default is 0 (no shadow blur). |
| shadow.x     | The shadow x offset, in pixels. | No; default is 0. |
| shadow.y     | The shadow y offset, in pixels. | No; default is 0. |

## Example ##
A simple serif font:
```
family = Adobe Caslon Pro, Times New Roman, serif
size = 24
color = #000000
```

A complex font:
```
family = Pigiarniq Bold.ttf
size = 80
style = bold

# If monospace.numerals is true, all digits 0-9 have the same width.
monospace = false
monospace.numerals = true

# Character set
# For Unicode tables, see http://unicode.org/charts/
# Default: Basic Latin
#chars = \u0020-\u007e
# Basic Latin and Latin-1 Supplement
#chars = \u0020-\u007e, \u00a1-\u00ff
# Just numbers and English letters
#chars = 0-9, A-Z
# Basic Latin, combining marks, and curly quotes
chars = \u0020-\u007e, \u0300-\u030a, \u2018-\u201f

# Color. All colors can have alpha.
color = #ffaa00

# Vertical gradient from the main color to this color.
gradient.color = #ff0000
gradient.offset = 30

# Stroke
stroke.color = #ffffff
stroke.size = 2

# Shadow 
shadow.x = 0
shadow.y = 2
shadow.color = #7f000000
shadow.blur = 3

# Tracking & bearing
tracking = 0
bearing.right.W = -2
bearing.right.Y = -5
# Example bearing for any Unicode character
#bearing.left.\u002e = 1

```
To see it in action, visit http://www.interactivepulp.com/pulpcore/text/
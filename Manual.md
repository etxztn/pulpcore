# Introduction #
PulpCore is an easy-to-use API designed to create modern-looking 2D web games with a strong focus on a positive user experience.

# PulpCore Setup #
First, download the distribution archive: http://code.google.com/p/pulpcore/downloads/list

All PulpCore examples have an [Ant](http://ant.apache.org/) build file with "build" and "run" targets. You can copy these build files from the "templates" folder in the distribution archive.

You can either use Ant from the command line, or setup PulpCore in [NetBeans](NetBeans.md), [Eclipse](Eclipse.md), or [jEdit](jEdit.md).

# Core Concepts #
  * **Stage** - The [Stage](http://www.interactivepulp.com/pulpcore/api/pulpcore/Stage.html) is where an app is displayed. There is only one Stage per app.
  * **Scene** - The Stage handles updating and drawing a [Scene](http://www.interactivepulp.com/pulpcore/api/pulpcore/scene/Scene.html). An app can have many Scenes, but only one Scene can be active at a time. Typically scenes for a game might include Title, Menu, Options, and Game. PulpCore provides the [Scene2D](http://www.interactivepulp.com/pulpcore/api/pulpcore/scene/Scene2D.html) class that manages sprites and layers, and renders using dirty rectangles for efficiency.
  * **Sprite** - A Scene2D typically contains many sprites, from a static background to an animated character. The base [Sprite](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/Sprite.html) class is abstract. Common sprites for games are [ImageSprite](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/ImageSprite.html) and [FilledSprite](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/FilledSprite.html). PulpCore also includes a few widget sprites, [Button](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/Button.html), [Label](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/Label.html), and [TextField](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/TextField.html). Sprites are rendered in the order that they were added to the Scene2D.
  * **Property** - Each sprite has several properties, including: x, y, width, height, angle, and alpha. Each property is either an [Int](http://www.interactivepulp.com/pulpcore/api/pulpcore/animation/Int.html) property or an [Fixed](http://www.interactivepulp.com/pulpcore/api/pulpcore/animation/Fixed.html) property (similar to a float) that can be animated over time. The [Timeline](http://www.interactivepulp.com/pulpcore/api/pulpcore/animation/Timeline.html) class is provided to group together several property animations.
  * **Layer** - The Scene2D class can manage multiple sprite layers, AKA the [Group](http://www.interactivepulp.com/pulpcore/api/pulpcore/sprite/Group.html) class. Typical layers might be the background, the game elements, and the heads-up display.
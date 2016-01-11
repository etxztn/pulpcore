# Introduction #
Here are a few ideas for volunteer projects. If you'd like do one of these, or you have an idea not mentioned here, get started and post a message on the discussion group!

## Spread the word ##
  * Publish your games and applets! The best promotion is going to come from great-looking examples.
  * Blog! Anything from a kind word to an in-depth tutorial.

## Participate ##
  * [Join the discussion group](http://groups.google.com/group/pulpcore)
  * [Report any issues](http://code.google.com/p/pulpcore/issues/list)

## Vote for Java bugs and RFEs ##
  * [Use SSE/MMX instructions to improve rendering performance on x86/x64 systems](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6766336)
> > This would improve performance of the software renderer.
  * [2D needs a way to synchronize onscreen rendering with vertical retrace](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6378181)
> > This would allow applets to have smoother animation.
  * [RFE: hardware acceleration for advanced blending modes (aka PhotoComposite)](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6541868)
> > This would allow Java2D to have fast additive and multiplicative blending (great for special effects).
  * [RFE: Render Capabilities API](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6655124)
> > Combined with PhotoComposite, this would allow PulpCore to use hardware acceleration where it's available.
  * [Java tray icon should be hidden by default](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6694710)
> > Java 6u10 makes this problem worse since it's possible to show several tray icons per browser. Users don't need it, so it should be hidden by default.
  * ~~[Regression: AccessControlExceptions introduced in 1.6.0\_03 on Firefox during LiveConnect calls](http://bugs.sun.com/view_bug.do?bug_id=6669818)~~
> > ~~PulpCore's workaround for this bug disables LiveConnect, which prevents the user-data feature from working on Firefox.~~ Fixed in 6u10.
  * ~~[Java Plug-In does not obey crossdomain.xml file directives](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6676256)~~
> > ~~This would allow certain web services to be accessed from unsigned applets.~~ Fixed in 6u10.

## Write some JavaDocs ##
If you can add JavaDoc to just one java file, great! Pick a file in SVN, doc it, and send it my way.

## Create a New Blend Mode ##
PulpCore 0.11 has an architecture that allows ProGuard to remove blending code that isn't used by an app, so essentially PulpCore could benefit from more blend modes without increasing the size of apps that don't use them. Currently PulpCore has all Porter-Duff modes, along with Add and Mulitply. Others could be:
  * Overlay
  * Screen
  * Any others you could think of.

See the [code in SVN](http://pulpcore.googlecode.com/svn/trunk/src/pulpcore/image/) to see how blending is implemented. It involves writing four functions and some cut & paste code.

## Create an Eclipse plugin ##
Eric Berry has created a [NetBeans 6.0 module](http://code.google.com/p/pulpcorenb/). It might also be beneficial to create an Eclipse plugin, so an Eclipse developer could quickly create a new project without fiddling with a bunch of dialogs. When creating a new project, a developer should have to make only 3 choices:

  1. Where to store a new project on disk.
  1. The name of the new project.
  1. Whether to use the "quick" template or the "project" template.

Ideally someone familiar with the inner-workings of Eclipse should take on this project. You can make the choice on whether it would be better to create an IDE plugin or a standalone wizard-style app.

## Create a UI Component ##
PulpCore has the basics: Buttons, Labels, ScrollPanes and TextFields. As of 0.11, buttons can be themed with SVG. Here's some other ideas:

  * TextArea
  * Drop-down boxes.

## Replace the Old Color Quantizer ##
The current asset pipeline has a option for automatic color reduction of fonts and PNGs. The idea is to let developers quickly reduce the size of images, thus shrinking the download size. To do this for, say, "background.png", you just create a "background.properties" file with the line: "colors=512" or some other number.

The current color reduction algorithm doesn't always produce good-looking results and requires a huge amount of memory to run, so it needs a new algorithm. Wu's color quantizer would look a lot better.

What you would do:
  1. Start with Wu's Color Quantizer: http://www.ece.mcmaster.ca/~xwu/cq.c
  1. Convert it to 4-dimensions (A, R, G, B) instead of 3 (R, G, B). The "weight" of the alpha might need to be 50%, and the weight of the others might need to be 16.67%. You'll have to experiment.
  1. Integrate with ConvertFontTask.java and ConvertImageTask.java in http://pulpcore.googlecode.com/svn/trunk/tools/assettools/
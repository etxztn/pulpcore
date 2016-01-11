Although tools such as Netbeans and Eclipse are quite powerful, this short guide will hopefully help increase the audience of PulpCore users to those who use BlueJ (from http://bluej.org: "an integrated Java environment specifically designed for introductory teaching") and wish to avoid ANT scripts.

## Setting up PulpCore in BlueJ ##

  * Download the pulpcore zip file from the "downloads" section, and extract all files.
  * In BlueJ, from the menu bar choose: "Tools" --> "Preferences...", to bring up the "BlueJ: Preferences" window. In this window, click the "Libraries" tab.
  * Click the "Add" button, and a new window will appears ("Select directory or jar/zip file").  In this window, find the directory where you extracted the pulpcore files. Within this directory, inside the "build" directory, select the file "pulpcore-applet-release.jar", and click the "Open" button. The "Select..." window will close.
  * Back in the User Libraries list in the "BlueJ: Preferences" window, there will be a new entry; in the "Location" column you will see the path to the "pulpcore-applet-release.jar" file, and in the "Status" column you will see the phrase "Not Loaded".
  * In the preferences window, click "OK". A window will pop up with a helpful message ("These changes you have made..."; click "OK".
  * Quit the BlueJ program, and then restart BlueJ.  (This enables the new JAR library to load.)

## (Yet Another) Hello World program for PulpCore ##

Credit where credit is due:
This example is amalgamated form the tutorials from http://www.alexjeffery.org (old code updated to work with PulpCore 0.11), and http://cloningtheclassics.com/ .

  * In the BlueJ main window, click the "New Class..." button.  Name the class PulpyDemo.
  * In the source code window that appears, delete all the preset code, and replace it with the following code:

```
import pulpcore.Stage; 
import pulpcore.scene.Scene2D; 
import pulpcore.image.Colors; 
import pulpcore.image.CoreFont; 
import pulpcore.image.CoreGraphics; 
import pulpcore.image.CoreImage; 
import pulpcore.sprite.Label; 
import pulpcore.sprite.Sprite; 
import pulpcore.sprite.FilledSprite; 
import pulpcore.sprite.ImageSprite; 

public class PulpyDemo extends Scene2D 
{ 
    FilledSprite background; 
    Label helloLabel; 
    ImageSprite logoImage; 

    public void load() 
    { 
        // Calculate coordinates of applet center 
        int centerX = Stage.getWidth() / 2; 
        int centerY = Stage.getHeight() / 2; 

        // Create a background 
        background = new FilledSprite(Colors.YELLOW); 
        add(background); 

        // Build a new label 
        helloLabel = new Label(CoreFont.getSystemFont(), "Hello World!", centerX, 40); 

        // Center the label horizontally and vertically 
        helloLabel.setAnchor(0.5, 0.5); 

        // Add the label to the scene 
        add(helloLabel); 

        // Load an image 
        logoImage = new ImageSprite(CoreImage.load("logo.png"), centerX, centerY); 

        // Center the image 
        logoImage.setAnchor(ImageSprite.CENTER); 

        // Add the image to the screen 
        add(logoImage); 
    } 

    public void update(int elapsedTime) 
    { 
        // this method has been intentionally left blank 
    } 
} 
```

  * Compile the code.


## Running the applet ##

Because PulpCore uses a JavaScript file to help load, we will not use BlueJ's built-in AppletViewer.  Instead, we will set up the necessary files manually.

  * First, make a copy of the "pulpcore-applet-release.jar", rename it to "myArchive.jar", and place it in your BlueJ project directory. Open the JAR file using a program such as 7zip or WinRAR. Copy the "PulpyDemo.class" file from your BlueJ project directory into the JAR file. Close the JAR file.
  * Create a new ZIP file (again, using either 7zip or WinRAR, for example) called "myAssets.zip".  This is where all program assets (images, sounds, etc.) should be placed.  For the demo program above to work correctly, this ZIP file needs to contain an image file named "logo.png".
  * Copy the files "splash.gif" and "pulpcore.js" into your directory.
  * Finally, open your favorite text editing program (such as Notepad++), and create a file called "index.html"; this will be the webpage that loads the applet.  Use the following code:

```
<html> 
<head> 
<title>PulpCore Demo</title> 
<body> 

<div id="game"> 
<script type="text/javascript"> 
<!-- 
pulpcore_width = 450; 
pulpcore_height = 250; 
pulpcore_archive = "myArchive.jar"; 
pulpcore_assets = "myAssets.zip"; 
pulpcore_scene = "PulpyDemo"; 
pulpcore_splash = "splash.gif"; 
//--> 
</script> 
<br /> 
<script type="text/javascript" src="pulpcore.js"></script> 
<br /> 
<noscript> 
<p>To play, enable JavaScript from the Options or Preferences menu.</ 
p> 
</noscript> 
</div> 

<div id="source"> 
<center> 
    Created with <a href="http://www.interactivepulp.com/ 
pulpcore/">PulpCore</a> 
</center> 
</div> 

</body> 
</html> 
```

  * Provided the files myArchive.jar, myAssets.zip, splash.gif, pulpcore.js, and index.html are all in the same directory, the applet should be ready to run. Open "index.html" in a web browser (Firefox is highly recommended), and enjoy!

Link to working copy of applet:
http://www.adelphi.edu/~stemkoski/pulpcore/index.html

Link to online version of this tutorial (includes applet):
http://www.adelphi.edu/~stemkoski/pulpcore/index-tutorial.html
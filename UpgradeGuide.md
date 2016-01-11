# Quick Upgrade Guide from 0.10 to 0.11 #

Most of the changes in 0.11 are enhancements or added functionality, but there are a few places where the you'll need to update your code. The changes are with colors, sounds, cursors, anchors, and blending modes.

## Colors ##
All colors must now have an alpha component. The pulpcore.image.Colors class provides convenience methods for
working with ARGB colors.

Search your code for any hex numbers (starting with '0x') or older color constants (starting with 'CoreGraphics.') and replace them like this:

Old:
```
import pulpcore.image.CoreGraphics;

g.setColor(0xff0080);
g.setColor(CoreGraphics.BLACK);
filledSprite = new FilledSprite(0x000011);
filledSprite.setBorderColor(0x808080)
```

New:
```
import static pulpcore.image.Colors.*;

g.setColor(rgb(255, 0, 128));
g.setColor(BLACK);
filledSprite = new FilledSprite(rgb(0x000011));
filledSprite.borderColor.set(gray(128))
```

## Sounds ##
To simplify the API, the SoundClip class is no longer available. To load sounds, use the Sound class.

Old:
```
SoundClip sound = SoundClip.load("blast.wav");
```
New:
```
Sound sound = Sound.load("blast.wav");
```

## Cursors ##
The Scene2D class now manages cursors, and Sprites keep track of their cursor.

Old:
```
public class GameScene extends Scene2D {
  ...
  public void update(int elapsedTime) {
    ...
    if (sprite.contains(mouseX, mouseY)) {
	Input.setCursor(Input.CURSOR_HAND);
    }
    else {
	Input.setCursor(Input.CURSOR_CROSSHAIR);
    }
  }
}
```
New:
```
public class GameScene extends Scene2D {
  ...
  public void load() {
    ...
    super.setCursor(Input.CURSOR_CROSSHAIR);
    sprite.setCursor(Input.CURSOR_HAND);
  }
}
```

## Anchors ##
Older anchors have been removed in favor of compass-style anchors.

Old:
```
sprite.setAnchor(Sprite.BOTTOM | Sprite.LEFT);
sprite.setAnchor(Sprite.HCENTER | Sprite.TOP);
sprite.setAnchor(Sprite.VCENTER | Sprite.HCENTER);
```
New:
```
sprite.setAnchor(Sprite.SOUTH_WEST);
sprite.setAnchor(Sprite.NORTH);
sprite.setAnchor(Sprite.CENTER);
```

## Blend Modes ##
The older Composite enumeration in CoreGraphics has been replaced with the BlendMode enumeration. The new enumeration creates blend modes lazily, which means code shrinkers (ProGuard) can remove the software compositing code for blend modes that your app does not use.

Old:
```
sprite.setComposite(CoreGraphics.COMPOSITE_MULT);
```
New:
```
sprite.setBlendMode(BlendMode.Multiply());
```
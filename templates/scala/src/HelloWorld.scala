import pulpcore.animation.Easing._
import pulpcore.image.Colors._
import pulpcore.scene.Scene2D
import pulpcore.sprite._
import PulpCore._

class HelloWorld extends Scene2D {
  
  override def load = {
    var sprite = new ImageSprite("success.png", 5, 5)
    
    add(new FilledSprite(WHITE))
    add(sprite)
    add(new Label("Hello World!", 26, 6))
    
    /* 
      The Java equivalent is: 
      if (sprite.x.get() == 5) {
          sprite.x.set(sprite.x.get() + 115);
          sprite.x.animateTo(5, 1000, Easing.NONE, 1000)
          // sprite.x.set(5)
          // sprite.x.animate(120, 5, 1000, Easing.REGULAR_OUT, 1000)
      }
    */
    if (sprite.x == 5) {
      sprite.x += 115
      sprite.x -> (5 dur 1000 delay 1000)
      //sprite.x -> 5
      //sprite.x -> ((120, 5) dur 1000 ease REGULAR_IN delay 1000)
    }
  }
    
  override def update(elapsedTime:Int) = {
      
  }
}

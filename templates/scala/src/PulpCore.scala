// Don't import Int - Scala will be confused
import pulpcore.animation.{ Property, Fixed, Bool, Color, Easing, Tween }
import pulpcore.math.CoreMath._

/**
  Contains implicit defs. Most code will use "import PulpCore._"
*/
object PulpCore {

  // Convert PulpCore properties to numbers
  implicit def Int2int(v: pulpcore.animation.Int):Int = v.get
  implicit def Fixed2double(v: Fixed):Double = v.get
  implicit def Bool2bool(v: Bool):Boolean = v.get
  implicit def Color2int(v: Color):Int = v.get

  // Add methods to PulpCore properties
  implicit def Fixed2FixedView(prop: Fixed) = new FixedView(prop)
  implicit def Int2IntView(prop: pulpcore.animation.Int) = new IntView(prop)
  implicit def Bool2BoolView(prop: Bool) = new BoolView(prop)
  implicit def Color2ColorView(prop: Color) = new ColorView(prop)

  // Create a new Tween animation
  implicit def Range2TweenBuilder(v: Range) = new TweenBuilder(
    true, v.start, v.end, 1000, null, 0)
  implicit def Int2TweenBuilder(v: Int) = new TweenBuilder(
    false, 0, v, 1000, null, 0)
  implicit def Double2TweenBuilder(v: Double) = new TweenBuilder(
    false, 0, v, 1000, null, 0)
  implicit def Double2MyDouble(v: Double) = new MyDouble(v)
}

class MyDouble(val v: Double) {
  // This isn't the best solution because (Int to Double) is illegal
  // But we can't put to(Double) in TweenBuilder because Int already has to(Int)
  def to(toValue: Double) = new TweenBuilder(true, v, toValue, 1000, null, 0)
}

class TweenBuilder(val useFromValue: Boolean,
                   val fromValue: Double, val toValue: Double,
                   val duration: Int, val easing: Easing, val delay: Int) {

  def dur(newDuration:Int) = new TweenBuilder(useFromValue, fromValue,
    toValue, newDuration, easing, delay)

  def ease(newEasing:Easing) = new TweenBuilder(useFromValue, fromValue,
    toValue, duration, newEasing, delay)

  def delay(newDelay:Int) = new TweenBuilder(useFromValue, fromValue,
    toValue, duration, easing, newDelay)

  def setBehavior(prop: Fixed) {
      if (useFromValue) {
          prop.animate(fromValue, toValue, duration, easing, delay)
      }
      else {
          prop.animateTo(toValue, duration, easing, delay)
      }
  }

  def setBehavior(prop: pulpcore.animation.Int) {
      if (useFromValue) {
          prop.animate(fromValue.toInt, toValue.toInt, duration, easing, delay)
      }
      else {
          prop.animateTo(toValue.toInt, duration, easing, delay)
      }
  }
}

class FixedView(val prop: Fixed) extends Ordered[Double] {

  def compare(that: Double): Int = { prop.get.compare(that) }

  override def equals(that: Any): Boolean = that match {
    case that: FixedView => prop.get equals that.prop.get
    case that        => prop.get equals that
  }

  def :=(v: TweenBuilder) {
    v.setBehavior(prop)
  }

  def :=(v:Double) {
    prop.set(v)
    prop
  }

  def +=(v:Double) = {
    prop.set(prop.get + v)
    prop
  }
  def -=(v:Double) = {
    prop.set(prop.get + v)
    prop
  }
  def *=(v:Double) = {
    prop.set(prop.get * v)
    prop
  }
  def /=(v:Double) = {
    prop.set(prop.get / v)
    prop
  }
}

class IntView(val prop: pulpcore.animation.Int) extends Ordered[Double] {

  def compare(that: Double): Int = { prop.get.toDouble.compare(that) }

  override def equals(that: Any): Boolean = that match {
    case that: IntView => prop.get equals that.prop.get
    case that        => prop.get equals that
  }

  def :=(v: TweenBuilder) {
    v.setBehavior(prop)
  }

  def :=(v:Int) {
    prop.set(v)
    prop
  }

  def +=(v:Int) = {
    prop.set(prop.get + v)
    prop
  }
  def -=(v:Int) = {
    prop.set(prop.get + v)
    prop
  }
  def *=(v:Int) = {
    prop.set(prop.get * v)
    prop
  }
  def /=(v:Int) = {
    prop.set(prop.get / v)
    prop
  }
}

class BoolView(val prop: Bool) {

  def :=(b: Boolean) {
    prop.set(b)
  }

}

class ColorView(val prop: Color) {

  def :=(v: Int) {
    prop.set(v)
  }

}


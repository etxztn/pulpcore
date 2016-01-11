The nightly builds of PulpCore now integrate new alpha blending modes.<br>
This article explains what are the differences between all these modes and how to use them in PulpCore.<br>
<br>
<h1>1. What is Alpha compositing</h1>

<a href='http://en.wikipedia.org/wiki/Alpha_compositing'>Wikipedia</a> defines "Alpha compositing as :<br>
<blockquote>... the process of combining an image with a background to create the appearance of partial transparency. It is often useful to render image elements in separate passes, and then combine the resulting multiple 2D images into a single, final image in a process called compositing. For example, compositing is used extensively when combining computer rendered image elements with live footage.</blockquote>

Basically, a composite mode explains how colors of each pixel of an image will blend together to create the resulting images. Alpha compositing is also known as "Porter and Duff rules" because they first explained how translucent images could be blend together to create various effects.<br>
<br>
<h1>2. A bit of theory about image representation</h1>
Before going further, let's explain how is composed an image. An image is composed of pixels (a colored dot). Each pixel of an image is composed of 4 components :<br>
<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/argb.png' />
<ul><li>A : for Alpha channel<br>
</li><li>R : for Red channel<br>
</li><li>G : for Green channel<br>
</li><li>B : for Blue channel<br>
Each of these components contains a value, representing the amount of color. Values can go from 0 (no color) to 255 (full color).</li></ul>

<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/argb_values.png' />

All components are then <a href='http://en.wikipedia.org/wiki/RGB_color_model'>added to create a color</a>. For example :<br>
<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/argb_examples.png' />

Each component value is stored as a byte (a byte = 8bits allows to have 256 different values for each component). A color, composed of 4 bytes is stored, in a 32 bits Java datatype : primitive type int.<br>
<br>
<h2>How about the alpha component ?</h2>
Alpha component represents the transparent component of the image. A value of 0 means transparent color, a value of 255 means an opaque color. Values between 0 and 255 means that the color is translucent, it means that this color will react with the underlying color (that'll be explained in next chapter).<br>
Notice that if a color has Alpha (A) component equals to 0, it will be transparent, not matter on Red (R), Green (G) or Blue (B) values.<br>
Two representations of translucent pixels exists :<br>
<ul><li>Unpremultiplied representation (also called classic representation or ARGB) : R, G and B values needs to be multiplied by A value to obtain the real amount of color in each component.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/unpremultiply.png' />
</li><li>Premultiplied representation (also called ARGB_PRE) : R, G and B values are already multiplied with the A value. The amount of color in each component is corresponding to the A value.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/premultiply.png' /></li></ul>

Premultiplied representation has an advantage over unpremultiply representation : 3 multiplications are spared for each pixel. Consider big images (800x600) and you'll see that it can spare a lot of operations time. <br>
PulpCore is using premultiplied images to keep good performances on image operations that need color calculations (and composites are part of it).<br>
<br>
<h1>3. Blending modes</h1>
The following blend modes operations are expected to be used with premultiplied colors. They explain how an existing image (a background image or Destination image), DST, will blend with the image that needs to be drawn (the foreground image or Source image) SRC.<br>
<blockquote>Ax represents the Alpha component.<br>
Cx represent each Component of a color.<br>
Cd, Ad are the Component and Alpha of the DESTination image.<br>
Cs, As are the Component and Alpha of the SouRCe image.<br>
Cr, Ar are the Component and Alpha of the resulting image.<br></blockquote>

<h2>2.1 <i>Clear</i></h2>
<blockquote>A<sub>r</sub> = 0<br>
C<sub>r</sub> = 0<br>
None of the terms are used.</blockquote>

<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/clear_mode.png' />
<h3>How to use <i>Clear</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.SrcClear());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.2 <i>Dst</i></h2>
<blockquote>A<sub>r</sub> = A<sub>d</sub><br>
C<sub>r</sub> = C<sub>d</sub><br>
Only the terms that contribute destination color are used.<br></blockquote>

<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_mode.png' />
<h3>How to use <i>Dst</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.Dst());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.3 <i>Dst Atop</i></h2>
<blockquote>A<sub>r</sub> = A<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + A<sub>d</sub> <code>*</code> A<sub>s</sub><br>
A<sub>r</sub> = A<sub>s</sub><br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + C<sub>d</sub> <code>*</code> A<sub>s</sub><br>
The destination that overlaps the source is composited with the source and replaces the destination.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_atop_mode.png' />
<h3>How to use <i>Dst Atop</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.DstAtop());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.4 <i>Dst In</i></h2>
A<sub>r</sub> = A<sub>d</sub> <code>*</code> A<sub>s</sub><br>
C<sub>r</sub> = C<sub>d</sub> <code>*</code> A<sub>s</sub><br>
The destination that overlaps the source, replaces the source.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_in_mode.png' />
<h3>How to use <i>Dst In</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.DstIn());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.5 <i>Dst Out</i></h2>
A<sub>r</sub> = A<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
C<sub>r</sub> = C<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
The destination that does not overlap the source replaces the source.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_out_mode.png' />
<h3>How to use <i>Dst Out</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.DstOut());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.6 <i>Dst Over</i></h2>
A<sub>r</sub> = A<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + A<sub>d</sub><br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + C<sub>d</sub><br>
The destination that overlaps the source is composited with the source and replaces the destination.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_over_mode.png' />
<h3>How to use <i>Dst Over</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.DstOver());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.7 <i>Src</i></h2>
A<sub>r</sub> = A<sub>s</sub><br>
C<sub>r</sub> = C<sub>s</sub><br>
Only the terms that contribute source color are used.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/src_mode.png' />
<h3>How to use <i>Src</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.Src());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.8 <i>Src Atop</i></h2>
A<sub>r</sub> = A<sub>s</sub> <code>*</code> A<sub>d</sub> + A<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
A<sub>r</sub> = A<sub>d</sub><br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> A<sub>d</sub> + C<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
The source that overlaps the destination is composited with the destination.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/src_atop_mode.png' />
<h3>How to use <i>Src Atop</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.SrcAtop());<br>
 add(redSprite);<br>
</code></pre>
<h2>2.9 <i>Src In</i></h2>
A<sub>r</sub> = A<sub>s</sub> <code>*</code> A<sub>d</sub><br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> A<sub>d</sub><br>
The source that overlaps the destination, replaces the destination.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/src_in_mode.png' />
<h3>How to use <i>Src In</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.SrcIn());<br>
 add(redSprite);<br>
</code></pre></blockquote>

<h2>2.10 <i>Src Out</i></h2>
<blockquote>A<sub>r</sub> = A<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>)<br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>)<br>
The source that does not overlap the destination replaces the destination.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/src_out_mode.png' />
<h3>How to use <i>Src Out</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.SrcOut());<br>
 add(redSprite);<br>
</code></pre></blockquote>

<h2>2.11 <i>Src Over</i> (Default)</h2>
<blockquote>A<sub>r</sub> = A<sub>s</sub> + A<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
C<sub>r</sub> = C<sub>s</sub> + C<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
The source color is placed over the destination color.<br>
Default blend mode. Also known as the <a href='http://en.wikipedia.org/wiki/Painter%27s_algorithm'>Painter's algorithm</a>.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/src_over_mode.png' />
<h3>How to use <i>Src Over</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.SrcOver());<br>
 add(redSprite);<br>
</code></pre>
or... nothing, because that's the default blend mode !</blockquote>

<h2>2.12 <i>Xor</i></h2>
<blockquote>A<sub>r</sub> = A<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + A<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
C<sub>r</sub> = C<sub>s</sub> <code>*</code> (1 - A<sub>d</sub>) + C<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
The non-overlapping regions of source and destination are combined.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/xor_mode.png' />
<h3>How to use <i>Xor</i> in PulpCore :</h3>
<pre><code> ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.Xor());<br>
 add(redSprite);<br>
</code></pre></blockquote>

<h1>3. Applications in PulpCore</h1>
<h2>3.1 Mixing Two images together</h2>
Even if you can add images directly in the Scene2D, you can't change the default blending mode here.<br>
You'll have to create a Group and add your images here.<br>
Two options are available to change the blend mode :<br>
<ul><li>Change the blend mode directly on the Group. Every Sprite added on the group will be blend with existing one using the composite provided with the <code>setBlendMode()</code> function.<br>
Example : <br>
<pre><code> groupA.setBlendMode(BlendMode.DstIn());<br>
 ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 add(redSprite);<br>
</code></pre>
</li><li>The other solution is to change the blending mode directly on the Sprite using the same method, but applying it on the Sprite object :<br>
<pre><code> groupA.setBlendMode(BlendMode.DstIn());<br>
 ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.Xor());<br>
 add(redSprite);<br>
</code></pre>
Both options are corrects. You'll have to choose one depending on what you want to do.</li></ul>

<h2>3. 2 Using the back buffer</h2>
You may notice, if you're using some blend modes that the result is not exactly what you expected. <br>
Example below with DST_OUT.<br>
<img src='http://greenshoes.free.fr/dotclear/public/pulpcore-tutorial/dst_out_backbuf.png' />

Why do I get this black rectangle ??<br>
That's because we're applying the filter directly on the background image and this image is opaque.<br>
Remember the DST_OUT equation ?<br>
<blockquote>A<sub>r</sub> = A<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
C<sub>r</sub> = C<sub>d</sub> <code>*</code> (1 - A<sub>s</sub>)<br>
if the destination image is opaque (that's the case of our background), Ad is 1.<br>
Imagine that the source image is also opaque, we have :<br>
A<sub>r</sub> = 1 <code>*</code> (1 - 1) = 0<br>
C<sub>r</sub> = C<sub>d</sub> <code>*</code>(1 - 1) = 0<br>
All the components are 0, the background is painted in black.<br>
This effect is explained in more details in <a href='http://filthyrichclients.org/'>filthy rich clients</a>.<br>
<br>
To avoid this, make sure to apply masks on a back buffer using the <code>createBackBuffer()</code> method :<br>
<pre><code> groupA = new Group();<br>
 groupA.createBackBuffer();<br>
 groupA.setBlendMode(BlendMode.DstIn());<br>
 ImageSprite blueSprite = new ImageSprite("blue.png", 10, 10);<br>
 add(blueSprite);<br>
 ImageSprite redSprite = new ImageSprite("red.png", 60, 60);<br>
 redSprite.setBlendMode(BlendMode.Xor());<br>
 add(redSprite);<br>
</code></pre>
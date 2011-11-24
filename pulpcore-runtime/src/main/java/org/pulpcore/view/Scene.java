/*
    Copyright (c) 2007-2011, Interactive Pulp, LLC
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package org.pulpcore.view;

import org.pulpcore.graphics.Color;

/**
 A Scene is the same as a Group, except it's initial backgroundColor is set to BLACK. If your
 scene completely covers the background (for example, with an opaque ImageView), an optimization
 would be to disable the background fill, like so: {@code backgroundColor.set(Color.TRANSPARENT)}.
 */
public class Scene extends Group {

    public Scene() {
        backgroundColor.set(Color.BLACK);
    }

    /**
     Performs any actions needed to load this scene. By default, this
     method does nothing. Typical implementations will load images and
     sounds in this method.
     This method will be called only if this Scene is added to a Stage, and not to another Scene or Group.
    */
    public void onLoad(Stage stage) { }

    /**
     Performs any actions needed to unload this scene. By default, this
     method does nothing. This method should return as quickly as possible; if unloading
     a scene requires a long computation, it should be done in a separate thread.
     This method will be called only if this Scene is added to a Stage, and not to another Scene or Group.
    */
    public void onUnload(Stage stage) { }


    /**
     Notifies that this scene has been shown after another Scene is hidden
     or immediately after a call to start(). Note, this method is not called
     if the OS shows the app. By default, this method does nothing.
     This method will be called only if this Scene is added to a Stage, and not to another Scene or Group.
    */
    public void onShow(Stage stage) { }

    /**
     Notifies that this scene has been hidden by another Scene or
     immediately before a call to stop(). Note, this method is not called if
     the OS hides the app. By default, this method does nothing.
     This method will be called only if this Scene is added to a Stage, and not to another Scene or Group.
    */
    public void onHide(Stage stage) { }

}

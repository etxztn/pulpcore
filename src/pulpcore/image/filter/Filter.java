/*
    Copyright (c) 2009, Interactive Pulp, LLC
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
package pulpcore.image.filter;

import pulpcore.image.CoreImage;

/**
    Base class for image filters. Subclasses override the
    {@link #filter(pulpcore.image.CoreImage, pulpcore.image.CoreImage) } method.
    @see pulpcore.sprite.Sprite#setFilter(pulpcore.image.filter.Filter)
*/
public abstract class Filter {

    private Filter input = null;
    private CoreImage output = null;
    private boolean isDirty = true;

    public int getWidth() {
        if (input != null) {
            return input.getWidth();
        }
        else {
            return 0;
        }
    }

    public int getHeight() {
        if (input != null) {
            return input.getHeight();
        }
        else {
            return 0;
        }
    }

    public boolean isOpaque() {
        if (input != null) {
            return input.isOpaque();
        }
        else {
            return false;
        }
    }

    /**
        Performs this filter on the input image onto a newly created output image. The current
        input image, if any, is ignored.
     */
    public final CoreImage filter(CoreImage input) {
        Filter oldInput = this.input;
        // Set input so that getWidth(), etc. is correct.
        setInput(input);
        CoreImage newOutput = new CoreImage(getWidth(), getHeight(), isOpaque());
        filter(input, newOutput);
        setInput(oldInput);
        return newOutput;
    }

    /**
        Performs this filter on the input image onto the specified output image. 
     <p>
        This method is called from {@link #getOutput()} if {@link #isDirty()} returns true.
        The output image will be the same dimensions as
        ({@link #getWidth()} x {@link #getHeight() }. Implementors must ensure
        that every pixel in {@code output} is drawn.
     */
    protected abstract void filter(CoreImage input, CoreImage output);

    public void setInput(CoreImage input) {
        setInput(new CoreImageInput(input));
    }
    
    public void setInput(Filter input) {
        if (this.input != input) {
            this.input = input;
            setDirty(true);
        }
    }

    public Filter getInput() {
        return input;
    }

    /**
        Gets the filtered output image.
    */
    public CoreImage getOutput() {
        if (isDirty() || this.output == null) {
            if (this.output == null ||
                this.output.getWidth() != getWidth() ||
                this.output.getHeight() != getHeight() ||
                this.output.isOpaque() != isOpaque())
            {
                this.output = new CoreImage(getWidth(), getHeight(), isOpaque());
                //pulpcore.CoreSystem.print("New image");
            }
            filter((input == null) ? null : input.getOutput(), this.output);
            //pulpcore.CoreSystem.print("Filtered: " + getClass().getName());
            setDirty(false);
        }
        return output;
    }

    /**
        Updates the filter. Subclasses should call super.update(elapsedTime).
    */
    public void update(int elapsedTime) {
        if (input != null) {
            input.update(elapsedTime);
            if (input.isDirty()) {
                setDirty(true);
            }
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    private static class CoreImageInput extends Filter {

        private final CoreImage image;

        CoreImageInput(CoreImage image) {
            this.image = image;
        }

        public CoreImage getOutput() {
            return image;
        }

        public int getWidth() {
            return image.getWidth();
        }

        public int getHeight() {
            return image.getHeight();
        }

        public boolean isOpaque() {
            return image.isOpaque();
        }

        protected void filter(CoreImage input, CoreImage output) {
            // Do nothing
        }
    }
}
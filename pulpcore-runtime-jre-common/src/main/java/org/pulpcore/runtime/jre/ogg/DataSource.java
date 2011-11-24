/*
    Copyright (c) 2008-2011, Interactive Pulp, LLC
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
package org.pulpcore.runtime.jre.ogg;

/**
 * A DataSource is a seekable data source.
 */
public abstract class DataSource {

    public abstract int size();

    public abstract int limit();

    public abstract int position();

    public abstract boolean position(int newPosition);

    public final void rewind() {
        position(0);
    }
    
    public final void fastForward() {
        position(size());
    }

    public final boolean isFilled() {
        return limit() == size();
    }

    public final int remaining() {
        return limit() - position();
    }

    /**

     @param dst
     @return a negative value if no bytes could be read because the limit was reached,
     0 if the end of the data has been reached,
     or a positive value indicating the number of bytes read.
     */
    public final int get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     @return a negative value if no bytes could be read because the limit was reached,
     0 if the end of the data has been reached,
     or a positive value indicating the number of bytes read.
     */
    public abstract int get(byte[] dst, int offset, int length);

    public abstract DataSource newView();

}

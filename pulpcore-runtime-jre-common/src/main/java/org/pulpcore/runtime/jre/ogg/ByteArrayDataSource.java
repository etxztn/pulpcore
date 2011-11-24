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

import java.util.Arrays;

public class ByteArrayDataSource extends DataSource {
    
    private final byte[] data;
    private final int dataOffset;
    private final int dataLength;

    private int position;

    public ByteArrayDataSource(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteArrayDataSource(byte[] data, int offset, int length) {
        checkBounds(offset, length, data.length);
        this.data = data;
        this.dataOffset = offset;
        this.dataLength = length;
        this.position = 0;
    }

    @Override
    public DataSource newView() {
        return new ByteArrayDataSource(data, dataOffset, dataLength);
    }

    private static void checkBounds(int offset, int length, int size) {
        if (offset < 0 || length < 0 || size < 0 || offset >= size || offset + length > size) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int size() {
        return dataLength;
    }

    @Override
    public int limit() {
        return dataLength;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public boolean position(int newPosition) {
        if (newPosition < 0 || newPosition > limit()) {
            return false;
        }
        this.position = newPosition;
        return true;
    }

    @Override
    public int get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        length = Math.min(length, remaining());
        if (length == 0) {
            if (isFilled()) {
                return 0;
            }
            else {
                return -1;
            }
        }
        else {
            System.arraycopy(data, position() + dataOffset, dst, offset, length);
            position(position() + length);
            return length;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteArrayDataSource other = (ByteArrayDataSource) obj;
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (this.dataOffset != other.dataOffset) {
            return false;
        }
        if (this.dataLength != other.dataLength) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Arrays.hashCode(this.data);
        hash = 97 * hash + this.dataOffset;
        hash = 97 * hash + this.dataLength;
        return hash;
    }

}

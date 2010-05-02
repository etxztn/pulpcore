/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
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
package pulpcore.sound.ogg;

import pulpcore.sound.ogg.data.DataSource;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.SyncState;

public class OggFile {

    private static final int CHUNK_SIZE = 65536;
    
    public static final int OV_FALSE = -1;
    public static final int OV_EOF = -2;
    public static final int OV_HOLE = -3;

    public static final int OV_EREAD      = -128;
    public static final int OV_EFAULT     = -129;
    public static final int OV_EIMPL      = -130;
    public static final int OV_EINVAL     = -131;
    public static final int OV_ENOTVORBIS = -132;
    public static final int OV_EBADHEADER = -133;
    public static final int OV_EVERSION   = -134;
    public static final int OV_ENOTAUDIO  = -135;
    public static final int OV_EBADPACKET = -136;
    public static final int OV_EBADLINK   = -137;
    public static final int OV_ENOSEEK    = -138;

    private int nextPageOffset = 0;

    private final DataSource data;

    protected final SyncState oy = new SyncState();

    public OggFile(DataSource data) {
        this.data = data;
    }

    public DataSource getData() {
        return data;
    }

    protected int getNextPageOffset() {
        return nextPageOffset;
    }

    protected int getDataChunk() {
        int index = oy.buffer(CHUNK_SIZE);
        int bytes = data.get(oy.data, index, CHUNK_SIZE);
        if (bytes > 0) {
            oy.wrote(bytes);
        }
        return bytes;
    }

    protected int seek(int offset) {
        boolean success = data.position(offset);
        if (success) {
            nextPageOffset = offset;
            oy.reset();
            return 0;
        }
        else {
            return OV_EREAD;
        }
    }

    /**
     @param boundary -1 for unbounded search, 0 for cache only, or a positive number
     to search for a new page beginning for n bytes.
     @return negative value on error; otherwise, the offset of the start of the page.
     */
    protected int getNextPage(Page page, int boundary) {
        if (boundary > 0) {
            boundary += nextPageOffset;
        }
        while (true) {
            if (boundary > 0 && nextPageOffset >= boundary) {
                return OV_FALSE;
            }
            int more = oy.pageseek(page);
            if (more < 0) {
                nextPageOffset -= more;
            }
            else if (more == 0) {
                if (boundary == 0) {
                    return OV_FALSE;
                }
                int bytes = getDataChunk();
                if (bytes == 0) {
                    return OV_EOF;
                }
                else if (bytes < 0) {
                    return OV_EREAD;
                }
            }
            else {
                int ret = nextPageOffset;
                nextPageOffset += more;
                return ret;
            }
        }
    }

    /**
     @return negative value on error; otherwise, the offset of the start of the page.
     */
    protected int getPrevPage(Page page) {
        int begin = nextPageOffset;
        int end = begin;
        int ret = 0;
        int offset = -1;

        while (offset == -1) {
            begin -= CHUNK_SIZE;
            if (begin < 0) {
                begin = 0;
            }
            ret = seek(begin);
            if (ret != 0) {
                return ret;
            }

            while (nextPageOffset < end) {
                ret = getNextPage(page, end-nextPageOffset);
                if (ret < 0) {
                    break;
                }
                else {
                    offset = ret;
                }
            }
        }

        if (offset >= 0 && page.header_len == 0) {
            ret = seek(offset);
            if (ret != 0) {
                return ret;
            }
            ret = getNextPage(page, -1);
            if (ret < 0) {
                return ret;
            }
        }
        return offset;
    }

    protected int getPrevPage(Page page, int serialno) {
        int begin = nextPageOffset;
        int end = begin;
        int ret = 0;
        int offset = -1;

        while (offset == -1) {
            begin -= CHUNK_SIZE;
            if (begin < 0) {
                begin = 0;
            }
            ret = seek(begin);
            if (ret != 0) {
                return ret;
            }

            while (nextPageOffset < end) {
                ret = getNextPage(page, end-nextPageOffset);
                if (ret < 0) {
                    break;
                }
                else if (page.serialno() == serialno) {
                    offset = ret;
                }
            }
        }
        if (offset >= 0 && (nextPageOffset != offset || page.header_len == 0)) {
            ret = seek(offset);
            if (ret != 0) {
                return ret;
            }
            ret = getNextPage(page, -1);
            if (ret < 0) {
                return ret;
            }
        }
        return offset;
    }

}

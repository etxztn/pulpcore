package pulpcore.sound.ogg.data;

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

package org.pulpcore.tools.packer;

public abstract class Object2D  implements Comparable<Object2D> {

    private int x;
    private int y;

    public abstract int getWidth();

    public abstract int getHeight();

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int compareTo(Object2D o) {
        int thisSize = getWidth();// * getHeight();
        int thatSize = o.getWidth();// * o.getHeight();
        if (thisSize < thatSize) {
            return 1;
        }
        else if (thisSize > thatSize) {
            return -1;
        }
        else {
            return 0;
        }
    }
}

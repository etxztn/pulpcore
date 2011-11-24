package org.pulpcore.tools.packer;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 A 2D bin packer.
 Algorithm from http://incise.org/2d-bin-packing-with-javascript-and-canvas.html and
 http://www.blackpawn.com/texts/lightmaps/ (Retrieved Feb, 2011)
 */
public class Bin2D<T extends Object2D> {

    private static class Node<T extends Object2D> {
        final Rectangle rect;
        Node<T> left = null;
        Node<T> right = null;
        T object2D = null;

        private Node(Node<T> clone) {
            this.rect = clone.rect;
            this.left = clone.left == null ? null : new Node<T>(clone.left);
            this.right = clone.right == null ? null : new Node<T>(clone.right);
            this.object2D = clone.object2D;
        }

        public Node(int x, int y, int w, int h) {
            rect = new Rectangle(x, y, w, h);
        }
        
        public boolean isLeaf() {
            return left == null;
        }

        private boolean fits(T object2D) {
            return object2D.getWidth() <= rect.width && object2D.getHeight() <= rect.height;
        }

        private boolean isSameSize(T object2D) {
            return object2D.getWidth() == rect.width && object2D.getHeight() == rect.height;
        }

        public boolean add(T object2D) {
            if (!isLeaf()) {
                return left.add(object2D) || right.add(object2D);
            }
            else if (this.object2D != null) {
                return false;
            }
            else if (!fits(object2D)) {
                return false;
            }
            else if (isSameSize(object2D)) {
                this.object2D = object2D;
                this.object2D.setLocation(rect.x, rect.y);
                return true;
            }
            else {
                int dw = rect.width - object2D.getWidth();
                int dh = rect.height - object2D.getHeight();

                if (dw > dh) {
                    left = new Node<T>(rect.x, rect.y, object2D.getWidth(), rect.height);
                    right = new Node<T>(rect.x + object2D.getWidth(), rect.y,
                            rect.width - object2D.getWidth(), rect.height);
                }
                else {
                    left = new Node<T>(rect.x, rect.y, rect.width, object2D.getHeight());
                    right = new Node<T>(rect.x, rect.y + object2D.getHeight(),
                            rect.width, rect.height - object2D.getHeight());
                }
                return left.add(object2D);
            }
        }

        private Node<T> copy() {
            return new Node<T>(this);
        }
    }
        
    private final Node<T> root;

    private Bin2D(Bin2D<T> bin2D) {
        this.root = bin2D.root.copy();
    }

    public Bin2D(int width, int height) {
        root = new Node<T>(0, 0, width, height);
    }

    public boolean isEmpty() {
        return !root.isLeaf();
    }

    public int getWidth() {
        return root.rect.width;
    }

    public int getHeight() {
        return root.rect.height;
    }

    /**
     Add a 2d object to this bin.
     @return true if the object fits, false otherwise.
     */
    public boolean add(T object) {
        return root.add(object);
    }

    public boolean addAll(Collection<T> objects) {
        boolean allAdded = true;
        for (T object : objects) {
            if (!add(object)) {
                allAdded = false;
            }
        }
        return allAdded;
    }

    public boolean canFit(T object) {
        return root.copy().add(object);
    }

    public boolean canFit(Collection<T> objects) {
        Node<T> clonedRoot = root.copy();
        for (T object : objects) {
            if (!clonedRoot.add(object)) {
                return false;
            }
        }
        return true;
    }

    public boolean canFit(T... objects) {
        Node<T> clonedRoot = root.copy();
        for (T object : objects) {
            if (!clonedRoot.add(object)) {
                return false;
            }
        }
        return true;
    }

    public List<T> getObjects() {
        List<T> list = new ArrayList<T>();
        getObjects(list, root);
        return list;
    }

    private void getObjects(List<T> list, Node<T> node) {
        if (node != null) {
            if (node.object2D != null) {
                list.add(node.object2D);
            }
            getObjects(list, node.left);
            getObjects(list, node.right);
        }
    }
}

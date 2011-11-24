package org.pulpcore.view;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.pulpcore.graphics.Color;
import org.pulpcore.graphics.Graphics;
import org.pulpcore.runtime.Input.TouchScrollEvent;
import org.pulpcore.runtime.RunLoop;
import org.pulpcore.runtime.Context;
import org.pulpcore.runtime.Surface;
import org.pulpcore.math.MathUtil;
import org.pulpcore.math.Recti;
import org.pulpcore.runtime.Input;
import org.pulpcore.runtime.Input.Event;
import org.pulpcore.runtime.Input.TouchEvent;

public class Stage {

    // NOTE: experiment with these two values for screen sizes other than 550x400
    /** If the non-dirty area inbetween two dirty rects is less than this value, the two
    rects are union'ed. */
    private static final int MAX_NON_DIRTY_AREA = 2048;
    private static final int NUM_DIRTY_RECTANGLES = 64;

    //private static final int MAX_PERCENT_DIRTY_RECTANGLE_COVERAGE = 80;

    public enum AutoScaling {
        /** Perform no auto scaling - the Scene dimensions are set to the surface dimensions */
        OFF,
        /** Automatically center the Scene in the Stage. The Scene is not scaled. */
        CENTER,
        /** Automatically scale the Scene to the Stage dimensions. */
        SCALE,
        /**
         Automatically scale the Scene to the Stage dimensions, preserving the Scene's
         aspect ratio.
        */
        FIT,
    }

    // Important stuff
    private final Context context;
    private final RunLoop runLoop;
    private final Surface surface;

    // Scene Graph
    private final LinkedList<Scene> sceneStack = new LinkedList<Scene>();
    private final Group root;
    private Scene currScene;
    private List<View> prevViewsWithTouchInside = new ArrayList<View>();
    private List<View> currViewsWithTouchInside = new ArrayList<View>();
    private List<View> pressedViews = new ArrayList<View>();
    private int pointerX = -1;
    private int pointerY = -1;

    // Scaling
    private AutoScaling autoScaling = AutoScaling.FIT;
    private boolean autoScalingChanged = false;
    private int width = 0;
    private int height = 0;

    // Dirty rectangles
    private boolean showDirtyRectangles = false;
    private boolean dirtyRectanglesEnabled = true;
    private boolean needsFullRedraw = true;
    private final Recti drawBounds = new Recti();
    private final RectList dirtyRectangles = new RectList(NUM_DIRTY_RECTANGLES);
    private final RectList subRects = new RectList(NUM_DIRTY_RECTANGLES);
    private final Recti newRect = new Recti();
    private final Recti workRect = new Recti();
    private final Recti workParentRect = new Recti();
    private final Recti unionRect = new Recti();
    private final Recti intersectionRect = new Recti();
    private int dirtyRectPadX = 1;
    private int dirtyRectPadY = 1;

    public Stage(Context context, RunLoop loop, Surface surface) {
        this.root = new Group();
        this.context = context;
        this.surface = surface;
        this.runLoop = loop;
        this.runLoop.setStage(this);
    }

    public Context getContext() {
        return context;
    }

    public RunLoop getRunLoop() {
        return runLoop;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    public AutoScaling getAutoScaling() {
        return autoScaling;
    }

    public void setAutoScaling(AutoScaling autoScaling) {
        if (this.autoScaling != autoScaling) {
            this.autoScaling = autoScaling;
            autoScalingChanged = true;
        }
    }

    private int getPreScaledWidth() {
        if (width == 0 || autoScaling == AutoScaling.OFF) {
            return surface.getWidth();
        }
        else {
            return width;
        }
    }

    private int getPreScaledHeight() {
        if (height == 0 || autoScaling == AutoScaling.OFF) {
            return surface.getHeight();
        }
        else {
            return height;
        }
    }

    /**
        Sets the dirty rectangle mode on or off. By default, the Stage has dirty rectangles
        enabled, but some apps may have better performance with
        dirty rectangles disabled.
    */
    public void setDirtyRectanglesEnabled(boolean dirtyRectanglesEnabled) {
        if (this.dirtyRectanglesEnabled != dirtyRectanglesEnabled) {
            this.dirtyRectanglesEnabled = dirtyRectanglesEnabled;
            needsFullRedraw = true;
            if (!this.dirtyRectanglesEnabled) {
                clearDirtyRects(root);
            }
        }
    }

    /**
        Checks the dirty rectangles are enabled.
        @return true if dirty rectangles are enabled.
        @see #setDirtyRectanglesEnabled(boolean)
    */
    public boolean isDirtyRectanglesEnabled() {
        return dirtyRectanglesEnabled;
    }

    /**
     Set to show dirty rectangles for debugging purposes.
     @param showDirtyRectangles
     */
    public void setShowDirtyRectangles(boolean showDirtyRectangles) {
        if (this.showDirtyRectangles != showDirtyRectangles) {
            this.showDirtyRectangles = showDirtyRectangles;
            needsFullRedraw = true;
        }
    }

    public boolean isShowDirtyRectangles() {
        return showDirtyRectangles;
    }

    // Called from the AWT event thread
    public void start() {
        runLoop.start();
        surface.start();
    }

    // Called from the AWT event thread
    public void stop() {
        runLoop.stop();
        surface.stop();
    }
    
    //
    // Scene Stack
    //
    
    public void pushScene(Scene scene) {
        sceneStack.push(scene);
        scene.setSize(root.getWidth(), root.getHeight());
        scene.onLoad(this);
    }

    public Scene popScene() {
        if (sceneStack.isEmpty()) {
            return null;
        }
        else {
            Scene scene = sceneStack.pop();
            scene.onUnload(this);
            return scene;
        }
    }

    public boolean canPopScene() {
        return !sceneStack.isEmpty();
    }

    public Scene peekScene() {
        if (sceneStack.isEmpty()) {
            return null;
        }
        else {
            return sceneStack.peek();
        }
    }

    public void setScene(Scene scene) {
        popScene();
        pushScene(scene);
    }

    public void clearSceneStack() {
        while (!sceneStack.isEmpty()) {
            sceneStack.pop().onUnload(this);
        }
    }

    //
    // RunLoop
    //

    private boolean drawBoundsSet() {
        return surface.isPrepared() &&
                root.getWidth() == getPreScaledWidth() &&
                root.getHeight() == getPreScaledHeight();
    }

    private boolean prepare() {
        if (surface.hasSizeChanged() || !drawBoundsSet() || autoScalingChanged) {
            resetDrawBounds();
        }
        boolean surfaceReady = surface.isPrepared();
        if (surfaceReady) {
            if (surface.contentsLost()) {
                needsFullRedraw = true;
            }
            Scene topScene = peekScene();
            if (topScene == null) {
                topScene = context.createFirstScene();
                if (topScene == null) {
                    stop();
                    return false;
                }
                else {
                    pushScene(topScene);
                }
            }
            if (currScene != topScene) {
                currScene = topScene;
                root.removeAllSubviews();
                root.addSubview(currScene);
                needsFullRedraw = true;
            }
        }
        return surfaceReady && root.size() > 0;
    }

    private boolean isPrepared() {
        return drawBoundsSet() && root.size() > 0;
    }


    /*
     TODO:
     onKeyPress (keyCode)
     onKeyRelease (keyCode)
     onKeyTyped (keyCode, repeat)
     */
    
    private void dispatchEnterEvents(View view, TouchEvent srcEvent) {
        
        TouchEvent enterEvent = null;
        while (view != null) {
            currViewsWithTouchInside.add(view);
            
            View.OnTouchEnterListener l = view.getOnTouchEnterListener();
            if (l != null && !prevViewsWithTouchInside.contains(view)) {
                if (enterEvent == null) {
                    enterEvent = new TouchEvent(srcEvent.isControlDown(), srcEvent.isShiftDown(),
                    srcEvent.isAltDown(), srcEvent.isMetaDown(), srcEvent.getType(), 
                    srcEvent.getX(), srcEvent.getY(), srcEvent.getButton());
                }
                if (enterEvent.shouldPropagate()) {
                    l.onEnter(view, enterEvent);
                }
            }
            view = view.getSuperview();
        }
    }
    
    
    private void dispatchExitEvents(View view, TouchEvent srcEvent) {
        TouchEvent exitEvent = null;
        for (View oldView : prevViewsWithTouchInside) {
            View.OnTouchExitListener l = oldView.getOnTouchExitListener();
            if (l != null && !currViewsWithTouchInside.contains(oldView)) {
                if (exitEvent == null) {
                    exitEvent = new TouchEvent(srcEvent.isControlDown(), srcEvent.isShiftDown(),
                    srcEvent.isAltDown(), srcEvent.isMetaDown(), srcEvent.getType(), 
                    srcEvent.getX(), srcEvent.getY(), srcEvent.getButton());
                }
                if (exitEvent.shouldPropagate()) {
                    l.onExit(oldView, exitEvent);
                }
                else {
                    break;
                }
            }
        }
        
        // Swap
        List<View> temp = prevViewsWithTouchInside;
        prevViewsWithTouchInside = currViewsWithTouchInside;
        currViewsWithTouchInside = temp;
        currViewsWithTouchInside.clear();
    }

    private void dispatchTouchMoveEvent(View view, TouchEvent event) {
        while (view != null && event.shouldPropagate()) {
            View.OnTouchMoveListener l = view.getOnTouchMoveListener();
            if (l != null) {
                l.onMove(view, event);
            }
            view = view.getSuperview();
        }
    }

    private void dispatchTouchHoverEvent(View view, TouchEvent event) {
        while (view != null && event.shouldPropagate()) {
            View.OnTouchHoverListener l = view.getOnTouchHoverListener();
            if (l != null) {
                l.onHover(view, event);
            }
            view = view.getSuperview();
        }
    }

    private void dispatchTouchPressEvent(View view, TouchEvent event) {
        pressedViews.clear();
        while (view != null) {
            pressedViews.add(view);
            if (event.shouldPropagate()) {
                View.OnTouchPressListener l = view.getOnTouchPressListener();
                if (l != null) {
                    l.onPress(view, event);
                }
            }
            view = view.getSuperview();
        }
    }

    private void dispatchTouchReleaseEvent(View view, TouchEvent event) {
        for (View pressedView : pressedViews) {
            if (event.shouldPropagate()) {
                View.OnTouchReleaseListener l = pressedView.getOnTouchReleaseListener();
                if (l != null) {
                    l.onRelease(view, event);
                }                
            }
            else {
                break;
            }
        }
        pressedViews.clear();
//        while (view != null && event.shouldPropagate()) {
//            View.OnTouchReleaseListener l = view.getOnTouchReleaseListener();
//            if (l != null) {
//                l.onRelease(view, event);
//            }
//            view = view.getSuperview();
//        }
    }

    private void dispatchTouchTapEvent(View view, TouchEvent event) {
        while (view != null && event.shouldPropagate()) {
            View.OnTouchTapListener l = view.getOnTouchTapListener();
            if (l != null) {
                l.onTap(view, event);
            }
            view = view.getSuperview();
        }
    }

    private void dispatchTouchScrollEvent(View view, TouchScrollEvent event) {
        while (view != null && event.shouldPropagate()) {
            View.OnTouchScrollListener l = view.getOnTouchScrollListener();
            if (l != null) {
                l.onScroll(view, event);
            }
            view = view.getSuperview();
        }
    }

    public void tick(float dt) {
        if (prepare()) {

            // Tick
            root.tick(dt);
            
            // Process input
            Input input = context.getInput();
            List<Event> events = input.consumeEvents();
            for (Event event : events) {
                if (event instanceof Input.KeyEvent) {
                    // TODO: For key events, find focused view
                }
                else if (event instanceof Input.TouchEvent) {
                    Input.TouchEvent e = (Input.TouchEvent)event;
                    pointerX = e.getX();
                    pointerY = e.getY();
                    View view = root.pick(pointerX, pointerY);
                    
                    try {
                        dispatchEnterEvents(view, e);
                        if (view != null) {
                            if (event instanceof Input.TouchScrollEvent) {
                                dispatchTouchScrollEvent(view, (Input.TouchScrollEvent)e);
                            }
                            else {
                                switch (e.getType()) {
                                    case MOVE: default:
                                        dispatchTouchHoverEvent(view, e);
                                        break;
                                    case DRAG:
                                        dispatchTouchMoveEvent(view, e);
                                        break;
                                    case PRESS:
                                        dispatchTouchPressEvent(view, e);
                                        break;
                                    case RELEASE:
                                        dispatchTouchReleaseEvent(view, e);
                                        break;
                                    case CLICK:
                                        dispatchTouchTapEvent(view, e);
                                        break;
                                }
                            }
                        }
                        dispatchExitEvents(view, e);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }
            }

            // Set cursor
            int cursor = Input.CURSOR_DEFAULT;
            if (pointerX != -1 && pointerY != -1) {
                View pick = root.pick(pointerX, pointerY);
                if (pick != null) {
                    cursor = pick.getCursor();
                }
            }
            input.setCursor(cursor);

            // Update transforms
            // (Must happen last)
            if (dirtyRectanglesEnabled) {
                addDirtyRectangles(root, null, null, needsFullRedraw, true);
            }
            else {
                updateTransforms(root);
            }
        }
    }

    public void render() {
        if (isPrepared()) {
            Graphics g = surface.getGraphics();

            if (surface.contentsLost()) {
                needsFullRedraw = true;
            }

            if (!dirtyRectanglesEnabled || needsFullRedraw || dirtyRectangles.isOverflowed()) {
                g.setClip(drawBounds);
                root.render(g);
                needsFullRedraw = false;
                surface.show();
            }
            else if (showDirtyRectangles) {
                g.setClip(drawBounds);
                root.render(g);

                g.setColor(Color.rgba(Color.GREEN, 128));
                for (int i = 0; i < dirtyRectangles.size(); i++) {
                    Recti r = dirtyRectangles.get(i);
                    g.setClip(r.x, r.y, r.width, r.height);
                    g.fill();
                }
                surface.show();
            }
            else {
                // This might be a place to optimize. Currently every sprite is drawn for every
                // rectangle, and the clipping bounds makes sure we don't overdraw.
                for (int i = 0; i < dirtyRectangles.size(); i++) {
                    Recti r = dirtyRectangles.get(i);
                    g.setClip(r.x, r.y, r.width, r.height);
                    root.render(g);
                }
                surface.show(dirtyRectangles.rects, dirtyRectangles.size);
            }

            dirtyRectangles.clear();
        }
    }

    //
    // Auto-scaling
    //

    private void resetDrawBounds() {

        float w = getPreScaledWidth();
        float h = getPreScaledHeight();
        root.setSize(w, h);
        if (currScene != null) {
            currScene.setSize(w, h);
        }

        if (autoScaling == AutoScaling.OFF ||
                (surface.getWidth() == w && surface.getHeight() == h)) {
            root.x.set(0);
            root.y.set(0);
            root.scaleX.set(1);
            root.scaleY.set(1);
        }
        else {

            switch (autoScaling) {
                default:
                    root.x.set(0);
                    root.y.set(0);
                    root.scaleX.set(1);
                    root.scaleY.set(1);
                    break;

                case CENTER:
                    root.x.set((int)(surface.getWidth() - w) / 2);
                    root.y.set((int)(surface.getHeight() - h) / 2);
                    root.scaleX.set(1);
                    root.scaleY.set(1);
                    break;

                case SCALE:
                    root.x.set(0);
                    root.y.set(0);
                    root.scaleX.set(surface.getWidth() / w);
                    root.scaleY.set(surface.getHeight() / h);
                    break;

                case FIT:
                    float a = h * surface.getWidth();
                    float b = w * surface.getHeight();
                    float newWidth;
                    float newHeight;

                    if (a > b) {
                        newHeight = surface.getHeight();
                        newWidth = (float)Math.floor(newHeight * w / h);
                    }
                    else if (a < b) {
                        newWidth = surface.getWidth();
                        newHeight = (float)Math.floor(newWidth * h / w);
                    }
                    else {
                        newWidth = surface.getWidth();
                        newHeight = surface.getHeight();
                    }

                    root.x.set((int)(surface.getWidth() - newWidth) / 2);
                    root.y.set((int)(surface.getHeight() - newHeight) / 2);
                    root.scaleX.set(newWidth / w);
                    root.scaleY.set(newHeight / h);
                    break;
            }
        }

        if (root.scaleX.get() == 1 && root.scaleY.get() == 1) {
            drawBounds.setBounds((int)root.x.get(), (int)root.y.get(), (int)w, (int)h);
            dirtyRectPadX = 1;
            dirtyRectPadY = 1;
        }
        else {
            drawBounds.setBounds(
                (int)(root.x.get()),
                (int)(root.y.get()),
                (int)(w * root.scaleX.get()),
                (int)(h * root.scaleY.get()));
            // Based off of BackBufferTest scaled to full screen
            dirtyRectPadX = 1 + (int)Math.ceil(root.scaleX.get());
            dirtyRectPadY = 1 + (int)Math.ceil(root.scaleY.get());
        }
        autoScalingChanged = false;
        needsFullRedraw = true;
    }

    //
    // Dirty Rects
    //

    private static class RectList {

        private final Recti[] rects;
        private int size;

        public RectList(int capacity) {
            rects = new Recti[capacity];
            for (int i = 0; i < capacity; i++) {
                rects[i] = new Recti();
            }
            clear();
        }

        public int getArea() {
            int area = 0;
            for (int i = 0; i < size; i++) {
                area += rects[i].getArea();
            }
            return area;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
        }

        public boolean isOverflowed() {
            return (size < 0);
        }

        public void overflow() {
            size = -1;
        }

        public Recti get(int i) {
            return rects[i];
        }

        public void remove(int i) {
            if (size > 0) {
                if (i < size - 1) {
                    rects[i].setBounds(rects[size - 1]);
                }
                size--;
            }
        }

        public boolean add(Recti r) {
            if (size < 0 || size == rects.length) {
                size = -1;
                return false;
            }
            else {
                rects[size++].setBounds(r);
                return true;
            }
        }
    }

    private void updateTransforms(Group group) {
        group.updateTransform();
        for (int i = 0; i < group.size(); i++) {
            View view = group.getSubview(i);
            if (view instanceof Group) {
                updateTransforms((Group)view);
            }
            else {
                view.updateTransform();
            }
        }
    }

    /**
        Recursive function to loop through all the subviews of the
        specified group.
    */
    private void addDirtyRectangles(Group group, Recti oldParentClip, Recti parentClip,
        boolean parentDirty, boolean parentVisible)
    {
        parentDirty |= group.isDirty();
        
        boolean areSubviewsClipped = group.isClippedToBounds() || group.hasBackBuffer();
        
        Recti oldClip = group.getDirtyRect();
        if (oldClip != null) {
            if (group.getContentsDirty()) {
                addDirtyRectangle(oldParentClip, oldClip);
            }
            if (areSubviewsClipped) {
                if (oldParentClip == null) {
                    oldParentClip = new Recti(oldClip);
                }
                else {
                    oldParentClip = new Recti(oldParentClip);
                    oldParentClip.intersection(oldClip);
                }
            }
        }
        boolean parentBoundsChanged = group.updateDirtyRect(parentVisible);
        Recti newClip = group.getDirtyRect();
        if (newClip != null) {
            if (group.getContentsDirty()) {
                addDirtyRectangle(parentClip, newClip);
            }
            if (areSubviewsClipped) {
                if (parentClip == null) {
                parentClip = new Recti(newClip);
                }
                else {
                    parentClip = new Recti(parentClip);
                    parentClip.intersection(newClip);
                }
            }
        }
        // Update the Group dirty rect.
//        // Groups only have a dirty rect if isOverflowClipped() is true
//        Recti oldClip = group.getDirtyRect();
//        if (oldClip != null) {
//            if (oldParentClip == null) {
//                oldParentClip = new Recti(oldClip);
//            }
//            else {
//                oldParentClip = new Recti(oldParentClip);
//                oldParentClip.intersection(oldClip);
//            }
//        }
//        boolean parentBoundsChanged = group.updateDirtyRect(parentVisible);
//        Recti newClip = group.getDirtyRect();
//        if (newClip != null) {
//            if (parentClip == null) {
//                parentClip = new Recti(newClip);
//            }
//            else {
//                parentClip = new Recti(parentClip);
//                parentClip.intersection(newClip);
//            }
//            if (group.getContentsDirty()) {
//                if (parentBoundsChanged) {
//                    addDirtyRectangle(null, oldParentClip);
//                }
//                addDirtyRectangle(null, parentClip);
//            }
//        }

        if (!parentBoundsChanged) {
            if (oldParentClip != parentClip) {
                if (oldParentClip == null || parentClip == null) {
                    parentBoundsChanged = true;
                }
                else {
                    parentBoundsChanged = !oldParentClip.equals(parentClip);
                }
            }
        }

        // Add dirty rects for removed views
        ArrayList<View> removedViews = group.getRemovedViews();
        if (removedViews != null) {
            for (int i = 0; i < removedViews.size(); i++) {
                View removedView = removedViews.get(i);
                if (removedView == group) {
                    // Special case: Group had a filter that was removed
                    addDirtyRectangle(null, oldParentClip);
                }
                else {
                    notifyRemovedView(oldParentClip, removedView);
                }
            }
        }

        parentDirty |= parentBoundsChanged;
        parentVisible &= group.isVisible() && group.opacity.get() > 0;

        // Add dirty rects for the views
        for (int i = 0; i < group.size(); i++) {
            View view = group.getSubview(i);
            if (view instanceof Group) {
                addDirtyRectangles((Group)view, oldParentClip, parentClip, parentDirty, parentVisible);
            }
            else if (parentDirty || view.isDirty()) {
                if (dirtyRectangles.isOverflowed()) {
                    view.updateDirtyRect(parentVisible);
                }
                else {
                    addDirtyRectangle(oldParentClip, view.getDirtyRect());
                    boolean boundsChanged = view.updateDirtyRect(parentVisible);
                    if (parentBoundsChanged || boundsChanged) {
                        addDirtyRectangle(parentClip, view.getDirtyRect());
                    }
                }
            }

            view.clearDirty();
        }
    }

    private void notifyRemovedView(Recti parentClip, View view) {
        if (dirtyRectangles.isOverflowed()) {
            return;
        }

        if (view instanceof Group) {
            Group group = (Group)view;
            for (int i = 0; i < group.size(); i++) {
                notifyRemovedView(parentClip, group.getSubview(i));
            }
        }
        else if (view != null) {
            addDirtyRectangle(parentClip, view.getDirtyRect());
        }
    }

    private void addDirtyRectangle(Recti parentClip, Recti r) {
        if (r == null) {
            return;
        }

        subRects.clear();

        Recti currParentClip = null;
        if (parentClip != null) {
            currParentClip = workParentRect;
            // Increase bounds to correct off-by-one miscalculation in some rare rotated views.
            currParentClip.setBounds(parentClip.x - dirtyRectPadX, parentClip.y - dirtyRectPadY,
                parentClip.width + dirtyRectPadX*2, parentClip.height + dirtyRectPadY*2);
        }

        // Increase bounds to correct off-by-one miscalculation in some rare rotated views.
        addDirtyRectangle(currParentClip, r.x - dirtyRectPadX, r.y - dirtyRectPadY,
            r.width + dirtyRectPadX*2, r.height + dirtyRectPadY*2, MAX_NON_DIRTY_AREA);

        int originalSize = subRects.size();
        for (int i = 0; i < subRects.size() && !dirtyRectangles.isOverflowed(); i++) {
            Recti r2 = subRects.get(i);
            if (i < originalSize) {
                addDirtyRectangle(null, r2.x, r2.y, r2.width, r2.height, MAX_NON_DIRTY_AREA);
            }
            else {
                addDirtyRectangle(null, r2.x, r2.y, r2.width, r2.height, 0);
            }
            if (subRects.isOverflowed()) {
                // Ah, crap.
                dirtyRectangles.overflow();
            }
        }

        // If covering too much area, don't use dirty rectangles
        // *** Disabled because I didn't see any improvement in the BubbleMark example
        //if (!dirtyRectangles.isOverflowed()) {
        //    int maxArea = drawBounds.getArea() * MAX_PERCENT_DIRTY_RECTANGLE_COVERAGE / 100;
        //    if (dirtyRectangles.getArea() >= maxArea) {
        //        dirtyRectangles.overflow();
        //    }
        //}
    }

    private void addDirtyRectangle(Recti parentClip, int x, int y, int w, int h,
        int maxNonDirtyArea)
    {
        if (w <= 0 || h <= 0 || dirtyRectangles.isOverflowed()) {
            return;
        }

        newRect.setBounds(x, y, w, h);
        newRect.intersection(drawBounds);
        if (parentClip != null) {
            newRect.intersection(parentClip);
        }
        if (newRect.width <= 0 || newRect.height <= 0) {
            return;
        }

        // The goal here is to have no overlapping dirty rectangles because
        // it would lead to problems with alpha blending.
        //
        // Performing a union on two overlapping rectangles would lead to
        // dirty rectangles that cover large portions of the scene that are
        // not dirty.
        //
        // Instead: shrink, split, or remove existing dirty rectangles, or
        // shrink or remove the new dirty rectangle.
        for (int i = 0; i < dirtyRectangles.size(); i++) {

            Recti dirtyRect = dirtyRectangles.get(i);

            unionRect.setBounds(dirtyRect);
            unionRect.union(newRect);
            if (unionRect.equals(dirtyRect)) {
                return;
            }
            intersectionRect.setBounds(dirtyRect);
            intersectionRect.intersection(newRect);

            int newArea = unionRect.getArea() + intersectionRect.getArea() -
                dirtyRect.getArea() - newRect.getArea();
            if (newArea < maxNonDirtyArea) {
                newRect.setBounds(unionRect);
                dirtyRectangles.remove(i);
                if (newArea > 0) {
                    // Start over - make sure there's no overlap
                    i = -1;
                }
                else {
                    i--;
                }
            }
            else if (dirtyRect.intersects(newRect)) {
                int code = dirtyRect.getIntersectionCode(newRect);
                int numSegments = MathUtil.countBits(code);
                if (numSegments == 0) {
                    // Remove the existing dirty rect in favor of the new one
                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 1) {
                    // Shrink the existing dirty rect
                    dirtyRect.setOutsideBoundary(Recti.getOppositeSide(code),
                        newRect.getBoundary(code));
                    subRects.add(dirtyRect);

                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 2) {
                    // Split the existing dirty rect into two
                    int side1 = 1 << MathUtil.log2(code);
                    int side2 = code - side1;
                    workRect.setBounds(dirtyRect);

                    // First split
                    dirtyRect.setOutsideBoundary(Recti.getOppositeSide(side1),
                        newRect.getBoundary(side1));
                    subRects.add(dirtyRect);

                    // Second split
                    workRect.setOutsideBoundary(side1,
                        dirtyRect.getBoundary(Recti.getOppositeSide(side1)));
                    workRect.setOutsideBoundary(Recti.getOppositeSide(side2),
                        newRect.getBoundary(side2));
                    subRects.add(workRect);

                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 3) {
                    // Shrink the new dirty rect
                    int side = code ^ 0xf;
                    newRect.setOutsideBoundary(Recti.getOppositeSide(side),
                        dirtyRect.getBoundary(side));
                    if (newRect.width <= 0 || newRect.height <= 0) {
                        return;
                    }
                }
                else if (numSegments == 4) {
                    // Exit - don't add this new rect
                    return;
                }
            }
        }

        dirtyRectangles.add(newRect);
    }

    private void clearDirtyRects(Group group) {
        group.clearDirtyRect();
        for (int i = 0; i < group.size(); i++) {
            View view = group.getSubview(i);
            if (view instanceof Group) {
                clearDirtyRects((Group)view);
            }
            else {
                view.clearDirtyRect();
            }
        }
    }
}

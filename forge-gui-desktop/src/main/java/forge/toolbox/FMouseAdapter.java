package forge.toolbox;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Special mouse adapter to make it handling specific mouse buttons easier
 * and improve responsiveness of click and double click actions
 *
 */
public abstract class FMouseAdapter extends MouseAdapter {
    //Event functions to override
    public void onLeftMouseDown(final MouseEvent e) {}
    public void onLeftMouseUp(final MouseEvent e) {}
    public void onLeftClick(final MouseEvent e) {}
    public void onLeftDoubleClick(final MouseEvent e) {}
    public void onLeftMouseDragging(final MouseEvent e) {}
    public void onLeftMouseDragDrop(final MouseEvent e) {}

    public void onMiddleMouseDown(final MouseEvent e) {}
    public void onMiddleMouseUp(final MouseEvent e) {}
    public void onMiddleClick(final MouseEvent e) {}
    public void onMiddleDoubleClick(final MouseEvent e) {}
    public void onMiddleMouseDragging(final MouseEvent e) {}
    public void onMiddleMouseDragDrop(final MouseEvent e) {}

    public void onRightMouseDown(final MouseEvent e) {}
    public void onRightMouseUp(final MouseEvent e) {}
    public void onRightClick(final MouseEvent e) {}
    public void onRightDoubleClick(final MouseEvent e) {}
    public void onRightMouseDragging(final MouseEvent e) {}
    public void onRightMouseDragDrop(final MouseEvent e) {}

    public void onMouseEnter(final MouseEvent e) {}
    public void onMouseExit(final MouseEvent e) {}

    /**
     * Forge Mouse Adapter with infinite click tolerance (so long as mouse doesn't leave component)
     */
    public FMouseAdapter() {
        this(false);
    }

    public FMouseAdapter(final boolean isCompDraggable0) {
        isCompDraggable = isCompDraggable0;
    }

    private static FMouseAdapter mouseDownAdapter;
    private static MouseEvent mouseDownEvent;

    public static void forceMouseUp() {
        setMouseDownAdapter(null);
    }

    private static void setMouseDownAdapter(final FMouseAdapter mouseDownAdapter0) {
        if (mouseDownAdapter == mouseDownAdapter0) { return; }
        if (mouseDownAdapter != null) {
            //ensure mouse up handled for mouse down adapter if needed
            switch (mouseDownAdapter.downButton) {
            case 1:
                mouseDownAdapter.onLeftMouseUp(mouseDownEvent);
                break;
            case 2:
                mouseDownAdapter.onMiddleMouseUp(mouseDownEvent);
                break;
            case 3:
                mouseDownAdapter.onRightMouseUp(mouseDownEvent);
                break;
            }
            //reset all fields on previous mouse down adapter
            mouseDownAdapter.resetMouseDownLoc();
            mouseDownAdapter.firstClickLoc = null;
            mouseDownAdapter.downButton = 0;
            mouseDownAdapter.buttonsDown = 0;
            mouseDownAdapter.firstClickButton = 0;
            mouseDownAdapter.hovered = false;
            mouseDownEvent = null;
        }
        mouseDownAdapter = mouseDownAdapter0;
    }

    private final boolean isCompDraggable;
    private boolean hovered;
    private Point mouseDownLoc, firstClickLoc;
    private int downButton, buttonsDown, firstClickButton;
    private Component tempMotionListenerComp;

    @Override
    public final void mousePressed(final MouseEvent e) {
        setMouseDownAdapter(this);
        mouseDownEvent = e;

        final int button = e.getButton();
        if (button < 1 || button > 3) {
            return;
        }

        if (downButton == 0) {
            downButton = button;
            switch (button) {
            case 1:
                onLeftMouseDown(e);
                break;
            case 2:
                onMiddleMouseDown(e);
                break;
            case 3:
                onRightMouseDown(e);
                break;
            }
        }

        buttonsDown += button;
        if (buttonsDown == 4 && downButton != 2) {
            downButton = 2;
            onMiddleMouseDown(e); //treat left and right together as equivalent of middle
        }

        mouseDownLoc = e.getLocationOnScreen();

        //if component is draggable, ensure this adapter added as mouse motion listener to component while mouse down
        if (isCompDraggable) {
            tempMotionListenerComp = e.getComponent();
            if (tempMotionListenerComp != null) {
                for (final MouseMotionListener motionListener : tempMotionListenerComp.getMouseMotionListeners()) {
                    if (motionListener == this) {
                        tempMotionListenerComp = null;
                        break;
                    }
                }
                if (tempMotionListenerComp != null) {
                    tempMotionListenerComp.addMouseMotionListener(this);
                }
            }
        }

        if (firstClickLoc != null) {
            //if first mouse down resulted in click and second mouse down with same button in close proximity,
            //handle double click event (don't wait until second mouse up to improve responsiveness)
            if (e.getClickCount() % 2 == 0 && downButton == firstClickButton &&
                    e.getLocationOnScreen().distance(firstClickLoc) <= 3) {
                switch (firstClickButton) {
                case 1:
                    onLeftDoubleClick(e);
                    break;
                case 2:
                    onMiddleDoubleClick(e);
                    break;
                case 3:
                    onRightDoubleClick(e);
                    break;
                }
            }
            firstClickLoc = null;
            firstClickButton = 0;
        }
    }

    @Override
    public final void mouseDragged(final MouseEvent e) {
        //clear mouse down location if component begins being dragged
        if (mouseDownLoc != null && isCompDraggable &&
                e.getLocationOnScreen().distance(mouseDownLoc) > 3) {
            resetMouseDownLoc();
        }
        if (tempMotionListenerComp == null) { //don't raise drag event if only a temporary motion listener
            switch (downButton) {
            case 1:
                onLeftMouseDragging(e);
                break;
            case 2:
                onMiddleMouseDragging(e);
                break;
            case 3:
                onRightMouseDragging(e);
                break;
            }
        }
    }

    @Override
    public final void mouseEntered(final MouseEvent e) {
        hovered = true;
        onMouseEnter(e);
    }

    @Override
    public final void mouseExited(final MouseEvent e) {
        hovered = false;
        onMouseExit(e);
    }

    private void resetMouseDownLoc() {
        mouseDownLoc = null;
        if (tempMotionListenerComp != null) {
            tempMotionListenerComp.removeMouseMotionListener(this);
            tempMotionListenerComp = null;
        }
    }

    @Override
    public final void mouseReleased(final MouseEvent e) {
        int button = e.getButton();
        if (button < 1 || button > 3 || downButton == 0 || mouseDownAdapter != this) {
            return;
        }
        buttonsDown -= button;
        if (buttonsDown > 0) { return; } //don't handle mouse up until all mouse buttons up

        button = downButton;
        downButton = 0;
        mouseDownAdapter = null;
        mouseDownEvent = null;

        switch (button) {
        case 1:
            onLeftMouseUp(e);
            break;
        case 2:
            onMiddleMouseUp(e);
            break;
        case 3:
            onRightMouseUp(e);
            break;
        }

        if (!hovered) { //reset mouse down location and don't handle click if mouse up outside component
            resetMouseDownLoc();
        }

        //if mouse down on component and not cleared by drag or exit, handle click
        if (mouseDownLoc != null) {
            //if first click, cache button and mouse down location for determination of double click later
            if (e.getClickCount() % 2 == 1) {
                firstClickButton = button;
                firstClickLoc = mouseDownLoc;
            }

            resetMouseDownLoc();

            switch (button) {
            case 1:
                onLeftClick(e);
                break;
            case 2:
                onMiddleClick(e);
                break;
            case 3:
                onRightClick(e);
                break;
            }
        }
        else if (isCompDraggable) { //handle drag drop if click not handled and component is draggable
            switch (button) {
            case 1:
                onLeftMouseDragDrop(e);
                break;
            case 2:
                onMiddleMouseDragDrop(e);
                break;
            case 3:
                onRightMouseDragDrop(e);
                break;
            }
        }
    }

    @Override
    public final void mouseClicked(final MouseEvent e) {
        //override mouseClicked as final to prevent it being used since it doesn't fire
        //if the user moves the mouse at all between mouse down and mouse up
    }
}

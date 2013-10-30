package forge.gui.toolbox;

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
    public void onLeftMouseDown(MouseEvent e) {}
    public void onLeftMouseUp(MouseEvent e) {}
    public void onLeftClick(MouseEvent e) {}
    public void onLeftDblClick(MouseEvent e) {}
    public void onLeftMouseDragging(MouseEvent e) {}
    public void onLeftMouseDragDrop(MouseEvent e) {}

    public void onMiddleMouseDown(MouseEvent e) {}
    public void onMiddleMouseUp(MouseEvent e) {}
    public void onMiddleClick(MouseEvent e) {}
    public void onMiddleDblClick(MouseEvent e) {}
    public void onMiddleMouseDragging(MouseEvent e) {}
    public void onMiddleMouseDragDrop(MouseEvent e) {}

    public void onRightMouseDown(MouseEvent e) {}
    public void onRightMouseUp(MouseEvent e) {}
    public void onRightClick(MouseEvent e) {}
    public void onRightDblClick(MouseEvent e) {}
    public void onRightMouseDragging(MouseEvent e) {}
    public void onRightMouseDragDrop(MouseEvent e) {}

    public void onMouseEnter(MouseEvent e) {}
    public void onMouseExit(MouseEvent e) {}
    
    /**
     * Forge Mouse Adapter with infinite click tolerance (so long as mouse doesn't leave component)
     */
    public FMouseAdapter() {
        this(false);
    }
    
    public FMouseAdapter(boolean isCompDraggable0) {
        isCompDraggable = isCompDraggable0;
    }

    private final boolean isCompDraggable;
    private boolean hovered;
    private Point mouseDownLoc;
    private Point firstClickLoc;
    private int firstClickButton;
    private Component tempMotionListenerComp;

    @Override
    public final void mousePressed(final MouseEvent e) {
        switch (e.getButton()) {
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

        mouseDownLoc = e.getLocationOnScreen();
        
        //if component is draggable, ensure this adapter added as mouse motion listener to component while mouse down
        if (isCompDraggable) {
            tempMotionListenerComp = e.getComponent();
            if (tempMotionListenerComp != null) {
                for (MouseMotionListener motionListener : tempMotionListenerComp.getMouseMotionListeners()) {
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
            if (e.getClickCount() == 2 && e.getButton() == firstClickButton &&
                    e.getLocationOnScreen().distance(firstClickLoc) <= 3) {
                switch (firstClickButton) {
                case 1:
                    onLeftDblClick(e);
                    break;
                case 2:
                    onMiddleDblClick(e);
                    break;
                case 3:
                    onRightDblClick(e);
                    break;
                }
            }
            firstClickLoc = null;
            firstClickButton = 0;
        }
    }
    
    @Override
    public final void mouseDragged(MouseEvent e) {
        //clear mouse down location if component begins being dragged
        if (mouseDownLoc != null && isCompDraggable &&
                e.getLocationOnScreen().distance(mouseDownLoc) > 3) {
            resetMouseDownLoc();
        }
        if (tempMotionListenerComp == null) { //don't raise drag event if only a temporary motion listener
            switch (e.getButton()) {
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
    public final void mouseEntered(MouseEvent e) {
        hovered = true;
        onMouseEnter(e);
    }
    
    @Override
    public final void mouseExited(MouseEvent e) {
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
    public final void mouseReleased(MouseEvent e) {
        switch (e.getButton()) {
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
            if (e.getClickCount() == 1) {
                firstClickButton = e.getButton();
                firstClickLoc = mouseDownLoc;
            }

            resetMouseDownLoc();

            switch (e.getButton()) {
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
            switch (e.getButton()) {
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
}

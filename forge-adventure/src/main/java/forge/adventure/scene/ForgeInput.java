package forge.adventure.scene;

import com.badlogic.gdx.Input;
import forge.Forge;
import forge.screens.match.MatchController;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;
import forge.toolbox.FOverlay;
import forge.util.Utils;
import forge.gui.error.BugReporter;

import java.util.ArrayList;
import java.util.List;


/**
 * ForgeInput
 * Handles input for forge screens
 */
public class ForgeInput extends FGestureAdapter {
    private static final List<FDisplayObject> potentialListeners = new ArrayList<>();
    private static char lastKeyTyped;
    private static boolean keyTyped, shiftKeyDown;
    private final ForgeScene forgeScene;
    //mouseMoved and scrolled events for desktop version
    private int mouseMovedX, mouseMovedY;

    public ForgeInput(ForgeScene forgeScene) {
        this.forgeScene=forgeScene;
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (keyCode == Input.Keys.MENU) {
            //showMenu();
            return true;
        }
        if (keyCode == Input.Keys.SHIFT_LEFT || keyCode == Input.Keys.SHIFT_RIGHT) {
            shiftKeyDown = true;
        }

        // Cursor keys emulate swipe gestures
        // First we touch the screen and later swipe (fling) in the direction of the key pressed
        if (keyCode == Input.Keys.LEFT) {
            touchDown(0, 0, 0, 0);
            return fling(1000, 0);
        }
        if (keyCode == Input.Keys.RIGHT) {
            touchDown(0, 0, 0, 0);
            return fling(-1000, 0);
        }
        if (keyCode == Input.Keys.UP) {
            touchDown(0, 0, 0, 0);
            return fling(0, -1000);
        }
        if (keyCode == Input.Keys.DOWN) {
            touchDown(0, 0, 0, 0);
            return fling(0, 1000);
        }
        /*
        if(keyCode == Input.Keys.BACK){
            if (destroyThis)
                deviceAdapter.exit();
            else if(onHomeScreen() && isLandscapeMode())
                back();
        }
        */
        if (Forge.keyInputAdapter == null) {
            if (Forge.KeyInputAdapter.isModifierKey(keyCode)) {
                return false; //don't process modifiers keys for unknown adapter
            }
            //if no active key input adapter, give current screen or overlay a chance to handle key
            FContainer container = FOverlay.getTopOverlay();
            if (container == null) {
                container = MatchController.getView();
                if (container == null) {
                    return false;
                }
            }
            return container.keyDown(keyCode);
        }
        return Forge.keyInputAdapter.keyDown(keyCode);
    }

    @Override
    public boolean keyUp(int keyCode) {
        keyTyped = false; //reset on keyUp
        if (keyCode == Input.Keys.SHIFT_LEFT || keyCode == Input.Keys.SHIFT_RIGHT) {
            shiftKeyDown = false;
        }
        if (Forge.keyInputAdapter != null) {
            return Forge.keyInputAdapter.keyUp(keyCode);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char ch) {
        if (Forge.keyInputAdapter != null) {
            if (ch >= ' ' && ch <= '~') { //only process this event if character is printable
                //prevent firing this event more than once for the same character on the same key down, otherwise it fires too often
                if (lastKeyTyped != ch || !keyTyped) {
                    keyTyped = true;
                    lastKeyTyped = ch;
                    return Forge.keyInputAdapter.keyTyped(ch);
                }
            }
        }
        return false;
    }

    private void updatePotentialListeners(int x, int y) {
        potentialListeners.clear();

        //base potential listeners on object containing touch down point
        for (FOverlay overlay : FOverlay.getOverlaysTopDown()) {
            if (overlay.isVisibleOnScreen(forgeScene.getScreen())) {
                overlay.buildTouchListeners(x, y, potentialListeners);
                if (overlay.preventInputBehindOverlay()) {
                    return;
                }
            }
        }
        forgeScene.buildTouchListeners(x, y, potentialListeners);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (pointer == 0) { //don't change listeners when second finger goes down for zoom
            updatePotentialListeners(x, y);
            if (Forge.keyInputAdapter != null) {
                if (!Forge.keyInputAdapter.allowTouchInput() || !potentialListeners.contains(Forge.keyInputAdapter.getOwner())) {
                    //endKeyInput(); //end key input if needed
                }
            }
        }
        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean press(float x, float y) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.press(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean release(float x, float y) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.release(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean longPress(float x, float y) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.longPress(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (shiftKeyDown && flick(x, y)) {
            return true; //give flick logic a chance to handle Shift+click
        }
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.tap(listener.screenToLocalX(x), listener.screenToLocalY(y), count)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean flick(float x, float y) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.flick(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.fling(velocityX, velocityY)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.pan(listener.screenToLocalX(x), listener.screenToLocalY(y), deltaX, deltaY, moreVertical)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean panStop(float x, float y) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.panStop(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        try {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.zoom(listener.screenToLocalX(x), listener.screenToLocalY(y), amount)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            BugReporter.reportException(ex);
            return true;
        }
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        mouseMovedX = x;
        mouseMovedY = y;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        updatePotentialListeners(mouseMovedX, mouseMovedY);

        if (Forge.KeyInputAdapter.isCtrlKeyDown()) { //zoom in or out based on amount
            return zoom(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amountY);
        }

        boolean handled;
        if (Forge.KeyInputAdapter.isShiftKeyDown()) {
            handled = pan(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amountX, 0, false);
        } else {
            handled = pan(mouseMovedX, mouseMovedY, 0, -Utils.AVG_FINGER_HEIGHT * amountY, true);
        }
        if (panStop(mouseMovedX, mouseMovedY)) {
            handled = true;
        }
        return handled;
    }
}
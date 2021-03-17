package forge.toolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.gui.FThreads;
import forge.screens.FScreen;
import forge.screens.match.MatchController;

public abstract class FOverlay extends FContainer {
    public static final float ALPHA_COMPOSITE = 0.5f;
    private static final Stack<FOverlay> overlays = new Stack<>();
    private static boolean hidingAll = false;
    private static FOverlay tempOverlay;

    private FSkinColor backColor;
    private FScreen openedOnScreen;

    public FOverlay() {
        this(FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(ALPHA_COMPOSITE));
    }
    public FOverlay(FSkinColor backColor0) {
        backColor = backColor0;
        super.setVisible(false); //hide by default
    }

    public static FOverlay getTopOverlay() {
        if (overlays.isEmpty()) {
            return null;
        }
        return overlays.peek();
    }

    public static Iterable<FOverlay> getOverlays() {
        return overlays;
    }

    //get list of overlays for iterating from top overlay down
    public static Iterable<FOverlay> getOverlaysTopDown() {
        if (overlays.size() < 2) {
            return overlays; //don't need to create new list if one or fewer overlay
        }
        List<FOverlay> reversedList = new ArrayList<>();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            reversedList.add(overlays.get(i));
        }
        return reversedList;
    }

    public boolean preventInputBehindOverlay() {
        return true; //prevent input behind overlay by default
    }

    public static void hideAll() {
        hidingAll = true;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            overlays.get(i).hide();
        }
        overlays.clear();
        hidingAll = false;
    }

    private static final Task hideTempOverlayTask = new Task() {
        @Override
        public void run () {
            tempOverlay.hide();
            tempOverlay = null;
        }
    };

    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    public void finishedloading() {
        Forge.setLoadingaMatch(false);
    }

    public boolean isVisibleOnScreen(FScreen screen) {
        return (openedOnScreen == screen); //by default, only show overlay on the same screen it's opened on
    }

    @Override
    public void setVisible(boolean visible0) {
        FThreads.assertExecutedByEdt(true); //prevent showing or hiding overlays from background thread

        if (this.isVisible() == visible0) { return; }

        //ensure task to hide temporary overlay cancelled and run if another overlay becomes shown or hidden
        if (tempOverlay != this && hideTempOverlayTask.isScheduled()) {
            hideTempOverlayTask.cancel();
            hideTempOverlayTask.run();
        }

        if (visible0) {
            //rotate overlay to face top human player if needed
            setRotate180(MatchController.getView() != null && MatchController.getView().isTopHumanPlayerActive());
            overlays.push(this);
            openedOnScreen = Forge.getCurrentScreen();
        }
        else {
            openedOnScreen = null;

            if (!hidingAll) { //hiding all handles cleaning up overlay collection
                if (overlays.get(overlays.size() - 1) == this) {
                    //after removing the top overlay, delay hiding overlay for a brief period
                    //to prevent back color flickering if another popup immediately follows
                    if (tempOverlay != this && backColor != null) {
                        tempOverlay = this;
                        Timer.schedule(hideTempOverlayTask, 0.025f);
                        return;
                    }
                    overlays.pop();
                }
                else {
                    overlays.remove(this);
                }
            }
        }
        super.setVisible(visible0);
    }

    public FSkinColor getBackColor() { 
        return backColor;
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (backColor == null) { return; }

        g.fillRect(backColor, 0, 0, this.getWidth(), this.getHeight());
    }

    //override all gesture listeners to prevent passing to display objects behind it
    @Override
    public boolean press(float x, float y) {
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        return true;
    }

    @Override
    public boolean release(float x, float y) {
        return true;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        return true;
    }

    @Override
    public boolean flick(float x, float y) {
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        return true;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        return true;
    }

    @Override
    public boolean panStop(float x, float y) {
        return true;
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        return true;
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        if (tempOverlay == this) { return; } //suppress touch events if waiting to be hidden

        super.buildTouchListeners(screenX, screenY, listeners);
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (tempOverlay == this) { return false; } //suppress key events if waiting to be hidden

        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            if (Forge.endKeyInput()) { return true; }

            hide(); //hide on escape by default
            return true;
        }
        return super.keyDown(keyCode);
    }
}
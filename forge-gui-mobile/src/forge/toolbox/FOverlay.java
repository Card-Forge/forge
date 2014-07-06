package forge.toolbox;

import java.util.Stack;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;

public abstract class FOverlay extends FContainer {
    public static final float ALPHA_COMPOSITE = 0.5f;
    private static final Stack<FOverlay> overlays = new Stack<FOverlay>();
    private static boolean hidingAll = false;
    private static FOverlay tempOverlay;

    private FSkinColor backColor;

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
        }
    };

    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        //ensure task to hide temporary overlay cancelled and run if another overlay becomes shown or hidden
        if (tempOverlay != this && hideTempOverlayTask.isScheduled()) {
            hideTempOverlayTask.cancel();
            hideTempOverlayTask.run();
        }

        if (visible0) {
            overlays.push(this);
        }
        else if (!hidingAll) { //hiding all handles cleaning up overlay collection
            if (overlays.get(overlays.size() - 1) == this) {
                overlays.pop();

                //after removing the top overlay, put up an empty overlay for a brief period
                //to prevent back color flickering if another popup immediately follows
                if (tempOverlay != this) {
                    if (tempOverlay == null) {
                        tempOverlay = new FOverlay() {
                            @Override
                            protected void doLayout(float width, float height) {
                            }
                        };
                    }
                    tempOverlay.backColor = backColor;
                    tempOverlay.show();
                    Timer.schedule(hideTempOverlayTask, 0.025f);
                }
            }
            else {
                overlays.remove(this);
            }
        }
        super.setVisible(visible0);
    }

    public FSkinColor getBackColor() { 
        return backColor;
    }

    @Override
    protected void drawBackground(Graphics g) {
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
    public boolean keyDown(int keyCode) {
        if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK) {
            if (Forge.endKeyInput()) { return true; }

            hide(); //hide on escape by default
            return true;
        }
        return super.keyDown(keyCode);
    }
}
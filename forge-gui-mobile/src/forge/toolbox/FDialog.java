package forge.toolbox;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.PhysicsObject;
import forge.util.TextUtil;
import forge.util.Utils;

public abstract class FDialog extends FOverlay {
    public static final FSkinFont MSG_FONT = FSkinFont.get(12);
    public static final FSkinColor MSG_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.9f);
    public static final FSkinColor MSG_BACK_COLOR = FScreen.Header.BACK_COLOR.alphaColor(0.75f);
    public static final float MSG_HEIGHT = MSG_FONT.getCapHeight() * 2.5f;
    protected static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS).alphaColor(0.8f);
    protected static final float BORDER_THICKNESS = Utils.scale(1);
    private static final FSkinColor SWIPE_BAR_COLOR = FScreen.Header.BACK_COLOR;
    private static final FSkinColor SWIPE_BAR_DOT_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.75f);
    private static final float SWIPE_BAR_HEIGHT = Utils.scale(12);

    private static int openDialogCount = 0;

    public static boolean isDialogOpen() {
        return openDialogCount > 0;
    }

    private final VPrompt prompt;
    private final FButton btnMiddle;
    private final String title;
    private final int buttonCount;
    private float totalHeight;
    private float revealPercent = 0;
    private float lastDy = 0;
    private boolean finishedFirstReveal;

    protected FDialog(String title0, int buttonCount0) {
        buttonCount = buttonCount0;
        prompt = add(new VPrompt("", "", null, null));
        if (buttonCount < 3) {
            title0 = TextUtil.fastReplace(title0," - ", "\n"); //breakup into lines
            btnMiddle = null;
            prompt.setMessage(title0); //only put title in message if no third button
        }
        else { //handle third button
            btnMiddle = prompt.add(new FButton()); //add to prompt
            btnMiddle.setCorner(Corner.BottomMiddle);
            btnMiddle.setSize(VPrompt.BTN_WIDTH, VPrompt.HEIGHT);
            btnMiddle.setEnabled(false); //disable until initialized
        }
        title = title0;
    }

    protected float getBottomMargin() {
        return 0;
    }

    @Override
    protected final void doLayout(float width, float height) {
        boolean firstReveal = (totalHeight == 0);
        float contentHeight = layoutAndGetHeight(width, height - VPrompt.HEIGHT - 2 * MSG_HEIGHT);
        totalHeight = contentHeight + VPrompt.HEIGHT;
        lastDy = 0; //reset whenever main layout occurs

        if (btnMiddle != null) {
            btnMiddle.setLeft((width - btnMiddle.getWidth()) / 2);
            if (!title.isEmpty()) {
                totalHeight += MSG_HEIGHT; //leave room for title above middle button
            }
        }
        prompt.setBounds(0, totalHeight - VPrompt.HEIGHT, width, VPrompt.HEIGHT);

        updateDisplayTop();

        if (firstReveal) { //start reveal animation after dialog first laid out
            updateRevealAnimation(3 * Forge.getScreenHeight());
        }
    }

    private void updateDisplayTop() {
        //shift all children into position
        float offsetDy = lastDy;
        lastDy = getHeight() - (totalHeight + getBottomMargin()) * revealPercent;
        float dy = lastDy - offsetDy;
        for (FDisplayObject child : getChildren()) {
            child.setTop(child.getTop() + dy);
        }
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        if (visible0) {
            openDialogCount++;
            MatchController.instance.cancelAwaitNextInput(); //ensure "Waiting for opponent..." prompt doesn't appear while dialog awaiting input
        }
        else if (openDialogCount > 0) {
            openDialogCount--;
        }
        super.setVisible(visible0);
    }

    protected abstract float layoutAndGetHeight(float width, float maxHeight);

    protected void initButton(int index, String text, FEventHandler command) {
        FButton button = getButton(index);
        if (button == null) { return; }

        button.setText(text);
        button.setCommand(command);
        button.setEnabled(true);
        if (button == btnMiddle) { //allow middle button to be sized to fit text
            button.setWidth(button.getAutoSizeBounds().width * 1.25f);
        }
    }

    protected FButton getButton(int index) {
        if (index >= buttonCount) { return null; }

        switch (index) {
        case 0:
            return prompt.getBtnOk();
        case 1:
            if (buttonCount == 2) {
                return prompt.getBtnCancel();
            }
            return btnMiddle;
        case 2:
            return prompt.getBtnCancel();
        default:
            return null;
        }
    }

    public boolean isButtonEnabled(int index) {
        FButton button = getButton(index);
        return button != null && button.isEnabled();
    }

    public void setButtonEnabled(int index, boolean enabled) {
        FButton button = getButton(index);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    @Override
    public boolean preventInputBehindOverlay() {
        return revealPercent > 0; //prevent input behind overlay unless completely hidden
    }

    //allow pan, zoom, and long press behind dialog if hidden
    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
        return preventInputBehindOverlay();
    }
    @Override
    public boolean panStop(float x, float y) {
        return preventInputBehindOverlay();
    }
    @Override
    public boolean zoom(float x, float y, float amount) {
        return preventInputBehindOverlay();
    }
    @Override
    public boolean longPress(float x, float y) {
        return false; //never handle long press inside dialog
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);

        if (revealPercent == 0) { return; }

        float x = 0;
        float y = getHeight() - (totalHeight + getBottomMargin()) * revealPercent;
        float w = getWidth();
        float h = totalHeight - VPrompt.HEIGHT;
        g.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);
    }

    @Override
    public void drawOverlay(Graphics g) {
        //draw swipe bar
        float dx = SWIPE_BAR_HEIGHT * 0.7f;
        float startX = dx * 0.7f;
        float x = startX;
        float y = getHeight() - (totalHeight + getBottomMargin()) * revealPercent - SWIPE_BAR_HEIGHT;
        float w = getWidth();
        float dotRadius = SWIPE_BAR_HEIGHT / 6;
        float dotTop = y + SWIPE_BAR_HEIGHT / 2;
        int dotCount = 3;
        g.fillRect(SWIPE_BAR_COLOR, 0, y, w, SWIPE_BAR_HEIGHT);
        for (int i = 0; i < dotCount; i++) {
            g.fillCircle(SWIPE_BAR_DOT_COLOR, x, dotTop, dotRadius);
            x += dx;
        }
        x = w - startX;
        for (int i = 0; i < dotCount; i++) {
            g.fillCircle(BORDER_COLOR, x, dotTop, dotRadius);
            x -= dx;
        }
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, w, y);

        if (revealPercent == 0) { return; } //skip rest if hidden

        y += SWIPE_BAR_HEIGHT;
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, w, y);

        //draw border above prompt
        y = prompt.getTop();
        if (btnMiddle != null && !title.isEmpty()) { //render title above prompt if middle button present
            y -= MSG_HEIGHT;
            g.fillRect(VPrompt.BACK_COLOR, 0, y, w, MSG_HEIGHT);
            g.drawText(title, VPrompt.FONT, VPrompt.FORE_COLOR, 0, y, w, MSG_HEIGHT, false, Align.center, true);
        }
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, w, y);
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (!finishedFirstReveal) { return false; }

        //support toggling temporary hide via swipe
        if (Math.abs(velocityY) > Math.abs(velocityX)) {
            velocityY = -velocityY; //must reverse velocityY for the sake of animation
            if ((revealPercent > 0) == (velocityY > 0)) {
                return false;
            }
            updateRevealAnimation(velocityY);
            return true;
        }
        return false;
    }

    private void updateRevealAnimation(float velocity) {
        if (activeRevealAnimation == null) {
            activeRevealAnimation = new RevealAnimation(velocity);
            activeRevealAnimation.start();
        }
        else { //update existing animation with new velocity if needed
            activeRevealAnimation.physicsObj.getVelocity().set(0, velocity);
        }
    }

    protected void onRevealFinished() {
        //provide ability to handle when first reveal finished
    }

    private RevealAnimation activeRevealAnimation;

    private class RevealAnimation extends ForgeAnimation {
        private final PhysicsObject physicsObj;

        private RevealAnimation(float velocity) {
            physicsObj = new PhysicsObject(new Vector2(0, (totalHeight + getBottomMargin()) * revealPercent), new Vector2(0, velocity));
        }

        @Override
        protected boolean advance(float dt) {
            if (physicsObj.isMoving()) { //avoid storing last fling stop time if scroll manually stopped
                physicsObj.advance(dt);
                float newRevealPercent = physicsObj.getPosition().y / (totalHeight + getBottomMargin());
                if (newRevealPercent < 0) {
                    newRevealPercent = 0;
                }
                else if (newRevealPercent > 1) {
                    newRevealPercent = 1;
                }
                if (revealPercent != newRevealPercent) {
                    revealPercent = newRevealPercent;
                    updateDisplayTop();
                    return physicsObj.isMoving();
                }
            }

            //end fling animation if can't reveal or hide anymore or physics object is no longer moving
            return false;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            activeRevealAnimation = null;
            if (!finishedFirstReveal) {
                finishedFirstReveal = true;
                onRevealFinished();
            }
        }
    }
}

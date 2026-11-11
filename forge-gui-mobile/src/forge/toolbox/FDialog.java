package forge.toolbox;

import com.badlogic.gdx.graphics.Color;
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
import forge.screens.match.arena.ArenaWidgets;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.PhysicsObject;
import forge.util.TextUtil;
import forge.util.Utils;

public abstract class FDialog extends FOverlay {
    public static final FSkinFont MSG_FONT = FSkinFont.get(12);
    public static FSkinColor getMsgForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT).alphaColor(0.9f);
        return FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.9f);
    }
    public static FSkinColor getMsgBackColor() {
        return FScreen.Header.getBackColor().alphaColor(0.75f);
    }
    public static final float MSG_HEIGHT = MSG_FONT.getCapHeight() * 2.5f;
    protected static FSkinColor getBorderColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_BORDERS).alphaColor(0.8f);
        return FSkinColor.get(Colors.CLR_BORDERS).alphaColor(0.8f);
    }
    protected static final float BORDER_THICKNESS = Utils.scale(1);
    private static FSkinColor getSwipeBarColor() {
        return FScreen.Header.getBackColor();
    }
    private static FSkinColor getSwipeBarDotColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT).alphaColor(0.75f);
        return FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.75f);
    }
    private static final float SWIPE_BAR_HEIGHT = Utils.scale(12);

    private final VPrompt prompt;
    private final FButton btnMiddle;
    private final String title;
    private final int buttonCount;
    private float totalHeight;
    private float revealPercent = 0;
    private float lastDy = 0;
    private boolean finishedFirstReveal;
    // the Arena look on a desktop's match screen: a card centred over the board, its heading on top, no keyboard margin
    private boolean arenaCard;
    private float cardX, cardW, headH;
    /** The Arena card's width (a share of the window's) and its heading's height (in message heights). */
    private static final float ARENA_CARD_W = 0.56f, ARENA_HEADING = 1.6f;

    protected FDialog(String title0, int buttonCount0) {
        buttonCount = buttonCount0;
        prompt = add(new VPrompt("", "", null, null) {
            @Override
            protected void drawBackground(Graphics g) {
                if (ArenaWidgets.dialogButtonBand(g, getWidth(), getHeight())) { return; } //Arena look on the match screen
                super.drawBackground(g);
            }
        });
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

    /** Below the dialog: the subclass's margin (room for a keyboard), or in the Arena card half the room left. */
    private float margin() {
        return arenaCard ? Math.max(0f, (getHeight() - totalHeight) / 2f) : getBottomMargin();
    }

    @Override
    protected final void doLayout(float width, float height) {
        boolean firstReveal = (totalHeight == 0);
        arenaCard = forge.screens.match.arena.ArenaTheme.isMatchBoard() && forge.screens.match.arena.ArenaMatchLayout.desktop();
        cardW = arenaCard ? Math.min(width, Math.round(width * ARENA_CARD_W)) : width;
        cardX = (width - cardW) / 2f;
        headH = arenaCard && !title.isEmpty() ? Math.round(MSG_HEIGHT * ARENA_HEADING) : 0;
        if (btnMiddle == null) { //the Arena card says the title in its heading, not between the buttons
            prompt.setMessage(arenaCard ? "" : title);
        }
        float contentHeight = layoutAndGetHeight(cardW, height - VPrompt.HEIGHT - 2 * MSG_HEIGHT - headH);
        totalHeight = contentHeight + VPrompt.HEIGHT + headH;
        lastDy = 0; //reset whenever main layout occurs

        if (btnMiddle != null) {
            btnMiddle.setLeft((cardW - btnMiddle.getWidth()) / 2);
            if (!title.isEmpty() && !arenaCard) {
                totalHeight += MSG_HEIGHT; //leave room for title above middle button
            }
        }
        prompt.setBounds(0, totalHeight - VPrompt.HEIGHT, cardW, VPrompt.HEIGHT);
        if (arenaCard) { //what was laid out from the top left moves into the card, under its heading
            for (FDisplayObject child : getChildren()) {
                child.setLeft(child.getLeft() + cardX);
                if (child != prompt) {
                    child.setTop(child.getTop() + headH);
                }
            }
        }

        updateDisplayTop();

        if (firstReveal) { //start reveal animation after dialog first laid out
            updateRevealAnimation(3 * Forge.getScreenHeight());
        }
    }

    private void updateDisplayTop() {
        //shift all children into position
        float offsetDy = lastDy;
        lastDy = getHeight() - (totalHeight + margin()) * revealPercent;
        float dy = lastDy - offsetDy;
        for (FDisplayObject child : getChildren()) {
            child.setTop(child.getTop() + dy);
        }
    }

    protected abstract float layoutAndGetHeight(float width, float maxHeight);

    protected void initButton(int index, String text, FEventHandler command) {
        FButton button = getButton(index);
        if (button == null) { return; }

        button.setText(text);
        button.setCommand(command);
        button.setEnabled(true);
        button.setArenaPrimary(index == 0); //the Arena look shows the first answer as the main one
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
        if (arenaCard) { //the Arena card: the board dimmed, a navy glass card with a gold rim
            ArenaWidgets.dialogCard(g, getWidth(), getHeight(), cardX, getHeight() - (totalHeight + margin()) * revealPercent, cardW,
                    totalHeight, headH, revealPercent);
            return;
        }
        if (ArenaWidgets.dialogBackground(g, getWidth(), getHeight(), getHeight() - (totalHeight + getBottomMargin()) * revealPercent,
                totalHeight - VPrompt.HEIGHT, revealPercent)) {
            return; //Arena look on the match screen
        }
        super.drawBackground(g);

        if (revealPercent == 0) { return; }

        float x = 0;
        float y = getHeight() - (totalHeight + getBottomMargin()) * revealPercent;
        float w = getWidth();
        float h = totalHeight - VPrompt.HEIGHT;
        if (VPrompt.isFrostedMatch()) { //frosted slate sheet
            g.fillRect(VPrompt.GLASS_SOLID, x, y, w, h);
            return;
        }
        g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, x, y, w, h);
        g.fillRect(FScreen.getTextureOverlayColor(), x, y, w, h);
    }

    @Override
    public void drawOverlay(Graphics g) {
        if (arenaCard) { //the Arena card's heading on top; no swipe bar
            if (headH > 0 && revealPercent > 0) {
                ArenaWidgets.dialogHeading(g, title, cardX, getHeight() - (totalHeight + margin()) * revealPercent, cardW, headH, revealPercent);
            }
            return;
        }
        //draw swipe bar
        float dx = SWIPE_BAR_HEIGHT * 0.7f;
        float startX = dx * 0.7f;
        float x = startX;
        float y = getHeight() - (totalHeight + getBottomMargin()) * revealPercent - SWIPE_BAR_HEIGHT;
        float w = getWidth();
        float dotRadius = SWIPE_BAR_HEIGHT / 6;
        float dotTop = y + SWIPE_BAR_HEIGHT / 2;
        int dotCount = 3;
        if (ArenaWidgets.dialogOverlay(g, w, y, SWIPE_BAR_HEIGHT, revealPercent == 0 ? -1 : prompt.getTop(),
                btnMiddle != null && revealPercent > 0 ? title : null, MSG_HEIGHT, VPrompt.FONT)) {
            return; //Arena look on the match screen
        }
        final boolean glass = VPrompt.isFrostedMatch();
        final Color dotColor = new Color(1f, 1f, 1f, 0.45f);
        g.fillRect(glass ? VPrompt.GLASS_SOLID : getSwipeBarColor().getColor(), 0, y, w, SWIPE_BAR_HEIGHT);
        for (int i = 0; i < dotCount; i++) {
            g.fillCircle(glass ? dotColor : getSwipeBarDotColor().getColor(), x, dotTop, dotRadius);
            x += dx;
        }
        x = w - startX;
        for (int i = 0; i < dotCount; i++) {
            g.fillCircle(glass ? dotColor : getBorderColor().getColor(), x, dotTop, dotRadius);
            x -= dx;
        }
        if (glass) { g.drawLine(1, VPrompt.GLASS_EDGE, 0, y, w, y); } else { g.drawLine(BORDER_THICKNESS, getBorderColor(), 0, y, w, y); }

        if (revealPercent == 0) { return; } //skip rest if hidden

        y += SWIPE_BAR_HEIGHT;
        if (glass) { g.drawLine(1, VPrompt.GLASS_EDGE, 0, y, w, y); } else { g.drawLine(BORDER_THICKNESS, getBorderColor(), 0, y, w, y); }

        //draw border above prompt
        y = prompt.getTop();
        if (btnMiddle != null && !title.isEmpty()) { //render title above prompt if middle button present
            y -= MSG_HEIGHT;
            g.fillRect(glass ? VPrompt.GLASS_SOLID : VPrompt.getBackColor().getColor(), 0, y, w, MSG_HEIGHT);
            g.drawText(title, VPrompt.FONT, VPrompt.getForeColor(), 0, y, w, MSG_HEIGHT, false, Align.center, true);
        }
        if (glass) { g.drawLine(1, VPrompt.GLASS_EDGE, 0, y, w, y); } else { g.drawLine(BORDER_THICKNESS, getBorderColor(), 0, y, w, y); }
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (!finishedFirstReveal || arenaCard) { return false; } //the Arena card does not swipe away

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
            physicsObj = new PhysicsObject(new Vector2(0, (totalHeight + margin()) * revealPercent), new Vector2(0, velocity));
        }

        @Override
        protected boolean advance(float dt) {
            if (physicsObj.isMoving()) { //avoid storing last fling stop time if scroll manually stopped
                physicsObj.advance(dt);
                float newRevealPercent = physicsObj.getPosition().y / (totalHeight + margin());
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

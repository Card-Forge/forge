package forge.screens.match.views;

import forge.Forge;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.menu.FMagnifyView;
import forge.toolbox.FButton;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.TextBounds;
import forge.util.Utils;
import com.badlogic.gdx.math.MathUtils;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.gamemodes.match.YieldController;
import forge.toolbox.FEvent;

public class VPrompt extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;
    public static final float BTN_WIDTH = HEIGHT * 1.5f;
    public static final float PADDING = Utils.scale(2);
    public static final FSkinFont FONT = FSkinFont.get(14);
    public static FSkinColor getBackColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_THEME2);
        return FSkinColor.get(Colors.CLR_THEME2);
    }
    public static FSkinColor getForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }

    private final FButton btnOk, btnCancel;
    private final MessageLabel lblMessage;
    private String message;
    private CardView card;

    private static final float UNDO_SIZE = HEIGHT * 0.62f;
    private static final float UNDO_GAP = Utils.scale(10);
    private final UndoButton btnUndo;
    private boolean undoShown;      //whether the undo button is currently part of the layout
    private boolean undoRequested;  //the game labelled Cancel as Undo, so there is something to undo

    public void setCardView(final CardView card) {
        this.card = card;
    }

    public VPrompt(String okText, String cancelText, FEventHandler okCommand, FEventHandler cancelCommand) {
        lblMessage = add(new MessageLabel());
        lblMessage.setLeft(BTN_WIDTH);
        lblMessage.setHeight(HEIGHT);
        btnOk = add(new FButton(okText, okCommand));
        btnCancel = add(new CancelButton(cancelText, e -> onCancelPressed(cancelCommand, e)));
        btnUndo = add(new UndoButton());
        btnOk.setSize(BTN_WIDTH, HEIGHT);
        btnCancel.setSize(BTN_WIDTH, HEIGHT);
        btnOk.setCorner(Corner.BottomLeft);
        btnCancel.setCorner(Corner.BottomRight);
        btnOk.setEnabled(false); //disable buttons until first input queued
        btnCancel.setEnabled(false);
    }

    public FButton getBtnOk() {
        return btnOk;
    }

    public FButton getBtnCancel() {
        return btnCancel;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message0) {
        message = message0;
        card = null;
    }
    public void setMessage(String message0, CardView card0) {
        message = message0;
        card = card0;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        //SDisplayUtil.remind(view);
    }

    /** Small round undo button that sits beside the End Turn button. */
    private class UndoButton extends FDisplayObject {
        @Override
        public boolean tap(float x, float y, int count) {
            MatchController.instance.getGameController().undoLastAction();
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float cx = getWidth() / 2, cy = getHeight() / 2;
            float r = Math.min(getWidth(), getHeight()) / 2f * (isHovered() ? 1f : 0.9f);
            boolean adv = Forge.isMobileAdventureMode;
            FSkinColor fill = FSkinColor.get(isHovered()
                    ? (adv ? Colors.ADV_CLR_PHASE_ACTIVE_ENABLED : Colors.CLR_PHASE_ACTIVE_ENABLED)
                    : (adv ? Colors.ADV_CLR_INACTIVE : Colors.CLR_INACTIVE));
            FSkinColor fore = getForeColor();
            float line = Utils.scale(1.5f);
            g.fillCircle(fill, cx, cy, r);
            g.drawCircle(Utils.scale(1), fore, cx, cy, r);

            //counter-clockwise arc with an arrowhead
            float ar = r * 0.45f, start = 25f, end = 255f;
            int steps = 14;
            float px = cx + ar * MathUtils.cosDeg(start), py = cy - ar * MathUtils.sinDeg(start);
            for (int i = 1; i <= steps; i++) {
                float a = start + (end - start) * i / steps;
                float nx = cx + ar * MathUtils.cosDeg(a), ny = cy - ar * MathUtils.sinDeg(a);
                g.drawLine(line, fore, px, py, nx, ny);
                px = nx;
                py = ny;
            }
            float dx = -MathUtils.sinDeg(end), dy = -MathUtils.cosDeg(end); //direction of travel on screen
            float head = r * 0.38f;
            drawHead(g, fore, line, px, py, dx, dy, head, 150f);
            drawHead(g, fore, line, px, py, dx, dy, head, -150f);
        }

        private void drawHead(Graphics g, FSkinColor c, float line, float px, float py, float dx, float dy, float len, float deg) {
            float rx = dx * MathUtils.cosDeg(deg) - dy * MathUtils.sinDeg(deg);
            float ry = dx * MathUtils.sinDeg(deg) + dy * MathUtils.cosDeg(deg);
            g.drawLine(line, c, px, py, px + rx * len, py + ry * len);
        }
    }

    private static int separateUndoCache = -1;

    /** @return true if undo has its own round button and Cancel stays End Turn (otherwise the old look is kept) */
    public static boolean isSeparateUndo() {
        if (separateUndoCache < 0) { reloadSeparateUndo(); }
        return separateUndoCache == 1;
    }

    /** Call when the setting changes. */
    public static void reloadSeparateUndo() {
        separateUndoCache = FModel.getPreferences().getPrefBoolean(FPref.UI_SEPARATE_UNDO_BUTTON) ? 1 : 0;
    }

    /**
     * The Cancel button. While an undo is possible the game labels it "Undo (n)"; with the separate undo button that
     * turns into the round undo button and Cancel stays End Turn.
     */
    private class CancelButton extends FButton {
        private CancelButton(String text, FEventHandler command) {
            super(text, command);
        }

        @Override
        public void setText(String text0) {
            boolean undo = text0 != null && isSeparateUndo()
                    && !ForgeConstants.UNDO_MODE_OFF.equals(FModel.getPreferences().getPref(FPref.UI_UNDO_MODE))
                    && text0.startsWith(Forge.getLocalizer().getMessage("lblUndo"));
            if (undo) {
                text0 = Forge.getLocalizer().getMessage("lblEndTurn");
            }
            super.setText(text0);
            if (undoRequested != undo) {
                undoRequested = undo;
                if (btnUndo != null) { //not while the prompt is still being built
                    undoShown = undo;
                    VPrompt.this.revalidate();
                }
            }
        }
    }

    private void onCancelPressed(FEventHandler command, FEvent e) {
        if (undoRequested) { //the game still treats Cancel as Undo here, but this button is End Turn
            YieldController.endTurn(MatchController.instance.getGameController(), MatchController.instance.getCurrentPlayer());
        } else if (command != null) {
            command.handleEvent(e);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        boolean showUndo = undoShown;
        btnUndo.setVisible(showUndo);
        float undoSpace = showUndo ? UNDO_SIZE + 2 * UNDO_GAP : 0;

        float labelLeft = BTN_WIDTH;
        float undoX;
        if (Forge.reversedPrompt) { //End Turn on the left
            btnOk.setCorner(Corner.BottomRight);
            btnCancel.setCorner(Corner.BottomLeft);
            btnCancel.setLeft(0);
            btnOk.setLeft(width - BTN_WIDTH);
            undoX = BTN_WIDTH + UNDO_GAP;
            labelLeft = BTN_WIDTH + undoSpace;
        } else { //End Turn on the right
            btnOk.setCorner(Corner.BottomLeft);
            btnCancel.setCorner(Corner.BottomRight);
            btnOk.setLeft(0);
            btnCancel.setLeft(width - BTN_WIDTH);
            undoX = width - BTN_WIDTH - UNDO_GAP - UNDO_SIZE;
        }
        lblMessage.setLeft(labelLeft);
        lblMessage.setWidth(width - 2 * BTN_WIDTH - undoSpace);
        btnUndo.setBounds(undoX, (height - UNDO_SIZE) / 2, UNDO_SIZE, UNDO_SIZE);
    }

    @Override
    protected void drawBackground(Graphics g) {
        g.fillRect(getBackColor(), 0, 0, getWidth(), getHeight());
    }
    
    private class MessageLabel extends FDisplayObject {
        private final TextRenderer renderer = new TextRenderer();

        @Override
        public boolean tap(float x, float y, int count) {
            //if not enough room for prompt at given size, show magnify view
            float maxWidth = getWidth() - 2 * PADDING;
            float maxHeight = getHeight() - 2 * PADDING;
            TextBounds textBounds = renderer.getWrappedBounds(message, FONT, maxWidth);
            if (textBounds.height > maxHeight) {
                FMagnifyView.show(this, message, getForeColor(), getBackColor(), FONT, false);
            }
            return true;
        }

        @Override
        public boolean fling(float x, float y) {
            if (card != null) {
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (card != null) {
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (!StringUtils.isEmpty(message)) {
                float x = PADDING;
                float y = PADDING;
                float w = getWidth() - 2 * PADDING;
                float h = getHeight() - 2 * PADDING;
                renderer.drawText(g, message, FONT, getForeColor(), x, y, w, h, y, h, true, Align.center, true);
            }
        }
    }
}

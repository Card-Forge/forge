package forge.screens.match;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FTextArea;
import forge.util.Utils;

/**
 * The note the "How to Play" tutorial shows on the match screen. Unlike a dialog it does not dim or block anything, so
 * the cards it talks about stay in view, and the player can collapse it to a thin bar to see the whole board.
 *
 * <p>Pressing Next hands control back to the tutorial, which then shows the next note or makes the next move.
 * Button texts come from the language files (btnTutNext, btnTutHide, btnTutShow).
 */
public class CoachPanel extends FContainer {
    private static final float PAD = Utils.scale(6);
    private static final float BUTTON_HEIGHT = Utils.scale(32);
    private static final float BUTTON_WIDTH = Utils.scale(74);
    private static final float TITLE_HEIGHT = Utils.scale(22);
    private static final float RADIUS = Utils.scale(8);
    private static final float BORDER = Math.max(1f, Utils.scale(1));
    private static final Color BACKDROP = new Color(20 / 255f, 26 / 255f, 34 / 255f, 0.95f);
    private static final Color EDGE = new Color(0.45f, 0.52f, 0.62f, 0.95f);
    private static final Color TITLE_COLOR = new Color(1f, 0.82f, 0.35f, 1f);
    private static final Color BACKDROP_CLEAR = new Color(20 / 255f, 26 / 255f, 34 / 255f, 0f); // fade edge of BACKDROP
    private static final Color THUMB = new Color(1f, 0.82f, 0.35f, 0.9f);
    private static final Color THUMB_TRACK = new Color(1f, 1f, 1f, 0.15f);

    // fonts come from the loaded skin, so they are made with the panel rather than when the class loads
    private FSkinFont titleFont = FSkinFont.get(15);
    private final FTextArea body = add(new FTextArea(false));
    private final FButton btnNext = add(new FButton(msg("btnTutNext")));
    private final FButton btnHide = add(new FButton(msg("btnTutHide")));

    private final Runnable relayout;
    private String title = "";
    private boolean collapsed;
    private boolean compact; // portrait: a taller panel with smaller text, since the screen is narrow
    private Runnable onNext;

    private static String msg(final String key) {
        return Forge.getLocalizer().getMessage(key);
    }

    /** @param relayout0 asks the match screen to size this panel again, which it does when the panel's height changes */
    public CoachPanel(final Runnable relayout0) {
        relayout = relayout0;
        body.setFont(FSkinFont.get(12));
        body.setTextColor(FSkinColor.getStandardColor(Color.WHITE));
        btnNext.setFont(FSkinFont.get(13));
        btnHide.setFont(FSkinFont.get(13));
        btnNext.setCommand(e -> next());
        btnHide.setCommand(e -> {
            collapsed = !collapsed;
            updateButtons();
            relayout.run();
        });
        setVisible(false);
    }

    /** Shows a note. A previous note that was never answered is replaced, and the panel opens if it was collapsed. */
    public void show(final String title0, final String text, final String nextLabel, final Runnable onNext0) {
        title = title0 == null ? "" : title0;
        onNext = onNext0;
        collapsed = false;
        body.setText(text == null ? "" : text);
        body.scrollToTop(); // a long note that was scrolled must not leave the next one starting halfway down
        btnNext.setText(nextLabel == null || nextLabel.isEmpty() ? msg("btnTutNext") : nextLabel);
        btnNext.setEnabled(true);
        updateButtons();
        setVisible(true);
        relayout.run();
        Gdx.graphics.requestRendering(); // the screen only redraws on demand; the note must appear without a touch
    }

    public void hidePanel() {
        onNext = null;
        setVisible(false);
        Gdx.graphics.requestRendering();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Switches between the landscape and portrait looks. Portrait is narrow, so the text is smaller and the panel
     * taller to keep a whole note readable without scrolling.
     */
    public void setCompact(final boolean compact0) {
        if (compact == compact0) {
            return;
        }
        compact = compact0;
        titleFont = FSkinFont.get(compact ? 13 : 15);
        body.setFont(FSkinFont.get(compact ? 10 : 12));
        btnNext.setFont(FSkinFont.get(compact ? 12 : 13));
        btnHide.setFont(FSkinFont.get(compact ? 12 : 13));
    }

    /** How tall the panel wants to be in a space of the given height: a bar when collapsed, else a share of the screen. */
    public float preferredHeight(final float availableHeight) {
        if (collapsed) {
            return BUTTON_HEIGHT + 2 * PAD;
        }
        return compact
                ? Math.min(availableHeight * 0.5f, Utils.scale(270))
                : Math.min(availableHeight * 0.42f, Utils.scale(210));
    }

    private void updateButtons() {
        btnHide.setText(msg(collapsed ? "btnTutShow" : "btnTutHide"));
    }

    private void next() {
        final Runnable r = onNext;
        onNext = null;
        if (r != null) {
            btnNext.setEnabled(false); // answered; the tutorial shows the next note when it is ready
            r.run();
        }
    }

    @Override
    protected void doLayout(final float width, final float height) {
        final float buttonsY = height - PAD - BUTTON_HEIGHT;
        btnNext.setBounds(width - PAD - BUTTON_WIDTH, buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);
        btnHide.setBounds(width - 2 * (PAD + BUTTON_WIDTH), buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);
        if (collapsed) {
            body.setVisible(false);
        } else {
            body.setVisible(true);
            body.setBounds(PAD, PAD + TITLE_HEIGHT, width - 2 * PAD, Math.max(0, buttonsY - PAD - (PAD + TITLE_HEIGHT)));
        }
    }

    @Override
    protected void drawBackground(final Graphics g) {
        // The border is a slightly larger filled shape behind the backdrop. Graphics.drawRoundRect would outline its
        // three corner pieces separately and leave stray lines across the inside of the panel.
        g.fillRoundRect(EDGE, 0, 0, getWidth(), getHeight(), RADIUS);
        g.fillRoundRect(BACKDROP, BORDER, BORDER, getWidth() - 2 * BORDER, getHeight() - 2 * BORDER, RADIUS - BORDER);
        final float titleWidth = getWidth() - 2 * (BUTTON_WIDTH + PAD) - 2 * PAD;
        if (collapsed) {
            g.drawText(title, titleFont, TITLE_COLOR, PAD, getHeight() - PAD - BUTTON_HEIGHT, titleWidth,
                    BUTTON_HEIGHT, false, Align.left, true);
        } else {
            g.drawText(title, titleFont, TITLE_COLOR, PAD, PAD, getWidth() - 2 * PAD, TITLE_HEIGHT, false,
                    Align.left, true);
        }
    }

    /**
     * Shows that a long note scrolls: a thumb in the margin beside the text, and a fade with an arrow at whichever edge
     * has more text beyond it. Without these a note that is taller than the panel just looks cut off. Drawn over the
     * text, after the children, and only while the text actually overflows.
     */
    @Override
    protected void drawOverlay(final Graphics g) {
        if (collapsed || !body.isVisible()) {
            return;
        }
        final float overflow = body.getMaxScrollTop();
        if (overflow < 1f) {
            return; // the whole note fits
        }
        final float top = body.getScrollTop();
        final float bx = body.getLeft(), by = body.getTop(), bw = body.getWidth(), bh = body.getHeight();
        final float fade = Math.min(Utils.scale(26), bh / 3f);
        final float arrow = Utils.scale(5);

        if (top < overflow - 1f) { // more below
            g.fillGradientRect(BACKDROP_CLEAR, BACKDROP, true, bx, by + bh - fade, bw, fade);
            final float cx = bx + bw / 2f, cy = by + bh - Utils.scale(4);
            g.fillTriangle(TITLE_COLOR, cx - arrow, cy - arrow, cx + arrow, cy - arrow, cx, cy);
        }
        if (top > 1f) { // already scrolled: more above
            g.fillGradientRect(BACKDROP, BACKDROP_CLEAR, true, bx, by, bw, fade);
            final float cx = bx + bw / 2f, cy = by + Utils.scale(4);
            g.fillTriangle(TITLE_COLOR, cx - arrow, cy + arrow, cx + arrow, cy + arrow, cx, cy);
        }

        // scroll thumb, in the margin right of the text so it never covers a letter
        final float thumbW = Math.max(2f, Utils.scale(3));
        final float tx = bx + bw + (PAD - thumbW) / 2f;
        final float thumbH = Math.max(Utils.scale(16), bh * bh / (bh + overflow));
        final float ty = by + (bh - thumbH) * Math.min(1f, Math.max(0f, top / overflow));
        g.fillRoundRect(THUMB_TRACK, tx, by, thumbW, bh, thumbW / 2f);
        g.fillRoundRect(THUMB, tx, ty, thumbW, thumbH, thumbW / 2f);
    }

    /** Taps on the panel itself must not fall through to the cards underneath it. */
    @Override
    public boolean press(final float x, final float y) {
        return true;
    }
}

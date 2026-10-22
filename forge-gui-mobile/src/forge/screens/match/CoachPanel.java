package forge.screens.match;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

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

    // fonts come from the loaded skin, so they are made with the panel rather than when the class loads
    private FSkinFont titleFont = FSkinFont.get(15);
    private final FTextArea body = add(new FTextArea(false));
    private final FButton btnNext = add(new FButton("Next"));
    private final FButton btnHide = add(new FButton("Hide"));

    private final Runnable relayout;
    private String title = "";
    private boolean collapsed;
    private boolean compact; // portrait: a taller panel with smaller text, since the screen is narrow
    private Runnable onNext;

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
        btnNext.setText(nextLabel == null || nextLabel.isEmpty() ? "Next" : nextLabel);
        btnNext.setEnabled(true);
        updateButtons();
        setVisible(true);
        relayout.run();
    }

    public void hidePanel() {
        onNext = null;
        setVisible(false);
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
        btnHide.setText(collapsed ? "Show" : "Hide");
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

    /** Taps on the panel itself must not fall through to the cards underneath it. */
    @Override
    public boolean press(final float x, final float y) {
        return true;
    }
}

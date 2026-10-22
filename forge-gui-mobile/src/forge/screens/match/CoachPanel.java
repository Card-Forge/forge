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
    private static final Color BACKDROP = new Color(20 / 255f, 26 / 255f, 34 / 255f, 0.93f);
    private static final Color EDGE = new Color(1f, 1f, 1f, 0.22f);
    private static final Color TITLE_COLOR = new Color(1f, 0.82f, 0.35f, 1f);

    // fonts come from the loaded skin, so they are made with the panel rather than when the class loads
    private final FSkinFont titleFont = FSkinFont.get(15);
    private final FTextArea body = add(new FTextArea(false));
    private final FButton btnNext = add(new FButton("Next"));
    private final FButton btnHide = add(new FButton("Hide"));

    private final Runnable relayout;
    private String title = "";
    private boolean collapsed;
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

    /** How tall the panel wants to be in a space of the given height: a bar when collapsed, else a share of the screen. */
    public float preferredHeight(final float availableHeight) {
        if (collapsed) {
            return BUTTON_HEIGHT + 2 * PAD;
        }
        return Math.min(availableHeight * 0.42f, Utils.scale(210));
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
        g.fillRoundRect(BACKDROP, 0, 0, getWidth(), getHeight(), RADIUS);
        g.drawRoundRect(1, EDGE, 0, 0, getWidth(), getHeight(), RADIUS);
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

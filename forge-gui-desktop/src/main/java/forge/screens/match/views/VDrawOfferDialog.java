package forge.screens.match.views;

import javax.swing.SwingConstants;

import forge.game.DrawOffer;
import forge.game.player.PlayerView;
import forge.gamemodes.match.DrawOfferMessage;
import forge.gui.UiCommand;
import forge.screens.match.CMatchUI;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.util.Localizer;
import forge.view.FDialog;

/**
 * Non-modal live-tally dialog for an in-flight draw offer. A single instance is
 * kept by {@link CMatchUI} and {@link #refresh} is called on every broadcast so
 * the tally updates in place; {@link #showResult} renders the terminal outcome
 * and stays open until the player dismisses it.
 */
@SuppressWarnings("serial")
public class VDrawOfferDialog extends FDialog {
    private static final int PADDING = 12;
    private static final int WIDTH = 320;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 26;

    private final CMatchUI matchUI;
    private PlayerView localResponder;

    public VDrawOfferDialog(final CMatchUI matchUI) {
        super(false, false, "0");
        this.matchUI = matchUI;
        setTitle(Localizer.getInstance().getMessage("lblOfferDrawTitle"));
    }

    /** Rebuild the tally for {@code update}; show Accept/Decline if {@code localResponder} still owes a vote. */
    public void refresh(final DrawOfferMessage.Status update, final PlayerView localResponder) {
        this.localResponder = localResponder;
        final Localizer localizer = Localizer.getInstance();
        removeBody();

        int y = addTally(update, localizer, PADDING);

        y += 6;
        if (localResponder != null) {
            final int btnX = (WIDTH - 2 * BUTTON_WIDTH - PADDING) / 2;
            final FButton accept = new FButton(localizer.getMessage("lblAccept"));
            accept.setCommand((UiCommand) () -> respond(true));
            add(accept, btnX, y, BUTTON_WIDTH, BUTTON_HEIGHT);

            final FButton decline = new FButton(localizer.getMessage("lblDecline"));
            decline.setCommand((UiCommand) () -> respond(false));
            add(decline, btnX + BUTTON_WIDTH + PADDING, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            y += BUTTON_HEIGHT;
        } else {
            final int btnX = (WIDTH - BUTTON_WIDTH) / 2;
            final FButton close = new FButton(localizer.getMessage("lblClose"));
            close.setCommand((UiCommand) () -> setVisible(false));
            add(close, btnX, y, BUTTON_WIDTH, BUTTON_HEIGHT);
            y += BUTTON_HEIGHT;
        }
        y += PADDING;

        finishLayout(y);
    }

    private int addTally(final DrawOfferMessage.Status update, final Localizer localizer, int y) {
        final int w = WIDTH - 2 * PADDING;
        final FLabel header = new FLabel.Builder()
                .text(localizer.getMessage("lblDrawOfferedBy", update.offerer().getName()))
                .fontAlign(SwingConstants.LEFT).build();
        add(header, PADDING, y, w, ROW_HEIGHT);
        y += ROW_HEIGHT + 4;
        for (final DrawOfferMessage.Entry entry : update.entries()) {
            final FLabel row = new FLabel.Builder()
                    .text(entry.player().getName() + ": " + voteText(entry.vote(), localizer))
                    .fontSize(12).fontAlign(SwingConstants.LEFT).build();
            add(row, PADDING, y, w, ROW_HEIGHT);
            y += ROW_HEIGHT;
        }
        return y;
    }

    /** Render the final tally and outcome; stays open until the player dismisses it. */
    public void showResult(final DrawOfferMessage.Status update) {
        final Localizer localizer = Localizer.getInstance();
        removeBody();

        final int w = WIDTH - 2 * PADDING;
        int y = addTally(update, localizer, PADDING);

        y += 6;
        final String msg = update.result() == DrawOfferMessage.Result.ACCEPTED
                ? localizer.getMessage("lblDrawAccepted")
                : localizer.getMessage("lblDrawDeclined");
        final FLabel result = new FLabel.Builder()
                .text(msg)
                .fontAlign(SwingConstants.LEFT).build();
        add(result, PADDING, y, w, ROW_HEIGHT * 2);
        y += ROW_HEIGHT * 2 + 6;

        final int btnX = (WIDTH - BUTTON_WIDTH) / 2;
        final FButton close = new FButton(localizer.getMessage("lblClose"));
        close.setCommand((UiCommand) this::dispose);
        add(close, btnX, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        y += BUTTON_HEIGHT + PADDING;

        finishLayout(y);
    }

    private void respond(final boolean accept) {
        matchUI.getGameController(localResponder).drawOfferAction(accept ? DrawOfferMessage.Action.ACCEPT : DrawOfferMessage.Action.DECLINE);
        setVisible(false);
    }

    private String voteText(final DrawOffer.Vote vote, final Localizer localizer) {
        return switch (vote) {
            case ACCEPTED -> localizer.getMessage("lblAccept");
            case DECLINED -> localizer.getMessage("lblDecline");
            case PENDING -> localizer.getMessage("lblDrawPending");
        };
    }

    private void removeBody() {
        getContentPane().removeAll();
    }

    private void finishLayout(final int height) {
        getContentPane().revalidate();
        getContentPane().repaint();
        pack(); // realize the title bar so its height is known before we size the frame
        setSize(WIDTH, height + getTitleBar().getHeight() + PADDING);
        if (!isVisible()) {
            setVisible(true);
        }
    }
}

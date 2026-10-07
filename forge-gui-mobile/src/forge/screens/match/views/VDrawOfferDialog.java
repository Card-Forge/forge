package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.game.DrawOffer;
import forge.game.player.PlayerView;
import forge.gamemodes.match.DrawOfferMessage;
import forge.screens.match.MatchController;
import forge.toolbox.FButton;
import forge.toolbox.FDialog;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

/**
 * Non-blocking live-tally dialog for an in-flight draw offer. A single instance
 * is kept by {@link MatchController}; {@link #refresh} is called on every
 * broadcast so the tally updates in place, and {@link #showResult} renders the
 * terminal outcome and stays open until the player dismisses it.
 */
public class VDrawOfferDialog extends FDialog {
    private static final float ROW_HEIGHT = Utils.scale(26);
    private static final float BUTTON_HEIGHT = Utils.scale(34);
    private static final float GAP = Utils.scale(8);

    private final List<FDisplayObject> body = new ArrayList<>();
    private PlayerView localResponder;

    public VDrawOfferDialog() {
        super(Forge.getLocalizer().getMessage("lblOfferDrawTitle"), 0);
    }

    public void refresh(final DrawOfferMessage.Status update, final PlayerView localResponder) {
        this.localResponder = localResponder;
        clearBody();
        addTally(update);
        if (localResponder != null) {
            addButton(Forge.getLocalizer().getMessage("lblAccept"), e -> respond(true));
            addButton(Forge.getLocalizer().getMessage("lblDecline"), e -> respond(false));
        } else {
            addButton(Forge.getLocalizer().getMessage("lblClose"), e -> hide());
        }
        present();
    }

    public void showResult(final DrawOfferMessage.Status update) {
        clearBody();
        addTally(update);
        body.add(add(new FLabel.Builder()
                .text(update.result() == DrawOfferMessage.Result.ACCEPTED
                        ? Forge.getLocalizer().getMessage("lblDrawAccepted")
                        : Forge.getLocalizer().getMessage("lblDrawDeclined"))
                .align(Align.left)
                .build()));
        addButton(Forge.getLocalizer().getMessage("lblClose"), e -> hide());
        present();
    }

    private void addTally(final DrawOfferMessage.Status update) {
        body.add(add(new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblDrawOfferedBy", update.offerer().getName()))
                .align(Align.left)
                .build()));
        for (final DrawOfferMessage.Entry entry : update.entries()) {
            body.add(add(new FLabel.Builder()
                    .text(entry.player().getName() + ": " + voteText(entry.vote()))
                    .align(Align.left)
                    .build()));
        }
    }

    private void addButton(final String text, final FEventHandler command) {
        final FButton button = new FButton(text);
        button.setCommand(command);
        body.add(add(button));
    }

    private void clearBody() {
        for (final FDisplayObject child : body) {
            remove(child);
        }
        body.clear();
    }

    private void present() {
        if (isVisible()) {
            revalidate(true);
        } else {
            show();
        }
    }

    private void respond(final boolean accept) {
        MatchController.instance.getGameController(localResponder).drawOfferAction(
                accept ? DrawOfferMessage.Action.ACCEPT : DrawOfferMessage.Action.DECLINE);
        hide();
    }

    private static String voteText(final DrawOffer.Vote vote) {
        return switch (vote) {
            case ACCEPTED -> Forge.getLocalizer().getMessage("lblAccept");
            case DECLINED -> Forge.getLocalizer().getMessage("lblDecline");
            case PENDING -> Forge.getLocalizer().getMessage("lblDrawPending");
        };
    }

    @Override
    protected float layoutAndGetHeight(final float width, final float maxHeight) {
        final float padding = FOptionPane.PADDING;
        final float w = width - 2 * padding;
        float y = padding;
        for (final FDisplayObject child : body) {
            if (child instanceof FButton) {
                child.setBounds(padding, y, w, BUTTON_HEIGHT);
                y += BUTTON_HEIGHT + GAP;
            } else {
                child.setBounds(padding, y, w, ROW_HEIGHT);
                y += ROW_HEIGHT;
            }
        }
        return y + padding;
    }
}

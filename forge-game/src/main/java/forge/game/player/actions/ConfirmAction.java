package forge.game.player.actions;

import forge.game.GameEntityView;
import forge.game.card.CardView;

public class ConfirmAction extends PlayerAction {
    private final boolean confirmed;

    public ConfirmAction(final GameEntityView cardView, final boolean confirmed) {
        super(cardView, confirmed ? "Confirm" : "Decline");
        this.confirmed = confirmed;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean matchesPrompt(final CardView inputCard, final String message) {
        final GameEntityView recordedView = getGameEntityView();
        if (!(recordedView instanceof CardView recordedCard)) {
            return true;
        }
        return (inputCard != null && recordedCard.getName().equals(inputCard.getName()))
                || (message != null && message.contains(recordedCard.getName()));
    }

    @Override
    public String describe() {
        final String action = localize(confirmed ? "lblMacroActionConfirm" : "lblMacroActionDecline");
        final String entity = describeEntity();
        return entity.isEmpty() ? action : localize("lblMacroActionChoiceFor", action, entity);
    }
}

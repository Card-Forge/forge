package forge.game.player.actions;

import forge.game.GameEntityView;

public class ConfirmAction extends PlayerAction {
    private final boolean confirmed;

    public ConfirmAction(final GameEntityView cardView, final boolean confirmed) {
        super(cardView, confirmed ? "Confirm" : "Decline");
        this.confirmed = confirmed;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" confirmed=").append(confirmed);
    }
}

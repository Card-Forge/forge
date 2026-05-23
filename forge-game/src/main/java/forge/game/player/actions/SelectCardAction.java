package forge.game.player.actions;

import forge.game.GameEntityView;
import forge.game.card.CardView;

public class SelectCardAction extends PlayerAction {
    public SelectCardAction(GameEntityView cardView) {
        super(cardView, "Select card");
    }

    @Override
    public boolean isSelectionAction() {
        return true;
    }

    @Override
    public CardView getSelectedCardView() {
        return getGameEntityView() instanceof CardView cardView ? cardView : null;
    }

    @Override
    public String describe() {
        return localize("lblMacroActionSelectCard", describeEntity());
    }
}

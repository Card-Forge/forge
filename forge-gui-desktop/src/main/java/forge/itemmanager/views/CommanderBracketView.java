package forge.itemmanager.views;

import forge.deck.CommanderBracketCalculator;
import forge.deck.DeckProxy;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerModel;

@SuppressWarnings("serial")
public final class CommanderBracketView extends CommanderBracketTextView<DeckProxy> {
    public CommanderBracketView(final DeckManager itemManager0) {
        super(itemManager0, getModel(itemManager0));
    }

    private static ItemManagerModel<DeckProxy> getModel(final DeckManager itemManager) {
        return itemManager.getModel();
    }

    @Override
    protected String getText() {
        final DeckProxy deck = getSelectedItem();
        if (deck == null) {
            return localizer.getMessage("lblCommanderBracketSelectDeck");
        }
        return deck.getName() + "\n\n" + CommanderBracketCalculator.getExplanation(deck.getDeck());
    }
}

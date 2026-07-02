package forge.itemmanager.views;

import forge.deck.CommanderBracketService;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerModel;

@SuppressWarnings("serial")
public final class CommanderBracketView extends CommanderBracketTextView<DeckProxy> {
    private DeckProxy materializedProxy;
    private Deck materializedDeck;

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
            materializedProxy = null;
            materializedDeck = null;
            return localizer.getMessage("lblCommanderBracketSelectDeck");
        }
        return deck.getName() + "\n\n" + CommanderBracketService.getExplanation(getMaterializedDeck(deck), deck);
    }

    @Override
    protected boolean isRefreshPending() {
        return CommanderBracketService.isPending(materializedDeck);
    }

    private Deck getMaterializedDeck(final DeckProxy deck) {
        if (deck != materializedProxy) {
            materializedProxy = deck;
            materializedDeck = deck.getDeck();
        }
        return materializedDeck;
    }
}

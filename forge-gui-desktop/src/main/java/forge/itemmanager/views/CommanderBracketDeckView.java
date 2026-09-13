package forge.itemmanager.views;

import forge.deck.CommanderBracketService;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerModel;

@SuppressWarnings("serial")
public final class CommanderBracketDeckView extends CommanderBracketTextView<PaperCard> {
    private final Deck deck;
    private final DeckProxy deckProxy;

    public CommanderBracketDeckView(final CardManager itemManager0, final ItemManagerModel<PaperCard> model0, final Deck deck0, final DeckProxy deckProxy0) {
        super(itemManager0, model0);
        this.deck = deck0;
        this.deckProxy = deckProxy0;
        updateText();
    }

    @Override
    protected String getText() {
        return deck.getName() + "\n\n" + CommanderBracketService.getExplanation(deck, deckProxy);
    }

    @Override
    protected boolean isRefreshPending() {
        return CommanderBracketService.isPending(deck);
    }
}

package forge.itemmanager.views;

import forge.deck.CommanderBracketCalculator;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerModel;

@SuppressWarnings("serial")
public final class CommanderBracketDeckView extends CommanderBracketTextView<PaperCard> {
    private final Deck deck;

    public CommanderBracketDeckView(final CardManager itemManager0, final ItemManagerModel<PaperCard> model0, final Deck deck0) {
        super(itemManager0, model0);
        this.deck = deck0;
        updateText();
    }

    @Override
    protected String getText() {
        return deck.getName() + "\n\n" + CommanderBracketCalculator.getExplanation(deck);
    }
}

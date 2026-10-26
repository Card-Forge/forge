package forge.screens.constructed;

import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.item.PaperCard;

/**
 * An entry in the lobby's commander picker. Choice lists draw an {@link IHasCardView}
 * as a card thumbnail next to its text, and zoom the card when it is tapped.
 */
public class CommanderChoice<T> implements IHasCardView {
    private final T value;
    private final String label;
    private final PaperCard card;
    private CardView cardView;

    /** @param card the card to show next to the label, or null for none */
    public CommanderChoice(final T value, final String label, final PaperCard card) {
        this.value = value;
        this.label = label;
        this.card = card;
    }

    public T getValue() {
        return value;
    }

    @Override
    public CardView getCardView() {
        // Drawn every frame, so build the view once
        if (cardView == null && card != null) {
            cardView = CardView.getCardForUi(card);
        }
        return cardView;
    }

    @Override
    public String toString() {
        return label;
    }
}

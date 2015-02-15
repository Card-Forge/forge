package forge.deck.generation;

import com.google.common.base.Predicate;

import forge.item.PaperCard;

public interface IDeckGenPool {
    PaperCard getCard(String name);
    PaperCard getCard(String name, String edition);
    PaperCard getCard(String name, String edition, int artIndex);
    Iterable<PaperCard> getAllCards();
    Iterable<PaperCard> getAllCards(Predicate<PaperCard> filter);
}

package forge.deck.generation;

import forge.item.PaperCard;

import java.util.function.Predicate;

public interface IDeckGenPool {
    PaperCard getCard(String name);
    PaperCard getCard(String name, String edition);
    PaperCard getCard(String name, String edition, int artIndex);
    Iterable<PaperCard> getAllCards();
    Iterable<PaperCard> getAllCards(Predicate<PaperCard> filter);
    boolean contains(String name);
}

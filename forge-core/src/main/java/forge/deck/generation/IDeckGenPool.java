package forge.deck.generation;

import forge.item.PaperCard;

public interface IDeckGenPool {
    PaperCard getCard(String name);
    PaperCard getCard(String name, String edition);
    PaperCard getCard(String name, String edition, int artIndex);
    Iterable<PaperCard> getAllCards();
}

package forge;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class FCollectionTest {
    /**
     * Just a quick test for FCollection.
     */
    /*@Test
    void testBadIteratorLogic() {
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            cards.add(new Card(i, null));
        CardCollection cc = new CardCollection(cards);
        Iterator<Card> it = cc.iterator();
        it.next();
        it.remove();
        assertEquals(cc.size(), 3);
    }

    /*@Test
    void testBadIteratorLogicTwo() {
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i <= 10; i++)
            cards.add(new Card(i, null));
        CardCollection cc = new CardCollection(cards);
        int i = 0;
        for (Card c : cc) {
            if (i != 3)
                cc.remove(c);  // throws error if the CardCollection not threadsafe
            i++;
        }
        assertEquals(cc.size(), 1);
    }*/// Commented out since the collection doesn't support modification while iterating over it directly

    /**
     * {@link forge.util.collect.FCollection#threadSafeIterable()} hands out a snapshot, so removing
     * from the collection while looping over it neither throws nor skips an element.
     */
    @Test
    void testRemoveWhileIterating() {
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            cards.add(new Card(i, null));
        CardCollection cc = new CardCollection(cards);
        int seen = 0;
        for (Card c : cc.threadSafeIterable()) {
            seen++;
            if (c.getId() % 2 > 0)
                cc.remove(c);
        }
        assertEquals(seen, 4);
        assertEquals(cc.size(), 2);
        for (Card c : cc)
            assertEquals(c.getId() % 2, 0);
    }
}

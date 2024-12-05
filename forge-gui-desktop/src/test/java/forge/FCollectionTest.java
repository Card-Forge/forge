package forge;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    }*/// Commented out since we use synchronized collection and it doesn't support modification while iteration

    @Test
    void testCompletableFuture() {
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            cards.add(new Card(i, null));
        CardCollection cc = new CardCollection(cards);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (Card c : cc.threadSafeIterable()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                if (c.getId() % 2 > 0)
                    cc.remove(c);
                return 0;
            }));
        }
        CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futuresArray).join();
        futures.clear();
        assertEquals(cc.size(), 2);
    }
}

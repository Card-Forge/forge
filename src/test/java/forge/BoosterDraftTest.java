package forge;

import forge.card.BoosterGenerator;
import forge.deck.Deck;

import forge.game.limited.BoosterDraft;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

import org.testng.annotations.Test;

/**
 * <p>BoosterDraftTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class BoosterDraftTest implements BoosterDraft {
    int n = 3;

    /**
     * <p>getDecks.</p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    @Test(timeOut = 1000)
    public Deck[] getDecks() {
        return null;
    }

    /**
     * <p>nextChoice.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public ItemPoolView<CardPrinted> nextChoice() {
        n--;
        BoosterGenerator pack = new BoosterGenerator(SetUtils.getSetByCode("M11"));
        return ItemPool.createFrom(pack.getBoosterPack(), CardPrinted.class);
    }

    /** {@inheritDoc} */
    public void setChoice(CardPrinted c) {
        System.out.println(c.getName());
    }

    /**
     * <p>hasNextChoice.</p>
     *
     * @return a boolean.
     */
    public boolean hasNextChoice() {
        return n > 0;
    }

    /**
     * <p>getChosenCards.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getChosenCards() {
        return null;
    }

    /**
     * <p>getUnchosenCards.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getUnchosenCards() {
        return null;
    }

    @Override
    public void finishedDrafting() {

    }
}

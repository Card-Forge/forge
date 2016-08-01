package forge;

import forge.card.BoosterGenerator;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.limited.IBoosterDraft;
import forge.model.FModel;

import org.testng.annotations.Test;

import java.util.List;

/**
 * <p>
 * BoosterDraftTest class.
 * </p>
 *
 * @author Forge
 * @version $Id: BoosterDraftTest.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class BoosterDraftTest implements IBoosterDraft {

    /** The n. */
    private int n = 3;

    /**
     * <p>
     * getDecks.
     * </p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    @Override
    @Test(timeOut = 1000)
    public Deck[] getDecks() {
        return null;
    }

    /**
     * <p>
     * nextChoice.
     * </p>
     *
     * @return a {@link forge.CardList} object.
     */
    @Override
    public CardPool nextChoice() {
        this.n--;
        SealedProduct.Template booster = FModel.getMagicDb().getBoosters().get("M11");
        CardPool result = new CardPool();
        result.addAllFlat(BoosterGenerator.getBoosterPack(booster));
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void setChoice(final PaperCard c) {
        System.out.println(c.getName());
    }

    /**
     * <p>
     * hasNextChoice.
     * </p>
     *
     * @return a boolean.
     */
    @Override
    public boolean hasNextChoice() {
        return this.n > 0;
    }

    @Override
    public boolean isRoundOver() {
        return hasNextChoice();
    }

    /**
     * <p>
     * getChosenCards.
     * </p>
     *
     * @return a {@link forge.CardList} object.
     */
    public List<Card> getChosenCards() {
        return null;
    }

    /**
     * <p>
     * getUnchosenCards.
     * </p>
     *
     * @return a {@link forge.CardList} object.
     */
    public List<Card> getUnchosenCards() {
        return null;
    }

    @Override
    public boolean isPileDraft() {
        return false;
    }
}

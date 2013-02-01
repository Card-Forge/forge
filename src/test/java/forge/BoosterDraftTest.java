package forge;

import java.util.List;

import org.testng.annotations.Test;

import forge.card.BoosterData;
import forge.card.BoosterGenerator;
import forge.deck.Deck;
import forge.game.limited.IBoosterDraft;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * BoosterDraftTest class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
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
    public ItemPoolView<CardPrinted> nextChoice() {
        this.n--;
        BoosterData booster = Singletons.getModel().getBoosters().get("M11");
        final BoosterGenerator pack = new BoosterGenerator(booster.getEditionFilter());
        return ItemPool.createFrom(pack.getBoosterPack(booster), CardPrinted.class, false);
    }

    /** {@inheritDoc} */
    @Override
    public void setChoice(final CardPrinted c) {
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

    /*
     * (non-Javadoc)
     * 
     * @see forge.game.limited.IBoosterDraft#finishedDrafting()
     */
    @Override
    public void finishedDrafting() {

    }
}

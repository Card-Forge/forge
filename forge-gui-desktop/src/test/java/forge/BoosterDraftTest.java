package forge;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.gamemodes.limited.IBoosterDraft;
import forge.gamemodes.limited.IDraftLog;
import forge.gamemodes.limited.LimitedPlayer;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.item.generation.BoosterGenerator;
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

    private int n = 3;
    private int round = 1;

    @Override
    @Test(timeOut = 1000)
    public Deck[] getDecks() {
        return null;
    }

    @Override
    public LimitedPlayer[] getOpposingPlayers() {
        return new LimitedPlayer[0];
    }

    @Override
    public LimitedPlayer getHumanPlayer() {
        return null;
    }

    @Override
    public int getRound() {
        return round;
    }

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

    @Override
    public boolean hasNextChoice() {
        return this.n > 0;
    }

    @Override
    public boolean isRoundOver() {
        return hasNextChoice();
    }

    public List<Card> getChosenCards() {
        return null;
    }

    public List<Card> getUnchosenCards() {
        return null;
    }

    @Override
    public boolean isPileDraft() {
        return false;
    }

    @Override
    public void setLogEntry(IDraftLog draftingProcess) {

    }

    @Override
    public IDraftLog getDraftLog() {
        return null;
    }

    @Override
    public LimitedPlayer getNeighbor(LimitedPlayer p, boolean left) {
        return null;
    }

    @Override
    public LimitedPlayer getPlayer(int i) {
        return null;
    }
}

package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * "Couldn't map Barrowgoyf" (Flat MC rollout failed): a static effect whose source is no longer in any copied zone
 * used to make the whole copy fail, so the AI fell back to the heuristics for that decision.
 */
public class GameCopierOrphanEffectTest extends SimulationTest {

    @Test
    public void aStaticEffectWhoseSourceIsNoLongerInAZoneDoesNotBreakTheCopy() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCard("Grizzly Bears", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        final Card barrowgoyf = addCard("Barrowgoyf", p);
        barrowgoyf.getZone().remove(barrowgoyf); // the source is in no zone any more
        // Its characteristic-defining effect is still listed (nothing has run a state check since): the case of the
        // error. It is made here, after the removal, so that nothing can clean it up before the copy is made.
        game.getStaticEffects().getStaticEffect(barrowgoyf.getStaticAbilities().iterator().next());
        AssertJUnit.assertTrue(game.getStaticEffects().getEffects().iterator().hasNext());

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertNotNull(copy);
        AssertJUnit.assertEquals(1, copy.getCardsIn(ZoneType.Battlefield).size());
        AssertJUnit.assertEquals(1, copy.getCardsIn(ZoneType.Graveyard).size());
    }

    /** The usual cause: the effect holds a last-known-information copy of the card (same id, another object). */
    @Test
    public void anEffectOfAnOldCopyOfACardIsUndoneOnTheCardInTheCopy() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        final Card barrowgoyf = addCard("Barrowgoyf", p);
        final Card old = forge.game.card.CardCopyService.getLKICopy(barrowgoyf);
        AssertJUnit.assertNotSame(barrowgoyf, old);
        AssertJUnit.assertEquals(barrowgoyf.getId(), old.getId());
        game.getStaticEffects().getStaticEffect(old.getStaticAbilities().iterator().next());

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertNotNull(copy);
        AssertJUnit.assertEquals(1, copy.getCardsIn(ZoneType.Battlefield).size());
    }

    @Test
    public void aBarrowgoyfThatIsStillInPlayIsCopiedWithItsPower() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCardToZone("Grizzly Bears", p, ZoneType.Graveyard); // one card type in the graveyards: power 1, toughness 2
        final Card barrowgoyf = addCard("Barrowgoyf", p);
        game.getAction().checkStateEffects(true);

        final Game copy = new GameCopier(game).makeCopy();

        for (final Card c : copy.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Barrowgoyf")) {
                AssertJUnit.assertEquals(barrowgoyf.getNetPower(), c.getNetPower());
                return;
            }
        }
        AssertJUnit.fail("the copy has no Barrowgoyf");
    }

    @Test
    public void aSourceThatIsStillInPlayHasItsEffectAppliedOnceInTheCopy() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        addCard("Glorious Anthem", p);
        game.getAction().checkStateEffects(true);

        final Game copy = new GameCopier(game).makeCopy();

        // undone and applied again by the copy's own state check: +1/+1 once, not twice
        for (final Card c : copy.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Grizzly Bears")) {
                AssertJUnit.assertEquals(bears.getNetPower(), c.getNetPower());
            }
        }
    }
}

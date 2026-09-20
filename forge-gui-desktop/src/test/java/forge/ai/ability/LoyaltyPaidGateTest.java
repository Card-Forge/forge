package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Way of the Mind Sculptor draws only when two or more loyalty counters were removed to activate a
 * loyalty ability, so the trigger has to read the amount actually paid rather than the printed cost.
 * Carth the Lion is the case that tells those apart: it raises every loyalty cost by [+1], so a
 * printed [-2] removes one counter and must not draw. Each case asserts the loyalty actually spent
 * as well, otherwise a harness that skipped cost adjustment would pass on the draw count alone.
 * Cards drawn are counted off the library so an unrelated AI play from hand cannot be mistaken for
 * the draw.
 */
public class LoyaltyPaidGateTest extends AITest {

    @Test
    public void drawsWhenTwoOrMoreLoyaltyCountersWerePaid() {
        assertActivation("Garruk Wildspeaker", ApiType.PumpAll, false, 4, 1);
    }

    @Test
    public void doesNotDrawWhenOnlyOneLoyaltyCounterWasPaid() {
        assertActivation("Garruk Wildspeaker", ApiType.Token, false, 1, 0);
    }

    /** Carth turns Garruk's printed [-4] into three counters removed, which still draws. */
    @Test
    public void drawsOnRaisedCostThatStillRemovesTwo() {
        assertActivation("Garruk Wildspeaker", ApiType.PumpAll, true, 3, 1);
    }

    /** Carth turns Sorin's printed [-2] into one counter removed, which must not draw. */
    @Test
    public void doesNotDrawWhenRaisedCostRemovesOnlyOne() {
        assertActivation("Sorin, Lord of Innistrad", ApiType.Effect, true, 1, 0);
    }

    /** Carth cancels Garruk's printed [-1] outright, so nothing is removed and nothing is drawn. */
    @Test
    public void doesNotDrawWhenRaisedCostRemovesNothing() {
        assertActivation("Garruk Wildspeaker", ApiType.Token, true, 0, 0);
    }

    private void assertActivation(String walkerName, ApiType api, boolean withCarth,
            int expectedLoyaltySpent, int expectedDraws) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Way of the Mind Sculptor", p);
        Card walker = addCard(walkerName, p);
        walker.setCounters(CounterEnumType.LOYALTY, 10);
        if (withCarth) {
            addCard("Carth the Lion", p);
        }
        fillLibrary(p, 5);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility candidate : walker.getSpellAbilities()) {
            if (candidate.getApi() == api) {
                sa = candidate;
                break;
            }
        }
        AssertJUnit.assertNotNull("no " + api + " ability on " + walkerName, sa);
        sa.setActivatingPlayer(p);

        int libraryBefore = p.getCardsIn(ZoneType.Library).size();
        int loyaltyBefore = walker.getCounters(CounterEnumType.LOYALTY);
        AssertJUnit.assertTrue("the loyalty ability was not activated",
                ComputerUtil.handlePlayingSpellAbility(p, sa, null));
        playUntilStackClear(game);

        AssertJUnit.assertEquals("wrong number of loyalty counters removed", expectedLoyaltySpent,
                loyaltyBefore - walker.getCounters(CounterEnumType.LOYALTY));
        AssertJUnit.assertEquals("wrong number of cards drawn", expectedDraws,
                libraryBefore - p.getCardsIn(ZoneType.Library).size());
    }
}

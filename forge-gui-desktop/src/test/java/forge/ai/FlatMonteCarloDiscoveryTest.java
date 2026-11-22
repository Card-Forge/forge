package forge.ai;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/** Regression tests for abilities that FlatMC discovery must not reintroduce after the heuristics reject them. */
public class FlatMonteCarloDiscoveryTest extends AITest {

    private static SpellAbility ability(final Card card, final ApiType api) {
        for (final SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void runawayPingIsNotAddedBackByDiscovery() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card analyst = addCard("Tal Shiar Analyst", ai);
        final SpellAbility ping = ability(analyst, ApiType.DealDamage);
        AssertJUnit.assertNotNull("Tal Shiar Analyst's ping ability", ping);
        ping.setActivatingPlayer(ai);
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();

        analyst.setSickness(true);
        AssertJUnit.assertFalse("summoning sickness makes the pinger's tap cost unpayable",
                brain.isStillCastable(ping));
        AssertJUnit.assertFalse("the tap cost must not succeed while the pinger is ability-sick",
                new forge.game.cost.CostTap().payAsDecided(ai, null, ping, false));
        analyst.setSickness(false);
        AssertJUnit.assertTrue("the ping is payable after summoning sickness ends", brain.isStillCastable(ping));
        AssertJUnit.assertTrue("a pinger is one of the abilities broad discovery may consider",
                forge.ai.simulation.FlatMonteCarloPicker.isDiscoverable(ping, true));

        for (int i = 0; i < RunawayLoop.LIMIT; i++) {
            RunawayLoop.note(ai, ping);
        }
        final AiPlayDecision decision = SpellApiToAi.Converter.get(ping).canPlayWithSubs(ai, ping).decision();
        AssertJUnit.assertEquals("the damage AI preserves the runaway rejection reason",
                AiPlayDecision.StopRunawayActivations, decision);
        AssertJUnit.assertFalse("FlatMC must not rediscover a runaway activation",
                brain.isLegalDiscovery(ping, decision));
    }

    @Test
    public void tappedDvanaCannotPassTheFinalDiscoveryLegalityCheck() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tendi = addCard("D'Vana Tendi, Optimistic Ensign", ai);
        addCard("Forest", ai); // a legal other permanent for the targeted untap ability

        final SpellAbility untap = ability(tendi, ApiType.Untap);
        AssertJUnit.assertNotNull("D'Vana Tendi's tap ability", untap);
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();

        tendi.setSickness(true);
        AssertJUnit.assertFalse("summoning sickness makes the tap ability unpayable", brain.isStillCastable(untap));
        AssertJUnit.assertFalse("payment must not succeed while the source is ability-sick",
                new forge.game.cost.CostTap().payAsDecided(ai, null, untap, false));

        tendi.setSickness(false);
        AssertJUnit.assertTrue("the ability can be paid while D'Vana is untapped", brain.isStillCastable(untap));
        tendi.setTapped(true);
        AssertJUnit.assertFalse("the final FlatMC check must reject the stale ability once its source is tapped",
                brain.isStillCastable(untap));
        AssertJUnit.assertFalse("payment must not succeed when the source is already tapped",
                new forge.game.cost.CostTap().payAsDecided(ai, null, untap, false));
    }
}

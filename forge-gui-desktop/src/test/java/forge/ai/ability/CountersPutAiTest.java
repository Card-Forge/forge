package forge.ai.ability;

import forge.ai.AiCostDecision;
import forge.ai.AiPlayDecision;
import forge.ai.LobbyPlayerAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.cost.CostSacrifice;
import forge.game.cost.PaymentDecision;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class CountersPutAiTest extends SimulationTest {

    @Test
    public void testPowerUp_savesAttackerFromLethalCombat_aiActivates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Brave Brawler: 2/1, Power-Up {4}{W}: put two +1/+1 counters on this creature (becomes 4/3)
        Card brawler = addCard("Brave Brawler", ai);
        addCards("Plains", 5, ai);

        // Giant Spider: 2/4 - unpumped Brawler dies without trading; pumped Brawler (4/3) survives and kills it
        Card spider = addCard("Giant Spider", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(ai);
        combat.addAttacker(brawler, opponent);
        combat.addBlocker(brawler, spider);
        game.getPhaseHandler().setCombat(combat);

        SpellAbility powerUpSA = findSAWithPrefix(brawler, "Power-Up");
        assertNotNull(powerUpSA);
        powerUpSA.setActivatingPlayer(ai);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, powerUpSA).decision();
        assertEquals("AI should activate Power-Up to save its attacker and win combat",
                AiPlayDecision.WillPlay, decision);
    }

    private SpellAbility counterAbility(final Card source, final Player ai) {
        for (final SpellAbility ability : source.getSpellAbilities()) {
            if (ability.isActivatedAbility() && ability.getApi() == ApiType.PutCounter) {
                ability.setActivatingPlayer(ai);
                return ability;
            }
        }
        throw new AssertionError("missing counter ability on " + source);
    }

    @Test
    public void routineAristocratGrowthIsPrecombatAndOncePerTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        final Card feeder = addCard("Carrion Feeder", ai);
        addTokens("w_0_1_goat", 2, ai);
        final SpellAbility growth = counterAbility(feeder, ai);
        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ApiType.PutCounter);

        assertFalse("routine growth waits for a precombat opportunity",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        assertTrue("expendable fodder allows one precombat activation",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        feeder.addAbilityActivated(growth);
        assertFalse("routine growth stops after the first activation",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
    }

    @Test
    public void blockedAristocratRequiresEnoughFodderAndStopsWhenSafe() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        final Card feeder = addCard("Carrion Feeder", ai);
        final Card blocker = addCard("Hill Giant", opponent);
        addToken("w_0_1_goat", ai);
        final SpellAbility growth = counterAbility(feeder, ai);
        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        final Combat combat = new Combat(ai);
        combat.addAttacker(feeder, opponent);
        combat.addBlocker(feeder, blocker);
        game.getPhaseHandler().setCombat(combat);

        assertFalse("one counter cannot save the attacker or kill the blocker",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        addTokens("w_0_1_goat", 2, ai);
        assertTrue("three sacrifices can make the attacker survive and kill",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        feeder.setCounters(CounterEnumType.P1P1, 3);
        assertFalse("no more sacrifices are needed once the combat goal is met",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
    }

    @Test
    public void threatenedFodderIsChosenOnTheOpponentsTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opponent);
        // Use the shared hint without a card-specific sacrifice preference.
        final Card outlet = addCard("Bloodflow Connoisseur", ai);
        final SpellAbility growth = counterAbility(outlet, ai);
        growth.getMapParams().put("AILogic", "AristocratCounters");
        final Card victim = addCard("Hill Giant", ai);
        final Card otherFodder = addCard("Ministrant of Obligation", ai);
        final Card swords = addCardToZone("Swords to Plowshares", opponent, ZoneType.Hand);
        final SpellAbility exile = swords.getFirstSpellAbility();
        exile.setActivatingPlayer(opponent);
        exile.getTargets().add(victim);
        game.getStack().addAndUnfreeze(exile);

        assertTrue("a response to removal is not routine growth",
                SpellApiToAi.Converter.get(ApiType.PutCounter).canPlayWithSubs(ai, growth).willingToPlay());
        final CostSacrifice cost = (CostSacrifice) growth.getPayCosts().getCostParts().get(0);
        final PaymentDecision payment = new AiCostDecision(ai, growth, false).visit(cost);
        assertNotNull(payment);
        assertEquals(1, payment.cards.size());
        assertTrue("the doomed creature is sacrificed before marked fodder", payment.cards.contains(victim));
        assertFalse("the outlet must survive", payment.cards.contains(outlet));
        assertFalse("safe fodder remains available", payment.cards.contains(otherFodder));
    }
}

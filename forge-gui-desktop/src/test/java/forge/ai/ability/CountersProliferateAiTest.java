package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


public class CountersProliferateAiTest extends AITest {

    @Test
    public void testProliferate_noCounters_aiDoesNotActivate() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        // Add Karn's Bastion (4 mana to proliferate) and enough lands
        Card karnsBastion = addCard("Karn's Bastion", ai);
        addCards("Wastes", 4, ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility proliferateSA = findSAWithPrefix(karnsBastion, "{4}, {T}: Proliferate");
        assertNotNull(proliferateSA);

        // AI should not want to activate with no counters on the battlefield
        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.Proliferate);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, proliferateSA).decision();
        assertEquals("AI should not activate proliferate with no counters",
                AiPlayDecision.CantPlayAi, decision);
    }

    @Test
    public void testProliferate_valueMeetsCost_aiActivates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // Add Karn's Bastion and enough lands
        Card karnsBastion = addCard("Karn's Bastion", ai);
        addCards("Wastes", 4, ai);

        // Add a planeswalker (value 3) and a creature with +1/+1 counter (value 1) = 4 total
        Card planeswalker = addCard("Karn, Scion of Urza", ai);
        planeswalker.setCounters(CounterEnumType.LOYALTY, 5);
        Card creature = addCard("Runeclaw Bear", ai);
        creature.setCounters(CounterEnumType.P1P1, 1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility proliferateSA = findSAWithPrefix(karnsBastion, "{4}, {T}: Proliferate");
        assertNotNull(proliferateSA);

        // AI should want to activate since value (4) meets cost (4)
        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.Proliferate);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, proliferateSA).decision();
        assertEquals("AI should activate proliferate when value meets cost",
                AiPlayDecision.WillPlay, decision);
    }

    @Test
    public void testProliferate_valueBelowCost_aiDoesNotActivate() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // Add Karn's Bastion and enough lands
        Card karnsBastion = addCard("Karn's Bastion", ai);
        addCards("Wastes", 4, ai);

        // Add only a creature with +1/+1 counter (value 1) < cost (4)
        Card creature = addCard("Runeclaw Bear", ai);
        creature.setCounters(CounterEnumType.P1P1, 1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility proliferateSA = findSAWithPrefix(karnsBastion, "{4}, {T}: Proliferate");
        assertNotNull(proliferateSA);

        // AI should not want to activate since value (1) < cost (4)
        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.Proliferate);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, proliferateSA).decision();
        assertEquals("AI should not activate proliferate when value below cost",
                AiPlayDecision.CantPlayAi, decision);
    }

    @Test
    public void testProliferate_lethalPoison_aiActivates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        // Add Karn's Bastion and enough lands
        Card karnsBastion = addCard("Karn's Bastion", ai);
        addCards("Wastes", 4, ai);

        // Opponent has 9 poison counters - proliferating would win the game
        opponent.setCounters(CounterEnumType.POISON, 9);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility proliferateSA = findSAWithPrefix(karnsBastion, "{4}, {T}: Proliferate");
        assertNotNull(proliferateSA);

        // AI should always activate when it can win by poison
        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.Proliferate);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, proliferateSA).decision();
        assertEquals("AI should activate proliferate for lethal poison",
                AiPlayDecision.WillPlay, decision);
    }
}

package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiAbilityDecision;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * An X-cost "remove up to X counters" - Hex Parasite - used to skip every target rule but
 * breaking its own Dark Depths and finishing a planeswalker, because those rules were gated
 * on the amount not being an X cost.
 */
public class CountersRemoveXAiTest extends AITest {

    private SpellAbility removeCounterAbility(Card parasite, Player ai) {
        for (SpellAbility sa : parasite.getSpellAbilities()) {
            if (sa.getApi() == ApiType.RemoveCounter) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("Hex Parasite has no RemoveCounter ability");
    }

    @Test
    public void stripsCountersFromAnOpposingCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card parasite = addCard("Hex Parasite", ai);
        addCards("Swamp", 6, ai);
        Card bear = addCard("Centaur Courser", opp);
        bear.setCounters(CounterEnumType.P1P1, 3);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility remove = removeCounterAbility(parasite, ai);
        AiAbilityDecision decision = SpellApiToAi.Converter.get(remove).canPlayWithSubs(ai, remove);

        assertTrue("should strip the counters", decision.willingToPlay());
        assertEquals(bear, remove.getTargets().getFirstTargetedCard());
        // pays for the three counters actually there, not the five it could afford
        assertEquals(3, (long) remove.getXManaCostPaid());
    }

    @Test
    public void clearsItsOwnPersistCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card parasite = addCard("Hex Parasite", ai);
        addCards("Swamp", 6, ai);
        Card finks = addCard("Kitchen Finks", ai);
        finks.setCounters(CounterEnumType.M1M1, 1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility remove = removeCounterAbility(parasite, ai);
        AiAbilityDecision decision = SpellApiToAi.Converter.get(remove).canPlayWithSubs(ai, remove);

        assertTrue("should clear its own -1/-1 counter", decision.willingToPlay());
        assertEquals(finks, remove.getTargets().getFirstTargetedCard());
        assertEquals(1, (long) remove.getXManaCostPaid());
    }

    @Test
    public void declinesWithNothingWorthRemoving() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card parasite = addCard("Hex Parasite", ai);
        addCards("Swamp", 6, ai);
        addCard("Centaur Courser", opp); // no counters anywhere

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility remove = removeCounterAbility(parasite, ai);
        AiAbilityDecision decision = SpellApiToAi.Converter.get(remove).canPlayWithSubs(ai, remove);

        assertTrue("nothing to remove, should hold", !decision.willingToPlay());
    }
}

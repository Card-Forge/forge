package forge.ai.controller;

import forge.ai.AiCardMemory;
import forge.ai.AiController;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * reserveManaSources holds back the sources needed for a spell the AI wants to cast later.
 * getManaSourcesToPayCost hands back the sources of a payment it has already worked out, or null
 * when the cost can't be paid, so the answer only depends on whether such a payment exists.
 */
public class ReserveManaSourcesTest extends SimulationTest {

    private Game gameWith(String[] battlefield) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (String name : battlefield) {
            addCard(name, ai);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    private SpellAbility spellInHand(Game game, String name) {
        Player ai = game.getPlayers().get(1);
        Card card = addCardToZone(name, ai, ZoneType.Hand);
        SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private static AiController aiOf(Game game) {
        return ((PlayerControllerAi) game.getPlayers().get(1).getController()).getAi();
    }

    /**
     * Sol Ring makes two mana off one card, so three cards can cover a mana value four spell.
     * Reservation has to follow whether the payment exists, not how many cards it uses.
     */
    @Test
    public void reservesWhenOneSourceMakesSeveralMana() {
        Game game = gameWith(new String[] { "Sol Ring", "Mountain", "Mountain" });
        SpellAbility sa = spellInHand(game, "Hill Giant");

        AssertJUnit.assertTrue("Sol Ring plus two lands pays for a mana value four spell",
                aiOf(game).reserveManaSources(sa));
        AssertJUnit.assertFalse("the sources it will use should have been remembered",
                AiCardMemory.isMemorySetEmpty(game.getPlayers().get(1),
                        AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2));
    }

    /** Nothing to reserve when the board can't pay for the spell at all. */
    @Test
    public void doesNotReserveWhatItCannotPay() {
        Game game = gameWith(new String[] { "Mountain", "Mountain" });
        SpellAbility sa = spellInHand(game, "Hill Giant");

        AssertJUnit.assertFalse("two lands cannot pay for a mana value four spell",
                aiOf(game).reserveManaSources(sa));
        AssertJUnit.assertTrue("nothing should have been remembered",
                AiCardMemory.isMemorySetEmpty(game.getPlayers().get(1),
                        AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2));
    }

    /**
     * When chaining two spells the second can't be promised sources the first already needs. If
     * removing them leaves nothing, there is nothing to reserve.
     */
    @Test
    public void doesNotReserveSourcesTheChainedSpellNeeds() {
        Game game = gameWith(new String[] { "Mountain", "Mountain", "Mountain", "Mountain" });
        SpellAbility first = spellInHand(game, "Hill Giant");
        SpellAbility second = spellInHand(game, "Hill Giant");

        AssertJUnit.assertFalse("all four lands are spoken for by the first spell",
                aiOf(game).reserveManaSourcesForNextSpell(second, first));
    }
}

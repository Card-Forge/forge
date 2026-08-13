package forge.ai.controller;

import forge.ai.ComputerUtilMana;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Upstream #7621: the human autopay button decides "can pay?" via
 * {@link ComputerUtilMana#getManaSourcesToPayCost}, which must work with
 * mana-producing creatures even when no untapped lands exist.
 */
public class AutopayManaDorkTest extends SimulationTest {

    private Game gameWith(String[] battlefield, boolean dorkWithoutSickness) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (String name : battlefield) {
            Card c = addCard(name, ai);
            if (dorkWithoutSickness && c.isCreature()) {
                c.setSickness(false);
            }
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

    /** Sanity check: the autopay path can find a payment for a single green mana at all. */
    @Test
    public void autopayWithLandAndDork() {
        Game game = gameWith(new String[] { "Forest", "Noble Hierarch" }, false);
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = spellInHand(game, "Tamiyo's Safekeeping");

        AssertJUnit.assertNotNull("lands and/or a dork can pay for Tamiyo's Safekeeping",
                ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(sa.getHostCard().getManaCost()), sa, ai, false));
    }

    /** Exact upstream #7621 scenario: no lands, one untapped dork with no summoning sickness. */
    @Test
    public void autopayUsesManaDorkWhenNoUntappedLandsExist() {
        Game game = gameWith(new String[] { "Noble Hierarch" }, true);
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = spellInHand(game, "Tamiyo's Safekeeping");

        AssertJUnit.assertNotNull("autopay should tap the untapped mana-dork with no lands available",
                ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(sa.getHostCard().getManaCost()), sa, ai, false));
    }

    /** CR 605.3b: summoning sickness does not stop mana abilities, so a sick dork is also a valid source. */
    @Test
    public void autopayUsesSickManaDork() {
        Game game = gameWith(new String[] { "Noble Hierarch" }, false);
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = spellInHand(game, "Tamiyo's Safekeeping");

        AssertJUnit.assertNotNull("mana abilities ignore summoning sickness, so a sick dork can still pay",
                ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(sa.getHostCard().getManaCost()), sa, ai, false));
    }
}
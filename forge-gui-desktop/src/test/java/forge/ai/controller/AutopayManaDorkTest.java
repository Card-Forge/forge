package forge.ai.controller;

import forge.ai.ComputerUtilMana;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Regression guard for upstream #7621: the human autopay button decides "can
 * pay?" via {@link ComputerUtilMana#getManaSourcesToPayCost}, which must work
 * with mana-producing creatures even when no untapped lands exist. A creature
 * with summoning sickness (CR 302.6) must NOT be tapped, so the sick case is
 * asserted to fail while the un-sick dork case must pay.
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

        CardCollection sources = ComputerUtilMana.getManaSourcesToPayCost(
                new ManaCostBeingPaid(sa.getHostCard().getManaCost()), sa, ai, false);
        Card dork = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                CardPredicates.nameEquals("Noble Hierarch")).get(0);
        AssertJUnit.assertTrue("autopay should tap the untapped mana-dork with no lands available",
                sources.contains(dork));
    }

    /** CR 302.6: a creature with summoning sickness cannot use its tap abilities, so a sick dork is not payable. */
    @Test
    public void autopayCannotUseSickManaDork() {
        Game game = gameWith(new String[] { "Noble Hierarch" }, false);
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = spellInHand(game, "Tamiyo's Safekeeping");

        AssertJUnit.assertNull("summoning sickness stops the dork from being tapped for mana (CR 302.6)",
                ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(sa.getHostCard().getManaCost()), sa, ai, false));
    }
}
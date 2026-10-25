package forge.game.spellability;

import java.util.List;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.CostAdjustment;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for prepared spells (Issue #10624), ensuring static abilities affecting
 * spell casting (flash enablement, cost reduction) can evaluate the prepared spell
 * in exile, while spells targeting cards in exile (e.g. Pull from Eternity) cannot.
 */
public class PreparedSpellTest extends AITest {

    private Game game;
    private Player me;
    private Player opp;

    private void newGame() {
        game = initAndCreateGame();
        me = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
    }

    private void settle() {
        game.getAction().checkStateEffects(true);
    }

    private Card prepareCreature(Card creature) {
        String svar = creature.hasSVar("DBPrepare") ? "DBPrepare" : "TrigPrepare";
        SpellAbility prepareSA = AbilityFactory.getAbility(creature.getSVar(svar), creature);
        prepareSA.setActivatingPlayer(me);
        AbilityUtils.resolve(prepareSA);
        settle();
        Card prepared = creature.getPreparedSpell();
        assertNotNull(prepared, creature.getName() + " should have a prepared spell");
        return prepared;
    }

    @Test
    public void preparedSpellGainsFlashFromStaticAbility() {
        newGame();
        Card eater = addCard("Adventurous Eater", me);
        Card prepared = prepareCreature(eater);
        SpellAbility sa = prepared.getFirstSpellAbility();
        sa.setActivatingPlayer(me);

        // Eater's prepared spell "Have a Bite" is a Sorcery.
        // During an opponent's turn, it cannot be cast without flash.
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        assertFalse(sa.withFlash(prepared, me),
                "Prepared sorcery cannot be cast during opponent turn without flash");

        // Add Leyline of Anticipation
        Card leyline = addCard("Leyline of Anticipation", me);
        settle();
        assertTrue(sa.withFlash(prepared, me),
                "Prepared sorcery can be cast with flash granted by Leyline of Anticipation");

        // Remove Leyline of Anticipation
        me.getZone(ZoneType.Battlefield).remove(leyline);
        settle();
        assertFalse(sa.withFlash(prepared, me),
                "Prepared sorcery cannot be cast with flash once Leyline is removed");

        // Add High Fae Trickster
        addCard("High Fae Trickster", me);
        settle();
        assertTrue(sa.withFlash(prepared, me),
                "Prepared sorcery can be cast with flash granted by High Fae Trickster");
    }

    @Test
    public void costReductionAppliesToPreparedSpell() {
        newGame();
        Card researchers = addCard("Joined Researchers", me);
        Card prepared = prepareCreature(researchers);
        SpellAbility sa = prepared.getFirstSpellAbility();
        sa.setActivatingPlayer(me);

        // "Secret Rendezvous" has printed mana cost {1}{W}{W}
        ManaCostBeingPaid costBefore = new ManaCostBeingPaid(prepared.getManaCost());
        CostAdjustment.adjust(costBefore, sa, me, null, false, false);
        assertEquals(costBefore.toString(), "{1}{W}{W}");

        // Add Goblin Electromancer (reduces instant/sorcery spells by {1})
        addCard("Goblin Electromancer", me);
        settle();

        ManaCostBeingPaid costAfter = new ManaCostBeingPaid(prepared.getManaCost());
        CostAdjustment.adjust(costAfter, sa, me, null, false, false);
        assertEquals(costAfter.toString(), "{W}{W}");
    }

    @Test
    public void pullFromEternityCannotTargetPreparedSpellInExile() {
        newGame();
        Card eater = addCard("Adventurous Eater", me);
        Card prepared = prepareCreature(eater);

        Card pull = addCardToZone("Pull from Eternity", opp, ZoneType.Hand);
        SpellAbility pullSA = pull.getFirstSpellAbility();

        assertFalse(pullSA.canTarget(prepared),
                "Pull from Eternity must not be able to target a prepared spell in exile via canTarget()");
        assertFalse(prepared.canBeTargetedBy(pullSA),
                "Prepared spell in exile must not be targetable via canBeTargetedBy()");
    }

    @Test
    public void alternativeCostAppliesToPreparedSpell() {
        newGame();
        Card researchers = addCard("Joined Researchers", me);
        Card prepared = prepareCreature(researchers);
        SpellAbility sa = prepared.getFirstSpellAbility();
        sa.setActivatingPlayer(me);

        // Add Fist of Suns ({W}{U}{B}{R}{G} alternative cost for spells)
        addCard("Fist of Suns", me);
        settle();

        List<SpellAbility> altCosts = forge.game.staticability.StaticAbilityAlternativeCost.alternativeCosts(sa, prepared, me);
        assertFalse(altCosts.isEmpty(), "Prepared spell should receive alternative costs from Fist of Suns");
        assertEquals(altCosts.get(0).getPayCosts().getTotalMana().toString(), "{W}{U}{B}{R}{G}");
    }
}

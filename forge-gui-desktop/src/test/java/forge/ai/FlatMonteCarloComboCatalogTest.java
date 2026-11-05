package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.AntiCombo;
import forge.ai.simulation.ComboCatalog;
import forge.ai.simulation.ComboGuard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The combo catalog (ComboCatalog): every combo is recognised from its cards, an opponent's assembled combo is a threat
 * and its pieces are what the AI aims at, and the AI's own assembled combo is worth points. A failure names the combo
 * (its number in the list) and prints what the AI sees; adjust the entry in ComboCatalog or the constants there.
 */
public class FlatMonteCarloComboCatalogTest extends AITest {
    private Game game;
    private Player opp;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
    }

    /** Every entry is found in a deck list made of its own cards, and an entry missing a card is not. */
    @Test
    public void everyComboIsRecognisedFromItsCardsAndNotWithoutOne() {
        final List<String> problems = new ArrayList<>();
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            final List<String> names = new ArrayList<>();
            for (final String slot : e.slotSpecs()) {
                names.add(slot.split(" / ")[0]);
            }
            if (!ComboCatalog.inNames(names).contains(e)) {
                problems.add(e + " is not recognised from " + names);
            }
            if (e.slotCount() >= 2) {
                final List<String> partial = new ArrayList<>(names);
                partial.remove(partial.size() - 1);
                if (ComboCatalog.inNames(partial).contains(e)) {
                    problems.add(e + " is recognised without its last card, from " + partial);
                }
            }
        }
        AssertJUnit.assertTrue("catalog problems: " + problems, problems.isEmpty());
        AssertJUnit.assertTrue("53 numbered combos in the list", ComboCatalog.entries().size() >= 53);
    }

    /** An opponent with every card of a combo on the battlefield is a threat; one card of it is not that. */
    @Test
    public void anAssembledOpponentComboIsAThreatAndHalfOfItIsNot() {
        setUp();
        addCard("Painter's Servant", opp);
        final int half = AntiCombo.threat(ai);
        addCard("Grindstone", opp);
        final int whole = AntiCombo.threat(ai);
        AssertJUnit.assertTrue("both halves must count for more than one: " + half + " then " + whole + " " + AntiCombo.describe(ai),
                whole >= half + ComboCatalog.visibleThreat(ai));
        AssertJUnit.assertEquals("the combo is in view: " + AntiCombo.describe(ai), 1, ComboCatalog.visibleCombos(ai).size());
    }

    /** A lock piece in view is a threat too (the lock list), at a lower weight than a win. */
    @Test
    public void aLockInViewIsAThreatSmallerThanAWin() {
        setUp();
        addCard("Counterbalance", opp);
        addCard("Sensei's Divining Top", opp);
        final int lock = ComboCatalog.visibleThreat(ai);
        AssertJUnit.assertEquals("lock weight: " + AntiCombo.describe(ai), ComboCatalog.VISIBLE_LOCK, lock);
    }

    /** The card that completes a combo whose other cards are in view is ranked above a lone piece. */
    @Test
    public void theCompletingCardIsRankedAboveALonePiece() {
        setUp();
        final Card servant = addCard("Painter's Servant", opp);
        final Card lone = addCard("Worship", opp);
        final int loneRank = AntiCombo.rank(ai, lone);
        final Card stone = addCard("Grindstone", opp);
        AssertJUnit.assertTrue("a piece has a rank: " + loneRank, loneRank > 0);
        AssertJUnit.assertTrue("a half built combo ranks its cards higher: servant " + AntiCombo.rank(ai, servant) + " vs lone "
                + loneRank + " " + AntiCombo.describe(ai), AntiCombo.rank(ai, servant) > loneRank);
        AssertJUnit.assertTrue(AntiCombo.rank(ai, stone) > loneRank);
        AssertJUnit.assertEquals("no rank for the AI's own card", 0, AntiCombo.rank(opp, stone));
    }

    /** A hand with a whole combo is worth more than one with half of it, and the other half missing one card is worth a little. */
    @Test
    public void theAisOwnAssembledComboIsWorthPoints() {
        setUp();
        AssertJUnit.assertEquals(0, ComboCatalog.term(ai));
        addCardToZone("Time Vault", ai, ZoneType.Hand);
        final int one = ComboCatalog.term(ai);
        addCardToZone("Manifold Key", ai, ZoneType.Hand);
        final int both = ComboCatalog.term(ai);
        AssertJUnit.assertTrue("both pieces are worth more than one: " + one + " then " + both, both > one);
        AssertJUnit.assertTrue("an assembled win is worth the full points: " + both, both >= ComboCatalog.ASSEMBLED_WIN);
    }

    /** A manland that is already a creature, or cannot attack, is not animated again (Mutavault). */
    @Test
    public void aManlandIsNotAnimatedWithoutAReason() {
        setUp();
        final Card vault = addCard("Mutavault", ai);
        vault.setSickness(true);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        forge.game.spellability.SpellAbility animate = null;
        for (final forge.game.spellability.SpellAbility sa : vault.getSpellAbilities()) {
            if (sa.getApi() == forge.game.ability.ApiType.Animate) {
                animate = sa;
            }
        }
        AssertJUnit.assertNotNull(animate);
        animate.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("a summoning sick land cannot attack", "manland-wasted", ComboGuard.problem(ai, animate, null, null));
        vault.setSickness(false);
        AssertJUnit.assertNull("in the main phase it can attack", ComboGuard.problem(ai, animate, null, null));
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals("after combat there is nothing left to do with it", "manland-wasted",
                ComboGuard.problem(ai, animate, null, null));
    }

    /** A combo with a spell in the hand earns nothing for holding it: casting Doomsday must never cost points. */
    @Test
    public void aSpellOfTheComboInTheHandIsNotWorthHolding() {
        setUp();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final int oracleAlone = ComboCatalog.term(ai); // the Oracle is a permanent: it may count for the Consultation and Pact combos
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("Doomsday in the hand adds nothing, so casting it costs nothing", oracleAlone,
                ComboCatalog.term(ai));
    }

    /** Yawgmoth's Will is cast for a Time Walk in the graveyard when there is mana for both. */
    @Test
    public void yawgmothsWillIsCastForATimeWalkInTheGraveyard() {
        setUp();
        for (int i = 0; i < 5; i++) {
            addCard("Swamp", ai);
        }
        addCardToZone("Time Walk", ai, ZoneType.Graveyard);
        final Card will = addCardToZone("Yawgmoth's Will", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        final List<forge.game.spellability.SpellAbility> candidates = new ArrayList<>(will.getSpellAbilities());
        AssertJUnit.assertNotNull("the Will is cast with 5 mana and a Time Walk to replay",
                forge.ai.simulation.WillReplay.sureCast(ai, candidates));
    }
}

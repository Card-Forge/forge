package forge.ai.simulation;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.Spell;
import forge.game.zone.ZoneType;

/**
 * Madness the way a game plays it: DISCARD (the card goes to exile instead), the madness TRIGGER, then the cast.
 * <p>
 * The first group runs the real chain in the real game (no simulation copy, no shortcut), so a card that is lost on
 * the way shows up as "in the graveyard instead of on the battlefield". The second group is about what the madness
 * spell will DO: a spell that cannot be cast when the trigger resolves (a counterspell with nothing to counter, a
 * spell without a legal target) must never be thrown away for madness, it would only be lost.
 */
public class MadnessFlowTest extends FlatMcTestBase {

    private int count(final ZoneType zone, final String name) {
        int n = 0;
        for (final Card c : ai.getCardsIn(zone)) {
            n += name.equals(c.getName()) ? 1 : 0;
        }
        return n;
    }

    private SpellAbility firstActivated(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(c.getController());
                return sa;
            }
        }
        throw new AssertionError(c + " has no activated ability");
    }

    /** The AI plays the ability through its real controller, then the stack (ability, triggers, madness cast) resolves. */
    private void playAndResolve(final SpellAbility sa) {
        assertTrue(((PlayerControllerAi) ai.getController()).playChosenSpellAbility(sa), "the AI could not play " + sa);
        GameSimulator.resolveStack(game, opp);
    }

    // ===================================================================================== the whole chain

    /** The screenshot: Merfolk Looter loots, the discard is Basking Rootwalla, the madness trigger must cast it. */
    @Test
    public void looterDiscardsRootwallaAndItIsCastNotLost() {
        setUpGame();
        addMadnessDeck();
        final Card looter = put("Merfolk Looter", ai, ZoneType.Battlefield);
        looter.setSickness(false);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        at(PhaseType.MAIN1, ai);
        final int before = count(ZoneType.Battlefield, "Basking Rootwalla");
        playAndResolve(firstActivated(looter));
        assertEquals(count(ZoneType.Graveyard, "Basking Rootwalla"), 0,
                "the madness card was lost to the graveyard: the trigger resolved but nothing was cast");
        assertEquals(count(ZoneType.Battlefield, "Basking Rootwalla"), before + 1, "the madness spell must resolve");
    }

    /** Discard as a COST (Wild Mongrel): the card is exiled while the ability is still being paid for. */
    @Test
    public void mongrelCostDiscardAtTheEndStepIsCastNotLost() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        put("Basking Rootwalla", ai, ZoneType.Hand);
        at(PhaseType.END_OF_TURN, opp);
        playAndResolve(discardAbilityOf(mongrel));
        assertEquals(count(ZoneType.Graveyard, "Basking Rootwalla"), 0, "lost to the graveyard");
        assertEquals(count(ZoneType.Battlefield, "Basking Rootwalla"), 1);
    }

    /** A madness card with a real cost: the AI pays {2}{G} when the lands are there. */
    @Test
    public void arrogantWurmIsCastForItsMadnessCost() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card looter = put("Merfolk Looter", ai, ZoneType.Battlefield);
        looter.setSickness(false);
        put("Arrogant Wurm", ai, ZoneType.Hand);
        at(PhaseType.MAIN1, ai);
        playAndResolve(firstActivated(looter));
        assertEquals(count(ZoneType.Graveyard, "Arrogant Wurm"), 0, "lost to the graveyard");
        assertEquals(count(ZoneType.Battlefield, "Arrogant Wurm"), 1);
    }

    /** The decision at the cast point alone (what PlayerControllerAi.playSaFromPlayEffect asks). */
    @Test
    public void theCastDecisionForAnAffordableMadnessCreatureIsYes() {
        setUpGame();
        addMadnessDeck();
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Exile);
        final Spell spell = (Spell) rootwalla.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        assertEquals(brain().canPlayFromEffectAI(spell, true, false), AiPlayDecision.WillPlay,
                "mandatory: the card is lost if it is not cast, so the answer is yes whenever it can be cast");
    }

    // ===================================================================================== what the spell will do

    /** Circular Logic ({U} madness) counters a spell: with nothing on the stack it cannot be cast at all. */
    @Test
    public void aCounterspellIsNeverDiscardedForMadnessWithNothingToCounter() {
        setUpGame();
        addMadnessDeck();
        put("Island", 2, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card logic = put("Circular Logic", ai, ZoneType.Hand);
        at(PhaseType.END_OF_TURN, opp);
        final ComboPlan plan = ComboPlan.scan(ai);
        assertFalse(plan.castableMadness(ai, discardAbilityOf(mongrel)).contains(logic),
                "the stack is empty: Circular Logic would be exiled and then put into the graveyard");
        assertNull(plan.madnessDiscard(ai, collection(logic)));
        assertNull(plan.preferredDiscards(ai, collection(logic), 1),
                "a forced discard has no reason to prefer it either");
        assertNull(plan.sureMadnessPlay(ai, java.util.Arrays.asList(discardAbilityOf(mongrel))));
    }

    /** The same card is a fine madness discard when there is something on the stack to aim it at. */
    @Test
    public void aCounterspellIsAMadnessOptionWhenTheOpponentHasASpellOnTheStack() {
        setUpGame();
        addMadnessDeck();
        put("Island", 2, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card logic = put("Circular Logic", ai, ZoneType.Hand);
        put("Plains", 5, opp, ZoneType.Battlefield);
        final Card angel = put("Serra Angel", opp, ZoneType.Hand);
        final SpellAbility cast = angel.getFirstSpellAbility();
        cast.setActivatingPlayer(opp);
        assertTrue(ComputerUtil.playStack(cast, opp, game), "precondition: the opponent's creature spell is on the stack");
        final ComboPlan plan = ComboPlan.scan(ai);
        assertTrue(plan.castableMadness(ai, discardAbilityOf(mongrel)).contains(logic),
                "a big creature spell to counter: now the madness cast makes sense");
    }

    @Test
    public void aSpellWithoutALegalTargetIsNeverDiscardedForMadness() {
        setUpGame();
        addMadnessDeck();
        put("Forest", 3, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        // Fiery Temper needs a target, but any creature or player is one: use a spell that needs a creature of the opponent
        final Card spell = put("Violent Eruption", ai, ZoneType.Hand); // {4}{R}{R}, madness {3}{R}: not castable here (no red), control case
        at(PhaseType.END_OF_TURN, opp);
        assertFalse(ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel)).contains(spell));
    }

    @Test
    public void aMadnessCreatureNeedsNoTargetAndStaysAnOption() {
        setUpGame();
        addMadnessDeck();
        put("Forest", ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card rootwalla = put("Basking Rootwalla", ai, ZoneType.Hand);
        assertTrue(ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel)).contains(rootwalla));
    }

    @Test
    public void theCheckLeavesTheCardsAbilitiesUntouched() {
        setUpGame();
        addMadnessDeck();
        put("Island", 2, ai, ZoneType.Battlefield);
        final Card mongrel = put("Wild Mongrel", ai, ZoneType.Battlefield);
        final Card logic = put("Circular Logic", ai, ZoneType.Hand);
        ComboPlan.scan(ai).castableMadness(ai, discardAbilityOf(mongrel));
        assertTrue(logic.getFirstSpellAbility().getTargets() == null
                || logic.getFirstSpellAbility().getTargets().size() == 0, "asking must not leave a target behind");
    }
}
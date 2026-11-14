package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tutors (Vampiric Tutor, Demonic Tutor, Intuition...) are plays for the rollouts to weigh when the deck plan knows a
 * card it is missing: they were filtered out (AI:RemoveDeck:All) or worth nothing to the evaluator.
 */
public class TutorPlanTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility spellOf(final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void tutorsAreRecognisedAndOtherSpellsAreNot() {
        AssertJUnit.assertTrue(TutorPlan.isLibraryTutor(spellOf(put("Vampiric Tutor", ai, ZoneType.Hand))));
        AssertJUnit.assertTrue(TutorPlan.isLibraryTutor(spellOf(put("Demonic Tutor", ai, ZoneType.Hand))));
        AssertJUnit.assertTrue(TutorPlan.isLibraryTutor(spellOf(put("Intuition", ai, ZoneType.Hand))));
        AssertJUnit.assertFalse(TutorPlan.isLibraryTutor(spellOf(put("Rampant Growth", ai, ZoneType.Hand)))); // a land onto the battlefield
        AssertJUnit.assertFalse(TutorPlan.isLibraryTutor(spellOf(put("Giant Growth", ai, ZoneType.Hand))));
    }

    @Test
    public void aTutorHasAPlanTargetWhenTheDeckIsMissingAComboPiece() {
        addMadnessDeck();
        final SpellAbility vampiric = spellOf(put("Vampiric Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(TutorPlan.hasPlanTarget(ai, vampiric));
        AssertJUnit.assertEquals(TutorPlan.PLAN_PICK_ON_TOP_BONUS * 100 / 100,
                TutorPlan.bonus(vampiric, new FlatMonteCarloPicker.Config()) * 100 / Math.max(1, new FlatMonteCarloPicker.Config().strategicWeightPercent));
        final SpellAbility demonic = spellOf(put("Demonic Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(TutorPlan.bonus(demonic, new FlatMonteCarloPicker.Config())
                > TutorPlan.bonus(vampiric, new FlatMonteCarloPicker.Config())); // into the hand beats on top
    }

    @Test
    public void aTutorFetchesThePieceThatCompletesACataloguedCombo() {
        addPlainDeck();
        put("Devoted Druid", ai, ZoneType.Battlefield); // Devoted Druid + Quillspike is a line of ai-combos.txt
        final Card quillspike = put("Quillspike", ai, ZoneType.Library);
        final SpellAbility demonic = spellOf(put("Demonic Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(TutorPlan.hasPlanTarget(ai, demonic));
        AssertJUnit.assertEquals(quillspike, TutorPlan.planPicks(ai, demonic).getFirst());
    }

    @Test
    public void aLonePieceOfACataloguedComboIsNotFetched() {
        addPlainDeck();
        put("Quillspike", ai, ZoneType.Library); // nothing of the combo is at hand or in play
        final SpellAbility demonic = spellOf(put("Demonic Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(ComboCatalog.missingPieces(ai, ai.getCardsIn(ZoneType.Library)).isEmpty());
        AssertJUnit.assertFalse(TutorPlan.hasPlanTarget(ai, demonic));
    }

    @Test
    public void theWinningComboIsFetchedBeforeTheLock() {
        addPlainDeck();
        put("Heartbeat of Spring", ai, ZoneType.Hand);
        put("Counterbalance", ai, ZoneType.Hand);
        final Card harvest = put("Early Harvest", ai, ZoneType.Library);
        put("Sensei's Divining Top", ai, ZoneType.Library);
        AssertJUnit.assertEquals(harvest, ComboCatalog.missingPieces(ai, ai.getCardsIn(ZoneType.Library)).get(0));
    }

    @Test
    public void aDeckWithoutAPlanOnlyGetsTheSmallBonusForUnusedMana() {
        addPlainDeck();
        final SpellAbility vampiric = spellOf(put("Vampiric Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertFalse(TutorPlan.hasPlanTarget(ai, vampiric));
        AssertJUnit.assertTrue(TutorPlan.hasUse(ai, vampiric));
        final FlatMonteCarloPicker.Config cfg = new FlatMonteCarloPicker.Config();
        AssertJUnit.assertEquals(TutorPlan.IDLE_BONUS * cfg.strategicWeightPercent / 100, TutorPlan.bonus(vampiric, cfg));
    }

    @Test
    public void anEmptyLibraryGivesNoBonus() {
        final SpellAbility vampiric = spellOf(put("Vampiric Tutor", ai, ZoneType.Hand));
        AssertJUnit.assertFalse(TutorPlan.hasUse(ai, vampiric));
        AssertJUnit.assertEquals(0, TutorPlan.bonus(vampiric, new FlatMonteCarloPicker.Config()));
    }
}

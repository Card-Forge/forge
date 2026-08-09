package forge.game.decision;

import forge.ai.AITest;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.cost.CostAdjustmentPreview;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PriorityCostFeasibilityTest extends AITest {

    private final PriorityCostFeasibility feasibility = new PriorityCostFeasibility();

    @Test
    public void ordinaryFixedManaCostIsPayable() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Mountain", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(bolt)));
    }

    @Test
    public void supportedInsufficientManaIsUnpayable() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, spell(bolt)));
    }

    @Test
    public void floatingManaCanPayWithoutAnUntappedSource() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final Card mountain = addCard("Mountain", player);
        mountain.setTapped(true);
        addFloatingMana(player, mountain, manaPart(mountain, part -> true), (byte) ManaAtom.RED);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(bolt)));
    }

    @Test
    public void floatingManaAndOneSourceCanCombineForPayment() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bear = addCardToZone("Runeclaw Bear", player, ZoneType.Hand);
        final Card mountain = addCard("Mountain", player);
        addCard("Forest", player);
        mountain.setTapped(true);
        addFloatingMana(player, mountain, manaPart(mountain, part -> true), (byte) ManaAtom.COLORLESS);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(bear)));
    }

    @Test
    public void oneManaActivationMayProduceMultipleMana() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card doppelganger = addCard("Dimir Doppelganger", player);
        addCard("Dimir Aqueduct", player);
        addCard("Island", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, activated(doppelganger)));
    }

    @Test
    public void alternativeProductionModesUseAtMostOneTapActivation() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Izzet Guildgate", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(bolt)));
    }

    @Test
    public void restrictedFloatingManaCannotPayAnUnrelatedSpell() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bear = addCardToZone("Runeclaw Bear", player, ZoneType.Hand);
        final Card temple = addCardToZone("Eldrazi Temple", player, ZoneType.Hand);
        addCard("Forest", player);
        addFloatingMana(player, temple, manaPart(temple, part -> !part.getManaRestrictions().isEmpty()), (byte) ManaAtom.COLORLESS);
        addFloatingMana(player, temple, manaPart(temple, part -> !part.getManaRestrictions().isEmpty()), (byte) ManaAtom.COLORLESS);

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, spell(bear)));
    }

    @Test
    public void ordinaryXUsesTheMinimumLegalValueForExistence() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card drainLife = addCardToZone("Drain Life", player, ZoneType.Hand);
        addCard("Swamp", player);
        addCard("Swamp", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(drainLife)));
    }

    @Test
    public void ordinaryXIsUnpayableWhenItsFixedPortionCannotBePaid() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card drainLife = addCardToZone("Drain Life", player, ZoneType.Hand);
        addCard("Swamp", player);

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, spell(drainLife)));
    }

    @Test
    public void equivalentTapSourcesDoNotExhaustTheBoundedSearch() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card darkness = addCardToZone("Spinning Darkness", player, ZoneType.Hand);
        for (int index = 0; index < 25; index++) {
            addCard("Swamp", player);
        }

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(darkness)));
    }

    @Test
    public void abundantWrongColorSourcesAreProvenUnpayableWithinTheSearchBound() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card darkness = addCardToZone("Spinning Darkness", player, ZoneType.Hand);
        for (int index = 0; index < 25; index++) {
            addCard("Forest", player);
        }

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, spell(darkness)));
    }

    @Test
    public void xMinimumIsIncludedInExistentialFeasibility() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card spell = addCardToZone("Expansive Reapplication", player, ZoneType.Hand);
        addCard("Forest", player);
        addCard("Island", player);
        addCard("Mountain", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, spell(spell)));
    }

    @Test
    public void additionalLifeCostUsesForgeCostPartLegality() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card rats = addCard("Sewer Rats", player);
        addCard("Swamp", player);

        assertResult(PriorityCostFeasibility.Result.PAYABLE, assess(player, activated(rats)));
        player.setLife(0, null);
        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, activated(rats)));
    }

    @Test
    public void tappedManaSourceIsNotAvailable() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final Card mountain = addCard("Mountain", player);
        mountain.setTapped(true);

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE, assess(player, spell(bolt)));
    }

    @Test
    public void repeatedAssessmentDoesNotMutateTheObservedGameState() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final Card mountain = addCard("Mountain", player);
        mountain.setTapped(true);
        addFloatingMana(player, mountain, manaPart(mountain, part -> true), (byte) ManaAtom.RED);

        final int manaBefore = player.getManaPool().totalMana();
        final int lifeBefore = player.getLife();
        final boolean tappedBefore = mountain.isTapped();
        final int handBefore = player.getCardsIn(ZoneType.Hand).size();
        final SpellAbility ability = spell(bolt);
        final Player activatingPlayerBefore = ability.getActivatingPlayer();
        final PriorityCostFeasibility.Assessment first = assess(player, ability);
        final PriorityCostFeasibility.Assessment second = assess(player, ability);

        assertEquals(second, first);
        assertEquals(player.getManaPool().totalMana(), manaBefore);
        assertEquals(player.getLife(), lifeBefore);
        assertEquals(mountain.isTapped(), tappedBefore);
        assertEquals(player.getCardsIn(ZoneType.Hand).size(), handBefore);
        assertEquals(ability.getActivatingPlayer(), activatingPlayerBefore);
    }

    @Test
    public void activatedManaCanFloatThenMakeTheSpellCandidatePayable() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final Card mountain = addCard("Mountain", player);
        final SpellAbility manaAbility = firstManaAbility(mountain);
        final PriorityActionProvider provider = new PriorityActionProvider();

        manaPart(mountain, part -> true).produceMana(manaAbility);
        mountain.setTapped(true);
        assertTrue(hasCastCandidate(provider.createPriorityRequest(player), bolt));
    }

    @Test
    public void dynamicManaProductionIsExplicitlyUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        final Card birds = addCard("Birds of Paradise", player);
        birds.setSickness(false);

        final PriorityCostFeasibility.Assessment assessment = assess(player, spell(bolt));

        assertResult(PriorityCostFeasibility.Result.UNSUPPORTED, assessment);
        assertEquals(assessment.getUnsupportedReason(), PriorityCostFeasibility.UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
    }

    @Test
    public void multipleCostReductionsRequireAnExplicitPreviewChoice() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card divination = addCardToZone("Divination", player, ZoneType.Hand);
        addCard("Goblin Electromancer", player);
        addCard("Goblin Electromancer", player);
        addCard("Island", player);
        addCard("Mountain", player);

        final PriorityCostFeasibility.Assessment assessment = assess(player, spell(divination));

        assertResult(PriorityCostFeasibility.Result.UNSUPPORTED, assessment);
        assertEquals(assessment.getUnsupportedReason(), PriorityCostFeasibility.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED);
        assertEquals(assessment.getAdjustmentStatus(), CostAdjustmentPreview.Status.CHOICE_REQUIRED);
        assertTrue(assessment.getAdjustmentPreviewNanos() >= 0L);
    }

    @Test
    public void specificXUsesAllCostIndependentFixedManaCapacityIncludingOffColorSources() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = spell(addCardToZone("Invoke the Firemind", player, ZoneType.Hand));
        invoke.setActivatingPlayer(player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        for (int index = 0; index < 5; index++) {
            addCard("Forest", player);
        }

        final PriorityCostFeasibility.CapacityAssessment capacity = feasibility.assessManaCapacity(player, invoke);

        assertEquals(capacity.getResult(), PriorityCostFeasibility.CapacityResult.SUPPORTED);
        assertEquals(capacity.getMaximumManaUnits(), 8L);
        assertResult(PriorityCostFeasibility.Result.PAYABLE,
                feasibility.assessPaymentAtX(player, invoke, 5));
        assertResult(PriorityCostFeasibility.Result.UNPAYABLE,
                feasibility.assessPaymentAtX(player, invoke, 6));
    }

    @Test
    public void capacityCountsTheLargestFixedBundleOncePerSource() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility ability = spell(addCardToZone("Divination", player, ZoneType.Hand));
        ability.setActivatingPlayer(player);
        addCard("Dimir Aqueduct", player);
        addCard("Island", player);

        final PriorityCostFeasibility.CapacityAssessment capacity = feasibility.assessManaCapacity(player, ability);

        assertEquals(capacity.getResult(), PriorityCostFeasibility.CapacityResult.SUPPORTED);
        assertEquals(capacity.getMaximumManaUnits(), 3L);
    }

    @Test
    public void incompleteManaInventoryCannotClaimACompleteCapacity() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility ability = spell(addCardToZone("Lightning Bolt", player, ZoneType.Hand));
        ability.setActivatingPlayer(player);
        final Card birds = addCard("Birds of Paradise", player);
        birds.setSickness(false);

        final PriorityCostFeasibility.CapacityAssessment capacity = feasibility.assessManaCapacity(player, ability);

        assertEquals(capacity.getResult(), PriorityCostFeasibility.CapacityResult.UNSUPPORTED);
        assertEquals(capacity.getUnsupportedReason(),
                PriorityCostFeasibility.UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
    }

    @Test
    public void specificXAssessmentUsesRootCostInsteadOfZeroCostSubAbility() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = spell(addCardToZone("Invoke the Firemind", player, ZoneType.Hand));
        invoke.setActivatingPlayer(player);
        final AbilitySub sub = new AbilitySub(ApiType.Draw, invoke.getHostCard(), null, Map.of());
        sub.setParent(invoke);
        sub.setActivatingPlayer(player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);

        assertResult(PriorityCostFeasibility.Result.UNPAYABLE,
                feasibility.assessPaymentAtX(player, sub, 1));
    }

    @Test
    public void specificXAssessmentDoesNotMutateRootManaAbilitiesOrManaPool() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = spell(addCardToZone("Invoke the Firemind", player, ZoneType.Hand));
        invoke.setActivatingPlayer(player);
        final Card island = addCard("Island", player);
        final SpellAbility manaAbility = firstManaAbility(island);
        final Player manaActivatorBefore = manaAbility.getActivatingPlayer();
        final int poolBefore = player.getManaPool().totalMana();

        feasibility.assessPaymentAtX(player, invoke, 0);

        assertEquals(invoke.getActivatingPlayer(), player);
        assertEquals(invoke.getXManaCostPaid(), null);
        assertEquals(manaAbility.getActivatingPlayer(), manaActivatorBefore);
        assertEquals(player.getManaPool().totalMana(), poolBefore);
        assertEquals(island.isTapped(), false);
    }

    private PriorityCostFeasibility.Assessment assess(final Player player, final SpellAbility ability) {
        return feasibility.assessPayment(player, ability);
    }

    private static void assertResult(final PriorityCostFeasibility.Result expected,
            final PriorityCostFeasibility.Assessment assessment) {
        assertEquals(assessment.getResult(), expected);
    }

    private static SpellAbility spell(final Card card) {
        for (final SpellAbility ability : card.getSpellAbilities()) {
            if (ability.isSpell()) {
                return ability;
            }
        }
        throw new AssertionError("No spell ability on " + card.getName());
    }

    private static SpellAbility activated(final Card card) {
        for (final SpellAbility ability : card.getSpellAbilities()) {
            if (ability.isActivatedAbility() && !ability.isManaAbility()) {
                return ability;
            }
        }
        throw new AssertionError("No non-mana activated ability on " + card.getName());
    }

    private static SpellAbility firstManaAbility(final Card card) {
        for (final SpellAbility ability : card.getManaAbilities()) {
            return ability;
        }
        throw new AssertionError("No mana ability on " + card.getName());
    }

    private static AbilityManaPart manaPart(final Card card, final Predicate<AbilityManaPart> predicate) {
        for (final SpellAbility ability : card.getManaAbilities()) {
            for (final AbilityManaPart part : ability.getAllManaParts()) {
                if (predicate.test(part)) {
                    return part;
                }
            }
        }
        throw new AssertionError("No matching mana part on " + card.getName());
    }

    private static void addFloatingMana(final Player player, final Card source, final AbilityManaPart part,
            final byte color) {
        player.getManaPool().addManaNoEvent(new Mana(color, source, part, player));
    }

    private static boolean hasCastCandidate(final DecisionRequest request, final Card source) {
        return request.getCandidates().stream().anyMatch(candidate -> candidate.getKind() == PriorityActionKind.CAST_SPELL
                && source.getName().equals(candidate.getSourceName()));
    }
}

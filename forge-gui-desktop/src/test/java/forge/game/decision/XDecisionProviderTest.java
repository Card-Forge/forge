package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostAdjustmentPreview;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlaySpellAbility;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XDecisionProviderTest extends AITest {
    private final XDecisionProvider provider = new XDecisionProvider();

    @Test
    public void invokeDrawModeExportsEveryCompletionSafeXIncludingZero() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Forest", player);
        addCard("Forest", player);

        final DecisionRequest request = decision(invoke, player, null);

        assertEquals(values(request), List.of(0, 1, 2));
        assertEquals(keys(request), List.of("X|0", "X|1", "X|2"));
        assertFalse(request.isForced());
        assertEquals(request.getXContext().getRawMin(), 0);
        assertEquals(request.getXContext().getRawMax(), Integer.MAX_VALUE);
    }

    @Test
    public void offColorSourcesExtendGenericXCapacity() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        for (int index = 0; index < 5; index++) {
            addCard("Forest", player);
        }

        assertEquals(values(decision(invoke, player, null)), List.of(0, 1, 2, 3, 4, 5));
    }

    @Test
    public void exactlyOneLegalXIsAForcedAtomicOutcome() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);

        final DecisionRequest request = decision(invoke, player, null);

        assertEquals(values(request), List.of(0));
        assertTrue(request.isForced());
    }

    @Test
    public void forgeXMinXMaxAndAnnounceMaxBoundTheDomain() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        invoke.getMapParams().put("XMin", "1");
        invoke.getMapParams().put("XMax", "4");
        invoke.getMapParams().put("AnnounceMax", "3");
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        for (int index = 0; index < 5; index++) {
            addCard("Forest", player);
        }

        assertEquals(values(decision(invoke, player, null)), List.of(1, 2, 3));
    }

    @Test
    public void fixedReductionAndIncreaseChangeThePayableSet() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility reduced = invoke(player, 0);
        addCard("Goblin Electromancer", player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Forest", player);

        assertEquals(values(decision(reduced, player, null)), List.of(0, 1, 2));

        final Game secondGame = initAndCreateGame();
        final Player secondPlayer = secondGame.getPlayers().get(1);
        final SpellAbility increased = invoke(secondPlayer, 0);
        addCard("Thorn of Amethyst", secondPlayer);
        addCard("Island", secondPlayer);
        addCard("Island", secondPlayer);
        addCard("Mountain", secondPlayer);
        addCard("Forest", secondPlayer);

        assertEquals(values(decision(increased, secondPlayer, null)), List.of(0));
    }

    @Test
    public void applyingXUsesForgeStateAndSpecificCostCarriesItForward() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Forest", player);

        final DecisionRequest request = decision(invoke, player, null);
        provider.apply(request, request.getCandidates().get(1));

        assertEquals(invoke.getXManaCostPaid(), Integer.valueOf(1));
        final CostAdjustmentPreview preview = CostAdjustment.preview(invoke.getPayCosts(), invoke, player,
                false, invoke.getXManaCostPaid(), invoke.getXColor());
        assertEquals(preview.getAdjustedManaCost().getGenericManaAmount(), 1);
        assertEquals(preview.getAdjustedManaCost().getConvertedManaCost(), 4);
        assertEquals(new PriorityCostFeasibility().assessPayment(player, invoke).getResult(),
                PriorityCostFeasibility.Result.PAYABLE);
    }

    @Test
    public void preexistingXDoesNotSuppressARealForgeAnnouncement() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Forest", player);

        final List<String> withoutOldX = keys(decision(invoke, player, null));
        invoke.setXManaCostPaid(9);

        final XDecisionProvider.Generation generation = provider.generateXRequest(invoke, player, null);

        assertEquals(generation.getStatus(), XDecisionProvider.Status.DECISION);
        assertEquals(keys(generation.getRequest()), withoutOldX);
        assertEquals(invoke.getXManaCostPaid(), Integer.valueOf(9));
    }

    @Test
    public void applyingCandidateReplacesPreexistingForgeX() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Forest", player);
        invoke.setXManaCostPaid(9);

        final DecisionRequest request = decision(invoke, player, null);
        provider.apply(request, request.getCandidates().get(1));

        assertEquals(invoke.getXManaCostPaid(), Integer.valueOf(1));
    }

    @Test
    public void staleCandidateIsRejectedBeforeMutation() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        final Card forest = addCard("Forest", player);
        final DecisionRequest request = decision(invoke, player, null);
        forest.setTapped(true);

        assertThrows(IllegalArgumentException.class,
                () -> provider.apply(request, request.getCandidates().get(1)));
        assertNull(invoke.getXManaCostPaid());
    }

    @Test
    public void continuationIdentityIsReusedOnlyForTheRealXRequest() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "42:Invoke the Firemind");

        final DecisionRequest request = decision(invoke, player, continuation);

        assertEquals(request.getXContext().getDecisionSequenceId(), Long.valueOf(481L));
        assertEquals(request.getXContext().getSubdecisionIndex(), Integer.valueOf(1));
    }

    @Test
    public void announcementChooserRemainsSeparateFromPaymentPayer() {
        final Game game = initAndCreateGame();
        final Player payer = game.getPlayers().get(1);
        final Player chooser = game.getPlayers().get(0);
        final SpellAbility invoke = invoke(payer, 0);
        addCard("Island", payer);
        addCard("Island", payer);
        addCard("Mountain", payer);

        final DecisionRequest request = decision(invoke, chooser, null);

        assertEquals(request.getXContext().getChoosingPlayerId(), chooser.getId());
        assertEquals(values(request), List.of(0));
    }

    @Test
    public void derivedCopiedAndWrapperXDoNotCreateAgentRequests() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility derived = invoke(player, 0);
        derived.setSVar("X", "Count$Valid Creature.YouCtrl");
        final SpellAbility copied = invoke(player, 0);
        copied.setCopied(true);
        final SpellAbility wrapper = mock(SpellAbility.class);
        when(wrapper.getRootAbility()).thenReturn(wrapper);
        when(wrapper.isWrapper()).thenReturn(true);

        assertEquals(provider.generateXRequest(derived, player, null).getStatus(),
                XDecisionProvider.Status.NOT_APPLICABLE);
        assertEquals(provider.generateXRequest(copied, player, null).getStatus(),
                XDecisionProvider.Status.NOT_APPLICABLE);
        assertEquals(provider.generateXRequest(wrapper, player, null).getStatus(),
                XDecisionProvider.Status.NOT_APPLICABLE);
    }

    @Test
    public void explicitCharmAnnouncementUsesForgeNeedXToPreventASecondCallback() throws Exception {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility charm = spell(addCardToZone("Kozilek's Command", player, ZoneType.Hand));
        charm.setActivatingPlayer(player);
        final PlayerController controller = mock(PlayerController.class);
        when(controller.announceRequirements(eq(charm), anyInt(), anyInt(), eq("X"))).thenReturn(2);
        final PlaySpellAbility play = new PlaySpellAbility(controller, charm);
        final Method announceValuesLikeX = PlaySpellAbility.class.getDeclaredMethod("announceValuesLikeX");
        announceValuesLikeX.setAccessible(true);

        assertTrue((Boolean) announceValuesLikeX.invoke(play));

        verify(controller, times(1)).announceRequirements(eq(charm), anyInt(), anyInt(), eq("X"));
        assertEquals(charm.getXManaCostPaid(), Integer.valueOf(2));
    }

    @Test
    public void unresolvedModeAndDynamicCapacityAreExplicitlyUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility unresolved = spell(addCardToZone("Invoke the Firemind", player, ZoneType.Hand));
        unresolved.setActivatingPlayer(player);
        assertEquals(provider.generateXRequest(unresolved, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.UNRESOLVED_MODE);

        final SpellAbility dynamic = invoke(player, 0);
        final Card birds = addCard("Birds of Paradise", player);
        birds.setSickness(false);
        assertEquals(provider.generateXRequest(dynamic, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);
    }

    @Test
    public void invokeWithDynamicAmountManaSourceDoesNotExportATruncatedDomain() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        final Card chalice = addCard("Everflowing Chalice", player);
        chalice.setCounters(CounterEnumType.CHARGE, 5);

        final XDecisionProvider.Generation generation = provider.generateXRequest(invoke, player, null);

        assertEquals(generation.getStatus(), XDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);
        assertNull(generation.getRequest());
    }

    @Test
    public void dynamicAnnouncementBoundsAreRejectedBeforeTheyCanLeakHiddenState() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);
        invoke.getMapParams().put("XMax", "Count$Valid Card.OppCtrl+inHand");

        assertEquals(provider.generateXRequest(invoke, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.DYNAMIC_ANNOUNCEMENT_BOUND);
    }

    @Test
    public void replacementAndAlternativeManaCannotUnderstateTheFiniteDomain() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility replaced = invoke(player, 0);
        addCard("Contamination", player);
        addCard("Island", player);
        assertEquals(provider.generateXRequest(replaced, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);

        final Game secondGame = initAndCreateGame();
        final Player secondPlayer = secondGame.getPlayers().get(1);
        final Player opponent = secondGame.getPlayers().get(0);
        final SpellAbility alternative = invoke(secondPlayer, 0);
        addCard("Island", opponent);
        secondPlayer.addChangedKeywords(List.of("Piracy"), null,
                secondGame.getNextTimestamp(), 0L);
        assertEquals(provider.generateXRequest(alternative, secondPlayer, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);
    }

    @Test
    public void fullyUnderstoodEmptyDomainIsInvalidXNotUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility invoke = invoke(player, 0);

        assertEquals(provider.generateXRequest(invoke, player, null).getStatus(),
                XDecisionProvider.Status.INVALID_X);
    }

    @Test
    public void nonManaXAndAdjustmentChoiceRemainExplicitlyUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility nonMana = invoke(player, 0);
        nonMana.setPayCosts(new Cost("X U U R Sac<X/Artifact>", false));
        assertEquals(provider.generateXRequest(nonMana, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.NON_MANA_X);

        final SpellAbility choice = invoke(player, 0);
        addCard("Goblin Electromancer", player);
        addCard("Goblin Electromancer", player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        assertEquals(provider.generateXRequest(choice, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED);
    }

    @Test
    public void unresolvedTargetChooserAndXDependentTargetCompletionAreUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final SpellAbility chooserUnknown = invoke(player, 1);
        chooserUnknown.getSubAbility().getMapParams().put("TargetingPlayer", "Opponent");
        assertEquals(provider.generateXRequest(chooserUnknown, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.TARGETING_PLAYER_CHOICE_REQUIRED);

        final SpellAbility xTargets = invoke(player, 1);
        xTargets.getSubAbility().setTargetRestrictions(new TargetRestrictions(Map.of(
                "ValidTgts", "Any", "TargetMin", "X", "TargetMax", "X")));
        assertEquals(provider.generateXRequest(xTargets, player, null).getUnsupportedReason(),
                XDecisionProvider.UnsupportedReason.TARGET_COMPLETION_X_DEPENDENT);
    }

    @Test
    public void irrelevantHiddenHandDoesNotChangeCandidateIdentityOrForcedStatus() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility invoke = invoke(player, 0);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        final DecisionRequest before = decision(invoke, player, null);
        addCardToZone("Counterspell", opponent, ZoneType.Hand);
        final DecisionRequest after = decision(invoke, player, null);

        assertEquals(keys(after), keys(before));
        assertEquals(after.isForced(), before.isForced());
    }

    @Test
    public void focusedNeutralFixtureMetricsRemainExactAndUnbucketed() {
        final List<Integer> candidateCounts = new ArrayList<>();
        SpellAbility largest = null;
        Player largestChooser = null;
        for (final int extraMana : List.of(0, 2, 5)) {
            final Game game = initAndCreateGame();
            final Player player = game.getPlayers().get(1);
            final SpellAbility invoke = invoke(player, 0);
            addCard("Island", player);
            addCard("Island", player);
            addCard("Mountain", player);
            for (int index = 0; index < extraMana; index++) {
                addCard("Forest", player);
            }
            candidateCounts.add(decision(invoke, player, null).getCandidates().size());
            largest = invoke;
            largestChooser = player;
        }
        final Game reducedGame = initAndCreateGame();
        final Player reducedPlayer = reducedGame.getPlayers().get(1);
        final SpellAbility reduced = invoke(reducedPlayer, 0);
        addCard("Goblin Electromancer", reducedPlayer);
        addCard("Island", reducedPlayer);
        addCard("Island", reducedPlayer);
        addCard("Mountain", reducedPlayer);
        addCard("Forest", reducedPlayer);
        candidateCounts.add(decision(reduced, reducedPlayer, null).getCandidates().size());

        final List<Long> timings = new ArrayList<>();
        for (int index = 0; index < 200; index++) {
            final XDecisionProvider.Generation generation = provider.generateXRequest(largest, largestChooser, null);
            if (index >= 20) {
                timings.add(generation.getGenerationNanos());
            }
        }
        timings.sort(Long::compareTo);
        System.out.println("FRL02D_FOCUSED_METRICS candidate_counts=" + candidateCounts
                + " candidate_mean=3.25 candidate_p50=3 candidate_p95=6 candidate_max=6"
                + " forced_percent=25.0 strategic_percent=75.0 candidate_min=0 candidate_max=5"
                + " generation_p50_ns=" + percentile(timings, 0.50)
                + " generation_p95_ns=" + percentile(timings, 0.95)
                + " generation_p99_ns=" + percentile(timings, 0.99));

        assertEquals(candidateCounts, List.of(1, 3, 6, 3));
    }

    private DecisionRequest decision(final SpellAbility ability, final Player player,
            final ActionContinuation continuation) {
        final XDecisionProvider.Generation generation = provider.generateXRequest(ability, player, continuation);
        assertEquals(generation.getStatus(), XDecisionProvider.Status.DECISION);
        return generation.getRequest();
    }

    private SpellAbility invoke(final Player player, final int modeIndex) {
        final SpellAbility ability = spell(addCardToZone("Invoke the Firemind", player, ZoneType.Hand));
        ability.setActivatingPlayer(player);
        final AbilitySub mode = ability.getAdditionalAbilityList("Choices").get(modeIndex);
        ability.setSubAbility((AbilitySub) mode.copy(player));
        return ability;
    }

    private static SpellAbility spell(final Card card) {
        return card.getSpellAbilities().stream().filter(SpellAbility::isSpell).findFirst().orElseThrow();
    }

    private static List<Integer> values(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getXValue).toList();
    }

    private static List<String> keys(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList();
    }

    private static long percentile(final List<Long> sorted, final double percentile) {
        return sorted.get(Math.min(sorted.size() - 1,
                (int) Math.ceil(percentile * sorted.size()) - 1));
    }
}

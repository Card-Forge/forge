package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ModeDecisionProviderTest extends AITest {
    private final ModeDecisionProvider provider = new ModeDecisionProvider();

    @Test
    public void callbackListKeepsOriginalModeOrdinalsAfterForgeFiltering() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card pendingCard = addCardToZone("Divination", opponent, ZoneType.Hand);
        final SpellAbility pendingSpell = spell(pendingCard);
        pendingSpell.setActivatingPlayer(opponent);
        game.getStackZone().add(pendingCard);
        game.getStack().add(pendingSpell);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final List<AbilitySub> original = izzet.getAdditionalAbilityList("Choices");

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(izzet,
                List.of(original.get(0), original.get(2)), 1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        final DecisionRequest request = generation.getRequest();
        assertEquals(request.getDecisionType(), DecisionType.MODE);
        assertEquals(keys(request), List.of("MODE|0", "MODE|2"));
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getModeOrdinal).toList(),
                List.of(0, 2));
        assertFalse(request.isForced());
        assertEquals(request.getModeContext().getChoosingPlayerId(), chooser.getId());
        assertEquals(request.getModeContext().getActivatingPlayerId(), chooser.getId());
        assertTrue(request.getCandidates().get(0).isModeUsesTargeting());
        assertFalse(request.getCandidates().get(1).isModeUsesTargeting());
        assertFalse(request.getCandidates().get(0).getModeDescription().isBlank());
    }

    @Test
    public void oneCallbackModeIsForcedAndRetainsTheLiveModePrivately() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final AbilitySub draw = izzet.getAdditionalAbilityList("Choices").get(2);

        final DecisionRequest request = provider.generateModeRequest(izzet, List.of(draw),
                1, 1, false, chooser, null).getRequest();

        assertTrue(request.isForced());
        assertSame(request.getCandidates().get(0).getMode(), draw);
    }

    @Test
    public void detachedMandatoryTargetModeWithoutTargetIsNotExported() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final List<AbilitySub> original = izzet.getAdditionalAbilityList("Choices");
        final AbilitySub creatureDamage = original.get(1);
        final AbilitySub draw = original.get(2);
        assertNull(izzet.getSubAbility());

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(izzet,
                List.of(creatureDamage, draw), 1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        assertEquals(keys(generation.getRequest()), List.of("MODE|2"));
        assertTrue(generation.getRequest().isForced());
        assertNull(izzet.getSubAbility());
    }

    @Test
    public void futureXUsesARequestFreeSharedPaymentDomain() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility invoke = spell(addCardToZone("Invoke the Firemind", chooser, ZoneType.Hand));
        invoke.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "Invoke the Firemind");

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(invoke,
                invoke.getAdditionalAbilityList("Choices"), 1, 1, false, chooser, continuation);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        assertEquals(keys(generation.getRequest()), List.of("MODE|0", "MODE|1"));
        assertEquals(generation.getRequest().getModeContext().getSubdecisionIndex(), Integer.valueOf(1));
        assertNull(invoke.getXManaCostPaid());
        assertNull(invoke.getSubAbility());
    }

    @Test
    public void announcedXRemainsAuthoritativeBeforeConfrontThePastMode() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCard("Jace Beleren", opponent);
        addCard("Swamp", chooser);
        addCard("Swamp", chooser);
        final SpellAbility confront = spell(addCardToZone("Confront the Past", chooser, ZoneType.Hand));
        confront.setActivatingPlayer(chooser);
        confront.setXManaCostPaid(1);
        final AbilitySub remove = confront.getAdditionalAbilityList("Choices").get(1);

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(confront,
                List.of(remove), 1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        assertEquals(keys(generation.getRequest()), List.of("MODE|1"));
        assertEquals(confront.getXManaCostPaid(), Integer.valueOf(1));
        assertNull(confront.getSubAbility());
    }

    @Test
    public void unsupportedModeStateDoesNotConsumeContinuationIndex() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "mode sequence");
        final SpellAbility unsupported = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        unsupported.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        unsupported.getMapParams().put("Optional", "True");

        final ModeDecisionProvider.Generation rejected = provider.generateModeRequest(unsupported,
                unsupported.getAdditionalAbilityList("Choices"), 1, 1, false, chooser, continuation);
        final SpellAbility supported = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        supported.setActivatingPlayer(chooser);
        final AbilitySub draw = supported.getAdditionalAbilityList("Choices").get(2);
        final DecisionRequest request = provider.generateModeRequest(supported, List.of(draw),
                1, 1, false, chooser, continuation).getRequest();

        assertEquals(rejected.getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        assertNull(rejected.getRequest());
        assertEquals(request.getModeContext().getSubdecisionIndex(), Integer.valueOf(1));
    }

    @Test
    public void dynamicOrMultiModeShapeIsExplicitlyUnsupported() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        izzet.getMapParams().put("CharmNum", "X");

        assertEquals(provider.generateModeRequest(izzet, izzet.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        izzet.getMapParams().put("CharmNum", "2");
        assertEquals(provider.generateModeRequest(izzet, izzet.getAdditionalAbilityList("Choices"),
                1, 2, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        izzet.getMapParams().put("CharmNum", "1");
        assertEquals(provider.generateModeRequest(izzet, izzet.getAdditionalAbilityList("Choices"),
                1, 1, true, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
    }

    @Test
    public void engineOwnedAndOutOfSliceModeShapesAreExplicit() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        final SpellAbility entwined = supportedIzzet(chooser);
        entwined.addOptionalCost(OptionalCost.Entwine);
        assertEquals(provider.generateModeRequest(entwined, entwined.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.NOT_APPLICABLE);

        final SpellAbility copied = supportedIzzet(chooser);
        copied.setCopied(true);
        assertEquals(provider.generateModeRequest(copied, copied.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.NOT_APPLICABLE);

        final SpellAbility triggered = supportedIzzet(chooser);
        triggered.setTrigger(mock(Trigger.class));
        assertEquals(provider.generateModeRequest(triggered, triggered.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility optional = supportedIzzet(chooser);
        optional.getMapParams().put("Optional", "True");
        assertEquals(provider.generateModeRequest(optional, optional.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility externalChooser = supportedIzzet(chooser);
        externalChooser.getMapParams().put("Chooser", "Opponent");
        assertEquals(provider.generateModeRequest(externalChooser,
                externalChooser.getAdditionalAbilityList("Choices"), 1, 1, false, opponent, null).getStatus(),
                ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility random = supportedIzzet(chooser);
        random.getMapParams().put("Random", "True");
        assertEquals(provider.generateModeRequest(random, random.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility modeCost = supportedIzzet(chooser);
        modeCost.getAdditionalAbilityList("Choices").get(2).getMapParams().put("ModeCost", "1");
        assertEquals(provider.generateModeRequest(modeCost, modeCost.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility spree = spell(addCardToZone("Three Steps Ahead", chooser, ZoneType.Hand));
        spree.setActivatingPlayer(chooser);
        assertEquals(provider.generateModeRequest(spree, spree.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility tiered = spell(addCardToZone("Ice Magic", chooser, ZoneType.Hand));
        tiered.setActivatingPlayer(chooser);
        assertEquals(provider.generateModeRequest(tiered, tiered.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);

        final SpellAbility pawprint = spell(addCardToZone("Season of the Burrow", chooser, ZoneType.Hand));
        pawprint.setActivatingPlayer(chooser);
        assertEquals(provider.generateModeRequest(pawprint, pawprint.getAdditionalAbilityList("Choices"),
                0, 5, true, chooser, null).getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
    }

    @Test
    public void generationDoesNotMutateLiveModalOrTargetState() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = supportedIzzet(chooser);
        final List<AbilitySub> possible = izzet.getAdditionalAbilityList("Choices");
        final List<String> charmOrder = possible.stream().map(mode -> mode.getSVar("CharmOrder")).toList();
        final List<Integer> targetCounts = possible.stream().map(mode -> mode.getTargets().size()).toList();

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(izzet, possible,
                1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        assertEquals(possible.stream().map(mode -> mode.getSVar("CharmOrder")).toList(), charmOrder);
        assertEquals(possible.stream().map(mode -> mode.getTargets().size()).toList(), targetCounts);
        assertNull(izzet.getSubAbility());
        assertNull(izzet.getXManaCostPaid());
        assertTrue(generation.getRuleLegalityProbes() > 0);
        assertTrue(generation.getDownstreamCompletionProbes() > 0);
    }

    @Test
    public void paymentCapabilityRejectsRelevantProduceManaReplacement() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        addCard("Island", chooser);
        addCard("Mountain", chooser);
        addCard("Contamination", chooser);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        final AbilitySub draw = izzet.getAdditionalAbilityList("Choices").get(2);

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(izzet, List.of(draw),
                1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(), ModeDecisionProvider.UnsupportedReason.MODE_PAYMENT_SUPPORT);
        assertNull(generation.getRequest());
    }

    @Test
    public void nestedTargetedModeBranchIsConservativelyUnsupported() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCard("Grizzly Bears", opponent);
        final SpellAbility izzet = supportedIzzet(chooser);
        final AbilitySub draw = izzet.getAdditionalAbilityList("Choices").get(2);
        final AbilitySub nestedTarget = (AbilitySub) izzet.getAdditionalAbilityList("Choices").get(1).copy(chooser);
        draw.setSubAbility(nestedTarget);

        final ModeDecisionProvider.Generation generation = provider.generateModeRequest(izzet, List.of(draw),
                1, 1, false, chooser, null);

        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        assertEquals(generation.getUnsupportedReason(),
                ModeDecisionProvider.UnsupportedReason.MODE_TARGET_COMPLETION);
        assertNull(generation.getRequest());
        assertNull(izzet.getSubAbility());
    }

    @Test
    public void choiceRestrictionIsUnsupportedWithoutConsumingContinuationIndex() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final ActionContinuation continuation = new ActionContinuation(481L,
                PriorityActionKind.CAST_SPELL, "choice restriction sequence");
        final SpellAbility restricted = supportedIzzet(chooser);
        restricted.getMapParams().put("ChoiceRestriction", "ThisTurn");

        final ModeDecisionProvider.Generation rejected = provider.generateModeRequest(restricted,
                restricted.getAdditionalAbilityList("Choices"), 1, 1, false, chooser, continuation);
        final SpellAbility supported = supportedIzzet(chooser);
        final AbilitySub draw = supported.getAdditionalAbilityList("Choices").get(2);
        final DecisionRequest request = provider.generateModeRequest(supported, List.of(draw),
                1, 1, false, chooser, continuation).getRequest();

        assertEquals(rejected.getStatus(), ModeDecisionProvider.Status.UNSUPPORTED);
        assertEquals(rejected.getUnsupportedReason(), ModeDecisionProvider.UnsupportedReason.UNSUPPORTED_SHAPE);
        assertNull(rejected.getRequest());
        assertEquals(request.getModeContext().getSubdecisionIndex(), Integer.valueOf(1));
    }

    @Test
    public void applyReturnsLiveModeAndLeavesForgeChainingAuthoritative() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final AbilitySub draw = izzet.getAdditionalAbilityList("Choices").get(2);
        final DecisionRequest request = provider.generateModeRequest(izzet, List.of(draw),
                1, 1, false, chooser, null).getRequest();

        final AbilitySub selected = provider.apply(request, request.getCandidates().get(0));

        assertSame(selected, draw);
        assertNull(izzet.getSubAbility());
        CharmEffect.chainAbilities(izzet, new ArrayList<>(List.of(selected)));
        assertFalse(izzet.getSubAbility() == selected);
        assertEquals(izzet.getSubAbility().getParam("SpellDescription"), draw.getParam("SpellDescription"));
    }

    @Test
    public void applyRejectsAStaleOriginalOrdinalWithoutSubstitution() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final AbilitySub draw = izzet.getAdditionalAbilityList("Choices").get(2);
        final DecisionRequest request = provider.generateModeRequest(izzet, List.of(draw),
                1, 1, false, chooser, null).getRequest();
        izzet.setAdditionalAbilityList("Choices", List.of(izzet.getAdditionalAbilityList("Choices").get(0)));

        expectThrows(IllegalArgumentException.class,
                () -> provider.apply(request, request.getCandidates().get(0)));
        assertNull(izzet.getSubAbility());
    }

    @Test
    public void applyRejectsModeWhoseMandatoryTargetCompletionBecameStale() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card target = addCard("Grizzly Bears", opponent);
        final SpellAbility izzet = supportedIzzet(chooser);
        final AbilitySub damage = izzet.getAdditionalAbilityList("Choices").get(1);
        final DecisionRequest request = provider.generateModeRequest(izzet, List.of(damage),
                1, 1, false, chooser, null).getRequest();

        game.getAction().moveToGraveyard(target, null);

        expectThrows(IllegalArgumentException.class,
                () -> provider.apply(request, request.getCandidates().get(0)));
        assertNull(izzet.getSubAbility());
    }

    @Test
    public void irrelevantHiddenOpponentCardDoesNotChangeModeRequest() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final SpellAbility invoke = spell(addCardToZone("Invoke the Firemind", chooser, ZoneType.Hand));
        invoke.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        final DecisionRequest before = provider.generateModeRequest(invoke,
                invoke.getAdditionalAbilityList("Choices"), 1, 1, false, chooser, null).getRequest();

        addCardToZone("Black Lotus", opponent, ZoneType.Hand);
        final DecisionRequest after = provider.generateModeRequest(invoke,
                invoke.getAdditionalAbilityList("Choices"), 1, 1, false, chooser, null).getRequest();

        assertEquals(keys(after), keys(before));
        assertEquals(after.isForced(), before.isForced());
    }

    @Test
    public void focusedModeMetricsCoverForcedStrategicAndThreeModeRequests() {
        final List<Integer> candidateCounts = new ArrayList<>();
        final List<Integer> ruleProbes = new ArrayList<>();
        final List<Integer> downstreamProbes = new ArrayList<>();

        Game game = initAndCreateGame();
        Player chooser = game.getPlayers().get(1);
        SpellAbility ability = supportedIzzet(chooser);
        addMetrics(provider.generateModeRequest(ability,
                List.of(ability.getAdditionalAbilityList("Choices").get(2)), 1, 1, false, chooser, null),
                candidateCounts, ruleProbes, downstreamProbes);

        game = initAndCreateGame();
        chooser = game.getPlayers().get(1);
        ability = spell(addCardToZone("Invoke the Firemind", chooser, ZoneType.Hand));
        ability.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        addMetrics(provider.generateModeRequest(ability, ability.getAdditionalAbilityList("Choices"),
                1, 1, false, chooser, null), candidateCounts, ruleProbes, downstreamProbes);

        game = initAndCreateGame();
        chooser = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCard("Grizzly Bears", opponent);
        final Card pendingCard = addCardToZone("Divination", opponent, ZoneType.Hand);
        final SpellAbility pendingSpell = spell(pendingCard);
        pendingSpell.setActivatingPlayer(opponent);
        game.getStackZone().add(pendingCard);
        game.getStack().add(pendingSpell);
        ability = supportedIzzet(chooser);
        final SpellAbility largest = ability;
        final Player largestChooser = chooser;
        addMetrics(provider.generateModeRequest(largest, largest.getAdditionalAbilityList("Choices"),
                1, 1, false, largestChooser, null), candidateCounts, ruleProbes, downstreamProbes);

        final List<Long> timings = new ArrayList<>();
        for (int index = 0; index < 200; index++) {
            final ModeDecisionProvider.Generation generation = provider.generateModeRequest(largest,
                    largest.getAdditionalAbilityList("Choices"), 1, 1, false, largestChooser, null);
            if (index >= 20) {
                timings.add(generation.getGenerationNanos());
            }
        }
        timings.sort(Long::compareTo);
        System.out.println("FRL02E_FOCUSED_METRICS candidate_counts=" + candidateCounts
                + " candidate_mean=2.0 candidate_p50=2 candidate_p95=3 candidate_max=3"
                + " rule_probes=" + ruleProbes + " downstream_probes=" + downstreamProbes
                + " generation_p50_ns=" + percentile(timings, 0.50)
                + " generation_p95_ns=" + percentile(timings, 0.95)
                + " generation_p99_ns=" + percentile(timings, 0.99));

        assertEquals(candidateCounts, List.of(1, 2, 3));
        assertEquals(ruleProbes, List.of(2, 3, 4));
        assertEquals(downstreamProbes, List.of(3, 3, 5));
    }

    private static void addMetrics(final ModeDecisionProvider.Generation generation,
            final List<Integer> candidateCounts, final List<Integer> ruleProbes,
            final List<Integer> downstreamProbes) {
        assertEquals(generation.getStatus(), ModeDecisionProvider.Status.DECISION);
        candidateCounts.add(generation.getRequest().getCandidates().size());
        ruleProbes.add(generation.getRuleLegalityProbes());
        downstreamProbes.add(generation.getDownstreamCompletionProbes());
    }

    private static long percentile(final List<Long> sorted, final double percentile) {
        return sorted.get(Math.min(sorted.size() - 1,
                (int) Math.ceil(percentile * sorted.size()) - 1));
    }

    private static List<String> keys(final DecisionRequest request) {
        return request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList();
    }

    private static SpellAbility spell(final Card card) {
        return card.getSpellAbilities().stream().filter(SpellAbility::isSpell).findFirst().orElseThrow();
    }

    private SpellAbility supportedIzzet(final Player chooser) {
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        addIzzetMana(chooser);
        return izzet;
    }

    private void addIzzetMana(final Player player) {
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
    }
}

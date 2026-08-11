package forge.game.decision;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/** Focused production tests for the narrow FRL-02K-B1 provider boundary. */
public class GelectrodeConfirmationDecisionProviderTest extends AITest {
    private final ConfirmationDecisionProvider provider = new ConfirmationDecisionProvider();

    @Test
    public void admittedGelectrodeCreatesExactlyOrderedUnforcedRequest() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(wrapper, player);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
        final DecisionRequest request = generation.getRequest();
        assertNotNull(request);
        assertEquals(request.getDecisionType(), DecisionType.CONFIRMATION);
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList(),
                List.of("ACCEPT", "DECLINE"));
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getConfirmationKind).toList(),
                List.of(ConfirmationCandidateKind.ACCEPT, ConfirmationCandidateKind.DECLINE));
        assertFalse(request.isForced());
        assertEquals(request.getConfirmationContext().getProfile(),
                ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF);
        assertEquals(request.getConfirmationContext().getEvent(), ConfirmationEventType.SPELL_CAST);
        assertEquals(request.getConfirmationContext().getTriggeringPlayerId(), player.getId());
        assertEquals(request.getConfirmationContext().getDeciderPlayerId(), player.getId());
        assertEquals(request.getConfirmationContext().getSourcePublicIdentity().getVisibleName(), "Gelectrode");
    }

    @Test
    public void contextKeepsInstanceIdentityTypedAndDoesNotExposeOccurrenceMetadata() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card source = addCard("Gelectrode", player);
        final WrappedAbility wrapper = wrapperFor(source, player, TriggerType.SpellCast);

        final DecisionRequest request = provider.generate(wrapper, player).getRequest();
        final CardSelectionCard identity = request.getConfirmationContext().getSourcePublicIdentity();

        assertEquals(identity.getCardId(), source.getId());
        assertEquals(identity.getGameTimestamp(), source.getGameTimestamp());
        assertFalse(Arrays.stream(request.getConfirmationContext().getClass().getDeclaredFields())
                .anyMatch(field -> field.getName().equals("requestId") || field.getName().equals("occurrenceIndex")));
    }

    @Test
    public void acceptAndDeclineApplyOnlyTheirBooleanMeaning() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);

        final DecisionRequest acceptRequest = provider.generate(wrapper, player).getRequest();
        assertTrue(provider.apply(acceptRequest, acceptRequest.getCandidates().get(0), wrapper));

        final DecisionRequest declineRequest = provider.generate(wrapper, player).getRequest();
        assertFalse(provider.apply(declineRequest, declineRequest.getCandidates().get(1), wrapper));
    }

    @Test
    public void wrongOrStaleCandidateCannotBeApplied() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);
        final DecisionRequest stale = provider.generate(wrapper, player).getRequest();
        final DecisionRequest current = provider.generate(wrapper, player).getRequest();

        assertThrows(IllegalArgumentException.class,
                () -> provider.apply(stale, stale.getCandidates().get(0), wrapper));
        assertThrows(IllegalArgumentException.class,
                () -> provider.apply(current, stale.getCandidates().get(0), wrapper));
        assertThrows(IllegalArgumentException.class,
                () -> provider.apply(current, LegalCandidate.pass(99), wrapper));
    }

    @Test
    public void nativeTeacherMapsBooleanWithoutSecondNativeCall() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);
        final DecisionRequest request = provider.generate(wrapper, player).getRequest();
        final int[] calls = {0};

        final LegalCandidate selected = provider.choose(request, () -> {
            calls[0]++;
            return true;
        });

        assertEquals(calls[0], 1);
        assertEquals(selected.getSemanticKey(), "ACCEPT");
        assertTrue(provider.apply(request, selected, wrapper));

        final DecisionRequest declineRequest = provider.generate(wrapper, player).getRequest();
        final LegalCandidate declined = provider.choose(declineRequest, () -> {
            calls[0]++;
            return false;
        });
        assertEquals(calls[0], 2);
        assertEquals(declined.getSemanticKey(), "DECLINE");
        assertFalse(provider.apply(declineRequest, declined, wrapper));
    }

    @Test
    public void requestCannotBeChosenOrAppliedTwice() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);
        final DecisionRequest request = provider.generate(wrapper, player).getRequest();

        final LegalCandidate selected = provider.choose(request, () -> true);
        assertThrows(IllegalArgumentException.class, () -> provider.choose(request, () -> false));
        assertTrue(provider.apply(request, selected, wrapper));
        assertThrows(IllegalArgumentException.class, () -> provider.apply(request, selected, wrapper));
        assertThrows(IllegalArgumentException.class, () -> provider.choose(request, () -> true));
    }

    @Test
    public void wrappedResolveAcceptUsesOneExternalCandidateAndUntapsOnce() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final ResolverController controller = installResolverController(game, player);
        final Card source = addCard("Gelectrode", player);
        source.setTapped(true);
        final WrappedAbility wrapper = wrapperFor(source, player, TriggerType.SpellCast);

        controller.getConfirmationDecisionProvider().setResolver(request -> {
            controller.resolverCalls++;
            return request.getCandidates().get(0);
        });

        wrapper.resolve();

        assertFalse(source.isTapped());
        assertEquals(controller.resolverCalls, 1);
        assertEquals(controller.confirmTriggerCalls, 0);
    }

    @Test
    public void wrappedResolveDeclineUsesOneExternalCandidateAndSkipsUntap() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final ResolverController controller = installResolverController(game, player);
        final Card source = addCard("Gelectrode", player);
        source.setTapped(true);
        final WrappedAbility wrapper = wrapperFor(source, player, TriggerType.SpellCast);

        controller.getConfirmationDecisionProvider().setResolver(request -> {
            controller.resolverCalls++;
            return request.getCandidates().get(1);
        });

        wrapper.resolve();

        assertTrue(source.isTapped());
        assertEquals(controller.resolverCalls, 1);
        assertEquals(controller.confirmTriggerCalls, 0);
    }

    @Test
    public void contraptionHelperConfirmTriggerDoesNotEnterB1Provider() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final ResolverController controller = installResolverController(game, player);
        controller.getConfirmationDecisionProvider().setResolver(request -> {
            controller.resolverCalls++;
            return request.getCandidates().get(0);
        });

        forge.StaticData.instance().attemptToLoadCard("Neural Network", "UST");
        final forge.item.PaperCard paperCard = forge.StaticData.instance().getVariantCards()
                .getCard("Neural Network", "UST");
        assertNotNull(paperCard);
        final Card contraption = Card.fromPaperCard(paperCard, player);
        contraption.setGameTimestamp(game.getNextTimestamp());
        player.getZone(ZoneType.Battlefield).add(contraption);
        final List<Card> selected = controller.chooseContraptionsToCrank(List.of(contraption));

        assertTrue(selected.isEmpty());
        assertEquals(controller.confirmTriggerCalls, 1);
        assertEquals(controller.resolverCalls, 0);
    }

    @Test
    public void unsupportedExternalOwnershipFailsClosedWithoutNativeFallback() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final ResolverController controller = installResolverController(game, player);
        controller.getConfirmationDecisionProvider().setResolver(request -> {
            controller.resolverCalls++;
            return request.getCandidates().get(0);
        });

        final UnsupportedConfirmationDecisionException exception = expectThrows(
                UnsupportedConfirmationDecisionException.class,
                () -> wrapperFor(game, player, "Lazav, Dimir Mastermind", TriggerType.ChangesZone).resolve());

        assertEquals(exception.getStatus(), ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(exception.getReason(), "CARD_IDENTITY");
        assertEquals(controller.resolverCalls, 0);
        assertEquals(controller.confirmTriggerCalls, 0);
    }

    @Test
    public void liveWrappedEffectMismatchIsUnsupported() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card source = addCard("Gelectrode", player);
        final WrappedAbility wrapper = wrapperWithLiveApi(source, player, TriggerType.SpellCast, ApiType.GainLife);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(wrapper, player);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(generation.getReason(), "LIVE_EFFECT_MISMATCH");
        assertNull(generation.getRequest());
    }

    @Test
    public void wrappedResolveEmitsCompleteConfirmationTrace() throws Exception {
        final Game game = initAndCreateGame();
        final Path directory = Files.createTempDirectory("frl02k-b1-trace-");
        final DeterminismTrace trace = DeterminismTrace.attach(game, 0,
                new forge.util.DeterminismAuditRandom(20260810L), directory);
        try {
            final Player player = game.getPlayers().get(1);
            final ResolverController controller = installResolverController(game, player);
            final Card source = addCard("Gelectrode", player);
            source.setTapped(true);
            final WrappedAbility wrapper = wrapperFor(source, player, TriggerType.SpellCast);
            controller.getConfirmationDecisionProvider().setResolver(
                    request -> request.getCandidates().get(0));

            wrapper.resolve();
            trace.finish();

            final List<String> records = Files.readAllLines(directory.resolve("game-001.decision.trace"),
                    StandardCharsets.UTF_8);
            assertEquals(records.size(), 2);
            assertTrue(records.get(0).contains("|CONFIRMATION|GELECTRODE_CONFIRMATION|0|false|[ACCEPT,DECLINE]|"));
            assertTrue(records.get(1).contains("|CHOSEN|ACCEPT|false|false|"));
            assertFalse(records.stream().anyMatch(value -> value.contains("SpellAbility")
                    || value.contains("WrappedAbility") || value.contains("CardLKI")));
        } finally {
            trace.finish();
            try (var files = Files.list(directory)) {
                for (final Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void unsupportedProfilesDoNotFabricateConfirmationRequests() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);

        final WrappedAbility lazav = wrapperFor(game, player, "Lazav, Dimir Mastermind", TriggerType.ChangesZone);
        final WrappedAbility bloodOperative = wrapperFor(game, player, "Blood Operative", TriggerType.ChangesZone);
        final WrappedAbility bloodOperativeCost = wrapperFor(game, player, "Blood Operative", TriggerType.Surveil);
        final WrappedAbility derived = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast, false);

        assertEquals(provider.generate(lazav, player).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(provider.generate(bloodOperative, player).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(provider.generate(bloodOperativeCost, player).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_COST);
        assertEquals(provider.generate(derived, player).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_PROVENANCE);
        assertNull(provider.generate(lazav, player).getRequest());
    }

    @Test
    public void mandatoryTriggerProducesNoConfirmationRequest() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility mandatory = wrapperFor(addCard("Bitterblossom", player), player,
                TriggerType.Phase, true, false);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(mandatory, player);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(generation.getReason(), "MANDATORY_TRIGGER");
        assertNull(generation.getRequest());
    }

    @Test
    public void hiddenSourceFailsClosedWithoutExportingHiddenIdentity() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card source = addCard("Gelectrode", player);
        final WrappedAbility wrapper = wrapperFor(source, player, TriggerType.SpellCast);
        source.turnFaceDown(true);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(wrapper, player);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.UNSUPPORTED_HIDDEN);
        assertNull(generation.getRequest());
    }

    @Test
    public void generationIsStateAndRngNeutral() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);

        final ConfirmationDecisionProvider.Generation generation = NeutralityAssertions.assertGameAndRngNeutral(
                "CONFIRMATION generation", game, () -> provider.generate(wrapper, player));

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
    }

    @Test
    public void rejectedAndHiddenAdmissionOutcomesAreAlsoStateAndRngNeutral() {
        final Game unsupportedGame = initAndCreateGame();
        final Player unsupportedPlayer = unsupportedGame.getPlayers().get(1);
        final WrappedAbility unsupported = wrapperFor(unsupportedGame, unsupportedPlayer,
                "Lazav, Dimir Mastermind", TriggerType.ChangesZone);
        assertEquals(NeutralityAssertions.assertGameAndRngNeutral("unsupported profile", unsupportedGame,
                () -> provider.generate(unsupported, unsupportedPlayer)).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);

        final Game costGame = initAndCreateGame();
        final Player costPlayer = costGame.getPlayers().get(1);
        final WrappedAbility cost = wrapperFor(costGame, costPlayer, "Blood Operative", TriggerType.Surveil);
        assertEquals(NeutralityAssertions.assertGameAndRngNeutral("cost-bearing profile", costGame,
                () -> provider.generate(cost, costPlayer)).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_COST);

        final Game hiddenGame = initAndCreateGame();
        final Player hiddenPlayer = hiddenGame.getPlayers().get(1);
        final Card hiddenSource = addCard("Gelectrode", hiddenPlayer);
        final WrappedAbility hidden = wrapperFor(hiddenSource, hiddenPlayer, TriggerType.SpellCast);
        hiddenSource.turnFaceDown(true);
        assertEquals(NeutralityAssertions.assertGameAndRngNeutral("hidden profile", hiddenGame,
                () -> provider.generate(hidden, hiddenPlayer)).getStatus(),
                ConfirmationDecisionProvider.Status.UNSUPPORTED_HIDDEN);
    }

    @Test
    public void contextHasNoActionContinuationOrRawEngineReferences() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final WrappedAbility wrapper = wrapperFor(game, player, "Gelectrode", TriggerType.SpellCast);
        final ConfirmationDecisionContext context = provider.generate(wrapper, player).getRequest()
                .getConfirmationContext();

        assertFalse(Arrays.stream(context.getClass().getDeclaredFields())
                .anyMatch(field -> field.getType().getName().contains("SpellAbility")
                        || field.getType().getName().contains("WrappedAbility")
                        || field.getType().getName().contains("ActionContinuation")));
    }

    private WrappedAbility wrapperFor(final Game game, final Player player, final String cardName,
            final TriggerType mode) {
        return wrapperFor(addCard(cardName, player), player, mode, true, true);
    }

    private WrappedAbility wrapperFor(final Card source, final Player player, final TriggerType mode) {
        return wrapperFor(source, player, mode, true, true);
    }

    private WrappedAbility wrapperFor(final Game game, final Player player, final String cardName,
            final TriggerType mode, final boolean intrinsic) {
        return wrapperFor(addCard(cardName, player), player, mode, intrinsic, true);
    }

    private WrappedAbility wrapperFor(final Card source, final Player player, final TriggerType mode,
            final boolean intrinsic) {
        return wrapperFor(source, player, mode, intrinsic, true);
    }

    private WrappedAbility wrapperFor(final Card source, final Player player, final TriggerType mode,
            final boolean intrinsic, final boolean optional) {
        final Trigger trigger = source.getTriggers().stream()
                .filter(value -> mode.equals(value.getMode()))
                .findFirst()
                .orElseThrow();
        final SpellAbility effect = AbilityFactory.getAbility(source, trigger.getParam("Execute"));
        effect.setActivatingPlayer(player);
        effect.setOptionalTrigger(optional);
        effect.setIntrinsic(intrinsic);
        final Card castSpell = addCardToZone("Opt", player, ZoneType.Hand);
        final SpellAbility castAbility = castSpell.getFirstSpellAbility();
        castAbility.setActivatingPlayer(player);
        final Map<AbilityKey, Object> triggeringObjects = AbilityKey.newMap();
        triggeringObjects.put(AbilityKey.Activator, player);
        triggeringObjects.put(AbilityKey.SpellAbility, castAbility);
        trigger.setTriggeringObjects(effect, triggeringObjects);
        return new WrappedAbility(trigger, effect, player);
    }

    private WrappedAbility wrapperWithLiveApi(final Card source, final Player player, final TriggerType mode,
            final ApiType liveApi) {
        final Trigger trigger = source.getTriggers().stream()
                .filter(value -> mode.equals(value.getMode()))
                .findFirst()
                .orElseThrow();
        final SpellAbility effect = AbilityFactory.getAbility(source, trigger.getParam("Execute"));
        effect.setApi(liveApi);
        effect.setActivatingPlayer(player);
        effect.setOptionalTrigger(true);
        effect.setIntrinsic(true);
        final Card castSpell = addCardToZone("Opt", player, ZoneType.Hand);
        final SpellAbility castAbility = castSpell.getFirstSpellAbility();
        castAbility.setActivatingPlayer(player);
        final Map<AbilityKey, Object> triggeringObjects = AbilityKey.newMap();
        triggeringObjects.put(AbilityKey.Activator, player);
        triggeringObjects.put(AbilityKey.SpellAbility, castAbility);
        trigger.setTriggeringObjects(effect, triggeringObjects);
        return new WrappedAbility(trigger, effect, player);
    }

    private ResolverController installResolverController(final Game game, final Player player) {
        final ResolverController controller = new ResolverController(game, player);
        player.dangerouslySetController(controller);
        return controller;
    }

    private static final class ResolverController extends PlayerControllerAi {
        private int confirmTriggerCalls;
        private int resolverCalls;

        private ResolverController(final Game game, final Player player) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-b1", null));
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            confirmTriggerCalls++;
            return false;
        }
    }
}

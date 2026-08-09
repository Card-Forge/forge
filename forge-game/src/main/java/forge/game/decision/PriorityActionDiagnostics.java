package forge.game.decision;

import forge.game.player.Player;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.GameEntity;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Optional, diagnostic-only trace for priority requests and their observed announcement callbacks.
 *
 * <p>The recorder is disabled unless {@value #OUTPUT_PATH_PROPERTY} specifies an output CSV. It observes
 * existing controller boundaries only; it never chooses, applies, or evaluates an action.</p>
 */
public final class PriorityActionDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.priority.metricsFile";

    private static final String HEADER = "event_type,process_id,decision_sequence_id,subdecision_index,"
            + "top_level_candidate_kind,top_level_source,downstream_callback_family,forced_if_known,"
            + "turn,phase,player,downstream_player,candidate_count,pass_present,pass_with_alternatives,request_generation_ns,"
            + "native_callback_ns,selection_mapping,feasibility_result,unsupported_reason,feasibility_ns,"
            + "adjustment_status,adjustment_reason,adjustment_preview_ns,target_group_index,target_status,done_present,"
            + "payment_stage,payer_id,remaining_cost_summary,payment_status,payment_unsupported_reason,"
            + "payment_candidate_kinds,x_raw_min,x_raw_max,x_candidate_min,x_candidate_max,x_status,"
            + "x_unsupported_reason,x_variable,mode_status,mode_rule_legality_probes,"
            + "mode_downstream_completion_probes,mode_original_ordinals,selection_adapter,selection_game_id,"
            + "selection_session_id,selection_step_index,selection_status,selection_reason,selection_selected_count,"
            + "selection_remaining_count,selection_initial_count,selection_candidate_shrinkage";
    private static final int COLUMN_COUNT = 54;
    private static final String OUTPUT_PATH = System.getProperty(OUTPUT_PATH_PROPERTY, "");
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final PriorityActionProvider PROVIDER = ENABLED ? new PriorityActionProvider() : null;
    private static final TargetDecisionProvider TARGET_PROVIDER = ENABLED ? new TargetDecisionProvider() : null;
    private static final PaymentDecisionProvider PAYMENT_PROVIDER = ENABLED ? new PaymentDecisionProvider() : null;
    private static final XDecisionProvider X_PROVIDER = ENABLED ? new XDecisionProvider() : null;
    private static final ModeDecisionProvider MODE_PROVIDER = ENABLED ? new ModeDecisionProvider() : null;
    private static final DiscardCardSelectionAdapter DISCARD_SELECTION_ADAPTER = ENABLED
            ? new DiscardCardSelectionAdapter() : null;
    private static final AttackDeclarationAdapter ATTACK_DECLARATION_ADAPTER = ENABLED
            ? new AttackDeclarationAdapter() : null;
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;
    private static final ThreadLocal<ActiveContinuation> ACTIVE_CONTINUATION = ENABLED ? new ThreadLocal<>() : null;

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(PriorityActionDiagnostics::writeCsv,
                    "forge-priority-action-diagnostics"));
        }
    }

    private PriorityActionDiagnostics() {
    }

    /** Builds and times a priority request immediately before the normal controller callback. */
    public static Capture capture(final Player player) {
        if (!ENABLED) {
            return null;
        }
        final long startedAtNanos = System.nanoTime();
        final PriorityActionProvider.Generation generation = PROVIDER.generatePriorityRequest(player);
        final DecisionRequest request = generation.getRequest();
        return new Capture(player.getGame().getPhaseHandler().getTurn(),
                String.valueOf(player.getGame().getPhaseHandler().getPhase()), player.getName(), request,
                generation.getFeasibilityMeasurements(), System.nanoTime() - startedAtNanos);
    }

    /** Starts timing the unmodified Forge player-controller callback. */
    public static long startNativeCallback() {
        return ENABLED ? System.nanoTime() : 0L;
    }

    /** Records the outcome of the unmodified Forge priority callback. */
    public static void recordNativeCallback(final Capture capture, final List<SpellAbility> selected,
            final long callbackStartedAtNanos) {
        if (capture == null || callbackStartedAtNanos == 0L) {
            return;
        }
        final LegalCandidate selectedCandidate = selectedCandidate(capture.request, selected);
        final String mapping = selectionMapping(capture.request, selected, selectedCandidate);
        synchronized (EVENTS) {
            for (final PriorityActionProvider.FeasibilityMeasurement measurement : capture.feasibilityMeasurements) {
                EVENTS.add(formatFeasibilityRecord(capture, measurement));
            }
            EVENTS.add(formatPriorityRecord(capture, selectedCandidate, mapping,
                    System.nanoTime() - callbackStartedAtNanos));
        }
    }

    /** Opens a correlation scope immediately before Forge announces the selected non-pass action. */
    public static void beginAction(final Capture capture, final SpellAbility selected, final int selectedAbilityCount) {
        if (capture == null || selected == null || !isSingleActionSelection(selectedAbilityCount)) {
            return;
        }
        final LegalCandidate candidate = selectedCandidate(capture.request, List.of(selected));
        if (candidate == null || candidate.getKind() == PriorityActionKind.PASS) {
            return;
        }
        ACTIVE_CONTINUATION.set(new ActiveContinuation(capture, new ActionContinuation(capture.request.getRequestId(),
                candidate.getKind(), topLevelSource(candidate))));
    }

    /** Closes the correlation scope after Forge returns from announcing the selected action. */
    public static void endAction() {
        if (ENABLED) {
            ACTIVE_CONTINUATION.remove();
        }
    }

    static boolean isSingleActionSelection(final int selectedAbilityCount) {
        return selectedAbilityCount == 1;
    }

    /** Records an existing downstream controller callback while a selected action is being announced. */
    public static void recordDownstreamCallback(final DownstreamCallbackFamily family, final int candidateCount,
            final Boolean forcedIfKnown, final Player downstreamPlayer) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        if (active == null) {
            return;
        }
        final ActionContinuation continuation = active.continuation;
        synchronized (EVENTS) {
            EVENTS.add(formatContinuationRecord("DOWNSTREAM", PROCESS_ID, continuation.getDecisionSequenceId(),
                    continuation.nextSubdecisionIndex(), continuation.getTopLevelCandidateKind(),
                    continuation.getTopLevelSource(), family, forcedIfKnown, active.capture.turn,
                    active.capture.phase, active.capture.player,
                    downstreamPlayer == null ? "" : downstreamPlayer.getName(), candidateCount));
        }
    }

    /** Counts Forge controller entry callbacks without allocating an agent subdecision index. */
    public static void recordRawPaymentCallback(final Player payer) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        synchronized (EVENTS) {
            EVENTS.add(formatRow("PAYMENT_CALLBACK", PROCESS_ID,
                    active == null ? "" : active.continuation.getDecisionSequenceId(), "",
                    active == null ? "" : active.continuation.getTopLevelCandidateKind(),
                    active == null ? "" : active.continuation.getTopLevelSource(), DecisionType.PAYMENT, "",
                    payer.getGame().getPhaseHandler().getTurn(), payer.getGame().getPhaseHandler().getPhase(),
                    active == null ? "" : active.capture.player, payer.getName(), "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", ""));
        }
    }

    /** Records the raw Forge callback separately from any completion-safe neutral X request. */
    public static void recordXAnnouncement(final SpellAbility ability, final Player choosingPlayer,
            final int rawMin, final int rawMax) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        final ActionContinuation continuation = active == null ? null : active.continuation;
        final int turn = ability.getHostCard().getGame().getPhaseHandler().getTurn();
        final String phase = String.valueOf(ability.getHostCard().getGame().getPhaseHandler().getPhase());
        final String player = active == null ? choosingPlayer.getName() : active.capture.player;
        synchronized (EVENTS) {
            EVENTS.add(formatXRecord("X_CALLBACK", PROCESS_ID,
                    continuation == null ? null : continuation.getDecisionSequenceId(), null,
                    continuation == null ? null : continuation.getTopLevelCandidateKind(),
                    continuation == null ? "" : continuation.getTopLevelSource(), false, turn, phase, player,
                    choosingPlayer.getName(), 0, 0L, rawMin, rawMax, null, null, null, null));
        }

        final XDecisionProvider.Generation generation;
        try {
            generation = X_PROVIDER.generateXRequest(ability, choosingPlayer, continuation);
        } catch (final RuntimeException ex) {
            synchronized (EVENTS) {
                EVENTS.add(formatXRecord("X_STATE", PROCESS_ID,
                        continuation == null ? null : continuation.getDecisionSequenceId(), null,
                        continuation == null ? null : continuation.getTopLevelCandidateKind(),
                        continuation == null ? "" : continuation.getTopLevelSource(), false, turn, phase, player,
                        choosingPlayer.getName(), 0, 0L, rawMin, rawMax, null, null,
                        XDecisionProvider.Status.UNSUPPORTED, "DIAGNOSTIC_EXCEPTION"));
            }
            return;
        }
        final DecisionRequest request = generation.getRequest();
        final XDecisionContext context = request == null ? null : request.getXContext();
        final Integer candidateMin = request == null ? null : request.getCandidates().get(0).getXValue();
        final Integer candidateMax = request == null ? null
                : request.getCandidates().get(request.getCandidates().size() - 1).getXValue();
        synchronized (EVENTS) {
            EVENTS.add(formatXRecord(request == null ? "X_STATE" : "X_VALUE", PROCESS_ID,
                    context == null ? continuation == null ? null : continuation.getDecisionSequenceId()
                            : context.getDecisionSequenceId(),
                    context == null ? null : context.getSubdecisionIndex(),
                    continuation == null ? null : continuation.getTopLevelCandidateKind(),
                    continuation == null ? "" : continuation.getTopLevelSource(),
                    request != null && request.isForced(), turn, phase, player, choosingPlayer.getName(),
                    request == null ? 0 : request.getCandidates().size(), generation.getGenerationNanos(),
                    rawMin, rawMax, candidateMin, candidateMax, generation.getStatus(),
                    generation.getUnsupportedReason()));
        }
    }

    /** Records the raw Forge MODE callback and, separately, any supported neutral MODE request. */
    public static void recordModeCallback(final SpellAbility ability, final List<AbilitySub> possible,
            final int min, final int num, final boolean allowRepeat, final Player choosingPlayer) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        final ActionContinuation continuation = active == null ? null : active.continuation;
        final int turn = ability.getHostCard().getGame().getPhaseHandler().getTurn();
        final String phase = String.valueOf(ability.getHostCard().getGame().getPhaseHandler().getPhase());
        final String player = active == null ? ability.getActivatingPlayer().getName() : active.capture.player;
        final boolean callbackForced = !allowRepeat && min == num && num == 1 && possible.size() == 1;
        synchronized (EVENTS) {
            EVENTS.add(formatModeRecord("MODE_CALLBACK", PROCESS_ID,
                    continuation == null ? null : continuation.getDecisionSequenceId(), null,
                    continuation == null ? null : continuation.getTopLevelCandidateKind(),
                    continuation == null ? "" : continuation.getTopLevelSource(), callbackForced,
                    turn, phase, player, choosingPlayer.getName(), possible.size(), 0L, null, null, 0, 0, ""));
        }

        final ModeDecisionProvider.Generation generation;
        try {
            generation = MODE_PROVIDER.generateModeRequest(ability, possible, min, num, allowRepeat,
                    choosingPlayer, continuation);
        } catch (final RuntimeException ex) {
            synchronized (EVENTS) {
                EVENTS.add(formatModeRecord("MODE_STATE", PROCESS_ID,
                        continuation == null ? null : continuation.getDecisionSequenceId(), null,
                        continuation == null ? null : continuation.getTopLevelCandidateKind(),
                        continuation == null ? "" : continuation.getTopLevelSource(), false,
                        turn, phase, player, choosingPlayer.getName(), 0, 0L,
                        ModeDecisionProvider.Status.UNSUPPORTED, "DIAGNOSTIC_EXCEPTION", 0, 0, ""));
            }
            return;
        }
        final DecisionRequest request = generation.getRequest();
        final ModeDecisionContext context = request == null ? null : request.getModeContext();
        final String ordinals = request == null ? "" : request.getCandidates().stream()
                .map(candidate -> Integer.toString(candidate.getModeOrdinal()))
                .reduce((left, right) -> left + "+" + right).orElse("");
        synchronized (EVENTS) {
            EVENTS.add(formatModeRecord(request == null ? "MODE_STATE" : "MODE", PROCESS_ID,
                    context == null ? continuation == null ? null : continuation.getDecisionSequenceId()
                            : context.getDecisionSequenceId(),
                    context == null ? null : context.getSubdecisionIndex(),
                    continuation == null ? null : continuation.getTopLevelCandidateKind(),
                    continuation == null ? "" : continuation.getTopLevelSource(),
                    request != null && request.isForced(), turn, phase, player, choosingPlayer.getName(),
                    request == null ? 0 : request.getCandidates().size(), generation.getGenerationNanos(),
                    generation.getStatus(), generation.getUnsupportedReason(), generation.getRuleLegalityProbes(),
                    generation.getDownstreamCompletionProbes(), ordinals));
        }
    }

    /** Records one neutral atomic request from the live Forge payment state. */
    public static void recordPaymentRequest(final ManaCostBeingPaid cost, final SpellAbility ability,
            final Player payer, final ManaConversionMatrix matrix) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        final ActionContinuation continuation = active == null ? null : active.continuation;
        final PaymentDecisionProvider.Generation generation = PAYMENT_PROVIDER.generatePaymentRequest(cost,
                ability, payer, matrix, continuation);
        final DecisionRequest request = generation.getRequest();
        final PaymentDecisionContext context = request == null ? null : request.getPaymentContext();
        final String eventType = request == null ? "PAYMENT_STATE" : "PAYMENT";
        final String candidateKinds = request == null ? "" : request.getCandidates().stream()
                .map(candidate -> candidate.getPaymentKind().name()).distinct().sorted()
                .reduce((left, right) -> left + "+" + right).orElse("");
        synchronized (EVENTS) {
            EVENTS.add(formatRow(eventType, PROCESS_ID,
                    context != null ? context.getDecisionSequenceId()
                            : active == null ? "" : active.continuation.getDecisionSequenceId(),
                    context == null ? "" : context.getSubdecisionIndex(),
                    active == null ? "" : active.continuation.getTopLevelCandidateKind(),
                    active == null ? "" : active.continuation.getTopLevelSource(), DecisionType.PAYMENT,
                    request == null ? "" : request.isForced(), payer.getGame().getPhaseHandler().getTurn(),
                    payer.getGame().getPhaseHandler().getPhase(), active == null ? "" : active.capture.player,
                    payer.getName(), request == null ? 0 : request.getCandidates().size(), "", "",
                    generation.getRequestGenerationNanos(), "", "", "",
                    generation.getUnsupportedReason(), "", "", "", "", "", "", "",
                    context == null ? "" : context.getPaymentStage(), payer.getId(),
                    context == null ? cost.toString(false, payer.getManaPool()) : context.getRemainingCostSummary(),
                    generation.getStatus(), generation.getUnsupportedReason(), candidateKinds));
        }
    }

    /**
     * Observes Forge's generic target-selection boundary after it has resolved the actual targeting player.
     * This is deliberately best-effort diagnostics: unsupported target semantics are recorded, never allowed
     * to alter the existing human or AI controller path.
     */
    public static void recordTargetRequest(final SpellAbility ability, final Player choosingPlayer) {
        if (!ENABLED) {
            return;
        }
        final ActiveContinuation active = ACTIVE_CONTINUATION.get();
        final ActionContinuation continuation = active == null ? null : active.continuation;
        final long startedAtNanos = System.nanoTime();
        try {
            final TargetDecisionProvider.Generation generation = TARGET_PROVIDER.generateTargetRequest(ability,
                    choosingPlayer, continuation);
            final DecisionRequest request = generation.getRequest();
            final TargetDecisionContext context = request == null ? null : request.getTargetContext();
            final int turn = active == null ? ability.getHostCard().getGame().getPhaseHandler().getTurn()
                    : active.capture.turn;
            final String phase = active == null
                    ? String.valueOf(ability.getHostCard().getGame().getPhaseHandler().getPhase())
                    : active.capture.phase;
            final String player = active == null || active.capture.player == null
                    ? ability.getActivatingPlayer().getName() : active.capture.player;
            final String topLevelSource = active == null ? "" : active.continuation.getTopLevelSource();
            final PriorityActionKind topLevelKind = active == null ? null
                    : active.continuation.getTopLevelCandidateKind();
            final boolean donePresent = request != null && request.getCandidates().stream()
                    .anyMatch(candidate -> candidate.getTargetKind() == TargetCandidateKind.DONE);
            synchronized (EVENTS) {
                EVENTS.add(formatTargetRecord(PROCESS_ID, context == null ? null : context.getDecisionSequenceId(),
                        context == null ? null : context.getSubdecisionIndex(), topLevelKind, topLevelSource,
                        request != null && request.isForced(), turn, phase, player, choosingPlayer.getName(),
                        request == null ? 0 : request.getCandidates().size(),
                        context == null ? -1 : context.getTargetGroupIndex(), generation.getStatus(), donePresent,
                        generation.getRequestGenerationNanos(), null));
            }
        } catch (final UnsupportedTargetDecisionException e) {
            final int turn = active == null ? ability.getHostCard().getGame().getPhaseHandler().getTurn()
                    : active.capture.turn;
            final String phase = active == null
                    ? String.valueOf(ability.getHostCard().getGame().getPhaseHandler().getPhase())
                    : active.capture.phase;
            final String player = active == null || active.capture.player == null
                    ? ability.getActivatingPlayer().getName() : active.capture.player;
            synchronized (EVENTS) {
                EVENTS.add(formatTargetRecord(PROCESS_ID, active == null ? null
                                : active.continuation.getDecisionSequenceId(), null,
                        active == null ? null : active.continuation.getTopLevelCandidateKind(),
                        active == null ? "" : active.continuation.getTopLevelSource(), false, turn, phase, player,
                        choosingPlayer.getName(), 0, -1, null, false, System.nanoTime() - startedAtNanos,
                        "UNSUPPORTED_TARGET_SEMANTICS"));
            }
        }
    }

    /** Snapshots the narrow discard callback before invoking the unchanged Forge controller. */
    public static DiscardSelectionCapture captureDiscardSelection(final Player chooser, final Player affectedPlayer,
            final SpellAbility ability, final CardCollection validCards, final int min, final int max,
            final CardCollectionView visibleToChooser) {
        if (!ENABLED) {
            return null;
        }
        try {
            final DiscardCardSelectionAdapter.Capture adapterCapture = DISCARD_SELECTION_ADAPTER.begin(chooser,
                    affectedPlayer, ability, validCards, min, max, visibleToChooser);
            return new DiscardSelectionCapture(adapterCapture, chooser.getGame().getId(),
                    chooser.getGame().getPhaseHandler().getTurn(),
                    String.valueOf(chooser.getGame().getPhaseHandler().getPhase()), chooser.getName(),
                    affectedPlayer.getName(), validCards.size());
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    /** Replays the controller's returned set only for diagnostics and never changes or rejects that result. */
    public static void recordDiscardSelection(final DiscardSelectionCapture capture,
            final CardCollectionView controllerResult, final long callbackStartedAtNanos) {
        if (capture == null) {
            return;
        }
        try {
            final long nativeNanos = callbackStartedAtNanos == 0L ? 0L
                    : System.nanoTime() - callbackStartedAtNanos;
            final DiscardCardSelectionAdapter.Capture adapterCapture = capture.adapterCapture;
            synchronized (EVENTS) {
                EVENTS.add(formatDiscardSelectionRecord("CARD_SELECTION_DISCARD_CALLBACK", PROCESS_ID,
                        capture.gameId, adapterCapture.getSelectionSessionId() < 0 ? null
                                : adapterCapture.getSelectionSessionId(), null, false, capture.turn, capture.phase,
                        capture.chooser, capture.affectedPlayer, capture.initialCandidateCount, 0L, nativeNanos,
                        adapterCapture.getStatus(), adapterCapture.getReason(), 0, capture.initialCandidateCount,
                        capture.initialCandidateCount, 0));
            }
            if (adapterCapture.getStatus() != DiscardCardSelectionAdapter.Status.SUPPORTED) {
                return;
            }
            final DiscardCardSelectionAdapter.Replay replay = DISCARD_SELECTION_ADAPTER.replay(adapterCapture,
                    controllerResult);
            synchronized (EVENTS) {
                for (final DiscardCardSelectionAdapter.ReplayStep step : replay.getSteps()) {
                    final DecisionRequest request = step.getRequest();
                    final CardSelectionContext context = request.getCardSelectionContext();
                    final int remaining = (int) request.getCandidates().stream()
                            .filter(candidate -> candidate.getCardSelectionKind()
                                    == CardSelectionCandidateKind.SELECT_CARD).count();
                    EVENTS.add(formatDiscardSelectionRecord("CARD_SELECTION", PROCESS_ID, context.getGameId(),
                            context.getSelectionSessionId(), context.getSelectionStepIndex(), request.isForced(),
                            capture.turn, capture.phase, capture.chooser, capture.affectedPlayer,
                            request.getCandidates().size(), step.getGenerationNanos(), 0L,
                            CardSelectionDecisionProvider.Status.DECISION, null, context.getSelectedCards().size(),
                            remaining, capture.initialCandidateCount, capture.initialCandidateCount - remaining));
                }
                if (replay.getStatus() != DiscardCardSelectionAdapter.ReplayStatus.COMPLETE) {
                    EVENTS.add(formatDiscardSelectionRecord("CARD_SELECTION_STATE", PROCESS_ID,
                            adapterCapture.getGameId(), adapterCapture.getSelectionSessionId(), null, false,
                            capture.turn, capture.phase, capture.chooser, capture.affectedPlayer, 0, 0L, 0L,
                            replay.getStatus(), replay.getReason(), controllerResult == null ? 0
                                    : controllerResult.size(), 0, capture.initialCandidateCount,
                            capture.initialCandidateCount));
                }
            }
        } catch (final RuntimeException ex) {
            try {
                synchronized (EVENTS) {
                    EVENTS.add(formatDiscardSelectionRecord("CARD_SELECTION_STATE", PROCESS_ID,
                            capture.gameId, null, null, false, capture.turn, capture.phase, capture.chooser,
                            capture.affectedPlayer, 0, 0L, 0L,
                            DiscardCardSelectionAdapter.ReplayStatus.MAPPING_FAILED, "DIAGNOSTIC_EXCEPTION", 0, 0,
                            capture.initialCandidateCount, capture.initialCandidateCount));
                }
            } catch (final RuntimeException ignored) {
                // Diagnostics must never alter the Forge callback result or resolution path.
            }
        }
    }

    /** Snapshots the narrow turn-based ATTACK boundary before the unchanged Forge controller callback. */
    public static AttackDeclarationCapture captureAttackDeclaration(final Player whoDeclares,
            final Player attackingPlayer, final Combat combat) {
        if (!ENABLED) {
            return null;
        }
        try {
            final AttackDeclarationAdapter.Capture adapterCapture = ATTACK_DECLARATION_ADAPTER.begin(whoDeclares,
                    attackingPlayer, combat);
            final int initialCandidateCount = adapterCapture.getStatus() == AttackDeclarationAdapter.Status.SUPPORTED
                    ? adapterCapture.getSession().getEligibleIdentities().size() : 0;
            return new AttackDeclarationCapture(adapterCapture, attackingPlayer.getGame().getId(),
                    attackingPlayer.getGame().getPhaseHandler().getTurn(),
                    String.valueOf(attackingPlayer.getGame().getPhaseHandler().getPhase()), whoDeclares.getName(),
                    attackingPlayer.getName(), initialCandidateCount);
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    /** Replays the unchanged controller's final declaration for diagnostics only. */
    public static void recordAttackDeclaration(final AttackDeclarationCapture capture, final Combat combat,
            final long callbackStartedAtNanos) {
        if (capture == null) {
            return;
        }
        try {
            final long nativeNanos = callbackStartedAtNanos == 0L ? 0L
                    : System.nanoTime() - callbackStartedAtNanos;
            final AttackDeclarationAdapter.Capture adapterCapture = capture.adapterCapture;
            synchronized (EVENTS) {
                EVENTS.add(formatAttackDeclarationRecord("ATTACK_CALLBACK", PROCESS_ID, capture.gameId,
                        adapterCapture.getStatus() == AttackDeclarationAdapter.Status.SUPPORTED
                                ? adapterCapture.getSession().getAttackSessionId() : null,
                        null, false, capture.turn, capture.phase, capture.declaringPlayer,
                        capture.attackingPlayer, capture.initialCandidateCount, 0L, nativeNanos,
                        adapterCapture.getStatus(), adapterCapture.getReason(), combat.getAttackers().size(),
                        capture.initialCandidateCount, capture.initialCandidateCount, 0));
            }
            if (adapterCapture.getStatus() != AttackDeclarationAdapter.Status.SUPPORTED) {
                synchronized (EVENTS) {
                    EVENTS.add(formatAttackDeclarationRecord("ATTACK_STATE", PROCESS_ID, capture.gameId,
                            null, null, false, capture.turn, capture.phase, capture.declaringPlayer,
                            capture.attackingPlayer, 0, 0L, 0L, adapterCapture.getStatus(), adapterCapture.getReason(),
                            combat.getAttackers().size(), 0, capture.initialCandidateCount,
                            capture.initialCandidateCount));
                }
                return;
            }
            final Map<Card, GameEntity> actualAssignments = combat.getAttackersAndDefenders();
            final AttackDeclarationAdapter.Replay replay = ATTACK_DECLARATION_ADAPTER.replay(adapterCapture,
                    actualAssignments);
            synchronized (EVENTS) {
                for (final AttackDeclarationAdapter.ReplayStep step : replay.getSteps()) {
                    final DecisionRequest request = step.getRequest();
                    final AttackDeclarationContext context = request.getAttackContext();
                    final int remaining = (int) request.getCandidates().stream()
                            .filter(candidate -> candidate.getAttackKind()
                                    == AttackDeclarationCandidateKind.ADD_ATTACKER).count();
                    EVENTS.add(formatAttackDeclarationRecord("ATTACK", PROCESS_ID, context.getGameId(),
                            context.getAttackSessionId(), context.getAttackStepIndex(), request.isForced(),
                            capture.turn, capture.phase, capture.declaringPlayer, capture.attackingPlayer,
                            request.getCandidates().size(), step.getGenerationNanos(), 0L,
                            AttackDeclarationDecisionProvider.Status.DECISION, null,
                            context.getSelectedAssignments().size(), remaining, capture.initialCandidateCount,
                            capture.initialCandidateCount - remaining));
                }
                if (replay.getStatus() != AttackDeclarationAdapter.ReplayStatus.COMPLETE) {
                    EVENTS.add(formatAttackDeclarationRecord("ATTACK_STATE", PROCESS_ID, capture.gameId,
                            adapterCapture.getSession().getAttackSessionId(), null, false, capture.turn,
                            capture.phase, capture.declaringPlayer, capture.attackingPlayer, 0, 0L, 0L,
                            replay.getStatus(), replay.getReason(), actualAssignments.size(), 0,
                            capture.initialCandidateCount, capture.initialCandidateCount));
                }
            }
        } catch (final RuntimeException ex) {
            try {
                synchronized (EVENTS) {
                    EVENTS.add(formatAttackDeclarationRecord("ATTACK_STATE", PROCESS_ID, capture.gameId,
                            null, null, false, capture.turn, capture.phase, capture.declaringPlayer,
                            capture.attackingPlayer, 0, 0L, 0L,
                            AttackDeclarationAdapter.ReplayStatus.MAPPING_FAILED, "DIAGNOSTIC_EXCEPTION",
                            0, 0, capture.initialCandidateCount, capture.initialCandidateCount));
                }
            } catch (final RuntimeException ignored) {
                // Diagnostics must never alter Forge's combat declaration or resolution.
            }
        }
    }

    private static LegalCandidate selectedCandidate(final DecisionRequest request, final List<SpellAbility> selected) {
        if (selected == null) {
            return PROVIDER.findCandidate(request, null);
        }
        if (selected.size() != 1) {
            return null;
        }
        return PROVIDER.findCandidate(request, selected.get(0));
    }

    private static String selectionMapping(final DecisionRequest request, final List<SpellAbility> selected,
            final LegalCandidate selectedCandidate) {
        if (selectedCandidate != null) {
            return selected == null ? "MAPPED_PASS" : "MAPPED";
        }
        if (selected != null && selected.size() != 1) {
            return "UNSUPPORTED_MULTI_ACTION";
        }
        if (selected == null) {
            return "UNMAPPED_PASS";
        }
        final SpellAbility ability = selected.get(0);
        System.err.println("Unmapped priority action diagnostic: host=" + ability.getHostCard().getName()
                + ", description=" + ability.getOriginalDescription() + ", cost=" + ability.getPayCosts());
        for (final LegalCandidate candidate : request.getCandidates()) {
            if (candidate.getSourceCardId() == ability.getHostCard().getId()) {
                final SpellAbility candidateAbility = candidate.getSpellAbility();
                System.err.println("Unmapped priority candidate diagnostic: description="
                        + candidateAbility.getOriginalDescription() + ", cost=" + candidateAbility.getPayCosts());
            }
        }
        return "UNMAPPED";
    }

    private static String formatPriorityRecord(final Capture capture, final LegalCandidate candidate,
            final String mapping, final long nativeCallbackNanos) {
        return formatRow("PRIORITY", Long.toString(PROCESS_ID), candidate == null ? "" : capture.request.getRequestId(),
                candidate == null ? "" : 0, candidate == null ? "" : candidate.getKind(),
                candidate == null ? "" : topLevelSource(candidate), "", Boolean.toString(capture.request.isForced()),
                capture.turn, capture.phase, capture.player, "", capture.request.getCandidates().size(), "true",
                Boolean.toString(capture.request.getCandidates().size() > 1), capture.generationNanos,
                nativeCallbackNanos, mapping, "", "", "", "", "", "", "", "", "");
    }

    static String formatContinuationRecord(final String eventType, final long processId, final long decisionSequenceId,
            final int subdecisionIndex, final PriorityActionKind topLevelKind, final String topLevelSource,
            final DownstreamCallbackFamily family, final Boolean forcedIfKnown, final int turn, final String phase,
            final String player, final String downstreamPlayer, final int candidateCount) {
        return formatRow(eventType, processId, decisionSequenceId, subdecisionIndex, topLevelKind, topLevelSource,
                family, forcedIfKnown, turn, phase, player, downstreamPlayer, candidateCount, "", "", "", "", "", "", "", "",
                "", "", "", "", "", "");
    }

    static String formatTargetRecord(final long processId, final Long decisionSequenceId,
            final Integer subdecisionIndex, final PriorityActionKind topLevelKind, final String topLevelSource,
            final boolean forced, final int turn, final String phase, final String player, final String choosingPlayer,
            final int candidateCount, final int targetGroupIndex, final TargetDecisionProvider.Status status,
            final boolean donePresent, final long generationNanos, final String unsupportedReason) {
        return formatRow("TARGET", processId, decisionSequenceId, subdecisionIndex, topLevelKind, topLevelSource,
                DecisionType.TARGET, forced, turn, phase, player, choosingPlayer, candidateCount, "", "",
                generationNanos, "", "", "", unsupportedReason, "", "", "", "", targetGroupIndex,
                status, donePresent);
    }

    static String formatXRecord(final String eventType, final long processId, final Long decisionSequenceId,
            final Integer subdecisionIndex, final PriorityActionKind topLevelKind, final String topLevelSource,
            final boolean forced, final int turn, final String phase, final String player, final String choosingPlayer,
            final int candidateCount, final long generationNanos, final int rawMin, final int rawMax,
            final Integer candidateMin, final Integer candidateMax, final XDecisionProvider.Status status,
            final Object unsupportedReason) {
        return formatRow(eventType, processId, decisionSequenceId, subdecisionIndex, topLevelKind, topLevelSource,
                DecisionType.X_VALUE, forced, turn, phase, player, choosingPlayer, candidateCount, "", "",
                generationNanos, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "", rawMin, rawMax, candidateMin, candidateMax, status, unsupportedReason, "X");
    }

    static String formatModeRecord(final String eventType, final long processId, final Long decisionSequenceId,
            final Integer subdecisionIndex, final PriorityActionKind topLevelKind, final String topLevelSource,
            final boolean forced, final int turn, final String phase, final String player, final String choosingPlayer,
            final int candidateCount, final long generationNanos, final ModeDecisionProvider.Status status,
            final Object unsupportedReason, final int ruleLegalityProbes, final int downstreamCompletionProbes,
            final String originalOrdinals) {
        return formatRow(eventType, processId, decisionSequenceId, subdecisionIndex, topLevelKind, topLevelSource,
                DecisionType.MODE, forced, turn, phase, player, choosingPlayer, candidateCount, "", "",
                generationNanos, "", "", "", unsupportedReason, "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", status, ruleLegalityProbes,
                downstreamCompletionProbes, originalOrdinals);
    }

    static String formatDiscardSelectionRecord(final String eventType, final long processId, final int gameId,
            final Long selectionSessionId, final Integer selectionStepIndex, final boolean forced, final int turn,
            final String phase, final String chooser, final String affectedPlayer, final int candidateCount,
            final long generationNanos, final long nativeCallbackNanos, final Object status, final Object reason,
            final int selectedCount, final int remainingCount, final int initialCount, final int candidateShrinkage) {
        final Object[] values = new Object[COLUMN_COUNT];
        Arrays.fill(values, "");
        values[0] = eventType;
        values[1] = processId;
        values[6] = DecisionType.CARD_SELECTION;
        values[7] = forced;
        values[8] = turn;
        values[9] = phase;
        values[10] = chooser;
        values[11] = affectedPlayer;
        values[12] = candidateCount;
        values[15] = generationNanos;
        values[16] = nativeCallbackNanos;
        values[44] = "DISCARD";
        values[45] = gameId < 0 ? "" : gameId;
        values[46] = selectionSessionId;
        values[47] = selectionStepIndex;
        values[48] = status;
        values[49] = reason;
        values[50] = selectedCount;
        values[51] = remainingCount;
        values[52] = initialCount;
        values[53] = candidateShrinkage;
        return formatRow(values);
    }

    static String formatAttackDeclarationRecord(final String eventType, final long processId, final int gameId,
            final Long selectionSessionId, final Integer selectionStepIndex, final boolean forced, final int turn,
            final String phase, final String declaringPlayer, final String attackingPlayer, final int candidateCount,
            final long generationNanos, final long nativeCallbackNanos, final Object status, final Object reason,
            final int selectedCount, final int remainingCount, final int initialCount, final int candidateShrinkage) {
        final Object[] values = new Object[COLUMN_COUNT];
        Arrays.fill(values, "");
        values[0] = eventType;
        values[1] = processId;
        values[6] = DecisionType.ATTACK;
        values[7] = forced;
        values[8] = turn;
        values[9] = phase;
        values[10] = declaringPlayer;
        values[11] = attackingPlayer;
        values[12] = candidateCount;
        values[15] = generationNanos;
        values[16] = nativeCallbackNanos;
        values[44] = "ATTACK";
        values[45] = gameId < 0 ? "" : gameId;
        values[46] = selectionSessionId;
        values[47] = selectionStepIndex;
        values[48] = status;
        values[49] = reason;
        values[50] = selectedCount;
        values[51] = remainingCount;
        values[52] = initialCount;
        values[53] = candidateShrinkage;
        return formatRow(values);
    }

    static String formatFeasibilityRecord(final long processId, final int turn, final int playerIndex,
            final PriorityCostFeasibility.Result result,
            final PriorityCostFeasibility.UnsupportedReason unsupportedReason, final long durationNanos,
            final forge.game.cost.CostAdjustmentPreview.Status adjustmentStatus,
            final forge.game.cost.CostAdjustmentPreview.Reason adjustmentReason,
            final long adjustmentPreviewNanos) {
        return formatFeasibilityRecord(processId, turn, "", Integer.toString(playerIndex), result,
                unsupportedReason, durationNanos, adjustmentStatus, adjustmentReason, adjustmentPreviewNanos);
    }

    private static String formatFeasibilityRecord(final Capture capture,
            final PriorityActionProvider.FeasibilityMeasurement measurement) {
        return formatFeasibilityRecord(PROCESS_ID, capture.turn, capture.phase, capture.player, measurement.getResult(),
                measurement.getUnsupportedReason(), measurement.getDurationNanos(), measurement.getAdjustmentStatus(),
                measurement.getAdjustmentReason(), measurement.getAdjustmentPreviewNanos());
    }

    private static String formatFeasibilityRecord(final long processId, final int turn, final String phase,
            final String player, final PriorityCostFeasibility.Result result,
            final PriorityCostFeasibility.UnsupportedReason unsupportedReason, final long durationNanos,
            final forge.game.cost.CostAdjustmentPreview.Status adjustmentStatus,
            final forge.game.cost.CostAdjustmentPreview.Reason adjustmentReason, final long adjustmentPreviewNanos) {
        return formatRow("FEASIBILITY", processId, "", "", "", "", "", "", turn, phase, player, "", "", "", "",
                "", "", "", result, unsupportedReason, durationNanos, adjustmentStatus, adjustmentReason,
                adjustmentPreviewNanos, "", "", "");
    }

    private static String topLevelSource(final LegalCandidate candidate) {
        return candidate.getSourceCardId() < 0 ? "" : candidate.getSourceCardId() + ":" + candidate.getSourceName();
    }

    private static String formatRow(final Object... values) {
        final StringBuilder row = new StringBuilder();
        for (int index = 0; index < Math.max(COLUMN_COUNT, values.length); index++) {
            if (index > 0) {
                row.append(',');
            }
            final Object value = index < values.length ? values[index] : "";
            final String text = value == null ? "" : value.toString();
            if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
                row.append('"').append(text.replace("\"", "\"\"")).append('"');
            } else {
                row.append(text);
            }
        }
        return row.toString();
    }

    private static void writeCsv() {
        final Path output = Path.of(OUTPUT_PATH);
        try {
            final Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                writer.write(HEADER);
                writer.newLine();
                synchronized (EVENTS) {
                    for (final String event : EVENTS) {
                        writer.write(event);
                        writer.newLine();
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("Unable to write Forge priority-action diagnostics: " + e.getMessage());
        }
    }

    /** Opaque timing state for one request/controller callback pair. */
    public static final class Capture {
        private final int turn;
        private final String phase;
        private final String player;
        private final DecisionRequest request;
        private final List<PriorityActionProvider.FeasibilityMeasurement> feasibilityMeasurements;
        private final long generationNanos;

        private Capture(final int turn, final String phase, final String player, final DecisionRequest request,
                final List<PriorityActionProvider.FeasibilityMeasurement> feasibilityMeasurements,
                final long generationNanos) {
            this.turn = turn;
            this.phase = phase;
            this.player = player;
            this.request = request;
            this.feasibilityMeasurements = feasibilityMeasurements;
            this.generationNanos = generationNanos;
        }
    }

    private static final class ActiveContinuation {
        private final Capture capture;
        private final ActionContinuation continuation;

        private ActiveContinuation(final Capture capture, final ActionContinuation continuation) {
            this.capture = capture;
            this.continuation = continuation;
        }
    }

    /** Opaque pre-controller state for one turn-based ATTACK callback. */
    public static final class AttackDeclarationCapture {
        private final AttackDeclarationAdapter.Capture adapterCapture;
        private final int gameId;
        private final int turn;
        private final String phase;
        private final String declaringPlayer;
        private final String attackingPlayer;
        private final int initialCandidateCount;

        private AttackDeclarationCapture(final AttackDeclarationAdapter.Capture adapterCapture, final int gameId,
                final int turn, final String phase, final String declaringPlayer, final String attackingPlayer,
                final int initialCandidateCount) {
            this.adapterCapture = adapterCapture;
            this.gameId = gameId;
            this.turn = turn;
            this.phase = phase;
            this.declaringPlayer = declaringPlayer;
            this.attackingPlayer = attackingPlayer;
            this.initialCandidateCount = initialCandidateCount;
        }
    }

    /** Opaque pre-controller state for one resolution-time discard callback. */
    public static final class DiscardSelectionCapture {
        private final DiscardCardSelectionAdapter.Capture adapterCapture;
        private final int gameId;
        private final int turn;
        private final String phase;
        private final String chooser;
        private final String affectedPlayer;
        private final int initialCandidateCount;

        private DiscardSelectionCapture(final DiscardCardSelectionAdapter.Capture adapterCapture, final int gameId,
                final int turn, final String phase, final String chooser, final String affectedPlayer,
                final int initialCandidateCount) {
            this.adapterCapture = adapterCapture;
            this.gameId = gameId;
            this.turn = turn;
            this.phase = phase;
            this.chooser = chooser;
            this.affectedPlayer = affectedPlayer;
            this.initialCandidateCount = initialCandidateCount;
        }
    }
}

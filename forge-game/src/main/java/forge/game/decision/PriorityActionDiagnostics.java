package forge.game.decision;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            + "adjustment_status,adjustment_reason,adjustment_preview_ns";
    private static final String OUTPUT_PATH = System.getProperty(OUTPUT_PATH_PROPERTY, "");
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final PriorityActionProvider PROVIDER = ENABLED ? new PriorityActionProvider() : null;
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
                nativeCallbackNanos, mapping, "", "", "", "", "", "");
    }

    static String formatContinuationRecord(final String eventType, final long processId, final long decisionSequenceId,
            final int subdecisionIndex, final PriorityActionKind topLevelKind, final String topLevelSource,
            final DownstreamCallbackFamily family, final Boolean forcedIfKnown, final int turn, final String phase,
            final String player, final String downstreamPlayer, final int candidateCount) {
        return formatRow(eventType, processId, decisionSequenceId, subdecisionIndex, topLevelKind, topLevelSource,
                family, forcedIfKnown, turn, phase, player, downstreamPlayer, candidateCount, "", "", "", "", "", "", "", "",
                "", "", "");
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
                adjustmentPreviewNanos);
    }

    private static String topLevelSource(final LegalCandidate candidate) {
        return candidate.getSourceCardId() < 0 ? "" : candidate.getSourceCardId() + ":" + candidate.getSourceName();
    }

    private static String formatRow(final Object... values) {
        final StringBuilder row = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                row.append(',');
            }
            final Object value = values[index];
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
}

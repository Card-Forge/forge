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
 * Optional, diagnostic-only trace for priority requests and their observed controller callback.
 *
 * <p>The recorder is disabled unless {@value #OUTPUT_PATH_PROPERTY} specifies an output CSV. It observes
 * existing controller boundaries only; it never chooses, applies, or evaluates an action.</p>
 */
public final class PriorityActionDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.priority.metricsFile";

    private static final String HEADER = "event_type,process_id,turn,phase,player,candidate_count,pass_present,"
            + "pass_with_alternatives,request_generation_ns,"
            + "native_callback_ns,selection_mapping,feasibility_result,unsupported_reason,feasibility_ns,"
            + "adjustment_status,adjustment_reason,adjustment_preview_ns";
    private static final String OUTPUT_PATH = System.getProperty(OUTPUT_PATH_PROPERTY, "");
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final PriorityActionProvider PROVIDER = ENABLED ? new PriorityActionProvider() : null;
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;

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
        final String mapping = selectionMapping(capture.request, selected);
        synchronized (EVENTS) {
            for (final PriorityActionProvider.FeasibilityMeasurement measurement : capture.feasibilityMeasurements) {
                EVENTS.add(formatFeasibilityRecord(capture, measurement));
            }
            EVENTS.add(formatPriorityRecord(capture, mapping,
                    System.nanoTime() - callbackStartedAtNanos));
        }
    }

    private static String selectionMapping(final DecisionRequest request, final List<SpellAbility> selected) {
        if (selected == null && PROVIDER.contains(request, null)) {
            return "MAPPED_PASS";
        }
        if (selected != null && selected.size() == 1 && PROVIDER.contains(request, selected.get(0))) {
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

    private static String formatPriorityRecord(final Capture capture, final String mapping, final long nativeCallbackNanos) {
        return formatRow("PRIORITY", Long.toString(PROCESS_ID), capture.turn, capture.phase, capture.player,
                capture.request.getCandidates().size(), "true",
                Boolean.toString(capture.request.getCandidates().size() > 1), capture.generationNanos,
                nativeCallbackNanos, mapping, "", "", "", "", "", "");
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
        return formatRow("FEASIBILITY", processId, turn, phase, player, "", "", "", "", "", "", result,
                unsupportedReason, durationNanos, adjustmentStatus, adjustmentReason, adjustmentPreviewNanos);
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
}

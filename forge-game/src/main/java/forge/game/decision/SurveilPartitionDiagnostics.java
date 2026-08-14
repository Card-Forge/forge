package forge.game.decision;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Process-local, sanitized diagnostics for the L2A capture milestone. */
public final class SurveilPartitionDiagnostics {
    static final String AUDIT_ENABLED_PROPERTY = "forge.surveil.partition.audit.enabled";
    static final String AUDIT_OUTPUT_PROPERTY = "forge.surveil.partition.audit.output";
    static final String AUDIT_SCHEMA = "FRL02L2A_SURVEIL_AUDIT_V1";
    private static final String CANONICAL_PROFILE = "SURVEIL_PARTITION";
    private static final String CANONICAL_FIRST_DECK = "Izzet Guild Kit";
    private static final String CANONICAL_SECOND_DECK = "Dimir Guild Kit";
    private static final int CANONICAL_GAMES = 10;
    private static final long CANONICAL_SEED = 20260810L;
    private static final String CANONICAL_WORKLOAD_COMMAND =
            "run-workload sim -d Izzet Guild Kit Dimir Guild Kit -n 10 -s 20260810 -q";
    private static final Set<String> APPROVED_REASON_TOKENS = Set.of(
            "NULL_TOP_N", "UNSUPPORTED_ADMISSION", "SESSION_INTEGRITY_FAILURE",
            "IDENTITY", "MAPPING_FAILED", "UNKNOWN");
    private static final Set<String> APPROVED_REASON_COUNTER_KEYS = Set.of(
            "capture_admission_failure_NULL_TOP_N",
            "capture_admission_failure_UNSUPPORTED_ADMISSION",
            "capture_admission_failure_SESSION_INTEGRITY_FAILURE",
            "capture_admission_failure_UNKNOWN",
            "mapping_failure_IDENTITY",
            "mapping_failure_MAPPING_FAILED",
            "mapping_failure_UNKNOWN");

    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty(AUDIT_ENABLED_PROPERTY, "false"));
    private static final Map<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(SurveilPartitionDiagnostics::write,
                    "forge-surveil-partition-diagnostics"));
        }
    }

    private SurveilPartitionDiagnostics() {
    }

    public static void recordCaptureAdmissionFailure(final String reason) {
        increment("capture_admission_failures");
        increment("capture_admission_failure_" + sanitize(reason));
    }

    static void recordArrangeCall() {
        increment("raw_arrange_for_surveil_invocations");
    }

    static void recordSessionSize(final int size) {
        if (size > 0) {
            increment("non_empty_sessions");
        }
        increment(size == 0 ? "n_bucket_0" : size == 1 ? "n_bucket_1"
                : size == 2 ? "n_bucket_2" : "n_bucket_ge3");
    }

    static void recordCallback(final boolean failure) {
        increment("native_callback_invocations");
        if (failure) {
            increment("native_callback_failures");
        }
    }

    static void recordMapping(final boolean valid, final String reason) {
        increment(valid ? "valid_partition_mappings" : "mapping_failures");
        if (!valid) {
            increment("mapping_failure_" + sanitize(reason));
        }
    }

    static void recordMembershipRequest() {
        increment("membership_request_count");
        incrementBy("candidate_count", 2L);
    }

    static void recordMembershipResult() {
        increment("membership_result_count");
        increment("teacher_eligibility_not_applicable_count");
    }

    static void recordN2Cardinality(final int graveyardCount, final int retainedCount) {
        if (graveyardCount == 0 && retainedCount == 2) {
            increment("n2_graveyard_0_retained_2");
        } else if (graveyardCount == 1 && retainedCount == 1) {
            increment("n2_graveyard_1_retained_1");
        } else if (graveyardCount == 2 && retainedCount == 0) {
            increment("n2_graveyard_2_retained_0");
        }
    }

    static void recordSymmetryConflict() {
        increment("public_symmetry_conflicts");
    }

    private static void increment(final String key) {
        incrementBy(key, 1L);
    }

    private static void incrementBy(final String key, final long amount) {
        if (ENABLED) {
            COUNTERS.computeIfAbsent(key, ignored -> new LongAdder()).add(amount);
        }
    }

    private static String sanitize(final String value) {
        return value != null && APPROVED_REASON_TOKENS.contains(value) ? value : "UNKNOWN";
    }

    private static void write() {
        if (!ENABLED) {
            return;
        }
        final String configuredPath = System.getProperty(AUDIT_OUTPUT_PROPERTY, "");
        if (configuredPath.isBlank()) {
            return;
        }
        try {
            final Path output = Path.of(configuredPath);
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                final List<String> requiredKeys = approvedCounterKeys();
                writer.write("schema=" + AUDIT_SCHEMA);
                writer.newLine();
                for (final String metadata : approvedWorkloadMetadata()) {
                    writer.write(metadata);
                    writer.newLine();
                }
                for (final String key : requiredKeys) {
                    try {
                        final String value = Long.toString(COUNTERS.getOrDefault(key, new LongAdder()).sum());
                        writer.write(key + "=" + value);
                        writer.newLine();
                    } catch (final IOException ignored) {
                        // Diagnostics must never alter shutdown behavior.
                    }
                }
            }
        } catch (final IOException | RuntimeException ignored) {
            // Diagnostics are strictly non-authoritative.
        }
    }

    private static List<String> approvedWorkloadMetadata() {
        if (!ENABLED) {
            return List.of();
        }
        final String javaCommand = System.getProperty("sun.java.command", "");
        final int workloadStart = javaCommand.indexOf("run-workload ");
        if (workloadStart < 0
                || !CANONICAL_WORKLOAD_COMMAND.equals(javaCommand.substring(workloadStart).trim())) {
            return List.of();
        }
        return List.of(
                "profile=" + CANONICAL_PROFILE,
                "workload_first_deck=" + CANONICAL_FIRST_DECK,
                "workload_second_deck=" + CANONICAL_SECOND_DECK,
                "games=" + CANONICAL_GAMES,
                "seed=" + CANONICAL_SEED);
    }

    private static List<String> approvedCounterKeys() {
        final Set<String> keys = new TreeSet<>(List.of(
                "raw_arrange_for_surveil_invocations", "capture_admission_failures", "non_empty_sessions",
                "n_bucket_0", "n_bucket_1", "n_bucket_2", "n_bucket_ge3", "native_callback_invocations",
                "native_callback_failures", "valid_partition_mappings", "mapping_failures",
                "membership_request_count", "membership_result_count", "candidate_count",
                "forced_request_count", "external_attempts", "trace_incomplete_count",
                "public_symmetry_conflicts", "teacher_eligibility_not_applicable_count",
                "teacher_eligibility_bc_eligible_count", "teacher_eligibility_bc_excluded_public_symmetry_count",
                "n2_graveyard_0_retained_2", "n2_graveyard_1_retained_1", "n2_graveyard_2_retained_0"));
        COUNTERS.keySet().stream()
                .filter(SurveilPartitionDiagnostics::isApprovedReasonCounter)
                .forEach(keys::add);
        return List.copyOf(keys);
    }

    private static boolean isApprovedReasonCounter(final String key) {
        return APPROVED_REASON_COUNTER_KEYS.contains(key);
    }
}

package forge.game.decision;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Disabled-by-default, value-only counters for the FRL-02L1 canonical gate. */
public final class SimultaneousTriggerOrderAuditDiagnostics {
    public static final String OUTPUT_FILE_PROPERTY = "forge.simultaneousTriggerOrder.auditFile";

    private static final Path OUTPUT = outputPath();
    private static final boolean ENABLED = OUTPUT != null;
    private static long total;
    private static long n0;
    private static long n1;
    private static long n2;
    private static long n3;
    private static long n4;
    private static long nOther;
    private static long rawMultiItemCallbacks;
    private static long rawNonL1MultiItemCallbacks;
    private static long rawOutsideL1NativeFallbacks;
    private static long l1Sessions;
    private static long l1AdmittedSessions;
    private static long l1OrderRequests;
    private static long l1CandidateSize2;
    private static long l1CandidateSize3;
    private static long l1CandidateSize4;
    private static long l1ForcedRequests;
    private static long l1UnsupportedFallbacks;
    private static long l1IntegrityFailures;
    private static long l1UnsupportedFailures;
    private static long l1InvalidExternalCandidates;
    private static long l1NativeCallbackFailures;
    private static long l1MappingFailures;
    private static long l1TraceIncomplete;
    private static long l1cSessions;
    private static long l1cAdmittedSessions;
    private static long l1cInputSize2;
    private static long l1cInputSize3;
    private static long l1cInputSize4;
    private static long l1cInputSizeOther;
    private static long l1cOrderRequests;
    private static long l1cCandidateSize2;
    private static long l1cCandidateSize3;
    private static long l1cCandidateSize4;
    private static long l1cCandidateSizeOther;
    private static long l1cForcedRequests;
    private static long l1cNativeTeacherCallbacks;
    private static long l1cIntegrityFailures;
    private static long l1cUnsupportedFailures;
    private static long l1cInvalidExternalCandidates;
    private static long l1cNativeCallbackFailures;
    private static long l1cMappingFailures;
    private static long l1cTraceIncomplete;
    private static final Map<String, Long> l1AdmissionRejections = new TreeMap<>();

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(
                    SimultaneousTriggerOrderAuditDiagnostics::write, "forge-order-l1-audit"));
        }
    }

    private SimultaneousTriggerOrderAuditDiagnostics() {
    }

    public static synchronized void recordRawInvocation(final int size) {
        if (!ENABLED) {
            return;
        }
        total++;
        if (size >= 2) {
            rawMultiItemCallbacks++;
        }
        switch (size) {
        case 0:
            n0++;
            break;
        case 1:
            n1++;
            break;
        case 2:
            n2++;
            break;
        case 3:
            n3++;
            break;
        case 4:
            n4++;
            break;
        default:
            nOther++;
            break;
        }
    }

    public static synchronized void recordSimultaneousTriggerProfileSession(final boolean admitted) {
        if (ENABLED) {
            l1Sessions++;
            if (admitted) {
                l1AdmittedSessions++;
            }
        }
    }

    public static synchronized void recordNonL1MultiItemCallback() {
        if (ENABLED) {
            rawNonL1MultiItemCallbacks++;
        }
    }

    public static synchronized void recordL1UnsupportedFallback() {
        if (ENABLED) {
            l1UnsupportedFallbacks++;
        }
    }

    public static synchronized void recordOutsideL1NativeFallback() {
        if (ENABLED) {
            rawOutsideL1NativeFallbacks++;
        }
    }

    public static synchronized void recordIntegrityFailure() {
        if (ENABLED) {
            l1IntegrityFailures++;
        }
    }

    public static synchronized void recordL1UnsupportedFailure() {
        if (ENABLED) {
            l1UnsupportedFailures++;
        }
    }

    public static synchronized void recordInvalidExternalCandidate() {
        if (ENABLED) {
            l1InvalidExternalCandidates++;
        }
    }

    public static synchronized void recordNativeCallbackFailure() {
        if (ENABLED) {
            l1NativeCallbackFailures++;
        }
    }

    public static synchronized void recordMappingFailure() {
        if (ENABLED) {
            l1MappingFailures++;
        }
    }

    public static synchronized void recordTraceIncomplete() {
        if (ENABLED) {
            l1TraceIncomplete++;
        }
    }

    public static synchronized void recordAdmissionRejection(final String reason) {
        if (ENABLED) {
            l1AdmissionRejections.merge(reason, 1L, Long::sum);
        }
    }

    public static synchronized void recordRequest(final int candidateCount, final boolean forced) {
        if (!ENABLED) {
            return;
        }
        l1OrderRequests++;
        if (candidateCount == 2) {
            l1CandidateSize2++;
        } else if (candidateCount == 3) {
            l1CandidateSize3++;
        } else if (candidateCount == 4) {
            l1CandidateSize4++;
        }
        if (forced) {
            l1ForcedRequests++;
        }
    }

    public static synchronized void recordCopySpellProfileSession(final int inputSize,
            final boolean admitted) {
        if (!ENABLED) {
            return;
        }
        l1cSessions++;
        if (admitted) {
            l1cAdmittedSessions++;
        }
        if (inputSize == 2) {
            l1cInputSize2++;
        } else if (inputSize == 3) {
            l1cInputSize3++;
        } else if (inputSize == 4) {
            l1cInputSize4++;
        } else {
            l1cInputSizeOther++;
        }
    }

    public static synchronized void recordCopySpellRequest(final int candidateCount, final boolean forced) {
        if (!ENABLED) {
            return;
        }
        l1cOrderRequests++;
        if (candidateCount == 2) {
            l1cCandidateSize2++;
        } else if (candidateCount == 3) {
            l1cCandidateSize3++;
        } else if (candidateCount == 4) {
            l1cCandidateSize4++;
        } else {
            l1cCandidateSizeOther++;
        }
        if (forced) {
            l1cForcedRequests++;
        }
    }

    public static synchronized void recordCopySpellNativeTeacherCallback() {
        if (ENABLED) {
            l1cNativeTeacherCallbacks++;
        }
    }

    public static synchronized void recordCopySpellIntegrityFailure() {
        if (ENABLED) {
            l1cIntegrityFailures++;
        }
    }

    public static synchronized void recordCopySpellUnsupportedFailure() {
        if (ENABLED) {
            l1cUnsupportedFailures++;
        }
    }

    public static synchronized void recordCopySpellInvalidExternalCandidate() {
        if (ENABLED) {
            l1cInvalidExternalCandidates++;
        }
    }

    public static synchronized void recordCopySpellNativeCallbackFailure() {
        if (ENABLED) {
            l1cNativeCallbackFailures++;
        }
    }

    public static synchronized void recordCopySpellMappingFailure() {
        if (ENABLED) {
            l1cMappingFailures++;
        }
    }

    public static synchronized void recordCopySpellTraceIncomplete() {
        if (ENABLED) {
            l1cTraceIncomplete++;
        }
    }

    private static Path outputPath() {
        final String value = System.getProperty(OUTPUT_FILE_PROPERTY, "");
        return value.isBlank() ? null : Path.of(value);
    }

    private static synchronized void write() {
        if (!ENABLED) {
            return;
        }
        final List<String> lines = new ArrayList<>();
        lines.add("version=FRL_02L1_ORDER_AUDIT_V3");
        lines.add("raw.orderSimultaneousSa.total=" + total);
        lines.add("raw.orderSimultaneousSa.n0=" + n0);
        lines.add("raw.orderSimultaneousSa.n1=" + n1);
        lines.add("raw.orderSimultaneousSa.n2=" + n2);
        lines.add("raw.orderSimultaneousSa.n3=" + n3);
        lines.add("raw.orderSimultaneousSa.n4=" + n4);
        lines.add("raw.orderSimultaneousSa.nOther=" + nOther);
        lines.add("raw.rawMultiItemCallbacks=" + rawMultiItemCallbacks);
        lines.add("raw.nonL1MultiItemCallbacks=" + rawNonL1MultiItemCallbacks);
        lines.add("raw.outsideL1NativeFallbacks=" + rawOutsideL1NativeFallbacks);
        lines.add("l1.triggerSessions=" + l1Sessions);
        lines.add("l1.admittedSessions=" + l1AdmittedSessions);
        lines.add("l1.orderRequests=" + l1OrderRequests);
        lines.add("l1.candidateSize2=" + l1CandidateSize2);
        lines.add("l1.candidateSize3=" + l1CandidateSize3);
        lines.add("l1.candidateSize4=" + l1CandidateSize4);
        lines.add("l1.forced=" + l1ForcedRequests);
        lines.add("l1.unsupportedFallbacks=" + l1UnsupportedFallbacks);
        lines.add("l1.integrityFailures=" + l1IntegrityFailures);
        lines.add("l1.unsupportedFailures=" + l1UnsupportedFailures);
        lines.add("l1.invalidExternalCandidates=" + l1InvalidExternalCandidates);
        lines.add("l1.nativeCallbackFailures=" + l1NativeCallbackFailures);
        lines.add("l1.mappingFailures=" + l1MappingFailures);
        lines.add("l1.traceIncomplete=" + l1TraceIncomplete);
        for (final Map.Entry<String, Long> entry : l1AdmissionRejections.entrySet()) {
            lines.add("l1.admissionReject." + entry.getKey() + "=" + entry.getValue());
        }
        lines.add("l1c.copySessions=" + l1cSessions);
        lines.add("l1c.admittedSessions=" + l1cAdmittedSessions);
        lines.add("l1c.inputSize2=" + l1cInputSize2);
        lines.add("l1c.inputSize3=" + l1cInputSize3);
        lines.add("l1c.inputSize4=" + l1cInputSize4);
        lines.add("l1c.inputSizeOther=" + l1cInputSizeOther);
        lines.add("l1c.orderRequests=" + l1cOrderRequests);
        lines.add("l1c.candidateSize2=" + l1cCandidateSize2);
        lines.add("l1c.candidateSize3=" + l1cCandidateSize3);
        lines.add("l1c.candidateSize4=" + l1cCandidateSize4);
        lines.add("l1c.candidateSizeOther=" + l1cCandidateSizeOther);
        lines.add("l1c.forced=" + l1cForcedRequests);
        lines.add("l1c.nativeTeacherCallbacks=" + l1cNativeTeacherCallbacks);
        lines.add("l1c.integrityFailures=" + l1cIntegrityFailures);
        lines.add("l1c.unsupportedFailures=" + l1cUnsupportedFailures);
        lines.add("l1c.invalidExternalCandidates=" + l1cInvalidExternalCandidates);
        lines.add("l1c.nativeCallbackFailures=" + l1cNativeCallbackFailures);
        lines.add("l1c.mappingFailures=" + l1cMappingFailures);
        lines.add("l1c.traceIncomplete=" + l1cTraceIncomplete);
        try {
            final Path parent = OUTPUT.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(OUTPUT, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (final IOException ignored) {
            // Diagnostics must never change the engine callback or shutdown result.
        }
    }
}

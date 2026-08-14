package forge.game.decision;

import java.util.List;

/** Immutable training-contract view of one decision trace request. */
public final class DecisionTraceRequestRecord {
    /** Closed profile discriminator persisted by V3 requests. */
    public enum Profile {
        OTHER,
        SIMULTANEOUS_TRIGGER_ORDER,
        COPY_SPELL_RESOLVE_FIRST_ORDER,
        SURVEIL_PARTITION
    }

    private final long traceRequestIndex;
    private final int turn;
    private final String phase;
    private final int actingPlayerSeat;
    private final DecisionType decisionType;
    private final String adapterOrStage;
    private final int decisionStepIndex;
    private final boolean forced;
    private final List<String> legalCandidates;
    private final String candidateSetHash;
    private final Profile profile;
    private final DecisionTraceTeacherLabelEligibility teacherLabelEligibility;

    DecisionTraceRequestRecord(final long traceRequestIndex, final int turn, final String phase,
            final int actingPlayerSeat, final DecisionType decisionType, final String adapterOrStage,
            final int decisionStepIndex, final boolean forced, final List<String> legalCandidates,
            final String candidateSetHash) {
        this(traceRequestIndex, turn, phase, actingPlayerSeat, decisionType, adapterOrStage,
                decisionStepIndex, forced, legalCandidates, candidateSetHash,
                inferProfile(decisionType, adapterOrStage),
                inferEligibility(decisionType, adapterOrStage));
    }

    DecisionTraceRequestRecord(final long traceRequestIndex, final int turn, final String phase,
            final int actingPlayerSeat, final DecisionType decisionType, final String adapterOrStage,
            final int decisionStepIndex, final boolean forced, final List<String> legalCandidates,
            final String candidateSetHash, final Profile profile,
            final DecisionTraceTeacherLabelEligibility teacherLabelEligibility) {
        this.traceRequestIndex = traceRequestIndex;
        this.turn = turn;
        this.phase = phase;
        this.actingPlayerSeat = actingPlayerSeat;
        this.decisionType = decisionType;
        this.adapterOrStage = adapterOrStage;
        this.decisionStepIndex = decisionStepIndex;
        this.forced = forced;
        this.legalCandidates = List.copyOf(legalCandidates);
        this.candidateSetHash = candidateSetHash;
        this.profile = profile;
        this.teacherLabelEligibility = teacherLabelEligibility;
    }

    public long getTraceRequestIndex() {
        return traceRequestIndex;
    }

    public int getTurn() {
        return turn;
    }

    public String getPhase() {
        return phase;
    }

    public int getActingPlayerSeat() {
        return actingPlayerSeat;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public String getAdapterOrStage() {
        return adapterOrStage;
    }

    public int getDecisionStepIndex() {
        return decisionStepIndex;
    }

    public boolean isForced() {
        return forced;
    }

    public List<String> getLegalCandidates() {
        return legalCandidates;
    }

    public String getCandidateSetHash() {
        return candidateSetHash;
    }

    public Profile getProfile() {
        return profile;
    }

    public DecisionTraceTeacherLabelEligibility getTeacherLabelEligibility() {
        return teacherLabelEligibility;
    }

    boolean isCopySpellResolveFirstOrderRequest() {
        return profile == Profile.COPY_SPELL_RESOLVE_FIRST_ORDER
                || "COPY_SPELL_RESOLVE_FIRST_ORDER".equals(adapterOrStage);
    }

    boolean hasExactCopySpellResolveFirstOrderMetadata() {
        return profile == Profile.COPY_SPELL_RESOLVE_FIRST_ORDER
                && "COPY_SPELL_RESOLVE_FIRST_ORDER".equals(adapterOrStage);
    }

    boolean isSimultaneousTriggerOrderRequest() {
        return profile == Profile.SIMULTANEOUS_TRIGGER_ORDER
                || "SIMULTANEOUS_TRIGGER_ORDER".equals(adapterOrStage);
    }

    boolean isSurveilPartitionRequest() {
        return decisionType == DecisionType.CARD_SELECTION
                && profile == Profile.SURVEIL_PARTITION
                && "SURVEIL_PARTITION".equals(adapterOrStage);
    }

    /** Parses a persisted REQUEST line, preserving malformed V3 metadata as null for fail-closed checks. */
    public static DecisionTraceRequestRecord fromSerializedRequest(final String serialized) {
        if (serialized == null) {
            throw new IllegalArgumentException("Request trace record must not be null");
        }
        final String[] fields = serialized.split("\\|", -1);
        if (fields.length < 2 || !"REQUEST".equals(fields[1])) {
            throw new IllegalArgumentException("Malformed decision trace request record");
        }
        final String version = fields[0];
        if (!DeterminismTrace.DECISION_TRACE_VERSION.equals(version)
                && !DeterminismTrace.DECISION_TRACE_V3.equals(version)) {
            throw new IllegalArgumentException("Unknown decision trace version: " + version);
        }
        if (DeterminismTrace.DECISION_TRACE_VERSION.equals(version) && fields.length != 12
                || DeterminismTrace.DECISION_TRACE_V3.equals(version)
                        && (fields.length < 12 || fields.length > 14)) {
            throw new IllegalArgumentException("Malformed decision trace request field count");
        }
        final DecisionType decisionType;
        try {
            decisionType = DecisionType.valueOf(fields[6]);
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Malformed decision type", ex);
        }
        final Profile profile;
        final DecisionTraceTeacherLabelEligibility eligibility;
        if (DeterminismTrace.DECISION_TRACE_VERSION.equals(version)) {
            profile = inferProfile(decisionType, decode(fields[7]));
            eligibility = inferEligibility(decisionType, decode(fields[7]));
        } else {
            profile = parseProfile(fields.length > 12 ? fields[12] : "");
            eligibility = parseEligibility(fields.length > 13 ? fields[13] : "");
        }
        return new DecisionTraceRequestRecord(parseLong(fields[2]), parseInt(fields[3]), decode(fields[4]),
                parseInt(fields[5]), decisionType, decode(fields[7]), parseInt(fields[8]),
                parseBoolean(fields[9]), parseCandidateList(fields[10]), fields[11], profile, eligibility);
    }

    private static Profile inferProfile(final DecisionType decisionType, final String adapterOrStage) {
        return decisionType == DecisionType.ORDER
                && "SIMULTANEOUS_TRIGGER_ORDER".equals(adapterOrStage)
                ? Profile.SIMULTANEOUS_TRIGGER_ORDER : Profile.OTHER;
    }

    private static DecisionTraceTeacherLabelEligibility inferEligibility(final DecisionType decisionType,
            final String adapterOrStage) {
        return decisionType == DecisionType.ORDER
                && "SIMULTANEOUS_TRIGGER_ORDER".equals(adapterOrStage)
                ? DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE
                : DecisionTraceTeacherLabelEligibility.NOT_APPLICABLE;
    }

    private static Profile parseProfile(final String value) {
        try {
            return value.isEmpty() ? null : Profile.valueOf(decode(value));
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    private static DecisionTraceTeacherLabelEligibility parseEligibility(final String value) {
        try {
            return value.isEmpty() ? null : DecisionTraceTeacherLabelEligibility.valueOf(decode(value));
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    private static List<String> parseCandidateList(final String value) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IllegalArgumentException("Malformed candidate list");
        }
        final String body = value.substring(1, value.length() - 1);
        if (body.isEmpty()) {
            return List.of();
        }
        final List<String> candidates = new java.util.ArrayList<>();
        for (final String candidate : body.split(",", -1)) {
            candidates.add(decodeList(candidate));
        }
        return candidates;
    }

    private static String decodeList(final String value) {
        return decode(value).replace("%2C", ",").replace("%5B", "[").replace("%5D", "]");
    }

    private static String decode(final String value) {
        return value.replace("%0D", "\r").replace("%0A", "\n").replace("%7C", "|")
                .replace("%2C", ",").replace("%5B", "[").replace("%5D", "]")
                .replace("%25", "%");
    }

    private static long parseLong(final String value) {
        try {
            return Long.parseLong(value);
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Malformed long value", ex);
        }
    }

    private static int parseInt(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Malformed integer value", ex);
        }
    }

    private static boolean parseBoolean(final String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("Malformed boolean value");
        }
        return Boolean.parseBoolean(value);
    }
}

package forge.game.decision;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEvent;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.util.DeterminismAuditRandom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Opt-in per-game canonical trace collector for FRL determinism audits. */
public final class DeterminismTrace {
    public static final String OUTPUT_DIRECTORY_PROPERTY = "forge.determinism.traceDir";
    public static final String AUDIT_RANDOM_PROPERTY = "forge.determinism.auditRandom";
    public static final String DECISION_TRACE_VERSION = "DECISION_TRACE_V2";
    public static final String GAMEPLAY_TRACE_VERSION = "GAMEPLAY_TRACE_V1";
    public static final String RNG_TRACE_VERSION = "RNG_TRACE_V1";

    private static final Map<Game, DeterminismTrace> ACTIVE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Game game;
    private final int gameIndex;
    private final DeterminismAuditRandom random;
    private final Path outputDirectory;
    private final long rngStartIndex;
    private final List<String> gameplayRecords = new ArrayList<>();
    private final List<String> decisionRecords = new ArrayList<>();
    private final List<DecisionTraceRequestRecord> decisionRequests = new ArrayList<>();
    private final List<DecisionTraceResultRecord> decisionResults = new ArrayList<>();
    private final Map<Long, RequestHandle> openRequests = new LinkedHashMap<>();
    private long nextRequestIndex;
    private boolean finished;

    private DeterminismTrace(final Game game, final int gameIndex, final DeterminismAuditRandom random,
            final Path outputDirectory, final long rngStartIndex) throws IOException {
        this.game = game;
        this.gameIndex = gameIndex;
        this.random = random;
        this.outputDirectory = outputDirectory;
        this.rngStartIndex = rngStartIndex;
        Files.createDirectories(outputDirectory);
        ACTIVE.put(game, this);
        game.subscribeToEvents(this);
        recordGameplayCheckpoint("ATTACH");
    }

    public static DeterminismTrace attach(final Game game, final int gameIndex,
            final DeterminismAuditRandom random, final Path outputDirectory) throws IOException {
        return attach(game, gameIndex, random, outputDirectory, random.getDrawCount());
    }

    public static DeterminismTrace attach(final Game game, final int gameIndex,
            final DeterminismAuditRandom random, final Path outputDirectory,
            final long rngStartIndex) throws IOException {
        return new DeterminismTrace(game, gameIndex, random, outputDirectory, rngStartIndex);
    }

    /** Records a V2 request without inferring or fabricating a selected candidate. */
    public static RequestHandle recordRequest(final Game game, final int actingPlayerSeat,
            final DecisionRequest request, final String adapterOrStage, final int decisionStepIndex) {
        final DeterminismTrace trace = ACTIVE.get(game);
        return trace == null ? RequestHandle.inactive()
                : trace.addRequest(actingPlayerSeat, request, adapterOrStage, decisionStepIndex);
    }

    @Subscribe
    public void receive(final GameEvent event) {
        recordGameplayCheckpoint("EVENT:" + event.getClass().getSimpleName());
    }

    public synchronized void recordGameplayCheckpoint(final String trigger) {
        if (finished) {
            return;
        }
        gameplayRecords.add(GAMEPLAY_TRACE_VERSION + '|' + gameplayRecords.size() + '|'
                + canonicalText(trigger) + '|' + ForgeStateFingerprint.canonical(game));
    }

    public synchronized void finish() throws IOException {
        if (finished) {
            return;
        }
        recordGameplayCheckpoint("FINAL");
        for (final RequestHandle request : List.copyOf(openRequests.values())) {
            request.recordTraceIncomplete();
        }
        DecisionTraceTrainingValidator.validateRecords(decisionRequests, decisionResults);
        final long rngEndIndex = random.getDrawCount();
        final List<String> rngRecords = random.getCanonicalRecords(rngStartIndex, rngEndIndex);
        final List<String> rngDiagnosticRecords = random.getDiagnosticRecords(rngStartIndex, rngEndIndex);
        final String prefix = String.format(Locale.ROOT, "game-%03d", gameIndex + 1);
        write(prefix + ".gameplay.trace", gameplayRecords);
        if (!decisionRecords.isEmpty()) {
            write(prefix + ".decision.trace", decisionRecords);
        }
        write(prefix + ".rng.trace", rngRecords);
        write(prefix + ".rng-diagnostic.trace", rngDiagnosticRecords);
        write(prefix + ".summary.properties", List.of(
                "gameplayTraceVersion=" + GAMEPLAY_TRACE_VERSION,
                "gameplayHash=" + DeterminismTraceHasher.sha256(gameplayRecords),
                "decisionTraceVersion=" + DECISION_TRACE_VERSION,
                "decisionHash=" + (decisionRecords.isEmpty() ? "ABSENT"
                        : DeterminismTraceHasher.sha256(decisionRecords)),
                "rngTraceVersion=" + RNG_TRACE_VERSION,
                "rngHash=" + DeterminismTraceHasher.sha256(rngRecords),
                "rngDrawStart=" + rngStartIndex,
                "rngDrawEnd=" + rngEndIndex,
                "rngDrawCount=" + (rngEndIndex - rngStartIndex),
                "outcome=" + outcome()));
        finished = true;
        ACTIVE.remove(game);
    }

    private synchronized RequestHandle addRequest(final int actingPlayerSeat, final DecisionRequest request,
            final String adapterOrStage, final int decisionStepIndex) {
        if (finished) {
            return RequestHandle.inactive();
        }
        final PhaseHandler phase = game.getPhaseHandler();
        final List<String> legalCandidates = request.getCandidates().stream()
                .map(LegalCandidate::getSemanticKey).toList();
        final String candidateList = canonicalCandidateList(legalCandidates);
        final String candidateSetHash = DeterminismTraceHasher.sha256(
                List.of("DECISION_CANDIDATE_SET_V1|" + candidateList));
        final DecisionTraceRequestRecord record = new DecisionTraceRequestRecord(nextRequestIndex++,
                phase.getTurn(), String.valueOf(phase.getPhase()), actingPlayerSeat, request.getDecisionType(),
                adapterOrStage, decisionStepIndex, request.isForced(), legalCandidates, candidateSetHash);
        decisionRequests.add(record);
        decisionRecords.add(String.join("|", DECISION_TRACE_VERSION, "REQUEST",
                Long.toString(record.getTraceRequestIndex()), Integer.toString(record.getTurn()),
                canonicalText(record.getPhase()), Integer.toString(record.getActingPlayerSeat()),
                record.getDecisionType().name(), canonicalText(record.getAdapterOrStage()),
                Integer.toString(record.getDecisionStepIndex()), Boolean.toString(record.isForced()),
                candidateList, record.getCandidateSetHash()));
        final RequestHandle handle = new RequestHandle(this, record);
        openRequests.put(record.getTraceRequestIndex(), handle);
        return handle;
    }

    private synchronized void complete(final RequestHandle handle, final DecisionTraceResultKind kind,
            final String selectedCandidateSemanticKey, final boolean nativeCallbackCompleted,
            final boolean mappingAttempted, final boolean engineRollbackObserved,
            final boolean engineForcedBypass, final boolean traceFinalization) {
        if (handle.trace != this || !openRequests.containsKey(handle.requestRecord.getTraceRequestIndex())) {
            throw new IllegalStateException("Decision trace request already has a terminal result");
        }
        final DecisionTraceResultRecord result = new DecisionTraceResultRecord(
                handle.requestRecord.getTraceRequestIndex(), kind, selectedCandidateSemanticKey,
                nativeCallbackCompleted, mappingAttempted, engineRollbackObserved,
                engineForcedBypass, traceFinalization);
        if (!DecisionTraceTrainingValidator.isHistoryValid(handle.requestRecord, result)) {
            throw new IllegalArgumentException("Invalid " + kind + " result for decision trace request "
                    + handle.requestRecord.getTraceRequestIndex());
        }
        openRequests.remove(handle.requestRecord.getTraceRequestIndex());
        handle.resultRecord = result;
        decisionResults.add(result);
        decisionRecords.add(String.join("|", DECISION_TRACE_VERSION, "RESULT",
                Long.toString(result.getTraceRequestIndex()), result.getKind().name(),
                canonicalText(result.getSelectedCandidateSemanticKey()),
                Boolean.toString(result.isNativeCallbackCompleted()),
                Boolean.toString(result.isMappingAttempted()),
                Boolean.toString(result.isEngineRollbackObserved()),
                Boolean.toString(result.isEngineForcedBypass()),
                Boolean.toString(result.isTraceFinalization())));
    }

    private String outcome() {
        if (!game.isGameOver() || game.getOutcome() == null) {
            return "IN_PROGRESS";
        }
        if (game.getOutcome().isDraw()) {
            return "DRAW";
        }
        final List<Integer> winningSeats = new ArrayList<>();
        for (final Player player : game.getRegisteredPlayers()) {
            if (player.getOutcome() != null && player.getOutcome().hasWon()) {
                winningSeats.add(player.getId());
            }
        }
        Collections.sort(winningSeats);
        if (winningSeats.size() == 1) {
            return "WINNER_SEAT_" + winningSeats.get(0);
        }
        if (winningSeats.isEmpty()) {
            return "MAPPING_FAILED";
        }
        return "INVALID_WINNER_SEATS_" + winningSeats.toString().replace(" ", "");
    }

    private void write(final String fileName, final List<String> records) throws IOException {
        Files.write(outputDirectory.resolve(fileName), records, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static String canonicalText(final Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace("%", "%25").replace("|", "%7C")
                .replace("\r", "%0D").replace("\n", "%0A");
    }

    private static String canonicalCandidateList(final List<String> candidates) {
        return candidates.stream().map(DeterminismTrace::canonicalListText)
                .reduce((left, right) -> left + "," + right).map(value -> '[' + value + ']')
                .orElse("[]");
    }

    private static String canonicalListText(final String value) {
        return canonicalText(value).replace(",", "%2C").replace("[", "%5B").replace("]", "%5D");
    }

    /** Trace-local lifecycle handle; an inactive handle is a no-op when no collector is attached. */
    public static final class RequestHandle {
        private final DeterminismTrace trace;
        private final DecisionTraceRequestRecord requestRecord;
        private DecisionTraceResultRecord resultRecord;

        private RequestHandle(final DeterminismTrace trace, final DecisionTraceRequestRecord requestRecord) {
            this.trace = trace;
            this.requestRecord = requestRecord;
        }

        private static RequestHandle inactive() {
            return new RequestHandle(null, null);
        }

        public boolean isActive() {
            return trace != null;
        }

        public DecisionTraceRequestRecord getRequestRecord() {
            if (!isActive()) {
                throw new IllegalStateException("No DeterminismTrace is attached");
            }
            return requestRecord;
        }

        public Optional<DecisionTraceResultRecord> getResultRecord() {
            return Optional.ofNullable(resultRecord);
        }

        /** Closes after a native result is observed and maps to this request. */
        public void recordMappedResult(final LegalCandidate selectedCandidate) {
            if (!isActive()) {
                return;
            }
            final String selected = selectedCandidate == null ? "" : selectedCandidate.getSemanticKey();
            if (!requestRecord.getLegalCandidates().contains(selected)) {
                throw new IllegalArgumentException("Selected candidate is not legal for request: " + selected);
            }
            trace.complete(this, requestRecord.isForced() ? DecisionTraceResultKind.FORCED
                    : DecisionTraceResultKind.CHOSEN, selected, true, true, false, false, false);
        }

        /** Closes when Forge proves a sole-candidate decision without invoking a native callback. */
        public void recordEngineForced() {
            if (!isActive()) {
                return;
            }
            if (!requestRecord.isForced()) {
                throw new IllegalArgumentException("Engine-forced completion requires exactly one candidate");
            }
            trace.complete(this, DecisionTraceResultKind.FORCED, requestRecord.getLegalCandidates().get(0),
                    false, false, false, true, false);
        }

        /** Closes after a normal native callback for which no trustworthy mapping seam exists. */
        public void recordUnobserved() {
            if (isActive()) {
                trace.complete(this, DecisionTraceResultKind.UNOBSERVED, "", true, false,
                        false, false, false);
            }
        }

        /** Closes after an observed native result was actually mapped and no candidate matched. */
        public void recordMappingFailed() {
            if (isActive()) {
                trace.complete(this, DecisionTraceResultKind.MAPPING_FAILED, "", true, true,
                        false, false, false);
            }
        }

        /** Closes only at a Forge seam that authoritatively reports engine rollback. */
        public void recordEngineRollback() {
            if (isActive()) {
                trace.complete(this, DecisionTraceResultKind.ENGINE_ROLLBACK, "", false, false,
                        true, false, false);
            }
        }

        private void recordTraceIncomplete() {
            trace.complete(this, DecisionTraceResultKind.TRACE_INCOMPLETE, "", false, false,
                    false, false, true);
        }
    }
}

package forge.game.decision;

import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Diagnostic-only observation around the existing AI mulligan callbacks. */
public final class MulliganDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.mulligan.metricsFile";

    private static final String OUTPUT_PATH = System.getProperty(OUTPUT_PATH_PROPERTY, "");
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final String HEADER = "event_type,process_id,game_id,mulligan_session_id,mulligan_round_index,"
            + "mulligan_step_index,acting_player,starting_player,cards_to_return,hand_size,candidate_count,forced,"
            + "selected_action,status,reason,generation_ns,native_callback_ns,selection_adapter,"
            + "selection_session_id,selection_step_index,selected_count,remaining_count,initial_count";
    private static final MulliganDiagnostics GLOBAL = new MulliganDiagnostics(!OUTPUT_PATH.isBlank(), true);

    private final boolean enabled;
    private final boolean writeAtShutdown;
    private final MulliganDecisionProvider mulliganProvider;
    private final MulliganBottomAdapter bottomAdapter;
    private final List<String> events = new ArrayList<>();

    public MulliganDiagnostics(final boolean enabled) {
        this(enabled, false, new MulliganDecisionProvider(), new MulliganBottomAdapter());
    }

    private MulliganDiagnostics(final boolean enabled, final boolean writeAtShutdown) {
        this(enabled, writeAtShutdown, new MulliganDecisionProvider(), new MulliganBottomAdapter());
    }

    MulliganDiagnostics(final boolean enabled, final MulliganDecisionProvider mulliganProvider,
            final MulliganBottomAdapter bottomAdapter) {
        this(enabled, false, mulliganProvider, bottomAdapter);
    }

    private MulliganDiagnostics(final boolean enabled, final boolean writeAtShutdown,
            final MulliganDecisionProvider mulliganProvider, final MulliganBottomAdapter bottomAdapter) {
        this.enabled = enabled;
        this.writeAtShutdown = writeAtShutdown;
        this.mulliganProvider = mulliganProvider;
        this.bottomAdapter = bottomAdapter;
        if (enabled && writeAtShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::writeCsv, "forge-mulligan-diagnostics"));
        }
    }

    public static MulliganDiagnostics global() {
        return GLOBAL;
    }

    public void endMulliganProcess(final Game game) {
        if (enabled && game != null) {
            mulliganProvider.endGame(game);
        }
    }

    public long startNativeCallback() {
        return enabled ? System.nanoTime() : 0L;
    }

    public KeepCapture captureKeepOrRedraw(final Player actingPlayer, final Player startingPlayer,
            final int cardsToReturn) {
        if (!enabled) {
            return null;
        }
        final int gameId = actingPlayer.getGame().getId();
        final int handSize = actingPlayer.getCardsIn(ZoneType.Hand).size();
        try {
            final MulliganDecisionProvider.SessionStart start = mulliganProvider.beginCallback(actingPlayer,
                    startingPlayer, new CardCollection(actingPlayer.getCardsIn(ZoneType.Hand)), cardsToReturn);
            return new KeepCapture(start, actingPlayer, startingPlayer, cardsToReturn, gameId, handSize, null);
        } catch (final RuntimeException ex) {
            return new KeepCapture(null, actingPlayer, startingPlayer, cardsToReturn, gameId, handSize,
                    ex.getClass().getSimpleName());
        }
    }

    public boolean recordKeepOrRedraw(final KeepCapture capture, final boolean nativeKeep,
            final long nativeCallbackStartedAtNanos) {
        if (!enabled || capture == null) {
            return nativeKeep;
        }
        final long nativeCallbackNanos = elapsed(nativeCallbackStartedAtNanos);
        try {
            if (capture.start == null || capture.start.getStatus() != MulliganDecisionProvider.Status.READY) {
                final String reason = capture.captureReason != null ? capture.captureReason
                        : capture.start == null ? "NO_SESSION" : capture.start.getStatus().name();
                emit("MULLIGAN_CALLBACK", capture.gameId, -1L, -1, -1, capture.actingPlayer.getId(),
                        capture.startingPlayer.getId(), capture.cardsToReturn, capture.handSize, 0, false, "",
                        "UNSUPPORTED", reason, 0L, nativeCallbackNanos, "", -1L, -1, 0, 0, 0);
                emit("MULLIGAN_STATE", capture.gameId, -1L, -1, -1, capture.actingPlayer.getId(),
                        capture.startingPlayer.getId(), capture.cardsToReturn, capture.handSize, 0, false, "",
                        "UNSUPPORTED", reason, 0L, nativeCallbackNanos, "", -1L, -1, 0, 0, 0);
                return nativeKeep;
            }

            final MulliganDecisionProvider.Generation generation = mulliganProvider.generateNext(
                    capture.start.getSession());
            final DecisionRequest request = generation.getRequest();
            final MulliganContext context = request == null ? null : request.getMulliganContext();
            emit("MULLIGAN_CALLBACK", context, generation.getRequest() == null ? 0
                    : generation.getRequest().getCandidates().size(), false, "", generation.getStatus().name(),
                    null, generation.getGenerationNanos(), nativeCallbackNanos, "", -1L, -1, 0, 0, 0);
            if (generation.getStatus() != MulliganDecisionProvider.Status.DECISION || request == null) {
                emit("MULLIGAN_STATE", context, 0, false, "", generation.getStatus().name(), "", 0L,
                        nativeCallbackNanos, "", -1L, -1, 0, 0, 0);
                return nativeKeep;
            }

            final MulliganCandidateKind selectedKind = nativeKeep ? MulliganCandidateKind.KEEP
                    : MulliganCandidateKind.REDRAW;
            final LegalCandidate selected = request.getCandidates().stream()
                    .filter(candidate -> candidate.getMulliganKind() == selectedKind)
                    .findFirst().orElse(null);
            if (selected == null) {
                traceDecision(capture.actingPlayer, DecisionType.MULLIGAN, "KEEP_OR_REDRAW",
                        context.getMulliganStepIndex(), request.isForced(), null);
                emit("MULLIGAN_STATE", context, request.getCandidates().size(), false, "",
                        "MAPPING_FAILED", "MULLIGAN_CANDIDATE_NOT_FOUND", 0L, nativeCallbackNanos, "", -1L,
                        -1, 0, 0, 0);
                return nativeKeep;
            }
            traceDecision(capture.actingPlayer, request, "KEEP_OR_REDRAW",
                    context.getMulliganStepIndex(), selected);
            final MulliganDecisionProvider.Generation applied = mulliganProvider.apply(request, selected);
            emit("MULLIGAN", context, request.getCandidates().size(), false, selected.getSemanticKey(),
                    applied.getStatus().name(), "", applied.getGenerationNanos(), nativeCallbackNanos, "", -1L,
                    -1, 0, 0, 0);
        } catch (final RuntimeException ex) {
            emit("MULLIGAN_STATE", capture.gameId, -1L, -1, -1, capture.actingPlayer.getId(),
                    capture.startingPlayer.getId(), capture.cardsToReturn, capture.handSize, 0, false, "",
                    "MAPPING_FAILED", ex.getClass().getSimpleName(), 0L, nativeCallbackNanos, "", -1L, -1,
                    0, 0, 0);
        }
        return nativeKeep;
    }

    public BottomCapture captureBottom(final Player actingPlayer, final CardCollectionView callbackHand,
            final int cardsToReturn) {
        if (!enabled) {
            return null;
        }
        final MulliganSession parent = mulliganProvider.currentSession(actingPlayer);
        final int callbackHandSize = callbackHand == null ? 0 : callbackHand.size();
        try {
            final MulliganBottomAdapter.Capture capture = bottomAdapter.begin(actingPlayer, callbackHand,
                    cardsToReturn);
            return new BottomCapture(capture, parent, actingPlayer, cardsToReturn, callbackHandSize, null);
        } catch (final RuntimeException ex) {
            return new BottomCapture(null, parent, actingPlayer, cardsToReturn, callbackHandSize,
                    ex.getClass().getSimpleName());
        }
    }

    public CardCollectionView recordBottom(final BottomCapture capture, final CardCollectionView nativeResult,
            final long nativeCallbackStartedAtNanos) {
        if (!enabled || capture == null) {
            return nativeResult;
        }
        final long nativeCallbackNanos = elapsed(nativeCallbackStartedAtNanos);
        try {
            emit("CARD_SELECTION_CALLBACK", parentGameId(capture), parentSessionId(capture), parentRound(capture),
                    parentStep(capture), capture.actingPlayer.getId(), parentStartingPlayerId(capture),
                    capture.cardsToReturn, capture.callbackHandSize, 0, false, "", "CALLBACK", "", 0L,
                    nativeCallbackNanos, CardSelectionAdapter.MULLIGAN_BOTTOM.name(),
                    capture.capture == null ? -1L : capture.capture.getSelectionSessionId(), -1, 0, 0,
                    capture.capture == null ? 0 : capture.capture.getInitialHandSize());
            if (capture.capture == null || capture.capture.getStatus() != MulliganBottomAdapter.Status.SUPPORTED) {
                final String reason = capture.captureReason != null ? capture.captureReason
                        : capture.capture == null ? "NO_CAPTURE" : capture.capture.getStatus().name();
                emitBottomState(capture, "UNSUPPORTED", reason, nativeCallbackNanos);
                return nativeResult;
            }
            final MulliganBottomAdapter.Replay replay = bottomAdapter.replay(capture.capture, nativeResult);
            for (final MulliganBottomAdapter.ReplayStep step : replay.getSteps()) {
                final DecisionRequest request = step.getRequest();
                final CardSelectionContext context = request.getCardSelectionContext();
                traceDecision(capture.actingPlayer, request, context.getSelectionAdapter().name(),
                        context.getSelectionStepIndex(), step.getCandidate());
                emit("CARD_SELECTION", parentGameId(capture), parentSessionId(capture), parentRound(capture),
                        parentStep(capture), capture.actingPlayer.getId(), parentStartingPlayerId(capture),
                        capture.cardsToReturn, context.getVisibleCards().size(), request.getCandidates().size(),
                        request.isForced(), "", "DECISION", "", step.getGenerationNanos(), nativeCallbackNanos,
                        context.getSelectionAdapter().name(), context.getSelectionSessionId(),
                        context.getSelectionStepIndex(), context.getSelectedCards().size(),
                        request.getCandidates().size(), context.getVisibleCards().size());
            }
            if (replay.getStatus() != MulliganBottomAdapter.ReplayStatus.COMPLETE) {
                emitBottomState(capture, replay.getStatus().name(), replay.getReason(), nativeCallbackNanos);
            }
        } catch (final RuntimeException ex) {
            emitBottomState(capture, "MAPPING_FAILED", ex.getClass().getSimpleName(), nativeCallbackNanos);
        }
        return nativeResult;
    }

    public void recordForcedKeep(final Player actingPlayer, final Player startingPlayer, final int cardsToReturn) {
        if (!enabled) {
            return;
        }
        traceDecision(actingPlayer, DecisionType.MULLIGAN, "KEEP_OR_REDRAW", -1, true,
                MulliganCandidateKind.KEEP.semanticKey());
        emit("MULLIGAN_CALLBACK", actingPlayer.getGame().getId(), -1L, -1, -1, actingPlayer.getId(),
                startingPlayer.getId(), cardsToReturn, actingPlayer.getCardsIn(ZoneType.Hand).size(), 0, true,
                MulliganCandidateKind.KEEP.semanticKey(), "FORCED_KEEP", "CAN_MULLIGAN_FALSE", 0L, 0L, "", -1L,
                -1, 0, 0, 0);
    }

    List<String> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    private static void traceDecision(final Player actingPlayer, final DecisionRequest request,
            final String adapterOrStage, final int decisionStepIndex, final LegalCandidate selectedCandidate) {
        traceDecision(actingPlayer, request.getDecisionType(), adapterOrStage, decisionStepIndex,
                request.isForced(), selectedCandidate == null ? "MAPPING_FAILED" : selectedCandidate.getSemanticKey());
    }

    private static void traceDecision(final Player actingPlayer, final DecisionType decisionType,
            final String adapterOrStage, final int decisionStepIndex, final boolean forced,
            final String selectedSemanticKey) {
        DeterminismTrace.recordDecision(actingPlayer.getGame(), actingPlayer.getId(), decisionType,
                adapterOrStage, decisionStepIndex, forced, selectedSemanticKey);
    }

    private void emitBottomState(final BottomCapture capture, final String status, final String reason,
            final long nativeCallbackNanos) {
        emit("MULLIGAN_STATE", parentGameId(capture), parentSessionId(capture), parentRound(capture),
                parentStep(capture), capture.actingPlayer.getId(), parentStartingPlayerId(capture),
                capture.cardsToReturn, capture.actingPlayer.getCardsIn(ZoneType.Hand).size(), 0, false, "", status,
                reason, 0L, nativeCallbackNanos, CardSelectionAdapter.MULLIGAN_BOTTOM.name(),
                capture.capture == null ? -1L : capture.capture.getSelectionSessionId(), -1, 0, 0,
                capture.capture == null ? 0 : capture.capture.getInitialHandSize());
    }

    private void emit(final String eventType, final MulliganContext context, final int candidateCount,
            final boolean forced, final String selectedAction, final String status, final String reason,
            final long generationNanos, final long nativeCallbackNanos, final String selectionAdapter,
            final long selectionSessionId, final int selectionStepIndex, final int selectedCount,
            final int remainingCount, final int initialCount) {
        if (context == null) {
            return;
        }
        emit(eventType, context.getGameId(), context.getMulliganSessionId(), context.getMulliganRoundIndex(),
                context.getMulliganStepIndex(), context.getActingPlayerId(), context.getStartingPlayerId(),
                context.getCardsToReturn(), context.getHandSize(), candidateCount, forced, selectedAction, status,
                reason, generationNanos, nativeCallbackNanos, selectionAdapter, selectionSessionId,
                selectionStepIndex, selectedCount, remainingCount, initialCount);
    }

    private void emit(final String eventType, final int gameId, final long sessionId, final int roundIndex,
            final int stepIndex, final int actingPlayerId, final int startingPlayerId, final int cardsToReturn,
            final int handSize, final int candidateCount, final boolean forced, final String selectedAction,
            final String status, final String reason, final long generationNanos, final long nativeCallbackNanos,
            final String selectionAdapter, final long selectionSessionId, final int selectionStepIndex,
            final int selectedCount, final int remainingCount, final int initialCount) {
        synchronized (events) {
            events.add(String.join(",", csv(eventType), Long.toString(PROCESS_ID), Integer.toString(gameId),
                    Long.toString(sessionId), Integer.toString(roundIndex), Integer.toString(stepIndex),
                    Integer.toString(actingPlayerId), Integer.toString(startingPlayerId),
                    Integer.toString(cardsToReturn), Integer.toString(handSize), Integer.toString(candidateCount),
                    Boolean.toString(forced), csv(selectedAction), csv(status), csv(reason),
                    Long.toString(generationNanos), Long.toString(nativeCallbackNanos), csv(selectionAdapter),
                    Long.toString(selectionSessionId), Integer.toString(selectionStepIndex),
                    Integer.toString(selectedCount), Integer.toString(remainingCount), Integer.toString(initialCount)));
        }
    }

    private void writeCsv() {
        if (!enabled || !writeAtShutdown || OUTPUT_PATH.isBlank()) {
            return;
        }
        try {
            final Path path = Paths.get(OUTPUT_PATH);
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final List<String> rows = new ArrayList<>();
            rows.add(HEADER);
            rows.addAll(events());
            Files.write(path, rows, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (final IOException ex) {
            System.err.println("Unable to write Forge mulligan diagnostics: " + ex.getMessage());
        }
    }

    private static String csv(final String value) {
        final String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static long elapsed(final long startedAtNanos) {
        return startedAtNanos == 0L ? 0L : System.nanoTime() - startedAtNanos;
    }

    private static int parentGameId(final BottomCapture capture) {
        return capture.parent == null ? capture.actingPlayer.getGame().getId() : capture.parent.getGameId();
    }

    private static long parentSessionId(final BottomCapture capture) {
        return capture.parent == null ? -1L : capture.parent.getMulliganSessionId();
    }

    private static int parentRound(final BottomCapture capture) {
        return capture.parent == null || capture.parent.getContext() == null ? -1
                : capture.parent.getContext().getMulliganRoundIndex();
    }

    private static int parentStep(final BottomCapture capture) {
        return capture.parent == null || capture.parent.getContext() == null ? -1
                : capture.parent.getContext().getMulliganStepIndex();
    }

    private static int parentStartingPlayerId(final BottomCapture capture) {
        return capture.parent == null ? -1 : capture.parent.getStartingPlayerId();
    }

    public static final class KeepCapture {
        private final MulliganDecisionProvider.SessionStart start;
        private final Player actingPlayer;
        private final Player startingPlayer;
        private final int cardsToReturn;
        private final int gameId;
        private final int handSize;
        private final String captureReason;

        private KeepCapture(final MulliganDecisionProvider.SessionStart start, final Player actingPlayer,
                final Player startingPlayer, final int cardsToReturn, final int gameId, final int handSize,
                final String captureReason) {
            this.start = start;
            this.actingPlayer = actingPlayer;
            this.startingPlayer = startingPlayer;
            this.cardsToReturn = cardsToReturn;
            this.gameId = gameId;
            this.handSize = handSize;
            this.captureReason = captureReason;
        }
    }

    public static final class BottomCapture {
        private final MulliganBottomAdapter.Capture capture;
        private final MulliganSession parent;
        private final Player actingPlayer;
        private final int cardsToReturn;
        private final int callbackHandSize;
        private final String captureReason;

        private BottomCapture(final MulliganBottomAdapter.Capture capture, final MulliganSession parent,
                final Player actingPlayer, final int cardsToReturn, final int callbackHandSize,
                final String captureReason) {
            this.capture = capture;
            this.parent = parent;
            this.actingPlayer = actingPlayer;
            this.cardsToReturn = cardsToReturn;
            this.callbackHandSize = callbackHandSize;
            this.captureReason = captureReason;
        }
    }
}

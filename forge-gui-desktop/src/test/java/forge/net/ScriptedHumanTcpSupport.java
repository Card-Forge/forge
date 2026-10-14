package forge.net;

import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.Game;
import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.game.zone.ZoneView;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputConfirmMulligan;
import forge.gamemodes.match.input.InputLondonMulligan;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.NetworkLogConfig;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.client.NetGameController;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.server.RemoteClient;
import forge.gui.FThreads;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.MessageUtil;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Test-only existing-TCP client and human-style decision/capture endpoint.
 * Captures immutable GUI callback arguments before this endpoint calls its superclass; the
 * network handler has already updated its trackers and replicated properties at this boundary.
 */
final class ScriptedHumanTcpSupport implements AutoCloseable {
    record PrivacyInputSnapshot(Input proxyInput, Input queuedInput) {
        String evidence() {
            return "proxy=" + describeInput(proxyInput) + ", queue=" + describeInput(queuedInput)
                    + ", proxyIsInputConfirm=" + (proxyInput instanceof InputConfirm)
                    + ", proxyIsQueueTop=" + (proxyInput != null && proxyInput == queuedInput);
        }

        static String describeInput(Input input) {
            return input == null ? "null" : input.getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(input));
        }
    }
    private record PrivacyStackPriorityCheckpoint(Game game, ScriptedGui endpoint, GameView view,
            Object tracker, PlayerControllerHuman controller, IGameController endpointController,
            InputPassPriority input, int seat, int viewId, long callbackOrdinal, int turn,
            PhaseType phase, int turnPlayerViewId, SpellAbility ability, PrivacyPaymentReceipt payment) { }
    private record PrivacyPaymentBinding(SpellAbility ability, PrivacyPaymentReceipt receipt) { }

    private record PrivacyShuffleT4PriorityInput(Game game, PlayerControllerHuman controller,
            InputPassPriority input, int ownerViewId, int gameViewId, long callbackOrdinal,
            ScriptedGui endpoint) { }

    record OpeningKeepGateReceipt(int seat, int gameViewId, long promptOrdinal,
            long appliedStateLowerBoundOrdinal, long appliedCallbackOrdinal, long appliedSequence,
            Class<?> inputType, int handCount, int londonBottomCardId, boolean bottomInSharedLibrary,
            boolean sameGame, boolean sameTracker, int okDispatchCount) { }
    record ReadinessGateSummary(int openingKeepDeferrals, int openingKeepCompletions,
            int openingKeepOkDispatches, int fengrafPreSendRepaints, int t4RepeatedPriorityCallbacks) { }
    private record OpeningKeepAppliedWitness(long callbackOrdinal, long sequence, int gameViewId,
            Object trackerIdentity, List<Integer> handIds, boolean bottomInSharedLibrary) {
        OpeningKeepAppliedWitness { handIds = List.copyOf(handIds); }
    }
    private record PendingOpeningKeep(Game game, PlayerControllerHuman controller,
            IGameController endpointController, InputConfirmMulligan input, int gameViewId,
            int seat, int chooserViewId, Capture prompt,
            String oneLabel, String twoLabel, boolean oneEnabled, boolean twoEnabled,
            Decision decision, int expectedHandCount, int londonBottomCardId,
            long appliedStateLowerBoundOrdinal, AtomicBoolean dispatched) { }

    enum Decision { PLAY_OR_DRAW, START_SEAT_ZERO, MULLIGAN_ANY, FREE_MULLIGAN, REGULAR_MULLIGAN,
        KEEP, KEEP_WITHOUT_LAND, BOTTOM_LONDON, PLAY_LAND, CAST_METAMORPHOSE,
        CHOOSE_METAMORPHOSE_HAND, PAY_METAMORPHOSE, ADVANCE_METAMORPHOSE_UNTAP, ADVANCE_METAMORPHOSE_UPKEEP,
        PASS_METAMORPHOSE_STACK, PASS_PRIORITY, CONCEDE, CAST_TELLING_TIME, PAY_TELLING_TIME,
        ACTIVATE_FENGRAF, PAY_FENGRAF, PASS_PRIVACY_STACK, CAST_DAYS_UNDOING, PAY_DAYS_UNDOING,
        DISCARD_T2_CLEANUP }

    record ZoneFact(String field, Integer playerViewId, ZoneType zoneType) { }
    record EventFact(String eventClass, List<Integer> playerViewIds, List<Integer> cardViewIds,
                     List<Integer> spellAbilityViewIds, List<Integer> stackItemViewIds,
                     List<ZoneFact> zoneFacts, List<String> scalarFacts,
                     String targetDescription, String stackDescription) {
        EventFact {
            playerViewIds = List.copyOf(playerViewIds);
            cardViewIds = List.copyOf(cardViewIds);
            spellAbilityViewIds = List.copyOf(spellAbilityViewIds);
            stackItemViewIds = List.copyOf(stackItemViewIds);
            zoneFacts = List.copyOf(zoneFacts);
            scalarFacts = List.copyOf(scalarFacts);
        }
    }

    record PriorityReceipt(int seat, int chooserViewId, long callbackOrdinal, int turn,
                           PhaseType phase, int turnPlayerViewId, String one, String two,
                           boolean enabledOne, boolean enabledTwo) { }

    record ClientProtocolSendReceipt(long sendOrdinal, int eventId, ProtocolMethod method,
                                     Integer gameViewId, long promptCallbackOrdinal,
                                     NextGameDecision nextGameDecision) { }
    record ServerProtocolReceiveReceipt(long receiveOrdinal, int eventId, ProtocolMethod method,
                                        Integer gameViewId, int seat, Channel channel,
                                        NextGameDecision nextGameDecision) { }

    static final class StaleActionRejectionLog {
        private final File file;
        private volatile long baselineBytes;
        private final String startToken;

        private StaleActionRejectionLog(File file, long baselineBytes, String startToken) {
            this.file = file;
            this.baselineBytes = baselineBytes;
            this.startToken = startToken;
        }

        void markBeforeAction() {
            baselineBytes = file.length();
        }

        static StaleActionRejectionLog start(String suffix) throws Exception {
            NetworkLogConfig.setTestMode(true);
            NetworkLogConfig.setInstanceSuffix(suffix);
            String startToken = "staleActionWitnessStart=" + suffix;
            IHasForgeLog.netLog.info("{}", startToken);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            File file;
            do {
                file = NetworkLogConfig.getCurrentLogFile();
                if (file != null && file.isFile()) {
                    String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    if (contents.contains(startToken)) {
                        return new StaleActionRejectionLog(file, file.length(), startToken);
                    }
                }
                Thread.sleep(5);
            } while (System.nanoTime() < deadline);
            throw new AssertionError("NETWORK log file did not initialize with the stale-action witness marker");
        }

        String awaitRejected(int eventId, ProtocolMethod method, int seat, int suppliedGameViewId,
                             int currentGameViewId, String reason, String phase, long timeout,
                             TimeUnit unit) throws Exception {
            String expected = "eventId=" + eventId + " method=" + method + " slot=" + seat
                    + " suppliedGameViewId=" + suppliedGameViewId + " currentGameViewId=" + currentGameViewId
                    + " reason=" + reason + " phase=" + phase;
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            long readPosition = baselineBytes;
            StringBuilder pendingLine = new StringBuilder();
            while (System.nanoTime() < deadline) {
                long fileLength = file.length();
                if (fileLength - baselineBytes > 64 * 1024) {
                    throw new AssertionError("Stale-action rejection log scan exceeded its 64 KiB bound");
                }
                if (fileLength > readPosition) {
                    try (RandomAccessFile log = new RandomAccessFile(file, "r")) {
                        log.seek(readPosition);
                        int chunkSize = (int) Math.min(fileLength - readPosition, 4096);
                        byte[] chunk = new byte[chunkSize];
                        log.readFully(chunk);
                        readPosition += chunkSize;
                        pendingLine.append(new String(chunk, StandardCharsets.UTF_8));
                    }
                    int lineEnd;
                    while ((lineEnd = pendingLine.indexOf("\n")) >= 0) {
                        String line = pendingLine.substring(0, lineEnd);
                        pendingLine.delete(0, lineEnd + 1);
                        if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
                        if (line.contains("REJECTED_STALE_GAME_ACTION ") && line.contains(expected)) {
                            return line;
                        }
                    }
                    if (pendingLine.indexOf("REJECTED_STALE_GAME_ACTION ") >= 0 && pendingLine.indexOf(expected) >= 0) {
                        return pendingLine.toString();
                    }
                }
                Thread.sleep(5);
            }
            throw new TimeoutException("No exact stale-action rejection record appeared after " + startToken);
        }
    }

    private record ArmedClientSend(int gameViewId, long promptCallbackOrdinal,
                                   ProtocolMethod method, NextGameDecision nextGameDecision) { }

    private static final class ClientProtocolObservation {
        private final Scenario scenario;
        private final boolean privacyConfirmationOnly;
        private final boolean metamorphoseDiagnosticOnly;
        private final AtomicLong observedSendOrdinal = new AtomicLong();
        private volatile ArmedClientSend armed;
        private volatile boolean metamorphoseResponseArmed;
        private volatile boolean privacyHistoryNextGameActionArmed;
        private volatile ClientProtocolSendReceipt receipt;
        private volatile Throwable failure;

        private ClientProtocolObservation(Scenario scenario, boolean privacyConfirmationOnly,
                                          boolean metamorphoseDiagnosticOnly) {
            this.scenario = scenario;
            this.privacyConfirmationOnly = privacyConfirmationOnly;
            this.metamorphoseDiagnosticOnly = metamorphoseDiagnosticOnly;
        }

        boolean privacyConfirmationOnly() { return privacyConfirmationOnly;
        }
        boolean metamorphoseDiagnosticOnly() { return metamorphoseDiagnosticOnly;
        }

        synchronized void arm(ProtocolMethod method, NextGameDecision nextGameDecision,
                              int gameViewId, long promptCallbackOrdinal) {
            boolean validMethod = (method == ProtocolMethod.selectButtonOk
                    || method == ProtocolMethod.selectStackItem) && nextGameDecision == null
                    || method == ProtocolMethod.nextGameDecision && nextGameDecision == NextGameDecision.QUIT;
            if (armed != null || gameViewId < 0 || promptCallbackOrdinal <= 0 || !validMethod) {
                throw new AssertionError("Client protocol observation cannot be armed for this lifecycle event");
            }
            receipt = null;
            armed = new ArmedClientSend(gameViewId, promptCallbackOrdinal, method, nextGameDecision);
            metamorphoseResponseArmed = false;
        }

        synchronized void armSelectButtonOk(int gameViewId, long promptCallbackOrdinal) {
            arm(ProtocolMethod.selectButtonOk, null, gameViewId, promptCallbackOrdinal);
        }

        synchronized boolean armMetamorphoseResponse(ProtocolMethod method, int gameViewId,
                                                       long promptCallbackOrdinal) {
            if (!metamorphoseDiagnosticOnly || armed != null || gameViewId < 0 || promptCallbackOrdinal <= 0 || method != ProtocolMethod.selectButtonOk && method != ProtocolMethod.selectButtonCancel && method != ProtocolMethod.selectCard) return false;
            receipt = null;
            armed = new ArmedClientSend(gameViewId, promptCallbackOrdinal, method, null);
            metamorphoseResponseArmed = true;
            return true;
        }

        synchronized void observe(NetEvent networkEvent) {
            ArmedClientSend expected = armed;
            if (expected == null || !(networkEvent instanceof GuiGameEvent event) || event.getMethod() != expected.method()) return;
            if (privacyHistoryNextGameActionArmed) {
                receipt = new ClientProtocolSendReceipt(observedSendOrdinal.incrementAndGet(), event.getId(),
                        event.getMethod(), event.getGameViewId(), expected.promptCallbackOrdinal(), null);
                armed = null;
                privacyHistoryNextGameActionArmed = false;
                return;
            }
            if (metamorphoseDiagnosticOnly && metamorphoseResponseArmed) {
                receipt = new ClientProtocolSendReceipt(observedSendOrdinal.incrementAndGet(), event.getId(),
                        event.getMethod(), event.getGameViewId(), expected.promptCallbackOrdinal(), null);
                scenario.recordMetamorphoseClientSend(new MetamorphoseClientSendReceipt(event.getMethod(),
                        event.getId(), Objects.equals(event.getGameViewId(), expected.gameViewId()),
                        expected.promptCallbackOrdinal()));
                armed = null;
                metamorphoseResponseArmed = false;
                return;
            }
            if (receipt != null) {
                failure = new AssertionError("More than one client " + expected.method()
                        + " was sent in the armed stale-action window");
                scenario.fail(failure);
                return;
            }
            NextGameDecision observedDecision = null;
            if (event.getMethod() == ProtocolMethod.nextGameDecision) {
                Object[] arguments = event.getObjects();
                if (arguments.length != 1 || !(arguments[0] instanceof NextGameDecision decision)) {
                    failure = new AssertionError("Client nextGameDecision event lacked one enum argument");
                    scenario.fail(failure);
                    return;
                }
                observedDecision = decision;
            }
            receipt = new ClientProtocolSendReceipt(observedSendOrdinal.incrementAndGet(), event.getId(),
                    event.getMethod(), event.getGameViewId(), expected.promptCallbackOrdinal(), observedDecision);
            if (privacyConfirmationOnly) {
                scenario.recordPrivacySacrificeClientSend(receipt);
                armed = null;
            }
        }

        ClientProtocolSendReceipt receipt() { return receipt;
        }
        Throwable failure() { return failure;
        }
        synchronized void disarm() {
            armed = null;
            metamorphoseResponseArmed = false;
            privacyHistoryNextGameActionArmed = false;
        }
    }

    /** Test-only scalar observation before server dispatch; never retains protocol arguments or payloads. */
    static final class ServerDecodedEventTap implements AutoCloseable {
        private static final AtomicLong NEXT_NAME = new AtomicLong();
        private final Channel channel;
        private final int seat;
        private final String handlerName = "dandanStaleActionDecodedTap-" + NEXT_NAME.incrementAndGet();
        private final AtomicLong decodedGuiEventOrdinal = new AtomicLong();
        private volatile ProtocolMethod armedMethod;
        private volatile boolean disposed;
        private volatile ServerProtocolReceiveReceipt receipt;
        private volatile CountDownLatch receiptArrived = new CountDownLatch(1);

        private ServerDecodedEventTap(Channel channel, int seat) {
            this.channel = Objects.requireNonNull(channel);
            this.seat = seat;
        }

        static ServerDecodedEventTap install(Channel channel, int seat) throws Exception {
            if (seat != 1 || channel == null || !channel.isActive()) {
                throw new AssertionError("Stale-action server tap requires B's active assigned channel");
            }
            ServerDecodedEventTap tap = new ServerDecodedEventTap(channel, seat);
            try {
                tap.runOnEventLoop(() -> {
                    if (tap.disposed) return;
                    var pipeline = channel.pipeline();
                    var decoder = pipeline.context(CompatibleObjectDecoder.class);
                    if (decoder == null) throw new AssertionError("Server channel has no CompatibleObjectDecoder");
                    if (pipeline.get(tap.handlerName) != null) throw new AssertionError("Server tap name is already installed");
                    pipeline.addAfter(decoder.name(), tap.handlerName, new ChannelInboundHandlerAdapter() {
                        @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                            try {
                                if (message instanceof GuiGameEvent event) {
                                    long ordinal = tap.decodedGuiEventOrdinal.incrementAndGet();
                                    ProtocolMethod expectedMethod = tap.armedMethod;
                                    if (expectedMethod != null && event.getMethod() == expectedMethod) {
                                        NextGameDecision nextGameDecision = null;
                                        if (expectedMethod == ProtocolMethod.nextGameDecision) {
                                            Object[] arguments = event.getObjects();
                                            if (arguments.length != 1 || !(arguments[0] instanceof NextGameDecision decision)) {
                                                throw new AssertionError("Decoded nextGameDecision event lacked one enum argument");
                                            }
                                            nextGameDecision = decision;
                                        }
                                        synchronized (tap) {
                                            if (tap.receipt != null) {
                                                throw new AssertionError("Duplicate decoded " + expectedMethod
                                                        + " in stale-action window");
                                            }
                                            tap.receipt = new ServerProtocolReceiveReceipt(ordinal, event.getId(),
                                                    event.getMethod(), event.getGameViewId(), tap.seat,
                                                    context.channel(), nextGameDecision);
                                            tap.receiptArrived.countDown();
                                        }
                                    }
                                }
                            } catch (Throwable failure) {
                                tap.recordFailure(failure);
                            } finally {
                                context.fireChannelRead(message);
                            }
                        }
                    });
                    if (tap.disposed) pipeline.remove(tap.handlerName);
                });
            } catch (Throwable failure) {
                tap.disposed = true;
                tap.armedMethod = null;
                try {
                    tap.removeHandlerIfPresent(false);
                } catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                if (failure instanceof Exception exception) throw exception;
                if (failure instanceof Error error) throw error;
                throw new RuntimeException(failure);
            }
            return tap;
        }

        synchronized void arm(ProtocolMethod method) {
            if (armedMethod != null || method != ProtocolMethod.selectButtonOk && method != ProtocolMethod.selectStackItem && method != ProtocolMethod.nextGameDecision && method != ProtocolMethod.selectCard && method != ProtocolMethod.undoLastAction) {
                throw new AssertionError("Server protocol tap cannot be armed for this lifecycle event");
            }
            receipt = null;
            receiptArrived = new CountDownLatch(1);
            armedMethod = method;
        }

        synchronized void armSelectButtonOk() { arm(ProtocolMethod.selectButtonOk); }

        synchronized void disarm() { armedMethod = null; }

        synchronized void disarmSelectButtonOk() {
            armedMethod = null;
        }

        ServerProtocolReceiveReceipt receipt() { return receipt;
        }

        Throwable failure() { return failure;
        }
        private volatile Throwable failure;

        private synchronized void recordFailure(Throwable error) {
            if (failure == null) failure = error;
        }

        private void runOnEventLoop(Runnable action) throws Exception {
            if (channel.eventLoop().isShuttingDown()) {
                throw new AssertionError("B's server event loop is shutting down before tap operation");
            }
            var operation = channel.eventLoop().submit(action);
            try {
                operation.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                operation.cancel(false);
                throw timeout;
            } catch (InterruptedException interrupted) {
                operation.cancel(false);
                Thread.currentThread().interrupt();
                throw interrupted;
            }
        }

        @Override public void close() throws Exception {
            disposed = true;
            armedMethod = null;
            removeHandlerIfPresent(true);
            if (failure != null) throw new AssertionError("Server decoded-event tap failed while observing the action", failure);
        }

        private void removeHandlerIfPresent(boolean required) throws Exception {
            runOnEventLoop(() -> {
                var context = channel.pipeline().context(handlerName);
                if (required && context == null) {
                    throw new AssertionError("Server decoded-event tap disappeared before cleanup");
                }
                if (context != null) channel.pipeline().remove(context.handler());
            });
        }
    }

    record AppliedViewReceipt(String callback, long sequence, long callbackOrdinal,
                              boolean stateBearing, boolean danDan, int turn,
                              PhaseType phase, int turnPlayerViewId, int stackItemCount,
                              Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
        AppliedViewReceipt {
            Map<Integer, Map<ZoneType, List<Integer>>> copied = new LinkedHashMap<>();
            zonesByPlayer.forEach((player, zones) -> {
                EnumMap<ZoneType, List<Integer>> byZone = new EnumMap<>(ZoneType.class);
                zones.forEach((zone, ids) -> byZone.put(zone, List.copyOf(ids)));
                copied.put(player, Collections.unmodifiableMap(byZone));
            });
            zonesByPlayer = Collections.unmodifiableMap(copied);
        }
    }

    record GameViewReceipt(String callback, long sequence, long callbackOrdinal,
                          int incomingGameViewId, int gameViewId,
                          Object trackerIdentity, boolean danDan, boolean stateBearing,
                          Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) { }
    record PrivacyLogRow(String type, String message, int sourceCardId) { }
    record PrivacyLogObservation(long observationOrdinal, String callback, long sourceCallbackOrdinal,
            long sourceSequence, long parentCallbackOrdinal, long parentSequence, int gameViewId,
            int ownPlayerViewId, List<Integer> ownHandIds, Set<Integer> publicCardIds,
            List<PrivacyLogRow> appendedRows,
            Set<Integer> permissionedPrivateCardIds) {
        PrivacyLogObservation {
            ownHandIds = List.copyOf(ownHandIds);
            publicCardIds = Collections.unmodifiableSet(new LinkedHashSet<>(publicCardIds));
            appendedRows = List.copyOf(appendedRows);
            permissionedPrivateCardIds = Collections.unmodifiableSet(new LinkedHashSet<>(permissionedPrivateCardIds));
        }
    }
    record LifecyclePriorityReceipt(int gameViewId, long callbackOrdinal, int seat, int turn,
                                    PhaseType phase, int turnPlayerViewId,
                                    Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) { }
    record LifecycleKeepReceipt(int gameViewId, long callbackOrdinal, int seat, int chooserViewId,
                                List<Integer> handIds) {
        LifecycleKeepReceipt { handIds = List.copyOf(handIds); }
    }

    record PrivacyHistoryNextGameTerminalReceipt(int gameViewId, long callbackOrdinal,
            int seat, boolean gameOver, int playedGames) { }

    record BufferedMetaSelectableReceipt(int gameViewId, int seat, long callbackOrdinal,
                                         int minimum, int maximum, List<Integer> cardIds,
                                         int actionCountAtCapture) {
        BufferedMetaSelectableReceipt { cardIds = List.copyOf(cardIds); }
    }
    record TerminalViewReceipt(int gameViewId, boolean gameOver, boolean winnerIsOwnPlayer,
                               int winningTeam, int playedGames) { }

    record PreHandlerFact(String method, int eventId, long sequence, int changedObjectCount,
                         List<Integer> newCardViewIds,
                         int wrappedEventCount, int wrappedEventBytes, boolean danDan,
                         int turn, String phase, int turnPlayerViewId,
                         List<Integer> playerViewIds, int promptOwnerViewId,
                         String promptOne, String promptTwo,
                         boolean promptOneEnabled, boolean promptTwoEnabled) {
        PreHandlerFact {
            newCardViewIds = List.copyOf(newCardViewIds);
            playerViewIds = List.copyOf(playerViewIds);
        }
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> immutableZoneSnapshot(
            Map<Integer, Map<ZoneType, List<Integer>>> source) {
        Map<Integer, Map<ZoneType, List<Integer>>> copied = new LinkedHashMap<>();
        source.forEach((player, zones) -> {
            EnumMap<ZoneType, List<Integer>> copiedZones = new EnumMap<>(ZoneType.class);
            zones.forEach((zone, ids) -> copiedZones.put(zone, List.copyOf(ids)));
            copied.put(player, Collections.unmodifiableMap(copiedZones));
        });
        return Collections.unmodifiableMap(copied);
    }

    private static Map<Integer, Map<ZoneType, List<Integer>>> snapshotDecodedFullZones(GameView view) {
        Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer = new LinkedHashMap<>();
        for (PlayerView player : view.getPlayers()) {
            EnumMap<ZoneType, List<Integer>> zones = new EnumMap<>(ZoneType.class);
            for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                    ZoneType.Graveyard, ZoneType.Exile)) {
                List<Integer> ids = new ArrayList<>();
                Iterable<CardView> cards = player.getCards(zone);
                if (cards != null) for (CardView card : cards) ids.add(card.getId());
                zones.put(zone, ids);
            }
            zonesByPlayer.put(player.getId(), zones);
        }
        return immutableZoneSnapshot(zonesByPlayer);
    }

    /** Owned, capped inbound bytes and detached decoded facts for one reconnect endpoint. */
    static final class ReconnectObservation {
        private static final int MAX_OWNED_BYTES = 10 * 1024 * 1024;
        private static final int MAX_FACTS = 100_000;
        private final List<byte[]> rawChunks = new ArrayList<>();
        private final List<byte[]> wrappedEventPayloads = new ArrayList<>();
        private final List<PreHandlerFact> facts = new ArrayList<>();
        private final Map<PreHandlerFact, Map<Integer, Map<ZoneType, List<Integer>>>> decodedFullZones =
                new java.util.IdentityHashMap<>();
        private final Map<String, Integer> privacyClaimedByMethod = new LinkedHashMap<>();
        private int ownedByteCount;
        private int rawOwnedByteCount;
        private int wrappedOwnedByteCount;
        private int wrappedOwnedEventCount;
        private int inboundByteCount;
        private boolean overflowed;
        private Throwable failure;

        synchronized int inboundByteCount() { return inboundByteCount;
        }
        synchronized int rawOwnedByteCount() { return rawOwnedByteCount;
        }
        synchronized int wrappedOwnedByteCount() { return wrappedOwnedByteCount;
        }
        synchronized int wrappedOwnedEventCount() { return wrappedOwnedEventCount;
        }
        synchronized boolean overflowed() { return overflowed;
        }
        synchronized Throwable failure() { return failure;
        }
        synchronized List<PreHandlerFact> facts() { return List.copyOf(facts);
        }
        synchronized Map<Integer, Map<ZoneType, List<Integer>>> decodedFullZones(PreHandlerFact fact) {
            return decodedFullZones.getOrDefault(fact, Map.of());
        }
        synchronized PreHandlerFact claimNext(String method) {
            int claimed = privacyClaimedByMethod.getOrDefault(method, 0);
            int found = 0;
            for (PreHandlerFact fact : facts) {
                if (!fact.method().equals(method)) continue;
                if (found++ != claimed) continue;
                privacyClaimedByMethod.put(method, claimed + 1);
                return fact;
            }
            return null;
        }

        void install(Channel channel) {
            if (!channel.eventLoop().inEventLoop()) throw new IllegalStateException("Reconnect tap must be installed on the client event loop");
            var pipeline = channel.pipeline();
            var decoder = pipeline.context(CompatibleObjectDecoder.class);
            if (decoder == null) throw new AssertionError("Reconnect client has no compatible object decoder");
            pipeline.addBefore(decoder.name(), "dandanReconnectRawInbound", new ChannelInboundHandlerAdapter() {
                @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    try {
                        if (message instanceof ByteBuf bytes) copyInbound(bytes);
                    } catch (Throwable error) {
                        recordFailure(error);
                    }
                    context.fireChannelRead(message);
                }
            });
            pipeline.addAfter(decoder.name(), "dandanReconnectDecodedFacts", new ChannelInboundHandlerAdapter() {
                @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    try {
                        if (message instanceof GuiGameEvent event) copyDecodedFact(event);
                    } catch (Throwable error) {
                        recordFailure(error);
                    }
                    context.fireChannelRead(message);
                }
            });
        }

        private synchronized void copyInbound(ByteBuf source) {
            int count = source.readableBytes();
            inboundByteCount += count;
            if (count > MAX_OWNED_BYTES - ownedByteCount) {
                overflowed = true;
                return;
            }
            byte[] copy = new byte[count];
            source.getBytes(source.readerIndex(), copy);
            rawChunks.add(copy);
            ownedByteCount += count;
            rawOwnedByteCount += count;
        }

        private synchronized void copyDecodedFact(GuiGameEvent event) throws ReflectiveOperationException {
            String method = event.getMethod().name();
            Object[] args = event.getObjects();
            long sequence = -1;
            int changedObjects = 0;
            List<Integer> newCardViewIds = new ArrayList<>();
            int packetWrappedEvents = 0;
            int packetWrappedBytes = 0;
            boolean danDan = false;
            int turn = -1;
            String phase = "";
            int turnPlayerId = -1;
            int promptOwner = -1;
            String promptOne = "";
            String promptTwo = "";
            boolean promptOneEnabled = false;
            boolean promptTwoEnabled = false;
            List<Integer> playerIds = new ArrayList<>();
            Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer = Map.of();

            if ("setGameView".equals(method) && args.length > 0 && args[0] instanceof GameView view) {
                if (args.length > 1 && args[1] instanceof Number value) sequence = value.longValue();
                danDan = view.isDanDan();
                turn = view.getTurn();
                phase = view.getPhase() == null ? "" : view.getPhase().name();
                turnPlayerId = view.getPlayerTurn() == null ? -1 : view.getPlayerTurn().getId();
                for (PlayerView player : view.getPlayers()) playerIds.add(player.getId());
                zonesByPlayer = snapshotDecodedFullZones(view);
            } else if ("openView".equals(method) && args.length > 0 && args[0] instanceof Iterable<?> players) {
                for (Object player : players) if (player instanceof PlayerView view) playerIds.add(view.getId());
            } else if ("updateButtons".equals(method) && args.length >= 5) {
                if (args[0] instanceof PlayerView owner) promptOwner = owner.getId();
                if (args[1] instanceof String value) promptOne = value;
                if (args[2] instanceof String value) promptTwo = value;
                if (args[3] instanceof Boolean value) promptOneEnabled = value;
                if (args[4] instanceof Boolean value) promptTwoEnabled = value;
            } else if ("applyDelta".equals(method) && args.length > 0 && args[0] instanceof DeltaPacket packet) {
                sequence = packet.getSequenceNumber();
                changedObjects = packet.getNewObjects().size() + packet.getObjectDeltas().size();
                for (int deltaKey : packet.getNewObjects().keySet()) {
                    if (DeltaPacket.getTypeFromDeltaKey(deltaKey) == DeltaPacket.TYPE_CARD_VIEW) {
                        newCardViewIds.add(DeltaPacket.getIdFromDeltaKey(deltaKey));
                    }
                }
                List<Object> envelopes = packet.getEvents();
                packetWrappedEvents = envelopes == null ? 0 : envelopes.size();
                if (envelopes != null) for (Object envelope : envelopes) {
                    if (envelope == null || !"forge.gamemodes.net.TrackableSerializer$WrappedEvent".equals(envelope.getClass().getName())) {
                        throw new AssertionError("Reconnect applyDelta carried an unexpected event envelope");
                    }
                    RecordComponent data = envelope.getClass().getRecordComponents()[0];
                    Method accessor = data.getAccessor();
                    accessor.setAccessible(true);
                    Object value = accessor.invoke(envelope);
                    if (!(value instanceof byte[] bytes)) throw new AssertionError("WrappedEvent data was not a byte array");
                    packetWrappedBytes += bytes.length;
                    if (bytes.length > MAX_OWNED_BYTES - ownedByteCount) {
                        overflowed = true;
                    } else {
                        wrappedEventPayloads.add(bytes.clone());
                        ownedByteCount += bytes.length;
                        wrappedOwnedByteCount += bytes.length;
                        wrappedOwnedEventCount++;
                    }
                }
            }
            if (facts.size() >= MAX_FACTS) throw new AssertionError("Reconnect decoded-fact limit exceeded");
            PreHandlerFact fact = new PreHandlerFact(method, event.getId(), sequence, changedObjects, newCardViewIds,
                    packetWrappedEvents, packetWrappedBytes, danDan, turn, phase,
                    turnPlayerId, playerIds, promptOwner, promptOne, promptTwo,
                    promptOneEnabled, promptTwoEnabled);
            facts.add(fact);
            if ("setGameView".equals(method)) decodedFullZones.put(fact, zonesByPlayer);
        }

        private synchronized void recordFailure(Throwable error) {
            if (failure == null) failure = error;
        }

        synchronized void clearOwnedBytes() {
            rawChunks.clear();
            wrappedEventPayloads.clear();
            ownedByteCount = 0;
            rawOwnedByteCount = 0;
            wrappedOwnedByteCount = 0;
            wrappedOwnedEventCount = 0;
        }
    }

    record Capture(String callback, long sequence, long callbackOrdinal, byte[] serializedArguments,
                   Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer,
                   List<Integer> publicCardIds, boolean stateBearing, int eventCount,
                   int wrappedEventCount, byte[] wrappedEventEnvelopeBytes,
                   long parentSequence, long parentCallbackOrdinal, List<EventFact> eventFacts) {
        Capture {
            serializedArguments = serializedArguments.clone();
            wrappedEventEnvelopeBytes = wrappedEventEnvelopeBytes.clone();
            Map<Integer, Map<ZoneType, List<Integer>>> copiedZones = new LinkedHashMap<>();
            zonesByPlayer.forEach((player, zones) -> {
                EnumMap<ZoneType, List<Integer>> copied = new EnumMap<>(ZoneType.class);
                zones.forEach((zone, ids) -> copied.put(zone, List.copyOf(ids)));
                copiedZones.put(player, Collections.unmodifiableMap(copied));
            });
            zonesByPlayer = Collections.unmodifiableMap(copiedZones);
            publicCardIds = List.copyOf(publicCardIds);
            eventFacts = List.copyOf(eventFacts);
        }
        @Override public byte[] serializedArguments() { return serializedArguments.clone(); }
        @Override public byte[] wrappedEventEnvelopeBytes() { return wrappedEventEnvelopeBytes.clone(); }
    }

    record ActionReceipt(Decision decision, int seat, int chooserViewId, Integer cardId, boolean abilityChosen) { }
    record NextGameDecisionReceipt(int seat, int chooserViewId, int gameViewId, NextGameDecision decision) { }

    record MetamorphoseStackPriorityReceipt(int seat, int chooserViewId, long callbackOrdinal,
            Game game, int gameViewId, int turn, PhaseType phase, int turnPlayerViewId, int stackSize,
            InputPassPriority priorityInput, int stackItemId, int sourceCardId, int activatorViewId,
            List<Integer> targetCardIds) {
        MetamorphoseStackPriorityReceipt { targetCardIds = List.copyOf(targetCardIds); }
    }
    record MetamorphoseAbilityReceipt(String role, int seat, int chooserViewId, int cardId,
                                      long callbackOrdinal, List<String> abilityFacts) {
        MetamorphoseAbilityReceipt { abilityFacts = List.copyOf(abilityFacts); }
    }
    record MetamorphoseManaAbilityReceipt(int seat, int chooserViewId, int sourceCardId,
                                          long callbackOrdinal, List<String> abilityFacts) {
        MetamorphoseManaAbilityReceipt { abilityFacts = List.copyOf(abilityFacts); }
    }
    record MetamorphoseInputButtonsReceipt(String prompt, int seat, int chooserViewId,
            long promptOrdinal, long buttonsOrdinal, String okLabel, String cancelLabel,
            boolean okEnabled, boolean cancelEnabled) { }
    record MetamorphoseAcceptedPrompt(String stage, int seat, int gameViewId, long promptOrdinal,
            long buttonsOrdinal, PrivacyInputSnapshot inputSnapshot) { }
    record MetamorphoseHeldOptionalRepaintReceipt(String callback, int gameViewId, long callbackOrdinal,
            int seat, int chooserViewId, int repaintCount) { }
    record MetamorphoseClientSendReceipt(ProtocolMethod method, int eventId,
            boolean gameViewMatches, long promptOrdinal) { }
    record MetamorphosePostResolutionPriority(long callbackOrdinal,
            Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
        MetamorphosePostResolutionPriority {
            Map<Integer, Map<ZoneType, List<Integer>>> copied = new LinkedHashMap<>();
            zonesByPlayer.forEach((player, zones) -> {
                EnumMap<ZoneType, List<Integer>> byZone = new EnumMap<>(ZoneType.class);
                zones.forEach((zone, ids) -> byZone.put(zone, List.copyOf(ids)));
                copied.put(player, Collections.unmodifiableMap(byZone));
            });
            zonesByPlayer = Collections.unmodifiableMap(copied);
        }
    }
    record PrivacyChoiceReceipt(int seat, int chooserViewId, long callbackOrdinal, int eventId,
            long wireSequence, String method,
            String message, int minimum, int maximum, List<Integer> cardIds, int selectedCardId,
            List<Boolean> mayLookByChooser, List<Boolean> canShowToChooser,
            List<Boolean> mayLookByOther, List<Boolean> canShowToOther,
            List<Boolean> hostMayLook, List<Boolean> hostCanShow) {
        PrivacyChoiceReceipt {
            cardIds = List.copyOf(cardIds);
            mayLookByChooser = List.copyOf(mayLookByChooser);
            canShowToChooser = List.copyOf(canShowToChooser);
            mayLookByOther = List.copyOf(mayLookByOther);
            canShowToOther = List.copyOf(canShowToOther);
            hostMayLook = List.copyOf(hostMayLook);
            hostCanShow = List.copyOf(hostCanShow);
        }
    }
    record PrivacyHeldEndpointSnapshot(int gameViewId, int ownPlayerViewId,
            Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer,
            Map<Integer, List<Boolean>> privateTopVisibility,
            List<Boolean> negativeHandVisibility) {
        PrivacyHeldEndpointSnapshot {
            Map<Integer, Map<ZoneType, List<Integer>>> copiedZones = new LinkedHashMap<>();
            zonesByPlayer.forEach((player, zones) -> {
                EnumMap<ZoneType, List<Integer>> copied = new EnumMap<>(ZoneType.class);
                zones.forEach((zone, ids) -> copied.put(zone, List.copyOf(ids)));
                copiedZones.put(player, Collections.unmodifiableMap(copied));
            });
            zonesByPlayer = Collections.unmodifiableMap(copiedZones);
            Map<Integer, List<Boolean>> copiedVisibility = new LinkedHashMap<>();
            privateTopVisibility.forEach((id, facts) -> copiedVisibility.put(id, List.copyOf(facts)));
            privateTopVisibility = Collections.unmodifiableMap(copiedVisibility);
            negativeHandVisibility = List.copyOf(negativeHandVisibility);
        }
    }
    record PrivacyAbilityReceipt(String role, int seat, int chooserViewId, int cardId, int abilityId,
            long callbackOrdinal, int eventId, long wireSequence, List<String> abilityFacts) {
        PrivacyAbilityReceipt { abilityFacts = List.copyOf(abilityFacts); }
    }
    record PrivacyRevealReceipt(int seat, int chooserViewId, long callbackOrdinal,
            int eventId, long wireSequence,
            String message, int minimum, int maximum, List<Integer> cardIds,
            List<Boolean> mayLookByChooser, List<Boolean> canShowToChooser, String diagnostic) {
        PrivacyRevealReceipt {
            cardIds = List.copyOf(cardIds);
            mayLookByChooser = List.copyOf(mayLookByChooser);
            canShowToChooser = List.copyOf(canShowToChooser);
        }
    }
    record PrivacyStateReceipt(int seat, String callback, long sequence, long callbackOrdinal,
            int turn, PhaseType phase, int turnPlayerViewId, int stackSize,
            boolean stateBearing, Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer,
            Map<Integer, Integer> drawnThisTurnByPlayer,
            Map<Integer, List<Boolean>> privateTopVisibilityByRemote) {
        PrivacyStateReceipt {
            Map<Integer, Map<ZoneType, List<Integer>>> copied = new LinkedHashMap<>();
            zonesByPlayer.forEach((player, zones) -> {
                EnumMap<ZoneType, List<Integer>> byZone = new EnumMap<>(ZoneType.class);
                zones.forEach((zone, ids) -> byZone.put(zone, List.copyOf(ids)));
                copied.put(player, Collections.unmodifiableMap(byZone));
            });
            zonesByPlayer = Collections.unmodifiableMap(copied);
            drawnThisTurnByPlayer = Collections.unmodifiableMap(new LinkedHashMap<>(drawnThisTurnByPlayer));
            Map<Integer, List<Boolean>> visibility = new LinkedHashMap<>();
            privateTopVisibilityByRemote.forEach((cardId, flags) -> visibility.put(cardId, List.copyOf(flags)));
            privateTopVisibilityByRemote = Collections.unmodifiableMap(visibility);
        }
    }
    record PrivacyPromptReceipt(int seat, int chooserViewId, long callbackOrdinal, String message) { }
    record PrivacyStackViewReceipt(int seat, int chooserViewId, long callbackOrdinal,
            int sourceCardId, int activatorViewId, boolean ability) { }
    record PrivacyPaymentReceipt(int seat, int chooserViewId, long callbackOrdinal, int sourceCardId,
            int abilityId, int activatorViewId, List<Integer> payingManaSourceIds,
            List<Integer> payingManaColors, List<Integer> payingAbilitySourceIds,
            List<Integer> sacrificedCardIds, boolean sourceTapped) {
        PrivacyPaymentReceipt {
            payingManaSourceIds = List.copyOf(payingManaSourceIds);
            payingManaColors = List.copyOf(payingManaColors);
            payingAbilitySourceIds = List.copyOf(payingAbilitySourceIds);
            sacrificedCardIds = List.copyOf(sacrificedCardIds);
        }
    }

    enum PrivacyPriorityAction { PASS, CAST_TELLING_TIME, HOLD_T3_SHUFFLE_CAST, HOLD_TELLING_TIME, PASS_STACK, HOLD_FENGRAF, HOLD_SHUFFLE_HISTORY, HOLD_HISTORY_RECONNECT_HOST }
    record CallbackReceipt(long wireSequence, long callbackOrdinal) { }
    record MulliganChoiceReceipt(Decision decision, int seat, int chooserViewId, int gameViewId,
                                 long receiptOrder, String callback, long callbackOrdinal,
                                 List<Integer> handIds, int landCount) {
        MulliganChoiceReceipt { handIds = List.copyOf(handIds); }
    }
    record FreeRevealReceipt(int receivingSeat, int gameViewId, int eventId, long receiptOrder,
                             String message, long getChoicesCallbackOrdinal,
                             List<Integer> cardIds) {
        FreeRevealReceipt { cardIds = List.copyOf(cardIds); }
    }
    record StarterButtonReceipt(int seat, int chooserViewId, boolean chosePlay, String playLabel,
                                String drawLabel, boolean playEnabled, boolean drawEnabled,
                                long callbackOrdinal) { }
    record StarterPromptReceipt(int seat, int chooserViewId, String message, long callbackOrdinal) { }
    record KeepOpeningReceipt(int seat, int chooserViewId, int gameViewId, long receiptOrder,
                              long callbackOrdinal, List<Integer> handIds) {
        KeepOpeningReceipt { handIds = List.copyOf(handIds); }
    }
    record SelectablesReceipt(int seat, int chooserViewId, long callbackOrdinal, String stage,
                              int minimum, int maximum, List<Integer> cardIds) {
        SelectablesReceipt { cardIds = List.copyOf(cardIds); }
    }
    record HumanChoiceReceipt(int seat, int chooserViewId, long callbackOrdinal, String stage,
                              String message, List<Integer> cardIds) {
        HumanChoiceReceipt { cardIds = List.copyOf(cardIds); }
    }
    record MetamorphoseResolutionReceipt(String callback, long sequence, long callbackOrdinal,
                                         int receiverSeat, int hostCardId, int targetCardId,
                                         int handCardId, List<Integer> libraryIds,
                                         List<Integer> hostGraveyardIds, List<Integer> recipientHandIds,
                                         List<Integer> recipientBattlefieldIds,
                                         Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
        MetamorphoseResolutionReceipt {
            libraryIds = List.copyOf(libraryIds);
            hostGraveyardIds = List.copyOf(hostGraveyardIds);
            recipientHandIds = List.copyOf(recipientHandIds);
            recipientBattlefieldIds = List.copyOf(recipientBattlefieldIds);
        }
    }
    /** Validated London prompt, own-hand selection, and OK dispatch intent; state receipt proves resolution. */
    record LondonBottomDispatchReceipt(int seat, int chooserViewId, int gameViewId, long receiptOrder,
                                       int cardId, List<Integer> chooserHandIds, long promptOrdinal,
                                       long appliedHandCallbackOrdinal, String oneCardPrompt, long zeroCardPromptOrdinal,
                                       String zeroCardPrompt) {
        LondonBottomDispatchReceipt { chooserHandIds = List.copyOf(chooserHandIds); }
    }

    private record HandFacts(List<Integer> ids, int landCount) {
        private HandFacts { ids = List.copyOf(ids); }
    }
    record PostKeepReceipt(Capture stateCapture, Map<Integer, Map<ZoneType, List<Integer>>> appliedZonesByPlayer,
                           Map<Integer, Integer> playerViewIdsBySeat, int prioritySeat,
                           long priorityCallbackOrdinal, int heldTurn, PhaseType heldPhase, int heldTurnPlayerViewId,
                           Map<Integer, Map<ZoneType, List<Integer>>> heldPriorityZonesByPlayer) {
        PostKeepReceipt {
            appliedZonesByPlayer = copyZones(appliedZonesByPlayer);
            playerViewIdsBySeat = Collections.unmodifiableMap(new LinkedHashMap<>(playerViewIdsBySeat));
            heldPriorityZonesByPlayer = copyZones(heldPriorityZonesByPlayer);
        }
        private static Map<Integer, Map<ZoneType, List<Integer>>> copyZones(
                Map<Integer, Map<ZoneType, List<Integer>>> source) {
            Map<Integer, Map<ZoneType, List<Integer>>> zones = new LinkedHashMap<>();
            source.forEach((player, byZone) -> {
                EnumMap<ZoneType, List<Integer>> copied = new EnumMap<>(ZoneType.class);
                byZone.forEach((zone, ids) -> copied.put(zone, List.copyOf(ids)));
                zones.put(player, Collections.unmodifiableMap(copied));
            });
            return Collections.unmodifiableMap(zones);
        }
    }
    private record ExpectedAction(Decision decision, int seat, Integer cardId) { }
    private record PostKeepCandidate(Capture stateCapture,
                                    Map<Integer, Map<ZoneType, List<Integer>>> appliedZonesByPlayer,
                                    Map<Integer, Integer> playerViewIdsBySeat) { }

    /** Shared semantic barriers for exactly this two-seat scenario, not a general harness. */
    static final class Scenario {
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final CountDownLatch[] publicLandMoves = {new CountDownLatch(1), new CountDownLatch(1)};
        private final CountDownLatch[] landAbilities = {new CountDownLatch(1), new CountDownLatch(1)};
        private final Map<Integer, Integer> expectedLandIds = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Map<Integer, Integer> seatByViewId = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Map<Integer, CallbackReceipt> publicMoveReceipts = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Set<Integer> observedLandIds = Collections.synchronizedSet(new LinkedHashSet<>());
        private final List<ActionReceipt> actions = Collections.synchronizedList(new ArrayList<>());
        private final List<NextGameDecisionReceipt> nextGameDecisions = Collections.synchronizedList(new ArrayList<>());
        private final List<MulliganChoiceReceipt> mulliganChoices = Collections.synchronizedList(new ArrayList<>());
        private final List<FreeRevealReceipt> freeReveals = Collections.synchronizedList(new ArrayList<>());
        private final List<StarterButtonReceipt> starterButtonReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<StarterPromptReceipt> starterPromptReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<KeepOpeningReceipt> keepOpeningReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<SelectablesReceipt> selectablesReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<HumanChoiceReceipt> humanChoiceReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<MetamorphoseStackPriorityReceipt> metamorphoseStackPriorities = Collections.synchronizedList(new ArrayList<>());
        private final List<MetamorphoseAbilityReceipt> metamorphoseAbilityReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<MetamorphoseManaAbilityReceipt> metamorphoseManaAbilityReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<MetamorphoseInputButtonsReceipt> metamorphoseInputButtons = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyChoiceReceipt> privacyChoices = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyAbilityReceipt> privacyAbilities = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyRevealReceipt> privacyReveals = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyPromptReceipt> privacyPrompts = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyStackViewReceipt> privacyStackViewReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyPaymentReceipt> privacyPaymentReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyPaymentBinding> privacyPaymentBindings = new ArrayList<>();
        private PrivacyStackPriorityCheckpoint privacyStackPriorityCheckpoint;
        private boolean privacyStackPriorityDispatchInvoked;
        private final List<LondonBottomDispatchReceipt> londonBottomDispatches = Collections.synchronizedList(new ArrayList<>());
        private long mulliganReceiptOrder;
        private final List<Runnable> failureHooks = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean concessionQueued = new AtomicBoolean();
        private final CountDownLatch postKeepState = new CountDownLatch(1);
        private final long[] keepPromptOrdinals = {-1, -1};
        private boolean postKeepPriorityHeld;
        private int postKeepPrioritySeat = -1;
        private long postKeepPriorityOrdinal = -1;
        private int heldPriorityTurn = -1;
        private PhaseType heldPriorityPhase;
        private int heldPriorityTurnPlayerViewId = -1;
        private int reconnectHostUpkeepPrompts;
        private Map<Integer, Map<ZoneType, List<Integer>>> heldPriorityZones = Map.of();
        private PostKeepCandidate postKeepCandidate;
        private volatile PostKeepReceipt postKeepReceipt;
        private volatile String expectedPostKeepCallback;
        private volatile boolean mulliganScenario;
        private volatile boolean starterSeatZeroScenario;
        private volatile BooleanSupplier initialShuffleConsumed;
        private volatile boolean reconnectScenario;
        private volatile Game disconnectAiNextGameOrigin;
        private volatile boolean reconnectPostureReached;
        private volatile boolean metamorphoseScenario;
        private volatile boolean metamorphoseOpeningDiagnostic;
        private volatile Game metamorphoseOpeningEngineGame;
        private volatile boolean metamorphoseAccept;
        private volatile boolean holdMetamorphoseOptional;
        private volatile boolean lifecycleScenario;
        private volatile boolean staleActionWitness;
        private volatile boolean staleQuitWitness;
        private volatile boolean lifecycleCheckpointArmed;
        private volatile LifecyclePriorityReceipt lifecyclePriorityReceipt;
        private int lifecycleHostPriorityAutoPassCount;
        private final List<LifecycleKeepReceipt> lifecycleKeepReceipts = Collections.synchronizedList(new ArrayList<>());
        private volatile int metamorphoseHostCardId = -1;
        private volatile int metamorphoseTargetCardId = -1;
        private volatile int metamorphoseHandCardId = -1;
        private volatile List<Integer> metamorphoseManaCardIds = List.of();
        private volatile int metamorphoseHostViewId = -1;
        private volatile int metamorphoseRemoteViewId = -1;
        private volatile String metamorphoseOptionalMessage;
        private volatile int metamorphoseStage;
        private volatile Game metamorphoseGame;
        private volatile ClientProtocolObservation metamorphoseClientSendObservation;
        private volatile MetamorphoseAcceptedPrompt metamorphoseOptionalAcceptedPrompt;
        private volatile MetamorphoseAcceptedPrompt metamorphoseHandAcceptedPrompt;
        private volatile int metamorphoseOptionalResponseQueued;
        private volatile int metamorphoseOptionalResponseDispatched;
        private volatile int metamorphoseHeldOptionalRepaintCount;
        private volatile MetamorphoseHeldOptionalRepaintReceipt lastMetamorphoseHeldOptionalRepaint;
        private volatile boolean metamorphoseOptionalResponseArmed;
        private volatile int metamorphoseHandResponseQueued;
        private volatile int metamorphoseHandResponseDispatched;
        private volatile boolean metamorphoseHandResponseArmed;
        private final List<MetamorphoseClientSendReceipt> metamorphoseClientSendReceipts =
                Collections.synchronizedList(new ArrayList<>());
        private volatile int metamorphoseAssignedConcessionGameViewId = -1;
        private volatile boolean metamorphoseLifecycleConcessionPending;

        private volatile BufferedMetaSelectableReceipt bufferedMetaSelectableReceipt;
        private int metamorphoseUntapPasses;
        private int metamorphoseExpectedUntapSeat = -1;
        private boolean metamorphoseUntapPassPending;
        private int metamorphosePreCastPasses;
        private int metamorphoseExpectedUpkeepSeat = -1;
        private int metamorphoseStackPasses;
        private boolean metamorphoseStackPassPending;
        private int metamorphoseExpectedStackSeat = -1;
        private boolean metamorphoseUpkeepPassPending;
        private boolean metamorphoseCastDispatchPending;
        private boolean metamorphoseConcessionSent;
        private volatile long metamorphoseTargetCallbackOrdinal = -1;
        private volatile long metamorphoseChooserCallbackOrdinal = -1;
        private volatile long metamorphoseChoiceCallbackOrdinal = -1;
        private volatile long metamorphoseOptionalButtonsOrdinal = -1;
        private volatile boolean metamorphosePostKeepReleased;
        private final AtomicReference<MetamorphoseResolutionReceipt> metamorphoseResolution = new AtomicReference<>();
        private final CountDownLatch metamorphoseResolved = new CountDownLatch(1);
        private final CountDownLatch metamorphoseOptionalHeld = new CountDownLatch(1);
        private final CountDownLatch metamorphoseFirstStackPriority = new CountDownLatch(1);
        private volatile boolean metamorphoseFirstStackPriorityReleased;
        private final AtomicReference<MetamorphosePostResolutionPriority> metamorphosePostResolutionPriority = new AtomicReference<>();
        private final CountDownLatch metamorphosePostResolutionPrompt = new CountDownLatch(1);
        private volatile boolean privacyScenario;
        private volatile boolean privacyShuffleHistoryScenario;
        private volatile Game privacyHistoryNextGame;
        private final List<PrivacyHistoryNextGameTerminalReceipt> privacyHistoryNextGameTerminals =
                Collections.synchronizedList(new ArrayList<>());
        private volatile boolean privacyDeltaSync;
        private volatile Game privacyGame;
        private volatile Game openingKeepEngineGame;
        private int openingKeepDeferralCount;
        private final List<OpeningKeepGateReceipt> openingKeepGateReceipts = new ArrayList<>();
        private volatile ScriptedGui privacyHostGui;
        private volatile int privacyHostViewId = -1;
        private volatile int privacyRemoteViewId = -1;
        private volatile int privacyTellingTimeSeat = 1;
        private volatile int privacyFengrafSeat = 0;
        private volatile int privacyTellingTimeId = -1;
        private volatile int privacyDaysUndoingId = -1;
        private volatile int privacyFengrafId = -1;
        private volatile int privacyFengrafAbilityId = -1;
        private volatile int privacyPublicCreatureId = -1;
        private volatile List<Integer> privacyTopIds = List.of();
        private volatile List<Integer> privacyInitialLibraryIds = List.of();
        private volatile List<Integer> privacyRemoteManaIds = List.of();
        private volatile List<Integer> privacyHostManaIds = List.of();
        private volatile String privacyExpectedRevealMessage;
        private volatile String privacyExpectedSacrificeMessage;
        private volatile Input privacyFirstSacrificeInput;
        private volatile ClientProtocolObservation privacyOkClientObservation;
        private volatile long privacyConfirmationOkSentNanos;
        private volatile int privacyPostSendRepaintStage;
        private volatile ReconnectObservation privacyObservation;
        private volatile int privacyPriorityStage;
        private final CountDownLatch privacyShuffleHistoryUpkeep = new CountDownLatch(1);
        private final CountDownLatch privacyShuffleHistoryPriorityHeld = new CountDownLatch(1);
        private final AtomicReference<PrivacyStateReceipt> privacyShuffleHistoryState = new AtomicReference<>();
        private volatile PrivacyShuffleT4PriorityInput privacyShuffleT4PriorityInput;
        private volatile int privacyShuffleT4RepeatedPriorityCallbacks;
        private volatile long privacyDaysUndoingCastOrdinal = -1;
        private int privacyTellingStackPasses;
        private int privacyFengrafStackPasses;
        private boolean privacyFengrafActorPriorityHeld;
        private boolean privacyHostPassedForFengraf;
        private boolean privacyTellingTimeCastDispatched;
        private boolean privacyFengrafDispatchPending;
        private boolean privacyFengrafAbilityChosen;
        private boolean privacyConcessionSent;
        private volatile int privacyTellingTimeChoiceCount;
        private volatile int privacyManaSelections;
        private volatile int privacyExpectedManaCount;
        private volatile int privacyTellingTimeAbilityChoices;
        private volatile int privacyFengrafAbilityChoices;
        private final Set<Integer> privacyManaAbilityChoices = new HashSet<>();
        private final Set<Integer> privacyHistoryManaAbilityChoices = new HashSet<>();
        private volatile Map<Integer, Integer> privacyTellingTimeStartDrawCounts = Map.of();
        private volatile long privacyTellingTimeStartDrawOrdinal = -1;
        private volatile long privacyTellingTimeFenceOrdinal = -1;
        private volatile long privacyFengrafFenceOrdinal = -1;
        private volatile long privacyRevealOrdinal = -1;
        private volatile int privacyFengrafConfirmOrdinal = -1;
        private volatile String privacyFengrafConfirmMessage;
        private volatile long privacyConfirmButtonsOrdinal = -1;
        private volatile long privacyPreSendRepaintOrdinal = -1;
        private volatile long privacyConfirmRepaintOrdinal = -1;
        private volatile long privacyConfirmRepaintPromptOrdinal = -1;
        private volatile boolean privacyConfirmationPending;
        private volatile boolean privacyConfirmationRepaintPending;
        private volatile boolean privacyConfirmationOkDispatched;
        private final AtomicReference<PrivacyStateReceipt> privacyTellingTimeState = new AtomicReference<>();
        private final AtomicReference<PrivacyStateReceipt> privacyFengrafState = new AtomicReference<>();
        private final CountDownLatch privacyFengrafActorPriority = new CountDownLatch(1);
        private final CountDownLatch privacyTellingTimeResolved = new CountDownLatch(1);
        private final CountDownLatch privacyFengrafStateArrived = new CountDownLatch(1);
        private final CountDownLatch privacyFengrafResolved = new CountDownLatch(1);
        private final CountDownLatch privacyFengrafReveal = new CountDownLatch(1);
        private final CountDownLatch privacyHostChoiceHeld = new CountDownLatch(1);
        private final CountDownLatch privacyHostChoiceReleased = new CountDownLatch(1);
        private volatile boolean privacyHostChoiceActive;
        private final AtomicInteger freeMulliganSeat = new AtomicInteger(-1);
        private final AtomicBoolean londonsBottomed = new AtomicBoolean();
        private volatile boolean stopped;

        void fail(Throwable error) {
            if (failure.compareAndSet(null, error)) {
                stopped = true;
                for (CountDownLatch latch : publicLandMoves) latch.countDown();
                for (CountDownLatch latch : landAbilities) latch.countDown();
                postKeepState.countDown();
                metamorphoseResolved.countDown();
                metamorphoseFirstStackPriority.countDown();
                metamorphosePostResolutionPrompt.countDown();
                privacyFengrafActorPriority.countDown();
                privacyTellingTimeResolved.countDown();
                privacyFengrafStateArrived.countDown();
                privacyFengrafResolved.countDown();
                privacyFengrafReveal.countDown();
                privacyShuffleHistoryUpkeep.countDown();
                privacyShuffleHistoryPriorityHeld.countDown();
                synchronized (failureHooks) { failureHooks.forEach(Runnable::run); }
            }
        }
        Throwable failure() { return failure.get();
        }
        boolean stopped() { return stopped;
        }
        void expectLand(int seat, int cardId) {
            synchronized (expectedLandIds) {
                if (expectedLandIds.putIfAbsent(seat, cardId) != null) fail(new AssertionError("A land was already queued for seat " + seat));
            }
        }
        void registerView(int seat, int viewId) { seatByViewId.put(viewId, seat); }
        int seatForView(int viewId) { return seatByViewId.getOrDefault(viewId, -1);
        }

        void recordAction(ActionReceipt receipt) { actions.add(receipt); }
        List<ActionReceipt> actions() { synchronized (actions) { return List.copyOf(actions); } }
        void recordNextGameDecision(NextGameDecisionReceipt receipt) { nextGameDecisions.add(receipt); }
        List<NextGameDecisionReceipt> nextGameDecisions() {
            synchronized (nextGameDecisions) { return List.copyOf(nextGameDecisions); }
        }
        void configureMulliganScenario(BooleanSupplier shuffleConsumed) {
            mulliganScenario = true;
            initialShuffleConsumed = shuffleConsumed;
        }
        void configureStarterSeatZeroScenario() { starterSeatZeroScenario = true; }

        void beginMetamorphoseScenario(boolean accept) {
            metamorphoseScenario = true;
            metamorphoseAccept = accept;
        }
        void holdMetamorphoseOptionalForLifecycle() {
            metamorphoseScenario = true;
            holdMetamorphoseOptional = true;
            lifecycleScenario = true;
        }
        void enableStaleActionWitness() {
            if (!lifecycleScenario) throw new AssertionError("Stale-action witness requires the existing lifecycle fixture");
            staleActionWitness = true;
        }
        void enableStaleQuitWitness() {
            if (!lifecycleScenario || staleQuitWitness) {
                throw new AssertionError("Stale-QUIT witness requires the existing lifecycle fixture exactly once");
            }
            staleActionWitness = true;
            staleQuitWitness = true;
        }
        boolean staleActionWitness() { return staleActionWitness;
        }
        boolean staleQuitWitness() { return staleQuitWitness;
        }
        boolean holdMetamorphoseOptional() { return holdMetamorphoseOptional;
        }
        boolean heldMetamorphoseOptionalBeforeLifecycleConcession() {
            return holdMetamorphoseOptional && lifecycleScenario && metamorphoseAssignedConcessionGameViewId < 0;
        }
        boolean lifecycleScenario() { return lifecycleScenario;
        }
        boolean awaitMetamorphoseOptionalHeld(long timeout, TimeUnit unit) throws InterruptedException {
            return metamorphoseOptionalHeld.await(timeout, unit);
        }
        void markMetamorphoseOptionalHeld() { metamorphoseOptionalHeld.countDown(); }
        PrivacyInputSnapshot metamorphoseRemoteInputSnapshot(Game game) {
            if (!metamorphoseScenario || game == null || game.getPlayers().size() < 2 || !(game.getPlayers().get(1).getController() instanceof PlayerControllerHuman controller)) return null;
            return new PrivacyInputSnapshot(controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
        }
        PrivacyInputSnapshot lifecyclePriorityInputSnapshot(Game game, int seat) {
            if (!lifecycleScenario || game == null || seat < 0 || seat >= game.getPlayers().size() || !(game.getPlayers().get(seat).getController() instanceof PlayerControllerHuman controller)) return null;
            return new PrivacyInputSnapshot(controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
        }
        synchronized void prepareLifecycleNextGame(String expectedCallback) {
            if (!holdMetamorphoseOptional || metamorphoseStage != 3) {
                fail(new AssertionError("Next-game reset requires the held actual Metamorphose confirmation"));
                return;
            }
            metamorphoseScenario = false;
            expectedPostKeepCallback = expectedCallback;
            postKeepPriorityHeld = false;
            postKeepPriorityReleased = false;
            postKeepCandidate = null;
            lifecycleCheckpointArmed = true;
            lifecyclePriorityReceipt = null;
            lifecycleHostPriorityAutoPassCount = 0;
            lifecycleKeepReceipts.clear();
            keepPromptOrdinals[0] = -1;
            keepPromptOrdinals[1] = -1;
        }
        LifecyclePriorityReceipt lifecyclePriorityReceipt() { return lifecyclePriorityReceipt;
        }
        synchronized int lifecycleHostPriorityAutoPassCount() { return lifecycleHostPriorityAutoPassCount;
        }
        boolean lifecycleCheckpointArmed() { return lifecycleCheckpointArmed;
        }
        boolean postKeepPriorityHeld() { return postKeepPriorityHeld;
        }

        List<LifecycleKeepReceipt> lifecycleKeepReceipts() {
            synchronized (lifecycleKeepReceipts) { return List.copyOf(lifecycleKeepReceipts); }
        }
        void recordLifecycleKeep(int gameViewId, long callbackOrdinal, long callbackSequence, String callback,
                                 int seat, int chooserViewId, List<Integer> handIds,
                                 boolean viewPlayerMatchesTracker, int viewHandCount, int trackerHandCount,
                                 int latestIncomingGameViewId, int latestAppliedGameViewId,
                                 long latestViewCallbackOrdinal) {
            if (!lifecycleCheckpointArmed) return; // the held Meta game has ordinary opening Keeps too
            if (gameViewId < 0 || callbackOrdinal <= 0 || seat < 0 || seat > 1 || chooserViewId < 0 || handIds.size() != 7) {

                fail(new AssertionError("N+1 Keep receipt invalid: gameViewId=" + gameViewId
                        + ",seat=" + seat + ",chooserViewId=" + chooserViewId + ",callback=" + callback
                        + ",callbackSequence=" + callbackSequence + ",callbackOrdinal=" + callbackOrdinal
                        + ",snapshotHandCount=" + handIds.size() + ",viewHandCount=" + viewHandCount
                        + ",trackerHandCount=" + trackerHandCount + ",viewPlayerMatchesTracker="
                        + viewPlayerMatchesTracker + ",latestIncomingGameViewId=" + latestIncomingGameViewId
                        + ",latestAppliedGameViewId=" + latestAppliedGameViewId
                        + ",latestViewCallbackOrdinal=" + latestViewCallbackOrdinal));
                return;
            }
            lifecycleKeepReceipts.add(new LifecycleKeepReceipt(gameViewId, callbackOrdinal, seat, chooserViewId, handIds));
        }
        void configureMetamorphoseScenario(Game game, boolean accept, int hostCardId, int targetCardId, int handCardId,
                                           List<Integer> manaCardIds,
                                           String expectedOptionalMessage) {
            metamorphoseScenario = true;
            metamorphoseGame = Objects.requireNonNull(game);
            metamorphoseAccept = accept;
            metamorphoseHostCardId = hostCardId;
            metamorphoseTargetCardId = targetCardId;
            metamorphoseHandCardId = handCardId;
            metamorphoseManaCardIds = List.copyOf(manaCardIds);
            metamorphoseOptionalMessage = expectedOptionalMessage;
        }
        synchronized void beginPrivacyScenario(boolean deltaSync, int tellingTimeSeat, int fengrafSeat) {
            if (tellingTimeSeat < 0 || tellingTimeSeat > 1 || fengrafSeat < 0 || fengrafSeat > 1 || tellingTimeSeat == fengrafSeat) {
                throw new IllegalArgumentException("Privacy actors must be the two distinct assigned seats");
            }
            privacyScenario = true;
            privacyDeltaSync = deltaSync;
            privacyTellingTimeSeat = tellingTimeSeat;
            privacyFengrafSeat = fengrafSeat;
        }

        Game privacyHistoryNextGame() { return privacyHistoryNextGame;
        }
        synchronized void recordPrivacyHistoryNextGameKeep(int gameViewId, long callbackOrdinal,
                long sequence, int seat, int chooserViewId, List<Integer> handIds,
                Object trackerIdentity, boolean trackerOwnsPlayerView, boolean controllerOwnsGame) {

                fail(new AssertionError("N+1 opening Keep lacks one fresh current endpoint receipt"));
                return;
        }

        List<PrivacyHistoryNextGameTerminalReceipt> privacyHistoryNextGameTerminals() {
            synchronized (privacyHistoryNextGameTerminals) { return List.copyOf(privacyHistoryNextGameTerminals); }
        }

        int privacyTellingTimeSeat() { return privacyTellingTimeSeat;
        }
        int privacyFengrafSeat() { return privacyFengrafSeat;
        }
        int privacyViewIdForSeat(int seat) {
            if (seat == 0) return privacyHostViewId;
            if (seat == 1) return privacyRemoteViewId;
            throw new AssertionError("Privacy role has no assigned seat: " + seat);
        }
        synchronized void attachPrivacyGame(Game game) {
            if (!privacyScenario || privacyGame != null || game == null) {
                fail(new AssertionError("Privacy scenario did not attach its actual host Game exactly once"));
                return;
            }
            privacyGame = game;

        }
        Game privacyGame() { return privacyGame;
        }
        void retainOpeningKeepEngineGame(Game game) {
            if (game == null || !(metamorphoseScenario || privacyScenario || lifecycleScenario)) return;
            synchronized (this) {
                if (openingKeepEngineGame == null) openingKeepEngineGame = game;
            }
        }
        Game openingKeepEngineGame() { return openingKeepEngineGame;
        }
        synchronized void recordOpeningKeepDeferred(int seat, int gameViewId, long promptOrdinal) {
            if (seat != 1 || gameViewId <= 0 || promptOrdinal <= 0 || openingKeepDeferralCount != 0) {
                fail(new AssertionError("Opening Keep readiness gate was not one bounded remote callback"));
                return;
            }
            openingKeepDeferralCount++;
        }
        synchronized void recordOpeningKeepGateDispatch(OpeningKeepGateReceipt receipt) {
            int expectedHandCount = privacyMulliganPrefixScenario() && receipt != null && receipt.seat() == 1 ? 6 : 7;
            boolean compositeBottom = receipt != null && receipt.londonBottomCardId() > 0;
            if (receipt == null || openingKeepGateReceipts.size() >= openingKeepDeferralCount || receipt.seat() != 1 || receipt.gameViewId() <= 0 || receipt.appliedStateLowerBoundOrdinal() <= 0 || receipt.appliedCallbackOrdinal() <= receipt.appliedStateLowerBoundOrdinal() || receipt.inputType() != InputConfirmMulligan.class || receipt.handCount() != expectedHandCount || (privacyMulliganPrefixScenario() && receipt.seat() == 1) != compositeBottom || (compositeBottom && !receipt.bottomInSharedLibrary()) || !receipt.sameGame() || !receipt.sameTracker() || receipt.okDispatchCount() != 1) {
                fail(new AssertionError("Deferred opening Keep lacks one typed same-Game applied-hand OK receipt"));
                return;
            }
            openingKeepGateReceipts.add(receipt);
        }

        synchronized List<OpeningKeepGateReceipt> openingKeepGateReceipts() {
            return List.copyOf(openingKeepGateReceipts);
        }
        synchronized ReadinessGateSummary readinessGateSummary() {
            return new ReadinessGateSummary(openingKeepDeferralCount, openingKeepGateReceipts.size(),
                    openingKeepGateReceipts.stream().mapToInt(OpeningKeepGateReceipt::okDispatchCount).sum(),
                    privacyPreSendRepaintOrdinal > 0 ? 1 : 0, privacyShuffleT4RepeatedPriorityCallbacks);
        }

        Game metamorphoseGame() { return metamorphoseGame;
        }
        void enableMetamorphoseOpeningDiagnostic() { metamorphoseOpeningDiagnostic = true; }
        void retainMetamorphoseOpeningEngineGame(Game game) {
            if (!metamorphoseOpeningDiagnostic || game == null) return;
            synchronized (this) {
                if (metamorphoseOpeningEngineGame == null) metamorphoseOpeningEngineGame = game;
            }
        }
        Game metamorphoseOpeningEngineGame() { return metamorphoseOpeningEngineGame;
        }
        PrivacyInputSnapshot privacyInputSnapshotForSeat(int seat) {
            Game game = privacyGame;
            if (game == null || seat < 0 || seat >= game.getPlayers().size() || !(game.getPlayers().get(seat).getController() instanceof PlayerControllerHuman controller)) {
                return null;
            }
            return new PrivacyInputSnapshot(controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
        }

        private boolean isSamePrivacyShuffleT4PriorityInput(int seat, int viewId, int gameViewId,
                long ordinal, int turn, PhaseType phase, int turnPlayerViewId, int stackSize,
                ScriptedGui endpoint) {
            PrivacyShuffleT4PriorityInput held = privacyShuffleT4PriorityInput;
            Game game = privacyGame;
            if (held == null || game == null || seat != 1 || viewId != privacyRemoteViewId || gameViewId != held.gameViewId() || turn != 4 || phase != PhaseType.UPKEEP || turnPlayerViewId != privacyRemoteViewId || stackSize != 0 || game != held.game() || seat >= game.getPlayers().size() || game.getPlayers().get(seat).getId() != viewId || game.getPlayers().get(seat).getController() != held.controller() || held.controller().getGame() != game || game.getView() == null || game.getView().getId() != held.gameViewId() || game.getPhaseHandler().getTurn() != 4 || game.getPhaseHandler().getPhase() != PhaseType.UPKEEP || game.getPhaseHandler().getPlayerTurn() == null || game.getPhaseHandler().getPlayerTurn().getId() != privacyRemoteViewId || game.getPhaseHandler().getPriorityPlayer() == null || game.getPhaseHandler().getPriorityPlayer().getId() != privacyRemoteViewId || game.getStack() == null || !game.getStack().isEmpty() || game.isGameOver() || game.getOutcome() != null || game.getPlayers().stream().anyMatch(Player::conceded)) return false;
            PrivacyInputSnapshot current = new PrivacyInputSnapshot(
                    held.controller().getInputProxy().getInput(), held.controller().getInputQueue().getInput());
            boolean laterOnHeldEndpoint = endpoint == held.endpoint() && ordinal > held.callbackOrdinal();
            boolean replacementReplay = endpoint != null && endpoint != held.endpoint()
                    && endpoint.hasCurrentPrivacyReconnectReplay(seat, viewId, gameViewId, ordinal,
                            turn, phase, turnPlayerViewId, stackSize)
                    && privacyShuffleHistoryState.get() != null
                    && privacyShuffleHistoryState.get().zonesByPlayer().equals(
                            endpoint.currentReconnectReplayZones(ordinal));
            return current.proxyInput() == held.input() && current.queuedInput() == held.input()
                    && held.input().getOwner() != null && held.input().getOwner().getId() == viewId
                    && (laterOnHeldEndpoint || replacementReplay);
        }

        long privacyShuffleT4FirstPriorityOrdinal() {
            PrivacyShuffleT4PriorityInput held = privacyShuffleT4PriorityInput;
            return held == null ? -1 : held.callbackOrdinal();
        }
        synchronized void configurePrivacyScenario(int hostViewId, int remoteViewId, int tellingTimeId,
                int fengrafId, int fengrafAbilityId, int publicCreatureId, int daysUndoingId,
                List<Integer> privateTopIds, List<Integer> initialLibraryIds,
                List<Integer> remoteManaIds, List<Integer> hostManaIds,
                String expectedRevealMessage, String expectedSacrificeMessage) {
            if (!privacyScenario || hostViewId < 0 || remoteViewId < 0 || hostViewId == remoteViewId || privateTopIds.size() != 3 || remoteManaIds.size() != 2 || hostManaIds.size() != 3 || initialLibraryIds.size() <= 3 || new LinkedHashSet<>(privateTopIds).size() != 3) {
                fail(new AssertionError("Privacy setup did not retain its finite two-seat card roles"));
                return;
            }
            privacyHostViewId = hostViewId;
            privacyRemoteViewId = remoteViewId;
            privacyTellingTimeId = tellingTimeId;
            privacyDaysUndoingId = daysUndoingId;
            privacyFengrafId = fengrafId;
            privacyFengrafAbilityId = fengrafAbilityId;
            privacyPublicCreatureId = publicCreatureId;
            privacyTopIds = List.copyOf(privateTopIds);
            privacyInitialLibraryIds = List.copyOf(initialLibraryIds);
            privacyRemoteManaIds = List.copyOf(remoteManaIds);
            privacyHostManaIds = List.copyOf(hostManaIds);
            privacyExpectedRevealMessage = expectedRevealMessage;
            privacyExpectedSacrificeMessage = expectedSacrificeMessage;
        }
        synchronized void bindPrivacyViews(int hostViewId, int remoteViewId) {
            if (!privacyScenario || hostViewId < 0 || remoteViewId < 0 || hostViewId == remoteViewId) {
                fail(new AssertionError("Privacy scenario did not receive two distinct assigned PlayerViews"));
                return;
            }
            privacyHostViewId = hostViewId;
            privacyRemoteViewId = remoteViewId;
        }
        synchronized void bindPrivacyHostGui(ScriptedGui gui) {
            if (!privacyScenario || gui == null || gui.seat != 0 || gui.remote) {
                fail(new AssertionError("Privacy host endpoint was not the assigned local GUI"));
                return;
            }
            privacyHostGui = gui;
        }
        synchronized Map<Integer, List<Boolean>> privacyHostVisibility(List<Integer> ids) {
            if (privacyHostGui == null) {
                fail(new AssertionError("Privacy host visibility was sampled before endpoint binding"));
                return Map.of();
            }
            return privacyHostGui.visibilityFacts(ids, privacyHostViewId);
        }
        synchronized void attachPrivacyObservation(ReconnectObservation observation) {
            if (!privacyScenario || privacyObservation != null || observation == null) {
                fail(new AssertionError("Privacy receive observation was absent or attached more than once"));
                return;
            }
            privacyObservation = observation;
        }
        synchronized void recordPrivacyAbility(PrivacyAbilityReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || receipt.seat() == 1 && receipt.eventId() < 0 || receipt.seat() == 0 && receipt.eventId() != -1) {
                fail(new AssertionError("Privacy ability callback lacks its real received-event provenance"));
                return;
            }
            if (receipt.cardId() == privacyTellingTimeId) {
                String expectedRole = privacyTellingTimeAbilityChoices == 0
                        ? "Telling Time initial spell choice" : "Telling Time additional-cost choice";
                if (receipt.seat() != privacyTellingTimeSeat || receipt.abilityId() < 0 || privacyPriorityStage != 6 && privacyPriorityStage != 7 || privacyTellingTimeAbilityChoices >= 2 || !expectedRole.equals(receipt.role())) {
                    fail(new AssertionError("Telling Time did not receive its two finite actual ability-choice callbacks"));
                    return;
                }
                privacyTellingTimeAbilityChoices++;
            } else if (false && receipt.cardId() == privacyDaysUndoingId) {
                if (receipt.seat() != 0 || receipt.abilityId() < 0 || privacyPriorityStage != 14 || !"Day's Undoing actual host spell choice".equals(receipt.role())) {
                    fail(new AssertionError("Day's Undoing ability callback was not the actual turn-3 host spell"));
                    return;
                }
            } else if (receipt.cardId() == privacyFengrafId) {
                String expectedRole = privacyFengrafAbilityChoices == 0
                        ? "Haunted Fengraf activation ability" : "Haunted Fengraf additional-cost choice";
                if (receipt.seat() != privacyFengrafSeat || receipt.abilityId() != privacyFengrafAbilityId || privacyPriorityStage != 10 || privacyFengrafAbilityChoices >= 2 || !expectedRole.equals(receipt.role())) {
                    fail(new AssertionError("Haunted Fengraf did not receive its two finite actual ability-choice callbacks"));
                    return;
                }
                privacyFengrafAbilityChoices++;
            } else if (false && privacyDaysUndoingCastOrdinal > 0
                    && privacyPriorityStage == 14 && privacyHostManaIds.contains(receipt.cardId())
                    && receipt.callbackOrdinal() > privacyDaysUndoingCastOrdinal) {
                if (receipt.seat() != 0 || receipt.abilityId() < 0 || privacyPriorityStage != 14 || !"selected Island mana source".equals(receipt.role()) || !privacyHistoryManaAbilityChoices.add(receipt.cardId()) || !actions.stream().anyMatch(action -> action.decision() == Decision.PAY_DAYS_UNDOING
                                && Objects.equals(action.cardId(), receipt.cardId()))) {
                    fail(new AssertionError("Day's Undoing did not use its three actual distinct host Island abilities"));
                    return;
                }
            } else if (privacyRemoteManaIds.contains(receipt.cardId()) || privacyHostManaIds.contains(receipt.cardId())) {
                int expectedSeat = privacyRemoteManaIds.contains(receipt.cardId())
                        ? privacyTellingTimeSeat : privacyFengrafSeat;
                if (receipt.seat() != expectedSeat || receipt.abilityId() < 0 || !"selected Island mana source".equals(receipt.role()) || !privacyManaAbilityChoices.add(receipt.cardId()) || !privacyManaSourceSelected(receipt.cardId())) {
                    fail(new AssertionError("Island mana ability did not match one distinct scripted payment source"));
                    return;
                }
            } else {
                fail(new AssertionError("Unassigned card reached a privacy ability-choice callback"));
                return;
            }
            privacyAbilities.add(receipt);
            if (receipt.cardId() == privacyTellingTimeId && receipt.seat() == privacyTellingTimeSeat && privacyPriorityStage == 6) {
                privacyPriorityStage = 7;
            }
            if (receipt.cardId() == privacyFengrafId && receipt.seat() == privacyFengrafSeat && privacyPriorityStage == 10) {
                privacyFengrafAbilityChosen = true;
            }
        }
        synchronized void recordPrivacyChoice(PrivacyChoiceReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || (receipt.seat() == 1 && receipt.eventId() < 0) || (receipt.seat() == 0 && receipt.eventId() != -1) || receipt.cardIds().isEmpty()) {
                fail(new AssertionError("Private choice callback lacks its real received-event provenance"));
                return;
            }
            privacyChoices.add(receipt);
            privacyTellingTimeChoiceCount++;
        }
        void markPrivacyHostChoiceHeld() {
            if (!privacyScenario || privacyTellingTimeSeat != 0 || privacyHostViewId < 0) {
                fail(new AssertionError("Held Telling Time choice was not the assigned local chooser"));
                return;
            }
            privacyHostChoiceActive = true;
            privacyHostChoiceHeld.countDown();
        }
        void signalPrivacyHostChoiceFailure() { privacyHostChoiceHeld.countDown(); }
        boolean awaitPrivacyHostChoiceHeld(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyHostChoiceHeld.await(timeout, unit);
        }
        boolean awaitPrivacyHostChoiceRelease(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyHostChoiceReleased.await(timeout, unit);
        }
        boolean privacyHostChoiceStillHeld() {
            return privacyHostChoiceActive && privacyHostChoiceReleased.getCount() == 1;
        }
        void clearPrivacyHostChoiceActive() { privacyHostChoiceActive = false; }
        void releasePrivacyHostChoice() { privacyHostChoiceReleased.countDown(); }
        synchronized void recordPrivacyReveal(PrivacyRevealReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || (receipt.seat() == 1 && receipt.eventId() < 0) || (receipt.seat() == 0 && receipt.eventId() != -1) || receipt.cardIds().isEmpty()) {
                fail(new AssertionError("Public reveal callback lacks its real received-event provenance"));
                return;
            }
            privacyReveals.add(receipt);
            privacyRevealOrdinal = receipt.callbackOrdinal();
            privacyFengrafReveal.countDown();
        }
        synchronized void recordPrivacyPrompt(PrivacyPromptReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || receipt.chooserViewId() < 0 || receipt.message() == null) {
                fail(new AssertionError("Privacy prompt lacked detached assigned-recipient facts"));
                return;
            }
            privacyPrompts.add(receipt);
        }
        synchronized void observePrivacyState(PrivacyStateReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || receipt.seat() != 1 || privacyTopIds.size() != 3 || privacyInitialLibraryIds.size() <= 3 || privacyHostViewId < 0 || privacyRemoteViewId < 0) return;
            if (privacyDeltaSync && (!"applyDelta".equals(receipt.callback()) || !receipt.stateBearing()
                    || receipt.sequence() < 0)) return;
            if (!privacyDeltaSync && !"setGameView".equals(receipt.callback())) return;
            Map<ZoneType, List<Integer>> host = receipt.zonesByPlayer().get(privacyHostViewId);
            Map<ZoneType, List<Integer>> remote = receipt.zonesByPlayer().get(privacyRemoteViewId);
            if (host == null || remote == null) return;

            List<Integer> library = host.get(ZoneType.Library);
            List<Integer> remoteLibrary = remote.get(ZoneType.Library);
            Map<ZoneType, List<Integer>> tellingTimeActor = receipt.zonesByPlayer()
                    .get(privacyViewIdForSeat(privacyTellingTimeSeat));
            List<Integer> expectedLibrary = new ArrayList<>();
            expectedLibrary.add(privacyTopIds.get(1));
            expectedLibrary.addAll(privacyInitialLibraryIds.subList(3, privacyInitialLibraryIds.size()));
            expectedLibrary.add(privacyTopIds.get(2));
            if (library != null && library.equals(remoteLibrary) && library.equals(expectedLibrary) && tellingTimeActor != null && tellingTimeActor.get(ZoneType.Hand) != null && tellingTimeActor.get(ZoneType.Hand).contains(privacyTopIds.get(0))) {
                privacyTellingTimeState.compareAndSet(null, receipt);
                privacyTellingTimeResolved.countDown();
            }
            Map<ZoneType, List<Integer>> fengrafActor = receipt.zonesByPlayer()
                    .get(privacyViewIdForSeat(privacyFengrafSeat));
            List<Integer> hand = fengrafActor == null ? null : fengrafActor.get(ZoneType.Hand);
            List<Integer> graveyard = host.get(ZoneType.Graveyard);
            long revealOrdinal = privacyRevealOrdinal;
            if (revealOrdinal > 0 && receipt.stackSize() == 0 && hand != null && hand.contains(privacyPublicCreatureId) && graveyard != null && !graveyard.contains(privacyPublicCreatureId)) {
                if (privacyFengrafState.compareAndSet(null, receipt)) privacyFengrafStateArrived.countDown();
            }
        }
        boolean privacyScenario() { return privacyScenario;
        }

        int privacyDaysUndoingId() { return privacyDaysUndoingId;
        }

        int privacyHostViewId() { return privacyHostViewId;
        }
        int privacyRemoteViewId() { return privacyRemoteViewId;
        }
        int privacyTellingTimeId() { return privacyTellingTimeId;
        }
        int privacyFengrafId() { return privacyFengrafId;
        }
        int privacyFengrafAbilityId() { return privacyFengrafAbilityId;
        }
        int privacyPublicCreatureId() { return privacyPublicCreatureId;
        }
        int privacyTellingTimeChoiceCount() { return privacyTellingTimeChoiceCount;
        }
        int privacyTellingTimeAbilityChoices() { return privacyTellingTimeAbilityChoices;
        }
        int privacyFengrafAbilityChoices() { return privacyFengrafAbilityChoices;
        }
        int privacyPriorityStage() { return privacyPriorityStage;
        }
        int privacyExpectedManaCount() { return privacyExpectedManaCount;
        }
        List<Integer> privacyTopIds() { return privacyTopIds;
        }
        List<Integer> privacyInitialLibraryIds() { return privacyInitialLibraryIds;
        }
        List<Integer> privacyRemoteManaIds() { return privacyRemoteManaIds;
        }
        List<Integer> privacyHostManaIds() { return privacyHostManaIds;
        }
        String privacyExpectedRevealMessage() { return privacyExpectedRevealMessage;
        }
        String privacyExpectedSacrificeMessage() { return privacyExpectedSacrificeMessage;
        }
        long privacyConfirmRepaintOrdinal() { return privacyConfirmRepaintOrdinal;
        }
        boolean privacyConfirmationRepaintPending() { return privacyConfirmationRepaintPending;
        }
        ReconnectObservation privacyObservation() { return privacyObservation;
        }
        List<PrivacyChoiceReceipt> privacyChoices() { synchronized (privacyChoices) { return List.copyOf(privacyChoices); } }
        List<PrivacyAbilityReceipt> privacyAbilities() { synchronized (privacyAbilities) { return List.copyOf(privacyAbilities); } }
        List<PrivacyRevealReceipt> privacyReveals() { synchronized (privacyReveals) { return List.copyOf(privacyReveals); } }
        List<PrivacyPromptReceipt> privacyPrompts() { synchronized (privacyPrompts) { return List.copyOf(privacyPrompts); } }
        List<PrivacyStackViewReceipt> privacyStackViewReceipts() {
            synchronized (privacyStackViewReceipts) { return List.copyOf(privacyStackViewReceipts); }
        }
        List<PrivacyPaymentReceipt> privacyPaymentReceipts() {
            synchronized (privacyPaymentReceipts) { return List.copyOf(privacyPaymentReceipts); }
        }
        Map<Integer, Integer> privacyTellingTimeStartDrawCounts() { return privacyTellingTimeStartDrawCounts; }
        long privacyTellingTimeStartDrawOrdinal() { return privacyTellingTimeStartDrawOrdinal;
        }
        long privacyTellingTimeFenceOrdinal() { return privacyTellingTimeFenceOrdinal;
        }
        long privacyFengrafFenceOrdinal() { return privacyFengrafFenceOrdinal;
        }
        long privacyRevealOrdinal() { return privacyRevealOrdinal;
        }
        synchronized void recordPrivacyTellingTimeStartDrawCounts(int seat, int viewId, long ordinal,
                int turn, PhaseType phase, Map<Integer, Integer> drawCounts) {
            if (!privacyScenario || seat != privacyTellingTimeSeat || viewId != privacyViewIdForSeat(privacyTellingTimeSeat) || turn != 1 || phase != PhaseType.MAIN1 || ordinal <= 0 || drawCounts.getOrDefault(privacyHostViewId, -1) < 0 || drawCounts.getOrDefault(privacyRemoteViewId, -1) < 0 || privacyPriorityStage != 6 || !privacyTellingTimeCastDispatched || !privacyTellingTimeStartDrawCounts.isEmpty()) {
                fail(new AssertionError("Telling Time draw baseline was not captured at the actual remote MAIN1 callback"));
                return;
            }
            privacyTellingTimeStartDrawCounts = Map.copyOf(drawCounts);
            privacyTellingTimeStartDrawOrdinal = ordinal;
        }
        PrivacyStateReceipt privacyTellingTimeState() { return privacyTellingTimeState.get();
        }
        PrivacyStateReceipt privacyFengrafState() { return privacyFengrafState.get();
        }
        synchronized void recordPrivacyStackView(PrivacyStackViewReceipt receipt) {
            if (!privacyScenario || receipt.callbackOrdinal() <= 0 || receipt.seat() < 0 || receipt.seat() > 1 || receipt.sourceCardId() < 0 || receipt.activatorViewId() != privacyViewIdForSeat(receipt.sourceCardId() == privacyTellingTimeId
                            ? privacyTellingTimeSeat : receipt.sourceCardId() == privacyDaysUndoingId
                                    ? 0 : privacyFengrafSeat)) {
                fail(new AssertionError("Privacy stack view receipt did not retain the assigned source and activator"));
                return;
            }
            privacyStackViewReceipts.add(receipt);
        }
        synchronized PrivacyStackPriorityCheckpoint capturePrivacyStackPriority(ScriptedGui endpoint,
                IGameController endpointController, GameView view, int seat, int viewId, long ordinal,
                int turn, PhaseType phase, int turnPlayerViewId, SpellAbility ability,
                PrivacyPaymentReceipt payment) {
            Game game = privacyGame;
            PlayerView endpointOwner = view == null ? null : ScriptedGui.findPlayerView(view, viewId);
            if (endpoint == null || endpoint.seat != seat || endpoint.remote != (seat == 1) || endpointController == null || endpointOwner == null || view == null || ordinal <= 0 || game == null || game.isGameOver() || game.getOutcome() != null || game.getPlayers().stream().anyMatch(Player::conceded) || seat < 0 || seat >= game.getPlayers().size() || game.getPlayers().get(seat).getId() != viewId || !(game.getPlayers().get(seat).getController() instanceof PlayerControllerHuman controller) || controller.getGame() != game || game.getView() == null || game.getView().getId() != view.getId() || (seat == 0 && view.getGame() != game) || endpoint.getGameView() != view || view.getTracker() == null || endpoint.getGameController(endpointOwner) != endpointController || (seat == 0 && endpointController != controller) || (seat == 1 && !(endpointController instanceof NetGameController)) || game.getPhaseHandler().getTurn() != turn || game.getPhaseHandler().getPhase() != phase || game.getPhaseHandler().getPlayerTurn() == null || game.getPhaseHandler().getPlayerTurn().getId() != turnPlayerViewId || game.getPhaseHandler().getPriorityPlayer() == null || game.getPhaseHandler().getPriorityPlayer().getId() != viewId || game.getStack() == null || game.getStack().size() != 1 || game.getStack().peekAbility() != ability || ability == null || view.getTurn() != turn || view.getPhase() != phase || view.getPlayerTurn() == null || view.getPlayerTurn().getId() != turnPlayerViewId || view.getStack() == null || view.getStack().size() != 1) {
                fail(new AssertionError("Privacy stack prompt lost its current Game, endpoint, owner, or paid ability"));
                return null;
            }
            StackItemView item = view.getStack().get(0);
            if (item == null || item.getSourceCard() == null || item.getActivatingPlayer() == null || item.getSourceCard().getId() != ability.getHostCard().getId() || item.getActivatingPlayer().getId() != ability.getActivatingPlayer().getId()) {
                fail(new AssertionError("Privacy stack prompt did not match its live ability source and activator"));
                return null;
            }
            PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                    controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
            if (!(inputs.proxyInput() instanceof InputPassPriority input) || inputs.proxyInput() != inputs.queuedInput() || input.getOwner() == null || input.getOwner().getId() != viewId) {
                fail(new AssertionError("Privacy stack prompt lacks its exact current priority input"));
                return null;
            }
            GameViewReceipt applied = endpoint.gameViewReceipts().stream()
                    .filter(receipt -> receipt.gameViewId() == view.getId())
                    .max(Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
            if (applied == null || applied.trackerIdentity() != view.getTracker()) {
                fail(new AssertionError("Privacy stack prompt lacks its current applied-view tracker receipt"));
                return null;
            }
            return new PrivacyStackPriorityCheckpoint(game, endpoint, view, view.getTracker(), controller,
                    endpointController, input, seat, viewId, ordinal, turn, phase, turnPlayerViewId,
                    ability, payment);
        }
        synchronized PrivacyPaymentReceipt snapshotPrivacyPayment(SpellAbility ability, long ordinal,
                int sourceId, int activatorViewId) {
            if (ability == null || ability.getHostCard() == null || ability.getActivatingPlayer() == null || ability.getHostCard().getId() != sourceId || ability.getActivatingPlayer().getId() != activatorViewId) {
                fail(new AssertionError("Host stack prompt did not match its live paid ability"));
                return null;
            }
            List<Integer> paidSources = ability.getPayingMana().stream().map(Mana::getSourceCard)
                    .filter(Objects::nonNull).map(Card::getId).toList();
            List<Integer> paidColors = ability.getPayingMana().stream()
                    .map(mana -> (int) mana.getColor()).toList();
            List<Integer> paidAbilitySources = ability.getPayingManaAbilities().stream()
                    .map(SpellAbility::getHostCard).filter(Objects::nonNull).map(Card::getId).toList();
            List<Integer> sacrificed = new ArrayList<>();
            boolean sourceTapped = false;
            for (Card card : ability.getPaidList("Sacrificed")) {
                sacrificed.add(card.getId());
                if (card.getId() == sourceId && card.isTapped()) sourceTapped = true;
            }
            return new PrivacyPaymentReceipt(0, privacyHostViewId, ordinal, sourceId, ability.getId(),
                    activatorViewId, paidSources, paidColors, paidAbilitySources, sacrificed, sourceTapped);
        }
        synchronized boolean isRepeatedPrivacyStackPriority(PrivacyStackPriorityCheckpoint current) {
            PrivacyStackPriorityCheckpoint previous = privacyStackPriorityCheckpoint;
            if (previous == null) return false;
            PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                    previous.controller().getInputProxy().getInput(), previous.controller().getInputQueue().getInput());
            if (inputs.proxyInput() != previous.input() || inputs.queuedInput() != previous.input()) return false;
            if (current.input() != previous.input() || current.game() != previous.game() || current.endpoint() != previous.endpoint() || current.view() != previous.view() || current.tracker() != previous.tracker() || current.controller() != previous.controller() || current.endpointController() != previous.endpointController() || current.seat() != previous.seat() || current.viewId() != previous.viewId() || current.turn() != previous.turn() || current.phase() != previous.phase() || current.turnPlayerViewId() != previous.turnPlayerViewId() || current.ability() != previous.ability() || !samePrivacyPaymentFacts(current.payment(), previous.payment())) {
                fail(new AssertionError("Repeated privacy stack callback changed its retained current input context"));
                return true;
            }
            if (current.callbackOrdinal() <= previous.callbackOrdinal()) {
                fail(new AssertionError("Repeated privacy stack callback did not advance its callback receipt"));
            }
            return true;
        }
        synchronized boolean beginPrivacyStackPriority(PrivacyStackPriorityCheckpoint current) {
            PrivacyStackPriorityCheckpoint previous = privacyStackPriorityCheckpoint;
            if (previous != null) {
                PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                        previous.controller().getInputProxy().getInput(), previous.controller().getInputQueue().getInput());
                if (inputs.proxyInput() == previous.input() || inputs.queuedInput() == previous.input() || !privacyStackPriorityDispatchInvoked || !isPrivacyStackLifecycleBound(previous)) {
                    fail(new AssertionError("Privacy stack advanced before its exact input was consumed and dispatched"));
                    return false;
                }

                completePrivacyStackPass(previous);
                if (stopped) return false;
            }
            privacyStackPriorityCheckpoint = current;
            privacyStackPriorityDispatchInvoked = false;
            return true;
        }
        synchronized boolean reconcilePrivacyStackPassBeforePriority() {
            PrivacyStackPriorityCheckpoint previous = privacyStackPriorityCheckpoint;
            if (previous == null) return true;
            PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                    previous.controller().getInputProxy().getInput(), previous.controller().getInputQueue().getInput());
            if (inputs.proxyInput() == previous.input() || inputs.queuedInput() == previous.input() || !privacyStackPriorityDispatchInvoked || !isPrivacyStackLifecycleBound(previous)) {
                fail(new AssertionError("Next privacy priority arrived before the retained stack action was consumed"));
                return false;
            }
            completePrivacyStackPass(previous);
            privacyStackPriorityCheckpoint = null;
            privacyStackPriorityDispatchInvoked = false;
            return !stopped;
        }
        synchronized void reserveAndDispatchPrivacyStackPass(PrivacyStackPriorityCheckpoint checkpoint,
                Runnable action) {
            if (privacyStackPriorityCheckpoint != checkpoint || privacyStackPriorityDispatchInvoked || !isCurrentPrivacyStackPriority(checkpoint)) {
                fail(new AssertionError("Privacy stack pass lost its exact reserved current input"));
                return;
            }
            // Publish invocation before signaling the input: resolution callbacks may run before this returns.
            privacyStackPriorityDispatchInvoked = true;
            try {
                action.run();
            } catch (Throwable failure) {
                fail(failure);
            }
        }
        private boolean isCurrentPrivacyStackPriority(PrivacyStackPriorityCheckpoint checkpoint) {
            Game game = checkpoint.game();
            PlayerView endpointOwner = ScriptedGui.findPlayerView(checkpoint.view(), checkpoint.viewId());
            if (privacyGame != game || game.isGameOver() || game.getOutcome() != null || game.getPlayers().stream().anyMatch(Player::conceded) || game.getView() == null || game.getView().getId() != checkpoint.view().getId() || game.getPhaseHandler().getTurn() != checkpoint.turn() || game.getPhaseHandler().getPhase() != checkpoint.phase() || game.getPhaseHandler().getPlayerTurn() == null || game.getPhaseHandler().getPlayerTurn().getId() != checkpoint.turnPlayerViewId() || game.getPhaseHandler().getPriorityPlayer() == null || game.getPhaseHandler().getPriorityPlayer().getId() != checkpoint.viewId() || game.getStack() == null || game.getStack().size() != 1 || game.getStack().peekAbility() != checkpoint.ability() || game.getPlayers().get(checkpoint.seat()).getController() != checkpoint.controller() || checkpoint.controller().getGame() != game || endpointOwner == null || checkpoint.endpoint().getGameView() != checkpoint.view() || checkpoint.view().getTracker() != checkpoint.tracker() || checkpoint.view().getTurn() != checkpoint.turn() || checkpoint.view().getPhase() != checkpoint.phase() || checkpoint.view().getPlayerTurn() == null || checkpoint.view().getPlayerTurn().getId() != checkpoint.turnPlayerViewId() || checkpoint.view().getStack() == null || checkpoint.view().getStack().size() != 1 || checkpoint.endpoint().getGameController(endpointOwner) != checkpoint.endpointController()) return false;
            StackItemView item = checkpoint.view().getStack().get(0);
            if (item == null || item.getSourceCard() == null || item.getActivatingPlayer() == null || item.getSourceCard().getId() != checkpoint.ability().getHostCard().getId() || item.getActivatingPlayer().getId() != checkpoint.ability().getActivatingPlayer().getId()) return false;
            if (checkpoint.payment() != null) {
                PrivacyPaymentReceipt currentPayment = snapshotPrivacyPayment(checkpoint.ability(),
                        checkpoint.payment().callbackOrdinal(), checkpoint.payment().sourceCardId(),
                        checkpoint.payment().activatorViewId());
                if (currentPayment == null || !samePrivacyPaymentFacts(checkpoint.payment(), currentPayment)) return false;
            }
            PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                    checkpoint.controller().getInputProxy().getInput(), checkpoint.controller().getInputQueue().getInput());
            return inputs.proxyInput() == checkpoint.input() && inputs.queuedInput() == checkpoint.input()
                    && checkpoint.input().getOwner() != null
                    && checkpoint.input().getOwner().getId() == checkpoint.viewId();
        }
        private boolean isPrivacyStackLifecycleBound(PrivacyStackPriorityCheckpoint checkpoint) {
            Game game = checkpoint.game();
            PlayerView owner = ScriptedGui.findPlayerView(checkpoint.view(), checkpoint.viewId());
            return privacyGame == game && !game.isGameOver() && game.getOutcome() == null
                    && game.getPlayers().stream().noneMatch(Player::conceded)
                    && checkpoint.seat() < game.getPlayers().size()
                    && game.getPlayers().get(checkpoint.seat()).getId() == checkpoint.viewId()
                    && game.getPlayers().get(checkpoint.seat()).getController() == checkpoint.controller()
                    && checkpoint.controller().getGame() == game
                    && game.getView() != null && game.getView().getId() == checkpoint.view().getId()
                    && (checkpoint.seat() != 0 || checkpoint.view().getGame() == game)
                    && checkpoint.endpoint().getGameView() == checkpoint.view()
                    && checkpoint.view().getTracker() == checkpoint.tracker()
                    && owner != null
                    && checkpoint.endpoint().getGameController(owner) == checkpoint.endpointController();
        }

        private void completePrivacyStackPass(PrivacyStackPriorityCheckpoint checkpoint) {
            int sourceId = checkpoint.ability().getHostCard().getId();
            if (sourceId == privacyTellingTimeId) {
                privacyTellingStackPasses++;
                if (privacyTellingStackPasses == 2) privacyPriorityStage = 9;
            } else if (sourceId == privacyFengrafId) {
                privacyFengrafStackPasses++;
                if (privacyFengrafStackPasses == 2) privacyPriorityStage = 12;
            } else
                fail(new AssertionError("Consumed privacy stack pass lost its paid ability identity"));

        }
        private static boolean samePrivacyPaymentFacts(PrivacyPaymentReceipt first, PrivacyPaymentReceipt second) {
            if (first == null || second == null) return first == second;
            return first.seat() == second.seat() && first.chooserViewId() == second.chooserViewId()
                    && first.sourceCardId() == second.sourceCardId() && first.abilityId() == second.abilityId()
                    && first.activatorViewId() == second.activatorViewId()
                    && first.payingManaSourceIds().equals(second.payingManaSourceIds())
                    && first.payingManaColors().equals(second.payingManaColors())
                    && first.payingAbilitySourceIds().equals(second.payingAbilitySourceIds())
                    && first.sacrificedCardIds().equals(second.sacrificedCardIds())
                    && first.sourceTapped() == second.sourceTapped();
        }
        synchronized void recordPrivacyPayment(PrivacyPaymentReceipt receipt, SpellAbility ability) {
            PrivacyPaymentBinding previous = privacyPaymentBindings.stream()
                    .filter(binding -> binding.receipt().sourceCardId() == receipt.sourceCardId())
                    .findFirst().orElse(null);
            if (previous != null) {
                if (previous.ability() == ability && samePrivacyPaymentFacts(previous.receipt(), receipt)) return;
                fail(new AssertionError("Privacy payment receipt changed its retained paid ability or immutable facts"));
                return;
            }
            if (!privacyScenario || receipt.seat() != 0 || receipt.chooserViewId() != privacyHostViewId || receipt.callbackOrdinal() <= 0 || receipt.abilityId() < 0 || ability == null || ability.getId() != receipt.abilityId() || (receipt.sourceCardId() != privacyTellingTimeId && receipt.sourceCardId() != privacyFengrafId
                            && (!false || receipt.sourceCardId() != privacyDaysUndoingId)) || receipt.activatorViewId() != privacyViewIdForSeat(receipt.sourceCardId() == privacyTellingTimeId
                            ? privacyTellingTimeSeat : privacyFengrafSeat) || privacyPaymentReceipts.stream().anyMatch(existing -> existing.sourceCardId() == receipt.sourceCardId())) {
                fail(new AssertionError("Privacy payment receipt was not one unique actual host stack checkpoint"));
                return;
            }
            privacyPaymentReceipts.add(receipt);
            privacyPaymentBindings.add(new PrivacyPaymentBinding(ability, receipt));

        }
        synchronized void releasePrivacyInitialPriority() {
            PostKeepReceipt held = postKeepReceipt;
            if (!privacyScenario || privacyPriorityStage != 0 || held == null || held.prioritySeat() != 0 || held.heldTurn() != 1 || held.heldPhase() != PhaseType.UNTAP || held.heldTurnPlayerViewId() != privacyHostViewId) {
                fail(new AssertionError("Privacy scenario did not start at the actual host turn-1 UNTAP priority"));
                return;
            }
            privacyPriorityStage = 1;
            actions.add(new ActionReceipt(Decision.PASS_PRIORITY, 0, privacyHostViewId, null, true));
            releasePostKeepPriority();
        }

        synchronized PrivacyPriorityAction routePrivacyPriority(int seat, int viewId, long ordinal,
                int gameViewId, int turn, PhaseType phase, int turnPlayerViewId, int stackSize,
                ScriptedGui endpoint) {

            if (!privacyScenario || ordinal <= 0 || turn != 1 || phase == null || stackSize < 0) {
                if (privacyScenario) fail(new AssertionError("Invalid privacy priority callback"));
                return null;
            }
            if (privacyPriorityStage == 1) {
                if (seat != 1 || viewId != privacyRemoteViewId || phase != PhaseType.UNTAP || turnPlayerViewId != privacyHostViewId || stackSize != 0) return privacyPriorityFailure("remote UNTAP");
                privacyPriorityStage = 2;
                actions.add(new ActionReceipt(Decision.PASS_PRIORITY, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS;
            }
            if (privacyPriorityStage == 2) {
                if (seat != 0 || viewId != privacyHostViewId || phase != PhaseType.UPKEEP || turnPlayerViewId != privacyHostViewId || stackSize != 0) return privacyPriorityFailure("host UPKEEP");
                privacyPriorityStage = 3;
                actions.add(new ActionReceipt(Decision.PASS_PRIORITY, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS;
            }
            if (privacyPriorityStage == 3) {
                if (seat != 1 || viewId != privacyRemoteViewId || phase != PhaseType.UPKEEP || turnPlayerViewId != privacyHostViewId || stackSize != 0) return privacyPriorityFailure("remote UPKEEP");
                privacyPriorityStage = 4;
                actions.add(new ActionReceipt(Decision.PASS_PRIORITY, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS;
            }
            if (privacyPriorityStage == 4) {
                if (seat != 0 || viewId != privacyHostViewId || phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 0) return privacyPriorityFailure("host MAIN1");
                if (privacyTellingTimeSeat == 0) return dispatchPrivacyTellingTime(seat, viewId);
                privacyPriorityStage = 5;
                actions.add(new ActionReceipt(Decision.PASS_PRIORITY, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS;
            }
            if (privacyPriorityStage == 5) {
                if (seat != privacyTellingTimeSeat || viewId != privacyViewIdForSeat(privacyTellingTimeSeat) || phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 0 || privacyTellingTimeCastDispatched) {
                    return privacyPriorityFailure("remote Telling Time cast");
                }
                return dispatchPrivacyTellingTime(seat, viewId);
            }
            if (privacyPriorityStage == 7) {
                int expectedSeat = privacyTellingStackPasses == 0
                        ? privacyTellingTimeSeat : 1 - privacyTellingTimeSeat;
                if (seat != expectedSeat || viewId != playerViewIdForSeat(seat) || phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 1 || privacyTellingStackPasses >= 2) {
                    return privacyPriorityFailure("Telling Time stack");
                }
                actions.add(new ActionReceipt(Decision.PASS_PRIVACY_STACK, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS_STACK;
            }
            if (privacyPriorityStage == 9) {
                if (phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 0) {
                    return privacyPriorityFailure("post-Telling Time actor priority");
                }
                if (privacyFengrafSeat == 0) {
                    if (seat != 0 || viewId != privacyHostViewId || privacyFengrafActorPriorityHeld) {
                        return privacyPriorityFailure("post-Telling Time host priority");
                    }
                    privacyFengrafActorPriorityHeld = true;
                } else if (!privacyHostPassedForFengraf) {
                    if (seat != 0 || viewId != privacyHostViewId) return privacyPriorityFailure("host pass to Fengraf actor");
                    privacyHostPassedForFengraf = true;
                    privacyTellingTimeFenceOrdinal = ordinal;
                    actions.add(new ActionReceipt(Decision.PASS_PRIORITY, seat, viewId, null, true));
                    return PrivacyPriorityAction.PASS;
                } else {
                    if (seat != 1 || viewId != privacyRemoteViewId || privacyFengrafActorPriorityHeld) {
                        return privacyPriorityFailure("post-Telling Time remote Fengraf priority");
                    }
                    privacyFengrafActorPriorityHeld = true;
                }
                if (seat == 0) privacyTellingTimeFenceOrdinal = ordinal;
                privacyFengrafActorPriority.countDown();
                return PrivacyPriorityAction.HOLD_TELLING_TIME;
            }
            if (privacyPriorityStage == 10) {
                if (stackSize != 1) return privacyPriorityFailure("Fengraf payment/stack");
                privacyPriorityStage = 11;
            }
            if (privacyPriorityStage == 11) {
                int expectedSeat = privacyFengrafStackPasses == 0
                        ? privacyFengrafSeat : 1 - privacyFengrafSeat;
                if (seat != expectedSeat || viewId != playerViewIdForSeat(seat) || phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 1 || privacyFengrafStackPasses >= 2) {
                    return privacyPriorityFailure("Fengraf stack");
                }
                actions.add(new ActionReceipt(Decision.PASS_PRIVACY_STACK, seat, viewId, null, true));
                return PrivacyPriorityAction.PASS_STACK;
            }
            if (privacyPriorityStage == 12) {
                if (seat != 0 || viewId != privacyHostViewId || phase != PhaseType.MAIN1 || turnPlayerViewId != privacyHostViewId || stackSize != 0) return privacyPriorityFailure("post-Fengraf host priority");
                privacyFengrafFenceOrdinal = ordinal;
                privacyFengrafResolved.countDown();
                return PrivacyPriorityAction.HOLD_FENGRAF;
            }
            return privacyPriorityFailure("unexpected priority stage " + privacyPriorityStage);
        }

        private PrivacyPriorityAction dispatchPrivacyTellingTime(int seat, int viewId) {
            if (seat != privacyTellingTimeSeat || viewId != privacyViewIdForSeat(privacyTellingTimeSeat) || privacyTellingTimeCastDispatched) return privacyPriorityFailure("Telling Time actor");
            privacyTellingTimeCastDispatched = true;
            privacyPriorityStage = 6;
            privacyExpectedManaCount = 2;
            privacyManaSelections = 0;
            actions.add(new ActionReceipt(Decision.CAST_TELLING_TIME, seat, viewId, privacyTellingTimeId, true));
            return PrivacyPriorityAction.CAST_TELLING_TIME;
        }
        private PrivacyPriorityAction privacyPriorityFailure(String checkpoint) {
            fail(new AssertionError("Unexpected priority callback at " + checkpoint));
            return null;
        }
        boolean awaitPrivacyFengrafActorPriority(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyFengrafActorPriority.await(timeout, unit);
        }
        boolean awaitPrivacyTellingTimeResolved(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyTellingTimeResolved.await(timeout, unit);
        }
        boolean awaitPrivacyFengrafResolved(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyFengrafResolved.await(timeout, unit);
        }

        synchronized boolean isSamePrivacyShuffleT4PriorityInput() {
            PrivacyShuffleT4PriorityInput held = privacyShuffleT4PriorityInput;
            if (held == null || privacyGame != held.game() || held.game().getPlayers().get(1).getController() != held.controller() || held.controller().getGame() != held.game() || held.game().getView() == null || held.game().getView().getId() != held.gameViewId() || held.game().getPhaseHandler().getTurn() != 4 || held.game().getPhaseHandler().getPhase() != PhaseType.UPKEEP || held.game().getPhaseHandler().getPlayerTurn() == null || held.game().getPhaseHandler().getPlayerTurn().getId() != held.ownerViewId() || held.game().getPhaseHandler().getPriorityPlayer() == null || held.game().getPhaseHandler().getPriorityPlayer().getId() != held.ownerViewId() || held.game().getStack() == null || !held.game().getStack().isEmpty() || held.game().isGameOver() || held.game().getOutcome() != null) return false;
            PrivacyInputSnapshot current = new PrivacyInputSnapshot(
                    held.controller().getInputProxy().getInput(), held.controller().getInputQueue().getInput());
            return current.proxyInput() == held.input() && current.queuedInput() == held.input()
                    && held.input().getOwner() != null && held.input().getOwner().getId() == held.ownerViewId();
        }

        boolean awaitPrivacyFengrafState(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyFengrafStateArrived.await(timeout, unit);
        }
        boolean awaitPrivacyFengrafReveal(long timeout, TimeUnit unit) throws InterruptedException {
            return privacyFengrafReveal.await(timeout, unit);
        }
        synchronized int nextPrivacyManaSource(int seat, int chooserViewId) {

            boolean tellingTimePayment = privacyPriorityStage < 10;
            int expectedSeat = tellingTimePayment ? privacyTellingTimeSeat : privacyFengrafSeat;
            List<Integer> ids = tellingTimePayment ? privacyRemoteManaIds : privacyHostManaIds;
            if (!privacyScenario || seat != expectedSeat || chooserViewId != privacyViewIdForSeat(expectedSeat) || privacyExpectedManaCount <= 0 || privacyManaSelections >= privacyExpectedManaCount) {
                fail(new AssertionError("Mana prompt exceeded its configured finite privacy action"));
                return -1;
            }
            for (Integer id : ids) {
                if (!privacyManaSourceSelected(id)) {
                    privacyManaSelections++;
                    actions.add(new ActionReceipt(tellingTimePayment ? Decision.PAY_TELLING_TIME : Decision.PAY_FENGRAF,
                            seat, chooserViewId, id, false));
                    return id;
                }
            }
            fail(new AssertionError("All distinct configured privacy mana sources were already selected"));
            return -1;
        }
        private boolean privacyManaSourceSelected(int cardId) {
            synchronized (actions) {
                return actions.stream().anyMatch(action -> (action.decision() == Decision.PAY_TELLING_TIME
                        || action.decision() == Decision.PAY_FENGRAF) && Objects.equals(action.cardId(), cardId));
            }
        }
        synchronized void beginPrivacyFengraf() {
            if (!privacyScenario || privacyPriorityStage != 9 || !privacyFengrafActorPriorityHeld || privacyTellingTimeState.get() == null || privacyFengrafDispatchPending) {
                fail(new AssertionError("Fengraf activation did not begin from the retained post-Telling Time priority"));
                return;
            }
            privacyPriorityStage = 10;
            privacyExpectedManaCount = 3;
            privacyManaSelections = 0;
            privacyFengrafDispatchPending = true;
        }
        synchronized void recordPrivacyFengrafDispatch(int seat, int viewId) {
            if (!privacyScenario || !privacyFengrafDispatchPending || seat != privacyFengrafSeat || viewId != privacyViewIdForSeat(privacyFengrafSeat)) {
                fail(new AssertionError("Fengraf was not activated by its assigned actor"));
                return;
            }
            privacyFengrafDispatchPending = false;
            actions.add(new ActionReceipt(Decision.ACTIVATE_FENGRAF, seat, viewId, privacyFengrafId, true));
        }
        synchronized void recordPrivacySacrificeConfirmation(int seat, int viewId, long buttonsOrdinal,
                long promptOrdinal, String message, int cardId, PrivacyInputSnapshot inputSnapshot) {
            if (privacyFirstSacrificeInput == null && inputSnapshot != null) {
                privacyFirstSacrificeInput = inputSnapshot.proxyInput();
            }
            if (!privacyScenario || privacyPriorityStage != 10 || !privacyFengrafAbilityChosen || seat != privacyFengrafSeat || viewId != privacyViewIdForSeat(privacyFengrafSeat) || buttonsOrdinal <= 0 || promptOrdinal <= buttonsOrdinal || !privacyExpectedSacrificeMessage.equals(message) || privacyFengrafConfirmOrdinal > 0 || !privacyConfirmationPending || !isCurrentPrivacySacrificeInput(inputSnapshot)) {
                fail(new AssertionError("Fengraf confirmation predicate failed: stage=" + privacyPriorityStage
                        + ", abilityChosen=" + privacyFengrafAbilityChosen + ", seat=" + seat
                        + ", view=" + viewId + "/" + privacyHostViewId + ", buttonsOrdinal=" + buttonsOrdinal
                        + ", promptOrdinal=" + promptOrdinal + ", expectedMessage=" + privacyExpectedSacrificeMessage
                        + ", message=" + message + ", card=" + cardId + ", alreadyConfirmed="
                        + privacyFengrafConfirmOrdinal + ", firstInputMatches="
                        + (inputSnapshot != null && inputSnapshot.proxyInput() == privacyFirstSacrificeInput)
                        + ", firstInput=" + PrivacyInputSnapshot.describeInput(privacyFirstSacrificeInput)
                        + ", " + (inputSnapshot == null ? "inputSnapshot=null" : inputSnapshot.evidence())));
                return;
            }
            privacyFengrafConfirmOrdinal = (int) promptOrdinal;
            privacyFengrafConfirmMessage = message;
            privacyConfirmButtonsOrdinal = buttonsOrdinal;
            privacyConfirmationPending = false;
            actions.add(new ActionReceipt(Decision.PAY_FENGRAF, seat, viewId, privacyFengrafId, true));
            if (privacyFengrafSeat == 1) {
                if (privacyOkClientObservation == null || privacyGame == null) {
                    fail(new AssertionError("Remote Fengraf confirmation has no client-send observer"));
                    return;
                }
                privacyOkClientObservation.armSelectButtonOk(privacyGame.getView().getId(), promptOrdinal);
            }
        }
        synchronized void recordPrivacySacrificeButtons(int seat, int viewId, long ordinal,
                String yes, String no, boolean yesEnabled, boolean noEnabled,
                PrivacyInputSnapshot inputSnapshot) {
            if (privacyFirstSacrificeInput == null && inputSnapshot != null && inputSnapshot.proxyInput() instanceof InputConfirm && inputSnapshot.proxyInput() == inputSnapshot.queuedInput()) {
                privacyFirstSacrificeInput = inputSnapshot.proxyInput();
            }
            boolean exactButtons = Localizer.getInstance().getMessage("lblYes").equalsIgnoreCase(yes)
                    && Localizer.getInstance().getMessage("lblNo").equalsIgnoreCase(no);
            boolean sameLiveInput = isCurrentPrivacySacrificeInput(inputSnapshot);
            boolean commonPredicate = privacyScenario && privacyPriorityStage == 10 && privacyFengrafAbilityChosen
                    && seat == privacyFengrafSeat && viewId == privacyViewIdForSeat(privacyFengrafSeat) && ordinal > 0 && exactButtons
                    && yesEnabled && noEnabled && sameLiveInput;
            if (!commonPredicate) {
                fail(new AssertionError("Fengraf button predicate failed: stage=" + privacyPriorityStage
                        + ", abilityChosen=" + privacyFengrafAbilityChosen + ", seat=" + seat
                        + ", view=" + viewId + "/" + privacyHostViewId + ", ordinal=" + ordinal
                        + ", buttons=" + yes + "/" + no + ", exactButtons=" + exactButtons
                        + ", enabled=" + yesEnabled + "/" + noEnabled + ", priorButtons="
                        + privacyConfirmButtonsOrdinal + ", repaintOrdinal=" + privacyConfirmRepaintOrdinal
                        + ", sameLiveInput=" + sameLiveInput
                        + ", firstInput=" + PrivacyInputSnapshot.describeInput(privacyFirstSacrificeInput)
                        + ", confirmationPending=" + privacyConfirmationPending + ", confirmOrdinal="
                        + privacyFengrafConfirmOrdinal + ", confirmMessage=" + privacyFengrafConfirmMessage + ", "
                        + (inputSnapshot == null ? "inputSnapshot=null" : inputSnapshot.evidence())));
                return;
            }
            if (privacyFengrafConfirmOrdinal > privacyConfirmButtonsOrdinal && !privacyConfirmationOkDispatched && privacyConfirmationOkSentNanos == 0 && privacyConfirmRepaintOrdinal <= 0) {
                if (privacyConfirmButtonsOrdinal <= 0 || ordinal <= privacyFengrafConfirmOrdinal || !privacyExpectedSacrificeMessage.equals(privacyFengrafConfirmMessage)) {
                    fail(new AssertionError("Fengraf pre-send repaint did not follow the retained exact confirmation: "
                            + "firstButtons=" + privacyConfirmButtonsOrdinal + ", confirmOrdinal="
                            + privacyFengrafConfirmOrdinal + ", repaintOrdinal=" + ordinal
                            + ", messageMatches=" + privacyExpectedSacrificeMessage.equals(privacyFengrafConfirmMessage)
                            + ", input="
                            + (inputSnapshot == null ? "null" : inputSnapshot.evidence())));
                    return;
                }
                if (privacyPreSendRepaintOrdinal > 0) {
                    fail(new AssertionError("Fengraf repeated pre-send buttons exceeded one exact same-input repaint: ordinal="
                            + ordinal + ", firstPreSendRepaint=" + privacyPreSendRepaintOrdinal + ", "
                            + (inputSnapshot == null ? "inputSnapshot=null" : inputSnapshot.evidence())));
                    return;
                }
                privacyPreSendRepaintOrdinal = ordinal;
                return;
            }
            if (privacyConfirmButtonsOrdinal > 0) {
                long callbackNanos = System.nanoTime();
                boolean exactPostSendInverseWindow = privacyFengrafSeat == 1
                        && privacyConfirmationOkDispatched && privacyConfirmationOkSentNanos > 0
                        && callbackNanos > privacyConfirmationOkSentNanos;
                if (privacyConfirmationPending || privacyFengrafConfirmOrdinal <= privacyConfirmButtonsOrdinal || !privacyExpectedSacrificeMessage.equals(privacyFengrafConfirmMessage) || ordinal <= privacyFengrafConfirmOrdinal || privacyConfirmRepaintOrdinal > 0 || privacyConfirmationOkDispatched && !exactPostSendInverseWindow) {
                    fail(new AssertionError("Fengraf repeated buttons were not a single post-confirmation repaint: stage="
                            + privacyPriorityStage + ", ordinal=" + ordinal + ", firstButtons="
                            + privacyConfirmButtonsOrdinal + ", repaintOrdinal=" + privacyConfirmRepaintOrdinal
                            + ", confirmOrdinal=" + privacyFengrafConfirmOrdinal + ", confirmMessage="
                            + privacyFengrafConfirmMessage + ", confirmationPending=" + privacyConfirmationPending
                            + ", okDispatched=" + privacyConfirmationOkDispatched
                            + ", input=" + (inputSnapshot == null ? "null" : inputSnapshot.evidence())));
                    return;
                }
                privacyConfirmRepaintOrdinal = ordinal;
                privacyPostSendRepaintStage = exactPostSendInverseWindow ? 1 : 0;
                privacyConfirmationRepaintPending = true;
                return;
            }
            if (privacyConfirmationPending || privacyFengrafConfirmOrdinal > 0) {
                fail(new AssertionError("Fengraf initial buttons arrived after confirmation state: ordinal=" + ordinal
                        + ", confirmationPending=" + privacyConfirmationPending + ", confirmOrdinal="
                        + privacyFengrafConfirmOrdinal));
                return;
            }
            privacyConfirmButtonsOrdinal = ordinal;
            privacyConfirmationPending = true;
        }
        private boolean isCurrentPrivacySacrificeInput(PrivacyInputSnapshot snapshot) {
            return snapshot != null && privacyFirstSacrificeInput instanceof InputConfirm
                    && snapshot.proxyInput() == privacyFirstSacrificeInput
                    && snapshot.queuedInput() == privacyFirstSacrificeInput;
        }
        boolean privacySacrificeInputStillCurrent() {
            return privacyScenario && privacyPriorityStage == 10 && privacyFengrafAbilityChosen
                    && privacyFengrafConfirmOrdinal > privacyConfirmButtonsOrdinal
                    && privacyExpectedSacrificeMessage.equals(privacyFengrafConfirmMessage)
                    && !privacyConfirmationRepaintPending
                    && (privacyConfirmRepaintOrdinal <= 0
                            || privacyConfirmRepaintPromptOrdinal > privacyConfirmRepaintOrdinal)
                    && isCurrentPrivacySacrificeInput(privacyInputSnapshotForSeat(privacyFengrafSeat));
        }
        synchronized boolean claimPrivacySacrificeOkDispatch() {
            if (privacyConfirmationOkDispatched || !privacySacrificeInputStillCurrent()) return false;
            privacyConfirmationOkDispatched = true;
            return true;
        }
        synchronized void attachPrivacyClientSendObservation(ClientProtocolObservation observation) {
            if (!privacyScenario || privacyFengrafSeat != 1 || privacyOkClientObservation != null || observation == null || !observation.privacyConfirmationOnly()) {
                fail(new AssertionError("Invalid remote Fengraf client-send observer attachment"));
                return;
            }
            privacyOkClientObservation = observation;
        }
        synchronized void recordPrivacySacrificeClientSend(ClientProtocolSendReceipt receipt) {
            if (!privacyScenario || privacyFengrafSeat != 1 || !privacyConfirmationOkDispatched || privacyGame == null || receipt == null || receipt.method() != ProtocolMethod.selectButtonOk || receipt.gameViewId() == null || receipt.gameViewId() != privacyGame.getView().getId() || receipt.promptCallbackOrdinal() != privacyFengrafConfirmOrdinal || privacyConfirmationOkSentNanos != 0) {
                fail(new AssertionError("Remote Fengraf OK send did not match its unique live confirmation"));
                return;
            }
            privacyConfirmationOkSentNanos = System.nanoTime();
        }
        synchronized void recordPrivacySacrificeRepaintPrompt(long promptOrdinal, String message, int cardId,
                PrivacyInputSnapshot inputSnapshot) {
            long callbackNanos = System.nanoTime();
            boolean exactPostSendInverseWindow = privacyFengrafSeat == 1
                    && privacyConfirmationOkDispatched && privacyConfirmationOkSentNanos > 0
                    && callbackNanos > privacyConfirmationOkSentNanos && privacyPostSendRepaintStage == 1;
            if (!privacyScenario || privacyPriorityStage != 10 || !privacyFengrafAbilityChosen || !privacyConfirmationRepaintPending || promptOrdinal <= privacyConfirmRepaintOrdinal || cardId >= 0 && cardId != privacyFengrafId || !privacyExpectedSacrificeMessage.equals(message) || privacyConfirmationOkDispatched && !exactPostSendInverseWindow || !isCurrentPrivacySacrificeInput(inputSnapshot)) {
                fail(new AssertionError("Fengraf repaint did not retain the exact live confirmation: stage="
                        + privacyPriorityStage + ", promptOrdinal=" + promptOrdinal + ", repaintButtons="
                        + privacyConfirmRepaintOrdinal + ", card=" + cardId + "/" + privacyFengrafId
                        + ", message=" + message + ", expected=" + privacyExpectedSacrificeMessage
                        + ", okDispatched=" + privacyConfirmationOkDispatched
                        + ", input=" + (inputSnapshot == null ? "null" : inputSnapshot.evidence())));
                return;
            }
            privacyConfirmRepaintPromptOrdinal = promptOrdinal;
            privacyConfirmationRepaintPending = false;
            privacyPostSendRepaintStage = exactPostSendInverseWindow ? 2 : 0;
        }
        synchronized void recordPrivacyConcession(int seat, int viewId) {
            if (!privacyScenario || seat != 0 || viewId != privacyHostViewId || privacyConcessionSent || privacyFengrafState.get() == null) {
                fail(new AssertionError("Privacy test terminal action lacked its real host result"));
                return;
            }
            privacyConcessionSent = true;
            actions.add(new ActionReceipt(Decision.CONCEDE, seat, viewId, null, true));
        }
        long privacyConfirmButtonsOrdinal() { return privacyConfirmButtonsOrdinal;
        }
        long privacyPreSendRepaintOrdinal() { return privacyPreSendRepaintOrdinal;
        }
        boolean privacyConfirmationPending() { return privacyConfirmationPending;
        }
        String privacyFengrafConfirmMessage() { return privacyFengrafConfirmMessage;
        }
        int privacyFengrafConfirmOrdinal() { return privacyFengrafConfirmOrdinal;
        }
        long privacyConfirmRepaintPromptOrdinal() { return privacyConfirmRepaintPromptOrdinal;
        }
        boolean privacyPostSendRepaintButtonsAccepted() { return privacyPostSendRepaintStage >= 1;
        }
        boolean privacyPostSendRepaintAccepted() { return privacyPostSendRepaintStage == 2;
        }
        boolean metamorphoseScenario() { return metamorphoseScenario;
        }
        boolean metamorphoseAccept() { return metamorphoseAccept;
        }
        int metamorphoseHostCardId() { return metamorphoseHostCardId;
        }
        int metamorphoseTargetCardId() { return metamorphoseTargetCardId;
        }
        int metamorphoseHandCardId() { return metamorphoseHandCardId;
        }
        int metamorphoseHostViewId() { return metamorphoseHostViewId;
        }
        int metamorphoseRemoteViewId() { return metamorphoseRemoteViewId;
        }
        List<Integer> metamorphoseManaCardIds() { return metamorphoseManaCardIds;
        }
        synchronized int metamorphoseStage() { return metamorphoseStage;
        }
        int metamorphoseAssignedConcessionGameViewId() { return metamorphoseAssignedConcessionGameViewId;
        }
        BufferedMetaSelectableReceipt bufferedMetaSelectableReceipt() { return bufferedMetaSelectableReceipt;
        }
        synchronized boolean recordMetamorphoseAssignedConcession(int seat, int chooserViewId, int gameViewId) {
            if (!lifecycleScenario || !holdMetamorphoseOptional || !metamorphoseScenario || metamorphoseStage != 3 || seat != 1 || chooserViewId != metamorphoseRemoteViewId || gameViewId < 0 || metamorphoseAssignedConcessionGameViewId >= 0) {
                fail(new AssertionError("Assigned-seat concession did not identify the held Meta game view"));
                return false;
            }
            metamorphoseAssignedConcessionGameViewId = gameViewId;
            metamorphoseLifecycleConcessionPending = true;
            return true;
        }
        synchronized void completeMetamorphoseTerminalPresentation(int seat, int gameViewId) {
            if (lifecycleScenario && metamorphoseLifecycleConcessionPending && seat == 1 && gameViewId == metamorphoseAssignedConcessionGameViewId) {
                metamorphoseLifecycleConcessionPending = false;
            }
        }
        synchronized boolean recordBufferedMetaSelectable(int seat, GameView view, long callbackOrdinal,
                int minimum, int maximum, List<Integer> cardIds, List<Integer> privateHandIds) {
            int observedViewId = view == null ? -1 : view.getId();
            boolean gameOver = view != null && view.isGameOver();
            if (!lifecycleScenario || !metamorphoseLifecycleConcessionPending || metamorphoseStage != 3 || seat != 1 || metamorphoseAssignedConcessionGameViewId < 0 || observedViewId != metamorphoseAssignedConcessionGameViewId || !gameOver || callbackOrdinal <= 0 || bufferedMetaSelectableReceipt != null || minimum != 1 || maximum != 1 || !cardIds.contains(metamorphoseHandCardId) || !new LinkedHashSet<>(cardIds).equals(new LinkedHashSet<>(privateHandIds))) {
                fail(new AssertionError("Buffered Meta selector lacks the actual assigned concession's terminal view: "
                        + "seat=" + seat + ",stage=" + metamorphoseStage + ",viewId=" + observedViewId
                        + ",gameOver=" + gameOver + ",concessionViewId="
                        + metamorphoseAssignedConcessionGameViewId + ",min=" + minimum + ",max=" + maximum
                        + ",eligibleOwnHandIds=" + privateHandIds + ",selectorIds=" + cardIds));
                return false;
            }
            bufferedMetaSelectableReceipt = new BufferedMetaSelectableReceipt(observedViewId, seat,
                    callbackOrdinal, minimum, maximum, cardIds, actions().size());
            return true;
        }
        synchronized boolean metamorphoseOptionalPromptArmed() {
            return metamorphoseScenario && metamorphoseStage == 2 && metamorphoseOptionalButtonsOrdinal > 0;
        }
        boolean metamorphoseUpkeepActive() {
            return metamorphoseScenario && metamorphoseStage == 0 && metamorphosePostKeepReleased;
        }
        synchronized void bindMetamorphoseViews(int hostViewId, int remoteViewId) {
            if (!metamorphoseScenario || hostViewId < 0 || remoteViewId < 0 || hostViewId == remoteViewId) {
                fail(new AssertionError("Metamorphose fixture did not receive two distinct assigned PlayerViews: host="
                        + hostViewId + " remote=" + remoteViewId));
                return;
            }
            metamorphoseHostViewId = hostViewId;
            metamorphoseRemoteViewId = remoteViewId;
        }
        synchronized void recordMetamorphoseCast(int seat, int chooserViewId, int cardId, long ordinal,
                                                  List<String> abilityFacts) {
            List<MetamorphoseAbilityReceipt> prior = metamorphoseAbilityReceipts();
            if (!metamorphoseScenario || seat != 0 || chooserViewId != metamorphoseHostViewId || cardId != metamorphoseHostCardId || ordinal <= 0 || metamorphosePreCastPasses != 2 || (prior.isEmpty() && (metamorphoseStage != 0 || !metamorphoseCastDispatchPending)) || (prior.size() == 1 && (metamorphoseStage != 1 || metamorphoseCastDispatchPending
                            || ordinal <= prior.get(0).callbackOrdinal())) || prior.size() > 1) {
                fail(new AssertionError("Unexpected Metamorphose cast ability callback"));
                return;
            }
            if (abilityFacts == null || abilityFacts.size() != 1) {
                fail(new AssertionError("Metamorphose ability callback did not offer exactly one playable ability"));
                return;
            }
            if (!prior.isEmpty() && (!prior.get(0).abilityFacts().equals(abilityFacts)
                    || prior.get(0).seat() != seat || prior.get(0).chooserViewId() != chooserViewId
                    || prior.get(0).cardId() != cardId)) {
                fail(new AssertionError("Metamorphose additional-cost callback changed its sole ability candidate"));
                return;
            }
            String role = prior.isEmpty() ? "initial cast selection" : "additional-cost selection";
            metamorphoseAbilityReceipts.add(new MetamorphoseAbilityReceipt(role, seat, chooserViewId,
                    cardId, ordinal, abilityFacts));
            if (prior.isEmpty()) {
                metamorphoseCastDispatchPending = false;
                metamorphoseStage = 1;
            }
        }
        List<MetamorphoseAbilityReceipt> metamorphoseAbilityReceipts() {
            synchronized (metamorphoseAbilityReceipts) { return List.copyOf(metamorphoseAbilityReceipts); }
        }
        List<MetamorphoseInputButtonsReceipt> metamorphoseInputButtons() {
            synchronized (metamorphoseInputButtons) { return List.copyOf(metamorphoseInputButtons); }
        }
        List<MetamorphoseManaAbilityReceipt> metamorphoseManaAbilityReceipts() {
            synchronized (metamorphoseManaAbilityReceipts) { return List.copyOf(metamorphoseManaAbilityReceipts); }
        }
        synchronized void recordMetamorphoseInputButtons(MetamorphoseInputButtonsReceipt receipt) {
            boolean validPrompt = switch (receipt.prompt()) {
                case "target" -> receipt.seat() == 0 && receipt.chooserViewId() == metamorphoseHostViewId
                        && metamorphoseStage == 1;
                case "private hand" -> receipt.seat() == 1 && receipt.chooserViewId() == metamorphoseRemoteViewId
                        && metamorphoseStage == 4 && metamorphoseAccept;
                default -> false;
            };
            if (!metamorphoseScenario || !validPrompt || receipt.promptOrdinal() <= 0 || receipt.buttonsOrdinal() <= receipt.promptOrdinal() || !Localizer.getInstance().getMessage("lblOK").equalsIgnoreCase(receipt.okLabel()) || !Localizer.getInstance().getMessage("lblCancel").equalsIgnoreCase(receipt.cancelLabel()) || receipt.okEnabled() || !receipt.cancelEnabled()) {
                fail(new AssertionError("Unexpected initial Metamorphose input buttons: " + receipt));
                return;
            }
            metamorphoseInputButtons.add(receipt);
        }
        List<MetamorphoseStackPriorityReceipt> metamorphoseStackPriorities() {
            synchronized (metamorphoseStackPriorities) { return List.copyOf(metamorphoseStackPriorities); }
        }
        synchronized boolean dispatchMetamorphoseCast(int seat, int chooserViewId, int turn,
                PhaseType phase, int turnPlayerViewId, int stackSize) {
            if (metamorphoseCastDispatchPending) return false;
            if (!metamorphoseScenario || seat != 0 || chooserViewId != metamorphoseHostViewId || turn != 1 || phase != PhaseType.MAIN1 || turnPlayerViewId != metamorphoseHostViewId || stackSize != 0 || metamorphoseStage != 0 || metamorphoseUntapPasses != 2 || metamorphosePreCastPasses != 2) {
                fail(new AssertionError("Metamorphose was not dispatched from the assigned host's turn 1 MAIN1 empty-stack priority"));
                return false;
            }
            metamorphoseCastDispatchPending = true;
            actions.add(new ActionReceipt(Decision.CAST_METAMORPHOSE,
                    seat, chooserViewId, metamorphoseHostCardId, true));
            return true;
        }
        synchronized boolean recordMetamorphoseUntapPass(int seat, int turn, PhaseType phase,
                int turnPlayerViewId, int stackSize) {
            if (metamorphoseUntapPassPending) {
                if (seat != metamorphoseExpectedUntapSeat) return false;
                metamorphoseUntapPassPending = false;
            }
            if (!metamorphoseScenario || metamorphoseStage != 0 || !metamorphosePostKeepReleased || metamorphoseUntapPasses != 1 || seat != 1 || turn != 1 || phase != PhaseType.UNTAP || turnPlayerViewId != metamorphoseHostViewId || stackSize != 0) {
                fail(new AssertionError("Unexpected priority before host turn 1 UPKEEP: seat=" + seat
                        + " turn=" + turn + " phase=" + phase + " turnPlayer=" + turnPlayerViewId
                        + " stack=" + stackSize + " untapPasses=" + metamorphoseUntapPasses));
                return false;
            }
            metamorphoseUntapPasses++;
            metamorphoseUntapPassPending = true;
            metamorphoseExpectedUntapSeat = -1;
            actions.add(new ActionReceipt(Decision.ADVANCE_METAMORPHOSE_UNTAP, seat,
                    playerViewIdForSeat(seat), null, true));
            return true;
        }
        synchronized int metamorphoseUntapPasses() { return metamorphoseUntapPasses;
        }
        synchronized boolean recordMetamorphosePreCastPass(int seat, int turn, PhaseType phase,
                int turnPlayerViewId, int stackSize) {
            if (metamorphoseUpkeepPassPending) {
                if (seat != metamorphoseExpectedUpkeepSeat) return false;
                metamorphoseUpkeepPassPending = false;
            }
            int expectedSeat = metamorphosePreCastPasses;
            if (!metamorphoseScenario || metamorphoseStage != 0 || !metamorphosePostKeepReleased || turn != 1 || phase != PhaseType.UPKEEP || turnPlayerViewId != metamorphoseHostViewId || stackSize != 0 || seat != expectedSeat || metamorphosePreCastPasses >= 2 || metamorphoseUntapPasses != 2) {
                fail(new AssertionError("Unexpected priority before the host's turn 1 MAIN1 Metamorphose cast: seat="
                        + seat + " turn=" + turn + " phase=" + phase + " turnPlayer=" + turnPlayerViewId
                        + " stack=" + stackSize + " passCount=" + metamorphosePreCastPasses));
                return false;
            }
            metamorphosePreCastPasses++;
            metamorphoseUpkeepPassPending = true;
            metamorphoseExpectedUpkeepSeat = metamorphosePreCastPasses == 1 ? 1 : -1;
            actions.add(new ActionReceipt(Decision.ADVANCE_METAMORPHOSE_UPKEEP, seat,
                    playerViewIdForSeat(seat), null, true));
            return true;
        }
        synchronized boolean recordMetamorphoseStackPriority(int seat, int chooserViewId, long ordinal,
                int gameViewId, int turn, PhaseType phase, int turnPlayerViewId, int stackItemId,
                int sourceCardId, int activatorViewId, List<Integer> targetCardIds,
                boolean stackItemIsAbility, int stackSize) {
            if (metamorphoseStackPassPending) {
                if (seat != metamorphoseExpectedStackSeat) return false;
                metamorphoseStackPassPending = false;
            }
            Game game = metamorphoseGame;
            PlayerControllerHuman controller = null;
            InputPassPriority priorityInput = null;
            if (game != null && seat >= 0 && seat < game.getPlayers().size() && game.getPlayers().get(seat).getController() instanceof PlayerControllerHuman human) {
                controller = human;
                Input proxyInput = controller.getInputProxy().getInput();
                Input queuedInput = controller.getInputQueue().getInput();
                if (proxyInput instanceof InputPassPriority pass && proxyInput == queuedInput && pass.getOwner() != null && pass.getOwner().getId() == game.getPlayers().get(seat).getId()) {
                    priorityInput = pass;
                }
            }
            if (!metamorphoseScenario || game == null || game.getView().getId() != gameViewId || controller == null || controller.getGame() != game || priorityInput == null || game.getPlayers().get(seat).getId() != chooserViewId || metamorphoseStage < 2 || metamorphoseStage >= 7 || metamorphoseStackPasses >= 2 || seat != metamorphoseStackPasses || chooserViewId != playerViewIdForSeat(seat) || ordinal <= 0 || turn != 1 || phase != PhaseType.MAIN1 || turnPlayerViewId != metamorphoseHostViewId || stackSize != 1 || stackItemId <= 0 || sourceCardId != metamorphoseHostCardId || activatorViewId != metamorphoseHostViewId || stackItemIsAbility || !targetCardIds.equals(List.of(metamorphoseTargetCardId))) {
                fail(new AssertionError("Priority was not for the one actual Metamorphose spell and its host-controlled target"));
                return false;
            }
            metamorphoseStackPasses++;
            metamorphoseStackPassPending = true;
            metamorphoseExpectedStackSeat = metamorphoseStackPasses == 1 ? 1 : -1;
            metamorphoseStackPriorities.add(new MetamorphoseStackPriorityReceipt(seat, chooserViewId,
                    ordinal, game, gameViewId, turn, phase, turnPlayerViewId, stackSize, priorityInput,
                    stackItemId, sourceCardId, activatorViewId, targetCardIds));
            if (seat == 1) {
                actions.add(new ActionReceipt(Decision.PASS_METAMORPHOSE_STACK, seat, chooserViewId, null, true));
            } else {
                metamorphoseFirstStackPriority.countDown();
            }
            return true;
        }
        boolean awaitMetamorphoseFirstStackPriority(long timeout, TimeUnit unit) throws InterruptedException {
            return metamorphoseFirstStackPriority.await(timeout, unit);
        }
        synchronized boolean holdMetamorphoseFirstStackPriority() {
            return metamorphoseScenario && metamorphoseStackPasses == 1;
        }
        synchronized void recordMetamorphoseFirstStackPass(int seat, int chooserViewId) {
            if (!holdMetamorphoseFirstStackPriority() || metamorphoseFirstStackPriorityReleased || seat != 0 || chooserViewId != metamorphoseHostViewId) {
                fail(new AssertionError("The held first Metamorphose stack priority was not answered by its host"));
                return;
            }
            metamorphoseFirstStackPriorityReleased = true;
            actions.add(new ActionReceipt(Decision.PASS_METAMORPHOSE_STACK, seat, chooserViewId, null, true));
        }
        synchronized void recordMetamorphoseManaSelection(int seat, int chooserViewId, int sourceCardId) {
            if (!metamorphoseScenario || seat != 0 || chooserViewId != metamorphoseHostViewId || !metamorphoseManaCardIds.contains(sourceCardId)) {
                fail(new AssertionError("Metamorphose mana selection was not an assigned host Island"));
                return;
            }
            actions.add(new ActionReceipt(Decision.PAY_METAMORPHOSE, seat, chooserViewId, sourceCardId, false));
        }
        synchronized void recordMetamorphoseManaAbility(int seat, int chooserViewId, int sourceCardId,
                long ordinal, List<String> abilityFacts) {
            List<MetamorphoseManaAbilityReceipt> prior = metamorphoseManaAbilityReceipts();
            int expectedIndex = prior.size();
            if (!metamorphoseScenario || seat != 0 || chooserViewId != metamorphoseHostViewId || metamorphoseStage != 2 || expectedIndex >= metamorphoseManaCardIds.size() || metamorphoseManaCardIds.get(expectedIndex) != sourceCardId || ordinal <= 0 || prior.stream().anyMatch(receipt -> receipt.callbackOrdinal() >= ordinal) || abilityFacts == null || abilityFacts.size() != 1) {
                fail(new AssertionError("Unexpected Metamorphose sole Island mana-ability callback for " + sourceCardId));
                return;
            }
            metamorphoseManaAbilityReceipts.add(new MetamorphoseManaAbilityReceipt(seat, chooserViewId,
                    sourceCardId, ordinal, abilityFacts));
        }
        synchronized void recordMetamorphoseHeldPriorityPass(int seat, int chooserViewId) {
            if (!metamorphoseScenario || metamorphoseUntapPasses != 0 || metamorphosePreCastPasses != 0 || seat != 0 || chooserViewId != metamorphoseHostViewId || postKeepReceipt == null || postKeepReceipt.prioritySeat() != seat || postKeepReceipt.heldTurn() != 1 || postKeepReceipt.heldPhase() != PhaseType.UNTAP || postKeepReceipt.heldTurnPlayerViewId() != chooserViewId) {
                fail(new AssertionError("Held host turn 1 untap was not the first actual Metamorphose priority"));
                return;
            }
            metamorphoseUntapPasses = 1;
            metamorphoseUntapPassPending = true;
            metamorphoseExpectedUntapSeat = 1;
            actions.add(new ActionReceipt(Decision.ADVANCE_METAMORPHOSE_UNTAP,
                    seat, chooserViewId, null, true));
        }
        synchronized boolean recordMetamorphoseTerminalConcession(int seat, int chooserViewId, int turn,
                PhaseType phase, int turnPlayerViewId, int stackSize) {
            if (metamorphoseConcessionSent) return false;
            if (!metamorphoseScenario || metamorphoseResolution.get() == null || metamorphoseStage != 7 || seat != 0 || chooserViewId != metamorphoseHostViewId || turn != 1 || phase != PhaseType.MAIN1 || turnPlayerViewId != metamorphoseHostViewId || stackSize != 0) {
                fail(new AssertionError("Host concession was not dispatched from the actual postresolution empty-stack prompt"));
                return false;
            }
            metamorphoseConcessionSent = true;
            actions.add(new ActionReceipt(Decision.CONCEDE, seat, chooserViewId, null, true));
            return true;
        }
        void recordMetamorphosePostResolutionPriority(long ordinal, int turn, PhaseType phase,
                int turnPlayerViewId, int stackSize,
                Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
            if (!metamorphoseScenario || ordinal <= 0 || turn != 1 || phase != PhaseType.MAIN1 || turnPlayerViewId != metamorphoseHostViewId || stackSize != 0 || metamorphoseStage < 6) return;
            Map<ZoneType, List<Integer>> host = zonesByPlayer.get(metamorphoseHostViewId);
            if (host == null || host.get(ZoneType.Library).isEmpty() || host.get(ZoneType.Library).get(0) != metamorphoseTargetCardId || !host.get(ZoneType.Graveyard).contains(metamorphoseHostCardId)) return;
            if (metamorphosePostResolutionPriority.compareAndSet(null,
                    new MetamorphosePostResolutionPriority(ordinal, zonesByPlayer))) {
                metamorphosePostResolutionPrompt.countDown();
            }
        }
        boolean awaitMetamorphosePostResolutionPriority(long timeout, TimeUnit unit) throws InterruptedException {
            return metamorphosePostResolutionPrompt.await(timeout, unit);
        }
        MetamorphosePostResolutionPriority metamorphosePostResolutionPriority() {
            return metamorphosePostResolutionPriority.get();
        }
        synchronized void recordMetamorphoseTargetSelected(int seat, int chooserViewId, long ordinal) {
            if (!metamorphoseScenario || seat != 0 || chooserViewId != metamorphoseHostViewId || metamorphoseStage != 1 || ordinal <= metamorphoseTargetCallbackOrdinal) {
                fail(new AssertionError("Metamorphose target selection did not follow its offered target list"));
                return;
            }
            metamorphoseStage = 2;
        }
        synchronized void recordMetamorphoseOptional(int chooserSeat, int chooserViewId, String message, long ordinal,
                int gameViewId, PrivacyInputSnapshot inputSnapshot) {
            if (!metamorphoseScenario || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || ordinal <= metamorphoseOptionalButtonsOrdinal || metamorphoseOptionalButtonsOrdinal <= 0 || message == null || message.isBlank() || metamorphoseStage != 2) {
                fail(new AssertionError("Unexpected Metamorphose optional choice callback: " + chooserSeat + "/" + chooserViewId + "/" + message));
                return;
            }
            if (!message.equals(metamorphoseOptionalMessage)) {
                fail(new AssertionError("Metamorphose optional prompt did not match the exact target/source message: " + message));
                return;
            }
            metamorphoseChoiceCallbackOrdinal = ordinal;
            metamorphoseOptionalAcceptedPrompt = new MetamorphoseAcceptedPrompt("optional", chooserSeat,
                    gameViewId, ordinal, metamorphoseOptionalButtonsOrdinal, inputSnapshot);
            metamorphoseStage = 3;
            humanChoiceReceipts.add(new HumanChoiceReceipt(chooserSeat, chooserViewId, ordinal,
                    "Metamorphose optional question", message, List.of(metamorphoseTargetCardId)));
        }
        synchronized void recordMetamorphoseOptionalButtons(int chooserSeat, int chooserViewId,
                long ordinal, String yes, String no, boolean yesEnabled, boolean noEnabled) {
            if (!metamorphoseScenario || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || metamorphoseStage != 2 || ordinal <= 0 || !yesEnabled || !noEnabled || !Localizer.getInstance().getMessage("lblYes").equalsIgnoreCase(yes) || !Localizer.getInstance().getMessage("lblNo").equalsIgnoreCase(no)) {
                fail(new AssertionError("Metamorphose optional buttons were absent or did not match actual enabled Yes/No"));
                return;
            }
            metamorphoseOptionalButtonsOrdinal = ordinal;
        }
        synchronized void recordMetamorphoseHeldOptionalButtonRepaint(int chooserSeat, int chooserViewId,
                int gameViewId, long ordinal, String yes, String no, boolean yesEnabled, boolean noEnabled,
                PrivacyInputSnapshot currentInput) {
            MetamorphoseAcceptedPrompt accepted = metamorphoseOptionalAcceptedPrompt;
            PrivacyInputSnapshot originalInput = accepted == null ? null : accepted.inputSnapshot();
            boolean valid = metamorphoseScenario && heldMetamorphoseOptionalBeforeLifecycleConcession()
                    && metamorphoseStage == 3
                    && accepted != null && "optional".equals(accepted.stage())
                    && chooserSeat == 1 && accepted.seat() == chooserSeat
                    && chooserViewId == metamorphoseRemoteViewId
                    && accepted.gameViewId() == gameViewId && ordinal > accepted.promptOrdinal()
                    && accepted.buttonsOrdinal() == metamorphoseOptionalButtonsOrdinal
                    && accepted.buttonsOrdinal() > 0 && yesEnabled && noEnabled
                    && Localizer.getInstance().getMessage("lblYes").equalsIgnoreCase(yes)
                    && Localizer.getInstance().getMessage("lblNo").equalsIgnoreCase(no)
                    && originalInput != null && originalInput.proxyInput() instanceof InputConfirm
                    && originalInput.proxyInput() == originalInput.queuedInput()
                    && currentInput != null && currentInput.proxyInput() == originalInput.proxyInput()
                    && currentInput.queuedInput() == originalInput.queuedInput()
                    && metamorphoseOptionalResponseQueued == 0 && metamorphoseOptionalResponseDispatched == 0
                    && !metamorphoseOptionalResponseArmed;
            if (!valid) {
                fail(new AssertionError("Held Metamorphose optional repaint changed its accepted prompt, enabled Yes/No "
                        + "pair, current InputConfirm, or no-send state: seat=" + chooserSeat + ",view="
                        + chooserViewId + ",gameView=" + gameViewId + ",ordinal=" + ordinal));
                return;
            }
            lastMetamorphoseHeldOptionalRepaint = new MetamorphoseHeldOptionalRepaintReceipt(
                    "updateButtons", gameViewId, ordinal, chooserSeat, chooserViewId,
                    ++metamorphoseHeldOptionalRepaintCount);
        }
        synchronized void recordMetamorphoseDeclinedOptionalButtonRepaint(int chooserSeat, int chooserViewId,
                int gameViewId, long ordinal, String yes, String no, boolean yesEnabled, boolean noEnabled,
                PrivacyInputSnapshot currentInput) {
            MetamorphoseAcceptedPrompt accepted = metamorphoseOptionalAcceptedPrompt;
            PrivacyInputSnapshot originalInput = accepted == null ? null : accepted.inputSnapshot();
            List<MetamorphoseClientSendReceipt> sends;
            synchronized (metamorphoseClientSendReceipts) {
                sends = List.copyOf(metamorphoseClientSendReceipts);
            }
            boolean valid = metamorphoseScenario && !metamorphoseAccept && metamorphoseStage == 6
                    && accepted != null && "optional".equals(accepted.stage())
                    && chooserSeat == 1 && accepted.seat() == chooserSeat
                    && chooserViewId == metamorphoseRemoteViewId
                    && accepted.gameViewId() == gameViewId && ordinal > accepted.promptOrdinal()
                    && accepted.buttonsOrdinal() == metamorphoseOptionalButtonsOrdinal
                    && accepted.buttonsOrdinal() > 0 && yesEnabled && noEnabled
                    && Localizer.getInstance().getMessage("lblYes").equalsIgnoreCase(yes)
                    && Localizer.getInstance().getMessage("lblNo").equalsIgnoreCase(no)
                    && originalInput != null && originalInput.proxyInput() instanceof InputConfirm
                    && originalInput.proxyInput() == originalInput.queuedInput()
                    && currentInput != null && currentInput.proxyInput() == originalInput.proxyInput()
                    && currentInput.queuedInput() == originalInput.queuedInput()
                    && metamorphoseOptionalResponseQueued == 1 && metamorphoseOptionalResponseDispatched == 1
                    && metamorphoseOptionalResponseArmed && sends.size() == 1
                    && sends.get(0).method() == ProtocolMethod.selectButtonCancel
                    && sends.get(0).gameViewMatches()
                    && sends.get(0).promptOrdinal() == accepted.promptOrdinal();
            if (!valid) {
                fail(new AssertionError("Declined Metamorphose optional repaint changed its accepted prompt, enabled "
                        + "Yes/No pair, current InputConfirm, or single Cancel response: seat=" + chooserSeat
                        + ",view=" + chooserViewId + ",gameView=" + gameViewId + ",ordinal=" + ordinal));
            }
        }
        synchronized void recordMetamorphoseHeldOptionalPromptRepaint(int chooserSeat, int chooserViewId,
                int gameViewId, long ordinal, String message, PrivacyInputSnapshot currentInput) {
            MetamorphoseAcceptedPrompt accepted = metamorphoseOptionalAcceptedPrompt;
            PrivacyInputSnapshot originalInput = accepted == null ? null : accepted.inputSnapshot();
            boolean valid = metamorphoseScenario && heldMetamorphoseOptionalBeforeLifecycleConcession()
                    && metamorphoseStage == 3
                    && accepted != null && "optional".equals(accepted.stage())
                    && chooserSeat == 1 && accepted.seat() == chooserSeat
                    && chooserViewId == metamorphoseRemoteViewId
                    && accepted.gameViewId() == gameViewId && ordinal > accepted.promptOrdinal()
                    && accepted.buttonsOrdinal() == metamorphoseOptionalButtonsOrdinal
                    && accepted.buttonsOrdinal() > 0 && metamorphoseOptionalMessage.equals(message)
                    && originalInput != null && originalInput.proxyInput() instanceof InputConfirm
                    && originalInput.proxyInput() == originalInput.queuedInput()
                    && currentInput != null && currentInput.proxyInput() == originalInput.proxyInput()
                    && currentInput.queuedInput() == originalInput.queuedInput()
                    && metamorphoseOptionalResponseQueued == 0 && metamorphoseOptionalResponseDispatched == 0
                    && !metamorphoseOptionalResponseArmed;
            if (!valid) {
                fail(new AssertionError("Held Metamorphose optional prompt repaint changed its exact accepted "
                        + "message, game/seat, current InputConfirm, or no-send state: seat=" + chooserSeat
                        + ",view=" + chooserViewId + ",gameView=" + gameViewId + ",ordinal=" + ordinal));
                return;
            }
            lastMetamorphoseHeldOptionalRepaint = new MetamorphoseHeldOptionalRepaintReceipt(
                    "showPromptMessage", gameViewId, ordinal, chooserSeat, chooserViewId,
                    ++metamorphoseHeldOptionalRepaintCount);
        }
        int metamorphoseHeldOptionalRepaintCount() { return metamorphoseHeldOptionalRepaintCount;
        }
        MetamorphoseHeldOptionalRepaintReceipt lastMetamorphoseHeldOptionalRepaint() {
            return lastMetamorphoseHeldOptionalRepaint;
        }
        synchronized void recordMetamorphoseTarget(int chooserSeat, int chooserViewId, long ordinal,
                                                    int minimum, int maximum, List<Integer> ids) {
            if (!metamorphoseScenario || chooserSeat != 0 || chooserViewId != metamorphoseHostViewId || metamorphoseStage != 1 || metamorphoseAbilityReceipts().size() != 2 || minimum != 1 || maximum != 1 || !ids.equals(List.of(metamorphoseTargetCardId)) || ordinal <= 0 || ordinal <= metamorphoseAbilityReceipts().get(1).callbackOrdinal()) {
                fail(new AssertionError("Unexpected Metamorphose target selection callback: " + ids + " min=" + minimum + " max=" + maximum));
                return;
            }
            selectablesReceipts.add(new SelectablesReceipt(chooserSeat, chooserViewId, ordinal,
                    "Metamorphose target", minimum, maximum, ids));
            metamorphoseTargetCallbackOrdinal = ordinal;
        }
        synchronized void recordMetamorphoseHandChoices(int chooserSeat, int chooserViewId, long ordinal,
                                                         int minimum, int maximum, List<Integer> ids,
                                                         List<Integer> privateHandIds) {
            if (!metamorphoseScenario || !metamorphoseAccept || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || metamorphoseStage != 4 || minimum != 1 || maximum != 1 || !ids.contains(metamorphoseHandCardId) || !new LinkedHashSet<>(ids).equals(new LinkedHashSet<>(privateHandIds)) || ordinal <= 0) {
                fail(new AssertionError("Unexpected Metamorphose B-hand choice list: " + ids));
                return;
            }
            selectablesReceipts.add(new SelectablesReceipt(chooserSeat, chooserViewId, ordinal,
                    "Metamorphose private hand", minimum, maximum, ids));
        }
        synchronized void recordMetamorphoseHandPrompt(int chooserSeat, int chooserViewId, long ordinal,
                String message, List<Integer> offeredIds, int gameViewId, long buttonsOrdinal,
                PrivacyInputSnapshot inputSnapshot) {
            long selectableOrdinal = selectablesReceipts().stream()
                    .filter(receipt -> receipt.stage().equals("Metamorphose private hand")
                            && receipt.seat() == chooserSeat && receipt.chooserViewId() == chooserViewId)
                    .mapToLong(SelectablesReceipt::callbackOrdinal).findFirst().orElse(-1);
            if (!metamorphoseScenario || !metamorphoseAccept || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || metamorphoseStage != 4 || ordinal <= selectableOrdinal || selectableOrdinal <= 0 || offeredIds.isEmpty() || !offeredIds.contains(metamorphoseHandCardId)) {
                fail(new AssertionError("Unexpected Metamorphose private hand prompt for seat " + chooserSeat));
                return;
            }
            metamorphoseChooserCallbackOrdinal = ordinal;
            if (metamorphoseHandAcceptedPrompt == null) {
                metamorphoseHandAcceptedPrompt = new MetamorphoseAcceptedPrompt("private hand", chooserSeat,
                        gameViewId, ordinal, buttonsOrdinal, inputSnapshot);
            }
            humanChoiceReceipts.add(new HumanChoiceReceipt(chooserSeat, chooserViewId, ordinal,
                    "Metamorphose private hand", message, offeredIds));
            metamorphoseStage = 5;
        }
        synchronized void captureMetamorphoseHandPrompt(int chooserSeat, int chooserViewId, long ordinal,
                int gameViewId, PrivacyInputSnapshot inputSnapshot) {
            if (metamorphoseScenario && metamorphoseAccept && chooserSeat == 1 && chooserViewId == metamorphoseRemoteViewId && metamorphoseStage == 4 && ordinal > 0 && metamorphoseHandAcceptedPrompt == null) {
                metamorphoseHandAcceptedPrompt = new MetamorphoseAcceptedPrompt("private hand", chooserSeat,
                        gameViewId, ordinal, -1, inputSnapshot);
            }
        }
        synchronized void attachMetamorphoseClientSendObservation(ClientProtocolObservation observation) {
            if (!metamorphoseScenario || metamorphoseClientSendObservation != null || observation == null || !observation.metamorphoseDiagnosticOnly()) return;
            metamorphoseClientSendObservation = observation;
        }
        synchronized void queueMetamorphoseResponse(String stage, int gameViewId, long promptOrdinal,
                ProtocolMethod method) {
            boolean armed = metamorphoseClientSendObservation != null
                    && metamorphoseClientSendObservation.armMetamorphoseResponse(method, gameViewId, promptOrdinal);
            if ("optional".equals(stage)) {
                metamorphoseOptionalResponseQueued++;
                metamorphoseOptionalResponseArmed = armed;
            } else if ("private hand".equals(stage)) {
                metamorphoseHandResponseQueued++;
                metamorphoseHandResponseArmed = armed;
            }
        }
        synchronized void dispatchMetamorphoseResponse(String stage) {
            if ("optional".equals(stage)) metamorphoseOptionalResponseDispatched++;
            else if ("private hand".equals(stage)) metamorphoseHandResponseDispatched++;
        }
        void recordMetamorphoseClientSend(MetamorphoseClientSendReceipt receipt) {
            if (metamorphoseScenario && receipt != null) metamorphoseClientSendReceipts.add(receipt);
        }
        String metamorphoseRemoteRepaintDiagnostic(int callbackSeat, int callbackOwnerViewId,
                int callbackGameViewId, long callbackOrdinal, boolean enabledOne, boolean enabledTwo) {
            PrivacyInputSnapshot current = metamorphoseRemoteInputSnapshot(metamorphoseGame);
            MetamorphoseAcceptedPrompt optional = metamorphoseOptionalAcceptedPrompt;
            MetamorphoseAcceptedPrompt hand = metamorphoseHandAcceptedPrompt;
            MetamorphoseInputButtonsReceipt handButtons = metamorphoseInputButtons().stream()
                    .filter(receipt -> "private hand".equals(receipt.prompt()) && hand != null
                            && receipt.seat() == hand.seat() && receipt.chooserViewId() == metamorphoseRemoteViewId
                            && receipt.promptOrdinal() == hand.promptOrdinal())
                    .findFirst().orElse(null);
            long handButtonsOrdinal = handButtons == null ? -1 : handButtons.buttonsOrdinal();
            List<MetamorphoseClientSendReceipt> sends;
            synchronized (metamorphoseClientSendReceipts) { sends = List.copyOf(metamorphoseClientSendReceipts); }
            return " metamorphoseRemoteRepaint{stage=" + metamorphoseStage()
                    + ",seat=" + callbackSeat + ",ownerView=" + callbackOwnerViewId
                    + ",gameView=" + callbackGameViewId + ",callbackOrdinal=" + callbackOrdinal
                    + ",enabled=" + enabledOne + "/" + enabledTwo
                    + ",optionalAccepted=" + (optional != null)
                    + ",optionalSeat=" + (optional == null ? -1 : optional.seat())
                    + ",optionalGameView=" + (optional == null ? -1 : optional.gameViewId())
                    + ",sameOptionalGameView=" + (optional != null && optional.gameViewId() == callbackGameViewId)
                    + ",optionalPromptOrdinal=" + (optional == null ? -1 : optional.promptOrdinal())
                    + ",optionalButtonsOrdinal=" + (optional == null ? -1 : optional.buttonsOrdinal())
                    + ",optionalProxyType=" + inputType(optional == null ? null : optional.inputSnapshot(), true)
                    + ",optionalQueueType=" + inputType(optional == null ? null : optional.inputSnapshot(), false)
                    + ",optionalProxyEqualsQueue=" + proxyEqualsQueue(optional == null ? null : optional.inputSnapshot())
                    + ",handAccepted=" + (hand != null)
                    + ",handSeat=" + (hand == null ? -1 : hand.seat())
                    + ",handGameView=" + (hand == null ? -1 : hand.gameViewId())
                    + ",sameHandGameView=" + (hand != null && hand.gameViewId() == callbackGameViewId)
                    + ",handPromptOrdinal=" + (hand == null ? -1 : hand.promptOrdinal())
                    + ",handButtonsOrdinal=" + handButtonsOrdinal
                    + ",handButtonsMatchAccepted=" + (handButtons != null)
                    + ",handProxyType=" + inputType(hand == null ? null : hand.inputSnapshot(), true)
                    + ",handQueueType=" + inputType(hand == null ? null : hand.inputSnapshot(), false)
                    + ",handProxyEqualsQueue=" + proxyEqualsQueue(hand == null ? null : hand.inputSnapshot())
                    + ",currentProxyType=" + inputType(current, true)
                    + ",currentQueueType=" + inputType(current, false)
                    + ",currentProxyEqualsQueue=" + proxyEqualsQueue(current)
                    + ",sameOptionalProxy=" + sameInput(optional == null ? null : optional.inputSnapshot(), current, true)
                    + ",sameOptionalQueue=" + sameInput(optional == null ? null : optional.inputSnapshot(), current, false)
                    + ",sameHandProxy=" + sameInput(hand == null ? null : hand.inputSnapshot(), current, true)
                    + ",sameHandQueue=" + sameInput(hand == null ? null : hand.inputSnapshot(), current, false)
                    + ",optionalQueuedDispatched=" + metamorphoseOptionalResponseQueued + "/"
                    + metamorphoseOptionalResponseDispatched + ",optionalSendArmed=" + metamorphoseOptionalResponseArmed
                    + ",handQueuedDispatched=" + metamorphoseHandResponseQueued + "/"
                    + metamorphoseHandResponseDispatched + ",handSendArmed=" + metamorphoseHandResponseArmed
                    + ",observedResponseReceiptCount=" + sends.size() + ",sends=" + sends.stream()
                            .map(send -> send.method().name() + ":" + send.eventId()).toList()
                    + ",sendGameViewMatches=" + sends.stream().map(MetamorphoseClientSendReceipt::gameViewMatches).toList()
                    + ",sendPromptOrdinals=" + sends.stream().map(MetamorphoseClientSendReceipt::promptOrdinal).toList()
                    + "}";
        }
        private static String inputType(PrivacyInputSnapshot snapshot, boolean proxy) {
            if (snapshot == null) return "none";
            Input input = proxy ? snapshot.proxyInput() : snapshot.queuedInput();
            return input == null ? "null" : input.getClass().getSimpleName();
        }
        private static boolean sameInput(PrivacyInputSnapshot accepted, PrivacyInputSnapshot current, boolean proxy) {
            if (accepted == null || current == null) return false;
            Input acceptedInput = proxy ? accepted.proxyInput() : accepted.queuedInput();
            Input currentInput = proxy ? current.proxyInput() : current.queuedInput();
            return acceptedInput != null && currentInput != null && acceptedInput == currentInput;
        }
        private static boolean proxyEqualsQueue(PrivacyInputSnapshot snapshot) {
            return snapshot != null && snapshot.proxyInput() != null && snapshot.queuedInput() != null
                    && snapshot.proxyInput() == snapshot.queuedInput();
        }
        synchronized void recordMetamorphoseHandDispatch(int chooserSeat, int chooserViewId, long promptOrdinal) {
            if (!metamorphoseScenario || !metamorphoseAccept || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || metamorphoseStage != 5 || promptOrdinal != metamorphoseChooserCallbackOrdinal) {
                fail(new AssertionError("Metamorphose hand selection was not dispatched from the actual B chooser prompt"));
                return;
            }
            actions.add(new ActionReceipt(Decision.CHOOSE_METAMORPHOSE_HAND, chooserSeat,
                    chooserViewId, metamorphoseHandCardId, true));
            metamorphoseStage = 6;
        }
        synchronized void recordMetamorphoseDecision(boolean accepted, int chooserSeat, int chooserViewId, long ordinal) {
            if (!metamorphoseScenario || chooserSeat != 1 || chooserViewId != metamorphoseRemoteViewId || accepted != metamorphoseAccept || metamorphoseStage != 3 || ordinal != metamorphoseChoiceCallbackOrdinal) {
                fail(new AssertionError("Metamorphose optional choice did not match the scripted decision"));
                return;
            }
            humanChoiceReceipts.add(new HumanChoiceReceipt(chooserSeat, chooserViewId, ordinal,
                    accepted ? "Metamorphose optional yes" : "Metamorphose optional no",
                    metamorphoseOptionalMessage, List.of(metamorphoseTargetCardId)));
            metamorphoseStage = accepted ? 4 : 6;
        }
        synchronized void observeMetamorphoseState(GameView view, Capture capture, int receivingSeat) {
            if (!metamorphoseScenario || view == null || capture == null || receivingSeat != 1 || metamorphoseStage < 6 || metamorphoseResolution.get() != null) return;
            if (!"setGameView".equals(capture.callback()) && !("applyDelta".equals(capture.callback()) && capture.stateBearing())) return;
            PlayerView host = null;
            PlayerView recipient = null;
            for (PlayerView player : view.getPlayers()) {
                if (player.getId() == metamorphoseHostViewId) host = player;
                if (player.getId() == metamorphoseRemoteViewId) recipient = player;
            }
            if (host == null || recipient == null || recipient.getHand() == null) return;
            List<Integer> library = cardIds(recipient.getCards(ZoneType.Library));
            List<Integer> hostGraveyard = cardIds(host.getCards(ZoneType.Graveyard));
            List<Integer> recipientHand = cardIds(recipient.getHand());
            List<Integer> recipientBattlefield = cardIds(recipient.getCards(ZoneType.Battlefield));
            boolean expectedChoiceZone = metamorphoseAccept
                    ? recipientBattlefield.contains(metamorphoseHandCardId)
                    : recipientHand.contains(metamorphoseHandCardId);
            if (!view.isDanDan() || library.isEmpty() || library.get(0) != metamorphoseTargetCardId || !hostGraveyard.contains(metamorphoseHostCardId) || !expectedChoiceZone) return;
            MetamorphoseResolutionReceipt receipt = new MetamorphoseResolutionReceipt(capture.callback(),
                    capture.sequence(), capture.callbackOrdinal(), receivingSeat, metamorphoseHostCardId,
                    metamorphoseTargetCardId, metamorphoseHandCardId, library, hostGraveyard,
                    recipientHand, recipientBattlefield, snapshotMetamorphoseZones(view));
            if (metamorphoseResolution.compareAndSet(null, receipt)) {
                metamorphoseStage = 7;
                metamorphoseResolved.countDown();
            }
        }
        private List<Integer> cardIds(Iterable<CardView> cards) {
            List<Integer> ids = new ArrayList<>();
            if (cards instanceof Collection<?> collection) {
                for (Object value : collection.toArray()) {
                    if (value instanceof CardView card) ids.add(card.getId());
                    else throw new AssertionError("Card collection contains a non-CardView entry");
                }
            } else if (cards != null) {
                for (CardView card : cards) ids.add(card.getId());
            }
            return List.copyOf(ids);
        }
        private Map<Integer, Map<ZoneType, List<Integer>>> snapshotMetamorphoseZones(GameView view) {
            Map<Integer, Map<ZoneType, List<Integer>>> result = new LinkedHashMap<>();
            for (PlayerView player : view.getPlayers()) {
                EnumMap<ZoneType, List<Integer>> zones = new EnumMap<>(ZoneType.class);
                for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                        ZoneType.Graveyard, ZoneType.Exile)) {
                    zones.put(zone, cardIds(player.getCards(zone)));
                }
                result.put(player.getId(), Collections.unmodifiableMap(zones));
            }
            return Collections.unmodifiableMap(result);
        }
        boolean awaitMetamorphoseResolved(long timeout, TimeUnit unit) throws InterruptedException {
            return metamorphoseResolved.await(timeout, unit);
        }
        MetamorphoseResolutionReceipt metamorphoseResolution() { return metamorphoseResolution.get();
        }
        String metamorphoseOptionalMessage() { return metamorphoseOptionalMessage;
        }
        List<SelectablesReceipt> selectablesReceipts() { synchronized (selectablesReceipts) { return List.copyOf(selectablesReceipts); } }
        List<HumanChoiceReceipt> humanChoiceReceipts() { synchronized (humanChoiceReceipts) { return List.copyOf(humanChoiceReceipts); } }
        synchronized void recordOpeningKeep(int seat, int viewId, long ordinal, int gameViewId,
                               String callback, String scriptStep, List<Integer> handIds, String snapshotDiagnostic) {
            if ((metamorphoseScenario || privacyScenario) && (handIds.size() != (privacyMulliganPrefixScenario() ? (seat == 0 ? 7 : 6) : 7)
                    || ordinal <= 0 || seat < 0 || seat > 1)) {
                boolean opening;
                synchronized (keepOpeningReceipts) {
                    opening = keepOpeningReceipts.stream().noneMatch(receipt -> receipt.seat() == seat);
                }
                fail(new AssertionError("TCP setup did not preserve the actual assigned opening before Keep"
                        + ": seat=" + seat + ",chooserViewId=" + viewId + ",callbackOrdinal=" + ordinal
                        + ",observedHandCount=" + handIds.size() + ",sourceGameViewId=" + gameViewId
                        + ",callback=" + callback + ",scriptStep=" + scriptStep
                        + ",keepStage=" + (opening ? "opening" : "later")
                        + (snapshotDiagnostic == null ? "" : "," + snapshotDiagnostic)));
            }
            keepOpeningReceipts.add(new KeepOpeningReceipt(seat, viewId, gameViewId,
                    ++mulliganReceiptOrder, ordinal, handIds));
        }
        List<KeepOpeningReceipt> keepOpeningReceipts() { synchronized (keepOpeningReceipts) { return List.copyOf(keepOpeningReceipts); } }
        void configureReconnectScenario() { reconnectScenario = true; }
        boolean reconnectScenario() { return reconnectScenario;
        }

        boolean mulliganScenario() { return mulliganScenario;
        }
        boolean starterSeatZeroScenario() { return starterSeatZeroScenario;
        }
        List<MulliganChoiceReceipt> mulliganChoices() {
            synchronized (mulliganChoices) { return List.copyOf(mulliganChoices); }
        }

        List<FreeRevealReceipt> freeReveals() {
            synchronized (freeReveals) { return List.copyOf(freeReveals); }
        }
        List<StarterButtonReceipt> starterButtonReceipts() {
            synchronized (starterButtonReceipts) { return List.copyOf(starterButtonReceipts); }
        }
        List<StarterPromptReceipt> starterPromptReceipts() {
            synchronized (starterPromptReceipts) { return List.copyOf(starterPromptReceipts); }
        }
        List<LondonBottomDispatchReceipt> londonBottomDispatches() {
            synchronized (londonBottomDispatches) { return List.copyOf(londonBottomDispatches); }
        }
        void assertInitialShuffleConsumed() {
            if (mulliganScenario && (initialShuffleConsumed == null || !initialShuffleConsumed.getAsBoolean())) {
                AssertionError error = new AssertionError("The controlled initial 80-card shuffle was not fully consumed before mulligan choice");
                fail(error);
                throw error;
            }
        }
        Decision chooseInitialMulligan(int seat) {
            if (freeMulliganSeat.compareAndSet(-1, seat)) return Decision.FREE_MULLIGAN;
            if (freeMulliganSeat.get() == seat) {
                fail(new AssertionError("The same seat was asked to choose a second initial free mulligan"));
                return Decision.REGULAR_MULLIGAN;
            }
            return Decision.REGULAR_MULLIGAN;
        }
        synchronized void recordMulliganChoice(MulliganChoiceReceipt receipt) {
            if (privacyMulliganPrefixScenario() && (receipt.gameViewId() <= 0 || receipt.seat() < 0
                    || receipt.seat() > 1)) {
                fail(new AssertionError("Composite mulligan receipt lacks its actual GameView/seat identity"));
            }
            mulliganChoices.add(new MulliganChoiceReceipt(receipt.decision(), receipt.seat(), receipt.chooserViewId(),
                    receipt.gameViewId(), ++mulliganReceiptOrder, receipt.callback(), receipt.callbackOrdinal(),
                    receipt.handIds(), receipt.landCount()));
            if (receipt.decision() == Decision.FREE_MULLIGAN) {
                if (receipt.handIds().size() != 7 || !(receipt.landCount() < 2
                        || receipt.handIds().size() - receipt.landCount() < 2)) {
                    fail(new AssertionError("The free-mulligan choice lacks an actually eligible seven-card hand"));
                }
            }
        }
        synchronized void recordFreeReveal(int receivingSeat, int gameViewId, int eventId,
                              String message, long getChoicesOrdinal,
                              List<Integer> cardIds, String expectedMessage) {
            int ownerSeat = freeMulliganSeat.get();
            if (!mulliganScenario || ownerSeat < 0 || receivingSeat != 1 - ownerSeat || !expectedMessage.equals(message) || cardIds.size() != 7 || getChoicesOrdinal <= 0) {
                fail(new AssertionError("Unexpected DanDan free-hand reveal receipt for receiver seat " + receivingSeat
                        + ": " + message + " / " + cardIds + " at getChoices callback " + getChoicesOrdinal));
                return;
            }
            if (privacyMulliganPrefixScenario() && (gameViewId <= 0 || eventId < 0)) {
                fail(new AssertionError("Composite FREE reveal lacks its same-GameView wire event identity"));
                return;
            }
            freeReveals.add(new FreeRevealReceipt(receivingSeat, gameViewId, eventId, ++mulliganReceiptOrder,
                    message, getChoicesOrdinal, cardIds));
        }
        void recordStarterButton(StarterButtonReceipt receipt) {
            starterButtonReceipts.add(receipt);
            if (!starterSeatZeroScenario || receipt.seat() < 0 || receipt.seat() > 1 || !receipt.playEnabled() || !receipt.drawEnabled() || receipt.chosePlay() != (receipt.seat() == 0)) {
                fail(new AssertionError("Unexpected Play/Draw action for host starter: " + receipt));
            }
        }
        void recordStarterPrompt(StarterPromptReceipt receipt) {
            starterPromptReceipts.add(receipt);
            if (!starterSeatZeroScenario || receipt.seat() < 0 || receipt.seat() > 1) {
                fail(new AssertionError("Unexpected actual coin-toss prompt for host starter: " + receipt));
            }
        }
        synchronized boolean recordLondonBottomDispatch(LondonBottomDispatchReceipt receipt) {
            if (!londonsBottomed.compareAndSet(false, true)) {
                fail(new AssertionError("Received more than one London bottom selection"));
                return false;
            }
            if (privacyMulliganPrefixScenario() && (receipt.gameViewId() <= 0 || receipt.chooserHandIds().size() != 7)) {
                fail(new AssertionError("Composite London receipt lacks the actual seven-card pre-bottom hand"));
            }
            londonBottomDispatches.add(new LondonBottomDispatchReceipt(receipt.seat(), receipt.chooserViewId(),
                    receipt.gameViewId(), ++mulliganReceiptOrder, receipt.cardId(), receipt.chooserHandIds(),
                    receipt.promptOrdinal(), receipt.appliedHandCallbackOrdinal(), receipt.oneCardPrompt(), receipt.zeroCardPromptOrdinal(),
                    receipt.zeroCardPrompt()));
            return true;
        }

        boolean privacyMulliganPrefixScenario() { return mulliganScenario && false;
        }
        private static Map<Integer, Map<ZoneType, List<Integer>>> immutableZones(
                Map<Integer, Map<ZoneType, List<Integer>>> zones) {
            Map<Integer, Map<ZoneType, List<Integer>>> copy = new LinkedHashMap<>();
            zones.forEach((player, byZone) -> {
                EnumMap<ZoneType, List<Integer>> values = new EnumMap<>(ZoneType.class);
                byZone.forEach((zone, ids) -> values.put(zone, List.copyOf(ids)));
                copy.put(player, Collections.unmodifiableMap(values));
            });
            return Collections.unmodifiableMap(copy);
        }
        MulliganChoiceReceipt freeMulliganChoice() {
            synchronized (mulliganChoices) {
                return mulliganChoices.stream().filter(choice -> choice.decision() == Decision.FREE_MULLIGAN)
                        .findFirst().orElse(null);
            }
        }
        MulliganChoiceReceipt regularMulliganChoice() {
            synchronized (mulliganChoices) {
                return mulliganChoices.stream().filter(choice -> choice.decision() == Decision.REGULAR_MULLIGAN)
                        .findFirst().orElse(null);
            }
        }
        boolean hasLondonBottom() { return londonsBottomed.get();
        }

        void expectPostKeepCallback(String callback) {
            if (!"setGameView".equals(callback) && !"applyDelta".equals(callback)) {
                throw new IllegalArgumentException("Unsupported post-Keep callback: " + callback);
            }
            expectedPostKeepCallback = callback;
        }
        synchronized boolean holdAtFirstPriority(int seat, int gameViewId, Capture promptCapture, int turn,
                PhaseType phase, int turnPlayerViewId,
                Supplier<Map<Integer, Map<ZoneType, List<Integer>>>> zoneSnapshot) {
            if (expectedPostKeepCallback == null) return false;
            if (postKeepPriorityReleased) return false;
            if (postKeepPriorityHeld) return true;
            if (!bothKeepPromptsSeen() || !bothKeepActionsRecorded()) return false;
            if (staleActionWitness && lifecycleScenario && lifecycleCheckpointArmed && seat != 1) {
                if (seat != 0 || ++lifecycleHostPriorityAutoPassCount > 1) {
                    fail(new AssertionError("Lifecycle N+1 checkpoint expected B's first priority, after at most one actual A pass"));
                    return true;
                }
                return false; // let the existing scripted A controller pass its actual priority
            }
            if ((reconnectScenario || false) && !reconnectPostureReached) {
                int hostViewId = playerViewIdsBySeat().getOrDefault(0, -1);
                if (turn != 1 || phase != PhaseType.UPKEEP || turnPlayerViewId != hostViewId) {
                    fail(new AssertionError("Reconnect fixture expected host turn 1 UPKEEP before its held remote prompt; saw turn "
                            + turn + " phase " + phase + " player view " + turnPlayerViewId));
                    return true;
                }
                if (seat == 0) {
                    if (++reconnectHostUpkeepPrompts != 1) {
                        fail(new AssertionError("Reconnect fixture observed more than one host turn 1 UPKEEP prompt"));
                        return true;
                    }
                    return false; // one actual host pass advances to the held remote input
                }
                if (seat != 1) return false;
                reconnectPostureReached = true;
            }
            postKeepPriorityHeld = true;
            postKeepPrioritySeat = seat;
            postKeepPriorityOrdinal = promptCapture.callbackOrdinal();
            heldPriorityTurn = turn;
            heldPriorityPhase = phase;
            heldPriorityTurnPlayerViewId = turnPlayerViewId;
            heldPriorityZones = zoneSnapshot.get();
            if (lifecycleScenario && lifecycleCheckpointArmed) {
                lifecyclePriorityReceipt = new LifecyclePriorityReceipt(gameViewId,
                        promptCapture.callbackOrdinal(), seat, turn, phase, turnPlayerViewId, heldPriorityZones);
                lifecycleCheckpointArmed = false;
            }
            publishPostKeepStateIfReady();
            return true;
        }
        private boolean postKeepPriorityReleased;
        synchronized void releasePostKeepPriority() {
            postKeepPriorityReleased = true;
            if (metamorphoseScenario) metamorphosePostKeepReleased = true;
        }
        synchronized void observePostKeepState(Capture source,
                Supplier<Map<Integer, Map<ZoneType, List<Integer>>>> zoneSnapshot, int receivingSeat) {
            if (expectedPostKeepCallback == null || receivingSeat != 1 || source == null || !bothKeepPromptsSeen() || !bothKeepActionsRecorded()) return;
            if (source.callbackOrdinal() <= keepPromptOrdinals[receivingSeat] || !source.callback().equals(expectedPostKeepCallback)) return;
            if ("applyDelta".equals(source.callback()) && !source.stateBearing()) return;
            Map<Integer, Map<ZoneType, List<Integer>>> appliedZones = zoneSnapshot.get();
            if (!hasDealtPrivateZones(appliedZones)) return;
            if (postKeepCandidate == null || source.callbackOrdinal() > postKeepCandidate.stateCapture().callbackOrdinal()) {
                postKeepCandidate = new PostKeepCandidate(source, appliedZones, playerViewIdsBySeat());
            }
            publishPostKeepStateIfReady();
        }
        boolean awaitPostKeepState(long timeout, TimeUnit unit) throws InterruptedException {
            return postKeepState.await(timeout, unit);
        }
        synchronized String postKeepTimeoutEvidence() {
            int[] keepActionsBySeat = new int[2];
            synchronized (actions) {
                for (ActionReceipt action : actions) {
                    if (action.decision() == Decision.KEEP_WITHOUT_LAND && action.seat() >= 0 && action.seat() < keepActionsBySeat.length) {
                        keepActionsBySeat[action.seat()]++;
                    }
                }
            }
            return "postKeep{expected=" + expectedPostKeepCallback
                    + ",keepPrompt0=" + (keepPromptOrdinals[0] > 0)
                    + ",keepPrompt1=" + (keepPromptOrdinals[1] > 0)
                    + ",keepAction0=" + keepActionsBySeat[0]
                    + ",keepAction1=" + keepActionsBySeat[1]
                    + ",priorityHeld=" + postKeepPriorityHeld
                    + ",candidate=" + (postKeepCandidate != null)
                    + ",candidateOrdinal=" + (postKeepCandidate == null ? -1
                            : postKeepCandidate.stateCapture().callbackOrdinal())
                    + ",receipt=" + (postKeepReceipt != null)
                    + ",failurePresent=" + (failure.get() != null) + "}";
        }
        PostKeepReceipt postKeepReceipt() { return postKeepReceipt;
        }
        private boolean bothKeepPromptsSeen() { return keepPromptOrdinals[0] > 0 && keepPromptOrdinals[1] > 0;
        }
        private boolean bothKeepActionsRecorded() {
            if (lifecycleScenario && lifecycleCheckpointArmed) {
                boolean[] seats = new boolean[2];
                int gameViewId = -1;
                synchronized (lifecycleKeepReceipts) {
                    for (LifecycleKeepReceipt receipt : lifecycleKeepReceipts) {
                        if (gameViewId < 0) gameViewId = receipt.gameViewId();
                        if (receipt.gameViewId() != gameViewId) continue;
                        seats[receipt.seat()] = true;
                    }
                }
                return seats[0] && seats[1];
            }
            boolean[] seats = new boolean[2];
            synchronized (actions) {
                for (ActionReceipt action : actions) {
                    if (action.decision() == Decision.KEEP_WITHOUT_LAND && action.seat() >= 0 && action.seat() < seats.length) {
                        seats[action.seat()] = true;
                    }
                }
            }
            return seats[0] && seats[1];
        }
        private boolean hasDealtPrivateZones(Map<Integer, Map<ZoneType, List<Integer>>> zones) {
            if (zones.size() < 2) return false;
            for (Map<ZoneType, List<Integer>> byZone : zones.values()) {
                if (byZone.getOrDefault(ZoneType.Hand, List.of()).isEmpty() || byZone.getOrDefault(ZoneType.Library, List.of()).isEmpty()) return false;
            }
            return true;
        }
        private Map<Integer, Integer> playerViewIdsBySeat() {
            Map<Integer, Integer> result = new LinkedHashMap<>();
            synchronized (seatByViewId) {
                seatByViewId.forEach((viewId, seat) -> result.put(seat, viewId));
            }
            return result;
        }
        int playerViewIdForSeat(int seat) { return playerViewIdsBySeat().getOrDefault(seat, -1);
        }
        private void publishPostKeepStateIfReady() {
            if (!postKeepPriorityHeld || postKeepCandidate == null || postKeepReceipt != null) return;
            postKeepReceipt = new PostKeepReceipt(postKeepCandidate.stateCapture(), postKeepCandidate.appliedZonesByPlayer(),
                    postKeepCandidate.playerViewIdsBySeat(),
                    postKeepPrioritySeat, postKeepPriorityOrdinal, heldPriorityTurn, heldPriorityPhase,
                    heldPriorityTurnPlayerViewId, heldPriorityZones);
            postKeepState.countDown();
        }
        boolean bothLandsObserved() { return observedLandIds.size() == 2;
        }

        void markLandAbility(int seat) { landAbilities[seat].countDown(); }
        void addFailureHook(Runnable hook) { failureHooks.add(hook); }

        boolean observeAppliedState(GameView view, Capture receipt) {
            if (view == null || view.getPlayers() == null) return false;
            boolean allPrivateZonesPopulated = view.getPlayers().size() >= 2;
            for (PlayerView player : view.getPlayers()) {
                allPrivateZonesPopulated &= player.getHand() != null && !player.getHand().isEmpty()
                        && player.getCards(ZoneType.Library) != null && !player.getCards(ZoneType.Library).isEmpty();
                int ownerSeat = seatForView(player.getId());
                if (ownerSeat < 0) continue;
                synchronized (expectedLandIds) {
                    Integer landId = expectedLandIds.get(ownerSeat);
                    if (landId == null) continue;
                    for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                        if (player.getCards(zone) != null && player.getCards(zone).stream().anyMatch(card -> card.getId() == landId) && observedLandIds.add(landId)) {
                            publicMoveReceipts.put(ownerSeat, new CallbackReceipt(receipt.sequence(), receipt.callbackOrdinal()));
                            publicLandMoves[ownerSeat].countDown();
                        }
                    }
                }
            }
            return allPrivateZonesPopulated;
        }
        private final int[] priorityPasses = new int[2];
        synchronized boolean recordPriorityPass(int seat) {
            if (++priorityPasses[seat] > 80) {
                fail(new AssertionError("Priority-pass script exceeded its bound for seat " + seat));
                return false;
            }
            return true;
        }
        void markTerminated(int gameViewId) {
            Game origin = disconnectAiNextGameOrigin;
            boolean disconnectGameN = false && origin != null
                    && origin.getView().getId() == gameViewId;
            Game reconnectGameN = false ? privacyGame : null;
            boolean expectedPrivacyReconnectGameN = reconnectGameN != null && reconnectGameN.getView() != null
                    && reconnectGameN.getView().getId() == gameViewId;
            if (!lifecycleScenario && !disconnectGameN && !expectedPrivacyReconnectGameN) stopped = true;
        }
        void stopActions() { stopped = true; }
        boolean tryQueueConcession() { return concessionQueued.compareAndSet(false, true);
        }
    }

    static final class ScriptedGui extends HeadlessNetworkGuiGame {
        private final int seat;
        private final boolean remote;
        private final String expectedPlayerName;
        private final Scenario scenario;
        private final BlockingDeque<ExpectedAction> script = new LinkedBlockingDeque<>();
        private final List<Capture> captures = Collections.synchronizedList(new ArrayList<>());
        private final List<GameViewReceipt> gameViewReceipts = Collections.synchronizedList(new ArrayList<>());
        private final List<PrivacyLogObservation> privacyLogObservations = Collections.synchronizedList(new ArrayList<>());
        private final Map<Object, Integer> privacyLogCursors = new IdentityHashMap<>();
        private final Map<GameLog, Observer> privacyObservedGameLogs = new IdentityHashMap<>();
        private final CountDownLatch fullState = new CountDownLatch(1);
        private final CountDownLatch openView = new CountDownLatch(1);
        private final CountDownLatch postOpenFullState = new CountDownLatch(1);
        private final CountDownLatch dealtState = new CountDownLatch(1);
        private final CountDownLatch populatedDelta = new CountDownLatch(1);
        private final CountDownLatch terminal = new CountDownLatch(1);
        private volatile TerminalViewReceipt terminalViewReceipt;
        private final CountDownLatch afterGameEnd = new CountDownLatch(1);
        private final CountDownLatch alertUserReceived = new CountDownLatch(1);
        private final CountDownLatch terminalOrFailure = new CountDownLatch(1);
        private final CountDownLatch nonemptyEvents = new CountDownLatch(1);
        private final AtomicInteger populatedDeltaCount = new AtomicInteger();
        private final AtomicReference<CallbackReceipt> dealtReceipt = new AtomicReference<>();
        private final AtomicLong callbackOrdinals = new AtomicLong();
        private final AtomicLong afterGameEndOrdinal = new AtomicLong(-1);
        private final ThreadLocal<Long> callbackSequence = ThreadLocal.withInitial(() -> -1L);
        private final ThreadLocal<Capture> applyingPacket = new ThreadLocal<>();
        private volatile int ownPlayerViewId = -1;
        private volatile PendingOpeningKeep pendingOpeningKeep;
        private volatile int pendingLandCardId = -1;
        private volatile int pendingLondonBottomCardId = -1;
        private volatile long pendingLondonPromptOrdinal = -1;
        private volatile long londonZeroPromptOrdinal = -1;
        private volatile String londonOneCardPrompt;
        private volatile String londonZeroCardPrompt;
        private volatile IGameController londonController;
        private volatile CardView londonSelectedCard;
        private volatile boolean londonEnabledOkCallbackSeen;
        private volatile boolean londonAwaitingAppliedHand;
        private volatile Game londonEngineGame;
        private volatile PlayerControllerHuman londonEngineController;
        private volatile InputLondonMulligan londonEngineInput;
        private volatile int londonGameViewId = -1;
        private volatile long londonButtonsCallbackOrdinal = -1;
        private volatile long londonAppliedHandCallbackOrdinal = -1;
        private volatile Object londonAppliedHandTrackerIdentity;
        private volatile Capture londonOneCardPromptCapture;
        private volatile String londonDeferredOneCardPrompt;
        private final AtomicBoolean londonSelectionClaimed = new AtomicBoolean();
        private volatile boolean expectingAlertUserFence;
        private final List<PriorityReceipt> priorityPrompts = Collections.synchronizedList(new ArrayList<>());
        private final List<AppliedViewReceipt> appliedViews = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch turnOneMainOnePriority = new CountDownLatch(1);
        private final CountDownLatch turnOneMainOneView = new CountDownLatch(1);
        private final CountDownLatch reconnectReplayPriority = new CountDownLatch(1);
        private volatile boolean expectReconnectReplay;
        private volatile List<Integer> pendingMetamorphoseTargetIds = List.of();
        private volatile List<Integer> pendingMetamorphoseHandIds = List.of();
        private volatile long pendingMetamorphoseTargetPromptOrdinal = -1;
        private volatile PrivacyInputSnapshot acceptedMetamorphoseTargetInput;
        private volatile int acceptedMetamorphoseTargetGameViewId = -1;
        private volatile int acceptedMetamorphoseTargetControllerViewId = -1;
        private volatile IGameController acceptedMetamorphoseTargetController;
        private final AtomicInteger metamorphoseTargetActionsQueued = new AtomicInteger();
        private final AtomicInteger metamorphoseTargetActionsDispatched = new AtomicInteger();
        private volatile Boolean metamorphoseTargetActionAccepted;
        private volatile long pendingMetamorphoseHandPromptOrdinal = -1;
        private volatile String pendingMetamorphoseHandPromptMessage;
        private final Set<Integer> metamorphoseManaSelections = Collections.synchronizedSet(new LinkedHashSet<>());
        private volatile int pendingMetamorphoseManaCardId = -1;
        private volatile int pendingPrivacyManaCardId = -1;
        private volatile long pendingPrivacyManaPromptOrdinal = -1;
        private volatile long lastPrivacyManaContinuationOrdinal = -1;
        private volatile int pendingPrivacyManaPromptOwnerId = -1;
        private volatile IGameController pendingPrivacyManaPromptController;

        ScriptedGui(int seat, boolean remote, String expectedPlayerName, Scenario scenario) {
            this.seat = seat;
            this.remote = remote;
            this.expectedPlayerName = expectedPlayerName;
            this.scenario = scenario;
            scenario.addFailureHook(this::wakeFailureWaiters);
            scenario.addFailureHook(this::detachPrivacyLogObservers);
        }

        int ownPlayerViewId() { return ownPlayerViewId;
        }
        int populatedDeltaCount() { return populatedDeltaCount.get();
        }

        List<Capture> captures() { synchronized (captures) { return List.copyOf(captures); } }
        List<GameViewReceipt> gameViewReceipts() {
            synchronized (gameViewReceipts) { return List.copyOf(gameViewReceipts); }
        }
        List<PrivacyLogObservation> privacyLogObservations() {
            synchronized (privacyLogObservations) { return List.copyOf(privacyLogObservations); }
        }

        private boolean privacyLogObservationEnabled() {
            return scenario.privacyScenario() || false;
        }

        private synchronized void observeHostPrivacyLog(GameView view) {
            if (remote) return;
            if (scenario.stopped() || !privacyLogObservationEnabled()) {
                detachPrivacyLogObservers();
                return;
            }
            GameLog currentLog = view == null ? null : view.getGameLog();
            privacyObservedGameLogs.entrySet().removeIf(entry -> {
                if (entry.getKey() == currentLog) return false;
                entry.getKey().deleteObserver(entry.getValue());
                return true;
            });
            if (currentLog == null || privacyObservedGameLogs.containsKey(currentLog)) return;
            Observer observer = (Observable source, Object ignored) -> {
                try {
                    if (getGameView() == null || getGameView().getGameLog() != currentLog) return;
                    Capture receipt = capturePrivacyLogNotification();
                    observePrivacyLog(receipt);
                } catch (Throwable failure) {
                    scenario.fail(failure);
                }
            };
            currentLog.addObserver(observer);
            privacyObservedGameLogs.put(currentLog, observer);
        }

        private synchronized void detachPrivacyLogObservers() {
            privacyObservedGameLogs.forEach(GameLog::deleteObserver);
            privacyObservedGameLogs.clear();
        }

        private Capture capturePrivacyLogNotification() {
            Capture captured = new Capture("GameLog.notifyObservers", -1, callbackOrdinals.incrementAndGet(),
                    new byte[0], Map.of(), List.of(), false, 0, 0, new byte[0], -1, -1, List.of());
            captures.add(captured);
            return captured;
        }

        private synchronized void observePrivacyLog(Capture source) {
            if (!privacyLogObservationEnabled() || source == null) return;
            try {
                GameView view = getGameView();
                if (view == null || view.getGameLog() == null) return;
                var log = view.getGameLog();
                List<GameLogEntry> entries = log.getAllEntries();
                int start = privacyLogCursors.getOrDefault(log, 0);
                if (start > entries.size()) start = 0;
                List<PrivacyLogRow> rows = new ArrayList<>();
                for (int i = start; i < entries.size(); i++) {
                    GameLogEntry entry = entries.get(i);
                    rows.add(new PrivacyLogRow(entry.type().name(), entry.message(),
                            entry.sourceCard() == null ? -1 : entry.sourceCard().getId()));
                }
                Set<Integer> permissionedIds = new LinkedHashSet<>();
                PlayerView assigned;
                if (ownPlayerViewId >= 0) {
                    assigned = findPlayerView(view, ownPlayerViewId);
                    if (assigned == null) throw new AssertionError(
                            "Privacy log snapshot lacks its assigned endpoint PlayerView");
                } else {
                    assigned = null;
                }
                if (assigned == null && view.getPlayers() != null) {
                    for (Object value : view.getPlayers().toArray()) {
                        if (!(value instanceof PlayerView player)) {
                            throw new AssertionError("Privacy log snapshot contains a non-PlayerView");
                        }
                        if (expectedPlayerName.equals(player.getName())) assigned = player;
                    }
                }
                int observedOwnPlayerViewId = assigned == null ? ownPlayerViewId : assigned.getId();
                List<Integer> ownHandIds = new ArrayList<>();
                Set<Integer> publicCardIds = new LinkedHashSet<>();
                if (assigned != null) {
                    ownHandIds.addAll(zoneCardIds(assigned, ZoneType.Hand));
                    for (Object value : view.getPlayers().toArray()) {
                        if (!(value instanceof PlayerView player)) {
                            throw new AssertionError("Privacy log snapshot contains a non-PlayerView");
                        }
                        for (ZoneType publicZone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                            publicCardIds.addAll(zoneCardIds(player, publicZone));
                        }
                    }
                    Game hostGame = remote ? null : scenario.privacyGame();
                    if (hostGame != null && hostGame.getView().getId() == view.getId()) {
                        for (Card card : hostGame.getCardsIn(ZoneType.Stack)) publicCardIds.add(card.getId());
                    }
                    Set<Integer> candidates = new LinkedHashSet<>(scenario.privacyTopIds());
                    candidates.add(scenario.privacyPublicCreatureId());
                    for (MulliganChoiceReceipt choice : scenario.mulliganChoices()) candidates.addAll(choice.handIds());
                    for (FreeRevealReceipt reveal : scenario.freeReveals()) candidates.addAll(reveal.cardIds());
                    for (PrivacyChoiceReceipt choice : scenario.privacyChoices()) candidates.addAll(choice.cardIds());
                    for (PrivacyRevealReceipt reveal : scenario.privacyReveals()) candidates.addAll(reveal.cardIds());
                    for (int id : candidates) {
                        CardView card = findVisibleCard(view, id);
                        if (card != null && (card.mayPlayerLook(assigned) || card.canBeShownTo(assigned))) {
                            permissionedIds.add(id);
                        }
                    }
                }
                PrivacyLogObservation receipt = new PrivacyLogObservation(callbackOrdinals.incrementAndGet(),
                        source.callback(), source.callbackOrdinal(), source.sequence(), source.parentCallbackOrdinal(),
                        source.parentSequence(), view.getId(), observedOwnPlayerViewId, ownHandIds, publicCardIds,
                        rows, permissionedIds);
                privacyLogObservations.add(receipt);
                privacyLogCursors.put(log, entries.size());
            } catch (Throwable failure) {
                scenario.fail(failure);
            }
        }

        private List<Integer> zoneCardIds(PlayerView player, ZoneType zone) {
            List<Integer> ids = new ArrayList<>();
            var cards = player.getCards(zone);
            if (cards == null) return ids;
            for (Object value : cards.toArray()) {
                if (!(value instanceof CardView card)) throw new AssertionError("Privacy zone has non-CardView entry");
                ids.add(card.getId());
            }
            return ids;
        }
        String openingKeepTimeoutEvidence() {
            try {
                List<Capture> callbackCaptures = captures();
                long fullCallbacks = callbackCaptures.stream()
                        .filter(capture -> "setGameView".equals(capture.callback())).count();
                long deltaCallbacks = callbackCaptures.stream()
                        .filter(capture -> "applyDelta".equals(capture.callback())).count();
                long buttonCallbacks = callbackCaptures.stream()
                        .filter(capture -> "updateButtons".equals(capture.callback())).count();
                long mulliganDialogs = callbackCaptures.stream()
                        .filter(capture -> "showOptionDialog".equals(capture.callback())).count();
                PendingOpeningKeep pending = pendingOpeningKeep;
                GameView current = getGameView();
                Game engine = pending == null ? scenario.openingKeepEngineGame() : pending.game();
                PlayerControllerHuman controller = pending != null ? pending.controller()
                        : engine != null && seat >= 0 && seat < engine.getPlayers().size()
                                && engine.getPlayers().get(seat).getController() instanceof PlayerControllerHuman human
                                        ? human : null;
                PrivacyInputSnapshot currentInput = controller == null ? null
                        : new PrivacyInputSnapshot(controller.getInputProxy().getInput(),
                                controller.getInputQueue().getInput());
                PlayerView owner = current == null || ownPlayerViewId < 0
                        ? null : findPlayerView(current, ownPlayerViewId);
                int handCount = owner == null || owner.getHand() == null ? -1 : owner.getHand().size();
                int libraryCount = owner == null || owner.getCards(ZoneType.Library) == null
                        ? -1 : owner.getCards(ZoneType.Library).size();
                GameViewReceipt latestCurrentViewReceipt = current == null ? null : gameViewReceipts().stream()
                        .filter(receipt -> receipt.gameViewId() == current.getId())
                        .max(Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
                boolean sameTracker = latestCurrentViewReceipt != null
                        && latestCurrentViewReceipt.trackerIdentity() == current.getTracker();
                boolean sameGame = engine != null && pending != null
                        && pending.controller().getGame() == pending.game()
                        && pending.game().getView() != null
                        && current != null && pending.game().getView().getId() == current.getId();
                boolean sameInput = pending != null && currentInput != null
                        && currentInput.proxyInput() == pending.input()
                        && currentInput.queuedInput() == pending.input();
                boolean bottomInLibrary = pending != null && pending.londonBottomCardId() > 0
                        && owner != null && owner.getCards(ZoneType.Library) != null
                        && owner.getCards(ZoneType.Library).stream()
                                .anyMatch(card -> card.getId() == pending.londonBottomCardId())
                        && (owner.getHand() == null || owner.getHand().stream()
                                .noneMatch(card -> card.getId() == pending.londonBottomCardId()));
                boolean witness = pending != null && currentOpeningKeepWitness(pending, Long.MAX_VALUE) != null;
                return "openingKeep{seat=" + seat + ",remote=" + remote
                        + ",callbacksFull=" + fullCallbacks + ",callbacksDelta=" + deltaCallbacks
                        + ",callbacksButtons=" + buttonCallbacks + ",mulliganDialogs=" + mulliganDialogs
                        + ",pending=" + (pending != null)
                        + ",expectedHand=" + (pending == null ? -1 : pending.expectedHandCount())
                        + ",promptOrdinal=" + (pending == null ? -1 : pending.prompt().callbackOrdinal())
                        + ",appliedLowerBound=" + (pending == null ? -1 : pending.appliedStateLowerBoundOrdinal())
                        + ",currentInput=" + (currentInput == null || currentInput.proxyInput() == null
                                ? "null" : currentInput.proxyInput().getClass().getSimpleName())
                        + ",proxyQueueSame=" + (currentInput != null
                                && currentInput.proxyInput() == currentInput.queuedInput())
                        + ",samePendingInput=" + sameInput
                        + ",currentView=" + (current != null)
                        + ",sameGame=" + sameGame
                        + ",sameTracker=" + sameTracker
                        + ",latestViewOrdinal=" + (latestCurrentViewReceipt == null ? -1
                                : latestCurrentViewReceipt.callbackOrdinal())
                        + ",latestViewHand=" + (latestCurrentViewReceipt == null ? -1
                                : latestCurrentViewReceipt.zonesByPlayer()
                                        .getOrDefault(ownPlayerViewId, Map.of())
                                        .getOrDefault(ZoneType.Hand, List.of()).size())
                        + ",currentHand=" + handCount + ",currentLibrary=" + libraryCount
                        + ",serverZonesMatch=" + (pending != null && openingKeepServerZonesMatch(pending))
                        + ",bottomInLibrary=" + bottomInLibrary
                        + ",witness=" + witness + "}";
            } catch (Throwable diagnosticFailure) {
                return "openingKeep{diagnosticFailed=" + diagnosticFailure.getClass().getSimpleName() + "}";
            }
        }
        TerminalViewReceipt terminalViewReceipt() { return terminalViewReceipt;
        }
        List<PriorityReceipt> priorityPrompts() { synchronized (priorityPrompts) { return List.copyOf(priorityPrompts); } }
        List<AppliedViewReceipt> appliedViews() { synchronized (appliedViews) { return List.copyOf(appliedViews); } }
        boolean awaitFull(long timeout, TimeUnit unit) throws InterruptedException { return fullState.await(timeout, unit);
        }
        boolean awaitOpenView(long timeout, TimeUnit unit) throws InterruptedException { return openView.await(timeout, unit);
        }
        boolean awaitPostOpenFullState(long timeout, TimeUnit unit) throws InterruptedException { return postOpenFullState.await(timeout, unit);
        }

        boolean awaitAfterGameEnd(long timeout, TimeUnit unit) throws InterruptedException { return afterGameEnd.await(timeout, unit);
        }
        boolean awaitAlertUser(long timeout, TimeUnit unit) throws InterruptedException { return alertUserReceived.await(timeout, unit);
        }

        boolean awaitActualTerminal(long timeout, TimeUnit unit) throws InterruptedException { return terminal.await(timeout, unit);
        }
        boolean awaitTurnOneMainOnePriority(long timeout, TimeUnit unit) throws InterruptedException { return turnOneMainOnePriority.await(timeout, unit);
        }
        boolean awaitTurnOneMainOneView(long timeout, TimeUnit unit) throws InterruptedException { return turnOneMainOneView.await(timeout, unit);
        }
        boolean awaitReconnectReplayPriority(long timeout, TimeUnit unit) throws InterruptedException { return reconnectReplayPriority.await(timeout, unit);
        }
        void expectReconnectReplay() { expectReconnectReplay = true; }
        private GameViewReceipt privacyReconnectReplayFullReceipt(long promptOrdinal) {
            long openOrdinal = captures().stream().filter(capture -> "openView".equals(capture.callback()))
                    .mapToLong(Capture::callbackOrdinal).max().orElse(-1);
            if (openOrdinal <= 0) return null;
            return gameViewReceipts().stream().filter(receipt -> "setGameView".equals(receipt.callback())
                            && receipt.callbackOrdinal() > openOrdinal
                            && receipt.callbackOrdinal() < promptOrdinal)
                    .max(Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
        }
        boolean hasCurrentPrivacyReconnectReplay(int expectedSeat, int expectedViewId, int expectedGameViewId,
                long promptOrdinal, int turn, PhaseType phase, int turnPlayerViewId, int stackSize) {
            if (!expectReconnectReplay || !remote || seat != expectedSeat || expectedSeat != 1 || ownPlayerViewId != expectedViewId || promptOrdinal <= 0 || turn != 4 || phase != PhaseType.UPKEEP || turnPlayerViewId != expectedViewId || stackSize != 0) return false;
            GameViewReceipt baseline = privacyReconnectReplayFullReceipt(promptOrdinal);
            GameView current = getGameView();
            boolean hasOwnPriorityReceipt = priorityPrompts().stream().anyMatch(receipt ->
                    receipt.seat() == expectedSeat && receipt.chooserViewId() == expectedViewId
                            && receipt.callbackOrdinal() == promptOrdinal && receipt.turn() == turn
                            && receipt.phase() == phase && receipt.turnPlayerViewId() == turnPlayerViewId
                            && (receipt.enabledOne() || receipt.enabledTwo()));
            return baseline != null && baseline.gameViewId() == expectedGameViewId
                    && baseline.danDan()
                    && current != null && current.getId() == expectedGameViewId
                    && current.getTracker() == baseline.trackerIdentity() && current.isDanDan()
                    && current.getTurn() == turn && current.getPhase() == phase
                    && current.getPlayerTurn() != null && current.getPlayerTurn().getId() == turnPlayerViewId
                    && current.getStack() != null && current.getStack().isEmpty()
                    && hasOwnPriorityReceipt;
        }
        Map<Integer, Map<ZoneType, List<Integer>>> currentReconnectReplayZones(long promptOrdinal) {
            GameViewReceipt baseline = privacyReconnectReplayFullReceipt(promptOrdinal);
            return baseline == null ? Map.of() : baseline.zonesByPlayer();
        }

        void script(Decision decision, Integer cardId) { script.addLast(new ExpectedAction(decision, seat, cardId)); }
        void expectAlertUserFence() { expectingAlertUserFence = true; }

        void releaseHeldPostKeepPriority() {
            if (ownPlayerViewId < 0 || scenario.postKeepReceipt() == null) {
                throw new AssertionError("No held post-Keep priority is assigned to this GUI");
            }
            scenario.releasePostKeepPriority();
            GameView current = getGameView();
            PlayerView own = null;
            if (current != null) for (PlayerView player : current.getPlayers()) {
                if (player.getId() == ownPlayerViewId) { own = player; break; }
            }
            if (own == null) throw new AssertionError("Held priority's assigned PlayerView is absent");
            IGameController controller = getGameController(own);
            if (controller == null) throw new AssertionError("Held priority has no live assigned controller");
            if (scenario.metamorphoseScenario()) {
                scenario.recordMetamorphoseHeldPriorityPass(seat, ownPlayerViewId);
            }
            FThreads.invokeInEdtAndWait(controller::selectButtonOk);
        }

        void releasePrivacyInitialPriority() {
            if (!scenario.privacyScenario() || seat != 0 || ownPlayerViewId != scenario.privacyHostViewId()) {
                throw new AssertionError("Only assigned host may release the actual initial privacy priority");
            }
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            if (current == null || current.getTurn() != 1 || current.getPhase() != PhaseType.UNTAP || current.getPlayerTurn() == null || current.getPlayerTurn().getId() != ownPlayerViewId || current.getStack() == null || !current.getStack().isEmpty() || controller == null) {
                throw new AssertionError("Initial privacy action no longer has the real host turn-1 UNTAP prompt");
            }
            scenario.releasePrivacyInitialPriority();
            FThreads.invokeInEdtAndWait(controller::selectButtonOk);
        }

        void activatePrivacyFengraf() {
            int actorSeat = scenario.privacyFengrafSeat();
            int actorViewId = scenario.privacyViewIdForSeat(actorSeat);
            if (!scenario.privacyScenario() || seat != actorSeat || ownPlayerViewId != actorViewId) {
                throw new AssertionError("Only the assigned Fengraf actor may activate Haunted Fengraf");
            }
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            CardView fengraf = findVisibleCard(current, scenario.privacyFengrafId());
            IGameController controller = own == null ? null : getGameController(own);
            if (current == null || current.getTurn() != 1 || current.getPhase() != PhaseType.MAIN1 || current.getPlayerTurn() == null || current.getStack() == null || !current.getStack().isEmpty() || fengraf == null || fengraf.isTapped() || controller == null) {
                throw new AssertionError("Assigned Fengraf actor no longer has the held post-Telling Time priority state");
            }
            scenario.beginPrivacyFengraf();
            scenario.recordPrivacyFengrafDispatch(seat, ownPlayerViewId);
            FThreads.invokeInEdtAndWait(() -> {
                if (!controller.selectCard(fengraf, null, null)) fail("Assigned actor controller rejected Haunted Fengraf");
            });
        }

        void concedePrivacyAfterAssertions() {
            if (!scenario.privacyScenario() || seat != 0 || ownPlayerViewId != scenario.privacyHostViewId()) {
                throw new AssertionError("Only assigned host may finish the privacy fixture");
            }
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            if (current == null || current.getTurn() != 1 || current.getPhase() != PhaseType.MAIN1 || current.getPlayerTurn() == null || current.getPlayerTurn().getId() != ownPlayerViewId || current.getStack() == null || !current.getStack().isEmpty() || controller == null) {
                throw new AssertionError("Host lacks the real empty-stack post-Fengraf priority");
            }
            scenario.recordPrivacyConcession(seat, ownPlayerViewId);
            FThreads.invokeInEdtAndWait(controller::concede);
        }

        void concedeMetamorphoseAfterAssertions() {
            if (!scenario.metamorphoseScenario() || seat != 0 || ownPlayerViewId != scenario.metamorphoseHostViewId() || scenario.metamorphoseResolution() == null || scenario.metamorphosePostResolutionPriority() == null) {
                throw new AssertionError("Metamorphose terminal action requires the host after an actual received resolution");
            }
            GameView current = getGameView();
            if (current == null || current.getTurn() != 1 || current.getPhase() != PhaseType.MAIN1 || current.getPlayerTurn() == null || current.getPlayerTurn().getId() != ownPlayerViewId || current.getStack() == null || !current.getStack().isEmpty()) {
                throw new AssertionError("Host has no actual empty-stack postresolution priority to concede from");
            }
            PlayerView own = findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            if (controller == null) throw new AssertionError("Host postresolution concession has no assigned controller");
            if (!scenario.recordMetamorphoseTerminalConcession(seat, ownPlayerViewId, current.getTurn(),
                    current.getPhase(), current.getPlayerTurn().getId(), current.getStack().size())) {
                throw new AssertionError("Host postresolution concession did not pass its actual state gate");
            }
            FThreads.invokeInEdtAndWait(controller::concede);
        }

        void concedeFromAssignedSeat() {
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            LifecyclePriorityReceipt lifecycleCheckpoint = scenario.lifecyclePriorityReceipt();
            if (current == null || own == null || controller == null || seat != 1 || !remote || !scenario.holdMetamorphoseOptional() || !(controller instanceof NetGameController) || lifecycleCheckpoint != null && lifecycleCheckpoint.gameViewId() != current.getId()) {
                throw new AssertionError("Held Meta prompt has no assigned remote controller to concede");
            }
            scenario.recordAction(new ActionReceipt(Decision.CONCEDE, seat, ownPlayerViewId, null, true));
            if (scenario.lifecycleScenario() && scenario.metamorphoseScenario() && !scenario.recordMetamorphoseAssignedConcession(
                    seat, ownPlayerViewId, current.getId())) {
                throw new AssertionError("Actual assigned-seat concession did not identify the held Meta game");
            }
            controller.concede();
        }

        void decideNextGame(NextGameDecision decision) {
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            if (current == null || own == null || controller == null) {
                throw new AssertionError("Next-game decision lacks this endpoint's assigned controller");
            }
            scenario.recordNextGameDecision(new NextGameDecisionReceipt(seat, ownPlayerViewId,
                    current.getId(), decision));
            controller.nextGameDecision(decision);
        }

        void releaseMetamorphoseFirstStackPriority(MetamorphoseStackPriorityReceipt receipt) {
            if (!scenario.metamorphoseScenario() || seat != 0 || ownPlayerViewId != scenario.metamorphoseHostViewId()) {
                throw new AssertionError("Only the assigned host can release the first Metamorphose stack priority");
            }
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            IGameController controller = own == null ? null : getGameController(own);
            Game game = scenario.metamorphoseGame();
            List<MetamorphoseStackPriorityReceipt> receipts = scenario.metamorphoseStackPriorities();
            if (receipt == null || receipts.size() != 1 || receipts.get(0) != receipt || game == null || game.getPlayers().size() != 2) {
                throw new AssertionError("The retained Metamorphose Game no longer has its two assigned players");
            }
            Player host = game.getPlayers().get(0);
            if (!(host.getController() instanceof PlayerControllerHuman hostController) || controller != hostController || hostController.getGame() != game || receipt.priorityInput() == null || hostController.getInputProxy().getInput() != receipt.priorityInput() || hostController.getInputQueue().getInput() != receipt.priorityInput() || receipt.priorityInput().getOwner() == null || receipt.priorityInput().getOwner().getId() != host.getId()) {
                throw new AssertionError("The captured host no longer holds the exact live InputPassPriority");
            }
            if (receipt.game() != game || receipt.gameViewId() != game.getView().getId() || current == null || current.getId() != receipt.gameViewId() || receipt.seat() != 0 || receipt.chooserViewId() != ownPlayerViewId || receipt.callbackOrdinal() <= 0 || receipt.turn() != 1 || receipt.phase() != PhaseType.MAIN1 || receipt.turnPlayerViewId() != ownPlayerViewId || receipt.stackSize() != 1 || current.getTurn() != receipt.turn() || current.getPhase() != receipt.phase() || current.getPlayerTurn() == null || current.getPlayerTurn().getId() != receipt.turnPlayerViewId() || current.getStack() == null || current.getStack().size() != 1 || current.getStack().get(0).getId() != receipt.stackItemId() || current.getStack().get(0).getSourceCard() == null || current.getStack().get(0).getSourceCard().getId() != receipt.sourceCardId() || current.getStack().get(0).getActivatingPlayer() == null || current.getStack().get(0).getActivatingPlayer().getId() != receipt.activatorViewId() || current.getStack().get(0).getTargetCards() == null || !current.getStack().get(0).getTargetCards().stream().map(CardView::getId).toList()
                            .equals(receipt.targetCardIds())) {
                throw new AssertionError("Host no longer has the retained actual Metamorphose stack prompt");
            }
            FThreads.invokeInEdtAndWait(() -> {
                GameView held = getGameView();
                if (scenario.failure() != null || held == null || held.getId() != receipt.gameViewId() || held.getTurn() != receipt.turn() || held.getPhase() != receipt.phase() || held.getStack() == null || held.getStack().size() != receipt.stackSize() || held.getStack().get(0).getId() != receipt.stackItemId() || hostController.getGame() != game || hostController.getInputProxy().getInput() != receipt.priorityInput() || hostController.getInputQueue().getInput() != receipt.priorityInput() || receipt.priorityInput().getOwner() == null || receipt.priorityInput().getOwner().getId() != host.getId()) {
                    throw new AssertionError("The exact held Metamorphose priority changed before release",
                            scenario.failure());
                }
                scenario.recordMetamorphoseFirstStackPass(seat, ownPlayerViewId);
                controller.selectButtonOk();
            });
        }

        void dispatchHeldReconnectPriorityPass() {
            if (!remote || !expectReconnectReplay || ownPlayerViewId < 0) {
                throw new AssertionError("Only the assigned replacement GUI can answer the replayed priority");
            }
            GameView current = getGameView();
            PlayerView own = null;
            if (current != null) for (PlayerView player : current.getPlayers()) {
                if (player.getId() == ownPlayerViewId) { own = player; break; }
            }
            int expectedTurn = false ? 4 : 1;
            int expectedTurnPlayer = false
                    ? scenario.privacyViewIdForSeat(1) : scenario.playerViewIdForSeat(0);
            if (current == null || own == null || current.getTurn() != expectedTurn || current.getPhase() != PhaseType.UPKEEP || current.getPlayerTurn() == null || current.getPlayerTurn().getId() != expectedTurnPlayer) {
                throw new AssertionError("Replacement no longer has the exact held reconnect UPKEEP priority");
            }
            PriorityReceipt prompt;
            synchronized (priorityPrompts) {
                prompt = priorityPrompts.stream().filter(receipt -> receipt.seat() == seat
                        && receipt.chooserViewId() == ownPlayerViewId && receipt.turn() == expectedTurn
                        && receipt.phase() == PhaseType.UPKEEP).reduce((left, right) -> right).orElse(null);
            }
            if (prompt == null || !prompt.enabledOne() && !prompt.enabledTwo()) {
                throw new AssertionError("Replacement has no enabled receipt for the held actual priority");
            }
            IGameController controller = getGameController(own);
            if (controller == null) throw new AssertionError("Replacement's assigned PlayerView has no live controller");

            FThreads.invokeInEdtAndWait(() -> {

                if (!scenario.recordPriorityPass(seat)) return;
                scenario.recordAction(new ActionReceipt(Decision.PASS_PRIORITY, seat, ownPlayerViewId, null, true));
                controller.selectButtonOk();
            });
        }

        private void recordAppliedView(String callback, Capture source, int incomingGameViewId, GameView view) {
            if (view == null) return;
            String snapshotSource = "callback=" + callback + ",endpoint=" + (remote ? "remote" : "host")
                    + ",seat=" + seat + ",callbackOrdinal=" + source.callbackOrdinal()
                    + ",callbackSequence=" + source.sequence()
                    + ",incomingGameViewId=" + incomingGameViewId + ",appliedGameViewId=" + view.getId()
                    + ",source=applied getGameView";
            gameViewReceipts.add(new GameViewReceipt(callback, source.sequence(), source.callbackOrdinal(),
                    incomingGameViewId,
                    view.getId(), view.getTracker(), view.isDanDan(), source.stateBearing(),
                    snapshotZones(new Object[] {view}, snapshotSource)));

            if (!scenario.reconnectScenario()) return;
            PlayerView turnPlayer = view.getPlayerTurn();
            appliedViews.add(new AppliedViewReceipt(callback, source.sequence(), source.callbackOrdinal(),
                    source.stateBearing(), view.isDanDan(), view.getTurn(), view.getPhase(),
                    turnPlayer == null ? -1 : turnPlayer.getId(), view.getStack().size(),
                    snapshotZones(new Object[] {view}, snapshotSource)));
            if (view.getTurn() == 1 && view.getPhase() == PhaseType.MAIN1) turnOneMainOneView.countDown();
        }

        private void recordPrivacyState(String callback, Capture source, GameView view,
                                        int incomingGameViewId, int preAppliedGameViewId) {
            if (!remote || !scenario.privacyScenario() || source == null || view == null) return;
            if (scenario.privacyHostViewId() < 0 || scenario.privacyRemoteViewId() < 0 || scenario.privacyTopIds().size() != 3 || scenario.privacyInitialLibraryIds().size() <= 3 || scenario.privacyPublicCreatureId() < 0) return;
            try {
                PlayerView turnPlayer = view.getPlayerTurn();
                Map<Integer, Integer> drawCounts = new LinkedHashMap<>();
                for (Object value : view.getPlayers().toArray()) {
                    if (!(value instanceof PlayerView player)) throw new AssertionError("Privacy view contains a non-PlayerView player entry");
                    drawCounts.put(player.getId(), player.getNumDrawnThisTurn());
                }
                Map<Integer, List<Boolean>> privateTopVisibility = new LinkedHashMap<>();
                PlayerView remoteView = findPlayerView(view, scenario.privacyRemoteViewId());
                if (remoteView == null) throw new AssertionError("Privacy callback has no assigned remote PlayerView");
                List<Integer> visibilityIds = new ArrayList<>(scenario.privacyTopIds());
                visibilityIds.add(scenario.privacyPublicCreatureId());
                for (int cardId : visibilityIds) {
                    CardView card = findVisibleCard(view, cardId);
                    if (card == null) throw new AssertionError("Privacy callback lacks expected physical card view " + cardId);
                    privateTopVisibility.put(cardId,
                            List.of(card.mayPlayerLook(remoteView), card.canBeShownTo(remoteView)));
                }
                scenario.observePrivacyState(new PrivacyStateReceipt(seat, callback, source.sequence(),
                        source.callbackOrdinal(), view.getTurn(), view.getPhase(),
                        turnPlayer == null ? -1 : turnPlayer.getId(),
                        view.getStack() == null ? -1 : view.getStack().size(),
                        source.stateBearing(), snapshotZones(new Object[] {view},
                                "callback=" + callback + ",endpoint=remote,seat=" + seat
                                        + ",callbackOrdinal=" + source.callbackOrdinal()
                                        + ",callbackSequence=" + source.sequence()
                                        + ",incomingGameViewId=" + incomingGameViewId
                                        + ",preAppliedGameViewId=" + preAppliedGameViewId
                                        + ",input=" + ("setGameView".equals(callback) ? "GameView" : "DeltaPacket")
                                        + ("applyDelta".equals(callback) ? ",packetSequence=" + source.sequence() : "")
                                        + ",source=applied getGameView"),
                        drawCounts, privateTopVisibility));
            } catch (Throwable error) {
                scenario.fail(error);
            }
        }

        Map<Integer, List<Boolean>> visibilityFacts(List<Integer> ids, int playerViewId) {
            GameView view = getGameView();
            PlayerView player = findPlayerView(view, playerViewId);
            if (view == null || player == null) throw new AssertionError("Assigned endpoint has no current privacy view");
            Map<Integer, List<Boolean>> facts = new LinkedHashMap<>();
            for (int id : ids) {
                CardView card = findVisibleCard(view, id);
                if (card == null) throw new AssertionError("Assigned endpoint lacks physical privacy card view " + id);
                facts.put(id, List.of(card.mayPlayerLook(player), card.canBeShownTo(player)));
            }
            return Collections.unmodifiableMap(facts);
        }

        PrivacyHeldEndpointSnapshot snapshotPrivacyHeldEndpoint(List<Integer> privateTopIds, int negativeHandId) {
            GameView view = getGameView();
            if (view == null) {
                throw new AssertionError("Assigned endpoint has no current held-choice view");
            }
            PlayerView ownPlayer = findPlayerView(view, ownPlayerViewId);
            if (ownPlayer == null) {
                throw new AssertionError("Assigned endpoint has no current held-choice view");
            }
            Map<Integer, List<Boolean>> privateFacts = new LinkedHashMap<>();
            for (int cardId : privateTopIds) {
                CardView card = findVisibleCard(view, cardId);
                if (card == null) throw new AssertionError("Held-choice endpoint is missing an expected private card view");
                privateFacts.put(cardId, List.of(card.mayPlayerLook(ownPlayer), card.canBeShownTo(ownPlayer)));
            }
            CardView negativeHandCard = findVisibleCard(view, negativeHandId);
            if (negativeHandCard == null) {
                throw new AssertionError("Held-choice endpoint is missing the known negative hand control");
            }
            List<Boolean> negativeFacts = List.of(
                    negativeHandCard.mayPlayerLook(ownPlayer), negativeHandCard.canBeShownTo(ownPlayer));
            return new PrivacyHeldEndpointSnapshot(view.getId(), ownPlayerViewId,
                    snapshotZones(new Object[] {view}), privateFacts, negativeFacts);
        }

        private Map<Integer, Map<ZoneType, List<Integer>>> snapshotZones(Object[] args) {
            return snapshotZones(args, "source=unspecified");
        }

        private Map<Integer, Map<ZoneType, List<Integer>>> snapshotZones(Object[] args, String source) {
            if (args.length > 0 && args[0] instanceof GameView view) {
                Map<Integer, Map<ZoneType, List<Integer>>> result = new LinkedHashMap<>();
                for (Object value : view.getPlayers().toArray()) {
                    if (!(value instanceof PlayerView player)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                    EnumMap<ZoneType, List<Integer>> byZone = new EnumMap<>(ZoneType.class);
                    for (ZoneType zone : relevantZones()) {
                        byZone.put(zone, cardIds(player.getCards(zone), source + ",gameViewId=" + view.getId()
                                + ",playerViewId=" + player.getId() + ",zone=" + zone));
                    }
                    result.put(player.getId(), Collections.unmodifiableMap(byZone));
                }
                return Collections.unmodifiableMap(result);
            }
            if (args.length > 0 && args[0] instanceof DeltaPacket packet) {
                Map<Integer, Map<ZoneType, List<Integer>>> result = new LinkedHashMap<>();
                Map<Integer, Map<TrackableProperty, Object>> objects = new LinkedHashMap<>();
                objects.putAll(packet.getNewObjects());
                objects.putAll(packet.getObjectDeltas());
                for (Map.Entry<Integer, Map<TrackableProperty, Object>> entry : objects.entrySet()) {
                    if (DeltaPacket.getTypeFromDeltaKey(entry.getKey()) != DeltaPacket.TYPE_PLAYER_VIEW) continue;
                    EnumMap<ZoneType, List<Integer>> byZone = new EnumMap<>(ZoneType.class);
                    int playerViewId = DeltaPacket.getIdFromDeltaKey(entry.getKey());
                    byZone.put(ZoneType.Hand, cardIds(entry.getValue().get(TrackableProperty.Hand),
                            source + ",deltaPlayerViewId=" + playerViewId + ",zone=" + ZoneType.Hand));
                    byZone.put(ZoneType.Library, cardIds(entry.getValue().get(TrackableProperty.Library),
                            source + ",deltaPlayerViewId=" + playerViewId + ",zone=" + ZoneType.Library));
                    result.put(playerViewId, Collections.unmodifiableMap(byZone));
                }
                return Collections.unmodifiableMap(result);
            }
            return Map.of();
        }

        private List<Integer> cardIds(Object value) {
            return cardIds(value, "source=unspecified");
        }

        private List<Integer> cardIds(Object value, String source) {
            if (value == null) return List.of();
            if (!(value instanceof Iterable<?> values)) {
                throw new AssertionError("Zone snapshot is not iterable; valueClass=" + value.getClass().getName()
                        + "," + source);
            }
            List<Integer> ids = new ArrayList<>();
            if (values instanceof Collection<?> collection) {
                Object[] items = collection.toArray();
                for (int index = 0; index < items.length; index++) {
                    Object item = items[index];
                    if (item instanceof CardView card) ids.add(card.getId());
                    else if (item instanceof Number id) ids.add(id.intValue());
                    else throw unsupportedCardIdEntry(item, index, items.length, source);
                }
            } else {
                int index = 0;
                for (Object item : values) {
                    if (item instanceof CardView card) ids.add(card.getId());
                    else if (item instanceof Number id) ids.add(id.intValue());
                    else throw unsupportedCardIdEntry(item, index, -1, source);
                    index++;
                }
            }
            return List.copyOf(ids);
        }

        private AssertionError unsupportedCardIdEntry(Object item, int index, int count, String source) {
            return new AssertionError("Card ID collection contains an unsupported entry; index=" + index
                    + ",count=" + (count < 0 ? "unknown" : count)
                    + ",entryClass=" + (item == null ? "null" : item.getClass().getName()) + "," + source);
        }

        private List<ZoneType> relevantZones() {
            return List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile);
        }

        private HandFacts snapshotOwnHand() {
            GameView view = getGameView();
            if (view == null || ownPlayerViewId < 0) throw new AssertionError("Missing assigned view for mulligan callback");
            for (PlayerView player : view.getPlayers()) {
                if (player.getId() != ownPlayerViewId) continue;
                List<Integer> ids = new ArrayList<>();
                int lands = 0;
                if (player.getHand() != null) for (CardView card : player.getHand()) {
                    ids.add(card.getId());
                    if (card.getCurrentState().getType() != null && card.getCurrentState().getType().isLand()) lands++;
                }
                return new HandFacts(ids, lands);
            }
            throw new AssertionError("Assigned PlayerView is absent from the current GameView");
        }

        private PendingOpeningKeep captureOpeningKeep(Capture prompt, PlayerView owner,
                String one, String two, boolean enabledOne, boolean enabledTwo, Decision decision,
                int expectedHandCount, int londonBottomCardId, long appliedStateLowerBoundOrdinal) {
            GameView view = getGameView();
            IGameController endpointController = owner == null ? null : getGameController(owner);
            Game engine = scenario.openingKeepEngineGame();
            if (prompt == null || owner == null || owner.getId() != ownPlayerViewId || !Localizer.getInstance().getMessage("lblKeep").equalsIgnoreCase(one) || !Localizer.getInstance().getMessage("lblMulligan").equalsIgnoreCase(two) || !enabledOne || !enabledTwo || view == null || !(endpointController instanceof NetGameController) || engine == null || seat < 0 || seat >= engine.getPlayers().size() || engine.getPlayers().get(seat).getId() != ownPlayerViewId || !(engine.getPlayers().get(seat).getController() instanceof PlayerControllerHuman controller) || controller.getGame() != engine || engine.getView() == null || engine.getView().getId() != view.getId()) {
                fail("Remote opening Keep callback lacks its assigned NetGameController and same authoritative Game");
                return null;
            }
            PrivacyInputSnapshot inputs = new PrivacyInputSnapshot(
                    controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
            if (inputs.proxyInput() == null || inputs.proxyInput() != inputs.queuedInput() || !(inputs.proxyInput() instanceof InputConfirmMulligan input) || input.getOwner() == null || input.getOwner().getId() != ownPlayerViewId) {
                fail("Opening Keep callback lacks its exact current InputConfirmMulligan: "
                        + inputs.evidence());
                return null;
            }
            return new PendingOpeningKeep(engine, controller, endpointController, input, view.getId(), seat,
                    ownPlayerViewId, prompt, one, two, enabledOne, enabledTwo, decision,
                    expectedHandCount, londonBottomCardId, appliedStateLowerBoundOrdinal, new AtomicBoolean());
        }

        private List<Integer> engineCardIds(Player player, ZoneType zone) {
            List<Integer> ids = new ArrayList<>();
            for (Card card : player.getCardsIn(zone)) ids.add(card.getId());
            return List.copyOf(ids);
        }

        private boolean openingKeepServerZonesMatch(PendingOpeningKeep pending) {
            if (pending == null || pending.seat() < 0 || pending.seat() >= pending.game().getPlayers().size()) {
                return false;
            }
            Player player = pending.game().getPlayers().get(pending.seat());
            if (player.getId() != pending.chooserViewId()) return false;
            List<Integer> handIds = engineCardIds(player, ZoneType.Hand);
            if (handIds.size() != pending.expectedHandCount()) return false;
            return pending.londonBottomCardId() < 0
                    || !handIds.contains(pending.londonBottomCardId())
                    && engineCardIds(player, ZoneType.Library).contains(pending.londonBottomCardId());
        }

        private boolean sameOpeningKeep(PendingOpeningKeep left, PendingOpeningKeep right) {
            return left.game() == right.game() && left.controller() == right.controller()
                    && left.endpointController() == right.endpointController()
                    && left.input() == right.input() && left.gameViewId() == right.gameViewId()
                    && left.seat() == right.seat() && left.chooserViewId() == right.chooserViewId()
                    && left.oneLabel().equals(right.oneLabel()) && left.twoLabel().equals(right.twoLabel())
                    && left.oneEnabled() == right.oneEnabled() && left.twoEnabled() == right.twoEnabled()
                    && left.decision() == right.decision()
                    && left.expectedHandCount() == right.expectedHandCount()
                    && left.londonBottomCardId() == right.londonBottomCardId()
                    && (left.londonBottomCardId() < 0
                            || left.appliedStateLowerBoundOrdinal() == right.appliedStateLowerBoundOrdinal());
        }

        private boolean absorbClaimedOpeningKeepRepeat(PlayerView owner, String one, String two,
                boolean enabledOne, boolean enabledTwo) {
            PendingOpeningKeep pending = pendingOpeningKeep;
            if (pending == null || !pending.dispatched().get()) return false;
            GameView view = getGameView();
            IGameController endpoint = owner == null ? null : getGameController(owner);
            PrivacyInputSnapshot input = new PrivacyInputSnapshot(
                    pending.controller().getInputProxy().getInput(), pending.controller().getInputQueue().getInput());
            boolean sameInput = owner != null && owner.getId() == pending.chooserViewId()
                    && view != null && view.getId() == pending.gameViewId()
                    && endpoint == pending.endpointController()
                    && input.proxyInput() == pending.input() && input.queuedInput() == pending.input();
            if (sameInput) {
                boolean samePrompt = Localizer.getInstance().getMessage("lblKeep").equalsIgnoreCase(one)
                        && Localizer.getInstance().getMessage("lblMulligan").equalsIgnoreCase(two)
                        && enabledOne == pending.oneEnabled() && enabledTwo == pending.twoEnabled();
                if (samePrompt) return true;
                fail("Claimed opening Keep changed its prompt while the exact InputConfirmMulligan remained live");
                return true;
            }
            pendingOpeningKeep = null;
            return false;
        }

        private OpeningKeepAppliedWitness currentOpeningKeepWitness(PendingOpeningKeep pending,
                long maximumOrdinal) {
            GameView current = getGameView();
            if (current == null || current.getId() != pending.gameViewId() || pending.controller().getGame() != pending.game() || pending.game().getView() == null || pending.game().getView().getId() != pending.gameViewId()) return null;
            GameViewReceipt receipt = gameViewReceipts().stream()
                    .filter(item -> item.gameViewId() == pending.gameViewId()
                            && item.trackerIdentity() == current.getTracker()
                            && item.callbackOrdinal() > pending.appliedStateLowerBoundOrdinal()
                            && item.callbackOrdinal() <= maximumOrdinal
                            && (item.stateBearing() || item.callback().equals("setGameView"))
                            && item.zonesByPlayer().getOrDefault(pending.chooserViewId(), Map.of())
                                    .getOrDefault(ZoneType.Hand, List.of()).size() == pending.expectedHandCount()
                            && (pending.londonBottomCardId() < 0
                                    || !item.zonesByPlayer().getOrDefault(pending.chooserViewId(), Map.of())
                                            .getOrDefault(ZoneType.Hand, List.of())
                                            .contains(pending.londonBottomCardId())
                                    && item.zonesByPlayer().getOrDefault(pending.chooserViewId(), Map.of())
                                            .getOrDefault(ZoneType.Library, List.of())
                                            .contains(pending.londonBottomCardId())))
                    .max(Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
            if (receipt == null) return null;
            PlayerView own = findPlayerView(current, pending.chooserViewId());
            if (own == null || own.getHand() == null || getGameController(own) != pending.endpointController()) return null;
            List<Integer> handIds = cardIds(own.getHand());
            List<Integer> receiptHand = receipt.zonesByPlayer().getOrDefault(pending.chooserViewId(), Map.of())
                    .getOrDefault(ZoneType.Hand, List.of());
            if (handIds.size() != pending.expectedHandCount() || !handIds.equals(receiptHand)) return null;
            List<Integer> currentLibrary = own.getCards(ZoneType.Library) == null
                    ? List.of() : cardIds(own.getCards(ZoneType.Library));
            boolean bottomInLibrary = pending.londonBottomCardId() >= 0
                    && currentLibrary.contains(pending.londonBottomCardId())
                    && !handIds.contains(pending.londonBottomCardId());
            if (pending.londonBottomCardId() >= 0 && !bottomInLibrary) return null;
            return new OpeningKeepAppliedWitness(receipt.callbackOrdinal(), receipt.sequence(),
                    receipt.gameViewId(), receipt.trackerIdentity(), handIds, bottomInLibrary);
        }

        private void observePendingOpeningKeep(Capture appliedCapture, GameView appliedView) {
            PendingOpeningKeep pending = pendingOpeningKeep;
            if (pending == null || pending.dispatched().get() || scenario.stopped() || appliedCapture == null || appliedView == null || appliedCapture.callbackOrdinal() <= pending.appliedStateLowerBoundOrdinal() || !(appliedCapture.callback().equals("setGameView")
                            || appliedCapture.callback().equals("applyDelta") && appliedCapture.stateBearing())) return;
            if (appliedView.getId() != pending.gameViewId() || getGameView() != appliedView || pending.controller().getGame() != pending.game() || pending.game().getView() == null || pending.game().getView().getId() != pending.gameViewId()) {
                fail("Applied opening Keep state changed the retained same-Game/GameView identity");
                return;
            }
            PrivacyInputSnapshot currentInput = new PrivacyInputSnapshot(
                    pending.controller().getInputProxy().getInput(), pending.controller().getInputQueue().getInput());
            if (currentInput.proxyInput() != pending.input() || currentInput.queuedInput() != pending.input() || pending.input().getOwner() == null || pending.input().getOwner().getId() != pending.chooserViewId()) {
                fail("Applied opening Keep state no longer has its retained InputConfirmMulligan: "
                        + currentInput.evidence());
                return;
            }
            OpeningKeepAppliedWitness witness = currentOpeningKeepWitness(pending,
                    appliedCapture.callbackOrdinal());
            if (witness == null) return;
            completeOpeningKeep(pending, witness);
        }

        private void completeOpeningKeep(PendingOpeningKeep pending, OpeningKeepAppliedWitness witness) {
            if (pendingOpeningKeep != pending || !pending.dispatched().compareAndSet(false, true) || witness == null || witness.gameViewId() != pending.gameViewId() || witness.handIds().size() != pending.expectedHandCount() || scenario.stopped()) return;
            GameView current = getGameView();
            PlayerView owner = current == null ? null : findPlayerView(current, pending.chooserViewId());
            IGameController currentEndpoint = owner == null ? null : getGameController(owner);
            PrivacyInputSnapshot currentInput = new PrivacyInputSnapshot(
                    pending.controller().getInputProxy().getInput(), pending.controller().getInputQueue().getInput());
            if (current == null || current.getId() != pending.gameViewId() || current.getTracker() != witness.trackerIdentity() || owner == null || owner.getHand() == null || cardIds(owner.getHand()).size() != pending.expectedHandCount() || pending.controller().getGame() != pending.game() || pending.game().getView() == null || pending.game().getView().getId() != pending.gameViewId() || !openingKeepServerZonesMatch(pending) || currentEndpoint != pending.endpointController() || currentInput.proxyInput() != pending.input() || currentInput.queuedInput() != pending.input() || pending.input().getOwner() == null || pending.input().getOwner().getId() != pending.chooserViewId()) {
                fail("Opening Keep dispatch lost the applied seven-card hand or exact live input");
                return;
            }
            if (script.peekFirst() == null || script.peekFirst().decision() != pending.decision() || script.peekFirst().seat() != pending.seat()) {
                fail("Opening Keep dispatch no longer matches its retained scripted decision");
                return;
            }
            HandFacts hand = snapshotOwnHand();
            if (!hand.ids().equals(witness.handIds())) {
                fail("Opening Keep hand changed after its applied state witness");
                return;
            }
            scenario.recordOpeningKeep(pending.seat(), pending.chooserViewId(), pending.prompt().callbackOrdinal(),
                    pending.gameViewId(), "updateButtons", pending.decision().name(), hand.ids(), null);
            if (scenario.lifecycleScenario()) recordLifecycleKeep(pending.prompt(), hand);
            consume(script.peekFirst());
            scenario.recordAction(new ActionReceipt(pending.decision(), pending.seat(), pending.chooserViewId(), null, true));
            queueAction(() -> {
                GameView dispatchView = getGameView();
                PlayerView dispatchOwner = dispatchView == null ? null : findPlayerView(dispatchView, pending.chooserViewId());
                IGameController dispatchEndpoint = dispatchOwner == null ? null : getGameController(dispatchOwner);
                List<Integer> dispatchHandIds = dispatchOwner == null || dispatchOwner.getHand() == null
                        ? List.of() : cardIds(dispatchOwner.getHand());
                List<Integer> dispatchLibraryIds = dispatchOwner == null
                        || dispatchOwner.getCards(ZoneType.Library) == null
                                ? List.of() : cardIds(dispatchOwner.getCards(ZoneType.Library));
                boolean dispatchBottomInLibrary = pending.londonBottomCardId() < 0
                        || dispatchLibraryIds.contains(pending.londonBottomCardId())
                        && !dispatchHandIds.contains(pending.londonBottomCardId());
                PrivacyInputSnapshot dispatchInput = new PrivacyInputSnapshot(
                        pending.controller().getInputProxy().getInput(), pending.controller().getInputQueue().getInput());
                if (scenario.stopped()) return;
                if (dispatchView == null || dispatchView.getId() != pending.gameViewId() || dispatchView.getTracker() != witness.trackerIdentity() || dispatchOwner == null || dispatchOwner.getHand() == null || dispatchHandIds.size() != pending.expectedHandCount() || !dispatchHandIds.equals(witness.handIds()) || !dispatchBottomInLibrary || pending.controller().getGame() != pending.game() || !openingKeepServerZonesMatch(pending) || dispatchEndpoint != pending.endpointController() || dispatchInput.proxyInput() != pending.input() || dispatchInput.queuedInput() != pending.input()) {
                    fail("Opening Keep's single OK lost the same Game, applied hand, or InputConfirmMulligan");
                    return;
                }
                pending.endpointController().selectButtonOk();
                scenario.recordOpeningKeepGateDispatch(new OpeningKeepGateReceipt(pending.seat(),
                        witness.gameViewId(), pending.prompt().callbackOrdinal(),
                        pending.appliedStateLowerBoundOrdinal(), witness.callbackOrdinal(),
                        witness.sequence(), pending.input().getClass(), hand.ids().size(),
                        pending.londonBottomCardId(), witness.bottomInSharedLibrary(),
                        dispatchView.getId() == pending.gameViewId()
                                && pending.controller().getGame() == pending.game(),
                        dispatchView.getTracker() == witness.trackerIdentity(), 1));
            });
        }

        private boolean routeOpeningKeepAfterAppliedHand(Capture prompt, PlayerView owner,
                String one, String two, boolean enabledOne, boolean enabledTwo, ExpectedAction expected) {
            boolean openingKeepScenario = scenario.metamorphoseScenario() || scenario.privacyScenario()
                    || scenario.lifecycleScenario();
            if (!openingKeepScenario || !remote || expected == null || expected.decision() != Decision.KEEP_WITHOUT_LAND) return false;
            if (owner == null || owner.getId() != ownPlayerViewId) {
                fail("Remote opening Keep callback did not belong to its assigned PlayerView");
                return true;
            }
            int expectedHandCount = 7;
            int londonBottomCardId = -1;
            long appliedStateLowerBoundOrdinal = prompt == null ? -1 : prompt.callbackOrdinal();
            GameView currentView = getGameView();
            if (scenario.privacyMulliganPrefixScenario()) {
                if (seat != 1 || expected.seat() != 1) {
                    fail("Composite opening Keep readiness gate was routed outside the regular-mulligan seat");
                    return true;
                }
                List<LondonBottomDispatchReceipt> bottoms = scenario.londonBottomDispatches();
                String zeroPrompt = String.format(Localizer.getInstance().getMessage("lblReturnForLondon"), 0);
                LondonBottomDispatchReceipt bottom = bottoms.size() == 1 ? bottoms.get(0) : null;
                if (bottom == null || bottom.seat() != 1 || bottom.chooserViewId() != ownPlayerViewId || prompt == null || currentView == null || bottom.gameViewId() != currentView.getId() || bottom.chooserHandIds().size() != 7 || !bottom.chooserHandIds().contains(bottom.cardId()) || bottom.zeroCardPromptOrdinal() <= bottom.promptOrdinal() || !zeroPrompt.equals(bottom.zeroCardPrompt())) {
                    fail("Composite regular Keep lacks its exact completed London selection/zero-card prompt receipt");
                    return true;
                }
                expectedHandCount = 6;
                londonBottomCardId = bottom.cardId();
                appliedStateLowerBoundOrdinal = bottom.zeroCardPromptOrdinal();
            }
            Game retainedOpeningGame = scenario.openingKeepEngineGame();
            if (retainedOpeningGame == null || currentView == null || retainedOpeningGame.getView() == null || retainedOpeningGame.getView().getId() != currentView.getId()) {
                if (scenario.privacyMulliganPrefixScenario()) {
                    fail("Composite regular Keep lacks its retained authoritative same-Game identity");
                    return true;
                }
                return false;
            }
            PendingOpeningKeep candidate = captureOpeningKeep(prompt, owner, one, two,
                    enabledOne, enabledTwo, expected.decision(), expectedHandCount,
                    londonBottomCardId, appliedStateLowerBoundOrdinal);
            if (candidate == null || scenario.stopped()) return true;
            if (!openingKeepServerZonesMatch(candidate)) {
                fail("Opening Keep lacks the exact live engine hand and completed London shared-library placement");
                return true;
            }
            PendingOpeningKeep existing = pendingOpeningKeep;
            if (existing != null) {
                if (!sameOpeningKeep(existing, candidate)) {
                    fail("Opening Keep prompt changed before its expected applied hand was available");
                }
                return true;
            }
            if (expectedHandCount == 7 && snapshotOwnHand().ids().size() == expectedHandCount) return false;
            if (expected.seat() != seat) {
                fail("Opening Keep script decision belongs to another assigned seat");
                return true;
            }
            pendingOpeningKeep = candidate;
            scenario.recordOpeningKeepDeferred(candidate.seat(), candidate.gameViewId(),
                    candidate.prompt().callbackOrdinal());
            HandFacts hand = snapshotOwnHand();
            if (hand.ids().size() == expectedHandCount) {
                OpeningKeepAppliedWitness witness = currentOpeningKeepWitness(candidate,
                        Long.MAX_VALUE);
                if (witness != null) completeOpeningKeep(candidate, witness);
                else if (expectedHandCount == 7) {
                    fail("Opening Keep already shows seven cards without a matching applied-state receipt");
                }
                // For the composite six-card path, the bounded observer owns any still-in-flight state packet.
                return true;
            }
            // The matching initial-deal/London state observer completes this retained prompt after applying its packet.
            // Keep the exact input pending; never block the EDT or send an OK against a zero-card view.
            return true;
        }

        private String openingKeepSnapshotDiagnostic(Capture callbackCapture, GameView currentView) {
            Game engine = scenario.metamorphoseOpeningEngineGame();
            int engineViewId = engine == null || engine.getView() == null ? -1 : engine.getView().getId();
            long callbackOrdinal = callbackCapture == null ? -1 : callbackCapture.callbackOrdinal();
            List<GameViewReceipt> receipts = gameViewReceipts();
            GameViewReceipt latestState = receipts.stream()
                    .filter(receipt -> receipt.stateBearing() && receipt.callbackOrdinal() <= callbackOrdinal)
                    .max(java.util.Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
            Capture decoded = latestState == null ? null : captures().stream()
                    .filter(capture -> capture.callbackOrdinal() == latestState.callbackOrdinal())
                    .findFirst().orElse(null);
            PlayerView owner = findPlayerView(currentView, ownPlayerViewId);
            PlayerView trackedOwner = currentView == null || currentView.getTracker() == null ? null
                    : currentView.getTracker().getObj(TrackableTypes.PlayerViewType, ownPlayerViewId);
            List<String> engineHandCountsBySeat = engine == null ? List.of()
                    : java.util.stream.IntStream.range(0, engine.getPlayers().size())
                            .mapToObj(index -> "seat" + index + "="
                                    + engine.getPlayers().get(index).getCardsIn(ZoneType.Hand).size()).toList();
            return "openingSnapshot={engineGameIdentity=" + (engine == null ? -1 : System.identityHashCode(engine))
                    + ",engineGameViewId=" + engineViewId
                    + ",currentGameViewId=" + (currentView == null ? -1 : currentView.getId())
                    + ",sameGameView=" + (engineViewId >= 0 && currentView != null
                            && engineViewId == currentView.getId())
                    + ",callbackOrdinal=" + callbackOrdinal
                    + ",callbackSequence=" + (callbackCapture == null ? -1 : callbackCapture.sequence())
                    + ",engineHandCountsBySeat=" + engineHandCountsBySeat
                    + ",decodedHandCountsBySeat=" + (decoded == null ? "missing"
                            : decoded.callback().equals("applyDelta") ? "unavailable-partialDeltaPacket"
                                    : handCountsBySeat(decoded.zonesByPlayer()))
                    + ",decodedScope=" + (decoded == null ? "missing"
                            : decoded.callback().equals("applyDelta") ? "partialDeltaPacket" : "fullGameView")
                    + ",appliedHandCountsBySeat=" + (latestState == null ? "missing"
                            : handCountsBySeat(latestState.zonesByPlayer()))
                    + ",latestStateCallback=" + (latestState == null ? "missing" : latestState.callback())
                    + ",latestStateSequence=" + (latestState == null ? -1 : latestState.sequence())
                    + ",latestStateOrdinal=" + (latestState == null ? -1 : latestState.callbackOrdinal())
                    + ",latestIncomingGameViewId=" + (latestState == null ? -1 : latestState.incomingGameViewId())
                    + ",latestAppliedGameViewId=" + (latestState == null ? -1 : latestState.gameViewId())
                    + ",latestStateSameGameView=" + (latestState != null && currentView != null
                            && latestState.gameViewId() == currentView.getId())
                    + ",latestStateTrackerSame=" + (latestState != null && currentView != null
                            && latestState.trackerIdentity() == currentView.getTracker())
                    + ",ownerTrackerSame=" + (owner != null && owner == trackedOwner)
                    + ",engineTrackerSame=" + (engine != null && currentView != null
                            && engine.getTracker() == currentView.getTracker()) + "}";
        }

        private String handCountsBySeat(Map<Integer, Map<ZoneType, List<Integer>>> zonesByPlayer) {
            return zonesByPlayer.entrySet().stream()
                    .map(entry -> "seat" + scenario.seatForView(entry.getKey()) + "="
                            + (entry.getValue().containsKey(ZoneType.Hand)
                                    ? entry.getValue().get(ZoneType.Hand).size() : "missing"))
                    .sorted().toList().toString();
        }

        private void recordLifecycleKeep(Capture callbackCapture, HandFacts hand) {
            GameView current = getGameView();
            PlayerView viewPlayer = findPlayerView(current, ownPlayerViewId);
            PlayerView trackerPlayer = current == null || current.getTracker() == null ? null
                    : current.getTracker().getObj(TrackableTypes.PlayerViewType, ownPlayerViewId);
            List<GameViewReceipt> receipts = gameViewReceipts();
            GameViewReceipt latestReceipt = callbackCapture == null ? null : receipts.stream()
                    .filter(receipt -> receipt.callbackOrdinal() <= callbackCapture.callbackOrdinal())
                    .max(java.util.Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
            scenario.recordLifecycleKeep(current == null ? -1 : current.getId(),
                    callbackCapture == null ? -1 : callbackCapture.callbackOrdinal(),
                    callbackCapture == null ? -1 : callbackCapture.sequence(),
                    callbackCapture == null ? "<missing>" : callbackCapture.callback(),
                    seat, ownPlayerViewId, hand.ids(), viewPlayer != null && viewPlayer == trackerPlayer,
                    viewPlayer == null || viewPlayer.getHand() == null ? -1 : viewPlayer.getHand().size(),
                    trackerPlayer == null || trackerPlayer.getHand() == null ? -1 : trackerPlayer.getHand().size(),
                    latestReceipt == null ? -1 : latestReceipt.incomingGameViewId(),
                    latestReceipt == null ? -1 : latestReceipt.gameViewId(),
                    latestReceipt == null ? -1 : latestReceipt.callbackOrdinal());
        }

        private void recordPrivacyHistoryNextGameKeep(Capture callbackCapture, HandFacts hand) {
            GameView current = getGameView();
            PlayerView viewPlayer = findPlayerView(current, ownPlayerViewId);
            PlayerView trackerPlayer = current == null || current.getTracker() == null ? null
                    : current.getTracker().getObj(TrackableTypes.PlayerViewType, ownPlayerViewId);
            IGameController endpointController = viewPlayer == null ? null : getGameController(viewPlayer);
            Game engine = scenario.privacyHistoryNextGame();
            Player enginePlayer = engine == null || seat < 0 || seat >= engine.getPlayers().size()
                    ? null : engine.getPlayers().get(seat);
            PlayerControllerHuman engineController = enginePlayer != null
                    && enginePlayer.getController() instanceof PlayerControllerHuman human ? human : null;
            boolean endpointControllerBound = remote ? endpointController instanceof NetGameController
                    : endpointController == engineController;
            boolean controllerOwnsGame = engineController != null && engineController.getGame() == engine
                    && enginePlayer.getId() == ownPlayerViewId && endpointControllerBound;
            scenario.recordPrivacyHistoryNextGameKeep(current == null ? -1 : current.getId(),
                    callbackCapture == null ? -1 : callbackCapture.callbackOrdinal(),
                    callbackCapture == null ? -1 : callbackCapture.sequence(), seat, ownPlayerViewId,
                    hand.ids(), current == null ? null : current.getTracker(),
                    viewPlayer != null && viewPlayer == trackerPlayer, controllerOwnsGame);
        }

        private static PlayerView findPlayerView(GameView view, int playerId) {
            if (view == null || view.getPlayers() == null) return null;
            for (Object value : view.getPlayers().toArray()) {
                if (!(value instanceof PlayerView player)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                if (player.getId() == playerId) return player;
            }
            return null;
        }

        private static CardView findVisibleCard(GameView view, int cardId) {
            if (view == null || view.getPlayers() == null) return null;
            for (Object value : view.getPlayers().toArray()) {
                if (!(value instanceof PlayerView player)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                for (ZoneType zone : List.of(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield,
                        ZoneType.Graveyard, ZoneType.Exile)) {
                    var cards = player.getCards(zone);
                    if (cards == null) continue;
                    for (Object cardValue : cards.toArray()) {
                        if (!(cardValue instanceof CardView card)) throw new AssertionError("Player zone contains a non-CardView entry");
                        if (card.getId() == cardId) return card;
                    }
                }
            }
            return null;
        }

        private String expectedFreeRevealMessage(String actualMessage) {
            MulliganChoiceReceipt freeChoice = scenario.freeMulliganChoice();
            GameView game = getGameView();
            if (freeChoice == null || game == null) throw new AssertionError("Missing actual free-mulligan owner view");
            PlayerView owner = null;
            PlayerView receiver = null;
            for (Object value : game.getPlayers().toArray()) {
                if (!(value instanceof PlayerView candidate)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                if (candidate.getId() == freeChoice.chooserViewId()) owner = candidate;
                if (candidate.getId() == ownPlayerViewId) receiver = candidate;
            }
            if (owner == null || receiver == null || seat != 1 - freeChoice.seat()) {
                throw new AssertionError("Free-mulligan reveal did not reach the other assigned PlayerView");
            }
            Localizer localizer = Localizer.getInstance();
            String prefix = localizer.getMessage("lblDanDanFreeMulliganRevealed", owner.getName());
            String suffix = localizer.getMessage("lblPlayerZone", "{player's}",
                    ZoneType.Hand.getTranslatedName().toLowerCase());
            String expected = MessageUtil.formatMessage(prefix + " " + suffix, receiver, owner);
            if (!actualMessage.startsWith(prefix)) {
                throw new AssertionError("Free-mulligan reveal prefix did not identify its actual owner");
            }
            return expected;
        }

        private Capture capture(String callback, long sequence, Object... args) {
            try {
                if ("handleGameEvents".equals(callback) && args.length > 0 && args[0] instanceof List<?> events) {
                    return captureAppliedEvents(events);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(args); }
                List<Integer> publicIds = new ArrayList<>();
                int events = 0;
                int wrappedEvents = 0;
                byte[] wrappedEventBytes = new byte[0];
                boolean state = false;
                if (args.length > 0 && args[0] instanceof DeltaPacket packet) {
                    Map<Integer, Map<TrackableProperty, Object>> objects = new LinkedHashMap<>();
                    objects.putAll(packet.getNewObjects());
                    objects.putAll(packet.getObjectDeltas());
                    state = !objects.isEmpty();
                    for (Map.Entry<Integer, Map<TrackableProperty, Object>> entry : objects.entrySet()) {
                        if (DeltaPacket.getTypeFromDeltaKey(entry.getKey()) != DeltaPacket.TYPE_CARD_VIEW) continue;
                        Object zone = entry.getValue().get(TrackableProperty.Zone);
                        if (zone == ZoneType.Battlefield || zone == ZoneType.Graveyard || zone == ZoneType.Exile) {
                            publicIds.add(DeltaPacket.getIdFromDeltaKey(entry.getKey()));
                        }
                    }
                    events = packet.getEvents() == null ? 0 : packet.getEvents().size();
                    if (events > 0) {
                        List<Object> envelopes = new ArrayList<>(packet.getEvents());
                        for (Object envelope : envelopes) {
                            if (!"forge.gamemodes.net.TrackableSerializer$WrappedEvent".equals(envelope.getClass().getName())) {
                                throw new AssertionError("Expected wrapped event at pre-unwrap applyDelta boundary, got "
                                        + envelope.getClass().getName());
                            }
                        }
                        ByteArrayOutputStream eventBytes = new ByteArrayOutputStream();
                        try (ObjectOutputStream out = new ObjectOutputStream(eventBytes)) { out.writeObject(envelopes); }
                        wrappedEventBytes = eventBytes.toByteArray();
                        wrappedEvents = envelopes.size();
                    }
                } else if ("handleGameEvents".equals(callback) && args.length > 0 && args[0] instanceof List<?> list) {
                    events = list.size();
                }
                if ("setGameView".equals(callback) && args.length > 0 && args[0] instanceof GameView view) {
                    for (Object value : view.getPlayers().toArray()) {
                        if (!(value instanceof PlayerView player)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                        for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile)) {
                            publicIds.addAll(cardIds(player.getCards(zone), "callback=" + callback
                                    + ",endpoint=" + (remote ? "remote" : "host") + ",seat=" + seat
                                    + ",callbackOrdinal=pre-receipt,callbackSequence=" + sequence
                                    + ",incomingGameViewId=" + view.getId()
                                    + ",playerViewId=" + player.getId() + ",zone=" + zone
                                    + ",source=incoming argument"));
                        }
                    }
                }
                String viewInput = args.length > 0 && args[0] instanceof GameView view
                        ? "incomingGameViewId=" + view.getId() : "incomingGameViewId=unavailable";
                Capture saved = new Capture(callback, sequence, callbackOrdinals.incrementAndGet(), bytes.toByteArray(),
                        snapshotZones(args, "callback=" + callback + ",endpoint=" + (remote ? "remote" : "host")
                                + ",seat=" + seat + ",callbackOrdinal=pre-receipt,callbackSequence=" + sequence
                                + "," + viewInput
                                + ",source=incoming argument"),
                        publicIds, state, events, wrappedEvents, wrappedEventBytes,
                        -1, -1, List.of());
                captures.add(saved);
                return saved;
            } catch (Throwable t) {
                scenario.fail(t);
                return null;
            }
        }

        private Capture captureAppliedEvents(List<?> events) {
            try {
                List<EventFact> facts = new ArrayList<>(events.size());
                for (Object event : events) {
                    if (!(event instanceof GameEvent gameEvent)) {
                        throw new AssertionError("Resolved event callback contained "
                                + (event == null ? "null" : event.getClass().getName()));
                    }
                    facts.add(snapshotEventFacts(gameEvent));
                }
                Capture parent = applyingPacket.get();
                long sequence = parent == null ? -1 : parent.sequence();
                long parentOrdinal = parent == null ? -1 : parent.callbackOrdinal();
                Capture saved = new Capture("handleGameEvents", sequence, callbackOrdinals.incrementAndGet(),
                        new byte[0], Map.of(), List.of(), false, facts.size(), 0, new byte[0],
                        sequence, parentOrdinal, facts);
                captures.add(saved);
                return saved;
            } catch (Throwable t) {
                scenario.fail(t);
                return null;
            }
        }

        private EventFact snapshotEventFacts(GameEvent event) throws ReflectiveOperationException {
            List<Integer> playerIds = new ArrayList<>();
            List<Integer> cardIds = new ArrayList<>();
            List<Integer> spellAbilityIds = new ArrayList<>();
            List<Integer> stackItemIds = new ArrayList<>();
            List<ZoneFact> zones = new ArrayList<>();
            List<String> scalarFacts = new ArrayList<>();
            if (event.getClass().isRecord()) {
                for (RecordComponent component : event.getClass().getRecordComponents()) {
                    Object value = component.getAccessor().invoke(event);
                    collectEventFacts(component.getName(), value, playerIds, cardIds,
                            spellAbilityIds, stackItemIds, zones, scalarFacts);
                }
            }
            String targetDescription = event instanceof GameEventSpellAbilityCast cast
                    ? cast.targetDescription() : null;
            String stackDescription = event instanceof GameEventSpellResolved resolved
                    ? resolved.stackDescription() : null;
            return new EventFact(event.getClass().getName(), playerIds, cardIds,
                    spellAbilityIds, stackItemIds, zones, scalarFacts, targetDescription, stackDescription);
        }

        private void collectEventFacts(String field, Object value, List<Integer> playerIds,
                                       List<Integer> cardIds, List<Integer> spellAbilityIds,
                                       List<Integer> stackItemIds, List<ZoneFact> zones,
                                       List<String> scalarFacts) {
            if (value == null) return;
            if (value instanceof ZoneView zone) {
                zones.add(new ZoneFact(field, zone.player() == null ? null : zone.player().getId(), zone.zoneType()));
            } else if (value instanceof SpellAbilityView ability) {
                spellAbilityIds.add(ability.getId());
                scalarFacts.add(field + ".id=" + ability.getId());
                if (ability.getHostCard() != null) cardIds.add(ability.getHostCard().getId());
                if (ability.getHostCard() != null) scalarFacts.add(field + ".hostCardId=" + ability.getHostCard().getId());
            } else if (value instanceof StackItemView item) {
                stackItemIds.add(item.getId());
                scalarFacts.add(field + ".id=" + item.getId());
                if (item.getSourceCard() != null) cardIds.add(item.getSourceCard().getId());
                if (item.getSourceCard() != null) scalarFacts.add(field + ".sourceCardId=" + item.getSourceCard().getId());
            } else if (value instanceof PlayerView player) {
                playerIds.add(player.getId());
            } else if (value instanceof CardView card) {
                cardIds.add(card.getId());
            } else if (value instanceof IHasCardView hasCard && hasCard.getCardView() != null) {
                cardIds.add(hasCard.getCardView().getId());
            } else if (value instanceof Enum<?> item) {
                scalarFacts.add(field + "=" + item.name());
            } else if (value instanceof Boolean || value instanceof Number) {
                scalarFacts.add(field + "=" + value);
            }
        }

        /** One-argument callback is the host's lifecycle path and the delegated remote callback. */
        @Override public void setGameView(GameView view) {

            if (!remote && seat == 0 && view != null) {
                scenario.retainOpeningKeepEngineGame(view.getGame());
                scenario.retainMetamorphoseOpeningEngineGame(view.getGame());
            }
            Capture received = null;
            GameView beforeApply = getGameView();
            if (view != null) {
                received = capture("setGameView", callbackSequence.get(), view);
            }
            super.setGameView(view);
            observeHostPrivacyLog(getGameView());
            if (view != null) {
                fullState.countDown();
                if (openView.getCount() == 0) postOpenFullState.countDown();
                if (!remote && received != null) recordAppliedView("setGameView", received, view.getId(), getGameView());
                if (remote && received != null) {
                    try {
                        GameView applied = getGameView();
                        recordAppliedView("setGameView", received, view.getId(), applied);
                        observeLondonAppliedHand(received, applied);
                        observePendingOpeningKeep(received, applied);
                        recordPrivacyState("setGameView", received, applied, view.getId(),
                                beforeApply == null ? -1 : beforeApply.getId());
                        resumePrivacyManaPaymentAfterAppliedState(applied);
                        if (scenario.observeAppliedState(applied, received)) {
                            dealtReceipt.compareAndSet(null, new CallbackReceipt(received.sequence(), received.callbackOrdinal()));
                            dealtState.countDown();
                        }
                        scenario.observeMetamorphoseState(applied, received, seat);
                        scenario.observePostKeepState(received, () -> snapshotZones(new Object[] {applied}), seat);
                    } catch (Throwable error) {
                        scenario.fail(error);
                    }
                }
                observePrivacyLog(received);
            }
        }

        /** IGuiGame's two-argument default delegates to the one-argument method. */
        @Override public void setGameView(GameView view, long sequence) {
            callbackSequence.set(sequence);
            try { super.setGameView(view, sequence); }
            finally { callbackSequence.remove(); }
        }

        @Override public void openView(TrackableCollection<PlayerView> players) {
            capture("openView", -1, players);
            if (players == null || players.size() != 1) {
                fail("Expected exactly one assigned PlayerView in openView for seat " + seat);
            } else {
                PlayerView assigned = players.iterator().next();
                ownPlayerViewId = assigned.getId();
                if (!expectedPlayerName.equals(assigned.getName())) {
                    fail("openView assigned " + assigned.getName() + " to slot " + seat + ", expected " + expectedPlayerName);
                }
                scenario.registerView(seat, assigned.getId());
            }
            super.openView(players);
            openView.countDown();
        }

        @Override public void applyDelta(DeltaPacket packet) {
            Capture captured = capture("applyDelta", packet.getSequenceNumber(), packet);
            GameView beforeApply = getGameView();
            if (captured != null && captured.stateBearing()) {
                populatedDeltaCount.incrementAndGet();
            }
            Capture previous = applyingPacket.get();
            applyingPacket.set(captured);
            try { super.applyDelta(packet); }
            catch (RuntimeException | Error failure) {
                try { scenario.fail(failure); }
                catch (Throwable latchFailure) {
                    if (latchFailure != failure) failure.addSuppressed(latchFailure);
                }
                throw failure;
            }
            finally {
                if (previous == null) applyingPacket.remove();
                else applyingPacket.set(previous);
            }
            if (!remote) {
                observePrivacyLog(captured);
                return;
            }
            if (captured != null) {
                try {
                    GameView applied = getGameView();
                    recordAppliedView("applyDelta", captured, -1, applied);
                    observeLondonAppliedHand(captured, applied);
                    observePendingOpeningKeep(captured, applied);
                    recordPrivacyState("applyDelta", captured, applied,
                            -1,
                            beforeApply == null ? -1 : beforeApply.getId());
                    resumePrivacyManaPaymentAfterAppliedState(applied);
                    if (scenario.observeAppliedState(applied, captured)) {
                        dealtReceipt.compareAndSet(null, new CallbackReceipt(captured.sequence(), captured.callbackOrdinal()));
                        dealtState.countDown();
                        if (captured.stateBearing()) populatedDelta.countDown();
                    }
                    scenario.observeMetamorphoseState(applied, captured, seat);
                    scenario.observePostKeepState(captured, () -> snapshotZones(new Object[] {applied}), seat);
                } catch (Throwable error) {
                    scenario.fail(error);
                }
            }
            observePrivacyLog(captured);
        }

        @Override public void handleGameEvents(List<GameEvent> events) {
            Capture captured = capture("handleGameEvents", -1, events);
            super.handleGameEvents(events);
            observePrivacyLog(captured);
            if (remote && events != null && !events.isEmpty()) nonemptyEvents.countDown();
        }

        @Override public void finishGame() {
            Capture terminalCapture = capture("finishGame", -1);
            GameView terminalView = getGameView();
            if (terminalView != null) terminalViewReceipt = new TerminalViewReceipt(terminalView.getId(),
                    terminalView.isGameOver(),
                    expectedPlayerName.equals(terminalView.getWinningPlayerName()), terminalView.getWinningTeam(),
                    terminalView.getNumPlayedGamesInMatch());
            super.finishGame();
            detachPrivacyLogObservers();

            scenario.markTerminated(terminalView == null ? -1 : terminalView.getId());
            terminal.countDown();
            terminalOrFailure.countDown();
        }

        @Override public void afterGameEnd() {
            Capture receipt = capture("afterGameEnd", -1);
            if (receipt != null) afterGameEndOrdinal.set(receipt.callbackOrdinal());
            GameView endedView = getGameView();
            scenario.completeMetamorphoseTerminalPresentation(seat, endedView == null ? -1 : endedView.getId());
            try { super.afterGameEnd(); }
            finally { afterGameEnd.countDown(); }
        }

        @Override public void alertUser() {
            Capture receipt = capture("alertUser", -1);
            long ended = afterGameEndOrdinal.get();
            if (expectingAlertUserFence && ended > 0 && receipt != null && receipt.callbackOrdinal() > ended) alertUserReceived.countDown();
        }

        @Override public void awaitNextInput() {
            capture("awaitNextInput", -1);
            super.awaitNextInput();
        }

        private PrivacyInputSnapshot snapshotInput(PlayerView owner) {
            IGameController assigned = owner == null ? null : getGameController(owner);
            if (!(assigned instanceof PlayerControllerHuman humanController)) return null;
            return new PrivacyInputSnapshot(humanController.getInputProxy().getInput(),
                    humanController.getInputQueue().getInput());
        }

        private static int gameViewId(GameView view) {
            return view == null ? -1 : view.getId();
        }

        private static int controllerGameViewId(IGameController controller) {
            if (!(controller instanceof PlayerControllerHuman humanController) || humanController.getGame() == null) return -1;
            return humanController.getGame().getView().getId();
        }

        private String metamorphoseTargetRepaintDiagnostic(PlayerView owner, Capture callback,
                boolean enabledOne, boolean enabledTwo) {
            PrivacyInputSnapshot currentInput = snapshotInput(owner);
            PrivacyInputSnapshot acceptedInput = acceptedMetamorphoseTargetInput;
            IGameController assigned = owner == null ? null : getGameController(owner);
            int currentGameViewId = gameViewId(getGameView());
            int currentControllerViewId = controllerGameViewId(assigned);
            SelectablesReceipt target = scenario.selectablesReceipts().stream()
                    .filter(receipt -> receipt.stage().equals("Metamorphose target")).findFirst().orElse(null);
            MetamorphoseInputButtonsReceipt acceptedButtons = scenario.metamorphoseInputButtons().stream()
                    .filter(receipt -> receipt.prompt().equals("target")).findFirst().orElse(null);
            return " metamorphoseTargetRepaint{stage=" + scenario.metamorphoseStage()
                    + ",seat=" + seat + ",ownerViewId=" + (owner == null ? -1 : owner.getId())
                    + ",acceptedGameViewId=" + acceptedMetamorphoseTargetGameViewId
                    + ",currentGameViewId=" + currentGameViewId
                    + ",acceptedControllerViewId=" + acceptedMetamorphoseTargetControllerViewId
                    + ",currentControllerViewId=" + currentControllerViewId
                    + ",sameGameView=" + (acceptedMetamorphoseTargetGameViewId == currentGameViewId)
                    + ",sameControllerGameView=" + (acceptedMetamorphoseTargetControllerViewId == currentControllerViewId)
                    + ",sourceCardId=" + scenario.metamorphoseHostCardId()
                    + ",targetCardId=" + scenario.metamorphoseTargetCardId()
                    + ",offersTarget=" + (target != null && target.cardIds().contains(scenario.metamorphoseTargetCardId()))
                    + ",offeredCount=" + (target == null ? -1 : target.cardIds().size())
                    + ",min=" + (target == null ? -1 : target.minimum())
                    + ",max=" + (target == null ? -1 : target.maximum())
                    + ",selectablesOrdinal=" + (target == null ? -1 : target.callbackOrdinal())
                    + ",targetPromptOrdinal=" + (acceptedButtons == null ? -1 : acceptedButtons.promptOrdinal())
                    + ",acceptedButtonsOrdinal=" + (acceptedButtons == null ? -1 : acceptedButtons.buttonsOrdinal())
                    + ",acceptedEnabledOne=" + (acceptedButtons != null && acceptedButtons.okEnabled())
                    + ",acceptedEnabledTwo=" + (acceptedButtons != null && acceptedButtons.cancelEnabled())
                    + ",repaintButtonsOrdinal=" + (callback == null ? -1 : callback.callbackOrdinal())
                    + ",repaintEnabledOne=" + enabledOne + ",repaintEnabledTwo=" + enabledTwo
                    + ",sameProxyInput=" + (acceptedInput != null && currentInput != null
                            && acceptedInput.proxyInput() == currentInput.proxyInput())
                    + ",sameQueuedInput=" + (acceptedInput != null && currentInput != null
                            && acceptedInput.queuedInput() == currentInput.queuedInput())
                    + ",acceptedInput=" + (acceptedInput == null ? "none" : acceptedInput.evidence())
                    + ",currentInput=" + (currentInput == null ? "none" : currentInput.evidence())
                    + ",actionQueued=" + metamorphoseTargetActionsQueued.get()
                    + ",actionDispatched=" + metamorphoseTargetActionsDispatched.get()
                    + ",actionAccepted=" + metamorphoseTargetActionAccepted + "}";
        }

        private void fail(String message) { scenario.fail(new AssertionError(message)); }
        private void wakeFailureWaiters() {
            fullState.countDown(); openView.countDown(); dealtState.countDown(); populatedDelta.countDown();
            nonemptyEvents.countDown(); alertUserReceived.countDown(); terminalOrFailure.countDown();
        }

        private void queueAction(Runnable action) {
            FThreads.invokeInEdtLater(() -> {
                if (scenario.stopped()) return;
                try { action.run(); }
                catch (Throwable t) { scenario.fail(t); }
            });
        }

        /** Joins a real enabled payment-buttons callback to the later applied tap receipt. */
        private synchronized void resumePrivacyManaPaymentAfterAppliedState(GameView applied) {
            if (!remote || !scenario.privacyScenario() || scenario.stopped() || pendingPrivacyManaPromptOrdinal <= 0 || pendingPrivacyManaCardId <= 0) return;
            CardView paidSource = findVisibleCard(applied, pendingPrivacyManaCardId);
            if (paidSource == null || !paidSource.isTapped()) return;
            int ownerId = pendingPrivacyManaPromptOwnerId;
            IGameController controller = pendingPrivacyManaPromptController;
            lastPrivacyManaContinuationOrdinal = pendingPrivacyManaPromptOrdinal;
            pendingPrivacyManaPromptOrdinal = -1;
            pendingPrivacyManaPromptOwnerId = -1;
            pendingPrivacyManaPromptController = null;
            pendingPrivacyManaCardId = -1;
            dispatchPrivacyManaSource(ownerId, controller);
        }

        private void dispatchPrivacyManaSource(int ownerId, IGameController controller) {
            if (scenario.privacyExpectedManaCount() == 0) return;
            if (controller == null) { fail("Privacy payment prompt had no assigned controller"); return; }
            int cardId = scenario.nextPrivacyManaSource(seat, ownerId);
            CardView source = findVisibleCard(getGameView(), cardId);
            if (source == null || source.isTapped()) {
                fail("Assigned privacy hand did not expose the next untapped scripted Island source " + cardId);
                return;
            }
            pendingPrivacyManaCardId = cardId;
            queueAction(() -> {
                if (!controller.selectCard(source, null, null)) fail("Controller rejected privacy Island payment " + cardId);
            });
        }

        private AssertionError unexpected(String method, Object... args) {
            capture(method, -1, args);
            AssertionError error = new AssertionError("Unexpected human decision prompt " + method + " for seat " + seat);
            scenario.fail(error);
            return error;
        }

        private PreHandlerFact claimPrivacyFact(String method, Capture callback) {
            ReconnectObservation observation = scenario.privacyObservation();
            PreHandlerFact fact = observation == null ? null : observation.claimNext(method);
            if (callback == null || callback.callbackOrdinal() <= 0 || fact == null || !method.equals(fact.method()) || fact.eventId() < 0) {
                throw unexpected("missing correlated pre-handler " + method + " event", callback, fact);
            }
            return fact;
        }

        private String privacyRevealDiagnostic(CardView argument, long callbackOrdinal,
                PreHandlerFact wire, int cardId) {
            GameView currentView = getGameView();
            CardView canonical = currentView == null || currentView.getTracker() == null ? null
                    : currentView.getTracker().getObj(TrackableTypes.CardViewType, cardId);
            List<PlayerView> assignedViews = new ArrayList<>();
            PlayerView hostView = findPlayerView(currentView, scenario.privacyHostViewId());
            PlayerView remoteView = findPlayerView(currentView, scenario.privacyRemoteViewId());
            if (hostView != null) assignedViews.add(hostView);
            if (remoteView != null) assignedViews.add(remoteView);

            ReconnectObservation observation = scenario.privacyObservation();
            List<PreHandlerFact> facts = observation == null ? List.of() : observation.facts();
            int wireFactIndex = -1;
            for (int i = 0; i < facts.size(); i++) {
                if (facts.get(i) == wire) {
                    wireFactIndex = i;
                    break;
                }
            }
            PreHandlerFact precedingDelta = null;
            int precedingDeltaIndex = -1;
            for (int i = 0; i < wireFactIndex; i++) {
                PreHandlerFact fact = facts.get(i);
                if ("applyDelta".equals(fact.method()) && fact.newCardViewIds().contains(cardId)) {
                    precedingDelta = fact;
                    precedingDeltaIndex = i;
                }
            }
            long precedingDeltaSequence = precedingDelta == null ? -1 : precedingDelta.sequence();
            Capture appliedDelta = precedingDelta == null ? null : captures().stream()
                    .filter(capture -> "applyDelta".equals(capture.callback())
                            && capture.sequence() == precedingDeltaSequence)
                    .max(java.util.Comparator.comparingLong(Capture::callbackOrdinal)).orElse(null);

            Game authoritativeGame = scenario.privacyGame();
            Card authoritativeCard = authoritativeGame == null ? null : authoritativeGame.findById(cardId);
            Player authoritativeRemote = authoritativeGame == null || authoritativeGame.getPlayers().size() < 2
                    ? null : authoritativeGame.getPlayers().get(1);
            CardView authoritativeView = authoritativeCard == null ? null : CardView.get(authoritativeCard);
            String authoritative = authoritativeCard == null ? "null" : "cardId=" + cardId
                    + ",zone=" + (authoritativeCard.getZone() == null ? "null" : authoritativeCard.getZone().getZoneType())
                    + ",owner=" + playerViewId(authoritativeCard.getOwner())
                    + ",controller=" + playerViewId(authoritativeCard.getController())
                    + ",mayLookRemote=" + (authoritativeRemote != null && authoritativeCard.mayPlayerLook(authoritativeRemote))
                    + ",view=" + cardViewEvidence(authoritativeView, assignedViews);

            return "callbackOrdinal=" + callbackOrdinal + ", wireEvent=" + (wire == null ? -1 : wire.eventId())
                    + ", wireSequence=" + (wire == null ? -1 : wire.sequence()) + ", argument=" + cardViewEvidence(argument, assignedViews)
                    + ", canonical=" + cardViewEvidence(canonical, assignedViews)
                    + ", argumentIsCanonical=" + (argument == canonical)
                    + ", assignedViewIds=" + assignedViews.stream().map(PlayerView::getId).toList()
                    + ", wireFactReceiptIndex=" + wireFactIndex
                    + ", hostAuthoritative={" + authoritative + "}, precedingFNewCardDelta="
                    + (precedingDelta == null ? "none" : "receiptIndex=" + precedingDeltaIndex + ",event="
                            + precedingDelta.eventId() + ",seq="
                            + precedingDelta.sequence() + ",newCardViews=" + precedingDelta.newCardViewIds()
                            + ",appliedCallbackOrdinal="
                            + (appliedDelta == null ? "missing" : appliedDelta.callbackOrdinal()));
        }

        private static int playerViewId(Player player) {
            return player == null || player.getView() == null ? -1 : player.getView().getId();
        }

        private static String cardViewEvidence(CardView card, List<PlayerView> assignedViews) {
            if (card == null) return "null";
            List<Integer> mayLookIds = new ArrayList<>();
            Map<TrackableProperty, Object> properties = card.getProps();
            Object mayLook = properties.get(TrackableProperty.PlayerMayLook);
            if (mayLook instanceof Iterable<?> viewers) for (Object viewer : viewers) {
                if (viewer instanceof PlayerView playerView) mayLookIds.add(playerView.getId());
            }
            List<Boolean> mayLookBySeat = assignedViews.stream().map(card::mayPlayerLook).toList();
            List<Boolean> canShowBySeat = assignedViews.stream().map(card::canBeShownTo).toList();
            return "id=" + card.getId() + ",identity=" + Integer.toHexString(System.identityHashCode(card))
                    + ",zone=" + card.getZone() + ",owner=" + (card.getOwner() == null ? -1 : card.getOwner().getId())
                    + ",controller=" + (card.getController() == null ? -1 : card.getController().getId())
                    + ",mayLookIds=" + mayLookIds + ",mayLookByAssignedSeats=" + mayLookBySeat
                    + ",canShowByAssignedSeats=" + canShowBySeat;
        }

        @Override public SpellAbilityView getAbilityToPlay(CardView card, List<SpellAbilityView> abilities, ITriggerEvent trigger) {
            Capture abilityReceipt;

            if (scenario.privacyScenario()) {
                List<String> facts = abilities == null ? List.of() : abilities.stream()
                        .map(ability -> ability.getId() + ":" + ability.canPlay() + ":" + ability.isSpell()
                                + ":" + (ability.getHostCard() == null ? -1 : ability.getHostCard().getId())
                                + ":" + ability.getDescription()).toList();
                int cardId = card == null ? -1 : card.getId();
                abilityReceipt = capture("getAbilityToPlay", -1, cardId, facts);
                PreHandlerFact wire = seat == 1 ? claimPrivacyFact("getAbilityToPlay", abilityReceipt) : null;
                SpellAbilityView chosen;
                String role;
                if (false && seat == 0
                        && cardId == scenario.privacyDaysUndoingId()) {
                    chosen = abilities != null && abilities.size() == 1 && abilities.get(0).canPlay()
                            && abilities.get(0).isSpell() && abilities.get(0).getHostCard() != null
                            && abilities.get(0).getHostCard().getId() == cardId ? abilities.get(0) : null;
                    role = "Day's Undoing actual host spell choice";
                } else if (seat == scenario.privacyTellingTimeSeat() && cardId == scenario.privacyTellingTimeId()) {
                    chosen = abilities != null && abilities.size() == 1 && abilities.get(0).canPlay()
                            && abilities.get(0).isSpell() && abilities.get(0).getHostCard() != null
                            && abilities.get(0).getHostCard().getId() == cardId ? abilities.get(0) : null;
                    role = scenario.privacyTellingTimeAbilityChoices() == 0
                            ? "Telling Time initial spell choice" : "Telling Time additional-cost choice";
                } else if (seat == scenario.privacyFengrafSeat() && cardId == scenario.privacyFengrafId()) {
                    int choiceNumber = scenario.privacyFengrafAbilityChoices();
                    if (abilities == null || choiceNumber == 0 && abilities.size() != 2 || choiceNumber == 1 && abilities.size() != 1 || abilities.stream().anyMatch(ability -> !ability.canPlay()
                                    || ability.getHostCard() == null || ability.getHostCard().getId() != cardId) || abilities.stream().filter(ability -> ability.getId()
                                    == scenario.privacyFengrafAbilityId()).count() != 1 || choiceNumber == 1 && abilities.get(0).getId() != scenario.privacyFengrafAbilityId()) {
                        throw unexpected("invalid Fengraf ability candidate role", choiceNumber, facts);
                    }
                    chosen = abilities == null ? null : abilities.stream()
                            .filter(ability -> ability.getId() == scenario.privacyFengrafAbilityId()
                                    && ability.canPlay() && !ability.isSpell()
                                    && ability.getHostCard() != null && ability.getHostCard().getId() == cardId)
                            .findFirst().orElse(null);
                    role = scenario.privacyFengrafAbilityChoices() == 0
                            ? "Haunted Fengraf activation ability" : "Haunted Fengraf additional-cost choice";
                } else if ((seat == scenario.privacyTellingTimeSeat() && scenario.privacyRemoteManaIds().contains(cardId)) || (seat == scenario.privacyFengrafSeat() && scenario.privacyHostManaIds().contains(cardId))) {
                    chosen = abilities != null && abilities.size() == 1 && abilities.get(0).canPlay()
                            && !abilities.get(0).isSpell() && abilities.get(0).getHostCard() != null
                            && abilities.get(0).getHostCard().getId() == cardId ? abilities.get(0) : null;
                    role = "selected Island mana source";
                } else {
                    throw unexpected("unassigned privacy getAbilityToPlay", cardId, facts);
                }
                if (chosen == null || abilityReceipt == null) {
                    throw unexpected("invalid privacy ability candidates", role, cardId, facts);
                }
                scenario.recordPrivacyAbility(new PrivacyAbilityReceipt(role, seat, ownPlayerViewId, cardId,
                        chosen.getId(),
                        abilityReceipt.callbackOrdinal(), wire == null ? -1 : wire.eventId(),
                        wire == null ? -1 : wire.sequence(), facts));
                return chosen;
            }
            if (scenario.metamorphoseScenario()) {
                List<String> abilityFacts = abilities == null ? List.of() : abilities.stream()
                        .map(ability -> ability.getId() + ":" + ability.canPlay() + ":" + ability.isSpell()
                                + ":" + (ability.getHostCard() == null ? -1 : ability.getHostCard().getId())
                                + ":" + ability.getDescription()).toList();
                abilityReceipt = capture("getAbilityToPlay", -1,
                        card == null ? -1 : card.getId(), abilityFacts);
                if (card != null && card.getId() == scenario.metamorphoseHostCardId()) {
                    if (seat != 0 || abilities == null || abilities.size() != 1 || !abilities.get(0).canPlay() || !abilities.get(0).isSpell() || abilities.get(0).getHostCard() == null || abilities.get(0).getHostCard().getId() != card.getId()) {
                        throw unexpected("invalid Metamorphose cast ability", card == null ? -1 : card.getId(), abilityFacts);
                    }
                    scenario.recordMetamorphoseCast(seat, ownPlayerViewId, card.getId(),
                            abilityReceipt == null ? -1 : abilityReceipt.callbackOrdinal(), abilityFacts);
                    return abilities.get(0);
                }
                if (card != null && card.getId() == pendingMetamorphoseManaCardId) {
                    if (seat != 0 || abilities == null || abilities.size() != 1 || !abilities.get(0).canPlay() || abilities.get(0).isSpell() || abilities.get(0).getHostCard() == null || abilities.get(0).getHostCard().getId() != card.getId()) {
                        throw unexpected("invalid Metamorphose Island mana ability", card.getId(), abilityFacts);
                    }
                    scenario.recordMetamorphoseManaAbility(seat, ownPlayerViewId, card.getId(),
                            abilityReceipt == null ? -1 : abilityReceipt.callbackOrdinal(), abilityFacts);
                    return abilities.get(0);
                }
            } else {
                abilityReceipt = capture("getAbilityToPlay", -1, card, abilities, trigger);
            }
            final int selectedCardId = pendingLandCardId;
            ExpectedAction queued = script.peekFirst();
            if (ownPlayerViewId < 0 || pendingLandCardId <= 0 || card.getId() != pendingLandCardId || card.getCurrentState().getType() == null || !card.getCurrentState().getType().isLand() || abilities == null || abilities.size() != 1 || abilities.get(0).getHostCard() == null || abilities.get(0).getHostCard().getId() != pendingLandCardId || !abilities.get(0).canPlay() || queued == null || queued.decision() != Decision.PLAY_LAND || queued.cardId() != pendingLandCardId) {
                throw unexpected("unmatched getAbilityToPlay", card, abilities);
            }
            consume(queued);
            scenario.recordAction(new ActionReceipt(Decision.PLAY_LAND, seat, ownPlayerViewId, selectedCardId, true));
            pendingLandCardId = -1;
            scenario.markLandAbility(seat);
            return abilities.get(0);
        }

        @Override public void setSelectables(Iterable<CardView> cards, int min, int max) {
            if (!scenario.metamorphoseScenario()) {
                super.setSelectables(cards, min, max);
                return;
            }
            List<Integer> ids = new ArrayList<>();
            if (cards != null) for (CardView card : cards) ids.add(card.getId());
            String stage = "unexpected";
            List<Integer> ownPermanentHandIds = List.of();
            if (seat == 1 && scenario.metamorphoseStage() == 3 && scenario.metamorphoseLifecycleConcessionPending && scenario.metamorphoseAssignedConcessionGameViewId() >= 0) {
                GameView current = getGameView();
                if (current == null || current.getId() != scenario.metamorphoseAssignedConcessionGameViewId() || !current.isGameOver()) {
                    fail("Buffered Meta selector arrived before its actual conceded game's terminal view: seat="
                            + seat + ",stage=" + scenario.metamorphoseStage() + ",viewId="
                            + (current == null ? -1 : current.getId()) + ",gameOver="
                            + (current != null && current.isGameOver()) + ",concessionViewId="
                            + scenario.metamorphoseAssignedConcessionGameViewId());
                    super.setSelectables(cards, min, max);
                    return;
                }
                Capture receipt = capture("setSelectables", callbackSequence.get(),
                        "bufferedMetaSelectorAfterConcession", current.getId(), List.copyOf(ids), min, max,
                        current.isGameOver());
                List<Integer> eligibleOwnHandIds = new ArrayList<>();
                PlayerView ownView = findPlayerView(current, ownPlayerViewId);
                if (ownView != null && ownView.getHand() != null) {
                    for (CardView card : ownView.getHand()) {
                        var type = card.getCurrentState().getType();
                        if (type != null && (type.isArtifact() || type.isCreature()
                                || type.isEnchantment() || type.isLand())) {
                            eligibleOwnHandIds.add(card.getId());
                        }
                    }
                }
                if (receipt == null || !scenario.recordBufferedMetaSelectable(seat, current,
                        receipt.callbackOrdinal(), min, max, ids, eligibleOwnHandIds)) {
                    fail("Could not capture the terminal old-game Meta selector receipt");
                }
                super.setSelectables(cards, min, max);
                return;
            }
            if (scenario.metamorphoseScenario()) {
                if (seat == 0 && scenario.metamorphoseStage() == 1) {
                    stage = "Metamorphose target";
                    pendingMetamorphoseTargetIds = List.copyOf(ids);
                } else if (seat == 1 && scenario.metamorphoseAccept() && scenario.metamorphoseStage() == 4) {
                    stage = "Metamorphose private hand";
                    List<Integer> permanentHand = new ArrayList<>();
                    GameView view = getGameView();
                    if (view != null) for (PlayerView player : view.getPlayers()) {
                        if (player.getId() != ownPlayerViewId || player.getHand() == null) continue;
                        for (CardView card : player.getHand()) {
                            var type = card.getCurrentState().getType();
                            if (type != null && (type.isArtifact() || type.isCreature()
                                    || type.isEnchantment() || type.isLand())) {
                                permanentHand.add(card.getId());
                            }
                        }
                    }
                    ownPermanentHandIds = List.copyOf(permanentHand);
                    pendingMetamorphoseHandIds = List.copyOf(ids);
                } else if (scenario.metamorphoseStage() > 0 && scenario.metamorphoseStage() < 7) {
                    fail("Unexpected selectable-card list during Metamorphose stage " + scenario.metamorphoseStage()
                            + " for seat " + seat + ": " + ids);
                }
            }
            Capture receipt = capture("setSelectables", -1, stage, List.copyOf(ids), min, max);
            if ("Metamorphose target".equals(stage) || "Metamorphose private hand".equals(stage)) {
                long ordinal = receipt == null ? -1 : receipt.callbackOrdinal();
                if ("Metamorphose target".equals(stage)) {
                    scenario.recordMetamorphoseTarget(seat, ownPlayerViewId, ordinal, min, max, ids);
                } else {
                    scenario.recordMetamorphoseHandChoices(seat, ownPlayerViewId,
                            ordinal, min, max, ids, ownPermanentHandIds);
                }
            }
            super.setSelectables(cards, min, max);
        }

        @Override public <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected,
                                                 FSerializableFunction<T, String> display) {
            boolean compositeFreeRevealPending = scenario.privacyMulliganPrefixScenario()
                    && scenario.freeMulliganChoice() != null && scenario.freeReveals().isEmpty();
            if (compositeFreeRevealPending) {
                MulliganChoiceReceipt freeChoice = scenario.freeMulliganChoice();
                MulliganChoiceReceipt regularChoice = scenario.regularMulliganChoice();
                List<Integer> ids = new ArrayList<>();
                if (choices != null) for (T choice : choices) {
                    if (!(choice instanceof CardView card)) {
                        throw unexpected("non-card composite free-mulligan reveal", message, choice);
                    }
                    ids.add(card.getId());
                }
                Capture receipt = capture("getChoices", -1, message, min, max, List.copyOf(ids));
                PreHandlerFact wire = seat == 1 ? claimPrivacyFact("getChoices", receipt) : null;
                GameView currentView = getGameView();
                Set<Integer> actualIds = new HashSet<>(ids);
                Set<Integer> expectedIds = new HashSet<>(freeChoice.handIds());
                if (freeChoice.seat() != 0 || regularChoice == null || regularChoice.seat() != 1 || regularChoice.receiptOrder() <= freeChoice.receiptOrder() || seat != 1 - freeChoice.seat() || ownPlayerViewId != regularChoice.chooserViewId() || receipt == null || currentView == null || currentView.getId() != freeChoice.gameViewId() || currentView.getId() != regularChoice.gameViewId() || wire == null || wire.eventId() < 0) {
                    throw unexpected("invalid actual composite free-mulligan reveal stage", seat, ownPlayerViewId,
                            min, max, ids, message, currentView == null ? -1 : currentView.getId(),
                            freeChoice.gameViewId(), regularChoice, wire);
                }
                String expectedMessage = expectedFreeRevealMessage(message);
                if (min != -1 || max != -1 || choices == null || ids.size() != 7 || actualIds.size() != 7 || !actualIds.equals(expectedIds) || !message.equals(expectedMessage)) {
                    throw unexpected("invalid actual composite free-mulligan reveal stage", seat, ownPlayerViewId,
                            min, max, ids, message, currentView == null ? -1 : currentView.getId(),
                            freeChoice.gameViewId(), regularChoice, wire);
                }
                scenario.recordFreeReveal(seat, currentView.getId(), wire.eventId(), message,
                        receipt.callbackOrdinal(), ids,
                        expectedMessage);
                return super.getChoices(message, min, max, choices, selected, display);
            }
            if (scenario.privacyScenario()) {
                List<Integer> ids = new ArrayList<>();
                if (choices != null) for (T choice : choices) {
                    if (!(choice instanceof CardView card)) throw unexpected("non-card Fengraf getChoices", message, choice);
                    ids.add(card.getId());
                }
                Capture receipt = capture("getChoices", -1, message, min, max, List.copyOf(ids));
                PreHandlerFact wire = seat == 1 ? claimPrivacyFact("getChoices", receipt) : null;
                PlayerView chooser = findPlayerView(getGameView(), ownPlayerViewId);
                List<Boolean> mayLook = new ArrayList<>();
                List<Boolean> canShow = new ArrayList<>();
                if (choices != null) for (T choice : choices) {
                    CardView card = (CardView) choice;
                    mayLook.add(card.mayPlayerLook(chooser));
                    canShow.add(card.canBeShownTo(chooser));
                }
                int expectedChooserSeat = 1 - scenario.privacyFengrafSeat();
                if (seat != expectedChooserSeat || ownPlayerViewId != scenario.privacyViewIdForSeat(expectedChooserSeat) || min != -1 || max != -1 || !ids.equals(List.of(scenario.privacyPublicCreatureId())) || !scenario.privacyExpectedRevealMessage().equals(message) || chooser == null) {
                    throw unexpected("unexpected Fengraf public getChoices", message, min, max, ids);
                }
                String diagnostic = privacyRevealDiagnostic((CardView) choices.get(0),
                        receipt.callbackOrdinal(), wire, ids.get(0));
                System.out.println("FENGRAF_REVEAL_DIAGNOSTIC " + diagnostic);
                scenario.recordPrivacyReveal(new PrivacyRevealReceipt(seat, ownPlayerViewId,
                        receipt.callbackOrdinal(), wire == null ? -1 : wire.eventId(), wire == null ? -1 : wire.sequence(), message, min, max, ids,
                        mayLook, canShow, diagnostic));
                return super.getChoices(message, min, max, choices, selected, display);
            }
            if (scenario.mulliganScenario() && min == -1 && max == -1) {
                List<Integer> ids = new ArrayList<>();
                if (choices != null) for (T choice : choices) {
                    if (!(choice instanceof CardView card)) {
                        throw unexpected("non-card free-mulligan reveal", message, choice);
                    }
                    ids.add(card.getId());
                }
                Capture presentation = capture("getChoices", -1, message, min, max, List.copyOf(ids));
                if (presentation == null) {
                    fail("Could not retain the actual remote FREE hand getChoices callback");
                    return List.of();
                }
                GameView currentView = getGameView();
                scenario.recordFreeReveal(seat, currentView == null ? -1 : currentView.getId(), -1,
                        message, presentation.callbackOrdinal(), ids,
                        expectedFreeRevealMessage(message));
                return super.getChoices(message, min, max, choices, selected, display);
            }
            throw unexpected("getChoices", message, min, max, choices, selected);
        }
        @Override public boolean confirm(CardView card, String question, boolean defaultYes, List<String> options) {
            throw unexpected("confirm", card, question, defaultYes, options);
        }
        @Override public boolean showConfirmDialog(String message, String title, String yes, String no, boolean defaultYes) {
            throw unexpected("showConfirmDialog", message, title, yes, no, defaultYes);
        }
        @Override public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) {
            Localizer localizer = Localizer.getInstance();
            if (ownPlayerViewId < 0 || expectedPlayerName == null || !localizer.getMessage("lblDanDanMulliganChoice", expectedPlayerName, 0).equals(message)) {
                throw unexpected("showOptionDialog chooser", message, title, icon, options, defaultOption);
            }
            List<String> expectedOptions = List.of(localizer.getMessage("lblKeep"),
                    localizer.getMessage("lblDanDanFreeMulligan"), localizer.getMessage("lblDanDanRegularMulligan"));
            if (!localizer.getMessage("lblDanDanMulligan").equals(title) || !FSkinProp.ICO_QUESTION.equals(icon) || !expectedOptions.equals(options)) {
                throw unexpected("showOptionDialog options", message, title, icon, options, defaultOption);
            }
            ExpectedAction expected = script.peekFirst();
            if (expected != null && (expected.decision() == Decision.PLAY_OR_DRAW
                    || expected.decision() == Decision.START_SEAT_ZERO)) {
                consume(expected); // this seat was not chosen for the match's single Play/Draw question
                expected = script.peekFirst();
            }
            if (scenario.mulliganScenario() && expected != null && expected.decision() == Decision.MULLIGAN_ANY) {
                scenario.assertInitialShuffleConsumed();
                Decision choice = scenario.chooseInitialMulligan(seat);
                HandFacts hand = snapshotOwnHand();
                Capture receipt = capture("showOptionDialog", -1, message, title, icon, options, defaultOption);
                if (receipt == null) throw new AssertionError("Could not retain initial DanDan mulligan callback");
                GameView currentView = getGameView();
                scenario.recordMulliganChoice(new MulliganChoiceReceipt(choice, seat, ownPlayerViewId,
                        currentView == null ? -1 : currentView.getId(), -1,
                        "showOptionDialog", receipt.callbackOrdinal(), hand.ids(), hand.landCount()));
                consume(expected);
                scenario.recordAction(new ActionReceipt(choice, seat, ownPlayerViewId, null, true));
                String selected = choice == Decision.FREE_MULLIGAN
                        ? localizer.getMessage("lblDanDanFreeMulligan")
                        : localizer.getMessage("lblDanDanRegularMulligan");
                int choiceIndex = options.indexOf(selected);
                if (choiceIndex < 0) throw unexpected("missing scripted DanDan mulligan option", selected, options);
                return choiceIndex;
            }
            if (expected == null || expected.decision() != Decision.KEEP_WITHOUT_LAND || expected.seat() != seat) {
                throw unexpected("unscripted DanDan mulligan choice", message, title, icon, options, defaultOption);
            }
            int keepIndex = options.indexOf(localizer.getMessage("lblKeep"));
            if (keepIndex < 0) throw unexpected("missing explicit Keep choice", message, title, icon, options, defaultOption);
            Capture receipt = capture("showOptionDialog", -1, message, title, icon, options, defaultOption);
            if (receipt == null) throw new AssertionError("Could not retain DanDan mulligan choice callback for seat " + seat);
            if (expected.decision() == Decision.KEEP_WITHOUT_LAND) {
                GameView disconnectAiSourceView = getGameView();
                int handCount = false ? snapshotOwnHand().ids().size() : -1;

                if (scenario.metamorphoseScenario() || scenario.privacyScenario() || scenario.lifecycleScenario()) {
                    HandFacts hand = snapshotOwnHand();
                    GameView sourceView = getGameView();
                    scenario.recordOpeningKeep(seat, ownPlayerViewId, receipt.callbackOrdinal(),
                            sourceView == null ? -1 : sourceView.getId(), "showOptionDialog",
                            expected.decision().name(), hand.ids(), null);
                    if (scenario.lifecycleScenario()) {
                        recordLifecycleKeep(receipt, hand);
                    }
                }

            }
            consume(expected);
            scenario.recordAction(new ActionReceipt(Decision.KEEP_WITHOUT_LAND, seat, ownPlayerViewId, null, true));
            return keepIndex;
        }
        @Override public String showInputDialog(String message, String title, FSkinProp icon, String initial, List<String> options, boolean numeric) {
            throw unexpected("showInputDialog", message, title, icon, initial, options, numeric);
        }
        @Override public <T> IGuiGame.OrderResult<T> order(String title, String top, int min, int max, List<T> source,
                List<T> destination, CardView reference, boolean sideboarding, boolean showRemember) {
            throw unexpected("order", title, top, min, max, source, destination, reference, sideboarding, showRemember);
        }
        @Override public List<PaperCard> sideboard(CardPool sideboard, CardPool main, String message) {
            throw unexpected("sideboard", sideboard, main, message);
        }
        @Override public GameEntityView chooseSingleEntityForEffect(String title, List<? extends GameEntityView> options,
                DelayedReveal reveal, boolean optional) {
            throw unexpected("chooseSingleEntityForEffect", title, options, reveal, optional);
        }
        @Override public List<GameEntityView> chooseEntitiesForEffect(String title, List<? extends GameEntityView> options,
                int min, int max, DelayedReveal reveal) {
            if (scenario.privacyScenario()) {
                Capture receipt;
                List<CardView> cards = new ArrayList<>();
                if (options != null) for (GameEntityView option : options) {
                    if (!(option instanceof CardView card)) throw unexpected("non-card Telling Time option", title, option);
                    cards.add(card);
                }
                List<Integer> ids = cards.stream().map(CardView::getId).toList();
                receipt = capture("chooseEntitiesForEffect", -1, title, min, max, ids);
                PreHandlerFact wire = seat == 1 ? claimPrivacyFact("chooseEntitiesForEffect", receipt) : null;
                PlayerView chooser = findPlayerView(getGameView(), ownPlayerViewId);
                PlayerView other = findPlayerView(getGameView(), scenario.privacyViewIdForSeat(1 - scenario.privacyTellingTimeSeat()));
                if (seat != scenario.privacyTellingTimeSeat() || ownPlayerViewId != scenario.privacyViewIdForSeat(scenario.privacyTellingTimeSeat()) || chooser == null || other == null || min != 1 || max != 1 || ids.isEmpty()) {
                    throw unexpected("invalid Telling Time private choice", title, min, max, ids);
                }
                int index = scenario.privacyTellingTimeChoiceCount();
                List<Integer> expected = index == 0 ? scenario.privacyTopIds()
                        : index == 1 ? List.of(scenario.privacyTopIds().get(2), scenario.privacyTopIds().get(1))
                        : List.of();
                if (!ids.equals(expected)) {
                    capture("privacyTellingTimeChoiceCandidates", -1, title, min, max, ids, expected);
                    AssertionError error = new AssertionError("Telling Time offered card IDs " + ids
                            + " but the scripted role order was " + expected);
                    scenario.fail(error);
                    throw error;
                }
                CardView selected = cards.stream().filter(card -> card.getId() == scenario.privacyTopIds().get(index)).findFirst()
                        .orElseThrow(() -> unexpected("Telling Time scripted choice absent", ids));
                List<Boolean> chooserMayLook = cards.stream().map(card -> card.mayPlayerLook(chooser)).toList();
                List<Boolean> chooserCanShow = cards.stream().map(card -> card.canBeShownTo(chooser)).toList();
                List<Boolean> otherMayLook = cards.stream().map(card -> card.mayPlayerLook(other)).toList();
                List<Boolean> otherCanShow = cards.stream().map(card -> card.canBeShownTo(other)).toList();
                Map<Integer, List<Boolean>> hostFacts = scenario.privacyHostVisibility(ids);
                List<Boolean> hostMayLook = ids.stream().map(id -> hostFacts.get(id).get(0)).toList();
                List<Boolean> hostCanShow = ids.stream().map(id -> hostFacts.get(id).get(1)).toList();
                scenario.recordPrivacyChoice(new PrivacyChoiceReceipt(seat, ownPlayerViewId,
                        receipt.callbackOrdinal(), wire == null ? -1 : wire.eventId(), wire == null ? -1 : wire.sequence(), "chooseEntitiesForEffect",
                        title, min, max, ids, selected.getId(), chooserMayLook, chooserCanShow, otherMayLook, otherCanShow,
                        hostMayLook, hostCanShow));
                if (seat == 0 && index == 0 && scenario.privacyTellingTimeSeat() == 0) {
                    if (java.awt.EventQueue.isDispatchThread()) {
                        AssertionError error = new AssertionError("Held local Telling Time callback must not block the EDT");
                        scenario.fail(error);
                        scenario.signalPrivacyHostChoiceFailure();
                        scenario.releasePrivacyHostChoice();
                        throw error;
                    }
                    scenario.markPrivacyHostChoiceHeld();
                    try {
                        if (!scenario.awaitPrivacyHostChoiceRelease(60, TimeUnit.SECONDS)) {
                            AssertionError error = new AssertionError("Held local Telling Time choice exceeded its bounded release wait");
                            scenario.fail(error);
                            throw error;
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        scenario.fail(error);
                        throw new AssertionError("Held local Telling Time choice was interrupted", error);
                    } finally {
                        scenario.clearPrivacyHostChoiceActive();
                    }
                }
                return List.of(selected);
            }
            throw unexpected("chooseEntitiesForEffect", title, options, min, max, reveal);
        }
        @Override public Map<CardView, Integer> assignCombatDamage(CardView attacker, List<CardView> blockers, int damage,
                GameEntityView defender, boolean overrideOrder, boolean maySkip) {
            throw unexpected("assignCombatDamage", attacker, blockers, damage, defender, overrideOrder, maySkip);
        }
        @Override public Map<Object, Integer> assignGenericAmount(CardView source, Map<Object, Integer> target, int amount,
                boolean atLeastOne, String label) {
            throw unexpected("assignGenericAmount", source, target, amount, atLeastOne, label);
        }
        @Override public List<CardView> manipulateCardList(String title, Iterable<CardView> cards, Iterable<CardView> manipulable,
                boolean toTop, boolean toBottom, boolean toAnywhere) {
            throw unexpected("manipulateCardList", title, cards, manipulable, toTop, toBottom, toAnywhere);
        }

        private boolean retainLondonForAppliedHand(Capture buttons, PlayerView owner,
                String one, String two, boolean enabledOne, boolean enabledTwo) {
            if (!scenario.privacyMulliganPrefixScenario() || !remote || seat != 1 || owner == null || owner.getId() != ownPlayerViewId || !Localizer.getInstance().getMessage("lblOK").equalsIgnoreCase(one) || !Localizer.getInstance().getMessage("lblAuto").equalsIgnoreCase(two) || enabledOne || !enabledTwo || buttons == null || buttons.callbackOrdinal() <= 0) return false;
            MulliganChoiceReceipt regular = scenario.regularMulliganChoice();
            Game engine = scenario.openingKeepEngineGame();
            GameView current = getGameView();
            if (regular == null || regular.decision() != Decision.REGULAR_MULLIGAN || regular.seat() != seat || regular.chooserViewId() != ownPlayerViewId || engine == null || current == null || engine.getView() == null || current.getId() != engine.getView().getId() || regular.gameViewId() != current.getId() || seat >= engine.getPlayers().size() || engine.getPlayers().get(seat).getId() != ownPlayerViewId || !(engine.getPlayers().get(seat).getController() instanceof PlayerControllerHuman serverController) || serverController.getGame() != engine) return false;
            PrivacyInputSnapshot input = new PrivacyInputSnapshot(
                    serverController.getInputProxy().getInput(), serverController.getInputQueue().getInput());
            if (input.proxyInput() == null || input.proxyInput() != input.queuedInput() || !(input.proxyInput() instanceof InputLondonMulligan londonInput) || londonInput.getOwner() == null || londonInput.getOwner().getId() != ownPlayerViewId) return false;
            IGameController endpoint = getGameController(owner);
            if (endpoint == null || scenario.stopped()) return false;
            if (pendingLondonPromptOrdinal >= 0) {
                return (londonAwaitingAppliedHand || londonSelectedCard != null)
                        && londonEngineGame == engine && londonEngineController == serverController
                        && londonEngineInput == londonInput && londonController == endpoint
                        && londonGameViewId == current.getId()
                        && buttons.callbackOrdinal() > londonButtonsCallbackOrdinal;
            }
            if (londonAwaitingAppliedHand || londonSelectedCard != null) return false;
            londonEngineGame = engine;
            londonEngineController = serverController;
            londonEngineInput = londonInput;
            londonController = endpoint;
            londonGameViewId = current.getId();
            londonButtonsCallbackOrdinal = buttons.callbackOrdinal();
            pendingLondonPromptOrdinal = buttons.callbackOrdinal();
            londonAwaitingAppliedHand = true;
            return true;
        }

        private void dispatchLondonSelectedCard(Capture prompt, String message) {
            String expectedOne = String.format(Localizer.getInstance().getMessage("lblReturnForLondon"), 1);
            if (prompt == null || prompt.callbackOrdinal() <= pendingLondonPromptOrdinal || !expectedOne.equals(message) || londonSelectedCard == null || londonController == null || !londonSelectionClaimed.compareAndSet(false, true)) {
                fail("London one-card prompt lacked one retained applied-hand selection");
                return;
            }
            londonOneCardPrompt = message;
            final IGameController controller = londonController;
            final CardView selected = londonSelectedCard;
            final boolean appliedHandDeferred = londonAppliedHandCallbackOrdinal > 0;
            pendingLondonBottomCardId = selected.getId();
            queueAction(() -> {
                try {
                    if (appliedHandDeferred && !isCurrentLondonAppliedSelection(selected, controller)) {
                        fail("Deferred London selectCard lost its retained applied Hand/input before EDT dispatch");
                        return;
                    }
                    if (!controller.selectCard(selected, null, null)) {
                        fail("Controller rejected London bottom card selection");
                    }
                } catch (Throwable t) { scenario.fail(t); }
            });
        }

        private boolean isCurrentLondonAppliedSelection(CardView selected, IGameController endpointController) {
            Game engine = londonEngineGame;
            GameView current = getGameView();
            PlayerView own = current == null ? null : findPlayerView(current, ownPlayerViewId);
            PrivacyInputSnapshot input = londonEngineController == null ? null : new PrivacyInputSnapshot(
                    londonEngineController.getInputProxy().getInput(), londonEngineController.getInputQueue().getInput());
            return londonAppliedHandCallbackOrdinal > londonButtonsCallbackOrdinal
                    && londonAppliedHandTrackerIdentity != null && current != null
                    && current.getId() == londonGameViewId && current.getTracker() == londonAppliedHandTrackerIdentity
                    && engine != null && engine == scenario.openingKeepEngineGame()
                    && engine.getView() != null && engine.getView().getId() == londonGameViewId
                    && londonEngineController != null && londonEngineController.getGame() == engine
                    && seat < engine.getPlayers().size()
                    && engine.getPlayers().get(seat).getId() == ownPlayerViewId
                    && engine.getPlayers().get(seat).getController() == londonEngineController
                    && londonEngineInput != null && input != null
                    && input.proxyInput() == londonEngineInput && input.queuedInput() == londonEngineInput
                    && londonEngineInput.getOwner() != null
                    && londonEngineInput.getOwner().getId() == ownPlayerViewId
                    && own != null && getGameController(own) == endpointController
                    && endpointController == londonController && own.getHand() != null
                    && own.getHand().size() == 7 && selected.getTracker() == current.getTracker()
                    && own.getHand().stream().anyMatch(card -> card.getId() == selected.getId());
        }

        private void observeLondonAppliedHand(Capture appliedCapture, GameView appliedView) {
            if (!londonAwaitingAppliedHand || appliedCapture == null || appliedView == null || !appliedCapture.stateBearing() || appliedCapture.callbackOrdinal() <= londonButtonsCallbackOrdinal || scenario.stopped()) return;
            if (appliedView.getId() != londonGameViewId || getGameView() != appliedView) {
                fail("London post-button state changed the retained current GameView identity");
                return;
            }
            Game engine = londonEngineGame;
            PlayerControllerHuman controller = londonEngineController;
            InputLondonMulligan input = londonEngineInput;
            if (engine == null || controller == null || input == null || engine != scenario.openingKeepEngineGame() || controller.getGame() != engine || engine.getView() == null || engine.getView().getId() != londonGameViewId || seat >= engine.getPlayers().size() || engine.getPlayers().get(seat).getId() != ownPlayerViewId) {
                fail("London applied-hand gate lost its retained same-Game controller identity");
                return;
            }
            GameViewReceipt receipt = gameViewReceipts().stream()
                    .filter(item -> item.stateBearing() && item.callbackOrdinal() > londonButtonsCallbackOrdinal
                            && item.gameViewId() == londonGameViewId && item.trackerIdentity() == appliedView.getTracker()
                            && item.zonesByPlayer().getOrDefault(ownPlayerViewId, Map.of())
                                    .getOrDefault(ZoneType.Hand, List.of()).size() == 7)
                    .max(Comparator.comparingLong(GameViewReceipt::callbackOrdinal)).orElse(null);
            if (receipt == null) return;
            PlayerView own = findPlayerView(appliedView, ownPlayerViewId);
            PrivacyInputSnapshot currentInput = new PrivacyInputSnapshot(
                    controller.getInputProxy().getInput(), controller.getInputQueue().getInput());
            IGameController endpoint = own == null ? null : getGameController(own);
            List<Integer> handIds = own == null || own.getHand() == null ? List.of() : cardIds(own.getHand());
            List<Integer> receiptHand = receipt.zonesByPlayer().getOrDefault(ownPlayerViewId, Map.of())
                    .getOrDefault(ZoneType.Hand, List.of());
            if (appliedView.getTracker() != receipt.trackerIdentity() || handIds.size() != 7 || !handIds.equals(receiptHand) || own == null || endpoint != londonController || currentInput.proxyInput() != input || currentInput.queuedInput() != input || input.getOwner() == null || input.getOwner().getId() != ownPlayerViewId) {
                fail("London post-button applied Hand did not retain its seven-card same-input witness");
                return;
            }
            CardView bottomCard = null;
            for (CardView card : own.getHand()) {
                if (bottomCard == null || card.getId() < bottomCard.getId()) bottomCard = card;
            }
            if (bottomCard == null) {
                fail("London applied Hand has no eligible card");
                return;
            }
            londonAppliedHandCallbackOrdinal = receipt.callbackOrdinal();
            londonAppliedHandTrackerIdentity = receipt.trackerIdentity();
            londonSelectedCard = bottomCard;
            londonAwaitingAppliedHand = false;
            if (londonOneCardPromptCapture != null) {
                dispatchLondonSelectedCard(londonOneCardPromptCapture, londonDeferredOneCardPrompt);
            }
        }

        @Override public void showPromptMessage(PlayerView player, String message, CardView card) {
            // Match CMatchUI: a current prompt supersedes any pending waiting timer.
            cancelAwaitNextInput();
            Capture prompt = capture("showPromptMessage", -1, player == null ? -1 : player.getId(), message,
                    card == null ? -1 : card.getId());
            super.showPromptMessage(player, message, card);
            if (scenario.privacyScenario() && prompt != null && player != null) {
                scenario.recordPrivacyPrompt(new PrivacyPromptReceipt(seat, player.getId(),
                        prompt.callbackOrdinal(), message));
            }
            if (scenario.privacyScenario() && seat == scenario.privacyFengrafSeat() && player != null && player.getId() == scenario.privacyViewIdForSeat(scenario.privacyFengrafSeat()) && scenario.privacyConfirmationRepaintPending()) {
                PrivacyInputSnapshot inputSnapshot = scenario.privacyInputSnapshotForSeat(scenario.privacyFengrafSeat());
                if (prompt == null || prompt.callbackOrdinal() <= scenario.privacyConfirmRepaintOrdinal() || card != null && card.getId() != scenario.privacyFengrafId() || !scenario.privacyExpectedSacrificeMessage().equals(message)) {
                    fail("Fengraf repaint message did not retain its exact source and prompt: prompt="
                            + (prompt == null ? "null" : prompt.callbackOrdinal()) + ", buttons="
                            + scenario.privacyConfirmRepaintOrdinal() + ", card="
                            + (card == null ? -1 : card.getId()) + "/" + scenario.privacyFengrafId()
                            + ", message=" + message + ", "
                            + (inputSnapshot == null ? "inputSnapshot=null" : inputSnapshot.evidence()));
                    return;
                }
                scenario.recordPrivacySacrificeRepaintPrompt(prompt.callbackOrdinal(), message,
                        card == null ? -1 : card.getId(), inputSnapshot);
                return;
            }
            if (scenario.privacyScenario() && seat == scenario.privacyFengrafSeat() && player != null && player.getId() == scenario.privacyViewIdForSeat(scenario.privacyFengrafSeat()) && scenario.privacyConfirmationPending()) {
                PrivacyInputSnapshot inputSnapshot = scenario.privacyInputSnapshotForSeat(scenario.privacyFengrafSeat());
                if (prompt == null || prompt.callbackOrdinal() <= scenario.privacyConfirmButtonsOrdinal() || card != null && card.getId() != scenario.privacyFengrafId()) {
                    fail("Fengraf message predicate failed: prompt=" + (prompt == null ? "null" : prompt.callbackOrdinal())
                            + ", buttons=" + scenario.privacyConfirmButtonsOrdinal() + ", card="
                            + (card == null ? -1 : card.getId()) + "/" + scenario.privacyFengrafId()
                            + ", message=" + message + ", "
                            + (inputSnapshot == null ? "inputSnapshot=null" : inputSnapshot.evidence()));
                    return;
                }
                scenario.recordPrivacySacrificeConfirmation(seat, ownPlayerViewId,
                        scenario.privacyConfirmButtonsOrdinal(), prompt.callbackOrdinal(), message,
                        card == null ? -1 : card.getId(), inputSnapshot);
                IGameController confirmController = getGameController(player);
                if (confirmController == null) fail("Fengraf confirmation had no assigned actor controller");
                else queueAction(() -> {
                    if (!scenario.claimPrivacySacrificeOkDispatch()) {
                        PrivacyInputSnapshot currentInput = scenario.privacyInputSnapshotForSeat(scenario.privacyFengrafSeat());
                        fail("Fengraf confirmation was not dispatchable before its single OK: confirmOrdinal="
                                + scenario.privacyFengrafConfirmOrdinal() + ", repaintButtons="
                                + scenario.privacyConfirmRepaintOrdinal() + ", repaintPending="
                                + scenario.privacyConfirmationRepaintPending() + ", "
                                + (currentInput == null ? "inputSnapshot=null" : currentInput.evidence()));
                        return;
                    }
                    confirmController.selectButtonOk();
                });
                return;
            }
            if (scenario.metamorphoseScenario()) {
                int promptViewId = player == null ? -1 : player.getId();
                if (seat == 0 && scenario.metamorphoseStage() == 1 && !pendingMetamorphoseTargetIds.isEmpty()) {
                    CardView selected = findVisibleCard(getGameView(), scenario.metamorphoseTargetCardId());
                    if (prompt == null || prompt.callbackOrdinal() <= 0 || promptViewId != ownPlayerViewId || card == null || card.getId() != scenario.metamorphoseHostCardId() || selected == null || !pendingMetamorphoseTargetIds.contains(selected.getId()) || pendingMetamorphoseTargetPromptOrdinal > 0) {
                        fail("Metamorphose's actual target prompt did not expose the assigned target Island");
                    } else {
                        pendingMetamorphoseTargetPromptOrdinal = prompt == null ? -1 : prompt.callbackOrdinal();
                    }
                    return;
                }
                if (seat == 1 && scenario.metamorphoseOptionalPromptArmed()) {
                    long optionalPromptOrdinal = prompt == null ? -1 : prompt.callbackOrdinal();
                    int acceptedGameViewId = gameViewId(getGameView());
                    scenario.recordMetamorphoseOptional(seat, promptViewId, message,
                            optionalPromptOrdinal, acceptedGameViewId,
                            scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                    if (scenario.holdMetamorphoseOptional()) {
                        scenario.markMetamorphoseOptionalHeld();
                        return;
                    }
                    scenario.recordMetamorphoseDecision(scenario.metamorphoseAccept(), seat, ownPlayerViewId,
                            optionalPromptOrdinal);
                    IGameController controller = player == null ? null : getGameController(player);
                    if (controller == null) fail("Metamorphose optional prompt had no remote assigned controller");
                    else {
                        ProtocolMethod responseMethod = scenario.metamorphoseAccept()
                                ? ProtocolMethod.selectButtonOk : ProtocolMethod.selectButtonCancel;
                        scenario.queueMetamorphoseResponse("optional", acceptedGameViewId,
                                optionalPromptOrdinal, responseMethod);
                        queueAction(() -> {
                            scenario.dispatchMetamorphoseResponse("optional");
                            if (scenario.metamorphoseAccept()) controller.selectButtonOk();
                            else controller.selectButtonCancel();
                        });
                    }
                    return;
                }
                if (seat == 1 && scenario.heldMetamorphoseOptionalBeforeLifecycleConcession() && scenario.metamorphoseStage() == 3) {
                    scenario.recordMetamorphoseHeldOptionalPromptRepaint(seat, promptViewId,
                            gameViewId(getGameView()), prompt == null ? -1 : prompt.callbackOrdinal(), message,
                            scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                    return;
                }
                if (seat == 1 && scenario.metamorphoseAccept() && scenario.metamorphoseStage() == 4 && !pendingMetamorphoseHandIds.isEmpty()) {
                    CardView selected = findVisibleCard(getGameView(), scenario.metamorphoseHandCardId());
                    if (selected == null || !pendingMetamorphoseHandIds.contains(selected.getId()) || player == null || player.getId() != ownPlayerViewId || prompt == null || prompt.callbackOrdinal() <= 0 || pendingMetamorphoseHandPromptOrdinal > 0) {
                        fail("Metamorphose private-hand prompt did not expose its exact assigned B chooser and selected card");
                    } else {
                        pendingMetamorphoseHandPromptOrdinal = prompt == null ? -1 : prompt.callbackOrdinal();
                        pendingMetamorphoseHandPromptMessage = message;
                        scenario.captureMetamorphoseHandPrompt(seat, ownPlayerViewId,
                                prompt.callbackOrdinal(), gameViewId(getGameView()),
                                scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                    }
                    return;
                }
            }
            if (scenario.starterSeatZeroScenario()) {
                String expectedStarterPrompt = Localizer.getInstance().getMessage("lblYouHaveWonTheCoinToss", expectedPlayerName)
                        + "\n\n" + Localizer.getInstance().getMessage("lblWouldYouLiketoPlayorDraw");
                if (expectedStarterPrompt.equals(message)) {
                    scenario.recordStarterPrompt(new StarterPromptReceipt(seat,
                            player == null ? -1 : player.getId(), message,
                            prompt == null ? -1 : prompt.callbackOrdinal()));
                }
            }
            String expectedOne = String.format(Localizer.getInstance().getMessage("lblReturnForLondon"), 1);
            String expectedZero = String.format(Localizer.getInstance().getMessage("lblReturnForLondon"), 0);
            if (!scenario.mulliganScenario()) return;
            if (expectedOne.equals(message) && londonAwaitingAppliedHand) {
                if (prompt == null || prompt.callbackOrdinal() <= londonButtonsCallbackOrdinal || londonOneCardPromptCapture != null) {
                    fail("London one-card message did not uniquely follow its retained button callback");
                    return;
                }
                londonOneCardPromptCapture = prompt;
                londonDeferredOneCardPrompt = message;
                return;
            }
            if (pendingLondonPromptOrdinal < 0) {
                if (expectedOne.equals(message) || expectedZero.equals(message)) fail("London message arrived without its button callback");
                return;
            }
            if (scenario.hasLondonBottom()) {
                if (expectedOne.equals(message) || expectedZero.equals(message)) fail("Repeated London prompt after completed bottom action");
                return;
            }
            if (player == null || player.getId() != ownPlayerViewId || prompt == null) {
                fail("London prompt was not delivered to the assigned chooser");
                return;
            }
            if (expectedOne.equals(message) && pendingLondonBottomCardId < 0 && londonSelectedCard != null) {
                if (prompt.callbackOrdinal() <= pendingLondonPromptOrdinal) {
                    fail("London one-card message did not follow its actual button callback");
                    return;
                }
                if (londonAppliedHandCallbackOrdinal > 0 && londonAppliedHandCallbackOrdinal <= pendingLondonPromptOrdinal) {
                    fail("London applied-hand witness did not follow its button callback");
                    return;
                }
                dispatchLondonSelectedCard(prompt, message);
                return;
            }
            if (expectedZero.equals(message) && pendingLondonBottomCardId > 0 && londonEnabledOkCallbackSeen && londonZeroPromptOrdinal < 0) {
                londonZeroPromptOrdinal = prompt.callbackOrdinal();
                londonZeroCardPrompt = message;
                final IGameController controller = londonController;
                final CardView selected = londonSelectedCard;
                queueAction(() -> {
                    try {
                        scenario.recordAction(new ActionReceipt(Decision.BOTTOM_LONDON, seat, ownPlayerViewId,
                                selected.getId(), false));
                        GameView currentView = getGameView();
                        scenario.recordLondonBottomDispatch(new LondonBottomDispatchReceipt(seat, ownPlayerViewId,
                                currentView == null ? -1 : currentView.getId(), -1,
                                selected.getId(), snapshotOwnHand().ids(), pendingLondonPromptOrdinal,
                                londonAppliedHandCallbackOrdinal, londonOneCardPrompt,
                                londonZeroPromptOrdinal, londonZeroCardPrompt));
                        controller.selectButtonOk();
                    } catch (Throwable t) { scenario.fail(t); }
                });
                return;
            }
            fail("Unexpected, repeated, or wrong-count London prompt: " + message);
        }

        @Override public void updateButtons(PlayerView owner, String one, String two, boolean enabledOne, boolean enabledTwo, boolean focus) {
            Capture promptCapture = capture("updateButtons", -1, owner == null ? -1 : owner.getId(), one, two,
                    enabledOne, enabledTwo, focus);
            super.updateButtons(owner, one, two, enabledOne, enabledTwo, focus);
            if (scenario.stopped()) return;
            if (absorbClaimedOpeningKeepRepeat(owner, one, two, enabledOne, enabledTwo)) return;
            Localizer localizer = Localizer.getInstance();
            String oneText = one == null ? "" : one.trim();
            String twoText = two == null ? "" : two.trim();
            if (scenario.privacyScenario() && owner != null && seat == scenario.privacyFengrafSeat() && owner.getId() == scenario.privacyViewIdForSeat(scenario.privacyFengrafSeat()) && localizer.getMessage("lblYes").equalsIgnoreCase(oneText) && localizer.getMessage("lblNo").equalsIgnoreCase(twoText)) {
                PrivacyInputSnapshot inputSnapshot = scenario.privacyInputSnapshotForSeat(scenario.privacyFengrafSeat());
                scenario.recordPrivacySacrificeButtons(seat, ownPlayerViewId,
                        promptCapture == null ? -1 : promptCapture.callbackOrdinal(), oneText, twoText,
                        enabledOne, enabledTwo, inputSnapshot);
                return;
            }
            if (scenario.privacyScenario() && owner != null && owner.getId() == ownPlayerViewId && localizer.getMessage("lblAuto").equalsIgnoreCase(oneText) && localizer.getMessage("lblCancel").equalsIgnoreCase(twoText)) {
                if (promptCapture != null && promptCapture.callbackOrdinal() > 0 && promptCapture.callbackOrdinal() <= lastPrivacyManaContinuationOrdinal) return;
                if (pendingPrivacyManaCardId > 0) {
                    CardView pending = findVisibleCard(getGameView(), pendingPrivacyManaCardId);
                    if (pending == null || !pending.isTapped()) {
                        if (enabledOne && enabledTwo && promptCapture != null && promptCapture.callbackOrdinal() > 0) {
                            pendingPrivacyManaPromptOrdinal = promptCapture.callbackOrdinal();
                            pendingPrivacyManaPromptOwnerId = owner.getId();
                            pendingPrivacyManaPromptController = getGameController(owner);
                        }
                        return;
                    }
                    pendingPrivacyManaCardId = -1;
                    pendingPrivacyManaPromptOrdinal = -1;
                    pendingPrivacyManaPromptOwnerId = -1;
                    pendingPrivacyManaPromptController = null;
                }
                if (scenario.privacyExpectedManaCount() == 0) return;
                if (enabledTwo == false) {
                    fail("Privacy payment prompt did not expose the legal Cancel choice");
                    return;
                }
                if (!enabledOne) return; // Initial callback disables Auto until a source can be selected.
                IGameController paymentController = getGameController(owner);
                dispatchPrivacyManaSource(ownPlayerViewId, paymentController);
                return;
            }

            if (scenario.metamorphoseScenario() && pendingMetamorphoseTargetPromptOrdinal > 0) {
                if (seat != 0 || owner == null || owner.getId() != ownPlayerViewId || promptCapture == null || promptCapture.callbackOrdinal() <= pendingMetamorphoseTargetPromptOrdinal || !localizer.getMessage("lblOK").equalsIgnoreCase(oneText) || !localizer.getMessage("lblCancel").equalsIgnoreCase(twoText) || enabledOne || !enabledTwo) {
                    fail("Metamorphose target prompt did not receive its matching initial OK-disabled/Cancel-enabled buttons");
                    return;
                }
                GameView current = getGameView();
                CardView selected = findVisibleCard(current, scenario.metamorphoseTargetCardId());
                IGameController targetController = getGameController(owner);
                if (selected == null || !pendingMetamorphoseTargetIds.contains(selected.getId()) || targetController == null) {
                    fail("Metamorphose target buttons arrived without the assigned visible target and controller");
                    return;
                }
                long targetPromptOrdinal = pendingMetamorphoseTargetPromptOrdinal;
                scenario.recordMetamorphoseInputButtons(new MetamorphoseInputButtonsReceipt("target", seat,
                        ownPlayerViewId, targetPromptOrdinal, promptCapture.callbackOrdinal(), oneText, twoText,
                        enabledOne, enabledTwo));
                if (scenario.stopped()) return;
                scenario.recordMetamorphoseTargetSelected(seat, ownPlayerViewId, targetPromptOrdinal);
                if (scenario.stopped()) return;
                acceptedMetamorphoseTargetInput = snapshotInput(owner);
                acceptedMetamorphoseTargetGameViewId = gameViewId(getGameView());
                acceptedMetamorphoseTargetControllerViewId = controllerGameViewId(targetController);
                acceptedMetamorphoseTargetController = targetController;
                pendingMetamorphoseTargetPromptOrdinal = -1;
                pendingMetamorphoseTargetIds = List.of();
                int queuedActions = metamorphoseTargetActionsQueued.incrementAndGet();
                if (queuedActions != 1) {
                    fail("Metamorphose target action was queued more than once");
                    return;
                }
                queueAction(() -> {
                    int dispatchedActions = metamorphoseTargetActionsDispatched.incrementAndGet();
                    if (dispatchedActions != 1) {
                        fail("Metamorphose target action was dispatched more than once");
                        return;
                    }
                    boolean accepted = targetController.selectCard(selected, null, null);
                    metamorphoseTargetActionAccepted = accepted;
                    if (!accepted) {
                        fail("Host controller rejected the legal Metamorphose target");
                    }
                });
                return;
            }
            if (scenario.metamorphoseScenario() && pendingMetamorphoseHandPromptOrdinal > 0) {
                if (seat != 1 || owner == null || owner.getId() != ownPlayerViewId || promptCapture == null || promptCapture.callbackOrdinal() <= pendingMetamorphoseHandPromptOrdinal || !localizer.getMessage("lblOK").equalsIgnoreCase(oneText) || !localizer.getMessage("lblCancel").equalsIgnoreCase(twoText) || enabledOne || !enabledTwo) {
                    fail("Metamorphose private-hand prompt did not receive its matching initial OK-disabled/Cancel-enabled buttons");
                    return;
                }
                GameView current = getGameView();
                CardView selected = findVisibleCard(current, scenario.metamorphoseHandCardId());
                IGameController handController = getGameController(owner);
                long handPromptOrdinal = pendingMetamorphoseHandPromptOrdinal;
                String handPromptMessage = pendingMetamorphoseHandPromptMessage;
                List<Integer> offeredIds = pendingMetamorphoseHandIds;
                if (selected == null || !offeredIds.contains(selected.getId()) || handController == null) {
                    fail("Metamorphose private-hand buttons arrived without the assigned visible card and controller");
                    return;
                }
                scenario.recordMetamorphoseInputButtons(new MetamorphoseInputButtonsReceipt("private hand", seat,
                        ownPlayerViewId, handPromptOrdinal, promptCapture.callbackOrdinal(), oneText, twoText,
                        enabledOne, enabledTwo));
                if (scenario.stopped()) return;
                scenario.recordMetamorphoseHandPrompt(seat, ownPlayerViewId, handPromptOrdinal,
                        handPromptMessage, offeredIds, gameViewId(current), promptCapture.callbackOrdinal(),
                        scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                if (scenario.stopped()) return;
                pendingMetamorphoseHandPromptOrdinal = -1;
                pendingMetamorphoseHandPromptMessage = null;
                pendingMetamorphoseHandIds = List.of();
                scenario.queueMetamorphoseResponse("private hand", gameViewId(current), handPromptOrdinal,
                        ProtocolMethod.selectCard);
                queueAction(() -> {
                    scenario.recordMetamorphoseHandDispatch(seat, ownPlayerViewId, handPromptOrdinal);
                    scenario.dispatchMetamorphoseResponse("private hand");
                    if (!handController.selectCard(selected, null, null)) {
                        fail("B controller rejected its legal private-hand card");
                    }
                });
                return;
            }
            if (owner == null || !enabledOne && !enabledTwo) return;
            if (ownPlayerViewId < 0 || owner.getId() != ownPlayerViewId) {
                fail("Enabled prompt owner does not match openView seat " + seat);
                return;
            }
            final IGameController controller = getGameController(owner);
            if (controller == null) { fail("Missing controller for assigned seat " + seat); return; }
            final GameView game = getGameView();
            if (game == null) { fail("Enabled choice arrived before GameView for seat " + seat); return; }
            PrivacyStackPriorityCheckpoint stackCheckpoint = null;
            PrivacyPaymentReceipt paymentReceipt = null;
            boolean privacyPriorityPrompt = scenario.privacyScenario() && scenario.privacyPriorityStage() > 0
                    && isPriorityPrompt(oneText, twoText);
            int stackSize = game.getStack() == null ? -1 : game.getStack().size();
            if (privacyPriorityPrompt && stackSize == 1) {
                PlayerView turnPlayer = game.getPlayerTurn();
                StackItemView item = game.getStack().get(0);
                CardView source = item == null ? null : item.getSourceCard();
                PlayerView activator = item == null ? null : item.getActivatingPlayer();
                Game liveGame = scenario.privacyGame();
                SpellAbility ability = liveGame == null || liveGame.getStack() == null
                        || liveGame.getStack().size() != 1 ? null : liveGame.getStack().peekAbility();
                if (source == null || activator == null || ability == null) {
                    fail("Actual privacy stack prompt has no retained live ability/source/activator");
                    return;
                }
                if (seat == 0) {
                    paymentReceipt = scenario.snapshotPrivacyPayment(ability,
                            promptCapture == null ? -1 : promptCapture.callbackOrdinal(), source.getId(), activator.getId());
                    if (paymentReceipt == null) return;
                }
                long ordinal = promptCapture == null ? -1 : promptCapture.callbackOrdinal();
                int callbackTurnPlayerId = turnPlayer == null ? -1 : turnPlayer.getId();
                stackCheckpoint = scenario.capturePrivacyStackPriority(this, controller, game, seat,
                        ownPlayerViewId, ordinal, game.getTurn(), game.getPhase(), callbackTurnPlayerId,
                        ability, paymentReceipt);
                if (stackCheckpoint == null) return;
                if (scenario.isRepeatedPrivacyStackPriority(stackCheckpoint)) return;
                if (!scenario.beginPrivacyStackPriority(stackCheckpoint)) return;
            } else if (privacyPriorityPrompt && stackSize == 0 && !scenario.reconcilePrivacyStackPassBeforePriority()) {
                return;
            }
            if (scenario.privacyScenario() && seat == 0 && promptCapture != null && isPriorityPrompt(oneText, twoText)) {
                recordAppliedView("privacyPriority", promptCapture, game.getId(), game);
            }
            if (isPriorityPrompt(oneText, twoText) && promptCapture != null) {
                PlayerView turnPlayer = game.getPlayerTurn();
                PriorityReceipt receipt = new PriorityReceipt(seat, ownPlayerViewId,
                        promptCapture.callbackOrdinal(), game.getTurn(), game.getPhase(),
                        turnPlayer == null ? -1 : turnPlayer.getId(), oneText, twoText,
                        enabledOne, enabledTwo);
                priorityPrompts.add(receipt);

                if (seat == 0 && receipt.turn() == 1 && receipt.phase() == PhaseType.MAIN1) {
                    turnOneMainOnePriority.countDown();
                }
                int replayTurn = false ? 4 : 1;
                int replayTurnPlayer = false
                        ? scenario.privacyViewIdForSeat(1) : scenario.playerViewIdForSeat(0);
                if (expectReconnectReplay && seat == 1 && receipt.turn() == replayTurn && receipt.phase() == PhaseType.UPKEEP && receipt.turnPlayerViewId() == replayTurnPlayer) {
                    reconnectReplayPriority.countDown();
                }
            }
            if (scenario.privacyScenario() && game.getStack() != null && game.getStack().size() == 1 && isPriorityPrompt(oneText, twoText) && promptCapture != null) {
                StackItemView item = game.getStack().get(0);
                CardView source = item == null ? null : item.getSourceCard();
                PlayerView activator = item == null ? null : item.getActivatingPlayer();
                if (source == null || activator == null) {
                    fail("Actual privacy stack view has no retained source/activator facts");
                    return;
                }
                scenario.recordPrivacyStackView(new PrivacyStackViewReceipt(seat, ownPlayerViewId,
                        promptCapture.callbackOrdinal(), source.getId(), activator.getId(), item.isAbility()));
                if (scenario.stopped()) return;
                if (seat == 0) {
                    if (paymentReceipt == null || stackCheckpoint == null) {
                        fail("Host payment was not captured from its current exact stack input");
                        return;
                    }
                    scenario.recordPrivacyPayment(paymentReceipt, stackCheckpoint.ability());
                    if (scenario.stopped()) return;
                    if (!remote && activator.getId() == ownPlayerViewId && pendingPrivacyManaCardId > 0 && paymentReceipt.payingManaSourceIds().contains(pendingPrivacyManaCardId)) {
                        pendingPrivacyManaCardId = -1;
                        pendingPrivacyManaPromptOrdinal = -1;
                        pendingPrivacyManaPromptOwnerId = -1;
                        pendingPrivacyManaPromptController = null;
                    }
                }
            }
            if (privacyPriorityPrompt) {
                PlayerView turnPlayer = game.getPlayerTurn();
                PrivacyPriorityAction action = scenario.routePrivacyPriority(seat, ownPlayerViewId,
                        promptCapture == null ? -1 : promptCapture.callbackOrdinal(), game.getId(),
                        game.getTurn(), game.getPhase(),
                        turnPlayer == null ? -1 : turnPlayer.getId(), game.getStack() == null ? -1 : game.getStack().size(),
                        this);
                if (action == null || scenario.stopped()) return;
                final PrivacyStackPriorityCheckpoint acceptedStackCheckpoint = stackCheckpoint;
                if (action == PrivacyPriorityAction.HOLD_TELLING_TIME || action == PrivacyPriorityAction.HOLD_FENGRAF || action == PrivacyPriorityAction.HOLD_SHUFFLE_HISTORY || action == PrivacyPriorityAction.HOLD_HISTORY_RECONNECT_HOST || action == PrivacyPriorityAction.HOLD_T3_SHUFFLE_CAST) return;
                if (action == PrivacyPriorityAction.CAST_TELLING_TIME) {
                    CardView tellingTime = findVisibleCard(game, scenario.privacyTellingTimeId());
                    PlayerView own = findPlayerView(game, ownPlayerViewId);
                    if (tellingTime == null || own == null || own.getHand() == null || !cardIds(own.getHand()).contains(scenario.privacyTellingTimeId())) {
                        fail("Remote assigned hand did not contain the actual Telling Time at MAIN1 priority");
                        return;
                    }
                    if (promptCapture == null) {
                        fail("Telling Time draw baseline lacked its real remote MAIN1 callback");
                        return;
                    }
                    Map<Integer, Integer> drawCounts = new LinkedHashMap<>();
                    for (Object value : game.getPlayers().toArray()) {
                        if (!(value instanceof PlayerView player)) throw new AssertionError("GameView contains a non-PlayerView player entry");
                        drawCounts.put(player.getId(), player.getNumDrawnThisTurn());
                    }
                    scenario.recordPrivacyTellingTimeStartDrawCounts(seat, ownPlayerViewId,
                            promptCapture.callbackOrdinal(), game.getTurn(), game.getPhase(), drawCounts);
                    if (scenario.stopped()) return;
                    queueAction(() -> {
                        if (!controller.selectCard(tellingTime, null, null)) fail("Remote controller rejected Telling Time");
                    });
                    return;
                }
                if (action == PrivacyPriorityAction.PASS_STACK) {
                    if (acceptedStackCheckpoint == null) {
                        fail("Privacy stack pass was not reserved from its exact paid-stack input");
                        return;
                    }
                    queueAction(() -> scenario.reserveAndDispatchPrivacyStackPass(
                            acceptedStackCheckpoint, controller::selectButtonOk));
                    return;
                }
                if (action == PrivacyPriorityAction.PASS) {
                    queueAction(controller::selectButtonOk);
                    return;
                }
            }
            ExpectedAction expected = script.peekFirst();

            if (scenario.mulliganScenario() && localizer.getMessage("lblOK").equalsIgnoreCase(oneText) && localizer.getMessage("lblAuto").equalsIgnoreCase(twoText)) {
                if (scenario.hasLondonBottom()) {
                    fail("Received an additional London bottom prompt after the scripted selection");
                    return;
                }
                if (pendingLondonBottomCardId > 0 && enabledOne) {
                    if (enabledTwo || londonZeroPromptOrdinal >= 0 || londonEnabledOkCallbackSeen) {
                        fail("Repeated London confirmation after the selected-card callback");
                    } else {
                        londonEnabledOkCallbackSeen = true;
                    }
                    return;
                }
                if (pendingLondonPromptOrdinal > 0 && (londonAwaitingAppliedHand || londonSelectedCard != null)) {
                    if (retainLondonForAppliedHand(promptCapture, owner, oneText, twoText,
                            enabledOne, enabledTwo)) return;
                    fail("Repeated London readiness buttons changed the retained same-input prompt");
                    return;
                }
                MulliganChoiceReceipt regularChoice = scenario.regularMulliganChoice();
                if (regularChoice == null || regularChoice.seat() != seat || regularChoice.chooserViewId() != ownPlayerViewId || enabledOne || !enabledTwo || pendingLondonBottomCardId > 0 || londonController != null) {
                    fail("Unexpected repeated or already-complete London bottom prompt for seat " + seat);
                    return;
                }
                PlayerView appliedOwner = findPlayerView(game, ownPlayerViewId);
                if (appliedOwner == null || appliedOwner.getHand() == null || appliedOwner.getHand().size() != 7) {
                    if (retainLondonForAppliedHand(promptCapture, owner, oneText, twoText, enabledOne, enabledTwo)) {
                        return;
                    }
                    fail("London bottom prompt did not have seven cards in the assigned applied hand for seat " + seat);
                    return;
                }
                CardView bottomCard = null;
                for (CardView card : appliedOwner.getHand()) {
                    if (bottomCard == null || card.getId() < bottomCard.getId()) bottomCard = card;
                }
                if (bottomCard == null) { fail("London bottom prompt has no eligible card in seat " + seat + "'s own hand"); return; }
                londonController = controller;
                pendingLondonPromptOrdinal = promptCapture == null ? -1 : promptCapture.callbackOrdinal();
                londonSelectedCard = bottomCard;
                return;
            }

            if (scenario.metamorphoseScenario() && owner.getId() == scenario.metamorphoseRemoteViewId() && localizer.getMessage("lblYes").equalsIgnoreCase(oneText) && localizer.getMessage("lblNo").equalsIgnoreCase(twoText)) {
                if (scenario.metamorphoseStage() == 2) {
                    scenario.recordMetamorphoseOptionalButtons(seat, ownPlayerViewId,
                            promptCapture == null ? -1 : promptCapture.callbackOrdinal(), oneText, twoText,
                            enabledOne, enabledTwo);
                    return;
                }
                if (scenario.heldMetamorphoseOptionalBeforeLifecycleConcession() && scenario.metamorphoseStage() == 3) {
                    scenario.recordMetamorphoseHeldOptionalButtonRepaint(seat, ownPlayerViewId,
                            gameViewId(getGameView()), promptCapture == null ? -1 : promptCapture.callbackOrdinal(),
                            oneText, twoText, enabledOne, enabledTwo,
                            scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                    return;
                }
                if (!scenario.metamorphoseAccept() && scenario.metamorphoseStage() == 6) {
                    scenario.recordMetamorphoseDeclinedOptionalButtonRepaint(seat, ownPlayerViewId,
                            gameViewId(getGameView()), promptCapture == null ? -1 : promptCapture.callbackOrdinal(),
                            oneText, twoText, enabledOne, enabledTwo,
                            scenario.metamorphoseRemoteInputSnapshot(scenario.metamorphoseGame()));
                    return;
                }
            }
            if (scenario.metamorphoseScenario() && owner.getId() == ownPlayerViewId && scenario.metamorphoseStage() == 2 && localizer.getMessage("lblAuto").equalsIgnoreCase(oneText) && localizer.getMessage("lblCancel").equalsIgnoreCase(twoText)) {
                if (pendingMetamorphoseManaCardId > 0) {
                    CardView pending = findVisibleCard(game, pendingMetamorphoseManaCardId);
                    if (pending == null || !pending.isTapped()) return;
                    pendingMetamorphoseManaCardId = -1;
                }
                if (!enabledOne) return;
                for (int cardId : scenario.metamorphoseManaCardIds()) {
                    if (metamorphoseManaSelections.contains(cardId)) continue;
                    CardView manaSource = findVisibleCard(game, cardId);
                    if (manaSource == null || manaSource.isTapped()) {
                        fail("A prepared untapped Island mana source was absent from the host's current view: " + cardId);
                        return;
                    }
                    metamorphoseManaSelections.add(cardId);
                    pendingMetamorphoseManaCardId = cardId;
                    final IGameController manaController = controller;
                    queueAction(() -> {
                        if (!manaController.selectCard(manaSource, null, null)) {
                            fail("Host controller rejected Metamorphose mana source " + cardId);
                        } else {
                            scenario.recordMetamorphoseManaSelection(seat, ownPlayerViewId, cardId);
                        }
                    });
                    return;
                }
                fail("Metamorphose mana prompt remained after both distinct Island sources were selected");
                return;
            }

            if (expected != null && expected.decision() == Decision.PLAY_OR_DRAW && isPlayDrawPrompt(oneText, twoText)) {
                consume(expected);
                scenario.recordAction(new ActionReceipt(Decision.PLAY_OR_DRAW, seat, ownPlayerViewId, null, true));
                queueAction(controller::selectButtonOk); // explicit Play choice
                return;
            }
            if (expected != null && expected.decision() == Decision.START_SEAT_ZERO && isLocalizedPlayDrawPrompt(oneText, twoText)) {
                if (!scenario.starterSeatZeroScenario()) {
                    fail("START_SEAT_ZERO was queued outside its named scenario");
                    return;
                }
                boolean choosePlay = seat == 0;
                scenario.recordStarterButton(new StarterButtonReceipt(seat, ownPlayerViewId, choosePlay,
                        oneText, twoText, enabledOne, enabledTwo,
                        promptCapture == null ? -1 : promptCapture.callbackOrdinal()));
                consume(expected);
                scenario.recordAction(new ActionReceipt(Decision.START_SEAT_ZERO, seat, ownPlayerViewId, null, choosePlay));
                queueAction(choosePlay ? controller::selectButtonOk : controller::selectButtonCancel);
                return;
            }
            if (isKeepPrompt(oneText, twoText)) {
                if (expected != null && (expected.decision() == Decision.PLAY_OR_DRAW
                        || expected.decision() == Decision.START_SEAT_ZERO)) {
                    consume(expected); // this seat was not chosen for the match's single Play/Draw question
                    expected = script.peekFirst();
                }
                if (scenario.mulliganScenario() && expected != null && expected.decision() == Decision.MULLIGAN_ANY) {
                    scenario.assertInitialShuffleConsumed();
                    Decision choice = scenario.chooseInitialMulligan(seat);
                    if (choice != Decision.REGULAR_MULLIGAN) {
                        fail("The guaranteed eligible seat did not receive its real free-choice dialog first");
                        return;
                    }
                    HandFacts hand = snapshotOwnHand();
                    if (promptCapture == null) { fail("Could not retain the actual regular Keep/Mulligan callback"); return; }
                    GameView currentView = getGameView();
                    scenario.recordMulliganChoice(new MulliganChoiceReceipt(choice, seat, ownPlayerViewId,
                            currentView == null ? -1 : currentView.getId(), -1,
                            "updateButtons", promptCapture.callbackOrdinal(),
                            hand.ids(), hand.landCount()));
                    consume(expected);
                    scenario.recordAction(new ActionReceipt(choice, seat, ownPlayerViewId, null, true));
                    queueAction(controller::selectButtonCancel); // explicit Regular London mulligan
                    return;
                }
                if (expected == null || expected.decision() != Decision.KEEP && expected.decision() != Decision.KEEP_WITHOUT_LAND) {
                    fail("Unexpected Keep/Mulligan prompt for seat " + seat);
                    return;
                }
                if (expected.decision() == Decision.KEEP_WITHOUT_LAND && promptCapture != null) {
                    HandFacts disconnectAiHand = false ? snapshotOwnHand() : null;

                    if (routeOpeningKeepAfterAppliedHand(promptCapture, owner, oneText, twoText,
                            enabledOne, enabledTwo, expected)) return;
                    if (scenario.metamorphoseScenario() || scenario.privacyScenario() || scenario.lifecycleScenario()) {
                        HandFacts hand = snapshotOwnHand();
                        boolean invalidOpeningKeep = hand.ids().size() != 7
                                || promptCapture.callbackOrdinal() <= 0 || seat < 0 || seat > 1;
                        scenario.recordOpeningKeep(seat, ownPlayerViewId, promptCapture.callbackOrdinal(),
                                game.getId(), "updateButtons", expected.decision().name(), hand.ids(),
                                scenario.metamorphoseScenario() && invalidOpeningKeep
                                        ? openingKeepSnapshotDiagnostic(promptCapture, game) : null);
                        if (scenario.lifecycleScenario()) {
                            recordLifecycleKeep(promptCapture, hand);
                        }
                    }

                }
                consume(expected);
                scenario.recordAction(new ActionReceipt(expected.decision(), seat, ownPlayerViewId, null, true));
                queueAction(controller::selectButtonOk); // explicit Keep choice
                return;
            }
            if (isYieldChoice(oneText, twoText)) {
                scenario.recordAction(new ActionReceipt(Decision.PASS_PRIORITY, seat, ownPlayerViewId, null, true));
                queueAction(controller::selectButtonCancel); // explicitly decline optional auto-yield suggestion
                return;
            }
            if (scenario.metamorphoseScenario() && seat == 0 && owner != null && owner.getId() == scenario.metamorphoseHostViewId() && scenario.metamorphoseStage() == 2 && localizer.getMessage("lblOK").equalsIgnoreCase(oneText) && localizer.getMessage("lblCancel").equalsIgnoreCase(twoText)) {
                SelectablesReceipt target = scenario.selectablesReceipts().stream()
                        .filter(receipt -> receipt.stage().equals("Metamorphose target")
                                && receipt.seat() == seat && receipt.chooserViewId() == ownPlayerViewId)
                        .findFirst().orElse(null);
                MetamorphoseInputButtonsReceipt acceptedButtons = scenario.metamorphoseInputButtons().stream()
                        .filter(receipt -> receipt.prompt().equals("target")
                                && receipt.seat() == seat && receipt.chooserViewId() == ownPlayerViewId)
                        .findFirst().orElse(null);
                PrivacyInputSnapshot currentInput = snapshotInput(owner);
                Game retainedGame = scenario.metamorphoseGame();
                PlayerView currentOwner = game == null ? null : findPlayerView(game, ownPlayerViewId);
                PlayerControllerHuman acceptedController = acceptedMetamorphoseTargetController
                        instanceof PlayerControllerHuman human ? human : null;
                boolean valid = promptCapture != null && target != null && acceptedButtons != null
                        && !enabledOne && enabledTwo
                        && currentInput != null && acceptedMetamorphoseTargetInput != null
                        && acceptedMetamorphoseTargetInput.proxyInput() instanceof InputSelectTargets acceptedTargets
                        && acceptedTargets.getOwner() == owner
                        && acceptedMetamorphoseTargetInput.proxyInput() == acceptedMetamorphoseTargetInput.queuedInput()
                        && currentInput.proxyInput() == acceptedMetamorphoseTargetInput.proxyInput()
                        && currentInput.queuedInput() == acceptedMetamorphoseTargetInput.queuedInput()
                        && target.minimum() == 1 && target.maximum() == 1
                        && target.cardIds().equals(List.of(scenario.metamorphoseTargetCardId()))
                        && target.callbackOrdinal() < acceptedButtons.promptOrdinal()
                        && acceptedButtons.promptOrdinal() < acceptedButtons.buttonsOrdinal()
                        && acceptedButtons.buttonsOrdinal() < promptCapture.callbackOrdinal()
                        && acceptedButtons.okLabel().equalsIgnoreCase(localizer.getMessage("lblOK"))
                        && acceptedButtons.cancelLabel().equalsIgnoreCase(localizer.getMessage("lblCancel"))
                        && !acceptedButtons.okEnabled() && acceptedButtons.cancelEnabled()
                        && acceptedMetamorphoseTargetGameViewId == gameViewId(game)
                        && acceptedMetamorphoseTargetControllerViewId == controllerGameViewId(controller)
                        && controller == acceptedMetamorphoseTargetController
                        && acceptedController != null && acceptedController.getGame() == retainedGame
                        && retainedGame != null && game.getGame() == retainedGame
                        && currentOwner == owner && retainedGame.getPlayers().size() == 2
                        && ownPlayerViewId == scenario.metamorphoseHostViewId()
                        && retainedGame.getPlayers().get(0).getId() == ownPlayerViewId
                        && retainedGame.getPlayers().get(0).getController() == acceptedController
                        && metamorphoseTargetActionsQueued.get() == 1
                        && metamorphoseTargetActionsDispatched.get() == 0
                        && metamorphoseTargetActionAccepted == null;
                if (valid) return; // Same accepted target input is still queued; do not enqueue another selection.
            }
            if (!isPriorityPrompt(oneText, twoText)) {
                String targetDiagnostic = scenario.metamorphoseScenario() && seat == 0
                        && localizer.getMessage("lblOK").equalsIgnoreCase(oneText)
                        && localizer.getMessage("lblCancel").equalsIgnoreCase(twoText)
                        ? metamorphoseTargetRepaintDiagnostic(owner, promptCapture, enabledOne, enabledTwo) : "";
                String remoteMetamorphoseDiagnostic = scenario.metamorphoseScenario() && seat == 1
                        ? scenario.metamorphoseRemoteRepaintDiagnostic(seat,
                                owner == null ? -1 : owner.getId(), gameViewId(getGameView()),
                                promptCapture == null ? -1 : promptCapture.callbackOrdinal(), enabledOne, enabledTwo)
                        : "";
                fail("Unexpected enabled prompt labels for seat " + seat + ": [" + oneText + ", " + twoText
                        + "]" + targetDiagnostic + remoteMetamorphoseDiagnostic);
                return;
            }
            if (scenario.metamorphoseScenario()) {
                if (promptCapture != null && scenario.holdAtFirstPriority(seat, game.getId(), promptCapture,
                        game.getTurn(), game.getPhase(), game.getPlayerTurn() == null ? -1 : game.getPlayerTurn().getId(),
                        () -> snapshotZones(new Object[] {game}))) return;
                if (scenario.metamorphoseUpkeepActive() && game.getTurn() == 1 && game.getPhase() == PhaseType.UNTAP && game.getStack().isEmpty() && game.getPlayerTurn() != null && game.getPlayerTurn().getId() == scenario.metamorphoseHostViewId()) {
                    if (!scenario.recordMetamorphoseUntapPass(seat, game.getTurn(), game.getPhase(),
                            game.getPlayerTurn().getId(), game.getStack().size())) return;
                    queueAction(controller::selectButtonOk);
                    return;
                }
                if (expected != null && expected.decision() == Decision.CAST_METAMORPHOSE && seat == 0 && scenario.metamorphoseStage() == 0 && game.getTurn() == 1 && game.getPhase() == PhaseType.MAIN1 && game.getStack().isEmpty() && game.getPlayerTurn() != null && game.getPlayerTurn().getId() == ownPlayerViewId) {
                    consume(expected);
                    CardView spell = findVisibleCard(game, scenario.metamorphoseHostCardId());
                    PlayerView own = findPlayerView(game, ownPlayerViewId);
                    if (spell == null || own == null || own.getHand() == null || own.getHand().stream().noneMatch(card -> card.getId() == spell.getId())) {
                        fail("The actual Metamorphose card is not in host's assigned hand at turn 1 MAIN1");
                        return;
                    }
                    if (!scenario.dispatchMetamorphoseCast(seat, ownPlayerViewId, game.getTurn(), game.getPhase(),
                            game.getPlayerTurn().getId(), game.getStack().size())) return;
                    queueAction(() -> {
                        if (!controller.selectCard(spell, null, null)) fail("Host controller rejected Metamorphose from its hand");
                    });
                    return;
                }
                if (scenario.metamorphoseUpkeepActive() && game.getTurn() == 1 && game.getPhase() == PhaseType.UPKEEP && game.getStack().isEmpty() && game.getPlayerTurn() != null && game.getPlayerTurn().getId() == scenario.metamorphoseHostViewId()) {
                    if (!scenario.recordMetamorphosePreCastPass(seat, game.getTurn(), game.getPhase(),
                            game.getPlayerTurn().getId(), game.getStack().size())) return;
                    queueAction(controller::selectButtonOk);
                    return;
                }
                if (expected != null && expected.decision() == Decision.CAST_METAMORPHOSE) {
                    fail("Metamorphose script reached an unexpected turn/phase before the host MAIN1 cast: turn="
                            + game.getTurn() + " phase=" + game.getPhase());
                    return;
                }
                if (!game.getStack().isEmpty() && scenario.metamorphoseStage() >= 2 && scenario.metamorphoseStage() < 7) {
                    StackItemView item = game.getStack().size() == 1 ? game.getStack().get(0) : null;
                    CardView source = item == null ? null : item.getSourceCard();
                    PlayerView activator = item == null ? null : item.getActivatingPlayer();
                    List<Integer> targetIds = item == null || item.getTargetCards() == null
                            ? List.of() : item.getTargetCards().stream().map(CardView::getId).toList();
                    if (promptCapture == null || !scenario.recordMetamorphoseStackPriority(seat,
                            ownPlayerViewId, promptCapture.callbackOrdinal(), game.getId(),
                            game.getTurn(), game.getPhase(),
                            game.getPlayerTurn() == null ? -1 : game.getPlayerTurn().getId(),
                            item == null ? -1 : item.getId(), source == null ? -1 : source.getId(),
                            activator == null ? -1 : activator.getId(), targetIds,
                            item != null && item.isAbility(), game.getStack().size())) return;
                    if (seat == 0 && scenario.holdMetamorphoseFirstStackPriority()) return;
                    queueAction(controller::selectButtonOk);
                    return;
                }
                if (scenario.metamorphoseStage() >= 6 && game.getStack().isEmpty() && game.getTurn() == 1 && game.getPhase() == PhaseType.MAIN1 && game.getPlayerTurn() != null && game.getPlayerTurn().getId() == ownPlayerViewId && promptCapture != null) {
                    scenario.recordMetamorphosePostResolutionPriority(promptCapture.callbackOrdinal(),
                            game.getTurn(), game.getPhase(), game.getPlayerTurn().getId(), game.getStack().size(),
                            snapshotZones(new Object[] {game}));
                }
                if (scenario.metamorphoseStage() >= 6 && game.getStack().isEmpty()) return;
                if (scenario.metamorphoseStage() > 0 && scenario.metamorphoseStage() < 6 && game.getStack().isEmpty()) {
                    fail("Metamorphose priority had an empty stack before its resolution receipt");
                    return;
                }
            }
            if (expected == null) {

                // The first normal priority callback follows both accepted Keeps. Leave that prompt
                // unresolved until the test finishes inspecting the retained post-Keep receipt.
                if (promptCapture != null && scenario.holdAtFirstPriority(seat, game.getId(), promptCapture, game.getTurn(), game.getPhase(),
                        game.getPlayerTurn() == null ? -1 : game.getPlayerTurn().getId(),
                        () -> snapshotZones(new Object[] {game}))) return;
                if (!scenario.recordPriorityPass(seat)) return;
                scenario.recordAction(new ActionReceipt(Decision.PASS_PRIORITY, seat, ownPlayerViewId, null, true));
                queueAction(controller::selectButtonOk);
                return;
            }
            if (expected.decision() == Decision.CONCEDE) {
                if (seat == 1 && scenario.bothLandsObserved() && game.getTurn() >= 4) {
                    consume(expected);
                    if (scenario.tryQueueConcession()) {
                        scenario.recordAction(new ActionReceipt(Decision.CONCEDE, seat, ownPlayerViewId, null, true));
                        queueAction(controller::concede);
                    }
                } else {
                    if (!scenario.recordPriorityPass(seat)) return;
                    scenario.recordAction(new ActionReceipt(Decision.PASS_PRIORITY, seat, ownPlayerViewId, null, true));
                    queueAction(controller::selectButtonOk);
                }
                return;
            }
            if (expected.decision() == Decision.PLAY_LAND && game.getPlayerTurn() != null && game.getPlayerTurn().getId() == ownPlayerViewId && game.getPhase() == PhaseType.MAIN1) {
                PlayerView ownPlayer = null;
                for (PlayerView player : game.getPlayers()) {
                    if (player.getId() == ownPlayerViewId) {
                        ownPlayer = player;
                        break;
                    }
                }
                if (ownPlayer == null) { fail("Assigned PlayerView is absent from the applied GameView for seat " + seat); return; }
                if (expected.cardId() == null && !bindQueuedLandCard(ownPlayer)) return;
                ExpectedAction bound = script.peekFirst();
                if (bound == null || bound.decision() != Decision.PLAY_LAND || bound.cardId() == null) {
                    fail("Queued land action was not bound at the assigned player's MAIN1 priority for seat " + seat);
                    return;
                }
                CardView card = findQueuedHandCard(ownPlayer, bound.cardId());
                if (card == null) { fail("Queued land card is not in assigned chooser's hand for seat " + seat); return; }
                pendingLandCardId = card.getId();
                scenario.expectLand(seat, card.getId());
                final int selectedCardId = card.getId();
                queueAction(() -> {
                    try {
                        if (!controller.selectCard(card, null, null)) fail("Controller rejected queued land card " + selectedCardId);
                    } catch (Throwable t) { scenario.fail(t); }
                });
                return;
            }
            if (!scenario.recordPriorityPass(seat)) return;
            scenario.recordAction(new ActionReceipt(Decision.PASS_PRIORITY, seat, ownPlayerViewId, null, true));
            queueAction(controller::selectButtonOk); // explicit pass at the current priority checkpoint
        }

        private boolean bindQueuedLandCard(PlayerView owner) {
            List<ExpectedAction> queuedActions;
            synchronized (script) { queuedActions = new ArrayList<>(script); }
            ExpectedAction queued = queuedActions.stream().filter(a -> a.decision() == Decision.PLAY_LAND).findFirst().orElse(null);
            if (queued == null) return false;
            if (queued.cardId() != null) return true;
            CardView chosen = null;
            if (owner.getHand() != null) {
                for (CardView card : owner.getHand()) {
                    if (card.getCurrentState().getType() != null && card.getCurrentState().getType().isLand() && (chosen == null || card.getId() < chosen.getId())) chosen = card;
                }
            }
            if (chosen == null) { fail("No legal land card available to queue for seat " + seat); return false; }
            synchronized (script) {
                script.clear();
                for (ExpectedAction action : queuedActions) {
                    script.addLast(action.equals(queued)
                            ? new ExpectedAction(queued.decision(), queued.seat(), chosen.getId()) : action);
                }
            }
            return true;
        }

        private CardView findQueuedHandCard(PlayerView owner, Integer expectedId) {
            if (owner.getHand() == null) return null;
            for (CardView card : owner.getHand()) {
                if (card.getCurrentState().getType() != null && card.getCurrentState().getType().isLand() && (expectedId == null || card.getId() == expectedId)) return card;
            }
            return null;
        }

        private void consume(ExpectedAction expected) { script.remove(expected); }
        private boolean isPlayDrawPrompt(String one, String two) {
            return "Play".equalsIgnoreCase(one) && "Draw".equalsIgnoreCase(two);
        }
        private boolean isLocalizedPlayDrawPrompt(String one, String two) {
            Localizer localizer = Localizer.getInstance();
            return localizer.getMessage("lblPlay").equalsIgnoreCase(one)
                    && localizer.getMessage("lblDraw").equalsIgnoreCase(two);
        }
        private boolean isKeepPrompt(String one, String two) {
            return "Keep".equalsIgnoreCase(one) && "Mulligan".equalsIgnoreCase(two);
        }
        private boolean isYieldChoice(String one, String two) {
            return "Accept".equalsIgnoreCase(one) && "Decline".equalsIgnoreCase(two);
        }
        private boolean isPriorityPrompt(String one, String two) {
            return "OK".equalsIgnoreCase(one)
                    && ("End Turn".equalsIgnoreCase(two) || two.startsWith("Undo (")
                            || Localizer.getInstance().getMessage("lblUndo").equalsIgnoreCase(two));
        }
    }

    private final String name;
    private final int expectedSeat;
    private final ScriptedGui gui;
    private final ClientGameLobby lobby = new ClientGameLobby();
    private final CountDownLatch assigned = new CountDownLatch(1);
    private final CountDownLatch readyAcknowledged = new CountDownLatch(1);
    private final ClientProtocolObservation clientProtocolObservation;
    private final FGameClient client;
    private volatile int slot = -1;

    ScriptedHumanTcpSupport(String name, int seat, String expectedPlayerName, int port, Scenario scenario) {
        this.name = name;
        expectedSeat = seat;
        gui = new ScriptedGui(seat, true, expectedPlayerName, scenario);
        boolean privacyConfirmationObservation = scenario.privacyScenario() && scenario.privacyFengrafSeat() == 1
                && expectedSeat == 1;
        boolean metamorphoseDiagnosticObservation = scenario.metamorphoseScenario() && expectedSeat == 1;
        clientProtocolObservation = scenario.staleActionWitness() || privacyConfirmationObservation
                || false
                || false
                || metamorphoseDiagnosticObservation
                ? new ClientProtocolObservation(scenario, privacyConfirmationObservation,
                        metamorphoseDiagnosticObservation) : null;
        if (privacyConfirmationObservation) scenario.attachPrivacyClientSendObservation(clientProtocolObservation);
        if (metamorphoseDiagnosticObservation) scenario.attachMetamorphoseClientSendObservation(clientProtocolObservation);
        scenario.addFailureHook(assigned::countDown);
        scenario.addFailureHook(readyAcknowledged::countDown);
        client = clientProtocolObservation == null ? new FGameClient(name, gui, "127.0.0.1", port)
                : new ObservedFGameClient(name, gui, "127.0.0.1", port, clientProtocolObservation);
        client.addLobbyListener(new ILobbyListener() {
            @Override public void update(GameLobbyData data, int assignedSlot) {
                lobby.setData(data);
                boolean readyInReceivedUpdate = lobby.getSlot(expectedSeat) != null
                        && lobby.getSlot(expectedSeat).getType() == forge.gamemodes.match.LobbySlotType.REMOTE
                        && lobby.getSlot(expectedSeat).isReady();
                if (assignedSlot >= 0) {
                    slot = assignedSlot;
                    lobby.setLocalPlayer(assignedSlot);
                    assigned.countDown();
                    if (assignedSlot != expectedSeat) gui.scenario.fail(new AssertionError("Remote assigned to unexpected lobby slot " + assignedSlot));
                }
                if (readyInReceivedUpdate) readyAcknowledged.countDown();
            }
            @Override public void message(String source, String message, ChatMessage.MessageType type) { }
            @Override public void close() { }
            @Override public ClientGameLobby getLobby() { return lobby;
        }
        });
    }

    void connect() { client.connect(); }
    Channel channel() throws ReflectiveOperationException {
        Field field = FGameClient.class.getDeclaredField("channel");
        field.setAccessible(true);
        return (Channel) field.get(client);
        }

    String clientUsername() { return client.getUsername();
        }

    void armSelectButtonOkObservation(int gameViewId, long promptCallbackOrdinal) {
        if (expectedSeat != 1 || clientProtocolObservation == null) {
            throw new AssertionError("Only remote B in the stale-action fixture can arm this observation");
        }
        clientProtocolObservation.armSelectButtonOk(gameViewId, promptCallbackOrdinal);
    }

    void armClientProtocolObservation(ProtocolMethod method, NextGameDecision decision,
                                      int gameViewId, long promptCallbackOrdinal) {
        if (expectedSeat != 1 || clientProtocolObservation == null || !gui.scenario.staleActionWitness()) {
            throw new AssertionError("Only remote B in the stale-action fixture can arm this protocol observation");
        }
        clientProtocolObservation.arm(method, decision, gameViewId, promptCallbackOrdinal);
    }

    ClientProtocolSendReceipt clientProtocolSendReceipt() {
        return clientProtocolObservation == null ? null : clientProtocolObservation.receipt();
        }

    ClientProtocolSendReceipt clientSelectButtonOkReceipt() {
        return clientProtocolSendReceipt();
        }
    Throwable clientProtocolObservationFailure() {
        return clientProtocolObservation == null ? null : clientProtocolObservation.failure();
        }
    void disarmClientSelectButtonOkObservation() {
        disarmClientProtocolObservation();
    }
    void disarmClientProtocolObservation() {
        if (clientProtocolObservation != null) clientProtocolObservation.disarm();
    }

    static Channel serverChannel(RemoteClient client) throws ReflectiveOperationException {
        Field field = RemoteClient.class.getDeclaredField("channel");
        field.setAccessible(true);
        return (Channel) field.get(client);
        }

    private static final class ObservedFGameClient extends FGameClient {
        private final ClientProtocolObservation observation;

        private ObservedFGameClient(String username, IGuiGame clientGui, String hostname, int port,
                                    ClientProtocolObservation observation) {
            super(username, clientGui, hostname, port);
            this.observation = observation;
        }

        @Override public void send(NetEvent event) {
            observation.observe(event);
            super.send(event);
        }
    }
    void installReconnectObservation(ReconnectObservation observation, long deadlineNanos)
            throws Exception {
        Channel connected = channel();
        if (connected == null || !connected.isActive()) throw new AssertionError("Replacement TCP channel is not active");
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new TimeoutException("Reconnect tap deadline expired before installation");
        connected.eventLoop().submit(() -> observation.install(connected)).get(remaining, TimeUnit.NANOSECONDS);
    }
    boolean awaitAssigned(long timeout, TimeUnit unit) throws InterruptedException { return assigned.await(timeout, unit);
        }
    boolean awaitReadyAcknowledged(long timeout, TimeUnit unit) throws InterruptedException { return readyAcknowledged.await(timeout, unit);
        }
    ScriptedGui gui() { return gui;
        }
    int assignedSlot() { return slot;
        }

    void ready() {
        if (slot != expectedSeat) throw new IllegalStateException(name + " has no expected assigned seat");
        // Keep this as a server-confirmed action: mutating ClientGameLobby here can
        // make an in-flight initial lobby callback look like a ready acknowledgement.
        client.send(forge.gamemodes.net.event.UpdateLobbyPlayerEvent.isReadyUpdate(true));
    }

    @Override public void close() { client.close(); }
}

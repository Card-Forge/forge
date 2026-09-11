package forge.gamemodes.net.draft;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.DraftAction;
import forge.gamemodes.limited.DraftPrompt;
import forge.gamemodes.limited.IDraftLog;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.EventPhase;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.limited.DraftPack;
import forge.gamemodes.limited.LimitedPlayer;
import forge.gamemodes.limited.LimitedPlayerAI;
import forge.gamemodes.net.event.DraftAutoPickedEvent;
import forge.gamemodes.net.event.DraftLogEvent;
import forge.gamemodes.net.event.DraftPackArrivedEvent;
import forge.gamemodes.net.event.DraftPromptEvent;
import forge.gamemodes.net.event.DraftSeatPickedEvent;
import forge.gamemodes.net.event.DraftSeatStateEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.event.ReceiveEventPoolEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.item.PaperCard;
import forge.util.IHasForgeLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Server-side adapter that wraps {@link BoosterDraft} for network play.
 *
 * <p>Async model: each seat has its own pack queue. When a seat picks, the
 * picked-from pack is passed to the next seat in the pass direction immediately,
 * regardless of what other seats are doing. A fast picker may bank up multiple
 * packs while waiting for slower seats. Each human pick has its own timer,
 * reset whenever a new pack reaches the head of the queue.
 *
 * <p>Mutable state is guarded by {@code synchronized(this)}. Each entry point runs
 * one step and sends that step's events at its end while it still holds the monitor,
 * so events reach every client in the order the steps ran. {@code RemoteClient.send}
 * queues the write and never waits, so a slow client cannot stall the pod.
 */
public final class BoosterDraftHost implements IHasForgeLog {

    /**
     * Per-seat connection state. A disconnected seat enters {@code IN_GRACE} for
     * {@link NetworkEvent#getDisconnectGraceSeconds()}; if no reconnect happens in
     * that window it transitions to {@code POST_GRACE_AUTO}, where the seat auto-picks
     * each pack and takes the default answer to each prompt. Reconnect at any point returns the seat to
     * {@code LIVE}. A grace value of zero skips IN_GRACE entirely.
     */
    private enum SeatConnectionState { LIVE, IN_GRACE, POST_GRACE_AUTO }

    private record PendingPrompt(DraftPrompt prompt, boolean blocking, Consumer<List<Integer>> onAnswer) {
        int seat() {
            return prompt.seatIndex();
        }
    }

    private final BoosterDraft draft;
    private final NetworkEvent event;
    private final List<EventParticipant> participants;
    private int currentPackNumber;  // 1-based round number — used to decide pass direction
    private int initialPackSize;    // pack size at start of current round, for pick-number display
    private volatile boolean finished;

    /** Whether a human seat currently has a pack notification in flight (waiting for pick). */
    private final boolean[] inFlight;

    /** Total picks each seat has committed so far (1-based pick number after each call). */
    private final int[] picksMadePerSeat;

    /** Per-seat connection state — initialized LIVE; transitioned by disconnect/reconnect. */
    private final SeatConnectionState[] seatState;

    /** Per-seat pick timers. Started when a pack is sent, cancelled on pick. */
    private final Map<Integer, ScheduledFuture<?>> seatTimers = new HashMap<>();

    /** Per-seat grace timers — scheduled on disconnect, cancelled on reconnect. */
    private final Map<Integer, ScheduledFuture<?>> graceTimers = new HashMap<>();

    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DraftPickTimer");
        t.setDaemon(true);
        return t;
    });

    private final int[] seq;
    /** The arrival number of each seat's pack in flight; a pick must echo it. */
    private final int[] inFlightSeq;
    private final List<List<NetEvent>> queuedPrivate = new ArrayList<>();
    private final Map<Integer, PendingPrompt> pendingPrompts = new LinkedHashMap<>();
    /** Prompt timers, by prompt id; a seat can have several timed prompts at once. */
    private final Map<Integer, ScheduledFuture<?>> promptTimers = new HashMap<>();
    private int nextPromptId = 1;
    private boolean finishing;
    /** Dispatches of the step in progress; only touched under the monitor. */
    private List<Runnable> step;

    public BoosterDraftHost(BoosterDraft draft, NetworkEvent event) {
        this.draft = draft;
        this.event = event;
        // Snapshot participants so a lobby-side repopulate after draft start
        // can't corrupt the host's running pod.
        this.participants = new ArrayList<>(event.getParticipants());
        this.currentPackNumber = draft.getRound();
        this.finished = false;
        int podSize = draft.getAllPlayers().size();
        this.inFlight = new boolean[podSize];
        this.picksMadePerSeat = new int[podSize];
        this.seatState = new SeatConnectionState[podSize];
        Arrays.fill(this.seatState, SeatConnectionState.LIVE);
        this.seq = new int[podSize];
        this.inFlightSeq = new int[podSize];
        List<LimitedPlayer> players = draft.getAllPlayers();
        for (int i = 0; i < podSize; i++) {
            queuedPrivate.add(new ArrayList<>());
            EventParticipant participant = EventParticipant.findBySeat(participants, i);
            if (participant != null) {
                players.get(i).setName(participant.getName());
            }
            if (!(players.get(i) instanceof LimitedPlayerAI)) {
                final int seat = i;
                players.get(i).setPromptSink((prompt, blocking, onAnswer) -> onPromptRaised(seat, prompt, blocking, onAnswer));
            }
        }
        draft.setLogEntry(new IDraftLog() {
            @Override
            public void addLogEntry(String message) {
                addLogEntry(message, null);
            }

            @Override
            public void addLogEntry(String message, PaperCard card) {
                step.add(() -> FServerManager.getInstance().broadcast(new DraftLogEvent(-1, message, card)));
            }

            @Override
            public void addPrivateLogEntry(int seatIndex, String message, PaperCard card) {
                addSendToSeat(seatIndex, new DraftLogEvent(seatIndex, message, card));
            }
        });
    }

    /**
     * Start the draft: set phase and distribute initial packs.
     * Called once after the BoosterDraft has been initialized.
     */
    public void start() {
        synchronized (this) {
            long humans = participants.stream().filter(EventParticipant::isHuman).count();
            netLog.info("Draft started — {} humans, {} seats total, timer={}s, product={}",
                    humans, participants.size(), event.getPickTimerSeconds(), event.getProductDescription());
            event.setPhase(EventPhase.DRAFTING);
            captureInitialPackSize();
            beginStep();
            // Before any AI pick logs a line, so each client creates its draft editor first
            for (int i = 0; i < seatState.length; i++) {
                if (!isAiSeat(i)) {
                    addSendToSeat(i, fullState(i));
                }
            }
            advanceDraft();
            endStepAndDispatch();
        }
    }

    /** True once the draft has ended, whether it completed or was shut down early. */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Stop the draft and release timer resources. Safe to call multiple times.
     * Does not distribute pools — call before the draft has legitimately finished
     * (e.g. host cleared the event mid-draft, lobby shutting down).
     */
    public synchronized void shutdown() {
        finished = true;
        for (Integer seatIndex : new ArrayList<>(seatTimers.keySet())) {
            cancelSeatTimer(seatIndex);
        }
        for (ScheduledFuture<?> f : graceTimers.values()) {
            if (f != null) f.cancel(false);
        }
        graceTimers.clear();
        for (ScheduledFuture<?> f : promptTimers.values()) {
            f.cancel(false);
        }
        promptTimers.clear();
        pendingPrompts.clear();
        timerExecutor.shutdown();
    }

    private void beginStep() {
        step = new ArrayList<>();
    }

    private void endStepAndDispatch() {
        List<LimitedPlayer> players = draft.getAllPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (!isAiSeat(i) && players.get(i).hasStateChanged()) {
                addSendToSeat(i, deltaState(i, shownPack(i)));
            }
        }
        // Pools go last, so no seat state follows a pool and reopens that seat's draft screen
        if (finishing && !finished && pendingPrompts.isEmpty()) {
            addFinishDraft();
        }
        List<Runnable> dispatches = step;
        step = null;
        for (Runnable r : dispatches) r.run();
    }

    /**
     * Handle an incoming pick from a human client.
     *
     * @param seatIndex the seat that made the pick
     * @param pickSeq   the arrival number of the pack the pick was made from
     * @param card      the chosen card; ignored for a hidden pack
     * @param variant   the chosen draft ability, or null
     */
    public void handlePick(int seatIndex, int pickSeq, PaperCard card, DraftAction variant) {
        synchronized (this) {
            if (finished || finishing || seatIndex < 0 || seatIndex >= seatState.length) return;
            // A late click after an auto-pick, or on an earlier arrival of a held pack
            if (!inFlight[seatIndex] || pickSeq != inFlightSeq[seatIndex]) return;
            LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
            DraftPack head = player.nextChoice();
            if (head == null) return;
            boolean valid = player.isPackHidden()
                    ? variant == null
                    : head.contains(card) && (variant == null
                            || (variant.isPickFor(card) && player.getActions(head).contains(variant)));
            beginStep();
            if (valid) {
                cancelSeatTimer(seatIndex);
                inFlight[seatIndex] = false;
                applyPickAndPass(player, seatIndex, card, variant);
                netLog.info("Seat {} picked from pack {}", seatIndex, currentPackNumber);
                addBroadcastSeatPicked(seatIndex);
                advanceDraft();
            } else {
                // Same arrival and timer, so an invalid pick cannot buy the seat more time
                netLog.warn("Seat {} sent an invalid pick; re-sending the pack", seatIndex);
                addSendPack(seatIndex, head);
            }
            endStepAndDispatch();
        }
    }

    public void handleActivate(int seatIndex, DraftAction action) {
        synchronized (this) {
            if (finished || finishing || seatIndex < 0 || seatIndex >= seatState.length) return;
            LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
            if (action == null || action.kind() != DraftAction.Kind.POOL
                    || !player.getActions(player.nextChoice()).contains(action)) return;
            beginStep();
            player.activate(action);
            advanceDraft();
            endStepAndDispatch();
        }
    }

    public void handlePromptResponse(int seatIndex, int promptId, List<Integer> chosen) {
        synchronized (this) {
            if (finished) return;
            PendingPrompt pending = pendingPrompts.get(promptId);
            if (pending == null || pending.seat() != seatIndex || !pending.prompt().isValidAnswer(chosen)) return;
            beginStep();
            resolvePrompt(pending, chosen);
            advanceDraft();
            endStepAndDispatch();
        }
    }

    /**
     * Apply a pick and, unless the picker keeps the pack, dequeue it from their
     * queue and route it to the next seat in direction. A pack is kept either
     * because a Conspiracy card such as Agent of Acquisitions made {@code draftCard}
     * return {@code false}, or because the pick rule grants a second card from it.
     * An emptied pack is never kept: {@code isRoundOver} tests for an empty queue, so
     * retaining one would stall the round.
     */
    private void applyPickAndPass(LimitedPlayer player, int seatIndex, PaperCard card, DraftAction variant) {
        Boolean passPack = player.draftCard(card, DeckSection.Sideboard, variant);
        picksMadePerSeat[seatIndex]++;
        if (Boolean.TRUE.equals(passPack)) {
            passUnlessKept(seatIndex);
        }
    }

    /** Core distribution loop: advance rounds, let AI pick, and act for each human seat in turn. */
    private void advanceDraft() {
        while (!finished && !finishing) {
            if (draft.isRoundOver() && noSeatBlocked()) {
                if (!draft.startRound()) {
                    beginFinishing();
                    return;
                }
                currentPackNumber = draft.getRound();
                captureInitialPackSize();
            }
            if (stepAiSeat() || stepHumanSeats()) {
                continue;
            }
            return;
        }
    }

    // A blocked seat may be about to add a Lore Seeker pack to this round
    private boolean noSeatBlocked() {
        return draft.getAllPlayers().stream().noneMatch(LimitedPlayer::isBlocked);
    }

    /** Let one AI seat with a pack act, so the loop re-checks state after each pick. */
    private boolean stepAiSeat() {
        List<LimitedPlayer> players = draft.getAllPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (!(players.get(i) instanceof LimitedPlayerAI ai)) continue;
            DraftPack head = ai.nextChoice();
            if (head == null) continue;
            if (head.isEmpty()) {
                ai.passPack();
                return true;
            }
            if (ai.shouldSkipThisPick()) {
                skipHead(i);
                return true;
            }
            Boolean passed = ai.draftNext();
            if (passed == null) continue;
            picksMadePerSeat[i]++;
            if (passed) {
                passUnlessKept(i);
            }
            addBroadcastSeatPicked(i);
            return true;
        }
        return false;
    }

    /**
     * The one decision point for each human seat: a blocked seat waits, a skipping seat
     * passes, a seat past grace auto-picks, and a live seat is sent its head pack.
     * Seats in IN_GRACE hold their packs silently until they reconnect or grace expires.
     */
    private boolean stepHumanSeats() {
        List<LimitedPlayer> players = draft.getAllPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (isAiSeat(i) || inFlight[i]) continue;
            LimitedPlayer p = players.get(i);
            DraftPack head = p.nextChoice();
            if (head == null || p.isBlocked() || p.isPromptHeld(head)) continue;
            if (head.isEmpty()) {
                p.passPack();
                return true;
            }
            if (p.shouldSkipThisPick()) {
                skipHead(i);
                return true;
            }
            switch (seatState[i]) {
                case POST_GRACE_AUTO -> {
                    netLog.info("Seat {} disconnected past grace — auto-picking", i);
                    applyPickAndPass(p, i, head.get(0), null);
                    addBroadcastSeatPicked(i);
                    return true;
                }
                case LIVE -> {
                    addSendPackToHuman(i, head);
                    inFlight[i] = true;
                    startSeatTimer(i);
                }
                case IN_GRACE -> { }
            }
        }
        return false;
    }

    /** Pass the head after a pick unless the pick rule grants the picker another card from it. */
    private void passUnlessKept(int seatIndex) {
        LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
        DraftPack head = player.nextChoice();
        if (head == null || head.isEmpty() || !draft.keepsPackAfterPick(player)) {
            passHead(seatIndex);
        }
    }

    /**
     * Pass the seat's head pack in the current direction (odd packs go right, even
     * packs go left), routing a last card to a Canal Dredger seat.
     */
    private void passHead(int seatIndex) {
        LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
        DraftPack head = player.nextChoice();
        if (head == null) {
            return;
        }
        if (!head.isEmpty()) {
            draft.routeLastCard(player, head);
            if (player.isPromptHeld(head)) {
                return;
            }
        }
        DraftPack passed = player.passPack();
        if (passed.isEmpty()) {
            return;
        }
        LimitedPlayer to = passed.getDestination();
        passed.setDestination(null);
        (to != null ? to : draft.getNeighbor(player, currentPackNumber % 2 == 1)).receiveOpenedPack(passed);
    }

    private void skipHead(int seatIndex) {
        LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
        DraftPack head = player.nextChoice();
        passHead(seatIndex);
        player.consumeSkip(head);
    }

    private void captureInitialPackSize() {
        for (LimitedPlayer pl : draft.getAllPlayers()) {
            DraftPack pack = pl.nextChoice();
            if (pack != null && !pack.isEmpty()) {
                initialPackSize = pack.size();
                return;
            }
        }
    }

    /** Pick number (0-based) within the current pack, derived from cards remaining. */
    private int pickNumberFor(DraftPack pack) {
        return pack == null ? 0 : Math.max(0, initialPackSize - pack.size());
    }

    private void addSendToSeat(int seatIndex, NetEvent netEvent) {
        EventParticipant participant = EventParticipant.findBySeat(participants, seatIndex);
        if (participant == null || participant.isAI()) {
            return;
        }
        if (seatState[seatIndex] != SeatConnectionState.LIVE) {
            // Private lines and peeks carry paid-for information; everything else is rebuilt on reconnect
            if (netEvent instanceof DraftLogEvent
                    || (netEvent instanceof DraftPromptEvent prompt && prompt.getPrompt().isInfoOnly())) {
                queuedPrivate.get(seatIndex).add(netEvent);
            }
            return;
        }
        int slot = participant.getLobbySlotIndex();
        step.add(() -> FServerManager.getInstance().sendToSlot(slot, netEvent));
    }

    private DraftPack shownPack(int seatIndex) {
        return inFlight[seatIndex] ? draft.getAllPlayers().get(seatIndex).nextChoice() : null;
    }

    // Pick offers only for the pack on the seat's screen; a blocked seat's next pack is not shown yet
    private List<DraftAction> actionsFor(int seatIndex, DraftPack shown) {
        return finishing ? List.of() : draft.getAllPlayers().get(seatIndex).getActions(shown);
    }

    private DraftSeatStateEvent fullState(int seatIndex) {
        LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
        player.drainPoolDelta();
        return new DraftSeatStateEvent(seatIndex, true, player.getPoolCards(), List.of(),
                actionsFor(seatIndex, shownPack(seatIndex)));
    }

    private DraftSeatStateEvent deltaState(int seatIndex, DraftPack shown) {
        LimitedPlayer.PoolDelta delta = draft.getAllPlayers().get(seatIndex).drainPoolDelta();
        return new DraftSeatStateEvent(seatIndex, false, delta.added(), delta.removed(), actionsFor(seatIndex, shown));
    }

    private void addSendPackToHuman(int seatIndex, DraftPack pack) {
        addSendToSeat(seatIndex, deltaState(seatIndex, pack));
        inFlightSeq[seatIndex] = ++seq[seatIndex];
        addSendPack(seatIndex, pack);
    }

    private void addSendPack(int seatIndex, DraftPack pack) {
        boolean hidden = draft.getAllPlayers().get(seatIndex).isPackHidden();
        List<PaperCard> cards = hidden ? List.of() : new ArrayList<>(pack);
        addSendToSeat(seatIndex, new DraftPackArrivedEvent(seatIndex, cards, currentPackNumber,
                pickNumberFor(pack), event.getPickTimerSeconds(), inFlightSeq[seatIndex], hidden ? pack.size() : 0));
    }

    private void addBroadcastSeatPicked(int seatIndex) {
        int[] queueDepths = computeQueueDepths();
        DraftSeatPickedEvent picked = new DraftSeatPickedEvent(seatIndex, queueDepths, draft.getAllPlayers().stream()
                .map(LimitedPlayer::getFaceUp).collect(Collectors.toList()));
        step.add(() -> FServerManager.getInstance().broadcast(picked));
    }

    private int computePickInPack(int seatPickCount) {
        int size = Math.max(1, initialPackSize);
        return ((seatPickCount - 1) % size) + 1;
    }

    private int[] computeQueueDepths() {
        List<LimitedPlayer> players = draft.getAllPlayers();
        int[] depths = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            depths[i] = players.get(i).getPackQueueSize();
        }
        return depths;
    }

    private void onPromptRaised(int seat, DraftPrompt prompt, boolean blocking, Consumer<List<Integer>> onAnswer) {
        if (prompt.isInfoOnly()) {
            addSendToSeat(seat, new DraftPromptEvent(prompt));
            return;
        }
        if (seatState[seat] == SeatConnectionState.POST_GRACE_AUTO) {
            onAnswer.accept(prompt.defaultAnswer());
            return;
        }
        PendingPrompt pending = new PendingPrompt(prompt.withPromptId(nextPromptId++), blocking, onAnswer);
        pendingPrompts.put(pending.prompt().promptId(), pending);
        if (seatState[seat] == SeatConnectionState.LIVE) {
            sendPrompt(pending);
        }
    }

    private void sendPrompt(PendingPrompt pending) {
        addSendToSeat(pending.seat(), new DraftPromptEvent(pending.prompt()));
        if (pending.blocking() || finishing) {
            startPromptTimer(pending);
        }
    }

    private void startPromptTimer(PendingPrompt pending) {
        int seconds = event.getPickTimerSeconds();
        if (seconds <= 0) return;
        int promptId = pending.prompt().promptId();
        cancelPromptTimer(promptId);
        promptTimers.put(promptId, timerExecutor.schedule(() -> onPromptTimerExpired(promptId), seconds, TimeUnit.SECONDS));
    }

    private void cancelPromptTimer(int promptId) {
        ScheduledFuture<?> f = promptTimers.remove(promptId);
        if (f != null) f.cancel(false);
    }

    private void resolvePromptsFor(int seatIndex) {
        for (PendingPrompt pending : new ArrayList<>(pendingPrompts.values())) {
            if (pending.seat() == seatIndex) {
                resolvePrompt(pending, pending.prompt().defaultAnswer());
            }
        }
    }

    private void onPromptTimerExpired(int promptId) {
        synchronized (this) {
            if (finished) return;
            PendingPrompt pending = pendingPrompts.get(promptId);
            if (pending == null || seatState[pending.seat()] != SeatConnectionState.LIVE) return;
            beginStep();
            addSendToSeat(pending.seat(), new DraftLogEvent(pending.seat(),
                    "Time ran out; the default was chosen for: " + pending.prompt().message(), null));
            resolvePrompt(pending, pending.prompt().defaultAnswer());
            advanceDraft();
            endStepAndDispatch();
        }
    }

    private void resolvePrompt(PendingPrompt pending, List<Integer> answer) {
        int promptId = pending.prompt().promptId();
        pendingPrompts.remove(promptId);
        cancelPromptTimer(promptId);
        pending.onAnswer().accept(answer);
        releaseHeldPack(pending.seat());
    }

    /** Pass a pack that waited on this seat's answer, unless an extra pick or whole-pack draft still holds it. */
    private void releaseHeldPack(int seatIndex) {
        LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
        DraftPack head = player.nextChoice();
        if (head == null || player.isBlocked() || !player.isPromptHeld(head)) return;
        player.clearPromptHold(head);
        if (!player.holdsPack(head)) {
            passUnlessKept(seatIndex);
        }
    }

    private void beginFinishing() {
        finishing = true;
        // A default answer can raise the next prompt of a chain, such as the Regicide colours
        PendingPrompt open;
        while ((open = firstNonBlockingPrompt()) != null) {
            resolvePrompt(open, open.prompt().defaultAnswer());
        }
        // No draft ability applies once the last pick is made, so every seat's offers are cleared
        for (int i = 0; i < seatState.length; i++) {
            if (!isAiSeat(i)) {
                addSendToSeat(i, deltaState(i, null));
            }
        }
        draft.postDraftActions();
    }

    private PendingPrompt firstNonBlockingPrompt() {
        return pendingPrompts.values().stream().filter(p -> !p.blocking()).findFirst().orElse(null);
    }

    /** Queue each human seat's pool; the step sends them after its seat-state flushes. */
    private void addFinishDraft() {
        netLog.info("Draft complete — distributing pools");
        List<LimitedPlayer> players = draft.getAllPlayers();
        String eventId = event.getEventId();

        for (int i = 0; i < players.size(); i++) {
            if (isAiSeat(i)) continue;
            LimitedPlayer player = players.get(i);
            Deck pool = new Deck(player.getDeck(), NetworkEvent.poolNameFor(event));
            pool.setDraftNotes(player.getSerializedDraftNotes());
            NetworkEvent.setEventTags(pool, event);
            addSendToSeat(i, new ReceiveEventPoolEvent(eventId, pool));
        }
        shutdown();
    }

    private void startSeatTimer(int seatIndex) {
        cancelSeatTimer(seatIndex);
        int seconds = event.getPickTimerSeconds();
        if (seconds <= 0) return;
        int arrival = inFlightSeq[seatIndex];
        ScheduledFuture<?> f = timerExecutor.schedule(
                () -> onSeatTimerExpired(seatIndex, arrival), seconds, TimeUnit.SECONDS);
        seatTimers.put(seatIndex, f);
    }

    private void cancelSeatTimer(int seatIndex) {
        ScheduledFuture<?> f = seatTimers.remove(seatIndex);
        if (f != null) f.cancel(false);
    }

    // --- Disconnect / reconnect handling ---

    /**
     * Notify the host that a drafting client's channel has been lost. The seat's
     * pick timer is cancelled and a grace window opens (duration from
     * {@link NetworkEvent#getDisconnectGraceSeconds()}); if no reconnect arrives
     * the seat switches to permanent auto-pick mode until the draft ends or the
     * player returns.
     *
     * <p>No-op for AI seats (not tied to channels) and for seats not currently
     * {@code LIVE} (idempotent against repeated disconnect signals).
     */
    public void onSeatDisconnected(int seatIndex) {
        synchronized (this) {
            if (finished || seatIndex < 0 || seatIndex >= seatState.length) return;
            if (seatState[seatIndex] != SeatConnectionState.LIVE) return;
            if (isAiSeat(seatIndex)) return;

            beginStep();
            cancelSeatTimer(seatIndex);
            pendingPrompts.values().stream().filter(p -> p.seat() == seatIndex)
                    .forEach(p -> cancelPromptTimer(p.prompt().promptId()));
            // Clear inFlight so the live-pack-distribution loop in advanceDraft
            // will re-send the current pack if the player reconnects before grace.
            inFlight[seatIndex] = false;

            int graceSeconds = event.getDisconnectGraceSeconds();
            addBroadcastDisconnect(seatIndex);
            if (graceSeconds > 0) {
                seatState[seatIndex] = SeatConnectionState.IN_GRACE;
                graceTimers.put(seatIndex, timerExecutor.schedule(
                        () -> onGraceExpired(seatIndex), graceSeconds, TimeUnit.SECONDS));
                netLog.info("Seat {} disconnected — {}s grace started", seatIndex, graceSeconds);
            } else {
                // Zero-grace config — skip IN_GRACE and start auto-picking immediately.
                seatState[seatIndex] = SeatConnectionState.POST_GRACE_AUTO;
                netLog.info("Seat {} disconnected — grace disabled, auto-picking immediately", seatIndex);
                resolvePromptsFor(seatIndex);
                advanceDraft();
            }
            endStepAndDispatch();
        }
    }

    /**
     * Notify the host that a previously-disconnected seat has reconnected. The seat
     * gets a full snapshot of its pool and offers, then its queued private lines and
     * pending prompts; its head pack follows through the normal {@code advanceDraft} path.
     */
    public void onSeatReconnected(int seatIndex) {
        synchronized (this) {
            if (finished || seatIndex < 0 || seatIndex >= seatState.length) return;
            if (seatState[seatIndex] == SeatConnectionState.LIVE) return;
            beginStep();
            cancelGraceTimer(seatIndex);
            seatState[seatIndex] = SeatConnectionState.LIVE;
            // Mobile rebuilds its draft screen on reconnect, so the pool goes as a full snapshot
            addSendToSeat(seatIndex, fullState(seatIndex));
            for (NetEvent queued : queuedPrivate.get(seatIndex)) {
                addSendToSeat(seatIndex, queued);
            }
            queuedPrivate.get(seatIndex).clear();
            for (PendingPrompt pending : pendingPrompts.values()) {
                if (pending.seat() == seatIndex) {
                    sendPrompt(pending);
                }
            }
            addBroadcastReconnect(seatIndex);
            netLog.info("Seat {} reconnected", seatIndex);
            advanceDraft();
            endStepAndDispatch();
        }
    }

    /** Grace timer callback — transition to POST_GRACE_AUTO and drain held/queued packs. */
    private void onGraceExpired(int seatIndex) {
        synchronized (this) {
            if (finished || seatState[seatIndex] != SeatConnectionState.IN_GRACE) return;
            beginStep();
            seatState[seatIndex] = SeatConnectionState.POST_GRACE_AUTO;
            graceTimers.remove(seatIndex);

            netLog.info("Seat {} grace expired — switching to auto-pick", seatIndex);
            addBroadcastGraceExpired(seatIndex);
            resolvePromptsFor(seatIndex);
            advanceDraft();
            endStepAndDispatch();
        }
    }

    private boolean isAiSeat(int seatIndex) {
        EventParticipant p = EventParticipant.findBySeat(participants, seatIndex);
        return p == null || p.isAI();
    }

    private void cancelGraceTimer(int seatIndex) {
        ScheduledFuture<?> f = graceTimers.remove(seatIndex);
        if (f != null) f.cancel(false);
    }

    private void addBroadcastDisconnect(int seatIndex) {
        EventParticipant participant = EventParticipant.findBySeat(participants, seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        int graceSeconds = event.getDisconnectGraceSeconds();
        String msg = graceSeconds > 0
                ? String.format("%s disconnected from draft — %ds to reconnect before auto-picking starts.",
                        name, graceSeconds)
                : String.format("%s disconnected from draft — auto-picking remaining packs.", name);
        step.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(msg)));
    }

    private void addBroadcastReconnect(int seatIndex) {
        EventParticipant participant = EventParticipant.findBySeat(participants, seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        step.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(
                String.format("%s reconnected — picking live again.", name))));
    }

    private void addBroadcastGraceExpired(int seatIndex) {
        EventParticipant participant = EventParticipant.findBySeat(participants, seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        step.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(
                String.format("%s grace period expired — auto-picking remaining packs.", name))));
    }

    /** Auto-pick the first card for a single seat that timed out. */
    private void onSeatTimerExpired(int seatIndex, int arrival) {
        synchronized (this) {
            // Defensive: a disconnected seat's pick timer is cancelled on disconnect,
            // but guard against the runnable firing between cancel scheduling and the
            // monitor being acquired.
            if (finished || !inFlight[seatIndex] || inFlightSeq[seatIndex] != arrival
                    || seatState[seatIndex] != SeatConnectionState.LIVE) return;

            List<LimitedPlayer> players = draft.getAllPlayers();
            LimitedPlayer player = players.get(seatIndex);
            DraftPack pack = player.nextChoice();
            if (pack == null || pack.isEmpty()) return;

            netLog.info("Pick timer expired for seat {} — auto-picking", seatIndex);

            beginStep();
            // A hidden pack draws at random, so the card drafted may not be the first one
            applyPickAndPass(player, seatIndex, pack.get(0), null);
            inFlight[seatIndex] = false;

            // Auto-pick first so client's pending-self cache is set before the echo flushes it.
            addNotifyAutoPick(seatIndex, player.getLastPick());
            addBroadcastSeatPicked(seatIndex);
            advanceDraft();
            endStepAndDispatch();
        }
    }

    private void addNotifyAutoPick(int seatIndex, PaperCard card) {
        int packNumber = currentPackNumber;
        int pickInPack = computePickInPack(picksMadePerSeat[seatIndex]);
        addSendToSeat(seatIndex, new DraftAutoPickedEvent(seatIndex, card, packNumber, pickInPack));
    }
}

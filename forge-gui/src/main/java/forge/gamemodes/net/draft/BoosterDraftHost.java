package forge.gamemodes.net.draft;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.EventPhase;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.limited.DraftPack;
import forge.gamemodes.limited.LimitedPlayer;
import forge.gamemodes.limited.LimitedPlayerAI;
import forge.gamemodes.net.event.DraftAutoPickedEvent;
import forge.gamemodes.net.event.DraftPackArrivedEvent;
import forge.gamemodes.net.event.DraftSeatPickedEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.ReceiveEventPoolEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.item.PaperCard;
import forge.util.IHasForgeLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Server-side adapter that wraps {@link BoosterDraft} for network play.
 *
 * <p>Async model: each seat has its own pack queue. When a seat picks, the
 * picked-from pack is passed to the next seat in the pass direction immediately,
 * regardless of what other seats are doing. A fast picker may bank up multiple
 * packs while waiting for slower seats. Each human pick has its own timer,
 * reset whenever a new pack reaches the head of the queue.
 *
 * <p>Mutable state is guarded by {@code synchronized(this)}, but all network
 * dispatch is deferred to a list and run outside the monitor — otherwise a slow
 * client's {@code channel.writeAndFlush().sync()} would block the entire pod.
 */
public final class BoosterDraftHost implements IHasForgeLog {

    /**
     * Per-seat connection state. A disconnected seat enters {@code IN_GRACE} for
     * {@link NetworkEvent#getDisconnectGraceSeconds()}; if no reconnect happens in
     * that window it transitions to {@code POST_GRACE_AUTO} where all future packs
     * auto-pick first-card on arrival. Reconnect at any point returns the seat to
     * {@code LIVE}. A grace value of zero skips IN_GRACE entirely.
     */
    private enum SeatConnectionState { LIVE, IN_GRACE, POST_GRACE_AUTO }

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
    }

    /**
     * Start the draft: set phase and distribute initial packs.
     * Called once after the BoosterDraft has been initialized.
     */
    public void start() {
        List<Runnable> dispatches;
        synchronized (this) {
            long humans = participants.stream().filter(EventParticipant::isHuman).count();
            netLog.info("Draft started — {} humans, {} seats total, timer={}s, product={}",
                    humans, participants.size(), event.getPickTimerSeconds(), event.getProductDescription());
            event.setPhase(EventPhase.DRAFTING);
            captureInitialPackSize();
            dispatches = new ArrayList<>();
            advanceDraft(dispatches);
        }
        run(dispatches);
    }

    /**
     * Stop the draft and release timer resources. Safe to call multiple times.
     * Does not distribute pools — call before the draft has legitimately finished
     * (e.g. host cleared the event mid-draft, lobby shutting down).
     */
    public synchronized void shutdown() {
        finished = true;
        cancelAllSeatTimers();
        cancelAllGraceTimers();
        timerExecutor.shutdown();
    }

    /**
     * Handle an incoming pick from a human client.
     *
     * @param seatIndex the seat that made the pick
     * @param card      the chosen card
     */
    public void handlePick(int seatIndex, PaperCard card) {
        List<Runnable> dispatches;
        synchronized (this) {
            if (finished) return;
            List<LimitedPlayer> players = draft.getAllPlayers();
            if (seatIndex < 0 || seatIndex >= players.size()) {
                netLog.warn("Invalid seat index: {}", seatIndex);
                return;
            }
            LimitedPlayer player = players.get(seatIndex);
            DraftPack headPack = player.nextChoice();
            if (headPack == null || !headPack.contains(card)) {
                netLog.warn("Seat {} picked a card not in the current pack", seatIndex);
                return;
            }

            dispatches = new ArrayList<>();
            applyPickAndPass(player, seatIndex, card);
            cancelSeatTimer(seatIndex);
            inFlight[seatIndex] = false;

            netLog.info("Seat {} picked from pack {}", seatIndex, currentPackNumber);
            addBroadcastSeatPicked(dispatches, seatIndex);
            advanceDraft(dispatches);
        }
        run(dispatches);
    }

    /**
     * Apply a pick and, if the card's effect passes the pack, dequeue it from
     * the picker's queue and route it to the next seat in direction. Conspiracy
     * cards such as Agent of Acquisitions cause {@code draftCard} to return
     * {@code false}, meaning the picker keeps the pack for another pick.
     */
    private void applyPickAndPass(LimitedPlayer player, int seatIndex, PaperCard card) {
        Boolean passPack = player.draftCard(card, DeckSection.Sideboard);
        picksMadePerSeat[seatIndex]++;
        if (!Boolean.FALSE.equals(passPack)) {
            DraftPack passed = player.passPack();
            if (passed != null && !passed.isEmpty()) {
                passToNext(seatIndex, passed);
            }
        }
    }

    /**
     * Core distribution loop: advance rounds, let AI pick, notify humans of
     * packs at the head of their queue. Collects network dispatches into
     * {@code dispatches} to be run outside the monitor.
     */
    private void advanceDraft(List<Runnable> dispatches) {
        while (!finished) {
            // Round advancement — all queues drained means the round is over
            if (draft.isRoundOver()) {
                if (!draft.startRound()) {
                    addFinishDraft(dispatches);
                    return;
                }
                currentPackNumber = draft.getRound();
                captureInitialPackSize();
            }

            // Let any one AI with a pack pick, then restart so we re-check state
            List<LimitedPlayer> players = draft.getAllPlayers();
            boolean aiProgressed = false;
            for (int i = 0; i < players.size(); i++) {
                LimitedPlayer p = players.get(i);
                if (!(p instanceof LimitedPlayerAI ai)) continue;
                DraftPack head = p.nextChoice();
                if (head == null || head.isEmpty()) continue;

                if (p.shouldSkipThisPick()) {
                    // Skip without picking — pass the pack along
                    DraftPack skipPass = p.passPack();
                    if (skipPass != null && !skipPass.isEmpty()) passToNext(i, skipPass);
                    aiProgressed = true;
                    break;
                }

                PaperCard choice = ai.chooseCard();
                if (choice == null) continue;
                applyPickAndPass(ai, i, choice);
                addBroadcastSeatPicked(dispatches, i);
                aiProgressed = true;
                break;
            }
            if (aiProgressed) continue;

            // Drain any seat that has timed out its grace window — same pattern
            // as the AI loop above: one pick per pass, then restart.
            boolean autoPicked = false;
            for (int i = 0; i < players.size(); i++) {
                LimitedPlayer p = players.get(i);
                if (p instanceof LimitedPlayerAI) continue;
                if (seatState[i] != SeatConnectionState.POST_GRACE_AUTO) continue;
                DraftPack head = p.nextChoice();
                if (head == null || head.isEmpty()) continue;

                PaperCard autoPick = head.get(0);
                netLog.info("Seat {} disconnected past grace — auto-picking {}", i, autoPick.getName());
                applyPickAndPass(p, i, autoPick);
                addBroadcastSeatPicked(dispatches, i);
                autoPicked = true;
                break;
            }
            if (autoPicked) continue;

            // No AI/auto work left — notify any live humans with a fresh pack.
            // Seats in IN_GRACE hold their packs silently until they reconnect
            // or grace expires; POST_GRACE_AUTO is already handled above.
            for (int i = 0; i < players.size(); i++) {
                LimitedPlayer p = players.get(i);
                if (p instanceof LimitedPlayerAI) continue;
                if (seatState[i] != SeatConnectionState.LIVE) continue;
                DraftPack head = p.nextChoice();
                if (head == null || head.isEmpty()) continue;
                if (inFlight[i]) continue;

                addSendPackToHuman(dispatches, i, head);
                inFlight[i] = true;
                startSeatTimer(i);
            }
            return;
        }
    }

    /**
     * Pass a non-empty pack from {@code fromSeat} to the next seat in the current
     * pass direction (odd packs go right, even packs go left — MTG convention).
     */
    private void passToNext(int fromSeat, DraftPack pack) {
        int podSize = draft.getAllPlayers().size();
        int dir = (currentPackNumber % 2 == 1) ? 1 : -1;
        int nextSeat = ((fromSeat + dir) % podSize + podSize) % podSize;
        draft.getAllPlayers().get(nextSeat).receiveOpenedPack(pack);
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

    private void addSendPackToHuman(List<Runnable> dispatches, int seatIndex, DraftPack pack) {
        EventParticipant participant = findParticipant(seatIndex);
        if (participant == null || participant.isAI()) return;

        List<PaperCard> packCards = new ArrayList<>(pack);
        int packNum = currentPackNumber;
        int pickNum = pickNumberFor(pack);
        int timerSecs = event.getPickTimerSeconds();
        int slot = participant.getLobbySlotIndex();

        dispatches.add(() -> FServerManager.getInstance().sendToSlot(slot,
                new DraftPackArrivedEvent(seatIndex, packCards, packNum, pickNum, timerSecs),
                l -> l.draftPackArrived(seatIndex, packCards, packNum, pickNum, timerSecs)));
    }

    private void addBroadcastSeatPicked(List<Runnable> dispatches, int seatIndex) {
        int[] queueDepths = computeQueueDepths();
        dispatches.add(() -> FServerManager.getInstance().broadcast(
                new DraftSeatPickedEvent(seatIndex, queueDepths)));
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

    /**
     * Build pools and queue sends to each human participant. Called from inside
     * the monitor; the actual network dispatch happens after release.
     */
    private void addFinishDraft(List<Runnable> dispatches) {
        finished = true;
        cancelAllSeatTimers();
        cancelAllGraceTimers();
        timerExecutor.shutdown();
        draft.postDraftActions();
        netLog.info("Draft complete — distributing pools");

        List<LimitedPlayer> players = draft.getAllPlayers();
        String eventId = event.getEventId();

        for (int i = 0; i < players.size(); i++) {
            LimitedPlayer player = players.get(i);
            if (player instanceof LimitedPlayerAI) continue;

            EventParticipant participant = findParticipant(i);
            if (participant == null) continue;

            Deck pool = new Deck(player.getDeck(), NetworkEvent.poolNameFor(participant, event));
            NetworkEvent.setEventTags(pool, event);
            int slot = participant.getLobbySlotIndex();
            dispatches.add(() -> FServerManager.getInstance().sendToSlot(slot,
                    new ReceiveEventPoolEvent(eventId, pool),
                    l -> l.receiveEventPool(eventId, pool)));
        }
    }

    private void startSeatTimer(int seatIndex) {
        cancelSeatTimer(seatIndex);
        int seconds = event.getPickTimerSeconds();
        if (seconds <= 0) return;
        ScheduledFuture<?> f = timerExecutor.schedule(
                () -> onSeatTimerExpired(seatIndex), seconds, TimeUnit.SECONDS);
        seatTimers.put(seatIndex, f);
    }

    private void cancelSeatTimer(int seatIndex) {
        ScheduledFuture<?> f = seatTimers.remove(seatIndex);
        if (f != null) f.cancel(false);
    }

    private void cancelAllSeatTimers() {
        for (Integer seatIndex : new ArrayList<>(seatTimers.keySet())) {
            cancelSeatTimer(seatIndex);
        }
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
        List<Runnable> dispatches;
        synchronized (this) {
            if (finished || seatIndex < 0 || seatIndex >= seatState.length) return;
            if (seatState[seatIndex] != SeatConnectionState.LIVE) return;
            if (isAiSeat(seatIndex)) return;

            cancelSeatTimer(seatIndex);
            // Clear inFlight so the live-pack-distribution loop in advanceDraft
            // will re-send the current pack if the player reconnects before grace.
            inFlight[seatIndex] = false;

            int graceSeconds = event.getDisconnectGraceSeconds();
            dispatches = new ArrayList<>();
            addBroadcastDisconnect(dispatches, seatIndex);
            if (graceSeconds > 0) {
                seatState[seatIndex] = SeatConnectionState.IN_GRACE;
                graceTimers.put(seatIndex, timerExecutor.schedule(
                        () -> onGraceExpired(seatIndex), graceSeconds, TimeUnit.SECONDS));
                netLog.info("Seat {} disconnected — {}s grace started", seatIndex, graceSeconds);
            } else {
                // Zero-grace config — skip IN_GRACE and start auto-picking immediately.
                seatState[seatIndex] = SeatConnectionState.POST_GRACE_AUTO;
                netLog.info("Seat {} disconnected — grace disabled, auto-picking immediately", seatIndex);
                advanceDraft(dispatches);
            }
        }
        run(dispatches);
    }

    /**
     * Notify the host that a previously-disconnected seat has reconnected. If a
     * pack is currently at the head of the seat's queue it's re-sent to the
     * client and the pick timer restarts; otherwise the next pack to arrive will
     * be sent via the normal {@code advanceDraft} path.
     */
    public void onSeatReconnected(int seatIndex) {
        List<Runnable> dispatches;
        synchronized (this) {
            if (finished || seatIndex < 0 || seatIndex >= seatState.length) return;
            if (seatState[seatIndex] == SeatConnectionState.LIVE) return;

            cancelGraceTimer(seatIndex);
            seatState[seatIndex] = SeatConnectionState.LIVE;

            dispatches = new ArrayList<>();
            LimitedPlayer player = draft.getAllPlayers().get(seatIndex);
            DraftPack head = player.nextChoice();
            if (head != null && !head.isEmpty()) {
                addSendPackToHuman(dispatches, seatIndex, head);
                inFlight[seatIndex] = true;
                startSeatTimer(seatIndex);
            }
            addBroadcastReconnect(dispatches, seatIndex);
            netLog.info("Seat {} reconnected", seatIndex);
        }
        run(dispatches);
    }

    /** Grace timer callback — transition to POST_GRACE_AUTO and drain held/queued packs. */
    private void onGraceExpired(int seatIndex) {
        List<Runnable> dispatches;
        synchronized (this) {
            if (finished || seatState[seatIndex] != SeatConnectionState.IN_GRACE) return;
            seatState[seatIndex] = SeatConnectionState.POST_GRACE_AUTO;
            graceTimers.remove(seatIndex);

            netLog.info("Seat {} grace expired — switching to auto-pick", seatIndex);
            dispatches = new ArrayList<>();
            addBroadcastGraceExpired(dispatches, seatIndex);
            advanceDraft(dispatches);
        }
        run(dispatches);
    }

    private boolean isAiSeat(int seatIndex) {
        EventParticipant p = findParticipant(seatIndex);
        return p == null || p.isAI();
    }

    private void cancelGraceTimer(int seatIndex) {
        ScheduledFuture<?> f = graceTimers.remove(seatIndex);
        if (f != null) f.cancel(false);
    }

    private void cancelAllGraceTimers() {
        for (ScheduledFuture<?> f : graceTimers.values()) {
            if (f != null) f.cancel(false);
        }
        graceTimers.clear();
    }

    private void addBroadcastDisconnect(List<Runnable> dispatches, int seatIndex) {
        EventParticipant participant = findParticipant(seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        int graceSeconds = event.getDisconnectGraceSeconds();
        String msg = graceSeconds > 0
                ? String.format("%s disconnected from draft — %ds to reconnect before auto-picking starts.",
                        name, graceSeconds)
                : String.format("%s disconnected from draft — auto-picking remaining packs.", name);
        dispatches.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(msg)));
    }

    private void addBroadcastReconnect(List<Runnable> dispatches, int seatIndex) {
        EventParticipant participant = findParticipant(seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        dispatches.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(
                String.format("%s reconnected — picking live again.", name))));
    }

    private void addBroadcastGraceExpired(List<Runnable> dispatches, int seatIndex) {
        EventParticipant participant = findParticipant(seatIndex);
        if (participant == null) return;
        String name = participant.getName();
        dispatches.add(() -> FServerManager.getInstance().broadcast(new MessageEvent(
                String.format("%s grace period expired — auto-picking remaining packs.", name))));
    }

    /** Auto-pick the first card for a single seat that timed out. */
    private void onSeatTimerExpired(int seatIndex) {
        List<Runnable> dispatches;
        synchronized (this) {
            // Defensive: a disconnected seat's pick timer is cancelled on disconnect,
            // but guard against the runnable firing between cancel scheduling and the
            // monitor being acquired.
            if (finished || !inFlight[seatIndex] || seatState[seatIndex] != SeatConnectionState.LIVE) return;

            List<LimitedPlayer> players = draft.getAllPlayers();
            LimitedPlayer player = players.get(seatIndex);
            DraftPack pack = player.nextChoice();
            if (pack == null || pack.isEmpty()) return;

            PaperCard autoPick = pack.get(0);
            netLog.info("Pick timer expired for seat {} — auto-picking {}", seatIndex, autoPick.getName());

            dispatches = new ArrayList<>();
            applyPickAndPass(player, seatIndex, autoPick);
            inFlight[seatIndex] = false;

            // Auto-pick first so client's pending-self cache is set before the echo flushes it.
            addNotifyAutoPick(dispatches, seatIndex, autoPick);
            addBroadcastSeatPicked(dispatches, seatIndex);
            advanceDraft(dispatches);
        }
        run(dispatches);
    }

    private void addNotifyAutoPick(List<Runnable> dispatches, int seatIndex, PaperCard card) {
        EventParticipant participant = findParticipant(seatIndex);
        if (participant == null || participant.isAI()) return;
        int slot = participant.getLobbySlotIndex();
        int packNumber = currentPackNumber;
        int pickInPack = computePickInPack(picksMadePerSeat[seatIndex]);
        dispatches.add(() -> FServerManager.getInstance().sendToSlot(slot,
                new DraftAutoPickedEvent(seatIndex, card, packNumber, pickInPack),
                l -> l.draftAutoPicked(seatIndex, card, packNumber, pickInPack)));
    }

    private EventParticipant findParticipant(int seatIndex) {
        for (EventParticipant p : participants) {
            if (p.getSeatIndex() == seatIndex) {
                return p;
            }
        }
        return null;
    }

    private static void run(List<Runnable> dispatches) {
        for (Runnable r : dispatches) r.run();
    }
}

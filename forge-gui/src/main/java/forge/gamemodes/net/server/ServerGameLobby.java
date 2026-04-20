package forge.gamemodes.net.server;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.draft.BoosterDraftHost;
import forge.gamemodes.net.EventFormat;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.EventPhase;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.event.ReceiveEventPoolEvent;
import forge.gui.interfaces.IGuiGame;
import forge.util.IHasForgeLog;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ServerGameLobby extends GameLobby implements IHasForgeLog {
    private static final int DRAFT_POD_SIZE = 8;

    /** Returned by {@link #startDraftEvent} with the info the UI needs for overlay/log setup. */
    public record DraftStartResult(String[] names, boolean[] aiFlags, int hostSeatIndex, int totalPacks) {}

    private BoosterDraftHost draftHost;
    private NetworkEvent currentEvent;

    public NetworkEvent getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(NetworkEvent event) { this.currentEvent = event; }

    @Override
    protected void updateView(boolean fullUpdate) {
        if (currentEvent != null) {
            getData().setEventView(currentEvent.toView());
        } else {
            getData().setEventView(null);
        }
        super.updateView(fullUpdate);
    }

    /** Set the lobby's declared mode (Constructed / Limited) and broadcast to clients. */
    public void setLimitedMode(boolean limited) {
        getData().setLimitedMode(limited);
        updateView(true);
    }

    public ServerGameLobby() {
        super(true);
        addSlot(new LobbySlot(LobbySlotType.LOCAL, localName(), localAvatarIndices()[0], localSleeveIndices()[0],0, true, false, Collections.emptySet()));
        addSlot(new LobbySlot(LobbySlotType.OPEN, null, -1, -1, 1, false, false, Collections.emptySet()));
    }

    /**
     * Connect a player to the first available open slot.
     * This method is synchronized to prevent race conditions when multiple
     * clients connect simultaneously (which could assign the same slot to
     * multiple clients).
     *
     * @param name the player's name
     * @param avatarIndex the avatar index
     * @param sleeveIndex the sleeve index
     * @return the assigned slot index, or -1 if no slots available
     */
    public synchronized int connectPlayer(final String name, final int avatarIndex, final int sleeveIndex) {
        final int nSlots = getNumberOfSlots();
        for (int index = 0; index < nSlots; index++) {
            final LobbySlot slot = getSlot(index);
            if (slot.getType() == LobbySlotType.OPEN) {
                connectPlayer(name, avatarIndex, sleeveIndex, slot);
                return index;
            }
        }
        return -1;
    }
    private void connectPlayer(final String name, final int avatarIndex, final int sleeveIndex, final LobbySlot slot) {
        slot.setType(LobbySlotType.REMOTE);
        slot.setName(name);
        slot.setAvatarIndex(avatarIndex);
        slot.setSleeveIndex(sleeveIndex);
        updateView(false);
    }
    public void disconnectPlayer(final int index) {
        final LobbySlot slot = getSlot(index);
        if (slot == null) {
            return;
        }
        slot.setType(LobbySlotType.OPEN);
        slot.setName(StringUtils.EMPTY);
        slot.setIsReady(false);
        updateView(false);
    }

    @Override
    public boolean hasControl() {
        return true;
    }

    @Override
    public boolean mayEdit(final int index) {
        final LobbySlotType type = getSlot(index).getType();
        return type != LobbySlotType.REMOTE && type != LobbySlotType.OPEN;
    }

    @Override
    public boolean mayControl(final int index) {
        return getSlot(index).getType() != LobbySlotType.REMOTE;
    }

    @Override
    public boolean mayRemove(final int index) {
        return index >= 2;
    }

    @Override
    protected IGuiGame getGui(final int index) {
        return FServerManager.getInstance().getGui(index);
    }

    @Override
    protected void onGameStarted() {
    }

    @Override
    protected void onMatchOver() {
        for (int i = 0; i < getNumberOfSlots(); i++) {
            final LobbySlot slot = getSlot(i);
            if (slot != null) {
                slot.setIsReady(false);
            }
        }
        super.onMatchOver();
        FServerManager.getInstance().clearPlayerGuis();
        FServerManager.getInstance().updateLobbyState();
    }

    /**
     * Create the in-memory event. Does not broadcast — clients see the event
     * only after {@link #configureEvent} completes successfully. If the user
     * cancels a sub-dialog during configure, the event is discarded without
     * ever being visible to remote clients.
     */
    public synchronized void createEvent(EventFormat format) {
        netLog.info("Event created — format={}", format);
        NetworkEvent event = new NetworkEvent(format);
        setCurrentEvent(event);
    }

    /**
     * Configure the current event with the user's chosen pool type, pick timer, and
     * pre-built draft or sealed product. The caller is responsible for popping any
     * product sub-dialogs (block/set/cube/theme) before calling this — mirrors how
     * the offline flow builds a {@link BoosterDraft} right after pool selection.
     * On success, broadcasts the fully-configured event to clients.
     *
     * @param draft pre-built draft for {@link EventFormat#BOOSTER_DRAFT} events; ignored for sealed
     * @return false if no current event exists or sealed pool generation failed, true otherwise
     */
    public synchronized boolean configureEvent(LimitedPoolType poolType, BoosterDraft draft,
            int pickTimerSeconds, int disconnectGraceSeconds) {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return false;

        event.setPoolType(poolType);
        event.setProductDescription(poolType.toString());
        event.setPickTimerSeconds(pickTimerSeconds);
        event.setDisconnectGraceSeconds(disconnectGraceSeconds);

        if (event.getFormat() == EventFormat.SEALED) {
            SealedCardPoolGenerator gen = new SealedCardPoolGenerator(poolType);
            if (gen.isEmpty()) return false;
            event.setSealedGenerator(gen);
            if (gen.getProductName() != null) {
                event.setProductDescription(poolType + ": " + gen.getProductName());
            }
        } else {
            event.setDraft(draft);
            if (draft != null && draft.getProductName() != null) {
                event.setProductDescription(poolType + ": " + draft.getProductName());
            }
        }

        updateView(true);
        return true;
    }

    /**
     * Clear the current event and notify clients. Used when the host dismisses an
     * in-progress new event via the panel's close control, or when switching lobby modes.
     */
    public synchronized void clearCurrentEvent() {
        if (getCurrentEvent() == null) return;
        netLog.info("Event cleared by host");
        if (draftHost != null) {
            draftHost.shutdown();
            draftHost = null;
        }
        setCurrentEvent(null);
        getData().setActiveEventId(null);
        updateView(true);
    }

    /**
     * Populate event participants from current lobby slots.
     * Each non-OPEN slot becomes a participant: LOCAL and REMOTE are HUMAN, AI is AI.
     */
    public synchronized void populateParticipants() {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return;
        if (event.getPhase() != EventPhase.LOBBY_GATHER) {
            throw new IllegalStateException("populateParticipants only valid in LOBBY_GATHER, not " + event.getPhase());
        }
        event.getParticipants().clear();
        int seatIndex = 0;
        for (int i = 0; i < getNumberOfSlots(); i++) {
            LobbySlot slot = getSlot(i);
            if (slot.getType() == LobbySlotType.OPEN) {
                continue;
            }
            EventParticipant.Type pType = (slot.getType() == LobbySlotType.AI)
                    ? EventParticipant.Type.AI : EventParticipant.Type.HUMAN;
            event.addParticipant(new EventParticipant(slot.getName(), pType, seatIndex, i));
            seatIndex++;
        }
    }

    /**
     * Fill remaining seats up to targetSize with AI participants.
     * AI seats are for draft pick selection only — they are not match opponents.
     */
    public synchronized void fillRemainingWithAI(int targetSize) {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return;
        int currentSize = event.getParticipants().size();
        for (int i = currentSize; i < targetSize; i++) {
            String aiName = "Seat " + (i + 1);
            event.addParticipant(new EventParticipant(aiName, EventParticipant.Type.AI, i, -1));
        }
    }

    /**
     * Shuffle draft seat positions randomly. Lobby slots and names stay the same —
     * only the seat index (which determines pack-passing neighbors) is randomized.
     */
    public synchronized void shuffleSeatPositions() {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return;
        List<EventParticipant> participants = event.getParticipants();
        List<Integer> seats = new ArrayList<>();
        for (EventParticipant p : participants) {
            seats.add(p.getSeatIndex());
        }
        Collections.shuffle(seats);
        List<EventParticipant> reshuffled = new ArrayList<>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            EventParticipant p = participants.get(i);
            reshuffled.add(new EventParticipant(p.getName(), p.getType(), seats.get(i), p.getLobbySlotIndex()));
        }
        participants.clear();
        participants.addAll(reshuffled);
    }

    /**
     * Orchestrate the full draft startup: populate participants, create the BoosterDraft,
     * configure the pod, and start. Returns UI-facing result for overlay/log setup,
     * or null if draft creation fails or is cancelled.
     */
    public synchronized DraftStartResult startDraftEvent() {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return null;

        populateParticipants();
        fillRemainingWithAI(DRAFT_POD_SIZE);
        shuffleSeatPositions();

        List<EventParticipant> participants = event.getParticipants();
        int podSize = participants.size();

        BoosterDraft draft = event.getDraft();
        if (draft == null) return null;

        if (podSize != draft.getPodSize()) {
            draft.setPodSize(podSize);
        }
        Set<Integer> humanSeats = new HashSet<>();
        for (EventParticipant p : participants) {
            if (p.isHuman()) {
                humanSeats.add(p.getSeatIndex());
            }
        }
        draft.setHumanSeats(humanSeats);
        draft.initializeBoosters();

        int totalPacks = draft.getNumRounds();
        event.setNumRounds(totalPacks);

        // Build pod info for the UI
        int hostSeatIndex = 0;
        String[] names = new String[podSize];
        boolean[] aiFlags = new boolean[podSize];
        for (EventParticipant p : participants) {
            int seat = p.getSeatIndex();
            if (seat >= 0 && seat < podSize) {
                names[seat] = p.getName();
                aiFlags[seat] = p.isAI();
                if (p.getLobbySlotIndex() == 0) {
                    hostSeatIndex = seat;
                }
            }
        }

        netLog.info("Starting draft — pod={}, humans={}, packs={}, product={}, timer={}s",
                podSize, humanSeats.size(), totalPacks,
                event.getProductDescription(), event.getPickTimerSeconds());

        draftHost = new BoosterDraftHost(draft, event);
        // Broadcast the fully-populated event (with participants and numRounds) so
        // clients can initialize their overlay with pod names before the first pack.
        updateView(true);
        draftHost.start();

        return new DraftStartResult(names, aiFlags, hostSeatIndex, totalPacks);
    }

    /**
     * Orchestrate sealed pool generation: populate participants and distribute pools.
     */
    public synchronized void startSealedEvent() {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return;
        netLog.info("Starting sealed — product={}", event.getProductDescription());
        populateParticipants();
        event.setPhase(EventPhase.POOL_DISTRIBUTION);
        // Broadcast the now-populated event so clients see the phase change.
        updateView(true);
        generateAndDistributeSealedPools();
    }

    /**
     * Generate sealed pools and send one to each human participant.
     * Each pool is 6 boosters opened into a CardPool, wrapped in a Deck.
     */
    public synchronized void generateAndDistributeSealedPools() {
        NetworkEvent event = getCurrentEvent();
        if (event == null) {
            netLog.warn("Cannot generate sealed pools: no event configured");
            return;
        }
        if (event.getFormat() != EventFormat.SEALED) {
            netLog.warn("Event is not sealed format");
            return;
        }

        SealedCardPoolGenerator gen = event.getSealedGenerator();
        if (gen == null || gen.isEmpty()) {
            netLog.warn("No sealed generator configured");
            return;
        }

        String eventId = event.getEventId();
        FServerManager server = FServerManager.getInstance();

        for (EventParticipant participant : event.getParticipants()) {
            if (participant.isAI()) {
                continue;
            }

            CardPool pool = gen.getCardPool(false);
            if (pool == null) {
                netLog.warn("Failed to generate pool for {}", participant.getName());
                continue;
            }

            Deck deck = new Deck(NetworkEvent.poolNameFor(event));
            deck.getOrCreate(DeckSection.Sideboard).addAll(pool);
            NetworkEvent.setEventTags(deck, event);

            server.sendToSlot(participant.getLobbySlotIndex(),
                    new ReceiveEventPoolEvent(eventId, deck),
                    l -> l.receiveEventPool(eventId, deck));
            netLog.info("Sent sealed pool to {} ({} cards)", participant.getName(), pool.countAll());
        }
    }

    /**
     * Route an incoming draft pick from a client to the draft host. When the
     * pick came over the wire, {@code expectedLobbySlot} identifies the
     * submitting client's lobby slot so we can verify it owns the seat —
     * otherwise any client could submit picks for anyone. Host-local picks
     * (where the host is the picker) pass -1 to skip the slot check.
     */
    public synchronized void handleDraftPick(DraftPickEvent pickEvent, int expectedLobbySlot) {
        if (draftHost == null) {
            netLog.warn("Draft pick received but no draft in progress");
            return;
        }
        int seat = pickEvent.getSeatIndex();
        if (expectedLobbySlot >= 0) {
            int ownerSlot = findLobbySlotForSeat(seat);
            if (ownerSlot != expectedLobbySlot) {
                netLog.warn("Rejecting pick from lobby slot {} for seat {} (owner slot {})",
                        expectedLobbySlot, seat, ownerSlot);
                return;
            }
        }
        draftHost.handlePick(seat, pickEvent.getCard());
    }

    /** Lobby slot of the participant occupying the given seat, or -1 if none. */
    public synchronized int findLobbySlotForSeat(int seatIndex) {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return -1;
        for (EventParticipant p : event.getParticipants()) {
            if (p.getSeatIndex() == seatIndex) return p.getLobbySlotIndex();
        }
        return -1;
    }

    /** Seat index of the participant occupying the given lobby slot, or -1 if none. */
    public synchronized int findSeatForLobbySlot(int slotIndex) {
        NetworkEvent event = getCurrentEvent();
        if (event == null) return -1;
        for (EventParticipant p : event.getParticipants()) {
            if (p.getLobbySlotIndex() == slotIndex) return p.getSeatIndex();
        }
        return -1;
    }

    /** Persist event selection on the lobby data and broadcast via LobbyUpdateEvent. */
    public void selectEventForMatch(String eventId, boolean deckConformance) {
        getData().setActiveEventId(eventId);
        getData().setActiveConformance(deckConformance);
        netLog.info("Selected event for match — eventId={}, conformance={}", eventId, deckConformance);
        updateView(true);
    }

    public BoosterDraftHost getDraftHost() {
        return draftHost;
    }
}

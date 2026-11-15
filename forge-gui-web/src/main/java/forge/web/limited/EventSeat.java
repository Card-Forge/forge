package forge.web.limited;

import forge.deck.Deck;
import forge.game.GameType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.net.EventFormat;
import forge.gamemodes.net.NetworkEventView;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.model.FModel;
import forge.util.storage.IStorage;
import forge.web.WebGuiBase;
import forge.web.WebSession;
import forge.web.WebSession.Setup;
import forge.web.deck.DeckCatalog;
import forge.web.deck.DeckWorkspace;
import forge.web.deck.DeckStore;
import forge.web.lobby.Lobby;
import forge.web.match.NetplaySeat;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.ToBrowser.DraftState;

import java.util.function.Supplier;

/** One session's seat in its table's draft or sealed event. It tracks the draft it picks in, the pool it is dealt, and its player leaving and returning. */
public final class EventSeat {
    private final WebSession session;
    private final WebGuiBase ui;
    private final Lobby lobby;
    private final DeckWorkspace decks;
    private final NetplaySeat local;
    /** The host's table, for any seat. Every seat's session runs in the host's process, where the draft runs. */
    private final Supplier<ServerGameLobby> hostTable;
    private final Runnable seatsChanged;
    /** This table's draft. Each new table gets a new draft. */
    private volatile OnlineDraft draft;
    /** Whether the draft host was told that this seat's player left. If so, it is told when the browser comes back. */
    private volatile boolean reportedGone;
    private volatile boolean toldDrafting;

    public EventSeat(final WebSession session, final WebGuiBase ui, final Lobby lobby, final DeckWorkspace decks, final NetplaySeat local,
            final Supplier<ServerGameLobby> hostTable, final Runnable seatsChanged) {
        this.session = session;
        this.ui = ui;
        this.lobby = lobby;
        this.decks = decks;
        this.local = local;
        this.hostTable = hostTable;
        this.seatsChanged = seatsChanged;
    }

    /** Creates this table's draft. Its events are handled one at a time on the client's dispatch executor, like the match's events. */
    public void newTable(final WebGuiGame gui) {
        reportedGone = false;
        toldDrafting = false;
        // An event from an earlier table's draft can still arrive. It does not belong to this table, so it is ignored
        final OnlineDraft[] self = new OnlineDraft[1];
        final OnlineDraft made = new OnlineDraft(gui.dispatchExecutor(), this::eventView, this::podSeat, this::seatHeld,
                local::sendToHost, state -> {
                    if (draft == self[0]) {
                        show(state);
                    }
                }, (eventId, pool) -> ui.invokeInEdtLater(() -> {
                    if (draft == self[0]) {
                        poolArrived(eventId, pool);
                    }
                }));
        self[0] = made;
        draft = made;
        local.setDraftHandler(made);
    }

    /** The draft this seat picks in, while its table has one. */
    public OnlineDraft draft() {
        return draft;
    }

    public boolean drafting() {
        final OnlineDraft d = draft;
        return d != null && d.drafting();
    }

    /** The browser came back to the table. It is shown the draft again. A draft host that was told it left is told it is back. */
    public void reconnected(final BrowserChannel channel) {
        if (drafting()) {
            channel.send(draft.latest());
        }
        if (reportedGone) {
            reportedGone = false;
            final ServerGameLobby table = hostTable.get();
            if (table != null && table.getDraftHost() != null && podSeat() >= 0) {
                table.getDraftHost().onSeatReconnected(podSeat());
            }
            seatsChanged.run();
        }
    }

    /** A pod seat's player left or came back, so this seat's dial is redrawn. */
    public void seatsChanged() {
        final OnlineDraft d = draft;
        if (d != null) {
            d.refresh();
        }
    }

    /** The browser has been gone for a while. A drafting seat's draft host is told the player left, as desktop's is told when a connection closes. */
    public void goneAWhile() {
        final ServerGameLobby table = hostTable.get();
        final int seat = podSeat();
        if (session.attached() || !drafting() || table == null || table.getDraftHost() == null || seat < 0) {
            return;
        }
        table.getDraftHost().onSeatDisconnected(seat);
        reportedGone = true;
        seatsChanged.run();
    }

    /** The table's event, as this seat's lobby last received it. */
    private NetworkEventView eventView() {
        final GameLobby table = local.hostedLobby() != null ? local.hostedLobby() : local.clientLobby();
        return table == null || table.getData() == null ? null : table.getData().getEventView();
    }

    /** This seat's place in the draft pod, or -1 before the pod is seated. */
    private int podSeat() {
        final ServerGameLobby table = hostTable.get();
        return table == null || local.webSeat() < 0 ? -1 : table.findSeatForLobbySlot(local.webSeat());
    }

    /** Whether a pod seat's player has left. */
    private boolean seatHeld(final int seat) {
        final ServerGameLobby table = hostTable.get();
        return table != null && table.getDraftHost() != null && table.getDraftHost().isSeatHeld(seat);
    }

    private void show(final DraftState state) {
        // The first pack moves the browser to the draft. The hello message tells it that it is in the draft
        if (!toldDrafting) {
            toldDrafting = true;
            session.tell(session.hello());
        }
        session.tell(state);
    }

    /** Opens the event's pool in the limited editor. The host stores it with the event decks. A guest's pool is stored in its browser. */
    private void poolArrived(final String eventId, final Deck pool) {
        // A pool that arrives as this seat leaves its table belongs to a closing table, so it is not shown
        if (!(session.stage() instanceof Setup)) {
            return;
        }
        final NetworkEventView event = eventView();
        final GameType type = event != null && event.getFormat() == EventFormat.SEALED ? GameType.Sealed : GameType.Draft;
        final BrowserChannel b = session.browser();
        if (session.hosting()) {
            final IStorage<Deck> eventDecks = FModel.getDecks().getNetworkEventDecks();
            final Deck kept;
            synchronized (DeckCatalog.DECKS) {
                // Two events of the same product on the same day get the same name. The newer pool must not replace the older one
                kept = new Deck(pool, DeckStore.freeName(eventDecks, pool.getName(), null));
                eventDecks.add(kept);
            }
            final ServerGameLobby table = local.hostedLobby();
            if (table != null) {
                table.selectEventForMatch(eventId, true);
            }
            decks.openEventPool(kept, type, eventDecks, null, b);
        } else {
            decks.openEventPool(pool, type, null, "event-" + eventId, b);
        }
        // An event's deck is new, so it gets a new sleeve instead of the sleeve the seat had when it sat down
        lobby.dealSleeve(lobby.mySeat());
        session.tell(session.hello());
    }
}

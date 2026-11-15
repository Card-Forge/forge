package forge.web.limited;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.NetworkEventView;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.interfaces.IDraftEventHandler;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.DraftCard;
import forge.web.protocol.ToBrowser.DraftLogKind;
import forge.web.protocol.ToBrowser.DraftSeat;
import forge.web.protocol.ToBrowser.DraftState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** One seat's view of a draft that the host's lobby runs. Events are handled one at a time, so the browser gets complete states in order. */
public final class OnlineDraft implements IDraftEventHandler, Draft {
    private final Executor serial;
    private final Supplier<NetworkEventView> event;
    private final IntSupplier lobbySeat;
    private final IntPredicate held;
    private final Consumer<NetEvent> send;
    private final Consumer<DraftState> publish;
    private final BiConsumer<String, Deck> onPool;
    private final DraftView view = new DraftView();
    private final List<DraftCard> picks = new ArrayList<>();
    private List<PaperCard> pack = List.of();
    private int mySeat = -1;
    private int packNumber;
    private int pickNumber;
    private int packSize;
    private int clockSeconds;
    private long clockEnd;
    private int[] depths;
    /** The card this seat sent as its pick. It counts only once the draft host says the seat picked. */
    private PaperCard pending;
    private boolean pendingSideboard;
    private volatile boolean pooled;

    /** lobbySeat finds this seat before the first pack gives its number. held says whether a seat's player has left. */
    public OnlineDraft(final Executor serial, final Supplier<NetworkEventView> event, final IntSupplier lobbySeat, final IntPredicate held,
            final Consumer<NetEvent> send, final Consumer<DraftState> publish, final BiConsumer<String, Deck> onPool) {
        this.serial = serial;
        this.event = event;
        this.lobbySeat = lobbySeat;
        this.held = held;
        this.send = send;
        this.publish = publish;
        this.onPool = onPool;
    }

    @Override
    public DraftState latest() {
        return view.latest();
    }

    /** Whether the draft is on screen. A pack has arrived, and the pool that ends the event has not. */
    public boolean drafting() {
        return view.latest() != null && !pooled;
    }

    @Override
    public void draftPackArrived(final int seatIndex, final List<PaperCard> cards, final int packNum, final int pickNum,
            final int timerSeconds) {
        serial.execute(() -> {
            mySeat = seatIndex;
            if (packNum != packNumber) {
                view.log(DraftLogKind.pack, Localizer.getInstance().getMessage("lblDraftLogPackHeader", packNum,
                        Localizer.getInstance().getMessage(DraftView.passDirection(packNum) > 0 ? "lblDraftLogPassingRight" : "lblDraftLogPassingLeft")));
            }
            pack = List.copyOf(cards);
            packNumber = packNum;
            pickNumber = pickNum;
            packSize = cards.size() + pickNum;
            clockSeconds = Math.max(0, timerSeconds);
            clockEnd = clockSeconds == 0 ? 0 : System.currentTimeMillis() + clockSeconds * 1000L;
            send(true, List.of());
        });
    }

    @Override
    public void draftSeatPicked(final int seatIndex, final int[] seatQueueDepths) {
        serial.execute(() -> {
            final int seat = seat();
            final List<Integer> moved = DraftView.moved(depths, seatQueueDepths, seatIndex, DraftView.passDirection(packNumber));
            depths = seatQueueDepths.clone();
            final boolean mine = seatIndex == seat;
            if (mine) {
                if (pending != null) {
                    picks.add(DraftView.card(pending, packNumber, pickNumber + 1, pendingSideboard));
                    view.log(DraftLogKind.own, Localizer.getInstance().getMessage("lblWebOnlineDraftYouPicked", pending.getName()));
                    pending = null;
                }
                // The pack picked from has been passed on. The next pack arrives as a separate event
                pack = List.of();
                clockEnd = 0;
            } else {
                view.log(DraftLogKind.seat, Localizer.getInstance().getMessage("lblWebOnlineDraftSeatPicked", nameOf(seatIndex), depthOf(seatIndex)));
            }
            if (view.latest() != null) {
                send(mine, moved);
            }
        });
    }

    @Override
    public void draftAutoPicked(final int seatIndex, final PaperCard card, final int packNum, final int pickInPack) {
        serial.execute(() -> {
            // The timer made the pick, so a click that arrived at the same time is not counted too
            pending = null;
            picks.add(DraftView.card(card, packNum, pickInPack, false));
            view.log(DraftLogKind.own, Localizer.getInstance().getMessage("lblWebOnlineDraftOutOfTime", card.getName()));
        });
    }

    @Override
    public void receiveEventPool(final String eventId, final Deck pool) {
        serial.execute(() -> {
            pooled = true;
            onPool.accept(eventId, sorted(pool));
        });
    }

    @Override
    public void pick(final int step, final int index, final boolean sideboard) {
        serial.execute(() -> {
            final DraftState now = view.latest();
            if (pooled || now == null || now.step() != step || pending != null || index < 0 || index >= pack.size()) {
                return;
            }
            pending = pack.get(index);
            pendingSideboard = sideboard;
            send.accept(new DraftPickEvent(mySeat, pending));
        });
    }

    /** At the end, the host's pool is sorted by these moves. */
    @Override
    public void move(final int index, final boolean sideboard) {
        serial.execute(() -> {
            if (pooled || view.latest() == null || index < 0 || index >= picks.size()) {
                return;
            }
            picks.set(index, DraftView.moved(picks.get(index), sideboard));
            send(false, List.of());
        });
    }

    /** The host does not learn where each pick went, so the pool is sorted by the picks this seat stored, and every other card goes to the sideboard. */
    private Deck sorted(final Deck pool) {
        final Map<String, Integer> main = new HashMap<>();
        for (final DraftCard pick : picks) {
            if (!pick.sideboard()) {
                main.merge(pick.name(), 1, Integer::sum);
            }
        }
        final List<PaperCard> all = new ArrayList<>(pool.getMain().toFlatList());
        if (pool.has(DeckSection.Sideboard)) {
            all.addAll(pool.get(DeckSection.Sideboard).toFlatList());
        }
        final Deck out = new Deck(pool, pool.getName());
        out.getMain().clear();
        out.getOrCreate(DeckSection.Sideboard).clear();
        for (final PaperCard card : all) {
            final int left = main.getOrDefault(card.getName(), 0);
            if (left > 0) {
                main.put(card.getName(), left - 1);
                out.getMain().add(card);
            } else {
                out.get(DeckSection.Sideboard).add(card);
            }
        }
        return out;
    }

    /** Sends the state again with each seat's held flag read again, after a player left or came back. */
    public void refresh() {
        serial.execute(() -> {
            if (view.latest() != null && !pooled) {
                send(false, List.of());
            }
        });
    }

    private int seat() {
        if (mySeat < 0) {
            mySeat = lobbySeat.getAsInt();
        }
        return mySeat;
    }

    private List<EventParticipant> participants() {
        final NetworkEventView e = event.get();
        return e == null ? List.of() : e.getParticipants();
    }

    private String nameOf(final int seat) {
        final EventParticipant p = EventParticipant.findBySeat(participants(), seat);
        return p == null || p.getName() == null ? Localizer.getInstance().getMessage("lblSeatN", seat + 1) : p.getName();
    }

    private int depthOf(final int seat) {
        return depths != null && seat >= 0 && seat < depths.length ? depths[seat] : 0;
    }

    private void send(final boolean newPack, final List<Integer> moved) {
        final NetworkEventView e = event.get();
        final int n = depths != null ? depths.length : participants().size();
        final int me = Math.max(0, seat());
        if (depths == null) {
            // Before any seat has picked, every seat holds the one pack it opened
            depths = new int[n];
            Arrays.fill(depths, 1);
        }
        final List<DraftSeat> seats = new ArrayList<>();
        for (int d = 0; d < n; d++) {
            final int s = (me + d) % n;
            final EventParticipant p = EventParticipant.findBySeat(participants(), s);
            seats.add(new DraftSeat(nameOf(s), p == null || p.isAI(), depths[s], held.test(s)));
        }
        final List<Integer> movedHere = moved.stream().map(s -> Math.floorMod(s - me, n)).toList();
        final int pick = pickNumber + 1;
        final List<DraftCard> cards = pack.stream().map(c -> DraftView.card(c, packNumber, pick, false)).toList();
        final int left = clockEnd == 0 ? 0 : (int) Math.max(0, clockEnd - System.currentTimeMillis());
        publish.accept(view.state(newPack, step -> new DraftState(step, e == null || e.getProductDescription() == null ? "" : e.getProductDescription(), packNumber,
                e == null ? 0 : e.getNumRounds(), pick, packSize, DraftView.passDirection(packNumber), seats, cards, List.copyOf(picks), movedHere,
                clockSeconds, left, view.lines(), false)));
    }
}

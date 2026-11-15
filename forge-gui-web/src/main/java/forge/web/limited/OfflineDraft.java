package forge.web.limited;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPlayer;
import forge.gamemodes.limited.LimitedPlayerAI;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.DraftCard;
import forge.web.protocol.ToBrowser.DraftSeat;
import forge.web.protocol.ToBrowser.DraftState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/** One player's booster draft against the computer. BoosterDraft has no locks, so all code that uses it runs on this draft's own thread. */
public final class OfflineDraft implements Draft {
    private final ExecutorService thread = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("Draft").setDaemon(true).build());
    private final String playerName;
    private final Consumer<DraftState> publish;
    private final Consumer<String> fail;
    private final List<DraftCard> picks = new ArrayList<>();
    /** The cards behind picks, in the same order. They are used to move a card between the deck's sections. */
    private final List<PaperCard> picked = new ArrayList<>();
    private BoosterDraft draft;
    private String product;
    private List<PaperCard> pack = List.of();
    private int round;
    private int packSize;
    /** Picks made from the current round's packs. This count gives the pick number, because pack sizes can differ within a round. */
    private int roundPicks;
    private final DraftView view = new DraftView();

    /** make builds the draft on the draft's thread, because importing a cube waits for a web site. fail receives the reason when the draft cannot start. */
    public OfflineDraft(final Supplier<BoosterDraft> make, final String playerName, final Consumer<DraftState> publish,
            final Consumer<String> fail) {
        this.playerName = playerName;
        this.publish = publish;
        this.fail = fail;
        thread.execute(() -> {
            try {
                draft = make.get();
                product = draft.getProductName() == null ? Localizer.getInstance().getMessage("lblLimitedPoolFull") : draft.getProductName();
                draft.initializeBoosters();
                advance(false);
            } catch (final RuntimeException e) {
                fail.accept(e.getMessage() == null ? Localizer.getInstance().getMessage("lblWebLobbyDraftSetUpFailed") : e.getMessage());
            }
        });
    }

    @Override
    public DraftState latest() {
        return view.latest();
    }

    @Override
    public void pick(final int stepShown, final int index, final boolean sideboard) {
        thread.execute(() -> {
            try {
                pickOn(stepShown, index, sideboard);
            } catch (final RuntimeException e) {
                fail.accept(Localizer.getInstance().getMessage("lblWebOfflineDraftStopped", String.valueOf(e.getMessage())));
            }
        });
    }

    @Override
    public void move(final int index, final boolean sideboard) {
        thread.execute(() -> {
            final DraftState now = view.latest();
            if (now == null || now.done() || index < 0 || index >= picks.size() || picks.get(index).sideboard() == sideboard) {
                return;
            }
            final Deck deck = draft.getHumanPlayer().getDeck();
            final PaperCard card = picked.get(index);
            deck.getOrCreate(sideboard ? DeckSection.Main : DeckSection.Sideboard).remove(card);
            deck.getOrCreate(sideboard ? DeckSection.Sideboard : DeckSection.Main).add(card);
            picks.set(index, DraftView.moved(picks.get(index), sideboard));
            send(false, false, false);
        });
    }

    private void pickOn(final int stepShown, final int index, final boolean sideboard) {
        final DraftState now = view.latest();
        if (now == null || now.done() || now.step() != stepShown || index < 0 || index >= pack.size()) {
            return;
        }
        final LimitedPlayer me = draft.getHumanPlayer();
        // Desktop checks these two cases before taking the card, in this order
        if (me.shouldSkipThisPick()) {
            advance(false);
            return;
        }
        final PaperCard card = me.hasArchdemonCurse() ? me.pickFromArchdemonCurse(me.nextChoice()) : pack.get(index);
        final boolean passed = draft.setChoice(card, sideboard ? DeckSection.Sideboard : DeckSection.Main);
        picks.add(DraftView.card(card, round, now.pick(), sideboard));
        picked.add(card);
        roundPicks++;
        advance(passed);
    }

    /** Builds the finished draft as desktop does: the player's picks and each computer's deck, all under name. */
    CompletableFuture<DeckGroup> save(final String name) {
        return CompletableFuture.supplyAsync(() -> draft.toDeckGroup(name), thread);
    }

    /** The commander that the drafted set gives every player, or null. Read it only after the draft is done, when no other code uses the draft. */
    String freeCommander() {
        return draft == null ? null : draft.getFreeCommanderName();
    }

    void close() {
        thread.shutdownNow();
    }

    /** Shows the next pack, found the same way as in desktop's showPackToDraft, or ends the draft. */
    private void advance(final boolean passed) {
        if (draft.hasNextChoice()) {
            final CardPool next = draft.nextChoice();
            if (next != null && !next.isEmpty()) {
                pack = next.toFlatList();
                if (draft.getRound() != round) {
                    round = draft.getRound();
                    packSize = pack.size();
                    roundPicks = 0;
                }
                send(passed, false, true);
                return;
            }
        }
        draft.postDraftActions();
        pack = List.of();
        send(passed, true, true);
    }

    /** When newPack is false the step stays the same, so a card the player has selected in the pack stays selected. */
    private void send(final boolean passed, final boolean done, final boolean newPack) {
        final List<DraftSeat> seats = new ArrayList<>();
        final List<LimitedPlayer> players = draft.getAllPlayers();
        for (int i = 0; i < players.size(); i++) {
            final LimitedPlayer p = players.get(i);
            final String name = i == 0 ? playerName : p.getName() == null || p.getName().isBlank() ? Localizer.getInstance().getMessage("lblSeatN", i + 1) : p.getName();
            seats.add(new DraftSeat(name, p instanceof LimitedPlayerAI, p.getPackQueueSize(), false));
        }
        final int pick = roundPicks + 1;
        final List<DraftCard> cards = pack.stream().map(c -> DraftView.card(c, round, pick, false)).toList();
        // Offline, all packs are passed at the same time, so a pass moves every seat's pack
        final List<Integer> moved = passed ? IntStream.range(0, seats.size()).boxed().toList() : List.of();
        publish.accept(view.state(newPack, step -> new DraftState(step, product, round, draft.getNumRounds(), pick, packSize,
                DraftView.passDirection(round), seats, cards, List.copyOf(picks), moved, 0, 0, List.of(), done)));
    }
}

package forge.web.limited;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.CommanderDraftUtil;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.web.WebGuiBase;
import forge.web.WebSession;
import forge.web.WebSession.Event;
import forge.web.WebSession.Playing;
import forge.web.WebSession.Stage;
import forge.web.deck.DeckCatalog;
import forge.web.deck.DeckFacts;
import forge.web.deck.DeckWorkspace;
import forge.web.deck.DeckStore;
import forge.web.lobby.Lobby;
import forge.web.match.NetplaySeat;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.Commands;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** One session's Limited play against the computer: its sealed pools, its draft and its gauntlet. */
public final class OfflineLimited implements IHasForgeLog {
    private final WebSession session;
    private final WebGuiBase ui;
    private final Lobby lobby;
    private final DeckWorkspace decks;
    private final NetplaySeat local;
    /** Whether a sealed pool is being opened. Opening takes seconds and may wait for an answer from the player. */
    private final AtomicBoolean openingPacks = new AtomicBoolean();
    /** A match seats at most eight players, so a game against several opponents has at most seven. */
    private static final int MOST_OPPONENTS = 7;
    private volatile boolean gauntletRunning;
    private volatile LimitedResult lastResult;
    /** The pool the last match returns to, and the game type it is played as, which a gauntlet keeps for its later rounds. */
    private volatile Event matchBack;
    private volatile GameType matchType;
    /** The pool numbers of a match's opponents in seat order, except in a gauntlet, whose round names its opponent. */
    private volatile List<Integer> opponentNumbers = List.of();
    private volatile LobbyPlayer matchPlayer;
    private volatile OfflineDraft draft;

    public OfflineLimited(final WebSession session, final WebGuiBase ui, final Lobby lobby, final DeckWorkspace decks, final NetplaySeat local) {
        this.session = session;
        this.ui = ui;
        this.lobby = lobby;
        this.decks = decks;
        this.local = local;
    }

    public static final Commands<OfflineLimited> COMMANDS = new Commands<OfflineLimited>()
            // Opening and deleting a pool read files, so they run on the deck thread in order with the editor's commands
            .on(PoolEdit.class, (l, ch, e) -> l.decks.inTurn(() -> l.editPool(ch, e.name())))
            .on(PoolDelete.class, (l, ch, d) -> l.decks.inTurn(() -> l.deletePool(ch, d.name())))
            .on(SealedCreate.class, OfflineLimited::createSealed)
            .on(DraftStart.class, OfflineLimited::startDraft)
            .on(DraftSave.class, OfflineLimited::saveDraft)
            .on(Plain.draftDiscard, (l, ch, c) -> l.endDraft())
            // The gauntlet starts its own rounds. Like any match, a round starts on the host UI thread
            .on(Plain.gauntletNext, (l, ch, c) -> l.ui.invokeInEdtLater(() -> {
                final LimitedResult result = l.lastResult;
                if (l.gauntletRunning && result != null && result.nextRound()) {
                    l.lastResult = null;
                    l.playRound(() -> FModel.getLimitedMatch().nextRound());
                }
            }))
            .on(Plain.gauntletRestart, (l, ch, c) -> l.ui.invokeInEdtLater(() -> {
                if (l.gauntletRunning) {
                    l.lastResult = null;
                    final HostedMatch match = l.local.hostedMatch();
                    if (match != null) {
                        match.restartMatch();
                    }
                }
            }))
            // A match is started on the host UI thread, like a table's match
            .on(PoolPlay.class, (l, ch, play) -> l.ui.invokeInEdtLater(() -> l.playPool(ch, play)));

    /** Sends the pools and the setup form's options, which the Limited pages are drawn from. Reading the lists reads files, so it runs as a background task. */
    public void sendLimited(final BrowserChannel channel) {
        ui.runBackgroundTask("Limited", () -> {
            channel.send(OfflinePools.pools());
            channel.send(OfflinePools.options());
        });
    }

    private void editPool(final BrowserChannel channel, final String name) {
        final Event e = session.stage() instanceof Event event ? event : null;
        final DeckGroup group = e == null ? null : e.storage().get(name);
        if (group == null) {
            channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionNoPool", name)));
            return;
        }
        decks.openPool(group.getHumanDeck(), e.storage(), poolType(e.type(), group.getHumanDeck()), null, channel);
    }

    private void deletePool(final BrowserChannel channel, final String name) {
        final Stage now = session.stage();
        if (!session.hosting() || !(now instanceof Event e)) {
            return;
        }
        if (name.equals(decks.openPoolName())) {
            channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionClosePoolDeck", name)));
            return;
        }
        synchronized (DeckCatalog.DECKS) {
            if (e.storage().contains(name)) {
                e.storage().delete(name);
            }
        }
        if (name.equals(e.pool())) {
            session.move(now, new Event(e.kind(), null));
        }
        channel.send(OfflinePools.pools());
    }

    /** Opens a sealed pool as desktop's sealed screen does, then opens its deck in the editor. The check for a name already in use comes first. */
    private void createSealed(final BrowserChannel channel, final SealedCreate create) {
        if (!session.hosting() || !(session.stage() instanceof Event e) || e.kind() != EventKind.sealed) {
            return;
        }
        final String name = create.name() == null ? "" : create.name().trim();
        final String problem = DeckStore.nameProblem(name);
        if (problem != null) {
            channel.send(WebSession.error(problem));
            return;
        }
        if (OfflinePools.sealed().contains(name) && !create.replace()) {
            channel.send(new NameTaken(name));
            return;
        }
        // A second request while the first is still opening would pass the name check above and replace the first pool
        if (!openingPacks.compareAndSet(false, true)) {
            channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionOpeningPacks")));
            return;
        }
        // Opening can wait for an answer from the player, so the results go to whichever browser is attached when it finishes
        ui.runBackgroundTask("Sealed", () -> {
            try {
                final DeckGroup group;
                try {
                    group = OfflinePools.create(create, name);
                } catch (final IllegalArgumentException ex) {
                    session.tell(WebSession.error(ex.getMessage()));
                    return;
                }
                if (group == null) {
                    return;
                }
                OfflinePools.store(OfflinePools.sealed(), group);
                final Stage now = session.stage();
                if (now instanceof Event) {
                    session.move(now, new Event(EventKind.sealed, name));
                }
                session.tell(OfflinePools.pools());
                decks.openPool(group.getHumanDeck(), OfflinePools.sealed(), GameType.Sealed, null, session.browser());
            } finally {
                openingPacks.set(false);
            }
        });
    }

    /** Starts an offline booster draft against the computer, as desktop's Draft screen does. One runs at a time. */
    private void startDraft(final BrowserChannel channel, final DraftStart start) {
        synchronized (session) {
            final Stage now = session.stage();
            if (!session.hosting() || !(now instanceof Event e) || e.kind() != EventKind.draft) {
                return;
            }
            if (draft != null) {
                channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionDraftRunning")));
                return;
            }
            final Supplier<BoosterDraft> make;
            try {
                make = OfflinePools.draft(start);
            } catch (final IllegalArgumentException ex) {
                channel.send(WebSession.error(ex.getMessage()));
                return;
            }
            draft = new OfflineDraft(make, session.playerName(), session::tell, problem -> {
                endDraft();
                session.tell(WebSession.error(problem));
            });
            session.move(now, new Event(EventKind.draft, null));
        }
    }

    /** The draft against the computer that is being played, or null. */
    public OfflineDraft draft() {
        return draft;
    }

    /** Stops the draft without saving it, as desktop's "quit without saving" does. */
    private void endDraft() {
        final OfflineDraft ended;
        synchronized (session) {
            ended = draft;
            draft = null;
        }
        if (ended == null) {
            return;
        }
        ended.close();
        final Stage now = session.stage();
        if (now instanceof Event e) {
            session.move(now, new Event(e.kind(), e.pool()));
        } else {
            session.tell(session.hello());
        }
    }

    /** Saves a finished draft as desktop does, then opens its deck. It asks before replacing a draft of the same name. */
    private void saveDraft(final BrowserChannel channel, final DraftSave save) {
        final OfflineDraft draft = this.draft;
        if (!session.hosting() || draft == null || draft.latest() == null || !draft.latest().done()) {
            return;
        }
        final String name = save.name() == null ? "" : save.name().trim();
        final String problem = DeckStore.nameProblem(name);
        if (problem != null) {
            channel.send(WebSession.error(problem));
            return;
        }
        final IStorage<DeckGroup> drafts = OfflinePools.storage(EventKind.draft);
        if (drafts.contains(name) && !save.replace()) {
            channel.send(new NameTaken(name));
            return;
        }
        // Run off the draft's own thread, because closing the draft interrupts that thread
        draft.save(name).whenCompleteAsync((group, ex) -> {
            if (ex != null) {
                session.tell(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionDraftSaveFailed", String.valueOf(ex.getMessage()))));
                return;
            }
            OfflinePools.store(drafts, group);
            endDraft();
            final Stage now = session.stage();
            if (now instanceof Event) {
                session.move(now, new Event(EventKind.draft, name));
            }
            session.tell(OfflinePools.pools());
            decks.openPool(group.getHumanDeck(), drafts, poolType(GameType.Draft, group.getHumanDeck()), draft.freeCommander(), session.browser());
        });
    }

    /** A drafted pool is a Commander Draft by the same test LimitedMatch uses. */
    private static GameType poolType(final GameType type, final Deck human) {
        return type == GameType.Draft && CommanderDraftUtil.isCommanderDraftDeck(human)
                ? GameType.CommanderDraft : type;
    }

    /** Plays a pool's deck against one opponent, several at once, or each in turn as a gauntlet. Afterwards it returns to the pool. */
    private void playPool(final BrowserChannel channel, final PoolPlay play) {
        final Event event = session.stage() instanceof Event e ? e : null;
        final DeckGroup group = event == null ? null : event.storage().get(play.name());
        if (!session.hosting() || group == null) {
            return;
        }
        final List<Deck> ai = group.getAiDecks();
        final Deck human = group.getHumanDeck();
        final GameType limitedType = poolType(event.type(), human);
        final String illegal = DeckFacts.problem(human, limitedType, null);
        if (illegal != null) {
            channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionYourDeck", illegal)));
            return;
        }
        if (play.games() == 1 || play.games() == 3 || play.games() == 5) {
            FModel.getPreferences().setPref(FPref.UI_MATCHES_PER_GAME, String.valueOf(play.games()));
            FModel.getPreferences().save();
        }
        final LimitedMatch limited = FModel.getLimitedMatch();
        final PoolPlayMode mode = play.mode() == null ? PoolPlayMode.one : play.mode();
        final List<Integer> numbers = new ArrayList<>();
        if (mode == PoolPlayMode.several) {
            final int count = Math.min(Math.min(play.count(), MOST_OPPONENTS), ai.size());
            if (event.type() != GameType.Draft || count < 2) {
                channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionSeveralNeedsDraft")));
                return;
            }
            // Opponents are chosen at random, as on desktop's draft screen
            for (int i = 1; i <= ai.size(); i++) {
                numbers.add(i);
            }
            Collections.shuffle(numbers);
            numbers.subList(count, numbers.size()).clear();
        } else if (mode == PoolPlayMode.one) {
            if (play.opponent() < 0 || play.opponent() >= ai.size()) {
                channel.send(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionNoSuchOpponent")));
                return;
            }
            numbers.add(play.opponent() + 1);
        }
        matchBack = new Event(event.kind(), play.name());
        matchType = limitedType;
        opponentNumbers = List.copyOf(numbers);
        gauntletRunning = mode == PoolPlayMode.gauntlet;
        lastResult = null;
        if (gauntletRunning) {
            // If the first round did not start, the pool stays on screen and there is no gauntlet to continue
            if (!playRound(() -> limited.launchGauntlet(event.type(), human, ai.size()))) {
                stopGauntlet();
            }
        } else {
            final List<Deck> opponents = numbers.stream().map(n -> ai.get(n - 1)).toList();
            playRound(() -> limited.startMatch(event.type(), human, opponents));
        }
    }

    /**
     * Opens a table where the browser sits alone, then runs start, which has LimitedMatch start the match there, as desktop's
     * Limited screens do. False when the match did not start.
     */
    private boolean playRound(final Runnable start) {
        final Event back = matchBack;
        final Stage from = session.stage();
        final Playing playing = new Playing(new WebGuiGame(session.settings()), false, false, back);
        if (back == null || !session.move(from, playing)) {
            playing.gui().close();
            return false;
        }
        session.attach(playing.gui());
        playing.gui().onGameOver(this::recordGauntletGame);
        try {
            final ServerGameLobby table = local.openAlone(session.playerName(), playing.gui());
            table.setLimitedType(matchType);
            final int seat = local.webSeat();
            ui.playSolo(new WebGuiBase.SoloTable(() -> FServerManager.getInstance().getGui(seat), (match, rules, players, human, begin) -> {
                dressRound(players, human);
                begin.run();
                table.adopt(match, Map.of(human, table.getSlot(seat)));
            }));
            start.run();
            return true;
        } catch (final RuntimeException ex) {
            netLog.error(ex, "Could not start the match");
            local.endMatch();
            session.move(playing, back);
            session.tell(WebSession.error(Localizer.getInstance().getMessage("lblWebSessionMatchFailed", String.valueOf(ex.getMessage()))));
            return false;
        } finally {
            ui.playSolo(null);
        }
    }

    /**
     * Names the players as desktop numbers a pool's opponents, gives each opponent the avatar and sleeve of its seat, and keeps
     * the player the result is read for.
     */
    private void dressRound(final List<RegisteredPlayer> players, final RegisteredPlayer me) {
        int seat = 0;
        for (final RegisteredPlayer player : players) {
            final LobbyPlayer dressed;
            if (player == me) {
                dressed = GamePlayerUtil.getGuiPlayer(session.playerName(), session.avatarIndex(), NetplaySeat.storedIndex(FPref.UI_SLEEVES, 0), false);
                matchPlayer = dressed;
            } else {
                final int number = gauntletRunning ? FModel.getLimitedMatch().getCurrentRound() : opponentNumbers.get(seat);
                seat++;
                dressed = GamePlayerUtil.createAiPlayer(Localizer.getInstance().getMessage("lblWebSessionOpponent", number),
                        NetplaySeat.storedIndex(FPref.UI_AVATARS, seat), NetplaySeat.storedIndex(FPref.UI_SLEEVES, seat));
            }
            dressed.setSleeveArtKey(player.getDeck().getSleeveArtKey());
            player.setPlayer(dressed);
        }
    }

    /**
     * A gauntlet game ended. The win or loss is recorded after every game, as LimitedWinLoseController records it from the host's Match.
     * That controller is not used because it needs a result screen's buttons.
     */
    private void recordGauntletGame() {
        final HostedMatch hosted = local.hostedMatch();
        final LobbyPlayer me = matchPlayer;
        if (!gauntletRunning || hosted == null) {
            return;
        }
        final Match match = hosted.getMatch();
        final LimitedMatch gauntlet = FModel.getLimitedMatch();
        if (match.getLastOutcome().isWinner(me)) {
            gauntlet.addWin();
        } else {
            gauntlet.addLoss();
        }
        lastResult = new LimitedResult(gauntlet.getCurrentRound(), gauntlet.getRounds(), gauntlet.getWins(), gauntlet.getLosses(),
                match.isMatchOver(), match.isWonBy(me) && gauntlet.getCurrentRound() < gauntlet.getRounds());
        session.tell(lastResult);
    }

    /** Sends a reconnecting browser the gauntlet result it was last shown. */
    public void resendResult(final BrowserChannel channel) {
        final LimitedResult result = lastResult;
        if (result != null) {
            channel.send(result);
        }
    }

    /** Leaving a gauntlet ends it, as desktop's Quit does. */
    public void stopGauntlet() {
        if (!gauntletRunning) {
            return;
        }
        gauntletRunning = false;
        lastResult = null;
        FModel.getLimitedMatch().resetCurrentRound();
    }
}

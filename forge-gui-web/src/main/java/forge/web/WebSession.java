package forge.web;

import com.google.gson.JsonObject;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.LogSafe;
import forge.util.storage.IStorage;
import forge.web.deck.CardCatalog;
import forge.web.deck.DeckCatalog;
import forge.web.deck.DeckWorkspace;
import forge.web.images.SkinSprites;
import forge.web.limited.EventSeat;
import forge.web.limited.OfflineDraft;
import forge.web.limited.OfflinePools;
import forge.web.limited.OfflineLimited;
import forge.web.lobby.Lobby;
import forge.web.lobby.LobbyFormats;
import forge.web.match.NetplaySeat;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.Commands;
import forge.web.protocol.Commands.Handler;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser.*;
import forge.web.protocol.MessageJson;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One browser's session. Its current place is one {@link Stage}, which changes only through {@link #move}, apart from the session closing. Stages include the menu,
 * the table and a match. The session holds or gives up the host's seat, shows a browser that connects again the place it left, and
 * passes each message to the part that handles it.
 */
public final class WebSession implements IHasForgeLog {
    private static final int CARD_SEARCH_LIMIT = 60;
    /** Long enough for any name a player types, short enough to fit on a seat plate. */
    static final int MAX_NAME_LENGTH = 24;
    private static final AtomicInteger SESSIONS = new AtomicInteger();
    /** The commands the session handles itself. The rest belong to the table, the deck editor, the Limited pages or the match. */
    public static final Commands<WebSession> COMMANDS = new Commands<WebSession>()
            .on(PageError.class, (s, ch, e) -> netLog.warn("Page error for {}: {}", s.label(), LogSafe.forLog(e.text(), 1500)))
            .on(SetName.class, (s, ch, n) -> s.rename(ch, n.name(), n.avatar(), n.sleeve()))
            .on(ClaimHost.class, (s, ch, c) -> s.claimHost(ch, c.password()))
            // Opening a game connects a client to the server, which is done on the host UI thread
            .on(Plain.lobby, (s, ch, c) -> s.ui.invokeInEdtLater(() -> s.openLobby(ch, false, null)))
            .on(Plain.leaveLobby, (s, ch, c) -> s.ui.invokeInEdtLater(s::leaveLobby))
            // One set of host questions serves the whole process, so only the host's browser may answer them
            .on(HostChoiceAnswer.class, host((s, ch, a) -> s.ui.hostQuestions().answer(a.id(), a.value())))
            .on(Plain.cardSets, (s, ch, c) -> ch.send(CardCatalog.sets()))
            .on(SearchCards.class, (s, ch, q) -> s.decks.inTurn(() -> ch.send(new CardSearch(CardCatalog.searchCardNames(q.query(), CARD_SEARCH_LIMIT)))))
            .on(AskPrintings.class, (s, ch, a) -> s.decks.inTurn(() -> ch.send(new Printings(a.name(), CardCatalog.printings(a.name(),
                    a.cardPool() == null ? null : FModel.getFormats().getFormat(a.cardPool()))))))
            // Starting a match can ask host questions, so it runs on the host UI thread and never blocks a socket thread
            .on(Start.class, (s, ch, st) -> s.ui.invokeInEdtLater(() -> s.start(ch, st)))
            .on(Plain.leave, (s, ch, c) -> s.ui.invokeInEdtLater(s::leave))
            // A cheat asks its questions as the game does and waits for the answers, so it runs on its own thread
            .on(Dev.class, host((s, ch, d) -> {
                if (s.stage instanceof Playing playing) {
                    s.ui.runBackgroundTask("Dev mode", () -> DevMode.run(s.local, playing.gui(), d, ch));
                }
            }))
            // A setting changed before a match is used to set up the match. During a match the game is told as well
            .on(SetSetting.class, (s, ch, set) -> {
                if (s.stage instanceof Playing p) {
                    p.gui().setSetting(set);
                } else {
                    // The host's settings are Forge's preferences, which are written on the host UI thread
                    s.ui.invokeInEdtLater(() -> Options.set(s.settings, null, set.key(), set.value()));
                }
            })
            .on(SetStops.class, (s, ch, stops) -> {
                if (s.stage instanceof Playing p) {
                    p.gui().setStops(stops);
                } else {
                    s.ui.invokeInEdtLater(() -> Options.setStops(s.settings, stops.mine(), stops.phases()));
                }
            });
    private final Lobby lobby;
    /** The deck editor and importer. They open beside the menu or the table and are not a stage of their own. */
    private final DeckWorkspace decks;
    private final WebGuiBase ui;
    private final WebHub sessions;
    /** True for the browser that claimed the host's seat. Only that browser may set up the table. */
    private volatile boolean isHost;
    /** Whether this browser is on the machine running the server. It takes the host's seat without the host password. */
    private final boolean onThisMachine;
    private final NetplaySeat local = new NetplaySeat();
    private volatile BrowserChannel browser;
    private volatile Stage stage = new Menu();
    /** This player's settings: a guest's own, or the host's, which are Forge's preferences. */
    private volatile PlayerSettings settings = PlayerSettings.fresh();
    /** The name this browser chose to play under. Null until it chooses one. */
    private volatile String name;
    /** The avatar chosen next to the name, held here before any seat exists. Null until it chooses one. */
    private volatile Integer avatar;
    /**
     * Tells sessions apart. A browser that is moved to another session, such as a seat handed over, sees the number change. The log also uses it,
     * because the player may have no name yet, or the same name as another.
     */
    private final int number = SESSIONS.incrementAndGet();
    /** When the browser last left, or 0 if it has never left. */
    private volatile long leftAt;

    /** Where the browser is. Each stage holds only what exists at that stage. */
    public sealed interface Stage permits Menu, Opening, Setup, Event, Playing { }

    /** The start page. For a guest, this is waiting for the host to open a game. */
    public record Menu() implements Stage { }

    /** Taking a seat. The table is built while the browser waits, and nothing may change it until the seat is taken. */
    record Opening() implements Stage { }

    /** Match setup, at a seat whose GUI the match will be played through. Invited means others can join. */
    public record Setup(WebGuiGame gui, boolean invited) implements Stage { }

    /** The Limited pages for a kind of event: the saved pools and the setup form. When pool is not null, that pool's opponents screen is shown. */
    public record Event(EventKind kind, String pool) implements Stage {
        public IStorage<DeckGroup> storage() {
            return OfflinePools.storage(kind);
        }

        public GameType type() {
            return kind == EventKind.draft ? GameType.Draft : GameType.Sealed;
        }
    }

    /** In a match, playing it or watching the computer play it. back is where leaving returns to: a pool, or null for the table. */
    public record Playing(WebGuiGame gui, boolean invited, boolean spectating, Stage back) implements Stage { }

    /** This session's name in the log. */
    String label() {
        final String who = playerName();
        return "#" + number + " " + (StringUtils.isBlank(who) ? "unnamed" : who);
    }

    /** How long the browser was gone before it came back just now, or -1 on its first connection. */
    long secondsAway() {
        final long left = leftAt;
        return left == 0 ? -1 : (System.currentTimeMillis() - left) / 1000;
    }

    WebSession(final WebGuiBase ui, final WebHub sessions, final boolean onThisMachine) {
        this.onThisMachine = onThisMachine;
        this.ui = ui;
        this.sessions = sessions;
        final Map<String, DeckCatalog.OnDevice> device = new ConcurrentHashMap<>();
        this.lobby = new Lobby(local, () -> !isHost, device, ui);
        this.decks = new DeckWorkspace(lobby, ui, () -> isHost, () -> stage instanceof Setup, device);
        this.limited = new OfflineLimited(this, ui, lobby, decks, local);
        this.eventSeat = new EventSeat(this, ui, lobby, decks, local, sessions::hostLobby, sessions::seatsChanged);
        this.table = new TableCommands(this, lobby, decks, limited, ui, sessions);
    }

    /** Takes the host's seat. Host dialogs go to whoever holds the seat, so they are sent to this session. */
    void becomeHost() {
        isHost = true;
        settings = PlayerSettings.saved();
        ui.setNoticeSink(notice -> {
            final BrowserChannel b = browser;
            if (b != null) {
                b.send(notice);
            }
        });
    }

    /** Gives up the host's seat, which another browser may then take. */
    void loseHost() {
        isHost = false;
        settings = PlayerSettings.fresh();
        local.disownServer();
    }

    private boolean needsHostPassword() {
        return !onThisMachine && ServerControl.hostPasswordSet();
    }

    /** Where the browser is. Only {@link #move} changes it, apart from the session closing. */
    public Stage stage() {
        return stage;
    }

    /** Whether this session is holding a game open. This keeps the host's seat reserved. */
    boolean hasGame() {
        final Stage now = stage;
        return now instanceof Setup || now instanceof Event || now instanceof Playing;
    }

    /** Moves to the next stage only if the browser is still at the stage the caller saw. The old GUI is closed unless the new stage keeps it. */
    public synchronized boolean move(final Stage from, final Stage next) {
        if (stage != from) {
            return false;
        }
        stage = next;
        netLog.info("{}: {} to {}", label(), from.getClass().getSimpleName(), next.getClass().getSimpleName());
        final WebGuiGame old = guiOf(from);
        if (old != null && old != guiOf(next)) {
            old.close();
        }
        tell(hello());
        // The panel of who is here shows every stage: waiting, joining, at a table or playing
        sessions.announcePresence();
        return true;
    }

    private static WebGuiGame guiOf(final Stage stage) {
        if (stage instanceof Setup s) {
            return s.gui();
        }
        return stage instanceof Playing p ? p.gui() : null;
    }

    /** The host's seat was freed or taken, so a waiting browser is told again what it may do. */
    void hostSeatChanged() {
        if (!isHost) {
            tell(hello());
        }
    }

    public PlayerSettings settings() {
        return settings;
    }

    /** The saved player name belongs to the host, so only a host that has not chosen a name plays under it. */
    public String playerName() {
        final String chosen = name;
        return chosen != null || !isHost ? chosen : FModel.getPreferences().getPref(FPref.PLAYER_NAME);
    }

    Lobby lobby() {
        return lobby;
    }

    public boolean attached() {
        return browser != null;
    }

    /** Whether this browser holds the host's seat. It can hold the seat before it opens a table, unlike {@link NetplaySeat#isHost}. */
    public boolean hosting() {
        return isHost;
    }

    /** The avatar chosen on the start page. The first avatar until one is chosen. */
    public int avatarIndex() {
        final Integer face = avatar;
        return face == null ? 0 : face;
    }

    public void tell(final Record message) {
        final BrowserChannel b = browser;
        if (b != null) {
            b.send(message);
        }
    }

    /** Shows the browser the game that a match is played through, when a browser is attached. */
    public void attach(final WebGuiGame gui) {
        final BrowserChannel b = browser;
        if (b != null) {
            gui.attach(b);
        }
    }

    /** A browser connected, for the first time or again. It is shown the place that its stage records. */
    synchronized void connected(final BrowserChannel channel) {
        final BrowserChannel previous = browser;
        browser = channel;
        decks.attach(channel);
        final Stage now = stage;
        if (previous != null) {
            // Another tab of this browser took over, so the old tab must not act for this seat
            previous.close();
            if (now instanceof Playing p) {
                p.gui().detach(previous);
            }
        }
        channel.send(hello());
        // These are the same for the whole run, so a browser gets them once instead of with every table change
        channel.send(new Formats(LobbyFormats.choices()));
        if (isHost) {
            ui.hostQuestions().replay(channel::send);
        }
        decks.reconnected(channel);
        if (now instanceof Playing p) {
            p.gui().attach(channel);
            limited.resendResult(channel);
        } else if (now instanceof Setup) {
            // Match setup is drawn from the table, and only these messages describe the table
            lobby.queueDecks(channel, decks::inTurn);
            lobby.sendState(channel);
            if (isHost && lobby.events().settingUpEvent()) {
                table.sendEventOptions(channel);
            }
            eventSeat.reconnected(channel);
        } else if (now instanceof Event) {
            limited.sendLimited(channel);
            final OfflineDraft draft = limited.draft();
            if (draft != null && draft.latest() != null) {
                channel.send(draft.latest());
            }
        } else if (now instanceof Menu) {
            // A guest that arrives while a game is open takes a seat without being asked, once it has a name
            joinHostGame();
        }
    }

    synchronized void disconnected(final BrowserChannel channel) {
        if (browser != channel) {
            return;
        }
        browser = null;
        leftAt = System.currentTimeMillis();
        decks.attach(null);
        if (stage instanceof Playing p) {
            p.gui().detach(channel);
        }
    }

    private static <T extends Record> Handler<WebSession, T> host(final Handler<WebSession, T> handler) {
        return (s, ch, c) -> {
            if (s.isHost) {
                handler.run(s, ch, c);
            }
        };
    }

    void onMessage(final BrowserChannel channel, final JsonObject msg) {
        final Record command = MessageJson.decode(msg);
        if (command == null) {
            netLog.warn("Unknown browser message {}", LogSafe.forLog(msg.toString(), 200));
        } else if (COMMANDS.run(this, channel, command) || TableCommands.COMMANDS.run(table, channel, command)
                || OfflineLimited.COMMANDS.run(limited, channel, command)) {
            return;
        } else if (DeckWorkspace.COMMANDS.handles(command)) {
            deckCommand(channel, command);
        } else if (WebGuiGame.COMMANDS.handles(command)) {
            if (stage instanceof Playing p) {
                p.gui().onCommand(channel, command);
            }
        }
    }

    /** A deck editor or importer command. A match fills the whole page, so deck commands are ignored during one. */
    private void deckCommand(final BrowserChannel channel, final Record command) {
        if (stage instanceof Playing) {
            return;
        }
        final boolean closing = command instanceof Bare b && b.t() == Plain.editorClose && stage instanceof Event;
        // The deck thread keeps editor saves and imports off the socket threads, in order with deck searches
        decks.inTurn(() -> {
            // Send the pools first, because closing the editor lists every deck again before the page changes
            if (closing) {
                channel.send(OfflinePools.pools());
            }
            DeckWorkspace.COMMANDS.run(decks, channel, command);
        });
    }

    private void claimHost(final BrowserChannel channel, final String password) {
        if (needsHostPassword() && !ServerControl.hostPasswordMatches(password)) {
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionWrongHostPassword")));
        } else if (!sessions.claimHost(this)) {
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionAlreadyHosting")));
        }
        channel.send(hello());
    }

    private void leaveLobby() {
        final Stage now = stage;
        if (now instanceof Setup && move(now, new Menu())) {
            local.endMatch();
            if (isHost) {
                sessions.hostGameClosed();
            }
        }
    }

    /** Opens match setup: a game of this machine's own, set up for an event when limited is not null, or a seat in the host's game. Tells the browser the result. */
    private void openLobby(final BrowserChannel channel, final boolean invite, final EventKind limited) {
        if (!isHost) {
            joinHostGame();
            return;
        }
        final Stage from = stage;
        final Opening opening = new Opening();
        // Leaving the old stage closes its match or table before the new one is built
        if (from instanceof Opening || !move(from, opening)) {
            return;
        }
        // A new table replaces the old one, so anyone seated at the old table is told it has closed before it is removed
        if (from instanceof Setup || from instanceof Playing) {
            sessions.hostGameClosed();
        }
        final WebGuiGame gui = new WebGuiGame(settings);
        lobby.forget();
        lobby.setShareable(invite);
        eventSeat.newTable(gui);
        try {
            local.openHost(playerName(), gui, this::lobbyChanged, this::chatted);
            if (invite) {
                // Someone must be able to sit down, so the second seat is left open instead of filled
                lobby.openSeat(1);
            }
            if (limited == null) {
                lobby.restoreSetup(invite);
            }
        } catch (final RuntimeException e) {
            netLog.error(e, "Could not open the lobby");
            gui.close();
            move(opening, new Menu());
            // The move's hello clears the browser's last error, so the reason must be sent after it
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionLobbyFailed", String.valueOf(e.getMessage()))));
            return;
        }
        if (!move(opening, new Setup(gui, invite))) {
            gui.close();
            return;
        }
        applyChosenAvatar();
        if (limited != null) {
            table.setLimited(channel, limited);
        }
        lobby.queueDecks(channel, decks::inTurn);
        lobby.sendState(channel);
        sessions.hostGameOpened();
    }

    /** The table changed. The browser is shown it only once it is set up. Until then it is still being built. */
    void lobbyChanged() {
        ui.invokeInEdtLater(() -> {
            if (isHost) {
                sessions.seatsMayHaveChanged();
            }
            final BrowserChannel b = browser;
            if (b != null && stage instanceof Setup) {
                // A guest learns the host's format or card pool only here, so its deck list is rebuilt here too
                lobby.relistDecks(b, decks::inTurn);
                lobby.sendState(b);
            }
        });
    }

    /** Takes the name this browser asked for, unless the name has a problem. A guest waiting for a name then takes its seat. */
    private void rename(final BrowserChannel channel, final String wanted, final Integer face, final Integer back) {
        final String problem = nameProblem(wanted);
        if (problem != null) {
            channel.send(error(problem));
            return;
        }
        netLog.info("{} takes the name {}.", label(), LogSafe.forLog(wanted.trim()));
        name = wanted.trim();
        if (face != null) {
            avatar = face;
        }
        if (back != null) {
            sleeve = back;
        }
        ui.invokeInEdtLater(this::applyChosenAvatar);
        channel.send(hello());
        sessions.announcePresence();
        joinHostGame();
    }

    /** Puts the avatar chosen on the start page on this session's seat, once it has a seat. A guest's remembered sleeve goes on it too. */
    private void applyChosenAvatar() {
        final Integer face = avatar;
        final Integer back = sleeve;
        final int seat = local.webSeat();
        if (seat < 0 || !(stage instanceof Setup)) {
            return;
        }
        if (face != null) {
            lobby.setAvatar(seat, face);
        }
        // A guest's seat starts with the host's sleeve, because the netplay login reads the host's preferences
        if (back != null && !isHost) {
            lobby.setSleeve(seat, back);
        }
    }

    /** Why a name cannot be this player's, or null if it can. */
    private String nameProblem(final String wanted) {
        final String trimmed = wanted == null ? "" : wanted.trim();
        if (trimmed.isEmpty()) {
            return Localizer.getInstance().getMessage("lblWebSessionNameEmpty");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            return Localizer.getInstance().getMessage("lblWebDeckStoreNameTooLong", MAX_NAME_LENGTH);
        }
        if (sessions.nameTaken(trimmed, this)) {
            return Localizer.getInstance().getMessage("lblWebSessionNameTaken", trimmed);
        }
        return null;
    }

    void applySeat(final BrowserChannel channel, final SetSeat seat) {
        // The name on the player's own seat is the name they play under, so it follows the same rules as the first name they chose
        if (seat.name() != null && seat.index() == local.webSeat()) {
            final String problem = nameProblem(seat.name());
            if (problem != null) {
                // Match setup has no line for errors, and the seat keeps its old name, so a notice is enough
                channel.send(new Notice(Localizer.getInstance().getMessage("lblWebSessionNameNotChanged"), problem, false));
            } else {
                name = seat.name().trim();
                lobby.setName(seat.index(), name);
            }
        } else if (seat.name() != null) {
            lobby.setName(seat.index(), seat.name());
        }
        if (seat.deck() != null) {
            lobby.setDeck(seat.index(), seat.deck());
        }
        if (seat.avatar() != null && lobby.setAvatar(seat.index(), seat.avatar()) && seat.index() == local.webSeat()) {
            // The avatar on the player's own seat is the one shown for them everywhere else too
            avatar = seat.avatar();
            sessions.announcePresence();
        }
        if (seat.sleeve() != null) {
            lobby.setSleeve(seat.index(), seat.sleeve());
            if (seat.index() == local.webSeat()) {
                sleeve = seat.sleeve();
            }
        }
        if (seat.commander() != null) {
            lobby.setCommander(seat.index(), seat.commander());
        }
        if (seat.aiProfile() != null) {
            lobby.setAiProfile(seat.index(), seat.aiProfile());
        }
    }

    public Hello hello() {
        final Stage now = stage;
        return new Hello(screen(now), now instanceof Playing p && p.spectating(), isHost,
                // Nobody becomes host by arriving, so a browser is offered the host's seat whenever it is free
                !isHost && sessions.hostSeatFree(), needsHostPassword(),
                // A game with no invited players has nobody to talk to, so the browser hides the chat completely
                invited(now) || !isHost,
                playerName(),
                // Seat 0 is the player and seat 1 the opponent, as in the desktop lobby's saved choices
                seatIndices(FPref.UI_AVATARS), seatIndices(FPref.UI_SLEEVES),
                SkinSprites.avatarCount(), SkinSprites.sleeveCount(), DeckCatalog.savedSleeveArt(),
                now instanceof Event e ? e.pool() : null, isHost ? OfflinePools.sealed().size() : 0,
                now instanceof Event e ? e.kind() : null, limited.draft() != null || (now instanceof Setup && eventSeat.drafting()),
                isHost ? OfflinePools.storage(EventKind.draft).size() : 0,
                // The jar's manifest holds the version, so a server run from classes has none
                WebSession.class.getPackage().getImplementationVersion(),
                // The menu's volume slider and music need the settings before any match sends them with its controls
                Options.values(settings), number);
    }

    private static Screen screen(final Stage stage) {
        if (stage instanceof Opening) {
            return Screen.opening;
        }
        if (stage instanceof Setup) {
            return Screen.setup;
        }
        if (stage instanceof Event) {
            return Screen.event;
        }
        return stage instanceof Playing ? Screen.playing : Screen.menu;
    }

    static boolean invited(final Stage stage) {
        if (stage instanceof Setup s) {
            return s.invited();
        }
        return stage instanceof Playing p && p.invited();
    }

    private static List<Integer> seatIndices(final FPref pref) {
        return List.of(NetplaySeat.storedIndex(pref, 0), NetplaySeat.storedIndex(pref, 1));
    }

    public static ErrorMessage error(final String message) {
        return new ErrorMessage(message);
    }

    private void start(final BrowserChannel channel, final Start msg) {
        if (!isHost) {
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionHostStarts")));
            return;
        }
        final Stage from = stage;
        if (!(from instanceof Setup setup)) {
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionNoLobby")));
            return;
        }
        final List<String> problems = lobby.problems();
        if (!problems.isEmpty()) {
            channel.send(error(problems.get(0)));
            return;
        }
        // Saves the seats' looks and the host's name, so HostedMatch does not ask for a name
        lobby.saveLooks();
        // Prepared before leaving the lobby, so the page stays where it was if the host backs out of the illegal-deck question
        final Runnable begin;
        try {
            begin = local.prepare();
        } catch (final RuntimeException e) {
            netLog.error(e, "Could not start the match");
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionMatchFailed", String.valueOf(e.getMessage()))));
            return;
        }
        if (begin == null) {
            return;
        }
        final Playing playing = new Playing(setup.gui(), setup.invited(), msg.spectate(), null);
        if (!move(from, playing)) {
            return;
        }
        attach(playing.gui());
        try {
            begin.run();
            if (playing.spectating()) {
                local.spectate();
            }
        } catch (final RuntimeException e) {
            netLog.error(e, "Could not start the match");
            local.endMatch();
            move(playing, new Menu());
            // The table closed with the match, and nothing else tells the guests seated at it
            sessions.hostGameClosed();
            channel.send(error(Localizer.getInstance().getMessage("lblWebSessionMatchFailed", String.valueOf(e.getMessage()))));
        }
    }

    /** Leaves the match: to the pool it came from, to a new table of the same kind for the host, or to the menu for a guest. */
    private void leave() {
        final Stage from = stage;
        if (!(from instanceof Playing playing)) {
            return;
        }
        final BrowserChannel b = browser;
        if (playing.back() != null) {
            limited.stopGauntlet();
            local.endMatch();
            if (move(from, playing.back()) && b != null) {
                limited.sendLimited(b);
            }
            return;
        }
        if (isHost && b != null) {
            // An event's matches follow one another, so the new table plays the same event's decks
            final ServerGameLobby was = local.hostedLobby();
            final GameLobbyData data = was == null ? null : was.getData();
            final String event = data != null && data.isLimitedMode() ? data.getActiveEventId() : null;
            final GameType type = data == null ? null : data.getLimitedType();
            final boolean decksOnly = data != null && data.isActiveConformance();
            // The new table gets the old table's settings, so a rematch needs no new choices
            final Lobby.Kept kept = event == null ? lobby.keep() : null;
            // Opening the lobby leaves the match and closes it
            openLobby(b, playing.invited(), null);
            if (event != null) {
                lobby.events().playEvent(event, type, decksOnly);
                lobby.relistDecks(b, decks::inTurn);
            } else if (kept != null) {
                lobby.restore(kept);
                lobby.relistDecks(b, decks::inTurn);
                lobby.sendState(b);
            }
        } else if (move(from, new Menu())) {
            local.endMatch();
            joinHostGame();
        }
    }

    /** Releases this browser's match and seat without stopping the process. */
    void shutdown() {
        closeGui();
        local.shutdown();
    }

    /** Each GUI holds its own thread, so a closing session must close its GUI. */
    private synchronized void closeGui() {
        final WebGuiGame gui = guiOf(stage);
        stage = new Menu();
        if (gui != null) {
            gui.close();
        }
    }
}

package forge.web;

import com.google.gson.JsonObject;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.HostingServer;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.ToBrowser.Address;
import forge.web.protocol.ToBrowser.ChatLine;
import forge.web.protocol.ToBrowser.Person;
import forge.web.protocol.ToBrowser.Presence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

/** Every browser attached to this process, each with its own session. Only the web port must be reachable from outside. */
final class WebHub implements WebServer.Endpoint, IHasForgeLog {
    /** The most browsers tracked at once, because anyone with an invite link can open one browser after another. */
    static final int MOST_SESSIONS = 64;
    /** How long the host's seat stays reserved for a browser that has left, so a reload keeps the seat. */
    private static final int HOST_GRACE_MILLIS = 20_000;
    /** Enough of the conversation for a browser that arrives late to see what was said. */
    private static final int CHAT_KEPT = 60;
    private final WebGuiBase ui;
    private final long idleMillis;
    private final Runnable onQuit;
    /** Sessions by the id that their browser stores. A reload then returns to the seat it left instead of taking a new one. */
    private final Map<String, WebSession> byId = new ConcurrentHashMap<>();
    private final Map<BrowserChannel, WebSession> byChannel = new ConcurrentHashMap<>();
    private volatile WebSession host;
    private volatile WebServer server;
    /** Set from the console's checkbox or the keepOpen launch flag. Without a console, nothing else shows that Forge is running. */
    private boolean quitWhenEmpty = true;
    private volatile boolean stopping;
    /** The app players in the last list of who is here. It is used to detect when one arrives or leaves. */
    private volatile List<String> appSeatNames = List.of();
    /** Runs when no browser has been connected for a while. That is the only sign that the players have finished. */
    private ScheduledFuture<?> idle;
    /** Whether the router has agreed to forward the port. This decides how the internet link is described. */
    private volatile boolean portForwarded;
    private final Deque<ChatLine> said = new ArrayDeque<>();

    WebHub(final WebGuiBase ui, final long idleMillis, final Runnable onQuit) {
        this.ui = ui;
        this.idleMillis = idleMillis;
        this.onQuit = onQuit;
        // This also covers a browser that never opens
        idle = ThreadUtil.delay((int) idleMillis, onQuit);
    }

    void portForwarded(final boolean value) {
        portForwarded = value;
    }

    /** The server is built around this endpoint, so it can only be passed in after the server exists. */
    void setServer(final WebServer server) {
        this.server = server;
    }

    @Override
    public void connected(final BrowserChannel channel, final String clientId, final boolean local) {
        final WebSession session = sessionFor(channel, clientId, local);
        if (session == null) {
            netLog.warn("Turned a browser away: {} are already here.", MOST_SESSIONS);
            channel.close();
            return;
        }
        // A tab replaced by this one is ignored until its socket closes
        byChannel.values().removeIf(s -> s == session);
        byChannel.put(channel, session);
        holdOpen();
        final long away = session.secondsAway();
        if (away < 0) {
            netLog.info("{} arrived from {}.", session.label(), local ? "this machine" : "another machine");
        } else {
            netLog.info("{} came back after {} s.", session.label(), away);
        }
        session.connected(channel);
        // Greet after the session releases its own lock, because shutdown takes this lock and then each session's lock
        greet(session);
        // If this browser holds a seat, the others were told that it had left
        announcePresence();
    }

    /** Stops the process from quitting before anyone connects. Used when the console shows that Forge is running. */
    synchronized void visibleElsewhere() {
        holdOpen();
    }

    /** Whether the host's seat is free. The browser offers the seat when this is true. */
    boolean hostSeatFree() {
        return host == null;
    }

    /** Takes the host's seat for this session if it is still free. If two claims arrive at once, only one wins. */
    synchronized boolean claimHost(final WebSession session) {
        if (host != null) {
            return host == session;
        }
        host = session;
        session.becomeHost();
        netLog.info("A browser has taken the host's seat.");
        announceSeat();
        announcePresence();
        return true;
    }

    /** Frees the host's seat once its browser has been gone for a while, unless the session holds a game. */
    private synchronized void releaseHostIfAbandoned() {
        if (host == null || host.attached() || host.hasGame()) {
            return;
        }
        netLog.info("The host's browser has not come back. The seat is free again.");
        host.loseHost();
        host = null;
        announceSeat();
    }

    /** The process stays alive while any browser is attached, so the host closing their browser does not end a guest's game. */
    private synchronized void holdOpen() {
        if (idle != null) {
            idle.cancel(false);
            idle = null;
        }
    }

    /** Sets whether the process ends once the last browser has left. Turning it on takes effect the next time a browser leaves. */
    synchronized void quitWhenEmpty(final boolean value) {
        quitWhenEmpty = value;
        if (!value && idle != null) {
            idle.cancel(false);
            idle = null;
        }
    }

    private synchronized void letGo() {
        if (quitWhenEmpty && idle == null && byChannel.isEmpty()) {
            netLog.info("No browser is attached. Forge will close in {} seconds.", idleMillis / 1000);
            idle = ThreadUtil.delay((int) idleMillis, this::quitUnlessAppsSeated);
        }
    }

    /** A desktop or mobile player at the host's table has no browser, so the wait starts again while one is seated. */
    private void quitUnlessAppsSeated() {
        synchronized (this) {
            idle = null;
            if (stopping || !quitWhenEmpty || !byChannel.isEmpty()) {
                return;
            }
            if (!appSeats().isEmpty()) {
                letGo();
                return;
            }
        }
        onQuit.run();
    }

    /** The session a browser returns to, or a new one, or null when full. The key includes where the browser is, so another machine never reaches this machine's session. */
    private synchronized WebSession sessionFor(final BrowserChannel channel, final String clientId, final boolean local) {
        final String id = clientId.isEmpty() ? String.valueOf(System.identityHashCode(channel)) : clientId;
        final String key = (local ? "local:" : "remote:") + id;
        final WebSession known = byId.get(key);
        if (known != null) {
            return known;
        }
        if (byId.size() >= MOST_SESSIONS) {
            byId.values().removeIf(s -> !s.attached() && !s.hasGame() && s != host);
            if (byId.size() >= MOST_SESSIONS) {
                return null;
            }
        }
        // Nobody becomes host by arriving. A browser asks for the seat, and the first to ask while it is free gets it
        final WebSession session = new WebSession(ui, this, local);
        byId.put(key, session);
        return session;
    }

    @Override
    public void disconnected(final BrowserChannel channel) {
        // Stopping the server closes every browser's socket after the sessions have shut down
        if (stopping) {
            return;
        }
        final WebSession session = byChannel.remove(channel);
        if (session != null) {
            netLog.info("{} left.", session.label());
            session.disconnected(channel);
            announcePresence();
            ThreadUtil.delay(DRAFT_HOLD_MILLIS, session.eventSeat()::goneAWhile);
        }
        letGo();
        // A seat held by a browser that never comes back would leave nobody able to set up the table
        ThreadUtil.delay(HOST_GRACE_MILLIS, this::releaseHostIfAbandoned);
    }

    @Override
    public void onMessage(final BrowserChannel channel, final JsonObject message) {
        final WebSession session = byChannel.get(channel);
        if (session != null) {
            session.onMessage(channel, message);
        }
    }

    /** The seated guest with this name. A session whose browser left can still hold the seat. */
    private WebSession seatedGuest(final String name) {
        for (final WebSession session : byId.values()) {
            if (session.seatedAsGuest() && name != null && name.equalsIgnoreCase(session.playerName())) {
                return session;
            }
        }
        return null;
    }

    /** The host gives a disconnected guest's seat to a waiting browser. That browser takes over the session that holds the seat. */
    void giveSeat(final String from, final String to) {
        final WebSession seat;
        final WebSession taker;
        final BrowserChannel channel;
        synchronized (this) {
            seat = seatedGuest(from);
            taker = byId.values().stream().filter(s -> s.attached() && s != host && !s.hasGame() && to != null && to.equalsIgnoreCase(s.playerName()))
                    .findFirst().orElse(null);
            channel = taker == null ? null : taker.browser();
            if (seat == null || channel == null || seat.attached()) {
                return;
            }
            // Otherwise the browser that left would come back to a seat it no longer has
            byId.values().remove(seat);
            byId.replaceAll((key, session) -> session == taker ? seat : session);
            byChannel.put(channel, seat);
        }
        taker.disconnected(channel);
        taker.shutdown();
        seat.connected(channel);
        greet(seat);
        announcePresence();
    }

    /** The host takes a disconnected guest's seat away, and the guest's name with it. */
    void dropPlayer(final String name) {
        final WebSession seat = seatedGuest(name);
        final WebSession h = host;
        // The draft host handles a drafter who has left. It holds the seat until the player returns
        if (seat == null || h == null || seat.attached() || drafting()) {
            return;
        }
        // A match cannot lose a player, so the AI keeps playing. A table's seat opens when the guest's connection closes
        h.local().handToAi(seat.local().webSeat());
        seat.gameGone();
        announcePresence();
    }

    /** Sends the guest in this seat of the host's table back to the waiting screen, which opens the seat. Returns false when no guest holds it. */
    boolean releaseGuestAt(final int index) {
        final ServerGameLobby table = hostLobby();
        if (table == null || drafting() || index < 0 || index >= table.getNumberOfSlots()) {
            return false;
        }
        // Find the guest by name, because a client keeps its original seat number when a seat below it is removed
        final WebSession guest = seatedGuest(table.getSlot(index).getName());
        if (guest != null) {
            guest.gameGone();
        }
        return guest != null;
    }

    /** The host's lobby, where the table's event and its draft run, or null when nobody is hosting one. */
    ServerGameLobby hostLobby() {
        final WebSession h = host;
        return h == null ? null : h.local().hostedLobby();
    }

    /** Sends the table to every browser again, after a change that no lobby update carries. */
    void lobbyChanged() {
        byId.values().forEach(WebSession::lobbyChanged);
    }

    /** Shuts down every session. Guests go first, so no guest session stays open after its game. */
    synchronized void shutdown() {
        stopping = true;
        holdOpen();
        for (final WebSession session : byId.values()) {
            if (session != host) {
                session.shutdown();
            }
        }
        if (host != null) {
            host.shutdown();
            host = null;
        }
        byId.clear();
        byChannel.clear();
    }

    int playersHere() {
        return (int) byId.values().stream().filter(WebSession::attached).count();
    }

    /** Whether the host's table is in a match. A table in a match takes no new players. */
    boolean hostPlaying() {
        final ServerGameLobby table = hostLobby();
        return table != null && table.isMatchActive();
    }

    boolean hostHasGame() {
        final WebSession h = host;
        return h != null && h.local().isHost();
    }

    /** Whether another player here or a computer at the host's table has this name, ignoring case. */
    boolean nameTaken(final String name, final WebSession asking) {
        for (final WebSession session : byId.values()) {
            if (session != asking && (session.attached() || session.hasGame() || session == host)
                    && name.equalsIgnoreCase(session.playerName())) {
                return true;
            }
        }
        final WebSession h = host;
        return h != null && (h.lobby().seatsOf(LobbySlotType.AI).stream().anyMatch(s -> name.equalsIgnoreCase(s.getName()))
                || appSeats().stream().anyMatch(s -> name.equalsIgnoreCase(s.getName())));
    }

    /** The seats at the host's table held by desktop or mobile Forge players. These have no session here. */
    private List<LobbySlot> appSeats() {
        final WebSession h = host;
        if (h == null) {
            return List.of();
        }
        final List<LobbySlot> seats = h.lobby().seatsOf(LobbySlotType.REMOTE);
        seats.removeIf(seat -> byId.values().stream().anyMatch(s -> s.hasGame() && seat.getName().equalsIgnoreCase(s.playerName())));
        return seats;
    }

    /** The host's table changed. This is the only notice that a desktop or mobile player arrived or left. */
    void seatsMayHaveChanged() {
        if (!appSeats().stream().map(LobbySlot::getName).toList().equals(appSeatNames)) {
            announcePresence();
        }
    }

    /** Sends a typed line to every browser here and to the host table's netplay chat. The server keeps the lines, so chat lasts longer than any one table. */
    void say(final WebSession from, final String text) {
        final String who = from.playerName();
        if (who == null) {
            return;
        }
        spread(new ChatLine(who, text, false));
        // Desktop and mobile players at the table read netplay's chat, and relay skips the copy that comes back because the line was already sent here
        if (HostingServer.isHosting()) {
            FServerManager.getInstance().broadcast(new MessageEvent(who, text));
        }
    }

    /** Sends every browser here a line from netplay's chat, said by a desktop or mobile player who has no session here. */
    void relay(final String from, final String text) {
        if (byId.values().stream().noneMatch(s -> from.equalsIgnoreCase(s.playerName()))) {
            spread(new ChatLine(from, text, false));
        }
    }

    private synchronized void spread(final ChatLine line) {
        said.addLast(line);
        while (said.size() > CHAT_KEPT) {
            said.removeFirst();
        }
        for (final WebSession session : byId.values()) {
            session.tell(line);
        }
    }

    /** Sends a newly arrived browser the list of who is here and the recent chat. */
    private synchronized void greet(final WebSession session) {
        session.tell(presence());
        for (final ChatLine line : said) {
            session.tell(new ChatLine(line.from(), line.text(), true));
        }
    }

    /** Who is on this server. Anyone still unnamed is left out. */
    private Presence presence() {
        final List<Person> people = new ArrayList<>();
        for (final WebSession session : byId.values()) {
            final String who = session.playerName();
            final boolean disconnected = !session.attached() && session.seatedAsGuest();
            if (who != null && (session.attached() || disconnected)) {
                people.add(new Person(who, session.avatarIndex(), session.doing(), session.hosting(), disconnected));
            }
        }
        final WebSession h = host;
        final List<LobbySlot> apps = appSeats();
        for (final LobbySlot seat : apps) {
            people.add(new Person(seat.getName(), seat.getAvatarIndex(), h.doing(), false, false));
        }
        appSeatNames = apps.stream().map(LobbySlot::getName).toList();
        return new Presence(people);
    }

    void announcePresence() {
        final Presence now = presence();
        byId.values().forEach(session -> session.tell(now));
    }

    /** Tells every browser except the host's that the host's seat has a new holder or is free. */
    private void announceSeat() {
        byId.values().forEach(WebSession::hostSeatChanged);
    }

    /** Tells the guests waiting for a game that there is now one to join. */
    void hostGameOpened() {
        forEachGuest(WebSession::joinHostGame);
    }

    /** Tells the guests that their game has closed. The server stays up, so nothing else would tell them. */
    void hostGameClosed() {
        forEachGuest(WebSession::gameGone);
    }

    private void forEachGuest(final Consumer<WebSession> action) {
        for (final WebSession session : byId.values()) {
            if (session != host) {
                action.accept(session);
            }
        }
    }

    private static String internetCaption(final int port, final boolean forwarded) {
        return forwarded ? Localizer.getInstance().getMessage("lblWebSessionsOverInternet")
                : Localizer.getInstance().getMessage("lblWebSessionsOverInternetForward", port);
    }

    /** Every link that would reach this machine, most likely first. */
    List<Address> inviteUrls() {
        final List<Address> list = new ArrayList<>();
        final WebServer s = server;
        if (s == null) {
            return list;
        }
        for (final Map.Entry<String, String> e : FServerManager.getAllLocalAddresses().entrySet()) {
            list.add(new Address(e.getKey(), s.inviteUrl(e.getValue())));
        }
        // Listed last, because without a forwarded port this link only reaches the router
        final String external = FServerManager.getExternalAddress();
        if (external != null) {
            list.add(new Address(internetCaption(s.port(), portForwarded), s.inviteUrl(external)));
        }
        return list;
    }
}

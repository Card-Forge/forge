package forge.web;

import com.google.gson.JsonObject;
import forge.gamemodes.match.LobbySlotType;
import forge.util.IHasForgeLog;
import forge.web.protocol.BrowserChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/** Every browser attached to this process, each with its own session. Only the web port must be reachable from outside. */
final class WebHub implements WebServer.Endpoint, IHasForgeLog {
    /** The most browsers tracked at once, because anyone with an invite link can open one browser after another. */
    static final int MOST_SESSIONS = 64;
    /** How long the host's seat stays reserved for a browser that has left, so a reload keeps the seat. */
    private static final int HOST_GRACE_MILLIS = 20_000;
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
    /** Runs when no browser has been connected for a while. That is the only sign that the players have finished. */
    private ScheduledFuture<?> idle;

    WebHub(final WebGuiBase ui, final long idleMillis, final Runnable onQuit) {
        this.ui = ui;
        this.idleMillis = idleMillis;
        this.onQuit = onQuit;
        // This also covers a browser that never opens
        idle = ThreadUtil.delay((int) idleMillis, onQuit);
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

    private void quitUnlessAppsSeated() {
        synchronized (this) {
            idle = null;
            if (stopping || !quitWhenEmpty || !byChannel.isEmpty()) {
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

    /** Tells every browser except the host's that the host's seat has a new holder or is free. */
    private void announceSeat() {
        byId.values().forEach(WebSession::hostSeatChanged);
    }
}

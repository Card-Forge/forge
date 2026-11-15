package forge.web.match;

import forge.game.GameLog;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameLogVerbosity;
import forge.game.card.CardView;
import forge.gamemodes.net.DeltaPacket;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.web.PlayerSettings;
import forge.web.images.Foil;
import forge.web.protocol.ToBrowser.LogEntry;
import forge.web.protocol.ToBrowser.LogMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** The game log for the browser. It holds the entries this seat may read, sends each as it arrives, and resends them all on connect. */
final class WebGameLog {
    private final Predicate<CardView> mayView;
    private final PlayerSettings settings;
    private final List<LogEntry> entries = new ArrayList<>();
    private GameLog logged;
    private int loggedCount;

    WebGameLog(final Predicate<CardView> mayView, final PlayerSettings settings) {
        this.mayView = mayView;
        this.settings = settings;
    }

    /** The entries added since the last call. When the match has moved to a new game, it is every entry. */
    LogMessage added(final GameLog log) {
        final List<GameLogEntry> all = log.getAllEntries();
        final boolean newGame = log != logged;
        if (newGame) {
            logged = log;
            loggedCount = 0;
        }
        final Set<GameLogEntryType> shown = shownTypes();
        final List<LogEntry> fresh = new ArrayList<>();
        for (final GameLogEntry entry : all.subList(loggedCount, all.size())) {
            if (shown.contains(entry.type())) {
                fresh.add(entry(entry));
            }
        }
        loggedCount = all.size();
        synchronized (entries) {
            if (newGame) {
                entries.clear();
            }
            entries.addAll(fresh);
        }
        return newGame || !fresh.isEmpty() ? message(fresh, newGame) : null;
    }

    /** Every entry this seat has seen so far, for a browser that has just connected. */
    LogMessage all() {
        synchronized (entries) {
            return message(entries, true);
        }
    }

    private LogEntry entry(final GameLogEntry entry) {
        final CardView card = entry.sourceCard();
        if (card != null && card.getCurrentState() != null && mayView.test(card)) {
            return new LogEntry(entry.type(), entry.message(),
                    DeltaPacket.makeDeltaKey(card), Foil.key(card, card.getCurrentState()));
        }
        return new LogEntry(entry.type(), entry.message(), null, null);
    }

    // A custom list of entry types is set on desktop, so it comes from the host's preferences. The web offers only the named levels
    private Set<GameLogEntryType> shownTypes() {
        final GameLogVerbosity verbosity = GameLogVerbosity.fromString(settings.get(FPref.DEV_LOG_ENTRY_TYPE));
        return verbosity == GameLogVerbosity.CUSTOM ? FModel.getPreferences().getCustomLogTypes() : verbosity.getIncludedTypes();
    }

    private static LogMessage message(final List<LogEntry> list, final boolean full) {
        return new LogMessage(full, List.copyOf(list));
    }
}

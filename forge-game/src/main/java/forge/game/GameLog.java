/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Set;

/**
 * <p>
 * GameLog class.
 * 
 * @author Forge
 * @version $Id: GameLog.java 12297 2011-11-28 19:56:47Z slapshot5 $
 */
public class GameLog extends Observable implements Serializable {
    private static final long serialVersionUID = 6465283802022948827L;

    private final List<GameLogEntry> log = new ArrayList<>();

    private final transient GameLogFormatter formatter = new GameLogFormatter(this);
    private long matchStartMillis = -1;

    public void markMatchStartIfNeeded() {
        if (matchStartMillis < 0) {
            matchStartMillis = System.currentTimeMillis();
        }
    }

    public long getMatchStartMillis() {
        return matchStartMillis;
    }

    /** Logging level:
     * 0 - Turn
     * 2 - Stack items
     * 3 - Poison Counters
     * 4 - Mana abilities
     * 6 - All Phase information
     */

    public GameLog() {
    }

    public void add(final GameLogEntryType type, final String message) {
        add(new GameLogEntry(type, message));
    }

    void add(GameLogEntry entry) {
        synchronized (log) {
            log.add(entry);
        }
        this.setChanged();
        this.notifyObservers();
    }

    private List<GameLogEntry> snapshot() {
        synchronized (log) {
            return new ArrayList<>(log);
        }
    }

    public int size() {
        synchronized (log) {
            return log.size();
        }
    }

    /** Undo support: drops every entry added after the log had {@code size} entries. */
    public void truncateTo(final int size) {
        synchronized (log) {
            if (size < 0 || size >= log.size()) {
                return;
            }
            log.subList(size, log.size()).clear();
        }
        this.setChanged();
        this.notifyObservers();
    }

    /** All entries in chronological (insertion) order — note {@link #getLogEntries} returns newest-first. */
    public List<GameLogEntry> getAllEntries() {
        return snapshot();
    }

    /**
     * Gets the log entries below a certain level as a list.
     *
     * @param logLevel the log level
     * @return the log text
     */
    public List<GameLogEntry> getLogEntries(final GameLogEntryType logLevel) { // null to fetch all
        final List<GameLogEntry> all = snapshot();
        final List<GameLogEntry> result = new ArrayList<>();
    
        for (int i = all.size() - 1; i >= 0; i--) {
            GameLogEntry le = all.get(i);
            if (logLevel == null || le.type().compareTo(logLevel) <= 0) {
                result.add(le);
            }
        }
        return result;
    }

    public List<GameLogEntry> getLogEntriesForVerbosity(final GameLogVerbosity verbosity) {
        return getLogEntriesForTypes(verbosity.getIncludedTypes());
    }

    public List<GameLogEntry> getLogEntriesForTypes(final Set<GameLogEntryType> types) {
        final List<GameLogEntry> all = snapshot();
        final List<GameLogEntry> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            GameLogEntry le = all.get(i);
            if (types.contains(le.type())) {
                result.add(le);
            }
        }
        return result;
    }

    public List<GameLogEntry> getLogEntriesExact(final GameLogEntryType logLevel) { // null to fetch all
        final List<GameLogEntry> all = snapshot();
        final List<GameLogEntry> result = new ArrayList<>();
    
        for (int i = all.size() - 1; i >= 0; i--) {
            GameLogEntry le = all.get(i);
            if (logLevel == null || le.type().compareTo(logLevel) == 0) {
                result.add(le);
            }
        }
        return result;
    }

    public GameLogFormatter getEventVisitor() {
        return formatter;
    }
}

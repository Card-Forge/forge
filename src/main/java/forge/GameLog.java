/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

package forge;

import java.util.ArrayList;


/**
 * <p>
 * GameLog class.
 * </p>
 * 
 * Logging level:
 * 0 - Turn
 * 2 - Stack items
 * 3 - Poison Counters
 * 4 - Mana abilities
 * 6 - All Phase information
 * 
 * @author Forge
 * @version $Id: GameLog.java 12297 2011-11-28 19:56:47Z slapshot5 $
 */
public class GameLog extends MyObservable {
    private ArrayList<LogEntry> log = new ArrayList<LogEntry>();

    /**
     * Instantiates a new game log.
     */
    public GameLog() {

    }

    /**
     * Adds the.
     *
     * @param type the type
     * @param message the message
     * @param level the level
     */
    public void add(final String type, final String message, final int level) {
        log.add(new LogEntry(type, message, level));
        this.updateObservers();
    }

    /**
     * Gets the log text.
     *
     * @return the log text
     */
    public String getLogText() {
        return getLogText(10);
    }

    /**
     * Gets the log text.
     *
     * @param logLevel the log level
     * @return the log text
     */
    public String getLogText(final int logLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = log.size() - 1; i >= 0; i--) {
            LogEntry le = log.get(i);
            if (le.getLevel() > logLevel) {
                continue;
            }
            sb.append(le.getType()).append(": ").append(le.getMessage());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Reset.
     */
    public void reset() {
        log.clear();
        this.updateObservers();
    }

    /**
     * Gets the log entry.
     *
     * @param index the index
     * @return the log entry
     */
    public LogEntry getLogEntry(int index) {
        return log.get(index);
    }

    private class LogEntry {
        private String type;
        private String message;
        private int level;

        LogEntry(final String typeIn, final String messageIn, final int levelIn) {
            type = typeIn;
            message = messageIn;
            level = levelIn;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public int getLevel() {
            return level;
        }
    }
} // end class GameLog

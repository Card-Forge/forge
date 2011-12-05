
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

    public GameLog() {

    }

    public void add(final String type, final String message, final int level) {
        log.add(new LogEntry(type, message, level));
        this.updateObservers();
    }

    public String getLogText() {
        return getLogText(10);
    }

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

    public void reset() {
        log.clear();
        this.updateObservers();
    }

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

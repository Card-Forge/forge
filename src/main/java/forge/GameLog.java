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

package forge;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.google.common.eventbus.Subscribe;

import forge.game.event.GameEvent;
import forge.game.phase.Combat;
import forge.util.MyObservable;


/**
 * <p>
 * GameLog class.
 * 
 * @author Forge
 * @version $Id: GameLog.java 12297 2011-11-28 19:56:47Z slapshot5 $
 */
public class GameLog extends MyObservable {
    private List<GameLogEntry> log = new ArrayList<GameLogEntry>();

    private GameLogFormatter formatter = new GameLogFormatter(this);
    /** Logging level:
     * 0 - Turn
     * 2 - Stack items
     * 3 - Poison Counters
     * 4 - Mana abilities
     * 6 - All Phase information
     */


    
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
     * @param type the level
     */
    public void add(final GameEventType type, final String message) {
        log.add(new GameLogEntry(type, message));
        this.updateObservers();
    }

    private void add(GameLogEntry entry) {
        log.add(entry);
        this.updateObservers();
    }    
    
    public String getLogText(final GameEventType logLevel) { 
        List<GameLogEntry> filteredAndReversed = getLogEntries(logLevel);
        return StringUtils.join(filteredAndReversed, "\r\n");
    }

    /**
     * Gets the log entries below a certain level as a list.
     *
     * @param logLevel the log level
     * @return the log text
     */
    public List<GameLogEntry> getLogEntries(final GameEventType logLevel) { // null to fetch all
        final List<GameLogEntry> result = new ArrayList<GameLogEntry>();
    
        for (int i = log.size() - 1; i >= 0; i--) {
            GameLogEntry le = log.get(i);
            if(logLevel == null || le.type.compareTo(logLevel) <= 0 )
                result.add(le);
        }
        return result;
    }

    public List<GameLogEntry> getLogEntriesExact(final GameEventType logLevel) { // null to fetch all
        final List<GameLogEntry> result = new ArrayList<GameLogEntry>();
    
        for (int i = log.size() - 1; i >= 0; i--) {
            GameLogEntry le = log.get(i);
            if(logLevel == null || le.type.compareTo(logLevel) == 0 )
                result.add(le);
        }
        return result;
    }


    // Special methods
    
    @Subscribe
    public void receiveGameEvent(GameEvent ev) { 
        GameLogEntry record = formatter.recieve(ev);
        if( null != record ) {
            add(record);
        }
    }

    
    public void addCombatAttackers(Combat combat) {
        this.add(GameLogFormatter.describeAttack(combat)); 
    }
    public void addCombatBlockers(Combat combat) {
        this.add(GameLogFormatter.describeBlock(combat)); 
    }
    // Special methods

}

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
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import com.google.common.eventbus.Subscribe;

import forge.game.GameOutcome;
import forge.game.event.DuelOutcomeEvent;
import forge.game.event.Event;
import forge.game.phase.Combat;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerStatistics;
import forge.util.Lang;
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

    /**
     * Gets the log text.
     *
     * @return the log text
     */
    public String getLogText() {
        return getLogText(null);
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
    public void receiveGameEvent(Event ev) { 
        if(ev instanceof DuelOutcomeEvent) {
            fillOutcome( ((DuelOutcomeEvent) ev).result, ((DuelOutcomeEvent) ev).history );
        }
    }

    
    /**
     * Generates and adds 
     */
    private void fillOutcome(GameOutcome result, List<GameOutcome> history) {

        // add result entries to the game log
        final LobbyPlayer human = Singletons.getControl().getLobby().getGuiPlayer();
        

        final List<String> outcomes = new ArrayList<String>();
        for (Entry<LobbyPlayer, PlayerStatistics> p : result) {
            String whoHas = p.getKey().equals(human) ? "You have" : p.getKey().getName() + " has";
            String outcome = String.format("%s %s", whoHas, p.getValue().getOutcome().toString());
            outcomes.add(outcome);
            this.add(GameEventType.GAME_OUTCOME, outcome);
        }
        
        final String statsSummary = generateSummary(history);
        this.add(GameEventType.MATCH_RESULTS, statsSummary);
    }
    
    private static String generateSummary(List<GameOutcome> gamesPlayed) {
        GameOutcome outcome1 = gamesPlayed.get(0);
        int[] wins = new int[outcome1.getNumPlayers()];
        LobbyPlayer[] players = new LobbyPlayer[outcome1.getNumPlayers()];
        for(int i = 0; i < wins.length; wins[i++] = 0);
        
        for (GameOutcome go : gamesPlayed) {
            int i = 0;
            for(Entry<LobbyPlayer, PlayerStatistics> ps : go) {
                players[i] = ps.getKey();
                if( ps.getValue().getOutcome().hasWon() )
                    wins[i]++;
                i++;
            }
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < wins.length; i++) {
            sb.append(players[i].getName()).append(": ").append(wins[i]).append(" ");
        }
        return sb.toString();
    }
    
    

    public void addCombatAttackers(Combat combat) {
        this.add(GameEventType.COMBAT, describeAttack(combat)); 
    }
    public void addCombatBlockers(Combat combat) {
        this.add(GameEventType.COMBAT, describeBlock(combat)); 
    }
    // Special methods


    private static String describeAttack(final Combat combat) {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker

        

        // Not a big fan of the triple nested loop here
        for (GameEntity defender : combat.getDefenders()) {
            List<Card> attackers = combat.getAttackersOf(defender);
            if (attackers == null || attackers.isEmpty()) {
                continue;
            }
            if ( sb.length() > 0 ) sb.append("\n");

            sb.append(combat.getAttackingPlayer()).append(" declared ").append(Lang.joinHomogenous(attackers));
            sb.append(" to attack ").append(defender.toString()).append(".");
        }

        return sb.toString();
    }


    private static String describeBlock(final Combat combat) {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        
        List<Card> blockers = null;
        

        for (GameEntity defender : combat.getDefenders()) {
            List<Card> attackers = combat.getAttackersOf(defender);
            if (attackers == null || attackers.isEmpty()) {
                continue;
            }
            if ( sb.length() > 0 ) sb.append("\n");

            String controllerName = defender instanceof Card ? ((Card)defender).getController().getName() : defender.getName();
            boolean firstAttacker = true;
            for (final Card attacker : attackers) {
                if ( !firstAttacker ) sb.append("\n");
                
                blockers = combat.getBlockers(attacker);
                if ( blockers.isEmpty() ) {
                    sb.append(controllerName).append(" didn't block ");
                } else {
                    sb.append(controllerName).append(" assigned ").append(Lang.joinHomogenous(blockers)).append(" to block ");
                }
                
                sb.append(attacker).append(".");
                firstAttacker = false;
            }
        }

        return sb.toString();
    }
    
    
    
} // end class GameLog

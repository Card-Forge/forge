package forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import forge.game.GameOutcome;
import forge.game.event.DuelOutcomeEvent;
import forge.game.event.Event;
import forge.game.event.PlayerControlEvent;
import forge.game.phase.Combat;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerStatistics;
import forge.util.Lang;

public class GameLogFormatter { 
    
    // Some events produce several log entries. I've let them added into log directly
    static GameLogEntry logEvent(Event ev, GameLog log) {
        if(ev instanceof DuelOutcomeEvent) {
            return GameLogFormatter.fillOutcome(log, ((DuelOutcomeEvent) ev).result, ((DuelOutcomeEvent) ev).history );
            
        } else if ( ev instanceof PlayerControlEvent ) {
            LobbyPlayer newController = ((PlayerControlEvent) ev).getNewController();
            Player p = ((PlayerControlEvent) ev).getPlayer();

            final String message;
            if( newController == null )
                message = p.getName() + " has restored control over themself";
            else
                message =  String.format("%s is controlled by %s", p.getName(), newController.getName());
            
            return new GameLogEntry(GameEventType.PLAYER_CONROL, message);
        }
        return null;
    }
    
    
    /**
     * Generates and adds 
     */
    private static GameLogEntry fillOutcome(GameLog log, GameOutcome result, List<GameOutcome> history) {
    
        // add result entries to the game log
        final LobbyPlayer human = Singletons.getControl().getLobby().getGuiPlayer();
        
    
        final List<String> outcomes = new ArrayList<String>();
        for (Entry<LobbyPlayer, PlayerStatistics> p : result) {
            String whoHas = p.getKey().equals(human) ? "You have" : p.getKey().getName() + " has";
            String outcome = String.format("%s %s", whoHas, p.getValue().getOutcome().toString());
            outcomes.add(outcome);
            log.add(GameEventType.GAME_OUTCOME, outcome);
        }
        
        return generateSummary(history);
    }

    private static GameLogEntry generateSummary(List<GameOutcome> gamesPlayed) {
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
        return new GameLogEntry(GameEventType.MATCH_RESULTS, sb.toString());
    }

    static GameLogEntry describeAttack(final Combat combat) {
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

        return new GameLogEntry(GameEventType.COMBAT, sb.toString());
    }


    static GameLogEntry describeBlock(final Combat combat) {
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

        return new GameLogEntry(GameEventType.COMBAT, sb.toString());
    }
    
    
    
} // end class GameLog
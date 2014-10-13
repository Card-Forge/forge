package forge.game;

import com.google.common.eventbus.Subscribe;

import forge.LobbyPlayer;
import forge.game.card.Card;
import forge.game.event.*;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.player.Player;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.maps.MapOfLists;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

public class GameLogFormatter extends IGameEventVisitor.Base<GameLogEntry> {
    private final GameLog log;

    public GameLogFormatter(GameLog gameLog) {
        log = gameLog;
    }

    @Override
    public GameLogEntry visit(GameEventGameOutcome ev) {
        for (Player p : ev.result.getPlayers()) {
            String outcome = String.format("%s has %s", p.getName(), p.getOutcome().toString());
            log.add(GameLogEntryType.GAME_OUTCOME, outcome);
        }
        return generateSummary(ev.history);
    }

    @Override
    public GameLogEntry visit(GameEventSpellResolved ev) {
        String messageForLog = ev.hasFizzled ? ev.spell.getHostCard().getName() + " ability fizzles." : ev.spell.getStackDescription();
        return new GameLogEntry(GameLogEntryType.STACK_RESOLVE, messageForLog);
    }

    @Override
    public GameLogEntry visit(GameEventSpellAbilityCast event) {
        String who = event.sa.getActivatingPlayer().getName();
        String action = event.sa.isSpell() ? " cast " : " activated ";
        String what = event.sa.getStackDescription().startsWith("Morph ") ? "Morph" : event.sa.getHostCard().toString();

        StringBuilder sb = new StringBuilder();
        sb.append(who).append(action).append(what);

        if (event.sa.getTargetRestrictions() != null) {
            sb.append(" targeting ");
            for (TargetChoices ch : event.sa.getAllTargetChoices()) {
                if (null != ch) {
                    sb.append(ch.getTargetedString());
                }
            }
        }
        sb.append(".");

        return new GameLogEntry(GameLogEntryType.STACK_ADD, sb.toString());
    }

    private GameLogEntry generateSummary(List<GameOutcome> gamesPlayed) {
        GameOutcome outcome1 = gamesPlayed.get(0);
        List<Player> players = outcome1.getPlayers();

        final int[] wins = new int[players.size()];

        // Calculate total games each player has won.
        for (GameOutcome game : gamesPlayed) {
            int i = 0;
            for (Player p : game.getPlayers()) {
                if (p.getOutcome().hasWon()) {
                    wins[i]++;
                }
                i++;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wins.length; i++) {
            Player player = players.get(i);
            String playerName = player.getName();
            sb.append(playerName).append(": ").append(wins[i]).append(" ");
        }
        return new GameLogEntry(GameLogEntryType.MATCH_RESULTS, sb.toString());
    }

    @Override
    public GameLogEntry visit(GameEventPlayerControl event) {
        // TODO Auto-generated method stub
        LobbyPlayer newController = event.newController;
        Player p = event.player;

        final String message;
        if (newController == null) {
            message = p.getName() + " has restored control over themself";
        }
        else {
            message =  String.format("%s is controlled by %s", p.getName(), newController.getName());
        }
        return new GameLogEntry(GameLogEntryType.PLAYER_CONROL, message);
    }

    @Override
    public GameLogEntry visit(GameEventTurnPhase ev) {
        Player p = ev.playerTurn;
        return new GameLogEntry(GameLogEntryType.PHASE, ev.phaseDesc + Lang.getPossesive(p.getName()) + " " + ev.phase.nameForUi);
    }

    @Override
    public GameLogEntry visit(GameEventCardDamaged event) {
        String additionalLog = "";
        if (event.type == DamageType.Deathtouch) {
            additionalLog = "(Deathtouch)";
        }
        if (event.type == DamageType.M1M1Counters) {
            additionalLog = "(As -1/-1 Counters)";
        }
        if (event.type == DamageType.LoyaltyLoss) {
            additionalLog = "(Removing " + Lang.nounWithAmount(event.amount, "loyalty counter") + ")";
        }
        String message = String.format("%s deals %d damage %s to %s.", event.source, event.amount, additionalLog, event.card);
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventLandPlayed)
     */
    @Override
    public GameLogEntry visit(GameEventLandPlayed ev) {
        String message = String.format("%s played %s", ev.player, ev.land);
        return new GameLogEntry(GameLogEntryType.LAND, message);
    }

    @Override
    public GameLogEntry visit(GameEventTurnBegan event) {
        String message = String.format("Turn %d (%s)", event.turnNumber, event.turnOwner);
        return new GameLogEntry(GameLogEntryType.TURN, message);
    }

    @Override
    public GameLogEntry visit(GameEventPlayerDamaged ev) {
        String extra = ev.infect ? " (as poison counters)" : "";
        String message = String.format("%s deals %d %s damage to %s%s.", ev.source, ev.amount, ev.combat ? "combat" : "non-combat", ev.target, extra);
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    @Override
    public GameLogEntry visit(GameEventPlayerPoisoned ev) {
        String message = String.format("%s receives %s from %s", ev.receiver, Lang.nounWithAmount(ev.amount, "posion counter"), ev.source);
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    @Override
    public GameLogEntry visit(final GameEventAttackersDeclared ev) {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker

        // Not a big fan of the triple nested loop here
        for (GameEntity k : ev.attackersMap.keySet()) {
            Collection<Card> attackers = ev.attackersMap.get(k);
            if (attackers == null || attackers.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(ev.player + " assigned " + Lang.joinHomogenous(attackers));
            sb.append(" to attack " + k + ".");
        }
        if (sb.length() == 0) {
            sb.append(ev.player + " didn't attack this turn.");
        }
        return new GameLogEntry(GameLogEntryType.COMBAT, sb.toString());
    }


    @Override
    public GameLogEntry visit(final GameEventBlockersDeclared ev) {
        final StringBuilder sb = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker

        Collection<Card> blockers = null;

        for (Entry<GameEntity, MapOfLists<Card, Card>> kv : ev.blockers.entrySet()) {
            GameEntity defender = kv.getKey();
            MapOfLists<Card, Card> attackers = kv.getValue();
            if (attackers == null || attackers.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }

            String controllerName = defender instanceof Card ? ((Card)defender).getController().getName() : defender.getName();
            boolean firstAttacker = true;
            for (final Entry<Card, Collection<Card>> att : attackers.entrySet()) {
                if (!firstAttacker) sb.append("\n");

                blockers = att.getValue();
                if (blockers.isEmpty()) {
                    sb.append(controllerName + " didn't block ");
                }
                else {
                    sb.append(controllerName + " assigned " + Lang.joinHomogenous(blockers) + " to block ");
                }

                sb.append(att.getKey()).append(".");
                firstAttacker = false;
            }
        }

        return new GameLogEntry(GameLogEntryType.COMBAT, sb.toString());
    }

    @Override
    public GameLogEntry visit(GameEventMulligan ev) {
        String message = String.format("%s has mulliganed down to %d cards.", ev.player, ev.player.getZone(ZoneType.Hand).size());
        return new GameLogEntry(GameLogEntryType.MULLIGAN, message);
    }

    @Subscribe
    public void recieve(GameEvent ev) {
        GameLogEntry le = ev.visit(this);
        if (le != null) {
            log.add(le);
        }
    }
} // end class GameLog
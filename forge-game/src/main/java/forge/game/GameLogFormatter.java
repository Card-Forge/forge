package forge.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.Subscribe;

import forge.LobbyPlayer;
import forge.game.card.Card;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.event.GameEventCardModeChosen;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventMulligan;
import forge.game.event.GameEventPlayerControl;
import forge.game.event.GameEventPlayerDamaged;
import forge.game.event.GameEventPlayerPoisoned;
import forge.game.event.GameEventScry;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventSurveil;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventTurnPhase;
import forge.game.event.IGameEventVisitor;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.maps.MapOfLists;

public class GameLogFormatter extends IGameEventVisitor.Base<GameLogEntry> {
    private final GameLog log;
    public GameLogFormatter(GameLog gameLog) {
        log = gameLog;
    }

    @Override
    public GameLogEntry visit(GameEventGameOutcome ev) {
        for(String outcome : ev.result.getOutcomeStrings()) {
            log.add(GameLogEntryType.GAME_OUTCOME, outcome);
        }
        return generateSummary(ev.history);
    }

    @Override
    public GameLogEntry visit(GameEventScry ev) {
        final Localizer localizer = Localizer.getInstance();
        String scryOutcome = "";

        if (ev.toTop > 0 && ev.toBottom > 0) {
            scryOutcome = localizer.getMessage("lblLogScryTopBottomLibrary").replace("%s", ev.player.toString()).replace("%top", String.valueOf(ev.toTop)).replace("%bottom", String.valueOf(ev.toBottom));
        } else if (ev.toBottom == 0) {
            scryOutcome = localizer.getMessage("lblLogScryTopLibrary").replace("%s", ev.player.toString()).replace("%top", String.valueOf(ev.toTop));
        } else {
            scryOutcome = localizer.getMessage("lblLogScryBottomLibrary").replace("%s", ev.player.toString()).replace("%bottom", String.valueOf(ev.toBottom));
        }

        return new GameLogEntry(GameLogEntryType.STACK_RESOLVE, scryOutcome);
    }

    @Override
    public GameLogEntry visit(GameEventSurveil ev) {
        final Localizer localizer = Localizer.getInstance();
        String surveilOutcome = "";

        if (ev.toLibrary > 0 && ev.toGraveyard > 0) {
            surveilOutcome = localizer.getMessage("lblLogSurveiledToLibraryGraveyard", ev.player.toString(), String.valueOf(ev.toLibrary), String.valueOf(ev.toGraveyard));
        } else if (ev.toGraveyard == 0) {
            surveilOutcome = localizer.getMessage("lblLogSurveiledToLibrary", ev.player.toString(), String.valueOf(ev.toLibrary));
        } else {
            surveilOutcome = localizer.getMessage("lblLogSurveiledToGraveyard", ev.player.toString(), String.valueOf(ev.toGraveyard));
        }

        return new GameLogEntry(GameLogEntryType.STACK_RESOLVE, surveilOutcome);
    }

    @Override
    public GameLogEntry visit(GameEventSpellResolved ev) {
        String messageForLog = ev.hasFizzled ? Localizer.getInstance().getMessage("lblLogCardAbilityFizzles", ev.spell.getHostCard().toString()) : ev.spell.getStackDescription();
        return new GameLogEntry(GameLogEntryType.STACK_RESOLVE, messageForLog);
    }

    @Override
    public GameLogEntry visit(GameEventSpellAbilityCast event) {
        final Localizer localizer = Localizer.getInstance();
        String player = event.sa.getActivatingPlayer().getName();
        String action = event.sa.isSpell() ? localizer.getMessage("lblCast")
                : event.sa.isTrigger() ? localizer.getMessage("lblTriggered")
                        : localizer.getMessage("lblActivated");
        String object = event.si.getStackDescription().startsWith("Morph ")
                ? localizer.getMessage("lblMorph")
                : event.sa.getHostCard().toString();

        String messageForLog = "";

        if (event.sa.getTargetRestrictions() != null) {
            StringBuilder sb = new StringBuilder();

            List<TargetChoices> targets = event.sa.getAllTargetChoices();
            // Include the TargetChoices from the stack instance, since the real target choices
            // are on that object at this point (see SpellAbilityStackInstance constructor).
            targets.add(event.si.getTargetChoices());
            for (TargetChoices ch : targets) {
                if (null != ch) {
                    sb.append(ch);
                }
            }
            messageForLog = localizer.getMessage("lblLogPlayerActionObjectWitchTarget", player, action, object, sb.toString());
        } else {
            messageForLog = localizer.getMessage("lblLogPlayerActionObject", player, action, object);
        }

        return new GameLogEntry(GameLogEntryType.STACK_ADD, messageForLog);
    }

    @Override
    public GameLogEntry visit(GameEventCardModeChosen ev) {
        if (!ev.log) {
            return null;
        }

        String modeChoiceOutcome = Localizer.getInstance().getMessage("lblLogPlayerChosenModeForCard", ev.player.toString(), ev.mode, ev.cardName);
        return new GameLogEntry(GameLogEntryType.STACK_RESOLVE, modeChoiceOutcome);
    }

    private static GameLogEntry generateSummary(final Collection<GameOutcome> gamesPlayed) {
        final GameOutcome outcome1 = Iterables.getFirst(gamesPlayed, null);
        final HashMap<RegisteredPlayer, String> players = outcome1.getPlayerNames();
        final HashMap<RegisteredPlayer, Integer> winCount = new HashMap<>();

        // Calculate total games each player has won.
        for (final GameOutcome game : gamesPlayed) {
            RegisteredPlayer player = game.getWinningPlayer();

            int amount = winCount.containsKey(player) ? winCount.get(player) : 0;
            winCount.put(player, amount + 1);
        }

        final StringBuilder sb = new StringBuilder();
        for (Entry<RegisteredPlayer, String> entry : players.entrySet()) {
            int amount = winCount.containsKey(entry.getKey()) ? winCount.get(entry.getKey()) : 0;

            sb.append(entry.getValue()).append(": ").append(amount).append(" ");
        }

        return new GameLogEntry(GameLogEntryType.MATCH_RESULTS, sb.toString());
    }

    @Override
    public GameLogEntry visit(final GameEventPlayerControl event) {
        final LobbyPlayer newLobbyPlayer = event.newLobbyPlayer;
        final Player p = event.player;

        final String message;
        if (newLobbyPlayer == null) {
            message = Localizer.getInstance().getMessage("lblLogPlayerHasRestoredControlThemself", p.getName());
        } else {
            message = Localizer.getInstance().getMessage("lblLogPlayerControlledTargetPlayer", p.getName(), newLobbyPlayer.getName());
        }
        return new GameLogEntry(GameLogEntryType.PLAYER_CONROL, message);
    }

    @Override
    public GameLogEntry visit(GameEventTurnPhase ev) {
        Player p = ev.playerTurn;
        return new GameLogEntry(GameLogEntryType.PHASE, ev.phaseDesc + Lang.getInstance().getPossessedObject(p.getName(), ev.phase.nameForUi));
    }

    @Override
    public GameLogEntry visit(GameEventCardDamaged event) {
        final Localizer localizer = Localizer.getInstance();
        String additionalLog = "";
        if (event.type == DamageType.Deathtouch) {
            additionalLog = localizer.getMessage("lblDeathtouch");
        }
        if (event.type == DamageType.M1M1Counters) {
            additionalLog = localizer.getMessage("lblAsM1M1Counters");
        }
        if (event.type == DamageType.LoyaltyLoss) {
            additionalLog = localizer.getMessage("lblRemovingNLoyaltyCounter", String.valueOf(event.amount));
        }
        String message = localizer.getMessage("lblSourceDealsNDamageToDest", event.source.toString(), String.valueOf(event.amount), additionalLog, event.card.toString());
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    /* (non-Javadoc)
     * @see forge.game.event.IGameEventVisitor.Base#visit(forge.game.event.GameEventLandPlayed)
     */
    @Override
    public GameLogEntry visit(GameEventLandPlayed ev) {
        String message = Localizer.getInstance().getMessage("lblLogPlayerPlayedLand", ev.player.toString(), ev.land.toString());
        return new GameLogEntry(GameLogEntryType.LAND, message);
    }

    @Override
    public GameLogEntry visit(GameEventTurnBegan event) {
        String message = Localizer.getInstance().getMessage("lblLogTurnNOwnerByPlayer", String.valueOf(event.turnNumber), event.turnOwner.toString());
        return new GameLogEntry(GameLogEntryType.TURN, message);
    }

    @Override
    public GameLogEntry visit(GameEventPlayerDamaged ev) {
        String extra = ev.infect ? Localizer.getInstance().getMessage("lblLogAsPoisonCounters") : "";
        String damageType = ev.combat ? Localizer.getInstance().getMessage("lblCombat") : Localizer.getInstance().getMessage("lblNonCombat");
        String message = Localizer.getInstance().getMessage("lblLogSourceDealsNDamageOfTypeToDest", ev.source.toString(),
                            String.valueOf(ev.amount), damageType, ev.target.toString(), extra);
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    @Override
    public GameLogEntry visit(GameEventPlayerPoisoned ev) {
        String message = Localizer.getInstance().getMessage("lblLogPlayerReceivesNPosionCounterFrom",
                            ev.receiver.toString(), String.valueOf(ev.amount), ev.source.toString());
        return new GameLogEntry(GameLogEntryType.DAMAGE, message);
    }

    @Override
    public GameLogEntry visit(final GameEventAttackersDeclared ev) {
        final StringBuilder sb = new StringBuilder();
        final Localizer localizer = Localizer.getInstance();

        // Loop through Defenders
        // Append Defending Player/Planeswalker

        // Not a big fan of the triple nested loop here
        for (GameEntity k : ev.attackersMap.keySet()) {
            Collection<Card> attackers = ev.attackersMap.get(k);
            if (attackers == null || attackers.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(localizer.getMessage("lblLogPlayerAssignedAttackerToAttackTarget", ev.player, Lang.joinHomogenous(attackers), k));
        }
        if (sb.length() == 0) {
            sb.append(localizer.getMessage("lblPlayerDidntAttackThisTurn").replace("%s", ev.player.toString()));
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
                    sb.append(Localizer.getInstance().getMessage("lblLogPlayerDidntBlockAttacker", controllerName, att.getKey()));
                } else {
                    sb.append(Localizer.getInstance().getMessage("lblLogPlayerAssignedBlockerToBlockAttacker", controllerName, Lang.joinHomogenous(blockers), att.getKey()));
                }
                firstAttacker = false;
            }
        }

        return new GameLogEntry(GameLogEntryType.COMBAT, sb.toString());
    }

    @Override
    public GameLogEntry visit(GameEventMulligan ev) {
        String message = Localizer.getInstance().getMessage("lblPlayerHasMulliganedDownToNCards").replace("%d", String.valueOf(ev.player.getZone(ZoneType.Hand).size())).replace("%s", ev.player.toString());
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
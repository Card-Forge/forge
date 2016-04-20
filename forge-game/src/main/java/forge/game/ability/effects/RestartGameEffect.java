package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.GameStage;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestartGameEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        FCollectionView<Player> players = game.getPlayers();
        Map<Player, List<Card>> playerLibraries = new HashMap<Player, List<Card>>();

        // Don't grab Ante Zones
        List<ZoneType> restartZones = new ArrayList<ZoneType>(Arrays.asList(ZoneType.Battlefield,
                ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command));

        ZoneType leaveZone = ZoneType.smartValueOf(sa.hasParam("RestrictFromZone") ? sa.getParam("RestrictFromZone") : null);
        restartZones.remove(leaveZone);
        String leaveRestriction = sa.hasParam("RestrictFromValid") ? sa.getParam("RestrictFromValid") : "Card";

        for (Player p : players) {
            CardCollection newLibrary = new CardCollection(p.getCardsIn(restartZones));
            List<Card> filteredCards = null;
            if (leaveZone != null) {
                filteredCards = CardLists.filter(p.getCardsIn(leaveZone),
                        CardPredicates.restriction(leaveRestriction.split(","), p, sa.getHostCard(), null));
                newLibrary.addAll(filteredCards);
            }
            playerLibraries.put(p, newLibrary);
        }
        
        //Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.game.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearDelayedTrigger();
        trigHandler.cleanUpTemporaryTriggers();
        trigHandler.suppressMode(TriggerType.ChangesZone);

        game.getStack().reset();
        GameAction action = game.getAction();
    
        List<Player> gamePlayers = game.getRegisteredPlayers();
        for (int i = 0; i < gamePlayers.size(); i++) {
            final Player player = gamePlayers.get(i);
            if (player.hasLost()) { continue; }

            RegisteredPlayer psc = game.getMatch().getPlayers().get(i);

            player.setStartingLife(psc.getStartingLife());
            player.setPoisonCounters(0, sa.getHostCard());
            player.resetLandsPlayedThisTurn();
            player.resetInvestigatedThisTurn();
            player.initVariantsZones(psc);

            List<Card> newLibrary = playerLibraries.get(player);
            for (Card c : newLibrary) {
                action.moveToLibrary(c, 0);
            }

            player.shuffle(null);
        }
    
        trigHandler.clearSuppression(TriggerType.ChangesZone);
    
        game.resetTurnOrder();
        game.setAge(GameStage.RestartedByKarn);
        // Do not need this because ability will resolve only during that player's turn
        //game.getPhaseHandler().setPlayerTurn(sa.getActivatingPlayer());
        
        // Set turn number?
        
        // The rest is handled by phaseHandler 
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public String getStackDescription(SpellAbility sa) {
        String desc = sa.getParam("SpellDescription");

        if (desc == null) {
            desc = "Restart the game.";
        }

        return desc.replace("CARDNAME", sa.getHostCard().getName());
    }
}


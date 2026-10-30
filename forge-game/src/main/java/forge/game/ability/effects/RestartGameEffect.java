package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.GameStage;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

public class RestartGameEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        FCollectionView<Player> players = game.getPlayers();

        // Don't grab Ante Zones
        List<ZoneType> restartZones = new ArrayList<>(Arrays.asList(ZoneType.Battlefield,
                ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile));

        ZoneType leaveZone = ZoneType.smartValueOf(sa.getParam("RestrictFromZone"));
        restartZones.remove(leaveZone);
        String leaveRestriction = sa.getParamOrDefault("RestrictFromValid", "Card");

        //Card.resetUniqueNumber();
        // need this code here, otherwise observables fail
        forge.game.trigger.Trigger.resetIDs();
        TriggerHandler trigHandler = game.getTriggerHandler();
        trigHandler.clearDelayedTrigger();
        trigHandler.clearPlayerDefinedDelayedTrigger();
        trigHandler.suppressMode(TriggerType.ChangesZone);
        // Avoid Psychic Surgery trigger in new game
        trigHandler.suppressMode(TriggerType.Shuffled);

        game.getPhaseHandler().restart();
        game.getUntap().clearCommands();
        game.getBeginOfCombat().clearCommands();
        game.getUpkeep().clearCommands();
        game.getEndOfCombat().clearCommands();
        game.getEndOfTurn().clearCommands();
        game.getCleanup().clearCommands();

        game.getStack().reset();
        game.clearCountersThisTurn();
        game.setMonarch(null);
        game.setHasInitiative(null);
        game.setDayTime(null);
        GameAction action = game.getAction();

        for (Player p: players) {
            p.setStartingLife(p.getStartingLife());
            p.clearCounters();
            p.resetSpellCastThisGame();
            p.onCleanupPhase();
            p.setLandsPlayedLastTurn(0);
            p.setSpellsCastLastTurn(0);
            p.setLifeLostLastTurn(0);
            p.resetCommanderStats();
            p.resetCompletedDungeons();
            p.resetRingTemptedYou();
            p.clearRingBearer();
            p.clearTheRing();
            p.setBlessing(false, null);
            p.clearController();

            CardCollection newLibrary = new CardCollection(p.getCardsIn(restartZones, false));
            if (leaveZone != null) {
                List<Card> filteredCards = CardLists.getValidCards(p.getCardsIn(leaveZone), leaveRestriction, p, sa.getHostCard(), sa);
                newLibrary.addAll(filteredCards);
            }

            // Only commanders stay in the command zone. Removing the commander effect stops its replacement offer below
            final PlayerZone command = p.getZone(ZoneType.Command);
            command.setCards(CardLists.filter(command.getCards(), Card::isCommander));

            List<Integer> ids = new ArrayList<>();
            for (Card c : newLibrary) {
                if (c.getIntensity(false) > 0) {
                    c.setIntensity(0);
                }
                action.moveToLibrary(c, 0, sa);
                ids.add(c.getId());
            }
            p.setCardsFromStartingDeck(ids);
            p.initVariantsZones(p.getRegisteredPlayer());

            p.shuffle(null);
        }

        // Karn Liberated ruling: commanders start the new game in the command zone. Moving them through the library
        // first splits a merged or melded commander, and catches one that another player controlled
        for (Player p : players) {
            for (Card c : CardLists.filter(p.getCardsIn(ZoneType.Library), Card::isCommander)) {
                action.moveTo(ZoneType.Command, c, sa, AbilityKey.newMap());
            }
            if (!p.getCommanders().isEmpty()) {
                p.createCommanderEffect();
            }
        }

        trigHandler.clearSuppression(TriggerType.Shuffled);
        trigHandler.clearSuppression(TriggerType.ChangesZone);

        game.resetTurnOrder();
        game.setAge(GameStage.RestartedByKarn);
        // For the rare case that you get to resolve it during another players turn
        game.getPhaseHandler().setPlayerTurn(activator);

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

        return desc;
    }
}

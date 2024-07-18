package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.GameStage;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
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

        ZoneType leaveZone = ZoneType.smartValueOf(sa.hasParam("RestrictFromZone") ? sa.getParam("RestrictFromZone") : null);
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
        game.getUpkeep().clearCommands();
        game.getEndOfCombat().clearCommands();
        game.getEndOfTurn().clearCommands();
        game.getCleanup().clearCommands();

        game.getStack().reset();
        game.clearCounterAddedThisTurn();
        game.clearCounterRemovedThisTurn();
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
            p.setBlessing(false);
            p.clearController();

            CardCollection newLibrary = new CardCollection(p.getCardsIn(restartZones, false));
            List<Card> filteredCards = null;
            if (leaveZone != null) {
                filteredCards = CardLists.getValidCards(p.getCardsIn(leaveZone), leaveRestriction, p, sa.getHostCard(), sa);
                newLibrary.addAll(filteredCards);
            }

            // special handling for Karn to filter out non-cards
            for (Card c : p.getCardsIn(ZoneType.Command)) {
                if (c.isCommander()) {
                    newLibrary.add(c);
                }
            }
            p.getZone(ZoneType.Command).removeAllCards(true);

            for (Card c : newLibrary) {
                if (c.getIntensity(false) > 0) {
                    c.setIntensity(0);
                }
                action.moveToLibrary(c, 0, sa);
            }
            p.initVariantsZones(p.getRegisteredPlayer());

            p.shuffle(null);
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

package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConniveEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     * returns the automatically generated stack description string
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Card> tgt = getTargetCards(sa);
        if (tgt.size() <= 0) {
            return "";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(Lang.joinHomogenous(tgt)).append(tgt.size() > 1 ? " connive." : " connives.");
            return sb.toString();
        }
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int num = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("ConniveNum", "1"), sa);

        CardCollection toConnive = getTargetCards(sa);
        if (toConnive.isEmpty()) { // if nothing is conniving, we're done
            return;
        }

        PlayerCollection controllers = new PlayerCollection(game.getPlayersInTurnOrder(game.getPhaseHandler().getPlayerTurn()));
        controllers.retainAll(toConnive.stream().map(Card::getController).collect(Collectors.toSet()));

        for (final Player p : controllers) {
            final CardCollection connivers = CardLists.filterControlledBy(toConnive, p);
            while (!connivers.isEmpty()) {
                final GameEntityCounterTable counterPlacements = new GameEntityCounterTable();

                Card conniver = connivers.size() > 1 ? p.getController().chooseSingleEntityForEffect(connivers, sa,
                        Localizer.getInstance().getMessage("lblChooseConniver"), null) : connivers.get(0);
                connivers.remove(conniver);

                if (game.getReplacementHandler().run(ReplacementType.Connive, AbilityKey.mapFromAffected(conniver))
                        != ReplacementResult.NotReplaced) {
                    continue;
                }

                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
                p.drawCards(num, sa, moveParams);
                zoneMovements.triggerChangesZoneAll(game, sa);

                // in case anything triggers from drawing that happened before discard, e.g. Sneaky Snacker
                game.getTriggerHandler().collectTriggerForWaiting();

                CardCollection hand = new CardCollection(p.getCardsIn(ZoneType.Hand));
                if (!hand.isEmpty() && p.canDiscardBy(sa, true)) {
                    int amt = Math.min(hand.size(), num);
                    CardCollectionView toBeDiscarded = amt == 0 ? CardCollection.EMPTY :
                            p.getController().chooseCardsToDiscardFrom(p, sa, hand, amt, amt);

                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);

                    // need to get newest game state to check if it is still on the battlefield and the timestamp didn't change
                    Card gamec = game.getCardState(conniver);
                    // if the card is not in the game anymore, this might still return true, but it's no problem
                    if (game.getZoneOf(gamec).is(ZoneType.Battlefield) && gamec.equalsWithGameTimestamp(conniver)) {
                        int numCntrs = CardLists.count(toBeDiscarded, CardPredicates.NON_LANDS);
                        conniver.addCounter(CounterEnumType.P1P1, numCntrs, p, counterPlacements);
                    }

                    moveParams = AbilityKey.newMap();
                    zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
                    discard(sa, true, Map.of(p, CardCollection.getView(toBeDiscarded)), moveParams);
                    counterPlacements.replaceCounterEffect(game, sa);
                    zoneMovements.triggerChangesZoneAll(game, sa);
                }

                game.getTriggerHandler().runTrigger(TriggerType.Connives, AbilityKey.mapFromCard(conniver), false);
            }
        }
    }
}

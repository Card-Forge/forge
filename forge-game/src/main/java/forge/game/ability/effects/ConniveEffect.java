package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        List<Player> controllers = new ArrayList<>();
        for (Card c : toConnive) {
            final Player controller = c.getController();
            if (!controllers.contains(controller)) {
                controllers.add(controller);
            }
        }
        //order controllers by APNAP
        int indexAP = controllers.indexOf(game.getPhaseHandler().getPlayerTurn());
        if (indexAP != -1) {
            Collections.rotate(controllers, - indexAP);
        }

        for (final Player p : controllers) {
            CardCollection connivers = CardLists.filterControlledBy(toConnive, p);
            while (connivers.size() > 0) {
                GameEntityCounterTable table = new GameEntityCounterTable();
                final CardZoneTable triggerList = new CardZoneTable();
                Map<Player, CardCollectionView> discardedMap = Maps.newHashMap();
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
                moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

                Card conniver = connivers.size() > 1 ? p.getController().chooseSingleEntityForEffect(connivers, sa,
                        Localizer.getInstance().getMessage("lblChooseConniver"), null) : toConnive.get(0);

                p.drawCards(num, sa, moveParams);

                CardCollectionView validCards = p.getCardsIn(ZoneType.Hand);
                validCards = CardLists.filter(validCards, CardPredicates.Presets.NON_TOKEN);
                if (validCards.isEmpty() || !p.canDiscardBy(sa, true)) { // hand being empty unlikely, just to be safe
                    continue;
                }

                int amt = Math.min(validCards.size(), num);
                CardCollectionView toBeDiscarded = amt == 0 ? CardCollection.EMPTY :
                        p.getController().chooseCardsToDiscardFrom(p, sa, validCards, amt, amt);

                if (toBeDiscarded.size() > 1) {
                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                }

                int numCntrs = CardLists.getValidCardCount(toBeDiscarded, "Card.nonLand", p, host, sa);

                // need to get newest game state to check if it is still on the battlefield and the timestamp didn't change
                Card gamec = game.getCardState(conniver);
                // if the card is not in the game anymore, this might still return true, but it's no problem
                if (game.getZoneOf(gamec).is(ZoneType.Battlefield) && gamec.equalsWithTimestamp(conniver)) {
                    conniver.addCounter(CounterEnumType.P1P1, numCntrs, p, table);
                }
                connivers.remove(conniver);
                discardedMap.put(p, CardCollection.getView(toBeDiscarded));
                discard(sa, triggerList, true, discardedMap, moveParams);
                table.replaceCounterEffect(game, sa, true);
                triggerList.triggerChangesZoneAll(game, sa);
            }
        }
    }
}

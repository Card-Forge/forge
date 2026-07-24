package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class RecruitEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Player> tgt = getTargetPlayers(sa);
        if (tgt.size() <= 0) {
            return "";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(Lang.joinHomogenous(tgt)).append(tgt.size() > 1 ? " recruit." : " recruits.");
            return sb.toString();
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        MutableBoolean combatChanged = new MutableBoolean(false);

        for (final Player p : getTargetPlayers(sa)) {

            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
            p.drawCards(1, sa, moveParams);
            zoneMovements.triggerChangesZoneAll(game, sa);

            // in case anything triggers from drawing that happened before discard, e.g. Sneaky Snacker
            game.getTriggerHandler().collectTriggerForWaiting();

            CardCollection hand = new CardCollection(p.getCardsIn(ZoneType.Hand));
            if (!hand.isEmpty() && p.canDiscardBy(sa, true)) {
                int amt = Math.min(hand.size(), 1);
                CardCollectionView toBeDiscarded = amt == 0 ? CardCollection.EMPTY :
                        p.getController().chooseCardsToDiscardFrom(p, sa, hand, amt, amt);

                toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);

                moveParams = AbilityKey.newMap();
                zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
                discard(sa, true, Map.of(p, CardCollection.getView(toBeDiscarded)), moveParams);
                zoneMovements.triggerChangesZoneAll(game, sa);

                if (toBeDiscarded.stream().anyMatch(CardPredicates.NON_LANDS)) {
                    CardZoneTable triggerList = new CardZoneTable();

                    makeTokenTable(makeTokenTableInternal(p, "w_1_1_human_soldier", 1, sa), false, triggerList, combatChanged, sa);

                    triggerList.triggerChangesZoneAll(game, sa);

                    game.fireEvent(new GameEventTokenCreated());
                }
            }
        }

        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }

}

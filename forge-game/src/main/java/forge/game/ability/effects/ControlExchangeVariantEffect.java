package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class ControlExchangeVariantEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Exchange cards controlled by " + StringUtils.join(getTargetPlayers(sa), ",");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final List<Player> players = getTargetPlayers(sa);
        if (players.size() != 2) {
            return;
        }
        final Player player1 = players.get(0);
        final Player player2 = players.get(1);
        final ZoneType zone = ZoneType.smartValueOf(sa.getParamOrDefault("Zone", "Battlefield"));
        final String type = sa.getParamOrDefault("Type", "Card");
        // get valid lists
        CardCollectionView list1 = AbilityUtils.filterListByType(player1.getCardsIn(zone), type, sa);
        CardCollectionView list2 = AbilityUtils.filterListByType(player2.getCardsIn(zone), type, sa);
        int max = Math.min(list1.size(), list2.size());
        // choose the same number of cards
        CardCollectionView chosen1 = activator.getController().chooseCardsForEffect(list1, sa, "Choose cards: " + player1, 0, max, true);
        int num = chosen1.size();
        CardCollectionView chosen2 = activator.getController().chooseCardsForEffect(list2, sa, "Choose cards: " + player2, num, num, true);
        // check all cards can be controlled by the other player
        for (final Card c : chosen1) {
            if (!c.canBeControlledBy(player2)) {
                return;
            }
        }
        for (final Card c : chosen2) {
            if (!c.canBeControlledBy(player1)) {
                return;
            }
        }
        // set new controller
        final long tStamp = sa.getActivatingPlayer().getGame().getNextTimestamp();
        for (final Card c : chosen1) {
            c.setController(player2, tStamp);
        }
        for (final Card c : chosen2) {
            c.setController(player1, tStamp);
        }
    }
}

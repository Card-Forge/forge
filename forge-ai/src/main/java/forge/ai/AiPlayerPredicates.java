package forge.ai;

import java.util.Comparator;

import forge.game.ability.AbilityUtils;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public final class AiPlayerPredicates {

    public static final Comparator<Player> compareByZoneValue(final String type, final ZoneType zone,
            final SpellAbility sa) {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                CardCollectionView list0 = AbilityUtils.filterListByType(arg0.getCardsIn(zone), type, sa);
                CardCollectionView list1 = AbilityUtils.filterListByType(arg1.getCardsIn(zone), type, sa);

                int v0, v1;

                if ((CardLists.getNotType(list0, "Creature").isEmpty())
                        && (CardLists.getNotType(list1, "Creature").isEmpty())) {
                    v0 = ComputerUtilCard.evaluateCreatureList(list0);
                    v1 = ComputerUtilCard.evaluateCreatureList(list1);
                } // otherwise evaluate both lists by CMC and pass only if human
                  // permanents are less valuable
                else {
                    v0 = ComputerUtilCard.evaluatePermanentList(list0);
                    v1 = ComputerUtilCard.evaluatePermanentList(list1);
                }
                return Integer.compare(v0, v1);
            }
        };
    }
}

package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Set;

public class CountersRemoveAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final CounterType cType = CounterType.valueOf(sa.getParam("CounterType"));
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);
        final String zone = sa.hasParam("ValidZone") ? sa.getParam("ValidZone") : "Battlefield";
        String amountString = Integer.toString(amount);

        if (sa.hasParam("AllCounters")) {
            amountString = "all";
        }

        sb.append("Remove ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (!amountString.equals("1")) {
            sb.append("s");
        }
        sb.append(" from each valid ");
        if (zone.matches("Battlefield")) {
            sb.append("permanent.");
        } else {
            sb.append("card in ").append(zone).append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final String type = sa.getParam("CounterType");
        int counterAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);
        final String valid = sa.getParam("ValidCards");
        final ZoneType zone = sa.hasParam("ValidZone") ? ZoneType.smartValueOf(sa.getParam("ValidZone")) : ZoneType.Battlefield;
        final Game game = sa.getActivatingPlayer().getGame();

        CardCollectionView cards = game.getCardsIn(zone);
        cards = CardLists.getValidCards(cards, valid, sa.getHostCard().getController(), sa.getHostCard());

        if (sa.usesTargeting()) {
            final Player pl = sa.getTargets().getFirstTargetedPlayer();
            cards = CardLists.filterControlledBy(cards, pl);
        }

        for (final Card tgtCard : cards) {
            if (sa.hasParam("AllCounterTypes")) {
            	Set<CounterType> types = tgtCard.getCounters().keySet();
            	for(CounterType ct : types) {
            		tgtCard.subtractCounter(ct, tgtCard.getCounters(ct));
            	}
                //tgtCard.getCounters().clear();
                continue;
            }
            if (sa.hasParam("AllCounters")) {
                counterAmount = tgtCard.getCounters(CounterType.valueOf(type));
            }

            tgtCard.subtractCounter(CounterType.valueOf(type), counterAmount);
        }
    }
}

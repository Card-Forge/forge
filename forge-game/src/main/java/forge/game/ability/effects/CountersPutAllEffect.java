package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CountersPutAllEffect extends SpellAbilityEffect  {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final CounterType cType = CounterType.getType(sa.getParam("CounterType"));
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);
        final String zone = sa.getParamOrDefault("ValidZone", "Battlefield");

        sb.append("Put ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" on each valid ");
        if (zone.matches("Battlefield")) {
            sb.append("permanent.");
        } else {
            sb.append("card in ").append(zone).append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final String type = sa.getParam("CounterType");
        final int counterAmount = AbilityUtils.calculateAmount(host, sa.getParam("CounterNum"), sa);
        final String valid = sa.getParam("ValidCards");
        final ZoneType zone = sa.hasParam("ValidZone") ? ZoneType.smartValueOf(sa.getParam("ValidZone")) : ZoneType.Battlefield;
        final boolean etbcounter = sa.hasParam("ETB");
        final Game game = activator.getGame();

        if (counterAmount <= 0) {
            return;
        }
        
        CardCollectionView cards = game.getCardsIn(zone);
        cards = CardLists.getValidCards(cards, valid, host.getController(), host, sa);

        if (sa.usesTargeting()) {
            final Player pl = sa.getTargets().getFirstTargetedPlayer();
            cards = CardLists.filterControlledBy(cards, pl);
        }

        Player placer = activator;
        if (sa.hasParam("Placer")) {
            final String pstr = sa.getParam("Placer");
            placer = AbilityUtils.getDefinedPlayers(host, pstr, sa).get(0);
        }

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final Card tgtCard : cards) {
            boolean inBattlefield = game.getZoneOf(tgtCard).is(ZoneType.Battlefield);
            if (etbcounter) {
                tgtCard.addEtbCounter(CounterType.getType(type), counterAmount, placer);
            } else {
                tgtCard.addCounter(CounterType.getType(type), counterAmount, placer, sa, inBattlefield, table);
            }
            game.updateLastStateForCard(tgtCard);
        }
        table.triggerCountersPutAll(game);
    }

}

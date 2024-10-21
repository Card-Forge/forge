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
import forge.util.Lang;

public class CountersPutAllEffect extends SpellAbilityEffect  {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final CounterType cType = CounterType.getType(sa.getParam("CounterType"));
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("CounterNum", "1"), sa);
        final String zone = sa.getParamOrDefault("ValidZone", "Battlefield");

        sb.append("Put ");
        sb.append(Lang.nounWithNumeralExceptOne(amount, cType.getName().toLowerCase() + " counter"));
        sb.append(" on each ");
        if (sa.hasParam("ValidCardsDesc")) {
            sb.append(sa.getParam("ValidCardsDesc")).append(".");
        } else {
            sb.append("valid ");
            if (zone.matches("Battlefield")) {
                sb.append("permanent.");
            } else {
                sb.append("card in ").append(zone).append(".");
            }
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final CounterType type = CounterType.getType(sa.getParam("CounterType"));
        int counterAmount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("CounterNum", "1"), sa);
        final String valid = sa.getParam("ValidCards");
        final ZoneType zone = sa.hasParam("ValidZone") ? ZoneType.smartValueOf(sa.getParam("ValidZone")) : ZoneType.Battlefield;
        final Game game = activator.getGame();

        if (counterAmount <= 0) {
            return;
        }

        CardCollectionView cards = game.getCardsIn(zone);
        cards = CardLists.getValidCards(cards, valid, activator, host, sa);

        if (sa.usesTargeting()) {
            final Player pl = sa.getTargets().getFirstTargetedPlayer();
            cards = CardLists.filterControlledBy(cards, pl);
        }

        Player placer = activator;
        String placerPerCard = "";
        if (sa.hasParam("Placer")) {
            final String pstr = sa.getParam("Placer");
            if (pstr.equals("Controller")) {
                placerPerCard = "Controller";
            } else if (pstr.equals("Owner")) {
                placerPerCard = "Owner";
            } else {
                placer = AbilityUtils.getDefinedPlayers(host, pstr, sa).get(0);
            }
        }

        GameEntityCounterTable table = new GameEntityCounterTable();
        for (final Card tgtCard : cards) {
            if (placerPerCard.equals("Controller")) {
                placer = tgtCard.getController();
            } else if (placerPerCard.equals("Owner")) {
                placer = tgtCard.getOwner();
            }
            if (sa.hasParam("AmountByChosenMap")) {
                final String[] parse = sa.getParam("AmountByChosenMap").split(" INDEX ");
                final int index = parse.length > 1 ? Integer.parseInt(parse[1]) : 0;
                if (index >= host.getChosenMap().get(placer).size()) continue;
                final Card chosen = host.getChosenMap().get(placer).get(index);
                counterAmount = AbilityUtils.xCount(chosen, parse[0], sa);
            }
            tgtCard.addCounter(type, counterAmount, placer, table);
        }

        if (sa.hasParam("ValidCards2") || sa.hasParam("CounterType2") || sa.hasParam("CounterNum2")) {
            final CounterType type2 = sa.hasParam("CounterType2") ?
                    CounterType.getType(sa.getParam("CounterType2")) : type;
            final ZoneType zone2 = sa.hasParam("ValidZone2") ?
                    ZoneType.smartValueOf(sa.getParam("ValidZone2")) : zone;
            if (sa.hasParam("ValidCards2")) {
                cards = CardLists.getValidCards(game.getCardsIn(zone2), sa.getParam("ValidCards2"),
                        activator, host, sa);
                if (sa.usesTargeting()) {
                    cards = CardLists.filterControlledBy(cards, sa.getTargets().getFirstTargetedPlayer());
                }
            }
            final int counterAmount2 = sa.hasParam("CounterNum2") ?
                    AbilityUtils.calculateAmount(host, sa.getParam("CounterNum2"), sa) : counterAmount;

            for (final Card tgtCard : cards) {
                if (placerPerCard.equals("Controller")) {
                    placer = tgtCard.getController();
                }
                tgtCard.addCounter(type2, counterAmount2, placer, table);
            }
        }

        table.replaceCounterEffect(game, sa, true);
    }

}

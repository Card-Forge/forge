package forge.card.ability.effects;

import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class CountersPutEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card card = sa.getSourceCard();

        final CounterType cType = CounterType.valueOf(sa.getParam("CounterType"));
        final int amount = AbilityUtils.calculateAmount(card, sa.getParam("CounterNum"), sa);
        sb.append("Put ");
        if (sa.hasParam("UpTo")) {
            sb.append("up to ");
        }
        sb.append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" on ");
        final Target tgt = sa.getTarget();
        final List<Card> tgtCards = tgt != null ? tgt.getTargetCards() :  AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);

        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            final Card tgtC = it.next();
            if (tgtC.isFaceDown()) {
                sb.append("Morph");
            } else {
                sb.append(tgtC);
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        CounterType counterType;

        try {
            counterType = AbilityUtils.getCounterType(sa.getParam("CounterType"), sa);
        } catch (Exception e) {
            System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
            return;
        }

        int counterAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("CounterNum"), sa);
        final int max = sa.hasParam("MaxFromEffect") ? Integer.parseInt(sa.getParam("MaxFromEffect")) : -1;

        if (sa.hasParam("UpTo")) {
            final Integer[] integers = new Integer[counterAmount + 1];
            for (int j = 0; j <= counterAmount; j++) {
                integers[j] = Integer.valueOf(j);
            }
            final Integer i = GuiChoose.oneOrNone("How many counters?", integers);
            if (null == i) {
                return;
            } else {
                counterAmount = i.intValue();
            }
        }

        List<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null && (tgt.getTargetPlayers().size() == 0)) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityUtils.getDefinedCards(card, sa.getParam("Defined"), sa);
        }

        for (final Card tgtCard : tgtCards) {
            if ((tgt == null) || tgtCard.canBeTargetedBy(sa)) {
                if (max != -1) {
                    counterAmount = max - tgtCard.getCounters(counterType);
                }
                final Zone zone = Singletons.getModel().getGame().getZoneOf(tgtCard);
                if (zone == null) {
                    // Do nothing, token disappeared
                } else if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                    tgtCard.addCounter(counterType, counterAmount, true);
                } else {
                    // adding counters to something like re-suspend cards
                    tgtCard.addCounter(counterType, counterAmount, false);
                }
            }
        }
    }

}

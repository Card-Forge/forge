package forge.game.ability.effects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

public class CountersPutEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card card = sa.getSourceCard();
        final boolean dividedAsYouChoose = sa.hasParam("DividedAsYouChoose");

        final CounterType cType = CounterType.valueOf(sa.getParam("CounterType"));
        final int amount = AbilityUtils.calculateAmount(card, sa.getParam("CounterNum"), sa);
        if (dividedAsYouChoose) {
            sb.append("Distribute ");
        } else {
            sb.append("Put ");
        }
        if (sa.hasParam("UpTo")) {
            sb.append("up to ");
        }
        sb.append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        if (dividedAsYouChoose) {
            sb.append(" among ");
        } else {
            sb.append(" on ");
        }
        final List<Card> tgtCards = getTargetCards(sa);

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

        final boolean remember = sa.hasParam("RememberCounters");
        int counterAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("CounterNum"), sa);
        final int max = sa.hasParam("MaxFromEffect") ? Integer.parseInt(sa.getParam("MaxFromEffect")) : -1;

        if (sa.hasParam("UpTo")) {
            counterAmount = sa.getActivatingPlayer().getController().chooseNumber(sa, "How many counters?", 0, counterAmount);
        }

        List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        for (final Card tgtCard : tgtCards) {
            counterAmount = sa.usesTargeting() && sa.hasParam("DividedAsYouChoose") ? sa.getTargetRestrictions().getDividedValue(tgtCard) : counterAmount;
            if (!sa.usesTargeting() || tgtCard.canBeTargetedBy(sa)) {
                if (max != -1) {
                    counterAmount = max - tgtCard.getCounters(counterType);
                }
                final Zone zone = tgtCard.getGame().getZoneOf(tgtCard);
                if (zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                    if (remember) {
                        final int value = tgtCard.getTotalCountersToAdd();
                        tgtCard.addCountersAddedBy(card, counterType, value);
                    }
                    tgtCard.addCounter(counterType, counterAmount, true);
                    if (sa.hasParam("Evolve")) {
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Card", tgtCard);
                        tgtCard.getController().getGame().getTriggerHandler().runTrigger(TriggerType.Evolved, runParams, false);
                    }
                    if (sa.hasParam("Monstrosity")) {
                        tgtCard.setMonstrous(true);
                        tgtCard.setMonstrosityNum(counterAmount);
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Card", tgtCard);
                        tgtCard.getController().getGame().getTriggerHandler().runTrigger(TriggerType.BecomeMonstrous, runParams, false);
                    }
                } else {
                    // adding counters to something like re-suspend cards
                    tgtCard.addCounter(counterType, counterAmount, false);
                }
            }
        }
    }

}

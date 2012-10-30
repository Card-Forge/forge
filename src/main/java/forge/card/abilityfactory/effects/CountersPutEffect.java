package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;

import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class CountersPutEffect extends SpellEffect {
    /**
     * <p>
     * putResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        final String type = params.get("CounterType");
        int counterAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("CounterNum"), sa);
        final int max = params.containsKey("MaxFromEffect") ? Integer.parseInt(params.get("MaxFromEffect")) : -1;

        if (params.containsKey("UpTo")) {
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

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

        for (final Card tgtCard : tgtCards) {
            if ((tgt == null) || tgtCard.canBeTargetedBy(sa)) {
                if (max != -1) {
                    counterAmount = max - tgtCard.getCounters(Counters.valueOf(type));
                }
                final Zone zone = Singletons.getModel().getGame().getZoneOf(tgtCard);
                if (zone == null) {
                    // Do nothing, token disappeared
                } else if (zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                    tgtCard.addCounter(Counters.valueOf(type), counterAmount);
                } else {
                    // adding counters to something like re-suspend cards
                    tgtCard.addCounterFromNonEffect(Counters.valueOf(type), counterAmount);
                }
            }
        }
    }

    /**
     * <p>
     * putStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card card = sa.getSourceCard();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(card.getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        } else {
            final Counters cType = Counters.valueOf(params.get("CounterType"));
            final int amount = AbilityFactory.calculateAmount(card, params.get("CounterNum"), sa);
            sb.append("Put ");
            if (params.containsKey("UpTo")) {
                sb.append("up to ");
            }
            sb.append(amount).append(" ").append(cType.getName()).append(" counter");
            if (amount != 1) {
                sb.append("s");
            }
            sb.append(" on ");
            ArrayList<Card> tgtCards;
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgtCards = tgt.getTargetCards();
            } else {
                tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
            }
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
        }
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    // *******************************************
    // ********** RemoveCounters *****************
    // *******************************************

}
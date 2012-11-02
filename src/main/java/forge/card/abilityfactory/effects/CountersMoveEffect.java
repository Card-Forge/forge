package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.Counters;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class CountersMoveEffect extends SpellEffect { 

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }
    
        Card source = null;
        ArrayList<Card> srcCards;
        final Target tgt = sa.getTarget();
        if (!params.containsKey("Source") && tgt != null) {
            srcCards = tgt.getTargetCards();
        } else {
            srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        }
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }
        ArrayList<Card> tgtCards;
        if (!params.containsKey("Defined") && tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }
    
        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("CounterNum"), sa);
    
        sb.append("Move ").append(amount).append(" ").append(cType.getName()).append(" counter");
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ");
        sb.append(source).append(" to ").append(tgtCards.get(0));
    
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("CounterNum"), sa);

        Card source = null;
        ArrayList<Card> srcCards;
        final Target tgt = sa.getTarget();
        if (!params.containsKey("Source") && tgt != null) {
            srcCards = tgt.getTargetCards();
        } else {
            srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        }
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }
        ArrayList<Card> tgtCards;
        if (!params.containsKey("Defined") && tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        }

        for (final Card dest : tgtCards) {
            if ((null != source) && (null != dest)) {
                if (source.getCounters(cType) >= amount) {
                    if (!dest.hasKeyword("CARDNAME can't have counters placed on it.")
                            && !(dest.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && cType
                                    .equals(Counters.M1M1))) {
                        dest.addCounter(cType, amount);
                        source.subtractCounter(cType, amount);
                    }
                }
            }
        }
    } // moveCounterResolve

} 
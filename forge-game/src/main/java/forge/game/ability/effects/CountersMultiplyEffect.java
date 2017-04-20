package forge.game.ability.effects;

import java.util.Map;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class CountersMultiplyEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final CounterType counterType = getCounterType(sa);
        
        sb.append("Double the number of ");

        if (counterType != null) {
            sb.append(counterType.getName());
            sb.append(" counters");
        } else {
            sb.append("each kind of counter");
        }
        sb.append(" on ");

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));

        sb.append(".");

        return sb.toString();
    }
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final CounterType counterType = getCounterType(sa);
        final int n = Integer.valueOf(sa.getParamOrDefault("Multiplier", "2")) - 1; 
        
        for (final Card tgtCard : getTargetCards(sa)) {
            if (counterType != null) {
                tgtCard.addCounter(counterType, tgtCard.getCounters(counterType) * n, host, false);
            } else {
                for (Map.Entry<CounterType, Integer> e : tgtCard.getCounters().entrySet()) {
                    tgtCard.addCounter(e.getKey(), e.getValue() * n, host, false);
                }
            }
        }
    }

    
    private CounterType getCounterType(SpellAbility sa) {
        if (sa.hasParam("CounterType")) {
            try {
                return AbilityUtils.getCounterType(sa.getParam("CounterType"), sa);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return null;
            }
        }
        return null;
    }
}

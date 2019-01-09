package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.game.spellability.SpellAbility;

public class CardCloneStates extends ForwardingMap<CardStateName, CardState> {

    private Map<CardStateName, CardState> dataMap = Maps.newEnumMap(CardStateName.class);

    private Card origin = null;
    private SpellAbility sa = null;

    public CardCloneStates(Card origin, SpellAbility sa) {
        super();
        this.origin = origin;
        this.sa = sa;
    }

    public Card getOrigin() {
        return origin;
    }

    public SpellAbility getSource() {
        return sa;
    }
    
    public Card getHost() {
        return sa.getHostCard();
    }

    @Override
    protected Map<CardStateName, CardState> delegate() {
        return dataMap;
    }

    public CardState get(CardStateName key) {
        if (dataMap.containsKey(key)) {
            return super.get(key);
        }
        CardState original = super.get(CardStateName.Original);
        // need to copy it so the view has the right state name
        CardState result = new CardState(original.getCard(), key);
        result.copyFrom(original, false);
        dataMap.put(key, result);
        return result;
    }

    
}

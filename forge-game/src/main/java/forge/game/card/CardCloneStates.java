package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.game.CardTraitBase;

public class CardCloneStates extends ForwardingMap<CardStateName, CardState> {

    private Map<CardStateName, CardState> dataMap = Maps.newEnumMap(CardStateName.class);

    private Card origin;
    private CardTraitBase ctb;

    public CardCloneStates(Card origin, CardTraitBase sa) {
        super();
        this.origin = origin;
        this.ctb = sa;
    }

    public Card getOrigin() {
        return origin;
    }

    public CardTraitBase getSource() {
        return ctb;
    }
    
    public Card getHost() {
        return ctb.getHostCard();
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

    public CardCloneStates copy(final Card host, final boolean lki) {
        CardCloneStates result = new CardCloneStates(origin, ctb);
        for (Map.Entry<CardStateName, CardState> e : dataMap.entrySet()) {
            result.put(e.getKey(), e.getValue().copy(host, e.getKey(), lki));
        }
        return result;
    }
}

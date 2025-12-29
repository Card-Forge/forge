package forge.card;

import java.util.Map;

public record TextChangedType(Map<String, String> textMap) implements ICardChangedType {

    @Override
    public CardType applyChanges(CardType newType) {
        for (Map.Entry<String, String> e : textMap.entrySet()) {
            if (newType.hasStringType(e.getKey())) {
                newType.subtypes.remove(e.getKey());
                newType.subtypes.add(e.getValue());
            }
        }
        return newType;
    }

}

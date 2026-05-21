package forge.card;

import java.util.Map;

public record WordChangedType(String oldWord, String newWord) implements ICardChangedType, IChangedText {

    @Override
    public CardType applyChanges(CardType newType) {
        if (newType.hasStringType(oldWord)) {
            newType.subtypes.remove(oldWord);
            newType.subtypes.add(newWord);
        }
        return newType;
    }

    @Override
    public void applyTypeChanges(Map<String, String> result) {
        for (Map.Entry<String, String> e : result.entrySet()) {
            if (e.getValue().equals(oldWord)) {
                e.setValue(newWord);
            }
        }
        result.put(oldWord, newWord);
    }
}

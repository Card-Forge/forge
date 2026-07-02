package forge.card;

public record WordChangedType(String oldWord, String newWord) implements ICardChangedType {

    @Override
    public CardType applyChanges(CardType newType) {
        if (newType.hasStringType(oldWord)) {
            newType.subtypes.remove(oldWord);
            newType.subtypes.add(newWord);
        }
        return newType;
    }
}

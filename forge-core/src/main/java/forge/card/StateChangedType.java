package forge.card;

public record StateChangedType(CardType type) implements ICardChangedType {

    @Override
    public CardType applyChanges(CardType newType) {
        return new CardType(type);
    }
}

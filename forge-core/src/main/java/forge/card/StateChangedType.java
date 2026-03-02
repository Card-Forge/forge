package forge.card;

public record StateChangedType(CardTypeView type) implements ICardChangedType {

    @Override
    public CardType applyChanges(CardType newType) {
        return new CardType(type);
    }
}

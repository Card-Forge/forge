package forge.card;

public record StateChangedType(CardTypeView type) implements ICardChangedType, ResetChangedText {

    @Override
    public CardType applyChanges(CardType newType) {
        return new CardType(type);
    }
}

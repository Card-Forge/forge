package forge.card;

public interface ICardChangedType {

    CardType applyChanges(CardType newType);
    default boolean isRemoveLandTypes() {
        return false;
    }
}
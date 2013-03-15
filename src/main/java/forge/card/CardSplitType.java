package forge.card;

import forge.CardCharacteristicName;

public enum CardSplitType
{
    None(AggregationMethod.USE_PRIMARY_FACE, null),
    Transform(AggregationMethod.USE_ACTIVE_FACE, CardCharacteristicName.Transformed),
    Split(AggregationMethod.AGGREGATE, CardCharacteristicName.RightSplit),
    Flip(AggregationMethod.USE_PRIMARY_FACE, CardCharacteristicName.Flipped),
    // used by 12 licid creatures to switch type into enchantment aura
    Licid(AggregationMethod.USE_PRIMARY_FACE, CardCharacteristicName.Licid); 

    private CardSplitType(AggregationMethod calcMode, CardCharacteristicName stateName) {
        method = calcMode;
        this.changedStateName = stateName;
    }

    public AggregationMethod getAggregationMethod() {
        return method;
    }

    private final AggregationMethod method;
    private final CardCharacteristicName changedStateName;
    
    public static CardSplitType smartValueOf(String text) {
        if ("DoubleFaced".equals(text)) return Transform;
        // Will throw exceptions here if bad text passed
        CardSplitType res = CardSplitType.valueOf(text);
        return res;
    }

    public CardCharacteristicName getChangedStateName() {
        return changedStateName;
    }
}

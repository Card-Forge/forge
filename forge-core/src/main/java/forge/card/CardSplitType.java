package forge.card;

import forge.card.CardFace.FaceSelectionMethod;

public enum CardSplitType
{
    None(FaceSelectionMethod.USE_PRIMARY_FACE, null),
    Transform(FaceSelectionMethod.USE_ACTIVE_FACE, CardCharacteristicName.Transformed),
    Split(FaceSelectionMethod.COMBINE, CardCharacteristicName.RightSplit),
    Flip(FaceSelectionMethod.USE_PRIMARY_FACE, CardCharacteristicName.Flipped);

    private CardSplitType(FaceSelectionMethod calcMode, CardCharacteristicName stateName) {
        method = calcMode;
        this.changedStateName = stateName;
    }

    public FaceSelectionMethod getAggregationMethod() {
        return method;
    }

    private final FaceSelectionMethod method;
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

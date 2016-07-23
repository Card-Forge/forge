package forge.card;

import forge.card.CardFace.FaceSelectionMethod;

public enum CardSplitType
{
    None(FaceSelectionMethod.USE_PRIMARY_FACE, null),
    Transform(FaceSelectionMethod.USE_ACTIVE_FACE, CardStateName.Transformed),
    Meld(FaceSelectionMethod.USE_ACTIVE_FACE, CardStateName.Meld),
    Split(FaceSelectionMethod.COMBINE, CardStateName.RightSplit),
    Flip(FaceSelectionMethod.USE_PRIMARY_FACE, CardStateName.Flipped);

    private CardSplitType(FaceSelectionMethod calcMode, CardStateName stateName) {
        method = calcMode;
        this.changedStateName = stateName;
    }

    public FaceSelectionMethod getAggregationMethod() {
        return method;
    }

    private final FaceSelectionMethod method;
    private final CardStateName changedStateName;
    
    public static CardSplitType smartValueOf(String text) {
        if ("DoubleFaced".equals(text)) return Transform;
        // Will throw exceptions here if bad text passed
        CardSplitType res = CardSplitType.valueOf(text);
        return res;
    }

    public CardStateName getChangedStateName() {
        return changedStateName;
    }
}

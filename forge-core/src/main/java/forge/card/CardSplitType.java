package forge.card;

import java.util.EnumSet;

public enum CardSplitType
{
    None(FaceSelectionMethod.USE_PRIMARY_FACE, null),
    Transform(FaceSelectionMethod.USE_ACTIVE_FACE, CardStateName.Backside),
    Meld(FaceSelectionMethod.USE_ACTIVE_FACE, CardStateName.Meld),
    Split(FaceSelectionMethod.COMBINE, CardStateName.RightSplit),
    Flip(FaceSelectionMethod.USE_PRIMARY_FACE, CardStateName.Flipped),
    Adventure(FaceSelectionMethod.USE_PRIMARY_FACE, CardStateName.Secondary),
    Omen(FaceSelectionMethod.USE_PRIMARY_FACE, CardStateName.Secondary),
    Modal(FaceSelectionMethod.USE_ACTIVE_FACE, CardStateName.Backside),
    Specialize(FaceSelectionMethod.USE_ACTIVE_FACE, null);

    public static final EnumSet<CardSplitType> DUAL_FACED_CARDS = EnumSet.of(
            CardSplitType.Transform, CardSplitType.Meld, CardSplitType.Modal);

    CardSplitType(FaceSelectionMethod calcMode, CardStateName stateName) {
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

    public enum FaceSelectionMethod {
        USE_ACTIVE_FACE,
        USE_PRIMARY_FACE,
        COMBINE
    }
}

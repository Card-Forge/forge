package forge.card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CardSplitType
{
    None(AggregationMethod.USE_PRIMARY_FACE),
    Transform(AggregationMethod.USE_ACTIVE_FACE),
    Split(AggregationMethod.AGGREGATE),
    Flip(AggregationMethod.USE_PRIMARY_FACE);
    
    
    private CardSplitType(AggregationMethod calcMode) {
        method = calcMode;
    }
    
    /**
     * @return the calculationMode
     */
    public AggregationMethod getAggregationMethod() {
        return method;
    }

    private final AggregationMethod method;
    
    
    public static CardSplitType smartValueOf(String text) {
        if ("DoubleFaced".equals(text)) return Transform;
        if ("Alternate".equals(text)) return None;
        // Will throw exceptions here if bad text passed
        CardSplitType res = CardSplitType.valueOf(text);
        return res;
    }
}


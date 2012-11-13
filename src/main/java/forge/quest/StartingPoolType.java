package forge.quest;

public enum StartingPoolType {
    Complete("Unrestricted"),
    Rotating("Sanctioned format"),
    CustomFormat("Custom format"),
    Precon("Event or starter deck"),
    SealedDeck("My sealed deck"),
    DraftDeck("My draft deck");
    
    private final String caption;
    
    private StartingPoolType(String caption0) {
        caption = caption0;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return caption;
    }
}

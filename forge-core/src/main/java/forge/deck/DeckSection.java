package forge.deck;

public enum DeckSection {
    Avatar(1),
    Commander(1),
    Main(60),
    Sideboard(15),
    Planes(10),
    Schemes(20);

    private final int typicalSize; // Rules enforcement is done in DeckFormat class, this is for reference only
    private DeckSection(int commonSize) {
        typicalSize = commonSize;
    }
    
    public boolean isSingleCard() { return typicalSize == 1; }
    
    public static DeckSection smartValueOf(String value) {
        if (value == null) {
            return null;
        }
        
        final String valToCompate = value.trim();
        for (final DeckSection v : DeckSection.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        
        return null;
    }
}

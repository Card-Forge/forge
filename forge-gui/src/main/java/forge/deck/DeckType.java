package forge.deck;

public enum DeckType {
    CUSTOM_DECK ("Custom User Decks"),
    CONSTRUCTED_DECK ("Constructed Decks"),
    COMMANDER_DECK ("Commander Decks"),
    SCHEME_DECKS ("Scheme Decks"),
    PLANAR_DECKS ("Planar Decks"),
    DRAFT_DECKS ("Draft Decks"),
    SEALED_DECKS ("Sealed Decks"),
    PRECONSTRUCTED_DECK("Preconstructed Decks"),
    QUEST_OPPONENT_DECK ("Quest Opponent Decks"),
    COLOR_DECK ("Random Color Decks"),
    THEME_DECK ("Random Theme Decks"),
    RANDOM_DECK ("Random Decks"),
    NET_DECK ("Net Decks"),
    NET_COMMANDER_DECK ("Net Commander Decks");

    private String value;
    private DeckType(String value) {
        this.value = value;
    }
    @Override
    public String toString() {
        return value;
    }
    public static DeckType fromString(String value){
        for (final DeckType d : DeckType.values()) {
            if (d.toString().equalsIgnoreCase(value)) {
                return d;
            }
        }
        throw new IllegalArgumentException("No Enum specified for this string");
    }
}

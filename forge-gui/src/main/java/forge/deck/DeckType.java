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

    public static final DeckType[] ConstructedOptions = new DeckType[] {
        DeckType.CUSTOM_DECK,
        DeckType.PRECONSTRUCTED_DECK,
        DeckType.QUEST_OPPONENT_DECK,
        DeckType.COLOR_DECK,
        DeckType.THEME_DECK,
        DeckType.RANDOM_DECK,
        DeckType.NET_DECK
    };

    private String value;
    private DeckType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

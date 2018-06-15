package forge.deck;

import forge.model.FModel;

public enum DeckType {
    CUSTOM_DECK ("Custom User Decks"),
    CONSTRUCTED_DECK ("Constructed Decks"),
    COMMANDER_DECK ("Commander Decks"),
    RANDOM_COMMANDER_DECK ("Random Commander Decks"),
    RANDOM_CARDGEN_COMMANDER_DECK ("Random Commander Card-based Decks"),
    TINY_LEADERS_DECKS ("Tiny Leaders Decks"),
    BRAWL_DECKS ("Brawl Decks"),
    SCHEME_DECKS ("Scheme Decks"),
    PLANAR_DECKS ("Planar Decks"),
    DRAFT_DECKS ("Draft Decks"),
    SEALED_DECKS ("Sealed Decks"),
    PRECONSTRUCTED_DECK("Preconstructed Decks"),
    QUEST_OPPONENT_DECK ("Quest Opponent Decks"),
    COLOR_DECK ("Random Color Decks"),
    STANDARD_CARDGEN_DECK ("Random Standard Archetype Decks"),
    MODERN_CARDGEN_DECK ("Random Modern Archetype Decks"),
    LEGACY_CARDGEN_DECK ("Random Legacy Archetype Decks"),
    VINTAGE_CARDGEN_DECK ("Random Vintage Archetype Decks"),
    STANDARD_COLOR_DECK ("Random Standard Color Decks"),
    MODERN_COLOR_DECK ("Random Modern Color Decks"),
    THEME_DECK ("Random Theme Decks"),
    RANDOM_DECK ("Random Decks"),
    NET_DECK ("Net Decks"),
    NET_COMMANDER_DECK ("Net Commander Decks");

    public static DeckType[] ConstructedOptions;
    public static DeckType[] CommanderOptions;

    static {
        if (FModel.isdeckGenMatrixLoaded()) {
            ConstructedOptions = new DeckType[]{
                    DeckType.CUSTOM_DECK,
                    DeckType.PRECONSTRUCTED_DECK,
                    DeckType.QUEST_OPPONENT_DECK,
                    DeckType.COLOR_DECK,
                    DeckType.STANDARD_CARDGEN_DECK,
                    DeckType.MODERN_CARDGEN_DECK,
                    DeckType.LEGACY_CARDGEN_DECK,
                    DeckType.VINTAGE_CARDGEN_DECK,
                    DeckType.STANDARD_COLOR_DECK,
                    DeckType.MODERN_COLOR_DECK,
                    DeckType.THEME_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_DECK
            };
        } else {
            ConstructedOptions = new DeckType[]{
                    DeckType.CUSTOM_DECK,
                    DeckType.PRECONSTRUCTED_DECK,
                    DeckType.QUEST_OPPONENT_DECK,
                    DeckType.COLOR_DECK,
                    DeckType.STANDARD_COLOR_DECK,
                    DeckType.MODERN_COLOR_DECK,
                    DeckType.THEME_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_DECK
            };
        }
    }
    static {
        if (FModel.isdeckGenMatrixLoaded()) {
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_CARDGEN_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK
            };
        }else{
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK
            };
        }

    }

    private String value;
    private DeckType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

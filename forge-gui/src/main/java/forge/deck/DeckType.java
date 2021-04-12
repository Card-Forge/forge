package forge.deck;

import forge.model.FModel;
import forge.util.Localizer;

public enum DeckType {
    CUSTOM_DECK("lblCustomUserDecks"),
    CONSTRUCTED_DECK("lblConstructedDecks"),
    COMMANDER_DECK("lblCommanderDecks"),
    RANDOM_COMMANDER_DECK("lblRandomCommanderDecks"),
    RANDOM_CARDGEN_COMMANDER_DECK("lblRandomCommanderCard-basedDecks"),
    OATHBREAKER_DECK("lblOathbreakerDecks"),
    TINY_LEADERS_DECK("lblTinyLeadersDecks"),
    BRAWL_DECK("lblBrawlDecks"),
    SCHEME_DECK("lblSchemeDecks"),
    PLANAR_DECK("lblPlanarDecks"),
    DRAFT_DECK("lblDraftDecks"),
    SEALED_DECK("lblSealedDecks"),
    PRECONSTRUCTED_DECK("lblPreconstructedDecks"),
    PRECON_COMMANDER_DECK("lblPreconCommanderDecks"),
    QUEST_OPPONENT_DECK("lblQuestOpponentDecks"),
    COLOR_DECK("lblRandomColorDecks"),
    STANDARD_CARDGEN_DECK("lblRandomStandardArchetypeDecks"),
    PIONEER_CARDGEN_DECK("lblRandomPioneerArchetypeDecks"),
    HISTORIC_CARDGEN_DECK("lblRandomHistoricArchetypeDecks"),
    MODERN_CARDGEN_DECK("lblRandomModernArchetypeDecks"),
    LEGACY_CARDGEN_DECK("lblRandomLegacyArchetypeDecks"),
    VINTAGE_CARDGEN_DECK("lblRandomVintageArchetypeDecks"),
    STANDARD_COLOR_DECK("lblRandomStandardColorDecks"),
    MODERN_COLOR_DECK("lblRandomModernColorDecks"),
    THEME_DECK("lblRandomThemeDecks"),
    RANDOM_DECK("lblRandomDecks"),
    NET_DECK("lblNetDecks"),
    NET_COMMANDER_DECK("lblNetCommanderDecks"),
    NET_ARCHIVE_STANDARD_DECK("lblNetArchiveStandardDecks"),
    NET_ARCHIVE_PIONEER_DECK("lblNetArchivePioneerDecks"),
    NET_ARCHIVE_MODERN_DECK("lblNetArchiveModernDecks"),
    NET_ARCHIVE_LEGACY_DECK("lblNetArchiveLegacyDecks"),
    NET_ARCHIVE_VINTAGE_DECK("lblNetArchiveVintageDecks"),
    NET_ARCHIVE_BLOCK_DECK("lblNetArchiveBlockDecks");

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
                    DeckType.PIONEER_CARDGEN_DECK,
                    DeckType.HISTORIC_CARDGEN_DECK,
                    DeckType.MODERN_CARDGEN_DECK,
                    DeckType.LEGACY_CARDGEN_DECK,
                    DeckType.VINTAGE_CARDGEN_DECK,
                    DeckType.STANDARD_COLOR_DECK,
                    DeckType.MODERN_COLOR_DECK,
                    DeckType.THEME_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_DECK,
                    DeckType.NET_ARCHIVE_STANDARD_DECK,
                    DeckType.NET_ARCHIVE_PIONEER_DECK,
                    DeckType.NET_ARCHIVE_MODERN_DECK,
                    DeckType.NET_ARCHIVE_LEGACY_DECK,
                    DeckType.NET_ARCHIVE_VINTAGE_DECK,
                    DeckType.NET_ARCHIVE_BLOCK_DECK
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
                    DeckType.NET_DECK,
                    DeckType.NET_ARCHIVE_STANDARD_DECK,
                    DeckType.NET_ARCHIVE_PIONEER_DECK,
                    DeckType.NET_ARCHIVE_MODERN_DECK,
                    DeckType.NET_ARCHIVE_LEGACY_DECK,
                    DeckType.NET_ARCHIVE_VINTAGE_DECK
            };
        }
    }
    static {
        if (FModel.isdeckGenMatrixLoaded()) {
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.PRECON_COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_CARDGEN_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK
            };
        }else{
            CommanderOptions = new DeckType[]{
                    DeckType.COMMANDER_DECK,
                    DeckType.PRECON_COMMANDER_DECK,
                    DeckType.RANDOM_COMMANDER_DECK,
                    DeckType.RANDOM_DECK,
                    DeckType.NET_COMMANDER_DECK
            };
        }

    }

    private String value;
    DeckType(final String value) {
        final Localizer localizer = Localizer.getInstance();
        this.value = localizer.getMessage(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

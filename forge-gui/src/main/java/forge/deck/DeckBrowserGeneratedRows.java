package forge.deck;

import com.google.common.collect.ImmutableList;
import forge.game.GameFormat;
import forge.game.IHasGameType;
import forge.gamemodes.quest.QuestController;
import forge.itemmanager.IItemManager;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class DeckBrowserGeneratedRows {
    public static final String HOME_PATH = "";
    public static final String RANDOM_PATH = "random";
    public static final String RANDOM_COLORS_PATH = "random/colors";
    public static final String RANDOM_ARCHETYPES_PATH = "random/archetypes";
    public static final String RANDOM_ARCHETYPE_GROUP_NAME = "Random Archetype Decks";

    private static final DeckType[] RANDOM_ARCHETYPE_DECK_TYPES = {
            DeckType.STANDARD_CARDGEN_DECK,
            DeckType.PIONEER_CARDGEN_DECK,
            DeckType.HISTORIC_CARDGEN_DECK,
            DeckType.MODERN_CARDGEN_DECK,
            DeckType.LEGACY_CARDGEN_DECK,
            DeckType.VINTAGE_CARDGEN_DECK,
            DeckType.PAUPER_CARDGEN_DECK
    };
    private static final DeckType[] RANDOM_COLOR_DECK_TYPES = {
            DeckType.STANDARD_COLOR_DECK,
            DeckType.MODERN_COLOR_DECK,
            DeckType.PAUPER_COLOR_DECK
    };

    private static final Set<DeckType> RANDOM_ARCHETYPE_DECK_TYPE_SET =
            EnumSet.copyOf(Arrays.asList(RANDOM_ARCHETYPE_DECK_TYPES));
    private static final Set<DeckType> RANDOM_COLOR_DECK_TYPE_SET =
            EnumSet.copyOf(Arrays.asList(RANDOM_COLOR_DECK_TYPES));
    private static final Set<DeckType> COMMANDER_GENERATED_DECK_TYPES = EnumSet.of(
            DeckType.RANDOM_COMMANDER_DECK,
            DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
    private static final Set<DeckType> CONSTRUCTED_LIST_DECK_TYPES = EnumSet.of(
            DeckType.PRECONSTRUCTED_DECK,
            DeckType.QUEST_OPPONENT_DECK);
    private static final Set<DeckType> GENERATED_RANDOM_PARENT_DECK_TYPES = EnumSet.of(
            DeckType.THEME_DECK,
            DeckType.RANDOM_COMMANDER_DECK,
            DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
    private static final Set<DeckType> GENERATED_DECK_TYPES = getGeneratedDeckTypes();

    private DeckBrowserGeneratedRows() {
    }

    public static boolean isGeneratedDeckType(final DeckType deckType) {
        return GENERATED_DECK_TYPES.contains(deckType);
    }

    public static boolean isConstructedListDeckType(final DeckType deckType) {
        return CONSTRUCTED_LIST_DECK_TYPES.contains(deckType);
    }

    public static boolean isCommanderGeneratedDeckType(final DeckType deckType) {
        return COMMANDER_GENERATED_DECK_TYPES.contains(deckType);
    }

    public static String getDefaultGeneratedParentPath(final DeckType deckType) {
        if (deckType == DeckType.COLOR_DECK || RANDOM_COLOR_DECK_TYPE_SET.contains(deckType)) {
            return RANDOM_COLORS_PATH;
        }
        if (RANDOM_ARCHETYPE_DECK_TYPE_SET.contains(deckType)) {
            return RANDOM_ARCHETYPES_PATH;
        }
        if (GENERATED_RANDOM_PARENT_DECK_TYPES.contains(deckType)) {
            return RANDOM_PATH;
        }
        return HOME_PATH;
    }

    public static String getGeneratedGroupDisplayName(final String path) {
        if (RANDOM_ARCHETYPES_PATH.equals(path)) {
            return RANDOM_ARCHETYPE_GROUP_NAME;
        }
        if (RANDOM_COLORS_PATH.equals(path)) {
            return DeckType.COLOR_DECK.toString();
        }
        if (RANDOM_PATH.equals(path)) {
            return DeckType.RANDOM_DECK.toString();
        }
        return lastPathSegment(path);
    }

    public static String getGeneratedGroupParentPath(final String path) {
        if (RANDOM_COLORS_PATH.equals(path) || RANDOM_ARCHETYPES_PATH.equals(path)) {
            return RANDOM_PATH;
        }
        return HOME_PATH;
    }

    public static DeckType getGeneratedGroupParentRootType(final String path) {
        return RANDOM_PATH.equals(path) ? DeckType.CUSTOM_DECK : null;
    }

    public static DeckType getGeneratedGroupShortcutDeckType(final String path) {
        if (RANDOM_COLORS_PATH.equals(path)) {
            return DeckType.COLOR_DECK;
        }
        if (RANDOM_PATH.equals(path) || RANDOM_ARCHETYPES_PATH.equals(path)) {
            return DeckType.RANDOM_DECK;
        }
        return null;
    }

    public static void addGeneratedGroupRows(final List<DeckProxy> rows, final String path,
            final IItemManager<DeckProxy> deckManager, final IHasGameType gameTypeProvider,
            final boolean isAi, final boolean includeGeneratedOptions) {
        if (RANDOM_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(RandomDeckGenerator.getRandomDecks(gameTypeProvider, isAi)));
            rows.add(DeckBrowserEntry.generatedGroup(RANDOM_ARCHETYPE_GROUP_NAME, RANDOM_ARCHETYPES_PATH));
            if (includeGeneratedOptions) {
                addGeneratedGroupRows(rows, RANDOM_ARCHETYPES_PATH, deckManager, gameTypeProvider, isAi, true);
            }
            rows.add(DeckBrowserEntry.generatedGroup(DeckType.COLOR_DECK.toString(), RANDOM_COLORS_PATH));
            if (includeGeneratedOptions) {
                addGeneratedGroupRows(rows, RANDOM_COLORS_PATH, deckManager, gameTypeProvider, isAi, true);
            }
            addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi, DeckType.THEME_DECK);
        } else if (RANDOM_COLORS_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(ColorDeckGenerator.getColorDecks(deckManager, null, isAi)));
            addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi, RANDOM_COLOR_DECK_TYPES);
        } else if (RANDOM_ARCHETYPES_PATH.equals(path) && FModel.isdeckGenMatrixLoaded()) {
            addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi, RANDOM_ARCHETYPE_DECK_TYPES);
        }
    }

    public static void addConstructedFolderRows(final List<DeckProxy> rows, final String path,
            final boolean includeGeneratedOptions, final IItemManager<DeckProxy> deckManager,
            final IHasGameType gameTypeProvider, final boolean isAi) {
        rows.add(DeckBrowserEntry.generatedGroup(DeckType.RANDOM_DECK.toString(), RANDOM_PATH));
        if (includeGeneratedOptions) {
            addGeneratedGroupRows(rows, RANDOM_PATH, deckManager, gameTypeProvider, isAi, true);
        }
        addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi,
                DeckType.PRECONSTRUCTED_DECK, DeckType.QUEST_OPPONENT_DECK);
    }

    public static void addCommanderFolderRows(final List<DeckProxy> rows, final String path,
            final boolean includeGeneratedOptions, final IItemManager<DeckProxy> deckManager,
            final IHasGameType gameTypeProvider, final boolean isAi) {
        addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi,
                DeckType.RANDOM_COMMANDER_DECK);
        if (FModel.isdeckGenMatrixLoaded()) {
            addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi,
                    DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
        }
        addGeneratedFolderRows(rows, path, includeGeneratedOptions, deckManager, gameTypeProvider, isAi,
                DeckType.PRECON_COMMANDER_DECK);
    }

    public static void addGeneratedFolderRows(final List<DeckProxy> rows, final String path,
            final boolean includeGeneratedOptions, final IItemManager<DeckProxy> deckManager,
            final IHasGameType gameTypeProvider, final boolean isAi, final DeckType... deckTypes) {
        for (final DeckType deckType : deckTypes) {
            rows.add(DeckBrowserEntry.generatedFolder(deckType.toString(), path, deckType));
            if (includeGeneratedOptions) {
                addGeneratedRows(rows, deckType, deckManager, gameTypeProvider, isAi);
            }
        }
    }

    public static void addGeneratedRows(final List<DeckProxy> rows, final DeckType deckType,
            final IItemManager<DeckProxy> deckManager, final IHasGameType gameTypeProvider, final boolean isAi) {
        if (deckType != null) {
            rows.addAll(wrapGeneratedOptions(getGeneratedDecks(deckType, deckManager, gameTypeProvider, isAi)));
        }
    }

    public static Iterable<DeckProxy> getGeneratedDecks(final DeckType deckType,
            final IItemManager<DeckProxy> deckManager, final IHasGameType gameTypeProvider, final boolean isAi) {
        switch (deckType) {
        case COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(deckManager, null, isAi);
        case STANDARD_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(deckManager, FModel.getFormats().getStandard().getFilterPrinted(), isAi);
        case MODERN_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(deckManager, FModel.getFormats().getModern().getFilterPrinted(), isAi);
        case PAUPER_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(deckManager, FModel.getFormats().getPauper().getFilterPrinted(), isAi);
        case STANDARD_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().getStandard(), isAi);
        case PIONEER_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().getPioneer(), isAi);
        case HISTORIC_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().getHistoric(), isAi);
        case MODERN_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().getModern(), isAi);
        case LEGACY_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().get("Legacy"), isAi);
        case VINTAGE_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().get("Vintage"), isAi);
        case PAUPER_CARDGEN_DECK:
            return getMatrixDecks(FModel.getFormats().getPauper(), isAi);
        case RANDOM_COMMANDER_DECK:
            return CommanderDeckGenerator.getCommanderDecks(DeckFormat.Commander, isAi, false);
        case RANDOM_CARDGEN_COMMANDER_DECK:
            return FModel.isdeckGenMatrixLoaded()
                    ? CommanderDeckGenerator.getCommanderDecks(DeckFormat.Commander, isAi, true) : ImmutableList.of();
        case THEME_DECK:
            return DeckProxy.getAllThemeDecks();
        case QUEST_OPPONENT_DECK:
            return DeckProxy.getAllQuestEventAndChallenges();
        case PRECONSTRUCTED_DECK:
            return DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons());
        case PRECON_COMMANDER_DECK:
            return DeckProxy.getAllCommanderPreconDecks();
        case RANDOM_DECK:
            return RandomDeckGenerator.getRandomDecks(gameTypeProvider, isAi);
        default:
            return ImmutableList.of();
        }
    }

    private static Iterable<DeckProxy> getMatrixDecks(final GameFormat format, final boolean isAi) {
        return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(format, isAi) : ImmutableList.of();
    }

    private static List<DeckProxy> wrapGeneratedOptions(final Iterable<DeckProxy> decks) {
        final List<DeckProxy> entries = new ArrayList<>();
        for (final DeckProxy deck : decks) {
            entries.add(DeckBrowserEntry.fromDeckProxy(deck));
        }
        return entries;
    }

    private static Set<DeckType> getGeneratedDeckTypes() {
        final EnumSet<DeckType> deckTypes = EnumSet.of(DeckType.COLOR_DECK, DeckType.RANDOM_DECK);
        deckTypes.addAll(RANDOM_COLOR_DECK_TYPE_SET);
        deckTypes.addAll(RANDOM_ARCHETYPE_DECK_TYPE_SET);
        deckTypes.addAll(COMMANDER_GENERATED_DECK_TYPES);
        deckTypes.addAll(GENERATED_RANDOM_PARENT_DECK_TYPES);
        return deckTypes;
    }

    private static String lastPathSegment(final String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        final int idx = path.lastIndexOf('/');
        return idx < 0 ? path : path.substring(idx + 1);
    }
}

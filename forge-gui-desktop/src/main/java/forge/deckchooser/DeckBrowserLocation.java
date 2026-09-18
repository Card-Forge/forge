package forge.deckchooser;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckType;
import forge.deck.DeckUrlLoader;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;

final class DeckBrowserLocation {
    private DeckBrowserLocation() {
    }

    static IStorage<Deck> storageFor(final DeckType deckType) {
        return deckType == null ? FModel.getDecks().getConstructed() : deckStorage(deckType, false);
    }

    static IStorage<Deck> freshStorageFor(final DeckType deckType) {
        return deckType == null ? decksHomeStorage() : deckStorage(deckType, true);
    }

    private static IStorage<Deck> deckStorage(final DeckType deckType, final boolean fresh) {
        return switch (deckType) {
        case NET_DECK, NET_COMMANDER_DECK -> freshStorage("Net decks", ForgeConstants.DECK_NET_DIR);
        case OATHBREAKER_DECK -> fresh ? freshStorage("Oathbreaker decks", ForgeConstants.DECK_OATHBREAKER_DIR)
                : FModel.getDecks().getOathbreaker();
        case BRAWL_DECK -> fresh ? freshStorage("Brawl decks", ForgeConstants.DECK_BRAWL_DIR)
                : FModel.getDecks().getBrawl();
        case TINY_LEADERS_DECK -> fresh ? freshStorage("Tiny Leaders decks", ForgeConstants.DECK_TINY_LEADERS_DIR)
                : FModel.getDecks().getTinyLeaders();
        case COMMANDER_DECK -> fresh ? freshStorage("Commander decks", ForgeConstants.DECK_COMMANDER_DIR)
                : FModel.getDecks().getCommander();
        case PROVIDED_DECK_URL -> DeckUrlLoader.getStorage();
        default -> fresh ? freshStorage("Constructed decks", ForgeConstants.DECK_CONSTRUCTED_DIR, true)
                : FModel.getDecks().getConstructed();
        };
    }

    static IStorage<Deck> decksHomeStorage() {
        return freshStorage("Decks", ForgeConstants.DECK_BASE_DIR);
    }

    static IStorage<Deck> archiveStorage() {
        return freshStorage("Archive", ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    static GameType gameTypeFor(final DeckType deckType, final GameType customDeckGameType) {
        if (deckType == null) {
            return GameType.Constructed;
        }
        if (deckType == DeckType.CUSTOM_DECK) {
            final DeckFormat deckFormat = customDeckGameType == null
                    ? DeckFormat.Constructed : customDeckGameType.getDeckFormat();
            return switch (deckFormat) {
            case Commander -> GameType.Commander;
            case Oathbreaker -> GameType.Oathbreaker;
            case Brawl -> GameType.Brawl;
            case TinyLeaders -> GameType.TinyLeaders;
            default -> GameType.Constructed;
            };
        }
        return switch (deckType) {
        case OATHBREAKER_DECK -> GameType.Oathbreaker;
        case BRAWL_DECK -> GameType.Brawl;
        case TINY_LEADERS_DECK -> GameType.TinyLeaders;
        case NET_COMMANDER_DECK, COMMANDER_DECK -> GameType.Commander;
        default -> GameType.Constructed;
        };
    }

    static DeckType shortcutDeckType(final IStorage<Deck> folder, final boolean forCommander) {
        if (isSameFolder(folder, ForgeConstants.DECK_CONSTRUCTED_DIR)) {
            return DeckType.CUSTOM_DECK;
        }
        if (isSameFolder(folder, ForgeConstants.DECK_COMMANDER_DIR)) {
            return DeckType.COMMANDER_DECK;
        }
        if (isSameFolder(folder, ForgeConstants.DECK_OATHBREAKER_DIR)) {
            return DeckType.OATHBREAKER_DECK;
        }
        if (isSameFolder(folder, ForgeConstants.DECK_BRAWL_DIR)) {
            return DeckType.BRAWL_DECK;
        }
        if (isSameFolder(folder, ForgeConstants.DECK_TINY_LEADERS_DIR)) {
            return DeckType.TINY_LEADERS_DECK;
        }
        if (isSameFolder(folder, ForgeConstants.DECK_NET_DIR)) {
            return forCommander ? DeckType.NET_COMMANDER_DECK : DeckType.NET_DECK;
        }
        return isSameFolder(folder, DeckUrlLoader.getStorage()) ? DeckType.PROVIDED_DECK_URL : null;
    }

    static boolean isSameFolder(final IStorage<?> first, final IStorage<?> second) {
        return first != null && second != null
                && normalizedPath(first.getFullPath()).equals(normalizedPath(second.getFullPath()));
    }

    static boolean isSameFolder(final IStorage<?> folder, final String path) {
        return folder != null && normalizedPath(folder.getFullPath()).equals(normalizedPath(path));
    }

    static boolean isFolderUnder(final IStorage<?> folder, final String rootPath) {
        return folder != null && isPathUnder(normalizedPath(folder.getFullPath()), normalizedPath(rootPath));
    }

    static String relativeFolderPath(final IStorage<?> folder, final String rootPath) {
        if (folder == null) {
            return "";
        }
        return relativePath(normalizedPath(folder.getFullPath()), normalizedPath(rootPath));
    }

    static String relativePathToRoot(final String path, final DeckType rootType) {
        final IStorage<Deck> rootFolder = storageFor(rootType);
        if (rootFolder == null || StringUtils.isBlank(path)) {
            return "";
        }

        final String rootName = rootFolder.getName();
        return path.equals(rootName) ? "" : StringUtils.removeStart(path, rootName + "/");
    }

    static boolean isPathUnder(final Path path, final Path root) {
        return path.startsWith(root);
    }

    static String relativePath(final Path path, final Path root) {
        if (!isPathUnder(path, root)) {
            return "";
        }
        return root.relativize(path).toString().replace(File.separatorChar, '/');
    }

    static String firstSegment(final String path) {
        return StringUtils.substringBefore(StringUtils.defaultString(path), "/");
    }

    static String childPath(final String base, final String name) {
        return StringUtils.isBlank(base) ? name : base + "/" + name;
    }

    static String parentPath(final String path) {
        final String cleanPath = StringUtils.stripEnd(StringUtils.defaultString(path), "/");
        final int separator = cleanPath.lastIndexOf('/');
        return separator <= 0 ? "" : cleanPath.substring(0, separator);
    }

    static String lastSegment(final String path) {
        final String cleanPath = StringUtils.stripEnd(StringUtils.defaultString(path), "/");
        return StringUtils.substringAfterLast(cleanPath, "/");
    }

    private static Path normalizedPath(final String path) {
        return new File(path).toPath().toAbsolutePath().normalize();
    }

    private static IStorage<Deck> freshStorage(final String name, final String path) {
        return freshStorage(name, path, false);
    }

    private static IStorage<Deck> freshStorage(final String name, final String path,
            final boolean moveWronglyNamedDecks) {
        return new StorageImmediatelySerialized<>(name,
                new DeckStorage(new File(path), ForgeConstants.DECK_BASE_DIR, moveWronglyNamedDecks), true);
    }
}

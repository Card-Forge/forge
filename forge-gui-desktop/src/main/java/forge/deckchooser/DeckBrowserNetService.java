package forge.deckchooser;

import forge.deck.Deck;
import forge.deck.DeckBrowserEntry;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.NetDeckArchiveBlock;
import forge.deck.NetDeckArchiveLegacy;
import forge.deck.NetDeckArchiveModern;
import forge.deck.NetDeckArchivePauper;
import forge.deck.NetDeckArchivePioneer;
import forge.deck.NetDeckArchiveStandard;
import forge.deck.NetDeckArchiveVintage;
import forge.deck.NetDeckCategory;
import forge.deck.NetDeckStorageBase;
import forge.game.GameType;
import forge.util.storage.IStorage;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DeckBrowserNetService {
    private interface NetArchiveLoader {
        NetDeckStorageBase selectAndLoad(GameType gameType, String name, boolean forceDownload);
    }

    private record NetArchiveSpec(DeckType deckType, String prefix, NetArchiveLoader loader) {}

    record LoadedNetFolder(DeckType rootType, NetDeckCategory category) {}

    record LoadedArchiveFolder(DeckType deckType, IStorage<Deck> category) {}

    private static final NetArchiveSpec[] NET_ARCHIVE_SPECS = {
            new NetArchiveSpec(DeckType.NET_ARCHIVE_STANDARD_DECK, NetDeckArchiveStandard.PREFIX, NetDeckArchiveStandard::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_MODERN_DECK, NetDeckArchiveModern.PREFIX, NetDeckArchiveModern::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_PAUPER_DECK, NetDeckArchivePauper.PREFIX, NetDeckArchivePauper::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_PIONEER_DECK, NetDeckArchivePioneer.PREFIX, NetDeckArchivePioneer::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_LEGACY_DECK, NetDeckArchiveLegacy.PREFIX, NetDeckArchiveLegacy::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_VINTAGE_DECK, NetDeckArchiveVintage.PREFIX, NetDeckArchiveVintage::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_BLOCK_DECK, NetDeckArchiveBlock.PREFIX, NetDeckArchiveBlock::selectAndLoad)
    };

    private final Map<DeckType, NetDeckStorageBase> loadedNetArchiveCategories = new EnumMap<>(DeckType.class);

    boolean isNetArchiveDeckType(final DeckType deckType) {
        return getNetArchiveSpec(deckType) != null;
    }

    private NetArchiveSpec getNetArchiveSpec(final String savedDeckType) {
        if (savedDeckType == null) {
            return null;
        }
        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            if (savedDeckType.startsWith(spec.prefix)) {
                return spec;
            }
        }
        return null;
    }

    DeckType restoreSavedNetArchiveState(final String savedDeckType, final GameType gameType) {
        final NetArchiveSpec spec = getNetArchiveSpec(savedDeckType);
        if (spec == null) {
            return null;
        }
        setLoadedNetArchiveCategory(spec.deckType,
                spec.loader.selectAndLoad(gameType, savedDeckType.substring(spec.prefix.length()), false));
        return spec.deckType;
    }

    LoadedNetFolder reloadNetFolder(final DeckType rootType, final GameType gameType, final String name) {
        NetDeckCategory category = NetDeckCategory.selectAndLoad(gameType, name);
        if (category != null) {
            return new LoadedNetFolder(rootType, NetDeckCategory.selectAndLoad(gameType, name, true));
        }

        final DeckType alternateRootType = rootType == DeckType.NET_COMMANDER_DECK
                ? DeckType.NET_DECK : DeckType.NET_COMMANDER_DECK;
        final GameType alternateGameType = alternateRootType == DeckType.NET_COMMANDER_DECK
                ? GameType.Commander : GameType.Constructed;
        category = NetDeckCategory.selectAndLoad(alternateGameType, name);
        return new LoadedNetFolder(alternateRootType,
                category == null ? null : NetDeckCategory.selectAndLoad(alternateGameType, name, true));
    }

    IStorage<Deck> findSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        return loadSelectedNetArchiveCategory(gameType, deckType, name, false);
    }

    LoadedArchiveFolder reloadNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        if (findSelectedNetArchiveCategory(gameType, deckType, name) != null) {
            return new LoadedArchiveFolder(deckType, reloadSelectedNetArchiveCategory(gameType, deckType, name));
        }

        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            if (spec.deckType == deckType) { continue; }
            if (findSelectedNetArchiveCategory(gameType, spec.deckType, name) != null) {
                return new LoadedArchiveFolder(spec.deckType,
                        reloadSelectedNetArchiveCategory(gameType, spec.deckType, name));
            }
        }
        return null;
    }

    IStorage<Deck> reloadSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        return loadSelectedNetArchiveCategory(gameType, deckType, name, true);
    }

    IStorage<Deck> getLoadedNetArchiveCategory(final DeckType deckType) {
        return deckType == null ? null : loadedNetArchiveCategories.get(deckType);
    }

    void setLoadedNetArchiveCategory(final DeckType deckType, final IStorage<Deck> category) {
        if (!isNetArchiveDeckType(deckType)) {
            return;
        }
        if (category instanceof NetDeckStorageBase) {
            loadedNetArchiveCategories.put(deckType, (NetDeckStorageBase) category);
        } else {
            loadedNetArchiveCategories.remove(deckType);
        }
    }

    String getLoadedNetArchiveDeckTypeLabel(final DeckType deckType) {
        final IStorage<Deck> category = getLoadedNetArchiveCategory(deckType);
        return category == null || deckType == null ? deckType == null ? "" : deckType.toString()
                : deckType + " - " + category.getName();
    }

    boolean appendLoadedNetArchiveState(final StringBuilder state, final DeckType deckType) {
        final IStorage<Deck> category = getLoadedNetArchiveCategory(deckType);
        final NetArchiveSpec spec = getNetArchiveSpec(deckType);
        if (category == null || spec == null) {
            return false;
        }
        state.append(spec.prefix).append(category.getName());
        return true;
    }

    void addNetArchiveVirtualFolders(final List<DeckProxy> rows, final String path) {
        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            rows.add(DeckBrowserEntry.netFolder(spec.deckType.toString(),
                    DeckBrowserLocation.childPath(path, spec.deckType.name()), null, spec.deckType));
        }
    }

    void addMissingNetCategoryFolders(final List<DeckProxy> rows, final String path,
            final IStorage<Deck> folder, final GameType gameType, final boolean includeCachedStorage) {
        final Set<String> realFolderNames = new HashSet<>();
        if (folder != null) {
            for (final IStorage<Deck> subFolder : folder.getFolders()) {
                realFolderNames.add(subFolder.getName());
            }
        }

        final Iterable<NetDeckCategory> categories = NetDeckCategory.getAvailableCategories(gameType);
        if (categories == null) {
            return;
        }
        for (final NetDeckCategory category : categories) {
            if (realFolderNames.contains(category.getName())) {
                continue;
            }
            final IStorage<Deck> cached = includeCachedStorage
                    ? NetDeckCategory.selectAndLoad(gameType, category.getName()) : null;
            rows.add(DeckBrowserEntry.netFolder(category.getName(),
                    DeckBrowserLocation.childPath(path, category.getName()), cached, DeckType.NET_DECK));
        }
    }

    private IStorage<Deck> loadSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType,
            final String name, final boolean forceDownload) {
        if (deckType == null) {
            return null;
        }
        final NetArchiveSpec spec = getNetArchiveSpec(deckType);
        return spec == null ? null : spec.loader.selectAndLoad(gameType, name, forceDownload);
    }

    private NetArchiveSpec getNetArchiveSpec(final DeckType deckType) {
        if (deckType == null) {
            return null;
        }
        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            if (spec.deckType == deckType) {
                return spec;
            }
        }
        return null;
    }
}

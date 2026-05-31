package forge.deckchooser;

import com.google.common.collect.ImmutableList;
import forge.Singletons;
import forge.deck.*;
import forge.deck.io.DeckPreferences;
import forge.deck.io.DeckStorage;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEvent;
import forge.gamemodes.quest.QuestEventChallenge;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.gui.UiCommand;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerContainer;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorConstructed;
import forge.screens.deckeditor.controllers.DeckController;
import forge.gui.framework.FScreen;
import forge.item.InventoryItem;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.deck.DeckBase;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.IHasName;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {
    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType;
    private ItemManagerContainer lstDecksContainer;
    private NetDeckCategory netDeckCategory;
    private final Map<DeckType, NetDeckStorageBase> loadedNetArchiveCategories = new EnumMap<>(DeckType.class);

    private boolean refreshingDeckType;
    private boolean isForCommander;
    private final boolean editorOnlyBrowser;
    private IStorage<Deck> browserFolder;
    private IStorage<Deck> browserParentFolder;
    private String browserPath = "";
    private String browserGeneratedParentPath = "";
    private DeckType browserRootType;
    private boolean browserGeneratedFolder;
    private boolean browserHasDecksHomeParent;
    private IStorage<Deck> browserListParentFolder;
    private String browserListParentPath = "";
    private DeckType browserListParentRootType;
    private boolean browserListParentHasDecksHomeParent;
    private String pendingBrowserSelectionPath;
    private String pendingBrowserSelectionName;
    private DeckType pendingBrowserSelectionDeckType;
    private boolean browserSearchActive;
    private boolean browserHasDeckRows;
    private boolean browserHasCommanderDeckRows;
    private static final String GENERATED_HOME_PATH = "";
    private static final String GENERATED_RANDOM_PATH = "random";
    private static final String GENERATED_RANDOM_COLORS_PATH = "random/colors";
    private static final String GENERATED_RANDOM_ARCHETYPES_PATH = "random/archetypes";
    private interface NetArchiveLoader {
        NetDeckStorageBase selectAndLoad(GameType gameType, String name, boolean forceDownload);
    }

    private static final class NetArchiveSpec {
        private final DeckType deckType;
        private final String prefix;
        private final NetArchiveLoader loader;

        private NetArchiveSpec(final DeckType deckType0, final String prefix0, final NetArchiveLoader loader0) {
            deckType = deckType0;
            prefix = prefix0;
            loader = loader0;
        }
    }

    private static final NetArchiveSpec[] NET_ARCHIVE_SPECS = {
            new NetArchiveSpec(DeckType.NET_ARCHIVE_STANDARD_DECK, NetDeckArchiveStandard.PREFIX, NetDeckArchiveStandard::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_MODERN_DECK, NetDeckArchiveModern.PREFIX, NetDeckArchiveModern::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_PAUPER_DECK, NetDeckArchivePauper.PREFIX, NetDeckArchivePauper::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_PIONEER_DECK, NetDeckArchivePioneer.PREFIX, NetDeckArchivePioneer::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_LEGACY_DECK, NetDeckArchiveLegacy.PREFIX, NetDeckArchiveLegacy::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_VINTAGE_DECK, NetDeckArchiveVintage.PREFIX, NetDeckArchiveVintage::selectAndLoad),
            new NetArchiveSpec(DeckType.NET_ARCHIVE_BLOCK_DECK, NetDeckArchiveBlock.PREFIX, NetDeckArchiveBlock::selectAndLoad)
    };
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
    private static final Set<DeckType> RANDOM_ARCHETYPE_DECK_TYPE_SET = EnumSet.copyOf(Arrays.asList(RANDOM_ARCHETYPE_DECK_TYPES));
    private static final Set<DeckType> RANDOM_COLOR_DECK_TYPE_SET = EnumSet.copyOf(Arrays.asList(RANDOM_COLOR_DECK_TYPES));
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

    private final DeckManager lstDecks;
    final Localizer localizer = Localizer.getInstance();

    private final FLabel btnViewDeck = new FLabel.ButtonBuilder().text(localizer.getMessage("lblViewDeck")).fontSize(14).build();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().fontSize(14).build();
    private final FLabel btnRefresh = new FLabel.ButtonBuilder()
            .icon(FSkin.getIcon(FSkinProp.ICO_OPEN).resize(20, 20))
            .iconScaleAuto(false)
            .reactOnMouseDown()
            .tooltip("Refresh")
            .build();

    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private FPref stateSetting = null;

    //Show dialog to select a deck
    public static Deck promptForDeck(final CDetailPicture cDetailPicture, final String title, final DeckType defaultDeckType, final boolean forAi) {
        FThreads.assertExecutedByEdt(true);
        boolean isForCommander = defaultDeckType.equals(DeckType.COMMANDER_DECK);
        final FDeckChooser chooser = new FDeckChooser(cDetailPicture, forAi, isForCommander? GameType.Commander : GameType.Constructed, isForCommander);
        chooser.initialize(defaultDeckType);
        chooser.populate();
        final Dimension parentSize = JOptionPane.getRootFrame().getSize();
        chooser.setMinimumSize(new Dimension((int)(parentSize.getWidth() / 2), (int)parentSize.getHeight() - 200));
        final Localizer localizer = Localizer.getInstance();
        final FOptionPane optionPane = new FOptionPane(null, title, null, chooser, ImmutableList.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")), 0);
        optionPane.setDefaultFocus(chooser);
        chooser.lstDecks.setItemActivateCommand((UiCommand) () -> {
            //accept selected deck on double click or Enter
            if (chooser.hasPlayableSelection()) {
                optionPane.setResult(0);
            }
        });
        optionPane.setVisible(true);
        final int dialogResult = optionPane.getResult();
        optionPane.dispose();
        if (dialogResult == 0) {
            return chooser.getDeck();
        }
        return null;
    }

    public FDeckChooser(final CDetailPicture cDetailPicture, final boolean forAi, GameType gameType, boolean forCommander) {
        this(cDetailPicture, forAi, gameType, forCommander, false);
    }

    public FDeckChooser(final CDetailPicture cDetailPicture, final boolean forAi, GameType gameType, boolean forCommander, boolean editorOnly) {
        lstDecks = new DeckManager(gameType, cDetailPicture);
        setOpaque(false);
        isAi = forAi;
        isForCommander = forCommander;
        editorOnlyBrowser = editorOnly;
        final UiCommand cmdViewDeck = () -> {
            DeckProxy selected = lstDecks.getSelectedItem();
            if (selected instanceof DeckBrowserEntry && !((DeckBrowserEntry) selected).isDeck()) {
                return;
            }
            if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                showDeckViewer();
            }
        };
        lstDecks.setItemActivateCommand(this::activateBrowserSelection);
        btnViewDeck.setCommand(cmdViewDeck);
        btnRefresh.setCommand(this::refreshBrowserFromButton);
        lstDecks.setSearchChangeListener(this::setBrowserSearchText);
        if (editorOnlyBrowser) {
            lstDecks.setDeleteCommand(this::refreshCurrentEditorBrowserLocation);
            lstDecks.setEditCommand(this::loadEditorDeck);
        }
        lstDecks.addViewButton(btnRefresh);
    }

    public void initialize() {
        initialize(DeckType.COLOR_DECK);
    }
    public void initialize(final DeckType defaultDeckType) {
        initialize(null, defaultDeckType);
    }
    public void initialize(final FPref savedStateSetting, final DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(final DeckType selectedDeckType0) {
        refreshDecksList(selectedDeckType0, false, null);
    }

    public DeckManager getLstDecks() { return lstDecks; }

    public void refreshEditorBrowser() {
        if (editorOnlyBrowser) {
            refreshCurrentEditorBrowserLocation();
        }
    }

    private void refreshCurrentEditorBrowserLocation() {
        if (browserGeneratedFolder) {
            updateDecksHome();
        } else if (browserFolder != null) {
            reloadBrowserFolderFromDisk();
            updateBrowserFolder();
        } else {
            updateDecksHome();
        }
    }

    private void refreshBrowserFromButton() {
        if (isInNetDeckFolder() && StringUtils.isNotBlank(getNetFolderName())) {
            refreshNetFolderFromSource(getNetFolderName());
        } else if (isInNetArchiveFolder() && StringUtils.isNotBlank(getNetArchiveFolderName())) {
            refreshNetArchiveFolderFromSource(getNetArchiveFolderName());
        } else if (editorOnlyBrowser) {
            refreshCurrentEditorBrowserLocation();
        } else if (browserFolder == null && StringUtils.isNotBlank(browserGeneratedParentPath)) {
            updateGeneratedGroup(browserGeneratedParentPath);
        } else if (browserGeneratedFolder || hasBrowserListParent()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (browserFolder != null) {
            reloadBrowserFolderFromDisk();
            updateBrowserFolder();
        } else {
            updateDecksHome();
        }
    }

    private String firstPathSegment(final String path) {
        final int idx = StringUtils.defaultString(path).indexOf('/');
        return idx < 0 ? StringUtils.defaultString(path) : path.substring(0, idx);
    }

    private boolean isInNetDeckFolder() {
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browserPath)) {
            return true;
        }
        return isFolderUnder(browserFolder, ForgeConstants.DECK_NET_DIR)
                && !isFolderPath(browserFolder, ForgeConstants.DECK_NET_DIR);
    }

    private boolean isInNetArchiveFolder() {
        if (isNetArchiveBrowser() && StringUtils.isNotBlank(browserPath)) {
            return true;
        }
        return isFolderUnder(browserFolder, ForgeConstants.DECK_NET_ARCHIVE_DIR)
                && !isFolderPath(browserFolder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private String getNetFolderName() {
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browserPath)) {
            return firstPathSegment(browserPath);
        }
        return firstPathSegment(relativeFolderPath(browserFolder, ForgeConstants.DECK_NET_DIR));
    }

    private String getNetArchiveFolderName() {
        if (StringUtils.startsWith(browserPath, "archive/")) {
            return firstPathSegment(StringUtils.removeStart(browserPath, "archive/"));
        }
        return firstPathSegment(relativeFolderPath(browserFolder, ForgeConstants.DECK_NET_ARCHIVE_DIR));
    }

    private boolean isFolderUnder(final IStorage<Deck> folder, final String rootPath) {
        final String relativePath = relativeFolderPath(folder, rootPath);
        return StringUtils.isNotBlank(relativePath) || isFolderPath(folder, rootPath);
    }

    private boolean isFolderPath(final IStorage<Deck> folder, final String rootPath) {
        if (folder == null) {
            return false;
        }
        return new File(folder.getFullPath()).getAbsoluteFile().equals(new File(rootPath).getAbsoluteFile());
    }

    private String relativeFolderPath(final IStorage<Deck> folder, final String rootPath) {
        if (folder == null) {
            return "";
        }
        final File root = new File(rootPath).getAbsoluteFile();
        final File current = new File(folder.getFullPath()).getAbsoluteFile();
        final String rootAbsolute = root.getPath();
        final String currentAbsolute = current.getPath();
        if (!currentAbsolute.startsWith(rootAbsolute)) {
            return "";
        }
        String relative = currentAbsolute.substring(rootAbsolute.length());
        while (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }
        return relative.replace(File.separatorChar, '/');
    }

    private void reloadBrowserFolderFromDisk() {
        final IStorage<Deck> rootFolder;
        if (StringUtils.startsWith(browserPath, "archive/")) {
            rootFolder = getArchiveStorage();
        } else if (browserRootType == null) {
            rootFolder = getDecksHomeStorage();
        } else {
            rootFolder = getFreshStorageForDeckType(browserRootType);
        }
        if (rootFolder == null) {
            return;
        }

        final String storagePath = StringUtils.startsWith(browserPath, "archive/")
                ? StringUtils.removeStart(browserPath, "archive/") : browserPath;
        final IStorage<Deck> refreshedFolder = StringUtils.isBlank(storagePath)
                ? rootFolder : rootFolder.tryGetFolder(storagePath);
        if (refreshedFolder == null) {
            return;
        }

        browserFolder = refreshedFolder;
        browserParentFolder = StringUtils.isBlank(storagePath) ? null : rootFolder.tryGetFolder(parentPath(storagePath));
    }

    private void activateBrowserSelection() {
        final DeckProxy selected = lstDecks.getSelectedItem();
        if (selected instanceof DeckBrowserEntry entry) {
            switch (entry.getKind()) {
            case FOLDER:
                clearBrowserListParent();
                if (entry.getDeckType() != null) {
                    browserRootType = entry.getDeckType();
                    final IStorage<Deck> shortcutRoot = getStorageForDeckType(browserRootType);
                    browserPath = isSameFolder(entry.getFolder(), shortcutRoot)
                            ? "" : getPathRelativeToShortcutRoot(entry.getPath(), browserRootType);
                    browserHasDecksHomeParent = true;
                    setShortcutDeckType(entry.getDeckType());
                } else {
                    browserParentFolder = browserFolder;
                    browserPath = entry.getPath();
                    browserHasDecksHomeParent = false;
                }
                browserFolder = entry.getFolder();
                browserGeneratedFolder = false;
                if (browserRootType != null) {
                    setShortcutDeckType(browserRootType);
                }
                final IStorage<Deck> folderRoot = browserRootType == null ? getDecksHomeStorage() : getStorageForDeckType(browserRootType);
                browserParentFolder = StringUtils.isBlank(browserPath) || folderRoot == null ? null : folderRoot.tryGetFolder(parentPath(browserPath));
                updateBrowserFolder();
                return;
            case PARENT_FOLDER:
                rememberCurrentBrowserLocationForParentSelection();
                if (!hasBrowserListParent() && browserHasDecksHomeParent
                        && StringUtils.isBlank(browserPath) && StringUtils.isBlank(entry.getPath())) {
                    updateDecksHome();
                    return;
                }
                if (entry.getFolder() == null) {
                    if (StringUtils.isBlank(entry.getPath())) {
                        updateDecksHome();
                    } else {
                        updateGeneratedGroup(entry.getPath());
                    }
                    return;
                }
                browserFolder = entry.getFolder();
                browserPath = entry.getPath();
                final DeckType parentShortcutType = getShortcutDeckTypeForFolder(browserFolder);
                if (parentShortcutType != null) {
                    browserRootType = parentShortcutType;
                    browserPath = getPathRelativeToShortcutRoot(browserPath, browserRootType);
                    browserHasDecksHomeParent = true;
                } else {
                    browserRootType = browserListParentRootType == null ? browserRootType : browserListParentRootType;
                    if (browserRootType == null && StringUtils.isBlank(browserPath)) {
                        browserRootType = getShortcutDeckTypeForFolder(browserFolder);
                    }
                    browserHasDecksHomeParent = browserListParentHasDecksHomeParent;
                }
                clearBrowserListParent();
                final IStorage<Deck> rootFolder = browserRootType == null ? getDecksHomeStorage() : getStorageForDeckType(browserRootType);
                browserParentFolder = StringUtils.isBlank(browserPath) || rootFolder == null ? null : rootFolder.tryGetFolder(parentPath(browserPath));
                browserGeneratedFolder = false;
                if (browserRootType != null) {
                    setShortcutDeckType(browserRootType);
                }
                updateBrowserFolder();
                return;
            case NET_FOLDER:
                if (isNetArchiveDeckType(entry.getDeckType())) {
                    openNetArchiveVirtualFolder(entry.getDeckType());
                } else {
                    openNetFolder(entry);
                }
                return;
            case GENERATED_GROUP:
                updateGeneratedGroup(entry.getPath());
                return;
            case GENERATED_FOLDER:
                if (isCommanderGeneratedDeckType(entry.getDeckType())) {
                    rememberCurrentBrowserLocationAsListParent();
                    browserGeneratedFolder = false;
                } else if (isGeneratedDeckType(entry.getDeckType())) {
                    browserGeneratedFolder = true;
                    browserGeneratedParentPath = entry.getPath();
                } else {
                    rememberCurrentBrowserLocationAsListParent();
                    browserGeneratedFolder = false;
                }
                setShortcutDeckType(entry.getDeckType());
                refreshDecksList(entry.getDeckType(), true, new DecksComboBoxEvent(decksComboBox, entry.getDeckType()));
                return;
            case GENERATED_OPTION:
            case DECK:
            default:
                if (editorOnlyBrowser) {
                    loadEditorDeck(selected);
                    return;
                }
                showDeckViewer();
                return;
            }
        }
        if (editorOnlyBrowser) {
            loadEditorDeck(selected);
            return;
        }
        showDeckViewer();
    }

    private void showDeckViewer() {
        FDeckViewer.show(getDeck(), true);
    }

    private DeckProxy getDeckProxy(final DeckProxy selected) {
        if (selected instanceof DeckBrowserEntry entry) {
            return entry.getDeckRowProxy();
        }
        return selected;
    }

    public DeckProxy getSelectedDeckProxy() {
        return getDeckProxy(lstDecks.getSelectedItem());
    }

    public boolean hasPlayableSelection() {
        return getSelectedDeckProxy() != null;
    }

    public List<DeckProxy> getSelectedDeckProxies() {
        final List<DeckProxy> decks = new ArrayList<>();
        for (final DeckProxy selected : lstDecks.getSelectedItems()) {
            final DeckProxy deck = getDeckProxy(selected);
            if (deck != null) {
                decks.add(deck);
            }
        }
        return decks;
    }

    public boolean selectFirstPlayableDeck() {
        for (final Entry<DeckProxy, Integer> entry : lstDecks.getFilteredItems()) {
            final DeckProxy deck = getDeckProxy(entry.getKey());
            if (deck != null) {
                return lstDecks.setSelectedItem(entry.getKey());
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void loadEditorDeck(final DeckProxy selected) {
        final DeckProxy deck = getDeckProxy(selected);
        if (deck == null || browserFolder == null) {
            return;
        }

        final FScreen screen = FScreen.DECK_EDITOR_CONSTRUCTED;
        if (!Singletons.getControl().ensureScreenActive(screen) || !SEditorIO.confirmSaveChanges(screen, true)) {
            return;
        }

        final GameType gameType = getGameTypeForDeckType(browserRootType);
        ACEditorBase<? extends InventoryItem, ? extends DeckBase> editor =
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (editor == null || editor.getGameType() != gameType) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorConstructed(lstDecks.getCDetailPicture(), gameType));
            editor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        }
        if (editor == null || editor.getDeckController() == null) {
            return;
        }

        IStorage<Deck> currentFolder = browserFolder;
        final IStorage<? extends IHasName> deckStorage = deck.getStorage();
        if (deckStorage != null) {
            currentFolder = (IStorage<Deck>) deckStorage;
        }

        final DeckController controller = editor.getDeckController();
        controller.setCurrentFolder(currentFolder, deck.getPath());
        controller.loadFromCurrentFolder(deck.getName());
        setEditorDeckPreference(gameType, deck);
    }

    private void setEditorDeckPreference(final GameType gameType, final DeckProxy deck) {
        switch (gameType) {
        case Commander:
        case Oathbreaker:
            DeckPreferences.setCommanderDeck(deck.toString());
            break;
        case Brawl:
            DeckPreferences.setBrawlDeck(deck.toString());
            break;
        case TinyLeaders:
            DeckPreferences.setTinyLeadersDeck(deck.toString());
            break;
        case Constructed:
        default:
            DeckPreferences.setCurrentDeck(deck.toString());
            break;
        }
    }

    private void openNetFolder(final DeckBrowserEntry entry) {
        final DeckType rootType = browserRootType == null ? entry.getDeckType() : browserRootType;
        final String name = entry.getName();
        refreshNetFolder(rootType, name);
    }

    private void refreshNetFolderFromSource(final String name) {
        final DeckType rootType = isNetBrowserRoot() ? browserRootType
                : isForCommander ? DeckType.NET_COMMANDER_DECK : DeckType.NET_DECK;
        refreshNetFolder(rootType, name);
    }

    private void refreshNetFolder(final DeckType rootType, final String name) {
        FThreads.invokeInBackgroundThread(() -> {
            DeckType resolvedRootType = rootType;
            NetDeckCategory category = reloadNetCategory(resolvedRootType, name);
            if (category == null && NetDeckCategory.selectAndLoad(getGameTypeForDeckType(resolvedRootType), name) == null) {
                resolvedRootType = rootType == DeckType.NET_COMMANDER_DECK ? DeckType.NET_DECK : DeckType.NET_COMMANDER_DECK;
                category = reloadNetCategory(resolvedRootType, name);
            }
            final DeckType finalRootType = resolvedRootType;
            final NetDeckCategory finalCategory = category;
            FThreads.invokeInEdtLater(() -> {
                if (finalCategory == null) {
                    return;
                }
                final IStorage<Deck> netRoot = getStorageForDeckType(finalRootType);
                final IStorage<Deck> downloadedFolder = netRoot == null ? null : netRoot.tryGetFolder(name);
                browserRootType = finalRootType;
                browserParentFolder = netRoot;
                browserFolder = downloadedFolder == null ? finalCategory : downloadedFolder;
                browserPath = childPath("", name);
                browserGeneratedFolder = false;
                clearBrowserListParent();
                updateBrowserFolder();
            });
        });
    }

    private NetDeckCategory reloadNetCategory(final DeckType rootType, final String name) {
        final GameType gameType = getGameTypeForDeckType(rootType);
        if (NetDeckCategory.selectAndLoad(gameType, name) == null) {
            return null;
        }
        return NetDeckCategory.selectAndLoad(gameType, name, true);
    }

    private void openNetArchiveFolder(final IStorage<Deck> category) {
        final IStorage<Deck> archiveRoot = getArchiveStorage();
        final IStorage<Deck> downloadedFolder = archiveRoot.tryGetFolder(category.getName());
        browserRootType = null;
        browserParentFolder = archiveRoot;
        browserFolder = downloadedFolder == null ? category : downloadedFolder;
        browserPath = childPath("archive", category.getName());
        browserGeneratedFolder = false;
        browserHasDecksHomeParent = false;
        clearBrowserListParent();
        updateBrowserFolder();
    }

    private void refreshNetArchiveFolderFromSource(final String name) {
        final DeckType deckType = selectedDeckType;
        final GameType gameType = lstDecks.getGameType();
        FThreads.invokeInBackgroundThread(() -> {
            final IStorage<Deck> category = reloadNetArchiveCategory(gameType, deckType, name);
            FThreads.invokeInEdtLater(() -> {
                if (category != null) {
                    openNetArchiveFolder(category);
                }
            });
        });
    }

    private void openNetArchiveVirtualFolder(final DeckType deckType) {
        final GameType gameType = lstDecks.getGameType();
        FThreads.invokeInBackgroundThread(() -> {
            final IStorage<Deck> category = reloadSelectedNetArchiveCategory(gameType, deckType, null);
            FThreads.invokeInEdtLater(() -> {
                if (category != null) {
                    selectedDeckType = deckType;
                    setShortcutDeckType(deckType);
                    openNetArchiveFolder(category);
                }
            });
        });
    }

    private IStorage<Deck> reloadNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        if (findSelectedNetArchiveCategory(gameType, deckType, name) != null) {
            return reloadSelectedNetArchiveCategory(gameType, deckType, name);
        }

        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            if (findSelectedNetArchiveCategory(gameType, spec.deckType, name) != null) {
                final IStorage<Deck> category = reloadSelectedNetArchiveCategory(gameType, spec.deckType, name);
                if (category != null) {
                    selectedDeckType = spec.deckType;
                }
                return category;
            }
        }
        return null;
    }

    private IStorage<Deck> findSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        return loadSelectedNetArchiveCategory(gameType, deckType, name, false);
    }

    private IStorage<Deck> reloadSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType, final String name) {
        return loadSelectedNetArchiveCategory(gameType, deckType, name, true);
    }

    private IStorage<Deck> loadSelectedNetArchiveCategory(final GameType gameType, final DeckType deckType,
            final String name, final boolean forceDownload) {
        if (deckType == null) {
            return null;
        }
        final NetArchiveSpec spec = getNetArchiveSpec(deckType);
        return spec == null ? null : spec.loader.selectAndLoad(gameType, name, forceDownload);
    }

    private void updateDecks(final Iterable<DeckProxy> decks) {
        updateDecks(decks, null);
    }

    private void updateDecks(final Iterable<DeckProxy> decks, final ItemManagerConfig config) {
        updateBrowserOptions(decks, false, localizer.getMessage("lblRandomDeck"),
                this::randomSelectBrowserDeck, new Integer[]{0}, config);
    }

    private void updateBrowserOptions(final Iterable<DeckProxy> decks, final boolean allowMultipleSelections,
            final String randomText, final UiCommand randomCommand, final Integer[] defaultSelection) {
        updateBrowserOptions(decks, allowMultipleSelections, randomText, randomCommand, defaultSelection, null);
    }

    private void updateBrowserOptions(final Iterable<DeckProxy> decks, final boolean allowMultipleSelections,
            final String randomText, final UiCommand randomCommand, final Integer[] defaultSelection,
            final ItemManagerConfig config) {
        lstDecks.setAllowMultipleSelections(allowMultipleSelections);

        final List<DeckProxy> rows = wrapGeneratedOptions(decks);
        int leadingRows = 0;
        if (browserGeneratedFolder) {
            rows.add(0, DeckBrowserEntry.parentFolder(browserGeneratedParentPath, null));
            leadingRows = 1;
        } else if (hasBrowserListParent()) {
            rows.add(0, DeckBrowserEntry.parentFolder(browserListParentPath, browserListParentFolder));
            leadingRows = 1;
        } else if (isHomeShortcutList(selectedDeckType)) {
            rows.add(0, DeckBrowserEntry.parentFolder("", null));
            leadingRows = 1;
        }
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows, config);

        btnRandom.setText(randomText);
        btnRandom.setCommand(randomCommand);

        if (displayedRows.isEmpty()) {
            return;
        }
        if (selectPendingBrowserRow(displayedRows)) {
            return;
        }
        if (allowMultipleSelections) {
            final Integer[] shiftedSelection = new Integer[defaultSelection.length];
            for (int i = 0; i < defaultSelection.length; i++) {
                shiftedSelection[i] = defaultSelection[i] + leadingRows;
            }
            lstDecks.setSelectedIndices(shiftedSelection);
        } else {
            lstDecks.setSelectedIndex(browserSearchActive ? 0
                    : leadingRows > 0 ? Math.min(leadingRows, displayedRows.size() - 1) : defaultSelection[0]);
        }
    }

    private List<Integer> getBrowserDeckRowIndices() {
        final List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < lstDecks.getItemCount(); i++) {
            final DeckProxy deck = lstDecks.getCurrentView().getItemAtIndex(i);
            if (!(deck instanceof DeckBrowserEntry) || ((DeckBrowserEntry) deck).isDeck()) {
                indices.add(i);
            }
        }
        return indices;
    }

    private void randomSelectBrowserDeck() {
        final List<Integer> indices = getBrowserDeckRowIndices();
        if (!indices.isEmpty()) {
            lstDecks.setSelectedIndex(indices.get(MyRandom.getRandom().nextInt(indices.size())));
        }
    }

    private void randomSelectBrowserColors() {
        final List<Integer> indices = getBrowserDeckRowIndices();
        if (indices.isEmpty()) {
            return;
        }
        final int colorCount = Math.min(MyRandom.getRandom().nextInt(3) + 1, indices.size());
        final List<Integer> selectedIndices = new ArrayList<>();
        while (selectedIndices.size() < colorCount) {
            final Integer index = indices.get(MyRandom.getRandom().nextInt(indices.size()));
            if (!selectedIndices.contains(index)) {
                selectedIndices.add(index);
            }
        }
        lstDecks.setSelectedIndices(selectedIndices);
    }

    private void updateCustom() {
        updateBrowserRoot(selectedDeckType);
    }

    private IStorage<Deck> getStorageForDeckType(final DeckType deckType) {
        if (deckType == null) {
            return FModel.getDecks().getConstructed();
        }
        switch (deckType) {
        case NET_DECK:
        case NET_COMMANDER_DECK:
            return new StorageImmediatelySerialized<>("Net decks",
                    new DeckStorage(new File(ForgeConstants.DECK_NET_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        case OATHBREAKER_DECK:
            return FModel.getDecks().getOathbreaker();
        case BRAWL_DECK:
            return FModel.getDecks().getBrawl();
        case TINY_LEADERS_DECK:
            return FModel.getDecks().getTinyLeaders();
        case COMMANDER_DECK:
            return FModel.getDecks().getCommander();
        default:
            return FModel.getDecks().getConstructed();
        }
    }

    private IStorage<Deck> getFreshStorageForDeckType(final DeckType deckType) {
        if (deckType == null) {
            return getDecksHomeStorage();
        }
        switch (deckType) {
        case NET_DECK:
        case NET_COMMANDER_DECK:
            return new StorageImmediatelySerialized<>("Net decks",
                    new DeckStorage(new File(ForgeConstants.DECK_NET_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        case OATHBREAKER_DECK:
            return new StorageImmediatelySerialized<>("Oathbreaker decks",
                    new DeckStorage(new File(ForgeConstants.DECK_OATHBREAKER_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        case BRAWL_DECK:
            return new StorageImmediatelySerialized<>("Brawl decks",
                    new DeckStorage(new File(ForgeConstants.DECK_BRAWL_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        case TINY_LEADERS_DECK:
            return new StorageImmediatelySerialized<>("Tiny Leaders decks",
                    new DeckStorage(new File(ForgeConstants.DECK_TINY_LEADERS_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        case COMMANDER_DECK:
            return new StorageImmediatelySerialized<>("Commander decks",
                    new DeckStorage(new File(ForgeConstants.DECK_COMMANDER_DIR), ForgeConstants.DECK_BASE_DIR),
                    true);
        default:
            return new StorageImmediatelySerialized<>("Constructed decks",
                    new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR, true),
                    true);
        }
    }

    private IStorage<Deck> getDecksHomeStorage() {
        return new StorageImmediatelySerialized<>("Decks",
                new DeckStorage(new File(ForgeConstants.DECK_BASE_DIR), ForgeConstants.DECK_BASE_DIR),
                true);
    }

    private IStorage<Deck> getArchiveStorage() {
        return new StorageImmediatelySerialized<>("Archive",
                new DeckStorage(new File(ForgeConstants.DECK_NET_ARCHIVE_DIR), ForgeConstants.DECK_BASE_DIR),
                true);
    }

    private GameType getGameTypeForDeckType(final DeckType deckType) {
        if (deckType == null) {
            return GameType.Constructed;
        }
        if (deckType == DeckType.CUSTOM_DECK) {
            DeckFormat deckFormat = lstDecks.getGameType().getDeckFormat();
            switch (deckFormat) {
            case Commander:
                return GameType.Commander;
            case Oathbreaker:
                return GameType.Oathbreaker;
            case Brawl:
                return GameType.Brawl;
            case TinyLeaders:
                return GameType.TinyLeaders;
            default:
                return GameType.Constructed;
            }
        }
        switch (deckType) {
        case OATHBREAKER_DECK:
            return GameType.Oathbreaker;
        case BRAWL_DECK:
            return GameType.Brawl;
        case TINY_LEADERS_DECK:
            return GameType.TinyLeaders;
        case NET_COMMANDER_DECK:
        case COMMANDER_DECK:
            return GameType.Commander;
        default:
            return GameType.Constructed;
        }
    }

    private String childPath(final String base, final String name) {
        return StringUtils.isBlank(base) ? name : base + "/" + name;
    }

    private String getPathRelativeToShortcutRoot(final String path, final DeckType rootType) {
        final IStorage<Deck> rootFolder = getStorageForDeckType(rootType);
        if (rootFolder == null || StringUtils.isBlank(path)) {
            return "";
        }

        final String rootName = rootFolder.getName();
        if (path.equals(rootName)) {
            return "";
        }
        return StringUtils.removeStart(path, rootName + "/");
    }

    private List<DeckProxy> wrapGeneratedOptions(final Iterable<DeckProxy> decks) {
        final List<DeckProxy> entries = new ArrayList<>();
        for (final DeckProxy deck : decks) {
            if (deck instanceof DeckBrowserEntry) {
                entries.add(deck);
            } else if (deck.isGeneratedDeck()) {
                entries.add(DeckBrowserEntry.generatedOption(deck));
            } else {
                entries.add(DeckBrowserEntry.deck(deck));
            }
        }
        sortBrowserRows(entries);
        return entries;
    }

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows) {
        return setBrowserPoolAndSetup(rows, null);
    }

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows, final ItemManagerConfig config) {
        final List<DeckProxy> displayedRows = browserSearchActive ? buildRecursiveSearchRows() : rows;
        browserHasDeckRows = containsDeckRows(displayedRows);
        browserHasCommanderDeckRows = containsCommanderDeckRows(displayedRows);
        lstDecks.setPool(displayedRows);
        lstDecks.setup(config == null ? getBrowserItemManagerConfig() : config);
        return displayedRows;
    }

    private boolean containsDeckRows(final Iterable<DeckProxy> rows) {
        for (final DeckProxy row : rows) {
            if (row instanceof DeckBrowserEntry) {
                if (((DeckBrowserEntry) row).isDeck()) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean containsCommanderDeckRows(final Iterable<DeckProxy> rows) {
        for (final DeckProxy row : rows) {
            final DeckProxy deck = getDeckProxy(row);
            if (isCommanderDeck(deck)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCommanderDeck(final DeckProxy deck) {
        return deck != null && deck.hasCommanderSection();
    }

    private void setBrowserSearchText(final String searchText) {
        final boolean active = StringUtils.isNotBlank(searchText);
        if (browserSearchActive == active) {
            return;
        }

        browserSearchActive = active;
        refreshCurrentBrowserRows();
    }

    private void refreshCurrentBrowserRows() {
        if (isGeneratedOrListBrowserView()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (StringUtils.isNotBlank(browserGeneratedParentPath) && browserFolder == null) {
            updateGeneratedGroup(browserGeneratedParentPath);
        } else if (isSearchGeneratedListType()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (browserFolder != null) {
            updateBrowserFolder();
        } else {
            updateDecksHome();
        }
    }

    private List<DeckProxy> buildRecursiveSearchRows() {
        final List<DeckProxy> rows = new ArrayList<>();
        if (editorOnlyBrowser) {
            if (browserFolder == null) {
                addDecksHomeRowsRecursively(rows, false);
            } else {
                addFolderRowsRecursively(rows, browserFolder, browserPath, browserRootType);
            }
        } else if (isGeneratedOrListBrowserView()) {
            addGeneratedRowsRecursively(rows, selectedDeckType);
        } else if (StringUtils.isNotBlank(browserGeneratedParentPath) && browserFolder == null) {
            addGeneratedGroupRowsRecursively(rows, browserGeneratedParentPath);
        } else if (isSearchGeneratedListType()) {
            addGeneratedRowsRecursively(rows, selectedDeckType);
        } else if (browserFolder != null) {
            addFolderRowsRecursively(rows, browserFolder, browserPath, browserRootType);
            addVirtualRowsForFolderRecursively(rows, browserPath, browserRootType, browserFolder);
        } else {
            addDecksHomeRowsRecursively(rows, true);
        }
        sortBrowserRows(rows);
        return rows;
    }

    private void addDecksHomeRowsRecursively(final List<DeckProxy> rows, final boolean includeVirtualRows) {
        final IStorage<Deck> decksHome = getDecksHomeStorage();
        for (final IStorage<Deck> folder : decksHome.getFolders()) {
            final DeckType shortcutDeckType = getShortcutDeckTypeForFolder(folder);
            final String path = folder.getName();
            rows.add(DeckBrowserEntry.folder(folder.getName(), path, folder, shortcutDeckType));
            addFolderRowsRecursively(rows, folder, path, shortcutDeckType);
            if (includeVirtualRows) {
                addVirtualRowsForFolderRecursively(rows, path, shortcutDeckType, folder);
            }
        }
    }

    private boolean isSearchGeneratedListType() {
        return selectedDeckType != null && isGeneratedDeckType(selectedDeckType);
    }

    private void addFolderRowsRecursively(final List<DeckProxy> rows, final IStorage<Deck> folder,
            final String path, final DeckType rootType) {
        if (folder == null) {
            return;
        }
        for (final IStorage<Deck> subFolder : folder.getFolders()) {
            final String subPath = childPath(path, subFolder.getName());
            rows.add(DeckBrowserEntry.folder(subFolder.getName(), subPath, subFolder, getFolderDeckTypeForBrowserRow(subFolder)));
            addFolderRowsRecursively(rows, subFolder, subPath, rootType);
            addVirtualRowsForFolderRecursively(rows, subPath, rootType, subFolder);
        }
        final GameType gameType = getGameTypeForDeckType(rootType);
        for (final Deck deck : folder) {
            rows.add(DeckBrowserEntry.deck(new DeckProxy(deck, gameType.toString(), gameType, path, folder, null)));
        }
    }

    private void addVirtualRowsForFolderRecursively(final List<DeckProxy> rows, final String path,
            final DeckType rootType, final IStorage<Deck> folder) {
        if (editorOnlyBrowser) {
            return;
        }

        final DeckType folderShortcutType = folder == null ? null : getShortcutDeckTypeForFolder(folder);
        final boolean isShortcutRoot = rootType != null && rootType == folderShortcutType;
        final boolean isArchiveRoot = isFolderPath(folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
        if (StringUtils.isNotBlank(path) && !isShortcutRoot && !isArchiveRoot) {
            return;
        }
        if (rootType == DeckType.CUSTOM_DECK) {
            rows.add(DeckBrowserEntry.generatedGroup(DeckType.RANDOM_DECK.toString(), GENERATED_RANDOM_PATH));
            addGeneratedGroupRowsRecursively(rows, GENERATED_RANDOM_PATH);
            addGeneratedFolderRows(rows, path, true, DeckType.PRECONSTRUCTED_DECK, DeckType.QUEST_OPPONENT_DECK);
        } else if (rootType == DeckType.COMMANDER_DECK) {
            addGeneratedFolderRows(rows, path, true, DeckType.RANDOM_COMMANDER_DECK);
            if (FModel.isdeckGenMatrixLoaded()) {
                addGeneratedFolderRows(rows, path, true, DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
            }
            addGeneratedFolderRows(rows, path, true, DeckType.PRECON_COMMANDER_DECK);
        } else if (rootType == DeckType.NET_DECK || rootType == DeckType.NET_COMMANDER_DECK) {
            final Set<String> realFolderNames = new HashSet<>();
            if (folder != null) {
                for (final IStorage<Deck> subFolder : folder.getFolders()) {
                    realFolderNames.add(subFolder.getName());
                }
            }
            final Map<String, NetDeckCategory> categories = NetDeckCategory.getCategories(lstDecks.getGameType());
            if (categories != null) {
                for (final NetDeckCategory category : categories.values()) {
                    if (!realFolderNames.contains(category.getName())) {
                        rows.add(DeckBrowserEntry.netFolder(category.getName(), childPath(path, category.getName()), null, DeckType.NET_DECK));
                    }
                }
            }
        } else if (isArchiveRoot) {
            addNetArchiveVirtualFolders(rows, path);
        }
    }

    private void addNetArchiveVirtualFolders(final List<DeckProxy> rows, final String path) {
        for (final NetArchiveSpec spec : NET_ARCHIVE_SPECS) {
            rows.add(DeckBrowserEntry.netFolder(spec.deckType.toString(), childPath(path, spec.deckType.name()), null, spec.deckType));
        }
    }

    private void addGeneratedGroupRowsRecursively(final List<DeckProxy> rows, final String path) {
        if (GENERATED_RANDOM_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(RandomDeckGenerator.getRandomDecks(lstDecks, isAi)));
            rows.add(DeckBrowserEntry.generatedGroup("Random Archetype Decks", GENERATED_RANDOM_ARCHETYPES_PATH));
            addGeneratedGroupRowsRecursively(rows, GENERATED_RANDOM_ARCHETYPES_PATH);
            rows.add(DeckBrowserEntry.generatedGroup(DeckType.COLOR_DECK.toString(), GENERATED_RANDOM_COLORS_PATH));
            addGeneratedGroupRowsRecursively(rows, GENERATED_RANDOM_COLORS_PATH);
            addGeneratedFolderRows(rows, path, true, DeckType.THEME_DECK);
        } else if (GENERATED_RANDOM_COLORS_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(ColorDeckGenerator.getColorDecks(lstDecks, null, isAi)));
            addGeneratedFolderRows(rows, path, true, RANDOM_COLOR_DECK_TYPES);
        } else if (GENERATED_RANDOM_ARCHETYPES_PATH.equals(path) && FModel.isdeckGenMatrixLoaded()) {
            addGeneratedFolderRows(rows, path, true, RANDOM_ARCHETYPE_DECK_TYPES);
        }
    }

    private void addGeneratedFolderRows(final List<DeckProxy> rows, final String path,
            final boolean includeGeneratedOptions, final DeckType... deckTypes) {
        for (final DeckType deckType : deckTypes) {
            rows.add(DeckBrowserEntry.generatedFolder(deckType.toString(), path, deckType));
            if (includeGeneratedOptions) {
                addGeneratedRowsRecursively(rows, deckType);
            }
        }
    }

    private void addGeneratedRowsRecursively(final List<DeckProxy> rows, final DeckType deckType) {
        if (deckType == null) {
            return;
        }
        rows.addAll(wrapGeneratedOptions(getGeneratedDecksForSearch(deckType)));
    }

    private Iterable<DeckProxy> getGeneratedDecksForSearch(final DeckType deckType) {
        switch (deckType) {
        case COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(lstDecks, null, isAi);
        case STANDARD_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(lstDecks, FModel.getFormats().getStandard().getFilterPrinted(), isAi);
        case MODERN_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(lstDecks, FModel.getFormats().getModern().getFilterPrinted(), isAi);
        case PAUPER_COLOR_DECK:
            return ColorDeckGenerator.getColorDecks(lstDecks, FModel.getFormats().getPauper().getFilterPrinted(), isAi);
        case STANDARD_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getStandard(), isAi) : ImmutableList.of();
        case PIONEER_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getPioneer(), isAi) : ImmutableList.of();
        case HISTORIC_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getHistoric(), isAi) : ImmutableList.of();
        case MODERN_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getModern(), isAi) : ImmutableList.of();
        case LEGACY_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().get("Legacy"), isAi) : ImmutableList.of();
        case VINTAGE_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().get("Vintage"), isAi) : ImmutableList.of();
        case PAUPER_CARDGEN_DECK:
            return FModel.isdeckGenMatrixLoaded() ? ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getPauper(), isAi) : ImmutableList.of();
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
            return RandomDeckGenerator.getRandomDecks(lstDecks, isAi);
        default:
            return ImmutableList.of();
        }
    }

    private void updateBrowserRoot(final DeckType deckType) {
        browserRootType = deckType;
        browserGeneratedFolder = false;
        browserFolder = getStorageForDeckType(deckType);
        final DeckType folderShortcut = browserFolder == null ? null : getShortcutDeckTypeForFolder(browserFolder);
        if (folderShortcut != null) {
            browserRootType = folderShortcut;
            setShortcutDeckType(folderShortcut);
        }
        browserParentFolder = null;
        browserPath = "";
        browserHasDecksHomeParent = true;
        clearBrowserListParent();
        updateBrowserFolder();
    }

    private void updateDecksHome() {
        final List<DeckProxy> rows = new ArrayList<>();
        final IStorage<Deck> decksHome = getDecksHomeStorage();
        for (final IStorage<Deck> folder : decksHome.getFolders()) {
            final DeckType shortcutDeckType = getShortcutDeckTypeForFolder(folder);
            rows.add(DeckBrowserEntry.folder(folder.getName(), folder.getName(), folder, shortcutDeckType));
        }
        browserFolder = null;
        browserParentFolder = null;
        browserPath = "";
        browserGeneratedParentPath = GENERATED_HOME_PATH;
        browserRootType = null;
        browserGeneratedFolder = false;
        browserHasDecksHomeParent = false;
        clearBrowserListParent();
        sortBrowserRows(rows);
        lstDecks.setCaption("Decks");
        lstDecks.setAllowMultipleSelections(false);
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows);
        btnRandom.setText(localizer.getMessage("lblRandomDeck"));
        btnRandom.setCommand(this::randomSelectBrowserDeck);
        if (!selectPendingBrowserRow(displayedRows) && !displayedRows.isEmpty()) {
            lstDecks.setSelectedIndex(0);
        }
        updateEditorSaveTarget();
    }

    private void updateGeneratedGroup(final String path) {
        syncComboBoxForGeneratedGroup(path);
        final List<DeckProxy> rows = new ArrayList<>();
        rows.add(DeckBrowserEntry.parentFolder(getGeneratedGroupParentPath(path), getGeneratedGroupParentFolder(path)));
        if (GENERATED_RANDOM_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(RandomDeckGenerator.getRandomDecks(lstDecks, isAi)));
            rows.add(DeckBrowserEntry.generatedGroup("Random Archetype Decks", GENERATED_RANDOM_ARCHETYPES_PATH));
            rows.add(DeckBrowserEntry.generatedGroup(DeckType.COLOR_DECK.toString(), GENERATED_RANDOM_COLORS_PATH));
            addGeneratedFolderRows(rows, path, false, DeckType.THEME_DECK);
        } else if (GENERATED_RANDOM_COLORS_PATH.equals(path)) {
            rows.addAll(wrapGeneratedOptions(ColorDeckGenerator.getColorDecks(lstDecks, null, isAi)));
            addGeneratedFolderRows(rows, path, false, RANDOM_COLOR_DECK_TYPES);
        } else if (GENERATED_RANDOM_ARCHETYPES_PATH.equals(path)) {
            if (FModel.isdeckGenMatrixLoaded()) {
                addGeneratedFolderRows(rows, path, false, RANDOM_ARCHETYPE_DECK_TYPES);
            }
        }
        browserFolder = null;
        browserParentFolder = null;
        browserPath = "";
        browserGeneratedParentPath = path;
        browserRootType = null;
        browserGeneratedFolder = false;
        browserHasDecksHomeParent = false;
        clearBrowserListParent();
        sortBrowserRows(rows);
        lstDecks.setAllowMultipleSelections(false);
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows);
        btnRandom.setText(localizer.getMessage("lblRandomDeck"));
        btnRandom.setCommand(this::randomSelectBrowserDeck);
        if (!selectPendingBrowserRow(displayedRows) && !displayedRows.isEmpty()) {
            lstDecks.setSelectedIndex(0);
        }
        updateEditorSaveTarget();
    }

    private String getGeneratedGroupParentPath(final String path) {
        if (GENERATED_RANDOM_COLORS_PATH.equals(path) || GENERATED_RANDOM_ARCHETYPES_PATH.equals(path)) {
            return GENERATED_RANDOM_PATH;
        }
        return GENERATED_HOME_PATH;
    }

    private IStorage<Deck> getGeneratedGroupParentFolder(final String path) {
        if (GENERATED_RANDOM_PATH.equals(path)) {
            return getStorageForDeckType(DeckType.CUSTOM_DECK);
        }
        return null;
    }

    private void syncComboBoxForGeneratedGroup(final String path) {
        if (GENERATED_RANDOM_PATH.equals(path)) {
            setShortcutDeckType(DeckType.RANDOM_DECK);
        } else if (GENERATED_RANDOM_COLORS_PATH.equals(path)) {
            setShortcutDeckType(DeckType.COLOR_DECK);
        } else if (GENERATED_RANDOM_ARCHETYPES_PATH.equals(path) && decksComboBox != null) {
            selectedDeckType = DeckType.RANDOM_DECK;
            decksComboBox.setText("Random Archetype Decks");
            lstDecks.setCaption("Random Archetype Decks");
        }
    }

    private DeckType getShortcutDeckTypeForFolder(final IStorage<Deck> folder) {
        final String folderPath = new File(folder.getFullPath()).getAbsolutePath();
        if (folderPath.equals(new File(ForgeConstants.DECK_CONSTRUCTED_DIR).getAbsolutePath())) {
            return DeckType.CUSTOM_DECK;
        }
        if (folderPath.equals(new File(ForgeConstants.DECK_COMMANDER_DIR).getAbsolutePath())) {
            return DeckType.COMMANDER_DECK;
        }
        if (folderPath.equals(new File(ForgeConstants.DECK_OATHBREAKER_DIR).getAbsolutePath())) {
            return DeckType.OATHBREAKER_DECK;
        }
        if (folderPath.equals(new File(ForgeConstants.DECK_BRAWL_DIR).getAbsolutePath())) {
            return DeckType.BRAWL_DECK;
        }
        if (folderPath.equals(new File(ForgeConstants.DECK_TINY_LEADERS_DIR).getAbsolutePath())) {
            return DeckType.TINY_LEADERS_DECK;
        }
        if (folderPath.equals(new File(ForgeConstants.DECK_NET_DIR).getAbsolutePath())) {
            return isForCommander ? DeckType.NET_COMMANDER_DECK : DeckType.NET_DECK;
        }
        return null;
    }

    private DeckType getFolderDeckTypeForBrowserRow(final IStorage<Deck> folder) {
        return getShortcutDeckTypeForFolder(folder);
    }

    private boolean isSameFolder(final IStorage<Deck> first, final IStorage<Deck> second) {
        if (first == null || second == null) {
            return false;
        }
        return new File(first.getFullPath()).getAbsoluteFile().equals(new File(second.getFullPath()).getAbsoluteFile());
    }

    private void setShortcutDeckType(final DeckType deckType) {
        if (deckType == null || decksComboBox == null) {
            return;
        }
        selectedDeckType = deckType;
        decksComboBox.setDisplayedDeckType(deckType);
        lstDecks.setCaption(deckType.toString());
    }

    private boolean hasBrowserListParent() {
        return browserListParentFolder != null || !StringUtils.isBlank(browserListParentPath) || browserListParentRootType != null
                || browserListParentHasDecksHomeParent;
    }

    private void clearBrowserListParent() {
        browserListParentFolder = null;
        browserListParentPath = "";
        browserListParentRootType = null;
        browserListParentHasDecksHomeParent = false;
    }

    private void rememberCurrentBrowserLocationAsListParent() {
        browserListParentFolder = browserFolder;
        browserListParentPath = browserPath;
        browserListParentRootType = browserRootType;
        browserListParentHasDecksHomeParent = browserHasDecksHomeParent;
    }

    private boolean isGeneratedOrListBrowserView() {
        return browserGeneratedFolder || hasBrowserListParent()
                || isConstructedListDeckType(selectedDeckType) || selectedDeckType == DeckType.PRECON_COMMANDER_DECK;
    }

    private void rememberCurrentBrowserLocationForParentSelection() {
        if (browserGeneratedFolder) {
            rememberBrowserSelection(browserGeneratedParentPath, getGeneratedFolderDisplayName(selectedDeckType), selectedDeckType);
            return;
        }
        if (hasBrowserListParent()) {
            rememberBrowserSelection(browserListParentPath, getGeneratedFolderDisplayName(selectedDeckType), selectedDeckType);
            return;
        }
        if (browserFolder == null && StringUtils.isNotBlank(browserGeneratedParentPath)) {
            rememberBrowserSelection(browserGeneratedParentPath, getGeneratedGroupDisplayName(browserGeneratedParentPath));
            return;
        }
        if (StringUtils.isNotBlank(browserPath)) {
            rememberBrowserSelection(browserPath, lastPathSegment(browserPath));
        } else if (browserFolder != null) {
            rememberBrowserSelection(null, browserFolder.getName(), getShortcutDeckTypeForFolder(browserFolder));
        }
    }

    private String getGeneratedFolderDisplayName(final DeckType deckType) {
        return deckType == null ? null : deckType.toString();
    }

    private String getGeneratedGroupDisplayName(final String path) {
        if (GENERATED_RANDOM_ARCHETYPES_PATH.equals(path)) {
            return "Random Archetype Decks";
        }
        if (GENERATED_RANDOM_COLORS_PATH.equals(path)) {
            return DeckType.COLOR_DECK.toString();
        }
        if (GENERATED_RANDOM_PATH.equals(path)) {
            return DeckType.RANDOM_DECK.toString();
        }
        return lastPathSegment(path);
    }

    private void rememberBrowserSelection(final String path, final String name) {
        rememberBrowserSelection(path, name, null);
    }

    private void rememberBrowserSelection(final String path, final String name, final DeckType deckType) {
        pendingBrowserSelectionPath = path;
        pendingBrowserSelectionName = name;
        pendingBrowserSelectionDeckType = deckType;
    }

    private boolean selectPendingBrowserRow(final List<DeckProxy> rows) {
        if (StringUtils.isBlank(pendingBrowserSelectionPath) && StringUtils.isBlank(pendingBrowserSelectionName)
                && pendingBrowserSelectionDeckType == null) {
            return false;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!(rows.get(i) instanceof DeckBrowserEntry entry)) {
                continue;
            }
            final boolean pathMatches = StringUtils.isBlank(pendingBrowserSelectionPath)
                    || StringUtils.equals(entry.getPath(), pendingBrowserSelectionPath);
            final boolean nameMatches = StringUtils.isBlank(pendingBrowserSelectionName)
                    || StringUtils.equals(entry.getName(), pendingBrowserSelectionName);
            final boolean deckTypeMatches = pendingBrowserSelectionDeckType == null
                    || entry.getDeckType() == pendingBrowserSelectionDeckType;
            final boolean identityMatches = pendingBrowserSelectionDeckType == null ? nameMatches
                    : deckTypeMatches || nameMatches;
            if (pathMatches && identityMatches) {
                clearPendingBrowserSelection();
                selectBrowserRow(i);
                return true;
            }
        }
        clearPendingBrowserSelection();
        return false;
    }

    private void selectBrowserRow(final int rowIndex) {
        lstDecks.setSelectedIndex(rowIndex);
        scrollSelectedBrowserRowIntoViewLater();
    }

    private void scrollSelectedBrowserRowIntoViewLater() {
        SwingUtilities.invokeLater(lstDecks::scrollSelectionIntoView);
    }

    private void clearPendingBrowserSelection() {
        pendingBrowserSelectionPath = null;
        pendingBrowserSelectionName = null;
        pendingBrowserSelectionDeckType = null;
    }

    private String lastPathSegment(final String path) {
        final String cleanPath = StringUtils.stripEnd(StringUtils.defaultString(path), "/");
        final int idx = cleanPath.lastIndexOf('/');
        return idx < 0 ? cleanPath : cleanPath.substring(idx + 1);
    }

    private void updateBrowserFolder() {
        final List<DeckProxy> rows = new ArrayList<>();
        if (browserParentFolder != null || !StringUtils.isBlank(browserPath) || browserRootType != null || browserHasDecksHomeParent) {
            rows.add(DeckBrowserEntry.parentFolder(parentPath(browserPath), browserParentFolder));
        }
        if (browserFolder != null) {
            final Set<String> realFolderNames = new HashSet<>();
            final GameType gameType = getGameTypeForDeckType(browserRootType);
            for (final IStorage<Deck> folder : browserFolder.getFolders()) {
                realFolderNames.add(folder.getName());
                rows.add(DeckBrowserEntry.folder(folder.getName(), childPath(browserPath, folder.getName()), folder,
                        getFolderDeckTypeForBrowserRow(folder)));
            }
            for (final Deck deck : browserFolder) {
                rows.add(DeckBrowserEntry.deck(new DeckProxy(deck, gameType.toString(), gameType, browserPath, browserFolder, null)));
            }
            if (StringUtils.isBlank(browserPath) && !isEditorOnlyBrowser()) {
                if (browserRootType == DeckType.CUSTOM_DECK) {
                    rows.add(DeckBrowserEntry.generatedGroup(DeckType.RANDOM_DECK.toString(), GENERATED_RANDOM_PATH));
                    addGeneratedFolderRows(rows, browserPath, false, DeckType.PRECONSTRUCTED_DECK, DeckType.QUEST_OPPONENT_DECK);
                } else if (browserRootType == DeckType.COMMANDER_DECK) {
                    addGeneratedFolderRows(rows, browserPath, false, DeckType.RANDOM_COMMANDER_DECK);
                    if (FModel.isdeckGenMatrixLoaded()) {
                        addGeneratedFolderRows(rows, browserPath, false, DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
                    }
                    addGeneratedFolderRows(rows, browserPath, false, DeckType.PRECON_COMMANDER_DECK);
                }
            }
            if (StringUtils.isBlank(browserPath) && !isEditorOnlyBrowser() && isNetBrowserRoot()) {
                final Map<String, NetDeckCategory> categories = NetDeckCategory.getCategories(lstDecks.getGameType());
                if (categories != null) {
                    for (final NetDeckCategory category : categories.values()) {
                        if (!realFolderNames.contains(category.getName())) {
                            NetDeckCategory cached = NetDeckCategory.selectAndLoad(lstDecks.getGameType(), category.getName());
                            rows.add(DeckBrowserEntry.netFolder(category.getName(), childPath(browserPath, category.getName()), cached, DeckType.NET_DECK));
                        }
                    }
                }
            }
            if (isNetArchiveBrowserRoot()) {
                addNetArchiveVirtualFolders(rows, browserPath);
            }
        }
        sortBrowserRows(rows);
        lstDecks.setAllowMultipleSelections(false);
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows);
        btnRandom.setText(localizer.getMessage("lblRandomDeck"));
        btnRandom.setCommand(this::randomSelectBrowserDeck);
        if (!selectPendingBrowserRow(displayedRows) && !displayedRows.isEmpty()) {
            lstDecks.setSelectedIndex(0);
        }
        updateEditorSaveTarget();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void updateEditorSaveTarget() {
        if (!editorOnlyBrowser || browserFolder == null) {
            return;
        }
        updateEditorDeckMode();
        final DeckController controller = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController() == null
                ? null : CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();
        if (controller != null) {
            controller.setCurrentFolder(browserFolder, browserPath);
        }
    }

    private void updateEditorDeckMode() {
        final GameType gameType = getGameTypeForDeckType(browserRootType);
        final ACEditorBase<? extends InventoryItem, ? extends DeckBase> editor =
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (editor == null || editor.getGameType() != gameType) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorConstructed(lstDecks.getCDetailPicture(), gameType));
        }
    }

    private boolean isEditorOnlyBrowser() {
        return editorOnlyBrowser;
    }

    private ItemManagerConfig getBrowserItemManagerConfig() {
        final ItemManagerConfig config = editorOnlyBrowser ? ItemManagerConfig.DECK_EDITOR_BROWSER : ItemManagerConfig.DECK_BROWSER;
        setBrowserColumnVisible(config, ColumnDef.DECK_FAVORITE, browserHasDeckRows);
        setBrowserColumnVisible(config, ColumnDef.DECK_ACTIONS, browserHasDeckRows);
        setBrowserColumnVisible(config, ColumnDef.DECK_BRACKET, browserHasCommanderDeckRows && !isGeneratedOrListBrowserView());
        return config;
    }

    private void setBrowserColumnVisible(final ItemManagerConfig config, final ColumnDef columnDef, final boolean visible) {
        final ItemColumnConfig column = config.getCols().get(columnDef);
        if (column != null) {
            column.setVisible(visible);
        }
    }

    private boolean isNetBrowserRoot() {
        return browserRootType == DeckType.NET_DECK || browserRootType == DeckType.NET_COMMANDER_DECK;
    }

    private boolean isNetArchiveBrowser() {
        return isNetArchiveDeckType(selectedDeckType) && StringUtils.startsWith(browserPath, "archive/");
    }

    private boolean isNetArchiveBrowserRoot() {
        return !editorOnlyBrowser && StringUtils.equals(browserPath, "archive")
                && isFolderPath(browserFolder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private boolean isNetArchiveDeckType(final DeckType deckType) {
        return getNetArchiveSpec(deckType) != null;
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

    private void sortBrowserRows(final List<DeckProxy> rows) {
        rows.sort(Comparator
                .comparingInt((DeckProxy deck) -> deck instanceof DeckBrowserEntry ? ((DeckBrowserEntry) deck).getSortGroup() : 3)
                .thenComparing(deck -> deck.getName().toLowerCase()));
    }

    private String parentPath(final String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        return idx <= 0 ? "" : path.substring(0, idx);
    }

    private void updateColors(Predicate<PaperCard> formatFilter) {
        updateBrowserOptions(ColorDeckGenerator.getColorDecks(lstDecks, formatFilter, isAi), true,
                localizer.getMessage("lblRandomColors"), this::randomSelectBrowserColors,
                new Integer[]{0, 1});
    }

    private void updateMatrix(GameFormat format) {
        updateBrowserOptions(ArchetypeDeckGenerator.getMatrixDecks(format, isAi), false,
                "Random", this::randomSelectBrowserDeck, new Integer[]{0});
    }

    private void updateRandomCommander() {
        updateCommanderGenerator(false);
    }

    private void updateRandomCardGenCommander() {
        updateCommanderGenerator(true);
    }

    private void updateCommanderGenerator(final boolean isCardGen) {
        DeckFormat deckFormat = lstDecks.getGameType().getDeckFormat();
        if (!deckFormat.hasCommander()) {
            deckFormat = DeckFormat.Commander;
        }

        updateDecks(CommanderDeckGenerator.getCommanderDecks(deckFormat, isAi, isCardGen));
    }

    private void updateThemes() {
        updateDecks(DeckProxy.getAllThemeDecks());
    }

    private void updatePrecons() {
        updateDecks(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()));
    }

    private void updateCommanderPrecons() {
        updateDecks(DeckProxy.getAllCommanderPreconDecks());
    }

    private void updateQuestEvents() {
        updateDecks(DeckProxy.getAllQuestEventAndChallenges());
    }

    private void updateNetDecks() {
        if (netDeckCategory != null) {
            decksComboBox.setText(netDeckCategory.getDeckType());
        }
        updateDecks(DeckProxy.getNetDecks(netDeckCategory));
    }

    private void updateNetArchiveDecks(final DeckType deckType) {
        final IStorage<Deck> category = getLoadedNetArchiveCategory(deckType);
        if (category != null) {
            decksComboBox.setText(getLoadedNetArchiveDeckTypeLabel(deckType));
            openNetArchiveFolder(category);
        }
    }

    private IStorage<Deck> getLoadedNetArchiveCategory(final DeckType deckType) {
        return deckType == null ? null : loadedNetArchiveCategories.get(deckType);
    }

    private void setLoadedNetArchiveCategory(final DeckType deckType, final IStorage<Deck> category) {
        if (!isNetArchiveDeckType(deckType)) {
            return;
        }
        if (category instanceof NetDeckStorageBase) {
            loadedNetArchiveCategories.put(deckType, (NetDeckStorageBase) category);
        } else {
            loadedNetArchiveCategories.remove(deckType);
        }
    }

    private String getLoadedNetArchiveDeckTypeLabel(final DeckType deckType) {
        final IStorage<Deck> category = getLoadedNetArchiveCategory(deckType);
        return category == null || deckType == null ? StringUtils.defaultString(deckType == null ? null : deckType.toString())
                : deckType + " - " + category.getName();
    }

    private void updateNetEventDecks() {
        updateDecks(DeckProxy.getAllNetworkEventDecks(), ItemManagerConfig.NET_EVENT_DECKS);
    }

    public Deck getDeck() {
        final DeckProxy proxy = getSelectedDeckProxy();
        if (proxy == null) {
            return null;
        }
        return proxy.getDeck();
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getPlayer() {
        if (lstDecks.getSelectedIndex() < 0) { return null; }

        // Special branch for quest events
        if (selectedDeckType == DeckType.QUEST_OPPONENT_DECK) {
            final QuestEvent event = DeckgenUtil.getQuestEvent(lstDecks.getSelectedItem().getName());
            final RegisteredPlayer result = new RegisteredPlayer(event.getEventDeck());
            if (event instanceof QuestEventChallenge qec) {
                result.setStartingLife(qec.getAiLife());
            }
            result.addExtraCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
            return result;
        }

        return new RegisteredPlayer(getDeck());
    }

    public void populate() {
        if (decksComboBox == null) { //initialize components with delayed initialization the first time this is populated
            decksComboBox = new DecksComboBox();
            lstDecksContainer = new ItemManagerContainer(lstDecks);
            decksComboBox.addListener(this);
            if (editorOnlyBrowser) {
                updateDecksHome();
            } else {
                restoreSavedState();
            }
        } else {
            removeAll();
        }
        this.setLayout(new MigLayout("insets 0, gap 0"));
        if (!editorOnlyBrowser) {
            decksComboBox.addTo(this, "w 100%, h 30px!, gapbottom 5px, spanx 2, wrap");
        }
        this.add(lstDecksContainer, "w 100%, growy, pushy, spanx 2, wrap");
        if (!editorOnlyBrowser) {
            this.add(btnViewDeck, "w 50%-3px, h 30px!, gaptop 5px, gapright 6px");
            this.add(btnRandom, "w 50%-3px, h 30px!, gaptop 5px");
        }
        if (isShowing()) {
            revalidate();
            repaint();
        }
    }

    public final boolean isAi() {
        return isAi;
    }
    public void setIsAi(final boolean isAiDeck) {
        isAi = isAiDeck;
    }

    @Override
    public void deckTypeSelected(final DecksComboBoxEvent ev) {
        if (handleNetArchiveDeckTypeSelected(ev)) {
            return;
        } else if ((ev.getDeckType() == DeckType.NET_DECK || ev.getDeckType() == DeckType.NET_COMMANDER_DECK) && !refreshingDeckType) {
            refreshDecksList(ev.getDeckType(), true, ev);
            return;
        }
        refreshDecksList(ev.getDeckType(), false, ev);
    }

    private boolean handleNetArchiveDeckTypeSelected(final DecksComboBoxEvent ev) {
        final DeckType deckType = ev.getDeckType();
        if (!isNetArchiveDeckType(deckType) || refreshingDeckType) {
            return false;
        }
        if (lstDecks.getGameType() != GameType.Constructed) {
            return true;
        }

        FThreads.invokeInBackgroundThread(() -> {
            final IStorage<Deck> category = findSelectedNetArchiveCategory(lstDecks.getGameType(), deckType, null);
            FThreads.invokeInEdtLater(() -> {
                if (category == null) {
                    decksComboBox.setDeckType(selectedDeckType);
                    decksComboBox.setText(getLoadedNetArchiveDeckTypeLabel(selectedDeckType));
                    return;
                }

                setLoadedNetArchiveCategory(deckType, category);
                refreshDecksList(deckType, true, ev);
            });
        });
        return true;
    }

    public void refreshDeckListForAI() {
        //remember current deck by name, refresh decklist for AI/Human then reselect if possible
        String currentName = lstDecks.getSelectedItem().getName();

        UiCommand selectCmd = lstDecks.getSelectCommand();
        // ignore selection changes while refreshing to avoid repeating some deck generator calls
        lstDecks.setSelectCommand(null);

        refreshDecksList(selectedDeckType, true, null);

        lstDecks.setSelectedString(currentName);

        lstDecks.setSelectCommand(selectCmd);
        lstDecks.refresh();

        saveState();
    }

    private void setBrowserListParentRoot(final DeckType rootType) {
        browserListParentRootType = rootType;
        browserListParentFolder = getStorageForDeckType(rootType);
        browserListParentPath = "";
        browserListParentHasDecksHomeParent = true;
        browserRootType = rootType;
        browserGeneratedFolder = false;
    }

    private void refreshDecksList(final DeckType deckType, final boolean forceRefresh, final DecksComboBoxEvent ev) {
        if (decksComboBox == null) { return; } // Not yet populated
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;
        if (isCommanderGeneratedDeckType(deckType)) {
            setBrowserListParentRoot(DeckType.COMMANDER_DECK);
        } else if (isGeneratedDeckType(deckType)) {
            if (!browserGeneratedFolder) {
                browserGeneratedParentPath = getDefaultGeneratedParentPath(deckType);
            }
            browserRootType = isCommanderGeneratedDeckType(deckType) ? DeckType.COMMANDER_DECK : DeckType.CUSTOM_DECK;
            browserGeneratedFolder = true;
            clearBrowserListParent();
        } else if (isConstructedListDeckType(deckType) && !hasBrowserListParent()) {
            setBrowserListParentRoot(DeckType.CUSTOM_DECK);
        } else if (deckType == DeckType.PRECON_COMMANDER_DECK && !hasBrowserListParent()) {
            setBrowserListParentRoot(DeckType.COMMANDER_DECK);
        }

        if (ev == null) {
            refreshingDeckType = true;
            decksComboBox.refresh(deckType, isForCommander);
            refreshingDeckType = false;
        }
        lstDecks.setCaption(deckType.toString());

        if (isNetArchiveDeckType(deckType)) {
            updateNetArchiveDecks(deckType);
            return;
        }

        switch (deckType) {
            case CUSTOM_DECK:
                updateCustom();
                break;
            case COMMANDER_DECK:
            case OATHBREAKER_DECK:
            case TINY_LEADERS_DECK:
            case BRAWL_DECK:
                updateCustom();
                break;
            case COLOR_DECK:
                updateColors(null);
                break;
            case STANDARD_COLOR_DECK:
                updateColors(FModel.getFormats().getStandard().getFilterPrinted());
                break;
            case MODERN_COLOR_DECK:
                updateColors(FModel.getFormats().getModern().getFilterPrinted());
                break;
            case PAUPER_COLOR_DECK:
                updateColors(FModel.getFormats().getPauper().getFilterPrinted());
                break;
            case STANDARD_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getStandard());
                }
                break;
            case PIONEER_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getPioneer());
                }
                break;
            case HISTORIC_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getHistoric());
                }
                break;
            case MODERN_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getModern());
                }
                break;
            case LEGACY_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().get("Legacy"));
                }
                break;
            case VINTAGE_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().get("Vintage"));
                }
                break;
            case PAUPER_CARDGEN_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateMatrix(FModel.getFormats().getPauper());
                }
                break;
            case RANDOM_COMMANDER_DECK:
                updateRandomCommander();
                break;
            case RANDOM_CARDGEN_COMMANDER_DECK:
                if(FModel.isdeckGenMatrixLoaded()) {
                    updateRandomCardGenCommander();
                }
                break;
            case THEME_DECK:
                updateThemes();
                break;
            case QUEST_OPPONENT_DECK:
                updateQuestEvents();
                break;
            case PRECONSTRUCTED_DECK:
                updatePrecons();
                break;
            case PRECON_COMMANDER_DECK:
                updateCommanderPrecons();
                break;
            case RANDOM_DECK:
                updateGeneratedGroup(GENERATED_RANDOM_PATH);
                break;
            case NET_DECK:
            case NET_COMMANDER_DECK:
                updateBrowserRoot(deckType);
                break;
            case NET_EVENT_DECK:
                updateNetEventDecks();
                break;
            default:
                break; //other deck types not currently supported here
        }
    }

    private final String SELECTED_DECK_DELIMITER = "::";

    public void saveState() {
        if (stateSetting == null) {
            throw new NullPointerException("State setting missing. Specify first using the initialize() method.");
        }
        prefs.setPref(stateSetting, getState());
        prefs.save();
    }

    private String getState() {
        final StringBuilder state = new StringBuilder();
        DeckType selectedDeckType = this.selectedDeckType;   // decksComboBox.getDeckType()
        if (isNetArchiveDeckType(selectedDeckType)) {
            if (!appendLoadedNetArchiveState(state, selectedDeckType)) { return ""; }
        } else if (selectedDeckType == null || selectedDeckType == DeckType.NET_DECK) {
            //handle special case of net decks
            if (netDeckCategory == null) { return ""; }
            state.append(NetDeckCategory.PREFIX).append(netDeckCategory.getName());
        }
        else {
            state.append(selectedDeckType.name());
        }
        state.append(";");
        joinSelectedDecks(state, SELECTED_DECK_DELIMITER);
        return state.toString();
    }

    private boolean appendLoadedNetArchiveState(final StringBuilder state, final DeckType deckType) {
        final IStorage<Deck> category = getLoadedNetArchiveCategory(deckType);
        final String prefix = getNetArchivePrefix(deckType);
        if (category == null || prefix == null) {
            return false;
        }
        state.append(prefix).append(category.getName());
        return true;
    }

    private String getNetArchivePrefix(final DeckType deckType) {
        final NetArchiveSpec spec = getNetArchiveSpec(deckType);
        return spec == null ? null : spec.prefix;
    }

    private void joinSelectedDecks(final StringBuilder state, final String delimiter) {
        final Iterable<DeckProxy> selectedDecks = lstDecks.getSelectedItems();
        boolean isFirst = true;
        if (selectedDecks != null) {
            for (final DeckProxy deck : selectedDecks) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    state.append(delimiter);
                }
                state.append(deck.toString());
            }
        }
    }

    public void restoreSavedState() {
        final DeckType oldDeckType = selectedDeckType;
        if (stateSetting == null) {
            //if can't restore saved state, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
            return;
        }

        final String savedState = prefs.getPref(stateSetting);
        final DeckType savedDeckType = getDeckTypeFromSavedState(savedState);
        final List<String> selectedDecks = getSelectedDecksFromSavedState(savedState);
        rememberBrowserSelectionForSavedState(savedDeckType, selectedDecks);
        refreshDecksList(savedDeckType, true, null);
        if (!selectSavedDecks(selectedDecks)) {
            //if can't select old decks, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
        }
    }

    private void rememberBrowserSelectionForSavedState(final DeckType deckType, final List<String> selectedDecks) {
        if (selectedDecks.isEmpty()) {
            return;
        }
        final String deckName = lastPathSegment(selectedDecks.get(0));
        if (isGeneratedDeckType(deckType)) {
            rememberBrowserSelection(getDefaultGeneratedParentPath(deckType), deckName, deckType);
        } else if (isConstructedListDeckType(deckType) || deckType == DeckType.PRECON_COMMANDER_DECK) {
            rememberBrowserSelection("", deckName, deckType);
        } else if (isNetArchiveDeckType(deckType) || deckType == DeckType.NET_DECK || deckType == DeckType.NET_COMMANDER_DECK) {
            rememberBrowserSelection(null, deckName, deckType);
        }
    }

    private boolean selectSavedDecks(final List<String> selectedDecks) {
        if (selectedDecks.isEmpty()) {
            return true;
        }
        if (lstDecks.setSelectedStrings(selectedDecks)) {
            scrollSelectedBrowserRowIntoViewLater();
            return true;
        }

        final List<DeckProxy> items = new ArrayList<>();
        for (final String selectedDeck : selectedDecks) {
            final String selectedName = lastPathSegment(selectedDeck);
            for (final Entry<DeckProxy, Integer> itemEntry : lstDecks.getFilteredItems()) {
                final DeckProxy deck = itemEntry.getKey();
                if (StringUtils.equals(deck.toString(), selectedDeck)
                        || StringUtils.equals(deck.getName(), selectedDeck)
                        || StringUtils.equals(deck.getName(), selectedName)) {
                    items.add(deck);
                    break;
                }
            }
        }
        if (!items.isEmpty() && lstDecks.setSelectedItems(items)) {
            scrollSelectedBrowserRowIntoViewLater();
            return true;
        }
        return false;
    }

    private DeckType getDeckTypeFromSavedState(final String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return selectedDeckType;
            } else {
                final String deckType = savedState.split(";")[0];
                if (deckType.startsWith(NetDeckCategory.PREFIX)) {
                    netDeckCategory = NetDeckCategory.selectAndLoad(lstDecks.getGameType(), deckType.substring(NetDeckCategory.PREFIX.length()));
                    return DeckType.NET_DECK;
                }
                final NetArchiveSpec spec = getNetArchiveSpec(deckType);
                if (spec != null) {
                    setLoadedNetArchiveCategory(spec.deckType,
                            spec.loader.selectAndLoad(lstDecks.getGameType(), deckType.substring(spec.prefix.length()), false));
                    return spec.deckType;
                }
                return DeckType.valueOf(deckType);
            }
        } catch (final IllegalArgumentException ex) {
            System.err.println(ex.getMessage() + ". Using default : " + selectedDeckType);
            return selectedDeckType;
        }
    }

    private boolean isGeneratedDeckType(final DeckType deckType) {
        return GENERATED_DECK_TYPES.contains(deckType);
    }

    private boolean isHomeShortcutList(final DeckType deckType) {
        if (deckType == null) {
            return false;
        }
        switch (deckType) {
        case QUEST_OPPONENT_DECK:
            return true;
        default:
            return false;
        }
    }

    private boolean isConstructedListDeckType(final DeckType deckType) {
        return CONSTRUCTED_LIST_DECK_TYPES.contains(deckType);
    }

    private boolean isCommanderGeneratedDeckType(final DeckType deckType) {
        return COMMANDER_GENERATED_DECK_TYPES.contains(deckType);
    }

    private String getDefaultGeneratedParentPath(final DeckType deckType) {
        if (deckType == DeckType.COLOR_DECK || RANDOM_COLOR_DECK_TYPE_SET.contains(deckType)) {
            return GENERATED_RANDOM_COLORS_PATH;
        }
        if (RANDOM_ARCHETYPE_DECK_TYPE_SET.contains(deckType)) {
            return GENERATED_RANDOM_ARCHETYPES_PATH;
        }
        if (GENERATED_RANDOM_PARENT_DECK_TYPES.contains(deckType)) {
            return GENERATED_RANDOM_PATH;
        }
        return GENERATED_HOME_PATH;
    }

    private static Set<DeckType> getGeneratedDeckTypes() {
        final EnumSet<DeckType> deckTypes = EnumSet.of(DeckType.COLOR_DECK, DeckType.RANDOM_DECK);
        deckTypes.addAll(RANDOM_COLOR_DECK_TYPE_SET);
        deckTypes.addAll(RANDOM_ARCHETYPE_DECK_TYPE_SET);
        deckTypes.addAll(COMMANDER_GENERATED_DECK_TYPES);
        deckTypes.addAll(GENERATED_RANDOM_PARENT_DECK_TYPES);
        return deckTypes;
    }

    private List<String> getSelectedDecksFromSavedState(final String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return new ArrayList<>();
            }
            final String[] parts = savedState.split(";", -1);
            return Arrays.asList(parts[1].split(SELECTED_DECK_DELIMITER));
        } catch (final Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return new ArrayList<>();
        }
    }

    public DecksComboBox getDecksComboBox() {
        return decksComboBox;
    }
}

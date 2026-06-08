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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import static forge.deck.DeckBrowserGeneratedRows.HOME_PATH;
import static forge.deck.DeckBrowserGeneratedRows.RANDOM_PATH;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {
    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType;
    private ItemManagerContainer lstDecksContainer;
    private NetDeckCategory netDeckCategory;
    private final DeckBrowserNetService netService = new DeckBrowserNetService();

    private boolean refreshingDeckType;
    private boolean isForCommander;
    private final boolean editorOnlyBrowser;
    private final BrowserState browser = new BrowserState();
    private static final Integer[] DEFAULT_DECK_SELECTION = {0};
    private static final Integer[] DEFAULT_COLOR_SELECTION = {0, 1};

    private static final class BrowserState {
        private IStorage<Deck> folder;
        private IStorage<Deck> parentFolder;
        private String path = "";
        private String generatedParentPath = "";
        private DeckType rootType;
        private boolean generatedFolder;
        private boolean hasDecksHomeParent;
        private IStorage<Deck> listParentFolder;
        private String listParentPath = "";
        private DeckType listParentRootType;
        private boolean listParentHasDecksHomeParent;
        private String pendingSelectionPath;
        private String pendingSelectionName;
        private DeckType pendingSelectionDeckType;
        private boolean searchActive;
        private boolean hasDeckRows;
        private boolean hasCommanderDeckRows;

        private boolean hasListParent() {
            return listParentFolder != null || !StringUtils.isBlank(listParentPath) || listParentRootType != null
                    || listParentHasDecksHomeParent;
        }

        private void clearListParent() {
            listParentFolder = null;
            listParentPath = "";
            listParentRootType = null;
            listParentHasDecksHomeParent = false;
        }

        private void rememberCurrentAsListParent() {
            listParentFolder = folder;
            listParentPath = path;
            listParentRootType = rootType;
            listParentHasDecksHomeParent = hasDecksHomeParent;
        }

        private void rememberSelection(final String path0, final String name0) {
            rememberSelection(path0, name0, null);
        }

        private void rememberSelection(final String path0, final String name0, final DeckType deckType0) {
            pendingSelectionPath = path0;
            pendingSelectionName = name0;
            pendingSelectionDeckType = deckType0;
        }

        private boolean hasPendingSelection() {
            return StringUtils.isNotBlank(pendingSelectionPath) || StringUtils.isNotBlank(pendingSelectionName)
                    || pendingSelectionDeckType != null;
        }

        private void clearPendingSelection() {
            pendingSelectionPath = null;
            pendingSelectionName = null;
            pendingSelectionDeckType = null;
        }
    }

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
        if (browser.generatedFolder) {
            updateDecksHome();
        } else if (browser.folder != null) {
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
        } else if (browser.folder == null && StringUtils.isNotBlank(browser.generatedParentPath)) {
            updateGeneratedGroup(browser.generatedParentPath);
        } else if (browser.generatedFolder || browser.hasListParent()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (browser.folder != null) {
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
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browser.path)) {
            return true;
        }
        return isFolderUnder(browser.folder, ForgeConstants.DECK_NET_DIR)
                && !isFolderPath(browser.folder, ForgeConstants.DECK_NET_DIR);
    }

    private boolean isInNetArchiveFolder() {
        if (isNetArchiveBrowser() && StringUtils.isNotBlank(browser.path)) {
            return true;
        }
        return isFolderUnder(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR)
                && !isFolderPath(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private String getNetFolderName() {
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browser.path)) {
            return firstPathSegment(browser.path);
        }
        return firstPathSegment(relativeFolderPath(browser.folder, ForgeConstants.DECK_NET_DIR));
    }

    private String getNetArchiveFolderName() {
        if (StringUtils.startsWith(browser.path, "archive/")) {
            return firstPathSegment(StringUtils.removeStart(browser.path, "archive/"));
        }
        return firstPathSegment(relativeFolderPath(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR));
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
        if (StringUtils.startsWith(browser.path, "archive/")) {
            rootFolder = getArchiveStorage();
        } else if (browser.rootType == null) {
            rootFolder = getDecksHomeStorage();
        } else {
            rootFolder = getFreshStorageForDeckType(browser.rootType);
        }
        if (rootFolder == null) {
            return;
        }

        final String storagePath = StringUtils.startsWith(browser.path, "archive/")
                ? StringUtils.removeStart(browser.path, "archive/") : browser.path;
        final IStorage<Deck> refreshedFolder = StringUtils.isBlank(storagePath)
                ? rootFolder : rootFolder.tryGetFolder(storagePath);
        if (refreshedFolder == null) {
            return;
        }

        browser.folder = refreshedFolder;
        browser.parentFolder = StringUtils.isBlank(storagePath) ? null : rootFolder.tryGetFolder(parentPath(storagePath));
    }

    private void activateBrowserSelection() {
        final DeckProxy selected = lstDecks.getSelectedItem();
        if (selected instanceof DeckBrowserEntry entry) {
            switch (entry.getKind()) {
            case FOLDER:
                browser.clearListParent();
                if (entry.getDeckType() != null) {
                    browser.rootType = entry.getDeckType();
                    final IStorage<Deck> shortcutRoot = getStorageForDeckType(browser.rootType);
                    browser.path = isSameFolder(entry.getFolder(), shortcutRoot)
                            ? "" : getPathRelativeToShortcutRoot(entry.getPath(), browser.rootType);
                    browser.hasDecksHomeParent = true;
                    setShortcutDeckType(entry.getDeckType());
                } else {
                    browser.parentFolder = browser.folder;
                    browser.path = entry.getPath();
                    browser.hasDecksHomeParent = false;
                }
                browser.folder = entry.getFolder();
                browser.generatedFolder = false;
                if (browser.rootType != null) {
                    setShortcutDeckType(browser.rootType);
                }
                final IStorage<Deck> folderRoot = browser.rootType == null ? getDecksHomeStorage() : getStorageForDeckType(browser.rootType);
                browser.parentFolder = StringUtils.isBlank(browser.path) || folderRoot == null ? null : folderRoot.tryGetFolder(parentPath(browser.path));
                updateBrowserFolder();
                return;
            case PARENT_FOLDER:
                rememberCurrentBrowserLocationForParentSelection();
                if (!browser.hasListParent() && browser.hasDecksHomeParent
                        && StringUtils.isBlank(browser.path) && StringUtils.isBlank(entry.getPath())) {
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
                browser.folder = entry.getFolder();
                browser.path = entry.getPath();
                final DeckType parentShortcutType = getShortcutDeckTypeForFolder(browser.folder);
                if (parentShortcutType != null) {
                    browser.rootType = parentShortcutType;
                    browser.path = getPathRelativeToShortcutRoot(browser.path, browser.rootType);
                    browser.hasDecksHomeParent = true;
                } else {
                    browser.rootType = browser.listParentRootType == null ? browser.rootType : browser.listParentRootType;
                    if (browser.rootType == null && StringUtils.isBlank(browser.path)) {
                        browser.rootType = getShortcutDeckTypeForFolder(browser.folder);
                    }
                    browser.hasDecksHomeParent = browser.listParentHasDecksHomeParent;
                }
                browser.clearListParent();
                final IStorage<Deck> rootFolder = browser.rootType == null ? getDecksHomeStorage() : getStorageForDeckType(browser.rootType);
                browser.parentFolder = StringUtils.isBlank(browser.path) || rootFolder == null ? null : rootFolder.tryGetFolder(parentPath(browser.path));
                browser.generatedFolder = false;
                if (browser.rootType != null) {
                    setShortcutDeckType(browser.rootType);
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
                if (DeckBrowserGeneratedRows.isCommanderGeneratedDeckType(entry.getDeckType())) {
                    browser.rememberCurrentAsListParent();
                    browser.generatedFolder = false;
                } else if (DeckBrowserGeneratedRows.isGeneratedDeckType(entry.getDeckType())) {
                    browser.generatedFolder = true;
                    browser.generatedParentPath = entry.getPath();
                } else {
                    browser.rememberCurrentAsListParent();
                    browser.generatedFolder = false;
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
        if (deck == null || browser.folder == null) {
            return;
        }

        final FScreen screen = FScreen.DECK_EDITOR_CONSTRUCTED;
        if (!Singletons.getControl().ensureScreenActive(screen) || !SEditorIO.confirmSaveChanges(screen, true)) {
            return;
        }

        final GameType gameType = getGameTypeForDeckType(browser.rootType);
        ACEditorBase<? extends InventoryItem, ? extends DeckBase> editor =
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (editor == null || editor.getGameType() != gameType) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorConstructed(lstDecks.getCDetailPicture(), gameType));
            editor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        }
        if (editor == null || editor.getDeckController() == null) {
            return;
        }

        IStorage<Deck> currentFolder = browser.folder;
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
        final DeckType rootType = browser.rootType == null ? entry.getDeckType() : browser.rootType;
        final String name = entry.getName();
        refreshNetFolder(rootType, name);
    }

    private void refreshNetFolderFromSource(final String name) {
        final DeckType rootType = isNetBrowserRoot() ? browser.rootType
                : isForCommander ? DeckType.NET_COMMANDER_DECK : DeckType.NET_DECK;
        refreshNetFolder(rootType, name);
    }

    private void refreshNetFolder(final DeckType rootType, final String name) {
        FThreads.invokeInBackgroundThread(() -> {
            final DeckBrowserNetService.LoadedNetFolder loadedFolder =
                    netService.reloadNetFolder(rootType, getGameTypeForDeckType(rootType), name);
            FThreads.invokeInEdtLater(() -> {
                if (loadedFolder == null || loadedFolder.category == null) {
                    return;
                }
                final IStorage<Deck> netRoot = getStorageForDeckType(loadedFolder.rootType);
                final IStorage<Deck> downloadedFolder = netRoot == null ? null : netRoot.tryGetFolder(name);
                browser.rootType = loadedFolder.rootType;
                browser.parentFolder = netRoot;
                browser.folder = downloadedFolder == null ? loadedFolder.category : downloadedFolder;
                browser.path = childPath("", name);
                browser.generatedFolder = false;
                browser.clearListParent();
                updateBrowserFolder();
            });
        });
    }

    private void openNetArchiveFolder(final IStorage<Deck> category) {
        final IStorage<Deck> archiveRoot = getArchiveStorage();
        final IStorage<Deck> downloadedFolder = archiveRoot.tryGetFolder(category.getName());
        browser.rootType = null;
        browser.parentFolder = archiveRoot;
        browser.folder = downloadedFolder == null ? category : downloadedFolder;
        browser.path = childPath("archive", category.getName());
        browser.generatedFolder = false;
        browser.hasDecksHomeParent = false;
        browser.clearListParent();
        updateBrowserFolder();
    }

    private void refreshNetArchiveFolderFromSource(final String name) {
        final DeckType deckType = selectedDeckType;
        final GameType gameType = lstDecks.getGameType();
        FThreads.invokeInBackgroundThread(() -> {
            final DeckBrowserNetService.LoadedArchiveFolder loadedFolder =
                    netService.reloadNetArchiveCategory(gameType, deckType, name);
            FThreads.invokeInEdtLater(() -> {
                if (loadedFolder != null && loadedFolder.category != null) {
                    selectedDeckType = loadedFolder.deckType;
                    openNetArchiveFolder(loadedFolder.category);
                }
            });
        });
    }

    private void openNetArchiveVirtualFolder(final DeckType deckType) {
        final GameType gameType = lstDecks.getGameType();
        FThreads.invokeInBackgroundThread(() -> {
            final IStorage<Deck> category = netService.reloadSelectedNetArchiveCategory(gameType, deckType, null);
            FThreads.invokeInEdtLater(() -> {
                if (category != null) {
                    selectedDeckType = deckType;
                    setShortcutDeckType(deckType);
                    openNetArchiveFolder(category);
                }
            });
        });
    }

    private void updateDecks(final Iterable<DeckProxy> decks) {
        updateDecks(decks, null);
    }

    private void updateDecks(final Iterable<DeckProxy> decks, final ItemManagerConfig config) {
        updateBrowserOptions(decks, false, localizer.getMessage("lblRandomDeck"),
                this::randomSelectBrowserDeck, DEFAULT_DECK_SELECTION, config);
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
        if (browser.generatedFolder) {
            rows.add(0, DeckBrowserEntry.parentFolder(browser.generatedParentPath, null));
            leadingRows = 1;
        } else if (browser.hasListParent()) {
            rows.add(0, DeckBrowserEntry.parentFolder(browser.listParentPath, browser.listParentFolder));
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
            lstDecks.setSelectedIndex(browser.searchActive ? 0
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
            entries.add(DeckBrowserEntry.fromDeckProxy(deck));
        }
        sortBrowserRows(entries);
        return entries;
    }

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows) {
        return setBrowserPoolAndSetup(rows, null);
    }

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows, final ItemManagerConfig config) {
        final List<DeckProxy> displayedRows = browser.searchActive ? buildRecursiveSearchRows() : rows;
        browser.hasDeckRows = containsDeckRows(displayedRows);
        browser.hasCommanderDeckRows = containsCommanderDeckRows(displayedRows);
        lstDecks.setup(config == null ? getBrowserItemManagerConfig() : config);
        lstDecks.setPool(displayedRows);
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
        if (browser.searchActive == active) {
            return;
        }

        browser.searchActive = active;
        refreshCurrentBrowserRows();
    }

    private void refreshCurrentBrowserRows() {
        if (isGeneratedOrListBrowserView()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (StringUtils.isNotBlank(browser.generatedParentPath) && browser.folder == null) {
            updateGeneratedGroup(browser.generatedParentPath);
        } else if (isSearchGeneratedListType()) {
            refreshDecksList(selectedDeckType, true, new DecksComboBoxEvent(decksComboBox, selectedDeckType));
        } else if (browser.folder != null) {
            updateBrowserFolder();
        } else {
            updateDecksHome();
        }
    }

    private List<DeckProxy> buildRecursiveSearchRows() {
        final List<DeckProxy> rows = new ArrayList<>();
        if (editorOnlyBrowser) {
            if (browser.folder == null) {
                addDecksHomeRowsRecursively(rows, false);
            } else {
                addFolderRowsRecursively(rows, browser.folder, browser.path, browser.rootType);
            }
        } else if (isGeneratedOrListBrowserView()) {
            DeckBrowserGeneratedRows.addGeneratedRows(rows, selectedDeckType, lstDecks, lstDecks, isAi);
        } else if (StringUtils.isNotBlank(browser.generatedParentPath) && browser.folder == null) {
            DeckBrowserGeneratedRows.addGeneratedGroupRows(rows, browser.generatedParentPath, lstDecks, lstDecks, isAi, true);
        } else if (isSearchGeneratedListType()) {
            DeckBrowserGeneratedRows.addGeneratedRows(rows, selectedDeckType, lstDecks, lstDecks, isAi);
        } else if (browser.folder != null) {
            addFolderRowsRecursively(rows, browser.folder, browser.path, browser.rootType);
            addVirtualRowsForFolderRecursively(rows, browser.path, browser.rootType, browser.folder);
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
        return selectedDeckType != null && DeckBrowserGeneratedRows.isGeneratedDeckType(selectedDeckType);
    }

    private void addFolderRowsRecursively(final List<DeckProxy> rows, final IStorage<Deck> folder,
            final String path, final DeckType rootType) {
        if (folder == null) {
            return;
        }
        for (final IStorage<Deck> subFolder : folder.getFolders()) {
            final String subPath = childPath(path, subFolder.getName());
            rows.add(DeckBrowserEntry.folder(subFolder.getName(), subPath, subFolder, getShortcutDeckTypeForFolder(subFolder)));
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
            DeckBrowserGeneratedRows.addConstructedFolderRows(rows, path, true, lstDecks, lstDecks, isAi);
        } else if (rootType == DeckType.COMMANDER_DECK) {
            DeckBrowserGeneratedRows.addCommanderFolderRows(rows, path, true, lstDecks, lstDecks, isAi);
        } else if (rootType == DeckType.NET_DECK || rootType == DeckType.NET_COMMANDER_DECK) {
            final Set<String> realFolderNames = new HashSet<>();
            if (folder != null) {
                for (final IStorage<Deck> subFolder : folder.getFolders()) {
                    realFolderNames.add(subFolder.getName());
                }
            }
            final Iterable<NetDeckCategory> categories = NetDeckCategory.getAvailableCategories(lstDecks.getGameType());
            if (categories != null) {
                for (final NetDeckCategory category : categories) {
                    if (!realFolderNames.contains(category.getName())) {
                        rows.add(DeckBrowserEntry.netFolder(category.getName(), childPath(path, category.getName()), null, DeckType.NET_DECK));
                    }
                }
            }
        } else if (isArchiveRoot) {
            netService.addNetArchiveVirtualFolders(rows, path);
        }
    }

    private void updateBrowserRoot(final DeckType deckType) {
        browser.rootType = deckType;
        browser.generatedFolder = false;
        browser.folder = getStorageForDeckType(deckType);
        final DeckType folderShortcut = browser.folder == null ? null : getShortcutDeckTypeForFolder(browser.folder);
        if (folderShortcut != null) {
            browser.rootType = folderShortcut;
            setShortcutDeckType(folderShortcut);
        }
        browser.parentFolder = null;
        browser.path = "";
        browser.hasDecksHomeParent = true;
        browser.clearListParent();
        updateBrowserFolder();
    }

    private void updateDecksHome() {
        final List<DeckProxy> rows = new ArrayList<>();
        final IStorage<Deck> decksHome = getDecksHomeStorage();
        for (final IStorage<Deck> folder : decksHome.getFolders()) {
            final DeckType shortcutDeckType = getShortcutDeckTypeForFolder(folder);
            rows.add(DeckBrowserEntry.folder(folder.getName(), folder.getName(), folder, shortcutDeckType));
        }
        browser.folder = null;
        browser.parentFolder = null;
        browser.path = "";
        browser.generatedParentPath = HOME_PATH;
        browser.rootType = null;
        browser.generatedFolder = false;
        browser.hasDecksHomeParent = false;
        browser.clearListParent();
        lstDecks.setCaption("Decks");
        displaySingleSelectBrowserRows(rows);
    }

    private void updateGeneratedGroup(final String path) {
        syncComboBoxForGeneratedGroup(path);
        final List<DeckProxy> rows = new ArrayList<>();
        rows.add(DeckBrowserEntry.parentFolder(DeckBrowserGeneratedRows.getGeneratedGroupParentPath(path),
                getGeneratedGroupParentFolder(path)));
        DeckBrowserGeneratedRows.addGeneratedGroupRows(rows, path, lstDecks, lstDecks, isAi, false);
        browser.folder = null;
        browser.parentFolder = null;
        browser.path = "";
        browser.generatedParentPath = path;
        browser.rootType = null;
        browser.generatedFolder = false;
        browser.hasDecksHomeParent = false;
        browser.clearListParent();
        displaySingleSelectBrowserRows(rows);
    }

    private IStorage<Deck> getGeneratedGroupParentFolder(final String path) {
        final DeckType parentRootType = DeckBrowserGeneratedRows.getGeneratedGroupParentRootType(path);
        return parentRootType == null ? null : getStorageForDeckType(parentRootType);
    }

    private void syncComboBoxForGeneratedGroup(final String path) {
        final DeckType shortcutDeckType = DeckBrowserGeneratedRows.getGeneratedGroupShortcutDeckType(path);
        if (shortcutDeckType != null && decksComboBox != null) {
            selectedDeckType = shortcutDeckType;
            decksComboBox.setDisplayedDeckType(shortcutDeckType);
            final String displayName = DeckBrowserGeneratedRows.getGeneratedGroupDisplayName(path);
            decksComboBox.setText(displayName);
            lstDecks.setCaption(displayName);
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

    private boolean isGeneratedOrListBrowserView() {
        return browser.generatedFolder || browser.hasListParent()
                || DeckBrowserGeneratedRows.isConstructedListDeckType(selectedDeckType)
                || selectedDeckType == DeckType.PRECON_COMMANDER_DECK;
    }

    private void rememberCurrentBrowserLocationForParentSelection() {
        if (browser.generatedFolder) {
            browser.rememberSelection(browser.generatedParentPath, getGeneratedFolderDisplayName(selectedDeckType), selectedDeckType);
            return;
        }
        if (browser.hasListParent()) {
            browser.rememberSelection(browser.listParentPath, getGeneratedFolderDisplayName(selectedDeckType), selectedDeckType);
            return;
        }
        if (browser.folder == null && StringUtils.isNotBlank(browser.generatedParentPath)) {
            browser.rememberSelection(browser.generatedParentPath,
                    DeckBrowserGeneratedRows.getGeneratedGroupDisplayName(browser.generatedParentPath));
            return;
        }
        if (StringUtils.isNotBlank(browser.path)) {
            browser.rememberSelection(browser.path, lastPathSegment(browser.path));
        } else if (browser.folder != null) {
            browser.rememberSelection(null, browser.folder.getName(), getShortcutDeckTypeForFolder(browser.folder));
        }
    }

    private String getGeneratedFolderDisplayName(final DeckType deckType) {
        return deckType == null ? null : deckType.toString();
    }

    private boolean selectPendingBrowserRow(final List<DeckProxy> rows) {
        if (!browser.hasPendingSelection()) {
            return false;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!(rows.get(i) instanceof DeckBrowserEntry entry)) {
                continue;
            }
            final boolean pathMatches = StringUtils.isBlank(browser.pendingSelectionPath)
                    || StringUtils.equals(entry.getPath(), browser.pendingSelectionPath);
            final boolean nameMatches = StringUtils.isBlank(browser.pendingSelectionName)
                    || StringUtils.equals(entry.getName(), browser.pendingSelectionName);
            final boolean deckTypeMatches = browser.pendingSelectionDeckType == null
                    || entry.getDeckType() == browser.pendingSelectionDeckType;
            final boolean identityMatches = browser.pendingSelectionDeckType == null ? nameMatches
                    : deckTypeMatches || nameMatches;
            if (pathMatches && identityMatches) {
                browser.clearPendingSelection();
                selectBrowserRow(i);
                return true;
            }
        }
        browser.clearPendingSelection();
        return false;
    }

    private void selectBrowserRow(final int rowIndex) {
        lstDecks.setSelectedIndex(rowIndex);
        scrollSelectedBrowserRowIntoViewLater();
    }

    private void scrollSelectedBrowserRowIntoViewLater() {
        SwingUtilities.invokeLater(lstDecks::scrollSelectionIntoView);
    }

    private String lastPathSegment(final String path) {
        final String cleanPath = StringUtils.stripEnd(StringUtils.defaultString(path), "/");
        final int idx = cleanPath.lastIndexOf('/');
        return idx < 0 ? cleanPath : cleanPath.substring(idx + 1);
    }

    private void updateBrowserFolder() {
        final List<DeckProxy> rows = new ArrayList<>();
        if (browser.parentFolder != null || !StringUtils.isBlank(browser.path) || browser.rootType != null || browser.hasDecksHomeParent) {
            rows.add(DeckBrowserEntry.parentFolder(parentPath(browser.path), browser.parentFolder));
        }
        if (browser.folder != null) {
            final Set<String> realFolderNames = new HashSet<>();
            final GameType gameType = getGameTypeForDeckType(browser.rootType);
            for (final IStorage<Deck> folder : browser.folder.getFolders()) {
                realFolderNames.add(folder.getName());
                rows.add(DeckBrowserEntry.folder(folder.getName(), childPath(browser.path, folder.getName()), folder,
                        getShortcutDeckTypeForFolder(folder)));
            }
            for (final Deck deck : browser.folder) {
                rows.add(DeckBrowserEntry.deck(new DeckProxy(deck, gameType.toString(), gameType, browser.path, browser.folder, null)));
            }
            if (StringUtils.isBlank(browser.path) && !editorOnlyBrowser) {
                if (browser.rootType == DeckType.CUSTOM_DECK) {
                    DeckBrowserGeneratedRows.addConstructedFolderRows(rows, browser.path, false, lstDecks, lstDecks, isAi);
                } else if (browser.rootType == DeckType.COMMANDER_DECK) {
                    DeckBrowserGeneratedRows.addCommanderFolderRows(rows, browser.path, false, lstDecks, lstDecks, isAi);
                }
            }
            if (StringUtils.isBlank(browser.path) && !editorOnlyBrowser && isNetBrowserRoot()) {
                final Iterable<NetDeckCategory> categories = NetDeckCategory.getAvailableCategories(lstDecks.getGameType());
                if (categories != null) {
                    for (final NetDeckCategory category : categories) {
                        if (!realFolderNames.contains(category.getName())) {
                            NetDeckCategory cached = NetDeckCategory.selectAndLoad(lstDecks.getGameType(), category.getName());
                            rows.add(DeckBrowserEntry.netFolder(category.getName(), childPath(browser.path, category.getName()), cached, DeckType.NET_DECK));
                        }
                    }
                }
            }
            if (isNetArchiveBrowserRoot()) {
                netService.addNetArchiveVirtualFolders(rows, browser.path);
            }
        }
        displaySingleSelectBrowserRows(rows);
    }

    private void displaySingleSelectBrowserRows(final List<DeckProxy> rows) {
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
        if (!editorOnlyBrowser || browser.folder == null) {
            return;
        }
        updateEditorDeckMode();
        final DeckController controller = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController() == null
                ? null : CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();
        if (controller != null) {
            controller.setCurrentFolder(browser.folder, browser.path);
        }
    }

    private void updateEditorDeckMode() {
        final GameType gameType = getGameTypeForDeckType(browser.rootType);
        final ACEditorBase<? extends InventoryItem, ? extends DeckBase> editor =
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (editor == null || editor.getGameType() != gameType) {
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorConstructed(lstDecks.getCDetailPicture(), gameType));
        }
    }

    private ItemManagerConfig getBrowserItemManagerConfig() {
        final ItemManagerConfig config = editorOnlyBrowser ? ItemManagerConfig.DECK_EDITOR_BROWSER : ItemManagerConfig.DECK_BROWSER;
        setBrowserColumnVisible(config, ColumnDef.DECK_FAVORITE, browser.hasDeckRows);
        setBrowserColumnVisible(config, ColumnDef.DECK_ACTIONS, browser.hasDeckRows);
        setBrowserColumnVisible(config, ColumnDef.DECK_BRACKET, browser.hasCommanderDeckRows && !isGeneratedOrListBrowserView());
        return config;
    }

    private void setBrowserColumnVisible(final ItemManagerConfig config, final ColumnDef columnDef, final boolean visible) {
        final ItemColumnConfig column = config.getCols().get(columnDef);
        if (column != null) {
            column.setVisible(visible);
        }
    }

    private boolean isNetBrowserRoot() {
        return browser.rootType == DeckType.NET_DECK || browser.rootType == DeckType.NET_COMMANDER_DECK;
    }

    private boolean isNetArchiveBrowser() {
        return isNetArchiveDeckType(selectedDeckType) && StringUtils.startsWith(browser.path, "archive/");
    }

    private boolean isNetArchiveBrowserRoot() {
        return !editorOnlyBrowser && StringUtils.equals(browser.path, "archive")
                && isFolderPath(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private boolean isNetArchiveDeckType(final DeckType deckType) {
        return netService.isNetArchiveDeckType(deckType);
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
                DEFAULT_COLOR_SELECTION);
    }

    private void updateMatrix(GameFormat format) {
        updateBrowserOptions(ArchetypeDeckGenerator.getMatrixDecks(format, isAi), false,
                "Random", this::randomSelectBrowserDeck, DEFAULT_DECK_SELECTION);
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
        final IStorage<Deck> category = netService.getLoadedNetArchiveCategory(deckType);
        if (category != null) {
            decksComboBox.setText(netService.getLoadedNetArchiveDeckTypeLabel(deckType));
            openNetArchiveFolder(category);
        }
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
            final IStorage<Deck> category = netService.findSelectedNetArchiveCategory(lstDecks.getGameType(), deckType, null);
            FThreads.invokeInEdtLater(() -> {
                if (category == null) {
                    decksComboBox.setDeckType(selectedDeckType);
                    decksComboBox.setText(netService.getLoadedNetArchiveDeckTypeLabel(selectedDeckType));
                    return;
                }

                netService.setLoadedNetArchiveCategory(deckType, category);
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
        browser.listParentRootType = rootType;
        browser.listParentFolder = getStorageForDeckType(rootType);
        browser.listParentPath = "";
        browser.listParentHasDecksHomeParent = true;
        browser.rootType = rootType;
        browser.generatedFolder = false;
    }

    private void refreshDecksList(final DeckType deckType, final boolean forceRefresh, final DecksComboBoxEvent ev) {
        if (decksComboBox == null) { return; } // Not yet populated
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;
        if (DeckBrowserGeneratedRows.isCommanderGeneratedDeckType(deckType)) {
            setBrowserListParentRoot(DeckType.COMMANDER_DECK);
        } else if (DeckBrowserGeneratedRows.isGeneratedDeckType(deckType)) {
            if (!browser.generatedFolder) {
                browser.generatedParentPath = DeckBrowserGeneratedRows.getDefaultGeneratedParentPath(deckType);
            }
            browser.rootType = DeckBrowserGeneratedRows.isCommanderGeneratedDeckType(deckType) ? DeckType.COMMANDER_DECK : DeckType.CUSTOM_DECK;
            browser.generatedFolder = true;
            browser.clearListParent();
        } else if (DeckBrowserGeneratedRows.isConstructedListDeckType(deckType) && !browser.hasListParent()) {
            setBrowserListParentRoot(DeckType.CUSTOM_DECK);
        } else if (deckType == DeckType.PRECON_COMMANDER_DECK && !browser.hasListParent()) {
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
                updateGeneratedGroup(RANDOM_PATH);
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
            if (!netService.appendLoadedNetArchiveState(state, selectedDeckType)) { return ""; }
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
        if (DeckBrowserGeneratedRows.isGeneratedDeckType(deckType)) {
            browser.rememberSelection(DeckBrowserGeneratedRows.getDefaultGeneratedParentPath(deckType), deckName, deckType);
        } else if (DeckBrowserGeneratedRows.isConstructedListDeckType(deckType) || deckType == DeckType.PRECON_COMMANDER_DECK) {
            browser.rememberSelection("", deckName, deckType);
        } else if (isNetArchiveDeckType(deckType) || deckType == DeckType.NET_DECK || deckType == DeckType.NET_COMMANDER_DECK) {
            browser.rememberSelection(null, deckName, deckType);
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
                final DeckType netArchiveDeckType = netService.restoreSavedNetArchiveState(deckType, lstDecks.getGameType());
                if (netArchiveDeckType != null) {
                    return netArchiveDeckType;
                }
                return DeckType.valueOf(deckType);
            }
        } catch (final IllegalArgumentException ex) {
            System.err.println(ex.getMessage() + ". Using default : " + selectedDeckType);
            return selectedDeckType;
        }
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

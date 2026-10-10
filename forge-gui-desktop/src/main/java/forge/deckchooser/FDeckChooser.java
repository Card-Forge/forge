package forge.deckchooser;

import com.google.common.collect.ImmutableList;
import forge.Singletons;
import forge.deck.*;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.quest.QuestEvent;
import forge.gamemodes.quest.QuestEventChallenge;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.gui.UiCommand;
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
import forge.toolbox.FTextField;
import forge.deck.DeckBase;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.IHasName;
import forge.util.storage.IStorage;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

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
    private static final DeckType[] COMMANDER_DECK_TYPES = {
            DeckType.COMMANDER_DECK, DeckType.OATHBREAKER_DECK,
            DeckType.BRAWL_DECK, DeckType.TINY_LEADERS_DECK
    };

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

        private void setListLocation(final String generatedParentPath0) {
            folder = null;
            parentFolder = null;
            path = "";
            generatedParentPath = generatedParentPath0;
            rootType = null;
            generatedFolder = false;
            hasDecksHomeParent = false;
            clearListParent();
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
            .tooltip(localizer.getMessage("lblRefresh"))
            .build();
    private JPanel pnlDeckUrl;
    private FTextField txtDeckUrl;
    private FLabel btnReloadUrl;
    private String lastImportedUrlDeckName;
    private UiCommand deckSelectionCommand;
    private boolean updatingDeckPool;

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
            final DeckProxy selected = lstDecks.getSelectedItem();
            if (selected instanceof DeckBrowserEntry entry && !entry.isDeck()) {
                return;
            }
            if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                showDeckViewer();
            }
        };
        lstDecks.setItemActivateCommand(this::activateBrowserSelection);
        lstDecks.setSelectCommand(this::handleDeckSelection);
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

    public GameType getEditorGameTypeForCurrentFolder() {
        return editorOnlyBrowser ? DeckBrowserLocation.gameTypeFor(browser.rootType, lstDecks.getGameType()) : null;
    }

    public void applyEditorSaveTarget() {
        updateEditorSaveTarget();
    }

    public void setDeckSelectionCommand(final UiCommand command) {
        deckSelectionCommand = command;
    }

    public void refreshEditorBrowser() {
        if (editorOnlyBrowser) {
            refreshCurrentEditorBrowserLocation();
        }
    }

    private void refreshCurrentEditorBrowserLocation() {
        if (browser.folder != null) {
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

    private boolean isInNetDeckFolder() {
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browser.path)) {
            return true;
        }
        return DeckBrowserLocation.isFolderUnder(browser.folder, ForgeConstants.DECK_NET_DIR)
                && !DeckBrowserLocation.isSameFolder(browser.folder, ForgeConstants.DECK_NET_DIR);
    }

    private boolean isInNetArchiveFolder() {
        if (isNetArchiveBrowser() && StringUtils.isNotBlank(browser.path)) {
            return true;
        }
        return DeckBrowserLocation.isFolderUnder(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR)
                && !DeckBrowserLocation.isSameFolder(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private String getNetFolderName() {
        if (isNetBrowserRoot() && StringUtils.isNotBlank(browser.path)) {
            return DeckBrowserLocation.firstSegment(browser.path);
        }
        return DeckBrowserLocation.firstSegment(
                DeckBrowserLocation.relativeFolderPath(browser.folder, ForgeConstants.DECK_NET_DIR));
    }

    private String getNetArchiveFolderName() {
        if (StringUtils.startsWith(browser.path, "archive/")) {
            return DeckBrowserLocation.firstSegment(StringUtils.removeStart(browser.path, "archive/"));
        }
        return DeckBrowserLocation.firstSegment(
                DeckBrowserLocation.relativeFolderPath(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR));
    }

    private void reloadBrowserFolderFromDisk() {
        final IStorage<Deck> rootFolder;
        if (StringUtils.startsWith(browser.path, "archive/")) {
            rootFolder = DeckBrowserLocation.archiveStorage();
        } else if (browser.rootType == null) {
            rootFolder = DeckBrowserLocation.decksHomeStorage();
        } else {
            rootFolder = DeckBrowserLocation.freshStorageFor(browser.rootType);
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
        browser.parentFolder = StringUtils.isBlank(storagePath) ? null
                : rootFolder.tryGetFolder(DeckBrowserLocation.parentPath(storagePath));
    }

    private void activateBrowserSelection() {
        final DeckProxy selected = lstDecks.getSelectedItem();
        if (selected instanceof DeckBrowserEntry entry) {
            switch (entry.getKind()) {
            case FOLDER:
                browser.clearListParent();
                if (entry.getDeckType() != null) {
                    browser.rootType = entry.getDeckType();
                    final IStorage<Deck> shortcutRoot = DeckBrowserLocation.storageFor(browser.rootType);
                    browser.path = DeckBrowserLocation.isSameFolder(entry.getFolder(), shortcutRoot)
                            ? "" : DeckBrowserLocation.relativePathToRoot(entry.getPath(), browser.rootType);
                    browser.hasDecksHomeParent = true;
                } else {
                    browser.path = entry.getPath();
                    browser.hasDecksHomeParent = false;
                }
                browser.folder = entry.getFolder();
                browser.generatedFolder = false;
                if (browser.rootType != null) {
                    setShortcutDeckType(browser.rootType);
                }
                final IStorage<Deck> folderRoot = browser.rootType == null ? DeckBrowserLocation.decksHomeStorage() : DeckBrowserLocation.storageFor(browser.rootType);
                browser.parentFolder = StringUtils.isBlank(browser.path) || folderRoot == null ? null
                        : folderRoot.tryGetFolder(DeckBrowserLocation.parentPath(browser.path));
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
                final DeckType parentShortcutType = DeckBrowserLocation.shortcutDeckType(browser.folder, isForCommander);
                if (parentShortcutType != null) {
                    browser.rootType = parentShortcutType;
                    browser.path = DeckBrowserLocation.relativePathToRoot(browser.path, browser.rootType);
                    browser.hasDecksHomeParent = true;
                } else {
                    browser.rootType = browser.listParentRootType == null ? browser.rootType : browser.listParentRootType;
                    if (browser.rootType == null && StringUtils.isBlank(browser.path)) {
                        browser.rootType = DeckBrowserLocation.shortcutDeckType(browser.folder, isForCommander);
                    }
                    browser.hasDecksHomeParent = browser.listParentHasDecksHomeParent;
                }
                browser.clearListParent();
                final IStorage<Deck> rootFolder = browser.rootType == null ? DeckBrowserLocation.decksHomeStorage() : DeckBrowserLocation.storageFor(browser.rootType);
                browser.parentFolder = StringUtils.isBlank(browser.path) || rootFolder == null ? null
                        : rootFolder.tryGetFolder(DeckBrowserLocation.parentPath(browser.path));
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
        return DeckBrowserEntry.unwrap(selected);
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

        final GameType gameType = getGameTypeForDeck(deck);
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
        final Deck loadedDeck = (Deck) controller.getModel();
        if (loadedDeck != null) {
            loadedDeck.setDeckFormat(gameType.getDeckFormat());
        }
        setEditorDeckPreference(gameType, deck);
    }

    private GameType getGameTypeForDeck(final DeckProxy deck) {
        final Deck loadedDeck = deck.getDeck();
        final DeckFormat deckFormat = loadedDeck == null ? null : loadedDeck.getDeckFormat();
        if ((deckFormat == null || deckFormat == DeckFormat.Constructed) && deck.hasCommanderSection()) {
            return getLegacyCommanderGameType(deck);
        }
        if (deckFormat == null) {
            return GameType.Constructed;
        }
        return switch (deckFormat) {
        case Commander -> GameType.Commander;
        case Oathbreaker -> GameType.Oathbreaker;
        case Brawl -> GameType.Brawl;
        case TinyLeaders -> GameType.TinyLeaders;
        default -> GameType.Constructed;
        };
    }

    private GameType getLegacyCommanderGameType(final DeckProxy deck) {
        final IStorage<? extends IHasName> storage = deck.getStorage();
        if (storage != null) {
            for (final DeckType deckType : COMMANDER_DECK_TYPES) {
                final IStorage<Deck> root = DeckBrowserLocation.storageFor(deckType);
                if (root != null && DeckBrowserLocation.isFolderUnder(storage, root.getFullPath())) {
                    return DeckBrowserLocation.gameTypeFor(deckType, lstDecks.getGameType());
                }
            }
        }
        final GameType folderGameType = DeckBrowserLocation.gameTypeFor(browser.rootType, lstDecks.getGameType());
        return folderGameType != null && folderGameType.getDeckFormat().hasCommander()
                ? folderGameType : GameType.Commander;
    }

    private void setEditorDeckPreference(final GameType gameType, final DeckProxy deck) {
        switch (gameType) {
        case Commander, Oathbreaker -> DeckPreferences.setCommanderDeck(deck.toString());
        case Brawl -> DeckPreferences.setBrawlDeck(deck.toString());
        case TinyLeaders -> DeckPreferences.setTinyLeadersDeck(deck.toString());
        default -> DeckPreferences.setCurrentDeck(deck.toString());
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
                    netService.reloadNetFolder(rootType,
                            DeckBrowserLocation.gameTypeFor(rootType, lstDecks.getGameType()), name);
            FThreads.invokeInEdtLater(() -> {
                if (loadedFolder == null || loadedFolder.category() == null) {
                    return;
                }
                final IStorage<Deck> netRoot = DeckBrowserLocation.storageFor(loadedFolder.rootType());
                final IStorage<Deck> downloadedFolder = netRoot == null ? null : netRoot.tryGetFolder(name);
                browser.rootType = loadedFolder.rootType();
                browser.parentFolder = netRoot;
                browser.folder = downloadedFolder == null ? loadedFolder.category() : downloadedFolder;
                browser.path = DeckBrowserLocation.childPath("", name);
                browser.generatedFolder = false;
                browser.clearListParent();
                updateBrowserFolder();
            });
        });
    }

    private void openNetArchiveFolder(final IStorage<Deck> category) {
        final IStorage<Deck> archiveRoot = DeckBrowserLocation.archiveStorage();
        final IStorage<Deck> downloadedFolder = archiveRoot.tryGetFolder(category.getName());
        browser.rootType = null;
        browser.parentFolder = archiveRoot;
        browser.folder = downloadedFolder == null ? category : downloadedFolder;
        browser.path = DeckBrowserLocation.childPath("archive", category.getName());
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
                if (loadedFolder != null && loadedFolder.category() != null) {
                    selectedDeckType = loadedFolder.deckType();
                    openNetArchiveFolder(loadedFolder.category());
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
                this::randomSelectBrowserDeck, config);
    }

    private void updateBrowserOptions(final Iterable<DeckProxy> decks, final boolean allowMultipleSelections,
            final String randomText, final UiCommand randomCommand) {
        updateBrowserOptions(decks, allowMultipleSelections, randomText, randomCommand, null);
    }

    private void updateBrowserOptions(final Iterable<DeckProxy> decks, final boolean allowMultipleSelections,
            final String randomText, final UiCommand randomCommand, final ItemManagerConfig config) {
        lstDecks.setAllowMultipleSelections(allowMultipleSelections);

        final List<DeckProxy> rows = DeckBrowserEntry.fromDeckProxies(decks);
        DeckBrowserEntry.sort(rows);
        if (browser.generatedFolder) {
            rows.add(0, DeckBrowserEntry.parentFolder(browser.generatedParentPath, null));
        } else if (browser.hasListParent()) {
            rows.add(0, DeckBrowserEntry.parentFolder(browser.listParentPath, browser.listParentFolder));
        }
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows, config);

        btnRandom.setText(randomText);
        btnRandom.setCommand(randomCommand);

        if (!selectPendingBrowserRow(displayedRows)) {
            lstDecks.clearSelection();
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

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows) {
        return setBrowserPoolAndSetup(rows, null);
    }

    private List<DeckProxy> setBrowserPoolAndSetup(final List<DeckProxy> rows, final ItemManagerConfig config) {
        final List<DeckProxy> displayedRows = browser.searchActive ? buildRecursiveSearchRows() : rows;
        browser.hasDeckRows = DeckBrowserEntry.containsDeckRows(displayedRows);
        browser.hasCommanderDeckRows = DeckBrowserEntry.containsCommanderDeckRows(displayedRows);
        updatingDeckPool = true;
        try {
            // Clear the old source before applying a new ItemManagerConfig; otherwise stale items can
            // be sorted/rendered with columns from the next deck browser during source transitions.
            lstDecks.setPool(ImmutableList.of());
            lstDecks.setup(config == null ? getBrowserItemManagerConfig() : config);
            lstDecks.setPool(displayedRows);
        } finally {
            updatingDeckPool = false;
        }
        return displayedRows;
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
                addDecksHomeRows(rows, true, false);
            } else {
                addFolderRows(rows, browser.folder, browser.path, browser.rootType, true);
            }
        } else if (isGeneratedOrListBrowserView()) {
            DeckBrowserGeneratedRows.addGeneratedRows(rows, selectedDeckType, lstDecks, lstDecks, isAi);
        } else if (StringUtils.isNotBlank(browser.generatedParentPath) && browser.folder == null) {
            DeckBrowserGeneratedRows.addGeneratedGroupRows(rows, browser.generatedParentPath, lstDecks, lstDecks, isAi, true);
        } else if (browser.folder != null) {
            addFolderRows(rows, browser.folder, browser.path, browser.rootType, true);
            addVirtualRowsForFolderRecursively(rows, browser.path, browser.rootType, browser.folder);
        } else {
            addDecksHomeRows(rows, true, true);
        }
        DeckBrowserEntry.sort(rows);
        return rows;
    }

    private void addDecksHomeRows(final List<DeckProxy> rows, final boolean includeDescendants,
            final boolean includeVirtualRows) {
        final IStorage<Deck> decksHome = DeckBrowserLocation.decksHomeStorage();
        for (final IStorage<Deck> folder : decksHome.getFolders()) {
            final DeckType shortcutDeckType = DeckBrowserLocation.shortcutDeckType(folder, isForCommander);
            final String path = folder.getName();
            rows.add(DeckBrowserEntry.folder(folder.getName(), path, folder, shortcutDeckType));
            if (includeDescendants) {
                addFolderRows(rows, folder, path, shortcutDeckType, true);
                if (includeVirtualRows) {
                    addVirtualRowsForFolderRecursively(rows, path, shortcutDeckType, folder);
                }
            }
        }
    }

    private void addFolderRows(final List<DeckProxy> rows, final IStorage<Deck> folder,
            final String path, final DeckType rootType, final boolean includeDescendants) {
        if (folder == null) {
            return;
        }
        for (final IStorage<Deck> subFolder : folder.getFolders()) {
            final String subPath = DeckBrowserLocation.childPath(path, subFolder.getName());
            rows.add(DeckBrowserEntry.folder(subFolder.getName(), subPath, subFolder, DeckBrowserLocation.shortcutDeckType(subFolder, isForCommander)));
            if (includeDescendants) {
                addFolderRows(rows, subFolder, subPath, rootType, true);
                addVirtualRowsForFolderRecursively(rows, subPath, rootType, subFolder);
            }
        }
        final GameType gameType = DeckBrowserLocation.gameTypeFor(rootType, lstDecks.getGameType());
        for (final Deck deck : folder) {
            rows.add(DeckBrowserEntry.deck(deck, gameType, path, folder));
        }
    }

    private void addVirtualRowsForFolderRecursively(final List<DeckProxy> rows, final String path,
            final DeckType rootType, final IStorage<Deck> folder) {
        if (editorOnlyBrowser) {
            return;
        }

        final DeckType folderShortcutType = folder == null ? null : DeckBrowserLocation.shortcutDeckType(folder, isForCommander);
        final boolean isShortcutRoot = rootType != null && rootType == folderShortcutType;
        final boolean isArchiveRoot = DeckBrowserLocation.isSameFolder(folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
        if (StringUtils.isNotBlank(path) && !isShortcutRoot && !isArchiveRoot) {
            return;
        }
        if (rootType == DeckType.CUSTOM_DECK) {
            DeckBrowserGeneratedRows.addConstructedFolderRows(rows, path, true, lstDecks, lstDecks, isAi);
        } else if (rootType == DeckType.COMMANDER_DECK) {
            DeckBrowserGeneratedRows.addCommanderFolderRows(rows, path, true, lstDecks, lstDecks, isAi);
        } else if (rootType == DeckType.NET_DECK || rootType == DeckType.NET_COMMANDER_DECK) {
            netService.addMissingNetCategoryFolders(
                    rows, path, folder, lstDecks.getGameType(), false);
        } else if (isArchiveRoot) {
            netService.addNetArchiveVirtualFolders(rows, path);
        }
    }

    private void updateBrowserRoot(final DeckType deckType) {
        browser.rootType = deckType;
        browser.generatedFolder = false;
        browser.folder = DeckBrowserLocation.storageFor(deckType);
        final DeckType folderShortcut = browser.folder == null ? null : DeckBrowserLocation.shortcutDeckType(browser.folder, isForCommander);
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
        addDecksHomeRows(rows, false, false);
        browser.setListLocation(HOME_PATH);
        lstDecks.setCaption(localizer.getMessage("lblDecks"));
        updateDeckUrlPanelVisibility();
        displaySingleSelectBrowserRows(rows);
    }

    private void updateGeneratedGroup(final String path) {
        syncComboBoxForGeneratedGroup(path);
        final List<DeckProxy> rows = new ArrayList<>();
        rows.add(DeckBrowserEntry.parentFolder(DeckBrowserGeneratedRows.getGeneratedGroupParentPath(path),
                getGeneratedGroupParentFolder(path)));
        DeckBrowserGeneratedRows.addGeneratedGroupRows(rows, path, lstDecks, lstDecks, isAi, false);
        browser.setListLocation(path);
        displaySingleSelectBrowserRows(rows);
    }

    private IStorage<Deck> getGeneratedGroupParentFolder(final String path) {
        final DeckType parentRootType = DeckBrowserGeneratedRows.getGeneratedGroupParentRootType(path);
        return parentRootType == null ? null : DeckBrowserLocation.storageFor(parentRootType);
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

    private void setShortcutDeckType(final DeckType deckType) {
        if (deckType == null || decksComboBox == null) {
            return;
        }
        selectedDeckType = deckType;
        decksComboBox.setDisplayedDeckType(deckType);
        lstDecks.setCaption(deckType.toString());
        updateDeckUrlPanelVisibility();
    }

    private boolean isGeneratedOrListBrowserView() {
        return browser.generatedFolder || browser.hasListParent()
                || DeckBrowserGeneratedRows.isDeckOptionType(selectedDeckType);
    }

    private void rememberCurrentBrowserLocationForParentSelection() {
        if (browser.generatedFolder) {
            browser.rememberSelection(browser.generatedParentPath,
                    selectedDeckType == null ? null : selectedDeckType.toString(), selectedDeckType);
            return;
        }
        if (browser.hasListParent()) {
            browser.rememberSelection(browser.listParentPath,
                    selectedDeckType == null ? null : selectedDeckType.toString(), selectedDeckType);
            return;
        }
        if (browser.folder == null && StringUtils.isNotBlank(browser.generatedParentPath)) {
            browser.rememberSelection(browser.generatedParentPath,
                    DeckBrowserGeneratedRows.getGeneratedGroupDisplayName(browser.generatedParentPath));
            return;
        }
        if (StringUtils.isNotBlank(browser.path)) {
            browser.rememberSelection(browser.path, DeckBrowserLocation.lastSegment(browser.path));
        } else if (browser.folder != null) {
            browser.rememberSelection(null, browser.folder.getName(), DeckBrowserLocation.shortcutDeckType(browser.folder, isForCommander));
        }
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

    private void updateBrowserFolder() {
        final List<DeckProxy> rows = new ArrayList<>();
        if (browser.parentFolder != null || !StringUtils.isBlank(browser.path) || browser.rootType != null || browser.hasDecksHomeParent) {
            rows.add(DeckBrowserEntry.parentFolder(DeckBrowserLocation.parentPath(browser.path), browser.parentFolder));
        }
        if (browser.folder != null) {
            addFolderRows(rows, browser.folder, browser.path, browser.rootType, false);
            if (StringUtils.isBlank(browser.path) && !editorOnlyBrowser) {
                if (browser.rootType == DeckType.CUSTOM_DECK) {
                    DeckBrowserGeneratedRows.addConstructedFolderRows(rows, browser.path, false, lstDecks, lstDecks, isAi);
                } else if (browser.rootType == DeckType.COMMANDER_DECK) {
                    DeckBrowserGeneratedRows.addCommanderFolderRows(rows, browser.path, false, lstDecks, lstDecks, isAi);
                }
            }
            if (StringUtils.isBlank(browser.path) && !editorOnlyBrowser && isNetBrowserRoot()) {
                netService.addMissingNetCategoryFolders(
                        rows, browser.path, browser.folder, lstDecks.getGameType(), true);
            }
            if (isNetArchiveBrowserRoot()) {
                netService.addNetArchiveVirtualFolders(rows, browser.path);
            }
        }
        displaySingleSelectBrowserRows(rows);
    }

    private void displaySingleSelectBrowserRows(final List<DeckProxy> rows) {
        DeckBrowserEntry.sort(rows);
        lstDecks.setAllowMultipleSelections(false);
        final List<DeckProxy> displayedRows = setBrowserPoolAndSetup(rows);
        btnRandom.setText(localizer.getMessage("lblRandomDeck"));
        btnRandom.setCommand(this::randomSelectBrowserDeck);
        if (!selectPendingBrowserRow(displayedRows)) {
            lstDecks.clearSelection();
        }
        updateEditorSaveTarget();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void updateEditorSaveTarget() {
        if (!editorOnlyBrowser || browser.folder == null) {
            return;
        }
        final CDeckEditorUI editorUI = CDeckEditorUI.SINGLETON_INSTANCE;
        editorUI.updatePristineDeckGameType(
                DeckBrowserLocation.gameTypeFor(browser.rootType, lstDecks.getGameType()));
        final DeckController controller = editorUI.getCurrentEditorController() == null
                ? null : editorUI.getCurrentEditorController().getDeckController();
        if (controller != null) {
            controller.setCurrentFolder(browser.folder, browser.path);
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
                && DeckBrowserLocation.isSameFolder(browser.folder, ForgeConstants.DECK_NET_ARCHIVE_DIR);
    }

    private boolean isNetArchiveDeckType(final DeckType deckType) {
        return netService.isNetArchiveDeckType(deckType);
    }

    private void updateDeckOptions(final DeckType deckType) {
        final Iterable<DeckProxy> decks =
                DeckBrowserGeneratedRows.getDeckOptions(deckType, lstDecks, lstDecks, isAi);
        if (DeckBrowserGeneratedRows.isColorDeckType(deckType)) {
            updateBrowserOptions(decks, true, localizer.getMessage("lblRandomColors"),
                    this::randomSelectBrowserColors);
        } else if (DeckBrowserGeneratedRows.isArchetypeDeckType(deckType)) {
            updateBrowserOptions(decks, false, "Random",
                    this::randomSelectBrowserDeck);
        } else {
            updateDecks(decks);
        }
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

    private void selectLastImportedUrlDeckRow() {
        if (lastImportedUrlDeckName != null) {
            lstDecks.setSelectedString(lastImportedUrlDeckName);
        }
        syncUrlFieldWithSelectedDeck();
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
            initializeDeckUrlPanel();
            decksComboBox.addListener(this);
            if (editorOnlyBrowser) {
                updateDecksHome();
            } else {
                restoreSavedState();
            }
        } else {
            removeAll();
        }
        this.setLayout(new MigLayout("insets 0, gap 0, hidemode 3"));
        if (!editorOnlyBrowser) {
            decksComboBox.addTo(this, "w 100%, h 30px!, gapbottom 5px, spanx 2, wrap");
            this.add(pnlDeckUrl, "w 100%, h 30px!, gapbottom 5px, spanx 2, wrap");
        }
        this.add(lstDecksContainer, "w 100%, growy, pushy, spanx 2, wrap");
        if (!editorOnlyBrowser) {
            this.add(btnViewDeck, "w 50%-3px, h 30px!, gaptop 5px, gapright 6px");
            this.add(btnRandom, "w 50%-3px, h 30px!, gaptop 5px");
            updateDeckUrlPanelVisibility();
        }
        if (isShowing()) {
            revalidate();
            repaint();
        }
    }

    private void initializeDeckUrlPanel() {
        pnlDeckUrl = new JPanel(new MigLayout("insets 0, gap 0"));
        pnlDeckUrl.setOpaque(false);
        pnlDeckUrl.add(new FLabel.Builder().text(localizer.getMessage("lblDeckUrlLabel")).fontSize(12).fontStyle(Font.BOLD).build(),
                "h " + FTextField.HEIGHT + "px!, gapright 6px");
        txtDeckUrl = new FTextField.Builder().build();
        txtDeckUrl.addActionListener(e -> loadDeckFromUrl());
        pnlDeckUrl.add(txtDeckUrl, "growx, pushx, h " + FTextField.HEIGHT + "px!, gapright 6px");
        btnReloadUrl = new FLabel.ButtonBuilder().text(localizer.getMessage("lblReload")).fontSize(14).build();
        btnReloadUrl.setCommand(this::loadDeckFromUrl);
        pnlDeckUrl.add(btnReloadUrl, "h " + FTextField.HEIGHT + "px!, w pref!");
    }

    private void updateDeckUrlPanelVisibility() {
        if (pnlDeckUrl != null) {
            pnlDeckUrl.setVisible(browser.rootType == DeckType.PROVIDED_DECK_URL);
        }
    }

    private void syncUrlFieldWithSelectedDeck() {
        if (txtDeckUrl == null || selectedDeckType != DeckType.PROVIDED_DECK_URL) {
            return;
        }
        final DeckProxy selected = getSelectedDeckProxy();
        if (selected != null) {
            txtDeckUrl.setText(selected.getSourceUrl());
        }
    }

    private void handleDeckSelection() {
        if (updatingDeckPool) {
            return;
        }
        syncUrlFieldWithSelectedDeck();
        if (hasPlayableSelection() && deckSelectionCommand != null) {
            deckSelectionCommand.run();
        }
    }

    private void loadDeckFromUrl() {
        if (txtDeckUrl == null) {
            return;
        }
        final String deckUrl = txtDeckUrl.getText().trim();
        if (deckUrl.isEmpty()) {
            return;
        }

        setDeckUrlLoading(true);
        FThreads.invokeInBackgroundThread(() -> {
            try {
                final DeckProxy deck = DeckUrlLoader.load(deckUrl);
                FThreads.invokeInEdtLater(() -> {
                    lastImportedUrlDeckName = deck.toString();
                    if (selectedDeckType == DeckType.PROVIDED_DECK_URL) {
                        refreshDecksList(DeckType.PROVIDED_DECK_URL, true, null);
                    }
                    setDeckUrlLoading(false);
                });
            } catch (final IOException ex) {
                FThreads.invokeInEdtLater(() -> {
                    setDeckUrlLoading(false);
                    FOptionPane.showErrorDialog(ex.getMessage(), localizer.getMessage("lblUnableToLoadDeckUrl"));
                });
            }
        });
    }

    private void setDeckUrlLoading(final boolean loading) {
        txtDeckUrl.setEnabled(!loading);
        btnReloadUrl.setEnabled(!loading);
        btnReloadUrl.setText(localizer.getMessage(loading ? "lblLoadingEllipsis" : "lblReload"));
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
            updateDeckUrlPanelVisibility();
            return;
        } else if ((ev.getDeckType() == DeckType.NET_DECK || ev.getDeckType() == DeckType.NET_COMMANDER_DECK) && !refreshingDeckType) {
            refreshDecksList(ev.getDeckType(), true, ev);
            updateDeckUrlPanelVisibility();
            return;
        }
        refreshDecksList(ev.getDeckType(), false, ev);
        updateDeckUrlPanelVisibility();
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
        final DeckProxy currentDeck = lstDecks.getSelectedItem();
        final String currentName = currentDeck == null ? null : currentDeck.getName();

        final UiCommand selectCmd = lstDecks.getSelectCommand();
        // ignore selection changes while refreshing to avoid repeating some deck generator calls
        lstDecks.setSelectCommand(null);

        refreshDecksList(selectedDeckType, true, null);

        if (currentName != null) {
            lstDecks.setSelectedString(currentName);
        }

        lstDecks.setSelectCommand(selectCmd);
        lstDecks.refresh();

        saveState();
    }

    private void setBrowserListParentRoot(final DeckType rootType) {
        browser.listParentRootType = rootType;
        browser.listParentFolder = DeckBrowserLocation.storageFor(rootType);
        browser.listParentPath = "";
        browser.listParentHasDecksHomeParent = true;
        browser.rootType = rootType;
        browser.generatedFolder = false;
    }

    private void refreshDecksList(final DeckType deckType, final boolean forceRefresh, final DecksComboBoxEvent ev) {
        if (decksComboBox == null) { return; } // Not yet populated
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;
        final DeckType listParentRootType = DeckBrowserGeneratedRows.getListParentRootType(deckType);
        if (DeckBrowserGeneratedRows.isCommanderGeneratedDeckType(deckType)) {
            setBrowserListParentRoot(listParentRootType);
        } else if (DeckBrowserGeneratedRows.isGeneratedDeckType(deckType)) {
            if (!browser.generatedFolder) {
                browser.generatedParentPath = DeckBrowserGeneratedRows.getDefaultGeneratedParentPath(deckType);
            }
            browser.rootType = DeckBrowserGeneratedRows.isCommanderGeneratedDeckType(deckType) ? DeckType.COMMANDER_DECK : DeckType.CUSTOM_DECK;
            browser.generatedFolder = true;
            browser.clearListParent();
        } else if (listParentRootType != null && !browser.hasListParent()) {
            setBrowserListParentRoot(listParentRootType);
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
        if (DeckBrowserGeneratedRows.isDeckOptionType(deckType)) {
            updateDeckOptions(deckType);
            return;
        }

        switch (deckType) {
        case CUSTOM_DECK, COMMANDER_DECK, OATHBREAKER_DECK, TINY_LEADERS_DECK, BRAWL_DECK ->
                updateBrowserRoot(selectedDeckType);
        case RANDOM_DECK -> updateGeneratedGroup(RANDOM_PATH);
        case NET_DECK, NET_COMMANDER_DECK, PROVIDED_DECK_URL -> {
            updateBrowserRoot(deckType);
            if (deckType == DeckType.PROVIDED_DECK_URL) {
                selectLastImportedUrlDeckRow();
            }
        }
        case NET_EVENT_DECK -> updateNetEventDecks();
        default -> { } // other deck types not currently supported here
        }
    }

    private static final String SELECTED_DECK_DELIMITER = "::";

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
        for (final DeckProxy deck : selectedDecks) {
            if (isFirst) {
                isFirst = false;
            } else {
                state.append(delimiter);
            }
            state.append(deck);
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
        final String deckName = DeckBrowserLocation.lastSegment(selectedDecks.get(0));
        if (DeckBrowserGeneratedRows.isGeneratedDeckType(deckType)) {
            browser.rememberSelection(DeckBrowserGeneratedRows.getDefaultGeneratedParentPath(deckType), deckName, deckType);
        } else if (DeckBrowserGeneratedRows.getListParentRootType(deckType) != null) {
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
            final String selectedName = DeckBrowserLocation.lastSegment(selectedDeck);
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

    private List<String> getSelectedDecksFromSavedState(final String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return List.of();
            }
            final String[] parts = savedState.split(";", -1);
            return Arrays.asList(parts[1].split(SELECTED_DECK_DELIMITER));
        } catch (final Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return List.of();
        }
    }

    public DecksComboBox getDecksComboBox() {
        return decksComboBox;
    }
}

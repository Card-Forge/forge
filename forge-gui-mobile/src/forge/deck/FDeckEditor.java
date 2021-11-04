package forge.deck;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.assets.*;
import forge.card.CardEdition;
import forge.deck.io.DeckPreferences;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.FThreads;
import forge.gui.card.CardPreferences;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.TabPageScreen;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.*;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    public static FSkinImage MAIN_DECK_ICON = Forge.hdbuttons ? FSkinImage.HDLIBRARY :FSkinImage.DECKLIST;
    public static FSkinImage SIDEBOARD_ICON = Forge.hdbuttons ? FSkinImage.HDSIDEBOARD : FSkinImage.FLASHBACK;
    private static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);

    public enum EditorType {
        Constructed(new DeckController<>(FModel.getDecks().getConstructed(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        Draft(new DeckController<>(FModel.getDecks().getDraft(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        }), null),
        Sealed(new DeckController<>(FModel.getDecks().getSealed(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        }), null),
        Winston(new DeckController<>(FModel.getDecks().getWinston(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        }), null),
        Commander(new DeckController<>(FModel.getDecks().getCommander(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        Oathbreaker(new DeckController<>(FModel.getDecks().getOathbreaker(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        TinyLeaders(new DeckController<>(FModel.getDecks().getTinyLeaders(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), DeckFormat.TinyLeaders.isLegalCardPredicate()),
        Brawl(new DeckController<>(FModel.getDecks().getBrawl(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), DeckFormat.Brawl.isLegalCardPredicate()),
        Archenemy(new DeckController<>(FModel.getDecks().getScheme(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        Planechase(new DeckController<>(FModel.getDecks().getPlane(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        Quest(new DeckController<>(null, new Supplier<Deck>() { //delay setting root folder until quest loaded
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        QuestCommander(new DeckController<>(null, new Supplier<Deck>() { //delay setting root folder until quest loaded
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null),
        QuestDraft(new DeckController<>(null, new Supplier<DeckGroup>() { //delay setting root folder until quest loaded
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        }), null),
        PlanarConquest(new DeckController<>(null, new Supplier<Deck>() { //delay setting root folder until conquest loaded
            @Override
            public Deck get() {
                return new Deck();
            }
        }), null);

        private final DeckController<? extends DeckBase> controller;
        private final Predicate<PaperCard> cardFilter;

        public DeckController<? extends DeckBase> getController() {
            return controller;
        }

        EditorType(DeckController<? extends DeckBase> controller0, Predicate<PaperCard> cardFilter0) {
            controller = controller0;
            cardFilter = cardFilter0;
        }

        private ItemPool<PaperCard> applyCardFilter(ItemPool<PaperCard> cardPool, Predicate<PaperCard> additionalFilter) {
            Predicate<PaperCard> filter = cardFilter;
            if (filter == null) {
                filter = additionalFilter;
                if (filter == null) {
                    return cardPool;
                }
            }
            else if (additionalFilter != null) {
                filter = Predicates.and(filter, additionalFilter);
            }

            ItemPool<PaperCard> filteredPool = new ItemPool<>(PaperCard.class);
            for (Entry<PaperCard, Integer> entry : cardPool) {
                if (filter.apply(entry.getKey())) {
                    filteredPool.add(entry.getKey(), entry.getValue());
                }
            }
            return filteredPool;
        }
    }

    private static DeckEditorPage[] getPages(EditorType editorType) {
        final Localizer localizer = Localizer.getInstance();
        boolean isLandscape = Forge.isLandscapeMode();
        switch (editorType) {
        default:
        case Constructed:
            return new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            };
        case Draft:
        case QuestDraft:
            return new DeckEditorPage[] {
                    new DraftPackPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.DRAFT_POOL)
            };
        case Sealed:
            return new DeckEditorPage[] {
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SEALED_POOL)
            };
        case Commander:
        case TinyLeaders:
        case Brawl:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Sideboard)
            };
        case Oathbreaker:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.OATHBREAKER_SECTION, localizer.getMessage("lblOathbreaker"), FSkinImage.COMMANDER),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.OATHBREAKER_SECTION, localizer.getMessage("lblOathbreaker"), FSkinImage.COMMANDER),
                    new DeckSectionPage(DeckSection.Sideboard)
            };
        case Archenemy:
            return new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.SCHEME_POOL),
                    new DeckSectionPage(DeckSection.Schemes, ItemManagerConfig.SCHEME_DECK_EDITOR)
            };
        case Planechase:
            return new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.PLANAR_POOL),
                    new DeckSectionPage(DeckSection.Planes, ItemManagerConfig.PLANAR_DECK_EDITOR)
            };
        case Quest:
            return new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, localizer.getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.QUEST_DECK_EDITOR)
            };
        case QuestCommander:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, localizer.getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, localizer.getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION)
            };
        case PlanarConquest:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CONQUEST_COLLECTION, localizer.getMessage("lblCollection"), FSkinImage.SPELLBOOK),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.CONQUEST_DECK_EDITOR, localizer.getMessage("lblDeck"), Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CONQUEST_COLLECTION, localizer.getMessage("lblCollection"), FSkinImage.SPELLBOOK),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.CONQUEST_DECK_EDITOR, localizer.getMessage("lblDeck"), Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION)
            };
        }
    }

    private final EditorType editorType;
    private Deck deck;
    private CatalogPage catalogPage;
    private DeckSectionPage mainDeckPage;
    private DeckSectionPage sideboardPage;
    private DeckSectionPage commanderPage;
    private FEventHandler saveHandler;

    protected final DeckHeader deckHeader = add(new DeckHeader());
    protected final FLabel lblName = deckHeader.add(new FLabel.Builder().font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build());
    private final FLabel btnSave = deckHeader.add(new FLabel.Builder().icon(Forge.hdbuttons ? FSkinImage.HDSAVE : FSkinImage.SAVE).align(Align.center).pressedColor(Header.BTN_PRESSED_COLOR).build());
    private final FLabel btnMoreOptions = deckHeader.add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(Align.center).pressedColor(Header.BTN_PRESSED_COLOR).build());

    public FDeckEditor(EditorType editorType0, DeckProxy editDeck, boolean showMainDeck) {
        this(editorType0, editDeck.getName(), editDeck.getPath(), null, showMainDeck,null);
    }
    public FDeckEditor(EditorType editorType0, String editDeckName, boolean showMainDeck,FEventHandler backButton) {
        this(editorType0, editDeckName, "", null, showMainDeck,backButton);
    }
    public FDeckEditor(EditorType editorType0, String editDeckName, boolean showMainDeck) {
        this(editorType0, editDeckName, "", null, showMainDeck,null);
    }
    public FDeckEditor(EditorType editorType0, Deck newDeck, boolean showMainDeck) {
        this(editorType0, "", "", newDeck, showMainDeck,null);
    }
    private FDeckEditor(EditorType editorType0, String editDeckName, String editDeckPath, Deck newDeck, boolean showMainDeck,FEventHandler backButton) {
        super(backButton,getPages(editorType0));

        if (editorType0 == EditorType.QuestCommander) //fix saving quest commander
            editorType = EditorType.Quest;
        else
            editorType = editorType0;

        editorType.getController().editor = this;

        //cache specific pages
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            if (tabPage instanceof CatalogPage) {
                catalogPage = (CatalogPage) tabPage;
            }
            else if (tabPage instanceof DeckSectionPage) {
                DeckSectionPage deckSectionPage = (DeckSectionPage) tabPage;
                switch (deckSectionPage.deckSection) {
                case Main:
                case Schemes:
                case Planes:
                    mainDeckPage = deckSectionPage;
                    break;
                case Sideboard:
                    sideboardPage = deckSectionPage;
                    break;
                case Commander:
                    commanderPage = deckSectionPage;
                    break;
                default:
                    break;
                }
            }
        }

        switch (editorType) {
        case Sealed:
            //if opening brand new sealed deck, show sideboard (card pool) by default
            if (!showMainDeck) {
                setSelectedPage(sideboardPage);
            }
            break;
        case Draft:
        case QuestDraft:
            break;
        default:
            //if editing existing non-limited deck, show main deck by default
            if (showMainDeck) {
                setSelectedPage(mainDeckPage);
            }
            break;
        }

        if (StringUtils.isEmpty(editDeckName)) {
            if (editorType == EditorType.Draft || editorType == EditorType.QuestDraft) {
                //hide deck header on while drafting
                setDeck(new Deck());
                deckHeader.setVisible(false);
            } else {
                if (newDeck == null) {
                    editorType.getController().newModel();
                } else {
                    editorType.getController().setDeck(newDeck);
                }
            }
        } else {
            if (editorType == EditorType.Draft || editorType == EditorType.QuestDraft) {
                tabPages[0].hideTab(); //hide Draft Pack page if editing existing draft deck
            }
            editorType.getController().load(editDeckPath, editDeckName);
        }

        btnSave.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                save(null);
            }
        });
        btnMoreOptions.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FPopupMenu menu = new FPopupMenu() {
                    @Override
                    protected void buildMenu() {
                        final Localizer localizer = Localizer.getInstance();

                        addItem(new FMenuItem(localizer.getMessage("lblAddBasicLands"), FSkinImage.LANDLOGO, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                CardEdition defaultLandSet;
                                switch (editorType) {
                                case Draft:
                                case Sealed:
                                case QuestDraft:
                                    //suggest a random set from the ones used in the limited card pool that have all basic lands
                                    Set<CardEdition> availableEditionCodes = new HashSet<>();
                                    for (PaperCard p : deck.getAllCardsInASinglePool().toFlatList()) {
                                        availableEditionCodes.add(FModel.getMagicDb().getEditions().get(p.getEdition()));
                                    }
                                    defaultLandSet = CardEdition.Predicates.getRandomSetWithAllBasicLands(availableEditionCodes);
                                    break;
                                case Quest:
                                    defaultLandSet = FModel.getQuest().getDefaultLandSet();
                                    break;
                                default:
                                    defaultLandSet = DeckProxy.getDefaultLandSet(deck);
                                    break;
                                }
                                AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck, defaultLandSet, new Callback<CardPool>() {
                                    @Override
                                    public void run(CardPool landsToAdd) {
                                        getMainDeckPage().addCards(landsToAdd);
                                    }
                                });
                                dialog.show();
                                setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck is visible below dialog
                            }
                        }));
                        if (!isLimitedEditor()) {
                            addItem(new FMenuItem(localizer.getMessage("lblImportFromClipboard"), Forge.hdbuttons ? FSkinImage.HDIMPORT : FSkinImage.OPEN, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    FDeckImportDialog dialog = new FDeckImportDialog(!deck.isEmpty(), editorType);
                                    dialog.setCallback(new Callback<Deck>() {
                                        @Override
                                        public void run(Deck importedDeck) {
                                            if (deck != null && importedDeck.hasName()) {
                                                deck.setName(importedDeck.getName());
                                                lblName.setText(importedDeck.getName());
                                            }
                                            if (dialog.createNewDeck()) {
                                                getMainDeckPage().setCards(importedDeck.getMain());
                                                if (getSideboardPage() != null)
                                                    getSideboardPage().setCards(importedDeck.getOrCreate(DeckSection.Sideboard));
                                                if (getCommanderPage() != null)
                                                    getCommanderPage().setCards(importedDeck.getOrCreate(DeckSection.Commander));
                                            } else {
                                                getMainDeckPage().addCards(importedDeck.getMain());
                                                if (getSideboardPage() != null)
                                                    getSideboardPage().addCards(importedDeck.getOrCreate(DeckSection.Sideboard));
                                                if (getCommanderPage() != null)
                                                    getCommanderPage().addCards(importedDeck.getOrCreate(DeckSection.Commander));
                                            }
                                        }
                                    });
                                    dialog.show();
                                    setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck if visible below dialog
                                }
                            }));
                            addItem(new FMenuItem(localizer.getMessage("lblSaveAs"), Forge.hdbuttons ? FSkinImage.HDSAVEAS : FSkinImage.SAVEAS, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    String defaultName = editorType.getController().getNextAvailableName();
                                    FOptionPane.showInputDialog(localizer.getMessage("lblNameNewCopyDeck"), defaultName, new Callback<String>() {
                                        @Override
                                        public void run(String result) {
                                            if (!StringUtils.isEmpty(result)) {
                                                editorType.getController().saveAs(result);
                                            }
                                        }
                                    });
                                }
                            }));
                        }
                        if (allowRename()) {
                            addItem(new FMenuItem(localizer.getMessage("lblRenameDeck"), Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    FOptionPane.showInputDialog(localizer.getMessage("lblNewNameDeck"), deck.getName(), new Callback<String>() {
                                        @Override
                                        public void run(String result) {
                                            editorType.getController().rename(result);
                                        }
                                    });
                                }
                            }));
                        }
                        if (allowDelete()) {
                            addItem(new FMenuItem(localizer.getMessage("lblDeleteDeck"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    FOptionPane.showConfirmDialog(
                                            localizer.getMessage("lblConfirmDelete") + " '" + deck.getName() + "'?",
                                            localizer.getMessage("lblDeleteDeck"), localizer.getMessage("lblDelete"), localizer.getMessage("lblCancel"), false, new Callback<Boolean>() {
                                                @Override
                                                public void run(Boolean result) {
                                                    if (result) {
                                                        editorType.getController().delete();
                                                        Forge.back();
                                                    }
                                                }
                                            });
                                }
                            }));
                        }
                        addItem(new FMenuItem(localizer.getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                FDeckViewer.copyDeckToClipboard(deck);
                            }
                        }));
                        ((DeckEditorPage)getSelectedPage()).buildDeckMenu(this);
                    }
                };
                menu.show(btnMoreOptions, 0, btnMoreOptions.getHeight());
            }
        });
    }

    protected boolean allowRename() {
        return true;
    }
    protected boolean allowDelete() {
        return true;
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        if (deckHeader.isVisible()) {
            deckHeader.setBounds(0, startY, width, HEADER_HEIGHT);
            startY += HEADER_HEIGHT;
        }
        super.doLayout(startY, width, height);
    }

    public EditorType getEditorType() {
        return editorType;
    }

    public Deck getDeck() {
        return deck;
    }
    public void setDeck(Deck deck0) {
        if (deck == deck0) { return; }
        deck = deck0;
        if (deck == null) { return; }

        //reinitialize tab pages when deck changes
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            ((DeckEditorPage)tabPage).initialize();
        }
    }

    protected CatalogPage getCatalogPage() {
        return catalogPage;
    }

    protected DeckSectionPage getMainDeckPage() {
        return mainDeckPage;
    }

    protected DeckSectionPage getSideboardPage() {
        return sideboardPage;
    }

    protected DeckSectionPage getCommanderPage() {
        return commanderPage;
    }

    protected BoosterDraft getDraft() {
        return null;
    }

    private enum CardLimit {
        Singleton,
        Default,
        None
    }
    private CardLimit getCardLimit() {
        switch (editorType) {
        case Constructed:
        case Planechase:
        case Archenemy:
        case Quest:
        default:
            if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
                return CardLimit.Default;
            }
            return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
        case Draft:
        case Sealed:
        case Winston:
        case QuestDraft:
            return CardLimit.None;
        case Commander:
        case QuestCommander:
        case Oathbreaker:
        case TinyLeaders:
        case Brawl:
        case PlanarConquest:
            return CardLimit.Singleton;
        }
    }

    public void setSaveHandler(FEventHandler saveHandler0) {
        saveHandler = saveHandler0;
    }

    protected void save(final Callback<Boolean> callback) {
        if (StringUtils.isEmpty(deck.getName())) {
            List<PaperCard> commanders = deck.getCommanders(); //use commander name as default deck name
            String initialInput = Lang.joinHomogenous(commanders);
            final Localizer localizer = Localizer.getInstance();
            FOptionPane.showInputDialog(localizer.getMessage("lblNameNewDeck"), initialInput, new Callback<String>() {
                @Override
                public void run(String result) {
                    if (StringUtils.isEmpty(result)) { return; }

                    editorType.getController().saveAs(result);
                    if (callback != null) {
                        callback.run(true);
                    }
                }
            });
            return;
        }

        editorType.getController().save();
        if (callback != null) {
            callback.run(true);
        }
    }

    private final static ImmutableList<String> onCloseOptions = ImmutableList.of(
        Localizer.getInstance().getMessage("lblSave"),
        Localizer.getInstance().getMessage("lblDontSave"),
        Localizer.getInstance().getMessage("lblCancel")
    );

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        if (editorType.getController().isSaved() || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        final Localizer localizer = Localizer.getInstance();
        FOptionPane.showOptionDialog(localizer.getMessage("lblSaveChangesCurrentDeck"), "",
                FOptionPane.QUESTION_ICON, onCloseOptions, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == 0) {
                            save(canCloseCallback);
                        }
                        else if (result == 1) {
                            editorType.getController().reload(); //reload if not saving changes
                            canCloseCallback.run(true);
                        }
                        else {
                            canCloseCallback.run(false);
                        }
                    }
        });
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.BACK:
            return true; //suppress Back button so it's not bumped while editing deck
        case Keys.S: //save deck on Ctrl+S
            if (KeyInputAdapter.isCtrlKeyDown()) {
                save(null);
                return true;
            }
            break;
        }
        return super.keyDown(keyCode);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null; //never use backdrop for editor
    }

    private boolean isLimitedEditor() {
        switch (editorType) {
        case Draft:
        case Sealed:
        case Winston:
        case QuestDraft:
            return true;
        default:
            return false;
        }
    }

    public static boolean allowsReplacement(final EditorType editorType){
        switch (editorType) {
            case Constructed:
            case Commander:
            case Oathbreaker:
            case TinyLeaders:
            case Brawl:
                return true;
            default:
                return false;
        }
    }

    private boolean isAllowedReplacement() {
        return allowsReplacement(editorType);
    }

    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        return null;
    }

    protected class DeckHeader extends FContainer {
        private DeckHeader() {
            setHeight(HEADER_HEIGHT);
        }

        @Override
        public void drawBackground(Graphics g) {
            g.fillRect(Header.BACK_COLOR, 0, 0, getWidth(), HEADER_HEIGHT);
        }

        @Override
        public void drawOverlay(Graphics g) {
            float y = HEADER_HEIGHT - Header.LINE_THICKNESS / 2;
            g.drawLine(Header.LINE_THICKNESS, Header.LINE_COLOR, 0, y, getWidth(), y);
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = 0;
            lblName.setBounds(0, 0, width - 2 * height, height);
            x += lblName.getWidth();
            btnSave.setBounds(x, 0, height, height);
            x += height;
            btnMoreOptions.setBounds(x, 0, height, height);
        }
    }

    protected static abstract class DeckEditorPage extends TabPage<FDeckEditor> {
        protected DeckEditorPage(String caption0, FImage icon0) {
            super(caption0, icon0);
        }

        protected void buildDeckMenu(FPopupMenu menu) {
        }

        protected abstract void initialize();

        @Override
        public boolean fling(float velocityX, float velocityY) {
            return false; //prevent left/right swipe to change tabs since it doesn't play nice with item managers
        }
    }

    protected static abstract class CardManagerPage extends DeckEditorPage {
        private final ItemManagerConfig config;
        protected final CardManager cardManager = add(new CardManager(false));

        protected CardManagerPage(ItemManagerConfig config0, String caption0, FImage icon0) {
            super(caption0, icon0);
            config = config0;
            cardManager.setItemActivateHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    onCardActivated(cardManager.getSelectedItem());
                }
            });
            cardManager.setContextMenuBuilder(new ContextMenuBuilder<PaperCard>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                    CardManagerPage.this.buildMenu(menu, card);
                }
            });
        }

        protected void initialize() {
            cardManager.setup(config, parentScreen.getColOverrides(config));
        }

        protected boolean canAddCards() {
            return true;
        }

        public void addCard(PaperCard card) {
            addCard(card, 1);
        }
        public void addCard(PaperCard card, int qty) {
            if (canAddCards()) {
                cardManager.addItem(card, qty);
                parentScreen.getEditorType().getController().notifyModelChanged();
                updateCaption();
            }
        }

        public void addCards(Iterable<Entry<PaperCard, Integer>> cards) {
            if (canAddCards()) {
                cardManager.addItems(cards);
                parentScreen.getEditorType().getController().notifyModelChanged();
                updateCaption();
            }
        }

        public void removeCard(PaperCard card) {
            removeCard(card, 1);
        }
        public void removeCard(PaperCard card, int qty) {
            cardManager.removeItem(card, qty);
            parentScreen.getEditorType().getController().notifyModelChanged();
            updateCaption();
        }

        public void setCards(CardPool cards) {
            cardManager.setItems(cards);
            parentScreen.getEditorType().getController().notifyModelChanged();
            updateCaption();
        }

        protected void updateCaption() {
        }

        protected abstract void onCardActivated(PaperCard card);
        protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

        private ItemPool<PaperCard> getAllowedAdditions(Iterable<Entry<PaperCard, Integer>> itemsToAdd, boolean isAddSource) {
            ItemPool<PaperCard> additions = new ItemPool<>(cardManager.getGenericType());
            CardLimit limit = parentScreen.getCardLimit();
            Deck deck = parentScreen.getDeck();

            for (Entry<PaperCard, Integer> itemEntry : itemsToAdd) {
                PaperCard card = itemEntry.getKey();

                int max;
                if (deck == null || card == null) {
                    max = Integer.MAX_VALUE;
                }
                else if (limit == CardLimit.None || DeckFormat.canHaveAnyNumberOf(card)) {
                    max = Integer.MAX_VALUE;
                    if (parentScreen.isLimitedEditor() && !isAddSource) {
                        //prevent adding more than is in other pool when editing limited decks
                        if (parentScreen.getMainDeckPage() == this) {
                            max = deck.get(DeckSection.Sideboard).count(card);
                        }
                        else if (parentScreen.getSideboardPage() == this) {
                            max = deck.get(DeckSection.Main).count(card);
                        }
                    }
                }
                else {
                    max = (limit == CardLimit.Singleton ? 1 : FModel.getPreferences().getPrefInt(FPref.DECK_DEFAULT_CARD_LIMIT));

                    Integer cardCopies = DeckFormat.canHaveSpecificNumberInDeck(card);
                    if (cardCopies != null) {
                        max = cardCopies;
                    }

                    max -= deck.getMain().count(card);
                    if (deck.has(DeckSection.Sideboard)) {
                        max -= deck.get(DeckSection.Sideboard).count(card);
                    }
                    if (deck.has(DeckSection.Commander)) {
                        max -= deck.get(DeckSection.Commander).count(card);
                    }
                    if (deck.has(DeckSection.Planes)) {
                        max -= deck.get(DeckSection.Planes).count(card);
                    }
                    if (deck.has(DeckSection.Schemes)) {
                        max -= deck.get(DeckSection.Schemes).count(card);
                    }
                }

                int qty;
                if (isAddSource) {
                    qty = itemEntry.getValue();
                }
                else if (parentScreen.getEditorType() == EditorType.Quest||parentScreen.getEditorType() == EditorType.QuestCommander) {
                    //prevent adding more than is in quest inventory
                    try {
                        qty = parentScreen.getCatalogPage().cardManager.getItemCount(card);
                    } catch (Exception e) {
                        //prevent NPE
                        qty = 0;
                    }
                }
                else {
                    //if not source of items being added, use max directly if unlimited pool
                    qty = max;
                }
                if (qty > max) {
                    qty = max;
                }
                if (qty > 0) {
                    additions.add(card, qty);
                }
            }

            return additions;
        }

        protected int getMaxMoveQuantity(boolean isAddMenu, boolean isAddSource) {
            ItemPool<PaperCard> selectedItemPool = cardManager.getSelectedItemPool();
            if (isAddMenu) {
                selectedItemPool = getAllowedAdditions(selectedItemPool, isAddSource);
            }
            if (selectedItemPool.isEmpty()) {
                return 0;
            }
            int max = Integer.MAX_VALUE;
            for (Entry<PaperCard, Integer> itemEntry : selectedItemPool) {
                if (itemEntry.getValue() < max) {
                    max = itemEntry.getValue();
                }
            }
            return max;
        }

        protected void addItem(FDropDownMenu menu, final String verb, String dest, FImage icon, boolean isAddMenu, boolean isAddSource, final Callback<Integer> callback) {
            final int max = getMaxMoveQuantity(isAddMenu, isAddSource);
            if (max == 0) { return; }

            String label = verb;
            if (!StringUtils.isEmpty(dest)) {
                label += " " + dest;
            }
            menu.addItem(new FMenuItem(label, icon, new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (max == 1) {
                        callback.run(max);
                    } else {
                        final Localizer localizer = Localizer.getInstance();
                        GuiChoose.getInteger(cardManager.getSelectedItem() + " - " + verb + " " + localizer.getMessage("lblHowMany"), 1, max, 20, callback);
                    }
                }
            }));
        }

        protected void addCommanderItems(final FDropDownMenu menu, final PaperCard card, boolean isAddMenu, boolean isAddSource) {
            final Localizer localizer = Localizer.getInstance();
            if (parentScreen.getCommanderPage() == null) {
                return;
            }
            boolean isLegalCommander;
            String captionSuffix = localizer.getMessage("lblCommander");
            switch (parentScreen.editorType) {
            case Brawl:
                isLegalCommander = card.getRules().canBeBrawlCommander();
                break;
            case TinyLeaders:
                isLegalCommander = card.getRules().canBeTinyLeadersCommander();
                break;
            case Oathbreaker:
                isLegalCommander = card.getRules().canBeOathbreaker();
                captionSuffix = localizer.getMessage("lblOathbreaker");
                break;
            case PlanarConquest:
                isLegalCommander = false; //don't set commander this way in Planar Conquest
                break;
            default:
                isLegalCommander = DeckFormat.Commander.isLegalCommander(card.getRules());
                break;
            }
            if (isLegalCommander && !parentScreen.getCommanderPage().cardManager.getPool().contains(card)) {
                addItem(menu, "Set", "as " + captionSuffix, parentScreen.getCommanderPage().getIcon(), isAddMenu, isAddSource, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }
                        setCommander(card);
                    }
                });
            }
            if (canHavePartnerCommander() && card.getRules().canBePartnerCommander()) {
                addItem(menu, "Set", "as Partner " + captionSuffix, parentScreen.getCommanderPage().getIcon(), isAddMenu, isAddSource, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }
                        setPartnerCommander(card);
                    }
                });
            }
            if (canHaveSignatureSpell() && card.getRules().canBeSignatureSpell()) {
                addItem(menu, "Set", "as Signature Spell", FSkinImage.SORCERY, isAddMenu, isAddSource, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }
                        setSignatureSpell(card);
                    }
                });
            }
        }

        protected boolean needsCommander() {
            return parentScreen.getCommanderPage() != null && parentScreen.getDeck().getCommanders().isEmpty();
        }

        protected boolean canHavePartnerCommander() {
            if (parentScreen.editorType == EditorType.Oathbreaker) {
                return false; //at least for now, simplify Oathbreaker by not supporting partners, since there's only one set of partner planeswalkers anyway
            }
            return parentScreen.getCommanderPage() != null && parentScreen.getDeck().getCommanders().size() == 1
                    && parentScreen.getDeck().getCommanders().get(0).getRules().canBePartnerCommander();
        }

        protected boolean canOnlyBePartnerCommander(final PaperCard card) {
            if (parentScreen.getCommanderPage() == null) {
                return false;
            }

            byte cmdCI = 0;
            for (final PaperCard p : parentScreen.getDeck().getCommanders()) {
                cmdCI |= p.getRules().getColorIdentity().getColor();
            }

            return !card.getRules().getColorIdentity().hasNoColorsExcept(cmdCI);
        }

        protected boolean canHaveSignatureSpell() {
            return parentScreen.editorType == EditorType.Oathbreaker && parentScreen.getDeck().getOathbreaker() != null;
        }

        protected void setCommander(PaperCard card) {
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            CardPool newPool = new CardPool();
            newPool.add(card);
            parentScreen.getCommanderPage().setCards(newPool);
            refresh(); //refresh so cards shown that match commander's color identity
        }

        protected void setPartnerCommander(PaperCard card) {
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            parentScreen.getCommanderPage().addCard(card);
            refresh(); //refresh so cards shown that match commander's color identity
        }

        protected void setSignatureSpell(PaperCard card) {
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            PaperCard signatureSpell = parentScreen.getDeck().getSignatureSpell();
            if (signatureSpell != null) {
                parentScreen.getCommanderPage().removeCard(signatureSpell); //remove existing signature spell if any
            }
            parentScreen.getCommanderPage().addCard(card);
            //refreshing isn't needed since color identity won't change from signature spell
        }

        public void refresh() {
            //not needed by default
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = 0;
            if (Forge.isLandscapeMode()) { //add some horizontal padding in landscape mode
                x = ItemFilter.PADDING;
                width -= 2 * x;
            }
            cardManager.setBounds(x, 0, width, height);
        }
    }

    protected static class CatalogPage extends CardManagerPage {
        private boolean initialized, needRefreshWhenShown;

        protected CatalogPage(ItemManagerConfig config) {
            this(config, Localizer.getInstance().getMessage("lblCatalog"), Forge.hdbuttons ? FSkinImage.HDFOLDER : FSkinImage.FOLDER);
        }
        protected CatalogPage(ItemManagerConfig config, String caption0, FImage icon0) {
            super(config, caption0, icon0);
        }

        @Override
        protected void initialize() {
            if (initialized) { return; } //prevent initializing more than once if deck changes
            initialized = true;

            super.initialize();
            cardManager.setCaption(getItemManagerCaption());

            if (!isVisible() && (parentScreen.getEditorType() != EditorType.Quest||parentScreen.getEditorType() != EditorType.QuestCommander)) {
                needRefreshWhenShown = true;
                return; //delay refreshing while hidden unless for quest inventory
            }
            refresh();
        }

        @Override
        protected boolean canAddCards() {
            if (needRefreshWhenShown) { //ensure refreshed before cards added if hasn't been refreshed yet
                needRefreshWhenShown = false;
                refresh();
            }
            return !cardManager.isInfinite();
        }

        protected String getItemManagerCaption() {
            final Localizer localizer = Localizer.getInstance();
            switch (parentScreen.getEditorType()) {
            case Archenemy:
                return localizer.getMessage("lblSchemes");
            case Planechase:
                return localizer.getMessage("lblPlanes");
            default:
                return localizer.getMessage("lblCards");
            }
        }

        @Override
        public void setVisible(boolean visible0) {
            if (isVisible() == visible0) { return; }

            super.setVisible(visible0);
            if (visible0 && needRefreshWhenShown) {
                needRefreshWhenShown = false;
                refresh();
            }
        }

        @Override
        public void refresh() {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    LoadingOverlay.show(Localizer.getInstance().getMessage("lblLoading"), new Runnable() {
                        @Override
                        public void run() {
                            Predicate<PaperCard> additionalFilter = null;
                            final EditorType editorType = parentScreen.getEditorType();
                            final Localizer localizer = Localizer.getInstance();
                            switch (editorType) {
                                case Archenemy:
                                    cardManager.setPool(FModel.getArchenemyCards(), true);
                                    break;
                                case Planechase:
                                    cardManager.setPool(FModel.getPlanechaseCards(), true);
                                    break;
                                case Quest:
                                    final ItemPool<PaperCard> questPool = new ItemPool<>(PaperCard.class);
                                    questPool.addAll(FModel.getQuest().getCards().getCardpool());
                                    // remove bottom cards that are in the deck from the card pool
                                    questPool.removeAll(parentScreen.getDeck().getMain());
                                    // remove sideboard cards from the catalog
                                    questPool.removeAll(parentScreen.getDeck().getOrCreate(DeckSection.Sideboard));
                                    cardManager.setPool(questPool);
                                    break;
                                case PlanarConquest:
                                    cardManager.setPool(ConquestUtil.getAvailablePool(parentScreen.getDeck()));
                                    break;
                                case QuestCommander:
                                case Commander:
                                case Oathbreaker:
                                case TinyLeaders:
                                case Brawl:
                                    final List<PaperCard> commanders = parentScreen.getDeck().getCommanders();
                                    if (commanders.isEmpty()) {
                                        //if no commander set for deck, only show valid commanders
                                        switch (editorType) {
                                            case Commander:
                                            case QuestCommander:
                                                additionalFilter = DeckFormat.Commander.isLegalCommanderPredicate();
                                                cardManager.setCaption(localizer.getMessage("lblCommanders"));
                                                break;
                                            case Oathbreaker:
                                                additionalFilter = DeckFormat.Oathbreaker.isLegalCommanderPredicate();
                                                cardManager.setCaption(localizer.getMessage("lblOathbreakers"));
                                                break;
                                            case TinyLeaders:
                                                additionalFilter = DeckFormat.TinyLeaders.isLegalCommanderPredicate();
                                                cardManager.setCaption(localizer.getMessage("lblCommanders"));
                                                break;
                                            case Brawl:
                                                additionalFilter = DeckFormat.Brawl.isLegalCommanderPredicate();
                                                cardManager.setCaption(localizer.getMessage("lblCommanders"));
                                                break;
                                            default:
                                                // Do nothing
                                        }
                                    } else {
                                        //if a commander has been set, only show cards that match its color identity
                                        switch (editorType) {
                                            case Commander:
                                            case QuestCommander:
                                                additionalFilter = DeckFormat.Commander.isLegalCardForCommanderPredicate(commanders);
                                                break;
                                            case Oathbreaker:
                                                additionalFilter = DeckFormat.Oathbreaker.isLegalCardForCommanderPredicate(commanders);
                                                break;
                                            case TinyLeaders:
                                                additionalFilter = DeckFormat.TinyLeaders.isLegalCardForCommanderPredicate(commanders);
                                                break;
                                            case Brawl:
                                                additionalFilter = DeckFormat.Brawl.isLegalCardForCommanderPredicate(commanders);
                                                break;
                                            default:
                                                // Do nothing
                                        }
                                        cardManager.setCaption(localizer.getMessage("lblCards"));
                                    }
                                    // fall through to below
                                default:
                                    if (cardManager.getWantUnique()) {
                                        cardManager.setPool(editorType.applyCardFilter(FModel.getUniqueCardsNoAlt(), additionalFilter), true);
                                    } else {
                                        cardManager.setPool(editorType.applyCardFilter(FModel.getAllCardsNoAlt(), additionalFilter), true);
                                    }
                                    break;
                            }
                        }
                    });
                }
            });
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            if (getMaxMoveQuantity(true, true) == 0) {
                return; //don't add card if maximum copies of card already in deck
            }
            if (needsCommander()) {
                setCommander(card); //handle special case of setting commander
                return;
            }
            if (canOnlyBePartnerCommander(card)) {
                return; //don't auto-change commander unexpectedly
            }
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            parentScreen.getMainDeckPage().addCard(card);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            final Localizer localizer = Localizer.getInstance();

            if (!needsCommander() && !canOnlyBePartnerCommander(card)) {
                addItem(menu, localizer.getMessage("lblAdd"), localizer.getMessage("lblTo") + " " + parentScreen.getMainDeckPage().cardManager.getCaption(), parentScreen.getMainDeckPage().getIcon(), true, true, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }
    
                        if (!cardManager.isInfinite()) {
                            removeCard(card, result);
                        }
                        parentScreen.getMainDeckPage().addCard(card, result);
                    }
                });
                if (parentScreen.getSideboardPage() != null) {
                    addItem(menu, localizer.getMessage("lblAdd"), localizer.getMessage("lbltosideboard"), parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }
    
                            if (!cardManager.isInfinite()) {
                                removeCard(card, result);
                            }
                            parentScreen.getSideboardPage().addCard(card, result);
                        }
                    });
                }
            }

            addCommanderItems(menu, card, true, true);

            if (parentScreen.getEditorType() == EditorType.Constructed) {
                //add option to add or remove card from favorites
                final CardPreferences prefs = CardPreferences.getPrefs(card);
                if (prefs.getStarCount() == 0) {
                    menu.addItem(new FMenuItem(localizer.getMessage("lblAddFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_FILLED : FSkinImage.STAR_FILLED, new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            prefs.setStarCount(1);
                            CardPreferences.save();
                        }
                    }));
                } else {
                    menu.addItem(new FMenuItem(localizer.getMessage("lblRemoveFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_OUTLINE : FSkinImage.STAR_OUTLINE, new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            prefs.setStarCount(0);
                            CardPreferences.save();
                        }
                    }));
                }

                //if card has more than one art option, add item to change user's preferred art
                final List<PaperCard> artOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                if (artOptions != null && artOptions.size() > 1) {
                    menu.addItem(new FMenuItem(localizer.getMessage("lblChangePreferredArt"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            //sort options so current option is on top and selected by default
                            List<PaperCard> sortedOptions = new ArrayList<>();
                            sortedOptions.add(card);
                            for (PaperCard option : artOptions) {
                                if (option != card) {
                                    sortedOptions.add(option);
                                }
                            }
                            GuiChoose.oneOrNone(localizer.getMessage("lblSelectPreferredArt") + " " + card.getName(), sortedOptions, new Callback<PaperCard>() {
                                @Override
                                public void run(PaperCard result) {
                                    if (result != null) {
                                        if (result != card) {
                                            cardManager.replaceAll(card, result);
                                        }
                                        prefs.setPreferredArt(result.getEdition(), result.getArtIndex());
                                        CardPreferences.save();
                                    }
                                }
                            });
                        }
                    }));
                }
            }
        }

        @Override
        protected void buildDeckMenu(FPopupMenu menu) {
            if (cardManager.getConfig().getShowUniqueCardsOption()) {
                final Localizer localizer = Localizer.getInstance();
                menu.addItem(new FCheckBoxMenuItem(localizer.getMessage("lblUniqueCardsOnly"), cardManager.getWantUnique(), new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        boolean wantUnique = !cardManager.getWantUnique();
                        cardManager.setWantUnique(wantUnique);
                        refresh();
                        cardManager.getConfig().setUniqueCardsOnly(wantUnique);
                    }
                }));
            }
        }
    }

    protected static class DeckSectionPage extends CardManagerPage {
        private String captionPrefix;
        private final DeckSection deckSection;

        protected DeckSectionPage(DeckSection deckSection0) {
            this(deckSection0, ItemManagerConfig.DECK_EDITOR);
        }
        protected DeckSectionPage(DeckSection deckSection0, ItemManagerConfig config) {
            super(config, null, null);

            final Localizer localizer = Localizer.getInstance();

            deckSection = deckSection0;
            switch (deckSection) {
            default:
            case Main:
                captionPrefix = localizer.getMessage("lblMain");
                cardManager.setCaption(localizer.getMessage("ttMain"));
                icon = MAIN_DECK_ICON;
                break;
            case Sideboard:
                captionPrefix = localizer.getMessage("lblSide");
                cardManager.setCaption(localizer.getMessage("lblSideboard"));
                icon = SIDEBOARD_ICON;
                break;
            case Commander:
                captionPrefix = localizer.getMessage("lblCommander");
                cardManager.setCaption(localizer.getMessage("lblCommander"));
                icon = FSkinImage.COMMANDER;
                break;
            case Avatar:
                captionPrefix = localizer.getMessage("lblAvatar");
                cardManager.setCaption(localizer.getMessage("lblAvatar"));
                icon = new FTextureRegionImage(FSkin.getAvatars().get(0));
                break;
            case Planes:
                captionPrefix = localizer.getMessage("lblPlanes");
                cardManager.setCaption(localizer.getMessage("lblPlanes"));
                icon = FSkinImage.CHAOS;
                break;
            case Schemes:
                captionPrefix = localizer.getMessage("lblSchemes");
                cardManager.setCaption(localizer.getMessage("lblSchemes"));
                icon = FSkinImage.POISON;
                break;
            }
        }
        protected DeckSectionPage(DeckSection deckSection0, ItemManagerConfig config, String caption0, FImage icon0) {
            super(config, null, icon0);

            deckSection = deckSection0;
            captionPrefix = caption0;
            cardManager.setCaption(caption0);
        }

        @Override
        protected void initialize() {
            super.initialize();
            cardManager.setPool(parentScreen.getDeck().getOrCreate(deckSection));
            updateCaption();
        }

        @Override
        protected void updateCaption() {
            if (deckSection == DeckSection.Commander) {
                caption = captionPrefix; //don't display count for commander section since it won't be more than 1
            } else {
                caption = captionPrefix + " (" + parentScreen.getDeck().get(deckSection).countAll() + ")";
            }
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            switch (deckSection) {
            case Main:
            case Planes:
            case Schemes:
                removeCard(card);
                switch (parentScreen.getEditorType()) {
                case Draft:
                case Sealed:
                case QuestDraft:
                    parentScreen.getSideboardPage().addCard(card);
                    break;
                default:
                    if (parentScreen.getCatalogPage() != null) {
                        parentScreen.getCatalogPage().addCard(card);
                    }
                    break;
                }
                break;
            case Sideboard:
                removeCard(card);
                parentScreen.getMainDeckPage().addCard(card);
                break;
            default:
                break;
            }
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            final Localizer localizer = Localizer.getInstance();
            switch (deckSection) {
            default:
            case Main:
                addItem(menu, localizer.getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        if (parentScreen.isLimitedEditor()) { //ensure card removed from sideboard before adding to main
                            parentScreen.getSideboardPage().removeCard(card, result);
                        }
                        else if (parentScreen.getEditorType() == EditorType.Quest || parentScreen.getEditorType() == EditorType.QuestCommander) {
                            parentScreen.getCatalogPage().removeCard(card, result);
                        }
                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            if (parentScreen.getCatalogPage() != null) {
                                parentScreen.getCatalogPage().addCard(card, result);
                            }
                        }
                    });
                }
                if (parentScreen.getSideboardPage() != null) {
                    addItem(menu, localizer.getMessage("lblMove"), localizer.getMessage("lbltosideboard"), parentScreen.getSideboardPage().getIcon(), false, false, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            parentScreen.getSideboardPage().addCard(card, result);
                        }
                    });
                }
                if (parentScreen.isAllowedReplacement()) {
                    final List<PaperCard> cardOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                    if (cardOptions != null && cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(localizer.getMessage("lblReplaceCard"), Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                //sort options so current option is on top and selected by default
                                List<PaperCard> sortedOptions = new ArrayList<>();
                                sortedOptions.add(card);
                                for (PaperCard option : cardOptions) {
                                    if (option != card) {
                                        sortedOptions.add(option);
                                    }
                                }
                                GuiChoose.oneOrNone(localizer.getMessage("lblSelectReplacementCard") + " " + card.getName(), sortedOptions, new Callback<PaperCard>() {
                                    @Override
                                    public void run(PaperCard result) {
                                        if (result != null) {
                                            if (result != card) {
                                                addCard(result);
                                                removeCard(card);
                                            }
                                        }
                                    }
                                });
                            }
                        }));
                    }
                }
                addCommanderItems(menu, card, false, false);
                break;
            case Sideboard:
                addItem(menu, localizer.getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        if (parentScreen.isLimitedEditor()) { //ensure card removed from main deck before adding to sideboard
                            parentScreen.getMainDeckPage().removeCard(card, result);
                        }
                        else if (parentScreen.getEditorType() == EditorType.Quest || parentScreen.getEditorType() == EditorType.QuestCommander) {
                            parentScreen.getCatalogPage().removeCard(card, result);
                        }
                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            if (parentScreen.getCatalogPage() != null) {
                                parentScreen.getCatalogPage().addCard(card, result);
                            }
                        }
                    });
                }
                addItem(menu, localizer.getMessage("lblMove"), localizer.getMessage("lblToMainDeck"), parentScreen.getMainDeckPage().getIcon(), false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        parentScreen.getMainDeckPage().addCard(card, result);
                    }
                });
                if (parentScreen.isAllowedReplacement()) {
                    final List<PaperCard> cardOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                    if (cardOptions != null && cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(localizer.getMessage("lblReplaceCard"), Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                //sort options so current option is on top and selected by default
                                List<PaperCard> sortedOptions = new ArrayList<>();
                                sortedOptions.add(card);
                                for (PaperCard option : cardOptions) {
                                    if (option != card) {
                                        sortedOptions.add(option);
                                    }
                                }
                                GuiChoose.oneOrNone(localizer.getMessage("lblSelectReplacementCard") + " " + card.getName(), sortedOptions, new Callback<PaperCard>() {
                                    @Override
                                    public void run(PaperCard result) {
                                        if (result != null) {
                                            if (result != card) {
                                                addCard(result);
                                                removeCard(card);
                                            }
                                        }
                                    }
                                });
                            }
                        }));
                    }
                }
                addCommanderItems(menu, card, false, false);
                break;
            case Commander:
                if (parentScreen.editorType != EditorType.PlanarConquest || isPartnerCommander(card)) {
                    addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) {
                                return;
                            }

                            removeCard(card, result);
                            parentScreen.getCatalogPage().refresh(); //refresh so commander options shown again
                            parentScreen.setSelectedPage(parentScreen.getCatalogPage());
                        }
                    });
                }
                if (parentScreen.isAllowedReplacement()) {
                    final List<PaperCard> cardOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                    if (cardOptions != null && cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(localizer.getMessage("lblReplaceCard"), Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                //sort options so current option is on top and selected by default
                                List<PaperCard> sortedOptions = new ArrayList<>();
                                sortedOptions.add(card);
                                for (PaperCard option : cardOptions) {
                                    if (option != card) {
                                        sortedOptions.add(option);
                                    }
                                }
                                GuiChoose.oneOrNone(localizer.getMessage("lblSelectReplacementCard") + " " + card.getName(), sortedOptions, new Callback<PaperCard>() {
                                    @Override
                                    public void run(PaperCard result) {
                                        if (result != null) {
                                            if (result != card) {
                                                addCard(result);
                                                removeCard(card);
                                            }
                                        }
                                    }
                                });
                            }
                        }));
                    }
                }
                break;
            case Avatar:
                addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Schemes:
                addItem(menu, localizer.getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Planes:
                addItem(menu, localizer.getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                addItem(menu, localizer.getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            }
        }

        private boolean isPartnerCommander(final PaperCard card) {
            if (parentScreen.getCommanderPage() == null || parentScreen.getDeck().getCommanders().isEmpty()) {
                return false;
            }

            PaperCard firstCmdr = parentScreen.getDeck().getCommanders().get(0);
            return !card.getName().equals(firstCmdr.getName());
        }
    }

    private static class DraftPackPage extends CatalogPage {
        protected DraftPackPage() {
            super(ItemManagerConfig.DRAFT_PACK, Localizer.getInstance().getMessage("lblPackN", String.valueOf(1)), FSkinImage.PACK);
        }

        @Override
        public void refresh() {
            BoosterDraft draft = parentScreen.getDraft();
            if (draft == null) { return; }

            CardPool pool = draft.nextChoice();
            int packNumber = draft.getCurrentBoosterIndex() + 1;
            caption = Localizer.getInstance().getMessage("lblPackN", String.valueOf(packNumber));
            cardManager.setPool(pool);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            super.onCardActivated(card);
            afterCardPicked(card);
        }

        private void afterCardPicked(PaperCard card) {
            BoosterDraft draft = parentScreen.getDraft();
            draft.setChoice(card);

            // TODO Implement handling of extra boosters

            if (draft.hasNextChoice()) {
                refresh();
            }
            else {
                hideTab(); //hide this tab page when finished drafting
                parentScreen.save(null);
            }
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            final Localizer localizer = Localizer.getInstance();
            addItem(menu, localizer.getMessage("lblAdd"), localizer.getMessage("lblToMainDeck"), parentScreen.getMainDeckPage().getIcon(), true, true, new Callback<Integer>() {
                @Override
                public void run(Integer result) { //ignore quantity
                    parentScreen.getMainDeckPage().addCard(card);
                    afterCardPicked(card);
                }
            });
            addItem(menu, localizer.getMessage("lblAdd"), localizer.getMessage("lbltosideboard"), parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
                @Override
                public void run(Integer result) { //ignore quantity
                    parentScreen.getSideboardPage().addCard(card);
                    afterCardPicked(card);
                }
            });
        }
    }

    public static class DeckController<T extends DeckBase> {
        private T model;
        private boolean saved;
        private boolean modelInStorage;
        private IStorage<T> rootFolder;
        private IStorage<T> currentFolder;
        private String modelPath;
        private FDeckEditor editor;
        private final Supplier<T> newModelCreator;

        protected DeckController(final IStorage<T> folder0, final Supplier<T> newModelCreator0) {
            setRootFolder(folder0);
            newModelCreator = newModelCreator0;
        }

        public void setRootFolder(IStorage<T> folder0) {
            rootFolder = folder0;
            currentFolder = folder0;
            model = null;
            saved = true;
            modelInStorage = false;
            modelPath = "";
        }

        public T getModel() {
            return model;
        }

        public String getModelPath() {
            return modelPath;
        }

        @SuppressWarnings("unchecked")
        public void setDeck(final Deck deck) {
            modelInStorage = false;
            model = (T)deck;
            currentFolder = rootFolder;
            modelPath = "";
            setSaved(false);
            editor.setDeck(deck);
        }

        public void setModel(final T document) {
            setModel(document, false);
        }
        public void setModel(final T document, final boolean isStored) {
            modelInStorage = isStored;
            model = document;

            if (isStored) {
                if (isModelInSyncWithFolder()) {
                    setSaved(true);
                } else {
                    notifyModelChanged();
                }
            }
            else { //TODO: Make this smarter
                currentFolder = rootFolder;
                modelPath = "";
                setSaved(true);
            }
            if (model != null) {
                editor.setDeck(model.getHumanDeck());
            } else {
                editor.setDeck(null);
            }

        }

        private boolean isModelInSyncWithFolder() {
            if (model.getName().isEmpty()) {
                return true;
            }

            final T modelStored = currentFolder.get(model.getName());
            // checks presence in dictionary only.
            if (modelStored == model) {
                return true;
            }
            if (modelStored == null) {
                return false;
            }

            return modelStored.equals(model);
        }

        public void notifyModelChanged() {
            if (saved) {
                setSaved(false);
            }
        }

        private void setSaved(boolean val) {
            saved = val;

            if (editor != null) {
                String name = this.getModelName();
                if (name.isEmpty()) {
                    final Localizer localizer = Localizer.getInstance();
                    name = "[" + localizer.getMessage("lblNewDeck") + "]";
                }
                if (!saved) {
                    name = "*" + name;
                }
                editor.lblName.setText(name);
                editor.btnSave.setEnabled(!saved);
            }
        }

        public void reload() {
            String name = getModelName();
            if (name.isEmpty()) {
                newModel();
            }
            else {
                load(name);
            }
        }

        public void load(final String path, final String name) {
            if (StringUtils.isBlank(path)) {
                currentFolder = rootFolder;
            }
            else {
                currentFolder = rootFolder.tryGetFolder(path);
            }
            modelPath = path;
            load(name);
        }

        @SuppressWarnings("unchecked")
        private void load(final String name) {
            T newModel = currentFolder.get(name);
            if (newModel != null) {
                setModel((T) newModel.copyTo(name), true);
            }
            else {
                setSaved(true);
            }
        }

        @SuppressWarnings("unchecked")
        public void save() {
            if (model == null) {
                return;
            }

            // copy to new instance before adding to current folder so further changes are auto-saved
            currentFolder.add((T) model.copyTo(model.getName()));
            model.setDirectory(DeckProxy.getDeckDirectory(currentFolder));
            modelInStorage = true;
            setSaved(true);

            //update saved deck names
            String deckStr = DeckProxy.getDeckString(getModelPath(), getModelName());
            switch (editor.getEditorType()) {
            case Constructed:
                DeckPreferences.setCurrentDeck(deckStr);
                break;
            case Commander:
                DeckPreferences.setCommanderDeck(deckStr);
                break;
            case Oathbreaker:
                DeckPreferences.setOathbreakerDeck(deckStr);
                break;
            case TinyLeaders:
                DeckPreferences.setTinyLeadersDeck(deckStr);
                break;
            case Brawl:
                DeckPreferences.setBrawlDeck(deckStr);
                break;
            case Archenemy:
                DeckPreferences.setSchemeDeck(deckStr);
                break;
            case Planechase:
                DeckPreferences.setPlanarDeck(deckStr);
                break;
            case Draft:
            case QuestDraft:
                DeckPreferences.setDraftDeck(deckStr);
                break;
            case Sealed:
                DeckPreferences.setSealedDeck(deckStr);
                break;
            case Quest:
            case QuestCommander:
                FModel.getQuest().setCurrentDeck(model.toString());
                FModel.getQuest().save();
                break;
            default:
                break;
            }
            editor.setDeck(model.getHumanDeck());
            if (editor.saveHandler != null) {
                editor.saveHandler.handleEvent(new FEvent(editor, FEventType.SAVE));
            }
        }

        @SuppressWarnings("unchecked")
        public void saveAs(final String name0) {
            model = (T)model.copyTo(name0);
            modelInStorage = false;
            save();
        }

        public void rename(final String name0) {
            if (StringUtils.isEmpty(name0)) { return; }

            String oldName = model.getName();
            if (name0.equals(oldName)) { return; }

            saveAs(name0);
            currentFolder.delete(oldName); //delete deck with old name
        }

        public String getNextAvailableName() {
            String name = model.getName();
            int idx = name.lastIndexOf('(');
            if (idx != -1) {
                name = name.substring(0, idx).trim(); //strip old number
            }

            String baseName = name;
            int number = 2;
            do {
                name = baseName + " (" + number + ")";
                number++;
            } while (fileExists(name));

            return name;
        }

        public boolean isSaved() {
            return saved;
        }

        public boolean fileExists(final String deckName) {
            return currentFolder.contains(deckName);
        }

        public void importDeck(final T newDeck) {
            setModel(newDeck);
        }

        public void refreshModel() {
            if (model == null) {
                newModel();
            }
            else {
                setModel(model, modelInStorage);
            }
        }

        public void newModel() {
            setModel(newModelCreator.get());
        }

        public String getModelName() {
            return model != null ? model.getName() : "";
        }

        public boolean delete() {
            if (model == null) { return false; }
            currentFolder.delete(model.getName());
            setModel(null);
            return true;
        }
    }
}

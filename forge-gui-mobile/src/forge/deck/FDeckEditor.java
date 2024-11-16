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
import forge.card.MagicColor;
import forge.deck.io.DeckPreferences;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.GuiBase;
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
import forge.screens.TabPageScreen;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.*;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    public static FSkinImage MAIN_DECK_ICON = Forge.hdbuttons ? FSkinImage.HDLIBRARY :FSkinImage.DECKLIST;
    public static FSkinImage SIDEBOARD_ICON = Forge.hdbuttons ? FSkinImage.HDSIDEBOARD : FSkinImage.FLASHBACK;
    private static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);

    public enum EditorType {
        Constructed(new DeckController<>(FModel.getDecks().getConstructed(), (Supplier<Deck>) Deck::new), null),
        Draft(new DeckController<>(FModel.getDecks().getDraft(), (Supplier<DeckGroup>) DeckGroup::new), null),
        Sealed(new DeckController<>(FModel.getDecks().getSealed(), (Supplier<DeckGroup>) DeckGroup::new), null),
        Winston(new DeckController<>(FModel.getDecks().getWinston(), (Supplier<DeckGroup>) DeckGroup::new), null),
        Commander(new DeckController<>(FModel.getDecks().getCommander(), (Supplier<Deck>) Deck::new), null),
        Oathbreaker(new DeckController<>(FModel.getDecks().getOathbreaker(), (Supplier<Deck>) Deck::new), null),
        TinyLeaders(new DeckController<>(FModel.getDecks().getTinyLeaders(), (Supplier<Deck>) Deck::new), DeckFormat.TinyLeaders.isLegalCardPredicate()),
        Brawl(new DeckController<>(FModel.getDecks().getBrawl(), (Supplier<Deck>) Deck::new), DeckFormat.Brawl.isLegalCardPredicate()),
        Archenemy(new DeckController<>(FModel.getDecks().getScheme(), (Supplier<Deck>) Deck::new), null),
        Planechase(new DeckController<>(FModel.getDecks().getPlane(), (Supplier<Deck>) Deck::new), null),
        Quest(new DeckController<>(null, (Supplier<Deck>) Deck::new), null), //delay setting root folder until quest loaded
        QuestCommander(new DeckController<>(null, (Supplier<Deck>) Deck::new), null),
        QuestDraft(new DeckController<>(null, (Supplier<DeckGroup>) DeckGroup::new), null),
        PlanarConquest(new DeckController<>(null, (Supplier<Deck>) Deck::new), null);

        private static final Set<EditorType> LIMITED_TYPES = Collections.unmodifiableSet(
                EnumSet.of(Draft, Sealed, Winston, QuestDraft)
        );
        private static final Set<EditorType> USER_CARD_POOL_TYPES = Collections.unmodifiableSet(
                EnumSet.of(Draft, Sealed, Winston, QuestDraft, Quest, QuestCommander, PlanarConquest)
        );
        private static final Set<EditorType> COMMANDER_TYPES = Collections.unmodifiableSet(
                EnumSet.of(Commander, Oathbreaker, TinyLeaders, Brawl, QuestCommander)
        );
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

        public boolean isLimitedType() {
            return LIMITED_TYPES.contains(this);
        }
        public boolean isCommanderType() {
            return COMMANDER_TYPES.contains(this);
        }

        /**
         * @return true if the editor provides unlimited copies of the format's full card pool.
         */
        public boolean hasInfiniteCardPool() {
            return !USER_CARD_POOL_TYPES.contains(this);
        }
    }

    private static DeckEditorPage[] getPages(EditorType editorType) {
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
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.OATHBREAKER_SECTION, Forge.getLocalizer().getMessage("lblOathbreaker"), FSkinImage.COMMANDER),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CARD_CATALOG),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.OATHBREAKER_SECTION, Forge.getLocalizer().getMessage("lblOathbreaker"), FSkinImage.COMMANDER),
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
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.QUEST_DECK_EDITOR)
            };
        case QuestCommander:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.QUEST_DECK_EDITOR)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.QUEST_DECK_EDITOR)
            };
        case PlanarConquest:
            return isLandscape ? new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CONQUEST_COLLECTION, Forge.getLocalizer().getMessage("lblCollection"), FSkinImage.SPELLBOOK),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.CONQUEST_DECK_EDITOR, Forge.getLocalizer().getMessage("lblDeck"), Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST)
            } : new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.CONQUEST_COLLECTION, Forge.getLocalizer().getMessage("lblCollection"), FSkinImage.SPELLBOOK),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.CONQUEST_DECK_EDITOR, Forge.getLocalizer().getMessage("lblDeck"), Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST),
                    new DeckSectionPage(DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION)
            };
        }
    }

    /**
     * @return an array of optional deck sections supported by the format, but aren't usually included.
     */
    public static DeckSection[] getExtraSections(EditorType editorType) {
        switch (editorType) {
            case Constructed:
            case Commander:
                return new DeckSection[]{
                        DeckSection.Avatar, DeckSection.Schemes, DeckSection.Planes, DeckSection.Conspiracy, DeckSection.Attractions
                };
            case Draft:
            case Sealed:
                return new DeckSection[]{DeckSection.Conspiracy, DeckSection.Attractions};
        }
        return new DeckSection[]{DeckSection.Attractions};
    }

    private static DeckSectionPage createPageForExtraSection(DeckSection deckSection, EditorType editorType) {
        switch (deckSection) {
            case Avatar:
            case Commander:
                return new DeckSectionPage(deckSection, ItemManagerConfig.COMMANDER_SECTION);
            case Schemes:
                return new DeckSectionPage(deckSection, ItemManagerConfig.SCHEME_DECK_EDITOR);
            case Planes:
                return new DeckSectionPage(deckSection, ItemManagerConfig.PLANAR_DECK_EDITOR);
            case Conspiracy:
                return new DeckSectionPage(deckSection, ItemManagerConfig.CONSPIRACY_DECKS);
            case Dungeon:
                return new DeckSectionPage(deckSection, ItemManagerConfig.DUNGEON_DECKS);
            case Attractions:
                if(editorType.isLimitedType())
                    return new DeckSectionPage(deckSection, ItemManagerConfig.ATTRACTION_DECK_EDITOR_LIMITED);
                return new DeckSectionPage(deckSection, ItemManagerConfig.ATTRACTION_DECK_EDITOR);
            default:
                System.out.printf("Editor (%s) added an unsupported extra deck section - %s%n", deckSection, editorType);
                return new DeckSectionPage(deckSection);
        }
    }

    private static String labelFromDeckSection(DeckSection deckSection) {
        String label = null;
        switch (deckSection) {
            case Main: label = "lblMain"; break;
            case Sideboard: label = "lblSide"; break;
            case Commander: label = "lblCommander"; break;
            case Planes: label = "lblPlanes"; break;
            case Schemes: label = "lblSchemes"; break;
            case Avatar: label = "lblAvatar"; break;
            case Conspiracy: label = "lblConspiracies"; break;
            case Attractions: label = "lblAttractions"; break;
        }
        String text = Localizer.getInstance().getMessage(label);
        if(text == null)
            return deckSection.toString();
        return text;
    }

    private final EditorType editorType;
    private Deck deck;
    private final List<DeckSection> hiddenExtraSections = new ArrayList<>();
    private CatalogPage catalogPage;
    private DeckSectionPage mainDeckPage;
    private DeckSectionPage sideboardPage;
    private DeckSectionPage commanderPage;
    private final Map<DeckSection, DeckSectionPage> pagesBySection = new EnumMap<>(DeckSection.class);
    private final Set<DeckSection> variantCardPools = new HashSet<>();
    private FEventHandler saveHandler;

    protected final DeckHeader deckHeader = add(new DeckHeader());
    protected final FLabel lblName = deckHeader.add(new FLabel.Builder().font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build());
    private final FLabel btnSave = deckHeader.add(new FLabel.Builder().icon(Forge.hdbuttons ? FSkinImage.HDSAVE : FSkinImage.SAVE).align(Align.center).pressedColor(Header.getBtnPressedColor()).build());
    private final FLabel btnMoreOptions = deckHeader.add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(Align.center).pressedColor(Header.getBtnPressedColor()).build());

    public FDeckEditor(EditorType editorType0, DeckProxy editDeck, boolean showMainDeck) {
        this(editorType0, editDeck.getName(), editDeck.getPath(), null, showMainDeck, null);
    }
    public FDeckEditor(EditorType editorType0, DeckProxy editDeck, boolean showMainDeck, FEventHandler backButton) {
        this(editorType0, editDeck.getName(), editDeck.getPath(), null, showMainDeck, backButton);
    }
    public FDeckEditor(EditorType editorType0, String editDeckName, boolean showMainDeck, FEventHandler backButton) {
        this(editorType0, editDeckName, "", null, showMainDeck, backButton);
    }
    public FDeckEditor(EditorType editorType0, String editDeckName, boolean showMainDeck) {
        this(editorType0, editDeckName, "", null, showMainDeck, null);
    }
    public FDeckEditor(EditorType editorType0, Deck newDeck, boolean showMainDeck) {
        this(editorType0, "", "", newDeck, showMainDeck, null);
    }
    private FDeckEditor(EditorType editorType0, String editDeckName, String editDeckPath, Deck newDeck, boolean showMainDeck, FEventHandler backButton) {
        super(backButton, getPages(editorType0));

        editorType = editorType0;

        editorType.getController().editor = this;

        //cache specific pages
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            if (tabPage instanceof CatalogPage) {
                catalogPage = (CatalogPage) tabPage;
            }
            else if (tabPage instanceof DeckSectionPage) {
                DeckSectionPage deckSectionPage = (DeckSectionPage) tabPage;
                pagesBySection.put(deckSectionPage.deckSection, deckSectionPage);
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
                tabPages.get(0).hideTab(); //hide Draft Pack page if editing existing draft deck
            }
            editorType.getController().load(editDeckPath, editDeckName);
        }

        for(DeckSection section : getExtraSections(editorType)) {
            if (deck != null && deck.has(section))
                this.showExtraSectionTab(section);
            else {
                this.hiddenExtraSections.add(section);
                this.createExtraSectionPage(section).hideTab();
            }
        }

        if(!this.getVariantCardPools().isEmpty() && editorType.hasInfiniteCardPool())
            getCatalogPage().scheduleRefresh();

        if(allowsSave())
        {
            btnSave.setCommand(e -> save(null));
        }
        else
        {
         btnSave.setVisible(false);
        }
        btnMoreOptions.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent fEvent) {
                FPopupMenu menu = new FPopupMenu() {
                    @Override
                    protected void buildMenu() {
                        final Localizer localizer = Forge.getLocalizer();
                        if (allowsAddBasic())
                            addItem(new FMenuItem(localizer.getMessage("lblAddBasicLands"), FSkinImage.LANDLOGO, e -> {
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
                                    case QuestCommander:
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
                                }, null);
                                dialog.show();
                                setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck is visible below dialog
                            }));
                        if (allowsAddExtraSection()) {
                            addItem(new FMenuItem(localizer.getMessage("lblAddDeckSection"), FSkinImage.CHAOS, e -> {
                                List<String> options = hiddenExtraSections.stream().map(FDeckEditor::labelFromDeckSection).collect(Collectors.toList());
                                GuiChoose.oneOrNone(localizer.getMessage("lblAddDeckSectionSelect"), options, new Callback<String>() {
                                    @Override
                                    public void run(String result) {
                                        if (result == null || !options.contains(result))
                                            return;
                                        DeckSection newSection = hiddenExtraSections.get(options.indexOf(result));
                                        showExtraSectionTab(newSection);
                                        filterCatalogForExtraSection(newSection);
                                        getCatalogPage().scheduleRefresh();
                                        setSelectedPage(getCatalogPage());
                                    }
                                });
                            }));
                        }
                        if (!isLimitedEditor()) {
                            addItem(new FMenuItem(localizer.getMessage("lblImportFromClipboard"), Forge.hdbuttons ? FSkinImage.HDIMPORT : FSkinImage.OPEN, e -> {
                                FDeckImportDialog dialog = new FDeckImportDialog(!deck.isEmpty(), editorType);
                                dialog.setCallback(new Callback<Deck>() {
                                    @Override
                                    public void run(Deck importedDeck) {
                                        if (deck != null && importedDeck.hasName()) {
                                            deck.setName(importedDeck.getName());
                                            lblName.setText(importedDeck.getName());
                                        }
                                        if (dialog.createNewDeck()) {
                                            for (Entry<DeckSection, CardPool> section : importedDeck) {
                                                DeckSectionPage page = getPageForSection(section.getKey());
                                                if (page != null)
                                                    page.setCards(section.getValue());
                                            }
                                        } else {
                                            for (Entry<DeckSection, CardPool> section : importedDeck) {
                                                DeckSectionPage page = getPageForSection(section.getKey());
                                                if (page != null)
                                                    page.addCards(section.getValue());
                                            }
                                        }
                                    }
                                });
                                dialog.show();
                                setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck if visible below dialog
                            }));
                            if (allowsSave())
                                addItem(new FMenuItem(localizer.getMessage("lblSaveAs"), Forge.hdbuttons ? FSkinImage.HDSAVEAS : FSkinImage.SAVEAS, e -> {
                                    String defaultName = editorType.getController().getNextAvailableName();
                                    FOptionPane.showInputDialog(localizer.getMessage("lblNameNewCopyDeck"), defaultName, new Callback<String>() {
                                        @Override
                                        public void run(String result) {
                                            if (!StringUtils.isEmpty(result)) {
                                                editorType.getController().saveAs(result);
                                            }
                                        }
                                    });
                                }));
                        }
                        if (allowRename()) {
                            addItem(new FMenuItem(localizer.getMessage("lblRenameDeck"), Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT, e -> FOptionPane.showInputDialog(
                                    localizer.getMessage("lblNewNameDeck"), deck.getName(), new Callback<String>() {
                                        @Override
                                        public void run(String result) {
                                            editorType.getController().rename(result);
                                        }
                                    }))
                            );
                        }
                        if (allowDelete()) {
                            addItem(new FMenuItem(localizer.getMessage("lblDeleteDeck"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, e -> FOptionPane.showConfirmDialog(
                                    localizer.getMessage("lblConfirmDelete") + " '" + deck.getName() + "'?",
                                    localizer.getMessage("lblDeleteDeck"),
                                    localizer.getMessage("lblDelete"),
                                    localizer.getMessage("lblCancel"), false,
                                    new Callback<Boolean>() {
                                        @Override
                                        public void run(Boolean result) {
                                            if (result) {
                                                editorType.getController().delete();
                                                Forge.back();
                                            }
                                        }
                                    }))
                            );
                        }
                        addItem(new FMenuItem(localizer.getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, e -> FDeckViewer.copyDeckToClipboard(deck)));
                        ((DeckEditorPage) getSelectedPage()).buildDeckMenu(this);
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

    protected DeckSectionPage getPageForSection(DeckSection section) {
        return pagesBySection.get(section);
    }

    protected Set<DeckSection> getVariantCardPools() {
        return variantCardPools;
    }

    public BoosterDraft getDraft() {
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

    private int getExtraSectionMaxCopies(DeckSection section) {
        switch(section) {
            case Avatar:
            case Commander:
            case Planes:
            case Dungeon:
                return 1;
            case Schemes:
                return 2;
            case Conspiracy:
                return Integer.MAX_VALUE;
            case Attractions:
                if(isLimitedEditor())
                    return Integer.MAX_VALUE;
                else
                    return 1;
            default:
                return FModel.getPreferences().getPrefInt(FPref.DECK_DEFAULT_CARD_LIMIT);
        }
    }

    protected ItemPool<PaperCard> getAllowedAdditions(Iterable<Entry<PaperCard, Integer>> itemsToAdd, CardManagerPage source, CardManagerPage destination)
    {
        ItemPool<PaperCard> additions = new ItemPool<>(destination.cardManager.getGenericType());

        for (Entry<PaperCard, Integer> itemEntry : itemsToAdd) {
            PaperCard card = itemEntry.getKey();
            int numAvailable = Math.min(itemEntry.getValue(), source.cardManager.getItemCount(card));

            int maxMovable = getMaxMovable(card, source, destination, numAvailable);
            if (maxMovable <= 0)
                continue;

            additions.add(card, maxMovable);
        }
        return additions;
    }

    protected int getMaxMovable(PaperCard card, CardManagerPage source, CardManagerPage destination) {
        return getMaxMovable(card, source, destination, source.cardManager.getItemCount(card));
    }

    /**
     * Returns the number of copies of {@code card} that can be moved from {@code source} to {@code destination}.
     * <br>
     * Accounts for amount available in the source page, number allowed in the deck per the format, and the card's
     * own rules for number allowed in deck (e.g. Relentless Rats).
     * <br>
     * Does not account for color identity, rules regarding designations within the command zone (e.g. whether a card
     * can partner with the current commander), or the format's bans or restrictions.
     */
    protected int getMaxMovable(PaperCard card, CardManagerPage source, CardManagerPage destination, int numAvailable) {
        if(numAvailable <= 0)
            return 0;

        if(destination instanceof CatalogPage || (isLimitedEditor() && destination == this.getSideboardPage())) {
            //Removing card. Move any number.
            return numAvailable;
        }

        //Count copies across all deck sections.
        int numInDeck = getDeck().countByName(card.getName());
        if(isLimitedEditor()) //In limited, disregard cards in sideboard. (This matters for cards like Seven Dwarves)
            numInDeck -= getDeck().getOrCreate(DeckSection.Sideboard).countByName(card);

        int numAllowedInDeck = getNumAllowedInDeck(card);

        if(numAllowedInDeck == Integer.MAX_VALUE)
            return numAvailable;
        else if(!FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            if(numAllowedInDeck == 1) //Don't prompt for quantity when editing singleton decks, even with conformity off.
                return 1;
            else
                return numAvailable;
        }
        else {
            //Limited number of copies. If we're adding to the deck, cap the amount accordingly.
            if(source instanceof CatalogPage || (isLimitedEditor() && source == this.getSideboardPage()))
                return Math.min(numAvailable, Math.max(numAllowedInDeck - numInDeck, 0));
            else
                return numAvailable;
        }
    }

    private int getNumAllowedInDeck(PaperCard card) {
        CardLimit limit = getCardLimit();
        if(DeckFormat.canHaveSpecificNumberInDeck(card) != null)
            return DeckFormat.canHaveSpecificNumberInDeck(card);
        else if (DeckFormat.canHaveAnyNumberOf(card))
            return Integer.MAX_VALUE;
        else if (card.getRules().isVariant())
            return getExtraSectionMaxCopies(DeckSection.matchingSection(card));
        else if (limit == CardLimit.None)
            return Integer.MAX_VALUE;
        else if (limit == CardLimit.Singleton)
            return 1;
        else
            return FModel.getPreferences().getPrefInt(FPref.DECK_DEFAULT_CARD_LIMIT);
    }

    protected DeckSectionPage showExtraSectionTab(DeckSection section) {
        this.variantCardPools.add(section);
        this.hiddenExtraSections.remove(section);
        DeckSectionPage page = this.getPageForSection(section);
        if(page == null)
            page = createExtraSectionPage(section);
        page.showTab();
        return page;
    }

    protected DeckSectionPage createExtraSectionPage(DeckSection section) {
        DeckSectionPage page = createPageForExtraSection(section, this.editorType);
        this.pagesBySection.put(section, page);
        this.addTabPage(page);
        page.initialize();
        return page;
    }

    protected void filterCatalogForExtraSection(DeckSection section) {
        CardManager cardManager = getCatalogPage().cardManager;
        switch (section) {
            case Avatar:
                cardManager.applyAdvancedSearchFilter("CARD_TYPE CONTAINS_ALL Vanguard");
                break;
            case Planes:
                cardManager.applyAdvancedSearchFilter("CARD_TYPE CONTAINS_ANY Plane;Phenomenon");
                break;
            case Schemes:
                cardManager.applyAdvancedSearchFilter("CARD_TYPE CONTAINS_ALL Scheme");
                break;
            case Conspiracy:
                cardManager.applyAdvancedSearchFilter("CARD_TYPE CONTAINS_ALL Conspiracy");
                break;
            case Dungeon:
                cardManager.applyAdvancedSearchFilter("CARD_TYPE CONTAINS_ALL Dungeon");
                break;
            case Attractions:
                cardManager.applyAdvancedSearchFilter(new String[]{
                        "CARD_TYPE CONTAINS_ALL Artifact",
                        "CARD_SUB_TYPE CONTAINS_ALL Attraction"
                }, true);
                break;
            default:
                cardManager.resetFilters();
        }
    }

    public void setSaveHandler(FEventHandler saveHandler0) {
        saveHandler = saveHandler0;
    }

    public void save(final Callback<Boolean> callback) {
        if (StringUtils.isEmpty(deck.getName())) {
            List<PaperCard> commanders = deck.getCommanders(); //use commander name as default deck name
            String initialInput = Lang.joinHomogenous(commanders);
            FOptionPane.showInputDialog(Forge.getLocalizer().getMessage("lblNameNewDeck"), initialInput, new Callback<String>() {
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
        FOptionPane.showOptionDialog(Forge.getLocalizer().getMessage("lblSaveChangesCurrentDeck"), "",
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

    protected boolean allowsSave() {
        return true;
    }
    protected boolean allowsAddBasic() {
        return true;
    }

    /**
     * @return true if the editor should show the "Add Deck Section" option in the menu. False otherwise.
     */
    protected boolean allowsAddExtraSection() {
        //In limited and formats with user inventories, variant cards can appear in their collection or card pool,
        //so they can create the section just by adding a card to it.
        return editorType.hasInfiniteCardPool() && !this.hiddenExtraSections.isEmpty();
    }
    protected boolean isLimitedEditor() {
        return editorType.isLimitedType();
    }
    protected boolean isCommanderEditor() {
        return editorType.isCommanderType();
    }
    protected boolean isDraftEditor() {
        switch (editorType) {
            case Draft:
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
            {
                if (editorType.isLimitedType())
                    return false;
                else if (editorType == EditorType.PlanarConquest || editorType == EditorType.Quest || editorType == EditorType.QuestCommander)
                    return FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED);
                else
                    return false;
            }
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
            g.fillRect(Header.getBackColor(), 0, 0, getWidth(), HEADER_HEIGHT);
        }

        @Override
        public void drawOverlay(Graphics g) {
            float y = HEADER_HEIGHT - Header.LINE_THICKNESS / 2;
            g.drawLine(Header.LINE_THICKNESS, Header.getLineColor(), 0, y, getWidth(), y);
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
            cardManager.setItemActivateHandler(e -> onCardActivated(cardManager.getSelectedItem()));
            cardManager.setContextMenuBuilder(new ContextMenuBuilder<PaperCard>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                    CardManagerPage.this.buildMenu(menu, card);
                }
            });
            cardManager.setShowRanking(ItemManagerConfig.DRAFT_CONSPIRACY.equals(config0)
                    || ItemManagerConfig.DRAFT_PACK.equals(config0) || ItemManagerConfig.DRAFT_POOL.equals(config0)
                    || ItemManagerConfig.DRAFT_DECKS.equals(config0) || (parentScreen != null && parentScreen.isDraftEditor()));
        }

        protected void initialize() {
            if (GuiBase.isAdventureMode())
                cardManager.setup(config);
            else //fix planar conquest deck editor and maybe others...
                cardManager.setup(config, parentScreen.getColOverrides(config));
            cardManager.setShowRanking(ItemManagerConfig.DRAFT_CONSPIRACY.equals(config)
                    || ItemManagerConfig.DRAFT_PACK.equals(config) || ItemManagerConfig.DRAFT_POOL.equals(config)
                    || ItemManagerConfig.DRAFT_DECKS.equals(config) || (parentScreen != null && parentScreen.isDraftEditor()));
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
            if (cardManager.isInfinite())
                return;
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
            menu.addItem(new FMenuItem(label, icon, e -> {
                if (max == 1) {
                    callback.run(max);
                } else {
                    GuiChoose.getInteger(cardManager.getSelectedItem() + " - " + verb + " " + Forge.getLocalizer().getMessage("lblHowMany"), 1, max, 20, callback);
                }
            }));
        }

        protected void addMoveCardMenuItem(FDropDownMenu menu, CardManagerPage source, CardManagerPage destination, final Callback<Integer> callback) {
            ItemPool<PaperCard> selectedItemPool = cardManager.getSelectedItemPool();
            if (source != this || cardManager.isInfinite()) {
                //Determine how many we can actually move.
                selectedItemPool = parentScreen.getAllowedAdditions(selectedItemPool, source, destination);
            }
            int maxMovable = selectedItemPool.isEmpty() ? 0 : Integer.MAX_VALUE;
            for (Entry<PaperCard, Integer> i : selectedItemPool)
                maxMovable = Math.min(maxMovable, i.getValue());
            if (maxMovable == 0)
                return;
            PaperCard sampleCard = cardManager.getSelectedItem();
            String labelAction, labelSection;
            if(destination == null || destination instanceof CatalogPage) {
                //Removing from this section, e.g. "Remove from sideboard"
                labelAction = "lblRemove";
                if(source instanceof DeckSectionPage)
                    labelSection = getMoveLabel((DeckSectionPage) source, sampleCard, true);
                else
                    labelSection = "lblCard";
            }
            else if(destination == this && source instanceof DeckSectionPage) {
                //Adding more to this section from another section, e.g. "Add from sideboard"
                labelAction = "lblAdd";
                labelSection = getMoveLabel((DeckSectionPage) source, sampleCard, true);
            }
            else if(source instanceof DeckSectionPage && destination instanceof DeckSectionPage) {
                //Moving from one named section to another, e.g. "Move to sideboard"
                labelAction = "lblMove";
                labelSection = getMoveLabel((DeckSectionPage) destination, sampleCard, false);
            }
            else if(destination instanceof DeckSectionPage) {
                //Moving from a card pool to a named section, e.g. "Add to sideboard"
                DeckSectionPage deckSectionPage = (DeckSectionPage) destination;
                if(deckSectionPage.deckSection == DeckSection.Commander || deckSectionPage.deckSection == DeckSection.Avatar)
                    labelAction = "lblSet";
                else
                    labelAction = "lblAdd";
                labelSection = getMoveLabel(deckSectionPage, sampleCard, false);
            }
            else {
                //Moving to something that isn't a deck section or a catalog. Shouldn't ever happen and I dunno what to do if it does.
                labelAction = "lblRemove";
                labelSection = "lblCard";
            }
            Localizer localizer = Localizer.getInstance();
            String action = localizer.getMessage(labelAction);
            String label = String.join(" ", action, localizer.getMessage(labelSection));
            String prompt = String.format("%s - %s %s", sampleCard, action, localizer.getMessage("lblHowMany"));

            FImage icon;
            if(source instanceof CatalogPage && destination instanceof DeckSectionPage && ((DeckSectionPage) destination).deckSection == DeckSection.Main)
                icon = Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS;
            else if(destination == null || destination instanceof CatalogPage)
                icon = Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS;
            else
                icon = destination.getIcon();

            final int max = maxMovable;
            menu.addItem(new FMenuItem(label, icon, (e) -> {
                if(max < 2)
                    callback.run(1);
                else
                    GuiChoose.getInteger(prompt, 1, max, 20, callback);
            }));
        }

        private String getMoveLabel(DeckSectionPage page, PaperCard selectedCard, boolean from) {
            //This might make more sense in the DeckSection class itself, and shared with the desktop editor.
            switch (page.deckSection) {
                default:
                case Main: return from ? "lblfromdeck" : "lbltodeck";
                case Sideboard: return from ? "lblfromsideboard" : "lbltosideboard";
                case Planes: return from ? "lblfromplanardeck" : "lbltoplanardeck";
                case Schemes: return from ? "lblfromschemedeck" : "lbltoschemedeck";
                case Conspiracy: return from ? "lblfromconspiracydeck" : "lbltoconspiracydeck";
                case Dungeon: return from ? "lblfromdungeondeck" : "lbltodungeondeck";
                case Attractions: return from ? "lblfromattractiondeck" : "lbltoattractiondeck";
                case Avatar: return "lblasavatar";
                case Commander:
                    if (parentScreen.editorType == EditorType.Oathbreaker) {
                        if(selectedCard.getRules().canBeOathbreaker())
                            return "lblasoathbreaker";
                        else
                            return "lblassignaturespell";
                    }
                    else
                        return "lblascommander";
            }
        }

        protected void addCommanderItems(final FDropDownMenu menu, final PaperCard card) {
            if(!parentScreen.isCommanderEditor())
                return;
            if(parentScreen.getMaxMovable(card, this, parentScreen.getCommanderPage()) <= 0)
                return;
            Localizer localizer = Forge.getLocalizer();
            String captionPrefix = localizer.getMessage("lblAddCommander");
            FImage icon = parentScreen.getCommanderPage().icon;
            if (canBeCommander(card)) {
                String captionSuffix;
                if(parentScreen.getEditorType() == EditorType.Oathbreaker)
                    captionSuffix = localizer.getMessage("lblasoathbreaker");
                else
                    captionSuffix = localizer.getMessage("lblascommander");
                String caption = String.join(" ", captionPrefix, captionSuffix);
                menu.addItem(new FMenuItem(caption, icon, e -> setCommander(card)));
            }
            if (canBePartnerCommander(card)) {
                String caption = String.join(" ", captionPrefix, localizer.getMessage("lblaspartnercommander"));
                menu.addItem(new FMenuItem(caption, icon, e -> setPartnerCommander(card)));
            }
            if (canBeSignatureSpell(card)) {
                String caption = String.join(" ", captionPrefix, localizer.getMessage("lblassignaturespell"));
                menu.addItem(new FMenuItem(caption, FSkinImage.SORCERY, e -> setSignatureSpell(card)));
            }
        }

        protected boolean needsCommander() {
            return parentScreen.isCommanderEditor() && parentScreen.getDeck().getCommanders().isEmpty();
        }

        protected boolean canBeCommander(final PaperCard card) {
            if(!parentScreen.isCommanderEditor() || parentScreen.getCommanderPage() == null)
                return false;
            if(parentScreen.getCommanderPage().cardManager.getPool().contains(card))
                return false; //Don't let it be the commander if it already is one.
            switch (parentScreen.editorType) {
                case Brawl:
                    return card.getRules().canBeBrawlCommander();
                case TinyLeaders:
                    return card.getRules().canBeTinyLeadersCommander();
                case Oathbreaker:
                    return card.getRules().canBeOathbreaker();
                case PlanarConquest:
                    return false; //don't set commander this way in Planar Conquest
                default:
                    return DeckFormat.Commander.isLegalCommander(card.getRules());
            }
        }

        protected boolean canBePartnerCommander(final PaperCard card) {
            if(!parentScreen.isCommanderEditor())
                return false;
            if(parentScreen.editorType == EditorType.Oathbreaker) {
                //FIXME: For now, simplify Oathbreaker by not supporting partners.
                //Needs support for tracking whose signature spell is whose, here and elsewhere.
                return false;
            }
            List<PaperCard> commanders = parentScreen.getDeck().get(DeckSection.Commander).toFlatList();
            commanders.removeIf((c) -> c.getRules().canBeSignatureSpell());
            if(commanders.size() != 1)
                return false;
            return commanders.get(0).getRules().canBePartnerCommanders(card.getRules());
        }

        protected boolean canBeSignatureSpell(final PaperCard card) {
            if(parentScreen.getEditorType() != EditorType.Oathbreaker)
                return false;
            PaperCard oathbreaker = parentScreen.getDeck().getOathbreaker();
            if(oathbreaker == null)
                return false;
            return card.getRules().canBeSignatureSpell() && card.getRules().getColorIdentity().hasNoColorsExcept(oathbreaker.getRules().getColorIdentity());
        }

        protected boolean canSideboard(final PaperCard card) {
            if(parentScreen.getSideboardPage() == null)
                return false;
            if(parentScreen.isLimitedEditor())
                return true;
            //Only allow sideboarding variant types in draft.
            //I don't know if that's correct.
            return DeckSection.matchingSection(card) == DeckSection.Main;
        }

        protected boolean canOnlyBePartnerCommander(final PaperCard card) {
            if (!parentScreen.isCommanderEditor()) {
                return false;
            }

            byte cmdCI = 0;
            for (final PaperCard p : parentScreen.getDeck().getCommanders()) {
                cmdCI |= p.getRules().getColorIdentity().getColor();
            }

            return !card.getRules().getColorIdentity().hasNoColorsExcept(cmdCI);
        }

        protected boolean canBeVanguard(final PaperCard card) {
            return DeckSection.matchingSection(card) == DeckSection.Avatar;
        }

        protected void setVanguard(PaperCard card) {
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            CardPool newPool = new CardPool();
            newPool.add(card);
            parentScreen.getPageForSection(DeckSection.Avatar).setCards(newPool);
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

    public static class CatalogPage extends CardManagerPage {
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

            if (!isVisible() && (parentScreen.getEditorType() != EditorType.Quest && parentScreen.getEditorType() != EditorType.QuestCommander)) {
                //delay refreshing while hidden unless for quest inventory
                needRefreshWhenShown = true;
                //Throw in the all cards that might be requested by other pages.
                cardManager.setPool(parentScreen.getDeck().getAllCardsInASinglePool(), true);
                return;
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
            switch (parentScreen.getEditorType()) {
            case Archenemy:
                return Forge.getLocalizer().getMessage("lblSchemes");
            case Planechase:
                return Forge.getLocalizer().getMessage("lblPlanes");
            default:
                return Forge.getLocalizer().getMessage("lblCards");
            }
        }

        public void scheduleRefresh() {
            if(isVisible())
                refresh();
            else
                this.needRefreshWhenShown = true;
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
            Predicate<PaperCard> additionalFilter = null;
            final EditorType editorType = parentScreen.getEditorType();
            Deck currentDeck = parentScreen.getDeck();
            switch (editorType) {
                case Archenemy:
                    cardManager.setPool(FModel.getArchenemyCards(), true);
                    break;
                case Planechase:
                    cardManager.setPool(FModel.getPlanechaseCards(), true);
                    break;
                case Quest:
                case QuestCommander:
                    ItemPool<PaperCard> questPool = new ItemPool<>(PaperCard.class);
                    questPool.addAll(FModel.getQuest().getCards().getCardpool());
                    // remove cards that are in the deck from the card pool
                    questPool.removeAll(currentDeck.getAllCardsInASinglePool(true, true));
                    if (editorType == EditorType.QuestCommander) {
                        List<PaperCard> commanders = currentDeck.getCommanders();
                        Predicate<PaperCard> filter;
                        String label;
                        if (commanders.isEmpty()) {
                            filter = DeckFormat.Commander.isLegalCommanderPredicate();
                            label = "lblCommanders";
                        }
                        else {
                            filter = DeckFormat.Commander.isLegalCardForCommanderPredicate(commanders);
                            label = "lblCards";
                        }
                        cardManager.setCaption(Forge.getLocalizer().getMessage(label));
                        questPool = editorType.applyCardFilter(questPool, filter);
                    }
                    cardManager.setPool(questPool);
                    break;
                case PlanarConquest:
                    cardManager.setPool(ConquestUtil.getAvailablePool(currentDeck));
                    break;
                case Commander:
                case Oathbreaker:
                case TinyLeaders:
                case Brawl:
                    final List<PaperCard> commanders = currentDeck.getCommanders();
                    if (commanders.isEmpty()) {
                        //if no commander set for deck, only show valid commanders
                        switch (editorType) {
                            case Commander:
                                additionalFilter = DeckFormat.Commander.isLegalCommanderPredicate();
                                cardManager.setCaption(Forge.getLocalizer().getMessage("lblCommanders"));
                                break;
                            case Oathbreaker:
                                additionalFilter = DeckFormat.Oathbreaker.isLegalCommanderPredicate();
                                cardManager.setCaption(Forge.getLocalizer().getMessage("lblOathbreakers"));
                                break;
                            case TinyLeaders:
                                additionalFilter = DeckFormat.TinyLeaders.isLegalCommanderPredicate();
                                cardManager.setCaption(Forge.getLocalizer().getMessage("lblCommanders"));
                                break;
                            case Brawl:
                                additionalFilter = DeckFormat.Brawl.isLegalCommanderPredicate();
                                cardManager.setCaption(Forge.getLocalizer().getMessage("lblCommanders"));
                                break;
                            default:
                                // Do nothing
                        }
                    } else {
                        //if a commander has been set, only show cards that match its color identity
                        switch (editorType) {
                            case Commander:
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
                        cardManager.setCaption(Forge.getLocalizer().getMessage("lblCards"));
                    }
                    // fall through to below
                default:
                    ItemPool<PaperCard> cardPool = cardManager.getWantUnique() ? FModel.getUniqueCardsNoAlt() : FModel.getAllCardsNoAlt();
                    //Dump all the variant cards our deck calls for into the card pool.
                    for(DeckSection variant : parentScreen.getVariantCardPools()) {
                        switch(variant) {
                            case Avatar: cardPool.addAll(FModel.getAvatarPool()); break;
                            case Conspiracy: cardPool.addAll(FModel.getConspiracyPool()); break;
                            case Planes: cardPool.addAll(FModel.getPlanechaseCards()); break;
                            case Schemes: cardPool.addAll(FModel.getArchenemyCards()); break;
                            case Dungeon: cardPool.addAll(FModel.getDungeonPool()); break;
                            case Attractions: cardPool.addAll(FModel.getAttractionPool()); break;
                        }
                    }
                    cardManager.setPool(editorType.applyCardFilter(cardPool, additionalFilter), true);
                    break;
            }
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            if(parentScreen.getMaxMovable(card, this, destinationPage) <= 0)
                return;
            if (needsCommander()) {
                setCommander(card); //handle special case of setting commander
                return;
            }
            if (destination == DeckSection.Avatar) {
                setVanguard(card);
                return;
            }
            if (canOnlyBePartnerCommander(card)) {
                return; //don't auto-change commander unexpectedly
            }
            if (!cardManager.isInfinite()) {
                removeCard(card);
            }
            destinationPage.addCard(card);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            if (card == null)
                return;

            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);

            if (!needsCommander() && !canOnlyBePartnerCommander(card) && !canBeVanguard(card)) {
                addMoveCardMenuItem(menu, this, destinationPage, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        destinationPage.addCard(card, result);
                    }
                });
                if (canSideboard(card)) {
                    addMoveCardMenuItem(menu, this, parentScreen.getSideboardPage(), new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            parentScreen.getSideboardPage().addCard(card, result);
                        }
                    });
                }
            }

            addCommanderItems(menu, card);

            if(canBeVanguard(card)) {
                Localizer localizer = Localizer.getInstance();
                String caption = String.join(" ", localizer.getMessage("lblAddCommander"), localizer.getMessage("lblasavatar"));
                menu.addItem(new FMenuItem(caption, destinationPage.getIcon(), e -> setVanguard(card)));
            }

            if (parentScreen.getEditorType() == EditorType.Constructed) {
                //add option to add or remove card from favorites
                final CardPreferences prefs = CardPreferences.getPrefs(card);
                if (prefs.getStarCount() == 0) {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAddFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_FILLED : FSkinImage.STAR_FILLED, e -> {
                        prefs.setStarCount(1);
                        CardPreferences.save();
                    }));
                } else {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRemoveFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_OUTLINE : FSkinImage.STAR_OUTLINE, e -> {
                        prefs.setStarCount(0);
                        CardPreferences.save();
                    }));
                }

                //if card has more than one art option, add item to change user's preferred art
                final List<PaperCard> artOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                if (artOptions.size() > 1) {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblChangePreferredArt"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, e -> {
                        //sort options so current option is on top and selected by default
                        List<PaperCard> sortedOptions = new ArrayList<>();
                        sortedOptions.add(card);
                        for (PaperCard option : artOptions) {
                            if (option != card) {
                                sortedOptions.add(option);
                            }
                        }
                        GuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblSelectPreferredArt") + " " + card.getName(), sortedOptions, new Callback<PaperCard>() {
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
                    }));
                }
            }
        }

        @Override
        protected void buildDeckMenu(FPopupMenu menu) {
            if (cardManager.getConfig().getShowUniqueCardsOption()) {
                menu.addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblUniqueCardsOnly"), cardManager.getWantUnique(), e -> {
                    boolean wantUnique = !cardManager.getWantUnique();
                    cardManager.setWantUnique(wantUnique);
                    refresh();
                    cardManager.getConfig().setUniqueCardsOnly(wantUnique);
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

            deckSection = deckSection0;
            final Localizer localizer = Forge.getLocalizer();
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
            case Conspiracy:
                captionPrefix = localizer.getMessage("lblConspiracies");
                cardManager.setCaption(localizer.getMessage("lblConspiracies"));
                icon = FSkinImage.UNKNOWN; //TODO: This and the other extra sections definitely need better icons.
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
            case Attractions:
                captionPrefix = localizer.getMessage("lblAttractions");
                cardManager.setCaption(localizer.getMessage("lblAttractions"));
                icon = FSkinImage.TICKET;
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
        public void addCard(PaperCard card, int qty) {
            super.addCard(card, qty);
            if(parentScreen.hiddenExtraSections.contains(this.deckSection))
                parentScreen.showExtraSectionTab(this.deckSection);
        }
        @Override
        public void addCards(Iterable<Entry<PaperCard, Integer>> cards) {
            super.addCards(cards);
            if(parentScreen.hiddenExtraSections.contains(this.deckSection))
                parentScreen.showExtraSectionTab(this.deckSection);
        }

        @Override
        public void setCards(CardPool cards) {
            super.setCards(cards);
            if(parentScreen.hiddenExtraSections.contains(this.deckSection) && !cards.isEmpty())
                parentScreen.showExtraSectionTab(this.deckSection);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            switch (deckSection) {
            case Main:
            case Planes:
            case Schemes:
            case Attractions:
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
            FSkinImage iconReplaceCard = Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST;
            final Localizer localizer = Forge.getLocalizer();
            String lblReplaceCard = localizer.getMessage("lblReplaceCard");

            CardManagerPage cardSourceSection;
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            // val for colorID setup
            int val;
            switch (deckSection) {
            default:
            case Main:
                cardSourceSection = parentScreen.isLimitedEditor() ? parentScreen.getSideboardPage() : parentScreen.getCatalogPage();
                addMoveCardMenuItem(menu, cardSourceSection, this, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        cardSourceSection.removeCard(card, result); //ensure card removed from sideboard before adding to main
                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addMoveCardMenuItem(menu, this, cardSourceSection, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            cardSourceSection.addCard(card, result);
                        }
                    });
                }
                if (parentScreen.getSideboardPage() != null) {
                    addMoveCardMenuItem(menu, this, parentScreen.getSideboardPage(), new Callback<Integer>() {
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
                    if (cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(lblReplaceCard, iconReplaceCard, e -> handleReplaceCard(card, cardOptions)));
                    }
                }
                addCommanderItems(menu, card);
                if ((val = card.getRules().getSetColorID()) > 0) {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblColorIdentity"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, e -> {
                        //sort options so current option is on top and selected by default
                        Set<String> colorChoices = new HashSet<>(MagicColor.Constant.ONLY_COLORS);
                        GuiChoose.getChoices(Forge.getLocalizer().getMessage("lblChooseAColor", Lang.getNumeral(val)), val, val, colorChoices, new Callback<>() {
                            @Override
                            public void run(List<String> result) {
                                addCard(card.getColorIDVersion(new HashSet<>(result)));
                                removeCard(card);
                            }
                        });
                    }));
                }
                break;
            case Sideboard:
                cardSourceSection = parentScreen.isLimitedEditor() ? parentScreen.getMainDeckPage() : parentScreen.getCatalogPage();
                addMoveCardMenuItem(menu, cardSourceSection, this, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        cardSourceSection.removeCard(card, result); //ensure card removed from main deck before adding to sideboard
                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addMoveCardMenuItem(menu, this, cardSourceSection, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            if (result == null || result <= 0) { return; }

                            removeCard(card, result);
                            cardSourceSection.addCard(card, result);
                        }
                    });
                }
                addMoveCardMenuItem(menu, this, destinationPage, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        destinationPage.addCard(card, result);
                    }
                });
                if (parentScreen.isAllowedReplacement()) {
                    final List<PaperCard> cardOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                    if (cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(lblReplaceCard, iconReplaceCard, e -> handleReplaceCard(card, cardOptions)));
                    }
                }
                addCommanderItems(menu, card);
                if ((val = card.getRules().getSetColorID()) > 0) {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblColorIdentity"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, e -> {
                        //sort options so current option is on top and selected by default
                        Set<String> colorChoices = new HashSet<>(MagicColor.Constant.ONLY_COLORS);
                        GuiChoose.getChoices(Forge.getLocalizer().getMessage("lblChooseAColor", Lang.getNumeral(val)), val, val, colorChoices, new Callback<>() {
                            @Override
                            public void run(List<String> result) {
                                addCard(card.getColorIDVersion(new HashSet<>(result)));
                                removeCard(card);
                            }
                        });
                    }));
                }
                break;
            case Commander:
                if (parentScreen.editorType != EditorType.PlanarConquest || isPartnerCommander(card)) {
                    addMoveCardMenuItem(menu, this, parentScreen.getCatalogPage(), new Callback<Integer>() {
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
                    if (cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(lblReplaceCard, iconReplaceCard, e -> handleReplaceCard(card, cardOptions)));
                    }
                }
                break;
            case Avatar:
                addMoveCardMenuItem(menu, this, parentScreen.getCatalogPage(), new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Schemes:
                addMoveCardMenuItem(menu, parentScreen.getCatalogPage(), this, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                addMoveCardMenuItem(menu, this, parentScreen.getCatalogPage(), new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Planes:
            case Attractions:
                addMoveCardMenuItem(menu, this, parentScreen.getCatalogPage(), new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                if (parentScreen.isAllowedReplacement()) {
                    final List<PaperCard> cardOptions = FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(card.getName());
                    if (cardOptions.size() > 1) {
                        menu.addItem(new FMenuItem(lblReplaceCard, iconReplaceCard, e -> handleReplaceCard(card, cardOptions)));
                    }
                }
                break;
            }
        }

        private void handleReplaceCard(PaperCard card, List<PaperCard> cardOptions) {
            //sort options so current option is on top and selected by default
            List<PaperCard> sortedOptions = new ArrayList<>();
            sortedOptions.add(card);
            for (PaperCard option : cardOptions) {
                if (option != card) {
                    sortedOptions.add(option);
                }
            }
            String prompt = Forge.getLocalizer().getMessage("lblSelectReplacementCard") + " " + card.getName();
            GuiChoose.oneOrNone(prompt, sortedOptions, new Callback<PaperCard>() {
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

        private boolean isPartnerCommander(final PaperCard card) {
            if (!parentScreen.isCommanderEditor() || parentScreen.getDeck().getCommanders().isEmpty()) {
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
            caption = Forge.getLocalizer().getMessage("lblPackN", String.valueOf(packNumber));
            cardManager.setPool(pool);
            cardManager.setShowRanking(true);
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
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            addMoveCardMenuItem(menu, this, destinationPage, new Callback<Integer>() {
                @Override
                public void run(Integer result) { //ignore quantity
                    DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
                    if(destinationPage == null)
                        destinationPage = parentScreen.showExtraSectionTab(destination);
                    destinationPage.addCard(card);
                    afterCardPicked(card);
                }
            });
            addMoveCardMenuItem(menu, this, parentScreen.getSideboardPage(), new Callback<Integer>() {
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
                    name = "[" + Forge.getLocalizer().getMessage("lblNewDeck") + "]";
                }
                if (!saved && editor.allowsSave()) {
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

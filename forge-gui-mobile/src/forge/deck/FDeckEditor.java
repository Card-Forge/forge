package forge.deck;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;
import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.assets.*;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.io.DeckPreferences;
import forge.game.GameLog;
import forge.game.GameLogEntryType;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.DraftPack;
import forge.gamemodes.limited.IDraftLog;
import forge.gamemodes.limited.LimitedPlayer;
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
import forge.localinstance.skin.FSkinProp;
import forge.menu.*;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.match.views.VLog;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.util.*;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    public static FSkinImage MAIN_DECK_ICON = Forge.hdbuttons ? FSkinImage.HDLIBRARY :FSkinImage.DECKLIST;
    public static FSkinImage SIDEBOARD_ICON = Forge.hdbuttons ? FSkinImage.HDSIDEBOARD : FSkinImage.FLASHBACK;
    private static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);

    //Toggle that suppresses most conformity logic in the editor.
    public static final String DECK_TAG_SUPPRESS_CONFORMITY = "noEditorConformity";

    public static abstract class DeckEditorConfig {
        public abstract GameType getGameType();
        public DeckFormat getDeckFormat() {
            GameType gameType = getGameType();
            return gameType == null ? null : gameType.getDeckFormat();
        }

        public ItemPool<PaperCard> getCardPool() {
            return FModel.getAllCardsNoAlt();
        }
        protected Predicate<PaperCard> getCardFilter() { return null; }

        public boolean isLimited() { return false; } //TODO: Figure out if I can derive this from gameType; Has different meaning for Quest...
        public boolean usePlayerInventory() { return false; }
        public boolean isDraft() { return false; } //Draft and quest draft
        public boolean hasInfiniteCardPool() { return !isLimited() && !usePlayerInventory(); }
        public boolean hasCommander() {
            DeckFormat deckFormat = getDeckFormat();
            return deckFormat != null && deckFormat.hasCommander();
        }
        public boolean allowsCardReplacement() { return hasInfiniteCardPool() || usePlayerInventory(); }

        public List<CardEdition> getBasicLandSets(Deck currentDeck) {
            if(hasInfiniteCardPool())
                return FModel.getMagicDb().getSortedEditions().stream().filter(CardEdition::hasBasicLands).collect(Collectors.toList());
            return List.of(DeckProxy.getDefaultLandSet(currentDeck));
        }

        protected abstract IDeckController getController();
        protected abstract DeckEditorPage[] getInitialPages();

        public DeckSection[] getPrimarySections() {
            if(getGameType() != null)
                return getGameType().getPrimaryDeckSections().toArray(new DeckSection[0]);
            return new DeckSection[]{DeckSection.Main, DeckSection.Sideboard};
        }

        public DeckSection[] getExtraSections() {
            if(getGameType() != null)
                return getGameType().getSupplimentalDeckSections().toArray(new DeckSection[0]);
            return new DeckSection[]{DeckSection.Attractions, DeckSection.Contraptions};
        }

        private IDeckController initDeckController(String editDeckName, String editDeckPath) {
            if(StringUtils.isEmpty(editDeckName))
                return initDeckController();
            IDeckController controller = this.getController();
            if(!(controller instanceof FileDeckController<?>))
                throw new UnsupportedOperationException("Tried to load a deck by file name without a FileDeckController.");
            controller.setEditor(null);
            ((FileDeckController<?>) controller).load(editDeckPath, editDeckName);
            return controller;
        }

        private IDeckController initDeckController(Deck newDeck) {
            IDeckController controller = this.getController();
            controller.setEditor(null);
            if(newDeck != null)
                controller.setDeck(newDeck);
            else
                controller.newDeck();
            return controller;
        }

        private IDeckController initDeckController(DeckGroup newDecks) {
            IDeckController controller = this.getController();
            if(newDecks == null) {
                controller.newDeck();
                return controller;
            }
            if(!(controller instanceof FileDeckGroupController fileController))
                return initDeckController(newDecks.getHumanDeck());
            fileController.setDeckGroup(newDecks);
            return controller;
        }

        private IDeckController initDeckController() {
            IDeckController controller = this.getController();
            controller.setEditor(null);
            controller.newDeck();
            return controller;
        }
    }

    public static class GameTypeDeckEditorConfig extends DeckEditorConfig {
        final GameType gameType;
        final DeckFormat deckFormat;
        final IDeckController controller;

        boolean usePlayerInventory = false;
        Predicate<PaperCard> cardFilter = null;
        ItemManagerConfig catalogConfig = null;
        ItemManagerConfig mainSectionConfig = null;
        ItemManagerConfig sideboardConfig = null;
        Function<Deck, Collection<CardEdition>> fnGetBasicLandSet = null;
        Supplier<ItemPool<PaperCard>> itemPoolSupplier = null;
        String catalogCaption = null;

        public GameTypeDeckEditorConfig(GameType gameType, IDeckController controller) {
            this.gameType = gameType;
            this.deckFormat = gameType.getDeckFormat();
            this.controller = controller;
        }

        @Override public GameType getGameType() { return gameType; }
        @Override public DeckFormat getDeckFormat() { return deckFormat; }
        @Override public IDeckController getController() { return controller; }
        @Override public Predicate<PaperCard> getCardFilter() { return cardFilter; }
        @Override public boolean isLimited() { return deckFormat == DeckFormat.Limited; }
        @Override public boolean isDraft() { return gameType.isDraft(); }
        @Override public boolean hasCommander() { return deckFormat.hasCommander(); }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            if(this.itemPoolSupplier != null)
                return itemPoolSupplier.get();
            return super.getCardPool();
        }

        @Override
        public boolean usePlayerInventory() {
            return this.usePlayerInventory;
        }

        public GameTypeDeckEditorConfig setCardFilter(Predicate<PaperCard> cardFilter) {
            this.cardFilter = cardFilter;
            return this;
        }

        public GameTypeDeckEditorConfig setCatalogConfig(ItemManagerConfig catalogConfig) {
            return setCatalogConfig(catalogConfig, null);
        }
        public GameTypeDeckEditorConfig setCatalogConfig(ItemManagerConfig catalogConfig, String captionKey) {
            this.catalogConfig = catalogConfig;
            this.catalogCaption = captionKey;
            return this;
        }

        public GameTypeDeckEditorConfig setMainSectionConfig(ItemManagerConfig mainSectionConfig) {
            this.mainSectionConfig = mainSectionConfig;
            return this;
        }
        public GameTypeDeckEditorConfig setSideboardConfig(ItemManagerConfig sideboardConfig) {
            this.sideboardConfig = sideboardConfig;
            return this;
        }
        public GameTypeDeckEditorConfig setBasicLandSetFunction(Function<Deck, Collection<CardEdition>> fnGetBasicLandSet) {
            this.fnGetBasicLandSet = fnGetBasicLandSet;
            return this;
        }
        public GameTypeDeckEditorConfig setItemPoolSupplier(Supplier<ItemPool<PaperCard>> itemPoolSupplier) {
            this.itemPoolSupplier = itemPoolSupplier;
            return this;
        }
        public GameTypeDeckEditorConfig setPlayerInventorySupplier(Supplier<ItemPool<PaperCard>> itemPoolSupplier) {
            this.itemPoolSupplier = itemPoolSupplier;
            this.usePlayerInventory = true;
            return this;
        }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            CardManagerPage catalogPage = null, mainPage = null, sideboardPage = null, commanderPage = null;

            EnumSet<DeckSection> primarySections = gameType.getPrimaryDeckSections();

            //Catalog page.
            CardManager catalogCM = new CardManager(false);
            if(catalogConfig != null) {
                catalogCM.setWantUnique(catalogConfig.getUniqueCardsOnly());
                if(catalogConfig == ItemManagerConfig.QUEST_EDITOR_POOL)
                    catalogPage = new CatalogPage(catalogCM, ItemManagerConfig.QUEST_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), FSkinImage.QUEST_BOX);
                else if(catalogConfig == ItemManagerConfig.CONQUEST_COLLECTION)
                    catalogPage = new CatalogPage(catalogCM, ItemManagerConfig.CONQUEST_COLLECTION, Forge.getLocalizer().getMessage("lblCollection"), FSkinImage.SPELLBOOK);
                else
                    catalogPage = new CatalogPage(catalogCM, catalogConfig);
            }
            else if(!isLimited()) {
                //Default, add a standard catalog page
                catalogPage = new CatalogPage(catalogCM, ItemManagerConfig.CARD_CATALOG);
            }
            else if(isDraft()) {
                catalogPage = new DraftPackPage(catalogCM);
            }

            if(catalogCaption != null && catalogPage != null)
                catalogPage.setItemManagerCaption(catalogCaption);

            //Main deck section
            CardManager mainCM = new CardManager(false);
            if(mainSectionConfig != null) {
                mainCM.setWantUnique(mainSectionConfig.getUniqueCardsOnly());
                if(mainSectionConfig == ItemManagerConfig.CONQUEST_DECK_EDITOR)
                    mainPage = new DeckSectionPage(mainCM, DeckSection.Main, ItemManagerConfig.CONQUEST_DECK_EDITOR, Forge.getLocalizer().getMessage("lblDeck"), Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST);
                else
                    mainPage = new DeckSectionPage(mainCM, DeckSection.Main, mainSectionConfig);
            }
            else if(primarySections.contains(DeckSection.Main)) {
                mainPage = new DeckSectionPage(mainCM, DeckSection.Main);
            }

            //Sideboard section
            CardManager sideCM = new CardManager(false);
            if(sideboardConfig != null) {
                sideCM.setWantUnique(sideboardConfig.getUniqueCardsOnly());
                sideboardPage = new DeckSectionPage(sideCM, DeckSection.Sideboard, sideboardConfig);
            }
            else if(primarySections.contains(DeckSection.Sideboard)) {
                if(isDraft())
                    sideboardPage = new DeckSectionPage(sideCM, DeckSection.Sideboard, ItemManagerConfig.DRAFT_POOL);
                else
                    sideboardPage = new DeckSectionPage(sideCM, DeckSection.Sideboard);
            }

            //Commander section
            if(primarySections.contains(DeckSection.Commander)) {
                CardManager commanderCM = new CardManager(false);
                if(gameType == GameType.Oathbreaker)
                    commanderPage = new DeckSectionPage(commanderCM, DeckSection.Commander, ItemManagerConfig.OATHBREAKER_SECTION, Forge.getLocalizer().getMessage("lblOathbreaker"), FSkinImage.COMMANDER);
                else
                    commanderPage = new DeckSectionPage(commanderCM, DeckSection.Commander, ItemManagerConfig.COMMANDER_SECTION);
            }

            List<DeckEditorPage> pages = new ArrayList<>(5);
            pages.add(catalogPage);
            if(Forge.isLandscapeMode()) {
                pages.add(commanderPage);
                pages.add(mainPage);
            }
            else {
                pages.add(mainPage);
                pages.add(commanderPage);
            }
            pages.add(sideboardPage);
            pages.removeIf(Objects::isNull);

            //Any extra pages.
            primarySections.stream()
                    .filter(e -> e != DeckSection.Main && e != DeckSection.Sideboard && e != DeckSection.Commander)
                    .map(e -> createPageForExtraSection(e, this))
                    .forEach(pages::add);

            return pages.toArray(new DeckEditorPage[0]);
        }

        @Override
        public DeckSection[] getPrimarySections() {
            return gameType.getPrimaryDeckSections().toArray(new DeckSection[0]);
        }

        @Override
        public DeckSection[] getExtraSections() {
            return gameType.getSupplimentalDeckSections().toArray(new DeckSection[0]);
        }

        @Override
        public List<CardEdition> getBasicLandSets(Deck currentDeck) {
            if(this.fnGetBasicLandSet != null)
                return List.copyOf(fnGetBasicLandSet.apply(currentDeck));
            return super.getBasicLandSets(currentDeck);
        }
    }

    public static DeckEditorConfig EditorConfigConstructed = new GameTypeDeckEditorConfig(GameType.Constructed,
            new FileDeckController<>(FModel.getDecks().getConstructed(), Deck::new, DeckPreferences::setCurrentDeck));

    public static final FileDeckGroupController DECK_CONTROLLER_DRAFT = new FileDeckGroupController(FModel.getDecks().getDraft(),
            DeckGroup::new, DeckPreferences::setDraftDeck);
    public static DeckEditorConfig EditorConfigDraft = new GameTypeDeckEditorConfig(GameType.Draft, DECK_CONTROLLER_DRAFT);

    public static DeckEditorConfig EditorConfigSealed = new GameTypeDeckEditorConfig(GameType.Sealed,
            new FileDeckGroupController(FModel.getDecks().getSealed(), DeckGroup::new, DeckPreferences::setSealedDeck))
            .setSideboardConfig(ItemManagerConfig.SEALED_POOL);
    public static DeckEditorConfig EditorConfigWinston = new GameTypeDeckEditorConfig(GameType.Winston,
            new FileDeckGroupController(FModel.getDecks().getWinston(), DeckGroup::new, null));
    public static DeckEditorConfig EditorConfigCommander = new GameTypeDeckEditorConfig(GameType.Commander,
            new FileDeckController<>(FModel.getDecks().getCommander(), Deck::new, DeckPreferences::setCommanderDeck));
    public static DeckEditorConfig EditorConfigOathbreaker = new GameTypeDeckEditorConfig(GameType.Oathbreaker,
            new FileDeckController<>(FModel.getDecks().getOathbreaker(), Deck::new, DeckPreferences::setOathbreakerDeck));
    public static DeckEditorConfig EditorConfigTinyLeaders = new GameTypeDeckEditorConfig(GameType.TinyLeaders,
            new FileDeckController<>(FModel.getDecks().getTinyLeaders(), Deck::new, DeckPreferences::setTinyLeadersDeck))
            .setCardFilter(DeckFormat.TinyLeaders.isLegalCardPredicate());

    public static DeckEditorConfig EditorConfigBrawl = new GameTypeDeckEditorConfig(GameType.Brawl,
            new FileDeckController<>(FModel.getDecks().getBrawl(), Deck::new, DeckPreferences::setBrawlDeck))
            .setCardFilter(DeckFormat.Brawl.isLegalCardPredicate());

    public static DeckEditorConfig EditorConfigArchenemy = new GameTypeDeckEditorConfig(GameType.Archenemy,
            new FileDeckController<>(FModel.getDecks().getScheme(), Deck::new, DeckPreferences::setSchemeDeck))
            .setCatalogConfig(ItemManagerConfig.SCHEME_POOL, "lblSchemes")
            .setItemPoolSupplier(FModel::getArchenemyCards);
    public static DeckEditorConfig EditorConfigPlanechase = new GameTypeDeckEditorConfig(GameType.Planechase,
            new FileDeckController<>(FModel.getDecks().getPlane(), Deck::new, DeckPreferences::setPlanarDeck))
            .setCatalogConfig(ItemManagerConfig.PLANAR_POOL, "lblPlanes")
            .setItemPoolSupplier(FModel::getPlanechaseCards);


    private static final Consumer<String> fnUpdateQuestDeck = deckStr -> {
        FModel.getQuest().setCurrentDeck(deckStr);
        FModel.getQuest().save();
    };
    //These are configured when the respective adventures are loaded.
    public static final FileDeckController<Deck> DECK_CONTROLLER_QUEST = new FileDeckController<>(null, Deck::new, fnUpdateQuestDeck);
    public static final FileDeckGroupController DECK_CONTROLLER_QUEST_DRAFT = new FileDeckGroupController(null, DeckGroup::new, fnUpdateQuestDeck);
    public static final FileDeckController<Deck> DECK_CONTROLLER_PLANAR_CONQUEST = new FileDeckController<>(null, Deck::new, null);
    public static DeckEditorConfig EditorConfigQuest = new GameTypeDeckEditorConfig(GameType.Quest, DECK_CONTROLLER_QUEST)
            .setCatalogConfig(ItemManagerConfig.QUEST_EDITOR_POOL)
            .setMainSectionConfig(ItemManagerConfig.QUEST_DECK_EDITOR)
            .setSideboardConfig(ItemManagerConfig.QUEST_DECK_EDITOR)
            .setPlayerInventorySupplier(() -> FModel.getQuest().getCards().getCardpool())
            .setBasicLandSetFunction(d -> FModel.getQuest().getAvailableLandSets());
    public static DeckEditorConfig EditorConfigQuestCommander = new GameTypeDeckEditorConfig(GameType.QuestCommander, DECK_CONTROLLER_QUEST)
            .setCatalogConfig(ItemManagerConfig.QUEST_EDITOR_POOL)
            .setMainSectionConfig(ItemManagerConfig.QUEST_DECK_EDITOR)
            .setSideboardConfig(ItemManagerConfig.QUEST_DECK_EDITOR)
            .setPlayerInventorySupplier(() -> FModel.getQuest().getCards().getCardpool())
            .setBasicLandSetFunction(d -> FModel.getQuest().getAvailableLandSets());
    public static DeckEditorConfig EditorConfigQuestDraft = new GameTypeDeckEditorConfig(GameType.QuestDraft, DECK_CONTROLLER_QUEST_DRAFT);
    public static DeckEditorConfig EditorConfigPlanarConquest = new GameTypeDeckEditorConfig(GameType.PlanarConquest, DECK_CONTROLLER_PLANAR_CONQUEST)
            .setCatalogConfig(ItemManagerConfig.CONQUEST_COLLECTION)
            .setMainSectionConfig(ItemManagerConfig.CONQUEST_DECK_EDITOR)
            .setPlayerInventorySupplier(ConquestUtil::getAvailablePool)
            .setBasicLandSetFunction(ConquestUtil::getBasicLandSets);

    protected static DeckSectionPage createPageForExtraSection(DeckSection deckSection, DeckEditorConfig editorConfig) {
        CardManager cm = new CardManager(false);
        return switch (deckSection) {
            case Avatar, Commander -> new DeckSectionPage(cm, deckSection, ItemManagerConfig.COMMANDER_SECTION);
            case Schemes -> new DeckSectionPage(cm, deckSection, ItemManagerConfig.SCHEME_DECK_EDITOR);
            case Planes -> new DeckSectionPage(cm, deckSection, ItemManagerConfig.PLANAR_DECK_EDITOR);
            case Conspiracy ->
                    new DeckSectionPage(cm, deckSection, editorConfig.isLimited() ? ItemManagerConfig.DRAFT_CONSPIRACY : ItemManagerConfig.CONSPIRACY_DECKS);
            case Dungeon -> new DeckSectionPage(cm, deckSection, ItemManagerConfig.DUNGEON_DECKS);
            case Attractions -> {
                if (editorConfig.isLimited())
                    yield new DeckSectionPage(cm, deckSection, ItemManagerConfig.ATTRACTION_DECK_EDITOR_LIMITED);
                yield new DeckSectionPage(cm, deckSection, ItemManagerConfig.ATTRACTION_DECK_EDITOR);
            }
            case Contraptions -> {
                if (editorConfig.isLimited())
                    yield new DeckSectionPage(cm, deckSection, ItemManagerConfig.CONTRAPTION_DECK_EDITOR_LIMITED);
                yield new DeckSectionPage(cm, deckSection, ItemManagerConfig.CONTRAPTION_DECK_EDITOR);
            }
            default -> {
                System.out.printf("Editor (%s) added an unsupported extra deck section - %s%n", deckSection, editorConfig.getGameType());
                yield new DeckSectionPage(cm, deckSection);
            }
        };
    }

    public static FImage iconFromDeckSection(DeckSection deckSection) {
        return FSkin.getImages().get(FSkinProp.iconFromDeckSection(deckSection, Forge.hdbuttons));
    }

    private final DeckEditorConfig editorConfig;
    private final IDeckController deckController;
    private Deck deck;
    private final List<DeckSection> hiddenExtraSections = new ArrayList<>();
    protected CatalogPage catalogPage;
    protected DeckSectionPage mainDeckPage;
    protected DeckSectionPage sideboardPage;
    protected DeckSectionPage commanderPage;
    private final Map<DeckSection, DeckSectionPage> pagesBySection = new EnumMap<>(DeckSection.class);
    private final Set<DeckSection> variantCardPools = new HashSet<>();
    private FEventHandler saveHandler;

    protected final DeckHeader deckHeader;

    private boolean tabsInitialized = false;

    public FDeckEditor(DeckEditorConfig editorConfig, String editDeckName) {
        this(editorConfig, editorConfig.initDeckController(editDeckName, ""));
    }
    public FDeckEditor(DeckEditorConfig editorConfig, DeckProxy editDeck) {
        this(editorConfig, editorConfig.initDeckController(editDeck.getName(), editDeck.getPath()));
    }
    public FDeckEditor(DeckEditorConfig editorConfig, Deck newDeck) {
        this(editorConfig, editorConfig.initDeckController(newDeck));
    }
    public FDeckEditor(DeckEditorConfig editorConfig, DeckGroup newDeckGroup) {
        this(editorConfig, editorConfig.initDeckController(newDeckGroup));
    }

    protected FDeckEditor(DeckEditorConfig editorConfig, IDeckController deckController) {
        super(editorConfig.getInitialPages());
        this.editorConfig = editorConfig;

        this.deckHeader = initDeckHeader();
        this.cacheTabPages();

        this.deckController = deckController;
        deckController.setEditor(this);

        if(isLimitedEditor()) {
            if(deckController.getDeck() == null)
                this.deck = new Deck();
            if(isDraftEditor() && !isDrafting() && catalogPage != null)
                catalogPage.hideTab(); //hide Draft Pack page if editing existing draft deck
        }

        //Initialize pages.
        tabPages.stream().map(DeckEditorPage.class::cast).forEach(DeckEditorPage::initialize);
        tabsInitialized = true;

        //Pick an initial page to show. Test for catalog page so we don't switch off a draft screen.
        if(!tabPages.isEmpty() && tabPages.get(0) instanceof CatalogPage) { //DraftPackPage is also a CatalogPage
            if(mainDeckPage != null && mainDeckPage.cardManager.getItemCount() > 0)
                setSelectedPage(mainDeckPage);
            else if(sideboardPage != null && sideboardPage.cardManager.getItemCount() > 0)
                setSelectedPage(sideboardPage);
        }

        //Show any extra sections that are currently in use.
        for(DeckSection section : editorConfig.getExtraSections()) {
            if (deck != null && deck.has(section))
                this.showExtraSectionTab(section);
            else {
                this.hiddenExtraSections.add(section);
                this.createExtraSectionPage(section).hideTab();
            }
        }
        //Update the card pool if we now need to mix variant cards into it.
        if(!this.getVariantCardPools().isEmpty() && editorConfig.hasInfiniteCardPool())
            getCatalogPage().scheduleRefresh();

        deckHeader.btnSave.setCommand(e -> save(null));
        updateDeckHeader();

        deckHeader.btnMoreOptions.setCommand(fEvent -> {
            FPopupMenu menu = createMoreOptionsMenu();
            menu.show(deckHeader.btnMoreOptions, 0, deckHeader.btnMoreOptions.getHeight());
        });
    }

    protected void cacheTabPages() {
        //cache specific pages
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            if (tabPage instanceof CatalogPage && catalogPage == null) {
                catalogPage = (CatalogPage) tabPage;
            }
            else if (tabPage instanceof DeckSectionPage deckSectionPage) {
                pagesBySection.put(deckSectionPage.deckSection, deckSectionPage);
                switch (deckSectionPage.deckSection) {
                    case Main:
                        mainDeckPage = deckSectionPage;
                        break;
                    case Schemes:
                    case Planes:
                        if(mainDeckPage == null)
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
    }

    protected DeckHeader initDeckHeader() {
        return add(new DeckHeader());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        if (deckHeader.isVisible()) {
            deckHeader.setBounds(0, startY, width, HEADER_HEIGHT);
            startY += HEADER_HEIGHT;
        }
        super.doLayout(startY, width, height);
    }

    protected void updateDeckHeader() {
        boolean allowSave = allowSave();
        deckHeader.btnSave.setEnabled(allowSave);
        deckHeader.btnSave.setVisible(allowSave);
        deckHeader.revalidate();
    }

    public void setHeaderText(String headerText) {
        this.deckHeader.lblName.setText(headerText);
    }

    protected FPopupMenu createMoreOptionsMenu() {
        IDeckController deckController = this.getDeckController();
        return new FPopupMenu() {
            @Override
            protected void buildMenu() {
                final Localizer localizer = Forge.getLocalizer();
                if (allowAddBasic())
                    addItem(new FMenuItem(localizer.getMessage("lblAddBasicLands"), FSkinImage.LANDLOGO, e -> showAddBasicLandsDialog()));
                if (showAddExtraSectionOption()) {
                    addItem(new FMenuItem(localizer.getMessage("lblAddDeckSection"), FSkinImage.CHAOS, e -> {
                        List<String> options = hiddenExtraSections.stream().map(DeckSection::getLocalizedName).collect(Collectors.toList());
                        GuiChoose.oneOrNone(localizer.getMessage("lblAddDeckSectionSelect"), options, result -> {
                            if (result == null || !options.contains(result))
                                return;
                            DeckSection newSection = hiddenExtraSections.get(options.indexOf(result));
                            showExtraSectionTab(newSection);
                            filterCatalogForExtraSection(newSection);
                            getCatalogPage().scheduleRefresh();
                            setSelectedPage(getCatalogPage());
                        });
                    }));
                }
                if (editorConfig.hasInfiniteCardPool() || editorConfig.usePlayerInventory()) {
                    addItem(new FMenuItem(localizer.getMessage("lblImportFromClipboard"), Forge.hdbuttons ? FSkinImage.HDIMPORT : FSkinImage.OPEN, e -> {
                        FDeckImportDialog dialog = new FDeckImportDialog(deck, FDeckEditor.this.editorConfig);
                        if(editorConfig.usePlayerInventory())
                            dialog.setFreePrintConverter(FDeckEditor.this::supplyPrintForImporter);
                        dialog.setImportBannedCards(!FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY));
                        dialog.setCallback(importedDeck -> {
                            if (deck != null && importedDeck.hasName()) {
                                deck.setName(importedDeck.getName());
                                setHeaderText(importedDeck.getName());
                            }
                            switch (dialog.getImportBehavior()) {
                                case REPLACE_CURRENT:
                                    for(DeckSectionPage page : pagesBySection.values()) {
                                        if(importedDeck.has(page.deckSection)) {
                                            page.setCards(importedDeck.get(page.deckSection));
                                            if(hiddenExtraSections.contains(page.deckSection))
                                                showExtraSectionTab(page.deckSection);
                                        }
                                        else
                                            page.setCards(new CardPool());
                                    }
                                    break;
                                case CREATE_NEW:
                                    deckController.setDeck(importedDeck);
                                    break;
                                case MERGE:
                                    for (Entry<DeckSection, CardPool> section : importedDeck) {
                                        DeckSectionPage page = getPageForSection(section.getKey());
                                        if (page != null)
                                            page.addCards(section.getValue());
                                    }
                            }
                        });
                        dialog.initParse();
                        dialog.show();
                        setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck if visible below dialog
                    }));
                    if (allowSaveAs())
                        addItem(new FMenuItem(localizer.getMessage("lblSaveAs"), Forge.hdbuttons ? FSkinImage.HDSAVEAS : FSkinImage.SAVEAS, e -> {
                            String defaultName = deckController.getNextAvailableName();
                            FOptionPane.showInputDialog(localizer.getMessage("lblNameNewCopyDeck"), defaultName, result -> {
                                if (!StringUtils.isEmpty(result)) {
                                    deckController.saveAs(result);
                                }
                            });
                        }));
                }
                if (allowRename()) {
                    addItem(new FMenuItem(localizer.getMessage("lblRenameDeck"), Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT, e -> FOptionPane.showInputDialog(
                            localizer.getMessage("lblNewNameDeck"), deck.getName(), deckController::rename))
                    );
                }
                if (allowDelete()) {
                    addItem(new FMenuItem(localizer.getMessage("lblDeleteDeck"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, e -> FOptionPane.showConfirmDialog(
                            localizer.getMessage("lblConfirmDelete") + " '" + deck.getName() + "'?",
                            localizer.getMessage("lblDeleteDeck"),
                            localizer.getMessage("lblDelete"),
                            localizer.getMessage("lblCancel"), false,
                            result -> {
                                if (result) {
                                    deckController.delete();
                                    Forge.back();
                                }
                            }))
                    );
                }
                addItem(new FMenuItem(localizer.getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, e -> FDeckViewer.copyDeckToClipboard(deck)));
                boolean devMode = FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED);
                if(!FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY) || devMode)
                    addItem(new FCheckBoxMenuItem(localizer.getMessage("cbEnforceDeckLegality"), shouldEnforceConformity(), e -> toggleConformity()));
                if(devMode && !editorConfig.hasInfiniteCardPool()) {
                    String devSuffix = " (" + localizer.getMessage("lblDev") + ")";
                    addItem(new FMenuItem(localizer.getMessage("lblAddcard") + devSuffix, FSkinImage.HDPLUS, e -> showDevAddCardDialog()));
                }
                ((DeckEditorPage) getSelectedPage()).buildDeckMenu(this);
            }
        };
    }

    protected void showAddBasicLandsDialog() {
        List<CardEdition> allowedLandSets = this.editorConfig.getBasicLandSets(getDeck());
        if(allowedLandSets == null || allowedLandSets.isEmpty())
            allowedLandSets = List.of(FModel.getMagicDb().getEditions().get("JMP"));
        CardEdition defaultLandSet = allowedLandSets.get(0);
        List<CardEdition> finalAllowedLandSets = allowedLandSets;
        FThreads.invokeInEdtNowOrLater(() -> {
            AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck, defaultLandSet, this::addChosenBasicLands, editorConfig.hasInfiniteCardPool() ? null : finalAllowedLandSets); //Null allows any lands to be selected
            dialog.show();

        });
        setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck is visible below dialog
    }

    protected void addChosenBasicLands(CardPool landsToAdd) {
        getMainDeckPage().addCards(landsToAdd);
    }

    /**
     * If a card is missing from a player's inventory while importing a deck, it gets run through here.
     * Returning a PaperCard will let unlimited copies of that card be used as a substitute. Returning null
     * will leave the card missing from the import.
     */
    protected PaperCard supplyPrintForImporter(PaperCard missingCard) {
        //Could support dungeons here too? Not that we really use them in the editor...
        if(!missingCard.isVeryBasicLand())
            return null;
        List<CardEdition> basicSets = editorConfig.getBasicLandSets(deck);
        String setCode = basicSets.isEmpty() ? "JMP" : basicSets.get(0).getCode();
        return FModel.getMagicDb().fetchCard(missingCard.getCardName(), setCode, null);
    }

    protected boolean shouldEnforceConformity() {
        if(FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY))
            return true;
        if(this.deck == null)
            return true;
        return !this.deck.getTags().contains(DECK_TAG_SUPPRESS_CONFORMITY);
    }

    protected void toggleConformity() {
        if(this.deck == null)
            return;
        Set<String> tags = this.deck.getTags();
        if(tags.contains(DECK_TAG_SUPPRESS_CONFORMITY))
            tags.remove(DECK_TAG_SUPPRESS_CONFORMITY);
        else
            tags.add(DECK_TAG_SUPPRESS_CONFORMITY);
        if(getCatalogPage() != null)
            getCatalogPage().scheduleRefresh(); //Refresh to update commander options.
    }

    protected void showDevAddCardDialog() {
        FOptionPane.showInputDialog(Forge.getLocalizer().getMessage("lblPromptCardRequest"), result -> {
            if(StringUtils.isBlank(result))
                return;
            CardPool requested = CardPool.fromSingleCardRequest(result);
            devAddCards(requested);
        });
    }


    /**
     * Adds cards to the catalog and data pool.
     */
    protected void devAddCards(CardPool cards) {
        if(this.getDraft() != null) {
            DraftPack pack = this.getDraft().getHumanPlayer().nextChoice();
            pack.addAll(cards.toFlatList());
            this.catalogPage.scheduleRefresh();
            return;
        }
        System.err.println("Adding cards not supported in this editor variant - " + this);
    }

    public DeckEditorConfig getEditorConfig() {
        return editorConfig;
    }

    public void setSelectedSection(DeckSection section) {
        if(hiddenExtraSections.contains(section))
            showExtraSectionTab(section);
        if(pagesBySection.containsKey(section))
            setSelectedPage(pagesBySection.get(section));
        else if(section == DeckSection.Main && pagesBySection.containsKey(mainDeckPage.deckSection))
            //Tried to switch to the Main page in a Planar or Scheme deck.
            setSelectedPage(pagesBySection.get(mainDeckPage.deckSection));
    }

    public void notifyNewControllerModel() {
        this.setDeck(this.deckController.getDeck());
    }

    public Deck getDeck() {
        return deck;
    }
    private void setDeck(Deck deck) {
        if (this.deck == deck) { return; }
        this.deck = deck;
        setHeaderText(getDeckController().getDeckDisplayName());
        if (deck == null || !tabsInitialized) { return; }

        //reinitialize tab pages when deck changes
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            ((CardManagerPage) tabPage).onDeckChanged(deck);
        }
    }

    public IDeckController getDeckController() {
        return this.deckController;
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

    protected CardManagerPage getCardSourcePage() {
        return isLimitedEditor() ? sideboardPage : catalogPage;
    }

    protected DeckSectionPage getPageForSection(DeckSection section) {
        return getPageForSection(section, false);
    }
    protected DeckSectionPage getPageForSection(DeckSection section, boolean forceCreateIfAbsent) {
        if(forceCreateIfAbsent && !pagesBySection.containsKey(section)) {
            this.hiddenExtraSections.add(section);
            this.createExtraSectionPage(section).hideTab();
        }
        return pagesBySection.get(section);
    }


    protected Set<DeckSection> getVariantCardPools() {
        return variantCardPools;
    }

    public BoosterDraft getDraft() {
        return null;
    }

    public boolean isDrafting() {
        return false;
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
        else if(!shouldEnforceConformity())
            return numAvailable;
        else {
            //Limited number of copies. If we're adding to the deck, cap the amount accordingly.
            if(source instanceof CatalogPage || (isLimitedEditor() && source == this.getSideboardPage()))
                return Math.min(numAvailable, Math.max(numAllowedInDeck - numInDeck, 0));
            else
                return numAvailable;
        }
    }

    public int getNumAllowedInDeck(PaperCard card) {
        DeckFormat format = this.editorConfig.getDeckFormat();
        if(card == null)
            return 0;
        else if (!shouldEnforceConformity())
            return Integer.MAX_VALUE;
        else if(format != null)
            return format.getMaxCardCopies(card);
        else
            return FModel.getPreferences().getPrefInt(FPref.DECK_DEFAULT_CARD_LIMIT);
    }

    protected DeckSectionPage showExtraSectionTab(DeckSection section) {
        this.variantCardPools.add(section);
        this.hiddenExtraSections.remove(section);
        DeckSectionPage page = this.getPageForSection(section, true);
        page.showTab();
        return page;
    }

    protected DeckSectionPage createExtraSectionPage(DeckSection section) {
        DeckSectionPage page = createPageForExtraSection(section, this.editorConfig);
        this.pagesBySection.put(section, page);
        this.addTabPage(page);
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
            case Contraptions:
                cardManager.applyAdvancedSearchFilter(new String[]{
                        "CARD_TYPE CONTAINS_ALL Artifact",
                        "CARD_SUB_TYPE CONTAINS_ALL Contraption"
                }, true);
                break;
            default:
                cardManager.resetFilters();
        }
    }

    public void setSaveHandler(FEventHandler saveHandler0) {
        saveHandler = saveHandler0;
    }

    public void save(final Consumer<Boolean> callback) {
        IDeckController deckController = getDeckController();
        if(deckController.supportsSave()) {
            if (!StringUtils.isEmpty(deck.getName())) {
                deckController.save();
            }
            else if (deckController.supportsRename()){
                List<PaperCard> commanders = deck.getCommanders(); //use commander name as default deck name
                String initialInput = commanders.isEmpty() ? "New Deck" : Lang.joinHomogenous(commanders);
                FThreads.invokeInEdtNowOrLater(() -> {
                    FOptionPane.showInputDialog(Forge.getLocalizer().getMessage("lblNameNewDeck"), initialInput, result -> {
                        if (StringUtils.isEmpty(result)) { return; }

                        deckController.saveAs(result);
                        if (callback != null) {
                            callback.accept(true);
                        }
                    });
                });
                return;
            }
        }

        if (callback != null) {
            callback.accept(true);
        }
    }

    public void completeDraft() {
        BoosterDraft draft = getDraft();
        assert(draft != null);

        final Deck[] computerDecks = draft.getComputerDecks();
        final LimitedPlayer[] players = draft.getOpposingPlayers();

        LimitedPlayer humanPlayer = draft.getHumanPlayer();
        humanPlayer.getDeck().setDraftNotes(humanPlayer.getSerializedDraftNotes());
        for(int i = 0; i < computerDecks.length; i++) {
            Deck deck = computerDecks[i];
            LimitedPlayer player = players[i];

            deck.setDraftNotes(player.getSerializedDraftNotes());
        }

        updateDeckHeader();

        save(null);
    }

    private final static ImmutableList<String> onCloseOptions = ImmutableList.of(
        Localizer.getInstance().getMessage("lblSave"),
        Localizer.getInstance().getMessage("lblDontSave"),
        Localizer.getInstance().getMessage("lblCancel")
    );

    @Override
    public void onClose(final Consumer<Boolean> canCloseCallback) {
        if (getDeckController().isSaved() || !allowSave() || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        FOptionPane.showOptionDialog(Forge.getLocalizer().getMessage("lblSaveChangesCurrentDeck"), "",
                FOptionPane.QUESTION_ICON, onCloseOptions, result -> {
                    if (result == 0) {
                        save(canCloseCallback);
                    } else if (result == 1) {
                        getDeckController().exitWithoutSaving(); //reload if not saving changes
                        canCloseCallback.accept(true);
                    } else {
                        canCloseCallback.accept(false);
                    }
                });
    }

    @Override
    public boolean keyDown(int keyCode) {
        try {
            //Pass input on to currently selected page. Calling super would invoke keypresses on all tabs.
            //Doing it first also gives the tab and card manager priority.
            //TODO: Generalize this to the TabPageScreen level if possible
            if(getSelectedPage().keyDown(keyCode))
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        switch (keyCode) {
            case Keys.BACK:
                return true; //suppress Back button so it's not bumped while editing deck
            case Keys.ESCAPE:
            case Keys.BUTTON_B:
            case Keys.BUTTON_SELECT:
                if (Forge.endKeyInput() || tabHeader.btnBack.trigger())
                    return true;
                break;
            case Keys.S: //save deck on Ctrl+S
                if (KeyInputAdapter.isCtrlKeyDown() && allowSave()) {
                    save(null);
                    return true;
                }
                break;
            case Keys.BUTTON_R1:
                controllerCycleTabs(1);
                return true;
            case Keys.F5:
                revalidate();
                break;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keyCode) {
        if (keyCode == Keys.ESCAPE && this.tabHeader.btnBack != null) {
            return this.tabHeader.btnBack.trigger();
        }
        return super.keyUp(keyCode);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null; //never use backdrop for editor
    }

    protected boolean allowRename() {
        return !isDrafting() && this.deckController.supportsRename();
    }
    protected boolean allowDelete() {
        return !isDrafting() && this.deckController.supportsDelete();
    }
    protected boolean allowSave() {
        return !isDrafting() && this.deckController.supportsSave();
    }
    protected boolean allowSaveAs() {
        return allowSave() && allowRename();
    }
    protected boolean allowAddBasic() {
        return !isDrafting();
    }

    /**
     * @return true if the editor should show the "Add Deck Section" option in the menu. False otherwise.
     */
    protected boolean showAddExtraSectionOption() {
        //In limited and formats with user inventories, variant cards can appear in their collection or card pool,
        //so they can create the section just by adding a card to it.
        return editorConfig.hasInfiniteCardPool() && !this.hiddenExtraSections.isEmpty();
    }
    public boolean isLimitedEditor() {
        return editorConfig.isLimited();
    }
    public boolean isCommanderEditor() {
        return editorConfig.hasCommander();
    }
    public boolean isDraftEditor() {
        return editorConfig.isDraft();
    }

    /**
     * @return true if DeckSectionPages are allowed to quick-swap cards with alternate prints taken from the card source page.
     */
    protected boolean isAllowedReplacement() {
        return editorConfig.allowsCardReplacement();
    }

    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        return null;
    }

    private boolean lastTabChangeWasController = false;
    /**
     * Cycles through tabs as a controller input. Changing tabs this way will hide unsupported options like filters.
     * @param amount amount of tabs to move the selection by; 1 for next, -1 for previous, etc.
     */
    protected void controllerCycleTabs(int amount) {
        List<TabPage<FDeckEditor>> visiblePages = tabPages.stream().filter(TabPage::isTabVisible).collect(Collectors.toList());
        if(visiblePages.isEmpty())
            return;
        int current = visiblePages.indexOf(getSelectedPage());
        int tabIndex = Math.floorMod(current + amount, visiblePages.size());
        this.lastTabChangeWasController = true;
        setSelectedPage(visiblePages.get(tabIndex));
    }

    protected static class DeckHeader extends FContainer {
        protected final FLabel lblName;
        protected final FLabel btnSave;
        protected final FLabel btnMoreOptions;
        protected FDisplayObject btnDraftLog;

        protected DeckHeader() {
            setHeight(HEADER_HEIGHT);
            this.lblName = add(new FLabel.Builder().font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build());
            this.btnSave = add(new FLabel.Builder().icon(Forge.hdbuttons ? FSkinImage.HDSAVE : FSkinImage.SAVE).align(Align.center).pressedColor(Header.getBtnPressedColor()).build());
            this.btnMoreOptions = add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(Align.center).pressedColor(Header.getBtnPressedColor()).build());
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
            float x = width;
            List<FDisplayObject> headerElements = layoutHeaderElements(height, width);
            for(FDisplayObject item : headerElements) {
                x -= item.getWidth();
                item.setPosition(x, 0);
            }
            lblName.setBounds(0, 0, x, height);
        }

        /**
         * Supplies buttons and other labels that are appended to the end of the header.
         * The deck name label will use the remaining space to the left.
         * The first item in the returned list will be the rightmost on the header.
         */
        @SuppressWarnings("SuspiciousNameCombination") //Intentionally using height for both edges so they're square.
        protected List<FDisplayObject> layoutHeaderElements(float height, float availableWidth) {
            List<FDisplayObject> out = new ArrayList<>();
            btnSave.setSize(height, height);
            btnMoreOptions.setSize(height, height);
            out.add(btnMoreOptions);
            if(btnSave.isVisible())
                out.add(btnSave);
            float remainingWidth = availableWidth - (float) out.stream().mapToDouble(FDisplayObject::getWidth).sum();
            if(btnDraftLog != null) {
                float width = Math.max(remainingWidth / 4, Math.min(height * 4, remainingWidth / 2));
                btnDraftLog.setSize(width, height);
                out.add(btnDraftLog);
            }
            return out;
        }

        public void initDraftLog(GameLog draftLog, FContainer parentScreen) {
            VLog draftLogContainer = new VLog(() -> draftLog);
            draftLogContainer.setDropDownContainer(parentScreen);
            this.btnDraftLog = new FLabel.ButtonBuilder()
                    .text(Localizer.getInstance().getMessage("lblEditorLog"))
                    .pressedColor(Header.getBtnPressedColor())
                    .command((e) -> draftLogContainer.show())
                    .font(FSkinFont.get(20))
                    .build();
            draftLogContainer.setDropdownOwner(btnDraftLog);
            this.add(btnDraftLog);
        }
    }

    protected static abstract class DeckEditorPage extends TabPage<FDeckEditor> {
        protected DeckEditorPage(String caption, FImage icon) {
            super(caption, icon);
        }

        public void buildDeckMenu(FPopupMenu menu) {
        }

        @Override
        protected void onAdd() {
            super.onAdd();
            //Initial batch of tabs gets added before the editorConfig can be defined.
            if(parentScreen.editorConfig != null)
                this.initialize();
        }

        protected abstract void initialize();

        @Override
        public boolean fling(float velocityX, float velocityY) {
            return false; //prevent left/right swipe to change tabs since it doesn't play nice with item managers
        }
    }

    protected static abstract class CardManagerPage extends DeckEditorPage {
        private final ItemManagerConfig config;
        protected final CardManager cardManager;

        protected CardManagerPage(CardManager cardManager, ItemManagerConfig config, String caption, FImage icon) {
            super(caption, icon);
            this.config = config;
            this.cardManager = add(cardManager);
        }

        protected void initialize() {
            cardManager.setItemActivateHandler(e -> onCardActivated(this.cardManager.getSelectedItem()));
            cardManager.setContextMenuBuilder(new ContextMenuBuilder<>() {
                @Override
                public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                    CardManagerPage.this.buildMenu(menu, card);
                }
            });
            cardManager.setup(config, parentScreen.getColOverrides(config));
            cardManager.setShowRanking(showDraftRanking());

            if(parentScreen.getDeck() != null)
                this.onDeckChanged(parentScreen.getDeck());
        }

        protected boolean showDraftRanking() {
            return ItemManagerConfig.DRAFT_CONSPIRACY == config
                    || ItemManagerConfig.DRAFT_PACK == config || ItemManagerConfig.DRAFT_POOL == config
                    || ItemManagerConfig.DRAFT_DECKS == config || (parentScreen != null && parentScreen.isDraftEditor());
        }

        @Override
        protected void onActivate() {
            super.onActivate();
            if(parentScreen.lastTabChangeWasController) {
                cardManager.getConfig().setPileBy(null);
                cardManager.setHideFilters(true);
                parentScreen.lastTabChangeWasController = false;
            }
            else {
                cardManager.setHideFilters(cardManager.getConfig().getHideFilters());
            }
        }

        @Override
        protected void onDeactivate() {
            super.onDeactivate();
            this.cardManager.closeMenu();
        }

        protected void onDeckChanged(Deck newDeck) {}

        public void addCard(PaperCard card) {
            addCard(card, 1);
        }
        public void addCard(PaperCard card, int qty) {
            if(cardManager.isInfinite())
                return;
            cardManager.addItem(card, qty);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        public void addCards(Iterable<Entry<PaperCard, Integer>> cards) {
            if(cardManager.isInfinite())
                return;
            cardManager.addItems(cards);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        public void removeCard(PaperCard card) {
            removeCard(card, 1);
        }
        public void removeCard(PaperCard card, int qty) {
            if (cardManager.isInfinite())
                return;
            cardManager.removeItem(card, qty);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        public void removeCards(Iterable<Entry<PaperCard, Integer>> cards) {
            if (cardManager.isInfinite())
                return;
            cardManager.removeItems(cards);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        public void setCards(CardPool cards) {
            cardManager.setItems(cards);
            parentScreen.getDeckController().notifyModelChanged();
            updateCaption();
        }

        /**
         * Moves a single copy of `card` from here to `destination`. Will not work if there are no copies of the card here.
         */
        public void moveCard(PaperCard card, CardManagerPage destination) {
            moveCard(card, destination, 1);
        }

        /**
         * Moves `qty` copies of `card` from here to `destination`. Will not move more copies than are available here.
         */
        public void moveCard(PaperCard card, CardManagerPage destination, int qty) {
            int amountToMove = Math.min(cardManager.getItemCount(card), qty);
            if(!this.cardManager.isInfinite())
                this.cardManager.removeItem(card, amountToMove);
            if(!destination.cardManager.isInfinite())
                destination.cardManager.addItem(card, amountToMove);
            parentScreen.getDeckController().notifyModelChanged();
            this.updateCaption();
            destination.updateCaption();
        }

        /**
         * Moves a pool of `cards` from here to `destination`. Cannot move more copies of a card than are available here.
         */
        public void moveCards(Iterable<Entry<PaperCard, Integer>> cards, CardManagerPage destination) {
            if(cardManager.isInfinite()) {
                destination.addCards(cards);
                return;
            }
            CardPool moveable = new CardPool();
            for (Entry<PaperCard, Integer> entry : cards) {
                int amountToMove = Math.min(entry.getValue(), this.cardManager.getItemCount(entry.getKey()));
                if(amountToMove <= 0)
                    continue;
                moveable.add(entry.getKey(), amountToMove);
            }
            this.cardManager.removeItems(moveable);
            if(!destination.cardManager.isInfinite())
                destination.cardManager.addItems(moveable);
            parentScreen.getDeckController().notifyModelChanged();
            this.updateCaption();
            destination.updateCaption();
        }

        protected void updateCaption() {}

        protected abstract void onCardActivated(PaperCard card);
        protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

        protected void addMoveCardMenuItem(FDropDownMenu menu, CardManagerPage source, CardManagerPage destination, final Consumer<Integer> callback) {
            //Determine how many we can actually move.
            ItemPool<PaperCard> selectedItemPool = parentScreen.getAllowedAdditions(cardManager.getSelectedItemPool(), source, destination);

            int maxMovable = selectedItemPool.isEmpty() ? 0 : Integer.MAX_VALUE;
            for (Entry<PaperCard, Integer> i : selectedItemPool)
                maxMovable = Math.min(maxMovable, i.getValue());
            if (maxMovable == 0)
                return;
            PaperCard sampleCard = cardManager.getSelectedItem();
            String labelAction, labelSection;
            CardManagerPage cardSourcePage = parentScreen.getCardSourcePage();
            if(destination == null || destination instanceof CatalogPage
                    || (destination == cardSourcePage && !(source instanceof CatalogPage))) {
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
                if(source == cardSourcePage)
                    labelAction = "lblAdd"; //"Add to deck" when we're in a limited sideboard
                else
                    labelAction = "lblMove";
                labelSection = getMoveLabel((DeckSectionPage) destination, sampleCard, false);
            }
            else if(destination instanceof DeckSectionPage deckSectionPage) {
                //Moving from a card pool to a named section, e.g. "Add to sideboard"
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
                    callback.accept(1);
                else
                    GuiChoose.getInteger(prompt, 1, max, 20, callback);
            }));
        }

        protected void addMoveCardMenuItem(FDropDownMenu menu, PaperCard card, CardManagerPage source, CardManagerPage destination) {
            if(source == null || destination == null) {
                System.err.println("Unable to create option to move card: " + source + " -> " + destination);
                return;
            }

            if(destination instanceof DeckSectionPage && ((DeckSectionPage) destination).deckSection == DeckSection.Avatar) {
                Localizer localizer = Localizer.getInstance();
                String caption = String.join(" ", localizer.getMessage("lblAddCommander"), localizer.getMessage("lblasavatar"));
                menu.addItem(new FMenuItem(caption, destination.getIcon(), e -> setVanguard(card)));
                return;
            }

            this.addMoveCardMenuItem(menu, source, destination, new MoveCardCallback(card, source, destination));
        }

        protected static class MoveCardCallback implements Consumer<Integer> {
            public final PaperCard card;
            public final CardManagerPage from;
            public final CardManagerPage to;

            public MoveCardCallback(PaperCard card, CardManagerPage from, CardManagerPage to) {
                this.card = card;
                this.from = from;
                this.to = to;
            }
            @Override
            public void accept(Integer result) {
                if(result == null || result == 0)
                    return;
                from.moveCard(card, to, result);
            }
        }

        protected static class MoveQuantityPrompt implements FEventHandler {
            final String prompt;
            final int max;
            final Consumer<Integer> callback;

            public MoveQuantityPrompt(String prompt, int max, Consumer<Integer> callback) {
                this.prompt = prompt;
                this.max = max;
                this.callback = callback;
            }

            @Override
            public void handleEvent(FEvent e) {
                if(max < 2)
                    callback.accept(1);
                else
                    GuiChoose.getInteger(prompt, 1, max, 20, result -> {
                        if (result == null || result == 0)
                            return;
                        callback.accept(result);
                    });
            }
        }

        private String getMoveLabel(DeckSectionPage page, PaperCard selectedCard, boolean from) {
            //TODO: This might make more sense in the DeckSection class itself, and shared with the desktop editor.
            //It also might be better to arrange strings as "Add to {zone}" rather than "Add" + "to zone".
            switch (page.deckSection) {
                default:
                case Main: return from ? "lblfromdeck" : "lbltodeck";
                case Sideboard: return from ? "lblfromsideboard" : "lbltosideboard";
                case Planes: return from ? "lblfromplanardeck" : "lbltoplanardeck";
                case Schemes: return from ? "lblfromschemedeck" : "lbltoschemedeck";
                case Conspiracy: return from ? "lblfromconspiracydeck" : "lbltoconspiracydeck";
                case Dungeon: return from ? "lblfromdungeondeck" : "lbltodungeondeck";
                case Attractions: return from ? "lblfromattractiondeck" : "lbltoattractiondeck";
                case Contraptions: return from ? "lblfromcontraptiondeck" : "lbltocontraptiondeck";
                case Avatar: return "lblasavatar";
                case Commander:
                    if (parentScreen.editorConfig.getGameType() == GameType.Oathbreaker) {
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
            if (canBeMainCommander(card) && canEditMainCommander()) {
                String captionSuffix;
                if(parentScreen.editorConfig.getGameType() == GameType.Oathbreaker)
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

        protected boolean canEditMainCommander() {
            //Planar conquest sets the commander by other means.
            return parentScreen.getEditorConfig().getGameType() != GameType.PlanarConquest;
        }

        protected boolean needsCommander() {
            return parentScreen.isCommanderEditor() && parentScreen.getDeck().getCommanders().isEmpty();
        }

        protected boolean canBeMainCommander(final PaperCard card) {
            if(!parentScreen.isCommanderEditor() || parentScreen.getCommanderPage() == null)
                return false;
            if(parentScreen.getCommanderPage().cardManager.getPool().contains(card))
                return false; //Don't let it be the commander if it already is one.
            DeckFormat format = parentScreen.editorConfig.getDeckFormat();
            if(format == null)
                format = DeckFormat.Commander;
            return format.isLegalCommander(card.getRules());
        }

        protected boolean canBePartnerCommander(final PaperCard card) {
            if(!parentScreen.isCommanderEditor())
                return false;
            if(parentScreen.editorConfig.getGameType() == GameType.Oathbreaker) {
                //FIXME: For now, simplify Oathbreaker by not supporting partners.
                //Needs support for tracking whose signature spell is whose, here and elsewhere.
                return false;
            }
            List<PaperCard> commanders = parentScreen.getDeck().get(DeckSection.Commander).toFlatList();
            commanders.removeIf((c) -> c.getRules().canBeSignatureSpell());
            if(commanders.size() != 1)
                return false;
            if(!parentScreen.shouldEnforceConformity())
                return true;
            return commanders.get(0).getRules().canBePartnerCommanders(card.getRules());
        }

        protected boolean canBeSignatureSpell(final PaperCard card) {
            DeckFormat format = parentScreen.editorConfig.getDeckFormat();
            if(!format.hasSignatureSpell())
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
            if(!parentScreen.shouldEnforceConformity()) {
                return false;
            }

            byte cmdCI = 0;
            for (final PaperCard p : parentScreen.getDeck().getCommanders()) {
                cmdCI |= p.getRules().getColorIdentity().getColor();
            }

            return !card.getRules().getColorIdentity().hasNoColorsExcept(cmdCI);
        }

        protected void setVanguard(PaperCard card) {
            DeckSectionPage avatarPage = parentScreen.getPageForSection(DeckSection.Avatar, true);
            avatarPage.ejectCards();
            moveCard(card, avatarPage);
        }

        protected void setCommander(PaperCard card) {
            DeckSectionPage commanderPage = parentScreen.getCommanderPage();
            commanderPage.ejectCards();
            moveCard(card, commanderPage);
            refresh(); //refresh so cards shown that match commander's color identity
        }

        protected void setPartnerCommander(PaperCard card) {
            moveCard(card, parentScreen.getCommanderPage());
            refresh(); //refresh so cards shown that match commander's color identity
        }

        protected void setSignatureSpell(PaperCard card) {
            PaperCard signatureSpell = parentScreen.getDeck().getSignatureSpell();
            if (signatureSpell != null) {
                parentScreen.getCommanderPage().ejectCard(signatureSpell); //remove existing signature spell if any
            }
            moveCard(card, parentScreen.getCommanderPage());
            //refreshing isn't needed since color identity won't change from signature spell
        }

        public void setItemManagerCaption(String localizerKey) {
            if(localizerKey == null)
                localizerKey = "lblCards";
            this.cardManager.setCaption(Forge.getLocalizer().getMessage(localizerKey));
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
        private boolean needRefreshWhenShown;
        private boolean isInitialized = false;

        protected CatalogPage(CardManager cardManager, ItemManagerConfig config) {
            this(cardManager, config, Localizer.getInstance().getMessage("lblCatalog"), Forge.hdbuttons ? FSkinImage.HDFOLDER : FSkinImage.FOLDER);
        }
        protected CatalogPage(CardManager cardManager, ItemManagerConfig config, String caption, FImage icon) {
            super(cardManager, config, caption, icon);
        }

        @Override
        protected void initialize() {
            super.initialize();
            cardManager.setCaption(this.caption);
            isInitialized = true;
            if(needRefreshWhenShown)
                scheduleRefresh();
        }

        @Override
        protected void onDeckChanged(Deck newDeck) {
            super.onDeckChanged(newDeck);
            //If we're hidden, we can delay refreshing. But there's a slight complication.
            //When looking at a deck section, selecting a card that could have more copies added will let you pull
            //those copies from this catalog. They need to be readily available.
            if (isInitialized && !isVisible() && parentScreen.getCardSourcePage() == this && !parentScreen.getEditorConfig().hasInfiniteCardPool()) {
                needRefreshWhenShown = true;
                //Throw in the all cards that might be requested by other pages. The other pages will determine if they
                //can actually be added, and we'll clear these out in the real refresh.
                cardManager.setPool(parentScreen.getDeck().getAllCardsInASinglePool(true, true), true);
            }
            else {
                scheduleRefresh();
            }
        }

        @Override
        public void addCard(PaperCard card, int qty) {
            doScheduledRefresh(); //ensure refreshed before cards added if hasn't been refreshed yet
            super.addCard(card, qty);
        }

        @Override
        public void addCards(Iterable<Entry<PaperCard, Integer>> cards) {
            doScheduledRefresh();
            super.addCards(cards);
        }

        public ItemPool<PaperCard> getCardPool() {
            final DeckEditorConfig editorConfig = parentScreen.getEditorConfig();
            Deck currentDeck = parentScreen.getDeck();
            List<Predicate<PaperCard>> filters = new ArrayList<>();

            //Clone the pool to ensure we don't mutate it by adding to or removing from this page.
            //Can override this if that behavior is desired.
            ItemPool<PaperCard> cardPool = CardPool.createFrom(parentScreen.getEditorConfig().getCardPool(), PaperCard.class);

            if(editorConfig.usePlayerInventory() && currentDeck != null) {
                //Remove any items from the pool that are in the deck.
                cardPool.removeAll(currentDeck.getAllCardsInASinglePool(true, true));
            }

            //Add format filter.
            if(editorConfig.getCardFilter() != null)
                filters.add(editorConfig.getCardFilter());

            if(editorConfig.hasInfiniteCardPool()) {
                //Dump all the variant cards our deck calls for into the card pool.
                for(DeckSection variant : parentScreen.getVariantCardPools()) {
                    switch(variant) {
                        case Avatar: cardPool.addAll(FModel.getAvatarPool()); break;
                        case Conspiracy: cardPool.addAll(FModel.getConspiracyPool()); break;
                        case Planes: cardPool.addAll(FModel.getPlanechaseCards()); break;
                        case Schemes: cardPool.addAll(FModel.getArchenemyCards()); break;
                        case Dungeon: cardPool.addAll(FModel.getDungeonPool()); break;
                        case Attractions: cardPool.addAll(FModel.getAttractionPool()); break;
                        case Contraptions: cardPool.addAll(FModel.getContraptionPool()); break;
                    }
                }
            }

            if(!filters.isEmpty()) {
                Predicate<PaperCard> filter = filters.get(0);
                if(filters.size() > 1)
                    for(int i = 1; i < filters.size(); i++)
                        filter = filter.and(filters.get(i));
                cardPool.retainIf(filter);
            }

            return cardPool;
        }

        public void scheduleRefresh() {
            if(isInitialized && isVisible())
                refresh();
            else
                this.needRefreshWhenShown = true;
        }

        private void doScheduledRefresh() {
            if (needRefreshWhenShown) {
                needRefreshWhenShown = false;
                refresh();
            }
        }

        @Override
        protected void onActivate() {
            super.onActivate();
            doScheduledRefresh();
        }

        @Override
        public void refresh() {
            final DeckEditorConfig editorConfig = parentScreen.getEditorConfig();
            final DeckFormat deckFormat = editorConfig.getDeckFormat();
            Deck currentDeck = parentScreen.getDeck();
            ItemPool<PaperCard> cardPool = this.getCardPool();
            String label = "lblCards";

            if(editorConfig.hasCommander() && parentScreen.getCardSourcePage() == this && deckFormat != null && currentDeck != null) {
                //TODO: Commander filters probably should be handled elsewhere. Probably the card manager. I don't like mutating the pool outside of getCardPool.
                //Maybe we add a button in the card manager to filter for valid cards and pass the current commander choice to it.
                //Then we just remove the "add to deck" option when clicking a card outside the color identity.
                if(needsCommander()) {
                    //If we need a commander, show only commander candidates.
                    cardPool.retainIf(deckFormat.isLegalCommanderPredicate());
                    if(editorConfig.getGameType() == GameType.Oathbreaker)
                        label = "lblOathbreakers";
                    else
                        label = "lblCommanders";
                }
                else if(parentScreen.shouldEnforceConformity()) //If we have a commander, filter for color identity.
                    cardPool.retainIf(deckFormat.isLegalCardForCommanderPredicate(currentDeck.getCommanders()));
            }

            cardManager.setCaption(Forge.getLocalizer().getMessage(label));
            cardManager.setPool(cardPool, editorConfig.hasInfiniteCardPool());
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            if(destinationPage == null) {
                System.err.println("Unable to quick-move card (no page for destination) - " + card + " -> " + destination);
                return; //Shouldn't happen?
            }
            if(parentScreen.getMaxMovable(card, this, destinationPage) <= 0)
                return;
            if (needsCommander()) {
                assert(canEditMainCommander()); //Deck should never be without a commander in the first place...
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

            moveCard(card, destinationPage);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            if (card == null)
                return;

            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);

            if (!needsCommander() && !canOnlyBePartnerCommander(card) && destinationPage != null) {
                addMoveCardMenuItem(menu, card, this, destinationPage);
                if (canSideboard(card) && destination != DeckSection.Sideboard) {
                    addMoveCardMenuItem(menu, card, this, parentScreen.getSideboardPage());
                }
            }

            addCommanderItems(menu, card);

            if(this.allowFavoriteCards()) {
                //add option to add or remove card from favorites
                if (!this.cardIsFavorite(card)) {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAddFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_FILLED : FSkinImage.STAR_FILLED,
                            e -> this.setCardFavorited(card, true)));
                } else {
                    menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRemoveFavorites"), Forge.hdbuttons ? FSkinImage.HDSTAR_OUTLINE : FSkinImage.STAR_OUTLINE,
                            e -> this.setCardFavorited(card, false)));
                }
            }

            if (parentScreen.getEditorConfig().hasInfiniteCardPool()) {
                final CardPreferences prefs = CardPreferences.getPrefs(card);
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
                        GuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblSelectPreferredArt") + " " + card.getDisplayName(), sortedOptions, result -> {
                            if (result != null) {
                                if (result != card) {
                                    cardManager.replaceAll(card, result);
                                }
                                prefs.setPreferredArt(result.getEdition(), result.getArtIndex());
                                CardPreferences.save();
                            }
                        });
                    }));
                }
            }
        }

        @Override
        public void buildDeckMenu(FPopupMenu menu) {
            if (cardManager.getConfig().getShowUniqueCardsOption()) {
                menu.addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblUniqueCardsOnly"), cardManager.getWantUnique(), e -> {
                    boolean wantUnique = !cardManager.getWantUnique();
                    cardManager.setWantUnique(wantUnique);
                    cardManager.getConfig().setUniqueCardsOnly(wantUnique);
                    cardManager.refresh();
                }));
            }
        }

        public void setCardFavorited(PaperCard card, boolean isFavorite) {
            final CardPreferences prefs = CardPreferences.getPrefs(card);
            prefs.setStarCount(isFavorite ? 1 : 0);
            CardPreferences.save();
        }

        protected boolean allowFavoriteCards() {
            return parentScreen.getEditorConfig().hasInfiniteCardPool();
        }

        protected boolean cardIsFavorite(PaperCard card) {
            return CardPreferences.getPrefs(card).getStarCount() > 0;
        }
    }

    protected static class DeckSectionPage extends CardManagerPage {
        private final String captionPrefix;
        protected final DeckSection deckSection;

        protected DeckSectionPage(CardManager cardManager, DeckSection deckSection) {
            this(cardManager, deckSection, ItemManagerConfig.DECK_EDITOR);
        }
        protected DeckSectionPage(CardManager cardManager, DeckSection deckSection, ItemManagerConfig config) {
            this(cardManager, deckSection, config, deckSection.getLocalizedShortName(), iconFromDeckSection(deckSection));
        }
        protected DeckSectionPage(CardManager cardManager, DeckSection deckSection, ItemManagerConfig config, String caption, FImage icon) {
            super(cardManager, config, null, icon);

            this.deckSection = deckSection;
            this.captionPrefix = caption;
        }

        @Override
        protected void initialize() {
            super.initialize();
            this.cardManager.setCaption(captionPrefix);
            updateCaption();
        }

        @Override
        protected void onDeckChanged(Deck newDeck) {
            super.onDeckChanged(newDeck);
            cardManager.setPool(newDeck.getOrCreate(deckSection));
        }

        @Override
        protected void updateCaption() {
            if (deckSection == DeckSection.Commander || parentScreen.getDeck() == null) {
                caption = captionPrefix; //don't display count for commander section since it won't be more than 1
            } else {
                caption = captionPrefix + " (" + parentScreen.getDeck().get(deckSection).countAll() + ")";
            }
            if(parentScreen.hiddenExtraSections.contains(this.deckSection) && cardManager.getPool() != null && !cardManager.getPool().isEmpty())
                parentScreen.showExtraSectionTab(this.deckSection);
        }

        /**
         * Sends all cards from this section to the sideboard or catalog, as appropriate.
         */
        public void ejectCards() {
            CardManagerPage destination = getCardSourcePage();
            if(destination == null)
                return;
            destination.addCards(cardManager.getPool());
            setCards(new CardPool());
        }

        /**
         * Sends a single card from this section to the sideboard or catalog, as appropriate.
         */
        public void ejectCard(PaperCard card) {
            CardManagerPage destination = getCardSourcePage();
            if(destination == null)
                return;
            moveCard(card, destination);
        }

        public CardManagerPage getCardSourcePage() {
            if(parentScreen.isLimitedEditor() && deckSection == DeckSection.Sideboard)
                return parentScreen.getMainDeckPage();
            return parentScreen.getCardSourcePage();
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            switch (deckSection) {
                case Main:
                case Planes:
                case Schemes:
                case Attractions:
                case Contraptions:
                case Sideboard:
                    ejectCard(card);
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {

            CardManagerPage cardSourcePage = getCardSourcePage();
            DeckSectionPage sideboardPage = parentScreen.getSideboardPage();
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            switch (deckSection) {
            default:
            case Main:
                //Take more cards from source
                addMoveCardMenuItem(menu, card, cardSourcePage, this);
                //Remove cards and return them to the source
                addMoveCardMenuItem(menu, card, this, cardSourcePage);
                //Send cards to the sideboard (if the above option doesn't cover that)
                if(cardSourcePage != sideboardPage && canSideboard(card))
                    addMoveCardMenuItem(menu, card, this, sideboardPage);

                addReplaceVariantItems(menu, card);
                addCommanderItems(menu, card);
                break;
            case Sideboard:
                //Send cards to main deck (or whatever section they go in)
                addMoveCardMenuItem(menu, card, this, destinationPage);
                if(cardSourcePage != this) {
                    //Take more cards from the source
                    addMoveCardMenuItem(menu, card, cardSourcePage, this);
                    //Remove cards and return them to the source
                    if(cardSourcePage != destinationPage)
                        addMoveCardMenuItem(menu, card, this, cardSourcePage);
                }

                addReplaceVariantItems(menu, card);
                addCommanderItems(menu, card);
                break;
            case Commander:
                if (canEditMainCommander() || isPartnerCommander(card)) {
                    addMoveCardMenuItem(menu, this, cardSourcePage, result -> {
                        moveCard(card, cardSourcePage, result);
                        if(cardSourcePage == parentScreen.getCatalogPage()) {
                            parentScreen.getCatalogPage().refresh(); //refresh so commander options shown again
                            parentScreen.setSelectedPage(parentScreen.getCatalogPage());
                        }
                    });
                    addReplaceVariantItems(menu, card);
                }
                break;
            case Avatar:
                addMoveCardMenuItem(menu, card, this, cardSourcePage);
                break;
            case Schemes:
            case Planes:
            case Attractions:
            case Contraptions:
                addMoveCardMenuItem(menu, card, cardSourcePage, this);
                addMoveCardMenuItem(menu, card, this, cardSourcePage);
                addReplaceVariantItems(menu, card);
                break;
            }

            addPerCardItems(menu, card);
        }

        protected void addPerCardItems(FDropDownMenu menu, PaperCard card) {
            int markedColorCount = card.getRules().getSetColorID();
            if (markedColorCount > 0) {
                menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblColorIdentity"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS, e -> {
                    Set<String> currentColors;
                    if(card.getMarkedColors() != null)
                        currentColors = card.getMarkedColors().stream().map(MagicColor.Color::getName).collect(Collectors.toSet());
                    else
                        currentColors = null;
                    String prompt = Forge.getLocalizer().getMessage("lblChooseAColor", Lang.getNumeral(markedColorCount));
                    GuiChoose.getChoices(prompt, markedColorCount, markedColorCount, MagicColor.Constant.ONLY_COLORS, currentColors, null, result -> {
                        addCard(card.copyWithMarkedColors(ColorSet.fromNames(result)));
                        removeCard(card);
                    });
                }));
            }
        }

        private void addReplaceVariantItems(FDropDownMenu menu, PaperCard card) {
            //Determine if we're allowed to replace cards, and if there are any cards to substitute in.
            if (!parentScreen.isAllowedReplacement())
                return;
            FSkinImage iconReplaceCard = Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST;
            final Localizer localizer = Forge.getLocalizer();
            final ItemPool<PaperCard> cardOptions = parentScreen.getCardSourcePage().cardManager.getPool().getFilteredPool((c) -> c.getName().equals(card.getName()));
            cardOptions.removeAll(card);
            if (!cardOptions.isEmpty()) {
                String lblReplaceCard = localizer.getMessage("lblReplaceCard");
                menu.addItem(new FMenuItem(lblReplaceCard, iconReplaceCard, e -> handleReplaceCard(e, card, cardOptions)));
            }

            if (parentScreen.getEditorConfig().hasInfiniteCardPool()) {
                //If there's a player inventory, foils can be included in the above. Otherwise, it's a separate option.
                boolean isFoil = card.isFoil();
                String lblFoil = (isFoil ? localizer.getMessage("lblRemove") : localizer.getMessage("lblSet"))
                        + (" " + localizer.getMessage("lblConvertToFoil"));
                menu.addItem(new FMenuItem(lblFoil, iconReplaceCard, e -> handleFoilCard(e, card, !isFoil)));
            }

        }

        private void handleReplaceCard(FEvent e, PaperCard card, ItemPool<PaperCard> cardOptions) {
            //sort options so current option is on top and selected by default
            List<PaperCard> sortedOptions = new ArrayList<>();
            sortedOptions.add(card);
            for (Entry<PaperCard, Integer> optionEntry : cardOptions) {
                sortedOptions.add(optionEntry.getKey());
            }
            final Localizer localizer = Forge.getLocalizer();
            String lblReplaceCard = localizer.getMessage("lblReplace");
            String prompt = localizer.getMessage("lblSelectReplacementCard") + " " + card.getDisplayName();
            String promptQuantity = String.format("%s - %s %s", card, lblReplaceCard, localizer.getMessage("lblHowMany"));
            //First have the player choose which card to swap in.
            GuiChoose.oneOrNone(prompt, sortedOptions, replacement -> {
                if (replacement == null || replacement == card)
                    return;
                //Next, ask how many copies they'd like to swap, taking into account the number available.
                int available = parentScreen.getCardSourcePage().cardManager.isInfinite() ? Integer.MAX_VALUE : cardOptions.count(replacement);
                int maxMovable = Math.min(available, cardManager.getItemCount(card));
                new MoveQuantityPrompt(promptQuantity, maxMovable, (amount) -> {
                    CardManagerPage sourcePage = parentScreen.getCardSourcePage();
                    //Finally, swap the cards.
                    DeckSectionPage.this.moveCard(card, sourcePage, amount);
                    sourcePage.moveCard(replacement, DeckSectionPage.this, amount);
                }).handleEvent(e);
            });
        }

        /**
         * Converts one or more copies of the selected card to foil. Only used when pulling cards from an infinite pool.
         */
        private void handleFoilCard(FEvent e, PaperCard card, boolean makeFoil) {
            final Localizer localizer = Forge.getLocalizer();
            String lblReplaceCard = localizer.getMessage("lblReplace");
            String promptQuantity = String.format("%s - %s %s", card, lblReplaceCard, localizer.getMessage("lblHowMany"));
            int maxMovable = cardManager.getItemCount(card);
            //This loses any marked flags. Could manually re-add them but for now it's probably fine.
            PaperCard newCard = makeFoil ? card.getFoiled() : card.getUnFoiled();
            new MoveQuantityPrompt(promptQuantity, maxMovable, (amount) -> {
                addCard(newCard, amount);
                removeCard(card, amount);
            }).handleEvent(e);
        }

        private boolean isPartnerCommander(final PaperCard card) {
            if (!parentScreen.isCommanderEditor() || parentScreen.getDeck().getCommanders().isEmpty()) {
                return false;
            }

            PaperCard firstCmdr = parentScreen.getDeck().getCommanders().get(0);
            return !card.getName().equals(firstCmdr.getName());
        }
    }

    public static class FDraftLog extends GameLog implements IDraftLog {
        @Override
        public void addLogEntry(String message) {
            this.add(GameLogEntryType.DRAFT, message);
        }
    }

    protected static class DraftPackPage extends CatalogPage {
        protected boolean draftingFaceDown = false;

        public DraftPackPage(CardManager cardManager) {
            super(cardManager, ItemManagerConfig.DRAFT_PACK, Localizer.getInstance().getMessage("lblPackN", String.valueOf(1)), FSkinImage.PACK);
            cardManager.setShowRanking(true);
        }

        @Override
        protected void updateCaption() {
            BoosterDraft draft = parentScreen.getDraft();
            if (draft == null)
                return;
            if(draft.getHumanPlayer().nextChoice() == null)
                return;
            int packNumber = draft.getHumanPlayer().nextChoice().getId();
            String lblPackN = Forge.getLocalizer().getMessage("lblPackN", String.valueOf(packNumber));
            caption = lblPackN;
            cardManager.setCaption(lblPackN);
        }

        @Override
        public void refresh() {
            BoosterDraft draft = parentScreen.getDraft();
            if (draft == null || !draft.hasNextChoice()) {
                return;
            }

            CardPool pool = draft.nextChoice();

            if(pool == null) {
                //Some packs were still being passed around, but now the round is over.
                cardManager.setPool(new CardPool());
                draft.postDraftActions();
                hideTab(); //hide this tab page when finished drafting
                parentScreen.completeDraft();
            }

            this.draftingFaceDown = getDraftPlayer().hasArchdemonCurse();

            if(draftingFaceDown) {
                ItemPool<PaperCard> fakePool = new ItemPool<>(PaperCard.class);
                for(Object ignored : pool)
                    fakePool.add(PaperCard.FAKE_CARD);
                cardManager.setPool(fakePool);
                cardManager.setShowRanking(false);
            }
            else {
                cardManager.setPool(pool);
                cardManager.setShowRanking(true);
            }

            if (getDraftPlayer().shouldSkipThisPick()) {
                skipPick();
                return;
            }

            this.updateCaption();
            cardManager.setEnabled(true);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            if(!cardManager.isEnabled())
                return;
            DeckSection destination;
            if(draftingFaceDown) {
                card = getDraftPlayer().pickFromArchdemonCurse(getDraftPlayer().nextChoice());
                destination = DeckSection.Sideboard;
            }
            else if(card.isVeryBasicLand()) {
                destination = DeckSection.Sideboard; //Throw these in the sideboard
            }
            else {
                destination = DeckSection.matchingSection(card);
                if (destination == DeckSection.Avatar || destination == DeckSection.Commander)
                    destination = DeckSection.Sideboard; //We don't want to quick-move to any singleton sections.
            }
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination);
            if(destinationPage == null) {
                System.err.println("Unable to quick-move card (no page for destination) - " + card + " -> " + destination);
                return; //Shouldn't happen?
            }
            moveCard(card, destinationPage);
        }

        @Override
        public void moveCard(PaperCard card, CardManagerPage destination, int qty) {
            assert(qty == 1);
            assert(destination instanceof DeckSectionPage);
            BoosterDraft draft = parentScreen.getDraft();
            cardManager.setEnabled(false); //Prevent any weird inputs until choices are made and the next set of cards is ready.
            DeckSection section = ((DeckSectionPage) destination).deckSection;
            FThreads.invokeInBackgroundThread(() -> {
                draft.setChoice(card, section);

                if (draft.hasNextChoice()) {
                    refresh();
                }
                else {
                    draft.postDraftActions();
                    hideTab(); //hide this tab page when finished drafting
                    parentScreen.completeDraft();
                }

                parentScreen.getDeckController().notifyModelChanged();
                destination.cardManager.refresh();
                this.updateCaption();
                destination.updateCaption();
            });
        }

        private void skipPick() {
            BoosterDraft draft = parentScreen.getDraft();
            cardManager.setEnabled(false);
            FThreads.invokeInBackgroundThread(() -> {
                draft.skipChoice();
                if (draft.hasNextChoice()) {
                    refresh();
                }
                else {
                    draft.postDraftActions();
                    hideTab(); //hide this tab page when finished drafting
                    parentScreen.completeDraft();
                }
            });

        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            if(!cardManager.isEnabled())
                return;
            if(draftingFaceDown) {
                addMoveCardMenuItem(menu, this, parentScreen.getSideboardPage(), result -> { //ignore quantity
                    PaperCard realCard = getDraftPlayer().pickFromArchdemonCurse(getDraftPlayer().nextChoice());
                    moveCard(realCard, parentScreen.getSideboardPage());
                });
                return;
            }
            DeckSection destination = DeckSection.matchingSection(card);
            final DeckSectionPage destinationPage = parentScreen.getPageForSection(destination, true);
            addMoveCardMenuItem(menu, this, destinationPage, result -> { //ignore quantity
                moveCard(card, destinationPage);
            });
            addMoveCardMenuItem(menu,
                    this,
                    parentScreen.getSideboardPage(),
                    result -> { //ignore quantity
                        moveCard(card, parentScreen.getSideboardPage());
                    });
        }

        @Override
        protected boolean needsCommander() {
            return false; //In a commander draft, they'll set the commander later.
        }

        protected LimitedPlayer getDraftPlayer() {
            //If we ever support online drafts, this'll need to be more complex.
            return parentScreen.getDraft().getHumanPlayer();
        }
    }

    public interface IDeckController {
        void setEditor(FDeckEditor editor);
        void setDeck(Deck deck);
        Deck getDeck();
        void newDeck();
        String getDeckDisplayName();
        void notifyModelChanged();
        void exitWithoutSaving();
        default boolean supportsSave() { return false; }
        default boolean supportsRename() { return false; }
        default boolean supportsDelete() { return false; }
        default String getNextAvailableName() {
            throw new UnsupportedOperationException();
        }
        default void save() {
            throw new UnsupportedOperationException();
        }
        default void rename(String name) {
            throw new UnsupportedOperationException();
        }
        default void saveAs(String name) {
            throw new UnsupportedOperationException();
        }
        default boolean isSaved() {
            return false;
        }
        default boolean delete() {
            throw new UnsupportedOperationException();
        }
    }

    public static class FileDeckController<T extends DeckBase> implements IDeckController {
        private T model;
        private boolean saved;
        private IStorage<T> rootFolder;
        private IStorage<T> currentFolder;
        private String modelPath;
        private FDeckEditor editor;
        private final Supplier<T> newModelCreator;
        private final Consumer<String> fnSetCurrentDeck;

        protected FileDeckController(final IStorage<T> folder, final Supplier<T> newModelCreator, Consumer<String> fnSetCurrentDeck) {
            setRootFolder(folder);
            this.newModelCreator = newModelCreator;
            this.fnSetCurrentDeck = fnSetCurrentDeck;
        }

        @Override
        public void setEditor(FDeckEditor editor) {
            this.editor = editor;
            if(editor != null)
                editor.notifyNewControllerModel();
        }

        public void setRootFolder(IStorage<T> folder0) {
            rootFolder = folder0;
            currentFolder = folder0;
            model = null;
            saved = true;
            modelPath = "";
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setDeck(final Deck deck) {
            model = (T)deck;
            currentFolder = rootFolder;
            modelPath = "";
            setSaved(false);
            if(editor != null)
                editor.notifyNewControllerModel();
        }

        @Override
        public Deck getDeck() {
            if(model == null)
                return null; //Shouldn't happen usually but can happen if a deck is deleted.
            return model.getHumanDeck();
        }

        @Override
        public void newDeck() {
            this.newModel();
            if(model instanceof DeckGroup)
                ((DeckGroup) model).setHumanDeck(new Deck());
        }

        @Override
        public String getDeckDisplayName() {
            String name = this.getModelName();
            if (name.isEmpty()) {
                name = "[" + Forge.getLocalizer().getMessage("lblNewDeck") + "]";
            }
            if (!saved && editor.allowSave()) {
                name = "*" + name;
            }
            return name;
        }

        protected void setModel(final T document) {
            setModel(document, false);
        }
        protected void setModel(final T document, final boolean isStored) {
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

            if(editor != null)
                editor.notifyNewControllerModel();
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

        @Override
        public void notifyModelChanged() {
            if (saved) {
                setSaved(false);
            }
        }

        private void setSaved(boolean val) {
            saved = val;

            if (editor != null) {
                editor.deckHeader.btnSave.setEnabled(!saved);
                editor.setHeaderText(getDeckDisplayName());
            }
        }

        @Override
        public void exitWithoutSaving() {
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

        @Override
        public boolean supportsSave() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void save() {
            if (model == null) {
                return;
            }

            // copy to new instance before adding to current folder so further changes are auto-saved
            currentFolder.add((T) model.copyTo(model.getName()));
            model.setDirectory(DeckProxy.getDeckDirectory(currentFolder));
            setSaved(true);

            //update saved deck names
            String deckStr = DeckProxy.getDeckString(modelPath, getModelName());
            if(this.fnSetCurrentDeck != null)
                this.fnSetCurrentDeck.accept(deckStr);
            editor.notifyNewControllerModel();
            if (editor.saveHandler != null) {
                editor.saveHandler.handleEvent(new FEvent(editor, FEventType.SAVE));
            }
        }

        @SuppressWarnings("unchecked")
        public void saveAs(final String name) {
            model = (T)model.copyTo(name);
            save();
        }

        @Override
        public boolean supportsRename() {
            return true;
        }

        @Override
        public void rename(final String name) {
            if (StringUtils.isEmpty(name)) { return; }

            String oldName = model.getName();
            if (name.equals(oldName)) { return; }

            saveAs(name);
            currentFolder.delete(oldName); //delete deck with old name
        }

        public String getNextAvailableName() {
            String name = model.getName();
            if(name.isEmpty())
                name = "New Deck";
            int idx = name.lastIndexOf('(');
            if (idx != -1) {
                name = name.substring(0, idx).trim(); //strip old number
            }

            String baseName = name;
            int number = 2;
            while (currentFolder.contains(name)) {
                name = baseName + " (" + number + ")";
                number++;
            }

            return name;
        }

        @Override
        public boolean isSaved() {
            return saved;
        }

        public void newModel() {
            setModel(newModelCreator.get());
        }

        private String getModelName() {
            return model != null ? model.getName() : "";
        }

        @Override
        public boolean supportsDelete() {
            return true;
        }

        @Override
        public boolean delete() {
            if (model == null) { return false; }
            currentFolder.delete(model.getName());
            setModel(null);
            return true;
        }
    }

    public static class FileDeckGroupController extends FileDeckController<DeckGroup> {

        protected FileDeckGroupController(IStorage<DeckGroup> folder, Supplier<DeckGroup> newModelCreator, Consumer<String> fnSetCurrentDeck) {
            super(folder, newModelCreator, fnSetCurrentDeck);
        }

        public void setDeckGroup(DeckGroup deckGroup) {
            this.setModel(deckGroup);
            this.setDeck(deckGroup.getHumanDeck());
        }
    }
}

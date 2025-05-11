package forge.adventure.scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.deck.*;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.*;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.toolbox.*;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class AdventureDeckEditor extends FDeckEditor {
    protected static class AdventureEditorConfig extends DeckEditorConfig {
        @Override public GameType getGameType() { return GameType.Adventure; }
        @Override public DeckFormat getDeckFormat() { return DeckFormat.Adventure; }
        @Override protected IDeckController getController() { return ADVENTURE_DECK_CONTROLLER; }
        @Override public boolean usePlayerInventory() { return true; }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            return new DeckEditorPage[]{
                    new CollectionCatalogPage(),
                    new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.ADVENTURE_EDITOR_POOL),
                    new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.ADVENTURE_SIDEBOARD),
                    new CollectionAutoSellPage()
            };
        }

        @Override
        public ItemPool<PaperCard> getCardPool(boolean wantUnique) {
            return Current.player().getCards();
        }

        @Override
        public CardEdition getBasicLandSet(Deck currentDeck) {
            return FModel.getMagicDb().getEditions().get("JMP");
        }
    }

    protected static class ShopConfig extends AdventureEditorConfig {
        @Override
        protected DeckEditorPage[] getInitialPages() {
            return new DeckEditorPage[]{
                    new StoreCatalogPage(),
                    new CollectionAutoSellPage()
            };
        }
    }

    protected static class DeckPreviewConfig extends AdventureEditorConfig {
        private final Deck deckToPreview;
        public DeckPreviewConfig(Deck deckToPreview) {
            this.deckToPreview = deckToPreview;
        }

        @Override public boolean usePlayerInventory() { return false; }
        @Override public boolean isLimited() { return true; }
        @Override public ItemPool<PaperCard> getCardPool(boolean wantUnique) { return deckToPreview.getAllCardsInASinglePool(); }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            return new DeckEditorPage[] {new ContentPreviewPage(deckToPreview)};
        }
    }

    protected static class AdventureEventEditorConfig extends DeckEditorConfig{
        protected AdventureEventData event;

        public AdventureEventEditorConfig(AdventureEventData event) {
            this.event = event;
        }

        @Override public GameType getGameType() { return GameType.AdventureEvent; }
        @Override public DeckFormat getDeckFormat() { return DeckFormat.Limited; }
        @Override public boolean isLimited() { return true; }
        @Override public boolean isDraft() { return event.getDraft() != null; }
        @Override protected IDeckController getController() { return ADVENTURE_DECK_CONTROLLER; }

        @Override
        public CardEdition getBasicLandSet(Deck currentDeck) {
            return DeckProxy.getDefaultLandSet(event.registeredDeck);
        }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            if (event.format == AdventureEventController.EventFormat.Draft) {
                switch (event.eventStatus) {
                    case Available:
                        return null;
                    case Started:
                    case Completed:
                    case Abandoned:
                    case Ready:
                        return new DeckEditorPage[]{
                                new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.DRAFT_POOL),
                                new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SIDEBOARD)
                        };
                    case Entered:
                        if (event.getDraft() != null)
                            return new DeckEditorPage[]{
                                new DraftPackPage(new AdventureCardManager()),
                                new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.DRAFT_POOL),
                                new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SIDEBOARD)
                            };
                    default:
                        return new DeckEditorPage[]{
                                new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.ADVENTURE_EDITOR_POOL),
                                new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.ADVENTURE_SIDEBOARD)
                        };

                }
            }
            if (event.format == AdventureEventController.EventFormat.Jumpstart) {
                return new DeckEditorPage[]{
                        new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.DRAFT_POOL),
                        new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SIDEBOARD)};
            }
            return new DeckEditorPage[]{};
        }
    }

    private static class ContentPreviewPage extends CatalogPage {
        Deck contents = new Deck();

        protected ContentPreviewPage(Deck cardsToShow) {
            super(new AdventureCardManager(), ItemManagerConfig.ADVENTURE_STORE_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
            contents = cardsToShow;
            cardManager.setBtnAdvancedSearchOptions(false);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            //No menus to show here, this page is only to be used for previewing the known contents of a sealed pack
        }

        @Override
        protected ItemPool<PaperCard> getCardPool() {
            return contents.getAllCardsInASinglePool();
        }

        @Override
        public void refresh() {
            if (contents != null) cardManager.setPool(contents.getAllCardsInASinglePool());
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            CardZoom.show(card);
        }
    }


    private static class StoreCatalogPage extends CatalogPage {
        protected StoreCatalogPage() {
            super(new AdventureCardManager(), ItemManagerConfig.ADVENTURE_STORE_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
            cardManager.setBtnAdvancedSearchOptions(true);
            scheduleRefresh();
        }

        @Override
        protected void initialize() {
            super.initialize();
            Current.player().onGoldChange(() -> ((AdventureDeckEditor) parentScreen).deckHeader.updateGold());
            cardManager.setPool(Current.player().getSellableCards());
            cardManager.setShowPriceInfo(true);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            Localizer localizer = Forge.getLocalizer();
            String label = localizer.getMessage("lblSellFor") + " " + Current.player().cardSellPrice(card);
            int sellable = cardManager.getItemCount(card);
            if(sellable <= 0)
                return;
            String prompt = card + " - " + label + " " + localizer.getMessage("lblHowMany");

            menu.addItem(new FMenuItem(label, SIDEBOARD_ICON, new MoveQuantityPrompt(prompt, sellable, result -> {
                    int sold = Current.player().sellCard(card, result);
                    removeCard(card, sold);
                })
            ));
        }

        @Override
        public void buildDeckMenu(FPopupMenu menu) {
            super.buildDeckMenu(menu);
            FMenuItem sellCurrentFilters = new FMenuItem(Forge.getLocalizer().getMessage("lblSellCurrentFilters"), FSkinImage.QUEST_COINSTACK, e1 -> sellAllByFilter());
            sellCurrentFilters.setTextColor(255, 0, 0);
            menu.addItem(sellCurrentFilters);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            CardZoom.show(card); //This should probably be replaced with call to longPress, where to get x,y params?
        }

        @Override
        public void refresh() {
            cardManager.setPool(Current.player().getSellableCards());
        }

        public void sellAllByFilter() {
            int value = 0;

            CardPool toSell = new CardPool();

            for (Map.Entry<PaperCard, Integer> entry : cardManager.getFilteredItems()) {
                toSell.add(entry.getKey(), entry.getValue());
                value += Current.player().cardSellPrice(entry.getKey()) * entry.getValue();
            }

            if(toSell.isEmpty())
                return;

            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblSellAllConfirm", toSell.countAll(), value), Forge.getLocalizer().getMessage("lblSellCurrentFilters"), Forge.getLocalizer().getMessage("lblSell"), Forge.getLocalizer().getMessage("lblCancel"), false, new Callback<>() {
                @Override
                public void run(Boolean result) {
                    if (result) {
                        Current.player().doBulkSell(toSell);
                        refresh();
                        //parentScreen.deckHeader.updateGold(); //TODO: Is this even needed?
                    }
                }
            });
        }
    }

    private static class CollectionCatalogPage extends CatalogPage {
        protected CollectionCatalogPage() {
            super(new AdventureCardManager(), ItemManagerConfig.ADVENTURE_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
        }

        @Override
        protected void initialize() {
            super.initialize();
            cardManager.setBtnAdvancedSearchOptions(true);
            cardManager.setCatalogDisplay(true);
            scheduleRefresh();
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            super.buildMenu(menu, card);

            if (!(parentScreen instanceof AdventureDeckEditor adventureEditor) || adventureEditor.getAutoSellPage() == null)
                return;

            Localizer localizer = Forge.getLocalizer();
            String lblHowMany = localizer.getMessage("lblHowMany");

            int amountInCollection = cardManager.getItemCount(card);
            CollectionAutoSellPage autoSellPage = adventureEditor.getAutoSellPage();
            int sellableCount = amountInCollection - Current.player().getCopiesUsedInDecks(card);
            int autoSellCount = Current.player().autoSellCards.count(card);

            if (card.hasNoSellValue()) {
                String prompt = String.format("%s - %s %s", card, localizer.getMessage("lblRemove"), lblHowMany);
                FMenuItem removeItem = new FMenuItem(localizer.getMessage("lblRemove"), FSkinImage.HDDELETE, new MoveQuantityPrompt(prompt, sellableCount, amount -> {
                    int sold = Current.player().sellCard(card, amount);
                    removeCard(card, sold);
                }));
                menu.addItem(removeItem);
                return;
            }

            if (sellableCount > 0) {
                String action = localizer.getMessage("lbltoSell", sellableCount, autoSellCount);
                String prompt = String.format("%s - %s %s", card, action, lblHowMany);
                FMenuItem moveToAutosell = new FMenuItem(action, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, new MoveQuantityPrompt(prompt, sellableCount, amount -> {
                    //Auto-sell page adds to and removes from the player's auto-sell pool.
                    //The auto-sell pool is part of the overall pool so there's no need to edit anything on our end either.
                    autoSellPage.addCard(card, amount);
                    removeCard(card, amount);
                }));
                menu.addItem(moveToAutosell);
            }

            if (autoSellCount > 0) {
                String action = localizer.getMessage("lbltoInventory", sellableCount, autoSellCount);
                String prompt = String.format("%s - %s %s", card, action, lblHowMany);
                FMenuItem moveToCatalog = new FMenuItem(action, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, new MoveQuantityPrompt(prompt, autoSellCount, amount -> {
                    autoSellPage.removeCard(card, amount);
                    addCard(card, amount);
                }));
                menu.addItem(moveToCatalog);
            }
        }

        @Override
        public void setCardFavorited(PaperCard card, boolean isFavorite) {
            AdventurePlayer player = Current.player();
            if(isFavorite)
                player.favoriteCards.add(card);
            else
                player.favoriteCards.remove(card);
        }
        @Override protected boolean cardIsFavorite(PaperCard card) { return Current.player().favoriteCards.contains(card); }
        @Override protected boolean allowFavoriteCards() { return true; }
    }

    protected static class CollectionAutoSellPage extends CatalogPage {
        private final String captionPrefix;

        protected CollectionAutoSellPage() {
            super(new AdventureCardManager(), ItemManagerConfig.ADVENTURE_EDITOR_POOL, Forge.getLocalizer().getMessage("lblAutoSell"), AUTO_SELL_ICON);
            this.captionPrefix = Forge.getLocalizer().getMessage("lblAutoSell");
        }

        @Override
        protected void updateCaption() {
            caption = captionPrefix + " (" + cardManager.getItemCount() + ")";
        }

        @Override
        protected void initialize() {
            super.initialize();
            cardManager.setBtnAdvancedSearchOptions(true);
            cardManager.setCatalogDisplay(true);
            scheduleRefresh();
        }

        @Override
        protected ItemPool<PaperCard> getCardPool() {
            //No need to override addCard and removeCard, because autoSellCards IS the card pool here.
            //It'll be updated automatically as cards are added and removed from this page.
            return Current.player().getAutoSellCards();
        }


        protected boolean isShop() {
            return parentScreen.getEditorConfig() instanceof ShopConfig;
        }

        @Override
        protected void buildMenu(FDropDownMenu menu, PaperCard card) {
            super.buildMenu(menu, card);
            Localizer localizer = Forge.getLocalizer();
            AdventurePlayer player = Current.player();
            if(isShop()) {
                String label = localizer.getMessage("lblSellFor") + " " + player.cardSellPrice(card);
                int sellable = cardManager.getItemCount(card);
                if(sellable <= 0)
                    return;
                String prompt = card + " - " + label + " " + localizer.getMessage("lblHowMany");

                menu.addItem(new FMenuItem(label, SIDEBOARD_ICON, new MoveQuantityPrompt(prompt, sellable, result -> {
                        int sold = player.sellCard(card, result);
                        removeCard(card, sold);
                    })
                ));
            }
            if(parentScreen instanceof AdventureDeckEditor adventureEditor && adventureEditor.getCatalogPage() != null) {
                CollectionCatalogPage catalogPage = (CollectionCatalogPage) adventureEditor.getCatalogPage();
                int autoSellCount = cardManager.getItemCount(card);
                int amountInCollection = player.getCards().count(card) - autoSellCount;
                int sellableCount = amountInCollection - player.getCopiesUsedInDecks(card);

                String action = localizer.getMessage("lbltoInventory", sellableCount, autoSellCount);
                String prompt = String.format("%s - %s %s", card, action, localizer.getMessage("lblHowMany"));
                FMenuItem moveToCatalog = new FMenuItem(action, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, new MoveQuantityPrompt(prompt, autoSellCount, amount -> {
                    removeCard(card, amount);
                    catalogPage.addCard(card, amount);
                }));
                menu.addItem(moveToCatalog);
            }

        }

        @Override
        protected void onCardActivated(PaperCard card) {
            if(isShop()) {
                Current.player().sellCard(card, 1);
                removeCard(card, 1);
            }
            //Move to deck? Back to catalog? Unclear.
        }
    }

    @Override
    public BoosterDraft getDraft() {
        if (currentEvent == null)
            return null;
        return currentEvent.getDraft();
    }

    @Override
    public boolean isDrafting() {
        return currentEvent != null && !currentEvent.isDraftComplete;
    }

    public static AdventureEventData currentEvent;

    public void setEvent(AdventureEventData event) {
        currentEvent = event;
    }

    @Override
    public void completeDraft() {
        super.completeDraft();
        currentEvent.isDraftComplete = true;
        Deck[] opponentDecks = currentEvent.getDraft().getDecks();
        for (int i = 0; i < currentEvent.participants.length && i < opponentDecks.length; i++) {
            currentEvent.participants[i].setDeck(opponentDecks[i]);
        }
        currentEvent.draftedDeck = (Deck) currentEvent.registeredDeck.copyTo("Draft Deck");
        if (allowsAddBasic()) {
            showAddBasicLandsDialog();
            //Might be annoying if you haven't pruned your deck yet, but best to remind player that
            //this probably needs to be done since it's there since it's not normally part of Adventure
        }
        if (currentEvent.eventStatus == AdventureEventController.EventStatus.Entered) {
            currentEvent.eventStatus = AdventureEventController.EventStatus.Ready;
        }
    }

    private static final FileHandle deckIcon = Config.instance().getFile("ui/maindeck.png");
    private static final FImage MAIN_DECK_ICON = deckIcon.exists() ? new FImage() {
        @Override
        public float getWidth() {
            return 100f;
        }

        @Override
        public float getHeight() {
            return 100f;
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            g.drawImage(Forge.getAssets().getTexture(deckIcon), x, y, w, h);
        }
    } : Forge.hdbuttons ? FSkinImage.HDLIBRARY : FSkinImage.DECKLIST;
    private static final FileHandle sideIcon = Config.instance().getFile("ui/sideboard.png");
    private static final FImage SIDEBOARD_ICON = sideIcon.exists() ? new FImage() {
        @Override
        public float getWidth() {
            return 100f;
        }

        @Override
        public float getHeight() {
            return 100f;
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            g.drawImage(Forge.getAssets().getTexture(sideIcon), x, y, w, h);
        }
    } : Forge.hdbuttons ? FSkinImage.HDSIDEBOARD : FSkinImage.FLASHBACK;

    private static final FileHandle binderIcon = Config.instance().getFile("ui/binder.png");
    private static final FImage CATALOG_ICON = binderIcon.exists() ? new FImage() {
        @Override
        public float getWidth() {
            return 100f;
        }

        @Override
        public float getHeight() {
            return 100f;
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            g.drawImage(Forge.getAssets().getTexture(binderIcon), x, y, w, h);
        }
    } : FSkinImage.QUEST_BOX;

    private static final FImage AUTO_SELL_ICON = FSkinImage.HDEXILE; //to-maybe-do: Custom adventure icon for this? Adventure should really just have its own skin.

    public static FImage iconFromDeckSection(DeckSection deckSection) {
        if(deckSection == DeckSection.Main)
            return MAIN_DECK_ICON;
        if(deckSection == DeckSection.Sideboard)
            return FDeckEditor.SIDEBOARD_ICON;
        return FDeckEditor.iconFromDeckSection(deckSection);
    }

    private static ItemPool<InventoryItem> decksUsingMyCards = new ItemPool<>(InventoryItem.class);

    public static void leave() {
        if (currentEvent != null && currentEvent.getDraft() != null && !currentEvent.isDraftComplete) {
            Localizer localizer = Forge.getLocalizer();
            String confirmPrompt = localizer.getMessageorUseDefault("lblEndAdventureEventConfirm", "This will end the current event, and your entry fee will not be refunded.\n\nLeave anyway?");
            FOptionPane.showConfirmDialog(confirmPrompt, localizer.getMessage("lblLeaveDraft"), localizer.getMessage("lblLeave"), localizer.getMessage("lblCancel"), false, new Callback<>() {
                @Override
                public void run(Boolean result) {
                    if (result) {
                        currentEvent.eventStatus = AdventureEventController.EventStatus.Abandoned;
                        Current.player().newCards.clear();
                        Forge.clearCurrentScreen();
                        Forge.switchToLast();
                    }
                }
            });
        } else {
            Current.player().newCards.clear();
            Forge.clearCurrentScreen();
            Forge.switchToLast();
        }
    }

    @Override
    public void onActivate() {
        decksUsingMyCards = new ItemPool<>(InventoryItem.class);
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            final Deck deck = Current.player().getDeck(i);
            CardPool main = deck.getMain();
            for (final Map.Entry<PaperCard, Integer> e : main) {
                decksUsingMyCards.add(e.getKey());
            }
            if (deck.has(DeckSection.Sideboard)) {
                for (final Map.Entry<PaperCard, Integer> e : deck.get(DeckSection.Sideboard)) {
                    // only add card if we haven't already encountered it in main
                    if (!main.contains(e.getKey())) {
                        decksUsingMyCards.add(e.getKey());
                    }
                }
            }
        }
        deckHeader.updateGold();

//            if (currentEvent.registeredDeck!=null && !currentEvent.registeredDeck.isEmpty()){
//                //Use this deck instead of selected deck
//            }
    }

    public void refresh() {
        for (TabPage<FDeckEditor> page : tabPages) {
            if (page instanceof CatalogPage)
                ((CatalogPage) page).scheduleRefresh();
            else if (page instanceof CardManagerPage)
                ((CardManagerPage) page).refresh();
        }
    }

    protected enum CatalogFilterOption {
        COLLECTION("lblCollection"),
        SELLABLE("lblSellable"),
        AUTO_SELLABLE("lblAutoSellable"),
        NON_SELLABLE("lblNonSellable");

        private final String label;
        CatalogFilterOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return Forge.getLocalizer().getMessage(label);
        }
    }


    boolean isShop;
    protected AdventureDeckHeader deckHeader;
    protected FDraftLog draftLog;
    protected CollectionAutoSellPage autoSellPage;

    public AdventureDeckEditor(boolean createAsShop) {
        super(createAsShop ? new ShopConfig() : new AdventureEditorConfig(),
                createAsShop ? null : Current.player().getSelectedDeck(),
                e -> leave());
        isShop = createAsShop;
        if(createAsShop)
            setHeaderText(Forge.getLocalizer().getMessage("lblSell"));
    }

    public AdventureDeckEditor(AdventureEventData event) {
        super(new AdventureEventEditorConfig(event), e -> leave());
        currentEvent = event;

        if(event.getDraft() != null) {
            this.draftLog = new FDraftLog();
            event.getDraft().setLogEntry(this.draftLog);
            deckHeader.initDraftLog(this.draftLog, this);
        }
    }

    public AdventureDeckEditor(Deck deckToPreview) {
        super(new DeckPreviewConfig(deckToPreview), e -> leave());
    }

    @Override
    protected DeckHeader initDeckHeader() {
        this.deckHeader = add(new AdventureDeckHeader());
        return this.deckHeader;
    }

    @Override
    protected FPopupMenu createMoreOptionsMenu() {
        return new FPopupMenu() {
            @Override
            protected void buildMenu() {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, e1 -> FDeckViewer.copyDeckToClipboard(getDeck())));
                if (allowsAddBasic()) {
                    FMenuItem addBasic = new FMenuItem(Forge.getLocalizer().getMessage("lblAddBasicLands"), FSkinImage.LANDLOGO, e1 -> showAddBasicLandsDialog());
                    addItem(addBasic);
                }
                ((DeckEditorPage) getSelectedPage()).buildDeckMenu(this);
            }
        };
    }

    @Override
    protected void cacheTabPages() {
        super.cacheTabPages();
        for(TabPage<FDeckEditor> page : tabPages) {
            if(page instanceof CollectionAutoSellPage)
                this.autoSellPage = (CollectionAutoSellPage) page;
        }
    }

    @Override
    protected boolean allowsAddBasic() {
        if (currentEvent == null)
            return true;
        if (!currentEvent.eventRules.allowsAddBasicLands)
            return false;
        if (isDrafting())
            return false;
        if (currentEvent.eventStatus == AdventureEventController.EventStatus.Entered
                || currentEvent.eventStatus == AdventureEventController.EventStatus.Ready)
            return true;
        return false;
    }

    private static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare = from -> Current.player().newCards.contains((PaperCard) from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
    private static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnNewGet = from -> Current.player().newCards.contains((PaperCard) from.getKey()) ? "NEW" : "";
    private static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = from -> decksUsingMyCards.count(from.getKey());
    private static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = from -> Integer.valueOf(decksUsingMyCards.count(from.getKey())).toString();
    private static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnPriceCompare = from -> Current.player().cardSellPrice((PaperCard) from.getKey());
    private static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnPriceGet = from -> Current.player().cardSellPrice((PaperCard) from.getKey());
    private static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnFavoriteCompare = from -> Current.player().favoriteCards.contains((PaperCard) from.getKey());
    private static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnFavoriteGet = from -> Current.player().favoriteCards.contains((PaperCard) from.getKey()) ? 1 : 0;

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
        //TODO: Feel like one InventoryItem entry to Comparable function should be sufficient for each of these. Maybe tinker with the signatures in ItemColumn...
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, fnNewCompare, fnNewGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, fnDeckCompare, fnDeckGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.PRICE, fnPriceCompare, fnPriceGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.FAVORITE, fnFavoriteCompare, fnFavoriteGet);
        return colOverrides;
    }

    @Override
    public Deck getDeck() {
        if (currentEvent == null)
            return Current.player().getSelectedDeck();
        else
            return currentEvent.registeredDeck;
    }

    private CollectionAutoSellPage getAutoSellPage() {
        return autoSellPage;
    }

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        String errorMessage = GameType.Adventure.getDeckFormat().getDeckConformanceProblem(getDeck());
        if (errorMessage != null) {
            FOptionPane.showErrorDialog(errorMessage);
        }

        // if currentEvent is null, it should have been cleared or overwritten somehow
        if (currentEvent != null && currentEvent.getDraft() != null && !isShop) {
            if (currentEvent.isDraftComplete || canCloseCallback == null) {
                super.onClose(canCloseCallback); //can skip prompt if draft saved
                return;
            }
            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessageorUseDefault("lblEndAdventureEventConfirm", "This will end the current event, and your entry fee will not be refunded.\n\nLeave anyway?"), Forge.getLocalizer().getMessage("lblLeaveDraft"), Forge.getLocalizer().getMessage("lblLeave"), Forge.getLocalizer().getMessage("lblCancel"), false, canCloseCallback);
        }
    }



    private static class AdventureCatalogFilter extends ItemFilter<PaperCard> {
        private boolean preventHandling = false;
        private final FComboBox<CatalogFilterOption> catalogDisplay = new FComboBox<>();

        public AdventureCatalogFilter(AdventureCardManager itemManager) {
            super(itemManager);
            catalogDisplay.setFont(FSkinFont.get(12));
            catalogDisplay.addItem(CatalogFilterOption.COLLECTION);
            catalogDisplay.addItem(CatalogFilterOption.SELLABLE);
            catalogDisplay.addItem(CatalogFilterOption.AUTO_SELLABLE);
            catalogDisplay.addItem(CatalogFilterOption.NON_SELLABLE);

            catalogDisplay.setChangedHandler(e -> {
                if (preventHandling)
                    return;
                //Need to either pipe this over to the deck editor or handle the filtering within the card manager.Latter sounds promising.
                itemManager.setCatalogFilter(catalogDisplay.getSelectedItem());
                itemManager.refresh();
            });
        }

        @Override
        public void reset() {
            preventHandling = true;
            catalogDisplay.setSelectedIndex(0);
            preventHandling = false;
        }

        @Override
        public FDisplayObject getMainComponent() {
            return catalogDisplay;
        }

        @Override
        public ItemFilter<PaperCard> createCopy() {
            AdventureCatalogFilter copy = new AdventureCatalogFilter((AdventureCardManager) itemManager);
            copy.preventHandling = true;
            copy.catalogDisplay.setSelectedIndex(catalogDisplay.getSelectedIndex());
            copy.preventHandling = false;
            return copy;
        }

        @Override
        protected void buildWidget(ItemFilter<PaperCard>.Widget widget) {
            widget.add(catalogDisplay);
        }

        @Override
        protected void doWidgetLayout(float width, float height) {
            catalogDisplay.setSize(width, height);
        }

        @Override
        protected Predicate<PaperCard> buildPredicate() {
            //Predicate won't suffice for this. We need to be able to remove specific quantities from the pool.
            return x -> true;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    protected static class AdventureCardManager extends CardManager {
        private boolean showCollectionCards = true, showAutoSellCards = false, showNoSellCards = true;
        private CatalogFilterOption filterOption;

        public AdventureCardManager() {
            super(false);
        }

        @Override
        protected void addDefaultFilters() {
            this.addFilter(new CardColorFilter(this));
            //this.addFilter(new AdventureCatalogFilter(this));
            this.addFilter(new CardTypeFilter(this));
        }

        protected void setCatalogFilter(CatalogFilterOption filter) {
            this.filterOption = filter;
            switch (filter) {
                case COLLECTION:
                    showCollectionCards = true;
                    showAutoSellCards = false;
                    showNoSellCards = true;
                    break;
                case SELLABLE:
                    showCollectionCards = true;
                    showAutoSellCards = false;
                    showNoSellCards = false;
                    break;
                case AUTO_SELLABLE:
                    showCollectionCards = false;
                    showAutoSellCards = true;
                    showNoSellCards = false;
                    break;
                case NON_SELLABLE:
                    showCollectionCards = false;
                    showAutoSellCards = false;
                    showNoSellCards = true;
                    break;
            }
        }

        public CatalogFilterOption getCatalogFilter() {
            return filterOption;
        }

        @Override
        public ItemPool<PaperCard> getFilteredItems() {
            ItemPool<PaperCard> pool;
            AdventurePlayer player = AdventurePlayer.current();
            if(showCollectionCards) {
                pool = super.getFilteredItems();
                if(!showNoSellCards)
                    pool.removeIf(PaperCard::hasNoSellValue);
                if(!showAutoSellCards)
                    pool.removeAll(player.getAutoSellCards());
            }
            else {
                pool = new CardPool();
                if(showNoSellCards) {
                    pool.addAll(super.getFilteredItems());
                    pool.retainIf(PaperCard::hasNoSellValue);
                }
                if(showAutoSellCards)
                    pool.addAll(player.getAutoSellCards());
            }
            return pool;
        }

        @Override
        protected String getItemSuffix(Map.Entry<PaperCard, Integer> item) {
            PaperCard card = item.getKey();
            String parentSuffix = super.getItemSuffix(item);
            if(card.hasNoSellValue()) {
                String valueText = " [NO VALUE]";
                if(parentSuffix == null)
                    return valueText;
                return String.join(" ", valueText, parentSuffix);
            }
            return parentSuffix;
        }

        @Override
        public ItemManager<PaperCard>.ItemRenderer getListItemRenderer(FList.CompactModeHandler compactModeHandler) {
            return new CardListItemRenderer(compactModeHandler) {
                @Override
                public void drawValue(Graphics g, Map.Entry<PaperCard, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    super.drawValue(g, value, font, foreColor, backColor, pressed, x, y, w, h);

                    if(showPriceInfo()) {
                        float totalHeight = h + 2 * FList.PADDING;
                        float cardArtWidth = totalHeight * CardRenderer.CARD_ART_RATIO;

                        String price = String.valueOf(Current.player().cardSellPrice(value.getKey()));
                        float priceHeight = font.getLineHeight();
                        y += totalHeight - priceHeight - FList.PADDING;
                        g.fillRect(backColor, x - FList.PADDING, y, cardArtWidth, priceHeight);
                        g.drawImage(FSkinImage.QUEST_COINSTACK, x, y, priceHeight, priceHeight);
                        float offset = priceHeight * 1.1f;
                        g.drawText(price, font, foreColor, x + offset, y, cardArtWidth - offset - 2 * FList.PADDING, priceHeight, false, Align.left, true);
                    }
                }
            };
        }
    }

    protected static class AdventureDeckHeader extends DeckHeader {
        private static final FileHandle sellIcon = Config.instance().getFile("ui/sell.png");
        public final FLabel lblGold;

        protected AdventureDeckHeader() {
            super();
            this.lblGold = new FLabel.Builder().text("0").icon(Forge.getAssets().getTexture(sellIcon) == null ? FSkinImage.QUEST_COINSTACK :
                    new FImage() {
                        @Override
                        public float getWidth() {
                            return 100f;
                        }

                        @Override
                        public float getHeight() {
                            return 100f;
                        }

                        @Override
                        public void draw(Graphics g, float x, float y, float w, float h) {
                            g.drawImage(Forge.getAssets().getTexture(sellIcon), x, y, w, h);
                        }
                    }
            ).font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build();
            this.add(lblGold);
        }

        @Override
        protected List<FDisplayObject> layoutHeaderElements(float height, float availableWidth) {
            List<FDisplayObject> out = super.layoutHeaderElements(height, availableWidth);
            float remainingWidth = availableWidth - (float) out.stream().mapToDouble(FDisplayObject::getWidth).sum();
            float width = Math.max(remainingWidth / 4, Math.min(height * 4, remainingWidth)); // Will push out name label if it has to.
            lblGold.setSize(width, height);
            out.add(lblGold);
            return out;
        }

        public void updateGold() {
            lblGold.setText(String.valueOf(Current.player().getGold()));
        }
    }

    protected static class AdventureDeckSectionPage extends DeckSectionPage {
        protected AdventureDeckSectionPage(DeckSection deckSection, ItemManagerConfig config) {
            super(new AdventureCardManager(), deckSection, config, deckSection.getLocalizedShortName(), iconFromDeckSection(deckSection));
            cardManager.setBtnAdvancedSearchOptions(deckSection == DeckSection.Main);
            cardManager.setCatalogDisplay(false);
        }
    }

    private static final AdventureDeckController ADVENTURE_DECK_CONTROLLER = new AdventureDeckController();
    /**
     * Barebones deck controller. Doesn't really need to do anything since Adventure Decks are updated in real time
     * while they're edited, and they're only saved when the adventure is saved.
     */
    private static class AdventureDeckController implements IDeckController {
        FDeckEditor editor;
        Deck currentDeck;

        @Override
        public void setEditor(FDeckEditor editor) {
            this.editor = editor;
            editor.notifyNewControllerModel();
        }

        @Override public void setDeck(Deck deck) {
            this.currentDeck = deck;
            if(editor != null)
                editor.notifyNewControllerModel();
        }
        @Override public Deck getDeck() { return currentDeck; }
        @Override public void newDeck() {
            setDeck(new Deck("Adventure Deck"));
        }

        @Override
        public String getDeckDisplayName() {
            if(currentDeck == null)
                return "New Deck";
            return currentDeck.getName();
        }

        @Override public void notifyModelChanged() {} //
        @Override public void exitWithoutSaving() {} //Too many external variables to just revert the deck. Not supported for now.
    }
}


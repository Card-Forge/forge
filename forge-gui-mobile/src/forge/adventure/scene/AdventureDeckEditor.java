package forge.adventure.scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import forge.Forge;
import forge.Graphics;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
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
                    new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.ADVENTURE_SIDEBOARD)
            };
        }

        @Override
        public ItemPool<PaperCard> getCardPool(boolean wantUnique) {
            return AdventurePlayer.current().getCards();
        }

        @Override
        public CardEdition getBasicLandSet(Deck currentDeck) {
            return FModel.getMagicDb().getEditions().get("JMP");
        }
    }

    protected static class ShopConfig extends AdventureEditorConfig {
        @Override
        protected DeckEditorPage[] getInitialPages() {
            return new DeckEditorPage[]{new StoreCatalogPage()};
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
                        return new DeckEditorPage[]{
                                event.getDraft() != null ? (new DraftPackPage()) :
                                        new AdventureCatalogPage(ItemManagerConfig.DRAFT_PACK, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON),
                                new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.DRAFT_POOL),
                                new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.SIDEBOARD)
                        };
                    default:
                        return new DeckEditorPage[]{
                                new AdventureCatalogPage(ItemManagerConfig.ADVENTURE_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON),
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
            super(ItemManagerConfig.ADVENTURE_STORE_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
            contents = cardsToShow;
            cardManager.setBtnAdvancedSearchOptions(false);
            refresh();
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            //No menus to show here, this page is only to be used for previewing the known contents of a sealed pack
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
            super(ItemManagerConfig.ADVENTURE_STORE_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
            Current.player().onGoldChange(() -> ((AdventureDeckEditor) parentScreen).deckHeader.updateGold());
            cardManager.setBtnAdvancedSearchOptions(false);
            refresh();
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            Localizer localizer = Forge.getLocalizer();
            String label = localizer.getMessage("lblSellFor") + " " + AdventurePlayer.current().cardSellPrice(card);
            int sellable = cardManager.getItemCount(card);
            if(sellable <= 0)
                return;
            String prompt = card + " - " + label + " " + localizer.getMessage("lblHowMany");
            menu.addItem(new FMenuItem(label, SIDEBOARD_ICON, (e) -> {
                if(sellable == 1) {
                    AdventurePlayer.current().sellCard(card, 1);
                    return;
                }
                GuiChoose.getInteger(prompt, 1, sellable, new Callback<>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) {
                            return;
                        }
                        AdventurePlayer.current().sellCard(card, result);
                        //((AdventureDeckEditor) parentScreen).deckHeader.updateGold(); //TODO: Is this needed?
                    }
                });
            }));
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            CardZoom.show(card); //This should probably be replaced with call to longPress, where to get x,y params?
        }

        @Override
        protected boolean canAddCards() {
            return false;
        }

        @Override
        public void refresh() {
            cardManager.setPool(AdventurePlayer.current().getSellableCards());
        }
    }

    private static class CollectionCatalogPage extends CatalogPage {
        protected CollectionCatalogPage() {
            super(ItemManagerConfig.ADVENTURE_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), CATALOG_ICON);
            cardManager.setBtnAdvancedSearchOptions(true);
            cardManager.setCatalogDisplay(true);
            cardManager.setShowNFSWatermark(true);
            refresh();
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            super.buildMenu(menu, card);

            int noSellCount = Current.player().noSellCards.count(card);
            int autoSellCount = Current.player().autoSellCards.count(card);
            int sellableCount = Current.player().getSellableCards().count(card);

            if (noSellCount > 0) {
                FMenuItem unsellableCount = new FMenuItem(Forge.getLocalizer().getMessage("lblUnsellableCount", noSellCount), null, null);
                unsellableCount.setEnabled(false);
                menu.addItem(unsellableCount);
            }

            if (sellableCount > 0) {
                FMenuItem moveToAutosell = new FMenuItem(Forge.getLocalizer().getMessage("lbltoSell", autoSellCount, sellableCount), Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, e1 -> {
                    Current.player().autoSellCards.add(card);
                    refresh();
                });
                moveToAutosell.setEnabled(sellableCount - autoSellCount > 0);
                menu.addItem(moveToAutosell);

                FMenuItem moveToCatalog = new FMenuItem(Forge.getLocalizer().getMessage("lbltoInventory", autoSellCount, sellableCount), Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, e1 -> {
                    Current.player().autoSellCards.remove(card);
                    refresh();
                });
                moveToCatalog.setEnabled(autoSellCount > 0);
                menu.addItem(moveToCatalog);
            }
        }

        public void sellAllByFilter() {
            // allow selling on catalog page only
            if (((AdventureCardManager) cardManager).getCatalogFilter() == CatalogFilterOption.SELLABLE) {
                int count = 0;
                int value = 0;

                for (Map.Entry<PaperCard, Integer> entry : cardManager.getFilteredItems()) {
                    value += AdventurePlayer.current().cardSellPrice(entry.getKey()) * entry.getValue();
                    count += entry.getValue();
                }

                FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblSellAllConfirm", count, value), Forge.getLocalizer().getMessage("lblSellCurrentFilters"), Forge.getLocalizer().getMessage("lblSell"), Forge.getLocalizer().getMessage("lblCancel"), false, new Callback<>() {
                    @Override
                    public void run(Boolean result) {
                        if (result) {
                            for (Map.Entry<PaperCard, Integer> entry : cardManager.getFilteredItems()) {
                                AdventurePlayer.current().sellCard(entry.getKey(), entry.getValue());
                            }
                            refresh();
                            //parentScreen.deckHeader.updateGold(); //TODO: Is this even needed?
                        }
                    }
                });
            } else {
                FOptionPane.showErrorDialog(Forge.getLocalizer().getMessage("lblChoose") + " " +
                        Forge.getLocalizer().getMessage("lblSellable"));
            }
        }
    }

    @Override
    public BoosterDraft getDraft() {
        if (currentEvent == null)
            return null;
        return currentEvent.getDraft();
    }

    public static AdventureEventData currentEvent;

    public void setEvent(AdventureEventData event) {
        currentEvent = event;
    }

    @Override
    public void completeDraft() {
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
                        AdventurePlayer.current().getNewCards().clear();
                        Forge.clearCurrentScreen();
                        Forge.switchToLast();
                    }
                }
            });
        } else {
            AdventurePlayer.current().getNewCards().clear();
            Forge.clearCurrentScreen();
            Forge.switchToLast();
        }
    }

    @Override
    public void onActivate() {
        decksUsingMyCards = new ItemPool<>(InventoryItem.class);
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            final Deck deck = AdventurePlayer.current().getDeck(i);
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
            if (page instanceof CardManagerPage)
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

    public AdventureDeckEditor(boolean createAsShop) {
        super(createAsShop ? new ShopConfig() : new AdventureEditorConfig(), e -> leave());
        isShop = createAsShop;
    }

    public AdventureDeckEditor(AdventureEventData event) {
        super(new AdventureEventEditorConfig(event), e -> leave());
        currentEvent = event;
    }

    public AdventureDeckEditor(Deck deckToPreview) {
        super(new DeckPreviewConfig(deckToPreview), e -> leave());
    }

    @Override
    protected DeckHeader initDeckHeader() {
        this.deckHeader = new AdventureDeckHeader();
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
                if (!isShop && catalogPage != null && catalogPage instanceof CollectionCatalogPage && !(getEditorConfig() instanceof AdventureEventEditorConfig)) {
                    // Add bulk sell menu option. This will sell all cards in the current filter.
                    addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSellCurrentFilters"), FSkinImage.QUEST_COINSTACK, e1 -> ((CollectionCatalogPage) catalogPage).sellAllByFilter()));
                }
                ((DeckEditorPage) getSelectedPage()).buildDeckMenu(this);
            }
        };
    }

    @Override
    protected boolean allowsAddBasic() {
        if (currentEvent == null)
            return true;
        if (!currentEvent.eventRules.allowsAddBasicLands)
            return false;
        if (currentEvent.eventStatus == AdventureEventController.EventStatus.Entered && currentEvent.isDraftComplete)
            return true;
        else return currentEvent.eventStatus == AdventureEventController.EventStatus.Ready;
    }

    private final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare = from -> AdventurePlayer.current().getNewCards().contains((PaperCard) from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
    private final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnNewGet = from -> AdventurePlayer.current().getNewCards().contains((PaperCard) from.getKey()) ? "NEW" : "";
    public static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = from -> decksUsingMyCards.count(from.getKey());
    public static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = from -> Integer.valueOf(decksUsingMyCards.count(from.getKey())).toString();

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, fnNewCompare, fnNewGet);
        ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, fnDeckCompare, fnDeckGet);
        return colOverrides;
    }

    @Override
    public Deck getDeck() {
        if (currentEvent == null)
            return AdventurePlayer.current().getSelectedDeck();
        else
            return currentEvent.registeredDeck;
    }

    protected AdventureCatalogPage getCatalogPage() {
        return (AdventureCatalogPage) catalogPage;
    }

    @Override
    protected CardManager createCardManager() {
        return new AdventureCardManager();
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
            this.addFilter(new AdventureCatalogFilter(this));
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
                    pool.removeAll(player.getNoSellCards());
                if(!showAutoSellCards)
                    pool.removeAll(player.getAutoSellCards());
            }
            else {
                pool = new CardPool();
                if(showNoSellCards)
                    pool.addAll(player.getNoSellCards());
                if(showAutoSellCards)
                    pool.addAll(player.getAutoSellCards());
            }
            return pool;
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
        }

        @Override
        @SuppressWarnings("SuspiciousNameCombination")
        protected void doLayout(float width, float height) {
            float x = 0;
            lblName.setBounds(0, 0, width - height, height);
            x += lblName.getWidth();
            btnMoreOptions.setBounds(x, 0, height, height);
            lblGold.setBounds(0, 0, width, height); //TODO: How did this work...?
        }

        public void updateGold() {
            lblGold.setText(String.valueOf(AdventurePlayer.current().getGold()));
        }
    }


    protected static class AdventureCatalogPage extends CatalogPage {
        protected final AdventureCardManager cardManager;

        protected AdventureCatalogPage(ItemManagerConfig config, String caption, FImage icon) {
            super(config, caption, icon);
            this.cardManager = (AdventureCardManager) super.cardManager;
            refresh();
        }

        //TODO: buildMenu had an isShop check in it. Ensure that wasn't really needed.
    }

    protected static class AdventureDeckSectionPage extends DeckSectionPage {
        protected AdventureDeckSectionPage(DeckSection deckSection, ItemManagerConfig config) {
            super(deckSection, config, deckSection.getLocalizedShortName(), iconFromDeckSection(deckSection));
            cardManager.setBtnAdvancedSearchOptions(deckSection == DeckSection.Main);
            cardManager.setCatalogDisplay(false);
            refresh();
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
        }

        @Override public void setDeck(Deck deck) {
            this.currentDeck = deck;
            if(editor != null)
                editor.setDeck(deck);
        }
        @Override public Deck getDeck() { return currentDeck; }
        @Override public void newDeck() { this.currentDeck = new Deck("Adventure Deck"); }

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


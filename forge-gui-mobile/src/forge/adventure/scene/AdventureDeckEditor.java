package forge.adventure.scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.data.AdventureEventData;
import forge.adventure.data.ItemData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.AdventureModes;
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
import forge.gui.FThreads;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.*;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.toolbox.*;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.Utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class AdventureDeckEditor extends FDeckEditor {
    protected static class AdventureEditorConfig extends DeckEditorConfig {
        @Override
        public GameType getGameType() {
            return GameType.Adventure;
        }

        @Override
        public DeckFormat getDeckFormat() {
            return AdventurePlayer.current().getAdventureMode() == AdventureModes.Commander ? DeckFormat.Commander : DeckFormat.Adventure;
        }

        @Override
        protected IDeckController getController() {
            return ADVENTURE_DECK_CONTROLLER;
        }

        @Override
        public boolean usePlayerInventory() {
            return true;
        }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            if (AdventurePlayer.current().getAdventureMode() == AdventureModes.Commander)
                return new DeckEditorPage[]{
                        new CollectionCatalogPage(),
                        new AdventureDeckSectionPage(DeckSection.Commander, ItemManagerConfig.ADVENTURE_EDITOR_POOL),
                        new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.ADVENTURE_EDITOR_POOL),
                        new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.ADVENTURE_SIDEBOARD),
                        new CollectionAutoSellPage()
                };
            else {
                return new DeckEditorPage[]{
                        new CollectionCatalogPage(),
                        new AdventureDeckSectionPage(DeckSection.Main, ItemManagerConfig.ADVENTURE_EDITOR_POOL),
                        new AdventureDeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.ADVENTURE_SIDEBOARD),
                        new CollectionAutoSellPage()
                };
            }
        }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            ItemPool<PaperCard> pool = new ItemPool<>(PaperCard.class);
            pool.addAll(Current.player().getCards());
            return pool;
        }

        @Override
        public List<CardEdition> getBasicLandSets(Deck currentDeck) {
            List<CardEdition> unlockedEditions = new ArrayList<>();
            unlockedEditions.add(FModel.getMagicDb().getEditions().get("JMP"));

            // Loop through Landscapes and add them to unlockedEditions
            Map<String, CardEdition> editionsByName = new HashMap<>();
            for (CardEdition e : FModel.getMagicDb().getEditions()) {
                editionsByName.put(e.getName().toLowerCase(), e);
                editionsByName.put(e.getName().replace(":", "").toLowerCase(), e); //TODO: Proper item migration support. This is just there to fix one typo'd item name
                editionsByName.put(e.getName().replace("'", "").toLowerCase(), e);
            }

            String sketchbookPrefix = "landscape sketchbook - ";
            for (ItemData itemData : AdventurePlayer.current().getItems()) {
                if (itemData == null)
                    continue;
                String itemName = itemData.name;
                if (!itemName.toLowerCase().startsWith(sketchbookPrefix)) {
                    continue;
                }

                // Extract the set name after the prefix
                String setName = itemName.substring(sketchbookPrefix.length()).trim();
                CardEdition edition = editionsByName.get(setName.toLowerCase());

                // Add the edition if found and it has basic lands
                if (edition != null && edition.hasBasicLands()) {
                    unlockedEditions.add(edition);
                }
            }
            return unlockedEditions;
        }
    }

    @Override
    public boolean isCommanderEditor() {
        if (isLimitedEditor())
            return false;
        if (AdventurePlayer.current().getAdventureMode() == AdventureModes.Commander)
            return true;
        return super.isCommanderEditor();
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

        @Override
        public boolean usePlayerInventory() {
            return false;
        }

        @Override
        public boolean isLimited() {
            return true;
        }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            return deckToPreview.getAllCardsInASinglePool(true, true);
        }

        @Override
        public boolean allowsCardReplacement() {
            return false;
        }

        @Override
        protected DeckEditorPage[] getInitialPages() {
            return new DeckEditorPage[]{new ContentPreviewPage(deckToPreview)};
        }
    }

    protected static class AdventureEventEditorConfig extends DeckEditorConfig {
        protected AdventureEventData event;
        private final AdventureEventDeckController controller;

        public AdventureEventEditorConfig(AdventureEventData event) {
            this.event = event;
            this.controller = new AdventureEventDeckController(event);
        }

        @Override
        public GameType getGameType() {
            return GameType.AdventureEvent;
        }

        @Override
        public DeckFormat getDeckFormat() {
            return DeckFormat.Limited;
        }

        @Override
        public boolean isLimited() {
            return true;
        }

        @Override
        public boolean isDraft() {
            return event.getDraft() != null;
        }

        @Override
        protected IDeckController getController() {
            return this.controller;
        }

        @Override
        public List<CardEdition> getBasicLandSets(Deck currentDeck) {
            if (event.cardBlock != null) {
                if (event.cardBlock.getLandSet() != null)
                    return List.of(event.cardBlock.getLandSet());
                List<CardEdition> eventSets = new ArrayList<>(event.cardBlock.getSets());
                eventSets.removeIf(Predicate.not(CardEdition::hasBasicLands));
                if (!eventSets.isEmpty())
                    return eventSets;
            }
            return List.of(DeckProxy.getDefaultLandSet(event.registeredDeck));
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
        public ItemPool<PaperCard> getCardPool() {
            return contents.getAllCardsInASinglePool(true, true);
        }

        @Override
        public void refresh() {
            if (contents != null) cardManager.setPool(contents.getAllCardsInASinglePool(true, true));
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
            if (sellable <= 0)
                return;
            String prompt = card + " - " + label + " " + localizer.getMessage("lblHowMany");

            FMenuItem sellItem = new FMenuItem(label, SIDEBOARD_ICON, new MoveQuantityPrompt(prompt, sellable, result -> {
                int sold = Current.player().sellCard(card, result);
                removeCard(card, sold);
            }));
            if (cardIsFavorite(card))
                sellItem.setTextColor(255, 0, 0);
            menu.addItem(sellItem);
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
            cardManager.setPool(getCardPool());
        }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            ItemPool<PaperCard> pool = Current.player().getSellableCards();
            pool.removeAll(Current.player().autoSellCards);
            return pool;
        }

        public void sellAllByFilter() {
            int value = 0;

            CardPool toSell = new CardPool();

            for (Map.Entry<PaperCard, Integer> entry : cardManager.getFilteredItems()) {
                if (cardIsFavorite(entry.getKey()))
                    continue;
                toSell.add(entry.getKey(), entry.getValue());
                value += Current.player().cardSellPrice(entry.getKey()) * entry.getValue();
            }

            if (toSell.isEmpty())
                return;

            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblSellAllConfirm", toSell.countAll(), value), Forge.getLocalizer().getMessage("lblSellCurrentFilters"), Forge.getLocalizer().getMessage("lblSell"), Forge.getLocalizer().getMessage("lblCancel"), false, result -> {
                if (result) {
                    Current.player().doBulkSell(toSell);
                    refresh();
                }
            });
        }

        @Override
        public void setCardFavorited(PaperCard card, boolean isFavorite) {
            AdventurePlayer player = Current.player();
            if (isFavorite)
                player.favoriteCards.add(card);
            else
                player.favoriteCards.remove(card);
        }

        @Override
        protected boolean cardIsFavorite(PaperCard card) {
            return Current.player().favoriteCards.contains(card);
        }

        @Override
        protected boolean allowFavoriteCards() {
            return true;
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
            scheduleRefresh();
        }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            ItemPool<PaperCard> pool = super.getCardPool();
            pool.removeAll(Current.player().autoSellCards);
            return pool;
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            super.buildMenu(menu, card);

            if (!(parentScreen instanceof AdventureDeckEditor adventureEditor) || adventureEditor.getAutoSellPage() == null)
                return;

            Localizer localizer = Forge.getLocalizer();
            String lblHowMany = localizer.getMessage("lblHowMany");

            CollectionAutoSellPage autoSellPage = adventureEditor.getAutoSellPage();
            int amountInCollection = Current.player().getCards().count(card); //Number we have, including ones in auto-sell and ones used in decks
            int copiesUsedInDecks = Current.player().getCopiesUsedInDecks(card); //Number currently in use by this or any other deck.
            int safeToSellCount = amountInCollection - copiesUsedInDecks; //Number we can sell without losing cards from a deck.
            int autoSellCount = Current.player().autoSellCards.count(card); //Number currently in auto-sell.
            int canMoveToAutoSell = safeToSellCount - autoSellCount; //Number that can be moved to auto-sell from here.

            if (card.getRules().isUnsupported()) {
                menu.clearItems();
                FMenuItem removeItem = new FMenuItem(localizer.getMessage("lblRemoveUnsupportedCard"), FSkinImage.HDDELETE, e ->
                        removeCard(card, safeToSellCount));
                menu.addItem(removeItem);
                return;
            }

            if (copiesUsedInDecks > 0) {
                String text = localizer.getMessage("lblCopiesInUse", copiesUsedInDecks);
                FMenuItem usedHint = new FMenuItem(text, FSkinImage.HDLIBRARY, n -> {
                });
                usedHint.setEnabled(false);
                menu.addItem(usedHint);
            }

            if (card.hasNoSellValue()) {
                String prompt = String.format("%s - %s %s", card, localizer.getMessage("lblRemove"), lblHowMany);
                FMenuItem removeItem = new FMenuItem(localizer.getMessage("lblRemove"), FSkinImage.HDDELETE, new MoveQuantityPrompt(prompt, safeToSellCount, amount -> {
                    int sold = Current.player().sellCard(card, amount);
                    removeCard(card, sold);
                }));
                menu.addItem(removeItem);
                return;
            }

            if (canMoveToAutoSell > 0) {
                String action = localizer.getMessage("lblToAutoSell", autoSellCount, safeToSellCount);
                String prompt = String.format("%s - %s %s", card, action, lblHowMany);
                //Auto-sell page adds to and removes from the player's auto-sell pool.
                //The auto-sell pool is part of the overall pool so there's no need to edit anything on our end either.
                FMenuItem moveToAutosell = new FMenuItem(action, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS,
                        new MoveQuantityPrompt(prompt, canMoveToAutoSell, amount -> moveCard(card, autoSellPage, amount)));
                menu.addItem(moveToAutosell);
            }

            if (autoSellCount > 0) {
                String action = localizer.getMessage("lblFromAutoSell", autoSellCount, safeToSellCount);
                String prompt = String.format("%s - %s %s", card, action, lblHowMany);
                FMenuItem moveToCatalog = new FMenuItem(action, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS,
                        new MoveQuantityPrompt(prompt, autoSellCount, amount -> autoSellPage.moveCard(card, this, amount)));
                menu.addItem(moveToCatalog);
            }
        }

        @Override
        public void setCardFavorited(PaperCard card, boolean isFavorite) {
            AdventurePlayer player = Current.player();
            if (isFavorite)
                player.favoriteCards.add(card);
            else
                player.favoriteCards.remove(card);
        }

        @Override
        protected boolean cardIsFavorite(PaperCard card) {
            return Current.player().favoriteCards.contains(card);
        }

        @Override
        protected boolean allowFavoriteCards() {
            return true;
        }

        @Override
        public void buildDeckMenu(FPopupMenu menu) {
            super.buildDeckMenu(menu);
            if (!(parentScreen instanceof AdventureDeckEditor adventureEditor) || adventureEditor.getAutoSellPage() == null)
                return;
            menu.addItem(new FMenuItem(Forge.getLocalizer().getMessage("btnCopyCollectionToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, e1 -> FDeckViewer.copyCollectionToClipboard(AdventurePlayer.current().getCards())));
            FMenuItem sellCurrentFilters = new FMenuItem(Forge.getLocalizer().getMessage("lblAutoSellCurrentFilters"), FSkinImage.QUEST_COINSTACK, e1 -> autoSellAllByFilter(adventureEditor.getAutoSellPage()));
            sellCurrentFilters.setTextColor(255, 0, 0);
            menu.addItem(sellCurrentFilters);
        }

        private void autoSellAllByFilter(CollectionAutoSellPage autoSellPage) {
            CardPool toMove = new CardPool();

            for (Map.Entry<PaperCard, Integer> entry : cardManager.getFilteredItems()) {
                if (cardIsFavorite(entry.getKey()))
                    continue;
                toMove.add(entry.getKey(), entry.getValue());
            }
            if (toMove.isEmpty())
                return;

            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblAutoSellCurrentFiltersConfirm", toMove.countAll()), Forge.getLocalizer().getMessage("lblAutoSellCurrentFilters"), Forge.getLocalizer().getMessage("lblAutoSell"), Forge.getLocalizer().getMessage("lblCancel"), false, result -> {
                if (result) {
                    moveCards(toMove, autoSellPage);
                }
            });
        }
    }

    protected static class CollectionAutoSellPage extends CatalogPage {
        private final String captionPrefix;

        protected CollectionAutoSellPage() {
            super(new AdventureCardManager(), ItemManagerConfig.ADVENTURE_EDITOR_POOL, Forge.getLocalizer().getMessage("lblAutoSell"), AUTO_SELL_ICON);
            this.captionPrefix = Forge.getLocalizer().getMessage("lblAutoSell");
        }

        @Override
        protected void updateCaption() {
            caption = captionPrefix + " (" + cardManager.getPool().countAll() + ")";
        }

        @Override
        protected void initialize() {
            super.initialize();
            cardManager.setBtnAdvancedSearchOptions(true);
            cardManager.setPool(getCardPool(), false); //Need to update this early for the caption.
            this.updateCaption();
        }

        @Override
        public ItemPool<PaperCard> getCardPool() {
            //No need to override addCard and removeCard, because autoSellCards IS the card pool here.
            //It'll be updated automatically as cards are added and removed from this page.
            return Current.player().getAutoSellCards();
        }

        @Override
        public void refresh() {
            super.refresh();
            //Used when executing an auto-sell.
            this.updateCaption();
        }

        protected boolean isShop() {
            return parentScreen.getEditorConfig() instanceof ShopConfig;
        }

        @Override
        protected void buildMenu(FDropDownMenu menu, PaperCard card) {
            super.buildMenu(menu, card);
            Localizer localizer = Forge.getLocalizer();
            AdventurePlayer player = Current.player();
            if (isShop()) {
                String label = localizer.getMessage("lblSellFor") + " " + player.cardSellPrice(card);
                int sellable = cardManager.getItemCount(card);
                if (sellable <= 0)
                    return;
                String prompt = card + " - " + label + " " + localizer.getMessage("lblHowMany");

                menu.addItem(new FMenuItem(label, SIDEBOARD_ICON, new MoveQuantityPrompt(prompt, sellable, result -> {
                    int sold = player.sellCard(card, result);
                    removeCard(card, sold);
                })
                ));
            }
            if (parentScreen instanceof AdventureDeckEditor adventureEditor && adventureEditor.getCatalogPage() != null) {
                CatalogPage catalogPage = adventureEditor.getCatalogPage();
                int autoSellCount = cardManager.getItemCount(card);
                int amountInCollection = player.getCards().count(card);
                int safeToSellCount = amountInCollection - player.getCopiesUsedInDecks(card);

                String action = localizer.getMessage("lblFromAutoSell", autoSellCount, safeToSellCount);
                String prompt = String.format("%s - %s %s", card, action, localizer.getMessage("lblHowMany"));
                FMenuItem moveToCatalog = new FMenuItem(action, CATALOG_ICON, new MoveQuantityPrompt(prompt, autoSellCount, amount -> moveCard(card, catalogPage, amount)));
                menu.addItem(moveToCatalog);
            }

        }

        @Override
        protected void onCardActivated(PaperCard card) {
            if (isShop()) {
                Current.player().sellCard(card, 1);
                removeCard(card, 1);
            }
            //Move to deck? Back to catalog? Unclear.
        }
    }

    public AdventureEventData getCurrentEvent() {
        IDeckController controller = getDeckController();
        if (!(controller instanceof AdventureEventDeckController eventController))
            return null;
        return eventController.currentEvent;
    }

    @Override
    public BoosterDraft getDraft() {
        AdventureEventData currentEvent = getCurrentEvent();
        if (currentEvent == null)
            return null;
        return currentEvent.getDraft();
    }

    @Override
    public boolean isDrafting() {
        AdventureEventData currentEvent = getCurrentEvent();
        if (currentEvent == null)
            return false;
        return currentEvent.draft != null && !currentEvent.isDraftComplete;
    }

    public static AdventureEventData currentEvent; //TODO: Remove. Should just get this from the controller.

    public void setEvent(AdventureEventData event) {
        currentEvent = event;
    }

    @Override
    public void completeDraft() {
        super.completeDraft();
        AdventureEventData currentEvent = getCurrentEvent();
        if (currentEvent == null)
            return;
        currentEvent.isDraftComplete = true;
        Deck[] opponentDecks = currentEvent.getDraft().getComputerDecks();
        for (int i = 0; i < currentEvent.participants.length && i < opponentDecks.length; i++) {
            currentEvent.participants[i].setDeck(opponentDecks[i]);
        }
        currentEvent.draftedDeck = (Deck) currentEvent.registeredDeck.copyTo("Draft Deck");
        if (allowAddBasic()) {
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
        if (deckSection == DeckSection.Main)
            return MAIN_DECK_ICON;
        if (deckSection == DeckSection.Sideboard)
            return FDeckEditor.SIDEBOARD_ICON;
        return FDeckEditor.iconFromDeckSection(deckSection);
    }

    private static ItemPool<InventoryItem> decksUsingMyCards = new ItemPool<>(InventoryItem.class);

    @Override
    public void onActivate() {
        decksUsingMyCards = new ItemPool<>(InventoryItem.class);
        for (int i = 0; i < Current.player().getDeckCount(); i++) {
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

        for (TabPage<FDeckEditor> page : tabPages) {
            if (page instanceof CollectionCatalogPage) {
                if (!Current.player().getUnsupportedCards().isEmpty())
                    GuiChoose.getChoices(Forge.getLocalizer().getMessage("lblRemoveAllUnsupportedCards"),
                            -1, -1, Current.player().getUnsupportedCards(), result -> Current.player().getUnsupportedCards().clear());
                break;
            }
        }
    }

    public void refresh() {
        FThreads.invokeInBackgroundThread(() -> {
            for (TabPage<FDeckEditor> page : tabPages) {
                if (page instanceof CollectionAutoSellPage p)
                    p.refresh();
                else if (page instanceof CatalogPage p)
                    p.scheduleRefresh();
                else if (page instanceof CardManagerPage p)
                    p.refresh();
            }
        });
    }


    protected AdventureDeckHeader deckHeader;
    protected FDraftLog draftLog;
    protected CollectionAutoSellPage autoSellPage;

    public AdventureDeckEditor(boolean createAsShop) {
        super(createAsShop ? new ShopConfig() : new AdventureEditorConfig(),
                createAsShop ? null : Current.player().getSelectedDeck());
        if (createAsShop)
            setHeaderText(Forge.getLocalizer().getMessage("lblSell"));
    }

    public AdventureDeckEditor(AdventureEventData event) {
        super(new AdventureEventEditorConfig(event), event.registeredDeck);
        currentEvent = event;

        if (event.getDraft() != null && event.getDraft().shouldShowDraftLog()) {
            this.draftLog = new FDraftLog();
            event.getDraft().setLogEntry(this.draftLog);
            deckHeader.initDraftLog(this.draftLog, this);
        }
    }

    public AdventureDeckEditor(Deck deckToPreview) {
        super(new DeckPreviewConfig(deckToPreview), deckToPreview);
    }

    @Override
    protected DeckHeader initDeckHeader() {
        this.deckHeader = add(new AdventureDeckHeader());
        return this.deckHeader;
    }

    @Override
    protected void addChosenBasicLands(CardPool landsToAdd) {
        if (isLimitedEditor())
            super.addChosenBasicLands(landsToAdd);

        //Take the basic lands from the player's collection if they have them. If they need more, create unsellable copies.
        CatalogPage catalog = getCatalogPage();
        if (catalog != null) { // TODO find out why this is null on some devices since it shouldn't be null
            CardPool requiredNewLands = new CardPool();
            CardPool landsToMove = new CardPool();
            ItemPool<PaperCard> availablePool = catalog.getCardPool();
            for (Map.Entry<PaperCard, Integer> entry : landsToAdd) {
                int needed = entry.getValue();
                PaperCard card = entry.getKey();
                int moveableSellable = Math.min(availablePool.count(card), needed);
                landsToMove.add(card, moveableSellable);
                needed -= moveableSellable;
                if (needed <= 0)
                    continue;
                PaperCard unsellable = card.getNoSellVersion();
                //It'd probably be better to do some kind of fuzzy search that matches prints but ignores flags.
                //But for now, unsellable is the only one that should matter here.
                int moveableUnsellable = Math.min(availablePool.count(unsellable), needed);
                landsToMove.add(unsellable, needed); //We'll acquire the rest later.
                if (needed > moveableUnsellable)
                    requiredNewLands.add(unsellable, needed - moveableUnsellable);
            }
            if (!requiredNewLands.isEmpty())
                Current.player().addCards(requiredNewLands);
            catalog.refresh();
            catalog.moveCards(landsToMove, getMainDeckPage());
        }
    }

    @Override
    protected PaperCard supplyPrintForImporter(PaperCard missingCard) {
        PaperCard out = super.supplyPrintForImporter(missingCard);
        return out == null ? null : out.getNoSellVersion();
    }

    @Override
    protected void cacheTabPages() {
        super.cacheTabPages();
        for (TabPage<FDeckEditor> page : tabPages) {
            if (page instanceof CollectionAutoSellPage)
                this.autoSellPage = (CollectionAutoSellPage) page;
        }
    }

    @Override
    protected boolean allowAddBasic() {
        if (getEditorConfig() instanceof DeckPreviewConfig)
            return false;
        AdventureEventData currentEvent = getCurrentEvent();
        if (currentEvent == null)
            return true;
        if (!currentEvent.eventRules.allowsAddBasicLands)
            return false;
        if (isDrafting())
            return false;
        if (currentEvent.eventStatus == AdventureEventController.EventStatus.Entered
                || currentEvent.eventStatus == AdventureEventController.EventStatus.Ready
                || currentEvent.eventStatus == AdventureEventController.EventStatus.Started)
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
        AdventureEventData currentEvent = getCurrentEvent();
        if (currentEvent == null)
            return Current.player().getSelectedDeck();
        else
            return currentEvent.registeredDeck;
    }

    private CollectionAutoSellPage getAutoSellPage() {
        return autoSellPage;
    }

    @Override
    public void onClose(final Consumer<Boolean> canCloseCallback) {
        if (canCloseCallback == null) {
            resolveClose(null, true);
            return;
        }

        Localizer localizer = Forge.getLocalizer();
        if (isDrafting()) {
            FOptionPane.showConfirmDialog(localizer.getMessage("lblEndAdventureEventConfirm"), localizer.getMessage("lblLeaveDraft"), localizer.getMessage("lblLeave"), localizer.getMessage("lblCancel"), false, result -> resolveClose(canCloseCallback, result == true));
            return;
        } else if (getEditorConfig().isLimited() || getDeck().isEmpty()) {
            resolveClose(canCloseCallback, true);
            return;
        }

        String deckError;
        if (!(getEditorConfig() instanceof ShopConfig))
        {
            deckError = getEditorConfig().getDeckFormat().getDeckConformanceProblem(getDeck());

            if (deckError != null) {
                //Allow the player to close the editor with an invalid deck, but warn them that cards may be swapped out.
                String warning = localizer.getMessage("lblAdventureDeckError", deckError);
                FOptionPane.showConfirmDialog(warning, localizer.getMessage("lblInvalidDeck"), false, result -> resolveClose(canCloseCallback, result == true));
                return;
            }
        }

        resolveClose(canCloseCallback, true);
    }

    private void resolveClose(final Consumer<Boolean> canCloseCallback, boolean result) {
        if (result) {
            Current.player().newCards.clear();
            if (isDrafting())
                getCurrentEvent().eventStatus = AdventureEventController.EventStatus.Abandoned;
        }
        if (canCloseCallback != null)
            canCloseCallback.accept(result);
    }

    @Override
    protected void devAddCards(CardPool cards) {
        if (!getEditorConfig().usePlayerInventory()) {
            //Drafting.
            super.devAddCards(cards);
            return;
        }
        Current.player().addCards(cards);
        getCatalogPage().scheduleRefresh();
    }

    protected static class AdventureCardManager extends CardManager {

        public AdventureCardManager() {
            super(false);
        }

        @Override
        protected void addDefaultFilters() {
            this.addFilter(new CardColorFilter(this));
            this.addFilter(new CardTypeFilter(this));
        }

        @Override
        protected String getItemSuffix(Map.Entry<PaperCard, Integer> item) {
            PaperCard card = item.getKey();
            String parentSuffix = super.getItemSuffix(item);
            if (card.hasNoSellValue()) {
                String valueText = " [NO VALUE]";
                if (parentSuffix == null)
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

                    if (showPriceInfo()) {
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
            if (editor != null)
                editor.notifyNewControllerModel();
        }

        @Override
        public void setDeck(Deck deck) {
            this.currentDeck = deck;
            if (editor != null)
                editor.notifyNewControllerModel();
        }

        @Override
        public Deck getDeck() {
            return currentDeck;
        }

        @Override
        public void newDeck() {
            setDeck(new Deck("Adventure Deck"));
        }

        @Override
        public String getDeckDisplayName() {
            if (currentDeck == null)
                return "New Deck";
            return currentDeck.getName();
        }

        @Override
        public void notifyModelChanged() {
        } //

        @Override
        public void exitWithoutSaving() {
        } //Too many external variables to just revert the deck. Not supported for now.
    }

    private static class AdventureEventDeckController implements IDeckController {
        FDeckEditor editor;
        AdventureEventData currentEvent;

        public AdventureEventDeckController(AdventureEventData currentEvent) {
            this.currentEvent = currentEvent;
        }

        @Override
        public void setEditor(FDeckEditor editor) {
            this.editor = editor;
            if (editor != null)
                editor.notifyNewControllerModel();
        }

        @Override
        public void setDeck(Deck deck) {
            this.newDeck();
        } //Deck is supplied by the event.

        @Override
        public void newDeck() {
            if (editor != null)
                editor.notifyNewControllerModel();
        }

        @Override
        public Deck getDeck() {
            return currentEvent.registeredDeck;
        }

        @Override
        public String getDeckDisplayName() {
            if (getDeck() == null)
                return "Uninitialized Deck";
            return getDeck().getName();
        }

        @Override
        public void notifyModelChanged() {
        } //

        @Override
        public void exitWithoutSaving() {
        } //Too many external variables to just revert the deck. Not supported for now.
    }

}


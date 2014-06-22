package forge.deck;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;
import com.google.common.base.Supplier;

import forge.Forge;
import forge.Graphics;
import forge.StaticData;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.card.CardEdition;
import forge.card.CardZoom;
import forge.deck.io.DeckPreferences;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.TabPageScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.Utils;
import forge.util.storage.IStorage;

public class FDeckEditor extends TabPageScreen<FDeckEditor> {
    private static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);

    public enum EditorType {
        Constructed(new DeckController<Deck>(FModel.getDecks().getConstructed(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Draft(new DeckController<DeckGroup>(FModel.getDecks().getDraft(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Sealed(new DeckController<DeckGroup>(FModel.getDecks().getSealed(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Winston(new DeckController<DeckGroup>(FModel.getDecks().getWinston(), new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        })),
        Commander(new DeckController<Deck>(FModel.getDecks().getCommander(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Archenemy(new DeckController<Deck>(FModel.getDecks().getScheme(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Planechase(new DeckController<Deck>(FModel.getDecks().getPlane(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        })),
        Vanguard(new DeckController<Deck>(FModel.getDecks().getConstructed(), new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        }));

        private final DeckController<? extends DeckBase> controller;

        public DeckController<? extends DeckBase> getController() {
            return controller;
        }

        private EditorType(DeckController<? extends DeckBase> controller0) {
            controller = controller0;
        }
    }

    private static DeckEditorPage[] getPages(EditorType editorType) {
        switch (editorType) {
        default:
        case Constructed:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard)
            };
        case Draft:
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
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Commander)
            };
        case Archenemy:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Schemes)
            };
        case Planechase:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Planes)
            };
        case Vanguard:
            return new DeckEditorPage[] {
                    new CatalogPage(),
                    new DeckSectionPage(DeckSection.Main),
                    new DeckSectionPage(DeckSection.Sideboard),
                    new DeckSectionPage(DeckSection.Avatar)
            };
        }
    }

    private final EditorType editorType;
    private Deck deck;
    private CatalogPage catalogPage;
    private DeckSectionPage mainDeckPage;
    private DeckSectionPage sideboardPage;
    private FEventHandler saveHandler;

    protected final DeckHeader deckHeader = add(new DeckHeader());
    protected final FLabel lblName = deckHeader.add(new FLabel.Builder().font(FSkinFont.get(16)).insets(new Vector2(Utils.scaleX(5), 0)).build());
    private final FLabel btnSave = deckHeader.add(new FLabel.Builder().icon(FSkinImage.SAVE).align(HAlignment.CENTER).pressedColor(Header.BTN_PRESSED_COLOR).build());
    private final FLabel btnMoreOptions = deckHeader.add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(HAlignment.CENTER).pressedColor(Header.BTN_PRESSED_COLOR).build());

    public FDeckEditor(EditorType editorType0, DeckProxy editDeck, boolean showMainDeck) {
        this(editorType0, editDeck.getName(), editDeck.getPath(), null, showMainDeck);
    }
    public FDeckEditor(EditorType editorType0, String editDeckName, boolean showMainDeck) {
        this(editorType0, editDeckName, "", null, showMainDeck);
    }
    public FDeckEditor(EditorType editorType0, Deck newDeck, boolean showMainDeck) {
        this(editorType0, "", "", newDeck, showMainDeck);
    }
    private FDeckEditor(EditorType editorType0, String editDeckName, String editDeckPath, Deck newDeck, boolean showMainDeck) {
        super(getPages(editorType0));

        editorType = editorType0;
        editorType.getController().editor = this;

        //cache specific pages
        for (TabPage<FDeckEditor> tabPage : tabPages) {
            if (tabPage instanceof CatalogPage) {
                catalogPage = (CatalogPage) tabPage;
            }
            else if (tabPage instanceof DeckSectionPage) {
                DeckSectionPage deckSectionPage = (DeckSectionPage) tabPage;
                if (deckSectionPage.deckSection == DeckSection.Main) {
                    mainDeckPage = deckSectionPage;
                }
                else if (deckSectionPage.deckSection == DeckSection.Sideboard) {
                    sideboardPage = deckSectionPage;
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
            break;
        default:
            //if editing existing non-limited deck, show main deck by default
            if (showMainDeck) {
                setSelectedPage(mainDeckPage);
            }
            break;
        }

        if (StringUtils.isEmpty(editDeckName)) {
            if (editorType == EditorType.Draft) {
                //hide deck header on while drafting
                setDeck(new Deck());
                deckHeader.setVisible(false);
            }
            else {
                if (newDeck == null) {
                    editorType.getController().newModel();
                }
                else {
                    editorType.getController().setDeck(newDeck);
                }
            }
        }
        else {
            if (editorType == EditorType.Draft) {
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
                        addItem(new FMenuItem("Add Basic Lands", FSkinImage.LAND, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                CardEdition defaultLandSet;
                                switch (editorType) {
                                case Draft:
                                case Sealed:
                                    //use most recent edition that all cards in limited pool came before or in 
                                    defaultLandSet = StaticData.instance().getEditions().getEarliestEditionWithAllCards(deck.getAllCardsInASinglePool());
                                    break;
                                default:
                                    //TODO: Support loading/saving default land set for non-limited decks
                                    defaultLandSet = StaticData.instance().getEditions().get("ZEN");
                                    break;
                                }
                                AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck, defaultLandSet, new Callback<CardPool>() {
                                    @Override
                                    public void run(CardPool landsToAdd) {
                                        getMainDeckPage().addCards(landsToAdd);
                                    }
                                });
                                dialog.show();
                                setSelectedPage(getMainDeckPage()); //select main deck page if needed so main deck if visible below dialog
                            }
                        }));
                        addItem(new FMenuItem("Rename Deck", FSkinImage.EDIT, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                FOptionPane.showInputDialog("Enter new name for deck:", "Rename Deck", null, deck.getName(), new Callback<String>() {
                                    @Override
                                    public void run(String result) {
                                        editorType.getController().rename(result);
                                    }
                                });
                            }
                        }));
                        addItem(new FMenuItem("Delete Deck", FSkinImage.DELETE, new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                FOptionPane.showConfirmDialog(
                                        "Are you sure you want to delete '" + deck.getName() + "'?",
                                        "Delete Deck", "Delete", "Cancel", false, new Callback<Boolean>() {
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
                };
                menu.show(btnMoreOptions, 0, btnMoreOptions.getHeight());
            }
        });
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
        case Vanguard:
        case Archenemy:
        default:
            if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
                return CardLimit.Default;
            }
            return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
        case Draft:
        case Sealed:
        case Winston:
            return CardLimit.None;
        case Commander:
            return CardLimit.Singleton;
        }
    }

    public void setSaveHandler(FEventHandler saveHandler0) {
        saveHandler = saveHandler0;
    }

    protected void save(final Callback<Boolean> callback) {
        if (StringUtils.isEmpty(deck.getName())) {
            FOptionPane.showInputDialog("Enter name for new deck:", "New Deck", new Callback<String>() {
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

    @Override
    public void onClose(final Callback<Boolean> canCloseCallback) {
        if (editorType.getController().isSaved() || canCloseCallback == null) {
            super.onClose(canCloseCallback); //can skip prompt if draft saved
            return;
        }
        FOptionPane.showOptionDialog("Save changes to current deck?", "Save Changes?",
                FOptionPane.QUESTION_ICON, new String[] {"Save", "Don't Save", "Cancel"}, new Callback<Integer>() {
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

    private boolean isLimitedEditor() {
        switch (editorType) {
        case Draft:
        case Sealed:
        case Winston:
            return true;
        default:
            return false;
        }
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

        protected abstract void initialize();
    }

    protected static abstract class CardManagerPage extends DeckEditorPage {
        protected final CardManager cardManager = add(new CardManager(false));

        protected CardManagerPage(ItemManagerConfig config, String caption0, FImage icon0) {
            super(caption0, icon0);
            cardManager.setup(config);
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
                    menu.addItem(new FMenuItem("Zoom/Details", new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            CardZoom.show(card);
                        }
                    }));
                }
            });
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

        protected void updateCaption() {
        }

        protected abstract void onCardActivated(PaperCard card);
        protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

        private static final List<String> limitExceptions = Arrays.asList(
                new String[]{"Relentless Rats", "Shadowborn Apostle"});

        private ItemPool<PaperCard> getAllowedAdditions(Iterable<Entry<PaperCard, Integer>> itemsToAdd, boolean isAddSource) {
            ItemPool<PaperCard> additions = new ItemPool<PaperCard>(cardManager.getGenericType());
            CardLimit limit = parentScreen.getCardLimit();
            Deck deck = parentScreen.getDeck();

            for (Entry<PaperCard, Integer> itemEntry : itemsToAdd) {
                PaperCard item = itemEntry.getKey();
                PaperCard card = item instanceof PaperCard ? (PaperCard)item : null;
                int qty = itemEntry.getValue();

                int max;
                if (deck == null || card == null || card.getRules().getType().isBasic() ||
                        limit == CardLimit.None || limitExceptions.contains(card.getName())) {
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
                    max -= deck.getMain().count(card);
                    if (deck.has(DeckSection.Sideboard)) {
                        max -= deck.get(DeckSection.Sideboard).count(card);
                    }
                    if (deck.has(DeckSection.Commander)) {
                        max -= deck.get(DeckSection.Commander).count(card);
                    }
                }
                if (isAddSource) {
                    if (qty > max) {
                        qty = max;
                    }
                    if (qty > 0) {
                        additions.add(item, qty);
                    }
                }
                else { //if not source of items being added, use max directly
                    additions.add(item, max);
                }
            }

            return additions;
        }

        private int getMaxMoveQuantity(boolean isAddMenu, boolean isAddSource) {
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
                    }
                    else {
                        GuiChoose.getInteger(cardManager.getSelectedItem() + " - " + verb + " how many?", 1, max, 20, callback);
                    }
                }
            }));
        }

        @Override
        protected void doLayout(float width, float height) {
            cardManager.setBounds(0, 0, width, height);
        }
    }

    protected static class CatalogPage extends CardManagerPage {
        private boolean initialized, needRefreshWhenShown;

        protected CatalogPage() {
            this(ItemManagerConfig.CARD_CATALOG, "Catalog", FSkinImage.FOLDER);
        }
        protected CatalogPage(ItemManagerConfig config, String caption0, FImage icon0) {
            super(config, caption0, icon0);
        }

        @Override
        protected void initialize() {
            if (initialized) { return; } //prevent initializing more than once if deck changes
            initialized = true;

            cardManager.setCaption(getItemManagerCaption());
            refresh();
        }

        @Override
        protected boolean canAddCards() {
            return false;
        }

        protected String getItemManagerCaption() {
            return "Cards";
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

        public void refresh() {
            if (!isVisible()) {
                needRefreshWhenShown = true;
                return; //delay refreshing while hidden
            }
            cardManager.setPool(ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class), true);
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            parentScreen.getMainDeckPage().addCard(card);
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            addItem(menu, "Add", "to main deck", parentScreen.getMainDeckPage().getIcon(), true, true, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    if (result == null || result <= 0) { return; }

                    parentScreen.getMainDeckPage().addCard(card, result);
                }
            });
            addItem(menu, "Add", "to sideboard", parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    if (result == null || result <= 0) { return; }

                    parentScreen.getSideboardPage().addCard(card, result);
                }
            });
        }
    }

    protected static class DeckSectionPage extends CardManagerPage {
        private final String captionPrefix;
        private final DeckSection deckSection;

        protected DeckSectionPage(DeckSection deckSection0) {
            this(deckSection0, ItemManagerConfig.DECK_EDITOR);
        }
        protected DeckSectionPage(DeckSection deckSection0, ItemManagerConfig config) {
            super(config, null, null);

            deckSection = deckSection0;
            switch (deckSection) {
            default:
            case Main:
                captionPrefix = "Main";
                cardManager.setCaption("Main Deck");
                icon = FSkinImage.DECKLIST;
                break;
            case Sideboard:
                captionPrefix = "Side";
                cardManager.setCaption("Sideboard");
                icon = FSkinImage.FLASHBACK;
                break;
            case Commander:
                captionPrefix = "Commander";
                cardManager.setCaption("Commander");
                icon = FSkinImage.PLANESWALKER;
                break;
            case Avatar:
                captionPrefix = "Avatar";
                cardManager.setCaption("Avatar");
                icon = new FTextureRegionImage(FSkin.getAvatars().get(0));
                break;
            case Planes:
                captionPrefix = "Planes";
                cardManager.setCaption("Planes");
                icon = FSkinImage.CHAOS;
                break;
            case Schemes:
                captionPrefix = "Schemes";
                cardManager.setCaption("Schemes");
                icon = FSkinImage.POISON;
                break;
            }
        }

        @Override
        protected void initialize() {
            cardManager.setPool(parentScreen.getDeck().getOrCreate(deckSection));
            updateCaption();
        }

        @Override
        protected void updateCaption() {
            caption = captionPrefix + " (" + parentScreen.getDeck().get(deckSection).countAll() + ")";
        }

        @Override
        protected void onCardActivated(PaperCard card) {
            switch (deckSection) {
            case Main:
                removeCard(card);
                switch (parentScreen.getEditorType()) {
                case Draft:
                case Sealed:
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
            switch (deckSection) {
            case Main:
                addItem(menu, "Add", null, FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
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
                addItem(menu, "Move", "to sideboard", parentScreen.getSideboardPage().getIcon(), false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        parentScreen.getSideboardPage().addCard(card, result);
                    }
                });
                break;
            case Sideboard:
                addItem(menu, "Add", null, FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                if (!parentScreen.isLimitedEditor()) {
                    addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
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
                addItem(menu, "Move", "to main deck", parentScreen.getMainDeckPage().getIcon(), false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        parentScreen.getMainDeckPage().addCard(card, result);
                    }
                });
                break;
            case Commander:
                addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Avatar:
                addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Schemes:
                addItem(menu, "Add", null, FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            case Planes:
                addItem(menu, "Add", null, FSkinImage.PLUS, true, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        addCard(card, result);
                    }
                });
                addItem(menu, "Remove", null, FSkinImage.MINUS, false, false, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                    }
                });
                break;
            }
        }
    }

    private static class DraftPackPage extends CatalogPage {
        protected DraftPackPage() {
            super(ItemManagerConfig.DRAFT_PACK, "Pack 1", FSkinImage.PACK);

            //hide filters and options panel so more of pack is visible by default
            cardManager.setHideViewOptions(1, true);
            cardManager.setAlwaysNonUnique(true);
        }

        @Override
        public void refresh() {
            BoosterDraft draft = parentScreen.getDraft();
            if (draft == null) { return; }

            CardPool pool = draft.nextChoice();
            int packNumber = draft.getCurrentBoosterIndex() + 1;
            caption = "Pack " + packNumber;
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

            if (draft.hasNextChoice()) {
                refresh();
            }
            else {
                hideTab(); //hide this tab page when finished drafting
                draft.finishedDrafting();
                parentScreen.save(null);
            }
        }

        @Override
        protected void buildMenu(final FDropDownMenu menu, final PaperCard card) {
            addItem(menu, "Add", "to main deck", parentScreen.getMainDeckPage().getIcon(), true, true, new Callback<Integer>() {
                @Override
                public void run(Integer result) { //ignore quantity
                    parentScreen.getMainDeckPage().addCard(card);
                    afterCardPicked(card);
                }
            });
            addItem(menu, "Add", "to sideboard", parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
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
        private final IStorage<T> rootFolder;
        private IStorage<T> currentFolder;
        private String modelPath;
        private FDeckEditor editor;
        private final Supplier<T> newModelCreator;

        protected DeckController(final IStorage<T> folder0, final Supplier<T> newModelCreator0) {
            rootFolder = folder0;
            currentFolder = rootFolder;
            model = null;
            saved = true;
            modelInStorage = false;
            modelPath = "";
            newModelCreator = newModelCreator0;
        }

        public Deck getDeck() {
            if (model == null) { return null; }

            if (model instanceof Deck) {
                return (Deck) model;
            }
            return ((DeckGroup) model).getHumanDeck();
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
                }
                else {
                    notifyModelChanged();
                }
            }
            else { //TODO: Make this smarter
                currentFolder = rootFolder;
                modelPath = "";
                setSaved(true);
            }
            editor.setDeck(getDeck());
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
                    name = "[New Deck]";
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
            modelInStorage = true;
            setSaved(true);

            //update saved deck names
            String deckStr = DeckProxy.getDeckString(getModelPath(), getModelName());
            switch (editor.getEditorType()) {
            case Constructed:
                DeckPreferences.setCurrentDeck(deckStr);
                break;
            case Draft:
                DeckPreferences.setDraftDeck(deckStr);
                break;
            case Sealed:
                DeckPreferences.setSealedDeck(deckStr);
                break;
            default:
                break;
            }
            editor.setDeck(getDeck());
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

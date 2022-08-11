package forge.adventure.scene;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Function;
import forge.Forge;
import forge.Graphics;
import forge.adventure.player.AdventurePlayer;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.*;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.*;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.properties.ForgePreferences;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.GuiChoose;
import forge.util.Callback;
import forge.util.ItemPool;
import forge.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
 
    public class AdventureDeckEditor extends TabPageScreen<AdventureDeckEditor> {
        public static FSkinImage MAIN_DECK_ICON = Forge.hdbuttons ? FSkinImage.HDLIBRARY :FSkinImage.DECKLIST;
        public static FSkinImage SIDEBOARD_ICON = Forge.hdbuttons ? FSkinImage.HDSIDEBOARD : FSkinImage.FLASHBACK;
        private static final float HEADER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.8f);
        private static final FLabel lblGold = new FLabel.Builder().text("0").icon(FSkinImage.QUEST_COINSTACK).font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build();

        private static ItemPool<InventoryItem> decksUsingMyCards=new ItemPool<>(InventoryItem.class);
        public static void leave() {
            AdventurePlayer.current().getNewCards().clear();
            Forge.clearCurrentScreen();
            Forge.switchToLast();
        }

        @Override
        public void onActivate() {
            decksUsingMyCards = new ItemPool<>(InventoryItem.class);
            for (int i=0;i<AdventurePlayer.NUMBER_OF_DECKS;i++)
            {
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
            lblGold.setText(String.valueOf(AdventurePlayer.current().getGold()));
        }
        public void refresh() {
            for(TabPage<AdventureDeckEditor> page:tabPages)
            {
                if(page instanceof CardManagerPage)
                    ((CardManagerPage)page).refresh();
            }
            for (TabPage<AdventureDeckEditor> tabPage : tabPages) {
                ((DeckEditorPage)tabPage).initialize();
            }
        }
        private static DeckEditorPage[] getPages() {
            return new DeckEditorPage[] {
                    new CatalogPage(ItemManagerConfig.QUEST_EDITOR_POOL, Forge.getLocalizer().getMessage("lblInventory"), FSkinImage.QUEST_BOX),
                    new DeckSectionPage(DeckSection.Main, ItemManagerConfig.QUEST_DECK_EDITOR),
                    new DeckSectionPage(DeckSection.Sideboard, ItemManagerConfig.QUEST_DECK_EDITOR)
            };
        }
        private CatalogPage catalogPage;
        private DeckSectionPage mainDeckPage;
        private DeckSectionPage sideboardPage;
        private DeckSectionPage commanderPage;

        protected final DeckHeader deckHeader = add(new DeckHeader());
        protected final FLabel lblName = deckHeader.add(new FLabel.Builder().font(FSkinFont.get(16)).insets(new Vector2(Utils.scale(5), 0)).build());
        private final FLabel btnMoreOptions = deckHeader.add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(Align.center).pressedColor(Header.BTN_PRESSED_COLOR).build());


        boolean isShop=false;
        public AdventureDeckEditor(boolean createAsShop) {
            super(new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    leave();
                }
            },getPages());

            isShop=createAsShop;

            //cache specific pages
            for (TabPage<AdventureDeckEditor> tabPage : tabPages) {
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

            btnMoreOptions.setCommand(new FEvent.FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    FPopupMenu menu = new FPopupMenu() {
                        @Override
                        protected void buildMenu() {
                            addItem(new FMenuItem(Forge.getLocalizer().getMessage("btnCopyToClipboard"), Forge.hdbuttons ? FSkinImage.HDEXPORT : FSkinImage.BLANK, new FEvent.FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e1) {
                                    FDeckViewer.copyDeckToClipboard(getDeck());
                                }
                            }));
                            ((DeckEditorPage)getSelectedPage()).buildDeckMenu(this);
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
        public Deck getDeck() {
            return AdventurePlayer.current().getSelectedDeck();
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

        @Override
        public void onClose(final Callback<Boolean> canCloseCallback) {

        }

        @Override
        public FScreen getLandscapeBackdropScreen() {
            return null; //never use backdrop for editor
        }

        protected class DeckHeader extends FContainer {
            private DeckHeader() {
                setHeight(HEADER_HEIGHT);
            }
            boolean init;

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
                //noinspection SuspiciousNameCombination
                x += height;
                //noinspection SuspiciousNameCombination
                btnMoreOptions.setBounds(x, 0, height, height);
                if (!init) {
                    add(lblGold);
                    init = true;
                }
                lblGold.setBounds(0, 0, width, height);
            }
        }

        protected static abstract class DeckEditorPage extends TabPage<AdventureDeckEditor> {
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
                cardManager.setItemActivateHandler(new FEvent.FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        CardManagerPage.this.onCardActivated(cardManager.getSelectedItem());
                    }
                });
                cardManager.setContextMenuBuilder(new ItemManager.ContextMenuBuilder<PaperCard>() {
                    @Override
                    public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                        CardManagerPage.this.buildMenu(menu, card);
                    }
                });
            }
            private final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare = new Function<Map.Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(Map.Entry<InventoryItem, Integer> from) {
                    return AdventurePlayer.current().getNewCards().contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
                }
            };
            private final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnNewGet = new Function<Map.Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(Map.Entry<? extends InventoryItem, Integer> from) {
                    return AdventurePlayer.current().getNewCards().contains(from.getKey()) ? "NEW" : "";
                }
            };
            public static final Function<Map.Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Map.Entry<InventoryItem, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(Map.Entry<InventoryItem, Integer> from) {
                    return decksUsingMyCards.count(from.getKey());
                }
            };
            public static final Function<Map.Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = new Function<Map.Entry<? extends InventoryItem, Integer>, Object>() {
                @Override
                public Object apply(Map.Entry<? extends InventoryItem, Integer> from) {
                    return Integer.valueOf(decksUsingMyCards.count(from.getKey())).toString();
                }
            };

            protected void initialize() {

                Map<ColumnDef, ItemColumn> colOverrides = new HashMap<>();
                ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, fnNewCompare, fnNewGet);
                ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS, fnDeckCompare, fnDeckGet);

                cardManager.setup(config, colOverrides);
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
                    updateCaption();
                }
            }


            public void removeCard(PaperCard card) {
                removeCard(card, 1);
            }
            public void removeCard(PaperCard card, int qty) {
                cardManager.removeItem(card, qty);
                updateCaption();
            }

            public void setCards(CardPool cards) {
                cardManager.setItems(cards);
                updateCaption();
            }

            protected void updateCaption() {
            }

            protected abstract void onCardActivated(PaperCard card);
            protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

            private ItemPool<PaperCard> getAllowedAdditions(Iterable<Map.Entry<PaperCard, Integer>> itemsToAdd, boolean isAddSource) {
                ItemPool<PaperCard> additions = new ItemPool<>(cardManager.getGenericType());
                Deck deck = parentScreen.getDeck();

                for (Map.Entry<PaperCard, Integer> itemEntry : itemsToAdd) {
                    PaperCard card = itemEntry.getKey();

                    int max;
                    if (deck == null || card == null) {
                        max = Integer.MAX_VALUE;
                    }
                    else if (DeckFormat.canHaveAnyNumberOf(card)) {
                        max = Integer.MAX_VALUE;
                    }
                    else {
                        max = FModel.getPreferences().getPrefInt(ForgePreferences.FPref.DECK_DEFAULT_CARD_LIMIT);

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
                    else {
                        try {
                            qty = parentScreen.getCatalogPage().cardManager.getItemCount(card);
                        } catch (Exception e) {
                            //prevent NPE
                            qty = 0;
                        }
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
                for (Map.Entry<PaperCard, Integer> itemEntry : selectedItemPool) {
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
                menu.addItem(new FMenuItem(label, icon, new FEvent.FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (max == 1) {
                            callback.run(max);
                        } else {
                            GuiChoose.getInteger(cardManager.getSelectedItem() + " - " + verb + " " + Forge.getLocalizer().getMessage("lblHowMany"), 1, max, 20, callback);
                        }
                    }
                }));
            }

            protected void addCommanderItems(final FDropDownMenu menu, final PaperCard card, boolean isAddMenu, boolean isAddSource) {
                if (parentScreen.getCommanderPage() == null) {
                    return;
                }
                boolean isLegalCommander;
                String captionSuffix = Forge.getLocalizer().getMessage("lblCommander");
                isLegalCommander = DeckFormat.Commander.isLegalCommander(card.getRules());
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
                return   parentScreen.getDeck().getOathbreaker() != null;
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

            protected CatalogPage(ItemManagerConfig config, String caption0, FImage icon0) {
                super(config, caption0, icon0);
            }

            @Override
            protected void initialize() {
                if (initialized) { return; } //prevent initializing more than once if deck changes
                initialized = true;

                super.initialize();
                cardManager.setCaption(getItemManagerCaption());

                if (!isVisible() ) {
                    needRefreshWhenShown = true;
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
                return Forge.getLocalizer().getMessage("lblCards");
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
                final ItemPool<PaperCard> adventurePool = new ItemPool<>(PaperCard.class);

                adventurePool.addAll(AdventurePlayer.current().getCards());
                // remove bottom cards that are in the deck from the card pool
                adventurePool.removeAll(AdventurePlayer.current().getSelectedDeck().getMain());
                // remove sideboard cards from the catalog
                adventurePool.removeAll(AdventurePlayer.current().getSelectedDeck().getOrCreate(DeckSection.Sideboard));
                cardManager.setPool(adventurePool);
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
                if (!needsCommander() && !canOnlyBePartnerCommander(card)) {
                    if(!parentScreen.isShop)
                    {
                        addItem(menu, Forge.getLocalizer().getMessage("lblAdd"), Forge.getLocalizer().getMessage("lblTo") + " " + parentScreen.getMainDeckPage().cardManager.getCaption(), parentScreen.getMainDeckPage().getIcon(), true, true, new Callback<Integer>() {
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
                            addItem(menu, Forge.getLocalizer().getMessage("lblAdd"), Forge.getLocalizer().getMessage("lbltosideboard"), parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
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
                    if(parentScreen.isShop)
                    {
                        addItem(menu, "Sell for ", String.valueOf(AdventurePlayer.current().cardSellPrice(card)), parentScreen.getSideboardPage().getIcon(), true, true, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                if (!cardManager.isInfinite()) {
                                    removeCard(card, result);
                                }
                                AdventurePlayer.current().sellCard(card,result);
                                lblGold.setText(String.valueOf(AdventurePlayer.current().getGold()));
                            }
                        });
                    }
                }


                addCommanderItems(menu, card, true, true);

            }

            @Override
            protected void buildDeckMenu(FPopupMenu menu) {
                if (cardManager.getConfig().getShowUniqueCardsOption()) {
                    menu.addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblUniqueCardsOnly"), cardManager.getWantUnique(), new FEvent.FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            boolean wantUnique = !cardManager.getWantUnique();
                            cardManager.setWantUnique(wantUnique);
                            CatalogPage.this.refresh();
                            cardManager.getConfig().setUniqueCardsOnly(wantUnique);
                        }
                    }));
                }
            }
        }

        protected static class DeckSectionPage extends CardManagerPage {
            private final String captionPrefix;
            private final DeckSection deckSection;

            protected DeckSectionPage(DeckSection deckSection0, ItemManagerConfig config) {
                super(config, null, null);

                deckSection = deckSection0;
                switch (deckSection) {
                    default:
                    case Main:
                        captionPrefix = Forge.getLocalizer().getMessage("lblMain");
                        cardManager.setCaption(Forge.getLocalizer().getMessage("ttMain"));
                        icon = MAIN_DECK_ICON;
                        break;
                    case Sideboard:
                        captionPrefix = Forge.getLocalizer().getMessage("lblSide");
                        cardManager.setCaption(Forge.getLocalizer().getMessage("lblSideboard"));
                        icon = SIDEBOARD_ICON;
                        break;
                    case Commander:
                        captionPrefix = Forge.getLocalizer().getMessage("lblCommander");
                        cardManager.setCaption(Forge.getLocalizer().getMessage("lblCommander"));
                        icon = FSkinImage.COMMANDER;
                        break;
                }
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
                        if (parentScreen.getCatalogPage() != null) {
                            parentScreen.getCatalogPage().addCard(card);
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
                    default:
                    case Main:
                        addItem(menu, Forge.getLocalizer().getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                parentScreen.getCatalogPage().removeCard(card, result);
                                addCard(card, result);
                            }
                        });
                        addItem(menu, Forge.getLocalizer().getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                removeCard(card, result);
                                if (parentScreen.getCatalogPage() != null) {
                                    parentScreen.getCatalogPage().addCard(card, result);
                                }
                            }
                        });
                        if (parentScreen.getSideboardPage() != null) {
                            addItem(menu, Forge.getLocalizer().getMessage("lblMove"), Forge.getLocalizer().getMessage("lbltosideboard"), parentScreen.getSideboardPage().getIcon(), false, false, new Callback<Integer>() {
                                @Override
                                public void run(Integer result) {
                                    if (result == null || result <= 0) { return; }

                                    removeCard(card, result);
                                    parentScreen.getSideboardPage().addCard(card, result);
                                }
                            });
                        }
                        addCommanderItems(menu, card, false, false);
                        break;
                    case Sideboard:
                        addItem(menu, Forge.getLocalizer().getMessage("lblAdd"), null, Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS, true, false, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                parentScreen.getCatalogPage().removeCard(card, result);
                                addCard(card, result);
                            }
                        });
                        addItem(menu, Forge.getLocalizer().getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                removeCard(card, result);
                                if (parentScreen.getCatalogPage() != null) {
                                    parentScreen.getCatalogPage().addCard(card, result);
                                }
                            }
                        });
                        addItem(menu, Forge.getLocalizer().getMessage("lblMove"), Forge.getLocalizer().getMessage("lblToMainDeck"), parentScreen.getMainDeckPage().getIcon(), false, false, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                if (result == null || result <= 0) { return; }

                                removeCard(card, result);
                                parentScreen.getMainDeckPage().addCard(card, result);
                            }
                        });
                        addCommanderItems(menu, card, false, false);
                        break;
                    case Commander:
                        if (  isPartnerCommander(card)) {
                            addItem(menu, Forge.getLocalizer().getMessage("lblRemove"), null, Forge.hdbuttons ? FSkinImage.HDMINUS : FSkinImage.MINUS, false, false, new Callback<Integer>() {
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

    }


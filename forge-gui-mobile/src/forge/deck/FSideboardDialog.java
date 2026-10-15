package forge.deck;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import org.apache.commons.lang3.StringUtils;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.ItemManagerConfig;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuBar;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FDialog;
import forge.toolbox.FLabel;
import forge.toolbox.GuiChoose;

public class FSideboardDialog extends FDialog {
    private final SideboardTabs tabs;
    private final Consumer<List<PaperCard>> callback;

    private final String playerName;
    private final FMenuBar menuBar; //match screen menu bar the "..." button is placed over, if available
    private final FLabel btnMoreOptions;
    private final FDeckEditor.DeckHeader deckHeader; //fallback when the menu bar isn't along the top
    private CardEdition landSet;

    public FSideboardDialog(CardPool sideboard, CardPool main, final Consumer<List<PaperCard>> callback0, String message, boolean allowAddBasicLands) {
        super(String.format(Forge.getLocalizer().getMessage("lblUpdateMainFromSideboard"), message), 1);

        callback = callback0;
        playerName = message;
        FScreen.Header screenHeader = Forge.getCurrentScreen() == null ? null : Forge.getCurrentScreen().getHeader();
        if (!allowAddBasicLands) {
            menuBar = null;
            btnMoreOptions = null;
            deckHeader = null;
        } else if (screenHeader instanceof FMenuBar bar && !bar.getRotate90()) {
            //limited formats have unlimited basic lands available while sideboarding;
            //offer them from a "..." button in the top right, where the deck editor has it
            menuBar = bar;
            btnMoreOptions = add(new FLabel.Builder().text("...").font(FSkinFont.get(20)).align(Align.center)
                    .pressedColor(FScreen.Header.getBtnPressedColor()).build());
            btnMoreOptions.setCommand(e -> showMoreOptionsMenu(btnMoreOptions));
            deckHeader = null;
        } else {
            menuBar = null;
            btnMoreOptions = null;
            deckHeader = add(new FDeckEditor.DeckHeader());
            deckHeader.lblName.setText(message);
            deckHeader.btnSave.setVisible(false);
            deckHeader.btnMoreOptions.setCommand(e -> showMoreOptionsMenu(deckHeader.btnMoreOptions));
        }
        tabs = add(new SideboardTabs(sideboard, main));
        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> hide());
        if (sideboard.isEmpty()) { //show main deck by default if sideboard is empty
            tabs.setSelectedPage(tabs.getMainDeckPage());
        }
    }

    private void showMoreOptionsMenu(FLabel button) {
        new FPopupMenu() {
            @Override
            protected void buildMenu() {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAddBasicLands"), FSkinImage.LANDLOGO, e -> showAddBasicLandsDialog()));
            }
        }.show(button, 0, button.getHeight());
    }

    private void showAddBasicLandsDialog() {
        Deck deck = new Deck(playerName);
        deck.getMain().addAll(tabs.getMainDeckPage().cardManager.getPool());
        deck.getOrCreate(DeckSection.Sideboard).addAll(tabs.getSideboardPage().cardManager.getPool());
        if (landSet == null) { //keep the same land set for the rest of this sideboarding session
            landSet = DeckProxy.getDefaultLandSet(deck);
        }
        tabs.setSelectedPage(tabs.getMainDeckPage());
        new AddBasicLandsDialog(deck, landSet, lands -> tabs.getMainDeckPage().addCards(lands), List.of(landSet)).show();
    }

    @Override
    public void setVisible(boolean visible0) {
        super.setVisible(visible0);
        if (menuBar != null) { //make room on the menu bar for the "..." button only while this is open
            menuBar.setTrailingWidth(visible0 ? FDeckEditor.HEADER_HEIGHT : 0);
        }
        if (!visible0) { //do callback when hidden to ensure you don't get stuck if Back pressed
            callback.accept(tabs.getMainDeckPage().cardManager.getPool().toFlatList());
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        if (btnMoreOptions != null) { //match the menu bar behind the button rather than the dimmed overlay
            g.fillRect(FScreen.Header.getBackColor(), btnMoreOptions.getLeft(), btnMoreOptions.getTop(), btnMoreOptions.getWidth(), btnMoreOptions.getHeight());
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        if (btnMoreOptions != null) {
            //content is laid out from the dialog's top edge and then shifted down by the space left above it,
            //so offset by that space to line the button up with the menu bar on screen
            float shift = getHeight() - maxHeight - VPrompt.HEIGHT;
            Rectangle barPos = menuBar.screenPos;
            float buttonWidth = FDeckEditor.HEADER_HEIGHT;
            btnMoreOptions.setBounds(barPos.x + barPos.width - buttonWidth - screenPos.x, barPos.y - screenPos.y - shift, buttonWidth, barPos.height);
        }
        float y = 0;
        if (deckHeader != null) {
            deckHeader.setBounds(0, 0, width, FDeckEditor.HEADER_HEIGHT);
            y = FDeckEditor.HEADER_HEIGHT;
        }
        tabs.setBounds(0, y, width, maxHeight - y);
        return maxHeight;
    }

    private static class SideboardTabs extends TabPageScreen<SideboardTabs> {
        private SideboardTabs(CardPool sideboard, CardPool main) {
            super(new TabPageBase[] {
                    new SideboardPage(sideboard),
                    new MainDeckPage(main)
            }, false);
            ((SideboardPage) tabPages.get(0)).parent = this;
            ((MainDeckPage) tabPages.get(1)).parent = this;
        }

        private SideboardPage getSideboardPage() {
            return ((SideboardPage) tabPages.get(0));
        }

        private MainDeckPage getMainDeckPage() {
            return ((MainDeckPage) tabPages.get(1));
        }

        @Override
        protected boolean canActivateTabPage() {
            return true; //always allow activating tab pages while this is open
        }

        @Override
        public FScreen getLandscapeBackdropScreen() {
            return null;
        }

        private static abstract class TabPageBase extends TabPage<SideboardTabs> {
            protected SideboardTabs parent;
            protected final CardManager cardManager = add(new CardManager(false));

            protected TabPageBase(CardPool cardPool, FImage icon0) {
                super("", icon0);

                cardManager.setItemActivateHandler(e -> onCardActivated(cardManager.getSelectedItem()));
                cardManager.setContextMenuBuilder(new ContextMenuBuilder<PaperCard>() {
                    @Override
                    public void buildMenu(final FDropDownMenu menu, final PaperCard card) {
                        TabPageBase.this.buildMenu(menu, card);
                    }
                });
                cardManager.setup(ItemManagerConfig.SIDEBOARD);
                cardManager.setPool(new CardPool(cardPool)); //create copy of card pool to avoid modifying the original card pool
                updateCaption();
            }

            protected void addCard(PaperCard card, int qty) {
                cardManager.addItem(card, qty);
                updateCaption();
            }

            protected void addCards(CardPool cards) {
                cardManager.addItems(cards);
                updateCaption();
            }

            protected void removeCard(PaperCard card, int qty) {
                cardManager.removeItem(card, qty);
                updateCaption();
            }

            protected void addItem(FDropDownMenu menu, final String verb, String dest, FImage icon, final Consumer<Integer> callback) {
                String label = verb;
                if (!StringUtils.isEmpty(dest)) {
                    label += " " + dest;
                }
                menu.addItem(new FMenuItem(label, icon, e -> {
                    PaperCard card = cardManager.getSelectedItem();
                    int max = cardManager.getItemCount(card);
                    if (max == 1) {
                        callback.accept(max);
                    }
                    else {
                        GuiChoose.getInteger(card + " - " + verb + " " + Forge.getLocalizer().getMessage("lblHowMany"), 1, max, 20, callback);
                    }
                }));
            }

            protected abstract void updateCaption();
            protected abstract void onCardActivated(PaperCard card);
            protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

            @Override
            protected void doLayout(float width, float height) {
                cardManager.setBounds(0, 0, width, height);
            }
        }

        private static class SideboardPage extends TabPageBase {
            protected SideboardPage(CardPool cardPool) {
                super(cardPool, FDeckEditor.SIDEBOARD_ICON);
                cardManager.setCaption(Forge.getLocalizer().getMessage("lblSideboard"));
            }

            @Override
            protected void updateCaption() {
                caption = Forge.getLocalizer().getMessage("lblSideboard") + " (" + cardManager.getPool().countAll() + ")";
            }

            @Override
            protected void onCardActivated(PaperCard card) {
                removeCard(card, 1);
                parent.getMainDeckPage().addCard(card, 1);
            }

            @Override
            protected void buildMenu(FDropDownMenu menu, final PaperCard card) {
                addItem(menu, Forge.getLocalizer().getMessage("lblMove"), Forge.getLocalizer().getMessage("lblToMainDeck"), FDeckEditor.MAIN_DECK_ICON, result -> {
                    if (result == null || result <= 0) { return; }

                    removeCard(card, result);
                    parent.getMainDeckPage().addCard(card, result);
                });
            }
        }

        private static class MainDeckPage extends TabPageBase {
            protected MainDeckPage(CardPool cardPool) {
                super(cardPool, FDeckEditor.MAIN_DECK_ICON);
                cardManager.setCaption(Forge.getLocalizer().getMessage("ttMain"));
            }

            @Override
            protected void updateCaption() {
                caption = Forge.getLocalizer().getMessage("ttMain") + " (" + cardManager.getPool().countAll() + ")";
            }

            @Override
            protected void onCardActivated(PaperCard card) {
                removeCard(card, 1);
                parent.getSideboardPage().addCard(card, 1);
            }

            @Override
            protected void buildMenu(FDropDownMenu menu, final PaperCard card) {
                addItem(menu, Forge.getLocalizer().getMessage("lblMove"), Forge.getLocalizer().getMessage("lbltosideboard"), FDeckEditor.SIDEBOARD_ICON, result -> {
                    if (result == null || result <= 0) { return; }

                    removeCard(card, result);
                    parent.getSideboardPage().addCard(card, result);
                });
            }
        }
    }
}

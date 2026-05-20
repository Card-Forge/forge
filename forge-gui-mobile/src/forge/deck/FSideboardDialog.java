package forge.deck;

import java.util.List;
import java.util.function.Consumer;

import forge.Forge;
import org.apache.commons.lang3.StringUtils;

import forge.assets.FImage;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.ItemManagerConfig;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.toolbox.FDialog;
import forge.toolbox.GuiChoose;

public class FSideboardDialog extends FDialog {
    private final SideboardTabs tabs;
    private final Consumer<List<PaperCard>> callback;

    public FSideboardDialog(CardPool sideboard, CardPool main, final Consumer<List<PaperCard>> callback0, String message) {
        super(String.format(Forge.getLocalizer().getMessage("lblUpdateMainFromSideboard"), message), 1);

        callback = callback0;
        tabs = add(new SideboardTabs(sideboard, main));
        initButton(0, Forge.getLocalizer().getMessage("lblOK"), e -> hide());
        if (sideboard.isEmpty()) { //show main deck by default if sideboard is empty
            tabs.setSelectedPage(tabs.getMainDeckPage());
        }
    }

    @Override
    public void setVisible(boolean visible0) {
        super.setVisible(visible0);
        if (!visible0) { //do callback when hidden to ensure you don't get stuck if Back pressed
            callback.accept(tabs.getMainDeckPage().cardManager.getPool().toFlatList());
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        tabs.setBounds(0, 0, width, maxHeight);
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

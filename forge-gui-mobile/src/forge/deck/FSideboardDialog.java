package forge.deck;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.assets.FImage;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManager.ContextMenuBuilder;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.TabPageScreen;
import forge.toolbox.FButton;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;

public class FSideboardDialog extends FDialog {
    private final SideboardTabs tabs;
    private final FButton btnOK;

    public FSideboardDialog(CardPool sideboard, CardPool main, final Callback<List<PaperCard>> callback) {
        super("Update main deck from sideboard");

        tabs = add(new SideboardTabs(sideboard, main));
        btnOK = add(new FButton("OK"));
        btnOK.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
                callback.run(tabs.getMainDeckPage().cardManager.getPool().toFlatList());
            }
        });
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float buttonHeight = FOptionPane.BUTTON_HEIGHT;
        float buttonTop = maxHeight - buttonHeight - FOptionPane.GAP_BELOW_BUTTONS;

        tabs.setBounds(0, 0, width, buttonTop - FOptionPane.PADDING);

        float buttonWidth = width / 2;
        btnOK.setBounds((width - buttonWidth) / 2, buttonTop, buttonWidth, buttonHeight);

        return maxHeight;
    }

    private static class SideboardTabs extends TabPageScreen<SideboardTabs> {
        private SideboardTabs(CardPool sideboard, CardPool main) {
            super(new TabPageBase[] {
                    new SideboardPage(sideboard),
                    new MainDeckPage(main)
            }, false);
            ((SideboardPage)tabPages[0]).parent = this;
            ((MainDeckPage)tabPages[1]).parent = this;
        }

        private SideboardPage getSideboardPage() {
            return ((SideboardPage)tabPages[0]);
        }

        private MainDeckPage getMainDeckPage() {
            return ((MainDeckPage)tabPages[1]);
        }

        private static abstract class TabPageBase extends TabPage<SideboardTabs> {
            protected SideboardTabs parent;
            protected final CardManager cardManager = add(new CardManager(false));

            protected TabPageBase(CardPool cardPool, FImage icon0) {
                super("", icon0);

                cardManager.setItemActivateHandler(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        onCardActivated(cardManager.getSelectedItem());
                    }
                });
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

            protected void addItem(FDropDownMenu menu, final String verb, String dest, FImage icon, final Callback<Integer> callback) {
                String label = verb;
                if (!StringUtils.isEmpty(dest)) {
                    label += " " + dest;
                }
                menu.addItem(new FMenuItem(label, icon, new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        PaperCard card = cardManager.getSelectedItem();
                        int max = cardManager.getItemCount(card);
                        if (max == 1) {
                            callback.run(max);
                        }
                        else {
                            GuiChoose.getInteger(card + " - " + verb + " how many?", 1, max, 20, callback);
                        }
                    }
                }));
            }

            protected abstract void updateCaption();
            protected abstract void onCardActivated(PaperCard card);
            protected abstract void buildMenu(final FDropDownMenu menu, final PaperCard card);

            @Override
            protected void doLayout(float width, float height) {
                float y = FOptionPane.PADDING - ItemFilter.PADDING;
                cardManager.setBounds(FOptionPane.PADDING, y, width - 2 * FOptionPane.PADDING, height - y);
            }
        }

        private static class SideboardPage extends TabPageBase {
            protected SideboardPage(CardPool cardPool) {
                super(cardPool, FDeckEditor.SIDEBOARD_ICON);
                cardManager.setCaption("Sideboard");
            }

            @Override
            protected void updateCaption() {
                caption = "Sideboard (" + cardManager.getPool().countAll() + ")";
            }

            @Override
            protected void onCardActivated(PaperCard card) {
                removeCard(card, 1);
                parent.getMainDeckPage().addCard(card, 1);
            }

            @Override
            protected void buildMenu(FDropDownMenu menu, final PaperCard card) {
                addItem(menu, "Move", "to main deck", FDeckEditor.MAIN_DECK_ICON, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        parent.getMainDeckPage().addCard(card, result);
                    }
                });
            }
        }

        private static class MainDeckPage extends TabPageBase {
            protected MainDeckPage(CardPool cardPool) {
                super(cardPool, FDeckEditor.MAIN_DECK_ICON);
                cardManager.setCaption("Main Deck");
            }

            @Override
            protected void updateCaption() {
                caption = "Main Deck (" + cardManager.getPool().countAll() + ")";
            }

            @Override
            protected void onCardActivated(PaperCard card) {
                removeCard(card, 1);
                parent.getSideboardPage().addCard(card, 1);
            }

            @Override
            protected void buildMenu(FDropDownMenu menu, final PaperCard card) {
                addItem(menu, "Move", "to sideboard", FDeckEditor.SIDEBOARD_ICON, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == null || result <= 0) { return; }

                        removeCard(card, result);
                        parent.getSideboardPage().addCard(card, result);
                    }
                });
            }
        }
    }
}

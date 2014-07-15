package forge.itemmanager;

import java.util.Map.Entry;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.filters.CardCMCFilter;
import forge.itemmanager.filters.CardCMCRangeFilter;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardFormatFilter;
import forge.itemmanager.filters.CardPowerFilter;
import forge.itemmanager.filters.CardSearchFilter;
import forge.itemmanager.filters.CardSetFilter;
import forge.itemmanager.filters.CardToughnessFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.menu.FSubMenu;
import forge.model.FModel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

/** 
 * ItemManager for cards
 *
 */
public class CardManager extends ItemManager<PaperCard> {
    public CardManager(boolean wantUnique0) {
        super(PaperCard.class, wantUnique0);
    }

    @Override
    protected void addDefaultFilters() {
        addDefaultFilters(this);
    }

    @Override
    protected ItemFilter<PaperCard> createSearchFilter() {
        return createSearchFilter(this);
    }

    @Override
    protected void buildAddFilterMenu(FPopupMenu menu) {
        buildAddFilterMenu(menu, this);
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super PaperCard> itemManager) {
        itemManager.addFilter(new CardColorFilter(itemManager));
        itemManager.addFilter(new CardTypeFilter(itemManager));
        itemManager.addFilter(new CardCMCFilter(itemManager));
    }

    public static ItemFilter<PaperCard> createSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new CardSearchFilter(itemManager);
    }

    public static void buildAddFilterMenu(FPopupMenu menu, final ItemManager<? super PaperCard> itemManager) {
        menu.addItem(new FSubMenu("Format", new FPopupMenu() {
            @Override
            protected void buildMenu() {
                for (final GameFormat f : FModel.getFormats()) {
                    addItem(new FMenuItem(f.getName(), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            itemManager.addFilter(new CardFormatFilter(itemManager, f));
                        }
                    }, CardFormatFilter.canAddFormat(f, itemManager.getFilter(CardFormatFilter.class))));
                }
            }
        }));

        menu.addItem(new FMenuItem("Sets...", null, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                CardSetFilter existingFilter = itemManager.getFilter(CardSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                }
                else {
                    /*final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override
                        public void run() {
                            List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                itemManager.addFilter(new CardSetFilter(itemManager, sets, dialog.getWantReprints()));
                            }
                        }
                    });*/
                }
            }
        }));

        /*menu.addItem(new FSubMenu("Quest world", new FPopupMenu() {
            @Override
            protected void buildMenu() {
                for (final QuestWorld w : FModel.getWorlds()) {
                    addItem(new FMenuItem(w.getName(), new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            itemManager.addFilter(new CardQuestWorldFilter(itemManager, w));
                        }
                    }, CardFormatFilter.canAddQuestWorld(w, itemManager.getFilter(CardQuestWorldFilter.class))));
                }
            }
        }));*/

        menu.addItem(new FMenuItem("Colors", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardColorFilter(itemManager));
            }
        }, itemManager.getFilter(CardColorFilter.class) == null));
        menu.addItem(new FMenuItem("Types", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardTypeFilter(itemManager));
            }
        }, itemManager.getFilter(CardTypeFilter.class) == null));
        menu.addItem(new FMenuItem("Converted mana costs", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardCMCFilter(itemManager));
            }
        }, itemManager.getFilter(CardCMCFilter.class) == null));

        menu.addItem(new FMenuItem("CMC range", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardCMCRangeFilter(itemManager));
            }
        }, itemManager.getFilter(CardCMCRangeFilter.class) == null));
        menu.addItem(new FMenuItem("Power range", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardPowerFilter(itemManager));
            }
        }, itemManager.getFilter(CardPowerFilter.class) == null));
        menu.addItem(new FMenuItem("Toughness range", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                itemManager.addFilter(new CardToughnessFilter(itemManager));
            }
        }, itemManager.getFilter(CardToughnessFilter.class) == null));
    }

    @Override
    public ItemRenderer getListItemRenderer() {
        return new ItemRenderer() {
            @Override
            public float getItemHeight() {
                return CardRenderer.getCardListItemHeight();
            }

            @Override
            public void drawValue(Graphics g, Entry<PaperCard, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                CardRenderer.drawCardListItem(g, font, foreColor, value.getKey(), isInfinite() ? 0 : value.getValue(), x, y, w, h);
            }

            @Override
            public boolean tap(Entry<PaperCard, Integer> value, float x, float y, int count) {
                return CardRenderer.cardListItemTap(value.getKey(), x, y, count);
            }

            @Override
            public boolean longPress(Entry<PaperCard, Integer> value, float x, float y) {
                CardZoom.show(value.getKey());
                return true;
            }
        };
    }
}

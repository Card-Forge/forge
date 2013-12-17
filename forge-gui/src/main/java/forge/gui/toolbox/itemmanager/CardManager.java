package forge.gui.toolbox.itemmanager;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import forge.Singletons;
import forge.game.GameFormat;
import forge.gui.GuiUtils;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.itemmanager.filters.CardCMCFilter;
import forge.gui.toolbox.itemmanager.filters.CardColorFilter;
import forge.gui.toolbox.itemmanager.filters.CardFormatFilter;
import forge.gui.toolbox.itemmanager.filters.CardPowerFilter;
import forge.gui.toolbox.itemmanager.filters.CardQuestWorldFilter;
import forge.gui.toolbox.itemmanager.filters.CardSearchFilter;
import forge.gui.toolbox.itemmanager.filters.CardSetFilter;
import forge.gui.toolbox.itemmanager.filters.CardToughnessFilter;
import forge.gui.toolbox.itemmanager.filters.CardTypeFilter;
import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.item.PaperCard;
import forge.quest.QuestWorld;

/** 
 * ItemManager for cards
 *
 */
@SuppressWarnings("serial")
public final class CardManager extends ItemManager<PaperCard> {
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
    protected void buildFilterMenu(JPopupMenu menu) {
        buildFilterMenu(menu, this);
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super PaperCard> itemManager) {
        itemManager.addFilter(new CardColorFilter(itemManager));
        itemManager.addFilter(new CardTypeFilter(itemManager));
    }

    public static ItemFilter<PaperCard> createSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new CardSearchFilter(itemManager);
    }

    public static void buildFilterMenu(JPopupMenu menu, final ItemManager<? super PaperCard> itemManager) {
        JMenu fmt = new JMenu("Format");
        for (final GameFormat f : Singletons.getModel().getFormats()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new CardFormatFilter(itemManager, f));
                }
            }, CardFormatFilter.canAddFormat(f, itemManager.getFilter(CardFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, "Sets...", null, new Runnable() {
            @Override
            public void run() {
                CardSetFilter existingFilter = itemManager.getFilter(CardSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit();
                }
                else {
                    final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override
                        public void run() {
                            List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                itemManager.addFilter(new CardSetFilter(itemManager, sets, dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        JMenu range = new JMenu("Value range");
        GuiUtils.addMenuItem(range, "CMC", null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardCMCFilter(itemManager));
            }
        }, itemManager.getFilter(CardCMCFilter.class) == null);
        GuiUtils.addMenuItem(range, "Power", null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardPowerFilter(itemManager));
            }
        }, itemManager.getFilter(CardPowerFilter.class) == null);
        GuiUtils.addMenuItem(range, "Toughness", null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardToughnessFilter(itemManager));
            }
        }, itemManager.getFilter(CardToughnessFilter.class) == null);
        menu.add(range);

        JMenu world = new JMenu("Quest world");
        for (final QuestWorld w : Singletons.getModel().getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new CardQuestWorldFilter(itemManager, w));
                }
            }, CardQuestWorldFilter.canAddQuestWorld(w, itemManager.getFilter(CardQuestWorldFilter.class)));
        }
        menu.add(world);
    }
}

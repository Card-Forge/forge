package forge.gui.toolbox.itemmanager;

import java.util.List;

import javax.swing.JMenu;

import forge.Singletons;
import forge.deck.Deck;
import forge.game.GameFormat;
import forge.gui.GuiUtils;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.itemmanager.filters.DeckColorFilter;
import forge.gui.toolbox.itemmanager.filters.DeckFormatFilter;
import forge.gui.toolbox.itemmanager.filters.DeckQuestWorldFilter;
import forge.gui.toolbox.itemmanager.filters.DeckSearchFilter;
import forge.gui.toolbox.itemmanager.filters.DeckSetFilter;
import forge.gui.toolbox.itemmanager.filters.ItemFilter;
import forge.quest.QuestWorld;

/** 
 * ItemManager for cards
 *
 */
@SuppressWarnings("serial")
public final class DeckManager extends ItemManager<Deck> {
    public DeckManager(boolean wantUnique0) {
        super(Deck.class, wantUnique0);
    }

    @Override
    protected void addDefaultFilters() {
        addDefaultFilters(this);
    }

    @Override
    protected ItemFilter<Deck> createSearchFilter() {
        return createSearchFilter(this);
    }

    @Override
    protected void buildAddFilterMenu(JMenu menu) {
        buildAddFilterMenu(menu, this);
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super Deck> itemManager) {
        itemManager.addFilter(new DeckColorFilter(itemManager));
    }

    public static ItemFilter<Deck> createSearchFilter(final ItemManager<? super Deck> itemManager) {
        return new DeckSearchFilter(itemManager);
    }

    public static void buildAddFilterMenu(JMenu menu, final ItemManager<? super Deck> itemManager) {
        GuiUtils.addSeparator(menu); //separate from current search item

        JMenu fmt = GuiUtils.createMenu("Format");
        for (final GameFormat f : Singletons.getModel().getFormats()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new DeckFormatFilter(itemManager, f));
                }
            }, DeckFormatFilter.canAddFormat(f, itemManager.getFilter(DeckFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, "Sets...", null, new Runnable() {
            @Override
            public void run() {
                DeckSetFilter existingFilter = itemManager.getFilter(DeckSetFilter.class);
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
                                itemManager.addFilter(new DeckSetFilter(itemManager, sets, dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        JMenu world = GuiUtils.createMenu("Quest world");
        for (final QuestWorld w : Singletons.getModel().getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new DeckQuestWorldFilter(itemManager, w));
                }
            }, DeckQuestWorldFilter.canAddQuestWorld(w, itemManager.getFilter(DeckQuestWorldFilter.class)));
        }
        menu.add(world);

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, "Colors", null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new DeckColorFilter(itemManager));
            }
        }, itemManager.getFilter(DeckColorFilter.class) == null);
    }
}

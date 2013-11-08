package forge.gui.toolbox.itemmanager;

import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import forge.Singletons;
import forge.game.GameFormat;
import forge.gui.GuiUtils;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
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

    public CardManager(Map<StatTypes, FLabel> statLabels0, boolean wantUnique0) {
        super(PaperCard.class, statLabels0, wantUnique0);
        
        this.addFilter(new CardColorFilter(this));
        this.addFilter(new CardTypeFilter(this));
    }

    @Override
    protected ItemFilter<PaperCard> createSearchFilter(String text) {
        return new CardSearchFilter(this, text);
    }

    @Override
    protected void buildFilterMenu(JPopupMenu menu) {        
        JMenu fmt = new JMenu("Format");
        for (final GameFormat f : Singletons.getModel().getFormats()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    addFilter(new CardFormatFilter(CardManager.this, f));
                }
            }, CardFormatFilter.canAddFormat(f, getFilter(CardFormatFilter.class)));
        }
        menu.add(fmt);
        
        GuiUtils.addMenuItem(menu, "Sets...", null, new Runnable() {
            @Override
            public void run() {
                CardSetFilter existingFilter = getFilter(CardSetFilter.class);
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
                                addFilter(new CardSetFilter(CardManager.this, sets));
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
                addFilter(new CardCMCFilter(CardManager.this));
            }
        }, getFilter(CardCMCFilter.class) == null);
        GuiUtils.addMenuItem(range, "Power", null, new Runnable() {
            @Override
            public void run() {
                addFilter(new CardPowerFilter(CardManager.this));
            }
        }, getFilter(CardPowerFilter.class) == null);
        GuiUtils.addMenuItem(range, "Toughness", null, new Runnable() {
            @Override
            public void run() {
                addFilter(new CardToughnessFilter(CardManager.this));
            }
        }, getFilter(CardToughnessFilter.class) == null);
        menu.add(range);
        
        JMenu world = new JMenu("Quest world");
        for (final QuestWorld w : Singletons.getModel().getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override
                public void run() {
                    addFilter(new CardQuestWorldFilter(CardManager.this, w));
                }
            }, CardQuestWorldFilter.canAddQuestWorld(w, getFilter(CardQuestWorldFilter.class)));
        }
        menu.add(world);
    }
}

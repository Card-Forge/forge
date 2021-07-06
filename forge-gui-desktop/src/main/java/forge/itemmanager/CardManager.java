package forge.itemmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JMenu;

import forge.game.GameFormat;
import forge.gamemodes.quest.QuestWorld;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gui.GuiUtils;
import forge.item.PaperCard;
import forge.itemmanager.filters.*;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.home.quest.DialogChooseFormats;
import forge.screens.home.quest.DialogChooseSets;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.Localizer;

/** 
 * ItemManager for cards
 *
 */
@SuppressWarnings("serial")
public class CardManager extends ItemManager<PaperCard> {
    
    private boolean QuestMode;

    public CardManager(final CDetailPicture cDetailPicture, final boolean wantUnique0, final boolean qm) {
        super(PaperCard.class, cDetailPicture, wantUnique0);
        QuestMode = qm;
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
    protected void buildAddFilterMenu(JMenu menu) {
        buildAddFilterMenu(menu, this);
    }

    @Override
    protected Iterable<Entry<PaperCard, Integer>> getUnique(Iterable<Entry<PaperCard, Integer>> items) {
        //use special technique for getting unique cards so that cards without art aren't shown
        HashMap<String, Entry<PaperCard, Integer>> map = new HashMap<>();
        for (Entry<PaperCard, Integer> item : items) {
            final String key = item.getKey().getName();
            final Entry<PaperCard, Integer> oldValue = map.get(key);
            if (oldValue == null || !oldValue.getKey().hasImage()) { //only replace in map if old value doesn't have image
                map.put(key, item);
            }
        }
        return map.values();
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super PaperCard> itemManager) {
        itemManager.addFilter(new CardColorFilter(itemManager));
        itemManager.addFilter(new CardTypeFilter(itemManager));
        itemManager.addFilter(new CardCMCFilter(itemManager));
        if (FModel.getQuestPreferences()
                .getPrefInt(QuestPreferences.QPref.FOIL_FILTER_DEFAULT) == 1) {
            itemManager.addFilter(new CardFoilFilter(itemManager));
        }
        if (FModel.getQuestPreferences()
                .getPrefInt(QuestPreferences.QPref.RATING_FILTER_DEFAULT) == 1) {
            itemManager.addFilter(new CardRatingFilter(itemManager));
        }
    }

    public static ItemFilter<PaperCard> createSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new CardSearchFilter(itemManager);
    }

    public void buildAddFilterMenu(JMenu menu, final ItemManager<? super PaperCard> itemManager) {
        GuiUtils.addSeparator(menu); //separate from current search item
        final Localizer localizer = Localizer.getInstance();
        JMenu fmt = GuiUtils.createMenu(localizer.getMessage("lblFormat"));
        for (final GameFormat f : FModel.getFormats().getFilterList()) {
            GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new CardFormatFilter(itemManager, f));
                }
            }, FormatFilter.canAddFormat(f, itemManager.getFilter(CardFormatFilter.class)));
        }
        menu.add(fmt);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblFormats") + "...", null, new Runnable() {
            @Override public void run() {
                final CardFormatFilter existingFilter = itemManager.getFilter(CardFormatFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit(itemManager);
                } else {
                    final DialogChooseFormats dialog = new DialogChooseFormats();
                    dialog.setWantReprintsCB(true); // assume user wants things permissive...
                    dialog.setOkCallback(new Runnable() {
                        @Override public void run() {
                            final List<GameFormat> formats = dialog.getSelectedFormats();
                            if (!formats.isEmpty()) {
                                itemManager.addFilter(new CardFormatFilter(itemManager,formats,dialog.getWantReprints()));
                            }
                        }
                    });
                }
            }
        });

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblSets") + "...", null, new Runnable() {
            @Override
            public void run() {
                CardSetFilter existingFilter = itemManager.getFilter(CardSetFilter.class);
                if (existingFilter != null) {
                    existingFilter.edit(itemManager);
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

        JMenu world = GuiUtils.createMenu(localizer.getMessage("lblQuestWorld"));
        for (final QuestWorld w : FModel.getWorlds()) {
            GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new CardQuestWorldFilter(itemManager, w));
                }
            }, CardQuestWorldFilter.canAddQuestWorld(w, itemManager.getFilter(CardQuestWorldFilter.class)));
        }
        menu.add(world);

        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.LOAD_HISTORIC_FORMATS)) {
            JMenu blocks = GuiUtils.createMenu(localizer.getMessage("lblBlock"));
            List<GameFormat> blockFormats = new ArrayList<>();
            for (GameFormat format : FModel.getFormats().getHistoricList()){
                if (format.getFormatSubType() != GameFormat.FormatSubType.BLOCK)
                    continue;
                if (!format.getName().endsWith("Block"))
                    continue;
                blockFormats.add(format);
            }
            Collections.sort(blockFormats);  // GameFormat will be sorted by Index!
            for (final GameFormat f : blockFormats) {
                GuiUtils.addMenuItem(blocks, f.getName(), null, new Runnable() {
                    @Override
                    public void run() {
                        itemManager.addFilter(new CardBlockFilter(itemManager, f));
                    }
                }, CardBlockFilter.canAddCardBlockWorld(f, itemManager.getFilter(CardBlockFilter.class)));
            }
            menu.add(blocks);
        }

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblColors"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardColorFilter(itemManager));
            }
        }, itemManager.getFilter(CardColorFilter.class) == null);
        GuiUtils.addMenuItem(menu, localizer.getMessage("lblTypes"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardTypeFilter(itemManager));
            }
        }, itemManager.getFilter(CardTypeFilter.class) == null);
        GuiUtils.addMenuItem(menu, localizer.getMessage("lblConvertedManaCosts"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardCMCFilter(itemManager));
            }
        }, itemManager.getFilter(CardCMCFilter.class) == null);

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblCMCRange"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardCMCRangeFilter(itemManager));
            }
        }, itemManager.getFilter(CardCMCRangeFilter.class) == null);
        GuiUtils.addMenuItem(menu, localizer.getMessage("lblPowerRange"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardPowerFilter(itemManager));
            }
        }, itemManager.getFilter(CardPowerFilter.class) == null);
        GuiUtils.addMenuItem(menu, localizer.getMessage("lblToughnessRange"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardToughnessFilter(itemManager));
            }
        }, itemManager.getFilter(CardToughnessFilter.class) == null);

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblFoil"), null, new Runnable() {
            @Override
            public void run() {
                itemManager.addFilter(new CardFoilFilter(itemManager));
            }
        }, itemManager.getFilter(CardFoilFilter.class) == null);

        if (QuestMode) {
            GuiUtils.addMenuItem(menu, localizer.getMessage("lblPersonalRating"), null, new Runnable() {
                @Override
                public void run() {
                    itemManager.addFilter(new CardRatingFilter(itemManager));
                }
            }, itemManager.getFilter(CardRatingFilter.class) == null);
        }

        GuiUtils.addSeparator(menu);

        GuiUtils.addMenuItem(menu, localizer.getMessage("lblAdvanced")+ "...", null, new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                AdvancedSearchFilter<PaperCard> filter = itemManager.getFilter(AdvancedSearchFilter.class);
                if (filter != null) {
                    filter.edit();
                }
                else {
                    filter = new AdvancedSearchFilter<>(itemManager);
                    itemManager.lockFiltering = true; //ensure filter not applied until added
                    boolean result = filter.edit();
                    itemManager.lockFiltering = false;
                    if (result) {
                        itemManager.addFilter(filter);
                    }
                }
            }
        });
    }
}

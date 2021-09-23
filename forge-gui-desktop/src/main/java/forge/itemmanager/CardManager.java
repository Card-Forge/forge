package forge.itemmanager;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import forge.StaticData;
import forge.card.CardEdition;
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
import forge.util.CollectionSuppliers;
import forge.util.Localizer;

import javax.swing.*;
import java.util.*;
import java.util.Map.Entry;

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
        ListMultimap<String, Entry<PaperCard, Integer>> entriesByName = Multimaps.newListMultimap(
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER), CollectionSuppliers.arrayLists());
        for (Entry<PaperCard, Integer> item : items) {
            final String cardName = item.getKey().getName();
            entriesByName.put(cardName, item);
        }

        // Now we're ready to go on with retrieving cards to be returned
        Map<PaperCard, Integer> cardsMap = new HashMap<>();
        for (String cardName : entriesByName.keySet()) {
            List<Entry<PaperCard, Integer>> entries = entriesByName.get(cardName);

            ListMultimap<CardEdition, Entry<PaperCard, Integer>> entriesByEdition = Multimaps.newListMultimap(new HashMap<>(), CollectionSuppliers.arrayLists());
            for (Entry<PaperCard, Integer> entry : entries) {
                CardEdition ed = StaticData.instance().getCardEdition(entry.getKey().getEdition());
                if (ed != null)
                    entriesByEdition.put(ed, entry);
            }
            if (entriesByEdition.size() == 0)
                continue;  // skip card

            // Try to retain only those editions accepted by the current Card Art Preference Policy
            List<CardEdition> acceptedEditions = Lists.newArrayList(Iterables.filter(entriesByEdition.keySet(), new Predicate<CardEdition>() {
                @Override
                public boolean apply(CardEdition ed) {
                    return StaticData.instance().getCardArtPreference().accept(ed);
                }
            }));

            // If policy too strict, fall back to getting all editions.
            if (acceptedEditions.size() == 0)
                // Policy is too strict for current PaperCard in Entry. Remove any filter
                acceptedEditions.addAll(entriesByEdition.keySet());

            Entry<PaperCard, Integer> cardEntry = getCardEntryToAdd(entriesByEdition, acceptedEditions);
            if (cardEntry != null)
                cardsMap.put(cardEntry.getKey(), cardEntry.getValue());
        }
        return cardsMap.entrySet();
    }

    // Select the Card Art Entry to add, based on current Card Art Preference Order.
    // This method will prefer the entry currently having an image. If that's not the case,
    private Entry<PaperCard, Integer> getCardEntryToAdd(ListMultimap<CardEdition, Entry<PaperCard, Integer>> entriesByEdition,
                                                            List<CardEdition> acceptedEditions) {
        // Use standard sort + index, for better performance!
        Collections.sort(acceptedEditions);
        if (StaticData.instance().cardArtPreferenceIsLatest())
            Collections.reverse(acceptedEditions);
        Iterator<CardEdition> editionIterator = acceptedEditions.iterator();
        Entry<PaperCard, Integer> candidateEntry = null;
        Entry<PaperCard, Integer> firstCandidateEntryFound = null;
        while (editionIterator.hasNext() && candidateEntry == null){
            CardEdition cardEdition = editionIterator.next();
            // These are now the entries to add to Cards Map
            List<Entry<PaperCard, Integer>> cardEntries = entriesByEdition.get(cardEdition);
            Iterator<Entry<PaperCard, Integer>> entriesIterator = cardEntries.iterator();
            candidateEntry = entriesIterator.hasNext() ? entriesIterator.next() : null;
            if (candidateEntry != null && firstCandidateEntryFound == null)
                firstCandidateEntryFound = candidateEntry;  // save reference to the first candidate entry found!
            while ((candidateEntry == null || !candidateEntry.getKey().hasImage()) && entriesIterator.hasNext()) {
                candidateEntry = entriesIterator.next();
                if (firstCandidateEntryFound == null)
                    firstCandidateEntryFound = candidateEntry;
            }

            if (candidateEntry != null && !candidateEntry.getKey().hasImage())
                candidateEntry = null;  // resetting for next edition
        }
        return candidateEntry != null ? candidateEntry : firstCandidateEntryFound;
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
                    List<String> limitedSets = getFilteredSetCodesInCatalog();
                    final DialogChooseSets dialog = new DialogChooseSets(null, null, limitedSets, true);
                    dialog.setOkCallback(new Runnable() {
                        @Override
                        public void run() {
                            List<String> sets = dialog.getSelectedSets();
                            if (!sets.isEmpty()) {
                                itemManager.addFilter(new CardSetFilter(itemManager, sets, limitedSets, dialog.getWantReprints()));
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

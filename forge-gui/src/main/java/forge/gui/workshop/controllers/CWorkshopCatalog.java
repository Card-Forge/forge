package forge.gui.workshop.controllers;

import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Command;
import forge.Singletons;
import forge.card.CardEdition;
import forge.card.EditionCollection;
import forge.game.GameFormat;
import forge.gui.GuiUtils;
import forge.gui.workshop.views.VWorkshopCatalog;
import forge.gui.deckeditor.views.VCardCatalog.RangeTypes;
import forge.gui.framework.ICDoc;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSpinner;
import forge.gui.toolbox.itemmanager.SFilterUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.gui.toolbox.itemmanager.table.SColumnUtil;
import forge.gui.toolbox.itemmanager.table.TableColumnInfo;
import forge.gui.toolbox.itemmanager.table.SColumnUtil.ColumnName;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.ItemPredicate;
import forge.quest.QuestWorld;
import forge.quest.data.GameFormatQuest;

/** 
 * Controls the "card catalog" panel in the workshop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CWorkshopCatalog implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final Set<Predicate<PaperCard>> activePredicates = new HashSet<Predicate<PaperCard>>();
    private final Set<GameFormat> activeFormats = new HashSet<GameFormat>();
    private final Set<QuestWorld> activeWorlds = new HashSet<QuestWorld>();
    private final Set<RangeTypes> activeRanges = EnumSet.noneOf(RangeTypes.class);
    
    private boolean disableFiltering = false;

    private CWorkshopCatalog() {
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    @SuppressWarnings("serial")
    public void initialize() {        
        final Command updateFilterCommand = new Command() {
            @Override
            public void run() {
                if (!disableFiltering) {
                    applyCurrentFilter();
                }
            }
        };

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> entry : VWorkshopCatalog.SINGLETON_INSTANCE.getStatLabels().entrySet()) {
            final FLabel statLabel = entry.getValue();
            statLabel.setCommand(updateFilterCommand);

            //hook so right-clicking a filter in a group toggles itself off and toggles on all other filters in group
            final SItemManagerUtil.StatTypes st = entry.getKey();
            final int group = st.group;
            if (group > 0) {
                statLabel.setRightClickCommand(new Command() {
                    @Override
                    public void run() {
                        if (!disableFiltering) {
                            disableFiltering = true;

                            boolean foundSelected = false;
                            for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
                                if (s.group == group && s != st) {
                                    FLabel lbl = VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getStatLabel(s);
                                    if (lbl.getSelected()) {
                                        foundSelected = true;
                                        lbl.setSelected(false);
                                    }
                                }
                            }
                            if (!statLabel.getSelected()) {
                                statLabel.setSelected(true);
                            }
                            else if (!foundSelected) {
                                //if statLabel only label in group selected, re-select all other labels in group
                                for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
                                    if (s.group == group && s != st) {
                                        FLabel lbl = VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getStatLabel(s);
                                        if (!lbl.getSelected()) {
                                            lbl.setSelected(true);
                                        }
                                    }
                                }
                            }

                            disableFiltering = false;
                            applyCurrentFilter();
                        }
                    }
                });
            }
        }

        VWorkshopCatalog.SINGLETON_INSTANCE.getStatLabels().get(SItemManagerUtil.StatTypes.TOTAL).setCommand(new Command() {
            private boolean lastToggle = true;
            
            @Override
            public void run() {
                disableFiltering = true;
                lastToggle = !lastToggle;
                for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
                    if (SItemManagerUtil.StatTypes.TOTAL != s) {
                        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getStatLabel(s).setSelected(lastToggle);
                    }
                }
                disableFiltering = false;
                applyCurrentFilter();
            }
        });
        
        // assemble add restriction menu
        final Command addRestrictionCommand = new Command() {
            @Override
            public void run() {
                JPopupMenu popup = new JPopupMenu("RestrictionPopupMenu");
                GuiUtils.addMenuItem(popup, "Current text search",
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                        new Runnable() {
                    @Override
                    public void run() {
                        addRestriction(buildSearchRestriction(), null, null);
                    }
                }, canSearch());
                JMenu fmt = new JMenu("Format");
                for (final GameFormat f : Singletons.getMagicDb().getFormats()) {
                    GuiUtils.addMenuItem(fmt, f.getName(), null, new Runnable() {
                        @Override
                        public void run() {
                            addRestriction(buildFormatRestriction(f.toString(), f, true), activeFormats, f);
                        }
                    }, !isActive(activeFormats, f));
                }
                popup.add(fmt);
                GuiUtils.addMenuItem(popup, "Sets...", null, new Runnable() {
                    @Override
                    public void run() {
                        final DialogChooseSets dialog = new DialogChooseSets(null, null, true);
                        dialog.setOkCallback(new Runnable() {
                            @Override
                            public void run() {
                                List<String> setCodes = dialog.getSelectedSets();
                                
                                if (setCodes.isEmpty()) {
                                    return;
                                }
                                
                                StringBuilder label = new StringBuilder("Sets:");
                                boolean truncated = false;
                                for (String code : setCodes)
                                {
                                    // don't let the full label get too long
                                    if (32 > label.length()) {
                                        label.append(" ").append(code).append(";");
                                    } else {
                                        truncated = true;
                                        break;
                                    }
                                }
                                
                                // chop off last semicolons
                                label.delete(label.length() - 1, label.length());
                                
                                if (truncated) {
                                    label.append("...");
                                }
                                
                                addRestriction(buildSetRestriction(label.toString(), setCodes, dialog.getWantReprints()), null, null);
                            }
                        });
                    }
                });
                JMenu range = new JMenu("Value range");
                for (final RangeTypes t : RangeTypes.values()) {
                    GuiUtils.addMenuItem(range, t.toLabelString() + " restriction", null, new Runnable() {
                        @Override
                        public void run() {
                            addRestriction(buildRangeRestriction(t), activeRanges, t);
                        }
                    }, !isActive(activeRanges, t));
                }
                popup.add(range);
                JMenu world = new JMenu("Quest world");
                for (final QuestWorld w : Singletons.getModel().getWorlds()) {
                    GameFormatQuest format = w.getFormat();
                    if (null == format) {
                        // assumes that no world other than the main world will have a null format
                        format = Singletons.getModel().getQuest().getMainFormat();
                    }
                    final GameFormatQuest f = format;
                    GuiUtils.addMenuItem(world, w.getName(), null, new Runnable() {
                        @Override
                        public void run() {
                            addRestriction(buildFormatRestriction(w.getName(), f, true), activeWorlds, w);
                        }
                    }, !isActive(activeWorlds, w) && null != f);
                }
                popup.add(world);
                popup.show(VWorkshopCatalog.SINGLETON_INSTANCE.getBtnAddRestriction(), 0,
                        VWorkshopCatalog.SINGLETON_INSTANCE.getBtnAddRestriction().getHeight());
            }
        };
        FLabel btnAddRestriction = VWorkshopCatalog.SINGLETON_INSTANCE.getBtnAddRestriction();
        btnAddRestriction.setCommand(addRestrictionCommand);
        btnAddRestriction.setRightClickCommand(addRestrictionCommand); //show menu on right-click too
        
        VWorkshopCatalog.SINGLETON_INSTANCE.getCbSearchMode().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                applyCurrentFilter();
            }
        });
        
        Runnable addSearchRestriction = new Runnable() {
            @Override
            public void run() {
                if (canSearch()) {
                    addRestriction(buildSearchRestriction(), null, null);
                }
            }
        };
        
        // add search restriction on ctrl-enter from either the textbox or combobox
        VWorkshopCatalog.SINGLETON_INSTANCE.getCbSearchMode().addKeyListener(new _OnCtrlEnter(addSearchRestriction));
        VWorkshopCatalog.SINGLETON_INSTANCE.getTxfSearch().addKeyListener(new _OnCtrlEnter(addSearchRestriction) {
            private boolean keypressPending;
            @Override
            public void keyReleased(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode() && 0 == e.getModifiers()) {
                    // set focus to table when a plain enter is typed into the text filter box
                    VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getTable().requestFocusInWindow();
                } else if (keypressPending) {
                    // do this in keyReleased instead of keyTyped since the textbox text isn't updated until the key is released
                    // but depend on keypressPending since otherwise we pick up hotkeys and other unwanted stuff
                    applyCurrentFilter();
                }
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                keypressPending = KeyEvent.VK_ENTER != e.getKeyCode();
            }
        });
        
        VWorkshopCatalog.SINGLETON_INSTANCE.getLblName().setCommand(updateFilterCommand);
        VWorkshopCatalog.SINGLETON_INSTANCE.getLblType().setCommand(updateFilterCommand);
        VWorkshopCatalog.SINGLETON_INSTANCE.getLblText().setCommand(updateFilterCommand);
        
        // ensure mins can's exceed maxes and maxes can't fall below mins
        for (Pair<FSpinner, FSpinner> sPair : VWorkshopCatalog.SINGLETON_INSTANCE.getSpinners().values()) {
            final FSpinner min = sPair.getLeft();
            final FSpinner max = sPair.getRight();
            
            min.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent arg0) {
                    if (Integer.parseInt(max.getValue().toString()) <
                            Integer.parseInt(min.getValue().toString()))
                    {
                        max.setValue(min.getValue());
                    }
                    applyCurrentFilter();
                }
            });
            
            max.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent arg0) {
                    if (Integer.parseInt(min.getValue().toString()) >
                            Integer.parseInt(max.getValue().toString()))
                    {
                        min.setValue(max.getValue());
                    }
                    applyCurrentFilter();
                }
            });
        }
    }

    private class _OnCtrlEnter extends KeyAdapter {
        private final Runnable action;
        _OnCtrlEnter(Runnable action) {
            this.action = action;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == 10) {
                if (e.isControlDown() || e.isMetaDown()) {
                    action.run();
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();
        lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getTable().setup(lstCatalogCols);
        //TODO: Restore previously selected card
    }

    public void applyCurrentFilter() {
        // The main trick here is to apply a CardPrinted predicate
        // to the table. CardRules will lead to difficulties.

        List<Predicate<? super PaperCard>> cardPredicates = new ArrayList<Predicate<? super PaperCard>>();
        cardPredicates.add(Predicates.instanceOf(PaperCard.class));
        cardPredicates.add(SFilterUtil.buildColorAndTypeFilter(VWorkshopCatalog.SINGLETON_INSTANCE.getStatLabels()));
        cardPredicates.addAll(activePredicates);
        
        // apply current values in the range filters
        for (RangeTypes t : RangeTypes.values()) {
            if (activeRanges.contains(t)) {
                cardPredicates.add(SFilterUtil.buildIntervalFilter(VWorkshopCatalog.SINGLETON_INSTANCE.getSpinners(), t));
            }
        }
        
        // get the current contents of the search box
        cardPredicates.add(SFilterUtil.buildTextFilter(
                VWorkshopCatalog.SINGLETON_INSTANCE.getTxfSearch().getText(),
                0 != VWorkshopCatalog.SINGLETON_INSTANCE.getCbSearchMode().getSelectedIndex(),
                VWorkshopCatalog.SINGLETON_INSTANCE.getLblName().getSelected(),
                VWorkshopCatalog.SINGLETON_INSTANCE.getLblType().getSelected(),
                VWorkshopCatalog.SINGLETON_INSTANCE.getLblText().getSelected()));
        
        Predicate<PaperCard> cardFilter = Predicates.and(cardPredicates);
        
        // show packs and decks in the card shop according to the toggle setting
        // this is special-cased apart from the buildColorAndTypeFilter() above
        if (VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().getStatLabel(StatTypes.PACK).getSelected()) {
            List<Predicate<? super PaperCard>> itemPredicates = new ArrayList<Predicate<? super PaperCard>>();
            itemPredicates.add(cardFilter);
            itemPredicates.add(ItemPredicate.Presets.IS_PACK);
            itemPredicates.add(ItemPredicate.Presets.IS_DECK);
            cardFilter = Predicates.or(itemPredicates);
        }

        // Apply to table
        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().setFilterPredicate(cardFilter);
    }
    
    private boolean canSearch() {
        return !VWorkshopCatalog.SINGLETON_INSTANCE.getTxfSearch().getText().isEmpty() &&
                (VWorkshopCatalog.SINGLETON_INSTANCE.getLblName().getSelected() ||
                 VWorkshopCatalog.SINGLETON_INSTANCE.getLblType().getSelected() ||
                 VWorkshopCatalog.SINGLETON_INSTANCE.getLblText().getSelected());
    }
    
    private <T> boolean isActive(Set<T> activeSet, T key) {
        return activeSet.contains(key);
    }
    
    @SuppressWarnings("serial")
    private <T> void addRestriction(Pair<? extends JComponent, Predicate<PaperCard>> restriction, final Set<T> activeSet, final T key) {
        final Predicate<PaperCard> predicate = restriction.getRight();
        
        if (null != predicate && activePredicates.contains(predicate)) {
            return;
        }
        
        VWorkshopCatalog.SINGLETON_INSTANCE.addRestrictionWidget(restriction.getLeft(), new Command() {
            @Override
            public void run() {
                if (null != key) {
                    activeSet.remove(key);
                }
                if (null != predicate) {
                    activePredicates.remove(predicate);
                }
                applyCurrentFilter();
            }
        });

        if (null != key) {
            activeSet.add(key);
        }
        if (null != predicate) {
            activePredicates.add(predicate);
        }
        
        applyCurrentFilter();
    }

    private Pair<JPanel, Predicate<PaperCard>> buildRangeRestriction(RangeTypes t) {
        final Pair<FSpinner, FSpinner> s = VWorkshopCatalog.SINGLETON_INSTANCE.getSpinners().get(t);
        s.getLeft().setValue(0);
        s.getRight().setValue(10);
        
        // set focus to lower bound widget
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((JSpinner.DefaultEditor)s.getLeft().getEditor()).getTextField().requestFocusInWindow();
            }
        });
        
        return Pair.of(VWorkshopCatalog.SINGLETON_INSTANCE.buildRangeRestrictionWidget(t), null);
    }
    
    private String buildSearchRestrictionText(String text, boolean isInverse, boolean wantName, boolean wantType, boolean wantText) {
        StringBuilder sb = new StringBuilder();
        sb.append(isInverse ? "Without" : "Contains");
        sb.append(": '").append(text).append("' in:");
        if (wantName) { sb.append(" name,"); }
        if (wantType) { sb.append(" type,"); }
        if (wantText) { sb.append(" text,"); }
        sb.delete(sb.length() - 1, sb.length()); // chop off last comma
        
        return sb.toString();
    }

    private Pair<FLabel, Predicate<PaperCard>> buildSearchRestriction() {
        boolean isInverse =
                VWorkshopCatalog.SEARCH_MODE_INVERSE_INDEX == VWorkshopCatalog.SINGLETON_INSTANCE.getCbSearchMode().getSelectedIndex();
        String text = VWorkshopCatalog.SINGLETON_INSTANCE.getTxfSearch().getText();
        boolean wantName = VWorkshopCatalog.SINGLETON_INSTANCE.getLblName().getSelected();
        boolean wantType = VWorkshopCatalog.SINGLETON_INSTANCE.getLblType().getSelected();
        boolean wantText = VWorkshopCatalog.SINGLETON_INSTANCE.getLblText().getSelected();
        
        String shortText = buildSearchRestrictionText(text, isInverse, wantName, wantType, wantText);
        String fullText = null;
        if (25 < text.length()) {
            fullText = shortText;
            shortText = buildSearchRestrictionText(text.substring(0, 22) + "...",
                            isInverse, wantName, wantType, wantText);
        }
        
        VWorkshopCatalog.SINGLETON_INSTANCE.getTxfSearch().setText("");
        
        return Pair.of(
                VWorkshopCatalog.SINGLETON_INSTANCE.buildPlainRestrictionWidget(shortText, fullText),
                SFilterUtil.buildTextFilter(text, isInverse, wantName, wantType, wantText));
    }
    
    private Pair<FLabel, Predicate<PaperCard>> buildFormatRestriction(String displayName, GameFormat format, boolean allowReprints) {
        EditionCollection editions = Singletons.getMagicDb().getEditions();
        StringBuilder tooltip = new StringBuilder("<html>Sets:");
        
        int lastLen = 0;
        int lineLen = 0;
        
        // use HTML tooltips so we can insert line breaks
        List<String> sets = format.getAllowedSetCodes();
        if (null == sets || sets.isEmpty()) {
            tooltip.append(" All");
        } else {
            for (String code : sets) {
                // don't let a single line get too long
                if (50 < lineLen) {
                    tooltip.append("<br>");
                    lastLen += lineLen;
                    lineLen = 0;
                }
                
                CardEdition edition = editions.get(code);
                tooltip.append(" ").append(edition.getName()).append(" (").append(code).append("),");
                lineLen = tooltip.length() - lastLen;
            }
            
            // chop off last comma
            tooltip.delete(tooltip.length() - 1, tooltip.length());
            
            if (allowReprints) {
                tooltip.append("<br><br>Allowing identical cards from other sets");
            }
        }

        List<String> bannedCards = format.getBannedCardNames();
        if (null != bannedCards && !bannedCards.isEmpty()) {
            tooltip.append("<br><br>Banned:");
            lastLen += lineLen;
            lineLen = 0;
            
            for (String cardName : bannedCards) {
                // don't let a single line get too long
                if (50 < lineLen) {
                    tooltip.append("<br>");
                    lastLen += lineLen;
                    lineLen = 0;
                }
                
                tooltip.append(" ").append(cardName).append(";");
                lineLen = tooltip.length() - lastLen;
            }
            
            // chop off last semicolon
            tooltip.delete(tooltip.length() - 1, tooltip.length());
        }
        tooltip.append("</html>");
        
        return Pair.of(
                VWorkshopCatalog.SINGLETON_INSTANCE.buildPlainRestrictionWidget(displayName, tooltip.toString()),
                allowReprints ? format.getFilterRules() : format.getFilterPrinted());
    }
    
    private Pair<FLabel, Predicate<PaperCard>> buildSetRestriction(String displayName, List<String> setCodes, boolean allowReprints) {
        return buildFormatRestriction(displayName, new GameFormat(null, setCodes, null), allowReprints);
    }
}

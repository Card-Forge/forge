package forge.gui.deckeditor.controllers;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Command;
import forge.Singletons;
import forge.card.CardEdition;
import forge.card.EditionCollection;
import forge.deck.DeckBase;
import forge.game.GameFormat;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.SFilterUtil;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCardCatalog.RangeTypes;
import forge.gui.framework.ICDoc;
import forge.gui.home.quest.DialogChooseSets;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSpinner;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.quest.QuestWorld;
import forge.quest.data.GameFormatQuest;
import forge.util.Pair;

/** 
 * Controls the "card catalog" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardCatalog implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final Set<Predicate<CardPrinted>> activePredicates = new HashSet<Predicate<CardPrinted>>();
    private final Set<GameFormat> activeFormats = new HashSet<GameFormat>();
    private final Set<QuestWorld> activeWorlds = new HashSet<QuestWorld>();
    private final Set<RangeTypes> activeRanges = EnumSet.noneOf(RangeTypes.class);
    
    private boolean disableFiltering = false;

    private CCardCatalog() {
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
        // Add/remove buttons (refresh analysis on add)
        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd().setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().addCard();
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getTableCatalog().getTable().requestFocusInWindow();
                CStatistics.SINGLETON_INSTANCE.update();
                CProbabilities.SINGLETON_INSTANCE.update();
            }
        });
        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4().setCommand(new Command() {
            @Override
            public void execute() {
                final InventoryItem item = CDeckEditorUI.SINGLETON_INSTANCE
                        .getCurrentEditorController().getTableCatalog().getSelectedCard();

                for (int i = 0; i < 4; i++) {
                    if (item != null && item.equals(CDeckEditorUI.SINGLETON_INSTANCE
                            .getCurrentEditorController().getTableCatalog().getSelectedCard())) {
                        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().addCard();
                    }
                }
                CStatistics.SINGLETON_INSTANCE.update();
                CProbabilities.SINGLETON_INSTANCE.update();
            }
        });
        
        final Command updateFilterCommand = new Command() {
            @Override
            public void execute() {
                if (!disableFiltering) {
                    applyCurrentFilter();
                }
            }
        };

        for (FLabel statLabel : VCardCatalog.SINGLETON_INSTANCE.getStatLabels().values()) {
            statLabel.setCommand(updateFilterCommand);
        }

        VCardCatalog.SINGLETON_INSTANCE.getStatLabel(SEditorUtil.StatTypes.TOTAL).setCommand(new Command() {
            private boolean lastToggle = true;
            
            @Override
            public void execute() {
                disableFiltering = true;
                lastToggle = !lastToggle;
                for (SEditorUtil.StatTypes s : SEditorUtil.StatTypes.values()) {
                    if (SEditorUtil.StatTypes.TOTAL != s) {
                        VCardCatalog.SINGLETON_INSTANCE.getStatLabel(s).setSelected(lastToggle);
                    }
                }
                disableFiltering = false;
                applyCurrentFilter();
            }
        });
        
        // assemble add restriction menu
        VCardCatalog.SINGLETON_INSTANCE.getBtnAddRestriction().setCommand(new Command() {
            @Override
            public void execute() {
                JPopupMenu popup = new JPopupMenu("Popup");
                addMenuItem(popup, "Current text search", canSearch(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                        new Command() {
                    @Override
                    public void execute() {
                        addRestriction(buildSearchRestriction(), null, null);
                    }
                });
                JMenu fmt = new JMenu("Format");
                for (final GameFormat f : Singletons.getModel().getFormats()) {
                    addMenuItem(fmt, f.getName(), !isActive(activeFormats, f), null, new Command() {
                        @Override
                        public void execute() {
                            addRestriction(buildFormatRestriction(f.toString(), f, true), activeFormats, f);
                        }
                    });
                }
                popup.add(fmt);
                addMenuItem(popup, "Edition (set)...", true, null, new Command() {
                    @Override
                    public void execute() {
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
                    addMenuItem(range, t.toLabelString() + " restriction", !isActive(activeRanges, t), null, new Command() {
                        @Override
                        public void execute() {
                            addRestriction(buildRangeRestriction(t), activeRanges, t);
                        }
                    });
                }
                popup.add(range);
                JMenu world = new JMenu("Quest world");
                for (final QuestWorld w : Singletons.getModel().getWorlds()) {
                    addMenuItem(world, w.getName(), !isActive(activeWorlds, w), null, new Command() {
                        @Override
                        public void execute() {
                            addRestriction(buildWorldRestriction(w), activeWorlds, w);
                        }
                    });
                }
                popup.add(world);
                popup.show(VCardCatalog.SINGLETON_INSTANCE.getBtnAddRestriction(), 0, 0);
            }
        });
        
        VCardCatalog.SINGLETON_INSTANCE.getCbSearchMode().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                applyCurrentFilter();
            }
        });
        
        VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        if (canSearch()) {
                            addRestriction(buildSearchRestriction(), null, null);
                        }
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                applyCurrentFilter();
            }
        });
        
        VCardCatalog.SINGLETON_INSTANCE.getLblName().setCommand(updateFilterCommand);
        VCardCatalog.SINGLETON_INSTANCE.getLblType().setCommand(updateFilterCommand);
        VCardCatalog.SINGLETON_INSTANCE.getLblText().setCommand(updateFilterCommand);
        
        // ensure mins can's exceed maxes and maxes can't fall below mins
        for (Pair<FSpinner, FSpinner> sPair : VCardCatalog.SINGLETON_INSTANCE.getSpinners().values()) {
            final FSpinner min = sPair.a;
            final FSpinner max = sPair.b;
            
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    @SuppressWarnings("unchecked")
    public void applyCurrentFilter() {
        // The main trick here is to apply a CardPrinted predicate
        // to the table. CardRules will lead to difficulties.

        List<Predicate<? super CardPrinted>> cardPredicates = new ArrayList<Predicate<? super CardPrinted>>();
        cardPredicates.add(Predicates.instanceOf(CardPrinted.class));
        cardPredicates.add(SFilterUtil.buildColorAndTypeFilter(VCardCatalog.SINGLETON_INSTANCE.getStatLabels()));
        cardPredicates.addAll(activePredicates);
        
        // apply current values in the range filters
        for (RangeTypes t : RangeTypes.values()) {
            if (activeRanges.contains(t)) {
                cardPredicates.add(SFilterUtil.buildIntervalFilter(VCardCatalog.SINGLETON_INSTANCE.getSpinners(), t));
            }
        }
        
        // get the current contents of the search box
        cardPredicates.add(SFilterUtil.buildTextFilter(
                VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().getText(),
                0 != VCardCatalog.SINGLETON_INSTANCE.getCbSearchMode().getSelectedIndex(),
                VCardCatalog.SINGLETON_INSTANCE.getLblName().isSelected(),
                VCardCatalog.SINGLETON_INSTANCE.getLblType().isSelected(),
                VCardCatalog.SINGLETON_INSTANCE.getLblText().isSelected()));
        
        Predicate<? super CardPrinted> cardFilter = Predicates.and(cardPredicates);
        
        // Until this is filterable, always show packs and decks in the card shop.
        List<Predicate<? super CardPrinted>> itemPredicates = new ArrayList<Predicate<? super CardPrinted>>();
        itemPredicates.add(cardFilter);
        itemPredicates.add(ItemPredicate.Presets.IS_PACK);
        itemPredicates.add(ItemPredicate.Presets.IS_DECK);
        Predicate<CardPrinted> filter = Predicates.or(itemPredicates);

        // Apply to table
        // TODO: is there really no way to make this type safe?
        ((ACEditorBase<CardPrinted, DeckBase>)CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController())
            .getTableCatalog().setFilter(filter);
    }
    
    private boolean canSearch() {
        return !VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().getText().isEmpty() &&
                (VCardCatalog.SINGLETON_INSTANCE.getLblName().isSelected() ||
                 VCardCatalog.SINGLETON_INSTANCE.getLblType().isSelected() ||
                 VCardCatalog.SINGLETON_INSTANCE.getLblText().isSelected());
    }
    
    private <T> boolean isActive(Set<T> activeSet, T key) {
        return activeSet.contains(key);
    }
    
    private JMenuItem createMenuItem(String label, boolean enabled, KeyStroke accelerator, final Command onClick) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (null != onClick) {
                    onClick.execute();
                }
            }
        });
        item.setEnabled(enabled);
        item.setAccelerator(accelerator);
        return item;
    }
    
    private void addMenuItem(JPopupMenu parent, String label, boolean enabled, KeyStroke accelerator, Command onClick) {
        parent.add(createMenuItem(label, enabled, accelerator, onClick));
    }
    
    private void addMenuItem(JMenuItem parent, String label, boolean enabled, KeyStroke accelerator, Command onClick) {
        parent.add(createMenuItem(label, enabled, accelerator, onClick));
    }
    
    @SuppressWarnings("serial")
    private <T> void addRestriction(Pair<JComponent, Predicate<CardPrinted>> restriction, final Set<T> activeSet, final T key) {
        final Predicate<CardPrinted> predicate = restriction.b;
        
        if (null != predicate && activePredicates.contains(predicate)) {
            return;
        }
        
        VCardCatalog.SINGLETON_INSTANCE.addRestrictionWidget(restriction.a, new Command() {
            @Override
            public void execute() {
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

    private Pair<JComponent, Predicate<CardPrinted>> buildRangeRestriction(RangeTypes t) {
        Pair<FSpinner, FSpinner> s = VCardCatalog.SINGLETON_INSTANCE.getSpinners().get(t);
        s.a.setValue(0);
        s.b.setValue(10);
        
        return new Pair<JComponent, Predicate<CardPrinted>>(
                VCardCatalog.SINGLETON_INSTANCE.buildRangeRestrictionWidget(t), null);
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

    private Pair<JComponent, Predicate<CardPrinted>> buildSearchRestriction() {
        boolean isInverse =
                VCardCatalog.SEARCH_MODE_INVERSE_INDEX == VCardCatalog.SINGLETON_INSTANCE.getCbSearchMode().getSelectedIndex();
        String text = VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().getText();
        boolean wantName = VCardCatalog.SINGLETON_INSTANCE.getLblName().getSelected();
        boolean wantType = VCardCatalog.SINGLETON_INSTANCE.getLblType().getSelected();
        boolean wantText = VCardCatalog.SINGLETON_INSTANCE.getLblText().getSelected();
        
        String shortText = buildSearchRestrictionText(text, isInverse, wantName, wantType, wantText);
        String fullText = null;
        if (25 < text.length()) {
            fullText = shortText;
            shortText = buildSearchRestrictionText(text.substring(0, 22) + "...",
                            isInverse, wantName, wantType, wantText);
        }
        
        VCardCatalog.SINGLETON_INSTANCE.getTxfSearch().setText("");
        
        return new Pair<JComponent, Predicate<CardPrinted>>(
                VCardCatalog.SINGLETON_INSTANCE.buildPlainRestrictionWidget(shortText, fullText),
                SFilterUtil.buildTextFilter(text, isInverse, wantName, wantType, wantText));
    }
    
    private Pair<JComponent, Predicate<CardPrinted>> buildFormatRestriction(String displayName, GameFormat format, boolean allowReprints) {
        EditionCollection editions = Singletons.getModel().getEditions();
        StringBuilder tooltip = new StringBuilder("<html>Sets:");
        
        int lastLen = 0;
        int lineLen = 0;
        
        // use HTML tooltips so we can insert line breaks
        List<String> sets = null == format ? null : format.getAllowedSetCodes();
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

        List<String> bannedCards = null == format ? null : format.getBannedCardNames();
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
        
        return new Pair<JComponent, Predicate<CardPrinted>>(
                VCardCatalog.SINGLETON_INSTANCE.buildPlainRestrictionWidget(displayName, tooltip.toString()),
                allowReprints ? format.getFilterRules() : format.getFilterPrinted());
    }
    
    private Pair<JComponent, Predicate<CardPrinted>> buildSetRestriction(String displayName, List<String> setCodes, boolean allowReprints) {
        return buildFormatRestriction(displayName, new GameFormat(null, setCodes, null), allowReprints);
    }

    private Pair<JComponent, Predicate<CardPrinted>> buildWorldRestriction(QuestWorld world) {
        GameFormatQuest format = world.getFormat();
        if (null == format) {
            // assumes that no world other than the main world will have a null format
            format = Singletons.getModel().getQuest().getMainFormat();
        }
        return buildFormatRestriction(world.getName(), format, true);
    }
}

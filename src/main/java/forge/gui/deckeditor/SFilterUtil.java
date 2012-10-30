package forge.gui.deckeditor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardEdition;
import forge.card.CardRulesPredicates;
import forge.card.CardRules;
import forge.game.GameFormat;
import forge.gui.WrapLayout;
import forge.gui.deckeditor.views.VFilters;
import forge.item.CardPrinted;
import forge.util.ComparableOp;
import forge.util.PredicateString.StringOp;

/** 
 * Static factory; holds blocks of form elements and predicates
 * which are used in various editing environments.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SFilterUtil {
    /** An enum to reference checkbox objects in their respective maps. */
    private enum FilterProperty { /** */
        BLACK, /** */
        BLUE, /** */
        COLORLESS, /** */
        GREEN, /** */
        MULTICOLOR, /** */
        RED, /** */
        WHITE, /** */

        ARTIFACT, /** */
        CREATURE, /** */
        ENCHANTMENT, /** */
        INSTANT, /** */
        LAND, /** */
        PLANESWALKER, /** */
        SORCERY
    }

    private static final Map<FilterProperty, ChbPnl> MAP_COLOR_CHECKBOXES =
            new HashMap<FilterProperty, ChbPnl>();

    private static final Map<FilterProperty, ChbPnl> MAP_TYPE_CHECKBOXES =
            new HashMap<FilterProperty, ChbPnl>();

    /**

     */
    private static boolean preventFiltering = false;

    /**
     * This will prevent a filter event on a checkbox state change.
     * It's used for programmatic changes to the checkboxes when rebuilding
     * the filter each time is expensive.
     * 
     * @return boolean &emsp; true if filtering is prevented
     */
    public static boolean isFilteringPrevented() {
        return preventFiltering;
    }

    /**
     * This will prevent a filter event on a checkbox state change.
     * It's used for programmatic changes to the checkboxes when rebuilding
     * the filter each time is expensive.
     * 
     * @param bool0 &emsp; true to prevent filtering
     */
    public static void setPreventFiltering(final boolean bool0) {
        preventFiltering = bool0;
    }

    /**
     * Fills and returns a JPanel with checkboxes for color filter set.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public static JPanel populateColorFilters() {
        MAP_COLOR_CHECKBOXES.clear();

        MAP_COLOR_CHECKBOXES.put(FilterProperty.BLACK,
                new ChbPnl(SEditorUtil.ICO_BLACK.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.MULTICOLOR,
                new ChbPnl(SEditorUtil.ICO_MULTI.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.BLUE,
                new ChbPnl(SEditorUtil.ICO_BLUE.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.BLACK,
                new ChbPnl(SEditorUtil.ICO_BLACK.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.GREEN,
                new ChbPnl(SEditorUtil.ICO_GREEN.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.RED,
                new ChbPnl(SEditorUtil.ICO_RED.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.WHITE,
                new ChbPnl(SEditorUtil.ICO_WHITE.getImage()));
        MAP_COLOR_CHECKBOXES.put(FilterProperty.COLORLESS,
                new ChbPnl(SEditorUtil.ICO_COLORLESS.getImage()));


        final JPanel pnl = new JPanel(new WrapLayout(SwingConstants.CENTER, 10, 5));
        pnl.setOpaque(false);

        for (FilterProperty p : MAP_COLOR_CHECKBOXES.keySet()) {
            pnl.add(MAP_COLOR_CHECKBOXES.get(p));
        }
        return pnl;
    }

    /**
     * Fills and returns a JPanel with checkboxes for color filter set.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public static JPanel populateTypeFilters() {
        MAP_TYPE_CHECKBOXES.clear();

        MAP_TYPE_CHECKBOXES.put(FilterProperty.ARTIFACT,
                new ChbPnl(SEditorUtil.ICO_ARTIFACT.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.CREATURE,
                new ChbPnl(SEditorUtil.ICO_CREATURE.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.ENCHANTMENT,
                new ChbPnl(SEditorUtil.ICO_ENCHANTMENT.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.INSTANT,
                new ChbPnl(SEditorUtil.ICO_INSTANT.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.LAND,
                new ChbPnl(SEditorUtil.ICO_LAND.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.PLANESWALKER,
                new ChbPnl(SEditorUtil.ICO_PLANESWALKER.getImage()));
        MAP_TYPE_CHECKBOXES.put(FilterProperty.SORCERY,
                new ChbPnl(SEditorUtil.ICO_SORCERY.getImage()));


        final JPanel pnl = new JPanel(new WrapLayout(SwingConstants.CENTER, 10, 5));
        pnl.setOpaque(false);

        for (FilterProperty p : MAP_TYPE_CHECKBOXES.keySet()) {
            pnl.add(MAP_TYPE_CHECKBOXES.get(p));
        }
        return pnl;
    }

    /** Turns all type checkboxes off or on.
     * @param select0 &emsp; boolean */
    public static void toggleTypeCheckboxes(final boolean select0) {
        for (FilterProperty p : MAP_TYPE_CHECKBOXES.keySet()) {
           MAP_TYPE_CHECKBOXES.get(p).getCheckBox().setSelected(select0);
        }
    }

    /** Turns all type checkboxes off or on.
     * @param select0 &emsp; boolean */
    public static void toggleColorCheckboxes(final boolean select0) {
        for (FilterProperty p : MAP_COLOR_CHECKBOXES.keySet()) {
           MAP_COLOR_CHECKBOXES.get(p).getCheckBox().setSelected(select0);
        }
    }

    /**
     * Assembles checkboxes for color and returns a filter predicate.
     * <br><br>
     * Handles "multicolor" label, which is quite tricky.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildColorFilter() {
        if (MAP_COLOR_CHECKBOXES.isEmpty()) { return Predicates.alwaysTrue(); }

        final List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();

        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.BLACK).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_BLACK); }
        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.BLUE).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_BLUE); }
        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.GREEN).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_GREEN); }
        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.RED).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_RED); }
        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.WHITE).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_WHITE); }
        if (MAP_COLOR_CHECKBOXES.get(FilterProperty.COLORLESS).getCheckBox().isSelected()) { colors.add(CardRulesPredicates.Presets.IS_COLORLESS); }
        
        final Predicate<CardRules> preColors = colors.size() == 6 ? null : Predicates.or(colors);
        
        
        boolean wantMulticolor = MAP_COLOR_CHECKBOXES.get(FilterProperty.MULTICOLOR).getCheckBox().isSelected(); 
        final Predicate<CardRules> preExceptMulti = wantMulticolor ? null : Predicates.not(CardRulesPredicates.Presets.IS_MULTICOLOR); 

        Predicate<CardRules> preFinal = colors.isEmpty() && wantMulticolor ? CardRulesPredicates.Presets.IS_MULTICOLOR : optimizedAnd(preExceptMulti, preColors);
        
        if ( null == preFinal)
            return Predicates.alwaysTrue();
        
        return Predicates.compose(preFinal, CardPrinted.FN_GET_RULES);
    }

    /**
     * Filters the set/format combo box.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildSetAndFormatFilter() {
        // Set/Format filter
        JComboBox cbox = VFilters.SINGLETON_INSTANCE.getCbxSets(); 
        if (cbox.getSelectedIndex() == 0) {
            return Predicates.alwaysTrue();
        }

        final Object selected = cbox.getSelectedItem();
        final Predicate<CardPrinted> filter;
        if (selected instanceof CardEdition) {
            filter = CardPrinted.Predicates.printedInSets(((CardEdition) selected).getCode());
        } else {
            filter = ((GameFormat) selected).getFilterRules();
        }

        return filter;
    }

    /**
     * Assembles checkboxes for type and returns a filter predicate.
     *
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildTypeFilter() {
        if (MAP_TYPE_CHECKBOXES.isEmpty()) { return Predicates.alwaysTrue(); }

        final List<Predicate<CardRules>> ors = new ArrayList<Predicate<CardRules>>();
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.ARTIFACT).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_ARTIFACT); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.CREATURE).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_CREATURE); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.ENCHANTMENT).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_ENCHANTMENT); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.INSTANT).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_INSTANT); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.LAND).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_LAND); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.PLANESWALKER).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_PLANESWALKER); }
        if (MAP_TYPE_CHECKBOXES.get(FilterProperty.SORCERY).getCheckBox().isSelected()) { ors.add(CardRulesPredicates.Presets.IS_SORCERY); }

        if (ors.size() == 7)
            return Predicates.alwaysTrue();
        
        return Predicates.compose(Predicates.or(ors), CardPrinted.FN_GET_RULES);
    }

    /**
     * Validates text field input (from txfContains and txfWithout),
     * then assembles AND and NOT predicates accordingly, ANDs
     * together, and returns.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildTextFilter() {
        Predicate<CardRules> filterAnd = null;
        Predicate<CardRules> filterNot = null;

        final String strContains = VFilters.SINGLETON_INSTANCE.getTxfContains().getText();
        final String strWithout = VFilters.SINGLETON_INSTANCE.getTxfWithout().getText();

        final boolean useName = VFilters.SINGLETON_INSTANCE.getChbTextName().isSelected();
        final boolean useType = VFilters.SINGLETON_INSTANCE.getChbTextType().isSelected();
        final boolean useText = VFilters.SINGLETON_INSTANCE.getChbTextText().isSelected();

        if (!strContains.isEmpty()) {
            final String[] splitContains = strContains
                    .replaceAll(",", "")
                    .replaceAll("  ", " ")
                    .toLowerCase().split(" ");

            final List<Predicate<CardRules>> ands = new ArrayList<Predicate<CardRules>>();

            for (final String s : splitContains) {
                final List<Predicate<CardRules>> subands = new ArrayList<Predicate<CardRules>>();

                if (useName) { subands.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s)); }
                if (useType) { subands.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
                // rules cannot compare in ignore-case way
                if (useText) { subands.add(CardRulesPredicates.rules(StringOp.CONTAINS, s)); }

                ands.add(Predicates.or(subands));
            }
            filterAnd = Predicates.and(ands);
        }

        if (!strWithout.isEmpty()) {
            final String[] splitWithout = strWithout
                    .replaceAll("  ", " ")
                    .replaceAll(",", "")
                    .toLowerCase().split(" ");

            final List<Predicate<CardRules>> nots = new ArrayList<Predicate<CardRules>>();

            for (final String s : splitWithout) {
                final List<Predicate<CardRules>> subnots = new ArrayList<Predicate<CardRules>>();

                if (useName) { subnots.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s)); }
                if (useType) { subnots.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
                // rules cannot compare in ignore-case way
                if (useText) { subnots.add(CardRulesPredicates.rules(StringOp.CONTAINS, s)); }

                nots.add(Predicates.or(subnots));
            }
            filterNot = Predicates.not(Predicates.or(nots));
        }
        Predicate<CardRules> preResult = optimizedAnd(filterAnd, filterNot);
        if ( preResult == null ) return Predicates.alwaysTrue();
        return Predicates.compose(preResult, CardPrinted.FN_GET_RULES);
    }

    private static Predicate<CardRules> getCardRulesFieldPredicate(String min, String max, CardRulesPredicates.LeafNumber.CardField field) {
        boolean hasMin = !("*".equals(min));
        boolean hasMax = !("10+".equals(max));
        
        Predicate<CardRules> pMin = !hasMin ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.GT_OR_EQUAL, Integer.valueOf(min));
        Predicate<CardRules> pMax = !hasMax ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.LT_OR_EQUAL, Integer.valueOf(max));
        
        return optimizedAnd(pMin, pMax);
    }
    
    private static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2)
    {
        return p1 == null ? p2 : ( p2 == null ? p1 : Predicates.and(p1, p2) );
    }
    
    /**
     * Validates combo box input, assembles predicate filters for each case,
     * stacks them all together, and returns the predicate.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildIntervalFilter() {
        final VFilters view = VFilters.SINGLETON_INSTANCE;

        // Must include -1 so non-creatures are included by default.
        Predicate<CardRules> preToughness = getCardRulesFieldPredicate(view.getCbxTLow().getSelectedItem().toString(), view.getCbxTHigh().getSelectedItem().toString(), CardRulesPredicates.LeafNumber.CardField.TOUGHNESS);
        Predicate<CardRules> prePower = getCardRulesFieldPredicate(view.getCbxPLow().getSelectedItem().toString(), view.getCbxPHigh().getSelectedItem().toString(), CardRulesPredicates.LeafNumber.CardField.POWER);
        Predicate<CardRules> preCMC = getCardRulesFieldPredicate(view.getCbxCMCLow().getSelectedItem().toString(), view.getCbxCMCHigh().getSelectedItem().toString(), CardRulesPredicates.LeafNumber.CardField.CMC);
        
        Predicate<CardRules> preCreature = optimizedAnd(preToughness, prePower);
        preCreature = preCreature == null ? null : Predicates.and( preCreature, CardRulesPredicates.Presets.IS_CREATURE ); 
        
        Predicate<CardRules> preFinal = optimizedAnd(preCMC, preCreature);
        if (preFinal == null)
            return Predicates.alwaysTrue();
        else
            return Predicates.compose(preFinal, CardPrinted.FN_GET_RULES);
    }

    //========== Custom class handling

    /**
     * A panel with a checkbox and an icon, which will toggle the
     * checkbox when anywhere in the panel is clicked.
     */
    @SuppressWarnings("serial")
    private static class ChbPnl extends JPanel implements ItemListener {
        private final JCheckBox checkBox = new JCheckBox();
        private final Image img;

        public ChbPnl(final Image img0) {
            super();
            this.img = img0;
            this.setOpaque(false);
            checkBox.setBorder(new EmptyBorder(0, 20, 0, 0));
            checkBox.setOpaque(false);
            checkBox.setSelected(true);
            checkBox.addItemListener(this);
            add(checkBox);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent me) {
                    checkBox.doClick();
                }
            });
        }

        public JCheckBox getCheckBox() {
            return this.checkBox;
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, null);
        }

        /* (non-Javadoc)
         * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
         */
        @Override
        public void itemStateChanged(final ItemEvent arg0) {
            if (!preventFiltering) {
                VFilters.SINGLETON_INSTANCE.getLayoutControl().buildFilter();
            }
        }
    }
}

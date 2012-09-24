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
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.game.GameFormat;
import forge.gui.WrapLayout;
import forge.gui.deckeditor.controllers.CFilters;
import forge.gui.deckeditor.views.VFilters;
import forge.item.CardPrinted;
import forge.util.closures.Predicate;
import forge.util.closures.PredicateInteger.ComparableOp;
import forge.util.closures.PredicateString.StringOp;

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
           ((ChbPnl) MAP_TYPE_CHECKBOXES.get(p)).getCheckBox().setSelected(select0);
        }
    }

    /** Turns all type checkboxes off or on.
     * @param select0 &emsp; boolean */
    public static void toggleColorCheckboxes(final boolean select0) {
        for (FilterProperty p : MAP_COLOR_CHECKBOXES.keySet()) {
           ((ChbPnl) MAP_COLOR_CHECKBOXES.get(p)).getCheckBox().setSelected(select0);
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
        if (MAP_COLOR_CHECKBOXES.isEmpty()) { return Predicate.getTrue(CardPrinted.class); }

        final List<Predicate<CardRules>> ors = new ArrayList<Predicate<CardRules>>();
        JCheckBox chbTemp;

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.BLACK)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_BLACK); }

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.BLUE)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_BLUE); }

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.GREEN)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_GREEN); }

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.RED)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_RED); }

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.WHITE)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_WHITE); }

        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.COLORLESS)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_COLORLESS); }

        // Multi-colored needs special XOR treatment, since "not multi" when OR-ed
        // with any other of its colors except colorless, will return true.
        // Careful when changing this.
        chbTemp = ((ChbPnl) MAP_COLOR_CHECKBOXES.get(FilterProperty.MULTICOLOR)).getCheckBox();
        final Predicate<CardPrinted> preMulti;
        if (chbTemp.isSelected()) {
            preMulti = Predicate.getTrue(CardPrinted.class);
        }
        else {
            preMulti = Predicate.not(Predicate.brigde(
                    CardRules.Predicates.Presets.IS_MULTICOLOR, CardPrinted.FN_GET_RULES));
        }

        final Predicate<CardPrinted> preColors =
                Predicate.brigde(Predicate.or(ors), CardPrinted.FN_GET_RULES);

        // Corner case: if multi is checked, and the rest are empty, AND won't work.
        // This still doesn't work perfectly :/
        boolean allEmptyExceptMulti = true;
        for (FilterProperty p : MAP_COLOR_CHECKBOXES.keySet()) {
            if (p.equals(FilterProperty.MULTICOLOR)) { continue; }
            if (((ChbPnl) MAP_COLOR_CHECKBOXES.get(p)).getCheckBox().isSelected()) {
                allEmptyExceptMulti = false;
                break;
            }
         }

        if (allEmptyExceptMulti) {
            return Predicate.brigde(
                CardRules.Predicates.Presets.IS_MULTICOLOR, CardPrinted.FN_GET_RULES);
        }
        else {
            return Predicate.and(preColors, preMulti);
        }
    }

    /**
     * Filters the set/format combo box.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildSetAndFormatFilter() {
        // Set/Format filter
        if (VFilters.SINGLETON_INSTANCE.getCbxSets().getSelectedIndex() == 0) {
            return Predicate.getTrue(CardPrinted.class);
        }

        final Object selected = VFilters.SINGLETON_INSTANCE.getCbxSets().getSelectedItem();
        final Predicate<CardPrinted> filter;
        if (selected instanceof CardEdition) {
            filter = CardPrinted.Predicates.printedInSets(((CardEdition) selected).getCode());
        }
        else {
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
        if (MAP_TYPE_CHECKBOXES.isEmpty()) { return Predicate.getTrue(CardPrinted.class); }

        final List<Predicate<CardRules>> ors = new ArrayList<Predicate<CardRules>>();
        JCheckBox chbTemp;

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.ARTIFACT)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_ARTIFACT); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.CREATURE)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_CREATURE); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.ENCHANTMENT)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_ENCHANTMENT); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.INSTANT)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_INSTANT); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.LAND)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_LAND); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.PLANESWALKER)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_PLANESWALKER); }

        chbTemp = ((ChbPnl) MAP_TYPE_CHECKBOXES.get(FilterProperty.SORCERY)).getCheckBox();
        if (chbTemp.isSelected()) { ors.add(CardRules.Predicates.Presets.IS_SORCERY); }

        return Predicate.brigde(Predicate.or(ors), CardPrinted.FN_GET_RULES);
    }

    /**
     * Validates text field input (from txfContains and txfWithout),
     * then assembles AND and NOT predicates accordingly, ANDs
     * together, and returns.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildTextFilter() {
        Predicate<CardPrinted> filterAnd = Predicate.getTrue(CardPrinted.class);
        Predicate<CardPrinted> filterNot = Predicate.getTrue(CardPrinted.class);

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

            final List<Predicate<CardPrinted>> ands = new ArrayList<Predicate<CardPrinted>>();

            for (final String s : splitContains) {
                final List<Predicate<CardPrinted>> subands = new ArrayList<Predicate<CardPrinted>>();

                if (useName) { subands.add(Predicate.brigde(CardRules.Predicates.name(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }
                if (useType) { subands.add(Predicate.brigde(CardRules.Predicates.joinedType(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }
                if (useText) { subands.add(Predicate.brigde(CardRules.Predicates.rules(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }

                ands.add(Predicate.or(subands));
            }

            filterAnd = Predicate.and(ands);
        }

        if (!strWithout.isEmpty()) {
            final String[] splitWithout = strWithout
                    .replaceAll("  ", " ")
                    .replaceAll(",", "")
                    .toLowerCase().split(" ");

            final List<Predicate<CardPrinted>> nots = new ArrayList<Predicate<CardPrinted>>();

            for (final String s : splitWithout) {
                final List<Predicate<CardPrinted>> subnots = new ArrayList<Predicate<CardPrinted>>();

                if (useName) { subnots.add(Predicate.brigde(CardRules.Predicates.name(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }
                if (useType) { subnots.add(Predicate.brigde(CardRules.Predicates.joinedType(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }
                if (useText) { subnots.add(Predicate.brigde(CardRules.Predicates.rules(
                        StringOp.CONTAINS, s), CardPrinted.FN_GET_RULES)); }

                nots.add(Predicate.or(subnots));
            }

            filterNot = Predicate.not(Predicate.or(nots));
        }

        return Predicate.and(filterAnd, filterNot);
    }

    /**
     * Validates combo box input, assembles predicate filters for each case,
     * stacks them all together, and returns the predicate.
     * 
     * @return Predicate<CardPrinted>
     */
    public static Predicate<CardPrinted> buildIntervalFilter() {
        final VFilters view = VFilters.SINGLETON_INSTANCE;
        Predicate<CardPrinted> filter = Predicate.getTrue(CardPrinted.class);

        // Must include -1 so non-creatures are included by default.
        final int plow = view.getCbxPLow().getSelectedItem().toString().equals("*")
                ? -1 : Integer.valueOf(view.getCbxPLow().getSelectedItem().toString());
        final int tlow = view.getCbxTLow().getSelectedItem().toString().equals("*")
                ? -1 : Integer.valueOf(view.getCbxTLow().getSelectedItem().toString());
        final int clow = view.getCbxCMCLow().getSelectedItem().toString().equals("*")
                ? -1 : Integer.valueOf(view.getCbxCMCLow().getSelectedItem().toString());

        // If a power, toughness, or CMC is higher than 100, that's bad.
        final int phigh = view.getCbxPHigh().getSelectedItem().toString().equals("10+")
                ? 100 : Integer.valueOf(view.getCbxPHigh().getSelectedItem().toString());
        final int thigh = view.getCbxTHigh().getSelectedItem().toString().equals("10+")
                ? 100 : Integer.valueOf(view.getCbxTHigh().getSelectedItem().toString());
        final int chigh = view.getCbxCMCHigh().getSelectedItem().toString().equals("10+")
                ? 100 : Integer.valueOf(view.getCbxCMCHigh().getSelectedItem().toString());

        // Assemble final predicates
        final Predicate<CardPrinted> prePower;
        final Predicate<CardPrinted> preToughness;
        final Predicate<CardPrinted> preCMC;

        // Power: CardRules returns null if no power, which means extra
        // filtering must be applied to allow all cards to be shown if * is chosen.
        // (Without this, lands and such would be filtered out by default.)
        if (plow > phigh) { prePower = Predicate.getFalse(CardPrinted.class); }
        else {
            // If * is selected in the combo box, cards without power
            // will be included in the filter.
            final Predicate<CardPrinted> preNotCreature;
            if (plow == -1) {
                preNotCreature = Predicate.not(
                    Predicate.brigde(CardRules.Predicates.Presets.IS_CREATURE,
                            CardPrinted.FN_GET_RULES));
            }
            // Otherwise, if 0 or higher is selected, cards without power
            // are excluded.
            else {
                preNotCreature = Predicate.getFalse(CardPrinted.class);
            }

            final Predicate<CardPrinted> prePowerTemp = Predicate.and(
            Predicate.brigde(CardRules.Predicates.power(
                    ComparableOp.GT_OR_EQUAL, plow), CardPrinted.FN_GET_RULES),
            Predicate.brigde(CardRules.Predicates.power(
                    ComparableOp.LT_OR_EQUAL, phigh), CardPrinted.FN_GET_RULES));

            prePower = Predicate.or(preNotCreature, prePowerTemp);
        }

        // Toughness: CardRules returns null if no toughness, which means extra
        // filtering must be applied to allow all cards to be shown if * is chosen.
        // (Without this, lands and such would be filtered out by default.)
        if (tlow > thigh) { preToughness = Predicate.getFalse(CardPrinted.class); }
        else {
            // If * is selected in the combo box, cards without toughness
            // will be included in the filter.
            final Predicate<CardPrinted> preNotCreature;
            if (tlow == -1) {
                preNotCreature = Predicate.not(
                    Predicate.brigde(CardRules.Predicates.Presets.IS_CREATURE,
                            CardPrinted.FN_GET_RULES));
            }
            // Otherwise, if 0 or higher is selected, cards without toughness
            // are excluded.
            else {
                preNotCreature = Predicate.getFalse(CardPrinted.class);
            }

            final Predicate<CardPrinted> preToughnessTemp = Predicate.and(
                Predicate.brigde(CardRules.Predicates.toughness(
                        ComparableOp.GT_OR_EQUAL, tlow), CardPrinted.FN_GET_RULES),
                Predicate.brigde(CardRules.Predicates.toughness(
                        ComparableOp.LT_OR_EQUAL, thigh), CardPrinted.FN_GET_RULES));

            preToughness = Predicate.or(preNotCreature, preToughnessTemp);
        }

        // CMC, thankfully, will return -1 if the card doesn't have a CMC,
        // so it can be compared directly against the low value, and non-CMC
        // cards will still be included if * is chosen.
        if (clow > chigh) { preCMC = Predicate.getFalse(CardPrinted.class); }
        else {
            preCMC = Predicate.and(
                Predicate.brigde(CardRules.Predicates.cmc(
                        ComparableOp.GT_OR_EQUAL, clow), CardPrinted.FN_GET_RULES),
                Predicate.brigde(CardRules.Predicates.cmc(
                    ComparableOp.LT_OR_EQUAL, chigh), CardPrinted.FN_GET_RULES));
        }

        // Stack them all together and return.
        filter = Predicate.and(preCMC, Predicate.and(prePower, preToughness));
        return filter;
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
                ((CFilters) VFilters.SINGLETON_INSTANCE.getLayoutControl()).buildFilter();
            }
        }
    }
}

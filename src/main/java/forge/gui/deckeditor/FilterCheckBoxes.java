package forge.gui.deckeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;

import net.slightlymagic.maxmtg.Predicate;
import forge.card.CardRules;
import forge.item.CardPrinted;

/**
 * A structural class for some checkboxes need for a deck editor, contains no
 * JPanel to store boxes on Checkboxes are public so the using class should
 * place them in some container.
 */
class FilterCheckBoxes {

    /** The white. */
    public final JCheckBox white;

    /** The blue. */
    public final JCheckBox blue;

    /** The black. */
    public final JCheckBox black;

    /** The red. */
    public final JCheckBox red;

    /** The green. */
    public final JCheckBox green;

    /** The colorless. */
    public final JCheckBox colorless;

    /** The land. */
    public final JCheckBox land;

    /** The creature. */
    public final JCheckBox creature;

    /** The sorcery. */
    public final JCheckBox sorcery;

    /** The instant. */
    public final JCheckBox instant;

    /** The planeswalker. */
    public final JCheckBox planeswalker;

    /** The artifact. */
    public final JCheckBox artifact;

    /** The enchantment. */
    public final JCheckBox enchantment;

    // Very handy for classes using mass operations on an array of checkboxes
    /** The all colors. */
    public final List<JCheckBox> allColors;

    /** The all types. */
    public final List<JCheckBox> allTypes;

    /**
     * Instantiates a new filter check boxes.
     * 
     * @param useGraphicalBoxes
     *            the use graphical boxes
     */
    public FilterCheckBoxes(final boolean useGraphicalBoxes) {
        if (useGraphicalBoxes) {
            white = new CheckBoxWithIcon("white", "White");
            blue = new CheckBoxWithIcon("blue", "Blue");
            black = new CheckBoxWithIcon("black", "Black");
            red = new CheckBoxWithIcon("red", "Red");
            green = new CheckBoxWithIcon("green", "Green");
            colorless = new CheckBoxWithIcon("colorless", "Colorless");

            land = new CheckBoxWithIcon("land", "Land");
            creature = new CheckBoxWithIcon("creature", "Creature");
            sorcery = new CheckBoxWithIcon("sorcery", "Sorcery");
            instant = new CheckBoxWithIcon("instant", "Instant");
            planeswalker = new CheckBoxWithIcon("planeswalker", "Planeswalker");
            artifact = new CheckBoxWithIcon("artifact", "Artifact");
            enchantment = new CheckBoxWithIcon("enchant", "Enchantment");
        } else {
            white = new JCheckBox("W", true);
            blue = new JCheckBox("U", true);
            black = new JCheckBox("B", true);
            red = new JCheckBox("R", true);
            green = new JCheckBox("G", true);
            colorless = new JCheckBox("C", true);

            land = new JCheckBox("Land", true);
            creature = new JCheckBox("Creature", true);
            sorcery = new JCheckBox("Sorcery", true);
            instant = new JCheckBox("Instant", true);
            planeswalker = new JCheckBox("Planeswalker", true);
            artifact = new JCheckBox("Artifact", true);
            enchantment = new JCheckBox("Enchant", true);
        }

        allColors = Arrays.asList(new JCheckBox[] { white, blue, black, red, green, colorless });
        allTypes = Arrays.asList(new JCheckBox[] { land, creature, sorcery, instant, planeswalker, artifact,
                enchantment });
    }

    /**
     * Builds the filter.
     * 
     * @return the predicate
     */
    public final Predicate<CardPrinted> buildFilter() {
        List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        if (white.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_WHITE);
        }
        if (blue.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_BLUE);
        }
        if (black.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_BLACK);
        }
        if (red.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_RED);
        }
        if (green.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_GREEN);
        }
        if (colorless.isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_COLORLESS);
        }
        Predicate<CardRules> filterByColor = colors.size() == 6 ? CardRules.Predicates.Presets.CONSTANT_TRUE : Predicate
                .or(colors);

        List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();
        if (land.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_LAND);
        }
        if (creature.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_CREATURE);
        }
        if (sorcery.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_SORCERY);
        }
        if (instant.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_INSTANT);
        }
        if (planeswalker.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_PLANESWALKER);
        }
        if (artifact.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_ARTIFACT);
        }
        if (enchantment.isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_ENCHANTMENT);
        }
        Predicate<CardRules> filterByType = types.size() == 7 ? CardRules.Predicates.Presets.CONSTANT_TRUE : Predicate
                .or(types);

        return Predicate.brigde(Predicate.and(filterByColor, filterByType), CardPrinted.fnGetRules);
    }

}

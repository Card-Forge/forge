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
    private final JCheckBox white;

    /** The blue. */
    private final JCheckBox blue;

    /** The black. */
    private final JCheckBox black;

    /** The red. */
    private final JCheckBox red;

    /** The green. */
    private final JCheckBox green;

    /** The colorless. */
    private final JCheckBox colorless;

    /** The land. */
    private final JCheckBox land;

    /** The creature. */
    private final JCheckBox creature;

    /** The sorcery. */
    private final JCheckBox sorcery;

    /** The instant. */
    private final JCheckBox instant;

    /** The planeswalker. */
    private final JCheckBox planeswalker;

    /** The artifact. */
    private final JCheckBox artifact;

    /** The enchantment. */
    private final JCheckBox enchantment;

    // Very handy for classes using mass operations on an array of checkboxes
    /** The all colors. */
    private final List<JCheckBox> allColors;

    /** The all types. */
    private final List<JCheckBox> allTypes;

    /**
     * Instantiates a new filter check boxes.
     * 
     * @param useGraphicalBoxes
     *            the use graphical boxes
     */
    public FilterCheckBoxes(final boolean useGraphicalBoxes) {
        if (useGraphicalBoxes) {
            this.white = new CheckBoxWithIcon("white", "White");
            this.blue = new CheckBoxWithIcon("blue", "Blue");
            this.black = new CheckBoxWithIcon("black", "Black");
            this.red = new CheckBoxWithIcon("red", "Red");
            this.green = new CheckBoxWithIcon("green", "Green");
            this.colorless = new CheckBoxWithIcon("colorless", "Colorless");

            this.land = new CheckBoxWithIcon("land", "Land");
            this.creature = new CheckBoxWithIcon("creature", "Creature");
            this.sorcery = new CheckBoxWithIcon("sorcery", "Sorcery");
            this.instant = new CheckBoxWithIcon("instant", "Instant");
            this.planeswalker = new CheckBoxWithIcon("planeswalker", "Planeswalker");
            this.artifact = new CheckBoxWithIcon("artifact", "Artifact");
            this.enchantment = new CheckBoxWithIcon("enchant", "Enchantment");
        } else {
            this.white = new JCheckBox("W", true);
            this.blue = new JCheckBox("U", true);
            this.black = new JCheckBox("B", true);
            this.red = new JCheckBox("R", true);
            this.green = new JCheckBox("G", true);
            this.colorless = new JCheckBox("C", true);

            this.land = new JCheckBox("Land", true);
            this.creature = new JCheckBox("Creature", true);
            this.sorcery = new JCheckBox("Sorcery", true);
            this.instant = new JCheckBox("Instant", true);
            this.planeswalker = new JCheckBox("Planeswalker", true);
            this.artifact = new JCheckBox("Artifact", true);
            this.enchantment = new JCheckBox("Enchant", true);
        }

        this.allColors = Arrays.asList(new JCheckBox[] { this.getWhite(), this.getBlue(), this.getBlack(), this.getRed(), this.getGreen(),
                this.getColorless() });
        this.allTypes = Arrays.asList(new JCheckBox[] { this.getLand(), this.getCreature(), this.getSorcery(), this.getInstant(),
                this.getPlaneswalker(), this.getArtifact(), this.getEnchantment() });
    }

    /**
     * Builds the filter.
     * 
     * @return the predicate
     */
    public final Predicate<CardPrinted> buildFilter() {
        final List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        if (this.getWhite().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_WHITE);
        }
        if (this.getBlue().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_BLUE);
        }
        if (this.getBlack().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_BLACK);
        }
        if (this.getRed().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_RED);
        }
        if (this.getGreen().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_GREEN);
        }
        if (this.getColorless().isSelected()) {
            colors.add(CardRules.Predicates.Presets.IS_COLORLESS);
        }
        final Predicate<CardRules> filterByColor = colors.size() == 6 ? CardRules.Predicates.Presets.CONSTANT_TRUE
                : Predicate.or(colors);

        final List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();
        if (this.getLand().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_LAND);
        }
        if (this.getCreature().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_CREATURE);
        }
        if (this.getSorcery().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_SORCERY);
        }
        if (this.getInstant().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_INSTANT);
        }
        if (this.getPlaneswalker().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_PLANESWALKER);
        }
        if (this.getArtifact().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_ARTIFACT);
        }
        if (this.getEnchantment().isSelected()) {
            types.add(CardRules.Predicates.Presets.IS_ENCHANTMENT);
        }
        final Predicate<CardRules> filterByType = types.size() == 7 ? CardRules.Predicates.Presets.CONSTANT_TRUE
                : Predicate.or(types);

        return Predicate.brigde(Predicate.and(filterByColor, filterByType), CardPrinted.fnGetRules);
    }

    /**
     * @return the allTypes
     */
    public List<JCheckBox> getAllTypes() {
        return allTypes;
    }

    /**
     * @return the white
     */
    public JCheckBox getWhite() {
        return white;
    }

    /**
     * @return the blue
     */
    public JCheckBox getBlue() {
        return blue;
    }

    /**
     * @return the black
     */
    public JCheckBox getBlack() {
        return black;
    }

    /**
     * @return the red
     */
    public JCheckBox getRed() {
        return red;
    }

    /**
     * @return the colorless
     */
    public JCheckBox getColorless() {
        return colorless;
    }

    /**
     * @return the green
     */
    public JCheckBox getGreen() {
        return green;
    }

    /**
     * @return the land
     */
    public JCheckBox getLand() {
        return land;
    }

    /**
     * @return the allColors
     */
    public List<JCheckBox> getAllColors() {
        return allColors;
    }

    /**
     * @return the creature
     */
    public JCheckBox getCreature() {
        return creature;
    }

    /**
     * @return the sorcery
     */
    public JCheckBox getSorcery() {
        return sorcery;
    }

    /**
     * @return the instant
     */
    public JCheckBox getInstant() {
        return instant;
    }

    /**
     * @return the planeswalker
     */
    public JCheckBox getPlaneswalker() {
        return planeswalker;
    }

    /**
     * @return the artifact
     */
    public JCheckBox getArtifact() {
        return artifact;
    }

    /**
     * @return the enchantment
     */
    public JCheckBox getEnchantment() {
        return enchantment;
    }

}

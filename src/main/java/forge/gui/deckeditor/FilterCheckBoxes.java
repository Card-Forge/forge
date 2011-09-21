package forge.gui.deckeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;

import net.slightlymagic.maxmtg.Predicate;
import forge.card.CardRules;
import forge.item.CardPrinted;

/** 
 * A structural class for some checkboxes need for a deck editor, contains no JPanel to store boxes on 
 * Checkboxes are public so the using class should place them in some container.
 */
class FilterCheckBoxes {
    public final JCheckBox white;
    public final JCheckBox blue;
    public final JCheckBox black;
    public final JCheckBox red;
    public final JCheckBox green;
    public final JCheckBox colorless;

    public final JCheckBox land;
    public final JCheckBox creature;
    public final JCheckBox sorcery;
    public final JCheckBox instant;
    public final JCheckBox planeswalker;
    public final JCheckBox artifact;
    public final JCheckBox enchantment;

    // Very handy for classes using mass operations on an array of checkboxes
    public final List<JCheckBox> allColors;
    public final List<JCheckBox> allTypes;

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

         allColors = Arrays.asList(new JCheckBox[]{ white, blue, black, red, green, colorless});
         allTypes = Arrays.asList(new JCheckBox[]{ land, creature, sorcery, instant, planeswalker, artifact, enchantment });
    }
    
    
    public final Predicate<CardPrinted> buildFilter() {
        List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        if (white.isSelected()) { colors.add(CardRules.Predicates.Presets.isWhite); }
        if (blue.isSelected()) { colors.add(CardRules.Predicates.Presets.isBlue); }
        if (black.isSelected()) { colors.add(CardRules.Predicates.Presets.isBlack); }
        if (red.isSelected()) { colors.add(CardRules.Predicates.Presets.isRed); }
        if (green.isSelected()) { colors.add(CardRules.Predicates.Presets.isGreen); }
        if (colorless.isSelected()) { colors.add(CardRules.Predicates.Presets.isColorless); }
        Predicate<CardRules> filterByColor = colors.size() == 6 ? CardRules.Predicates.Presets.constantTrue : Predicate.or(colors);

        List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();
        if (land.isSelected()) { types.add(CardRules.Predicates.Presets.isLand); }
        if (creature.isSelected()) { types.add(CardRules.Predicates.Presets.isCreature); }
        if (sorcery.isSelected()) { types.add(CardRules.Predicates.Presets.isSorcery); }
        if (instant.isSelected()) { types.add(CardRules.Predicates.Presets.isInstant); }
        if (planeswalker.isSelected()) { types.add(CardRules.Predicates.Presets.isPlaneswalker); }
        if (artifact.isSelected()) { types.add(CardRules.Predicates.Presets.isArtifact); }
        if (enchantment.isSelected()) { types.add(CardRules.Predicates.Presets.isEnchantment); }
        Predicate<CardRules> filterByType = types.size() == 7 ? CardRules.Predicates.Presets.constantTrue : Predicate.or(types);

        return Predicate.brigde(Predicate.and(filterByColor, filterByType), CardPrinted.fnGetRules);
    }
    
}

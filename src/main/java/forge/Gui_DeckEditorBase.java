package forge;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFrame;

import net.slightlymagic.maxmtg.Predicate;

import forge.card.CardCoreType;
import forge.card.CardRules;
import forge.card.CardRules.Predicates;
import forge.card.CardPoolView;

public class Gui_DeckEditorBase extends JFrame  {
    private static final long serialVersionUID = -401223933343539977L;

    //public JCheckBox whiteCheckBox = new GuiFilterCheckBox("white", "White");
    public JCheckBox whiteCheckBox = new JCheckBox("W", true);
    public JCheckBox blueCheckBox = new JCheckBox("U", true);
    public JCheckBox blackCheckBox = new JCheckBox("B", true);
    public JCheckBox redCheckBox = new JCheckBox("R", true);
    public JCheckBox greenCheckBox = new JCheckBox("G", true);
    public JCheckBox colorlessCheckBox = new JCheckBox("C", true);

    public JCheckBox landCheckBox = new JCheckBox("Land", true);
    public JCheckBox creatureCheckBox = new JCheckBox("Creature", true);
    public JCheckBox sorceryCheckBox = new JCheckBox("Sorcery", true);
    public JCheckBox instantCheckBox = new JCheckBox("Instant", true);
    public JCheckBox planeswalkerCheckBox = new JCheckBox("Planeswalker", true);
    public JCheckBox artifactCheckBox = new JCheckBox("Artifact", true);
    public JCheckBox enchantmentCheckBox = new JCheckBox("Enchant", true);

    public static String getStats(CardPoolView deck) {
        int total = deck.countAll();
        int creature = CardRules.Predicates.Presets.isCreature.aggregate(deck, CardPoolView.fnToCard, CardPoolView.fnToCount);
        int land = CardRules.Predicates.Presets.isLand.aggregate(deck, CardPoolView.fnToCard, CardPoolView.fnToCount);

        StringBuffer show = new StringBuffer();
        show.append("Total - ").append(total).append(", Creatures - ").append(creature).append(", Land - ")
                .append(land);
        String[] color = Constant.Color.onlyColors;
        List<Predicate<CardRules>> predicates = CardRules.Predicates.Presets.colors;
        for (int i = 0; i < color.length; ++i) {
            show.append(String.format(", %s - %d", color[i], predicates.get(i).count(deck, CardPoolView.fnToCard)));
        }
        
        return show.toString();
    }// getStats()

    public final Predicate<CardRules> buildFilter() {
        List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        if (whiteCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isWhite); }
        if (blueCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isBlue); }
        if (blackCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isBlack); }
        if (redCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isRed); }
        if (greenCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isGreen); }
        if (colorlessCheckBox.isSelected()) { colors.add(CardRules.Predicates.Presets.isColorless); }
        Predicate<CardRules> filterByColor = colors.size() == 6 ? Predicate.getTrue(CardRules.class) : Predicate.or(colors);

        List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();
        if (landCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isLand); }
        if (creatureCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isCreature); }
        if (sorceryCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isSorcery); }
        if (instantCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isInstant); }
        if (planeswalkerCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isPlaneswalker); }
        if (artifactCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isArtifact); }
        if (enchantmentCheckBox.isSelected()) { types.add(CardRules.Predicates.Presets.isEnchantment); }
        Predicate<CardRules> filterByType = colors.size() == 7 ? Predicate.getTrue(CardRules.class) : Predicate.or(types);

        return Predicate.and(filterByColor, filterByType);
    }

}

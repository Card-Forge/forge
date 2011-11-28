/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

        this.allColors = Arrays.asList(new JCheckBox[] { this.getWhite(), this.getBlue(), this.getBlack(),
                this.getRed(), this.getGreen(), this.getColorless() });
        this.allTypes = Arrays.asList(new JCheckBox[] { this.getLand(), this.getCreature(), this.getSorcery(),
                this.getInstant(), this.getPlaneswalker(), this.getArtifact(), this.getEnchantment() });
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

        return Predicate.brigde(Predicate.and(filterByColor, filterByType), CardPrinted.FN_GET_RULES);
    }

    /**
     * Gets the all types.
     * 
     * @return the allTypes
     */
    public List<JCheckBox> getAllTypes() {
        return this.allTypes;
    }

    /**
     * Gets the white.
     * 
     * @return the white
     */
    public JCheckBox getWhite() {
        return this.white;
    }

    /**
     * Gets the blue.
     * 
     * @return the blue
     */
    public JCheckBox getBlue() {
        return this.blue;
    }

    /**
     * Gets the black.
     * 
     * @return the black
     */
    public JCheckBox getBlack() {
        return this.black;
    }

    /**
     * Gets the red.
     * 
     * @return the red
     */
    public JCheckBox getRed() {
        return this.red;
    }

    /**
     * Gets the colorless.
     * 
     * @return the colorless
     */
    public JCheckBox getColorless() {
        return this.colorless;
    }

    /**
     * Gets the green.
     * 
     * @return the green
     */
    public JCheckBox getGreen() {
        return this.green;
    }

    /**
     * Gets the land.
     * 
     * @return the land
     */
    public JCheckBox getLand() {
        return this.land;
    }

    /**
     * Gets the all colors.
     * 
     * @return the allColors
     */
    public List<JCheckBox> getAllColors() {
        return this.allColors;
    }

    /**
     * Gets the creature.
     * 
     * @return the creature
     */
    public JCheckBox getCreature() {
        return this.creature;
    }

    /**
     * Gets the sorcery.
     * 
     * @return the sorcery
     */
    public JCheckBox getSorcery() {
        return this.sorcery;
    }

    /**
     * Gets the instant.
     * 
     * @return the instant
     */
    public JCheckBox getInstant() {
        return this.instant;
    }

    /**
     * Gets the planeswalker.
     * 
     * @return the planeswalker
     */
    public JCheckBox getPlaneswalker() {
        return this.planeswalker;
    }

    /**
     * Gets the artifact.
     * 
     * @return the artifact
     */
    public JCheckBox getArtifact() {
        return this.artifact;
    }

    /**
     * Gets the enchantment.
     * 
     * @return the enchantment
     */
    public JCheckBox getEnchantment() {
        return this.enchantment;
    }

}

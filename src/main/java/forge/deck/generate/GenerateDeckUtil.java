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
package forge.deck.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.CardRules;
import forge.util.closures.Predicate;

/**
 * <p>
 * GenerateDeckUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GenerateDeckUtil.java 10011 2011-08-28 12:20:52Z Sloth $
 */
public class GenerateDeckUtil {

    public static final Predicate<CardRules> AI_CAN_PLAY = new Predicate<CardRules>() {
        @Override
        public boolean isTrue(CardRules c) {
            return !c.getRemAIDecks() && !c.getRemRandomDecks();
        }
    };

    public static final Predicate<CardRules> HUMAN_CAN_PLAY = new Predicate<CardRules>() {
        @Override
        public boolean isTrue(CardRules c) {
            return !c.getRemRandomDecks();
        }
    };

    public static final Predicate<CardRules> COLORLESS_CARDS = new Predicate<CardRules>() {
        @Override
        public boolean isTrue(CardRules c) {
            CardManaCost mc = c.getManaCost();
            return mc.getColorProfile() == 0 && !mc.isEmpty();
        }
    };

    public static class ContainsAllColorsFrom extends Predicate<CardRules> {
        private final CardColor allowedColor;
        public ContainsAllColorsFrom(CardColor color) {
            allowedColor = color;
        }

        @Override
        public boolean isTrue(CardRules subject) {
            return allowedColor.containsAllColorsFrom(subject.getManaCost().getColorProfile());
        }
    }

    public static class FilterCMC extends Predicate<CardRules> {
        private final int min;
        private final int max;

        public FilterCMC(int from, int to) {
            min = from; max = to;
        }

        @Override
        public boolean isTrue(CardRules c) {
            CardManaCost mc = c.getManaCost();
            int cmc = mc.getCMC();
            return cmc >= min && cmc <= max && !mc.isEmpty();
        }
    }

    private static Map<Integer, String[]> dualLands = new HashMap<Integer, String[]>();
    static {
        dualLands.put(CardColor.WHITE | CardColor.BLUE, new String[]{"Tundra", "Hallowed Fountain", "Flooded Strand"});
        dualLands.put(CardColor.BLACK | CardColor.BLUE, new String[]{"Underground Sea", "Watery Grave", "Polluted Delta"});
        dualLands.put(CardColor.BLACK | CardColor.RED, new String[]{"Badlands", "Blood Crypt", "Bloodstained Mire"});
        dualLands.put(CardColor.GREEN | CardColor.RED, new String[]{"Taiga", "Stomping Ground", "Wooded Foothills"});
        dualLands.put(CardColor.GREEN | CardColor.WHITE, new String[]{"Savannah", "Temple Garden", "Windswept Heath"});

        dualLands.put(CardColor.WHITE | CardColor.BLACK, new String[]{"Scrubland", "Godless Shrine", "Marsh Flats"});
        dualLands.put(CardColor.BLUE | CardColor.RED, new String[]{"Volcanic Island", "Steam Vents", "Scalding Tarn"});
        dualLands.put(CardColor.BLACK | CardColor.GREEN, new String[]{"Bayou", "Overgrown Tomb", "Verdant Catacombs"});
        dualLands.put(CardColor.WHITE | CardColor.RED, new String[]{"Plateau", "Sacred Foundry", "Arid Mesa"});
        dualLands.put(CardColor.GREEN | CardColor.BLUE, new String[]{"Tropical Island", "Breeding Pool", "Misty Rainforest"});
    }

    public static List<String> getDualLandList(final CardColor color) {

        final List<String> dLands = new ArrayList<String>();

        if (color.countColors() > 3) {
            dLands.add("Rupture Spire");
            dLands.add("Undiscovered Paradise");
        }

        if (color.countColors() > 2) {
            dLands.add("Evolving Wilds");
            dLands.add("Terramorphic Expanse");
        }
        for (Entry<Integer, String[]> dual : dualLands.entrySet()) {
            if (color.hasAllColors(dual.getKey())) {
                for (String s : dual.getValue()) {
                    dLands.add(s);
                }
            }
        }

        return dLands;
    }
}

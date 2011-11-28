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

/**
 * <p>
 * GenerateDeckUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GenerateDeckUtil.java 10011 2011-08-28 12:20:52Z Sloth $
 */
public class GenerateDeckUtil {

    /**
     * 
     * Arrays of dual and tri-land cards.
     * 
     * @param colors
     *            a String
     * @return ArrayList<String>
     */
    public static ArrayList<String> getDualLandList(final String colors) {

        final ArrayList<String> dLands = new ArrayList<String>();

        if (colors.length() > 3) {
            dLands.add("Rupture Spire");
            dLands.add("Undiscovered Paradise");
        }

        if (colors.length() > 2) {
            dLands.add("Evolving Wilds");
            dLands.add("Terramorphic Expanse");
        }

        if (colors.contains("W") && colors.contains("U")) {
            dLands.add("Tundra");
            dLands.add("Hallowed Fountain");
            dLands.add("Flooded Strand");
        }

        if (colors.contains("U") && colors.contains("B")) {
            dLands.add("Underground Sea");
            dLands.add("Watery Grave");
            dLands.add("Polluted Delta");
        }

        if (colors.contains("B") && colors.contains("R")) {
            dLands.add("Badlands");
            dLands.add("Blood Crypt");
            dLands.add("Bloodstained Mire");
        }

        if (colors.contains("R") && colors.contains("G")) {
            dLands.add("Taiga");
            dLands.add("Stomping Ground");
            dLands.add("Wooded Foothills");
        }

        if (colors.contains("G") && colors.contains("W")) {
            dLands.add("Savannah");
            dLands.add("Temple Garden");
            dLands.add("Windswept Heath");
        }

        if (colors.contains("W") && colors.contains("B")) {
            dLands.add("Scrubland");
            dLands.add("Godless Shrine");
            dLands.add("Marsh Flats");
        }

        if (colors.contains("U") && colors.contains("R")) {
            dLands.add("Volcanic Island");
            dLands.add("Steam Vents");
            dLands.add("Scalding Tarn");
        }

        if (colors.contains("B") && colors.contains("G")) {
            dLands.add("Bayou");
            dLands.add("Overgrown Tomb");
            dLands.add("Verdant Catacombs");
        }

        if (colors.contains("R") && colors.contains("W")) {
            dLands.add("Plateau");
            dLands.add("Sacred Foundry");
            dLands.add("Arid Mesa");
        }

        if (colors.contains("G") && colors.contains("U")) {
            dLands.add("Tropical Island");
            dLands.add("Breeding Pool");
            dLands.add("Misty Rainforest");
        }

        return dLands;
    }
}

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

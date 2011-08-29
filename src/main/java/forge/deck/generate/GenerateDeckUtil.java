package forge.deck.generate;

import forge.*;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>GenerateDeckUtil class.</p>
 *
 * @author Forge
 * @version $Id: GenerateDeckUtil.java 10011 2011-08-28 12:20:52Z Sloth $
 */
public class GenerateDeckUtil {
    

    public static ArrayList<String> getDualLandList(String colors) {
        
        ArrayList<String> DLands = new ArrayList<String>();
        
        if (colors.length() > 3) {
            DLands.add("Rupture Spire");
            DLands.add("Undiscovered Paradise");
        }
        
        if (colors.length() > 2) {
            DLands.add("Evolving Wilds");
            DLands.add("Terramorphic Expanse");
        }
        
        if (colors.contains("W") && colors.contains("U")) {
            DLands.add("Tundra");
            DLands.add("Hallowed Fountain");
            DLands.add("Flooded Strand");
        }

        if (colors.contains("U") && colors.contains("B")) {
            DLands.add("Underground Sea");
            DLands.add("Watery Grave");
            DLands.add("Polluted Delta");
        }
        
        if (colors.contains("B") && colors.contains("R")) {
            DLands.add("Badlands");
            DLands.add("Blood Crypt");
            DLands.add("Bloodstained Mire");
        }
        
        if (colors.contains("R") && colors.contains("G")) {
            DLands.add("Taiga");
            DLands.add("Stomping Ground");
            DLands.add("Wooded Foothills");
        }
        
        if (colors.contains("G") && colors.contains("W")) {
            DLands.add("Savannah");
            DLands.add("Temple Garden");
            DLands.add("Windswept Heath");
        }
        
        if (colors.contains("W") && colors.contains("B")) {
            DLands.add("Scrubland");
            DLands.add("Godless Shrine");
            DLands.add("Marsh Flats");
        }
        
        if (colors.contains("U") && colors.contains("R")) {
            DLands.add("Volcanic Island");
            DLands.add("Steam Vents");
            DLands.add("Scalding Tarn");
        }
        
        if (colors.contains("B") && colors.contains("G")) {
            DLands.add("Bayou");
            DLands.add("Overgrown Tomb");
            DLands.add("Verdant Catacombs");
        }
        
        if (colors.contains("R") && colors.contains("W")) {
            DLands.add("Plateau");
            DLands.add("Sacred Foundry");
            DLands.add("Arid Mesa");
        }
        
        if (colors.contains("G") && colors.contains("U")) {
            DLands.add("Tropical Island");
            DLands.add("Breeding Pool");
            DLands.add("Misty Rainforest");
        }

        return DLands;
    }
}

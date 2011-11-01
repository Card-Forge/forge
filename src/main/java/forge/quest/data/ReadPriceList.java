package forge.quest.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.esotericsoftware.minlog.Log;

import forge.MyRandom;
import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * ReadPriceList class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ReadPriceList implements NewConstants {

    /** Constant <code>comment="//"</code>. */
    private static final String comment = "//";

    private HashMap<String, Integer> priceMap;

    /**
     * <p>
     * Constructor for ReadPriceList.
     * </p>
     */
    public ReadPriceList() {
        this.setup();
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        this.priceMap = this.readFile(ForgeProps.getFile(Quest.PRICE));
        this.priceMap.putAll(this.readFile(ForgeProps.getFile(Quest.BOOSTER_PRICE)));

    } // setup()

    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.util.HashMap} object.
     */
    private HashMap<String, Integer> readFile(final File file) {
        BufferedReader in;
        final HashMap<String, Integer> map = new HashMap<String, Integer>();
        final Random r = MyRandom.getRandom();
        try {

            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while ((line != null) && (line.trim().length() != 0)) {
                if (!line.startsWith(ReadPriceList.comment)) {
                    final String[] s = line.split("=");
                    final String name = s[0].trim();
                    final String price = s[1].trim();

                    // System.out.println("Name: " + name + ", Price: " +
                    // price);

                    try {
                        int val = Integer.parseInt(price.trim());

                        if (!(name.equals("Plains") || name.equals("Island") || name.equals("Swamp")
                                || name.equals("Mountain") || name.equals("Forest")
                                || name.equals("Snow-Covered Plains") || name.equals("Snow-Covered Island")
                                || name.equals("Snow-Covered Swamp") || name.equals("Snow-Covered Mountain") || name
                                    .equals("Snow-Covered Forest"))) {
                            float ff = 0;
                            if (r.nextInt(100) < 90) {
                                ff = r.nextInt(10) * (float) .01;
                            } else {
                                // +/- 50%
                                ff = r.nextInt(50) * (float) .01;
                            }

                            if (r.nextInt(100) < 50) {
                                val = (int) (val * (1 - ff));
                            } else {
                                // +ff%
                                val = (int) (val * (1 + ff));
                            }
                        }

                        map.put(name, val);
                    } catch (final NumberFormatException nfe) {
                        Log.warn("NumberFormatException: " + nfe.getMessage());
                    }
                }
                line = in.readLine();
            } // if

        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadPriceList : readFile error, " + ex);
        }

        return map;
    } // readFile()

    /**
     * <p>
     * getPriceList.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<String, Integer> getPriceList() {
        return this.priceMap;
    }
}

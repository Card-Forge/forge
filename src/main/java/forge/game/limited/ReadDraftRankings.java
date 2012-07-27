package forge.game.limited;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.minlog.Log;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * ReadDraftRankings class.
 * 
 */
public class ReadDraftRankings {

    /** Constant <code>comment="//"</code>. */
    private static final String COMMENT = "//";

    private Map<String, Map<String, Integer>> draftRankings;

    /**
     * <p>
     * Constructor for ReadPriceList.
     * </p>
     */
    public ReadDraftRankings() {
        this.setup();
    }

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        this.draftRankings = this.readFile(ForgeProps.getFile(NewConstants.Draft.RANKINGS));
    } // setup()

    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.util.Map} object.
     */
    private Map<String, Map<String, Integer>> readFile(final File file) {
        BufferedReader in;
        final Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
        try {

            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while ((line != null) && (line.trim().length() != 0)) {
                if (!line.startsWith(ReadDraftRankings.COMMENT)) {
                    final String[] s = line.split(",");
                    final String rankStr = s[0].trim().substring(1);
                    final String name = s[1].trim();
                    // final String rarity = s[2].trim();
                    final String edition = s[3].trim();

                    try {
                        final int rank = Integer.parseInt(rankStr);
                        if (!map.containsKey(edition)) {
                            map.put(edition, new HashMap<String, Integer>());
                        }
                        map.get(edition).put(name, rank);
                    } catch (NumberFormatException nfe) {
                        Log.warn("NumberFormatException: " + nfe.getMessage());
                    }
                }
                line = in.readLine();
            } // if

        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadDraftRankings : readFile error, " + ex);
        }

        return map;
    } // readFile()

    public Integer getRanking(String cardName, String edition) {
        Integer rank = null;
        if (draftRankings.containsKey(edition)) {
            String safeName = cardName.replaceAll("[^A-Za-z ]", "");
            rank = draftRankings.get(edition).get(safeName);
        }
        return rank;
    }
}

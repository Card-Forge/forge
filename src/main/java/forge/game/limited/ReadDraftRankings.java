package forge.game.limited;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    private Map<String, Integer> setSizes;

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
        this.setSizes = new HashMap<String, Integer>();
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
        BufferedReader in = null;
        final Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
        try {

            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while ((line != null) && (line.trim().length() != 0)) {
                if (!line.startsWith(ReadDraftRankings.COMMENT)) {
                    final String[] s = line.split("\\|");
                    final String rankStr = s[0].trim().substring(1);
                    final String name = s[1].trim().replaceAll("-", " ").replaceAll("[^A-Za-z ]", "");
                    // final String rarity = s[2].trim();
                    final String edition = s[3].trim();

                    try {
                        final int rank = Integer.parseInt(rankStr);
                        if (!map.containsKey(edition)) {
                            map.put(edition, new HashMap<String, Integer>());
                        }
                        map.get(edition).put(name, rank);
                        if (setSizes.containsKey(edition)) {
                            setSizes.put(edition, Math.max(setSizes.get(edition), rank));
                        } else {
                            setSizes.put(edition, rank);
                        }
                    } catch (NumberFormatException nfe) {
                        Log.warn("NumberFormatException: " + nfe.getMessage());
                    }
                }
                line = in.readLine();
            } // if

        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadDraftRankings : readFile error, " + ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return map;
    } // readFile()

    /**
     * Get the relative ranking for the given card name in the given edition.
     * 
     * @param cardName
     *            the card name
     * @param edition
     *            the card's edition
     * @return ranking
     */
    public Double getRanking(String cardName, String edition) {
        Double rank = null;

        // Basic lands should be excluded from the evaluation --BBU
        if (cardName.equals("Island") || cardName.equals("Forest") || cardName.equals("Swamp")
                || cardName.equals("Plains") || cardName.equals("Mountain")) {
                return null;
                }

        if (draftRankings.containsKey(edition)) {
            String safeName = cardName.replaceAll("-", " ").replaceAll("[^A-Za-z ]", "");

            // If a card has no ranking, don't try to look it up --BBU
            if (draftRankings.get(edition).get(safeName) == null) {
                // System.out.println("WARNING! " + safeName + " NOT found in " + edition);
                return null;
            }
            rank = (double) draftRankings.get(edition).get(safeName) / (double) setSizes.get(edition);
        }
        return rank;
    }
}

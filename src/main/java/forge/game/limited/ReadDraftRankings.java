package forge.game.limited;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.minlog.Log;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/**
 * ReadDraftRankings class.
 * 
 */
public class ReadDraftRankings {

    /** Constant <code>comment="//"</code>. */
    private static final String COMMENT = "//";

    private final Map<String, Map<String, Integer>> draftRankings;
    private final Map<String, Integer> setSizes;

    /**
     * <p>
     * Constructor for ReadPriceList.
     * </p>
     */
    public ReadDraftRankings() {
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

        final Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
        for (String line : FileUtil.readFile(file)) {
            // stop reading if end of file or blank line is read
            if (line == null || line.length() == 0) {
                break;
            }

            if (line.startsWith(ReadDraftRankings.COMMENT)) {
                continue;
            }
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

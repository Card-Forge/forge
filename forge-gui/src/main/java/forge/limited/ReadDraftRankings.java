package forge.limited;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.esotericsoftware.minlog.Log;

import forge.properties.ForgeConstants;
import forge.util.FileUtil;

/**
 * ReadDraftRankings class.
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
        this.draftRankings = this.readFile(FileUtil.readFile(ForgeConstants.DRAFT_RANKINGS_FILE));
    } // setup()

    private static String getRankingCardName(final String name) {
        return StringUtils.replaceChars(name.trim(), "-áàâéèêúùûíìîóòô", " aaaeeeuuuiiiooo").replaceAll("[^A-Za-z ]", "");
    }

    /**
     * <p>
     * readFile.
     * </p>
     *
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.util.Map} object.
     */
    private Map<String, Map<String, Integer>> readFile(final List<String> lines) {

        final Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
        for (final String line : lines) {
            // stop reading if end of file or blank line is read
            if (line == null || line.length() == 0) {
                break;
            }

            if (line.startsWith(ReadDraftRankings.COMMENT)) {
                continue;
            }
            final String[] s = line.split("\\|");
            final String rankStr = s[0].trim().substring(1);
            final String name = getRankingCardName(s[1]);
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
            } catch (final NumberFormatException nfe) {
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
    public Double getRanking(final String cardName, final String edition) {
        if (draftRankings.containsKey(edition)) {
            final String safeName = getRankingCardName(cardName);

            // If a card has no ranking, don't try to look it up --BBU
            if (draftRankings.get(edition).get(safeName) == null) {
                // System.out.println("WARNING! " + safeName + " NOT found in " + edition);
                return null;
            }
            return Double.valueOf(draftRankings.get(edition).get(safeName).doubleValue() / setSizes.get(edition).doubleValue());
        }
        return null;
    }
}

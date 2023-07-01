package forge.gamemodes.limited;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.localinstance.properties.ForgeConstants;
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
        this.setSizes = new HashMap<>();
        this.draftRankings = this.readFile(FileUtil.readFile(ForgeConstants.DRAFT_RANKINGS_FILE));
    } // setup()

    public ReadDraftRankings(String customFile) {
        this.setSizes = new HashMap<>();
        this.draftRankings = this.readFile(FileUtil.readFile(ForgeConstants.DRAFT_DIR + customFile));
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
    private Map<String, Map<String, Integer>> readFile(List<String> lines) {

        final Map<String, Map<String, Integer>> map = new HashMap<>();
        for (String line : lines) {
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
                    map.put(edition, new HashMap<>());
                }
                map.get(edition).put(name, rank);
                if (setSizes.containsKey(edition)) {
                    setSizes.put(edition, Math.max(setSizes.get(edition), rank));
                } else {
                    setSizes.put(edition, rank);
                }
            } catch (NumberFormatException nfe) {
                System.err.println("NumberFormatException: " + nfe.getMessage());
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

        if (draftRankings.containsKey(edition)) {
            String safeName = cardName.replaceAll("-", " ").replaceAll("[^A-Za-z ]", "").replaceAll("  ", " ");

            // If a card has no ranking, don't try to look it up --BBU
            if (draftRankings.get(edition).get(safeName) == null) {
                // System.out.println("WARNING! " + safeName + " NOT found in " + edition);
                return null;
            }
            rank = (double) draftRankings.get(edition).get(safeName) / (double) setSizes.get(edition);
        }
        return rank;
    }

    public Double getCustomRanking(String cardName) {
        return getRanking(cardName, "CUSTOM");
    }
}

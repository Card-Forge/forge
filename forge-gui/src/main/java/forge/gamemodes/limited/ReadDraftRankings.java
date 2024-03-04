package forge.gamemodes.limited;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

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
        this.draftRankings = readRankingFolder();
    } // setup()

    public ReadDraftRankings(String customFile) {
        this.setSizes = new HashMap<>();
        this.draftRankings = this.readFile(FileUtil.readFile(ForgeConstants.DRAFT_DIR + customFile));
    }

    private Map<String, Map<String, Integer>> readRankingFolder() {
        Map<String, Map<String, Integer>> map = new HashMap<>();

        File rankingDirectory = new File(ForgeConstants.DRAFT_RANKINGS_FOLDER);

        if (!rankingDirectory.isDirectory()) {
            System.out.println(rankingDirectory + " is not a directory...");
            return null;
        }

        for(File rank : rankingDirectory.listFiles()) {
            if (rank.isDirectory()) {
                continue;
            }

            this.readFile(FileUtil.readFile(rank), map);
        }

        return map;
    }


    private Map<String, Map<String, Integer>> readFile(List<String> lines) {
        final Map<String, Map<String, Integer>> map = new HashMap<>();

        return readFile(lines, map);
    }
    private Map<String, Map<String, Integer>> readFile(List<String> lines, Map<String, Map<String, Integer>> map) {
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
            final String name = StringUtils.stripAccents(s[1].trim());
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
            // This should be updated
            String safeName = StringUtils.stripAccents(cardName);

            // handle split cards
            safeName = safeName.replace(" // ", " ");

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

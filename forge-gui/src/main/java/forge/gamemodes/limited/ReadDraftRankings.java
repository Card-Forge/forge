package forge.gamemodes.limited;

import java.io.File;
import java.util.ArrayList;
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

    private final Map<String, Map<String, Double>> draftRankings;

    /**
     * <p>
     * Constructor for ReadPriceList.
     * </p>
     */
    public ReadDraftRankings() {
        this.draftRankings = readRankingFolder();
    } // setup()

    public ReadDraftRankings(String customFile) {
        this.draftRankings = this.readFile(FileUtil.readFile(ForgeConstants.DRAFT_DIR + customFile));
    }

    private Map<String, Map<String, Double>> readRankingFolder() {
        Map<String, Map<String, Double>> map = new HashMap<>();

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


    private Map<String, Map<String, Double>> readFile(List<String> lines) {
        final Map<String, Map<String, Double>> map = new HashMap<>();

        return readFile(lines, map);
    }
    private Map<String, Map<String, Double>> readFile(List<String> lines, Map<String, Map<String, Double>> map) {
        record Entry(String edition, String name, int rank) {}
        final List<Entry> entries = new ArrayList<>();
        final Map<String, Integer> maxRank = new HashMap<>();
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
                maxRank.merge(edition, rank, Math::max);
                entries.add(new Entry(edition, name, rank));
            } catch (NumberFormatException nfe) {
                System.err.println("NumberFormatException: " + nfe.getMessage());
            }
        }

        // Scale within this file only, so files sharing a set code don't rescale each other.
        for (Entry e : entries) {
            map.computeIfAbsent(e.edition(), k -> new HashMap<>()).put(e.name(), e.rank() / (double) maxRank.get(e.edition()));
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

            rank = draftRankings.get(edition).get(safeName);
        }
        return rank;
    }
}

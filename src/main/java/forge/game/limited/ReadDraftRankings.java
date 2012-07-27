package forge.game.limited;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, List<String>> draftRankings;

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
    private Map<String, List<String>> readFile(final File file) {
        BufferedReader in;
        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        try {

            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            // stop reading if end of file or blank line is read
            while ((line != null) && (line.trim().length() != 0)) {
                if (!line.startsWith(ReadDraftRankings.COMMENT)) {
                    final String[] s = line.split(",");
                    //final String rank = s[0].trim().substring(1);
                    final String name = s[1].trim();
                    //final String rarity = s[2].trim();
                    final String edition = s[3].trim();

                    if (!map.containsKey(edition)) {
                        map.put(edition, new ArrayList<String>());
                    }
                    map.get(edition).add(name);
                }
                line = in.readLine();
            } // if

        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadDraftRankings : readFile error, " + ex);
        }

        return map;
    } // readFile()

    /**
     * <p>
     * getDraftRankings.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<String, List<String>> getDraftRankings() {
        return this.draftRankings;
    }
}

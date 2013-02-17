package forge.card;

import com.google.common.base.Function;

import forge.util.FileSection;
import forge.util.storage.StorageReaderFile;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FatPackData {
    private final String edition;
    public final String getEdition() {
        return edition;
    }

    private final String landsEdition;
    public final String getLandsEdition() {
        return landsEdition == null ? edition : landsEdition;
    }

    public int getCntBoosters() {
        return cntBoosters;
    }

    public int getCntLands() {
        return cntLands;
    }

    private final int cntBoosters;
    private final int cntLands;

    public FatPackData(String edition0, String landsEdition0, int nBoosters, int nBasicLands)
    {
        cntBoosters = nBoosters;
        cntLands = nBasicLands;
        edition = edition0;
        landsEdition = landsEdition0;
    }

    public static final Function<FatPackData, String> FN_GET_CODE = new Function<FatPackData, String>() {

        @Override
        public String apply(FatPackData arg1) {
            return arg1.edition;
        }
    };

    public static final class Reader extends StorageReaderFile<FatPackData> {

        public Reader(String pathname) {
            super(pathname, FatPackData.FN_GET_CODE);
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected FatPackData read(String line) {
            final FileSection section = FileSection.parse(line, ":", "|");
            int nBoosters = section.getInt("Boosters", 0);
            int nLand = section.getInt("BasicLands", 0);
            return new FatPackData(section.get("Set"), section.get("LandSet"), nBoosters, nLand);
        }
    }
}

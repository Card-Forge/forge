package forge.card;

import forge.util.FileSection;
import forge.util.storage.StorageReaderFile;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FatPackData extends PackData {
    private final int cntBoosters;
    public int getCntBoosters() {
        return cntBoosters;
    }

    public FatPackData(String edition0, String landEdition0, int nBoosters, int nBasicLands)
    {
        super(edition0, landEdition0, nBasicLands);
        cntBoosters = nBoosters;
    }

    public static final class Reader extends StorageReaderFile<FatPackData> {
        public Reader(String pathname) {
            super(pathname, PackData.FN_GET_CODE);
        }

        @Override
        protected FatPackData read(String line, int i) {
            final FileSection section = FileSection.parse(line, ":", "|");
            int nBoosters = section.getInt("Boosters", 0);
            int nLand = section.getInt("BasicLands", 0);
            return new FatPackData(section.get("Set"), section.get("LandSet"), nBoosters, nLand);
        }
    }
    
    @Override
    public String toString() {
        if (0 >= cntBoosters) {
            return "no cards";
        }
        
        StringBuilder s = new StringBuilder();
        
        if (0 < getCntLands()) {
            s.append(getCntLands()).append(" land");
            if (1 < getCntLands()) {
                s.append("s");
            }
            
            if (!getEdition().equalsIgnoreCase(getLandEdition())) {
                s.append(" from edition: ").append(getLandEdition());
            }
            
            if (0 < cntBoosters) {
                s.append(" and ");
            }
        }

        if (0 < cntBoosters) {
            s.append(cntBoosters).append(" booster packs, each containing ");
        }
        
        return s.toString();
    }
}

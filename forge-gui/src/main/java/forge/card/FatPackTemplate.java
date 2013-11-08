package forge.card;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FatPackTemplate extends SealedProductTemplate {
    private final int cntBoosters;


    public int getCntBoosters() { return cntBoosters; }

    private FatPackTemplate(String edition, int boosters, Iterable<Pair<String, Integer>> itrSlots)
    {
        super(edition, itrSlots);
        cntBoosters = boosters;
    }

    public static final class Reader extends StorageReaderFile<FatPackTemplate> {
        public Reader(String pathname) {
            super(pathname, FatPackTemplate.FN_GET_NAME);
        }

        @Override
        protected FatPackTemplate read(String line, int i) {
            String[] headAndData = TextUtil.split(line, ':', 2);
            final String edition = headAndData[0];
            final String[] data = TextUtil.splitWithParenthesis(headAndData[1], ',');
            int nBoosters = 6;

            List<Pair<String, Integer>> slots = new ArrayList<Pair<String,Integer>>();
            for(String slotDesc : data) {
                String[] kv = TextUtil.split(slotDesc, ' ', 2);
                if (kv[1].startsWith("Booster"))
                    nBoosters = Integer.parseInt(kv[0]);
                else
                    slots.add(ImmutablePair.of(kv[1], Integer.parseInt(kv[0])));
            }

            return new FatPackTemplate(edition, nBoosters, slots);
        }
    }
    
    @Override
    public String toString() {
        if (0 >= cntBoosters) {
            return "no cards";
        }

        StringBuilder s = new StringBuilder();
        for(Pair<String, Integer> p : slots) {
            s.append(p.getRight()).append(" ").append(p.getLeft()).append(", ");
        }
        // trim the last comma and space
        if( s.length() > 0 )
            s.replace(s.length() - 2, s.length(), "");

        if (0 < cntBoosters) {
            if( s.length() > 0 )
                s.append(" and ");
                
            s.append(cntBoosters).append(" booster packs ");
        }
        return s.toString();
    }
}

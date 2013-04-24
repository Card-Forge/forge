package forge.card;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FatPackTemplate extends SealedProductTemplate {
    private final int cntBoosters;
    private final String edition;

    @Override
    public String getEdition() { return edition; }
    public int getCntBoosters() { return cntBoosters; }

    public static final Function<? super FatPackTemplate, String> FN_GET_CODE = new Function<FatPackTemplate, String>() {
        @Override
        public String apply(FatPackTemplate arg1) {
            return arg1.edition;
        }
    };

    private FatPackTemplate(String edition0, int boosters, Iterable<Pair<String, Integer>> itrSlots)
    {
        super(itrSlots);
        edition = edition0;
        cntBoosters = boosters;
    }

    public static final class Reader extends StorageReaderFile<FatPackTemplate> {
        public Reader(String pathname) {
            super(pathname, FatPackTemplate.FN_GET_CODE);
        }

        @Override
        protected FatPackTemplate read(String line, int i) {
            String[] headAndData = TextUtil.split(line, ':', 2);
            final String edition = headAndData[0];
            final String[] data = TextUtil.splitWithParenthesis(headAndData[1], ',', '(', ')');
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
        s.replace(s.length() - 2, s.length(), "");
        
        s.append(" and ");
        if (0 < cntBoosters) {
            s.append(cntBoosters).append(" booster packs");
        }
        
        return s.toString();
    }
}

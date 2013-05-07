package forge.card;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

public class BoosterTemplate extends SealedProductTemplate {

    @SuppressWarnings("unchecked")
    public final static BoosterTemplate genericBooster = new BoosterTemplate(null, 1, Lists.newArrayList(
        Pair.of(BoosterGenerator.COMMON, 10), Pair.of(BoosterGenerator.UNCOMMON, 3), 
        Pair.of(BoosterGenerator.RARE_MYTHIC, 1), Pair.of(BoosterGenerator.BASIC_LAND, 1)
    ));
    
    private final int foilRate = 68;
    private final int artIndices;

    private BoosterTemplate(String edition, int artIndices0, Iterable<Pair<String, Integer>> itrSlots) {
        super(edition, itrSlots);
        artIndices = artIndices0;
    }

    public final int getFoilChance() {
        return this.foilRate;
    }
    
    public int getArtIndices() {
        return artIndices;
    }
    

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        
        
        s.append("consisting of ");
        for(Pair<String, Integer> p : slots) {
            s.append(p.getRight()).append(" ").append(p.getLeft()).append(", ");
        }

        // trim the last comma and space
        s.replace(s.length() - 2, s.length(), "");
        
        // put an 'and' before the previous comma
        int lastCommaIdx = s.lastIndexOf(","); 
        if (0 < lastCommaIdx) {
            s.replace(lastCommaIdx+1, lastCommaIdx+1, " and");
        }

        return s.toString();
    }

    public static final class Reader extends StorageReaderFile<BoosterTemplate> {
        public Reader(String pathname) {
            super(pathname, BoosterTemplate.FN_GET_NAME);
        }

        @Override
        protected BoosterTemplate read(String line, int i) {
            String[] headAndData = TextUtil.split(line, ':', 2);
            final String edition = headAndData[0];
            final String[] data = TextUtil.splitWithParenthesis(headAndData[1], ',', '(', ')');
            int nCovers = 1;

            List<Pair<String, Integer>> slots = new ArrayList<Pair<String,Integer>>();
            for(String slotDesc : data) {
                String[] kv = TextUtil.splitWithParenthesis(slotDesc, ' ', '(', ')', 2);
                if (kv[1].startsWith("cover"))
                    nCovers = Integer.parseInt(kv[0]);
                else
                    slots.add(ImmutablePair.of(kv[1], Integer.parseInt(kv[0])));
            }

            return new BoosterTemplate(edition, nCovers, slots);
        }
    }
}

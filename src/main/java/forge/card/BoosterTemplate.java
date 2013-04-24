package forge.card;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;

public class BoosterTemplate extends SealedProductTemplate {

    @SuppressWarnings("unchecked")
    public final static BoosterTemplate genericBooster = new BoosterTemplate(null, 1, Lists.newArrayList(
        Pair.of("Common", 10), Pair.of("Uncommon", 3), Pair.of("Rare", 1), Pair.of("BasicLand", 1)  
    ));
    
    private final int foilRate = 68;
    private final int artIndices;
    private final String edition;
    
    private BoosterTemplate(String edition0, int artIndices0, Iterable<Pair<String, Integer>> itrSlots) {
        super(itrSlots);
        this.edition = edition0;
        artIndices = artIndices0;
    }

    public final int getFoilChance() {
        return this.foilRate;
    }
    
    public int getArtIndices() {
        return artIndices;
    }
    
    @Override
    public final String getEdition() { return edition; } 

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
        
        if (0 < foilRate) {
            s.append(", with a foil rate of 1 in ").append(foilRate);
        }
        
        return s.toString();
    }

    public static final Function<? super BoosterTemplate, String> FN_GET_CODE = new Function<BoosterTemplate, String>() {
        @Override
        public String apply(BoosterTemplate arg1) {
            return arg1.edition;
        }
    };

    
    public static final class Reader extends StorageReaderFile<BoosterTemplate> {
        public Reader(String pathname) {
            super(pathname, BoosterTemplate.FN_GET_CODE);
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

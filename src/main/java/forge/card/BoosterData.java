package forge.card;

import com.google.common.base.Predicate;

import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.util.FileSection;
import forge.util.storage.StorageReaderFile;

public class BoosterData extends PackData {
    private final int nCommon;
    private final int nUncommon;
    private final int nRare;
    private final int nSpecial;
    private final int nDoubleFaced;
    private final int foilRate;
    private static final int CARDS_PER_BOOSTER = 15;

    public BoosterData(final String edition, final String editionLand, final int nC, final int nU, final int nR, final int nS, final int nDF) {
        // if this booster has more that 10 cards, there must be a land in
        // 15th slot unless it's already taken
        this(edition, editionLand, nC, nU, nR, nS, nDF, (nC + nR + nU + nS + nDF) > 10 ? BoosterData.CARDS_PER_BOOSTER - nC - nR - nU
                - nS - nDF : 0, 68);
    }

    public BoosterData(final String edition0, final String editionLand, final int nC, final int nU, final int nR, final int nS, final int nDF, final int nL,
            final int oneFoilPer) {
        super(edition0, editionLand, nL > 0 ? nL : 0);
        this.nCommon = nC;
        this.nUncommon = nU;
        this.nRare = nR;
        this.nSpecial = nS;
        this.nDoubleFaced = nDF;
        this.foilRate = oneFoilPer;
    }

    public final int getCommon() {
        return this.nCommon;
    }

    public final Predicate<CardPrinted> getEditionFilter() {
        return IPaperCard.Predicates.printedInSets(getEdition());
    }
    public final Predicate<CardPrinted> getLandEditionFilter() {
        return IPaperCard.Predicates.printedInSets(getLandEdition());
    }
    
    public final int getUncommon() {
        return this.nUncommon;
    }

    public final int getRare() {
        return this.nRare;
    }

    public final int getSpecial() {
        return this.nSpecial;
    }

    public final int getDoubleFaced() {
        return this.nDoubleFaced;
    }

    public final int getTotal() {
        return this.nCommon + this.nUncommon + this.nRare + this.nSpecial + this.nDoubleFaced + getCntLands();
    }

    public final int getFoilChance() {
        return this.foilRate;
    }
    
    private void _append(StringBuilder s, int val, String name) {
        if (0 >= val) {
            return;
        }
        s.append(val).append(' ').append(name);
        if (1 < val) {
            s.append('s');
        }
        s.append(", ");
    }

    @Override
    public String toString() {
        int total = getTotal();
        
        if (0 >= total) {
            return "no cards";
        }
        
        StringBuilder s = new StringBuilder();
        
        _append(s, total, "card");
        if (0 < total) {
            // remove comma
            s.deleteCharAt(s.length() - 2);
        }
        
        s.append("consisting of ");
        _append(s, nSpecial, "special");
        _append(s, nDoubleFaced, "double faced card");
        _append(s, nRare, "rare");
        _append(s, nUncommon, "uncommon");
        _append(s, nCommon, "common");
        if (getEdition().equalsIgnoreCase(getLandEdition())) {
            _append(s, getCntLands(), "land");
        } else if (0 < getCntLands()) {
            s.append(getCntLands()).append("land");
            if (1 < getCntLands()) {
                s.append("s");
            }
            s.append("from edition: ").append(getLandEdition()).append(", ");
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

    public static final class Reader extends StorageReaderFile<BoosterData> {
        public Reader(String pathname) {
            super(pathname, BoosterData.FN_GET_CODE);
        }

        @Override
        protected BoosterData read(String line, int i) {
            final FileSection section = FileSection.parse(line, ":", "|");
            int nC = section.getInt("Commons", 0);
            int nU = section.getInt("Uncommons", 0);
            int nR = section.getInt("Rares", 0);
            int nS = section.getInt("Special", 0);
            int nDf = section.getInt("DoubleFaced", 0);
            int nLand = section.getInt("BasicLands", 0);
            int nFoilRate = section.getInt("FoilRate", 68);
            String edition = section.get("Set");
            String editionLand = section.get("LandSet");
            if (editionLand == null) {
                editionLand = edition;
            }
            
            return new BoosterData(edition, editionLand, nC, nU, nR, nS, nDf, nLand, nFoilRate);
        }
    }
}

package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardBlock;
import forge.card.CardSet;
import forge.game.GameFormat;

/**
 * <p>SetInfoUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class SetUtils {

    private SetUtils() {
        throw new AssertionError();
    }

    /** Constant <code>setData</code>. */
    private static Map<String, CardSet> setsByCode = new HashMap<String, CardSet>();
    private static List<CardSet> allSets = new ArrayList<CardSet>();
    private static List<CardBlock> allBlocks = new ArrayList<CardBlock>();
    
    private static List<GameFormat> formats = new ArrayList<GameFormat>();
    private static GameFormat fmtStandard = null;
    private static GameFormat fmtExtended = null;
    private static GameFormat fmtModern = null;
    
    public static GameFormat getStandard() { return fmtStandard; }
    public static GameFormat getExtended() { return fmtExtended; }
    public static GameFormat getModern() { return fmtModern; }

    // list are immutable, no worries
    public static List<GameFormat> getFormats() { return formats; }
    public static List<CardBlock> getBlocks() { return allBlocks; }
    public static List<CardSet> getAllSets() { return allSets; } 
    
    // Perform that first of all
    static {
        loadSetData(loadBoosterData());
        loadBlockData();
        loadFormatData();
    }

    public static CardSet getSetByCode(final String code) {
        return setsByCode.get(code);
    }

    public static CardSet getSetByCodeOrThrow(final String code) {
        CardSet set = setsByCode.get(code);
        if (null == set) { throw new RuntimeException(String.format("Set with code '%s' not found", code)); }
        return set;
    }

    // used by image generating code
    public static String getCode2ByCode(final String code) {
        CardSet set = setsByCode.get(code);
        return set == null ? "" : set.getCode2();
    }
    
    private static Map<String, CardSet.BoosterData> loadBoosterData()
    {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/boosters.txt");
        Map<String, CardSet.BoosterData> result = new HashMap<String, CardSet.BoosterData>();
        
        
        for (String s : fData) {
            if (StringUtils.isBlank(s)) { continue; }
    
            String[] sParts = s.trim().split("\\|");
            String code = null;
            int nC = 0, nU = 0, nR = 0, nS = 0, nDF = 0;
            for (String sPart : sParts) {
                String[] kv = sPart.split(":", 2);
                String key = kv[0].toLowerCase();

                if (key.equalsIgnoreCase("Set")) { code = kv[1]; }
                if (key.equalsIgnoreCase("Commons")) { nC = Integer.parseInt(kv[1]); }
                if (key.equalsIgnoreCase("Uncommons")) { nU = Integer.parseInt(kv[1]); }
                if (key.equalsIgnoreCase("Rares")) { nR = Integer.parseInt(kv[1]); }
                if (key.equalsIgnoreCase("Special")) { nS = Integer.parseInt(kv[1]); }
                if (key.equalsIgnoreCase("DoubleFaced")) { nDF = Integer.parseInt(kv[1]); }
            }
            result.put(code, new CardSet.BoosterData(nC, nU, nR, nS, nDF));
        }
        return result;
    }

    // parser code - quite boring.
    private static void loadSetData(Map<String, CardSet.BoosterData> boosters) {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/setdata.txt");
    
        for (String s : fData) {
            if (StringUtils.isBlank(s)) { continue; }
    
            String[] sParts = s.trim().split("\\|");
            String code = null, code2 = null, name = null;
    
            int index = -1;
            String alias = null;
            for (String sPart : sParts) {
                String[] kv = sPart.split(":", 2);
                String key = kv[0].toLowerCase();
                if ("code3".equals(key)) {
                    code = kv[1];
                } else if ("code2".equals(key)) {
                    code2 = kv[1];
                } else if ("name".equals(key)) {
                    name = kv[1];
                } else if ("index".equals(key)) {
                    index = Integer.parseInt(kv[1]);
                } else if ("alias".equals(key)) {
                    alias = kv[1];
                }
            }
            CardSet set = new CardSet(index, name, code, code2, boosters.get(code));
            boosters.remove(code);
            setsByCode.put(code, set);
            if (alias != null) { setsByCode.put(alias, set); }
            allSets.add(set);
        }
        assert boosters.isEmpty();
        Collections.sort(allSets);
        allSets = Collections.unmodifiableList(allSets);
    }


    private static void loadBlockData() {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/blocks.txt");
    
        for (String s : fData) {
            if (StringUtils.isBlank(s)) { continue; }
            
            String[] sParts = s.trim().split("\\|");
    
            String name = null;
            int index = -1;
            List<CardSet> sets = new ArrayList<CardSet>(4);
            CardSet landSet = null;
            int draftBoosters = 3;
            int sealedBoosters = 6;
    
            for (String sPart : sParts) {
                String[] kv = sPart.split(":", 2);
                String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("index".equals(key)) {
                    index = Integer.parseInt(kv[1]);
                } else if ("set0".equals(key) || "set1".equals(key) || "set2".equals(key)) {
                    sets.add(getSetByCodeOrThrow(kv[1]));
                } else if ("landsetcode".equals(key)) {
                    landSet = getSetByCodeOrThrow(kv[1]);
                } else if ("draftpacks".equals(key)) {
                    draftBoosters = Integer.parseInt(kv[1]);
                } else if ("sealedpacks".equals(key)) {
                    sealedBoosters = Integer.parseInt(kv[1]);
                }
    
            }
            allBlocks.add(new CardBlock(index, name, sets , landSet, draftBoosters, sealedBoosters));
        }
        Collections.reverse(allBlocks);
        allBlocks = Collections.unmodifiableList(allBlocks);
    }
    
    private static void loadFormatData() {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/formats.txt");
        
        for (String s : fData) {
            if (StringUtils.isBlank(s)) { continue; }

            String name = null;
            List<String> sets = new ArrayList<String>(); // default: all sets allowed
            List<String> bannedCards = new ArrayList<String>(); // default: nothing banned

            String[] sParts = s.trim().split("\\|");
            for (String sPart : sParts) {
                String[] kv = sPart.split(":", 2);
                String key = kv[0].toLowerCase();
                if ("name".equals(key)) {
                    name = kv[1];
                } else if ("sets".equals(key)) {
                    sets.addAll(Arrays.asList(kv[1].split(", ")));
                } else if ("banned".equals(key)) {
                    bannedCards.addAll(Arrays.asList(kv[1].split("; ")));
                }
            }
            if( name == null ) { throw new RuntimeException("Format must have a name! Check formats.txt file"); }
            GameFormat thisFormat = new GameFormat(name, sets, bannedCards);
            if ( name.equalsIgnoreCase("Standard") ) { fmtStandard = thisFormat; }
            if ( name.equalsIgnoreCase("Modern") ) { fmtModern = thisFormat; }
            if ( name.equalsIgnoreCase("Extended") ) { fmtExtended = thisFormat; }
            formats.add(thisFormat);
        }
        formats = Collections.unmodifiableList(formats);
    }

}

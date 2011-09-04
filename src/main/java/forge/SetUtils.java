package forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.slightlymagic.maxmtg.Predicate;

import forge.card.CardBlock;
import forge.card.CardSet;

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
    private static HashMap<String, CardSet> setsByCode = new HashMap<String, CardSet>();
    private static List<CardSet> allSets = new ArrayList<CardSet>();
    private static List<CardBlock> allBlocks = new ArrayList<CardBlock>();

    // Perform that first of all
    static {
        loadSetData();
        loadBlockData();
    }

    public static CardSet getSetByCode(final String code) {
        return setsByCode.get(code);
    }

    public static CardSet getSetByCodeOrThrow(final String code) {
        CardSet set = setsByCode.get(code);
        if (null == set) { throw new RuntimeException(String.format("Set with code '%s' not found", code)); }
        return set;
    }

    // deckeditor again
    public static List<String> getNameList() {
        return Predicate.getTrue(CardSet.class).select(allSets, CardSet.fn1, CardSet.fnGetName);
    }

    // deckeditor needs this
    public static String getCode3ByName(final String setName) {
        for (CardSet s : setsByCode.values()) {
            if (s.getName().equals(setName)) { return s.getCode(); }
        }

        return "";
    }


    // used by image generating code
    public static String getCode2ByCode(final String code) {
        CardSet set = setsByCode.get(code);
        return set == null ? "" : set.getCode2();
    }

    public static List<CardBlock> getBlocks() {
        if (allBlocks.isEmpty()) { loadBlockData(); }
        return Collections.unmodifiableList(allBlocks);
    }


    // parser code - quite boring.
    private static void loadSetData() {
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
            CardSet set = new CardSet(index, name, code, code2);
            setsByCode.put(code, set);
            if (alias != null) { setsByCode.put(alias, set); }
            allSets.add(set);
        }
        Collections.sort(allSets);
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
    }

}

package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.slightlymagic.maxmtg.Predicate;

import forge.card.CardSet;

/**
 * <p>SetInfoUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class SetInfoUtil {
    /** Constant <code>setData</code> */
    private static HashMap<String, CardSet> setsByCode = new HashMap<String, CardSet>();
    private static List<CardSet> allSets = new ArrayList<CardSet>();

    /**
     * <p>loadSetData.</p>
     */
    private static void loadSetData() {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/setdata.txt");

        for (String s : fData) {
            if (s.length() < 6) { continue; }

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

    public static CardSet getSetByCode(final String code) {
        if (setsByCode.isEmpty()) { loadSetData(); }
        return setsByCode.get(code);
    }
    public static CardSet getSetByCodeOrThrow(final String code) {
        if (setsByCode.isEmpty()) { loadSetData(); }
        CardSet set = setsByCode.get(code);
        if (null == set) { throw new RuntimeException(String.format("Set with code '%s' not found", code)); }
        return set;
    }

    public static List<String> getCodeList() {
        if (setsByCode.isEmpty()) { loadSetData(); }
        return new ArrayList<String>(setsByCode.keySet());
    }

    /**
     * <p>getSetNameList.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public static List<String> getNameList() {
        if (setsByCode.isEmpty()) { loadSetData(); }

        return Predicate.getTrue(CardSet.class).select(allSets, CardSet.fn1, CardSet.fnGetName);
    }

    public static String getCode3ByName(final String setName) {
        if (setsByCode.isEmpty()) { loadSetData(); }

        for (CardSet s : setsByCode.values()) {
            if (s.getName().equals(setName)) { return s.getCode(); }
        }

        return "";
    }

    public static String getCode2ByCode(final String code) {
        if (setsByCode.isEmpty()) { loadSetData(); }
        CardSet set = setsByCode.get(code);
        return set == null ? "" : set.getCode2();
    }

    /**
     * <p>getSetName_SetCode3.</p>
     *
     * @param SetCode3 a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getNameByCode(String code) {
        if (setsByCode.isEmpty()) { loadSetData(); }
        CardSet set = setsByCode.get(code);
        return set == null ? "" : set.getName();
    }

    /**
     * <p>getMostRecentSet.</p>
     *
     * @param alSI a {@link java.util.ArrayList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostRecentSet(final ArrayList<SetInfo> alSI) {
        if (setsByCode.isEmpty()) { loadSetData(); }

        int size = alSI.size();
        if (size == 0) { return ""; }
        if (size == 1) { return alSI.get(0).Code; }

        CardSet[] sets = new CardSet[size];
        for (int i = 0; i < size; i++) { sets[i] = setsByCode.get(alSI.get(i).Code); }
        Arrays.sort(sets);

        return sets[sets.length - 1].getCode();
    }

    /**
     * <p>getSetInfo_Code.</p>
     *
     * @param SetList a {@link java.util.ArrayList} object.
     * @param SetCode a {@link java.lang.String} object.
     * @return a {@link forge.SetInfo} object.
     */
    public static SetInfo getSetInfo_Code(ArrayList<SetInfo> SetList, String SetCode) {
        SetInfo si;

        for (int i = 0; i < SetList.size(); i++) {
            si = SetList.get(i);
            if (si.Code.equals(SetCode)) { return si; }
        }

        return null;
    }

    /**
     * <p>getSetIndex.</p>
     *
     * @param SetCode a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getIndexByCode(final String code) {
        if (setsByCode.isEmpty()) { loadSetData(); }
        CardSet set = setsByCode.get(code);
        return set == null ? 0 : set.getIndex();
    }

    /** Constant <code>blockData</code> */
    private static ArrayList<HashMap<String, String>> blockData = new ArrayList<HashMap<String, String>>();

    /**
     * <p>loadBlockData.</p>
     */
    private static void loadBlockData() {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/blocks.txt");

        if (fData.size() > 0) {
            for (int i = 0; i < fData.size(); i++) {
                String s = fData.get(i);
                if (s.length() > 5) {
                    HashMap<String, String> sm = new HashMap<String, String>();

                    String ss[] = s.split("\\|");
                    for (int j = 0; j < ss.length; j++) {
                        String kv[] = ss[j].split(":");
                        sm.put(kv[0], kv[1]);
                    }

                    blockData.add(sm);
                }
            }

        }
    }

    /**
     * <p>getBlockNameList.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getBlockNameList() {
        ArrayList<String> bnl = new ArrayList<String>();

        if (blockData.size() == 0)
            loadBlockData();

        for (int i = 0; i < blockData.size(); i++)
            bnl.add(blockData.get(i).get("Name"));

        return bnl;
    }

    /**
     * <p>getSets_BlockName.</p>
     *
     * @param blockName a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getSets_BlockName(String blockName) {
        ArrayList<String> sets = new ArrayList<String>();

        if (blockData.size() == 0)
            loadBlockData();

        for (int i = 0; i < blockData.size(); i++) {
            if (blockData.get(i).get("Name").equals(blockName)) {
                if (blockData.get(i).containsKey("Set0"))
                    sets.add(blockData.get(i).get("Set0"));

                if (blockData.get(i).containsKey("Set1"))
                    sets.add(blockData.get(i).get("Set1"));

                if (blockData.get(i).containsKey("Set2"))
                    sets.add(blockData.get(i).get("Set2"));
            }
        }

        return sets;
    }

    /**
     * <p>getDraftPackCount.</p>
     *
     * @param blockName a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getDraftPackCount(String blockName) {
        if (blockData.size() == 0)
            loadBlockData();

        for (int i = 0; i < blockData.size(); i++) {
            if (blockData.get(i).get("Name").equals(blockName))
                return Integer.parseInt(blockData.get(i).get("DraftPacks"));
        }

        return 0;
    }

    /**
     * <p>getSealedPackCount.</p>
     *
     * @param blockName a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getSealedPackCount(String blockName) {
        if (blockData.size() == 0)
            loadBlockData();

        for (int i = 0; i < blockData.size(); i++) {
            if (blockData.get(i).get("Name").equals(blockName))
                return Integer.parseInt(blockData.get(i).get("SealedPacks"));
        }

        return 0;
    }

    /**
     * <p>getLandCode.</p>
     *
     * @param blockName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLandCode(String blockName) {
        if (blockData.size() == 0)
            loadBlockData();

        for (int i = 0; i < blockData.size(); i++) {
            if (blockData.get(i).get("Name").equals(blockName))
                return blockData.get(i).get("LandSetCode");
        }

        return "M11"; // default, should never happen IRL
    }
    
    public static ArrayList<String> getLegalSets(String fmt) {
    	ArrayList<String> lglSets = new ArrayList<String>();
    	
    	lglSets = FileUtil.readFile("res/blockdata/" + fmt + ".txt");
    	
    	return lglSets;
    }
}

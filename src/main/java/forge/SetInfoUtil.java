package forge;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>SetInfoUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class SetInfoUtil {
    /** Constant <code>setData</code> */
    private static ArrayList<HashMap<String, String>> setData = new ArrayList<HashMap<String, String>>();

    /**
     * <p>loadSetData.</p>
     */
    private static void loadSetData() {
        ArrayList<String> fData = FileUtil.readFile("res/blockdata/setdata.txt");

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

                    setData.add(sm);
                }
            }

        }
    }

    /**
     * <p>getSetCode2List.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getSetCode2List() {
        ArrayList<String> scl = new ArrayList<String>();

        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            scl.add(setData.get(i).get("Code2"));

        return scl;
    }

    /**
     * <p>getSetCode3List.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getSetCode3List() {
        ArrayList<String> scl = new ArrayList<String>();

        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            scl.add(setData.get(i).get("Code3"));

        return scl;
    }

    /**
     * <p>getSetNameList.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getSetNameList() {
        ArrayList<String> snl = new ArrayList<String>();

        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            snl.add(setData.get(i).get("Name"));

        return snl;
    }

    /**
     * <p>getSetCode2_SetName.</p>
     *
     * @param SetName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetCode2_SetName(String SetName) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Name").equals(SetName))
                return setData.get(i).get("Code2");

        return "";
    }

    /**
     * <p>getSetCode3_SetName.</p>
     *
     * @param SetName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetCode3_SetName(String SetName) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Name").equals(SetName))
                return setData.get(i).get("Code3");

        return "";
    }

    /**
     * <p>getSetCode2_SetCode3.</p>
     *
     * @param SetCode3 a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetCode2_SetCode3(String SetCode3) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Code3").equals(SetCode3))
                return setData.get(i).get("Code2");

        return "";
    }

    /**
     * <p>getSetCode3_SetCode2.</p>
     *
     * @param SetCode2 a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetCode3_SetCode2(String SetCode2) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Code2").equals(SetCode2))
                return setData.get(i).get("Code3");

        return "";
    }

    /**
     * <p>getSetName_SetCode2.</p>
     *
     * @param SetCode2 a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetName_SetCode2(String SetCode2) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Code2").equals(SetCode2))
                return setData.get(i).get("Name");

        return "";
    }

    /**
     * <p>getSetName_SetCode3.</p>
     *
     * @param SetCode3 a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getSetName_SetCode3(String SetCode3) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++)
            if (setData.get(i).get("Code3").equals(SetCode3))
                return setData.get(i).get("Name");

        return "";
    }

    /**
     * <p>getMostRecentSet.</p>
     *
     * @param alSI a {@link java.util.ArrayList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostRecentSet(ArrayList<SetInfo> alSI) {
        if (setData.size() == 0)
            loadSetData();

        int mostRecent = -1;

        for (SetInfo s : alSI) {
            for (int j = 0; j < setData.size(); j++) {
                if (setData.get(j).get("Code3").equals(s.Code)) {
                    if (j > mostRecent) {
                        mostRecent = j;
                        break;
                    }
                }
            }

        }

        if (mostRecent > -1)
            return setData.get(mostRecent).get("Code3");

        return "";
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
            if (si.Code.equals(SetCode))
                return si;
        }

        return null;
    }

    /**
     * <p>getSetIndex.</p>
     *
     * @param SetCode a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getSetIndex(String SetCode) {
        if (setData.size() == 0)
            loadSetData();

        for (int i = 0; i < setData.size(); i++) {
            if (setData.get(i).get("Code3").equals(SetCode))
                return Integer.parseInt(setData.get(i).get("Index"));
        }

        return 0;
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

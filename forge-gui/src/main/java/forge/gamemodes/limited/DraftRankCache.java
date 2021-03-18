package forge.gamemodes.limited;

/**
 * DraftRankCache 
 * Note: requires restart to load
 * @author arman.sepetci
 */
public class DraftRankCache {
    private static ReadDraftRankings rankings = null;
    private static ReadDraftRankings customRankings = null;
    private static String customRankingsFileName = "";
    
    private DraftRankCache(){
        customRankingsFileName = "";
    }

    public static Double getRanking(String name, String edition){
        if (rankings == null){
            rankings = new ReadDraftRankings();
        }
        return rankings.getRanking(name, edition);
    }
   
    public static Double getCustomRanking(String customRankingsSource, String name) {
        if (customRankings == null || !customRankingsFileName.equals(customRankingsSource)) {
            customRankingsFileName = customRankingsSource;
            customRankings = new ReadDraftRankings(customRankingsFileName);
        }
        return customRankings.getRanking(name, "CUSTOM");
    }
}

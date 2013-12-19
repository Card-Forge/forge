package forge.limited;

/**
 * DraftRankCache 
 * Note: requires restart to load
 * @author arman.sepetci
 */
public class DraftRankCache {
    private static ReadDraftRankings rankings = null;
    
    private DraftRankCache(){
        
    }

    public static Double getRanking(String name, String edition){
        if (rankings == null){
            rankings = new ReadDraftRankings();
        }
        return rankings.getRanking(name, edition);
    }
   
}

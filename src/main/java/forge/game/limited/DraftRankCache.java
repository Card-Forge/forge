package forge.game.limited;

/**
 * DraftRankCache 
 * Note: requires restart to load
 * @author arman.sepetci
 */
public class DraftRankCache {
    
    private static DraftRankCache instance = null;
    private static ReadDraftRankings rankings = null;
    
    private DraftRankCache(){
        
    }
    
    public static DraftRankCache getInstance(){
        if (instance == null) {
            instance = new DraftRankCache();
        }
        return instance;
    }
    
    @Override
    public DraftRankCache clone(){
        return null;
    }
    
    public static Double getRanking(String name, String edition){
        if (rankings == null){
            rankings = new ReadDraftRankings();
        }
        return rankings.getRanking(name, edition);
    }
   
}

package forge.quest;

import java.util.List;

import forge.quest.data.QuestData;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class SellRules {
    
    private int minWins = 0;
    private int cost = 250;
    private int minDifficulty = 0;
    private int maxDifficulty = 5;

    public SellRules(List<String> questShop) {
        if( null == questShop || questShop.isEmpty() ) return;
        
        for( String s : questShop ) {
            String[] kv = s.split("=");
            if( "WinsToUnlock".equalsIgnoreCase(kv[0]) )        minWins = Integer.parseInt(kv[1]);
            else if ("Credits".equalsIgnoreCase(kv[0]))         cost = Integer.parseInt(kv[1]);
            else if ("MaxDifficulty".equalsIgnoreCase(kv[0]))   maxDifficulty = Integer.parseInt(kv[1]);
            else if ("MinDifficulty".equalsIgnoreCase(kv[0]))   minDifficulty = Integer.parseInt(kv[1]);
        }
    }

    public boolean meetsRequiremnts(QuestData quest)
    {
        if( quest.getWin() < minWins ) return false;
        if( quest.getDifficultyIndex() < minDifficulty || quest.getDifficultyIndex() > maxDifficulty ) return false;
        
        return true;
    }

    public final int getCost() {
        return cost;
    }
    
    
    
    
}

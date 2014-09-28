package forge.game;

import java.util.HashSet;

//class for storing information during a game that is used at the end of the game to determine achievements
public class AchievementTracker {
    public final HashSet<String> activatedUltimates = new HashSet<String>();
    public int mulliganTo = 7;
    public int spellsCast = 0;
    public int maxStormCount = 0;
    public int landsPlayed = 0;
}

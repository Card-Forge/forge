package forge.game;

import java.util.EnumSet;
import java.util.Set;

public class GameRules {
    private GameType gameType;
    private boolean manaBurn;
    private int poisonCountersToLose = 10; // is commonly 10, but turns into 15 for 2HG
    private int gamesPerMatch = 3;
    private int gamesToWinMatch = 2;
    private boolean playForAnte = false;
    private boolean matchAnteRarity = false;
    private EnumSet<GameType> appliedVariants = EnumSet.noneOf(GameType.class);

    // it's a preference, not rule... but I could hardly find a better place for it
    public boolean canCloneUseTargetsImage;
    
    public GameRules(GameType type) {
        this.gameType = type;
    }

    public GameType getGameType() {
        return gameType;
    }

    /**
     * @return the manaBurn
     */
    public boolean hasManaBurn() {
        return manaBurn;
    }
    /**
     * @param manaBurn the manaBurn to set
     */
    public void setManaBurn(boolean manaBurn) {
        this.manaBurn = manaBurn;
    }
    /**
     * @return the poisonCountersToLose
     */
    public int getPoisonCountersToLose() {
        return poisonCountersToLose;
    }
    /**
     * @param poisonCountersToLose the poisonCountersToLose to set
     */
    public void setPoisonCountersToLose(int amount) {
        this.poisonCountersToLose = amount;
    }

    public int getGamesPerMatch() {
        return gamesPerMatch;
    }

    public void setGamesPerMatch(int gamesPerMatch) {
        this.gamesPerMatch = gamesPerMatch;
        this.gamesToWinMatch = gamesPerMatch / 2 + 1;
    }

    public boolean useAnte() {
        return playForAnte;
    }

    public void setPlayForAnte(boolean useAnte) {
        this.playForAnte = useAnte;
    }

    public boolean getMatchAnteRarity() {
        return matchAnteRarity;
    }

    public void setMatchAnteRarity(boolean matchRarity) {
        matchAnteRarity = matchRarity;
    }

    public int getGamesToWinMatch() {
        return gamesToWinMatch;
    }

    public void setAppliedVariants(Set<GameType> appliedVariants) {
        this.appliedVariants.addAll(appliedVariants);
    }

    public boolean hasAppliedVariant(GameType variant) {
        return appliedVariants.contains(variant);
    }

    public boolean hasCommander() {
        return appliedVariants.contains(GameType.Commander) || appliedVariants.contains(GameType.TinyLeaders);
    }
}

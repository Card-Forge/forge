package forge.game;

import java.util.EnumSet;
import java.util.Set;

public class GameRules {
    private final GameType gameType;
    private boolean manaBurn;
    private int poisonCountersToLose = 10; // is commonly 10, but turns into 15 for 2HG
    private int gamesPerMatch = 3;
    private int gamesToWinMatch = 2;
    private boolean playForAnte = false;
    private boolean matchAnteRarity = false;
    private final Set<GameType> appliedVariants = EnumSet.noneOf(GameType.class);

    // it's a preference, not rule... but I could hardly find a better place for it
    private boolean canCloneUseTargetsImage;

    public GameRules(final GameType type) {
        this.gameType = type;
    }

    public GameType getGameType() {
        return gameType;
    }

    public boolean hasManaBurn() {
        return manaBurn;
    }

    public void setManaBurn(final boolean manaBurn) {
        this.manaBurn = manaBurn;
    }

    public int getPoisonCountersToLose() {
        return poisonCountersToLose;
    }

    public void setPoisonCountersToLose(final int amount) {
        this.poisonCountersToLose = amount;
    }

    public int getGamesPerMatch() {
        return gamesPerMatch;
    }

    public void setGamesPerMatch(final int gamesPerMatch) {
        this.gamesPerMatch = gamesPerMatch;
        this.gamesToWinMatch = gamesPerMatch / 2 + 1;
    }

    public boolean useAnte() {
        return playForAnte;
    }

    public void setPlayForAnte(final boolean useAnte) {
        this.playForAnte = useAnte;
    }

    public boolean getMatchAnteRarity() {
        return matchAnteRarity;
    }

    public void setMatchAnteRarity(final boolean matchRarity) {
        matchAnteRarity = matchRarity;
    }

    public int getGamesToWinMatch() {
        return gamesToWinMatch;
    }

    public void setAppliedVariants(final Set<GameType> appliedVariants) {
        this.appliedVariants.addAll(appliedVariants);
    }

    public boolean hasAppliedVariant(final GameType variant) {
        return appliedVariants.contains(variant);
    }

    public boolean hasCommander() {
        return appliedVariants.contains(GameType.Commander) || appliedVariants.contains(GameType.TinyLeaders);
    }

    public boolean canCloneUseTargetsImage() {
        return canCloneUseTargetsImage;
    }
    public void setCanCloneUseTargetsImage(final boolean canCloneUseTargetsImage) {
        this.canCloneUseTargetsImage = canCloneUseTargetsImage;
    }
}

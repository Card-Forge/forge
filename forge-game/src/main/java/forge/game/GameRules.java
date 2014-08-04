package forge.game;

import java.util.EnumSet;
import java.util.Set;

import forge.game.io.GameStateDeserializer;
import forge.game.io.GameStateSerializer;
import forge.game.io.IGameStateObject;

public class GameRules implements IGameStateObject {
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

    @Override
    public void loadState(GameStateDeserializer gsd) {
        gameType = GameType.valueOf(gsd.readString());
        manaBurn = gsd.readBoolean();
        poisonCountersToLose = gsd.readInt();
        gamesPerMatch = gsd.readInt();
        gamesToWinMatch = gsd.readInt();
        playForAnte = gsd.readBoolean();
        matchAnteRarity = gsd.readBoolean();
        canCloneUseTargetsImage = gsd.readBoolean();

        int variantCount = gsd.readInt();
        appliedVariants.clear();
        for (int i = 0; i < variantCount; i++) {
            appliedVariants.add(GameType.valueOf(gsd.readString()));
        }
    }

    @Override
    public void saveState(GameStateSerializer gss) {
        gss.write(gameType.name());
        gss.write(manaBurn);
        gss.write(poisonCountersToLose);
        gss.write(gamesPerMatch);
        gss.write(gamesToWinMatch);
        gss.write(playForAnte);
        gss.write(matchAnteRarity);
        gss.write(canCloneUseTargetsImage);

        gss.write(appliedVariants.size());
        for (GameType variant : appliedVariants) {
            gss.write(variant.name());
        }
    }
    
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
}

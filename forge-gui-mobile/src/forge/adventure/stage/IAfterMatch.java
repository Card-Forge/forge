package forge.adventure.stage;

public interface IAfterMatch {
    default void setWinner(boolean winner, boolean isArena) {
        String playerResultText = "LOST!";
        if (winner) {
            playerResultText = "WON!";
        }
        System.out.println("FORGE_ARCHIPELAGO DETECTED GAME CONCLUSION. THE PLAYER HAS " + playerResultText);
    }
}

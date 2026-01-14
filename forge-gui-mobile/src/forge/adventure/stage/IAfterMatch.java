package forge.adventure.stage;

public interface IAfterMatch {
    default void setWinner(boolean winner, boolean isArena) {
        // Todo: Print a message on the screen to show that we know the player won
        System.out.println("FORGE_ARCHIPELAGO DETECTED GAME CONCLUSION.");
    }
}

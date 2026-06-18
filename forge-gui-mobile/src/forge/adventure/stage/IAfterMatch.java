package forge.adventure.stage;

import forge.adventure.archipelago.ArchipelagoData;

public interface IAfterMatch {
    default void setWinner(boolean winner, boolean isArena) {
        String playerResultText = "LOST!";
        if (winner) {
            playerResultText = "WON!";
            ArchipelagoData.getInstance().addTotalBattlesWon(1);
        }
        System.out.println("FORGE_ARCHIPELAGO: DETECTED GAME CONCLUSION. THE PLAYER HAS " + playerResultText);
    }
}

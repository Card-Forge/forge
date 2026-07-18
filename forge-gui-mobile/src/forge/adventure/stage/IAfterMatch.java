package forge.adventure.stage;

import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.ArchipelagoMode;

public interface IAfterMatch {
    default void setWinner(boolean winner, boolean isArena) {
        if (ArchipelagoData.getInstance().getArchipelagoMode() != ArchipelagoMode.disabled) {
            String playerResultText = "LOST!";
            if (winner) {
                playerResultText = "WON!";
                ArchipelagoData.getInstance().addTotalBattlesWon(1);
            }
            System.out.println("Randomizer: Battle Concluded. The player has " + playerResultText);
        }
    }
}

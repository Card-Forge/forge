package forge.adventure.stage;

import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.ArchipelagoMode;

public interface IAfterMatch {
    default void setWinner(boolean winner, boolean isArena) {
        if (ArchipelagoData.getInstance().getArchipelagoMode() != ArchipelagoMode.disabled && winner) {
            ArchipelagoData.getInstance().addTotalBattlesWon(1);
        }
    }
}

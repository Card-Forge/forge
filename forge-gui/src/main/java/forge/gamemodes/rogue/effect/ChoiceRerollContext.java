package forge.gamemodes.rogue.effect;

import java.util.List;

/**
 * Mutable context for optional reroll buttons on Rogue choice dialogs.
 */
public class ChoiceRerollContext {
    public int remainingRerolls;
    public List<ChestEffect> forcedChestChoices;
}

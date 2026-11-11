package forge.gamemodes.rogue.path;

import forge.gamemodes.rogue.effect.ChestEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Chest node in a Rogue Commander path.
 * Chest nodes provide a random reward ('loot') from the ChestEffect pool.
 */
public class NodeChest extends RoguePathNode {

    private List<ChestEffect> chestEffects;

    public NodeChest() {
        super();
    }

    public NodeChest(List<ChestEffect> chestEffects) {
        super();
        setChestEffects(chestEffects);
    }

    public List<ChestEffect> getChestEffects() {
        return chestEffects != null ? chestEffects : new ArrayList<>();
    }

    public void setChestEffects(List<ChestEffect> chestEffects) {
        this.chestEffects = chestEffects != null ? new ArrayList<>(chestEffects) : new ArrayList<>();
    }

    @Override
    public boolean isSideNode() {
        return true;
    }

    @Override
    public String toString() {
        List<ChestEffect> effects = getChestEffects();
        return !effects.isEmpty()
            ? "Loot (" + String.join(" / ", effects.stream().map(ChestEffect::getDisplayName).collect(Collectors.toList())) + ")"
            : "Loot (Treasure)";
    }
}

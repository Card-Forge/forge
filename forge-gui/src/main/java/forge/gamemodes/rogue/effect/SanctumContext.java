package forge.gamemodes.rogue.effect;

import forge.gamemodes.rogue.npc.NPCContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context for Sanctum interactions.
 */
public class SanctumContext {
    public record SanctumChoice(String id, String label, String description) {}

    public boolean restLifeGainDisabled;
    public boolean restFullHeal;
    public int restMaxLifeCost;
    public final List<NPCContext> preSanctumDialogs = new ArrayList<>();
    public final List<SanctumChoice> extraChoices = new ArrayList<>();
}

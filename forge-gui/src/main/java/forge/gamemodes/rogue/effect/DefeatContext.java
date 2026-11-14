package forge.gamemodes.rogue.effect;

/**
 * Mutable context for onDefeat triggers.
 * If an effect sets revived=true, the run continues with reviveLife.
 */
public class DefeatContext {
    public boolean revived;
    public int reviveLife;
}

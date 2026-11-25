package forge.gamemodes.rogue.effect;

/**
 * Mutable context for onCardSelection triggers (card rewards AND bazaar).
 * Effects accumulate into this object; the caller reads the final state.
 */
public class CardSelectionContext {
    public int extraMythics;
    public int freeRerolls;
}

package forge.gamemodes.rogue.effect;

/**
 * Mutable context for onBeforeGainLife triggers.
 * Effects can reduce or prevent explicit Rogue-side life gain before it is applied.
 */
public class GainLifeContext {
    public int amount;

    public GainLifeContext(int amount) {
        this.amount = amount;
    }
}

package forge.gamemodes.rogue.effect;

import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared interface for planebound node effects (Wrathful, Cursed).
 * Provides common CONSUME behavior and shared utility methods.
 */
public interface PlaneboundEffect extends RogueEffect {

    enum PlaneboundEffectCategory { WRATHFUL, CURSED }

    PlaneboundEffectCategory getCategory();

    @Override
    default EffectType getEffectType() { return EffectType.CONSUME; }

    @Override
    default int getChargesForRank(int rank) { return 1; }

    /** Pick a random enum value excluding the given set. Falls back to any value if all excluded. */
    static <E extends Enum<E> & PlaneboundEffect> E getRandomExcluding(E[] values, Set<E> exclude) {
        List<E> candidates = new ArrayList<>();
        for (E v : values) {
            if (!exclude.contains(v)) candidates.add(v);
        }
        if (candidates.isEmpty()) {
            return values[MyRandom.getRandom().nextInt(values.length)];
        }
        return candidates.get(MyRandom.getRandom().nextInt(candidates.size()));
    }

    /** Cross-type lookup: tries Wrathful first, then Cursed. */
    static PlaneboundEffect fromId(String id) {
        WrathfulEffect w = WrathfulEffect.fromId(id);
        if (w != null) return w;
        return CursedEffect.fromId(id);
    }
}

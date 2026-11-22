package forge.ai;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Assigns coloured mana requirements to distinct physical mana sources. */
public final class ManaSourceAllocation {
    private ManaSourceAllocation() {
    }

    /**
     * Whether these sources can cover the coloured shards in a mana cost.
     * A source with several colours can pay for one shard of any of those colours, but it cannot pay for two shards
     * at once. Phyrexian, two-or-generic, and colorless-hybrid shards are left to the caller's alternate-payment checks.
     */
    public static boolean canPayColoredPips(final ManaCost cost, final List<Byte> sourceColors) {
        final List<Byte> requirements = new ArrayList<>();
        for (final ManaCostShard shard : cost) {
            if (shard.isPhyrexian() || shard.isOr2Generic() || shard.isColorless()) {
                continue;
            }
            final byte colors = shard.getColorMask();
            if (colors != 0) {
                requirements.add(colors);
            }
        }
        return canPayColoredPips(requirements, sourceColors);
    }

    /**
     * Match each required colour mask to a different source. The augmenting-path step matters for flexible sources:
     * for example, an Underground Sea can cover either {U} or {B}, while an Island can cover the other one.
     */
    public static boolean canPayColoredPips(final List<Byte> requirements, final List<Byte> sourceColors) {
        if (requirements.isEmpty()) {
            return true;
        }
        if (requirements.size() > sourceColors.size()) {
            return false;
        }

        // Source -> requirement currently assigned to it. Each source has capacity one.
        final int[] assigned = new int[sourceColors.size()];
        Arrays.fill(assigned, -1);
        final int[] visited = new int[sourceColors.size()];
        int visit = 0;
        for (int i = 0; i < requirements.size(); i++) {
            if (!assign(i, requirements, sourceColors, assigned, visited, ++visit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assigns as many optional coloured pips as possible after assigning every required pip.
     * Optional pips represent Phyrexian symbols: an unassigned one can instead be paid with life.
     * Returns -1 when the required pips cannot all be assigned.
     */
    public static int maxPayableOptionalColoredPips(final List<Byte> requirements,
                                                     final List<Byte> optionalRequirements,
                                                     final List<Byte> sourceColors) {
        if (requirements.size() > sourceColors.size()) {
            return -1;
        }
        final List<Byte> allRequirements = new ArrayList<>(requirements.size() + optionalRequirements.size());
        allRequirements.addAll(requirements);
        allRequirements.addAll(optionalRequirements);
        final int[] assigned = new int[sourceColors.size()];
        Arrays.fill(assigned, -1);
        final int[] visited = new int[sourceColors.size()];
        int visit = 0;
        for (int i = 0; i < requirements.size(); i++) {
            if (!assign(i, allRequirements, sourceColors, assigned, visited, ++visit)) {
                return -1;
            }
        }
        int matched = 0;
        for (int i = 0; i < optionalRequirements.size(); i++) {
            if (assign(requirements.size() + i, allRequirements, sourceColors, assigned, visited, ++visit)) {
                matched++;
            }
        }
        return matched;
    }

    private static boolean assign(final int requirement, final List<Byte> requirements, final List<Byte> sourceColors,
                                  final int[] assigned, final int[] visited, final int visit) {
        final byte wanted = requirements.get(requirement);
        for (int i = 0; i < sourceColors.size(); i++) {
            if (visited[i] == visit || (sourceColors.get(i) & wanted) == 0) {
                continue;
            }
            visited[i] = visit;
            if (assigned[i] < 0 || assign(assigned[i], requirements, sourceColors, assigned, visited, visit)) {
                assigned[i] = requirement;
                return true;
            }
        }
        return false;
    }
}

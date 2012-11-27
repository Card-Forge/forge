package forge.game.phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public enum PhaseType {
    MULLIGAN("Mulligan"),
    // Note: Mulligan is not part of "All Phases" which are strictly ingame phases
    UNTAP("Untap"),
    UPKEEP("Upkeep"),
    DRAW("Draw"),
    MAIN1("Main1"),
    COMBAT_BEGIN("BeginCombat"),
    COMBAT_DECLARE_ATTACKERS("Declare Attackers"),
    COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY("Declare Attackers - Play Instants and Abilities"),
    COMBAT_DECLARE_BLOCKERS("Declare Blockers"),
    COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY("Declare Blockers - Play Instants and Abilities"),
    COMBAT_FIRST_STRIKE_DAMAGE("First Strike Damage"),
    COMBAT_DAMAGE("Combat Damage"),
    COMBAT_END("EndCombat"),
    MAIN2("Main2"),
    END_OF_TURN("End of Turn"),
    CLEANUP("Cleanup");

    public static final List<PhaseType> ALL_PHASES = Collections.unmodifiableList(
            Arrays.asList(
                    UNTAP, UPKEEP, DRAW, MAIN1,
                    COMBAT_BEGIN, COMBAT_DECLARE_ATTACKERS, COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY,
                    COMBAT_DECLARE_BLOCKERS, COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY,
                    COMBAT_FIRST_STRIKE_DAMAGE, COMBAT_DAMAGE, COMBAT_END,
                    MAIN2, END_OF_TURN, CLEANUP
                    )
            );

    public final String Name;
    private PhaseType(String name) {
        Name = name;
    }

    public final boolean isAfter(final PhaseType phase) {
        return ALL_PHASES.indexOf(this) > ALL_PHASES.indexOf(phase);
    }

    public final boolean isMain() {
        return this == MAIN1 || this == MAIN2;
    }

    public final boolean isBefore(final PhaseType phase) {
        return ALL_PHASES.indexOf(this) < ALL_PHASES.indexOf(phase);
    }

    public static PhaseType smartValueOf(final String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final PhaseType v : PhaseType.values()) {
            if (v.Name.compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum PhaseType");
    }

    public static List<PhaseType> listValueOf(final String values) {
        final List<PhaseType> result = new ArrayList<PhaseType>();
        for (final String s : values.split("[, ]+")) {
            result.add(PhaseType.smartValueOf(s));
        }
        return result;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param string
     * @return
     */
    public static List<PhaseType> parseRange(String values) {
        final List<PhaseType> result = new ArrayList<PhaseType>();
        for (final String s : values.split(",")) {
            int idxArrow = s.indexOf("->");
            if (idxArrow >= 0) {
                PhaseType from = PhaseType.smartValueOf(s.substring(0, idxArrow));
                String sTo = s.substring(idxArrow + 2);
                PhaseType to = StringUtils.isBlank(sTo) ? PhaseType.CLEANUP : PhaseType.smartValueOf(sTo);
                int iFrom = ALL_PHASES.indexOf(from);
                int iTo = ALL_PHASES.indexOf(to);
                for (int i = iFrom; i <= iTo; i++) {
                    result.add(ALL_PHASES.get(i));
                }
            }
            else {
                result.add(PhaseType.smartValueOf(s));
            }
        }
        return result;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public PhaseType getNextPhase() {
        int iNext = ALL_PHASES.indexOf(this) + 1;
        while (iNext >= ALL_PHASES.size()) {
            iNext = 0;
        }
        return ALL_PHASES.get(iNext);
    }
}

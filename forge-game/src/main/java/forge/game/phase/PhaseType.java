package forge.game.phase;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public enum PhaseType {
    UNTAP("Untap"),
    UPKEEP("Upkeep"),
    DRAW("Draw"),
    MAIN1("Main, precombat", "Main1"),
    COMBAT_BEGIN("Begin Combat", "BeginCombat"),
    COMBAT_DECLARE_ATTACKERS("Declare Attackers"),
    COMBAT_DECLARE_BLOCKERS("Declare Blockers"),
    COMBAT_FIRST_STRIKE_DAMAGE("First Strike Damage"),
    COMBAT_DAMAGE("Combat Damage"),
    COMBAT_END("End Combat", "EndCombat"),
    MAIN2("Main, postcombat", "Main2"),
    END_OF_TURN("End of Turn"),
    CLEANUP("Cleanup");

    public static final List<PhaseType> ALL_PHASES = Collections.unmodifiableList(
            Arrays.asList(
                    UNTAP, UPKEEP, DRAW, 
                    MAIN1,
                    COMBAT_BEGIN, COMBAT_DECLARE_ATTACKERS, COMBAT_DECLARE_BLOCKERS, COMBAT_FIRST_STRIKE_DAMAGE, COMBAT_DAMAGE, COMBAT_END,
                    MAIN2, 
                    END_OF_TURN, CLEANUP
                    )
            );

    public final String nameForUi;
    public final String nameForScripts;
    
    PhaseType(String name) {
        this(name, name);
    }
    PhaseType(String name, String name_for_scripts) {
        nameForUi = name;
        nameForScripts = name_for_scripts;
    }


    public final boolean phaseforUpdateField() {
        boolean result =
                ((ALL_PHASES.indexOf(this) >= ALL_PHASES.indexOf(UNTAP)
                && ALL_PHASES.indexOf(this) < ALL_PHASES.indexOf(COMBAT_FIRST_STRIKE_DAMAGE))
                || (ALL_PHASES.indexOf(this) >= ALL_PHASES.indexOf(MAIN2)
                && ALL_PHASES.indexOf(this) < ALL_PHASES.indexOf(CLEANUP)));
        return result;
    }

    public final boolean isCombatPhase() {
        return ((ALL_PHASES.indexOf(this) >=  ALL_PHASES.indexOf(COMBAT_BEGIN))
        && (ALL_PHASES.indexOf(this) <=  ALL_PHASES.indexOf(COMBAT_END)));
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
            if (v.nameForScripts.equalsIgnoreCase(valToCompate)|| v.name().equalsIgnoreCase(valToCompate)) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum PhaseType");
    }

    /**
     * TODO: Write javadoc for this method.
     * @param string
     * @return
     */
    public static Set<PhaseType> parseRange(String values) {
        final Set<PhaseType> result = EnumSet.noneOf(PhaseType.class);
        for (final String s : values.split(",")) {
            int idxArrow = s.indexOf("->");
            if (idxArrow >= 0) {
                PhaseType from = PhaseType.smartValueOf(s.substring(0, idxArrow));
                String sTo = s.substring(idxArrow + 2);
                PhaseType to = StringUtils.isBlank(sTo) ? PhaseType.CLEANUP : PhaseType.smartValueOf(sTo);

                result.addAll(EnumSet.range(from, to));
            }
            else {
                result.add(PhaseType.smartValueOf(s));
            }
        }
        return result;
    }

    /**
     * Get the next PhaseType in turn order. 
     * @return
     */
    public static PhaseType getNext(PhaseType current) {
        int iNext = ALL_PHASES.indexOf(current) + 1;
        if (iNext >= ALL_PHASES.size()) {
            iNext = 0;
        }
        return ALL_PHASES.get(iNext);
    }
}

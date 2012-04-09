package forge.game.phase;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public enum PhaseType {
    UNTAP("Untap", 0),
    UPKEEP("Upkeep", 1),
    DRAW("Draw", 2),
    MAIN1("Main1", 3),
    COMBAT_BEGIN("BeginCombat", 4),
    COMBAT_DECLARE_ATTACKERS("Declare Attackers", 5),
    COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY("Declare Attackers - Play Instants and Abilities", 6),
    COMBAT_DECLARE_BLOCKERS("Declare Blockers", 7),
    COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY("Declare Blockers - Play Instants and Abilities", 8),
    COMBAT_FIRST_STRIKE_DAMAGE("First Strike Damage", 9),
    COMBAT_DAMAGE("Combat Damage", 10),
    COMBAT_END("EndCombat", 11),
    MAIN2("Main2", 12),
    END_OF_TURN("End of Turn", 13),
    CLEANUP("Cleanup", 14);

    public final String Name;
    public final int Index;
    private PhaseType(String name, int index) {
        Name = name;
        Index = index;
    }

    public static PhaseType getByIndex(int idx) {
        for (PhaseType ph : PhaseType.values()) {

            if (ph.Index == idx) {
                return ph;
            }
        }
        throw new InvalidParameterException("No PhaseType found with index " + idx);
    }

    public final boolean isAfter(final PhaseType phase) {
        return this.Index > phase.Index;
    }

    public final boolean isMain() {
        return this == MAIN1 || this == MAIN2;
    }


    public final boolean isBefore(final PhaseType phase) {
        return this.Index < phase.Index;
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
                for (int i = from.Index; i <= to.Index; i++) {
                    result.add(PhaseType.getByIndex(i));
                }
            }
            else {
                result.add(PhaseType.smartValueOf(s));
            }
        }
        return result;
    }
}

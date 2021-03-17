package forge.game.phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.util.Localizer;


public enum PhaseType {
    UNTAP("lblUntapStep", "Untap"),
    UPKEEP("lblUpkeepStep", "Upkeep"),
    DRAW("lblDrawStep", "Draw"),
    MAIN1("lblMainPhase1", "Main1"),
    COMBAT_BEGIN("lblCombatBeginStep", "BeginCombat"),
    COMBAT_DECLARE_ATTACKERS("lblCombatDeclareAttackersStep", "Declare Attackers"),
    COMBAT_DECLARE_BLOCKERS("lblCombatDeclareBlockersStep", "Declare Blockers"),
    COMBAT_FIRST_STRIKE_DAMAGE("lblCombatFirstStrikeDamageStep", "First Strike Damage"),
    COMBAT_DAMAGE("lblCombatDamageStep", "Combat Damage"),
    COMBAT_END("lblCombatEndStep", "EndCombat"),
    MAIN2("lblMainPhase2", "Main2"),
    END_OF_TURN("lblEndStep", "End of Turn"),
    CLEANUP("lblCleanupStep", "Cleanup");

    public static final List<List<PhaseType>> PHASE_GROUPS = Arrays.asList(
                    Arrays.asList(UNTAP, UPKEEP, DRAW),
                    Arrays.asList(MAIN1),
                    Arrays.asList(COMBAT_BEGIN, COMBAT_DECLARE_ATTACKERS, COMBAT_DECLARE_BLOCKERS, COMBAT_FIRST_STRIKE_DAMAGE, COMBAT_DAMAGE, COMBAT_END),
                    Arrays.asList(MAIN2),
                    Arrays.asList(END_OF_TURN)
            );

    public static List<PhaseType> AllPhases() {
        return AllPhases(false);
    }

    public static List<PhaseType> AllPhases(boolean isTopsy) {
        List<PhaseType> result = new ArrayList<PhaseType>();
        List<List<PhaseType>> phase_groups = PHASE_GROUPS;
        if (isTopsy) {
            phase_groups = Lists.reverse(phase_groups);
        }
        for (final List<PhaseType> group : phase_groups) {
            result.addAll(group);
        }
        result.add(CLEANUP); // Some cards get confused if cleanup isn't last, it works better this way.

        return result;
    }

    public final String nameForUi;
    public final String nameForScripts;
    
    PhaseType(String name, String name_for_scripts) {
        nameForUi = Localizer.getInstance().getMessage(name);
        nameForScripts = name_for_scripts;
    }


    public final boolean phaseforUpdateField() {
        return ((AllPhases().indexOf(this) >= AllPhases().indexOf(UNTAP)
                && AllPhases().indexOf(this) < AllPhases().indexOf(COMBAT_FIRST_STRIKE_DAMAGE))
                || (AllPhases().indexOf(this) >= AllPhases().indexOf(MAIN2)
                && AllPhases().indexOf(this) < AllPhases().indexOf(CLEANUP)));
    }

    public final boolean isCombatPhase() {
        return ((AllPhases().indexOf(this) >=  AllPhases().indexOf(COMBAT_BEGIN))
                && (AllPhases().indexOf(this) <=  AllPhases().indexOf(COMBAT_END)));
    }

    public final boolean isAfter(final PhaseType phase) {
        return AllPhases().indexOf(this) > AllPhases().indexOf(phase);
    }

    public final boolean isMain() {
        return this == MAIN1 || this == MAIN2;
    }

    public final boolean isBefore(final PhaseType phase) {
        return isBefore(phase, false);
    }

    public final boolean isBefore(final PhaseType phase, boolean isTopsy) {
        return AllPhases(isTopsy).indexOf(this) < AllPhases(isTopsy).indexOf(phase);
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

    public static boolean isLast(PhaseType current, boolean isTopsy) {
        if (current == null) {
            return true;
        }
        return AllPhases(isTopsy).indexOf(current) == AllPhases(isTopsy).size() - 1;
    }

    /**
     * Get the next PhaseType in turn order. 
     * @return
     */
    public static PhaseType getNext(PhaseType current, boolean isTopsy) {
        int iNext = AllPhases(isTopsy).indexOf(current) + 1;
        if (iNext >= AllPhases(isTopsy).size()) {
            iNext = 0;
        }
        return AllPhases(isTopsy).get(iNext);
    }
}

package forge.game.phase;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

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
                    Arrays.asList(END_OF_TURN),
                    Arrays.asList(CLEANUP)
            );

    private static final Map<PhaseType, Integer> PHASE_INDEX = initializePhaseIndex();

    private static final Map<PhaseType, Integer> initializePhaseIndex() {
        Map<PhaseType, Integer> phaseIndex = Maps.newEnumMap(PhaseType.class);
        phaseIndex.put(UNTAP, 0);
        phaseIndex.put(UPKEEP, 0);
        phaseIndex.put(DRAW, 0);
        phaseIndex.put(MAIN1, 1);
        phaseIndex.put(COMBAT_BEGIN, 2);
        phaseIndex.put(COMBAT_DECLARE_ATTACKERS, 2);
        phaseIndex.put(COMBAT_DECLARE_BLOCKERS, 2);
        phaseIndex.put(COMBAT_FIRST_STRIKE_DAMAGE, 2);
        phaseIndex.put(COMBAT_DAMAGE, 2);
        phaseIndex.put(COMBAT_END, 2);
        phaseIndex.put(MAIN2, 3);
        phaseIndex.put(END_OF_TURN, 4);
        phaseIndex.put(CLEANUP, 5);
        return phaseIndex;
    }

    public final String nameForUi;
    public final String nameForScripts;
    
    PhaseType(String name, String name_for_scripts) {
        nameForUi = Localizer.getInstance().getMessage(name);
        nameForScripts = name_for_scripts;
    }

    public final boolean isAfter(final PhaseType phase) {
        return isBefore(phase, true);
    }

    public final boolean isMain() {
        return this == MAIN1 || this == MAIN2;
    }

    public final boolean isBefore(final PhaseType phase) {
        return isBefore(phase, false);
    }

    public final boolean isBefore(final PhaseType phase, boolean isTopsy) {
        int thisPhaseIndex = PHASE_INDEX.get(this);
        int cmpPhaseIndex = PHASE_INDEX.get(phase);
        if (thisPhaseIndex == cmpPhaseIndex) {
            final List<PhaseType> phaseGroup = PHASE_GROUPS.get(thisPhaseIndex);
            return isTopsy ? phaseGroup.indexOf(this) > phaseGroup.indexOf(phase) : phaseGroup.indexOf(this) < phaseGroup.indexOf(phase);
        }
        return isTopsy ? thisPhaseIndex > cmpPhaseIndex : thisPhaseIndex < cmpPhaseIndex;
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
            } else {
                result.add(PhaseType.smartValueOf(s));
            }
        }
        return result;
    }

    public static boolean isLast(PhaseType current, boolean isTopsy) {
        if (current == null) {
            return true;
        }
        // Some cards get confused if cleanup isn't last (comment from who initially implemented Topsy Turvy)
        // So the last phase will always be CLEANUP even if isTopsy == true
        return current == CLEANUP;
    }

    /**
     * Get the next PhaseType in turn order. 
     * @return
     */
    public static PhaseType getNext(PhaseType current, boolean isTopsy) {
        if (current == null) return PHASE_GROUPS.get(0).get(0);
        int phaseIndex = PHASE_INDEX.get(current);
        final List<PhaseType> phaseGroup = PHASE_GROUPS.get(phaseIndex);
        int nextStepIndex = phaseGroup.indexOf(current) + 1;
        if (nextStepIndex >= phaseGroup.size()) {
            nextStepIndex = 0;
            if (!isTopsy) {
                phaseIndex += 1;
                if (phaseIndex >= PHASE_GROUPS.size()) {
                    phaseIndex = 0;
                }
            } else {
                phaseIndex -= 1;
                if (phaseIndex < 0) {
                    phaseIndex = PHASE_GROUPS.size() - 1;
                }
            }
        }
        return PHASE_GROUPS.get(phaseIndex).get(nextStepIndex);
    }
}

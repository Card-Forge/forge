package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.phase.ExtraPhase;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AddPhaseEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        boolean isTopsy = sa.getActivatingPlayer().getAmountOfKeyword("The phases of your turn are reversed.") % 2 == 1;
        PhaseHandler phaseHandler = sa.getActivatingPlayer().getGame().getPhaseHandler();
        PhaseType currentPhase = phaseHandler.getPhase();

        // Check for World at War - may need to be moved to SpellAbilityCondition later?
        if (sa.hasParam("BeforeFirstPostCombatMainEnd")) {
            if (!phaseHandler.beforeFirstPostCombatMainEnd()) {
                return;
            }
        }

        PhaseType afterPhase;
        if (sa.hasParam("AfterPhase")) {
            afterPhase = PhaseType.smartValueOf(sa.getParam("AfterPhase"));
        } else {
            // If "AfterPhase" param is missing it means the added Phase comes afterPhase this Phase
            afterPhase = currentPhase;
        }

        // The original next phase following afterPhase
        PhaseType nextPhase = PhaseType.getNext(afterPhase, isTopsy);
        List<PhaseType> extraPhaseList = new ArrayList<>();

        // Insert ExtraPhase
        String extra = sa.getParam("ExtraPhase");
        if (extra.equals("Beginning")) {
            extraPhaseList.addAll(PhaseType.PHASE_GROUPS.get(0));
        } else if (extra.equals("Combat")) {
            extraPhaseList.addAll(PhaseType.PHASE_GROUPS.get(2));
        } else {    // Currently no effect will add End Phase
            extraPhaseList.add(PhaseType.smartValueOf(extra));
        }

        // Insert FollowedBy
        if (sa.hasParam("FollowedBy")) {
            // Currently all FollowedBy are Main2 phase, which has no step
            PhaseType followingExtra = PhaseType.smartValueOf(sa.getParam("FollowedBy"));
            extraPhaseList.add(followingExtra);
        }

        ExtraPhase extraPhase = phaseHandler.addExtraPhase(afterPhase, extraPhaseList, nextPhase);

        if (sa.hasParam("ExtraPhaseDelayedTrigger")) {
            final Trigger delTrig = TriggerHandler.parseTrigger(sa.getSVar(sa.getParam("ExtraPhaseDelayedTrigger")), sa.getHostCard(), true);
            SpellAbility overridingSA = AbilityFactory.getAbility(sa.getSVar(sa.getParam("ExtraPhaseDelayedTriggerExcute")), sa.getHostCard());
            overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
            delTrig.setOverridingAbility(overridingSA);
            extraPhase.addTrigger(delTrig);
        }
    }
}

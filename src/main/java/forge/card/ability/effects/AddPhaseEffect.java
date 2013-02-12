package forge.card.ability.effects;

import forge.Singletons;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AddPhaseEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {
        PhaseHandler phaseHandler = Singletons.getModel().getGame().getPhaseHandler();
        PhaseType extra = PhaseType.smartValueOf(sa.getParam("ExtraPhase"));

        PhaseType after;
        if (sa.hasParam("AfterPhase")) {
            after = PhaseType.smartValueOf(sa.getParam("AfterPhase"));
        }
        else {
            // If "AfterPhase" param is missing it means the added Phase comes after this Phase
            after = phaseHandler.getPhase();
        }
        phaseHandler.addExtraPhase(after, extra);
        
        if (sa.hasParam("FollowedBy")) {
            String followedBy = sa.getParam("FollowedBy");
            PhaseType followingExtra;
            if ("ThisPhase".equals(followedBy)) {
                followingExtra = phaseHandler.getPhase();
            } else {
                followingExtra = PhaseType.smartValueOf(followedBy);
            }
            PhaseType followingAfter = extra.equals(PhaseType.COMBAT_BEGIN) ? PhaseType.COMBAT_END : extra;
            phaseHandler.addExtraPhase(followingAfter, followingExtra);
        }
    }
}

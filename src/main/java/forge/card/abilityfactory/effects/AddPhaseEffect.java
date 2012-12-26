package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
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
            after = phaseHandler.getPhase();
        }
        
        phaseHandler.addExtraPhase(after, extra);
    }

}

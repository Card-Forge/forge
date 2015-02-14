package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class PossibleTargetSelector {
    private SpellAbility sa;
    private TargetRestrictions tgt;
    private int targetIndex;
    private List<GameObject> validTargets;

    public PossibleTargetSelector(Game game, Player self, SpellAbility sa) {
        this.sa = sa;
        this.tgt = sa.getTargetRestrictions();
        this.targetIndex = 0;
        this.validTargets = new ArrayList<GameObject>();
        for (GameObject o : tgt.getAllCandidates(sa, true)) {
            validTargets.add(o);
        }
    }
 
    public boolean selectNextTargets() {
        if (targetIndex >= validTargets.size()) {
            return false;
        }
        sa.resetTargets();
        int index = targetIndex;
        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa) && index < validTargets.size()) {
            sa.getTargets().add(validTargets.get(index++));
        }
        // TODO: smarter about multiple targets, identical targets, etc...
        targetIndex++;
        return true;
    }
}

package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
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
        sa.setActivatingPlayer(self);
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

        // Divide up counters, since AI is expected to do this. For now,
        // divided evenly with left-overs going to the first target.
        if (sa.hasParam("DividedAsYouChoose")) {
            final String amountStr = sa.getParam("CounterNum");
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
            final int targetCount = sa.getTargets().getTargetCards().size();
            final int amountPerCard = amount / sa.getTargets().getTargetCards().size();
            int amountLeftOver = amount - (amountPerCard * targetCount);
            final TargetRestrictions tgtRes = sa.getTargetRestrictions();
            for (GameObject target : sa.getTargets().getTargets()) {
                tgtRes.addDividedAllocation(target, amountPerCard + amountLeftOver);
                amountLeftOver = 0;
            }
        }

        // TODO: smarter about multiple targets, identical targets, etc...
        targetIndex++;
        return true;
    }
}

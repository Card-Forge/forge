package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.CardUtil;
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
        if (tgt.canTgtPermanent() || tgt.canTgtCreature()) {
            // TODO: What about things that target enchantments and such?
            validTargets.addAll(CardUtil.getValidCardsToTarget(tgt, sa));
        }
        if (tgt.canTgtPlayer()) {
            for (Player p : game.getPlayers()) {
                if (p != self || !tgt.canOnlyTgtOpponent()) {
                    validTargets.add(p);
                }
            }
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

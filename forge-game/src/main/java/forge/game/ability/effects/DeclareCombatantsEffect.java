package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.ability.SpellAbilityEffect;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.TextUtil;

public class DeclareCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        boolean attackers = sa.hasParam("DeclareAttackers");
        boolean blockers = sa.hasParam("DeclareBlockers");
        String what = Lang.joinHomogenous(
                attackers
                        ? "which creatures attack"
                        : null,
                blockers
                        ? "which creatures block this turn and how those creatures block"
                        : null
        );
        String duration = "EndOfTurn".equals(sa.getParam("Until")) ? "turn" : "combat";
        return TextUtil.concatWithSpace(Lang.joinHomogenous(tgtPlayers),Lang.joinVerb(tgtPlayers, "choose"),what,"this",TextUtil.addSuffix(duration,"."));
    }

    @Override
    public void resolve(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        
        final boolean attackers = sa.hasParam("DeclareAttackers");
        final boolean blockers = sa.hasParam("DeclareBlockers");
        
        String until = sa.getParam("Until");
        boolean untilEoT = "EndOfTurn".equals(until);
        
        for (Player p : tgtPlayers) { // Obviously the last player will be applied
            final PhaseHandler ph = p.getGame().getPhaseHandler();
            if (attackers) ph.setPlayerDeclaresAttackers(p);
            if (blockers) ph.setPlayerDeclaresBlockers(p);
            
            GameCommand removeOverrides = new GameCommand() {
                private static final long serialVersionUID = -8064627517852651016L;

                @Override
                public void run() {
                    if (attackers) ph.setPlayerDeclaresAttackers(null);
                    if (blockers) ph.setPlayerDeclaresBlockers(null);
                }
            };
            
            if (untilEoT)
                p.getGame().getEndOfTurn().addUntil(removeOverrides);
            else
                p.getGame().getEndOfCombat().addUntil(removeOverrides);
        }

    }

}

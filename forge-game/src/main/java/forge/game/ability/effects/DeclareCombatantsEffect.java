package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.ability.SpellAbilityEffect;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;

public class DeclareCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        boolean attackers = sa.hasParam("DeclareAttackers");
        final String attDesc =  "which creatures attack";

        boolean blockers = sa.hasParam("DeclareBlockers");
        final String defDesc = "which creatures block this turn and how those creatures block";
        
        String what = Lang.joinHomogenous(attackers ? attDesc : null, blockers ? defDesc : null);
        String until = sa.getParam("Until");
        String duration = "EndOfTurn".equals(until) ? "turn" : "combat";
        
        // TODO Auto-generated method stub
        return Lang.joinHomogenous(tgtPlayers)  + " " + Lang.joinVerb(tgtPlayers, "choose") + " " + what + " this " + duration +".";
    }

    @Override
    public void resolve(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        
        final boolean attackers = sa.hasParam("DeclareAttackers");
        final boolean blockers = sa.hasParam("DeclareBlockers");
        
        String until = sa.getParam("Until");
        boolean untilEoT = "EndOfTurn".equals(until);

        
        for(Player p : tgtPlayers) { // Obviuosly the last player will be applied
            final PhaseHandler ph = p.getGame().getPhaseHandler();
            if( attackers ) ph.setPlayerDeclaresAttackers(p);
            if( blockers ) ph.setPlayerDeclaresBlockers(p);
            
            GameCommand removeOverrides = new GameCommand() {
                private static final long serialVersionUID = -8064627517852651016L;

                @Override
                public void run() {
                    if( attackers ) ph.setPlayerDeclaresAttackers(null);
                    if( blockers ) ph.setPlayerDeclaresBlockers(null);
                }
            };
            
            if ( untilEoT )
                p.getGame().getEndOfTurn().addUntil(removeOverrides);
            else
                p.getGame().getEndOfCombat().addUntil(removeOverrides);
        }

    }

}

package forge.card.ability.effects;

import java.util.List;

import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.util.Lang;

public class DeclareCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersBeforeTargetOnes(sa);

        boolean attackers = sa.hasParam("DeclareAttackers");
        final String attDesc =  "which creatures attack";

        boolean blockers = sa.hasParam("DeclareBlockers");
        final String defDesc = "which creatures block this turn and how those creatures block";
        
        String what = Lang.joinHomogenous(attackers ? attDesc : null, blockers ? defDesc : null);
        
        // TODO Auto-generated method stub
        return Lang.joinHomogenous(tgtPlayers)  + " " + Lang.joinVerb(tgtPlayers, "choose") + " " + what + " this turn.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersBeforeTargetOnes(sa);
        
        boolean attackers = sa.hasParam("DeclareAttackers");
        boolean blockers = sa.hasParam("DeclareBlockers");
        
        for(Player p : tgtPlayers) { // Obviuosly the last player will be applied
            final PhaseHandler ph = p.getGame().getPhaseHandler();
            if( attackers ) ph.setPlayerDeclaresAttackers(p);
            if( blockers ) ph.setPlayerDeclaresBlockers(p);
        }

    }

}

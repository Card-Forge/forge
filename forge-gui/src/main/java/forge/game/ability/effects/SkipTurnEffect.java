package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SkipTurnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumTurns"), sa);

        List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("skips his/her next ").append(numTurns).append(" turn(s).");
        return sb.toString();
    }
    
    @Override
    public void resolve(SpellAbility sa) {
        final int numTurns = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumTurns"), sa);
        List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player player : tgtPlayers) {
            for(int i = 0; i < numTurns; i++) {
                player.addKeyword("Skip your next turn.");
            }
        }
    }
}


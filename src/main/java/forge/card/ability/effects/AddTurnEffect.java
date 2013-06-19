package forge.card.ability.effects;

import java.util.List;

import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.phase.ExtraTurn;
import forge.game.player.Player;

public class AddTurnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumTurns"), sa);

        List<Player> tgtPlayers = getTargetPlayers(sa);


        for (final Player player : tgtPlayers) {
            sb.append(player).append(" ");
        }

        sb.append("takes ");
        sb.append(numTurns > 1 ? numTurns : "an");
        sb.append(" extra turn");

        if (numTurns > 1) {
            sb.append("s");
        }
        sb.append(" after this one.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final int numTurns = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumTurns"), sa);

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            if ((sa.getTargetRestrictions() == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < numTurns; i++) {
                    ExtraTurn extra = p.getGame().getPhaseHandler().addExtraTurn(p);
                    if (sa.hasParam("LoseAtEndStep")) {
                        extra.setLoseAtEndStep(true);
                    }
                    if (sa.hasParam("SkipUntap")) {
                        extra.setSkipUntap(true);
                    }
                    if (sa.hasParam("NoSchemes")) {
                        extra.setCantSetSchemesInMotion(true);
                    }
                }
            }
        }
    }

}

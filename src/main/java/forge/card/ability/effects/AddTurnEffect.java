package forge.card.ability.effects;

import java.util.List;

import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
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

        List<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < numTurns; i++) {
                    ExtraTurn extra = Singletons.getModel().getGame().getPhaseHandler().addExtraTurn(p);
                    if (sa.hasParam("LoseAtEndStep")) {
                        extra.setLoseAtEndStep(true);
                    }
                    if (sa.hasParam("SkipUntap")) {
                        extra.setSkipUntap(true);
                    }
                }
            }
        }
    }

}

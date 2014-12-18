package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.phase.ExtraTurn;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.List;

public class AddTurnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumTurns"), sa);

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
        final int numTurns = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumTurns"), sa);

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            if ((sa.getTargetRestrictions() == null) || p.canBeTargetedBy(sa)) {
                for (int i = 0; i < numTurns; i++) {
                    ExtraTurn extra = p.getGame().getPhaseHandler().addExtraTurn(p);
                    if (sa.hasParam("ExtraTurnDelayedTrigger")) {
                        final Trigger delTrig = TriggerHandler.parseTrigger(sa.getSVar(sa.getParam("ExtraTurnDelayedTrigger")), sa.getHostCard(), true);
                        SpellAbility overridingSA = AbilityFactory.getAbility(sa.getSVar(sa.getParam("ExtraTurnDelayedTriggerExcute")), sa.getHostCard());
                        overridingSA.setActivatingPlayer(sa.getActivatingPlayer());
                        delTrig.setOverridingAbility(overridingSA);
                        extra.addTrigger(delTrig);
                    }
                    if (sa.hasParam("SkipUntap")) {
                        extra.setSkipUntap(true);
                    }
                    if (sa.hasParam("NoSchemes")) {
                        extra.setCantSetSchemesInMotion(true);
                    }
                    if (sa.hasParam("ShowMessage")) {
                        p.getGame().getAction().nofityOfValue(sa, p, p + " takes an extra turn.", null);
                    }
                }
            }
        }
    }

}

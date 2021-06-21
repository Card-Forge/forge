package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ControlPlayerEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {

        List<Player> tgtPlayers = getTargetPlayers(sa);
        return TextUtil.concatWithSpace(sa.getActivatingPlayer().toString(),"controls", Lang.joinHomogenous(tgtPlayers),"during their next turn");
    }

    @SuppressWarnings("serial")
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Player controller = sa.hasParam("Controller") ? AbilityUtils.getDefinedPlayers(
                sa.getHostCard(), sa.getParam("Controller"), sa).get(0) : activator;

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player pTarget: tgtPlayers) {
            // on next untap gain control
            game.getUntap().addUntil(pTarget, new GameCommand() {
                @Override
                public void run() {
                    // CR 800.4b
                    if (controller.hasLost()) {
                        return;
                    }

                    long ts = game.getNextTimestamp();
                    pTarget.addController(ts, controller);

                    // on following cleanup release control
                    game.getEndOfTurn().addUntil(new GameCommand() {
                        @Override
                        public void run() {
                            pTarget.removeController(ts);
                        }
                    });
                }
            });
        }
    }
}

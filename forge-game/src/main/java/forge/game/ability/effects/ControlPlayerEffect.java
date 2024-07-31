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
        return TextUtil.concatWithSpace(sa.getActivatingPlayer().toString(), "controls", Lang.joinHomogenous(tgtPlayers), "during their next turn");
    }

    @SuppressWarnings("serial")
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Player controller = sa.hasParam("Controller") ? AbilityUtils.getDefinedPlayers(
                sa.getHostCard(), sa.getParam("Controller"), sa).get(0) : activator;

        for (final Player pTarget: getTargetPlayers(sa)) {
            // before next untap gain control
            game.getCleanup().addUntil(pTarget, (GameCommand) () -> {
                // CR 800.4b
                if (!controller.isInGame()) {
                    return;
                }

                long ts = game.getNextTimestamp();
                pTarget.addController(ts, controller);

                // after following cleanup release control
                game.getCleanup().addUntil((GameCommand) () -> pTarget.removeController(ts));
            });
        }
    }
}

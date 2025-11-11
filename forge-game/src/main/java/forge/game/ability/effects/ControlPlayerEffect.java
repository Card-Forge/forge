package forge.game.ability.effects;

import java.util.List;

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
        final Player controller = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Controller"), sa).get(0);
        final Game game = controller.getGame();
        final boolean combat = sa.hasParam("Combat");

        for (final Player pTarget: getTargetPlayers(sa)) {
            // before next untap gain control
            (combat ? game.getBeginOfCombat() : game.getCleanup()).addUntil(pTarget, () -> {
                // CR 800.4b
                if (!controller.isInGame()) {
                    return;
                }

                long ts = game.getNextTimestamp();
                pTarget.addController(ts, controller);

                // after following cleanup release control
                (combat ? game.getEndOfCombat() : game.getCleanup()).addUntil(() -> pTarget.removeController(ts));
            });
        }
    }
}

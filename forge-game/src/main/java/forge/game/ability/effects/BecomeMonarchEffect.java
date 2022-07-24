package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class BecomeMonarchEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(Lang.joinHomogenous(tgtPlayers)).append(tgtPlayers.size() == 1 ? " becomes" : " become");
        sb.append(" the monarch.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // TODO: improve ai and fix corner cases
        final String set = sa.getHostCard().getSetCode();

        for (final Player p : getTargetPlayers(sa)) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                if (p.canBecomeMonarch()) {
                    p.getGame().getAction().becomeMonarch(p, set);
                }
            }
        }
    }

}

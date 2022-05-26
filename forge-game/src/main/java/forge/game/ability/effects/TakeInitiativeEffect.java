package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.apache.commons.lang3.StringUtils;

public class TakeInitiativeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" takes the initiative.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // TODO: improve ai and fix corner cases
        final String set = sa.getHostCard().getSetCode();

        for (final Player p : getTargetPlayers(sa)) {
            final Game game = p.getGame();
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                game.getAction().takeInitiative(p, set);
            }
        }
    }
}

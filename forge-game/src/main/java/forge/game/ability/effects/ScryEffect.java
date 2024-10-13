package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;


public class ScryEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> players = getTargetPlayers(sa);
        sb.append(Lang.joinHomogenous(players)).append(" ");

        int num = 1;
        if (sa.hasParam("ScryNum")) {
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("ScryNum"), sa);
        }

        sb.append(players.size() == 1 ? "scries " : "scry ").append(num).append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        int num = 1;
        if (sa.hasParam("ScryNum")) {
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("ScryNum"), sa);
        }

        boolean isOptional = sa.hasParam("Optional");

        final List<Player> players = Lists.newArrayList(); // players really affected

        // Optional here for spells that have optional multi-player scrying
        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            if (!isOptional || p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWanttoScry"), null)) {
                players.add(p);
            }
        }
        sa.getActivatingPlayer().getGame().getAction().scry(players, num, sa);
    }
}

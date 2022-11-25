package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

public class LifeLoseEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String amountStr = sa.getParam("LifeAmount");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        final String spellDesc = sa.getParam("SpellDescription");

        int affected = getTargetPlayers(sa).size();
        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        sb.append(affected > 1 ? " each lose " : " loses ");
        if (!StringUtils.isNumeric(amountStr) && spellDesc != null && spellDesc.contains("life equal to")) {
            sb.append(spellDesc.substring(spellDesc.indexOf("life equal to")));
        } else {
            sb.append(amount).append(" life.");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        int lifeLost = 0;

        final int lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("LifeAmount"), sa);

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            lifeLost += p.loseLife(lifeAmount, false, false);
        }
        sa.setSVar("AFLifeLost", "Number$" + lifeLost);
    }

}

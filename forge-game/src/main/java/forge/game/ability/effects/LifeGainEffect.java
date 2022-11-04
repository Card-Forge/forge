package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

public class LifeGainEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String amountStr = sa.getParam("LifeAmount");

        sb.append(Lang.joinHomogenous(getDefinedPlayersOrTargeted(sa)));
        sb.append(getDefinedPlayersOrTargeted(sa).size() > 1 ? " gain " : " gains ");
        if (!StringUtils.isNumeric(amountStr) && sa.hasParam("SpellDescription") &&
                sa.getParam("SpellDescription").contains("life equal to")) {
            String xDesc = sa.getParam("SpellDescription");
            xDesc = xDesc.substring(xDesc.indexOf("life equal to"));
            sb.append(xDesc);
        } else if (!amountStr.equals("AFLifeLost") || sa.hasSVar(amountStr)) {
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);

            sb.append(amount).append(" life.");
        } else {
            sb.append("life equal to the life lost this way.");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        if (tgtPlayers.isEmpty()) {
            tgtPlayers.add(sa.getActivatingPlayer());
        }
        String amount = sa.getParam("LifeAmount");
        boolean variableAmount = amount.equals("AFNotDrawnNum");
        int lifeAmount = 0;
        if (variableAmount) {
            amount = "X";
        } else {
            lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), amount, sa);
        }

        for (final Player p : tgtPlayers) {
            if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                if (variableAmount) {
                    sa.setSVar("AFNotDrawnNum", sa.getSVar("AFNotDrawnNum_" + p.getId()));
                    lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), amount, sa);
                }
                p.gainLife(lifeAmount, sa.getHostCard(), sa);
            }
        }
    }

}

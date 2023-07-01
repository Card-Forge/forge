package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

public class BranchEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        // TODO Reuse SpellAbilityCondition and areMet() here instead of repeating each

        int value = 0;
        if (sa.hasParam("BranchCondition")) {
            if (sa.getParam("BranchCondition").equals("ChosenCard")) {
                value = host.getChosenCards().size();
            }
        } else {
            value = AbilityUtils.calculateAmount(host, sa.getParam("BranchConditionSVar"), sa);
        }
        String branchCompare = sa.getParamOrDefault("BranchConditionSVarCompare", "GE1");

        String operator = branchCompare.substring(0, 2);
        String operand = branchCompare.substring(2);

        final int operandValue = AbilityUtils.calculateAmount(host, operand, sa);

        SpellAbility sub = null;
        if (Expressions.compare(value, operator, operandValue)) {
            sub = sa.getAdditionalAbility("TrueSubAbility");
        } else {
            sub = sa.getAdditionalAbility("FalseSubAbility");
        }
        if (sub != null) {
            AbilityUtils.resolve(sub);
        }
    }
}

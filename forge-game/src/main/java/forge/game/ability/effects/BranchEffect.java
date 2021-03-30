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

        // For now branch conditions will only be an Svar Compare
        String branchSVar = sa.getParam("BranchConditionSVar");
        String branchCompare = sa.getParamOrDefault("BranchConditionSVarCompare", "GE1");

        String operator = branchCompare.substring(0, 2);
        String operand = branchCompare.substring(2);

        final int svarValue = AbilityUtils.calculateAmount(host, branchSVar, sa);
        final int operandValue = AbilityUtils.calculateAmount(host, operand, sa);

        SpellAbility sub = null;
        if (Expressions.compare(svarValue, operator, operandValue)) {
            sub = sa.getAdditionalAbility("TrueSubAbility");
        } else {
            sub = sa.getAdditionalAbility("FalseSubAbility");
        }
        if (sub != null) {
            AbilityUtils.resolve(sub);
        }
    }
}

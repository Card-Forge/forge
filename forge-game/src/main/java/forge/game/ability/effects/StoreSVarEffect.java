package forge.game.ability.effects;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

public class StoreSVarEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        //SVar$ OldToughness | Type$ Count | Expression$ CardToughness
        Card source = sa.getHostCard();

        String key = null;
        String type = null;
        String expr = null;

        if (sa.hasParam("SVar")) {
            key = sa.getParam("SVar");
        }

        if (sa.hasParam("Type")) {
            type = sa.getParam("Type");
        }

        if (sa.hasParam("Expression")) {
            expr = sa.getParam("Expression");
        }

        if (key == null || type == null || expr == null) {
            System.out.println("SVar, Type and Expression paramaters required for StoreSVar. They are missing for " + source.getName());
            return;
        }

        int value = 0;

        if (type.equals("Count")) {
            value = AbilityUtils.xCount(source, expr, sa);
        }
        else if (type.equals("Number")) {
            value = Integer.valueOf(expr);
        }
        else if (type.equals("CountSVar")) {
            if (expr.contains("/")) {
                final String exprMathVar = expr.split("\\/")[1].split("\\.")[1];
                int exprMath = AbilityUtils.calculateAmount(source, exprMathVar, sa);
                expr = TextUtil.fastReplace(expr, exprMathVar, Integer.toString(exprMath));
            }
            value = AbilityUtils.xCount(source, "SVar$" + expr, sa);
        } else if (type.equals("Targeted")) {
            value = AbilityUtils.handlePaid(sa.findTargetedCards(), expr, source, sa);
        } else if (type.equals("Triggered")) {
            Card trigCard = (Card)sa.getTriggeringObject(AbilityKey.Card);
            value = AbilityUtils.xCount(trigCard, expr, sa);
        } else if (type.equals("Calculate")) {
            value = AbilityUtils.calculateAmount(source, expr, sa);
        }
        //TODO For other types call a different function

        StringBuilder numBuilder = new StringBuilder();
        numBuilder.append("Number$");
        numBuilder.append(value);

        source.setSVar(key, numBuilder.toString());

        SpellAbility root = sa.getRootAbility();
        while (root != null) {
            root.setSVar(key, numBuilder.toString());
            root = root.getSubAbility();
        }
    }
}

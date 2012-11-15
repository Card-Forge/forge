package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class StoreSVarEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {
        //SVar$ OldToughness | Type$ Count | Expression$ CardToughness
        Card source = sa.getSourceCard();

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
            value = CardFactoryUtil.xCount(source, expr);
        }
        else if (type.equals("Number")) {
            value = Integer.valueOf(expr);
        }
        else if (type.equals("CountSVar")) {
            value = CardFactoryUtil.xCount(source, "SVar$" + expr);
        }
        else if (type.equals("Targeted")) {
            List<Card> list = new ArrayList<Card>();
            final Target t = sa.getTarget();
            if (null != t) {
                final ArrayList<Object> all = t.getTargets();
                list = new ArrayList<Card>();
                if (!all.isEmpty() && (all.get(0) instanceof SpellAbility)) {
                    final SpellAbility saTargeting = sa.getParentTargetingSA();
                    // possible NPE on next line
                    final ArrayList<SpellAbility> sas = saTargeting.getTarget().getTargetSAs();
                    for (final SpellAbility tgtsa : sas) {
                        list.add(tgtsa.getSourceCard());
                    }
                } else {
                    final SpellAbility saTargeting = sa.getParentTargetingCard();
                    if (null != saTargeting.getTarget()) {
                        list.addAll(saTargeting.getTarget().getTargetCards());
                    }
                }
            } else {
                final SpellAbility parent = sa.getParentTargetingCard();
                if (parent.getTarget() != null) {
                    final ArrayList<Object> all = parent.getTarget().getTargets();
                    if (!all.isEmpty() && (all.get(0) instanceof SpellAbility)) {
                        list = new ArrayList<Card>();
                        final ArrayList<SpellAbility> sas = parent.getTarget().getTargetSAs();
                        for (final SpellAbility tgtsa : sas) {
                            list.add(tgtsa.getSourceCard());
                        }
                    } else {
                        list = new ArrayList<Card>(parent.getTarget().getTargetCards());
                    }
                }
            }
            value = CardFactoryUtil.handlePaid(list, expr, source);
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
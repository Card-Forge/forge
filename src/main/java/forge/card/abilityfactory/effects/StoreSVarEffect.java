package forge.card.abilityfactory.effects;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

public class StoreSVarEffect extends SpellEffect { 

    /**
     * <p>
     * storeSVarResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        //SVar$ OldToughness | Type$ Count | Expression$ CardToughness
        Card source = sa.getSourceCard();

        String key = null;
        String type = null;
        String expr = null;

        if (params.containsKey("SVar")) {
            key = params.get("SVar");
        }

        if (params.containsKey("Type")) {
            type = params.get("Type");
        }

        if (params.containsKey("Expression")) {
            expr = params.get("Expression");
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
        //TODO For other types call a different function

        StringBuilder numBuilder = new StringBuilder();
        numBuilder.append("Number$");
        numBuilder.append(value);

        source.setSVar(key, numBuilder.toString());
        
        SpellAbility root = sa.getRootSpellAbility();
        while(root != null) {
            root.setSVar(key, numBuilder.toString());
            root = root.getSubAbility();
        }
    }

} // end class AbilityFactorystoreSVar
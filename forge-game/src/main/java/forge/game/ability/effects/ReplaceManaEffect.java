package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;


import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        // outside of Replacement Effect, unwanted result
        if (!sa.isReplacementAbility()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> params = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);

        String anyColor = null;
        if ("Any".equals(sa.getParam("ReplaceMana")) || "Any".equals(sa.getParam("ReplaceType"))) {
            anyColor = MagicColor.toShortString(sa.getActivatingPlayer().getController().chooseColor("Choose a color", sa, ColorSet.WUBRG));
        }
        params.put(AbilityKey.Mana, replaceMana(sa, (String) sa.getReplacingObject(AbilityKey.Mana), anyColor));
        // effect was updated
        params.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }

    /** @param anyColor the color to use where the effect lets its controller choose one */
    public static String replaceMana(final SpellAbility sa, String replaced, final String anyColor) {
        final Card card = sa.getHostCard();
        if (sa.hasParam("ReplaceMana")) {
            // replace type and amount
            replaced = sa.getParam("ReplaceMana");
            if ("Any".equals(replaced)) {
                replaced = anyColor;
            }
        } else if (sa.hasParam("ReplaceType")) {
            // replace color and colorless
            String color = sa.getParam("ReplaceType");
            if ("Any".equals(color)) {
                color = anyColor;
            } else {
                // convert in case Color Word used
                color = MagicColor.toShortString(color);
            }
            for (byte c : MagicColor.WUBRGC) {
                String s = MagicColor.toShortString(c);
                replaced = replaced.replace(s, color);
            }
        } else if (sa.hasParam("ReplaceColor")) {
            // replace color
            String color = sa.getParam("ReplaceColor");
            if ("Chosen".equals(color)) {
                if (card.hasChosenColor()) {
                    color = MagicColor.toShortString(card.getChosenColor());
                }
            } else {
                // convert in case Color Word used
                color = MagicColor.toShortString(color);
            }
            if (sa.hasParam("ReplaceOnly")) {
                replaced = replaced.replace(sa.getParam("ReplaceOnly"), color);
            } else {
                for (byte c : MagicColor.WUBRG) {
                    String s = MagicColor.toShortString(c);
                    replaced = replaced.replace(s, color);
                }
            }
        } else if (sa.hasParam("ReplaceAmount")) {
            // replace amount = multiples
            replaced = StringUtils.repeat(replaced, " ", Integer.parseInt(sa.getParam("ReplaceAmount")));
        }
        return replaced;
    }

}

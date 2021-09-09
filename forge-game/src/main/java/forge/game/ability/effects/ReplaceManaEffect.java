package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;


import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();

        // outside of Replacement Effect, unwanted result
        if (!sa.isReplacementAbility()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> params = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);
        
        String replaced = (String)sa.getReplacingObject(AbilityKey.Mana);
        if (sa.hasParam("ReplaceMana")) {
            // replace type and amount
            replaced = sa.getParam("ReplaceMana");
            if ("Any".equals(replaced)) {
                byte rs = MagicColor.GREEN;
                rs = player.getController().chooseColor("Choose a color", sa, ColorSet.ALL_COLORS);
                replaced = MagicColor.toShortString(rs);
            }
        } else if (sa.hasParam("ReplaceType")) {
            // replace color and colorless
            String color = sa.getParam("ReplaceType");
            if ("Any".equals(color)) {
                byte rs = MagicColor.GREEN;
                rs = player.getController().chooseColor("Choose a color", sa, ColorSet.ALL_COLORS);
                color = MagicColor.toShortString(rs);
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
            replaced = StringUtils.repeat(replaced, " ", Integer.valueOf(sa.getParam("ReplaceAmount")));
        }
        params.put(AbilityKey.Mana, replaced);
        // effect was updated
        params.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }

}

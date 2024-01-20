package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;

public class AlterAttributeEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        boolean activate = Boolean.valueOf(sa.getParamOrDefault("Activate", "true"));
        ArrayList<String> attributes = Lists.newArrayList(sa.getParam("Attributes").split(","));
        CardCollection defined = getDefinedCardsOrTargeted(sa, "Defined");

        for(Card c : defined) {
            for(String attr : attributes) {
                switch(attr.trim()) {
                    case "Solve":
                    case "Solved":
                        c.setSolved(activate);
                        break;

                    // Other attributes: renown, monstrous, suspected, etc

                    default:
                        break;
                }
            }
            c.updateAbilityTextForView();
        }
    }
}

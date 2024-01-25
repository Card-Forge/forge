package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.TextUtil;

import java.util.ArrayList;

public class AlterAttributeEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        boolean activate = Boolean.valueOf(sa.getParamOrDefault("Activate", "true"));
        ArrayList<String> attributes = Lists.newArrayList(sa.getParam("Attributes").split(","));
        CardCollection defined = getDefinedCardsOrTargeted(sa, "Defined");

        if (sa.hasParam("Optional")) {
            final String targets = Lang.joinHomogenous(defined);
            final String message = sa.hasParam("OptionQuestion")
                    ? TextUtil.fastReplace(sa.getParam("OptionQuestion"), "TARGETS", targets)
                    : getStackDescription(sa);

            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, message, null)) {
                return;
            }
        }

        for(Card c : defined) {
            for(String attr : attributes) {
                switch(attr.trim()) {
                    case "Solve":
                    case "Solved":
                        c.setSolved(activate);
                        break;
                    case "Suspect":
                    case "Suspected":
                        c.setSuspected(activate);
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

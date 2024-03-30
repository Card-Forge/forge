package forge.game.ability.effects;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;
import forge.util.TextUtil;


public class AlterAttributeEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        boolean activate = Boolean.valueOf(sa.getParamOrDefault("Activate", "true"));
        String[] attributes = sa.getParam("Attributes").split(",");
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

        for (Card c : defined) {
            for (String attr : attributes) {
                boolean altered = false;

                switch (attr.trim()) {
                    case "Plotted":
                        altered = c.setPlotted(activate);
                        break;
                    case "Solve":
                    case "Solved":
                        altered = c.setSolved(activate);
                        if (altered) {
                            Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
                            runParams.put(AbilityKey.Player, sa.getActivatingPlayer());
                            c.getGame().getTriggerHandler().runTrigger(TriggerType.CaseSolved, runParams, false);
                        }
                        break;
                    case "Suspect":
                    case "Suspected":
                        altered = c.setSuspected(activate);
                        break;
                    case "Saddle":
                    case "Saddled":
                        // currently clean up in Card manually
                        altered = c.setSaddled(activate);
                        break;

                        // Other attributes: renown, monstrous, suspected, etc

                    default:
                        break;
                }

                if (altered && sa.hasParam("RememberAltered")) {
                    sa.getHostCard().addRemembered(c);
                }
            }
            c.updateAbilityTextForView();
        }
    }
}

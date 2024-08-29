package forge.game.ability.effects;

import java.util.Map;

import forge.game.GameLogEntryType;
import forge.game.GameType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;
import forge.util.TextUtil;


public class AlterAttributeEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        boolean activate = Boolean.parseBoolean(sa.getParamOrDefault("Activate", "true"));
        String[] attributes = sa.getParam("Attributes").split(",");
        CardCollection defined = getDefinedCardsOrTargeted(sa);

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
                        if (altered) {
                            CardCollection saddlers = (CardCollection) sa.getPaidList("TappedCards", true);
                            c.addSaddledByThisTurn(saddlers);
                            Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
                            runParams.put(AbilityKey.Crew, saddlers);
                            c.getGame().getTriggerHandler().runTrigger(TriggerType.BecomesSaddled, runParams, false);
                        }
                        break;
                    case "Commander":
                        //This implementation doesn't let a card make someone else's creature your commander. But that's an edge case among edge cases.
                        Player p = c.getOwner();
                        if (c.isCommander() == activate || p.getCommanders().contains(c) == activate)
                            break; //Isn't changing status.
                        if (activate) {
                            if(!c.getGame().getRules().hasCommander()) {
                                System.out.println("Commander status applied in non-commander format. Applying Commander variant.");
                                c.getGame().getRules().addAppliedVariant(GameType.Commander);
                            }
                            p.addCommander(c);
                            //Seems important enough to mention in the game log.
                            c.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, String.format("%s is now %s's commander.", c.getPaperCard().getName(), p));
                        }
                        else {
                            p.removeCommander(c);
                            c.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, String.format("%s is no longer %s's commander.", c.getPaperCard().getName(), p));
                        }
                        altered = true;
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

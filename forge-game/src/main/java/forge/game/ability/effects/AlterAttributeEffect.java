package forge.game.ability.effects;

import java.util.Map;

import forge.game.GameLogEntryType;
import forge.game.GameType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.GameEventCardPlotted;
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
            final Card gameCard = c.getGame().getCardState(c, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !c.equalsWithGameTimestamp(gameCard) || gameCard.isPhasedOut()) {
                continue;
            }

            for (String attr : attributes) {
                boolean altered = false;

                switch (attr.trim()) {
                    case "Harnessed":
                        altered = gameCard.setHarnessed(activate);
                        break;
                    case "Plotted":
                        altered = gameCard.setPlotted(activate);

                        c.getGame().fireEvent(new GameEventCardPlotted(c, sa.getActivatingPlayer()));
                        break;
                    case "Solve":
                    case "Solved":
                        altered = gameCard.setSolved(activate);
                        if (altered) {
                            Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(gameCard);
                            runParams.put(AbilityKey.Player, sa.getActivatingPlayer());
                            c.getGame().getTriggerHandler().runTrigger(TriggerType.CaseSolved, runParams, false);
                        }
                        break;
                    case "Suspect":
                    case "Suspected":
                        altered = gameCard.setSuspected(activate);
                        break;
                    case "Saddle":
                    case "Saddled":
                        // currently clean up in Card manually
                        altered = gameCard.setSaddled(activate);
                        if (altered) {
                            CardCollection saddlers = sa.getPaidList("TappedCards", true);
                            gameCard.addSaddledByThisTurn(saddlers);
                            Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(gameCard);
                            runParams.put(AbilityKey.Crew, saddlers);
                            c.getGame().getTriggerHandler().runTrigger(TriggerType.BecomesSaddled, runParams, false);
                        }
                        break;
                    case "Commander":
                        //This implementation doesn't let a card make someone else's creature your commander. But that's an edge case among edge cases.
                        Player p = gameCard.getOwner();
                        if (gameCard.isCommander() == activate || p.getCommanders().contains(gameCard) == activate)
                            break; //Isn't changing status.
                        if (activate) {
                            if (!gameCard.getGame().getRules().hasCommander()) {
                                System.out.println("Commander status applied in non-commander format. Applying Commander variant.");
                                gameCard.getGame().getRules().addAppliedVariant(GameType.Commander);
                            }
                            p.addCommander(gameCard);
                            //Seems important enough to mention in the game log.
                            gameCard.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, String.format("%s is now %s's commander.", gameCard.getPaperCard().getDisplayName(), p));
                        } else {
                            p.removeCommander(gameCard);
                            gameCard.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, String.format("%s is no longer %s's commander.", gameCard.getPaperCard().getDisplayName(), p));
                        }
                        altered = true;
                        break;

                        // Other attributes: renown, monstrous, suspected, etc

                    default:
                        break;
                }

                if (altered && sa.hasParam("RememberAltered")) {
                    sa.getHostCard().addRemembered(gameCard);
                }
            }
            gameCard.updateAbilityTextForView();
        }
    }
}

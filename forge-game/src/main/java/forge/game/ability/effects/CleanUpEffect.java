package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventRandomLog;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;

public class CleanUpEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = source.getGame();

        String logMessage = "";
        if (sa.hasParam("Log")) {
            logMessage = logOutput(sa, source);
        }

        if (sa.hasParam("ClearRemembered")) {
            source.clearRemembered();
            game.getCardState(source).clearRemembered();
        }
        if (sa.hasParam("ForgetDefined")) {
            for (final Card card : AbilityUtils.getDefinedCards(source, sa.getParam("ForgetDefined"), sa)) {
                source.removeRemembered(card);
            }
        }
        if (sa.hasParam("ForgetDefinedPlayer")) {
            for (final Player player : AbilityUtils.getDefinedPlayers(source, sa.getParam("ForgetDefinedPlayer"), sa)) {
                source.removeRemembered(player);
            }
        }
        if (sa.hasParam("ClearImprinted")) {
            source.clearImprintedCards();
            game.getCardState(source).clearImprintedCards();
        }
        if (sa.hasParam("ClearTriggered")) {
            game.getTriggerHandler().clearDelayedTrigger(source);
        }
        if (sa.hasParam("ClearCoinFlips")) {
            source.clearFlipResult();
        }
        if (sa.hasParam("ClearChosenCard")) {
            source.setChosenCards(null);
        }
        if (sa.hasParam("ClearChosenPlayer")) {
            source.setChosenPlayer(null);
        }
        if (sa.hasParam("ClearChosenType")) {
            source.setChosenType("");
            source.setChosenType2("");
        }
        if (sa.hasParam("ClearChosenColor")) {
            source.setChosenColors(null);
        }
        if (sa.hasParam("ClearNamedCard")) {
            source.setNamedCard("");
        }
        if (sa.hasParam("Log")) {
            source.getController().getGame().fireEvent(new GameEventRandomLog(logMessage));
        }
    }

    protected String logOutput(SpellAbility sa, Card source) {
        final StringBuilder log = new StringBuilder();
        final String name = CardTranslation.getTranslatedName(source.getName());
        String linebreak = "\r\n";

        if (sa.hasParam("ClearRemembered") && source.getRememberedCount() != 0) {
            for (Object o : source.getRemembered()) {
                String rem = o.toString();
                if (o instanceof Card) {
                    log.append(log.length() > 0 ? linebreak : "");
                    log.append(Localizer.getInstance().getMessage("lblChosenCard", name, rem));
                } else if (o instanceof Player) {
                    log.append(log.length() > 0 ? linebreak : "");
                    log.append(Localizer.getInstance().getMessage("lblChosenPlayer", name, rem));
                }
            }
        }

        String chCard = sa.hasParam("ClearChosenCard") && source.hasChosenCard() ? source.getChosenCards()
                .toString().replace("[","").replace("]", "") : "";
        if (chCard.length() > 0 && !log.toString().contains(chCard)) {
            log.append(log.length() > 0 ? linebreak : "");
            String message = source.getChosenCards().size() > 1 ? "lblChosenMultiCard" : "lblChosenCard";
            log.append(Localizer.getInstance().getMessage(message, name, chCard));
        }

        String chPlay = sa.hasParam("ClearChosenPlayer") && source.hasChosenPlayer()
                ? source.getChosenPlayer().toString() : "";
        if (chPlay.length() > 0 && !log.toString().contains(chPlay)) {
            log.append(log.length() > 0 ? linebreak : "");
            log.append(Localizer.getInstance().getMessage("lblChosenPlayer", name, chPlay));
        }
        log.append(log.length() > 0 ? "" : Localizer.getInstance().getMessage("lblNoValidChoice", name));

        return log.toString();
    }
}

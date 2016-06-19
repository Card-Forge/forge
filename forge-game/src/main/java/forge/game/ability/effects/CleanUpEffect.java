package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class CleanUpEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = source.getGame();

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
        if (sa.hasParam("ClearChosenX")) {
            source.setSVar("ChosenX", "");
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
        }
    }
}

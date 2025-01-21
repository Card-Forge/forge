package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.GameEventCardRegenerated;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

import java.util.Map;

public class RegenerationEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        CardCollection tapped = new CardCollection();
        for (Card c : getTargetCards(sa)) {
            // checks already done in ReplacementEffect

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(c, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !c.equalsWithGameTimestamp(gameCard)) {
                continue;
            }

            SpellAbility cause = (SpellAbility)sa.getReplacingObject(AbilityKey.Cause);

            gameCard.setDamage(0);
            gameCard.setHasBeenDealtDeathtouchDamage(false);
            if (gameCard.tap(true, cause, gameCard.getController())) tapped.add(gameCard);
            gameCard.addRegeneratedThisTurn();

            if (game.getCombat() != null) {
                game.getCombat().saveLKI(gameCard);
                game.getCombat().removeFromCombat(gameCard);
            }

            // Play the Regen sound
            game.fireEvent(new GameEventCardRegenerated(gameCard));

            if (host.isImmutable()) {
                gameCard.decShieldCount();
                host.removeRemembered(gameCard);
            }
        }
        if (!tapped.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, tapped);
            game.getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
        }
    }

}

package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardRegenerated;
import forge.game.spellability.SpellAbility;

public class RegenerationEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        for (Card c : getTargetCards(sa)) {
            // checks already done in ReplacementEffect

            SpellAbility cause = (SpellAbility)sa.getReplacingObject(AbilityKey.Cause);

            c.setDamage(0);
            c.setHasBeenDealtDeathtouchDamage(false);
            c.tap(true, cause, c.getController());
            c.addRegeneratedThisTurn();

            if (game.getCombat() != null) {
                game.getCombat().saveLKI(c);
                game.getCombat().removeFromCombat(c);
            }

            // Play the Regen sound
            game.fireEvent(new GameEventCardRegenerated(c));

            if (host.isImmutable()) {
                c.decShieldCount();
                host.removeRemembered(c);
            }
        }
    }

}

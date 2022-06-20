package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class TapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final boolean remTapped = sa.hasParam("RememberTapped");
        final boolean alwaysRem = sa.hasParam("AlwaysRemember");
        if (remTapped) {
            card.clearRemembered();
        }

        for (final Card tgtC : getTargetCards(sa)) {
            if (tgtC.isInPlay()) {
                // check if the object is still in game or if it was moved
                Card gameCard = game.getCardState(tgtC, null);
                // gameCard is LKI in that case, the card is not in game anymore
                // or the timestamp did change
                // this should check Self too
                if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                    continue;
                }
                if (gameCard.isUntapped() && remTapped || alwaysRem) {
                    card.addRemembered(gameCard);
                }
                gameCard.tap(true);
            }
            if (sa.hasParam("ETB")) {
                // do not fire Taps triggers
                tgtC.setTapped(true);
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Tap ");
        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(".");
        return sb.toString();
    }

}

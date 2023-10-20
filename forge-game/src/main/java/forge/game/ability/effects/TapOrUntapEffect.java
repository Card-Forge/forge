package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class TapOrUntapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        sb.append("Tap or untap ");

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        Player activator = sa.getActivatingPlayer();
        PlayerController pc = activator.getController();

        for (final Card tgtC : getTargetCards(sa)) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            if (tgtC.isPhasedOut()) {
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }
            // If the effected card is controlled by the same controller of the SA, default to untap.
            boolean tap = pc.chooseBinary(sa, Localizer.getInstance().getMessage("lblTapOrUntapTarget", CardTranslation.getTranslatedName(gameCard.getName())), PlayerController.BinaryChoiceType.TapOrUntap,
                    !gameCard.getController().equals(activator));

            if (tap) {
                Player tapper = activator;
                if (sa.hasParam("Tapper")) {
                    tapper = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Tapper"), sa).getFirst();
                }

                gameCard.tap(true, sa, tapper);
            } else {
                gameCard.untap(true);
            }
        }
    }
}

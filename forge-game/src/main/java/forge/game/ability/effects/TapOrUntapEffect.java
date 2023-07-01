package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
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
        final List<Card> tgtCards = getTargetCards(sa);
        PlayerController pc = sa.getActivatingPlayer().getController();

        for (final Card tgtC : tgtCards) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            if (tgtC.isPhasedOut()) {
                continue;
            }

            // If the effected card is controlled by the same controller of the SA, default to untap.
            boolean tap = pc.chooseBinary(sa, Localizer.getInstance().getMessage("lblTapOrUntapTarget", CardTranslation.getTranslatedName(tgtC.getName())), PlayerController.BinaryChoiceType.TapOrUntap,
                    !tgtC.getController().equals(sa.getActivatingPlayer()) );

            if (tap) {
                tgtC.tap(true);
            } else {
                tgtC.untap(true);
            }
        }
    }

}

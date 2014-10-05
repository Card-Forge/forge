package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class TapOrUntapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();


        sb.append("Tap or untap ");

        final List<Card> tgtCards = getTargetCards(sa);
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<Card> tgtCards = getTargetCards(sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        PlayerController pc = sa.getActivatingPlayer().getController();
        
        for (final Card tgtC : tgtCards) {
            if (tgtC.isInPlay() && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                // If the effected card is controlled by the same controller of the SA, default to untap.
                boolean tap = pc.chooseBinary(sa, "Tap or Untap " + tgtC + "?", PlayerController.BinaryChoiceType.TapOrUntap,
                        !tgtC.getController().equals(sa.getActivatingPlayer()) );

                if (tap) {
                    tgtC.tap();
                } else {
                    tgtC.untap();
                }
            }
        }
    }

}

package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ManifestAi extends ManifestBaseAi {

    @Override
    protected boolean shouldApply(final Card card, final Player ai, final SpellAbility sa) {
        // check to ensure that there are no replacement effects that prevent creatures ETBing from library
        // (e.g. Grafdigger's Cage)
        Card topCopy = CardUtil.getLKICopy(card);
        topCopy.turnFaceDownNoUpdate();
        topCopy.setManifested(true);

        if (ComputerUtil.isETBprevented(topCopy)) {
            return false;
        }

        if (card.getView().canBeShownTo(ai.getView())) {
            // try to avoid manifest a non Permanent
            if (!card.isPermanent())
                return false;

            // do not manifest a card with X in its cost
            if (card.getManaCost().countX() > 0)
                return false;

            // try to avoid manifesting a creature with zero or less toughness
            if (card.isCreature() && card.getNetToughness() <= 0)
                return false;

            // card has ETBTrigger or ETBReplacement
            if (card.hasETBTrigger(false) || card.hasETBReplacement()) {
                return false;
            }
        }
        return true;
    }
}

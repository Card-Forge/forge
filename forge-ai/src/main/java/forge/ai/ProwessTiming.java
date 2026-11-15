package forge.ai;

import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * An instant or sorcery is cast before combat when a creature of the AI with prowess is going to attack: the trigger
 * only helps the attack if the spell comes first (Boltwave after Monastery Swiftspear has attacked is a wasted trigger).
 * Read from the keyword on the creature and the AI's own predicted attack, so it holds for any prowess creature.
 */
public final class ProwessTiming {
    private ProwessTiming() {
    }

    public static boolean worthBeforeCombat(final Player ai, final SpellAbility sa) {
        try {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || host.isPermanent() || !(ai.getController() instanceof PlayerControllerAi)) {
                return false;
            }
            final PhaseHandler ph = ai.getGame().getPhaseHandler();
            if (!ph.is(PhaseType.MAIN1, ai)) {
                return false;
            }
            for (final Card c : ai.getCreaturesInPlay()) {
                if (c.hasKeyword(Keyword.PROWESS) && !c.isTapped() && ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
                    return true;
                }
            }
        } catch (final RuntimeException e) {
            return false; // a timing refinement only: never let it break a turn
        }
        return false;
    }
}

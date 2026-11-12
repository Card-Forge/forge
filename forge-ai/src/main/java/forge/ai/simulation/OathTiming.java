package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Oath of Druids with Forbidden Orchard: the Oath only works at the upkeep of its controller when an opponent controls
 * more creatures, and an AI with no creatures and an opponent with none has nothing to trigger it. The Orchard gives
 * the opponent a 1/1 whenever it is tapped for mana, so it is tapped at the end step of the opponent's turn (the last
 * moment before the upkeep) and only when it is worth it:
 * <ul>
 *   <li>the Oath is in play and the opponent does not control more creatures yet,</li>
 *   <li>the first creature card of the library is a big one (the Oath would put it onto the battlefield) and not too
 *       many cards would be milled to reach it,</li>
 *   <li>there are enough untapped Orchards for the opponent to end up with more creatures.</li>
 * </ul>
 * The mana the Orchard makes is not needed: it is not a way to pay for anything here, only to make the token.
 */
public final class OathTiming {
    /** The Oath puts the cards before the creature into the graveyard: this many cards must remain after it. */
    private static final int LIBRARY_FLOOR = 3;

    private OathTiming() {
    }

    /** The mana ability of the Orchard to use now, or null. */
    public static SpellAbility sureCast(final Player ai) {
        if (ai == null) {
            return null;
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final Player active = ph.getPlayerTurn();
        if (active == null || ph.isPlayerTurn(ai) || !ai.isOpponentOf(active) || !ph.is(PhaseType.END_OF_TURN)
                || !game.getStack().isEmpty()) {
            return null;
        }
        boolean oath = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            oath |= "Oath of Druids".equals(c.getName());
        }
        final int mine = ai.getCreaturesInPlay().size();
        final int theirs = active.getCreaturesInPlay().size();
        if (!oath || theirs > mine || !wantsOath(ai)) {
            return null;
        }
        final int needed = mine - theirs + 1;
        SpellAbility first = null;
        int available = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!AiCardLists.has(AiCardLists.Role.GIVES_OPPONENT_CREATURE, c.getName()) || c.isTapped()) {
                continue;
            }
            for (final SpellAbility sa : c.getManaAbilities()) {
                sa.setActivatingPlayer(ai);
                if (sa.canPlay()) {
                    available++;
                    first = first == null ? sa : first;
                    break;
                }
            }
        }
        return available >= needed ? first : null;
    }

    /**
     * Whether the Oath would pay off now: the first creature card of the library is a creature worth cheating in, and
     * the cards before it can be milled without leaving the library nearly empty.
     */
    static boolean wantsOath(final Player ai) {
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        int index = 0;
        for (final Card c : library) {
            if (c.isCreature()) {
                final boolean big = ComboPlan.isBigTarget(c) || AiCardLists.has(AiCardLists.Role.CHEAT_TARGET, c.getName());
                return big && library.size() - index > LIBRARY_FLOOR;
            }
            index++;
        }
        return false;
    }
}

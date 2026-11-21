package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Abilities that put a creature card from the hand onto the battlefield (Quicksilver Amulet, Elvish Piper, Sneak Attack,
 * Meek Attack) and the moments at which that is a surprise worth having. Used at the wrong moment they are only a slow
 * way to cast a creature: the opponent sees it, and a creature that enters in the AI's own main phase cannot attack and
 * can be killed by a sorcery. The moments:
 * <ul>
 *   <li>{@link Window#OPPONENT_ATTACKS}: the opponent's attackers are declared and the blockers are not: the creature
 *       enters in time to block, and the opponent cannot take the attack back.</li>
 *   <li>{@link Window#OPPONENT_END_STEP}: nothing can answer it at sorcery speed, and the creature is ready to attack when
 *       the AI's turn begins. The mana of the turn is spare: the lands untap next.</li>
 *   <li>{@link Window#BEFORE_OWN_COMBAT}: only for a creature that can attack at once (it has haste, or the ability gives
 *       it haste, as Sneak Attack does).</li>
 * </ul>
 * This class says which of the three the game is in and whether the ability has a creature worth putting there; the
 * rollouts say whether it is better than waiting (see {@code FlatMonteCarloPicker}).
 */
public final class SurpriseEntry {
    public enum Window {
        NONE, OPPONENT_ATTACKS, OPPONENT_END_STEP, BEFORE_OWN_COMBAT
    }

    private SurpriseEntry() {
    }

    /** An activated, untargeted ability of a permanent in play that puts a creature card from the hand onto the battlefield. */
    public static boolean isEntryAbility(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isActivatedAbility() || sa.isManaAbility() || sa.isPwAbility() || sa.usesTargeting()
                || !host.isInZone(ZoneType.Battlefield) || sa.getApi() != ApiType.ChangeZone) {
            return false;
        }
        final String type = sa.getParamOrDefault("ChangeType", "");
        return "Hand".equals(sa.getParamOrDefault("Origin", "")) && "Battlefield".equals(sa.getParamOrDefault("Destination", ""))
                && type.contains("Creature");
    }

    /** The creature cards in the hand that the ability can put onto the battlefield. */
    public static List<Card> candidates(final Player ai, final SpellAbility sa) {
        final List<Card> found = new ArrayList<>();
        if (!isEntryAbility(sa)) {
            return found;
        }
        for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Hand), sa.getParam("ChangeType"), ai,
                sa.getHostCard(), sa)) {
            found.add(c);
        }
        return found;
    }

    /** Whether the creature attacks the moment it enters: it has haste, or the ability gives it haste. */
    public static boolean hasteOnEntry(final SpellAbility sa, final Card creature) {
        if (creature.hasKeyword(Keyword.HASTE)) {
            return true;
        }
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (sub.getParamOrDefault("Keywords", "").contains("Haste")) {
                return true;
            }
        }
        return false;
    }

    /** Whether the creature could block the attacker (as far as can be told from a card in the hand: flying and reach). */
    static boolean couldBlock(final Card creature, final Card attacker) {
        if (creature.hasKeyword("CARDNAME can't block.")) {
            return false;
        }
        return !attacker.hasKeyword(Keyword.FLYING) || creature.hasKeyword(Keyword.FLYING) || creature.hasKeyword(Keyword.REACH);
    }

    /** The moment the game is at, from the AI's point of view. */
    public static Window window(final Player ai) {
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!game.getStack().isEmpty()) {
            return Window.NONE;
        }
        if (ph.isPlayerTurn(ai)) {
            return ph.is(PhaseType.MAIN1) || ph.is(PhaseType.COMBAT_BEGIN) ? Window.BEFORE_OWN_COMBAT : Window.NONE;
        }
        if (ph.is(PhaseType.END_OF_TURN)) {
            return Window.OPPONENT_END_STEP;
        }
        final Combat combat = ph.getCombat();
        if (ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && combat != null && !combat.getAttackers().isEmpty()) {
            return Window.OPPONENT_ATTACKS;
        }
        return Window.NONE;
    }

    /**
     * Whether the ability is worth offering to the rollouts now: the game is at one of the moments, and the hand holds a
     * creature that suits it (one that could block an attacker, one that attacks at once, or any at the end step).
     */
    public static boolean offered(final Player ai, final SpellAbility sa) {
        final List<Card> creatures = candidates(ai, sa);
        if (creatures.isEmpty()) {
            return false;
        }
        switch (window(ai)) {
            case OPPONENT_END_STEP:
                return true;
            case BEFORE_OWN_COMBAT:
                for (final Card c : creatures) {
                    if (hasteOnEntry(sa, c)) {
                        return true;
                    }
                }
                return false;
            case OPPONENT_ATTACKS:
                final Combat combat = ai.getGame().getPhaseHandler().getCombat();
                for (final Card attacker : combat.getAttackers()) {
                    for (final Card c : creatures) {
                        if (couldBlock(c, attacker)) {
                            return true;
                        }
                    }
                }
                return false;
            default:
                return false;
        }
    }
}

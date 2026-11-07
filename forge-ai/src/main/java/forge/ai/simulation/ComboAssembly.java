package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.player.GameLossReason;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Putting a catalogued combo together on purpose. A combo of {@code ai-combos.txt} whose every slot is already filled
 * by the AI's own cards (in play, in hand, or in the graveyard for the reanimation pieces) and whose cards still in
 * hand can all be paid for this turn is assembled without rollouts: the cheapest piece first. The rollouts only see
 * the points of {@link ComboCatalog#term}, which hold for two pieces cast in the right order only by luck, and a
 * piece alone is worth nothing to the heuristics (an enchantment that does nothing by itself, a creature with no
 * attack) so it is held back. The combos that have a play of their own (the Oracle with the Consultation, the Vault
 * with its key, the Doomsday pile, Channel, the storm decks, Donate, the Hermit Druid, Dark Depths ...) are not
 * handled here: see {@link #COVERED}.
 */
public final class ComboAssembly {
    /** Combos that have a dedicated play and test; the first words of the catalogue name decide. */
    static final Set<String> COVERED = new HashSet<>(Arrays.asList(
            "Channel", "Time Vault", "Thassa's Oracle", "Doomsday", "Demonic Consultation", "Hermit Druid", "Tinker",
            "Ad Nauseam", "Tendrils", "Yawgmoth's Will", "Illusions of Grandeur", "Painter's Servant", "Hive Mind",
            "Dark Depths"));

    private ComboAssembly() {
    }

    /** Whether a dedicated play handles the combo. */
    public static boolean isCovered(final ComboCatalog.Entry e) {
        for (final String c : COVERED) {
            if (e.name.startsWith(c)) {
                return true;
            }
        }
        return false;
    }

    /** The first card of the player's that fills the slot, in the zones asked for, or null. */
    private static Card filler(final Iterable<Card> cards, final java.util.Set<String> slot) {
        for (final Card c : cards) {
            if (slot.contains(forge.ai.simulation.AiCardLists.normalize(c.getName()))) {
                return c;
            }
        }
        return null;
    }

    /**
     * The next spell that brings a combo closer, or null. {@code approve} is the AI's own check of the play (targets,
     * cost, the guards); candidates are the abilities it could play now.
     */
    public static SpellAbility next(final Player ai, final List<SpellAbility> candidates,
                                    final Predicate<SpellAbility> approve) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        final List<Card> hand = new ArrayList<>(ai.getCardsIn(ZoneType.Hand));
        final List<Card> play = new ArrayList<>(ai.getCardsIn(ZoneType.Battlefield));
        final List<Card> yard = new ArrayList<>(ai.getCardsIn(ZoneType.Graveyard));
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            if (isCovered(e)) {
                continue;
            }
            final List<Card> inHand = new ArrayList<>();
            boolean whole = true;
            for (final java.util.Set<String> slot : e.slots) {
                if (filler(play, slot) != null) {
                    continue;
                }
                final Card h = filler(hand, slot);
                if (h != null) {
                    inHand.add(h);
                } else if (filler(yard, slot) == null) {
                    whole = false;
                    break;
                }
            }
            if (!whole || inHand.isEmpty()) {
                continue;
            }
            int cost = 0;
            final List<Card> spells = new ArrayList<>();
            for (final Card c : inHand) {
                if (!c.isLand()) {
                    cost += c.getCMC();
                    spells.add(c);
                }
            }
            if (spells.isEmpty() || ComputerUtilMana.getAvailableManaEstimate(ai) < cost) {
                continue; // not the turn for it: the pieces are held
            }
            spells.sort((a, b) -> Integer.compare(a.getCMC(), b.getCMC()));
            for (final Card piece : spells) {
                for (final SpellAbility sa : candidates) {
                    if (sa != null && sa.getHostCard() == piece && sa.isSpell() && !sa.isLandAbility()) {
                        sa.setActivatingPlayer(ai);
                        if (approve.test(sa)) {
                            return sa;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * The finishing draw: a spell that makes a target player draw (Stroke of Genius, Brain Freeze is not one) cast at an
     * opponent whose whole library it takes, for the X the mana allows. The opponent loses at the next draw step, and
     * the rollouts only see that when they play on into the opponent's turn, so the play is made without them. Null when
     * no opponent can be drawn out, has Laboratory Maniac, cannot lose to an empty library, or no such spell is at hand.
     */
    public static SpellAbility finish(final Player ai, final List<SpellAbility> candidates,
                                      final Predicate<SpellAbility> approve) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                    || sa.getApi() != ApiType.Draw || !sa.usesTargeting() || !sa.hasParam("NumCards")) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            int draw;
            try {
                if (sa.costHasManaX()) {
                    draw = ComputerUtilCost.setMaxXValue(sa, ai, false);
                } else {
                    draw = AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa);
                }
            } catch (final RuntimeException e) {
                continue;
            }
            for (final Player opp : ai.getOpponents()) {
                final int library = opp.getCardsIn(ZoneType.Library).size();
                if (draw >= Math.max(1, library) && opp.canDraw() && !opp.cantLoseCheck(GameLossReason.Milled)
                        && !opp.isCardInPlay("Laboratory Maniac") && sa.canTarget(opp) && approve.test(sa)) {
                    return sa;
                }
            }
        }
        return null;
    }
}

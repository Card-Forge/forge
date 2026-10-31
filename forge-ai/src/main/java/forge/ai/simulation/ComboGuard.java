package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Plays that Flat Monte Carlo must not spend a card on, whatever the rollouts think of them. Three kinds:
 * <ul>
 *   <li><b>The legend rule</b>: reanimating (Reanimate, Animate Dead, Exhume, Goryo's Vengeance, ...) or cheating in
 *       a legendary creature the AI already has in play and that still does its job. One of the two copies would go
 *       straight back to the graveyard. A copy that is useless (it cannot attack, it is phased out, it has no power
 *       left, it leaves at end of turn) does not count: replacing it is fine.</li>
 *   <li><b>Duplicated win pieces</b>: a second Omniscience, Dream Halls, Sneak Attack, or a second copy of a win card
 *       of the deck, put onto the battlefield while the first one is there. Show and Tell is also not cast when every
 *       permanent in the hand is such a duplicate: it would only give the opponent a free permanent.</li>
 *   <li><b>Cantrips</b> (Brainstorm, Ponder, Preordain, ...) are kept in the hand while the AI is confident to win this
 *       turn or the next ({@link #winLikelySoon}): they dig for nothing then, and the mana and the card are worth
 *       more as a spare for a counterspell or a fizzled combo.</li>
 * </ul>
 * Everything here reads the real game state only, and every method falls back to "no objection" on an unexpected
 * state: it is a refinement and never breaks a turn.
 */
public final class ComboGuard {
    private ComboGuard() {
    }

    /** Effects a cantrip may have besides drawing: seeing or arranging the top of the library, a mana, a look at a hand. */
    private static final Set<String> CANTRIP_APIS = names("Draw", "Dig", "Scry", "Surveil", "RearrangeTopOfLibrary",
            "Mana", "RevealHand", "Reveal");

    /** When the AI is this far ahead on board (percent of the opponent's life) the next turn is taken as won. */
    private static final int NEXT_TURN_MARGIN_PERCENT = 125;

    // ------------------------------------------------------------------------------------------------ verdicts

    /**
     * Why the play should not be made, or null when there is nothing against it.
     *
     * @param aim the target the play is going to be aimed at when that is not what its targets say yet (a target
     *            variant, or what the heuristics aimed at before the rollouts disturbed it), else null
     */
    public static String problem(final Player ai, final SpellAbility sa, final GameObject aim, final ComboPlan plan) {
        if (ai == null || sa == null) {
            return null;
        }
        try {
            return problemImpl(ai, sa, aim, plan);
        } catch (final RuntimeException e) {
            return null; // a refinement only
        }
    }

    private static String problemImpl(final Player ai, final SpellAbility sa, final GameObject aim, final ComboPlan plan) {
        final Card host = sa.getHostCard();
        final SpellAbility single = FlatMonteCarloPicker.singleTargetingAbility(sa);
        // the legend rule: what a reanimation spell is aimed at
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (!a.usesTargeting() || !bringsBack(a)) {
                continue;
            }
            if (aim != null && a == single) {
                if (wouldBreakLegendRule(ai, a, aim)) {
                    return "legend-rule";
                }
            } else if (a.getTargets() != null) {
                for (final GameEntity e : a.getTargets().getTargetEntities()) {
                    if (wouldBreakLegendRule(ai, a, e)) {
                        return "legend-rule";
                    }
                }
            }
        }
        if (host == null) {
            return null;
        }
        // a cheating spell with nothing worth putting in: Show and Tell would only feed the opponent
        if (isHandCheat(sa) && !hasUsefulPermanentInHand(ai, host, plan)) {
            return "cheat-redundant";
        }
        // a second Omniscience or Sneak Attack
        if (sa.isSpell() && host.isPermanent() && !host.isLand() && AiCardLists.has(AiCardLists.Role.COMBO_ENGINE, host.getName())
                && controlsWorkingCopy(ai, host)) {
            return "duplicate-engine";
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ the legend rule

    /** Whether the ability puts a card from a graveyard onto the battlefield (Animate Dead is an aura that does it). */
    static boolean bringsBack(final SpellAbility a) {
        final ApiType api = a.getApi();
        if (api == ApiType.ChangeZone) {
            return "Battlefield".equals(a.getParamOrDefault("Destination", ""))
                    && a.getParamOrDefault("Origin", "").contains("Graveyard");
        }
        if (api == ApiType.Attach) {
            return a.getParamOrDefault("TgtZone", "").contains("Graveyard");
        }
        return false;
    }

    /**
     * Whether aiming the reanimation ability {@code ts} at the target would only bring back a legend the AI already has.
     * Cards of any graveyard count (the AI may reanimate what the opponent lost) but the rule is the AI's own.
     */
    public static boolean wouldBreakLegendRule(final Player ai, final SpellAbility ts, final GameObject target) {
        if (!(target instanceof Card) || ts == null || !bringsBack(ts)) {
            return false;
        }
        final Card c = (Card) target;
        return c.isInZone(ZoneType.Graveyard) && duplicatesWorkingLegend(ai, c);
    }

    /**
     * The card is a legend whose namesake the player already has in play and that is still doing its job, so that a
     * second one would only be thrown away by the legend rule.
     */
    public static boolean duplicatesWorkingLegend(final Player ai, final Card incoming) {
        if (ai == null || incoming == null || !incoming.getType().isLegendary()) {
            return false;
        }
        final CardCollectionView field = ai.getCardsIn(ZoneType.Battlefield);
        for (final Card c : field) {
            if ("Mirror Gallery".equals(c.getName())) {
                return false; // the legend rule does not apply
            }
        }
        for (final Card o : field) {
            if (o != incoming && o.getType().isLegendary() && o.getName().equals(incoming.getName()) && isWorking(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a permanent of the player is doing its job. A creature is not when it cannot attack (an effect on it, a
     * wall, an aura), is phased out, has no power left, stays tapped, or leaves at end of turn (Sneak Attack and the
     * like); another permanent is not when it is phased out or temporary. A legend that is not working may be
     * replaced by a second copy.
     */
    public static boolean isWorking(final Card c) {
        if (c == null || c.isPhasedOut()) {
            return false;
        }
        if (c.hasSVar("EndOfTurnLeavePlay")) {
            return false; // it will be gone before it matters
        }
        if (!c.isCreature()) {
            return true;
        }
        if (c.getNetPower() <= 0) {
            return false;
        }
        if (c.isTapped() && c.hasKeyword("CARDNAME doesn't untap during your untap step.")) {
            return false;
        }
        try {
            return canAttackNextTurn(c);
        } catch (final RuntimeException e) {
            return true;
        }
    }

    /**
     * Whether the creature can attack something next turn: summoning sickness and being tapped do not matter then,
     * effects that forbid it do (CombatUtil only knows the question for one defender, so every possible one is asked).
     */
    static boolean canAttackNextTurn(final Card attacker) {
        for (final GameEntity defender : CombatUtil.getAllPossibleDefenders(attacker.getController())) {
            if (CombatUtil.canAttackNextTurn(attacker, defender)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------- duplicated win pieces

    /**
     * A card that would add nothing to the battlefield of the player: a legend it already has (see
     * {@link #duplicatesWorkingLegend}), or another copy of an engine or win card of the deck whose first copy works.
     */
    public static boolean duplicatesWinPiece(final Player ai, final Card incoming, final ComboPlan plan) {
        if (ai == null || incoming == null) {
            return false;
        }
        if (duplicatesWorkingLegend(ai, incoming)) {
            return true;
        }
        if (incoming.isLand() || !incoming.isPermanent() || !isWinPiece(incoming, plan)) {
            return false;
        }
        return controlsWorkingCopy(ai, incoming);
    }

    private static boolean isWinPiece(final Card c, final ComboPlan plan) {
        return AiCardLists.has(AiCardLists.Role.COMBO_ENGINE, c.getName()) || plan != null && plan.isWinCard(c);
    }

    private static boolean controlsWorkingCopy(final Player ai, final Card c) {
        for (final Card o : ai.getCardsIn(ZoneType.Battlefield)) {
            if (o != c && o.getName().equals(c.getName()) && isWorking(o)) {
                return true;
            }
        }
        return false;
    }

    /** Show and Tell, Sneak Attack and the like: puts a card from the hand onto the battlefield. */
    static boolean isHandCheat(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host != null && sa.isSpell() && AiCardLists.has(AiCardLists.Role.HAND_CHEAT, host.getName())) {
            return true;
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.ChangeZone && "Hand".equals(a.getParamOrDefault("Origin", ""))
                    && "Battlefield".equals(a.getParamOrDefault("Destination", ""))
                    && !"Self".equals(a.getParamOrDefault("Defined", ""))) {
                // not an effect that only puts a land in (Gaea's Touch): that is no cheat
                final String type = a.getParamOrDefault("ChangeType", "");
                if (type.isEmpty() || type.contains("Creature") || type.contains("Artifact")
                        || type.contains("Enchantment") || type.contains("Permanent")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether the hand holds a permanent (other than the spell itself) that is worth putting in. */
    private static boolean hasUsefulPermanentInHand(final Player ai, final Card host, final ComboPlan plan) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c == host || c.isLand() || !c.isPermanent()) {
                continue;
            }
            if (!duplicatesWinPiece(ai, c, plan)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ cantrips

    /**
     * A cheap instant or sorcery that only draws or digs (and may look at or arrange the top of the library, put
     * cards back, or add a mana): Brainstorm, Ponder, Preordain, Opt, Sleight of Hand, Gitaxian Probe, Manamorphose.
     * Looting spells (they discard), life payments and anything that does something else are not cantrips.
     */
    public static boolean isCantrip(final SpellAbility sa) {
        if (sa == null || !sa.isSpell()) {
            return false;
        }
        final Card host = sa.getHostCard();
        if (host == null || host.isLand() || host.isCreature() || !(host.isInstant() || host.isSorcery())
                || host.getCMC() > 2) {
            return false;
        }
        boolean replacesItself = false;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == null) {
                continue;
            }
            final String name = api.name();
            if (name.equals("ChangeZone")) {
                // Brainstorm puts two cards back; any other zone change is something else
                if (!"Hand".equals(a.getParamOrDefault("Origin", "")) || !"Library".equals(a.getParamOrDefault("Destination", ""))) {
                    return false;
                }
                continue;
            }
            if (!CANTRIP_APIS.contains(name)) {
                return false;
            }
            if (a.usesTargeting() && !name.equals("RevealHand")) {
                return false;
            }
            replacesItself |= name.equals("Draw") || name.equals("Dig");
        }
        return replacesItself;
    }

    // --------------------------------------------------------------------------------------------- win confidence

    /**
     * Whether the AI is confident to win this turn or the next: the damage of its creatures that no blocker of the
     * opponent can stop is lethal (this turn when the combat is still to come, otherwise the next one with a margin),
     * or a free-casting engine (Omniscience) is working and a win card is in the hand. Only for a single opponent.
     * Storm decks never say yes: their cantrips are the count.
     */
    public static boolean winLikelySoon(final Player ai, final ComboPlan plan) {
        if (ai == null) {
            return false;
        }
        try {
            return winLikelyImpl(ai, plan);
        } catch (final RuntimeException e) {
            return false;
        }
    }

    private static boolean winLikelyImpl(final Player ai, final ComboPlan plan) {
        if (ai.getOpponents().size() != 1 || plan != null && plan.has(ComboPlan.Kind.STORM)) {
            return false;
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || opp.cantLose() || opp.getLife() <= 0) {
            return false;
        }
        final int life = opp.getLife();
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        final List<Card> blockersNow = new ArrayList<>();
        final List<Card> blockersLater = new ArrayList<>();
        for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature()) {
                blockersLater.add(c);
                if (!c.isTapped()) {
                    blockersNow.add(c);
                }
            }
        }
        final List<Card> attackNow = new ArrayList<>();
        final List<Card> attackLater = new ArrayList<>();
        final boolean combatToCome = ph.getPlayerTurn() == ai && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isCreature()) {
                continue;
            }
            if (combatToCome && CombatUtil.canAttack(c, opp)) {
                attackNow.add(c);
            }
            if (canAttackNextTurn(c)) {
                attackLater.add(c);
            }
        }
        if (combatToCome && unblockedDamage(attackNow, blockersNow) >= life) {
            return true;
        }
        if (unblockedDamage(attackLater, blockersLater) * 100 >= life * NEXT_TURN_MARGIN_PERCENT) {
            return true;
        }
        // Omniscience (or the like) is working and a win card waits in the hand: everything is free from here
        if (plan != null) {
            boolean engine = false;
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                engine |= AiCardLists.has(AiCardLists.Role.COMBO_ENGINE, c.getName()) && isWorking(c);
            }
            if (engine) {
                for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                    if (!c.isLand() && plan.isWinCard(c)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * What gets through: attackers that nothing can block count in full, of the others one is stopped for every
     * blocker, the biggest first. Trample and double strike are ignored (the answer is on the careful side).
     */
    static int unblockedDamage(final List<Card> attackers, final List<Card> blockers) {
        int damage = 0;
        final List<Integer> blockable = new ArrayList<>();
        for (final Card a : attackers) {
            final int power = Math.max(0, a.getNetPower());
            boolean stoppable = false;
            for (final Card b : blockers) {
                if (CombatUtil.canBlock(a, b)) {
                    stoppable = true;
                    break;
                }
            }
            if (stoppable) {
                blockable.add(power);
            } else {
                damage += power;
            }
        }
        Collections.sort(blockable, Collections.reverseOrder());
        for (int i = blockers.size(); i < blockable.size(); i++) {
            damage += blockable.get(i);
        }
        return damage;
    }

    private static Set<String> names(final String... names) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(names)));
    }
}

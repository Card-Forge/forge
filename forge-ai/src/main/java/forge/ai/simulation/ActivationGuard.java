package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import forge.ai.ComputerUtilCard;
import forge.ai.BounceValue;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayRelevance;
import forge.ai.ability.AnimateAi;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.keyword.Keyword;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostReturn;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTapType;
import forge.game.cost.CostUntapType;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * An activated ability that would do nothing: Oran-Rief, the Vastwood with no green creature that entered this turn, a
 * "tap target creature" with no creature to tap, a Planeswalker's "up to one target creature" with none in play.
 *
 * <p>The heuristics refuse these, but Flat Monte Carlo discovery treats a counter, token or animate effect as
 * development and re-adds it, and the rollout cannot see that the tap (and the mana) bought nothing. This guard reads
 * the ability itself, effect by effect, and says "idle" only when every effect in its chain is certain to do nothing
 * and nothing else is gained.
 *
 * <p>Each effect of the chain (the ability and its sub-abilities) is judged:
 * <ul>
 *   <li><b>targeted</b>: no legal candidate for the targets it needs (or, for "up to", no candidate at all), or fewer
 *   candidates than the minimum;</li>
 *   <li><b>"All" effects</b> (PutCounterAll, PumpAll, UntapAll, TapAll, DamageAll, DestroyAll, AnimateAll,
 *   ProtectionAll, RemoveCounterAll, SacrificeAll, TapOrUntapAll, ChangeZoneAll): the cards they name are not there (and for
 *   the one-sided ones, none on the side they would help or hurt);</li>
 *   <li><b>choices made on resolution</b>: a sacrifice with nothing to sacrifice, a card to choose with none to choose
 *   from, a zone change with nothing of the type in the zone it looks at;</li>
 *   <li>an effect that depends on one of those (it uses the target, the chosen or the remembered card) is idle with it;</li>
 *   <li>every other effect (draw, life, mana, tokens, ...) is taken to do something.</li>
 * </ul>
 * A Planeswalker ability that adds loyalty is never idle: the loyalty is worth the activation. Separately, an ability
 * whose cost cannot be paid (nothing to sacrifice, discard or tap for it) is refused.
 */
public final class ActivationGuard {
    private ActivationGuard() {
    }

    /** @return true for an activated, non-mana ability on the battlefield: the abilities this guard has an opinion about. */
    public static boolean isActivated(final SpellAbility sa) {
        return sa != null && sa.isActivatedAbility() && !sa.isManaAbility() && sa.getHostCard() != null
                && sa.getHostCard().isInZone(ZoneType.Battlefield);
    }

    /**
     * @return "no-valid-targets", "nothing-to-affect" or "cost-unpayable" when the ability would do nothing or cannot be
     * paid, else null
     */
    public static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || !isActivated(sa)) {
            return null;
        }
        try {
            return problemImpl(ai, sa);
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    private static String problemImpl(final Player ai, final SpellAbility sa) {
        if (costUnpayable(ai, sa)) {
            return "cost-unpayable";
        }
        if (AnimateAi.needsItsOwnMana(ai, sa)) {
            return "cost-unpayable"; // the land would tap itself to animate itself: a tapped creature
        }
        if (onlyValuableSacrifice(ai, sa)) {
            return "valuable-sacrifice";
        }
        if (poorSacrifice(ai, sa)) {
            return "poor-sacrifice";
        }
        if (enoughCopies(ai, sa)) {
            return "enough-copies";
        }
        if (declinedByAristocratLogic(ai, sa)) {
            return "aristocrat-declines";
        }
        if (loyaltyGain(sa)) {
            return null; // a Planeswalker's plus ability is worth its loyalty whatever the effect does
        }
        final String idle = idleChain(ai, sa);
        if (idle != null) {
            return idle; // the more specific reason first
        }
        final String pump = PlayRelevance.pointlessPump(ai, sa);
        return pump != null ? pump : badBounce(ai, sa);
    }

    /**
     * An instant or sorcery that would do nothing when cast (Doom Blade with no creature to destroy, Wrath of God with
     * an empty board): the heuristics refuse these, the rollouts' own discovery did not. Permanent spells, abilities
     * and spells that need something that is not there yet (a counterspell: the stack) are left to their own checks.
     *
     * @return "no-valid-targets" or "nothing-to-affect", else null
     */
    public static String spellProblem(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || !sa.isSpell() || sa.getHostCard() == null || sa.getHostCard().isPermanent()
                || sa.isCopied() || sa.getApi() == null || sa.getApi() == ApiType.Counter) {
            return null;
        }
        final Card host = sa.getHostCard();
        // a spell with a second use (Cascade, Storm, Rebound, Gravestorm) is worth casting for it: Violent Outburst with no creature
        // to pump still cascades
        if (host.hasKeyword(Keyword.CASCADE) || host.hasKeyword(Keyword.STORM) || host.hasKeyword(Keyword.REBOUND)
                || host.hasKeyword(Keyword.GRAVESTORM)) {
            return null;
        }
        try {
            for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
                // an aim at a spell (Misdirection, Counterspell, Spell Pierce) is for the stack, which is empty until the
                // opponent casts something
                if (node.usesTargeting() && node.getTargetRestrictions() != null
                        && node.getTargetRestrictions().getZone() != null
                        && node.getTargetRestrictions().getZone().contains(ZoneType.Stack)) {
                    return null;
                }
            }
            if (givesMoreThanItTakes(ai, sa)) {
                return "gift-exceeds-target";
            }
            final String idle = idleChain(ai, sa);
            if (idle != null) {
                return idle;
            }
            final String pump = PlayRelevance.pointlessPump(ai, sa);
            return pump != null ? pump : badBounce(ai, sa);
        } catch (final RuntimeException e) {
            return null; // a refinement only
        }
    }

    /** What a basic land is worth to the player who gets it, on the scale of {@link ComputerUtilCard#evaluateCreature}. */
    static final int LAND_GIFT = 130;
    /** What a 3/3 token is worth to the player who gets it (Beast Within, Generous Gift). */
    static final int TOKEN_GIFT = 190;

    /**
     * Manabarbs and its kin: what a play costs in life through the lands that it taps, against the life the AI can
     * spare. The reserve is what the opponent's creatures could hit for next turn (at least three). A play that removes
     * the source of the damage is the exception, it may take the life down to one; so is a play that costs nothing.
     * Returns true when the cost is too high.
     */
    public static boolean tooPainfulToPay(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || sa.isManaAbility() || sa.isTrigger() || !ai.canLoseLife()
                || ai.cantLoseForZeroOrLessLife()) {
            return false;
        }
        final List<SpellAbility> payment = ComputerUtilMana.estimateTapPayment(ai, sa);
        final int damage = ComputerUtilMana.tapDamage(ai, payment);
        if (damage <= 0) {
            return false;
        }
        final boolean clearsTapDamage = removesAllTapDamage(ai, sa);
        // Judge survival on the board the play leaves behind: removed attackers are gone, and creatures tapped for mana
        // cannot block during the opponent's next turn. Clearing the tap-damage source lowers the fixed reserve to 1,
        // but it must not make the AI ignore an attacker that will still hit it next turn.
        final int threat = nextTurnThreat(ai, removedByPlay(ai, sa), creaturesSpentOnMana(payment));
        final int reserve = clearsTapDamage ? Math.max(1, threat + 1) : Math.max(3, threat + 1);
        return ai.getLife() - damage < reserve;
    }

    /**
     * What the opponent's creatures can hit for on their next turn, not on this one: a tapped attacker untaps, a creature that
     * came this turn is no longer sick. Each untapped creature of the AI that can block takes the strongest attacker that it can
     * block out of the sum (a rough count: no tricks, no gang blocks). Defenders do not attack.
     */
    public static int nextTurnThreat(final Player ai) {
        return nextTurnThreat(ai, Collections.<Card>emptySet(), Collections.<Card>emptySet());
    }

    /**
     * {@link #nextTurnThreat(Player)} for the board that a play leaves behind: {@code gone} are creatures the play takes
     * off the table (neither attack nor block), {@code spent} are creatures of the AI tapped to pay for it (no block).
     */
    static int nextTurnThreat(final Player ai, final Collection<Card> gone, final Collection<Card> spent) {
        final List<Card> attackers = new ArrayList<>();
        final List<Card> availableBlockers = new ArrayList<>();
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCreaturesInPlay()) {
                // what it deals in combat (toughness under Doran), and whether it may attack at all (a defender under Arcades may)
                if (!gone.contains(c) && c.getNetCombatDamage() > 0
                        && StaticAbilityCantAttackBlock.getCantAttackKeyword(c) == null && !c.isDetained()
                        && StaticAbilityCantAttackBlock.findCantAttackAbility(c, ai) == null
                        && !(c.isTapped() && c.hasKeyword("CARDNAME doesn't untap during your untap step."))) {
                    attackers.add(c);
                }
            }
        }
        for (final Card blocker : ai.getCreaturesInPlay()) {
            // A creature that already carries lethal damage (or deathtouch damage, e.g. in Main 2) is dead before it can
            // block, unless it cannot be destroyed (indestructible). One that regenerates is tapped by it and cannot block
            // either, so a regeneration shield changes nothing here.
            final boolean doomed = (blocker.getLethalDamage() <= 0 || blocker.hasBeenDealtDeathtouchDamage())
                    && blocker.canBeDestroyed();
            if (!blocker.isTapped() && !gone.contains(blocker) && !spent.contains(blocker) && !doomed) {
                availableBlockers.add(blocker);
            }
        }

        // Assign scarce blockers to the attackers with the fewest legal options first. In particular, one ground
        // blocker must not be spent on an attacker that another blocker could handle while a flyer goes through.
        final List<ThreatTarget> assignments = new ArrayList<>(attackers.size());
        for (final Card attacker : attackers) {
            final Pair<Integer, Integer> limits = StaticAbilityCantAttackBlock.getMinMaxBlocker(attacker, ai);
            final List<Card> candidates = new ArrayList<>();
            for (final Card blocker : availableBlockers) {
                if (CombatUtil.canBlock(attacker, blocker, true)) {
                    candidates.add(blocker);
                }
            }
            int min = Math.max(1, limits.getLeft());
            if (attacker.hasKeyword("CARDNAME must be blocked by two or more creatures if able.")) {
                min = Math.max(min, 2); // Gorm the Great: with fewer blockers than that, the estimate treats it as unblocked
            }
            assignments.add(new ThreatTarget(attacker, candidates, min, limits.getRight()));
        }

        assignments.sort((a, b) -> {
            final int aSlack = a.blockers.size() - a.minBlockers;
            final int bSlack = b.blockers.size() - b.minBlockers;
            final int bySlack = Integer.compare(aSlack, bSlack);
            if (bySlack != 0) {
                return bySlack;
            }
            final int byRequirement = Integer.compare(b.minBlockers, a.minBlockers);
            return byRequirement != 0 ? byRequirement : Integer.compare(b.attacker.getNetCombatDamage(), a.attacker.getNetCombatDamage());
        });

        int threat = 0;
        for (final ThreatTarget assignment : assignments) {
            final Card attacker = assignment.attacker;
            final List<Card> candidates = new ArrayList<>();
            for (final Card blocker : assignment.blockers) {
                if (availableBlockers.contains(blocker)) {
                    candidates.add(blocker);
                }
            }
            final int power = Math.max(0, attacker.getNetCombatDamage());
            if (assignment.maxBlockers < assignment.minBlockers || candidates.size() < assignment.minBlockers) {
                // menace / a blocker restriction: not enough legal blockers
                threat += combatDamage(ai, attacker, attacker.hasDoubleStrike() ? power * 2 : power);
                continue;
            }

            if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                // For trample, assign the minimum legal group and credit only the damage those blockers can absorb.
                // This intentionally errs on the cautious side when additional blockers could also be assigned.
                candidates.sort((a, b) -> Integer.compare(b.getNetToughness(), a.getNetToughness()));
            }
            int blockedDamage = 0;
            for (int i = 0; i < assignment.minBlockers; i++) {
                final Card blocker = candidates.get(i);
                availableBlockers.remove(blocker);
                if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                    // Existing marked damage reduces what must be assigned as lethal; deathtouch needs only one.
                    blockedDamage += attacker.hasKeyword(Keyword.DEATHTOUCH) ? 1 : Math.max(0, blocker.getLethalDamage());
                }
            }
            if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                // Double strike can deal damage in both combat-damage steps. Do not assume a blocker survives the first one.
                final int damage = attacker.hasDoubleStrike() ? power * 2 : power;
                threat += combatDamage(ai, attacker, Math.max(0, damage - blockedDamage));
            }
        }
        return threat;
    }

    /** What this much combat damage from the attacker comes to after the replacement effects on the AI (Dictate of the Twin Gods, Furnace of Rath). */
    private static int combatDamage(final Player ai, final Card attacker, final int damage) {
        return damage <= 0 ? 0 : Math.max(0, ai.staticReplaceDamage(damage, attacker, true));
    }

    private static final class ThreatTarget {
        final Card attacker;
        final List<Card> blockers;
        final int minBlockers;
        final int maxBlockers;

        ThreatTarget(final Card attacker, final List<Card> blockers, final int minBlockers, final int maxBlockers) {
            this.attacker = attacker;
            this.blockers = blockers;
            this.minBlockers = minBlockers;
            this.maxBlockers = maxBlockers;
        }
    }

    /** The creatures of the AI that the payment taps (or sacrifices) for mana: they are not there to block next turn. */
    static Set<Card> creaturesSpentOnMana(final List<SpellAbility> payment) {
        final Set<Card> spent = new HashSet<>();
        for (final SpellAbility tap : payment) {
            final Card host = tap.getHostCard();
            if (host != null && host.isCreature()) {
                spent.add(host);
            }
        }
        return spent;
    }

    /**
     * The creatures that the play takes off the table before their next turn: the chosen targets (or the defined cards) of
     * a destroy, an exile or bounce, damage that is lethal, a shrink to nothing, and the creatures that a mass effect names.
     * Tapping and enchanting (Pacifism) are not counted: that threat stays in the reserve.
     */
    static Set<Card> removedByPlay(final Player ai, final SpellAbility sa) {
        final Set<Card> gone = new HashSet<>();
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            final ApiType api = node.getApi();
            if (api == null) {
                continue;
            }
            final Card host = node.getHostCard();
            switch (api) {
                case Destroy:
                    for (final Card c : aimedCards(node)) {
                        if (destroyable(c, node)) {
                            gone.add(c);
                        }
                    }
                    break;
                case ChangeZone:
                    if (leavesBattlefield(node)) {
                        gone.addAll(aimedCards(node));
                    }
                    break;
                case DealDamage: {
                    final int dmg = AbilityUtils.calculateAmount(host, node.getParamOrDefault("NumDmg", "0"), node);
                    for (final Card c : aimedCards(node)) {
                        if (c.isCreature() && dmg > 0 && destroyable(c, node) && c.getLethalDamage() <= dmg) {
                            gone.add(c);
                        }
                    }
                    break;
                }
                case Pump: {
                    final int def = AbilityUtils.calculateAmount(host, node.getParamOrDefault("NumDef", "0"), node);
                    for (final Card c : aimedCards(node)) {
                        if (c.isCreature() && def < 0 && c.getNetToughness() + def <= 0) {
                            gone.add(c);
                        }
                    }
                    break;
                }
                case DestroyAll:
                case DamageAll:
                case ChangeZoneAll:
                    gone.addAll(sweptBy(ai, node, api));
                    break;
                default:
                    break;
            }
        }
        return gone;
    }

    private static List<Card> aimedCards(final SpellAbility node) {
        if (node.usesTargeting()) {
            return new ArrayList<>(node.getTargets().getTargetCards());
        }
        if (node.hasParam("Defined")) {
            return new ArrayList<>(AbilityUtils.getDefinedCards(node.getHostCard(), node.getParam("Defined"), node));
        }
        return new ArrayList<>();
    }

    private static boolean destroyable(final Card c, final SpellAbility node) {
        return c.canBeDestroyed() && !regenerates(c, node);
    }

    /** A regeneration shield saves the creature from this destruction (it stays, tapped) unless the effect forbids it. */
    private static boolean regenerates(final Card c, final SpellAbility node) {
        return !node.hasParam("NoRegen") && c.getShieldCount() > 0 && c.canBeShielded();
    }

    private static boolean leavesBattlefield(final SpellAbility node) {
        return node.getParamOrDefault("Origin", "Battlefield").contains("Battlefield")
                && !"Battlefield".equals(node.getParam("Destination"));
    }

    /** The creatures of both sides that a mass effect (Wrath of God, Pyroclasm, a sweeping exile) takes. */
    private static List<Card> sweptBy(final Player ai, final SpellAbility node, final ApiType api) {
        final List<Card> swept = new ArrayList<>();
        final String valid = api == ApiType.ChangeZoneAll ? node.getParam("ChangeType") : node.getParam("ValidCards");
        if (valid == null || api == ApiType.ChangeZoneAll && !leavesBattlefield(node)) {
            return swept;
        }
        final int dmg = api == ApiType.DamageAll
                ? AbilityUtils.calculateAmount(node.getHostCard(), node.getParamOrDefault("NumDmg", "0"), node) : 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!c.isCreature() || !c.isValid(valid, ai, node.getHostCard(), node)) {
                continue;
            }
            if (api == ApiType.DestroyAll && !destroyable(c, node)) {
                continue;
            }
            if (api == ApiType.DamageAll && (dmg <= 0 || !destroyable(c, node) || c.getLethalDamage() > dmg)) {
                continue;
            }
            swept.add(c);
        }
        return swept;
    }

    /**
     * The play really takes the damage source away: a spell aimed at it (the chosen target, not any legal one), or a
     * mass effect that takes it with the rest. A Disenchant aimed at another permanent does not qualify.
     */
    private static boolean removesAllTapDamage(final Player ai, final SpellAbility sa) {
        boolean found = false;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!ComputerUtilMana.damagesWhoeverTaps(c, ai)) {
                continue;
            }
            found = true;
            if (!removesTapDamageSource(ai, sa, c)) {
                return false; // one recognized source remains active; do not grant the life-to-one exception
            }
        }
        return found;
    }

    /** True only when this play will actually remove this particular tap-damage permanent. */
    private static boolean removesTapDamageSource(final Player ai, final SpellAbility sa, final Card source) {
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            final ApiType api = node.getApi();
            if (api == ApiType.Destroy || api == ApiType.ChangeZone) {
                for (final Card target : aimedCards(node)) {
                    if (target != source) {
                        continue;
                    }
                    if (api == ApiType.Destroy && destroyable(source, node)
                            || api == ApiType.ChangeZone && leavesBattlefield(node)) {
                        return true;
                    }
                }
            } else if (api == ApiType.DestroyAll) {
                final String valid = node.getParam("ValidCards");
                if (valid != null && source.isValid(valid, ai, node.getHostCard(), node)
                        && destroyable(source, node)) {
                    return true;
                }
            } else if (api == ApiType.ChangeZoneAll) {
                // ChangeZoneAll uses ChangeType, not ValidCards.
                final String changeType = node.getParam("ChangeType");
                if (leavesBattlefield(node) && changeType != null
                        && source.isValid(changeType, ai, node.getHostCard(), node)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removal that pays its victim's controller (Path to Exile, Erode, Assassin's Trophy: a land; Beast Within, Generous
     * Gift: a 3/3) is only worth it for a target that is worth more than the gift: aimed at a mana dork or a small body
     * it spends the card to ramp the opponent, and the removal is gone for something that matters. Judged on the chosen
     * targets, else on everything the spell could be aimed at, so a real threat in play still gets the spell.
     */
    public static boolean givesMoreThanItTakes(final Player ai, final SpellAbility sa) {
        if (sa == null || !sa.isSpell() || !sa.usesTargeting()) {
            return false;
        }
        int gift = 0;
        for (SpellAbility node = sa.getSubAbility(); node != null; node = node.getSubAbility()) {
            if (node.getApi() == ApiType.ChangeZone && "Library".equals(node.getParam("Origin"))
                    && "Battlefield".equals(node.getParam("Destination"))
                    && node.getParamOrDefault("ChangeType", "").contains("Land")
                    && "TargetedController".equals(node.getParam("DefinedPlayer"))) {
                gift = Math.max(gift, LAND_GIFT);
            } else if (node.getApi() == ApiType.Token && "TargetedController".equals(node.getParam("TokenOwner"))) {
                gift = Math.max(gift, TOKEN_GIFT);
            }
        }
        if (gift == 0) {
            return false;
        }
        final List<Card> candidates = new ArrayList<>();
        if (sa.getTargets() != null && !sa.getTargets().getTargetCards().isEmpty()) {
            candidates.addAll(sa.getTargets().getTargetCards());
        } else {
            for (final Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
                if (sa.canTarget(c)) {
                    candidates.add(c);
                }
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        for (final Card c : candidates) {
            if (!c.isCreature() || c.isLand()) {
                return false; // a planeswalker, an artifact, an enchantment: not judged here
            }
            if (!isSmallBody(c) && ComputerUtilCard.evaluateCreature(c) >= gift) {
                return false; // something that is worth the gift
            }
        }
        return true;
    }

    /**
     * Evoking an elemental by exiling a card from the hand (Solitude, Fury) spends two cards for one enter-the-battlefield
     * removal: against a mana dork the AI gives up a good removal and a card for a card that only ramped them (#13026).
     * Judged on everything the removal could hit, so a creature of theirs that is worth the second card keeps the play
     * open. Only single-target creature removal is judged (Fury divides its damage and may kill several): Grief, Endurance and Subtlety do not aim at creatures.
     */
    public static boolean evokeOnlyHitsManaDorks(final Player ai, final SpellAbility sa) {
        if (sa == null || !sa.isSpell() || !sa.isEvoke() || sa.getPayCosts() == null || sa.getHostCard() == null) {
            return false;
        }
        boolean pitchesACard = false;
        for (final CostPart part : sa.getPayCosts().getCostParts()) {
            pitchesACard |= part instanceof CostExile && ((CostExile) part).getFrom().contains(ZoneType.Hand);
        }
        if (!pitchesACard) {
            return false; // paying mana for an evoke (Shriekmaw) is a different trade
        }
        boolean anyTarget = false;
        for (final Trigger t : sa.getHostCard().getTriggers()) {
            if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Destination"))
                    || !"Card.Self".equals(t.getParam("ValidCard"))) {
                continue;
            }
            final SpellAbility shared = t.ensureAbility(); // cached on the trigger and shared with the card state: never modified here
            final String valid = shared == null ? "" : shared.getParamOrDefault("ValidTgts", "");
            if (!valid.startsWith("Creature") || valid.contains("Player")) {
                continue;
            }
            if (shared == null || !shared.usesTargeting()) {
                continue;
            }
            // Targetability depends on the controller of this trigger: hexproof, shroud and protection
            // must not make an otherwise valuable creature look like a legal alternative to a mana dork.
            final SpellAbility etb;
            try {
                etb = shared.copy(ai);
            } catch (final RuntimeException e) {
                return false;
            }
            if (etb.hasParam("DividedAsYouChoose") || etb.hasParam("TargetMax") && !"1".equals(etb.getParam("TargetMax"))) {
                continue; // Fury: 4 damage divided can kill two mana dorks, a two-for-two. Only a single-target removal is judged here
            }
            for (final Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
                final boolean targetable;
                try {
                    targetable = etb.canTarget(c);
                } catch (final RuntimeException e) {
                    return false; // fail open if an unusual target restriction cannot be evaluated
                }
                if (!targetable) {
                    continue;
                }
                if (!c.isCreature()) {
                    return false; // a legal planeswalker target is an alternative worth preserving the evoke for
                }
                anyTarget = true;
                if (!isSmallBody(c) || c.getManaAbilities().isEmpty()) {
                    return false; // a creature that is worth the second card
                }
            }
        }
        return anyTarget;
    }

    /**
     * A bounce spell or ability aimed at the permanent of an opponent that is not worth the card (see {@link BounceValue}):
     * the creature that gives them a second enter-the-battlefield trigger (Vendilion Clique, Mulldrifter), that comes back
     * with flash, or that costs nothing to cast again. Judged on the chosen targets, else on everything it could be aimed at:
     * a creature of the AI's own among them (a rescue, a second trigger of its own) keeps the play open.
     *
     * @return "bounce-feeds-opponent", else null
     */
    public static String badBounce(final Player ai, final SpellAbility sa) {
        if (sa == null || sa.isTrigger() || sa.getHostCard() == null) {
            return null;
        }
        try {
            return bounceVerdict(ai, sa);
        } catch (final RuntimeException e) {
            // a refinement of the targeting: an odd game state must not stop the AI's turn, but it is not swept under
            System.err.println("badBounce could not judge " + sa + ": " + e);
            return null;
        }
    }

    private static String bounceVerdict(final Player ai, final SpellAbility sa) {
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            if (node.getApi() != ApiType.ChangeZone || !node.usesTargeting()
                    || !"Battlefield".equals(node.getParam("Origin")) || !"Hand".equals(node.getParam("Destination"))) {
                continue;
            }
            final List<Card> candidates = new ArrayList<>();
            if (node.getTargets() != null && !node.getTargets().getTargetCards().isEmpty()) {
                candidates.addAll(node.getTargets().getTargetCards());
            } else {
                for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
                    if (node.canTarget(c)) {
                        candidates.add(c);
                    }
                }
            }
            if (candidates.isEmpty()) {
                continue;
            }
            boolean worth = false;
            for (final Card c : candidates) {
                if (BounceValue.worthBouncing(ai, c)) {
                    worth = true;
                    break;
                }
            }
            if (!worth) {
                return "bounce-feeds-opponent";
            }
        }
        return null;
    }

    /** A creature worth less than what a removal spell gives away for it: a mana creature or another body with no use of its own. */
    private static boolean isSmallBody(final Card c) {
        // no ability but mana (Stoneforge Mystic, Mother of Runes are worth removing), no continuous effect on others (a lord), power 1 at most
        // (a 0/3 Treefolk Harbinger has nothing left to do once it has fetched its card)
        return c.getCMC() <= 2 && c.getNetPower() <= 1 && c.getStaticAbilities().stream().noneMatch(st -> st.checkMode(forge.game.staticability.StaticAbilityMode.Continuous)) // (flying and the like are statics too, but no lord's)
                && c.getSpellAbilities().stream().noneMatch(a -> a.isAbility() && !a.isManaAbility()) // (the cast of the card itself is a Spell, not an ability)
                && !c.hasSVar("HasAttackEffect") && c.getAttachedCards().isEmpty() && c.getCounters(CounterEnumType.P1P1) == 0;
    }

    private static String idleChain(final Player ai, final SpellAbility sa) {
        boolean providerIdle = false;
        boolean rootTargeted = false;
        int index = 0;
        for (SpellAbility node = sa; node != null; node = node.getSubAbility(), index++) {
            if (node.getApi() == ApiType.Cleanup) {
                continue; // bookkeeping at the end of a chain (clear the remembered cards): not an effect
            }
            final Verdict v = judge(ai, node);
            if (v == Verdict.USEFUL && providerIdle && dependsOnProvider(node)) {
                continue; // it acts on what the idle effect would have chosen: idle with it
            }
            if (v == Verdict.USEFUL) {
                return null;
            }
            if (v == Verdict.IDLE_TARGETS && index == 0) {
                rootTargeted = true;
            }
            if (v == Verdict.IDLE_TARGETS || v == Verdict.IDLE_CHOICE) {
                providerIdle = true;
            }
        }
        return rootTargeted ? "no-valid-targets" : "nothing-to-affect";
    }

    private enum Verdict { USEFUL, IDLE_TARGETS, IDLE_CHOICE, IDLE }

    // ------------------------------------------------------------------------------------------ cost

    /** The costs that need a card to pay with: nothing to sacrifice, discard, tap or exile for them. */
    private static boolean costUnpayable(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice || part instanceof CostDiscard || part instanceof CostTapType
                    || part instanceof CostUntapType || part instanceof CostReturn || part instanceof CostExile
                    || part instanceof CostRemoveCounter) {
                if (!part.canPay(sa, ai, false)) {
                    return true;
                }
            }
        }
        return sacrificesNeedMoreCards(ai, sa, cost);
    }

    /**
     * Whether the sacrifice parts of a cost cannot be paid with different permanents. Each part passes {@code canPay}
     * on its own, but a permanent is sacrificed once: one creature that is blue, black and red (Grixis Grimblade) is
     * not the blue, the black and the red creature that Demon's Herald asks for.
     */
    static boolean sacrificesNeedMoreCards(final Player ai, final SpellAbility sa, final Cost cost) {
        final Card host = sa.getHostCard();
        final List<CardCollection> slots = new ArrayList<>(); // one for each permanent the cost sacrifices
        for (final CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostSacrifice)) {
                continue;
            }
            if (part.payCostFromSource()) {
                slots.add(new CardCollection(host));
            } else if ("OriginalHost".equals(part.getType())) {
                if (sa.getOriginalHost() != null) {
                    slots.add(new CardCollection(sa.getOriginalHost()));
                }
            } else {
                final int amount;
                try {
                    amount = Integer.parseInt(part.getAmount());
                } catch (final NumberFormatException e) {
                    continue; // X or all: not counted here
                }
                final String type = part.getType().replace("+WithDifferentNames", "");
                if (type.contains("X")) {
                    continue; // what it asks for depends on X
                }
                final CardCollection fit = CardLists.filter(CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield),
                        type.split(";"), ai, host, sa), CardPredicates.canBeSacrificedBy(sa, false));
                for (int i = 0; i < amount; i++) {
                    slots.add(fit);
                }
            }
        }
        if (slots.size() < 2) {
            return false;
        }
        final Map<Card, Integer> takenBy = new HashMap<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!giveDistinctCard(slot, slots, takenBy, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    /** Matches the slot to a card that no other slot uses, moving another slot to a different card when it must. */
    private static boolean giveDistinctCard(final int slot, final List<CardCollection> slots,
                                            final Map<Card, Integer> takenBy, final Set<Card> seen) {
        for (final Card c : slots.get(slot)) {
            if (!seen.add(c)) {
                continue;
            }
            final Integer other = takenBy.get(c);
            if (other == null || giveDistinctCard(other, slots, takenBy, seen)) {
                takenBy.put(c, slot);
                return true;
            }
        }
        return false;
    }

    /** The most a creature may be worth to be sacrificed to an outlet for what the outlet gives (a weak token or creature). */
    static final int FODDER_VALUE = 140;

    /**
     * A sacrifice outlet (Viscera Seer, Ashnod's Altar, Carrion Feeder) offered to the rollouts only when there is fodder:
     * a token, a creature marked to be sacrificed, or a weak one. The cost is paid with the AI's worst creature, and
     * the rollouts cannot see what a sacrificed good creature was worth beyond the evaluator's number.
     */
    private static boolean onlyValuableSacrifice(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostSacrifice)) {
                continue;
            }
            final String type = part.getType();
            if (type == null || "CARDNAME".equals(type) || part.payCostFromSource()) {
                continue; // the permanent itself: a sacrifice of its own
            }
            boolean any = false;
            for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split("/")[0],
                    ai, sa.getHostCard(), sa)) {
                any = true;
                if (!c.isCreature() || c.isToken() || c.hasSVar("SacMe")
                        || ComputerUtilCard.evaluateCreature(c) <= FODDER_VALUE) {
                    return false;
                }
            }
            if (any) {
                return true;
            }
        }
        return false;
    }

    /**
     * A sacrifice outlet that gives a creature up for less than it is worth, or gives the host up for its own sake
     * (Carrion Feeder, which fed its whole team to itself, and then itself). The creature that pays is the one the AI's
     * own cost payment would choose; it is fine to give up when it is fodder (a token, a creature marked SacMe, a weak
     * one), when it is about to die anyway, or when what the ability gives is worth at least as much as the creature.
     */
    /** The most hasty copies a turn needs: a copy loop (Splinter Twin, Kiki-Jiki) stops here whatever the life total. */
    public static final int MAX_COPIES = 40;

    /**
     * A "tap: create a hasty copy that goes away at the end of turn" ability (Splinter Twin, Kiki-Jiki) is used for as
     * long as it makes something: the copy's enters trigger is worth it, or another attacker. It stops when the copies
     * already in play are lethal (their power covers the life of the weakest opponent) or there are as many as a
     * turn needs ({@link #MAX_COPIES}); more is the loop going round for nothing.
     */
    public static boolean enoughCopies(final Player ai, final SpellAbility sa) {
        final SpellAbility root = sa == null ? null : sa.getRootAbility();
        if (root == null || root.getApi() != ApiType.CopyPermanent || !root.hasParam("AtEOT")
                || !root.getParamOrDefault("AddKeywords", "").contains("Haste")) {
            return false;
        }
        final Player opp = ai.getWeakestOpponent();
        final boolean goblinCombo = root.getHostCard() != null && root.getHostCard().getName().equals("Conspicuous Snoop");
        int copies = 0;
        int power = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isToken() && c.isCreature() && c.hasKeyword(Keyword.HASTE) && !c.isTapped()) {
                copies++;
                power += Math.max(0, c.getNetPower());
            }
        }
        // the Snoops are worth a life each to Sling-Gang Lieutenant, whatever their power
        return copies >= MAX_COPIES || !goblinCombo && opp != null && copies > 0 && power >= opp.getLife() + 3;
    }

    static boolean poorSacrifice(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final Card host = sa.getHostCard();
        if (cost == null || host == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostSacrifice) || part.payCostFromSource()) {
                continue;
            }
            final String type = part.getType();
            if (type == null || "CARDNAME".equals(type) || !"1".equals(part.getAmount())) {
                continue; // a sacrifice of its own, or of several: not judged here
            }
            final CardCollection paid = forge.ai.ComputerUtil.chooseSacrificeType(ai, type, sa, null, false, 1, null);
            if (paid == null || paid.isEmpty()) {
                continue; // cannot be paid: costUnpayable says so
            }
            final Card c = paid.get(0);
            if (c.equals(host)) {
                if (benefitsOnlyHost(sa)) {
                    return true; // fed to itself for its own benefit: nothing is left to benefit
                }
                continue;
            }
            if (!c.isCreature() || c.isToken() || c.hasSVar("SacMe") || ComputerUtilCard.evaluateCreature(c) <= FODDER_VALUE
                    || ComputerUtilCard.hasActiveUndyingOrPersist(c)) {
                continue;
            }
            if (forge.ai.ComputerUtil.predictThreatenedObjects(ai, null, true).contains(c)) {
                continue; // it is lost anyway
            }
            if (forge.ai.ability.SacrificeWorth.rewardWithRoot(sa) < forge.ai.ability.SacrificeWorth.cost(ai, c)) {
                return true;
            }
        }
        return false;
    }

    /** Every step of the ability acts on the host alone (counters on itself, a keyword for itself). */
    private static boolean benefitsOnlyHost(final SpellAbility sa) {
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            if (node.getApi() == ApiType.Cleanup) {
                continue;
            }
            if (node.usesTargeting() || !"Self".equals(node.getParamOrDefault("Defined", "Self"))) {
                return false;
            }
        }
        return true;
    }

    /**
     * An outlet with its own logic for when to use it (AILogic$ AristocratCounters: Carrion Feeder and its kind) is
     * asked, because the rollouts' own list of abilities does not go through that logic.
     */
    private static boolean declinedByAristocratLogic(final Player ai, final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        if (!logic.startsWith("AristocratCounters")) {
            return false;
        }
        return !forge.ai.SpecialAiLogic.doAristocratWithCountersLogic(ai, sa).willingToPlay();
    }

    /** A loyalty ability that puts loyalty counters on the Planeswalker (+N, or an X it cannot read). */
    public static boolean gainsLoyalty(final SpellAbility sa) {
        return loyaltyGain(sa);
    }

    private static boolean loyaltyGain(final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null || !sa.isPwAbility()) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPutCounter && ((CostPutCounter) part).getCounter().is(CounterEnumType.LOYALTY)) {
                try {
                    return Integer.parseInt(part.getAmount().trim()) > 0;
                } catch (final NumberFormatException e) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ one effect

    private static Verdict judge(final Player ai, final SpellAbility node) {
        if (node.usesTargeting()) {
            if (node.getTargetRestrictions() == null) {
                return Verdict.USEFUL;
            }
            final Verdict refined = targetedRefinement(ai, node);
            if (refined != null) {
                return refined;
            }
            final int min = node.getMinTargets();
            final boolean idle = min > 0
                    ? node.getTargetRestrictions().getNumCandidates(node) < min
                    : !node.getTargetRestrictions().hasCandidates(node);
            return idle ? Verdict.IDLE_TARGETS : Verdict.USEFUL;
        }
        final ApiType api = node.getApi();
        if (alreadyDone(node)) {
            return Verdict.IDLE;
        }
        if (api == null || node.hasParam("Defined") && api != ApiType.Sacrifice) {
            return Verdict.USEFUL;
        }
        final Card host = node.getHostCard();
        switch (api) {
            case PutCounterAll:
            case PumpAll:
            case UntapAll:
            case TapAll:
            case DamageAll:
            case DestroyAll:
            case AnimateAll:
            case ProtectionAll:
            case RemoveCounterAll:
            case SacrificeAll:
            case TapOrUntapAll:
                return allEffect(ai, node, api, host);
            case ChangeZoneAll:
                return changeZoneAll(ai, node, host);
            case Sacrifice:
                return sacrifice(ai, node, host);
            case ChooseCard:
                return chooseCard(ai, node, host);
            case ChangeZone:
                return changeZone(ai, node, host);
            default:
                return Verdict.USEFUL;
        }
    }

    /** The parameters of a Pump that do more than hand out keywords (a Pump with any of them is not judged here). */
    private static final String[] PUMP_EXTRAS = { "NumAtt", "NumDef", "RemoveKW", "KWChoice", "RandomKeyword",
            "RandomKWNum", "DefinedKW", "DefinedLandwalk", "AtEOT", "RememberObjects", "ImprintCards", "RememberPumped",
            "Radiance", "SharedKeywordsZone", "CanBlockAmount", "CanBlockAny", "ThisDefinedAndTgts" };

    /**
     * An effect that the game has already done to the host and that cannot be done twice:
     * <ul>
     *   <li>Adapt (Emperor of Bones, Sharktocrab): nothing happens while the creature has a +1/+1 counter, unless a
     *   static ability lets it adapt anyway;</li>
     *   <li>Monstrosity: nothing happens to a creature that is already monstrous;</li>
     *   <li>a keyword ability ("{1}: gains flying until end of turn") when the creature has all the keywords already.</li>
     * </ul>
     * The activation costs mana and gains nothing, and Flat Monte Carlo cannot tell a repeat from the first.
     */
    static boolean alreadyDone(final SpellAbility node) {
        final Card host = node.getHostCard();
        if (host == null || node.usesTargeting()
                || node.hasParam("Defined") && !"Self".equals(node.getParam("Defined"))) {
            return false;
        }
        if (node.getApi() == ApiType.PutCounter) {
            if (node.hasParam("Adapt")) {
                return host.getCounters(CounterEnumType.P1P1) > 0
                        && !forge.game.staticability.StaticAbilityAdapt.anyWithAdapt(node, host);
            }
            return node.hasParam("Monstrosity") && host.isMonstrous();
        }
        // a shield that is up, a tap of the tapped, an untap of the untapped, an animate that has happened
        if (node.getApi() == ApiType.Regenerate && host.getShieldCount() > 0 && !node.hasParam("Cleanup")) {
            return true;
        }
        if (node.getApi() == ApiType.Tap && host.isTapped() || node.getApi() == ApiType.Untap && !host.isTapped()) {
            return true;
        }
        if (alreadyAnimated(node, host)) {
            return true;
        }
        if (node.getApi() == ApiType.Pump && node.hasParam("KW")) {
            for (final String extra : PUMP_EXTRAS) {
                if (node.hasParam(extra)) {
                    return false;
                }
            }
            for (final String kw : node.getParam("KW").split(" & ")) {
                if (!host.hasKeyword(kw)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** A pump that hands out keywords and nothing else. */
    private static boolean keywordGrantOnly(final SpellAbility node) {
        if (!node.hasParam("KW") || node.hasParam("NumAtt") || node.hasParam("NumDef")) {
            return false;
        }
        for (final String extra : PUMP_EXTRAS) {
            if (node.hasParam(extra)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAllKeywords(final Card c, final SpellAbility node) {
        for (final String kw : node.getParam("KW").split(" & ")) {
            if (!c.hasKeyword(kw)) {
                return false;
            }
        }
        return true;
    }

    /** An animate of the host that has already happened this turn: it is a creature of those types and that size already. */
    private static boolean alreadyAnimated(final SpellAbility node, final Card host) {
        if (node.getApi() != ApiType.Animate || !node.hasParam("Types") || !host.isCreature()
                || node.hasParam("RemoveTypes") || node.hasParam("RemoveCreatureTypes") || node.hasParam("Colors")
                || node.hasParam("OverwriteTypes") || node.hasParam("Abilities") || node.hasParam("Triggers")) {
            return false;
        }
        for (final String t : node.getParam("Types").split(",")) {
            if (!host.getType().hasStringType(t.trim())) {
                return false;
            }
        }
        try {
            if (node.hasParam("Power") && host.getCurrentPower() != Integer.parseInt(node.getParam("Power").trim())) {
                return false;
            }
            if (node.hasParam("Toughness") && host.getCurrentToughness() != Integer.parseInt(node.getParam("Toughness").trim())) {
                return false;
            }
        } catch (final NumberFormatException e) {
            return false;
        }
        if (node.hasParam("Keywords")) {
            for (final String kw : node.getParam("Keywords").split(" & ")) {
                if (!host.hasKeyword(kw)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A pump with numbers it can read, none of them negative, and no keyword that hurts (Giant Growth, not a curse). */
    static boolean beneficialPump(final SpellAbility node) {
        if (!node.hasParam("NumAtt") && !node.hasParam("NumDef") && !node.hasParam("KW")) {
            return false;
        }
        for (final String param : new String[] { "NumAtt", "NumDef" }) {
            if (node.hasParam(param)) {
                try {
                    if (Integer.parseInt(node.getParam(param).trim()) < 0) {
                        return false;
                    }
                } catch (final NumberFormatException e) {
                    return false; // an X it cannot read: not judged
                }
            }
        }
        final String kw = node.getParamOrDefault("KW", "");
        return !kw.contains("can't") && !kw.contains("Defender") && !node.hasParam("RemoveKW");
    }

    /**
     * The counters go on something that is not a creature yet, and a later part of the same ability makes it one
     * (Case of the Filched Falcon: counters on a noncreature artifact, which then becomes a 0/0 creature).
     */
    static boolean animatesTargetToCreature(final SpellAbility node) {
        for (SpellAbility a = node; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Animate && a.getParamOrDefault("Types", "").contains("Creature")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two targeted effects whose candidates are not all worth the ability. A +1/+1 counter belongs on a creature
     * (Ozolith, the Shattered Spire and its like also name noncreature artifacts, and a counter there does nothing),
     * and an Equipment moved onto the creature it is already attached to does nothing, unless something in play
     * pays for every attachment (a "becomes attached" trigger), which is what makes an equip {0} loop worth going round.
     */
    private static Verdict targetedRefinement(final Player ai, final SpellAbility node) {
        final TargetRestrictions tr = node.getTargetRestrictions();
        final ApiType api = node.getApi();
        if (api == ApiType.PutCounter && "P1P1".equals(node.getParam("CounterType")) && !animatesTargetToCreature(node)) {
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (e instanceof Card && ((Card) e).isCreature()) {
                    return null; // a real target exists: the ordinary count decides
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Pump && beneficialPump(node)) {
            // a pump that only helps (Giant Growth) when every creature it could go on is an opponent's: it helps him; a
            // grant of keywords (Arnjlot's Ascent: flying) when every creature of its own already has them
            final boolean keywordsOnly = keywordGrantOnly(node);
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (!(e instanceof Card)) {
                    return null; // a player: the ordinary count decides
                }
                final Card c = (Card) e;
                if (!c.getController().isOpponentOf(ai) && !(keywordsOnly && hasAllKeywords(c, node))) {
                    return null; // something of its own that gains from it: the ordinary count decides
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Tap || api == ApiType.Untap) {
            // tapping what is tapped already, or untapping what is untapped (and never an opponent's permanent: untapping it helps him)
            final boolean tap = api == ApiType.Tap;
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (!(e instanceof Card)) {
                    return null;
                }
                final Card c = (Card) e;
                if (tap ? !c.isTapped() : c.getController().equals(ai) && c.isTapped()) {
                    return null; // something it works on (a tap of an untapped permanent of its own is not judged)
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Regenerate && !node.hasParam("Cleanup")) {
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (!(e instanceof Card) || ((Card) e).getController().equals(ai) && ((Card) e).getShieldCount() == 0) {
                    return null; // a creature of its own that has no shield yet
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Destroy && !node.hasParam("Sacrifice")) {
            // nothing it could destroy: every opposing candidate is indestructible
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (!(e instanceof Card)) {
                    return null;
                }
                final Card c = (Card) e;
                if (!c.getController().isOpponentOf(ai) || !c.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                    return null; // a permanent of its own (not judged) or one that can be destroyed
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Attach && node.isEquip() && node.getHostCard().isEquipping()
                && !attachPayoff(ai, node.getHostCard())) {
            final Card current = node.getHostCard().getEquipping();
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (e != current) {
                    return null;
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        return null;
    }

    /**
     * Moving an Equipment onto the creature it is already on does nothing by itself, so it is only worth doing as a step
     * of a combo the AI knows (ai-combos.txt) that is whole on its side of the table, with the Equipment or a permanent
     * that triggers when something becomes attached as one of its pieces. A "becomes attached" trigger on its own is no
     * reason: most of them pay once, and an equip {0} loop for a trickle of value is how the AI burns its turn.
     */
    private static boolean attachPayoff(final Player ai, final Card equipment) {
        final CardCollection mine = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            if (!e.assembledBy(mine)) {
                continue;
            }
            if (e.fillsSomeSlot(equipment)) {
                return true;
            }
            for (final Card c : e.fillers(mine)) {
                for (final forge.game.trigger.Trigger t : c.getTriggers()) {
                    if (t.getMode() == forge.game.trigger.TriggerType.Attached) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Verdict allEffect(final Player ai, final SpellAbility node, final ApiType api, final Card host) {
        if (!node.hasParam("ValidCards")) {
            return Verdict.USEFUL;
        }
        final String valid = node.getParam("ValidCards");
        if (readsRuntime(valid)) {
            return Verdict.USEFUL;
        }
        final ZoneType zone = node.hasParam("ValidZone") ? ZoneType.smartValueOf(node.getParam("ValidZone"))
                : ZoneType.Battlefield;
        if (zone == null) {
            return Verdict.USEFUL;
        }
        final CardCollection matches = CardLists.getValidCards(ai.getGame().getCardsIn(zone), valid, ai, host, node);
        if (matches.isEmpty()) {
            return Verdict.IDLE;
        }
        final boolean curse = node.isCurse();
        final boolean any;
        switch (api) {
            case PutCounterAll:
            case PumpAll:
            case AnimateAll:
            case ProtectionAll:
                // a boost helps its own side, a curse the opposing one
                any = matches.anyMatch(c -> curse ? c.getController().isOpponentOf(ai) : c.getController().equals(ai));
                break;
            case UntapAll:
                any = matches.anyMatch(c -> c.isTapped() && c.getController().equals(ai));
                break;
            case TapAll:
                any = matches.anyMatch(c -> !c.isTapped() && c.getController().isOpponentOf(ai));
                break;
            case DamageAll:
            case DestroyAll:
                any = matches.anyMatch(c -> c.getController().isOpponentOf(ai));
                break;
            default: // counters to remove, sacrifices, tap or untap: anything matching is something
                any = true;
                break;
        }
        return any ? Verdict.USEFUL : Verdict.IDLE;
    }

    private static Verdict changeZoneAll(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("ChangeType") || !node.hasParam("Origin") || readsRuntime(node.getParam("ChangeType"))) {
            return Verdict.USEFUL;
        }
        final List<ZoneType> origin = ZoneType.listValueOf(node.getParam("Origin"));
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : origin) {
            cards.addAll(ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("ChangeType"), ai, host, node).isEmpty()
                ? Verdict.IDLE : Verdict.USEFUL;
    }

    /** "Sacrifice a creature": nothing to sacrifice. (The source itself, "Self", can always be.) */
    private static Verdict sacrifice(final Player ai, final SpellAbility node, final Card host) {
        if (node.hasParam("Defined") && !"You".equals(node.getParam("Defined"))) {
            return Verdict.USEFUL;
        }
        final String valid = node.getParamOrDefault("SacValid", "Self");
        if (valid.contains("Self") || readsRuntime(valid)) {
            return Verdict.USEFUL;
        }
        return CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    private static Verdict chooseCard(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("Choices") || readsRuntime(node.getParam("Choices"))) {
            return Verdict.USEFUL;
        }
        final List<ZoneType> zones = node.hasParam("ChoiceZone") ? ZoneType.listValueOf(node.getParam("ChoiceZone"))
                : List.of(ZoneType.Battlefield);
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("Choices"), ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    /** A zone change that picks its card on resolution from a zone that is not the library. */
    private static Verdict changeZone(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("Origin") || !node.hasParam("ChangeType") || readsRuntime(node.getParam("ChangeType"))) {
            return Verdict.USEFUL;
        }
        if (node.hasParam("Hidden") || node.hasParam("Reveal")) {
            return Verdict.USEFUL; // a hidden search or a wish: the cards are not readable from the game state
        }
        final List<ZoneType> origin = ZoneType.listValueOf(node.getParam("Origin"));
        // the AI's own hand is readable (Sorin, Imperious Bloodlord's -3 with no Vampire in it does nothing), unless the
        // ability names another player's hand or lets someone else choose
        final boolean ownHandOnly = !node.hasParam("DefinedPlayer") && !node.hasParam("Chooser") && !node.hasParam("Defined")
                && !node.hasParam("ChoiceZone") && !node.hasParam("GainControl");
        for (final ZoneType z : origin) {
            if (z == ZoneType.Hand && ownHandOnly) {
                continue;
            }
            if (z != ZoneType.Battlefield && z != ZoneType.Graveyard && z != ZoneType.Exile) {
                // Library, Sideboard (outside the game), another hand, Stack, Command: not judged, a tutor or wish is never idle
                return Verdict.USEFUL;
            }
        }
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : origin) {
            cards.addAll(z == ZoneType.Hand ? ai.getCardsIn(ZoneType.Hand) : ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("ChangeType"), ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    /** A valid-cards string that depends on something decided while the ability resolves: not read. */
    private static boolean readsRuntime(final String valid) {
        return valid.contains("Targeted") || valid.contains("Remembered") || valid.contains("Imprinted")
                || valid.contains("Chosen") || valid.contains("ParentTarget") || valid.contains("TriggeredCard");
    }

    /** An effect that acts on the card or player an earlier effect of the chain would have targeted or chosen. */
    private static boolean dependsOnProvider(final SpellAbility node) {
        final List<String> values = new ArrayList<>(node.getMapParams().values());
        for (final String v : values) {
            if (v.contains("Targeted") || v.contains("ParentTarget") || v.contains("Chosen")
                    || v.contains("Remembered")) {
                return true;
            }
        }
        return false;
    }
}

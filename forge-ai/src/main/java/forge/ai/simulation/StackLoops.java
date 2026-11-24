package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.CostTap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Loops that are played with something on the stack.
 * <ul>
 * <li><b>Basalt Monolith with Rings of Brighthearth.</b> Tap the Monolith for {C}{C}{C}, pay {3} to untap it, and Rings of
 * Brighthearth copies the untap for {2}. The copy resolves first and untaps the Monolith: with the original untap still on the
 * stack it is tapped for {C}{C}{C} again, and the original untaps it once more. A round is six mana in and five out: one over.
 * The steps are two at an empty stack (the tap; the untap, with the pool and the lands to pay both the {3} and the {2}) and
 * one with the original untap on the stack and the Monolith untapped: {@link #response}.</li>
 * <li><b>Naru Meha with Ghostly Flicker.</b> Ghostly Flicker is cast on two lands, and Naru Meha (flash) is cast in response: her
 * enter trigger copies the Flicker, the copy flickers Naru Meha and a land, her enter trigger copies the Flicker again, and so
 * on. The land is tapped for mana each time it is back (with her trigger on the stack) and flickered again: a mana a round.
 * The copy that ends it flickers two lands and not Naru Meha, once the pool holds what the hand costs.</li>
 * </ul>
 */
public final class StackLoops {
    private static final int LIMIT = 80;
    private static final int RINGS = 2;
    private static final String NARU = "Naru Meha, Master Wizard";
    private static final String FLICKER = "Ghostly Flicker";
    private static final int NARU_ROUNDS = 60;

    private StackLoops() {
    }

    private static Card ringsOf(final Player ai) {
        return LoopRunner.inPlay(ai, "Rings of Brighthearth");
    }

    /** The paid untap of the card itself, with no target. */
    private static SpellAbility untapOf(final Iterable<SpellAbility> candidates, final Card host) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isActivatedAbility() && !sa.isManaAbility()
                    && sa.getApi() == ApiType.Untap && !sa.usesTargeting()
                    && "Self".equals(sa.getParamOrDefault("Defined", "Self"))) {
                return sa;
            }
        }
        return null;
    }

    /** The tap for mana of the card, which costs only the tap. */
    private static SpellAbility tapOf(final Iterable<SpellAbility> candidates, final Card host) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isManaAbility() && sa.getApi() == ApiType.Mana
                    && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostTap.class)
                    && sa.getPayCosts().getCostMana() == null) {
                return sa;
            }
        }
        return null;
    }

    /** A round makes this many mana over, with the Rings: two taps, the untap and the copy. Zero or less goes nowhere. */
    private static int roundNet(final Player ai, final Card host, final SpellAbility tap, final SpellAbility untap) {
        return 2 * ManaLoopPlan.produced(ai, host, tap) - ManaLoopPlan.untapCost(ai, untap) - RINGS;
    }

    /**
     * Whether the untap that is copied is the loop's: a permanent that taps for more than its untap costs
     * is the loop of {@link ManaLoopPlan}, and one that does not is this loop's (with the Rings it pays).
     */
    public static boolean copyIsTheLoop(final Player ai, final SpellAbility copied) {
        final Card host = copied == null ? null : copied.getHostCard();
        if (host == null || copied.getApi() != ApiType.Untap || ringsOf(ai) == null || !host.isTapped()
                || ManaLoopPlan.goal(ai) <= 0) {
            return false;
        }
        final SpellAbility tap = tapOf(host.getSpellAbilities(), host);
        return tap != null && roundNet(ai, host, tap, copied) > 0;
    }

    /** The tap and the untap at an empty stack. */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        final SpellAbility twin = twinStart(ai, candidates, approve);
        if (twin != null) {
            return twin;
        }
        final SpellAbility chain = chainStart(ai, candidates, approve);
        if (chain != null) {
            return chain;
        }
        final SpellAbility start = naruStart(ai, candidates, approve);
        if (start != null) {
            return start;
        }
        final int goal = ManaLoopPlan.goal(ai);
        if (ringsOf(ai) == null || goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            if (host.isLand() || host.isCreature() && host.isSick()) {
                continue;
            }
            final SpellAbility tap = tapOf(candidates, host);
            final SpellAbility untap = untapOf(candidates, host);
            if (tap == null || untap == null || roundNet(ai, host, tap, untap) <= 0) {
                continue;
            }
            final int cost = ManaLoopPlan.untapCost(ai, untap);
            if (ManaLoopPlan.produced(ai, host, tap) > cost) {
                continue; // it makes mana on its own: the plan of the untap loops has it
            }
            if (!host.isTapped()) {
                tap.setActivatingPlayer(ai);
                if (!RunawayLoop.exceeded(tap, LIMIT) && approve.test(tap)) {
                    VetoLog.note(ai, "rings-loop | " + host.getName() + " taps, pool " + ai.getManaPool().totalMana()
                            + " of " + goal);
                    return tap;
                }
                continue;
            }
            int lands = 0;
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                lands += c.isLand() && !c.isTapped() && !c.getManaAbilities().isEmpty() ? 1 : 0;
            }
            if (ai.getManaPool().totalMana() + lands >= cost + RINGS && !RunawayLoop.exceeded(untap, LIMIT)) {
                untap.setActivatingPlayer(ai);
                untap.resetTargets();
                if (approve.test(untap)) {
                    VetoLog.note(ai, "rings-loop | " + host.getName() + " untaps, copied by the Rings");
                    return untap;
                }
            }
        }
        return null;
    }

    /**
     * With the AI's own untap of a Monolith on the stack, the Monolith untapped (the copy has resolved): the tap for mana,
     * before the original untap resolves.
     */
    public static SpellAbility response(final Player ai, final SpellAbility top, final List<SpellAbility> candidates,
                                        final Predicate<SpellAbility> approve) {
        // Time Sieve's extra turn on the stack, its five artifacts in the graveyard: Faith's Reward brings them back
        if (top != null && top.isActivatedAbility() && top.getApi() == ApiType.AddTurn && ai.equals(top.getActivatingPlayer())
                && ArtifactLoopPlan.rewardRefills(ai)) {
            final SpellAbility reward = LoopRunner.castFromHand(ai, candidates, ArtifactLoopPlan.REWARD);
            if (reward != null && forge.ai.ComputerUtilMana.canPayManaCost(reward, ai, 0, false)) {
                reward.setActivatingPlayer(ai);
                reward.resetTargets();
                if (approve.test(reward)) {
                    VetoLog.note(ai, "sieve-loop | Faith's Reward brings the Sieve and the artifacts back");
                    return reward;
                }
            }
        }
        final SpellAbility twin = twinResponse(ai, top, candidates, approve);
        if (twin != null) {
            return twin;
        }
        final SpellAbility naru = naruResponse(ai, top, candidates, approve);
        if (naru != null) {
            return naru;
        }
        if (top == null || !top.isActivatedAbility() || top.getApi() != ApiType.Untap || !ai.equals(top.getActivatingPlayer())
                || ringsOf(ai) == null) {
            return null;
        }
        final Card host = top.getHostCard();
        if (host == null || host.isTapped() || !ai.equals(host.getController()) || ManaLoopPlan.goal(ai) <= 0) {
            return null;
        }
        final SpellAbility tap = tapOf(candidates, host);
        if (tap == null || roundNet(ai, host, tap, top) <= 0 || RunawayLoop.exceeded(tap, LIMIT)) {
            return null;
        }
        tap.setActivatingPlayer(ai);
        if (!approve.test(tap)) {
            return null;
        }
        VetoLog.note(ai, "rings-loop | " + host.getName() + " taps again with its untap on the stack");
        return tap;
    }

    // ------------------------------------------------------------------------------------------------ Naru Meha

    private static int naruGoal(final Player ai) {
        return ManaLoopPlan.goal(ai, FLICKER + "|" + NARU);
    }

    /** The lands that tap for mana, the first by id first: the loop uses the first, and the Flicker is aimed at the first two. */
    private static List<Card> landsByAge(final Player ai) {
        final List<Card> lands = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && !c.getManaAbilities().isEmpty()) {
                lands.add(c);
            }
        }
        lands.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return lands;
    }

    /** Ghostly Flicker on two lands, with Naru Meha in the hand and the mana for both. */
    private static SpellAbility naruStart(final Player ai, final List<SpellAbility> candidates,
                                          final Predicate<SpellAbility> approve) {
        if (LoopRunner.inPlay(ai, NARU) != null || !LoopRunner.inHand(ai, NARU) || !LoopRunner.inHand(ai, FLICKER)
                || naruGoal(ai) <= 0 || ai.getManaPool().totalMana() >= naruGoal(ai)
                || forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai) < 7) {
            return null;
        }
        final List<Card> lands = landsByAge(ai);
        final SpellAbility flicker = LoopRunner.castFromHand(ai, candidates, FLICKER);
        if (flicker == null || lands.size() < 2) {
            return null;
        }
        flicker.setActivatingPlayer(ai);
        flicker.resetTargets();
        if (!flicker.canTarget(lands.get(0)) || !flicker.getTargets().add(lands.get(0))
                || !flicker.canTarget(lands.get(1)) || !flicker.getTargets().add(lands.get(1)) || !approve.test(flicker)) {
            flicker.resetTargets();
            return null;
        }
        VetoLog.note(ai, "naru-loop | Ghostly Flicker on two lands, Naru Meha to follow");
        return flicker;
    }

    /** The cast of Naru Meha with the Flicker on the stack, and the land tapped each time it is back. */
    private static SpellAbility naruResponse(final Player ai, final SpellAbility top, final List<SpellAbility> candidates,
                                             final Predicate<SpellAbility> approve) {
        if (top == null || !ai.equals(top.getActivatingPlayer())) {
            return null;
        }
        final Card naru = LoopRunner.inPlay(ai, NARU);
        if (naru == null) {
            final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, NARU);
            if (cast != null && top.isSpell() && top.getHostCard() != null && top.getHostCard().getName().equals(FLICKER)
                    && naruGoal(ai) > 0 && forge.ai.ComputerUtilMana.canPayManaCost(cast, ai, 0, false)) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                if (approve.test(cast)) {
                    VetoLog.note(ai, "naru-loop | Naru Meha with the Flicker on the stack");
                    return cast;
                }
            }
            return null;
        }
        if (top.getHostCard() != naru || !top.isTrigger() || ai.getManaPool().totalMana() >= naruGoal(ai)) {
            return null;
        }
        final List<Card> lands = landsByAge(ai);
        if (lands.isEmpty() || lands.get(0).isTapped()) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == lands.get(0) && sa.isManaAbility() && !RunawayLoop.exceeded(sa, 4 * NARU_ROUNDS)) {
                sa.setActivatingPlayer(ai);
                if (approve.test(sa)) {
                    VetoLog.note(ai, "naru-loop | the land taps with Naru Meha's trigger on the stack, pool "
                            + ai.getManaPool().totalMana() + " of " + naruGoal(ai));
                    return sa;
                }
            }
        }
        return null;
    }

    /** A spell of the AI's own that is bad to have twice: it sweeps the board, empties hands or takes the AI's own life. */
    public static boolean notWorthCopying(final SpellAbility spell) {
        for (SpellAbility sa = spell; sa != null; sa = sa.getSubAbility()) {
            final ApiType api = sa.getApi();
            if (api == ApiType.DestroyAll || api == ApiType.DamageAll || api == ApiType.ChangeZoneAll
                    || api == ApiType.SacrificeAll || api == ApiType.Discard && !sa.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The targets of a copy of Ghostly Flicker while Naru Meha is in play: Naru Meha and the land of the loop while the pool is
     * short of what the hand costs (and for {@value #NARU_ROUNDS} rounds at most), the two lands without her when it is not.
     *
     * @return true if the targets are set
     */
    public static boolean naruCopy(final Player ai, final SpellAbility copy) {
        if (twinCopy(ai, copy) || chainCopy(ai, copy)) {
            return true;
        }
        final Card host = copy == null ? null : copy.getHostCard();
        final Card naru = LoopRunner.inPlay(ai, NARU);
        if (host == null || !host.getName().equals(FLICKER) || naru == null || !copy.usesTargeting()) {
            return false;
        }
        final List<Card> lands = landsByAge(ai);
        if (lands.size() < 2) {
            return false;
        }
        final int rounds = naru.hasSVar("NaruRounds") ? Integer.parseInt(naru.getSVar("NaruRounds")) : 0;
        final boolean go = naruGoal(ai) > 0 && ai.getManaPool().totalMana() < naruGoal(ai) && rounds < NARU_ROUNDS;
        final Card[] picks = go ? new Card[] {naru, lands.get(0)} : new Card[] {lands.get(0), lands.get(1)};
        copy.setActivatingPlayer(ai);
        for (SpellAbility s = copy; s != null; s = s.getSubAbility()) {
            s.clearTargets();
        }
        for (final Card c : picks) {
            if (!copy.canTarget(c) || !copy.getTargets().add(c)) {
                copy.clearTargets();
                return false;
            }
        }
        naru.setSVar("NaruRounds", String.valueOf(rounds + 1));
        return true;
    }

    // ------------------------------------------------------------------------------------------------ Dualcaster Mage

    static final String DUALCASTER = "Dualcaster Mage";
    static final String TWINFLAME = "Twinflame";
    private static final int DUAL_TOKENS = 40;

    private static int redSources(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() || c.isTapped()) {
                continue;
            }
            for (final SpellAbility ma : c.getManaAbilities()) {
                if (ma.canProduce("R")) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** The hasty tokens of the Mage that are in play, and the power that they are enough for. */
    private static int dualcasterPower(final Player ai) {
        int power = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isToken() && DUALCASTER.equals(c.getName())) {
                power += Math.max(0, c.getNetPower());
            }
        }
        return power;
    }

    private static int dualcasterTokens(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            n += c.isToken() && DUALCASTER.equals(c.getName()) ? 1 : 0;
        }
        return n;
    }

    /** The power that kills: the opponent's life, two for every creature that may block, and two more. */
    private static int dualcasterNeed(final Player ai) {
        final Player opp = firstOpponentOf(ai);
        if (opp == null) {
            return 0;
        }
        int blockers = 0;
        for (final Card c : opp.getCreaturesInPlay()) {
            blockers += c.isUntapped() ? 1 : 0;
        }
        return opp.getLife() + 2 * blockers + 2;
    }

    private static Player firstOpponentOf(final Player ai) {
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLife() > 0) {
                return opp;
            }
        }
        return null;
    }

    /**
     * Dualcaster Mage with Twinflame: Twinflame is cast for no creature, the Mage comes in with flash in answer and copies it
     * for itself, the token that it makes copies it again, and so on until the tokens are enough to kill.
     */
    private static SpellAbility twinStart(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (!LoopRunner.inHand(ai, TWINFLAME) || !LoopRunner.inHand(ai, DUALCASTER) || LoopRunner.inPlay(ai, DUALCASTER) != null
                || dualcasterNeed(ai) <= 0 || forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai) < 5 || redSources(ai) < 3) {
            return null;
        }
        final SpellAbility twin = LoopRunner.castFromHand(ai, candidates, TWINFLAME);
        if (twin == null) {
            return null;
        }
        twin.setActivatingPlayer(ai);
        twin.resetTargets();
        if (!approve.test(twin)) {
            return null;
        }
        VetoLog.note(ai, "twinflame-loop | Twinflame for no creature, the Mage to follow");
        return twin;
    }

    private static SpellAbility twinResponse(final Player ai, final SpellAbility top, final List<SpellAbility> candidates,
                                             final Predicate<SpellAbility> approve) {
        if (top == null || !top.isSpell() || top.getHostCard() == null || !TWINFLAME.equals(top.getHostCard().getName())
                || !ai.equals(top.getActivatingPlayer()) || top.getTargets().size() > 0 || LoopRunner.inPlay(ai, DUALCASTER) != null) {
            return null;
        }
        final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, DUALCASTER);
        if (cast == null || dualcasterNeed(ai) <= 0 || !forge.ai.ComputerUtilMana.canPayManaCost(cast, ai, 0, false)) {
            return null;
        }
        cast.setActivatingPlayer(ai);
        cast.resetTargets();
        if (!approve.test(cast)) {
            return null;
        }
        VetoLog.note(ai, "twinflame-loop | the Mage in answer to Twinflame");
        return cast;
    }

    /** The target of a copy of Twinflame: the Mage while the tokens do not kill, none once they do. */
    private static boolean twinCopy(final Player ai, final SpellAbility copy) {
        final Card host = copy == null ? null : copy.getHostCard();
        if (host == null || !TWINFLAME.equals(host.getName()) || !copy.usesTargeting()) {
            return false;
        }
        Card mage = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (DUALCASTER.equals(c.getName()) && (mage == null || mage.isToken() && !c.isToken())) {
                mage = c;
            }
        }
        if (mage == null) {
            return false;
        }
        copy.setActivatingPlayer(ai);
        for (SpellAbility s = copy; s != null; s = s.getSubAbility()) {
            s.clearTargets();
        }
        final boolean go = dualcasterPower(ai) < dualcasterNeed(ai) && dualcasterTokens(ai) < DUAL_TOKENS;
        if (go && (!copy.canTarget(mage) || !copy.getTargets().add(mage))) {
            copy.clearTargets();
        }
        VetoLog.note(ai, "twinflame-loop | " + dualcasterTokens(ai) + " tokens, power " + dualcasterPower(ai) + " of "
                + dualcasterNeed(ai) + (go ? ", one more" : ", the chain ends"));
        return true;
    }

    // ------------------------------------------------------------------------------------------------ Chain of Smog

    static final String WITHERBLOOM = "Witherbloom Apprentice";
    static final String CHAIN = "Chain of Smog";

    /**
     * Witherbloom Apprentice with Chain of Smog: the AI aims the Chain at itself with nothing else in hand, so nothing is
     * discarded, and copies it for itself again and again. Every copy is a spell copied, and Witherbloom drains one for it.
     */
    public static boolean chainLoop(final Player ai) {
        if (LoopRunner.inPlay(ai, WITHERBLOOM) == null || firstOpponentOf(ai) == null) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!CHAIN.equals(c.getName())) {
                return false;
            }
        }
        return true;
    }

    private static SpellAbility chainStart(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (!LoopRunner.inHand(ai, CHAIN) || !chainLoop(ai)) {
            return null;
        }
        final SpellAbility chain = LoopRunner.castFromHand(ai, candidates, CHAIN);
        if (chain == null) {
            return null;
        }
        chain.setActivatingPlayer(ai);
        chain.resetTargets();
        if (!chain.canTarget(ai) || !chain.getTargets().add(ai) || !approve.test(chain)) {
            chain.resetTargets();
            return null;
        }
        VetoLog.note(ai, "chain-loop | the Chain at itself with an empty hand, opponent at " + firstOpponentOf(ai).getLife());
        return chain;
    }

    /** The target of a copy of the Chain: the AI itself, while the opponent still lives. */
    private static boolean chainCopy(final Player ai, final SpellAbility copy) {
        final Card host = copy == null ? null : copy.getHostCard();
        if (host == null || !CHAIN.equals(host.getName()) || !copy.usesTargeting() || !chainLoop(ai)) {
            return false;
        }
        copy.setActivatingPlayer(ai);
        for (SpellAbility sa = copy; sa != null; sa = sa.getSubAbility()) {
            sa.clearTargets();
        }
        if (!copy.canTarget(ai) || !copy.getTargets().add(ai)) {
            copy.clearTargets();
            return false;
        }
        return true;
    }

    /** Whether the Scepter's copy of its imprinted Dramatic Reversal is a loop's (the AI wants mana the rocks can make). */
    public static boolean scepterCopiesTheReversal(final Player ai, final SpellAbility sa, final Card option) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && host.getName().equals("Isochron Scepter") && option != null
                && option.getName().equals("Dramatic Reversal") && ai.equals(host.getController()) && ManaLoopPlan.goal(ai) > 0;
    }
}

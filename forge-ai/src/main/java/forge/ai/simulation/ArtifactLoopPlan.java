package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostTap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Loops of the artifact decks, one step at a time with an empty stack:
 * <ul>
 * <li><b>Sensei's Divining Top with Bolas's Citadel and Aetherflux Reservoir.</b> Top draws and goes on top of the library,
 * the Citadel lets it be cast from there for one life, and the Reservoir gains a life for each spell cast this turn: a life
 * less one each round, then two, then three. Fifty life later the Reservoir pays them for fifty damage.</li>
 * <li><b>Isochron Scepter with Dramatic Reversal (and mana rocks).</b> With the Reversal imprinted, every rock is tapped,
 * the Scepter pays {2} and taps, and the copy of the Reversal untaps the rocks and the Scepter: the rocks' mana less two a
 * round.</li>
 * <li><b>Time Sieve with Thopter Foundry and Sword of the Meek.</b> {1} and the Sword make a Thopter and the Sword comes
 * back on it: an artifact for each {1}, until Time Sieve has its five for an extra turn.</li>
 * </ul>
 * Faith's Reward after the Sieve's ability brings the Sieve and the artifacts back for another: {@link StackLoops#response}.
 */
final class ArtifactLoopPlan {
    private static final int LIMIT = 120;
    static final String SIEVE = "Time Sieve";
    static final String REWARD = "Faith's Reward";

    private ArtifactLoopPlan() {
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        final SpellAbility top = top(ai, candidates, approve);
        if (top != null) {
            return top;
        }
        final SpellAbility scepter = scepter(ai, candidates, approve);
        if (scepter != null) {
            return scepter;
        }
        final SpellAbility metal = metalworker(ai, candidates, approve);
        if (metal != null) {
            return metal;
        }
        final SpellAbility pili = piliPala(ai, candidates, approve);
        return pili != null ? pili : sieve(ai, candidates, approve);
    }

    // ------------------------------------------------------------------------------------------------ Metalworker

    private static int artifactsInHand(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            n += c.isArtifact() ? 1 : 0;
        }
        return n;
    }

    /**
     * Metalworker with Staff of Domination: the Metalworker reveals the artifacts in hand for two mana each, the Staff
     * untaps it for three and untaps itself for one, so a round pays 2N - 4 and wants three artifacts in hand.
     */
    private static SpellAbility metalworker(final Player ai, final List<SpellAbility> candidates,
                                            final Predicate<SpellAbility> approve) {
        final Card metal = LoopRunner.inPlay(ai, "Metalworker");
        final Card staff = LoopRunner.inPlay(ai, "Staff of Domination");
        final int goal = ManaLoopPlan.goal(ai);
        if (metal == null || staff == null || goal <= 0 || ai.getManaPool().totalMana() >= goal || artifactsInHand(ai) < 3) {
            return null;
        }
        if (!metal.isTapped()) {
            final SpellAbility reveal = LoopRunner.activated(candidates, metal, ApiType.Reveal);
            if (reveal != null && !RunawayLoop.exceeded(reveal, LIMIT)) {
                reveal.setActivatingPlayer(ai);
                reveal.resetTargets();
                if (approve.test(reveal)) {
                    VetoLog.note(ai, "metalworker-loop | reveals " + artifactsInHand(ai) + " artifacts, pool " + ai.getManaPool().totalMana()
                            + " of " + goal);
                    return reveal;
                }
            }
            return null;
        }
        for (final SpellAbility sa : candidates) {
            if (sa == null || sa.getHostCard() != staff || !sa.isActivatedAbility() || sa.getApi() != ApiType.Untap
                    || RunawayLoop.exceeded(sa, LIMIT)) {
                continue;
            }
            final boolean targeted = sa.usesTargeting();
            if (staff.isTapped() == targeted) {
                continue; // tapped: the one-mana self untap; untapped: the three-mana untap aimed at the Metalworker
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (targeted) {
                if (!sa.canTarget(metal)) {
                    continue;
                }
                sa.getTargets().add(metal);
            }
            if (approve.test(sa)) {
                VetoLog.note(ai, "metalworker-loop | the Staff untaps " + (targeted ? "the Metalworker" : "itself"));
                return sa;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ Pili-Pala

    /**
     * Pili-Pala with Grand Architect: the Architect makes Pili-Pala blue, tapping it pays the Architect's two colourless
     * for artifact abilities, and Pili-Pala's {2}{Q} untaps it for one mana of any colour: one more mana a round.
     */
    private static SpellAbility piliPala(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final Card pili = LoopRunner.inPlay(ai, "Pili-Pala");
        final Card architect = LoopRunner.inPlay(ai, "Grand Architect");
        final int goal = ManaLoopPlan.goal(ai);
        if (pili == null || architect == null || goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        if (!pili.getColor().hasBlue()) {
            final SpellAbility paint = LoopRunner.activated(candidates, architect, ApiType.Animate);
            if (paint == null || RunawayLoop.exceeded(paint, LIMIT)) {
                return null;
            }
            paint.setActivatingPlayer(ai);
            paint.resetTargets();
            if (!paint.canTarget(pili)) {
                return null;
            }
            paint.getTargets().add(pili);
            if (approve.test(paint)) {
                VetoLog.note(ai, "pili-loop | the Architect makes Pili-Pala blue");
                return paint;
            }
            return null;
        }
        SpellAbility pay = null;
        SpellAbility fromArchitect = null;
        for (final SpellAbility sa : candidates) {
            if (sa == null || !sa.isManaAbility() || sa.getApi() != ApiType.Mana || RunawayLoop.exceeded(sa, LIMIT)) {
                continue;
            }
            if (sa.getHostCard() == pili) {
                pay = sa;
            } else if (sa.getHostCard() == architect) {
                fromArchitect = sa;
            }
        }
        if (pili.isTapped() && pay != null && ai.getManaPool().totalMana() >= 2) {
            pay.setActivatingPlayer(ai);
            if (approve.test(pay)) {
                VetoLog.note(ai, "pili-loop | Pili-Pala untaps for a mana, pool " + ai.getManaPool().totalMana() + " of " + goal);
                return pay;
            }
        }
        if (fromArchitect != null && !(pili.isTapped() && architect.isTapped())) {
            fromArchitect.setActivatingPlayer(ai);
            if (approve.test(fromArchitect)) {
                VetoLog.note(ai, "pili-loop | a blue creature taps for the Architect's two");
                return fromArchitect;
            }
        }
        return null;
    }

    private static Player firstOpponent(final Player ai) {
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLife() > 0) {
                return opp;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ Top

    private static SpellAbility top(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final Card reservoir = LoopRunner.inPlay(ai, "Aetherflux Reservoir");
        if (reservoir == null || LoopRunner.inPlay(ai, "Bolas's Citadel") == null) {
            return null;
        }
        final Player opp = firstOpponent(ai);
        if (opp == null) {
            return null;
        }
        if (ai.getLife() > 55 || ai.getLife() > 50 && opp.getLife() <= 50) {
            final SpellAbility shot = LoopRunner.aimed(ai, LoopRunner.activated(candidates, reservoir, ApiType.DealDamage), opp,
                    approve, LIMIT);
            if (shot != null) {
                VetoLog.note(ai, "top-loop | the Reservoir for fifty, life " + ai.getLife() + " against " + opp.getLife());
                return shot;
            }
        }
        final CardCollection library = new CardCollection(ai.getCardsIn(ZoneType.Library));
        if (library.size() <= VaultLoop.LIBRARY_FLOOR || ai.getLife() < 4) {
            return null;
        }
        final Card onTop = library.getFirst();
        if ("Sensei's Divining Top".equals(onTop.getName())) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && onTop.equals(sa.getHostCard()) && sa.isSpell() && !RunawayLoop.exceeded(sa, LIMIT)) {
                    sa.setActivatingPlayer(ai);
                    sa.resetTargets();
                    if (approve.test(sa)) {
                        VetoLog.note(ai, "top-loop | Top is cast from the library, life " + ai.getLife());
                        return sa;
                    }
                }
            }
            return null;
        }
        final Card top = LoopRunner.inPlay(ai, "Sensei's Divining Top");
        if (top != null && !top.isTapped()) {
            final SpellAbility draw = LoopRunner.activated(candidates, top, ApiType.Draw);
            if (draw != null && !RunawayLoop.exceeded(draw, LIMIT)) {
                draw.setActivatingPlayer(ai);
                draw.resetTargets();
                if (approve.test(draw)) {
                    VetoLog.note(ai, "top-loop | Top draws and goes back on the library");
                    return draw;
                }
            }
            return null;
        }
        if (top == null) {
            final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, "Sensei's Divining Top");
            if (cast != null && approve.test(cast)) {
                return cast;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ Scepter

    private static SpellAbility tapOf(final List<SpellAbility> candidates, final Card host) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isManaAbility() && sa.getApi() == ApiType.Mana
                    && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostTap.class)
                    && sa.getPayCosts().getCostMana() == null && sa.getPayCosts().getTotalMana().getCMC() == 0) {
                return sa;
            }
        }
        return null;
    }

    private static SpellAbility scepter(final Player ai, final List<SpellAbility> candidates,
                                        final Predicate<SpellAbility> approve) {
        final Card scepter = LoopRunner.inPlay(ai, "Isochron Scepter");
        final int goal = ManaLoopPlan.goal(ai);
        if (scepter == null || goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        boolean imprinted = false;
        for (final Card c : scepter.getImprintedCards()) {
            imprinted |= "Dramatic Reversal".equals(c.getName());
        }
        final SpellAbility play = LoopRunner.activated(candidates, scepter, ApiType.Play);
        if (!imprinted || play == null) {
            return null;
        }
        // what the rocks make in a round (the Reversal untaps every nonland permanent: the Scepter's own cost is two)
        int made = 0;
        SpellAbility untappedRock = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() || c == scepter || c.isCreature() && c.isSick()) {
                continue;
            }
            final SpellAbility tap = tapOf(candidates, c);
            if (tap == null) {
                continue;
            }
            made += ManaLoopPlan.produced(ai, c, tap);
            if (!c.isTapped() && untappedRock == null) {
                tap.setActivatingPlayer(ai);
                if (approve.test(tap)) {
                    untappedRock = tap;
                }
            }
        }
        if (made - 2 <= 0) {
            return null;
        }
        if (untappedRock != null) {
            VetoLog.note(ai, "scepter-loop | " + untappedRock.getHostCard().getName() + " taps, pool " + ai.getManaPool().totalMana()
                    + " of " + goal);
            return untappedRock;
        }
        if (scepter.isTapped() || ai.getManaPool().totalMana() < 2 || RunawayLoop.exceeded(play, LIMIT)) {
            return null;
        }
        play.setActivatingPlayer(ai);
        play.resetTargets();
        if (!approve.test(play)) {
            return null;
        }
        VetoLog.note(ai, "scepter-loop | the Scepter copies the Reversal");
        return play;
    }

    // ------------------------------------------------------------------------------------------------ Time Sieve

    static int artifacts(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            n += c.isArtifact() ? 1 : 0;
        }
        return n;
    }

    /** The marks of the sacrifice the AI chooses by: the Thopters and the Sword first, any artifact for the Reward. */
    private static void markArtifacts(final Player ai, final boolean any) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            final boolean mark = c.isArtifact() && (any || c.isToken() || "Sword of the Meek".equals(c.getName()));
            if (mark) {
                SacMarks.mark(c);
            }
        }
    }

    private static SpellAbility sieve(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final Card sieve = LoopRunner.inPlay(ai, SIEVE);
        if (sieve == null || sieve.isTapped()) {
            return null;
        }
        final int artifacts = artifacts(ai);
        if (artifacts >= 5) {
            final SpellAbility turn = LoopRunner.activated(candidates, sieve, ApiType.AddTurn);
            if (turn == null || RunawayLoop.exceeded(turn, 4)) {
                return null;
            }
            markArtifacts(ai, LoopRunner.inHand(ai, REWARD));
            turn.setActivatingPlayer(ai);
            turn.resetTargets();
            if (!approve.test(turn)) {
                return null;
            }
            VetoLog.note(ai, "sieve-loop | Time Sieve for an extra turn, " + artifacts + " artifacts");
            return turn;
        }
        final Card foundry = LoopRunner.inPlay(ai, "Thopter Foundry");
        final Card sword = LoopRunner.inPlay(ai, "Sword of the Meek");
        if (foundry == null || sword == null || forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai) < 5 - artifacts) {
            return null;
        }
        final SpellAbility make = LoopRunner.activated(candidates, foundry, ApiType.Token);
        if (make == null || RunawayLoop.exceeded(make, LIMIT)) {
            return null;
        }
        SacMarks.mark(sword);
        make.setActivatingPlayer(ai);
        make.resetTargets();
        if (!approve.test(make)) {
            return null;
        }
        VetoLog.note(ai, "sieve-loop | Foundry on the Sword, " + artifacts + " artifacts of 5");
        return make;
    }

    /** Whether Faith's Reward brings the Sieve and four other artifacts back: five nontoken artifacts in the graveyard. */
    static boolean rewardRefills(final Player ai) {
        int n = 0;
        boolean sieve = false;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (c.isArtifact() && !c.isToken()) {
                n++;
                sieve |= SIEVE.equals(c.getName());
            }
        }
        return sieve && n >= 5;
    }
}

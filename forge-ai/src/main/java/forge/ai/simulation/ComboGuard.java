package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.ai.ComputerUtil;
import forge.ai.NamedDiscard;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.combat.CombatUtil;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Plays that Flat Monte Carlo must not spend a card on, whatever the rollouts think of them. Three kinds:
 * <ul>
 *   <li><b>The legend rule</b>: casting, reanimating (Reanimate, Animate Dead, Exhume, Goryo's Vengeance, ...) or cheating
 *       in a legendary permanent the AI already has in play and that still does its job. One of the two copies would go
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
        // A Pact first and on its own: another guard failing (a card with an odd mana ability, an unreadable script) must
        // not let a Pact through that cannot be paid, which loses the game at the next upkeep. If it cannot be told,
        // the Pact waits.
        try {
            if (pactUnpayable(ai, sa, plan)) {
                return "pact-unpayable";
            }
        } catch (final RuntimeException e) {
            if (isPact(sa)) {
                return "pact-unpayable";
            }
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
        // A second working copy of a legendary permanent is wasted even when it is cast directly (not reanimated).
        // Respect cards with an explicit AI legendary exception; Mirror Gallery is handled by duplicatesWorkingLegend.
        if (sa.isSpell() && host.isPermanent() && !host.ignoreLegendRule() && !host.hasSVar("AILegendaryException")
                && duplicatesWorkingLegend(ai, host)) {
            return "duplicate-legend";
        }
        // Balustrade Spy and Undercity Informer milling their own controller without the Oracle to follow it
        if (plan != null && plan.has(ComboPlan.Kind.LIBRARY_OUT)
                && AiCardLists.has(AiCardLists.Role.SELF_MILL_OUTLET, host.getName())) {
            // The cast of the creature has no target: its trigger picks one when it enters. What is refused is therefore
            // the cast itself, with the creature counted among those the flashback of Dread Return can sacrifice, and
            // an Oracle in hand only counts when the mana for it is there after the Spy (the library is then empty for
            // good: the next draw would lose the game).
            if (sa.isSpell() && host.isCreature()
                    && (!plan.libraryOutSelfMillSafe(ai, 1) || !LibraryOutWin.castable(ai, plan, sa, 1))) {
                return "self-mill-no-winner";
            }
            if (!plan.libraryOutSelfMillSafe(ai)) {
                boolean self = ai.equals(aim);
                for (SpellAbility a = sa; a != null && !self; a = a.getSubAbility()) {
                    self = a.usesTargeting() && a.getTargets() != null && a.getTargets().contains(ai);
                }
                if (self) {
                    return "self-mill-no-winner";
                }
            }
        }
        // Breakthrough without a dredger, a card for the graveyard and Lion's Eye Diamond in play only empties the hand.
        // Not left to AiController.canPlaySa: it answers AnotherTime, and a fuel play (a sorcery that discards) with that
        // answer is still offered to the rollouts as a discovered play, which the rollouts like (the graveyard fills).
        if (plan != null && DredgeLine.mustWait(ai, plan, sa)) {
            return "dredge-line-incomplete";
        }
        // Griselbrand: pay life, draw cards. Offered to the rollouts as a discovered play whatever the heuristics think,
        // and the rollouts like the cards; the guard stops it when the life or the library cannot take it.
        if (LifeDrawGuard.reckless(ai, sa, plan)) {
            return "life-draw-reckless";
        }
        // Void Mirror and Chalice of the Void (anyone's) counter spells: a free spell is countered by either of them
        final String walled = counterWall(ai, sa, host);
        if (walled != null) {
            return walled;
        }
        // a creature land (Mutavault), a crew, a saddle or a station at a moment it cannot do anything: only wastes the mana
        // and the creatures tapped for it (see TimedAbilityGuard)
        final String untimely = TimedAbilityGuard.problem(ai, sa);
        if (untimely != null) {
            return untimely;
        }
        // an activated ability with nothing to act on (Oran-Rief with no new green creature): see ActivationGuard
        final String idle = ActivationGuard.problem(ai, sa);
        // "valuable-sacrifice" is the outlet guard's own reading of the evaluator: the combo guard has the sacrifice rules of
        // the deck plan (sacrifice-key), and a deck without a combo plan sacrifices as the heuristics decide
        if (idle != null && !"valuable-sacrifice".equals(idle)) {
            return idle;
        }
        // Demonic Consultation exiles the library: only to win, with the Oracle's trigger on the stack (ConsultationWin)
        if (ConsultationWin.isConsultation(sa) && !ConsultationWin.winnerOnStack(ai.getGame(), ai)) {
            return "consultation-no-winner";
        }
        // Doomsday without the Oracle in hand (or the life, or a library to cut) only throws the deck away
        if (sa.isSpell() && DoomsdayPile.isDoomsday(host) && !DoomsdayPile.ready(ai)) {
            return "doomsday-not-ready";
        }
        // with a winning Doomsday in hand, a creature cast or a cycling first only spends what Doomsday needs
        if (DoomsdayPile.mustWaitForDoomsday(ai, sa)) {
            return "doomsday-first";
        }
        // Illusions of Grandeur without the Donate that makes it a combo, Donate with nothing to give
        final String donate = DonateCombo.problem(ai, sa);
        if (donate != null) {
            return donate;
        }
        // Doomsday Excruciator, or Superior Spider-Man with nothing else to copy: the cut loses without a finisher
        if (cutsTheLibraries(ai, sa, plan)) {
            return "library-cut-unsafe";
        }
        // Aang's waterbend transform taps the loop's Ally tokens and ends the airbend loop: not while it is running
        if (endsAirbendLoop(ai, sa, plan)) {
            return "airbend-loop-transform";
        }
        // a Pact: the cost at the next upkeep must be payable, or the game is lost, unless this turn wins the game
        if (pactUnpayable(ai, sa, plan)) {
            return "pact-unpayable";
        }
        // Cabal Therapy aimed at the AI itself, with nothing of its own worth naming, discards nothing
        if (isNamedDiscard(sa)) {
            final Player target = aim instanceof Player ? (Player) aim : NamedDiscard.discardTarget(sa);
            if (target == ai && plan != null && !plan.selfDiscardWanted(ai, host)) {
                return "named-discard-self";
            }
        }
        // a sacrifice cost must not throw away a piece of the deck's combos (Wight of the Reliquary, a flashback)
        if (sacrificesKeyCard(ai, sa, plan)) {
            return "sacrifice-key";
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

    /**
     * Whether paying the sacrifice cost of the play (Wight of the Reliquary, the flashback of Cabal Therapy) would
     * give up a piece of the combos of the deck or one of its win cards, going by the creature the AI picks for such
     * a cost. A deck without a combo plan and a cost that sacrifices the card itself are never a problem.
     */
    /**
     * A land that animates itself until end of turn (Mutavault, Stirring Wildwood) at a moment it is worth nothing:
     * see {@link TimedAbilityGuard}, which also covers Vehicles, Mounts and Spacecraft.
     */
    static boolean wastedManland(final Player ai, final SpellAbility sa, final Card host) {
        return sa != null && host != null && host.isLand() && "manland-wasted".equals(TimedAbilityGuard.problem(ai, sa));
    }

    /** Whether the spell is a Pact, also when its script cannot be read any further. */
    static boolean isPact(final SpellAbility sa) {
        try {
            return pactCost(sa) != null;
        } catch (final RuntimeException e) {
            final Card host = sa == null ? null : sa.getHostCard();
            return host != null && host.getName() != null && host.getName().endsWith(" Pact") || host != null
                    && host.getName() != null && host.getName().startsWith("Pact of ");
        }
    }

    private static final Pattern PACT_COST = Pattern.compile("UnlessCost\\$\\s*([^|]+)");

    /**
     * The mana cost of the "at the beginning of your next upkeep, pay {cost}, if you don't, you lose the game" part
     * of a Pact (Pact of Negation, Summoner's Pact, Slaughter Pact, Intervention Pact, Pact of the Titan), written as
     * in the card script ("3 U U"), or null for any other spell.
     */
    public static String pactCost(final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null) {
            return null;
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() != ApiType.DelayedTrigger || !"Upkeep".equals(a.getParam("Phase"))) {
                continue;
            }
            final String exec = a.getParam("Execute");
            final String svar = exec == null ? "" : host.getSVar(exec);
            if (svar.contains("LosesGame")) {
                final Matcher m = PACT_COST.matcher(svar);
                if (m.find()) {
                    return m.group(1).trim();
                }
            }
        }
        return null;
    }

    /**
     * Whether casting the Pact would lose the game: the AI would not be able to pay the cost at its next upkeep (with
     * the costs of the Pacts it already cast this turn), and this turn is not the one that wins. The mana counted is
     * what its permanents make again after they untap (not Lotus Petal or Lion's Eye Diamond), and a land that can
     * still be played this turn.
     */
    static boolean pactUnpayable(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        final String cost = pactCost(sa);
        if (cost == null) {
            return false;
        }
        final int[] colors = new int[5];
        int owed = addPactCost(cost, colors);
        for (final SpellAbility cast : ai.getGame().getStack().getSpellsCastThisTurn()) {
            if (ai.equals(cast.getActivatingPlayer()) && cast != sa && pactCost(cast) != null) {
                owed += addPactCost(pactCost(cast), colors);
            }
        }
        int total = 0;
        final int[] canMake = new int[5];
        final List<Card> sources = toList(ai.getCardsIn(ZoneType.Battlefield));
        final boolean ownTurn = ai.getGame().getPhaseHandler().getPlayerTurn() == ai;
        if (ownTurn && ai.getLandsPlayedThisTurn() < ai.getMaxLandPlays()) {
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (c.isLand()) {
                    sources.add(c); // the land drop still to come
                    break;
                }
            }
        }
        for (final Card c : sources) {
            int best = 0;
            final boolean[] makes = new boolean[5];
            for (final SpellAbility ma : c.getManaAbilities()) {
                if (ma.getPayCosts() != null && (ma.getPayCosts().hasSpecificCostType(CostSacrifice.class)
                        || ma.getPayCosts().hasSpecificCostType(CostDiscard.class))) {
                    continue; // used up by making the mana
                }
                ManaConversion.withActivator(ma); // asking what it makes checks its conditions, which want an activator
                try {
                    best = Math.max(best, Math.max(1, ma.amountOfManaGenerated(false)));
                    for (int i = 0; i < 5; i++) {
                        makes[i] |= ma.canProduce(String.valueOf("WUBRG".charAt(i)));
                    }
                } catch (final RuntimeException e) {
                    best = Math.max(best, 1); // an odd mana ability (Chrome Mox, Gemstone Mine): one mana, of no color we can name
                }
            }
            total += best;
            for (int i = 0; i < 5; i++) {
                if (makes[i] && best > 0) {
                    canMake[i]++;
                }
            }
        }
        boolean payable = total >= owed;
        for (int i = 0; i < 5 && payable; i++) {
            payable = canMake[i] >= colors[i];
        }
        if (payable) {
            return false;
        }
        return !winsThisTurn(ai, plan);
    }

    private static List<Card> toList(final CardCollectionView cards) {
        final List<Card> list = new ArrayList<>();
        for (final Card c : cards) {
            list.add(c);
        }
        return list;
    }

    /** Adds one Pact cost ("2 G G") to the colored counts (WUBRG) and returns its total mana. */
    private static int addPactCost(final String cost, final int[] colors) {
        int total = 0;
        for (final String token : cost.trim().split("\\s+")) {
            final int at = "WUBRG".indexOf(token);
            if (at >= 0 && token.length() == 1) {
                colors[at]++;
                total++;
            } else if (token.matches("\\d+")) {
                total += Integer.parseInt(token);
            } else if (!token.isEmpty()) {
                total++;
            }
        }
        return total;
    }

    /**
     * Whether the game is won this turn, so that a Pact that cannot be paid does not matter: a lethal attack is
     * still to come, or the library of a library-out deck (Oops! All Spells) is nearly gone on its own turn.
     */
    static boolean winsThisTurn(final Player ai, final ComboPlan plan) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.getPlayerTurn() != ai) {
            return false;
        }
        if (plan != null && plan.has(ComboPlan.Kind.LIBRARY_OUT)
                && ai.getCardsIn(ZoneType.Library).size() <= PACT_LIBRARY_OUT_LEFT) {
            return true;
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || ai.getOpponents().size() != 1 || !ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }
        final List<Card> attackers = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && CombatUtil.canAttack(c, opp)) {
                attackers.add(c);
            }
        }
        final List<Card> blockers = new ArrayList<>();
        for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && !c.isTapped()) {
                blockers.add(c);
            }
        }
        return opp.getLife() > 0 && unblockedDamage(attackers, blockers) >= opp.getLife();
    }

    /** With this few cards left in the library the library-out deck is taken to win the turn it is cast in. */
    private static final int PACT_LIBRARY_OUT_LEFT = 3;

    /**
     * Whether the spell would cut both libraries (Doomsday Excruciator cast, or Superior Spider-Man whose only creature
     * to copy in the graveyards is such a card) while the AI has nothing to empty the opponent's library faster.
     */
    static boolean cutsTheLibraries(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        final Card host = sa.getHostCard();
        if (plan == null || !plan.has(ComboPlan.Kind.LIBRARY_CUT) || host == null || !sa.isSpell() || host.isLand()) {
            return false;
        }
        if (plan.libraryCutSafe(ai)) {
            return false;
        }
        if (AiCardLists.has(AiCardLists.Role.LIBRARY_CUT, host.getName())) {
            return true;
        }
        if (!host.isCreature() || !AiCardLists.has(AiCardLists.Role.REANIMATION_SPELL, host.getName())) {
            return false;
        }
        boolean cut = false;
        for (final Player p : ai.getGame().getPlayers()) {
            for (final Card c : p.getCardsIn(ZoneType.Graveyard)) {
                if (!c.isCreature()) {
                    continue;
                }
                if (!AiCardLists.has(AiCardLists.Role.LIBRARY_CUT, c.getName())) {
                    return false; // something else to copy: the choice avoids the cut
                }
                cut = true;
            }
        }
        return cut;
    }

    /**
     * Whether the ability transforms a creature of the airbend loop (Aang, Swift Savior: Waterbend {8}) while the loop
     * still has a creature to airbend and has not reached its cap. The transformed side airbends nothing, so the loop
     * would end there (and the Ally tokens that paid for it are tapped). After the cap, or without Doc Aurlock, it is fine.
     */
    static boolean endsAirbendLoop(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        final Card host = sa.getHostCard();
        if (plan == null || host == null || sa.isSpell() || !ApiType.SetState.equals(sa.getApi())
                || !"Transform".equals(sa.getParam("Mode"))) {
            return false;
        }
        return plan.airbendLoopLive(ai, host);
    }

    private static boolean isNamedDiscard(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (NamedDiscard.LOGIC.equals(a.getParam("AILogic"))) {
                return true;
            }
        }
        return false;
    }

    static boolean sacrificesKeyCard(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        if (plan == null || plan.isEmpty() || sa.getPayCosts() == null
                || !sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
            return false;
        }
        final CostSacrifice cost = sa.getPayCosts().getCostPartByType(CostSacrifice.class);
        if (cost == null || cost.payCostFromSource()) {
            return false;
        }
        final CardCollectionView chosen = ComputerUtil.chooseSacrificeType(ai, cost.getType(), sa, sa.getTargetCard(),
                false, Math.max(1, cost.getAbilityAmount(sa)), CardCollection.EMPTY);
        if (chosen == null) {
            return false;
        }
        for (final Card c : chosen) {
            if (plan.isKeyCard(c) || plan.isWinCard(c)) {
                return true;
            }
        }
        return false;
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
        final boolean combatToCome = ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS);
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

    /**
     * The spell is countered by a permanent in play: Void Mirror counters any spell cast without colored mana, so a spell
     * that costs nothing always is (the first playtest cast Black Lotus and Mox Emerald into its own); Chalice of the Void
     * counters the spells whose mana value equals its charge counters, with none on it that is every free spell.
     *
     * @return the refusal code, or null
     */
    static String counterWall(final Player ai, final SpellAbility sa, final Card host) {
        if (!sa.isSpell() || host.isLand() || sa.getPayCosts() == null) {
            return null;
        }
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if ("Chalice of the Void".equals(c.getName()) && host.getCMC() == c.getCounters(forge.game.card.CounterEnumType.CHARGE)) {
                return "chalice-counters-spell";
            }
        }
        if (sa.getPayCosts().getTotalMana() != null && sa.getPayCosts().getTotalMana().isZero()
                && !sa.getPayCosts().hasXInAnyCostPart() && coloredManaCountersSpells(ai)) {
            return "void-mirror-counters-free-spell";
        }
        return null;
    }

    /** Whether a permanent in play counters spells cast without colored mana (Void Mirror). */
    static boolean coloredManaCountersSpells(final Player ai) {
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final forge.game.trigger.Trigger t : c.getTriggers()) {
                if ("True".equals(t.getParam("NoColoredMana")) && forge.game.trigger.TriggerType.SpellCast.equals(t.getMode())) {
                    return true;
                }
            }
        }
        return false;
    }
}

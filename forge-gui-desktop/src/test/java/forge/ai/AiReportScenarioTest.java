package forge.ai;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.CardLists;
import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.simulation.ActivationGuard;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.OpeningHand;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * One scenario per open "AI doesn't / AI wrongly ..." report (issue number in the scenario name).
 *
 * Every scenario builds a small board, asks the AI for its next decision and asserts the CORRECT behaviour,
 * so a FAILED scenario means the report still reproduces and a PASSED one means it is fixed (or the setup does
 * not reproduce it - see the fidelity of the scenario in the summary).
 *
 * Fidelity: EXACT = the situation of the title, APPROX = card text/behaviour assumed from the report title,
 * SMOKE = only checks that the AI decides without blowing up (no verdict possible without a card script).
 *
 * mvn -pl forge-gui-desktop test -Dtest=AiReportScenarioTest -DfailIfNoTests=false
 *   -Dforge.test.aiProfile=NAME   run the AI under another profile (e.g. the FlatMC one)
 *   -Dforge.test.issue=4261       run a single scenario
 *
 * The engine calls that are most likely to need a tweak for a given tree are collected in the Ctx class.
 */
public class AiReportScenarioTest extends AITest {

    private static final String PROFILE = System.getProperty("forge.test.aiProfile", "");
    /** The FlatMC picker is off unless the game says so (the Match turns it on in a real game): default on, -Dforge.test.flatmc=false for the plain heuristics. */
    private static final boolean FLAT_MC = !"false".equals(System.getProperty("forge.test.flatmc", "true"));
    private static final String ONLY = System.getProperty("forge.test.issue", "");
    private static final List<String> SUMMARY = new ArrayList<>();

    private static void forceAttackAggression(final AiAttackController controller, final int aggression) {
        try {
            final Field field = AiAttackController.class.getDeclaredField("aiAggression");
            field.setAccessible(true);
            field.setInt(controller, aggression);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("Could not set the attack aggression for the regression scenario", e);
        }
    }

    private enum Fid { EXACT, APPROX, SMOKE }

    private interface Setup { void run(Ctx c); }
    /** @return null when the AI behaved correctly, otherwise what it did instead */
    private interface Check { String run(Ctx c); }

    private static final class Scenario {
        final String issue, title;
        final Fid fid;
        final String phase; // name of the PhaseType: the enum needs the Localizer, which only exists once a game is created
        final boolean aiTurn;
        final Setup setup;
        final Check check;

        Scenario(final String issue, final String title, final Fid fid, final String phase, final boolean aiTurn,
                 final Setup setup, final Check check) {
            this.issue = issue; this.title = title; this.fid = fid; this.phase = phase; this.aiTurn = aiTurn;
            this.setup = setup; this.check = check;
        }
        @Override public String toString() { return "#" + issue + " " + title + " [" + fid + "]"; }
    }

    // ------------------------------------------------------------------------------------------ engine glue

    private final class Ctx {
        final Game game;
        final Player ai, opp;

        Ctx(final Game g) { game = g; opp = g.getPlayers().get(0); ai = g.getPlayers().get(1); }

        /** A permanent that has been there for a while: no summoning sickness, so {T} abilities and attacks are possible. */
        Card bf(final String name, final Player p) {
            final Card k = addCardToZone(name, p, ZoneType.Battlefield);
            k.setSickness(false);
            return k;
        }
        void bf(final String name, final int n, final Player p) { for (int i = 0; i < n; i++) { bf(name, p); } }
        Card hand(final String name, final Player p) { return addCardToZone(name, p, ZoneType.Hand); }
        Card gy(final String name, final Player p) { return addCardToZone(name, p, ZoneType.Graveyard); }
        Card lib(final String name, final Player p) { return addCardToZone(name, p, ZoneType.Library); }
        void lib(final String name, final int n, final Player p) { for (int i = 0; i < n; i++) { lib(name, p); } }
        Card exile(final String name, final Player p) { return addCardToZone(name, p, ZoneType.Exile); }

        /** Plenty of mana of every colour for the AI. */
        void richMana() { for (final String l : new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" }) { bf(l, 3, ai); } }
        void counters(final Card c, final CounterEnumType t, final int n) { c.setCounters(t, n); }

        /** Evaluate a trigger without depending on AiController's convenience wrapper. */
        boolean shouldRunAiTrigger(final SpellAbility sa, final boolean mandatory) {
            if (sa instanceof forge.game.trigger.WrappedAbility) {
                return shouldRunAiTrigger(((forge.game.trigger.WrappedAbility) sa).getWrappedAbility(), mandatory);
            }
            if (sa.getApi() != null) {
                return SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, mandatory);
            }
            return sa.getPayCosts().isFree() && !sa.usesTargeting();
        }

        AiController controller() { return ((PlayerControllerAi) ai.getController()).getAi(); }

        List<SpellAbility> plays() {
            final List<SpellAbility> l = controller().chooseSpellAbilityToPlay();
            return l == null ? new ArrayList<SpellAbility>() : l;
        }
        SpellAbility playOf(final String card) {
            for (final SpellAbility sa : plays()) {
                if (sa.getHostCard() != null && card.equals(sa.getHostCard().getName())) { return sa; }
            }
            return null;
        }
        boolean plays(final String card) { return playOf(card) != null; }
        SpellAbility first() { final List<SpellAbility> l = plays(); return l.isEmpty() ? null : l.get(0); }
        Card target(final SpellAbility sa) {
            return sa != null && sa.usesTargeting() ? sa.getTargets().getFirstTargetedCard() : null;
        }
        SpellAbility tokenAbility(final String card) {
            for (final SpellAbility sa : plays()) {
                if (sa.getHostCard() != null && card.equals(sa.getHostCard().getName()) && sa.getApi() == ApiType.Token) {
                    return sa;
                }
            }
            return null;
        }
        /** The land drops the AI would make, best first. */
        List<Card> landPlays() {
            final List<Card> out = new ArrayList<>();
            for (final SpellAbility sa : plays()) { // a land drop comes back as a play whose host is a land in hand
                if (sa.getHostCard() != null && sa.getHostCard().isLand() && sa.getHostCard().isInZone(ZoneType.Hand)) { out.add(sa.getHostCard()); }
            }
            return out;
        }
        /** What the heuristics think of each ability of the card (in play or in hand): why a play was left out. */
        String why(final String card) {
            final StringBuilder sb = new StringBuilder(" | sorcery=" + ai.canCastSorcery() + " phase=" + game.getPhaseHandler().getPhase()
                    + " turn=" + game.getPhaseHandler().getPlayerTurn() + " ai=" + ai + " mana=" + ComputerUtilMana.getAvailableManaEstimate(ai)
                    + " | heuristics:");
            for (final ZoneType z : new ZoneType[] { ZoneType.Battlefield, ZoneType.Hand }) {
                for (final Card k : ai.getCardsIn(z)) {
                    if (!card.equals(k.getName())) { continue; }
                    if (z == ZoneType.Hand) { // what the enter-the-battlefield check makes of it, and of each "as it enters" choice
                        try {
                            final SpellAbility main = k.getFirstSpellAbility();
                            main.setActivatingPlayer(ai);
                            sb.append(" [etbCheck=").append(controller().checkETBEffects(k, main, null));
                            for (final forge.game.replacement.ReplacementEffect re : k.getReplacementEffects()) {
                                final SpellAbility ex = re.getOverridingAbility();
                                if (ex != null) { sb.append(" replacement:").append(ex.getApi()).append("=").append(shouldRunAiTrigger(ex.copy(ai), false))
                                        .append(" decision=").append(SpellApiToAi.Converter.get(ex).doTriggerNoCostWithSubs(ai, ex.copy(ai), false))
                                        .append(" opposing permanents with an activated ability=").append(CardLists.getValidCards(
                                                ai.getOpponents().getCardsIn(ZoneType.Battlefield), "Card.OppCtrl+hasNonManaActivatedAbility", ai, k, ex)); }
                            }
                            for (final forge.game.trigger.Trigger tr : k.getTriggers()) { // each enter-the-battlefield trigger, one ability of its chain at a time
                                if (tr.getMode() != forge.game.trigger.TriggerType.ChangesZone || !"Battlefield".equals(tr.getParam("Destination"))) { continue; }
                                sb.append(" trigger(stack=").append(game.getStack().size()).append("):");
                                for (SpellAbility ex = tr.ensureAbility().copy(ai); ex != null; ex = ex.getSubAbility()) {
                                    sb.append(' ').append(ex.getApi()).append('=').append(shouldRunAiTrigger(ex, false));
                                }
                            }
                            sb.append(']');
                        } catch (final RuntimeException e) { sb.append(" [etbCheck threw ").append(e).append(']'); }
                    }
                    for (final SpellAbility sa : k.getSpellAbilities()) {
                        try {
                            sa.setActivatingPlayer(ai);
                            sb.append(" [").append(z).append(' ').append(sa.getApi()).append(" -> ").append(controller().canPlaySa(sa))
                                    .append(" real=").append(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa)).append(" payable=").append(ComputerUtilCost.canPayCost(sa, ai, false)).append(" failedBefore=").append(controller().hasFailedToCast(sa)).append(']');
                        } catch (final RuntimeException e) { sb.append(" [").append(sa.getApi()).append(" threw ").append(e).append(']'); }
                    }
                }
            }
            return sb.toString();
        }
        /** Attackers the AI would declare now. */
        List<Card> attackers() {
            final Combat combat = new Combat(ai);
            ai.getController().declareAttackers(ai, combat);
            return new ArrayList<Card>(combat.getAttackers());
        }
        /** Blockers the AI would assign against the given attackers of the opponent. */
        List<Card> blockers(final Card... attackers) {
            final Combat combat = new Combat(opp);
            for (final Card a : attackers) { combat.addAttacker(a, ai); }
            game.getPhaseHandler().setCombat(combat);
            new AiBlockController(ai, false).assignBlockersForCombat(combat);
            return new ArrayList<Card>(combat.getAllBlockers());
        }
        /** The player puts a spell with the given target(s) on the stack, for the AI to respond to. */
        void stack(final Player owner, final String name, final GameObject... targets) {
            final SpellAbility sa = hand(name, owner).getFirstSpellAbility();
            sa.setActivatingPlayer(owner);
            for (final GameObject t : targets) { sa.getTargets().add(t); }
            game.getStack().add(sa);
        }
        /** Plays and resolves an ability the AI chose (costs paid, stack emptied). */
        void resolve(final SpellAbility sa) {
            ComputerUtil.playStack(sa, ai, game);
            resolveAll();
        }

        /** Cast Overlord using its real Impending alternative cost, then restore the scenario's phase/turn. */
        Card impendingOverlord() {
            final PhaseType previousPhase = game.getPhaseHandler().getPhase();
            final Player previousTurn = game.getPhaseHandler().getPlayerTurn();
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

            final Card spell = hand("Overlord of the Balemurk", ai);
            SpellAbility impending = null;
            for (final SpellAbility sa : spell.getSpellAbilities()) {
                if (sa.isSpell() && sa.isImpending()) {
                    impending = sa;
                    break;
                }
            }
            if (impending == null) {
                throw new AssertionError("Overlord has no Impending spell ability");
            }

            impending.setActivatingPlayer(ai);
            try {
                if (!ComputerUtil.playStack(impending, ai, game)) {
                    throw new AssertionError("Could not cast Overlord using its Impending cost");
                }
                resolveAll();
            } finally {
                game.getPhaseHandler().devModeSet(previousPhase, previousTurn);
            }

            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if ("Overlord of the Balemurk".equals(c.getName())) {
                    if (c.getCastSA() == null || !c.getCastSA().isImpending() || c.isCreature()
                            || c.getCounters(CounterEnumType.TIME) != 5) {
                        throw new AssertionError("Overlord did not enter as an impending noncreature: " + c);
                    }
                    return c;
                }
            }
            throw new AssertionError("Impending Overlord did not enter the battlefield");
        }

        /** Resolves the stack until nothing is left, putting triggered abilities (enter-the-battlefield ...) on it as the real game does. */
        final StringBuilder trace = new StringBuilder();
        void resolveAll() {
            for (int i = 0; i < 50; i++) {
                game.getStack().addAllTriggeredAbilitiesToStack();
                if (game.getStack().isEmpty()) { return; }
                final SpellAbility top = game.getStack().peekAbility();
                trace.append(" [resolving ").append(top).append(top.usesTargeting() ? " -> " + top.getTargets() : "").append(']');
                game.getStack().resolveStack();
                game.getAction().checkStateEffects(true); // lethal damage kills, as it does between two priority passes
            }
        }
        /** The player gets the effect of a spell for free (protection, hexproof ...). */
        void effect(final Player owner, final String name) {
            bf("Plains", 3, owner); // the effect is paid for like any spell, and the player has no mana of its own ({2}{W} for Teferi's Protection)
            final SpellAbility sa = hand(name, owner).getFirstSpellAbility();
            sa.setActivatingPlayer(owner);
            if (!ComputerUtil.playNoStack(owner, sa, game, true)) {
                throw new IllegalStateException("setup: the effect of " + name + " could not be played (cost not payable?)");
            }
            if (name.equals("Teferi's Protection") && !owner.hasKeyword("Protection from everything")) {
                throw new IllegalStateException("setup: " + name + " resolved but the player has no protection, keywords=" + owner.getKeywords());
            }
        }
    }

    // ------------------------------------------------------------------------------------------ factories

    private static Scenario play(final String id, final String t, final Fid f, final String ph, final boolean aiTurn,
                                 final String card, final Setup s) {
        return new Scenario(id, t + " (should play " + card + ")", f, ph, aiTurn, s,
                c -> c.plays(card) ? null : "AI did not play/activate " + card + c.why(card));
    }

    private static Scenario noPlay(final String id, final String t, final Fid f, final String ph, final boolean aiTurn,
                                   final String card, final Setup s) {
        return new Scenario(id, t + " (should NOT play " + card + ")", f, ph, aiTurn, s,
                c -> c.plays(card) ? "AI played/activated " + card + ": " + c.plays() : null);
    }

    /** The card is played and aimed at the wanted target. */
    private static Scenario aimed(final String id, final String t, final Fid f, final String ph, final boolean aiTurn,
                                  final String card, final String wanted, final Setup s) {
        return new Scenario(id, t + " (" + card + " should target " + wanted + ")", f, ph, aiTurn, s, c -> {
            final SpellAbility sa = c.playOf(card);
            if (sa == null) { return "AI did not play " + card + c.why(card); }
            final Card got = c.target(sa);
            return got != null && wanted.equals(got.getName()) ? null : card + " targeted " + got;
        });
    }

    /** The ability is activated: richMana, host in play, a target for everything. */
    private static Scenario activates(final String id, final String t, final String host, final Setup extra) {
        return new Scenario(id, t + " (should activate " + host + ")", Fid.APPROX, "MAIN1", true, c -> {
            c.richMana(); c.bf(host, c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Sol Ring", c.opp);
            if (extra != null) { extra.run(c); }
        }, c -> c.plays(host) ? null : "AI never activated " + host + c.why(host));
    }

    private AiPlayDecision freeCast(final Ctx c, final String name) {
        final Card k = addCardToZone(name, c.ai, ZoneType.Exile);
        final SpellAbility sa = k.getFirstSpellAbility().copyWithNoManaCost();
        sa.setActivatingPlayer(c.ai);
        return c.controller().canPlaySa(sa);
    }

    private static Scenario skip(final String id, final String t, final String why) {
        return new Scenario(id, t, Fid.SMOKE, "MAIN1", true, c -> { }, c -> { throw new SkipException(why); });
    }

    // ------------------------------------------------------------------------------------------ scenarios

    /** Casts the Machine God's Effigy for the AI, lets it resolve, and checks what it became (the name of the copied card, or its own). */
    private String effigyBecame(final Ctx c, final String expected) {
        final Card effigy = c.hand("Machine God's Effigy", c.ai);
        final SpellAbility sa = effigy.getFirstSpellAbility();
        sa.setActivatingPlayer(c.ai);
        c.resolve(sa);
        final Card now = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getId() == effigy.getId()).findFirst()
                .orElse(c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals(expected)).findFirst().orElse(null));
        if (now == null) { return "the Effigy is not on the battlefield" + c.trace; }
        final boolean named = now.getName().equals(expected);
        final boolean noCreature = !now.isCreature() && now.isArtifact();
        return named && (noCreature || expected.startsWith("Machine")) ? null
                : "the Effigy became " + now.getName() + (now.isCreature() ? " (a creature)" : "") + ", expected " + expected + c.trace;
    }

    private List<Scenario> scenarios() {
        final List<Scenario> l = new ArrayList<>();
        final String M1 = "MAIN1";

        // ---------------- AI doesn't cast ...
        l.add(play("4261", "name-a-card lock piece against a Sol Ring", Fid.EXACT, "MAIN2", true, "Pithing Needle",
                c -> { c.bf("Plains", c.ai); c.hand("Pithing Needle", c.ai); c.counters(c.bf("Wrenn and Six", c.opp), CounterEnumType.LOYALTY, 3); /* (bf() adds no loyalty and the walker would die) a planeswalker is what the AI Needles for sure; a plain activated ability is only a 5% roll, and a mana ability (Sol Ring) is exempt */ }));
        l.add(play("4279", "Solitary Confinement at low life with two other cards (the discard for its upkeep)", Fid.EXACT, "MAIN2", true, "Solitary Confinement",
                c -> { c.ai.setLife(5, null); c.bf("Plains", 3, c.ai); c.hand("Solitary Confinement", c.ai); c.hand("Grizzly Bears", c.ai); c.hand("Wurmcoil Engine", c.ai); /* uncastable with Plains only: discard fodder */ c.bf("Grizzly Bears", c.opp); }));
        l.add(noPlay("4279", "Solitary Confinement at full life: no reason to skip its own draw step", Fid.EXACT, "MAIN2", true, "Solitary Confinement",
                c -> { c.bf("Plains", 3, c.ai); c.hand("Solitary Confinement", c.ai); c.hand("Grizzly Bears", c.ai); c.hand("Wurmcoil Engine", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(play("4279", "Forge Anew with an equipment in play", Fid.APPROX, "MAIN2", true, "Forge Anew",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Forge Anew", c.ai); c.gy("Bonesplitter", c.ai); /* its enter trigger needs an Equipment in the graveyard */ c.bf("Grizzly Bears", c.ai); }));
        l.add(play("4279", "Springleaf Drum with a creature to tap", Fid.EXACT, M1, true, "Springleaf Drum",
                c -> { c.bf("Forest", 2, c.ai); c.hand("Springleaf Drum", c.ai); c.bf("Grizzly Bears", c.ai); }));
        l.add(play("4302", "Unmoored Ego against a full hand", Fid.EXACT, M1, true, "Unmoored Ego",
                c -> { c.bf("Island", c.ai); c.bf("Swamp", 2, c.ai); /* {1}{U}{B} */ c.hand("Unmoored Ego", c.ai); c.hand("Lightning Bolt", c.opp); c.hand("Lightning Bolt", c.opp); }));
        l.add(new Scenario("4326", "bounce land with a single land in play (should play Azorius Chancery)", Fid.EXACT, M1, true,
                c -> { c.bf("Plains", c.ai); c.hand("Azorius Chancery", c.ai); },
                c -> c.landPlays().stream().anyMatch(k -> k.getName().equals("Azorius Chancery")) ? null : "Chancery not chosen as land drop"));
        l.add(play("4359", "suspending Restore Balance with one land", Fid.EXACT, "MAIN2", true, "Restore Balance",
                c -> { c.bf("Plains", c.ai); c.hand("Restore Balance", c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Grizzly Bears", c.opp); }));
        l.add(play("4464", "protection trick against a burn spell aimed at the AI's creature", Fid.APPROX, M1, false, "Gods Willing",
                c -> { c.bf("Plains", c.ai); c.hand("Gods Willing", c.ai); final Card birds = c.bf("Birds of Paradise", c.ai); /* Serra Angel (4 toughness) survives a Bolt, so nothing to save */ c.stack(c.opp, "Lightning Bolt", birds); }));
        l.add(play("5897", "Invert Polarity against a burn spell", Fid.APPROX, M1, false, "Invert Polarity",
                c -> { c.richMana(); c.hand("Invert Polarity", c.ai); c.stack(c.opp, "Serra Angel"); /* the report: a spell worth 3+ mana (a Bolt is not) */ }));
        l.add(play("7626", "Titan's Presence with a colorless creature to reveal", Fid.APPROX, M1, true, "Titan's Presence",
                c -> { c.bf("Forest", 3, c.ai); c.hand("Titan's Presence", c.ai); c.hand("Wurmcoil Engine", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(new Scenario("4113", "cascaded/free Glimpse of Tomorrow is resolved (not just exiled) with only lands in play", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); c.bf("Forest", 2, c.ai); c.lib("Grizzly Bears", c.ai); c.lib("Serra Angel", c.ai); c.lib("Wurmcoil Engine", c.ai); },
                c -> { // the cascade offers the exiled card for free: the AI's answer to "cast it without paying its mana cost?"
                    final Card g = addCardToZone("Glimpse of Tomorrow", c.ai, ZoneType.Exile);
                    final SpellAbility sa = g.getFirstSpellAbility().copyWithNoManaCost();
                    sa.setActivatingPlayer(c.ai);
                    final AiPlayDecision d = c.controller().canPlaySa(sa);
                    return d == AiPlayDecision.WillPlay ? null : "AI would not cast the cascaded Glimpse of Tomorrow: " + d;
                }));
        l.add(play("4210", "Violent Outburst with no creature to attack", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); /* {1}{R}{G} */ c.hand("Violent Outburst", c.ai); c.lib("Lightning Bolt", c.ai); c.lib("Mountain", c.ai); }));
        // the report: a Living End deck casts it for the cascade, once the graveyard is ready (ComboPlan decides, see massReanimationRuling)
        l.add(play("4210", "Living End deck, five creatures in the graveyard, no creature to pump", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); c.hand("Violent Outburst", c.ai); c.lib("Living End", c.ai); c.lib("Lightning Bolt", c.ai);
                    for (int i = 0; i < 5; i++) { c.gy("Grizzly Bears", c.ai); } }));
        l.add(noPlay("4210", "Living End deck, empty graveyard: the cascade would waste the payoff", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); c.hand("Violent Outburst", c.ai); c.lib("Living End", c.ai); c.lib("Lightning Bolt", c.ai); }));
        // #4389: cascading into Living End with a full board kills the AI's own creatures and returns the opponent's best ones
        l.add(noPlay("4389", "Living End deck, graveyard ready but five creatures on the battlefield: no cascade spell", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); c.hand("Violent Outburst", c.ai); c.lib("Living End", c.ai); c.lib("Lightning Bolt", c.ai);
                    for (int i = 0; i < 5; i++) { c.gy("Grizzly Bears", c.ai); c.bf("Grizzly Bears", c.ai); } c.gy("Serra Angel", c.opp); }));
        l.add(new Scenario("4389", "cascaded Living End with five creatures on the battlefield and the opponent's angels in the graveyard is not cast", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); for (int i = 0; i < 5; i++) { c.bf("Grizzly Bears", c.ai); } c.gy("Grizzly Bears", c.ai);
                    c.gy("Serra Angel", c.opp); c.gy("Serra Angel", c.opp); c.gy("Wurmcoil Engine", c.opp); },
                c -> {
                    final AiPlayDecision d = freeCast(c, "Living End");
                    return d != AiPlayDecision.WillPlay ? null : "AI cast the cascaded Living End and sacrificed its " + c.ai.getCreaturesInPlay().size() + " creatures";
                }));
        l.add(new Scenario("4389", "control: cascaded Living End with an empty battlefield and a full graveyard is cast", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); for (int i = 0; i < 4; i++) { c.gy("Serra Angel", c.ai); } c.gy("Grizzly Bears", c.opp); },
                c -> {
                    final AiPlayDecision d = freeCast(c, "Living End");
                    return d == AiPlayDecision.WillPlay ? null : "AI would not cast the cascaded Living End: " + d;
                }));
        // re-equipping the creature an Equipment is already on is idle: a "becomes attached" trigger alone is no combo
        l.add(noPlay("FMC-equip0", "Lightning Greaves stays put when only a Bramble Elemental trigger could use the re-equip", Fid.APPROX, M1, true, "Lightning Greaves",
                c -> { final Card g = c.bf("Lightning Greaves", c.ai); final Card b = c.bf("Grizzly Bears", c.ai); g.attachToEntity(b, null);
                    c.bf("Bramble Elemental", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(play("4215", "Collected Company at the opponent's end step", Fid.EXACT, "END_OF_TURN", false, "Collected Company",
                c -> { c.bf("Forest", 4, c.ai); c.hand("Collected Company", c.ai); for (int i = 0; i < 6; i++) { c.lib("Grizzly Bears", c.ai); } }));
        l.add(play("4352", "Adventure side while the main spell is castable too", Fid.APPROX, M1, true, "Bonecrusher Giant",
                c -> { c.bf("Mountain", 3, c.ai); c.hand("Bonecrusher Giant", c.ai); c.hand("Goblin Guide", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(play("6015", "Skullcrack against a player at 20 life", Fid.EXACT, "MAIN2", true, "Skullcrack",
                c -> { c.bf("Mountain", 2, c.ai); c.hand("Skullcrack", c.ai); }));
        l.add(play("6601", "Boltwave against a player at 20 life with creatures", Fid.EXACT, "MAIN2", true, "Boltwave",
                c -> { c.bf("Mountain", 3, c.ai); c.hand("Boltwave", c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Grizzly Bears", c.opp); }));

        // ---------------- the same name chosen by several copies
        l.add(new Scenario("12801", "a second Meddling Mage names a different card than the first", Fid.APPROX, M1, true,
                c -> {
                    c.bf("Birds of Paradise", c.opp); c.bf("Elvish Mystic", c.opp); c.bf("Llanowar Elves", c.opp);
                    c.bf("Meddling Mage", c.ai).setNamedCards(Arrays.asList("Birds of Paradise"));
                },
                c -> {
                    final Card second = c.bf("Meddling Mage", c.ai);
                    final SpellAbility naming = second.getReplacementEffects().get(0).getOverridingAbility();
                    naming.setActivatingPlayer(c.ai);
                    final String name = ((PlayerControllerAi) c.ai.getController()).chooseCardName(naming, f -> true, "Card.nonLand", "");
                    return "Birds of Paradise".equals(name) ? "named Birds of Paradise again" : null;
                }));
        // Meddling Mage names the opponent's key cards in the order of the list, skipping one that is already in play
        for (final boolean firstInPlay : new boolean[] { false, true }) {
            l.add(new Scenario("meddling-mage-key-order", "Meddling Mage names the first key card" + (firstInPlay ? " that is not already in play" : ""),
                    Fid.EXACT, M1, true,
                    c -> {
                        if (c.opp.getRegisteredPlayer() == null || c.opp.getRegisteredPlayer().getDeck() == null) {
                            throw new SkipException("the test game has no registered deck to hold key cards");
                        }
                        // getKeyCards() hands out a copy: the list is filled through addKeyCard, in the order the player wants them answered
                        for (final String key : new String[] { "Eladamri, Korvecdal", "Serra's Emissary", "Iona, Shield of Emeria", "Champion of Rhonas" }) {
                            c.opp.getRegisteredPlayer().getDeck().addKeyCard(key);
                        }
                        if (firstInPlay) { c.bf("Eladamri, Korvecdal", c.opp); } else { c.hand("Eladamri, Korvecdal", c.opp); }
                        c.hand("Serra's Emissary", c.opp); c.hand("Iona, Shield of Emeria", c.opp); c.hand("Champion of Rhonas", c.opp);
                    },
                    c -> {
                        final Card mage = c.bf("Meddling Mage", c.ai);
                        final SpellAbility naming = mage.getReplacementEffects().get(0).getOverridingAbility();
                        naming.setActivatingPlayer(c.ai);
                        final String name = ((PlayerControllerAi) c.ai.getController()).chooseCardName(naming, f -> true, "Card.nonLand", "");
                        final String wanted = firstInPlay ? "Serra's Emissary" : "Eladamri, Korvecdal";
                        return wanted.equals(name) ? null : "named " + name + " instead of " + wanted;
                    }));
        }
        // Vampire Hexmage: "remove all counters" frees an impending permanent (Overlord of the Balemurk is no creature until the last time counter is gone)
        l.add(aimed("hexmage-impending-end-step", "Vampire Hexmage frees the impending Overlord at the opponent's end step", Fid.EXACT, "END_OF_TURN", false,
                "Vampire Hexmage", "Overlord of the Balemurk",
                c -> { c.bf("Swamp", 2, c.ai); c.impendingOverlord(); c.bf("Vampire Hexmage", c.ai); }));
        l.add(aimed("hexmage-impending-removal", "Vampire Hexmage frees Overlord in response to Disenchant", Fid.EXACT, "MAIN1", false,
                "Vampire Hexmage", "Overlord of the Balemurk",
                c -> { c.bf("Swamp", 2, c.ai); final Card overlord = c.impendingOverlord();
                    c.bf("Vampire Hexmage", c.ai); c.stack(c.opp, "Disenchant", overlord); }));
        l.add(aimed("hexmage-in-danger", "Vampire Hexmage frees Overlord before Hexmage is destroyed", Fid.EXACT, "MAIN1", false,
                "Vampire Hexmage", "Overlord of the Balemurk",
                c -> { c.bf("Swamp", 2, c.ai); c.impendingOverlord();
                    final Card hexmage = c.bf("Vampire Hexmage", c.ai); c.stack(c.opp, "Terror", hexmage); }));
        l.add(aimed("hexmage-haywire-mite", "Vampire Hexmage frees Overlord in response to Haywire Mite", Fid.EXACT, "MAIN1", false,
                "Vampire Hexmage", "Overlord of the Balemurk",
                c -> {
                    c.bf("Swamp", 2, c.ai);
                    final Card overlord = c.impendingOverlord();
                    c.bf("Vampire Hexmage", c.ai);
                    final Card mite = c.bf("Haywire Mite", c.opp);
                    SpellAbility exile = null;
                    for (final SpellAbility sa : mite.getSpellAbilities()) {
                        if (sa.getApi() == ApiType.ChangeZone && "Exile".equals(sa.getParam("Destination"))) {
                            exile = sa;
                            break;
                        }
                    }
                    if (exile == null) {
                        throw new AssertionError("Haywire Mite has no exile ability");
                    }
                    exile.setActivatingPlayer(c.opp);
                    if (!exile.canTarget(overlord)) {
                        throw new AssertionError("Haywire Mite cannot target impending Overlord");
                    }
                    exile.getTargets().add(overlord);
                    c.game.getStack().add(exile);
                }));
        l.add(noPlay("hexmage-idle", "Vampire Hexmage keeps itself while nothing threatens it and it is not the end of the opponent's turn", Fid.EXACT, "MAIN1", false, "Vampire Hexmage",
                c -> { c.bf("Swamp", 2, c.ai); c.impendingOverlord(); c.bf("Vampire Hexmage", c.ai); }));
        // Tavern Scoundrel + Krark, the Thumbless: winning the coin flip of Krark's trigger is winning a coin flip
        l.add(new Scenario("krark-tavern-scoundrel", "Krark's coin flip won: Tavern Scoundrel creates two Treasure tokens", Fid.EXACT, M1, true,
                c -> {
                    c.bf("Krark, the Thumbless", c.ai);
                    c.bf("Tavern Scoundrel", c.ai);
                    c.bf("Edgar, King of Figaro", c.ai); // Two-Headed Coin: the first flip each turn is won, so the test is not random
                },
                c -> {
                    c.bf("Mountain", c.ai);
                    final Card bolt = c.hand("Lightning Bolt", c.ai);
                    final SpellAbility sa = bolt.getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    sa.getTargets().add(c.opp);
                    c.resolve(sa);
                    final long treasures = c.ai.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(k -> k.getName().startsWith("Treasure")).count();
                    return treasures == 2 ? null : "the flip was won but there are " + treasures + " Treasure tokens" + c.trace;
                }));
        l.add(new Scenario("5896", "a second Pithing Needle names a different card than the first", Fid.APPROX, M1, true,
                c -> {
                    c.bf("Spellskite", c.opp); c.bf("Agatha's Soul Cauldron", c.opp); c.bf("Shalai, Voice of Plenty", c.opp);
                    c.bf("Pithing Needle", c.ai).setNamedCards(Arrays.asList("Shalai, Voice of Plenty"));
                },
                c -> {
                    final Card second = c.bf("Pithing Needle", c.ai);
                    final SpellAbility naming = second.getReplacementEffects().get(0).getOverridingAbility();
                    naming.setActivatingPlayer(c.ai);
                    final String name = ((PlayerControllerAi) c.ai.getController()).chooseCardName(naming, f -> true, "", "");
                    return "Shalai, Voice of Plenty".equals(name) ? "named Shalai, Voice of Plenty again" : null;
                }));

        // ---------------- AI casts ... wrongly
        l.add(new Scenario("3994", "burn at a creature it cannot kill (Lightning Bolt should not target Wurmcoil Engine)", Fid.EXACT, M1, true,
                c -> { c.bf("Mountain", c.ai); c.hand("Lightning Bolt", c.ai); c.bf("Wurmcoil Engine", c.opp); },
                c -> { final Card t = c.target(c.playOf("Lightning Bolt")); return t != null && t.getName().equals("Wurmcoil Engine") ? "Bolt aimed at Wurmcoil Engine" : null; }));
        l.add(noPlay("X0", "an {X} animate with no spare mana is paid with X=0 and repeated at every priority pass", Fid.EXACT, "DRAW", false, "Chimeric Staff",
                c -> { c.bf("Chimeric Staff", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(noPlay("4122", "Persist on Orcish Bowmasters", Fid.EXACT, M1, true, "Persist",
                c -> { c.bf("Swamp", 3, c.ai); c.hand("Persist", c.ai); c.gy("Orcish Bowmasters", c.ai); }));
        l.add(noPlay("4124", "Underworld Breach with an empty graveyard", Fid.EXACT, M1, true, "Underworld Breach",
                c -> { c.bf("Mountain", 2, c.ai); c.hand("Underworld Breach", c.ai); }));
        l.add(noPlay("4130", "second Inquisition into a hand with no legal target", Fid.EXACT, M1, true, "Inquisition of Kozilek",
                c -> { c.bf("Swamp", c.ai); c.hand("Inquisition of Kozilek", c.ai); c.hand("Wurmcoil Engine", c.opp); c.hand("Mountain", c.opp); }));
        l.add(noPlay("4233", "evoking Grief while the player has hexproof", Fid.EXACT, M1, true, "Grief",
                c -> { c.hand("Grief", c.ai); c.hand("Thoughtseize", c.ai); c.bf("Leyline of Sanctity", c.opp); c.hand("Lightning Bolt", c.opp); }));
        l.add(new Scenario("4278", "Up the Beanstalk before the 5+ cost spell (should be the first play)", Fid.EXACT, M1, true,
                c -> { c.bf("Forest", 10, c.ai); c.hand("Up the Beanstalk", c.ai); c.hand("Craterhoof Behemoth", c.ai); },
                c -> { final SpellAbility sa = c.first();
                    return sa != null && sa.getHostCard().getName().equals("Up the Beanstalk") ? null
                            : "first play was " + sa + c.why("Up the Beanstalk") + c.why("Craterhoof Behemoth"); }));
        l.add(noPlay("4299", "Preordain in the player's upkeep under Teferi, Time Raveler", Fid.EXACT, "UPKEEP", false, "Preordain",
                c -> { c.bf("Island", 2, c.ai); c.hand("Preordain", c.ai); c.bf("Teferi, Time Raveler", c.opp); }));
        l.add(noPlay("4325", "Angel's Grace against non-lethal damage", Fid.APPROX, M1, false, "Angel's Grace",
                c -> { c.bf("Plains", c.ai); c.hand("Angel's Grace", c.ai); c.stack(c.opp, "Lightning Bolt", c.ai); }));
        l.add(new Scenario("4443", "kicked Bloodchief's Thirst on a 2-mana creature (kicker only for a 3+ mana value target)", Fid.APPROX, M1, true,
                c -> { c.bf("Swamp", 4, c.ai); c.hand("Bloodchief's Thirst", c.ai); c.hand("Smallpox", c.ai); c.bf("Giant Ox", c.opp); c.bf("Grizzly Bears", c.opp); },
                c -> { final SpellAbility sa = c.playOf("Bloodchief's Thirst"); return sa != null && sa.isKicked() ? "cast kicked on a creature of mana value 2 or less" : null; }));
        l.add(noPlay("5891", "flickering Grief when the player has no cards in hand", Fid.APPROX, M1, true, "Ephemerate",
                c -> { c.bf("Plains", c.ai); c.hand("Ephemerate", c.ai); c.bf("Grief", c.ai); }));

        // ---------------- AI doesn't use ability ...
        l.add(play("4391", "Waker of Waves in hand with three lands, one of them blue (it can never be cast)", Fid.EXACT, "MAIN2", true, "Waker of Waves",
                c -> { c.bf("Island", c.ai); c.bf("Mountain", 2, c.ai); c.hand("Waker of Waves", c.ai); c.lib("Island", c.ai); c.lib("Mountain", c.ai); }));
        l.add(activates("5904", "Unstable Amulet with energy to spend", "Unstable Amulet", c -> c.ai.setCounters(CounterEnumType.ENERGY, 4)));
        l.add(new Scenario("5935", "channel ability that does not deal damage (Boseiju at an artifact)", Fid.APPROX, M1, true,
                c -> { c.bf("Forest", 2, c.ai); c.hand("Boseiju, Who Endures", c.ai); c.bf("Sol Ring", c.opp); },
                c -> c.plays().stream().anyMatch(sa -> sa.getHostCard().getName().equals("Boseiju, Who Endures") && sa.isAbility()) ? null : "channel not used"));
        l.add(new Scenario("6108", "The Underworld Cookbook: discard the fatty for the reanimation spell (should activate The Underworld Cookbook)", Fid.APPROX, "MAIN2", true,
                c -> { c.bf("Swamp", 3, c.ai); /* too little mana to cast Emrakul: it can only be discarded */ c.bf("The Underworld Cookbook", c.ai); c.hand("Goryo's Vengeance", c.ai); c.hand("Griselbrand", c.ai); /* (not Emrakul: its own trigger shuffles it out of the graveyard the moment it is discarded) */ c.bf("Grizzly Bears", c.opp); },
                c -> c.plays("The Underworld Cookbook") ? null : "AI never activated The Underworld Cookbook" + c.why("The Underworld Cookbook")));
        l.add(play("6719", "Catapult Captain can kill the opponent by sacrificing a 0/6", Fid.EXACT, M1, true, "Catapult Captain",
                c -> { c.bf("Swamp", 3, c.ai); c.bf("Catapult Fodder", c.ai).changeCardState("Transform", null, null); /* the Captain is its back face */ c.bf("Giant Ox", c.ai); c.opp.setLife(6, null); }));
        l.add(play("4022", "Stoneforge Mystic with an equipment in hand (activate, don't attack)", Fid.EXACT, M1, true, "Stoneforge Mystic",
                c -> { c.bf("Plains", 2, c.ai); c.bf("Stoneforge Mystic", c.ai); c.hand("Batterskull", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(new Scenario("4023", "Stoneforge Mystic fetches the best equipment (Batterskull, not Bonesplitter)", Fid.APPROX, M1, true,
                c -> { c.bf("Plains", 2, c.ai); c.bf("Stoneforge Mystic", c.ai); c.hand("Bonesplitter", c.ai); c.hand("Batterskull", c.ai); },
                c -> {
                    final SpellAbility sa = c.playOf("Stoneforge Mystic");
                    if (sa == null) { return "no activation"; }
                    c.resolve(sa);
                    return c.ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(k -> k.getName().equals("Batterskull")) ? null : "did not put Batterskull in play";
                }));
        l.add(new Scenario("4087", "fetchland: the shockland it fetches should come in untapped when the mana is needed", Fid.APPROX, M1, true,
                c -> { c.bf("Wooded Foothills", c.ai); c.bf("Mountain", c.ai); c.hand("Grizzly Bears", c.ai); c.lib("Stomping Ground", c.ai); },
                c -> {
                    final SpellAbility fetch = c.playOf("Wooded Foothills");
                    if (fetch == null) { return "AI did not crack the fetchland"; }
                    c.resolve(fetch);
                    for (final Card k : c.ai.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Stomping Ground")) { return k.isTapped() ? "shockland entered tapped" : null; }
                    }
                    return "no shockland fetched";
                }));
        l.add(play("4116", "Arbor Elf untapping a Forest to cast Grizzly Bears", Fid.EXACT, M1, true, "Arbor Elf",
                c -> { final Card f = c.bf("Forest", c.ai); c.bf("Forest", c.ai); c.bf("Arbor Elf", c.ai); f.setTapped(true); c.hand("Savannah Lions", c.ai); c.hand("Grizzly Bears", c.ai); }));
        l.add(new Scenario("4131", "Wrenn and Six at low loyalty with no X/1 targets (should not use the -1)", Fid.EXACT, M1, true,
                c -> { final Card w = c.bf("Wrenn and Six", c.ai); c.counters(w, CounterEnumType.LOYALTY, 2); c.bf("Grizzly Bears", c.opp); },
                c -> c.plays().stream().anyMatch(sa -> sa.getHostCard().getName().equals("Wrenn and Six") && sa.getDescription().contains("1 damage")) ? "used the -1" : null));
        l.add(play("4169", "Agatha's Soul Cauldron in response to Unearth", Fid.APPROX, M1, false, "Agatha's Soul Cauldron",
                c -> { c.bf("Agatha's Soul Cauldron", c.ai); final Card bears = c.gy("Grizzly Bears", c.opp); c.stack(c.opp, "Unearth", bears); }));
        l.add(new Scenario("4373", "Giver of Runes is not activated before combat and the AI still attacks", Fid.APPROX, M1, true,
                c -> { c.bf("Giver of Runes", c.ai); c.bf("Grizzly Bears", c.ai); c.bf("Assault Formation", c.ai); c.bf("Forest", 2, c.ai); },
                c -> {
                    if (c.plays("Giver of Runes")) { return "activated Giver of Runes precombat"; }
                    return c.attackers().isEmpty() ? "AI did not attack into an empty board" : null;
                }));
        l.add(new Scenario("decayed-not-thrown-away", "A decayed 5/5 does not attack an empty board for 5 of 20 (it would be sacrificed)", Fid.APPROX, M1, true,
                c -> { c.bf("Rot-Curse Rakshasa", c.ai); },
                c -> c.attackers().isEmpty() ? null : "attacked with a decayed creature for no lethal"));
        l.add(new Scenario("decayed-for-lethal", "A decayed 5/5 does attack when it is lethal", Fid.APPROX, M1, true,
                c -> { c.bf("Rot-Curse Rakshasa", c.ai); c.opp.setLife(5, null); },
                c -> c.attackers().isEmpty() ? "did not attack for lethal with the decayed creature" : null));
        l.add(noPlay("12930-ephemerate", "Ephemerate in its own upkeep with a Grizzly Bears to blink: wait for the main phase", Fid.APPROX, "UPKEEP", true, "Ephemerate",
                c -> { c.bf("Grizzly Bears", c.ai); c.bf("Plains", c.ai); c.hand("Ephemerate", c.ai); }));
        l.add(noPlay("12930-kozileks-command", "Kozilek's Command in the player's draw step with a creature to exile: not now", Fid.APPROX, "DRAW", false, "Kozilek's Command",
                c -> { c.bf("Wastes", 4, c.ai); c.bf("Grizzly Bears", c.opp); c.hand("Kozilek's Command", c.ai); }));
        l.add(noPlay("12934-thraben-charm-yard", "Thraben Charm / Endurance do not exile a graveyard holding only a fetch land", Fid.APPROX, M1, true, "Thraben Charm",
                c -> { c.bf("Plains", c.ai); c.bf("Plains", c.ai); c.hand("Thraben Charm", c.ai); c.gy("Wooded Foothills", c.opp); }));
        l.add(new Scenario("12927-dryad-arbor", "Dryad Arbor does not attack into Lyra Dawnbringer (it is just killed)", Fid.APPROX, M1, true,
                c -> { c.bf("Dryad Arbor", c.ai); c.bf("Gladecover Scout", c.ai); c.bf("Lyra Dawnbringer", c.opp); },
                c -> c.attackers().stream().anyMatch(a -> a.getName().equals("Dryad Arbor")) ? "Dryad Arbor attacked into Lyra" : null));
        // Manabarbs: the lands that pay for a spell cost life, and the AI ran into it until it died
        l.add(noPlay("manabarbs-low-life", "At 3 life under Manabarbs the AI does not pay four lands for a Hill Giant", Fid.APPROX, "MAIN2", true, "Hill Giant",
                c -> { c.ai.setLife(3, null); c.bf("Mountain", 4, c.ai); c.hand("Hill Giant", c.ai); c.bf("Manabarbs", c.opp); }));
        l.add(play("manabarbs-healthy", "At 20 life under Manabarbs a cheap creature is still cast", Fid.APPROX, "MAIN2", true, "Savannah Lions",
                c -> { c.bf("Plains", 3, c.ai); c.hand("Savannah Lions", c.ai); c.bf("Manabarbs", c.opp); }));
        l.add(play("manabarbs-answer", "At 4 life the AI still casts Disenchant on the Manabarbs", Fid.APPROX, M1, true, "Disenchant",
                c -> { c.ai.setLife(4, null); c.bf("Plains", 2, c.ai); c.hand("Disenchant", c.ai); c.bf("Manabarbs", c.opp); }));
        l.add(play("manabarbs-creature-mana", "Mana from a creature costs no life: the Elf pays the {G} of a Scryb Sprites", Fid.APPROX, "MAIN2", true, "Scryb Sprites",
                c -> { c.ai.setLife(3, null); c.bf("Plains", c.ai); c.bf("Llanowar Elves", c.ai); c.hand("Scryb Sprites", c.ai); c.bf("Manabarbs", c.opp); }));
        // review of the Manabarbs patch: the estimate is the payment that the AI would make (colours, creature mana, multi-mana lands),
        // the life budget buys the cheapest lands first, and only a removal that is really aimed at the source is exempt
        l.add(new Scenario("manabarbs-estimate-colour", "A white spell needs a Plains tapped: the Elves cannot pay the {W}, so it costs a life", Fid.APPROX, M1, true,
                c -> { c.bf("Plains", 2, c.ai); c.bf("Forest", 2, c.ai); c.bf("Llanowar Elves", c.ai); c.bf("Manabarbs", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Savannah Lions", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 1 ? null : "{W} should cost 1 life, the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-estimate-elves", "The Elves pay the {G} of Grizzly Bears, one land pays the {1}", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Llanowar Elves", c.ai); c.bf("Manabarbs", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 1 ? null : "the Elves and one land pay it (1 life), the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-estimate-multi", "A land that makes two mana is tapped once: Mind Stone ({2}) from Ancient Tomb costs Manabarbs 1 plus its own 2", Fid.APPROX, M1, true,
                c -> { c.bf("Ancient Tomb", c.ai); c.bf("Manabarbs", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Mind Stone", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 3 ? null : "Mind Stone ({2}) from the Tomb is one tap: Manabarbs 1 + the Tomb's own 2, the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-pain-colourless", "Karplusan Forest tapped for its colourless ability costs only Manabarbs (1), Mountain 1 more", Fid.APPROX, M1, true,
                c -> { c.bf("Karplusan Forest", c.ai); c.bf("Mountain", c.ai); c.bf("Manabarbs", c.opp); c.hand("Mind Stone", c.ai); },
                c -> {
                    final SpellAbility sa = c.hand("Mind Stone", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 2 ? null : "the colourless ability should cost 1 and the Mountain 1, the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-pain-coloured", "The {G} of Karplusan Forest also hurts (1 + 1), Mountain adds 1", Fid.APPROX, M1, true,
                c -> { c.bf("Karplusan Forest", c.ai); c.bf("Mountain", c.ai); c.bf("Manabarbs", c.opp); c.hand("Grizzly Bears", c.ai); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 3 ? null : "Forest {G} (2) + Mountain (1) expected, the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-pain-river", "Underground River's coloured ability counts its own damage under Manabarbs", Fid.APPROX, M1, true,
                c -> { c.bf("Underground River", c.ai); c.bf("Island", c.ai); c.bf("Manabarbs", c.opp); c.hand("Mind Stone", c.ai); },
                c -> {
                    final SpellAbility sa = c.hand("Mind Stone", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int d = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return d == 2 ? null : "colourless River (1) + Island (1) expected, the estimate is " + d;
                }));
        l.add(new Scenario("manabarbs-gorm-needs-two", "Gorm the Great must be blocked by two creatures: one blocker cannot stop it", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(6, null); c.bf("Forest", 2, c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp);
                       c.bf("Glorious Anthem", 2, c.opp); c.bf("Gorm the Great", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the lone Bears was counted as blocking Gorm";
                }));
        l.add(new Scenario("manabarbs-dictate-threat", "Dictate of the Twin Gods doubles the next attack: a 3/3 hits for 6", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(10, null); c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp);
                       c.bf("Dictate of the Twin Gods", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the doubled combat damage was not in the reserve";
                }));
        l.add(new Scenario("manabarbs-doomed-blocker", "A Grizzly Bears with lethal damage marked does not block next turn", Fid.APPROX, "MAIN2", true,
                c -> { c.ai.setLife(6, null); c.bf("Forest", 2, c.ai); c.bf("Grizzly Bears", c.ai).setDamage(2); c.hand("Grizzly Bears", c.ai);
                       c.bf("Manabarbs", c.opp); c.bf("Craw Wurm", c.opp); },
                c -> {
                    SpellAbility sa = null;
                    for (final Card k : c.ai.getCardsIn(ZoneType.Hand)) { sa = k.getFirstSpellAbility(); }
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the dying Bears was counted as a blocker";
                }));
        l.add(new Scenario("manabarbs-indestructible-blocker", "A Darksteel Myr with lethal damage marked survives and still blocks the Hill Giant", Fid.APPROX, "MAIN2", true,
                c -> { c.ai.setLife(5, null); c.bf("Mountain", c.ai); c.bf("Plains", c.ai); c.bf("Darksteel Myr", c.ai).setDamage(1); c.hand("Boros Swiftblade", c.ai);
                       c.bf("Manabarbs", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Boros Swiftblade", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? "the indestructible Myr was dropped as a blocker" : null;
                }));
        l.add(new Scenario("manabarbs-doran-threat", "Under Doran, the Siege Tower a 0/5 deals 5 in combat: it is a threat", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(7, null); c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp); c.bf("Doran, the Siege Tower", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "Doran assigns damage by toughness: 5 next turn was not in the reserve";
                }));
        l.add(new Scenario("manabarbs-arcades-defender", "Under Arcades a Wall of Stone attacks for its toughness", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(8, null); c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp); c.bf("Arcades, the Strategist", c.opp); c.bf("Wall of Stone", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the defender that Arcades lets attack was left out of the reserve";
                }));
        l.add(new Scenario("manabarbs-regenerating-victim", "A Hill Giant with a regeneration shield is not removed by a Lightning Bolt: it still attacks", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(4, null); c.bf("Mountain", c.ai); c.hand("Lightning Bolt", c.ai); c.bf("Manabarbs", c.opp); c.bf("Hill Giant", c.opp).incShieldCount(); },
                c -> {
                    final SpellAbility sa = c.hand("Lightning Bolt", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Hill Giant")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the shielded Hill Giant was counted as removed by the Bolt";
                }));
        l.add(new Scenario("deathtouch-plan-stays-consistent", "The Craw Wurm that stays home does not send a Grizzly Bears alone into the blocker meant for it", Fid.APPROX, M1, true,
                c -> { c.bf("Craw Wurm", c.ai); c.bf("Grizzly Bears", c.ai); c.bf("Typhoid Rats", c.opp); },
                c -> {
                    final boolean bears = c.attackers().stream().anyMatch(a -> a.getName().equals("Grizzly Bears"));
                    final boolean wurm = c.attackers().stream().anyMatch(a -> a.getName().equals("Craw Wurm"));
                    return bears && !wurm ? "the Bears went in alone against the blocker that was counted as busy with the Wurm" : null;
                }));
        // ---- pumps that cannot matter: Chameleon Colossus pumped while it was tapped when the opponent attacked
        final Setup tappedColossus = c -> {
            c.bf("Forest", 4, c.ai); c.bf("Chameleon Colossus", c.ai).setTapped(true);
            final Card att = c.bf("Hill Giant", c.opp);
            final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(noPlay("pump-tapped-after-blocks", "A tapped Colossus is no blocker: no +4/+4 once the blockers are declared", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Chameleon Colossus", tappedColossus));
        l.add(noPlay("pump-tapped-before-blocks", "A tapped Colossus cannot block: no +4/+4 when the attackers are declared", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", false,
                "Chameleon Colossus", tappedColossus));
        l.add(noPlay("pump-after-combat", "No +4/+4 in the second main phase: nothing is left to fight", Fid.APPROX, "MAIN2", true, "Chameleon Colossus",
                c -> { c.bf("Forest", 4, c.ai); c.bf("Chameleon Colossus", c.ai); c.bf("Hill Giant", c.opp); }));
        l.add(noPlay("pump-spell-tapped", "Giant Growth on a tapped creature of the AI's, with a Hill Giant attacking", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false, "Giant Growth",
                c -> { tappedColossus.run(c); c.hand("Giant Growth", c.ai); }));
        l.add(play("pump-blocker-in-combat", "The Colossus that blocks a Craw Wurm is pumped: that is the fight", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false, "Chameleon Colossus",
                c -> {
                    c.bf("Forest", 4, c.ai);
                    final Card blocker = c.bf("Chameleon Colossus", c.ai);
                    final Card att = c.bf("Craw Wurm", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(play("sakura-tribe-elder-after-block", "Sakura-Tribe Elder cashes in after blocking a creature that would kill it", Fid.APPROX,
                "COMBAT_DECLARE_BLOCKERS", false, "Sakura-Tribe Elder", c -> {
                    c.bf("Forest", c.ai); c.lib("Forest", c.ai);
                    final Card blocker = c.bf("Sakura-Tribe Elder", c.ai);
                    final Card attacker = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp);
                    combat.addAttacker(attacker, c.ai);
                    combat.addBlocker(attacker, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("sakura-tribe-elder-keeps-profitable-trade", "Sakura-Tribe Elder does not sacrifice itself when its block also kills the attacker", Fid.APPROX,
                "COMBAT_DECLARE_BLOCKERS", false, "Sakura-Tribe Elder", c -> {
                    c.lib("Forest", c.ai);
                    final Card blocker = c.bf("Sakura-Tribe Elder", c.ai);
                    final Card attacker = c.bf("Sakura-Tribe Elder", c.opp);
                    final Combat combat = new Combat(c.opp);
                    combat.addAttacker(attacker, c.ai);
                    combat.addBlocker(attacker, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("sakura-tribe-elder-stays-to-stop-trample", "Sakura-Tribe Elder blocks a trampler instead of sacrificing itself and letting all damage through", Fid.APPROX,
                "COMBAT_DECLARE_BLOCKERS", false, "Sakura-Tribe Elder", c -> {
                    c.lib("Forest", c.ai);
                    final Card blocker = c.bf("Sakura-Tribe Elder", c.ai);
                    final Card attacker = c.bf("Colossal Dreadmaw", c.opp);
                    final Combat combat = new Combat(c.opp);
                    combat.addAttacker(attacker, c.ai);
                    combat.addBlocker(attacker, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(play("spore-frog-after-block-trample", "Spore Frog uses its combat-damage prevention after blocking a trampler", Fid.APPROX,
                "COMBAT_DECLARE_BLOCKERS", false, "Spore Frog", c -> {
                    c.ai.setLife(4, null);
                    final Card blocker = c.bf("Spore Frog", c.ai);
                    final Card attacker = c.bf("Colossal Dreadmaw", c.opp);
                    final Combat combat = new Combat(c.opp);
                    combat.addAttacker(attacker, c.ai);
                    combat.addBlocker(attacker, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("goblin-legionnaire-insufficient-trample-prevention", "Goblin Legionnaire does not sacrifice itself for a two-damage shield that still leaves lethal trample damage", Fid.APPROX,
                "COMBAT_DECLARE_BLOCKERS", false, "Goblin Legionnaire", c -> {
                    c.ai.setLife(2, null);
                    c.bf("Plains", c.ai); // can pay the white activation cost
                    final Card blocker = c.bf("Goblin Legionnaire", c.ai);
                    final Card attacker = c.bf("Colossal Dreadmaw", c.opp);
                    final Combat combat = new Combat(c.opp);
                    combat.addAttacker(attacker, c.ai);
                    combat.addBlocker(attacker, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        // ---- bounce: a tempo play, not for the creature that gives them another trigger
        l.add(noPlay("bounce-vendilion-clique", "Unsummon on a Vendilion Clique: they cast it again and look at another hand", Fid.APPROX, M1, true, "Unsummon",
                c -> { c.bf("Island", 2, c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Unsummon", c.ai); c.bf("Vendilion Clique", c.opp); }));
        l.add(noPlay("bounce-mulldrifter", "Unsummon on a Mulldrifter: they draw two cards again", Fid.APPROX, M1, true, "Unsummon",
                c -> { c.bf("Island", 2, c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Unsummon", c.ai); c.bf("Mulldrifter", c.opp); }));
        l.add(noPlay("bounce-snapcaster", "Unsummon on a Snapcaster Mage: flash and a trigger", Fid.APPROX, M1, true, "Unsummon",
                c -> { c.bf("Island", 2, c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Unsummon", c.ai); c.bf("Snapcaster Mage", c.opp); }));
        l.add(play("bounce-saves-blocker", "Unsummon on the Craw Wurm that would kill the blocker", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false, "Unsummon",
                c -> {
                    c.bf("Island", c.ai); c.hand("Unsummon", c.ai);
                    final Card blocker = c.bf("Grizzly Bears", c.ai);
                    final Card att = c.bf("Craw Wurm", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(new Scenario("bounce-value", "Which permanents of the opponent a bounce is worth a card for", Fid.APPROX, M1, true,
                c -> {
                    c.bf("Vendilion Clique", c.opp); c.bf("Mulldrifter", c.opp); c.bf("Snapcaster Mage", c.opp);
                    c.bf("Llanowar Elves", c.opp); c.bf("Craw Wurm", c.opp); c.bf("Forest", c.opp); c.bf("Gaea's Revenge", c.opp);
                    c.counters(c.bf("Grizzly Bears", c.opp), CounterEnumType.P1P1, 2);
                },
                c -> {
                    final StringBuilder bad = new StringBuilder();
                    final java.util.Map<String, Boolean> expected = new java.util.HashMap<>();
                    expected.put("Vendilion Clique", false);   // flash and a trigger
                    expected.put("Mulldrifter", false);        // draws two again
                    expected.put("Snapcaster Mage", false);    // flash and a trigger
                    expected.put("Llanowar Elves", false);     // costs one mana to cast again
                    expected.put("Craw Wurm", true);           // six mana for a vanilla body
                    expected.put("Gaea's Revenge", true);      // flash alone is no reason: five mana to cast again, no trigger
                    expected.put("Forest", true);              // a land drop
                    expected.put("Grizzly Bears", true);       // loses its counters
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        final Boolean want = expected.get(k.getName());
                        if (want != null && BounceValue.worthBouncing(c.ai, k) != want) { bad.append(k.getName()).append(" expected ").append(want).append("; "); }
                    }
                    return bad.length() == 0 ? null : bad.toString();
                }));
        // ---- counterspells: not for a bluff, for what matters
        final Setup baitOnStack = c -> {
            c.bf("Island", 2, c.ai); c.hand("Counterspell", c.ai);
            final Card tapped = c.bf("Hill Giant", c.opp); tapped.setTapped(true);
            c.bf("Mountain", c.opp);
            c.stack(c.opp, "Giant Growth", tapped);
        };
        l.add(noPlay("counter-bait-pump", "Giant Growth on a tapped creature outside combat is a bluff: no Counterspell", Fid.APPROX, M1, false, "Counterspell", baitOnStack));
        l.add(play("counter-creature", "A Craw Wurm on the stack is worth the Counterspell", Fid.APPROX, M1, false, "Counterspell",
                c -> { c.bf("Island", 2, c.ai); c.hand("Counterspell", c.ai); c.stack(c.opp, "Craw Wurm"); }));
        l.add(new Scenario("counter-pump-in-combat", "A Giant Growth on the creature that is blocked is no bluff, on the tapped one it is", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> {
                    c.bf("Island", 2, c.ai); c.hand("Counterspell", c.ai);
                    final Card att = c.bf("Hill Giant", c.opp);
                    final Card idle = c.bf("Grizzly Bears", c.opp); idle.setTapped(true);
                    final Card blocker = c.bf("Craw Wurm", c.ai);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                    c.stack(c.opp, "Giant Growth", att);
                },
                c -> {
                    final SpellAbility pump = c.game.getStack().peekAbility();
                    if (PlayRelevance.isBait(c.ai, pump)) { return "the pump on the attacker in combat was taken for a bluff"; }
                    final Card idle = c.opp.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Grizzly Bears")).findFirst().get();
                    return PlayRelevance.combatRelevant(idle) ? "a tapped creature that is not in the fight counted as part of it" : null;
                }));
        l.add(new Scenario("shock-pays-for-curve", "Plenty of life and a four-drop that needs the untapped land: pay", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == true ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-dual-source", "A dual land only pays for one coloured shard", Fid.EXACT, M1, true,
                c -> { c.bf("Underground Sea", c.ai); c.hand("Baleful Strix", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? "the AI counted Underground Sea as both a blue and a black mana source" : null;
                }));
        l.add(new Scenario("shock-phyrexian-life-budget", "Do not spend life on a shockland if it is needed for a Phyrexian cost", Fid.EXACT, M1, true,
                c -> { c.ai.setLife(12, null); c.bf("Mountain", 3, c.ai); c.hand("Phyrexian Metamorph", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? "the AI paid 2 life for the Foundry and left no life to pay Phyrexian Metamorph's blue symbol" : null;
                }));
        l.add(new Scenario("shock-karns-sylex-phyrexian", "Do not use the life alternative for Phyrexian Metamorph under Karn's Sylex", Fid.EXACT, M1, true,
                c -> { c.ai.setLife(14, null); c.bf("Mountain", 2, c.ai); c.bf("Karn's Sylex", c.ai); c.bf("Grizzly Bears", c.opp);
                       c.hand("Phyrexian Metamorph", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? "the AI treated Metamorph's Phyrexian blue symbol as payable with life despite Karn's Sylex" : null;
                }));
        l.add(new Scenario("shock-low-life", "At 11 life the Foundry enters tapped, whatever is in hand", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(11, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-just-enough-life", "At 12 life the four-drop is worth it (10 is the floor)", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(12, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == true ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-burn-in-hand", "Lightning Helix is held for their turn: no life for an untapped land", Fid.APPROX, M1, true,
                c -> { c.bf("Plains", c.ai); c.hand("Lightning Helix", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-flash-creature", "A flash creature is held too: Vendilion Clique does not make it worth the life", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 2, c.ai); c.hand("Vendilion Clique", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-threatened", "Their Craw Wurm and Hill Giant hit for nine: stay above 14, so 15 life does not pay", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(15, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); c.bf("Craw Wurm", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-threat-handled", "The same board at 20 life pays", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(20, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); c.bf("Craw Wurm", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == true ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-one-drop-healthy", "With 20 life even a one-drop on curve is worth it", Fid.APPROX, M1, true,
                c -> { c.hand("Grim Lavamancer", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == true ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-one-drop-low", "With 13 life a one-drop is not worth 2 life", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(13, null); c.hand("Grim Lavamancer", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-nothing-to-cast", "No card that needs the land this turn: tapped", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("shock-aura-no-creature", "An Aura with nothing to enchant is no reason to pay", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 2, c.ai); c.hand("Rancor", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays == false ? null : "the AI " + (pays ? "paid" : "did not pay") + " 2 life for the Sacred Foundry (life " + c.ai.getLife() + ")";
                }));
        // 12986: Malevolent Rumble's Spawn pays for a second spell, so the third Overgrown Tomb is worth the two life
        l.add(new Scenario("12986-rumble-shockland", "Two Tombs and Malevolent Rumble with Emperor of Bones in hand: the third Tomb is worth two life", Fid.APPROX, M1, true,
                c -> { c.bf("Overgrown Tomb", 2, c.ai); c.hand("Malevolent Rumble", c.ai); c.hand("Emperor of Bones", c.ai); c.hand("Overgrown Tomb", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Overgrown Tomb")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? null : "the AI let the third Overgrown Tomb enter tapped (life " + c.ai.getLife() + ")";
                }));
        l.add(play("12986-rumble-first", "With three untapped lands, Malevolent Rumble is cast before Emperor of Bones: the Spawn pays for it", Fid.APPROX,
                M1, true, "Malevolent Rumble", c -> {
                    c.bf("Overgrown Tomb", 3, c.ai); c.hand("Malevolent Rumble", c.ai); c.hand("Emperor of Bones", c.ai);
                    c.lib("Forest", 4, c.ai);
                }));
        // 12982: a spell that only aims at the AI's own permanents is no reason to pay for the land
        l.add(new Scenario("12982-ephemerate-fountain", "Hallowed Fountain and Ephemerate on turn one with no creature: the Fountain enters tapped", Fid.APPROX, M1, true,
                c -> { c.hand("Ephemerate", c.ai); c.hand("Hallowed Fountain", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Hallowed Fountain")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? "the AI paid 2 life for a Hallowed Fountain with only Ephemerate and no creature" : null;
                }));
        // 12984: a Plains and green one-drops
        l.add(new Scenario("12984-wrong-colour-land", "A Plains and green one-drops is no keep", Fid.APPROX, M1, true,
                c -> { for (int i = 0; i < 20; i++) { c.lib("Forest", c.ai); c.lib("Llanowar Elves", c.ai); } },
                c -> {
                    for (final String n : new String[] { "Plains", "Llanowar Elves", "Llanowar Elves", "Elvish Mystic", "Elvish Mystic", "Fyndhorn Elves" }) { c.hand(n, c.ai); }
                    final DeckPlan plan = DeckPlan.forSetting("auto", c.ai);
                    return OpeningHand.keep(c.ai, c.ai.getCardsIn(ZoneType.Hand), 0, true, plan, 4) ? "kept a Plains with green one-drops" : null;
                }));
        l.add(new Scenario("shock-colour-sources", "Swamp, Forest and a Sacred Foundry for Geralf's Messenger: one black source is not BBB", Fid.APPROX, M1, true,
                c -> { c.bf("Swamp", c.ai); c.bf("Forest", c.ai); c.hand("Geralf's Messenger", c.ai); c.hand("Sacred Foundry", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? "paid 2 life for a Sacred Foundry that cannot make black for Geralf's Messenger" : null;
                }));
        l.add(new Scenario("shock-colour-sources-enough", "Two Swamps and an Overgrown Tomb for Geralf's Messenger: the third black source is worth the life", Fid.APPROX, M1, true,
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Geralf's Messenger", c.ai); c.hand("Overgrown Tomb", c.ai); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Overgrown Tomb")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    final boolean pays = new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null);
                    return pays ? null : "did not pay 2 life for the third black source (life " + c.ai.getLife() + ")";
                }));
        l.add(new Scenario("12984-hybrid-keep", "Three Plains and Kitchen Finks: the hybrid cost is white, a keep", Fid.APPROX, M1, true,
                c -> { for (int i = 0; i < 20; i++) { c.lib("Plains", c.ai); c.lib("Kitchen Finks", c.ai); } },
                c -> {
                    for (final String n : new String[] { "Plains", "Plains", "Plains", "Kitchen Finks", "Kitchen Finks", "Plains", "Kitchen Finks" }) { c.hand(n, c.ai); }
                    final DeckPlan plan = DeckPlan.forSetting("auto", c.ai);
                    return OpeningHand.keep(c.ai, c.ai.getCardsIn(ZoneType.Hand), 0, true, plan, 4) ? null : "sent back Plains and Kitchen Finks, a hybrid cost that white pays";
                }));
        // ---- which creatures a pump can still matter to (before / after the attackers and the blockers are declared)
        l.add(new Scenario("relevance-attacker-before-declaring", "Own turn, declare attackers, none declared yet: the untapped Colossus can still attack", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Chameleon Colossus", c.ai); c.game.getPhaseHandler().setCombat(new Combat(c.ai)); },
                c -> PlayRelevance.combatRelevant(c.ai.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "an untapped creature that can attack was ruled out"));
        l.add(new Scenario("relevance-blocker-before-blocks", "Their declare attackers: the untapped Colossus can still block", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", false,
                c -> { c.bf("Chameleon Colossus", c.ai); final Card att = c.bf("Craw Wurm", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> PlayRelevance.combatRelevant(c.ai.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "an untapped creature that can block was ruled out"));
        l.add(new Scenario("relevance-tapped-before-blocks", "Their declare attackers: the tapped Colossus cannot block", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", false,
                c -> { c.bf("Chameleon Colossus", c.ai).setTapped(true); final Card att = c.bf("Craw Wurm", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> PlayRelevance.combatRelevant(c.ai.getCardsIn(ZoneType.Battlefield).get(0)) ? "a tapped creature counted as a blocker" : null));
        l.add(new Scenario("relevance-blockers-declared", "After the blocks the one that blocks is in the fight, the one that stays out is not", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { final Card blocker = c.bf("Craw Wurm", c.ai); c.bf("Grizzly Bears", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker); c.game.getPhaseHandler().setCombat(combat); },
                c -> {
                    for (final Card k : c.ai.getCardsIn(ZoneType.Battlefield)) {
                        final boolean want = k.getName().equals("Craw Wurm");
                        if (PlayRelevance.combatRelevant(k) != want) { return k.getName() + (want ? " blocks and was ruled out" : " does not block and was counted"); }
                    }
                    return null;
                }));
        l.add(new Scenario("relevance-main-phase", "Before combat an untapped creature can matter, a tapped one cannot", Fid.APPROX, M1, true,
                c -> { c.bf("Chameleon Colossus", c.ai); c.bf("Grizzly Bears", c.ai).setTapped(true); },
                c -> {
                    for (final Card k : c.ai.getCardsIn(ZoneType.Battlefield)) {
                        final boolean want = k.getName().equals("Chameleon Colossus");
                        if (PlayRelevance.combatRelevant(k) != want) { return k.getName() + " was judged " + !want; }
                    }
                    return null;
                }));
        l.add(new Scenario("pump-in-response-to-burn", "A pump in response to burn is not outside combat: the toughness can save the creature", Fid.APPROX, M1, true,
                c -> { c.bf("Forest", c.ai); final Card giant = c.bf("Hill Giant", c.ai); giant.setTapped(true); c.hand("Giant Growth", c.ai); c.stack(c.opp, "Lightning Bolt", giant); },
                c -> {
                    final SpellAbility sa = c.hand("Giant Growth", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return PlayRelevance.pointlessPump(c.ai, sa) == null ? null : "a pump with burn on the stack was thrown out";
                }));
        l.add(new Scenario("counter-pump-in-response", "The opponent's pump in response to burn is not a bluff", Fid.APPROX, M1, false,
                c -> { c.bf("Island", 2, c.ai); c.hand("Counterspell", c.ai); final Card g = c.bf("Hill Giant", c.opp); g.setTapped(true);
                       c.stack(c.opp, "Lightning Bolt", c.ai); c.stack(c.opp, "Giant Growth", g); },
                c -> PlayRelevance.isBait(c.ai, c.game.getStack().peekAbility()) ? "a pump on top of a spell was taken for a bluff" : null));
        // ---- bounce: the attacker that is lethal, and the one that is not
        l.add(new Scenario("bounce-lethal-attacker", "A Vendilion Clique that is lethal is worth bouncing: without it the AI lives", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setLife(3, null); final Card clique = c.bf("Vendilion Clique", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(clique, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.worthBouncing(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "the lethal Clique was refused"));
        l.add(new Scenario("bounce-irrelevant-attacker", "A Vendilion Clique beside a Craw Wurm that is lethal anyway: bouncing it saves nothing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setLife(5, null); final Card clique = c.bf("Vendilion Clique", c.opp); final Card wurm = c.bf("Craw Wurm", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(clique, c.ai); combat.addAttacker(wurm, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> {
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Vendilion Clique") && BounceValue.worthBouncing(c.ai, k)) { return "bounced the Clique although the Wurm kills anyway"; }
                    }
                    return null;
                }));
        l.add(aimed("bounce-prefers-craw-wurm", "Mulldrifter and Craw Wurm: the Wurm is the one to send back", Fid.APPROX, M1, true, "Unsummon", "Craw Wurm",
                c -> { c.bf("Island", 2, c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Unsummon", c.ai); c.bf("Mulldrifter", c.opp); c.bf("Craw Wurm", c.opp); }));
        // ---- which spell on the stack is worth the counterspell
        l.add(new Scenario("counter-value-removal-aim", "A Terminate on the AI's creature outranks a Terminate on their own", Fid.APPROX, M1, false,
                c -> { c.bf("Grizzly Bears", c.ai); c.bf("Hill Giant", c.opp); c.hand("Terminate", c.opp); c.hand("Terminate", c.opp); },
                c -> {
                    final Card mine = c.ai.getCardsIn(ZoneType.Battlefield).get(0), theirs = c.opp.getCardsIn(ZoneType.Battlefield).get(0);
                    final java.util.List<Card> terminates = new java.util.ArrayList<>(c.opp.getCardsIn(ZoneType.Hand));
                    final SpellAbility onMine = terminates.get(0).getFirstSpellAbility(), onTheirs = terminates.get(1).getFirstSpellAbility();
                    onMine.setActivatingPlayer(c.opp); onMine.getTargets().add(mine);
                    onTheirs.setActivatingPlayer(c.opp); onTheirs.getTargets().add(theirs);
                    return PlayRelevance.stackValue(c.ai, onMine) > PlayRelevance.stackValue(c.ai, onTheirs) ? null
                            : "removal on their own creature was valued like removal on the AI's";
                }));
        // ---- shocklands: what the opponent can really hit for next turn
        l.add(new Scenario("shock-tapped-attackers-untap", "Their tapped Craw Wurm and Hill Giant untap before they attack: 15 life does not pay", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(15, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai);
                       c.bf("Craw Wurm", c.opp).setTapped(true); c.bf("Hill Giant", c.opp).setTapped(true); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    return new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null)
                            ? "paid with nine power ready to untap against 15 life" : null;
                }));
        l.add(new Scenario("shock-blockers-cover", "Two untapped Grizzly Bears stop the Wurm and the Giant: 12 life pays", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(12, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai);
                       c.bf("Grizzly Bears", 2, c.ai); c.bf("Craw Wurm", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    return new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null)
                            ? null : "refused although the blockers stop everything";
                }));
        l.add(new Scenario("shock-defender-ignored", "A Wall of Stone cannot attack: it adds nothing to the threat", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(12, null); c.bf("Mountain", 3, c.ai); c.hand("Hill Giant", c.ai); c.hand("Sacred Foundry", c.ai); c.bf("Wall of Stone", c.opp); },
                c -> {
                    final Card land = c.ai.getCardsIn(ZoneType.Hand).stream().filter(k -> k.getName().equals("Sacred Foundry")).findFirst().get();
                    final SpellAbility re = land.getReplacementEffects().get(0).ensureAbility();
                    re.setActivatingPlayer(c.ai);
                    return new forge.ai.ability.TapAi().willPayUnlessCost(c.ai, re, AbilityUtils.calculateUnlessCost(re, "PayLife<2>", false), false, null)
                            ? null : "a defender was counted as an attacker";
                }));
        // ---- who can still take part in a fight: summoning sickness and defender are no part of it
        l.add(new Scenario("relevance-sick-and-defender", "Before combat on their own turn a summoning-sick creature and a Wall of Stone cannot attack: a pump is wasted on them", Fid.APPROX, M1, true,
                c -> { c.bf("Chameleon Colossus", c.ai).setSickness(true); c.bf("Wall of Stone", c.ai); },
                c -> {
                    for (final Card k : c.ai.getCardsIn(ZoneType.Battlefield)) {
                        if (PlayRelevance.combatRelevant(k)) { return k.getName() + " cannot attack but was counted"; }
                    }
                    return null;
                }));
        l.add(new Scenario("pump-keyword-only", "Arnjlot's Ascent grants flying and no P/T: it is not a combat pump to be thrown out", Fid.APPROX, "MAIN2", true,
                c -> { c.bf("Arnjlot's Ascent", c.ai); c.bf("Hill Giant", c.ai).setTapped(true); },
                c -> {
                    final Card ascent = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Arnjlot's Ascent")).findFirst().get();
                    final SpellAbility sa = ascent.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
                    sa.setActivatingPlayer(c.ai);
                    return PlayRelevance.pointlessPump(c.ai, sa) == null ? null : "a keyword grant was classified as a +X/+X pump";
                }));
        // ---- a blocked trampler can still be lethal; a blocked one without trample cannot
        l.add(new Scenario("bounce-blocked-trampler", "Ghalta (12/12 trample) blocked by a Grizzly Bears still kills the AI at 5: worth bouncing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setLife(5, null); final Card bears = c.bf("Grizzly Bears", c.ai); final Card ghalta = c.bf("Ghalta, Stampede Tyrant", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(ghalta, c.ai); combat.addBlocker(ghalta, bears); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.worthBouncing(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "the lethal blocked trampler was refused"));
        l.add(new Scenario("bounce-blocked-no-trample", "A Crested Herdcaller (trample) blocked by a Craw Wurm deals nothing through: with an ETB it is not worth bouncing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setLife(5, null); final Card wurm = c.bf("Craw Wurm", c.ai); final Card herd = c.bf("Crested Herdcaller", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(herd, c.ai); combat.addBlocker(herd, wurm); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.worthBouncing(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? "bounced an attacker that the block stops completely" : null));
        l.add(new Scenario("bounce-lethal-infect", "An unblocked infect attacker that takes the AI from 9 to 10 poison is worth bouncing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setPoisonCounters(9, c.opp); final Card elf = c.bf("Glistener Elf", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(elf, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.savesTheCombat(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "the lethal poison attacker was not recognized"));
        l.add(new Scenario("bounce-poison-danger-floor", "An infect attack from 7 to 8 poison crosses the AI's nonlethal danger floor", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setPoisonCounters(7, c.opp); final Card elf = c.bf("Glistener Elf", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(elf, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.savesTheCombat(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "the nonlethal poison danger was not recognized"));
        l.add(new Scenario("bounce-blocked-infect-trampler", "Blightsteel Colossus still deals lethal poison through a Grizzly Bears blocker", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setPoisonCounters(9, c.opp); final Card blocker = c.bf("Grizzly Bears", c.ai); final Card colossus = c.bf("Blightsteel Colossus", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(colossus, c.ai); combat.addBlocker(colossus, blocker); combat.setBlocked(colossus, true);
                       c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.savesTheCombat(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "the blocked infect trampler's poison damage was missed"));
        l.add(new Scenario("bounce-double-strike-lethal", "An unblocked double-striker that deals lethal damage is worth bouncing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> { c.ai.setLife(2, null); final Card blade = c.bf("Boros Swiftblade", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(blade, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> BounceValue.savesTheCombat(c.ai, c.opp.getCardsIn(ZoneType.Battlefield).get(0)) ? null : "double-strike damage was not included in the combat forecast"));
        // ---- Zagorka, Mother of Sanctum: the land is not worth an attack that never comes
        final java.util.function.Function<Ctx, SpellAbility> sanctum = c -> {
            final Card z = c.bf("Zagorka, Mother of Sanctum", c.opp);
            return z.getTriggers().get(0).ensureAbility().getAdditionalAbility("RepeatSubAbility");
        };
        l.add(new Scenario("zagorka-declines-with-attackers", "With a Grizzly Bears that can attack next turn the AI does not take the Sanctum", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Forest", 3, c.ai); c.bf("Grizzly Bears", c.ai); },
                c -> new forge.ai.ability.TokenAi().confirmAction(c.ai, sanctum.apply(c), null, "", null) ? "took the land and gave up its attack" : null));
        l.add(new Scenario("zagorka-accepts-without-attackers", "With nothing that could attack the land is free", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Forest", 3, c.ai); },
                c -> new forge.ai.ability.TokenAi().confirmAction(c.ai, sanctum.apply(c), null, "", null) ? null : "refused a free land with no attacker"));
        l.add(new Scenario("zagorka-declines-flooded", "Seven lands already: another adds nothing, no creature or not", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Forest", 7, c.ai); },
                c -> new forge.ai.ability.TokenAi().confirmAction(c.ai, sanctum.apply(c), null, "", null) ? "took a land at seven" : null));
        l.add(new Scenario("zagorka-controller-takes-it", "Her controller always takes the Sanctum", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Forest", 7, c.opp); c.bf("Grizzly Bears", c.opp); },
                c -> new forge.ai.ability.TokenAi().confirmAction(c.opp, sanctum.apply(c), null, "", null) ? null : "Zagorka's controller refused"));
        l.add(new Scenario("temple-of-the-false-god-last", "Turn one with the Temple and four Mountains in hand: the Temple is not the land drop", Fid.APPROX, M1, true,
                c -> { c.hand("Temple of the False God", c.ai); c.hand("Mountain", c.ai); c.hand("Mountain", c.ai); c.hand("Mountain", c.ai);
                       c.hand("Mountain", c.ai); c.hand("Goblin Guide", c.ai); },
                c -> {
                    final List<Card> drops = c.landPlays();
                    return !drops.isEmpty() && drops.get(0).getName().equals("Temple of the False God") ? "played the Temple on turn one" : null;
                }));
        l.add(new Scenario("birthing-ritual-legend-copy", "Phelia in play and another Phelia among the seven: the copy is no reward (legend rule), a different creature is", Fid.APPROX, "END_OF_TURN", true,
                c -> { c.bf("Phelia, Exuberant Shepherd", c.ai); c.lib("Phelia, Exuberant Shepherd", c.ai); c.lib("Grizzly Bears", c.ai); },
                c -> {
                    final Card phelia = c.ai.getCardsIn(ZoneType.Library).stream().filter(k -> k.getName().startsWith("Phelia")).findFirst().get();
                    final Card bears = c.ai.getCardsIn(ZoneType.Library).stream().filter(k -> k.getName().equals("Grizzly Bears")).findFirst().get();
                    final Card inPlay = c.ai.getCardsIn(ZoneType.Battlefield).get(0);
                    if (!forge.ai.ability.DigAi.legendIsAlreadyHere(c.ai, phelia, null)) { return "a second Phelia was thought fine"; }
                    if (forge.ai.ability.DigAi.legendIsAlreadyHere(c.ai, phelia, inPlay)) { return "the Phelia that is sacrificed still counted"; }
                    return forge.ai.ability.DigAi.legendIsAlreadyHere(c.ai, bears, null) ? "a plain creature was called a legend copy" : null;
                }));
        l.add(play("forcefield-unblocked", "Forcefield: one unblocked Hill Giant is a reason to pay one mana", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false, "Forcefield",
                c -> { c.bf("Mountain", 5, c.ai); c.bf("Forcefield", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                       final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.setBlocked(att, false); c.game.getPhaseHandler().setCombat(combat); }));
        l.add(new Scenario("attack-deal-nothing-to-attack-with", "The Second Doctor / Advokist: nothing that could attack, so take the card or the counters", Fid.APPROX, "END_OF_TURN", false,
                c -> { },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) == true ? null : "the deal was judged wrongly (should be taken)"));
        l.add(new Scenario("attack-deal-small-attack", "One Grizzly Bears against 20 life is a small attack: take the card", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Grizzly Bears", c.ai); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) == true ? null : "the deal was judged wrongly (should be taken)"));
        l.add(new Scenario("attack-deal-real-attack", "Three Hill Giants against 20 life and no blockers is the plan: refuse", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Hill Giant", 3, c.ai); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) == false ? null : "the deal was judged wrongly (should be refused)"));
        l.add(new Scenario("attack-deal-blocked-anyway", "Three Hill Giants against three blockers get nowhere: take the card", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Hill Giant", 3, c.ai); c.bf("Hill Giant", 3, c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) == true ? null : "the deal was judged wrongly (should be taken)"));
        l.add(new Scenario("attack-deal-lethal", "Three Grizzly Bears against 5 life is lethal: refuse", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(5, null); c.bf("Grizzly Bears", 3, c.ai); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) == false ? null : "the deal was judged wrongly (should be refused)"));
        l.add(new Scenario("attack-deal-blocked-lethal", "Three 2/2 attackers against three 3/3 blockers: the nominal six damage is stopped", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(5, null); c.bf("Grizzly Bears", 3, c.ai); c.bf("Hill Giant", 3, c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? null : "suitable blockers stop the nominally lethal attack"));
        l.add(new Scenario("attack-deal-incompatible-blockers", "Three Wind Drakes against three ground blockers: the blockers cannot stop the lethal attack", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(5, null); c.bf("Wind Drake", 3, c.ai); c.bf("Hill Giant", 3, c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? "ground blockers were incorrectly counted against flying attackers" : null));
        l.add(new Scenario("attack-deal-double-strike-lethal", "A double striker deals both combat-damage steps", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(3, null); c.bf("Mirran Crusader", c.ai); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? "double-strike damage was counted only once" : null));
        l.add(new Scenario("attack-deal-trample-lethal", "A trampler gets lethal excess damage through its blocker", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(3, null); c.bf("Colossal Dreadmaw", c.ai); c.bf("Grizzly Bears", c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? "trample damage through the blocker was ignored" : null));
        l.add(new Scenario("attack-deal-trample-extra-blockers", "The defender may use multiple blockers to stop a trampler", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(5, null); c.bf("Colossal Dreadmaw", c.ai); c.bf("Grizzly Bears", 3, c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? null : "additional legal blockers were not assigned to stop trample"));
        l.add(new Scenario("attack-deal-assign-as-unblocked", "An attacker that assigns damage as unblocked still hits the player when blocked", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.opp.setLife(5, null); c.bf("Thorn Elemental", c.ai); c.bf("Grizzly Bears", c.opp); },
                c -> AttackRestrictionDeal.worthTaking(c.ai, c.opp) ? "damage-as-unblocked was incorrectly treated as stopped by a blocker" : null));
        // ---- the real cards that offer "take X, then you can't attack me": The Second Doctor, Orzhov Advokist, Willie Lumpkin
        final java.util.function.Function<Ctx, SpellAbility> doctor = c -> c.bf("The Second Doctor", c.opp).getTriggers().get(0).ensureAbility();
        final java.util.function.Function<Ctx, SpellAbility> advokist = c -> c.bf("Orzhov Advokist", c.opp).getTriggers().get(0).ensureAbility();
        final java.util.function.Function<Ctx, SpellAbility> willie = c -> {
            final SpellAbility effect = c.bf("Willie Lumpkin, Postman", c.opp).getTriggers().get(0).ensureAbility().getSubAbility();
            effect.setActivatingPlayer(c.opp);
            return effect;
        };
        final java.util.function.BiFunction<Ctx, SpellAbility, Boolean> willieDraws = (c, sa) ->
                new forge.ai.ability.EffectAi().willPayUnlessCost(c.ai, sa, forge.game.cost.Cost.Zero, false,
                        new forge.util.collect.FCollection<Player>(c.ai));
        final Setup bigBoard = c -> c.bf("Hill Giant", 3, c.ai);
        final Setup wallOnly = c -> c.bf("Wall of Stone", c.ai); // cannot attack: nothing is given up
        l.add(new Scenario("second-doctor-takes-card-free", "The Second Doctor: with nothing that could attack, the AI draws", Fid.APPROX, "END_OF_TURN", false, wallOnly,
                c -> new forge.ai.ability.DrawAi().confirmAction(c.ai, doctor.apply(c), null, "", null) ? null : "refused a free card"));
        l.add(new Scenario("second-doctor-refuses-card-for-attack", "The Second Doctor: three Hill Giants ready to attack are worth more than a card", Fid.APPROX, "END_OF_TURN", false, bigBoard,
                c -> new forge.ai.ability.DrawAi().confirmAction(c.ai, doctor.apply(c), null, "", null) ? "drew and gave up its attack" : null));
        l.add(new Scenario("advokist-takes-counters-free", "Orzhov Advokist: with only a Wall the AI takes the counters", Fid.APPROX, "END_OF_TURN", false, wallOnly,
                c -> new forge.ai.ability.ChooseCardAi().chooseSingleCard(c.ai, advokist.apply(c), c.ai.getCreaturesInPlay(), true, null, null) != null ? null : "refused free counters"));
        l.add(new Scenario("advokist-refuses-counters-for-attack", "Orzhov Advokist: three Hill Giants ready to attack, so no counters and no restriction", Fid.APPROX, "END_OF_TURN", false, bigBoard,
                c -> new forge.ai.ability.ChooseCardAi().chooseSingleCard(c.ai, advokist.apply(c), c.ai.getCreaturesInPlay(), true, null, null) == null ? null : "took the counters and gave up its attack"));
        l.add(new Scenario("willie-draws-free", "Willie Lumpkin: with nothing that could attack, the AI takes the card", Fid.APPROX, "END_OF_TURN", false, wallOnly,
                c -> willieDraws.apply(c, willie.apply(c)) ? null : "refused a free card"));
        l.add(new Scenario("willie-refuses-card-for-attack", "Willie Lumpkin: three Hill Giants ready to attack are worth more than a card", Fid.APPROX, "END_OF_TURN", false, bigBoard,
                c -> willieDraws.apply(c, willie.apply(c)) ? "drew and gave up its attack" : null));
        l.add(new Scenario("expedition-forest-before-fetch", "Khalni Heart Expedition one mana short: the untapped Forest is the land drop, not the fetch land", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", c.ai); c.hand("Wooded Foothills", c.ai); c.hand("Forest", c.ai); c.hand("Khalni Heart Expedition", c.ai);
                       c.hand("Sakura-Tribe Elder", c.ai); c.lib("Forest", c.ai); c.lib("Mountain", c.ai); },
                c -> {
                    final List<Card> drops = c.landPlays();
                    return !drops.isEmpty() && !drops.get(0).getName().equals("Forest") ? "played " + drops.get(0).getName() + " first" : null;
                }));
        l.add(new Scenario("land-creature-gift", "Dryad Arbor into a Serra Angel that kills it for nothing is a gift to the blocker", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Dryad Arbor", c.ai); c.bf("Serra Angel", c.opp); },
                c -> {
                    final Card arbor = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Dryad Arbor")).findFirst().get();
                    return AiAttackController.isGiftToABlocker(c.ai, arbor, new Combat(c.ai)) ? null : "the Arbor was not recognised as a gift";
                }));
        l.add(new Scenario("land-creature-all-out-gift", "Even at all-out aggression 5, Dryad Arbor stays home when a blocker kills it for free", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Dryad Arbor", c.ai); c.bf("Serra Angel", c.opp); },
                c -> {
                    final Card arbor = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Dryad Arbor")).findFirst().get();
                    final AiAttackController attack = new AiAttackController(c.ai);
                    forceAttackAggression(attack, 5);
                    return attack.shouldAttack(arbor, c.opp.getCreaturesInPlay(), new Combat(c.ai), c.opp)
                            ? "the animated land was sent into a free kill at aggression 5" : null;
                }));
        l.add(new Scenario("land-creature-exalted-gate", "Only the dedicated Exalted strategy bypasses the animated-land gift guard", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Dryad Arbor", c.ai); c.bf("Serra Angel", c.opp); },
                c -> {
                    final Card arbor = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Dryad Arbor")).findFirst().get();
                    final Combat combat = new Combat(c.ai);
                    final boolean ordinary = AiAttackController.shouldDeclineLandGift(c.ai, arbor, combat, false);
                    final boolean exalted = AiAttackController.shouldDeclineLandGift(c.ai, arbor, combat, true);
                    return ordinary && !exalted ? null : "the land-gift guard was not limited to the explicit Exalted path";
                }));
        l.add(new Scenario("manabarbs-budget-order", "With 3 life the two basic lands (1 each) are kept, the nonbasic one (2 with Burning Earth) is dropped", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(3, null); c.bf("Plateau", c.ai); c.bf("Plains", 2, c.ai); c.bf("Manabarbs", c.opp); c.bf("Burning Earth", c.opp); },
                c -> {
                    final CardCollection sources = ComputerUtilMana.getAvailableManaSources(c.ai, true);
                    return sources.size() == 2 && sources.stream().noneMatch(k -> k.getName().equals("Plateau")) ? null
                            : "the cheap lands should buy the mana: " + sources;
                }));
        l.add(new Scenario("manabarbs-removal-other-target", "A Disenchant aimed at another permanent gets no exemption from the cost", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(3, null); c.bf("Plains", 2, c.ai); c.bf("Manabarbs", c.opp); c.bf("Sol Ring", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Disenchant", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Sol Ring")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "aimed at the Sol Ring it is no answer to the Manabarbs, but it was allowed";
                }));
        l.add(new Scenario("manabarbs-removal-aimed", "A Disenchant aimed at the Manabarbs itself is exempt", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(3, null); c.bf("Plains", 2, c.ai); c.bf("Manabarbs", c.opp); c.bf("Sol Ring", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Disenchant", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Manabarbs")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? "aimed at the Manabarbs it should be allowed" : null;
                }));
        // Machine God's Effigy copies as a non-creature: a vanilla 8/8 is only a mana rock then
        l.add(new Scenario("effigy-copy-value", "Machine God's Effigy copies what a non-creature does, not what a creature weighs", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.hand("Machine God's Effigy", c.ai); c.bf("Hill Giant", c.opp); c.bf("Mulldrifter", c.opp); c.bf("Glorious Anthem", c.opp); },
                c -> {
                    int bears = 0, drifter = 0;
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Hill Giant")) { bears = forge.ai.ability.CloneAi.nonCreatureCopyValue(c.ai, k); }
                        if (k.getName().equals("Mulldrifter")) { drifter = forge.ai.ability.CloneAi.nonCreatureCopyValue(c.ai, k); }
                    }
                    return bears > 0 ? "a vanilla creature is worth a copy (" + bears + ")" : drifter <= 0 ? "the enters trigger of Mulldrifter is worth nothing" : null;
                }));
        // the reserve is what the opponent hits for next turn: a tapped creature untaps
        l.add(noPlay("manabarbs-tapped-threat", "6 life, a tapped 6/4 on their side: two taps (2 life) would leave 4, not enough for its next attack", Fid.APPROX, "MAIN2", true, "Grizzly Bears",
                c -> { c.ai.setLife(6, null); c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp); c.bf("Craw Wurm", c.opp).setTapped(true); }));
        l.add(play("manabarbs-tapped-small", "6 life, a tapped 1/1 on their side: the same two taps are fine", Fid.APPROX, "MAIN2", true, "Grizzly Bears",
                c -> { c.ai.setLife(6, null); c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp); c.bf("Llanowar Elves", c.opp).setTapped(true); }));
        l.add(new Scenario("manabarbs-spent-dork", "A Llanowar Elves tapped to pay cannot also block the Craw Wurm", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(6, null); c.bf("Forest", c.ai); c.bf("Llanowar Elves", c.ai); c.hand("Grizzly Bears", c.ai); c.bf("Manabarbs", c.opp); c.bf("Craw Wurm", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Grizzly Bears", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the Elf was counted as a blocker even though it pays for the spell";
                }));
        l.add(new Scenario("manabarbs-trample-reserve", "A 2/2 blocker does not absorb all the damage from an 8/8 trampler", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(8, null); c.bf("Forest", 2, c.ai); c.bf("Llanowar Elves", c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Centaur Courser", c.ai); c.bf("Manabarbs", c.opp); c.bf("Terra Stomper", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Centaur Courser", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the estimate treated the Grizzly Bears as stopping all trample damage";
                }));
        l.add(new Scenario("manabarbs-menace-reserve", "A single blocker cannot block the opponent's menace creature", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(5, null); c.bf("Forest", 2, c.ai); c.bf("Llanowar Elves", c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Centaur Courser", c.ai); c.bf("Manabarbs", c.opp); c.bf("Boggart Brute", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Centaur Courser", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "one blocker was incorrectly counted against menace";
                }));
        l.add(new Scenario("manabarbs-removal-threat", "Doom Blade removes the only attacker before the life reserve is calculated", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(5, null); c.bf("Swamp", 2, c.ai); c.hand("Doom Blade", c.ai); c.bf("Manabarbs", c.opp); c.bf("Craw Wurm", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Doom Blade", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Craw Wurm")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? "the removed Craw Wurm was still counted as next-turn damage" : null;
                }));
        l.add(new Scenario("manabarbs-remove-one-source", "Destroying Manabarbs does not ignore a second source such as Burning Earth", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(5, null); c.bf("Plains", c.ai); c.bf("Plateau", c.ai); c.hand("Disenchant", c.ai); c.bf("Manabarbs", c.opp); c.bf("Burning Earth", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Disenchant", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Manabarbs")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "removing only Manabarbs incorrectly granted the life-to-one exception";
                }));
        l.add(new Scenario("manabarbs-remove-source-but-threat-remains", "Removing Manabarbs does not ignore a Craw Wurm that will attack next turn", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(4, null); c.bf("Plains", 2, c.ai); c.hand("Disenchant", c.ai); c.bf("Manabarbs", c.opp); c.bf("Craw Wurm", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Disenchant", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Manabarbs")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "the life-to-one exception ignored the Craw Wurm's next attack";
                }));
        l.add(new Scenario("manabarbs-indestructible-source", "Disenchant does not get the exception when Manabarbs is indestructible", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(4, null); c.bf("Plains", 2, c.ai); c.bf("Serra Angel", c.ai); c.hand("Disenchant", c.ai); c.bf("Manabarbs", c.opp); c.bf("Avacyn, Angel of Hope", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Disenchant", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    for (final Card k : c.opp.getCardsIn(ZoneType.Battlefield)) {
                        if (k.getName().equals("Manabarbs")) { sa.getTargets().add(k); }
                    }
                    return ActivationGuard.tooPainfulToPay(c.ai, sa) ? null : "an indestructible Manabarbs was treated as removed";
                }));
        l.add(new Scenario("manabarbs-ojer-replacement", "Ojer Axonil increases each Manabarbs trigger to its power", Fid.APPROX, M1, true,
                c -> { c.ai.setLife(10, null); c.bf("Mountain", 2, c.ai); c.hand("Mind Stone", c.ai); c.bf("Manabarbs", c.opp); c.bf("Ojer Axonil, Deepest Might", c.opp); },
                c -> {
                    final SpellAbility sa = c.hand("Mind Stone", c.ai).getFirstSpellAbility();
                    sa.setActivatingPlayer(c.ai);
                    final int damage = ComputerUtilMana.estimateTapDamage(c.ai, sa);
                    return damage == 8 ? null : "expected 8 damage from two taps with Ojer Axonil, estimated " + damage;
                }));
        // review of the Effigy patch: what the AI really becomes when the Effigy enters
        l.add(new Scenario("effigy-picks-etb", "The Effigy becomes a Mulldrifter (its enters trigger) and not the Hill Giant", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.bf("Hill Giant", c.opp); c.bf("Mulldrifter", c.opp); c.lib("Island", 8, c.ai); },
                c -> effigyBecame(c, "Mulldrifter")));
        l.add(new Scenario("effigy-declines-vanilla", "Only a vanilla creature on the table: the Effigy stays the rock that it is", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.bf("Craw Wurm", c.opp); },
                c -> effigyBecame(c, "Machine God's Effigy")));
        l.add(new Scenario("effigy-own-legend", "The AI has Kiora already: the copy of the opponent's Kiora would be lost to the legend rule", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.bf("Kiora, the Rising Tide", c.ai); c.bf("Kiora, the Rising Tide", c.opp); c.bf("Craw Wurm", c.opp); c.lib("Island", 8, c.ai); },
                c -> effigyBecame(c, "Machine God's Effigy")));
        l.add(new Scenario("effigy-copies-opp-legend", "Without a Kiora of its own the AI copies the opponent's one", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.bf("Kiora, the Rising Tide", c.opp); c.bf("Craw Wurm", c.opp); c.lib("Island", 8, c.ai); },
                c -> effigyBecame(c, "Kiora, the Rising Tide")));
        // a deathtouch blocker takes one attacker: it does not hold back a whole army
        l.add(new Scenario("deathtouch-swarm", "Four 2/2s attack into one Typhoid Rats: it can block only one", Fid.APPROX, M1, true,
                c -> { c.bf("Grizzly Bears", 4, c.ai); c.bf("Typhoid Rats", c.opp); },
                c -> c.attackers().size() < 2 ? "only " + c.attackers().size() + " attacked into a single deathtouch blocker" : null));
        l.add(new Scenario("deathtouch-lone", "A lone Hill Giant does not attack into the Typhoid Rats", Fid.APPROX, M1, true,
                c -> { c.bf("Hill Giant", c.ai); c.bf("Typhoid Rats", c.opp); },
                c -> c.attackers().isEmpty() ? null : "the Hill Giant attacked into a deathtouch blocker with nothing else"));
        // review of the deathtouch patch: only the blocks that are possible count, and only blockers that kill and die
        l.add(new Scenario("deathtouch-flyer", "A Serra Angel that the Rats cannot block attacks, the Hill Giant stays", Fid.APPROX, M1, true,
                c -> { c.bf("Serra Angel", c.ai); c.bf("Hill Giant", c.ai); c.bf("Typhoid Rats", c.opp); },
                c -> c.attackers().stream().anyMatch(a -> a.getName().equals("Serra Angel")) ? null : "the flyer did not attack past a ground deathtouch blocker"));
        l.add(new Scenario("deathtouch-protection", "White Knights with protection from black are not blocked by the Rats, they attack", Fid.APPROX, M1, true,
                c -> { c.bf("White Knight", 2, c.ai); c.bf("Typhoid Rats", c.opp); },
                c -> c.attackers().isEmpty() ? "protection from black attackers stayed home against a black deathtouch blocker" : null));
        l.add(new Scenario("deathtouch-first-strike-wall", "Glissa (first strike, deathtouch) takes ONE Hill Giant: the other two still attack", Fid.APPROX, M1, true,
                c -> { c.bf("Hill Giant", 3, c.ai); c.bf("Glissa Sunslayer", c.opp); },
                c -> c.attackers().size() >= 2 ? null : "only " + c.attackers().size() + " of 3 Hill Giants attacked, she can block only one"));
        l.add(new Scenario("deathtouch-group-damage",
                "Two Grizzly Bears attack together into a Palace Guard with Gorgon's Head: combined damage kills it, but its 1 power only kills one",
                Fid.APPROX, M1, true,
                c -> {
                    c.bf("Grizzly Bears", 2, c.ai);

                    final Card guard = c.bf("Palace Guard", c.opp);
                    c.bf("Gorgon's Head", c.opp).attachToEntity(guard, null);
                },
                c -> {
                    final Card guard = c.opp.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(k -> k.getName().equals("Palace Guard"))
                            .findFirst().orElse(null);

                    final Card head = c.opp.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(k -> k.getName().equals("Gorgon's Head"))
                            .findFirst().orElse(null);

                    if (guard == null || head == null || !head.isAttachedToEntity(guard)) {
                        return "Gorgon's Head was not attached to Palace Guard";
                    }

                    if (!guard.hasKeyword("Deathtouch")) {
                        return "Gorgon's Head did not grant deathtouch to Palace Guard";
                    }

                    if (!guard.canBlockAny()) {
                        return "Palace Guard cannot block multiple attackers";
                    }

                    final long attackingBears = c.attackers().stream()
                            .filter(a -> a.getName().equals("Grizzly Bears"))
                            .count();

                    return attackingBears == 2 ? null
                            : "only " + attackingBears
                            + " of 2 Bears attacked; combined damage should kill the Guard";
                }));
        // #5907: removal that hands its victim's controller a land is a card spent to ramp them when it hits a mana dork
        l.add(noPlay("5907-path-hierarch", "Path to Exile against a lone Noble Hierarch only gives them a land", Fid.APPROX, M1, true, "Path to Exile",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Path to Exile", c.ai); c.bf("Noble Hierarch", c.opp); }));
        l.add(noPlay("5907-path-birds", "Path to Exile against a lone Birds of Paradise", Fid.APPROX, M1, true, "Path to Exile",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Path to Exile", c.ai); c.bf("Birds of Paradise", c.opp); }));
        l.add(noPlay("5907-erode-harbinger", "Erode against a lone Treefolk Harbinger only gives them a land", Fid.APPROX, M1, true, "Erode",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Erode", c.ai); c.bf("Treefolk Harbinger", c.opp); }));
        l.add(noPlay("5907-trophy-halfling", "Assassin's Trophy against a lone Delighted Halfling only gives them a land", Fid.APPROX, M1, true, "Assassin's Trophy",
                c -> { c.bf("Swamp", 2, c.ai); c.bf("Forest", 2, c.ai); c.hand("Assassin's Trophy", c.ai); c.bf("Delighted Halfling", c.opp); }));
        l.add(play("5907-path-angel", "Path to Exile still goes at a real threat", Fid.APPROX, M1, true, "Path to Exile",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Path to Exile", c.ai); c.bf("Baneslayer Angel", c.opp); }));
        l.add(aimed("5907-path-angel-not-dork", "Path to Exile at the Angel, not at the mana dork beside it", Fid.APPROX, M1, true, "Path to Exile", "Baneslayer Angel",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Path to Exile", c.ai); c.bf("Noble Hierarch", c.opp); c.bf("Baneslayer Angel", c.opp); }));
        l.add(play("5907-erode-serra", "Erode on a Serra Angel is worth the land", Fid.APPROX, M1, true, "Erode",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Erode", c.ai); c.bf("Serra Angel", c.opp); }));
        l.add(play("4430", "Elvish Piper when a creature is also castable", Fid.EXACT, M1, true, "Elvish Piper",
                c -> { c.bf("Forest", 3, c.ai); c.bf("Elvish Piper", c.ai); c.hand("Grizzly Bears", c.ai); c.hand("Craterhoof Behemoth", c.ai); }));
        l.add(new Scenario("4439", "Might of Old Krosa goes on the creature that is in combat", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                c -> {
                    c.bf("Forest", c.ai); c.hand("Might of Old Krosa", c.ai);
                    final Card blocker = c.bf("Grizzly Bears", c.ai); c.bf("Llanowar Elves", c.ai);
                    final Card att = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                },
                c -> {
                    final SpellAbility sa = c.playOf("Might of Old Krosa");
                    if (sa == null) { return "not cast"; }
                    final Card t = c.target(sa);
                    return t != null && t.getName().equals("Grizzly Bears") ? null : "targeted " + t;
                }));
        // Mischievous Poltergeist (Pay 1 life: Regenerate) blocks something it cannot survive: one shield, paid once
        final Setup poltergeistBlocks = c -> {
            final Card blocker = c.bf("Mischievous Poltergeist", c.ai);
            final Card att = c.bf("Hill Giant", c.opp);
            final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(play("poltergeist-shield", "regenerates the blocker that would die (the first shield is worth the life)", Fid.EXACT, "COMBAT_DECLARE_BLOCKERS", false,
                "Mischievous Poltergeist", poltergeistBlocks));
        l.add(noPlay("poltergeist-shield-up", "no second shield for a blocker that already has one (it drained its life, 1 per pass)", Fid.EXACT, "COMBAT_DECLARE_BLOCKERS", false,
                "Mischievous Poltergeist", c -> { poltergeistBlocks.run(c);
                    c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Mischievous Poltergeist")).forEach(Card::incShieldCount); }));
        // the board before the spell: a creature that cannot hurt the AI (Spellskite, or anything under Moat) is not worth a wipe or removal
        l.add(noPlay("harmless-wrath", "Wrath of God for a lone Spellskite", Fid.EXACT, "MAIN2", true, "Wrath of God",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Wrath of God", c.ai); c.bf("Spellskite", c.opp); }));
        l.add(noPlay("harmless-wrath-moat", "Wrath of God for a creature that cannot attack because of Moat", Fid.EXACT, "MAIN2", true, "Wrath of God",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Wrath of God", c.ai); c.bf("Moat", c.ai); c.bf("Hill Giant", c.opp); }));
        l.add(noPlay("harmless-removal", "Doom Blade for a lone Spellskite", Fid.EXACT, "MAIN2", true, "Doom Blade",
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Doom Blade", c.ai); c.bf("Spellskite", c.opp); }));
        l.add(play("harmful-wrath", "Wrath of God for three Hill Giants is still fine", Fid.EXACT, "MAIN2", true, "Wrath of God",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Wrath of God", c.ai); c.bf("Hill Giant", 3, c.opp); }));
        l.add(play("harmful-removal", "Doom Blade for a Hill Giant is still fine", Fid.EXACT, "MAIN2", true, "Doom Blade",
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Doom Blade", c.ai); c.bf("Hill Giant", c.opp); }));
        // #12895 Crovax the Cursed: flying has to be there when the blockers are declared, and it never sacrifices itself for its own counter
        final Setup crovaxVsSerra = c -> {
            c.bf("Swamp", c.ai);
            c.counters(c.bf("Crovax the Cursed", c.ai), CounterEnumType.P1P1, 6);
            final Card serra = c.bf("Serra Angel", c.opp);
            final Combat combat = new Combat(c.opp); combat.addAttacker(serra, c.ai);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(play("12895-flying-before-blocks", "Crovax gains flying when the Serra Angel attacks, before blockers", Fid.EXACT, "COMBAT_DECLARE_ATTACKERS", false,
                "Crovax the Cursed", crovaxVsSerra));
        l.add(noPlay("12895-flying-after-blocks", "no flying for Crovax once the blockers are declared (too late to block)", Fid.EXACT, "COMBAT_DECLARE_BLOCKERS", false,
                "Crovax the Cursed", crovaxVsSerra));
        // keywords that win the fight (after the blocks, only for a creature that is in it) and keywords that decide the block (before it)
        final Setup impBlocks = c -> {
            c.bf("Swamp", c.ai);
            final Card blocker = c.bf("Fetid Imp", c.ai); final Card att = c.bf("Hill Giant", c.opp);
            final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(play("kw-deathtouch-blocker", "Fetid Imp blocks the Hill Giant: deathtouch makes it a trade", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Fetid Imp", impBlocks));
        l.add(noPlay("kw-deathtouch-not-in-combat", "Fetid Imp stays out of the fight: no deathtouch for nothing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Fetid Imp", c -> {
                    c.bf("Swamp", c.ai); c.bf("Fetid Imp", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(play("kw-indestructible-blocker", "Drudge Sentinel blocks the Hill Giant: indestructible saves it", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Drudge Sentinel", c -> {
                    c.bf("Swamp", 3, c.ai);
                    final Card blocker = c.bf("Drudge Sentinel", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-indestructible-not-in-combat", "Drudge Sentinel is not in the fight and nothing threatens it", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Drudge Sentinel", c -> {
                    c.bf("Swamp", 3, c.ai); c.bf("Drudge Sentinel", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        final Setup askariAttacks = c -> {
            c.bf("Mountain", 2, c.ai);
            final Card askari = c.bf("Searing Spear Askari", c.ai); c.bf("Hill Giant", c.opp);
            final Combat combat = new Combat(c.ai); combat.addAttacker(askari, c.opp);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(play("kw-menace-before-blocks", "Searing Spear Askari attacks into a lone Hill Giant: menace before the blockers", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Searing Spear Askari", askariAttacks));
        l.add(noPlay("kw-menace-after-blocks", "no menace once the blockers are declared (too late)", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", true,
                "Searing Spear Askari", askariAttacks));
        // evasion before the blockers has to be worth it: the keyword must dodge the bad blocker, and the mana must be spare
        l.add(play("kw-fear-dodges", "Hooded Kavu attacks into a Hill Giant (red): fear dodges the bad block", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Hooded Kavu", c -> {
                    c.bf("Swamp", c.ai); final Card kavu = c.bf("Hooded Kavu", c.ai); c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.ai); combat.addAttacker(kavu, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-fear-black-blocker", "Hooded Kavu against a black Black Knight: fear changes nothing", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Hooded Kavu", c -> {
                    c.bf("Swamp", c.ai); final Card kavu = c.bf("Hooded Kavu", c.ai); c.bf("Black Knight", c.opp);
                    final Combat combat = new Combat(c.ai); combat.addAttacker(kavu, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-menace-two-blockers", "Searing Spear Askari against two Hill Giants: they can still double-block", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Searing Spear Askari", c -> {
                    c.bf("Mountain", 2, c.ai); final Card askari = c.bf("Searing Spear Askari", c.ai); c.bf("Hill Giant", c.opp); c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.ai); combat.addAttacker(askari, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-menace-mana-wanted", "Searing Spear Askari: the mana is for the Gray Ogre in hand, not for menace", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Searing Spear Askari", c -> {
                    c.bf("Mountain", 3, c.ai); final Card askari = c.bf("Searing Spear Askari", c.ai); c.bf("Hill Giant", c.opp);
                    c.hand("Gray Ogre", c.ai); // {2}{R}: castable with the three Mountains, but not with menace paid for first
                    final Combat combat = new Combat(c.ai); combat.addAttacker(askari, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        // X spells: no Epiphany at the Drownyard for X=0 (it reveals one card), a random target stays random
        l.add(noPlay("omarthis-elesh-norn", "Omarthis with four lands under Elesh Norn: X=2 dies as it enters (-2/-2)", Fid.APPROX, "MAIN1", true,
                "Omarthis, Ghostfire Initiate", c -> { c.bf("Mountain", 4, c.ai); c.hand("Omarthis, Ghostfire Initiate", c.ai); c.bf("Elesh Norn, Grand Cenobite", c.opp); }));
        l.add(noPlay("fire-ice-not-at-the-face", "Fire // Ice in the second main phase against a player with no creatures: Fire is not thrown at their face", Fid.APPROX, "MAIN2", true,
                "Fire // Ice", c -> { c.bf("Mountain", c.ai); c.bf("Island", c.ai); c.hand("Fire // Ice", c.ai); c.hand("Grizzly Bears", c.ai); c.hand("Forest", c.ai); c.hand("Island", c.ai); }));
        l.add(play("fire-ice-cantrip-at-their-end", "Ice at the end of the opponent's turn: a card for two mana that were not used", Fid.APPROX, "END_OF_TURN", false,
                "Fire // Ice", c -> { c.bf("Mountain", c.ai); c.bf("Island", c.ai); c.hand("Fire // Ice", c.ai); c.bf("Forest", c.opp); }));
        l.add(noPlay("epiphany-x0", "Epiphany at the Drownyard with a single Island (X=0)", Fid.APPROX, "END_OF_TURN", false,
                "Epiphany at the Drownyard", c -> { c.bf("Island", c.ai); c.hand("Epiphany at the Drownyard", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(new Scenario("goblin-test-pilot", "Goblin Test Pilot's target is random: the AI does not aim it", Fid.APPROX, "MAIN1", true,
                c -> { c.bf("Goblin Test Pilot", c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Hill Giant", c.opp); },
                c -> {
                    final SpellAbility sa = c.playOf("Goblin Test Pilot");
                    return sa != null && sa.getTargets() != null && !sa.getTargets().isEmpty() ? "the AI chose the target itself: " + sa.getTargets() : null;
                }));
        // a suspended Rift Bolt that comes out of exile: free, and lost if not cast, so any legal target will do
        l.add(new Scenario("rift-bolt-suspended-cast", "suspended Rift Bolt with only Shalai, Voice of Plenty to aim at (the player is hexproof)", Fid.APPROX, "UPKEEP", true,
                c -> { c.exile("Rift Bolt", c.ai); c.bf("Shalai, Voice of Plenty", c.opp); },
                c -> {
                    final Card bolt = c.ai.getCardsIn(ZoneType.Exile).stream().filter(k -> k.getName().equals("Rift Bolt")).findFirst().get();
                    final forge.game.trigger.Trigger play = bolt.getTriggers().stream()
                            .filter(t -> t.getMode() == forge.game.trigger.TriggerType.CounterRemoved).findFirst().get();
                    final SpellAbility playSa = play.ensureAbility();
                    playSa.setTrigger(play);
                    playSa.setActivatingPlayer(c.ai);
                    if (bolt.hasSuspend() || !"TIME".equals(play.getParam("CounterType"))
                            || !"0".equals(play.getParam("NewCounterAmount"))) {
                        return "scenario did not model the removal of the last TIME counter";
                    }
                    final Card shalai = c.opp.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(k -> k.getName().equals("Shalai, Voice of Plenty")).findFirst().get();
                    if (!ComputerUtil.playNoStack(c.ai, playSa, c.game, true)) {
                        return "the suspended Rift Bolt Play effect did not resolve";
                    }
                    if (c.game.getStack().isEmpty()) {
                        return "the suspended Rift Bolt was not cast; the stack is empty";
                    }
                    final SpellAbility cast = c.game.getStack().peekAbility();
                    return cast != null && "Rift Bolt".equals(cast.getHostCard().getName())
                            && cast.getTargets().contains(shalai) ? null
                            : "the suspended Rift Bolt was not cast targeting Shalai; stack=" + c.game.getStack();
                }));
        l.add(new Scenario("rift-bolt-suspended-crystal-barricade", "suspended Rift Bolt with only Crystal Barricade to aim at (the player is hexproof)", Fid.APPROX,
                "UPKEEP", true, c -> { c.exile("Rift Bolt", c.ai); c.bf("Crystal Barricade", c.opp); },
                c -> {
                    final Card bolt = c.ai.getCardsIn(ZoneType.Exile).stream().filter(k -> k.getName().equals("Rift Bolt")).findFirst().get();
                    final forge.game.trigger.Trigger play = bolt.getTriggers().stream()
                            .filter(t -> t.getMode() == forge.game.trigger.TriggerType.CounterRemoved).findFirst().get();
                    final SpellAbility playSa = play.ensureAbility();
                    playSa.setTrigger(play);
                    playSa.setActivatingPlayer(c.ai);
                    if (bolt.hasSuspend() || !"TIME".equals(play.getParam("CounterType"))
                            || !"0".equals(play.getParam("NewCounterAmount"))) {
                        return "scenario did not model the removal of the last TIME counter";
                    }
                    final Card barricade = c.opp.getCardsIn(ZoneType.Battlefield).stream()
                            .filter(k -> k.getName().equals("Crystal Barricade")).findFirst().get();
                    if (!ComputerUtil.playNoStack(c.ai, playSa, c.game, true)) {
                        return "the suspended Rift Bolt Play effect did not resolve";
                    }
                    if (c.game.getStack().isEmpty()) {
                        return "the suspended Rift Bolt was not cast; the stack is empty";
                    }
                    final SpellAbility cast = c.game.getStack().peekAbility();
                    return cast != null && "Rift Bolt".equals(cast.getHostCard().getName())
                            && cast.getTargets().contains(barricade) ? null
                            : "the suspended Rift Bolt was not cast targeting Crystal Barricade; stack=" + c.game.getStack();
                }));
        l.add(play("scapeshift-valakut-library-lethal", "Scapeshift fetches Valakut and seven Mountains as a lethal batch", Fid.APPROX,
                M1, true, "Scapeshift", c -> {
                    c.richMana(); c.hand("Scapeshift", c.ai); c.lib("Valakut, the Molten Pinnacle", c.ai); c.lib("Mountain", 7, c.ai);
                }));
        l.add(new Scenario("scapeshift-valakut-trigger-batch-resolves",
                "Scapeshift resolves and all seven Valakut triggers target the opponent as a lethal batch", Fid.APPROX, M1, true,
                c -> {
                    // Eight lands are sacrificed: three Mountains and five Forests. The seven fetched Mountains
                    // enter together with Valakut, so each one sees at least five other Mountains.
                    c.bf("Mountain", 3, c.ai);
                    c.bf("Forest", 5, c.ai);
                    c.hand("Scapeshift", c.ai);
                    c.lib("Valakut, the Molten Pinnacle", c.ai);
                    c.lib("Mountain", 7, c.ai);
                },
                c -> {
                    final SpellAbility scapeshift = c.playOf("Scapeshift");
                    if (scapeshift == null) {
                        return "AI did not choose Scapeshift" + c.why("Scapeshift");
                    }
                    if (!ComputerUtil.playStack(scapeshift, c.ai, c.game)) {
                        return "Scapeshift could not be cast";
                    }

                    int valakutTriggers = 0;
                    final List<String> misdirected = new ArrayList<>();
                    for (int i = 0; i < 50; i++) {
                        c.game.getStack().addAllTriggeredAbilitiesToStack();
                        if (c.game.getStack().isEmpty()) {
                            break;
                        }
                        final SpellAbility top = c.game.getStack().peekAbility();
                        c.trace.append(" [resolving ").append(top)
                                .append(top.usesTargeting() ? " -> " + top.getTargets() : "").append(']');
                        final Card source = top.getHostCard();
                        if (top.isTrigger() && top.getApi() == ApiType.DealDamage && source != null
                                && "Valakut, the Molten Pinnacle".equals(source.getName())) {
                            valakutTriggers++;
                            if (top.getTargets() == null || !top.getTargets().contains(c.opp)) {
                                misdirected.add(String.valueOf(top.getTargets()));
                            }
                        }
                        c.game.getStack().resolveStack();
                        c.game.getAction().checkStateEffects(true);
                    }

                    if (valakutTriggers != 7) {
                        return "expected seven simultaneous Valakut triggers, observed " + valakutTriggers
                                + " | " + c.trace;
                    }
                    if (!misdirected.isEmpty()) {
                        return "Valakut triggers were not all aimed at the opponent: " + misdirected;
                    }
                    return c.opp.getLife() <= 0 ? null
                            : "the seven 3-damage triggers did not finish the opponent (life " + c.opp.getLife() + ")";
                }));
        l.add(noPlay("scapeshift-valakut-not-lethal", "Scapeshift waits when its six fetched Mountains only deal 18", Fid.APPROX,
                M1, true, "Scapeshift", c -> {
                    c.richMana(); c.hand("Scapeshift", c.ai); c.bf("Valakut, the Molten Pinnacle", c.ai); c.lib("Mountain", 6, c.ai);
                }));
        // keyword abilities that aim at a creature (Arnjlot's Ascent) follow the same timing, and shields only for a creature in the fight
        l.add(aimed("kw-targeted-flying-block", "Wind Drake attacks: flying for the blocker, in the declare attackers step", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", false,
                "Arnjlot's Ascent", "Grizzly Bears", c -> {
                    c.bf("Island", c.ai); c.bf("Arnjlot's Ascent", c.ai); c.bf("Grizzly Bears", c.ai);
                    final Card drake = c.bf("Wind Drake", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(drake, c.ai);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-targeted-flying-too-late", "the same, once the blockers are declared", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Arnjlot's Ascent", c -> {
                    c.bf("Island", c.ai); c.bf("Arnjlot's Ascent", c.ai); c.bf("Grizzly Bears", c.ai);
                    final Card drake = c.bf("Wind Drake", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(drake, c.ai);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(aimed("kw-targeted-flying-attack", "Hill Giant attacks into a Craw Wurm: flying for the attacker, before the blockers", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Arnjlot's Ascent", "Hill Giant", c -> {
                    c.bf("Island", c.ai); c.bf("Arnjlot's Ascent", c.ai); final Card giant = c.bf("Hill Giant", c.ai); c.bf("Craw Wurm", c.opp);
                    final Combat combat = new Combat(c.ai); combat.addAttacker(giant, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("kw-targeted-flying-no-bad-block", "Craw Wurm attacks into a Hill Giant: nobody would block it badly, flying is a waste", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                "Arnjlot's Ascent", c -> {
                    c.bf("Island", c.ai); c.bf("Arnjlot's Ascent", c.ai); final Card wurm = c.bf("Craw Wurm", c.ai); c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.ai); combat.addAttacker(wurm, c.opp);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        l.add(noPlay("regenerate-not-in-fight", "Drudge Skeletons is not in the fight: no regeneration shield for nothing", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Drudge Skeletons", c -> {
                    c.bf("Swamp", c.ai); c.bf("Drudge Skeletons", c.ai); final Card att = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai);
                    c.game.getPhaseHandler().setCombat(combat);
                }));
        // pacts: free now, {3}{U}{U} at the next upkeep or the game is lost: only when that can be paid
        l.add(noPlay("pact-wrong-colors", "Pact of Negation with five Mountains: {3}{U}{U} could never be paid", Fid.APPROX, M1, false, "Pact of Negation",
                c -> { c.bf("Mountain", 5, c.ai); c.hand("Pact of Negation", c.ai); c.stack(c.opp, "Serra Angel"); }));
        l.add(noPlay("pact-too-few-lands", "Pact of Negation with three Islands: five mana are needed at the upkeep", Fid.APPROX, M1, false, "Pact of Negation",
                c -> { c.bf("Island", 3, c.ai); c.hand("Pact of Negation", c.ai); c.stack(c.opp, "Serra Angel"); }));
        l.add(play("pact-payable", "Pact of Negation with five Islands against a Serra Angel: it can be paid", Fid.APPROX, M1, false, "Pact of Negation",
                c -> { c.bf("Island", 5, c.ai); c.hand("Pact of Negation", c.ai); c.stack(c.opp, "Serra Angel"); }));
        // Regenerate, Animate and Destroy abilities: used when they do something, never when they do nothing
        final Setup skeletonsBlock = c -> {
            c.bf("Swamp", 2, c.ai);
            final Card blocker = c.bf("Drudge Skeletons", c.ai); final Card att = c.bf("Hill Giant", c.opp);
            final Combat combat = new Combat(c.opp); combat.addAttacker(att, c.ai); combat.addBlocker(att, blocker);
            c.game.getPhaseHandler().setCombat(combat);
        };
        l.add(play("regen-blocker", "Drudge Skeletons blocks a Hill Giant: the shield saves it", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Drudge Skeletons", skeletonsBlock));
        l.add(noPlay("regen-shield-up", "Drudge Skeletons already has a shield: no second one", Fid.APPROX, "COMBAT_DECLARE_BLOCKERS", false,
                "Drudge Skeletons", c -> { skeletonsBlock.run(c);
                    c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Drudge Skeletons")).forEach(Card::incShieldCount); }));
        l.add(noPlay("regen-idle", "Drudge Skeletons with nothing happening: no shield for nothing", Fid.APPROX, M1, true,
                "Drudge Skeletons", c -> { c.bf("Swamp", 2, c.ai); c.bf("Drudge Skeletons", c.ai); c.bf("Hill Giant", c.opp); }));
        l.add(play("regen-in-response", "Lightning Bolt aimed at Drudge Skeletons: regenerate in response", Fid.APPROX, M1, false,
                "Drudge Skeletons", c -> { c.bf("Swamp", 2, c.ai); final Card sk = c.bf("Drudge Skeletons", c.ai); c.stack(c.opp, "Lightning Bolt", sk); }));
        l.add(noPlay("regen-bolt-at-face", "Lightning Bolt aimed at the AI itself: nothing to regenerate", Fid.APPROX, M1, false,
                "Drudge Skeletons", c -> { c.bf("Swamp", 2, c.ai); c.bf("Drudge Skeletons", c.ai); c.stack(c.opp, "Lightning Bolt", c.ai); }));
        l.add(noPlay("animate-already", "Mutavault is a creature already this turn: no second animation", Fid.APPROX, M1, true,
                "Mutavault", c -> {
                    c.bf("Mountain", 3, c.ai); final Card mv = c.bf("Mutavault", c.ai); c.bf("Hill Giant", c.opp);
                    for (final SpellAbility sa : mv.getSpellAbilities()) {
                        if (sa.getApi() == forge.game.ability.ApiType.Animate) { sa.setActivatingPlayer(c.ai); c.resolve(sa); break; }
                    }
                }));
        l.add(noPlay("animate-own-mana", "Mutavault alone: it would tap itself for its own {1} and be a tapped creature", Fid.APPROX, "COMBAT_BEGIN", true,
                "Mutavault", c -> { c.bf("Mutavault", c.ai); }));
        l.add(play("animate-attack", "Mutavault against an empty board: animate it and attack (at the start of combat, where the AI animates)", Fid.APPROX, "COMBAT_BEGIN", true,
                "Mutavault", c -> { c.bf("Mountain", 2, c.ai); c.bf("Mutavault", c.ai); }));
        l.add(noPlay("destroy-no-tapped-creature", "Royal Assassin with only untapped creatures to aim at", Fid.APPROX, M1, true,
                "Royal Assassin", c -> { c.bf("Swamp", c.ai); c.bf("Royal Assassin", c.ai); c.bf("Hill Giant", c.opp); }));
        l.add(play("destroy-tapped-creature", "Royal Assassin with a tapped Hill Giant to destroy", Fid.APPROX, M1, true,
                "Royal Assassin", c -> { c.bf("Swamp", c.ai); c.bf("Royal Assassin", c.ai); c.bf("Hill Giant", c.opp).tap(true, null, null); }));
        l.add(noPlay("destroy-indestructible", "Royal Assassin against a tapped indestructible Darksteel Colossus", Fid.APPROX, M1, true,
                "Royal Assassin", c -> { c.bf("Swamp", c.ai); c.bf("Royal Assassin", c.ai); c.bf("Darksteel Colossus", c.opp).tap(true, null, null); }));
        l.add(noPlay("destroy-own-creature", "Royal Assassin with only its own tapped creature to destroy", Fid.APPROX, M1, true,
                "Royal Assassin", c -> { c.bf("Swamp", c.ai); c.bf("Royal Assassin", c.ai); c.bf("Hill Giant", c.ai).tap(true, null, null); }));
        // Seal of Fire against Shalai, Voice of Plenty: the player is hexproof, the 2 damage cannot kill a 4-toughness flyer
        l.add(noPlay("seal-of-fire-shalai", "Seal of Fire aimed at Shalai that it cannot kill (the player at 2 life is hexproof)", Fid.APPROX, "END_OF_TURN", false,
                "Seal of Fire", c -> { c.bf("Seal of Fire", c.ai); c.bf("Shalai, Voice of Plenty", c.opp); c.opp.setLife(2, null); }));
        l.add(play("seal-of-fire-face", "Seal of Fire for the last 2 life of the opponent", Fid.APPROX, "END_OF_TURN", false,
                "Seal of Fire", c -> { c.bf("Seal of Fire", c.ai); c.opp.setLife(2, null); }));
        l.add(new Scenario("12895-sacrifice-itself", "Crovax alone does not agree to sacrifice a creature (it would be itself)", Fid.EXACT, "UPKEEP", true,
                c -> { c.counters(c.bf("Crovax the Cursed", c.ai), CounterEnumType.P1P1, 3); },
                c -> {
                    final Card crovax = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Crovax the Cursed")).findFirst().get();
                    SpellAbility sac = crovax.getTriggers().get(0).ensureAbility();
                    sac.setActivatingPlayer(c.ai);
                    return ((PlayerControllerAi) c.ai.getController()).confirmAction(sac, null, "", null) ? "agreed to sacrifice Crovax to its own ability" : null;
                }));
        l.add(new Scenario("12895-sacrifice-other", "Crovax with a Grizzly Bears to spare may sacrifice it (the counter is worth a 2/2)", Fid.APPROX, "UPKEEP", true,
                c -> { c.counters(c.bf("Crovax the Cursed", c.ai), CounterEnumType.P1P1, 3); c.bf("Llanowar Elves", c.ai); },
                c -> {
                    final Card crovax = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Crovax the Cursed")).findFirst().get();
                    SpellAbility sac = crovax.getTriggers().get(0).ensureAbility();
                    sac.setActivatingPlayer(c.ai);
                    return ((PlayerControllerAi) c.ai.getController()).confirmAction(sac, null, "", null) ? null : "refused to sacrifice an Elf for a counter";
                }));
        // #12892 Garruk, Veiled Butcher: the -3 discards, even though creature cards in hand match his "creature an opponent controls would die" replacement
        l.add(new Scenario("12892", "Garruk, Veiled Butcher -3: each opponent discards two cards (creature cards in the hand included)", Fid.EXACT, "MAIN1", true,
                c -> { c.counters(c.bf("Garruk, Veiled Butcher", c.ai), CounterEnumType.LOYALTY, 5);
                    c.hand("Grizzly Bears", c.opp); c.hand("Hill Giant", c.opp); },
                c -> {
                    final Card garruk = c.ai.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Garruk, Veiled Butcher")).findFirst().get();
                    final SpellAbility discard = garruk.getSpellAbilities().stream().filter(a -> a.getDescription().contains("discards two")).findFirst().get();
                    discard.setActivatingPlayer(c.ai);
                    c.resolve(discard);
                    final int left = c.opp.getCardsIn(ZoneType.Hand).size();
                    return left == 0 ? null : "the opponent still has " + left + " cards in hand";
                }));
        l.add(noPlay("4435", "Quicksilver Amulet for a 1-mana creature", Fid.EXACT, M1, true, "Quicksilver Amulet",
                c -> { c.bf("Island", 4, c.ai); c.bf("Quicksilver Amulet", c.ai); c.hand("Mother of Runes", c.ai); }));
        l.add(noPlay("5604", "abilities used as soon as possible for no reason (Mother of Runes, empty board)", Fid.APPROX, M1, true, "Mother of Runes",
                c -> { c.bf("Mother of Runes", c.ai); c.bf("Grizzly Bears", c.ai); }));
        l.add(noPlay("6391", "untapping Sleep-Cursed Faerie in response to any spell", Fid.EXACT, M1, false, "Sleep-Cursed Faerie",
                c -> { c.bf("Island", 3, c.ai); final Card f = c.bf("Sleep-Cursed Faerie", c.ai); f.setTapped(true); c.stack(c.opp, "Brainstorm"); }));
        l.add(noPlay("4348", "sacrificing Undercity Informer for nothing", Fid.APPROX, M1, true, "Undercity Informer",
                c -> { c.bf("Swamp", 2, c.ai); c.bf("Undercity Informer", c.ai); c.bf("Grizzly Bears", c.ai); }));
        // a creature is held for MAIN2 by the AI unless it has a reason to cast it first (PermanentAi "wait for Main2"), so test #7480 there
        l.add(new Scenario("7480", "two ETB triggers (Panharmonicon) must not pick the same target", Fid.APPROX, "MAIN2", true,
                c -> { c.bf("Mountain", 4, c.ai); c.bf("Panharmonicon", c.ai); c.hand("Flametongue Kavu", c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Grizzly Bears", c.opp); },
                c -> {
                    final SpellAbility sa = c.playOf("Flametongue Kavu");
                    if (sa == null) { return "not cast" + c.why("Flametongue Kavu"); }
                    c.resolve(sa);
                    return c.opp.getCreaturesInPlay().isEmpty() ? null : c.opp.getCreaturesInPlay().size() + " creature(s) survived (damage " + c.opp.getCreaturesInPlay().stream().map(x -> String.valueOf(x.getDamage())).collect(Collectors.joining("/")) + "), Kavu in " + c.ai.getCardsIn(forge.game.zone.ZoneType.Battlefield).stream().anyMatch(x -> x.getName().equals("Flametongue Kavu")) + " | stack:" + c.trace;
                }));
        l.add(play("7480", "control: plain Grizzly Bears with two Forests (is ANY creature cast in this harness?)", Fid.APPROX, "MAIN2", true, "Grizzly Bears",
                c -> { c.bf("Forest", 2, c.ai); c.hand("Grizzly Bears", c.ai); }));
        // A/B for #7480: the same board without Panharmonicon, and with only one creature to target
        l.add(play("7480", "control: Flametongue Kavu with two targets and NO Panharmonicon", Fid.APPROX, "MAIN2", true, "Flametongue Kavu",
                c -> { c.bf("Mountain", 4, c.ai); c.hand("Flametongue Kavu", c.ai); c.bf("Grizzly Bears", c.opp); c.bf("Grizzly Bears", c.opp); }));
        l.add(play("7480", "control: Flametongue Kavu with Panharmonicon and one target", Fid.APPROX, "MAIN2", true, "Flametongue Kavu",
                c -> { c.bf("Mountain", 4, c.ai); c.bf("Panharmonicon", c.ai); c.hand("Flametongue Kavu", c.ai); c.bf("Grizzly Bears", c.opp); }));

        // ---------------- combat
        l.add(new Scenario("4264", "attacking a player with protection from creatures", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Grizzly Bears", c.ai); c.effect(c.opp, "Teferi's Protection"); },
                c -> c.attackers().isEmpty() ? null : "attacked into protection"));
        // protection from a chosen player (Guardian Archon, Eon Frolicker, Noble Heritage): the valid list has a comma in it, which crashed the attack check
        l.add(new Scenario("protection-from-player", "attacking a player with protection from the attacking player (comma separated valid list)", Fid.EXACT, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Grizzly Bears", c.ai);
                    c.opp.addChangedKeywords(Arrays.asList("Protection:Player.PlayerUID_" + c.ai.getId() + ":" + c.ai.getName()), new ArrayList<String>(), 1L, 0L); },
                c -> c.attackers().isEmpty() ? null : "attacked into protection from the attacking player"));
        // The Mindskinner: its damage is prevented and milled instead, an attack still does something
        l.add(new Scenario("mindskinner", "The Mindskinner keeps attacking (its damage is replaced by a mill)", Fid.EXACT, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("The Mindskinner", c.ai); },
                c -> c.attackers().stream().anyMatch(k -> k.getName().equals("The Mindskinner")) ? null : "The Mindskinner stayed home"));
        l.add(new Scenario("mindskinner-token", "a creature other than The Mindskinner also attacks while the mill replacement is in play", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("The Mindskinner", c.ai); c.bf("Hill Giant", c.ai); },
                c -> c.attackers().stream().anyMatch(k -> k.getName().equals("Hill Giant")) ? null : "Hill Giant stayed home"));
        // Prismatic: the AI may spend any mana for it, so the colour of its lands must not decide whether it casts it
        l.add(new Scenario("prismatic", "casts a spell with Prismatic using lands of another colour (should play Duress)", Fid.EXACT, "MAIN2", true,
                c -> { c.bf("Island", 3, c.ai); c.hand("Lightning Bolt", c.opp);
                    final Card duress = c.hand("Duress", c.ai);
                    duress.addChangedCardKeywords(Arrays.asList("Prismatic"), new ArrayList<String>(), false, c.game.getNextTimestamp(), null); },
                c -> c.plays("Duress") ? null : "AI did not cast Duress with Prismatic and only Islands" + c.why("Duress")));
        l.add(noPlay("prismatic-control", "control for Prismatic: Duress without it is not payable with Islands", Fid.EXACT, "MAIN2", true, "Duress",
                c -> { c.bf("Island", 3, c.ai); c.hand("Lightning Bolt", c.opp); c.hand("Duress", c.ai); }));
        l.add(new Scenario("4265", "keeps a blocker home against Noble Hierarch's exalted", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.ai.setLife(3, null); c.bf("Grizzly Bears", c.ai); c.bf("Serra Angel", c.ai); c.bf("Noble Hierarch", c.opp); c.bf("Grizzly Bears", c.opp); },
                c -> c.attackers().size() >= 2 ? "attacked with everything while facing lethal exalted damage" : null));
        l.add(new Scenario("4301", "animates Mutavault to block and then actually blocks with it", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", false,
                c -> { c.ai.setLife(2, null); c.bf("Mutavault", c.ai); c.bf("Plains", c.ai); final Card a = c.bf("Hill Giant", c.opp);
                    final Combat combat = new Combat(c.opp); combat.addAttacker(a, c.ai); c.game.getPhaseHandler().setCombat(combat); },
                c -> {
                    final SpellAbility sa = c.playOf("Mutavault");
                    if (sa == null) { return "did not animate Mutavault"; }
                    c.resolve(sa);
                    final Card att = c.opp.getCardsIn(ZoneType.Battlefield).stream().filter(k -> k.getName().equals("Hill Giant")).findFirst().get();
                    return c.blockers(att).stream().anyMatch(k -> k.getName().equals("Mutavault")) ? null : "animated Mutavault but did not block";
                }));
        l.add(new Scenario("4360", "trampler attacks when the AI is low on life", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.ai.setLife(3, null); c.bf("Colossal Dreadmaw", c.ai); c.bf("Grizzly Bears", c.opp); },
                c -> c.attackers().stream().anyMatch(k -> k.getName().equals("Colossal Dreadmaw")) ? null : "trampler stayed home"));
        l.add(new Scenario("4656", "lone Signal Pest into an untapped blocker", Fid.EXACT, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Signal Pest", c.ai); c.bf("Serra Angel", c.opp); /* Signal Pest can only be blocked by flyers: the blocker has to fly */ },
                c -> c.attackers().isEmpty() ? null : "attacked with " + c.attackers()));

        // ---------------- target / value choices
        l.add(noPlay("4118", "removal spell aimed at its own creature (no opposing creature)", Fid.APPROX, M1, true, "Swords to Plowshares",
                c -> { c.bf("Plains", c.ai); c.hand("Swords to Plowshares", c.ai); c.bf("Grizzly Bears", c.ai); }));
        l.add(aimed("4129", "kill priority: Doran over a 6-toughness vanilla body", Fid.APPROX, M1, true, "Terminate", "Doran, the Siege Tower",
                c -> { c.bf("Swamp", c.ai); c.bf("Mountain", c.ai); c.hand("Terminate", c.ai); c.bf("Doran, the Siege Tower", c.opp); c.bf("Colossal Dreadmaw", c.opp); }));
        l.add(aimed("4171", "kill priority: Zulaport Cutthroat over Serrated Scorpion", Fid.APPROX, M1, true, "Terminate", "Zulaport Cutthroat",
                c -> { c.bf("Swamp", c.ai); c.bf("Mountain", c.ai); c.hand("Terminate", c.ai); c.bf("Serrated Scorpion", c.opp); c.bf("Zulaport Cutthroat", c.opp); }));
        l.add(play("4193", "answering the opponent's Oath of Nissa", Fid.APPROX, M1, true, "Disenchant",
                c -> { c.bf("Plains", 2, c.ai); c.hand("Disenchant", c.ai); c.bf("Oath of Nissa", c.opp); }));
        l.add(noPlay("4208", "suspended Rift Bolt when the player and creatures are untargetable", Fid.APPROX, M1, true, "Rift Bolt",
                c -> { c.bf("Mountain", c.ai); c.hand("Rift Bolt", c.ai); c.bf("Grizzly Bears", c.ai); c.bf("Leyline of Sanctity", c.opp); }));
        l.add(aimed("4225", "kill priority: Elvish Piper over Birds of Paradise", Fid.APPROX, M1, true, "Terminate", "Elvish Piper",
                c -> { c.bf("Swamp", c.ai); c.bf("Mountain", c.ai); c.hand("Terminate", c.ai); c.bf("Birds of Paradise", c.opp); c.bf("Elvish Piper", c.opp); }));
        l.add(new Scenario("4282", "tutor pile early in the game must not be all expensive cards", Fid.APPROX, "END_OF_TURN", false,
                c -> { c.bf("Island", 4, c.ai); c.hand("Gifts Ungiven", c.ai); c.lib("Wurmcoil Engine", c.ai); c.lib("Inferno Titan", c.ai); c.lib("Griselbrand", c.ai); c.lib("Craterhoof Behemoth", c.ai); /* (Emrakul shuffles the graveyard back into the library, the Island with it) */ c.lib("Island", c.ai); },
                c -> {
                    final SpellAbility sa = c.playOf("Gifts Ungiven");
                    if (sa == null) { return "Gifts not cast" + c.why("Gifts Ungiven"); }
                    c.resolve(sa);
                    // the pile has to contain the cheap card (the Island): whichever two cards the opponent leaves, the Island is in hand or in the graveyard, never still in the library
                    final boolean tookCheapCard = Stream.concat(c.ai.getCardsIn(ZoneType.Hand).stream(), c.ai.getCardsIn(ZoneType.Graveyard).stream()).anyMatch(k -> k.getCMC() == 0 && k.isLand());
                    return tookCheapCard ? null : "the pile was only top-end cards" + c.trace;
                }));
        l.add(noPlay("4300", "stealing the player's Pithing Needle that names its own card", Fid.APPROX, M1, true, "Steal Artifact",
                c -> { c.bf("Island", 4, c.ai); c.hand("Steal Artifact", c.ai); c.bf("Sol Ring", c.ai); final Card n = c.bf("Pithing Needle", c.opp); n.setNamedCards(Arrays.asList("Sol Ring")); }));
        l.add(new Scenario("5892", "Surgical Extraction takes the opposing key card (and its copies in hand and library), not its own most expensive one", Fid.APPROX, "END_OF_TURN", false, /* an instant graveyard hate card waits for the opponent's end step (ChangeZoneAi.isPreferredTarget) */
                c -> { c.bf("Swamp", c.ai); c.hand("Surgical Extraction", c.ai); c.gy("Inferno Titan", c.ai); c.gy("Reanimate", c.opp); c.hand("Reanimate", c.opp); c.lib("Reanimate", c.opp); },
                c -> {
                    final SpellAbility sa = c.playOf("Surgical Extraction");
                    if (sa == null) { return "Surgical Extraction not played" + c.why("Surgical Extraction"); }
                    final Card t = c.target(sa);
                    if (t == null || t.getOwner() != c.opp) { return "targeted " + t; }
                    c.resolve(sa);
                    for (final ZoneType z : new ZoneType[] { ZoneType.Graveyard, ZoneType.Hand, ZoneType.Library }) {
                        if (c.opp.getCardsIn(z).stream().anyMatch(k -> k.getName().equals("Reanimate"))) { return "a Reanimate was left in " + z; }
                    }
                    return c.ai.getCardsIn(ZoneType.Graveyard).stream().anyMatch(k -> k.getName().equals("Inferno Titan")) ? null : "exiled its own Inferno Titan";
                }));
        l.add(play("5894", "Tishana's Tidebinder against an activated ability", Fid.APPROX, M1, false, "Tishana's Tidebinder",
                c -> { c.bf("Island", 3, c.ai); c.hand("Tishana's Tidebinder", c.ai); final Card m = c.bf("Mother of Runes", c.opp); /* getSpellAbilities().get(0) is the creature spell: the tap ability is the one that goes on the stack */
                    final SpellAbility sa = m.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get(); sa.setActivatingPlayer(c.opp); sa.getTargets().add(m); c.game.getStack().add(sa);
                    if (c.game.getStack().isEmpty()) { throw new IllegalStateException("the opponent's ability did not reach the stack"); } }));
        l.add(new Scenario("4054", "burn into Unsettled Mariner with exactly one mana (cannot pay the tax)", Fid.EXACT, M1, true,
                c -> { c.bf("Mountain", c.ai); c.hand("Lightning Bolt", c.ai); c.bf("Unsettled Mariner", c.opp); },
                c -> { final Card t = c.target(c.playOf("Lightning Bolt")); return t != null && t.getName().equals("Unsettled Mariner") ? "targeted the Mariner" : null; }));
        l.add(new Scenario("4152", "does not play a permanent whose cost equals the Ratchet Bomb counters", Fid.APPROX, M1, true,
                c -> { c.bf("Plains", 2, c.ai); c.hand("Silvercoat Lion", c.ai); c.hand("Savannah Lions", c.ai); final Card b = c.bf("Ratchet Bomb", c.opp); c.counters(b, CounterEnumType.CHARGE, 2); },
                c -> { final SpellAbility sa = c.first(); return sa != null && sa.getHostCard().getName().equals("Silvercoat Lion") ? "walked into the bomb" : null; }));
        l.add(new Scenario("4152", "decides normally with The Filigree Sylex on the opposing side", Fid.SMOKE, M1, true,
                c -> { c.richMana(); c.hand("Grizzly Bears", c.ai); c.bf("The Filigree Sylex", c.opp); },
                c -> { c.plays(); return null; }));
        l.add(play("4162", "second The One Ring to reset the burden counters", Fid.EXACT, M1, true, "The One Ring",
                c -> { c.bf("Mountain", 4, c.ai); final Card r = c.bf("The One Ring", c.ai); c.counters(r, CounterEnumType.BURDEN, 5); c.hand("The One Ring", c.ai); }));
        l.add(noPlay("4324", "destroy effect on an indestructible creature (stand-in for Kaldra Compleat)", Fid.APPROX, M1, true, "Doom Blade",
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Doom Blade", c.ai); c.bf("Darksteel Colossus", c.opp); }));
        l.add(new Scenario("5603", "a hand of modal double-faced lands counts as lands in the mulligan decision", Fid.APPROX, M1, true,
                c -> { for (int i = 0; i < 20; i++) { c.lib("Mountain", c.ai); c.lib("Lightning Bolt", c.ai); } },
                c -> {
                    for (final String n : new String[] { "Agadeem's Awakening", "Emeria's Call", "Shatterskull Smashing", "Hagra Mauling", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt" }) { c.hand(n, c.ai); }
                    final DeckPlan plan = DeckPlan.forSetting("auto", c.ai);
                    return OpeningHand.keep(c.ai, c.ai.getCardsIn(ZoneType.Hand), 0, true, plan, 4) ? null : "mulliganed a hand with four MDFC lands";
                }));
        l.add(new Scenario("12929-lands-and-auras", "a hand of lands and Auras for creatures, no creature, is sent back", Fid.APPROX, M1, true,
                c -> { for (int i = 0; i < 20; i++) { c.lib("Plains", c.ai); c.lib("Grizzly Bears", c.ai); } },
                c -> {
                    for (final String n : new String[] { "Plains", "Plains", "Plains", "Holy Strength", "Holy Strength", "Rancor", "Holy Strength" }) { c.hand(n, c.ai); }
                    final DeckPlan plan = DeckPlan.forSetting("auto", c.ai);
                    return OpeningHand.keep(c.ai, c.ai.getCardsIn(ZoneType.Hand), 0, true, plan, 4) ? "kept lands and Auras without a creature" : null;
                }));
        l.add(noPlay("5860", "edict against a player with nothing to sacrifice", Fid.EXACT, M1, true, "Diabolic Edict",
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Diabolic Edict", c.ai); }));
        l.add(noPlay("5879", "free spell under the opponent's Roiling Vortex", Fid.EXACT, M1, true, "Snuff Out",
                c -> { c.bf("Swamp", c.ai); c.hand("Snuff Out", c.ai); c.bf("Roiling Vortex", c.opp); c.bf("Grizzly Bears", c.opp); }));
        l.add(new Scenario("5890", "As Foretold with a counter and Ancestral Vision in hand: pays {0} for the spell (should not only suspend it)", Fid.EXACT, "MAIN2", true,
                c -> { c.bf("Island", c.ai); final Card a = c.bf("As Foretold", c.ai); c.counters(a, CounterEnumType.TIME, 1); c.hand("Ancestral Vision", c.ai); },
                c -> c.plays().stream().anyMatch(sa -> sa.getHostCard().getName().equals("Ancestral Vision") && !sa.getDescription().contains("Suspend")) ? null : "AI did not cast Ancestral Vision for {0}: plays=" + c.plays() + c.why("Ancestral Vision")));
        l.add(new Scenario("5901", "Guide of Souls: pays the energy for its second ability when attacking", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("Guide of Souls", c.ai); c.bf("Grizzly Bears", c.ai); c.ai.setCounters(CounterEnumType.ENERGY, 6); },
                c -> {
                    final Combat combat = new Combat(c.ai);
                    c.ai.getController().declareAttackers(c.ai, combat);
                    c.game.getPhaseHandler().setCombat(combat);
                    if (combat.getAttackers().isEmpty()) { return "AI did not attack"; }
                    final Map<AbilityKey, Object> p = AbilityKey.newMap();
                    p.put(AbilityKey.Attackers, combat.getAttackers());
                    p.put(AbilityKey.AttackingPlayer, c.ai);
                    p.put(AbilityKey.AttackedTarget, new ArrayList<>(combat.getDefenders()));
                    c.game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.AttackersDeclared, p, false);
                    c.resolveAll();
                    return c.ai.getCounters(CounterEnumType.ENERGY) < 6 ? null : "energy never spent";
                }));
        l.add(new Scenario("5940", "Calibrated Blast's damage goes to the creature that gives the player hexproof (Shalai, Voice of Plenty)", Fid.APPROX, M1, true,
                c -> { c.bf("Mountain", 3, c.ai); /* {2}{R} */ c.hand("Calibrated Blast", c.ai); c.lib("Serra Angel", c.ai); /* the nonland card to reveal: 5 damage */
                    c.bf("Shalai, Voice of Plenty", c.opp); c.bf("Grizzly Bears", c.opp); },
                c -> {
                    final SpellAbility sa = c.playOf("Calibrated Blast");
                    if (sa == null) { return "AI did not play Calibrated Blast" + c.why("Calibrated Blast"); } // the ability has no target of its own: it is the revealed card's trigger that aims
                    c.resolve(sa);
                    // the revealed card's mana value is the damage: judge the aim (the trigger's target), not the library the harness filled
                    final boolean gone = c.opp.getCardsIn(ZoneType.Battlefield).stream().noneMatch(k -> k.getName().equals("Shalai, Voice of Plenty"));
                    return gone || c.trace.toString().contains("-> [Shalai") ? null : "damage not aimed at Shalai |" + c.trace;
                }));
        l.add(new Scenario("6031", "plays a different Urza's land than the one already in play", Fid.EXACT, M1, true,
                c -> { c.bf("Urza's Tower", c.ai); c.hand("Urza's Tower", c.ai); c.hand("Urza's Mine", c.ai); },
                c -> { final List<Card> p = c.landPlays(); return !p.isEmpty() && p.get(0).getName().equals("Urza's Mine") ? null : "first land drop: " + p; }));
        l.add(new Scenario("6105", "Suppression Field, not Stony Silence, against non-artifact abilities", Fid.EXACT, "MAIN2", true,
                c -> { c.bf("Plains", 2, c.ai); c.hand("Stony Silence", c.ai); c.hand("Suppression Field", c.ai); c.bf("Mother of Runes", c.opp); c.bf("Giver of Runes", c.opp); },
                c -> { final SpellAbility sa = c.first(); return sa != null && sa.getHostCard().getName().equals("Suppression Field") ? null : "first play was " + sa + c.why("Suppression Field") + c.why("Stony Silence"); }));
        l.add(play("6617", "noncreature spell before combat with a prowess creature", Fid.EXACT, M1, true, "Lightning Bolt",
                c -> { c.bf("Mountain", c.ai); c.bf("Monastery Swiftspear", c.ai); c.hand("Lightning Bolt", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(noPlay("7335", "sacrifice-at-end-of-turn haste creature into a player with protection from creatures", Fid.APPROX, M1, true, "Ball Lightning",
                c -> { c.bf("Mountain", 3, c.ai); c.hand("Ball Lightning", c.ai); c.effect(c.opp, "Teferi's Protection"); }));
        l.add(new Scenario("10922", "The Kingpin of Crime: pays 2 life for its attack trigger with two toughness-over-power creatures", Fid.APPROX, "COMBAT_DECLARE_ATTACKERS", true,
                c -> { c.bf("The Kingpin of Crime", c.ai); c.bf("Pegasus Courser", 2, c.ai); },
                c -> {
                    final Combat combat = new Combat(c.ai);
                    c.ai.getController().declareAttackers(c.ai, combat);
                    c.game.getPhaseHandler().setCombat(combat);
                    if (combat.getAttackers().isEmpty()) { return "AI did not attack"; }
                    final Map<AbilityKey, Object> p = AbilityKey.newMap();
                    p.put(AbilityKey.Attackers, combat.getAttackers());
                    p.put(AbilityKey.AttackingPlayer, c.ai);
                    p.put(AbilityKey.AttackedTarget, new ArrayList<>(combat.getDefenders()));
                    c.game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.AttackersDeclared, p, false);
                    c.resolveAll();
                    return c.ai.getLife() < 20 ? null : "life stayed at " + c.ai.getLife() + " with " + combat.getAttackers().size() + " attackers";
                }));
        l.add(play("12058", "Rise and Shine for its normal cost with a Darksteel Relic", Fid.APPROX, M1, true, "Rise and Shine",
                c -> { c.bf("Island", 2, c.ai); c.bf("Darksteel Relic", c.ai); c.hand("Rise and Shine", c.ai); }));
        l.add(play("12058", "solved Case of the Filched Falcon with noncreature artifacts to animate", Fid.APPROX, M1, true, "Case of the Filched Falcon",
                c -> { c.bf("Island", 3, c.ai); c.bf("Darksteel Relic", 2, c.ai); c.bf("Case of the Filched Falcon", c.ai).setSolved(true); }));
        // ---- Flat MC: equip {0}, counters, wipes, land wipes
        l.add(play("FMC-equip0", "Lightning Greaves (equip {0}) onto a creature that has none", Fid.APPROX, M1, true, "Lightning Greaves",
                c -> { c.bf("Lightning Greaves", c.ai); c.bf("Grizzly Bears", c.ai); c.bf("Grizzly Bears", c.opp); }));
        l.add(noPlay("FMC-equip0", "Lightning Greaves is not moved onto the creature it already equips", Fid.APPROX, M1, true, "Lightning Greaves",
                c -> { final Card g = c.bf("Lightning Greaves", c.ai); final Card b = c.bf("Grizzly Bears", c.ai); g.attachToEntity(b, null); c.bf("Grizzly Bears", c.opp); }));
        l.add(new Scenario("FMC-ozolith", "Ozolith, the Shattered Spire puts its counter on a creature, not on a noncreature artifact", Fid.APPROX, "MAIN1", true,
                c -> { c.bf("Forest", 2, c.ai); c.bf("Ozolith, the Shattered Spire", c.ai); c.bf("Sol Ring", c.ai); c.bf("Darksteel Relic", c.ai); c.bf("Grizzly Bears", c.ai); },
                c -> {
                    final SpellAbility sa = c.playOf("Ozolith, the Shattered Spire");
                    if (sa == null) { return "AI did not activate Ozolith" + c.why("Ozolith, the Shattered Spire"); }
                    final Card got = c.target(sa);
                    return got != null && got.isCreature() ? null : "counter went to " + got;
                }));
        l.add(play("FMC-wrath", "a wrath when the opponent's board is far better than the AI's", Fid.APPROX, "MAIN2", true, "Wrath of God",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Wrath of God", c.ai); c.bf("Grizzly Bears", c.ai);
                    c.bf("Serra Angel", c.opp); c.bf("Baneslayer Angel", c.opp); c.bf("Craterhoof Behemoth", c.opp); }));
        l.add(noPlay("FMC-wrath", "no wrath when the AI's own board is the better one", Fid.APPROX, "MAIN2", true, "Wrath of God",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Wrath of God", c.ai); c.bf("Serra Angel", c.ai); c.bf("Baneslayer Angel", c.ai);
                    c.bf("Grizzly Bears", c.opp); }));
        l.add(play("FMC-armageddon", "Armageddon when the AI's creatures are far ahead and the opponent is on a clock", Fid.APPROX, "MAIN2", true, "Armageddon",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Armageddon", c.ai); c.bf("Serra Angel", c.ai); c.bf("Baneslayer Angel", c.ai);
                    c.bf("Mountain", 5, c.opp); c.bf("Grizzly Bears", c.opp); c.opp.setLife(10, null); }));
        l.add(noPlay("FMC-armageddon", "no Armageddon when the opponent's creatures are ahead", Fid.APPROX, "MAIN2", true, "Armageddon",
                c -> { c.bf("Plains", 4, c.ai); c.hand("Armageddon", c.ai); c.bf("Grizzly Bears", c.ai);
                    c.bf("Mountain", 5, c.opp); c.bf("Serra Angel", c.opp); c.bf("Baneslayer Angel", c.opp); }));
        l.add(play("FMC-armageddon", "Armageddon when the AI's own lands are indestructible", Fid.APPROX, "MAIN2", true, "Armageddon",
                c -> { c.bf("Plains", c.ai); c.bf("Darksteel Citadel", 3, c.ai); c.hand("Armageddon", c.ai); c.hand("Grizzly Bears", c.ai);
                    c.bf("Mountain", 5, c.opp); c.hand("Serra Angel", c.opp); }));
        l.add(noPlay("12765", "a second Aether Vial next to one already in play", Fid.APPROX, M1, true, "Aether Vial",
                c -> { c.bf("Mountain", c.ai); final Card v = c.bf("Aether Vial", c.ai); c.counters(v, CounterEnumType.CHARGE, 1); c.hand("Aether Vial", c.ai); }));
        l.add(noPlay("12766", "Aura on its creature while the opponent's Spellskite can redirect it", Fid.APPROX, M1, true, "Rancor",
                c -> { c.bf("Forest", c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Rancor", c.ai); c.bf("Spellskite", c.opp); }));
        // a deck that is not built around the graveyard: Nethergoyf (escape) is a card to cast, not one to Thoughtseize away
        l.add(new Scenario("12888", "Thoughtseize aimed at the AI itself to discard Nethergoyf", Fid.EXACT, "MAIN2", true,
                c -> { c.bf("Swamp", 2, c.ai); c.hand("Thoughtseize", c.ai); c.hand("Thoughtseize", c.ai); c.hand("Nethergoyf", c.ai); c.hand("Lightning Bolt", c.opp); },
                c -> {
                    final SpellAbility sa = c.first();
                    if (sa == null || !sa.getHostCard().getName().equals("Thoughtseize") || !sa.usesTargeting()) { return null; }
                    for (final forge.game.GameEntity e : sa.getTargets().getTargetEntities()) {
                        if (e == c.ai) { return "Thoughtseize aimed at the AI itself"; }
                    }
                    return null;
                }));
        l.add(new Scenario("estrid-mask-own-target", "Estrid's Mask uses its friendly attachment preference instead of enchanting an opposing permanent", Fid.EXACT, M1, true,
                c -> {
                    final Card estrid = c.bf("Estrid, the Masked", c.ai);
                    c.counters(estrid, CounterEnumType.LOYALTY, 3);
                    c.bf("Grizzly Bears", c.ai);
                    c.bf("Baneslayer Angel", c.opp);
                },
                c -> {
                    final SpellAbility sa = c.tokenAbility("Estrid, the Masked");
                    if (sa == null) { return "Estrid did not activate its Mask ability" + c.why("Estrid, the Masked"); }
                    final Card target = c.target(sa);
                    return target != null && target.getController() == c.ai ? null : "Mask targeted " + target + " instead of an AI permanent";
                }));
        l.add(new Scenario("estrid-mask-no-friendly-target", "Estrid does not spend loyalty to give an opponent an Umbra Armor Aura when it has no friendly target", Fid.EXACT, M1, true,
                c -> {
                    final Card estrid = c.bf("Estrid, the Masked", c.ai);
                    c.counters(estrid, CounterEnumType.LOYALTY, 3);
                    c.bf("Baneslayer Angel", c.opp);
                },
                c -> {
                    final SpellAbility sa = c.tokenAbility("Estrid, the Masked");
                    return sa == null ? null : "used the Mask ability on " + c.target(sa);
                }));
        return l;
    }

    // ------------------------------------------------------------------------------------------ runner

    @DataProvider(name = "scenarios")
    public Object[][] provide() {
        final List<Object[]> rows = new ArrayList<>();
        for (final Scenario s : scenarios()) {
            if (ONLY.isEmpty() || ONLY.equals(s.issue)) { rows.add(new Object[] { s }); }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "scenarios")
    public void aiBehavesAsReported(final Scenario s) {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = FLAT_MC;
        if (System.getProperty("forge.test.issue") != null) { forge.ai.simulation.FlatMonteCarloPicker.DEBUG = true; } // a single scenario: print what the picker made of it
        game.AI_TIMEOUT = 10;
        final Ctx c = new Ctx(game);
        if (FLAT_MC) { fillLibrary(c.ai, 20); fillLibrary(c.opp, 20); } // rollouts must not deck anyone
        if (!PROFILE.isEmpty()) {
            ((LobbyPlayerAi) c.ai.getLobbyPlayer()).setAiProfile(PROFILE);
        }
        String failure;
        try {
            // phase first: devModeSet ends any combat, so a combat built by the setup must come after it
            game.getPhaseHandler().devModeSet(PhaseType.valueOf(s.phase), s.aiTurn ? c.ai : c.opp);
            s.setup.run(c);
            game.getAction().checkStateEffects(true);
            failure = s.check.run(c);
        } catch (final SkipException e) {
            SUMMARY.add("SKIP   " + s + " -> " + e.getMessage());
            throw e;
        } catch (final Throwable t) {
            SUMMARY.add("ERROR  " + s + " -> " + t);
            throw t;
        }
        SUMMARY.add((failure == null ? "OK     " : "BUG    ") + s + (failure == null ? "" : " -> " + failure));
        AssertJUnit.assertNull(s + (failure == null ? "" : " -> " + failure), failure); // the reason is in the assertion, not only in the summary
    }

    @AfterClass
    public void printSummary() {
        System.out.println("===== AI report scenarios (profile: " + (PROFILE.isEmpty() ? "default" : PROFILE) + ", FlatMC: " + FLAT_MC + ") =====");
        for (final String line : SUMMARY) { System.out.println(line); }
    }
}
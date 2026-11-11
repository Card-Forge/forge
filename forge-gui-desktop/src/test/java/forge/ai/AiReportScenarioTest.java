package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.OpeningHand;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
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

        /** Plenty of mana of every colour for the AI. */
        void richMana() { for (final String l : new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" }) { bf(l, 3, ai); } }
        void counters(final Card c, final CounterEnumType t, final int n) { c.setCounters(t, n); }

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
                                if (ex != null) { sb.append(" replacement:").append(ex.getApi()).append("=").append(controller().doTrigger(ex.copy(ai), false))
                                        .append(" decision=").append(SpellApiToAi.Converter.get(ex).doTriggerNoCostWithSubs(ai, ex.copy(ai), false))
                                        .append(" opposing permanents with an activated ability=").append(forge.game.card.CardLists.getValidCards(
                                                ai.getOpponents().getCardsIn(ZoneType.Battlefield), "Card.OppCtrl+hasNonManaActivatedAbility", ai, k, ex)); }
                            }
                            for (final forge.game.trigger.Trigger tr : k.getTriggers()) { // each enter-the-battlefield trigger, one ability of its chain at a time
                                if (tr.getMode() != forge.game.trigger.TriggerType.ChangesZone || !"Battlefield".equals(tr.getParam("Destination"))) { continue; }
                                sb.append(" trigger(stack=").append(game.getStack().size()).append("):");
                                for (SpellAbility ex = tr.ensureAbility().copy(ai); ex != null; ex = ex.getSubAbility()) {
                                    sb.append(' ').append(ex.getApi()).append('=').append(controller().doTrigger(ex, false));
                                }
                            }
                            sb.append(']');
                        } catch (final RuntimeException e) { sb.append(" [etbCheck threw ").append(e).append(']'); }
                    }
                    for (final SpellAbility sa : k.getSpellAbilities()) {
                        try {
                            sa.setActivatingPlayer(ai);
                            sb.append(" [").append(z).append(' ').append(sa.getApi()).append(" -> ").append(controller().canPlaySa(sa))
                                    .append(" payable=").append(ComputerUtilCost.canPayCost(sa, ai, false)).append(" failedBefore=").append(controller().hasFailedToCast(sa)).append(']');
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

    private static Scenario skip(final String id, final String t, final String why) {
        return new Scenario(id, t, Fid.SMOKE, "MAIN1", true, c -> { }, c -> { throw new SkipException(why); });
    }

    // ------------------------------------------------------------------------------------------ scenarios

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
        l.add(skip("4113", "cascaded/free Glimpse of Tomorrow is resolved (not just exiled)", "needs a real cascade; playNoStack does not model it"));
        l.add(play("4210", "Violent Outburst with no creature to attack", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); /* {1}{R}{G} */ c.hand("Violent Outburst", c.ai); c.lib("Lightning Bolt", c.ai); c.lib("Mountain", c.ai); }));
        // the report: a Living End deck casts it for the cascade, once the graveyard is ready (ComboPlan decides, see massReanimationRuling)
        l.add(play("4210", "Living End deck, five creatures in the graveyard, no creature to pump", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); c.hand("Violent Outburst", c.ai); c.lib("Living End", c.ai); c.lib("Lightning Bolt", c.ai);
                    for (int i = 0; i < 5; i++) { c.gy("Grizzly Bears", c.ai); } }));
        l.add(noPlay("4210", "Living End deck, empty graveyard: the cascade would waste the payoff", Fid.APPROX, M1, true, "Violent Outburst",
                c -> { c.bf("Mountain", 2, c.ai); c.bf("Forest", c.ai); c.hand("Violent Outburst", c.ai); c.lib("Living End", c.ai); c.lib("Lightning Bolt", c.ai); }));
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
                    c.bf("Meddling Mage", c.ai).setNamedCards(java.util.Arrays.asList("Birds of Paradise"));
                },
                c -> {
                    final Card second = c.bf("Meddling Mage", c.ai);
                    final SpellAbility naming = second.getReplacementEffects().get(0).getOverridingAbility();
                    naming.setActivatingPlayer(c.ai);
                    final String name = ((PlayerControllerAi) c.ai.getController()).chooseCardName(naming, f -> true, "Card.nonLand", "");
                    return "Birds of Paradise".equals(name) ? "named Birds of Paradise again" : null;
                }));
        l.add(new Scenario("5896", "a second Pithing Needle names a different card than the first", Fid.APPROX, M1, true,
                c -> {
                    c.bf("Spellskite", c.opp); c.bf("Agatha's Soul Cauldron", c.opp); c.bf("Shalai, Voice of Plenty", c.opp);
                    c.bf("Pithing Needle", c.ai).setNamedCards(java.util.Arrays.asList("Shalai, Voice of Plenty"));
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
                    return c.opp.getCreaturesInPlay().isEmpty() ? null : c.opp.getCreaturesInPlay().size() + " creature(s) survived (damage " + c.opp.getCreaturesInPlay().stream().map(x -> String.valueOf(x.getDamage())).collect(java.util.stream.Collectors.joining("/")) + "), Kavu in " + c.ai.getCardsIn(forge.game.zone.ZoneType.Battlefield).stream().anyMatch(x -> x.getName().equals("Flametongue Kavu")) + " | stack:" + c.trace;
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
        l.add(new Scenario("4282", "tutor pile early in the game must not be all expensive cards", Fid.APPROX, M1, true,
                c -> { c.bf("Island", 4, c.ai); c.hand("Gifts Ungiven", c.ai); c.lib("Wurmcoil Engine", c.ai); c.lib("Inferno Titan", c.ai); c.lib("Griselbrand", c.ai); c.lib("Craterhoof Behemoth", c.ai); /* (Emrakul shuffles the graveyard back into the library, the Island with it) */ c.lib("Island", c.ai); },
                c -> {
                    final SpellAbility sa = c.playOf("Gifts Ungiven");
                    if (sa == null) { return "Gifts not cast" + c.why("Gifts Ungiven"); }
                    c.resolve(sa);
                    // the pile has to contain the cheap card (the Island): whichever two cards the opponent leaves, the Island is in hand or in the graveyard, never still in the library
                    final boolean tookCheapCard = java.util.stream.Stream.concat(c.ai.getCardsIn(ZoneType.Hand).stream(), c.ai.getCardsIn(ZoneType.Graveyard).stream()).anyMatch(k -> k.getCMC() == 0 && k.isLand());
                    return tookCheapCard ? null : "the pile was only top-end cards" + c.trace;
                }));
        l.add(noPlay("4300", "stealing the player's Pithing Needle that names its own card", Fid.APPROX, M1, true, "Steal Artifact",
                c -> { c.bf("Island", 4, c.ai); c.hand("Steal Artifact", c.ai); c.bf("Sol Ring", c.ai); final Card n = c.bf("Pithing Needle", c.opp); n.setNamedCards(java.util.Arrays.asList("Sol Ring")); }));
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
                    final java.util.Map<forge.game.ability.AbilityKey, Object> p = forge.game.ability.AbilityKey.newMap();
                    p.put(forge.game.ability.AbilityKey.Attackers, combat.getAttackers());
                    p.put(forge.game.ability.AbilityKey.AttackingPlayer, c.ai);
                    p.put(forge.game.ability.AbilityKey.AttackedTarget, new ArrayList<>(combat.getDefenders()));
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
        l.add(skip("10922", "The Kingpin of Crime life payment on its trigger", "needs the card script: which trigger asks for the 2 life (paste the Forge script and I will add the setup)"));
        l.add(skip("12058", "\"Target artifact you control becomes a 0/0 artifact creature. Put X +1/+1 counters on it\"", "the report gives no card name: tell me which card and I will add it"));
        l.add(noPlay("12765", "a second Aether Vial next to one already in play", Fid.APPROX, M1, true, "Aether Vial",
                c -> { c.bf("Mountain", c.ai); final Card v = c.bf("Aether Vial", c.ai); c.counters(v, CounterEnumType.CHARGE, 1); c.hand("Aether Vial", c.ai); }));
        l.add(noPlay("12766", "Aura on its creature while the opponent's Spellskite can redirect it", Fid.APPROX, M1, true, "Rancor",
                c -> { c.bf("Forest", c.ai); c.bf("Grizzly Bears", c.ai); c.hand("Rancor", c.ai); c.bf("Spellskite", c.opp); }));
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
        AssertJUnit.assertNull(s.toString(), failure);
    }

    @AfterClass
    public void printSummary() {
        System.out.println("===== AI report scenarios (profile: " + (PROFILE.isEmpty() ? "default" : PROFILE) + ", FlatMC: " + FLAT_MC + ") =====");
        for (final String line : SUMMARY) { System.out.println(line); }
    }
}
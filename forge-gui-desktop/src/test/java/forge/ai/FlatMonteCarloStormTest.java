package forge.ai;

import java.util.Arrays;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tutor storm decks (Beseech Storm): the deck is read as a combo, the finisher is held until the storm count makes it
 * lethal, and tutors find what the position needs.
 */
public class FlatMonteCarloStormTest extends AITest {

    /** The main deck of "[Legacy] Beseech Storm" (60 cards). */
    private static final Object[] BESEECH_STORM = {
            "Ad Nauseam", 1, "Beseech the Mirror", 2, "Burning Wish", 2, "Chrome Mox", 4, "City of Traitors", 1,
            "Dark Ritual", 4, "Duress", 2, "Gaea's Will", 1, "Giant's Boulder", 4, "Infernal Tutor", 4,
            "Lion's Eye Diamond", 4, "Lotus Petal", 4, "Mox Opal", 4, "Skateboard", 1, "Song of Creation", 1,
            "Spire of Industry", 3, "Tendrils of Agony", 1, "Thoughtseize", 4, "Urza's Saga", 4, "Vault of Whispers", 4,
            "Veil of Summer", 2, "Wishclaw Talisman", 3 };

    private Game newGame() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = false; // the heuristics and the deck plan only: deterministic
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private Player dealBeseechStorm(final Game game) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < BESEECH_STORM.length; i += 2) {
            for (int n = 0; n < (Integer) BESEECH_STORM[i + 1]; n++) {
                addCardToZone((String) BESEECH_STORM[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    /** Pretend that this many spells were cast this turn (the storm count). */
    private static void setStormCount(final Game game, final Card any, final int count) {
        game.getStack().getSpellsCastThisTurn().clear();
        for (int i = 0; i < count; i++) {
            game.getStack().getSpellsCastThisTurn().add(new SpellAbility.EmptySa(ApiType.Draw, any));
        }
    }

    private static String nextPlay(final Player ai) {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    private static Card fromLibrary(final Player p, final String name) {
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new AssertionError(name + " not in the library");
    }

    private static void toHand(final Player p, final Card c) {
        p.getZone(ZoneType.Library).remove(c);
        p.getZone(ZoneType.Hand).add(c);
    }

    @Test
    public void beseechStormIsReadAsATutorStormCombo() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        System.out.println("[FlatMonteCarloStormTest] plan: " + plan.describe());
        AssertJUnit.assertEquals(DeckPlan.Archetype.COMBO, plan.archetype);
        AssertJUnit.assertTrue("TUTOR_STORM expected in " + plan.combos, plan.combos.has(ComboPlan.Kind.TUTOR_STORM));
        for (final String piece : new String[] { "Tendrils of Agony", "Infernal Tutor", "Beseech the Mirror",
                "Burning Wish", "Wishclaw Talisman", "Dark Ritual", "Lotus Petal", "Lion's Eye Diamond",
                "Chrome Mox", "Gaea's Will" }) {
            AssertJUnit.assertTrue(piece + " should be a piece of the combo",
                    plan.combos.pieceNames().contains(piece));
        }
    }

    @Test
    public void ordinaryDecksAreNotTutorStormDecks() {
        final Game game = newGame();
        final Player p = game.getPlayers().get(1);
        for (final String n : new String[] { "Lightning Bolt", "Goblin Guide", "Monastery Swiftspear", "Chain Lightning" }) {
            for (int i = 0; i < 4; i++) {
                addCardToZone(n, p, ZoneType.Library);
            }
        }
        for (int i = 0; i < 16; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
        }
        AssertJUnit.assertFalse(DeckPlan.forSetting("auto", p).combos.has(ComboPlan.Kind.TUTOR_STORM));
    }

    @Test
    public void tendrilsIsLethalOnlyWhenTheStormCountIsHighEnough() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        final Card tendrils = fromLibrary(p, "Tendrils of Agony");
        toHand(p, tendrils);
        opp.setLife(20, null);
        // the spell and every copy take 2: (storm + 1) * 2 >= 20 from a storm count of 9
        for (int storm = 0; storm <= 8; storm++) {
            setStormCount(game, tendrils, storm);
            AssertJUnit.assertFalse("storm " + storm + " is not lethal against 20", ComboPlan.stormIsLethal(p, tendrils));
        }
        setStormCount(game, tendrils, 9);
        AssertJUnit.assertTrue("storm 9 is lethal against 20", ComboPlan.stormIsLethal(p, tendrils));
        opp.setLife(12, null);
        setStormCount(game, tendrils, 5);
        AssertJUnit.assertTrue("storm 5 is lethal against 12", ComboPlan.stormIsLethal(p, tendrils));
        setStormCount(game, tendrils, 4);
        AssertJUnit.assertFalse("storm 4 is not lethal against 12", ComboPlan.stormIsLethal(p, tendrils));
    }

    @Test
    public void notLethalTendrilsIsHeldAndLethalTendrilsIsCast() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        final Card tendrils = fromLibrary(p, "Tendrils of Agony");
        toHand(p, tendrils);
        toHand(p, fromLibrary(p, "Duress")); // another spell in the hand, so Tendrils is not simply the only play
        addCards("Swamp", 5, p);
        opp.setLife(20, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        setStormCount(game, tendrils, 3);
        final String early = nextPlay(p);
        System.out.println("[FlatMonteCarloStormTest] storm 3 -> " + early);
        AssertJUnit.assertFalse("Tendrils at storm 3 (8 damage of 20) must wait", "Tendrils of Agony".equals(early));

        setStormCount(game, tendrils, 9);
        final String lethal = nextPlay(p);
        System.out.println("[FlatMonteCarloStormTest] storm 9 -> " + lethal);
        AssertJUnit.assertEquals("Tendrils at storm 9 is lethal, even in the first main phase", "Tendrils of Agony", lethal);
    }

    @Test
    public void beseechFindsTheEngineWhenTheGraveyardHasSpellsToCastAgain() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        DeckPlan.forSetting("auto", p);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        addCardToZone("Dark Ritual", p, ZoneType.Graveyard);
        addCardToZone("Lotus Petal", p, ZoneType.Graveyard);
        addCardToZone("Chrome Mox", p, ZoneType.Graveyard);
        final CardCollection options = new CardCollection();
        for (final String n : new String[] { "Gaea's Will", "Tendrils of Agony", "Dark Ritual", "Thoughtseize" }) {
            options.add(fromLibrary(p, n));
        }
        // Beseech the Mirror exiles the card face down and casts it for free: mana value 4 or less
        final Card pick = combos.choose(p, ZoneType.Exile, Arrays.asList(ZoneType.Library), options);
        AssertJUnit.assertNotNull(pick);
        AssertJUnit.assertEquals("Gaea's Will", pick.getName());
    }

    @Test
    public void aTutorDoesNotFetchTheFinisherBeforeTheSetupIsComplete() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        addCards("Swamp", 5, p);
        game.getAction().checkStateEffects(true);
        final CardCollection options = new CardCollection();
        for (final String n : new String[] { "Tendrils of Agony", "Dark Ritual", "Thoughtseize" }) {
            options.add(fromLibrary(p, n));
        }
        final Card pick = combos.choose(p, ZoneType.Hand, Arrays.asList(ZoneType.Library), options);
        AssertJUnit.assertEquals("Dark Ritual", pick.getName());
    }

    @Test
    public void aTutorFindsTheFinisherWhenThereIsManaForIt() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        addCards("Swamp", 5, p); // five untapped sources
        game.getAction().checkStateEffects(true);
        game.getPlayers().get(0).setLife(20, null);
        setStormCount(game, fromLibrary(p, "Duress"), 9);
        final CardCollection options = new CardCollection();
        for (final String n : new String[] { "Gaea's Will", "Tendrils of Agony", "Dark Ritual", "Thoughtseize" }) {
            options.add(fromLibrary(p, n));
        }
        final Card pick = combos.choose(p, ZoneType.Hand, Arrays.asList(ZoneType.Library), options);
        AssertJUnit.assertNotNull(pick);
        AssertJUnit.assertEquals("Tendrils of Agony", pick.getName());
    }

    @Test
    public void aTutorFindsManaWhenThereIsNoneAndNoFinisherYet() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final CardCollection options = new CardCollection();
        for (final String n : new String[] { "Tendrils of Agony", "Dark Ritual", "Thoughtseize", "Duress" }) {
            options.add(fromLibrary(p, n));
        }
        final Card pick = combos.choose(p, ZoneType.Hand, Arrays.asList(ZoneType.Library), options);
        AssertJUnit.assertNotNull(pick);
        // nothing on the table: a finisher cannot be cast, mana comes first
        AssertJUnit.assertEquals("Dark Ritual", pick.getName());
    }

    @Test
    public void progressGrowsWithManaAndStormAndVanishesWithTheFinisher() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        opp.setLife(20, null);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final Card tendrils = fromLibrary(p, "Tendrils of Agony");
        toHand(p, tendrils);
        setStormCount(game, tendrils, 0);
        final int bare = combos.term(p);

        addCards("Lotus Petal", 3, p);
        game.getAction().checkStateEffects(true);
        final int withMana = combos.term(p);

        setStormCount(game, tendrils, 5);
        final int withStorm = combos.term(p);
        System.out.println("[FlatMonteCarloStormTest] term: bare=" + bare + " mana=" + withMana + " storm=" + withStorm);
        AssertJUnit.assertTrue("mana is progress", withMana > bare);
        AssertJUnit.assertTrue("a storm count is progress", withStorm > withMana);

        // the finisher exiled (a Chrome Mox imprint, discard) and no tutor in hand to find another: nothing to prepare
        p.getZone(ZoneType.Hand).remove(tendrils);
        p.getZone(ZoneType.Exile).add(tendrils);
        for (final Card c : new CardCollection(p.getCardsIn(ZoneType.Library))) {
            if (c.getName().equals("Tendrils of Agony")) {
                p.getZone(ZoneType.Library).remove(c);
            }
        }
        AssertJUnit.assertEquals(0, combos.term(p));
    }

    @Test
    public void theSolverFindsAWinningLineAndFollowsIt() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        toHand(p, fromLibrary(p, "Tendrils of Agony"));
        toHand(p, fromLibrary(p, "Lotus Petal"));
        toHand(p, fromLibrary(p, "Lotus Petal"));
        addCards("Swamp", 3, p);
        opp.setLife(4, null); // two copies are enough: a storm count of one
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        setStormCount(game, fromLibrary(p, "Duress"), 0);

        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final forge.ai.simulation.StormSolver.Line line =
                forge.ai.simulation.StormSolver.solve(p, combos, 200, 20_000_000_000L);
        AssertJUnit.assertNotNull("a Petal and then Tendrils wins", line);
        System.out.println("[FlatMonteCarloStormTest] line " + line.steps + " nodes=" + line.nodes);
        AssertJUnit.assertEquals("Lotus Petal", line.steps.get(0).host);
        AssertJUnit.assertEquals("Tendrils of Agony", line.steps.get(line.steps.size() - 1).host);

        final forge.ai.simulation.StormSolver solver = new forge.ai.simulation.StormSolver();
        final SpellAbility first = solver.next(p, combos);
        AssertJUnit.assertNotNull(first);
        AssertJUnit.assertEquals("Lotus Petal", first.getHostCard().getName());
    }

    @Test
    public void theSolverFindsNothingWhenThereIsNoWin() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        toHand(p, fromLibrary(p, "Tendrils of Agony"));
        addCards("Swamp", 4, p);
        opp.setLife(20, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        setStormCount(game, fromLibrary(p, "Duress"), 0);

        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        AssertJUnit.assertNull(forge.ai.simulation.StormSolver.solve(p, combos, 50, 5_000_000_000L));
        AssertJUnit.assertNull(new forge.ai.simulation.StormSolver().next(p, combos));
    }

    /**
     * The pattern of the deck on a hand that can do it: Beseech the Mirror (bargained with Wishclaw Talisman) finds and
     * casts Gaea's Will for free, and Tendrils of Agony, held back, is cast last. If this fails the solver does not
     * know the line; the game runs of StormGoldfishTest then say nothing about the AI until it passes.
     */
    @Test
    public void theSolverFindsTheBeseechLine() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Player opp = game.getPlayers().get(0);
        toHand(p, fromLibrary(p, "Beseech the Mirror"));
        toHand(p, fromLibrary(p, "Dark Ritual"));
        toHand(p, fromLibrary(p, "Tendrils of Agony"));
        addCards("Swamp", 6, p); // Dark Ritual makes it 8 mana: Beseech (4) and Tendrils (4)
        addCard("Wishclaw Talisman", p); // something to sacrifice for the Bargain
        opp.setLife(8, null); // Tendrils with a storm count of three: four copies of two life
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        setStormCount(game, fromLibrary(p, "Duress"), 0);

        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final forge.ai.simulation.StormSolver.Line line =
                forge.ai.simulation.StormSolver.solve(p, combos, 400, 20_000_000_000L);
        System.out.println("[FlatMonteCarloStormTest] beseech line " + (line == null ? null : line.steps + " nodes=" + line.nodes));
        AssertJUnit.assertNotNull("Dark Ritual, Beseech (Gaea's Will for free), Tendrils is a kill; the solver saw: "
                + forge.ai.simulation.StormSolver.lastDiagnostic(), line);
        AssertJUnit.assertEquals("Tendrils of Agony", line.steps.get(line.steps.size() - 1).host);
    }

    @Test
    public void cheapFodderIsHeldUnlessTheBargainNeedsIt() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final Card boulder = fromLibrary(p, "Giant's Boulder");
        toHand(p, boulder);
        toHand(p, fromLibrary(p, "Duress"));
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final SpellAbility cast = boulder.getSpellAbilities().get(0);
        cast.setActivatingPlayer(p);
        AssertJUnit.assertTrue("Giant's Boulder for nothing is a wasted card", combos.wastesFodder(cast));

        toHand(p, fromLibrary(p, "Beseech the Mirror"));
        AssertJUnit.assertFalse("with Beseech waiting and nothing to sacrifice, it is the Bargain", combos.wastesFodder(cast));
        addCard("Lotus Petal", p);
        AssertJUnit.assertTrue("with something to sacrifice already, it is held again", combos.wastesFodder(cast));
    }

    @Test
    public void theMulliganKeepsHandsThatCanActAndSendsBackSlowOnes() {
        final Game game = newGame();
        final Player p = dealBeseechStorm(game);
        final ComboPlan combos = DeckPlan.forSetting("auto", p).combos;
        final CardCollection slow = new CardCollection();
        for (final String n : new String[] { "Vault of Whispers", "Thoughtseize", "Duress", "Veil of Summer", "Veil of Summer", "Thoughtseize", "Duress" }) {
            slow.add(addCard(n, p));
        }
        AssertJUnit.assertFalse("only disruption", combos.keepsTutorStormHand(slow, 7));
        final CardCollection fast = new CardCollection();
        for (final String n : new String[] { "Vault of Whispers", "Beseech the Mirror", "Lotus Petal", "Dark Ritual", "Duress", "Chrome Mox", "Infernal Tutor" }) {
            fast.add(addCard(n, p));
        }
        AssertJUnit.assertTrue("two tutors and mana", combos.keepsTutorStormHand(fast, 7));
        AssertJUnit.assertTrue("a five card hand is kept whatever it holds", combos.keepsTutorStormHand(slow, 5));
    }
}

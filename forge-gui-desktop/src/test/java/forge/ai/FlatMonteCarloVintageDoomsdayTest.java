package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.DoomsdayPile;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Doomsday with Thassa's Oracle: when it is cast, which five cards it keeps, and how the deck is read. */
public class FlatMonteCarloVintageDoomsdayTest extends AITest {
    private Game game;
    private Player ai;

    /** The real Doomsday list is the library; the AI is the second player. */
    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (final String line : FlatMonteCarloVintageMetaTest.DOOMSDAY.split("\n")) {
            final int space = line.indexOf(' ');
            if (space > 0) {
                for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                    addCardToZone(line.substring(space + 1).trim(), ai, ZoneType.Library);
                }
            }
        }
    }

    private ComboPlan plan() {
        return DeckPlan.forSetting("auto", ai).combos;
    }

    @Test
    public void theDoomsdayListIsReadAsADoomsdayDeck() {
        setUp();
        final ComboPlan combos = plan();
        AssertJUnit.assertTrue("Doomsday should be read as DOOMSDAY, the scan found: " + combos.summary()
                + " (look at doomsday in ComboPlan, and the doomsday roles)", combos.has(ComboPlan.Kind.DOOMSDAY));
    }

    @Test
    public void doomsdayIsNotCastWithoutTheOracleInHand() {
        setUp();
        addCards("Swamp", 3, ai);
        final Card doomsday = addCardToZone("Doomsday", ai, ZoneType.Hand);
        final SpellAbility sa = doomsday.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("doomsday-not-ready", ComboGuard.problem(ai, sa, null, plan()));
    }

    @Test
    public void doomsdayIsCastWithTheOracleInHand() {
        setUp();
        addCards("Swamp", 3, ai);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final Card doomsday = addCardToZone("Doomsday", ai, ZoneType.Hand);
        final SpellAbility sa = doomsday.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertNull("with the Oracle in hand nothing is against it", ComboGuard.problem(ai, sa, null, plan()));
    }

    @Test
    public void doomsdayIsNotCastWithTooLittleLife() {
        setUp();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final Card doomsday = addCardToZone("Doomsday", ai, ZoneType.Hand);
        ai.setLife(3, null);
        final SpellAbility sa = doomsday.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("doomsday-not-ready", ComboGuard.problem(ai, sa, null, plan()));
    }

    private CardCollection options(final String... names) {
        final CardCollection list = new CardCollection();
        for (final String name : names) {
            list.add(addCardToZone(name, ai, ZoneType.Library));
        }
        return list;
    }

    @Test
    public void thePileIsTheCardsThatDrawTheMost() {
        setUp();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final CardCollection options = options("Island", "Dark Ritual", "Gitaxian Probe", "Ancestral Recall", "Gush", "Brainstorm");
        final Card first = DoomsdayPile.pick(ai, options);
        AssertJUnit.assertNotNull(first);
        AssertJUnit.assertEquals("Ancestral Recall draws three for one mana", "Ancestral Recall", first.getName());
        options.remove(first);
        AssertJUnit.assertEquals("Gush draws two", "Gush", DoomsdayPile.pick(ai, options).getName());
        options.remove(DoomsdayPile.pick(ai, options));
        AssertJUnit.assertEquals("then the free draw", "Gitaxian Probe", DoomsdayPile.pick(ai, options).getName());
        options.remove(DoomsdayPile.pick(ai, options));
        AssertJUnit.assertEquals("then mana", "Dark Ritual", DoomsdayPile.pick(ai, options).getName());
    }

    @Test
    public void theOracleGoesIntoThePileWhenNoneIsInHand() {
        setUp();
        final CardCollection options = options("Island", "Gitaxian Probe", "Thassa's Oracle");
        AssertJUnit.assertEquals("Thassa's Oracle", DoomsdayPile.pick(ai, options).getName());
    }

    @Test
    public void aCutLibraryWithTheOracleInHandIsAlmostTheGame() {
        setUp();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("a full library is no progress", 0, DoomsdayPile.term(ai));
        while (ai.getCardsIn(ZoneType.Library).size() > 5) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        AssertJUnit.assertTrue("five cards left with the Oracle in hand", DoomsdayPile.term(ai) >= 200);
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Hand))) {
            ai.getZone(ZoneType.Hand).remove(c);
        }
        AssertJUnit.assertTrue("five cards left and nothing to win with", DoomsdayPile.term(ai) < 0);
    }

    /** The whole turn with the real list: Doomsday is cast with the Oracle in hand and the mana for both. */
    @Test(timeOut = 120000)
    public void theAiCastsDoomsdayWithTheOracleInHand() {
        setUp();
        addCards("Swamp", 3, ai);
        addCards("Island", 2, ai);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertEquals("Doomsday should have been cast\n" + gameStateToString(game), 0,
                countCardsWithName(game, "Doomsday", ZoneType.Hand));
    }

    /** After Doomsday: two cards left and the Oracle in hand, the AI must cast it and win. */
    @Test(timeOut = 120000)
    public void theAiCastsTheOracleWithTheLibraryCut() {
        setUp();
        final ComboPlan combos = plan();
        while (ai.getCardsIn(ZoneType.Library).size() > 2) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        addCards("Island", 2, ai);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertTrue("the Oracle should have been cast and won (plan: " + combos.summary()
                + "; if it was cast but did not win look at DoomsdayPile.term, if not cast at ComboGuard)\n"
                + gameStateToString(game), game.isGameOver() || countCardsWithName(game, "Thassa's Oracle", ZoneType.Battlefield) > 0);
    }

    /**
     * The whole spell resolved by the AI: the five cards it keeps must come from the pile hook (the draw cards and the
     * mana, not lands and not the filler), and the rest of the library is exiled.
     */
    @Test(timeOut = 120000)
    public void theAiKeepsAUsefulPileWhenDoomsdayResolves() {
        setUp();
        addCards("Swamp", 3, ai);
        addCards("Island", 2, ai);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 600 && !game.isGameOver() && countCardsWithName(game, "Doomsday", ZoneType.Graveyard) == 0
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertEquals("Doomsday should have resolved\n" + gameStateToString(game), 1,
                countCardsWithName(game, "Doomsday", ZoneType.Graveyard));
        final CardCollection pile = new CardCollection(ai.getCardsIn(ZoneType.Library));
        int useful = 0;
        for (final Card c : pile) {
            if (forge.ai.simulation.AiCardLists.has(forge.ai.simulation.AiCardLists.Role.DOOMSDAY_DRAW, c.getName())
                    || forge.ai.simulation.AiCardLists.has(forge.ai.simulation.AiCardLists.Role.DOOMSDAY_MANA, c.getName())) {
                useful++;
            }
        }
        AssertJUnit.assertEquals("the pile is five cards\n" + gameStateToString(game), 5, pile.size());
        AssertJUnit.assertTrue("at least three of the five should be draw or mana cards, the pile was " + pile
                + " (the choice goes through ComboPlan.choose: look at the DOOMSDAY hook and at AiController"
                + ".chooseCardToHiddenOriginChangeZone)", useful >= 3);
    }

    // ------------------------------------------------------------------------------------ the pile is planned

    /** The hand of the report: two Underground Sea, Island, Lotus Petal; Doomsday, Dark Ritual, Street Wraith, a counter. */
    private void reportedHand(final boolean withDrawAndRitual) {
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        addCardToZone("Lotus Petal", ai, ZoneType.Battlefield);
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        addCardToZone("Misdirection", ai, ZoneType.Hand);
        if (withDrawAndRitual) {
            addCardToZone("Dark Ritual", ai, ZoneType.Hand);
            addCardToZone("Street Wraith", ai, ZoneType.Hand);
        }
    }

    private String doomsdayProblem() {
        Card doomsday = null;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals("Doomsday")) {
                doomsday = c;
            }
        }
        final SpellAbility sa = doomsday.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return ComboGuard.problem(ai, sa, null, plan());
    }

    /** The Oracle is still in the library: a pile of Ancestral, Black Lotus and the Oracle wins, so Doomsday is cast. */
    @Test
    public void doomsdayIsCastWithTheOracleInTheLibraryWhenAPileDrawsThroughToIt() {
        setUp();
        reportedHand(true);
        AssertJUnit.assertTrue("a winning pile exists (Wraith, Ancestral, Black Lotus, Oracle)", DoomsdayPile.ready(ai));
        AssertJUnit.assertNull("nothing is against casting it, got " + doomsdayProblem(), doomsdayProblem());
    }

    /** Without a draw spell or the ritual in hand no pile reaches the Oracle this turn, and the Oracle is not in hand. */
    @Test
    public void doomsdayIsNotCastWhenNoPileReachesTheOracle() {
        setUp();
        reportedHand(false);
        AssertJUnit.assertFalse("no draw in hand: no pile wins", DoomsdayPile.ready(ai));
        AssertJUnit.assertEquals("doomsday-not-ready", doomsdayProblem());
    }

    /** One land short for the Oracle's blue: no pile can win even with all the draw. */
    @Test
    public void doomsdayIsNotCastWithoutTheBlueForTheOracle() {
        setUp();
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Lotus Petal", ai, ZoneType.Battlefield);
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("one blue source at most: the Oracle cannot be cast", DoomsdayPile.ready(ai));
    }

    /** Too little life for the Wraith and the cut: the cost of the draw is part of the plan. */
    @Test
    public void doomsdayIsNotCastWithTheLifeOnlyForTheCut() {
        setUp();
        reportedHand(true);
        ai.setLife(4, null);
        AssertJUnit.assertFalse("2 life are left after Doomsday and the Wraith costs 2", DoomsdayPile.ready(ai));
    }

    /** With one Street Wraith the first card of the pile has to be the draw spell, and the Oracle is never at the bottom. */
    @Test
    public void thePileIsOrderedSoThatItDrawsThroughToTheOracle() {
        setUp();
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        final CardCollection five = options("Thassa's Oracle", "Black Lotus", "Dark Ritual", "Ancestral Recall", "Flusterstorm");
        final forge.game.card.CardCollectionView order = DoomsdayPile.order(ai, five);
        AssertJUnit.assertNotNull(order);
        final List<Card> bottomFirst = new ArrayList<>();
        for (final Card c : order) {
            bottomFirst.add(c);
        }
        AssertJUnit.assertEquals(5, bottomFirst.size());
        final Card top = bottomFirst.get(4);
        final Card bottom = bottomFirst.get(0);
        AssertJUnit.assertEquals("the Wraith draws the top card: it has to be Ancestral Recall, the order was " + bottomFirst,
                "Ancestral Recall", top.getName());
        AssertJUnit.assertFalse("Ancestral draws the next three: the Oracle is not at the bottom, " + bottomFirst,
                bottom.getName().equals("Thassa's Oracle"));
        AssertJUnit.assertFalse("and neither is the Lotus that pays for it, " + bottomFirst,
                bottom.getName().equals("Black Lotus"));
    }

    /** The pick of the five goes through the plan: Ancestral, the Lotus and the Oracle are all kept. */
    @Test
    public void theFivePicksIncludeTheCardsOfTheWinningPile() {
        setUp();
        reportedHand(true);
        final CardCollection options = new CardCollection(ai.getCardsIn(ZoneType.Library));
        final List<String> kept = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final Card pick = DoomsdayPile.pick(ai, options);
            AssertJUnit.assertNotNull("pick " + i + " exists", pick);
            kept.add(pick.getName());
            options.remove(pick);
        }
        AssertJUnit.assertTrue("Ancestral Recall is kept, the picks were " + kept, kept.contains("Ancestral Recall"));
        AssertJUnit.assertTrue("Black Lotus is kept, the picks were " + kept, kept.contains("Black Lotus"));
        AssertJUnit.assertTrue("a Thassa's Oracle is kept, the picks were " + kept, kept.contains("Thassa's Oracle"));
    }

    /** A pile that is already drawn through to the Oracle counts for the evaluator: the Doomsday cast is a progress. */
    @Test
    public void aPileWithTheOracleInItIsProgressForTheEvaluator() {
        setUp();
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Library))) {
            ai.getZone(ZoneType.Library).remove(c);
        }
        for (final String name : new String[] { "Flusterstorm", "Thassa's Oracle", "Black Lotus", "Ancestral Recall", }) {
            addCardToZone(name, ai, ZoneType.Library);
        }
        addCardToZone("Dark Ritual", ai, ZoneType.Library);
        AssertJUnit.assertTrue("a five card library with the Oracle in it is not 'nothing to win with', term was "
                + DoomsdayPile.term(ai), DoomsdayPile.term(ai) > 0);
    }

    /** The whole turn of the report: the Oracle is in the library, the AI casts Doomsday and wins in the same turn. */
    @Test(timeOut = 240000)
    public void theAiCastsDoomsdayAndWinsTheSameTurnWithTheOracleInTheLibrary() {
        setUp();
        reportedHand(true);
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 1500 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertEquals("Doomsday should have been cast\n" + gameStateToString(game), 0,
                countCardsWithName(game, "Doomsday", ZoneType.Hand));
        AssertJUnit.assertTrue("the Oracle should be on the battlefield or the game won\n" + gameStateToString(game),
                game.isGameOver() || countCardsWithName(game, "Thassa's Oracle", ZoneType.Battlefield) > 0);
    }

    /** Street Wraith's cycling pays 2 life: with the library cut to the pile it is the draw-through, not decking. */
    @Test
    public void theWraithCyclesWhenTheLibraryIsTheCutPile() {
        setUp();
        final ComboPlan combos = plan();
        while (ai.getCardsIn(ZoneType.Library).size() > 5) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        ai.setLife(10, null);
        final Card wraith = addCardToZone("Street Wraith", ai, ZoneType.Hand);
        SpellAbility cycling = null;
        for (final SpellAbility sa : wraith.getAllSpellAbilities()) {
            if (sa.isCycling()) {
                cycling = sa;
            }
        }
        AssertJUnit.assertNotNull("Street Wraith has a cycling ability", cycling);
        cycling.setActivatingPlayer(ai);
        AssertJUnit.assertNull("cycling is the draw-through of the pile, got " + ComboGuard.problem(ai, cycling, null, combos),
                ComboGuard.problem(ai, cycling, null, combos));
    }

    /** The draw heuristics refuse to "deck yourself": with the pile as the library and the Oracle in hand they must not. */
    @Test
    public void theDrawHeuristicsAllowTheDrawThroughOfTheCutPile() {
        setUp();
        while (ai.getCardsIn(ZoneType.Library).size() > 5) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        ai.setLife(10, null);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final Card wraith = addCardToZone("Street Wraith", ai, ZoneType.Hand);
        SpellAbility cycling = null;
        for (final SpellAbility sa : wraith.getAllSpellAbilities()) {
            if (sa.isCycling()) {
                cycling = sa;
            }
        }
        AssertJUnit.assertNotNull(cycling);
        cycling.setActivatingPlayer(ai);
        final forge.ai.AiPlayDecision decision = forge.ai.SpellApiToAi.Converter.get(cycling).canPlayWithSubs(ai, cycling).decision();
        AssertJUnit.assertTrue("drawing through the pile is no decking, the draw heuristics said " + decision
                + " (DrawAi: the deck-yourself rule, the phase restrictions, willPayCosts)", decision.willingToPlay());
    }

    @Test
    public void aLibraryOfFiveWithoutAnyWinnerStillMustNotBeDrawnDown() {
        setUp();
        while (ai.getCardsIn(ZoneType.Library).size() > 5) {
            ai.getZone(ZoneType.Library).remove(ai.getCardsIn(ZoneType.Library).get(0));
        }
        AssertJUnit.assertFalse("no winner anywhere: drawing is decking", DoomsdayPile.drawsThroughPile(ai, 1));
    }

    /** Each card drawn out of the pile is progress: otherwise the rollouts see no reason to cycle the Wraith. */
    @Test
    public void drawingThroughThePileRaisesTheEvaluatorTerm() {
        setUp();
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Library))) {
            ai.getZone(ZoneType.Library).remove(c);
        }
        for (final String name : new String[] { "Dark Ritual", "Black Lotus", "Thassa's Oracle", "Mental Misstep", "Ancestral Recall" }) {
            addCardToZone(name, ai, ZoneType.Library);
        }
        final int before = DoomsdayPile.term(ai);
        final Card drawn = ai.getCardsIn(ZoneType.Library).get(0);
        game.getAction().moveToHand(drawn, null);
        AssertJUnit.assertTrue("a card drawn out of the pile is progress: " + before + " then " + DoomsdayPile.term(ai),
                DoomsdayPile.term(ai) > before);
    }

    /** The library is the pile (top first), the lands are in play and the Wraith is in hand. */
    private List<SpellAbility> pileWithWraith(final String... topFirst) {
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Library))) {
            ai.getZone(ZoneType.Library).remove(c);
        }
        for (int i = topFirst.length - 1; i >= 0; i--) {
            final Card c = addCardToZone(topFirst[i], ai, ZoneType.Exile);
            game.getAction().moveToLibrary(c, 0, null); // the last one moved is the top
        }
        final Card wraith = addCardToZone("Street Wraith", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final List<SpellAbility> sas = new ArrayList<>();
        for (final SpellAbility sa : wraith.getAllSpellAbilities()) {
            sa.setActivatingPlayer(ai);
            sas.add(sa);
        }
        return sas;
    }

    @Test
    public void theNextPlayOfAWinningPileIsTheWraithCycling() {
        setUp();
        final List<SpellAbility> sas = pileWithWraith("Ancestral Recall", "Mental Misstep", "Thassa's Oracle",
                "Black Lotus", "Dark Ritual");
        ai.setLife(10, null);
        final SpellAbility next = DoomsdayPile.sureDraw(ai, sas);
        AssertJUnit.assertNotNull("the pile wins through the Wraith and Ancestral, so the Wraith is cycled; library "
                + ai.getCardsIn(ZoneType.Library), next);
        AssertJUnit.assertTrue("it is the cycling, not the five mana creature spell: " + next, next.isCycling());
    }

    @Test
    public void noPlayIsMadeWhenThePileHasNoWinner() {
        setUp();
        final List<SpellAbility> sas = pileWithWraith("Ancestral Recall", "Mental Misstep", "Dark Ritual",
                "Black Lotus", "Flusterstorm");
        ai.setLife(10, null);
        AssertJUnit.assertNull("no Oracle anywhere: nothing to draw to", DoomsdayPile.sureDraw(ai, sas));
    }
}

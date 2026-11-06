package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.DigUntilAi;
import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Oops! All Spells: Balustrade Spy and Undercity Informer empty the AI's own library only while the win can follow:
 * Thassa's Oracle in hand, or in the library with Dread Return and three creatures to pay for its flashback. Without
 * it the activated outlets aim at the opponent, as their scripts have them, and the creatures are not even cast: the
 * cast of Balustrade Spy has no target (its trigger picks one), so the guard has to refuse the cast itself. An Oracle
 * in hand only counts when the mana to cast it after the Spy is there too: the library is empty for good by then,
 * and the next draw would lose the game.
 */
public class FlatMonteCarloOopsSpyTest extends AITest {
    private static final Object[] OOPS_DECK = { "Balustrade Spy", 4, "Undercity Informer", 1, "Thassa's Oracle", 1,
        "Narcomoeba", 4, "Dread Return", 4, "Bridge from Below", 4, "Cabal Therapy", 4, "Lotus Petal", 4,
        "Dark Ritual", 4, "Elvish Spirit Guide", 4, "Simian Spirit Guide", 4, "Poxwalkers", 4, "Tireless Tracker", 1,
        "Street Wraith", 3, "Lion's Eye Diamond", 4 };

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < OOPS_DECK.length; i += 2) {
            for (int n = 0; n < (Integer) OOPS_DECK[i + 1]; n++) {
                addCardToZone((String) OOPS_DECK[i], ai, ZoneType.Library);
            }
        }
    }

    private ComboPlan plan() {
        return DeckPlan.forSetting("auto", ai).combos;
    }

    /** Takes every copy of the card out of the library into exile: gone for good. */
    private void exileFromLibrary(final String name) {
        for (final Card c : new CardCollection(ai.getCardsIn(ZoneType.Library))) {
            if (name.equals(c.getName())) {
                ai.getZone(ZoneType.Library).remove(c);
                ai.getZone(ZoneType.Exile).add(c);
            }
        }
    }

    private SpellAbility informerMill() {
        final Card informer = addCardToZone("Undercity Informer", ai, ZoneType.Battlefield);
        for (final SpellAbility sa : informer.getSpellAbilities()) {
            if (sa.getApi() == ApiType.DigUntil) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("no DigUntil ability on " + informer);
    }

    private String aimOf(final SpellAbility mill) {
        new DigUntilAi().canPlayWithSubs(ai, mill);
        return mill.getTargets().contains(ai) ? "itself" : mill.getTargets().contains(opp) ? "the opponent" : "nobody";
    }

    @Test
    public void withTheOracleInHandTheMillAimsAtItself() {
        setUp();
        plan();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("the Oracle is in hand: the mill is the win", "itself", aimOf(informerMill()));
    }

    @Test
    public void withTheOracleInTheLibraryAndTheFlashbackLineItAimsAtItself() {
        setUp();
        plan();
        AssertJUnit.assertEquals("Oracle, Dread Return and Narcomoebas are all in the library", "itself", aimOf(informerMill()));
    }

    @Test
    public void withoutTheOracleTheMillAimsAtTheOpponent() {
        setUp();
        plan();
        exileFromLibrary("Thassa's Oracle");
        AssertJUnit.assertEquals("no Oracle anywhere: milling itself only loses", "the opponent", aimOf(informerMill()));
    }

    @Test
    public void withoutDreadReturnTheLibraryOracleIsNotAWin() {
        setUp();
        plan();
        exileFromLibrary("Dread Return");
        AssertJUnit.assertEquals("the Oracle would be milled and nothing brings it back", "the opponent", aimOf(informerMill()));
    }

    @Test
    public void withoutCreaturesToPayTheFlashbackTheMillAimsAtTheOpponent() {
        setUp();
        plan();
        exileFromLibrary("Narcomoeba");
        AssertJUnit.assertEquals("Informer alone is one creature of the three the flashback needs", "the opponent",
                aimOf(informerMill()));
    }

    @Test
    public void theGuardRefusesASelfMillWithoutAWinnerAndAllowsOneWithIt() {
        setUp();
        final ComboPlan combos = plan();
        exileFromLibrary("Thassa's Oracle");
        final Card spy = addCardToZone("Balustrade Spy", ai, ZoneType.Hand);
        final SpellAbility cast = spy.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("self-mill-no-winner", ComboGuard.problem(ai, cast, ai, combos));
        AssertJUnit.assertEquals("the cast is refused whatever it is aimed at: its trigger picks the target on entering",
                "self-mill-no-winner", ComboGuard.problem(ai, cast, opp, combos));
        giveMana();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        AssertJUnit.assertNull("with the Oracle in hand the self-mill is allowed", ComboGuard.problem(ai, cast, ai, combos));
    }

    /** What the AI really asks the guard: the cast of the Spy, with no target and nothing aimed (aim is null). */
    @Test
    public void theCastOfTheSpyIsRefusedWithoutAWinner() {
        setUp();
        final ComboPlan combos = plan();
        exileFromLibrary("Thassa's Oracle");
        final Card spy = addCardToZone("Balustrade Spy", ai, ZoneType.Hand);
        final SpellAbility cast = spy.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("no Oracle anywhere: casting the Spy only wastes it", "self-mill-no-winner",
                ComboGuard.problem(ai, cast, null, combos));
        giveMana();
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        AssertJUnit.assertNull("with the Oracle in hand and the mana for both the Spy is cast", ComboGuard.problem(ai, cast, null, combos));
    }

    /** The Spy that is cast is the third creature the flashback of Dread Return needs: that cast must not be refused. */
    @Test
    public void theSpyBeingCastCountsAmongTheCreaturesTheFlashbackSacrifices() {
        setUp();
        final ComboPlan combos = plan();
        exileFromLibrary("Narcomoeba");
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        final Card spy = addCardToZone("Balustrade Spy", ai, ZoneType.Hand);
        final SpellAbility cast = spy.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("two creatures are not three", combos.libraryOutSelfMillSafe(ai));
        AssertJUnit.assertTrue("with the Spy that is cast there are three", combos.libraryOutSelfMillSafe(ai, 1));
        AssertJUnit.assertNull("the cast completes the line", ComboGuard.problem(ai, cast, null, combos));
    }

    /**
     * The Oracle in hand is no winner when it cannot be cast after the Spy: the library is empty for good, and the next
     * draw loses. Six Swamps pay for the Spy but make no {U}{U}; two Islands complete it.
     */
    @Test
    public void theSpyIsRefusedWhenTheOracleCannotBeCastAfterIt() {
        setUp();
        final ComboPlan combos = plan();
        exileFromLibrary("Thassa's Oracle");
        final Card spy = addCardToZone("Balustrade Spy", ai, ZoneType.Hand);
        final SpellAbility cast = spy.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        for (int i = 0; i < 6; i++) {
            addCard("Swamp", ai);
        }
        AssertJUnit.assertEquals("no blue: the Oracle cannot follow the Spy", "self-mill-no-winner",
                ComboGuard.problem(ai, cast, null, combos));
        addCard("Island", ai);
        addCard("Island", ai);
        AssertJUnit.assertNull("with {U}{U} left after the Spy the win is certain", ComboGuard.problem(ai, cast, null, combos));
    }

    /** Enough untapped lands for the Spy ({3}{B}) and the Oracle ({U}{U}) in the same turn. */
    private void giveMana() {
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        for (int i = 0; i < 2; i++) {
            addCard("Island", ai);
        }
    }

    @Test
    public void aDeckThatIsNotLibraryOutHasNothingToSayAboutIt() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int n = 0; n < 24; n++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        AssertJUnit.assertTrue(plan().libraryOutSelfMillSafe(ai));
    }
}

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
 * it they aim at the opponent, as their scripts have them.
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
        AssertJUnit.assertNull("aimed at the opponent nothing is against it", ComboGuard.problem(ai, cast, opp, combos));
        addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        AssertJUnit.assertNull("with the Oracle in hand the self-mill is allowed", ComboGuard.problem(ai, cast, ai, combos));
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

package forge.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboAssembly;
import forge.ai.simulation.ComboCatalog;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Every combo of ai-combos.txt that has no play of its own (ComboAssembly.COVERED lists the ones that do, with their
 * own tests): the AI holds all the cards and the mana, and has to put the combo together in one turn. One scenario per
 * line of the list, so a new line is tested by being written. A failure names the combo and the cards that were left
 * in hand; the combo is then either not castable by the AI (look at the card's AI logic) or held by a guard (look at
 * ComboGuard and ComboAssembly).
 */
public class FlatMonteCarloComboAssemblyTest extends AITest {
    /** Cards that cannot be cast from the hand without help: where the scenario starts them instead. */
    private static final Map<String, ZoneType> START = new HashMap<>();
    static {
        START.put("Worldgorger Dragon", ZoneType.Graveyard); // Animate Dead needs a creature card in a graveyard
    }

    /**
     * The payoffs that are held until they win: with the rest of the combo in play and nobody to win against they are
     * worth a card, so they stay in hand and that is the right play (the finishing tests below give them a winning
     * moment).
     */
    private static final Set<String> FINISHERS = new HashSet<>(Arrays.asList(
            "Stroke of Genius", "Brain Freeze", "Retraction Helix", "Chain of Smog", "Peer into the Abyss", "Unburial Rites"));

    /** Combos that are put together only with a mana outlet: alone the loop exiles the AI's own board. */
    private static final Set<String> NEEDS_AN_OUTLET = new HashSet<>(Arrays.asList("Worldgorger Dragon + Animate Dead"));

    private Game game;
    private Player ai;

    @DataProvider(name = "combos")
    public Object[][] combos() {
        final List<Object[]> rows = new ArrayList<>();
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            if (!ComboAssembly.isCovered(e)) {
                rows.add(new Object[] { e.name });
            }
        }
        return rows.toArray(new Object[0][]);
    }

    private ComboCatalog.Entry entry(final String name) {
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        throw new IllegalStateException("no combo named " + name);
    }

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
        }
    }

    private void mana(final int each) {
        for (final String basic : new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" }) {
            addCards(basic, each, ai);
        }
    }

    private void runTurn() {
        runTurn(PhaseType.MAIN1);
    }

    private void runTurn(final PhaseType from) {
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(from, ai);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 800 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
    }

    /** The first alternative of each slot, in hand (or where the scenario starts it); a second land starts in play. */
    private List<String> deal(final ComboCatalog.Entry e) {
        final List<String> pieces = new ArrayList<>();
        boolean landInHand = false;
        for (final String spec : e.slotSpecs()) {
            final String name = spec.split(" / ")[0];
            pieces.add(name);
            ZoneType zone = START.getOrDefault(name, ZoneType.Hand);
            final Card c = addCardToZone(name, ai, zone);
            if (c.isLand() && zone == ZoneType.Hand) {
                if (landInHand) {
                    game.getAction().moveToPlay(c, null, null); // only one land is played a turn
                }
                landInHand = true;
            }
        }
        return pieces;
    }

    /** Where the pieces are at the end of the turn: not in the hand or the library means it was cast or played. */
    private List<String> stuck(final List<String> pieces) {
        final List<String> left = new ArrayList<>();
        for (final String name : pieces) {
            boolean gone = false;
            for (final ZoneType z : new ZoneType[] { ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Stack }) {
                for (final Card c : ai.getCardsIn(z)) {
                    gone |= c.getName().equals(name);
                }
            }
            if (!gone) {
                left.add(name);
            }
        }
        return left;
    }

    @Test(dataProvider = "combos", timeOut = 180000)
    public void theComboIsPutTogetherWhenAllItsCardsAreInHand(final String name) {
        setUp();
        mana(5);
        final ComboCatalog.Entry e = entry(name);
        final List<String> pieces = deal(e);
        runTurn();
        final List<String> left = stuck(pieces);
        if (NEEDS_AN_OUTLET.contains(name)) {
            AssertJUnit.assertEquals(e + ": without a mana outlet the loop is not started (it would exile the AI's own board)\n"
                    + gameStateToString(game), 0, countCardsWithName(game, "Animate Dead", ZoneType.Battlefield));
            return;
        }
        left.removeAll(FINISHERS);
        AssertJUnit.assertTrue(e + ": " + left + " are still in hand or library at the end of the turn, with all the mana"
                + " (the first one not cast is the one to look at: its AI logic, or ComboGuard)\n" + gameStateToString(game),
                left.isEmpty());
    }

    @Test(dataProvider = "combos", timeOut = 180000)
    public void aPieceIsHeldWhenTheOthersAreNotThere(final String name) {
        final ComboCatalog.Entry e = entry(name);
        if (e.slotCount() < 2) {
            return;
        }
        setUp();
        mana(5);
        // only the first piece, and when it is a land a spell-less hand: the combo cannot be put together
        final String first = e.slotSpecs().get(0).split(" / ")[0];
        final Card c = addCardToZone(first, ai, ZoneType.Hand);
        final SpellAbility sa = c.isLand() ? null : c.getFirstSpellAbility();
        if (sa == null) {
            return;
        }
        final List<SpellAbility> sas = new ArrayList<>();
        sa.setActivatingPlayer(ai);
        sas.add(sa);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNull(e + ": the assembly plays a single piece " + first + " with the rest of the combo missing",
                ComboAssembly.next(ai, sas, s -> true));
    }

    @Test
    public void theCheapestPieceIsCastFirst() {
        setUp();
        mana(5);
        final List<SpellAbility> sas = new ArrayList<>();
        for (final String name : new String[] { "Rings of Brighthearth", "Basalt Monolith" }) {
            final Card c = addCardToZone(name, ai, ZoneType.Hand);
            c.getFirstSpellAbility().setActivatingPlayer(ai);
            sas.add(c.getFirstSpellAbility());
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility next = ComboAssembly.next(ai, sas, s -> true);
        AssertJUnit.assertNotNull("Basalt Monolith + Rings of Brighthearth in hand with the mana: cast one", next);
        AssertJUnit.assertEquals("the cheaper piece first", "Basalt Monolith", next.getHostCard().getName());
    }

    @Test
    public void thePiecesAreHeldWithoutTheManaForAll() {
        setUp();
        addCards("Plains", 3, ai); // 3 + 4 mana are needed, only 3 are there
        final List<SpellAbility> sas = new ArrayList<>();
        for (final String name : new String[] { "Rings of Brighthearth", "Basalt Monolith" }) {
            final Card c = addCardToZone(name, ai, ZoneType.Hand);
            c.getFirstSpellAbility().setActivatingPlayer(ai);
            sas.add(c.getFirstSpellAbility());
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNull("three mana for a three and a four: the combo is not put together this turn",
                ComboAssembly.next(ai, sas, s -> true));
    }

    @Test
    public void thePieceInPlayCountsForTheSlot() {
        setUp();
        mana(2);
        addCardToZone("Basalt Monolith", ai, ZoneType.Battlefield);
        final Card rings = addCardToZone("Rings of Brighthearth", ai, ZoneType.Hand);
        rings.getFirstSpellAbility().setActivatingPlayer(ai);
        final List<SpellAbility> sas = new ArrayList<>();
        sas.add(rings.getFirstSpellAbility());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility next = ComboAssembly.next(ai, sas, s -> true);
        AssertJUnit.assertNotNull("Monolith in play, the Rings in hand with the mana: cast the Rings", next);
        AssertJUnit.assertEquals("Rings of Brighthearth", next.getHostCard().getName());
    }

    @Test
    public void aComboWithItsOwnPlayIsLeftToIt() {
        setUp();
        mana(5);
        final List<SpellAbility> sas = new ArrayList<>();
        for (final String name : new String[] { "Channel", "Fireball" }) {
            final Card c = addCardToZone(name, ai, ZoneType.Hand);
            c.getFirstSpellAbility().setActivatingPlayer(ai);
            sas.add(c.getFirstSpellAbility());
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNull("Channel + Fireball has its own play", ComboAssembly.next(ai, sas, s -> true));
    }

    /** The payoff that draws is cast when it wins: the opponent has three cards left and the mana is there. */
    @Test(timeOut = 180000)
    public void strokeOfGeniusIsCastAtAnOpponentWhoCanBeDrawnOut() {
        setUp();
        mana(5);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        final Card stroke = addCardToZone("Stroke of Genius", ai, ZoneType.Hand);
        // what the AI makes of it in the second main phase (the draw heuristics keep draws out of the first)
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        final SpellAbility sa = stroke.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final String verdict = "heuristics: " + ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa)
                + ", guard: " + forge.ai.simulation.ComboGuard.problem(ai, sa, null,
                        forge.ai.simulation.DeckPlan.forSetting("auto", ai).combos);
        runTurn(PhaseType.MAIN2);
        AssertJUnit.assertTrue("Stroke of Genius should have been cast for X of at least 3 at the opponent (library "
                + opp.getCardsIn(ZoneType.Library).size() + "; " + verdict + ")\n" + gameStateToString(game),
                game.isGameOver() || countCardsWithName(game, "Stroke of Genius", ZoneType.Hand) == 0);
    }

    private List<SpellAbility> strokeInHand(final int opponentLibrary) {
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < opponentLibrary; i++) {
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        final Card stroke = addCardToZone("Stroke of Genius", ai, ZoneType.Hand);
        stroke.getFirstSpellAbility().setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final List<SpellAbility> sas = new ArrayList<>();
        sas.add(stroke.getFirstSpellAbility());
        return sas;
    }

    @Test
    public void theFinishingDrawIsMadeAtAnOpponentItEmpties() {
        setUp();
        mana(5);
        AssertJUnit.assertNotNull("25 lands: X is 22 at least, the opponent has 3 cards",
                ComboAssembly.finish(ai, strokeInHand(3), s -> true));
    }

    @Test
    public void theFinishingDrawIsNotMadeWhenTheLibraryIsTooBig() {
        setUp();
        mana(2); // X of 7 at most
        AssertJUnit.assertNull("the opponent has 30 cards, 7 are not enough", ComboAssembly.finish(ai, strokeInHand(30), s -> true));
    }
}

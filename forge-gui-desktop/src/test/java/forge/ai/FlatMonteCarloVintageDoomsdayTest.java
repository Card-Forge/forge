package forge.ai;

import java.util.ArrayList;

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
}

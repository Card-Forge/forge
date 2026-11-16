package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * What a turn keeps that cards read: the copy of a game must keep it too, or a rollout from the second main phase is
 * played on a turn where nothing has happened (morbid, raid, "the first time you gain life each turn", ...).
 */
public class GameCopierTurnStateTest extends SimulationTest {

    private Game newGameInMain2(int turn) {
        final Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, game.getPlayers().get(1), true, turn);
        return game;
    }

    private static Card find(Game game, String name) {
        for (final Card c : game.getCardsInGame()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        AssertJUnit.fail("no " + name + " in the game");
        return null;
    }

    @Test
    public void aCreatureThatDiedThisTurnIsStillMorbidInTheCopy() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        game.getAction().moveToGraveyard(bears, null);
        AssertJUnit.assertEquals(1, p.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield).size());

        final Game copy = new GameCopier(game).makeCopy();

        final Player q = copy.getPlayers().get(1);
        AssertJUnit.assertEquals(1, q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield).size());
    }

    @Test
    public void aCreatureThatAttackedThisTurnIsKeptForRaid() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        p.addCreaturesAttackedThisTurn(forge.game.card.CardCopyService.getLKICopy(bears), game.getPlayers().get(0));

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertEquals(1, copy.getPlayers().get(1).getCreaturesAttackedThisTurn().size());
    }

    /** The copy sets the life from the starting total: a loss there must not undo a gain this turn, nor a gain add one. */
    @Test
    public void theLifeGainedThisTurnIsCountedAsInTheGame() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        p.loseLife(10, false, false, null);
        p.gainLife(5, null, null); // life 15, one gain this turn
        final Player o = game.getPlayers().get(0);
        o.setLife(25, null);
        o.setLifeGainedTimesThisTurn(0); // life 25 from an earlier turn, no gain this turn

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertEquals(1, copy.getPlayers().get(1).getLifeGainedTimesThisTurn());
        AssertJUnit.assertEquals(0, copy.getPlayers().get(0).getLifeGainedTimesThisTurn());
    }

    @Test
    public void theCardsDrawnThisTurnAreCounted() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        p.drawCards(2);

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertEquals(2, copy.getPlayers().get(1).getNumDrawnThisTurn());
    }

    @Test
    public void aCardKeepsTheTurnItEnteredItsZone() {
        final Game game = newGameInMain2(5);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        bears.setTurnInZone(2);

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertEquals(2, find(copy, "Grizzly Bears").getTurnInZone());
    }

    /** "Activate only once": activated in an earlier turn, it must not be activatable again in the copy. */
    @Test
    public void anAbilityActivatedInAnEarlierTurnKeepsItsActivationsThisGame() {
        final Game game = newGameInMain2(5);
        final Player p = game.getPlayers().get(1);
        final Card librarian = addCard("Mild-Mannered Librarian", p);
        final SpellAbility animate = librarian.getSpellAbilities().getFirst();
        animate.setActivatingPlayer(p);
        librarian.addAbilityActivated(animate);
        librarian.getAbilityActivatedThisTurn().clear(); // as at the end of that turn
        AssertJUnit.assertEquals(0, animate.getActivationsThisTurn());
        AssertJUnit.assertEquals(1, animate.getActivationsThisGame());

        final Game copy = new GameCopier(game).makeCopy();

        final SpellAbility copied = find(copy, "Mild-Mannered Librarian").getSpellAbilities().getFirst();
        AssertJUnit.assertEquals(0, copied.getActivationsThisTurn());
        AssertJUnit.assertEquals(1, copied.getActivationsThisGame());
    }
}

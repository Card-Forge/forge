package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
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

    /** Raid: the only attack this turn killed a planeswalker, which is no longer there to be a defender. */
    @Test
    public void anAttackOnAPlaneswalkerThatHasLeftIsKeptForRaid() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        final Card jace = addCard("Jace Beleren", game.getPlayers().get(0));
        p.addCreaturesAttackedThisTurn(forge.game.card.CardCopyService.getLKICopy(bears), jace);
        game.getAction().moveToGraveyard(jace, null);

        final Game copy = new GameCopier(game).makeCopy();

        AssertJUnit.assertEquals(1, copy.getPlayers().get(1).getCreaturesAttackedThisTurn().size());
    }

    /** Morbid with YouCtrl: a stolen creature that died is recorded under who controlled it, not its owner. */
    @Test
    public void aStolenCreatureThatDiedKeepsItsController() {
        final Game game = newGameInMain2(3);
        final Player owner = game.getPlayers().get(0);
        final Player thief = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", owner);
        bears.setController(thief, game.getNextTimestamp());
        game.getAction().moveToGraveyard(bears, null);
        final Card died = owner.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield).get(0);
        AssertJUnit.assertEquals(thief, died.getController());

        final Game copy = new GameCopier(game).makeCopy();

        final Card copied = copy.getPlayers().get(0).getZone(ZoneType.Graveyard)
                .getCardsAddedThisTurn(ZoneType.Battlefield).get(0);
        AssertJUnit.assertEquals(copy.getPlayers().get(1), copied.getController());
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

    /** The card's activated abilities (not its spell), in the order of its script. */
    private static List<SpellAbility> activated(Card c) {
        final List<SpellAbility> res = new ArrayList<>();
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                res.add(sa);
            }
        }
        return res;
    }

    /** "Activate only once each turn": activated twice this turn, it counts twice this turn and this game. */
    @Test
    public void anAbilityActivatedThisTurnKeepsEachActivation() {
        final Game game = newGameInMain2(5);
        final Player p = game.getPlayers().get(1);
        final Card librarian = addCard("Mild-Mannered Librarian", p);
        final SpellAbility animate = activated(librarian).get(0);
        animate.setActivatingPlayer(p);
        librarian.addAbilityActivated(animate);
        librarian.addAbilityActivated(animate);

        final Game copy = new GameCopier(game).makeCopy();

        final SpellAbility copied = activated(find(copy, "Mild-Mannered Librarian")).get(0);
        AssertJUnit.assertEquals(2, copied.getActivationsThisTurn());
        AssertJUnit.assertEquals(2, copied.getActivationsThisGame());
    }

    /**
     * Skyblade's Boon has two abilities with the same cost and text (one from the battlefield, one from the graveyard):
     * the second one's activations stay with the second one, not the first one with that description.
     */
    @Test
    public void twoAbilitiesWithTheSameDescriptionKeepTheirOwnActivations() {
        final Game game = newGameInMain2(5);
        final Player p = game.getPlayers().get(1);
        final Card boon = addCard("Skyblade's Boon", p);
        boon.attachToEntity(addCard("Grizzly Bears", p), null); // an aura on nothing would go to the graveyard
        final List<SpellAbility> abilities = activated(boon);
        AssertJUnit.assertEquals(2, abilities.size());
        AssertJUnit.assertEquals(abilities.get(0).getDescription(), abilities.get(1).getDescription());
        final SpellAbility second = abilities.get(1);
        second.setActivatingPlayer(p);
        boon.addAbilityActivated(second);

        final Game copy = new GameCopier(game).makeCopy();

        final List<SpellAbility> copied = activated(find(copy, "Skyblade's Boon"));
        AssertJUnit.assertEquals(0, copied.get(0).getActivationsThisTurn());
        AssertJUnit.assertEquals(1, copied.get(1).getActivationsThisTurn());
    }

    /** A card moved twice this turn (battlefield, exile, graveyard): each zone keeps where its card came from. */
    @Test
    public void aCardMovedTwiceThisTurnKeepsEachMove() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        final Card exiled = game.getAction().exile(bears, null, null);
        game.getAction().moveToGraveyard(exiled, null);

        final Game copy = new GameCopier(game).makeCopy();

        final Player q = copy.getPlayers().get(1);
        AssertJUnit.assertEquals(1, q.getZone(ZoneType.Exile).getCardsAddedThisTurn(ZoneType.Battlefield).size());
        AssertJUnit.assertEquals(1, q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Exile).size());
        AssertJUnit.assertEquals(0, q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield).size());
    }

    @Test
    public void everyCreatureThatDiedThisTurnIsKept() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final List<String> names = List.of("Grizzly Bears", "Hill Giant", "Llanowar Elves");
        for (final String name : names) {
            game.getAction().moveToGraveyard(addCard(name, p), null);
        }

        final Game copy = new GameCopier(game).makeCopy();

        final List<String> died = new ArrayList<>();
        final Player q = copy.getPlayers().get(1);
        for (final Card c : q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield)) {
            died.add(c.getName());
        }
        Collections.sort(died);
        AssertJUnit.assertEquals(names, died);
    }

    /** Raid and morbid: a token that attacked, died and ceased to exist is rebuilt from what was last known of it. */
    @Test
    public void aTokenThatAttackedAndCeasedToExistIsKept() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card token = addToken("g_2_2_bear", p);
        p.addCreaturesAttackedThisTurn(CardCopyService.getLKICopy(token), game.getPlayers().get(0));
        final Card dead = game.getAction().moveToGraveyard(token, null);
        game.getAction().ceaseToExist(dead, true);
        AssertJUnit.assertNull(game.findById(token.getId()));

        final Game copy = new GameCopier(game).makeCopy();

        final Player q = copy.getPlayers().get(1);
        final List<Card> attacked = q.getCreaturesAttackedThisTurn();
        AssertJUnit.assertEquals(1, attacked.size());
        AssertJUnit.assertTrue(attacked.get(0).isToken());
        final List<Card> died = q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield);
        AssertJUnit.assertEquals(1, died.size());
        AssertJUnit.assertTrue(died.get(0).isToken());
    }

    /** Making a copy reads the original's history and leaves it as it was: the same cards, of the original game. */
    @Test
    public void makingACopyLeavesTheOriginalHistoryAlone() {
        final Game game = newGameInMain2(3);
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        p.addCreaturesAttackedThisTurn(CardCopyService.getLKICopy(bears), game.getPlayers().get(0));
        game.getAction().moveToGraveyard(bears, null);
        final List<Card> attacked = p.getCreaturesAttackedThisTurn();
        final List<Card> died = p.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield);
        final List<Card> left = new ArrayList<>(game.getLeftBattlefieldThisTurn());
        AssertJUnit.assertEquals(1, left.size());

        final Game copy = new GameCopier(game).makeCopy();

        assertSameCards(game, attacked, p.getCreaturesAttackedThisTurn());
        assertSameCards(game, died, p.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield));
        assertSameCards(game, left, game.getLeftBattlefieldThisTurn());
        final Player q = copy.getPlayers().get(1);
        AssertJUnit.assertSame(copy, q.getCreaturesAttackedThisTurn().get(0).getGame());
        final Card diedInCopy = q.getZone(ZoneType.Graveyard).getCardsAddedThisTurn(ZoneType.Battlefield).get(0);
        AssertJUnit.assertSame(copy, diedInCopy.getGame());
    }

    /** The same card objects (cards are equal by id, so a copy's card would pass equals), still of {@code game}. */
    private static void assertSameCards(Game game, List<Card> before, List<Card> after) {
        AssertJUnit.assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) {
            AssertJUnit.assertSame(before.get(i), after.get(i));
            AssertJUnit.assertSame(game, after.get(i).getGame());
        }
    }
}

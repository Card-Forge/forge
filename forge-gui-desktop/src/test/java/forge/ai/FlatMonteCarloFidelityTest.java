package forge.ai;

import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.GameSimulator;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * The copied game a rollout plays in must remember what happened this turn and must survive being made while a combat
 * is going on, otherwise plays that depend on it are judged wrongly (or not at all).
 */
public class FlatMonteCarloFidelityTest extends AITest {

    @Test
    public void copyRemembersWhatLeftTheBattlefieldThisTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bear = addCard("Grizzly Bears", ai);
        game.getAction().destroy(bear, null, false, null);
        AssertJUnit.assertEquals("the real game saw it leave", 1, game.getLeftBattlefieldThisTurn().size());

        final GameSimulator sim = new GameSimulator(game, ai);
        final Game copy = sim.getSimulatedGameState();
        AssertJUnit.assertEquals("revolt needs this in the copy", 1, copy.getLeftBattlefieldThisTurn().size());
        AssertJUnit.assertEquals("Grizzly Bears", copy.getLeftBattlefieldThisTurn().get(0).getName());
    }

    @Test
    public void copyOfAnEmptyTurnHasNoHistory() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        AssertJUnit.assertTrue(copy.getLeftBattlefieldThisTurn().isEmpty());
    }

    @Test
    public void copyDuringCombatSurvivesAPlaneswalkerThatLeft() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card attacker = addCard("Grizzly Bears", opp);
        attacker.setSickness(false);
        final Card walker = addCard("Jace Beleren", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false);
        game.getPhaseHandler().setCombat(new forge.game.combat.Combat(opp));
        game.getCombat().addAttacker(attacker, walker);
        game.getAction().destroy(walker, null, false, null);
        // the defender is gone from the game, still named in the combat: copying must not throw
        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        AssertJUnit.assertNotNull(copy);
    }

    @Test
    public void copyKeepsATransformedCardAndItsOtherFace() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card fable = addCard("Fable of the Mirror-Breaker", ai);
        fable.setState(forge.card.CardStateName.Backside, true);
        AssertJUnit.assertEquals("Reflection of Kiki-Jiki", fable.getName());

        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        Card copied = null;
        for (final Card c : copy.getCardsIn(ZoneType.Battlefield)) {
            if (c.getId() == fable.getId()) {
                copied = c;
            }
        }
        AssertJUnit.assertNotNull(copied);
        AssertJUnit.assertEquals("Reflection of Kiki-Jiki", copied.getName());
        AssertJUnit.assertTrue("it can still be transformed back", copied.hasState(forge.card.CardStateName.Original));
        // the last-known-information copy the simulator takes after every play must work for it too
        AssertJUnit.assertNotNull(copy.getCardsIn(ZoneType.Battlefield));
        copy.copyLastState();
    }

    @Test
    public void copyKeepsTheOtherFaceOfATransformedToken() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card original = addCard("Fable of the Mirror-Breaker", ai);
        // a token copy of a transforming permanent, showing its back face (as a copy of Fable that has transformed)
        final Card token = forge.game.card.CardFactory.getCard(original.getPaperCard(), ai, game.nextCardId(), game);
        token.setGamePieceType(forge.card.GamePieceType.TOKEN);
        game.getAction().moveToPlay(token, null, null);
        token.setState(forge.card.CardStateName.Backside, true);
        AssertJUnit.assertTrue(token.isToken());
        AssertJUnit.assertEquals("Reflection of Kiki-Jiki", token.getName());

        final Game copy = new GameSimulator(game, ai).getSimulatedGameState();
        Card copied = null;
        for (final Card c : copy.getCardsIn(ZoneType.Battlefield)) {
            if (c.getId() == token.getId()) {
                copied = c;
            }
        }
        AssertJUnit.assertNotNull(copied);
        AssertJUnit.assertEquals("Reflection of Kiki-Jiki", copied.getName());
        AssertJUnit.assertTrue(copied.hasState(forge.card.CardStateName.Original));
        copy.copyLastState();
    }

    @Test
    public void copyOfTheStackKeepsTheOrderAndPointsACounterspellAtItsSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCards("Plains", 5, opp);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Serra Angel", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        castFromHand(game, opp, "Serra Angel", null);
        final forge.game.spellability.SpellAbility angel = game.getStack().peekAbility();
        castFromHand(game, ai, "Counterspell", angel);
        AssertJUnit.assertEquals(2, game.getStack().size());

        final Game copy = new forge.ai.simulation.GameCopier(game).withStack(true).makeCopy();
        AssertJUnit.assertEquals("both spells are in the copy", 2, copy.getStack().size());
        AssertJUnit.assertEquals("the counterspell is on top, as in the real game", "Counterspell",
                copy.getStack().peekAbility().getHostCard().getName());
        AssertJUnit.assertEquals(1, copy.getStack().peekAbility().getTargets().size());
        AssertJUnit.assertEquals("and it targets the copy of the angel", "Serra Angel",
                copy.getStack().peekAbility().getTargets().getFirstTargetedSpell().getHostCard().getName());
    }

    private static void castFromHand(final Game game, final Player p, final String name,
            final forge.game.spellability.SpellAbility target) {
        final PlayerControllerAi pc = (PlayerControllerAi) p.getController();
        for (final forge.game.spellability.SpellAbility sa : ComputerUtilAbility.getSpellAbilities(
                ComputerUtilAbility.getAvailableCards(game, p), p)) {
            if (sa.getHostCard().getName().equals(name) && sa.isSpell()) {
                sa.setActivatingPlayer(p);
                if (target != null && sa.usesTargeting()) {
                    sa.resetTargets();
                    sa.getTargets().add(target);
                }
                pc.playChosenSpellAbility(sa);
                return;
            }
        }
        throw new IllegalStateException(p + " cannot cast " + name);
    }

    /** The one-prediction danger check must say what the two separate predictions it replaces said. */
    private static int oldDanger(final Player ai) {
        if (ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, 0, null) == Integer.MIN_VALUE) {
            return 2;
        }
        return ComputerUtil.predictNextCombatsRemainingLife(ai, false, false, 0, null) == Integer.MIN_VALUE ? 1 : 0;
    }

    private void assertSameDanger(final String what, final Player ai) {
        AssertJUnit.assertEquals(what, oldDanger(ai), ComputerUtil.predictNextCombatsDanger(ai, ai.getOpponents()));
    }

    @Test
    public void dangerCheckAgreesWithTheTwoPredictionsItReplaces() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        assertSameDanger("no attackers", ai);
        addCards("Grizzly Bears", 2, opp);
        assertSameDanger("two small attackers, healthy", ai);
        for (final int life : new int[] {20, 12, 8, 5, 4, 3, 2, 1}) {
            ai.setLife(life, null);
            assertSameDanger("two bears at " + life + " life", ai);
        }
        addCards("Serra Angel", 2, opp);
        addCard("Grizzly Bears", ai);
        for (final int life : new int[] {20, 14, 10, 7, 5, 3}) {
            ai.setLife(life, null);
            assertSameDanger("bears and angels against one bear at " + life + " life", ai);
        }
        addCards("Air Elemental", 3, opp);
        for (final int life : new int[] {20, 9, 6}) {
            ai.setLife(life, null);
            assertSameDanger("a big board at " + life + " life", ai);
        }
    }
}

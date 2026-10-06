package forge.ai.simulation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.GameLogVerbosity;
import forge.game.UndoHistory;
import forge.game.UndoRequestedException;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Undo through the real game loop: the game thread is unwound from a prompt and resumes from the
 * restored point. Scripted controllers stand in for the humans, recording points where a human with
 * the given stops would be asked to act.
 */
public class GameUndoLoopTest extends SimulationTest {

    /** Plays scripted choices; as a "human" it records undo points like PlayerControllerHuman does. */
    private static class Scripted extends PlayerControllerAi {
        final boolean human;
        Predicate<Game> stops = g -> false;
        Function<Scripted, List<SpellAbility>> onPriority = c -> null;
        BiConsumer<Player, Combat> onAttack = (p, c) -> { };
        BiConsumer<Player, Combat> onBlock = (p, c) -> { };
        Function<Scripted, Boolean> onLosing = c -> false;
        Card target;
        UndoHistory.Point lastPoint;

        Scripted(Game game, Player p, boolean human) {
            super(game, p, p.getLobbyPlayer());
            this.human = human;
        }

        UndoHistory history() {
            return getGame().getUndoHistory();
        }

        @Override
        public boolean isAI() {
            return !human;
        }

        @Override
        public List<SpellAbility> chooseSpellAbilityToPlay() {
            if (!human || !stops.test(getGame())) {
                return null; // an auto-pass: no prompt, so no point
            }
            lastPoint = history().recordPriority(getPlayer());
            final List<SpellAbility> choice = onPriority.apply(this);
            if (lastPoint != null) {
                lastPoint.setOutcome(choice == null ? "passed" : "cast " + choice.get(0).getHostCard().getName());
            }
            return choice;
        }

        @Override
        public boolean playChosenSpellAbility(SpellAbility sa) {
            sa.setActivatingPlayer(getPlayer());
            return ComputerUtil.handlePlayingSpellAbility(getPlayer(), sa, s -> {
                if (target != null) {
                    s.getTargets().add(target);
                }
            });
        }

        @Override
        public void declareAttackers(Player attacker, Combat combat) {
            onAttack.accept(attacker, combat);
        }

        @Override
        public void declareBlockers(Player defender, Combat combat) {
            final UndoHistory.Point point = human
                    ? history().commitStepStart(getPlayer(), UndoHistory.Kind.DECLARE_BLOCKERS) : null;
            onBlock.accept(defender, combat);
            if (point != null) {
                point.setOutcome("blocked with " + combat.getAllBlockers().size());
            }
        }

        @Override
        public boolean offerUndoInsteadOfLosing() {
            return onLosing.apply(this);
        }

        boolean requestUndoTo(Predicate<UndoHistory.Point> which) {
            final UndoHistory.Point target = history().getUndoTargets(getPlayer()).stream()
                    .filter(which).findFirst().orElseThrow(() -> new AssertionError("no such point: " + history().getPoints()));
            return history().requestUndo(getPlayer(), target);
        }

        /** What the GUI does on Undo: pick a point, then the waiting input gives way. */
        List<SpellAbility> undoTo(Predicate<UndoHistory.Point> which) {
            AssertJUnit.assertTrue(requestUndoTo(which));
            throw new UndoRequestedException();
        }
    }

    private static void playUntil(Game game, Predicate<Game> done) {
        for (int i = 0; i < 500 && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();
            if (done.test(game)) {
                return;
            }
        }
        AssertJUnit.fail("condition not reached; phase " + game.getPhaseHandler().getPhase());
    }

    private static boolean at(Game game, PhaseType phase) {
        return game.getPhaseHandler().is(phase);
    }

    private Game setUp() {
        Game game = initAndCreateGame();
        game.getUndoHistory().setEnabled(true);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 10);
        }
        return game;
    }

    @Test
    public void undoAfterCombatDamageGoesBackToDeclaringBlockers() {
        Game game = setUp();
        Player opp = game.getPlayers().get(0);
        Player human = game.getPlayers().get(1);
        Card giant = addCard("Hill Giant", opp);
        giant.setSickness(false);
        Card bear = addCard("Runeclaw Bear", human);
        bear.setSickness(false);

        Scripted oppCtl = new Scripted(game, opp, false);
        oppCtl.onAttack = (p, combat) -> combat.addAttacker(giant, human);
        opp.dangerouslySetController(oppCtl);

        Scripted humanCtl = new Scripted(game, human, true);
        final int[] blockPrompts = {0};
        final boolean[] undone = {false};
        humanCtl.onBlock = (p, combat) -> {
            // first time the misplay: no blocks; after the undo, block
            if (blockPrompts[0]++ > 0) {
                combat.addBlocker(giant, bear);
            }
        };
        humanCtl.stops = g -> at(g, PhaseType.END_OF_TURN);
        humanCtl.onPriority = c -> {
            if (!undone[0]) {
                undone[0] = true;
                AssertJUnit.assertEquals(17, human.getLife());
                return c.undoTo(p -> p.getKind() == UndoHistory.Kind.DECLARE_BLOCKERS);
            }
            return null;
        };
        human.dangerouslySetController(humanCtl);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getPhaseHandler().onStackResolved();

        playUntil(game, g -> undone[0] && at(g, PhaseType.END_OF_TURN));

        AssertJUnit.assertEquals(2, blockPrompts[0]);
        AssertJUnit.assertEquals(20, human.getLife());
        // the undo is in the log, at the verbosity players have by default and at the lowest one
        for (GameLogVerbosity verbosity : List.of(GameLogVerbosity.MEDIUM, GameLogVerbosity.LOW)) {
            AssertJUnit.assertEquals(1, game.getGameLog().getLogEntriesForVerbosity(verbosity).stream()
                    .filter(e -> e.type() == GameLogEntryType.UNDO && e.message().contains(human.getName())).count());
        }
        AssertJUnit.assertEquals(ZoneType.Graveyard, game.findById(bear.getId()).getZone().getZoneType());
        AssertJUnit.assertEquals(2, game.findById(giant.getId()).getDamage());
        AssertJUnit.assertEquals(opp, game.getPhaseHandler().getPlayerTurn());
    }

    @Test
    public void aLethalMisplayCanBeUndoneBeforeTheGameIsLost() {
        Game game = setUp();
        Player opp = game.getPlayers().get(0);
        Player human = game.getPlayers().get(1);
        human.setLife(3, null);
        Card giant = addCard("Hill Giant", opp);
        giant.setSickness(false);
        Card bear = addCard("Runeclaw Bear", human);
        bear.setSickness(false);

        Scripted oppCtl = new Scripted(game, opp, false);
        oppCtl.onAttack = (p, combat) -> combat.addAttacker(giant, human);
        opp.dangerouslySetController(oppCtl);

        Scripted humanCtl = new Scripted(game, human, true);
        final int[] blockPrompts = {0};
        final int[] lossOffers = {0};
        humanCtl.onBlock = (p, combat) -> {
            if (blockPrompts[0]++ > 0) {
                combat.addBlocker(giant, bear);
            }
        };
        humanCtl.onLosing = c -> {
            lossOffers[0]++;
            return c.requestUndoTo(p -> p.getKind() == UndoHistory.Kind.DECLARE_BLOCKERS);
        };
        human.dangerouslySetController(humanCtl);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getPhaseHandler().onStackResolved();

        playUntil(game, g -> blockPrompts[0] == 2 && at(g, PhaseType.END_OF_TURN));

        AssertJUnit.assertFalse(game.isGameOver());
        AssertJUnit.assertEquals(1, lossOffers[0]);
        AssertJUnit.assertEquals(3, human.getLife());
        AssertJUnit.assertFalse(human.hasLost());
        AssertJUnit.assertEquals(ZoneType.Graveyard, game.findById(bear.getId()).getZone().getZoneType());
    }

    @Test
    public void undoGoesBackToTheResponseAndLetsThePlayerPassInstead() {
        Game game = setUp();
        Player opp = game.getPlayers().get(0);
        Player human = game.getPlayers().get(1);
        Card bear = addCard("Runeclaw Bear", human);
        addCard("Forest", human);
        Card growth = addCardToZone("Giant Growth", human, ZoneType.Hand);
        addCard("Mountain", opp);
        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);

        Scripted oppCtl = new Scripted(game, opp, false);
        oppCtl.target = bear;
        opp.dangerouslySetController(oppCtl);

        Scripted humanCtl = new Scripted(game, human, true);
        humanCtl.target = bear;
        final boolean[] responded = {false};
        final boolean[] undone = {false};
        humanCtl.stops = g -> !g.getStack().isEmpty() || at(g, PhaseType.END_OF_TURN);
        humanCtl.onPriority = c -> {
            if (!game.getStack().isEmpty()) {
                if (!responded[0]) {
                    responded[0] = true;
                    return List.of(game.findById(growth.getId()).getFirstSpellAbility());
                }
                return null;
            }
            if (!undone[0]) {
                undone[0] = true;
                AssertJUnit.assertEquals(ZoneType.Battlefield, game.findById(bear.getId()).getZone().getZoneType());
                return c.undoTo(p -> "cast Giant Growth".equals(p.getOutcome()));
            }
            return null;
        };
        human.dangerouslySetController(humanCtl);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getPhaseHandler().onStackResolved();
        Card boltNow = game.findById(bolt.getId());
        boltNow.getFirstSpellAbility().setActivatingPlayer(opp);
        AssertJUnit.assertTrue(oppCtl.playChosenSpellAbility(boltNow.getFirstSpellAbility()));

        playUntil(game, g -> undone[0] && at(g, PhaseType.END_OF_TURN));

        AssertJUnit.assertEquals(ZoneType.Graveyard, game.findById(bear.getId()).getZone().getZoneType());
        AssertJUnit.assertEquals(ZoneType.Hand, game.findById(growth.getId()).getZone().getZoneType());
        AssertJUnit.assertEquals(ZoneType.Graveyard, game.findById(bolt.getId()).getZone().getZoneType());
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isLand() && !c.isTapped()));
    }
}

/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;

/**
 * Every simulation branch of one candidate scores the same unchanged original game as its baseline,
 * and that evaluation copies the whole game again when there is a combat to look ahead to. The first
 * branch still evaluates it; the rest take that value.
 *
 * <p>The reuse is deliberately scoped to the branches of a single candidate. A baseline taken before
 * candidate generation does <em>not</em> survive to the branches — working out whether a candidate
 * can be played and paid for touches state the evaluator reads — and the shadow check inside
 * {@code GameSimulator} catches exactly that. Because that check runs whenever assertions are on,
 * which is how Surefire runs, the whole simulation suite is a corpus for the narrower invariant this
 * relies on. These tests pin what would otherwise only be covered incidentally: that a supplied
 * baseline is the same value the simulator would have derived, that scores do not move, and that the
 * reuse actually engages.</p>
 */
public class BaselineScoreReuseTest extends SimulationTest {
    @AfterMethod
    public void restoreProbeState() {
        PerfProbe.reset();
    }

    private Game combatLookaheadGame() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        opponent.setLife(14, null);
        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        // creatures on the AI's side before combat are what make the evaluator copy the game to
        // look ahead, so this fixture exercises the expensive form of the baseline evaluation
        for (final Card c : addCards("Grizzly Bears", 2, ai)) {
            c.setSickness(false);
        }
        addCards("Runeclaw Bear", 2, opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    /** A simulator handed a baseline must behave as one that evaluated the baseline itself. */
    @Test(timeOut = 300000)
    public void suppliedBaselineMatchesTheEvaluatedOne() {
        final Game game = combatLookaheadGame();
        final Player ai = game.getPlayers().get(1);

        final Score evaluated = new GameStateEvaluator().getScoreForGameState(game, ai);

        final SimulationController controller = new SimulationController(evaluated, 0);
        final GameSimulator selfEvaluating = new GameSimulator(controller, game, ai, null);
        final GameSimulator supplied = new GameSimulator(controller, game, ai, null, evaluated);

        Assert.assertTrue(supplied.getScoreForOrigGame().equals(selfEvaluating.getScoreForOrigGame()),
                "the supplied baseline differs from the one the simulator would have evaluated");
        Assert.assertTrue(supplied.getScoreForOrigGame().equals(evaluated));
    }

    /**
     * Candidate scores do not move, and the reuse really is engaging — a fast path that quietly fell
     * back to re-evaluating every branch would pass the first half of this alone. Several candidates
     * are evaluated because only ones with more than one branch can reuse anything: the first branch
     * of every candidate still evaluates its own baseline.
     */
    @Test(timeOut = 300000)
    public void candidateScoresAreUnchangedAndTheBaselineIsReused() {
        final Game game = combatLookaheadGame();
        final Player ai = game.getPlayers().get(1);

        final SpellAbilityPicker picker = new SpellAbilityPicker(ai);
        final List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();
        Assert.assertFalse(candidates.isEmpty(), "the fixture must offer the AI something to consider");

        final Score baseline = new GameStateEvaluator().getScoreForGameState(game, ai);
        final PhaseType phase = game.getPhaseHandler().getPhase();

        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        final List<String> first = new ArrayList<>();
        final List<String> second = new ArrayList<>();
        try {
            for (int i = 0; i < candidates.size(); i++) {
                first.add(String.valueOf(
                        picker.evaluateSa(new SimulationController(baseline, 0), phase, candidates, i)));
            }
            for (int i = 0; i < candidates.size(); i++) {
                second.add(String.valueOf(
                        picker.evaluateSa(new SimulationController(baseline, 0), phase, candidates, i)));
            }
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertEquals(second, first, "evaluating the same candidates twice gave different scores");
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.BASELINE_SCORE_REUSES) > 0L,
                "no branch actually reused the baseline, so this proved nothing");
    }
}

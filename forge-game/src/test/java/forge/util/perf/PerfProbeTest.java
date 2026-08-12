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
package forge.util.perf;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Mechanics tests for the AI measurement probes.
 *
 * <p>These guard the properties the rest of the performance programme relies on: probes cost nothing
 * and record nothing while disabled, nested spans and nested decisions do not double count, records
 * survive being written from the watchdog thread, and a tracing generator reproduces the exact
 * sequence of the plain one it replaces.</p>
 */
public class PerfProbeTest {
    @BeforeMethod
    public void setUp() {
        PerfProbe.reset();
    }

    @AfterMethod
    public void tearDown() {
        PerfProbe.reset();
    }

    @Test
    public void disabledProbesRecordNothing() {
        Assert.assertFalse(PerfProbe.isEnabled(), "probing must be off by default");

        final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "p", 1, "MAIN1");
        Assert.assertNull(record, "no record may be created while disabled");

        final long token = PerfProbe.start(PerfTimer.DECISION);
        PerfProbe.count(PerfCounter.CAN_PLAY_CHECKS, 5L);
        PerfProbe.stop(PerfTimer.DECISION, token);
        PerfProbe.endDecision(record);

        Assert.assertEquals(PerfProbe.getGlobal().get(PerfCounter.CAN_PLAY_CHECKS), 0L);
        Assert.assertEquals(PerfProbe.getGlobal().getSpanCount(PerfTimer.DECISION), 0L);
    }

    @Test
    public void countersAndSpansLandOnBothDecisionAndGlobal() {
        PerfProbe.setEnabled(true);
        final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 3, "MAIN2");
        Assert.assertNotNull(record);
        Assert.assertSame(PerfProbe.getCurrentDecision(), record);

        final long token = PerfProbe.start(PerfTimer.GAME_COPY);
        PerfProbe.count(PerfCounter.GAME_COPY_CARDS, 42L);
        PerfProbe.stop(PerfTimer.GAME_COPY, token);
        PerfProbe.endDecision(record);

        Assert.assertEquals(record.get(PerfCounter.GAME_COPY_CARDS), 42L);
        Assert.assertEquals(record.getSpanCount(PerfTimer.GAME_COPY), 1L);
        Assert.assertTrue(record.getNanos(PerfTimer.GAME_COPY) >= 0L);
        Assert.assertTrue(record.getDurationNanos() >= 0L, "a closed decision must have a duration");
        Assert.assertEquals(record.getKind(), DecisionKind.PRIORITY);
        Assert.assertEquals(record.getTurn(), 3);
        Assert.assertEquals(record.getPhase(), "MAIN2");

        Assert.assertEquals(PerfProbe.getGlobal().get(PerfCounter.GAME_COPY_CARDS), 42L);
        Assert.assertNull(PerfProbe.getCurrentDecision(), "the decision must be cleared when it ends");
    }

    @Test
    public void reentrantSpansAreFoldedIntoTheOutermost() {
        PerfProbe.setEnabled(true);
        final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");

        final long outer = PerfProbe.start(PerfTimer.STATIC_ABILITIES);
        final long inner = PerfProbe.start(PerfTimer.STATIC_ABILITIES);
        final long innermost = PerfProbe.start(PerfTimer.STATIC_ABILITIES);
        PerfProbe.stop(PerfTimer.STATIC_ABILITIES, innermost);
        PerfProbe.stop(PerfTimer.STATIC_ABILITIES, inner);
        PerfProbe.stop(PerfTimer.STATIC_ABILITIES, outer);
        PerfProbe.endDecision(record);

        Assert.assertEquals(record.getSpanCount(PerfTimer.STATIC_ABILITIES), 1L,
                "a recursive rules path must not multiply its own elapsed time");
    }

    @Test
    public void nestedDecisionsAreAttributedToTheOutermost() {
        PerfProbe.setEnabled(true);
        final List<DecisionRecord> delivered = new ArrayList<>();
        PerfProbe.addSink(delivered::add);

        final DecisionRecord outer = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");
        // A simulated branch resolving combat calls back into the block controller.
        final DecisionRecord nested = PerfProbe.beginDecision(DecisionKind.BLOCK, "ai", 1, "DECLARE_BLOCKERS");
        PerfProbe.count(PerfCounter.POSSIBLE_BLOCKERS, 7L);
        PerfProbe.endDecision(nested);
        PerfProbe.endDecision(outer);

        Assert.assertEquals(delivered.size(), 1, "only the outermost decision may be reported");
        Assert.assertEquals(delivered.get(0).getKind(), DecisionKind.PRIORITY);
        Assert.assertEquals(delivered.get(0).get(PerfCounter.POSSIBLE_BLOCKERS), 7L,
                "nested work must be attributed to the decision the player waits for");

        // The depth bookkeeping must be balanced, or the next decision would be swallowed.
        final DecisionRecord next = PerfProbe.beginDecision(DecisionKind.ATTACK, "ai", 2, "DECLARE_ATTACKERS");
        Assert.assertNotNull(next);
        PerfProbe.endDecision(next);
        Assert.assertEquals(delivered.size(), 2);
    }

    @Test
    public void recordsSurviveWritesFromTheEvaluationThread() throws Exception {
        PerfProbe.setEnabled(true);
        final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");

        // Mirrors the real shape: the game thread opens the decision, the "Game AI Eval" thread does
        // the work, and the game thread closes it.
        final CountDownLatch done = new CountDownLatch(1);
        final Thread worker = new Thread(() -> {
            final long token = PerfProbe.start(PerfTimer.CANDIDATE_EVALUATION);
            for (int i = 0; i < 1000; i++) {
                PerfProbe.count(PerfCounter.CAN_PLAY_CHECKS);
            }
            PerfProbe.stop(PerfTimer.CANDIDATE_EVALUATION, token);
            done.countDown();
        }, "Game AI Eval");
        worker.start();
        Assert.assertTrue(done.await(10, TimeUnit.SECONDS), "worker must finish");
        worker.join();
        PerfProbe.endDecision(record);

        Assert.assertEquals(record.get(PerfCounter.CAN_PLAY_CHECKS), 1000L);
        Assert.assertEquals(record.getSpanCount(PerfTimer.CANDIDATE_EVALUATION), 1L);
    }

    @Test
    public void tracingIsSeparatelyGatedAndOrdered() {
        PerfProbe.setEnabled(true);
        Assert.assertFalse(PerfProbe.isTracing(), "enabling probing must not enable tracing");

        DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");
        PerfProbe.trace(TraceCategory.CANDIDATE, "ignored");
        PerfProbe.endDecision(record);
        Assert.assertTrue(record.getTrace().isEmpty());

        PerfProbe.setTracing(true);
        record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");
        PerfProbe.trace(TraceCategory.CANDIDATE, "first");
        PerfProbe.trace(TraceCategory.CANDIDATE, "second");
        PerfProbe.trace(TraceCategory.CHOSEN, "first");
        PerfProbe.endDecision(record);

        Assert.assertEquals(record.getTrace(),
                List.of("candidate\tfirst", "candidate\tsecond", "chosen\tfirst"),
                "trace order is the parity artefact and must be preserved exactly");
    }

    @Test
    public void reportComputesNearestRankPercentiles() {
        PerfProbe.setEnabled(true);
        final PerfReport report = new PerfReport("test");
        PerfProbe.addSink(report);

        for (int i = 0; i < 100; i++) {
            final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", i, "MAIN1");
            PerfProbe.count(PerfCounter.CANDIDATE_ABILITIES, 2L);
            PerfProbe.endDecision(record);
        }

        Assert.assertEquals(report.getDecisionCount(DecisionKind.PRIORITY), 100);
        Assert.assertEquals(report.getDecisionCount(), 100);
        Assert.assertEquals(report.getTotals(DecisionKind.PRIORITY).get(PerfCounter.CANDIDATE_ABILITIES), 200L);
        Assert.assertEquals(report.getDecisionCount(DecisionKind.ATTACK), 0);
        Assert.assertNull(report.getTotals(DecisionKind.ATTACK));
        Assert.assertEquals(report.getDurationPercentileNanos(DecisionKind.ATTACK, 50.0d), -1L);

        final long median = report.getDurationPercentileNanos(DecisionKind.PRIORITY, 50.0d);
        final long p99 = report.getDurationPercentileNanos(DecisionKind.PRIORITY, 99.0d);
        final long max = report.getDurationPercentileNanos(DecisionKind.PRIORITY, 100.0d);
        Assert.assertTrue(median >= 0L);
        Assert.assertTrue(p99 >= median);
        Assert.assertTrue(max >= p99);

        final String json = report.toJson();
        Assert.assertTrue(json.contains("\"kind\": \"priority\""), json);
        Assert.assertTrue(json.contains("\"count\": 100"), json);
        Assert.assertTrue(json.contains("\"candidateAbilities\": 200"), json);
    }

    @Test
    public void traceWriterEmitsOneJsonLinePerTracedDecision() {
        PerfProbe.setTracing(true);
        final StringWriter out = new StringWriter();
        PerfProbe.addSink(new DecisionTraceWriter(out));

        DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "Ai(1)", 4, "MAIN1");
        PerfProbe.trace(TraceCategory.CHOSEN, "c17:Lightning Bolt");
        PerfProbe.endDecision(record);

        // A decision with nothing traced must not produce a line at all: empty lines would make two
        // otherwise identical trace files differ for a reason unrelated to behaviour.
        record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "Ai(1)", 5, "MAIN1");
        PerfProbe.endDecision(record);

        final String[] lines = out.toString().split("\n");
        Assert.assertEquals(lines.length, 1, out.toString());
        Assert.assertTrue(lines[0].startsWith("{\"id\": "), lines[0]);
        Assert.assertTrue(lines[0].contains("\"kind\": \"priority\""), lines[0]);
        Assert.assertTrue(lines[0].contains("\"turn\": 4"), lines[0]);
        Assert.assertTrue(lines[0].contains("chosen\\tc17:Lightning Bolt"), lines[0]);
    }

    @Test
    public void tracingRandomReproducesThePlainSequence() {
        final long seed = 20260812L;
        final Random plain = new Random(seed);
        final TracingRandom traced = new TracingRandom(seed);

        // Every Random method funnels through next(int), so overriding only that observes the stream
        // without altering it. If this ever diverges, an RNG-scoping change could silently alter play.
        for (int i = 0; i < 500; i++) {
            Assert.assertEquals(traced.nextInt(), plain.nextInt());
            Assert.assertEquals(traced.nextInt(37), plain.nextInt(37));
            Assert.assertEquals(traced.nextLong(), plain.nextLong());
            Assert.assertEquals(traced.nextBoolean(), plain.nextBoolean());
            Assert.assertEquals(traced.nextDouble(), plain.nextDouble(), 0.0d);
        }
        Assert.assertTrue(traced.getDrawCount() > 0L);
    }

    @Test
    public void aFailingSinkCannotBreakADecision() {
        PerfProbe.setEnabled(true);
        PerfProbe.addSink(record -> {
            throw new IllegalStateException("sink is broken");
        });
        final List<DecisionRecord> delivered = new ArrayList<>();
        PerfProbe.addSink(delivered::add);

        final DecisionRecord record = PerfProbe.beginDecision(DecisionKind.PRIORITY, "ai", 1, "MAIN1");
        PerfProbe.endDecision(record);

        Assert.assertEquals(delivered.size(), 1, "a broken sink must not stop later sinks or the game");
    }
}

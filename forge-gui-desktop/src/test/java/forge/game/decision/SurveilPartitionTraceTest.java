package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.PlayerControllerForTests;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SurveilPartitionTraceTest extends AITest {
    public static void main(final String[] args) throws Exception {
        if (args.length == 3 && "symmetry-diagnostic".equals(args[0])) {
            System.setProperty(SurveilPartitionDiagnostics.AUDIT_ENABLED_PROPERTY, "true");
            System.setProperty(SurveilPartitionDiagnostics.AUDIT_OUTPUT_PROPERTY, args[1]);
            final SurveilPartitionTraceTest test = new SurveilPartitionTraceTest();
            test.initializeModel();
            test.runSymmetryDiagnosticScenario(args[2]);
            return;
        }
        if (args.length == 2 && "mixed-symmetry-diagnostic".equals(args[0])) {
            System.setProperty(SurveilPartitionDiagnostics.AUDIT_ENABLED_PROPERTY, "true");
            System.setProperty(SurveilPartitionDiagnostics.AUDIT_OUTPUT_PROPERTY, args[1]);
            final SurveilPartitionTraceTest test = new SurveilPartitionTraceTest();
            test.initializeModel();
            final Fixture fixture = test.fixture("Island", "Island");
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                    new CardCollection(fixture.cards().get(1)), new CardCollection(fixture.cards().get(0)));
            coordinator().captureNativeSurveil(fixture.chooser(), new CardCollection(fixture.cards()),
                    ignored -> nativePair);
        }
    }

    @Test
    public void surveilProfileIsExplicitAndUsesV3Metadata() {
        final DecisionTraceRequestRecord record = DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V3|REQUEST|1|0|MAIN|1|CARD_SELECTION|SURVEIL_PARTITION|0|false|"
                        + "[SURVEIL_PARTITION%7CCLASSIFY_GRAVEYARD%7C1,SURVEIL_PARTITION%7CCLASSIFY_RETAIN%7C1]"
                        + "|hash|SURVEIL_PARTITION|NOT_APPLICABLE");

        assertTrue(record.isSurveilPartitionRequest());
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(record,
                new DecisionTraceResultRecord(1, DecisionTraceResultKind.CHOSEN,
                        "SURVEIL_PARTITION|CLASSIFY_GRAVEYARD|1", true, true,
                        false, false, false)));
    }

    @Test
    public void historicalV2RecordsDoNotInferSurveilFromStageText() {
        final DecisionTraceRequestRecord record = DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V2|REQUEST|1|0|MAIN|1|CARD_SELECTION|SURVEIL_PARTITION|0|false|[]|hash");

        assertFalse(record.isSurveilPartitionRequest());
    }

    @Test
    public void surveilMetadataDoesNotClassifyOrderOrTargetAsSurveilPartition() {
        for (final String decisionType : List.of("ORDER", "TARGET")) {
            final DecisionTraceRequestRecord record = DecisionTraceRequestRecord.fromSerializedRequest(
                    "DECISION_TRACE_V3|REQUEST|1|0|MAIN|1|" + decisionType
                            + "|SURVEIL_PARTITION|0|false|"
                            + "[SURVEIL_PARTITION%7CCLASSIFY_RETAIN%7C1]|hash|SURVEIL_PARTITION|BC_ELIGIBLE");
            final DecisionTraceResultRecord chosen = new DecisionTraceResultRecord(1,
                    DecisionTraceResultKind.CHOSEN, "SURVEIL_PARTITION|CLASSIFY_RETAIN|1",
                    true, true, false, false, false);

            assertFalse(record.isSurveilPartitionRequest());
            assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(record, chosen));
        }
    }

    @Test
    public void coordinatorMaterializesOnlyAfterNativeCallbackInCanonicalSequentialPairs() throws Exception {
        final Fixture fixture = fixture("Island", "Forest", "Mountain");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionDecisionCoordinator coordinator =
                new SurveilPartitionDecisionCoordinator(provider);
        final AtomicInteger callbackCount = new AtomicInteger();
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                    new CardCollection(fixture.cards()), new CardCollection());
            final Pair<CardCollection, CardCollection> result = coordinator.captureNativeSurveil(
                    fixture.chooser(), new CardCollection(List.of(
                            fixture.cards().get(0), fixture.cards().get(1), fixture.cards().get(2))), ignored -> {
                        assertEquals(capture.trace().requestCountForTesting(), 0);
                        assertEquals(capture.trace().openRequestCountForTesting(), 0);
                        callbackCount.incrementAndGet();
                        return nativePair;
                    });

            assertSame(result, nativePair);
            assertEquals(callbackCount.get(), 1);
            assertEquals(capture.trace().openRequestCountForTesting(), 0);
            assertTrue(capture.trace().maxOpenRequestCountForTesting() <= 1);
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertEquals(rows.size(), 6);
            for (int step = 0; step < 3; step++) {
                final String request = rows.get(step * 2);
                final String resultRow = rows.get(step * 2 + 1);
                final String[] requestFields = request.split("\\|", -1);
                final String[] resultFields = resultRow.split("\\|", -1);
                assertEquals(requestFields[0], "DECISION_TRACE_V3");
                assertEquals(requestFields[1], "REQUEST");
                assertEquals(requestFields[6], "CARD_SELECTION");
                assertEquals(requestFields[7], "SURVEIL_PARTITION");
                assertEquals(requestFields[8], Integer.toString(step));
                assertEquals(requestFields[12], "SURVEIL_PARTITION");
                assertEquals(requestFields[13], "NOT_APPLICABLE");
                assertEquals(resultFields[0], "DECISION_TRACE_V3");
                assertEquals(resultFields[1], "RESULT");
                assertEquals(resultFields[2], requestFields[2]);
                assertEquals(resultFields[5], "true");
                assertEquals(resultFields[6], "true");
                assertEquals(resultFields[4],
                        ("SURVEIL_PARTITION|CLASSIFY_RETAIN|"
                                + SurveilPartitionItemId.opaqueItemId(step + 1)).replace("|", "%7C"));
            }
        } finally {
            capture.close();
        }
    }

    @Test
    public void mixedPublicSymmetryIsDiagnosticOnlyAndNativeOwnersRemainNotApplicable() throws Exception {
        final Fixture fixture = fixture("Island", "Island");
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                    new CardCollection(fixture.cards().get(1)), new CardCollection(fixture.cards().get(0)));
            coordinator.captureNativeSurveil(fixture.chooser(), new CardCollection(fixture.cards()), ignored -> nativePair);
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertEquals(rows.stream().filter(row -> row.contains("|REQUEST|")).count(), 2L);
            assertTrue(rows.stream().filter(row -> row.contains("|REQUEST|"))
                    .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
            assertFalse(rows.stream().anyMatch(row -> row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")));
            final DiagnosticSnapshot diagnostics = runSymmetryDiagnosticChild("mixed");
            assertEquals(diagnostics.publicSymmetryConflicts(), 1L,
                    "mixed public labels must record one concrete diagnostic conflict");
            assertEquals(diagnostics.teacherEligibilityNotApplicable(), 2L);
            assertEquals(diagnostics.teacherEligibilityBcEligible(), 0L);
            assertEquals(diagnostics.teacherEligibilityBcExcludedPublicSymmetry(), 0L);
        } finally {
            capture.close();
        }
    }

    @Test
    public void publicSymmetryDiagnosticsCountersDistinguishConflictFreeAndMixedGroups() throws Exception {
        final DiagnosticSnapshot graveyardGroup = runSymmetryDiagnosticChild("graveyard-graveyard");
        final DiagnosticSnapshot retainedGroup = runSymmetryDiagnosticChild("retain-retain");
        final DiagnosticSnapshot mixedGroup = runSymmetryDiagnosticChild("mixed");

        assertEquals(graveyardGroup.publicSymmetryConflicts(), 0L);
        assertEquals(graveyardGroup.teacherEligibilityNotApplicable(), 2L);
        assertEquals(graveyardGroup.teacherEligibilityBcEligible(), 0L);
        assertEquals(graveyardGroup.teacherEligibilityBcExcludedPublicSymmetry(), 0L);
        assertEquals(retainedGroup, graveyardGroup);

        assertEquals(mixedGroup.publicSymmetryConflicts(), 1L);
        assertEquals(mixedGroup.teacherEligibilityNotApplicable(), 2L);
        assertEquals(mixedGroup.teacherEligibilityBcEligible(), 0L);
        assertEquals(mixedGroup.teacherEligibilityBcExcludedPublicSymmetry(), 0L);
    }

    @Test
    public void publicSymmetryDiagnosticsAreInvariantAcrossNativeOwnersAndRetainedOrders() throws Exception {
        final DiagnosticSnapshot baseline = runSymmetryDiagnosticChild("ai-order-a");
        for (final String scenario : List.of("ai-order-b", "human-order-a", "human-order-b")) {
            assertEquals(runSymmetryDiagnosticChild(scenario), baseline, scenario);
        }

        assertEquals(baseline.publicSymmetryConflicts(), 1L);
        assertEquals(baseline.teacherEligibilityNotApplicable(), 3L);
        assertEquals(baseline.teacherEligibilityBcEligible(), 0L);
        assertEquals(baseline.teacherEligibilityBcExcludedPublicSymmetry(), 0L);
    }

    @Test
    public void equalPublicSymmetryLabelsRemainConflictFreeAndNotApplicable() throws Exception {
        final Fixture fixture = fixture("Island", "Island");
        final SurveilPartitionDecisionCoordinator coordinator = coordinator();
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                    new CardCollection(), new CardCollection(fixture.cards()));
            coordinator.captureNativeSurveil(fixture.chooser(), new CardCollection(fixture.cards()), ignored -> nativePair);
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertTrue(rows.stream().filter(row -> row.contains("|REQUEST|"))
                    .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
            assertFalse(rows.stream().anyMatch(row -> row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")));
        } finally {
            capture.close();
        }
    }

    @Test
    public void retainedOrderPermutationDoesNotChangeMembershipLabelsOrCurrentEligibility() throws Exception {
        final Fixture firstFixture = fixture("Island", "Island", "Forest");
        final Fixture secondFixture = fixture("Island", "Island", "Forest");
        final List<String> firstRows = captureNativePartition(firstFixture,
                new CardCollection(List.of(firstFixture.cards().get(2), firstFixture.cards().get(0))),
                new CardCollection(firstFixture.cards().get(1)));
        final List<String> secondRows = captureNativePartition(secondFixture,
                new CardCollection(List.of(secondFixture.cards().get(0), secondFixture.cards().get(2))),
                new CardCollection(secondFixture.cards().get(1)));

        final List<String> firstLabels = resultLabels(firstRows);
        final List<String> secondLabels = resultLabels(secondRows);
        assertEquals(firstLabels, secondLabels);
        assertEquals(firstLabels, List.of("CLASSIFY_RETAIN", "CLASSIFY_RETAIN", "CLASSIFY_GRAVEYARD"));
        assertTrue(firstRows.stream().filter(row -> row.contains("|REQUEST|"))
                .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
        assertTrue(secondRows.stream().filter(row -> row.contains("|REQUEST|"))
                .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
        assertFalse(firstRows.stream().anyMatch(row -> row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")));
        assertFalse(secondRows.stream().anyMatch(row -> row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")));
        assertFalse(firstRows.stream().anyMatch(row -> row.contains("BC_ELIGIBLE")));
        assertFalse(secondRows.stream().anyMatch(row -> row.contains("BC_ELIGIBLE")));
    }

    @Test
    public void surveilTeacherValidatorRequiresExplicitProfileParityGateAndEligibility() {
        final DecisionTraceResultRecord chosen = new DecisionTraceResultRecord(1,
                DecisionTraceResultKind.CHOSEN, "SURVEIL_PARTITION|CLASSIFY_RETAIN|1",
                true, true, false, false, false);

        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "NOT_APPLICABLE"), chosen));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "BC_EXCLUDED_PUBLIC_SYMMETRY"), chosen));
        assertTrue(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "BC_ELIGIBLE"), chosen));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("OTHER", "BC_ELIGIBLE"), chosen));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "BC_ELIGIBLE", "OTHER"), chosen));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "BC_ELIGIBLE"), null));
        assertFalse(DecisionTraceTrainingValidator.isBCPolicySample(
                surveilRequest("SURVEIL_PARTITION", "BC_ELIGIBLE"),
                new DecisionTraceResultRecord(1, DecisionTraceResultKind.CHOSEN,
                        "SURVEIL_PARTITION|CLASSIFY_RETAIN|1", false, true,
                        false, false, false)));
    }

    @Test
    public void serializedSurveilRowsContainNoNativeIdentityOrPrivateOrderingPayload() throws Exception {
        final Fixture fixture = fixture("Mountain", "Island");
        final Card first = fixture.cards().get(0);
        final Card second = fixture.cards().get(1);
        first.setGameTimestamp(987654321L);
        second.setGameTimestamp(987654322L);
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            coordinator().captureNativeSurveil(fixture.chooser(), new CardCollection(List.of(first, second)),
                    ignored -> new ImmutablePair<>(new CardCollection(first), new CardCollection(second)));
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertTrue(rows.stream().allMatch(row -> row.startsWith("DECISION_TRACE_V3|")));
            assertTrue(rows.stream().noneMatch(row -> row.contains(Integer.toHexString(System.identityHashCode(first)))
                    || row.contains(Integer.toHexString(System.identityHashCode(second)))));
            assertTrue(rows.stream().noneMatch(row -> row.contains(Long.toString(first.getGameTimestamp()))
                    || row.contains(Long.toString(second.getGameTimestamp()))));
            assertTrue(rows.stream().noneMatch(row -> row.contains("CardLKI")
                    || row.contains("SpellAbility") || row.contains("originalLibraryPosition")
                    || row.contains("retainedOrder") || row.contains("RNG")));
        } finally {
            capture.close();
        }
    }

    @Test
    public void nativeHumanAndAiParityIsUnprovenSoBothRemainNotApplicable() throws Exception {
        final Fixture aiFixture = fixture("Island");
        final Fixture humanFixture = fixture("Island");
        final PlayerControllerForTests humanController = new PlayerControllerForTests(
                humanFixture.game(), humanFixture.chooser(), humanFixture.chooser().getOriginalLobbyPlayer());
        humanFixture.chooser().dangerouslySetController(humanController);
        assertTrue(aiFixture.chooser().getController().isAI());
        assertFalse(humanFixture.chooser().getController().isAI());
        assertNativeOwnerNotApplicable(aiFixture);
        assertNativeOwnerNotApplicable(humanFixture);
    }

    @Test
    public void nativeAiAndHumanRetainedPermutationsKeepLabelsAndNotApplicableEligibility() throws Exception {
        final Fixture aiFixture = fixture("Island", "Island", "Forest");
        final Fixture humanFixture = fixture("Island", "Island", "Forest");
        final PlayerControllerForTests humanController = new PlayerControllerForTests(
                humanFixture.game(), humanFixture.chooser(), humanFixture.chooser().getOriginalLobbyPlayer());
        humanFixture.chooser().dangerouslySetController(humanController);

        final List<String> aiRows = captureNativePartition(
                aiFixture.chooser().getController().getSurveilPartitionDecisionCoordinator(), aiFixture,
                new CardCollection(List.of(aiFixture.cards().get(2), aiFixture.cards().get(0))),
                new CardCollection(aiFixture.cards().get(1)));
        final List<String> humanRows = captureNativePartition(
                humanFixture.chooser().getController().getSurveilPartitionDecisionCoordinator(), humanFixture,
                new CardCollection(List.of(humanFixture.cards().get(0), humanFixture.cards().get(2))),
                new CardCollection(humanFixture.cards().get(1)));

        assertTrue(aiFixture.chooser().getController().isAI());
        assertFalse(humanFixture.chooser().getController().isAI());
        assertEquals(resultLabels(aiRows), resultLabels(humanRows));
        for (final List<String> rows : List.of(aiRows, humanRows)) {
            assertTrue(rows.stream().filter(row -> row.contains("|REQUEST|"))
                    .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
            assertFalse(rows.stream().anyMatch(row -> row.contains("BC_ELIGIBLE")
                    || row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")));
        }
    }

    private static void assertNativeOwnerNotApplicable(final Fixture fixture) throws Exception {
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            final Card card = fixture.cards().get(0);
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(
                    new CardCollection(), new CardCollection(card));
            fixture.chooser().getController().getSurveilPartitionDecisionCoordinator()
                    .captureNativeSurveil(fixture.chooser(), new CardCollection(card), ignored -> nativePair);
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertTrue(rows.stream().anyMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")));
            assertFalse(rows.stream().anyMatch(row -> row.endsWith("|SURVEIL_PARTITION|BC_ELIGIBLE")));
        } finally {
            capture.close();
        }
    }

    private List<String> captureNativePartition(final Fixture fixture,
            final CardCollection retained, final CardCollection graveyard) throws Exception {
        return captureNativePartition(coordinator(), fixture, retained, graveyard);
    }

    private List<String> captureNativePartition(final SurveilPartitionDecisionCoordinator coordinator,
            final Fixture fixture, final CardCollection retained, final CardCollection graveyard) throws Exception {
        final TraceCapture capture = attachTrace(fixture.game());
        try {
            coordinator.captureNativeSurveil(fixture.chooser(),
                    new CardCollection(fixture.cards()),
                    ignored -> new ImmutablePair<>(retained, graveyard));
            return capture.finishAndReadDecisionTrace();
        } finally {
            capture.close();
        }
    }

    private static List<String> resultLabels(final List<String> rows) {
        return rows.stream()
                .filter(row -> row.contains("|RESULT|"))
                .map(row -> row.contains("CLASSIFY_GRAVEYARD")
                        ? "CLASSIFY_GRAVEYARD" : "CLASSIFY_RETAIN")
                .toList();
    }

    private static DiagnosticSnapshot runSymmetryDiagnosticChild(final String scenario) throws Exception {
        final Path temporaryDirectory = Files.createTempDirectory("frl02l2a-symmetry-diagnostic-");
        final Path output = temporaryDirectory.resolve("audit.properties");
        Process child = null;
        try {
            final List<String> command = List.of(javaExecutable().toString(),
                    "-Djava.io.tmpdir=" + temporaryDirectory,
                    "-cp", System.getProperty("java.class.path"),
                    SurveilPartitionTraceTest.class.getName(),
                    "symmetry-diagnostic", output.toString(), scenario);
            child = new ProcessBuilder(command)
                    .directory(repositoryRoot().resolve("forge-gui").toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!child.waitFor(120, TimeUnit.SECONDS)) {
                child.destroyForcibly();
                child.waitFor(5, TimeUnit.SECONDS);
                throw new AssertionError("symmetry diagnostic child timed out: " + scenario);
            }
            final String childOutput = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(child.exitValue(), 0, childOutput);
            final Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            assertEquals(properties.getProperty("schema"), SurveilPartitionDiagnostics.AUDIT_SCHEMA);
            return new DiagnosticSnapshot(
                    counter(properties, "public_symmetry_conflicts"),
                    counter(properties, "teacher_eligibility_not_applicable_count"),
                    counter(properties, "teacher_eligibility_bc_eligible_count"),
                    counter(properties, "teacher_eligibility_bc_excluded_public_symmetry_count"));
        } finally {
            if (child != null && child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(5, TimeUnit.SECONDS);
            }
            deleteTree(temporaryDirectory);
        }
    }

    private void runSymmetryDiagnosticScenario(final String scenario) throws Exception {
        final boolean humanOwner = scenario.startsWith("human-");
        final Fixture fixture;
        final CardCollection retained;
        final CardCollection graveyard;
        switch (scenario) {
            case "graveyard-graveyard":
                fixture = fixture("Island", "Island");
                retained = new CardCollection();
                graveyard = new CardCollection(fixture.cards());
                break;
            case "retain-retain":
                fixture = fixture("Island", "Island");
                retained = new CardCollection(fixture.cards());
                graveyard = new CardCollection();
                break;
            case "mixed":
                fixture = fixture("Island", "Island");
                retained = new CardCollection(fixture.cards().get(1));
                graveyard = new CardCollection(fixture.cards().get(0));
                break;
            case "ai-order-a":
            case "human-order-a":
            case "ai-order-b":
            case "human-order-b":
                fixture = fixture("Island", "Island", "Forest");
                final boolean orderB = scenario.endsWith("order-b");
                retained = new CardCollection(orderB
                        ? List.of(fixture.cards().get(0), fixture.cards().get(2))
                        : List.of(fixture.cards().get(2), fixture.cards().get(0)));
                graveyard = new CardCollection(fixture.cards().get(1));
                break;
            default:
                throw new IllegalArgumentException("unknown symmetry diagnostic scenario: " + scenario);
        }

        if (humanOwner) {
            final PlayerControllerForTests humanController = new PlayerControllerForTests(
                    fixture.game(), fixture.chooser(), fixture.chooser().getOriginalLobbyPlayer());
            fixture.chooser().dangerouslySetController(humanController);
        }

        final TraceCapture capture = attachTrace(fixture.game());
        try {
            final Pair<CardCollection, CardCollection> nativePair = new ImmutablePair<>(retained, graveyard);
            fixture.chooser().getController().getSurveilPartitionDecisionCoordinator()
                    .captureNativeSurveil(fixture.chooser(), new CardCollection(fixture.cards()), ignored -> nativePair);
            final List<String> rows = capture.finishAndReadDecisionTrace();
            assertEquals(rows.stream().filter(row -> row.contains("|REQUEST|")).count(),
                    (long) fixture.cards().size(), scenario);
            assertTrue(rows.stream().filter(row -> row.contains("|REQUEST|"))
                    .allMatch(row -> row.endsWith("|SURVEIL_PARTITION|NOT_APPLICABLE")), scenario);
            assertFalse(rows.stream().anyMatch(row -> row.contains("BC_ELIGIBLE")
                    || row.contains("BC_EXCLUDED_PUBLIC_SYMMETRY")), scenario);
        } finally {
            capture.close();
        }
    }

    private static long counter(final Properties properties, final String key) {
        final String value = properties.getProperty(key);
        assertTrue(value != null, "missing diagnostics counter: " + key);
        return Long.parseLong(value);
    }

    private static Path javaExecutable() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String executableName = osName.startsWith("windows") ? "java.exe" : "java";
        final Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);
        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("Java executable does not exist: " + executable);
        }
        return executable;
    }

    private static Path repositoryRoot() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
                ? workingDirectory.getParent() : workingDirectory;
    }

    private static DecisionTraceRequestRecord surveilRequest(final String eligibility) {
        return surveilRequest("SURVEIL_PARTITION", eligibility, "SURVEIL_PARTITION");
    }

    private static DecisionTraceRequestRecord surveilRequest(final String profile, final String eligibility) {
        return surveilRequest(profile, eligibility, "SURVEIL_PARTITION");
    }

    private static DecisionTraceRequestRecord surveilRequest(final String profile, final String eligibility,
            final String adapter) {
        return DecisionTraceRequestRecord.fromSerializedRequest(
                "DECISION_TRACE_V3|REQUEST|1|0|MAIN|1|CARD_SELECTION|" + adapter + "|0|false|"
                        + "[SURVEIL_PARTITION%7CCLASSIFY_GRAVEYARD%7C1,SURVEIL_PARTITION%7CCLASSIFY_RETAIN%7C1]"
                        + "|hash|" + profile + "|" + eligibility);
    }

    private static SurveilPartitionDecisionCoordinator coordinator() {
        return new SurveilPartitionDecisionCoordinator(new SurveilPartitionDecisionProvider());
    }

    private Fixture fixture(final String... names) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = new ArrayList<>();
        for (final String name : names) {
            cards.add(addCardToZone(name, chooser, ZoneType.Hand));
        }
        return new Fixture(game, chooser, cards);
    }

    private static TraceCapture attachTrace(final Game game) throws IOException {
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260814L);
        MyRandom.setRandom(auditRandom);
        final Path directory = Files.createTempDirectory("frl02l2a-success-trace-");
        try {
            return new TraceCapture(DeterminismTrace.attach(game, 0, auditRandom, directory), directory,
                    previousRandom);
        } catch (final IOException | RuntimeException ex) {
            MyRandom.setRandom(previousRandom);
            deleteTree(directory);
            throw ex;
        }
    }

    private static void deleteTree(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record Fixture(Game game, Player chooser, List<Card> cards) {
    }

    private record DiagnosticSnapshot(long publicSymmetryConflicts,
            long teacherEligibilityNotApplicable, long teacherEligibilityBcEligible,
            long teacherEligibilityBcExcludedPublicSymmetry) {
    }

    private static final class TraceCapture implements AutoCloseable {
        private final DeterminismTrace trace;
        private final Path directory;
        private final Random previousRandom;
        private boolean finished;

        private TraceCapture(final DeterminismTrace trace, final Path directory, final Random previousRandom) {
            this.trace = trace;
            this.directory = directory;
            this.previousRandom = previousRandom;
        }

        private DeterminismTrace trace() {
            return trace;
        }

        private List<String> finishAndReadDecisionTrace() throws IOException {
            if (!finished) {
                trace.finish();
                finished = true;
            }
            final Path decisionTrace = directory.resolve("game-001.decision.trace");
            return Files.exists(decisionTrace)
                    ? Files.readAllLines(decisionTrace, StandardCharsets.UTF_8) : List.of();
        }

        @Override
        public void close() throws Exception {
            try {
                if (!finished) {
                    trace.finish();
                    finished = true;
                }
            } finally {
                try {
                    deleteTree(directory);
                } finally {
                    MyRandom.setRandom(previousRandom);
                }
            }
        }
    }
}

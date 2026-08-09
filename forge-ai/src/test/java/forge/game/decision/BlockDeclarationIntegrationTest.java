package forge.game.decision;

import forge.card.CardType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.ai.LobbyPlayerAi;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class BlockDeclarationIntegrationTest {

    @BeforeClass
    public void initializeForgeLocalization() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir"));
        final Path repositoryRoot = workingDirectory.resolve("forge-gui").toFile().isDirectory()
                ? workingDirectory
                : workingDirectory.getParent();
        final Path languageDirectory = repositoryRoot.resolve("forge-gui").resolve("res")
                .resolve("languages");
        Localizer.getInstance().initialize("en-US", languageDirectory.toString());
        Lang.createInstance("en-US");
    }

    @Test
    public void oneBlockerOneAttackerUsesTwoStagesWithoutPartialCombatMutation() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();

        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.READY, start.getStatus());

        BlockDeclarationDecisionProvider.Generation next = provider.generateNext(start.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.DECISION, next.getStatus());
        AssertJUnit.assertEquals(2, next.getRequest().getCandidates().size());
        AssertJUnit.assertEquals(DecisionType.BLOCK, next.getRequest().getDecisionType());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());

        final LegalCandidate blockerCandidate = next.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.CHOOSE_BLOCKER)
                .findFirst().orElseThrow();
        next = provider.apply(next.getRequest(), blockerCandidate);
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.DECISION, next.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationStage.CHOOSE_ATTACKER_FOR_BLOCKER,
                next.getRequest().getBlockContext().getBlockStage());
        AssertJUnit.assertEquals(1, next.getRequest().getCandidates().size());
        AssertJUnit.assertTrue(next.getRequest().isForced());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());
        AssertJUnit.assertFalse(next.getRequest().getCandidates().stream()
                .anyMatch(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.DONE));

        next = provider.apply(next.getRequest(), next.getRequest().getCandidates().get(0));
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.DECISION, next.getStatus());
        AssertJUnit.assertEquals(1, next.getRequest().getCandidates().size());
        AssertJUnit.assertEquals(BlockDeclarationCandidateKind.DONE,
                next.getRequest().getCandidates().get(0).getBlockKind());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());

        next = provider.apply(next.getRequest(), next.getRequest().getCandidates().get(0));
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.COMPLETE, next.getStatus());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());

        final BlockDeclarationDecisionProvider.ApplyResult applied = provider.applyCompletedToCombat(next.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.COMPLETE, applied.getStatus());
        AssertJUnit.assertEquals(1, state.combat.getBlockers(state.attacker).size());
        AssertJUnit.assertSame(state.blocker, state.combat.getBlockers(state.attacker).get(0));
    }

    @Test
    public void diagnosticReplayMapsManyBlockersToOneAttackerWithoutMutatingCombat() {
        final TestState state = createState();
        final Card secondBlocker = creature(1003, "Second Blocker", state.combat.getAttackingPlayer().getGame(),
                state.defender);
        state.defender.getZone(ZoneType.Battlefield).add(secondBlocker);

        final BlockDeclarationAdapter adapter = new BlockDeclarationAdapter();
        final BlockDeclarationAdapter.Capture capture = adapter.begin(state.defender, state.defender, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationAdapter.Status.SUPPORTED, capture.getStatus());

        state.combat.addBlocker(state.attacker, state.blocker);
        state.combat.addBlocker(state.attacker, secondBlocker);
        final int liveAssignmentsBeforeReplay = state.combat.getAllBlockers().size();

        final BlockDeclarationAdapter.Replay replay = adapter.replay(capture, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationAdapter.ReplayStatus.COMPLETE, replay.getStatus());
        AssertJUnit.assertEquals(2, replay.getCompletedAssignments().size());
        AssertJUnit.assertEquals(liveAssignmentsBeforeReplay, state.combat.getAllBlockers().size());
    }

    @Test
    public void twoIndependentBlockersCanBeCommittedSequentially() {
        final TestState state = createState();
        final Card secondBlocker = creature(1003, "Second Blocker", state.combat.getAttackingPlayer().getGame(),
                state.defender);
        state.defender.getZone(ZoneType.Battlefield).add(secondBlocker);
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());

        for (int assignment = 0; assignment < 2; assignment++) {
            final LegalCandidate blocker = generation.getRequest().getCandidates().stream()
                    .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.CHOOSE_BLOCKER)
                    .findFirst().orElseThrow();
            generation = provider.apply(generation.getRequest(), blocker);
            AssertJUnit.assertEquals(BlockDeclarationStage.CHOOSE_ATTACKER_FOR_BLOCKER,
                    generation.getRequest().getBlockContext().getBlockStage());
            generation = provider.apply(generation.getRequest(), generation.getRequest().getCandidates().get(0));
        }
        final LegalCandidate done = generation.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.DONE)
                .findFirst().orElseThrow();
        generation = provider.apply(generation.getRequest(), done);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.COMPLETE, generation.getStatus());
        AssertJUnit.assertEquals(2, generation.getSession().getSelectedAssignments().size());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.COMPLETE,
                provider.applyCompletedToCombat(generation.getSession()).getStatus());
        AssertJUnit.assertEquals(2, state.combat.getAllBlockers().size());
    }

    @Test
    public void blockerUsedForTwoAttackersFailsDiagnosticMapping() {
        final TestState state = createState();
        final Card secondAttacker = creature(1003, "Second Attacker", state.combat.getAttackingPlayer().getGame(),
                state.combat.getAttackingPlayer());
        state.combat.getAttackingPlayer().getZone(ZoneType.Battlefield).add(secondAttacker);
        state.combat.addAttacker(secondAttacker, state.defender);

        final BlockDeclarationAdapter adapter = new BlockDeclarationAdapter();
        final BlockDeclarationAdapter.Capture capture = adapter.begin(state.defender, state.defender, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationAdapter.Status.SUPPORTED, capture.getStatus());
        state.combat.addBlocker(state.attacker, state.blocker);
        state.combat.addBlocker(secondAttacker, state.blocker);

        final BlockDeclarationAdapter.Replay replay = adapter.replay(capture, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationAdapter.ReplayStatus.MAPPING_FAILED, replay.getStatus());
        AssertJUnit.assertEquals(1, state.combat.getAllBlockers().size());
    }

    @Test
    public void blockerWithoutAnAdmittedPairIsNotExported() {
        final TestState state = createState();
        final Card tapped = creature(1003, "Tapped Blocker", state.combat.getAttackingPlayer().getGame(),
                state.defender);
        tapped.setTapped(true);
        state.defender.getZone(ZoneType.Battlefield).add(tapped);

        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        final BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.DECISION, generation.getStatus());
        AssertJUnit.assertFalse(generation.getRequest().getCandidates().stream()
                .anyMatch(candidate -> candidate.getBlockerCard() != null
                        && candidate.getBlockerCard().getCardId() == tapped.getId()));
    }

    @Test
    public void noAdmittedPairMeansNoSupportedSession() {
        final TestState state = createState();
        state.blocker.setTapped(true);

        final BlockDeclarationDecisionProvider.SessionStart start = new BlockDeclarationDecisionProvider()
                .beginSession(state.defender, state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.NO_LEGAL_BLOCK_PAIR,
                start.getReason());
    }

    @Test
    public void exactAttackerSetChangeIsStale() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        final Card secondAttacker = creature(1003, "Second Attacker", state.combat.getAttackingPlayer().getGame(),
                state.combat.getAttackingPlayer());
        state.combat.getAttackingPlayer().getZone(ZoneType.Battlefield).add(secondAttacker);
        state.combat.addAttacker(secondAttacker, state.defender);

        final BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.STALE_BLOCK_DECLARATION,
                generation.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.STALE_ATTACK_DECLARATION,
                generation.getReason());
    }

    @Test
    public void sameNameReplacementWithNewTimestampIsStale() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        state.defender.getZone(ZoneType.Battlefield).remove(state.blocker);
        final Card replacement = creature(state.blocker.getId(), state.blocker.getName(),
                state.combat.getAttackingPlayer().getGame(), state.defender);
        state.defender.getZone(ZoneType.Battlefield).add(replacement);

        final BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.STALE_BLOCK_DECLARATION,
                generation.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.STALE_BLOCK_DECLARATION,
                generation.getReason());
    }

    @Test
    public void unattackedPlaneswalkerDoesNotRejectPlayerOnlyActualTarget() {
        final TestState state = createState(true);
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.READY, start.getStatus());
    }

    @Test
    public void actuallyAttackedPlaneswalkerIsOutsideV0DefenderShape() {
        final TestState state = createState(true);
        final Card planeswalker = state.defender.getCardsIn(ZoneType.Battlefield).stream()
                .filter(Card::isPlaneswalker).findFirst().orElseThrow();
        state.combat.addAttacker(state.attacker, planeswalker);

        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_DEFENDER_SHAPE,
                start.getReason());
    }

    @Test
    public void knownMustBlockStateFailsClosedBeforeAnyRequest() {
        final TestState state = createState();
        state.blocker.addMustBlockCard(state.combat.getAttackingPlayer().getGame().getNextTimestamp(),
                state.attacker);
        AssertJUnit.assertNotNull(CombatUtil.validateBlocks(state.combat, state.defender));

        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.BLOCK_REQUIREMENT, start.getReason());
    }

    @Test
    public void requestOutstandingDoesNotAllocateAnotherStep() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        final BlockDeclarationDecisionProvider.Generation first = provider.generateNext(start.getSession());
        final int nextStep = start.getSession().getNextBlockStepIndex();

        final BlockDeclarationDecisionProvider.Generation second = provider.generateNext(start.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.STALE_BLOCK_DECLARATION,
                second.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.REQUEST_OUTSTANDING, second.getReason());
        AssertJUnit.assertEquals(nextStep, start.getSession().getNextBlockStepIndex());
        AssertJUnit.assertNotNull(first.getRequest());
    }

    @Test
    public void pendingBlockerLosingItsPairBecomesStaleWithoutEmptyAttackerRequest() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());
        final LegalCandidate blocker = generation.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.CHOOSE_BLOCKER)
                .findFirst().orElseThrow();

        generation = provider.apply(generation.getRequest(), blocker);
        state.blocker.setTapped(true);
        final BlockDeclarationDecisionProvider.Generation stale = provider.apply(generation.getRequest(),
                generation.getRequest().getCandidates().get(0));

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.STALE_BLOCK_DECLARATION,
                stale.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.STALE_BLOCK_DECLARATION,
                stale.getReason());
        AssertJUnit.assertNull(stale.getRequest());
    }

    @Test
    public void multiAttackerBlockerCapacityIsUnsupported() {
        final TestState state = createState();
        state.blocker.addCanBlockAdditional(1, state.combat.getAttackingPlayer().getGame().getNextTimestamp());

        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_MULTI_BLOCKER_ASSIGNMENT,
                start.getReason());
    }

    @Test
    public void externalDeclaringPlayerIsRejected() {
        final TestState state = createState();
        final BlockDeclarationDecisionProvider.SessionStart start =
                new BlockDeclarationDecisionProvider().beginSession(state.defender,
                        state.combat.getAttackingPlayer(), state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.EXTERNAL_DECLARER, start.getReason());
    }

    @Test
    public void blockCostRejectsWholePairDomain() {
        final TestState state = createState();
        state.attacker.addStaticAbility("Mode$ CantBlockUnless | ValidCard$ Creature | Cost$ 1");

        final BlockDeclarationDecisionProvider.SessionStart start = new BlockDeclarationDecisionProvider()
                .beginSession(state.defender, state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_BLOCK_COST,
                start.getReason());
    }

    @Test
    public void postApplyBlockCostRecheckRollsBackNeutralAssignments() {
        final TestState state = createState();
        state.attacker.getGame().getPhaseHandler().setCombat(state.combat);
        state.attacker.addStaticAbility("Mode$ CantBlockUnless | ValidCard$ Creature.blocking | Cost$ 1");
        final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(state.defender,
                state.defender, state.combat);
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.READY, start.getStatus());

        BlockDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());
        generation = provider.apply(generation.getRequest(), generation.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.CHOOSE_BLOCKER)
                .findFirst().orElseThrow());
        generation = provider.apply(generation.getRequest(), generation.getRequest().getCandidates().get(0));
        generation = provider.apply(generation.getRequest(), generation.getRequest().getCandidates().get(0));

        final BlockDeclarationDecisionProvider.ApplyResult applied = provider.applyCompletedToCombat(
                generation.getSession());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.APPLY_FAILED, applied.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_BLOCK_COST,
                applied.getReason());
        AssertJUnit.assertEquals(0, state.combat.getAllBlockers().size());
        AssertJUnit.assertEquals(0, state.combat.getAttackersBlockedBy(state.blocker).size());
    }

    @Test
    public void attackerBlockerCountRestrictionIsUnsupported() {
        final TestState state = createState();
        state.attacker.addStaticAbility("Mode$ MinMaxBlocker | ValidCard$ Card.Self | Min$ 2");

        final BlockDeclarationDecisionProvider.SessionStart start = new BlockDeclarationDecisionProvider()
                .beginSession(state.defender, state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.ATTACKER_BLOCK_COUNT_RESTRICTION,
                start.getReason());
    }

    @Test
    public void globalBlockerLimitIsUnsupported() {
        final TestState state = createState();
        state.blocker.addStaticAbility("Mode$ BlockRestrict | MaxBlockers$ 1");

        final BlockDeclarationDecisionProvider.SessionStart start = new BlockDeclarationDecisionProvider()
                .beginSession(state.defender, state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.GLOBAL_BLOCK_RESTRICTION,
                start.getReason());
    }

    @Test
    public void multiCardAttackingBandIsUnsupportedByShape() {
        final TestState state = createState();
        final Card secondAttacker = creature(1003, "Second Attacker", state.combat.getAttackingPlayer().getGame(),
                state.combat.getAttackingPlayer());
        state.combat.getAttackingPlayer().getZone(ZoneType.Battlefield).add(secondAttacker);
        state.combat.addAttacker(secondAttacker, state.defender, state.combat.getBandOfAttacker(state.attacker));

        final BlockDeclarationDecisionProvider.SessionStart start = new BlockDeclarationDecisionProvider()
                .beginSession(state.defender, state.defender, state.combat);

        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Status.UNSUPPORTED, start.getStatus());
        AssertJUnit.assertEquals(BlockDeclarationDecisionProvider.Reason.UNSUPPORTED_ATTACKING_BAND,
                start.getReason());
    }

    private static TestState createState() {
        return createState(false);
    }

    private static TestState createState(final boolean includeUnattackedPlaneswalker) {
        final GameRules rules = new GameRules(GameType.Constructed);
        final RegisteredPlayer attacking = new RegisteredPlayer(new Deck()).setPlayer(
                new LobbyPlayerAi("Attacking", Set.of()));
        final RegisteredPlayer defending = new RegisteredPlayer(new Deck()).setPlayer(
                new LobbyPlayerAi("Defending", Set.of()));
        final Match match = new Match(rules, List.of(attacking, defending), "BLOCK test");
        final Game game = new Game(List.of(attacking, defending), rules, match);
        final Player attackerPlayer = game.getPlayers().get(0);
        final Player defenderPlayer = game.getPlayers().get(1);

        final Card attacker = creature(1001, "Attacker", game, attackerPlayer);
        final Card blocker = creature(1002, "Blocker", game, defenderPlayer);
        attackerPlayer.getZone(ZoneType.Battlefield).add(attacker);
        defenderPlayer.getZone(ZoneType.Battlefield).add(blocker);
        if (includeUnattackedPlaneswalker) {
            final Card planeswalker = new Card(1004, game);
            planeswalker.setName("Unattacked Walker");
            final CardType planeswalkerType = new CardType(false);
            planeswalkerType.add("Planeswalker");
            planeswalker.setType(planeswalkerType);
            planeswalker.setOwner(defenderPlayer);
            planeswalker.setController(defenderPlayer, game.getNextTimestamp());
            defenderPlayer.getZone(ZoneType.Battlefield).add(planeswalker);
        }

        final Combat combat = new Combat(attackerPlayer);
        combat.addAttacker(attacker, defenderPlayer);
        return new TestState(combat, attackerPlayer, defenderPlayer, attacker, blocker);
    }

    private static Card creature(final int id, final String name, final Game game, final Player owner) {
        final Card card = new Card(id, game);
        card.setName(name);
        final CardType type = new CardType(false);
        type.add("Creature");
        card.setType(type);
        card.setOwner(owner);
        card.setController(owner, game.getNextTimestamp());
        return card;
    }

    private static final class TestState {
        private final Combat combat;
        private final Player attackerPlayer;
        private final Player defender;
        private final Card attacker;
        private final Card blocker;

        private TestState(final Combat combat, final Player attackerPlayer, final Player defender,
                final Card attacker, final Card blocker) {
            this.combat = combat;
            this.attackerPlayer = attackerPlayer;
            this.defender = defender;
            this.attacker = attacker;
            this.blocker = blocker;
        }
    }
}

package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class AttackDeclarationDecisionProviderTest extends AITest {
    private final AttackDeclarationDecisionProvider provider = new AttackDeclarationDecisionProvider();

    @Test
    public void ordinaryAttackOffersAttackerAndDoneWithoutMutatingCombat() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final AttackDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.READY);
        assertEquals(generation.getStatus(), AttackDeclarationDecisionProvider.Status.DECISION);
        assertEquals(generation.getRequest().getDecisionType(), DecisionType.ATTACK);
        assertEquals(generation.getRequest().getCandidates().size(), 2);
        assertFalse(generation.getRequest().isForced());
        assertTrue(generation.getRequest().getCandidates().stream()
                .anyMatch(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.DONE));
        assertEquals(combat.getAttackers().size(), 0);
        assertEquals(generation.getRequest().getAttackContext().getAttackStepIndex(), 0);
        assertNull(generation.getRequest().getAttackContext().getDecisionSequenceId());
        assertNull(generation.getRequest().getAttackContext().getActionSubdecisionIndex());
        final AttackDeclarationCard identity = generation.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER)
                .findFirst().orElseThrow().getAttackCard();
        assertEquals(identity.getCardId(), creature.getId());
        assertEquals(identity.getGameTimestamp(), creature.getGameTimestamp());
        assertEquals(identity.getControllerId(), attacker.getId());
    }

    @Test
    public void selectingAttackerLeavesCombatEmptyUntilFinalApplication() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final AttackDeclarationDecisionProvider.Generation first = provider.generateNext(start.getSession());
        final LegalCandidate add = first.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER)
                .findFirst().orElseThrow();

        final AttackDeclarationDecisionProvider.Generation second = provider.apply(first.getRequest(), add);

        assertEquals(second.getStatus(), AttackDeclarationDecisionProvider.Status.DECISION);
        assertEquals(second.getRequest().getCandidates().size(), 1);
        assertEquals(second.getRequest().getCandidates().get(0).getAttackKind(), AttackDeclarationCandidateKind.DONE);
        assertTrue(start.getSession().getSelectedAssignments().stream()
                .anyMatch(assignment -> assignment.getCard().getCardId() == creature.getId()));
        assertTrue(combat.getAttackers().isEmpty());
    }

    @Test
    public void doneCommitsTerminalSessionAndOnlyFinalApplyMutatesCombat() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final AttackDeclarationDecisionProvider.Generation first = provider.generateNext(start.getSession());
        final LegalCandidate add = first.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER)
                .findFirst().orElseThrow();
        final AttackDeclarationDecisionProvider.Generation second = provider.apply(first.getRequest(), add);
        final LegalCandidate done = second.getRequest().getCandidates().get(0);

        final AttackDeclarationDecisionProvider.Generation complete = provider.apply(second.getRequest(), done);
        final AttackDeclarationDecisionProvider.Generation afterDone = provider.generateNext(start.getSession());
        final AttackDeclarationDecisionProvider.Generation reapplied = provider.apply(second.getRequest(), done);

        assertEquals(complete.getStatus(), AttackDeclarationDecisionProvider.Status.COMPLETE);
        assertTrue(start.getSession().isCompleted());
        assertEquals(afterDone.getStatus(), AttackDeclarationDecisionProvider.Status.COMPLETE);
        assertNull(afterDone.getRequest());
        assertTrue(combat.getAttackers().isEmpty());
        assertEquals(reapplied.getStatus(), AttackDeclarationDecisionProvider.Status.STALE_ATTACK_DECLARATION);
        assertEquals(reapplied.getReason(), AttackDeclarationDecisionProvider.Reason.REQUEST_OWNERSHIP);

        final AttackDeclarationDecisionProvider.ApplyResult applied = provider.applyCompletedToCombat(start.getSession());
        assertEquals(applied.getStatus(), AttackDeclarationDecisionProvider.Status.COMPLETE);
        assertTrue(combat.isAttacking(creature));
        assertSame(combat.getDefenderByAttacker(creature), game.getPlayers().get(0));
    }

    @Test
    public void secondGenerationWithOutstandingRequestDoesNotConsumeAnotherStep() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final AttackDeclarationDecisionProvider.Generation first = provider.generateNext(start.getSession());
        final AttackDeclarationDecisionProvider.Generation duplicate = provider.generateNext(start.getSession());

        assertEquals(first.getRequest().getAttackContext().getAttackStepIndex(), 0);
        assertEquals(duplicate.getStatus(), AttackDeclarationDecisionProvider.Status.STALE_ATTACK_DECLARATION);
        assertEquals(duplicate.getReason(), AttackDeclarationDecisionProvider.Reason.REQUEST_OUTSTANDING);
        assertNull(duplicate.getRequest());
        assertEquals(start.getSession().getNextAttackStepIndex(), 1);
    }

    @Test
    public void constraintfulCombatIsRejectedBeforeGeneratingCandidates() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Juggernaut", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.ATTACK_REQUIREMENT);
        assertNull(start.getSession());
    }

    @Test
    public void attackCostRejectsWholeDomainInsteadOfDroppingAttacker() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        addCardToZone("Propaganda", game.getPlayers().get(0), ZoneType.Battlefield);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.ATTACK_COST);
    }

    @Test
    public void attackCostIntroducedAfterAdmissionMakesTheSessionStale() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        addCardToZone("Propaganda", game.getPlayers().get(0), ZoneType.Battlefield);

        final AttackDeclarationDecisionProvider.Generation generation = provider.generateNext(start.getSession());

        assertEquals(generation.getStatus(), AttackDeclarationDecisionProvider.Status.STALE_ATTACK_DECLARATION);
        assertEquals(generation.getReason(), AttackDeclarationDecisionProvider.Reason.LIVE_STATE_CHANGED);
    }

    @Test
    public void groupAttackRestrictionRejectsTheWholeSession() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Orcish Conscripts", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.GROUP_ATTACK_RESTRICTION);
    }

    @Test
    public void globalAttackMaximumRejectsTheWholeSession() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card first = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        final Card second = addCardToZone("Runeclaw Bear", attacker, ZoneType.Battlefield);
        first.setSickness(false);
        second.setSickness(false);
        addCardToZone("Dueling Grounds", game.getPlayers().get(0), ZoneType.Battlefield);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.GLOBAL_ATTACK_RESTRICTION);
    }

    @Test
    public void optionalExertRejectsTheWholeSession() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Glorybringer", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.EXERT);
    }

    @Test
    public void bandingRejectsTheWholeSession() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Benalish Hero", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.BANDING);
    }

    @Test
    public void leavingAndReturningWithSameIdButNewTimestampIsStale() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card source = addCardToZone("Izzet Charm", attacker, ZoneType.Hand);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final DecisionRequest request = provider.generateNext(start.getSession()).getRequest();
        final long originalTimestamp = creature.getGameTimestamp();
        final int originalId = creature.getId();

        final Card graveyard = game.getAction().moveToGraveyard(creature,
                source.getSpellAbilities().stream().findFirst().orElseThrow());
        final Card returned = game.getAction().moveTo(ZoneType.Battlefield, graveyard,
                source.getSpellAbilities().stream().findFirst().orElseThrow(), AbilityKey.newMap());

        assertEquals(returned.getId(), originalId);
        assertTrue(returned.getGameTimestamp() != originalTimestamp);
        final AttackDeclarationDecisionProvider.Generation stale = provider.apply(request,
                request.getCandidates().stream()
                        .filter(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER)
                        .findFirst().orElseThrow());
        assertEquals(stale.getStatus(), AttackDeclarationDecisionProvider.Status.STALE_ATTACK_DECLARATION);
        assertEquals(stale.getReason(), AttackDeclarationDecisionProvider.Reason.LIVE_STATE_CHANGED);
    }

    @Test
    public void sameNamedReplacementIsNeverSubstituted() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card source = addCardToZone("Izzet Charm", attacker, ZoneType.Hand);
        final Card original = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        original.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker, attacker, combat);
        final DecisionRequest request = provider.generateNext(start.getSession()).getRequest();

        game.getAction().moveToGraveyard(original, source.getSpellAbilities().stream().findFirst().orElseThrow());
        final Card replacement = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        replacement.setSickness(false);
        final AttackDeclarationDecisionProvider.Generation stale = provider.apply(request,
                request.getCandidates().stream()
                        .filter(candidate -> candidate.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER)
                        .findFirst().orElseThrow());

        assertTrue(replacement.getId() != original.getId());
        assertEquals(stale.getStatus(), AttackDeclarationDecisionProvider.Status.STALE_ATTACK_DECLARATION);
        assertTrue(combat.getAttackers().isEmpty());
    }

    @Test
    public void externalDeclarerIsRejectedWithoutARequest() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player externalDeclarer = game.getPlayers().get(0);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attacker,
                externalDeclarer, combat);

        assertEquals(start.getStatus(), AttackDeclarationDecisionProvider.Status.UNSUPPORTED);
        assertEquals(start.getReason(), AttackDeclarationDecisionProvider.Reason.EXTERNAL_DECLARER);
        assertNull(start.getSession());
    }
}

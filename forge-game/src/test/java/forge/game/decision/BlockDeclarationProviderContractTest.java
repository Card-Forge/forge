package forge.game.decision;

import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BlockDeclarationProviderContractTest {

    @Test
    public void providerExposesTheApprovedBlockLifecycleStatuses() throws Exception {
        final Class<?> providerType = Class.forName("forge.game.decision.BlockDeclarationDecisionProvider");
        final Class<?> statusType = Class.forName(providerType.getName() + "$Status");
        final String[] statusNames = {"READY", "DECISION", "COMPLETE", "UNSUPPORTED", "STALE_BLOCK_DECLARATION",
                "APPLY_FAILED"};
        for (final String statusName : statusNames) {
            AssertJUnit.assertTrue(statusName, Arrays.asList((Object[]) statusType.getMethod("values").invoke(null))
                    .stream().anyMatch(value -> ((Enum<?>) value).name().equals(statusName)));
        }
    }

    @Test
    public void blockCandidatesHaveDistinctSelectionStages() {
        final BlockDeclarationCard blocker = new BlockDeclarationCard(11, 101L, "Blocker", null, 1);
        final BlockDeclarationCard attacker = new BlockDeclarationCard(22, 202L, "Attacker", null, 2);

        final LegalCandidate chooseBlocker = LegalCandidate.chooseBlocker(0, blocker);
        final LegalCandidate chooseAttacker = LegalCandidate.chooseAttacker(0, blocker, attacker);
        final LegalCandidate done = LegalCandidate.blockDone(0);

        AssertJUnit.assertEquals(BlockDeclarationCandidateKind.CHOOSE_BLOCKER, chooseBlocker.getBlockKind());
        AssertJUnit.assertEquals(BlockDeclarationCandidateKind.CHOOSE_ATTACKER_FOR_BLOCKER,
                chooseAttacker.getBlockKind());
        AssertJUnit.assertEquals(BlockDeclarationCandidateKind.DONE, done.getBlockKind());
        AssertJUnit.assertEquals(attacker, chooseAttacker.getBlockAttackerCard());
    }
}

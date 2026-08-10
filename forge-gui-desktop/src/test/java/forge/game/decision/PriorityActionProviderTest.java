package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PriorityActionProviderTest extends AITest {

    private final PriorityActionProvider provider = new PriorityActionProvider();

    @Test
    public void passIsTheOnlyCandidateWhenNoTopLevelActionIsAvailable() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);

        final DecisionRequest request = NeutralityAssertions.assertGameAndRngNeutral(
                "PRIORITY_ACTION generation", game, () -> provider.createPriorityRequest(player));

        assertEquals(request.getDecisionType(), DecisionType.PRIORITY_ACTION);
        assertEquals(request.getCandidates().size(), 1);
        assertEquals(request.getCandidates().get(0).getKind(), PriorityActionKind.PASS);
        assertTrue(request.isForced());
    }

    @Test
    public void castableSpellAppearsAlongsideStrategicPass() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Mountain", player);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.PASS, null));
        assertTrue(hasCandidate(request, PriorityActionKind.CAST_SPELL, bolt.getName()));
        assertFalse(request.isForced());
    }

    @Test
    public void insufficientManaDoesNotExposeCastCandidate() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertFalse(hasCandidate(request, PriorityActionKind.CAST_SPELL, bolt.getName()));
        assertEquals(request.getCandidates().size(), 1);
    }

    @Test
    public void diagnosticGenerationRetainsAnUnpayableAssessment() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        addCardToZone("Lightning Bolt", player, ZoneType.Hand);

        final PriorityActionProvider.Generation generation = provider.generatePriorityRequest(player);

        assertEquals(generation.getFeasibilityMeasurements().size(), 1);
        assertEquals(generation.getFeasibilityMeasurements().get(0).getResult(),
                PriorityCostFeasibility.Result.UNPAYABLE);
    }

    @Test
    public void supportedXCostIsExcludedWhenItCannotBePaid() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card fireball = addCardToZone("Fireball", player, ZoneType.Hand);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertFalse(hasCandidate(request, PriorityActionKind.CAST_SPELL, fireball.getName()));
    }

    @Test
    public void jumpstartAlternativeCostIsExposedAsItsOwnLegalCast() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card insight = addCardToZone("Chemister's Insight", player, ZoneType.Graveyard);
        addCardToZone("Runeclaw Bear", player, ZoneType.Hand);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Island", player);
        final SpellAbility base = insight.getSpellAbilities().get(0);
        base.setActivatingPlayer(player);
        final SpellAbility jumpstart = GameActionUtil.addOptionalCosts(base,
                List.of(GameActionUtil.getOptionalCostValues(base).get(0)));

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(provider.contains(request, jumpstart));
    }

    @Test
    public void publicExileCardWithForgeMayPlayPermissionIsExposed() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card nightveil = addCard("Nightveil Specter", player);
        final Card boilerworks = addCardToZone("Izzet Boilerworks", opponent, ZoneType.Exile);
        boilerworks.setMayPlay(player, false, null, false, true, nightveil.getStaticAbilities().get(0));

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.PLAY_LAND, boilerworks.getName()));
    }

    @Test
    public void priorityTimeDiscardActionIsRepresentedAsSpecialAction() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card vultures = addCardToZone("Circling Vultures", player, ZoneType.Hand);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.SPECIAL_ACTION, vultures.getName()));
    }

    @Test
    public void sorcerySpeedSpellIsNotExposedAtOpponentPriority() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card bear = addCardToZone("Runeclaw Bear", player, ZoneType.Hand);
        addCard("Forest", player);
        addCard("Forest", player);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opponent);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertFalse(hasCandidate(request, PriorityActionKind.CAST_SPELL, bear.getName()));
    }

    @Test
    public void legalLandPlayAppearsOnlyAtSorceryTiming() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card forest = addCardToZone("Forest", player, ZoneType.Hand);

        assertTrue(hasCandidate(provider.createPriorityRequest(player), PriorityActionKind.PLAY_LAND, forest.getName()));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opponent);
        assertFalse(hasCandidate(provider.createPriorityRequest(player), PriorityActionKind.PLAY_LAND, forest.getName()));
    }

    @Test
    public void legalActivatedAbilityAppearsAlongsidePass() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card guildmage = addCard("Izzet Guildmage", player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Mountain", player);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.ACTIVATE_ABILITY, guildmage.getName()));
        assertTrue(hasCandidate(request, PriorityActionKind.PASS, null));
    }

    @Test
    public void activatedAbilityWithManaAndExistingAdditionalCostCheckAppears() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card rats = addCard("Sewer Rats", player);
        addCard("Swamp", player);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.ACTIVATE_ABILITY, rats.getName()));
    }

    @Test
    public void priorityManaAbilityIsRepresentedWhenItCanBeActivated() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card mountain = addCard("Mountain", player);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(hasCandidate(request, PriorityActionKind.ACTIVATE_MANA_ABILITY, mountain.getName()));
    }

    @Test
    public void candidateOrderingAndIdsAreStableWhenOneCardHasMultipleAbilities() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card guildmage = addCard("Izzet Guildmage", player);
        addCard("Island", player);
        addCard("Island", player);
        addCard("Mountain", player);
        addCard("Mountain", player);

        final List<LegalCandidate> first = provider.createPriorityRequest(player).getCandidates();
        final List<LegalCandidate> second = provider.createPriorityRequest(player).getCandidates();

        assertEquals(semanticKeys(first), semanticKeys(second));
        assertEquals(candidateIds(first), candidateIds(second));
        assertTrue(first.stream().filter(c -> c.getSourceName().equals(guildmage.getName())).count() >= 2);
    }

    @Test
    public void forgeAbilitySelectedAtPriorityMapsToTheGeneratedCandidate() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Mountain", player);
        final SpellAbility forgeChoice = bolt.getAllPossibleAbilities(player, true).get(0);

        final DecisionRequest request = provider.createPriorityRequest(player);

        assertTrue(provider.contains(request, forgeChoice));
    }

    @Test
    public void generatedRequestCanReturnTheMappedCandidateWithoutRegeneratingPriorityActions() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Mountain", player);
        final SpellAbility forgeChoice = bolt.getAllPossibleAbilities(player, true).get(0);
        final DecisionRequest request = provider.createPriorityRequest(player);

        final LegalCandidate candidate = provider.findCandidate(request, forgeChoice);

        assertEquals(candidate.getKind(), PriorityActionKind.CAST_SPELL);
        assertEquals(candidate.getSourceCardId(), bolt.getId());
    }

    @Test
    public void unrelatedOpponentHandInformationDoesNotChangeTheRequest() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCardToZone("Lightning Bolt", player, ZoneType.Hand);
        addCard("Mountain", player);

        final List<String> before = semanticKeys(provider.createPriorityRequest(player).getCandidates());
        addCardToZone("Counterspell", opponent, ZoneType.Hand);
        final List<String> after = semanticKeys(provider.createPriorityRequest(player).getCandidates());

        assertEquals(after, before);
    }

    private static boolean hasCandidate(final DecisionRequest request, final PriorityActionKind kind, final String sourceName) {
        return request.getCandidates().stream()
                .anyMatch(candidate -> candidate.getKind() == kind
                        && (sourceName == null || sourceName.equals(candidate.getSourceName())));
    }

    private static List<String> semanticKeys(final List<LegalCandidate> candidates) {
        return candidates.stream().map(LegalCandidate::getSemanticKey).toList();
    }

    private static List<Integer> candidateIds(final List<LegalCandidate> candidates) {
        return candidates.stream().map(LegalCandidate::getCandidateId).toList();
    }
}

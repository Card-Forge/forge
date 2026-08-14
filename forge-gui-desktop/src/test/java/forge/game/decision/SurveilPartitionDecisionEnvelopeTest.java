package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionDecisionEnvelopeTest extends AITest {
    @Test
    public void surveilRequestUsesCardSelectionAndTypedContext() {
        final SurveilPartitionContext context = context(11L, 0);
        final DecisionRequest request = validSurveilRequest(context);

        assertEquals(request.getDecisionType(), DecisionType.CARD_SELECTION);
        assertNull(request.getCardSelectionContext());
        assertSame(request.getSurveilPartitionContext(), context);
        assertEquals(context.getProfile(), SurveilPartitionProfile.SURVEIL_PARTITION);
        assertEquals(context.getSurveilSessionId(), 19L);
        assertEquals(context.getDecisionStepIndex(), 0);
        assertEquals(context.getChoosingPlayerId(), 7);
        assertEquals(context.getOriginalItemCount(), 2);
        assertEquals(context.getVisibleItems().size(), 2);
        assertEquals(context.getVisibleItems().get(0).getItemId(), 11L);
        assertEquals(context.getVisibleItems().get(0).getVisibleName(), "Island");
        assertEquals(context.getVisibleItems().get(1).getItemId(), 12L);
        assertEquals(context.getVisibleItems().get(1).getVisibleName(), "Forest");
        assertEquals(context.getCurrentItemId(), 11L);
        assertFalse(request.isForced());
    }

    @Test
    public void surveilCandidatesUseOnlyTheirTypedPayload() {
        final DecisionRequest request = validSurveilRequest(context(11L, 0));

        assertEquals(request.getCandidates().size(), 2);
        for (final LegalCandidate candidate : request.getCandidates()) {
            assertNotNull(candidate.getSurveilPartitionCandidateKind());
            assertNotNull(candidate.getSurveilPartitionCard());
            assertEquals(candidate.getSurveilPartitionCard().getItemId(), 11L);
            assertNull(candidate.getKind());
            assertNull(candidate.getTargetKind());
            assertNull(candidate.getPaymentKind());
            assertNull(candidate.getXValue());
            assertNull(candidate.getModeOrdinal());
            assertNull(candidate.getCardSelectionKind());
            assertNull(candidate.getCardSelectionCard());
            assertNull(candidate.getAttackKind());
            assertNull(candidate.getAttackCard());
            assertNull(candidate.getAttackDefender());
            assertNull(candidate.getBlockKind());
            assertNull(candidate.getBlockerCard());
            assertNull(candidate.getBlockAttackerCard());
            assertNull(candidate.getMulliganKind());
            assertNull(candidate.getConfirmationKind());
            assertNull(candidate.getOrderKind());
            assertNull(candidate.getOrderItem());
            assertNull(candidate.getCopySpellResolveFirstOrderKind());
            assertNull(candidate.getCopySpellResolveFirstOrderItem());
            assertNull(candidate.getSpellAbility());
            assertNull(candidate.getTarget());
            assertNull(candidate.getMana());
            assertEquals(candidate.getSourceCardId(), -1);
            assertEquals(candidate.getSourceName(), "");
            assertNull(candidate.getSourceZone());
            assertNull(candidate.getSourceState());
            assertEquals(candidate.getAbilityDescription(), "");
        }

        assertEquals(request.getCandidates().get(0).getSurveilPartitionCandidateKind(),
                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD);
        assertEquals(request.getCandidates().get(1).getSurveilPartitionCandidateKind(),
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN);
        assertEquals(request.getCandidates().get(0).getSemanticKey(),
                "SURVEIL_PARTITION|CLASSIFY_GRAVEYARD|11");
        assertEquals(request.getCandidates().get(1).getSemanticKey(),
                "SURVEIL_PARTITION|CLASSIFY_RETAIN|11");
    }

    @Test
    public void legacyCardSelectionContextRulesRemainUnchanged() {
        final DecisionRequest generated = genericCardSelectionRequest();
        final CardSelectionContext context = generated.getCardSelectionContext();

        final DecisionRequest accepted = new DecisionRequest(701L, DecisionType.CARD_SELECTION,
                generated.getCandidates(), context);
        assertSame(accepted.getCardSelectionContext(), context);
        assertNull(accepted.getSurveilPartitionContext());

        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(702L, DecisionType.MULLIGAN,
                        generated.getCandidates(), context));
    }

    @Test
    public void surveilRequestRejectsWrongProfileCandidateStepOrItem() {
        final SurveilPartitionContext wrongProfile = context(11L, 0);
        replaceField(wrongProfile, "profile", null);
        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(801L, DecisionType.CARD_SELECTION,
                        validSurveilRequest(context(11L, 0)).getCandidates(), wrongProfile));

        final SurveilPartitionContext wrongStep = context(11L, 0);
        replaceField(wrongStep, "decisionStepIndex", 2);
        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(802L, DecisionType.CARD_SELECTION,
                        validSurveilRequest(context(11L, 0)).getCandidates(), wrongStep));

        final SurveilPartitionContext context = context(11L, 0);
        final List<LegalCandidate> wrongItemCandidates = List.of(
                LegalCandidate.surveilPartition(0, SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD,
                        card(12L, "Forest")),
                LegalCandidate.surveilPartition(1, SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                        card(12L, "Forest")));
        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(803L, DecisionType.CARD_SELECTION,
                        wrongItemCandidates, context));

        final List<LegalCandidate> wrongSemanticKeyCandidates = validSurveilRequest(context(11L, 0))
                .getCandidates();
        replaceField(wrongSemanticKeyCandidates.get(1), "semanticKey", "SURVEIL_PARTITION|WRONG|11");
        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(804L, DecisionType.CARD_SELECTION,
                        wrongSemanticKeyCandidates, context(11L, 0)));
    }

    @Test
    public void genericCardSelectionDoesNotAcceptSurveilNativeFields() {
        final DecisionRequest generated = genericCardSelectionRequest();
        final LegalCandidate surveilCandidate = LegalCandidate.surveilPartition(0,
                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD, card(11L, "Island"));

        assertNull(generated.getSurveilPartitionContext());
        expectThrows(IllegalArgumentException.class,
                () -> new DecisionRequest(901L, DecisionType.CARD_SELECTION,
                        List.of(surveilCandidate), generated.getCardSelectionContext()));
    }

    private static DecisionRequest validSurveilRequest(final SurveilPartitionContext context) {
        final SurveilPartitionCard item = context.getVisibleItems().stream()
                .filter(candidate -> candidate.getItemId() == context.getCurrentItemId())
                .findFirst().orElseThrow();
        return new DecisionRequest(17L, DecisionType.CARD_SELECTION,
                List.of(
                        LegalCandidate.surveilPartition(0,
                                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD, item),
                        LegalCandidate.surveilPartition(1,
                                SurveilPartitionCandidateKind.CLASSIFY_RETAIN, item)),
                context);
    }

    private DecisionRequest genericCardSelectionRequest() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility source = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        source.setActivatingPlayer(chooser);
        final Card card = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(card);
        final CardSelectionDecisionProvider provider = new CardSelectionDecisionProvider();
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(
                chooser, chooser, source, valid, 1, 1, valid);
        assertEquals(start.getStatus(), CardSelectionDecisionProvider.Status.READY);
        final CardSelectionDecisionProvider.Generation generation = provider.generateNext(
                start.getSession(), null);
        assertEquals(generation.getStatus(), CardSelectionDecisionProvider.Status.DECISION);
        assertTrue(generation.getRequest().getCardSelectionContext() != null);
        return generation.getRequest();
    }

    private static SurveilPartitionContext context(final long currentItemId, final int stepIndex) {
        return new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION, 19L,
                stepIndex, 7, 2, List.of(card(11L, "Island"), card(12L, "Forest")), currentItemId);
    }

    private static SurveilPartitionCard card(final long itemId, final String visibleName) {
        return new SurveilPartitionCard(itemId, visibleName);
    }

    private static SpellAbility spell(final Card card) {
        return card.getSpellAbilities().stream().filter(SpellAbility::isSpell).findFirst().orElseThrow();
    }

    private static void replaceField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}

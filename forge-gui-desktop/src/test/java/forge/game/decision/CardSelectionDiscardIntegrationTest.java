package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CardSelectionDiscardIntegrationTest extends AITest {
    private final DiscardCardSelectionAdapter adapter = new DiscardCardSelectionAdapter();

    @Test
    public void realIzzetDiscardReplaysExactTwoWithoutChangingForgeStateOrResult() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        final SpellAbility discard = izzet.getAdditionalAbilityList("Choices").get(2).getSubAbility();
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final Card third = addCardToZone("Forest", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(List.of(first, second, third));
        final CardCollection aiResult = new CardCollection(List.of(second, first));
        final int originalHandSize = chooser.getCardsIn(ZoneType.Hand).size();

        final DiscardCardSelectionAdapter.Capture capture = adapter.begin(chooser, chooser, discard,
                valid, 2, 2, valid);
        final DiscardCardSelectionAdapter.Replay replay = adapter.replay(capture, aiResult);

        assertEquals(capture.getStatus(), DiscardCardSelectionAdapter.Status.SUPPORTED);
        assertEquals(replay.getStatus(), DiscardCardSelectionAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 2);
        assertEquals(replay.getSteps().get(0).getRequest().getCandidates().size(), 3);
        assertEquals(replay.getSteps().get(1).getRequest().getCandidates().size(), 2);
        assertEquals(replay.getCompletedCards().size(), 2);
        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), originalHandSize);
        assertSame(aiResult.get(0), second);
        assertSame(aiResult.get(1), first);
        assertNull(replay.getSteps().get(0).getRequest().getCardSelectionContext().getDecisionSequenceId());
        assertNull(replay.getSteps().get(0).getRequest().getCardSelectionContext().getActionSubdecisionIndex());
        assertEquals(replay.getSteps().get(0).getRequest().getCardSelectionContext().getSelectionSessionId(),
                replay.getSteps().get(1).getRequest().getCardSelectionContext().getSelectionSessionId());
    }

    @Test
    public void v0AdapterRejectsUnverifiedDiscardShapesWithoutAClaimedSession() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Player affected = game.getPlayers().get(0);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        final SpellAbility discard = izzet.getAdditionalAbilityList("Choices").get(2).getSubAbility();
        final Card own = addCardToZone("Island", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(own);

        discard.getMapParams().put("Mode", "YouChoose");
        assertEquals(adapter.begin(chooser, chooser, discard, valid, 1, 1, valid).getStatus(),
                DiscardCardSelectionAdapter.Status.UNSUPPORTED_MODE);
        discard.getMapParams().put("Mode", "TgtChoose");
        discard.getMapParams().put("RevealNumber", "1");
        assertEquals(adapter.begin(chooser, chooser, discard, valid, 1, 1, valid).getStatus(),
                DiscardCardSelectionAdapter.Status.UNSUPPORTED_DEPENDENCY);
        discard.getMapParams().remove("RevealNumber");
        discard.getMapParams().put("UnlessType", "Creature");
        assertEquals(adapter.begin(chooser, chooser, discard, valid, 1, 1, valid).getStatus(),
                DiscardCardSelectionAdapter.Status.UNSUPPORTED_DEPENDENCY);
        discard.getMapParams().remove("UnlessType");
        assertEquals(adapter.begin(chooser, affected, discard, valid, 1, 1, valid).getStatus(),
                DiscardCardSelectionAdapter.Status.UNSUPPORTED_CHOOSER);
    }

    @Test
    public void invalidDiagnosticReplayDoesNotChangeTheControllerCollectionOrHand() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Hand));
        izzet.setActivatingPlayer(chooser);
        final SpellAbility discard = izzet.getAdditionalAbilityList("Choices").get(2).getSubAbility();
        final Card validCard = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card unrelated = addCardToZone("Mountain", chooser, ZoneType.Hand);
        final CardCollection valid = new CardCollection(validCard);
        final DiscardCardSelectionAdapter.Capture capture = adapter.begin(chooser, chooser, discard,
                valid, 1, 1, valid);
        final CardCollection invalidResult = new CardCollection(unrelated);
        final int handSize = chooser.getCardsIn(ZoneType.Hand).size();

        final DiscardCardSelectionAdapter.Replay replay = adapter.replay(capture, invalidResult);

        assertEquals(replay.getStatus(), DiscardCardSelectionAdapter.ReplayStatus.INVALID_CONTROLLER_RESULT);
        assertEquals(replay.getReason(), "RESULT_CARD_NOT_VALID");
        assertSame(invalidResult.get(0), unrelated);
        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), handSize);
        assertTrue(chooser.getCardsIn(ZoneType.Hand).contains(validCard));
        assertTrue(chooser.getCardsIn(ZoneType.Hand).contains(unrelated));
    }

    @Test
    public void realDiscardEffectStillAppliesTheControllerCollectionThroughForge() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Graveyard));
        izzet.setActivatingPlayer(chooser);
        final SpellAbility discard = izzet.getAdditionalAbilityList("Choices").get(2).getSubAbility();
        discard.setActivatingPlayer(chooser);
        addCardToZone("Island", chooser, ZoneType.Hand);
        addCardToZone("Mountain", chooser, ZoneType.Hand);
        addCardToZone("Forest", chooser, ZoneType.Hand);
        final int handBefore = chooser.getCardsIn(ZoneType.Hand).size();
        final int graveyardBefore = chooser.getCardsIn(ZoneType.Graveyard).size();

        AbilityUtils.resolve(discard);

        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), handBefore - 2);
        assertEquals(chooser.getCardsIn(ZoneType.Graveyard).size(), graveyardBefore + 2);
    }

    @Test
    public void enabledDiagnosticMappingFailureCannotBlockTheNormalForgeDiscard() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final SpellAbility izzet = spell(addCardToZone("Izzet Charm", chooser, ZoneType.Graveyard));
        izzet.setActivatingPlayer(chooser);
        final SpellAbility discard = izzet.getAdditionalAbilityList("Choices").get(2).getSubAbility();
        discard.setActivatingPlayer(chooser);
        final Card first = addCardToZone("Island", chooser, ZoneType.Hand);
        final Card invalid = addCardToZone("Mountain", chooser, ZoneType.Hand);
        addCardToZone("Forest", chooser, ZoneType.Hand);
        final CardCollection callbackValid = new CardCollection(first);
        final PriorityActionDiagnostics.DiscardSelectionCapture capture =
                PriorityActionDiagnostics.captureDiscardSelection(chooser, chooser, discard, callbackValid,
                        1, 1, callbackValid);
        final boolean diagnosticsEnabled = !System.getProperty(PriorityActionDiagnostics.OUTPUT_PATH_PROPERTY, "")
                .isBlank();
        if (diagnosticsEnabled) {
            assertNotNull(capture);
            assertNull(PriorityActionDiagnostics.captureDiscardSelection(chooser, chooser, discard, null,
                    1, 1, callbackValid));
        }
        PriorityActionDiagnostics.recordDiscardSelection(capture, new CardCollection(invalid),
                PriorityActionDiagnostics.startNativeCallback());
        final int handBefore = chooser.getCardsIn(ZoneType.Hand).size();
        final int graveyardBefore = chooser.getCardsIn(ZoneType.Graveyard).size();

        AbilityUtils.resolve(discard);

        assertEquals(chooser.getCardsIn(ZoneType.Hand).size(), handBefore - 2);
        assertEquals(chooser.getCardsIn(ZoneType.Graveyard).size(), graveyardBefore + 2);
    }

    private static SpellAbility spell(final Card card) {
        return card.getSpellAbilities().stream().filter(SpellAbility::isSpell).findFirst().orElseThrow();
    }
}

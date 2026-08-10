package forge.ai.ability;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.decision.ForgeStateFingerprint;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/** Focused, test-only evidence for the FRL-02K trigger boundary audit. */
public class FRL02KConfirmationAuditTest extends AITest {

    private static final class CountingController extends PlayerControllerAi {
        private final boolean triggerDecision;
        private int confirmTriggerCalls;
        private int activeContinuationCalls;

        private CountingController(final Game game, final Player player, final boolean triggerDecision) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-audit", null));
            this.triggerDecision = triggerDecision;
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            confirmTriggerCalls++;
            if (hasActiveContinuation()) {
                activeContinuationCalls++;
            }
            return triggerDecision;
        }

        @Override
        public void orderAndPlaySimultaneousSa(final List<SpellAbility> activePlayerSAs) {
            for (final SpellAbility sa : activePlayerSAs) {
                sa.resolve();
            }
        }
    }

    @Test
    public void mandatoryTriggerResolvesWithoutConfirmTrigger() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final CountingController controller = installController(game, player, true);
        final int battlefieldBefore = game.getCardsIn(ZoneType.Battlefield).size();
        addCard("Bitterblossom", player);

        runUpkeepTrigger(game, player);

        AssertJUnit.assertEquals("a mandatory trigger must not call confirmTrigger", 0,
                controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("the mandatory trigger effect still resolves", battlefieldBefore + 2,
                game.getCardsIn(ZoneType.Battlefield).size());
        AssertJUnit.assertEquals("Bitterblossom's mandatory trigger resolves", 19, player.getLife());
    }

    @Test
    public void optionalNoCostTriggerMapsTrueToEffectAndFalseToNoEffect() {
        final TriggerRunResult accepted = runOptionalAngel(true);
        AssertJUnit.assertEquals("optional no-cost trigger asks exactly once", 1,
                accepted.controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("true allows the token effect", accepted.battlefieldBefore + 2,
                accepted.game.getCardsIn(ZoneType.Battlefield).size());

        final TriggerRunResult declined = runOptionalAngel(false);
        AssertJUnit.assertEquals("optional no-cost trigger asks exactly once", 1,
                declined.controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("false prevents the token effect", declined.battlefieldBefore + 1,
                declined.game.getCardsIn(ZoneType.Battlefield).size());
        AssertJUnit.assertEquals("ordinary trigger resolution must not carry an action continuation", 0,
                accepted.controller.activeContinuationCalls);
        AssertJUnit.assertEquals("ordinary trigger resolution must not carry an action continuation", 0,
                declined.controller.activeContinuationCalls);
    }

    @Test
    public void publicTriggerContextIsStableFailClosedAndNeutral() {
        final Game game = initAndCreateGame();
        final Player deciderViewer = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCard("Luminous Angel", deciderViewer);
        final Card sameDefinition = addCard("Luminous Angel", deciderViewer);
        final Card hiddenLibraryCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Library);
        final Card faceDownSource = addCard("Runeclaw Bear", opponent);
        faceDownSource.turnFaceDown(true);

        final List<String> sourceDefinitions = stableSemanticDefinitionKeys(source);
        AssertJUnit.assertEquals("same semantic card definition must keep trigger ordering and identity inputs",
                sourceDefinitions, stableSemanticDefinitionKeys(sameDefinition));
        AssertJUnit.assertFalse("opponent library identity is not public to the fixed decider viewer",
                hiddenLibraryCard.getView().canBeShownTo(deciderViewer.getView()));
        AssertJUnit.assertFalse("opponent face-down identity is not public to the fixed decider viewer",
                faceDownSource.getView().canFaceDownBeShownTo(deciderViewer.getView()));

        final String stateBefore = ForgeStateFingerprint.canonical(game);
        final Random previousRandom = MyRandom.getRandom();
        final DeterminismAuditRandom auditRandom = new DeterminismAuditRandom(20260810L);
        MyRandom.setRandom(auditRandom);
        try {
            final String sourceContext = publicCardContext(source, deciderViewer);
            final String hiddenSourceContext = publicCardContext(hiddenLibraryCard, deciderViewer);
            final String hiddenObjectContext = publicCardContext(hiddenLibraryCard, deciderViewer);
            final String faceDownContext = publicCardContext(faceDownSource, deciderViewer);
            AssertJUnit.assertTrue("public source must have a neutral encoding", sourceContext.startsWith("CARD|"));
            AssertJUnit.assertEquals("hidden source must fail closed without identity export", "UNSUPPORTED_HIDDEN",
                    hiddenSourceContext);
            AssertJUnit.assertEquals("hidden triggering object must fail closed without identity export",
                    "UNSUPPORTED_HIDDEN", hiddenObjectContext);
            AssertJUnit.assertEquals("face-down source must fail closed for v0", "UNSUPPORTED_HIDDEN",
                    faceDownContext);
            final String eventFromDecider = publicContext(source, sourceDefinitions.get(0), deciderViewer, deciderViewer);
            final String eventFromOpponent = publicContext(source, sourceDefinitions.get(0), deciderViewer, opponent);
            AssertJUnit.assertFalse("fixed-viewer public triggering players must not alias",
                    eventFromDecider.equals(eventFromOpponent));
            AssertJUnit.assertEquals("context projection must not consume gameplay RNG", 0L,
                    auditRandom.getDrawCount());
        } finally {
            MyRandom.setRandom(previousRandom);
        }
        AssertJUnit.assertEquals("context projection must not mutate Forge state", stateBefore,
                ForgeStateFingerprint.canonical(game));
    }

    private static boolean hasActiveContinuation() {
        try {
            final Field field = Class.forName("forge.game.decision.PriorityActionDiagnostics")
                    .getDeclaredField("ACTIVE_CONTINUATION");
            field.setAccessible(true);
            final Object value = field.get(null);
            return value instanceof ThreadLocal && ((ThreadLocal<?>) value).get() != null;
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("cannot inspect ActionContinuation diagnostics", ex);
        }
    }

    private static List<String> stableSemanticDefinitionKeys(final Card card) {
        final List<String> definitions = new ArrayList<>();
        int ordinal = 0;
        for (final Trigger trigger : card.getTriggers()) {
            final List<String> parameters = new ArrayList<>();
            for (final Map.Entry<String, String> entry : trigger.getOriginalMapParams().entrySet()) {
                if (!"TriggerDescription".equals(entry.getKey())) {
                    parameters.add(entry.getKey() + "=" + entry.getValue());
                }
            }
            Collections.sort(parameters);
            // Audit-only approximation: the repository has no proven canonical rules-definition ID.
            // Printing/set identity, runtime IDs, timestamps, and object identity are deliberately
            // excluded from this semantic key and may be retained only as diagnostics/provenance.
            definitions.add(card.getName() + "|"
                    + card.getCurrentStateName() + "|ordinal=" + ordinal++
                    + "|mode=" + trigger.getMode() + "|intrinsic=" + trigger.isIntrinsic()
                    + "|static=" + trigger.isStatic() + "|params=" + parameters);
        }
        return definitions;
    }

    private static String publicCardContext(final Card card, final Player viewer) {
        if (card.isFaceDown() && !card.getView().canFaceDownBeShownTo(viewer.getView())) {
            return "UNSUPPORTED_HIDDEN";
        }
        if (card.isFaceDown() || !card.getView().canBeShownTo(viewer.getView())) {
            return "UNSUPPORTED_HIDDEN";
        }
        return "CARD|name=" + card.getName() + "|diagnosticId=" + card.getId()
                + "|diagnosticTimestamp=" + card.getGameTimestamp();
    }

    private static String publicContext(final Card card, final String definition,
            final Player deciderViewer, final Player triggeringPlayer) {
        return publicCardContext(card, deciderViewer) + "|definition=" + definition
                + "|triggeringPlayer=" + triggeringPlayer.getId();
    }

    private TriggerRunResult runOptionalAngel(final boolean decision) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final CountingController controller = installController(game, player, decision);
        final int battlefieldBefore = game.getCardsIn(ZoneType.Battlefield).size();
        addCard("Luminous Angel", player);
        runUpkeepTrigger(game, player);
        return new TriggerRunResult(game, controller, battlefieldBefore);
    }

    private CountingController installController(final Game game, final Player player, final boolean decision) {
        final CountingController controller = new CountingController(game, player, decision);
        player.dangerouslySetController(controller);
        return controller;
    }

    private void runUpkeepTrigger(final Game game, final Player player) {
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, player);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(player), false);
        AssertJUnit.assertTrue("fixture must create a simultaneous trigger entry",
                game.getStack().hasSimultaneousStackEntries());
        game.getStack().addAllTriggeredAbilitiesToStack();
    }

    private static final class TriggerRunResult {
        private final Game game;
        private final CountingController controller;
        private final int battlefieldBefore;

        private TriggerRunResult(final Game game, final CountingController controller, final int battlefieldBefore) {
            this.game = game;
            this.controller = controller;
            this.battlefieldBefore = battlefieldBefore;
        }
    }
}

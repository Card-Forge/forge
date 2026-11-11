package forge.screens.home.rogue;

import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.EffectResultContext;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.path.NodeChest;
import forge.localinstance.properties.ForgePreferences;
import forge.util.MyRandom;
import java.util.Collections;
import java.util.List;

class NodeChestHelper {
    private static final int CHEST_LOOT_CHOICE_COUNT = 2;

    private final CSubmenuRogueMap map;
    private NodeEffectResultHelper effectResultHelper;

    NodeChestHelper(CSubmenuRogueMap map) {
        this.map = map;
    }

    void setEffectResultHelper(NodeEffectResultHelper effectResultHelper) {
        this.effectResultHelper = effectResultHelper;
    }

    void handleChestNode(NodeChest chestNode, RogueRun currentRun) {
        if (currentRun == null) {
            return;
        }

        NodeFlowOutcome chestOutcome = resolveChest(currentRun, chestNode);
        if (chestOutcome == NodeFlowOutcome.COMPLETE_NODE) {
            map.completeSideNode(chestNode);
        }
    }

    NodeFlowOutcome resolveChest(RogueRun currentRun, NodeChest chestNode) {
        ChoiceRerollContext initialCtx = new ChoiceRerollContext();
        RogueEffectComposite.INSTANCE.onBeforeChestLoot(initialCtx, currentRun);
        List<ChestEffect> chestEffects = chestNode.getChestEffects();
        if (initialCtx.forcedChestChoices != null) {
            chestEffects = initialCtx.forcedChestChoices;
        } else {
            List<ChestEffect> availableEffects = ChestEffect.getAvailableEffects(currentRun, null);
            if (chestEffects.size() < CHEST_LOOT_CHOICE_COUNT
                || !availableEffects.containsAll(chestEffects)) {
                chestEffects = generateChestLootChoices(currentRun, List.of());
            }
            chestEffects = maybeOverrideChestChoices(chestEffects);
        }
        chestNode.setChestEffects(chestEffects);

        RogueTutorialHelper.showIfNotSeen(RogueTutorial.CHEST);
        ChestDialog.DialogResult result;
        boolean rerollRequested;
        do {
            ChoiceRerollContext rerollCtx = initialCtx;
            CodexHelper.recordTraitChoices(chestEffects);
            result = new ChestDialog(chestEffects, currentRun, rerollCtx).show();
            rerollRequested = result.rerollRequested();

            if (rerollRequested) {
                RogueEffectComposite.INSTANCE.onChoiceReroll(rerollCtx, currentRun);
                initialCtx = new ChoiceRerollContext();
                RogueEffectComposite.INSTANCE.onBeforeChestLoot(initialCtx, currentRun);
                chestEffects = initialCtx.forcedChestChoices != null
                    ? initialCtx.forcedChestChoices
                    : maybeOverrideChestChoices(generateChestLootChoices(currentRun, chestEffects));
                chestNode.setChestEffects(chestEffects);
            }
        } while (rerollRequested);

        ChestEffect chestEffect = result.choice();
        if (chestEffect == null) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }
        CodexHelper.recordTraitAcquired(chestEffect);

        EffectResultContext ctx = new EffectResultContext();
        if (chestEffect.getEffectType() == RogueEffect.EffectType.ONESHOT) {
            chestEffect.applyEffect(currentRun, ctx);
            if (effectResultHelper == null) {
                CodexHelper.recordAcquiredCards(currentRun, ctx.addedCards);
                return NodeFlowOutcome.COMPLETE_NODE;
            }
            NodeFlowOutcome outcome = effectResultHelper.handleEffectTrigger(chestNode, ctx, currentRun);
            if (outcome == NodeFlowOutcome.COMPLETE_NODE) {
                CodexHelper.recordAcquiredCards(currentRun, ctx.addedCards);
            }
            return outcome;
        } else {
            currentRun.addChestEffect(chestEffect);
        }
        return NodeFlowOutcome.COMPLETE_NODE;
    }

    private List<ChestEffect> generateChestLootChoices(RogueRun currentRun,
                                                       List<ChestEffect> excludedChoices) {
        List<ChestEffect> chestEffects = ChestEffect.getAvailableEffects(currentRun, null);
        if (chestEffects.size() - excludedChoices.size() >= CHEST_LOOT_CHOICE_COUNT) {
            chestEffects.removeAll(excludedChoices);
        }
        Collections.shuffle(chestEffects, MyRandom.getRandom());
        return chestEffects.subList(0, Math.min(CHEST_LOOT_CHOICE_COUNT, chestEffects.size()));
    }

    private List<ChestEffect> maybeOverrideChestChoices(List<ChestEffect> chestEffects) {
        if (!ForgePreferences.DEV_MODE) {
            return chestEffects;
        }
        return new ChestChoiceOverrideDialog(chestEffects).show();
    }
}

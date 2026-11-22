package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.EffectResultContext;
import forge.gamemodes.rogue.path.NodeChest;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.NodeSanctum;
import forge.gamemodes.rogue.path.RoguePathNode;

class NodeEffectResultHelper {

    private final CSubmenuRogueMap map;
    private final NodeBazaarHelper bazaarHelper;
    private final NodeChestHelper chestHelper;
    private final NodeSanctumHelper sanctumHelper;
    private final NodePlaneboundHelper planeboundHelper;

    NodeEffectResultHelper(CSubmenuRogueMap map, NodeBazaarHelper bazaarHelper,
                           NodeChestHelper chestHelper, NodeSanctumHelper sanctumHelper,
                           NodePlaneboundHelper planeboundHelper) {
        this.map = map;
        this.bazaarHelper = bazaarHelper;
        this.chestHelper = chestHelper;
        this.sanctumHelper = sanctumHelper;
        this.planeboundHelper = planeboundHelper;
    }

    NodeFlowOutcome handleEffectTrigger(RoguePathNode sourceNode, EffectResultContext ctx, RogueRun currentRun) {
        if (ctx.trigger == null) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }

        switch (ctx.trigger) {
            case BAZAAR:
                ctx.addedCards = bazaarHelper.runBazaarShopping(currentRun, ctx.bazaarContext);
                return NodeFlowOutcome.COMPLETE_NODE;
            case PLANEBOUND:
                return handlePlaneboundTrigger(sourceNode, ctx, currentRun);
            case CHEST:
                return handleChestTrigger(sourceNode, currentRun);
            case SANCTUM:
                sanctumHelper.resolveSanctum(currentRun, new NodeSanctum());
                map.completeSideNode(sourceNode);
                return NodeFlowOutcome.NODE_ALREADY_COMPLETED;
            case MOVE:
                return handleMoveTrigger(ctx, currentRun);
            case CARD_REMOVAL, CARD_ADDITION, CARD_REWARD, MYTHIC_CARD_REWARD:
                return EffectResultHelper.handleTrigger(ctx, currentRun)
                    ? NodeFlowOutcome.COMPLETE_NODE
                    : NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
            default:
                return NodeFlowOutcome.COMPLETE_NODE;
        }
    }

    private NodeFlowOutcome handleChestTrigger(RoguePathNode sourceNode, RogueRun currentRun) {
        NodeFlowOutcome chestOutcome = chestHelper.resolveChest(currentRun, new NodeChest());
        if (chestOutcome == NodeFlowOutcome.COMPLETE_NODE) {
            map.completeSideNode(sourceNode);
            return NodeFlowOutcome.NODE_ALREADY_COMPLETED;
        }
        return chestOutcome;
    }

    private NodeFlowOutcome handleMoveTrigger(EffectResultContext ctx, RogueRun currentRun) {
        if (currentRun.getPath() == null || ctx.moveNodeIndex < 0) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }

        if (ctx.moveNodeIndex >= currentRun.getPath().getNodeCount()) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }

        if (currentRun.getPath().getNode(ctx.moveNodeIndex) == null) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }

        int targetRow = currentRun.getPath().getNode(ctx.moveNodeIndex).getRowIndex();
        int resetFromRow = Math.max(0, targetRow - 1);
        currentRun.getPath().updateNodes(
            node -> node.getRowIndex() >= resetFromRow,
            node -> node.setCompleted(false));
        currentRun.setCurrentNodeIndex(ctx.moveNodeIndex);
        map.enterNode();
        return NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
    }

    private NodeFlowOutcome handlePlaneboundTrigger(RoguePathNode sourceNode, EffectResultContext ctx,
                                                    RogueRun currentRun) {
        if (ctx.planebound == null) {
            return NodeFlowOutcome.COMPLETE_NODE;
        }

        if (sourceNode instanceof NodeEvent eventNode) {
            eventNode.setEventPlanebound(ctx.planebound);
        }

        NodePlanebound tempNode = new NodePlanebound(ctx.planebound);
        tempNode.setRowIndex(sourceNode.getRowIndex());
        planeboundHelper.handlePlaneboundNode(tempNode, currentRun);
        return NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
    }
}

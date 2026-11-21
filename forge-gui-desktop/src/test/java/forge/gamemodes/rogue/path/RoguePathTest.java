package forge.gamemodes.rogue.path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import forge.gamemodes.rogue.effect.PathUpdateContext;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class RoguePathTest {

    @Test
    public void reachableNodesByPosition() {
        RoguePath path = createPath(3, 4);

        assertEquals(path.getReachableNodeIndices(0), List.of(3, 4));
        assertEquals(path.getReachableNodeIndices(1), List.of(4, 5));
        assertEquals(path.getReachableNodeIndices(2), List.of(5, 6));
        assertTrue(path.getReachableNodeIndices(-1).isEmpty());
        assertTrue(path.getReachableNodeIndices(path.getNodeCount()).isEmpty());

        RoguePath singleNodeRow = createPath(1, 3);
        assertEquals(singleNodeRow.getReachableNodeIndices(0), List.of(1, 2, 3));
    }

    @Test
    public void mapVisibilityFollowsProgress() {
        RoguePath path = createPath(1, 3, 2, 3, 1);
        path.getNode(0).setCompleted(true);

        assertEquals(path.getVisibleNodesInCurrentRow(1, new PathUpdateContext()), List.of(1, 2, 3));
        assertEquals(path.getVisibleNodesInCurrentRow(2, new PathUpdateContext()), List.of(4, 5));
        path.getNode(1).setCompleted(true);
        assertEquals(path.getVisibleNodesInCurrentRow(2, new PathUpdateContext()), List.of(4));

        PathUpdateContext showAll = new PathUpdateContext();
        showAll.allowAllNodesInCurrentRow = true;
        assertEquals(path.getVisibleNodesInCurrentRow(2, showAll), List.of(4, 5));

        PathUpdateContext reveal = new PathUpdateContext();
        reveal.additionalVisiblePlaneboundRows = 1;
        assertEquals(path.getVisibleNodesInFuturePlaneboundRows(1, reveal), List.of(4, 5));
        reveal.additionalVisiblePlaneboundRows = 2;
        assertEquals(path.getVisibleNodesInFuturePlaneboundRows(1, reveal), List.of(4, 5, 9));
        assertTrue(path.getVisibleNodesInFuturePlaneboundRows(1, new PathUpdateContext()).isEmpty());
    }

    @Test
    public void updatesNextPlanebounds() {
        RoguePath path = createPath(1, 3, 2, 3, 1);

        path.updateNextPlaneboundRows(0, 2, node -> node.setWrathfulCount(1));

        assertEquals(((NodePlanebound) path.getNode(0)).getWrathfulCount(), 0);
        assertEquals(((NodePlanebound) path.getNode(4)).getWrathfulCount(), 1);
        assertEquals(((NodePlanebound) path.getNode(5)).getWrathfulCount(), 1);
        assertEquals(((NodePlanebound) path.getNode(9)).getWrathfulCount(), 1);
        assertEquals(path.countPlaneboundRowsUpTo(0), 1);
        assertEquals(path.countPlaneboundRowsUpTo(1), 1);
        assertEquals(path.countPlaneboundRowsUpTo(2), 2);
        assertEquals(path.countPlaneboundRowsUpTo(4), 3);
    }

    private static RoguePath createPath(int... rowWidths) {
        List<RoguePathNode> nodes = new ArrayList<>();
        for (int row = 0; row < rowWidths.length; row++) {
            for (int column = 0; column < rowWidths[row]; column++) {
                RoguePathNode node = row % 2 == 0 ? new NodePlanebound() : new NodeBazaar();
                node.setRowIndex(row);
                node.setColumnIndex(column);
                nodes.add(node);
            }
        }
        return new RoguePath(nodes);
    }
}

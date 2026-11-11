package forge.gamemodes.rogue.path;

import forge.gamemodes.rogue.effect.PathUpdateContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents the path (sequence of nodes) in a Rogue Commander run.
 * For MVP, paths are linear (no branching).
 */
public class RoguePath {

    private List<RoguePathNode> nodes;

    public RoguePath(List<RoguePathNode> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    public RoguePathNode getNode(int index) {
        if (index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public List<RoguePathNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<RoguePathNode> nodes) {
        this.nodes = nodes;
    }

    public void replaceNodes(Predicate<RoguePathNode> filter, Supplier<? extends RoguePathNode> replacementFactory) {
        if (filter == null || replacementFactory == null) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            RoguePathNode node = nodes.get(i);
            if (!filter.test(node)) {
                continue;
            }

            RoguePathNode replacement = replacementFactory.get();
            if (replacement == null) {
                continue;
            }

            replacement.setRowIndex(node.getRowIndex());
            replacement.setColumnIndex(node.getColumnIndex());
            replacement.setCompleted(node.isCompleted());
            nodes.set(i, replacement);
        }
    }

    public void updateNodes(Predicate<RoguePathNode> filter, Consumer<RoguePathNode> updater) {
        if (filter == null || updater == null) {
            return;
        }

        for (RoguePathNode node : nodes) {
            if (filter.test(node)) {
                updater.accept(node);
            }
        }
    }

    public void updatePlanebounds(Predicate<NodePlanebound> filter, Consumer<NodePlanebound> updater) {
        if (filter == null || updater == null) {
            return;
        }

        for (RoguePathNode node : nodes) {
            if (node instanceof NodePlanebound planebound && filter.test(planebound)) {
                updater.accept(planebound);
            }
        }
    }

    public void updateNextPlaneboundRows(int startRowExclusive, int planeboundRowCount,
                                         Consumer<NodePlanebound> updater) {
        if (updater == null || planeboundRowCount <= 0) {
            return;
        }

        List<Integer> targetRows = new ArrayList<>();
        for (int row = startRowExclusive + 1; row <= getMaxRow(); row++) {
            boolean hasPlanebound = getNodesInRow(row).stream()
                .anyMatch(NodePlanebound.class::isInstance);
            if (!hasPlanebound) {
                continue;
            }

            targetRows.add(row);
            if (targetRows.size() >= planeboundRowCount) {
                break;
            }
        }

        if (targetRows.isEmpty()) {
            return;
        }

        updatePlanebounds(node -> targetRows.contains(node.getRowIndex()), updater);
    }

    // Path queries
    public boolean isComplete() {
        return nodes.stream().allMatch(RoguePathNode::isCompleted);
    }

    public int getCompletedCount() {
        return (int) nodes.stream().filter(RoguePathNode::isCompleted).count();
    }

    /**
     * Get all nodes in a specific row.
     * @param rowIndex The row index to filter by
     * @return List of nodes in the specified row
     */
    public List<RoguePathNode> getNodesInRow(int rowIndex) {
        List<RoguePathNode> rowNodes = new ArrayList<>();
        for (RoguePathNode node : nodes) {
            if (node.getRowIndex() == rowIndex) {
                rowNodes.add(node);
            }
        }
        // order nodes by columnIndes ascending
        rowNodes.sort(Comparator.comparingInt(RoguePathNode::getColumnIndex));

        return rowNodes;
    }

    /**
     * Get the maximum row index in the path.
     * @return Highest rowIndex value
     */
    public int getMaxRow() {
        int maxRow = 0;
        for (RoguePathNode node : nodes) {
            if (node.getRowIndex() > maxRow) {
                maxRow = node.getRowIndex();
            }
        }
        return maxRow;
    }

    /**
     * Calculate which nodes in the next row are reachable from the given node.
     * Reachability rules:
     * - Single node in row: connects to ALL nodes in next row
     * - Multi-node row: first/last nodes connect to first/last in next row, middle nodes connect to adjacent (±1)
     *
     * @param fromNode The node to check reachability from
     * @return List of reachable nodes in the next row
     */
    public List<RoguePathNode> getReachableNodes(RoguePathNode fromNode) {
        if (fromNode == null) {
            return new ArrayList<>();
        }

        List<RoguePathNode> currentRowNodes = getNodesInRow(fromNode.getRowIndex());
        List<RoguePathNode> nextRowNodes = getNodesInRow(fromNode.getRowIndex() + 1);
        if (currentRowNodes.isEmpty() || nextRowNodes.isEmpty()) {
            return new ArrayList<>();
        }

        if (currentRowNodes.size() == 1) {
            return nextRowNodes;
        }

        List<RoguePathNode> reachable = new ArrayList<>();
        int fromCol = fromNode.getColumnIndex();
        int minCol = currentRowNodes.get(0).getColumnIndex();
        int maxCol = currentRowNodes.get(currentRowNodes.size() - 1).getColumnIndex();

        if (fromCol == minCol) {
            addReachableFromLeftEdge(currentRowNodes, nextRowNodes, reachable);
        } else if (fromCol == maxCol) {
            addReachableFromRightEdge(currentRowNodes, nextRowNodes, reachable);
        } else {
            addReachableFromMiddle(fromCol, currentRowNodes, nextRowNodes, reachable);
        }

        return reachable;
    }

    private void addReachableFromLeftEdge(List<RoguePathNode> currentRowNodes,
                                          List<RoguePathNode> nextRowNodes,
                                          List<RoguePathNode> reachable) {
        if (nextRowNodes.size() <= currentRowNodes.size()) {
            addNodeAtColumn(nextRowNodes, 0, reachable);
            return;
        }

        addNodesAtColumns(nextRowNodes, reachable, 0, 1);
    }

    private void addReachableFromRightEdge(List<RoguePathNode> currentRowNodes,
                                           List<RoguePathNode> nextRowNodes,
                                           List<RoguePathNode> reachable) {
        if (nextRowNodes.size() <= currentRowNodes.size()) {
            reachable.add(nextRowNodes.get(nextRowNodes.size() - 1));
            return;
        }

        int lastColumn = nextRowNodes.size() - 1;
        addNodesAtColumns(nextRowNodes, reachable, lastColumn, lastColumn - 1);
    }

    private void addReachableFromMiddle(int fromCol, List<RoguePathNode> currentRowNodes,
                                        List<RoguePathNode> nextRowNodes,
                                        List<RoguePathNode> reachable) {
        if (nextRowNodes.size() == currentRowNodes.size()) {
            addNodeAtColumn(nextRowNodes, fromCol, reachable);
            return;
        }

        int indexShift = getMiddleIndexShift(currentRowNodes, nextRowNodes);
        boolean sameParity = currentRowNodes.size() % 2 == nextRowNodes.size() % 2;

        for (RoguePathNode node : nextRowNodes) {
            int toCol = node.getColumnIndex() + indexShift;
            if (toCol == fromCol || (!sameParity && toCol - fromCol == 1)) {
                reachable.add(node);
            }
        }
    }

    private int getMiddleIndexShift(List<RoguePathNode> currentRowNodes,
                                    List<RoguePathNode> nextRowNodes) {
        if (nextRowNodes.size() >= currentRowNodes.size()) {
            return 0;
        }

        double middleIndexCurrent = currentRowNodes.size() / 2.0;
        double middleIndexNext = nextRowNodes.size() / 2.0;
        return (int) Math.round(middleIndexCurrent - middleIndexNext);
    }

    private void addNodeAtColumn(List<RoguePathNode> rowNodes, int column,
                                 List<RoguePathNode> target) {
        rowNodes.stream()
            .filter(node -> node.getColumnIndex() == column)
            .findFirst()
            .ifPresent(target::add);
    }

    private void addNodesAtColumns(List<RoguePathNode> rowNodes, List<RoguePathNode> target,
                                   int firstColumn, int secondColumn) {
        for (RoguePathNode node : rowNodes) {
            int column = node.getColumnIndex();
            if (column == firstColumn || column == secondColumn) {
                target.add(node);
            }
        }
    }

    /**
     * Get indices of nodes reachable from the node at the given index.
     * This is the index-based version for UI usage.
     *
     * @param fromNodeIndex The index of the node to check reachability from
     * @return List of indices of reachable nodes
     */
    public List<Integer> getReachableNodeIndices(int fromNodeIndex) {
        if (fromNodeIndex < 0 || fromNodeIndex >= nodes.size()) {
            return new ArrayList<>();
        }

        RoguePathNode fromNode = nodes.get(fromNodeIndex);
        List<RoguePathNode> reachableNodes = getReachableNodes(fromNode);

        List<Integer> indices = new ArrayList<>();
        for (RoguePathNode node : reachableNodes) {
            int index = nodes.indexOf(node);
            if (index >= 0) {
                indices.add(index);
            }
        }

        return indices;
    }

    /**
     * Get indices of visible nodes in the current row based on last completed node in previous row.
     * At the start (first row), all nodes in current row are visible.
     * For subsequent rows, only nodes reachable from the completed node in previous row are visible.
     *
     * @param currentRow The current row index
     * @return List of indices of visible nodes in current row
     */
    public List<Integer> getVisibleNodesInCurrentRow(int currentRow, PathUpdateContext pathCtx) {
        if (pathCtx != null && pathCtx.allowAllNodesInCurrentRow) {
            return getNodeIndicesInRow(currentRow);
        }

        Integer lastCompletedInPrevRow = getCompletedNodeIndexInRow(currentRow - 1);
        if (lastCompletedInPrevRow != null) {
            return getReachableNodeIndices(lastCompletedInPrevRow);
        }

        return getNodeIndicesInRow(currentRow);
    }

    public List<Integer> getVisibleNodesInFuturePlaneboundRows(int currentRow,
                                                               PathUpdateContext pathCtx) {
        if (pathCtx == null || pathCtx.additionalVisiblePlaneboundRows <= 0) {
            return new ArrayList<>();
        }

        Set<Integer> visibleIndices = new LinkedHashSet<>();
        List<Integer> frontier = getVisibleNodesInCurrentRow(currentRow, pathCtx);
        int visiblePlaneboundRows = 0;

        for (int row = currentRow + 1; row <= getMaxRow(); row++) {
            frontier = getReachableNodeIndicesInRow(frontier, row);
            if (frontier.isEmpty()) {
                break;
            }

            List<Integer> visiblePlaneboundsInRow = getPlaneboundIndices(frontier);
            if (visiblePlaneboundsInRow.isEmpty()) {
                continue;
            }

            visibleIndices.addAll(visiblePlaneboundsInRow);
            visiblePlaneboundRows++;
            if (visiblePlaneboundRows >= pathCtx.additionalVisiblePlaneboundRows) {
                break;
            }
        }

        return new ArrayList<>(visibleIndices);
    }

    private List<Integer> getReachableNodeIndicesInRow(List<Integer> fromNodeIndices, int row) {
        Set<Integer> reachableInRow = new LinkedHashSet<>();
        for (Integer fromNodeIndex : fromNodeIndices) {
            for (Integer reachableIndex : getReachableNodeIndices(fromNodeIndex)) {
                if (nodes.get(reachableIndex).getRowIndex() == row) {
                    reachableInRow.add(reachableIndex);
                }
            }
        }
        return new ArrayList<>(reachableInRow);
    }

    private List<Integer> getPlaneboundIndices(List<Integer> nodeIndices) {
        List<Integer> planeboundIndices = new ArrayList<>();
        for (Integer nodeIndex : nodeIndices) {
            if (nodes.get(nodeIndex) instanceof NodePlanebound) {
                planeboundIndices.add(nodeIndex);
            }
        }
        return planeboundIndices;
    }

    private List<Integer> getNodeIndicesInRow(int row) {
        List<Integer> rowIndices = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getRowIndex() == row) {
                rowIndices.add(i);
            }
        }
        return rowIndices;
    }

    private Integer getCompletedNodeIndexInRow(int row) {
        if (row < 0) {
            return null;
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getRowIndex() == row && nodes.get(i).isCompleted()) {
                return i;
            }
        }
        return null;
    }

    /**
     * Count how many Planebound rows exist up to and including the given row.
     * Only counts rows that contain at least one NodePlanebound.
     * Used for calculating opponent life (life should only increase with Plane rows, not Sanctum/Bazaar).
     *
     * @param upToRow The row index to count up to (inclusive)
     * @return Number of Planebound rows from 0 to upToRow
     */
    public int countPlaneboundRowsUpTo(int upToRow) {
        int count = 0;
        for (int row = 0; row <= upToRow; row++) {
            List<RoguePathNode> rowNodes = getNodesInRow(row);
            // Check if this row contains any Planebound nodes
            boolean hasPlaneboundNode = rowNodes.stream()
                .anyMatch(NodePlanebound.class::isInstance);
            if (hasPlaneboundNode) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "Path with " + nodes.size() + " nodes (" + getCompletedCount() + " completed)";
    }
}

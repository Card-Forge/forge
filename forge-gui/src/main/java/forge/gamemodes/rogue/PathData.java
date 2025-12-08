package forge.gamemodes.rogue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the path (sequence of nodes) in a Rogue Commander run.
 * For MVP, paths are linear (no branching).
 */
public class PathData {

    private List<NodeData> nodes;

    // Constructors
    public PathData() {
        this.nodes = new ArrayList<>();
    }

    public PathData(List<NodeData> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    // Factory method for linear path generation
    public static PathData createLinearPath(NodeData... nodes) {
        return new PathData(Arrays.asList(nodes));
    }

    // Node management
    public void addNode(NodeData node) {
        nodes.add(node);
    }

    public NodeData getNode(int index) {
        if (index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public List<NodeData> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeData> nodes) {
        this.nodes = nodes;
    }

    // Path queries
    public boolean isComplete() {
        return nodes.stream().allMatch(NodeData::isCompleted);
    }

    public int getCompletedCount() {
        return (int) nodes.stream().filter(NodeData::isCompleted).count();
    }

    public NodeData getCurrentNode() {
        for (NodeData node : nodes) {
            if (!node.isCompleted()) {
                return node;
            }
        }
        return null; // All nodes completed
    }

    public int getCurrentNodeIndex() {
        for (int i = 0; i < nodes.size(); i++) {
            if (!nodes.get(i).isCompleted()) {
                return i;
            }
        }
        return -1; // All nodes completed
    }

    public boolean hasNextNode(int currentIndex) {
        return currentIndex >= 0 && currentIndex < nodes.size() - 1;
    }

    @Override
    public String toString() {
        return "Path with " + nodes.size() + " nodes (" + getCompletedCount() + " completed)";
    }
}

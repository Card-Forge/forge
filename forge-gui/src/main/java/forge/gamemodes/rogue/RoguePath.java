package forge.gamemodes.rogue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the path (sequence of nodes) in a Rogue Commander run.
 * For MVP, paths are linear (no branching).
 */
public class RoguePath {

    private List<RoguePathNode> nodes;

    // Constructors
    public RoguePath() {
        this.nodes = new ArrayList<>();
    }

    public RoguePath(List<RoguePathNode> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    // Factory method for linear path generation
    public static RoguePath createLinearPath(RoguePathNode... nodes) {
        return new RoguePath(Arrays.asList(nodes));
    }

    // Node management
    public void addNode(RoguePathNode node) {
        nodes.add(node);
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

    // Path queries
    public boolean isComplete() {
        return nodes.stream().allMatch(RoguePathNode::isCompleted);
    }

    public int getCompletedCount() {
        return (int) nodes.stream().filter(RoguePathNode::isCompleted).count();
    }

    public RoguePathNode getCurrentNode() {
        for (RoguePathNode node : nodes) {
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

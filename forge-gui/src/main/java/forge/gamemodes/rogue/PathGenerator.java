package forge.gamemodes.rogue;

import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates paths for Rogue Commander runs.
 * Creates randomized encounters from available Planebound configurations.
 */
public class PathGenerator {

    /**
     * Generate a random linear path with the specified number of nodes.
     * Planebounds are randomly selected from all available configurations.
     *
     * @param nodeCount Number of nodes in the path (typically 5)
     * @return PathData with randomized plane encounters
     */
    public static PathData generateRandomLinearPath(int nodeCount) {
        List<PlaneboundConfig> availablePlanebounds = RogueConfig.getAllPlanebounds();

        if (availablePlanebounds.isEmpty()) {
            throw new IllegalStateException("No planebounds available for path generation");
        }

        // Shuffle planebounds to randomize selection
        List<PlaneboundConfig> shuffled = new ArrayList<>(availablePlanebounds);
        Collections.shuffle(shuffled, MyRandom.getRandom());

        // Create nodes from randomly selected planebounds
        List<NodeData> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            // Use modulo to wrap around if we need more nodes than available planebounds
            PlaneboundConfig planeboundConfig = shuffled.get(i % shuffled.size());

            NodeData node = NodeData.createPlane(planeboundConfig);

            // Set row index for life scaling: Row 0 = 5 life, Row 1 = 10 life, etc.
            node.setRowIndex(i);

            nodes.add(node);
        }

        // Create linear path from nodes
        return PathData.createLinearPath(nodes.toArray(new NodeData[0]));
    }

    /**
     * Generate a linear path with specific planebounds.
     * Useful for testing or curated experiences.
     *
     * @param planeboundConfigs Specific planebounds to use
     * @return PathData with specified plane encounters
     */
    public static PathData generateLinearPath(List<PlaneboundConfig> planeboundConfigs) {
        if (planeboundConfigs == null || planeboundConfigs.isEmpty()) {
            throw new IllegalArgumentException("Must provide at least one planebound configuration");
        }

        List<NodeData> nodes = new ArrayList<>();
        for (int i = 0; i < planeboundConfigs.size(); i++) {
            PlaneboundConfig planeboundConfig = planeboundConfigs.get(i);

            NodeData node = NodeData.createPlane(planeboundConfig);

            node.setRowIndex(i);
            nodes.add(node);
        }

        return PathData.createLinearPath(nodes.toArray(new NodeData[0]));
    }
}

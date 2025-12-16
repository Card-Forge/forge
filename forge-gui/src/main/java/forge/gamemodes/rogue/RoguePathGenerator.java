package forge.gamemodes.rogue;

import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates paths for Rogue Commander runs.
 * Creates randomized encounters from available Planebound configurations.
 */
public class RoguePathGenerator {

    /**
     * Generate a random linear path with the specified number of nodes.
     * Planebounds are randomly selected from all available configurations.
     *
     * @param nodeCount Number of nodes in the path (typically 5)
     * @return PathData with randomized plane encounters
     */
    public static RoguePath generateRandomLinearPath(int nodeCount) {
        List<RoguePlanebound> availablePlanebounds = RogueConfig.loadPlanebounds();

        if (availablePlanebounds.isEmpty()) {
            throw new IllegalStateException("No planebounds available for path generation");
        }

        // Shuffle planebounds to randomize selection
        List<RoguePlanebound> shuffled = new ArrayList<>(availablePlanebounds);
        Collections.shuffle(shuffled, MyRandom.getRandom());

        // Create nodes from randomly selected planebounds
        List<RoguePathNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            // Use modulo to wrap around if we need more nodes than available planebounds
            RoguePlanebound roguePlanebound = shuffled.get(i % shuffled.size());

            // Determine node type based on position (last node is boss, others are normal)
            NodePlanebound node;
            if (i == nodeCount - 1) {
                node = NodePlanebound.createBoss(roguePlanebound);
            } else {
                node = NodePlanebound.createNormal(roguePlanebound);
            }

            // Set row index for life scaling: Row 0 = 5 life, Row 1 = 10 life, etc.
            node.setRowIndex(i);

            nodes.add(node);
        }

        // Create linear path from nodes
        return RoguePath.createLinearPath(nodes.toArray(new RoguePathNode[0]));
    }
}

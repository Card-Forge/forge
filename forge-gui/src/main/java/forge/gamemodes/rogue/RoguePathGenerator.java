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
     * Planebounds are randomly selected based on their type: normal encounters for rows 1-4,
     * boss encounter for row 5.
     *
     * @param nodeCount Number of nodes in the path (typically 5)
     * @return PathData with randomized plane encounters
     */
    public static RoguePath generateRandomLinearPath(int nodeCount) {
        List<RoguePlanebound> availablePlanebounds = RogueConfig.loadPlanebounds();

        if (availablePlanebounds.isEmpty()) {
            throw new IllegalStateException("No planebounds available for path generation");
        }

        // Split planebounds into normal, elite and boss lists
        List<RoguePlanebound> normalPlanebounds = new ArrayList<>();
        List<RoguePlanebound> elitePlanebounds = new ArrayList<>();
        List<RoguePlanebound> bossPlanebounds = new ArrayList<>();

        for (RoguePlanebound planebound : availablePlanebounds) {
            if (planebound.type() == RoguePlaneboundType.BOSS) {
                bossPlanebounds.add(planebound);
            } else if (planebound.type() == RoguePlaneboundType.ELITE) {
                elitePlanebounds.add(planebound);
            } else {
                normalPlanebounds.add(planebound);
            }
        }

        // Validate we have the required planebounds
        if (normalPlanebounds.isEmpty()) {
            throw new IllegalStateException("No normal planebounds available for path generation");
        }
        if (elitePlanebounds.isEmpty()) {
            throw new IllegalStateException("No elite planebounds available for path generation");
        }
        if (bossPlanebounds.isEmpty()) {
            throw new IllegalStateException("No boss planebounds available for path generation");
        }

        // Shuffle lists for randomization
        Collections.shuffle(normalPlanebounds, MyRandom.getRandom());
        Collections.shuffle(elitePlanebounds, MyRandom.getRandom());
        Collections.shuffle(bossPlanebounds, MyRandom.getRandom());

        // Create nodes from randomly selected planebounds
        List<RoguePathNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            RoguePlanebound roguePlanebound;

            // Middle node (row 3) is always elite
            if (i == nodeCount / 2) {
                roguePlanebound = elitePlanebounds.get(0);
            }
            // Last node (row 5) is always a boss, others are normal
            else if (i == nodeCount - 1) {
                roguePlanebound = bossPlanebounds.get(0);
            } else {
                roguePlanebound = normalPlanebounds.get(i % normalPlanebounds.size());
            }

            NodePlanebound node = new NodePlanebound(roguePlanebound);

            // Set row index for life scaling: Row 0 = 5 life, Row 1 = 10 life, etc.
            node.setRowIndex(i);

            nodes.add(node);
        }

        // Create linear path from nodes
        return RoguePath.createLinearPath(nodes.toArray(new RoguePathNode[0]));
    }
}

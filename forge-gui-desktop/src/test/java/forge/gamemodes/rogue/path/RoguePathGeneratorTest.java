package forge.gamemodes.rogue.path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.CustomRunModification;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RoguePathGeneratorTest {
    private Random previousRandom;

    @BeforeMethod
    public void seedRandom() {
        previousRandom = MyRandom.getRandom();
        MyRandom.setRandom(new Random(0));
    }

    @AfterMethod(alwaysRun = true)
    public void restoreRandom() {
        MyRandom.setRandom(previousRandom);
    }

    @Test
    public void generatedPathsHaveValidRowsAndEverySideNodeType() {
        try (MockedStatic<RogueConfig> config = Mockito.mockStatic(RogueConfig.class)) {
            config.when(RogueConfig::loadPlanebounds).thenReturn(createPlanebounds());

            for (int seed = 0; seed < 50; seed++) {
                MyRandom.setRandom(new Random(seed));
                RogueRun run = new RogueRun();
                RoguePathGenerator.generateRandomPath(run);
                RoguePath path = run.getPath();

                assertEquals(path.getMaxRow(), 10);
                for (int row = 0; row <= 10; row++) {
                    List<RoguePathNode> rowNodes = path.getNodesInRow(row);
                    int maximum = row == 10 ? 1 : row == 9 ? 3 : 4;
                    int minimum = row == 10 ? 1 : 2;
                    assertTrue(rowNodes.size() >= minimum && rowNodes.size() <= maximum,
                        "Unexpected node count at row " + row + " for seed " + seed);
                    for (int column = 0; column < rowNodes.size(); column++) {
                        RoguePathNode node = rowNodes.get(column);
                        assertEquals(node.getColumnIndex(), column);
                        assertEquals(node.getRowIndex(), row);
                        assertEquals(node instanceof NodePlanebound, row % 2 == 0);
                    }
                }

                assertHasEverySideNodeType(path);
                Set<RogueEvent> usedEvents = new HashSet<>();
                Set<ChestEffect> usedChestEffects = new HashSet<>();
                for (RoguePathNode node : path.getNodes()) {
                    if (node instanceof NodeEvent event) {
                        assertNotNull(event.getEvent());
                        assertTrue(usedEvents.add(event.getEvent()));
                    } else if (node instanceof NodeChest chest) {
                        assertEquals(chest.getChestEffects().size(), 2);
                        for (ChestEffect effect : chest.getChestEffects()) {
                            assertTrue(usedChestEffects.add(effect));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void missingTypesReplaceNodesFromTiedMostCommonTypes() {
        RoguePlanebound planebound = new RoguePlanebound("Plane", "Boss", "boss.dck", 1,
            RoguePlaneboundType.BOSS);
        NodePlanebound boss = new NodePlanebound(planebound);
        List<RoguePathNode> nodes = new ArrayList<>();
        nodes.add(boss);
        for (int i = 0; i < 10; i++) {
            RoguePathNode node = i < 5 ? new NodeBazaar() : new NodeSanctum();
            node.setRowIndex(1 + 2 * (i / 2));
            node.setColumnIndex(i % 2);
            node.setCompleted(true);
            nodes.add(node);
        }
        RoguePath path = new RoguePath(nodes);
        List<RoguePathNode> originalNodes = new ArrayList<>(nodes);

        RoguePathGenerator.ensureSideNodeTypes(path, List.of(RogueEvent.AFTER_DUSK),
            List.of(ChestEffect.CARD_CACHE, ChestEffect.CARD_CACHE_MYTHIC));

        assertHasEverySideNodeType(path);
        assertSame(path.getNode(0), boss);
        assertEquals(path.getNodes().size(), originalNodes.size());
        for (int i = 1; i < path.getNodeCount(); i++) {
            assertEquals(path.getNode(i).getRowIndex(), originalNodes.get(i).getRowIndex());
            assertEquals(path.getNode(i).getColumnIndex(), originalNodes.get(i).getColumnIndex());
            assertTrue(path.getNode(i).isCompleted());
        }
        assertEquals(path.getNodes().stream().filter(NodeEvent.class::isInstance).count(), 1L);
        assertEquals(path.getNodes().stream().filter(NodeChest.class::isInstance).count(), 1L);
        assertEquals(path.getNodes().stream().filter(NodeBazaar.class::isInstance).count(), 4L);
        assertEquals(path.getNodes().stream().filter(NodeSanctum.class::isInstance).count(), 4L);
        NodeEvent event = (NodeEvent) path.getNodes().stream()
            .filter(NodeEvent.class::isInstance).findFirst().orElseThrow();
        NodeChest chest = (NodeChest) path.getNodes().stream()
            .filter(NodeChest.class::isInstance).findFirst().orElseThrow();
        assertEquals(event.getEvent(), RogueEvent.AFTER_DUSK);
        assertEquals(chest.getChestEffects(),
            List.of(ChestEffect.CARD_CACHE, ChestEffect.CARD_CACHE_MYTHIC));
    }

    @Test
    public void completeSideNodeMixIsLeftUntouched() {
        List<RoguePathNode> nodes = new ArrayList<>(List.of(new NodeBazaar(), new NodeSanctum(),
            new NodeEvent(RogueEvent.AFTER_DUSK),
            new NodeChest(List.of(ChestEffect.CARD_CACHE, ChestEffect.CARD_CACHE_MYTHIC))));
        RoguePath path = new RoguePath(nodes);
        List<RoguePathNode> originalNodes = new ArrayList<>(nodes);

        RoguePathGenerator.ensureSideNodeTypes(path, List.of(RogueEvent.AFTER_DUSK),
            List.of(ChestEffect.CARD_CACHE, ChestEffect.CARD_CACHE_MYTHIC));

        for (int i = 0; i < nodes.size(); i++) {
            assertSame(path.getNode(i), originalNodes.get(i));
        }
    }

    @Test
    public void pathChangingEffectsStillRunAfterTheSideNodeSafeguard() {
        try (MockedStatic<RogueConfig> config = Mockito.mockStatic(RogueConfig.class)) {
            config.when(RogueConfig::loadPlanebounds).thenReturn(createPlanebounds());
            RogueRun run = new RogueRun();
            run.addCustomRunModification(CustomRunModification.RICH);

            RoguePathGenerator.generateRandomPath(run);

            assertTrue(run.getPath().getNodes().stream().filter(RoguePathNode::isSideNode)
                .allMatch(NodeBazaar.class::isInstance));
        }
    }

    private static List<RoguePlanebound> createPlanebounds() {
        List<RoguePlanebound> planebounds = new ArrayList<>();
        for (RoguePlaneboundType type : RoguePlaneboundType.values()) {
            int count = type == RoguePlaneboundType.NORMAL ? 16
                : type == RoguePlaneboundType.ELITE ? 3 : 1;
            for (int i = 0; i < count; i++) {
                String name = type.name() + i;
                planebounds.add(new RoguePlanebound(name, name, name + ".dck", 1, type));
            }
        }
        return planebounds;
    }

    private static void assertHasEverySideNodeType(RoguePath path) {
        assertTrue(path.getNodes().stream().anyMatch(NodeBazaar.class::isInstance));
        assertTrue(path.getNodes().stream().anyMatch(NodeSanctum.class::isInstance));
        assertTrue(path.getNodes().stream().anyMatch(NodeEvent.class::isInstance));
        assertTrue(path.getNodes().stream().anyMatch(NodeChest.class::isInstance));
    }
}

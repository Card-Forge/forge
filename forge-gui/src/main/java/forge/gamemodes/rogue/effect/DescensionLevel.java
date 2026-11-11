package forge.gamemodes.rogue.effect;

import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.util.MyRandom;
import java.util.*;
import java.util.function.Consumer;

/**
 * Descension levels for Rogue Commander — applies progressively harder modifiers.
 * Each level implements its own RogueEffect trigger methods.
 *
 * Active effects per descension level (cumulative):
 */
public enum DescensionLevel implements RogueEffect {

    LEVEL_1(1, "Wrathful",
        "Some Planebounds on the path become Wrathful, gaining random buffs for the match.",
        null) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            List<NodePlanebound> planeboundNodes = new ArrayList<>();
            for (RoguePathNode node : run.getPath().getNodes()) {
                if (node instanceof NodePlanebound np) {
                    planeboundNodes.add(np);
                }
            }
            if (planeboundNodes.isEmpty()) return;

            int markerCount = planeboundNodes.size() - 3;
            Random rng = MyRandom.getRandom();
            for (int i = 0; i < markerCount; i++) {
                List<NodePlanebound> eligible = new ArrayList<>();
                for (NodePlanebound np : planeboundNodes) {
                    if (np.canAddRandomWrathfulMarker()) eligible.add(np);
                }
                if (eligible.isEmpty()) break;
                NodePlanebound target = eligible.get(rng.nextInt(eligible.size()));
                target.setWrathfulCount(target.getWrathfulCount() + 1);
            }
        }
    },

    LEVEL_2(2, "Cursed",
        "Some Planebounds on the path become Cursed, gaining powerful buffs for the opponent.",
        null) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            List<NodePlanebound> planeboundNodes = new ArrayList<>();
            for (RoguePathNode node : run.getPath().getNodes()) {
                if (node instanceof NodePlanebound np) {
                    planeboundNodes.add(np);
                }
            }
            if (planeboundNodes.isEmpty()) return;

            int markerCount = planeboundNodes.size() - 5;
            Random rng = MyRandom.getRandom();
            for (int i = 0; i < markerCount; i++) {
                List<NodePlanebound> eligible = new ArrayList<>();
                for (NodePlanebound np : planeboundNodes) {
                    if (np.canAddRandomCursedMarker()) eligible.add(np);
                }
                if (eligible.isEmpty()) break;
                NodePlanebound target = eligible.get(rng.nextInt(eligible.size()));
                target.setCursedCount(target.getCursedCount() + 1);
            }
        }
    },

    LEVEL_3(3, "Wounded",
        "Start the run with a random Wound.",
        null) {
        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            gainWound(run, new EffectResultContext());
        }
    },

    LEVEL_4(4, "Elites",
        "2 random Normal Planes of the Path are replaced by Elite Planes.",
        null) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            List<RoguePathNode> nodes = run.getPath().getNodes();
            Set<String> usedNames = new HashSet<>();
            for (RoguePathNode node : nodes) {
                if (node instanceof NodePlanebound np && np.getPlaneboundType() == RoguePlaneboundType.ELITE) {
                    usedNames.add(np.getRoguePlanebound().planeboundName());
                }
            }
            List<RoguePlanebound> unusedElites = new ArrayList<>();
            for (RoguePlanebound pb : RogueConfig.loadPlanebounds()) {
                if (pb.type() == RoguePlaneboundType.ELITE && !usedNames.contains(pb.planeboundName())) {
                    unusedElites.add(pb);
                }
            }
            if (unusedElites.size() < 2) return;
            Collections.shuffle(unusedElites, MyRandom.getRandom());

            List<Integer> normalIndices = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i) instanceof NodePlanebound np && np.getPlaneboundType() == RoguePlaneboundType.NORMAL) {
                    normalIndices.add(i);
                }
            }
            if (normalIndices.size() < 2) return;
            Collections.shuffle(normalIndices, MyRandom.getRandom());

            for (int i = 0; i < 2; i++) {
                int idx = normalIndices.get(i);
                NodePlanebound orig = (NodePlanebound) nodes.get(idx);
                NodePlanebound replacement = new NodePlanebound(unusedElites.get(i));
                replacement.setRowIndex(orig.getRowIndex());
                replacement.setColumnIndex(orig.getColumnIndex());
                nodes.set(idx, replacement);
            }
        }
    },

    LEVEL_5(5, "Hardened",
        "All Planebounds have +5 starting life.",
        null) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            for (RoguePathNode node : run.getPath().getNodes()) {
                if (node instanceof NodePlanebound planebound) {
                    planebound.setStartingLifeModification(
                        planebound.getStartingLifeModification() + 5);
                }
            }
        }
    },

    LEVEL_6(6, "Crisis",
        "Healing at a Sanctum only cures all Wounds, all Bazaar offers cost 2 more Gold.",
        null) {
        @Override
        public void onBeforeSanctum(SanctumContext ctx, RogueRun run) {
            ctx.restLifeGainDisabled = true;
        }

        @Override
        public void onBeforeBazaar(BazaarContext ctx, RogueRun run) {
            ctx.priceAdjustment += 2;
        }
    },

    LEVEL_7(7, "Nemesis",
        "Bosses have +15 starting life and an additional instance of Cursed.",
        null) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            for (RoguePathNode node : run.getPath().getNodes()) {
                if (node instanceof NodePlanebound planebound
                    && planebound.getPlaneboundType() == RoguePlaneboundType.BOSS) {
                    planebound.setStartingLifeModification(
                        planebound.getStartingLifeModification() + 15);
                    planebound.setCursedCount(planebound.getCursedCount() + 1);
                }
            }
        }
    };

    public final int level;
    public final String name;
    public final String description;
    private final String effectCardReference;

    DescensionLevel(int level, String name, String description, String effectCardReference) {
        this.level = level;
        this.name = name;
        this.description = description;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getRawDescription() { return description; }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public static DescensionLevel forLevel(int level) {
        for (DescensionLevel dl : values()) {
            if (dl.level == level) return dl;
        }
        return null;
    }

    public static int getMaxLevel() {
        return values().length;
    }
}

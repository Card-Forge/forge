package forge.gamemodes.rogue.effect;

import com.google.common.collect.ImmutableList;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueCustomDeckGenerator;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.path.NodeBazaar;
import forge.gamemodes.rogue.path.NodeChest;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.Localizer;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Optional run modifiers, including generated decks and persistent Rogue effects. */
public enum CustomRunModification implements RogueEffect {
    EXPERIMENT("custom:experiment", "Experiment",
        "Choose from Rogue Commanders that are not or not yet in the official roster.", false),
    CHOSEN_ONE("custom:choose_rogue_commander", "Chosen One",
        "Choose any legendary creature as your Commander.", false) {
        @Override
        public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
                BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
            return RogueCustomDeckGenerator.generateForChosenCommander(commander);
        }
    },
    MANIA("custom:random_rogue_commander", "Mania",
        "A random legendary creature will be chosen as your Commander.", false) {
        @Override
        public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
                BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
            return RogueCustomDeckGenerator.generate();
        }
    },
    RANDOM_START("custom:random_start", "Random Start",
        "Start with an automatically created Start Deck according to synergy and color identity for the selected Commander instead of its default deck.", true) {
        @Override
        public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
                BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
            return RogueCustomDeckGenerator.generateStartingDeck(selectedDeck);
        }
    },
    RANDOM_REWARDS("custom:random_rewards", "Random Rewards",
        "Start with an automatically created Reward Pool according to synergy and color identity for the selected Commander instead of its default pool.", false) {
        @Override
        public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
                BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
            return RogueCustomDeckGenerator.generateRewardPool(selectedDeck);
        }
    },
    SEALED("custom:sealed", "Sealed",
        "Choose 25 out of 60 non-mythic cards from the Start and Reward Pool as your Start Deck. "
            + "(also works with automatically generated decks and pools)", true) {
        @Override
        public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
                BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
            int pickCount = 25;
            CardPool startingMain = selectedDeck.getStartDeck().getMain();
            List<PaperCard> candidates = startingMain.toFlatList();
            candidates.addAll(selectedDeck.getRewardPoolCards());

            List<PaperCard> commanders = selectedDeck.getStartDeck().getCommanders();
            int commanderColors = 0;
            Set<String> usedNames = new HashSet<>();
            for (PaperCard startingCommander : commanders) {
                CardRules rules = startingCommander.getRules();
                usedNames.add(rules.getNormalizedName());
                commanderColors |= rules.getColorIdentity().getColor();
            }
            candidates.removeIf(card -> card.getRules().getType().isBasicLand()
                || PaperCardPredicates.IS_MYTHIC_RARE.test(card)
                || !usedNames.add(card.getRules().getNormalizedName()));
            if (candidates.size() < pickCount) {
                return null;
            }
            Collections.shuffle(candidates, MyRandom.getRandom());

            RogueDeck sealedDeck = selectedDeck.copy();
            CardPool sealedMain = sealedDeck.getStartDeck().getMain();
            sealedMain.clear();

            EffectResultContext ctx = new EffectResultContext();
            ctx.candidateCards = candidates.subList(0, Math.min(60, candidates.size()));
            ctx.cardSelectionMinCount = pickCount;
            ctx.cardSelectionMaxCount = pickCount;
            ctx.cardSelectionSubtitle = Localizer.getInstance().getMessage("lblRogueChooseStartingDeckCards");
            ctx.buildDeckPreview = picks -> {
                Deck preview = new Deck(sealedDeck.getStartDeck());
                preview.getMain().addAllFlat(picks);
                return preview;
            };
            List<PaperCard> selected = selectCards.apply(sealedDeck, ctx);
            if (selected.size() != pickCount) {
                return null;
            }
            sealedMain.addAllFlat(selected);
            int cardCount = sealedMain.countAll();
            int landCount = sealedMain.countAll(card -> {
                CardRules rules = card.getRules();
                return rules.getType().isLand()
                    || (rules.getOtherPart() != null && rules.getOtherPart().getType().isLand());
            });
            int basicsNeeded = Math.max(0, 30 - cardCount);
            // Expected lands in a seven-card hand = 7 * lands / deck size.
            while (7 * (landCount + basicsNeeded) < 3 * (cardCount + basicsNeeded)) {
                basicsNeeded++;
            }

            List<PaperCard> basics = new ArrayList<>();
            String landEdition = RogueConfig.getBasicLandEdition(sealedDeck.getLandEdition(), commanders.get(0));
            if (RogueCustomDeckGenerator.skipBasicLands(basics, ColorSet.fromMask(commanderColors),
                landEdition, basicsNeeded)) {
                return null;
            }
            sealedMain.addAllFlat(basics);
            return sealedDeck;
        }
    },
    PATHFINDER("custom:pathfinder", "Pathfinder",
        "Ignore Paths when choosing the next Node.", true) {
        @Override
        public void onPathUpdate(PathUpdateContext ctx, RogueRun run) {
            ChestEffect.RELIC_OF_PATHFINDING.onPathUpdate(ctx, run);
        }
    },
    RICH("custom:rich", "Rich",
        "Gain 5 additional Gold after each victorious match. All Side Nodes are Bazaars.", true) {
        @Override
        public void onBeforeRewards(MatchRewardContext ctx, RogueRun run) {
            ctx.goldRewardAdjustment += 5;
        }

        @Override
        public void afterPathGeneration(RogueRun run) {
            run.getPath().replaceNodes(RoguePathNode::isSideNode, NodeBazaar::new);
        }
    },
    USURPER("custom:usurper", "Usurper",
        "Start with 2 random Chest Traits. All Planebounds gain +1 Cursed.", true) {
        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            List<ChestEffect> traits = ChestEffect.getAvailableEffects(run, EffectType.PERMANENT);
            Collections.shuffle(traits, MyRandom.getRandom());
            for (int i = 0; i < Math.min(2, traits.size()); i++) {
                run.addChestEffect(traits.get(i));
                CodexHelper.recordTraitAcquired(traits.get(i));
            }
        }

        @Override
        public void afterPathGeneration(RogueRun run) {
            run.getPath().updatePlanebounds(node -> true,
                node -> node.setCursedCount(node.getCursedCount() + 1));
        }
    },
    BORROWED_HEALTH("custom:borrowed_health", "Borrowed Health",
        "Resting at a Sanctum heals all Life but costs 3 Max. Life.", true) {
        @Override
        public void onBeforeSanctum(SanctumContext ctx, RogueRun run) {
            if (!ctx.restLifeGainDisabled) {
                ctx.restFullHeal = true;
                ctx.restMaxLifeCost = 3;
            }
        }
    },
    VOID("custom:void", "Void",
        "No Planes or Planar Die are added for any battle.", true) {
        @Override
        public void onBeforeMatchSetup(MatchSetupContext ctx, RogueRun run) {
            ctx.includePlanes = false;
        }
    },
    PATH_OF_THE_BANNED("custom:path_of_the_banned", "Path of the Banned",
        "All Side Nodes are Chests offering only cards from the Commander Banlist.", true) {
        @Override
        public void afterPathGeneration(RogueRun run) {
            run.getPath().replaceNodes(RoguePathNode::isSideNode, NodeChest::new);
        }

        @Override
        public void onBeforeChestLoot(ChoiceRerollContext ctx, RogueRun run) {
            ctx.forcedChestChoices = ImmutableList.of(ChestEffect.CARD_CACHE_BAN);
            ctx.remainingRerolls = 0;
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean recordsCuratedCommanderRewards;

    CustomRunModification(String id, String displayName, String description,
            boolean recordsCuratedCommanderRewards) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.recordsCuratedCommanderRewards = recordsCuratedCommanderRewards;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return description; }

    public RogueDeck createRogueDeck(RogueDeck selectedDeck, PaperCard commander,
            BiFunction<RogueDeck, EffectResultContext, List<PaperCard>> selectCards) {
        return selectedDeck;
    }

    public boolean recordsCuratedCommanderRewards() {
        return recordsCuratedCommanderRewards;
    }

    public boolean isGeneratedCommander() {
        return this == MANIA || this == CHOSEN_ONE;
    }

    public boolean isIncludedWithGeneratedCommander() {
        return this == RANDOM_START || this == RANDOM_REWARDS;
    }

    public boolean conflictsWith(CustomRunModification other) {
        return this != other && ((isGeneratedCommander() && other.isGeneratedCommander())
            || (this == EXPERIMENT && other.isGeneratedCommander())
            || (other == EXPERIMENT && isGeneratedCommander())
            || (this == RICH && other == PATH_OF_THE_BANNED)
            || (this == PATH_OF_THE_BANNED && other == RICH));
    }

    public static CustomRunModification fromId(String id) {
        for (CustomRunModification modification : values()) {
            if (modification.id.equals(id)) return modification;
        }
        return null;
    }
}

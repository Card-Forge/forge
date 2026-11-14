package forge.gamemodes.rogue.effect;

import forge.card.CardRules;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.AetherUpgrade;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Enum defining all available Aetherworks in Rogue Commander mode.
 * Each constant implements its own RogueRunEffect trigger methods.
 */
public enum AetherEffect implements RogueEffect {

    // Base Aetherworks (requiredUpgradeLevel=0) — always visible

    ZHALFIRIN_SANCTUARY("zhalfirin_sanctuary", "Zhalfirin Sanctuary",
        "Begin each Run with +%s Max Life.",
        new EffectRankContext(
            new int[]{3, 6, 9, 12}, // Echo costs per rank (rank 1-4)
            new int[]{3, 6, 9, 12}, // Effect values: +3/+6/+9/+12 life
            3, 0),
        EffectType.PERMANENT, null) {
        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            int bonus = getEffectValueAtRank(run.getRunEffectRank(getId()));
            if (bonus > 0) run.addMaxLife(bonus);
        }

        @Override
        public boolean appliesOnRunStart(RogueRun run) {
            return run.getEndlessLevel() == 0;
        }
    },

    AETHER_EXCHANGE("aether_exchange", "Aether Exchange",
        "Gain +%s starting Gold.",
        new EffectRankContext(
            new int[]{3, 6, 9, 12}, // Echo costs per rank
            new int[]{2, 4, 6, 8}, // Effect values: +2/+4/+6/+8 gold
            3, 0),
        EffectType.PERMANENT, null) {
        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            int bonus = getEffectValueAtRank(run.getRunEffectRank(getId()));
            if (bonus > 0) run.addGold(bonus);
        }
    },

    RESTORATION_CHAMBER("restoration_chamber", "Restoration Chamber",
        "Gain %s life after each match victory.",
        new EffectRankContext(
            new int[]{2, 4, 8, 16}, // Echo costs per rank
            new int[]{2, 4, 6, 8},  // Effect values: gain 2/4/6/8 life
            3, 0),
        EffectType.PERMANENT, "Aetherwork - Restoration Chamber") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            int rank = run.getRunEffectRank(getId());
            int heal = getEffectValueAtRank(rank);
            if (heal > 0) {
                RogueEffect.addCardToCommandZone(getEffectCardReferenceForRank(rank), human);
            }
        }

        @Override
        public void onMatchWin(RogueRun run) {
            if (run.getCurrentLife() >= run.getMaxLife()) return;
            int heal = getEffectValueAtRank(run.getRunEffectRank(getId()));
            if (heal > 0) run.gainLifeUpToMax(heal);
        }
    },

    SPECTRAL_EXPANDER("spectral_expander", "Spectral Expander",
        "Gain %s free reroll(s) in Card Rewards and Bazaar.",
        new EffectRankContext(
            new int[]{2, 4, 8, 12}, // Echo costs (rank 1-3)
            new int[]{1, 2, 3, 4},  // Effect values: 1/2/3 free rerolls
            3, 0),
        EffectType.PERMANENT, null) {
        @Override
        public void onCardSelection(CardSelectionContext ctx, RogueRun run) {
            ctx.freeRerolls += getEffectValueAtRank(run.getRunEffectRank(getId()));
        }
    },

    TOLARIAN_ARCHIVE("tolarian_archive", "Tolarian Archive",
        "See +%s more mythic cards in Rewards and Bazaar.",
        new EffectRankContext(
            new int[]{3, 6, 9}, // Echo costs per rank
            new int[]{1, 2, 3},  // Effect values: +1/+2/+3 extra mythics
            2, 0),
        EffectType.PERMANENT, null) {
        @Override
        public void onCardSelection(CardSelectionContext ctx, RogueRun run) {
            ctx.extraMythics += getEffectValueAtRank(run.getRunEffectRank(getId()));
        }
    },

    CONTINUUM_ENGINE("continuum_engine", "Continuum Engine",
        "Survive defeat and revive with %s Life once.",
        new EffectRankContext(
            new int[]{10, 20, 30}, // Echo costs (rank 1-3)
            new int[]{5, 10, 15},  // Life on revival
            2, 0),
        EffectType.CONSUME, "Aetherwork - Continuum Engine") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            int rank = run.getRunEffectRank(getId());
            if (rank > 0) {
                RogueEffect.addCardToCommandZone(getEffectCardReferenceForRank(rank), human);
            }
        }

        @Override
        public int getChargesForRank(int rank) { return rank > 0 ? 1 : 0; }

        @Override
        public void onDefeat(DefeatContext ctx, RogueRun run) {
            ctx.revived = true;
            ctx.reviveLife = getEffectValueAtRank(run.getRunEffectRank(getId()));
            while (run.getRunEffectCharges(getId()) > 0) {
                run.consumeEffect(getId());
            }
        }
    },

    // Advanced Aetherworks — visible after Aether Upgrade(s)

    MEMORY_CACHE("memory_cache", "Memory Cache",
        "See 1 more card in standard Card Rewards. Keep up to %s extra cards from Card Rewards.",
        new EffectRankContext(
            new int[]{8, 12, 16}, // Echo costs (rank 1-3)
            new int[]{1, 2, 3},   // Extra picks
            2, 2),
        EffectType.PERMANENT, null) {
        @Override
        public void onCardReward(CardRewardContext ctx, RogueRun run) {
            int extraPicks = getEffectValueAtRank(run.getRunEffectRank(getId()));
            if (extraPicks > 0) {
                ctx.maxPicks += extraPicks;
                ctx.additionalNonMythicCards++;
            }
        }
    },

    BRACKET_BREAKER("bracket_breaker", "Bracket Breaker",
        "Replace %s non-mythic card(s) of Card Rewards with cards from the Gamechanger list.",
        new EffectRankContext(
            new int[]{8, 12, 16}, // Echo costs (rank 1-3)
            new int[]{1, 2, 3},   // Effect values: replace 1/2/3 non-mythics
            2, 2),
        EffectType.PERMANENT, null) {
        @Override
        public void onCardReward(CardRewardContext ctx, RogueRun run) {
            int count = getEffectValueAtRank(run.getRunEffectRank(getId()));
            if (count <= 0) {
                return;
            }

            ctx.nonMythicCardReplacementCount += count;
            ctx.nonMythicCardReplacementCandidates.addAll(run.getGamechangerCardsForActiveCommander());
        }
    },

    ZHALFIRIN_WARD("zhalfirin_ward", "Zhalfirin Ward",
        "Fellows and Items you control have hexproof and indestructible. "
            + "Start each Run by choosing %s of 20 rare legendary colorless Fellows and Items.",
        new EffectRankContext(
            new int[]{10, 15, 20}, // Echo costs (rank 1-3)
            new int[]{1, 2, 3},    // Starting Carry Cards
            2, 2),
        EffectType.PERMANENT, "Aetherwork - Zhalfirin Ward") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            if (run.getRunEffectRank(getId()) > 0) {
                RogueEffect.addCardToCommandZone(getEffectCardReference(), human);
            }
        }

        @Override
        public boolean appliesOnRunStart(RogueRun run) {
            return run.getEndlessLevel() == 0;
        }

        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            chooseStartingCarryCards(run, handleResult, card -> {
                CardRules rules = card.getRules();
                return PaperCardPredicates.IS_RARE.test(card) && rules.getColor().isColorless()
                    && rules.getType().isLegendary()
                    && (rules.getType().isCreature() || rules.getType().isArtifact());
            }, card -> card.getRules().getType().isCreature() ? CarryCardType.FELLOW : CarryCardType.ITEM);
        }
    },

    SPELLWRIGHTS_PRESS("spellwrights_press", "Spellwright's Press",
        "Scrolls you cast cannot be countered. "
            + "Start each Run by choosing %s of 20 mythic rare Scrolls.",
        new EffectRankContext(
            new int[]{10, 15, 20}, // Echo costs (rank 1-3)
            new int[]{1, 2, 3},    // Starting Scrolls
            2, 2),
        EffectType.PERMANENT, "Aetherwork - Spellwright's Press") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            if (run.getRunEffectRank(getId()) > 0) {
                RogueEffect.addCardToCommandZone(getEffectCardReference(), human);
            }
        }

        @Override
        public boolean appliesOnRunStart(RogueRun run) {
            return run.getEndlessLevel() == 0;
        }

        @Override
        public void onRunStart(RogueRun run, Consumer<EffectResultContext> handleResult) {
            chooseStartingCarryCards(run, handleResult,
                card -> PaperCardPredicates.IS_MYTHIC_RARE.test(card)
                    && RogueConfig.getBazaarCarryCardType(card) == CarryCardType.SCROLL,
                card -> CarryCardType.SCROLL);
        }
    },

    FATEBENDER("fatebender", "Fatebender",
        "Reroll NPC Boons, Chest Loot and Events up to %s time(s) during the Run.",
        new EffectRankContext(
            new int[]{6, 10, 14, 18}, // Echo costs (rank 1-4)
            new int[]{1, 2, 3, 4},    // Effect values: 1/2/3/4 reroll charges
            3, 2),
        EffectType.CONSUME, null) {
        @Override
        public int getChargesForRank(int rank) {
            return getEffectValueAtRank(rank);
        }

        @Override
        public void onBeforeNpcBoons(ChoiceRerollContext ctx, RogueRun run) {
            ctx.remainingRerolls = run.getRunEffectCharges(getId());
        }

        @Override
        public void onBeforeChestLoot(ChoiceRerollContext ctx, RogueRun run) {
            ctx.remainingRerolls = run.getRunEffectCharges(getId());
        }

        @Override
        public void onBeforeEvent(ChoiceRerollContext ctx, RogueRun run) {
            ctx.remainingRerolls = run.getRunEffectCharges(getId());
        }

        @Override
        public void onChoiceReroll(ChoiceRerollContext ctx, RogueRun run) {
            run.consumeEffect(getId());
        }
    },

    PLANEBENDER("planebender", "Planebender",
        "Reroll Planebound Nodes and their Wrathful / Cursed markers in the next row on the map up to %s time(s) during the Run.",
        new EffectRankContext(
            new int[]{6, 10, 14, 18}, // Echo costs (rank 1-4)
            new int[]{1, 2, 3, 4},    // Effect values: 1/2/3/4 reroll charges
            3, 2),
        EffectType.CONSUME, null) {
        @Override
        public int getChargesForRank(int rank) {
            return getEffectValueAtRank(rank);
        }

        @Override
        public void onPathUpdate(PathUpdateContext ctx, RogueRun run) {
            ctx.remainingPlaneboundRerolls = run.getRunEffectCharges(getId());
        }

        @Override
        public void onPathNodeReroll(PathUpdateContext ctx, RogueRun run) {
            if (run.getCurrentNode() instanceof NodePlanebound node) {
                int descension = run.getDescensionLevel();
                node.rerollMarkers(descension >= DescensionLevel.LEVEL_1.level,
                    descension >= DescensionLevel.LEVEL_2.level);
            }
            run.consumeEffect(getId());
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final EffectRankContext rankContext;
    private final EffectType effectType;
    private final String effectCardReference;
    AetherEffect(String id, String displayName, String description, EffectRankContext rankContext,
                 EffectType effectType, String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.rankContext = rankContext;
        this.effectType = effectType;
        this.effectCardReference = effectCardReference;
    }

    final void chooseStartingCarryCards(RogueRun run, Consumer<EffectResultContext> handleResult,
                                       Predicate<PaperCard> filter, Function<PaperCard, CarryCardType> typeForCard) {
        int pickCount = getEffectValueAtRank(run.getRunEffectRank(getId()));
        if (pickCount <= 0) {
            return;
        }

        List<PaperCard> candidates = new ArrayList<>(RogueConfig.getRandomCards(filter));
        candidates.removeIf(card -> !run.canAddCardAsCarryCard(card));
        Collections.shuffle(candidates, MyRandom.getRandom());

        Set<String> offeredNames = new HashSet<>();
        List<PaperCard> offers = candidates.stream()
            .filter(card -> offeredNames.add(card.getRules().getNormalizedName()))
            .limit(20)
            .collect(Collectors.toList());
        if (offers.isEmpty()) {
            return;
        }

        int requiredPicks = Math.min(pickCount, offers.size());
        EffectResultContext ctx = new EffectResultContext();
        ctx.trigger = EffectResultContext.ActionTriggerType.CARD_ADDITION;
        ctx.candidateCards = offers;
        ctx.cardSelectionMinCount = requiredPicks;
        ctx.cardSelectionMaxCount = requiredPicks;
        ctx.cardSelectionSubtitle = getDisplayName() + " - Choose " + requiredPicks
            + (requiredPicks == 1 ? " Carry Card." : " Carry Cards.");
        ctx.onCardsSelected = selected -> {
            for (PaperCard card : selected) {
                addCarryCard(run, ctx, card, typeForCard.apply(card));
            }
        };
        handleResult.accept(ctx);
    }

    @Override
    public EffectType getEffectType() {
        return effectType;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getRawDescription() { return description; }

    public String getDescriptionForRank(int rank) {
        int value = getEffectValueAtRank(rank);
        return String.format(description, value > 0 ? Integer.toString(value) : "?");
    }

    @Override
    public String getActiveDescription(RogueRun run) {
        return getDescriptionForRank(run.getRunEffectRank(getId()));
    }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    @Override
    public PaperCard getEffectCard() {
        return null;
    }

    public int getMaxRank() {
        return rankContext.maxRank();
    }

    /**
     * Whether this Aetherwork is accessible given the current Aether Upgrade level.
     */
    public boolean isAccessibleAt(int upgradeLevel) {
        return rankContext.requiredUpgradeLevel() <= upgradeLevel;
    }

    /**
     * Get the effective maximum rank, including any bonus from Aether Upgrades.
     * Aether Upgrades can add bonus ranks to all Aetherworks.
     */
    public int getEffectiveMaxRank(int upgradeLevel) {
        int bonus = 0;
        for (int l = 1; l <= upgradeLevel; l++) {
            AetherUpgrade u = AetherUpgrade.forLevel(l);
            if (u != null) bonus += u.extraEffectRanks;
        }
        return rankContext.maxRank() + bonus;
    }

    /**
     * Get the echo cost to upgrade from (rank-1) to (rank).
     * Bounds use echoCosts.length to allow the extra rank from Aether Upgrade 4.
     * @param rank The target rank (1-indexed)
     * @return The cost in echoes, or 0 if invalid rank
     */
    public int getEchoCostForRank(int rank) {
        if (rank < 1 || rank > rankContext.echoCosts().length) {
            return 0;
        }
        return rankContext.echoCosts()[rank - 1];
    }

    /**
     * Get the effect value at a specific rank.
     * Bounds use effectValues.length to allow the extra rank from Aether Upgrade 4.
     * @param rank The current rank (1-indexed)
     * @return The effect magnitude, or 0 if not unlocked
     */
    public int getEffectValueAtRank(int rank) {
        if (rank < 1 || rank > rankContext.effectValues().length) {
            return 0;
        }
        return rankContext.effectValues()[rank - 1];
    }

    /**
     * Get the description showing all rank values with the current rank highlighted.
     * Uses HTML formatting. Shows values up to the effective max rank for the given upgrade level.
     * @param currentRank The current rank (0 = not unlocked)
     * @param upgradeLevel The current Aether Upgrade level
     * @return HTML-formatted description string
     */
    @Override
    public String getDescriptionWithAllRanks(int currentRank, int upgradeLevel) {
        String allValues = buildAllValuesString(currentRank, upgradeLevel);
        return "<html>" + String.format(description, allValues) + "</html>";
    }

    /**
     * Build the all-values string showing values up to effectiveMaxRank(upgradeLevel).
     * The current rank value is highlighted with bold+underline.
     */
    private String buildAllValuesString(int currentRank, int upgradeLevel) {
        int effectiveMax = getEffectiveMaxRank(upgradeLevel);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rankContext.effectValues().length && (i + 1) <= effectiveMax; i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            int rankForThisValue = i + 1;
            if (rankForThisValue == currentRank) {
                sb.append("<b><u>").append(rankContext.effectValues()[i]).append("</u></b>");
            } else {
                sb.append(rankContext.effectValues()[i]);
            }
        }
        return sb.toString();
    }

    private record EffectRankContext(int[] echoCosts, int[] effectValues, int maxRank,
                                     int requiredUpgradeLevel) { }

    /**
     * Find an Aetherwork by its ID.
     */
    public static AetherEffect fromId(String id) {
        for (AetherEffect type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}

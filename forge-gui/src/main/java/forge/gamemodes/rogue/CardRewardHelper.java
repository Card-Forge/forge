package forge.gamemodes.rogue;

import org.apache.commons.lang3.StringUtils;

import forge.gamemodes.rogue.effect.CardRewardContext;
import forge.gamemodes.rogue.effect.CardSelectionContext;
import forge.gamemodes.rogue.effect.MatchRewardContext;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Shared card reward logic used by RogueWinLoseController (post-match rewards)
 * and CSubmenuRogueMap (chest node rewards).
 */
public class CardRewardHelper {

    public static final int REROLL_BASE_COST = 2;
    public static final int REROLL_COST_INCREMENT = 2;

    /**
     * Platform-agnostic callback for showing the card reward dialog.
     * Returns selected cards, or null if reroll was clicked.
     */
    @FunctionalInterface
    public interface RewardDialog {
        List<PaperCard> show(String title, List<PaperCard> cards, int maxSelections,
                             String rerollLabel, boolean rerollEnabled, int gold);
    }

    /**
     * Calculate reroll gold cost for a given paid reroll index (0-indexed).
     */
    public static int getRerollCost(int paidRerollIndex) {
        return REROLL_BASE_COST + paidRerollIndex * REROLL_COST_INCREMENT;
    }

    /**
     * Build the reroll button label showing gold cost, or "Reroll (free)" if free.
     */
    public static String buildRerollLabel(int freeRerolls, int rerollCount) {
        if (rerollCount < freeRerolls) {
            return "Reroll: 0";
        }
        return "Reroll: " + getRerollCost(rerollCount - freeRerolls);
    }

    /**
     * Check if the player can afford the next reroll.
     */
    public static boolean canAffordReroll(int freeRerolls, int rerollCount, int gold) {
        if (rerollCount < freeRerolls) return true;
        return gold >= getRerollCost(rerollCount - freeRerolls);
    }

    /**
     * Run a card reward selection as part of post-match rewards.
     * MatchRewardContext applies match-scoped reward adjustments before generic card-reward effects.
     */
    public static List<PaperCard> runReward(RogueRun run, RewardDialog dialog, boolean mythicOnly,
                                            MatchRewardContext matchRewardCtx,
                                            CardRewardContext cardRewardCtx) {
        RogueDeck rogueDeck = run.getSelectedRogueDeck();
        if (rogueDeck == null) return List.of();

        boolean customReward = cardRewardCtx != null;
        CardRewardContext rewardCtx = getRewardContext(cardRewardCtx, mythicOnly, matchRewardCtx);
        RogueEffectComposite.INSTANCE.onCardReward(rewardCtx, run);

        CardSelectionContext selCtx = new CardSelectionContext();
        RogueEffectComposite.INSTANCE.onCardSelection(selCtx, run);

        List<PaperCard> chosenCards = chooseRewardCards(run, rogueDeck, dialog, mythicOnly, rewardCtx, selCtx,
            customReward);
        applyChosenRewardCards(run, rogueDeck, chosenCards, customReward);

        return chosenCards;
    }

    private static CardRewardContext getRewardContext(CardRewardContext cardRewardCtx, boolean mythicOnly,
                                                      MatchRewardContext matchRewardCtx) {
        return cardRewardCtx != null ? cardRewardCtx : createDefaultRewardContext(mythicOnly, matchRewardCtx);
    }

    private static List<PaperCard> chooseRewardCards(RogueRun run, RogueDeck rogueDeck, RewardDialog dialog,
                                                     boolean mythicOnly, CardRewardContext rewardCtx,
                                                     CardSelectionContext selCtx, boolean customReward) {
        int maxPicks = rewardCtx.maxPicks;
        int freeRerolls = customReward ? 0 : selCtx.freeRerolls;
        String title = getRewardTitle(rewardCtx, mythicOnly);

        List<PaperCard> chosenCards;
        int rerollCount = 0;
        do {
            List<PaperCard> rewardOptions = buildRewardOptions(run, rogueDeck, mythicOnly, rewardCtx, selCtx,
                customReward);
            if (rewardOptions.isEmpty()) return List.of();
            CodexHelper.recordCardRewardOptions(run, rewardOptions);

            chosenCards = showRewardDialog(run, dialog, rewardOptions, maxPicks, freeRerolls, rerollCount, title,
                customReward);
            if (!customReward) {
                rogueDeck.discardRewardOptions(rewardOptions);
            }
            if (isPaidReroll(chosenCards, freeRerolls, rerollCount, customReward)) {
                run.spendGold(getRerollCost(rerollCount - freeRerolls));
            }
            if (!customReward && chosenCards == null) {
                rerollCount++;
            }
        } while (!customReward && chosenCards == null);

        return chosenCards == null ? new ArrayList<>() : chosenCards;
    }

    private static List<PaperCard> showRewardDialog(RogueRun run, RewardDialog dialog, List<PaperCard> rewardOptions,
                                                    int maxPicks, int freeRerolls, int rerollCount, String title,
                                                    boolean customReward) {
        String rerollLabel = customReward ? null : buildRerollLabel(freeRerolls, rerollCount);
        boolean rerollEnabled = !customReward && canAffordReroll(freeRerolls, rerollCount, run.getCurrentGold());
        return dialog.show(title, rewardOptions, maxPicks, rerollLabel, rerollEnabled, run.getCurrentGold());
    }

    private static boolean isPaidReroll(List<PaperCard> chosenCards, int freeRerolls, int rerollCount,
                                        boolean customReward) {
        return !customReward && chosenCards == null && rerollCount >= freeRerolls;
    }

    private static List<PaperCard> buildRewardOptions(RogueRun run, RogueDeck rogueDeck, boolean mythicOnly,
                                                      CardRewardContext rewardCtx, CardSelectionContext selCtx,
                                                      boolean customReward) {
        return customReward
            ? buildCustomRewardOptions(run, rewardCtx)
            : buildNormalRewardOptions(run, rogueDeck, mythicOnly, rewardCtx, selCtx);
    }

    private static void applyChosenRewardCards(RogueRun run, RogueDeck rogueDeck, List<PaperCard> chosenCards,
                                               boolean customReward) {
        if (chosenCards.isEmpty()) {
            return;
        }

        if (!customReward) {
            rogueDeck.removeFromCardPools(chosenCards);
        }
        run.addCardsToDeck(chosenCards, true);
        CodexHelper.recordAcquiredCards(run, chosenCards);
    }

    private static CardRewardContext createDefaultRewardContext(boolean mythicOnly,
                                                                MatchRewardContext matchRewardCtx) {
        CardRewardContext rewardCtx = new CardRewardContext(mythicOnly ? 1 : 3);
        if (matchRewardCtx != null) {
            rewardCtx.nonMythicCardCountAdjustment = matchRewardCtx.nonMythicCardCountAdjustment;
        }
        return rewardCtx;
    }

    private static String getRewardTitle(CardRewardContext rewardCtx, boolean mythicOnly) {
        if (!StringUtils.isBlank(rewardCtx.title)) {
            return rewardCtx.title;
        }
        return mythicOnly ? "Choose Your Mythic Reward" : "Choose Your Rewards";
    }

    private static List<PaperCard> buildNormalRewardOptions(RogueRun run, RogueDeck rogueDeck,
                                                            boolean mythicOnly, CardRewardContext rewardCtx,
                                                            CardSelectionContext selCtx) {
        int baseNonMythics;
        int baseMythics;
        if (mythicOnly) {
            baseNonMythics = 0;
            baseMythics = 3;
        } else {
            baseNonMythics = Math.max(0, 6 - selCtx.extraMythics + rewardCtx.nonMythicCardCountAdjustment)
                + rewardCtx.additionalNonMythicCards;
            baseMythics = 1 + selCtx.extraMythics;
        }

        Predicate<PaperCard> notAlreadyOwned = run.getNotAlreadyInDeckPredicate();
        List<PaperCard> nonMythicCards = baseNonMythics > 0
            ? rogueDeck.drawRewardOptions(baseNonMythics,
                combineFilters(PaperCardPredicates.IS_MYTHIC_RARE.negate(), notAlreadyOwned))
            : new ArrayList<>();
        Set<String> selectedReplacementCardNames = replaceNonMythicCards(nonMythicCards, baseNonMythics,
            rewardCtx, notAlreadyOwned);

        List<PaperCard> mythicCards = rogueDeck.drawRewardOptions(baseMythics,
            combineFilters(PaperCardPredicates.IS_MYTHIC_RARE, notAlreadyOwned)
                .and(card -> !selectedReplacementCardNames.contains(getNormalizedName(card))));

        List<PaperCard> rewardOptions = new ArrayList<>();
        rewardOptions.addAll(nonMythicCards);
        rewardOptions.addAll(mythicCards);
        return rewardOptions;
    }

    private static Set<String> replaceNonMythicCards(List<PaperCard> nonMythicCards, int baseNonMythics,
                                                     CardRewardContext rewardCtx,
                                                     Predicate<PaperCard> notAlreadyOwned) {
        Set<String> selectedReplacementCardNames = new HashSet<>();
        if (baseNonMythics <= 0 || rewardCtx.nonMythicCardReplacementCount <= 0
            || rewardCtx.nonMythicCardReplacementCandidates.isEmpty()) {
            return selectedReplacementCardNames;
        }

        Set<String> normalNonMythicCardNames = new HashSet<>();
        for (PaperCard card : nonMythicCards) {
            normalNonMythicCardNames.add(getNormalizedName(card));
        }

        List<PaperCard> replacementCandidates = new ArrayList<>();
        for (PaperCard card : rewardCtx.nonMythicCardReplacementCandidates) {
            String normalizedName = getNormalizedName(card);
            if (notAlreadyOwned.test(card) && !normalNonMythicCardNames.contains(normalizedName)) {
                replacementCandidates.add(card);
            }
        }
        if (replacementCandidates.isEmpty()) {
            return selectedReplacementCardNames;
        }

        Collections.shuffle(replacementCandidates, MyRandom.getRandom());
        Collections.shuffle(nonMythicCards, MyRandom.getRandom());

        int actualReplacementCount = Math.min(baseNonMythics, Math.min(nonMythicCards.size(),
            Math.min(replacementCandidates.size(), rewardCtx.nonMythicCardReplacementCount)));
        if (actualReplacementCount <= 0) {
            return selectedReplacementCardNames;
        }

        nonMythicCards.subList(0, actualReplacementCount).clear();
        List<PaperCard> selectedReplacementCards = replacementCandidates.subList(0, actualReplacementCount);
        nonMythicCards.addAll(selectedReplacementCards);
        for (PaperCard card : selectedReplacementCards) {
            selectedReplacementCardNames.add(getNormalizedName(card));
        }
        return selectedReplacementCardNames;
    }

    private static String getNormalizedName(PaperCard card) {
        return card.getRules().getNormalizedName();
    }

    private static List<PaperCard> buildCustomRewardOptions(RogueRun run, CardRewardContext rewardCtx) {
        if (rewardCtx.rewardCards == null || rewardCtx.rewardCards.isEmpty()) {
            return List.of();
        }
        Predicate<PaperCard> notAlreadyOwned = run.getNotAlreadyInDeckPredicate();
        return rewardCtx.rewardCards.stream()
            .filter(notAlreadyOwned)
            .collect(Collectors.toList());
    }

    public static <T> Predicate<T> combineFilters(Predicate<T> baseFilter,
                                                  Predicate<T> extraFilter) {
        return extraFilter == null ? baseFilter : baseFilter.and(extraFilter);
    }
}

package forge.screens.home.rogue;

import forge.deck.DeckSection;
import forge.gamemodes.rogue.CardRewardHelper;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.EffectResultContext;
import forge.item.PaperCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class EffectResultHelper {

    private EffectResultHelper() {
    }

    static boolean handleTrigger(EffectResultContext ctx, RogueRun currentRun) {
        if (ctx.trigger == null) {
            return true;
        }

        return switch (ctx.trigger) {
            case CARD_REMOVAL -> handleCardRemovalTrigger(ctx, currentRun);
            case CARD_ADDITION -> {
                handleCardAdditionTrigger(ctx, currentRun);
                yield true;
            }
            case CARD_REWARD, MYTHIC_CARD_REWARD -> {
                handleCardRewardTrigger(ctx, currentRun);
                yield true;
            }
            default -> throw new IllegalStateException(
                "Unsupported portable effect trigger: " + ctx.trigger);
        };
    }

    private static boolean handleCardRemovalTrigger(EffectResultContext ctx, RogueRun currentRun) {
        int removeMaxCount = Math.min(ctx.cardSelectionMaxCount, getCandidateCardCount(ctx));
        int removeMinCount = Math.min(ctx.cardSelectionMinCount, removeMaxCount);
        if (removeMaxCount <= 0) {
            return true;
        }

        List<PaperCard> removed = new CardSelectionDialog(
            "Card Selection",
            getCardRemovalSubtitle(removeMinCount, removeMaxCount),
            ctx.candidateCards, currentRun, removeMinCount, removeMaxCount, ctx.buildDeckPreview).show();
        if (removed.size() < removeMinCount) {
            return false;
        }

        for (PaperCard card : removed) {
            currentRun.getCurrentDeck().getMain().remove(card);
        }
        ctx.removedCards = removed;

        if (ctx.replacementCards != null && !ctx.replacementCards.isEmpty()) {
            currentRun.addCardsToDeck(ctx.replacementCards, false);
            if (currentRun.getSelectedRogueDeck() != null) {
                currentRun.getSelectedRogueDeck().removeFromCardPools(ctx.replacementCards);
            }
            ctx.addedCards.addAll(ctx.replacementCards);
            CodexHelper.recordAcquiredCards(currentRun, ctx.replacementCards);
        }
        if (ctx.replacementCarryCard != null && ctx.replacementCarryCard.card() != null) {
            currentRun.addCarryCard(ctx.replacementCarryCard.card(), ctx.replacementCarryCard.type(),
                ctx.replacementCarryCard.sourceId());
            ctx.addedCards.add(ctx.replacementCarryCard.card());
            CodexHelper.recordAcquiredCards(currentRun, List.of(ctx.replacementCarryCard.card()));
        }
        return true;
    }

    private static void handleCardAdditionTrigger(EffectResultContext ctx, RogueRun currentRun) {
        int addMaxCount = Math.min(ctx.cardSelectionMaxCount, getCandidateCardCount(ctx));
        int addMinCount = Math.min(ctx.cardSelectionMinCount, addMaxCount);
        if (addMaxCount <= 0) {
            return;
        }

        CodexHelper.recordCardRewardOptions(currentRun, ctx.candidateCards);
        String subtitle = ctx.cardSelectionSubtitle != null
            ? ctx.cardSelectionSubtitle : getCardAdditionSubtitle(addMinCount, addMaxCount);
        List<PaperCard> added = new CardSelectionDialog(
            "Card Selection",
            subtitle,
            ctx.candidateCards, currentRun, addMinCount, addMaxCount, ctx.buildDeckPreview).show();
        if (added.isEmpty() && addMinCount > 0) {
            return;
        }

        if (ctx.onCardsSelected != null) {
            ctx.onCardsSelected.accept(added);
            CodexHelper.recordAcquiredCards(currentRun, ctx.addedCards);
            return;
        }

        if (ctx.addSection == DeckSection.Commander) {
            if (ctx.replaceCurrentCardsInAddSection) {
                List<PaperCard> removedCommanders = new ArrayList<>(currentRun.getCurrentDeck().getCommanders());
                if (!removedCommanders.isEmpty()) {
                    for (PaperCard commander : removedCommanders) {
                        currentRun.getCurrentDeck().getOrCreate(DeckSection.Commander).remove(commander);
                    }
                    ctx.removedCards = removedCommanders;
                }
            }
            for (PaperCard card : added) {
                currentRun.getCurrentDeck().getOrCreate(DeckSection.Commander).add(card);
            }
        } else {
            if (ctx.cardSelectionCopyCount > 1) {
                List<PaperCard> copiedCards = new ArrayList<>();
                for (PaperCard card : added) {
                    copiedCards.addAll(Collections.nCopies(ctx.cardSelectionCopyCount, card));
                }
                added = copiedCards;
            }
            currentRun.addCardsToDeck(added, false);
        }
        ctx.addedCards = added;
        CodexHelper.recordAcquiredCards(currentRun, added);
    }

    private static void handleCardRewardTrigger(EffectResultContext ctx, RogueRun currentRun) {
        boolean mythicOnly = ctx.trigger == EffectResultContext.ActionTriggerType.MYTHIC_CARD_REWARD;
        CardRewardHelper.runReward(currentRun,
            (title, cards, max, label, enabled, gold) ->
                new CardRewardDialog(title, cards, max, label, enabled, gold).show(),
            mythicOnly, null, ctx.cardRewardContext);
    }

    private static int getCandidateCardCount(EffectResultContext ctx) {
        return ctx.candidateCards == null ? 0 : ctx.candidateCards.size();
    }

    private static String getCardAdditionSubtitle(int addMinCount, int addMaxCount) {
        if (addMinCount == addMaxCount) {
            return addMaxCount == 1 ? "Choose 1 card to add." : "Choose " + addMaxCount + " cards to add.";
        }
        return "Choose up to " + addMaxCount + " cards to add.";
    }

    private static String getCardRemovalSubtitle(int removeMinCount, int removeMaxCount) {
        if (removeMinCount == removeMaxCount) {
            return removeMaxCount == 1 ? "Choose 1 card to remove." : "Choose " + removeMaxCount + " cards to remove.";
        }
        return "Choose up to " + removeMaxCount + " cards to remove.";
    }
}

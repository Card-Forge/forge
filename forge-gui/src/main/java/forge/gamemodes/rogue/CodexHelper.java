package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.rogue.effect.BazaarItem;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.EventEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.WoundEffect;
import forge.item.PaperCard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CodexHelper {
    public enum TraitCategory {
        NPC,
        CHEST,
        EVENT,
        WOUND
    }

    private static final ZoneType[] PLANEBOUND_PUBLIC_ZONES = {
        ZoneType.Battlefield,
        ZoneType.Graveyard,
        ZoneType.Exile,
        ZoneType.Command,
        ZoneType.Stack
    };

    private static final Map<String, Set<String>> COMMANDER_REWARD_NAMES = new HashMap<>();

    private CodexHelper() {
    }

    public static void recordCardRewardOptions(RogueRun run, Collection<PaperCard> cards) {
        recordCommanderRewardCards(run, cards, false);
    }

    public static void recordAcquiredCards(RogueRun run, Collection<PaperCard> cards) {
        recordCommanderRewardCards(run, cards, true);
    }

    public static void recordBazaarInventory(RogueRun run, Collection<BazaarItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (BazaarItem item : items) {
            if (item == null) {
                continue;
            }
            recordCommanderRewardCard(run, item.card(), false);
            recordTraitChoice(item.traitEffect());
        }
    }

    public static void recordBazaarPurchases(RogueRun run, Collection<BazaarItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (BazaarItem item : items) {
            if (item == null) {
                continue;
            }
            recordCommanderRewardCard(run, item.card(), true);
            recordTraitAcquired(item.traitEffect());
        }
    }

    public static void recordTraitChoices(Collection<? extends RogueEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (RogueEffect effect : effects) {
            recordTraitChoice(effect);
        }
    }

    public static void recordTraitChoice(RogueEffect effect) {
        if (getCodexVisibleTraitCategory(effect) == null) {
            return;
        }
        RogueMetaProgress.getInstance().markTraitSeen(effect);
    }

    public static void recordTraitAcquired(RogueEffect effect) {
        if (getCodexVisibleTraitCategory(effect) == null) {
            return;
        }
        RogueMetaProgress.getInstance().markTraitAcquired(effect);
    }

    public static void recordPlaneboundEncounter(RoguePlanebound planebound) {
        RogueMetaProgress.getInstance().markPlaneboundEncountered(planebound);
    }

    public static void recordPlaneboundCommanderCards(RoguePlanebound planebound, Collection<PaperCard> cards) {
        if (planebound == null || cards == null || cards.isEmpty()) {
            return;
        }

        for (PaperCard card : cards) {
            if (card != null) {
                RogueMetaProgress.getInstance().markPlaneboundCardSeen(planebound, card);
            }
        }
    }

    public static void recordPlaneboundPublicCardsOwnedBy(RoguePlanebound planebound,
                                                          Collection<Player> zonePlayers,
                                                          Collection<Player> cardOwners) {
        if (planebound == null || zonePlayers == null || zonePlayers.isEmpty()
            || cardOwners == null || cardOwners.isEmpty()) {
            return;
        }

        Set<Player> ownerSet = new HashSet<>(cardOwners);
        for (Player zonePlayer : zonePlayers) {
            if (zonePlayer == null) {
                continue;
            }
            for (ZoneType zone : PLANEBOUND_PUBLIC_ZONES) {
                for (Card card : zonePlayer.getCardsIn(zone)) {
                    if (card != null && ownerSet.contains(card.getOwner())) {
                        recordPlaneboundCard(planebound, card);
                    }
                }
            }
        }
    }

    public static void unlockCodex() {
        RogueMetaProgress.getInstance().updateCodexProgress(codexProgress -> {
            boolean changed = false;
            for (RogueDeck commander : RogueConfig.loadRogueDecks()) {
                if (!commander.isExperimental()) {
                    changed |= unlockCommanderCodex(codexProgress, commander);
                }
            }
            for (RoguePlanebound planebound : RogueConfig.loadPlanebounds()) {
                changed |= unlockPlaneboundCodex(codexProgress, planebound);
            }
            for (List<RogueEffect> traits : getCodexTraitsByCategory().values()) {
                for (RogueEffect trait : traits) {
                    changed |= codexProgress.markTraitAcquired(trait.getId());
                }
            }
            return changed;
        });
    }

    public static boolean isCodexComplete(RogueMetaProgress progress) {
        if (progress == null) {
            return false;
        }

        List<RogueDeck> commanders = RogueConfig.loadRogueDecks().stream()
            .filter(commander -> !commander.isExperimental())
            .collect(Collectors.toList());
        List<RoguePlanebound> planebounds = RogueConfig.loadPlanebounds();
        if (commanders.isEmpty() || planebounds.isEmpty()) {
            return false;
        }

        for (RogueDeck commander : commanders) {
            if (!isCommanderCodexComplete(progress, commander)) {
                return false;
            }
        }
        for (RoguePlanebound planebound : planebounds) {
            if (!isPlaneboundCodexComplete(progress, planebound)) {
                return false;
            }
        }
        return areCodexTraitsComplete(progress);
    }

    public static Map<TraitCategory, List<RogueEffect>> getCodexTraitsByCategory() {
        Map<TraitCategory, List<RogueEffect>> result = new EnumMap<>(TraitCategory.class);
        addCodexTraits(result, ImmutableList.copyOf(NPCEffect.values()));
        addCodexTraits(result, ImmutableList.copyOf(ChestEffect.values()));
        addCodexTraits(result, ImmutableList.copyOf(EventEffect.values()));
        addCodexTraits(result, ImmutableList.copyOf(WoundEffect.values()));
        for (List<RogueEffect> traits : result.values()) {
            traits.sort(Comparator.comparing(RogueEffect::getUIDisplayName, String.CASE_INSENSITIVE_ORDER));
        }
        return result;
    }

    public static TraitCategory getTraitCategory(RogueEffect effect) {
        if (effect == null || StringUtils.isBlank(effect.getId())) {
            return null;
        }
        if (effect instanceof NPCEffect) {
            return TraitCategory.NPC;
        }
        if (effect instanceof WoundEffect) {
            return TraitCategory.WOUND;
        }
        String effectCardReference = effect.getEffectCardReference();
        if (StringUtils.isBlank(effectCardReference)) {
            return null;
        }
        if (effect instanceof ChestEffect) {
            return TraitCategory.CHEST;
        }
        if (effect instanceof EventEffect) {
            return TraitCategory.EVENT;
        }
        return null;
    }

    private static TraitCategory getCodexVisibleTraitCategory(RogueEffect effect) {
        TraitCategory category = getTraitCategory(effect);
        if (category == null) {
            return null;
        }

        PaperCard effectCard = effect.getEffectCard();
        if (!isValidCard(effectCard)) {
            return null;
        }

        String expectedCardType = category == TraitCategory.WOUND ? "Wound" : "Trait";
        return effectCard.getRules().getType().hasStringType(expectedCardType) ? category : null;
    }

    private static boolean isCommanderCodexComplete(RogueMetaProgress progress, RogueDeck commander) {
        if (commander == null || commander.getCommanderCardName() == null) {
            return true;
        }
        for (PaperCard card : commander.getRewardPoolCards()) {
            if (!isValidCard(card)) {
                continue;
            }
            if (!progress.hasAcquiredCommanderRewardCard(commander.getCommanderCardName(), card)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlaneboundCodexComplete(RogueMetaProgress progress, RoguePlanebound planebound) {
        if (!progress.hasEncounteredPlanebound(planebound)) {
            return false;
        }
        for (PaperCard card : getPlaneboundCodexCards(planebound)) {
            if (!progress.hasSeenPlaneboundCard(planebound, card)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areCodexTraitsComplete(RogueMetaProgress progress) {
        boolean hasTraits = false;
        for (List<RogueEffect> traits : getCodexTraitsByCategory().values()) {
            for (RogueEffect trait : traits) {
                hasTraits = true;
                if (!progress.hasAcquiredTrait(trait)) {
                    return false;
                }
            }
        }
        return hasTraits;
    }

    private static boolean unlockCommanderCodex(RogueMetaProgress.CodexProgress codexProgress,
                                                RogueDeck commander) {
        if (commander == null || commander.getCommanderCardName() == null) {
            return false;
        }
        boolean changed = false;
        for (PaperCard card : commander.getRewardPoolCards()) {
            if (!isValidCard(card)) {
                continue;
            }
            changed |= codexProgress.markCommanderRewardCardAcquired(commander.getCommanderCardName(),
                normalize(card));
        }
        return changed;
    }

    private static boolean unlockPlaneboundCodex(RogueMetaProgress.CodexProgress codexProgress,
                                                 RoguePlanebound planebound) {
        if (planebound == null || StringUtils.isBlank(planebound.deckPath())) {
            return false;
        }

        boolean changed = codexProgress.markPlaneboundEncountered(planebound.deckPath());
        for (PaperCard card : getPlaneboundCodexCards(planebound)) {
            changed |= codexProgress.markPlaneboundCardSeen(planebound.deckPath(), normalize(card));
        }
        return changed;
    }

    private static List<PaperCard> getPlaneboundCodexCards(RoguePlanebound planebound) {
        Deck deck = RogueConfig.loadPlaneboundDeck(planebound);
        if (deck == null) {
            return ImmutableList.of();
        }

        List<PaperCard> cards = new ArrayList<>();
        for (PaperCard commander : deck.getCommanders()) {
            if (isValidCard(commander)) {
                cards.add(commander);
            }
        }

        Set<String> displayedBasicLandNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PaperCard card : deck.getAllCardsInASinglePool(false, false).toFlatList()) {
            if (isValidCard(card)) {
                boolean shouldAddCard = !card.getRules().getType().isBasicLand()
                    || displayedBasicLandNames.add(card.getName());
                if (shouldAddCard) {
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    private static boolean isValidCard(PaperCard card) {
        return card != null && card.getRules() != null;
    }

    private static void addCodexTraits(Map<TraitCategory, List<RogueEffect>> result,
                                       List<? extends RogueEffect> traits) {
        for (RogueEffect trait : traits) {
            TraitCategory category = getCodexVisibleTraitCategory(trait);
            if (category != null) {
                result.computeIfAbsent(category, key -> new ArrayList<>()).add(trait);
            }
        }
    }

    private static void recordCommanderRewardCards(RogueRun run, Collection<PaperCard> cards, boolean acquired) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        for (PaperCard card : cards) {
            recordCommanderRewardCard(run, card, acquired);
        }
    }

    private static void recordCommanderRewardCard(RogueRun run, PaperCard card, boolean acquired) {
        if (run == null || card == null || run.getSelectedRogueDeck() == null
            || run.getSelectedRogueDeck().isExperimental() || !run.recordsCuratedCommanderRewards()) {
            return;
        }

        String commanderName = run.getSelectedRogueDeck().getCommanderCardName();
        if (commanderName == null || !getCommanderRewardNames(commanderName).contains(normalize(card))) {
            return;
        }

        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        if (acquired) {
            progress.markCommanderRewardCardAcquired(commanderName, card);
        } else {
            progress.markCommanderRewardCardSeen(commanderName, card);
        }
    }

    private static void recordPlaneboundCard(RoguePlanebound planebound, Card card) {
        if (card == null) {
            return;
        }

        if (card.getPaperCard() instanceof PaperCard paperCard) {
            RogueMetaProgress.getInstance().markPlaneboundCardSeen(planebound, paperCard);
        }
    }

    private static Set<String> getCommanderRewardNames(String commanderName) {
        return COMMANDER_REWARD_NAMES.computeIfAbsent(commanderName, key -> {
            Set<String> names = new HashSet<>();
            RogueConfig.loadRogueDecks().stream()
                .filter(deck -> key.equals(deck.getCommanderCardName()))
                .findFirst()
                .ifPresent(deck -> {
                    for (PaperCard card : deck.getRewardPoolCards()) {
                        String normalized = normalize(card);
                        if (normalized != null) {
                            names.add(normalized);
                        }
                    }
                });
            return names;
        });
    }

    private static String normalize(PaperCard card) {
        return card == null || card.getRules() == null ? null : card.getRules().getNormalizedName();
    }
}

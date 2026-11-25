package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.DeckHints;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.CommanderBracketCalculator;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.gamemodes.limited.CardThemedCommanderDeckBuilder;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.MyRandom;
import org.tinylog.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Builds a small, playable Rogue deck from Forge's Commander deck generator. */
public final class RogueCustomDeckGenerator {
    private static final int MAX_GENERATION_ATTEMPTS = 8;
    private static final int STARTING_SPELL_COUNT = 19;
    private static final int STARTING_LAND_COUNT = 14;
    private static final int STARTING_THEME_TARGET = 5;
    private static final int MAX_STARTING_CMC = 6;
    private static final int MAX_STARTING_SUPPORT_BRACKET = 2;
    private static final int REWARD_POOL_SIZE = 180;
    private static final int MIN_REWARD_CREATURE_PERCENT = 30;
    private static final int MAX_REWARD_CREATURE_PERCENT = 55;

    private RogueCustomDeckGenerator() {
    }

    public static List<PaperCard> getEligibleCommanders() {
        Set<String> commanderPoolNames = new HashSet<>();
        FModel.getCommanderPool().forEach(entry -> commanderPoolNames.add(entry.getKey().getName()));
        Map<String, PaperCard> commanders = new LinkedHashMap<>();
        for (PaperCard card : RogueConfig.getAllCards(candidate ->
            commanderPoolNames.contains(candidate.getName())
                && !candidate.getRules().canBeBackground()
                && DeckFormat.Commander.isLegalCard(candidate)
                && DeckFormat.Commander.isLegalCommander(candidate.getRules())
                && !candidate.getName().startsWith("A-"))) {
            PaperCard preferredPrint = RogueConfig.findPreferredPrint(card, RogueConfig::isEligibleRogueCard, null)
                .orElse(card);
            commanders.putIfAbsent(preferredPrint.getName(), preferredPrint);
        }
        return new ArrayList<>(commanders.values());
    }

    public static List<PaperCard> getEligiblePartnerCommanders(PaperCard commander) {
        if (commander == null || !commander.getRules().canBePartnerCommander()) {
            return Collections.emptyList();
        }
        return RogueConfig.getAllCards(candidate ->
            DeckFormat.Commander.isLegalCard(candidate)
                && DeckFormat.Commander.isLegalCommander(candidate.getRules())
                && !candidate.getName().startsWith("A-")
                && !candidate.getName().equals(commander.getName())
                && commander.getRules().canBePartnerCommanders(candidate.getRules())).stream()
            .map(card -> RogueConfig.findPreferredPrint(card, RogueConfig::isEligibleRogueCard, null).orElse(card))
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public static RogueDeck generate() {
        List<PaperCard> commanders = getEligibleCommanders();
        if (commanders.isEmpty()) {
            return null;
        }
        Collections.shuffle(commanders, MyRandom.getRandom());
        PaperCard commander = commanders.get(0);
        List<PaperCard> partners = getEligiblePartnerCommanders(commander);
        List<PaperCard> selectedCommanders = partners.isEmpty() ? ImmutableList.of(commander)
            : ImmutableList.of(commander, partners.get(MyRandom.getRandom().nextInt(partners.size())));

        Map<String, List<Map.Entry<PaperCard, Integer>>> matrix = getSynergyMatrix();
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            RogueDeck deck = tryGenerate(selectedCommanders, matrix, true);
            if (deck != null) {
                return deck;
            }
        }
        return null;
    }

    /** Empty when they cannot command a deck together. */
    public static List<PaperCard> findChosenCommanders(List<PaperCard> chosenCommanders) {
        if (chosenCommanders == null || chosenCommanders.isEmpty() || chosenCommanders.size() > 2) {
            return Collections.emptyList();
        }
        PaperCard commander = getEligibleCommanders().stream()
            .filter(card -> card.getName().equals(chosenCommanders.get(0).getName()))
            .findFirst().orElse(null);
        if (commander == null) {
            return Collections.emptyList();
        }
        List<PaperCard> commanders = new ArrayList<>();
        commanders.add(commander);
        if (chosenCommanders.size() == 2) {
            PaperCard partner = getEligiblePartnerCommanders(commander).stream()
                .filter(card -> card.getName().equals(chosenCommanders.get(1).getName()))
                .findFirst().orElse(null);
            if (partner == null) {
                return Collections.emptyList();
            }
            commanders.add(partner);
        }
        return commanders;
    }

    public static RogueDeck generateForChosenCommander(List<PaperCard> chosenCommanders) {
        List<PaperCard> commanders = findChosenCommanders(chosenCommanders);
        if (commanders.isEmpty()) {
            return null;
        }

        Map<String, List<Map.Entry<PaperCard, Integer>>> matrix = getSynergyMatrix();
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            RogueDeck deck = tryGenerate(commanders, matrix, false);
            if (deck != null) {
                return deck;
            }
        }
        return null;
    }

    public static RogueDeck generateStartingDeck(RogueDeck selectedDeck) {
        return generateModifiedDeck(selectedDeck, true);
    }

    public static RogueDeck generateRewardPool(RogueDeck selectedDeck) {
        return generateModifiedDeck(selectedDeck, false);
    }

    private static RogueDeck generateModifiedDeck(RogueDeck selectedDeck,
            boolean replaceStartingDeck) {
        if (selectedDeck == null || selectedDeck.getStartDeck() == null
            || selectedDeck.getStartDeck().getCommanders().size() != 1) {
            return null;
        }
        PaperCard commander = selectedDeck.getStartDeck().getCommanders().get(0);
        List<Map.Entry<PaperCard, Integer>> synergy = getCommanderSynergy(ImmutableList.of(commander), getSynergyMatrix());
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            RogueDeck deck = tryGenerateModifiedDeck(selectedDeck, commander, synergy,
                replaceStartingDeck);
            if (deck != null) {
                return deck;
            }
        }
        return null;
    }

    private static RogueDeck tryGenerateModifiedDeck(RogueDeck selectedDeck, PaperCard commander,
            List<Map.Entry<PaperCard, Integer>> synergy, boolean replaceStartingDeck) {
        try {
            DeckGenerationContext context = prepareDeckGenerationContext(ImmutableList.of(commander), synergy);
            RogueDeck deck = selectedDeck.copy();
            if (replaceStartingDeck) {
                Deck start = buildStartingDeck(context, selectedDeck.getName(),
                    selectedDeck.getLandEdition());
                if (start == null) {
                    return null;
                }
                deck.setStartDeck(start);
            } else {
                CardPool rewards = buildRewardPool(context, deck.getStartDeck());
                if (rewards == null) {
                    return null;
                }
                deck.setRewardPool(rewards);
            }
            return deck;
        } catch (RuntimeException e) {
            Logger.warn(e, "Could not generate custom deck part for {}", commander.getName());
            return null;
        }
    }

    private static Map<String, List<Map.Entry<PaperCard, Integer>>> getSynergyMatrix() {
        if (!CardRelationMatrixGenerator.cardPools.containsKey(DeckFormat.Commander.toString())) {
            try {
                CardRelationMatrixGenerator.initializeFormat(DeckFormat.Commander);
            } catch (RuntimeException e) {
                Logger.warn(e, "Commander synergy data unavailable; using color-identity generation");
            }
        }
        return CardRelationMatrixGenerator.cardPools.get(DeckFormat.Commander.toString());
    }

    private static List<Map.Entry<PaperCard, Integer>> getCommanderSynergy(List<PaperCard> commanders,
            Map<String, List<Map.Entry<PaperCard, Integer>>> matrix) {
        if (matrix == null) {
            return ImmutableList.of();
        }
        Map<String, Map.Entry<PaperCard, Integer>> synergy = new LinkedHashMap<>();
        for (PaperCard commander : commanders) {
            for (Map.Entry<PaperCard, Integer> entry : matrix.getOrDefault(commander.getName(), ImmutableList.of())) {
                synergy.merge(entry.getKey().getName(), entry,
                    (first, second) -> Map.entry(first.getKey(), first.getValue() + second.getValue()));
            }
        }
        return new ArrayList<>(synergy.values());
    }

    private static RogueDeck tryGenerate(List<PaperCard> commanders,
            Map<String, List<Map.Entry<PaperCard, Integer>>> matrix, boolean randomlyChosen) {
        List<Map.Entry<PaperCard, Integer>> synergy = getCommanderSynergy(commanders, matrix);
        try {
            return generateForCommander(commanders, synergy, randomlyChosen);
        } catch (RuntimeException e) {
            Logger.warn(e, "Could not generate Rogue deck for {}", commanders.get(0).getName());
            return null;
        }
    }

    private static RogueDeck generateForCommander(List<PaperCard> commanders,
            List<Map.Entry<PaperCard, Integer>> synergy, boolean randomlyChosen) {
        PaperCard commander = commanders.get(0);
        DeckGenerationContext context = prepareDeckGenerationContext(commanders, synergy);
        Deck start = buildStartingDeck(context,
            (randomlyChosen ? "Random " : "Chosen ") + commander.getName(), null);
        if (start == null) {
            return null;
        }
        CardPool rewardPool = buildRewardPool(context, start);
        if (rewardPool == null) {
            return null;
        }

        RogueDeck rogueDeck = new RogueDeck();
        rogueDeck.setName(start.getName());
        rogueDeck.setCommanderCardName(commander.getName());
        rogueDeck.setStartDeck(start);
        rogueDeck.setRewardPool(rewardPool);
        rogueDeck.setDescription(randomlyChosen ? "A random Commander and starting deck."
            : "A chosen Commander and generated starting deck.");
        rogueDeck.setThemeDescription("Generated from Commander synergy and color identity.");
        rogueDeck.setAvatarIndex(0);
        rogueDeck.setSleeveIndex(1);
        rogueDeck.setLandEdition("");
        rogueDeck.setIncludeColorlessBasics(context.commanderColors().isColorless());
        return rogueDeck;
    }

    private record DeckGenerationContext(List<PaperCard> commanders, ColorSet commanderColors,
            List<PaperCard> startingCards,
            Map<String, PaperCard> startingByName, List<PaperCard> eligibleCards,
            Map<String, PaperCard> eligibleByName, LinkedHashMap<String, PaperCard> generatedSpells,
            LinkedHashMap<String, PaperCard> generatedRewardCards,
            LinkedHashMap<String, PaperCard> generatedLands,
            List<Map.Entry<PaperCard, Integer>> rankedSynergy, int maxBracket,
            int targetStartingCreatures, ThemeMatches themeMatches) {}

    private record ThemeMatches(Set<String> needs, Set<String> hints) {
        private boolean matches(PaperCard card) {
            return needs.contains(card.getName()) || hints.contains(card.getName());
        }

        private int count(Collection<PaperCard> cards) {
            return (int) cards.stream().filter(card -> !card.getRules().getType().isLand())
                .filter(this::matches).count();
        }

        private List<List<PaperCard>> prioritize(List<List<PaperCard>> sources) {
            List<List<PaperCard>> prioritized = new ArrayList<>();
            for (Set<String> names : ImmutableList.of(needs, hints)) {
                for (List<PaperCard> source : sources) {
                    prioritized.add(source.stream().filter(card -> names.contains(card.getName())).collect(Collectors.toList()));
                }
            }
            return prioritized;
        }
    }

    private enum RewardThemeCategory {
        MYTHICS, CREATURES, SPELLS;

        private boolean matches(PaperCard card) {
            if (card.getRules().getType().isLand()) {
                return false;
            }
            return switch (this) {
                case MYTHICS -> card.getRarity() == CardRarity.MythicRare;
                case CREATURES -> card.getRarity() != CardRarity.MythicRare
                    && card.getRules().getType().isCreature();
                case SPELLS -> card.getRarity() != CardRarity.MythicRare
                    && !card.getRules().getType().isCreature();
            };
        }
    }

    private static Set<String> getThemeCardNames(DeckHints hints, List<PaperCard> candidates) {
        Set<String> names = new HashSet<>();
        if (hints != null && hints.isValid()) {
            for (PaperCard card : hints.filter(candidates)) {
                names.add(card.getName());
            }
        }
        return names;
    }

    private static DeckGenerationContext prepareDeckGenerationContext(List<PaperCard> commanders,
            List<Map.Entry<PaperCard, Integer>> synergy) {
        int colorMask = 0;
        Set<String> commanderNames = new HashSet<>();
        Set<String> completeNames = new HashSet<>();
        for (PaperCard commander : commanders) {
            colorMask |= commander.getRules().getColorIdentity().getColor();
            commanderNames.add(commander.getName());
            completeNames.addAll(CommanderBracketCalculator.getCardNames(commander));
        }
        ColorSet commanderColors = ColorSet.fromMask(colorMask);
        Predicate<PaperCard> colorIdentity = card -> card.getRules().getColorIdentity()
            .hasNoColorsExcept(commanderColors);
        Predicate<PaperCard> eligible = card ->
            DeckFormat.Commander.isLegalCard(card) && colorIdentity.test(card)
                && !commanderNames.contains(card.getName());
        String preferredEdition = commanders.get(0).getEdition();
        List<PaperCard> eligibleCards = new ArrayList<>(
            RogueConfig.getRandomCardsForEdition(eligible, preferredEdition));
        List<PaperCard> startingCards = getStartingPrints(eligibleCards, eligible, preferredEdition);
        Set<String> excludedStartingCardNames = ImmutableSet.of("Sol Ring", "Arcane Signet");
        startingCards.removeIf(card -> excludedStartingCardNames.contains(card.getName()));
        Map<String, PaperCard> eligibleByName = indexByName(eligibleCards);
        Map<String, PaperCard> startingByName = indexByName(startingCards);
        Set<String> needs = new HashSet<>();
        Set<String> hints = new HashSet<>();
        for (PaperCard commander : commanders) {
            needs.addAll(getThemeCardNames(commander.getRules().getAiHints().getDeckNeeds(), eligibleCards));
            hints.addAll(getThemeCardNames(commander.getRules().getAiHints().getDeckHints(), eligibleCards));
        }
        hints.removeAll(needs);
        ThemeMatches themeMatches = new ThemeMatches(needs, hints);

        int commanderBracket = CommanderBracketCalculator.calculate(completeNames).getBracket();
        int maxBracket = Math.max(MAX_STARTING_SUPPORT_BRACKET, commanderBracket);
        List<Map.Entry<PaperCard, Integer>> rankedSynergy = new ArrayList<>(synergy);
        Collections.shuffle(rankedSynergy, MyRandom.getRandom());
        rankedSynergy.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        Deck generated = generateCommanderDeck(commanders, rankedSynergy, eligibleByName, maxBracket);
        if (generated == null) {
            throw new IllegalStateException("Could not generate a deck for the selected commanders");
        }

        LinkedHashMap<String, PaperCard> generatedSpells = new LinkedHashMap<>();
        LinkedHashMap<String, PaperCard> generatedRewardCards = new LinkedHashMap<>();
        LinkedHashMap<String, PaperCard> generatedLands = new LinkedHashMap<>();
        List<PaperCard> generatedCards = generated.getMain().toFlatList();
        long generatedNonlands = generatedCards.stream()
            .filter(card -> !card.getRules().getType().isLand()).count();
        long generatedCreatures = generatedCards.stream()
            .filter(card -> !card.getRules().getType().isLand()
                && card.getRules().getType().isCreature()).count();
        // Scale Forge's full-deck creature mix to the smaller Rogue starting deck.
        int targetStartingCreatures = generatedNonlands == 0 ? 0
            : (int) Math.round((double) STARTING_SPELL_COUNT * generatedCreatures / generatedNonlands);
        for (PaperCard card : generatedCards) {
            addGeneratedCard(card, startingByName, eligibleByName, generatedSpells,
                generatedRewardCards, generatedLands);
        }

        return new DeckGenerationContext(ImmutableList.copyOf(commanders), commanderColors,
            startingCards, startingByName, eligibleCards,
            eligibleByName, generatedSpells, generatedRewardCards, generatedLands,
            rankedSynergy, maxBracket, targetStartingCreatures, themeMatches);
    }

    private static List<PaperCard> getStartingPrints(List<PaperCard> eligibleCards,
            Predicate<PaperCard> eligible, String preferredEdition) {
        List<PaperCard> startingCards = new ArrayList<>();
        for (PaperCard card : eligibleCards) {
            PaperCard startingCard = card.getRarity() == CardRarity.MythicRare
                ? RogueConfig.getNonMythicRandomPrint(card, eligible, preferredEdition) : card;
            if (startingCard != null) {
                startingCards.add(startingCard);
            }
        }
        return startingCards;
    }

    private static Map<String, PaperCard> indexByName(List<PaperCard> cards) {
        Map<String, PaperCard> byName = new HashMap<>();
        for (PaperCard card : cards) {
            byName.putIfAbsent(card.getName(), card);
        }
        return byName;
    }

    private static Deck generateCommanderDeck(List<PaperCard> commanders,
            List<Map.Entry<PaperCard, Integer>> synergy, Map<String, PaperCard> eligibleByName, int maxBracket) {
        PaperCard commander = commanders.get(0);
        if (commanders.size() == 2 || commander.getRules().canBePartnerCommander()) {
            return generatePartnerCommanderDeck(commanders, synergy, eligibleByName, maxBracket);
        }
        Deck generated = null;
        if (!synergy.isEmpty()) {
            try {
                generated = DeckgenUtil.generateRandomCommanderDeck(commander,
                    DeckFormat.Commander, false, true, maxBracket);
            } catch (RuntimeException e) {
                Logger.warn(e, "Commander synergy generation failed for {}; using color-identity generation",
                    commander.getName());
            }
        }
        if (generated == null || generated.getMain().countAll() == 0) {
            generated = DeckgenUtil.generateRandomCommanderDeck(commander,
                DeckFormat.Commander, false, false, maxBracket);
        }
        return generated;
    }

    private static Deck generatePartnerCommanderDeck(List<PaperCard> commanders,
            List<Map.Entry<PaperCard, Integer>> synergy, Map<String, PaperCard> eligibleByName, int maxBracket) {
        // Match DeckgenUtil's weighted random ordering of synergy candidates.
        List<Map.Entry<PaperCard, Integer>> weightedSynergy = new ArrayList<>(synergy);
        Map<Map.Entry<PaperCard, Integer>, Double> sortKeys = new IdentityHashMap<>();
        for (Map.Entry<PaperCard, Integer> entry : weightedSynergy) {
            sortKeys.put(entry, Math.log(MyRandom.getRandom().nextDouble()) / Math.max(1, entry.getValue()));
        }
        weightedSynergy.sort(Comparator.comparingDouble(sortKeys::get).reversed());
        List<PaperCard> synergyCards = getSynergyCards(weightedSynergy, eligibleByName);
        List<PaperCard> colorCards = new ArrayList<>(eligibleByName.values());
        Collections.shuffle(colorCards, MyRandom.getRandom());
        colorCards = colorCards.subList(0, Math.min(400, colorCards.size()));
        for (List<PaperCard> source : ImmutableList.of(synergyCards, colorCards)) {
            if (source.isEmpty()) {
                continue;
            }
            List<PaperCard> candidates = limitCardsToCommanderBracket(source, commanders, maxBracket);
            Deck generated = buildCommanderDeck(commanders, candidates);
            if (generated != null) {
                return generated;
            }
        }
        return null;
    }

    private static List<PaperCard> limitCardsToCommanderBracket(List<PaperCard> cards,
            List<PaperCard> commanders, int maxBracket) {
        if (maxBracket < 1 || maxBracket >= 4) {
            return cards;
        }
        Set<String> completeNames = new HashSet<>();
        for (PaperCard commander : commanders) {
            completeNames.addAll(CommanderBracketCalculator.getCardNames(commander));
        }
        List<PaperCard> candidates = new ArrayList<>();
        for (PaperCard card : cards) {
            Set<String> nextNames = new HashSet<>(completeNames);
            nextNames.addAll(CommanderBracketCalculator.getCardNames(card));
            if (CommanderBracketCalculator.calculate(nextNames).getBracket() <= maxBracket) {
                candidates.add(card);
                completeNames = nextNames;
            }
        }
        return candidates;
    }

    private static Deck buildCommanderDeck(List<PaperCard> commanders, List<PaperCard> candidates) {
        PaperCard commander = commanders.get(0);
        PaperCard partner = commanders.size() == 2 ? commanders.get(1) : null;
        // The wrapper chooses its own partner; this builder keeps the selected command zone.
        try {
            CardThemedCommanderDeckBuilder builder = new CardThemedCommanderDeckBuilder(
                commander, partner, candidates, false, DeckFormat.Commander);
            builder.setSingleton(true);
            builder.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
            CardPool cards = builder.getDeck(DeckFormat.Commander.getMainRange().getMaximum(), false);
            if (cards.isEmpty()) {
                return null;
            }
            Deck generated = new Deck(commander.getName());
            generated.getMain().addAll(cards);
            generated.getOrCreate(DeckSection.Commander).addAllFlat(commanders);
            return generated;
        } catch (RuntimeException e) {
            Logger.warn(e, "Could not generate a deck for the selected command zone of {}",
                commander.getName());
            return null;
        }
    }

    private static void addGeneratedCard(PaperCard card, Map<String, PaperCard> startingByName,
            Map<String, PaperCard> eligibleByName, Map<String, PaperCard> generatedSpells,
            Map<String, PaperCard> generatedRewardCards, Map<String, PaperCard> generatedLands) {
        PaperCard eligibleCard = eligibleByName.get(card.getName());
        if (eligibleCard != null && !eligibleCard.getRules().getType().isBasicLand()) {
            generatedRewardCards.putIfAbsent(eligibleCard.getName(), eligibleCard);
        }
        PaperCard startingCard = startingByName.get(card.getName());
        if (startingCard == null) {
            return;
        }
        if (startingCard.getRules().getType().isLand()) {
            if (!startingCard.getRules().getType().isBasicLand()) {
                generatedLands.putIfAbsent(startingCard.getName(), startingCard);
            }
        } else {
            generatedSpells.putIfAbsent(startingCard.getName(), startingCard);
        }
    }

    private static Deck buildStartingDeck(DeckGenerationContext context, String name,
            String landEdition) {
        List<PaperCard> spellCandidates = new ArrayList<>(context.generatedSpells().values());
        Collections.shuffle(spellCandidates, MyRandom.getRandom());
        spellCandidates.sort(Comparator.comparingInt(card -> card.getRules().getManaCost().getCMC()));
        List<PaperCard> synergyCandidates = getSynergyCards(context.rankedSynergy(), context.startingByName());
        Collections.shuffle(synergyCandidates, MyRandom.getRandom());
        List<PaperCard> broadCandidates = new ArrayList<>(context.startingCards());
        Collections.shuffle(broadCandidates, MyRandom.getRandom());
        // Prefer Forge's generated deck, then other synergy cards, then the legal pool.
        List<List<PaperCard>> sources = ImmutableList.of(spellCandidates, synergyCandidates, broadCandidates);
        List<PaperCard> nonbasicLands = new ArrayList<>(context.generatedLands().values());
        Collections.shuffle(nonbasicLands, MyRandom.getRandom());

        Deck start = selectStartingDeck(context, name, landEdition, sources, nonbasicLands, false);
        if (start == null || isStartingDeckInvalid(context, start)) {
            return null;
        }
        if (context.themeMatches().count(start.getMain().toFlatList()) >= STARTING_THEME_TARGET
            || context.themeMatches().count(context.startingCards()) == 0) {
            return start;
        }
        Deck retry = selectStartingDeck(context, name, landEdition, sources, nonbasicLands, true);
        if (retry != null && !isStartingDeckInvalid(context, retry)
            && context.themeMatches().count(retry.getMain().toFlatList()) >= STARTING_THEME_TARGET) {
            return retry;
        }
        return start;
    }

    private static Deck selectStartingDeck(DeckGenerationContext context, String name,
            String landEdition, List<List<PaperCard>> sources, List<PaperCard> nonbasicLands,
            boolean prioritizeTheme) {
        Set<String> supportingNames = new HashSet<>();
        Set<String> completeNames = new HashSet<>();
        for (PaperCard commander : context.commanders()) {
            completeNames.addAll(CommanderBracketCalculator.getCardNames(commander));
        }
        Map<String, PaperCard> spells = selectStartingSpells(context, supportingNames, completeNames,
            sources, prioritizeTheme);
        if (spells.size() != STARTING_SPELL_COUNT) {
            return null;
        }
        List<PaperCard> lands = selectStartingLands(context, supportingNames, completeNames,
            landEdition, nonbasicLands);
        if (lands.isEmpty()) {
            return null;
        }

        Deck start = new Deck(name);
        start.getOrCreate(DeckSection.Commander).addAllFlat(context.commanders());
        for (PaperCard card : spells.values()) {
            start.getMain().add(card);
        }
        for (PaperCard card : lands) {
            start.getMain().add(card);
        }
        return start;
    }

    private static Map<String, PaperCard> selectStartingSpells(DeckGenerationContext context,
            Set<String> supportingNames, Set<String> completeNames,
            List<List<PaperCard>> sources, boolean prioritizeTheme) {
        LinkedHashMap<String, PaperCard> spells = new LinkedHashMap<>();
        addHighCurveSynergySpells(spells, supportingNames, completeNames, context);

        if (prioritizeTheme) {
            int targetSize = spells.size()
                + Math.max(0, STARTING_THEME_TARGET - context.themeMatches().count(spells.values()));
            for (List<PaperCard> source : context.themeMatches().prioritize(sources)) {
                addSpells(spells, source, supportingNames, completeNames, context.maxBracket(),
                    context.targetStartingCreatures(), targetSize);
            }
        }
        for (List<PaperCard> source : sources) {
            addSpells(spells, source, supportingNames, completeNames, context.maxBracket(),
                context.targetStartingCreatures(), STARTING_SPELL_COUNT);
        }
        if (spells.size() < STARTING_SPELL_COUNT) {
            // Relax only the creature target if the preferred mix cannot fill every slot.
            for (List<PaperCard> source : sources) {
                addSpells(spells, source, supportingNames, completeNames, context.maxBracket(), -1,
                    STARTING_SPELL_COUNT);
            }
        }
        return spells;
    }

    private static void addHighCurveSynergySpells(Map<String, PaperCard> spells,
            Set<String> supportingNames, Set<String> completeNames, DeckGenerationContext context) {
        // Reserve a few signature cards before the remaining Forge cards are sorted by cost.
        for (Map.Entry<PaperCard, Integer> entry : context.rankedSynergy()) {
            PaperCard card = context.generatedSpells().get(entry.getKey().getName());
            if (card != null && card.getRules().getManaCost().getCMC() >= 4
                && card.getRules().getManaCost().getCMC() <= MAX_STARTING_CMC
                && isWithinStartingRatio(spells, card, context.targetStartingCreatures())) {
                addSupportingCard(spells, card, supportingNames, completeNames,
                    context.maxBracket());
                if (spells.size() == 4) {
                    break;
                }
            }
        }
    }

    private static List<PaperCard> getSynergyCards(List<Map.Entry<PaperCard, Integer>> synergy,
            Map<String, PaperCard> cardsByName) {
        List<PaperCard> fallback = new ArrayList<>();
        for (Map.Entry<PaperCard, Integer> entry : synergy) {
            PaperCard card = cardsByName.get(entry.getKey().getName());
            if (card != null) {
                fallback.add(card);
            }
        }
        return fallback;
    }

    private static List<PaperCard> selectStartingLands(DeckGenerationContext context,
            Set<String> supportingNames, Set<String> completeNames, String landEdition,
            List<PaperCard> nonbasicLands) {
        ColorSet commanderColors = context.commanderColors();
        nonbasicLands.sort(Comparator.comparingInt(
            (PaperCard card) -> fixingColors(card, commanderColors)).reversed());
        int nonbasicLimit = switch (commanderColors.countColors()) {
            case 3 -> 8;
            case 4 -> 10;
            case 5 -> 9;
            default -> 6;
        };
        List<PaperCard> lands = new ArrayList<>();
        for (PaperCard card : nonbasicLands) {
            if (canAddSupportingCard(card, supportingNames, completeNames, context.maxBracket())) {
                lands.add(card);
                supportingNames.addAll(CommanderBracketCalculator.getCardNames(card));
                completeNames.addAll(CommanderBracketCalculator.getCardNames(card));
            }
            if (lands.size() == nonbasicLimit) {
                break;
            }
        }
        if (skipBasicLands(lands, commanderColors,
            RogueConfig.getBasicLandEdition(landEdition, context.commanders().get(0)), STARTING_LAND_COUNT)) {
            return Collections.emptyList();
        }
        return lands;
    }

    private static CardPool buildRewardPool(DeckGenerationContext context, Deck start) {
        List<PaperCard> generatedCards = new ArrayList<>(context.generatedRewardCards().values());
        Collections.shuffle(generatedCards, MyRandom.getRandom());
        List<PaperCard> synergyCards = getSynergyCards(context.rankedSynergy(), context.eligibleByName());
        List<PaperCard> eligibleCards = new ArrayList<>(context.eligibleCards());
        Collections.shuffle(eligibleCards, MyRandom.getRandom());
        // The full synergy list remains available even if Forge's one generated deck omitted a card.
        List<List<PaperCard>> sources = ImmutableList.of(generatedCards, synergyCards, eligibleCards);
        Set<String> used = new HashSet<>();
        for (PaperCard card : start.getMain().toFlatList()) {
            used.add(card.getName());
        }
        Map<RewardThemeCategory, Integer> targets = new EnumMap<>(RewardThemeCategory.class);
        CardPool normal = selectRewardPool(context, sources, used, targets);
        if (isRewardPoolInvalid(normal)) {
            return null;
        }
        List<PaperCard> normalCards = normal.toFlatList();
        for (RewardThemeCategory category : RewardThemeCategory.values()) {
            int availableMatches = (int) context.eligibleCards().stream()
                .filter(card -> !used.contains(card.getName())
                    && category.matches(card) && context.themeMatches().matches(card))
                .map(PaperCard::getName).distinct().count();
            if (availableMatches > 0) {
                int count = (int) normalCards.stream().filter(category::matches).count();
                targets.put(category, Math.min((int) Math.ceil(count * 0.25), availableMatches));
            }
        }
        if (meetsRewardThemeTargets(normal, context.themeMatches(), targets)) {
            return normal;
        }
        CardPool retry = selectRewardPool(context, sources, used, targets);
        if (!isRewardPoolInvalid(retry)
            && meetsRewardThemeTargets(retry, context.themeMatches(), targets)) {
            return retry;
        }
        return normal;
    }

    private static boolean meetsRewardThemeTargets(CardPool rewardPool, ThemeMatches themeMatches,
            Map<RewardThemeCategory, Integer> targets) {
        List<PaperCard> cards = rewardPool.toFlatList();
        for (Map.Entry<RewardThemeCategory, Integer> target : targets.entrySet()) {
            long matches = cards.stream().filter(target.getKey()::matches).filter(themeMatches::matches).count();
            if (matches < target.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static CardPool selectRewardPool(DeckGenerationContext context,
            List<List<PaperCard>> sources, Set<String> startingNames,
            Map<RewardThemeCategory, Integer> targets) {
        Set<String> used = new HashSet<>(startingNames);
        List<List<PaperCard>> themeSources = targets.isEmpty()
            ? ImmutableList.of() : context.themeMatches().prioritize(sources);
        CardPool rewardPool = new CardPool();
        // Reserve each required category from the same priority-ordered sources.
        addRewards(rewardPool, used, themeSources, targets.getOrDefault(RewardThemeCategory.MYTHICS, 0),
            RewardThemeCategory.MYTHICS::matches);
        addRewards(rewardPool, used, sources, 40,
            card -> card.getRarity() == CardRarity.MythicRare);
        int mythics = rewardPool.countAll();
        int nonMythics = REWARD_POOL_SIZE - mythics;
        int targetLands = (int) Math.round(nonMythics * 0.10);
        addRewards(rewardPool, used, sources, rewardPool.countAll() + targetLands,
            card -> card.getRarity() != CardRarity.MythicRare
                && card.getRules().getType().isLand());
        int lands = rewardPool.countAll() - mythics;
        int nonlandNonMythics = nonMythics - lands;
        int minCreatures = (int) Math.ceil(nonlandNonMythics * MIN_REWARD_CREATURE_PERCENT / 100.0);
        int maxCreatures = (int) Math.floor(nonlandNonMythics * MAX_REWARD_CREATURE_PERCENT / 100.0);
        int creatureTargetSize = rewardPool.countAll() + minCreatures;
        addRewards(rewardPool, used, themeSources, rewardPool.countAll()
            + targets.getOrDefault(RewardThemeCategory.CREATURES, 0),
            RewardThemeCategory.CREATURES::matches);
        addRewards(rewardPool, used, sources, creatureTargetSize, RewardThemeCategory.CREATURES::matches);
        addRewards(rewardPool, used, themeSources, rewardPool.countAll()
            + targets.getOrDefault(RewardThemeCategory.SPELLS, 0),
            RewardThemeCategory.SPELLS::matches);
        addRemainingNonlandRewards(rewardPool, used, sources, maxCreatures);
        return rewardPool;
    }

    private static void addRewards(CardPool rewardPool, Set<String> used,
            List<List<PaperCard>> sources, int targetSize, Predicate<PaperCard> filter) {
        int remaining = targetSize - rewardPool.countAll();
        for (List<PaperCard> source : sources) {
            for (PaperCard card : source) {
                if (remaining == 0) {
                    return;
                }
                if (filter.test(card) && addReward(rewardPool, used, card)) {
                    remaining--;
                }
            }
        }
    }

    private static void addRemainingNonlandRewards(CardPool rewardPool, Set<String> used,
            List<List<PaperCard>> sources, int maxCreatures) {
        int creatures = (int) rewardPool.toFlatList().stream()
            .filter(RewardThemeCategory.CREATURES::matches).count();
        int remaining = REWARD_POOL_SIZE - rewardPool.countAll();
        for (List<PaperCard> source : sources) {
            for (PaperCard card : source) {
                if (remaining == 0) {
                    return;
                }
                boolean creature = card.getRules().getType().isCreature();
                if (card.getRarity() == CardRarity.MythicRare
                    || card.getRules().getType().isLand()
                    || (creature && creatures >= maxCreatures)
                    || !addReward(rewardPool, used, card)) {
                    continue;
                }
                remaining--;
                if (creature) {
                    creatures++;
                }
            }
        }
    }

    private static boolean isStartingDeckInvalid(DeckGenerationContext context, Deck start) {
        Predicate<PaperCard> colorIdentity = card -> card.getRules().getColorIdentity()
            .hasNoColorsExcept(context.commanderColors());
        List<PaperCard> mainCards = start.getMain().toFlatList();
        Deck supportingDeck = new Deck("Supporting cards");
        for (PaperCard card : mainCards) {
            supportingDeck.getMain().add(card);
        }
        return start.getCommanders().size() != context.commanders().size()
            || !new HashSet<>(start.getCommanders()).containsAll(context.commanders())
            || start.getMain().countAll() != STARTING_SPELL_COUNT + STARTING_LAND_COUNT
            || mainCards.stream().filter(card -> card.getRules().getType().isLand()).count() != STARTING_LAND_COUNT
            || mainCards.stream().anyMatch(card -> !DeckFormat.Commander.isLegalCard(card)
                || !colorIdentity.test(card))
            || CommanderBracketCalculator.getBracket(supportingDeck) > MAX_STARTING_SUPPORT_BRACKET
            || CommanderBracketCalculator.getBracket(start) > context.maxBracket();
    }

    private static boolean isRewardPoolInvalid(CardPool rewardPool) {
        List<PaperCard> cards = rewardPool.toFlatList();
        int mythics = (int) cards.stream()
            .filter(card -> card.getRarity() == CardRarity.MythicRare).count();
        int nonMythics = cards.size() - mythics;
        int lands = (int) cards.stream()
            .filter(card -> card.getRarity() != CardRarity.MythicRare
                && card.getRules().getType().isLand()).count();
        int nonlandNonMythics = nonMythics - lands;
        int creatures = (int) cards.stream()
            .filter(RewardThemeCategory.CREATURES::matches).count();
        return cards.size() != REWARD_POOL_SIZE || mythics < 30 || mythics > 45
            || lands * 100 < nonMythics * 8 || lands * 100 > nonMythics * 12
            || creatures * 100 < nonlandNonMythics * MIN_REWARD_CREATURE_PERCENT
            || creatures * 100 > nonlandNonMythics * MAX_REWARD_CREATURE_PERCENT;
    }

    private static void addSpells(Map<String, PaperCard> spells, List<PaperCard> candidates,
            Set<String> supportingNames, Set<String> completeNames, int maxBracket,
            int targetCreatures, int targetSize) {
        for (PaperCard card : candidates) {
            if (spells.size() >= targetSize) {
                return;
            }
            if (!card.getRules().getType().isLand()
                && card.getRules().getManaCost().getCMC() <= MAX_STARTING_CMC
                && isWithinStartingRatio(spells, card, targetCreatures)) {
                addSupportingCard(spells, card, supportingNames, completeNames, maxBracket);
            }
        }
    }

    private static boolean isWithinStartingRatio(Map<String, PaperCard> spells, PaperCard card,
            int targetCreatures) {
        if (targetCreatures < 0) {
            return true;
        }
        long creatures = spells.values().stream()
            .filter(selected -> selected.getRules().getType().isCreature()).count();
        return card.getRules().getType().isCreature()
            ? creatures < targetCreatures : spells.size() - creatures < STARTING_SPELL_COUNT - targetCreatures;
    }

    private static void addSupportingCard(Map<String, PaperCard> spells, PaperCard card,
            Set<String> supportingNames, Set<String> completeNames, int maxBracket) {
        if (spells.containsKey(card.getName())
            || !canAddSupportingCard(card, supportingNames, completeNames, maxBracket)) {
            return;
        }
        spells.put(card.getName(), card);
        supportingNames.addAll(CommanderBracketCalculator.getCardNames(card));
        completeNames.addAll(CommanderBracketCalculator.getCardNames(card));
    }

    private static boolean canAddSupportingCard(PaperCard card, Set<String> supportingNames,
            Set<String> completeNames, int maxBracket) {
        if (card.getRarity() == CardRarity.MythicRare) {
            return false;
        }
        Set<String> cardNames = CommanderBracketCalculator.getCardNames(card);
        Set<String> nextSupporting = new HashSet<>(supportingNames);
        nextSupporting.addAll(cardNames);
        // Supporting cards stay at bracket 2; the Commander may make the complete deck higher.
        if (CommanderBracketCalculator.calculate(nextSupporting).getBracket() > MAX_STARTING_SUPPORT_BRACKET) {
            return false;
        }
        Set<String> nextComplete = new HashSet<>(completeNames);
        nextComplete.addAll(cardNames);
        return CommanderBracketCalculator.calculate(nextComplete).getBracket() <= maxBracket;
    }

    private static int fixingColors(PaperCard land, ColorSet commanderColors) {
        if (land.getRules().getOracleText().toLowerCase(Locale.ROOT).contains("any color")) {
            return commanderColors.countColors();
        }
        return Integer.bitCount(land.getRules().getDeckbuildingColors().getColor()
            & commanderColors.getColor());
    }

    /** Fills the list to the target count with basic lands in the commander's colors. */
    public static boolean skipBasicLands(List<PaperCard> lands, ColorSet colors, String landEdition,
            int targetLandCount) {
        String[] basicNames = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
        byte[] basicColors = {MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK,
            MagicColor.RED, MagicColor.GREEN};
        List<PaperCard> basics = new ArrayList<>();
        for (int i = 0; i < basicNames.length; i++) {
            if (colors.hasAnyColor(basicColors[i])) {
                PaperCard basic = RogueConfig.getBasicLandPrint(basicNames[i], landEdition);
                if (basic == null) {
                    return true;
                }
                basics.add(basic);
            }
        }
        if (basics.isEmpty()) {
            PaperCard wastes = RogueConfig.getBasicLandPrint("Wastes", landEdition);
            if (wastes == null) {
                return true;
            }
            basics.add(wastes);
        }
        for (int i = 0; lands.size() < targetLandCount; i++) {
            lands.add(basics.get(i % basics.size()));
        }
        return false;
    }

    private static boolean addReward(CardPool rewards, Set<String> used, PaperCard card) {
        if (card == null || card.getRules().getType().isBasicLand() || !used.add(card.getName())) {
            return false;
        }
        rewards.add(card);
        return true;
    }
}

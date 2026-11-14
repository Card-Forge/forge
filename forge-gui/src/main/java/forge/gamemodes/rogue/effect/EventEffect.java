package forge.gamemodes.rogue.effect;

import com.google.common.collect.ImmutableList;
import forge.deck.DeckSection;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.*;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.gamemodes.rogue.path.NodeChest;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum EventEffect implements RogueEffect {

    AFTER_DUSK_INSANE("after_dusk_insane", "Turn Insane",
        "Add 3 random Nightmare cards to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Nightmare")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsToDeck(run, ctx, getDBCardsFilter(), 3, null, ImmutableList.of());
        }
    },
    AFTER_DUSK_EXPLORE("after_dusk_explore", "Explore Mansion",
        "Lose 3 Life. Choose up to 5 Room cards to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.subType("Room"));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(3);
            selectCardsForDeck(run, ctx, getDBCardsFilter(), 20, 0, 5, null);
        }
    },
    AFTER_DUSK_FEED("after_dusk_feed", "Feed Monsters",
        "Remove all white cards from your deck. Gain 2 random legendary Horror {{Fellow}}s.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Horror")));
        }

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_WHITE);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            addCarryCards(run, ctx, getDBCardsFilter(), 2, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    AMBUSH_FIGHT("ambush_fight", "Fight!", "Fight a random {{Planebound}} on a random Plane!",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            triggerPlanebound(ctx, RoguePlaneboundType.NORMAL);
        }
    },
    AMBUSH_BRIBE("ambush_bribe", "Lose 4 Gold", "You lose 4 gold.",
        EffectType.ONESHOT, 4, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.spendGold(getGoldCost());
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost());
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return run.hasEnoughGold(getGoldCost()) ? null : INSUFFICIENT_GOLD;
        }
    },
    AMONG_MURDERERS_INVESTIGATE("among_murderers_investigate", "Investigate",
        "Gain a random Clue {{Item}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Clue")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, RogueRun.CarryCardType.ITEM, ImmutableList.of());
        }
    },
    AMONG_MURDERERS_CONFESS("among_murderers_confess", "Confess",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Confession") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    AMONG_MURDERERS_HIRE("among_murderers_hire", "Hire",
        "Pay 4 {{Gold}}. Choose up to 5 out of 20 Detectives to add to your deck.",
        EffectType.ONESHOT, 4, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Detective")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.spendGold(getGoldCost());
            selectCardsForDeck(run, ctx, getDBCardsFilter(), 20, 0, 5, null);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost())
                && !run.getAllCardsForActiveCommander(getDBCardsFilter()).isEmpty();
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            if (!run.hasEnoughGold(getGoldCost())) {
                return INSUFFICIENT_GOLD;
            }
            return "No detectives are available for your commander.";
        }
    },
    BENDING_WALK("bending_walk", "Walk with a Companion", "Gain a random Ally {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Ally")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, RogueRun.CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    BENDING_STUDY("bending_study", "Study the Scrolls",
        "Gain 3 random Lesson {{Scroll}}s.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                (CardRulesPredicates.IS_INSTANT
                    .or(CardRulesPredicates.IS_SORCERY))
                    .and(CardRulesPredicates.subType("Lesson")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 3, RogueRun.CarryCardType.SCROLL, ImmutableList.of());
        }
    },
    BREAKING_OLD_LAWS_DUPLICATE("breaking_the_old_laws_duplicate", "Duplicate",
        "Lose 3 Life. Choose a card from your deck. Add 3 copies of the chosen card to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> candidateCards = run.getSelectableDeckCards(null);
            if (candidateCards.isEmpty()) {
                return;
            }

            run.loseLife(3);
            selectCardsForDeck(ctx, candidateCards, 1, 1);
            ctx.cardSelectionCopyCount = 3;
        }
    },
    BREAKING_OLD_LAWS_BLACK_MARKET("breaking_the_old_laws_black_market", "Black Market",
        "Shop from a selection of cards from the Commander Banlist at doubled prices. Opens a {{Bazaar}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> banlistCards = run.getBanlistCardsForActiveCommander();
            if (banlistCards.isEmpty()) {
                return;
            }

            var priceOverrides = new HashMap<String, Integer>();
            for (PaperCard card : banlistCards) {
                priceOverrides.put(card.getName(), BazaarPricing.getCardPrice(card) * 2);
            }

            triggerCustomBazaar(ctx, "Black Market", banlistCards, priceOverrides);
        }
    },
    BREAKING_OLD_LAWS_PARTNER_UP("breaking_the_old_laws_partner_up", "Partner Up",
        "Lose 4 {{Max. Life}}. " + TRAIT_GAIN_DESCRIPTION + " Choose 1 out of 20 Partners "
            + "for your commander.",
        EffectType.ONESHOT, "Event Trait - Partnership") {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(rules -> rules.hasKeyword("Partner")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseMaxLife(4);
            run.addEventEffect(this);

            List<PaperCard> candidateCards = new ArrayList<>(RogueConfig.getRandomCards(getDBCardsFilter()).stream()
                .map(RogueConfig::getRulesNamePrint)
                .collect(Collectors.toList()));
            candidateCards = run.filterDuplicateCards(candidateCards);
            List<CardReference> partnerPrintOverrides = ImmutableList.of(
                new CardReference("Akiri, Line-Slinger", "PZ2", 1),
                new CardReference("Armix, Filigree Thrasher", "CMR", 1),
                new CardReference("Bruse Tarl, Boorish Herder", "PZ2", 1),
                new CardReference("Eligeth, Crossroads Augur", "CMR", 1),
                new CardReference("Esior, Wardwing Familiar", "CMR", 1),
                new CardReference("Glacian, Powerstone Engineer", "CMR", 1),
                new CardReference("Kraum, Ludevic's Opus", "PZ2", 1),
                new CardReference("Kydele, Chosen of Kruphix", "PZ2", 1),
                new CardReference("Ludevic, Necro-Alchemist", "PZ2", 1),
                new CardReference("Ravos, Soultender", "PZ2", 1),
                new CardReference("Reyhan, Last of the Abzan", "CM2", 1),
                new CardReference("Siani, Eye of the Storm", "CMR", 1),
                new CardReference("Silas Renn, Seeker Adept", "PZ2", 1),
                new CardReference("Tana, the Bloodsower", "PZ2", 1),
                new CardReference("Tymna the Weaver", "PZ2", 1)
            );
            candidateCards = applyCardPrintOverrides(candidateCards, partnerPrintOverrides);
            Collections.shuffle(candidateCards, MyRandom.getRandom());
            candidateCards = new ArrayList<>(candidateCards.subList(0, Math.min(20, candidateCards.size())));

            ctx.addSection = DeckSection.Commander;
            selectCardsForDeck(ctx, candidateCards, 1, 1);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.getMaxLife() > 4;
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null : INSUFFICIENT_MAX_LIFE;
        }
    },
    BURROWED_BROWSE("burrowed_browse", "Browse",
        "Shop from a selection of Woodland creatures.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            Predicate<PaperCard> creatureFilter = PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(
                    CardRulesPredicates.subType("Bat")
                        .or(CardRulesPredicates.subType("Bird"))
                        .or(CardRulesPredicates.subType("Frog"))
                        .or(CardRulesPredicates.subType("Lizard"))
                        .or(CardRulesPredicates.subType("Mouse"))
                        .or(CardRulesPredicates.subType("Otter"))
                        .or(CardRulesPredicates.subType("Rabbit"))
                        .or(CardRulesPredicates.subType("Raccoon"))
                        .or(CardRulesPredicates.subType("Rat"))
                        .or(CardRulesPredicates.subType("Squirrel"))));
            return card -> ("BLB".equalsIgnoreCase(card.getEdition())
                || "BLC".equalsIgnoreCase(card.getEdition()))
                && creatureFilter.test(card);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            triggerCustomBazaar(ctx, "Woodland Caravan",
                run.getAllCardsForActiveCommander(getDBCardsFilter()), null);
        }
    },
    BURROWED_FREE("burrowed_free", "Free",
        "Gain a random {{Wound}}. Gain 3 random legendary Woodland {{Fellow}}s.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            Predicate<PaperCard> creatureFilter = PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Bat")
                        .or(CardRulesPredicates.subType("Bird"))
                        .or(CardRulesPredicates.subType("Frog"))
                        .or(CardRulesPredicates.subType("Lizard"))
                        .or(CardRulesPredicates.subType("Mouse"))
                        .or(CardRulesPredicates.subType("Otter"))
                        .or(CardRulesPredicates.subType("Rabbit"))
                        .or(CardRulesPredicates.subType("Raccoon"))
                        .or(CardRulesPredicates.subType("Rat"))
                        .or(CardRulesPredicates.subType("Squirrel"))));
            return card -> ("BLB".equalsIgnoreCase(card.getEdition())
                || "BLC".equalsIgnoreCase(card.getEdition()))
                && creatureFilter.test(card);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
            addCarryCards(run, ctx, getDBCardsFilter(), 3, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    BURROWED_SELL("burrowed_sell", "Sell",
        "Sell all non-Human creatures from your deck for 2 {{Gold}} each.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Human").negate()));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            if (!ctx.removedCards.isEmpty()) {
                run.addGold(ctx.removedCards.size() * 2);
            }
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return !run.getSelectableDeckCards(getDeckCardFilter()).isEmpty();
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null : "Your deck has no non-Human creatures to sell.";
        }
    },
    CARAVAN_ROB("caravan_rob", "Caravan Plunder", "Lose 3 Life, gain 8 {{Gold}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(3);
            run.addGold(8);
        }
    },
    CARAVAN_BROWSE("caravan_browse", "Browse Wares", "Opens a {{Bazaar}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            ctx.trigger = EffectResultContext.ActionTriggerType.BAZAAR;
        }
    },
    CROOKED_COUNSEL_FELLOWSHIP("crooked_counsel_fellowship", "Rally the Free Peoples",
        "Remove all black cards from your deck. Choose up to 9 creatures from a set of legendary Halflings, Humans, Elves, Dwarves, and Wizards to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Halfling")
                        .or(CardRulesPredicates.subType("Human"))
                        .or(CardRulesPredicates.subType("Elf"))
                        .or(CardRulesPredicates.subType("Dwarf"))
                        .or(CardRulesPredicates.subType("Wizard"))));
        }

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_BLACK);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            List<PaperCard> fellowshipCards = run.getAllCardsForActiveCommander(getDBCardsFilter());
            if (fellowshipCards.isEmpty()) {
                return;
            }

            selectCardsForDeck(run, ctx, getDBCardsFilter(), 20, 0, 9, null);
        }
    },
    CROOKED_COUNSEL_RING("crooked_counsel_ring", "Keep to your own path",
        "Lose 5 Life. Gain the legendary artifact {{Item}} %s.",
        EffectType.ONESHOT, "The One Ring|LTR|2") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(5);
            addEffectCardAsCarryCard(run, ctx, RogueRun.CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardAsCarryCard(getEffectCard());
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null
                : "You already have The One Ring in your deck or as an active Item.";
        }
    },
    CROOKED_COUNSEL_NAZGUL("crooked_counsel_nazgul", "Join with the dark lord",
        "Remove 9 random creatures from your deck. When you do, add 9 copies of [[Nazgûl]] to your deck.",
        EffectType.ONESHOT, null) {

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_CREATURE);
        }

        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return card -> card.getRules().getName().equals("Nazgûl");
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            if (run.getSelectableDeckCards(getDeckCardFilter()).size() < 9) {
                return;
            }
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), 9);
            addCardsToDeck(run, ctx, getDBCardsFilter(), 1, 9, ImmutableList.of());
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardToDeck(RogueConfig.getCard("Nazgûl", null, null));
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null : "Your commander or deck does not allow Nazgûl.";
        }
    },
    DISTORTION_EMBRACE("distortion_embrace", "Distortion - Embrace",
        "Turn all uncompleted future {{Side Node}}s into {{Chest}} Nodes. All {{Planebound}}s of the next 2 rows gain an additional instance of {{Cursed}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            if (run.getCurrentNode() == null || run.getPath() == null) {
                return;
            }

            int currentRow = run.getCurrentNode().getRowIndex();
            run.getPath().replaceNodes(
                node -> node.getRowIndex() > currentRow
                    && !node.isCompleted()
                    && node.isSideNode()
                    && !(node instanceof NodeChest),
                NodeChest::new);
            run.getPath().updateNextPlaneboundRows(currentRow, 2,
                node -> node.setCursedCount(node.getCursedCount() + 1));
        }
    },
    DISTORTION_ENDURE("distortion_endure", "Distorted Reality",
        TRAIT_GAIN_DESCRIPTION + "!{{Gold}} !{{Card Reward}}",
        EffectType.CONSUME, "Event Trait - Distorted Reality") {
        @Override
        public int getChargesForRank(int rank) { return 2; }

        @Override
        public void onBeforeRewards(MatchRewardContext ctx, RogueRun run) {
            ctx.goldRewardAdjustment -= 1;
            ctx.nonMythicCardCountAdjustment -= 3;
            run.consumeEffect(getId());
        }
    },
    DRIFTED_RESCUE("drifted_rescue", "Rescue Pilot", "Lose 3 Life. Gain a random legendary Human {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Human")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {

            run.loseLife(3);
            addCarryCards(run, ctx, getDBCardsFilter(), 1, RogueRun.CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    DRIFTED_STEAL("drifted_steal", "Steal Vehicle", "Gain a random Vehicle {{Item}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Vehicle")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, RogueRun.CarryCardType.ITEM, ImmutableList.of());
        }
    },
    ETERNAL_CRUSADE_SECURE_SPECIMEN("eternal_crusade_secure_specimen", "Secure Specimen",
        "Gain a random Tyranid {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Tyranid")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    ETERNAL_CRUSADE_JOIN_SPACE_MARINES("eternal_crusade_join_space_marines", "Join Space Marines",
        "All future {{Planebound}}s gain 2 additional instances of {{Wrathful}}. Gain the {{Trait}} "
            + "%s.",
        EffectType.ONESHOT, "Event Trait - Codex Astartes") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            if (run.getCurrentNode() != null && run.getPath() != null) {
                int currentRow = run.getCurrentNode().getRowIndex();
                run.getPath().updatePlanebounds(
                    node -> node.getRowIndex() > currentRow && !node.isCompleted(),
                    node -> node.setWrathfulCount(node.getWrathfulCount() + 2));
            }
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    ETERNAL_CRUSADE_OFFER_SACRIFICE("eternal_crusade_offer_sacrifice", "Offer Sacrifice",
        "Choose 3 creatures to remove from your deck. When you do, gain the legendary {{Item}} [[The Golden Throne|40K|1]].",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_CREATURE);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            PaperCard goldenThrone = RogueConfig.getCard("The Golden Throne", "40K", 1);
            selectCardsFromDeck(run, ctx, getDeckCardFilter(), 3, 3, null,
                goldenThrone == null ? null
                    : new EffectResultContext.CarryCardReward(goldenThrone, CarryCardType.ITEM, getId()));
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            PaperCard goldenThrone = RogueConfig.getCard("The Golden Throne", "40K", 1);
            return run.getSelectableDeckCards(getDeckCardFilter()).size() >= 3
                && run.canAddCardAsCarryCard(goldenThrone);
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            if (run.getSelectableDeckCards(getDeckCardFilter()).size() < 3) {
                return "You need at least 3 creatures in your deck.";
            }
            PaperCard goldenThrone = RogueConfig.getCard("The Golden Throne", "40K", 1);
            return run.canAddCardAsCarryCard(goldenThrone) ? null
                : "You already have The Golden Throne in your deck or as an active Item.";
        }
    },
    FINAL_PREPARATIONS_LEARN_SUMMONING("final_preparations_learn_summoning", "Learn Summoning",
        "Gain a random Summon {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return card -> card.getName().startsWith("Summon:");
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    FINAL_PREPARATIONS_LEVEL_UP("final_preparations_level_up", "Level Up",
        "Gain 3 {{Max. Life}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addMaxLife(3);
        }
    },
    FINAL_PREPARATIONS_VISIT_SMITH("final_preparations_visit_smith", "Visit Smith",
        "Shop from a selection of legendary Equipment cards. Opens a {{Bazaar}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Equipment")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            triggerCustomBazaar(ctx, "Last-Stop Smithy",
                run.getAllCardsForActiveCommander(getDBCardsFilter()), null);
        }
    },
    GAMECHANGER_TRUST("gamechanger_trust", "Trade for Gamechangers",
        "Remove 3 random cards from your deck. Choose 3 out of 20 cards from the Gamechanger list to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> gamechangerCards = run.getGamechangerCardsForActiveCommander();
            swapDeckCards(run, ctx, gamechangerCards);
        }
    },
    GAMECHANGER_CHOOSE("gamechanger_choose", "Browse Gamechangers",
        "Shop from a selection of Gamechanger cards at doubled prices. Opens a {{Bazaar}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> gamechangerCards = run.getGamechangerCardsForActiveCommander();
            if (gamechangerCards.isEmpty()) {
                return;
            }

            var priceOverrides = new HashMap<String, Integer>();
            for (PaperCard card : gamechangerCards) {
                priceOverrides.put(card.getName(),
                    BazaarPricing.getCardPrice(card) * 2);
            }

            triggerCustomBazaar(ctx, "Gamechanger Shop", gamechangerCards, priceOverrides);
        }
    },
    GROUND_ZERO_SPECIAL("ground_zero_special", "You're S.P.E.C.I.A.L.", "Add all 7 **Bobblehead** artifacts to your deck. ![[Charisma Bobblehead|PIP|1]] ![[Intelligence Bobblehead|PIP|1]] ![[Strength Bobblehead|PIP|1]]",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Bobblehead")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            String setCode = "PIP";
            int artIndex = 1;

            addCardsToDeck(run, ctx, getDBCardsFilter(), null, null, ImmutableList.of(
                new CardReference("Agility Bobblehead", setCode, artIndex),
                new CardReference("Charisma Bobblehead", setCode, artIndex),
                new CardReference("Endurance Bobblehead", setCode, artIndex),
                new CardReference("Intelligence Bobblehead", setCode, artIndex),
                new CardReference("Luck Bobblehead", setCode, artIndex),
                new CardReference("Perception Bobblehead", setCode, artIndex),
                new CardReference("Strength Bobblehead", setCode, artIndex)
            ));
        }
    },
    GROUND_ZERO_REPAIR("ground_zero_repair", "Use Workbench",
        "Remove all artifact cards from your deck and lose all {{Item}}s. Gain 3 random Robot {{Fellow}}s.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Robot")));
        }

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_ARTIFACT);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            removeCarryCards(run, ctx, CarryCardType.ITEM);
            addCarryCards(run, ctx, getDBCardsFilter(), 3, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    GROUND_ZERO_MUTATE("ground_zero_mutate", "Explore Wasteland", "Remove 5 random creatures from your deck. For each creature removed this way, add a random radiation mutant to your deck.",
        EffectType.ONESHOT, null) {

        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            Predicate<PaperCard> mutantFilter = PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Mutant")));
            return card -> "PIP".equalsIgnoreCase(card.getEdition())
                && mutantFilter.test(card);
        }

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.IS_CREATURE);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), 5);
            int removedCount = ctx.removedCards.size();
            addCardsToDeck(run, ctx, getDBCardsFilter(), removedCount, null, ImmutableList.of());
        }
    },
    HEALER_POTION("healer_potion", "Healer's Potion", "Pay 5 {{Gold}}, gain 8 Life.",
            EffectType.ONESHOT, 5, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.gainLifeUpToMax(8);
            run.spendGold(getGoldCost());
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost()) && run.getCurrentLife() < run.getMaxLife();
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            if (!run.hasEnoughGold(getGoldCost())) {
                return INSUFFICIENT_GOLD;
            }
            if (run.getCurrentLife() >= run.getMaxLife()) {
                return "You are already at maximum Life.";
            }
            return null;
        }
    },
    HEALER_TREAT_WOUNDS("healer_treatment", "Healer's Treatment", "Pay 3 {{Gold}}, clear all {{Wound}}s.",
        EffectType.ONESHOT, 3, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.clearWounds();
            run.spendGold(getGoldCost());
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost()) && !run.getActiveWoundEffects().isEmpty();
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            if (!run.hasEnoughGold(getGoldCost())) {
                return INSUFFICIENT_GOLD;
            }
            if (run.getActiveWoundEffects().isEmpty()) {
                return "You have no active wounds.";
            }
            return null;
        }
    },
    HEALER_STRENGTHEN("healer_strengthen", "Healer's Strength", "Pay 7 {{Gold}}, gain 10 {{Max. Life}}.",
        EffectType.ONESHOT, 7, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addMaxLife(10);
            run.spendGold(getGoldCost());
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost());
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return run.hasEnoughGold(getGoldCost()) ? null : INSUFFICIENT_GOLD;
        }
    },
    HORROR_SURRENDER("horror_surrender", "Lose All Gold", "You lose all your {{Gold}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.setCurrentGold(0);
        }
    },
    HORROR_RESIST("horror_resist", "Lose All Echoes", "You lose all your {{Echoes}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            RogueMetaProgress.getInstance().setTotalEchoes(0);
        }
    },
    INFAMOUS_JUNCTION_RAISE_GANG("infamous_junction_raise_gang", "Raise a Gang",
        "Remove all Human creatures from your deck. For each card removed this way, choose a Rogue or Mercenary to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Human")));
        }

        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.subType("Rogue")
                        .or(CardRulesPredicates.subType("Mercenary"))));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            if (ctx.removedCards.isEmpty()) {
                return;
            }

            selectCardsForDeck(run, ctx, getDBCardsFilter(),
                Math.max(ctx.removedCards.size(), 20), ctx.removedCards.size(), ctx.removedCards.size(), null);
        }
    },
    INFAMOUS_JUNCTION_ROB_BANK("infamous_junction_rob_bank", "Rob the Local Bank",
        "Lose 3 Life. Gain 8 {{Gold}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(3);
            run.addGold(8);
        }
    },
    INFAMOUS_JUNCTION_ROPE_CATTLE("infamous_junction_rope_cattle", "Rope the Lost Cattle",
        "Gain a random Mount {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Mount")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, CarryCardType.FELLOW, ImmutableList.of());
        }
    },
    LOST_NOT_FORGOTTEN_PARTY("lost_not_forgotten_party", "Stumble Into Party",
        "Add a random legendary Wizard, Warrior, Cleric and Rogue to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsToDeck(run, ctx, getWizardFilter(), 1, null, null);
            addCardsToDeck(run, ctx, getWarriorFilter(), 1, null, null);
            addCardsToDeck(run, ctx, getClericFilter(), 1, null, null);
            addCardsToDeck(run, ctx, getRogueFilter(), 1, null, null);
        }

        private Predicate<PaperCard> getWizardFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Wizard")));
        }

        private Predicate<PaperCard> getWarriorFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Warrior")));
        }

        private Predicate<PaperCard> getClericFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Cleric")));
        }

        private Predicate<PaperCard> getRogueFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Rogue")));
        }
    },
    LOST_NOT_FORGOTTEN_LEVEL_UP("lost_not_forgotten_level_up", "Level Up",
        "Pay 4 {{Gold}}. Choose up to 5 Classes to add to your deck.",
        EffectType.ONESHOT, 4, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(CardRulesPredicates.subType("Class"));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.spendGold(getGoldCost());
            selectCardsForDeck(run, ctx, getDBCardsFilter(), 20, 0, 5, null);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.hasEnoughGold(getGoldCost());
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return run.hasEnoughGold(getGoldCost()) ? null : INSUFFICIENT_GOLD;
        }
    },
    LOST_NOT_FORGOTTEN_VENTURE_DEEPER("lost_not_forgotten_venture_deeper", "Venture deeper",
        "Roll a d20.\n1: Trap - Gain a random {{Wound}}.\n2-9: Enemies - Fight a random {{Planebound}} on a random Plane.\n10-19: Find Loot - Gain the {{Item}} %s.\n20: Find Treasure Vault - Gain 20 {{Gold}} and find a {{Chest}}.",
        EffectType.ONESHOT, "Treasure Chest") {

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            int result = rollD20();
            if (result == 1) {
                gainWound(run, ctx);
                ctx.resultTextOverride = "A floor plate drops under your boot. Darts strike before you can reach cover.";
                return;
            }
            if (result <= 9) {
                triggerPlanebound(ctx, RoguePlaneboundType.NORMAL);
                return;
            }
            if (result <= 19) {
                addEffectCardAsCarryCard(run, ctx, CarryCardType.ITEM);
                ctx.resultTextOverride = "You pry a loose stone from the wall and find a sealed coffer behind it.";
                return;
            }

            run.addGold(20);
            triggerChest(ctx);
        }
    },
    LOST_DEPART("lost_depart", "Lost Connection - Depart",
        "You may not cast your Commander in the next match.",
        EffectType.CONSUME, "Event Trait - Depart") {
        @Override
        public int getChargesForRank(int rank) { return 1; }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            run.consumeEffect(getId());
        }
    },
    LOST_PERSIST("lost_weakened", "Lost Connection - Commander Weakened",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.PERMANENT, "Event Trait - Commander Weakened") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    LOST_REPLACE("lost_replace", "Lost Connection - Replace",
        "Choose a new Commander for your deck for the rest of the Run.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            int commanderColorIdentityMask = run.getCommanderColorIdentityMask();
            Predicate<PaperCard> commanderFilter = card ->
                card.getRules().getType().isCreature()
                    && card.getRules().getType().isLegendary()
                    && card.getRules().getColorIdentity().getColor() == commanderColorIdentityMask;

            ctx.addSection = DeckSection.Commander;
            ctx.replaceCurrentCardsInAddSection = true;
            selectCardsForDeck(run, ctx, commanderFilter, 20, 1, 1, null);
        }
    },
    NEON_LID_SAMURAI("neon_lid_samurai", "Path of the Samurai",
        "Remove all Human creatures from your deck. For each creature removed this way, choose a Samurai to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Samurai")));
        }

        @Override
        public Predicate<PaperCard> getDeckCardFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Human")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            removeCardsFromDeck(run, ctx, getDeckCardFilter(), null);
            selectCardsForDeck(run, ctx, getDBCardsFilter(), Math.max(ctx.removedCards.size(), 20), ctx.removedCards.size(), ctx.removedCards.size(), null);
        }
    },
    NEON_LID_NINJA("neon_lid_ninja", "Path of the Ninja",
        "Lose 6 {{Max. Life}}. " + TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Ninjutsu Mastery") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseMaxLife(6);
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.getMaxLife() > 6;
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null : INSUFFICIENT_MAX_LIFE;
        }
    },
    NEON_LID_SHRINE("neon_lid_shrine", "Path of Inner Peace",
        "For each color of your commander, add a random legendary Shrine to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_LEGENDARY.and(CardRulesPredicates.subType("Shrine")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            int colorCount = Integer.bitCount(run.getCommanderColorIdentityMask());
            addCardsToDeck(run, ctx, getDBCardsFilter(), colorCount, null, ImmutableList.of());
        }
    },
    ON_THE_EDGE_BOARD("on_the_edge_board", "Board",
        "Move to a random other location on the map and enter it.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            if (run.getPath() == null || run.getPath().getNodeCount() <= 1) {
                return;
            }

            int currentNodeIndex = run.getCurrentNodeIndex();
            int moveNodeIndex = currentNodeIndex;
            while (moveNodeIndex == currentNodeIndex) {
                moveNodeIndex = MyRandom.getRandom().nextInt(run.getPath().getNodeCount());
            }

            ctx.moveNodeIndex = moveNodeIndex;
            ctx.trigger = EffectResultContext.ActionTriggerType.MOVE;
        }
    },
    ON_THE_EDGE_HIJACK("on_the_edge_hijack", "Hijack",
        "Gain a random Spacecraft {{Item}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Spacecraft")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, getDBCardsFilter(), 1, CarryCardType.ITEM, ImmutableList.of());
        }
    },
    ON_THE_EDGE_SCAVENGE("on_the_edge_scavenge", "Scavenge",
        "Lose 3 Life. Choose up to 5 out of 20 Robots to add to your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE.and(CardRulesPredicates.subType("Robot")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(3);
            selectCardsForDeck(run, ctx, getDBCardsFilter(), 20, 0, 5, null);
        }
    },
    NOTHING("nothing", "Nothing", "No effect.",
        EffectType.ONESHOT, null),
    PLANAR_EXCHANGE_EXCHANGE("planar_exchange_exchange", "Planar Exchange",
        "Choose 3 cards to remove (excluding basic lands), then receive 3 random cards from your {{Reward Pool}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            int exchangeCount = 3;
            RogueDeck rogueDeck = run.getSelectedRogueDeck();
            List<PaperCard> replacementCards = rogueDeck == null
                ? ImmutableList.of()
                : rogueDeck.drawRewardOptions(exchangeCount, run.getNotAlreadyInDeckPredicate());
            selectCardsFromDeck(run, ctx, PaperCardPredicates.fromRules(CardRulesPredicates.NOT_BASIC_LAND),
                exchangeCount, exchangeCount, replacementCards, null);
        }
    },
    PLANAR_RIFT_ENERGY("planar_rift_energy", "Planar Rift Energy", "Gain 5 {{Gold}}.",
            EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addGold(5);
        }
    },
    PLANAR_RIFT_BOOST("planar_rift_boost", "Planar Rift - Commander Boost",
        TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Commander Boost") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addEventEffect(this);
        }
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    PLANAR_TRIBUTE_REPLACE("planar_tribute_replace", "Planar Shuffle",
            "Remove 3 random cards (excluding basic lands) from your deck. Add 3 random cards from your {{Reward Pool}}.",
            EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {

            RogueDeck rogueDeck = run.getSelectedRogueDeck();
            if (rogueDeck == null) return;

            int swapCount = 3;
            removeCardsFromDeck(run, ctx, PaperCardPredicates.fromRules(CardRulesPredicates.NOT_BASIC_LAND), swapCount);

            List<PaperCard> toAdd = rogueDeck.drawRewardOptions(swapCount, run.getNotAlreadyInDeckPredicate());
            run.addCardsToDeck(toAdd, false);
            rogueDeck.removeFromCardPools(toAdd);
            ctx.addedCards = toAdd;
        }
    },
    PLANAR_TRIBUTE_REMOVE("planar_tribute_remove", "Planar Sacrifice",
        "Choose 3 cards (excluding basic lands) to remove from your deck.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            selectCardsFromDeck(run, ctx, PaperCardPredicates.fromRules(CardRulesPredicates.NOT_BASIC_LAND),
                3, 3, null, null);
        }
    },
    SATCHEL_OPEN("satchel_open", "Open the Satchel", "You find a hidden {{Chest}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            triggerChest(ctx);
        }
    },
    STREET_OF_CONCEALMENT_ACCEPT("street_of_concealment_accept", ACCEPT,
        TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Concealment") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    STREET_OF_GREED_ACCEPT("street_of_greed_accept", ACCEPT,
        "Gain 666 {{Gold}}. " + TRAIT_GAIN_DESCRIPTION + " You cannot gain Life during the Run in any way.",
        EffectType.ONESHOT, "Event Trait - Greed") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addGold(666);
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public void onBeforeGainLife(GainLifeContext ctx, RogueRun run) {
            ctx.amount = 0;
        }
    },
    STREET_OF_FORCEFULNESS_ACCEPT("street_of_forcefulness_accept", ACCEPT,
        TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Forcefulness") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.moveCardsFromDeckToBattlefield(c ->
                c.getRules().getType().isCreature() && c.getRules().getManaCost().getCMC() <= 3,
                1, opponent);
        }
    },
    SHRINE_KNEEL("shrine_kneel", "Kneel", "You discover a hidden {{Sanctum}}.",
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            triggerSanctum(ctx);
        }
    },
    THORNS_ENDURE("thorns_endure", "Gain Wound", "Gain a random {{Wound}}.",
            EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
            if (ctx.gainedWoundEffect == null) {
                ctx.resultTextOverride = "You already bear all wounds.";
            }
        }
    },
    THORNS_PRESS("thorns_press", "Lose 4 Life", "You lose 4 Life.",
            EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(4);
        }
    },
    TRAPPED_IN_THE_LAIR_EXAMINE("trapped_in_the_lair_examine", "Examine",
        "Lose 4 {{Max. Life}}. "  + TRAIT_GAIN_DESCRIPTION,
        EffectType.ONESHOT, "Event Trait - Mutagen") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseMaxLife(4);
            run.addEventEffect(this);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.getMaxLife() > 4;
        }

        @Override
        public String getUnavailableReason(RogueRun run) {
            return isChoiceAvailable(run) ? null : "You don't have enough Max. Life.";
        }
    },
    TRAPPED_IN_THE_LAIR_SLAY("trapped_in_the_lair_slay", "Slay",
        "Gain a random {{Wound}}. Gain 3 random Food {{Item}}s.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Food")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
            addCarryCards(run, ctx, getDBCardsFilter(), 3, CarryCardType.ITEM, ImmutableList.of());
        }
    },
    TRAPPED_IN_THE_LAIR_TAME("trapped_in_the_lair_tame", "Tame",
        "Lose 3 Life. Gain a random legendary Beast {{Fellow}}.",
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(
                CardRulesPredicates.IS_CREATURE
                    .and(CardRulesPredicates.IS_LEGENDARY)
                    .and(CardRulesPredicates.subType("Beast")));
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseLife(3);
            addCarryCards(run, ctx, getDBCardsFilter(), 1, CarryCardType.FELLOW, ImmutableList.of());
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final EffectType effectType;
    private final int goldCost;
    private final String effectCardReference;
    EventEffect(String id, String displayName, String description, EffectType effectType,
                String effectCardReference) {
        this(id, displayName, description, effectType, 0, effectCardReference);
    }

    EventEffect(String id, String displayName, String description, EffectType effectType, int goldCost,
                String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.effectType = effectType;
        this.goldCost = goldCost;
        this.effectCardReference = effectCardReference;
    }

    @Override
    public void applyEffect(RogueRun run, EffectResultContext ctx) { /* Override in ONESHOT constants to apply immediate event effects. */}

    @Override
    public EffectType getEffectType() { return effectType; }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getRawDescription() { return formatEffectCardDescription(description); }

    @Override
    public String getEffectCardReference() { return effectCardReference; }

    public int getGoldCost() { return goldCost; }

    public boolean isChoiceAvailable(RogueRun run) { return true; }

    public String getUnavailableReason(RogueRun run) {
        return null;
    }

    public static EventEffect fromId(String id) {
        for (EventEffect eb : values())
            if (eb.id.equals(id)) return eb;
        return null;
    }
}

package forge.gamemodes.rogue.effect;

import com.google.common.collect.ImmutableList;
import forge.card.CardSplitType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.rogue.CardReference;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.gamemodes.rogue.npc.NPC;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * NPC effects granted during run start encounters.
 * Each constant defines its own behavior explicitly.
 */
public enum NPCEffect implements RogueEffect {

    // Henzie effects
    HENZIE_CONTAINMENT("npc_henzie_containment", "Henzie's Containment",
        "Gain the {{Item}} [[Mana Vault|2X2|2]] and the {{Fellow}} [[Emrakul, the Aeons Torn|2X2|2]].", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Mana Vault|2X2|2", "Emrakul, the Aeons Torn|2X2|2"),
                CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Mana Vault", "Emrakul, the Aeons Torn"));
        }
    },
    HENZIE_CONTRABAND("npc_henzie_contraband", "Henzie's Contraband",
        "Lose 3 {{Max. Life}}. Remove 3 random cards from your deck. Choose 3 out of 20 cards from the Commander Banlist to add to your deck.", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            run.loseMaxLife(3);
            List<PaperCard> banlistCards = run.getBanlistCardsForActiveCommander();
            swapDeckCards(run, ctx, banlistCards);
        }
        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.getMaxLife() > 3;
        }
    },
    HENZIE_PRECIOUS("npc_henzie_precious", "Henzie's Precious",
        ITEM_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.ONESHOT, "The One Ring|LTR|3") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of(getEffectCardReference()),
                CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of(getEffectCardReference()));
        }
    },
    HENZIE_EXQUISITE_TRAITS("npc_henzie_exquisite_traits", "Exquisite Traits",
        "Gain 2 random Chest {{Traits}}.", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<ChestEffect> chestTraits = ChestEffect.getAvailableEffects(run, EffectType.PERMANENT);
            Collections.shuffle(chestTraits, MyRandom.getRandom());
            for (int i = 0; i < Math.min(2, chestTraits.size()); i++) {
                run.addChestEffect(chestTraits.get(i));
            }
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return !ChestEffect.getAvailableEffects(run, EffectType.PERMANENT).isEmpty();
        }
    },
    HENZIE_GAMECHANGERS("npc_henzie_gamechangers", "Henzie's Gamechangers",
        "Remove 3 random cards from your deck. Choose 3 out of 20 cards from the Gamechanger list to add to your deck.", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> gamechangerCards = run.getGamechangerCardsForActiveCommander();
            swapDeckCards(run, ctx, gamechangerCards);
        }
    },
    HENZIE_MYTHICS("npc_henzie_mythics", "Henzie's Mythics",
        "Gain 3 random mythic rare cards from your {{Reward Pool}}.", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsFromCardRewardPool(run, ctx, 3, PaperCardPredicates.IS_MYTHIC_RARE);
        }
    },
    HENZIE_CHERISHED("npc_henzie_cherished", "Henzie's Cherished",
        "Add [[Mana Crypt|2XM|2]] and [[Ugin, the Spirit Dragon|M21|2]] to your deck.", NPC.HENZIE,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsToDeck(run, ctx, ImmutableList.of("Mana Crypt|2XM|2", "Ugin, the Spirit Dragon|M21|2"));
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            PaperCard manaCrypt = RogueConfig.getCard("Mana Crypt", null, null);
            PaperCard ugin = RogueConfig.getCard("Ugin, the Spirit Dragon", null, null);
            return run.canAddCardToDeck(manaCrypt) || run.canAddCardToDeck(ugin);
        }
    },
    HENZIE_CITY("npc_henzie_city", "Henzie's City",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[City of Brass|2X2|1]]", NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Henzie's City") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("City of Brass|2X2|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("City of Brass|2X2|1");
        }
    },
    HENZIE_FIELD("npc_henzie_field", "Henzie's Field",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Field of the Dead|M20|1]]", NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Henzie's Field") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Field of the Dead|M20|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Field of the Dead|M20|1");
        }
    },
    HENZIE_DRAMATIC_ENTRANCE("npc_henzie_dramatic_entrance", "Dramatic Entrance",
        TRAIT_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Dramatic Entrance") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.moveCardsFromDeckToBattlefield(c ->
                c.getRules().getType().isPermanent() && !c.getRules().getType().isLand()
                    && c.getRules().getManaCost().getCMC() <= 3,
                2, human);
        }
    },
    HENZIE_STREET_SMART("npc_henzie_street_smart", "Street Smart",
        TRAIT_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Street Smart") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    HENZIE_SO_MODERATE("npc_henzie_so_moderate", "So Moderate",
        TRAIT_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - So Moderate") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    HENZIE_TOMB("npc_henzie_tomb", "Henzie's Tomb",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Ancient Tomb|UMA|1]]", NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Henzie's Tomb") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Ancient Tomb|UMA|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Ancient Tomb|UMA|1");
        }
    },
    HENZIE_TORMENTOR("npc_henzie_tormentor", "Tormentor",
        TRAIT_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Tormentor") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    HENZIE_CONNECTIONS("npc_henzie_connections", "Henzie's Connections",
        TRAIT_GAIN_DESCRIPTION, NPC.HENZIE,
        EffectType.PERMANENT, "Henzie Trait - Henzie's Connections") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },

    // Narset effects
    NARSET_LANDS_OF_LEGENDS("npc_narset_lands_of_legends", "Lands Of Legends",
        "Choose up to 3 out of 20 legendary lands to add to your deck.", NPC.NARSET,
        EffectType.ONESHOT, null) {
        @Override
        public Predicate<PaperCard> getDBCardsFilter() {
            return PaperCardPredicates.fromRules(rules -> rules.getType().isLand()
                && rules.getType().isLegendary()
                && rules.getSplitType() == CardSplitType.None);
        }

        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            List<PaperCard> candidateCards = run.getAllCardsForActiveCommander(getDBCardsFilter()).stream()
                .map(RogueConfig::getRulesNamePrint)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            candidateCards = run.filterDuplicateCards(candidateCards);
            if (candidateCards.isEmpty()) {
                return;
            }

            List<CardReference> landsOfLegendsPrintOverrides = ImmutableList.of(
                new CardReference("Gemstone Caverns", "PLST", 1),
                new CardReference("Inventors' Fair", "KLD", 1),
                new CardReference("Urborg, Tomb of Yawgmoth", "UMA", 1),
                new CardReference("Miren, the Moaning Well", "SOK", 1),
                new CardReference("Eye of Ugin", "MM2", 1),
                new CardReference("Boseiju, Who Shelters All", "CHK", 1),
                new CardReference("Urborg, Tomb of Yawgmoth", "UMA", 1)
            );
            candidateCards = applyCardPrintOverrides(candidateCards, landsOfLegendsPrintOverrides);
            Collections.shuffle(candidateCards, MyRandom.getRandom());
            candidateCards = new ArrayList<>(candidateCards.subList(0, Math.min(20, candidateCards.size())));
            selectCardsForDeck(ctx, candidateCards, 0, 3);
        }
    },
    NARSET_UTENSILS("npc_narset_utensils", "Narset's Utensils",
        "Gain the {{Item}}s [[Fractured Powerstone|MOC|1]] and [[Ichor Elixir|MOC|1]].", NPC.NARSET,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Fractured Powerstone|MOC|1", "Ichor Elixir|MOC|1"),
                CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Fractured Powerstone", "Ichor Elixir"));
        }
    },
    NARSET_ESCORTS("npc_narset_escorts", "Narset's Escorts",
        "Gain the {{Fellow}}s [[Roaming Throne|LCI|1]] and [[Walking Ballista|2XM|1]].", NPC.NARSET,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Roaming Throne|LCI|1", "Walking Ballista|2XM|1"),
                CarryCardType.FELLOW);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Roaming Throne", "Walking Ballista"));
        }
    },
    NARSET_TRAVELOGUE("npc_narset_travelogue", "Narset's Travelogue",
        "Add [[Horizon of Progress|M3C|1]] and [[Talon Gates of Madara|M3C|1]] to your deck.", NPC.NARSET,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsToDeck(run, ctx,
                ImmutableList.of("Horizon of Progress|M3C|1", "Talon Gates of Madara|M3C|1"));
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            PaperCard horizon = RogueConfig.getCard("Horizon of Progress", "M3C", 1);
            PaperCard talonGates = RogueConfig.getCard("Talon Gates of Madara", "M3C", 1);
            return run.canAddCardToDeck(horizon) || run.canAddCardToDeck(talonGates);
        }
    },
    NARSET_FARSIGHT("npc_narset_farsight", "Farsight",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Farsight") {
        @Override
        public void onPathUpdate(PathUpdateContext ctx, RogueRun run) {
            ctx.additionalVisiblePlaneboundRows += 3;
        }
    },
    NARSET_PASSAGE("npc_narset_passage", "Narset's Passage",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Rogue's Passage|FDC|1]]", NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Narset's Passage") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Rogue's Passage|FDC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Rogue's Passage|FDC|1");
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardToDeck(RogueConfig.getCard("Rogue's Passage", null, null));
        }
    },
    NARSET_TOWER("npc_narset_tower", "Narset's Tower",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Reliquary Tower|TDC|1]]", NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Narset's Tower") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Reliquary Tower|CMM|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Reliquary Tower|CMM|1");
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardToDeck(RogueConfig.getCard("Reliquary Tower", null, null));
        }
    },
    NARSET_VAULT("npc_narset_vault", "Narset's Vault",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Vault of the Archangel|TDC|1]]", NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Narset's Vault") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Vault of the Archangel|TDC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Vault of the Archangel|TDC|1");
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardToDeck(RogueConfig.getCard("Vault of the Archangel", null, null));
        }
    },
    NARSET_WOLF_RUN("npc_narset_wolf_run", "Narset's Wolf Run",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Kessig Wolf Run|TDC|1]]", NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Narset's Wolf Run") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Kessig Wolf Run|TDC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Kessig Wolf Run|TDC|1");
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return run.canAddCardToDeck(RogueConfig.getCard("Kessig Wolf Run", null, null));
        }
    },
    NARSET_CHAOSWALKER("npc_chaoswalker", "Chaoswalker",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Chaoswalker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    NARSET_DARKWALKER("npc_narset_darkwalker", "Darkwalker",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Darkwalker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    NARSET_LIGHTWALKER("npc_narset_lightwalker", "Lightwalker",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Lightwalker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    NARSET_MATTERWALKER("npc_narset_matterwalker", "Matterwalker",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Matterwalker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    NARSET_MINDWALKER("npc_narset_mindwalker", "Mindwalker",
        TRAIT_GAIN_DESCRIPTION, NPC.NARSET,
        EffectType.PERMANENT, "Narset Trait - Mindwalker") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },

    // Tyvar effects
    TYVAR_COMPANIONS("npc_tyvar_companions", "Tyvar's Companions",
        "Gain the {{Fellow}}s [[Loyal Apprentice|CMM|1]] and [[Loyal Unicorn|CMM|1]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Loyal Apprentice|CMM|1", "Loyal Unicorn|CMM|1"), CarryCardType.FELLOW);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Loyal Apprentice", "Loyal Unicorn"));
        }
    },
    TYVAR_FOLLOWERS("npc_tyvar_followers", "Tyvar's Followers",
        "Gain the {{Fellow}}s [[Loyal Guardian|CMM|1]] and [[Loyal Subordinate|CMM|1]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Loyal Guardian|CMM|1", "Loyal Subordinate|CMM|1"), CarryCardType.FELLOW);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Loyal Guardian", "Loyal Subordinate"));
        }
    },
    TYVAR_PETS("npc_tyvar_pets", "Tyvar's Pets",
        "Gain the {{Fellow}}s [[Loyal Drake|CMM|1]] and [[Loyal Unicorn|CMM|1]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Loyal Drake|CMM|1", "Loyal Unicorn|CMM|1"), CarryCardType.FELLOW);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Loyal Drake", "Loyal Unicorn"));
        }
    },
    TYVAR_ARMORY("npc_tyvar_armory", "Tyvar's Armory",
        "Gain the {{Item}}s [[Commander's Sphere|CMM|1]] and [[Commander's Plate|CMR|1]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Commander's Sphere|CMM|1", "Commander's Plate|CMR|1"), CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Commander's Sphere", "Commander's Plate"));
        }
    },
    TYVAR_LOADOUT("npc_tyvar_loadout", "Tyvar's Loadout",
        "Gain the {{Item}}s [[Skullclamp|MSC|2]] and [[Swiftfoot Boots|FDN|2]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Skullclamp|MSC|2", "Swiftfoot Boots|FDN|2"),
                CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Skullclamp", "Swiftfoot Boots"));
        }
    },
    TYVAR_RESPITE("npc_tyvar_respite", "Tyvar's Respite",
        "Gain the {{Item}}s [[Campfire|CMM|1]] and [[Tome of Legends|MKC|1]].", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCarryCards(run, ctx, ImmutableList.of("Campfire|CMM|1", "Tome of Legends|MKC|1"), CarryCardType.ITEM);
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            return canAddAllCarryCards(run, ImmutableList.of("Campfire|CMM|1", "Tome of Legends|MKC|1"));
        }
    },
    TYVAR_STAPLES("npc_tyvar_staples", "Tyvar's Staples",
        "Add a [[Sol Ring|CMM|2]] and [[Arcane Signet|CMM|2]] to your deck.", NPC.TYVAR,
        EffectType.ONESHOT, null) {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            addCardsToDeck(run, ctx, ImmutableList.of("Sol Ring|CMM|2", "Arcane Signet|CMM|2"));
        }

        @Override
        public boolean isChoiceAvailable(RogueRun run) {
            PaperCard solRing = RogueConfig.getCard("Sol Ring", null, null);
            PaperCard arcaneSignet = RogueConfig.getCard("Arcane Signet", null, null);
            return run.canAddCardToDeck(solRing) || run.canAddCardToDeck(arcaneSignet);
        }
    },
    TYVAR_AUGMENTED("npc_tyvar_augmented_commander", "Augmented Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Augmented Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_BASTION("npc_tyvar_bastion_commander", "Bastion Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Bastion Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_BLOODSWORN("npc_tyvar_bloodsworn_commander", "Bloodsworn Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Bloodsworn Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_BRAWLER("npc_tyvar_brawler_commander", "Brawler Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Brawler Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_CHARLATAN("npc_tyvar_charlatan_commander", "Charlatan Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Charlatan Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_CHEF("npc_tyvar_chef_commander", "Master Chef Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Master Chef Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_CRIMINAL("npc_tyvar_criminal_commander", "Criminal Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Criminal Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_CULTIST("npc_tyvar_cultist_commander", "Cultist Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Cultist Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_FLAME("npc_tyvar_flame_commander", "Flame Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Flame Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_SAGE("npc_tyvar_sage_commander", "Sage Commander",
        TRAIT_GAIN_DESCRIPTION, NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Sage Commander") {
        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
        }
    },
    TYVAR_BEACON("npc_tyvar_beacon", "Tyvar's Beacon",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Command Beacon|OTC|1]]", NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Tyvar's Beacon") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Command Beacon|OTC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Command Beacon");
        }
    },
    TYVAR_PALACE("npc_tyvar_palace", "Tyvar's Palace",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Opal Palace|SOC|1]]", NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Tyvar's Palace") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Opal Palace|SOC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Opal Palace");
        }
    },
    TYVAR_SANCTUM("npc_tyvar_sanctum", "Tyvar's Sanctum",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[Sanctum of Eternity|C19|1]]", NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Tyvar's Sanctum") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("Sanctum of Eternity|C19|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("Sanctum of Eternity");
        }
    },
    TYVAR_WAR_ROOM("npc_tyvar_war_room", "Tyvar's War Room",
        TRAIT_GAIN_DESCRIPTION + " Gain a random {{Wound}}. ![[War Room|FDC|1]]", NPC.TYVAR,
        EffectType.PERMANENT, "Tyvar Trait - Tyvar's War Room") {
        @Override
        public void applyEffect(RogueRun run, EffectResultContext ctx) {
            gainWound(run, ctx);
        }

        @Override
        public void onMatchStart(RegisteredPlayer human, RegisteredPlayer opponent, RogueRun run) {
            addEffectCardToCommandZone(human);
            RogueEffect.addCardToBattlefield("War Room|FDC|1", human);
        }

        @Override
        public List<String> getDuplicateProtectedCardReferences() {
            return ImmutableList.of("War Room");
        }
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final NPC ownerNpc;
    private final EffectType effectType;
    private final String effectCardReference;

    NPCEffect(String id, String displayName, String description, NPC ownerNpc, EffectType effectType,
              String effectCardReference) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.ownerNpc = ownerNpc;
        this.effectType = effectType;
        this.effectCardReference = effectCardReference;
    }

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

    public NPC getOwnerNpc() { return ownerNpc; }

    public boolean isChoiceAvailable(RogueRun run) { return true; }

    public static List<NPCEffect> getEffectsForNpc(NPC npc, RogueRun run) {
        List<String> activeNpcEffectIds = run == null ? ImmutableList.of() : run.getActiveNPCEffects().stream()
            .map(RogueEffect::getId)
            .collect(Collectors.toList());
        List<NPCEffect> effects = new ArrayList<>();
        for (NPCEffect effect : values()) {
            if (effect.ownerNpc == npc
                && (effect.effectType != EffectType.PERMANENT || !activeNpcEffectIds.contains(effect.id))
                && (run == null || effect.isChoiceAvailable(run))) {
                effects.add(effect);
            }
        }
        return effects;
    }

    public static NPCEffect fromId(String id) {
        for (NPCEffect b : values())
            if (b.id.equals(id)) return b;
        return null;
    }
}
